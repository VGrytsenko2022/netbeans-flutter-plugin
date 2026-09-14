# AnimatedFractionallySizedBox

Pinned contract: Flutter 3.44.8; all non-key public constructor arguments.
[Official constructor](https://api.flutter.dev/flutter/widgets/AnimatedFractionallySizedBox/AnimatedFractionallySizedBox.html),
[class documentation](https://api.flutter.dev/flutter/widgets/AnimatedFractionallySizedBox-class.html).
SDK implementation: widgets/implicit_animations.dart, lines 2332–2435.

## Properties and placement

Six typed writable rows: alignment, widthFactor, heightFactor, curve, durationUs,
onEnd. Child is optional; empty insertion, removal, replacement, movement and
wrapping use the shared placement rules. Sliver and ParentData rules remain intact.

Alignment accepts physical or directional finite coordinates, including positions
outside the unit square, or a typed AlignmentGeometry reference/getter/factory.
Creation uses center; resetting to omission uses the native center default.
Explicit null alignment is rejected.

Each independent factor accepts omission, explicit null, a nonnegative finite
portable number, or a typed double? reference/getter/factory. Values above one
are allowed. Factors describe fractions of the incoming available space,
not multiples of the child's natural size. For an explicit factor, the native
layout needs a finite maximum constraint on that axis.

The numeric editor has four distinct modes: omission, null, local number and
project reference. Negative/non-finite numbers and raw Dart expressions are
rejected; edits stay in the draft until OK. Cancel and reopened property rows
retain their typed values.

Curve supports all 43 reviewed presets or a typed Curve reference. Duration is
required: nonnegative portable integer microseconds or typed Duration reference;
creation uses 300000 microseconds. On end is nullable VoidCallback and is exposed
in Events. Literal constructors remain const-capable; project references retain
analyzer proof for their exact Dart type. Child is not fabricated.

## Native behavior and Canvas boundaries

Canvas mounts the native AnimatedFractionallySizedBox and preserves child state.
Alignment and both non-null factors animate using the selected curve. RTL
resolution remains Flutter's responsibility. Initial mount and unchanged targets
do not invent completion events; interruption and zero duration follow the SDK.

In Flutter 3.44.8, changing a factor back to null/omission skips its tween visitor
without clearing the previous tween. The visible factor can remain, and a later
alignment change can replay that retained tween. A fresh mount starts with null.
Canvas follows this native behavior rather than silently changing generated Dart.

Project-owned values and callbacks never execute in Canvas. Referenced alignment
uses center, factors use null, curve uses linear, duration uses 300 ms and onEnd
is disconnected. Tooltips name unavailable fields; stored source retains them.

Canvas uses an eager layout observer so native intrinsic measurement remains
available to IntrinsicWidth/IntrinsicHeight parents. For unbounded axes or an
overflowing factor-times-extent, only that preview axis uses null with a tooltip.
A bounds-mode change resets the native animation shell, retaining the child.

Before a nonzero-duration transition, 101 samples of a reviewed curve check
nonnegative finite factors and resulting extents, including curve-only edits and
resizes. Unsafe preview transitions show their target immediately with a tooltip;
child identity is preserved. This sampling guard is not a mathematical proof for
arbitrary curves or a fix for invalid layouts in the generated application.

## Verification scope

Core: complete defaults/slots/placement, every curve, physical/directional
alignment, nullable factors, imported/current reference shapes, strict types,
codec round-trip and mutation commands.
Swing: six writable rows, four-mode numeric drafts, optional alignment,
curve choices, cancellation, stable rows and reopen.
Real SDK: six generated forms mounted LTR/RTL, exact candidate analysis,
reference mismatch rejection, source preservation, save/reopen and Undo.
Canvas: initial independent factor combinations, alignment and factor tweens,
child retention, native completion/null-replay, actual drop/zero-size selection,
overshoot containment, intrinsic parents and bounded/unbounded recovery.

FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
Interactive IDE smoke is separate and is not claimed by automated tests.

## Recorded verification (2026-09-13)

- Dedicated Canvas suite: 229 passing tests; full runner suite: 7929 passing tests.
  Flutter analyzer: no issues.
- Full core plus selected NetBeans/analyzer/resource regressions: 3781 cases
  in 509 fresh Surefire reports, 71 conditional skips, zero failures/errors
  (3710 executed). This is not every unfiltered repository Java test.
- Real SDK: six generated forms mounted in LTR and RTL, including empty Child,
  literals, typed nullable factor getters/factories, explicit null and omission.
  Exact candidate proof, wrong-type rejection, save/reopen and Undo passed.
- Catalog: 188 definitions, 180 typed-property definitions, 7103 writable rows,
  7085 outside Scaffold, 155 const-capable definitions; 53 Layout items.
  Events: 170 rows/56 types; all callables: 229 rows/80 types.
  Insertion matrix: 32712 candidates, 21589 accepted, 11123 rejected;
  174 destinations (155 ordinary any-widget destinations).
  Required-child wrappers remain 34, including 24 compatible with root wrapping.
- Web release build, Maven install and nbm:cluster succeeded. Four reviewed
  icon variants, 40 runner sources and 35 local Web files match their manifests.
  All four development-cluster JARs match the NBM.
- NBM: 8930818 bytes, SHA-256
  `0f1c92daef536cce272700c5aa1bcb6500ca6f1656b118b5252c15562ae10852`,
  built at 2026-09-13T20:21:23.9224061Z.
- main.dart.js: 3814161 bytes, SHA-256
  `a07d74e0066bbf04891659d05bdd86f95c856dd1e763e47d67e0c9ebb4ce985c`.
- Working checkout: G:. No IDE restart or user Flutter-project modification.
