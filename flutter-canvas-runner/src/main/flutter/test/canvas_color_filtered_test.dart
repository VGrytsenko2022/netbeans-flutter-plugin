import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.ColorFiltered';
const presence = {'kind': 'dartObjectReferencePresence'};
const presets = [
  'mode',
  'matrix',
  'linearToSrgbGamma',
  'srgbToLinearGamma',
  'saturation',
];
Map<String, Object?> data([
  Map<String, Object?> p = const {},
  bool empty = false,
]) {
  final raw = a.data(), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'colorFilter': {'kind': 'string', 'value': 'matrix'},
    ...p,
  };
  n['slots'] = {
    'child': f.single(
      empty
          ? null
          : f.node(
              'f6c996d7-e409-4c19-8662-38d348537cfe',
              'flutter.widgets.ColoredBox',
              {
                'color': {'kind': 'color', 'argb': '0xFF224488'},
              },
              {
                'child': f.single(
                  f.node(
                    'cbdc4049-90b8-4cdb-83b6-60511abaf14f',
                    'flutter.widgets.SizedBox',
                    {'width': f.number(48), 'height': f.number(32)},
                  ),
                ),
              },
            ),
    ),
  };
  return raw;
}

void main() {
  test('closed24-row schema optional box slot and strict types', () {
    expect(canvasDropSlotsForWidgetType(type), hasLength(1));
    expect(
      canvasReviewedWidgetSchemaContract
          .split('W|$type\n')[1]
          .split('W|')[0]
          .split('\n')
          .where((x) => x.startsWith('P|')),
      hasLength(24),
    );
    for (final field in [
      'colorFilter',
      'color',
      'blendMode',
      'saturation',
      for (int r = 0; r < 4; r++)
        for (int c = 0; c < 5; c++) 'm$r$c',
    ]) {
      expect(
        () => f.decode(
          data({
            field: {'kind': 'null'},
          }),
        ),
        throwsFormatException,
      );
    }
    expect(
      () => f.decode(data({'colorFilter': presence, 'color': presence})),
      returnsNormally,
    );
    final missing = data();
    (a.builder(missing)['properties'] as Map).remove('colorFilter');
    expect(() => f.decode(missing), throwsFormatException);
    expect(
      () => f.decode(
        data({
          'colorFilter': {'kind': 'string', 'value': 'bogus'},
        }),
      ),
      throwsFormatException,
    );
    expect(
      () => f.decode(
        data({
          'saturation': {'kind': 'double', 'value': 'Infinity'},
        }),
      ),
      throwsFormatException,
    );
  });
  for (final preset in presets) {
    for (final empty in [false, true]) {
      testWidgets('native $preset empty=$empty preserves child geometry', (
        tester,
      ) async {
        await f.pump(
          tester,
          data({
            'colorFilter': {'kind': 'string', 'value': preset},
            'color': {'kind': 'color', 'argb': '0xFFFF0000'},
            'blendMode': {'kind': 'enum', 'type': 'BlendMode', 'value': 'src'},
            'm00': f.number(-1),
            'm04': f.number(255),
            'saturation': f.number(-0.5),
          }, empty),
        );
        final n = tester.widget<ColorFiltered>(find.byType(ColorFiltered));
        final matrix =
            <double>[1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0]
              ..[0] = -1
              ..[4] = 255;
        final expected = switch (preset) {
          'mode' => const ColorFilter.mode(Color(0xFFFF0000), BlendMode.src),
          'matrix' => ColorFilter.matrix(matrix),
          'linearToSrgbGamma' => const ColorFilter.linearToSrgbGamma(),
          'srgbToLinearGamma' => const ColorFilter.srgbToLinearGamma(),
          _ => ColorFilter.saturation(-0.5),
        };
        expect(n.colorFilter, expected);
        expect(n.child == null, empty);
        expect(
          find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
          findsOneWidget,
        );
        expect(find.text('Sibling'), findsOneWidget);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
      });
    }
  }
  for (final blend in BlendMode.values) {
    testWidgets('all native blend modes $blend', (tester) async {
      await f.pump(
        tester,
        data({
          'colorFilter': {'kind': 'string', 'value': 'mode'},
          'blendMode': {
            'kind': 'enum',
            'type': 'BlendMode',
            'value': blend.name,
          },
        }),
      );
      expect(
        tester.widget<ColorFiltered>(find.byType(ColorFiltered)).colorFilter,
        ColorFilter.mode(Colors.transparent, blend),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
  }
  testWidgets(
    'project override is identity and inactive source is not announced',
    (tester) async {
      await f.pump(
        tester,
        data({'colorFilter': presence, 'color': presence, 'm00': f.number(-1)}),
      );
      expect(
        tester.widget<ColorFiltered>(find.byType(ColorFiltered)).colorFilter,
        const ColorFilter.matrix([
          1,
          0,
          0,
          0,
          0,
          0,
          1,
          0,
          0,
          0,
          0,
          0,
          1,
          0,
          0,
          0,
          0,
          0,
          1,
          0,
        ]),
      );
      expect(
        tester
            .widgetList<Tooltip>(find.byType(Tooltip))
            .map((v) => v.message ?? '')
            .join(' '),
        contains('project-owned ColorFilter is not executed'),
      );
      await f.pump(
        tester,
        data({
          'colorFilter': {'kind': 'string', 'value': 'mode'},
          'color': presence,
        }),
      );
      expect(
        tester
            .widgetList<Tooltip>(find.byType(Tooltip))
            .map((v) => v.message ?? '')
            .join(' '),
        contains('project-owned Color is not executed'),
      );
      await f.pump(
        tester,
        data({
          'colorFilter': {'kind': 'string', 'value': 'matrix'},
          'color': presence,
        }),
      );
      expect(
        tester
            .widgetList<Tooltip>(find.byType(Tooltip))
            .map((v) => v.message ?? '')
            .join(' '),
        isNot(contains('project-owned Color')),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets('filter updates keep the same native render object', (
    tester,
  ) async {
    await f.pump(tester, data());
    final old = tester.renderObject<RenderObject>(find.byType(ColorFiltered));
    await f.pump(
      tester,
      data({
        'colorFilter': {'kind': 'string', 'value': 'saturation'},
        'saturation': f.number(0),
      }),
    );
    expect(tester.renderObject(find.byType(ColorFiltered)), same(old));
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
}
