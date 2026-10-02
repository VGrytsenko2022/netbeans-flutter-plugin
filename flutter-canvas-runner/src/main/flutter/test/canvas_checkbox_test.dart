import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Checkbox';
const _id = 'ca96bbfb-b385-4167-b7fc-3f3d8337bc49';
const _boundaryId = '658c0969-15b7-47e1-87b3-3e721cf203f1';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _states = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _model({
  String variant = 'standard',
  String platform = 'windows',
  Map<String, Object?> properties = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = _node(
    '8798daa7-a409-45d6-9ee5-f8bc0686ed26',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'ef4c8288-b304-4e4c-89e2-d1ff8d31dcaa',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(
                  _node(_id, _type, {
                    'value': _bool(false),
                    'enabled': _bool(true),
                    'variant': _string(variant),
                    ...properties,
                  }),
                ),
              }),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

Map _control(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child']['slots']['child']['child']
        as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Checkbox _sdk(WidgetTester tester) =>
    tester.widget<Checkbox>(find.byType(Checkbox));
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  String? selectedId,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        selectedWidgetId: selectedId,
        onSelected: selected?.add ?? (_) {},
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    return Uint8List.fromList(data!.buffer.asUint8List());
  } finally {
    image.dispose();
  }
}))!;

Checkbox _raw({
  String variant = 'standard',
  bool? value = false,
  bool enabled = true,
  bool isError = false,
  Color? activeColor,
  Color? checkColor,
  WidgetStateProperty<Color?>? fillColor,
  WidgetStateProperty<Color?>? overlayColor,
  BorderSide? side,
  OutlinedBorder? shape,
  VisualDensity? density,
  MaterialTapTargetSize? tapTarget,
  String? label,
}) {
  final create = variant == 'adaptive' ? Checkbox.adaptive : Checkbox.new;
  return create(
    value: value,
    tristate: true,
    onChanged: enabled ? (_) {} : null,
    isError: isError,
    activeColor: activeColor,
    checkColor: checkColor,
    fillColor: fillColor,
    overlayColor: overlayColor,
    side: side,
    shape: shape,
    visualDensity: density,
    materialTapTargetSize: tapTarget,
    semanticLabel: label,
  );
}

Future<void> _expectRawPixels(
  WidgetTester tester,
  Checkbox raw,
  ThemeData theme,
) async {
  tester
          .renderObject<RenderCustomPaint>(
            find.byKey(const ValueKey('canvas-widget-outline-$_id')),
          )
          .foregroundPainter =
      null;
  await tester.pump();
  final canvas = await _pixels(
    tester,
    tester.renderObject<RenderRepaintBoundary>(
      find
          .descendant(
            of: _widget(_boundaryId),
            matching: find.byType(RepaintBoundary),
          )
          .first,
    ),
  );
  final canvasSize = tester.getSize(find.byType(Checkbox));
  final key = GlobalKey();
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      home: Scaffold(
        body: Center(
          child: RepaintBoundary(key: key, child: raw),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  expect(tester.getSize(find.byType(Checkbox)), canvasSize);
  expect(
    canvas,
    orderedEquals(
      await _pixels(
        tester,
        key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
      ),
    ),
  );
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets(
    'Raw and Canvas Checkbox retain extreme finite side geometry without narrowing the model',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final width in [0.0, 1.7976931348623157e308]) {
          for (final align in [
            -1.7976931348623157e308,
            1.7976931348623157e308,
          ]) {
            final theme = ThemeData(useMaterial3: material3);
            final raw = WidgetStateBorderSide.fromMap({
              WidgetState.any: BorderSide(width: width, strokeAlign: align),
            });
            await tester.pumpWidget(
              MaterialApp(
                theme: theme,
                home: Scaffold(
                  body: Center(child: _raw(value: true, side: raw)),
                ),
              ),
            );
            expect(tester.takeException(), isNull);
            await _pump(
              tester,
              _model(
                properties: {
                  'value': _bool(true),
                  'sideStateful': _bool(true),
                  'sideWidth': _number(width),
                  'sideStrokeAlign': _number(align),
                },
              ),
              theme: theme,
            );
            expect(
              (_sdk(tester).side as WidgetStateBorderSide).resolve({})!.width,
              width,
            );
            expect(tester.takeException(), isNull);
          }
        }
      }
    },
  );
  testWidgets(
    'Checkbox missing-direction guard follows effective local or theme shape and retains actual State',
    (tester) async {
      State<Checkbox>? retained;
      for (final material3 in [false, true]) {
        for (final apple in [false, true]) {
          for (final linear in [false, true]) {
            for (final inherited in [false, true]) {
              final rawShape = linear
                  ? const LinearBorder()
                  : const RoundedRectangleBorder(
                      borderRadius: BorderRadiusDirectional.only(
                        topStart: Radius.circular(3),
                      ),
                    );
              final theme = ThemeData(
                useMaterial3: material3,
                platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
                checkboxTheme: CheckboxThemeData(
                  shape: inherited ? rawShape : null,
                ),
              );
              final props = <String, Object?>{'value': _bool(true)};
              if (!inherited) {
                props['shapeKind'] = _string(
                  linear ? 'linear' : 'roundedRectangle',
                );
                if (!linear) {
                  props['shapeRadius'] = {
                    'kind': 'borderRadius',
                    'geometry': {
                      'kind': 'directional',
                      'topStart': {'x': 3, 'y': 3},
                      'topEnd': {'x': 0, 'y': 0},
                      'bottomEnd': {'x': 0, 'y': 0},
                      'bottomStart': {'x': 0, 'y': 0},
                    },
                  };
                }
              }
              final data = _model(variant: 'adaptive', properties: props);
              final bytes = jsonEncode(data);
              await _pump(tester, data, theme: theme, selectedId: _id);
              retained ??= tester.state<State<Checkbox>>(find.byType(Checkbox));
              expect(
                tester.state<State<Checkbox>>(find.byType(Checkbox)),
                same(retained),
              );
              final expectedGuard = !(inherited && apple);
              expect(
                _diagnostics(tester).contains('TextDirection'),
                expectedGuard,
              );
              expect(
                _diagnostics(tester).contains('default-shape approximation'),
                expectedGuard,
              );
              if (expectedGuard) {
                final shape = _sdk(tester).shape! as RoundedRectangleBorder;
                expect(
                  shape.borderRadius,
                  BorderRadius.circular(
                    apple
                        ? 4
                        : material3
                        ? 2
                        : 1,
                  ),
                );
              } else {
                expect(_sdk(tester).shape, isNull);
              }
              expect(_sdk(tester).value, isTrue);
              expect(jsonEncode(data), bytes);
              expect(tester.takeException(), isNull);
            }
          }
        }
      }
      await _pump(
        tester,
        _model(
          properties: {'shapeKind': _string('circle'), 'value': _bool(true)},
        ),
        selectedId: _id,
      );
      expect(
        tester.state<State<Checkbox>>(find.byType(Checkbox)),
        same(retained),
      );
      expect(_sdk(tester).shape, isA<CircleBorder>());
      expect(_diagnostics(tester), isNot(contains('TextDirection')));
    },
  );
  for (final apple in [false, true]) {
    for (final linear in [false, true]) {
      testWidgets(
        'Raw Checkbox apple=$apple ${linear ? "LinearBorder" : "directional corners"} fails because SDK painter omits TextDirection',
        (tester) async {
          final shape = linear
              ? const LinearBorder()
              : const RoundedRectangleBorder(
                  borderRadius: BorderRadiusDirectional.only(
                    topStart: Radius.circular(3),
                  ),
                );
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(
                platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
              ),
              home: Scaffold(
                body: Center(
                  child: _raw(variant: 'adaptive', value: true, shape: shape),
                ),
              ),
            ),
          );
          expect(tester.takeException().toString(), contains('TextDirection'));
          await tester.pumpWidget(const SizedBox.shrink());
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  for (final material3 in [false, true]) {
    testWidgets(
      '${material3 ? "M3" : "M2"} Checkbox theme precedence explicit null state colors density and shape match SDK',
      (tester) async {
        final colors = ColorScheme.fromSeed(
          seedColor: Colors.orange,
          brightness: Brightness.dark,
        );
        final theme = ThemeData(
          useMaterial3: material3,
          colorScheme: colors,
          visualDensity: const VisualDensity(horizontal: 3, vertical: -2),
          checkboxTheme: CheckboxThemeData(
            fillColor: WidgetStateProperty.resolveWith(
              (states) => states.contains(WidgetState.selected)
                  ? Colors.purple
                  : Colors.yellow,
            ),
            checkColor: const WidgetStatePropertyAll(Colors.lime),
            visualDensity: const VisualDensity(horizontal: -2, vertical: 3),
            shape: const BeveledRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(3)),
            ),
          ),
        );
        for (final selected in [false, true, null]) {
          for (final enabled in [false, true]) {
            for (final local in [false, true]) {
              final props = <String, Object?>{
                'value': selected == null ? {'kind': 'null'} : _bool(selected),
                'tristate': _bool(true),
                'enabled': _bool(enabled),
                'activeColor': _color(0xff336699),
                'visualDensityHorizontal': _number(1.0),
                'materialTapTargetSize': _enum(
                  'MaterialTapTargetSize',
                  'shrinkWrap',
                ),
                if (local)
                  'fillColorDefault': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                if (local) 'fillColorDisabled': {'kind': 'null'},
                if (local) 'fillColorSelected': {'kind': 'null'},
              };
              await _pump(tester, _model(properties: props), theme: theme);
              expect(
                _sdk(tester).visualDensity,
                const VisualDensity(horizontal: 1),
              );
              await _expectRawPixels(
                tester,
                _raw(
                  value: selected,
                  enabled: enabled,
                  activeColor: const Color(0xff336699),
                  density: const VisualDensity(horizontal: 1),
                  tapTarget: MaterialTapTargetSize.shrinkWrap,
                  fillColor: local
                      ? WidgetStateProperty<Color?>.fromMap({
                          WidgetState.disabled: null,
                          WidgetState.selected: null,
                          WidgetState.any: colors.primary,
                        })
                      : null,
                ),
                theme,
              );
            }
          }
        }
      },
    );
  }

  testWidgets(
    'Checkbox nine direction-independent shape families match SDK and directional corners are diagnosed',
    (tester) async {
      final shapes = <String, OutlinedBorder>{
        'roundedRectangle': const RoundedRectangleBorder(side: BorderSide.none),
        'beveledRectangle': const BeveledRectangleBorder(side: BorderSide.none),
        'continuousRectangle': const ContinuousRectangleBorder(
          side: BorderSide.none,
        ),
        'roundedSuperellipse': const RoundedSuperellipseBorder(
          side: BorderSide.none,
        ),
        'circle': const CircleBorder(side: BorderSide.none),
        'oval': const OvalBorder(side: BorderSide.none),
        'stadium': const StadiumBorder(side: BorderSide.none),
        'star': const StarBorder(side: BorderSide.none),
        'polygon': const StarBorder.polygon(side: BorderSide.none),
      };
      for (final apple in [false, true]) {
        final theme = ThemeData(
          platform: apple ? TargetPlatform.macOS : TargetPlatform.windows,
        );
        for (final shape in shapes.entries) {
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {
                'shapeKind': _string(shape.key),
                'value': _bool(true),
              },
            ),
            theme: theme,
          );
          expect(_sdk(tester).shape, shape.value);
          await _expectRawPixels(
            tester,
            _raw(variant: 'adaptive', value: true, shape: shape.value),
            theme,
          );
        }
      }
      for (final directional in [false, true]) {
        final radius = <String, Object?>{
          'kind': 'borderRadius',
          'geometry': {
            'kind': directional ? 'directional' : 'physical',
            directional ? 'topStart' : 'topLeft': {'x': 1, 'y': 2},
            directional ? 'topEnd' : 'topRight': {'x': 3, 'y': 4},
            directional ? 'bottomEnd' : 'bottomRight': {'x': 5, 'y': 6},
            directional ? 'bottomStart' : 'bottomLeft': {'x': 7, 'y': 8},
          },
        };
        await _pump(
          tester,
          _model(
            properties: {
              'shapeKind': _string('roundedRectangle'),
              'shapeRadius': radius,
            },
          ),
        );
        final shape = _sdk(tester).shape! as RoundedRectangleBorder;
        expect(
          shape.borderRadius.resolve(TextDirection.rtl).topLeft,
          directional
              ? const Radius.circular(2)
              : const Radius.elliptical(1, 2),
        );
        expect(
          _diagnostics(tester).contains('default-shape approximation'),
          directional,
        );
      }
    },
  );

  testWidgets(
    'Checkbox ignores only Apple-inactive project fields and diagnoses every active isolated reference',
    (tester) async {
      final onlyIgnored = _model(
        variant: 'adaptive',
        properties: {'fillColor': _reference, 'overlayColor': _reference},
      );
      await _pump(
        tester,
        onlyIgnored,
        theme: ThemeData(platform: TargetPlatform.iOS),
      );
      expect(_diagnostics(tester), isNot(contains('preview limitation')));
      expect(find.byType(CupertinoCheckbox), findsOneWidget);
      await _pump(
        tester,
        onlyIgnored,
        theme: ThemeData(platform: TargetPlatform.windows),
      );
      expect(_diagnostics(tester), contains('project appearance'));
      for (final enabled in [false, true]) {
        await _pump(
          tester,
          _model(
            variant: 'adaptive',
            properties: {
              'enabled': _bool(enabled),
              'shape': _reference,
              'side': _reference,
              'focusNode': _reference,
              'mouseCursor': _reference,
              'onChanged': _reference,
            },
          ),
          theme: ThemeData(platform: TargetPlatform.iOS),
        );
        final sdk = _sdk(tester);
        expect(sdk.shape, isNull);
        expect(sdk.side, isNull);
        expect(sdk.focusNode, isNull);
        expect(sdk.mouseCursor, isNull);
        expect(sdk.onChanged != null, enabled);
        expect(find.byType(CupertinoCheckbox), findsOneWidget);
        for (final phrase in [
          'project appearance',
          'focus ownership',
          'Project cursor',
          if (enabled) 'Project onChanged',
        ]) {
          expect(_diagnostics(tester), contains(phrase));
        }
        expect(_diagnostics(tester).contains('Project onChanged'), enabled);
      }
      final data = _model(
        properties: {
          'shapeKind': _string('star'),
          'shapePoints': _number(5000),
        },
      );
      await _pump(tester, data);
      expect(find.byType(Checkbox), findsOneWidget);
      expect(_sdk(tester).shape, isNull);
      expect(
        _diagnostics(tester),
        contains('exceeds the isolated Canvas budget'),
      );
      expect(_control(data)['properties']['shapePoints'], _number(5000));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'Checkbox controlled animation survives TickerMode and zero-size layout without a stored preview state',
    (tester) async {
      State<Checkbox>? state;
      for (final enabled in [true, false, true]) {
        final data = _model(
          properties: {'value': _bool(!enabled), 'autofocus': _bool(true)},
        );
        final boundary =
            (data['root']
                as Map)['slots']['body']['child']['slots']['child']['child'];
        (data['root']
            as Map)['slots']['body']['child']['slots']['child'] = _single(
          _node(
            'bdb63d89-72da-4bce-998c-f0a69ade0ebf',
            'flutter.widgets.TickerMode',
            {'enabled': _bool(enabled)},
            {'child': _single(boundary)},
          ),
        );
        await _pump(tester, data);
        state ??= tester.state<State<Checkbox>>(find.byType(Checkbox));
        expect(
          tester.state<State<Checkbox>>(find.byType(Checkbox)),
          same(state),
        );
        expect(
          TickerMode.valuesOf(tester.element(find.byType(Checkbox))).enabled,
          enabled,
        );
        expect(_sdk(tester).value, !enabled);
        expect(tester.takeException(), isNull);
      }
      final zero = _model();
      final boundary =
          (zero['root']
              as Map)['slots']['body']['child']['slots']['child']['child'];
      (zero['root']
          as Map)['slots']['body']['child']['slots']['child'] = _single(
        _node(
          'bdb63d89-72da-4bce-998c-f0a69ade0ebf',
          'flutter.widgets.SizedBox',
          {'width': _number(0), 'height': _number(0)},
          {'child': _single(boundary)},
        ),
      );
      await _pump(tester, zero, selectedId: _id);
      expect(_widget(_id), findsOneWidget);
      expect(tester.getSize(find.byType(Checkbox)), Size.zero);
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'Checkbox complete closed model has106 fields with exact required values and relations',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final section = contract.substring(
        start,
        contract.indexOf('\nW|', start + 1),
      );
      expect(
        section.split('\n').where((line) => line.startsWith('P|')),
        hasLength(106),
      );
      expect(section, contains('P|value|boolean,null|1|boolean:false'));
      expect(section, isNot(contains('S|')));
      final reviewedStart = canvasReviewedWidgetSchemaContract.indexOf(
        'W|$_type\n',
      );
      final reviewed = canvasReviewedWidgetSchemaContract
          .substring(
            reviewedStart,
            canvasReviewedWidgetSchemaContract.indexOf(
              '\nW|',
              reviewedStart + 1,
            ),
          )
          .split('\n');
      final runtime = section.split('\n');
      expect(runtime.length, reviewed.length);
      for (var index = 0; index < runtime.length; index++) {
        expect(
          runtime[index],
          reviewed[index],
          reason: 'Java-authored Checkbox record $index',
        );
      }
      for (final variant in ['standard', 'adaptive']) {
        for (final value in [false, true, null]) {
          expect(
            () => _decode(
              _model(
                variant: variant,
                properties: {
                  'value': value == null ? {'kind': 'null'} : _bool(value),
                  'tristate': _bool(true),
                },
              ),
            ),
            returnsNormally,
          );
        }
      }
      for (final properties in <Map<String, Object?>>[
        {
          'value': {'kind': 'null'},
        },
        {
          'value': {'kind': 'null'},
          'tristate': _bool(false),
        },
        {'variant': _string('unsupported')},
        {
          'fillColor': _reference,
          'fillColorDefault': {'kind': 'null'},
        },
        {'overlayColor': _reference, 'overlayColorFocused': _color(0xff123456)},
        {'side': _reference, 'sideStateful': _bool(false)},
        {'sidePressedWidth': _number(2)},
        {'sideStateful': _bool(false), 'sidePressedMode': _string('border')},
        {
          'sideStateful': _bool(true),
          'sidePressedMode': _string('inherit'),
          'sidePressedColor': _color(0xff123456),
        },
        {'shape': _reference, 'shapeKind': _string('circle')},
        {
          'shapeRadius': {'kind': 'null'},
        },
        {'splashRadius': _enum('double', 'nan')},
        {'visualDensityHorizontal': _number(1)},
        {'visualDensityVertical': _number(4.1)},
        {'sideWidth': _number(-1)},
        {'unknown': _bool(true)},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: '$properties',
        );
      }
      for (final name in ['value', 'enabled', 'variant']) {
        final data = _model();
        (_control(data)['properties'] as Map).remove(name);
        expect(
          () => _decode(data),
          throwsFormatException,
          reason: 'Required $name',
        );
      }
      for (final radius in [
        _number(-8),
        _number(0),
        _number(1.5),
        _enum('double', 'infinity'),
      ]) {
        expect(
          () => _decode(_model(properties: {'splashRadius': radius})),
          returnsNormally,
        );
      }
    },
  );

  testWidgets(
    'Checkbox color maps preserve all256 state combinations and explicit null presence',
    (tester) async {
      for (final family in ['fillColor', 'overlayColor']) {
        final props = <String, Object?>{'${family}Default': _color(0xff223344)};
        var index = 0;
        for (final state in _states.values) {
          props['$family$state'] = _color(0xff000001 + index++);
        }
        await _pump(tester, _model(properties: props));
        final property = family == 'fillColor'
            ? _sdk(tester).fillColor!
            : _sdk(tester).overlayColor!;
        for (var bits = 0; bits < 256; bits++) {
          final states = <WidgetState>{
            for (var i = 0; i < 8; i++)
              if ((bits & (1 << i)) != 0) _states.keys.elementAt(i),
          };
          final first = bits == 0
              ? -1
              : List.generate(
                  8,
                  (i) => i,
                ).firstWhere((i) => (bits & (1 << i)) != 0);
          expect(
            property.resolve(states),
            Color(first < 0 ? 0xff223344 : 0xff000001 + first),
          );
        }
        await _pump(
          tester,
          _model(properties: {'${family}Default': _color(0xff223344)}),
        );
        final base = family == 'fillColor'
            ? _sdk(tester).fillColor!
            : _sdk(tester).overlayColor!;
        expect(base.resolve({WidgetState.disabled}), const Color(0xff223344));
        props['${family}Disabled'] = {'kind': 'null'};
        await _pump(tester, _model(properties: props));
        final nullable = family == 'fillColor'
            ? _sdk(tester).fillColor!
            : _sdk(tester).overlayColor!;
        expect(
          nullable.resolve({WidgetState.disabled, WidgetState.selected}),
          isNull,
        );
      }
    },
  );

  testWidgets(
    'Checkbox side state maps distinguish plain, border defaults and explicit inherit',
    (tester) async {
      await _pump(
        tester,
        _model(properties: {'sideColor': _color(0xff123456)}),
      );
      expect(_sdk(tester).side, isNot(isA<WidgetStateBorderSide>()));
      expect(_sdk(tester).side, const BorderSide(color: Color(0xff123456)));
      final props = <String, Object?>{
        'sideStateful': _bool(true),
        'sideWidth': _number(5),
        'sideColor': _color(0xff123456),
      };
      for (final state in _states.values) {
        props['side${state}Mode'] = _string('border');
        props['side${state}Width'] = _number(
          _states.values.toList().indexOf(state) + 2,
        );
      }
      await _pump(tester, _model(properties: props));
      final side = _sdk(tester).side! as WidgetStateBorderSide;
      for (var bits = 0; bits < 256; bits++) {
        final states = <WidgetState>{
          for (var i = 0; i < 8; i++)
            if ((bits & (1 << i)) != 0) _states.keys.elementAt(i),
        };
        final first = bits == 0
            ? -1
            : List.generate(
                8,
                (i) => i,
              ).firstWhere((i) => (bits & (1 << i)) != 0);
        final resolved = side.resolve(states)!;
        expect(resolved.width, first < 0 ? 5 : first + 2);
        expect(
          resolved.color,
          first < 0 ? const Color(0xff123456) : const Color(0xff000000),
        );
        expect(resolved.strokeAlign, -1);
      }
      props.remove('sideDisabledWidth');
      props['sideDisabledMode'] = _string('inherit');
      await _pump(tester, _model(properties: props));
      expect(
        (_sdk(tester).side as WidgetStateBorderSide).resolve({
          WidgetState.disabled,
          WidgetState.selected,
        }),
        isNull,
      );
      await _pump(tester, _model(properties: {'sideStateful': _bool(true)}));
      expect((_sdk(tester).side as WidgetStateBorderSide).resolve({}), isNull);
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      '$platform Checkbox state survives values variants diagnostics theme and model history',
      (tester) async {
        State<Checkbox>? retained;
        for (final variant in [
          'standard',
          'adaptive',
          'standard',
          'adaptive',
        ]) {
          for (final value in [false, true, null, false]) {
            final data = _model(
              platform: platform,
              variant: variant,
              properties: {
                'value': value == null ? {'kind': 'null'} : _bool(value),
                'tristate': _bool(true),
                if (value == true) 'focusNode': _reference,
                if (value == null) 'fillColor': _reference,
                'onChanged': _reference,
              },
            );
            await _pump(
              tester,
              data,
              theme: ThemeData(
                platform: variant == 'adaptive'
                    ? TargetPlatform.iOS
                    : TargetPlatform.windows,
              ),
              selectedId: _id,
            );
            retained ??= tester.state<State<Checkbox>>(find.byType(Checkbox));
            expect(
              tester.state<State<Checkbox>>(find.byType(Checkbox)),
              same(retained),
            );
            expect(_sdk(tester).value, value);
            _sdk(tester).onChanged!(value == false ? true : false);
            await tester.pump();
            expect(_sdk(tester).value, value);
            expect(_diagnostics(tester), contains('controlled stored value'));
            expect(tester.takeException(), isNull);
          }
        }
      },
    );
  }
  for (final material3 in [false, true]) {
    testWidgets(
      '${material3 ? "M3" : "M2"} Checkbox standard/adaptive has exact SDK pixels across platforms values enabled error and RTL',
      (tester) async {
        for (final variant in ['standard', 'adaptive']) {
          for (final apple in [false, true]) {
            for (final value in [false, true, null]) {
              for (final enabled in [false, true]) {
                for (final error in [false, true]) {
                  for (final rtl in [false, true]) {
                    final theme = ThemeData(
                      useMaterial3: material3,
                      platform: apple
                          ? TargetPlatform.iOS
                          : TargetPlatform.windows,
                      colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
                    );
                    final data = _model(
                      variant: variant,
                      properties: {
                        'value': value == null
                            ? {'kind': 'null'}
                            : _bool(value),
                        'tristate': _bool(true),
                        'enabled': _bool(enabled),
                        'isError': _bool(error),
                      },
                    );
                    if (rtl) {
                      final body =
                          (data['root'] as Map)['slots']['body']['child'];
                      (data['root'] as Map)['slots']['body'] = _single(
                        _node(
                          'ea88d8e0-98cf-4e2b-9c19-316ef6239361',
                          'flutter.widgets.Directionality',
                          {'textDirection': _enum('TextDirection', 'rtl')},
                          {'child': _single(body)},
                        ),
                      );
                    }
                    await _pump(tester, data, theme: theme);
                    tester
                            .renderObject<RenderCustomPaint>(
                              find.byKey(
                                const ValueKey('canvas-widget-outline-$_id'),
                              ),
                            )
                            .foregroundPainter =
                        null;
                    await tester.pump();
                    final canvas = await _pixels(
                      tester,
                      tester.renderObject<RenderRepaintBoundary>(
                        find
                            .descendant(
                              of: _widget(_boundaryId),
                              matching: find.byType(RepaintBoundary),
                            )
                            .first,
                      ),
                    );
                    final key = GlobalKey();
                    await tester.pumpWidget(
                      MaterialApp(
                        theme: theme,
                        home: Directionality(
                          textDirection: rtl
                              ? TextDirection.rtl
                              : TextDirection.ltr,
                          child: Scaffold(
                            body: Center(
                              child: RepaintBoundary(
                                key: key,
                                child: _raw(
                                  variant: variant,
                                  value: value,
                                  enabled: enabled,
                                  isError: error,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    );
                    await tester.pump(const Duration(milliseconds: 300));
                    final raw = await _pixels(
                      tester,
                      key.currentContext!.findRenderObject()!
                          as RenderRepaintBoundary,
                    );
                    expect(
                      canvas,
                      orderedEquals(raw),
                      reason:
                          '$variant apple=$apple value=$value enabled=$enabled error=$error rtl=$rtl',
                    );
                    expect(tester.takeException(), isNull);
                  }
                }
              }
            }
          }
        }
      },
    );

    testWidgets(
      '${material3 ? "M3" : "M2"} Checkbox plain and stateful side rendering follows raw SDK selected-state rules',
      (tester) async {
        for (final apple in [false, true]) {
          for (final value in [false, true, null]) {
            for (final stateful in [false, true]) {
              final theme = ThemeData(
                useMaterial3: material3,
                platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
              );
              final props = <String, Object?>{
                'value': value == null ? {'kind': 'null'} : _bool(value),
                'tristate': _bool(true),
                'sideWidth': _number(3),
                'sideColor': _color(0xffff0000),
                'sideStateful': _bool(stateful),
                'shapeKind': _string('roundedRectangle'),
              };
              await _pump(
                tester,
                _model(variant: 'adaptive', properties: props),
                theme: theme,
              );
              tester
                      .renderObject<RenderCustomPaint>(
                        find.byKey(
                          const ValueKey('canvas-widget-outline-$_id'),
                        ),
                      )
                      .foregroundPainter =
                  null;
              await tester.pump();
              final canvas = await _pixels(
                tester,
                tester.renderObject<RenderRepaintBoundary>(
                  find
                      .descendant(
                        of: _widget(_boundaryId),
                        matching: find.byType(RepaintBoundary),
                      )
                      .first,
                ),
              );
              const plain = BorderSide(color: Color(0xffff0000), width: 3);
              final key = GlobalKey();
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  home: Scaffold(
                    body: Center(
                      child: RepaintBoundary(
                        key: key,
                        child: _raw(
                          variant: 'adaptive',
                          value: value,
                          shape: const RoundedRectangleBorder(
                            side: BorderSide.none,
                          ),
                          side: stateful
                              ? WidgetStateBorderSide.fromMap({
                                  WidgetState.any: plain,
                                })
                              : plain,
                        ),
                      ),
                    ),
                  ),
                ),
              );
              await tester.pump(const Duration(milliseconds: 300));
              expect(
                canvas,
                orderedEquals(
                  await _pixels(
                    tester,
                    key.currentContext!.findRenderObject()!
                        as RenderRepaintBoundary,
                  ),
                ),
                reason: 'apple=$apple value=$value stateful=$stateful',
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    for (final radius in [-5.0, 0.0, 30.0, double.infinity]) {
      testWidgets(
        'Raw SDK ${material3 ? "M3" : "M2"} Checkbox splash radius $radius stays safe through hover focus and press',
        (tester) async {
          final focus = FocusNode();
          addTearDown(focus.dispose);
          var callbacks = 0;
          final boundaryKey = GlobalKey();
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(useMaterial3: material3),
              home: Scaffold(
                body: Center(
                  child: RepaintBoundary(
                    key: boundaryKey,
                    child: Checkbox(
                      value: false,
                      onChanged: (_) => callbacks++,
                      splashRadius: radius,
                      focusNode: focus,
                    ),
                  ),
                ),
              ),
            ),
          );
          final mouse = await tester.createGesture(
            kind: PointerDeviceKind.mouse,
          );
          await mouse.addPointer();
          await mouse.moveTo(tester.getCenter(find.byType(Checkbox)));
          await tester.pump(const Duration(milliseconds: 150));
          focus.requestFocus();
          await tester.pump(const Duration(milliseconds: 150));
          final gesture = await tester.startGesture(
            tester.getCenter(find.byType(Checkbox)),
          );
          await tester.pump(const Duration(milliseconds: 60));
          final boundary =
              boundaryKey.currentContext!.findRenderObject()!
                  as RenderRepaintBoundary;
          final image = await boundary.toImage();
          expect(image.width, greaterThan(0));
          image.dispose();
          await gesture.up();
          await tester.pump(const Duration(milliseconds: 150));
          expect(callbacks, 1);
          expect(tester.takeException(), isNull);
          await mouse.removePointer();
        },
      );
    }
  }

  for (final variant in ['standard', 'adaptive']) {
    for (final apple in [false, true]) {
      testWidgets(
        'Raw $variant apple=$apple controlled callback cycle and Canvas checked/mixed semantics',
        (tester) async {
          bool? value = false;
          final changes = <bool?>[];
          final theme = ThemeData(
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
          );
          await tester.pumpWidget(
            MaterialApp(
              theme: theme,
              home: Scaffold(
                body: Center(
                  child: StatefulBuilder(
                    builder: (context, setState) {
                      final create = variant == 'adaptive'
                          ? Checkbox.adaptive
                          : Checkbox.new;
                      return create(
                        value: value,
                        tristate: true,
                        onChanged: (next) {
                          changes.add(next);
                          setState(() => value = next);
                        },
                      );
                    },
                  ),
                ),
              ),
            ),
          );
          for (final next in [true, null, false]) {
            await tester.tap(find.byType(Checkbox));
            await tester.pump(const Duration(milliseconds: 300));
            expect(value, next);
          }
          expect(changes, [true, null, false]);
          final handle = tester.ensureSemantics();
          try {
            for (final stored in [false, true, null]) {
              for (final enabled in [false, true]) {
                await _pump(
                  tester,
                  _model(
                    variant: variant,
                    properties: {
                      'value': stored == null
                          ? {'kind': 'null'}
                          : _bool(stored),
                      'tristate': _bool(true),
                      'enabled': _bool(enabled),
                      'semanticLabel': _string('Reviewed checkbox'),
                    },
                  ),
                  theme: theme,
                );
                final semantics = tester
                    .getSemantics(find.byType(Checkbox))
                    .getSemanticsData();
                expect(semantics.label, contains('Reviewed checkbox'));
                expect(
                  semantics.flagsCollection.isChecked != ui.CheckedState.none,
                  isTrue,
                );
                expect(
                  semantics.flagsCollection.isChecked == ui.CheckedState.isTrue,
                  stored == true,
                );
                expect(
                  semantics.flagsCollection.isChecked == ui.CheckedState.mixed,
                  stored == null,
                );
                expect(
                  semantics.flagsCollection.isEnabled == ui.Tristate.isTrue,
                  enabled,
                );
                expect(_sdk(tester).onChanged != null, enabled);
                expect(tester.takeException(), isNull);
              }
            }
          } finally {
            handle.dispose();
          }
        },
      );
    }
  }

  testWidgets(
    'Raw adaptive Checkbox chooses Cupertino by Theme platform but tap target by host platform',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(platform: TargetPlatform.iOS),
          home: Scaffold(
            body: Center(
              child: Checkbox.adaptive(value: false, onChanged: (_) {}),
            ),
          ),
        ),
      );
      expect(find.byType(CupertinoCheckbox), findsOneWidget);
      expect(
        tester.getSize(find.byType(CupertinoCheckbox)),
        Size.square(defaultTargetPlatform == TargetPlatform.windows ? 14 : 44),
      );
      expect(tester.takeException(), isNull);
    },
    variant: TargetPlatformVariant({
      TargetPlatform.windows,
      TargetPlatform.iOS,
    }),
  );
}
