import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const nestedId = '13490d5d-e1a2-42bd-b025-b63459d32b8f';
const firstId = 'd1bd3b95-afce-4df7-b4df-158a5e1c781a';
const secondId = 'e729bdb9-b883-4feb-8edb-a6c331e838b5';
const thirdId = 'b2a46116-260a-4ba2-9521-acb2cde2fb3c';
const lastBoxId = '79e87923-ebed-40ac-86c2-a8cfba0e40e8';
Map<String, Object?> list(List<Map<String, Object?>> children) => {
  'kind': 'list',
  'children': children,
};
Map<String, Object?> group(String id, List<Map<String, Object?>> children) =>
    f.node(id, 'flutter.widgets.SliverCrossAxisGroup', {}, {
      'slivers': list(children),
    });
Map<String, Object?> adapter(String id, String boxId, double extent) =>
    f.node(id, 'flutter.widgets.SliverToBoxAdapter', {}, {
      'child': f.single(
        f.node(boxId, 'flutter.widgets.SizedBox', {
          'width': f.number(extent),
          'height': f.number(extent),
        }),
      ),
    });
Map<String, Object?> data({
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  bool empty = false,
  bool nested = false,
  double extent = 48,
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
  final children = <Map<String, Object?>>[
    adapter(firstId, f.bodyId, extent),
    if (nested)
      group(nestedId, [adapter(secondId, lastBoxId, extent * 2)])
    else
      adapter(secondId, lastBoxId, extent * 2),
  ];
  fill.addAll(group(f.fillId, empty ? [] : children));
  return raw;
}

void main() {
  testWidgets(
    'stable group keys refresh order, nested ownership, removal and empty geometry',
    (tester) async {
      final raw = data(nested: true, extent: 30);
      final outer = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      final slots = outer['slots'] as Map;
      final children = (slots['slivers'] as Map)['children'] as List;
      CanvasMovePreviewResolver? move;
      Future<void> show(double extent, double firstOffset) async {
        await f.pump(tester, raw, move: (value) => move = value);
        final groupFinder = find.byType(
          SliverCrossAxisGroup,
          skipOffstage: false,
        );
        final render = tester.renderObject<RenderSliverCrossAxisGroup>(
          groupFinder.first,
        );
        expect(render.geometry!.scrollExtent, closeTo(extent, .01));
        if (extent > 0) {
          final box = tester.renderObject<RenderBox>(
            find.byKey(
              const ValueKey('canvas-widget-${f.bodyId}'),
              skipOffstage: false,
            ),
          );
          final viewport = tester.renderObject<RenderBox>(
            find.byType(CustomScrollView),
          );
          expect(
            box.localToGlobal(Offset.zero, ancestor: viewport).dx,
            closeTo(firstOffset, .01),
          );
        }
        expect(tester.takeException(), isNull);
      }

      await show(60, 0);
      final first = children.removeAt(0) as Map<String, Object?>;
      children.add(first);
      await show(60, 150);
      final nested = children.first as Map<String, Object?>;
      final nestedChildren =
          ((nested['slots'] as Map)['slivers'] as Map)['children'] as List;
      children.remove(first);
      nestedChildren.add(first);
      await show(60, 150);
      expect(move!(firstId, f.fillId, 'slivers', 1), isNotNull);
      nestedChildren.removeAt(0);
      await show(30, 0);
      expect(
        find.byKey(
          const ValueKey('canvas-widget-$lastBoxId'),
          skipOffstage: false,
        ),
        findsNothing,
      );
      children.clear();
      await show(0, 0);
      expect(
        find.byType(SliverCrossAxisGroup, skipOffstage: false),
        findsOneWidget,
      );
      expect(
        move!(firstId, f.fillId, 'slivers', 0),
        isNull,
        reason: 'Removed widget has no stale move handle.',
      );
    },
  );
  test(
    'exact required sliver-list schema: empty and nested valid, boxes and unknown fields rejected',
    () {
      final raw = data(empty: true);
      final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      expect(() => f.decode(raw), returnsNormally);
      node['slots'] = <String, Object?>{};
      expect(() => f.decode(raw), throwsFormatException);
      for (final slot in [
        f.single(null),
        list([
          f.node(f.bodyId, 'flutter.widgets.Text', {
            'data': {'kind': 'string', 'value': 'bad'},
          }),
        ]),
      ]) {
        node['slots'] = {'slivers': slot};
        expect(() => f.decode(raw), throwsFormatException);
      }
      node['slots'] = {
        'slivers': list([group(nestedId, [])]),
      };
      expect(() => f.decode(raw), returnsNormally);
      for (final name in ['scrollDirection', 'reverse', 'children', 'onTap']) {
        node['properties'] = {name: f.boolean(true)};
        expect(() => f.decode(raw), throwsFormatException);
      }
      node['properties'] = <String, Object?>{};
      raw['root'] = node;
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'mixed main-axis lanes have disjoint drop zones h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            final raw = data(
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
            );
            final outer = f.findNode(
              raw['root'] as Map<String, Object?>,
              f.fillId,
            );
            Map<String, Object?> lane(String id, Map<String, Object?> child) =>
                f.node(id, 'flutter.widgets.SliverMainAxisGroup', {}, {
                  'slivers': list([child]),
                });
            final left = lane(nestedId, adapter(firstId, f.bodyId, 96));
            final right = lane(thirdId, adapter(secondId, lastBoxId, 144));
            outer['slots'] = {
              'slivers': list([left, right]),
            };
            CanvasMovePreviewResolver? move;
            CanvasDropResolver? drop;
            Future<void> show() => f.pump(
              tester,
              raw,
              move: (value) => move = value,
              drop: (value) => drop = value,
            );
            await show();
            expect(
              tester
                  .renderObject<RenderSliverCrossAxisGroup>(
                    find.byType(SliverCrossAxisGroup),
                  )
                  .geometry!
                  .scrollExtent,
              144,
            );
            final leftZone = move!(secondId, nestedId, 'slivers', 1)!.zone!;
            final rightZone = move!(firstId, thirdId, 'slivers', 1)!.zone!;
            expect(leftZone.isEmpty, isFalse);
            expect(rightZone.isEmpty, isFalse);
            if (horizontal) {
              expect(
                leftZone.bottomMicros,
                lessThanOrEqualTo(rightZone.topMicros + 1),
              );
            } else {
              expect(
                leftZone.rightMicros,
                lessThanOrEqualTo(rightZone.leftMicros + 1),
              );
            }
            for (final entry in {
              nestedId: leftZone,
              thirdId: rightZone,
            }.entries) {
              final zone = entry.value;
              final target = drop!(
                (zone.leftMicros + zone.rightMicros) ~/ 2,
                (zone.topMicros + zone.bottomMicros) ~/ 2,
                CanvasPaletteDragSource(
                  token: 'cross-lane',
                  widgetType: 'flutter.widgets.SliverCrossAxisGroup',
                  traits: {canvasSliverWidgetTrait},
                ),
              );
              expect(target?.parentWidgetId, entry.key);
              expect(target?.slotName, 'slivers');
              expect(target?.insertionIndex, 1);
            }
            final rightChildren =
                ((right['slots'] as Map)['slivers'] as Map)['children'] as List;
            final outerChildren =
                ((outer['slots'] as Map)['slivers'] as Map)['children'] as List;
            final moved = rightChildren.removeAt(0);
            outerChildren.add(moved);
            await show();
            expect(
              tester.takeException(),
              isNull,
              reason:
                  'Main-axis to cross-axis reparenting resets native parent data.',
            );
            final movedBox = tester.renderObject<RenderBox>(
              find.byKey(
                const ValueKey('canvas-widget-$lastBoxId'),
                skipOffstage: false,
              ),
            );
            expect(
              horizontal ? movedBox.size.height : movedBox.size.width,
              closeTo((horizontal ? 160 : 300) / 3, .01),
            );
            outerChildren.remove(moved);
            rightChildren.add(moved);
            await show();
            expect(tester.takeException(), isNull);
            (left['slots'] as Map)['slivers'] = list([]);
            await show();
            final empty = move!(secondId, nestedId, 'slivers', 0);
            expect(empty, isNotNull);
            expect(
              move!(firstId, thirdId, 'slivers', 1),
              isNull,
              reason: 'Removed children cannot retain stale handles.',
            );
            expect(tester.takeException(), isNull);
          },
        );
        for (final nested in [false, true]) {
          for (final extent in [0.0, 24.5, 200.0]) {
            testWidgets(
              'native group geometry h=$horizontal reverse=$reverse rtl=$rtl nested=$nested extent=$extent',
              (tester) async {
                CanvasMovePreviewResolver? move;
                await f.pump(
                  tester,
                  data(
                    horizontal: horizontal,
                    reverse: reverse,
                    rtl: rtl,
                    nested: nested,
                    extent: extent,
                  ),
                  move: (value) => move = value,
                );
                final groups = find.byType(
                  SliverCrossAxisGroup,
                  skipOffstage: false,
                );
                expect(groups, findsNWidgets(nested ? 2 : 1));
                final render = tester.renderObject<RenderSliverCrossAxisGroup>(
                  groups.first,
                );
                expect(render.geometry!.scrollExtent, closeTo(2 * extent, .01));
                final viewport = tester.renderObject<RenderBox>(
                  find.byType(CustomScrollView),
                );
                final box = tester.renderObject<RenderBox>(
                  find.byKey(
                    const ValueKey('canvas-widget-${f.bodyId}'),
                    skipOffstage: false,
                  ),
                );
                expect(
                  horizontal ? box.size.width : box.size.height,
                  closeTo(extent, .01),
                );
                expect(
                  horizontal ? box.size.height : box.size.width,
                  horizontal ? 80 : 150,
                );
                final origin = box.localToGlobal(
                  Offset.zero,
                  ancestor: viewport,
                );
                final backwards = horizontal ? rtl != reverse : reverse;
                expect(
                  horizontal ? origin.dx : origin.dy,
                  closeTo(
                    backwards
                        ? (horizontal ? 300 : 160) -
                              math.min(2 * extent, horizontal ? 300 : 160) +
                              math.min(extent, horizontal ? 300 : 160) -
                              extent
                        : 0,
                    .01,
                  ),
                );
                expect(move!(firstId, f.fillId, 'slivers', 1), isNotNull);
                expect(move!(firstId, f.fillId, 'slivers', 2), isNull);
                expect(move!(f.bodyId, f.fillId, 'slivers', 0), isNull);
                expect(move!(f.fillId, f.fillId, 'slivers', 0), isNull);
                if (nested) {
                  expect(
                    move!(firstId, nestedId, 'slivers', 1),
                    isNotNull,
                    reason: 'Nested groups occupy their own cross-axis lane.',
                  );
                  expect(move!(f.fillId, nestedId, 'slivers', 0), isNull);
                }
                final position = tester
                    .state<ScrollableState>(find.byType(Scrollable).first)
                    .position;
                expect(
                  position.maxScrollExtent,
                  closeTo(
                    math.max(0, 2 * extent - (horizontal ? 300 : 160)),
                    .01,
                  ),
                );
                position.jumpTo(position.maxScrollExtent);
                await tester.pump();
                if (nested) {
                  expect(
                    move!(firstId, nestedId, 'slivers', 1),
                    isNotNull,
                    reason:
                        'The scrolled-into-view group regains its drop handle.',
                  );
                }
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
        testWidgets(
          'empty group is selectable and accepts only slivers h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            final selected = <String>[];
            await f.pump(
              tester,
              data(
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
                empty: true,
              ),
              drop: (value) => drop = value,
              selected: selected.add,
            );
            final viewport = tester.getRect(find.byType(CustomScrollView));
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            CanvasDropTarget? target;
            Offset? hit;
            CanvasDropTarget? at(Offset p, String type, Set<String> traits) =>
                drop!(
                  ((p.dx - surface.left) / surface.width * 1000000).round(),
                  ((p.dy - surface.top) / surface.height * 1000000).round(),
                  CanvasPaletteDragSource(
                    token: 'test',
                    widgetType: type,
                    traits: traits,
                  ),
                );
            for (final x in [.01, .05, .5, .95, .99]) {
              for (final y in [.01, .05, .5, .95, .99]) {
                final point =
                    viewport.topLeft +
                    Offset(x * viewport.width, y * viewport.height);
                final candidate = at(
                  point,
                  'flutter.widgets.SliverCrossAxisGroup',
                  {canvasSliverWidgetTrait},
                );
                if (candidate?.parentWidgetId == f.fillId) {
                  target = candidate;
                  hit = point;
                  break;
                }
              }
              if (hit != null) break;
            }
            expect(target?.parentWidgetId, f.fillId);
            expect(target?.slotName, 'slivers');
            expect(target?.insertionIndex, 0);
            expect(
              at(hit!, 'flutter.widgets.Text', {})?.parentWidgetId,
              isNot(f.fillId),
            );
            await tester.tapAt(hit);
            await tester.pump();
            expect(selected, contains(f.fillId));
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
}
