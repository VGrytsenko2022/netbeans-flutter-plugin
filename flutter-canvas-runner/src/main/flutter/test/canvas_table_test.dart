import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_table_width.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

String id(int n) => 'e24a8847-910d-4e50-83c0-${n.toString().padLeft(12, '0')}';
Map<String, Object?> str(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> en(String type, String v) => {
  'kind': 'enum',
  'type': type,
  'value': v,
};
Map<String, Object?> data({String alignment = 'top', bool rtl = false}) {
  final raw = c.data(), table = a.builder(raw);
  table['type'] = 'flutter.widgets.Table';
  table['properties'] = {
    'defaultVerticalAlignment': en('TableCellVerticalAlignment', alignment),
    'textBaseline': en('TextBaseline', 'alphabetic'),
    'textDirection': en('TextDirection', rtl ? 'rtl' : 'ltr'),
    'columnWidths': str('0=fixed(80);1=flex(1)'),
    'border': str('all'),
  };
  table['slots'] = {
    'children': {
      'kind': 'list',
      'children': [
        for (var r = 0; r < 2; r++)
          f.node(
            id(r + 1),
            'flutter.widgets.TableRow',
            {'key': str('row$r')},
            {
              'children': {
                'kind': 'list',
                'children': [
                  f.node(
                    id(10 + r * 2),
                    'flutter.widgets.TableCell',
                    {
                      'verticalAlignment': en(
                        'TableCellVerticalAlignment',
                        alignment,
                      ),
                    },
                    {
                      'child': f.single(
                        f.node(id(100 + r), 'flutter.widgets.Text', {
                          'data': str('Cell $r'),
                        }),
                      ),
                    },
                  ),
                  f.node(id(11 + r * 2), 'flutter.widgets.SizedBox', {
                    'width': f.number(40),
                    'height': f.number(40),
                  }),
                ],
              },
            },
          ),
      ],
    },
  };
  return raw;
}

List<Map> rows(Map raw) =>
    ((((a.builder(raw.cast<String, Object?>())['slots'] as Map)['children']
                as Map)['children'])
            as List)
        .cast<Map>();

void main() {
  test(
    'Java and runtime schema agree exactly including table grammar and row traits',
    () {
      expect(
        canvasRuntimeWidgetSchemaContractForTesting().trim(),
        canvasReviewedWidgetSchemaContract.trim(),
      );
    },
  );
  test(
    'closed width parser supports every family and rejects code and invalid geometry',
    () {
      for (final v in [
        'fixed(0)',
        'fixed(1e300)',
        'fixed(1e-300)',
        'fixed(1e-999)',
        'flex(1)',
        'fraction(.3)',
        'intrinsic()',
        'intrinsic(2)',
        'max(fixed(4),min(flex(1),intrinsic()))',
      ]) {
        expect(() => canvasTableWidth(v), returnsNormally);
      }
      for (final v in [
        '',
        'evil()',
        'flex(0)',
        'intrinsic(1e-999)',
        'fixed(-1)',
        'fixed(.001e-999)',
        'fixed(1e-999999999)',
        'fixed(1e999)',
        'fixed(1);evil()',
        'min(fixed(1))',
      ]) {
        expect(() => canvasTableWidth(v), throwsFormatException);
      }
      expect(canvasTableWidths('0=fixed(20);3=intrinsic()').keys, [0, 3]);
      for (final v in ['0=flex(1);0=fixed(2)', '10000=flex(1)', '0=flex(1);']) {
        expect(() => canvasTableWidths(v), throwsFormatException);
      }
    },
  );
  test('decoder rejects ragged, empty and duplicate-key rows', () {
    var raw = data();
    ((rows(raw)[0]['slots'] as Map)['children'] as Map)['children'] = [];
    expect(() => f.decode(raw), throwsFormatException);
    raw = data();
    (rows(raw)[1]['properties'] as Map)['key'] = str('row0');
    expect(() => f.decode(raw), throwsFormatException);
    raw = data(alignment: 'baseline');
    (a.builder(raw)['properties'] as Map).remove('textBaseline');
    expect(() => f.decode(raw), throwsFormatException);
  });
  for (final rtl in [false, true]) {
    for (final align in TableCellVerticalAlignment.values) {
      testWidgets(
        'native rectangular table and TableCell ${align.name} RTL $rtl',
        (tester) async {
          final raw = data(alignment: align.name, rtl: rtl);
          await f.pump(tester, raw);
          expect(tester.takeException(), isNull);
          final table = tester.renderObject<RenderTable>(find.byType(Table));
          expect(table.rows, 2);
          expect(table.columns, 2);
          expect(
            table.textDirection,
            rtl ? TextDirection.rtl : TextDirection.ltr,
          );
          expect(table.defaultVerticalAlignment, align);
          expect(table.border!.horizontalInside.width, 1);
          expect(table.getRowBox(0).width, greaterThan(80));
          final rowList = rows(raw);
          final first = rowList.removeAt(0);
          rowList.add(first);
          // rows() is a cast view; mutate the model's actual list explicitly.
          ((a.builder(raw)['slots'] as Map)['children'] as Map)['children'] =
              rowList;
          await f.pump(tester, raw);
          expect(tester.takeException(), isNull);
          expect(find.text('Cell 0'), findsOneWidget);
          expect(find.text('Cell 1'), findsOneWidget);
        },
      );
    }
  }
  testWidgets(
    'table selects row descriptors without rendering TableRow as a widget',
    (tester) async {
      await tester.pumpWidget(
        CanvasModelApp(
          model: f.decode(data()),
          selectedWidgetId: id(1),
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      expect(find.byType(Table), findsOneWidget);
    },
  );
  testWidgets(
    'row append, row move and cell wrapping use the native table geometry',
    (tester) async {
      CanvasDropResolver? drop;
      CanvasMovePreviewResolver? move;
      final raw = data();
      await f.pump(tester, raw, drop: (v) => drop = v, move: (v) => move = v);
      final table = find.byType(Table);
      final render = tester.renderObject<RenderTable>(table);
      final first = render.row(0).first;
      final rowMove = move!(id(1), a.builderId, 'children', 1);
      expect(rowMove, isNotNull);
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      CanvasDropTarget? at(Offset point, String type) => drop!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'table-drop',
          widgetType: type,
          traits: {if (type.endsWith('Row')) type},
        ),
      );
      expect(
        at(
          tester.getBottomRight(table) - const Offset(4, 4),
          'flutter.widgets.TableRow',
        )?.parentWidgetId,
        a.builderId,
      );
      final point = tester.getTopLeft(table) + const Offset(90, 20);
      final wrapped = at(point, 'flutter.widgets.TableCell');
      expect(wrapped?.parentWidgetId, id(1));
      expect(wrapped?.insertionIndex, 1);
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: 'flutter.widgets.Padding',
          childWidgetType: 'flutter.widgets.TableCell',
        ),
        isFalse,
      );
      final children = rows(raw).reversed.toList();
      ((a.builder(raw)['slots'] as Map)['children'] as Map)['children'] =
          children;
      await f.pump(tester, raw);
      expect(tester.renderObject(table), same(render));
      expect(render.row(0).first, isNot(same(first)));
      expect(tester.takeException(), isNull);
    },
  );
  test('TableRow and TableCell cannot be roots or enter ordinary slots', () {
    final raw = data();
    for (final type in [
      'flutter.widgets.TableRow',
      'flutter.widgets.TableCell',
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.Column',
          slotName: 'children',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: 'table',
            widgetType: type,
            traits: {if (type.endsWith('Row')) type},
          ),
        ),
        isFalse,
      );
    }
    raw['root'] = rows(raw).first;
    expect(() => f.decode(raw), throwsFormatException);
  });
}
