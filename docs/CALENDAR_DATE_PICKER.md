# CalendarDatePicker

Pinned contract: Flutter 3.44.8, Material → CalendarDatePicker, palette order 650.
This is an inline calendar, not DatePickerDialog or a Navigator route.
All ten constructor arguments are represented by ten writable property rows.
The constructor is non-const and has no visual child slots.

## Dates and native initial state

initialDate is required but nullable. The creation value is explicit null: no
selected day, with navigation initially based on currentDate/the calendar clock.
firstDate and lastDate are required, non-null and default to 1900-01-01 and
2100-12-31 in the Designer creation seed. currentDate can be omitted or null.
Local dates use strictly validated Gregorian YYYY-MM-DD, years 0001–9999.
Impossible dates, reversed bounds and an out-of-bounds initial selection reject
the entire edit without changing either FD or Dart.

Date source mode supports current/imported getters, static members and
zero-argument factories. Initial/current sources accept DateTime? as well as
DateTime; bounds require DateTime. Object/dynamic/incompatible types fail
strict analyzer proof. Native UTC/time values and dates outside the local
editor range remain available through typed sources.

calendarDelegate supports the Gregorian preset or a verified, non-null
CalendarDelegate<DateTime> implementation. Custom date normalization, month
arithmetic and calendar semantics stay source-owned; the Designer does not
guess their bounds or execute the delegate.

initialDate and initialCalendarMode (day/year) are initialization inputs.
Same-key rebuilds retain native calendar state. Change Key to recreate the
calendar with new initial values. Key accepts String ValueKey, a typed Key,
explicit null or omission. No new controlled State consumers are fabricated.

## Events and date predicate

- onDateChanged: required ValueChanged<DateTime>, the default Event. The
  creation/disconnected value is an explicit no-op. Null/omission are rejected.
  It reports native day selection and selectable year navigation.
- onDisplayedMonthChanged: optional ValueChanged<DateTime>, reporting the
  delegate's first day of a newly displayed month, including year navigation.
  It is not called on every rebuild or every day selection.
- selectableDayPredicate: optional bool Function(DateTime). It stays in
  Properties, not Events. Create/select/navigate/rename an editable handler;
  the generated starter returns true. A non-null initial date must satisfy it.

Callbacks/predicates also accept verified source references and factories.
User method bodies survive regeneration, rename, disconnect, Undo/Redo and
save/reopen. Optional callbacks/predicate may be explicitly null or omitted.
These are constructor callbacks, not invented onConfirm/onCancel/route events.

Place the generated calendar under Material, MaterialLocalizations and
Directionality ancestors, normally MaterialApp + Scaffold. Application code
owns event responses, changing keys, theme, localization and keyboard behavior.

## Canvas boundary

The isolated preview uses a native CalendarDatePicker for local dates,
including day/year and LTR/RTL. A transparent Material ancestor satisfies its
ink requirement; AbsorbPointer and ExcludeFocus prevent date selection,
calendar navigation and focus changes from hijacking Designer operations.
Designer model edits deliberately remount the preview's initial state.

Project date/calendar sources show an explicit preview-unavailable placeholder,
not fabricated dates or a substituted calendar. Canvas never executes project
getters, factories, callbacks or predicates. Run/Debug exercises those branches
and native state retention. No installed IDE/userdir is modified or restarted
by the automated verification.

## Inventory

236 admitted definitions, 7,790 writable property rows (7,772 outside Scaffold),
227 typed definitions, nine propertyless definitions and 193 const-capable ones.
262 callables across 97 types: 193 Events across 68 types, 50 builders, six
predicates, two formatters and eleven callable delegates. State remains 178
consumers across 60 types. FD 17, Catalog API 16, Canvas model 20 and transport 1
are unchanged. Global placement covers 47,908 pairs: 30,668 accepted / 17,240 rejected.

## Verification

The SDK suite passed 120 generated/native cases: local/current/imported/static
date and callback sources, factories, nullable dates, custom calendars, both
modes, LTR/RTL, keys, event/predicate creation and rename, exact save/reopen and
Undo/Redo. Native tests exercise day selection, month/year navigation, predicate
filtering, null selection and same-key versus changed-key lifecycle.
Sixteen incompatible Object/dynamic/nullable/wrong-signature sources were
rejected by strict analyzer proof without modifying user files.

Swing tests cover all ten writable rows, stable property-set identity and the
required-null initial-date editor. Null and local-date drafts are independent;
switching source or cancelling never commits the inactive draft.
Command tests preserve user bodies and exact FD/Dart history, reject invalid
dates/required resets, and restore the required event's no-op on disconnect.
Pair-save tests reject forged DateTime spans, IDs, kinds, libraries and paths.

The complete Canvas suite passed 11,637 tests, including 12 dedicated calendar
cases. The Java parity gate then caught an unnecessary empty CalendarDatePicker
entry in the container-only drop table. Removing it preserves the ordinary
leaf fallback. All 281 focused calendar/view/DnD tests subsequently passed;
Flutter analyze reported no issues and final release Web compilation passed.

After the full Java regression and targeted corrective build, fresh Surefire
reports contain 6,258 cases: 5,987 passed, 271 SDK/environment skips and zero
failures/errors. The explicitly enabled SDK run is recorded separately above.
The full test JVM also logged a NetBeans file-lock/shutdown warning; the final
targeted install completed with exit code zero.

All nine PluginPackageMetadataIT checks passed. Package verification compared
implementation classes, 40 date/table icon variants, 41 runner sources,
35 Web artifact files and all four development-cluster JARs against current
source/build output and the NBM.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
9,343,705 bytes. SHA-256:
`bc7f9ccef82aa045e1c22cfa596ec18751dc44b9011dc71bd0074e00a18f384c`.

Release main.dart.js: 4,736,854 bytes. SHA-256:
`7a3077c2ce39bdbae09d96e38d03b513524dc6dae6c3154e969c6496e6f6226a`.

Interactive acceptance in a running NetBeans desktop was not performed or
claimed. The installed IDE/userdir and user Flutter projects were untouched.

Official sources:
[constructor](https://api.flutter.dev/flutter/material/CalendarDatePicker/CalendarDatePicker.html),
[initialDate lifecycle](https://api.flutter.dev/flutter/material/CalendarDatePicker/initialDate.html),
[displayed month callback](https://api.flutter.dev/flutter/material/CalendarDatePicker/onDisplayedMonthChanged.html).
