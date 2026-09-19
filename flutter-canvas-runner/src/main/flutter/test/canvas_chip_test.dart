import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_dialog_test.dart' as d;

const source = <String, Object?>{'kind': 'dartObjectReferencePresence'};
const nil = <String, Object?>{'kind': 'null'};
Map<String, Object?> data({
  Map<String, Object?> props = const {},
  bool icons = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasChipType, props, {
    'label': f.single(
      f.node(f.bodyId, 'flutter.widgets.Text', {'data': d.s('Label')}),
    ),
    'avatar': f.single(
      icons
          ? f.node(f.headerId, 'flutter.widgets.Text', {'data': d.s('A')})
          : null,
    ),
    'deleteIcon': f.single(
      icons
          ? f.node(
              'a37ca388-91b5-4754-821a-2a8518b3865b',
              'flutter.widgets.Text',
              {'data': d.s('Delete')},
            )
          : null,
    ),
  });
  return raw;
}

Future<void> pump(WidgetTester t, Map<String, Object?> raw) async {
  await t.pumpWidget(
    CanvasModelApp(
      model: f.decode(raw),
      selectedWidgetId: f.fillId,
      onSelected: (_) {},
    ),
  );
  await t.pumpAndSettle();
}

void main() {
  test('exact schema and seeded insertion/drop slots', () {
    final actual = canvasRuntimeWidgetSchemaContractForTesting().trim().split(
          '\n',
        ),
        expected = canvasReviewedWidgetSchemaContract.trim().split('\n');
    expect(actual.length, expected.length);
    for (var i = 0; i < actual.length; i++) {
      expect(actual[i], expected[i], reason: 'line $i');
    }
    expect(canvasReviewedRequiredWrapperSlot(canvasChipType), isNull);
    expect(
      canvasDropSlotsForWidgetType(canvasChipType).map((s) => s.slotName),
      ['label', 'avatar', 'deleteIcon'],
    );
  });
  for (final platform in TargetPlatform.values.where(
    (p) => p != TargetPlatform.fuchsia,
  )) {
    testWidgets('native chip $platform and inactive delete slot', (t) async {
      final raw = data(icons: true);
      (raw['profile'] as Map)['targetPlatform'] = platform.name;
      await pump(t, raw);
      expect(find.byType(Chip), findsOneWidget);
      expect(find.text('Label'), findsOneWidget);
      expect(find.text('A'), findsOneWidget);
      expect(find.text('Delete'), findsNothing);
      expect(t.takeException(), isNull);
    });
  }
  testWidgets(
    'deletion presence reveals icon but never deletes or autofocuses',
    (t) async {
      await pump(
        t,
        data(
          icons: true,
          props: {
            'onDeleted': source,
            'autofocus': f.boolean(true),
            'focusNode': source,
          },
        ),
      );
      final chip = t.widget<Chip>(find.byType(Chip));
      expect(chip.onDeleted, isNotNull);
      expect(chip.autofocus, false);
      expect(chip.focusNode, isNull);
      await t.tap(find.text('Delete'), warnIfMissed: false);
      await t.pumpAndSettle();
      expect(find.byType(Chip), findsOneWidget);
      expect(t.takeException(), isNull);
      await pump(t, data(icons: true, props: {'onDeleted': nil}));
      expect(find.text('Delete'), findsNothing);
      expect(t.takeException(), isNull);
    },
  );
  for (final field in [
    'key',
    'labelStyle',
    'labelPadding',
    'onDeleted',
    'deleteIconColor',
    'deleteButtonTooltipMessage',
    'side',
    'shape',
    'focusNode',
    'color',
    'backgroundColor',
    'padding',
    'visualDensity',
    'elevation',
    'shadowColor',
    'surfaceTintColor',
    'iconTheme',
    'avatarBoxConstraints',
    'deleteIconBoxConstraints',
    'chipAnimationStyle',
    'mouseCursor',
  ]) {
    testWidgets('isolates and names source $field', (t) async {
      await pump(t, data(props: {field: source}));
      expect(find.byType(Chip), findsOneWidget);
      expect(d.tooltips(t), contains(field));
      expect(t.takeException(), isNull);
    });
  }
  testWidgets(
    'local state color side cursor icon and text style preserve semantics',
    (t) async {
      await pump(
        t,
        data(
          props: {
            'onDeleted': source,
            'colorDefault': {'kind': 'color', 'argb': '0xFF123456'},
            'colorHovered': nil,
            'sideStateful': f.boolean(true),
            'sideWidth': f.number(2),
            'sideHoveredMode': d.s('inherit'),
            'mouseCursorDefault': d.s('basic'),
            'mouseCursorHovered': d.s('click'),
            'labelStyleFontSize': f.number(18),
            'iconThemeSize': f.number(24),
            'iconThemeOpacity': f.number(2),
            'visualDensityHorizontal': f.number(-2),
            'deleteButtonTooltipMessage': d.s(''),
          },
        ),
      );
      final chip = t.widget<Chip>(find.byType(Chip));
      expect(chip.color!.resolve({}), const Color(0xff123456));
      expect(chip.color!.resolve({WidgetState.hovered}), isNull);
      expect((chip.side as WidgetStateBorderSide).resolve({})!.width, 2);
      expect(
        (chip.side as WidgetStateBorderSide).resolve({WidgetState.hovered}),
        isNull,
      );
      expect(
        (chip.mouseCursor as WidgetStateMouseCursor).resolve({
          WidgetState.hovered,
        }),
        SystemMouseCursors.click,
      );
      expect(chip.visualDensity, VisualDensity(horizontal: -2));
      expect(chip.labelStyle!.fontSize, 18);
      expect(chip.iconTheme!.size, 24);
      expect(chip.deleteButtonTooltipMessage, '');
      expect(t.takeException(), isNull);
    },
  );
  testWidgets(
    'explicit null styles preserve theme fallback and animation is static',
    (t) async {
      await pump(
        t,
        data(
          props: {
            'labelStyle': nil,
            'iconTheme': nil,
            'chipAnimationStyle': source,
          },
        ),
      );
      final chip = t.widget<Chip>(find.byType(Chip));
      expect(chip.labelStyle, isNull);
      expect(chip.iconTheme, isNull);
      expect(chip.chipAnimationStyle!.enableAnimation!.duration, Duration.zero);
      expect(t.takeException(), isNull);
    },
  );
  for (final shape in [
    'roundedRectangle',
    'beveledRectangle',
    'continuousRectangle',
    'roundedSuperellipse',
    'circle',
    'oval',
    'stadium',
    'linear',
    'star',
    'polygon',
  ]) {
    testWidgets('native outlined shape $shape', (t) async {
      await pump(t, data(props: {'shapeKind': d.s(shape)}));
      expect(t.widget<Chip>(find.byType(Chip)).shape, isNotNull);
      expect(t.takeException(), isNull);
    });
  }
  test('rejects conflicting whole/local and malformed nested inputs', () {
    for (final props in [
      {'labelStyle': nil, 'labelStyleFontSize': f.number(18)},
      {'iconTheme': nil, 'iconThemeSize': f.number(24)},
      {
        'chipAnimationStyle': nil,
        'chipAnimationStyleEnableAnimation': d.s('noAnimation'),
      },
      {
        'chipAnimationStyleEnableAnimation': nil,
        'chipAnimationStyleEnableAnimationDurationUs': {
          'kind': 'integer',
          'value': 3,
        },
      },
      {'sideHoveredMode': d.s('border')},
      {'mouseCursorHovered': d.s('click')},
      {'clipBehavior': nil},
      {'autofocus': nil},
      {'elevation': f.number(-1)},
    ]) {
      expect(() => f.decode(data(props: props)), throwsFormatException);
    }
    final raw = data();
    ((raw['root'] as Map)['slots'] as Map)['label'] = f.single(null);
    expect(() => f.decode(raw), throwsFormatException);
  });
}
