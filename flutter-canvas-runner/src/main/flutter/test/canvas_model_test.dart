import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

void main() {
  test('Canvas model protocol v18 is exact and rejects v17 payloads', () {
    expect(canvasModelProtocolVersion, 18);
    expect(() => _decode(_modelJson()), returnsNormally);

    final oldProtocol = _modelJson()..['protocolVersion'] = 17;
    expect(() => _decode(oldProtocol), throwsFormatException);
  });

  test('reviewed widget schema matches runtime metadata exactly', () {
    expect(canvasReviewedWidgetSchemaContract, startsWith('\n'));
    expect(
      canvasReviewedWidgetSchemaContract.substring(1),
      canvasRuntimeWidgetSchemaContractForTesting(),
    );
  });

  test('decodes every reviewed Scaffold scalar with exact closed types', () {
    final json = _modelJson();
    final root = json['root']! as Map<String, Object?>;
    root['properties'] = <String, Object?>{
      'floatingActionButtonLocation': {
        'kind': 'string',
        'value': 'miniCenterDocked',
      },
      'floatingActionButtonAnimator': {
        'kind': 'string',
        'value': 'noAnimation',
      },
      'persistentFooterAlignment': {'kind': 'string', 'value': 'bottomStart'},
      'onDrawerChanged': {'kind': 'callbackPresence'},
      'onEndDrawerChanged': {'kind': 'callbackPresence'},
      'backgroundColor': {
        'kind': 'themeToken',
        'token': 'material.colorScheme.surface',
      },
      'resizeToAvoidBottomInset': {'kind': 'boolean', 'value': false},
      'primary': {'kind': 'boolean', 'value': false},
      'drawerDragStartBehavior': {
        'kind': 'enum',
        'type': 'DragStartBehavior',
        'value': 'down',
      },
      'extendBody': {'kind': 'boolean', 'value': true},
      'drawerBarrierDismissible': {'kind': 'boolean', 'value': false},
      'extendBodyBehindAppBar': {'kind': 'boolean', 'value': true},
      'drawerScrimColor': {'kind': 'color', 'argb': '0x80112233'},
      'drawerEdgeDragWidth': {'kind': 'double', 'value': 24.5},
      'drawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'endDrawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'restorationId': {'kind': 'string', 'value': 'home-scaffold'},
    };

    final scaffold = _decode(json).root;

    expect(scaffold.properties, hasLength(17));
    expect(
      scaffold.properties['floatingActionButtonLocation']!.value,
      'miniCenterDocked',
    );
    expect(
      scaffold.properties['floatingActionButtonAnimator']!.value,
      'noAnimation',
    );
    expect(
      scaffold.properties['persistentFooterAlignment']!.value,
      'bottomStart',
    );
    expect(scaffold.properties['onDrawerChanged']!.kind, 'callbackPresence');
    expect(scaffold.properties['onEndDrawerChanged']!.value, isTrue);
    expect(
      (scaffold.properties['backgroundColor']!.value as CanvasThemeToken)
          .wireId,
      'material.colorScheme.surface',
    );
    final drag =
        scaffold.properties['drawerDragStartBehavior']!.value
            as CanvasEnumValue;
    expect(drag.type, 'DragStartBehavior');
    expect(drag.value, 'down');
    expect(scaffold.properties['drawerEdgeDragWidth']!.value, 24.5);
    expect(scaffold.properties['restorationId']!.value, 'home-scaffold');
  });

  test('rejects Scaffold values outside reviewed presets and bounds', () {
    Map<String, Object?> invalid(String name, Map<String, Object?> value) {
      final json = _modelJson();
      final root = json['root']! as Map<String, Object?>;
      root['properties'] = <String, Object?>{name: value};
      return json;
    }

    for (final json in <Map<String, Object?>>[
      invalid('floatingActionButtonLocation', {
        'kind': 'string',
        'value': 'custom',
      }),
      invalid('floatingActionButtonAnimator', {
        'kind': 'string',
        'value': 'custom',
      }),
      invalid('persistentFooterAlignment', {'kind': 'string', 'value': 'left'}),
      invalid('drawerDragStartBehavior', {
        'kind': 'enum',
        'type': 'DragStartBehavior',
        'value': 'move',
      }),
      invalid('drawerEdgeDragWidth', {'kind': 'integer', 'value': -1}),
      invalid('restorationId', {'kind': 'string', 'value': ''}),
    ]) {
      expect(() => _decode(json), throwsFormatException);
    }
  });

  test('AppBar reviewed below-type contract matches Java fingerprint', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.material.AppBar\n');
    final end = contract.indexOf('W|flutter.material.Badge\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final bytes = utf8.encode(contract.substring(start, end));
    expect(bytes, hasLength(50905));
    expect(
      sha256Hex(bytes),
      'efcbcdee37b660a8ec4f8cb85b152aa92009033b6ae498b6c25861d7efbdb3be',
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

  test('decodes the exact optional Align contract and child slot', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '5f60e614-e995-4948-bdf6-d5575575f7ee',
        'flutter.widgets.Align',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      '81122682-3f99-4d39-9c0f-edde405c7d13',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Aligned child'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'alignment': _canvasAlignment(
            basis: 'directional',
            horizontal: 2.25,
            vertical: -1.5,
          ),
          'widthFactor': {'kind': 'integer', 'value': 0},
          'heightFactor': {'kind': 'double', 'value': 2.5},
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.Align');
    expect(decoded.properties.keys, const [
      'alignment',
      'widthFactor',
      'heightFactor',
    ]);
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, 2.25);
    expect(alignment.vertical, -1.5);
    expect(decoded.properties['widthFactor']!.value, 0);
    expect(decoded.properties['heightFactor']!.value, 2.5);
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);

    final explicitEmpty = _decode(model(slots: {'child': _single(null)})).root;
    expect(explicitEmpty.slot('child')!.child, isNull);

    final physical = _decode(
      model(
        properties: {
          'alignment': _canvasAlignment(horizontal: -3, vertical: 4),
          'widthFactor': {'kind': 'double', 'value': 0.0},
          'heightFactor': {'kind': 'integer', 'value': 1},
        },
      ),
    ).root;
    final physicalAlignment =
        physical.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(physicalAlignment.basis, 'physical');
    expect(physicalAlignment.horizontal, -3);
    expect(physicalAlignment.vertical, 4);
  });

  test('rejects values outside the reviewed Align projection', () {
    Map<String, Object?> model(Map<String, Object?> properties) {
      final json = _modelJson();
      json['root'] = _node(
        '5f60e614-e995-4948-bdf6-d5575575f7ee',
        'flutter.widgets.Align',
        properties: properties,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'widthFactor': {'kind': 'integer', 'value': -1},
      },
      const {
        'heightFactor': {'kind': 'double', 'value': -0.0001},
      },
      const {
        'widthFactor': {'kind': 'boolean', 'value': true},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'center'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'integer', 'value': 0},
      },
    ]) {
      expect(
        () => _decode(model(properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
  });

  test('Align reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Align\n');
    final end = contract.indexOf('W|flutter.widgets.AspectRatio\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Align\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|heightFactor|double,integer|0|-|'
      'double:0:1:*:1;integer:0:1:9007199254740991:1|'
      'double:range:0:1:*:1;integer:range:0:1:9007199254740991:1\n'
      'P|widthFactor|double,integer|0|-|'
      'double:0:1:*:1;integer:0:1:9007199254740991:1|'
      'double:range:0:1:*:1;integer:range:0:1:9007199254740991:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'decodes the exact optional FractionallySizedBox contract and child slot',
    () {
      Map<String, Object?> model({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '2276af62-761d-43b2-95c5-47da42403bb7',
          'flutter.widgets.FractionallySizedBox',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final child = _node(
        'ab1460a1-0659-4793-b42f-14e0f4f07059',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Fractional child'},
        },
      );
      final decoded = _decode(
        model(
          properties: {
            'alignment': _canvasAlignment(
              basis: 'directional',
              horizontal: 2.25,
              vertical: -1.5,
            ),
            'widthFactor': {'kind': 'integer', 'value': 0},
            'heightFactor': {'kind': 'double', 'value': 2.5},
          },
          slots: {'child': _single(child)},
        ),
      ).root;

      expect(decoded.type, 'flutter.widgets.FractionallySizedBox');
      expect(decoded.properties.keys, const [
        'alignment',
        'widthFactor',
        'heightFactor',
      ]);
      final alignment =
          decoded.properties['alignment']!.value
              as CanvasAlignmentGeometryValue;
      expect(alignment.basis, 'directional');
      expect(alignment.horizontal, 2.25);
      expect(alignment.vertical, -1.5);
      expect(decoded.properties['widthFactor']!.value, 0);
      expect(decoded.properties['heightFactor']!.value, 2.5);
      expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

      final omitted = _decode(model()).root;
      expect(omitted.properties, isEmpty);
      expect(omitted.slot('child'), isNull);

      final explicitEmpty = _decode(
        model(slots: {'child': _single(null)}),
      ).root;
      expect(explicitEmpty.slot('child')!.child, isNull);

      final physical = _decode(
        model(
          properties: {
            'alignment': _canvasAlignment(horizontal: -3, vertical: 4),
            'widthFactor': {'kind': 'double', 'value': 0.0},
            'heightFactor': {'kind': 'integer', 'value': 1},
          },
        ),
      ).root;
      final physicalAlignment =
          physical.properties['alignment']!.value
              as CanvasAlignmentGeometryValue;
      expect(physicalAlignment.basis, 'physical');
      expect(physicalAlignment.horizontal, -3);
      expect(physicalAlignment.vertical, 4);
    },
  );

  test('rejects values outside the FractionallySizedBox projection', () {
    Map<String, Object?> model(Map<String, Object?> properties) {
      final json = _modelJson();
      json['root'] = _node(
        '2276af62-761d-43b2-95c5-47da42403bb7',
        'flutter.widgets.FractionallySizedBox',
        properties: properties,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'widthFactor': {'kind': 'integer', 'value': -1},
      },
      const {
        'heightFactor': {'kind': 'double', 'value': -0.0001},
      },
      const {
        'widthFactor': {'kind': 'boolean', 'value': true},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'center'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'integer', 'value': 0},
      },
    ]) {
      expect(
        () => _decode(model(properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
  });

  test('FractionallySizedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.FractionallySizedBox\n');
    final end = contract.indexOf('W|flutter.widgets.GridView\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.FractionallySizedBox\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|heightFactor|double,integer|0|-|'
      'double:0:1:*:1;integer:0:1:9007199254740991:1|'
      'double:range:0:1:*:1;integer:range:0:1:9007199254740991:1\n'
      'P|widthFactor|double,integer|0|-|'
      'double:0:1:*:1;integer:0:1:9007199254740991:1|'
      'double:range:0:1:*:1;integer:range:0:1:9007199254740991:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes every optional FittedBox leaf and its child slot', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'b587a092-9a65-420a-9d1c-e127cc752f8d',
        'flutter.widgets.FittedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      'feea5045-2de2-4d34-b08d-79e5ee710f7e',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Fitted child'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'cover'},
          'alignment': _canvasAlignment(
            basis: 'directional',
            horizontal: 1.5,
            vertical: -0.5,
          ),
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAliasWithSaveLayer',
          },
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.FittedBox');
    expect(decoded.properties.keys, const ['fit', 'alignment', 'clipBehavior']);
    expect(
      (decoded.properties['fit']!.value as CanvasEnumValue).value,
      'cover',
    );
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, 1.5);
    expect(alignment.vertical, -0.5);
    expect(
      (decoded.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAliasWithSaveLayer',
    );
    expect(decoded.slot('child')!.child!.id, child['id']);

    expect(_decode(model()).root.slot('child'), isNull);
    expect(
      _decode(model(slots: {'child': _single(null)})).root.slot('child')!.child,
      isNull,
    );
  });

  test('rejects values outside the reviewed FittedBox projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'b587a092-9a65-420a-9d1c-e127cc752f8d',
        'flutter.widgets.FittedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'stretch'},
      },
      const {
        'fit': {'kind': 'enum', 'type': 'ImageRepeat', 'value': 'repeat'},
      },
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'center'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
  });

  test('FittedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.FittedBox\n');
    final end = contract.indexOf('W|flutter.widgets.Flexible\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.FittedBox\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|fit|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the exact ClipOval clip and optional-child contract', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '91684e03-f63a-4cde-b899-7a50c888f9e7',
        'flutter.widgets.ClipOval',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      '9f5350ee-598c-4664-9cdd-11e344d0f4e0',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Oval-clipped child'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAliasWithSaveLayer',
          },
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.ClipOval');
    expect(decoded.properties.keys, const ['clipBehavior']);
    expect(
      (decoded.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAliasWithSaveLayer',
    );
    expect(decoded.slot('child')!.child!.id, child['id']);

    final customClipperNode = _decode(
      model(
        properties: const {
          'clipper': {'kind': 'dartObjectReferencePresence'},
        },
      ),
    ).root;
    expect(
      customClipperNode.properties['clipper']!.kind,
      'dartObjectReferencePresence',
    );
    expect(customClipperNode.properties['clipper']!.value, isTrue);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);
    final explicitEmpty = _decode(model(slots: {'child': _single(null)})).root;
    expect(explicitEmpty.slot('child')!.child, isNull);
  });

  test('rejects every malformed or out-of-contract ClipOval branch', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '91684e03-f63a-4cde-b899-7a50c888f9e7',
        'flutter.widgets.ClipOval',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'clipBehavior': {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'none',
        },
      },
      const {
        'clipBehavior': {'kind': 'string', 'value': 'antiAlias'},
      },
      const {
        'clipper': {'kind': 'string', 'value': 'custom'},
      },
      const {
        'futureProperty': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
    expect(
      () => _decode(model(slots: {'futureSlot': _single(null)})),
      throwsFormatException,
    );
    final expanded = _node(
      'c7ee4f80-b95d-4870-864f-a6d2da74c79e',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            'ee3d36a6-2b38-485f-8d0a-9af46d05592f',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under ClipOval',
    );
  });

  test('ClipOval reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ClipOval\n');
    final end = contract.indexOf('W|flutter.widgets.ClipPath\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ClipOval\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|clipper|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:CustomClipper<Rect>:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes exact physical and directional ClipRRect radius geometry', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '5582922d-9044-4e78-bba8-bb740884a49d',
        'flutter.widgets.ClipRRect',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      '68d29542-ab18-41fb-a509-8db0451aed8e',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Rounded-clipped child'},
      },
    );
    final physicalNode = _decode(
      model(
        properties: const {
          'borderRadius': {
            'kind': 'borderRadius',
            'geometry': {
              'kind': 'physical',
              'topLeft': {'x': 2.0, 'y': 3.0},
              'topRight': {'x': 4.0, 'y': 5.0},
              'bottomRight': {'x': 6.0, 'y': 7.0},
              'bottomLeft': {'x': 8.0, 'y': 9.0},
            },
          },
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAliasWithSaveLayer',
          },
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(physicalNode.type, 'flutter.widgets.ClipRRect');
    expect(physicalNode.properties.keys, const [
      'borderRadius',
      'clipBehavior',
    ]);
    final physical =
        physicalNode.properties['borderRadius']!.value
            as CanvasPhysicalBorderRadiusValue;
    expect(physical.topLeft.x, 2);
    expect(physical.topLeft.y, 3);
    expect(physical.topRight.x, 4);
    expect(physical.topRight.y, 5);
    expect(physical.bottomRight.x, 6);
    expect(physical.bottomRight.y, 7);
    expect(physical.bottomLeft.x, 8);
    expect(physical.bottomLeft.y, 9);
    expect(
      (physicalNode.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAliasWithSaveLayer',
    );
    expect(physicalNode.slot('child')!.child!.id, child['id']);

    final directionalNode = _decode(
      model(
        properties: const {
          'borderRadius': {
            'kind': 'borderRadius',
            'geometry': {
              'kind': 'directional',
              'topStart': {'x': 10.0, 'y': 11.0},
              'topEnd': {'x': 12.0, 'y': 13.0},
              'bottomEnd': {'x': 14.0, 'y': 15.0},
              'bottomStart': {'x': 16.0, 'y': 17.0},
            },
          },
        },
      ),
    ).root;
    final directional =
        directionalNode.properties['borderRadius']!.value
            as CanvasDirectionalBorderRadiusValue;
    expect(directional.topStart.x, 10);
    expect(directional.topStart.y, 11);
    expect(directional.topEnd.x, 12);
    expect(directional.topEnd.y, 13);
    expect(directional.bottomEnd.x, 14);
    expect(directional.bottomEnd.y, 15);
    expect(directional.bottomStart.x, 16);
    expect(directional.bottomStart.y, 17);

    final customClipperNode = _decode(
      model(
        properties: const {
          'clipper': {'kind': 'dartObjectReferencePresence'},
        },
      ),
    ).root;
    expect(
      customClipperNode.properties['clipper']!.kind,
      'dartObjectReferencePresence',
    );
    expect(customClipperNode.properties['clipper']!.value, isTrue);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);
    final explicitEmpty = _decode(model(slots: {'child': _single(null)})).root;
    expect(explicitEmpty.slot('child')!.child, isNull);
  });

  test('rejects malformed and unsupported ClipRRect branches', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '5582922d-9044-4e78-bba8-bb740884a49d',
        'flutter.widgets.ClipRRect',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'clipBehavior': {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'none',
        },
      },
      const {
        'clipBehavior': {'kind': 'string', 'value': 'antiAlias'},
      },
      const {
        'borderRadius': {'kind': 'boxDecoration', 'geometry': null},
      },
      const {
        'borderRadius': {'kind': 'borderRadius'},
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {'kind': 'custom'},
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'physical',
            'topLeft': {'x': 1.0, 'y': 1.0},
            'topRight': {'x': 2.0, 'y': 2.0},
            'bottomRight': {'x': 3.0, 'y': 3.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'physical',
            'topLeft': {'x': -1.0, 'y': 1.0},
            'topRight': {'x': 2.0, 'y': 2.0},
            'bottomRight': {'x': 3.0, 'y': 3.0},
            'bottomLeft': {'x': 4.0, 'y': 4.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'directional',
            'topStart': {'x': 1.0, 'y': 1.0},
            'topEnd': {'x': 2.0, 'y': 2.0},
            'bottomEnd': {'x': 3.0, 'y': '3'},
            'bottomStart': {'x': 4.0, 'y': 4.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'directional',
            'topStart': {'x': 1.0, 'y': 1.0},
            'topEnd': {'x': 2.0, 'y': 2.0},
            'bottomEnd': {'x': 3.0, 'y': 3.0},
            'bottomStart': {'x': 4.0, 'y': 4.0, 'z': 5.0},
          },
        },
      },
      const {
        'clipper': {'kind': 'string', 'value': 'custom'},
      },
      const {
        'clipper': {'kind': 'callbackPresence'},
      },
      const {
        'clipper': {
          'kind': 'dartObjectReferencePresence',
          'symbol': 'mustNotCrossTheCanvasBoundary',
        },
      },
      const {
        'futureProperty': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
    expect(
      () => _decode(model(slots: {'futureSlot': _single(null)})),
      throwsFormatException,
    );
    final expanded = _node(
      '5b833e24-0a85-48d7-8663-8333adffb829',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            'dd999db4-d619-42bc-80da-81368b43935c',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under ClipRRect',
    );
  });

  test('ClipRRect reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ClipRRect\n');
    final end = contract.indexOf(
      'W|flutter.widgets.ClipRSuperellipse\n',
      start,
    );
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ClipRRect\n'
      'P|borderRadius|borderRadius|0|-|-|'
      'borderRadius:borderRadius:v1:physical,directional:finiteNonNegative\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|clipper|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:CustomClipper<RRect>:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'decodes exact physical and directional ClipRSuperellipse radius geometry',
    () {
      Map<String, Object?> model({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '5582922d-9044-4e78-bba8-bb740884a49d',
          'flutter.widgets.ClipRSuperellipse',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final child = _node(
        '68d29542-ab18-41fb-a509-8db0451aed8e',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Rounded-clipped child'},
        },
      );
      final physicalNode = _decode(
        model(
          properties: const {
            'borderRadius': {
              'kind': 'borderRadius',
              'geometry': {
                'kind': 'physical',
                'topLeft': {'x': 2.0, 'y': 3.0},
                'topRight': {'x': 4.0, 'y': 5.0},
                'bottomRight': {'x': 6.0, 'y': 7.0},
                'bottomLeft': {'x': 8.0, 'y': 9.0},
              },
            },
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': 'antiAliasWithSaveLayer',
            },
          },
          slots: {'child': _single(child)},
        ),
      ).root;

      expect(physicalNode.type, 'flutter.widgets.ClipRSuperellipse');
      expect(physicalNode.properties.keys, const [
        'borderRadius',
        'clipBehavior',
      ]);
      final physical =
          physicalNode.properties['borderRadius']!.value
              as CanvasPhysicalBorderRadiusValue;
      expect(physical.topLeft.x, 2);
      expect(physical.topLeft.y, 3);
      expect(physical.topRight.x, 4);
      expect(physical.topRight.y, 5);
      expect(physical.bottomRight.x, 6);
      expect(physical.bottomRight.y, 7);
      expect(physical.bottomLeft.x, 8);
      expect(physical.bottomLeft.y, 9);
      expect(
        (physicalNode.properties['clipBehavior']!.value as CanvasEnumValue)
            .value,
        'antiAliasWithSaveLayer',
      );
      expect(physicalNode.slot('child')!.child!.id, child['id']);

      final directionalNode = _decode(
        model(
          properties: const {
            'borderRadius': {
              'kind': 'borderRadius',
              'geometry': {
                'kind': 'directional',
                'topStart': {'x': 10.0, 'y': 11.0},
                'topEnd': {'x': 12.0, 'y': 13.0},
                'bottomEnd': {'x': 14.0, 'y': 15.0},
                'bottomStart': {'x': 16.0, 'y': 17.0},
              },
            },
          },
        ),
      ).root;
      final directional =
          directionalNode.properties['borderRadius']!.value
              as CanvasDirectionalBorderRadiusValue;
      expect(directional.topStart.x, 10);
      expect(directional.topStart.y, 11);
      expect(directional.topEnd.x, 12);
      expect(directional.topEnd.y, 13);
      expect(directional.bottomEnd.x, 14);
      expect(directional.bottomEnd.y, 15);
      expect(directional.bottomStart.x, 16);
      expect(directional.bottomStart.y, 17);

      final customClipperNode = _decode(
        model(
          properties: const {
            'clipper': {'kind': 'dartObjectReferencePresence'},
          },
        ),
      ).root;
      expect(
        customClipperNode.properties['clipper']!.kind,
        'dartObjectReferencePresence',
      );
      expect(customClipperNode.properties['clipper']!.value, isTrue);

      final omitted = _decode(model()).root;
      expect(omitted.properties, isEmpty);
      expect(omitted.slot('child'), isNull);
      final explicitEmpty = _decode(
        model(slots: {'child': _single(null)}),
      ).root;
      expect(explicitEmpty.slot('child')!.child, isNull);
    },
  );

  test('rejects malformed and unsupported ClipRSuperellipse branches', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '5582922d-9044-4e78-bba8-bb740884a49d',
        'flutter.widgets.ClipRSuperellipse',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'clipBehavior': {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'none',
        },
      },
      const {
        'clipBehavior': {'kind': 'string', 'value': 'antiAlias'},
      },
      const {
        'borderRadius': {'kind': 'boxDecoration', 'geometry': null},
      },
      const {
        'borderRadius': {'kind': 'borderRadius'},
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {'kind': 'custom'},
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'physical',
            'topLeft': {'x': 1.0, 'y': 1.0},
            'topRight': {'x': 2.0, 'y': 2.0},
            'bottomRight': {'x': 3.0, 'y': 3.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'physical',
            'topLeft': {'x': -1.0, 'y': 1.0},
            'topRight': {'x': 2.0, 'y': 2.0},
            'bottomRight': {'x': 3.0, 'y': 3.0},
            'bottomLeft': {'x': 4.0, 'y': 4.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'directional',
            'topStart': {'x': 1.0, 'y': 1.0},
            'topEnd': {'x': 2.0, 'y': 2.0},
            'bottomEnd': {'x': 3.0, 'y': '3'},
            'bottomStart': {'x': 4.0, 'y': 4.0},
          },
        },
      },
      const {
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'directional',
            'topStart': {'x': 1.0, 'y': 1.0},
            'topEnd': {'x': 2.0, 'y': 2.0},
            'bottomEnd': {'x': 3.0, 'y': 3.0},
            'bottomStart': {'x': 4.0, 'y': 4.0, 'z': 5.0},
          },
        },
      },
      const {
        'clipper': {'kind': 'string', 'value': 'custom'},
      },
      const {
        'clipper': {'kind': 'callbackPresence'},
      },
      const {
        'clipper': {
          'kind': 'dartObjectReferencePresence',
          'symbol': 'mustNotCrossTheCanvasBoundary',
        },
      },
      const {
        'futureProperty': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
    expect(
      () => _decode(model(slots: {'futureSlot': _single(null)})),
      throwsFormatException,
    );
    final expanded = _node(
      '5b833e24-0a85-48d7-8663-8333adffb829',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            'dd999db4-d619-42bc-80da-81368b43935c',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under ClipRSuperellipse',
    );
  });

  test('ClipRSuperellipse reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ClipRSuperellipse\n');
    final end = contract.indexOf('W|flutter.widgets.ClipRect\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ClipRSuperellipse\n'
      'P|borderRadius|borderRadius|0|-|-|'
      'borderRadius:borderRadius:v1:physical,directional:finiteNonNegative\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|clipper|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:CustomClipper<RSuperellipse>:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the exact ClipRect clip and optional-child contract', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
        'flutter.widgets.ClipRect',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      '3bb165aa-21d8-497c-8e95-0c5c960588b0',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Clipped child'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAliasWithSaveLayer',
          },
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.ClipRect');
    expect(decoded.properties.keys, const ['clipBehavior']);
    expect(
      (decoded.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAliasWithSaveLayer',
    );
    expect(decoded.slot('child')!.child!.id, child['id']);

    final customClipperNode = _decode(
      model(
        properties: const {
          'clipper': {'kind': 'dartObjectReferencePresence'},
        },
      ),
    ).root;
    expect(
      customClipperNode.properties['clipper']!.kind,
      'dartObjectReferencePresence',
    );
    expect(customClipperNode.properties['clipper']!.value, isTrue);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);
    final explicitEmpty = _decode(model(slots: {'child': _single(null)})).root;
    expect(explicitEmpty.slot('child')!.child, isNull);
  });

  test('rejects every malformed or out-of-contract ClipRect branch', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
        'flutter.widgets.ClipRect',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'clipBehavior': {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'none',
        },
      },
      const {
        'clipBehavior': {'kind': 'string', 'value': 'hardEdge'},
      },
      const {
        'clipper': {'kind': 'string', 'value': 'custom'},
      },
      const {
        'futureProperty': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
    expect(
      () => _decode(model(slots: {'futureSlot': _single(null)})),
      throwsFormatException,
    );
    final expanded = _node(
      '1768d247-784b-4b80-a1e8-7130143e827b',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            '6d6baf79-6265-427c-8f42-e08cb6cbefbc',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under ClipRect',
    );
  });

  test('ClipRect reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ClipRect\n');
    final end = contract.indexOf('W|flutter.widgets.ColoredBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ClipRect\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|clipper|dartObjectReference|0|-|-|'
      'dartObjectReference:dartObjectReference:v1:CustomClipper<Rect>:'
      'currentOrPackage:root,optionalMember:reference,'
      'zeroArgumentInvocation:requiredConstnessBoolean(false,true)\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'decodes finite, unbounded, expanding, and mixed ConstrainedBox axes',
    () {
      Map<String, Object?> model(
        Map<String, Object?> constraints, {
        Map<String, Object?>? child,
        bool includeSlot = true,
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '93d89766-af04-4fee-af57-a56c4ed5e37c',
          'flutter.widgets.ConstrainedBox',
          properties: {'constraints': constraints},
          slots: includeSlot ? {'child': _single(child)} : const {},
        );
        return json;
      }

      final child = _node(
        'bacaf0a6-b27a-4c98-9f88-8c8eb91597c1',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Constrained child'},
        },
      );
      final cases =
          <
            ({
              Map<String, Object?> wire,
              double? minWidth,
              double? maxWidth,
              double? minHeight,
              double? maxHeight,
            })
          >[
            (
              wire: _canvasBoxConstraints(10, 80, 20, 90),
              minWidth: 10,
              maxWidth: 80,
              minHeight: 20,
              maxHeight: 90,
            ),
            (
              wire: _canvasBoxConstraints(0, null, 0, null),
              minWidth: 0,
              maxWidth: null,
              minHeight: 0,
              maxHeight: null,
            ),
            (
              wire: _canvasBoxConstraints(null, null, null, null),
              minWidth: null,
              maxWidth: null,
              minHeight: null,
              maxHeight: null,
            ),
            (
              wire: _canvasBoxConstraints(null, null, 12.5, 40),
              minWidth: null,
              maxWidth: null,
              minHeight: 12.5,
              maxHeight: 40,
            ),
            (
              wire: _canvasBoxConstraints(4, null, null, null),
              minWidth: 4,
              maxWidth: null,
              minHeight: null,
              maxHeight: null,
            ),
          ];

      for (final axisCase in cases) {
        final decoded = _decode(model(axisCase.wire, child: child)).root;
        final value =
            decoded.properties['constraints']!.value
                as CanvasBoxConstraintsValue;
        expect(value.minWidth, axisCase.minWidth, reason: '${axisCase.wire}');
        expect(value.maxWidth, axisCase.maxWidth, reason: '${axisCase.wire}');
        expect(value.minHeight, axisCase.minHeight, reason: '${axisCase.wire}');
        expect(value.maxHeight, axisCase.maxHeight, reason: '${axisCase.wire}');
        expect(decoded.slot('child')!.child!.id, child['id']);
      }

      expect(
        _decode(
          model(_canvasBoxConstraints(0, null, 0, null)),
        ).root.slot('child')!.child,
        isNull,
      );
      expect(
        _decode(
          model(_canvasBoxConstraints(0, null, 0, null), includeSlot: false),
        ).root.slot('child'),
        isNull,
      );
    },
  );

  test('rejects every malformed ConstrainedBox boundary and shape', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '93d89766-af04-4fee-af57-a56c4ed5e37c',
        'flutter.widgets.ConstrainedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final valid = _canvasBoxConstraints(0, null, 0, null);
    final invalidConstraints = <Map<String, Object?>>[
      for (final missing in const [
        'minWidth',
        'maxWidth',
        'minHeight',
        'maxHeight',
      ])
        Map<String, Object?>.from(valid)..remove(missing),
      {...valid, 'extra': 0},
      _canvasBoxConstraints(-0.001, null, 0, null),
      _canvasBoxConstraints(0, -0.001, 0, null),
      _canvasBoxConstraints(0, null, -0.001, null),
      _canvasBoxConstraints(0, null, 0, -0.001),
      _canvasBoxConstraints(20, 19.999, 0, null),
      _canvasBoxConstraints(0, null, 20, 19.999),
      _canvasBoxConstraints(null, 100, 0, null),
      _canvasBoxConstraints(0, null, null, 100),
      _canvasBoxConstraints('0', null, 0, null),
      _canvasBoxConstraints(0, true, 0, null),
    ];

    expect(() => _decode(model()), throwsFormatException);
    expect(
      () => _decode(
        model(
          properties: {
            'constraints': {'kind': 'string', 'value': '0..infinity'},
          },
        ),
      ),
      throwsFormatException,
    );
    for (final constraints in invalidConstraints) {
      expect(
        () => _decode(model(properties: {'constraints': constraints})),
        throwsFormatException,
        reason: constraints.toString(),
      );
    }
    for (final field in const [
      'minWidth',
      'maxWidth',
      'minHeight',
      'maxHeight',
    ]) {
      final constraints = Map<String, Object?>.from(valid)
        ..[field] = 987654321.125;
      final finiteJson = jsonEncode(
        model(properties: {'constraints': constraints}),
      );
      final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
      expect(
        () =>
            CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
        throwsFormatException,
        reason: field,
      );
    }
    expect(
      () => _decode(
        model(
          properties: {'constraints': valid},
          slots: {'child': _list(const [])},
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        model(
          properties: {
            'constraints': valid,
            'unknown': {'kind': 'boolean', 'value': true},
          },
        ),
      ),
      throwsFormatException,
    );
  });

  test('ConstrainedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ConstrainedBox\n');
    final end = contract.indexOf('W|flutter.widgets.Container\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ConstrainedBox\n'
      'P|constraints|boxConstraints|1|'
      'boxConstraints:0,inf,0,inf|-|'
      'boxConstraints:boxConstraints:v2:finiteOrPositiveInfinity\n'
      'S|child|single|0|0|1|any\n',
    );
    expect(
      RegExp(
        r'^P\|constraints\|boxConstraints\|',
        multiLine: true,
      ).allMatches(contract),
      hasLength(3), // ConstrainedBox, Container, CircularProgressIndicator.
    );
  });

  test('decodes both LimitedBox limits, omission, and its optional child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '57b8ce90-edde-4988-a4b7-bbf2eec66922',
        'flutter.widgets.LimitedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      'f28acc9b-e654-4b3a-ad07-59d75742b433',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Limited child'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'maxWidth': {'kind': 'double', 'value': 120.0},
          'maxHeight': {'kind': 'double', 'value': 47.5},
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.LimitedBox');
    expect(decoded.properties.keys, const ['maxWidth', 'maxHeight']);
    expect(decoded.properties['maxWidth']!.value, 120.0);
    expect(decoded.properties['maxHeight']!.value, 47.5);
    expect(decoded.slot('child')!.child!.id, child['id']);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);
    expect(
      _decode(model(slots: {'child': _single(null)})).root.slot('child')!.child,
      isNull,
    );
    final zero = _decode(
      model(
        properties: const {
          'maxWidth': {'kind': 'double', 'value': 0.0},
          'maxHeight': {'kind': 'double', 'value': 0.0},
        },
      ),
    ).root;
    expect(zero.properties['maxWidth']!.value, 0.0);
    expect(zero.properties['maxHeight']!.value, 0.0);
  });

  test('rejects values outside the reviewed LimitedBox projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '57b8ce90-edde-4988-a4b7-bbf2eec66922',
        'flutter.widgets.LimitedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'maxWidth': {'kind': 'integer', 'value': 1},
      },
      const {
        'maxHeight': {'kind': 'double', 'value': -0.001},
      },
      const {
        'maxHeight': {'kind': 'string', 'value': '100'},
      },
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );

    final finiteJson = jsonEncode(
      model(
        properties: const {
          'maxWidth': {'kind': 'double', 'value': 987654321.125},
        },
      ),
    );
    final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
    expect(
      () => CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
      throwsFormatException,
    );
  });

  test('LimitedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.LimitedBox\n');
    final end = contract.indexOf('W|flutter.widgets.ListBody\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.LimitedBox\n'
      'P|maxHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'P|maxWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the exact optional ListBody contract and ordered children', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '69fdc5a9-8321-4e42-9a8e-2dadc1344b38',
        'flutter.widgets.ListBody',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final first = _node(
      '3aa5f08c-1307-4634-8bed-68b86091cc90',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'First'},
      },
    );
    final second = _node(
      '5019f83d-0103-4b4e-9932-cc6833343904',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Second'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'horizontal'},
          'reverse': {'kind': 'boolean', 'value': true},
        },
        slots: {
          'children': _list([first, second]),
        },
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.ListBody');
    expect(decoded.properties.keys, const ['mainAxis', 'reverse']);
    final mainAxis = decoded.properties['mainAxis']!.value as CanvasEnumValue;
    expect(mainAxis.type, 'Axis');
    expect(mainAxis.value, 'horizontal');
    expect(decoded.properties['reverse']!.value, isTrue);
    expect(decoded.slot('children')!.children.map((child) => child.id), [
      first['id'],
      second['id'],
    ]);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('children'), isNull);

    final explicitEmpty = _decode(
      model(slots: {'children': _list(const [])}),
    ).root;
    expect(explicitEmpty.slot('children')!.children, isEmpty);
  });

  test('rejects values outside the reviewed ListBody projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '69fdc5a9-8321-4e42-9a8e-2dadc1344b38',
        'flutter.widgets.ListBody',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'diagonal'},
      },
      const {
        'mainAxis': {
          'kind': 'enum',
          'type': 'AxisDirection',
          'value': 'vertical',
        },
      },
      const {
        'mainAxis': {'kind': 'string', 'value': 'vertical'},
      },
      const {
        'reverse': {'kind': 'integer', 'value': 1},
      },
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'children': _single(null)})),
      throwsFormatException,
    );
  });

  test('ListBody reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ListBody\n');
    final end = contract.indexOf('W|flutter.widgets.ListView\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ListBody\n'
      'P|mainAxis|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Axis:horizontal,vertical\n'
      'P|reverse|boolean|0|-|-|boolean:any\n'
      'S|children|list|0|0|10000|any\n',
    );
  });

  test(
    'decodes the exact optional OverflowBar contract and ordered children',
    () {
      Map<String, Object?> model({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '765940f9-e63f-46d8-8837-07c52904c9b9',
          'flutter.widgets.OverflowBar',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final first = _node(
        '1120bc37-90cf-4980-8ee5-611c0ca6b876',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'First'},
        },
      );
      final second = _node(
        'e02c34d9-1ca6-4373-b074-79b3a8052ce8',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Second'},
        },
      );
      final decoded = _decode(
        model(
          properties: const {
            'spacing': {'kind': 'double', 'value': -2.5},
            'alignment': {
              'kind': 'enum',
              'type': 'MainAxisAlignment',
              'value': 'spaceEvenly',
            },
            'overflowSpacing': {'kind': 'double', 'value': -1.25},
            'overflowAlignment': {
              'kind': 'enum',
              'type': 'OverflowBarAlignment',
              'value': 'center',
            },
            'overflowDirection': {
              'kind': 'enum',
              'type': 'VerticalDirection',
              'value': 'up',
            },
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': 'rtl',
            },
          },
          slots: {
            'children': _list([first, second]),
          },
        ),
      ).root;

      expect(decoded.type, 'flutter.widgets.OverflowBar');
      expect(decoded.properties['spacing']!.value, -2.5);
      expect(decoded.properties['overflowSpacing']!.value, -1.25);
      for (final entry in const {
        'alignment': ('MainAxisAlignment', 'spaceEvenly'),
        'overflowAlignment': ('OverflowBarAlignment', 'center'),
        'overflowDirection': ('VerticalDirection', 'up'),
        'textDirection': ('TextDirection', 'rtl'),
      }.entries) {
        final value = decoded.properties[entry.key]!.value as CanvasEnumValue;
        expect(value.type, entry.value.$1, reason: entry.key);
        expect(value.value, entry.value.$2, reason: entry.key);
      }
      expect(decoded.slot('children')!.children.map((child) => child.id), [
        first['id'],
        second['id'],
      ]);

      final omitted = _decode(model()).root;
      expect(omitted.properties, isEmpty);
      expect(omitted.slot('children'), isNull);

      final explicitEmpty = _decode(
        model(slots: {'children': _list(const [])}),
      ).root;
      expect(explicitEmpty.slot('children')!.children, isEmpty);
    },
  );

  test('rejects values outside the reviewed OverflowBar projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '765940f9-e63f-46d8-8837-07c52904c9b9',
        'flutter.widgets.OverflowBar',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'spacing': {'kind': 'integer', 'value': 2},
      },
      const {
        'alignment': {
          'kind': 'enum',
          'type': 'MainAxisAlignment',
          'value': 'baseline',
        },
      },
      const {
        'overflowAlignment': {
          'kind': 'enum',
          'type': 'MainAxisAlignment',
          'value': 'start',
        },
      },
      const {
        'overflowDirection': {
          'kind': 'enum',
          'type': 'VerticalDirection',
          'value': 'sideways',
        },
      },
      const {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'auto',
        },
      },
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'children': _single(null)})),
      throwsFormatException,
    );

    final finiteJson = jsonEncode(
      model(
        properties: const {
          'overflowSpacing': {'kind': 'double', 'value': 987654321.125},
        },
      ),
    );
    final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
    expect(
      () => CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
      throwsFormatException,
    );
  });

  test('OverflowBar reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.OverflowBar\n');
    final end = contract.indexOf('W|flutter.widgets.OverflowBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.OverflowBar\n'
      'P|alignment|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'MainAxisAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start\n'
      'P|overflowAlignment|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'OverflowBarAlignment:center,end,start\n'
      'P|overflowDirection|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'VerticalDirection:down,up\n'
      'P|overflowSpacing|double|0|-|double:*:1:*:1|'
      'double:range:*:1:*:1\n'
      'P|spacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1\n'
      'P|textDirection|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextDirection:ltr,rtl\n'
      'S|children|list|0|0|10000|any\n',
    );
  });

  test('decodes the complete optional OverflowBox contract and child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4',
        'flutter.widgets.OverflowBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      'f72283ea-320c-4733-9072-963bc8281e7b',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Overflow child'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'alignment': _canvasAlignment(
            basis: 'directional',
            horizontal: -1.25,
            vertical: 0.75,
          ),
          'minWidth': {'kind': 'double', 'value': 120.0},
          'maxWidth': {'kind': 'double', 'value': 240.5},
          'minHeight': {'kind': 'double', 'value': 0.0},
          'maxHeight': {'kind': 'double', 'value': 160.25},
          'fit': {
            'kind': 'enum',
            'type': 'OverflowBoxFit',
            'value': 'deferToChild',
          },
        },
        slots: {'child': _single(child)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.OverflowBox');
    expect(decoded.properties.keys, const [
      'alignment',
      'minWidth',
      'maxWidth',
      'minHeight',
      'maxHeight',
      'fit',
    ]);
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, -1.25);
    expect(alignment.vertical, 0.75);
    expect(decoded.properties['minWidth']!.value, 120.0);
    expect(decoded.properties['maxWidth']!.value, 240.5);
    expect(decoded.properties['minHeight']!.value, 0.0);
    expect(decoded.properties['maxHeight']!.value, 160.25);
    expect(
      (decoded.properties['fit']!.value as CanvasEnumValue).value,
      'deferToChild',
    );
    expect(decoded.slot('child')!.child!.id, child['id']);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);
    expect(
      _decode(model(slots: {'child': _single(null)})).root.slot('child')!.child,
      isNull,
    );
    final equalBounds = _decode(
      model(
        properties: const {
          'minWidth': {'kind': 'double', 'value': 42.0},
          'maxWidth': {'kind': 'double', 'value': 42.0},
          'minHeight': {'kind': 'double', 'value': 17.0},
          'maxHeight': {'kind': 'double', 'value': 17.0},
        },
      ),
    ).root;
    expect(equalBounds.properties, hasLength(4));
  });

  test('rejects every value outside the reviewed OverflowBox projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4',
        'flutter.widgets.OverflowBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'minWidth': {'kind': 'integer', 'value': 1},
      },
      const {
        'maxWidth': {'kind': 'double', 'value': -0.001},
      },
      const {
        'minHeight': {'kind': 'string', 'value': '0'},
      },
      const {
        'minWidth': {'kind': 'double', 'value': 81.0},
        'maxWidth': {'kind': 'double', 'value': 80.0},
      },
      const {
        'minHeight': {'kind': 'double', 'value': 41.0},
        'maxHeight': {'kind': 'double', 'value': 40.0},
      },
      const {
        'fit': {'kind': 'enum', 'type': 'OverflowBoxFit', 'value': 'expand'},
      },
      const {
        'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'contain'},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'center'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );

    for (final property in const [
      'minWidth',
      'maxWidth',
      'minHeight',
      'maxHeight',
    ]) {
      expect(
        () => _decode(
          model(
            properties: {
              property: {'kind': 'integer', 'value': 1},
            },
          ),
        ),
        throwsFormatException,
        reason: '$property rejects integer wire values',
      );
      expect(
        () => _decode(
          model(
            properties: {
              property: {'kind': 'double', 'value': -0.001},
            },
          ),
        ),
        throwsFormatException,
        reason: '$property rejects negative values',
      );
      final finiteJson = jsonEncode(
        model(
          properties: {
            property: {'kind': 'double', 'value': 987654321.125},
          },
        ),
      );
      final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
      expect(
        () =>
            CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
        throwsFormatException,
        reason: property,
      );
    }
  });

  test('OverflowBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.OverflowBox\n');
    final end = contract.indexOf('W|flutter.widgets.Padding\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.OverflowBox\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|fit|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:'
      'OverflowBoxFit:deferToChild,max\n'
      'P|maxHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'P|maxWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'P|minHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'P|minWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'decodes every optional UnconstrainedBox property and its child slot',
    () {
      Map<String, Object?> model({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '67247867-79f8-470f-b109-59971aa7392c',
          'flutter.widgets.UnconstrainedBox',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final child = _node(
        'ff6ed199-2782-4be8-873b-e61ed6422664',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Natural size'},
        },
      );
      final decoded = _decode(
        model(
          properties: {
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': 'rtl',
            },
            'alignment': _canvasAlignment(
              basis: 'directional',
              horizontal: -1.25,
              vertical: 0.5,
            ),
            'constrainedAxis': {
              'kind': 'enum',
              'type': 'Axis',
              'value': 'vertical',
            },
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': 'antiAliasWithSaveLayer',
            },
          },
          slots: {'child': _single(child)},
        ),
      ).root;

      expect(decoded.type, 'flutter.widgets.UnconstrainedBox');
      expect(decoded.properties.keys, const [
        'textDirection',
        'alignment',
        'constrainedAxis',
        'clipBehavior',
      ]);
      expect(
        (decoded.properties['textDirection']!.value as CanvasEnumValue).value,
        'rtl',
      );
      final alignment =
          decoded.properties['alignment']!.value
              as CanvasAlignmentGeometryValue;
      expect(alignment.basis, 'directional');
      expect(alignment.horizontal, -1.25);
      expect(alignment.vertical, 0.5);
      expect(
        (decoded.properties['constrainedAxis']!.value as CanvasEnumValue).value,
        'vertical',
      );
      expect(
        (decoded.properties['clipBehavior']!.value as CanvasEnumValue).value,
        'antiAliasWithSaveLayer',
      );
      expect(decoded.slot('child')!.child!.id, child['id']);

      final omitted = _decode(model()).root;
      expect(omitted.properties, isEmpty);
      expect(omitted.slot('child'), isNull);
      expect(
        _decode(
          model(slots: {'child': _single(null)}),
        ).root.slot('child')!.child,
        isNull,
      );
      for (final axis in const ['horizontal', 'vertical']) {
        final axisNode = _decode(
          model(
            properties: {
              'constrainedAxis': {
                'kind': 'enum',
                'type': 'Axis',
                'value': axis,
              },
            },
          ),
        ).root;
        expect(
          (axisNode.properties['constrainedAxis']!.value as CanvasEnumValue)
              .value,
          axis,
        );
      }
    },
  );

  test('rejects values outside the reviewed UnconstrainedBox projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '67247867-79f8-470f-b109-59971aa7392c',
        'flutter.widgets.UnconstrainedBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'auto',
        },
      },
      const {
        'textDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      },
      const {
        'constrainedAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'both'},
      },
      const {
        'constrainedAxis': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'ltr',
        },
      },
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'center'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'child': _list(const [])})),
      throwsFormatException,
    );
  });

  test('UnconstrainedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.UnconstrainedBox\n');
    final end = contract.indexOf('W|flutter.widgets.Visibility\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.UnconstrainedBox\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|constrainedAxis|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Axis:horizontal,vertical\n'
      'P|textDirection|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextDirection:ltr,rtl\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the exact optional Stack contract and ordered children', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '8c1fa4e5-15ac-491c-843a-642633474416',
        'flutter.widgets.Stack',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final first = _node(
      '8a75a6c4-928f-46b2-8b75-9b72909dd5f3',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Bottom'},
      },
    );
    final second = _node(
      '7cdd7705-d8c9-4d77-b733-a559a04fc008',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Top'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'alignment': _canvasAlignment(
            basis: 'directional',
            horizontal: -1,
            vertical: 0.5,
          ),
          'textDirection': {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': 'ltr',
          },
          'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'passthrough'},
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAlias',
          },
        },
        slots: {
          'children': _list([first, second]),
        },
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.Stack');
    expect(decoded.properties.keys, const [
      'alignment',
      'textDirection',
      'fit',
      'clipBehavior',
    ]);
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, -1);
    expect(alignment.vertical, 0.5);
    expect(
      (decoded.properties['textDirection']!.value as CanvasEnumValue).value,
      'ltr',
    );
    expect(
      (decoded.properties['fit']!.value as CanvasEnumValue).value,
      'passthrough',
    );
    expect(
      (decoded.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAlias',
    );
    expect(decoded.slot('children')!.children.map((child) => child.id), [
      first['id'],
      second['id'],
    ]);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('children'), isNull);

    final explicitEmpty = _decode(
      model(slots: {'children': _list(const [])}),
    ).root;
    expect(explicitEmpty.slot('children')!.children, isEmpty);
  });

  test('rejects values outside the reviewed Stack projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '8c1fa4e5-15ac-491c-843a-642633474416',
        'flutter.widgets.Stack',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'tight'},
      },
      const {
        'fit': {'kind': 'enum', 'type': 'FlexFit', 'value': 'loose'},
      },
      const {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'auto',
        },
      },
      const {
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'defer'},
      },
      const {
        'alignment': {'kind': 'string', 'value': 'topStart'},
      },
      {'alignment': _canvasAlignment(basis: 'fractional')},
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'children': _single(null)})),
      throwsFormatException,
    );
  });

  test('Stack reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Stack\n');
    final end = contract.indexOf('W|flutter.widgets.Text\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Stack\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|fit|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'StackFit:expand,loose,passthrough\n'
      'P|textDirection|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextDirection:ltr,rtl\n'
      'S|children|list|0|0|10000|any\n',
    );
  });

  test(
    'decodes exact IndexedStack properties, nullable index, and ordered children',
    () {
      Map<String, Object?> model({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '4b36c27c-6f9d-4922-9747-17bd44396a4e',
          'flutter.widgets.IndexedStack',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final first = _node(
        '0da17d8a-a37b-4422-96e7-cdd3813939e2',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'First'},
        },
      );
      final second = _node(
        '6d83b1e9-4a15-44ef-b20f-c1af78920b07',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Second'},
        },
      );
      final decoded = _decode(
        model(
          properties: {
            'alignment': _canvasAlignment(
              basis: 'directional',
              horizontal: 1,
              vertical: -0.5,
            ),
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': 'rtl',
            },
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': 'antiAliasWithSaveLayer',
            },
            'sizing': {
              'kind': 'enum',
              'type': 'StackFit',
              'value': 'passthrough',
            },
            'index': {'kind': 'integer', 'value': 1},
          },
          slots: {
            'children': _list([first, second]),
          },
        ),
      ).root;

      expect(decoded.type, 'flutter.widgets.IndexedStack');
      expect(decoded.properties.keys, const [
        'alignment',
        'textDirection',
        'clipBehavior',
        'sizing',
        'index',
      ]);
      expect(decoded.properties['index']!.kind, 'integer');
      expect(decoded.properties['index']!.value, 1);
      expect(
        (decoded.properties['sizing']!.value as CanvasEnumValue).value,
        'passthrough',
      );
      expect(decoded.slot('children')!.children.map((child) => child.id), [
        first['id'],
        second['id'],
      ]);

      final omitted = _decode(
        model(
          slots: {
            'children': _list([first]),
          },
        ),
      ).root;
      expect(omitted.properties.containsKey('index'), isFalse);

      final explicitNull = _decode(
        model(
          properties: const {
            'index': {'kind': 'null'},
          },
          slots: {
            'children': _list([first, second]),
          },
        ),
      ).root;
      expect(explicitNull.properties['index']!.kind, 'null');
      expect(explicitNull.properties['index']!.value, isNull);
      expect(explicitNull.slot('children')!.children, hasLength(2));
    },
  );

  test('enforces the exact IndexedStack index-to-children relationship', () {
    Map<String, Object?> model({
      Object? index = const _AbsentTestValue(),
      List<Map<String, Object?>> children = const [],
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '4b36c27c-6f9d-4922-9747-17bd44396a4e',
        'flutter.widgets.IndexedStack',
        properties: {
          if (index is! _AbsentTestValue)
            'index': index == null
                ? const {'kind': 'null'}
                : {'kind': 'integer', 'value': index},
        },
        slots: {'children': _list(children)},
      );
      return json;
    }

    final child = _node(
      '0da17d8a-a37b-4422-96e7-cdd3813939e2',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Child'},
      },
    );
    final second = _node(
      '6d83b1e9-4a15-44ef-b20f-c1af78920b07',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Second'},
      },
    );

    expect(() => _decode(model()), returnsNormally);
    expect(() => _decode(model(index: 0)), returnsNormally);
    expect(() => _decode(model(index: null)), returnsNormally);
    expect(() => _decode(model(children: [child])), returnsNormally);
    expect(
      () => _decode(model(index: 1, children: [child, second])),
      returnsNormally,
    );
    for (final negative in <Map<String, Object?>>[
      model(index: -1),
      model(index: -1, children: [child]),
    ]) {
      expect(
        () => _decode(negative),
        throwsA(
          isA<FormatException>().having(
            (error) => error.message,
            'message',
            contains('numeric value is outside its bounds'),
          ),
        ),
      );
    }
    for (final invalid in <Map<String, Object?>>[
      model(index: 1),
      model(index: 1, children: [child]),
      model(index: 2, children: [child, second]),
    ]) {
      expect(
        () => _decode(invalid),
        throwsA(
          isA<FormatException>().having(
            (error) => error.message,
            'message',
            contains(
              'IndexedStack index must be null or within children.length',
            ),
          ),
        ),
      );
    }
  });

  test('rejects values outside the reviewed IndexedStack projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '4b36c27c-6f9d-4922-9747-17bd44396a4e',
        'flutter.widgets.IndexedStack',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'index': {'kind': 'null', 'value': null},
      },
      const {
        'index': {'kind': 'double', 'value': 0.0},
      },
      const {
        'sizing': {'kind': 'enum', 'type': 'StackFit', 'value': 'tight'},
      },
      const {
        'sizing': {'kind': 'enum', 'type': 'FlexFit', 'value': 'loose'},
      },
      const {
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'loose'},
      },
      const {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'auto',
        },
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'children': _single(null)})),
      throwsFormatException,
    );
    final expanded = _node(
      '70e18fc2-a3d6-4976-aa99-45f0aed417b5',
      'flutter.widgets.Expanded',
      properties: const {
        'flex': {'kind': 'integer', 'value': 1},
      },
      slots: {
        'child': _single(
          _node(
            'bf98849f-028e-47f1-829e-766f007c69d7',
            'flutter.widgets.Text',
            properties: const {
              'data': {'kind': 'string', 'value': 'Invalid'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(
        model(
          slots: {
            'children': _list([expanded]),
          },
        ),
      ),
      throwsFormatException,
    );
  });

  test('IndexedStack reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.IndexedStack\n');
    final end = contract.indexOf('W|flutter.widgets.IntrinsicHeight\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.IndexedStack\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|index|integer,null|0|-|'
      'integer:0:1:9007199254740991:1|'
      'integer:range:0:1:9007199254740991:1;null:any\n'
      'P|sizing|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'StackFit:expand,loose,passthrough\n'
      'P|textDirection|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextDirection:ltr,rtl\n'
      'S|children|list|0|0|10000|any\n',
    );
  });

  test('decodes every optional Wrap leaf and preserves ordered children', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '0cdde885-b68c-4b21-bd47-246721c4e8b2',
        'flutter.widgets.Wrap',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final first = _node(
      'a4fe95c8-6b3e-45bb-a430-2fced89a6d19',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'First'},
      },
    );
    final second = _node(
      '6cd6814d-f127-43d7-9348-11d5df75cd0e',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Second'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'direction': {'kind': 'enum', 'type': 'Axis', 'value': 'vertical'},
          'alignment': {
            'kind': 'enum',
            'type': 'WrapAlignment',
            'value': 'spaceEvenly',
          },
          'spacing': {'kind': 'double', 'value': -2.5},
          'runAlignment': {
            'kind': 'enum',
            'type': 'WrapAlignment',
            'value': 'center',
          },
          'runSpacing': {'kind': 'double', 'value': 4.5},
          'crossAxisAlignment': {
            'kind': 'enum',
            'type': 'WrapCrossAlignment',
            'value': 'end',
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
          'clipBehavior': {
            'kind': 'enum',
            'type': 'Clip',
            'value': 'antiAliasWithSaveLayer',
          },
        },
        slots: {
          'children': _list([first, second]),
        },
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.Wrap');
    expect(decoded.properties.keys, const [
      'direction',
      'alignment',
      'spacing',
      'runAlignment',
      'runSpacing',
      'crossAxisAlignment',
      'textDirection',
      'verticalDirection',
      'clipBehavior',
    ]);
    for (final expected in const <String, String>{
      'direction': 'vertical',
      'alignment': 'spaceEvenly',
      'runAlignment': 'center',
      'crossAxisAlignment': 'end',
      'textDirection': 'rtl',
      'verticalDirection': 'up',
      'clipBehavior': 'antiAliasWithSaveLayer',
    }.entries) {
      expect(
        (decoded.properties[expected.key]!.value as CanvasEnumValue).value,
        expected.value,
      );
    }
    expect(decoded.properties['spacing']!.value, -2.5);
    expect(decoded.properties['runSpacing']!.value, 4.5);
    expect(decoded.slot('children')!.children.map((child) => child.id), [
      first['id'],
      second['id'],
    ]);

    final omitted = _decode(model()).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('children'), isNull);
    final explicitEmpty = _decode(
      model(slots: {'children': _list(const [])}),
    ).root;
    expect(explicitEmpty.slot('children')!.children, isEmpty);
  });

  test('rejects values outside the reviewed Wrap projection', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '0cdde885-b68c-4b21-bd47-246721c4e8b2',
        'flutter.widgets.Wrap',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {
        'direction': {'kind': 'enum', 'type': 'Axis', 'value': 'diagonal'},
      },
      const {
        'alignment': {
          'kind': 'enum',
          'type': 'MainAxisAlignment',
          'value': 'center',
        },
      },
      const {
        'crossAxisAlignment': {
          'kind': 'enum',
          'type': 'WrapCrossAlignment',
          'value': 'stretch',
        },
      },
      const {
        'spacing': {'kind': 'integer', 'value': 2},
      },
      const {
        'runSpacing': {'kind': 'string', 'value': '4'},
      },
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(model(slots: {'children': _single(null)})),
      throwsFormatException,
    );
  });

  test('Wrap reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Wrap\n');
    expect(start, greaterThanOrEqualTo(0));
    expect(
      contract.substring(start),
      'W|flutter.widgets.Wrap\n'
      'P|alignment|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'WrapAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start\n'
      'P|clipBehavior|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|crossAxisAlignment|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'WrapCrossAlignment:center,end,start\n'
      'P|direction|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Axis:horizontal,vertical\n'
      'P|runAlignment|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'WrapAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start\n'
      'P|runSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1\n'
      'P|spacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1\n'
      'P|textDirection|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextDirection:ltr,rtl\n'
      'P|verticalDirection|enum|0|-|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'VerticalDirection:down,up\n'
      'S|children|list|0|0|10000|any\n',
    );
  });

  test(
    'decodes exact Expanded contract only in direct Row or Column slots',
    () {
      final text = _node(
        'c44e7670-369d-4f25-93cb-0c6ee0e506c4',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Expanded child'},
        },
      );
      Map<String, Object?> expanded({int? flex}) => _node(
        '22929511-43bf-492c-a1f8-b9de57b0dc8e',
        'flutter.widgets.Expanded',
        properties: {
          if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
        },
        slots: {'child': _single(text)},
      );
      Map<String, Object?> flexParent(String type, {int? flex}) {
        final json = _modelJson();
        json['root'] = _node(
          'b0e7cb42-7ca5-45db-9fc6-c457a3a5acb6',
          type,
          slots: {
            'children': _list([expanded(flex: flex)]),
          },
        );
        return json;
      }

      final omitted = _decode(flexParent('flutter.widgets.Row')).root;
      final omittedExpanded = omitted.slot('children')!.child!;
      expect(omittedExpanded.type, 'flutter.widgets.Expanded');
      expect(omittedExpanded.properties, isEmpty);
      expect(
        omittedExpanded.slot('child')!.child!.type,
        'flutter.widgets.Text',
      );

      final explicit = _decode(
        flexParent('flutter.widgets.Column', flex: 0),
      ).root.slot('children')!.child!;
      expect(explicit.properties['flex']!.value, 0);

      final maximum = _decode(
        flexParent('flutter.widgets.Row', flex: maxCanvasSequence),
      ).root.slot('children')!.child!;
      expect(maximum.properties['flex']!.value, maxCanvasSequence);
    },
  );

  test('rejects invalid Expanded flex, child, root, and parent placement', () {
    final text = _node(
      'c44e7670-369d-4f25-93cb-0c6ee0e506c4',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Child'},
      },
    );
    Map<String, Object?> expanded({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      '22929511-43bf-492c-a1f8-b9de57b0dc8e',
      'flutter.widgets.Expanded',
      properties: properties,
      slots: slots,
    );
    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    Map<String, Object?> rowWith(Map<String, Object?> child) => rootWith(
      _node(
        'b0e7cb42-7ca5-45db-9fc6-c457a3a5acb6',
        'flutter.widgets.Row',
        slots: {
          'children': _list([child]),
        },
      ),
    );

    expect(
      () => _decode(rootWith(expanded(slots: {'child': _single(text)}))),
      throwsFormatException,
    );
    expect(() => _decode(rowWith(expanded())), throwsFormatException);
    expect(
      () => _decode(rowWith(expanded(slots: {'child': _single(null)}))),
      throwsFormatException,
    );
    expect(
      () => _decode(
        rowWith(
          expanded(
            properties: const {
              'flex': {'kind': 'integer', 'value': -1},
            },
            slots: {'child': _single(text)},
          ),
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        rowWith(
          expanded(
            properties: const {
              'flex': {'kind': 'double', 'value': 1.0},
            },
            slots: {'child': _single(text)},
          ),
        ),
      ),
      throwsFormatException,
    );

    final invalidStack = _node(
      '891585b6-df15-43d5-a046-4f34f23228dd',
      'flutter.widgets.Stack',
      slots: {
        'children': _list([
          expanded(slots: {'child': _single(text)}),
        ]),
      },
    );
    expect(() => _decode(rootWith(invalidStack)), throwsFormatException);

    for (final nestedType in <String>[
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
    ]) {
      final nested = expanded(
        slots: {
          'child': _single(
            _node(
              '67b222c1-a2d8-4f36-8650-e19c95cf8660',
              nestedType,
              slots: {'child': _single(text)},
            ),
          ),
        },
      );
      expect(() => _decode(rowWith(nested)), throwsFormatException);
    }
  });

  test(
    'Expanded reviewed schema, placement, and creation contract is exact',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|flutter.widgets.Expanded\n');
      final end = contract.indexOf('W|flutter.widgets.FittedBox\n', start);
      expect(start, greaterThanOrEqualTo(0));
      expect(end, greaterThan(start));
      expect(
        contract.substring(start, end),
        'W|flutter.widgets.Expanded\n'
        'P|flex|integer|0|-|integer:0:1:9007199254740991:1|'
        'integer:range:0:1:9007199254740991:1\n'
        'S|child|single|1|1|1|any\n'
        'R|flutter.widgets.Expanded|directParentSlot|'
        'flutter.widgets.Column|children\n'
        'R|flutter.widgets.Expanded|directParentSlot|'
        'flutter.widgets.Row|children\n'
        'C|flutter.widgets.Expanded|paletteCreate|wrapExistingChild|child\n',
      );
    },
  );

  test(
    'decodes exact Flexible contract only in direct Row or Column slots',
    () {
      final text = _node(
        '5de0fa9d-39cf-47b8-b808-e0f5698b7ad6',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Flexible child'},
        },
      );
      Map<String, Object?> flexible({int? flex, String? fit}) => _node(
        'd8105c14-b229-4392-b397-7398f30aeb44',
        'flutter.widgets.Flexible',
        properties: {
          if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
          if (fit != null)
            'fit': {'kind': 'enum', 'type': 'FlexFit', 'value': fit},
        },
        slots: {'child': _single(text)},
      );
      Map<String, Object?> flexParent(String type, {int? flex, String? fit}) {
        final json = _modelJson();
        json['root'] = _node(
          'a465673d-110a-47b2-b3ce-064c9ba06335',
          type,
          slots: {
            'children': _list([flexible(flex: flex, fit: fit)]),
          },
        );
        return json;
      }

      final omitted = _decode(flexParent('flutter.widgets.Row')).root;
      final omittedFlexible = omitted.slot('children')!.child!;
      expect(omittedFlexible.type, 'flutter.widgets.Flexible');
      expect(omittedFlexible.properties, isEmpty);
      expect(
        omittedFlexible.slot('child')!.child!.type,
        'flutter.widgets.Text',
      );

      final explicit = _decode(
        flexParent('flutter.widgets.Column', flex: 0, fit: 'tight'),
      ).root.slot('children')!.child!;
      expect(explicit.properties['flex']!.value, 0);
      expect(
        (explicit.properties['fit']!.value as CanvasEnumValue).value,
        'tight',
      );

      final maximum = _decode(
        flexParent(
          'flutter.widgets.Row',
          flex: maxCanvasSequence,
          fit: 'loose',
        ),
      ).root.slot('children')!.child!;
      expect(maximum.properties['flex']!.value, maxCanvasSequence);
      expect(
        (maximum.properties['fit']!.value as CanvasEnumValue).value,
        'loose',
      );
    },
  );

  test(
    'rejects invalid Flexible values, child, root, and parent placement',
    () {
      final text = _node(
        '5de0fa9d-39cf-47b8-b808-e0f5698b7ad6',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Child'},
        },
      );
      Map<String, Object?> flexible({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) => _node(
        'd8105c14-b229-4392-b397-7398f30aeb44',
        'flutter.widgets.Flexible',
        properties: properties,
        slots: slots,
      );
      Map<String, Object?> rootWith(Map<String, Object?> root) {
        final json = _modelJson();
        json['root'] = root;
        return json;
      }

      Map<String, Object?> rowWith(Map<String, Object?> child) => rootWith(
        _node(
          'a465673d-110a-47b2-b3ce-064c9ba06335',
          'flutter.widgets.Row',
          slots: {
            'children': _list([child]),
          },
        ),
      );

      expect(
        () => _decode(rootWith(flexible(slots: {'child': _single(text)}))),
        throwsFormatException,
      );
      expect(() => _decode(rowWith(flexible())), throwsFormatException);
      expect(
        () => _decode(rowWith(flexible(slots: {'child': _single(null)}))),
        throwsFormatException,
      );
      for (final properties in <Map<String, Object?>>[
        const {
          'flex': {'kind': 'integer', 'value': -1},
        },
        const {
          'flex': {'kind': 'double', 'value': 1.0},
        },
        const {
          'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'loose'},
        },
        const {
          'fit': {'kind': 'enum', 'type': 'FlexFit', 'value': 'invalid'},
        },
      ]) {
        expect(
          () => _decode(
            rowWith(
              flexible(properties: properties, slots: {'child': _single(text)}),
            ),
          ),
          throwsFormatException,
        );
      }

      final invalidStack = _node(
        '96a547e2-e6c1-499a-a219-a6089b270573',
        'flutter.widgets.Stack',
        slots: {
          'children': _list([
            flexible(slots: {'child': _single(text)}),
          ]),
        },
      );
      expect(() => _decode(rootWith(invalidStack)), throwsFormatException);

      for (final nestedType in <String>[
        'flutter.widgets.Expanded',
        'flutter.widgets.Flexible',
      ]) {
        final nested = flexible(
          slots: {
            'child': _single(
              _node(
                '3e611abb-ce3c-4c6a-8ad2-a945acbc19f3',
                nestedType,
                slots: {'child': _single(text)},
              ),
            ),
          },
        );
        expect(() => _decode(rowWith(nested)), throwsFormatException);
      }
    },
  );

  test(
    'Flexible reviewed schema, placement, and creation contract is exact',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|flutter.widgets.Flexible\n');
      final end = contract.indexOf(
        'W|flutter.widgets.FractionallySizedBox\n',
        start,
      );
      expect(start, greaterThanOrEqualTo(0));
      expect(end, greaterThan(start));
      expect(
        contract.substring(start, end),
        'W|flutter.widgets.Flexible\n'
        'P|fit|enum|0|-|-|enum:enum:'
        'cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:'
        'FlexFit:loose,tight\n'
        'P|flex|integer|0|-|integer:0:1:9007199254740991:1|'
        'integer:range:0:1:9007199254740991:1\n'
        'S|child|single|1|1|1|any\n'
        'R|flutter.widgets.Flexible|directParentSlot|'
        'flutter.widgets.Column|children\n'
        'R|flutter.widgets.Flexible|directParentSlot|'
        'flutter.widgets.Row|children\n'
        'C|flutter.widgets.Flexible|paletteCreate|wrapExistingChild|child\n',
      );
    },
  );

  test('decodes Spacer only as a direct Row or Column child', () {
    Map<String, Object?> spacer({int? flex}) => _node(
      '83a1364b-efac-4cc7-a4ef-660ff49fba7c',
      'flutter.widgets.Spacer',
      properties: {
        if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
      },
    );
    Map<String, Object?> model(String parentType, {int? flex}) {
      final json = _modelJson();
      json['root'] = _node(
        '10e37c05-b2bd-436b-91d7-cad781bdb47a',
        parentType,
        slots: {
          'children': _list([spacer(flex: flex)]),
        },
      );
      return json;
    }

    final omitted = _decode(model('flutter.widgets.Row')).root;
    final omittedSpacer = omitted.slot('children')!.child!;
    expect(omittedSpacer.type, 'flutter.widgets.Spacer');
    expect(omittedSpacer.properties, isEmpty);
    expect(omittedSpacer.slots, isEmpty);

    final minimum = _decode(
      model('flutter.widgets.Column', flex: 1),
    ).root.slot('children')!.child!;
    expect(minimum.properties['flex']!.value, 1);

    final maximum = _decode(
      model('flutter.widgets.Row', flex: maxCanvasSequence),
    ).root.slot('children')!.child!;
    expect(maximum.properties['flex']!.value, maxCanvasSequence);
  });

  test('rejects invalid Spacer values, slots, root, and parent placement', () {
    Map<String, Object?> spacer({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      '83a1364b-efac-4cc7-a4ef-660ff49fba7c',
      'flutter.widgets.Spacer',
      properties: properties,
      slots: slots,
    );
    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    Map<String, Object?> rowWith(Map<String, Object?> child) => rootWith(
      _node(
        '10e37c05-b2bd-436b-91d7-cad781bdb47a',
        'flutter.widgets.Row',
        slots: {
          'children': _list([child]),
        },
      ),
    );

    expect(() => _decode(rootWith(spacer())), throwsFormatException);
    expect(
      () => _decode(
        rootWith(
          _node(
            '255aaf93-d0d3-4dbc-96f0-d00a36954d53',
            'flutter.widgets.Stack',
            slots: {
              'children': _list([spacer()]),
            },
          ),
        ),
      ),
      throwsFormatException,
    );
    for (final properties in <Map<String, Object?>>[
      const {
        'flex': {'kind': 'integer', 'value': 0},
      },
      const {
        'flex': {'kind': 'integer', 'value': -1},
      },
      const {
        'flex': {'kind': 'integer', 'value': 9007199254740992},
      },
      const {
        'flex': {'kind': 'double', 'value': 1.0},
      },
      const {
        'flex': {'kind': 'dartExpression', 'source': '1'},
      },
      const {
        'unknown': {'kind': 'integer', 'value': 1},
      },
    ]) {
      expect(
        () => _decode(rowWith(spacer(properties: properties))),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(
        rowWith(
          spacer(
            slots: {
              'child': _single(
                _node(
                  'ec741ce1-7eeb-45d2-8b91-e57d07d171cc',
                  'flutter.widgets.Text',
                  properties: {
                    'data': {'kind': 'string', 'value': 'invalid'},
                  },
                ),
              ),
            },
          ),
        ),
      ),
      throwsFormatException,
    );
  });

  test('Spacer reviewed schema and placement contract is exact', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Spacer\n');
    final end = contract.indexOf('W|flutter.widgets.Stack\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Spacer\n'
      'P|flex|integer|0|-|integer:1:1:9007199254740991:1|'
      'integer:range:1:1:9007199254740991:1\n'
      'R|flutter.widgets.Spacer|directParentSlot|'
      'flutter.widgets.Column|children\n'
      'R|flutter.widgets.Spacer|directParentSlot|'
      'flutter.widgets.Row|children\n',
    );
  });

  test('decodes the exact direct Image contract and resolved provider', () {
    const resourceId =
        '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
    final json = _modelJson();
    json['root'] = _node(
      '2ee0f071-2884-4105-b5c5-190654025c39',
      'flutter.widgets.Image',
      properties: {
        'image': _canvasDirectImageProvider(
          provider: _canvasImageProvider(
            kind: 'exactAsset',
            assetName: 'assets/images/hero.png',
            packageName: 'image_package',
            exactScale: 2,
            resize: {
              'width': 320,
              'height': 180,
              'policy': 'fit',
              'allowUpscaling': false,
            },
            resolution: {
              'kind': 'resolved',
              'resourceId': resourceId,
              'resolvedScale': 2,
            },
          ),
        ),
        'frameBuilder': {'kind': 'callbackPresence'},
        'loadingBuilder': {'kind': 'callbackPresence'},
        'errorBuilder': {'kind': 'callbackPresence'},
        'semanticLabel': {'kind': 'string', 'value': 'Hero image'},
        'excludeFromSemantics': {'kind': 'boolean', 'value': false},
        'width': {'kind': 'integer', 'value': 320},
        'height': {'kind': 'double', 'value': 180.5},
        'color': {
          'kind': 'themeToken',
          'token': 'material.colorScheme.primary',
        },
        'opacity': {'kind': 'double', 'value': 0.75},
        'colorBlendMode': {
          'kind': 'enum',
          'type': 'BlendMode',
          'value': 'multiply',
        },
        'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'fill'},
        'alignment': _canvasAlignment(
          basis: 'directional',
          horizontal: 1,
          vertical: -1,
        ),
        'repeat': {'kind': 'enum', 'type': 'ImageRepeat', 'value': 'repeatX'},
        'centerSliceLeft': {'kind': 'double', 'value': 1.0},
        'centerSliceTop': {'kind': 'double', 'value': 2.0},
        'centerSliceRight': {'kind': 'double', 'value': 20.0},
        'centerSliceBottom': {'kind': 'double', 'value': 30.0},
        'matchTextDirection': {'kind': 'boolean', 'value': true},
        'gaplessPlayback': {'kind': 'boolean', 'value': true},
        'isAntiAlias': {'kind': 'boolean', 'value': true},
        'filterQuality': {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'high',
        },
      },
    );

    final decoded = _decode(json);
    final image = decoded.root;
    expect(image.type, 'flutter.widgets.Image');
    expect(image.properties, hasLength(22));
    final provider =
        image.properties['image']!.value as CanvasImageProviderValue;
    expect(provider.providerKind, 'exactAsset');
    expect(provider.assetName, 'assets/images/hero.png');
    expect(provider.packageName, 'image_package');
    expect(provider.exactScale, 2);
    expect(provider.resize!.width, 320);
    expect(provider.resize!.height, 180);
    expect(provider.resize!.policy, 'fit');
    expect(provider.resize!.allowUpscaling, isFalse);
    expect(provider.resolution, isA<CanvasResolvedImageValue>());
    expect(decoded.imageResourceIds, {resourceId});
    expect(image.properties['frameBuilder']!.value, isTrue);
    expect(image.properties['opacity']!.value, 0.75);
  });

  test('rejects incomplete or invalid direct Image values', () {
    Map<String, Object?> model(Map<String, Object?> properties) {
      final json = _modelJson();
      json['root'] = _node(
        '2ee0f071-2884-4105-b5c5-190654025c39',
        'flutter.widgets.Image',
        properties: properties,
      );
      return json;
    }

    final image = _canvasDirectImageProvider();
    for (final properties in <Map<String, Object?>>[
      const {},
      {
        'image': image,
        'centerSliceLeft': {'kind': 'double', 'value': 0.0},
      },
      {
        'image': image,
        'centerSliceLeft': {'kind': 'double', 'value': -1.0},
        'centerSliceTop': {'kind': 'double', 'value': 0.0},
        'centerSliceRight': {'kind': 'double', 'value': 2.0},
        'centerSliceBottom': {'kind': 'double', 'value': 2.0},
      },
      {
        'image': image,
        'centerSliceLeft': {'kind': 'double', 'value': 1.0},
        'centerSliceTop': {'kind': 'double', 'value': 0.0},
        'centerSliceRight': {'kind': 'double', 'value': 1.0},
        'centerSliceBottom': {'kind': 'double', 'value': 2.0},
      },
      {
        'image': image,
        'centerSliceLeft': {'kind': 'double', 'value': 0.0},
        'centerSliceTop': {'kind': 'double', 'value': 0.0},
        'centerSliceRight': {'kind': 'double', 'value': 2.0},
        'centerSliceBottom': {'kind': 'double', 'value': 2.0},
        'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'cover'},
      },
      {
        'image': image,
        'opacity': {'kind': 'integer', 'value': 1},
      },
      {
        'image': image,
        'opacity': {'kind': 'double', 'value': 1.01},
      },
      {
        'image': image,
        'width': {'kind': 'integer', 'value': -1},
      },
      {
        'image': {
          'kind': 'imageProvider',
          'assetName': 'assets/images/hero.png',
        },
      },
    ]) {
      expect(
        () => _decode(model(properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }

    final unavailable = _decode(model({'image': _canvasDirectImageProvider()}));
    expect(unavailable.imageResourceIds, isEmpty);
  });

  test('Image reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Image\n');
    final end = contract.indexOf('W|flutter.widgets.ImageIcon\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final bytes = utf8.encode(contract.substring(start, end));
    expect(bytes, hasLength(3583));
    expect(
      sha256Hex(bytes),
      'e3e0b2b0fbc7b678814d51a257191381fb872a39e797ac46c861954f3eee7b1a',
    );
    final block = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(22));
    expect(
      block,
      contains(
        'P|image|imageProvider|1|-|-|imageProvider:imageProvider:v1:'
        'asset,exactAsset:package:exactScale:'
        'resize(1..16384,exact,fit,allowUpscaling)\n',
      ),
    );
  });

  test('decodes the exact property-free IntrinsicHeight child contract', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '15aa2055-201d-4f3e-bd50-24cc60fa50c0',
        'flutter.widgets.IntrinsicHeight',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final omitted = _decode(model()).root;
    expect(omitted.type, 'flutter.widgets.IntrinsicHeight');
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);

    final empty = _decode(model(slots: {'child': _single(null)})).root;
    expect(empty.slot('child'), isNotNull);
    expect(empty.slot('child')!.child, isNull);

    final text = _node(
      'cf781f2a-8d2e-4a88-a05a-9758e5e911d8',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Measured child'},
      },
    );
    final occupied = _decode(model(slots: {'child': _single(text)})).root;
    expect(occupied.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test('rejects every non-contract IntrinsicHeight shape and flex child', () {
    Map<String, Object?> intrinsicHeight({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      '15aa2055-201d-4f3e-bd50-24cc60fa50c0',
      'flutter.widgets.IntrinsicHeight',
      properties: properties,
      slots: slots,
    );

    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    expect(
      () => _decode(
        rootWith(
          intrinsicHeight(
            properties: const {
              'height': {'kind': 'double', 'value': 10.0},
            },
          ),
        ),
      ),
      throwsFormatException,
    );
    expect(
      () =>
          _decode(rootWith(intrinsicHeight(slots: {'child': _list(const [])}))),
      throwsFormatException,
    );
    expect(
      () => _decode(
        rootWith(intrinsicHeight(slots: {'children': _list(const [])})),
      ),
      throwsFormatException,
    );

    final text = _node(
      'cf781f2a-8d2e-4a88-a05a-9758e5e911d8',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Flex child'},
      },
    );
    for (final flexType in const [
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
    ]) {
      final flexChild = _node(
        '4ce1aa49-68ea-4e84-ae5c-57f89107633d',
        flexType,
        slots: flexType == 'flutter.widgets.Spacer'
            ? const {}
            : {'child': _single(text)},
      );
      expect(
        () => _decode(
          rootWith(intrinsicHeight(slots: {'child': _single(flexChild)})),
        ),
        throwsFormatException,
        reason: '$flexType is legal only in Row.children or Column.children',
      );
    }
  });

  test('IntrinsicHeight reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.IntrinsicHeight\n');
    final end = contract.indexOf('W|flutter.widgets.IntrinsicWidth\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.IntrinsicHeight\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes exact nullable IntrinsicWidth steps and optional child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'bbce8cc2-8ff1-4337-83e2-46f70a579075',
        'flutter.widgets.IntrinsicWidth',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final omitted = _decode(model()).root;
    expect(omitted.type, 'flutter.widgets.IntrinsicWidth');
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);

    final zero = _decode(
      model(
        properties: const {
          'stepWidth': {'kind': 'double', 'value': 0.0},
          'stepHeight': {'kind': 'double', 'value': 0.0},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    expect(zero.properties['stepWidth']!.kind, 'double');
    expect(zero.properties['stepWidth']!.value, 0.0);
    expect(zero.properties['stepHeight']!.kind, 'double');
    expect(zero.properties['stepHeight']!.value, 0.0);
    expect(zero.slot('child')!.child, isNull);

    final text = _node(
      '4eaf2797-e975-402c-8e3a-827e40ee9c83',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Measured child'},
      },
    );
    final positive = _decode(
      model(
        properties: const {
          'stepWidth': {'kind': 'double', 'value': 24.5},
          'stepHeight': {'kind': 'double', 'value': 12.0},
        },
        slots: {'child': _single(text)},
      ),
    ).root;
    expect(positive.properties['stepWidth']!.value, 24.5);
    expect(positive.properties['stepHeight']!.value, 12.0);
    expect(positive.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test('rejects non-contract IntrinsicWidth values, slots, and flex child', () {
    Map<String, Object?> intrinsicWidth({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      'bbce8cc2-8ff1-4337-83e2-46f70a579075',
      'flutter.widgets.IntrinsicWidth',
      properties: properties,
      slots: slots,
    );

    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    for (final invalid in <Map<String, Object?>>[
      const {
        'stepWidth': {'kind': 'double', 'value': -1.0},
      },
      const {
        'stepHeight': {'kind': 'double', 'value': -0.001},
      },
      const {
        'stepWidth': {'kind': 'boolean', 'value': true},
      },
      const {
        'stepHeight': {'kind': 'string', 'value': '12'},
      },
      const {
        'stepWidth': {'kind': 'integer', 'value': 12},
      },
      const {
        'unknown': {'kind': 'integer', 'value': 1},
      },
    ]) {
      expect(
        () => _decode(rootWith(intrinsicWidth(properties: invalid))),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }
    expect(
      () =>
          _decode(rootWith(intrinsicWidth(slots: {'child': _list(const [])}))),
      throwsFormatException,
    );
    expect(
      () => _decode(
        rootWith(intrinsicWidth(slots: {'children': _list(const [])})),
      ),
      throwsFormatException,
    );

    final text = _node(
      '4eaf2797-e975-402c-8e3a-827e40ee9c83',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Flex child'},
      },
    );
    for (final flexType in const [
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
    ]) {
      final flexChild = _node(
        '05fca0c6-53be-4ef3-9b4a-2aad669bce7b',
        flexType,
        slots: flexType == 'flutter.widgets.Spacer'
            ? const {}
            : {'child': _single(text)},
      );
      expect(
        () => _decode(
          rootWith(intrinsicWidth(slots: {'child': _single(flexChild)})),
        ),
        throwsFormatException,
        reason: '$flexType is legal only in Row.children or Column.children',
      );
    }
  });

  test('IntrinsicWidth reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.IntrinsicWidth\n');
    final end = contract.indexOf('W|flutter.widgets.LimitedBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.IntrinsicWidth\n'
      'P|stepHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'P|stepWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes omitted, true, and false Offstage with optional child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'aa0bc346-b863-471f-bf3a-bcaa9bbf5090',
        'flutter.widgets.Offstage',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final omitted = _decode(model()).root;
    expect(omitted.type, 'flutter.widgets.Offstage');
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);

    final explicitTrue = _decode(
      model(
        properties: const {
          'offstage': {'kind': 'boolean', 'value': true},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    expect(explicitTrue.properties['offstage']!.value, isTrue);
    expect(explicitTrue.slot('child')!.child, isNull);

    final text = _node(
      'eea084e6-c139-421b-8a62-4e82a877b72c',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Visible child'},
      },
    );
    final explicitFalse = _decode(
      model(
        properties: const {
          'offstage': {'kind': 'boolean', 'value': false},
        },
        slots: {'child': _single(text)},
      ),
    ).root;
    expect(explicitFalse.properties['offstage']!.value, isFalse);
    expect(explicitFalse.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test('rejects non-contract Offstage values, slots, and flex child', () {
    Map<String, Object?> offstage({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      'aa0bc346-b863-471f-bf3a-bcaa9bbf5090',
      'flutter.widgets.Offstage',
      properties: properties,
      slots: slots,
    );

    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    for (final invalid in <Map<String, Object?>>[
      const {
        'offstage': {'kind': 'integer', 'value': 1},
      },
      const {
        'offstage': {'kind': 'string', 'value': 'true'},
      },
      const {
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(rootWith(offstage(properties: invalid))),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }
    expect(
      () => _decode(rootWith(offstage(slots: {'child': _list(const [])}))),
      throwsFormatException,
    );
    expect(
      () => _decode(rootWith(offstage(slots: {'children': _list(const [])}))),
      throwsFormatException,
    );

    final text = _node(
      'eea084e6-c139-421b-8a62-4e82a877b72c',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Flex child'},
      },
    );
    for (final flexType in const [
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
    ]) {
      final flexChild = _node(
        '52f7536c-b092-47dc-aa34-ad28d8bad093',
        flexType,
        slots: flexType == 'flutter.widgets.Spacer'
            ? const {}
            : {'child': _single(text)},
      );
      expect(
        () => _decode(rootWith(offstage(slots: {'child': _single(flexChild)}))),
        throwsFormatException,
        reason: '$flexType is legal only in Row.children or Column.children',
      );
    }
  });

  test('Offstage reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Offstage\n');
    final end = contract.indexOf('W|flutter.widgets.Opacity\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Offstage\n'
      'P|offstage|boolean|0|-|-|boolean:any\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes required signed portable RotatedBox turns and child', () {
    Map<String, Object?> model({
      required int quarterTurns,
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '81cdfd65-c958-40cf-ab25-3494d1a9e1fc',
        'flutter.widgets.RotatedBox',
        properties: {
          'quarterTurns': {'kind': 'integer', 'value': quarterTurns},
        },
        slots: slots,
      );
      return json;
    }

    for (final quarterTurns in const [
      -maxCanvasSequence,
      -1,
      0,
      1,
      maxCanvasSequence,
    ]) {
      final decoded = _decode(model(quarterTurns: quarterTurns)).root;
      expect(decoded.type, 'flutter.widgets.RotatedBox');
      expect(decoded.properties['quarterTurns']!.kind, 'integer');
      expect(decoded.properties['quarterTurns']!.value, quarterTurns);
      expect(decoded.slot('child'), isNull);
    }

    final text = _node(
      '908cd88b-2ffd-449a-9af8-a7e31dcc0055',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Rotated child'},
      },
    );
    final occupied = _decode(
      model(quarterTurns: -5, slots: {'child': _single(text)}),
    ).root;
    expect(occupied.properties['quarterTurns']!.value, -5);
    expect(occupied.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test('rejects invalid RotatedBox turns, slots, and flex children', () {
    Map<String, Object?> rotated({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      '81cdfd65-c958-40cf-ab25-3494d1a9e1fc',
      'flutter.widgets.RotatedBox',
      properties: properties,
      slots: slots,
    );

    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    for (final invalid in <Map<String, Object?>>[
      const {},
      const {
        'quarterTurns': {'kind': 'double', 'value': 1.0},
      },
      const {
        'quarterTurns': {'kind': 'integer', 'value': maxCanvasSequence + 1},
      },
      const {
        'quarterTurns': {'kind': 'integer', 'value': -maxCanvasSequence - 1},
      },
      const {
        'quarterTurns': {'kind': 'integer', 'value': 1},
        'unknown': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(rootWith(rotated(properties: invalid))),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }

    const validTurns = {
      'quarterTurns': {'kind': 'integer', 'value': 1},
    };
    expect(
      () => _decode(
        rootWith(
          rotated(properties: validTurns, slots: {'child': _list(const [])}),
        ),
      ),
      throwsFormatException,
    );

    final text = _node(
      '908cd88b-2ffd-449a-9af8-a7e31dcc0055',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Invalid flex child'},
      },
    );
    for (final flexType in const [
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
    ]) {
      final flexChild = _node(
        '52f7536c-b092-47dc-aa34-ad28d8bad093',
        flexType,
        slots: flexType == 'flutter.widgets.Spacer'
            ? const {}
            : {'child': _single(text)},
      );
      expect(
        () => _decode(
          rootWith(
            rotated(
              properties: validTurns,
              slots: {'child': _single(flexChild)},
            ),
          ),
        ),
        throwsFormatException,
        reason: '$flexType is legal only in Row.children or Column.children',
      );
    }
  });

  test('RotatedBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.RotatedBox\n');
    final end = contract.indexOf('W|flutter.widgets.Row\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.RotatedBox\n'
      'P|quarterTurns|integer|1|integer:1|'
      'integer:-9007199254740991:1:9007199254740991:1|'
      'integer:range:-9007199254740991:1:9007199254740991:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes required SizedOverflowBox size, alignment, and child', () {
    Map<String, Object?> model({
      required Map<String, Object?> properties,
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'd01c13f5-c20d-48bf-a360-5da7b6b36c67',
        'flutter.widgets.SizedOverflowBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final zero = _decode(
      model(
        properties: const {
          'size': {'kind': 'size', 'width': 0, 'height': 0},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    final zeroSize = zero.properties['size']!.value as CanvasSizeValue;
    expect(zeroSize.width, 0.0);
    expect(zeroSize.height, 0.0);
    expect(zero.properties['alignment'], isNull);
    expect(zero.slot('child')!.child, isNull);

    final text = _node(
      '04bf06da-4419-4d88-b504-25c93ba7a76a',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Overflow child'},
      },
    );
    final configured = _decode(
      model(
        properties: const {
          'size': {'kind': 'size', 'width': 120.5, 'height': 80.25},
          'alignment': {
            'kind': 'alignmentGeometry',
            'basis': 'directional',
            'horizontal': -1.0,
            'vertical': 1.0,
          },
        },
        slots: {'child': _single(text)},
      ),
    ).root;
    final size = configured.properties['size']!.value as CanvasSizeValue;
    expect(size.width, 120.5);
    expect(size.height, 80.25);
    final alignment =
        configured.properties['alignment']!.value
            as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, -1.0);
    expect(alignment.vertical, 1.0);
    expect(configured.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test(
    'rejects invalid SizedOverflowBox size, properties, slots, and child',
    () {
      Map<String, Object?> sizedOverflow({
        Map<String, Object?> properties = const {},
        Map<String, Object?> slots = const {},
      }) => _node(
        'd01c13f5-c20d-48bf-a360-5da7b6b36c67',
        'flutter.widgets.SizedOverflowBox',
        properties: properties,
        slots: slots,
      );

      Map<String, Object?> rootWith(Map<String, Object?> root) {
        final json = _modelJson();
        json['root'] = root;
        return json;
      }

      for (final invalid in <Map<String, Object?>>[
        const {},
        const {
          'size': {'kind': 'double', 'value': 100.0},
        },
        const {
          'size': {'kind': 'size', 'width': -0.001, 'height': 10.0},
        },
        const {
          'size': {'kind': 'size', 'width': 10.0, 'height': -0.001},
        },
        const {
          'size': {'kind': 'size', 'width': '100', 'height': 10.0},
        },
        const {
          'size': {'kind': 'size', 'width': 100.0},
        },
        const {
          'size': {
            'kind': 'size',
            'width': 100.0,
            'height': 100.0,
            'depth': 1.0,
          },
        },
        const {
          'size': {'kind': 'size', 'width': 100.0, 'height': 100.0},
          'alignment': {'kind': 'boolean', 'value': true},
        },
        const {
          'size': {'kind': 'size', 'width': 100.0, 'height': 100.0},
          'unknown': {'kind': 'boolean', 'value': true},
        },
      ]) {
        expect(
          () => _decode(rootWith(sizedOverflow(properties: invalid))),
          throwsFormatException,
          reason: invalid.toString(),
        );
      }
      const validSize = {
        'size': {'kind': 'size', 'width': 100.0, 'height': 100.0},
      };
      expect(
        () => _decode(
          rootWith(
            sizedOverflow(
              properties: validSize,
              slots: {'child': _list(const [])},
            ),
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => _decode(
          rootWith(
            sizedOverflow(
              properties: validSize,
              slots: {'children': _list(const [])},
            ),
          ),
        ),
        throwsFormatException,
      );

      final text = _node(
        '04bf06da-4419-4d88-b504-25c93ba7a76a',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Flex child'},
        },
      );
      for (final flexType in const [
        'flutter.widgets.Expanded',
        'flutter.widgets.Flexible',
        'flutter.widgets.Spacer',
      ]) {
        final flexChild = _node(
          '591255df-4442-4411-9bad-63eaf741a307',
          flexType,
          slots: flexType == 'flutter.widgets.Spacer'
              ? const {}
              : {'child': _single(text)},
        );
        expect(
          () => _decode(
            rootWith(
              sizedOverflow(
                properties: validSize,
                slots: {'child': _single(flexChild)},
              ),
            ),
          ),
          throwsFormatException,
          reason: '$flexType is legal only in Row.children or Column.children',
        );
      }
    },
  );

  test('SizedOverflowBox reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.SizedOverflowBox\n');
    final end = contract.indexOf('W|flutter.widgets.Spacer\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.SizedOverflowBox\n'
      'P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry\n'
      'P|size|size|1|size:100,100|-|size:size:finiteNonNegative\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the complete Transform.new contract and optional child', () {
    Map<String, Object?> model({
      required Map<String, Object?> properties,
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '6408cfe9-e227-43de-916c-bb9d66224de9',
        'flutter.widgets.Transform',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final text = _node(
      '3d93189a-6a63-49d1-abe5-cb32319428fd',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Transformed child'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'transform': {
            'kind': 'matrix4',
            'storage': <Object?>[
              1,
              0,
              0,
              0,
              0,
              1,
              0,
              0,
              0,
              0,
              1,
              0,
              24.5,
              -8.25,
              0,
              1,
            ],
          },
          'origin': {'kind': 'offset', 'dx': -12.5, 'dy': 8.25},
          'alignment': {
            'kind': 'alignmentGeometry',
            'basis': 'directional',
            'horizontal': -1.0,
            'vertical': 0.5,
          },
          'transformHitTests': {'kind': 'boolean', 'value': false},
          'filterQuality': {
            'kind': 'enum',
            'type': 'FilterQuality',
            'value': 'high',
          },
        },
        slots: {'child': _single(text)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.Transform');
    expect(decoded.properties.keys, const [
      'transform',
      'origin',
      'alignment',
      'transformHitTests',
      'filterQuality',
    ]);
    final matrix = decoded.properties['transform']!.value as CanvasMatrix4Value;
    expect(matrix.storage, hasLength(16));
    expect(matrix.storage[12], 24.5);
    expect(matrix.storage[13], -8.25);
    final origin = decoded.properties['origin']!.value as CanvasOffsetValue;
    expect(origin.dx, -12.5);
    expect(origin.dy, 8.25);
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, -1.0);
    expect(alignment.vertical, 0.5);
    expect(decoded.properties['transformHitTests']!.value, isFalse);
    final filter =
        decoded.properties['filterQuality']!.value as CanvasEnumValue;
    expect(filter.type, 'FilterQuality');
    expect(filter.value, 'high');
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    final defaults = _decode(
      model(
        properties: const {
          'transform': {
            'kind': 'matrix4',
            'storage': <Object?>[
              1,
              0,
              0,
              0,
              0,
              1,
              0,
              0,
              0,
              0,
              1,
              0,
              0,
              0,
              0,
              1,
            ],
          },
        },
      ),
    ).root;
    expect(defaults.properties.keys, const ['transform']);
    expect(defaults.slot('child'), isNull);
  });

  test('keeps finite singular and perspective Transform matrices valid', () {
    const matrices = <(String, List<Object?>)>[
      (
        'all-zero singular',
        <Object?>[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      ),
      (
        'invertible perspective sending the local origin to infinity',
        <Object?>[0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0],
      ),
    ];

    for (final (description, storage) in matrices) {
      final json = _modelJson();
      json['root'] = _node(
        '6408cfe9-e227-43de-916c-bb9d66224de9',
        'flutter.widgets.Transform',
        properties: {
          'transform': {'kind': 'matrix4', 'storage': storage},
        },
      );

      final decoded = _decode(json).root;
      final matrix =
          decoded.properties['transform']!.value as CanvasMatrix4Value;
      expect(
        matrix.storage,
        storage.map((value) => (value! as num).toDouble()).toList(),
        reason: description,
      );
    }
  });

  test('rejects invalid Transform values, slots, and flex children', () {
    Map<String, Object?> transform({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) => _node(
      '6408cfe9-e227-43de-916c-bb9d66224de9',
      'flutter.widgets.Transform',
      properties: properties,
      slots: slots,
    );

    Map<String, Object?> rootWith(Map<String, Object?> root) {
      final json = _modelJson();
      json['root'] = root;
      return json;
    }

    const identity = <String, Object?>{
      'transform': {
        'kind': 'matrix4',
        'storage': <Object?>[1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
      },
    };
    for (final invalid in <Map<String, Object?>>[
      const {},
      const {
        'transform': {
          'kind': 'matrix4',
          'storage': <Object?>[1, 0],
        },
      },
      const {
        'transform': {'kind': 'boolean', 'value': true},
      },
      {
        ...identity,
        'origin': const {'kind': 'offset', 'dx': 1},
      },
      {
        ...identity,
        'origin': const {'kind': 'offset', 'dx': 1, 'dy': 2, 'dz': 3},
      },
      {
        ...identity,
        'origin': const {'kind': 'size', 'width': 1, 'height': 2},
      },
      {
        ...identity,
        'transformHitTests': const {'kind': 'double', 'value': 1.0},
      },
      {
        ...identity,
        'filterQuality': const {
          'kind': 'enum',
          'type': 'FilterQuality',
          'value': 'ultra',
        },
      },
      {
        ...identity,
        'filterQuality': const {
          'kind': 'enum',
          'type': 'BoxFit',
          'value': 'high',
        },
      },
      {
        ...identity,
        'unknown': const {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(rootWith(transform(properties: invalid))),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }
    expect(
      () => _decode(
        rootWith(
          transform(properties: identity, slots: {'child': _list(const [])}),
        ),
      ),
      throwsFormatException,
    );

    final expanded = _node(
      'b670b67a-f3f7-4fca-abd7-8bb297b06709',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            '3d93189a-6a63-49d1-abe5-cb32319428fd',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Invalid flex child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(
        rootWith(
          transform(properties: identity, slots: {'child': _single(expanded)}),
        ),
      ),
      throwsFormatException,
    );

    final finiteJson = jsonEncode(
      rootWith(
        transform(
          properties: {
            ...identity,
            'origin': const {'kind': 'offset', 'dx': 987654321.125, 'dy': 0},
          },
        ),
      ),
    );
    final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
    expect(
      () => CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('must be finite'),
        ),
      ),
    );
  });

  test('Transform reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Transform\n');
    final end = contract.indexOf('W|flutter.widgets.UnconstrainedBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Transform\n'
      'P|alignment|alignmentGeometry|0|-|-|'
      'alignmentGeometry:alignmentGeometry\n'
      'P|filterQuality|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'FilterQuality:high,low,medium,none\n'
      'P|origin|offset|0|-|-|offset:offset:finiteSigned\n'
      'P|transform|matrix4|1|'
      'matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1|-|'
      'matrix4:matrix4\n'
      'P|transformHitTests|boolean|0|-|-|boolean:any\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes all 17 reviewed ListView leaves and its ordered children', () {
    final json = _modelJson();
    json['root'] = _node(
      '810bc9c3-0189-4bc4-bf7f-cea660474a58',
      'flutter.widgets.ListView',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
        'reverse': {'kind': 'boolean', 'value': true},
        'primary': {'kind': 'boolean', 'value': false},
        'physics': {'kind': 'string', 'value': 'rangeMaintaining'},
        'shrinkWrap': {'kind': 'boolean', 'value': true},
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 1,
          'top': 2,
          'end': 3,
          'bottom': 4,
        },
        'itemExtent': {'kind': 'double', 'value': 48.5},
        'addAutomaticKeepAlives': {'kind': 'boolean', 'value': false},
        'addRepaintBoundaries': {'kind': 'boolean', 'value': false},
        'addSemanticIndexes': {'kind': 'boolean', 'value': false},
        'scrollCacheExtent': {'kind': 'integer', 'value': 240},
        'semanticChildCount': {'kind': 'integer', 'value': 2},
        'dragStartBehavior': {
          'kind': 'enum',
          'type': 'DragStartBehavior',
          'value': 'down',
        },
        'keyboardDismissBehavior': {
          'kind': 'enum',
          'type': 'ScrollViewKeyboardDismissBehavior',
          'value': 'onDrag',
        },
        'restorationId': {'kind': 'string', 'value': 'primary-list'},
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
        'hitTestBehavior': {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'translucent',
        },
      },
      slots: {
        'children': _list([
          _node(
            '7a6d767e-9d7b-4cf1-9bfd-83f75383a08f',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'First'},
            },
          ),
          _node(
            '99de11d2-8f49-4efc-bb79-c6d0e89fcae6',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Second'},
            },
          ),
        ]),
      },
    );

    final listView = _decode(json).root;
    expect(listView.properties, hasLength(17));
    expect(listView.slot('children')!.children, hasLength(2));
    expect(listView.properties['physics']!.value, 'rangeMaintaining');
    expect(listView.properties['scrollCacheExtent']!.value, 240);
    expect(
      (listView.properties['hitTestBehavior']!.value as CanvasEnumValue).value,
      'translucent',
    );
  });

  test('rejects ListView values outside the reviewed closed contract', () {
    Map<String, Object?> invalid(String name, Map<String, Object?> value) {
      final json = _modelJson();
      json['root'] = _node(
        '6ea67702-c5b3-4332-a790-4a77a8a0d903',
        'flutter.widgets.ListView',
        properties: {name: value},
        slots: {'children': _list([])},
      );
      return json;
    }

    for (final json in <Map<String, Object?>>[
      invalid('physics', {'kind': 'string', 'value': 'custom'}),
      invalid('itemExtent', {'kind': 'double', 'value': -0.1}),
      invalid('scrollCacheExtent', {'kind': 'integer', 'value': -1}),
      invalid('restorationId', {'kind': 'string', 'value': ''}),
      invalid('scrollDirection', {
        'kind': 'enum',
        'type': 'Axis',
        'value': 'diagonal',
      }),
      invalid('hitTestBehavior', {
        'kind': 'enum',
        'type': 'HitTestBehavior',
        'value': 'ignore',
      }),
    ]) {
      expect(() => _decode(json), throwsFormatException);
    }
  });

  test('rejects ListView semanticChildCount above children length', () {
    final json = _modelJson();
    json['root'] = _node(
      '0654a243-6805-4131-a3fd-80f9873f424c',
      'flutter.widgets.ListView',
      properties: {
        'semanticChildCount': {'kind': 'integer', 'value': 1},
      },
      slots: {'children': _list([])},
    );

    expect(
      () => _decode(json),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('/properties/semanticChildCount'),
        ),
      ),
    );
  });

  test('ListView reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ListView\n');
    final end = contract.indexOf('W|flutter.widgets.MergeSemantics\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final block = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(17));
    expect(block, contains('S|children|list|0|0|10000|any\n'));
    expect(block, contains('P|scrollCacheExtent|double,integer|0|-|'));
  });

  test('decodes all 21 GridView.count leaves and ordered children', () {
    final json = _modelJson();
    json['root'] = _node(
      '7c5646ab-89dc-45b0-b147-f46604fc411f',
      'flutter.widgets.GridView',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
        'reverse': {'kind': 'boolean', 'value': true},
        'primary': {'kind': 'boolean', 'value': false},
        'physics': {'kind': 'string', 'value': 'bouncing'},
        'shrinkWrap': {'kind': 'boolean', 'value': true},
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 1.0,
          'top': 2.0,
          'end': 3.0,
          'bottom': 4.0,
        },
        'crossAxisCount': {'kind': 'integer', 'value': 2},
        'mainAxisSpacing': {'kind': 'double', 'value': 5.0},
        'crossAxisSpacing': {'kind': 'double', 'value': 6.0},
        'childAspectRatio': {'kind': 'double', 'value': 1.5},
        'mainAxisExtent': {'kind': 'double', 'value': 48.0},
        'addAutomaticKeepAlives': {'kind': 'boolean', 'value': false},
        'addRepaintBoundaries': {'kind': 'boolean', 'value': false},
        'addSemanticIndexes': {'kind': 'boolean', 'value': false},
        'scrollCacheExtent': {'kind': 'integer', 'value': 240},
        'semanticChildCount': {'kind': 'integer', 'value': 2},
        'dragStartBehavior': {
          'kind': 'enum',
          'type': 'DragStartBehavior',
          'value': 'down',
        },
        'keyboardDismissBehavior': {
          'kind': 'enum',
          'type': 'ScrollViewKeyboardDismissBehavior',
          'value': 'onDrag',
        },
        'restorationId': {'kind': 'string', 'value': 'primary-grid'},
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
        'hitTestBehavior': {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'translucent',
        },
      },
      slots: {
        'children': _list([
          _node(
            '0e15ae86-0c58-4298-b80e-2ab6b3a55371',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'First'},
            },
          ),
          _node(
            'c2fab615-373d-475e-81ca-76cc1d28460c',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Second'},
            },
          ),
        ]),
      },
    );

    final grid = _decode(json).root;
    expect(grid.type, 'flutter.widgets.GridView');
    expect(grid.properties, hasLength(21));
    expect(grid.properties['crossAxisCount']!.value, 2);
    expect(grid.properties['childAspectRatio']!.value, 1.5);
    expect(grid.properties['scrollCacheExtent']!.value, 240);
    expect(grid.slot('children')!.children.map((child) => child.id), [
      '0e15ae86-0c58-4298-b80e-2ab6b3a55371',
      'c2fab615-373d-475e-81ca-76cc1d28460c',
    ]);
  });

  test('requires positive GridView.count crossAxisCount with default 2', () {
    Map<String, Object?> model(Map<String, Object?> properties) {
      final json = _modelJson();
      json['root'] = _node(
        '7c5646ab-89dc-45b0-b147-f46604fc411f',
        'flutter.widgets.GridView',
        properties: properties,
        slots: {'children': _list([])},
      );
      return json;
    }

    expect(() => _decode(model({})), throwsFormatException);
    expect(
      () => _decode(
        model({
          'crossAxisCount': {'kind': 'integer', 'value': 0},
        }),
      ),
      throwsFormatException,
    );
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    expect(
      contract,
      contains('P|crossAxisCount|integer|1|integer:2|integer:1:1:'),
    );
  });

  test('rejects values outside the closed GridView.count projection', () {
    Map<String, Object?> invalid(String name, Map<String, Object?> value) {
      final json = _modelJson();
      json['root'] = _node(
        '7c5646ab-89dc-45b0-b147-f46604fc411f',
        'flutter.widgets.GridView',
        properties: {
          'crossAxisCount': {'kind': 'integer', 'value': 2},
          name: value,
        },
        slots: {'children': _list([])},
      );
      return json;
    }

    for (final json in <Map<String, Object?>>[
      invalid('physics', {'kind': 'string', 'value': 'custom'}),
      invalid('mainAxisSpacing', {'kind': 'double', 'value': -0.1}),
      invalid('crossAxisSpacing', {'kind': 'double', 'value': -0.1}),
      invalid('childAspectRatio', {'kind': 'double', 'value': 0.0}),
      invalid('mainAxisExtent', {'kind': 'double', 'value': -0.1}),
      invalid('restorationId', {'kind': 'string', 'value': ''}),
      invalid('mainAxisSpacing', {'kind': 'integer', 'value': 1}),
      invalid('scrollDirection', {
        'kind': 'enum',
        'type': 'Axis',
        'value': 'diagonal',
      }),
      invalid('hitTestBehavior', {
        'kind': 'enum',
        'type': 'HitTestBehavior',
        'value': 'ignore',
      }),
    ]) {
      expect(() => _decode(json), throwsFormatException);
    }
  });

  test('rejects GridView semanticChildCount above children length', () {
    final json = _modelJson();
    json['root'] = _node(
      '7c5646ab-89dc-45b0-b147-f46604fc411f',
      'flutter.widgets.GridView',
      properties: {
        'crossAxisCount': {'kind': 'integer', 'value': 2},
        'semanticChildCount': {'kind': 'integer', 'value': 1},
      },
      slots: {'children': _list([])},
    );

    expect(
      () => _decode(json),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('/properties/semanticChildCount'),
        ),
      ),
    );
  });

  test('GridView.count reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.GridView\n');
    final end = contract.indexOf('W|flutter.widgets.Icon\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final block = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(21));
    expect(block, contains('S|children|list|0|0|10000|any\n'));
    expect(block, isNot(contains('controller')));
    expect(block, isNot(contains('cacheExtent|')));
  });

  test('decodes all 10 SingleChildScrollView leaves and optional child', () {
    final json = _modelJson();
    json['root'] = _node(
      'cd068a37-bb45-49a3-a622-6ba944878d58',
      'flutter.widgets.SingleChildScrollView',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
        'reverse': {'kind': 'boolean', 'value': true},
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 1.0,
          'top': 2.0,
          'end': 3.0,
          'bottom': 4.0,
        },
        'primary': {'kind': 'boolean', 'value': false},
        'physics': {'kind': 'string', 'value': 'rangeMaintaining'},
        'dragStartBehavior': {
          'kind': 'enum',
          'type': 'DragStartBehavior',
          'value': 'down',
        },
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
        'hitTestBehavior': {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'translucent',
        },
        'restorationId': {'kind': 'string', 'value': 'single-scroll'},
        'keyboardDismissBehavior': {
          'kind': 'enum',
          'type': 'ScrollViewKeyboardDismissBehavior',
          'value': 'onDrag',
        },
      },
      slots: {
        'child': _single(
          _node(
            'f1009a48-969b-4cee-bcf7-8fe208ae8c9f',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Scrollable child'},
            },
          ),
        ),
      },
    );

    final scrollView = _decode(json).root;
    expect(scrollView.type, 'flutter.widgets.SingleChildScrollView');
    expect(scrollView.properties, hasLength(10));
    expect(scrollView.properties['physics']!.value, 'rangeMaintaining');
    expect(
      (scrollView.properties['hitTestBehavior']!.value as CanvasEnumValue)
          .value,
      'translucent',
    );
    expect(scrollView.slot('child')!.child!.type, 'flutter.widgets.Text');
  });

  test('accepts omitted SingleChildScrollView values and an empty child', () {
    final json = _modelJson();
    json['root'] = _node(
      'cd068a37-bb45-49a3-a622-6ba944878d58',
      'flutter.widgets.SingleChildScrollView',
      slots: {'child': _single(null)},
    );

    final scrollView = _decode(json).root;
    expect(scrollView.properties, isEmpty);
    expect(scrollView.slot('child')!.child, isNull);
  });

  test(
    'rejects values outside the SingleChildScrollView closed projection',
    () {
      Map<String, Object?> invalid(String name, Map<String, Object?> value) {
        final json = _modelJson();
        json['root'] = _node(
          'cd068a37-bb45-49a3-a622-6ba944878d58',
          'flutter.widgets.SingleChildScrollView',
          properties: {name: value},
          slots: {'child': _single(null)},
        );
        return json;
      }

      for (final json in <Map<String, Object?>>[
        invalid('physics', {'kind': 'string', 'value': 'custom'}),
        invalid('padding', {
          'kind': 'edgeInsets',
          'left': -1.0,
          'top': 0.0,
          'right': 0.0,
          'bottom': 0.0,
        }),
        invalid('restorationId', {'kind': 'string', 'value': ''}),
        invalid('scrollDirection', {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'diagonal',
        }),
        invalid('dragStartBehavior', {
          'kind': 'enum',
          'type': 'DragStartBehavior',
          'value': 'invalid',
        }),
        invalid('hitTestBehavior', {
          'kind': 'enum',
          'type': 'HitTestBehavior',
          'value': 'ignore',
        }),
        invalid('unknown', {'kind': 'boolean', 'value': true}),
      ]) {
        expect(() => _decode(json), throwsFormatException);
      }

      final wrongSlot = _modelJson();
      wrongSlot['root'] = _node(
        'cd068a37-bb45-49a3-a622-6ba944878d58',
        'flutter.widgets.SingleChildScrollView',
        slots: {'child': _list([])},
      );
      expect(() => _decode(wrongSlot), throwsFormatException);
    },
  );

  test('SingleChildScrollView reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.SingleChildScrollView\n');
    final end = contract.indexOf('W|flutter.widgets.SizedBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.SingleChildScrollView\n'
      'P|clipBehavior|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none\n'
      'P|dragStartBehavior|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:'
      'DragStartBehavior:down,start\n'
      'P|hitTestBehavior|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:'
      'HitTestBehavior:deferToChild,opaque,translucent\n'
      'P|keyboardDismissBehavior|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'ScrollViewKeyboardDismissBehavior:manual,onDrag\n'
      'P|padding|edgeInsets,edgeInsetsDirectional|0|-|'
      'edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|'
      'edgeInsets:edgeInsets:1:0:1:*:1;'
      'edgeInsetsDirectional:edgeInsets:1:0:1:*:1\n'
      'P|physics|string|0|-|-|string:pattern:'
      'KD86YWx3YXlzU2Nyb2xsYWJsZXxib3VuY2luZ3xjbGFtcGluZ3xuZXZlclNjcm9sbGFibGV8cGFnZXxyYW5nZU1haW50YWluaW5nKQ\n'
      'P|primary|boolean|0|-|-|boolean:any\n'
      'P|restorationId|string|0|-|-|string:length:1:256\n'
      'P|reverse|boolean|0|-|-|boolean:any\n'
      'P|scrollDirection|enum|0|-|-|enum:enum:'
      'cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'Axis:horizontal,vertical\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes all 54 reviewed TextField constructor leaves exactly', () {
    final json = _modelJson();
    json['root'] = _node(
      '73d9ec43-3d37-4304-8998-71fe51804284',
      'flutter.material.TextField',
      properties: {
        'keyboardType': {'kind': 'string', 'value': 'numberSignedDecimal'},
        'textInputAction': {
          'kind': 'enum',
          'type': 'TextInputAction',
          'value': 'done',
        },
        'textCapitalization': {
          'kind': 'enum',
          'type': 'TextCapitalization',
          'value': 'words',
        },
        'textAlign': {'kind': 'enum', 'type': 'TextAlign', 'value': 'center'},
        'textAlignVertical': {'kind': 'string', 'value': 'bottom'},
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'ltr',
        },
        'readOnly': {'kind': 'boolean', 'value': true},
        'showCursor': {'kind': 'boolean', 'value': false},
        'autofocus': {'kind': 'boolean', 'value': true},
        'obscuringCharacter': {'kind': 'string', 'value': '•'},
        'obscureText': {'kind': 'boolean', 'value': false},
        'autocorrect': {'kind': 'boolean', 'value': false},
        'smartDashesType': {
          'kind': 'enum',
          'type': 'SmartDashesType',
          'value': 'disabled',
        },
        'smartQuotesType': {
          'kind': 'enum',
          'type': 'SmartQuotesType',
          'value': 'enabled',
        },
        'enableSuggestions': {'kind': 'boolean', 'value': false},
        'maxLines': {'kind': 'integer', 'value': 4},
        'minLines': {'kind': 'integer', 'value': 2},
        'expands': {'kind': 'boolean', 'value': false},
        'maxLength': {'kind': 'integer', 'value': -1},
        'maxLengthEnforcement': {
          'kind': 'enum',
          'type': 'MaxLengthEnforcement',
          'value': 'truncateAfterCompositionEnds',
        },
        'onChanged': {'kind': 'callbackPresence'},
        'onEditingComplete': {'kind': 'callbackPresence'},
        'onSubmitted': {'kind': 'callbackPresence'},
        'onAppPrivateCommand': {'kind': 'callbackPresence'},
        'enabled': {'kind': 'boolean', 'value': true},
        'ignorePointers': {'kind': 'boolean', 'value': false},
        'cursorWidth': {'kind': 'integer', 'value': 3},
        'cursorHeight': {'kind': 'double', 'value': 22.5},
        'cursorRadiusX': {'kind': 'double', 'value': 4.0},
        'cursorRadiusY': {'kind': 'double', 'value': 6.0},
        'cursorOpacityAnimates': {'kind': 'boolean', 'value': true},
        'cursorColor': {
          'kind': 'themeToken',
          'token': 'material.colorScheme.primary',
        },
        'cursorErrorColor': {'kind': 'color', 'argb': '0xFFAA1122'},
        'selectionHeightStyle': {
          'kind': 'enum',
          'type': 'BoxHeightStyle',
          'value': 'includeLineSpacingBottom',
        },
        'selectionWidthStyle': {
          'kind': 'enum',
          'type': 'BoxWidthStyle',
          'value': 'max',
        },
        'keyboardAppearance': {
          'kind': 'enum',
          'type': 'Brightness',
          'value': 'dark',
        },
        'scrollPaddingLeft': {'kind': 'double', 'value': 1.0},
        'scrollPaddingTop': {'kind': 'double', 'value': 2.0},
        'scrollPaddingRight': {'kind': 'double', 'value': 3.0},
        'scrollPaddingBottom': {'kind': 'double', 'value': 4.0},
        'dragStartBehavior': {
          'kind': 'enum',
          'type': 'DragStartBehavior',
          'value': 'down',
        },
        'enableInteractiveSelection': {'kind': 'boolean', 'value': false},
        'selectAllOnFocus': {'kind': 'boolean', 'value': true},
        'onTap': {'kind': 'callbackPresence'},
        'onTapAlwaysCalled': {'kind': 'boolean', 'value': true},
        'onTapOutside': {'kind': 'callbackPresence'},
        'onTapUpOutside': {'kind': 'callbackPresence'},
        'mouseCursor': {'kind': 'string', 'value': 'resizeColumn'},
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
        'restorationId': {'kind': 'string', 'value': 'profile-name'},
        'stylusHandwritingEnabled': {'kind': 'boolean', 'value': false},
        'enableIMEPersonalizedLearning': {'kind': 'boolean', 'value': false},
        'enableInlinePrediction': {'kind': 'boolean', 'value': true},
        'canRequestFocus': {'kind': 'boolean', 'value': false},
      },
    );

    final textField = _decode(json).root;
    expect(textField.type, 'flutter.material.TextField');
    expect(textField.properties, hasLength(54));
    expect(textField.slots, isEmpty);
    expect(textField.properties['maxLength']!.value, -1);
    expect(textField.properties['cursorWidth']!.kind, 'integer');
    expect(
      (textField.properties['selectionHeightStyle']!.value as CanvasEnumValue)
          .type,
      'BoxHeightStyle',
    );
    expect(textField.properties['onTapUpOutside']!.value, isTrue);
  });

  test('closes TextField presets, bounds, and constructor relationships', () {
    Map<String, Object?> model(Map<String, Object?> properties) {
      final json = _modelJson();
      json['root'] = _node(
        '73d9ec43-3d37-4304-8998-71fe51804284',
        'flutter.material.TextField',
        properties: properties,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      {
        'cursorRadiusX': {'kind': 'double', 'value': 1.0},
      },
      {
        'scrollPaddingLeft': {'kind': 'double', 'value': 1.0},
      },
      {
        'minLines': {'kind': 'integer', 'value': 2},
      },
      {
        'maxLines': {'kind': 'integer', 'value': 2},
        'minLines': {'kind': 'integer', 'value': 3},
      },
      {
        'expands': {'kind': 'boolean', 'value': true},
        'maxLines': {'kind': 'integer', 'value': 2},
      },
      {
        'obscureText': {'kind': 'boolean', 'value': true},
        'maxLines': {'kind': 'integer', 'value': 2},
      },
      {
        'obscureText': {'kind': 'boolean', 'value': true},
        'expands': {'kind': 'boolean', 'value': true},
      },
      {
        'maxLength': {'kind': 'integer', 'value': 0},
      },
      {
        'keyboardType': {'kind': 'string', 'value': 'text'},
        'textInputAction': {
          'kind': 'enum',
          'type': 'TextInputAction',
          'value': 'newline',
        },
        'maxLines': {'kind': 'integer', 'value': 2},
      },
      {
        'keyboardType': {'kind': 'string', 'value': 'custom'},
      },
      {
        'cursorRadiusX': {'kind': 'integer', 'value': 1},
        'cursorRadiusY': {'kind': 'double', 'value': 1.0},
      },
      {
        'scrollPaddingLeft': {'kind': 'integer', 'value': 1},
        'scrollPaddingTop': {'kind': 'double', 'value': 1.0},
        'scrollPaddingRight': {'kind': 'double', 'value': 1.0},
        'scrollPaddingBottom': {'kind': 'double', 'value': 1.0},
      },
    ]) {
      expect(
        () => _decode(model(properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }

    for (final value in <String>[
      'A',
      '•',
      '\u0000',
      '\uD7FF',
      '\uE000',
      '\uFFFF',
    ]) {
      expect(
        () => _decode(
          model({
            'obscuringCharacter': {'kind': 'string', 'value': value},
          }),
        ),
        returnsNormally,
        reason: value.codeUnits.toString(),
      );
    }
    for (final value in <String>['', 'ab', '\u{1F600}', '\uD800', '\uDC00']) {
      expect(
        () => _decode(
          model({
            'obscuringCharacter': {'kind': 'string', 'value': value},
          }),
        ),
        throwsFormatException,
        reason: value.codeUnits.toString(),
      );
    }

    for (final properties in <Map<String, Object?>>[
      {
        'expands': {'kind': 'boolean', 'value': true},
      },
      {
        'textInputAction': {
          'kind': 'enum',
          'type': 'TextInputAction',
          'value': 'newline',
        },
        'maxLines': {'kind': 'integer', 'value': 2},
      },
      {
        'keyboardType': {'kind': 'string', 'value': 'multiline'},
        'textInputAction': {
          'kind': 'enum',
          'type': 'TextInputAction',
          'value': 'newline',
        },
        'maxLines': {'kind': 'integer', 'value': 2},
      },
      {
        'maxLength': {'kind': 'integer', 'value': -1},
      },
    ]) {
      expect(
        () => _decode(model(properties)),
        returnsNormally,
        reason: properties.toString(),
      );
    }
  });

  test('TextField reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.material.TextField\n');
    final end = contract.indexOf('W|flutter.material.VerticalDivider\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final block = contract.substring(start, end);
    final bytes = utf8.encode(block);
    expect(bytes, hasLength(8076));
    expect(
      sha256Hex(bytes),
      '0cae00ba20bef22302e2b2db29fafbe19a535e51d9791416d670e6881162f61f',
    );
    expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(54));
    expect(RegExp(r'^S\|', multiLine: true).allMatches(block), isEmpty);
    expect(RegExp(r'^R\|', multiLine: true).allMatches(block), isEmpty);
  });

  test('decodes the exact AspectRatio contract and optional child slot', () {
    Map<String, Object?> model({
      required Map<String, Object?> properties,
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '59b8460c-29e5-4922-813d-9412a977ed9c',
        'flutter.widgets.AspectRatio',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final text = _node(
      'fd4a62c3-99f4-4e38-b55a-90b97d187b5c',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Ratio child'},
      },
    );
    final decoded = _decode(
      model(
        properties: {
          'aspectRatio': {'kind': 'double', 'value': 16 / 9},
        },
        slots: {'child': _single(text)},
      ),
    ).root;

    expect(decoded.type, 'flutter.widgets.AspectRatio');
    expect(decoded.properties.keys, const ['aspectRatio']);
    expect(decoded.properties['aspectRatio']!.kind, 'double');
    expect(decoded.properties['aspectRatio']!.value, closeTo(16 / 9, 1e-12));
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    final explicitEmpty = _decode(
      model(
        properties: const {
          'aspectRatio': {'kind': 'double', 'value': 1.0},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    expect(explicitEmpty.slot('child')!.child, isNull);

    final omittedSlot = _decode(
      model(
        properties: const {
          'aspectRatio': {'kind': 'double', 'value': 1.0},
        },
      ),
    ).root;
    expect(omittedSlot.slot('child'), isNull);

    expect(
      () => _decode(model(properties: const {})),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('flutter.widgets.AspectRatio.aspectRatio'),
        ),
      ),
    );
    for (final candidate in const <Map<String, Object?>>[
      {'kind': 'double', 'value': 0.0},
      {'kind': 'double', 'value': -0.01},
      {'kind': 'integer', 'value': 1},
      {'kind': 'boolean', 'value': true},
    ]) {
      expect(
        () => _decode(model(properties: {'aspectRatio': candidate})),
        throwsFormatException,
        reason: '${candidate['kind']}:${candidate['value']}',
      );
    }

    final finiteJson = jsonEncode(
      model(
        properties: const {
          'aspectRatio': {'kind': 'double', 'value': 987654321.125},
        },
      ),
    );
    final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
    expect(
      () => CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('must be finite'),
        ),
      ),
    );
  });

  test('AspectRatio reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.AspectRatio\n');
    final end = contract.indexOf('W|flutter.widgets.Baseline\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.AspectRatio\n'
      'P|aspectRatio|double|1|double:1|double:0:0:*:1|'
      'double:range:0:0:*:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test(
    'decodes every finite Baseline value, baseline type, and child shape',
    () {
      Map<String, Object?> model({
        required double baseline,
        required String baselineType,
        Map<String, Object?>? child,
        bool includeSlot = true,
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae',
          'flutter.widgets.Baseline',
          properties: {
            'baseline': {'kind': 'double', 'value': baseline},
            'baselineType': {
              'kind': 'enum',
              'type': 'TextBaseline',
              'value': baselineType,
            },
          },
          slots: includeSlot ? {'child': _single(child)} : const {},
        );
        return json;
      }

      final child = _node(
        '0197b4c0-11f0-45b2-a191-d9b1b00a21ce',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Baseline child'},
        },
      );
      for (final baselineCase in const [
        (value: -24.5, type: 'alphabetic'),
        (value: 0.0, type: 'ideographic'),
        (value: 175.25, type: 'alphabetic'),
      ]) {
        final decoded = _decode(
          model(
            baseline: baselineCase.value,
            baselineType: baselineCase.type,
            child: child,
          ),
        ).root;
        expect(decoded.type, 'flutter.widgets.Baseline');
        expect(decoded.properties.keys, const ['baseline', 'baselineType']);
        expect(decoded.properties['baseline']!.kind, 'double');
        expect(decoded.properties['baseline']!.value, baselineCase.value);
        final type =
            decoded.properties['baselineType']!.value as CanvasEnumValue;
        expect(type.type, 'TextBaseline');
        expect(type.value, baselineCase.type);
        expect(decoded.slot('child')!.child!.id, child['id']);
      }

      expect(
        _decode(
          model(baseline: 24, baselineType: 'alphabetic'),
        ).root.slot('child')!.child,
        isNull,
      );
      expect(
        _decode(
          model(baseline: 24, baselineType: 'alphabetic', includeSlot: false),
        ).root.slot('child'),
        isNull,
      );
    },
  );

  test('rejects malformed Baseline properties and child slots', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae',
        'flutter.widgets.Baseline',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    const validProperties = <String, Object?>{
      'baseline': {'kind': 'double', 'value': 24.0},
      'baselineType': {
        'kind': 'enum',
        'type': 'TextBaseline',
        'value': 'alphabetic',
      },
    };
    for (final properties in <Map<String, Object?>>[
      const {
        'baselineType': {
          'kind': 'enum',
          'type': 'TextBaseline',
          'value': 'alphabetic',
        },
      },
      const {
        'baseline': {'kind': 'double', 'value': 24.0},
      },
      {
        ...validProperties,
        'baseline': const {'kind': 'integer', 'value': 24},
      },
      {
        ...validProperties,
        'baselineType': const {
          'kind': 'enum',
          'type': 'TextBaseline',
          'value': 'central',
        },
      },
      {
        ...validProperties,
        'baselineType': const {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'alphabetic',
        },
      },
      {
        ...validProperties,
        'unknown': const {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }
    expect(
      () => _decode(
        model(properties: validProperties, slots: {'child': _list(const [])}),
      ),
      throwsFormatException,
    );

    final finiteJson = jsonEncode(
      model(
        properties: const {
          'baseline': {'kind': 'double', 'value': 987654321.125},
          'baselineType': {
            'kind': 'enum',
            'type': 'TextBaseline',
            'value': 'ideographic',
          },
        },
      ),
    );
    final nonFiniteJson = finiteJson.replaceFirst('987654321.125', '1e309');
    expect(
      () => CanvasModel.decode(Uint8List.fromList(utf8.encode(nonFiniteJson))),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('must be finite'),
        ),
      ),
    );
  });

  test('Baseline reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Baseline\n');
    final end = contract.indexOf('W|flutter.widgets.BlockSemantics\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Baseline\n'
      'P|baseline|double|1|double:24|double:*:1:*:1|'
      'double:range:*:1:*:1\n'
      'P|baselineType|enum|1|enum:TextBaseline:alphabetic|-|'
      'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
      'TextBaseline:alphabetic,ideographic\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the exact Opacity contract, semantics, bounds, and child', () {
    Map<String, Object?> model({
      required Map<String, Object?> properties,
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '99a8d608-2729-47f3-8fa0-a805d44cb8b7',
        'flutter.widgets.Opacity',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final text = _node(
      'f7cda0d7-7afd-4922-853c-218b698cc8f1',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Faded child'},
      },
    );
    final decoded = _decode(
      model(
        properties: const {
          'opacity': {'kind': 'double', 'value': 0.5},
          'alwaysIncludeSemantics': {'kind': 'boolean', 'value': true},
        },
        slots: {'child': _single(text)},
      ),
    ).root;
    expect(decoded.type, 'flutter.widgets.Opacity');
    expect(decoded.properties.keys, const [
      'opacity',
      'alwaysIncludeSemantics',
    ]);
    expect(decoded.properties['opacity']!.value, 0.5);
    expect(decoded.properties['alwaysIncludeSemantics']!.value, isTrue);
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    for (final value in const [0.0, 1.0]) {
      final endpoint = _decode(
        model(
          properties: {
            'opacity': {'kind': 'double', 'value': value},
          },
        ),
      ).root;
      expect(endpoint.properties['opacity']!.value, value);
      expect(
        endpoint.properties.containsKey('alwaysIncludeSemantics'),
        isFalse,
      );
      expect(endpoint.slot('child'), isNull);
    }
    final explicitFalse = _decode(
      model(
        properties: const {
          'opacity': {'kind': 'double', 'value': 1.0},
          'alwaysIncludeSemantics': {'kind': 'boolean', 'value': false},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    expect(explicitFalse.properties['alwaysIncludeSemantics']!.value, isFalse);
    expect(explicitFalse.slot('child')!.child, isNull);

    expect(
      () => _decode(model(properties: const {})),
      throwsA(
        isA<FormatException>().having(
          (failure) => failure.message,
          'message',
          contains('flutter.widgets.Opacity.opacity'),
        ),
      ),
    );
    for (final candidate in const <Map<String, Object?>>[
      {'kind': 'double', 'value': -0.0001},
      {'kind': 'double', 'value': 1.0001},
      {'kind': 'integer', 'value': 0},
      {'kind': 'boolean', 'value': true},
    ]) {
      expect(
        () => _decode(model(properties: {'opacity': candidate})),
        throwsFormatException,
        reason: '${candidate['kind']}:${candidate['value']}',
      );
    }
    expect(
      () => _decode(
        model(
          properties: const {
            'opacity': {'kind': 'double', 'value': 0.5},
            'alwaysIncludeSemantics': {'kind': 'double', 'value': 0.0},
          },
        ),
      ),
      throwsFormatException,
    );
  });

  test('Opacity reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Opacity\n');
    final end = contract.indexOf('W|flutter.widgets.OverflowBar\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.Opacity\n'
      'P|alwaysIncludeSemantics|boolean|0|-|-|boolean:any\n'
      'P|opacity|double|1|double:1|double:0:1:1:1|'
      'double:range:0:1:1:1\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes exact Placeholder defaults, values, and optional child', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'b4d88c9a-9eca-4a53-92a6-71f1631ef235',
        'flutter.widgets.Placeholder',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final omitted = _decode(model()).root;
    expect(omitted.type, 'flutter.widgets.Placeholder');
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child'), isNull);

    final child = _node(
      '5cdf8725-c754-4590-873e-c2e60e8c8872',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Placeholder child'},
      },
    );
    final explicit = _decode(
      model(
        properties: const {
          'color': {'kind': 'color', 'argb': '0xFF123456'},
          'strokeWidth': {'kind': 'double', 'value': 3.5},
          'fallbackWidth': {'kind': 'integer', 'value': 240},
          'fallbackHeight': {'kind': 'double', 'value': 120.25},
        },
        slots: {'child': _single(child)},
      ),
    ).root;
    expect(explicit.properties.keys, const [
      'color',
      'strokeWidth',
      'fallbackWidth',
      'fallbackHeight',
    ]);
    expect(explicit.properties['color']!.value, 0xff123456);
    expect(explicit.properties['strokeWidth']!.value, 3.5);
    expect(explicit.properties['fallbackWidth']!.value, 240);
    expect(explicit.properties['fallbackHeight']!.value, 120.25);
    expect(explicit.slot('child')!.child!.type, 'flutter.widgets.Text');

    final themed = _decode(
      model(
        properties: const {
          'color': {
            'kind': 'themeToken',
            'token': 'material.colorScheme.secondaryContainer',
          },
          'strokeWidth': {'kind': 'integer', 'value': 0},
          'fallbackWidth': {'kind': 'double', 'value': 0.0},
          'fallbackHeight': {'kind': 'integer', 'value': 0},
        },
        slots: {'child': _single(null)},
      ),
    ).root;
    expect(
      (themed.properties['color']!.value as CanvasThemeToken).wireId,
      'material.colorScheme.secondaryContainer',
    );
    expect(themed.slot('child')!.child, isNull);
  });

  test('rejects unsupported or non-finite Placeholder branches', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'b4d88c9a-9eca-4a53-92a6-71f1631ef235',
        'flutter.widgets.Placeholder',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final invalid in <Map<String, Object?>>[
      model(
        properties: const {
          'color': {'kind': 'string', 'value': 'blueGrey'},
        },
      ),
      model(
        properties: const {
          'color': {
            'kind': 'themeToken',
            'token': 'material.colorScheme.notReviewed',
          },
        },
      ),
      model(
        properties: const {
          'strokeWidth': {'kind': 'double', 'value': -0.01},
        },
      ),
      model(
        properties: const {
          'fallbackWidth': {'kind': 'integer', 'value': -1},
        },
      ),
      model(
        properties: const {
          'fallbackHeight': {'kind': 'integer', 'value': 9007199254740992},
        },
      ),
      model(
        properties: const {
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
      ),
      model(
        slots: {
          'child': {'kind': 'list', 'children': <Object?>[]},
        },
      ),
      model(slots: {'futureSlot': _single(null)}),
    ]) {
      expect(
        () => _decode(invalid),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }

    final finite = jsonEncode(
      model(
        properties: const {
          'fallbackHeight': {'kind': 'double', 'value': 1.0},
        },
      ),
    );
    expect(
      () => CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(finite.replaceFirst('"value":1.0', '"value":1e400')),
        ),
      ),
      throwsFormatException,
    );

    final expanded = _node(
      '028eb9bb-9824-46e0-a787-34036c35d48a',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            '5cdf8725-c754-4590-873e-c2e60e8c8872',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under Placeholder',
    );
  });

  test('Placeholder reviewed schema is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Placeholder\n');
    final end = contract.indexOf('W|flutter.widgets.RepaintBoundary\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final slice = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(slice), hasLength(4));
    expect(RegExp(r'^S\|', multiLine: true).allMatches(slice), hasLength(1));
    expect(
      slice,
      startsWith(
        'W|flutter.widgets.Placeholder\n'
        'P|color|color,themeToken|0|-|-|color:any;'
        'themeToken:tokens:material.colorScheme.error,',
      ),
    );
    expect(
      slice,
      endsWith(
        'P|fallbackHeight|double,integer|0|-|double:0:1:*:1;'
        'integer:0:1:9007199254740991:1|double:range:0:1:*:1;'
        'integer:range:0:1:9007199254740991:1\n'
        'P|fallbackWidth|double,integer|0|-|double:0:1:*:1;'
        'integer:0:1:9007199254740991:1|double:range:0:1:*:1;'
        'integer:range:0:1:9007199254740991:1\n'
        'P|strokeWidth|double,integer|0|-|double:0:1:*:1;'
        'integer:0:1:9007199254740991:1|double:range:0:1:*:1;'
        'integer:range:0:1:9007199254740991:1\n'
        'S|child|single|0|0|1|any\n',
      ),
    );
  });

  test(
    'decodes the exact ColoredBox color, anti-alias, and optional child contract',
    () {
      Map<String, Object?> model({
        required Map<String, Object?> properties,
        Map<String, Object?> slots = const {},
      }) {
        final json = _modelJson();
        json['root'] = _node(
          'ec949ebe-9c66-48b7-8901-c691d7356e07',
          'flutter.widgets.ColoredBox',
          properties: properties,
          slots: slots,
        );
        return json;
      }

      final child = _node(
        '01d263fe-dc50-4ba9-823d-fd5271db0590',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Blue child'},
        },
      );
      final literal = _decode(
        model(
          properties: const {
            'color': {'kind': 'color', 'argb': '0xFF2196F3'},
            'isAntiAlias': {'kind': 'boolean', 'value': false},
          },
          slots: {'child': _single(child)},
        ),
      ).root;
      expect(literal.type, 'flutter.widgets.ColoredBox');
      expect(literal.properties.keys, const ['color', 'isAntiAlias']);
      expect(literal.properties['color']!.value, 0xff2196f3);
      expect(literal.properties['isAntiAlias']!.value, isFalse);
      expect(literal.slot('child')!.child!.type, 'flutter.widgets.Text');

      final themed = _decode(
        model(
          properties: const {
            'color': {
              'kind': 'themeToken',
              'token': 'material.colorScheme.primaryContainer',
            },
          },
          slots: {'child': _single(null)},
        ),
      ).root;
      expect(
        (themed.properties['color']!.value as CanvasThemeToken).wireId,
        'material.colorScheme.primaryContainer',
      );
      expect(themed.properties.containsKey('isAntiAlias'), isFalse);
      expect(themed.slot('child')!.child, isNull);

      final withoutSlot = _decode(
        model(
          properties: const {
            'color': {'kind': 'color', 'argb': '0x00000000'},
          },
        ),
      ).root;
      expect(withoutSlot.properties['color']!.value, 0);
      expect(withoutSlot.slot('child'), isNull);
    },
  );

  test('rejects every unsupported ColoredBox property and slot branch', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'ec949ebe-9c66-48b7-8901-c691d7356e07',
        'flutter.widgets.ColoredBox',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    for (final properties in <Map<String, Object?>>[
      const {},
      const {
        'color': {'kind': 'color', 'argb': '0xff2196f3'},
      },
      const {
        'color': {
          'kind': 'themeToken',
          'token': 'material.colorScheme.notReviewed',
        },
      },
      const {
        'color': {'kind': 'string', 'value': 'blue'},
      },
      const {
        'color': {'kind': 'color', 'argb': '0xFF2196F3'},
        'isAntiAlias': {'kind': 'integer', 'value': 1},
      },
      const {
        'color': {'kind': 'color', 'argb': '0xFF2196F3'},
        'futureProperty': {'kind': 'boolean', 'value': true},
      },
    ]) {
      expect(
        () => _decode(model(properties: properties)),
        throwsFormatException,
        reason: properties.toString(),
      );
    }

    const color = {
      'color': {'kind': 'color', 'argb': '0xFF2196F3'},
    };
    expect(
      () => _decode(
        model(
          properties: color,
          slots: {
            'child': {'kind': 'list', 'children': <Object?>[]},
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        model(properties: color, slots: {'futureSlot': _single(null)}),
      ),
      throwsFormatException,
    );
    final expanded = _node(
      '48640f23-0589-4bcc-a769-79cd23d05065',
      'flutter.widgets.Expanded',
      slots: {
        'child': _single(
          _node(
            '211f6d05-a9f3-4ced-81a0-2f5e36b64e0e',
            'flutter.widgets.Text',
            properties: {
              'data': {'kind': 'string', 'value': 'Flex-only child'},
            },
          ),
        ),
      },
    );
    expect(
      () => _decode(
        model(properties: color, slots: {'child': _single(expanded)}),
      ),
      throwsFormatException,
      reason: 'a ParentData child cannot be reparented under ColoredBox',
    );
  });

  test('ColoredBox reviewed schema is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ColoredBox\n');
    final end = contract.indexOf('W|flutter.widgets.Column\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final slice = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(slice), hasLength(2));
    expect(RegExp(r'^S\|', multiLine: true).allMatches(slice), hasLength(1));
    expect(
      slice,
      startsWith(
        'W|flutter.widgets.ColoredBox\n'
        'P|color|color,themeToken|1|color:0xFF2196F3|-|color:any;'
        'themeToken:tokens:material.colorScheme.error,',
      ),
    );
    expect(
      slice,
      endsWith(
        'P|isAntiAlias|boolean|0|-|-|boolean:any\n'
        'S|child|single|0|0|1|any\n',
      ),
    );
  });

  test(
    'decodes the exact Directionality LTR and RTL required-child contract',
    () {
      Map<String, Object?> model({
        required String direction,
        Map<String, Object?>? child,
      }) {
        final json = _modelJson();
        json['root'] = _node(
          '1dd83790-acde-4aa4-8d58-4ab79f08042d',
          'flutter.widgets.Directionality',
          properties: {
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': direction,
            },
          },
          slots: {'child': _single(child)},
        );
        return json;
      }

      final child = _node(
        'd2156ed9-c715-4bb4-a245-70326cafba93',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Directional child'},
        },
      );
      for (final direction in const ['ltr', 'rtl']) {
        final node = _decode(model(direction: direction, child: child)).root;
        expect(node.type, 'flutter.widgets.Directionality');
        final value =
            node.properties['textDirection']!.value as CanvasEnumValue;
        expect(value.type, 'TextDirection');
        expect(value.value, direction);
        expect(node.slot('child')!.children, hasLength(1));
        expect(node.slot('child')!.child!.type, 'flutter.widgets.Text');
      }
    },
  );

  test('rejects incomplete or unsupported Directionality branches', () {
    final text = _node(
      'd2156ed9-c715-4bb4-a245-70326cafba93',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Directional child'},
      },
    );
    Map<String, Object?> model({
      Map<String, Object?> properties = const {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'ltr',
        },
      },
      Map<String, Object?>? child,
      Map<String, Object?>? slots,
    }) {
      final json = _modelJson();
      json['root'] = _node(
        '1dd83790-acde-4aa4-8d58-4ab79f08042d',
        'flutter.widgets.Directionality',
        properties: properties,
        slots: slots ?? {'child': _single(child)},
      );
      return json;
    }

    for (final invalid in <Map<String, Object?>>[
      model(properties: const {}, child: text),
      model(child: null),
      model(
        properties: const {
          'textDirection': {'kind': 'string', 'value': 'ltr'},
        },
        child: text,
      ),
      model(
        properties: const {
          'textDirection': {
            'kind': 'enum',
            'type': 'Axis',
            'value': 'horizontal',
          },
        },
        child: text,
      ),
      model(
        properties: const {
          'textDirection': {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': 'automatic',
          },
        },
        child: text,
      ),
      model(
        properties: const {
          'textDirection': {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': 'ltr',
          },
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
        child: text,
      ),
      model(
        slots: {
          'child': {
            'kind': 'list',
            'children': [text],
          },
        },
      ),
      model(slots: {'child': _single(text), 'futureSlot': _single(null)}),
    ]) {
      expect(() => _decode(invalid), throwsFormatException);
    }

    final expanded = _node(
      '47184ff3-7c45-49e5-9039-3079417a7c67',
      'flutter.widgets.Expanded',
      slots: {'child': _single(text)},
    );
    for (final unsafe in <Map<String, Object?>>[
      expanded,
      _node(
        '47184ff3-7c45-49e5-9039-3079417a7c68',
        'flutter.widgets.Flexible',
        slots: {'child': _single(text)},
      ),
      _node('47184ff3-7c45-49e5-9039-3079417a7c69', 'flutter.widgets.Spacer'),
    ]) {
      expect(
        () => _decode(model(child: unsafe)),
        throwsFormatException,
        reason: 'Directionality must not break a flex ParentData path',
      );
    }
  });

  test(
    'Directionality reviewed schema and wrapper creation contract are exact',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|flutter.widgets.Directionality\n');
      final end = contract.indexOf('W|flutter.widgets.ExcludeFocus\n', start);
      expect(start, greaterThanOrEqualTo(0));
      expect(end, greaterThan(start));
      expect(
        contract.substring(start, end),
        'W|flutter.widgets.Directionality\n'
        'P|textDirection|enum|1|enum:TextDirection:ltr|-|'
        'enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:'
        'TextDirection:ltr,rtl\n'
        'S|child|single|1|1|1|any\n'
        'C|flutter.widgets.Directionality|paletteCreate|'
        'wrapExistingChild|child\n',
      );
    },
  );

  test('decodes the exact SafeArea properties and required child contract', () {
    Map<String, Object?> model({
      required Map<String, Object?> properties,
      required Map<String, Object?> slots,
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'df5babd2-16cf-44c4-b497-24375532ec68',
        'flutter.widgets.SafeArea',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final child = _node(
      'b84ced1f-049e-479a-a5c1-6940a09ca2b3',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Safe child'},
      },
    );
    final explicit = _decode(
      model(
        properties: const {
          'left': {'kind': 'boolean', 'value': false},
          'top': {'kind': 'boolean', 'value': true},
          'right': {'kind': 'boolean', 'value': false},
          'bottom': {'kind': 'boolean', 'value': true},
          'minimum': {
            'kind': 'edgeInsets',
            'left': -4,
            'top': 5.5,
            'right': -6.25,
            'bottom': 7,
          },
          'maintainBottomViewPadding': {'kind': 'boolean', 'value': true},
        },
        slots: {'child': _single(child)},
      ),
    ).root;
    expect(explicit.type, 'flutter.widgets.SafeArea');
    expect(explicit.properties, hasLength(6));
    expect(explicit.properties['left']!.value, isFalse);
    expect(explicit.properties['top']!.value, isTrue);
    expect(explicit.properties['right']!.value, isFalse);
    expect(explicit.properties['bottom']!.value, isTrue);
    expect(explicit.properties['maintainBottomViewPadding']!.value, isTrue);
    final minimum = explicit.properties['minimum']!.value as CanvasEdgeInsets;
    expect(
      [minimum.left, minimum.top, minimum.right, minimum.bottom],
      [-4, 5.5, -6.25, 7],
    );
    expect(explicit.slot('child')!.child!.type, 'flutter.widgets.Text');

    final omitted = _decode(
      model(properties: const {}, slots: {'child': _single(child)}),
    ).root;
    expect(omitted.properties, isEmpty);
    expect(omitted.slot('child')!.children, hasLength(1));
  });

  test('rejects unsupported SafeArea value, slot, and child branches', () {
    Map<String, Object?> model({
      Map<String, Object?> properties = const {},
      Map<String, Object?> slots = const {},
    }) {
      final json = _modelJson();
      json['root'] = _node(
        'df5babd2-16cf-44c4-b497-24375532ec68',
        'flutter.widgets.SafeArea',
        properties: properties,
        slots: slots,
      );
      return json;
    }

    final text = _node(
      'b84ced1f-049e-479a-a5c1-6940a09ca2b3',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Safe child'},
      },
    );
    for (final invalid in <Map<String, Object?>>[
      model(),
      model(slots: {'child': _single(null)}),
      model(
        properties: const {
          'minimum': {
            'kind': 'edgeInsetsDirectional',
            'start': 1,
            'top': 2,
            'end': 3,
            'bottom': 4,
          },
        },
        slots: {'child': _single(text)},
      ),
      model(
        properties: const {
          'left': {'kind': 'integer', 'value': 1},
        },
        slots: {'child': _single(text)},
      ),
      model(
        properties: const {
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
        slots: {'child': _single(text)},
      ),
      model(
        slots: {
          'child': {
            'kind': 'list',
            'children': [text],
          },
        },
      ),
      model(slots: {'child': _single(text), 'futureSlot': _single(null)}),
    ]) {
      expect(() => _decode(invalid), throwsFormatException);
    }

    final expanded = _node(
      '47184ff3-7c45-49e5-9039-3079417a7c67',
      'flutter.widgets.Expanded',
      slots: {'child': _single(text)},
    );
    expect(
      () => _decode(model(slots: {'child': _single(expanded)})),
      throwsFormatException,
      reason: 'SafeArea cannot become the RenderFlex parent of Expanded',
    );
  });

  test('SafeArea reviewed schema and wrapper creation contract are exact', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.SafeArea\n');
    final end = contract.indexOf(
      'W|flutter.widgets.SingleChildScrollView\n',
      start,
    );
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.SafeArea\n'
      'P|bottom|boolean|0|-|-|boolean:any\n'
      'P|left|boolean|0|-|-|boolean:any\n'
      'P|maintainBottomViewPadding|boolean|0|-|-|boolean:any\n'
      'P|minimum|edgeInsets|0|-|edgeInsets:*:1:*:1|'
      'edgeInsets:edgeInsetsPhysical:0:*:1:*:1\n'
      'P|right|boolean|0|-|-|boolean:any\n'
      'P|top|boolean|0|-|-|boolean:any\n'
      'S|child|single|1|1|1|any\n'
      'C|flutter.widgets.SafeArea|paletteCreate|wrapExistingChild|child\n',
    );
  });

  test(
    'decodes required DecoratedBox decoration, position, optional child, and image resource',
    () {
      const resourceId =
          'd8d66d4a8f164f97eb04596859253f033379068bed9f0153118c36957351f352';
      final empty = _decode(
        _decoratedBoxModel(decoration: _canvasBoxDecoration()),
      ).root;
      expect(empty.type, 'flutter.widgets.DecoratedBox');
      expect(empty.properties.keys, const ['decoration']);
      final emptyDecoration =
          empty.properties['decoration']!.value as CanvasBoxDecorationValue;
      expect(emptyDecoration.color, isNull);
      expect(emptyDecoration.image, isNull);
      expect(emptyDecoration.border, isNull);
      expect(emptyDecoration.borderRadius, isNull);
      expect(emptyDecoration.boxShadow, isEmpty);
      expect(emptyDecoration.gradient, isNull);
      expect(emptyDecoration.backgroundBlendMode, isNull);
      expect(emptyDecoration.shape, 'rectangle');
      expect(empty.slot('child')!.child, isNull);

      final child = _node(
        '2042d2d2-013a-4366-9483-71dcc7d1a711',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Decorated child'},
        },
      );
      final configured = _decode(
        _decoratedBoxModel(
          decoration: _canvasBoxDecoration(
            color: _canvasThemeColor('material.colorScheme.primaryContainer'),
            image: _canvasDecorationImage(
              image: _canvasImageProvider(
                resolution: {
                  'kind': 'resolved',
                  'resourceId': resourceId,
                  'resolvedScale': 1,
                },
              ),
            ),
            borderRadius: _canvasDirectionalRadius(),
            boxShadow: [_canvasBoxShadow()],
          ),
          position: 'foreground',
          child: child,
        ),
      );
      final node = configured.root;
      final position = node.properties['position']!.value as CanvasEnumValue;
      expect(position.type, 'DecorationPosition');
      expect(position.value, 'foreground');
      expect(node.slot('child')!.child!.type, 'flutter.widgets.Text');
      expect(configured.imageResourceIds, {resourceId});
    },
  );

  test('rejects incomplete or unreviewed DecoratedBox branches', () {
    final text = _node(
      '2042d2d2-013a-4366-9483-71dcc7d1a711',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Child'},
      },
    );
    for (final invalid in <Map<String, Object?>>[
      _decoratedBoxModel(properties: const {}),
      _decoratedBoxModel(
        decoration: const {'kind': 'color', 'argb': '0xFF112233'},
      ),
      _decoratedBoxModel(
        decoration: _canvasBoxDecoration(),
        position: 'middle',
      ),
      _decoratedBoxModel(
        decoration: _canvasBoxDecoration(),
        position: 'foreground',
        positionType: 'BoxDecorationPosition',
      ),
      _decoratedBoxModel(
        decoration: _canvasBoxDecoration(
          borderRadius: _canvasPhysicalRadius(),
          shape: 'circle',
        ),
      ),
      _decoratedBoxModel(
        decoration: _canvasBoxDecoration(),
        properties: {
          'decoration': _canvasBoxDecoration(),
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
      ),
    ]) {
      expect(
        () => _decode(invalid),
        throwsFormatException,
        reason: invalid.toString(),
      );
    }

    final wrongSlot = _decoratedBoxModel(decoration: _canvasBoxDecoration());
    (wrongSlot['root']! as Map<String, Object?>)['slots'] = {
      'child': {
        'kind': 'list',
        'children': [text],
      },
    };
    expect(() => _decode(wrongSlot), throwsFormatException);

    final expanded = _node(
      '47184ff3-7c45-49e5-9039-3079417a7c67',
      'flutter.widgets.Expanded',
      slots: {'child': _single(text)},
    );
    expect(
      () => _decode(
        _decoratedBoxModel(decoration: _canvasBoxDecoration(), child: expanded),
      ),
      throwsFormatException,
      reason: 'DecoratedBox cannot become the RenderFlex parent of Expanded',
    );
  });

  test('DecoratedBox reviewed schema is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.DecoratedBox\n');
    final end = contract.indexOf(
      'W|flutter.widgets.DefaultSelectionStyle\n',
      start,
    );
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final slice = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(slice), hasLength(2));
    expect(RegExp(r'^S\|', multiLine: true).allMatches(slice), hasLength(1));
    expect(
      slice,
      startsWith(
        'W|flutter.widgets.DecoratedBox\n'
        'P|decoration|boxDecoration|1|boxDecoration:empty|-|'
        'boxDecoration:boxDecoration:v2:',
      ),
    );
    expect(
      slice,
      contains(
        'P|position|enum|0|-|-|enum:enum:'
        'cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:'
        'DecorationPosition:background,foreground\n',
      ),
    );
    expect(slice, endsWith('S|child|single|0|0|1|any\n'));
  });

  test(
    'decodes omitted, true, and false ExcludeSemantics with optional child',
    () {
      final omitted = _decode(
        _excludeSemanticsModel(includeChildSlot: false),
      ).root;
      expect(omitted.type, 'flutter.widgets.ExcludeSemantics');
      expect(omitted.properties, isEmpty);
      expect(omitted.slot('child'), isNull);

      final explicitTrue = _decode(
        _excludeSemanticsModel(excluding: true),
      ).root;
      expect(explicitTrue.properties['excluding']!.value, isTrue);
      expect(explicitTrue.slot('child')!.child, isNull);

      final text = _node(
        '4cded0e1-23b0-43f2-b310-ab6ca68e99dc',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Semantic child'},
        },
      );
      final explicitFalse = _decode(
        _excludeSemanticsModel(excluding: false, child: text),
      ).root;
      expect(explicitFalse.properties['excluding']!.value, isFalse);
      expect(explicitFalse.slot('child')!.child!.type, 'flutter.widgets.Text');
    },
  );

  test(
    'rejects non-contract ExcludeSemantics values, slots, and flex child',
    () {
      for (final properties in <Map<String, Object?>>[
        const {
          'excluding': {'kind': 'integer', 'value': 1},
        },
        const {
          'excluding': {'kind': 'string', 'value': 'true'},
        },
        const {
          'futureProperty': {'kind': 'boolean', 'value': true},
        },
      ]) {
        expect(
          () => _decode(_excludeSemanticsModel(properties: properties)),
          throwsFormatException,
          reason: properties.toString(),
        );
      }

      final wrongSlot = _excludeSemanticsModel();
      (wrongSlot['root']! as Map<String, Object?>)['slots'] = {
        'child': {'kind': 'list', 'children': const []},
      };
      expect(() => _decode(wrongSlot), throwsFormatException);

      final text = _node(
        '4cded0e1-23b0-43f2-b310-ab6ca68e99dc',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Flex child'},
        },
      );
      for (final flexType in const [
        'flutter.widgets.Expanded',
        'flutter.widgets.Flexible',
        'flutter.widgets.Spacer',
      ]) {
        final flexChild = _node(
          'f1d79860-3e86-4f0f-9fa5-b0c690fc4300',
          flexType,
          slots: flexType == 'flutter.widgets.Spacer'
              ? const {}
              : {'child': _single(text)},
        );
        expect(
          () => _decode(_excludeSemanticsModel(child: flexChild)),
          throwsFormatException,
          reason: '$flexType requires a direct Row or Column children slot',
        );
      }
    },
  );

  test('ExcludeSemantics reviewed schema is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.ExcludeSemantics\n');
    final end = contract.indexOf('W|flutter.widgets.Expanded\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    expect(
      contract.substring(start, end),
      'W|flutter.widgets.ExcludeSemantics\n'
      'P|excluding|boolean|0|-|-|boolean:any\n'
      'S|child|single|0|0|1|any\n',
    );
  });

  test('decodes the complete strict Container contract and nested unions', () {
    final child = _node(
      'e1c67a19-d25c-4675-92c4-cab1772bf571',
      'flutter.widgets.Text',
      properties: {
        'data': {'kind': 'string', 'value': 'Container child'},
      },
    );
    final properties = <String, Object?>{
      'alignment': _canvasAlignment(
        basis: 'directional',
        horizontal: -0.5,
        vertical: 0.25,
      ),
      'padding': {
        'kind': 'edgeInsetsDirectional',
        'start': 12,
        'top': 8,
        'end': 16,
        'bottom': 10,
      },
      'isAntiAlias': {'kind': 'boolean', 'value': false},
      'decoration': _canvasBoxDecoration(
        color: _canvasThemeColor('material.colorScheme.primaryContainer'),
        border: _canvasPhysicalBorder(),
        borderRadius: _canvasPhysicalRadius(),
        boxShadow: [_canvasBoxShadow()],
        gradient: _canvasLinearGradient(),
        backgroundBlendMode: 'multiply',
      ),
      'foregroundDecoration': _canvasBoxDecoration(
        border: _canvasDirectionalBorder(),
        gradient: _canvasRadialGradient(),
      ),
      'width': {'kind': 'integer', 'value': 160},
      'height': {'kind': 'double', 'value': 80.5},
      'constraints': {
        'kind': 'boxConstraints',
        'minWidth': 100,
        'maxWidth': 200,
        'minHeight': 40,
        'maxHeight': null,
      },
      'margin': {
        'kind': 'edgeInsets',
        'left': 4,
        'top': 2,
        'right': 6,
        'bottom': 8,
      },
      'transform': {
        'kind': 'matrix4',
        'storage': <Object?>[1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 14, -6, 0, 1],
      },
      'transformAlignment': _canvasAlignment(
        basis: 'physical',
        horizontal: 1,
        vertical: -1,
      ),
      'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'hardEdge'},
    };
    final decoded = _decode(
      _containerModel(properties: properties, child: child),
    ).root;

    expect(decoded.type, 'flutter.widgets.Container');
    expect(decoded.properties, hasLength(12));
    final alignment =
        decoded.properties['alignment']!.value as CanvasAlignmentGeometryValue;
    expect(alignment.basis, 'directional');
    expect(alignment.horizontal, -0.5);
    expect(alignment.vertical, 0.25);
    final constraints =
        decoded.properties['constraints']!.value as CanvasBoxConstraintsValue;
    expect(constraints.minWidth, 100);
    expect(constraints.maxWidth, 200);
    expect(constraints.minHeight, 40);
    expect(constraints.maxHeight, isNull);
    final matrix = decoded.properties['transform']!.value as CanvasMatrix4Value;
    expect(matrix.storage, hasLength(16));
    expect(matrix.storage[12], 14);
    expect(matrix.storage[13], -6);
    final decoration =
        decoded.properties['decoration']!.value as CanvasBoxDecorationValue;
    expect(decoration.color, isA<CanvasThemeColor>());
    expect(decoration.border, isA<CanvasPhysicalBoxBorderValue>());
    expect(decoration.borderRadius, isA<CanvasPhysicalBorderRadiusValue>());
    expect(decoration.boxShadow.single.blurStyle, 'outer');
    expect(decoration.gradient, isA<CanvasLinearGradientValue>());
    expect(decoration.backgroundBlendMode, 'multiply');
    final foreground =
        decoded.properties['foregroundDecoration']!.value
            as CanvasBoxDecorationValue;
    expect(foreground.border, isA<CanvasDirectionalBoxBorderValue>());
    expect(foreground.gradient, isA<CanvasRadialGradientValue>());
    expect(decoded.slot('child')!.child!.type, 'flutter.widgets.Text');

    final literal = _decode(
      _containerModel(
        properties: {
          'color': {'kind': 'color', 'argb': '0x7F123456'},
        },
      ),
    ).root;
    expect(literal.properties['color']!.value, 0x7f123456);
  });

  test('decodes every reviewed Container gradient and directional union', () {
    for (final gradient in <Map<String, Object?>>[
      _canvasLinearGradient(),
      _canvasRadialGradient(),
      _canvasRadialGradient(radius: 0),
      _canvasSweepGradient(),
    ]) {
      final decoded = _decode(
        _containerModel(
          properties: {
            'decoration': _canvasBoxDecoration(
              border: _canvasDirectionalBorder(),
              borderRadius: _canvasDirectionalRadius(),
              gradient: gradient,
            ),
          },
        ),
      ).root;
      final value =
          decoded.properties['decoration']!.value as CanvasBoxDecorationValue;
      expect(value.border, isA<CanvasDirectionalBoxBorderValue>());
      expect(value.borderRadius, isA<CanvasDirectionalBorderRadiusValue>());
      expect(value.gradient, switch (gradient['kind']) {
        'linear' => isA<CanvasLinearGradientValue>(),
        'radial' => isA<CanvasRadialGradientValue>(),
        _ => isA<CanvasSweepGradientValue>(),
      });
    }
  });

  test(
    'decodes strict resolved DecorationImage and every ColorFilter union',
    () {
      const resourceId =
          'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
      for (final colorFilter in <Map<String, Object?>>[
        {
          'kind': 'mode',
          'color': _canvasThemeColor('material.colorScheme.primary'),
          'blendMode': 'srcATop',
        },
        {'kind': 'matrix', 'values': List<Object?>.generate(20, (i) => i)},
        {'kind': 'linearToSrgbGamma'},
        {'kind': 'srgbToLinearGamma'},
        {'kind': 'saturation', 'value': 0.35},
      ]) {
        final decoded = _decode(
          _containerModel(
            properties: {
              'decoration': _canvasBoxDecoration(
                image: _canvasDecorationImage(
                  image: _canvasImageProvider(
                    kind: 'exactAsset',
                    exactScale: 2,
                    resize: {
                      'width': 320,
                      'height': null,
                      'policy': 'fit',
                      'allowUpscaling': false,
                    },
                    resolution: {
                      'kind': 'resolved',
                      'resourceId': resourceId,
                      'resolvedScale': 2,
                    },
                  ),
                  colorFilter: colorFilter,
                  fit: 'contain',
                  alignment: _canvasNestedAlignment(
                    basis: 'directional',
                    horizontal: -1,
                    vertical: 0.5,
                  ),
                  repeat: 'repeatX',
                  matchTextDirection: true,
                  scale: 1.25,
                  opacity: 0.65,
                  filterQuality: 'high',
                  invertColors: true,
                  isAntiAlias: true,
                ),
              ),
            },
          ),
        );
        final decoration =
            decoded.root.properties['decoration']!.value
                as CanvasBoxDecorationValue;
        final image = decoration.image!;
        expect(image.image.providerKind, 'exactAsset');
        expect(image.image.exactScale, 2);
        expect(image.image.resize!.width, 320);
        expect(image.image.resize!.policy, 'fit');
        expect(image.image.resolution, isA<CanvasResolvedImageValue>());
        expect(image.alignment.basis, 'directional');
        expect(image.matchTextDirection, isTrue);
        expect(image.filterQuality, 'high');
        expect(decoded.imageResourceIds, {resourceId});
      }
    },
  );

  test('decodes unavailable image status without admitting a resource id', () {
    final decoded = _decode(
      _containerModel(
        properties: {
          'decoration': _canvasBoxDecoration(
            image: _canvasDecorationImage(
              image: _canvasImageProvider(
                resolution: {
                  'kind': 'unavailable',
                  'code': 'corrupt',
                  'reason': 'assets/panel.png could not be decoded as PNG',
                },
              ),
              centerSlice: {'left': 1, 'top': 2, 'right': 5, 'bottom': 6},
            ),
          ),
        },
      ),
    );
    final decoration =
        decoded.root.properties['decoration']!.value
            as CanvasBoxDecorationValue;
    final resolution =
        decoration.image!.image.resolution as CanvasUnavailableImageValue;
    expect(resolution.code, 'corrupt');
    expect(resolution.reason, contains('could not be decoded'));
    expect(decoded.imageResourceIds, isEmpty);
  });

  test('accepts an ExactAssetImage scale above the DPR variant bound', () {
    const resourceId =
        'abababababababababababababababababababababababababababababababab';
    final decoded = _decode(
      _containerModel(
        properties: {
          'decoration': _canvasBoxDecoration(
            image: _canvasDecorationImage(
              image: _canvasImageProvider(
                kind: 'exactAsset',
                exactScale: 125,
                resolution: {
                  'kind': 'resolved',
                  'resourceId': resourceId,
                  'resolvedScale': 125,
                },
              ),
            ),
          ),
        },
      ),
    );
    final decoration =
        decoded.root.properties['decoration']!.value
            as CanvasBoxDecorationValue;
    final provider = decoration.image!.image;
    expect(provider.exactScale, 125);
    expect(
      (provider.resolution as CanvasResolvedImageValue).resolvedScale,
      125,
    );
    expect(decoded.imageResourceIds, {resourceId});
  });

  test('rejects non-canonical DecorationImage and provider wire values', () {
    void rejects(Map<String, Object?> image, String reason) {
      expect(
        () => _decode(
          _containerModel(
            properties: {'decoration': _canvasBoxDecoration(image: image)},
          ),
        ),
        throwsFormatException,
        reason: reason,
      );
    }

    final valid = _canvasDecorationImage();
    rejects({...valid, 'unknown': true}, 'closed DecorationImage object');
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(assetName: '../secret.png'),
      ),
      'unsafe asset path',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(assetName: '~/secret.png'),
      ),
      'home-relative asset path',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(assetName: 'assets/%2e%2e/secret.png'),
      ),
      'percent-encoded asset path',
    );
    for (final (assetName, reason) in const [
      (' ', 'blank asset path'),
      ('\u1680', 'Unicode-blank asset path'),
      (' assets/images/logo.png', 'leading-whitespace asset path'),
      ('assets/images/logo.png ', 'trailing-whitespace asset path'),
    ]) {
      rejects(
        _canvasDecorationImage(
          image: _canvasImageProvider(assetName: assetName),
        ),
        reason,
      );
    }
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(kind: 'exactAsset', exactScale: null),
      ),
      'ExactAssetImage requires exactScale',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(
          resize: {
            'width': null,
            'height': null,
            'policy': 'exact',
            'allowUpscaling': false,
          },
        ),
      ),
      'ResizeImage requires one dimension',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(
          resolution: {
            'kind': 'unavailable',
            'code': 'network',
            'reason': 'not allowed',
          },
        ),
      ),
      'closed unavailable code',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(
          resolution: {
            'kind': 'unavailable',
            'code': 'corrupt',
            'reason': 'bad\u0001bytes',
          },
        ),
      ),
      'unavailable reason control character',
    );
    rejects(
      _canvasDecorationImage(
        image: _canvasImageProvider(
          resolution: {
            'kind': 'unavailable',
            'code': 'corrupt',
            'reason': List.filled(513, '\u{1f642}').join(),
          },
        ),
      ),
      'unavailable reason UTF-16 bound',
    );
    rejects(
      _canvasDecorationImage(
        colorFilter: {'kind': 'matrix', 'values': List<Object?>.filled(19, 0)},
      ),
      'matrix length',
    );
    rejects(
      _canvasDecorationImage(
        fit: 'cover',
        centerSlice: {'left': 0, 'top': 0, 'right': 1, 'bottom': 1},
      ),
      'centerSlice fit invariant',
    );
    rejects(
      _canvasDecorationImage(
        fit: 'none',
        centerSlice: {'left': 0, 'top': 0, 'right': 1, 'bottom': 1},
      ),
      'centerSlice none fit invariant',
    );
    rejects(
      _canvasDecorationImage(
        centerSlice: {'left': 1, 'top': 0, 'right': 1, 'bottom': 1},
      ),
      'centerSlice requires positive width',
    );
    rejects(
      _canvasDecorationImage(
        centerSlice: {'left': 0, 'top': 1, 'right': 1, 'bottom': 1},
      ),
      'centerSlice requires positive height',
    );
    final containedSlice = _decode(
      _containerModel(
        properties: {
          'decoration': _canvasBoxDecoration(
            image: _canvasDecorationImage(
              fit: 'contain',
              centerSlice: {'left': 0, 'top': 0, 'right': 1, 'bottom': 1},
            ),
          ),
        },
      ),
    );
    final containedDecoration =
        containedSlice.root.properties['decoration']!.value
            as CanvasBoxDecorationValue;
    expect(containedDecoration.image!.fit, 'contain');
    rejects(_canvasDecorationImage(opacity: 1.1), 'opacity bound');
  });

  test('rejects unsafe or non-canonical Container structured values', () {
    void rejects(Map<String, Object?> properties, String reason) {
      expect(
        () => _decode(_containerModel(properties: properties)),
        throwsFormatException,
        reason: reason,
      );
    }

    rejects({
      'alignment': _canvasAlignment(basis: 'absolute'),
    }, 'unknown alignment basis');
    rejects({
      'constraints': {
        'kind': 'boxConstraints',
        'minWidth': 10,
        'maxWidth': 9,
        'minHeight': 0,
        'maxHeight': null,
      },
    }, 'maximum below minimum');
    rejects({
      'transform': {'kind': 'matrix4', 'storage': List<Object?>.filled(15, 0)},
    }, 'matrix length');
    rejects({
      'padding': {
        'kind': 'edgeInsets',
        'left': -1,
        'top': 0,
        'right': 0,
        'bottom': 0,
      },
    }, 'negative padding');
    rejects({
      'margin': {
        'kind': 'edgeInsetsDirectional',
        'start': 0,
        'top': -1,
        'end': 0,
        'bottom': 0,
      },
    }, 'negative margin');
    rejects({
      'color': {'kind': 'color', 'argb': '0xFF000000'},
      'decoration': _canvasBoxDecoration(),
    }, 'color and decoration');
    rejects({
      'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
    }, 'clip without decoration');
    rejects({
      'decoration': _canvasBoxDecoration(
        borderRadius: _canvasPhysicalRadius(),
        shape: 'circle',
      ),
    }, 'circle radius');
    rejects({
      'decoration': _canvasBoxDecoration(backgroundBlendMode: 'multiply'),
    }, 'blend without color or gradient');
    rejects({
      'decoration': _canvasBoxDecoration(
        gradient: _canvasLinearGradient(
          stops: [
            _canvasGradientStop('3501a21a-29f2-48e5-9f03-1bf08f93ac00', 0.8),
            _canvasGradientStop('e2ca03a9-2f55-418b-835f-426766679dc5', 0.2),
          ],
        ),
      ),
    }, 'decreasing stops');
    rejects({
      'decoration': _canvasBoxDecoration(
        gradient: _canvasRadialGradient(focal: null, focalRadius: 0.2),
      ),
    }, 'focal radius without focal');
    rejects({
      'decoration': _canvasBoxDecoration(
        gradient: _canvasRadialGradient(radius: -0.1),
      ),
    }, 'negative radial radius');
    final concentric = _decode(
      _containerModel(
        properties: {
          'decoration': _canvasBoxDecoration(
            gradient: _canvasRadialGradient(
              focal: _canvasNestedAlignment(
                basis: 'directional',
                horizontal: 0.25,
                vertical: 0,
              ),
              center: _canvasNestedAlignment(horizontal: -0.25, vertical: 0),
            ),
          ),
        },
      ),
    );
    expect(
      (concentric.root.properties['decoration']!.value
              as CanvasBoxDecorationValue)
          .gradient,
      isA<CanvasRadialGradientValue>(),
    );
    rejects({
      'decoration': _canvasBoxDecoration(
        gradient: _canvasSweepGradient(startAngle: 2, endAngle: 1),
      ),
    }, 'sweep angle order');
    rejects({
      'decoration': _canvasBoxDecoration(
        color: _canvasThemeColor('material.colorScheme.notReviewed'),
      ),
    }, 'unknown theme token');
    rejects({
      'decoration': _canvasBoxDecoration(
        border: _canvasPhysicalBorder(
          left: _canvasBorderSide(
            color: _canvasLiteralColor('0xFF445566'),
            strokeAlign: 0,
          ),
        ),
      ),
    }, 'unsafe nonuniform rectangular border');
  });

  test('Container reviewed contract is exact and closed', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.widgets.Container\n');
    final end = contract.indexOf('W|flutter.widgets.DecoratedBox\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final slice = contract.substring(start, end);
    expect(RegExp(r'^P\|', multiLine: true).allMatches(slice), hasLength(13));
    expect(RegExp(r'^S\|', multiLine: true).allMatches(slice), hasLength(1));
    expect(slice, contains('P|alignment|alignmentGeometry|'));
    expect(slice, contains('P|constraints|boxConstraints|'));
    expect(slice, contains('P|transform|matrix4|'));
    expect(slice, contains('P|decoration|boxDecoration|'));
    expect(slice, contains('P|foregroundDecoration|boxDecoration|'));
    expect(
      slice,
      contains(
        'P|margin|edgeInsets,edgeInsetsDirectional|0|-|'
        'edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|',
      ),
    );
    expect(slice, contains('S|child|single|0|0|1|any\n'));
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

  test('decodes complete AppBar properties, five slots, and trait slots', () {
    final decoded = CanvasModel.decode(appBarModelBytesForViewTest());
    final appBar = decoded.root.slot('appBar')!.child!;
    expect(appBar.type, 'flutter.material.AppBar');
    expect(
      appBar.properties.keys,
      containsAll(appBarPropertiesForViewTest().keys),
    );
    expect(appBar.slot('leading')!.child!.type, 'flutter.widgets.Icon');
    expect(appBar.slot('title')!.child!.type, 'flutter.widgets.Text');
    expect(appBar.slot('actions')!.children, hasLength(2));
    expect(appBar.slot('flexibleSpace')!.child!.type, 'flutter.widgets.Center');
    expect(appBar.slot('bottom')!.child!.type, 'flutter.material.AppBar');
    expect(
      (appBar.properties['clipBehavior']!.value as CanvasEnumValue).value,
      'antiAlias',
    );
    expect(
      appBar.properties['actionsPadding']!.value,
      isA<CanvasEdgeInsetsDirectional>(),
    );

    final wrongScaffoldChild = _appBarModelJson();
    final scaffold = wrongScaffoldChild['root']! as Map<String, Object?>;
    (scaffold['slots']! as Map<String, Object?>)['appBar'] = _single(
      _node(
        '5660844b-9c39-4d2a-a076-5dc1e95cc8f0',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Not preferred'},
        },
      ),
    );
    expect(() => _decode(wrongScaffoldChild), throwsFormatException);

    final wrongBottomChild = _appBarModelJson();
    final wrongAppBar = _findNodeByType(
      wrongBottomChild['root']! as Map<String, Object?>,
      'flutter.material.AppBar',
    );
    (wrongAppBar['slots']! as Map<String, Object?>)['bottom'] = _single(
      _node(
        '952589d3-34f6-470e-b57d-291e6a97afc7',
        'flutter.widgets.Text',
        properties: {
          'data': {'kind': 'string', 'value': 'Not preferred'},
        },
      ),
    );
    expect(() => _decode(wrongBottomChild), throwsFormatException);
  });

  test('enforces exact AppBar bounds, enums, shape and style relations', () {
    void expectProperty(
      String name,
      Map<String, Object?> value,
      Matcher matcher,
    ) {
      final json = _appBarModelJson(properties: {name: value});
      expect(() => _decode(json), matcher, reason: '$name=$value');
    }

    for (final name in const [
      'elevation',
      'scrolledUnderElevation',
      'toolbarHeight',
      'leadingWidth',
    ]) {
      expectProperty(name, const {
        'kind': 'integer',
        'value': 0,
      }, returnsNormally);
      expectProperty(name, const {
        'kind': 'double',
        'value': -0.01,
      }, throwsFormatException);
    }
    for (final name in const [
      'toolbarOpacity',
      'bottomOpacity',
      'iconThemeFill',
      'actionsIconThemeOpacity',
    ]) {
      expectProperty(name, const {
        'kind': 'double',
        'value': 0.0,
      }, returnsNormally);
      expectProperty(name, const {
        'kind': 'double',
        'value': 1.0,
      }, returnsNormally);
      expectProperty(name, const {
        'kind': 'double',
        'value': 1.01,
      }, throwsFormatException);
    }
    expect(
      () => _decode(
        _appBarModelJson(
          properties: const {
            'shapeKind': {'kind': 'string', 'value': 'stadium'},
            'shapeSideStrokeAlign': {'kind': 'double', 'value': -123.5},
          },
        ),
      ),
      returnsNormally,
    );
    expect(
      () => _decode(
        _appBarModelJson(
          properties: const {
            'shapeKind': {'kind': 'string', 'value': 'stadium'},
            'shapeSideStrokeAlign': {'kind': 'double', 'value': 123.5},
          },
        ),
      ),
      returnsNormally,
    );
    expectProperty('clipBehavior', const {
      'kind': 'enum',
      'type': 'Clip',
      'value': 'future',
    }, throwsFormatException);
    expectProperty('notificationPredicate', const {
      'kind': 'string',
      'value': 'depthZero',
    }, returnsNormally);
    expectProperty('notificationPredicate', const {
      'kind': 'string',
      'value': 'depthZero|all',
    }, throwsFormatException);

    final missingKind = _appBarModelJson(
      properties: {
        'shapeSideWidth': {'kind': 'double', 'value': 1.0},
      },
    );
    expect(() => _decode(missingKind), throwsFormatException);
    final wrongRadius = _appBarModelJson(
      properties: {
        'shapeKind': {'kind': 'string', 'value': 'circle'},
        'shapeRadiusTopLeft': {'kind': 'double', 'value': 1.0},
      },
    );
    expect(() => _decode(wrongRadius), throwsFormatException);
    final wrongEccentricity = _appBarModelJson(
      properties: {
        'shapeKind': {'kind': 'string', 'value': 'stadium'},
        'shapeCircleEccentricity': {'kind': 'double', 'value': 0.5},
      },
    );
    expect(() => _decode(wrongEccentricity), throwsFormatException);
    final styleConflict = _appBarModelJson(
      properties: {
        'titleTextStyleColor': {'kind': 'color', 'argb': '0xFF000000'},
        'titleTextStyleForeground': _paint(const {
          'kind': 'literal',
          'argb': '0xFF000000',
        }),
      },
    );
    expect(() => _decode(styleConflict), throwsFormatException);
  });

  test(
    'ElevatedButton reviewed contract is exact and closed at 286 leaves',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|flutter.material.ElevatedButton\n');
      final end = contract.indexOf('\nW|', start) + 1;
      expect(start, greaterThanOrEqualTo(0));
      expect(end, greaterThan(start));
      final section = contract.substring(start, end);
      expect(
        section.split('\n').where((line) => line.startsWith('P|')),
        hasLength(286),
      );
      expect(
        section,
        contains('P|enabled|boolean|0|boolean:true|-|boolean:any\n'),
      );
      expect(
        section,
        contains('P|onPressed|callback|0|-|-|callback:callbackReference\n'),
      );
      expect(section, contains('S|child|single|1|0|1|any\n'));
    },
  );

  test('Scaffold reviewed contract is exact and closed at 17 leaves', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|flutter.material.Scaffold\n');
    final end = contract.indexOf('W|flutter.material.TextButton\n', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));
    final section = contract.substring(start, end);
    expect(
      section.split('\n').where((line) => line.startsWith('P|')),
      hasLength(17),
    );
    expect(
      section,
      contains(
        'P|drawerDragStartBehavior|enum|0|-|-|enum:enum:'
        'cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:'
        'DragStartBehavior:down,start\n',
      ),
    );
    expect(
      section,
      contains('P|onDrawerChanged|callback|0|-|-|callback:callbackReference\n'),
    );
    expect(section, contains('S|appBar|single|0|0|1|trait:'));
    expect(section, contains('S|body|single|0|0|1|any\n'));
    expect(section, contains('S|floatingActionButton|single|0|0|1|any\n'));
  });

  test('decodes callbacks as presence only and requires the child slot', () {
    final decoded = CanvasModel.decode(elevatedButtonModelBytesForViewTest());
    final button = decoded.root.slot('body')!.child!;
    expect(button.type, 'flutter.material.ElevatedButton');
    expect(button.properties.keys, elevatedButtonPropertiesForViewTest().keys);
    for (final name in const [
      'onPressed',
      'onLongPress',
      'onHover',
      'onFocusChange',
    ]) {
      expect(button.properties[name]!.kind, 'callbackPresence');
      expect(button.properties[name]!.value, isTrue);
    }
    expect(button.slot('child')!.child!.type, 'flutter.widgets.Text');

    final missingSlot = _elevatedButtonModel(
      properties: const {
        'enabled': {'kind': 'boolean', 'value': true},
      },
    );
    (_elevatedButtonNode(missingSlot)['slots']! as Map<String, Object?>).remove(
      'child',
    );
    expect(() => _decode(missingSlot), throwsFormatException);

    for (final callback in const [
      {'kind': 'callback', 'value': 'handler'},
      {'kind': 'callbackPresence', 'configured': true},
      {'kind': 'callbackPresence', 'value': 'handler'},
    ]) {
      expect(
        () =>
            _decode(_elevatedButtonModel(properties: {'onPressed': callback})),
        throwsFormatException,
        reason: '$callback',
      );
    }
  });

  test(
    'enforces exact ElevatedButton bounds and pinned preset vocabularies',
    () {
      void expectProperty(
        String name,
        Map<String, Object?> value,
        Matcher matcher,
      ) {
        expect(
          () => _decode(_elevatedButtonModel(properties: {name: value})),
          matcher,
          reason: '$name=$value',
        );
      }

      for (final name in const [
        'styleElevation',
        'styleMinimumWidth',
        'styleFixedHeight',
        'styleMaximumWidth',
        'styleIconSize',
      ]) {
        expectProperty(name, const {
          'kind': 'integer',
          'value': 0,
        }, returnsNormally);
        expectProperty(name, const {
          'kind': 'double',
          'value': -0.01,
        }, throwsFormatException);
      }
      for (final name in const [
        'styleVisualDensityHorizontal',
        'styleVisualDensityVertical',
      ]) {
        expectProperty(name, const {
          'kind': 'double',
          'value': -4.0,
        }, returnsNormally);
        expectProperty(name, const {
          'kind': 'double',
          'value': 4.0,
        }, returnsNormally);
        expectProperty(name, const {
          'kind': 'double',
          'value': 4.01,
        }, throwsFormatException);
      }
      expectProperty('styleSideStrokeAlign', const {
        'kind': 'double',
        'value': -123.5,
      }, returnsNormally);
      expectProperty('styleSideStrokeAlign', const {
        'kind': 'double',
        'value': 123.5,
      }, returnsNormally);
      expectProperty('styleShapeCircleEccentricity', const {
        'kind': 'double',
        'value': -0.01,
      }, throwsFormatException);

      for (final preset in const [
        'roundedSuperellipse',
        'none',
        'resizeColumn',
        'resizeRow',
        'zoomIn',
        'zoomOut',
        'inkSplash',
      ]) {
        final property = switch (preset) {
          'roundedSuperellipse' => 'styleShapeKind',
          'inkSplash' => 'styleSplashFactory',
          _ => 'styleMouseCursor',
        };
        expectProperty(property, {
          'kind': 'string',
          'value': preset,
        }, returnsNormally);
      }
      expectProperty('styleMouseCursor', const {
        'kind': 'string',
        'value': 'defer',
      }, throwsFormatException);
      expectProperty('styleSplashFactory', const {
        'kind': 'string',
        'value': 'futureSplash',
      }, throwsFormatException);
    },
  );

  test('enforces ElevatedButton shape and effective TextStyle relations', () {
    for (final properties in const [
      {
        'styleMinimumWidth': {'kind': 'double', 'value': 101.0},
        'styleMaximumWidth': {'kind': 'double', 'value': 100.0},
      },
      {
        'styleMinimumHeight': {'kind': 'double', 'value': 40.0},
        'styleFixedHeight': {'kind': 'double', 'value': 39.0},
      },
      {
        'styleFixedWidth': {'kind': 'double', 'value': 201.0},
        'styleMaximumWidth': {'kind': 'double', 'value': 200.0},
      },
    ]) {
      expect(
        () => _decode(_elevatedButtonModel(properties: properties)),
        throwsFormatException,
        reason: '$properties',
      );
    }
    for (final properties in const [
      {
        'styleAlignmentKind': {'kind': 'string', 'value': 'physical'},
      },
      {
        'styleAlignmentX': {'kind': 'double', 'value': 0.0},
        'styleAlignmentY': {'kind': 'double', 'value': 0.0},
      },
    ]) {
      expect(
        () => _decode(_elevatedButtonModel(properties: properties)),
        throwsFormatException,
        reason: '$properties',
      );
    }
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleShapeRadiusTopLeft': {'kind': 'double', 'value': 4.0},
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleShapeKind': {'kind': 'string', 'value': 'circle'},
            'styleShapeRadiusTopLeft': {'kind': 'double', 'value': 4.0},
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleShapeKind': {'kind': 'string', 'value': 'circle'},
            'styleShapeCircleEccentricity': {'kind': 'double', 'value': 0.5},
          },
        ),
      ),
      returnsNormally,
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleShapeKind': {'kind': 'string', 'value': 'roundedRectangle'},
            'stylePressedShapeRadiusTopLeft': {'kind': 'double', 'value': 12.0},
          },
        ),
      ),
      returnsNormally,
      reason: 'a state shape fragment inherits the enabled/base discriminator',
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: {
            'styleTextBackgroundColor': const {
              'kind': 'color',
              'argb': '0xFF000000',
            },
            'styleTextBackground': _paint(const {
              'kind': 'literal',
              'argb': '0xFFFFFFFF',
            }),
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleTextPackage': {'kind': 'string', 'value': 'design_fonts'},
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleTextFontFamily': {'kind': 'string', 'value': 'BaseFamily'},
            'stylePressedTextPackage': {
              'kind': 'string',
              'value': 'design_fonts',
            },
          },
        ),
      ),
      returnsNormally,
      reason: 'an inheriting active fragment may reuse the raw base family',
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleTextInherit': {'kind': 'boolean', 'value': false},
            'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
            'styleTextFontFamily': {'kind': 'string', 'value': 'BaseFamily'},
            'stylePressedTextInherit': {'kind': 'boolean', 'value': false},
            'stylePressedTextPackage': {
              'kind': 'string',
              'value': 'design_fonts',
            },
          },
        ),
      ),
      throwsFormatException,
      reason: 'inherit=false may not borrow a raw family from the base layer',
    );
    expect(
      () => _decode(
        _elevatedButtonModel(
          properties: const {
            'styleTextInherit': {'kind': 'boolean', 'value': false},
            'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
            'styleTextFontFamily': {'kind': 'string', 'value': 'BaseFamily'},
            'stylePressedTextInherit': {'kind': 'boolean', 'value': false},
            'stylePressedTextFontFamilyFallback': {
              'kind': 'string',
              'value': 'PressedFallback',
            },
            'stylePressedTextPackage': {
              'kind': 'string',
              'value': 'design_fonts',
            },
          },
        ),
      ),
      returnsNormally,
      reason: 'inherit=false accepts a same-fragment raw fallback',
    );
  });

  test(
    'requires one transition-safe ElevatedButton TextStyle inherit mode',
    () {
      expect(
        () => _decode(
          _elevatedButtonModel(
            properties: const {
              'styleTextInherit': {'kind': 'boolean', 'value': false},
            },
          ),
        ),
        throwsFormatException,
        reason:
            'disabled inherit is mandatory once local inherit is configured',
      );
      expect(
        () => _decode(
          _elevatedButtonModel(
            properties: const {
              'styleTextInherit': {'kind': 'boolean', 'value': false},
              'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
              'stylePressedTextInherit': {'kind': 'boolean', 'value': true},
            },
          ),
        ),
        throwsFormatException,
        reason: 'reachable state inherit modes may not differ',
      );
      expect(
        () => _decode(
          _elevatedButtonModel(
            properties: const {
              'styleTextInherit': {'kind': 'boolean', 'value': false},
              'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
              'stylePressedTextTheme': {
                'kind': 'themeToken',
                'token': 'material.textTheme.labelLarge',
              },
            },
          ),
        ),
        throwsFormatException,
        reason: 'a local state theme token requires an explicit matching mode',
      );
      expect(
        () => _decode(
          _elevatedButtonModel(
            properties: const {
              'styleTextInherit': {'kind': 'boolean', 'value': false},
              'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
              'stylePressedTextInherit': {'kind': 'boolean', 'value': false},
              'stylePressedTextTheme': {
                'kind': 'themeToken',
                'token': 'material.textTheme.labelLarge',
              },
            },
          ),
        ),
        returnsNormally,
      );
    },
  );

  test('decodes the complete strict Icon contract and nullable IconData', () {
    final decoded = _decode(
      _iconModel(properties: iconPropertiesForViewTest()),
    ).root.slot('body')!.child!.slot('child')!.child!;
    final icon = decoded.properties['icon']!.value as CanvasIconDataValue;
    expect(icon.codePoint, 0xe5fc);
    expect(icon.fontFamily, 'MaterialIcons');
    expect(icon.fontPackage, isNull);
    expect(icon.matchTextDirection, isTrue);
    expect(icon.fontFamilyFallback, isEmpty);
    expect(decoded.properties['size']!.value, 32);
    expect(decoded.properties['fill']!.value, 0.75);
    expect(decoded.properties['weight']!.value, 600.0);
    expect(decoded.properties['grade']!.value, -25.0);
    expect(decoded.properties['opticalSize']!.value, 24.0);
    expect(decoded.properties['color']!.value, isA<CanvasThemeToken>());
    expect(
      decoded.properties['shadows']!.value,
      isA<List<CanvasShadowValue>>(),
    );
    expect(
      (decoded.properties['blendMode']!.value as CanvasEnumValue).value,
      'multiply',
    );
    expect(
      (decoded.properties['fontWeight']!.value as CanvasEnumValue).value,
      'w700',
    );

    final empty = _decode(
      _iconModel(
        properties: {
          'icon': iconDataValueForViewTest(codePoint: null, fontFamily: null),
        },
      ),
    ).root.slot('body')!.child!.slot('child')!.child!;
    final emptyIcon = empty.properties['icon']!.value as CanvasIconDataValue;
    expect(emptyIcon.codePoint, isNull);
    expect(emptyIcon.fontFamily, isNull);
    expect(emptyIcon.fontPackage, isNull);
    expect(emptyIcon.matchTextDirection, isFalse);
    expect(emptyIcon.fontFamilyFallback, isEmpty);
  });

  test('enforces exact IconData scalar, metadata and wire-shape rules', () {
    void expectIconData(Map<String, Object?> value, Matcher matcher) {
      expect(
        () => _decode(_iconModel(properties: {'icon': value})),
        matcher,
        reason: value.toString(),
      );
    }

    expectIconData(iconDataValueForViewTest(), returnsNormally);
    expectIconData(
      iconDataValueForViewTest(codePoint: null, fontFamily: null),
      returnsNormally,
    );

    expectIconData(
      iconDataValueForViewTest(codePoint: 0xe29f),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(matchTextDirection: true),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(codePoint: 0xe5fc),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(codePoint: 0xe67e),
      returnsNormally,
    );
    expectIconData(
      iconDataValueForViewTest(codePoint: 0xe67e, matchTextDirection: true),
      returnsNormally,
    );

    for (final scalar in const [0, 0x10ffff]) {
      expectIconData(
        iconDataValueForViewTest(codePoint: scalar, fontFamily: 'Family'),
        throwsFormatException,
      );
    }
    for (final scalar in const [-1, 0xd800, 0xdfff, 0x110000]) {
      expectIconData(
        iconDataValueForViewTest(codePoint: scalar),
        throwsFormatException,
      );
    }

    final validAstral = String.fromCharCode(0x1f600) * 256;
    expectIconData(
      iconDataValueForViewTest(fontFamily: validAstral),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(
        fontFamily: validAstral + String.fromCharCode(0x1f601),
      ),
      throwsFormatException,
    );
    for (final metadata in <String>[
      '',
      ' Family',
      'Family ',
      'A\u0000B',
      String.fromCharCode(0x061c),
      String.fromCharCode(0x200e),
      String.fromCharCode(0x200f),
      String.fromCharCode(0x2028),
      String.fromCharCode(0x202e),
      String.fromCharCode(0x2066),
      String.fromCharCode(0x2069),
      String.fromCharCode(0xfeff),
      String.fromCharCode(0xd800),
    ]) {
      expectIconData(
        iconDataValueForViewTest(fontFamily: metadata),
        throwsFormatException,
      );
    }

    expectIconData(
      iconDataValueForViewTest(fontPackage: 'package'),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(codePoint: null, fontFamily: 'MaterialIcons'),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(
        fontFamilyFallback: List<String>.generate(33, (index) => 'F$index'),
      ),
      throwsFormatException,
    );
    expectIconData(
      iconDataValueForViewTest(fontFamilyFallback: const ['Same', 'Same']),
      throwsFormatException,
    );

    final missing = iconDataValueForViewTest()..remove('fontPackage');
    expectIconData(missing, throwsFormatException);
    final extra = iconDataValueForViewTest()..['extra'] = true;
    expectIconData(extra, throwsFormatException);
    final wrongList = iconDataValueForViewTest()
      ..['fontFamilyFallback'] = 'Fallback';
    expectIconData(wrongList, throwsFormatException);
  });

  test('enforces every Icon constructor bound, enum and leaf contract', () {
    void expectProperty(
      String name,
      Map<String, Object?> value,
      Matcher matcher,
    ) {
      expect(
        () => _decode(
          _iconModel(
            properties: {'icon': iconDataValueForViewTest(), name: value},
          ),
        ),
        matcher,
        reason: '$name=$value',
      );
    }

    for (final candidate in const [
      {'kind': 'integer', 'value': 0},
      {'kind': 'integer', 'value': 9007199254740991},
      {'kind': 'double', 'value': 0.0},
    ]) {
      expectProperty('size', candidate, returnsNormally);
    }
    expectProperty('size', const {
      'kind': 'integer',
      'value': -1,
    }, throwsFormatException);
    for (final value in const [0.0, 1.0]) {
      expectProperty('fill', {
        'kind': 'double',
        'value': value,
      }, returnsNormally);
    }
    for (final value in const [-0.01, 1.01]) {
      expectProperty('fill', {
        'kind': 'double',
        'value': value,
      }, throwsFormatException);
    }
    expectProperty('fill', const {
      'kind': 'integer',
      'value': 1,
    }, throwsFormatException);
    for (final name in const ['weight', 'opticalSize']) {
      expectProperty(name, const {
        'kind': 'double',
        'value': 0.0,
      }, throwsFormatException);
      expectProperty(name, const {
        'kind': 'double',
        'value': 0.001,
      }, returnsNormally);
      expectProperty(name, const {
        'kind': 'double',
        'value': 32767.999,
      }, returnsNormally);
      expectProperty(name, const {
        'kind': 'double',
        'value': 32768.0,
      }, throwsFormatException);
    }
    for (final value in const [-32768.0, 32767.999]) {
      expectProperty('grade', {
        'kind': 'double',
        'value': value,
      }, returnsNormally);
    }
    for (final value in const [-32768.001, 32768.0]) {
      expectProperty('grade', {
        'kind': 'double',
        'value': value,
      }, throwsFormatException);
    }
    expectProperty('textDirection', const {
      'kind': 'enum',
      'type': 'TextDirection',
      'value': 'auto',
    }, throwsFormatException);
    expectProperty('blendMode', const {
      'kind': 'enum',
      'type': 'BlendMode',
      'value': 'futureMode',
    }, throwsFormatException);
    expectProperty('fontWeight', const {
      'kind': 'enum',
      'type': 'FontWeight',
      'value': 'bold',
    }, throwsFormatException);

    final missingRequired = _iconModel(properties: const {});
    expect(() => _decode(missingRequired), throwsFormatException);
    final unknownSlot = _iconModel(
      properties: {'icon': iconDataValueForViewTest()},
    );
    final iconNode = _findNodeByType(
      unknownSlot['root']! as Map<String, Object?>,
      'flutter.widgets.Icon',
    );
    iconNode['slots'] = {'child': _single(null)};
    expect(() => _decode(unknownSlot), throwsFormatException);
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
    final oldProtocol = _modelJson()..['protocolVersion'] = 15;
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

  test('decodes the exact typed project component color vocabulary', () {
    final json = _modelJson();
    _theme(json)['components'] = {
      'scaffold.backgroundColor': {'kind': 'argb', 'argb': '0xFF123456'},
      'appBar.foregroundColor': {'kind': 'colorScheme', 'role': 'onPrimary'},
      'elevatedButton.overlayColor.pressed': {
        'kind': 'argb',
        'argb': '0x66112233',
      },
    };

    final components = _decode(json).profile.theme.components;
    expect(components, hasLength(3));
    expect(
      (components['scaffold.backgroundColor']! as CanvasThemeLiteralColor).argb,
      0xff123456,
    );
    expect(
      (components['appBar.foregroundColor']! as CanvasThemeRoleColor).role,
      'onPrimary',
    );
    expect(
      (components['elevatedButton.overlayColor.pressed']!
              as CanvasThemeLiteralColor)
          .argb,
      0x66112233,
    );

    expect(canvasThemeComponentColorRoles, hasLength(36));

    final unknown = _modelJson();
    _theme(unknown)['components'] = {
      'futureWidget.color': {'kind': 'argb', 'argb': '0xFF000000'},
    };
    expect(() => _decode(unknown), throwsFormatException);

    final malformed = _modelJson();
    _theme(malformed)['components'] = {
      'icon.color': {'kind': 'literal', 'argb': '0xFF000000'},
    };
    expect(() => _decode(malformed), throwsFormatException);

    final missing = _modelJson();
    _theme(missing).remove('components');
    expect(() => _decode(missing), throwsFormatException);
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

const appBarWidgetIdForViewTest = '1cb20d76-4829-446f-b558-a2b12304f0f0';

Map<String, Object?> appBarPropertiesForViewTest() => {
  'backgroundColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.surfaceContainer',
  },
  'centerTitle': {'kind': 'boolean', 'value': true},
  'elevation': {'kind': 'integer', 'value': 4},
  'automaticallyImplyLeading': {'kind': 'boolean', 'value': false},
  'automaticallyImplyActions': {'kind': 'boolean', 'value': false},
  'scrolledUnderElevation': {'kind': 'double', 'value': 7.5},
  'notificationPredicate': {'kind': 'string', 'value': 'depthZero'},
  'shadowColor': {'kind': 'color', 'argb': '0xFF010203'},
  'surfaceTintColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.surfaceTint',
  },
  'foregroundColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.onSurface',
  },
  'primary': {'kind': 'boolean', 'value': false},
  'excludeHeaderSemantics': {'kind': 'boolean', 'value': true},
  'titleSpacing': {'kind': 'double', 'value': 13.5},
  'toolbarOpacity': {'kind': 'double', 'value': 0.8},
  'bottomOpacity': {'kind': 'double', 'value': 0.7},
  'toolbarHeight': {'kind': 'integer', 'value': 64},
  'leadingWidth': {'kind': 'double', 'value': 52.0},
  'forceMaterialTransparency': {'kind': 'boolean', 'value': true},
  'useDefaultSemanticsOrder': {'kind': 'boolean', 'value': false},
  'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
  'actionsPadding': {
    'kind': 'edgeInsetsDirectional',
    'start': 3,
    'top': 4,
    'end': 5,
    'bottom': 6,
  },
  'animateColor': {'kind': 'boolean', 'value': true},
  'shapeKind': {'kind': 'string', 'value': 'roundedRectangle'},
  'shapeSideColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.outline',
  },
  'shapeSideWidth': {'kind': 'double', 'value': 2.0},
  'shapeSideStyle': {'kind': 'enum', 'type': 'BorderStyle', 'value': 'solid'},
  'shapeSideStrokeAlign': {'kind': 'double', 'value': 12.5},
  'shapeRadiusTopLeft': {'kind': 'double', 'value': 1.0},
  'shapeRadiusTopRight': {'kind': 'double', 'value': 2.0},
  'shapeRadiusBottomRight': {'kind': 'double', 'value': 3.0},
  'shapeRadiusBottomLeft': {'kind': 'double', 'value': 4.0},
  ..._iconThemePropertiesForViewTest(
    'iconTheme',
    shadowId: '7c37c659-01b5-46e5-852f-11abdb77504c',
  ),
  ..._iconThemePropertiesForViewTest(
    'actionsIconTheme',
    shadowId: '374275df-4de3-4b1b-b542-56567108ff16',
  ),
  ..._prefixedTextStylePropertiesForViewTest('toolbarTextStyle'),
  ..._prefixedTextStylePropertiesForViewTest('titleTextStyle'),
  'systemOverlayStyleSystemNavigationBarColor': {
    'kind': 'color',
    'argb': '0xFF111213',
  },
  'systemOverlayStyleSystemNavigationBarDividerColor': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.outlineVariant',
  },
  'systemOverlayStyleSystemNavigationBarIconBrightness': {
    'kind': 'enum',
    'type': 'Brightness',
    'value': 'dark',
  },
  'systemOverlayStyleSystemNavigationBarContrastEnforced': {
    'kind': 'boolean',
    'value': false,
  },
  'systemOverlayStyleStatusBarColor': {'kind': 'color', 'argb': '0xFF212223'},
  'systemOverlayStyleStatusBarBrightness': {
    'kind': 'enum',
    'type': 'Brightness',
    'value': 'light',
  },
  'systemOverlayStyleStatusBarIconBrightness': {
    'kind': 'enum',
    'type': 'Brightness',
    'value': 'dark',
  },
  'systemOverlayStyleSystemStatusBarContrastEnforced': {
    'kind': 'boolean',
    'value': true,
  },
};

Map<String, Object?> _iconThemePropertiesForViewTest(
  String prefix, {
  required String shadowId,
}) => {
  '${prefix}Size': {'kind': 'integer', 'value': 27},
  '${prefix}Fill': {'kind': 'double', 'value': 0.25},
  '${prefix}Weight': {'kind': 'double', 'value': 500.0},
  '${prefix}Grade': {'kind': 'double', 'value': -10.0},
  '${prefix}OpticalSize': {'kind': 'double', 'value': 28.0},
  '${prefix}Color': {
    'kind': 'themeToken',
    'token': 'material.colorScheme.primary',
  },
  '${prefix}Opacity': {'kind': 'double', 'value': 0.65},
  '${prefix}Shadows': {
    'kind': 'shadowList',
    'items': [_shadow(shadowId, '0x80112233')],
  },
  '${prefix}ApplyTextScaling': {'kind': 'boolean', 'value': true},
};

Map<String, Object?> _prefixedTextStylePropertiesForViewTest(String prefix) => {
  '${prefix}ThemeTextStyle': {
    'kind': 'themeToken',
    'token': 'material.textTheme.titleMedium',
  },
  for (final entry in _expandedTextProperties().entries)
    if (entry.key.startsWith('style'))
      '$prefix${entry.key.substring('style'.length)}': entry.value,
};

Uint8List appBarModelBytesForViewTest() => Uint8List.fromList(
  utf8.encode(
    jsonEncode(_appBarModelJson(properties: appBarPropertiesForViewTest())),
  ),
);

Map<String, Object?> _appBarModelJson({Map<String, Object?>? properties}) {
  final json = _modelJson();
  json['root'] = _node(
    'e223fd18-36c3-469b-ae9c-d7e69dd2fd83',
    'flutter.material.Scaffold',
    slots: {
      'appBar': _single(
        _node(
          appBarWidgetIdForViewTest,
          'flutter.material.AppBar',
          properties: properties ?? const {},
          slots: {
            'leading': _single(
              _node(
                'cc68aefc-165d-4038-bf6b-dcb776f4f81c',
                'flutter.widgets.Icon',
                properties: {'icon': iconDataValueForViewTest()},
              ),
            ),
            'title': _single(
              _node(
                '146cc972-81e9-4797-a474-ee97d10f3b34',
                'flutter.widgets.Text',
                properties: {
                  'data': {'kind': 'string', 'value': 'App title'},
                },
              ),
            ),
            'actions': _list([
              _node(
                'fa56a964-786e-42fb-b470-1e40cb693082',
                'flutter.widgets.Text',
                properties: {
                  'data': {'kind': 'string', 'value': 'A1'},
                },
              ),
              _node(
                '6f1a2c85-1a13-4d71-a16c-52a64d119789',
                'flutter.widgets.Text',
                properties: {
                  'data': {'kind': 'string', 'value': 'A2'},
                },
              ),
            ]),
            'flexibleSpace': _single(
              _node(
                'd47f84e2-4900-410d-95ce-f9a2d8190cd4',
                'flutter.widgets.Center',
                slots: {'child': _single(null)},
              ),
            ),
            'bottom': _single(
              _node(
                '2f94c285-436e-4627-a903-50e201f4446a',
                'flutter.material.AppBar',
                slots: {
                  'leading': _single(null),
                  'title': _single(null),
                  'actions': _list(const []),
                  'flexibleSpace': _single(null),
                  'bottom': _single(null),
                },
              ),
            ),
          },
        ),
      ),
      'body': _single(null),
      'floatingActionButton': _single(null),
    },
  );
  return json;
}

const iconWidgetIdForViewTest = 'b3403b57-2a86-4aed-bec6-3a245af290bd';

Map<String, Object?> iconDataValueForViewTest({
  int? codePoint = 0xe5f9,
  String? fontFamily = 'MaterialIcons',
  String? fontPackage,
  bool matchTextDirection = false,
  List<String> fontFamilyFallback = const [],
}) => {
  'kind': 'iconData',
  'codePoint': codePoint,
  'fontFamily': fontFamily,
  'fontPackage': fontPackage,
  'matchTextDirection': matchTextDirection,
  'fontFamilyFallback': fontFamilyFallback,
};

Map<String, Object?> iconPropertiesForViewTest() => {
  'icon': iconDataValueForViewTest(codePoint: 0xe5fc, matchTextDirection: true),
  'size': {'kind': 'integer', 'value': 32},
  'fill': {'kind': 'double', 'value': 0.75},
  'weight': {'kind': 'double', 'value': 600.0},
  'grade': {'kind': 'double', 'value': -25.0},
  'opticalSize': {'kind': 'double', 'value': 24.0},
  'color': {'kind': 'themeToken', 'token': 'material.colorScheme.primary'},
  'shadows': {
    'kind': 'shadowList',
    'items': [
      {
        'id': 'a019424b-d086-4745-9f69-7e72844ffc7c',
        'color': {'kind': 'theme', 'token': 'material.colorScheme.shadow'},
        'offsetX': -1.0,
        'offsetY': 2.0,
        'blurRadius': 3.0,
      },
    ],
  },
  'semanticLabel': {'kind': 'string', 'value': 'Reviewed icon'},
  'textDirection': {'kind': 'enum', 'type': 'TextDirection', 'value': 'rtl'},
  'applyTextScaling': {'kind': 'boolean', 'value': false},
  'blendMode': {'kind': 'enum', 'type': 'BlendMode', 'value': 'multiply'},
  'fontWeight': {'kind': 'enum', 'type': 'FontWeight', 'value': 'w700'},
};

Uint8List iconModelBytesForViewTest({Map<String, Object?>? properties}) =>
    Uint8List.fromList(
      utf8.encode(
        jsonEncode(
          _iconModel(properties: properties ?? iconPropertiesForViewTest()),
        ),
      ),
    );

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

const elevatedButtonWidgetIdForViewTest =
    '7452c92f-78cb-40ef-8548-c374df878d90';

Uint8List elevatedButtonModelBytesForViewTest({
  Map<String, Object?>? properties,
  bool withChild = true,
}) => Uint8List.fromList(
  utf8.encode(
    jsonEncode(
      _elevatedButtonModel(
        properties: properties ?? elevatedButtonPropertiesForViewTest(),
        withChild: withChild,
      ),
    ),
  ),
);

Map<String, Object?> elevatedButtonPropertiesForViewTest() => {
  'enabled': {'kind': 'boolean', 'value': true},
  'onPressed': {'kind': 'callbackPresence'},
  'onLongPress': {'kind': 'callbackPresence'},
  'onHover': {'kind': 'callbackPresence'},
  'onFocusChange': {'kind': 'callbackPresence'},
  'autofocus': {'kind': 'boolean', 'value': true},
  'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'antiAlias'},
  for (final entry in const [
    ('style', 0),
    ('styleDisabled', 1),
    ('stylePressed', 2),
    ('styleHovered', 3),
    ('styleFocused', 4),
  ])
    ..._elevatedButtonStatePropertiesForViewTest(entry.$1, entry.$2),
  'styleVisualDensityHorizontal': {'kind': 'double', 'value': -2.0},
  'styleVisualDensityVertical': {'kind': 'double', 'value': 1.5},
  'styleTapTargetSize': {
    'kind': 'enum',
    'type': 'MaterialTapTargetSize',
    'value': 'shrinkWrap',
  },
  'styleAnimationDurationMs': {'kind': 'integer', 'value': 275},
  'styleEnableFeedback': {'kind': 'boolean', 'value': false},
  'styleAlignmentKind': {'kind': 'string', 'value': 'directional'},
  'styleAlignmentX': {'kind': 'double', 'value': 0.25},
  'styleAlignmentY': {'kind': 'double', 'value': -0.5},
  'styleSplashFactory': {'kind': 'string', 'value': 'inkSplash'},
};

Map<String, Object?> _elevatedButtonStatePropertiesForViewTest(
  String prefix,
  int index,
) {
  final channel = 0x20 + index * 0x10;
  String argb(int offset) =>
      '0xFF${(channel + offset).toRadixString(16).padLeft(2, '0').toUpperCase()}'
      '${(0x40 + offset).toRadixString(16).padLeft(2, '0').toUpperCase()}'
      '${(0x80 + offset).toRadixString(16).padLeft(2, '0').toUpperCase()}';
  final cursor = const [
    'none',
    'resizeColumn',
    'resizeRow',
    'zoomIn',
    'zoomOut',
  ][index];
  return {
    '${prefix}BackgroundColor': {'kind': 'color', 'argb': argb(0)},
    '${prefix}ForegroundColor': {
      'kind': 'themeToken',
      'token': 'material.colorScheme.onPrimary',
    },
    '${prefix}OverlayColor': {'kind': 'color', 'argb': argb(1)},
    '${prefix}ShadowColor': {
      'kind': 'themeToken',
      'token': 'material.colorScheme.shadow',
    },
    '${prefix}SurfaceTintColor': {
      'kind': 'themeToken',
      'token': 'material.colorScheme.surfaceTint',
    },
    '${prefix}Elevation': {'kind': 'double', 'value': 1.5 + index},
    '${prefix}Padding': index.isEven
        ? {
            'kind': 'edgeInsets',
            'left': 8.0 + index,
            'top': 9.0 + index,
            'right': 10.0 + index,
            'bottom': 11.0 + index,
          }
        : {
            'kind': 'edgeInsetsDirectional',
            'start': 8.0 + index,
            'top': 9.0 + index,
            'end': 10.0 + index,
            'bottom': 11.0 + index,
          },
    '${prefix}MinimumWidth': {'kind': 'integer', 'value': 40 + index},
    '${prefix}MinimumHeight': {'kind': 'double', 'value': 24.0 + index},
    '${prefix}FixedWidth': {'kind': 'double', 'value': 96.0 + index},
    '${prefix}FixedHeight': {'kind': 'integer', 'value': 44 + index},
    '${prefix}MaximumWidth': {'kind': 'double', 'value': 240.0 + index},
    '${prefix}MaximumHeight': {'kind': 'integer', 'value': 80 + index},
    '${prefix}IconColor': {'kind': 'color', 'argb': argb(2)},
    '${prefix}IconSize': {'kind': 'integer', 'value': 18 + index},
    '${prefix}SideColor': {
      'kind': 'themeToken',
      'token': 'material.colorScheme.outline',
    },
    '${prefix}SideWidth': {'kind': 'double', 'value': 1.0 + index / 4},
    '${prefix}SideStyle': {
      'kind': 'enum',
      'type': 'BorderStyle',
      'value': 'solid',
    },
    '${prefix}SideStrokeAlign': {'kind': 'double', 'value': -1.0 + index / 2},
    '${prefix}ShapeKind': {'kind': 'string', 'value': 'roundedSuperellipse'},
    '${prefix}ShapeRadiusTopLeft': {'kind': 'double', 'value': 1.0 + index},
    '${prefix}ShapeRadiusTopRight': {'kind': 'double', 'value': 2.0 + index},
    '${prefix}ShapeRadiusBottomRight': {'kind': 'double', 'value': 3.0 + index},
    '${prefix}ShapeRadiusBottomLeft': {'kind': 'double', 'value': 4.0 + index},
    '${prefix}MouseCursor': {'kind': 'string', 'value': cursor},
    '${prefix}TextTheme': {
      'kind': 'themeToken',
      'token': 'material.textTheme.labelLarge',
    },
    '${prefix}TextInherit': {'kind': 'boolean', 'value': true},
    '${prefix}TextBackgroundColor': {'kind': 'color', 'argb': argb(3)},
    '${prefix}TextFontSize': {'kind': 'double', 'value': 14.0 + index},
    '${prefix}TextFontWeight': {
      'kind': 'enum',
      'type': 'FontWeight',
      'value': 'w600',
    },
    '${prefix}TextFontStyle': {
      'kind': 'enum',
      'type': 'FontStyle',
      'value': 'italic',
    },
    '${prefix}TextLetterSpacing': {'kind': 'double', 'value': 0.25 + index},
    '${prefix}TextWordSpacing': {'kind': 'double', 'value': 0.5 + index},
    '${prefix}TextTextBaseline': {
      'kind': 'enum',
      'type': 'TextBaseline',
      'value': 'alphabetic',
    },
    '${prefix}TextHeight': {'kind': 'double', 'value': 1.2 + index / 10},
    '${prefix}TextLeadingDistribution': {
      'kind': 'enum',
      'type': 'TextLeadingDistribution',
      'value': 'even',
    },
    '${prefix}TextLocaleLanguageCode': {'kind': 'string', 'value': 'en'},
    '${prefix}TextLocaleScriptCode': {'kind': 'string', 'value': 'Latn'},
    '${prefix}TextLocaleCountryCode': {'kind': 'string', 'value': 'US'},
    '${prefix}TextShadows': {
      'kind': 'shadowList',
      'items': [
        {
          'id': '11111111-1111-4111-8111-111111111111',
          'color': {'kind': 'literal', 'argb': argb(4)},
          'offsetX': 1.0,
          'offsetY': 2.0,
          'blurRadius': 3.0,
        },
      ],
    },
    '${prefix}TextFontFeatures': {
      'kind': 'fontFeatureList',
      'items': const [
        {
          'id': '22222222-2222-4222-8222-222222222222',
          'tag': 'smcp',
          'value': 1,
        },
      ],
    },
    '${prefix}TextFontVariations': {
      'kind': 'fontVariationList',
      'items': const [
        {
          'id': '33333333-3333-4333-8333-333333333333',
          'axis': 'wght',
          'value': 650.0,
        },
      ],
    },
    '${prefix}TextDecorationUnderline': {'kind': 'boolean', 'value': true},
    '${prefix}TextDecorationOverline': {'kind': 'boolean', 'value': false},
    '${prefix}TextDecorationLineThrough': {'kind': 'boolean', 'value': true},
    '${prefix}TextDecorationColor': {
      'kind': 'themeToken',
      'token': 'material.colorScheme.error',
    },
    '${prefix}TextDecorationStyle': {
      'kind': 'enum',
      'type': 'TextDecorationStyle',
      'value': 'wavy',
    },
    '${prefix}TextDecorationThickness': {
      'kind': 'double',
      'value': 1.25 + index,
    },
    '${prefix}TextFontFamily': {'kind': 'string', 'value': 'Inter'},
    '${prefix}TextFontFamilyFallback': {
      'kind': 'string',
      'value': 'Noto Sans\nNoto Color Emoji',
    },
    '${prefix}TextPackage': {'kind': 'string', 'value': 'design_fonts'},
    '${prefix}TextOverflow': {
      'kind': 'enum',
      'type': 'TextOverflow',
      'value': 'ellipsis',
    },
  };
}

Map<String, Object?> _containerModel({
  required Map<String, Object?> properties,
  Map<String, Object?>? child,
}) {
  final model = _modelJson();
  model['root'] = _node(
    '97c8e20f-d895-42d0-a968-dc8cc0306327',
    'flutter.widgets.Container',
    properties: properties,
    slots: {'child': _single(child)},
  );
  return model;
}

Map<String, Object?> _decoratedBoxModel({
  Map<String, Object?>? decoration,
  String? position,
  String positionType = 'DecorationPosition',
  Map<String, Object?>? child,
  Map<String, Object?>? properties,
}) {
  final model = _modelJson();
  final values =
      properties ??
      <String, Object?>{
        'decoration': ?decoration,
        if (position != null)
          'position': {'kind': 'enum', 'type': positionType, 'value': position},
      };
  model['root'] = _node(
    '607a1c0f-5fd3-438c-ae36-0a054ce05949',
    'flutter.widgets.DecoratedBox',
    properties: values,
    slots: {'child': _single(child)},
  );
  return model;
}

Map<String, Object?> _excludeSemanticsModel({
  bool? excluding,
  Map<String, Object?>? child,
  Map<String, Object?>? properties,
  bool includeChildSlot = true,
}) {
  final model = _modelJson();
  model['root'] = _node(
    '2a082f33-7251-4a63-9f41-845aa0aa7a80',
    'flutter.widgets.ExcludeSemantics',
    properties:
        properties ??
        <String, Object?>{
          if (excluding != null)
            'excluding': {'kind': 'boolean', 'value': excluding},
        },
    slots: includeChildSlot ? {'child': _single(child)} : const {},
  );
  return model;
}

Map<String, Object?> _canvasBoxConstraints(
  Object? minWidth,
  Object? maxWidth,
  Object? minHeight,
  Object? maxHeight,
) => {
  'kind': 'boxConstraints',
  'minWidth': minWidth,
  'maxWidth': maxWidth,
  'minHeight': minHeight,
  'maxHeight': maxHeight,
};

Map<String, Object?> _canvasAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {
  'kind': 'alignmentGeometry',
  ..._canvasNestedAlignment(
    basis: basis,
    horizontal: horizontal,
    vertical: vertical,
  ),
};

Map<String, Object?> _canvasNestedAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {'basis': basis, 'horizontal': horizontal, 'vertical': vertical};

Map<String, Object?> _canvasLiteralColor([String argb = '0xFF112233']) => {
  'kind': 'literal',
  'argb': argb,
};

Map<String, Object?> _canvasThemeColor(String token) => {
  'kind': 'theme',
  'token': token,
};

Map<String, Object?> _canvasBorderSide({
  Map<String, Object?>? color,
  num width = 2,
  String style = 'solid',
  num strokeAlign = -1,
}) => {
  'color': color ?? _canvasLiteralColor(),
  'width': width,
  'style': style,
  'strokeAlign': strokeAlign,
};

Map<String, Object?> _canvasPhysicalBorder({
  Map<String, Object?>? top,
  Map<String, Object?>? right,
  Map<String, Object?>? bottom,
  Map<String, Object?>? left,
}) {
  final common = _canvasBorderSide();
  return {
    'kind': 'physical',
    'top': top ?? Map<String, Object?>.from(common),
    'right': right ?? Map<String, Object?>.from(common),
    'bottom': bottom ?? Map<String, Object?>.from(common),
    'left': left ?? Map<String, Object?>.from(common),
  };
}

Map<String, Object?> _canvasDirectionalBorder() {
  final common = _canvasBorderSide(
    color: _canvasThemeColor('material.colorScheme.outline'),
    width: 1,
  );
  return {
    'kind': 'directional',
    'top': Map<String, Object?>.from(common),
    'start': Map<String, Object?>.from(common),
    'end': Map<String, Object?>.from(common),
    'bottom': Map<String, Object?>.from(common),
  };
}

Map<String, Object?> _canvasRadius([num x = 8, num y = 6]) => {'x': x, 'y': y};

Map<String, Object?> _canvasPhysicalRadius() => {
  'kind': 'physical',
  'topLeft': _canvasRadius(8, 6),
  'topRight': _canvasRadius(10, 7),
  'bottomRight': _canvasRadius(12, 8),
  'bottomLeft': _canvasRadius(14, 9),
};

Map<String, Object?> _canvasDirectionalRadius() => {
  'kind': 'directional',
  'topStart': _canvasRadius(8, 6),
  'topEnd': _canvasRadius(10, 7),
  'bottomEnd': _canvasRadius(12, 8),
  'bottomStart': _canvasRadius(14, 9),
};

Map<String, Object?> _canvasBoxShadow() => {
  'id': '7984f84c-7cd3-4404-a030-efc29410a3e8',
  'color': _canvasThemeColor('material.colorScheme.shadow'),
  'offsetX': 3,
  'offsetY': 4,
  'blurRadius': 5,
  'spreadRadius': -1,
  'blurStyle': 'outer',
};

Map<String, Object?> _canvasGradientStop(String id, num stop) => {
  'id': id,
  'color': stop == 0
      ? _canvasLiteralColor('0xFF102030')
      : _canvasThemeColor('material.colorScheme.primary'),
  'stop': stop,
};

List<Map<String, Object?>> _canvasGradientStops() => [
  _canvasGradientStop('3501a21a-29f2-48e5-9f03-1bf08f93ac00', 0),
  _canvasGradientStop('e2ca03a9-2f55-418b-835f-426766679dc5', 1),
];

Map<String, Object?> _canvasLinearGradient({
  List<Map<String, Object?>>? stops,
}) => {
  'kind': 'linear',
  'begin': _canvasNestedAlignment(horizontal: -1, vertical: -1),
  'end': _canvasNestedAlignment(
    basis: 'directional',
    horizontal: 1,
    vertical: 1,
  ),
  'stops': stops ?? _canvasGradientStops(),
  'tileMode': 'mirror',
  'rotationRadians': 0.25,
};

Map<String, Object?> _canvasRadialGradient({
  Map<String, Object?>? center,
  Map<String, Object?>? focal = const {
    'basis': 'physical',
    'horizontal': 0.25,
    'vertical': -0.25,
  },
  num focalRadius = 0.1,
  num radius = 0.75,
}) => {
  'kind': 'radial',
  'center': center ?? _canvasNestedAlignment(),
  'radius': radius,
  'focal': focal,
  'focalRadius': focalRadius,
  'stops': _canvasGradientStops(),
  'tileMode': 'clamp',
  'rotationRadians': null,
};

Map<String, Object?> _canvasSweepGradient({
  num startAngle = 0,
  num endAngle = 6.28,
}) => {
  'kind': 'sweep',
  'center': _canvasNestedAlignment(
    basis: 'directional',
    horizontal: 0.1,
    vertical: 0.2,
  ),
  'startAngle': startAngle,
  'endAngle': endAngle,
  'stops': _canvasGradientStops(),
  'tileMode': 'decal',
  'rotationRadians': -0.5,
};

Map<String, Object?> _canvasImageProvider({
  String kind = 'asset',
  String assetName = 'assets/images/panel.png',
  String? packageName,
  num? exactScale,
  Map<String, Object?>? resize,
  Map<String, Object?>? resolution,
}) => {
  'kind': kind,
  'assetName': assetName,
  'packageName': packageName,
  'exactScale': exactScale,
  'resize': resize,
  'resolution':
      resolution ??
      <String, Object?>{
        'kind': 'unavailable',
        'code': 'missing',
        'reason': 'The project asset is missing',
      },
};

Map<String, Object?> _canvasDirectImageProvider({
  Map<String, Object?>? provider,
}) => {'kind': 'imageProvider', 'value': provider ?? _canvasImageProvider()};

Map<String, Object?> _canvasDecorationImage({
  Map<String, Object?>? image,
  bool onError = false,
  Map<String, Object?>? colorFilter,
  String? fit,
  Map<String, Object?>? alignment,
  Map<String, Object?>? centerSlice,
  String repeat = 'noRepeat',
  bool matchTextDirection = false,
  num scale = 1,
  num opacity = 1,
  String filterQuality = 'medium',
  bool invertColors = false,
  bool isAntiAlias = false,
}) => {
  'image': image ?? _canvasImageProvider(),
  'onError': onError,
  'colorFilter': colorFilter,
  'fit': fit,
  'alignment': alignment ?? _canvasNestedAlignment(),
  'centerSlice': centerSlice,
  'repeat': repeat,
  'matchTextDirection': matchTextDirection,
  'scale': scale,
  'opacity': opacity,
  'filterQuality': filterQuality,
  'invertColors': invertColors,
  'isAntiAlias': isAntiAlias,
};

Map<String, Object?> _canvasBoxDecoration({
  Map<String, Object?>? color,
  Map<String, Object?>? image,
  Map<String, Object?>? border,
  Map<String, Object?>? borderRadius,
  List<Map<String, Object?>> boxShadow = const [],
  Map<String, Object?>? gradient,
  String? backgroundBlendMode,
  String shape = 'rectangle',
}) => {
  'kind': 'boxDecoration',
  'color': color,
  'image': image,
  'border': border,
  'borderRadius': borderRadius,
  'boxShadow': boxShadow,
  'gradient': gradient,
  'backgroundBlendMode': backgroundBlendMode,
  'shape': shape,
};

Map<String, Object?> _elevatedButtonModel({
  required Map<String, Object?> properties,
  bool withChild = true,
}) {
  final model = _modelJson();
  final root = model['root']! as Map<String, Object?>;
  final slots = root['slots']! as Map<String, Object?>;
  slots['body'] = _single(
    _node(
      elevatedButtonWidgetIdForViewTest,
      'flutter.material.ElevatedButton',
      properties: properties,
      slots: {
        'child': _single(
          withChild
              ? _node(
                  '45c0b04d-a3c8-4dab-8bfe-692492671835',
                  'flutter.widgets.Text',
                  properties: {
                    'data': {'kind': 'string', 'value': 'Run safely'},
                  },
                )
              : null,
        ),
      },
    ),
  );
  slots.remove('floatingActionButton');
  return model;
}

Map<String, Object?> _elevatedButtonNode(Map<String, Object?> model) {
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  return body['child']! as Map<String, Object?>;
}

class _AbsentTestValue {
  const _AbsentTestValue();
}

Map<String, Object?> _modelJson() => {
  'format': 'netbeans-flutter-canvas-model',
  'protocolVersion': 18,
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
      'components': <String, Object?>{},
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

Map<String, Object?> _iconModel({required Map<String, Object?> properties}) {
  final model = _modelJson();
  final root = model['root']! as Map<String, Object?>;
  final slots = root['slots']! as Map<String, Object?>;
  slots['body'] = _single(
    _node(
      'f0901ec9-b86a-4843-a119-75bce467efb8',
      'flutter.widgets.Center',
      slots: {
        'child': _single(
          _node(
            iconWidgetIdForViewTest,
            'flutter.widgets.Icon',
            properties: properties,
          ),
        ),
      },
    ),
  );
  slots.remove('floatingActionButton');
  return model;
}

Map<String, Object?> _findNodeByType(Map<String, Object?> node, String type) {
  if (node['type'] == type) {
    return node;
  }
  for (final rawSlot in (node['slots']! as Map<String, Object?>).values) {
    final slot = rawSlot! as Map<String, Object?>;
    final child = slot['child'];
    if (child is Map<String, Object?>) {
      try {
        return _findNodeByType(child, type);
      } on StateError {
        // Continue with sibling slots.
      }
    }
    final children = slot['children'];
    if (children is List<Object?>) {
      for (final child in children.cast<Map<String, Object?>>()) {
        try {
          return _findNodeByType(child, type);
        } on StateError {
          // Continue with sibling children.
        }
      }
    }
  }
  throw StateError('Fixture does not contain $type.');
}

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
