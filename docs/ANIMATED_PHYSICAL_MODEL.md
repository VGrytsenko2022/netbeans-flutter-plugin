# AnimatedPhysicalModel

Pinned contract: Flutter 3.44.8; non-key public constructor, including inherited
Curve, Duration and On end.
[Official constructor](https://api.flutter.dev/flutter/widgets/AnimatedPhysicalModel/AnimatedPhysicalModel.html),
[class documentation](https://api.flutter.dev/flutter/widgets/AnimatedPhysicalModel-class.html).
SDK implementation: widgets/implicit_animations.dart, lines 2179–2304.

## Complete properties and placement

11 typed writable rows: shape, clipBehavior, borderRadius, elevation, color,
animateColor, shadowColor, animateShadowColor, curve, durationUs and onEnd.
Required Child is created by wrapping an existing box child; root wrapping is
supported where placement permits. No fabricated child is inserted. Removing
Child without replacement is blocked, and sliver/ParentData constraints remain
enforced by the shared placement rules.

Color and Shadow color are required and accept literal/theme colors or typed
Color references/getters/factories. Creation defaults are blue (0xFF2196F3) and
opaque black (0xFF000000). Both animation flags default to true and use the
shared centered checkbox editor. Required Duration starts at 300000 microseconds,
accepts nonnegative portable integers or a typed Duration reference, and cannot
be reset to omission. Curve exposes all 43 reviewed presets or a Curve reference.

Elevation is omitted/native zero, a finite nonnegative local number or a double
reference. Local infinity, NaN and negative values are rejected.
Border radius supports omission, explicit null, all four physical elliptical
corners or a BorderRadius? reference. No BorderRadiusDirectional is silently
accepted/resolved: this constructor expects physical BorderRadius.
The structured editor has distinct omission/null/local/project modes; drafts
remain uncommitted until OK. Nullable reference type proof is explicitly
allowlisted in core generation and the analyzer.

Shape supports rectangle/circle; Clip supports all four native modes. Circle
ignores but preserves the stored corner radii. Literal-only constructors remain
const-capable; theme/ref-driven values use normal source generation.

## Native animation and events

The SDK tracks four tweens: BorderRadius, elevation, fill color and shadow color.
Omitted/null radius is treated as zero and animates toward/from zero.
Shape and clipping change immediately. Color interpolation is independently
controlled by animateColor and animateShadowColor; false applies the target
color immediately but does not discard the corresponding native tween.
Turning a flag on mid-flight resumes that tracked tween.

On end is a nullable VoidCallback using the shared Events/source lifecycle.
No initial/unchanged-target completion is invented. Interruptions replace the
target, zero duration completes immediately, and color-only target changes can
still complete a tween when their visual animation is disabled.

## Canvas isolation and safety

Canvas executes no project references or handlers. It previews custom Color
as blue, custom Shadow color as black, custom Elevation as zero, custom radius
as null/zero, custom Curve as linear and custom Duration as 300 ms.
A tooltip identifies the unavailable fields; saved/generated Dart retains the
exact typed references and analyzer proof.

Some curves overshoot, so valid nonnegative endpoint elevations can produce a
negative intermediate elevation rejected by Flutter PhysicalModel. Before a
transition, Canvas samples 101 points of the selected reviewed curve, checking
elevation and corner finiteness. It tracks the current displayed geometry when
any tween target changes, and retains begin geometry for curve-only updates.
Zero-duration updates bypass this preflight and use the immediate native target.
Negative interpolated radii alone follow native behavior; no invented clamp is
applied to a legal SDK transition.

An unsafe sampled transition resets only the preview animation to the target,
retains the child, and shows a limitation tooltip. Stored values/source are
unchanged. This is a Canvas guard, not a proof for arbitrary custom curves or
a fix for unsafe transitions in the generated application.

## Verification scope

Core tests cover every shape/clip/flag pair, themes, physical radii, null,
required Child and properties, all curves, strict references, codec round-trip
and wrapping/replacement commands. Swing tests cover all rows, physical-only
radius drafts and rejected invalid input, mode changes, cancellation, checkbox
flags, stable property rows and reopen.

Real-SDK tests analyze exact candidate source, reject mismatched reference types
(including directional radii), preserve user code, save/reopen/undo, and mount
five generated forms in LTR/RTL. Canvas tests cover all four tweens, curves,
flags, immediate properties, interruption/completion, project isolation,
zero-size selection, actual drop wrapping and unsafe-transition recovery.

FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
Interactive IDE smoke is separate and is not claimed by automated tests.

## Recorded verification (2026-09-13)

- Dedicated Canvas suite: 71 passing tests; full runner suite: 7700 passing tests.
  Flutter analyzer: no issues.
- Full core plus selected NetBeans/analyzer/resource regressions: 3771 cases
  in 505 fresh Surefire reports, 70 conditional skips, zero failures/errors
  (3701 executed). This is not every unfiltered repository Java test.
- Real SDK: five generated forms mounted in LTR and RTL; typed references,
  wrong-type rejection (including directional radii), exact candidate proof,
  save/reopen and undo passed.
- Catalog: 187 definitions, 179 typed-property definitions, 7097 writable rows,
  7079 outside Scaffold, 154 const-capable definitions; 52 Layout items.
  Events: 169 rows/55 types; all callables: 228 rows/79 types.
  Insertion matrix: 32351 candidates, 21300 accepted, 11051 rejected;
  34 required-child wrappers, 24 compatible with root wrapping.
- Web release build, Maven install and nbm:cluster succeeded. Four reviewed
  icon variants, 40 runner sources and 35 local Web files match their manifests.
  All four development-cluster JARs match the NBM.
- NBM: 8924369 bytes, SHA-256
  `a435e76674da0f93190ac39117d880aaeacc9f53506765a4394e27027fea0bf4`,
  built at 2026-09-13T19:53:10.4886380Z.
- main.dart.js: 3809299 bytes, SHA-256
  `662b880cb5341b5ad8d076bd03bba06e12dafef4b53d090179b81e625eadc754`.
- Working checkout: G:. No IDE restart or user Flutter-project modification.
