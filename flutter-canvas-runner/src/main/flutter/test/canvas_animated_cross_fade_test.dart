import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;
import 'canvas_animated_rotation_test.dart' as r;

const type = 'flutter.widgets.AnimatedCrossFade';
const secondId = '34077fd4-16d0-432b-b12e-c1329fd4c8a0';
Map<String, Object?> data({
  Map<String, Object?> properties = const {},
  bool second = false,
  bool rtl = false,
  double height = 80,
}) {
  final raw = c.data(rtl: rtl);
  final n = a.builder(raw);
  n['type'] = type;
  n['properties'] = <String, Object?>{
    'durationUs': {'kind': 'integer', 'value': 100000},
    'crossFadeState': {
      'kind': 'enum',
      'type': 'CrossFadeState',
      'value': second ? 'showSecond' : 'showFirst',
    },
    ...properties,
  };
  n['slots'] = {
    'firstChild': f.single(
      f.node(f.bodyId, 'flutter.widgets.SizedBox', {
        'width': f.number(48),
        'height': f.number(48),
      }),
    ),
    'secondChild': f.single(
      f.node(secondId, 'flutter.widgets.SizedBox', {
        'width': f.number(48),
        'height': f.number(height),
      }),
    ),
  };
  return raw;
}

Finder native() => find.byType(AnimatedCrossFade);
List<double> fades(WidgetTester t) => t
    .widgetList<FadeTransition>(
      find.descendant(of: native(), matching: find.byType(FadeTransition)),
    )
    .map((w) => w.opacity.value)
    .toList();
void main() {
  test('complete closed schema requires two children state and duration', () {
    expect(() => f.decode(data()), returnsNormally);
    for (final field in ['crossFadeState', 'durationUs']) {
      final raw = data();
      (a.builder(raw)['properties'] as Map).remove(field);
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final slot in ['firstChild', 'secondChild']) {
      final raw = data();
      (a.builder(raw)['slots'] as Map)[slot] = f.single(null);
      expect(() => f.decode(raw), throwsFormatException);
      (a.builder(raw)['slots'] as Map).remove(slot);
      expect(() => f.decode(raw), throwsFormatException);
    }
    for (final field in ['durationUs', 'reverseDurationUs']) {
      for (final bad in [-1, 9007199254740992, 1.5, true, '500']) {
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
    for (final field in [
      'alignment',
      'crossFadeState',
      'firstCurve',
      'secondCurve',
      'sizeCurve',
      'durationUs',
      'layoutBuilder',
      'excludeBottomFocus',
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
    for (final field in ['reverseDurationUs', 'onEnd']) {
      expect(
        () => f.decode(
          data(
            properties: {
              field: {'kind': 'null'},
            },
          ),
        ),
        returnsNormally,
      );
    }
    expect(
      () => f.decode(
        data(
          properties: {
            'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'none'},
          },
        ),
      ),
      throwsFormatException,
    );
    expect(
      () => f.decode(
        data(
          properties: {
            'crossFadeState': {
              'kind': 'enum',
              'type': 'CrossFadeState',
              'value': 'invalid',
            },
          },
        ),
      ),
      throwsFormatException,
    );
  });
  testWidgets(
    'native defaults both mounted keyed children and reversible progress',
    (t) async {
      await f.pump(t, data());
      final state = t.state(native());
      var w = t.widget<AnimatedCrossFade>(native());
      expect(w.alignment, Alignment.topCenter);
      expect(w.excludeBottomFocus, isTrue);
      expect(w.onEnd, isNull);
      expect(fades(t), containsAll([0.0, 1.0]));
      final keys = t
          .widgetList<Positioned>(
            find.descendant(of: native(), matching: find.byType(Positioned)),
          )
          .map((w) => w.key)
          .toSet();
      expect(
        keys,
        containsAll([
          const ValueKey(CrossFadeState.showFirst),
          const ValueKey(CrossFadeState.showSecond),
        ]),
      );
      await f.pump(t, data(second: true));
      await t.pump(const Duration(milliseconds: 50));
      for (final value in fades(t)) {
        expect(value, closeTo(.5, .01));
      }
      expect(identical(state, t.state(native())), isTrue);
      await f.pump(t, data(second: false));
      await t.pump(const Duration(milliseconds: 25));
      expect(fades(t).any((v) => v > 0 && v < 1), isTrue);
      await t.pumpAndSettle();
      expect(fades(t), containsAll([0.0, 1.0]));
      expect(identical(state, t.state(native())), isTrue);
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('independent reverse duration and excludeBottomFocus', (t) async {
    final p = <String, Object?>{
      'reverseDurationUs': {'kind': 'integer', 'value': 400000},
      'excludeBottomFocus': f.boolean(false),
    };
    await f.pump(t, data(second: true, properties: p));
    await f.pump(t, data(properties: p));
    await t.pump(const Duration(milliseconds: 100));
    expect(fades(t), containsAll([.25, .75]));
    final focuses = t.widgetList<ExcludeFocus>(
      find.descendant(of: native(), matching: find.byType(ExcludeFocus)),
    );
    expect(focuses.every((w) => !w.excluding), isTrue);
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
  });
  for (final rtl in [false, true]) {
    testWidgets('directional alignment and both layout extents rtl=$rtl', (
      t,
    ) async {
      await f.pump(
        t,
        data(
          rtl: rtl,
          properties: {'alignment': r.alignment(-2.5, 1, directional: true)},
        ),
      );
      var w = t.widget<AnimatedCrossFade>(native());
      expect(w.alignment, const AlignmentDirectional(-2.5, 1));
      final size = find.descendant(
        of: native(),
        matching: find.byType(AnimatedSize),
      );
      expect(t.getSize(size).height, 48);
      await f.pump(
        t,
        data(
          second: true,
          rtl: rtl,
          properties: {'alignment': r.alignment(-2.5, 1, directional: true)},
        ),
      );
      await t.pumpAndSettle();
      expect(t.getSize(size).height, 80);
      expect(t.takeException(), isNull);
    });
  }
  testWidgets(
    'project references are presence-only native defaults with visible explanation',
    (t) async {
      await f.pump(
        t,
        data(
          properties: {
            for (final field in [
              'alignment',
              'durationUs',
              'reverseDurationUs',
              'firstCurve',
              'secondCurve',
              'sizeCurve',
              'layoutBuilder',
              'onEnd',
            ])
              field: {'kind': 'dartObjectReferencePresence'},
          },
        ),
      );
      final w = t.widget<AnimatedCrossFade>(native());
      expect(w.duration, const Duration(milliseconds: 300));
      expect(w.reverseDuration, isNull);
      expect(w.alignment, Alignment.topCenter);
      expect(w.firstCurve, Curves.linear);
      expect(w.secondCurve, Curves.linear);
      expect(w.sizeCurve, Curves.linear);
      expect(w.onEnd, isNull);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip && w.message?.contains('AnimatedCrossFade') == true,
        ),
        findsWidgets,
      );
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'zero duration completes and two zero-size children do not lose owner selection',
    (t) async {
      final raw = data(
        second: true,
        height: 0,
        properties: {
          'durationUs': {'kind': 'integer', 'value': 0},
        },
      );
      final selected = <String>[];
      await f.pump(t, raw, selected: selected.add);
      await t.pumpAndSettle();
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-${a.builderId}'),
      );
      expect(handle, findsOneWidget);
      await t.tapAt(t.getCenter(handle));
      expect(selected, contains(a.builderId));
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'only top child is a Canvas drop destination; other child stays in model',
    (t) async {
      CanvasDropResolver? drop;
      for (final second in [false, true]) {
        await f.pump(t, data(second: second), drop: (value) => drop = value);
        await t.pumpAndSettle();
        final size = find.descendant(
          of: native(),
          matching: find.byType(AnimatedSize),
        );
        final point = t.getTopLeft(size) + const Offset(10, 10),
            surface = t.getRect(find.byType(CanvasDocumentView));
        final result = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: 'test',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        expect(result?.parentWidgetId, second ? secondId : f.bodyId);
        expect(result?.slotName, 'child');
        expect(t.takeException(), isNull);
      }
      expect(canvasDropSlotsForWidgetType(type), isEmpty);
      for (final slot in ['firstChild', 'secondChild']) {
        expect(
          canvasExistingChildWrapTargetSlot(
            parentWidgetType: type,
            slotName: slot,
          ),
          isNotNull,
        );
      }
    },
  );
  const curves = [
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
  ];
  for (final name in curves) {
    testWidgets('native three curves preserve pinned interpolation $name', (
      t,
    ) async {
      final p = {
        for (final field in ['firstCurve', 'secondCurve', 'sizeCurve'])
          field: {'kind': 'string', 'value': name},
      };
      await f.pump(t, data(properties: p));
      await f.pump(t, data(second: true, properties: p));
      for (var step = 0; step < 10; step++) {
        await t.pump(const Duration(milliseconds: 10));
        expect(t.takeException(), isNull);
      }
      await t.pumpAndSettle();
      expect(t.takeException(), isNull);
    });
  }
}
