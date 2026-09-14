# AnimatedScale

Pinned baseline: Flutter 3.44.8. Implemented in the canonical G: checkout.
This is a complete constructor slice, not a partial property placeholder.

## Constructor and editors

The native [AnimatedScale constructor](https://api.flutter.dev/flutter/widgets/AnimatedScale/AnimatedScale.html)
and pinned `widgets/implicit_animations.dart` were checked, including inherited
ImplicitlyAnimatedWidget arguments. Six typed scalar rows, optional box Child
and shared stable Key cover the constructor.

| Argument | Designer representation |
| --- | --- |
| scale | Required finite signed integer/double or exact typed double reference/getter/factory; creation default 1 |
| alignment | Optional physical Alignment coordinates/presets or exact typed Alignment reference/getter/factory; omission preserves center |
| filterQuality | Optional none/low/medium/high or explicit null; omission and null remain distinct saved states |
| curve | Optional one of all 43 pinned Curves presets or typed Curve reference/factory; omission preserves linear |
| duration | Required portable nonnegative integer microseconds in Duration, or typed Duration reference/factory; creation default 300000 |
| onEnd | Optional VoidCallback reference/factory or null, exposed through native Events |
| child | Optional single box Child; omitted, explicit empty and populated slots are supported |
| key | Shared stable widget identity and existing Key support |

Scale is not clamped to 0..1: zero, negative and greater-than-one targets are
valid. Numeric literals must remain representable as finite Dart doubles;
integer literals use the portable ±9007199254740991 range.
Negative scale flips both axes. Zero collapses paint and native child hit testing,
but does not discard the modeled Child or the parent's layout size.

Alignment accepts finite coordinates outside [-1,1]. It is physical in both
LTR and RTL. **AlignmentDirectional is not accepted**, because this constructor
takes Alignment rather than AlignmentGeometry. Java and the closed Canvas schema
enforce this difference; typed references require the exact Alignment type.
Existing generic/directional alignment editors remain supported elsewhere.

The Scale dialog contains only finite-number and project-reference sources:
no false null/omission choice for the required argument. Alignment has a
physical-only basis selector. Invalid local drafts do not publish into the cell;
required arguments reject Restore Default, and edits retain row identity.

## Native animation behavior

Canvas mounts the real AnimatedScale/ScaleTransition, not a custom approximation.
Layout is unchanged by paint scaling. Child element/state identity survives updates.
All 43 native curves, overshoot, zero duration and interrupted animations are retained.

Only Scale has a tween. Alignment and Filter quality update immediately;
changing them alone does not start a transition or call On end.
The pinned MatrixTransition applies configured FilterQuality only while its
animation is running; initial mount and completed animation use a null filter.
The stored value and generated constructor argument are not rewritten.

On end fires after completed scale transitions, not initial mount or an unchanged
target. An interrupted transition completes at the replacement target.
Native transformed hit testing is preserved, including negative and zero scales.
Ancestor bounds may still limit hits on enlarged/transformed content.

Empty zero-sized instances retain Designer selection and Child drop handles.
At zero scale with a nonempty Child, the AnimatedScale parent remains selectable;
the invisible native Child does not receive hits.

## Source, persistence and isolation

Creation, child insertion/replacement/removal/move, nesting, property reset,
FD codec, paired source analysis, Save, reopen and undo/redo use the shared pipeline.
Const construction is supported. Duration uses the closed
`animated-scale-duration` synthetic evidence ID; typed scale, Alignment, Curve,
Duration and callback probes retain strict expected types. Wrong-typed references
are rejected without modifying the analysis baseline.

Current-library and package references, getters, members and zero-argument factories
are retained in generated Dart. No raw arbitrary expression editor was introduced.

As elsewhere in the Designer, isolated Canvas never executes project objects,
getters, factories or callbacks. It carries presence-only metadata, labels the
preview limitation and uses scale 1, center alignment, linear curve, 300 ms and
no callback for custom bindings. Generated application code uses the exact bindings.
There is no silent claim that the custom project's animation is running in Canvas.

No FD/model/transport version bump: FD 16, Catalog API 15, Canvas model 19,
transport 1. The new physical-only Alignment constraint has an explicit canonical
fingerprint, distinct from AlignmentGeometry.

## Inventory after admission

179 built-in definitions: 171 with typed scalar Properties, eight structural.
6,989 writable scalar rows, 6,971 outside Scaffold; 148 const-capable definitions.
Layout now contains 44 entries. Native Events: 161 rows across 47 types.
All callables: 220 across 71 types; 45 builders, three predicates, two formatters
and nine delegates remain unchanged.

The placement matrix contains 30,430 candidates: 20,140 accepted and 10,290 rejected,
across 170 ordinary destinations (151 AnyWidget and 19 constrained).
A box AnimatedScale is not accepted directly in a sliver-only slot.

## Verification

- Dart analysis: no issues.
- New Canvas test file: 391 tests, all passed.
- Complete Canvas suite: 6,815 tests, all passed.
- Real Flutter 3.44.8 SDK: eight generated forms, 18 Flutter cases; strict positive
  and wrong-type negative analyzer evidence, guarded user-source preservation,
  reopen and undo/redo passed.
- Four light/dark 16/32 SVG variants visually reviewed.
- Full designer-core reactor plus targeted NetBeans/catalog/editor/palette/Canvas/
  analyzer regression suite: 3,714 fresh Java cases in 481 reports, zero failures
  or errors, 64 conditional environment skips. The final AnimatedScale real-SDK
  test explicitly ran and passed (one Java integration case, zero skips).
  This is not an unfiltered whole-repository test claim; the unrelated previously
  observed JNA Windows pixel-metric assertion was not part of this focused gate.
- Web release build passed; refreshed source/Web manifests retain strict byte
  counts and SHA-256 verification.

## Artifact receipt

NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
in `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

- Bytes: 8,863,217.
- SHA-256: `16506ce1f98b847ddfc75cf1f0272a74442fae1afb4989c056d10f20ca160d0a`.
- Modified UTC: `2026-09-13T16:21:51.3565289Z`.
- Web main.dart.js: 3,781,118 bytes,
  SHA-256 `48cb5f66fd251eb9d7a95a5e612b8fcb51e7a2c912839e59199ed30d6d7f2557`.
- Packaged schema, constraints, editor/evidence/catalog/generator classes,
  four SVG variants, 40 runner sources and 35 Web artifact files verified.
- All four development-cluster JARs byte-match the NBM.
- `mvn -pl netbeans-plugin -am -DskipTests install` and
  `mvn -pl netbeans-plugin nbm:cluster` passed after the tests.

No live IDE session was restarted or claimed as a manual Windows UI smoke.
CJK IME and Linux/macOS Canvas providers are unchanged.

Next palette slice: AnimatedRotation.
