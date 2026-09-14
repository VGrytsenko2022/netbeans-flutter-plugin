# AnimatedDefaultTextStyle

Pinned contract: Flutter 3.44.8. Sources:
[constructor](https://api.flutter.dev/flutter/widgets/AnimatedDefaultTextStyle/AnimatedDefaultTextStyle.html),
[TextStyle](https://api.flutter.dev/flutter/painting/TextStyle-class.html),
and the pinned SDK widgets/implicit_animations.dart and painting/text_style.dart.

## Complete constructor and local style

One const-capable box definition with 44 writable rows:
required Style and Duration, six paragraph arguments, 31 local style rows,
three local TextHeightBehavior rows, Curve and On end.
The 31 style rows represent all 26 TextStyle constructor arguments: locale and
decoration have structured component rows, plus an optional theme base.

Create by wrapping an existing box child, including the document root where
placement allows. Child is required: no invented text seed or empty removal.
It is not a sliver and does not inherit Stack ParentData through a wrapper.

Style selects local TextStyle fields or a strict TextStyle reference/getter/factory.
Local starts at const TextStyle(); it does not invent a merge with the outer
DefaultTextStyle. A descendant Text inherits it unless its own style overrides it.
Theme-only styles use a non-null empty-style fallback if the nullable theme
lookup is absent, preserving the required constructor contract.

Local fields include inherit, both colors, family/fallback/package, size, weight,
font style, letter/word spacing, baseline, height, leading distribution, locale,
foreground/background Paint, shadows, font features/variations, all decoration
components, debug label and overflow. Existing shared typed editors and limits
apply; arbitrary Dart text is not accepted. Package requires a family or fallback.

## Editing and source ownership

Selecting a whole style clears local style values in one undoable patch.
Editing a local field switches back to local. Color and Paint peers are mutually
exclusive and switch atomically. Whole TextHeightBehavior (including explicit
null) and its three local fields are also exclusive and switch atomically.
Property rows survive presentation refresh; Cancel leaves drafts uncommitted.

Max lines supports omitted, explicit null, a positive portable integer or int?
reference. Text height behavior supports omitted, null or TextHeightBehavior?
reference. The new nullable types are explicitly allowlisted and analyzer-proven.
Fractional/zero/negative local maxLines values are rejected, not coerced.

Duration supports nonnegative integer microseconds up to 9007199254740991 or a
Duration reference; creation is 300000 microseconds. Curve supports all 43 pinned
presets and a typed Curve reference. Native onEnd is configured through Events.
FD, source ownership, candidate proof, save/reopen and history use shared gates.
No schema/protocol version change: FD 16, Catalog API 15, Canvas 19, transport 1.

## Animation and preview boundaries

The SDK animates one TextStyleTween. Paragraph settings (alignment, wrapping,
overflow, maxLines, width basis and height behavior) update immediately.
On end is not called on initial mount or unchanged targets; zero duration
completes immediately, and interruption replaces the current target.

Canvas executes no project reference/factory/callback. Whole style previews as
empty TextStyle, custom height behavior and maxLines as null, custom curve as
linear and custom duration as 300 ms; tooltips identify these placeholders.
Generated application source retains exact typed values.

Flutter TextStyle.lerp cannot interpolate some differing-inherit styles or a
Paint against a missing matching color. Reviewed curves may also overshoot into
invalid font size. Canvas samples 101 points of the selected reviewed curve from
the currently displayed style before starting a transition. This is a preview
guard, not a general proof for arbitrary custom curves or application styles.
On a rejected transition it shows the target without that transition, retains
the child identity, and exposes a limitation tooltip. It does not rewrite saved
values or generated Dart, and does not make an invalid application transition
valid. Layout notifications refresh zero-sized selection targets as text changes.

## Verification

Core contracts cover schema, required-child commands, all curves, typed proof,
theme fallback, strict/null domains and codec round-trip. Swing contracts cover
44 writable rows, exact integer drafts, atomic source switching, stable rows
and reopen. Real SDK checks source analysis, wrong-type rejection, candidate
ownership, save/reopen/undo and generated forms mounted in LTR/RTL.

Dedicated Flutter tests cover native interpolation, paragraph timing, inherited
style, project isolation, incompatible transitions, retained children and On end.
An interactive IDE smoke run is separate and is not claimed here.

## Recorded verification (2026-09-13)

- Dedicated Canvas suite: 53 passing tests; full runner suite: 7629 passing tests.
  Flutter analyzer: no issues.
- Full core plus selected NetBeans/analyzer/resource regressions: 3764 cases
  in 501 fresh Surefire reports, 69 conditional skips, zero failures/errors
  (3695 executed). This is not every unfiltered repository Java test.
- Real SDK: five generated forms mounted in LTR and RTL; typed references,
  wrong-type rejection, exact candidate proof, save/reopen and undo passed.
- Catalog: 186 definitions, 178 typed-property definitions, 7086 writable rows,
  7068 outside Scaffold, 153 const-capable definitions; 51 Layout items.
  Events: 168 rows/54 types; all callables: 227 rows/78 types.
  Insertion matrix: 32178 candidates, 21146 accepted, 11032 rejected;
  33 required-child wrappers, 23 compatible with root wrapping.
- Web release build, Maven install and nbm:cluster succeeded. Four reviewed
  icon variants, 40 runner sources and 35 local Web files match their manifests.
  All four development-cluster JARs match the NBM.
- NBM: 8913687 bytes, SHA-256
  `c1a6d193f709607dc6cfcf8f5d078838b3cd6bfe9481b060dd0e170ebd72dc49`,
  built at 2026-09-13T19:22:55.7347635Z.
- main.dart.js: 3805375 bytes, SHA-256
  `f6cfa3e82762103ca8032a276cb70cb3879afab1eba5147f63983d3558843995`.
- Working checkout: G:. No IDE restart or user Flutter-project modification.

