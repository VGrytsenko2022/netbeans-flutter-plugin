# TooltipTheme — complete nearest tooltip theme

Baseline: installed Flutter **3.44.8**, checked against
`packages/flutter/lib/src/material/tooltip_theme.dart`, `tooltip.dart` and
`widgets/raw_tooltip.dart`, together with the official
[TooltipTheme API](https://api.flutter.dev/flutter/material/TooltipTheme-class.html)
and [TooltipThemeData constructor](https://api.flutter.dev/flutter/material/TooltipThemeData/TooltipThemeData.html).

## Constructor and property contract

Material `flutter.material.TooltipTheme` has the Designer-owned Key, required
Data and required Child. The palette provides **47 editable properties**:
one whole Data reference, all **15 TooltipThemeData constructor fields**, and
**31 shared TextStyle leaves**. Its const capability is preserved.

The two Data forms are deliberately exclusive:

- Leave whole Data unset to construct a fresh `TooltipThemeData` from local
  fields. With no fields this emits `const TooltipThemeData()`, not null.
- Supply a strictly typed, non-null `TooltipThemeData` reference, getter,
  member or zero-argument factory, including imported values.

Whole Data conflicts with every local field, including explicit null and local
State bindings. Such edits are rejected without deleting stored values. Reset
Data before editing local fields; explicitly reset local fields/disconnect
their bindings before selecting whole Data. Object construction and factory
lifecycle remain user-owned; Canvas never executes project references.

The 15 native fields are Height, Constraints, Padding, Margin, Vertical offset,
Prefer below, Exclude from semantics, Decoration, Text style, Text align,
Wait duration, Show duration, Exit duration, Trigger mode and Enable feedback.
Durations are stored as exact signed integer microseconds (`...DurationUs`),
strict non-null Duration references/factories, or explicit null. Numeric
Height/Vertical offset retain signed and non-finite source values; unsafe
mounted geometry is diagnosed separately in Canvas. Non-null Height and
Constraints are exclusive. Height is retained as a deprecated SDK branch.

Padding/Margin support signed physical and directional insets and strict
EdgeInsetsGeometry references. Constraints uses the full shared BoxConstraints
editor. Decoration supports the reviewed local BoxDecoration compounds
(border/radii, gradients, shadows and declared images) or any strictly typed
Decoration reference, including ShapeDecoration/custom implementations.
There is no new visual ShapeDecoration editor: those objects remain in Dart.

Whole Text style (including explicit null) is exclusive with all 31 local
TextStyle leaves. Existing atomic color/foreground, background-color/background
paint, and package/font-family rules apply. Inner compound edits preserve
unrelated theme fields and the required Child.

## Native inheritance and runtime semantics

An explicitly non-null property on Tooltip overrides the nearest theme field.
The **nearest TooltipTheme replaces its outer tooltip theme; it does not merge
individual fields**. Empty or explicit-null fields on the nearest data use
Tooltip's own SDK/platform defaults, not the outer scope's corresponding field.

Generation and Canvas construct fresh data. They do not compose it through
`copyWith` or `lerp`: the installed 3.44.8 `copyWith` implementation does not
forward Exit duration, and `lerp` does not retain the five timing/trigger/feedback
fields. The plugin neither modifies the SDK nor pretends those helpers preserve
all fields.

Trigger modes are manual, tap and longPress. Manual does not disable mouse
hover. Wait duration is hover delay, Show duration is the touch-release visible
duration, and Exit duration is the mouse-exit delay. The theme has no native
Event, callback or visibility producer. Descendant Tooltip owns its onTriggered.
TooltipVisibility remains a separate nearest visibility scope; false removes
RawTooltip and its message annotation while leaving anchor child semantics.

## State, wrapping and save integrity

Prefer below, Exclude from semantics and Enable feedback are **three nullable
boolean State consumers**, not controlled State producers. Generated field
references belong inside TooltipThemeData. Bind/unbind, rename, Reset, Undo,
Redo, save and reopen retain child IDs and user-owned Dart members.

Creation uses the shared atomic required-child wrapping flow: palette/tree/
Canvas wrapping keeps the existing child and never inserts an invalid empty
TooltipTheme. Required-slot destinations are not ordinary empty drop targets.
The shared Designer ParentData policy still requires Expanded/Flexible/Spacer
to have a direct model Row/Column parent; this is a Designer restriction, not a
claim that Flutter forbids transparent inherited widgets in that relationship.

Whole Data and local references use the existing exact generated symbol/type
proofs. The synthesized TooltipThemeData is an ordinary Material compound
constructor occurrence. Only generated literal Duration probes in the exact
`tooltip-theme-core-duration:<wait|show|exit>DurationUs` namespace may use the
existing trusted SDK `core/duration.dart` paths; there is no broad dart:core trust
exception. Wrong/dynamic/nullable references fail closed. Source and FD save
authority remains bound to the immutable analyzed candidate and live baseline.

## Canvas and inventory

Canvas uses native TooltipTheme with fresh local data, inherited placement/
styling/timing and explicit reference-fallback diagnostics. Unknown whole Data
previews as an empty nearest theme rather than leaking outer data into it.
Preview safety does not rewrite stored/source values or execute user code.

Inventory at the TooltipTheme milestone (current: [MenuItemButton](MENU_ITEM_BUTTON.md)):
**100 widgets, 4,561 properties** (4,543 outside Scaffold),
**92 const-capable definitions**, and **158 State consumers across 53 types**.
Callables remain 155 across 30 types: 133 Events across 27 types, 17 builders,
two predicates, two formatters and one delegate. Controlled families remain 11.
FD 16, Catalog API 15, Canvas model 19 and transport protocol 1 are unchanged.

Ordinary insertion matrix: 100 sources × 89 destinations = **8,900 candidates**,
**8,447 accepted / 453 rejected**. Required-child wrapping is tested separately.

## Verification

Measured on 2026-09-09:

- Full Designer core: **2,138 tests across 256 suites**, all passed with zero
  failures, errors or skips. This includes the pinned SDK constructor/callable
  checks, all-field/codec/State contracts, and the 8,900-candidate drop matrix.
- Actual SDK candidate gate: **4 Java tests / 43 candidates**, all passed.
  Nine complete local/default/whole/import/paint cases, six reference access
  combinations, 27 rejected wrong/dynamic/nullable references across nine
  families, and one 42-widget numeric/mode/null matrix retain exact immutable
  source, FD and generated symbol/type evidence.
- Generated-app State gate: **1 Java / 6 Flutter tests**, all passed, with
  **two real analyzer/pair-save transitions**. Verifies all 15 native data
  fields, fresh Exit duration, nearest empty/null replacement, explicit Tooltip
  precedence, three live State consumers, open overlay identity, semantics,
  native callback ownership and exact hover/exit timing. Save/reopen, reset,
  disconnect, Undo and rejected whole/local/State conflicts retain user code.
- Exact evidence gate: **68 tests**, all passed. New regressions reject forged
  duration IDs/spans/libraries/types/SDK targets, stale post-CAS targets and
  a Widgets-library witness for the Material-only TooltipThemeData type.
- Canvas: **21 focused / 2,027 full Flutter tests**, all passed; analyzer clean.
  Includes complete local compounds, all reference fallbacks, nearest data,
  three trigger modes, implicit IconButton/FAB tooltips, TooltipVisibility,
  unsafe inherited geometry, baseline/intrinsic parity, focus, overlay State,
  structural lifecycle and 49-point drop geometry.
- Offline Web release passed. All **40 source + 35 Web manifest records** were
  checked for exact bytes, SHA-256 and duplicate paths. `main.dart.js`:
  **3,434,355 bytes**, SHA-256
  `0ff115708c5dd09191eb4eec984da5e042b6918115849ab0a6ac45bcdeebd5db`.

- Final coordinated Maven `verify`: **1,467 plugin tests across 68 suites**,
  **40 analyzer tests** and **9 Failsafe integration tests**, all passed with
  zero failures, errors or skips. Includes all 18 new UI/workflow regressions,
  existing boolean/property-sheet/State behavior, root/tree/slot wrapping,
  save/reopen history, SVG registry and source/Web artifact contracts. This is
  the selected complete milestone regression set, not every unrelated test.

- Reactor `install` and both root/module `nbm:cluster` commands passed.
  All **1,698 non-manifest module payload entries** match the built JAR, NBM
  and both development clusters. Designer, analyzer and Canvas dependency JARs
  match by SHA-256. The TooltipTheme schema class, FD 16 schema and all four
  TooltipTheme SVG variants are present.

Verified package at this milestone (later builds replace the same output path):
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
— **8,361,470 bytes**, built 2026-09-09 08:25:35 UTC, SHA-256
`bfa2a33d89e3163c6761328fdb6ef34d0867ff317ab4a7d6523182348a73ed88`.

The user's application and running IDE have not been modified or restarted.
Physical desktop acceptance is not claimed by these automated test results.
