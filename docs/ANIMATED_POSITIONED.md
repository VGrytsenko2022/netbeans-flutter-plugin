# AnimatedPositioned and AnimatedPositionedDirectional

Pinned baseline: Flutter 3.44.8. All three public constructor surfaces are admitted:
[AnimatedPositioned](https://api.flutter.dev/flutter/widgets/AnimatedPositioned/AnimatedPositioned.html),
[AnimatedPositioned.fromRect](https://api.flutter.dev/flutter/widgets/AnimatedPositioned/AnimatedPositioned.fromRect.html)
and [AnimatedPositionedDirectional](https://api.flutter.dev/flutter/widgets/AnimatedPositionedDirectional/AnimatedPositionedDirectional.html).
The pinned implicit_animations.dart, basic.dart and rendering/stack.dart were also checked.

## Complete constructor surfaces

Three Layout palette items with 9, 8 and 9 typed rows respectively. All have
required single box Child, shared Key, required Duration, Curve and native On end.
The ordinary and directional constructors are const-capable; fromRect is not.

| Argument | Designer representation |
| --- | --- |
| left, top, right, bottom, width, height | Ordinary constructor: omitted, explicit null, finite signed number or typed double? reference |
| start, top, end, bottom, width, height | Directional constructor: same domain, using native ambient Directionality |
| rect | fromRect: required local LTWH rectangle or exact typed Rect reference/getter/factory |
| rectLeft, rectTop, rectWidth, rectHeight | Four required finite signed local leaves; creation (0,0,48,48), emitted together as Rect.fromLTWH |
| duration | Required nonnegative portable integer microseconds (durationUs) or typed Duration reference; creation 300000 |
| curve | All 43 pinned Curves presets or typed Curve reference; default linear |
| onEnd | Optional nullable VoidCallback reference/factory, shared Events lifecycle |
| child / key | Required box child slot / shared widget identity |

Integer bounds are +/-9007199254740991; durations are nonnegative.
Rectangle leaves remain stored while a project Rect is selected; editing a leaf
atomically selects local. Local Rect edges and derived sizes must also stay
finite. No raw Dart expression or additional FD/protocol value kind was added.

Two non-null values per axis are allowed. Setting a third edge clears width
or height; setting a third size clears right/end or bottom. Each is one atomic
undoable property edit, retaining row identity. Imported malformed combinations
are rejected, not normalized. References count as potentially non-null; three
nullable project references are conservatively rejected even if user code might
return null at runtime.

## Placement and native behavior

Drag a palette item onto an existing direct child of Stack to wrap that child.
The required Child is never fabricated. Empty Stack insertion, root placement,
ordinary box parents, nested ParentData wrappers and sliver children are rejected.
The planner, model validator, Canvas resolver and generation use the same rule.

IndexedStack is deliberately not admitted as a parent: the pinned SDK inserts
Visibility/render wrappers between its children and RenderStack. A real-SDK
negative test verifies the resulting ParentData assertion. The Designer's
direct-parent rule is conservative: it does not attempt to prove arbitrary
StatelessWidget/StatefulWidget-only intermediate ancestry.

Native AnimatedPositioned controls actual Stack layout, not just painting.
All six tweens, start/end RTL resolution, overshooting curves and interruption
keep SDK behavior and existing child state. Signed negative widths/heights are
valid input; native Stack clamps the derived layout size to zero. Null removes
that tween immediately, and a new non-null value has no null starting point.

On end fires on completed transitions, including zero-duration changes, not on
initial mount or an unchanged target. Interrupted targets are not completions.
Project callbacks are never executed in the isolated Canvas.

## Canvas boundaries

Canvas mounts the actual SDK constructors outside the instrumentation render
wrappers, preserving Positioned -> RenderStack ParentData. Offset-only animation
refreshes zero-size selection handles, even when child layout size is unchanged.

When all children are positioned, Stack needs bounded parent dimensions.
Unbounded preview layout is contained with an explicit diagnostic; constrain
Stack (for example with SizedBox) or provide an ordinary sizing child.
The guard preserves native zero intrinsic extents and valid IntrinsicWidth /
IntrinsicHeight measurement. Stored data and generated Dart remain unchanged;
the guard does not make an invalid application layout valid.

Project values are presence-only: positions preview as null, a custom Rect as
(0,0,48,48), custom duration as 300 ms, custom curve as linear and On end as absent.
Tooltips state the unavailable fields. Generated source retains the exact typed
references and analyzer proof. Native source, not preview placeholders, defines
application runtime behavior.

## Verification

Contract tests cover every destination/axis combination, strict numbers and
nullable references, all curves, constructor const rules, FD round-trip, required
Child commands, atomic axis/rectangle edits, stable property rows and cancellation.
Real-SDK checks exercise exact candidate source, typed getters/factories,
wrong-type rejection, save/reopen/history and nine generated forms in LTR/RTL.

Dedicated Flutter tests cover parent data, all scalar tweens and curves, RTL,
signed sizes, null/interrupted transitions, native On end, zero-size selection,
live Canvas DnD, unbounded containment/recovery and intrinsic measurement.
Interactive IDE smoke testing is separate and is not claimed by these tests.

## Recorded verification (2026-09-13)

- Dedicated AnimatedPositioned Canvas suite: 167 passing tests.
- Full Flutter runner suite: 7576 passing tests; analyzer: no issues.
- Full core plus selected NetBeans/analyzer/resource regressions: 3756 cases
  in 497 fresh reports, 68 conditional skips, zero failures/errors (3688 executed).
  This is not a claim that every unfiltered repository Java test was run.
- Real SDK: nine generated forms, each mounted in LTR and RTL; exact candidate
  proof, typed getters/factories, wrong-type rejection, save/reopen and undo passed.
  An additional native test proves IndexedStack is not a valid ParentData parent.
- Palette: 185 definitions, 177 typed-property definitions, 7042 writable rows,
  7024 outside Scaffold, 152 const-capable definitions. Layout: 50 items.
  Ordinary insertion matrix: 32005 candidates, 20992 accepted, 11013 rejected.
  Required-child wrappers: 32; 22 also support root wrapping.
- Web release build, Maven install and nbm:cluster succeeded. Twelve icon
  variants, 40 packaged runner sources and 35 local Web artifact entries were
  verified against their manifests. All four dev-cluster JARs match the NBM.
- NBM: 8899189 bytes, SHA-256
  `3e58d2aa33a4738adb030fd7108ca7bdb6bddff6e596170932082e5deb2be010`,
  built at 2026-09-13T18:44:53.464312Z.
- main.dart.js: 3800402 bytes, SHA-256
  `ee1a99d694e97b4f45f30608938f781048faec271dedb639c88f471262cb3101`.
- IDE and user project were not restarted or modified; working checkout is G:.
