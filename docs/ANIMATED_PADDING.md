# AnimatedPadding

Complete pinned Flutter 3.44.8 constructor:
`AnimatedPadding({Key? key, required EdgeInsetsGeometry padding, Widget? child,
Curve curve = Curves.linear, required Duration duration, VoidCallback? onEnd})`.

Reviewed against `widgets/implicit_animations.dart` and the
[official constructor](https://api.flutter.dev/flutter/widgets/AnimatedPadding/AnimatedPadding.html).
This constructor is **not const**. The shared identity mechanism manages Key;
all remaining arguments are exposed as four typed scalar rows, optional Child,
and the native On end Event.

## Values and editing

- Padding is required and nonnegative. The structured editor covers independent
  physical left/top/right/bottom and directional start/top/end/bottom values.
  Directional values resolve through the nearest Directionality, including RTL.
  The nonnullable editor exposes local value or project reference, never unset
  or null. Initial 16 pixels per side are a Designer creation preset, not a
  Flutter default.
- Typed EdgeInsetsGeometry references, members/getters and zero-argument factories
  support mixed geometry and application-owned implementations. Static source
  analysis checks the exact expected type; the application remains responsible
  for nonnegative runtime values.
- Curve exposes all 43 pinned Curves presets or a typed Curve reference/factory.
  Omission preserves native linear. Duration exposes portable nonnegative integer
  microseconds through 9007199254740991 or a typed Duration reference/factory.
  Creation uses 300000 microseconds (300 ms); zero completes immediately.
- On end supports optional VoidCallback reference/member/factory, explicit null
  and shared Events handler creation/selection/navigation. Initial mount and an
  unchanged padding target do not emit completion. An interrupted transition
  completes only on reaching its replacement target.
- Child is an optional box slot with insertion, replacement, removal, movement
  and nested wrapping. Slivers and incompatible direct parent-data widgets are
  rejected. AnimatedPadding can be a document root or ordinary box child.

Required argument removal is rejected. Optional Restore Default removes the
argument rather than inventing an explicit value. Property rows retain their
identity during refresh and reopening restores the exact values. FD codec,
Dart imports and static symbol proofs, guarded user code, Save/reopen and
Undo/Redo are tested. The generator-owned Duration probe has its own exact
occurrence ID; no broadening of the evidence trust boundary is required.

No FD, Catalog API, Canvas model or transport version change: existing typed
insets, references and constructor contracts are reused.

## Native Canvas and limitations

Canvas mounts real AnimatedPadding with Flutter's EdgeInsetsGeometryTween.
Physical/directional and mixed interpolation, RTL, intermediate geometry,
overshooting curves and nonnegative clamping follow the pinned implementation.
Child identity is preserved through changes. Empty and zero-padding nodes keep
selection and Child drop targets.

Project-owned EdgeInsetsGeometry, Curve, Duration and On end are never executed
in isolated Canvas. An explicit limitation tooltip identifies the custom
arguments and explains the fallback: 16 pixels per side, linear curve, 300 ms,
and no completion callback. Generated Dart retains exact application bindings;
run the application to validate custom code, callbacks and object validity.

## Verification

Verified on 2026-09-13 using pinned Flutter 3.44.8:

- Flutter analyze reports no issues.
- All 325 dedicated Canvas tests pass, including independent sides, loose/tight
  constraints, RTL, empty Child, all 43 curves at three durations, mixed
  interpolation, overshoot clamping, child identity and inert reference labels.
- All 6065 Flutter regression tests pass.
- Full flutter-designer suite: 2394 cases, zero failures/errors, 20 conditional
  skips. Java and independent Canvas schemas agree exactly.
- The explicitly enabled real-SDK gate passes: eight generated forms, 14 native
  Flutter cases, original-source static proof, wrong EdgeInsetsGeometry type
  rejection, Save/reopen, preserved user code, Undo/Redo and callback lifecycle.
- Release Web compilation passes with offline-safe build flags. Source and Web
  manifests and their artifact contracts pass.
- Combined fresh Java core and focused NetBeans/integration reports: 3695 cases
  across 473 suites, zero failures/errors, 62 conditional skips. The
  AnimatedPadding real-SDK gate was explicitly enabled and passed without skips.
  This is not an unfiltered full-reactor claim: the previously known unrelated
  Windows native Canvas pixel-metric test remains outside this slice.

Maven install and nbm:cluster pass. Byte verification confirms current compiled
schema/property/evidence classes, four SVG variants, 40 packaged runner sources,
35 Web artifact files, and all four development-cluster JARs matching the NBM.
The user's IDE was not restarted.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
Size: 8852668 bytes; modified UTC: `2026-09-13T15:36:50.1551359Z`.
SHA-256: `4bbf4e318fb3b6441daf44a6646c79b9162de2c88ba2fa3b27d5f261b80cefad`.

Inventory: 177 definitions (169 typed scalar and eight structural), 6979 writable
rows (6961 outside Scaffold), 146 const-capable definitions and 636 boolean rows.
The placement matrix has 168 ordinary destinations (149 AnyWidget and 19
constrained), 29736 candidates: 19584 accepted and 10152 rejected.
Native Events: 159 rows across 45 types; all callables: 218 across 69 types.
Next planned slice: AnimatedSlide.
