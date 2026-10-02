import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.LayoutBuilder';
const builderId = 'b62f3f42-a721-422f-8cf2-6579867a5201';
const frameId = 'b62f3f42-a721-422f-8cf2-6579867a5202';
const columnId = 'b62f3f42-a721-422f-8cf2-6579867a5203';
Map<String, Object?> data({
  bool reference = false,
  bool tight = false,
  bool rtl = false,
  double width = 240,
}) {
  final raw = f.modelJson();
  final builder = f.node(builderId, type, {
    'builder': reference
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': 'empty'},
  });
  final child = tight
      ? builder
      : f.node(
          'b62f3f42-a721-422f-8cf2-6579867a5204',
          'flutter.widgets.Align',
          {},
          {'child': f.single(builder)},
        );
  raw['root'] = f.node(
    columnId,
    'flutter.widgets.Column',
    {
      'mainAxisSize': {'kind': 'enum', 'type': 'MainAxisSize', 'value': 'min'},
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': rtl ? 'rtl' : 'ltr',
      },
    },
    {
      'children': {
        'kind': 'list',
        'children': [
          f.node(
            frameId,
            'flutter.widgets.SizedBox',
            {'width': f.number(width), 'height': f.number(160)},
            {'child': f.single(child)},
          ),
          f.node(
            'b62f3f42-a721-422f-8cf2-6579867a5205',
            'flutter.widgets.Text',
            {
              'data': {'kind': 'string', 'value': 'Sibling'},
            },
          ),
        ],
      },
    },
  );
  return raw;
}

Map<String, Object?> builder(Map<String, Object?> raw) =>
    f.findNode(raw['root'] as Map<String, Object?>, builderId);
Finder native() =>
    find.byKey(const ValueKey('canvas-layout-builder-$builderId'));

void main() {
  test(
    'required closed preset or presence-only reference; no fabricated child slots',
    () {
      for (final invalid in <Object?>[
        null,
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'callbackPresence'},
        {'kind': 'string', 'value': 'noop'},
        {'kind': 'string', 'value': '(_, c) => Text("raw")'},
        {
          'kind': 'dartObjectReferencePresence',
          'rootSymbol': 'privateProjectCode',
        },
      ]) {
        final raw = data();
        final properties = builder(raw)['properties'] as Map;
        if (invalid == null) {
          properties.remove('builder');
        } else {
          properties['builder'] = invalid;
        }
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final name in ['child', 'children', 'sliver']) {
        final raw = data();
        builder(raw)['slots'] = {name: f.single(null)};
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final reference in [false, true]) {
        expect(f.decode(data(reference: reference)), isA<CanvasModel>());
      }
      expect(canvasDropSlotsForWidgetType(type), isEmpty);
      expect(canvasReviewedRequiredWrapperSlot(type), isNull);
    },
  );
  for (final tight in [false, true]) {
    for (final rtl in [false, true]) {
      for (final reference in [false, true]) {
        testWidgets(
          'native empty geometry resize identity selection and DnD tight=$tight rtl=$rtl custom=$reference',
          (tester) async {
            CanvasDropResolver? drop;
            final selected = <String>[];
            await f.pump(
              tester,
              data(tight: tight, rtl: rtl, reference: reference),
              selected: selected.add,
              drop: (r) => drop = r,
            );
            expect(native(), findsOneWidget);
            final render = tester.renderObject<RenderBox>(native());
            expect(render.size, tight ? const Size(240, 160) : Size.zero);
            expect(render.constraints.maxWidth, 240);
            expect(render.constraints.maxHeight, 160);
            final hints = find
                .byType(Tooltip)
                .evaluate()
                .map((e) => (e.widget as Tooltip).message ?? '')
                .join(' ');
            expect(
              hints.contains('project builder is not executed'),
              reference,
            );
            final targetFinder = tight
                ? native()
                : find.byKey(
                    const ValueKey('canvas-zero-size-widget-target-$builderId'),
                  );
            expect(targetFinder, findsOneWidget);
            await tester.tapAt(tester.getCenter(targetFinder));
            expect(selected, contains(builderId));
            final surface = tester.getRect(find.byType(CanvasDocumentView)),
                point =
                    tester.getBottomLeft(
                      find.byKey(const ValueKey('canvas-widget-$columnId')),
                    ) +
                    const Offset(100, -5);
            final target = drop!(
              ((point.dx - surface.left) / surface.width * 1000000).round(),
              ((point.dy - surface.top) / surface.height * 1000000).round(),
              CanvasPaletteDragSource(
                token: 'layout',
                widgetType: type,
                traits: {},
              ),
            );
            expect(target, isNotNull);
            expect(target!.parentWidgetId, isNot(builderId));
            await f.pump(
              tester,
              data(tight: tight, rtl: rtl, reference: !reference, width: 280),
            );
            expect(identical(render, tester.renderObject(native())), isTrue);
            expect(render.constraints.maxWidth, 280);
            expect(render.size, tight ? const Size(280, 160) : Size.zero);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  for (final horizontal in [false, true]) {
    testWidgets(
      'native empty preset accepts an unbounded scroll axis horizontal=$horizontal',
      (tester) async {
        final raw = data();
        raw['root'] = f.node(
          frameId,
          'flutter.widgets.SingleChildScrollView',
          {
            'scrollDirection': {
              'kind': 'enum',
              'type': 'Axis',
              'value': horizontal ? 'horizontal' : 'vertical',
            },
          },
          {'child': f.single(builder(raw))},
        );
        await f.pump(tester, raw);
        final render = tester.renderObject<RenderBox>(native());
        expect(
          horizontal
              ? render.constraints.maxWidth
              : render.constraints.maxHeight,
          double.infinity,
        );
        expect(
          render.size,
          Size(render.constraints.minWidth, render.constraints.minHeight),
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
}
