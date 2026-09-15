import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> data({
  String mode = 'day',
  bool rtl = false,
  bool empty = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasCalendarDatePickerType, {
    'firstDate': s('2000-01-01'),
    'lastDate': s('2030-12-31'),
    'initialDate': empty ? {'kind': 'null'} : s('2024-02-29'),
    'currentDate': s('2024-02-15'),
    'onDateChanged': s('noop'),
    'onDisplayedMonthChanged': s('noop'),
    'initialCalendarMode': {
      'kind': 'enum',
      'type': 'DatePickerMode',
      'value': mode,
    },
  });
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
  test(
    'exact schema, nullable required initial date and nonnullable required event',
    () {
      expect(
        canvasRuntimeWidgetSchemaContractForTesting().trim(),
        canvasReviewedWidgetSchemaContract.trim(),
      );
      expect(
        canvasDropSlotsForWidgetType(canvasCalendarDatePickerType),
        isEmpty,
      );
      for (final name in [
        'initialDate',
        'firstDate',
        'lastDate',
        'onDateChanged',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException, reason: name);
      }
      for (final name in [
        'firstDate',
        'lastDate',
        'onDateChanged',
        'calendarDelegate',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException, reason: name);
      }
      expect(() => f.decode(data(empty: true)), returnsNormally);
      for (final invalid in [
        '2025-02-29',
        '0000-01-01',
        '2024-1-1',
        '2024-02-29\n',
        '1999-12-31',
        '2031-01-01',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)['initialDate'] = s(invalid);
        expect(() => f.decode(raw), throwsFormatException, reason: invalid);
      }
      final raw = data();
      ((raw['root'] as Map)['properties'] as Map)['firstDate'] = s(
        '2040-01-01',
      );
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  for (final mode in ['day', 'year']) {
    for (final rtl in [false, true]) {
      for (final empty in [false, true]) {
        testWidgets('native inline calendar mode=$mode rtl=$rtl empty=$empty', (
          tester,
        ) async {
          await f.pump(tester, data(mode: mode, rtl: rtl, empty: empty));
          await tester.pumpAndSettle();
          final finder = find.byType(CalendarDatePicker);
          final calendar = tester.widget<CalendarDatePicker>(finder);
          expect(calendar.initialDate, empty ? null : DateTime(2024, 2, 29));
          expect(calendar.firstDate, DateTime(2000));
          expect(calendar.lastDate, DateTime(2030, 12, 31));
          expect(calendar.initialCalendarMode.name, mode);
          expect(
            find.ancestor(of: finder, matching: find.byType(Material)),
            findsWidgets,
          );
          expect(
            find.ancestor(of: finder, matching: find.byType(ExcludeFocus)),
            findsWidgets,
          );
          expect(
            find.ancestor(of: finder, matching: find.byType(AbsorbPointer)),
            findsWidgets,
          );
          expect(find.byType(DatePickerDialog), findsNothing);
          if (mode == 'day') {
            await tester.tap(find.byTooltip('Next month'), warnIfMissed: false);
            await tester.pumpAndSettle();
            expect(find.text('February 2024'), findsOneWidget);
          }
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
  testWidgets('project dates and calendars are disclosed, never executed', (
    tester,
  ) async {
    for (final name in [
      'firstDate',
      'lastDate',
      'initialDate',
      'currentDate',
      'calendarDelegate',
    ]) {
      final raw = data();
      ((raw['root'] as Map)['properties'] as Map)[name] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      expect(find.byType(CalendarDatePicker), findsNothing);
      expect(
        find.textContaining(
          'project DateTime/calendar delegate preview unavailable',
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets('callbacks and predicate presence cannot execute project code', (
    tester,
  ) async {
    final raw = data();
    for (final name in [
      'onDateChanged',
      'onDisplayedMonthChanged',
      'selectableDayPredicate',
    ]) {
      ((raw['root'] as Map)['properties'] as Map)[name] = {
        'kind': 'dartObjectReferencePresence',
      };
    }
    await f.pump(tester, raw);
    final calendar = tester.widget<CalendarDatePicker>(
      find.byType(CalendarDatePicker),
    );
    expect(calendar.selectableDayPredicate, isNull);
    expect(() => calendar.onDateChanged(DateTime(2024)), returnsNormally);
    expect(
      () => calendar.onDisplayedMonthChanged!(DateTime(2024)),
      returnsNormally,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets('model edits deliberately remount initial date and mode', (
    tester,
  ) async {
    await f.pump(tester, data());
    await tester.pumpAndSettle();
    final raw = data(mode: 'year');
    ((raw['root'] as Map)['properties'] as Map)['initialDate'] = s(
      '2025-01-02',
    );
    await f.pump(tester, raw);
    await tester.pumpAndSettle();
    final calendar = tester.widget<CalendarDatePicker>(
      find.byType(CalendarDatePicker),
    );
    expect(calendar.initialDate, DateTime(2025, 1, 2));
    expect(calendar.initialCalendarMode, DatePickerMode.year);
    expect(find.byType(YearPicker), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
