import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.OutlinedButton';
const _id = 'f1dd3aa1-fc2f-45b7-a9fc-0c8b5e1ac511';
const _childId = '7c37c659-01b5-46e5-852f-11abdb77504c';
const _iconId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _states = <WidgetState, String>{
  WidgetState.disabled: 'styleDisabled',
  WidgetState.error: 'styleError',
  WidgetState.dragged: 'styleDragged',
  WidgetState.pressed: 'stylePressed',
  WidgetState.selected: 'styleSelected',
  WidgetState.scrolledUnder: 'styleScrolledUnder',
  WidgetState.hovered: 'styleHovered',
  WidgetState.focused: 'styleFocused',
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _model({
  String variant = 'standard',
  bool icon = false,
  String platform = 'windows',
  double scale = 1,
  Map<String, Object?> properties = const {},
  bool empty = false,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  (model['profile'] as Map)['textScaleFactor'] = scale;
  final button = _node(
    _id,
    _type,
    {'enabled': _bool(true), 'variant': _string(variant), ...properties},
    {
      'child': _single(
        empty
            ? _node(_childId, 'flutter.widgets.SizedBox', {
                'width': _number(0),
                'height': _number(0),
              })
            : _node(_childId, 'flutter.widgets.Text', {
                'data': _string('Action'),
              }),
      ),
      if (icon)
        'icon': _single(
          _node(_iconId, 'flutter.widgets.Text', {'data': _string('I')}),
        ),
    },
  );
  model['root'] = _node(
    'e5e23da0-f3e6-46fa-88fb-0ab84d966dd3',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          '29154d08-5472-4eae-8d4d-9352da0bbd52',
          'flutter.widgets.Center',
          {},
          {'child': _single(button)},
        ),
      ),
    },
  );
  return model;
}

Map _button(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child'] as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
OutlinedButton _sdk(WidgetTester tester) =>
    tester.widget<OutlinedButton>(find.byType(OutlinedButton));
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  void Function(CanvasDropResolver?)? onDrop,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: null,
        onSelected: selected?.add ?? (_) {},
        onDropResolverChanged: onDrop,
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

String _section(String contract) => contract.substring(
  contract.indexOf('W|$_type\n'),
  contract.indexOf('W|flutter.material.Radio\n'),
);
Map<String, Object?> _allLocalStyles() {
  final elevated = fixture.elevatedButtonPropertiesForViewTest();
  final result = <String, Object?>{
    for (final entry in elevated.entries)
      if (entry.key.startsWith('style')) entry.key: entry.value,
  };
  for (final prefix in [
    'styleError',
    'styleDragged',
    'styleSelected',
    'styleScrolledUnder',
  ]) {
    for (final entry in elevated.entries.where(
      (entry) => entry.key.startsWith('stylePressed'),
    )) {
      result['$prefix${entry.key.substring('stylePressed'.length)}'] =
          entry.value;
    }
  }
  result.addAll({
    'styleIconAlignment': _enum('IconAlignment', 'end'),
    'styleBackgroundBuilder': _reference,
    'styleForegroundBuilder': _reference,
  });
  return result;
}

List<double> _geometry(WidgetTester tester, bool icon) {
  final box = tester.renderObject<RenderBox>(find.byType(OutlinedButton));
  final label = box.globalToLocal(tester.getTopLeft(find.text('Action')));
  final iconOffset = icon
      ? box.globalToLocal(tester.getTopLeft(find.text('I')))
      : Offset.zero;
  final labelSize = tester.getSize(find.text('Action'));
  return [
    box.size.width,
    box.size.height,
    label.dx,
    label.dy,
    labelSize.width,
    labelSize.height,
    iconOffset.dx,
    iconOffset.dy,
  ];
}

Map<String, Object?> _paint() => {
  'kind': 'paint',
  'color': {'kind': 'literal', 'argb': '0xFF123456'},
  'blendMode': 'srcOver',
  'style': 'fill',
  'strokeWidth': 0.0,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
};

Future<(Size, Uint8List)> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage(pixelRatio: 1);
  try {
    final bytes = (await image.toByteData(format: ui.ImageByteFormat.rawRgba))!;
    return (
      Size(image.width.toDouble(), image.height.toDouble()),
      bytes.buffer.asUint8List(),
    );
  } finally {
    image.dispose();
  }
}))!;

void main() {
  test(
    'OutlinedButton does not expose the inherited semantic constructor property',
    () {
      for (final variant in ['standard', 'icon']) {
        for (final value in [_null, _bool(true), _bool(false)]) {
          expect(
            () => _decode(
              _model(variant: variant, properties: {'isSemanticButton': value}),
            ),
            throwsFormatException,
          );
        }
      }
    },
  );

  for (final m3 in [false, true]) {
    testWidgets(
      'M${m3 ? 3 : 2} outline pixels match raw SDK and default side overrides shape side',
      (tester) async {
        final scheme = ColorScheme.fromSeed(seedColor: Colors.teal);
        final theme = ThemeData(
          useMaterial3: m3,
          colorScheme: scheme,
          outlinedButtonTheme: const OutlinedButtonThemeData(
            style: ButtonStyle(
              shape: WidgetStatePropertyAll(
                RoundedRectangleBorder(
                  borderRadius: BorderRadius.all(Radius.circular(9)),
                  side: BorderSide(color: Colors.red, width: 7),
                ),
              ),
            ),
          ),
          // Unrelated button themes must never influence this widget.
          textButtonTheme: const TextButtonThemeData(
            style: ButtonStyle(
              side: WidgetStatePropertyAll(
                BorderSide(color: Colors.purple, width: 19),
              ),
            ),
          ),
        );
        for (final enabled in [true, false]) {
          for (final kind in ['default', 'shape', 'partialSide', 'localSide']) {
            final model = _model(
              properties: {
                'enabled': _bool(enabled),
                if (kind == 'shape') 'styleShapeKind': _string('stadium'),
                if (kind == 'partialSide') ...{
                  'styleSideWidth': _number(3.0),
                  'styleDisabledSideWidth': _number(3.0),
                },
                if (kind == 'localSide') ...{
                  'styleSideColor': _color(0xff00aa44),
                  'styleSideWidth': _number(3.0),
                  'styleDisabledSideColor': _color(0xff00aa44),
                  'styleDisabledSideWidth': _number(3.0),
                },
              },
            );
            final center =
                (model['root'] as Map)['slots']['body']['child'] as Map;
            final button = center['slots']['child']['child'];
            const boundaryId = '6e0c7b68-68ac-46e9-9a9d-a852823c7239';
            center['slots']['child'] = _single(
              _node(boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(button),
              }),
            );
            await _pump(tester, model, theme: theme);
            final states = <WidgetState>{if (!enabled) WidgetState.disabled};
            final defaults = _sdk(
              tester,
            ).defaultStyleOf(tester.element(find.byType(OutlinedButton)));
            final defaultSide = defaults.side!.resolve(states)!;
            expect(defaultSide.color, isNot(Colors.red));
            final expectedSide = kind == 'localSide'
                ? const BorderSide(color: Color(0xff00aa44), width: 3)
                : kind == 'partialSide'
                ? defaultSide.copyWith(width: 3)
                : defaultSide;
            final effectiveShape =
                tester
                        .widgetList<Material>(
                          find.descendant(
                            of: find.byType(OutlinedButton),
                            matching: find.byType(Material),
                          ),
                        )
                        .first
                        .shape!
                    as OutlinedBorder;
            expect(
              effectiveShape.side,
              expectedSide,
              reason: '$kind enabled=$enabled',
            );
            // Compare the SDK paint, not the intentional Designer selection
            // outlines around each model node. No SDK painter is changed.
            for (final id in [_id, _childId]) {
              tester
                      .renderObject<RenderCustomPaint>(
                        find.byKey(ValueKey('canvas-widget-outline-$id')),
                      )
                      .foregroundPainter =
                  null;
            }
            await tester.pump();
            final canvasBoundary = tester.renderObject<RenderRepaintBoundary>(
              find
                  .descendant(
                    of: _widget(boundaryId),
                    matching: find.byType(RepaintBoundary),
                  )
                  .first,
            );
            final canvasPixels = await _pixels(tester, canvasBoundary);
            final rawKey = GlobalKey();
            final rawStyle = kind == 'default'
                ? null
                : ButtonStyle(
                    shape: kind == 'shape'
                        ? WidgetStateProperty.resolveWith<OutlinedBorder?>(
                            (states) => states.contains(WidgetState.disabled)
                                ? null
                                : const StadiumBorder(),
                          )
                        : null,
                    side: kind == 'partialSide' || kind == 'localSide'
                        ? WidgetStatePropertyAll(expectedSide)
                        : null,
                  );
            await tester.pumpWidget(
              MaterialApp(
                theme: theme,
                home: Scaffold(
                  body: Center(
                    child: RepaintBoundary(
                      key: rawKey,
                      child: OutlinedButton(
                        onPressed: enabled ? () {} : null,
                        style: rawStyle,
                        child: const Text('Action'),
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump(const Duration(milliseconds: 300));
            final rawBoundary =
                rawKey.currentContext!.findRenderObject()!
                    as RenderRepaintBoundary;
            final rawPixels = await _pixels(tester, rawBoundary);
            expect(canvasPixels.$1, rawPixels.$1);
            expect(
              canvasPixels.$2,
              orderedEquals(rawPixels.$2),
              reason: '$kind enabled=$enabled',
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );
  }

  testWidgets(
    'outlined side theme resolves all states before default or shape side',
    (tester) async {
      final side = WidgetStateProperty.resolveWith<BorderSide?>(
        (states) => states.contains(WidgetState.disabled)
            ? const BorderSide(color: Colors.orange, width: 4)
            : states.contains(WidgetState.error)
            ? const BorderSide(color: Colors.blue, width: 5)
            : const BorderSide(color: Colors.green, width: 6),
      );
      final theme = ThemeData(
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: ButtonStyle(
            side: side,
            shape: const WidgetStatePropertyAll(
              StadiumBorder(side: BorderSide(color: Colors.red, width: 9)),
            ),
          ),
        ),
      );
      await _pump(
        tester,
        _model(
          properties: {
            'styleSideStrokeAlign': _number(0.25),
            for (final prefix in _states.values)
              '${prefix}SideStrokeAlign': _number(0.25),
          },
        ),
        theme: theme,
      );
      final style = _sdk(tester).style!;
      for (final state in [
        <WidgetState>{},
        for (final value in _states.keys) {value},
      ]) {
        expect(
          style.side!.resolve(state),
          side.resolve(state)!.copyWith(strokeAlign: 0.25),
        );
      }
    },
  );

  test(
    'OutlinedButton capability has exactly 510 rows and two precise slots',
    () {
      final section = _section(canvasRuntimeWidgetSchemaContractForTesting());
      expect(section, _section(canvasReviewedWidgetSchemaContract));
      expect(
        section.split('\n').where((line) => line.startsWith('P|')),
        hasLength(510),
      );
      expect(
        section,
        contains('P|enabled|boolean|1|boolean:true|-|boolean:any\n'),
      );
      expect(
        section,
        contains('S|child|single|1|1|1|any\nS|icon|single|0|0|1|any\n'),
      );
      expect(
        section,
        contains('C|$_type|paletteCreate|wrapExistingChild|child'),
      );
    },
  );

  test(
    'OutlinedButton decodes 480 jointly compatible local style leaves with distinct references',
    () {
      final styles = _allLocalStyles();
      // Circle eccentricity/radii and TextStyle background paint/color are
      // mutually exclusive; the remaining 18 leaves are covered separately.
      expect(styles, hasLength(480));
      final model = _model(properties: styles);
      expect(
        _decode(
          model,
        ).root.slot('body')!.child!.slot('child')!.child!.properties,
        hasLength(482),
      );
      for (final variant in ['standard', 'icon']) {
        expect(
          () => _decode(
            _model(
              variant: variant,
              properties: {
                'style': _reference,
                'focusNode': _reference,
                'statesController': _reference,
                for (final name in [
                  'onPressed',
                  'onLongPress',
                  'onHover',
                  'onFocusChange',
                ])
                  name: _reference,
              },
            ),
          ),
          returnsNormally,
        );
      }
    },
  );

  for (final bad in <Map<String, Object?>>[
    {'style': _reference, 'styleErrorBackgroundColor': _color(0xff123456)},
    {'style': _reference, 'styleBackgroundBuilder': _reference},
    {'iconAlignment': _enum('IconAlignment', 'end')},
    {
      'onPressed': {'kind': 'callbackPresence'},
    },
    {
      'onHover': {
        'kind': 'dartObjectReferencePresence',
        'expression': 'runProjectCode()',
      },
    },
    {
      'styleErrorMinimumWidth': _number(40),
      'styleErrorMaximumWidth': _number(30),
    },
    {'styleDraggedShapeRadiusTopLeft': _number(4.0)},
    {'styleSelectedTextPackage': _string('missing_family')},
  ]) {
    test(
      'OutlinedButton rejects invalid ${bad.keys.join('/')}',
      () =>
          expect(() => _decode(_model(properties: bad)), throwsFormatException),
    );
  }
  test('constructor-only slots and nullable semantic flag are exact', () {
    expect(() => _decode(_model(icon: true)), throwsFormatException);
    expect(
      () => _decode(
        _model(variant: 'icon', properties: {'isSemanticButton': _null}),
      ),
      throwsFormatException,
    );
    final missing = _model();
    (_button(missing)['slots'] as Map).remove('child');
    expect(() => _decode(missing), throwsFormatException);
    (_button(missing)['slots'] as Map)['child'] = _single(null);
    expect(() => _decode(missing), throwsFormatException);
    for (final name in ['enabled', 'variant']) {
      final data = _model();
      (_button(data)['properties'] as Map).remove(name);
      expect(() => _decode(data), throwsFormatException);
    }
  });

  test('densest legal OutlinedButton payload preserves 490 exact values', () {
    final properties = {
      ..._allLocalStyles(),
      for (final name in [
        'onPressed',
        'onLongPress',
        'onHover',
        'onFocusChange',
        'focusNode',
        'statesController',
      ])
        name: _reference,
      'autofocus': _bool(true),
      'clipBehavior': _null,
    };
    final model = _model(properties: properties);
    final node = _decode(model).root.slot('body')!.child!.slot('child')!.child!;
    expect(node.properties, hasLength(490));
    expect(node.properties.keys, containsAll(properties.keys));
    final iconModel = _model(
      variant: 'icon',
      icon: true,
      properties: {
        ...properties,
        'iconAlignment': _enum('IconAlignment', 'end'),
      },
    );
    expect(
      _decode(
        iconModel,
      ).root.slot('body')!.child!.slot('child')!.child!.properties,
      hasLength(491),
    );
  });

  test(
    'effective local bounds reject conflicts across all enabled combinations',
    () {
      final prefixes = _states.values
          .where((prefix) => prefix != 'styleDisabled')
          .toList();
      for (final minimum in prefixes) {
        for (final maximum in prefixes) {
          expect(
            () => _decode(
              _model(
                properties: {
                  '${minimum}MinimumWidth': _number(120),
                  '${maximum}MaximumWidth': _number(100),
                },
              ),
            ),
            throwsFormatException,
            reason: '$minimum/$maximum',
          );
        }
      }
      expect(
        () => _decode(
          _model(
            properties: {
              'styleDisabledMinimumWidth': _number(120),
              'styleMaximumWidth': _number(100),
            },
          ),
        ),
        returnsNormally,
      );
      expect(
        () => _decode(
          _model(
            properties: {
              'styleMinimumWidth': _number(120),
              'styleDisabledMaximumWidth': _number(100),
            },
          ),
        ),
        returnsNormally,
      );
    },
  );

  test('effective shape kind conflicts fail closed across the new states', () {
    for (final prefix in _states.values.where(
      (prefix) => prefix != 'styleDisabled',
    )) {
      expect(
        () => _decode(
          _model(
            properties: {
              'styleShapeKind': _string('roundedRectangle'),
              'styleShapeRadiusTopLeft': _number(9.0),
              '${prefix}ShapeKind': _string('circle'),
            },
          ),
        ),
        throwsFormatException,
      );
      expect(
        () => _decode(
          _model(
            properties: {
              'styleShapeKind': _string('circle'),
              'styleShapeCircleEccentricity': _number(0.4),
              '${prefix}ShapeKind': _string('stadium'),
            },
          ),
        ),
        throwsFormatException,
      );
    }
  });

  testWidgets(
    'unknown theme floors and out-of-bounds fixed size retain SDK clamping',
    (tester) async {
      await _pump(
        tester,
        _model(
          properties: {
            'styleMaximumHeight': _number(20),
            'styleFixedHeight': _number(80),
            'styleTapTargetSize': _enum('MaterialTapTargetSize', 'shrinkWrap'),
          },
        ),
        theme: ThemeData(useMaterial3: true),
      );
      final style = _sdk(tester).style!;
      expect(style.minimumSize!.resolve({})!.height, 40);
      expect(style.maximumSize!.resolve({})!.height, 40);
      expect(style.fixedSize!.resolve({})!.height, 80);
      expect(
        tester
            .getSize(
              find
                  .descendant(
                    of: find.byType(OutlinedButton),
                    matching: find.byType(Material),
                  )
                  .first,
            )
            .height,
        40,
      );
      expect(
        () => _decode(
          _model(
            properties: {
              'styleMinimumWidth': _number(120),
              'styleFixedWidth': _number(1),
            },
          ),
        ),
        returnsNormally,
      );
      expect(tester.takeException(), isNull);
    },
  );

  for (final platform in ['windows', 'web']) {
    for (final m3 in [false, true]) {
      for (final variant in ['standard', 'icon']) {
        testWidgets(
          '$platform M${m3 ? 3 : 2} $variant actual SDK default/style identity',
          (tester) async {
            final theme = ThemeData(useMaterial3: m3);
            await _pump(
              tester,
              _model(
                platform: platform,
                variant: variant,
                icon: variant == 'icon',
              ),
              theme: theme,
            );
            final sdk = _sdk(tester);
            expect(sdk.enabled, isTrue);
            expect(sdk.style, isNull);
            expect(sdk.clipBehavior, isNull);
            final defaults = sdk.defaultStyleOf(
              tester.element(find.byType(OutlinedButton)),
            );
            expect(defaults.elevation!.resolve({}), 0);
            expect(defaults.minimumSize!.resolve({}), Size(64, m3 ? 40 : 36));
            expect(
              defaults.shape!.resolve({}),
              m3 ? isA<StadiumBorder>() : isA<RoundedRectangleBorder>(),
            );
            expect(find.text('Action'), findsOneWidget);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }

  testWidgets(
    'all state buckets resolve with exact priority and isolated disabled',
    (tester) async {
      final properties = <String, Object?>{
        'styleBackgroundColor': _color(0xff111111),
      };
      var i = 0;
      for (final entry in _states.entries) {
        properties['${entry.value}BackgroundColor'] = _color(0xff220000 + i++);
      }
      await _pump(tester, _model(properties: properties));
      final style = _sdk(tester).style!;
      final remaining = _states.keys.toSet();
      i = 0;
      for (final entry in _states.entries) {
        expect(
          style.backgroundColor!.resolve(remaining),
          Color(0xff220000 + i++),
        );
        remaining.remove(entry.key);
      }
      expect(style.backgroundColor!.resolve({}), const Color(0xff111111));
      await _pump(
        tester,
        _model(
          properties: {
            'styleBackgroundColor': _color(0xff111111),
            'styleErrorBackgroundColor': _color(0xff222222),
          },
        ),
      );
      expect(
        _sdk(tester).style!.backgroundColor!.resolve({
          WidgetState.disabled,
          WidgetState.error,
        }),
        isNull,
      );
    },
  );

  testWidgets(
    'all jointly compatible local style fields resolve every state without losing SDK child',
    (tester) async {
      await _pump(
        tester,
        _model(variant: 'icon', icon: true, properties: _allLocalStyles()),
      );
      final style = _sdk(tester).style!;
      for (final states in [
        <WidgetState>{},
        for (final state in _states.keys) {state},
        _states.keys.toSet(),
      ]) {
        expect(style.textStyle!.resolve(states), isNotNull);
        expect(style.backgroundColor!.resolve(states), isNotNull);
        expect(style.foregroundColor!.resolve(states), isNotNull);
        expect(style.overlayColor!.resolve(states), isNotNull);
        expect(style.shadowColor!.resolve(states), isNotNull);
        expect(style.surfaceTintColor!.resolve(states), isNotNull);
        expect(style.elevation!.resolve(states), isNotNull);
        expect(style.padding!.resolve(states), isNotNull);
        expect(style.minimumSize!.resolve(states), isNotNull);
        expect(style.fixedSize!.resolve(states), isNotNull);
        expect(style.maximumSize!.resolve(states), isNotNull);
        expect(style.iconColor!.resolve(states), isNotNull);
        expect(style.iconSize!.resolve(states), isNotNull);
        expect(style.side!.resolve(states), isNotNull);
        expect(style.shape!.resolve(states), isNotNull);
        expect(style.mouseCursor!.resolve(states), isNotNull);
      }
      expect(
        style.visualDensity,
        const VisualDensity(horizontal: -2, vertical: 1.5),
      );
      expect(style.iconAlignment, IconAlignment.end);
      expect(style.backgroundBuilder, isNotNull);
      expect(style.foregroundBuilder, isNotNull);
      expect(_diagnostics(tester), contains('identity layers'));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'project references are isolated with actual button/child and explicit warning',
    (tester) async {
      await _pump(
        tester,
        _model(
          properties: {
            'style': _reference,
            'focusNode': _reference,
            'statesController': _reference,
            'onPressed': _reference,
            'onLongPress': _reference,
            'onHover': _reference,
            'onFocusChange': _reference,
          },
        ),
      );
      final sdk = _sdk(tester);
      expect(sdk.style, isNull);
      expect(sdk.focusNode, isNull);
      expect(sdk.statesController, isNull);
      expect(sdk.onPressed, isNotNull);
      expect(sdk.onLongPress, isNotNull);
      sdk.onPressed!();
      sdk.onLongPress!();
      sdk.onHover!(true);
      sdk.onFocusChange!(true);
      final diagnostics = _diagnostics(tester);
      expect(diagnostics, contains('SDK default/theme preview'));
      expect(diagnostics, contains('isolated local state'));
      expect(diagnostics, contains('benign no-ops'));
      expect(find.text('Action'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'both constructors retain nullable clip with explicit layer builders',
    (tester) async {
      for (final variant in ['standard', 'icon']) {
        for (final explicitNull in [false, true]) {
          await _pump(
            tester,
            _model(
              variant: variant,
              properties: {
                'styleBackgroundBuilder': _reference,
                if (explicitNull) 'clipBehavior': _null,
              },
            ),
          );
          final material = tester
              .widgetList<Material>(
                find.descendant(
                  of: find.byType(OutlinedButton),
                  matching: find.byType(Material),
                ),
              )
              .last;
          expect(material.clipBehavior, Clip.antiAlias);
          expect(find.text('Action'), findsOneWidget);
        }
      }
    },
  );

  testWidgets(
    'semantic button role remains SDK true with enabled and disabled state',
    (tester) async {
      final semantics = tester.ensureSemantics();
      for (final enabled in [true, false]) {
        await _pump(tester, _model(properties: {'enabled': _bool(enabled)}));
        expect(_sdk(tester).isSemanticButton, isTrue);
        expect(_sdk(tester).enabled, enabled);
        final sem = tester
            .widgetList<Semantics>(
              find.descendant(
                of: find.byType(OutlinedButton),
                matching: find.byType(Semantics),
              ),
            )
            .first;
        expect(sem.properties.button, isTrue);
        expect(sem.properties.enabled, enabled);
      }
      semantics.dispose();
    },
  );

  for (final m3 in [false, true]) {
    for (final rtl in [false, true]) {
      testWidgets(
        'raw SDK M${m3 ? 3 : 2} ${rtl ? 'RTL' : 'LTR'} padding/gap/scaling parity',
        (tester) async {
          final theme = ThemeData(useMaterial3: m3);
          for (final scale in [1.0, 2.0, 3.0]) {
            for (final shape in ['standard', 'icon-empty', 'icon-present']) {
              final icon = shape == 'icon-present';
              final variant = shape == 'standard' ? 'standard' : 'icon';
              final model = _model(variant: variant, icon: icon, scale: scale);
              // Directionality is a real model widget, not an extra renderer assumption.
              if (rtl) {
                final body = (model['root'] as Map)['slots']['body']['child'];
                (model['root'] as Map)['slots']['body']['child'] = _node(
                  'b51c7e37-e117-471b-a324-5859129861bd',
                  'flutter.widgets.Directionality',
                  {'textDirection': _enum('TextDirection', 'rtl')},
                  {'child': _single(body)},
                );
              }
              await _pump(tester, model, theme: theme);
              final canvas = _geometry(tester, icon);
              final sdk = variant == 'standard'
                  ? OutlinedButton(
                      onPressed: () {},
                      child: const Text('Action'),
                    )
                  : OutlinedButton.icon(
                      onPressed: () {},
                      icon: icon ? const Text('I') : null,
                      label: const Text('Action'),
                    );
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  home: MediaQuery(
                    data: MediaQueryData(textScaler: TextScaler.linear(scale)),
                    child: Directionality(
                      textDirection: rtl
                          ? TextDirection.rtl
                          : TextDirection.ltr,
                      child: Scaffold(body: Center(child: sdk)),
                    ),
                  ),
                ),
              );
              await tester.pump(const Duration(milliseconds: 300));
              final raw = _geometry(tester, icon);
              for (var i = 0; i < raw.length; i++) {
                expect(
                  canvas[i],
                  closeTo(raw[i], 0.001),
                  reason: '$shape scale$scale metric$i',
                );
              }
              expect(tester.takeException(), isNull);
            }
          }
        },
      );
    }
  }

  testWidgets(
    'SDK icon alignment precedence is constructor then theme then local style',
    (tester) async {
      for (final rtl in [false, true]) {
        for (final constructor in [false, true]) {
          final model = _model(
            variant: 'icon',
            icon: true,
            properties: {
              'styleIconAlignment': _enum('IconAlignment', 'start'),
              if (constructor) 'iconAlignment': _enum('IconAlignment', 'start'),
            },
          );
          if (rtl) {
            final body = (model['root'] as Map)['slots']['body']['child'];
            (model['root'] as Map)['slots']['body']['child'] = _node(
              'b51c7e37-e117-471b-a324-5859129861bd',
              'flutter.widgets.Directionality',
              {'textDirection': _enum('TextDirection', 'rtl')},
              {'child': _single(body)},
            );
          }
          await _pump(
            tester,
            model,
            theme: ThemeData(
              outlinedButtonTheme: const OutlinedButtonThemeData(
                style: ButtonStyle(iconAlignment: IconAlignment.end),
              ),
            ),
          );
          final iconLeft =
              tester.getTopLeft(find.text('I')).dx <
              tester.getTopLeft(find.text('Action')).dx;
          expect(iconLeft, constructor != rtl);
        }
      }
    },
  );

  testWidgets(
    'partial compound styles use OutlinedButtonTheme and all added state layers',
    (tester) async {
      const inherited = ButtonStyle(
        minimumSize: WidgetStatePropertyAll(Size(93, 37)),
        maximumSize: WidgetStatePropertyAll(Size(333, 111)),
        fixedSize: WidgetStatePropertyAll(Size(177, 73)),
        textStyle: WidgetStatePropertyAll(
          TextStyle(fontSize: 19, letterSpacing: 2, locale: Locale('en', 'US')),
        ),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(9)),
            side: BorderSide(color: Colors.red, width: 3),
          ),
        ),
        visualDensity: VisualDensity(horizontal: 1, vertical: 2),
      );
      final theme = ThemeData(
        outlinedButtonTheme: const OutlinedButtonThemeData(style: inherited),
        elevatedButtonTheme: const ElevatedButtonThemeData(
          style: ButtonStyle(
            minimumSize: WidgetStatePropertyAll(Size(900, 900)),
          ),
        ),
      );
      final properties = <String, Object?>{
        'styleTextFontSize': _number(21.0),
        'styleMinimumWidth': _number(101),
        'styleVisualDensityHorizontal': _number(-1.0),
        'styleShapeKind': _string('roundedRectangle'),
        'styleShapeRadiusTopLeft': _number(5.0),
      };
      var count = 0;
      for (final prefix in _states.values) {
        properties.addAll({
          '${prefix}MinimumHeight': _number(40 + count),
          '${prefix}FixedWidth': _number(150 + count),
          '${prefix}SideWidth': _number(2.0 + count),
          '${prefix}TextWordSpacing': _number(3.0 + count),
        });
        count++;
      }
      await _pump(tester, _model(properties: properties), theme: theme);
      final style = _sdk(tester).style!;
      count = 0;
      for (final state in _states.keys) {
        final set = {state};
        expect(
          style.minimumSize!.resolve(set),
          Size(state == WidgetState.disabled ? 93 : 101, 40.0 + count),
        );
        expect(style.fixedSize!.resolve(set), Size(150.0 + count, 73));
        expect(style.textStyle!.resolve(set)!.letterSpacing, 2);
        expect(
          style.textStyle!.resolve(set)!.fontSize,
          state == WidgetState.disabled ? 19 : 21,
        );
        expect(style.textStyle!.resolve(set)!.wordSpacing, 3.0 + count);
        expect(style.side!.resolve(set)!.width, 2.0 + count);
        expect(
          style.side!.resolve(set)!.color,
          _sdk(tester)
              .defaultStyleOf(tester.element(find.byType(OutlinedButton)))
              .side!
              .resolve(set)!
              .color,
        );
        count++;
      }
      final shape =
          style.shape!.resolve({WidgetState.error})! as RoundedRectangleBorder;
      expect(
        shape.borderRadius,
        const BorderRadius.only(
          topLeft: Radius.circular(5),
          topRight: Radius.circular(9),
          bottomLeft: Radius.circular(9),
          bottomRight: Radius.circular(9),
        ),
      );
      expect(
        style.visualDensity,
        const VisualDensity(horizontal: -1, vertical: 2),
      );
    },
  );

  testWidgets(
    'remaining circle eccentricity and Paint leaves render across every bucket',
    (tester) async {
      final props = <String, Object?>{};
      for (final prefix in ['style', ..._states.values]) {
        props.addAll({
          '${prefix}ShapeKind': _string('circle'),
          '${prefix}ShapeCircleEccentricity': _number(0.4),
          '${prefix}TextBackground': _paint(),
        });
      }
      await _pump(tester, _model(properties: props));
      final style = _sdk(tester).style!;
      for (final state in [
        <WidgetState>{},
        for (final value in _states.keys) {value},
      ]) {
        expect(
          (style.shape!.resolve(state)! as CircleBorder).eccentricity,
          0.4,
        );
        expect(
          style.textStyle!.resolve(state)!.background!.color.toARGB32(),
          0xff123456,
        );
      }
      final represented = {..._allLocalStyles().keys, ...props.keys};
      expect(represented, hasLength(498));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'real local press hover focus and disabled lifecycle never execute project code',
    (tester) async {
      final props = {
        'onPressed': _reference,
        'onLongPress': _reference,
        'onHover': _reference,
        'onFocusChange': _reference,
        'autofocus': _bool(true),
        'styleBackgroundColor': _color(0xff111111),
        'stylePressedBackgroundColor': _color(0xff222222),
        'styleHoveredBackgroundColor': _color(0xff333333),
        'styleFocusedBackgroundColor': _color(0xff444444),
        'styleDisabledBackgroundColor': _color(0xff555555),
      };
      await _pump(tester, _model(properties: props));
      final state = tester.state(find.byType(OutlinedButton));
      final childElement = tester.element(
        find
            .descendant(
              of: _widget(_childId),
              matching: find.byType(GestureDetector),
            )
            .first,
      );
      final ink = tester.widget<InkWell>(
        find
            .descendant(
              of: find.byType(OutlinedButton),
              matching: find.byType(InkWell),
            )
            .first,
      );
      final controller = ink.statesController!;
      expect(controller.value, contains(WidgetState.focused));
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: Offset.zero);
      await mouse.moveTo(tester.getCenter(find.byType(OutlinedButton)));
      await tester.pump(const Duration(milliseconds: 300));
      expect(controller.value, contains(WidgetState.hovered));
      final gesture = await tester.startGesture(
        tester.getCenter(find.byType(OutlinedButton)),
      );
      await tester.pump(const Duration(milliseconds: 300));
      expect(controller.value, contains(WidgetState.pressed));
      final material = tester.widget<Material>(
        find
            .descendant(
              of: find.byType(OutlinedButton),
              matching: find.byType(Material),
            )
            .first,
      );
      expect(material.color, const Color(0xff222222));
      await gesture.up();
      await mouse.removePointer();
      await _pump(
        tester,
        _model(
          variant: 'icon',
          icon: true,
          properties: {
            ...props,
            'enabled': _bool(false),
            'statesController': _reference,
            'focusNode': _reference,
          },
        ),
      );
      expect(tester.state(find.byType(OutlinedButton)), same(state));
      expect(
        tester.element(
          find
              .descendant(
                of: _widget(_childId),
                matching: find.byType(GestureDetector),
              )
              .first,
        ),
        same(childElement),
      );
      expect(controller.value, contains(WidgetState.disabled));
      expect(controller.value, isNot(contains(WidgetState.pressed)));
      await _pump(tester, _model(properties: props));
      expect(tester.state(find.byType(OutlinedButton)), same(state));
      expect(
        tester.element(
          find
              .descendant(
                of: _widget(_childId),
                matching: find.byType(GestureDetector),
              )
              .first,
        ),
        same(childElement),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'optional icon destination exists only in icon mode and never substitutes required child',
    (tester) async {
      CanvasDropResolver? resolver;
      for (final variant in ['standard', 'icon']) {
        await _pump(
          tester,
          _model(variant: variant),
          onDrop: (value) => resolver = value,
        );
        final surface = tester.getRect(
          find.byKey(const ValueKey('canvas-interaction-surface')),
        );
        final point = tester.getCenter(find.byType(OutlinedButton));
        final target = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'text',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        expect(target?.parentWidgetId == _id, variant == 'icon');
        if (variant == 'icon') {
          expect(target?.slotName, 'icon');
          expect(target?.insertionIndex, 0);
        }
      }
      expect(isCanvasRequiredChildWrapperWidgetType(_type), isTrue);
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Text',
        ),
        isTrue,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Expanded',
        ),
        isFalse,
      );
    },
  );

  testWidgets(
    'stateful child and SDK controller survive constructor/style/history edits',
    (tester) async {
      Map<String, Object?> withField(
        String variant,
        bool icon,
        Map<String, Object?> props,
      ) {
        final model = _model(variant: variant, icon: icon, properties: props);
        (_button(model)['slots'] as Map)['child'] = _single(
          _node(_childId, 'flutter.material.TextField', {}),
        );
        return model;
      }

      await _pump(tester, withField('standard', false, {}));
      final childState = tester.state(find.byType(TextField));
      final editState = tester.state<EditableTextState>(
        find.byType(EditableText),
      );
      final buttonState = tester.state(find.byType(OutlinedButton));
      editState.widget.controller.text = 'Unsaved local child state';
      for (final edit in [
        ('icon', true, {'styleForegroundColor': _color(0xff225588)}),
        ('icon', false, {'style': _reference}),
        ('standard', false, {'styleForegroundBuilder': _reference}),
        ('standard', false, <String, Object?>{}),
      ]) {
        await _pump(tester, withField(edit.$1, edit.$2, edit.$3));
        expect(tester.state(find.byType(OutlinedButton)), same(buttonState));
        expect(tester.state(find.byType(TextField)), same(childState));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(editState),
        );
        expect(editState.widget.controller.text, 'Unsaved local child state');
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'local font package and inherit-false branches cover all added states',
    (tester) async {
      for (final prefix in [
        'styleError',
        'styleDragged',
        'styleSelected',
        'styleScrolledUnder',
      ]) {
        final state = _states.entries
            .singleWhere((entry) => entry.value == prefix)
            .key;
        await _pump(
          tester,
          _model(
            properties: {
              'styleTextFontFamily': _string('BaseFont'),
              '${prefix}TextPackage': _string('local_fonts'),
            },
          ),
        );
        expect(
          _sdk(tester).style!.textStyle!.resolve({state})!.fontFamily,
          'packages/local_fonts/BaseFont',
        );
        await _pump(
          tester,
          _model(
            properties: {
              'styleTextInherit': _bool(false),
              'styleDisabledTextInherit': _bool(false),
              '${prefix}TextInherit': _bool(false),
              '${prefix}TextFontFamily': _string('OwnFont'),
              '${prefix}TextPackage': _string('local_fonts'),
              '${prefix}TextFontSize': _number(22.0),
            },
          ),
        );
        final text = _sdk(tester).style!.textStyle!.resolve({state})!;
        expect(text.inherit, isFalse);
        expect(text.fontFamily, 'packages/local_fonts/OwnFont');
        expect(text.fontSize, 22);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'explicit empty fallback preserves approved base package dependency',
    (tester) async {
      await _pump(
        tester,
        _model(
          properties: {
            'styleTextFontFamilyFallback': _string('BaseFont'),
            'styleErrorTextFontFamilyFallback': _string(''),
            'styleErrorTextPackage': _string('local_fonts'),
          },
        ),
      );
      final text = _sdk(tester).style!.textStyle!.resolve({WidgetState.error})!;
      expect(text.fontFamilyFallback, isEmpty);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'multiple OutlinedButtons own independent retained diagnostic keys',
    (tester) async {
      const secondId = 'a28b2472-241a-4569-af8d-9719b908c753';
      Map<String, Object?> multiple(bool refs) {
        final model = _model();
        final first = _button(
          _model(properties: refs ? {'style': _reference} : {}),
        );
        final second = _button(
          _model(
            properties: refs ? {} : {'styleForegroundBuilder': _reference},
          ),
        );
        second['id'] = secondId;
        second['slots']['child']['child']['id'] = _iconId;
        final center = model['root'] as Map;
        center['slots']['body']['child']['slots']['child']['child'] = _node(
          '2e9bb6ca-9143-4b61-bdd6-76d9ae823f1d',
          'flutter.widgets.Column',
          {'mainAxisSize': _enum('MainAxisSize', 'min')},
          {
            'children': {
              'kind': 'list',
              'children': [first, second],
            },
          },
        );
        return model;
      }

      Finder button(String id) => find.descendant(
        of: _widget(id),
        matching: find.byType(OutlinedButton),
      );
      await _pump(tester, multiple(false));
      final first = tester.state(button(_id));
      final second = tester.state(button(secondId));
      expect(identical(first, second), isFalse);
      for (final refs in [true, false, true]) {
        await _pump(tester, multiple(refs));
        expect(tester.state(button(_id)), same(first));
        expect(tester.state(button(secondId)), same(second));
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'theme builders preserve the shared omitted and explicit-null clip behavior',
    (tester) async {
      final theme = ThemeData(
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: ButtonStyle(foregroundBuilder: (_, _, child) => child!),
        ),
      );
      for (final variant in ['standard', 'icon']) {
        for (final explicit in [false, true]) {
          await _pump(
            tester,
            _model(
              variant: variant,
              properties: {if (explicit) 'clipBehavior': _null},
            ),
            theme: theme,
          );
          final material = tester.widget<Material>(
            find
                .descendant(
                  of: find.byType(OutlinedButton),
                  matching: find.byType(Material),
                )
                .first,
          );
          expect(material.clipBehavior, Clip.antiAlias);
        }
      }
    },
  );

  testWidgets(
    'typed callback activation distinguishes no callback long press only and disabled',
    (tester) async {
      for (final item in [
        (<String, Object?>{}, true, false),
        ({'onLongPress': _reference}, false, true),
        ({'onPressed': _reference}, true, false),
        ({'onPressed': _reference, 'onLongPress': _reference}, true, true),
        (
          {
            'enabled': _bool(false),
            'onPressed': _reference,
            'onLongPress': _reference,
          },
          false,
          false,
        ),
      ]) {
        await _pump(tester, _model(properties: item.$1));
        expect(_sdk(tester).onPressed != null, item.$2);
        expect(_sdk(tester).onLongPress != null, item.$3);
        expect(_sdk(tester).enabled, item.$2 || item.$3);
      }
    },
  );

  testWidgets(
    'zero-size SDK button stays selectable without invented model sizes',
    (tester) async {
      final selections = <String>[];
      final model = _model(
        empty: true,
        properties: {
          'styleMinimumWidth': _number(0),
          'styleMinimumHeight': _number(0),
          'styleMaximumWidth': _number(0),
          'styleMaximumHeight': _number(0),
          'styleTapTargetSize': _enum('MaterialTapTargetSize', 'shrinkWrap'),
          'stylePadding': {
            'kind': 'edgeInsets',
            'left': 0.0,
            'top': 0.0,
            'right': 0.0,
            'bottom': 0.0,
          },
        },
      );
      final before = jsonEncode(model);
      await _pump(tester, model, selected: selections);
      expect(tester.getSize(find.byType(OutlinedButton)), Size.zero);
      await tester.tapAt(tester.getCenter(_widget(_id)));
      await tester.pump();
      expect(selections, contains(_id));
      expect(jsonEncode(model), before);
      expect(tester.takeException(), isNull);
    },
  );
}
