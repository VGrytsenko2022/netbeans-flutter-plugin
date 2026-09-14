import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_fill_viewport_test.dart' as viewport_fixture;

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
}) {
  final raw = viewport_fixture.data(
    empty: empty,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  fill['type'] =
      'flutter.widgets.SliverFixedExtentList${variant == 'list' ? '' : '.$variant'}';
  fill['properties'] = <String, Object?>{
    'itemExtent': f.number(extent),
    if (variant != 'delegate' && flags != null)
      for (final name in [
        'addAutomaticKeepAlives',
        'addRepaintBoundaries',
        'addSemanticIndexes',
      ])
        name: f.boolean(flags),
    if (variant == 'builder') ...{
      'itemBuilder': reference
          ? {'kind': 'dartObjectReferencePresence'}
          : {'kind': 'string', 'value': 'empty'},
      'itemCount': ?count,
      'semanticIndexOffset': {'kind': 'integer', 'value': 4},
    },
    if (variant == 'delegate')
      'delegate': reference
          ? {'kind': 'dartObjectReferencePresence'}
          : {'kind': 'string', 'value': 'empty'},
  };
  if (variant != 'list') fill['slots'] = <String, Object?>{};
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
              find.byType(SliverFixedExtentList, skipOffstage: false),
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
                widgetType: 'flutter.widgets.SliverFixedExtentList',
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
    'exact native schema, required extent, preset/null and slot admission',
    () {
      for (final variant in ['list', 'builder', 'delegate']) {
        final raw = data(variant: variant, empty: true);
        final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
        final props = fill['properties'] as Map<String, Object?>;
        for (final value in [
          {'kind': 'null'},
          f.number(-1),
          {'kind': 'integer', 'value': 48},
          {'kind': 'string', 'value': '48'},
        ]) {
          props['itemExtent'] = value;
          expect(() => f.decode(raw), throwsFormatException);
        }
        props.remove('itemExtent');
        expect(() => f.decode(raw), throwsFormatException);
        props['itemExtent'] = f.number(0);
        expect(() => f.decode(raw), returnsNormally);
        if (variant != 'delegate') {
          for (final flag in [
            'addAutomaticKeepAlives',
            'addRepaintBoundaries',
            'addSemanticIndexes',
          ]) {
            props[flag] = {'kind': 'null'};
            expect(() => f.decode(raw), throwsFormatException);
            props[flag] = f.boolean(false);
            expect(() => f.decode(raw), returnsNormally);
            props.remove(flag);
          }
        }
        if (variant == 'builder') {
          for (final field in ['itemCount', 'findChildIndexCallback']) {
            props[field] = {'kind': 'null'};
            expect(() => f.decode(raw), returnsNormally);
            props.remove(field);
          }
          props['itemCount'] = {'kind': 'integer', 'value': -1};
          expect(() => f.decode(raw), throwsFormatException);
          props.remove('itemCount');
          props['semanticIndexOffset'] = {'kind': 'integer', 'value': -1};
          expect(() => f.decode(raw), throwsFormatException);
          props.remove('semanticIndexOffset');
        }
        if (variant != 'list') {
          final field = variant == 'builder' ? 'itemBuilder' : 'delegate';
          for (final value in [
            {'kind': 'null'},
            {'kind': 'string', 'value': 'wrong'},
          ]) {
            props[field] = value;
            expect(() => f.decode(raw), throwsFormatException);
          }
          props.remove(field);
          expect(() => f.decode(raw), throwsFormatException);
          props[field] = {'kind': 'string', 'value': 'empty'};
          fill['slots'] = {
            'children': {'kind': 'list', 'children': []},
          };
          expect(() => f.decode(raw), throwsFormatException);
          fill['slots'] = <String, Object?>{};
        } else {
          fill['slots'] = {
            'children': {
              'kind': 'list',
              'children': [
                f.node(f.bodyId, 'flutter.widgets.SliverToBoxAdapter'),
              ],
            },
          };
          expect(() => f.decode(raw), throwsFormatException);
          fill['slots'] = {
            'children': {'kind': 'list', 'children': []},
          };
        }
        raw['root'] = fill;
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final extent in [0.0, 48.0, 400.0]) {
    for (final flags in <bool?>[null, false, true]) {
      for (final horizontal in [false, true]) {
        for (final reverse in [false, true]) {
          for (final rtl in [false, true]) {
            for (final empty in [false, true]) {
              testWidgets(
                'fixed extent=$extent flags=$flags horizontal=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
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
                  final widget = tester.widget<SliverFixedExtentList>(
                    find.byType(SliverFixedExtentList, skipOffstage: false),
                  );
                  final render = tester
                      .renderObject<RenderSliverFixedExtentList>(
                        find.byType(SliverFixedExtentList, skipOffstage: false),
                      );
                  expect(widget.itemExtent, extent);
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
      'fixed extent scrolls to last item with preceding sliver h=$horizontal',
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
          final sdk = tester.widget<SliverFixedExtentList>(
            find.byType(SliverFixedExtentList, skipOffstage: false),
          );
          expect(sdk.itemExtent, 48);
          expect(find.text('First page'), findsNothing);
          expect(
            find.textContaining('preview limitation', skipOffstage: false),
            ref ? findsWidgets : findsNothing,
          );
          if (variant == 'builder') {
            final delegate = sdk.delegate as SliverChildBuilderDelegate;
            expect(delegate.childCount, count?['value']);
            expect(delegate.semanticIndexOffset, 4);
            expect(delegate.addAutomaticKeepAlives, isFalse);
            expect(delegate.addRepaintBoundaries, isFalse);
            expect(delegate.addSemanticIndexes, isFalse);
          }
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
}
