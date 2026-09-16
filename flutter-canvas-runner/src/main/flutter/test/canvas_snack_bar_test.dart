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
  Map<String, Object?> actionProps = const {},
  bool action = true,
  bool standalone = false,
}) {
  final raw = f.modelJson();
  final button = f.node(f.headerId, canvasSnackBarActionType, {
    'label': d.s('Undo'),
    'onPressed': d.s('noop'),
    ...actionProps,
  });
  raw['root'] = standalone
      ? button
      : f.node(
          f.fillId,
          canvasSnackBarType,
          {'animation': f.number(1), ...props},
          {
            'content': f.single(
              f.node(f.bodyId, 'flutter.widgets.Text', {
                'data': d.s('Message'),
              }),
            ),
            'action': f.single(action ? button : null),
          },
        );
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
  test('reviewed schema and exact action destination', () {
    final actual = canvasRuntimeWidgetSchemaContractForTesting().trim().split(
      '\n',
    );
    final expected = canvasReviewedWidgetSchemaContract.trim().split('\n');
    expect(actual.length, expected.length);
    for (var i = 0; i < actual.length; i++) {
      expect(actual[i], expected[i], reason: 'line $i');
    }
    expect(
      canvasDropSlotsForWidgetType(canvasSnackBarType).map((s) => s.slotName),
      ['content', 'action'],
    );
  });
  for (final platform in TargetPlatform.values.where(
    (p) => p != TargetPlatform.fuchsia,
  )) {
    testWidgets('native snackbar $platform', (t) async {
      final raw = data();
      (raw['profile'] as Map)['targetPlatform'] = platform.name;
      await pump(t, raw);
      final bar = t.widget<SnackBar>(find.byType(SnackBar));
      expect(bar.content, isNotNull);
      expect(bar.action!.label, 'Undo');
      expect(bar.persist, true);
      expect(bar.animation!.value, 1);
      expect(bar.duration, const Duration(seconds: 4));
      expect(find.text('Message'), findsOneWidget);
      expect(t.takeException(), isNull);
    });
  }
  testWidgets('literals floating layout nullable resets and selection', (
    t,
  ) async {
    await pump(
      t,
      data(
        props: {
          'key': d.s('bar'),
          'backgroundColor': {'kind': 'color', 'argb': '0xFF123456'},
          'elevation': f.number(3),
          'behavior': d.en('SnackBarBehavior', 'floating'),
          'width': f.number(320),
          'actionOverflowThreshold': f.number(0),
          'showCloseIcon': f.boolean(true),
          'closeIconColor': {'kind': 'color', 'argb': '0xFFABCDEF'},
          'persist': f.boolean(false),
          'durationUs': {'kind': 'integer', 'value': 1234567},
          'hitTestBehavior': d.en('HitTestBehavior', 'translucent'),
          'clipBehavior': d.en('Clip', 'antiAlias'),
          'dismissDirection': d.en('DismissDirection', 'horizontal'),
          'animation': f.number(0),
        },
      ),
      selected: f.headerId,
    );
    final bar = t.widget<SnackBar>(find.byType(SnackBar));
    expect(bar.key, const ValueKey<String>('bar'));
    expect(bar.width, 320);
    expect(bar.elevation, 3);
    expect(bar.backgroundColor, const Color(0xff123456));
    expect(bar.closeIconColor, const Color(0xffabcdef));
    expect(bar.duration, const Duration(microseconds: 1234567));
    expect(bar.persist, false);
    expect(bar.animation!.value, 1);
    expect(bar.hitTestBehavior, HitTestBehavior.translucent);
    expect(bar.clipBehavior, Clip.antiAlias);
    expect(bar.actionOverflowThreshold, 0);
    expect(bar.dismissDirection, DismissDirection.horizontal);
    final key =
        t.widget<SnackBarAction>(find.byType(SnackBarAction)).key as GlobalKey;
    expect(key.currentContext!.findRenderObject(), isNotNull);
    expect(t.takeException(), isNull);
    await pump(
      t,
      data(
        action: false,
        props: {'showCloseIcon': nil, 'persist': nil, 'animation': nil},
      ),
    );
    final reset = t.widget<SnackBar>(find.byType(SnackBar));
    expect(reset.action, isNull);
    expect(reset.persist, false);
    expect(reset.width, isNull);
    expect(reset.shape, isNull);
    expect(t.takeException(), isNull);
  });
  testWidgets('activation and dismissal do not remove design', (t) async {
    await pump(
      t,
      data(
        props: {
          'showCloseIcon': f.boolean(true),
          'durationUs': {'kind': 'integer', 'value': 1},
        },
      ),
    );
    await t.tap(find.text('Undo'), warnIfMissed: false);
    await t.pumpAndSettle();
    await t.drag(
      find.byType(SnackBar),
      const Offset(0, 200),
      warnIfMissed: false,
    );
    await t.pumpAndSettle();
    await t.tap(find.byIcon(Icons.close), warnIfMissed: false);
    await t.pump(const Duration(seconds: 10));
    expect(find.byType(SnackBar), findsOneWidget);
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
    testWidgets('local $shape', (t) async {
      await pump(t, data(props: {'shapeKind': d.s(shape)}));
      expect(t.widget<SnackBar>(find.byType(SnackBar)).shape, isNotNull);
      expect(t.takeException(), isNull);
    });
  }
  for (final field in [
    'key',
    'backgroundColor',
    'elevation',
    'margin',
    'padding',
    'width',
    'actionOverflowThreshold',
    'closeIconColor',
    'durationUs',
    'animation',
    'onVisible',
  ]) {
    testWidgets('source $field never evaluated', (t) async {
      await pump(
        t,
        data(
          props: {
            'behavior': d.en('SnackBarBehavior', 'floating'),
            field: source,
          },
        ),
      );
      expect(find.byType(SnackBar), findsOneWidget);
      expect(d.tooltips(t), contains(field));
      expect(t.takeException(), isNull);
    });
  }
  testWidgets(
    'action alone preserves literal fields and source labels are disclosed',
    (t) async {
      await pump(
        t,
        data(
          standalone: true,
          actionProps: {
            'key': d.s('action'),
            'textColor': {'kind': 'color', 'argb': '0xFF123456'},
          },
        ),
        selected: f.headerId,
      );
      var action = t.widget<SnackBarAction>(find.byType(SnackBarAction));
      expect(action.label, 'Undo');
      expect(action.key, const ValueKey<String>('action'));
      expect(action.textColor, const Color(0xff123456));
      await pump(
        t,
        data(
          standalone: true,
          actionProps: {'label': source, 'onPressed': source},
        ),
        selected: f.headerId,
      );
      action = t.widget<SnackBarAction>(find.byType(SnackBarAction));
      expect(action.label, 'Project label preview unavailable');
      expect(d.tooltips(t), contains('label'));
      expect(t.takeException(), isNull);
    },
  );
  testWidgets('custom shape explicit unavailable placeholder', (t) async {
    await pump(t, data(props: {'shape': source}));
    expect(find.byType(SnackBar), findsNothing);
    expect(find.textContaining('preview unavailable'), findsWidgets);
    expect(t.takeException(), isNull);
  });
  test('invalid geometry nulls and exact action fail closed', () {
    for (final props in [
      {'width': f.number(12)},
      {
        'behavior': d.en('SnackBarBehavior', 'floating'),
        'width': f.number(12),
        'margin': source,
      },
      {'actionOverflowThreshold': f.number(1.01)},
      {'elevation': f.number(-1)},
      {'clipBehavior': nil},
      {'durationUs': nil},
      {'animation': f.number(-1)},
      {'shape': nil, 'shapeKind': d.s('circle')},
    ]) {
      expect(() => f.decode(data(props: props)), throwsFormatException);
    }
    for (final name in ['label', 'onPressed']) {
      expect(
        () => f.decode(data(actionProps: {name: nil})),
        throwsFormatException,
      );
    }
    final raw = data();
    final root = raw['root'] as Map;
    final slots = root['slots'] as Map;
    slots['action'] = f.single(
      f.node(f.headerId, 'flutter.widgets.Text', {'data': d.s('bad')}),
    );
    expect(() => f.decode(raw), throwsFormatException);
    slots['action'] = f.single(null);
    slots['content'] = f.single(null);
    expect(() => f.decode(raw), throwsFormatException);
  });
}
