# AnimatedModalBarrier

Pinned contract: Flutter 3.44.8, widgets/modal_barrier.dart.
[Constructor](https://api.flutter.dev/flutter/widgets/AnimatedModalBarrier/AnimatedModalBarrier.html)
and [class](https://api.flutter.dev/flutter/widgets/AnimatedModalBarrier-class.html).

## Complete constructor and defaults

All seven non-Key arguments are represented in SDK order. Shared Key and identity
behavior is unchanged. This is a childless Basic palette entry, not a wrapper.

1. Color: required, non-null Animation<Color?>. Local ARGB, reviewed theme tokens
   and a local null color generate AlwaysStoppedAnimation<Color/Color?>. Creation
   supplies AlwaysStoppedAnimation<Color?>(null), a transparent but present
   animation. The property cannot be omitted/reset. This differs from the
   optional plain Color? on ModalBarrier.
2. Dismissible: optional Boolean checkbox; omission preserves true, not null.
3. Semantics label: optional string, including empty, null or unset.
4. Barrier semantics dismissible: optional Boolean checkbox, null or unset.
   Both omission and explicit null preserve the native platform-dependent
   semantics decision. Unlike ModalBarrier, this constructor does not default
   the argument to true.
5. On dismiss: optional nullable VoidCallback? source or native Event handler.
6. Clip details notifier: optional nullable ValueNotifier<EdgeInsets>? source.
7. Semantics on tap hint: optional string, including empty, null or unset.

The inherited listenable is derived from Color and is not a separate constructor
property. There is no Child, implicit route, local controller, duration or curve.
The widget fills bounded parent constraints. An unbounded Column/ListView axis
still needs explicit constraints; no dimensions are silently stored.

## Animation, source and ownership

Color supports all eight current/package, root/member, reference/zero-argument
factory forms. Strict analyzer evidence requires non-null Animation<Color?>,
including its covariant Animation<Color> subtype; a plain Color, nullable outer
animation, wrong generic animation or dynamic source is rejected.
A typed animation may currently yield a null color without being a null animation.

Generated Dart preserves live AnimationController/ColorTween updates, forward and
reverse playback and external animation replacement. AnimatedWidget subscribes
to the current animation, detaches from replaced/disposed widget instances and
does not own or dispose the application's animation/controller.
Local theme colors are resolved from the current build context and then wrapped
in a stopped animation; literal and null stopped animations can be const.

The required color editor offers stopped literal/theme color, stopped transparent
color and typed project animation. It does not expose omission or progress-widget
fallback labels. Drafts are cancel-safe; invalid sources do not publish changes.
Property rows remain stable across edits and save/reopen; Boolean cells use
the shared centered checkbox editor with distinct unset/null semantics.

## Events and accessibility

On dismiss appears in Events. Create Handler, bind, navigate, rename and disconnect
use existing analyzed source/model transactions. Callback code survives binding
removal and save/reopen. Dismissible false retains the callback but ignores it at
runtime. Dismissible true calls it when present; otherwise Navigator.maybePop is
used. A supplied callback owns dismissal; it does not automatically remove a route.

The wrapped native ModalBarrier keeps platform accessibility behavior:
dismissible semantics are supported on Android, iOS and macOS, not Windows,
Linux or Fuchsia. Non-null labels, the semantics Boolean and ambient direction
remain native. Clip details notifier changes semantic bounds only, not pixels,
layout or physical hit extent. The notifier is application-owned.

## Canvas safety and placement

Canvas renders a real AnimatedModalBarrier with a stopped local color. An
AbsorbPointer shield plus a no-op callback prevents callbacks, navigation and
SDK alert sounds while keeping the Designer selection layer operational.
Project animations are never executed: source Color previews as a stopped null
color, source On dismiss as a no-op and source Clip details notifier as null.
Diagnostics identify only the actual substituted fields. Exported Dart is unchanged.

An unbounded axis produces an explicit preview diagnostic and a zero-size
selectable target; restoring bounds restores native rendering. A transparent
barrier remains selectable. Placement, move, remove, compatible wrapping,
undo/redo and source/model persistence follow the ordinary box-leaf contract.

## Inventory and verification

208 definitions, 7,296 writable rows (7,278 outside Scaffold), 200 typed
property definitions, 174 const-capable definitions and 28 Basic entries.
Native Events: 174 rows across 60 types. All callables: 237 across 86 types,
with 48 builders and 10 delegates. FD 16, Catalog API 15, Canvas model 19 and
transport 1 are unchanged; no existing snapshot migration is needed.

Verification covers local/default/null/theme values, all typed source forms,
rejected types, constructor placement and required-color omission, editor drafts,
stable rows and Boolean editors, event lifecycle, pair save/reopen, actual
animation pixels/listeners/controllers, route behavior and platform semantics.
Flutter 3.44.8 verification: 100 focused Canvas tests, all 11,072 Canvas tests,
94 generated/native real-SDK cases, and a clean Flutter analyzer run.
Fresh Java reports contain 3,973 tests across 592 suites: zero failures or
errors and 102 conditional skips. The AnimatedModalBarrier SDK test ran with the
pinned SDK. Maven install and nbm:cluster passed.

Packed verification matched the schema, color-animation editor classes, all
four SVG variants, 40 runner sources and 35 Web artifact files against current
manifests. All four development-cluster JARs match the NBM. No IDE restart,
sample-project modification or deferred platform-provider work was performed.
