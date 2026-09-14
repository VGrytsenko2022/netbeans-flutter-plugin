import 'dart:convert';
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_opacity_test.dart' as o;

const type = 'flutter.widgets.AnimatedSlide';
Map<String, Object?> data({
  double dx = 0,
  double dy = 0,
  int duration = 300000,
  String? curve,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = o.data(empty: empty, tight: tight, rtl: rtl);
  final widget = a.builder(raw);
  widget['type'] = type;
  widget['properties'] = {
    'offset': <String, Object?>{'kind': 'offset', 'dx': dx, 'dy': dy},
    'durationUs': {'kind': 'integer', 'value': duration},
    if (curve != null) 'curve': {'kind': 'string', 'value': curve},
  };
  raw['root'] = f.node(
    'f20a4798-c555-4d61-b878-cb71c082ea2f',
    'flutter.widgets.Directionality',
    {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {'child': f.single(raw['root'] as Map<String, Object?>)},
  );
  return raw;
}

Finder native() => find.byType(AnimatedSlide);
Finder slide() =>
    find.descendant(of: native(), matching: find.byType(SlideTransition)).first;
Finder body() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test('required signed finite Offset and exact Duration/presence domains', () {
    for (final name in ['offset', 'durationUs']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map).remove(name);
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final name in ['offset', 'curve', 'durationUs']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)[name] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final coordinate in ['dx', 'dy']) {
      for (final invalid in ['Infinity', 'NaN', null, true]) {
        final raw = data();
        ((a.builder(raw)['properties'] as Map)['offset'] as Map)[coordinate] =
            invalid;
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data();
      ((a.builder(raw)['properties'] as Map)['offset'] as Map).remove(
        coordinate,
      );
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final literal in ['1e999', '-1e999']) {
      final raw = data(dx: 9.87654321);
      final bytes = Uint8List.fromList(
        utf8.encode(jsonEncode(raw).replaceFirst('9.87654321', literal)),
      );
      expect(() => CanvasModel.decode(bytes), throwsFormatException);
    }
    for (final us in [-1, 9007199254740992]) {
      expect(() => f.decode(data(duration: us)), throwsFormatException);
    }
    for (final us in [0, 1, 9007199254740991]) {
      expect(() => f.decode(data(duration: us)), returnsNormally);
    }
    expect(() => f.decode(data(curve: 'invented')), throwsFormatException);
    for (final n in ['offset', 'curve', 'durationUs', 'onEnd']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map)[n] = {
        'kind': 'dartObjectReferencePresence',
        'rootSymbol': 'mustNotCrossWire',
      };
      expect(() => f.decode(raw), throwsFormatException);
    }
    final raw = data();
    (a.builder(raw)['properties'] as Map)['onEnd'] = {'kind': 'null'};
    expect(() => f.decode(raw), returnsNormally);
    for (final slot in ['sliver', 'children']) {
      final raw = data();
      a.builder(raw)['slots'] = {slot: f.single(null)};
      expect(() => f.decode(raw), throwsFormatException);
    }
  });
  for (final rtl in [false, true]) {
    for (final dx in [-2.5, 0.0, .25, 1.0]) {
      for (final dy in [-1.0, 0.0, .5]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'physical fractional paint without layout change dx=$dx dy=$dy rtl=$rtl tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(dx: dx, dy: dy, rtl: rtl, tight: tight, empty: empty),
                );
                final widget = tester.widget<AnimatedSlide>(native()),
                    transition = tester.widget<SlideTransition>(slide());
                expect(widget.offset, Offset(dx, dy));
                expect(widget.duration, const Duration(milliseconds: 300));
                expect(widget.curve, Curves.linear);
                expect(widget.onEnd, isNull);
                expect(transition.position.value, Offset(dx, dy));
                expect(transition.textDirection, isNull);
                expect(transition.transformHitTests, isTrue);
                final parent = tester.renderObject<RenderBox>(native());
                expect(
                  parent.size,
                  tight
                      ? const Size(240, 160)
                      : empty
                      ? Size.zero
                      : const Size(48, 48),
                );
                if (!empty) {
                  final child = tester.renderObject<RenderBox>(body());
                  expect(child.size, parent.size);
                  expect(
                    child.localToGlobal(Offset.zero, ancestor: parent),
                    Offset(parent.size.width * dx, parent.size.height * dy),
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
  for (final curve in [
    'linear',
    'decelerate',
    'fastLinearToSlowEaseIn',
    'fastEaseInToSlowEaseOut',
    'ease',
    'easeIn',
    'easeInToLinear',
    'easeInSine',
    'easeInQuad',
    'easeInCubic',
    'easeInQuart',
    'easeInQuint',
    'easeInExpo',
    'easeInCirc',
    'easeInBack',
    'easeOut',
    'linearToEaseOut',
    'easeOutSine',
    'easeOutQuad',
    'easeOutCubic',
    'easeOutQuart',
    'easeOutQuint',
    'easeOutExpo',
    'easeOutCirc',
    'easeOutBack',
    'easeInOut',
    'easeInOutSine',
    'easeInOutQuad',
    'easeInOutCubic',
    'easeInOutCubicEmphasized',
    'easeInOutQuart',
    'easeInOutQuint',
    'easeInOutExpo',
    'easeInOutCirc',
    'easeInOutBack',
    'fastOutSlowIn',
    'slowMiddle',
    'bounceIn',
    'bounceOut',
    'bounceInOut',
    'elasticIn',
    'elasticOut',
    'elasticInOut',
  ]) {
    for (final duration in [0, 1, 200000]) {
      for (final rtl in [false, true]) {
        testWidgets(
          'native unclamped transition curve=$curve duration=$duration rtl=$rtl',
          (tester) async {
            await f.pump(
              tester,
              data(
                dx: -.5,
                dy: .25,
                curve: curve,
                duration: duration,
                rtl: rtl,
              ),
            );
            final child = tester.element(body()),
                state = tester.state(native()),
                initialSize = tester.getSize(native());
            await f.pump(
              tester,
              data(
                dx: .5,
                dy: -.25,
                curve: curve,
                duration: duration,
                rtl: rtl,
              ),
            );
            if (duration > 1) {
              await tester.pump(Duration(microseconds: duration ~/ 4));
              final progress = tester
                  .widget<AnimatedSlide>(native())
                  .curve
                  .transform(.25);
              final expected = Offset.lerp(
                const Offset(-.5, .25),
                const Offset(.5, -.25),
                progress,
              )!;
              final actual = tester
                  .widget<SlideTransition>(slide())
                  .position
                  .value;
              expect(actual.dx, closeTo(expected.dx, 1e-12));
              expect(actual.dy, closeTo(expected.dy, 1e-12));
            }
            await tester.pump(Duration(microseconds: duration + 1));
            expect(
              tester.widget<SlideTransition>(slide()).position.value,
              const Offset(.5, -.25),
            );
            expect(tester.getSize(native()), initialSize);
            expect(identical(child, tester.element(body())), isTrue);
            expect(identical(state, tester.state(native())), isTrue);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets('inert project bindings are labeled with zero offset fallback', (
    tester,
  ) async {
    final raw = data();
    for (final field in ['offset', 'curve', 'durationUs', 'onEnd']) {
      (a.builder(raw)['properties'] as Map)[field] = {
        'kind': 'dartObjectReferencePresence',
      };
    }
    await f.pump(tester, raw);
    final widget = tester.widget<AnimatedSlide>(native());
    expect(widget.offset, Offset.zero);
    expect(widget.curve, Curves.linear);
    expect(widget.duration, const Duration(milliseconds: 300));
    expect(widget.onEnd, isNull);
    expect(
      find.byWidgetPredicate(
        (w) => w is Tooltip && w.message?.contains('AnimatedSlide') == true,
      ),
      findsWidgets,
    );
    final state = tester.state(native());
    await f.pump(tester, data(dx: .25));
    await tester.pumpAndSettle();
    expect(identical(state, tester.state(native())), isTrue);
    expect(tester.takeException(), isNull);
  });
  testWidgets('empty zero-size node retains drop and selection target', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true, dx: 2, dy: -1),
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
    'child resizing scales displacement without restarting a target tween',
    (tester) async {
      await f.pump(tester, data(dx: .5, dy: -.5));
      final state = tester.state(native());
      final raw = data(dx: .5, dy: -.5);
      final node = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
      node['properties'] = {'width': f.number(96), 'height': f.number(80)};
      await f.pump(tester, raw);
      final parent = tester.renderObject<RenderBox>(native()),
          child = tester.renderObject<RenderBox>(body());
      expect(parent.size, const Size(96, 80));
      expect(
        child.localToGlobal(Offset.zero, ancestor: parent),
        const Offset(48, -40),
      );
      expect(identical(state, tester.state(native())), isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'translated child remains selectable inside ancestor hit bounds',
    (tester) async {
      final raw = data(dx: .25, dy: .25), selected = <String>[];
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
        'child': f.single(null),
      };
      await f.pump(tester, raw, selected: selected.add);
      await tester.tapAt(tester.getCenter(body()));
      expect(selected, contains(f.bodyId));
      expect(tester.takeException(), isNull);
    },
  );
}
