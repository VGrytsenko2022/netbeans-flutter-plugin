import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const types = ['flutter.widgets.Flow', 'flutter.widgets.Flow.unwrapped'];
const presence = {'kind': 'dartObjectReferencePresence'};
const ids = [
  '159ba5f6-601f-4fe8-a392-89f6f41502d1',
  '159ba5f6-601f-4fe8-a392-89f6f41502d2',
];
Map<String, Object?> data(String type, {int count = 2, Clip? clip}) {
  final raw = c.data(), n = a.builder(raw);
  n['type'] = type;
  n['properties'] = {
    'delegate': presence,
    if (clip != null)
      'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': clip.name},
  };
  n['slots'] = {
    'children': {
      'kind': 'list',
      'children': [
        for (var i = 0; i < count; i++)
          f.node(ids[i], 'flutter.widgets.SizedBox', {
            'width': f.number(48),
            'height': f.number(32),
          }),
      ],
    },
  };
  return raw;
}

List<Map<String, Object?>> children(Map<String, Object?> raw) =>
    (((a.builder(raw)['slots'] as Map)['children'] as Map)['children'] as List)
        .cast<Map<String, Object?>>();
Offset offset(RenderFlow r, RenderBox child) =>
    MatrixUtils.transformPoint(child.getTransformTo(r), Offset.zero);

void main() {
  test(
    'both constructors expose exact required delegate and optional ordinary children',
    () {
      expect(canvasReviewedWidgetSchemaContract, contains('FlowDelegate:'));
      for (final type in types) {
        expect(canvasDropSlotsForWidgetType(type), hasLength(1));
        expect(canvasReviewedRequiredWrapperSlot(type), isNull);
        for (final count in [0, 1, 2]) {
          expect(() => f.decode(data(type, count: count)), returnsNormally);
        }
        for (final bad in [
          null,
          {'kind': 'null'},
          {'kind': 'string', 'value': 'code'},
        ]) {
          final raw = data(type);
          a.builder(raw)['properties'] = bad == null ? {} : {'delegate': bad};
          expect(() => f.decode(raw), throwsFormatException);
        }
        final invalidClip = data(type);
        a.builder(invalidClip)['properties'] = {
          'delegate': presence,
          'clipBehavior': {'kind': 'null'},
        };
        expect(() => f.decode(invalidClip), throwsFormatException);
        for (final wrong in [
          'flutter.widgets.LayoutId',
          'flutter.widgets.PositionedTransition',
          'flutter.widgets.Expanded',
          'flutter.widgets.SliverList',
        ]) {
          final raw = data(type);
          children(raw)[0]['type'] = wrong;
          expect(() => f.decode(raw), throwsFormatException);
        }
      }
    },
  );
  for (final type in types) {
    for (final count in [0, 1, 2]) {
      for (final clip in Clip.values) {
        testWidgets('native $type children=$count clip=${clip.name}', (
          tester,
        ) async {
          await f.pump(tester, data(type, count: count, clip: clip));
          final finder = find.byType(Flow),
              w = tester.widget<Flow>(finder),
              r = tester.renderObject<RenderFlow>(finder);
          expect(r.size, const Size(240, 160));
          expect(r.childCount, count);
          expect(w.clipBehavior, clip);
          expect(r.clipBehavior, clip);
          if (count > 0) {
            expect(w.children.first is RepaintBoundary, type == types.first);
          }
          var child = r.firstChild;
          for (var i = 0; i < count; i++) {
            expect(child!.parentData, isA<FlowParentData>());
            expect(child.size, const Size(48, 32));
            expect(offset(r, child), Offset(i * 56, 0));
            child = (child.parentData! as FlowParentData).nextSibling;
          }
          for (final constraints in [
            const BoxConstraints(),
            const BoxConstraints.tightFor(width: 0, height: 0),
            const BoxConstraints(maxWidth: 80, maxHeight: 40),
          ]) {
            final size = w.delegate.getSize(constraints);
            expect(size.isFinite, isTrue);
            expect(constraints.isSatisfiedBy(size), isTrue);
            expect(
              w.delegate.getConstraintsForChild(0, constraints).maxWidth,
              lessThanOrEqualTo(48),
            );
          }
          expect(
            tester
                .widgetList<Tooltip>(find.byType(Tooltip))
                .any(
                  (t) =>
                      t.message?.contains(
                        'project FlowDelegate code is not executed',
                      ) ??
                      false,
                ),
            isTrue,
          );
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
  for (final type in types) {
    testWidgets('reorder identity transformed selection and move $type', (
      tester,
    ) async {
      final raw = data(type), selected = <String>[];
      CanvasMovePreviewResolver? move;
      await f.pump(tester, raw, selected: selected.add, move: (v) => move = v);
      final finder = find.byType(Flow),
          r = tester.renderObject<RenderFlow>(finder),
          first = r.firstChild!,
          second = r.lastChild!;
      expect(move!(ids[0], a.builderId, 'children', 1), isNotNull);
      await tester.tapAt(tester.getTopLeft(finder) + const Offset(60, 12));
      expect(selected, contains(ids[1]));
      a.builder(raw)['slots'] = {
        'children': {
          'kind': 'list',
          'children': children(raw).reversed.toList(),
        },
      };
      await f.pump(tester, raw);
      expect(tester.renderObject(finder), same(r));
      expect(r.firstChild, same(second));
      expect(r.lastChild, same(first));
      expect(offset(r, second), Offset.zero);
      expect(offset(r, first), const Offset(56, 0));
      a.builder(raw)['slots'] = {
        'children': {
          'kind': 'list',
          'children': [children(raw).first],
        },
      };
      await f.pump(tester, raw);
      expect(r.firstChild, same(second));
      expect(r.childCount, 1);
      expect(tester.takeException(), isNull);
    });
    testWidgets('empty and populated append drop remains available $type', (
      tester,
    ) async {
      for (final count in [0, 2]) {
        CanvasDropResolver? resolver;
        await f.pump(
          tester,
          data(type, count: count),
          drop: (v) => resolver = v,
        );
        final finder = find.byType(Flow), r = tester.renderObject(finder);
        final point = tester.getBottomRight(finder) - const Offset(8, 8),
            surface = tester.getRect(find.byType(CanvasDocumentView));
        final x = ((point.dx - surface.left) / surface.width * 1000000).round(),
            y = ((point.dy - surface.top) / surface.height * 1000000).round();
        final result = resolver!(
          x,
          y,
          CanvasPaletteDragSource(
            token: 'flow-drop',
            widgetType: 'flutter.widgets.Text',
            traits: {},
          ),
        );
        expect(result, isNotNull);
        expect(result!.parentWidgetId, a.builderId);
        expect(result.insertionIndex, count);
        for (final wrong in [
          'flutter.widgets.LayoutId',
          'flutter.widgets.PositionedTransition',
          'flutter.widgets.Expanded',
        ]) {
          final rejected = resolver!(
            x,
            y,
            CanvasPaletteDragSource(
              token: 'flow-invalid',
              widgetType: wrong,
              traits: {wrong},
            ),
          );
          expect(
            rejected?.parentWidgetId,
            isNot(a.builderId),
            reason: '$wrong target=$rejected',
          );
        }
        await f.pump(tester, data(type, count: count));
        expect(tester.renderObject(finder), same(r));
        expect(tester.takeException(), isNull);
      }
    });
  }
}
