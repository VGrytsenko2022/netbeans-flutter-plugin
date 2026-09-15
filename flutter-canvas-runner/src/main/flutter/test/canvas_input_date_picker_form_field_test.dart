import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> data({
  bool empty = false,
  bool rtl = false,
  String keyboard = 'datetime',
  bool accept = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasInputDatePickerFormFieldType, {
    'firstDate': s('2000-01-01'),
    'lastDate': s('2030-12-31'),
    'initialDate': empty ? {'kind': 'null'} : s('2024-02-29'),
    'onDateSubmitted': s('noop'),
    'onDateSaved': s('noop'),
    'errorFormatText': s('Bad format'),
    'errorInvalidText': s('Bad date'),
    'fieldHintText': s('Date hint'),
    'fieldLabelText': s('Date label'),
    'autofocus': {'kind': 'boolean', 'value': true},
    'acceptEmptyDate': {'kind': 'boolean', 'value': accept},
    'keyboardType': s(keyboard),
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

class _ProjectCalendar extends GregorianCalendarDelegate {
  const _ProjectCalendar();
  @override
  String formatCompactDate(
    DateTime date,
    MaterialLocalizations localizations,
  ) => 'day=${date.day}';
  @override
  DateTime? parseCompactDate(
    String? input,
    MaterialLocalizations localizations,
  ) => input == 'day=29' ? DateTime(2024, 2, 29) : null;
}

void main() {
  testWidgets(
    'native custom calendar owns format and parse independently of labels',
    (tester) async {
      final saved = <DateTime>[], key = GlobalKey<FormState>();
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Form(
              key: key,
              child: InputDatePickerFormField(
                initialDate: DateTime(2024, 2, 29),
                firstDate: DateTime(2024),
                lastDate: DateTime(2025),
                calendarDelegate: const _ProjectCalendar(),
                fieldHintText: 'Not a parser',
                onDateSaved: saved.add,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(
        tester.widget<EditableText>(find.byType(EditableText)).controller.text,
        'day=29',
      );
      expect(key.currentState!.validate(), isTrue);
      key.currentState!.save();
      expect(saved, [DateTime(2024, 2, 29)]);
      await tester.enterText(find.byType(TextFormField), '02/29/2024');
      expect(key.currentState!.validate(), isFalse);
      key.currentState!.save();
      expect(saved, [DateTime(2024, 2, 29)]);
      expect(tester.takeException(), isNull);
    },
  );
  test(
    'exact schema, required bounds, nullable initial and nonnullable booleans',
    () {
      expect(
        canvasRuntimeWidgetSchemaContractForTesting().trim(),
        canvasReviewedWidgetSchemaContract.trim(),
      );
      expect(
        canvasDropSlotsForWidgetType(canvasInputDatePickerFormFieldType),
        isEmpty,
      );
      for (final name in ['firstDate', 'lastDate']) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map).remove(name);
        expect(() => f.decode(raw), throwsFormatException, reason: name);
      }
      for (final name in [
        'firstDate',
        'lastDate',
        'calendarDelegate',
        'autofocus',
        'acceptEmptyDate',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)[name] = {'kind': 'null'};
        expect(() => f.decode(raw), throwsFormatException, reason: name);
      }
      final raw = data();
      ((raw['root'] as Map)['properties'] as Map).remove('initialDate');
      expect(() => f.decode(raw), returnsNormally);
      for (final invalid in [
        '2025-02-29',
        '0000-01-01',
        '2024-1-1',
        '1999-12-31',
        '2031-01-01',
      ]) {
        final raw = data();
        ((raw['root'] as Map)['properties'] as Map)['initialDate'] = s(invalid);
        expect(() => f.decode(raw), throwsFormatException, reason: invalid);
      }
    },
  );
  for (final keyboard in [
    'text',
    'multiline',
    'number',
    'numberSigned',
    'numberDecimal',
    'numberSignedDecimal',
    'phone',
    'datetime',
    'emailAddress',
    'url',
    'visiblePassword',
    'name',
    'streetAddress',
    'none',
    'webSearch',
    'twitter',
  ]) {
    for (final rtl in [false, true]) {
      for (final empty in [false, true]) {
        testWidgets('native input keyboard=$keyboard rtl=$rtl empty=$empty', (
          tester,
        ) async {
          await f.pump(
            tester,
            data(keyboard: keyboard, rtl: rtl, empty: empty, accept: empty),
          );
          await tester.pumpAndSettle();
          final finder = find.byType(InputDatePickerFormField);
          final field = tester.widget<InputDatePickerFormField>(finder);
          expect(field.initialDate, empty ? null : DateTime(2024, 2, 29));
          expect(field.autofocus, isFalse);
          expect(field.focusNode, isNull);
          expect(field.acceptEmptyDate, empty);
          expect(field.fieldLabelText, 'Date label');
          expect(field.fieldHintText, 'Date hint');
          expect(field.errorFormatText, 'Bad format');
          expect(field.errorInvalidText, 'Bad date');
          expect(
            field.keyboardType,
            {
              'text': TextInputType.text,
              'multiline': TextInputType.multiline,
              'number': TextInputType.number,
              'numberSigned': const TextInputType.numberWithOptions(
                signed: true,
              ),
              'numberDecimal': const TextInputType.numberWithOptions(
                decimal: true,
              ),
              'numberSignedDecimal': const TextInputType.numberWithOptions(
                signed: true,
                decimal: true,
              ),
              'phone': TextInputType.phone,
              'datetime': TextInputType.datetime,
              'emailAddress': TextInputType.emailAddress,
              'url': TextInputType.url,
              'visiblePassword': TextInputType.visiblePassword,
              'name': TextInputType.name,
              'streetAddress': TextInputType.streetAddress,
              'none': TextInputType.none,
              'webSearch': TextInputType.webSearch,
              'twitter': TextInputType.twitter,
            }[keyboard],
          );
          expect(
            find.ancestor(of: finder, matching: find.byType(ExcludeFocus)),
            findsWidgets,
          );
          expect(
            find.ancestor(of: finder, matching: find.byType(AbsorbPointer)),
            findsWidgets,
          );
          final editable = tester.widget<EditableText>(
            find.byType(EditableText),
          );
          expect(editable.controller.text, empty ? '' : '02/29/2024');
          await tester.tap(find.byType(TextFormField), warnIfMissed: false);
          await tester.pump();
          expect(editable.focusNode.hasFocus, isFalse);
          expect(tester.takeException(), isNull);
        });
      }
    }
  }
  testWidgets('source dates and calendars are not executed', (tester) async {
    for (final name in [
      'firstDate',
      'lastDate',
      'initialDate',
      'calendarDelegate',
    ]) {
      final raw = data();
      ((raw['root'] as Map)['properties'] as Map)[name] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      expect(find.byType(InputDatePickerFormField), findsNothing);
      expect(
        find.textContaining(
          'project DateTime/calendar delegate preview unavailable',
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets('source callback predicate keyboard and focus are isolated', (
    tester,
  ) async {
    final raw = data();
    for (final name in [
      'onDateSubmitted',
      'onDateSaved',
      'selectableDayPredicate',
      'keyboardType',
      'focusNode',
    ]) {
      ((raw['root'] as Map)['properties'] as Map)[name] = {
        'kind': 'dartObjectReferencePresence',
      };
    }
    await f.pump(tester, raw);
    final field = tester.widget<InputDatePickerFormField>(
      find.byType(InputDatePickerFormField),
    );
    expect(field.selectableDayPredicate, isNull);
    expect(field.focusNode, isNull);
    expect(field.keyboardType, isNull);
    expect(() => field.onDateSubmitted!(DateTime(2024)), returnsNormally);
    expect(() => field.onDateSaved!(DateTime(2024)), returnsNormally);
    expect(tester.takeException(), isNull);
  });
  testWidgets('model edits update date text and clear with null', (
    tester,
  ) async {
    await f.pump(tester, data());
    await tester.pumpAndSettle();
    expect(
      tester.widget<EditableText>(find.byType(EditableText)).controller.text,
      '02/29/2024',
    );
    final raw = data();
    ((raw['root'] as Map)['properties'] as Map)['initialDate'] = s(
      '2025-01-02',
    );
    await f.pump(tester, raw);
    await tester.pumpAndSettle();
    expect(
      tester.widget<EditableText>(find.byType(EditableText)).controller.text,
      '01/02/2025',
    );
    await f.pump(tester, data(empty: true));
    await tester.pumpAndSettle();
    expect(
      tester.widget<EditableText>(find.byType(EditableText)).controller.text,
      isEmpty,
    );
    expect(tester.takeException(), isNull);
  });
}
