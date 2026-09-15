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
    canvasDatePickerDialogType,
    {
      'firstDate': s('2000-01-01'),
      'lastDate': s('2030-12-31'),
      'initialDate': s('2024-02-29'),
      'currentDate': s('2024-02-15'),
      'initialEntryMode': {
        'kind': 'enum',
        'type': 'DatePickerEntryMode',
        'value': mode,
      },
      'helpText': s('Choose a date'),
      'cancelText': s('Abort'),
      'confirmText': s('Accept'),
      'onDatePickerModeChange': s('noop'),
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
  test('exact Java schema and Icon-only slots', () {
    expect(
      canvasRuntimeWidgetSchemaContractForTesting().trim(),
      canvasReviewedWidgetSchemaContract.trim(),
    );
    expect(canvasDropSlotsForWidgetType(canvasDatePickerDialogType).length, 2);
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
    p['initialDate'] = s('2031-01-01');
    expect(() => f.decode(raw), throwsFormatException);
    p['initialDate'] = s('2024-02-29');
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
          final native = tester.widget<DatePickerDialog>(
            find.byType(DatePickerDialog),
          );
          expect(native.firstDate, DateTime(2000));
          expect(native.lastDate, DateTime(2030, 12, 31));
          expect(native.initialDate, DateTime(2024, 2, 29));
          expect(native.initialEntryMode.name, mode);
          expect(native.helpText, 'Choose a date');
          expect(native.confirmText, 'Accept');
          expect(
            find.ancestor(
              of: find.byType(DatePickerDialog),
              matching: find.byType(ExcludeFocus),
            ),
            findsWidgets,
          );
          await tester.tap(find.text('Abort'), warnIfMissed: false);
          await tester.pump();
          expect(find.byType(DatePickerDialog), findsOneWidget);
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
        'initialDate',
        'currentDate',
        'calendarDelegate',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)[field] = {
          'kind': 'dartObjectReferencePresence',
        };
        await f.pump(tester, raw);
        expect(find.byType(DatePickerDialog), findsNothing);
        expect(
          find.textContaining(
            'project DateTime/calendar delegate preview unavailable',
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
          .widget<DatePickerDialog>(find.byType(DatePickerDialog))
          .switchToInputEntryModeIcon,
      isNotNull,
    );
    await f.pump(tester, data(mode: 'inputOnly'));
    await tester.pumpAndSettle();
    expect(find.byType(InputDatePickerFormField), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
