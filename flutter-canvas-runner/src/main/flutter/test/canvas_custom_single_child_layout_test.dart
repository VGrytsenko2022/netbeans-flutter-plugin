import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.CustomSingleChildLayout';
const presence = {'kind': 'dartObjectReferencePresence'};
Map<String, Object?> data({bool empty = false}) {
  final raw = c.data({}, empty), node = a.builder(raw);
  node['type'] = type;
  node['properties'] = {'delegate': presence};
  return raw;
}

void main() {
  test(
    'required strict delegate, one optional box child and redacted source',
    () {
      final section = canvasReviewedWidgetSchemaContract
          .split('W|$type\n')[1]
          .split('W|')[0];
      expect(
        section.split('\n').where((s) => s.startsWith('P|')),
        hasLength(1),
      );
      expect(section, contains('SingleChildLayoutDelegate:'));
      expect(canvasDropSlotsForWidgetType(type), hasLength(1));
      expect(() => f.decode(data()), returnsNormally);
      for (final props in [
        {},
        {
          'delegate': {'kind': 'null'},
        },
        {
          'delegate': {'kind': 'string', 'value': 'raw code'},
        },
        {'delegate': presence, 'unexpected': presence},
      ]) {
        final raw = data();
        a.builder(raw)['properties'] = props;
        expect(() => f.decode(raw), throwsFormatException);
      }
    },
  );
  for (final empty in [false, true]) {
    testWidgets('bounded preview, child and disclosure empty=$empty', (
      tester,
    ) async {
      await f.pump(tester, data(empty: empty));
      final finder = find.byType(CustomSingleChildLayout);
      final widget = tester.widget<CustomSingleChildLayout>(finder);
      final render = tester.renderObject<RenderCustomSingleChildLayoutBox>(
        finder,
      );
      expect(render.size, const Size(128, 96));
      expect(widget.child == null, empty);
      if (!empty) {
        expect(render.child!.size, const Size(48, 32));
        expect(
          (render.child!.parentData! as BoxParentData).offset,
          const Offset(40, 32),
        );
      }
      expect(
        tester
            .widgetList<Tooltip>(find.byType(Tooltip))
            .any(
              (t) =>
                  t.message?.contains('project delegate is not executed') ??
                  false,
            ),
        isTrue,
      );
      for (final constraints in [
        const BoxConstraints(),
        const BoxConstraints(maxWidth: 80, maxHeight: 50),
        const BoxConstraints.tightFor(width: 0, height: 0),
        const BoxConstraints.tightFor(width: 300, height: 200),
      ]) {
        final size = widget.delegate.getSize(constraints);
        expect(size.isFinite, isTrue);
        expect(constraints.isSatisfiedBy(size), isTrue);
        final childConstraints = widget.delegate.getConstraintsForChild(
          constraints,
        );
        expect(childConstraints.biggest, size);
      }
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('empty preview is selectable and accepts Child insertion', (
    tester,
  ) async {
    final selected = <String>[];
    CanvasDropResolver? resolver;
    await f.pump(
      tester,
      data(empty: true),
      selected: selected.add,
      drop: (v) => resolver = v,
    );
    final box = find.byType(CustomSingleChildLayout),
        point = tester.getCenter(box);
    await tester.tapAt(point);
    expect(selected, contains(a.builderId));
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'layout-child',
        widgetType: 'flutter.widgets.Text',
        traits: {},
      ),
    );
    expect(drop, isNotNull);
    expect(drop!.parentWidgetId, a.builderId);
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'refresh preserves native render identity and source-owned Child',
    (tester) async {
      await f.pump(tester, data());
      final render = tester.renderObject(find.byType(CustomSingleChildLayout));
      await f.pump(tester, data());
      expect(
        tester.renderObject(find.byType(CustomSingleChildLayout)),
        same(render),
      );
      expect(find.text('Sibling'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
