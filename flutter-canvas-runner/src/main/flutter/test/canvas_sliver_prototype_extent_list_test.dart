import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_fill_viewport_test.dart' as viewport_fixture;

import 'canvas_sliver_fixed_extent_list_test.dart' as fixed_fixture;

const prototypeId = '0ec04b35-2b28-4dcf-bb08-82c29a9f9b18';
const prototypeChildId = '682da603-624b-4e6f-bbf2-4c7f49ea795d';
Map<String, Object?> data({
  double extent = 48,
  bool? flags,
  bool empty = false,
  String variant = 'list',
  bool reference = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
  Map<String, Object?>? count,
  bool preset = false,
  Map<String, Object?>? prototype,
}) {
  final raw = fixed_fixture.data(
    extent: extent,
    flags: flags,
    empty: empty,
    variant: variant,
    reference: reference,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
    count: count,
  );
  final sliver = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  sliver['type'] =
      'flutter.widgets.SliverPrototypeExtentList${variant == 'list' ? '' : '.$variant'}';
  final props = sliver['properties'] as Map<String, Object?>;
  props.remove('itemExtent');
  props.remove('semanticIndexOffset');
  final slots = sliver['slots'] as Map<String, Object?>;
  slots['prototypeItem'] = f.single(
    preset
        ? null
        : prototype ??
              f.node(
                prototypeId,
                'flutter.widgets.SizedBox',
                {'width': f.number(extent), 'height': f.number(extent)},
                {
                  'child': f.single(
                    f.node(prototypeChildId, 'flutter.widgets.Text', {
                      'data': {'kind': 'string', 'value': 'Measure only'},
                    }),
                  ),
                },
              ),
  );
  return raw;
}

void main() {
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'empty visual children accept box drop h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            final selected = <String>[];
            await f.pump(
              tester,
              data(
                empty: true,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              ),
              selected: selected.add,
              drop: (v) => drop = v,
            );
            final viewport = tester.getRect(find.byType(CustomScrollView));
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            CanvasDropTarget? found;
            Offset? hit;
            for (final dx in [.01, .05, .1, .5, .9, .95, .99]) {
              for (final dy in [.01, .05, .1, .5, .9, .95, .99]) {
                final point =
                    viewport.topLeft +
                    Offset(dx * viewport.width, dy * viewport.height);
                final result = drop!(
                  ((point.dx - surface.left) / surface.width * 1000000).round(),
                  ((point.dy - surface.top) / surface.height * 1000000).round(),
                  CanvasPaletteDragSource(
                    token: 'box',
                    widgetType: 'flutter.widgets.Text',
                    traits: {},
                  ),
                );
                if (result?.parentWidgetId == f.fillId) {
                  found = result;
                  hit = point;
                  break;
                }
              }
              if (found != null) break;
            }
            final render = tester.renderObject<RenderSliver>(
              find.byType(SliverPrototypeExtentList, skipOffstage: false),
            );
            expect(
              found?.parentWidgetId,
              f.fillId,
              reason:
                  'viewport=$viewport transform=${render.getTransformTo(null)} geometry=${render.geometry} constraints=${render.constraints}',
            );
            expect(found?.slotName, 'children');
            expect(found?.insertionIndex, 0);
            await tester.tapAt(hit!);
            await tester.pump();
            expect(selected, contains(f.fillId));
            final invalid = drop!(
              ((hit.dx - surface.left) / surface.width * 1000000).round(),
              ((hit.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'sliver',
                widgetType: 'flutter.widgets.SliverPrototypeExtentList',
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(invalid?.parentWidgetId, isNot(f.fillId));
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }

  test(
    'exact constructor fields and independent prototype/children slot admission',
    () {
      for (final variant in ['list', 'builder', 'delegate']) {
        final raw = data(variant: variant, empty: true, preset: true);
        final sliver = f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        );
        final props = sliver['properties'] as Map<String, Object?>;
        final slots = sliver['slots'] as Map<String, Object?>;
        expect(() => f.decode(raw), returnsNormally);
        for (final invalid in ['itemExtent', 'semanticIndexOffset']) {
          props[invalid] = {'kind': 'integer', 'value': 0};
          expect(() => f.decode(raw), throwsFormatException);
          props.remove(invalid);
        }
        for (final rejected in [
          'SliverToBoxAdapter',
          'Expanded',
          'Flexible',
          'Spacer',
        ]) {
          slots['prototypeItem'] = f.single(
            f.node(
              prototypeId,
              'flutter.widgets.$rejected',
              rejected == 'Spacer'
                  ? {
                      'flex': {'kind': 'integer', 'value': 1},
                    }
                  : {},
            ),
          );
          expect(() => f.decode(raw), throwsFormatException);
        }
        slots['prototypeItem'] = {'kind': 'list', 'children': []};
        expect(() => f.decode(raw), throwsFormatException);
        slots['prototypeItem'] = f.single(null);
        if (variant != 'list') {
          slots['children'] = {'kind': 'list', 'children': []};
          expect(() => f.decode(raw), throwsFormatException);
          slots.remove('children');
          final field = variant == 'builder' ? 'itemBuilder' : 'delegate';
          props[field] = {'kind': 'null'};
          expect(() => f.decode(raw), throwsFormatException);
          props.remove(field);
          expect(() => f.decode(raw), throwsFormatException);
          props[field] = {'kind': 'string', 'value': 'empty'};
        }
        if (variant == 'builder') {
          props['itemCount'] = {'kind': 'integer', 'value': -1};
          expect(() => f.decode(raw), throwsFormatException);
          props['itemCount'] = {'kind': 'null'};
          props['findChildIndexCallback'] = {'kind': 'null'};
        }
        expect(() => f.decode(raw), returnsNormally);
      }
    },
  );
  for (final extent in [0.0, 24.5, 400.0]) {
    for (final flags in <bool?>[null, false, true]) {
      for (final horizontal in [false, true]) {
        for (final reverse in [false, true]) {
          for (final rtl in [false, true]) {
            for (final empty in [false, true]) {
              testWidgets(
                'prototype extent=$extent flags=$flags horizontal=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
                (tester) async {
                  CanvasMovePreviewResolver? move;
                  await f.pump(
                    tester,
                    data(
                      extent: extent,
                      flags: flags,
                      horizontal: horizontal,
                      reverse: reverse,
                      rtl: rtl,
                      empty: empty,
                    ),
                    move: (v) => move = v,
                  );
                  final widget = tester.widget<SliverPrototypeExtentList>(
                    find.byType(SliverPrototypeExtentList, skipOffstage: false),
                  );
                  final render = tester
                      .renderObject<RenderSliverFixedExtentBoxAdaptor>(
                        find.byType(
                          SliverPrototypeExtentList,
                          skipOffstage: false,
                        ),
                      );
                  expect(render.itemExtent, extent);
                  final delegate = widget.delegate as SliverChildListDelegate;
                  expect(delegate.addAutomaticKeepAlives, flags ?? true);
                  expect(delegate.addRepaintBoundaries, flags ?? true);
                  expect(delegate.addSemanticIndexes, flags ?? true);
                  expect(render.geometry!.scrollExtent, empty ? 0 : 2 * extent);
                  if (!empty) {
                    final viewport = tester.renderObject<RenderBox>(
                      find.byType(CustomScrollView),
                    );
                    final body = tester.renderObject<RenderBox>(
                      find.byKey(
                        const ValueKey('canvas-widget-${f.bodyId}'),
                        skipOffstage: false,
                      ),
                    );
                    expect(
                      body.size.width,
                      closeTo(horizontal ? extent : 300, .01),
                    );
                    expect(
                      body.size.height,
                      closeTo(horizontal ? 160 : extent, .01),
                    );
                    final offset = body.localToGlobal(
                      Offset.zero,
                      ancestor: viewport,
                    );
                    final backwards = horizontal ? rtl != reverse : reverse;
                    final start = backwards
                        ? (horizontal ? 300.0 : 160.0) - extent
                        : 0.0;
                    expect(
                      horizontal ? offset.dx : offset.dy,
                      closeTo(start, .01),
                    );
                    expect(move!(f.bodyId, f.fillId, 'children', 1), isNotNull);
                    expect(move!(f.bodyId, f.fillId, 'children', 2), isNull);
                  }
                  expect(move!(f.fillId, f.fillId, 'children', 0), isNull);
                  expect(tester.takeException(), isNull);
                },
              );
            }
          }
        }
      }
    }
  }
  for (final horizontal in [false, true]) {
    testWidgets(
      'prototype extent scrolls to last item with preceding sliver h=$horizontal',
      (tester) async {
        await f.pump(
          tester,
          data(extent: 200, horizontal: horizontal, preceding: 40),
        );
        final position = tester
            .state<ScrollableState>(find.byType(Scrollable).first)
            .position;
        expect(
          position.maxScrollExtent,
          closeTo(math.max(0, 440 - (horizontal ? 300 : 160)), .01),
        );
        position.jumpTo(position.maxScrollExtent);
        await tester.pump();
        final view = tester.renderObject<RenderBox>(
          find.byType(CustomScrollView),
        );
        final last = tester.renderObject<RenderBox>(
          find.byKey(
            const ValueKey('canvas-widget-${viewport_fixture.lastId}'),
          ),
        );
        final origin = last.localToGlobal(Offset.zero, ancestor: view);
        expect(
          horizontal
              ? origin.dx + last.size.width
              : origin.dy + last.size.height,
          closeTo(horizontal ? 300 : 160, .01),
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
  for (final variant in ['builder', 'delegate']) {
    for (final ref in [false, true]) {
      for (final count in <Map<String, Object?>?>[
        null,
        {'kind': 'null'},
        {'kind': 'integer', 'value': 0},
        {'kind': 'integer', 'value': 3},
      ]) {
        testWidgets('empty/presence $variant ref=$ref count=$count', (
          tester,
        ) async {
          await f.pump(
            tester,
            data(variant: variant, reference: ref, count: count, flags: false),
          );
          final sdk = tester.widget<SliverPrototypeExtentList>(
            find.byType(SliverPrototypeExtentList, skipOffstage: false),
          );
          expect(
            tester
                .renderObject<RenderSliverFixedExtentBoxAdaptor>(
                  find.byType(SliverPrototypeExtentList, skipOffstage: false),
                )
                .itemExtent,
            48,
          );
          expect(find.text('First page'), findsNothing);
          expect(
            find.textContaining('preview limitation', skipOffstage: false),
            ref ? findsWidgets : findsNothing,
          );
          if (variant == 'builder') {
            final delegate = sdk.delegate as SliverChildBuilderDelegate;
            expect(delegate.childCount, count?['value']);
            expect(delegate.semanticIndexOffset, 0);
            expect(delegate.addAutomaticKeepAlives, isFalse);
            expect(delegate.addRepaintBoundaries, isFalse);
            expect(delegate.addSemanticIndexes, isFalse);
          }
          expect(tester.takeException(), isNull);
        });
      }
    }
  }

  for (final variant in ['list', 'builder', 'delegate']) {
    testWidgets('empty prototype uses native 48x48 preset: $variant', (
      tester,
    ) async {
      await f.pump(tester, data(preset: true, variant: variant));
      expect(
        tester
            .renderObject<RenderSliverFixedExtentBoxAdaptor>(
              find.byType(SliverPrototypeExtentList, skipOffstage: false),
            )
            .itemExtent,
        48,
      );
      expect(tester.takeException(), isNull);
    });
  }
  for (final horizontal in [false, true]) {
    testWidgets(
      'prototype is measured, not selected or targeted, and can be cleared h=$horizontal',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final selected = <String>[];
        final raw = data(
          horizontal: horizontal,
          prototype: f.node(
            prototypeId,
            'flutter.widgets.SizedBox',
            {'width': f.number(80), 'height': f.number(60)},
            {
              'child': f.single(
                f.node(prototypeChildId, 'flutter.widgets.Container'),
              ),
            },
          ),
        );
        await f.pump(
          tester,
          raw,
          selected: selected.add,
          drop: (v) => drop = v,
          move: (v) => move = v,
        );
        final render = tester.renderObject<RenderSliverFixedExtentBoxAdaptor>(
          find.byType(SliverPrototypeExtentList, skipOffstage: false),
        );
        expect(render.itemExtent, horizontal ? 80 : 60);
        final viewport = tester.getRect(find.byType(CustomScrollView));
        final surface = tester.getRect(find.byType(CanvasDocumentView));
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
            expect(
              hit?.parentWidgetId,
              isNot(isIn([prototypeId, prototypeChildId])),
            );
            expect(hit?.slotName, isNot('prototypeItem'));
            await tester.tapAt(point);
            await tester.pump();
          }
        }
        expect(selected, isNot(contains(prototypeId)));
        expect(selected, isNot(contains(prototypeChildId)));
        expect(move!(f.bodyId, prototypeChildId, 'child', 0), isNull);
        expect(
          move!(f.bodyId, f.fillId, 'prototypeItem', 0),
          isNull,
          reason: 'occupied prototype cannot be overwritten by move',
        );
        final sliver = f.findNode(
          raw['root'] as Map<String, Object?>,
          f.fillId,
        );
        (sliver['slots'] as Map<String, Object?>)['prototypeItem'] = f.single(
          null,
        );
        await f.pump(tester, raw, move: (v) => move = v);
        expect(
          tester
              .renderObject<RenderSliverFixedExtentBoxAdaptor>(
                find.byType(SliverPrototypeExtentList, skipOffstage: false),
              )
              .itemExtent,
          48,
        );
        expect(move!(f.bodyId, f.fillId, 'prototypeItem', 0), isNotNull);
        expect(tester.takeException(), isNull);
      },
    );
    testWidgets('prototype relayout responds to its own size h=$horizontal', (
      tester,
    ) async {
      final raw = data(horizontal: horizontal, extent: 24.5);
      await f.pump(tester, raw);
      final prototype = f.findNode(
        raw['root'] as Map<String, Object?>,
        prototypeId,
      );
      (prototype['properties'] as Map<String, Object?>)[horizontal
          ? 'width'
          : 'height'] = f.number(
        90.25,
      );
      await f.pump(tester, raw);
      final render = tester.renderObject<RenderSliverFixedExtentBoxAdaptor>(
        find.byType(SliverPrototypeExtentList, skipOffstage: false),
      );
      expect(render.itemExtent, 90.25);
      expect(render.geometry!.scrollExtent, 180.5);
      expect(tester.takeException(), isNull);
    });
  }
}
