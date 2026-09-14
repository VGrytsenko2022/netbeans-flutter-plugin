import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_theme_test.dart' as t;

const type = 'flutter.material.Theme';

Map<String, Object?> data({
  String preset = 'light',
  Map<String, Object?> properties = const {},
  bool empty = false,
  bool rtl = false,
}) {
  final raw = t.data(
    preset: preset,
    properties: properties,
    empty: empty,
    rtl: rtl,
  );
  a.builder(raw)['type'] = type;
  return raw;
}

Finder native() => find
    .ancestor(of: find.text('Themed child'), matching: find.byType(Theme))
    .first;

void main() {
  test(
    'one required ThemeData property and required wrapper child, no animation',
    () {
      expect(() => f.decode(data()), returnsNormally);
      expect(() => f.decode(data(empty: true)), throwsFormatException);
      final missing = data();
      (a.builder(missing)['properties'] as Map).remove('data');
      expect(() => f.decode(missing), throwsFormatException);
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': 'Theme.of(context)'},
        {'kind': 'string', 'value': 'darkM3'},
      ]) {
        expect(
          () => f.decode(data(properties: {'data': bad})),
          throwsFormatException,
        );
      }
      for (final field in ['curve', 'durationUs', 'onEnd']) {
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
      testWidgets('native preset $preset and inherited IconTheme rtl=$rtl', (
        tester,
      ) async {
        await f.pump(tester, data(preset: preset, rtl: rtl));
        final theme = tester.widget<Theme>(native());
        final expected = t.presetTheme(preset);
        expect(theme.data, expected);
        final context = tester.element(find.text('Themed child'));
        expect(Theme.of(context).colorScheme, expected.colorScheme);
        expect(Theme.of(context).useMaterial3, expected.useMaterial3);
        expect(IconTheme.of(context).color, expected.iconTheme.color);
        expect(tester.takeException(), isNull);
      });
    }
  }

  testWidgets('edits apply immediately without replacing child Element', (
    tester,
  ) async {
    await f.pump(tester, data());
    final element = tester.element(find.text('Themed child'));
    for (final preset in ['darkM2', 'light', 'fallbackM2', 'dark']) {
      await f.pump(tester, data(preset: preset));
      expect(t.theme(tester).colorScheme, t.presetTheme(preset).colorScheme);
      expect(tester.element(find.text('Themed child')), same(element));
      expect(tester.takeException(), isNull);
    }
  });

  testWidgets('project ThemeData stays inert with a labeled fallback', (
    tester,
  ) async {
    await f.pump(
      tester,
      data(
        properties: {
          'data': {'kind': 'dartObjectReferencePresence'},
        },
      ),
    );
    expect(tester.widget<Theme>(native()).data, ThemeData.fallback());
    expect(
      find.byWidgetPredicate(
        (w) =>
            w is Tooltip && w.message?.contains('ThemeData.fallback()') == true,
      ),
      findsWidgets,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('nearest Theme replaces outer Theme rather than merging', (
    tester,
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
    await f.pump(tester, raw);
    expect(t.theme(tester).brightness, Brightness.light);
    expect(t.theme(tester).useMaterial3, isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets('zero-size required child keeps a Theme selection handle', (
    tester,
  ) async {
    final raw = data();
    (a.builder(raw)['slots'] as Map)['child'] = f.single(
      f.node(f.bodyId, 'flutter.widgets.SizedBox', {
        'width': f.number(0),
        'height': f.number(0),
      }),
    );
    final selected = <String>[];
    await f.pump(tester, raw, selected: selected.add);
    await tester.pumpAndSettle();
    final handles = find.byWidgetPredicate(
      (w) =>
          w.key.toString().contains('canvas-zero-size-widget-target') &&
          w.key.toString().contains(a.builderId),
    );
    expect(handles, findsWidgets);
    await tester.tapAt(tester.getCenter(handles.first));
    expect(selected, contains(a.builderId));
    expect(tester.takeException(), isNull);
  });
}
