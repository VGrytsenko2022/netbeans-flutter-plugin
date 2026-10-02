import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'shader_mask_native_test.dart' as pixels;

const type = 'flutter.widgets.ShaderMask';
Map<String, Object?> alignment(double x, {bool directional = true}) => {
  'basis': directional ? 'directional' : 'physical',
  'horizontal': x,
  'vertical': 0,
};
Map<String, Object?> gradient(String family, TileMode tile, bool theme) => {
  'kind': family,
  'tileMode': tile.name,
  'rotationRadians': -.25,
  'stops': [
    for (int i = 0; i < 3; i++)
      {
        'id': '30ff2123-0000-4000-8000-00000000000${i + 1}',
        'color': i == 0 && theme
            ? {'kind': 'theme', 'token': 'material.colorScheme.primary'}
            : {
                'kind': 'literal',
                'argb': ['0xFFFF0000', '0x8000FF00', '0x000000FF'][i],
              },
        'stop': i * .5,
      },
  ],
  if (family == 'linear') ...{'begin': alignment(-1), 'end': alignment(1)},
  if (family == 'radial') ...{
    'center': alignment(0),
    'radius': .75,
    'focal': alignment(-1),
    'focalRadius': .05,
  },
  if (family == 'sweep') ...{
    'center': alignment(0),
    'startAngle': -.5,
    'endAngle': 5.5,
  },
};
Map<String, Object?> data({
  String family = 'linear',
  TileMode tile = TileMode.clamp,
  bool theme = false,
  bool rtl = false,
  bool empty = false,
  bool source = false,
  BlendMode? blend,
}) {
  final raw = c.data({}, empty), node = a.builder(raw);
  node['type'] = type;
  node['properties'] = {
    'shaderCallback': source
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'gradient', 'gradient': gradient(family, tile, theme)},
    if (blend != null)
      'blendMode': {'kind': 'enum', 'type': 'BlendMode', 'value': blend.name},
  };
  raw['root'] = f.node(
    '30ff2123-0000-4000-8000-000000000099',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {'child': f.single(raw['root'] as Map<String, Object?>)},
  );
  return raw;
}

Gradient expected(String family, TileMode tile, Color first) {
  final colors = [first, const Color(0x8000ff00), const Color(0x000000ff)];
  const stops = [0.0, .5, 1.0], rotation = GradientRotation(-.25);
  return switch (family) {
    'linear' => LinearGradient(
      begin: AlignmentDirectional.centerStart,
      end: AlignmentDirectional.centerEnd,
      colors: colors,
      stops: stops,
      tileMode: tile,
      transform: rotation,
    ),
    'radial' => RadialGradient(
      center: AlignmentDirectional.center,
      radius: .75,
      focal: AlignmentDirectional.centerStart,
      focalRadius: .05,
      colors: colors,
      stops: stops,
      tileMode: tile,
      transform: rotation,
    ),
    _ => SweepGradient(
      center: AlignmentDirectional.center,
      startAngle: -.5,
      endAngle: 5.5,
      colors: colors,
      stops: stops,
      tileMode: tile,
      transform: rotation,
    ),
  };
}

void main() {
  test('exact required shader callback and strict gradient schema', () {
    expect(canvasModelProtocolVersion, 20);
    expect(canvasDropSlotsForWidgetType(type), hasLength(1));
    final section = canvasReviewedWidgetSchemaContract
        .split('W|$type\n')[1]
        .split('W|')[0];
    expect(section.split('\n').where((s) => s.startsWith('P|')), hasLength(2));
    for (final bad in [
      {'kind': 'null'},
      {'kind': 'string', 'value': 'shader'},
      {'kind': 'callback', 'handler': '_untyped'},
    ]) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)['shaderCallback'] = bad;
      expect(() => f.decode(raw), throwsFormatException);
    }
    final raw = data();
    (a.builder(raw)['properties'] as Map).remove('shaderCallback');
    expect(() => f.decode(raw), throwsFormatException);
    final unknown = data();
    ((a.builder(unknown)['properties'] as Map)['shaderCallback']
            as Map)['unexpected'] =
        0;
    expect(() => f.decode(unknown), throwsFormatException);
    expect(() => f.decode(data(source: true)), returnsNormally);
  });
  for (final family in ['linear', 'radial', 'sweep']) {
    for (final tile in TileMode.values) {
      for (final theme in [false, true]) {
        for (final rtl in [false, true]) {
          testWidgets(
            'gradient native pixels $family $tile theme=$theme rtl=$rtl',
            (tester) async {
              await f.pump(
                tester,
                data(family: family, tile: tile, theme: theme, rtl: rtl),
              );
              final finder = find.byType(ShaderMask),
                  mask = tester.widget<ShaderMask>(finder);
              final color = theme
                  ? Theme.of(tester.element(finder)).colorScheme.primary
                  : const Color(0xffff0000);
              final want = expected(family, tile, color);
              expect(mask.blendMode, BlendMode.modulate);
              for (final bounds in [
                const Rect.fromLTWH(0, 0, 48, 32),
                const Rect.fromLTWH(4, 8, 96, 64),
              ]) {
                final actual = await tester.runAsync(
                  () => pixels.raster(mask.shaderCallback(bounds), bounds),
                );
                final reference = await tester.runAsync(
                  () => pixels.raster(
                    want.createShader(
                      bounds,
                      textDirection: rtl
                          ? TextDirection.rtl
                          : TextDirection.ltr,
                    ),
                    bounds,
                  ),
                );
                expect(actual, reference);
              }
              expect(find.text('Sibling'), findsOneWidget);
              expect(tester.takeException(), isNull);
              await tester.pumpWidget(const SizedBox());
            },
          );
        }
      }
    }
  }
  for (final blend in BlendMode.values) {
    testWidgets('all blend modes $blend', (tester) async {
      await f.pump(tester, data(blend: blend));
      expect(
        tester.widget<ShaderMask>(find.byType(ShaderMask)).blendMode,
        blend,
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
  }
  for (final empty in [false, true]) {
    testWidgets('opaque source fallback with optional child=$empty', (
      tester,
    ) async {
      await f.pump(tester, data(source: true, empty: empty));
      final mask = tester.widget<ShaderMask>(find.byType(ShaderMask));
      expect(mask.child == null, empty);
      const bounds = Rect.fromLTWH(0, 0, 8, 8);
      final actual = await tester.runAsync(
        () => pixels.raster(mask.shaderCallback(bounds), bounds),
      );
      expect(actual, everyElement(255));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
  }
}
