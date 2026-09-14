import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.SliverResizingHeader';
const minId = '2e1a6b24-8a31-4cf5-b22a-b5af13c0471c';
const maxId = '2347e302-8521-4fdb-a349-ff7c0c9a2c9c';
const minChildId = '827ebc19-c502-4390-9be0-76e5ba43214e';
const maxChildId = '9c85163b-b85c-41df-9776-2d00ee8e366b';
Map<String, Object?> box(String id, double extent, {String? childId}) => f.node(
  id,
  'flutter.widgets.SizedBox',
  {'width': f.number(extent), 'height': f.number(extent)},
  {
    if (childId != null)
      'child': f.single(f.node(childId, 'flutter.widgets.Container')),
  },
);
Map<String, Object?> data({
  int mask = 7,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double min = 24,
  double max = 120,
  double child = 80,
  bool tail = false,
}) {
  final raw = f.modelJson(
    empty: true,
    preceding: 0,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
  );
  final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  node['type'] = type;
  node['properties'] = <String, Object?>{};
  node['slots'] = <String, Object?>{
    'minExtentPrototype': f.single(
      mask & 1 != 0 ? box(minId, min, childId: minChildId) : null,
    ),
    'maxExtentPrototype': f.single(
      mask & 2 != 0 ? box(maxId, max, childId: maxChildId) : null,
    ),
    'child': f.single(mask & 4 != 0 ? box(f.bodyId, child) : null),
  };
  if (tail) {
    final viewport = f.findNode(
      raw['root'] as Map<String, Object?>,
      '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
    );
    (((viewport['slots'] as Map)['slivers'] as Map)['children'] as List).add(
      f.node(f.headerId, 'flutter.widgets.SliverToBoxAdapter', {}, {
        'child': f.single(box('401dfb33-70b4-48f0-a354-bd329f0a0109', 1000)),
      }),
    );
  }
  return raw;
}

void main() {
  test(
    'three optional box slots, no scalar escape hatch and sliver-only placement',
    () {
      for (final name in ['delegate', 'pinned', 'minExtent', 'maxExtent']) {
        final raw = data();
        f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        )['properties'] = {
          name: f.boolean(true),
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in [
        'minExtentPrototype',
        'maxExtentPrototype',
        'child',
      ]) {
        final raw = data();
        final slots =
            f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['slots']
                as Map;
        slots[name] = f.single(
          f.node(minId, 'flutter.widgets.SliverToBoxAdapter'),
        );
        expect(() => f.decode(raw), throwsFormatException);
      }
      expect(canvasDropSlotsForWidgetType(type).map((s) => s.slotName), [
        'child',
        'minExtentPrototype',
        'maxExtentPrototype',
      ]);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (var mask = 0; mask < 8; mask++) {
          testWidgets(
            'exact nullable prototype geometry mask=$mask h=$horizontal rev=$reverse rtl=$rtl',
            (tester) async {
              final raw = data(
                mask: mask,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
                tail: true,
              );
              await f.pump(tester, raw);
              final finder = find.byType(
                SliverResizingHeader,
                skipOffstage: false,
              );
              final native = tester.widget<SliverResizingHeader>(finder);
              expect(native.minExtentPrototype != null, mask & 1 != 0);
              expect(native.maxExtentPrototype != null, mask & 2 != 0);
              expect(native.child != null, mask & 4 != 0);
              final render = tester.renderObject<RenderSliver>(finder);
              final max = mask & 2 != 0
                  ? 120.0
                  : mask & 4 != 0
                  ? 80.0
                  : 0.0;
              final min = mask & 1 != 0 ? 24.0 : 0.0;
              expect(render.geometry!.scrollExtent, max);
              expect(render.geometry!.maxScrollObstructionExtent, min);
              final position = Scrollable.of(tester.element(finder)).position;
              position.jumpTo(200);
              await tester.pump();
              expect(render.geometry!.paintExtent, min);
              expect(render.geometry!.layoutExtent, 0);
              position.jumpTo(0);
              await tester.pump();
              await f.pump(tester, raw);
              expect(identical(render, tester.renderObject(finder)), isTrue);
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
    testWidgets(
      'hidden prototype geometry does not intercept selection/drop h=$horizontal',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final selected = <String>[];
        final raw = data(mask: 3, horizontal: horizontal);
        await f.pump(
          tester,
          raw,
          selected: selected.add,
          drop: (v) => drop = v,
          move: (v) => move = v,
        );
        final viewport = tester.getRect(find.byType(CustomScrollView));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final hits = <String>[];
        for (final dx in [.01, .2, .5, .99]) {
          for (final dy in [.01, .2, .5, .99]) {
            final point =
                viewport.topLeft +
                Offset(viewport.width * dx, viewport.height * dy);
            final hit = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'test',
                widgetType: 'flutter.widgets.Text',
                traits: {},
              ),
            );
            if (hit != null) hits.add(hit.slotName);
            expect(
              hit?.parentWidgetId,
              isNot(isIn([minId, maxId, minChildId, maxChildId])),
            );
            expect(
              hit?.slotName,
              isNot(isIn(['minExtentPrototype', 'maxExtentPrototype'])),
            );
            await tester.tapAt(point);
            await tester.pump();
          }
        }
        expect(hits, contains('child'));
        expect(
          selected.where([minId, maxId, minChildId, maxChildId].contains),
          isEmpty,
        );
        expect(move!(f.headerId, minChildId, 'child', 0), isNull);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets(
      'prototype edits relayout retained header and no min-max guess is imposed h=$horizontal',
      (tester) async {
        final raw = data(horizontal: horizontal, min: 100, max: 40);
        await f.pump(tester, raw);
        final finder = find.byType(SliverResizingHeader, skipOffstage: false);
        final render = tester.renderObject<RenderSliver>(finder);
        expect(render.geometry!.scrollExtent, 40);
        expect(render.geometry!.maxScrollObstructionExtent, 100);
        final prototype = f.findNode(
          raw['root'] as Map<String, Object?>,
          maxId,
        );
        (prototype['properties'] as Map)[horizontal ? 'width' : 'height'] = f
            .number(160);
        await f.pump(tester, raw);
        expect(identical(render, tester.renderObject(finder)), isTrue);
        expect(render.geometry!.scrollExtent, 160);
        expect(tester.takeException(), isNull);
      },
    );
  }
}
