# TweenAnimationBuilder vertical slice

Pinned Flutter 3.44.8. Canonical checkout:
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native contract

[Constructor](https://api.flutter.dev/flutter/widgets/TweenAnimationBuilder/TweenAnimationBuilder.html):
const TweenAnimationBuilder<T>({Key? key, required Tween<T> tween,
required Duration duration, Curve curve = Curves.linear,
required ValueWidgetBuilder<T> builder, VoidCallback? onEnd, Widget? child}).

[Ownership and lifecycle](https://api.flutter.dev/flutter/widgets/TweenAnimationBuilder-class.html):
Flutter takes exclusive ownership of the Tween and mutates it. Supply a fresh
instance; do not share it between widgets or inspect/mutate it after transfer.
For project bindings, a factory is recommended; static typing cannot establish
freshness or exclusive ownership of arbitrary application objects.

The first animation uses begin/end. Null begin starts at end without an initial
transition. End must be non-null even when T is nullable. Retargeting uses the
current animated value, ignoring a replacement begin. onEnd runs on completed
animations, including an actual initial transition. Equal targets do not restart
an animation. Duration zero is allowed. Child is passed unchanged to Builder.

## Designer surface

Two placements generate the same real unnamed constructor:

- Layout order 310: flutter.widgets.TweenAnimationBuilder (box Child/result).
- Scrolling order 510: flutter.widgets.TweenAnimationBuilder.sliver (sliver Child/result).

The .sliver suffix is a catalog projection, not an SDK constructor. All native
arguments are represented: shared Key identity, Tween, Duration (microseconds),
Curve, Builder, On end, optional Child. Value type and Nullable value type are
additional Designer controls for the explicit generic T, never constructor args.

Supported type identities match ValueListenableBuilder: six core built-ins,
simple current-file/declared-package classes, enums and closed typedefs.
A project typedef can name SDK types (Color, Rect), complex generic types, or
nullable aliases. Direct raw type-expression strings and dynamic/void/Never/Null
identities are not admitted. Hidden dynamic and mismatched nullability aliases
are rejected by independent type evidence. This is the reviewed Designer type
surface, not a claim that Flutter prohibits other type arguments.

The required Tween property accepts a Default preset or a typed object reference,
getter, member or zero-argument factory. Custom Tween subclasses (e.g. ColorTween,
RectTween and application-specific interpolation) are supported through those
bindings. This is a typed binding editor, not an inline Tween source-code editor.
Begin/end/interpolation of custom tweens are authored in the referenced Dart code;
no unvalidated raw-expression escape hatch was added.

The Default preset creates a new instance per build: Tween<double/num> from
0.0 to 1.0, IntTween from 0 to 1, or ConstantTween for empty String, false bool,
and zero Object. Nullable built-in types retain a non-null end. Every project type,
including nullable T, requires a custom Tween; no null end or fictitious object
is generated. Creation uses double, Default tween, 300000 microseconds and the
Child builder preset. These are Designer creation defaults, not SDK defaults.

The Value type dialog atomically edits T, nullability, Tween and Builder.
Cancel publishes nothing, stale drafts are rejected, Undo restores all four.
Duration supports portable nonnegative integer microseconds or typed Duration
references/factories; Curve supports all 43 pinned presets or typed Curve bindings.
On end is an optional native Event and supports shared handler/navigation/source
history. Builder is a required BUILDER callback, not an Event.

Child/output must match the chosen box/sliver protocol. Cross-protocol custom
builders are not represented by these fixed projections; Widget return typing
alone cannot prove a RenderBox/RenderSliver result.

## Proof and isolated Canvas

Generated Tween<T> and ValueWidgetBuilder<T> proofs share the separately proven T.
Original tween.lerp(0.5) and callback-result witnesses reject hidden dynamic
expression types. These witnesses are analyzed only, never executed. Runtime
end non-nullness and application ownership remain the caller's responsibility.

Core generic and Duration symbol admission remains tied to exact prepared
manifests, occurrence IDs, source spans, expected class files and accepted
analysis. No blanket SDK-root or raw Dart allowance was added.

Canvas mounts a native TweenAnimationBuilder<double> with its own fresh 0-to-1
tween and pass-through Child. Application T/Tween/builder/curve/duration/onEnd
references are never evaluated. Typed curves fall back to linear and referenced
durations to 300000 microseconds. Custom behavior is explicitly labeled.
No generated handler is invoked by preview completion. Run the application to
test real interpolated output, ownership and callbacks.

The native preview keeps Child geometry, zero-size selection, DnD, box/sliver
protocols, axes/reverse/RTL and mobile/tablet orientation handling. Eight reviewed
16/32 light/dark SVGs use an interpolation ramp with distinct child markers.

## Inventory

174 definitions: 166 typed scalar and eight structural; 6964 writable rows
(6946 outside Scaffold); 144 const-capable catalog definitions.
Layout 39, Material 49, Scrolling 51. Ordinary destinations 165:
146 any-widget and 19 constrained. Placement matrix: 28710 candidates,
18765 accepted, 9945 rejected. Required wrappers remain 29 (22 root-compatible).
635 boolean rows and 50 nullable boolean unions.
Native Events: 156 across 42 types. All callables: 215 across 66 types,
including 45 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- All three real-SDK gates passed, explicitly enabled without skips:
  box 81.369 s, selected-type matrix 168.125 s, sliver 63.023 s.
  Sixty generated forms run 142 native Flutter tests. This includes six built-ins
  with/without nullability, classes/enums/closed generic aliases, ColorTween,
  RectTween, nullable aliases, strict rejected bindings, save/reopen/history,
  mutable tween ownership, mid-flight retargeting, completion, zero duration,
  null begin, invalid null end, Child retention and rendering protocols.
- All 5249 Flutter regression tests passed, including 42 new Canvas tests.
  flutter analyze reports no issues. Release Web build passed with the project's
  required no-tree-shake-icons, no-web-resources-cdn and no-PWA settings.
- Full Java core regression and focused NetBeans/analyzer/resource-contract
  checks passed: 3664 unique reported cases, zero failures/errors, 59 conditional
  skips. The three real-SDK tests above were independently enabled and passed.
  This is not an unfiltered whole-reactor run: the known unrelated Windows JNA
  metric test remains outside this slice.
- Maven install and nbm:cluster passed. Byte verification checked the compiled
  schema/generator/analyzer/editor classes, all eight new SVGs, 40 packaged Canvas
  source files and the packaged Web manifest against 35 local release files.
  All four development-cluster JARs match the NBM. NetBeans was not restarted.
- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
  8836072 bytes, built 2026-09-13T14:17:23.3591083Z.
  SHA-256: f29394013008fe31ebc693727734b25f67cb4310a4b5ae721f6137ab5052aa8b.

Next planned slice: [AnimatedOpacity](https://api.flutter.dev/flutter/widgets/AnimatedOpacity/AnimatedOpacity.html).
No CJK IME or non-Windows Canvas-provider work is part of this slice.
