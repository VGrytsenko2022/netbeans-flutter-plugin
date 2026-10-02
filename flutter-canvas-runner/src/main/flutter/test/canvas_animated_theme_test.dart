import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const type = 'flutter.material.AnimatedTheme';
Map<String, Object?> data({
  String preset = 'light',
  Map<String, Object?> properties = const {},
  bool empty = false,
  bool rtl = false,
}) {
  final raw = c.data(rtl: rtl), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'data': {'kind': 'string', 'value': preset},
    ...properties,
  };
  n['slots'] = {
    'child': f.single(
      empty
          ? null
          : f.node(f.bodyId, 'flutter.widgets.Text', {
              'data': {'kind': 'string', 'value': 'Themed child'},
            }),
    ),
  };
  return raw;
}

Finder native() => find
    .ancestor(
      of: find.text('Themed child'),
      matching: find.byType(AnimatedTheme),
    )
    .first;
ThemeData theme(WidgetTester t) =>
    Theme.of(t.element(find.text('Themed child')));
ThemeData presetTheme(String name) => switch (name.replaceAll('M2', '')) {
  'dark' => ThemeData.dark(useMaterial3: !name.endsWith('M2')),
  'light' => ThemeData.light(useMaterial3: !name.endsWith('M2')),
  _ => ThemeData.fallback(useMaterial3: !name.endsWith('M2')),
};
void main() {
  test(
    'closed four-property schema optional duration required data and child',
    () {
      expect(() => f.decode(data()), returnsNormally);
      expect(() => f.decode(data(empty: true)), throwsFormatException);
      final missing = data();
      (a.builder(missing)['properties'] as Map).remove('data');
      expect(() => f.decode(missing), throwsFormatException);
      for (final field in ['data', 'curve', 'durationUs']) {
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
              'onEnd': {'kind': 'null'},
            },
          ),
        ),
        returnsNormally,
      );
      for (final bad in [-1, 9007199254740992, 1.5, true, '500']) {
        expect(
          () => f.decode(
            data(
              properties: {
                'durationUs': {'kind': 'integer', 'value': bad},
              },
            ),
          ),
          throwsFormatException,
        );
      }
      for (final good in [0, 1, 9007199254740991]) {
        expect(
          () => f.decode(
            data(
              properties: {
                'durationUs': {'kind': 'integer', 'value': good},
              },
            ),
          ),
          returnsNormally,
        );
      }
      for (final bad in [
        'Theme.of(context)',
        'ThemeData()',
        'Light',
        'darkM3',
      ]) {
        expect(() => f.decode(data(preset: bad)), throwsFormatException);
      }
      expect(canvasDropSlotsForWidgetType(type), isEmpty);
      expect(canvasReviewedRequiredWrapperSlot(type), 'child');
    },
  );
  for (final preset in [
    'light',
    'dark',
    'fallback',
    'lightM2',
    'darkM2',
    'fallbackM2',
  ]) {
    for (final rtl in [false, true]) {
      testWidgets(
        'factory $preset exactly matches SDK theme on initial mount rtl=$rtl',
        (t) async {
          await f.pump(t, data(preset: preset, rtl: rtl));
          final w = t.widget<AnimatedTheme>(native()),
              expected = presetTheme(preset);
          expect(w.duration, const Duration(milliseconds: 200));
          expect(w.curve, Curves.linear);
          expect(w.onEnd, isNull);
          expect(w.data, expected);
          expect(theme(t).colorScheme, expected.colorScheme);
          expect(theme(t).useMaterial3, expected.useMaterial3);
          expect(
            IconTheme.of(t.element(find.text('Themed child'))).color,
            expected.iconTheme.color,
          );
          expect(t.takeException(), isNull);
        },
      );
    }
  }
  testWidgets('theme lerp midpoint retargeting and child State survive edits', (
    t,
  ) async {
    await f.pump(t, data());
    final state = t.state(native()),
        element = t.element(find.text('Themed child'));
    await f.pump(t, data(preset: 'dark'));
    await t.pump(const Duration(milliseconds: 100));
    final expected = ThemeData.lerp(ThemeData.light(), ThemeData.dark(), .5);
    expect(theme(t).colorScheme, expected.colorScheme);
    expect(identical(state, t.state(native())), isTrue);
    expect(identical(element, t.element(find.text('Themed child'))), isTrue);
    await f.pump(t, data());
    await t.pumpAndSettle();
    expect(theme(t).colorScheme, ThemeData.light().colorScheme);
    expect(t.takeException(), isNull);
  });
  testWidgets('zero duration updates immediately; reset returns 200ms', (
    t,
  ) async {
    await f.pump(t, data());
    await f.pump(
      t,
      data(
        preset: 'dark',
        properties: {
          'durationUs': {'kind': 'integer', 'value': 0},
        },
      ),
    );
    await t.pumpAndSettle();
    expect(theme(t).brightness, Brightness.dark);
    await f.pump(t, data());
    expect(
      t.widget<AnimatedTheme>(native()).duration,
      const Duration(milliseconds: 200),
    );
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
  });
  testWidgets(
    'project ThemeData and other references are inert labeled defaults',
    (t) async {
      await f.pump(
        t,
        data(
          properties: {
            for (final field in ['data', 'curve', 'durationUs', 'onEnd'])
              field: {'kind': 'dartObjectReferencePresence'},
          },
        ),
      );
      final w = t.widget<AnimatedTheme>(native());
      expect(w.data, ThemeData.fallback());
      expect(w.duration, const Duration(milliseconds: 200));
      expect(w.curve, Curves.linear);
      expect(w.onEnd, isNull);
      expect(
        find.byWidgetPredicate(
          (w) =>
              w is Tooltip &&
              w.message?.contains('ThemeData.fallback()') == true,
        ),
        findsWidgets,
      );
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('nearest AnimatedTheme replaces parent data without merging', (
    t,
  ) async {
    final raw = data();
    raw['root'] = f.node(
      'a302c576-a0f7-40d3-9c8d-cbb261671c73',
      type,
      {
        'data': {'kind': 'string', 'value': 'darkM2'},
      },
      {'child': f.single(raw['root'] as Map<String, Object?>)},
    );
    await f.pump(t, raw);
    expect(theme(t).brightness, Brightness.light);
    expect(theme(t).useMaterial3, isTrue);
    expect(t.takeException(), isNull);
  });
  testWidgets(
    'zero-size required child still has a Designer selection handle',
    (t) async {
      final raw = data();
      (a.builder(raw)['slots'] as Map)['child'] = f.single(
        f.node(f.bodyId, 'flutter.widgets.SizedBox', {
          'width': f.number(0),
          'height': f.number(0),
        }),
      );
      final selected = <String>[];
      await f.pump(t, raw, selected: selected.add);
      await t.pumpAndSettle();
      final handles = find.byWidgetPredicate(
        (w) =>
            w.key.toString().contains('canvas-zero-size-widget-target') &&
            w.key.toString().contains(a.builderId),
      );
      expect(handles, findsWidgets);
      await t.tapAt(t.getCenter(handles.first));
      expect(selected, contains(a.builderId));
      expect(t.takeException(), isNull);
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
    testWidgets(
      'native ThemeData tween preserves pinned curve $name across M2/M3',
      (t) async {
        final properties = {
          'curve': {'kind': 'string', 'value': name},
        };
        await f.pump(t, data(preset: 'lightM2', properties: properties));
        await f.pump(t, data(preset: 'dark', properties: properties));
        for (var frame = 0; frame < 20; frame++) {
          await t.pump(const Duration(milliseconds: 10));
          expect(t.takeException(), isNull);
        }
        await t.pumpAndSettle();
        expect(theme(t).colorScheme, ThemeData.dark().colorScheme);
        expect(t.takeException(), isNull);
      },
    );
  }
}
