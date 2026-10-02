# AnimatedRotation

Pinned baseline: Flutter 3.44.8. Implemented in the canonical G: checkout.
The [native constructor](https://api.flutter.dev/flutter/widgets/AnimatedRotation/AnimatedRotation.html),
pinned widgets/implicit_animations.dart and widgets/transitions.dart were checked,
including inherited ImplicitlyAnimatedWidget parameters and MatrixTransition behavior.

## Constructor and editors

Six typed scalar rows, optional box Child and shared Key support cover the constructor.

| Argument | Designer representation |
| --- | --- |
| turns | Required finite signed integer/double revolutions or exact typed double reference/getter/factory; creation default 0 |
| alignment | Optional physical Alignment coordinates/presets or typed Alignment reference/getter/factory; omission preserves center |
| filterQuality | Optional none/low/medium/high or explicit null; omission and null are distinct saved states |
| curve | Optional one of all 43 pinned Curves presets or typed Curve reference/factory; omission preserves linear |
| duration | Required portable nonnegative integer microseconds in Duration, or typed Duration reference/factory; creation default 300000 |
| onEnd | Optional VoidCallback reference/factory or null, exposed through native Events |
| child | Optional single box Child; omitted, explicit empty and populated slots |
| key | Shared stable identity and existing Key support |

Turns are revolutions, not radians or degrees: 1 is 360 degrees and 0.25 is
90 degrees clockwise. Negative turns are counterclockwise, also in RTL.
Zero is identity, not invisible. Values are not clamped to 0..1.
Numeric literals must remain finite and Dart-representable; integer literals
use the portable ±9007199254740991 range. Very large finite values can overflow
native angle/matrix arithmetic; the preview containment below handles this.

Alignment is physical in LTR and RTL, permits finite coordinates outside [-1,1],
and rejects explicit null and AlignmentDirectional. This matches the native
Alignment parameter, not the broader AlignmentGeometry domain.
Its UI has only the physical coordinate basis. Generic/directional alignment
properties on other widgets retain their existing behavior.

Turns uses the shared finite-number/project-reference dialog with no null or
omission option for this required argument. Inactive local drafts start from
the widget's creation default (0 here, 1 for AnimatedScale) and do not publish
until commit. Invalid input, required resets, stable row identity and reopen
are covered by the Properties contract tests.

## Native interpolation and events

Canvas mounts the real AnimatedRotation/RotationTransition, preserving the native
scalar tween, all curves/overshoot, interrupted transitions and zero duration.
It does not normalize turns modulo one or substitute a shortest rotation path:

- 0 to 1 performs one full revolution even though final orientation matches.
- 0.95 to 1.05 advances by 0.1 revolution.
- 0.95 to 0.05 goes backward by 0.9 revolution, not forward by 0.1.
- 0 to -2 performs two counterclockwise revolutions.

Only Turns has a tween. Alignment/Filter quality updates are immediate and do
not independently start animation or fire On end.
The native filter is applied only while the animation is running; initial mount
and completed animation use a null filter without rewriting the stored setting.
Layout size and Child element/state identity are unchanged by paint rotation.

On end fires on completed transitions, including full revolutions, but not initial
mount, unchanged turns or alignment/filter-only edits. Interrupted transitions
complete at the replacement target. Canvas never invokes project callbacks;
the generated application retains the actual handler.

Native transformed hit testing is preserved. Ancestor hit bounds still constrain
rotated content. Empty zero-size nodes retain Designer selection and Child drop
handles; zero turns keeps the ordinary Child visible and selectable.

## Non-finite frame containment

A direct native test with extreme finite turns exposed a non-finite semantics
rectangle assertion. A narrowly scoped Canvas guard now reads the actual native
RenderTransform matrix at the paint/semantics/hit boundaries. It retains the
real SDK widget/tween and Child, but withholds unsafe paint, semantics and hits
for frames whose matrix contains non-finite values.

An explicit AnimatedRotation diagnostic identifies the widget and explains the
unavailable geometry, suggesting smaller Turns or alignment coordinates.
The guard does not normalize, clamp or replace the stored value, source argument,
target tween or Child. It clears automatically when the matrix becomes finite.
Tests cover huge signed turns, overflowing physical pivots, a temporarily
non-finite overshoot frame, recovery and retained native/Child identity.
This is preview containment, not a promise that the generated application can
render numerically invalid native geometry.

## Source, persistence and isolation

Catalog/properties, Events, palette creation, box/sliver-safe placement, Child
insertion/removal/replacement/move, nesting, FD codec, Save, reopen and undo/redo
use the shared pipeline. Const construction is supported.

The closed duration evidence ID is animated-rotation-duration. Double, Alignment,
Curve, Duration and VoidCallback references carry strict expected-type proofs.
Wrong types (including AlignmentGeometry for Alignment) are rejected while
preserving the analysis baseline. Current-library and package references,
getters, members and zero-argument factories retain their exact generated form.
No arbitrary Dart-expression escape hatch was introduced.

Project objects/getters/factories/callbacks are never executed in isolated Canvas.
Presence-only metadata is labeled with its preview limitation, using zero turns,
center, linear, 300 ms and no callback. Generated application code uses exact
project bindings. This preview is not execution of the user's custom behavior.

Formats remain FD 16, Catalog API 15, Canvas model 19 and transport 1.

## Inventory after admission

180 definitions: 172 with typed scalar Properties and eight structural definitions.
6,995 writable rows, 6,977 outside Scaffold; 149 const-capable definitions.
Layout has 45 entries. Native Events: 162 rows across 48 types.
All callables: 221 across 72 types, with 45 builders, three predicates,
two formatters and nine delegates unchanged.

Placement: 30,780 candidates, 20,421 accepted and 10,359 rejected across
171 ordinary destinations (152 AnyWidget and 19 constrained).
A box AnimatedRotation is not admitted directly in a sliver-only slot.

## Verification

- New Canvas test file: 472 tests, all passed; Dart analysis found no issues.
- Real pinned SDK: eight generated forms and 22 Flutter scenarios passed,
  including positive/negative strict type proof, preserved user source,
  paired save/reopen, undo/redo, exact turns, native filter lifecycle and callbacks.
- Full designer-core reactor tests passed.
- Four SVG light/dark 16/32 variants visually reviewed.
- Complete Canvas suite: 7,287 tests passed.
- Full designer-core reactor plus targeted NetBeans/catalog/editor/palette/Canvas/
  analyzer regression suite: 3,724 fresh Java cases in 485 reports, zero failures
  or errors, 65 conditional environment skips. The final AnimatedRotation real-SDK
  integration case explicitly ran and passed with zero skips.
  This is not an unfiltered whole-repository test claim; the unrelated previously
  observed JNA Windows pixel-metric assertion was not part of this focused gate.
- Web release build passed, and source/Web manifests were regenerated with strict
  byte count and SHA-256 verification.

## Artifact receipt

NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
in `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

- Bytes: 8,875,207.
- SHA-256: `ebb6da0667e4cac3450b53ca8c75d05001e9e9429a1a5c1e5c40c9b1952bfe8d`.
- Modified UTC: `2026-09-13T16:55:14.2213727Z`.
- Web main.dart.js: 3,785,432 bytes,
  SHA-256 `adf2bd8cf8e85a9eae99892f36c2d11b70a8fe0b4a3bba0a1c8d2ce1fb13559d`.
- Packaged schema, constraints, editor/evidence/catalog/generator classes,
  four SVG variants, 40 runner sources and 35 Web artifact files verified.
- All four development-cluster JARs byte-match the NBM.
- `mvn -pl netbeans-plugin -am -DskipTests install` and
  `mvn -pl netbeans-plugin nbm:cluster` passed after the tests.

No IDE was restarted; automated tests are not claimed as a manual Windows UI smoke.
CJK IME and Linux/macOS Canvas providers are unchanged.
Next palette slice: AnimatedContainer.
