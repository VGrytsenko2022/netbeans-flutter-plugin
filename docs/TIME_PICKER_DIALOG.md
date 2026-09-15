# TimePickerDialog

Pinned API: Flutter 3.44.8, Material palette order 670.
[Official constructor](https://api.flutter.dev/flutter/material/TimePickerDialog/TimePickerDialog.html).
All 15 constructor arguments are implemented: 13 writable Properties and two
optional single-child slots restricted to Icon. The constructor is const-capable.

## Constructor coverage

| Argument | Designer representation |
| --- | --- |
| key | String ValueKey, verified Key source, null or omission |
| initialTime | Required HH:mm or verified TimeOfDay source; creation seed 09:00 |
| initialEntryMode | dial, input, dialOnly, inputOnly; omission uses native dial |
| orientation | portrait, landscape, null or omission (MediaQuery) |
| emptyInitialInput | Omission or explicit non-null boolean checkbox; native false |
| cancelText, confirmText, helpText | Localized override strings, explicit empty, null or omission |
| errorInvalidText, hourLabelText, minuteLabelText | Localized validation/input labels, explicit empty, null or omission |
| restorationId | String, null or omission |
| onEntryModeChanged | Editable Event handler, typed EntryModeChangeCallback, no-op, null or omission |
| switchToInputEntryModeIcon | Optional Icon-only slot |
| switchToTimerEntryModeIcon | Optional Icon-only slot |

The two-field time editor accepts hours 0–23 and minutes 0–59, serializes HH:mm
and never commits partially edited drafts. Source/local drafts are independent.
TimeOfDay has no date, timezone or seconds. All 1,440 minute values are valid;
invalid bounds, signs, whitespace, alternate digits and trailing data reject the
whole FD/Dart edit. Null and omission cannot replace the required initialTime.
Generated local time is const TimeOfDay(hour: ..., minute: ...).

Typed sources include current/imported getters, fields, static members and
zero-argument factories. Strict analyzer type/provenance proof rejects dynamic,
Object, nullable TimeOfDay, nullable callbacks and wrong callback signatures.
The Canvas payload carries presence only, never the source expression or identity.

## Native lifecycle, routes and Events

MaterialApp supplies localization, MediaQuery and directionality. Display follows
the locale and ambient 12/24-hour preference; HH:mm storage does not force the
display format. Orientation changes layout, not the selected time. Native minimum
dialog sizes still apply: a forced landscape dialog needs sufficient width.

dial/input allow toggling; dialOnly/inputOnly hide the toggle. The exact event is
void Function(TimePickerEntryMode mode), called when entry mode changes, not when
the time changes or the user confirms/cancels. Handler bodies survive rename,
disconnect, regeneration, Undo/Redo and save/reopen.

Confirm validates input, then Navigator.pop returns TimeOfDay. Cancel returns
null. Insert the generated dialog inside a DialogRoute/showDialog and await that
result in application code; placing a dialog directly on a page is not route
creation. The Designer does not invent onChanged/onConfirm properties.

initialTime, initialEntryMode and orientation seed native restorable state.
Changing them with the same Key does not replace retained state. A new Key resets
the dialog. A null/omitted stored orientation follows MediaQuery dynamically.
emptyInitialInput initially clears input fields in input/inputOnly; it does not
change dial selection and can clear newly-created input fields after a toggle.
restorationId preserves selected time, mode, orientation and validation state
inside a RestorationScope. Restoring the route itself requires a restorable
DialogRoute; the property alone does not restore a popped route.

There is no controlled value/controller constructor parameter. The State
inventory remains 178 consumers across 60 widget types.

## Canvas boundary and verification

Canvas uses the native Material TimePickerDialog for local values with
AbsorbPointer and ExcludeFocus: it cannot focus inputs, invoke application code
or close routes. Model changes recreate preview state so edits appear immediately.
A source-backed initialTime produces an explicit unavailable preview instead of
an invented time. Source callbacks are represented by inert no-ops. Run/Debug
uses the actual saved source and native route behavior.

Coverage includes exact schemas/requiredness, every minute value, property draft
validation, both Icon slots, generator/FD round trips, event/history integrity,
real SDK analysis and native rendering, all modes/orientations/LTR/RTL, 12/24-hour
presentation, route results, invalid input, Key changes and state restoration.
Light/dark 16/32 SVG families are visually reviewed. No installed IDE or userdir
is modified by these tests.

Verified on 2026-09-15:
- 62 dedicated Canvas/native tests, plus the full 11,768-test Canvas suite.
- 56 generated-code native SDK cases; seven unsafe source shapes rejected.
- Full Java reactor: 6,280 total, 6,006 passed, 274 conditional skips, zero
  failures/errors; Maven exit 0. The existing NetBeans shutdown-hook lock warning
  and Surefire fork-exit timeout still appear after successful test completion.
- Flutter analyze: no issues; release Web build successful.

Package verification: Maven install and nbm:cluster exit 0; all nine metadata
integration tests passed. The NBM contains byte-matching schema/editor/event
classes, 48 table/date/time SVG variants, all 41 runner sources and 35 Web files.
All four development-cluster JARs match the NBM.

Interactive testing in an already-open NetBeans desktop was not performed.
