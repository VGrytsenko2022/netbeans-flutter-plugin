import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_dialog_test.dart' as d;

Map<String, Object?> data({
  Map<String, Object?> props = const {},
  Map<String, Object?> optionProps = const {},
  bool children = true,
  int count = 1,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasSimpleDialogType, props, {
    'title': f.single(
      children
          ? f.node(f.headerId, 'flutter.widgets.Text', {'data': d.s('Title')})
          : null,
    ),
    'children': {
      'kind': 'list',
      'children': [
        if (children)
          for (var i = 0; i < count; i++)
            f.node(
              '00000000-0000-4000-8000-${i.toString().padLeft(12, '0')}',
              canvasSimpleDialogOptionType,
              optionProps,
              {
                'child': f.single(
                  f.node(
                    '00000000-0000-4001-8000-${i.toString().padLeft(12, '0')}',
                    'flutter.widgets.Text',
                    {'data': d.s('Choice $i')},
                  ),
                ),
              },
            ),
      ],
    },
  });
  return raw;
}

Future<void> pump(WidgetTester tester, Map<String, Object?> raw) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: f.decode(raw),
      selectedWidgetId: f.fillId,
      onSelected: (_) {},
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  test('exact canonical contracts and native drop slots', () {
    final actual = canvasRuntimeWidgetSchemaContractForTesting().trim().split(
      '\n',
    );
    final expected = canvasReviewedWidgetSchemaContract.trim().split('\n');
    expect(actual.length, expected.length);
    for (var i = 0; i < actual.length; i++) {
      expect(actual[i], expected[i], reason: 'line $i');
    }
    expect(
      canvasDropSlotsForWidgetType(
        canvasSimpleDialogType,
      ).map((s) => s.slotName),
      ['title', 'children'],
    );
    expect(
      canvasDropSlotsForWidgetType(
        canvasSimpleDialogOptionType,
      ).map((s) => s.slotName),
      ['child'],
    );
  });
  for (final platform in TargetPlatform.values.where(
    (p) => p != TargetPlatform.fuchsia,
  )) {
    testWidgets('native Material dialogs on $platform', (tester) async {
      final raw = data();
      (raw['profile'] as Map)['targetPlatform'] = platform.name;
      await pump(tester, raw);
      expect(find.byType(SimpleDialog), findsOneWidget);
      expect(find.byType(SimpleDialogOption), findsOneWidget);
      expect(find.text('Title'), findsOneWidget);
      expect(find.text('Choice 0'), findsOneWidget);
      final dialog = tester.widget<SimpleDialog>(find.byType(SimpleDialog));
      expect(dialog.titlePadding, const EdgeInsets.fromLTRB(24, 24, 24, 0));
      expect(dialog.contentPadding, const EdgeInsets.fromLTRB(0, 12, 0, 16));
      expect(
        tester
            .widget<SimpleDialogOption>(find.byType(SimpleDialogOption))
            .onPressed,
        isNull,
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('all appearance fields, local styles, option padding and reset', (
    tester,
  ) async {
    final props = <String, Object?>{
      'key': d.s('choice-dialog'),
      'titlePadding': d.insets,
      'contentPadding': d.insets,
      'insetPadding': d.insets,
      'semanticLabel': d.s('Select an option'),
      'elevation': f.number(4),
      'backgroundColor': {'kind': 'color', 'argb': '0xFF123456'},
      'shadowColor': {'kind': 'color', 'argb': '0xFF234567'},
      'surfaceTintColor': {'kind': 'color', 'argb': '0xFF345678'},
      'clipBehavior': d.en('Clip', 'antiAlias'),
      'shapeKind': d.s('stadium'),
      'alignment': {
        'kind': 'alignmentGeometry',
            'basis': 'directional',
            'horizontal': 0.5,
            'vertical': 0,
      },
      'constraints': {
        'kind': 'boxConstraints',
        'minWidth': 100,
        'maxWidth': 400,
        'minHeight': 0,
        'maxHeight': 600,
      },
      for (final family in ['titleTextStyle', 'contentTextStyle']) ...{
        '${family}FontSize': f.number(21),
        '${family}FontFamily': d.s('Roboto'),
        '${family}DecorationUnderline': f.boolean(true),
      },
    };
    await pump(
      tester,
      data(
        props: props,
        optionProps: {
          'key': d.s('option'),
          'padding': d.insets,
          'onPressed': d.presence,
        },
      ),
    );
    final dialog = tester.widget<SimpleDialog>(find.byType(SimpleDialog));
    expect(dialog.key, const ValueKey<String>('choice-dialog'));
    expect(dialog.shape, isA<StadiumBorder>());
    expect(dialog.titleTextStyle?.fontSize, 21);
    expect(dialog.contentTextStyle?.decoration, TextDecoration.underline);
    expect(dialog.titlePadding, const EdgeInsets.fromLTRB(8, 10, 12, 14));
    expect(dialog.contentPadding, dialog.titlePadding);
    expect(dialog.insetPadding, dialog.titlePadding);
    expect(dialog.backgroundColor, const Color(0xff123456));
    expect(dialog.shadowColor, const Color(0xff234567));
    expect(dialog.surfaceTintColor, const Color(0xff345678));
    expect(dialog.elevation, 4);
    expect(dialog.clipBehavior, Clip.antiAlias);
    expect(dialog.semanticLabel, 'Select an option');
    expect(dialog.alignment, const AlignmentDirectional(0.5, 0));
    expect(dialog.constraints?.maxWidth, 400);
    final option = tester.widget<SimpleDialogOption>(
      find.byType(SimpleDialogOption),
    );
    expect(option.key, const ValueKey<String>('option'));
    expect(option.padding, dialog.insetPadding);
    expect(option.onPressed, isNotNull);
    option.onPressed!();
    expect(find.byType(SimpleDialog), findsOneWidget);
    await pump(tester, data());
    final reset = tester.widget<SimpleDialog>(find.byType(SimpleDialog));
    expect(reset.shape, isNull);
    expect(reset.titleTextStyle, isNull);
    expect(reset.backgroundColor, isNull);
    expect(
      tester
          .widget<SimpleDialogOption>(find.byType(SimpleDialogOption))
          .onPressed,
      isNull,
    );
    expect(tester.takeException(), isNull);
  });
  for (final shape in d.kinds) {
    testWidgets('native shape $shape', (tester) async {
      await pump(tester, data(props: {'shapeKind': d.s(shape)}));
      expect(
        tester.widget<SimpleDialog>(find.byType(SimpleDialog)).shape,
        isNotNull,
      );
      expect(tester.takeException(), isNull);
    });
  }
  for (final field in [
    'key',
    'titlePadding',
    'contentPadding',
    'insetPadding',
    'titleTextStyle',
    'contentTextStyle',
    'semanticLabel',
    'alignment',
    'constraints',
    'backgroundColor',
    'shadowColor',
    'surfaceTintColor',
    'elevation',
  ]) {
    testWidgets('source $field is disclosed and never evaluated', (
      tester,
    ) async {
      await pump(tester, data(props: {field: d.presence}));
      expect(d.tooltips(tester), contains(field));
      expect(d.tooltips(tester), contains('never executes'));
      expect(find.byType(SimpleDialog), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets(
    'nullable sources, shape unavailable and option enabled presence',
    (tester) async {
      await pump(
        tester,
        data(
          optionProps: {
            'key': d.presence,
            'padding': d.presence,
            'onPressed': d.presence,
          },
        ),
      );
      expect(
        tester
            .widget<SimpleDialogOption>(find.byType(SimpleDialogOption))
            .onPressed,
        isNotNull,
      );
      await pump(tester, data(props: {'shape': d.presence}));
      expect(find.byType(SimpleDialog), findsNothing);
      expect(find.textContaining('preview unavailable'), findsWidgets);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('long choices scroll without overflow', (tester) async {
    await pump(tester, data(count: 40));
    expect(find.byType(SimpleDialogOption), findsNWidgets(40));
    final scroll = find.descendant(
      of: find.byType(SimpleDialog),
      matching: find.byType(SingleChildScrollView),
    );
    expect(scroll, findsOneWidget);
    await tester.drag(scroll, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
  for (final type in [canvasSimpleDialogType, canvasSimpleDialogOptionType]) {
    testWidgets('zero size $type remains selectable and droppable', (
      tester,
    ) async {
      final raw = data(children: false);
      raw['root'] = f.node(f.headerId, 'flutter.widgets.Center', {}, {
        'child': f.single(
          f.node(
            f.bodyId,
            'flutter.widgets.SizedBox',
            {'width': f.number(0), 'height': f.number(0)},
            {
              'child': f.single(
                f.node(
                  f.fillId,
                  type,
                  {},
                  type == canvasSimpleDialogType
                      ? {
                          'title': f.single(null),
                          'children': {'kind': 'list', 'children': []},
                        }
                      : {'child': f.single(null)},
                ),
              ),
            },
          ),
        ),
      });
      final selected = <String>[];
      CanvasDropResolver? drop;
      await tester.pumpWidget(
        CanvasModelApp(
          model: f.decode(raw),
          selectedWidgetId: f.fillId,
          onSelected: selected.add,
          onDropResolverChanged: (v) => drop = v,
        ),
      );
      await tester.pump();
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-${f.fillId}'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      expect(selected, contains(f.fillId));
      final surface = tester.getRect(find.byType(CanvasDocumentView)),
          point = tester.getCenter(handle);
      final target = drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: canvasWidgetTraitsForType('flutter.widgets.Text'),
        ),
      );
      expect(target?.parentWidgetId, f.fillId);
      expect(
        target?.slotName,
        isIn(
          type == canvasSimpleDialogType ? ['title', 'children'] : ['child'],
        ),
      );
      expect(tester.takeException(), isNull);
    });
  }
  test('padding and whole/local validation match Java', () {
    const directional = {
      'kind': 'edgeInsetsDirectional',
      'start': 1,
      'top': 2,
      'end': 3,
      'bottom': 4,
    };
    for (final name in ['titlePadding', 'contentPadding']) {
      expect(() => f.decode(data(props: {name: directional})), returnsNormally);
      expect(() => f.decode(data(props: {name: d.nil})), throwsFormatException);
    }
    expect(
      () => f.decode(data(props: {'insetPadding': directional})),
      throwsFormatException,
    );
    expect(
      () => f.decode(data(optionProps: {'padding': directional})),
      throwsFormatException,
    );
    expect(
      () => f.decode(data(optionProps: {'padding': d.nil, 'onPressed': d.nil})),
      returnsNormally,
    );
    expect(
      () => f.decode(data(props: {'shape': d.nil, 'shapeKind': d.s('circle')})),
      throwsFormatException,
    );
    for (final family in ['titleTextStyle', 'contentTextStyle']) {
      expect(
        () => f.decode(
          data(props: {family: d.nil, '${family}FontSize': f.number(16)}),
        ),
        throwsFormatException,
      );
    }
  });
}
