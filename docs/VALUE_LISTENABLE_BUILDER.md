# ValueListenableBuilder vertical slice

Pinned Flutter 3.44.8. Canonical checkout:
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native contract

[Constructor](https://api.flutter.dev/flutter/widgets/ValueListenableBuilder/ValueListenableBuilder.html):
const ValueListenableBuilder<T>({Key? key, required ValueListenable<T> valueListenable,
required ValueWidgetBuilder<T> builder, Widget? child}).

[ValueWidgetBuilder<T>](https://api.flutter.dev/flutter/widgets/ValueWidgetBuilder.html)
is Widget Function(BuildContext, T, Widget?). The callback receives the source's
actual value, including null when T permits it. The native widget reads the value
and subscribes at initialization, reads the replacement source immediately when
the source changes, removes the old subscription and unsubscribes on disposal.
It never disposes the application-owned source.

Unlike AnimatedBuilder, this source must implement ValueListenable<T>, not merely
Listenable. There is no native onChanged, onEnd, duration or curve argument.

## Designer surface and type changes

Two admitted placements use the same unnamed constructor:

- Layout order 300: flutter.widgets.ValueListenableBuilder (box Child/result).
- Scrolling order 500: flutter.widgets.ValueListenableBuilder.sliver (sliver Child/result).

The sliver suffix is a catalog projection, not an SDK constructor.
Both expose Value listenable, Builder, Value type and Nullable value type,
plus the optional single Child slot. Key retains the shared identity mechanism.
Only valueListenable, builder and child are emitted as constructor arguments;
the two type-control rows determine the explicit generic T.

Supported T identities: String, int, double, num, bool, Object, a simple current-file
or declared-package class/enum, or a closed project typedef (including complex
generic aliases). Nullable value type explicitly selects T?. Direct raw type
expressions and dynamic/void/Never/Null identities are not admitted. A typedef
hiding dynamic or nullability inconsistent with the selected flag is rejected by
the independent analyzer proof. This is a reviewed Designer type surface, not a
claim that Flutter prohibits other type arguments.

Value type opens an atomic four-field editor: type, nullability, source and
builder. OK applies one guarded PatchProperties command, Undo restores the whole
draft, Cancel publishes nothing, and a stale dialog cannot overwrite changed
dependent fields. Source/getter/member/zero-argument factory and builder references
retain their exact current-file or declared-package symbol identities. The owner
must provide stable intended instances; Designer does not allocate or dispose a
ValueNotifier or application controller.

Creation selects double, the Constant default source preset and Child builder.
The preset generates const AlwaysStoppedAnimation<T>(value): empty String, zero
for numeric/Object types, false for bool, or null for nullable T. A non-nullable
project type has no fabricated value and requires a typed source binding; change
type and source together. These are Designer creation defaults, not Flutter
constructor defaults. Builder's Child preset returns Child or the projection's
empty SizedBox.shrink()/SliverToBoxAdapter() result.

Child and builder output must match the selected box/sliver rendering protocol.
Cross-protocol custom builders are not represented by these fixed projections.
Widget return typing cannot prove RenderBox/RenderSliver; application output must
still match its selected parent.

## Proof, source and events

Each source is proven against ValueListenable<T> and each builder against
ValueWidgetBuilder<T>, using the same separately proven selected T. The catalog's
Object generic placeholders never replace the actual selected source type in
generated Dart or proof. A proof-only Foundation import qualifies ValueListenable;
the generator preserves implicit dart:core scope rather than injecting a prefixed
core import that would break user code.

Original source.value and callback invocation-result witnesses reject hidden
dynamic values/returns, incorrect T, nullable outer sources, nullable Widget
returns, wrong arity and a required non-null Child parameter. Closed generic-type
symbol admission still requires exact prepared manifests, Type evidence and the
corresponding trusted SDK core class file. No broad dart:core allowance was added.

Builder is BUILDER, not a native Event. Value listenable is a source object, not an
event producer. FD/source round-trip, Save/reopen, insertion/move, typed edits,
package imports and Undo/Redo use the existing guarded pipeline.

## Isolated Canvas and icons

Canvas mounts a native ValueListenableBuilder<Object?> with an immutable,
preview-owned AlwaysStoppedAnimation and a pass-through Child callback. It does
not evaluate project type identities, source values, getters, factories or custom
builders. A custom source uses an inert null placeholder; the preview labels
custom behavior as unavailable and does not simulate notifications. Run the
application to verify real value-dependent output and source ownership.

The preview T is deliberately not the unavailable project type. Empty geometry,
zero-size selection, Child drops, sliver axes/reverse/RTL, profile rotation and
retained element identity use the existing native paths. Eight reviewed SVGs
(light/dark, 16/32, box/sliver) use a value diamond/repeat motif and distinct child
protocol markers; no font glyphs or external resources.

## Inventory

172 definitions: 164 typed scalar and eight structural, 6950 writable property
rows (6932 outside Scaffold), 142 const-capable definitions.
Layout 38, Material 49, Scrolling 50. Ordinary destinations: 163 (145 any-widget,
18 constrained); 28036 placement candidates, 18437 accepted and 9599 rejected.
Required wrappers remain 29, including 22 root-compatible box wrappers.
Boolean rows: 633; nullable boolean unions remain 50.
Native Events remain 154 across 40 types. All callables: 211 across 64 catalog
types, including 43 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- All three real-SDK gates passed, explicitly enabled (no skips):
  box 62.318 s; generic type matrix 115.314 s; sliver 59.817 s.
  Fifty-four generated forms run 128 native Flutter tests.
  The matrix includes all six built-ins with and without nullability, class/enum/
  closed-generic aliases, nullable aliases, and rejection of hidden dynamic.
  It verifies package imports, exact type proof, Save/reopen, Undo/Redo,
  real source replacement, immediate replacement values, equality suppression,
  listener removal without owner disposal, prebuilt Child, nullable notifications,
  inherited updates, RTL/light/dark, and real AnimationController forward/reverse.
- Full Flutter regression passed: 5207 tests, including 42 new Canvas tests.
  flutter analyze reports no issues. Release Web build passed.
- Full Java core regression and focused NetBeans/analyzer/packaging-contract tests
  passed: 3647 unique reported cases, zero failures/errors, 56 conditional skips.
  This is not an unfiltered whole-reactor run; the unrelated Windows JNA
  metric test remains outside this slice. The three new real-SDK gates above
  were run separately with their opt-in flag enabled and all passed.
- Maven install and nbm:cluster passed. Byte-level verification checked the new
  schema, generator/analyzer/properties classes, all eight new SVGs, 40 packaged
  runner sources and the packaged Web manifest against 35 local release files.
  All four development-cluster JARs match the NBM. No IDE restart was performed.
- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
  8828416 bytes, built 2026-09-13T13:42:58.5384173Z.
  SHA-256: bede1e800cfdfa7564cca263e48952b76221e6e219e735aa6f0a69dbd68d47d0.

Next planned slice: [TweenAnimationBuilder](https://api.flutter.dev/flutter/widgets/TweenAnimationBuilder/TweenAnimationBuilder.html),
with owned Tween<T>, Duration, Curve, optional Child and onEnd; not implemented
as part of this slice.
