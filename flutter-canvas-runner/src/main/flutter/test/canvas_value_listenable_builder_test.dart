import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.ValueListenableBuilder';
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
    'valueType': {'kind': 'string', 'value': 'double'},
    'valueListenable': {'kind': 'string', 'value': 'constant'},
    'builder': reference
        ? {'kind': 'dartObjectReferencePresence'}
        : {'kind': 'string', 'value': 'child'},
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
    find.byKey(const ValueKey('canvas-value-listenable-builder-$builderId'));

void main() {
  testWidgets(
    'all builtin nullable presets and custom source fallback retain native identity',
    (tester) async {
      Element? retained;
      for (final nullable in [false, true]) {
        for (final type in [
          'String',
          'int',
          'double',
          'num',
          'bool',
          'Object',
        ]) {
          final raw = data();
          final props = builder(raw)['properties'] as Map;
          props['valueType'] = {'kind': 'string', 'value': type};
          props['nullableValueType'] = {'kind': 'boolean', 'value': nullable};
          await f.pump(tester, raw);
          final w = tester.widget<ValueListenableBuilder<Object?>>(native());
          final expected = nullable
              ? null
              : switch (type) {
                  'String' => '',
                  'bool' => false,
                  'double' => 0.0,
                  _ => 0,
                };
          expect(w.valueListenable.value, expected);
          final element = tester.element(native());
          if (retained != null) expect(identical(retained, element), isTrue);
          retained = element;
          expect(tester.takeException(), isNull);
        }
      }
    },
  );
  test(
    'value type and non-nullable custom source relationships are closed',
    () {
      for (final bad in [
        null,
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        {'kind': 'string', 'value': 'dynamic'},
        {'kind': 'string', 'value': 'List<int>'},
      ]) {
        final raw = data();
        final props = builder(raw)['properties'] as Map;
        if (bad == null) {
          props.remove('valueType');
        } else {
          props['valueType'] = bad;
        }
        expect(() => f.decode(raw), throwsFormatException);
      }
      final raw = data();
      final props = builder(raw)['properties'] as Map;
      props['valueType'] = {'kind': 'dartObjectReferencePresence'};
      expect(() => f.decode(raw), throwsFormatException);
      props['nullableValueType'] = {'kind': 'boolean', 'value': true};
      expect(() => f.decode(raw), returnsNormally);
      props.remove('nullableValueType');
      props['valueListenable'] = {'kind': 'dartObjectReferencePresence'};
      expect(() => f.decode(raw), returnsNormally);
    },
  );

  testWidgets(
    'mobile and tablet profile rotation updates native MediaQuery without replacing the builder',
    (tester) async {
      Element? retained;
      for (final mode in ['MOBILE', 'TABLET']) {
        for (final size in [
          const Size(390, 844),
          const Size(844, 390),
          const Size(600, 600),
        ]) {
          final raw = data();
          final profile = raw['profile'] as Map;
          profile['previewMode'] = mode;
          profile['logicalWidth'] = size.width.toInt();
          profile['logicalHeight'] = size.height.toInt();
          await f.pump(tester, raw);
          final finder = native();
          final element = tester.element(finder);
          final query = MediaQuery.of(element);
          expect(query.size, size);
          expect(
            query.orientation,
            size.width > size.height
                ? Orientation.landscape
                : Orientation.portrait,
          );
          if (retained != null) expect(identical(retained, element), isTrue);
          retained = element;
          expect(tester.takeException(), isNull);
        }
      }
    },
  );
  test(
    'required closed preset or presence-only reference; optional typed Child and optional typed Child; no invented slots',
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
      for (final name in ['children', 'sliver']) {
        final raw = data();
        builder(raw)['slots'] = {name: f.single(null)};
        expect(() => f.decode(raw), throwsFormatException);
      }
      for (final reference in [false, true]) {
        expect(f.decode(data(reference: reference)), isA<CanvasModel>());
      }
      expect(canvasDropSlotsForWidgetType(type), hasLength(1));
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
              hints.contains(
                'project sources/getters/factories and builders are not executed',
              ),
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
            for (final width in [80.0, 160.0, 280.0]) {
              await f.pump(
                tester,
                data(
                  tight: tight,
                  rtl: rtl,
                  reference: !reference,
                  width: width,
                ),
              );
              expect(tester.widget(native()), isA<ValueListenableBuilder>());
              expect(identical(render, tester.renderObject(native())), isTrue);
              expect(render.constraints.maxWidth, width);
              expect(render.size, tight ? Size(width, 160) : Size.zero);
            }
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
