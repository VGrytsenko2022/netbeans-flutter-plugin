import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';

import 'canvas_data_table_test.dart' as t;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> data({bool rtl = false, bool header = false}) {
  final raw = t.data(rtl: rtl);
  final n = t.table(raw);
  n['type'] = canvasPaginatedDataTableType;
  n['properties'] = {
    'source': {'kind': 'dartObjectReferencePresence'},
    'horizontalMargin': f.number(8),
    'columnSpacing': f.number(12),
    'rowsPerPage': t.integer(2),
    'availableRowsPerPage': t.str('2, 10, 20'),
    'showEmptyRows': f.boolean(false),
    'showFirstLastButtons': f.boolean(true),
    'onRowsPerPageChanged': t.str('noop'),
    'onPageChanged': t.str('noop'),
    'headingRowColor': t.str('local'),
    'headingRowColorDefault': {'kind': 'color', 'argb': '0xFFEEDDCC'},
  };
  final slots = n['slots'] as Map;
  slots.remove('rows');
  slots['header'] = f.single(
    header
        ? f.node(t.id(900), 'flutter.widgets.Text', {'data': t.str('Header')})
        : null,
  );
  slots['actions'] = t.list(
    header
        ? [
            f.node(t.id(901), 'flutter.widgets.Text', {
              'data': t.str('Action'),
            }),
          ]
        : [],
  );
  return raw;
}

Map table(Map<String, Object?> raw) {
  final node = a.builder(raw);
  return node['type'] == canvasPaginatedDataTableType
      ? node
      : ((node['slots'] as Map)['child'] as Map)['child'] as Map;
}

void main() {
  test('PaginatedDataTable schema matches Java and has no visual row slot', () {
    expect(
      canvasRuntimeWidgetSchemaContractForTesting().trim(),
      canvasReviewedWidgetSchemaContract.trim(),
    );
    expect(
      canvasDropSlotsForWidgetType(
        canvasPaginatedDataTableType,
      ).map((s) => s.slotName),
      containsAll(['header', 'actions', 'columns']),
    );
  });
  test('page sizes and native assertions reject invalid values', () {
    for (final value in [
      '0',
      '1,1',
      '1,',
      '-1',
      '1.0',
      'foo()',
      '9007199254740992',
    ]) {
      expect(() => canvasPageSizes(value), throwsFormatException);
    }
    for (var mode = 0; mode < 6; mode++) {
      final raw = data(),
          node = table(raw),
          p = node['properties'] as Map,
          slots = node['slots'] as Map;
      if (mode == 0) p.remove('source');
      if (mode == 1) p['rowsPerPage'] = t.integer(0);
      if (mode == 2) p['availableRowsPerPage'] = t.str('10, 20');
      if (mode == 3) {
        p['primary'] = f.boolean(true);
        p['controller'] = {'kind': 'dartObjectReferencePresence'};
      }
      if (mode == 4) {
        slots['actions'] = t.list([
          f.node(t.id(900), 'flutter.widgets.Text', {'data': t.str('Action')}),
        ]);
      }
      if (mode == 5) p['sortColumnIndex'] = t.integer(2);
      expect(() => f.decode(raw), throwsFormatException, reason: 'mode $mode');
    }
  });
  for (final rtl in [false, true]) {
    for (final header in [false, true]) {
      testWidgets('native empty-source preview rtl=$rtl header=$header', (
        tester,
      ) async {
        final raw = data(rtl: rtl, header: header);
        await f.pump(tester, raw);
        final native = tester.widget<PaginatedDataTable>(
          find.byType(PaginatedDataTable),
        );
        expect(native.source.rowCount, 0);
        expect(native.source.isRowCountApproximate, isFalse);
        expect(native.rowsPerPage, 2);
        expect(native.availableRowsPerPage, [2, 10, 20]);
        expect(native.header != null, header);
        expect(native.actions != null, header);
        expect(native.columns.length, 2);
        expect(native.headingRowColor!.resolve({}), const Color(0xffeeddcc));
        expect(native.showFirstLastButtons, isTrue);
        native.onRowsPerPageChanged!(10);
        native.onPageChanged!(2);
        await tester.pump();
        expect(
          tester
              .widget<PaginatedDataTable>(find.byType(PaginatedDataTable))
              .rowsPerPage,
          2,
        );
        expect(tester.takeException(), isNull);
        final source = native.source;
        await tester.pumpWidget(const SizedBox());
        // Verify native unmount removed its subscription from the shared preview source.
        // ignore: invalid_use_of_protected_member
        expect(source.hasListeners, isFalse);
      });
    }
  }
  testWidgets(
    'column geometry does not resolve to a nested table in the header',
    (tester) async {
      final raw = data(header: true), slots = table(raw)['slots'] as Map;
      slots['header'] = f.single(
        f.node(t.id(910), 'flutter.widgets.Table', {}, {
          'children': t.list([
            f.node(t.id(911), 'flutter.widgets.TableRow', {}, {
              'children': t.list([
                f.node(t.id(912), 'flutter.widgets.Text', {
                  'data': t.str('Nested header table'),
                }),
              ]),
            }),
          ]),
        }),
      );
      CanvasMovePreviewResolver? move;
      await f.pump(tester, raw, move: (resolver) => move = resolver);
      expect(find.byType(Table), findsNWidgets(2));
      expect(move!(t.id(1), a.builderId, 'columns', 1), isNotNull);
      expect(move!(t.id(2), a.builderId, 'columns', 0), isNotNull);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'oversized preview has a disclosed bound instead of allocating huge rows',
    (tester) async {
      final raw = data(), p = table(raw)['properties'] as Map;
      p['rowsPerPage'] = t.integer(1001);
      p['onRowsPerPageChanged'] = {'kind': 'null'};
      await f.pump(tester, raw);
      expect(find.byType(PaginatedDataTable), findsNothing);
      expect(find.textContaining('limited to 1,000'), findsOneWidget);
    },
  );
}
