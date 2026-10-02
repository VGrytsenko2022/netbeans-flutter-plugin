# AnimatedAlign

Complete pinned Flutter 3.44.8 constructor:
`const AnimatedAlign({Key? key, required AlignmentGeometry alignment,
Widget? child, double? heightFactor, double? widthFactor,
Curve curve = Curves.linear, required Duration duration, VoidCallback? onEnd})`.

Reviewed against `widgets/implicit_animations.dart` and the
[official constructor](https://api.flutter.dev/flutter/widgets/AnimatedAlign/AnimatedAlign.html).
The shared identity mechanism manages Key. All remaining arguments are exposed:
six typed scalar rows, optional Child and the native On end Event.

## Values and editors

- Alignment is required. The shared editor offers physical Alignment and
  direction-aware AlignmentDirectional, including finite coordinates outside
  [-1,1], or an analyzer-verified AlignmentGeometry reference/member/factory.
  Creation starts at physical center. The nonnullable editor exposes local
  value and project reference only, without invalid unset/null modes.
- Width factor and Height factor are independent optional nonnegative finite
  multipliers. Integer, double, explicit null and omitted states are distinct
  in FD/source; Restore Default removes an optional argument.
- Curve supports all 43 pinned Curves presets and a typed Curve reference.
  Omission preserves linear. Duration supports nonnegative portable integer
  microseconds up to 9007199254740991 or a typed Duration reference/factory;
  creation uses 300000 microseconds (300 ms), and zero completes immediately.
- On end supports optional VoidCallback reference/member/factory, explicit null,
  and shared Events handler creation/selection/navigation. Initial mount and an
  unchanged target do not emit completion. Interrupted transitions do not emit
  a premature completion event.
- Child is an optional box slot with insert, replace, remove, move and nested
  wrapping. Slivers and incompatible direct parent-data widgets are rejected.
  AnimatedAlign can itself be a document root or ordinary box child.

Codec round-trip, generated imports/symbol evidence, saved user code, Undo/Redo
and reopen are retained. Property rows keep their identity while editing.
A typed AlignmentGeometry creation fingerprint was added to the existing
capability contract; no new FD, Catalog API, Canvas model or transport version.

## Native Canvas and limitations

Canvas uses the real AnimatedAlign and its native alignment/factor tweens,
including directional interpolation, RTL and out-of-unit-square coordinates.
Child identity, intermediate/terminal animation geometry, empty/zero-factor
selection and Child drop targets are tested.

Project-owned AlignmentGeometry, Curve, Duration and On end are never executed
in Canvas. A visible limitation label explains the substitute values: center
alignment, linear curve, 300 ms and no completion callback. Generated Dart
retains the exact bindings; run the app to test user code and object validity.

Flutter 3.44.8 has a native factor-reset nuance: after a numeric width/height
factor has created a tween, changing that factor back to null or omission can
retain the previous tween value until the AnimatedAlign state is recreated.
FD, Properties and source do store the requested null/omission correctly.
The plugin deliberately preserves native behavior rather than remounting the
widget and discarding child state. Dedicated SDK and Canvas regressions pin
this behavior. Initial null/omitted factors use normal native sizing.

## Verification

Dedicated core tests cover all arguments, placement, nullability, numeric limits,
physical/directional literals, all 43 curves, typed-reference proof and commands.
Property tests exercise the nonnullable alignment/reference editor, independent
nullable factors and stable row identity after refresh/reopen.
The explicitly enabled real-SDK gate passes: 11 generated forms, 21 native
Flutter tests, original-source analyzer verification, wrong AlignmentGeometry
binding rejection, completion/interrupt behavior and the factor-reset nuance.
All 215 dedicated Canvas tests pass.

Verified on 2026-09-13 with pinned Flutter 3.44.8:

- Flutter analyze reports no issues.
- All 5740 Flutter regression tests pass, including the 215 new Canvas cases.
- Full flutter-designer suite: 2389 cases, zero failures/errors, 20 conditional
  skips. The alignment fingerprint and portable integer bounds agree exactly
  between Java catalog metadata and the independent Canvas schema.
- The release Web build passes with the repository's offline-safe build flags.
  Source and Web manifests are updated and verified by the artifact contracts.
- Combined fresh Java core and focused NetBeans/integration reports: 3686
  cases across 469 suites, zero failures/errors, 61 conditional skips. The
  AnimatedAlign real-SDK gate was explicitly enabled and passed without skips.
  This is not an unfiltered full-reactor claim: the previously known unrelated
  Windows native Canvas pixel-metric test remains outside this slice.

Maven install and nbm:cluster pass. Byte verification confirms the compiled
schema/property/evidence classes, four SVG variants, 40 packaged runner sources,
35 Web artifact files, and all four development-cluster JARs matching the NBM.
The user's IDE was not restarted.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
Size: 8851156 bytes; modified UTC: `2026-09-13T15:15:58.8830823Z`.
SHA-256: `75523104b60c94ed60d7e9f6ffa3fe23b63ddf9f800cc60f7ce0d86561557af7`.

Inventory: 176 definitions (168 typed scalar and eight structural), 6975
writable rows (6957 outside Scaffold), 146 const-capable definitions, 636 boolean
rows. The placement matrix has 167 ordinary destinations and 29392 candidates:
19309 accepted, 10083 rejected. Native Events: 158 rows across 44 types;
all callables: 217 rows across 68 types. Next planned slice: AnimatedPadding.
