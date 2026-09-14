# MenuItemButton — complete constructor and interaction slice

Baseline: Flutter **3.44.8**, verified against installed `material/menu_anchor.dart`,
`widgets/shortcuts.dart` and `services/keyboard_key.g.dart`. References:
[MenuItemButton constructor](https://api.flutter.dev/flutter/material/MenuItemButton/MenuItemButton.html),
[SingleActivator constructor](https://api.flutter.dev/flutter/widgets/SingleActivator/SingleActivator.html),
[CharacterActivator constructor](https://api.flutter.dev/flutter/widgets/CharacterActivator/CharacterActivator.html).

## Scope and inventory

Material `flutter.material.MenuItemButton` admits all **17 constructor arguments**:
Designer-owned Key, **13 native scalar arguments**, and three **optional**
single-child slots: Child, Leading icon, Trailing icon. No TextButton variant,
onLongPress or invented required Child is added. The SDK permits an empty item,
although a useful application normally supplies a label.

There are **520 editable properties**: 14 direct properties (13 native scalars
plus required Designer Enabled), **498 sparse ButtonStyle leaves**, and **8 local
shortcut fields**. Palette creation is enabled; const capability is preserved.

Inventory at this milestone (see [MenuAnchor](MENU_ANCHOR.md) for current totals):
**101 widgets**, **5,081 properties** (**5,063 outside Scaffold**),
**93 const-capable types**. Callables: **160 across 31 types** — **136 Events across
28 types**, 19 builders, two predicates, two formatters, one delegate. State:
**162 consumers across 54 types**; the 11 controlled producer families are unchanged.

## Activation, Events and State

Enabled is a Designer selector, not a native argument. Disabled emits null
On pressed while retaining its callback. Enabled without a callback emits a
no-op. A State binding controls activation instead of the retained preview
literal. Reset cannot remove required Enabled.

The three Events are On pressed (`VoidCallback`, the default), On hover and
On focus change (`ValueChanged<bool>`). Create/reuse/rename/disconnect/navigation,
save/reopen and history use the shared typed infrastructure. Background and
foreground layer builders remain builders, not Events. User-owned bodies are
preserved, never regenerated.

Enabled, Autofocus, Request focus on hover and Close on activate are four boolean
State consumers. There is no State producer, onChanged or shortcut-modifier State.

Native behavior is retained:

- Activation closes the root menu when Close on activate is true, applies focus
  changes, then invokes On pressed from a post-frame callback.
- Hover callbacks may occur while activation is disabled.
- Autofocus is effective only while enabled.
- Semantics label overrides the entire item, including the three children.
- MenuAnchor/MenuBar ancestry can determine orientation instead of Overflow axis.
- Clip defaults to `Clip.none` even with a layer builder.

Nullable Focus node and States controller references require strict non-null
target types when supplied. Null/omission retain native ownership. Object and
factory lifecycle remain project-owned.

## Shortcuts are display hints, not registrations

Whole Shortcut accepts null or a strict `MenuSerializableShortcut` reference,
getter, member or zero-argument factory, including imports. It is exclusive with
all eight local fields, even when null.

- SingleActivator: Trigger, Control, Shift, Alt, Meta, Num lock, Include repeats.
  Trigger offers **432** SDK LogicalKeyboardKey constants. The 12 modifiers
  rejected by the SDK constructor are excluded. Exact IDs/services imports remain.
- CharacterActivator: Character, Control, Alt, Meta, Include repeats. Character
  is preserved exactly. Shift/Num lock are not native parameters for this branch.

Exactly one Trigger/Character anchor is required with local leaves. Omitted
modifiers use SDK defaults. Custom logical keys/serializable shortcut
implementations use the whole typed reference, not arbitrary local raw Dart.

Editor transitions are atomic: whole references clear local leaves; an anchor
clears the opposite branch. Character also clears Shift/Num lock, retaining
compatible modifiers. Reset anchor clears its compound; reset modifier affects
only that modifier. Modifiers without an anchor are rejected without inventing
a key. Raw model conflicts fail closed without deleting stored values.

**SDK boundary:** CharacterActivator itself accepts empty, multi-character and
Unicode strings, but native MenuItemButton hint serialization requires one
UTF-16 code unit. Empty strings, multi-character strings and astral characters
such as emoji therefore cannot mount as native hints. Source/model values remain
exact; isolated Canvas omits that hint with an explicit diagnostic, preserving
the actual button/children. Valid constructor/type analysis is not a guarantee
that every value can pass every runtime assertion.

**Shortcut only displays a hint.** Applications register commands separately
through Shortcuts/ShortcutRegistry. Designer does not silently install global
shortcuts. Native focused Enter/Space activation remains unchanged.

## Complete sparse styling

All 498 local fields reuse the reviewed ButtonStyle contract: common fields and
nine sparse WidgetState buckets, typography, colors/paints, physical/directional
geometry, borders, shapes, cursors and layer builders. Whole Style, including
null, is exclusive with every local style leaf.

Compound fallback uses **MenuButtonTheme plus MenuItemButton.defaultStyleOf**.
Whole typed ButtonStyle references support custom combinations/resolvers, shapes
and builders. Native Flutter merges WidgetStateProperty objects, not individual
resolved states: a supplied sparse scalar resolver returning null can therefore
reach the internal TextButtonTheme fallback. Generated code preserves this SDK
behavior; a direct native oracle test distinguishes it from the ordinary
no-local-style MenuButtonTheme fallback.

## Canvas and safety

Java projection and independent Dart parsing retain all 520 fields and three
slot identities. Logical leading/child/trailing drop zones support RTL.
Project callbacks, builders, controllers, factories and references are not
executed by isolated Canvas; unavailable previews are diagnosed explicitly.

Inside MenuItemButton, Designer uses pointer-down/deepest-child selection so
child text/icon selection does not win the gesture arena and swallow native
activation. Routing outside this widget is unchanged.

For native menu geometry that cannot fit safely (including an unbounded width
with a tight-flex native label), the scoped Canvas guard retains the subtree and model children
but withholds unsafe painting, pointer hits, child drop geometry and semantics,
with a concrete diagnostic. It does not rewrite generated dimensions or insert
placeholder children. Valid horizontal shrink-wrap and finite native style-width
constraints remain renderable. This is a preview limitation, not a source constraint.

This slice does not add MenuAnchor, MenuBar, SubmenuButton, ShortcutRegistry or
application routing to the palette; those are separate widgets/features.

FD **16**, Designer API **15**, Canvas model **19**, transport **1** are unchanged.
Pair-save still requires accepted exact SDK analyzer/type evidence and an
immutable baseline. Existing proof limits/trust boundaries are not relaxed.

## Verification

Coverage includes schema/catalog/commands, exact SDK key parity, real analyzer
candidates for all 432 keys and strict reference families, generated State/native
runtime, Properties/Events/editors/history/drop and independent native Canvas
tests. Package receipts are recorded after coordinated verification; this
section does not claim a manual installed-IDE test.

Measured on 2026-09-09 before final packaging:

- Complete Designer core: **2,156 tests / 259 suites**, no failures/errors/skips.
- Real SDK candidates: **5 Java tests / 48 candidates**, including six batches
  covering all 432 keys and 27 rejected wrong/dynamic/nullable reference cases.
  The dense/null method was rerun with both nullable hover/focus callbacks.
- Generated State/native runtime: **1 Java test**, **7 Flutter runtime tests**
  and two accepted analyzer/pair-save transitions, including a direct native
  style-resolution oracle, display-only shortcuts and actual MenuAnchor closure.
- Independent interaction QA: **7 Canvas tests**, including an open nested
  Tooltip across native wrapper transitions and preservation of focus/State/IDs.
- Coordinated selected plugin regression: **1,485 tests / 71 suites**, analyzer
  **40 tests / 2 suites**, and package integration **9 tests / 1 suite**, all
  without failures/errors/skips. This is the affected selected plugin set, not
  an assertion that every unrelated plugin test was run.
- Final full Canvas: **2,050 tests passed**; MenuItemButton-focused set **23**;
  Dart analyzer clean. Includes direct native horizontal shrink-wrap, vertical
  finite-style-width, RTL overflow/recovery and live overlay regressions.

## Final package receipt

After the final Canvas correction and Web rebuild, all **37 artifact/source-parity
tests plus 9 package integration tests** passed again. All **40 native source
entries and 35 Web entries** match their exact manifests. No later source drift
was found. Web main.dart.js is **3474633 bytes**, SHA-256
`7f2e6ab19e524e2d88308e8e2bb95f4a5f8e8a6f34afb56e19dfac170c1d90ab`.

`mvn install` and both root/module `nbm:cluster` targets passed. The final NBM is
`netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`, **8394887
bytes**, built **2026-09-09T09:18:01.4460853Z**, SHA-256
`06eb7e0dda5e5eab78e90b0ff3ddf08e1164f91b7824185e5b4f2d5fc5c7cd57`.

All **1702 non-manifest plugin JAR entries**, all three dependency JARs,
the four MenuItemButton SVGs, the MenuItemButton schema/key catalog classes and
FD16 schema were verified against the NBM and both G: development clusters.
The IDE was not restarted and no manual installed-IDE interaction is claimed.
