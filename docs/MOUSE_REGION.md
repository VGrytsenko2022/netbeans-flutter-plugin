# MouseRegion interaction slice

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned SDK's
`widgets/basic.dart`, `services/mouse_tracking.dart`, `services/mouse_cursor.dart`
and [official constructor](https://api.flutter.dev/flutter/widgets/MouseRegion/MouseRegion.html).

## Scope and usage

The **Interaction** palette contains **MouseRegion** after Listener. All **6
non-key constructor properties** are supported: **3 events**, `cursor`, `opaque`
and `hitTestBehavior`, plus the optional structural `child`. The constructor is
const; generated calls respect the const-safety of their actual values and child.
`key` remains Designer-owned.

Select an existing widget and use **Wrap with MouseRegion**. This explicit,
analyzed command preserves its subtree, IDs, State metadata and user source.
The three interaction wrap actions share internal placement validation without
changing the public GestureDetector/Listener actions. Parent-data and slot
constraints still apply. Ordinary palette Add/Drop adds a new MouseRegion; it
does not silently wrap the selected widget.

Events reuse create, bind-existing, typed-reference, navigate, rename and
disconnect actions. Handler bodies remain user-owned Dart outside managed
regions and are not serialized into `.fd`. Disconnect keeps the method. Cancelling
an unsubmitted draft does not change the binding/source; mutation buttons apply
their atomic operation immediately and close the editor. Read-only/stale
contexts cannot dispatch mutations.

## Complete constructor contract

| Property | Supported contract |
| --- | --- |
| `onEnter` | `void Function(PointerEnterEvent)` |
| `onExit` | `void Function(PointerExitEvent)` |
| `onHover` | `void Function(PointerHoverEvent)` |
| `cursor` | All 41 reviewed presets or a strict, analyzer-verified `MouseCursor` reference/factory; omission preserves `MouseCursor.defer`; explicit null is invalid |
| `opaque` | Omitted or boolean; SDK default true; shared State consumer DIRECT/NOT/EQUALS |
| `hitTestBehavior` | Omitted, explicit null, deferToChild, opaque or translucent; omission preserves SDK null |

Each event supports omission, explicit null, local callback shorthand and typed
project references. No default handler is fabricated. Cursor presets include all
**36 SystemMouseCursors**, **MouseCursor.defer/uncontrolled**, and **3
WidgetStateMouseCursor** presets. Custom constants, fields/getters/static members
and zero-argument factories reuse the existing closed Dart-object-reference
model, with exact `MouseCursor` type evidence. Arbitrary raw Dart expressions are
not accepted.

`opaque` controls mouse annotations behind the region; it is not a synonym for
`hitTestBehavior`. Cursor defer/uncontrolled preserve their actual SDK meaning.
WidgetStateMouseCursor uses the SDK's empty-state resolution when used directly
as a MouseRegion cursor; this wrapper does not fabricate button states.

Pointer movement and region changes determine enter/exit delivery. In
particular, [onExit is not called when a hovered MouseRegion is unmounted](https://api.flutter.dev/flutter/widgets/MouseRegion/onExit.html).
This is Flutter behavior, not a missing Designer event. Hover is not a substitute
for pressed-pointer movement, gestures, keyboard or focus APIs.

## Persistence, analysis and Canvas

- FD schema **16**, Catalog API **15**, Canvas model protocol **19** and transport
  protocol **1** remain unchanged. Existing value kinds suffice; frozen schemas
  are not edited.
- All three callback typedefs are Services-owned. Proof-only overlays qualify
  the fixed reviewed SDK types without changing user imports or source. Handler
  signatures import the actual gesture event types. Typed references and local
  shorthand receive the same strict analyzer proof before pair-save admission.
- `opaque` shares the existing State consumer editor and literal Canvas
  projection; no automatic State producer or controller lifecycle is added.
- Canvas uses a real MouseRegion with isolated local no-op callbacks. Application
  handlers and custom cursor implementations are never executed. Designer
  decoration inside the region does not override its cursor/hover semantics;
  ordinary deepest-child selection and discoverable empty-wrapper drop geometry
  remain separate from application event delivery.
- **Custom cursor preview limitation:** a project cursor reference is represented
  by presence only. Canvas uses `MouseCursor.defer` and an explicit preview-
  unavailable tooltip. Saved properties and generated Dart retain the exact
  custom reference/factory; the preview does not claim to display it.
- Native source and internal offline Web artifacts must match exact byte-size /
  SHA-256 manifests. No additional product Canvas provider is enabled.

## Verification

Coverage includes pinned constructor/typedef/cursor inventory, every preset and
binding form, invalid null values, exact generated symbol/type evidence, custom
cursor references/factories, State opacity, explicit wrapping, source/history,
save/reopen, cursor activation, overlapping regions and hover/unmount behavior.

Measured automated results on 2026-09-08:

- Complete `flutter-designer` suite: **1,932 tests / 227 suites**, no failures,
  errors or skips; the pinned Flutter SDK checks were enabled.
- Integrated plugin selection: **907 tests / 36 suites**, plus **25 analyzer
  tests** and **9 NBM package integration tests**, all passing. This includes
  Events/State editors, all three interaction wrapper planners, the actual
  property sheets, admission/drop matrices, source/history/save orchestration,
  real SDK checks and strict source/Web artifact contracts.
- Complete Flutter Canvas suite: **1,746/1,746**, including **55 MouseRegion
  tests**; `flutter analyze` is clean. Internal offline Web release passes.
- The MouseRegion real-SDK test generates **48 Flutter widget tests**: all
  41 presets, custom constant/factory cursors, three generated callbacks, real
  mouse movement and hovered-region unmount. Separate analyzer admission checks
  reject wrong/dynamic callbacks and wrong/dynamic/nullable cursor references
  even when diagnostic suppression is present. A correctly typed `MouseCursor?`
  getter is rejected because the constructor requires non-null `MouseCursor`.
- Native source manifest: **40 entries**; Web manifest: **35 entries**. Every
  declared size/hash matches. `main.dart.js` is **3,336,779 bytes**, SHA-256
  `5460c22bcd8bfacf50a584236e2601869a4d3741ffe532e79984cbe6f46e94a8`.

Reactor install and both `nbm:cluster` commands passed. The NBM and both
development clusters match all **1,648 plugin payload entries** and all three
embedded dependency JAR hashes (`flutter-designer`, `dart-analysis` and
`flutter-canvas-runner`). All three interaction catalog classes and schema 16
are present.

Package: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(**8,173,992 bytes**). SHA-256:
`567697F23737917BB9880A4598CF4435332C28BCBA52A23F7763B221C16B18B2`.

Installed-IDE interactive acceptance remains separate: add/wrap MouseRegion,
edit events/cursor/opacity, save/reopen and Undo/Redo. An automated test does not
claim that the running IDE was restarted or manually inspected.
