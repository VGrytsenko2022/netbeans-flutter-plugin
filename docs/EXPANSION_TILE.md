# ExpansionTile — native expansion, complete properties and typed animation

Baseline: Flutter **3.44.8**, reviewed against the installed
`material/expansion_tile.dart`, `widgets/expansible.dart` and
`animation/animation_style.dart`. References:
[ExpansionTile constructor](https://api.flutter.dev/flutter/material/ExpansionTile/ExpansionTile.html)
and [ExpansibleController](https://api.flutter.dev/flutter/widgets/ExpansibleController-class.html).
The pinned SDK implementation governs runtime behavior, including differences
between fields accepted by AnimationStyle and fields actually used by the tile.

## Constructor and creation

Material palette order **290**, type `flutter.material.ExpansionTile`.
All 34 constructor arguments are accounted for: the Designer-owned key,
**28 non-slot SDK fields** and **five widget slots**. The const constructor
expands to **76 editable properties** through two density axes, two independent
21-field shape families and four AnimationStyle members.

| Surface | Count | Orders |
|---|---:|---|
| Title, Leading, Subtitle, Trailing, Children | 5 slots | 0–4 |
| Direct SDK arguments | 28 | 5–32 |
| VisualDensity axes | 2 | 33–34 |
| Expanded shape | 21 | 35–55 |
| Collapsed shape | 21 | 56–76 |
| AnimationStyle members | 4 | 77–80 |

Title is required; Leading, Subtitle and Trailing are optional single-widget
slots, and Children is an independent optional widget list. The existing
atomic required-slot creation workflow wraps the selected widget into Title.
It does not fabricate a Text title or turn the selected widget into expanded
content. Add expanded content to Children separately. An incomplete titleless
node cannot be inserted; removing or moving its required Title is rejected
atomically. This leaves the shared detached-prototype contract unchanged.

No callback or scalar default is synthesized at creation. Omission preserves
the SDK constructor default; explicit null remains separate wherever allowed.
ExpandedCrossAxisAlignment supports start/end/center/stretch and null; baseline
is rejected by the real constructor assertion.

## Properties and animation

Both shapes expose all ten existing local shape constructors, their detailed
borders and radii, or a strict ShapeBorder reference/factory/null. Each whole
shape is exclusive with its own local fields, without modifying its peer.
The same whole/local exclusivity applies to VisualDensity and AnimationStyle.

All seven colors, both physical/directional paddings and expanded alignment
retain typed local editors, project references/factories and explicit null.
Project references must satisfy source-bound non-dynamic type proofs; a
nullable variable is not a substitute for an explicitly chosen null value.

AnimationStyle supports a whole reference, explicit null, the noAnimation
preset, or all four local members:

- Duration and reverse duration: exact signed integer microseconds within the
  existing portable range ±9,007,199,254,740,991, strict Duration references or
  factories, or null. No millisecond rounding is introduced.
- Curve and reverse curve: all **43 pinned Curves presets**, strict Curve
  references/factories, or null. Custom curve implementation remains user-owned.

Local duration literals generate real dart:core Duration constructors. Their
generator-owned occurrence, original property path, exact SDK core/duration.dart
navigation target and prepared source manifest are verified before save.
Duration reference proofs use the core type even if project names shadow it;
they must not be checked against a fictitious Flutter-prefixed Duration type.

**Pinned SDK nuance:** ExpansionTile uses AnimationStyle.duration, curve and
reverseCurve but does not forward reverseDuration to Expansible. ReverseDuration
therefore remains editable, stored and generated without claiming an independent
collapse timing effect. Null members retain the individual SDK/theme fallbacks.

Signed or nonfinite constructor values retain the existing source-level
contract. Unsafe mounted geometry, negative effective animation duration and
unsafe predefined height curves are diagnosed in Canvas, not silently rewritten
in the document or generated Dart.
For these unsafe preview animations, only the affected duration or curve is
replaced locally with zero duration or a non-undershooting curve, accompanied
by a diagnostic. Ignored reverseDuration does not trigger that fallback.

## Events, State and controller ownership

The single native Event is `onExpansionChanged(bool isExpanded)`. It fires when
expansion changes, including changes initiated by a supplied controller.
Omission/null disables only notification; it does not disable expansion.
Handler bodies remain user-owned outside the managed source regions.

InitiallyExpanded is an **initialization seed**, not a controlled current value.
The SDK ignores it on ordinary widget updates. It is deliberately excluded from
State consumers, and ExpansionTile is not falsely registered as a two-way State
producer. Six reviewed boolean consumers are available: showTrailingIcon,
maintainState, dense, enableFeedback, enabled and internalAddSemanticForOnTap.
They can consume existing typed fields through DIRECT, NOT and EQUALS.

Controller accepts a strict ExpansibleController reference/factory or null; the
deprecated ExpansionTileController typedef remains compatible through Dart
analysis. The application owns supplied controllers and their disposal. A
factory can create a new controller on each rebuild; Designer does not invent
ownership or disposal code. WidgetStatesController represents header interaction
states, not the tile's expanded state.

Enabled false disables header activation, but does not prohibit an external
controller from expanding/collapsing or remove the expansion callback.
InitiallyExpanded true expands a supplied controller before the tile installs
its listener; an already-expanded controller can supply an initially open state
even when the seed is false. SDK PageStorage restoration remains authoritative.

## Canvas, children and accessibility

Canvas mounts the real SDK widget with one preview-owned public controller.
Ordinary property updates preserve local transient expansion. Explicit seed
edits may synchronize that isolated preview controller so the edited seed is
visible without remounting the tile; this is a Designer preview action, not a
claim that a runtime rebuild reapplies initiallyExpanded.
Changing selection between the tile, its Title, an ancestor and no selection
also preserves the preview controller, SDK state and editable child text. The
preview identity is document-owned and removed when the node is removed.

Project controllers, callbacks, factories and custom curves are never executed.
Unresolved project values receive explicit preview diagnostics; they do not
gain invented behavior or source values.

The real maintainState flag determines whether collapsed children are removed
after animation or retained Offstage with disabled tickers. Collapsed content
remains selectable in the host widget tree but has no Canvas geometry/F2/drop
target. It is not automatically revealed. Title-child clicks retain Designer
selection behavior; the SDK arrow or uncovered header area toggles expansion.

ControlAffinity defaults to trailing. Explicit Leading/Trailing may replace the
automatic arrow. ShowTrailingIcon false suppresses even a supplied Trailing
widget but not the leading-affinity arrow; hidden content remains stored.
The SDK controls platform-adaptive expansion semantics and keyboard activation.
Generated stable keys are not presented as user-configurable PageStorageKeys.

Current inventory: **97 widgets, 4,460 properties, 89 const definitions**;
**132 native Events across 26 types**, 153 callables across 29 types;
149 State consumers across 50 types and 11 controlled families.
FD schema 16, Catalog API 15, Canvas model 19 and transport protocol 1 are unchanged.

## Verification

Measured on 2026-09-09 against Flutter 3.44.8:

- Full Designer core: **2,089 tests**, no failures, errors or skips.
- Real SDK candidate suite: **5 tests / 84 analyzed candidates**: 21 accepted
  and 63 intentionally rejected wrong/dynamic/nullable references across all
  21 reference families. One accepted document contains 122 tiles covering all
  43 forward/reverse curve presets, optional-slot combinations, affinities,
  signed/null durations and nonfinite height values. Exact positive/negative
  portable integer duration boundaries are also covered.
- Generated application State/Events runtime: **1 Java test / 8 Flutter tests**,
  including Android/iOS variants, external/pre-expanded controllers, disabled
  header activation, callback counts, keyboard activation and maintainState's
  exact child-state retention/disposal behavior.
- Final selected Maven reactor verify: **40 analyzer tests, 1,308 plugin tests
  and 9 package integration tests**; no failures, errors or skips. This includes
  the 59-test pair-save evidence gate suite with four new closed Duration-proof
  tests, all property editors, Events/State save/reopen/Undo/Redo, required-slot
  mutation checks and source/Web artifact contracts.
- Canvas: **22 targeted / 1,937 full Flutter tests**, all passed; analyze reports
  no issues. Selection changes retain controller, SDK state and editable child
  text. Nested tiles, disposal, hidden content geometry, DnD, unsafe preview
  values and every predefined curve are covered.
- Offline Web release succeeded. All **40 source + 35 Web manifest entries**
  were independently rehashed. `main.dart.js`: **3,408,707 bytes**, SHA-256
  `fe98997f0600f6d9299d931a9230dd4bd8b3171f60e03ca1d8d6c6cda9419e7a`.
- Maven install and both root/module `nbm:cluster` runs succeeded. The NBM and
  both local clusters match all **1,683 module payload entries** and the exact
  Designer, analyzer and Canvas dependency artifacts, including the new palette
  SVG and ExpansionTile schema class.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
**8,313,353 bytes**, SHA-256
`8af6aa7f7ac107bb8e7c231d3f09463472c0d17f52a6ba48bd592a5cd4507565`.

All work and artifacts are in the canonical
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter` checkout. The IDE
was not restarted and the user's Flutter application was not changed. No manual
test in the installed IDE is claimed.
