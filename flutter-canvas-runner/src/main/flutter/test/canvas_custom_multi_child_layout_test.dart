import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.CustomMultiChildLayout';
const idType = 'flutter.widgets.LayoutId';
const presence = {'kind': 'dartObjectReferencePresence'};
const ids = [
  '159ba5f6-601f-4fe8-a392-89f6f41502d1',
  '159ba5f6-601f-4fe8-a392-89f6f41502d2',
];
Map<String, Object?> data({int count = 2}) {
  final raw = c.data(), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {'delegate': presence};
  n['slots'] = {
    'children': {
      'kind': 'list',
      'children': [
        for (int i = 0; i < count; i++)
          f.node(
            ids[i],
            idType,
            {
              'id': {'kind': 'string', 'value': 'source-id-$i'},
            },
            {
              'child': f.single(
                f.node(
                  '159ba5f6-601f-4fe8-a392-89f6f41502e${i + 1}',
                  'flutter.widgets.SizedBox',
                  {'width': f.number(48), 'height': f.number(32)},
                ),
              ),
            },
          ),
      ],
    },
  };
  return raw;
}

List<Map<String, Object?>> children(Map<String, Object?> raw) =>
    (((a.builder(raw)['slots'] as Map)['children'] as Map)['children'] as List)
        .cast<Map<String, Object?>>();
void main() {
  test('canonical schema exactly matches decoder', () {
    expect(
      canvasReviewedWidgetSchemaContract,
      contains('MultiChildLayoutDelegate:'),
    );
    expect(canvasDropSlotsForWidgetType(type), hasLength(1));
    expect(canvasReviewedRequiredWrapperSlot(idType), isNull);
    expect(canvasDropSlotsForWidgetType(idType), isEmpty);
    for (final count in [0, 1, 2]) {
      expect(() => f.decode(data(count: count)), returnsNormally);
    }
  });
  test(
    'rejects wrong parent, missing ID and duplicate Dart-equal literals',
    () {
      for (final values in [
        [
          {'kind': 'integer', 'value': 1},
          {'kind': 'double', 'value': 1.0},
        ],
        [
          {'kind': 'boolean', 'value': false},
          {'kind': 'boolean', 'value': false},
        ],
        [
          {'kind': 'string', 'value': ''},
          {'kind': 'string', 'value': ''},
        ],
      ]) {
        final raw = data();
        final list = children(raw);
        for (int i = 0; i < 2; i++) {
          list[i]['properties'] = {'id': values[i]};
        }
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final value in [
        null,
        {'kind': 'null'},
      ]) {
        final raw = data();
        children(raw)[0]['properties'] = value == null ? {} : {'id': value};
        expect(() => f.decode(raw), throwsFormatException);
      }
      final ordinary = data();
      a.builder(ordinary)['type'] = 'flutter.widgets.Column';
      a.builder(ordinary)['properties'] = {};
      expect(() => f.decode(ordinary), throwsFormatException);
      final wrongChild = data();
      children(wrongChild)[0]['type'] = 'flutter.widgets.SizedBox';
      children(wrongChild)[0]['properties'] = {};
      expect(() => f.decode(wrongChild), throwsFormatException);
      final refs = data();
      for (final n in children(refs)) {
        n['properties'] = {'id': presence};
      }
      expect(() => f.decode(refs), returnsNormally);
    },
  );
  for (final count in [0, 1, 2]) {
    testWidgets(
      'native parent data, finite size and private IDs count=$count',
      (tester) async {
        await f.pump(tester, data(count: count));
        final finder = find.byType(CustomMultiChildLayout).last;
        final w = tester.widget<CustomMultiChildLayout>(finder);
        final r = tester.renderObject<RenderCustomMultiChildLayoutBox>(finder);
        expect(r.size, const Size(240, 160));
        expect(r.childCount, count);
        var child = r.firstChild;
        for (int i = 0; i < count; i++) {
          final pd = child!.parentData! as MultiChildLayoutParentData;
          expect(pd.id, ids[i]);
          expect(pd.offset, Offset(0, i * 160 / count));
          expect(child.size, const Size(48, 32));
          child = pd.nextSibling;
        }
        expect(
          tester
              .widgetList<LayoutId>(
                find.descendant(of: finder, matching: find.byType(LayoutId)),
              )
              .map((x) => x.id),
          orderedEquals(ids.take(count)),
        );
        for (final constraints in [
          const BoxConstraints(),
          const BoxConstraints.tightFor(width: 0, height: 0),
          const BoxConstraints(maxWidth: 80, maxHeight: 40),
        ]) {
          expect(
            constraints.isSatisfiedBy(w.delegate.getSize(constraints)),
            isTrue,
          );
          expect(w.delegate.getSize(constraints).isFinite, isTrue);
        }
        expect(
          tester
              .widgetList<Tooltip>(find.byType(Tooltip))
              .any(
                (x) =>
                    x.message?.contains(
                      'project delegates and IDs are not executed',
                    ) ??
                    false,
              ),
          isTrue,
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
  testWidgets(
    'reorder retains native child identity and required child cannot be detached',
    (tester) async {
      final selected = <String>[];
      final raw = data();
      CanvasMovePreviewResolver? move;
      await f.pump(
        tester,
        raw,
        selected: selected.add,
        move: (value) => move = value,
      );
      final finder = find.byType(CustomMultiChildLayout).last;
      final render = tester.renderObject<RenderCustomMultiChildLayoutBox>(
        finder,
      );
      final first = render.firstChild!, second = render.lastChild!;
      expect(
        move!(
          '159ba5f6-601f-4fe8-a392-89f6f41502e1',
          a.builderId,
          'children',
          0,
        ),
        isNull,
      );
      expect(move!(ids[0], a.builderId, 'children', 1), isNotNull);
      final point = tester.getTopLeft(finder) + const Offset(12, 12);
      await tester.tapAt(point);
      expect(selected, contains('159ba5f6-601f-4fe8-a392-89f6f41502e1'));
      final list = children(raw).reversed.toList();
      a.builder(raw)['slots'] = {
        'children': {'kind': 'list', 'children': list},
      };
      await f.pump(tester, raw);
      expect(tester.renderObject(finder), same(render));
      expect(render.firstChild, same(second));
      expect(render.lastChild, same(first));
      expect(
        (second.parentData! as MultiChildLayoutParentData).offset,
        Offset.zero,
      );
      expect(
        (first.parentData! as MultiChildLayoutParentData).offset,
        const Offset(0, 80),
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'empty and populated drop accepts only LayoutId; stable render on refresh',
    (tester) async {
      for (final count in [0, 2]) {
        CanvasDropResolver? resolver;
        final selected = <String>[];
        await f.pump(
          tester,
          data(count: count),
          selected: selected.add,
          drop: (v) => resolver = v,
        );
        final finder = find.byType(CustomMultiChildLayout).last,
            p = tester.getBottomRight(finder) - const Offset(8, 8);
        final r = tester.renderObject(finder),
            surface = tester.getRect(find.byType(CanvasDocumentView));
        final x = ((p.dx - surface.left) / surface.width * 1000000).round(),
            y = ((p.dy - surface.top) / surface.height * 1000000).round();
        final result = resolver!(
          x,
          y,
          CanvasPaletteDragSource(
            token: 'id-drop',
            widgetType: idType,
            traits: {idType},
          ),
        );
        expect(result, isNotNull);
        expect(result!.parentWidgetId, a.builderId);
        final box = resolver!(
          x,
          y,
          CanvasPaletteDragSource(
            token: 'box-drop',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        expect(box?.parentWidgetId, isNot(a.builderId));
        await f.pump(tester, data(count: count));
        expect(tester.renderObject(finder), same(r));
        expect(tester.takeException(), isNull);
      }
    },
  );
}
