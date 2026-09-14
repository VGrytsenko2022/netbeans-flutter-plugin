# AnimatedSize

Pinned baseline: Flutter 3.44.8. The [official constructor](https://api.flutter.dev/flutter/widgets/AnimatedSize/AnimatedSize.html)
and pinned widgets/animated_size.dart and rendering/animated_size.dart were checked.

## Complete constructor

Six writable typed rows, optional single box Child and shared Key support.
The constructor is const-capable. Only Duration is required; creation uses
300000 microseconds. Width, height and vsync are not public constructor arguments.

| Argument | Designer representation |
| --- | --- |
| alignment | Physical/directional AlignmentGeometry or typed reference; default center; non-null |
| curve | All 43 pinned Curves presets or typed Curve reference; default linear |
| duration | Required nonnegative portable integer microseconds or typed Duration reference |
| reverseDuration | Omitted, null, nonnegative portable integer microseconds or typed Duration? reference |
| clipBehavior | none, hardEdge, antiAlias, antiAliasWithSaveLayer; default hardEdge |
| onEnd | Native Events, VoidCallback reference/factory or explicit null |
| child / key | Optional box slot / shared identity support |

Durations retain exact integer microseconds through FD storage, generated Dart
and Canvas. The upper portable bound is 9007199254740991. Reverse duration is
stored as reverseDurationUs but generated as reverseDuration. Independent
Duration constructor occurrences retain distinct analyzer proof IDs.

References support current/package symbols, members and zero-argument invocations.
Nullable Duration getter types are checked without dynamic coercion. Omitted
and explicit null reverse durations are distinct stored values. Draft editor
changes and Cancel do not publish commands; property rows retain identity.

## Native behavior

AnimatedSize changes its own layout size when its child changes size. Initial
mount does not animate. The child is laid out at its target size during animation;
Alignment positions it within the animated box. Alignment/clipping updates are
immediate. Directional alignment resolves using the surrounding text direction.
Finite coordinates outside [-1,1] remain valid.

Important pinned SDK detail: RenderAnimatedSize always starts ordinary size
transitions with controller.forward. Consequently both growing and shrinking
use duration in Flutter 3.44.8, even when a distinct reverseDuration is supplied.
The argument is fully editable and generated unchanged; Designer does not
invent alternate timing or silently discard it.

Tight parent constraints prevent size animation. Consecutive child size changes
are handled by the SDK's unstable state, tracking the child directly until it
stabilizes. Empty child uses the smallest permitted size. Parent constraints
bound curve overshoot using native RenderAnimatedSize behavior.

Clipping applies to visual overflow; Clip.none does not enlarge hit-test bounds.
onEnd runs on completed native transitions, not initial mount or unchanged size.
No fabricated tap/change event or user-code execution is added to Canvas.

## Canvas and verification

Canvas mounts real AnimatedSize and retains state on model updates. Local values,
RTL, all curves, clips and native size changes are exercised. Empty nodes remain
selectable and accept Child drops.

Project references are presence-only: custom alignment previews as center,
curve as linear, duration as 300 ms, reverseDuration as null and onEnd as absent.
A tooltip identifies the widget and unavailable fields. Generated Dart retains
the exact references.

Coverage includes complete constructor/domain/codec/projection/placement tests,
Child commands and undo/reopen, typed editor validation/cancellation/stable rows,
real-SDK exact source proof and generated forms mounted in both LTR and RTL.
Dedicated Canvas tests cover 43 curves, intermediate growth/shrink, native
unstable-child handling, tight constraints, clip/alignment/hit bounds and
reference isolation. Interactive IDE testing is separate and is not claimed
by the automated tests.

## Recorded verification (2026-09-13)

- Dedicated AnimatedSize Canvas suite: 66 passing tests, including selection
  after animation reaches zero and after growth restores nonzero bounds.
- Full Flutter runner suite: 7409 passing tests; analyzer: no issues.
- Full core plus selected NetBeans/analyzer/resource regressions: 3745 cases in
  493 fresh reports, 67 conditional skips, zero failures/errors (3678 executed).
  This is not a claim that every unfiltered repository Java test was run.
- Real SDK: four generated forms, each mounted in LTR and RTL; exact source
  proof, nullable getter, wrong-type rejection, save/reopen and history passed.
- Web release build, Maven install and nbm:cluster succeeded. Four icon variants,
  40 packaged runner sources and 35 local Web artifact entries were verified
  against their manifests. All four dev-cluster JARs match the NBM.
- NBM: 8885082 bytes, SHA-256
  `39585ecafac43088948cc6b560770a48568725d91abca1a9e1122ea33cf1ab7f`,
  built at 2026-09-13T18:01:25.6612633Z.
- main.dart.js: 3790983 bytes, SHA-256
  `b8d25235bd17c665808a035a1f2e93427cec19564651f8c27796a5de09bb717b`.
- Palette: 182 definitions, 174 typed-property definitions, 7016 writable rows,
  6998 outside Scaffold, 150 const-capable definitions. Layout: 47 items.
  Ordinary insertion matrix: 31486 candidates, 20989 accepted, 10497 rejected.
- IDE and user project were not restarted or modified; working checkout is G:.
