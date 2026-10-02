# Focus interaction slice

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned SDK's
`widgets/focus_scope.dart` and `widgets/focus_manager.dart`, and the official
[Focus constructor](https://api.flutter.dev/flutter/widgets/Focus/Focus.html),
[external-node constructor](https://api.flutter.dev/flutter/widgets/Focus/Focus.withExternalFocusNode.html)
and [keyboard callback](https://api.flutter.dev/flutter/widgets/FocusOnKeyEventCallback.html).

## Scope and workflow

**Interaction → Focus** provides both `Focus()` and
`Focus.withExternalFocusNode()`. The schema contains all **12 non-key, non-child
SDK properties**, one constructor selector and the **required child** slot.
`key` remains Designer-owned. **Wrap with Focus** explicitly wraps an existing
widget while preserving its subtree, stable IDs, event bindings, State metadata
and user-owned source. Required-child admission follows the existing atomic
wrapper workflow; an invalid empty Focus is not added to the model.

The constructor selector is `standard` or `withExternalFocusNode`. Omission means
standard. Before choosing the external constructor, supply a typed non-null
`focusNode` reference. No project node is fabricated to make that change pass.

## Complete constructor contract

| Property | Standard constructor | External-node constructor |
| --- | --- | --- |
| `focusNode` | Omitted/null uses an SDK-owned internal node; typed `FocusNode?` references/factories accepted | Required typed non-null `FocusNode` reference/factory |
| `parentNode` | Omitted/null or typed `FocusNode?` reference/factory | Same |
| `autofocus` | Boolean; omission false | Same |
| `onFocusChange` | `void Function(bool hasFocus)` | Same |
| `onKeyEvent` | `KeyEventResult Function(FocusNode node, KeyEvent event)` | Callback comes from the external node |
| `onKey` | Legacy `KeyEventResult Function(FocusNode node, RawKeyEvent event)`; deprecated by Flutter | Callback comes from the external node |
| `canRequestFocus` | Nullable boolean; SDK/node fallback | Comes from the external node |
| `skipTraversal` | Nullable boolean; SDK/node fallback | Comes from the external node |
| `descendantsAreFocusable` | Nullable boolean; SDK/node fallback | Comes from the external node |
| `descendantsAreTraversable` | Nullable boolean; SDK/node fallback | Comes from the external node |
| `includeSemantics` | Boolean; omission true | Same |
| `debugLabel` | Omitted, explicit null or String, including empty String | Comes from the external node |

The seven standard-only properties remain stored when changing to the external
constructor, but are not emitted as constructor arguments. Their rows explicitly
show that they are inactive and retained. Changing back restores their use.
Inactive key bindings cannot be newly created or applied; existing handler
navigation, rename and disconnect remain explicit operations on retained data.
No constructor switch silently deletes a handler or its body.

Concrete booleans retain the centered checkbox editor. The four nullable boolean
properties distinguish omitted, explicit null and literal true/false. Debug label
also distinguishes omission, null and empty text. All six boolean properties
reuse reviewed State consumers; no automatic focus-state producer is introduced.
Standard-only consumers remain retained but inactive in external mode.

## Events and ownership

Events reuse create, bind existing, typed project reference, navigate, rename and
disconnect. Every binding goes through exact analyzer admission and paired
source/model history. Handler bodies remain in user-owned Dart, not the `.fd`
model. Save/reopen and property regeneration must preserve them.

Keyboard events are not void notifications. Returning `KeyEventResult.ignored`
allows ancestor handlers to receive the event. `handled` and
`skipRemainingHandlers` both stop focus-tree propagation, with different handling
results for the platform. Modern and legacy callbacks retain Flutter's result
combination semantics. New non-void handlers use the existing explicit
`UnimplementedError` stub until the user supplies the desired result; Designer
does not silently choose the application's keyboard policy.

Without a supplied `focusNode`, Flutter owns and disposes its internal node. A
project-owned node, including one returned by a getter/factory, stays
user-owned: the application must retain a stable instance and dispose it in its
owner's lifecycle. A factory reference is not a generated caching/lifecycle
mechanism. External mode obtains attributes from that node; standard mode applies
its own configured attributes. Null fields retain the actual SDK fallback and
update behavior, rather than being rewritten into invented explicit defaults.
In particular, Flutter keeps a supplied node's existing debug label on initial
mount; a changed widget label is applied during update. An internal node receives
the configured label when it is created.
Shared-node identity, attachment and parent graphs remain application concerns.

The proof grammar adds only the reviewed nullable `FocusNode?` destination, not
arbitrary nullable project types. External `focusNode` is proved against
non-null `FocusNode`. Dynamic/wrong values and nullable external references remain
rejected. The analyzer's trusted-root, strict-cast and exact-evidence checks are
not weakened.

## Canvas and persistence

Canvas renders a real Focus and uses local inert callbacks; key callbacks return
`ignored` so application preview callbacks do not consume Designer shortcuts.
Application handlers and project `FocusNode` code are never executed.

**Reference preview limitation:** project node references carry presence only,
without identifiers or source. Canvas uses a locally owned node approximation
and explains that the project's node state/callbacks cannot be reproduced.
A project `parentNode` cannot be reconstructed; the nearest preview focus ancestor
is used with an explicit limitation. External mode uses a stable locally owned
node with reset/dispose handling; retained standard-only fields must not leak into
its configuration. Saved properties and generated Dart keep exact references.

FD schema **16**, Catalog API **15**, Canvas model protocol **19** and transport
protocol **1** remain unchanged. Frozen schemas are not rewritten. Native source
and internal offline Web artifacts must match their exact size/SHA-256 manifests.

## Verification

Measured automated results on 2026-09-08:

- Full `flutter-designer` suite: **1,955 tests / 230 suites**, zero failures,
  errors or skips. This includes **20 Focus core tests** and the new payload
  regressions. Pinned SDK constructor/signature checks were enabled.
- `FocusRealSdkTest`: both Java scenarios pass, generating **17 Flutter widget
  tests**. They cover all nine modern/legacy keyboard result combinations and
  platform handling results, focus changes, null/default flags, all six State
  consumers (including nullable booleans), internal/external node disposal,
  constructor-specific node configuration and the supplied-node debug-label
  update behavior. Exact analyzer admission checks shorthand/typed callbacks,
  nullable getter/factory references and rejects wrong/dynamic values or nullable
  external nodes even with suppressed source diagnostics. Original files remain
  unchanged by analysis.
- Focus property editor tests: **7/7**; explicit wrap planner: **4/4**; shared
  Events bridge: **21/21**. Tests include stable row identities, scoped variant
  notifications, centered booleans, debug-label omission/null/empty text,
  retained inactive handlers and required external node guards.
- Full Flutter Canvas suite: **1,760/1,760**, including **14 Focus tests**;
  the combined interaction/exclusion selection has **137 passing tests**.
  `flutter analyze` and internal offline Web release pass.
- All **40 source / 35 Web** manifest entries match exact byte sizes and hashes.
  `main.dart.js`: **3,342,189 bytes**, SHA-256
  `eb5ef9d142462253558b80740291c9bfb61be21dcec4b65c0f200a8b59427f6c`.

- Broad selected plugin regression: **924 tests / 39 suites**; analyzer and
  static-type proof regression: **28 tests / 2 suites**; Failsafe integration:
  **9 tests**. All completed with zero failures, errors or skips. The plugin run
  includes all four interaction wrapper save/reopen/history scenarios and the
  real-SDK Focus, MouseRegion, Listener, GestureDetector and State regressions.
- Reactor `install` and both root/module `nbm:cluster` builds pass. All **1,658
  plugin payload entries** (excluding the packaging-specific manifest) match the
  built JAR, NBM and both development clusters. All three dependency JARs match
  byte-for-byte; FD schema 16 and all four interaction schemas are present.
- `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`:
  **8,196,731 bytes**, SHA-256
  `853bd3f0f77236396f677ac9850499ddb920f4b460774c7dedcaf27d098e61e6`.

Installed-IDE interactive acceptance is separate; automated checks do not claim
that the running IDE was restarted or manually inspected.

## Next stage

Next interaction widget: **NotificationListener**, including its notification
type, boolean callback result and propagation semantics. The **seven known
builder/predicate gaps across five existing widget definitions** remain separately
tracked in [the Events audit](EVENTS_API_AUDIT.md#known-callable-gaps-in-admitted-constructors).
Adding Focus does not mean the entire Flutter palette or all controller/listener
APIs are complete.
