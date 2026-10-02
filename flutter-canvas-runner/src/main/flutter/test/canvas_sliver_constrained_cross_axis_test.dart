import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_cross_axis_group_test.dart' as g;

const type = 'flutter.widgets.SliverConstrainedCrossAxis';
const wrapperId = 'cdde8aa1-1d91-4593-9901-33b56b2966ce';
const infinity = {'kind': 'enum', 'type': 'double', 'value': 'infinity'};
Map<String, Object?> wrapped(
  String id,
  Map<String, Object?> child,
  Object extent,
) => f.node(
  id,
  type,
  {
    'maxExtent': extent is Map<String, Object?>
        ? extent
        : f.number((extent as num).toDouble()),
  },
  {'sliver': f.single(child)},
);
Map<String, Object?> data({
  Object extent = 120.0,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
}) {
  final raw = f.modelJson(
    empty: true,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  fill.addAll(wrapped(f.fillId, g.adapter(g.firstId, f.bodyId, 48), extent));
  return raw;
}

void main() {
  test(
    'strict schema accepts only required nonnegative numeric extent and one sliver',
    () {
      for (final value in [0, .125, 1e308, infinity]) {
        expect(() => f.decode(data(extent: value)), returnsNormally);
      }
      for (final bad in [
        <String, Object?>{},
        {'maxExtent': f.number(-1)},
        {
          'maxExtent': {'kind': 'null'},
        },
        {
          'maxExtent': {'kind': 'string', 'value': 'double.infinity'},
        },
        {
          'maxExtent': {'kind': 'enum', 'type': 'double', 'value': 'nan'},
        },
        {
          'maxExtent': {
            'kind': 'enum',
            'type': 'double',
            'value': 'negativeInfinity',
          },
        },
      ]) {
        final raw = data();
        f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        )['properties'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final bad in [
        f.single(null),
        g.list([]),
        f.single(
          f.node(g.firstId, 'flutter.widgets.Text', {
            'data': {'kind': 'string', 'value': 'box'},
          }),
        ),
      ]) {
        final raw = data();
        f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['slots'] = {
          'sliver': bad,
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data();
      raw['root'] = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (final extent in [0.0, .125, 50.0, 120.0, 1e308, infinity]) {
          testWidgets(
            'Canvas clamps natively extent=$extent h=$horizontal reverse=$reverse rtl=$rtl',
            (tester) async {
              final raw = data(
                extent: extent,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              );
              await f.pump(tester, raw);
              final native = tester.widget<SliverConstrainedCrossAxis>(
                find.byType(SliverConstrainedCrossAxis),
              );
              final expected = math.min(
                extent is num ? extent.toDouble() : double.infinity,
                horizontal ? 160.0 : 300.0,
              );
              expect(
                native.maxExtent,
                extent is num ? extent : double.infinity,
              );
              final render = tester
                  .renderObject<RenderSliverConstrainedCrossAxis>(
                    find.byType(SliverConstrainedCrossAxis),
                  );
              expect(render.geometry!.crossAxisExtent, expected);
              expect(render.geometry!.scrollExtent, 48);
              expect(
                (render.parentData! as SliverPhysicalParentData).crossAxisFlex,
                0,
              );
              final size = tester.getSize(
                find.byKey(
                  const ValueKey('canvas-widget-${f.bodyId}'),
                  skipOffstage: false,
                ),
              );
              expect(horizontal ? size.height : size.width, expected);
              expect(tester.takeException(), isNull);
            },
          );
        }
        testWidgets(
          'constrained and expanded lanes share remaining space with exact wrap zones h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            final raw = g.data(
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
            );
            final group = f.findNode(
              raw['root'] as Map<String, Object?>,
              f.fillId,
            );
            final list =
                ((group['slots'] as Map)['slivers'] as Map)['children'] as List;
            list[0] = wrapped(wrapperId, list[0] as Map<String, Object?>, 40.0);
            list[1] = f.node(
              g.nestedId,
              'flutter.widgets.SliverCrossAxisExpanded',
              {
                'flex': {'kind': 'integer', 'value': 2},
              },
              {'sliver': f.single(list[1] as Map<String, Object?>)},
            );
            CanvasDropResolver? drop;
            CanvasMovePreviewResolver? move;
            await f.pump(
              tester,
              raw,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            final first = tester.getRect(
              find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
            );
            final second = tester.getRect(
              find.byKey(const ValueKey('canvas-widget-${g.lastBoxId}')),
            );
            final firstSize = tester.getSize(
              find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
            );
            final secondSize = tester.getSize(
              find.byKey(const ValueKey('canvas-widget-${g.lastBoxId}')),
            );
            expect(horizontal ? firstSize.height : firstSize.width, 40);
            expect(
              horizontal ? secondSize.height : secondSize.width,
              (horizontal ? 160 : 300) - 40,
            );
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            CanvasDropTarget? at(Offset p) => drop!(
              ((p.dx - surface.left) / surface.width * 1000000).round(),
              ((p.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'constrained',
                widgetType: type,
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(
              at(first.center)?.parentWidgetId,
              wrapperId,
              reason:
                  'Nested required Sliver replacement wraps the actual first lane.',
            );
            expect(
              at(second.center)?.parentWidgetId,
              g.nestedId,
              reason: 'The constrained lane must not steal another lane hit.',
            );
            expect(move!(g.firstId, f.fillId, 'slivers', 2), isNull);
            expect(
              move!(
                wrapperId,
                '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
                'slivers',
                1,
              ),
              isNotNull,
            );
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets(
    'live limit edits and Undo/Redo preserve stable children and clear stale zero flex',
    (tester) async {
      final raw = g.data();
      final group = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      final list =
          ((group['slots'] as Map)['slivers'] as Map)['children'] as List;
      final child = list.first as Map<String, Object?>;
      Future<void> show(double? max) async {
        list[0] = max == null ? child : wrapped(wrapperId, child, max);
        await f.pump(tester, raw);
        final render = tester.renderObject<RenderSliverCrossAxisGroup>(
          find.byType(SliverCrossAxisGroup),
        );
        expect(
          (render.firstChild!.parentData! as SliverPhysicalParentData)
              .crossAxisFlex,
          max == null ? 1 : 0,
        );
        expect(
          tester
              .getSize(find.byKey(const ValueKey('canvas-widget-${f.bodyId}')))
              .width,
          max ?? 150,
        );
        expect(tester.takeException(), isNull);
      }

      await show(null);
      await show(50);
      await show(80);
      await show(0);
      await show(null);
      await show(120);
    },
  );
  testWidgets(
    'zero cross-axis wrapper remains selectable and offscreen wrapper has no stale target',
    (tester) async {
      CanvasDropResolver? drop;
      var raw = data(extent: 0.0);
      final selected = <String>[];
      await f.pump(tester, raw, drop: (v) => drop = v, selected: selected.add);
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final viewport = tester.getRect(find.byType(CustomScrollView));
      final hit = viewport.topLeft + const Offset(8, 8);
      await tester.tapAt(hit);
      await tester.pump();
      expect(
        selected,
        contains(f.bodyId),
        reason:
            'The zero-width descendant retains its bounded selection handle.',
      );
      raw = data(extent: 50.0, preceding: 700);
      await f.pump(tester, raw, drop: (v) => drop = v);
      final target = drop!(
        ((hit.dx - surface.left) / surface.width * 1000000).round(),
        ((hit.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'scrolled',
          widgetType: type,
          traits: {canvasSliverWidgetTrait},
        ),
      );
      expect(target?.parentWidgetId, isNot(f.fillId));
      expect(tester.takeException(), isNull);
    },
  );

  for (final parent in [
    'viewport',
    'main',
    'padding',
    'cross',
    'expanded',
    'nested',
  ]) {
    testWidgets('native supported parent $parent', (tester) async {
      Widget sliver = const SliverConstrainedCrossAxis(
        maxExtent: 50,
        sliver: SliverToBoxAdapter(child: SizedBox(width: 40, height: 40)),
      );
      sliver = switch (parent) {
        'main' => SliverMainAxisGroup(slivers: [sliver]),
        'padding' => SliverPadding(
          padding: const EdgeInsets.all(4),
          sliver: sliver,
        ),
        'cross' => SliverCrossAxisGroup(slivers: [sliver]),
        'expanded' => SliverCrossAxisGroup(
          slivers: [SliverCrossAxisExpanded(flex: 2, sliver: sliver)],
        ),
        'nested' => SliverConstrainedCrossAxis(maxExtent: 25, sliver: sliver),
        _ => sliver,
      };
      await tester.pumpWidget(
        MaterialApp(
          home: SizedBox(
            width: 300,
            height: 160,
            child: CustomScrollView(slivers: [sliver]),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      final render = tester.renderObject<RenderSliverConstrainedCrossAxis>(
        find.byType(SliverConstrainedCrossAxis).first,
      );
      expect(render.geometry!.crossAxisExtent, parent == 'nested' ? 25 : 50);
    });
  }
}
