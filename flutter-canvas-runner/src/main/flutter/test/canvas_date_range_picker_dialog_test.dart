import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> data({
  String mode = 'calendar',
  bool rtl = false,
  bool icons = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(
    f.fillId,
    canvasDateRangePickerDialogType,
    {
      'firstDate': s('2000-01-01'),
      'lastDate': s('2030-12-31'),
      'initialDateRange': s('2024-02-29/2024-03-01'),
      'currentDate': s('2024-02-15'),
      'initialEntryMode': {
        'kind': 'enum',
        'type': 'DatePickerEntryMode',
        'value': mode,
      },
      'helpText': s('Choose a date'),
      'cancelText': s('Abort'),
      'confirmText': s('Accept'),
      'saveText': s('Save range'),
    },
    {
      'switchToInputEntryModeIcon': f.single(
        icons
            ? f.node(f.bodyId, 'flutter.widgets.Icon', {
                'icon': {
                  'kind': 'iconData',
                  'codePoint': 0xe8b8,
                  'fontFamily': 'MaterialIcons',
                  'fontPackage': null,
                  'fontFamilyFallback': <String>[],
                  'matchTextDirection': false,
                },
              })
            : null,
      ),
      'switchToCalendarEntryModeIcon': f.single(null),
    },
  );
  if (rtl) {
    raw['root'] = f.node(
      f.headerId,
      'flutter.widgets.Directionality',
      {
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'rtl',
        },
      },
      {'child': f.single(raw['root'] as Map<String, Object?>)},
    );
  }
  return raw;
}

void main() {
  test('range invariants and exact non-nullable keyboard are enforced', () {
    for (final value in [
      '2024-03-02/2024-03-01',
      '2025-02-29/2025-03-01',
      '2024-02-29/2024-03-01\n',
      '0000-01-01/2024-01-01',
    ]) {
      expect(() => canvasGregorianDateRange(value), throwsFormatException);
    }
    final raw = data(), p = (raw['root'] as Map)['properties'] as Map;
    p['initialDateRange'] = s('2000-01-01/2000-01-01');
    expect(() => f.decode(raw), returnsNormally);
    p['keyboardType'] = {'kind': 'null'};
    expect(() => f.decode(raw), throwsFormatException);
    p.remove('keyboardType');
    p['initialDateRange'] = s('1999-12-31/2000-01-01');
    expect(() => f.decode(raw), throwsFormatException);
    p['calendarDelegate'] = {'kind': 'dartObjectReferencePresence'};
    expect(() => f.decode(raw), returnsNormally);
    p['initialDateRange'] = s('2024-03-02/2024-03-01');
    expect(() => f.decode(raw), throwsFormatException);
  });
  testWidgets(
    'omitted range and keyboard retain native defaults without autofocus',
    (tester) async {
      final raw = data(mode: 'input'),
          p = (raw['root'] as Map)['properties'] as Map;
      p.remove('initialDateRange');
      await f.pump(tester, raw);
      await tester.pumpAndSettle();
      final dialog = tester.widget<DateRangePickerDialog>(
        find.byType(DateRangePickerDialog),
      );
      expect(dialog.initialDateRange, isNull);
      expect(dialog.keyboardType, TextInputType.datetime);
      for (final field in tester.widgetList<EditableText>(
        find.byType(EditableText),
      )) {
        expect(field.focusNode.hasFocus, isFalse);
      }
      expect(tester.takeException(), isNull);
    },
  );
  test('exact Java schema and Icon-only slots', () {
    expect(
      canvasRuntimeWidgetSchemaContractForTesting().trim(),
      canvasReviewedWidgetSchemaContract.trim(),
    );
    expect(
      canvasDropSlotsForWidgetType(canvasDateRangePickerDialogType).length,
      2,
    );
    for (final value in [
      '2025-02-29',
      '2024-02-30',
      '0000-01-01',
      '10000-01-01',
      '2024-1-1',
      '2024-01-01\n',
      '2024-01-01\r',
      ' 2024-01-01',
    ]) {
      expect(() => canvasGregorianDate(value), throwsFormatException);
    }
    expect(canvasGregorianDate('2000-02-29').day, 29);
    final raw = data(),
        root = raw['root'] as Map,
        p = root['properties'] as Map;
    p['initialDateRange'] = s('2031-01-01/2031-01-02');
    expect(() => f.decode(raw), throwsFormatException);
    p['initialDateRange'] = s('2024-02-29/2024-03-01');
    (root['slots'] as Map)['switchToInputEntryModeIcon'] = f.single(
      f.node(f.bodyId, 'flutter.widgets.Text', {'data': s('Not an Icon')}),
    );
    expect(() => f.decode(raw), throwsFormatException);
  });
  for (final mode in ['calendar', 'input', 'calendarOnly', 'inputOnly']) {
    for (final rtl in [false, true]) {
      testWidgets(
        'native mode=$mode rtl=$rtl without focus or route mutations',
        (tester) async {
          await f.pump(tester, data(mode: mode, rtl: rtl));
          await tester.pumpAndSettle();
          final native = tester.widget<DateRangePickerDialog>(
            find.byType(DateRangePickerDialog),
          );
          expect(native.firstDate, DateTime(2000));
          expect(native.lastDate, DateTime(2030, 12, 31));
          expect(
            native.initialDateRange,
            DateTimeRange(
              start: DateTime(2024, 2, 29),
              end: DateTime(2024, 3, 1),
            ),
          );
          expect(native.initialEntryMode.name, mode);
          expect(native.helpText, 'Choose a date');
          expect(native.confirmText, 'Accept');
          expect(
            find.ancestor(
              of: find.byType(DateRangePickerDialog),
              matching: find.byType(ExcludeFocus),
            ),
            findsWidgets,
          );
          if (mode.startsWith('input')) {
            await tester.tap(find.text('Abort'), warnIfMissed: false);
          }
          await tester.pump();
          expect(find.byType(DateRangePickerDialog), findsOneWidget);
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets(
    'typed calendar/date is disclosed, never replaced by a false calendar',
    (tester) async {
      for (final field in [
        'firstDate',
        'lastDate',
        'initialDateRange',
        'currentDate',
        'calendarDelegate',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
        await f.pump(tester, raw);
        expect(find.byType(DateRangePickerDialog), findsNothing);
        expect(
          find.textContaining(
            'project DateTime/range/calendar delegate preview unavailable',
          ),
          findsOneWidget,
        );
      }
    },
  );
  testWidgets('Icon slot and changed initial mode refresh native preview', (
    tester,
  ) async {
    await f.pump(tester, data(icons: true));
    expect(
      tester
          .widget<DateRangePickerDialog>(find.byType(DateRangePickerDialog))
          .switchToInputEntryModeIcon,
      isNotNull,
    );
    await f.pump(tester, data(mode: 'inputOnly'));
    await tester.pumpAndSettle();
    expect(find.byType(TextField), findsNWidgets(2));
    expect(tester.takeException(), isNull);
  });
}
