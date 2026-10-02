# DateRangePickerDialog

Pinned API: Flutter 3.44.8. Material → DateRangePickerDialog, after DatePickerDialog.
All 23 native constructor arguments are covered: 21 writable properties and two
optional Icon-only slots. The native constructor is const-capable; generated
local DateTime values correctly make the enclosing invocation non-const.
FD 17, Catalog API 16, Canvas model 20 and transport 1 are unchanged.

## Dates, ranges and calendars

firstDate and lastDate are required and default to 1900-01-01 / 2100-12-31 for a
new palette widget. currentDate and initialDateRange can be omitted or explicitly
null; omission does not invent a selected range. Today is supplied by the native
calendar delegate when currentDate is absent.

The shared date editor accepts Gregorian YYYY-MM-DD (years 0001–9999) or a
verified DateTime reference/getter/zero-argument factory. The range editor has
separate accessible Start date and End date fields. Its closed storage form is
YYYY-MM-DD/YYYY-MM-DD, converted to DateTimeRange(start: DateTime(...), end:
DateTime(...)), never emitted as a string or accepted as arbitrary Dart code.
Both endpoints are inclusive and same-day ranges are supported. Invalid dates,
reversed ranges and known endpoints outside known bounds reject the entire edit,
preserving both FD and Dart bytes.

Range source mode accepts verified non-null DateTimeRange<DateTime> values,
including current/imported getters, class static members and factories.
Explicit null remains a distinct local choice. Use typed sources for the wider
native Dart date range and UTC/time data; source expressions are preserved.
calendarDelegate supports Gregorian and any verified CalendarDelegate<DateTime>
implementation, including alternative calendars. Delegate conversion, parsing
and formatting stay user-owned. Custom calendar relationships are evaluated by
Flutter, not guessed by the Designer; the local DateTimeRange start <= end
invariant still applies because the native range constructor requires it.

## Properties, slots and predicate

All four entry modes are supported: calendar, input, calendarOnly and inputOnly.
There are twelve nullable text fields: help/cancel/confirm/save labels, three
validation messages, separate start/end hints and labels, and restorationId.
Empty text remains different from omission/null. Save text belongs to fullscreen
calendar mode; Confirm/Cancel text belongs to input mode.

keyboardType supports all sixteen reviewed TextInputType presets and a typed
source. Unlike DatePickerDialog, DateRangePickerDialog.keyboardType is
non-nullable: omit it for the datetime default. key supports a String ValueKey,
typed Key, null or omission. The two entry-mode icon slots accept actual Icon
nodes with the full Icon property editors, never arbitrary widget substitutions.

selectableDayPredicate has the exact signature:
`bool Function(DateTime day, DateTime? selectedStartDay, DateTime? selectedEndDay)`.
Properties supports typed source selection and create/navigate/rename actions.
The generated starter returns true; edit its body to constrain endpoint choices.
Nullable start/end arguments reflect partial selection. This is a predicate,
not a selected-range Event; it stays out of the Events tab. Handler bodies
survive regeneration, disconnect, Undo/Redo and save/reopen.

## Route result and lifetime

Use showDialog<DateTimeRange<DateTime>> / DialogRoute around the generated
dialog and await its result in application code. Calendar Save or input OK
returns a range through Navigator; cancellation returns null. This constructor
has no onChanged, onConfirm or entry-mode callback. Helper-level showDateRangePicker
barrier/builder options are not fabricated as constructor properties.

The selected range and entry mode are initial/restorable values, not controlled
State fields. A parent restoration scope and restorable route are needed for
route restoration; restorationId alone cannot provide that. Update application
State after awaiting a route result. No new State property consumers are added.
Native Material theme, localizations, RTL and orientation continue to apply.

## Canvas boundary

Local dates/ranges use the native fullscreen calendar or two-field input dialog.
Pointer input and descendant autofocus are suppressed in the isolated preview
to avoid dismissing its route or changing editor focus. A changed Designer model
remounts preview initial state; generated applications retain native lifecycle.

Project date/range/calendar sources show an explicit preview-unavailable message,
not fabricated dates or a substituted Gregorian calendar. Predicates/getters/
factories are never executed by Canvas. A project keyboard source previews the
disclosed datetime default. Run/Debug exercises application-owned values,
calendar logic, date restrictions, typing, restoration and Navigator results.

## Verification

The SDK suite passed 107 generated/native cases: current/imported/static date
and range getters/factories, custom calendars, all entry modes and keyboard
presets, LTR/RTL, nullable options, predicate create/rename/history, native
predicate arguments, calendar Save and input OK/Cancel Navigator results.
Fourteen incompatible Object/dynamic/nullable/wrong-signature sources were
rejected by strict analyzer proof without changing user files.

The complete Canvas suite passed 11,625 tests. After lint-only brace corrections,
all 13 dedicated range tests passed again, Flutter analyze reported no issues,
and release Web compilation passed. Swing tests verify all 21 writable rows,
two-field draft isolation, accessible labels, invalid/reversed date rejection,
omit/null, unchanged property-set identity, atomic history and exact reopen.
Pair-save tests cover both endpoint DateTime probes and reject forged
span/kind/library/file evidence.

After the full Java regression and targeted corrective rerun, Surefire reports
contain 6,246 cases: 5,976 passed, 270 SDK/environment skips, zero failures/errors.
The only final full-run failure was an old global predicate-count expectation
(four instead of five); the corrected six-test suite passed in the final build.
The separate new-widget SDK run above was explicitly enabled.

All nine PluginPackageMetadataIT checks passed. Package verification compared
implementation classes (including the range value and editor), 36 date/table
icon variants, 41 runner sources, 35 Web artifact files and all four
development-cluster JARs against current source/build output and the NBM.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
9,337,110 bytes. SHA-256:
`6a35513c69c6e9d03d83ae60e623c2bc473b67084e57e56a2717a0ac9e5ad4dc`.

Release main.dart.js: 4,734,439 bytes, SHA-256:
`91385af7d959e86ee3bfa2ecd898b0adc5aaac9451f7fba723c6b08a4aced338`.

No installed IDE/userdir was modified or restarted. Interactive NetBeans
desktop acceptance is separate and is not claimed here.

Inventory: 235 definitions, 7,780 property rows (7,762 outside Scaffold), 226 typed
definitions, 193 const-capable definitions; 259 callables across 96 types:
191 native Events across 67 types, 50 builders, five predicates, two formatters
and eleven callable delegates. State remains 178 consumers across 60 types.

Official references:
[constructor](https://api.flutter.dev/flutter/material/DateRangePickerDialog/DateRangePickerDialog.html),
[range predicate](https://api.flutter.dev/flutter/material/SelectableDayForRangePredicate.html),
[CalendarDelegate](https://api.flutter.dev/flutter/material/CalendarDelegate-class.html).
