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
  bool child = true,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(
    f.fillId,
    canvasBottomSheetType,
    {
      'enableDrag': f.boolean(false),
      'showDragHandle': f.boolean(false),
      'onClosing': d.s('noop'),
      'builder': d.s('child'),
      ...props,
    },
    {
      'child': f.single(
        child
            ? f.node(f.bodyId, 'flutter.widgets.SizedBox', {
                'width': f.number(200),
                'height': f.number(120),
              })
            : null,
      ),
    },
  );
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
  test('exact reviewed projection and Child drop destination', () {
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
        canvasBottomSheetType,
      ).map((s) => s.slotName),
      ['child'],
    );
  });
  for (final platform in TargetPlatform.values.where(
    (p) => p != TargetPlatform.fuchsia,
  )) {
    testWidgets('native sheet on $platform with safe defaults', (t) async {
      final raw = data();
      (raw['profile'] as Map)['targetPlatform'] = platform.name;
      await pump(t, raw);
      final sheet = t.widget<BottomSheet>(find.byType(BottomSheet));
      expect(sheet.enableDrag, false);
      expect(sheet.showDragHandle, false);
      expect(sheet.animationController, isNotNull);
      expect(sheet.animationController!.value, 1);
      expect(t.takeException(), isNull);
      await t.pumpWidget(const SizedBox());
      await t.pump();
      expect(t.takeException(), isNull);
    });
  }
  testWidgets('appearance, handle, native callbacks and controller disposal', (
    t,
  ) async {
    await pump(
      t,
      data(
        props: {
          'animationController': source,
          'enableDrag': f.boolean(true),
          'showDragHandle': f.boolean(true),
          'dragHandleColor': {'kind': 'color', 'argb': '0xFF123456'},
          'dragHandleSize': {'kind': 'size', 'width': 40, 'height': 6},
          'backgroundColor': {'kind': 'color', 'argb': '0xFFABCDEF'},
          'elevation': f.number(3),
          'onClosing': source,
          'onDragStart': source,
          'onDragEnd': source,
        },
      ),
    );
    final sheet = t.widget<BottomSheet>(find.byType(BottomSheet));
    expect(sheet.dragHandleSize, const Size(40, 6));
    expect(sheet.dragHandleColor, const Color(0xff123456));
    expect(sheet.backgroundColor, const Color(0xffabcdef));
    expect(sheet.elevation, 3);
    sheet.animationController!.value = 0.25;
    sheet.onDragEnd!(DragEndDetails(), isClosing: true);
    sheet.onClosing();
    await t.pumpAndSettle();
    expect(sheet.animationController!.value, 1);
    expect(t.takeException(), isNull);
    expect(find.byType(BottomSheet), findsOneWidget);
    expect(d.tooltips(t), contains('never executes project builders/events'));
    await pump(t, data());
    expect(t.takeException(), isNull);
    await t.pumpWidget(const SizedBox());
    await t.pump();
    expect(t.takeException(), isNull);
  });
  testWidgets('project builder has explicit placeholder not invented content', (
    t,
  ) async {
    await pump(t, data(props: {'builder': source}, child: false));
    expect(
      find.text('BottomSheet builder preview unavailable'),
      findsOneWidget,
    );
    expect(d.tooltips(t), contains('builder'));
    expect(t.takeException(), isNull);
  });
  testWidgets('empty sheet stays selectable', (t) async {
    final raw = data(child: false);
    raw['root'] = f.node(f.headerId, 'flutter.widgets.Column', {}, {
      'children': {
        'kind': 'list',
        'children': [raw['root']],
      },
    });
    await pump(t, raw);
    expect(
      find.byKey(ValueKey('canvas-zero-size-widget-target-${f.fillId}')),
      findsOneWidget,
    );
    expect(t.takeException(), isNull);
  });
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
    testWidgets('local $shape shape', (t) async {
      await pump(t, data(props: {'shapeKind': d.s(shape)}));
      expect(t.widget<BottomSheet>(find.byType(BottomSheet)).shape, isNotNull);
      expect(t.takeException(), isNull);
    });
  }
  for (final field in [
    'key',
    'dragHandleSize',
    'dragHandleColor',
    'constraints',
    'elevation',
    'shadowColor',
    'backgroundColor',
  ]) {
    testWidgets('source-owned $field is disclosed, never executed', (t) async {
      await pump(t, data(props: {field: source}));
      expect(find.byType(BottomSheet), findsOneWidget);
      expect(d.tooltips(t), contains(field));
      expect(t.takeException(), isNull);
    });
  }
  testWidgets('literal key and reset preserve native theme fallback', (
    t,
  ) async {
    await pump(
      t,
      data(
        props: {'key': d.s('sheet'), 'clipBehavior': d.en('Clip', 'antiAlias')},
      ),
    );
    expect(
      t.widget<BottomSheet>(find.byType(BottomSheet)).key,
      const ValueKey<String>('sheet'),
    );
    expect(
      t.widget<BottomSheet>(find.byType(BottomSheet)).clipBehavior,
      Clip.antiAlias,
    );
    await pump(t, data());
    final reset = t.widget<BottomSheet>(find.byType(BottomSheet));
    expect(reset.key, isNull);
    expect(reset.clipBehavior, isNull);
    expect(reset.shape, isNull);
    expect(reset.backgroundColor, isNull);
    expect(reset.constraints, isNull);
    expect(t.takeException(), isNull);
  });
  testWidgets('custom shape is explicitly unavailable', (t) async {
    await pump(t, data(props: {'shape': source}));
    expect(find.byType(BottomSheet), findsNothing);
    expect(find.textContaining('preview unavailable'), findsWidgets);
    expect(t.takeException(), isNull);
  });
  test('unsafe drag, builder and shape conflicts rejected', () {
    for (final p in ['enableDrag', 'showDragHandle']) {
      expect(
        () => f.decode(data(props: {p: f.boolean(true)})),
        throwsFormatException,
      );
    }
    expect(
      () => f.decode(data(props: {'showDragHandle': nil})),
      throwsFormatException,
    );
    expect(
      () => f.decode(data(props: {'builder': source})),
      throwsFormatException,
    );
    expect(
      () => f.decode(data(props: {'shape': nil, 'shapeKind': d.s('circle')})),
      throwsFormatException,
    );
    for (final p in ['builder', 'onClosing']) {
      expect(() => f.decode(data(props: {p: nil})), throwsFormatException);
    }
    for (final dims in [(-1, 4), (4, -1)]) {
      expect(
        () => f.decode(
          data(
            props: {
              'dragHandleSize': {
                'kind': 'size',
                'width': dims.$1,
                'height': dims.$2,
              },
            },
          ),
        ),
        throwsFormatException,
      );
    }
  });
}
