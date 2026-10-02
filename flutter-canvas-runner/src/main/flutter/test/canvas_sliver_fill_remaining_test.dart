import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_model_test.dart' as fixture;

const fillId = 'e4d795fb-b211-4549-83ed-938a7c280d60';
const bodyId = '3bb99bf4-983e-4fbe-8f4d-349f115c0307';
const headerId = 'c37cb388-31b5-4754-821a-2a8518b3865b';
Map<String, Object?> node(
  String id,
  String type, [
  Map<String, Object?> properties = const {},
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> single(Map<String, Object?>? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> boolean(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> number(double value) => {'kind': 'double', 'value': value};

Map<String, Object?> modelJson({
  bool? scroll,
  bool? overscroll,
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 40,
  double extent = 48,
  bool bouncing = false,
  Map<String, Object?>? child,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  final fill = node(
    fillId,
    'flutter.widgets.SliverFillRemaining',
    {
      if (scroll != null) 'hasScrollBody': boolean(scroll),
      if (overscroll != null) 'fillOverscroll': boolean(overscroll),
    },
    {
      'child': single(
        empty
            ? null
            : child ??
                  node(
                    bodyId,
                    'flutter.widgets.SizedBox',
                    {'width': number(extent), 'height': number(extent)},
                    {
                      'child': single(
                        node(
                          '325ac58f-fc3e-4900-809a-16f1a47a9d4d',
                          'flutter.widgets.Text',
                          {
                            'data': {'kind': 'string', 'value': 'Fill body'},
                          },
                        ),
                      ),
                    },
                  ),
      ),
    },
  );
  final viewport = node(
    '82890f1f-a16d-4dc3-ac0c-12bb1b2698c8',
    'flutter.widgets.CustomScrollView',
    {
      'primary': boolean(false),
      'reverse': boolean(reverse),
      'scrollDirection': {
        'kind': 'enum',
        'type': 'Axis',
        'value': horizontal ? 'horizontal' : 'vertical',
      },
      if (bouncing) 'physics': {'kind': 'string', 'value': 'bouncing'},
    },
    {
      'slivers': {
        'kind': 'list',
        'children': [
          if (preceding > 0)
            node(headerId, 'flutter.widgets.SliverToBoxAdapter', {}, {
              'child': single(
                node(
                  'e5b7e58d-77c1-4af2-9dad-813287fe3f82',
                  'flutter.widgets.SizedBox',
                  {horizontal ? 'width' : 'height': number(preceding)},
                ),
              ),
            }),
          fill,
        ],
      },
    },
  );
  model['root'] = node(
    'cdb52e37-7b98-4ffb-afda-e31b8a1ea3d3',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {
      'child': single(
        node(
          '2abeb694-fd0b-4df0-80ee-7f53969e822b',
          'flutter.widgets.Center',
          {},
          {
            'child': single(
              node(
                'b5fef1dd-0126-4f98-beb0-4ec5effc8b08',
                'flutter.widgets.SizedBox',
                {'width': number(300), 'height': number(160)},
                {'child': single(viewport)},
              ),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

CanvasModel decode(Map<String, Object?> raw) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(raw))));
Map<String, Object?> findNode(Map<String, Object?> root, String id) {
  if (root['id'] == id) return root;
  for (final slot in (root['slots'] as Map).values.cast<Map>()) {
    for (final child in [
      if (slot['child'] != null) slot['child'],
      ...?slot['children'] as List?,
    ]) {
      try {
        return findNode(child as Map<String, Object?>, id);
      } on StateError {
        /* next subtree */
      }
    }
  }
  throw StateError('Missing $id');
}

Future<void> pump(
  WidgetTester tester,
  Map<String, Object?> raw, {
  ValueChanged<String>? selected,
  ValueChanged<CanvasDropResolver?>? drop,
  ValueChanged<CanvasMovePreviewResolver?>? move,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: decode(raw),
      selectedWidgetId: fillId,
      onSelected: selected ?? (_) {},
      onDropResolverChanged: drop,
      onMovePreviewResolverChanged: move,
    ),
  );
  await tester.pump();
}

void main() {
  test(
    'SliverFillRemaining schema rejects null/invalid booleans and sliver children',
    () {
      final raw = modelJson(empty: true);
      final fill = findNode(raw['root'] as Map<String, Object?>, fillId);
      for (final field in ['hasScrollBody', 'fillOverscroll']) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'string', 'value': 'true'},
          {'kind': 'integer', 'value': 1},
        ]) {
          fill['properties'] = {field: value};
          expect(() => decode(raw), throwsFormatException);
        }
      }
      fill['properties'] = <String, Object?>{};
      expect(() => decode(raw), returnsNormally);
      fill['slots'] = {
        'child': single(node(bodyId, 'flutter.widgets.SliverToBoxAdapter')),
      };
      expect(() => decode(raw), throwsFormatException);
      fill['slots'] = {
        'child': {'kind': 'list', 'children': []},
      };
      expect(() => decode(raw), throwsFormatException);
      fill['slots'] = <String, Object?>{};
      expect(() => decode(raw), returnsNormally);
      raw['root'] = fill;
      expect(() => decode(raw), throwsFormatException);
    },
  );

  for (final scroll in [null, false, true]) {
    for (final overscroll in [null, false, true]) {
      for (final horizontal in [false, true]) {
        for (final reverse in [false, true]) {
          for (final rtl in [false, true]) {
            for (final empty in [false, true]) {
              testWidgets(
                'SliverFillRemaining scroll=$scroll overscroll=$overscroll horizontal=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
                (tester) async {
                  // Compare transforms to the actual SDK too: its scrollable
                  // branch retains the full viewport scrollExtent even when
                  // preceding slivers reduce paintExtent on reversed axes.
                  const nativeKey = ValueKey('native-fill-body');
                  await tester.pumpWidget(
                    MaterialApp(
                      home: Directionality(
                        textDirection: rtl
                            ? TextDirection.rtl
                            : TextDirection.ltr,
                        child: Center(
                          child: SizedBox(
                            width: 300,
                            height: 160,
                            child: CustomScrollView(
                              primary: false,
                              scrollDirection: horizontal
                                  ? Axis.horizontal
                                  : Axis.vertical,
                              reverse: reverse,
                              slivers: [
                                SliverToBoxAdapter(
                                  child: SizedBox(
                                    width: horizontal ? 40 : null,
                                    height: horizontal ? null : 40,
                                  ),
                                ),
                                SliverFillRemaining(
                                  hasScrollBody: scroll ?? true,
                                  fillOverscroll: overscroll ?? false,
                                  child: empty
                                      ? null
                                      : const SizedBox(
                                          key: nativeKey,
                                          width: 48,
                                          height: 48,
                                        ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  );
                  await tester.pump();
                  Offset? expectedOffset;
                  if (!empty) {
                    final nativeBox = tester.renderObject<RenderBox>(
                      find.byKey(nativeKey),
                    );
                    expectedOffset = nativeBox.localToGlobal(
                      Offset.zero,
                      ancestor: tester.renderObject<RenderBox>(
                        find.byType(CustomScrollView),
                      ),
                    );
                  }
                  CanvasDropResolver? drop;
                  CanvasMovePreviewResolver? move;
                  final selected = <String>[];
                  await pump(
                    tester,
                    modelJson(
                      scroll: scroll,
                      overscroll: overscroll,
                      horizontal: horizontal,
                      reverse: reverse,
                      rtl: rtl,
                      empty: empty,
                    ),
                    selected: selected.add,
                    drop: (v) => drop = v,
                    move: (v) => move = v,
                  );
                  final sdk = tester.widget<SliverFillRemaining>(
                    find.byType(SliverFillRemaining, skipOffstage: false),
                  );
                  expect(sdk.hasScrollBody, scroll ?? true);
                  expect(sdk.fillOverscroll, overscroll ?? false);
                  final render = tester.renderObject<RenderSliver>(
                    find.byType(SliverFillRemaining, skipOffstage: false),
                  );
                  expect(
                    render.runtimeType,
                    (scroll ?? true)
                        ? RenderSliverFillRemainingWithScrollable
                        : (overscroll ?? false)
                        ? RenderSliverFillRemainingAndOverscroll
                        : RenderSliverFillRemaining,
                  );
                  final viewport = tester.renderObject<RenderBox>(
                    find.byType(CustomScrollView),
                  );
                  if (!empty) {
                    final box = tester.renderObject<RenderBox>(
                      find.byKey(const ValueKey('canvas-widget-$bodyId')),
                    );
                    expect(
                      box.size.width,
                      closeTo(horizontal ? 260 : 300, .01),
                    );
                    expect(
                      box.size.height,
                      closeTo(horizontal ? 160 : 120, .01),
                    );
                    final relative = box.localToGlobal(
                      Offset.zero,
                      ancestor: viewport,
                    );
                    expect(relative.dx, closeTo(expectedOffset!.dx, .01));
                    expect(relative.dy, closeTo(expectedOffset.dy, .01));
                    expect(move!(bodyId, fillId, 'child', 0), isNotNull);
                  }
                  final leftward = horizontal && (rtl != reverse);
                  final local = horizontal
                      ? Offset(leftward ? 258 : 42, 2)
                      : Offset(2, reverse ? 118 : 42);
                  final point = viewport.localToGlobal(local);
                  final surface = tester.getRect(
                    find.byType(CanvasDocumentView),
                  );
                  final result = drop!(
                    ((point.dx - surface.left) / surface.width * 1000000)
                        .round(),
                    ((point.dy - surface.top) / surface.height * 1000000)
                        .round(),
                    CanvasPaletteDragSource(
                      token: 'box',
                      widgetType: 'flutter.widgets.Text',
                      traits: {},
                    ),
                  );
                  if (empty) {
                    expect(result?.parentWidgetId, fillId);
                    expect(result?.slotName, 'child');
                    await tester.tapAt(point);
                    await tester.pump();
                    expect(selected, contains(fillId));
                  } else {
                    expect(result?.parentWidgetId, isNot(fillId));
                  }
                  expect(move!(fillId, fillId, 'child', 0), isNull);
                  expect(move!(headerId, fillId, 'child', 0), isNull);
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
    for (final preceding in [0.0, 40.0, 400.0]) {
      for (final extent in [0.0, 48.0, 500.0]) {
        testWidgets(
          'SliverFillRemaining intrinsic extent horizontal=$horizontal preceding=$preceding extent=$extent',
          (tester) async {
            await pump(
              tester,
              modelJson(
                scroll: false,
                preceding: preceding,
                extent: extent,
                horizontal: horizontal,
              ),
            );
            final box = tester.renderObject<RenderBox>(
              find.byKey(
                const ValueKey('canvas-widget-$bodyId'),
                skipOffstage: false,
              ),
            );
            expect(
              horizontal ? box.size.width : box.size.height,
              closeTo(
                math.max(extent, (horizontal ? 300 : 160) - preceding),
                .01,
              ),
            );
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  for (final scroll in [false, true]) {
    for (final overscroll in [false, true]) {
      testWidgets(
        'SliverFillRemaining native overscroll scroll=$scroll fill=$overscroll',
        (tester) async {
          await pump(
            tester,
            modelJson(
              scroll: scroll,
              overscroll: overscroll,
              preceding: 40,
              bouncing: true,
              child: node(bodyId, 'flutter.widgets.Align', {}, {
                'child': single(
                  node(
                    '325ac58f-fc3e-4900-809a-16f1a47a9d4d',
                    'flutter.widgets.Text',
                    {
                      'data': {'kind': 'string', 'value': 'Stretch'},
                    },
                  ),
                ),
              }),
            ),
          );
          final position = tester
              .state<ScrollableState>(find.byType(Scrollable))
              .position;
          final overscrolledOffset = position.maxScrollExtent + 30;
          position.jumpTo(overscrolledOffset);
          final hold = position.hold(() {});
          await tester.pump();
          expect(position.pixels, overscrolledOffset);
          final box = tester.renderObject<RenderBox>(
            find.byKey(const ValueKey('canvas-widget-$bodyId')),
          );
          expect(
            box.size.height,
            closeTo(
              scroll
                  ? 160
                  : overscroll
                  ? 150
                  : 120,
              .01,
            ),
          );
          hold.cancel();
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets('SliverFillRemaining scrollable body remains a real viewport', (
    tester,
  ) async {
    final child = node(
      bodyId,
      'flutter.widgets.ListView',
      {'primary': boolean(false)},
      {
        'children': {
          'kind': 'list',
          'children': [
            node(
              '4fcd13b2-9bad-44c4-84e8-4bda2f4ff0dc',
              'flutter.widgets.Text',
              {
                'data': {'kind': 'string', 'value': 'Nested list'},
              },
            ),
          ],
        },
      },
    );
    await pump(tester, modelJson(child: child));
    expect(find.byType(ListView), findsOneWidget);
    expect(find.text('Nested list'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
