import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_opacity_test.dart' as o;

const type = 'flutter.widgets.AnimatedRotation';
Map<String, Object?> alignment(
  double x,
  double y, {
  bool directional = false,
}) => {
  'kind': 'alignmentGeometry',
  'basis': directional ? 'directional' : 'physical',
  'horizontal': x,
  'vertical': y,
};
Map<String, Object?> data({
  double turns = 0,
  double? x,
  double y = 0,
  int duration = 300000,
  String? quality,
  String? curve,
  bool empty = false,
  bool tight = false,
  bool rtl = false,
}) {
  final raw = o.data(empty: empty, tight: tight, rtl: rtl),
      widget = a.builder(raw);
  widget['type'] = type;
  widget['properties'] = <String, Object?>{
    'turns': f.number(turns),
    'durationUs': {'kind': 'integer', 'value': duration},
    if (x != null) 'alignment': alignment(x, y),
    if (quality != null)
      'filterQuality': quality == 'null'
          ? {'kind': 'null'}
          : {'kind': 'enum', 'type': 'FilterQuality', 'value': quality},
    if (curve != null) 'curve': {'kind': 'string', 'value': curve},
  };
  raw['root'] = f.node(
    'ee156af3-65da-4f78-82a9-42f796c77726',
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

Finder native() => find.byType(AnimatedRotation);
Finder inner() => find
    .descendant(of: native(), matching: find.byType(RotationTransition))
    .first;
Finder transform() =>
    find.descendant(of: inner(), matching: find.byType(Transform)).first;
Finder body() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
void main() {
  test(
    'closed signed scalar physical Alignment enum and reference domains',
    () {
      for (final name in ['turns', 'durationUs']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['turns', 'alignment', 'durationUs', 'curve']) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final invalid in ['Infinity', 'NaN', null, true]) {
        final raw = data();
        ((a.builder(raw)['properties'] as Map)['turns'] as Map)['value'] =
            invalid;
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final literal in ['1e999', '-1e999']) {
        final raw = data(turns: 9.87654321);
        expect(
          () => CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(jsonEncode(raw).replaceFirst('9.87654321', literal)),
            ),
          ),
          throwsFormatException,
        );
      }
      final directional = data();
      (a.builder(directional)['properties'] as Map)['alignment'] = alignment(
        0,
        0,
        directional: true,
      );
      expect(() => f.decode(directional), throwsFormatException);
      for (final us in [-1, 9007199254740992]) {
        expect(() => f.decode(data(duration: us)), throwsFormatException);
      }
      for (final us in [0, 1, 9007199254740991]) {
        expect(() => f.decode(data(duration: us)), returnsNormally);
      }
      for (final field in [
        'turns',
        'alignment',
        'curve',
        'durationUs',
        'onEnd',
      ]) {
        final raw = data();
        (a.builder(raw)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
          'rootSymbol': 'mustNotCrossWire',
        };
        expect(() => f.decode(raw), throwsFormatException);
      }
      expect(() => f.decode(data(quality: 'invented')), throwsFormatException);
      expect(() => f.decode(data(curve: 'invented')), throwsFormatException);
      for (final slot in ['children', 'sliver']) {
        final raw = data();
        a.builder(raw)['slots'] = {slot: f.single(null)};
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final rtl in [false, true]) {
    for (final turns in [-2.5, -.25, 0.0, .125, .25, .5, 1.0, 2.0]) {
      for (final x in [-2.0, 0.0, 1.0]) {
        for (final tight in [false, true]) {
          for (final empty in [false, true]) {
            testWidgets(
              'physical paint turns=$turns alignment=$x rtl=$rtl tight=$tight empty=$empty',
              (tester) async {
                await f.pump(
                  tester,
                  data(
                    turns: turns,
                    x: x,
                    y: .5,
                    rtl: rtl,
                    tight: tight,
                    empty: empty,
                  ),
                );
                final widget = tester.widget<AnimatedRotation>(native()),
                    transition = tester.widget<RotationTransition>(inner());
                expect(widget.turns, turns);
                expect(widget.alignment, Alignment(x, .5));
                expect(widget.duration, const Duration(milliseconds: 300));
                expect(widget.curve, Curves.linear);
                expect(widget.onEnd, isNull);
                expect(transition.turns.value, turns);
                expect(
                  tester.widget<Transform>(transform()).transformHitTests,
                  isTrue,
                );
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
                  final pivot = Alignment(x, .5).alongSize(parent.size);
                  final origin = child.localToGlobal(
                    Offset.zero,
                    ancestor: parent,
                  );
                  final angle = turns * math.pi * 2,
                      c = math.cos(angle),
                      sn = math.sin(angle);
                  expect(
                    origin.dx,
                    closeTo(pivot.dx * (1 - c) + pivot.dy * sn, 1e-9),
                  );
                  expect(
                    origin.dy,
                    closeTo(pivot.dy * (1 - c) - pivot.dx * sn, 1e-9),
                  );
                  final point =
                      child.localToGlobal(
                        const Offset(10, 20),
                        ancestor: parent,
                      ) -
                      origin;
                  expect(point.dx, closeTo(10 * c - 20 * sn, 1e-9));
                  expect(point.dy, closeTo(10 * sn + 20 * c, 1e-9));
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
          'unclamped turns crosses zero curve=$curve duration=$duration rtl=$rtl',
          (tester) async {
            await f.pump(
              tester,
              data(turns: -.5, curve: curve, duration: duration, rtl: rtl),
            );
            final child = tester.element(body()),
                state = tester.state(native()),
                size = tester.getSize(native());
            await f.pump(
              tester,
              data(turns: .5, curve: curve, duration: duration, rtl: rtl),
            );
            if (duration > 1) {
              await tester.pump(Duration(microseconds: duration ~/ 4));
              final progress = tester
                  .widget<AnimatedRotation>(native())
                  .curve
                  .transform(.25);
              expect(
                tester.widget<RotationTransition>(inner()).turns.value,
                closeTo(-.5 + progress, 1e-12),
              );
            }
            await tester.pump(Duration(microseconds: duration + 1));
            expect(tester.widget<RotationTransition>(inner()).turns.value, .5);
            expect(tester.getSize(native()), size);
            expect(identical(child, tester.element(body())), isTrue);
            expect(identical(state, tester.state(native())), isTrue);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  for (final quality in [null, 'null', 'none', 'low', 'medium', 'high']) {
    testWidgets(
      'native filter lifecycle and immediate alignment quality=$quality',
      (tester) async {
        await f.pump(tester, data(quality: quality));
        final state = tester.state(native());
        expect(tester.widget<Transform>(transform()).filterQuality, isNull);
        await f.pump(tester, data(quality: quality, turns: 2));
        await tester.pump(const Duration(milliseconds: 75));
        final expected = quality == null || quality == 'null'
            ? null
            : FilterQuality.values.byName(quality);
        expect(tester.widget<Transform>(transform()).filterQuality, expected);
        final current = tester.widget<RotationTransition>(inner()).turns.value;
        await f.pump(tester, data(quality: quality, turns: 2, x: 1, y: -1));
        expect(tester.widget<RotationTransition>(inner()).turns.value, current);
        expect(
          tester.widget<Transform>(transform()).alignment,
          Alignment.topRight,
        );
        await tester.pumpAndSettle();
        expect(tester.widget<Transform>(transform()).filterQuality, isNull);
        expect(
          tester.widget<AnimatedRotation>(native()).filterQuality,
          expected,
        );
        expect(identical(state, tester.state(native())), isTrue);
        expect(tester.takeException(), isNull);
      },
    );
  }
  testWidgets('project bindings are inert and labeled with exact fallback', (
    tester,
  ) async {
    final raw = data();
    for (final field in [
      'turns',
      'alignment',
      'curve',
      'durationUs',
      'onEnd',
    ]) {
      (a.builder(raw)['properties'] as Map)[field] = {
        'kind': 'dartObjectReferencePresence',
      };
    }
    await f.pump(tester, raw);
    final widget = tester.widget<AnimatedRotation>(native());
    expect(widget.turns, 0);
    expect(widget.alignment, Alignment.center);
    expect(widget.curve, Curves.linear);
    expect(widget.duration, const Duration(milliseconds: 300));
    expect(widget.onEnd, isNull);
    expect(
      find.byWidgetPredicate(
        (w) => w is Tooltip && w.message?.contains('AnimatedRotation') == true,
      ),
      findsWidgets,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets('empty node retains selection and Child drop target', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    final selected = <String>[];
    await f.pump(
      tester,
      data(empty: true, turns: 0),
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
  for (final turns in [-1.0, -.25, 0.0, .125, .25, .5, 1.0]) {
    testWidgets('transformed child hit testing turns=$turns', (tester) async {
      final raw = data(turns: turns), selected = <String>[];
      f.findNode(raw['root'] as Map<String, Object?>, f.bodyId)['slots'] = {
        'child': f.single(null),
      };
      await f.pump(tester, raw, selected: selected.add);
      await tester.tapAt(tester.getCenter(body()));
      expect(selected, contains(f.bodyId));
      expect(tester.takeException(), isNull);
    });
  }
  for (final (begin, end, middle) in [
    (0.0, 1.0, .5),
    (.95, 1.05, 1.0),
    (.95, .05, .5),
    (0.0, -2.0, -1.0),
  ]) {
    testWidgets(
      'exact turns tween without modulo or shortest path $begin to $end',
      (tester) async {
        await f.pump(tester, data(turns: begin, duration: 200000));
        await f.pump(tester, data(turns: end, duration: 200000));
        await tester.pump(const Duration(milliseconds: 100));
        expect(
          tester.widget<RotationTransition>(inner()).turns.value,
          closeTo(middle, 1e-12),
        );
        await tester.pumpAndSettle();
        expect(tester.widget<RotationTransition>(inner()).turns.value, end);
        expect(tester.takeException(), isNull);
      },
    );
  }
  testWidgets(
    'extreme finite turns are quarantined and recover without losing child state',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()),
          element = tester.element(body()),
          size = tester.getSize(native());
      for (final turns in [1e307, -1e307, 1e308, -1e308, 0.0, .25]) {
        await f.pump(tester, data(turns: turns, duration: 0));
        await tester.pumpAndSettle();
        expect(tester.widget<AnimatedRotation>(native()).turns, turns);
        final warning = find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        );
        expect(
          warning,
          (turns * math.pi * 2).isFinite ? findsNothing : findsWidgets,
        );
        expect(identical(state, tester.state(native())), isTrue);
        expect(identical(element, tester.element(body())), isTrue);
        expect(tester.getSize(native()), size);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'native curve overshoot and extreme physical pivot cannot poison Canvas semantics',
    (tester) async {
      await f.pump(
        tester,
        data(turns: 0, curve: 'easeOutBack', duration: 1000000),
      );
      final state = tester.state(native()), element = tester.element(body());
      await f.pump(
        tester,
        data(turns: 2.8e307, curve: 'easeOutBack', duration: 1000000),
      );
      await tester.pump(const Duration(milliseconds: 600));
      await tester.pump();
      final transition = tester.widget<RotationTransition>(inner());
      expect((transition.turns.value * math.pi * 2).isFinite, isFalse);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
      await tester.pumpAndSettle();
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsNothing,
      );
      await f.pump(tester, data(turns: .25, x: 1e308, y: 1e308, duration: 0));
      await tester.pumpAndSettle();
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsWidgets,
      );
      expect(tester.takeException(), isNull);
      await f.pump(tester, data(turns: .25, duration: 0));
      await tester.pumpAndSettle();
      expect(identical(state, tester.state(native())), isTrue);
      expect(identical(element, tester.element(body())), isTrue);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('native rotation matrix is non-finite') ==
                  true,
        ),
        findsNothing,
      );
      expect(tester.takeException(), isNull);
    },
  );
}
