# DatePickerDialog

Pinned API: Flutter 3.44.8. Material → DatePickerDialog, after PaginatedDataTable.
This non-const constructor exposes all 22 arguments: 20 writable properties and
two optional, exact Icon-only visual slots. FD 17, Catalog API 16, Canvas model
20 and transport 1 stay unchanged.

## Dates and calendar

firstDate and lastDate are required. New widgets use 1900-01-01 and 2100-12-31.
initialDate and currentDate may be omitted or explicitly null. An absent initial
date means no selected date; Confirm stays disabled until a date is chosen.
An absent current date uses calendarDelegate.now(), not a fabricated saved date.

The date editor supports a validated Gregorian YYYY-MM-DD value, years 0001–9999,
or a verified non-null DateTime project value/getter/zero-argument factory.
Imported sources and class static members are supported. Use a typed source for
the wider native Dart date range. UTC/time-of-day source values are retained in
generated Dart and normalized by the selected delegate's dateOnly operation.
The UI never accepts raw Dart expressions as date strings.

Local Gregorian bounds are inclusive; first must not exceed last, and a known
initial date must be within known bounds. Invalid leap dates and month/day
rollovers are rejected before committing a command. currentDate may be outside
the selectable range. With a project calendar delegate, model validation does
not pretend to know its conversion or ordering semantics: those relationships
and selectable-day rules are verified by the application at runtime.

calendarDelegate supports the Gregorian preset or any verified non-null
CalendarDelegate<DateTime> implementation, including alternate calendars and
project getter/factory sources. Its full conversion, navigation, formatting,
parsing and localization API remains editable Dart code. No custom-calendar
branch is silently converted to Gregorian.

## Fields, slots and source actions

All four DatePickerEntryMode values (calendar, input, calendarOnly, inputOnly)
and both DatePickerMode values (day, year) are available. Eight nullable text
fields include button/help labels, format/invalid-date messages, input hint/
label and restoration ID; an empty string is an explicit value, not omission.
keyboardType offers all 16 TextInputType presets, a typed source, or null/default.

insetPadding supports physical non-negative EdgeInsets or a typed EdgeInsets
source; omitted defaults are horizontal 16 / vertical 24. Directional insets
and explicit null are rejected because this constructor does not accept them.
key supports a string ValueKey, a typed Key, explicit null or omission.

switchToInputEntryModeIcon and switchToCalendarEntryModeIcon are real Icon-only
slots, each using the complete existing Icon editor. Arbitrary widgets cannot
be dropped into native Icon parameters. Palette, tree, slots, Canvas targets,
Undo/Redo and save/reopen share the same placement constraints.

Events exposes onDatePickerModeChange(DatePickerEntryMode). It supports typed
handler create/select/navigate/rename, an explicit no-op, null or omission.
Properties exposes selectableDayPredicate(bool Function(DateTime)) with source
actions and an initial return-true handler. Edit its body to restrict dates;
initialDate must satisfy it. Handler bodies remain user-owned during regeneration.

## Application usage and lifecycle

Confirm returns DateTime through Navigator.pop; Cancel returns null. Present the
dialog with showDialog<DateTime> or a DialogRoute and await its result in user
code. There is no onChanged/onConfirm parameter to synthesize. Helper-only
showDatePicker options such as barrier settings are not constructor properties.

Initial dates and modes are not controlled State fields. A rebuild does not
reset native restorable dialog state. Restoration additionally needs a parent
restoration scope and a restorable DialogRoute; restorationId alone is not enough.
Theme, Material localizations, orientation and directionality are inherited.
The existing State consumer inventory is deliberately unchanged.

## Isolated Canvas

Local dates use the native dialog in all four modes, with native labels,
calendar/input layout and Icon geometry. Preview disables pointer interaction
and descendant focus to prevent accidental route dismissal or input focus.
Saved property changes remount its isolated initial state for preview only.

Project DateTime or CalendarDelegate sources show an explicit preview-unavailable
placeholder rather than invented dates or a substituted calendar. Predicates,
callbacks, getters and factories are never executed by Canvas. Typed keyboard
and inset sources use disclosed preview defaults. Test actual date filtering,
custom calendars, focus/keyboard input and Navigator results in Run/Debug.

## Verification

After the full Java regression and focused corrective reruns, the current
Surefire reports contain 6,238 cases: 5,969 passed, 269 explicit SDK/environment
skips, zero failures/errors. The new command test verifies invalid-date/reset
rejection, edited predicate bodies, rename/disconnect, Undo/Redo, Icon insertion
and exact save/reopen. The 81-case pair-save evidence suite passes, including
closed DateTime constructor admission and forged-span/kind/library/file rejection.

The DatePickerDialog SDK test separately passed 91 generated/native cases:
all four entry modes and two calendar modes, all keyboard presets, LTR/RTL,
local/current/imported/static getter and factory sources, custom calendars,
strict rejection of Object/dynamic/nullable required sources, typed callbacks,
predicate execution and native Navigator Confirm/Cancel results.

The final complete Canvas suite passed all 11,612 tests. Flutter analyze reported
no issues; release Web compilation passed. Packaging passed all nine metadata
integration tests. Byte/hash verification covered the implementation classes,
all four DatePickerDialog SVGs (plus 28 table variants), 41 runner sources and
35 Web artifact files. All four development-cluster JARs match the NBM.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
(9,323,912 bytes), SHA-256:
`3ca022cc76516540108cbe87d86165090db3f60ee7c3a2fd0445b9c7827be3ee`.

Current inventory: 234 definitions, 7,759 writable property rows (7,741 outside
Scaffold), 191 native Events across 67 types and 258 callables across 95 types.
The 178 State consumers across 60 types are unchanged.

No installed IDE/userdir was modified or restarted. Interactive NetBeans desktop
acceptance is separate and is not claimed here.

Official references:
[DatePickerDialog constructor](https://api.flutter.dev/flutter/material/DatePickerDialog/DatePickerDialog.html),
[CalendarDelegate](https://api.flutter.dev/flutter/material/CalendarDelegate-class.html),
[GregorianCalendarDelegate](https://api.flutter.dev/flutter/material/GregorianCalendarDelegate-class.html).
