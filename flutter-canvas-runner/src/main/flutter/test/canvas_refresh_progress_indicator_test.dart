// ignore_for_file: deprecated_member_use

import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.RefreshProgressIndicator';
const _id = '1934f733-85ab-40bb-8c7a-360cdd4fccfe';
const _boxId = 'ca8c98a2-0aa1-4e41-bd02-8c123ecdbdad';
const _outerId = 'd5d112bb-47fc-4706-8f50-e9b4aa97df29';
const _null = {'kind': 'null'};
const _reference = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
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
    'properties': <String, Object?>{...properties},
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
  matching: find.byType(RefreshProgressIndicator),
);
Finder _paintFinder({Finder? of}) => find
    .descendant(
      of: of ?? _finder(),
      matching: find.byWidgetPredicate(
        (widget) =>
            widget is CustomPaint &&
            widget.painter?.runtimeType.toString() ==
                '_RefreshProgressIndicatorPainter',
      ),
    )
    .first;
Material _material(WidgetTester tester) => tester.widget<Material>(
  find.descendant(of: _finder(), matching: find.byType(Material)).first,
);
Opacity _opacity(WidgetTester tester) => tester.widget<Opacity>(
  find.descendant(of: _finder(), matching: find.byType(Opacity)).first,
);
Transform _rotation(WidgetTester tester) => tester.widget<Transform>(
  find.descendant(of: _finder(), matching: find.byType(Transform)).first,
);
dynamic _painter(WidgetTester tester) =>
    tester.widget<CustomPaint>(_paintFinder()).painter!;
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
      painter.paint(Canvas(recorder), const Size(17, 17));
      final picture = recorder.endRecording();
      final image = await picture.toImage(17, 17);
      try {
        return (await image.toByteData(
          format: ui.ImageByteFormat.rawRgba,
        ))!.buffer.asUint8List();
      } finally {
        image.dispose();
        picture.dispose();
      }
    }))!;

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    return (await image.toByteData(
      format: ui.ImageByteFormat.rawRgba,
    ))!.buffer.asUint8List();
  } finally {
    image.dispose();
  }
}))!;
List<int> _interior(Uint8List data) => [
  for (var y = 4; y < 45; y++)
    for (var x = 4; x < 45; x++)
      ...data.sublist((y * 49 + x) * 4, (y * 49 + x) * 4 + 4),
];

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
    'RefreshProgressIndicator exact twelve optional fields retain omission and explicit null width',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|', start + 3),
      );
      final rows = block
          .split('\n')
          .where((line) => line.startsWith('P|'))
          .toList();
      expect(rows, hasLength(12));
      for (final row in rows) {
        expect(row.split('|').sublist(3, 5), ['0', '-']);
      }
      expect(block, contains('P|strokeWidth|double,integer,null|'));
      expect(block, isNot(contains('S|')));
      expect(block, isNot(contains('C|')));
      expect(canvasModelProtocolVersion, 19);
      expect(_node(_decode(_model())).properties, isEmpty);
      expect(_leaf(_model())['slots'], isEmpty);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
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
          () => _decode(
            _model(
              properties: {
                name: {'kind': 'enum', 'type': 'double', 'value': 'infinity'},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      expect(
        () => _decode(_model(properties: {'strokeWidth': _null})),
        returnsNormally,
      );
      for (final name in [
        'variant',
        'controller',
        'constraints',
        'padding',
        'trackGap',
        'year2023',
        'child',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _null})),
          throwsFormatException,
        );
      }
      for (final props in <Map<String, Object?>>[
        {'elevation': _int(-1)},
        {'elevation': _null},
        {'strokeAlign': _null},
        {'value': _null},
        {'indicatorMargin': _padding(-1)},
        {'indicatorPadding': _padding(-1, directional: true)},
        {
          'valueColor': {
            'kind': 'dartObjectReferencePresence',
            'expression': 'execute()',
          },
        },
      ]) {
        expect(() => _decode(_model(properties: props)), throwsFormatException);
      }
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'RefreshProgressIndicator M2 M3 real shell defaults and all local theme fields on $platform',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final themed in [false, true]) {
            for (final local in [false, true]) {
              final theme = ThemeData(
                useMaterial3: material3,
                progressIndicatorTheme: themed
                    ? const ProgressIndicatorThemeData(
                        color: Colors.orange,
                        refreshBackgroundColor: Colors.purple,
                        strokeWidth: 7,
                        strokeAlign: -2,
                        strokeCap: StrokeCap.square,
                        // These are not Refresh constructor arguments or effective defaults.
                        year2023: false,
                        trackGap: 9,
                        constraints: BoxConstraints(
                          minWidth: 90,
                          minHeight: 90,
                        ),
                        circularTrackPadding: EdgeInsets.all(20),
                      )
                    : const ProgressIndicatorThemeData(),
              );
              await _pump(
                tester,
                _model(
                  platform: platform,
                  properties: {
                    'value': _double(.5),
                    if (local) ...{
                      'backgroundColor': _color('0xFF0000FF'),
                      'color': _color('0xFFFF0000'),
                      'strokeWidth': _int(6),
                      'strokeAlign': _int(1),
                      'strokeCap': _enum('StrokeCap', 'round'),
                      'elevation': _double(5),
                      'indicatorMargin': _padding(3),
                      'indicatorPadding': _padding(10),
                      'semanticsLabel': _string('Refreshing'),
                      'semanticsValue': _string('50%'),
                    },
                  },
                ),
                theme: theme,
              );
              final widget = tester.widget<RefreshProgressIndicator>(_finder());
              final painter = _painter(tester);
              final material = _material(tester);
              final effective = Theme.of(tester.element(_finder()));
              expect(widget.strokeWidth, local ? 6 : 2.5);
              expect(
                painter.strokeWidth,
                local ? 6 : 2.5,
                reason: 'omission keeps constructor2.5 even with theme7',
              );
              expect(
                painter.strokeAlign,
                local
                    ? 1
                    : themed
                    ? -2
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
                (local
                        ? const Color(0xFFFF0000)
                        : themed
                        ? Colors.orange
                        : effective.colorScheme.primary)
                    .withValues(alpha: 1),
              );
              expect(painter.trackColor, isNull);
              expect(painter.trackGap, isNull);
              expect(painter.year2023, true);
              expect(
                painter.value,
                isNull,
                reason:
                    'Refresh draws indeterminate arc even for determinate semantic value',
              );
              expect(painter.arrowheadScale, 1);
              expect(
                material.color,
                local
                    ? const Color(0xFF0000FF)
                    : themed
                    ? Colors.purple
                    : effective.canvasColor,
              );
              expect(material.type, MaterialType.circle);
              expect(material.elevation, local ? 5 : 2);
              expect(widget.indicatorMargin, EdgeInsets.all(local ? 3 : 4));
              expect(widget.indicatorPadding, EdgeInsets.all(local ? 10 : 12));
              expect(tester.getSize(_finder()), Size.square(local ? 47 : 49));
              expect(
                tester.getSize(_paintFinder()),
                Size.square(local ? 21 : 17),
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator width absent null explicit transitions and stopped colors preserve SDK state on $platform',
      (tester) async {
        State? state;
        for (final themed in [false, true]) {
          final theme = ThemeData(
            progressIndicatorTheme: themed
                ? const ProgressIndicatorThemeData(strokeWidth: 7)
                : const ProgressIndicatorThemeData(),
          );
          for (final width in <Map<String, Object?>?>[
            null,
            _null,
            _double(2.5),
            _int(-2),
            null,
            _null,
          ]) {
            for (final color in <Map<String, Object?>?>[
              null,
              _null,
              _color('0x8000FF00'),
              _theme('material.colorScheme.secondary'),
            ]) {
              final model = _model(
                platform: platform,
                properties: {
                  'value': _double(.5),
                  'strokeWidth': ?width,
                  'color': _color('0xFFFF0000'),
                  'valueColor': ?color,
                  'backgroundColor': _color('0xFF0000FF'),
                },
              );
              final before = jsonEncode(model);
              await _pump(tester, model, theme: theme);
              if (state != null) expect(tester.state(_finder()), same(state));
              state = tester.state(_finder());
              final widget = tester.widget<RefreshProgressIndicator>(_finder());
              expect(
                widget.strokeWidth,
                width == null
                    ? 2.5
                    : width['kind'] == 'null'
                    ? null
                    : width['value'],
              );
              expect(
                _painter(tester).strokeWidth,
                width == null
                    ? 2.5
                    : width['kind'] == 'null'
                    ? themed
                          ? 7
                          : 4
                    : width['value'],
              );
              expect(
                widget.valueColor,
                color == null ? isNull : isA<AlwaysStoppedAnimation<Color?>>(),
              );
              if (color?['kind'] == 'null') {
                expect(widget.valueColor!.value, isNull);
              }
              expect(_material(tester).color, const Color(0xFF0000FF));
              expect(
                _opacity(tester).opacity,
                color?['kind'] == 'color' ? 128 / 255 : 1,
              );
              final expected = color == null || color['kind'] == 'null'
                  ? const Color(0xFFFF0000)
                  : color['kind'] == 'color'
                  ? const Color(0xFF00FF00)
                  : Theme.of(tester.element(_finder())).colorScheme.secondary;
              expect(_painter(tester).valueColor, expected);
              expect(
                find.ancestor(
                  of: find
                      .descendant(
                        of: _finder(),
                        matching: find.byType(Material),
                      )
                      .first,
                  matching: find.byType(Opacity),
                ),
                findsNothing,
                reason:
                    'alpha applies to inner paint only, never the Material background',
              );
              expect(jsonEncode(model), before);
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator raw SDK painter pixels and rotation match across retained arrow state on $platform',
      (tester) async {
        final controller = AnimationController(vsync: tester, value: .37);
        final theme = ThemeData(
          progressIndicatorTheme: ProgressIndicatorThemeData(
            controller: controller,
          ),
        );
        try {
          final actual = <Uint8List>[];
          final transforms = <List<double>>[];
          State? state;
          for (final value in <double?>[null, 0, .1, .2, .7, null, 1, null]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  if (value != null) 'value': _double(value),
                  'color': _color('0xFF00FF00'),
                  'strokeWidth': _double(3),
                  'strokeAlign': _int(-1),
                  'strokeCap': _enum('StrokeCap', 'round'),
                },
              ),
              theme: theme,
            );
            if (state != null) expect(tester.state(_finder()), same(state));
            state = tester.state(_finder());
            actual.add(await _painted(tester, _painter(tester)));
            transforms.add(List.of(_rotation(tester).transform.storage));
            expect(tester.takeException(), isNull);
          }
          controller.value = .37;
          for (var i = 0; i < actual.length; i++) {
            final value = <double?>[null, 0, .1, .2, .7, null, 1, null][i];
            await tester.pumpWidget(
              MaterialApp(
                theme: theme,
                themeAnimationDuration: Duration.zero,
                home: Center(
                  child: RefreshProgressIndicator(
                    value: value,
                    color: const Color(0xFF00FF00),
                    strokeWidth: 3,
                    strokeAlign: -1,
                    strokeCap: StrokeCap.round,
                  ),
                ),
              ),
            );
            await tester.pump();
            final raw = find.byType(RefreshProgressIndicator);
            final painter = tester
                .widget<CustomPaint>(_paintFinder(of: raw))
                .painter!;
            expect(actual[i], orderedEquals(await _painted(tester, painter)));
            final transform = tester.widget<Transform>(
              find.descendant(of: raw, matching: find.byType(Transform)).first,
            );
            expect(transforms[i], orderedEquals(transform.transform.storage));
            expect(tester.takeException(), isNull);
          }
          expect(
            transforms[4],
            orderedEquals(transforms[5]),
            reason: 'value→null retains last determinate outer rotation',
          );
          expect(transforms[6], orderedEquals(transforms[7]));
        } finally {
          await tester.pumpWidget(const SizedBox());
          controller.dispose();
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator complete shell pixels preserve opaque Material behind translucent inner paint on $platform',
      (tester) async {
        for (final alpha in ['00', '80', 'FF']) {
          final model = _model(
            platform: platform,
            properties: {
              'value': _double(.5),
              'elevation': _int(0),
              'backgroundColor': _color('0xFF0000FF'),
              'valueColor': _color('0x${alpha}00FF00'),
            },
          );
          _wrap(model, 'flutter.widgets.RepaintBoundary', {});
          await _pump(tester, model);
          final boundary = tester.renderObject<RenderRepaintBoundary>(
            find
                .descendant(
                  of: _widget(_outerId),
                  matching: find.byType(RepaintBoundary),
                )
                .first,
          );
          final actual = await _pixels(tester, boundary);
          final key = GlobalKey();
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: RepaintBoundary(
                  key: key,
                  child: RefreshProgressIndicator(
                    value: .5,
                    elevation: 0,
                    backgroundColor: const Color(0xFF0000FF),
                    valueColor: AlwaysStoppedAnimation<Color?>(
                      Color(int.parse('${alpha}00FF00', radix: 16)),
                    ),
                  ),
                ),
              ),
            ),
          );
          await tester.pump();
          final expected = await _pixels(
            tester,
            key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
          );
          expect(_interior(actual), orderedEquals(_interior(expected)));
          expect(actual.sublist((24 * 49 + 24) * 4, (24 * 49 + 24) * 4 + 4), [
            0,
            0,
            255,
            255,
          ]);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator diagnostics are conditional on visible arrow and preserve every value on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final value in <double?>[null, 0, .1, .1001, .5]) {
            for (final alpha in ['0x0000FF00', '0x8000FF00', '0xFF00FF00']) {
              final model = _model(
                platform: platform,
                width: 70,
                height: 49,
                properties: {
                  if (value != null) 'value': _double(value),
                  'valueColor': _color(alpha),
                },
              );
              final before = jsonEncode(model);
              await _pump(tester, model);
              final unavailable =
                  value != null && value > .1 && alpha != '0x0000FF00';
              expect(_finder(), unavailable ? findsNothing : findsOneWidget);
              if (unavailable) {
                expect(_labels(tester), contains('square inner paint area'));
              }
              expect(jsonEncode(model), before);
              expect(tester.takeException(), isNull);
            }
          }
          for (final props in <Map<String, Object?>>[
            {'valueColor': _reference},
            {'strokeWidth': _int(4), 'strokeAlign': _double(1e308)},
            {'value': _double(.5), 'strokeWidth': _double(1e308)},
            {'indicatorMargin': _padding(1e308)},
            {'indicatorPadding': _padding(1e308)},
          ]) {
            final model = _model(platform: platform, properties: props);
            final before = jsonEncode(model);
            await _pump(tester, model);
            expect(_finder(), findsNothing);
            expect(_labels(tester), contains('preview unavailable'));
            expect(jsonEncode(model), before);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator matches RenderOpacity byte-rounded theme alpha before geometry diagnostics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final alpha in [.001, .002, .000001]) {
            for (final source in ['theme', 'color', 'valueColor']) {
              final color = Colors.green.withValues(alpha: alpha);
              final theme = ThemeData(
                colorScheme: ColorScheme.fromSeed(
                  seedColor: Colors.blue,
                ).copyWith(primary: color),
                progressIndicatorTheme: ProgressIndicatorThemeData(
                  color: color,
                ),
              );
              final model = _model(
                platform: platform,
                width: 70,
                height: 49,
                properties: {
                  'value': _double(.5),
                  if (source != 'theme')
                    source: _theme('material.colorScheme.primary'),
                },
              );
              await _pump(tester, model, theme: theme);
              final paints = Color.getAlphaFromOpacity(alpha) > 0;
              expect(_finder(), paints ? findsNothing : findsOneWidget);
              if (!paints) {
                expect(_opacity(tester).opacity, color.opacity);
              } else {
                expect(_labels(tester), contains('square inner paint area'));
              }
              expect(tester.takeException(), isNull);
              await tester.pumpWidget(
                MaterialApp(
                  theme: theme,
                  themeAnimationDuration: Duration.zero,
                  home: Center(
                    child: SizedBox(
                      width: 70,
                      height: 49,
                      child: RefreshProgressIndicator(
                        key: UniqueKey(),
                        value: .5,
                        color: source == 'color' ? color : null,
                        valueColor: source == 'valueColor'
                            ? AlwaysStoppedAnimation<Color?>(color)
                            : null,
                      ),
                    ),
                  ),
                ),
              );
              expect(
                tester.takeException(),
                paints ? isA<AssertionError>() : isNull,
              );
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator semantics use effective progress but painter stays indeterminate on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final value in <double?>[null, -3, 0, .005, .3, 1, 5]) {
            for (final text in <String?>[
              null,
              '45%',
              'free text',
              'NaN',
              '101',
              '',
            ]) {
              final model = _model(
                platform: platform,
                properties: {
                  if (value != null) 'value': _double(value),
                  'semanticsLabel': _string('Refresh'),
                  if (text != null) 'semanticsValue': _string(text),
                },
              );
              await _pump(tester, model);
              final invalid =
                  value != null && ['free text', '101', ''].contains(text);
              expect(_finder(), invalid ? findsNothing : findsOneWidget);
              if (invalid) {
                expect(
                  _labels(tester),
                  contains('semanticsValue preview unavailable'),
                );
              } else {
                expect(
                  tester.widget<RefreshProgressIndicator>(_finder()).value,
                  value,
                );
                expect(_painter(tester).value, isNull);
                final semantic = tester
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
                  semantic.role,
                  value == null
                      ? ui.SemanticsRole.loadingSpinner
                      : ui.SemanticsRole.progressBar,
                );
                expect(semantic.label, 'Refresh');
                expect(
                  semantic.value,
                  text ??
                      (value == null
                          ? null
                          : '${(value.clamp(0, 1) * 100).round()}'),
                );
              }
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'RefreshProgressIndicator honors directional insets ticker muting and zero-size leaf selection on $platform',
      (tester) async {
        for (final direction in TextDirection.values) {
          for (final directional in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  'indicatorMargin': {
                    'kind': directional
                        ? 'edgeInsetsDirectional'
                        : 'edgeInsets',
                    directional ? 'start' : 'left': 3,
                    'top': 4,
                    directional ? 'end' : 'right': 9,
                    'bottom': 8,
                  },
                  'indicatorPadding': {
                    'kind': directional
                        ? 'edgeInsetsDirectional'
                        : 'edgeInsets',
                    directional ? 'start' : 'left': 1,
                    'top': 2,
                    directional ? 'end' : 'right': 3,
                    'bottom': 2,
                  },
                  'value': _double(.5),
                },
              ),
              direction: direction,
            );
            final widget = tester.widget<RefreshProgressIndicator>(_finder());
            expect(
              widget.indicatorMargin.resolve(direction),
              EdgeInsets.fromLTRB(
                directional && direction == TextDirection.rtl ? 9 : 3,
                4,
                directional && direction == TextDirection.rtl ? 3 : 9,
                8,
              ),
            );
            expect(tester.getSize(_paintFinder()), const Size(37, 37));
            expect(tester.takeException(), isNull);
          }
        }
        final model = _model(platform: platform);
        _wrap(model, 'flutter.widgets.TickerMode', {'enabled': _bool(false)});
        await _pump(tester, model);
        final start = _painter(tester).arcStart;
        await tester.pump(const Duration(milliseconds: 500));
        expect(_painter(tester).arcStart, start);
        final wrapper =
            ((_box(model)['slots'] as Map)['child'] as Map)['child'] as Map;
        (wrapper['properties'] as Map)['enabled'] = _bool(true);
        await _pump(tester, model);
        await tester.pump(const Duration(milliseconds: 500));
        expect(_painter(tester).arcStart, isNot(start));
        for (final size in [0.0, .001, 49.0]) {
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              width: size,
              height: size,
              properties: {'value': _double(.5)},
            ),
            selected: selected,
          );
          final target = size == 0
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
      },
    );

    testWidgets(
      'RefreshProgressIndicator is ordinary palette leaf with explicit move geometry not pull-to-refresh behavior on $platform',
      (tester) async {
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
            token: 'refresh',
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
        expect(find.byType(RefreshIndicator), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'RefreshProgressIndicator remains Material on every supported preview platform',
    (tester) async {
      for (final platform in [
        'android',
        'ios',
        'macos',
        'linux',
        'windows',
        'web',
      ]) {
        await _pump(
          tester,
          _model(platform: platform, properties: {'value': _double(.5)}),
        );
        expect(_finder(), findsOneWidget);
        expect(_material(tester).type, MaterialType.circle);
        expect(tester.getSize(_finder()), const Size(49, 49));
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'RefreshProgressIndicator trusted theme controller is written by determinate build and not started on null',
    (tester) async {
      final controller = AnimationController(vsync: tester, value: .37);
      final theme = ThemeData(
        progressIndicatorTheme: ProgressIndicatorThemeData(
          controller: controller,
        ),
      );
      try {
        await _pump(tester, _model(), theme: theme);
        expect(controller.value, .37);
        expect(controller.isAnimating, false);
        await _pump(
          tester,
          _model(properties: {'value': _double(.5)}),
          theme: theme,
        );
        expect(controller.value, closeTo(1 / 4444, 1e-12));
        await _pump(tester, _model(), theme: theme);
        final start = _painter(tester).arcStart;
        await tester.pump(const Duration(milliseconds: 500));
        expect(controller.isAnimating, false);
        expect(_painter(tester).arcStart, start);
        controller.value = .1;
        await tester.pump();
        expect(_painter(tester).arcStart, isNot(start));
        expect(tester.takeException(), isNull);
      } finally {
        await tester.pumpWidget(const SizedBox());
        controller.dispose();
      }
    },
  );
  testWidgets(
    'raw SDK Refresh arrow square assertion is skipped for zero opacity and nonvisible arrow',
    (tester) async {
      for (final value in <double?>[null, 0, .1, .1001, .5]) {
        for (final color in [
          const Color(0x0000FF00),
          const Color(0x8000FF00),
          const Color(0xFF00FF00),
        ]) {
          await tester.pumpWidget(
            MaterialApp(
              home: Center(
                child: SizedBox(
                  width: 70,
                  height: 49,
                  child: RefreshProgressIndicator(
                    key: UniqueKey(),
                    value: value,
                    valueColor: AlwaysStoppedAnimation<Color?>(color),
                  ),
                ),
              ),
            ),
          );
          final failure = value != null && value > .1 && color.a > 0;
          expect(
            tester.takeException(),
            failure ? isA<AssertionError>() : isNull,
            reason: 'value=$value opacity=${color.a}',
          );
        }
      }
    },
  );
  testWidgets(
    'raw SDK RefreshProgressIndicator signed width and alignment characterization',
    (tester) async {
      for (final name in ['strokeWidth', 'strokeAlign']) {
        for (final number in [-3.0, 0.0, 1.0, 1e308, double.infinity]) {
          for (final value in <double?>[null, 0, .1, .5]) {
            await tester.pumpWidget(
              MaterialApp(
                home: Center(
                  child: RefreshProgressIndicator(
                    value: value,
                    strokeWidth: name == 'strokeWidth' ? number : 4,
                    strokeAlign: name == 'strokeAlign' ? number : -1,
                  ),
                ),
              ),
            );
            final error = tester.takeException();
            final fails =
                name == 'strokeWidth' && number.isInfinite ||
                name == 'strokeAlign' && number >= 1e308;
            expect(
              error,
              fails ? isA<AssertionError>() : isNull,
              reason: '$name=$number value=$value',
            );
          }
        }
      }
    },
  );
}
