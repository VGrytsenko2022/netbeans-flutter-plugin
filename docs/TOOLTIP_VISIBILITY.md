# TooltipVisibility — inherited tooltip policy

Baseline: Flutter **3.44.8**, checked against the installed
`material/tooltip_visibility.dart`, `material/tooltip.dart` and
`widgets/raw_tooltip.dart`, together with the official
[TooltipVisibility API](https://api.flutter.dev/flutter/material/TooltipVisibility-class.html)
and [constructor](https://api.flutter.dev/flutter/material/TooltipVisibility/TooltipVisibility.html).

## Complete constructor and Designer workflow

Material `flutter.material.TooltipVisibility`, palette order 310, covers all
three constructor arguments: the Designer-owned key, required non-null boolean
**Visible**, and required single **Child**. The constructor is const-capable.
Designer creates an explicit `visible: true`; Flutter has no constructor default
for this required argument. False is a real value, not omission. Null, unset,
string booleans, arbitrary Dart expressions and direct Dart object references
are rejected; reviewed State bindings are a separate supported mechanism.

Visible uses the shared centered checkbox editor. The shared required-child
palette/tree/root wrapping workflow creates the wrapper and supplies its child
in one transaction. An empty prototype is not independently insertable; no
artificial placeholder child is persisted. Child replacement, nesting, moves,
save/reopen and Undo/Redo retain the normal required-slot validation.

The existing shared Designer placement policy still requires Expanded,
Flexible and Spacer to be direct model children of Row/Column. It does not
traverse transparent inherited wrappers, even though native Flutter can permit
that ancestry. Therefore wrapping an existing Expanded in TooltipVisibility is
rejected; use `Expanded(child: TooltipVisibility(...))` instead. This is a
documented Designer placement boundary, not a Flutter constructor restriction.

## State and runtime behavior

Visible is one non-null boolean **State consumer**. Existing typed fields can
drive it through the shared reviewed boolean transformations. The wrapper has
no callback, Event or controlled-value State producer; it does not invent an
`onChanged` or expose one tooltip's transient overlay state.

The **nearest scope wins**, not an AND of ancestors. A true inner scope can
enable its tooltips inside a false outer scope. With no scope, the native SDK
defaults to true. False disables Material Tooltip activation by hover, tap,
long press and `ensureTooltipVisible()`. It removes the SDK RawTooltip instance
and closes an already open overlay; re-enabling permits fresh activation.
This does not hide, disable or remove the actual Child widget.

**Pinned SDK accessibility nuance:** the broad API prose says that semantic
information is retained. Actual Flutter 3.44.8 preserves the child's semantic
label, but removes RawTooltip's own tooltip-message annotation while disabled.
The generated-app test verifies both facts and restoration on re-enable.
Designer and Canvas do not add an invented semantic compatibility wrapper.

The generated application keeps normal Flutter lifecycle behavior. Persistent
Designer IDs are not a promise that arbitrary unkeyed application child State
survives every internal SDK wrapper transition.

## Canvas and source integrity

Canvas uses native TooltipVisibility, with exact local true/false values and
nearest-scope behavior. A State binding preserves the stored literal preview
value; Canvas does not execute the project's State fields or callbacks.
The policy also reaches implicit SDK tooltips owned by IconButton and
FloatingActionButton. Ordinary visibility edits preserve the outer Tooltip
State. If removing/reinserting its RawTooltip reparents an independently enabled
nested tooltip, Canvas closes only the affected transient nested preview to
avoid an SDK OverlayPortal layout mutation. Stored IDs and properties remain
unchanged, and the nested preview can reopen. Empty implicit tooltip text does
not count as an active overlay owner; diagnostic UI is not a model tooltip.
No Canvas/model/FD/protocol format change is needed for the existing required
boolean plus single-child representation.

At the TooltipVisibility milestone, before TooltipTheme: **99 widgets, 4,514 properties** (4,496 outside Scaffold),
**91 const-capable definitions** and **155 State consumers across 52 types**.
Callables remain 155 across 30 types: 133 Events across 27 types, 17 builders,
two predicates, two formatters and one delegate. Controlled families remain 11.
FD 16, Catalog API 15, Canvas model 19 and transport protocol 1 are unchanged.
For the current inventory, see [TooltipTheme](TOOLTIP_THEME.md).

Required Child does not create an empty drop destination. The exact ordinary
insertion matrix has 99 sources × 89 destinations = **8,811 candidates**:
**8,360 accepted, 451 rejected**. Required-slot wrapping is checked separately.

## Verification

Measured on 2026-09-09:

- Full Designer core: **2,122 tests across 254 suites**, all passed with zero
  failures, errors or skips, including pinned SDK constructor/callable checks
  and the expanded required-wrapper/State/placement contracts.
- Generated-app real SDK test: **1 Java / 13 Flutter tests**, all passed.
  Nine mode/platform cases cover manual/tap/long-press on Android, iOS and
  Windows TargetPlatform variants; four additional cases cover nearest-scope
  overrides, already open overlay shutdown/re-enable, exact accessibility
  behavior and cancellation of a pending hover timer.
- **Two real analyzer/pair-save transitions** passed: renamed boolean State
  binding and disconnect to the retained literal. Both retain the exact loaded
  source/FD baseline and user-owned callback bodies. Save/reopen and Undo restore
  exact source; invented events/producers and invalid required booleans are
  rejected without changing the pair.
- **12 focused UI/workflow tests across eight suites** passed with zero
  failures, errors or skips: centered required boolean editing, State drafts,
  palette/root/slot wrapping, all SVG variants, and transactional
  save/reopen/Undo/Redo preserving the required child and user source.
- Full Canvas Flutter suite: **2,006 tests**, all passed; **19 focused
  TooltipVisibility tests** cover literal/nearest-scope behavior, native
  activation and accessibility parity, geometry/drop targets, ordinary anchor
  identity and nested overlay cleanup, including implicit IconButton/FAB
  tooltips and empty/nonempty tooltip transitions. Flutter analyzer is clean.
- Offline Web release completed. All **40 source + 35 Web manifest entries**
  were independently checked for exact sizes, SHA-256 and duplicate paths.
  `main.dart.js`: **3,430,373 bytes**, SHA-256
  `3fb1ae5027bf7f0f2b89966ee660c3a508342c0f5250dc6fb70fe2f67e2cee5a`.

- Final coordinated Maven `verify`: **1,444 plugin tests**, **40 analyzer
  tests** and **9 Failsafe integration tests**, all passed with zero failures,
  errors or skips. This is the selected complete milestone regression set,
  including the full palette tree-drop adapter suite and artifact contracts,
  not a claim of running every unrelated plugin test.
- Reactor `install` and both root/module `nbm:cluster` commands completed.
  All **1,694 non-manifest module payload entries** match the NBM and both
  development clusters. Designer, analyzer and Canvas dependency JARs match
  by SHA-256; the new schema class, FD 16 schema and all four TooltipVisibility
  SVG variants are present.

Verified package at this milestone (later builds replace the same output path):
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
— **8,348,019 bytes**, built 2026-09-09 07:48:16 UTC, SHA-256
`f54644450ea6a795d37327cd01990579c501665aaca969ca5ad98feacfcaeb35`.

The user's application and running IDE have not been modified or restarted.
