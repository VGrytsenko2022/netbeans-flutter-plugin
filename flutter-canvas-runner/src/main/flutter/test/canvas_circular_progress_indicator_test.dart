// ignore_for_file: deprecated_member_use

import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.CircularProgressIndicator';
const _id = '1934f733-85ab-40bb-8c7a-360cdd4fccfe';
const _boxId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _outerId = 'd5d112bb-47fc-4706-8f50-e9b4aa97df29';
const _infinity = {'kind': 'enum', 'type': 'double', 'value': 'infinity'};
const _null = {'kind': 'null'};
const _reference = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _constraints({double? min = 0, double? max}) => {
  'kind': 'boxConstraints',
  'minWidth': min,
  'maxWidth': max,
  'minHeight': min,
  'maxHeight': max,
};
Map<String, Object?> _padding(double value, {bool directional = false}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': value,
  'top': value,
  directional ? 'end' : 'right': value,
  'bottom': value,
};
Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  double? width,
  double? height,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  final leaf = {
    'id': _id,
    'type': _type,
    'properties': <String, Object?>{
      'variant': _string('material'),
      ...properties,
    },
    'slots': <String, Object?>{},
  };
  final box = {
    'id': _boxId,
    'type': 'flutter.widgets.SizedBox',
    'properties': {
      if (width != null) 'width': _double(width),
      if (height != null) 'height': _double(height),
    },
    'slots': {
      'child': <String, Object?>{'kind': 'single', 'child': leaf},
    },
  };
  final center = {
    'id': '29154d08-5472-4eae-8d4d-9352da0bbd52',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': {
      'child': {'kind': 'single', 'child': box},
    },
  };
  model['root'] = {
    'id': 'e5e23da0-f3e6-46fa-88fb-0ab84d966dd3',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {'kind': 'single', 'child': center},
    },
  };
  return model;
}

Map _box(Map<String, Object?> model) {
  final body = ((model['root'] as Map)['slots'] as Map)['body'] as Map;
  final center = body['child'] as Map;
  final child = (center['slots'] as Map)['child'] as Map;
  return child['child'] as Map;
}

Map _leaf(Map<String, Object?> model) {
  Map node = _box(model);
  while (node['type'] != _type) {
    final slots = node['slots'] as Map;
    final slot = slots.values.first as Map;
    node = slot['kind'] == 'list'
        ? (slot['children'] as List).single as Map
        : slot['child'] as Map;
  }
  return node;
}

void _wrap(
  Map<String, Object?> model,
  String type,
  Map<String, Object?> properties, {
  bool list = false,
}) {
  final slot = ((_box(model)['slots'] as Map)['child'] as Map);
  slot['child'] = {
    'id': _outerId,
    'type': type,
    'properties': properties,
    'slots': {
      list ? 'children' : 'child': list
          ? {
              'kind': 'list',
              'children': [slot['child']],
            }
          : {'kind': 'single', 'child': slot['child']},
    },
  };
}

CanvasNode _node(CanvasModel model) {
  CanvasNode node = model.root;
  while (node.type != _type) {
    node = node.slots.values.first.children.single;
  }
  return node;
}

Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _finder() => find.descendant(
  of: _widget(_id),
  matching: find.byType(CircularProgressIndicator),
);
dynamic _painter(WidgetTester tester) => tester
    .widget<CustomPaint>(
      find.descendant(of: _finder(), matching: find.byType(CustomPaint)).first,
    )
    .painter!;
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  void Function(CanvasDropResolver?)? onDrop,
  void Function(CanvasMovePreviewResolver?)? onMove,
  TextDirection direction = TextDirection.ltr,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: (theme ?? ThemeData()).copyWith(
        platform: switch ((model['profile'] as Map)['targetPlatform']) {
          'ios' => TargetPlatform.iOS,
          'macos' => TargetPlatform.macOS,
          'android' => TargetPlatform.android,
          'linux' => TargetPlatform.linux,
          _ => TargetPlatform.windows,
        },
      ),
      themeAnimationDuration: Duration.zero,
      home: Directionality(
        textDirection: direction,
        child: CanvasDocumentView(
          model: _decode(model),
          selectedWidgetId: null,
          onSelected: selected?.add ?? (_) {},
          onDropResolverChanged: onDrop,
          onMovePreviewResolverChanged: onMove,
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, _) =>
              throw StateError('Progress cannot edit text'),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 100));
  await tester.pump();
}

Future<Uint8List> _painted(WidgetTester tester, CustomPainter painter) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      painter.paint(Canvas(recorder), const Size(52, 54));
      final picture = recorder.endRecording();
      final image = await picture.toImage(52, 54);
      try {
        return (await image.toByteData(
          format: ui.ImageByteFormat.rawRgba,
        ))!.buffer.asUint8List();
      } finally {
        image.dispose();
        picture.dispose();
      }
    }))!;

String _labels(WidgetTester tester) {
  final labels = <String>[];
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) labels.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return labels.join('\n');
}

void main() {
  test(
    'CircularProgressIndicator exact fifteen closed fields and constructor contracts',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf(
        'W|flutter.material.CircularProgressIndicator\n',
      );
      final block = contract.substring(
        start,
        contract.indexOf('W|', start + 3),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(15));
      expect(
        block,
        contains(
          'P|variant|string|1|string:bWF0ZXJpYWw|-|string:pattern:KD86bWF0ZXJpYWx8YWRhcHRpdmUp',
        ),
      );
      expect(block, isNot(contains('S|')));
      final model = _model();
      expect(_node(_decode(model)).properties.keys, ['variant']);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(canvasModelProtocolVersion, 19);
      for (final name in ['value', 'strokeWidth', 'strokeAlign']) {
        for (final value in [
          _int(-9007199254740991),
          _int(9007199254740991),
          _double(-1e308),
          _double(1e308),
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
        expect(
          () => _decode(_model(properties: {name: _infinity})),
          throwsFormatException,
        );
      }
      expect(
        () => _decode(_model(properties: {'trackGap': _infinity})),
        returnsNormally,
      );
      expect(
        () => _decode(
          _model(properties: {'value': _double(.5), 'controller': _reference}),
        ),
        throwsFormatException,
      );
      expect(
        () => _decode(
          _model(
            properties: {
              'variant': _string('adaptive'),
              'color': _color('0xFFFF0000'),
            },
          ),
        ),
        throwsFormatException,
      );
      for (final bad in ['Adaptive', 'bogus', 'material()']) {
        expect(
          () => _decode(_model(properties: {'variant': _string(bad)})),
          throwsFormatException,
        );
      }
      (_leaf(model)['properties'] as Map).remove('variant');
      expect(() => _decode(model), throwsFormatException);
      for (final bad in [
        {'padding': _padding(-1)},
        {'constraints': _constraints(min: -1)},
        {'strokeCap': _enum('StrokeCap', 'fake')},
        {'strokeWidth': _null},
        {
          'valueColor': {
            'kind': 'dartObjectReferencePresence',
            'expression': 'evil()',
          },
        },
      ]) {
        expect(() => _decode(_model(properties: bad)), throwsFormatException);
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'CircularProgressIndicator forwards all material fields with actual M2 M3 year and theme defaults on $platform',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final year in <bool?>[null, true, false]) {
            for (final local in [false, true]) {
              for (final themed in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: material3,
                  progressIndicatorTheme: themed
                      ? const ProgressIndicatorThemeData(
                          color: Colors.orange,
                          circularTrackColor: Colors.purple,
                          strokeWidth: 7,
                          strokeAlign: 2,
                          strokeCap: StrokeCap.square,
                          constraints: BoxConstraints(
                            minWidth: 44,
                            minHeight: 45,
                          ),
                          trackGap: 6,
                          year2023: false,
                          circularTrackPadding: EdgeInsets.all(3),
                        )
                      : const ProgressIndicatorThemeData(),
                );
                final properties = <String, Object?>{
                  'value': _double(.3),
                  if (year != null) 'year2023': _bool(year),
                  if (local) ...{
                    'color': _color('0xFFFF0000'),
                    'backgroundColor': _color('0xFF0000FF'),
                    'strokeWidth': _int(9),
                    'strokeAlign': _double(-2),
                    'strokeCap': _enum('StrokeCap', 'round'),
                    'constraints': _constraints(min: 50),
                    'trackGap': _int(8),
                    'padding': _padding(2),
                    'semanticsLabel': _string('Download'),
                    'semanticsValue': _string('30%'),
                  },
                };
                await _pump(
                  tester,
                  _model(platform: platform, properties: properties),
                  theme: theme,
                );
                final widget = tester.widget<CircularProgressIndicator>(
                  _finder(),
                );
                final painter = _painter(tester);
                final effective = Theme.of(tester.element(_finder()));
                final actualYear = year ?? (themed ? false : true);
                expect(painter.year2023, actualYear);
                expect(
                  painter.strokeWidth,
                  local
                      ? 9
                      : themed
                      ? 7
                      : 4,
                );
                expect(
                  painter.strokeAlign,
                  local
                      ? -2
                      : themed
                      ? 2
                      : material3 && !actualYear
                      ? -1
                      : 0,
                );
                expect(
                  painter.strokeCap,
                  local
                      ? StrokeCap.round
                      : themed
                      ? StrokeCap.square
                      : null,
                );
                expect(
                  painter.valueColor,
                  local
                      ? const Color(0xFFFF0000)
                      : themed
                      ? Colors.orange
                      : effective.colorScheme.primary,
                );
                expect(
                  painter.trackColor,
                  local
                      ? const Color(0xFF0000FF)
                      : themed
                      ? Colors.purple
                      : material3 && !actualYear
                      ? effective.colorScheme.secondaryContainer
                      : null,
                );
                expect(
                  painter.trackGap,
                  actualYear
                      ? null
                      : local
                      ? 8
                      : themed
                      ? 6
                      : material3
                      ? 4
                      : null,
                );
                expect(widget.padding, local ? const EdgeInsets.all(2) : null);
                expect(
                  widget.constraints,
                  local
                      ? const BoxConstraints(minWidth: 50, minHeight: 50)
                      : null,
                );
                expect(widget.semanticsLabel, local ? 'Download' : null);
                expect(widget.semanticsValue, local ? '30%' : null);
                final dimension = local
                    ? 54.0
                    : themed
                    ? 50.0
                    : material3 && !actualYear
                    ? 48.0
                    : 36.0;
                expect(
                  tester.getSize(_finder()),
                  Size(dimension, themed && !local ? 51 : dimension),
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      },
    );

    testWidgets(
      'CircularProgressIndicator pixels match raw SDK variants RTL padding and trusted theme animation on $platform',
      (tester) async {
        final controller = AnimationController(vsync: tester, value: .37);
        try {
          for (final variant in ['material', 'adaptive']) {
            for (final direction in TextDirection.values) {
              for (final directional in [false, true]) {
                for (final value in <double?>[null, .3]) {
                  final padding = <String, Object?>{
                    'kind': directional
                        ? 'edgeInsetsDirectional'
                        : 'edgeInsets',
                    directional ? 'start' : 'left': 3,
                    'top': 4,
                    directional ? 'end' : 'right': 9,
                    'bottom': 6,
                  };
                  final theme = ThemeData(
                    progressIndicatorTheme: ProgressIndicatorThemeData(
                      controller: controller,
                    ),
                  );
                  await _pump(
                    tester,
                    _model(
                      platform: platform,
                      width: 64,
                      height: 64,
                      properties: {
                        'variant': _string(variant),
                        if (value != null) 'value': _double(value),
                        'padding': padding,
                        'backgroundColor': _color('0xFF112233'),
                        'valueColor': _color('0xFF00FF00'),
                        'strokeWidth': _int(6),
                        'strokeAlign': _double(-2),
                        'strokeCap': _enum('StrokeCap', 'round'),
                        'trackGap': _int(7),
                        'year2023': _bool(false),
                        'constraints': _constraints(min: 30, max: 80),
                      },
                    ),
                    theme: theme,
                    direction: direction,
                  );
                  final widget = tester.widget<CircularProgressIndicator>(
                    _finder(),
                  );
                  final insets = widget.padding!.resolve(direction);
                  expect(
                    insets,
                    EdgeInsets.fromLTRB(
                      directional && direction == TextDirection.rtl ? 9 : 3,
                      4,
                      directional && direction == TextDirection.rtl ? 3 : 9,
                      6,
                    ),
                  );
                  expect(
                    tester.getSize(
                      find
                          .descendant(
                            of: _finder(),
                            matching: find.byType(CustomPaint),
                          )
                          .first,
                    ),
                    const Size(52, 54),
                  );
                  if (value == null) {
                    final before = _painter(tester).rotationValue;
                    controller.value = .64;
                    await tester.pump();
                    expect(_painter(tester).rotationValue, isNot(before));
                    controller.value = .37;
                    await tester.pump();
                  }
                  final actual = await _painted(tester, _painter(tester));
                  final raw = variant == 'adaptive'
                      ? CircularProgressIndicator.adaptive(
                          value: value,
                          backgroundColor: const Color(0xFF112233),
                          valueColor: const AlwaysStoppedAnimation<Color?>(
                            Color(0xFF00FF00),
                          ),
                          strokeWidth: 6,
                          strokeAlign: -2,
                          strokeCap: StrokeCap.round,
                          trackGap: 7,
                          year2023: false,
                        )
                      : CircularProgressIndicator(
                          value: value,
                          backgroundColor: const Color(0xFF112233),
                          valueColor: const AlwaysStoppedAnimation<Color?>(
                            Color(0xFF00FF00),
                          ),
                          strokeWidth: 6,
                          strokeAlign: -2,
                          strokeCap: StrokeCap.round,
                          trackGap: 7,
                          year2023: false,
                        );
                  await tester.pumpWidget(
                    MaterialApp(
                      theme: theme.copyWith(platform: TargetPlatform.windows),
                      home: Directionality(
                        textDirection: direction,
                        child: Center(child: raw),
                      ),
                    ),
                  );
                  await tester.pump();
                  final painter = tester
                      .widget<CustomPaint>(
                        find
                            .descendant(
                              of: find.byType(CircularProgressIndicator),
                              matching: find.byType(CustomPaint),
                            )
                            .first,
                      )
                      .painter!;
                  expect(
                    actual,
                    orderedEquals(await _painted(tester, painter)),
                  );
                  expect(tester.takeException(), isNull);
                }
              }
            }
          }
        } finally {
          await tester.pumpWidget(const SizedBox());
          controller.dispose();
        }
      },
    );

    testWidgets(
      'CircularProgressIndicator value animation precedence explicit null and color theme preserve SDK state on $platform',
      (tester) async {
        State? state;
        for (final color in [
          null,
          _color('0xFF00FF00'),
          _theme('material.colorScheme.secondary'),
          _null,
          null,
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'value': _double(.3),
                'color': _color('0xFFFF0000'),
                'valueColor': ?color,
              },
            ),
          );
          final widget = tester.widget<CircularProgressIndicator>(_finder());
          if (state != null) expect(tester.state(_finder()), same(state));
          state = tester.state(_finder());
          expect(
            widget.valueColor,
            color == null ? isNull : isA<AlwaysStoppedAnimation<Color?>>(),
          );
          expect(
            _painter(tester).valueColor,
            color == null || color['kind'] == 'null'
                ? const Color(0xFFFF0000)
                : color['kind'] == 'color'
                ? const Color(0xFF00FF00)
                : Theme.of(tester.element(_finder())).colorScheme.secondary,
          );
          if (color?['kind'] == 'null') {
            expect(widget.valueColor!.value, isNull);
          }
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'CircularProgressIndicator material semantics clamp exact roles and guard explicit invalid strings on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final value in <double?>[null, -3, 0, .005, .5, 1, 5]) {
            for (final explicit in <String?>[
              null,
              '50%',
              'Free text',
              'NaN',
              '101',
              '',
            ]) {
              final model = _model(
                platform: platform,
                properties: {
                  if (value != null) 'value': _double(value),
                  'semanticsLabel': _string('Download'),
                  if (explicit != null) 'semanticsValue': _string(explicit),
                },
              );
              final before = jsonEncode(model);
              await _pump(tester, model);
              final invalid =
                  value != null && ['Free text', '101', ''].contains(explicit);
              expect(_finder(), invalid ? findsNothing : findsOneWidget);
              if (invalid) {
                expect(
                  _labels(tester),
                  contains('semanticsValue preview unavailable'),
                );
              } else {
                final widget = tester.widget<CircularProgressIndicator>(
                  _finder(),
                );
                expect(widget.value, value);
                expect(_painter(tester).value, value?.clamp(0, 1));
                final props = tester
                    .widget<Semantics>(
                      find
                          .descendant(
                            of: _finder(),
                            matching: find.byType(Semantics),
                          )
                          .first,
                    )
                    .properties;
                expect(
                  props.role,
                  value == null
                      ? ui.SemanticsRole.loadingSpinner
                      : ui.SemanticsRole.progressBar,
                );
                expect(props.label, 'Download');
                expect(
                  props.value,
                  explicit ??
                      (value == null
                          ? null
                          : '${(value.clamp(0, 1) * 100).round()}'),
                );
              }
              expect(jsonEncode(model), before);
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'CircularProgressIndicator exact signed geometry Infinity gap and diagnostics keep stored values on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final properties in <Map<String, Object?>>[
            {
              'strokeWidth': _int(-3),
              'strokeAlign': _int(-2),
              'trackGap': _int(-5),
            },
            {'strokeWidth': _double(1e308), 'strokeAlign': _int(0)},
            {
              'trackGap': _infinity,
              'strokeWidth': _int(0),
              'strokeAlign': _int(0),
            },
          ]) {
            final model = _model(
              platform: platform,
              width: 40,
              height: 40,
              properties: {
                'value': _double(.5),
                'year2023': _bool(false),
                ...properties,
              },
            );
            await _pump(tester, model);
            expect(_finder(), findsOneWidget);
            expect(tester.takeException(), isNull);
          }
          for (final properties in <Map<String, Object?>>[
            {'strokeWidth': _int(4), 'strokeAlign': _double(1e308)},
            {'padding': _padding(1e308)},
            {'controller': _reference},
            {'valueColor': _reference},
          ]) {
            final model = _model(platform: platform, properties: properties);
            final before = jsonEncode(model);
            await _pump(tester, model);
            expect(_finder(), findsNothing);
            expect(_labels(tester), contains('preview unavailable'));
            expect(jsonEncode(model), before);
            expect(
              find.byKey(const ValueKey('canvas-zero-size-widget-target-$_id')),
              findsOneWidget,
            );
            expect(tester.takeException(), isNull);
          }
          final infinite = _model(
            platform: platform,
            properties: {'constraints': _constraints(min: null)},
          );
          _wrap(infinite, 'flutter.widgets.Column', {}, list: true);
          await _pump(tester, infinite);
          expect(_finder(), findsNothing);
          expect(_labels(tester), contains('constraints preview unavailable'));
          expect(tester.takeException(), isNull);
          final huge = _model(
            platform: platform,
            properties: {
              'constraints': _constraints(min: 1e308),
              'strokeWidth': _double(1e308),
              'strokeAlign': _int(1),
            },
          );
          _wrap(huge, 'flutter.widgets.Column', {}, list: true);
          final before = jsonEncode(huge);
          await _pump(tester, huge);
          expect(_finder(), findsNothing);
          expect(_labels(tester), contains('overflow finite SDK arc bounds'));
          expect(jsonEncode(huge), before);
          expect(tester.takeException(), isNull);
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'CircularProgressIndicator stable state determinate adaptive transitions and muted ticker on $platform',
      (tester) async {
        State? state;
        for (final variant in ['material', 'adaptive', 'material']) {
          for (final value in <double?>[null, .5, null, 0, 1]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  'variant': _string(variant),
                  if (value != null) 'value': _double(value),
                },
              ),
            );
            if (state != null) expect(tester.state(_finder()), same(state));
            state = tester.state(_finder());
            expect(tester.takeException(), isNull);
          }
        }
        final model = _model(platform: platform);
        _wrap(model, 'flutter.widgets.TickerMode', {'enabled': _bool(false)});
        await _pump(tester, model);
        final before = _painter(tester).arcStart;
        await tester.pump(const Duration(milliseconds: 500));
        expect(_painter(tester).arcStart, before);
        final wrapper =
            ((_box(model)['slots'] as Map)['child'] as Map)['child'] as Map;
        (wrapper['properties'] as Map)['enabled'] = _bool(true);
        await _pump(tester, model);
        await tester.pump(const Duration(milliseconds: 500));
        expect(_painter(tester).arcStart, isNot(before));
      },
    );

    testWidgets(
      'CircularProgressIndicator zero selection F2 leaf and ordinary palette move paths on $platform',
      (tester) async {
        for (final dimension in [0.0, .001, 40.0]) {
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              width: dimension,
              height: dimension,
              properties: {'value': _double(.5)},
            ),
            selected: selected,
          );
          final target = dimension == 0
              ? find.byKey(
                  const ValueKey('canvas-zero-size-widget-target-$_id'),
                )
              : _widget(_id);
          await tester.tap(target);
          await tester.pump();
          expect(selected, contains(_id));
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          expect(find.byType(EditableText), findsNothing);
          expect(tester.takeException(), isNull);
        }
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final model = _model(platform: platform, width: 100, height: 100);
        final slot = (_box(model)['slots'] as Map)['child'] as Map;
        final leaf = slot['child'];
        slot['child'] = null;
        await _pump(
          tester,
          model,
          onDrop: (r) => drop = r,
          onMove: (r) => move = r,
        );
        final point = tester.getCenter(_widget(_boxId));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'circular',
            widgetType: _type,
            traits: const {},
          ),
        );
        expect(target?.parentWidgetId, _boxId);
        expect(target?.slotName, 'child');
        slot['child'] = leaf;
        _wrap(model, 'flutter.widgets.Column', {}, list: true);
        await _pump(
          tester,
          model,
          onDrop: (r) => drop = r,
          onMove: (r) => move = r,
        );
        expect(move!(_id, _outerId, 'children', 0)?.parentWidgetId, _outerId);
        expect(move!(_id, _id, 'child', 0), isNull);
      },
    );
  }
  for (final platform in ['ios', 'macos']) {
    testWidgets(
      'CircularProgressIndicator adaptive Cupertino ignores every unused property and external reference on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          State? state;
          for (final value in <double?>[null, -3, 0, .5, 1, 5, null]) {
            final model = _model(
              platform: platform,
              properties: {
                'variant': _string('adaptive'),
                if (value != null) 'value': _double(value),
                'backgroundColor': _color('0xFFFF0000'),
                'valueColor': _reference,
                if (value == null) 'controller': _reference,
                'strokeWidth': _double(1e308),
                'strokeAlign': _double(1e308),
                'strokeCap': _enum('StrokeCap', 'square'),
                'trackGap': _infinity,
                'constraints': _constraints(min: null),
                'padding': _padding(1e308),
                'year2023': _bool(false),
                'semanticsLabel': _string('Ignored label'),
                'semanticsValue': _string('Ignored free text'),
              },
            );
            final before = jsonEncode(model);
            await _pump(tester, model);
            expect(_finder(), findsOneWidget);
            if (state != null) expect(tester.state(_finder()), same(state));
            state = tester.state(_finder());
            final cupertino = find.descendant(
              of: _finder(),
              matching: find.byType(CupertinoActivityIndicator),
            );
            expect(cupertino, findsOneWidget);
            final activity = tester.widget<CupertinoActivityIndicator>(
              cupertino,
            );
            expect(activity.color, const Color(0xFFFF0000));
            expect(activity.animating, value == null);
            expect(activity.progress, value?.clamp(0, 1) ?? 1);
            expect(activity.radius, 10);
            expect(tester.getSize(_finder()), const Size(20, 20));
            expect(_labels(tester), isNot(contains('preview unavailable')));
            expect(_labels(tester), isNot(contains('Ignored label')));
            expect(jsonEncode(model), before);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );
    testWidgets(
      'CircularProgressIndicator material constructor remains Material on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(platform: platform, properties: {'value': _double(.5)}),
        );
        expect(find.byType(CupertinoActivityIndicator), findsNothing);
        expect(_painter(tester).value, .5);
      },
    );
  }
  testWidgets(
    'CircularProgressIndicator adaptive branch uses every non-Apple target and project refs recover on platform transition',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final platform in [
          'android',
          'linux',
          'windows',
          'web',
          'ios',
          'macos',
          'windows',
        ]) {
          final model = _model(
            platform: platform,
            properties: {
              'variant': _string('adaptive'),
              'valueColor': _reference,
              'controller': _reference,
            },
          );
          await _pump(tester, model);
          final apple = ['ios', 'macos'].contains(platform);
          expect(_finder(), apple ? findsOneWidget : findsNothing);
          expect(
            find.byType(CupertinoActivityIndicator),
            apple ? findsOneWidget : findsNothing,
          );
          if (!apple) {
            expect(_labels(tester), contains('does not execute project'));
          }
          expect(tester.takeException(), isNull);
          final props = _leaf(model)['properties'] as Map;
          props.remove('valueColor');
          props.remove('controller');
          await _pump(tester, model);
          expect(_finder(), findsOneWidget);
          expect(tester.takeException(), isNull);
        }
      } finally {
        semantics.dispose();
      }
    },
  );

  testWidgets(
    'raw SDK adaptive ignores controller padding constraints and exposes Cupertino ticks on every Apple target',
    (tester) async {
      final controller = AnimationController(vsync: tester, value: .37);
      try {
        for (final target in TargetPlatform.values) {
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(platform: target),
              themeAnimationDuration: Duration.zero,
              home: Center(
                child: CircularProgressIndicator.adaptive(
                  backgroundColor: Colors.red,
                  controller: controller,
                  valueColor: const AlwaysStoppedAnimation<Color?>(
                    Colors.green,
                  ),
                  constraints: const BoxConstraints(
                    minWidth: 70,
                    minHeight: 80,
                  ),
                  padding: const EdgeInsets.all(5),
                ),
              ),
            ),
          );
          final apple =
              target == TargetPlatform.iOS || target == TargetPlatform.macOS;
          expect(
            find.byType(CupertinoActivityIndicator),
            apple ? findsOneWidget : findsNothing,
          );
          expect(
            tester.getSize(find.byType(CircularProgressIndicator)),
            apple ? const Size(20, 20) : const Size(80, 90),
          );
          expect(tester.takeException(), isNull);
        }
      } finally {
        await tester.pumpWidget(const SizedBox());
        controller.dispose();
      }
    },
  );

  testWidgets('raw SDK CircularProgressIndicator numeric characterization', (
    tester,
  ) async {
    for (final property in ['strokeWidth', 'strokeAlign', 'trackGap']) {
      for (final number in [-3.0, 0.0, 1.0, 1e308, double.infinity]) {
        for (final value in <double?>[null, .5]) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: SizedBox(
                  width: 40,
                  height: 40,
                  child: CircularProgressIndicator(
                    value: value,
                    year2023: false,
                    backgroundColor: Colors.blue,
                    strokeWidth: property == 'strokeWidth' ? number : 4,
                    strokeAlign: property == 'strokeAlign' ? number : -1,
                    trackGap: property == 'trackGap' ? number : 4,
                  ),
                ),
              ),
            ),
          );
          final error = tester.takeException();
          final fails =
              property == 'strokeWidth' && number.isInfinite ||
              property == 'strokeAlign' && number >= 1e308;
          expect(
            error,
            fails ? isA<AssertionError>() : isNull,
            reason: '$property $number value=$value',
          );
        }
      }
    }
  });
  testWidgets(
    'raw SDK CircularProgressIndicator zero arc and signed gap paint are finite-engine tolerant',
    (tester) async {
      for (final size in [0.0, .001, 4.0, 40.0]) {
        for (final width in [0.0, 4.0, -4.0]) {
          for (final gap in [0.0, 4.0, double.infinity]) {
            await tester.pumpWidget(
              MaterialApp(
                home: Center(
                  child: SizedBox.square(
                    dimension: size,
                    child: CircularProgressIndicator(
                      value: .5,
                      year2023: false,
                      padding: EdgeInsets.zero,
                      backgroundColor: Colors.blue,
                      strokeWidth: width,
                      strokeAlign: 0,
                      trackGap: gap,
                    ),
                  ),
                ),
              ),
            );
            expect(tester.takeException(), isNull, reason: '$size/$width/$gap');
          }
        }
      }
    },
  );
}
