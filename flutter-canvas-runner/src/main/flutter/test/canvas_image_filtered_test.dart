import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.ImageFiltered';
const presence = {'kind': 'dartObjectReferencePresence'};
const presets = ['blur', 'dilate', 'erode', 'matrix', 'compose', 'shader'];
Map<String, Object?> data([
  Map<String, Object?> props = const {},
  bool empty = false,
]) {
  final raw = c.data({}, empty), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'imageFilter': {'kind': 'string', 'value': 'blur'},
    ...props,
  };
  return raw;
}

void main() {
  test('closed17-row schema sources and shader activation', () {
    expect(canvasDropSlotsForWidgetType(type), hasLength(1));
    expect(
      canvasReviewedWidgetSchemaContract
          .split('W|$type\n')[1]
          .split('W|')[0]
          .split('\n')
          .where((x) => x.startsWith('P|')),
      hasLength(17),
    );
    for (final field in [
      'imageFilter',
      'enabled',
      'sigmaX',
      'sigmaY',
      'radiusX',
      'radiusY',
      'matrix4',
      'filterQuality',
      'inner',
      'outer',
      'shader',
    ]) {
      expect(
        () => f.decode(
          data({
            field: {'kind': 'null'},
          }),
        ),
        throwsFormatException,
        reason: field,
      );
    }
    for (final field in [
      'sigmaX',
      'sigmaY',
      'radiusX',
      'radiusY',
      'boundsWidth',
      'boundsHeight',
    ]) {
      expect(() => f.decode(data({field: f.number(-1)})), returnsNormally);
    }
    expect(
      () => f.decode(
        data({
          'imageFilter': {'kind': 'string', 'value': 'shader'},
        }),
      ),
      throwsFormatException,
    );
    expect(
      () => f.decode(
        data({
          'imageFilter': {'kind': 'string', 'value': 'shader'},
          'shader': presence,
        }),
      ),
      returnsNormally,
    );
    expect(
      () => f.decode(data({'imageFilter': presence, 'shader': presence})),
      returnsNormally,
    );
    expect(
      () => f.decode(
        data({
          'tileMode': {'kind': 'null'},
          'bounds': {'kind': 'null'},
          'boundsLeft': f.number(-4),
        }),
      ),
      returnsNormally,
    );
  });
  for (final preset in presets) {
    for (final enabled in [true, false]) {
      for (final empty in [true, false]) {
        testWidgets('native $preset enabled=$enabled empty=$empty', (
          tester,
        ) async {
          await f.pump(
            tester,
            data({
              'imageFilter': {'kind': 'string', 'value': preset},
              'enabled': {'kind': 'boolean', 'value': enabled},
              if (preset == 'shader') 'shader': presence,
              'sigmaX': f.number(2),
              'sigmaY': f.number(3),
              'radiusX': f.number(1),
              'radiusY': f.number(2),
            }, empty),
          );
          final widget = tester.widget<ImageFiltered>(
            find.byType(ImageFiltered),
          );
          final expected = switch (preset) {
            'blur' => ui.ImageFilter.blur(sigmaX: 2, sigmaY: 3),
            'dilate' => ui.ImageFilter.dilate(radiusX: 1, radiusY: 2),
            'erode' => ui.ImageFilter.erode(radiusX: 1, radiusY: 2),
            'matrix' => ui.ImageFilter.matrix(Matrix4.identity().storage),
            'compose' => ui.ImageFilter.compose(
              inner: ui.ImageFilter.blur(),
              outer: ui.ImageFilter.blur(),
            ),
            _ => ui.ImageFilter.blur(),
          };
          expect(widget.imageFilter, expected);
          expect(widget.enabled, enabled);
          expect(
            tester.getSize(find.byType(ImageFiltered)),
            empty ? Size.zero : const Size(48, 32),
          );
          expect(tester.takeException(), isNull);
          await tester.pumpWidget(const SizedBox());
        });
      }
    }
  }
  for (final tile in TileMode.values) {
    testWidgets('blur local bounds and tile ${tile.name}', (tester) async {
      await f.pump(
        tester,
        data({
          'sigmaX': f.number(2),
          'tileMode': {'kind': 'enum', 'type': 'TileMode', 'value': tile.name},
          'boundsLeft': f.number(-3),
          'boundsTop': f.number(4),
          'boundsWidth': f.number(12),
          'boundsHeight': f.number(8),
        }),
      );
      expect(
        tester.widget<ImageFiltered>(find.byType(ImageFiltered)).imageFilter,
        ui.ImageFilter.blur(
          sigmaX: 2,
          tileMode: tile,
          bounds: const Rect.fromLTWH(-3, 4, 12, 8),
        ),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
  }
  for (final quality in FilterQuality.values) {
    testWidgets('column major matrix quality ${quality.name}', (tester) async {
      final matrix = Matrix4.translationValues(3, 4, 0);
      await f.pump(
        tester,
        data({
          'imageFilter': {'kind': 'string', 'value': 'matrix'},
          'matrix4': {'kind': 'matrix4', 'storage': matrix.storage.toList()},
          'filterQuality': {
            'kind': 'enum',
            'type': 'FilterQuality',
            'value': quality.name,
          },
        }),
      );
      expect(
        tester.widget<ImageFiltered>(find.byType(ImageFiltered)).imageFilter,
        ui.ImageFilter.matrix(matrix.storage, filterQuality: quality),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    });
  }
  testWidgets('source overrides inactive fields and matrix snapshots', (
    tester,
  ) async {
    await f.pump(
      tester,
      data({
        'imageFilter': presence,
        'sigmaX': f.number(99),
        'bounds': presence,
        'shader': presence,
      }),
    );
    expect(
      tester.widget<ImageFiltered>(find.byType(ImageFiltered)).imageFilter,
      ui.ImageFilter.blur(),
    );
    expect(
      tester
          .widgetList<Tooltip>(find.byType(Tooltip))
          .map((v) => v.message ?? '')
          .join(' '),
      contains('project-owned ImageFilter'),
    );
    await f.pump(
      tester,
      data({
        'imageFilter': {'kind': 'string', 'value': 'matrix'},
        'matrix4': presence,
        'shader': presence,
      }),
    );
    expect(
      tester.widget<ImageFiltered>(find.byType(ImageFiltered)).imageFilter,
      ui.ImageFilter.matrix(Matrix4.identity().storage),
    );
    expect(
      tester
          .widgetList<Tooltip>(find.byType(Tooltip))
          .map((v) => v.message ?? '')
          .join(' '),
      isNot(contains('project-owned shader')),
    );
    await f.pump(
      tester,
      data({'bounds': presence, 'boundsWidth': f.number(20)}),
    );
    expect(
      tester.widget<ImageFiltered>(find.byType(ImageFiltered)).imageFilter,
      ui.ImageFilter.blur(),
    );
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });
  testWidgets(
    'toggle Enabled keeps native render object and changes layer requirement',
    (tester) async {
      await f.pump(tester, data());
      final render = tester.renderObject<RenderObject>(
        find.byType(ImageFiltered),
      );
      expect(render.needsCompositing, isTrue);
      await f.pump(
        tester,
        data({
          'enabled': {'kind': 'boolean', 'value': false},
        }),
      );
      expect(tester.renderObject(find.byType(ImageFiltered)), same(render));
      expect(render.needsCompositing, isFalse);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
}
