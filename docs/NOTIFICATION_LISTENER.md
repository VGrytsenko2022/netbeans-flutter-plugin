# NotificationListener interaction slice

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`widgets/notification_listener.dart` and the official
[constructor](https://api.flutter.dev/flutter/widgets/NotificationListener/NotificationListener.html)
and [Notification API](https://api.flutter.dev/flutter/widgets/Notification-class.html).

## Complete constructor and type contract

**Interaction → NotificationListener** adds the one const constructor
`NotificationListener<T extends Notification>`, its required child and optional
nullable `onNotification`. The key remains Designer-owned. The required
`notificationType` property selects the explicit generic argument; creation uses
`Notification`. There are no additional constructor properties or State producers.

All **14 public SDK filter types** are offered: Notification,
LayoutChangedNotification, ScrollNotification, ScrollStartNotification,
ScrollUpdateNotification, OverscrollNotification, ScrollEndNotification,
UserScrollNotification, SizeChangedLayoutNotification, ScrollMetricsNotification,
OverscrollIndicatorNotification, DraggableScrollableNotification,
KeepAliveNotification and NavigationNotification. All are exported by widgets.dart;
abstract base types are valid filters too.

The type editor also accepts a simple project class or typedef reference, with an
optional canonical package library. It does not accept a member, invocation,
constness flag, arbitrary Dart expression, raw generic expression or nullable
type spelling. A simple typedef can name a closed generic custom notification.
An analyzer proof independently verifies the actual non-null, non-dynamic type
and its `Notification` bound **even when the callback is omitted or null**.
Type aliases to unrelated classes, dynamic or nullable types are rejected.

## Events and editing workflow

`onNotification` is an **Event**, not a predicate property, despite returning a
boolean. Its exact callback type is `bool Function(T notification)`.

- `true` stops propagation to further ancestors; `false` continues.
- Omission and explicit null are preserved separately and both leave propagation
  unchanged. Notifications reach a listener only when their runtime type is a
  subtype of its selected `T`.
- Create, bind existing, project reference, navigate, rename and disconnect reuse
  the paired model/source workflow. Generated stubs use the safe contravariant
  signature `bool handler(Notification notification)`, compatible with every
  selected subtype. Existing specialized handlers are proved against the exact
  selected `T`, not forced to accept the base Notification type.
- Non-void generated stubs explicitly throw `UnimplementedError` until the user
  supplies their intended result. Designer does not silently choose an
  application's propagation policy.
- Changing the type never rewrites handler parameters or bodies. Incompatible
  existing handlers block application of the change rather than being discarded.
- Notifications may arrive during layout. A layout notification is not permission
  to call `setState` during layout; application code must follow Flutter's timing
  rules. No automatic notification-to-State binding is introduced.

**Wrap with NotificationListener** preserves the selected subtree and IDs.
Required-child palette drops use the atomic wrap workflow. Empty destinations do
not receive an invalid childless wrapper. Save/reopen, Undo/Redo and rejected
edits preserve the paired files and user-owned handler bodies. Property updates
keep row identities and notify only the type and affected callback rows.

## Canvas and evidence boundaries

The 14 SDK selections render real, exactly typed NotificationListener widgets.
Callback presence uses a local inert callback returning **false**; absence/null
uses null. Canvas never invokes application code or consumes framework/Designer
notifications on behalf of the user's handler.

**Custom-type preview limitation:** custom class/typedef code cannot execute in
the isolated Canvas. Reference identities are stripped from its payload; it uses
an explicitly labelled base-Notification approximation. The saved model and
generated Dart retain the exact custom type and callback. Child identity,
geometry and selection remain stable when changing the filter.

The generated Type proof carries the closed `Notification` bound through the
exact analyzer/evidence manifest. An SDK-qualified witness checks that bound
independently from ordinary source diagnostics, including suppressed diagnostics.
Dropping or changing the bound cannot satisfy the prepared pair's evidence.
Existing Radio proofs and trusted-root checks remain intact.

FD schema **16**, Catalog API **15**, Canvas model protocol **19** and transport
protocol **1** are unchanged. No frozen schema is rewritten.

## Verification

Measured automated results on 2026-09-08:

- Full `flutter-designer` suite: **1,972 tests / 233 suites**, zero failures,
  errors or skips, including **15 NotificationListener core tests** and two
  payload regressions. Pinned SDK constructor/type checks were enabled.
- NotificationListener property editor: **7/7**; explicit wrap planner: **4/4**;
  shared Events bridge: **22/22**. Required-child and specialized-handler editing
  retain the established atomic workflow.
- Full Flutter Canvas suite: **1,782/1,782**, including **22 NotificationListener
  tests**; `flutter analyze` is clean and the offline Web release passes.
- All **40 source / 35 Web** manifest entries have verified byte sizes and
  SHA-256 hashes. `main.dart.js`: **3,345,577 bytes**, SHA-256
  `ce23703f4a88b08831ccc9b44d4d47c5e00a52298677b70a1aae05ae62ad1e7c`.

- `NotificationListenerRealSdkTest`: **2 Java scenarios / 16 generated Flutter
  widget tests** pass. They verify all 14 generated generic filters, real subtype
  dispatch, true/false ancestor propagation, null dispatch, typed custom handlers,
  custom aliases including a closed generic typedef, and incompatible filter
  changes. Wrong/dynamic/nullable/non-type references and invalid callback
  signatures are rejected. Analyzer checks leave original source files intact;
  save/reopen and regeneration preserve implemented handler bodies.
- Broad selected plugin regression: **942 tests / 42 suites**; analyzer/proof
  regression: **30 tests / 2 suites**; Failsafe integration: **9 tests**. All have
  zero failures, errors or skips. The run includes all five interaction wrappers'
  save/reopen/history scenarios, evidence-bound tampering checks, and earlier
  interaction/State real-SDK regressions.
- Reactor `install` and both root/module `nbm:cluster` builds pass. All **1,668
  plugin payload entries** (excluding packaging-specific manifests) match the
  built JAR, NBM and both development clusters. All three dependency JARs match
  byte-for-byte; schema 16 and all five interaction schemas are present.
- `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`:
  **8,217,079 bytes**, SHA-256
  `92e193ccf1a90756b8fc9cd84d8cb8b7fa32cabcb3e53ee7a1b4981fa3e3d29f`.

Installed-IDE interactive acceptance remains separate; automated checks do not
mean the running IDE was restarted or manually inspected.

## Next stage

Close the **seven known builder/predicate gaps across five admitted widgets** in
[the Events audit](EVENTS_API_AUDIT.md#known-callable-gaps-in-admitted-constructors),
starting with Scaffold's missing callable surface. This slice does not claim
completion of the entire Flutter palette, dynamic builder constructors or
object-owned controller/listener APIs.
