# Listener interaction slice

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned SDK's
`widgets/basic.dart`, `rendering/proxy_box.dart`, `services/mouse_tracking.dart`
and [official constructor](https://api.flutter.dev/flutter/widgets/Listener/Listener.html).

## Scope and usage

The **Interaction** palette contains **Listener** after GestureDetector.
It supports all **10 non-key constructor properties**: **9 events** and
`behavior`, plus the optional structural `child`. The constructor is const;
generation still respects whether a particular child or callback is const-safe.
`key` remains Designer-owned, as elsewhere in the palette.

Select an existing widget and use **Wrap with Listener** in the Designer toolbar.
This explicit action keeps its entire subtree and stable IDs, validates the
prospective placement and applies one analyzed command. Existing parent-data
and slot constraints still apply. Ordinary palette Add/Drop adds a new Listener
and never silently wraps the selection. The existing GestureDetector wrap action
retains its name and behavior; both use the same internal placement checks.

Under **Events**, create a handler, select a compatible existing method or bind a
typed project reference. Editing, navigation, rename, disconnect, save/reopen,
stale-editor protection and Undo/Redo use the existing shared Events workflow.
Disconnect removes the binding, not the user's Dart method. Handler bodies stay
outside managed regions and are never serialized into `.fd`.
Closing or cancelling an unsubmitted editor draft leaves the binding and source
unchanged. Create/Bind/Rename/Disconnect/Apply Reference commit their atomic
operation immediately and close the editor; mutation actions are unavailable in
read-only or stale contexts.

## Complete constructor contract

| Event | Exact argument |
| --- | --- |
| `onPointerDown` | `PointerDownEvent` |
| `onPointerMove` | `PointerMoveEvent` |
| `onPointerUp` | `PointerUpEvent` |
| `onPointerHover` | `PointerHoverEvent` |
| `onPointerCancel` | `PointerCancelEvent` |
| `onPointerPanZoomStart` | `PointerPanZoomStartEvent` |
| `onPointerPanZoomUpdate` | `PointerPanZoomUpdateEvent` |
| `onPointerPanZoomEnd` | `PointerPanZoomEndEvent` |
| `onPointerSignal` | `PointerSignalEvent` |

All nine callbacks return `void`. Each supports omission, explicit null, local
callback shorthand and typed Dart references. No default handler is fabricated.
All nine may coexist: GestureDetector's pan/scale recognizer conflicts do not
apply to raw Listener callbacks.

`behavior` accepts `HitTestBehavior.deferToChild`, `opaque` or `translucent`.
Omission preserves **deferToChild even without a child**; explicit null is
invalid. An empty Listener can receive pointer events when configured for the
appropriate hit-testing behavior; adding it alone does not invent application
interaction. `onPointerSignal` includes signals such as wheel scrolling, and
pan/zoom delivery depends on the input device/platform.

Listener observes raw pointer events; it does not replace gesture recognition
or provide mouse enter/exit, keyboard or focus callbacks. Those require their
actual Flutter APIs. `MouseRegion`, `Focus` and `NotificationListener` are not
implicitly added by this slice. No new State producer or controller lifecycle
is introduced.

## Persistence, analysis and Canvas

- Existing values suffice: FD schema **16**, Catalog API **15**, Canvas model
  protocol **19** and transport protocol **1** remain unchanged. Frozen schemas
  are not edited.
- Generated handler signatures import the reviewed gestures library for pointer
  event types. Every local shorthand and typed reference receives exact static
  type evidence before pair-save admission.
- Several Listener typedefs are absent from the Widgets/Material exports.
  Proof-only overlays qualify the fixed reviewed eight Rendering typedefs and
  the Services-owned `PointerHoverEventListener` from their actual SDK libraries.
  Collision-free prefixes preserve user imports and same-named project types.
  The analyzer's trusted-root, strict-cast and exact-evidence gates are unchanged.
- Canvas creates the real Listener with isolated local no-op callbacks; it never
  invokes application handler bodies or imports. Raw hover/move/cancel/signal
  notifications cannot select another Designer node or steal child selection.
  Empty wrappers remain discoverable for selection/drop independently of app
  hit-testing semantics.
- Native source and internal offline Web artifacts are regenerated together and
  checked against their exact byte-size/SHA-256 manifests. Web packaging does not
  enable another product Canvas provider.

## Verification

Coverage includes pinned constructor and typedef inventory, all binding forms,
const generation, typed proofs, behavior validation, explicit wrapping,
descendant/State metadata preservation, Events actions, source ownership,
save/reopen, Undo/Redo and Canvas selection/drop behavior.

Verified on 2026-09-08:

- Full `flutter-designer` suite: **1,912 tests / 224 suites**, no failures,
  errors or skips. This includes exact SDK constructor/typedef checks and all
  **132** callable signatures. Historical test snapshots remain distinct from
  the current **91-source / 77-destination** matrix (**7,007 cells**,
  **6,608 accepted / 399 rejected**).
- `DartCandidateAnalyzerTest`: **25 passing tests**, including fixed callback
  library ownership, collision avoidance, foreign-name preservation and
  strict-cast rejection controls.
- Selected NetBeans integration/UI/Events/packaging `verify`: **892 tests /
  32 suites**, plus **9 packaged NBM integration tests**, all passing with no
  skips. Both wrapper actions, property editors, source/restage, pair-save and
  history regressions are included; unrelated NetBeans suites were not all rerun.
- `ListenerRealSdkTest`: **11 Flutter tests**, all **9** callback proofs in both
  shorthand and typed-reference forms, actual raw render-event dispatch and
  pointer hit-testing. Wrong-signature and dynamic bindings are rejected even
  with suppressed Dart diagnostics; original files remain unchanged by analysis.
  The existing GestureDetector SDK regression also passed.
- Full Canvas suite: **1,691 passing tests**; `flutter analyze` clean. Source
  manifest **40/40** and internal Web manifest **35/35** match exact byte sizes
  and SHA-256 hashes. Offline Web was rebuilt with `--no-wasm-dry-run` and has
  exactly one local build record.
- Reactor install and both `nbm:cluster` commands passed. The NBM and both
  development clusters match all **1,640 plugin payload entries** and all three
  embedded dependency JAR hashes. Both interaction catalog classes and schema16
  are present.

Package: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(8,160,494 bytes). SHA-256:
`075611C3C68206BEDA49E2983A5B44B55CB5539D3C25CF6D4539701F814B861D`.

Interactive installed-IDE acceptance is separate and has not been performed:
add Listener, wrap an existing child, create/edit an event handler, change
behavior, save/reopen and exercise Undo/Redo. The running IDE was not restarted
or modified during this implementation.
