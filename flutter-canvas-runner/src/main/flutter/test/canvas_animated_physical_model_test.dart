import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const type = 'flutter.widgets.AnimatedPhysicalModel';
Map<String, Object?> color(int n) => {
  'kind': 'color',
  'argb': '0x${n.toRadixString(16).toUpperCase().padLeft(8, '0')}',
};
Map<String, Object?> en(String t, String v) => {
  'kind': 'enum',
  'type': t,
  'value': v,
};
Map<String, Object?> boolean(bool b) => {'kind': 'boolean', 'value': b};
Map<String, Object?> radius(double n) => {
  'kind': 'borderRadius',
  'geometry': {
    'kind': 'physical',
    for (final corner in ['topLeft', 'topRight', 'bottomRight', 'bottomLeft'])
      corner: {'x': n, 'y': n * 2},
  },
};
Map<String, Object?> data({
  Map<String, Object?> properties = const {},
  bool rtl = false,
  bool zero = false,
}) {
  final raw = c.data(rtl: rtl);
  a.builder(raw)['type'] = type;
  (a.builder(raw)['properties'] as Map).addAll(<String, Object?>{
    'color': color(0xff2196f3),
    'shadowColor': color(0xff000000),
    ...properties,
  });
  final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
  child['properties'] = <String, Object?>{
    'width': f.number(zero ? 0 : 48),
    'height': f.number(zero ? 0 : 48),
  };
  child['slots'] = {'child': f.single(null)};
  return raw;
}

Finder native() => find.byWidgetPredicate(
  (w) => w is AnimatedPhysicalModel && w.key is GlobalKey,
);
AnimatedPhysicalModel target(WidgetTester t) =>
    t.widget<AnimatedPhysicalModel>(native());
PhysicalModel current(WidgetTester t) => t.widget<PhysicalModel>(
  find.descendant(of: native(), matching: find.byType(PhysicalModel)).first,
);
void main() {
  test(
    'strict required fields nullable physical radii and nonnegative elevation',
    () {
      for (final field in ['color', 'shadowColor', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map).remove(field);
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data();
      a.builder(raw)['slots'] = {'child': f.single(null)};
      expect(() => f.decode(raw), throwsFormatException);
      for (final field in [
        'shape',
        'clipBehavior',
        'elevation',
        'color',
        'shadowColor',
        'animateColor',
        'animateShadowColor',
        'curve',
        'durationUs',
      ]) {
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
      expect(
        () => f.decode(
          data(
            properties: {
              'borderRadius': {'kind': 'null'},
            },
          ),
        ),
        returnsNormally,
      );
      expect(
        () => f.decode(data(properties: {'elevation': f.number(-1)})),
        throwsFormatException,
      );
      expect(
        () => f.decode(data(properties: {'borderRadius': radius(-1)})),
        throwsFormatException,
      );
      final r = radius(2);
      (r['geometry'] as Map)['kind'] = 'directional';
      expect(
        () => f.decode(data(properties: {'borderRadius': r})),
        throwsFormatException,
      );
    },
  );
  for (final rtl in [false, true]) {
    for (final shape in ['rectangle', 'circle']) {
      for (final clip in Clip.values) {
        testWidgets(
          'shape $shape clip ${clip.name} rtl=$rtl defaults and retained corners',
          (t) async {
            await f.pump(
              t,
              data(
                rtl: rtl,
                properties: {
                  'shape': en('BoxShape', shape),
                  'clipBehavior': en('Clip', clip.name),
                  'borderRadius': radius(10),
                },
              ),
            );
            final v = current(t);
            expect(
              v.shape,
              shape == 'circle' ? BoxShape.circle : BoxShape.rectangle,
            );
            expect(v.clipBehavior, clip);
            expect(
              v.borderRadius,
              BorderRadius.all(const Radius.elliptical(10, 20)),
            );
            expect(v.elevation, 0);
            expect(v.color.toARGB32(), 0xff2196f3);
            expect(v.shadowColor.toARGB32(), 0xff000000);
            expect(target(t).animateColor, true);
            expect(target(t).animateShadowColor, true);
            expect(target(t).onEnd, isNull);
            expect(t.takeException(), isNull);
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
    testWidgets('native four tweens curve ${curve.key}', (t) async {
      final p = {
        'curve': {'kind': 'string', 'value': curve.key},
      };
      await f.pump(
        t,
        data(
          properties: {
            ...p,
            'elevation': f.number(10),
            'borderRadius': radius(10),
            'color': color(0xff000000),
            'shadowColor': color(0xff000000),
          },
        ),
      );
      final state = t.state(native());
      await f.pump(
        t,
        data(
          properties: {
            ...p,
            'elevation': f.number(20),
            'borderRadius': radius(20),
            'color': color(0xff88aaff),
            'shadowColor': color(0xff88aaff),
          },
        ),
      );
      for (final v in [.25, .5, .75, 1.0]) {
        await t.pump(const Duration(milliseconds: 25));
        final x = curve.value.transform(v), n = current(t);
        expect(n.elevation, closeTo(10 + 10 * x, .0001));
        expect(n.borderRadius!.topLeft.x, closeTo(10 + 10 * x, .0001));
        expect(n.borderRadius!.topLeft.y, closeTo(20 + 20 * x, .0001));
        final expected = Color.lerp(
          const Color(0xff000000),
          const Color(0xff88aaff),
          x,
        )!;
        for (final actual in [n.color, n.shadowColor]) {
          expect(actual.r, closeTo(expected.r, 1e-6));
          expect(actual.g, closeTo(expected.g, 1e-6));
          expect(actual.b, closeTo(expected.b, 1e-6));
          expect(actual.a, closeTo(expected.a, 1e-6));
        }
      }
      expect(identical(state, t.state(native())), true);
      expect(t.takeException(), isNull);
    });
  }
  for (final fill in [true, false]) {
    for (final shadow in [true, false]) {
      testWidgets('independent animateColor $fill animateShadowColor $shadow', (
        t,
      ) async {
        final p = {
          'animateColor': boolean(fill),
          'animateShadowColor': boolean(shadow),
        };
        await f.pump(
          t,
          data(
            properties: {
              ...p,
              'color': color(0xff000000),
              'shadowColor': color(0xff000000),
            },
          ),
        );
        await f.pump(
          t,
          data(
            properties: {
              ...p,
              'color': color(0xffffffff),
              'shadowColor': color(0xffffffff),
              'shape': en('BoxShape', 'circle'),
              'clipBehavior': en('Clip', 'antiAlias'),
            },
          ),
        );
        expect(current(t).shape, BoxShape.circle);
        expect(current(t).clipBehavior, Clip.antiAlias);
        await t.pump(const Duration(milliseconds: 50));
        final mid = Color.lerp(
          const Color(0xff000000),
          const Color(0xffffffff),
          .5,
        )!.toARGB32();
        expect(current(t).color.toARGB32(), fill ? mid : 0xffffffff);
        expect(current(t).shadowColor.toARGB32(), shadow ? mid : 0xffffffff);
        await t.pumpAndSettle();
        expect(t.takeException(), isNull);
      });
    }
  }
  testWidgets(
    'radius-only overshoot follows SDK and zero duration does not restart child',
    (t) async {
      await f.pump(t, data(properties: {'borderRadius': radius(0)}));
      final state = t.state(native());
      await f.pump(
        t,
        data(
          properties: {
            'borderRadius': radius(10),
            'curve': {'kind': 'string', 'value': 'easeInBack'},
          },
        ),
      );
      await t.pump(const Duration(milliseconds: 25));
      expect(
        current(t).borderRadius!.topLeft.x,
        closeTo(10 * Curves.easeInBack.transform(.25), .0001),
      );
      expect(identical(state, t.state(native())), true);
      await t.pumpAndSettle();
      await f.pump(
        t,
        data(
          properties: {
            'elevation': f.number(20),
            'curve': {'kind': 'string', 'value': 'easeInBack'},
            'durationUs': {'kind': 'integer', 'value': 0},
          },
        ),
      );
      await t.pumpAndSettle();
      expect(current(t).elevation, 20);
      expect(identical(state, t.state(native())), true);
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'turning on color animation mid-flight resumes the tracked tween',
    (t) async {
      await f.pump(
        t,
        data(
          properties: {
            'color': color(0xff000000),
            'animateColor': boolean(false),
          },
        ),
      );
      await f.pump(
        t,
        data(
          properties: {
            'color': color(0xffffffff),
            'animateColor': boolean(false),
          },
        ),
      );
      await t.pump(const Duration(milliseconds: 50));
      expect(current(t).color.toARGB32(), 0xffffffff);
      final state = t.state(native());
      await f.pump(
        t,
        data(
          properties: {
            'color': color(0xffffffff),
            'animateColor': boolean(true),
          },
        ),
      );
      await t.pump(const Duration(milliseconds: 25));
      expect(current(t).color.r, closeTo(.75, 1e-6));
      expect(identical(state, t.state(native())), true);
      await t.pumpAndSettle();
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('radius null is animated zero and references are inert', (
    t,
  ) async {
    await f.pump(t, data(properties: {'borderRadius': radius(10)}));
    await f.pump(
      t,
      data(
        properties: {
          'borderRadius': {'kind': 'null'},
        },
      ),
    );
    await t.pump(const Duration(milliseconds: 50));
    expect(current(t).borderRadius!.topLeft.x, closeTo(5, .001));
    await t.pumpAndSettle();
    await f.pump(
      t,
      data(
        properties: {
          for (final f in [
            'borderRadius',
            'elevation',
            'color',
            'shadowColor',
            'curve',
            'durationUs',
            'onEnd',
          ])
            f: {'kind': 'dartObjectReferencePresence'},
        },
      ),
    );
    final n = target(t);
    expect(n.borderRadius, isNull);
    expect(n.elevation, 0);
    expect(n.color.toARGB32(), 0xff2196f3);
    expect(n.duration, const Duration(milliseconds: 300));
    expect(n.curve, Curves.linear);
    expect(n.onEnd, isNull);
    expect(
      find.byWidgetPredicate(
        (w) =>
            w is Tooltip &&
            w.message?.contains('AnimatedPhysicalModel') == true,
      ),
      findsWidgets,
    );
    expect(t.takeException(), isNull);
  });
  for (final curveOnly in [false, true]) {
    testWidgets(
      'unsafe overshoot curveOnly=$curveOnly keeps child and allows recovery',
      (t) async {
        await f.pump(t, data(properties: {'elevation': f.number(0)}));
        final element = t.element(
              find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
            ),
            state = t.state(native());
        if (curveOnly) {
          await f.pump(t, data(properties: {'elevation': f.number(20)}));
          await t.pump(const Duration(milliseconds: 30));
        }
        await f.pump(
          t,
          data(
            properties: {
              'elevation': f.number(20),
              'curve': {'kind': 'string', 'value': 'easeInBack'},
            },
          ),
        );
        await t.pumpAndSettle();
        expect(current(t).elevation, 20);
        expect(identical(state, t.state(native())), false);
        expect(
          identical(
            element,
            t.element(find.byKey(const ValueKey('canvas-widget-${f.bodyId}'))),
          ),
          true,
        );
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                w.message?.contains(
                      'Showing the target without this transition',
                    ) ==
                    true,
          ),
          findsWidgets,
        );
        await f.pump(t, data(properties: {'elevation': f.number(10)}));
        await t.pumpAndSettle();
        expect(current(t).elevation, 10);
        expect(t.takeException(), isNull);
      },
    );
  }
  testWidgets('occupied required Child wrap and zero-size selection survive', (
    t,
  ) async {
    CanvasDropResolver? resolver;
    await f.pump(t, data(), drop: (r) => resolver = r);
    final surface = t.getRect(find.byType(CanvasDocumentView)),
        point = t.getCenter(
          find.byKey(const ValueKey('canvas-widget-${f.bodyId}')),
        );
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(token: 'physical', widgetType: type, traits: {}),
    );
    expect(drop?.parentWidgetId, a.builderId);
    expect(drop?.slotName, 'child');
    expect(drop?.insertionIndex, 0);
    await f.pump(t, data(zero: true));
    await t.pumpAndSettle();
    expect(
      find.byWidgetPredicate(
        (w) =>
            w.key.toString().contains('canvas-zero-size-widget-target') &&
            w.key.toString().contains(a.builderId),
      ),
      findsWidgets,
    );
    await f.pump(t, data());
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
  });
  testWidgets(
    'native onEnd tracks disabled color tween interruption and zero duration',
    (t) async {
      var ended = 0;
      Widget build(Color color, {int ms = 100}) => Directionality(
        textDirection: TextDirection.ltr,
        child: AnimatedPhysicalModel(
          color: color,
          shadowColor: color,
          animateColor: false,
          animateShadowColor: false,
          duration: Duration(milliseconds: ms),
          onEnd: () => ended++,
          child: const SizedBox(width: 20, height: 20),
        ),
      );
      await t.pumpWidget(build(Colors.black));
      await t.pumpAndSettle();
      expect(ended, 0);
      await t.pumpWidget(build(Colors.white));
      await t.pump(const Duration(milliseconds: 50));
      expect(ended, 0);
      await t.pumpWidget(build(Colors.blue));
      await t.pumpAndSettle();
      expect(ended, 1);
      await t.pumpWidget(build(Colors.blue));
      await t.pumpAndSettle();
      expect(ended, 1);
      await t.pumpWidget(build(Colors.red, ms: 0));
      await t.pumpAndSettle();
      expect(ended, 2);
      expect(t.takeException(), isNull);
    },
  );
}
