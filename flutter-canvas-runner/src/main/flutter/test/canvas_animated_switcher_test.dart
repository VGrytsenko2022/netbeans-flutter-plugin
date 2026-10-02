import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const type = 'flutter.widgets.AnimatedSwitcher';
const otherId = '34077fd4-16d0-432b-b12e-c1329fd4c8a0';
Map<String, Object?> data({
  Map<String, Object?> properties = const {},
  String? childType = 'flutter.widgets.SizedBox',
  String childId = f.bodyId,
  bool rtl = false,
  double height = 48,
}) {
  final raw = c.data(rtl: rtl), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'durationUs': {'kind': 'integer', 'value': 100000},
    ...properties,
  };
  n['slots'] = {
    'child': f.single(
      childType == null ? null : child(childType, childId, height),
    ),
  };
  return raw;
}

Map<String, Object?> child(String kind, String id, double height) => f.node(
  id,
  kind,
  kind == 'flutter.widgets.SizedBox'
      ? {'width': f.number(48), 'height': f.number(height)}
      : kind == 'flutter.widgets.Text'
      ? {
          'data': {'kind': 'string', 'value': 'Current'},
        }
      : {},
);
Finder native() => find.byType(AnimatedSwitcher);
List<double> fades(WidgetTester t) => t
    .widgetList<FadeTransition>(
      find.descendant(of: native(), matching: find.byType(FadeTransition)),
    )
    .map((w) => w.opacity.value)
    .toList();

void main() {
  test('closed six-field schema optional child and duration domains', () {
    expect(() => f.decode(data()), returnsNormally);
    expect(() => f.decode(data(childType: null)), returnsNormally);
    final missing = data();
    (a.builder(missing)['properties'] as Map).remove('durationUs');
    expect(() => f.decode(missing), throwsFormatException);
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
      'durationUs',
      'switchInCurve',
      'switchOutCurve',
      'transitionBuilder',
      'layoutBuilder',
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
            'reverseDurationUs': {'kind': 'null'},
          },
        ),
      ),
      returnsNormally,
    );
    for (final field in ['onEnd', 'alignment', 'clipBehavior']) {
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
    expect(canvasDropSlotsForWidgetType(type).single.slotName, 'child');
  });
  testWidgets(
    'initial child is fully visible; same native type updates without transition despite new Stable ID',
    (t) async {
      await f.pump(t, data());
      final state = t.state(native());
      expect(fades(t), [1.0]);
      await f.pump(t, data(childId: otherId, height: 70));
      expect(fades(t), [1.0]);
      expect(identical(state, t.state(native())), isTrue);
      expect(t.getSize(native()).height, 70);
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'type replacement crossfades; rapid A B A and Undo IDs never collide',
    (t) async {
      await f.pump(t, data());
      await f.pump(t, data(childType: 'flutter.widgets.Text'));
      await t.pump(const Duration(milliseconds: 30));
      expect(fades(t), containsAll([.7, .3]));
      await f.pump(t, data());
      await t.pump(const Duration(milliseconds: 10));
      expect(fades(t).length, 3, reason: 'fades after A-B-A: ${fades(t)}');
      for (final kind in [
        'flutter.widgets.Text',
        'flutter.widgets.SizedBox',
        'flutter.widgets.Text',
        'flutter.widgets.SizedBox',
      ]) {
        await f.pump(t, data(childType: kind));
        await t.pump(const Duration(milliseconds: 5));
        expect(t.takeException(), isNull);
      }
      await t.pumpAndSettle();
      expect(fades(t), [1.0]);
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('entry captures reverse duration and curves when created', (
    t,
  ) async {
    final p = {
      'reverseDurationUs': {'kind': 'integer', 'value': 400000},
    };
    await f.pump(t, data(properties: p));
    await f.pump(
      t,
      data(
        childType: 'flutter.widgets.Text',
        properties: {
          'reverseDurationUs': {'kind': 'integer', 'value': 100000},
        },
      ),
    );
    await t.pump(const Duration(milliseconds: 100));
    expect(fades(t), containsAll([.75, 1.0]));
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
  });
  testWidgets(
    'removal retains fading child then empty wrapper remains selectable',
    (t) async {
      final selected = <String>[];
      await f.pump(t, data(), selected: selected.add);
      await f.pump(t, data(childType: null), selected: selected.add);
      expect(fades(t).length, 1);
      await t.pumpAndSettle();
      expect(fades(t), isEmpty);
      // An empty native Stack takes the bounded maximum extent; it is not
      // necessarily zero sized. The ordinary Canvas selection surface remains.
      expect(t.getSize(native()), const Size(240, 160));
      await t.tapAt(t.getCenter(native()));
      expect(selected, contains(a.builderId));
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('zero duration replacement and null reverse complete cleanly', (
    t,
  ) async {
    final p = {
      'durationUs': {'kind': 'integer', 'value': 0},
      'reverseDurationUs': {'kind': 'null'},
    };
    await f.pump(t, data(properties: p));
    await f.pump(t, data(childType: 'flutter.widgets.Text', properties: p));
    await t.pumpAndSettle();
    expect(fades(t), [1.0]);
    expect(t.takeException(), isNull);
  });
  testWidgets('all project references are presence-only native defaults', (
    t,
  ) async {
    await f.pump(
      t,
      data(
        properties: {
          for (final field in [
            'durationUs',
            'reverseDurationUs',
            'switchInCurve',
            'switchOutCurve',
            'transitionBuilder',
            'layoutBuilder',
          ])
            field: {'kind': 'dartObjectReferencePresence'},
        },
      ),
    );
    final w = t.widget<AnimatedSwitcher>(native());
    expect(w.duration, const Duration(milliseconds: 300));
    expect(w.reverseDuration, isNull);
    expect(w.switchInCurve, Curves.linear);
    expect(w.switchOutCurve, Curves.linear);
    expect(
      find.byWidgetPredicate(
        (w) => w is Tooltip && w.message?.contains('AnimatedSwitcher') == true,
      ),
      findsWidgets,
    );
    expect(t.takeException(), isNull);
  });
  for (final rtl in [false, true]) {
    testWidgets(
      'default layout keeps maximum outgoing extent and centers rtl=$rtl',
      (t) async {
        await f.pump(t, data(height: 100, rtl: rtl));
        await f.pump(t, data(childType: 'flutter.widgets.Text', rtl: rtl));
        await t.pump(const Duration(milliseconds: 50));
        expect(t.getSize(native()).height, 100);
        await t.pumpAndSettle();
        expect(t.getSize(native()).height, lessThan(100));
        expect(t.takeException(), isNull);
      },
    );
  }
  testWidgets(
    'moving current child out while outgoing entry remains uses distinct instrumentation keys',
    (t) async {
      Map<String, Object?> moved(bool outside) {
        final raw = data(
          childType: outside ? null : 'flutter.widgets.SizedBox',
        );
        final n = a.builder(raw),
            switcher = Map<String, Object?>.from(a.builder(raw));
        n['type'] = 'flutter.widgets.Column';
        n['id'] = otherId;
        n['properties'] = <String, Object?>{};
        n['slots'] = {
          'children': {
            'kind': 'list',
            'children': [
              switcher,
              if (outside) child('flutter.widgets.SizedBox', f.bodyId, 48),
            ],
          },
        };
        return raw;
      }

      await f.pump(t, moved(false));
      final state = t.state(native());
      await f.pump(t, moved(true));
      await t.pump(const Duration(milliseconds: 10));
      expect(identical(state, t.state(native())), isTrue);
      expect(fades(t).length, 1);
      expect(t.takeException(), isNull);
      await f.pump(
        t,
        moved(false),
      ); // Undo while the original entry is still fading.
      await t.pump(const Duration(milliseconds: 10));
      expect(fades(t).length, 2);
      expect(t.takeException(), isNull);
      await t.pumpAndSettle();
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'nested switchers retain separate entries and release all keys after removal',
    (t) async {
      Map<String, Object?> nested(bool empty) {
        final raw = data();
        if (!empty) {
          (a.builder(raw)['slots'] as Map)['child'] = f.single(
            f.node(
              otherId,
              type,
              {
                'durationUs': {'kind': 'integer', 'value': 100000},
              },
              {
                'child': f.single(
                  child('flutter.widgets.SizedBox', f.bodyId, 48),
                ),
              },
            ),
          );
        }
        return raw;
      }

      await f.pump(t, nested(false));
      expect(native(), findsNWidgets(2));
      await f.pump(t, nested(true));
      await t.pump(const Duration(milliseconds: 20));
      expect(t.takeException(), isNull);
      await f.pump(t, nested(false));
      await t.pump(const Duration(milliseconds: 10));
      expect(t.takeException(), isNull);
      await t.pumpAndSettle();
      expect(native(), findsNWidgets(2));
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'pinned SDK default transition has repeated-key limitation; unkeyed outer builder avoids it',
    (t) async {
      Widget direct(Widget child, {bool isolated = false}) => MaterialApp(
        home: Center(
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 100),
            transitionBuilder: isolated
                ? (child, animation) => KeyedSubtree(
                    child: AnimatedSwitcher.defaultTransitionBuilder(
                      child,
                      animation,
                    ),
                  )
                : AnimatedSwitcher.defaultTransitionBuilder,
            child: child,
          ),
        ),
      );
      await t.pumpWidget(direct(const SizedBox(width: 48, height: 48)));
      await t.pump();
      await t.pumpWidget(direct(const Text('B')));
      await t.pump(const Duration(milliseconds: 20));
      // Flutter 3.44.8 KeyedSubtree.wrap uses the default FadeTransition's key,
      // which is ValueKey(null) for every unkeyed generated child.
      expect(fades(t).length, 1);
      expect(t.takeException(), isNull);
      await t.pumpAndSettle();
      await t.pumpWidget(direct(const Text('B'), isolated: true));
      await t.pump();
      await t.pumpWidget(
        direct(const SizedBox(width: 48, height: 48), isolated: true),
      );
      await t.pump(const Duration(milliseconds: 20));
      expect(fades(t).length, 2);
      expect(t.takeException(), isNull);
      await t.pumpAndSettle();
    },
  );
  testWidgets(
    'outgoing child cannot take selection or drop from new active geometry',
    (t) async {
      CanvasDropResolver? drop;
      final selected = <String>[];
      await f.pump(
        t,
        data(height: 100),
        selected: selected.add,
        drop: (d) => drop = d,
      );
      await f.pump(
        t,
        data(childType: 'flutter.widgets.Text', childId: otherId),
        selected: selected.add,
        drop: (d) => drop = d,
      );
      await t.pump(const Duration(milliseconds: 10));
      final surface = t.getRect(find.byType(CanvasDocumentView)),
          point = t.getCenter(native());
      final hit = drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'test',
          widgetType: 'flutter.widgets.Text',
          traits: {},
        ),
      );
      expect(hit?.parentWidgetId, isNot(f.bodyId));
      final excludes = t.widgetList<ExcludeFocus>(
        find.descendant(of: native(), matching: find.byType(ExcludeFocus)),
      );
      expect(excludes.any((w) => w.excluding), isTrue);
      expect(t.takeException(), isNull);
    },
  );
  for (final kind in ['flutter.material.MenuBar', 'flutter.material.Tooltip']) {
    testWidgets(
      'retained $kind subtrees isolate preview controller and overlay GlobalKeys',
      (t) async {
        Map<String, Object?> overlay(bool inside) {
          final raw = data();
          if (inside) {
            (a.builder(raw)['slots'] as Map)['child'] = f.single(
              f.node(
                otherId,
                kind,
                kind == 'flutter.material.Tooltip'
                    ? {
                        'message': {
                          'kind': 'string',
                          'value': 'Retained tooltip',
                        },
                      }
                    : {},
                kind == 'flutter.material.MenuBar'
                    ? {
                        'children': {'kind': 'list', 'children': <Object?>[]},
                      }
                    : {
                        'child': f.single(
                          child('flutter.widgets.SizedBox', f.bodyId, 48),
                        ),
                      },
              ),
            );
          }
          return raw;
        }

        await f.pump(t, overlay(true));
        await f.pump(t, overlay(false));
        await t.pump(const Duration(milliseconds: 20));
        await f.pump(t, overlay(true));
        await t.pump(const Duration(milliseconds: 10));
        expect(t.takeException(), isNull);
        await t.pumpAndSettle();
        expect(t.takeException(), isNull);
      },
    );
  }
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
    testWidgets('native switch curves including overshoot remain valid $name', (
      t,
    ) async {
      final p = {
        for (final field in ['switchInCurve', 'switchOutCurve'])
          field: {'kind': 'string', 'value': name},
      };
      await f.pump(t, data(properties: p));
      await f.pump(t, data(childType: 'flutter.widgets.Text', properties: p));
      for (var step = 0; step < 10; step++) {
        await t.pump(const Duration(milliseconds: 10));
        expect(t.takeException(), isNull);
      }
      await t.pumpAndSettle();
      expect(t.takeException(), isNull);
    });
  }
}
