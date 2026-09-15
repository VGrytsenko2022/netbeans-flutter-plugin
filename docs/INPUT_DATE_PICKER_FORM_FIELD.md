# InputDatePickerFormField

Pinned contract: Flutter 3.44.8, Material palette order 660. Non-const leaf widget.
All 16 constructor parameters have writable property rows; no invented slots.

## Dates, sources and constructor coverage

- key: String ValueKey, typed Key, null or omission.
- initialDate: optional strict Gregorian YYYY-MM-DD, typed DateTime? or null.
- firstDate/lastDate: required non-null dates, creation defaults 1900-01-01 and
  2100-12-31. Bounds inclusive and ordered; initial date must be in range.
- onDateSubmitted/onDateSaved: optional ValueChanged<DateTime>, editable handlers,
  verified source references/factories, explicit no-op, null or omission.
- selectableDayPredicate: optional typed SelectableDayPredicate, including editable
  bool Function(DateTime) handlers; non-null initial date must satisfy it.
- errorFormatText/errorInvalidText/fieldHintText/fieldLabelText: strings including
  explicit empty strings, null or omission (ambient localized defaults).
- keyboardType: all 16 TextInputType presets, typed source, null or omission.
- autofocus/acceptEmptyDate: optional non-null booleans (SDK defaults false),
  centered checkbox editors with omission retained.
- focusNode: verified FocusNode? reference/getter/factory, null or omission.
- calendarDelegate: Gregorian preset or verified CalendarDelegate<DateTime>.
  Application-owned custom calendars support native conversion, parsing and
  formatting; their semantic constraints are not guessed by the Designer.

Local dates are Gregorian years 0001–9999. Impossible dates, reversed bounds and
out-of-range initial dates reject the whole edit atomically. Typed sources admit
the wider native DateTime range. A project calendar owns cross-field relations,
but impossible local date strings are still rejected. Sources support current
and imported values, static members, getters and zero-argument factories.
Strict analyzer type/provenance proof rejects dynamic and incompatible values.

## Native lifecycle and Events

Use MaterialApp/Scaffold ancestors for Material, localization and directionality.
Parsing and formatting follow the ambient locale/calendar; labels do not change
the parse format. Submission only reports a valid date. FormState.save() invokes
onDateSaved independently; it does not fire on submission. FormState.validate()
displays format versus out-of-range/predicate validation messages.
acceptEmptyDate allows empty validation but never emits null or a previous date.

Unlike CalendarDatePicker, changing initialDate updates the private controller
on the next frame with the same Key. An unchanged initialDate retains typed text;
null clears it. A changed Key recreates the native form/controller state.
No public controller, live onChanged, restorationId or controlled value argument
exists. The Designer does not invent such constructor properties. State inventory
therefore remains 178 consumers across 60 types.

A supplied FocusNode belongs to application code; retain and dispose it there,
and do not allocate fresh nodes each build. Autofocus selects initial text once.
Create/select/navigate/rename event and predicate handlers through existing
source actions. User bodies survive disconnect, regeneration and save/reopen;
Undo/Redo preserves exact FD/Dart bytes. Predicates stay outside the Events tab.

## Canvas boundary

Local values render a native InputDatePickerFormField under transparent Material.
AbsorbPointer and ExcludeFocus suppress typing; autofocus is disabled and project
FocusNodes are never shared. Source callbacks become inert no-ops; predicates are
omitted. Source keyboard types use native datetime. Project date/calendar sources
show an explicit preview-unavailable message instead of fabricated values.
Designer model edits remount the isolated preview. Run/Debug exercises real
input, locale parsing, focus, validation, form saving and all project sources.

## Inventory

237 definitions, 7,806 writable rows (7,788 outside Scaffold), 228 typed definitions,
nine propertyless definitions, 193 const-capable definitions, 63 Material entries.
265 callables across 98 types: 195 Events across 69 types, 50 builders, seven
predicates, two formatters and eleven delegates. Placement checks cover 48,111
pairs: 30,842 accepted and 17,269 rejected. FD17/CatalogAPI16/Canvas20/transport1
remain unchanged.

## Verification

The explicit SDK suite passed 215 generated/native scenarios and rejected
20 Object/dynamic/nullable/wrong-signature sources through strict analysis.
Coverage includes current/imported/static/member/factory references, nullable
date/focus sources, all 16 keyboard presets, both booleans, labels, null/omission,
event and predicate create/rename, exact save/reopen and Undo/Redo.
Native tests distinguish format/range/predicate errors, submit versus Form.save,
empty validation without null events, same-key date updates and focus ownership.

All 11,706 Canvas tests passed, including 69 dedicated input-date scenarios:
16 keyboard mappings across LTR/RTL and empty/selected input, non-execution of
project sources, inert callbacks, date remounts and custom-calendar parse/format.
A final 82-case input/submenu smoke run also passed after restoring a test label.
Flutter analyze reported no issues; release Web compilation passed.

Core and Swing tests cover exact arguments/requiredness, strict local dates,
optional null/omission drafts, stable property sets, generated imports and user
method preservation. Pair-save rejects forged DateTime spans/IDs/kinds/paths.
The full Java reactor finished with exit code zero. Fresh Surefire reports:
6,270 cases, 5,997 passed, 273 SDK/environment skips, zero failures/errors.
The explicitly enabled SDK suite is recorded separately above. NetBeans logged
a file-lock shutdown exception and a delayed fork exit after the tests; no
test failed, and Maven returned zero. The final targeted install also passed.
All nine PluginPackageMetadataIT checks passed. Package verification compared
implementation classes, 44 date/table SVG variants, 41 runner sources and
35 Web files with the current build; all four cluster JARs match the NBM.

NBM: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
9,350,888 bytes; SHA-256:
`a7cde1ed44a25e1bd7b58013783e205cf6657d243ea563cdbf3763c7fc6e177f`.

Release main.dart.js: 4,739,879 bytes; SHA-256:
`2bca340e11f112d75c28a1aeb90aae90ec08605d91382d6134317d98c8f079df`.

Interactive acceptance in a running NetBeans desktop is separate: this work does
not modify/restart the installed IDE, userdir or sample Flutter projects.

Official sources:
[constructor](https://api.flutter.dev/flutter/material/InputDatePickerFormField/InputDatePickerFormField.html),
[class and lifecycle](https://api.flutter.dev/flutter/material/InputDatePickerFormField-class.html).
