import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

void main() {
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
    final end = contract.indexOf('W|flutter.material.ElevatedButton\n', start);
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
    final end = contract.indexOf('W|flutter.widgets.Center\n', start);
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
      final end = contract.indexOf('W|flutter.material.Scaffold\n', start);
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
    final end = contract.indexOf('W|flutter.widgets.AspectRatio\n', start);
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

Map<String, Object?> _modelJson() => {
  'format': 'netbeans-flutter-canvas-model',
  'protocolVersion': 9,
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
