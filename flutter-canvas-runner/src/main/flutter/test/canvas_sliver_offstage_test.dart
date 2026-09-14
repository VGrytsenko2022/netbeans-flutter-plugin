import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_sliver_opacity_test.dart' as o;

const type = 'flutter.widgets.SliverOffstage';
Map<String, Object?> data({
  bool? offstage,
  bool empty = false,
  bool horizontal = false,
  bool reverse = false,
  bool rtl = false,
  double preceding = 0,
}) {
  final raw = o.data(
    empty: empty,
    horizontal: horizontal,
    reverse: reverse,
    rtl: rtl,
    preceding: preceding,
  );
  final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
  fill['type'] = type;
  fill['properties'] = {if (offstage != null) 'offstage': f.boolean(offstage)};
  return raw;
}

void main() {
  test(
    'strict boolean contract, omitted slot, sliver-only placement and parent data',
    () {
      final raw = data(empty: true);
      final fill = f.findNode(raw['root'] as Map<String, Object?>, f.fillId);
      for (final field in ['offstage']) {
        for (final bad in [
          {'kind': 'integer', 'value': 1},
          {'kind': 'string', 'value': 'true'},
          f.number(1),
        ]) {
          fill['properties'] = {field: bad};
          expect(() => f.decode(raw), throwsFormatException);
        }
      }
      fill['properties'] = {
        'offstage': {'kind': 'null'},
      };
      expect(() => f.decode(raw), throwsFormatException);
      fill['properties'] = <String, Object?>{};
      fill['slots'] = <String, Object?>{};
      expect(() => f.decode(raw), returnsNormally);
      fill['slots'] = {
        'sliver': {'kind': 'list', 'children': []},
      };
      expect(() => f.decode(raw), throwsFormatException);
      for (final child in [
        f.node(o.adapterId, 'flutter.widgets.Text', {
          'data': {'kind': 'string', 'value': 'Box'},
        }),
        f.node(
          o.adapterId,
          'flutter.widgets.SliverCrossAxisExpanded',
          {
            'flex': {'kind': 'integer', 'value': 1},
          },
          {
            'sliver': f.single(
              f.node(f.bodyId, 'flutter.widgets.SliverToBoxAdapter'),
            ),
          },
        ),
      ]) {
        fill['slots'] = {'sliver': f.single(child)};
        expect(() => f.decode(raw), throwsFormatException);
      }
      fill['slots'] = <String, Object?>{};
      raw['root'] = fill;
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final offstage in [null, false, true]) {
    for (final horizontal in [false, true]) {
      for (final reverse in [false, true]) {
        for (final rtl in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'native offstage=$offstage h=$horizontal reverse=$reverse rtl=$rtl empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    offstage: offstage,
                    horizontal: horizontal,
                    reverse: reverse,
                    rtl: rtl,
                    empty: empty,
                  ),
                );
                final native = tester.widget<SliverOffstage>(
                  find.byType(SliverOffstage, skipOffstage: false),
                );
                expect(native.offstage, offstage ?? true);
                if (empty) {
                  expect((native.child! as SliverToBoxAdapter).child, isNull);
                }
                final render = tester.renderObject<RenderSliverOffstage>(
                  find.byType(SliverOffstage, skipOffstage: false),
                );
                expect(
                  render.geometry!.scrollExtent,
                  empty || (offstage ?? true) ? 0 : 48,
                );
                expect(
                  render.geometry!.paintExtent,
                  empty || (offstage ?? true) ? 0 : 48,
                );
                if (!empty) {
                  final size = tester.getSize(
                    find.byKey(
                      const ValueKey('canvas-widget-${f.bodyId}'),
                      skipOffstage: false,
                    ),
                  );
                  expect(horizontal ? size.width : size.height, 48);
                  expect(
                    horizontal ? size.height : size.width,
                    horizontal ? 160 : 300,
                  );
                }
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
  }

  testWidgets(
    'hide and show retain render identity and child layout but remove geometry and semantics',
    (tester) async {
      RenderSliverOffstage? original;
      RenderObject? child;
      for (final offstage in [false, true, false, true]) {
        await f.pump(tester, data(offstage: offstage));
        final render = tester.renderObject<RenderSliverOffstage>(
          find.byType(SliverOffstage, skipOffstage: false),
        );
        original ??= render;
        child ??= render.child;
        expect(render, same(original));
        expect(render.child, same(child));
        expect(render.child!.geometry!.scrollExtent, 48);
        expect(render.geometry!.scrollExtent, offstage ? 0 : 48);
        expect(render.geometry!.paintExtent, offstage ? 0 : 48);
        final semantics = <RenderObject>[];
        render.visitChildrenForSemantics(semantics.add);
        expect(semantics.isEmpty, offstage);
        if (offstage) {
          expect(
            render.hitTest(
              SliverHitTestResult(),
              mainAxisPosition: 5,
              crossAxisPosition: 5,
            ),
            false,
          );
        }
        expect(tester.takeException(), isNull);
      }
    },
  );
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'hidden nested slivers have no Canvas handles or drop geometry h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            final selected = <String>[];
            final raw = data(
              horizontal: horizontal,
              reverse: reverse,
              rtl: rtl,
            );
            // An empty nested offstage is itself a potential drop destination when visible.
            final nested = f.findNode(
              raw['root'] as Map<String, Object?>,
              o.adapterId,
            );
            nested['type'] = type;
            nested['properties'] = {'offstage': f.boolean(false)};
            nested['slots'] = <String, Object?>{};
            await f.pump(
              tester,
              raw,
              selected: selected.add,
              drop: (v) => drop = v,
            );
            final handle = find.byKey(
              const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
            );
            expect(handle, findsOneWidget);
            expect(
              find.byKey(
                const ValueKey('canvas-zero-size-widget-target-${o.adapterId}'),
              ),
              findsNothing,
            );
            expect(
              find.byKey(
                const ValueKey(
                  'canvas-zero-size-widget-target-group-${f.fillId}',
                ),
              ),
              findsNothing,
            );
            await tester.tap(handle);
            expect(selected, [f.fillId]);
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final point = tester.getCenter(handle);
            final target = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'hidden-child',
                widgetType: 'flutter.widgets.SliverToBoxAdapter',
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(target?.parentWidgetId, isNot(o.adapterId));
            final outer = f.findNode(
              raw['root'] as Map<String, Object?>,
              f.fillId,
            );
            outer['properties'] = {'offstage': f.boolean(false)};
            await f.pump(tester, raw, drop: (v) => drop = v);
            final visible = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'visible-child',
                widgetType: 'flutter.widgets.SliverToBoxAdapter',
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(visible?.parentWidgetId, o.adapterId);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets('hidden box descendants never expose synthetic handles', (
    tester,
  ) async {
    final raw = data();
    final body = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
    body['properties'] = {'height': f.number(0), 'width': f.number(0)};
    await f.pump(tester, raw);
    expect(
      find.byKey(const ValueKey('canvas-zero-size-widget-target-${f.bodyId}')),
      findsNothing,
    );
    expect(
      find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-${f.fillId}'),
      ),
      findsNothing,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'native hidden state preserves animation state while suppressing pointer and semantics',
    (tester) async {
      final semantics = tester.ensureSemantics();
      final key = GlobalKey<_AnimatedBodyState>();
      var taps = 0;
      for (final hidden in [false, true, false]) {
        final previous = key.currentState;
        await tester.pumpWidget(
          MaterialApp(
            home: CustomScrollView(
              slivers: [
                SliverOffstage(
                  offstage: hidden,
                  sliver: SliverToBoxAdapter(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () => taps++,
                      child: Semantics(
                        label: 'Offstage action',
                        child: _AnimatedBody(key: key),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
        final before = key.currentState!.ticks;
        await tester.pump(const Duration(milliseconds: 50));
        await tester.pump(const Duration(milliseconds: 50));
        expect(key.currentState!.ticks, greaterThan(before));
        if (previous != null) {
          expect(key.currentState, same(previous));
        }
        final beforeTaps = taps;
        await tester.tapAt(const Offset(20, 20));
        expect(taps, hidden ? beforeTaps : beforeTaps + 1);
        expect(
          find.bySemanticsLabel('Offstage action'),
          hidden ? findsNothing : findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      }
      await tester.pumpWidget(const SizedBox.shrink());
      semantics.dispose();
    },
  );
  testWidgets('offstage leaves programmatic keyboard focus intact', (
    tester,
  ) async {
    final node = FocusNode();
    var keys = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: CustomScrollView(
          slivers: [
            SliverOffstage(
              sliver: SliverToBoxAdapter(
                child: Focus(
                  focusNode: node,
                  onKeyEvent: (_, event) {
                    if (event is KeyDownEvent) {
                      keys++;
                    }
                    return KeyEventResult.handled;
                  },
                  child: const SizedBox(height: 60),
                ),
              ),
            ),
          ],
        ),
      ),
    );
    node.requestFocus();
    await tester.pump();
    await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
    expect(node.hasFocus, true);
    expect(keys, 1);
    await tester.pumpWidget(const SizedBox.shrink());
    node.dispose();
    expect(tester.takeException(), isNull);
  });
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'empty offstage sliver drop h=$horizontal reverse=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            await f.pump(
              tester,
              data(
                empty: true,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              ),
              drop: (v) => drop = v,
            );
            final surface = tester.getRect(find.byType(CanvasDocumentView));
            final viewport = tester.getRect(find.byType(CustomScrollView));
            final flipped = reverse != (horizontal && rtl);
            final point = horizontal
                ? Offset(
                    flipped ? viewport.right - 5 : viewport.left + 5,
                    viewport.center.dy,
                  )
                : Offset(
                    viewport.center.dx,
                    flipped ? viewport.bottom - 5 : viewport.top + 5,
                  );
            final target = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'ignore-child',
                widgetType: 'flutter.widgets.SliverToBoxAdapter',
                traits: {canvasSliverWidgetTrait},
              ),
            );
            expect(target?.parentWidgetId, f.fillId);
            expect(target?.slotName, 'sliver');
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets('offscreen offstage sliver exposes no stale nested drop target', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    await f.pump(
      tester,
      data(empty: true, preceding: 700),
      drop: (v) => drop = v,
    );
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point =
        tester.getRect(find.byType(CustomScrollView)).topLeft +
        const Offset(5, 5);
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'offscreen-ignore',
        widgetType: 'flutter.widgets.SliverToBoxAdapter',
        traits: {canvasSliverWidgetTrait},
      ),
    );
    expect(target?.parentWidgetId, isNot(f.fillId));
    expect(tester.takeException(), isNull);
  });
}

class _AnimatedBody extends StatefulWidget {
  const _AnimatedBody({super.key});
  @override
  State<_AnimatedBody> createState() => _AnimatedBodyState();
}

class _AnimatedBodyState extends State<_AnimatedBody>
    with SingleTickerProviderStateMixin {
  late final AnimationController controller;
  int ticks = 0;
  @override
  void initState() {
    super.initState();
    controller =
        AnimationController(vsync: this, duration: const Duration(seconds: 1))
          ..addListener(() => ticks++)
          ..repeat();
  }

  @override
  Widget build(BuildContext context) => const SizedBox(height: 60);
  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }
}
