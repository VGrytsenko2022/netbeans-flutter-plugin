import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';

void main() {
  test('reviewed widget schema matches runtime metadata exactly', () {
    expect(canvasReviewedWidgetSchemaContract, startsWith('\n'));
    expect(
      canvasReviewedWidgetSchemaContract.substring(1),
      canvasRuntimeWidgetSchemaContractForTesting(),
    );
  });

  test('decodes the strict baseline projection fixture', () {
    final model = CanvasModel.decode(modelBytesForViewTest());

    expect(model.profile.previewMode, 'mobile');
    expect(model.profile.targetPlatform, 'windows');
    expect(model.profile.theme.definitionId, 'light');
    expect(model.profile.theme.seedArgb, 0xff6750a4);
    expect(model.profile.theme.brightness, 'light');
    expect(model.profile.theme.digestIdentity, 'A' * 64);
    expect(model.root.type, 'flutter.material.Scaffold');
    expect(model.widgetIds, hasLength(8));
    expect(
      model.root.slot('body')!.child!.slot('children')!.children,
      hasLength(2),
    );
    final column = model.root.slot('body')!.child!;
    expect(column.properties['spacing']!.value, 12.5);
    expect(
      (column.properties['crossAxisAlignment']!.value as CanvasEnumValue).value,
      'baseline',
    );
    final text = column
        .slot('children')!
        .children
        .first
        .slot('child')!
        .child!
        .slot('child')!
        .child!;
    expect(text.properties['semanticsIdentifier']!.value, 'primary-greeting');
    expect(text.properties['selectionColor']!.value, 0xff336699);
  });

  test('rejects baseline alignment without an exact text baseline', () {
    final json = _modelJson();
    final properties = _column(json)['properties']! as Map<String, Object?>;
    properties.remove('textBaseline');

    expect(() => _decode(json), throwsFormatException);
  });

  test('rejects negative or non-double flex spacing', () {
    final negative = _modelJson();
    (_column(negative)['properties']! as Map<String, Object?>)['spacing'] = {
      'kind': 'double',
      'value': -0.5,
    };
    expect(() => _decode(negative), throwsFormatException);

    final integer = _modelJson();
    (_column(integer)['properties']! as Map<String, Object?>)['spacing'] = {
      'kind': 'integer',
      'value': 1,
    };
    expect(() => _decode(integer), throwsFormatException);
  });

  test('enforces the reviewed list slot child-count bound', () {
    final json = _modelJson();
    _column(json)['slots'] = {
      'children': {
        'kind': 'list',
        'children': List<Object?>.filled(10001, null),
      },
    };

    expect(
      () => _decode(json),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('slot child count is outside its bounds'),
        ),
      ),
    );
  });

  test('requires non-negative Padding values on every physical side', () {
    final missing = _modelJson();
    _paddingProperties(missing).remove('padding');
    expect(
      () => _decode(missing),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('flutter.widgets.Padding.padding'),
        ),
      ),
    );

    for (final side in const {'left', 'top', 'right', 'bottom'}) {
      final negative = _modelJson();
      final padding =
          _paddingProperties(negative)['padding']! as Map<String, Object?>;
      padding[side] = -0.25;

      expect(
        () => _decode(negative),
        throwsA(
          isA<FormatException>().having(
            (failure) => failure.message,
            'message',
            contains('/padding/$side'),
          ),
        ),
        reason: side,
      );
    }

    final zero = _modelJson();
    final zeroPadding =
        _paddingProperties(zero)['padding']! as Map<String, Object?>;
    for (final side in const {'left', 'top', 'right', 'bottom'}) {
      zeroPadding[side] = 0;
    }
    final decoded = _decode(zero);
    final insets =
        _decodedPadding(decoded).properties['padding']!.value
            as CanvasEdgeInsets;
    expect([
      insets.left,
      insets.top,
      insets.right,
      insets.bottom,
    ], everyElement(0));
  });

  test('decodes only strict physical and directional Padding shapes', () {
    final physical = _decode(_modelJson());
    final physicalInsets =
        _decodedPadding(physical).properties['padding']!.value
            as CanvasEdgeInsets;
    expect(
      [
        physicalInsets.left,
        physicalInsets.top,
        physicalInsets.right,
        physicalInsets.bottom,
      ],
      [16, 16, 16, 16],
    );

    final directional = _modelJson();
    _paddingProperties(directional)['padding'] = {
      'kind': 'edgeInsetsDirectional',
      'start': 1,
      'top': 2.5,
      'end': 3,
      'bottom': 4.25,
    };
    final directionalInsets =
        _decodedPadding(_decode(directional)).properties['padding']!.value
            as CanvasEdgeInsetsDirectional;
    expect(
      [
        directionalInsets.start,
        directionalInsets.top,
        directionalInsets.end,
        directionalInsets.bottom,
      ],
      [1, 2.5, 3, 4.25],
    );

    final malformed = <Map<String, Object?>>[
      {
        'kind': 'edgeInsets',
        'left': 1,
        'top': 2,
        'right': 3,
        'bottom': 4,
        'start': 1,
      },
      {
        'kind': 'edgeInsetsDirectional',
        'start': 1,
        'top': 2,
        'end': 3,
        'bottom': 4,
        'left': 1,
      },
      {'kind': 'edgeInsets', 'left': 1, 'top': 2, 'bottom': 4},
      {'kind': 'edgeInsetsDirectional', 'start': 1, 'top': 2, 'bottom': 4},
      {
        'kind': 'edgeInsetsDirectional',
        'start': 1,
        'top': 2,
        'end': 3,
        'bottom': 4,
        'extra': 0,
      },
    ];
    for (final value in malformed) {
      final json = _modelJson();
      _paddingProperties(json)['padding'] = value;
      expect(
        () => _decode(json),
        throwsFormatException,
        reason: value.toString(),
      );
    }
  });

  test('requires non-negative Padding values on every directional side', () {
    for (final side in const {'start', 'top', 'end', 'bottom'}) {
      final negative = _modelJson();
      _paddingProperties(negative)['padding'] = {
        'kind': 'edgeInsetsDirectional',
        'start': 0,
        'top': 0,
        'end': 0,
        'bottom': 0,
        side: -0.25,
      };

      expect(
        () => _decode(negative),
        throwsA(
          isA<FormatException>().having(
            (failure) => failure.message,
            'message',
            contains('/padding/$side'),
          ),
        ),
        reason: side,
      );
    }

    final zero = _modelJson();
    _paddingProperties(zero)['padding'] = {
      'kind': 'edgeInsetsDirectional',
      'start': 0,
      'top': 0,
      'end': 0,
      'bottom': 0,
    };
    final insets =
        _decodedPadding(_decode(zero)).properties['padding']!.value
            as CanvasEdgeInsetsDirectional;
    expect([
      insets.start,
      insets.top,
      insets.end,
      insets.bottom,
    ], everyElement(0));
  });

  test('validates nullable non-negative Center width and height factors', () {
    final omitted = _modelJson();
    final omittedProperties = _centerProperties(omitted)..remove('widthFactor');
    final omittedCenter = _decodedCenter(_decode(omitted));
    expect(omittedProperties, isEmpty);
    expect(omittedCenter.properties['widthFactor'], isNull);
    expect(omittedCenter.properties['heightFactor'], isNull);

    final zero = _modelJson();
    _centerProperties(zero)
      ..['widthFactor'] = {'kind': 'integer', 'value': 0}
      ..['heightFactor'] = {'kind': 'double', 'value': 0.0};
    final zeroCenter = _decodedCenter(_decode(zero));
    expect(zeroCenter.properties['widthFactor']!.kind, 'integer');
    expect(zeroCenter.properties['widthFactor']!.value, 0);
    expect(zeroCenter.properties['heightFactor']!.kind, 'double');
    expect(zeroCenter.properties['heightFactor']!.value, 0.0);

    for (final property in const {'widthFactor', 'heightFactor'}) {
      for (final candidate in const [
        {'kind': 'integer', 'value': -1},
        {'kind': 'double', 'value': -0.1},
      ]) {
        final negative = _modelJson();
        _centerProperties(negative)[property] = candidate;
        expect(
          () => _decode(negative),
          throwsFormatException,
          reason: '$property ${candidate['kind']}',
        );
      }
    }
  });

  test('decodes strict nullable SizedBox dimensions and its single child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?>? child,
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '38f49912-8e51-4e62-bd4c-2517ecad4962',
        'flutter.widgets.SizedBox',
        properties: properties,
        slots: {'child': _single(child)},
      );
      return json;
    }

    final text = _node(
      'f195f817-cf8e-455c-befc-31dd30f874df',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Sized child'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'width': {'kind': 'integer', 'value': 120},
          'height': {'kind': 'double', 'value': 48.5},
        },
        child: text,
      ),
    ).root;
    expect(decoded.type, 'flutter.widgets.SizedBox');
    expect(decoded.properties['width']!.kind, 'integer');
    expect(decoded.properties['width']!.value, 120);
    expect(decoded.properties['height']!.kind, 'double');
    expect(decoded.properties['height']!.value, 48.5);
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child')!.child, isNull);

    for (final property in const {'width', 'height'}) {
      for (final candidate in const [
        {'kind': 'integer', 'value': -1},
        {'kind': 'double', 'value': -0.1},
        {'kind': 'boolean', 'value': true},
      ]) {
        expect(
          () => _decode(model(properties: {property: candidate})),
          throwsFormatException,
          reason: '$property ${candidate['kind']}',
        );
      }
    }
  });

  test('rejects duplicate Text semantics identifiers across the tree', () {
    final json = _modelJson();
    final texts = _textNodes(_column(json));
    final firstProperties = texts[0]['properties']! as Map<String, Object?>;
    final secondProperties = texts[1]['properties']! as Map<String, Object?>;
    secondProperties['semanticsIdentifier'] = Map<String, Object?>.from(
      firstProperties['semanticsIdentifier']! as Map<String, Object?>,
    );

    expect(() => _decode(json), throwsFormatException);
  });

  test('rejects executable or unknown property values', () {
    final json = _modelJson();
    final text =
        (((json['root'] as Map<String, Object?>)['slots']
                    as Map<String, Object?>)['body']
                as Map<String, Object?>)['child']
            as Map<String, Object?>;
    final properties = text['properties'] as Map<String, Object?>;
    properties['onTap'] = {'kind': 'dartExpression', 'value': 'launch()'};

    expect(
      () =>
          CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json)))),
      throwsFormatException,
    );
  });

  test('rejects duplicate stable widget identities', () {
    final json = _modelJson();
    final root = json['root'] as Map<String, Object?>;
    final column =
        ((root['slots'] as Map<String, Object?>)['body']
                as Map<String, Object?>)['child']
            as Map<String, Object?>;
    final children =
        ((column['slots'] as Map<String, Object?>)['children']
                as Map<String, Object?>)['children']
            as List<Object?>;
    (children[1] as Map<String, Object?>)['id'] =
        (children[0] as Map<String, Object?>)['id'];

    expect(
      () =>
          CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json)))),
      throwsFormatException,
    );
  });

  test(
    'admits the exact target-platform vocabulary and rejects unknown targets',
    () {
      for (final platform in const [
        'android',
        'ios',
        'windows',
        'macos',
        'linux',
        'web',
      ]) {
        final json = _modelJson();
        (json['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
        expect(
          CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ).profile.targetPlatform,
          platform,
        );
      }

      final invalid = _modelJson();
      (invalid['profile']! as Map<String, Object?>)['targetPlatform'] =
          'browser';
      expect(
        () => CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(invalid))),
        ),
        throwsFormatException,
      );
    },
  );

  test('rejects malformed or ambiguous project theme values', () {
    final oldProtocol = _modelJson()..['protocolVersion'] = 4;
    expect(() => _decode(oldProtocol), throwsFormatException);

    final invalidSeed = _modelJson();
    _theme(invalidSeed)['seedArgb'] = '0xff6750a4';
    expect(() => _decode(invalidSeed), throwsFormatException);

    final transparentSeed = _modelJson();
    _theme(transparentSeed)['seedArgb'] = '0x7F6750A4';
    expect(() => _decode(transparentSeed), throwsFormatException);

    final invalidDigest = _modelJson();
    _theme(invalidDigest)['digestIdentity'] = 'a' * 64;
    expect(() => _decode(invalidDigest), throwsFormatException);

    final paddedId = _modelJson();
    _theme(paddedId)['definitionId'] = ' light';
    expect(() => _decode(paddedId), throwsFormatException);

    final unknown = _modelJson();
    _theme(unknown)['extra'] = true;
    expect(() => _decode(unknown), throwsFormatException);
  });

  test('decodes typed project ColorScheme and TextTheme overrides', () {
    final json = _modelJson();
    _theme(json)
      ..['colorScheme'] = {'primary': '0xFF123456', 'onSurface': '0xFF112233'}
      ..['textTheme'] = {
        'bodyMedium': {
          'color': {'kind': 'colorScheme', 'role': 'onSurface'},
          'backgroundColor': {'kind': 'argb', 'argb': '0x11000000'},
          'fontSize': 16.0,
          'fontWeight': 'w600',
          'fontStyle': 'italic',
          'height': 1.4,
          'fontFamily': 'Noto Sans',
          'decoration': ['underline', 'lineThrough'],
          'decorationColor': {'kind': 'colorScheme', 'role': 'primary'},
          'decorationStyle': 'dashed',
          'decorationThickness': 2.0,
        },
      };

    final theme = _decode(json).profile.theme;
    final body = theme.textTheme['bodyMedium']!;

    expect(theme.colorScheme['primary'], 0xff123456);
    expect((body.color! as CanvasThemeRoleColor).role, 'onSurface');
    expect((body.backgroundColor! as CanvasThemeLiteralColor).argb, 0x11000000);
    expect(body.fontSize, 16.0);
    expect(body.decoration, {'underline', 'lineThrough'});

    final unknownRole = _modelJson();
    _theme(unknownRole)['colorScheme'] = {'futureRole': '0xFF000000'};
    expect(() => _decode(unknownRole), throwsFormatException);

    final emptyStyle = _modelJson();
    _theme(emptyStyle)['textTheme'] = {'bodyMedium': <String, Object?>{}};
    expect(() => _decode(emptyStyle), throwsFormatException);
  });

  test('decodes exact light and dark project theme variants', () {
    final light = CanvasModel.decode(modelBytesForViewTest());
    expect(light.profile.theme.brightness, 'light');

    final darkJson = _modelJson();
    _theme(darkJson)
      ..['definitionId'] = 'dark'
      ..['seedArgb'] = '0xFF102030'
      ..['brightness'] = 'DARK'
      ..['digestIdentity'] = 'B' * 64;
    final dark = _decode(darkJson);
    expect(dark.profile.theme.definitionId, 'dark');
    expect(dark.profile.theme.seedArgb, 0xff102030);
    expect(dark.profile.theme.brightness, 'dark');
    expect(dark.profile.theme.digestIdentity, 'B' * 64);
  });

  test('decodes the complete flattened Text style and strut vocabulary', () {
    final model = CanvasModel.decode(expandedTextModelBytesForViewTest());
    final text = model.root
        .slot('body')!
        .child!
        .slot('children')!
        .children
        .first
        .slot('child')!
        .child!
        .slot('child')!
        .child!;

    expect(text.properties.keys, containsAll(_expandedTextProperties().keys));
    expect(text.properties['localeLanguageCode']!.value, 'zh');
    expect(text.properties['textScalerFactor']!.value, 1.25);
    expect(
      (text.properties['styleFontWeight']!.value as CanvasEnumValue).value,
      'w600',
    );
    expect(
      (text.properties['styleDecorationStyle']!.value as CanvasEnumValue).value,
      'wavy',
    );
    expect(
      (text.properties['strutLeadingDistribution']!.value as CanvasEnumValue)
          .value,
      'even',
    );
    expect(
      text.properties['styleFontFamilyFallback']!.value,
      'Noto Sans\nNoto Color Emoji\r\n',
    );
  });

  test('requires the positional Text data property', () {
    final json = _modelJson();
    _helloProperties(json).remove('data');

    expect(
      () => _decode(json),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('flutter.widgets.Text.data'),
        ),
      ),
    );
  });

  test('mirrors catalog bounds for Text font reference strings', () {
    void expectString(String name, String value, Matcher matcher) {
      final json = _modelJson();
      final properties = _helloProperties(json);
      properties[name] = {'kind': 'string', 'value': value};
      if (name.endsWith('Package') && value.isNotEmpty) {
        properties[name.replaceFirst('Package', 'FontFamily')] = {
          'kind': 'string',
          'value': 'Inter',
        };
      }
      expect(() => _decode(json), matcher, reason: '$name (${value.length})');
    }

    for (final name in const {
      'styleFontFamily',
      'stylePackage',
      'strutFontFamily',
      'strutPackage',
    }) {
      expectString(name, '', throwsFormatException);
      expectString(name, 'F' * 256, returnsNormally);
      expectString(name, 'F' * 257, throwsFormatException);
    }

    for (final name in const {
      'styleFontFamilyFallback',
      'strutFontFamilyFallback',
    }) {
      expectString(name, '', returnsNormally);
      expectString(name, 'F\n' * 2048, returnsNormally);
      expectString(name, 'F\n' * 2048 + 'x', throwsFormatException);
    }
  });

  test('accepts independent Locale subtags and rejects malformed subtags', () {
    for (final entry in const {
      'localeScriptCode': 'Hans',
      'localeCountryCode': 'CN',
      'styleLocaleScriptCode': 'Latn',
      'styleLocaleCountryCode': 'GB',
    }.entries) {
      final json = _modelJson();
      _helloProperties(json)[entry.key] = {
        'kind': 'string',
        'value': entry.value,
      };
      expect(() => _decode(json), returnsNormally, reason: entry.key);
    }

    for (final name in const {
      'localeLanguageCode',
      'localeScriptCode',
      'localeCountryCode',
      'styleLocaleLanguageCode',
      'styleLocaleScriptCode',
      'styleLocaleCountryCode',
    }) {
      final json = _modelJson();
      _helloProperties(json)[name] = {'kind': 'string', 'value': '  '};
      expect(() => _decode(json), throwsFormatException, reason: name);
    }

    for (final entry in const {
      'localeLanguageCode': 'EN',
      'localeScriptCode': 'hans',
      'localeCountryCode': 'us',
      'styleLocaleLanguageCode': 'e',
      'styleLocaleScriptCode': 'Latin',
      'styleLocaleCountryCode': 'USA',
    }.entries) {
      final json = _modelJson();
      _helloProperties(json)[entry.key] = {
        'kind': 'string',
        'value': entry.value,
      };
      expect(() => _decode(json), throwsFormatException, reason: entry.key);
    }
  });

  test('requires a usable font reference whenever a package is present', () {
    for (final prefix in const {'style', 'strut'}) {
      final missingReference = _modelJson();
      _helloProperties(missingReference)['${prefix}Package'] = {
        'kind': 'string',
        'value': 'fonts',
      };
      expect(
        () => _decode(missingReference),
        throwsFormatException,
        reason: prefix,
      );

      final familyReference = _modelJson();
      _helloProperties(familyReference).addAll({
        '${prefix}Package': {'kind': 'string', 'value': 'fonts'},
        '${prefix}FontFamily': {'kind': 'string', 'value': 'Inter'},
      });
      expect(() => _decode(familyReference), returnsNormally, reason: prefix);

      final fallbackReference = _modelJson();
      _helloProperties(fallbackReference).addAll({
        '${prefix}Package': {'kind': 'string', 'value': 'fonts'},
        '${prefix}FontFamilyFallback': {
          'kind': 'string',
          'value': '\n Noto Sans \r\n',
        },
      });
      expect(() => _decode(fallbackReference), returnsNormally, reason: prefix);
    }
  });

  test('enforces only the explicit Text constructor numeric constraints', () {
    void expectDouble(String name, double value, Matcher matcher) {
      final json = _modelJson();
      _helloProperties(json)[name] = {'kind': 'double', 'value': value};
      expect(() => _decode(json), matcher, reason: '$name=$value');
    }

    expectDouble('textScalerFactor', -0.1, throwsFormatException);
    expectDouble('textScalerFactor', 0, returnsNormally);
    expectDouble('strutFontSize', 0, throwsFormatException);
    expectDouble('strutFontSize', -1, throwsFormatException);
    expectDouble('strutFontSize', 0.1, returnsNormally);
    expectDouble('strutLeading', -0.1, throwsFormatException);
    expectDouble('strutLeading', 0, returnsNormally);
    expectDouble('styleFontSize', -0.1, throwsFormatException);
    expectDouble('styleFontSize', 0, returnsNormally);

    for (final name in const {
      'styleHeight',
      'styleDecorationThickness',
      'strutHeight',
    }) {
      expectDouble(name, -1, returnsNormally);
    }
  });

  test('rejects an unknown expanded Text enum value', () {
    final json = _modelJson();
    _helloProperties(json)['styleFontWeight'] = {
      'kind': 'enum',
      'type': 'FontWeight',
      'value': 'bold',
    };

    expect(() => _decode(json), throwsFormatException);
  });

  test('decodes typed Text theme, Paint, Shadow and OpenType values', () {
    final model = CanvasModel.decode(complexTextModelBytesForViewTest());
    final text = model.root
        .slot('body')!
        .child!
        .slot('children')!
        .children
        .first
        .slot('child')!
        .child!
        .slot('child')!
        .child!;

    expect(
      (text.properties['styleThemeTextStyle']!.value as CanvasThemeToken)
          .wireId,
      'material.textTheme.bodyLarge',
    );
    expect(
      (text.properties['selectionColor']!.value as CanvasThemeToken).wireId,
      'material.colorScheme.primary',
    );
    final foreground = text.properties['styleForeground']!.value as CanvasPaint;
    expect(foreground.blendMode, 'srcOver');
    expect(foreground.style, 'stroke');
    expect(foreground.strokeWidth, 2.5);
    expect(foreground.maskFilter!.style, 'outer');
    expect(foreground.maskFilter!.sigma, 1.5);
    expect(
      (foreground.color as CanvasThemeColor).token.wireId,
      'material.colorScheme.secondary',
    );
    final shadows =
        text.properties['styleShadows']!.value as List<CanvasShadowValue>;
    expect(shadows, hasLength(2));
    expect(shadows.first.offsetX, -1.25);
    expect(
      (shadows.first.color as CanvasThemeColor).token.wireId,
      'material.colorScheme.shadow',
    );
    final features =
        text.properties['styleFontFeatures']!.value
            as List<CanvasFontFeatureValue>;
    expect(features.map((item) => '${item.tag}:${item.value}'), [
      'liga:1',
      'kern:0',
    ]);
    final variations =
        text.properties['styleFontVariations']!.value
            as List<CanvasFontVariationValue>;
    expect(variations.map((item) => '${item.axis}:${item.value}'), [
      'wght:700.0',
      'wdth:100.0',
    ]);
  });

  test('rejects unreviewed theme roles and TextStyle paint conflicts', () {
    final unknownColor = _modelJson();
    _helloProperties(unknownColor)['selectionColor'] = {
      'kind': 'themeToken',
      'token': 'material.colorScheme.futureRole',
    };
    expect(() => _decode(unknownColor), throwsFormatException);

    final wrongDomain = _modelJson();
    _helloProperties(wrongDomain)['styleThemeTextStyle'] = {
      'kind': 'themeToken',
      'token': 'material.colorScheme.primary',
    };
    expect(() => _decode(wrongDomain), throwsFormatException);

    final conflict = _modelJson();
    _helloProperties(conflict).addAll({
      'styleColor': {'kind': 'color', 'argb': '0xFF000000'},
      'styleForeground': _paint(const {
        'kind': 'literal',
        'argb': '0xFFFFFFFF',
      }),
    });
    expect(() => _decode(conflict), throwsFormatException);
  });

  test('rejects duplicate structured identities and OpenType tags', () {
    final duplicateId = _modelJson();
    final properties = _helloProperties(duplicateId);
    final items = [
      _shadow('192489fb-3bbb-46c5-9bac-c988f412218c', '0xFF000000'),
      _shadow('192489fb-3bbb-46c5-9bac-c988f412218c', '0xFFFFFFFF'),
    ];
    properties['styleShadows'] = {'kind': 'shadowList', 'items': items};
    expect(() => _decode(duplicateId), throwsFormatException);

    final duplicateTag = _modelJson();
    _helloProperties(duplicateTag)['styleFontFeatures'] = {
      'kind': 'fontFeatureList',
      'items': [
        {
          'id': 'd8ca6ff9-1aa5-4bb1-944b-fdd475b5359d',
          'tag': 'liga',
          'value': 1,
        },
        {
          'id': '71c21a43-4662-48bb-a6ae-f59a96662692',
          'tag': 'liga',
          'value': 0,
        },
      ],
    };
    expect(() => _decode(duplicateTag), throwsFormatException);
  });
}

Uint8List modelBytesForViewTest() =>
    Uint8List.fromList(utf8.encode(jsonEncode(_modelJson())));

Uint8List expandedTextModelBytesForViewTest() {
  final json = _modelJson();
  _helloProperties(json).addAll(_expandedTextProperties());
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Uint8List complexTextModelBytesForViewTest() {
  final json = _modelJson();
  _helloProperties(json).addAll(_complexTextProperties());
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Map<String, Object?> _complexTextProperties() => {
  'selectionColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.primary',
  },
  'styleThemeTextStyle': {
    'kind': 'themeToken',
    'token': 'material.textTheme.bodyLarge',
  },
  'styleFontSize': {'kind': 'double', 'value': 21.0},
  'styleForeground': _paint(
    const {'kind': 'theme', 'token': 'material.colorScheme.secondary'},
    style: 'stroke',
    strokeWidth: 2.5,
    maskFilter: const {'style': 'outer', 'sigma': 1.5},
  ),
  'styleBackground': _paint(const {'kind': 'literal', 'argb': '0x22112233'}),
  'styleShadows': {
    'kind': 'shadowList',
    'items': [
      {
        'id': '192489fb-3bbb-46c5-9bac-c988f412218c',
        'color': {'kind': 'theme', 'token': 'material.colorScheme.shadow'},
        'offsetX': -1.25,
        'offsetY': 2.5,
        'blurRadius': 4.0,
      },
      _shadow('fc64a487-5133-46d8-9ae1-d0b5336d875b', '0x80445566'),
    ],
  },
  'styleFontFeatures': {
    'kind': 'fontFeatureList',
    'items': [
      {'id': 'd8ca6ff9-1aa5-4bb1-944b-fdd475b5359d', 'tag': 'liga', 'value': 1},
      {'id': '71c21a43-4662-48bb-a6ae-f59a96662692', 'tag': 'kern', 'value': 0},
    ],
  },
  'styleFontVariations': {
    'kind': 'fontVariationList',
    'items': [
      {
        'id': '2115406c-c05d-4323-81a2-7cfe7ea35dc4',
        'axis': 'wght',
        'value': 700.0,
      },
      {
        'id': '677dad17-6648-43fd-a803-d3fa6649b34f',
        'axis': 'wdth',
        'value': 100.0,
      },
    ],
  },
  'styleDecorationColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.error',
  },
};

Map<String, Object?> _paint(
  Map<String, Object?> color, {
  String style = 'fill',
  double strokeWidth = 0,
  Map<String, Object?>? maskFilter,
}) => {
  'kind': 'paint',
  'color': color,
  'blendMode': 'srcOver',
  'style': style,
  'strokeWidth': strokeWidth,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
  'maskFilter': ?maskFilter,
};

Map<String, Object?> _shadow(String id, String argb) => {
  'id': id,
  'color': {'kind': 'literal', 'argb': argb},
  'offsetX': 0,
  'offsetY': 1,
  'blurRadius': 2,
};

Map<String, Object?> _expandedTextProperties() => {
  'localeLanguageCode': {'kind': 'string', 'value': 'zh'},
  'localeScriptCode': {'kind': 'string', 'value': 'Hans'},
  'localeCountryCode': {'kind': 'string', 'value': 'CN'},
  'textScalerFactor': {'kind': 'double', 'value': 1.25},
  'textHeightApplyFirstAscent': {'kind': 'boolean', 'value': false},
  'textHeightApplyLastDescent': {'kind': 'boolean', 'value': false},
  'textHeightLeadingDistribution': {
    'kind': 'enum',
    'type': 'TextLeadingDistribution',
    'value': 'even',
  },
  'styleInherit': {'kind': 'boolean', 'value': false},
  'styleColor': {'kind': 'color', 'argb': '0xFF102030'},
  'styleBackgroundColor': {'kind': 'color', 'argb': '0xFFE0D0C0'},
  'styleFontSize': {'kind': 'double', 'value': 18.5},
  'styleFontWeight': {'kind': 'enum', 'type': 'FontWeight', 'value': 'w600'},
  'styleFontStyle': {'kind': 'enum', 'type': 'FontStyle', 'value': 'italic'},
  'styleLetterSpacing': {'kind': 'double', 'value': 1.25},
  'styleWordSpacing': {'kind': 'double', 'value': 2.5},
  'styleTextBaseline': {
    'kind': 'enum',
    'type': 'TextBaseline',
    'value': 'ideographic',
  },
  'styleHeight': {'kind': 'double', 'value': 1.4},
  'styleLeadingDistribution': {
    'kind': 'enum',
    'type': 'TextLeadingDistribution',
    'value': 'proportional',
  },
  'styleLocaleLanguageCode': {'kind': 'string', 'value': 'en'},
  'styleLocaleScriptCode': {'kind': 'string', 'value': 'Latn'},
  'styleLocaleCountryCode': {'kind': 'string', 'value': 'GB'},
  'styleDecorationUnderline': {'kind': 'boolean', 'value': true},
  'styleDecorationOverline': {'kind': 'boolean', 'value': true},
  'styleDecorationLineThrough': {'kind': 'boolean', 'value': true},
  'styleDecorationColor': {'kind': 'color', 'argb': '0xFF112233'},
  'styleDecorationStyle': {
    'kind': 'enum',
    'type': 'TextDecorationStyle',
    'value': 'wavy',
  },
  'styleDecorationThickness': {'kind': 'double', 'value': 2.25},
  'styleDebugLabel': {'kind': 'string', 'value': 'designer text'},
  'styleFontFamily': {'kind': 'string', 'value': 'Inter'},
  'styleFontFamilyFallback': {
    'kind': 'string',
    'value': 'Noto Sans\nNoto Color Emoji\r\n',
  },
  'stylePackage': {'kind': 'string', 'value': 'design_fonts'},
  'styleOverflow': {'kind': 'enum', 'type': 'TextOverflow', 'value': 'fade'},
  'strutFontFamily': {'kind': 'string', 'value': 'Roboto'},
  'strutFontFamilyFallback': {
    'kind': 'string',
    'value': 'Noto Sans\n Noto Serif ',
  },
  'strutFontSize': {'kind': 'double', 'value': 16.0},
  'strutHeight': {'kind': 'double', 'value': 1.2},
  'strutLeadingDistribution': {
    'kind': 'enum',
    'type': 'TextLeadingDistribution',
    'value': 'even',
  },
  'strutLeading': {'kind': 'double', 'value': 0.3},
  'strutFontWeight': {'kind': 'enum', 'type': 'FontWeight', 'value': 'w500'},
  'strutFontStyle': {'kind': 'enum', 'type': 'FontStyle', 'value': 'normal'},
  'strutForceHeight': {'kind': 'boolean', 'value': true},
  'strutDebugLabel': {'kind': 'string', 'value': 'designer strut'},
  'strutPackage': {'kind': 'string', 'value': 'metric_fonts'},
};

Map<String, Object?> _modelJson() => {
  'format': 'netbeans-flutter-canvas-model',
  'protocolVersion': 5,
  'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
  'presentationSequence': 4,
  'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
  'logicalRevisionId': 2,
  'profile': {
    'previewMode': 'MOBILE',
    'targetPlatform': 'WINDOWS',
    'logicalWidth': 390,
    'logicalHeight': 844,
    'devicePixelRatio': 1,
    'theme': {
      'definitionId': 'light',
      'seedArgb': '0xFF6750A4',
      'brightness': 'LIGHT',
      'digestIdentity': 'A' * 64,
      'colorScheme': <String, Object?>{},
      'textTheme': <String, Object?>{},
    },
    'locale': 'en-US',
    'textScaleFactor': 1,
  },
  'root': _node(
    '6e88bff4-8d73-48aa-92b5-87aa3344f6a7',
    'flutter.material.Scaffold',
    properties: {
      'backgroundColor': {'kind': 'color', 'argb': '0xFFFFFFFF'},
      'resizeToAvoidBottomInset': {'kind': 'boolean', 'value': true},
    },
    slots: {
      'body': _single(
        _node(
          '4c04dc44-7381-4ca8-826f-8c23bc2351d1',
          'flutter.widgets.Column',
          properties: {
            'mainAxisAlignment': {
              'kind': 'enum',
              'type': 'MainAxisAlignment',
              'value': 'center',
            },
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
            'crossAxisAlignment': {
              'kind': 'enum',
              'type': 'CrossAxisAlignment',
              'value': 'baseline',
            },
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': 'ltr',
            },
            'verticalDirection': {
              'kind': 'enum',
              'type': 'VerticalDirection',
              'value': 'down',
            },
            'textBaseline': {
              'kind': 'enum',
              'type': 'TextBaseline',
              'value': 'alphabetic',
            },
            'spacing': {'kind': 'double', 'value': 12.5},
          },
          slots: {
            'children': _list([
              _node(
                '0f78bed5-2fba-42cd-914e-a3abbd2c48c3',
                'flutter.widgets.Padding',
                properties: {
                  'padding': {
                    'kind': 'edgeInsets',
                    'left': 16,
                    'top': 16,
                    'right': 16,
                    'bottom': 16,
                  },
                },
                slots: {
                  'child': _single(
                    _node(
                      '733ef462-41d7-4849-b780-5d9520666ae4',
                      'flutter.widgets.Center',
                      properties: {
                        'widthFactor': {'kind': 'double', 'value': 1.0},
                      },
                      slots: {
                        'child': _single(
                          _node(
                            '5ab6c203-3d32-489c-9d7a-7c14f29637cb',
                            'flutter.widgets.Text',
                            properties: {
                              'data': {'kind': 'string', 'value': 'Hello'},
                              'textAlign': {
                                'kind': 'enum',
                                'type': 'TextAlign',
                                'value': 'center',
                              },
                              'textDirection': {
                                'kind': 'enum',
                                'type': 'TextDirection',
                                'value': 'ltr',
                              },
                              'softWrap': {'kind': 'boolean', 'value': false},
                              'overflow': {
                                'kind': 'enum',
                                'type': 'TextOverflow',
                                'value': 'ellipsis',
                              },
                              'maxLines': {'kind': 'integer', 'value': 2},
                              'semanticsLabel': {
                                'kind': 'string',
                                'value': 'Primary greeting',
                              },
                              'semanticsIdentifier': {
                                'kind': 'string',
                                'value': 'primary-greeting',
                              },
                              'textWidthBasis': {
                                'kind': 'enum',
                                'type': 'TextWidthBasis',
                                'value': 'longestLine',
                              },
                              'selectionColor': {
                                'kind': 'color',
                                'argb': '0xFF336699',
                              },
                            },
                          ),
                        ),
                      },
                    ),
                  ),
                },
              ),
              _node(
                '1035b7df-df9b-442b-9af2-72b4c90f1462',
                'flutter.widgets.Row',
                properties: {
                  'mainAxisAlignment': {
                    'kind': 'enum',
                    'type': 'MainAxisAlignment',
                    'value': 'end',
                  },
                  'mainAxisSize': {
                    'kind': 'enum',
                    'type': 'MainAxisSize',
                    'value': 'min',
                  },
                  'crossAxisAlignment': {
                    'kind': 'enum',
                    'type': 'CrossAxisAlignment',
                    'value': 'baseline',
                  },
                  'textDirection': {
                    'kind': 'enum',
                    'type': 'TextDirection',
                    'value': 'rtl',
                  },
                  'verticalDirection': {
                    'kind': 'enum',
                    'type': 'VerticalDirection',
                    'value': 'up',
                  },
                  'textBaseline': {
                    'kind': 'enum',
                    'type': 'TextBaseline',
                    'value': 'ideographic',
                  },
                  'spacing': {'kind': 'double', 'value': 4.0},
                },
                slots: {
                  'children': _list([
                    _node(
                      '64260967-f830-4e3c-bbd1-f81cb79db092',
                      'flutter.widgets.Text',
                      properties: {
                        'data': {'kind': 'string', 'value': 'World'},
                        'semanticsIdentifier': {
                          'kind': 'string',
                          'value': 'secondary-greeting',
                        },
                      },
                    ),
                  ]),
                },
              ),
            ]),
          },
        ),
      ),
      'floatingActionButton': _single(
        _node(
          'd341f7e9-c3d5-47f2-a91d-25963065f9f8',
          'flutter.widgets.Text',
          properties: {
            'data': {'kind': 'string', 'value': 'Action'},
            'semanticsIdentifier': {
              'kind': 'string',
              'value': 'floating-action-label',
            },
          },
        ),
      ),
    },
  ),
};

Map<String, Object?> _theme(Map<String, Object?> model) {
  final profile = model['profile']! as Map<String, Object?>;
  final theme = Map<String, Object?>.from(
    profile['theme']! as Map<String, Object?>,
  );
  profile['theme'] = theme;
  return theme;
}

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?> slots = const {},
}) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _single(Map<String, Object?>? child) => {
  'kind': 'single',
  'child': child,
};

Map<String, Object?> _list(List<Map<String, Object?>> children) => {
  'kind': 'list',
  'children': children,
};

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));

Map<String, Object?> _column(Map<String, Object?> json) {
  final root = json['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  return body['child']! as Map<String, Object?>;
}

Map<String, Object?> _paddingNode(Map<String, Object?> json) {
  final column = _column(json);
  final childrenSlot =
      (column['slots']! as Map<String, Object?>)['children']!
          as Map<String, Object?>;
  return (childrenSlot['children']! as List<Object?>).first
      as Map<String, Object?>;
}

Map<String, Object?> _centerNode(Map<String, Object?> json) {
  final padding = _paddingNode(json);
  final childSlot =
      (padding['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  return childSlot['child']! as Map<String, Object?>;
}

Map<String, Object?> _paddingProperties(Map<String, Object?> json) =>
    _paddingNode(json)['properties']! as Map<String, Object?>;

Map<String, Object?> _centerProperties(Map<String, Object?> json) =>
    _centerNode(json)['properties']! as Map<String, Object?>;

CanvasNode _decodedPadding(CanvasModel model) =>
    model.root.slot('body')!.child!.slot('children')!.children.first;

CanvasNode _decodedCenter(CanvasModel model) =>
    _decodedPadding(model).slot('child')!.child!;

Map<String, Object?> _helloProperties(Map<String, Object?> json) =>
    _textNodes(_column(json)).first['properties']! as Map<String, Object?>;

List<Map<String, Object?>> _textNodes(Map<String, Object?> node) {
  final result = <Map<String, Object?>>[];
  void visit(Map<String, Object?> current) {
    if (current['type'] == 'flutter.widgets.Text') {
      result.add(current);
    }
    final slots = current['slots']! as Map<String, Object?>;
    for (final rawSlot in slots.values) {
      final slot = rawSlot! as Map<String, Object?>;
      if (slot['kind'] == 'single') {
        final child = slot['child'];
        if (child is Map<String, Object?>) {
          visit(child);
        }
      } else {
        for (final child in slot['children']! as List<Object?>) {
          visit(child! as Map<String, Object?>);
        }
      }
    }
  }

  visit(node);
  return result;
}
