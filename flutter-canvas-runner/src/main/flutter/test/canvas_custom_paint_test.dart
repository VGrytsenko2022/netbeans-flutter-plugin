import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.CustomPaint';
const presence = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> data([
  Map<String, Object?> props = const {},
  bool empty = false,
]) {
  final raw = c.data({}, empty), node = a.builder(raw);
  node['type'] = type;
  node['properties'] = props;
  return raw;
}

void main() {
  testWidgets(
    'zero-size CustomPaint remains selectable and palette insertable',
    (tester) async {
      final selected = <String>[];
      CanvasDropResolver? resolver;
      await f.pump(
        tester,
        data({}, true),
        selected: selected.add,
        drop: (value) => resolver = value,
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
      );
      expect(target, findsOneWidget);
      await tester.tapAt(tester.getCenter(target));
      expect(selected, contains(a.builderId));
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point =
          tester.getBottomLeft(
            find.byKey(const ValueKey('canvas-widget-${a.columnId}')),
          ) +
          const Offset(100, -5);
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'custom-paint',
          widgetType: type,
          traits: {},
        ),
      );
      expect(drop, isNotNull);
      expect(drop!.parentWidgetId, isNot(a.builderId));
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'property refresh preserves native renderer and child wins over preferred size',
    (tester) async {
      Finder painted() =>
          find.byWidgetPredicate((w) => w is CustomPaint && w.size.width == 65);
      await f.pump(
        tester,
        data({
          'size': {'kind': 'size', 'width': 65.0, 'height': 29.0},
        }, true),
      );
      final render = tester.renderObject<RenderCustomPaint>(painted());
      await f.pump(
        tester,
        data({
          'size': {'kind': 'size', 'width': 65.0, 'height': 29.0},
          'painter': presence,
        }),
      );
      expect(tester.renderObject(painted()), same(render));
      expect(render.size, const Size(48, 32));
      expect(
        find.byKey(const ValueKey('canvas-widget-${a.builderId}')),
        findsOneWidget,
      );
      expect(find.text('Sibling'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
  test('five rows optional child strict wire and relationship validation', () {
    final section = canvasReviewedWidgetSchemaContract
        .split('W|$type\n')[1]
        .split('W|')[0];
    expect(section.split('\n').where((s) => s.startsWith('P|')), hasLength(5));
    expect(canvasDropSlotsForWidgetType(type), hasLength(1));
    expect(() => f.decode(data()), returnsNormally);
    for (final field in ['painter', 'foregroundPainter', 'size']) {
      expect(() => f.decode(data({field: presence})), returnsNormally);
      expect(
        () => f.decode(
          data({
            field: {'kind': 'string', 'value': 'raw source'},
          }),
        ),
        throwsFormatException,
      );
    }
    for (final field in ['isComplex', 'willChange']) {
      expect(
        () => f.decode(
          data({
            field: {'kind': 'boolean', 'value': true},
          }),
        ),
        throwsFormatException,
      );
      for (final painter in ['painter', 'foregroundPainter']) {
        expect(
          () => f.decode(
            data({
              painter: presence,
              field: {'kind': 'boolean', 'value': true},
            }),
          ),
          returnsNormally,
        );
      }
    }
    for (final value in [
      {'kind': 'null'},
      {'kind': 'size', 'width': -1, 'height': 0},
      {'kind': 'size', 'width': 1, 'height': 2, 'extra': 3},
    ]) {
      expect(() => f.decode(data({'size': value})), throwsFormatException);
    }
    expect(
      () => f.decode(data({'unexpected': presence})),
      throwsFormatException,
    );
  });
  for (final empty in [false, true]) {
    for (final front in [false, true]) {
      for (final back in [false, true]) {
        for (final hint in [false, true]) {
          if (hint && !front && !back) continue;
          testWidgets(
            'native source fallback empty=$empty front=$front back=$back hint=$hint',
            (tester) async {
              await f.pump(
                tester,
                data({
                  if (front) 'foregroundPainter': presence,
                  if (back) 'painter': presence,
                  'size': {'kind': 'size', 'width': 65.0, 'height': 29.0},
                  'isComplex': {'kind': 'boolean', 'value': hint},
                  'willChange': {'kind': 'boolean', 'value': hint},
                }, empty),
              );
              final finder = find.byWidgetPredicate(
                (w) => w is CustomPaint && w.size == const Size(65, 29),
              );
              final w = tester.widget<CustomPaint>(finder),
                  r = tester.renderObject<RenderCustomPaint>(finder);
              expect(w.painter != null, back);
              expect(w.foregroundPainter != null, front);
              expect(w.isComplex, hint);
              expect(w.willChange, hint);
              expect(r.size, empty ? const Size(65, 29) : const Size(48, 32));
              expect(w.painter?.hitTest(Offset.zero), back ? false : null);
              expect(
                w.foregroundPainter?.hitTest(Offset.zero),
                front ? false : null,
              );
              expect(w.painter?.semanticsBuilder, isNull);
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
  testWidgets('source Size uses zero and source disclosure is visible', (
    tester,
  ) async {
    await f.pump(tester, data({'size': presence}, true));
    final tips = tester.widgetList<Tooltip>(find.byType(Tooltip));
    expect(
      tips.any((t) => t.message?.contains('CustomPaint') ?? false),
      isTrue,
    );
    expect(tester.takeException(), isNull);
  });
}
