import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_cross_axis_group_test.dart' as g;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const wrapperId = 'f2e043de-bea4-471e-90c9-2c23e0a2e6cb';
const type = 'flutter.widgets.SliverCrossAxisExpanded';
Map<String, Object?> wrapped(Map<String, Object?> child, [int flex = 1]) =>
    f.node(
      wrapperId,
      type,
      {
        'flex': {'kind': 'integer', 'value': flex},
      },
      {'sliver': f.single(child)},
    );
List children(Map<String, Object?> raw) =>
    ((f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['slots']
                as Map)['slivers']
            as Map)['children']
        as List;

void main() {
  test(
    'required positive flex and single sliver reject malformed persisted models',
    () {
      for (final bad in [null, 0, -1, 1.5, 9007199254740992, '2']) {
        final raw = g.data();
        final w = wrapped(children(raw).first as Map<String, Object?>);
        children(raw)[0] = w;
        w['properties'] = bad == null
            ? <String, Object?>{}
            : {
                'flex': {'kind': 'integer', 'value': bad},
              };
        expect(() => f.decode(raw), throwsFormatException, reason: '$bad');
      }
      for (final slot in [f.single(null), g.list([]), <String, Object?>{}]) {
        final raw = g.data();
        final w = wrapped(children(raw).first as Map<String, Object?>);
        children(raw)[0] = w;
        w['slots'] = {'sliver': slot};
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = g.data();
      final w = wrapped(children(raw).first as Map<String, Object?>);
      children(raw)[0] = w;
      expect(() => f.decode(raw), returnsNormally);
      final outer = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      outer['type'] = 'flutter.widgets.SliverMainAxisGroup';
      expect(() => f.decode(raw), throwsFormatException);
      outer['type'] = 'flutter.widgets.SliverCrossAxisGroup';
      outer['slots'] = {
        'slivers': g.list([
          f.node(
            g.nestedId,
            'flutter.widgets.SliverPadding',
            {
              'padding': {
                'kind': 'edgeInsets',
                'left': 0,
                'top': 0,
                'right': 0,
                'bottom': 0,
              },
            },
            {'sliver': f.single(w)},
          ),
        ]),
      };
      expect(() => f.decode(raw), throwsFormatException);
      raw['root'] = w;
      expect(() => f.decode(raw), throwsFormatException);
    },
  );

  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (final flex in [1, 2, 7, 9007199254740991]) {
          testWidgets(
            'native parent data flex=$flex horizontal=$horizontal reverse=$reverse rtl=$rtl',
            (tester) async {
              final raw = g.data(
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              );
              children(raw)[0] = wrapped(
                children(raw).first as Map<String, Object?>,
                flex,
              );
              await f.pump(tester, raw);
              final native = tester.widget<SliverCrossAxisExpanded>(
                find.byType(SliverCrossAxisExpanded),
              );
              expect(native.flex, flex);
              final group = tester.renderObject<RenderSliverCrossAxisGroup>(
                find.byType(SliverCrossAxisGroup),
              );
              expect(
                (group.firstChild!.parentData!
                        as SliverPhysicalContainerParentData)
                    .crossAxisFlex,
                flex,
              );
              expect(group.geometry!.scrollExtent, 96);
              final first = tester.renderObject<RenderBox>(
                find.byKey(
                  const ValueKey('canvas-widget-${f.bodyId}'),
                  skipOffstage: false,
                ),
              );
              final second = tester.renderObject<RenderBox>(
                find.byKey(
                  const ValueKey('canvas-widget-${g.lastBoxId}'),
                  skipOffstage: false,
                ),
              );
              final cross = horizontal ? 160.0 : 300.0;
              expect(
                horizontal ? first.size.height : first.size.width,
                closeTo(cross * flex / (flex + 1), .001),
              );
              expect(
                horizontal ? second.size.height : second.size.width,
                closeTo(cross / (flex + 1), .001),
              );
              expect(tester.takeException(), isNull);
            },
          );
        }
        testWidgets(
          'wrap targets use actual sliver geometry and move restrictions h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            final raw = g.data(
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
            );
            CanvasDropResolver? drop;
            CanvasMovePreviewResolver? move;
            await f.pump(
              tester,
              raw,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            final surface = tester.getRect(
              find.byType(CanvasDocumentView),
            );
            final box = tester.getRect(
              find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
            );
            final p = box.center;
            final target = drop!(
              ((p.dx - surface.left) / surface.width * 1000000).round(),
              ((p.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'wrap',
                widgetType: type,
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(target?.parentWidgetId, f.fillId);
            expect(target?.slotName, 'slivers');
            expect(target?.insertionIndex, 0);
            children(raw)[0] = wrapped(
              children(raw).first as Map<String, Object?>,
              2,
            );
            await f.pump(
              tester,
              raw,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            expect(
              move!(g.firstId, f.fillId, 'slivers', 2),
              isNull,
              reason: 'Required child cannot leave its wrapper.',
            );
            expect(move!(wrapperId, f.fillId, 'slivers', 1), isNotNull);
            expect(
              move!(
                wrapperId,
                '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
                'slivers',
                0,
              ),
              isNull,
            );
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets(
    'live flex edits, undo wrapping and redo retain child identity without stale parent data',
    (tester) async {
      final raw = g.data();
      final first = children(raw).first as Map<String, Object?>;
      Future<void> show(int? flex) async {
        children(raw)[0] = flex == null ? first : wrapped(first, flex);
        await f.pump(tester, raw);
        final group = tester.renderObject<RenderSliverCrossAxisGroup>(
          find.byType(SliverCrossAxisGroup),
        );
        final effective = flex ?? 1;
        expect(
          (group.firstChild!.parentData! as SliverPhysicalContainerParentData)
              .crossAxisFlex,
          effective,
        );
        final box = tester.getSize(
          find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
        );
        expect(box.width, closeTo(300 * effective / (effective + 1), .001));
        expect(tester.takeException(), isNull);
      }

      await show(null);
      await show(2);
      await show(5);
      await show(1);
      await show(null);
      await show(7);
    },
  );
  testWidgets('empty cross-axis group offers no orphan wrapper placeholder', (
    tester,
  ) async {
    final raw = g.data(empty: true);
    CanvasDropResolver? drop;
    await f.pump(tester, raw, drop: (v) => drop = v);
    for (final x in [10000, 100000, 500000, 900000, 990000]) {
      for (final y in [10000, 100000, 500000, 900000, 990000]) {
        expect(
          drop!(
            x,
            y,
            CanvasPaletteDragSource(
              token: 'empty',
              widgetType: type,
              traits: {canvasSliverWidgetTrait},
            ),
          ),
          isNull,
        );
      }
    }
    expect(tester.takeException(), isNull);
  });
}
