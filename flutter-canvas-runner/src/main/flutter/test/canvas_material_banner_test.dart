import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_dialog_test.dart' as d;

const source = <String, Object?>{'kind': 'dartObjectReferencePresence'};
const nil = <String, Object?>{'kind': 'null'};
const leadingId = 'a37ca388-91b5-4754-821a-2a8518b3865b';
const labelId = 'a37ca388-91b5-4754-821a-2a8518b3865c';
Map<String, Object?> data({
  Map<String, Object?> props = const {},
  int actions = 1,
  bool leading = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasMaterialBannerType, props, {
    'content': f.single(
      f.node(f.bodyId, 'flutter.widgets.Text', {'data': d.s('Message')}),
    ),
    'leading': f.single(
      leading
          ? f.node(leadingId, 'flutter.widgets.Text', {'data': d.s('Lead')})
          : null,
    ),
    'actions': {
      'kind': 'list',
      'children': [
        if (actions > 0)
          f.node(
            f.headerId,
            'flutter.material.TextButton',
            {
              'enabled': f.boolean(true),
              'variant': d.s('standard'),
              'onPressed': source,
            },
            {
              'child': f.single(
                f.node(labelId, 'flutter.widgets.Text', {
                  'data': d.s('Action'),
                }),
              ),
            },
          ),
        if (actions > 1)
          f.node(
            'a37ca388-91b5-4754-821a-2a8518b3865d',
            'flutter.widgets.Text',
            {'data': d.s('Second')},
          ),
      ],
    },
  });
  return raw;
}

Future<void> pump(
  WidgetTester t,
  Map<String, Object?> raw, {
  String? selected,
}) async {
  await t.pumpWidget(
    CanvasModelApp(
      model: f.decode(raw),
      selectedWidgetId: selected ?? f.fillId,
      onSelected: (_) {},
    ),
  );
  await t.pumpAndSettle();
}

void main() {
  test(
    'exact reviewed schema, seeded insertion and three drop destinations',
    () {
      final actual = canvasRuntimeWidgetSchemaContractForTesting().trim().split(
        '\n',
      );
      final expected = canvasReviewedWidgetSchemaContract.trim().split('\n');
      expect(actual.length, expected.length);
      for (var i = 0; i < actual.length; i++) {
        expect(actual[i], expected[i], reason: 'line $i');
      }
      expect(
        canvasReviewedRequiredWrapperSlot(canvasMaterialBannerType),
        isNull,
      );
      expect(
        canvasDropSlotsForWidgetType(
          canvasMaterialBannerType,
        ).map((s) => s.slotName),
        ['content', 'actions', 'leading'],
      );
    },
  );
  for (final platform in TargetPlatform.values.where(
    (p) => p != TargetPlatform.fuchsia,
  )) {
    testWidgets('native static banner on $platform', (t) async {
      final raw = data();
      (raw['profile'] as Map)['targetPlatform'] = platform.name;
      await pump(t, raw);
      final bar = t.widget<MaterialBanner>(find.byType(MaterialBanner));
      expect(bar.animation, isNull);
      expect(bar.minActionBarHeight, 52);
      expect(bar.forceActionsBelow, false);
      expect(bar.actions.length, 1);
      expect(find.text('Message'), findsOneWidget);
      expect(t.takeException(), isNull);
    });
  }
  testWidgets('all local appearance fields style leading and action layout', (
    t,
  ) async {
    await pump(
      t,
      data(
        actions: 2,
        leading: true,
        props: {
          'key': d.s('banner'),
          'elevation': f.number(3),
          for (final name in [
            'backgroundColor',
            'surfaceTintColor',
            'shadowColor',
            'dividerColor',
          ])
            name: {'kind': 'color', 'argb': '0xFF123456'},
          for (final name in ['padding', 'margin', 'leadingPadding'])
            name: {
              'kind': 'edgeInsetsDirectional',
              'start': 2.0,
              'top': 3.0,
              'end': 4.0,
              'bottom': 5.0,
            },
          'contentTextStyleFontSize': f.number(18),
          'contentTextStyleFontWeight': d.en('FontWeight', 'w600'),
          'forceActionsBelow': f.boolean(true),
          'overflowAlignment': d.en('OverflowBarAlignment', 'center'),
          'minActionBarHeight': f.number(60),
          'animation': f.number(0),
          'onVisible': source,
        },
      ),
      selected: f.headerId,
    );
    final bar = t.widget<MaterialBanner>(find.byType(MaterialBanner));
    expect(bar.key, const ValueKey<String>('banner'));
    expect(bar.elevation, 3);
    expect(bar.contentTextStyle!.fontSize, 18);
    expect(bar.contentTextStyle!.fontWeight, FontWeight.w600);
    for (final color in [
      bar.backgroundColor,
      bar.surfaceTintColor,
      bar.shadowColor,
      bar.dividerColor,
    ]) {
      expect(color, const Color(0xff123456));
    }
    expect(bar.padding, const EdgeInsetsDirectional.fromSTEB(2, 3, 4, 5));
    expect(bar.leading, isNotNull);
    expect(bar.actions.length, 2);
    expect(bar.forceActionsBelow, true);
    expect(bar.overflowAlignment, OverflowBarAlignment.center);
    expect(bar.minActionBarHeight, 60);
    expect(bar.animation!.value, 1);
    expect(bar.onVisible, isNull);
    await t.tap(find.text('Action'), warnIfMissed: false);
    await t.pumpAndSettle();
    expect(find.byType(MaterialBanner), findsOneWidget);
    expect(t.takeException(), isNull);
  });
  for (final name in [
    'key',
    'contentTextStyle',
    'elevation',
    'backgroundColor',
    'surfaceTintColor',
    'shadowColor',
    'dividerColor',
    'padding',
    'margin',
    'leadingPadding',
    'animation',
    'onVisible',
    'minActionBarHeight',
  ]) {
    testWidgets('source $name is isolated and disclosed', (t) async {
      await pump(t, data(props: {name: source}));
      expect(find.byType(MaterialBanner), findsOneWidget);
      expect(d.tooltips(t), contains(name));
      expect(t.takeException(), isNull);
    });
  }
  testWidgets('explicit nulls preserve static mode and theme fallback', (
    t,
  ) async {
    await pump(
      t,
      data(
        props: {
          for (final name in [
            'key',
            'contentTextStyle',
            'elevation',
            'backgroundColor',
            'surfaceTintColor',
            'shadowColor',
            'dividerColor',
            'padding',
            'margin',
            'leadingPadding',
            'animation',
            'onVisible',
          ])
            name: nil,
        },
      ),
    );
    final bar = t.widget<MaterialBanner>(find.byType(MaterialBanner));
    expect(bar.animation, isNull);
    expect(bar.contentTextStyle, isNull);
    expect(bar.backgroundColor, isNull);
    expect(t.takeException(), isNull);
  });
  for (final alignment in OverflowBarAlignment.values) {
    testWidgets('overflow $alignment', (t) async {
      await pump(
        t,
        data(
          actions: 2,
          props: {
            'forceActionsBelow': f.boolean(true),
            'overflowAlignment': d.en('OverflowBarAlignment', alignment.name),
          },
        ),
      );
      expect(
        t.widget<MaterialBanner>(find.byType(MaterialBanner)).overflowAlignment,
        alignment,
      );
      expect(t.takeException(), isNull);
    });
  }
  test('invalid slots numbers and whole/local styles fail closed', () {
    expect(() => f.decode(data(actions: 0)), throwsFormatException);
    for (final props in [
      {'minActionBarHeight': nil},
      {'forceActionsBelow': nil},
      {'overflowAlignment': nil},
      {'minActionBarHeight': f.number(-1)},
      {'elevation': f.number(-1)},
      {'animation': f.number(1.1)},
      {'contentTextStyle': nil, 'contentTextStyleFontSize': f.number(18)},
      {'contentTextStylePackage': d.s('fonts')},
    ]) {
      expect(() => f.decode(data(props: props)), throwsFormatException);
    }
    final raw = data();
    ((raw['root'] as Map)['slots'] as Map)['content'] = f.single(null);
    expect(() => f.decode(raw), throwsFormatException);
  });
}
