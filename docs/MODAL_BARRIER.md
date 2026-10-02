# ModalBarrier

Pinned contract: Flutter 3.44.8, `widgets/modal_barrier.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/ModalBarrier/ModalBarrier.html)
and [class](https://api.flutter.dev/flutter/widgets/ModalBarrier-class.html).

## Complete constructor and placement

All seven non-Key parameters are represented in SDK order; shared Key/identity
behavior is unchanged. None is required and no constructor default is persisted
on creation. The result is a childless `const ModalBarrier()`.

- Color: literal ARGB (including fully transparent), reviewed theme color,
  nullable typed `Color?` reference/getter/zero-argument factory, null or unset.
- Dismissible: checkbox, true/false or unset (native default true); not null.
- On dismiss: optional nullable `VoidCallback?` reference/getter/factory,
  null or unset. This is a native Event, not a builder or state binding.
- Semantics label: string (including empty), null or unset.
- Barrier semantics dismissible: checkbox with true/false/null/unset.
  Unset preserves the constructor default true. Explicit null follows Flutter's
  platform-dependent semanticsDismissible computation; it is not persisted true.
- Clip details notifier: nullable typed `ValueNotifier<EdgeInsets>?`
  reference/getter/factory, null or unset. The notifier is application-owned.
- Semantics on tap hint: string (including empty), null or unset.

The barrier fills its parent's constraints. It requires bounded width and height;
placing it directly in an unbounded Column/ListView axis does not invent an
intrinsic size. Use explicit bounds or an appropriately constrained Stack.
It can be inserted, moved, removed or wrapped by compatible ordinary box wrappers,
but has no child/drop slot of its own. There is no implicit child, route wrapper,
width/height property, duration or color animation. AnimatedModalBarrier is a
separate next slice.

## Events and application semantics

When Dismissible is true, a supplied On dismiss callback runs instead of route
navigation; the callback owns dismissal and does not automatically remove the
barrier. Null/unset calls `Navigator.maybePop`. The root route may decline to pop.
Dismissible false still blocks physical input and asks Flutter for its alert sound;
a stored callback remains intact and is ignored at runtime.

Create Handler, bind, navigate, rename, disconnect and source save/reopen use the
shared revision-bound, analyzed source/model transactions. Disconnect removes the
binding and preserves the user's method; it restores native maybePop behavior,
not a generated no-op. Changes made while Dismissible is false are still retained.

Accessibility dismissal is natively supported on Android, iOS and macOS, not
Windows, Linux or Fuchsia. Inclusion also depends on Dismissible and Barrier
semantics dismissible. Label-driven semantic tap/dismiss actions require a
non-null label; ambient Directionality supplies text direction. These are SDK
platform branches, not new desktop Canvas providers.

Clip details notifier changes only semantic bounds using physical EdgeInsets;
it does not clip pixels, change layout or shrink physical hit testing.
Notifier creation, updates and disposal remain in application code. No local
notifier is silently allocated by generated build methods. Strict analyzer
evidence requires ValueNotifier<EdgeInsets>?, not a weaker ValueListenable,
directional/mixed geometry notifier, nullable-insets notifier or dynamic source.
All three source-capable parameters support the existing eight reference forms,
including explicitly typed nullable sources that currently evaluate to null.

## Editing and Canvas safety

Typed rows preserve identity through incremental refresh and save/reopen.
Boolean values use centered checkboxes; unset and explicit null stay distinct.
Invalid drafts do not publish; optional reset retains native omission semantics.
On dismiss appears in Events, all other fields in Properties.

Canvas renders the real ModalBarrier in bounded space. A selection-preserving
input shield and a no-op callback suppress project callbacks, route navigation
and alert sounds. This is a documented design-preview substitution, not a change
to exported Dart. Source Color previews transparently; a source notifier is
omitted; diagnostics name the actual substituted fields. Local values remain
unchanged. Project code never executes in Canvas.

For an unbounded axis, Canvas shows a specific unavailable-preview diagnostic and
a zero-size selectable representation; it does not persist invented dimensions.
Restoring bounds restores native rendering. A transparent barrier remains
selectable. Native route behavior, callback handling, platform accessibility,
semantic clipping/lifecycle and pixel/hit behavior are covered separately by
real-SDK tests.

## Inventory and verification

207 definitions, 7,289 writable properties (7,271 outside Scaffold), 199 typed
property definitions, 173 const-capable definitions, 27 Basic entries.
Native Events: 173 rows / 59 types. All callables: 236 rows / 85 types, with
48 builders and 10 delegates. FD 16, Catalog API 15, Canvas model 19 and transport
1 remain unchanged. Original historical widget totals are not remaining-work counts.

Coverage includes constructor/FD/generation/placement contracts, typed property
and checkbox editors, strict nullable source proofs, event lifecycle and
save/reopen, native route/semantics/notifier behavior, Canvas isolation,
transparent selection and unbounded-layout recovery. Verification on Flutter 3.44.8: 100 focused Canvas tests, all 10,972 Canvas
regressions, 88 generated/native real-SDK runtime cases and a clean Flutter
analyzer run. Fresh Java reports: 3,964 tests across 590 suites, zero failures
or errors, 102 conditional skips (the ModalBarrier real-SDK test was run
separately with the pinned SDK).

Maven install and nbm:cluster passed. Package verification matched the schema,
all four SVG variants, all 40 runner source files and all 35 Web artifact files;
the four development-cluster JARs match the NBM. No IDE restart or sample-project
mutation was performed.
