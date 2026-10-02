# AnimatedSlide

Complete pinned Flutter 3.44.8 constructor:
`const AnimatedSlide({Key? key, Widget? child, required Offset offset,
Curve curve = Curves.linear, required Duration duration, VoidCallback? onEnd})`.

Reviewed against `widgets/implicit_animations.dart`, `widgets/transitions.dart`,
`rendering/proxy_box.dart` and the
[official constructor](https://api.flutter.dev/flutter/widgets/AnimatedSlide/AnimatedSlide.html).
The shared identity mechanism manages Key. All remaining arguments are exposed:
four typed scalar rows, optional Child and the native On end Event.

## Values and editing

- Offset is required: two finite signed coordinates or an analyzer-verified
  Offset reference, member/getter or zero-argument factory. The structured editor
  exposes local coordinates or project reference only, never unset/null.
  Creation uses Offset.zero. Negative coordinates and values outside [-1,1] are
  allowed; NaN, infinity, nonnumeric values and missing coordinates are rejected.
- Coordinates are fractions of the child's actual laid-out size, not pixels:
  Offset(0.5, -0.25) moves a 100 by 80 child 50 pixels right and 20 pixels up.
  Positive X means right in both LTR and RTL. AnimatedSlide has no textDirection
  parameter and does not mirror Offset automatically.
- Curve supports all 43 pinned Curves presets or a typed Curve reference/factory.
  Omission preserves linear. Overshooting curves are not clamped.
- Duration supports nonnegative portable integer microseconds through
  9007199254740991 or a typed Duration reference/factory. Creation uses 300000
  microseconds (300 ms); zero completes immediately.
- On end supports optional VoidCallback reference/member/factory, explicit null
  and shared Events handler creation/selection/navigation. Initial mount and
  unchanged targets do not emit completion. Interrupted transitions complete at
  the replacement target, not prematurely.
- Child is an optional box slot with insertion, replacement, removal, movement
  and nested wrapping. Slivers and incompatible direct parent-data widgets are
  rejected. AnimatedSlide may be a document root or an ordinary box child.

Required argument removal is rejected; optional Restore Default omits its
argument. Property rows preserve their identity during refresh and restore the
same values after reopening. Codec round-trip, exact Dart imports/type proof,
guarded user code, Save/reopen and Undo/Redo are retained.

The shared Offset/reference editor now also accepts nonnullable unions. Existing
nullable MenuAnchor/SubmenuButton bindings retain their omit/null choices.
An exact Offset creation fingerprint was added to the existing capability
contract. The generator-owned Duration probe uses its own closed occurrence ID.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Native Canvas, hit testing and limitations

Canvas mounts native AnimatedSlide/SlideTransition and its Offset tween.
Translation affects paint, hit testing and semantics, not the size reserved by
layout. A child's changed size immediately changes the pixel displacement for
the same fractional Offset. Child identity and animation state survive edits.

Native SlideTransition uses transformHitTests=true and null textDirection.
RenderFractionalTranslation tests the transformed child without checking its
own untransformed rectangle. Content outside that rectangle can still respond
if its ancestors admit the point; a bounded ancestor can prevent the hit.
The real-SDK tests cover both cases. Canvas tests cover selection at the
translated position and selection/drop on an empty zero-size Child slot.

Custom Offset, Curve, Duration and On end references are not executed in isolated
Canvas. A limitation tooltip identifies the custom arguments and explains the
fallback: zero offset, linear curve, 300 ms and no completion callback.
Generated source preserves exact project bindings. Run the application to
validate custom code, object validity, callbacks and ancestor hit/clip behavior.

## Verification

Verified on 2026-09-13 with pinned Flutter 3.44.8:

- Flutter analyze reports no issues.
- All 359 dedicated Canvas tests pass: signed fractional coordinates, independent
  axes, RTL, loose/tight constraints, empty Child, all 43 curves at three
  durations, intermediate and final geometry, overshoot, state/child identity,
  size-dependent displacement, transformed selection and inert custom values.
- All 6424 Flutter regression tests pass.
- Full flutter-designer suite: 2399 cases, zero failures/errors, 20 conditional
  skips. Java and independent Canvas schemas agree exactly, including the new
  Offset creation fingerprint.
- Release Web compilation passes with offline-safe flags; source and Web
  manifests and their artifact contracts pass.
- Combined fresh Java core and focused NetBeans/integration reports: 3704 cases
  across 477 suites, zero failures/errors, 63 conditional skips. The
  AnimatedSlide real-SDK gate was explicitly enabled and passed without skips.
  This is not an unfiltered full-reactor claim: the previously known unrelated
  Windows native Canvas pixel-metric test remains outside this slice.
- The explicitly enabled real-SDK gate passes: eight generated forms, 15 native
  Flutter tests, exact original-source analyzer evidence, wrong Offset type
  rejection, Save/reopen, user-code preservation, Undo/Redo, callback lifecycle
  and transformed hits with and without limiting ancestor bounds.

Maven install and nbm:cluster pass. Byte verification confirms the current
schema/property/evidence classes, four SVG variants, 40 packaged runner sources,
35 Web artifact files and all four development-cluster JARs matching the NBM.
The user's IDE was not restarted.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
Size: 8854531 bytes; modified UTC: `2026-09-13T15:58:49.7150089Z`.
SHA-256: `2274c750d513ce38dd1128f71a46bc2642b1d97e5d032a6a8faff95f559fc8eb`.

Inventory: 178 definitions (170 typed scalar and eight structural), 6983 writable
rows (6965 outside Scaffold), 147 const-capable definitions and 636 boolean rows.
Placement: 169 ordinary destinations (150 AnyWidget and 19 constrained), 30082
candidates: 19861 accepted and 10221 rejected. Native Events: 160 rows across
46 types; all callables: 219 across 70 types. Next planned slice: AnimatedScale.
