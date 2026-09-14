# AnimatedContainer

Pinned baseline: Flutter 3.44.8, canonical G: checkout.
The [official constructor](https://api.flutter.dev/flutter/widgets/AnimatedContainer/AnimatedContainer.html)
and pinned widgets/implicit_animations.dart were checked, including inherited arguments.

## Complete constructor

15 writable typed rows, optional single box Child and shared Key support.
The constructor is not const. Duration alone is required; creation uses 300000
microseconds and does not invent dimensions or decoration.

| Arguments | Designer representation |
| --- | --- |
| alignment, transformAlignment | Physical/directional AlignmentGeometry, explicit null, or typed AlignmentGeometry? reference |
| padding, margin | Nonnegative physical/directional EdgeInsetsGeometry, null, or typed EdgeInsetsGeometry? reference |
| color | Literal/theme Color, null, or typed Color? reference |
| decoration, foregroundDecoration | Complete shared BoxDecoration editor including border/radii/shadows/gradients/DecorationImage, null, or typed Decoration? reference |
| width, height | Nonnegative portable integer/finite double, positive infinity, null, or typed double? reference |
| constraints | Structured BoxConstraints including unbounded maxima, null, or typed BoxConstraints? reference |
| transform | Structured Matrix4, null, or typed Matrix4? reference |
| clipBehavior | none, hardEdge, antiAlias, antiAliasWithSaveLayer; no null |
| curve | All 43 pinned Curves or typed Curve reference; omission uses linear |
| duration | Required nonnegative portable integer microseconds or typed Duration reference |
| onEnd | Native Events handler, typed VoidCallback reference/factory or explicit null |
| child / key | Optional box slot / existing shared identity support |

There is no isAntiAlias constructor argument on AnimatedContainer. References
support current/package symbols, optional members and zero-argument invocations.
Nullable getter types are checked through the exact analyzer proof, not coerced
or weakened to dynamic. Arbitrary Decoration/ShapeDecoration is supported in Source
through this typed reference branch, not restricted to local BoxDecoration.

## Native dependencies and animation

Non-null color and decoration are exclusive; explicit null is not a conflict.
Color is converted by Flutter to BoxDecoration and is sufficient for clipping.
Foreground decoration alone is not sufficient. UI changes clear conflicting
background values atomically. Selecting a non-none clip with no background creates
an empty BoxDecoration; removing the last background resets clipping atomically.
Undo/redo retains the complete transaction and the property sheet keeps stable rows.

Width and height tighten BoxConstraints using Flutter's own clamping. A width
outside the configured min/max interval is not an invented validation error.
Infinity requires suitable finite constraints from the parent.

Native AnimatedContainer supplies eight tweens: alignment, padding, decoration,
foregroundDecoration, constraints, margin, transform and transformAlignment.
Color/dimensions feed decoration/constraints rather than separate tweens.
Clip behavior changes immediately. Initial/unchanged/clip-only updates do not
emit onEnd. Null removes the corresponding tween immediately; new non-null
properties do not interpolate from null. Interruptions start from the current
interpolated value. Child identity follows native Container topology: adding or
removing wrapper-producing nullable properties is not a guarantee of preserving
every descendant State.

Matrix4Tween decomposes and recomposes translation, rotation and scale; it is
not element-wise interpolation and does not promise retention of arbitrary
perspective/shear components. Native behavior is retained, not replaced.

## Canvas and boundaries

Canvas mounts real AnimatedContainer with local values, theme/asset resolution,
native RTL, animation, clipping and transformed hit testing. Optional empty
children retain creation/drop support. Project code is never executed:
custom geometry/color/decorations preview as null, custom duration as 300 ms,
curve as linear, callback as absent. A concrete tooltip names the widget and
unavailable fields; clipping falls back to none if its custom background is unavailable.
Stored values and generated source are not changed by these preview fallbacks.

A shared per-frame matrix guard with AnimatedRotation withholds unsafe paint,
semantics and pointer input for non-finite native matrices (including singular
Matrix4Tween decomposition). It keeps the native tween/state, labels the issue,
and recovers when the matrix becomes finite. Reset Transform to null before
setting a regular matrix if an invalid native tween endpoint persists.

Other native layout/interpolation preconditions still apply. In particular,
overshooting curves combined with extreme geometry may produce invalid
intermediate constraints/insets/decoration. Designer does not silently clamp
Dart or replace the requested curve. Custom Decoration painting/interpolation
is not executed in isolated Canvas.

## Verification coverage

Core constructor/domain/projection/codec/placement and command tests; NetBeans
typed custom editors, local-draft cancellation behavior, stable rows and atomic
background edits; real-SDK exact analysis and save/reopen/history tests including
ShapeDecoration, nullable getters, Matrix4 and infinity and rejection of dynamic/
wrong reference types. Generated forms are mounted in LTR and RTL.

Dedicated Canvas tests cover all eight native tweens for all 43 curves,
interruption, constructor arguments, null resets, events, reference isolation,
bounded infinity, optional child and non-finite matrix recovery.
Full Flutter regression and selected Java regressions cover shared editors,
palette, source proofs, packaged resources and byte-for-byte artifact freshness.
IDE is not restarted and live desktop interaction is not claimed by these tests.

## Recorded verification (2026-09-13)

- Dedicated AnimatedContainer Canvas suite: 56 passing tests.
- Complete Flutter suite: 7343 passing tests; analysis reported no issues.
- Final diagnostic wording: 528 AnimatedContainer/AnimatedRotation tests rerun and passed.
- Full designer-core reactor plus selected NetBeans/catalog/editor/palette/Canvas/
  analyzer regression: 3735 cases in 489 fresh reports, zero failures/errors,
  66 conditional environment skips. This is not an unfiltered whole-repository run.
- AnimatedContainer real-SDK gate explicitly passed without skipping: five generated
  forms, ten native LTR/RTL scenarios, exact typed analysis, negative dynamic/wrong
  type proof, preserved source, pair save/reopen and undo/redo.
- Four light/dark 16/32 SVGs visually reviewed.
- Web release build, Maven install and nbm:cluster passed.
- Packaged schema/constraint/editor/evidence classes, four SVGs, all 40 runner
  sources and 35 Web files were checked against current bytes and manifests.
  All four development-cluster JARs exactly match the NBM.
- NBM: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
  8881196 bytes, SHA-256
  `069f18036837326275bcc30aaefb0af445601ec99fc3f1a3f5996614157ba205`,
  UTC 2026-09-13T17:35:08.1324096Z.
- Web main.dart.js: 3789181 bytes, SHA-256
  `f3d49287bd02054db70447496d9c275e682a587f4eb4d61faa3686745aec02d9`.

## Current catalog

181 definitions: 173 with typed properties and eight structural definitions.
7010 writable rows (6992 outside Scaffold), 149 const-capable definitions.
Layout 46, Material 49, Scrolling 51. Placement: 31132 candidates,
20704 accepted and 10428 rejected; 172 ordinary destinations (153 any-widget,
19 constrained). 163 native event rows across 49 widget types; 222 callables
across 73 types, with 45 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
Next palette slice: AnimatedSize. CJK IME and Linux/macOS Canvas providers remain deferred.
