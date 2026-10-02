import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.PinnedHeaderSliver';
const tailId = 'd861d748-6f1a-4c72-aad8-150a3aefcf3b';
const tailBoxId = '26932213-8a9e-4c36-8af2-1eec938f6da9';
const contentId = 'f70895d4-aa16-4452-b7ee-6f2d5a349729';
Map<String, Object?> data({
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double extent = 48,
  double preceding = 0,
}) {
  final raw = f.modelJson(
    empty: empty,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    extent: extent,
    preceding: preceding,
  );
  final header = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  header['type'] = type;
  header['properties'] = <String, Object?>{};
  if (!empty) {
    final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
    child['slots'] = <String, Object?>{};
  }
  final viewport = f.findNode(
    raw['root'] as Map<String, Object?>,
    '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
  );
  (((viewport['slots'] as Map)['slivers'] as Map)['children'] as List).add(
    f.node(tailId, 'flutter.widgets.SliverToBoxAdapter', {}, {
      'child': f.single(
        f.node(tailBoxId, 'flutter.widgets.SizedBox', {
          'width': f.number(1000),
          'height': f.number(1000),
        }),
      ),
    }),
  );
  return raw;
}

void main() {
  test(
    'complete nullable Child schema, strict box placement and no invented scalar arguments',
    () {
      for (final name in [
        'delegate',
        'pinned',
        'floating',
        'minExtent',
        'maxExtent',
      ]) {
        final raw = data();
        f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        )['properties'] = {
          name: f.boolean(true),
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data(empty: true);
      final node = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      expect(() => f.decode(raw), returnsNormally);
      node['slots'] = <String, Object?>{};
      expect(() => f.decode(raw), returnsNormally);
      node['slots'] = {
        'child': {'kind': 'list', 'children': []},
      };
      expect(() => f.decode(raw), throwsFormatException);
      node['slots'] = {
        'child': f.single(
          f.node(f.bodyId, 'flutter.widgets.SliverToBoxAdapter'),
        ),
      };
      expect(() => f.decode(raw), throwsFormatException);
      node['slots'] = {'child': f.single(null)};
      raw['root'] = node;
      expect(() => f.decode(raw), throwsFormatException);
      expect(canvasDropSlotsForWidgetType(type).map((s) => s.slotName), [
        'child',
      ]);
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        for (final extent in [null, 0.0, 48.0, 400.0]) {
          testWidgets(
            'native pinned geometry extent=$extent h=$horizontal reverse=$reverse rtl=$rtl',
            (tester) async {
              final raw = data(
                empty: extent == null,
                extent: extent ?? 0,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              );
              await f.pump(tester, raw);
              final finder = find.byType(
                PinnedHeaderSliver,
                skipOffstage: false,
              );
              expect(
                tester.widget<PinnedHeaderSliver>(finder).child == null,
                extent == null,
              );
              final render = tester.renderObject<RenderSliver>(finder);
              final position = Scrollable.of(tester.element(finder)).position;
              final size = extent ?? 0;
              for (final offset in [0.0, 24.0, 500.0, 0.0]) {
                position.jumpTo(offset);
                await tester.pump();
                expect(render.geometry!.scrollExtent, size);
                expect(render.geometry!.maxScrollObstructionExtent, size);
                expect(
                  render.geometry!.paintExtent,
                  math.min(size, horizontal ? 300 : 160),
                );
                expect(
                  render.geometry!.layoutExtent,
                  math.max(0, size - offset).clamp(0, horizontal ? 300 : 160),
                );
                expect(tester.takeException(), isNull);
              }
              if (extent != null) {
                final child = f.findNode(
                  raw['root'] as Map<String, Object?>,
                  f.bodyId,
                );
                (child['properties'] as Map)[horizontal ? 'width' : 'height'] =
                    f.number(64);
                await f.pump(tester, raw);
                expect(identical(render, tester.renderObject(finder)), isTrue);
                expect(render.geometry!.scrollExtent, 64);
                expect(render.geometry!.maxScrollObstructionExtent, 64);
              }
            },
          );
        }
        testWidgets(
          'pinned painted Child stays selectable and accepts precise drop after scroll h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            CanvasMovePreviewResolver? move;
            final selected = <String>[];
            final raw = data(
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
              preceding: 40,
            );
            await f.pump(
              tester,
              raw,
              selected: selected.add,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            final finder = find.byType(PinnedHeaderSliver);
            Scrollable.of(tester.element(finder)).position.jumpTo(100);
            await tester.pump();
            final rect = tester.getRect(
              find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
            );
            final viewport = tester.getRect(find.byType(CustomScrollView));
            expect(viewport.overlaps(rect), isTrue);
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final point = rect.intersect(viewport).center;
            final hit = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'test',
                widgetType: 'flutter.widgets.Text',
                traits: {},
              ),
            );
            expect(hit?.parentWidgetId, f.bodyId);
            expect(hit?.slotName, 'child');
            await tester.tapAt(point);
            await tester.pump();
            expect(selected, contains(f.bodyId));
            expect(move!(tailBoxId, f.bodyId, 'child', 0), isNotNull);
            final sliverHit = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'sliver',
                widgetType: type,
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(sliverHit?.parentWidgetId, isNot(f.bodyId));
            expect(tester.takeException(), isNull);
          },
        );
        testWidgets(
          'empty header keeps exact Child drop handle h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            CanvasMovePreviewResolver? move;
            final raw = data(
              empty: true,
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
            );
            await f.pump(
              tester,
              raw,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            // A following empty SizedBox is a deeper valid box destination.
            // Fill it before testing the otherwise empty header's pointer target.
            f.findNode(
              raw['root'] as Map<String, Object?>,
              tailBoxId,
            )['slots'] = {
              'child': f.single(
                f.node(contentId, 'flutter.widgets.Text', {
                  'data': {'kind': 'string', 'value': 'Following item'},
                }),
              ),
            };
            await f.pump(
              tester,
              raw,
              drop: (v) => drop = v,
              move: (v) => move = v,
            );
            final preview = move!(tailBoxId, f.fillId, 'child', 0);
            expect(move!(tailId, f.fillId, 'child', 0), isNull);
            expect(preview, isNotNull);
            final zone = preview!.zone!;
            expect(zone.isEmpty, isFalse);
            final hit = drop!(
              (zone.leftMicros + zone.rightMicros) ~/ 2,
              (zone.topMicros + zone.bottomMicros) ~/ 2,
              CanvasPaletteDragSource(
                token: 'test',
                widgetType: 'flutter.widgets.Text',
                traits: {},
              ),
            );
            expect(hit?.parentWidgetId, f.fillId);
            expect(hit?.slotName, 'child');
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets('native semantics retain child label in pinned header', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    try {
      final raw = data();
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
        'child': f.single(
          f.node(contentId, 'flutter.widgets.Text', {
            'data': {'kind': 'string', 'value': 'Pinned title'},
          }),
        ),
      };
      await f.pump(tester, raw);
      final finder = find.byType(PinnedHeaderSliver);
      Scrollable.of(tester.element(finder)).position.jumpTo(100);
      await tester.pump();

      expect(find.bySemanticsLabel(RegExp('Pinned title')), findsWidgets);
      expect(tester.takeException(), isNull);
    } finally {
      semantics.dispose();
    }
  });
}
