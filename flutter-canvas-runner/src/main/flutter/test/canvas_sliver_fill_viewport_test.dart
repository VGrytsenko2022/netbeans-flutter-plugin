import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const lastId = '13490d5d-e1a2-42bd-b025-b63459d32b8f';
Map<String, Object?> data({
  double? fraction,
  bool? pad,
  bool? implicit,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  bool empty = false,
  bool delegate = false,
  bool reference = false,
  double preceding = 0,
  bool? delegateFlags,
}) {
  final raw = f.modelJson(
    empty: true,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  fill['type'] =
      'flutter.widgets.SliverFillViewport${delegate ? '.delegate' : ''}';
  fill['properties'] = <String, Object?>{
    if (fraction != null) 'viewportFraction': f.number(fraction),
    if (pad != null) 'padEnds': f.boolean(pad),
    if (implicit != null) 'allowImplicitScrolling': f.boolean(implicit),
    if (delegate)
      'delegate': reference
          ? {'kind': 'dartObjectReferencePresence'}
          : {'kind': 'string', 'value': 'empty'},
    if (!delegate && reference)
      'semanticIndexCallback': {'kind': 'dartObjectReferencePresence'},
    if (!delegate) 'semanticIndexOffset': {'kind': 'integer', 'value': 7},
    if (!delegate && delegateFlags != null)
      for (final flag in [
        'addAutomaticKeepAlives',
        'addRepaintBoundaries',
        'addSemanticIndexes',
      ])
        flag: f.boolean(delegateFlags),
  };
  fill['slots'] = <String, Object?>{
    if (!delegate)
      'children': {
        'kind': 'list',
        'children': <Map<String, Object?>>[
          if (!empty)
            f.node(f.bodyId, 'flutter.widgets.SizedBox', {}, {
              'child': f.single(
                f.node(
                  'db0c2928-6c32-453c-8bd2-6edb0523bb15',
                  'flutter.widgets.Text',
                  {
                    'data': {'kind': 'string', 'value': 'First page'},
                  },
                ),
              ),
            }),
          if (!empty)
            f.node(lastId, 'flutter.widgets.SizedBox', {}, {
              'child': f.single(
                f.node(
                  'dd9c0f1a-7ddd-4c96-bc14-177bbeb4e75f',
                  'flutter.widgets.Text',
                  {
                    'data': {'kind': 'string', 'value': 'Last page'},
                  },
                ),
              ),
            }),
        ],
      },
  };
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
              find.byType(SliverFillViewport, skipOffstage: false),
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
                widgetType: 'flutter.widgets.SliverFillViewport',
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
  test('typed contracts reject malformed values and sliver children', () {
    for (final delegate in [false, true]) {
      final raw = data(delegate: delegate);
      final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      final props = fill['properties'] as Map<String, Object?>;
      for (final name in [
        'padEnds',
        'allowImplicitScrolling',
        if (!delegate) 'addAutomaticKeepAlives',
        if (!delegate) 'addRepaintBoundaries',
        if (!delegate) 'addSemanticIndexes',
      ]) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'string', 'value': 'true'},
          {'kind': 'integer', 'value': 1},
        ]) {
          props[name] = value;
          expect(() => f.decode(raw), throwsFormatException);
        }
        props.remove(name);
      }
      for (final value in [0.0, -1.0]) {
        props['viewportFraction'] = f.number(value);
        expect(() => f.decode(raw), throwsFormatException);
      }
      props['viewportFraction'] = f.number(2);
      expect(() => f.decode(raw), returnsNormally);
      if (delegate) {
        props['delegate'] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove('delegate');
        expect(() => f.decode(raw), throwsFormatException);
        props['delegate'] = {'kind': 'string', 'value': 'empty'};
        fill['slots'] = {
          'children': {'kind': 'list', 'children': []},
        };
        expect(() => f.decode(raw), throwsFormatException);
        fill['slots'] = <String, Object?>{};
      } else {
        props['semanticIndexCallback'] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove('semanticIndexCallback');
        props['semanticIndexOffset'] = {'kind': 'integer', 'value': -1};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove('semanticIndexOffset');
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
      expect(() => f.decode(raw), returnsNormally);
      raw['root'] = fill;
      expect(() => f.decode(raw), throwsFormatException);
    }
  });

  for (final fraction in <double?>[null, .5, 1, 1.5]) {
    for (final pad in <bool?>[null, false, true]) {
      for (final implicit in <bool?>[null, false, true]) {
        for (final horizontal in [false, true]) {
          for (final reverse in [false, true]) {
            for (final rtl in [false, true]) {
              testWidgets(
                'native fraction=$fraction pad=$pad implicit=$implicit h=$horizontal reverse=$reverse rtl=$rtl',
                (tester) async {
                  CanvasMovePreviewResolver? move;
                  await f.pump(
                    tester,
                    data(
                      fraction: fraction,
                      pad: pad,
                      implicit: implicit,
                      horizontal: horizontal,
                      reverse: reverse,
                      rtl: rtl,
                    ),
                    move: (v) => move = v,
                  );
                  final widget = tester.widget<SliverFillViewport>(
                    find.byType(SliverFillViewport, skipOffstage: false),
                  );
                  expect(widget.viewportFraction, fraction ?? 1);
                  expect(widget.padEnds, pad ?? true);
                  expect(widget.allowImplicitScrolling, implicit ?? true);
                  final delegate = widget.delegate as SliverChildListDelegate;
                  expect(delegate.addAutomaticKeepAlives, isTrue);
                  expect(delegate.addRepaintBoundaries, isTrue);
                  expect(delegate.addSemanticIndexes, isTrue);
                  expect(delegate.semanticIndexOffset, 7);
                  final viewport = tester.renderObject<RenderBox>(
                    find.byType(CustomScrollView),
                  );
                  final body = tester.renderObject<RenderBox>(
                    find.byKey(
                      const ValueKey('canvas-widget-${f.bodyId}'),
                      skipOffstage: false,
                    ),
                  );
                  final axis = horizontal ? 300.0 : 160.0;
                  final extent = axis * (fraction ?? 1);
                  final padding = (pad ?? true)
                      ? math.max(0.0, axis - extent) / 2
                      : 0.0;
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
                  final start = backwards ? axis - padding - extent : padding;
                  expect(
                    horizontal ? offset.dx : offset.dy,
                    closeTo(start, .01),
                  );
                  expect(move!(f.bodyId, f.fillId, 'children', 1), isNotNull);
                  expect(move!(f.bodyId, f.fillId, 'children', 2), isNull);
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
    for (final fraction in [.5, 1.5]) {
      testWidgets(
        'native trailing padding and scrolling h=$horizontal fraction=$fraction',
        (tester) async {
          await f.pump(
            tester,
            data(
              horizontal: horizontal,
              fraction: fraction,
              pad: true,
              preceding: 40,
            ),
          );
          final position = tester
              .state<ScrollableState>(find.byType(Scrollable).first)
              .position;
          final length = horizontal ? 300.0 : 160.0;
          final padding = math.max(0.0, 1 - fraction) * length / 2;
          expect(
            position.maxScrollExtent,
            closeTo(
              math.max(0, 40 + 2 * fraction * length + 2 * padding - length),
              .01,
            ),
          );
          position.jumpTo(position.maxScrollExtent);
          await tester.pump();
          final viewport = tester.renderObject<RenderBox>(
            find.byType(CustomScrollView),
          );
          final last = tester.renderObject<RenderBox>(
            find.byKey(const ValueKey('canvas-widget-$lastId')),
          );
          final origin = last.localToGlobal(Offset.zero, ancestor: viewport);
          expect(
            (horizontal
                ? origin.dx + last.size.width
                : origin.dy + last.size.height),
            closeTo(length - padding, .01),
          );
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  for (final delegate in [false, true]) {
    for (final reference in [false, true]) {
      testWidgets(
        'empty/project delegate=$delegate reference=$reference has explicit preview limitations',
        (tester) async {
          await f.pump(
            tester,
            data(empty: true, delegate: delegate, reference: reference),
          );
          expect(
            find.byType(SliverFillViewport, skipOffstage: false),
            findsOneWidget,
          );
          expect(
            find.textContaining('preview limitation', skipOffstage: false),
            reference ? findsWidgets : findsNothing,
          );
          expect(find.text('First page'), findsNothing);
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  for (final flags in [false, true]) {
    testWidgets('list delegate flags=$flags reach SDK', (tester) async {
      await f.pump(tester, data(fraction: .5, delegateFlags: flags));
      final delegate =
          tester
                  .widget<SliverFillViewport>(find.byType(SliverFillViewport))
                  .delegate
              as SliverChildListDelegate;
      expect(delegate.addAutomaticKeepAlives, flags);
      expect(delegate.addRepaintBoundaries, flags);
      expect(delegate.addSemanticIndexes, flags);
      expect(delegate.semanticIndexOffset, 7);
      expect(delegate.semanticIndexCallback(const SizedBox(), 3), 3);
      expect(tester.takeException(), isNull);
    });
  }
}
