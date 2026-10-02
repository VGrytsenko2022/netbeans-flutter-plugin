import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;
import 'canvas_animated_rotation_test.dart' as r;

const type = 'flutter.widgets.AnimatedSize';
Map<String, Object?> data({
  Map<String, Object?> properties = const {},
  double extent = 40,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = c.data(
    properties: properties,
    empty: empty,
    tight: tight,
    rtl: rtl,
  );
  a.builder(raw)['type'] = type;
  if (!empty) {
    final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
    child['properties'] = {
      'width': f.number(extent),
      'height': f.number(extent),
    };
    child['slots'] = {'child': f.single(null)};
  }
  return raw;
}

Finder native() => find.byType(AnimatedSize);
RenderAnimatedSize render(WidgetTester tester) =>
    tester.renderObject<RenderAnimatedSize>(native());
void main() {
  test('closed domains both durations nullable reverse and optional child', () {
    for (final field in ['durationUs', 'reverseDurationUs']) {
      for (final bad in [-1, 9007199254740992, 1.5, '400', true]) {
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'integer', 'value': bad},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final value in [0, 1, 9007199254740991]) {
        expect(
          () => f.decode(
            data(
              properties: {
                field: {'kind': 'integer', 'value': value},
              },
            ),
          ),
          returnsNormally,
        );
      }
    }
    for (final field in ['alignment', 'curve', 'durationUs', 'clipBehavior']) {
      expect(
        () => f.decode(
          data(
            properties: {
              field: {'kind': 'null'},
            },
          ),
        ),
        throwsFormatException,
      );
    }
    final missing = data();
    (a.builder(missing)['properties'] as Map).remove('durationUs');
    expect(() => f.decode(missing), throwsFormatException);
    for (final field in ['width', 'height', 'vsync']) {
      expect(
        () => f.decode(data(properties: {field: f.number(1)})),
        throwsFormatException,
      );
    }
    expect(
      () => f.decode(
        data(
          properties: {
            'reverseDurationUs': {'kind': 'null'},
            'onEnd': {'kind': 'null'},
          },
        ),
      ),
      returnsNormally,
    );
  });
  testWidgets(
    'growth shrink and retained state use pinned native forward duration',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native());
      expect(render(tester).size, const Size(40, 40));
      await f.pump(tester, data(extent: 80));
      await tester.pump(const Duration(milliseconds: 50));
      expect(render(tester).size.width, closeTo(60, .1));
      await tester.pumpAndSettle();
      expect(render(tester).size, const Size(80, 80));
      await f.pump(
        tester,
        data(
          extent: 40,
          properties: {
            'reverseDurationUs': {'kind': 'integer', 'value': 400000},
          },
        ),
      );
      await tester.pump(const Duration(milliseconds: 50));
      expect(render(tester).size.width, closeTo(60, .1));
      await tester.pump(const Duration(milliseconds: 50));
      expect(render(tester).size, const Size(40, 40));
      expect(identical(state, tester.state(native())), isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  for (final rtl in [false, true]) {
    for (final directional in [false, true]) {
      for (final clip in Clip.values) {
        testWidgets(
          'alignment overflow and clipping rtl=$rtl directional=$directional clip=$clip',
          (tester) async {
            final props = <String, Object?>{
              'alignment': r.alignment(1, 1, directional: directional),
              'clipBehavior': {
                'kind': 'enum',
                'type': 'Clip',
                'value': clip.name,
              },
            };
            await f.pump(tester, data(rtl: rtl, properties: props));
            await f.pump(tester, data(extent: 80, rtl: rtl, properties: props));
            await tester.pump(const Duration(milliseconds: 50));
            final box = render(tester), child = render(tester).child!;
            expect(box.clipBehavior, clip);
            final offset = (child.parentData! as BoxParentData).offset;
            expect(offset.dx, closeTo(directional && rtl ? 0 : -20, .1));
            expect(offset.dy, closeTo(-20, .1));
            expect(
              box.hitTest(BoxHitTestResult(), position: const Offset(-1, 1)),
              isFalse,
            );
            expect(
              box.hitTest(
                BoxHitTestResult(),
                position: Offset(box.size.width + 1, 1),
              ),
              isFalse,
            );
            await tester.pumpAndSettle();
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  for (final curve in <String, Curve>{
    'linear': Curves.linear,
    'decelerate': Curves.decelerate,
    'fastLinearToSlowEaseIn': Curves.fastLinearToSlowEaseIn,
    'fastEaseInToSlowEaseOut': Curves.fastEaseInToSlowEaseOut,
    'ease': Curves.ease,
    'easeIn': Curves.easeIn,
    'easeInToLinear': Curves.easeInToLinear,
    'easeInSine': Curves.easeInSine,
    'easeInQuad': Curves.easeInQuad,
    'easeInCubic': Curves.easeInCubic,
    'easeInQuart': Curves.easeInQuart,
    'easeInQuint': Curves.easeInQuint,
    'easeInExpo': Curves.easeInExpo,
    'easeInCirc': Curves.easeInCirc,
    'easeInBack': Curves.easeInBack,
    'easeOut': Curves.easeOut,
    'linearToEaseOut': Curves.linearToEaseOut,
    'easeOutSine': Curves.easeOutSine,
    'easeOutQuad': Curves.easeOutQuad,
    'easeOutCubic': Curves.easeOutCubic,
    'easeOutQuart': Curves.easeOutQuart,
    'easeOutQuint': Curves.easeOutQuint,
    'easeOutExpo': Curves.easeOutExpo,
    'easeOutCirc': Curves.easeOutCirc,
    'easeOutBack': Curves.easeOutBack,
    'easeInOut': Curves.easeInOut,
    'easeInOutSine': Curves.easeInOutSine,
    'easeInOutQuad': Curves.easeInOutQuad,
    'easeInOutCubic': Curves.easeInOutCubic,
    'easeInOutCubicEmphasized': Curves.easeInOutCubicEmphasized,
    'easeInOutQuart': Curves.easeInOutQuart,
    'easeInOutQuint': Curves.easeInOutQuint,
    'easeInOutExpo': Curves.easeInOutExpo,
    'easeInOutCirc': Curves.easeInOutCirc,
    'easeInOutBack': Curves.easeInOutBack,
    'fastOutSlowIn': Curves.fastOutSlowIn,
    'slowMiddle': Curves.slowMiddle,
    'bounceIn': Curves.bounceIn,
    'bounceOut': Curves.bounceOut,
    'bounceInOut': Curves.bounceInOut,
    'elasticIn': Curves.elasticIn,
    'elasticOut': Curves.elasticOut,
    'elasticInOut': Curves.elasticInOut,
  }.entries) {
    testWidgets('native curve and constraint interpolation ${curve.key}', (
      tester,
    ) async {
      final props = <String, Object?>{
        'curve': {'kind': 'string', 'value': curve.key},
      };
      await f.pump(tester, data(extent: 0, properties: props));
      await f.pump(tester, data(extent: 80, properties: props));
      for (final t in [.25, .5, .75, 1.0]) {
        await tester.pump(const Duration(milliseconds: 25));
        final expected = (80 * curve.value.transform(t)).clamp(0.0, 160.0);
        expect(render(tester).size.width, closeTo(expected, .01));
      }
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('tight constraints and empty child retain native defaults', (
    tester,
  ) async {
    await f.pump(tester, data(tight: true));
    expect(render(tester).size, const Size(240, 160));
    await f.pump(tester, data(tight: true, extent: 80));
    expect(render(tester).size, const Size(240, 160));
    expect(render(tester).isAnimating, isFalse);
    await f.pump(tester, data(empty: true));
    expect(render(tester).size, Size.zero);
    expect(tester.widget<AnimatedSize>(native()).clipBehavior, Clip.hardEdge);
    expect(tester.takeException(), isNull);
  });
  testWidgets('project references are inert with precise preview fallbacks', (
    tester,
  ) async {
    final props = <String, Object?>{
      for (final name in [
        'alignment',
        'curve',
        'durationUs',
        'reverseDurationUs',
        'onEnd',
      ])
        name: {'kind': 'dartObjectReferencePresence'},
    };
    await f.pump(tester, data(properties: props));
    final n = tester.widget<AnimatedSize>(native());
    expect(n.alignment, Alignment.center);
    expect(n.curve, Curves.linear);
    expect(n.duration, const Duration(milliseconds: 300));
    expect(n.reverseDuration, isNull);
    expect(n.onEnd, isNull);
    expect(
      find.byWidgetPredicate(
        (w) => w is Tooltip && w.message?.contains('AnimatedSize') == true,
      ),
      findsWidgets,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets('empty node remains selectable and accepts Child drop', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true),
      drop: (r) => drop = r,
      selected: selected.add,
    );
    final handle = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-${a.builderId}'),
    );
    expect(handle, findsOneWidget);
    final point = tester.getCenter(handle),
        surface = tester.getRect(find.byType(CanvasDocumentView));
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'test',
        widgetType: 'flutter.widgets.Text',
        traits: {},
      ),
    );
    expect(target?.parentWidgetId, a.builderId);
    expect(target?.slotName, 'child');
    await tester.tapAt(point);
    expect(selected, contains(a.builderId));
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'selection handles follow shrink to zero and growth without another model edit',
    (tester) async {
      await f.pump(tester, data());
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-${a.builderId}'),
      );
      expect(handle, findsNothing);
      await f.pump(tester, data(extent: 0));
      await tester.pumpAndSettle();
      expect(render(tester).size, Size.zero);
      expect(handle, findsOneWidget);
      await f.pump(tester, data(extent: 40));
      await tester.pumpAndSettle();
      expect(handle, findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('continuous child changes follow native unstable size state', (
    tester,
  ) async {
    double extent = 40;
    var completed = 0;
    late StateSetter update;
    final content = StatefulBuilder(
      builder: (context, setState) {
        update = setState;
        return AnimatedSize(
          duration: const Duration(milliseconds: 100),
          onEnd: () => completed++,
          child: SizedBox(width: extent, height: extent),
        );
      },
    );
    Future<void> frame(double n) async {
      extent = n;
      update(() {});
      await tester.pump();
    }

    await tester.pumpWidget(
      Directionality(
        textDirection: TextDirection.ltr,
        child: Center(child: content),
      ),
    );
    expect(completed, 0);
    await frame(80);
    expect(render(tester).state, RenderAnimatedSizeState.changed);
    await frame(90);
    expect(render(tester).state, RenderAnimatedSizeState.unstable);
    expect(render(tester).size, const Size(90, 90));
    await frame(100);
    expect(render(tester).size, const Size(100, 100));
    await tester.pump(const Duration(milliseconds: 1));
    expect(render(tester).state, RenderAnimatedSizeState.stable);
    expect(render(tester).isAnimating, isFalse);
    expect(completed, 0);
    await frame(60);
    await tester.pump();
    await tester.pumpAndSettle();
    expect(completed, 1);
    await frame(60);
    await tester.pump(const Duration(milliseconds: 200));
    expect(completed, 1);
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    expect(tester.takeException(), isNull);
  });
}
