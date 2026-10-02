# Chip

Pinned API: Flutter 3.44.8, `packages/flutter/lib/src/material/chip.dart`;
[official constructor](https://api.flutter.dev/flutter/material/Chip/Chip.html).
Definition: `flutter.material.Chip`, Material palette, insert prototype rather
than wrapping an existing child. FD 17 / Catalog API 16 / Canvas model 20 /
transport 1 are unchanged: this is an additive reviewed catalog contract.

## Native surface

All 27 constructor arguments are represented: `key`, `avatar`, `label`,
`labelStyle`, `labelPadding`, `deleteIcon`, `onDeleted`, `deleteIconColor`,
`deleteButtonTooltipMessage`, `side`, `shape`, `clipBehavior`, `focusNode`,
`autofocus`, `color`, `backgroundColor`, `padding`, `visualDensity`,
`materialTapTargetSize`, `elevation`, `shadowColor`, `surfaceTintColor`,
`iconTheme`, `avatarBoxConstraints`, `deleteIconBoxConstraints`,
`chipAnimationStyle`, `mouseCursor`.

Label is a required single box slot, seeded with an editable Text('Chip').
Avatar and Delete icon are optional single box slots. Slivers are rejected;
dropping onto an occupied slot does not silently overwrite its child. Removing
the required Label is rejected. Delete icon is retained in the model but the
SDK hides it until `onDeleted` is non-null. Creation uses deterministic child
IDs and no appearance overrides, preserving native theme defaults.

170 writable/resettable rows:

| Family | Rows |
| --- | ---: |
| Direct native properties (slots excluded) | 24 |
| Complete local TextStyle | 31 |
| Ten local OutlinedBorder shape families | 21 |
| Local BorderSide and state variants | 45 |
| State colors | 9 |
| State mouse cursors | 9 |
| Local VisualDensity axes | 2 |
| Complete local IconThemeData | 9 |
| Four nested AnimationStyle groups | 20 |

Whole typed objects and their local fields are exclusive. Editing one side
clears the opposing representation in one undoable command; unrelated fields
are retained. TextStyle foreground/color and background/backgroundColor are
exclusive. Shape-kind changes remove only inapplicable shape fields. Border
state edits enable the stateful form. Cursor state buckets require Default.
Boolean fields use the shared checkbox editor; omission and permitted explicit
null remain distinct. Nullable icon shadows preserve null, an empty list and
an omitted argument as three separate values without eager editor commits.

## Semantics and ownership

- Chip is informational: `onDeleted: VoidCallback?` is its only native Event.
  It does not have `onPressed` or `onSelected`. Deletion does not remove Chip;
  application state owns that decision. Managed handlers support creation,
  existing-source selection, rename/reset, undo/redo and paired save/reopen.
- `shape` requires **OutlinedBorder?**, not arbitrary ShapeBorder. `side`
  supports BorderSide? including stateful native implementations.
- State maps resolve Disabled, Error, Dragged, Pressed, Selected,
  ScrolledUnder, Hovered, Focused, then Default. Explicit null color and
  inherited side results stop the first-match search and preserve SDK fallback.
- Null delete tooltip uses MaterialLocalizations; an empty string suppresses
  the tooltip. FocusNode lifetime/disposal belongs to the application.
- Whole/null TextStyle and IconThemeData retain native ChipTheme/M2/M3
  fallback. Local partial VisualDensity uses zero for its omitted peer axis,
  not the theme's axis. IconTheme opacity is finite but intentionally not
  constrained to 0..1; Flutter clamps the effective opacity.
- Four AnimationStyle groups expose duration/reverseDuration and all 43
  curve presets, strict source expressions, explicit null and noAnimation.
  **Pinned SDK limitation:** controllers consume durations at initialization;
  ordinary widget updates do not reconfigure them, and supplied curves are
  ignored. Change Key to recreate state when changing durations. The Designer
  preserves all values in Dart, rather than pretending the SDK uses them.
  ChipAnimationStyle has a non-const constructor; generation respects this.

## Canvas and source proof

Canvas uses native Chip with Material layout, real local style/state data,
slots, selection and drop targets. Project references are not evaluated:
named diagnostics disclose affected fields and the preview uses SDK defaults.
`onDeleted` presence supplies an inert callback so the affordance is visible;
pointer actions are absorbed, focus is excluded, autofocus is false and all
four animation groups use noAnimation. Generated application code is unaffected.

All 50 source-capable property fields request exact analyzer type proof.
Five newly admitted nullable type spellings are closed and explicit; arbitrary
nullable grammar is not accepted. Material-only VisualDensity? and
ChipAnimationStyle? use the Material proof import even when edited alone.
Literal animation durations have exact generator-owned occurrence IDs and
must resolve to the trusted SDK's core/duration.dart. Forged IDs, spans,
symbols, types, roots, missing evidence and post-save target changes are rejected.

## Verification

Automated checks cover catalog/Java-runtime contract parity, all property
editors, atomic families, source preservation, Events/history, required slots,
palette insertion and complete placement matrices. Real-SDK checks cover
typed sources and rejection of Object/dynamic/wrong signatures, generated
local and null styles, all ten shapes, six platform themes, M2/M3, LTR/RTL,
delete behavior/tooltips and animation initialization. Platform-theme tests
are not claims of running native Canvas providers on those operating systems.
No manual interactive NetBeans validation is claimed.

The generated reviewed Canvas schema now exceeds the former 4 MiB per-file
extraction bound. Only `lib/src/canvas_model.dart` has an 8 MiB bound; all other
runner sources retain 4 MiB and the bundle retains its 32 MiB total limit.
Exact manifest sizes, SHA-256, safe paths and verified cache reuse are unchanged.
Tests reject larger lookalike paths, truncated content and over-budget models.

Verified on 2026-09-16 in the canonical G: checkout, JDK 25 and Flutter 3.44.8:

- Full Java reactor followed by targeted reruns of all changed contracts and
  the corrected source-bundle/UI gates: 6,360 current test results, 280 skipped
  by environment gates, zero failures/errors. Final `mvn install` succeeded.
- Dedicated real-SDK gate: 460 generated/native widget scenarios, all 50
  source-capable fields, 42 unsafe/wrong source cases rejected.
- Entire Canvas suite: 12,194 tests passed. `flutter analyze`: no issues.
- Release Web Canvas built from `lib/main_web.dart`; main.dart.js is 4,909,327
  bytes, SHA-256 `ac1d84683d09550ad07d8f5d8a51c700b8e833e92a9aea1973a601e1d4a38c2f`.
- Nine package metadata integration checks passed; `nbm:cluster` succeeded.
- NBM inspected against current classes/resources: 41 runner sources, all 35
  Web artifact entries, 92 recent icon variants, 28 animated previews/license,
  and all four development-cluster JARs matched. The four new Chip SVGs were
  also rendered and visually reviewed at 16/32 px for light/dark themes.
- Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
  9,451,862 bytes, SHA-256
  `14258f12da86dfd15942cdfa75c2571b16f79dfb3c4378e4a090bc93e9f417bd`.

Rechecked on 2026-09-19 after the separate Maven-coordinate edits: all Chip
catalog, property, command and real-SDK checks passed (460 scenarios, 50 typed
fields and 42 rejected sources), as did the selected evidence/editor/source-bundle
regressions and nine package-metadata checks. `mvn verify` succeeded. The rebuilt
`netbeans-plugin/target/netbeans-flutter-plugin-0.1.3.nbm` was 9,449,349 bytes,
SHA-256 `2a6bf88437aa7060dae43bea29cd8685d17022ee7f2e53371b0801b77cf8763f`.
This is a build receipt before the requested Java package-namespace migration;
subsequent namespace builds supersede this artifact hash, not the Chip API contract.

Current inventory: 249 definitions, 8,476 property rows (8,458 outside Scaffold),
205 const-capable definitions, 240 typed/property-bearing and nine structural
definitions. There are 223 optional-slot destinations: the complete 55,527-case
placement matrix accepts 36,078 and rejects 19,449. This is not a count of all
Flutter widgets or a verified remaining-work total. Next candidate: ActionChip.
