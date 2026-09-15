import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;

Map<String, Object?> s(String value) => {'kind': 'string', 'value': value};
Map<String, Object?> en(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> data({
  String mode = 'dial',
  String? orientation,
  bool empty = false,
  bool rtl = false,
}) {
  final raw = f.modelJson();
  raw['root'] = f.node(f.fillId, canvasTimePickerDialogType, {
    'initialTime': s('23:59'),
    'initialEntryMode': en('TimePickerEntryMode', mode),
    if (orientation != null) 'orientation': en('Orientation', orientation),
    'cancelText': s('Cancel time'),
    'confirmText': s('Save time'),
    'helpText': s('Select time'),
    'errorInvalidText': s('Invalid time'),
    'hourLabelText': s('Hours'),
    'minuteLabelText': s('Minutes'),
    'emptyInitialInput': f.boolean(empty),
    'onEntryModeChanged': s('noop'),
  });
  if (rtl) {
    raw['root'] = f.node(
      f.headerId,
      'flutter.widgets.Directionality',
      {'textDirection': en('TextDirection', 'rtl')},
      {'child': f.single(raw['root'] as Map<String, Object?>)},
    );
  }
  return raw;
}

Map properties(Map<String, Object?> raw) =>
    (f.findNode(raw['root'] as Map<String, Object?>, f.fillId)['properties']
        as Map);
List<String> inputText(WidgetTester tester) => tester
    .widgetList<EditableText>(find.byType(EditableText))
    .map((e) => e.controller.text)
    .toList();
Widget nativeApp(Widget child, {bool hours24 = true}) => MaterialApp(
  restorationScopeId: 'app',
  home: MediaQuery(
    data: MediaQueryData(
      size: const Size(800, 600),
      alwaysUse24HourFormat: hours24,
    ),
    child: Scaffold(body: child),
  ),
);

void main() {
  test('exact reviewed contract and strict time domain', () {
    expect(
      canvasRuntimeWidgetSchemaContractForTesting().trim(),
      canvasReviewedWidgetSchemaContract.trim(),
    );
    expect(
      canvasDropSlotsForWidgetType(canvasTimePickerDialogType),
      hasLength(2),
    );
    for (final text in [
      '24:00',
      '23:60',
      '9:00',
      '09:0',
      '09:00\n',
      '09:00\r',
      ' 09:00',
      '09:00:00',
      'TimeOfDay.now()',
      '',
    ]) {
      final raw = data();
      properties(raw)['initialTime'] = s(text);
      expect(() => f.decode(raw), throwsFormatException, reason: text);
    }
    for (final name in [
      'initialTime',
      'initialEntryMode',
      'emptyInitialInput',
    ]) {
      final raw = data();
      properties(raw)[name] = {'kind': 'null'};
      expect(() => f.decode(raw), throwsFormatException, reason: name);
    }
    final raw = data();
    properties(raw).remove('initialTime');
    expect(() => f.decode(raw), throwsFormatException);
    for (final name in [
      'key',
      'orientation',
      'onEntryModeChanged',
      'cancelText',
      'confirmText',
      'helpText',
      'errorInvalidText',
      'hourLabelText',
      'minuteLabelText',
      'restorationId',
    ]) {
      final raw = data();
      properties(raw)[name] = {'kind': 'null'};
      expect(() => f.decode(raw), returnsNormally);
    }
    for (final mode in ['date', 'INPUT', 'input-only']) {
      final raw = data();
      properties(raw)['initialEntryMode'] = en('TimePickerEntryMode', mode);
      expect(() => f.decode(raw), throwsFormatException);
    }
  });
  for (final mode in ['dial', 'input', 'dialOnly', 'inputOnly']) {
    for (final orientation in <String?>[null, 'portrait', 'landscape']) {
      for (final empty in [false, true]) {
        for (final rtl in [false, true]) {
          testWidgets(
            'Canvas mode=$mode orientation=$orientation empty=$empty rtl=$rtl',
            (tester) async {
              await f.pump(
                tester,
                data(
                  mode: mode,
                  orientation: orientation,
                  empty: empty,
                  rtl: rtl,
                ),
              );
              await tester.pumpAndSettle();
              final finder = find.byType(TimePickerDialog),
                  dialog = tester.widget<TimePickerDialog>(
                    find.byType(TimePickerDialog),
                  );
              expect(dialog.initialTime, const TimeOfDay(hour: 23, minute: 59));
              expect(
                dialog.initialEntryMode,
                TimePickerEntryMode.values.byName(mode),
              );
              expect(
                dialog.orientation,
                orientation == null
                    ? null
                    : Orientation.values.byName(orientation),
              );
              expect(dialog.emptyInitialInput, empty);
              expect(dialog.cancelText, 'Cancel time');
              expect(dialog.confirmText, 'Save time');
              expect(dialog.helpText, 'Select time');
              expect(dialog.errorInvalidText, 'Invalid time');
              expect(dialog.hourLabelText, 'Hours');
              expect(dialog.minuteLabelText, 'Minutes');
              expect(
                find.ancestor(of: finder, matching: find.byType(AbsorbPointer)),
                findsWidgets,
              );
              expect(
                find.ancestor(of: finder, matching: find.byType(ExcludeFocus)),
                findsWidgets,
              );
              if (mode.startsWith('input')) {
                expect(inputText(tester), empty ? ['', ''] : ['11', '59']);
                for (final editable in tester.widgetList<EditableText>(
                  find.byType(EditableText),
                )) {
                  expect(editable.focusNode.hasFocus, isFalse);
                }
              } else {
                expect(find.byType(TextFormField), findsNothing);
              }
              await tester.tap(find.text('Save time'), warnIfMissed: false);
              await tester.pump();
              expect(find.byType(TimePickerDialog), findsOneWidget);
              expect(tester.takeException(), isNull);
            },
          );
        }
      }
    }
  }
  testWidgets(
    'Canvas time edits remount initial state while project time remains opaque',
    (tester) async {
      await f.pump(tester, data(mode: 'input'));
      await tester.pumpAndSettle();
      expect(inputText(tester), ['11', '59']);
      final raw = data(mode: 'input');
      properties(raw)['initialTime'] = s('00:07');
      await f.pump(tester, raw);
      await tester.pumpAndSettle();
      expect(inputText(tester), ['12', '07']);
      properties(raw)['onEntryModeChanged'] = {
        'kind': 'dartObjectReferencePresence',
      };
      await f.pump(tester, raw);
      await tester.pumpAndSettle();
      expect(
        () => tester
            .widget<TimePickerDialog>(find.byType(TimePickerDialog))
            .onEntryModeChanged!(TimePickerEntryMode.dial),
        returnsNormally,
      );
      properties(raw)['initialTime'] = {'kind': 'dartObjectReferencePresence'};
      await f.pump(tester, raw);
      expect(find.byType(TimePickerDialog), findsNothing);
      expect(
        find.textContaining('project TimeOfDay preview unavailable'),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'both Icon-only slots render exact icons and reject arbitrary widgets',
    (tester) async {
      final raw = data();
      final root = raw['root'] as Map;
      root['slots'] = {
        for (final name in [
          'switchToInputEntryModeIcon',
          'switchToTimerEntryModeIcon',
        ])
          name: f.single(
            f.node(
              name == 'switchToInputEntryModeIcon' ? f.bodyId : f.headerId,
              'flutter.widgets.Icon',
              {
                'icon': {
                  'kind': 'iconData',
                  'codePoint': 0xe8b8,
                  'fontFamily': 'MaterialIcons',
                  'fontPackage': null,
                  'fontFamilyFallback': <String>[],
                  'matchTextDirection': false,
                },
              },
            ),
          ),
      };
      await f.pump(tester, raw);
      await tester.pumpAndSettle();
      final widget = tester.widget<TimePickerDialog>(
        find.byType(TimePickerDialog),
      );
      expect(widget.switchToInputEntryModeIcon!.icon!.codePoint, 0xe8b8);
      expect(widget.switchToTimerEntryModeIcon!.icon!.codePoint, 0xe8b8);
      for (final name in [
        'switchToInputEntryModeIcon',
        'switchToTimerEntryModeIcon',
      ]) {
        final slots = root['slots'] as Map, previous = slots[name];
        slots[name] = f.single(
          f.node(f.bodyId, 'flutter.widgets.Text', {
            'data': s('Invalid child'),
          }),
        );
        expect(() => f.decode(raw), throwsFormatException);
        slots[name] = previous;
      }
      expect(tester.takeException(), isNull);
    },
  );
  for (final hours24 in [false, true]) {
    for (final mode in TimePickerEntryMode.values) {
      testWidgets(
        'native 12/24 hour=$hours24 mode=$mode toggle callback and only-mode affordance',
        (tester) async {
          final events = <TimePickerEntryMode>[];
          await tester.pumpWidget(
            nativeApp(
              TimePickerDialog(
                initialTime: const TimeOfDay(hour: 23, minute: 59),
                initialEntryMode: mode,
                onEntryModeChanged: events.add,
                switchToInputEntryModeIcon: const Icon(Icons.edit),
                switchToTimerEntryModeIcon: const Icon(Icons.schedule),
              ),
              hours24: hours24,
            ),
          );
          await tester.pumpAndSettle();
          expect(events, isEmpty);
          final isInput =
              mode == TimePickerEntryMode.input ||
              mode == TimePickerEntryMode.inputOnly;
          final only =
              mode == TimePickerEntryMode.inputOnly ||
              mode == TimePickerEntryMode.dialOnly;
          if (isInput) {
            expect(inputText(tester), [hours24 ? '23' : '11', '59']);
          }
          if (only) {
            expect(find.byIcon(Icons.edit), findsNothing);
            expect(find.byIcon(Icons.schedule), findsNothing);
          } else {
            await tester.tap(
              find.byIcon(isInput ? Icons.schedule : Icons.edit),
            );
            await tester.pumpAndSettle();
            expect(events, [
              isInput ? TimePickerEntryMode.dial : TimePickerEntryMode.input,
            ]);
          }
          expect(tester.takeException(), isNull);
        },
      );
    }
  }
  testWidgets(
    'native confirm returns selected time, invalid input retains route, cancel returns null',
    (tester) async {
      BuildContext? context;
      TimeOfDay? result;
      bool finished = false;
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (c) {
              context = c;
              return const Scaffold();
            },
          ),
        ),
      );
      Future<void> open() async {
        finished = false;
        result = await showDialog<TimeOfDay>(
          context: context!,
          builder: (c) => MediaQuery(
            data: MediaQuery.of(c).copyWith(alwaysUse24HourFormat: true),
            child: const TimePickerDialog(
              initialTime: TimeOfDay(hour: 9, minute: 0),
              initialEntryMode: TimePickerEntryMode.inputOnly,
              emptyInitialInput: true,
              errorInvalidText: 'Invalid time',
              confirmText: 'Save time',
              cancelText: 'Cancel time',
            ),
          ),
        );
        finished = true;
      }

      final first = open();
      await tester.pumpAndSettle();
      expect(inputText(tester), ['', '']);
      await tester.tap(find.text('Save time'));
      await tester.pumpAndSettle();
      expect(finished, isFalse);
      expect(find.text('Invalid time'), findsWidgets);
      await tester.enterText(find.byType(TextFormField).first, '23');
      await tester.enterText(find.byType(TextFormField).last, '59');
      await tester.tap(find.text('Save time'));
      await tester.pumpAndSettle();
      await first;
      expect(result, const TimeOfDay(hour: 23, minute: 59));
      expect(finished, isTrue);
      final second = open();
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cancel time'));
      await tester.pumpAndSettle();
      await second;
      expect(result, isNull);
      expect(finished, isTrue);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'native same key retains initial time mode and orientation while new key resets',
    (tester) async {
      Widget app(
        String key,
        TimeOfDay time,
        TimePickerEntryMode mode,
        Orientation orientation,
      ) => nativeApp(
        TimePickerDialog(
          key: ValueKey(key),
          initialTime: time,
          initialEntryMode: mode,
          orientation: orientation,
          switchToInputEntryModeIcon: const Icon(Icons.edit),
          switchToTimerEntryModeIcon: const Icon(Icons.schedule),
        ),
      );
      await tester.pumpWidget(
        app(
          'same',
          const TimeOfDay(hour: 9, minute: 7),
          TimePickerEntryMode.input,
          Orientation.portrait,
        ),
      );
      await tester.pumpAndSettle();
      expect(inputText(tester), ['09', '07']);
      await tester.pumpWidget(
        app(
          'same',
          const TimeOfDay(hour: 18, minute: 45),
          TimePickerEntryMode.dial,
          Orientation.landscape,
        ),
      );
      await tester.pumpAndSettle();
      expect(inputText(tester), ['09', '07']);
      await tester.pumpWidget(
        app(
          'new',
          const TimeOfDay(hour: 18, minute: 45),
          TimePickerEntryMode.inputOnly,
          Orientation.landscape,
        ),
      );
      await tester.pumpAndSettle();
      expect(inputText(tester), ['18', '45']);
      expect(find.byIcon(Icons.schedule), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'native restoration retains selected time and switched entry mode',
    (tester) async {
      await tester.pumpWidget(
        nativeApp(
          const TimePickerDialog(
            initialTime: TimeOfDay(hour: 9, minute: 7),
            initialEntryMode: TimePickerEntryMode.input,
            restorationId: 'time',
            switchToInputEntryModeIcon: Icon(Icons.edit),
            switchToTimerEntryModeIcon: Icon(Icons.schedule),
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField).first, '18');
      await tester.enterText(find.byType(TextFormField).last, '45');
      await tester.tap(find.byIcon(Icons.schedule));
      await tester.pumpAndSettle();
      await tester.restartAndRestore();
      await tester.pumpAndSettle();
      expect(find.byType(TextFormField), findsNothing);
      await tester.tap(find.byIcon(Icons.edit));
      await tester.pumpAndSettle();
      expect(inputText(tester), ['18', '45']);
      expect(tester.takeException(), isNull);
    },
  );
}
