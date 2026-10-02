# MatrixTransition

Pinned contract: Flutter 3.44.8, `widgets/transitions.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/MatrixTransition/MatrixTransition.html),
[class](https://api.flutter.dev/flutter/widgets/MatrixTransition-class.html) and
[TransformCallback](https://api.flutter.dev/flutter/widgets/TransformCallback.html).

## Complete native constructor

- Required Animation: signed finite local scalar or typed `Animation<double>`
  project reference, getter or zero-argument factory; creation value 0.
- Required On transform: structured 4 by 4 local matrix, or typed
  `TransformCallback = Matrix4 Function(double animationValue)`; creation identity.
- Alignment: optional physical `Alignment`, including finite coordinates outside
  [-1, 1], or a strict typed project source. Omission uses center. Explicit null
  and AlignmentDirectional/AlignmentGeometry sources are not accepted.
- Filter quality: omitted, explicit null, none, low, medium or high.
- Optional single box Child: empty creation/insertion, replacement, movement,
  removal and atomic wrapping (including root) use the shared commands.
  Sliver, Flex and Stack parent-data children retain shared placement restrictions.
- Shared Key/identity support is unchanged. Native parameter order is animation,
  onTransform, alignment, filterQuality, child. There is no Duration, Curve,
  onEnd, Origin, Transform hit tests or Clip behavior parameter.

Local Animation generates `AlwaysStoppedAnimation<double>`; it does not create
an animation controller. Local On transform generates a fresh
`Matrix4.fromList(...)` on every callback invocation, with column-major storage.
It is a **fixed matrix**, independent of animationValue. The editor labels this
distinction. Application-owned TransformCallback sources provide arbitrary
animation-dependent translation, scale, rotation, skew and 3D/perspective math;
they are not limited to a closed list of transform presets. Controllers, tweens,
units and lifecycle remain in application code. No raw Dart escape is introduced.

All eight supported source/reference/getter/factory forms are available for each
of the three source-capable fields. Required reset/null, non-finite matrix
entries, wrong arity, incompatible parameter/return types, nullable required
sources and dynamic-only evidence are rejected. A callback accepting a broader
num parameter is assignable to TransformCallback under Dart's function rules.

## Delegate editing and persistence

On transform is a computation **delegate in Properties**, not a native Event.
The regular local/source editor is retained inside the shared callback editor.
Create Handler, Bind, Navigate, Rename and Disconnect operate through the existing
analyzed source/model transactions. A new user-owned method returns
`Matrix4.identity()`; customize it in Source. Disconnect restores the local
identity callback while preserving the user's method body.

Local matrix editing displays row/column positions while storing column-major
values. Invalid drafts do not publish, switching modes preserves drafts, and
incremental refresh retains property-row identity. Required fields cannot become
unset; optional alignment/filter fields retain their proper reset semantics.
FD round trips and source save/reopen preserve typed references and local values.

## Native rendering and isolated Canvas

Generated Dart uses Flutter's native MatrixTransition. It transforms paint and
hit testing, not layout or sibling positions. Alignment is physical in both LTR
and RTL. There is no implicit clipping; painting outside bounds does not extend
the parent's hit-test bounds.

Filter quality is applied only while `animation.isAnimating`. A local
AlwaysStoppedAnimation reports forward status, so it takes the active branch.
Flutter's AnimatedWidget listens to value notifications, not status-only changes:
a status-only notification does not itself rebuild; the next value frame
recomputes filter selection.

Canvas renders the actual native widget without executing project sources:
a source animation previews as 0, a source callback as identity, and a source
alignment as center. The diagnostic names exactly the substituted fields; local
fields and exported source expressions remain intact. Each callback gets a fresh
matrix. Source fallback and recovery preserve native State and child identity.

The shared matrix preview guard withholds paint, hits and child semantics when
the effective matrix has non-finite entries. It keeps the values, generated Dart,
State and child, and restores preview when corrected. Finite singular matrices
and projective edge cases retain Flutter behavior; the guard does not claim to
make every application transform invertible or visible. Empty/zero-size widgets
remain externally selectable. This optional-child widget is not added to the
palette's required-child-wrapper classification.

## Verification and inventory

Coverage includes core generation/FD/placement contracts; matrix and callback
editors; real SDK analyzer admission and source preservation; native runtime
animation/callback replacement, physical 2D/3D transforms, filter statuses,
pointer/pixel behavior and optional-child lifecycle; and the complete Canvas
suite: 10,872 passing tests, including 157 MatrixTransition-specific cases.
The real SDK test passes 104 generated/native runtime scenarios after analyzer
admission, source-preserving create/rename/disconnect and invalid-source checks.
Java property tests also exercise the actual callback editor and revision-bound
operations bridge; other non-event callback types keep their existing admission
boundaries. Flutter analyze reports no issues. Canvas-specific tests exercise local matrices, constrained layout, both
text directions, source isolation, zero-size selection and unsafe-matrix recovery.

Inventory: **206 definitions**, **7,282 writable rows** (7,264 outside Scaffold),
198 typed-property definitions, 172 const-capable definitions and 66 Layout items.
Native Events remain 172 rows / 58 types. All callables are 235 / 84 types,
including 48 builders and 10 delegates. Formats remain FD 16, Catalog API 15,
Canvas model 19 and transport 1. This is not a claim that the complete Flutter
widget inventory is implemented.
