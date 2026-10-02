import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_opacity_test.dart' as b;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_opacity_test.dart' as s;
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> data(
  bool sliver, {
  double opacity = 1,
  bool? semantics,
  bool empty = false,
  bool rtl = false,
  bool horizontal = false,
  bool reverse = false,
}) {
  final raw = sliver
      ? s.data(
          opacity: opacity,
          semantics: semantics,
          empty: empty,
          rtl: rtl,
          horizontal: horizontal,
          reverse: reverse,
        )
      : b.data(opacity: opacity, semantics: semantics, empty: empty, rtl: rtl);
  final node = target(raw, sliver);
  node['type'] = sliver
      ? 'flutter.widgets.SliverFadeTransition'
      : 'flutter.widgets.FadeTransition';
  (node['properties'] as Map).remove('durationUs');
  return raw;
}

Map<String, Object?> target(Map<String, Object?> raw, bool sliver) =>
    f.findNode(
      raw['root'] as Map<String, Object?>,
      sliver ? f.fillId : a.builderId,
    );
Finder native(bool sliver) => find.byType(
  sliver ? SliverFadeTransition : FadeTransition,
  skipOffstage: false,
);
void main() {
  for (final sliver in [false, true]) {
    test('strict domains and constructor arguments sliver=$sliver', () {
      final raw = data(sliver),
          props = target(raw, sliver)['properties'] as Map;
      for (final bad in [
        f.number(-.01),
        f.number(1.01),
        {'kind': 'integer', 'value': 2},
        {'kind': 'null'},
        {'kind': 'string', 'value': '.5'},
        {'kind': 'boolean', 'value': true},
      ]) {
        props['opacity'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      props.remove('opacity');
      expect(() => f.decode(raw), throwsFormatException);
      for (final value in [
        f.number(0),
        f.number(.5),
        f.number(1),
        {'kind': 'integer', 'value': 0},
        {'kind': 'dartObjectReferencePresence'},
      ]) {
        props['opacity'] = value;
        expect(() => f.decode(raw), returnsNormally);
      }
      props['opacity'] = f.number(1);
      for (final field in ['durationUs', 'curve', 'onEnd']) {
        props[field] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove(field);
      }
      props['alwaysIncludeSemantics'] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
    });
    testWidgets(
      'project animation is presence-only with explicit fallback sliver=$sliver',
      (tester) async {
        final raw = data(sliver);
        (target(raw, sliver)['properties'] as Map)['opacity'] = {
          'kind': 'dartObjectReferencePresence',
        };
        await f.pump(tester, raw);
        final n = tester.widget(native(sliver));
        expect(
          n is FadeTransition
              ? n.opacity.value
              : (n as SliverFadeTransition).opacity.value,
          1,
        );
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                w.message?.contains('stopped opacity of 1') == true,
          ),
          findsWidgets,
        );
        expect(tester.takeException(), isNull);
      },
    );
    for (final opacity in [0.0, .125, 1.0]) {
      for (final semantics in <bool?>[null, false, true]) {
        for (final empty in [false, true]) {
          for (final rtl in [false, true]) {
            testWidgets(
              'native geometry and semantics s=$sliver o=$opacity a=$semantics e=$empty rtl=$rtl',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    sliver,
                    opacity: opacity,
                    semantics: semantics,
                    empty: empty,
                    rtl: rtl,
                  ),
                );
                final n = tester.widget(native(sliver));
                expect(
                  n is FadeTransition
                      ? n.opacity.value
                      : (n as SliverFadeTransition).opacity.value,
                  opacity,
                );
                final render = tester.renderObject(native(sliver));
                if (render is RenderAnimatedOpacity) {
                  expect(render.size.height, empty ? 0 : 48);
                }
                if (render is RenderSliverAnimatedOpacity) {
                  expect(render.geometry!.scrollExtent, empty ? 0 : 48);
                }
                final children = <RenderObject>[];
                render.visitChildrenForSemantics(children.add);
                expect(
                  children.isNotEmpty,
                  (!empty || sliver) && (opacity > 0 || semantics == true),
                );
                expect(tester.takeException(), isNull);
              },
            );
          }
        }
      }
    }
    testWidgets(
      'local updates preserve render and child identity sliver=$sliver',
      (tester) async {
        RenderObject? original;
        Element? child;
        for (final opacity in [1.0, .5, 0.0, .001, 1.0]) {
          await f.pump(tester, data(sliver, opacity: opacity));
          final render = tester.renderObject(native(sliver));
          original ??= render;
          expect(identical(render, original), true);
          final element = tester.element(
            find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
          );
          child ??= element;
          expect(identical(child, element), true);
          final children = <RenderObject>[];
          render.visitChildrenForSemantics(children.add);
          expect(children.isNotEmpty, opacity >= .5 / 255);
          expect(tester.takeException(), isNull);
        }
      },
    );
  }
  testWidgets('empty box exposes insertion handle and restricts slivers', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    await f.pump(tester, data(false, empty: true), drop: (v) => drop = v);
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final handle = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
    );
    expect(handle, findsOneWidget);
    final point = tester.getCenter(handle);
    final x = ((point.dx - surface.left) / surface.width * 1000000).round(),
        y = ((point.dy - surface.top) / surface.height * 1000000).round();
    final box = drop!(
      x,
      y,
      CanvasPaletteDragSource(
        token: 'fade-child',
        widgetType: 'flutter.widgets.Text',
        traits: {},
      ),
    );
    expect(box?.parentWidgetId, a.builderId);
    expect(box?.slotName, 'child');
    final sliver = drop!(
      x,
      y,
      CanvasPaletteDragSource(
        token: 'fade-sliver',
        widgetType: 'flutter.widgets.SliverToBoxAdapter',
        traits: {canvasSliverWidgetTrait},
      ),
    );
    expect(sliver, isNull);
    expect(tester.takeException(), isNull);
  });
  for (final horizontal in [false, true]) {
    for (final reverse in [false, true]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'empty sliver insertion h=$horizontal r=$reverse rtl=$rtl',
          (tester) async {
            CanvasDropResolver? drop;
            await f.pump(
              tester,
              data(
                true,
                opacity: 0,
                empty: true,
                horizontal: horizontal,
                reverse: reverse,
                rtl: rtl,
              ),
              drop: (v) => drop = v,
            );
            final surface = tester.getRect(find.byType(CanvasDocumentView)),
                viewport = tester.getRect(find.byType(CustomScrollView));
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
                token: 'sliver-fade-child',
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
}
