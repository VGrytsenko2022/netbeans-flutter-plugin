import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_color_filtered_test.dart' as c;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

String id(int n) => 'd24a8847-910d-4e50-83c0-${n.toString().padLeft(12, '0')}';
Map<String, Object?> str(String s) => {'kind': 'string', 'value': s};
Map<String, Object?> integer(int n) => {'kind': 'integer', 'value': n};
Map<String, Object?> list(List<Map<String, Object?>> n) => {
  'kind': 'list',
  'children': n,
};
Map<String, Object?> data({
  bool rtl = false,
  bool flex = false,
  bool empty = false,
  bool refs = false,
}) {
  final raw = c.data(), table = a.builder(raw);
  final frame = (raw['root'] as Map)['slots'] as Map;
  final frameNode =
      ((frame['children'] as Map)['children'] as List).first as Map;
  frameNode['properties'] = {'width': f.number(600), 'height': f.number(300)};
  table['type'] = canvasDataTableType;
  table['properties'] = {
    'horizontalMargin': f.number(8),
    'columnSpacing': f.number(12),
    'sortColumnIndex': integer(1),
    'sortAscending': f.boolean(false),
    'onSelectAll': str('noop'),
    'dataTextStyle': str('local'),
    'dataTextStyleFontSize': f.number(12),
    'headingTextStyle': str('local'),
    'headingTextStyleFontSize': f.number(14),
    'dataRowColor': str('local'),
    'dataRowColorSelected': {'kind': 'null'},
    'dataRowColorDefault': {'kind': 'color', 'argb': '0xFFEEDDCC'},
    if (refs) 'border': {'kind': 'dartObjectReferencePresence'},
  };
  table['slots'] = {
    'columns': list([
      for (var col = 0; col < 2; col++)
        f.node(
          id(1 + col),
          canvasDataColumnType,
          {
            'onSort': str('noop'),
            'tooltip': str('Column $col'),
            'numeric': f.boolean(col == 1),
            if (flex) 'columnWidth': str('fixed(120)'),
            'mouseCursor': str('local'),
            'mouseCursorHovered': str('click'),
          },
          {
            'label': f.single(
              flex
                  ? f.node(id(50 + col), 'flutter.widgets.Expanded', {}, {
                      'child': f.single(
                        f.node(id(60 + col), 'flutter.widgets.Text', {
                          'data': str('Head $col'),
                        }),
                      ),
                    })
                  : f.node(id(60 + col), 'flutter.widgets.Text', {
                      'data': str('Head $col'),
                    }),
            ),
          },
        ),
    ]),
    'rows': list([
      for (var r = 0; r < 2; r++)
        f.node(
          id(10 + r),
          r == 0
              ? 'flutter.material.DataRow.byIndex'
              : 'flutter.material.DataRow',
          {
            if (r == 0) 'index': integer(3),
            'selected': f.boolean(r == 0),
            'onSelectChanged': str('noop'),
            'onLongPress': str('noop'),
            'onHover': str('noop'),
            'color': str('local'),
            'colorHovered': {'kind': 'color', 'argb': '0xFF123456'},
          },
          {
            'cells': list([
              for (var col = 0; col < 2; col++)
                if (empty && r == 0 && col == 0)
                  f.node(
                    id(100 + r * 2 + col),
                    'flutter.material.DataCell.empty',
                  )
                else
                  f.node(
                    id(100 + r * 2 + col),
                    'flutter.material.DataCell',
                    {
                      'onTap': str('noop'),
                      'onDoubleTap': str('noop'),
                      'onLongPress': str('noop'),
                      'onTapDown': str('noop'),
                      'onTapCancel': str('noop'),
                      'showEditIcon': f.boolean(col == 1),
                      'placeholder': f.boolean(false),
                    },
                    {
                      'child': f.single(
                        f.node(id(200 + r * 2 + col), 'flutter.widgets.Text', {
                          'data': str('Cell $r:$col'),
                        }),
                      ),
                    },
                  ),
            ]),
          },
        ),
    ]),
  };
  if (rtl) {
    // Place Directionality above the data table, preserving the existing constrained fixture.
    final saved = Map<String, Object?>.from(table);
    table['id'] = id(999);
    table['type'] = 'flutter.widgets.Directionality';
    table['properties'] = {
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': 'rtl',
      },
    };
    table['slots'] = {'child': f.single(saved)};
  }
  return raw;
}

Map table(Map<String, Object?> raw) {
  final n = a.builder(raw);
  return n['type'] == canvasDataTableType
      ? n
      : ((n['slots'] as Map)['child'] as Map)['child'] as Map;
}

List<Map> children(Map node, String slot) =>
    (((node['slots'] as Map)[slot] as Map)['children'] as List).cast<Map>();

void main() {
  testWidgets('SDK native DataTable baseline with tooltip sort and semantics', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DataTable(
            columns: [
              DataColumn(
                label: const Text('Head'),
                tooltip: 'Column',
                onSort: (_, _) {},
              ),
            ],
            rows: const [
              DataRow(cells: [DataCell(Text('Cell'))]),
            ],
          ),
        ),
      ),
    );
    expect(tester.takeException(), isNull);
  });

  test(
    'DataTable runtime contract exactly matches reviewed Java projection',
    () {
      expect(
        canvasRuntimeWidgetSchemaContractForTesting().trim(),
        canvasReviewedWidgetSchemaContract.trim(),
      );
    },
  );
  test(
    'rejects ragged cells, empty columns, duplicate indices and invalid sort/height',
    () {
      for (var mode = 0; mode < 6; mode++) {
        final raw = data(), n = table(raw), rows = children(n, 'rows');
        if (mode == 0) {
          ((rows[0]['slots'] as Map)['cells'] as Map)['children'] = [];
        }
        if (mode == 1) ((n['slots'] as Map)['columns'] as Map)['children'] = [];
        if (mode == 2) (n['properties'] as Map)['sortColumnIndex'] = integer(2);
        if (mode == 3) {
          (n['properties'] as Map)['dataRowMinHeight'] = f.number(90);
          (n['properties'] as Map)['dataRowMaxHeight'] = f.number(40);
        }
        if (mode == 4) {
          (n['properties'] as Map)['dataRowHeight'] = f.number(48);
          (n['properties'] as Map)['dataRowMinHeight'] = f.number(40);
        }
        if (mode == 5) {
          rows[1]['type'] = 'flutter.material.DataRow.byIndex';
          (rows[1]['properties'] as Map)['index'] = integer(3);
        }
        expect(
          () => f.decode(raw),
          throwsFormatException,
          reason: 'mode $mode',
        );
      }
    },
  );
  for (final rtl in [false, true]) {
    for (final flex in [false, true]) {
      for (final empty in [false, true]) {
        testWidgets(
          'native descriptors and states rtl=$rtl flex=$flex empty=$empty',
          (tester) async {
            final raw = data(rtl: rtl, flex: flex, empty: empty);
            await f.pump(tester, raw);
            final native = tester.widget<DataTable>(find.byType(DataTable));
            expect(native.columns.length, 2);
            expect(native.rows.length, 2);
            expect(native.sortColumnIndex, 1);
            expect(native.rows[0].selected, isTrue);
            expect(
              native.dataRowColor!.resolve({WidgetState.selected}),
              isNull,
            );
            expect(native.dataRowColor!.resolve({}), const Color(0xffeeddcc));
            expect(
              native.rows[0].color!.resolve({WidgetState.hovered}),
              const Color(0xff123456),
            );
            expect(
              native.columns[0].mouseCursor!.resolve({WidgetState.hovered}),
              SystemMouseCursors.click,
            );
            expect(native.columns[0].mouseCursor!.resolve({}), isNull);
            native.columns[0].onSort!(0, true);
            native.rows[0].onSelectChanged!(false);
            expect(
              native.rows[0].selected,
              isTrue,
              reason:
                  'Preview callbacks must not mutate controlled model flags',
            );
            if (empty) expect(native.rows[0].cells[0], same(DataCell.empty));
            expect(native.dataTextStyle!.fontSize, 12);
            expect(native.headingTextStyle!.fontSize, 14);
            final layout = tester.renderObject<RenderTable>(find.byType(Table));
            expect(
              layout.columns,
              3,
              reason: 'Implicit checkbox column uses native layout',
            );
            expect(layout.rows, 3);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  testWidgets('source values remain inert and row drops use table geometry', (
    tester,
  ) async {
    CanvasDropResolver? drop;
    CanvasMovePreviewResolver? move;
    await f.pump(
      tester,
      data(refs: true),
      drop: (r) => drop = r,
      move: (r) => move = r,
    );
    final native = tester.widget<DataTable>(find.byType(DataTable));
    expect(native.border, isNull);
    final surface = tester.getRect(find.byType(CanvasDocumentView)),
        rect = tester.getRect(find.byType(Table));
    final point = rect.bottomRight - const Offset(3, 3);
    final target = drop!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
      CanvasPaletteDragSource(
        token: 'data-row',
        widgetType: 'flutter.material.DataRow.byIndex',
        traits: {'flutter.material.DataRow'},
      ),
    );
    expect(target?.parentWidgetId, a.builderId);
    expect(target?.slotName, 'rows');
    expect(move!(id(10), a.builderId, 'rows', 1), isNotNull);
    expect(move!(id(1), a.builderId, 'columns', 1), isNotNull);
    expect(move!(id(100), id(10), 'cells', 1), isNotNull);
    expect(
      move!(id(100), id(11), 'cells', 1),
      isNull,
      reason: 'Cross-row cell moves cannot make ragged rows',
    );
    expect(tester.takeException(), isNull);
  });
  test('descriptors only enter their exact native descriptor slots', () {
    for (final type in [
      canvasDataColumnType,
      ...canvasDataRowTypes,
      ...canvasDataCellTypes,
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: 'flutter.widgets.Padding',
          childWidgetType: type,
        ),
        isFalse,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.Column',
          slotName: 'children',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: 'x',
            widgetType: type,
            traits: {type},
          ),
        ),
        isFalse,
      );
    }
  });
}
