# Tooltip — native triggers, complete constructor and typed content

Baseline: Flutter **3.44.8**, checked against the installed
`material/tooltip.dart` and `widgets/raw_tooltip.dart` and the official
[Tooltip constructor](https://api.flutter.dev/flutter/material/Tooltip/Tooltip.html)
and [RawTooltip API](https://api.flutter.dev/flutter/widgets/RawTooltip-class.html).
The pinned implementation, not an inferred interaction model, governs behavior.

## Constructor, creation and content

Material `flutter.material.Tooltip` accounts for all 24 constructor arguments:
the Designer-owned key, **22 direct fields** and one optional **Child** slot.
The shared complete TextStyle projection adds 31 local fields, giving
**53 editable properties**. Creation stores the explicit message `Tooltip`;
it does not create a callback, artificial anchor or visible-state binding.

Ordinary palette Add permits an empty Child, matching the SDK's zero-sized
anchor. **Wrap with Tooltip** wraps an existing widget atomically and preserves
its identity. A missing/zero-sized anchor can still be selected in the host tree.

Exactly one of Message and Rich message must be non-null. Setting either
non-null content branch clears its peer in the same edit; clearing the last
active branch is rejected without changing the model/source/history. Empty
plain text is valid: the SDK returns Child directly and creates no tooltip
overlay, cursor wrapper or RawTooltip trigger machinery.

Rich message accepts a strictly typed non-null **InlineSpan** reference, getter,
member or zero-argument factory, including imported values. TextSpan trees,
WidgetSpan children, recognizers and custom InlineSpan implementations remain
user-owned Dart. There is **no local visual span-tree editor** in this slice;
the rich-message constructor branch is supported through typed source, not
through unvalidated raw Dart expressions. Explicit null is separate from a
nullable reference and is valid only while the other content branch is active.

## Appearance, geometry and timing

- Decoration supports the existing structured BoxDecoration editor, including
  its borders, radii, gradients, shadows and typed image assets. Strict
  Decoration references/factories support ShapeDecoration and custom
  implementations; these are not new local visual decoration constructors.
- Text style supports a whole strict TextStyle reference/null or all 31 shared
  local fields, including paints, shadows, locale, font features/variations and
  decorations. Whole/local branches are mutually exclusive; omission preserves
  theme defaults.
- Constraints supports structured BoxConstraints or a strict reference/null.
  The deprecated Height remains supported and is mutually exclusive with
  non-null Constraints. Padding and Margin retain physical/directional local
  insets, strict references and null.
- Wait, Show and Exit duration are exact signed portable integer microseconds
  (within ±9,007,199,254,740,991), strict Duration references/factories or null.
  No millisecond rounding or fake delay defaults are stored. Local literals
  require exact generator-owned symbol evidence for the SDK core/duration.dart
  constructor; other core symbols/IDs/files cannot acquire this permission.
- Position delegate is **Offset Function(TooltipPositionContext context)**,
  not a Widget builder or notification Event. Typed source references receive
  the SDK's resolved target, anchor size, overlay size, tooltip size,
  vertical offset and preferred direction. Canvas never executes this code.

## Events and State

`onTriggered()` is the one native Event, with the shared create/select/rename,
disconnect, save/reopen and Undo/Redo workflow. Omission/null removes only the
notification, not tooltip activation. Its body remains outside managed regions.

**Pinned SDK nuance:** tap and long press call onTriggered. Mouse hover does
not. Although the published callback prose also mentions programmatic
activation, Flutter 3.44.8's `ensureTooltipVisible()` does not call it. Manual
mode disables touch triggering, but mouse hover continues to show the tooltip.
Manual visibility belongs to the public runtime TooltipState, not a generated
Designer visibility controller or State producer.

The five reviewed boolean State consumers are Prefer below, Exclude from
semantics, Enable tap to dismiss, Enable feedback and Ignore pointer. They
consume existing typed fields without inventing a two-way visible property.
Only Enable tap to dismiss is non-nullable; the other flags retain explicit
null/theme behavior.

The SDK's default Ignore pointer is true for plain Message and false for Rich
message. Rich content's plain-text form provides the semantics tooltip unless
excluded. The actual tooltip overlay is transient UI, not a separate Designer
widget slot or persistent tree node.

## Canvas isolation

Canvas uses the real SDK Tooltip and RawTooltip behavior for admitted local
values. Project callbacks, span factories, positioning delegates and decoration
implementations are never executed. Unresolved rich content is visibly labeled
as unavailable preview content; default positioning or other substitutions
must be accompanied by diagnostics and must not rewrite source values.

Tooltip-specific selection and identity handling preserves the anchor's
editable state and native tap/long-press gesture competition. Selecting the
wrapper, child, ancestor or nothing does not remount an open tooltip. Warning
badges and selection markers do not participate in anchor baseline/intrinsic
geometry. The transient overlay has no Designer selection or drop targets.

Structural unwrap/move edits close affected transient previews rather than
reparenting an open SDK OverlayPortal during layout. Persistent widget IDs,
stored values and user source are retained; the tooltip can reopen at its new
anchor. Ordinary selection, diagnostic changes and Tooltip appearance edits
retain the live preview state. Ancestor property changes that alter the SDK's
internal wrapper structure use reviewed branch signatures, not arbitrary
property snapshots. Container wrapper changes, notification generic types,
clipper fallbacks, Badge/FAB SDK keys and ExpansionTile shape/zero-alpha
transitions close only affected previews. Ordinary RGB edits, stable warning
changes and unchanged wrapper branches preserve the anchor's State and text.
This is a transient Canvas lifecycle policy, not a modification of Flutter
constructor values or the generated application.

## Verification

At the Tooltip milestone, before TooltipVisibility: **98 widgets, 4,513
properties and 90 const-capable definitions**. Native Events total **133 across 27 widget types**; all callables
total **155 across 30 types** (133 Events, 17 builders, two predicates, two
formatters and one positioning delegate). State consumers total **154 across
51 types**, with 11 controlled families. FD 16, Catalog API 15, Canvas model 19
and transport protocol 1 are unchanged. For the next milestone and current
inventory, see [TooltipTheme](TOOLTIP_THEME.md).

Measured checks completed on 2026-09-09:

- Full Designer core: **2,106 tests**, no failures, errors or skips. This
  includes exact SDK signatures for all 155 callables and the existing
  generated-handler command contract.
- Real SDK candidate suite: **4 tests / 51 analyzed candidates**: 15 accepted
  and 36 intentionally rejected wrong/dynamic/nullable values across all
  12 direct reference families. Accepted cases include both local text color
  and Paint branches, imported/nested TextSpan and WidgetSpan content,
  ShapeDecoration, explicit nulls, all three reference access forms and a
  50-widget matrix of trigger modes, flags, optional children, signed exact
  duration boundaries and nonfinite geometry.
- Generated application State/Events runtime: **1 Java / 11 Flutter tests**,
  covering all three trigger modes on Android/iOS/Windows TargetPlatform
  variants, actual hover/touch/programmatic callback behavior, all five State
  consumers, WidgetSpan rendering, empty-message bypass and tap dismissal.
- Pair-save evidence gate: **63 tests**, all passed, including four new
  Tooltip Duration cases for both trusted core-library trees, exact manifest
  identity, rejected symbols/roots/kinds and post-apply target revalidation.
- Focused property/editor/wrap suites: **13 tests**, all passed. The new
  transactional wrap/content/Events/State save/reopen/Undo/Redo scenario passed
  within the broader plugin run. The renderer parity test also passed after
  updating its exact source anchor to the renamed SDK-child variable.
- Full Canvas Flutter suite: **1,987 tests**, all passed; Flutter analyzer
  reports no issues. The **50 Tooltip-focused tests** cover native triggers,
  semantics, timing, local styles, exact baseline/intrinsic/drop geometry,
  inline-edit and nested-anchor identity, warning transitions, pending timers,
  unwrap/move disposal and reviewed ancestor SDK topology transitions.
- Offline Web release completed. Both bundle manifests were regenerated and
  all **40 source + 35 Web entries** were independently rehashed. Packaged
  `main.dart.js` is **3,428,652 bytes**, SHA-256
  `1ace305bb2d40c6da3e07b86a931a554fecc9def71c8f0310365c1854861acaa`.
- Final coordinated Maven `verify`: **1,330 plugin tests**, **40 analyzer
  tests** and **9 Failsafe integration tests**, all passed with zero failures,
  errors or skips. This includes the source/Web artifact contracts and complete
  capability, property, Events/State and pair-save regressions selected for this
  milestone; it is not a claim of running every unrelated plugin test.
- Maven reactor `install` and both root/module `nbm:cluster` commands completed.
  The built module's **1,690 non-manifest payload entries** match the NBM and
  both development clusters. The Designer, analyzer and Canvas dependency JARs
  match by SHA-256; the Tooltip SVG and FD 16 schema/catalog classes are present.

Verified package at this milestone (later builds replace the same output path):
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter/netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
— **8,341,409 bytes**, built 2026-09-09 07:12:37 UTC, SHA-256
`784323417b75c74260b5cd2d988da3aabdc8602254cddd3a21b787f14ed570a6`.

No manual test in the installed IDE is claimed; the user's application and
running IDE have not been modified or restarted.
