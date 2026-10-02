# Designer Events implementation contract

## Coverage means every supported API, not an arbitrary subset of widgets

The Events infrastructure is shared by every palette widget. An individual
widget exposes the event callbacks actually declared by its supported Flutter
constructor and inherited API. A widget without event callbacks must not acquire
invented constructor arguments such as `Container.onTap`.

The per-widget inventory, pinned SDK version, official API links, callable
classification and outstanding catalog gaps belong in
[Events API audit](EVENTS_API_AUDIT.md). Inventory coverage is not implementation
completion. A missing callback is an implementation gap, not evidence that Flutter
cannot support that event.

There are three distinct cases:

| Case | Designer behavior | Completion requirement |
| --- | --- | --- |
| Native event callback | Show the event under Events, with its exact callback contract | Binding, handler editing/navigation, source preservation and analyzer validation |
| No native event callback | Explain that the selected widget has no native events | Do not invent properties or report an implementation error |
| Interaction supplied by a wrapper | Identify the required explicit wrapper | Admit the wrapper as a full palette slice before offering automatic wrapping |

Flutter supplies gesture interaction through
[GestureDetector](https://api.flutter.dev/flutter/widgets/GestureDetector-class.html),
raw pointer interaction through
[Listener](https://api.flutter.dev/flutter/widgets/Listener-class.html),
mouse enter/exit/hover through
[MouseRegion](https://api.flutter.dev/flutter/widgets/MouseRegion-class.html), and
keyboard/focus interaction through
[Focus](https://api.flutter.dev/flutter/widgets/Focus-class.html), and bubbling notifications through
[NotificationListener](https://api.flutter.dev/flutter/widgets/NotificationListener-class.html).
Wrapping must remain explicit because it affects hit testing, gesture competition,
focus and widget-tree structure; it is not equivalent to adding an arbitrary
property to the selected widget.

GestureDetector, Listener, MouseRegion, Focus and NotificationListener are admitted constructor slices.
Their explicit **Wrap with...** actions preserve the selected subtree and stable
IDs; required-child wrappers also use atomic wrapping for an occupied palette
drop target. See the [GestureDetector](GESTURE_DETECTOR.md), [Listener](LISTENER.md),
[MouseRegion](MOUSE_REGION.md), [Focus](FOCUS.md) and
[NotificationListener](NOTIFICATION_LISTENER.md) implementation/verification contracts.

## Callable contracts

- Classify each callable explicitly as EVENT, BUILDER, PREDICATE, FORMATTER or DELEGATE.
  Function type or an `on` name prefix alone is insufficient.
- Preserve parameter types, nullability, return type and generic substitutions.
  A form without a type parameter must never receive an undeclared `T` in a stub.
- Preserve required, omitted, explicit-null and default/no-op behavior. For
  example, a null `Checkbox.onChanged` disables the control.
- A handler may be reused only when the Dart analyzer accepts its assignability
  to the selected event. A textual signature match is not sufficient evidence.
- Builders and value-producing predicates remain under Properties. They are not
  ordinary event notifications, even when Flutter calls them callbacks.

G1, `Scaffold.bottomSheetScrimBuilder`, follows that distinction: its row is in
**Properties → Appearance**, while Scaffold retains its two drawer Events.
Its exact type is `Widget? Function(BuildContext, Animation<double>)`, with a
non-null callback and nullable Widget result. Omission preserves the actual SDK
default; explicit callback null is invalid, while returning null suppresses the
scrim. The shared typed-reference editor supports project callables, getters,
static/instance members and zero-argument factories, not raw code or Events
Create/Rename actions. Generation retains strict exact-signature evidence and an
independent dynamic-safe result proof. Canvas does not invoke user code and
explicitly previews the SDK default as an approximation. See the
[G1 contract and verification status](SCAFFOLD_BOTTOM_SHEET_SCRIM_BUILDER.md).

G2, `AppBar.notificationPredicate`, is a **PREDICATE** under
**Properties → Behavior**. Existing `default`, `depthZero` and `all` presets keep
their exact representation and generated expressions; project references,
getters/members and zero-argument factories add no new property or Event. The
callback is non-null `ScrollNotificationPredicate`, returning bool for a
ScrollNotification. Omission uses the SDK depth-zero default; explicit null,
legacy CallbackValue and raw code are invalid. An independent SDK-qualified
bool-result witness rejects dynamic-return predicates and also hardens the
existing RefreshIndicator proof family. The predicate filters AppBar's
scroll-under processing without consuming notifications. Canvas executes the
reviewed presets only and explicitly approximates custom references with the
SDK default. See the [G2 contract](APP_BAR_NOTIFICATION_PREDICATE.md).

G3, ElevatedButton's `styleBackgroundBuilder/styleForegroundBuilder`, adds two
**BUILDER** rows to the common style section of Properties. The exact
ButtonLayerBuilder takes BuildContext, Set<WidgetState> and nullable Widget child,
and returns a non-null Widget. The SDK style fields are nullable; Designer
supports their omission/theme fallback through independent Reset and accepts
only non-null typed references/getters/members/factories when set. No explicit
null, raw code, new Event or State binding is introduced. The shared Material-SDK
proof now independently requires a Widget invocation result, also hardening the
other button families. Existing five-state ElevatedButton styles and all shared
families' property orders remain intact. Canvas never executes project builders;
see the [G3 scope and preview limitation](ELEVATED_BUTTON_LAYER_BUILDERS.md).

G4 adds TextField `buildCounter/contextMenuBuilder` as two ordinary **BUILDER**
rows under Properties → Builders, after the original 54 properties. Both support
explicit null and exact nullable typed references/getters/members/factories.
Counter omission/null uses the SDK counter policy; a callback returning null
hides the counter. Menu omission uses the SDK default, while null disables it.
The counter signature preserves required named currentLength, maxLength (int?)
and isFocused; requiredness is separate from nullability. Shared metadata retains
the old positional constructor and admits optional named parameters without
explicit defaults only when nullable. Exact SDK typedef proof plus an independent
original-expression null-aware Widget? result witness reject dynamic results
without executing user code; the typedef assignment independently rejects a
nullable Widget result from a non-null menu callback. Canvas preserves explicit
null and approximates custom references, including nullable ones, with isolated
SDK defaults and an explicit notice. These builders add no Event or State binding;
see the [G4 scope, null semantics and verification status](TEXT_FIELD_BUILDERS.md).

G5 adds `ListView.itemExtentBuilder` under Properties → Layout on the admitted
static constructor, retaining all previous 17 orders and children slot order 11.
The nullable ItemExtentBuilder? reference receives int and SDK Rendering-owned
SliverLayoutDimensions and returns double?. Omission/null uses normal sizing or
the configured itemExtent; any project reference conflicts with itemExtent,
including a nullable reference. Rejection identifies explicit Reset/Null choices
without clearing a field. Actual children require valid extents; null results
are only for out-of-range indexes, not truncation or default sizing. Exact SDK
typedef assignment and an independent double? invocation-result witness reject
incompatible/dynamic bindings without executing code. Canvas explicitly
approximates custom references with normal SDK child sizing. No Event/State or
dynamic-constructor expansion is added; see the
[G5 scope and verification status](LIST_VIEW_ITEM_EXTENT_BUILDER.md).

## Implemented Events actions

MenuAnchor exposes onOpen, onClose and onAnimationStatusChanged as native Events.
Its MenuAnchorChildBuilder remains a non-event Properties callable with exact
Widget(BuildContext, MenuController, Widget?) signature. The explicitly scoped
Create Menu Builder command inserts a fixed user-owned TextButton opener and
binds it in one analyzed paired edit; it cannot accept an arbitrary body, replace
an existing non-null builder, create a controller field or mutate menu children.
Ordinary Events create/rename rules are not widened to builders. Source edits,
collisions, Undo/Redo and save/reopen use the existing lexical/evidence gates.
There is no open-state producer or State consumer for the inert deprecated
anchorTapClosesMenu argument. See [MenuAnchor](MENU_ANCHOR.md).

MenuItemButton exposes onPressed, onHover and onFocusChange with exact native
signatures. Its background/foreground ButtonLayerBuilder references remain
ordinary Properties, as do display-only shortcut activators. Native activation
uses the SDK post-frame callback after pending focus updates; disabled activation
retains the user handler without emitting it. Only an explicitly reviewed Enabled
State consumer synthesizes conditional onPressed. Hover callbacks can still occur
while activation is disabled, whereas the inner focus-change callback is gated by
native enabled behavior. Callback bodies and FocusNode/controller lifecycle stay
project-owned. See [MenuItemButton](MENU_ITEM_BUTTON.md).

SubmenuButton adds five native Events: onHover, onFocusChange, onOpen, onClose
and onAnimationStatusChanged. Its two ButtonLayerBuilder references remain
ordinary Properties. Use root overlay and Animated are independent boolean State
consumers; opening/closing remains native controller state. The required
menuChildren list and nullable Child plus optional icon slots, whole/local
ButtonStyle/MenuStyle branches and four submenu-icon state buckets are preserved
atomically. See [SubmenuButton](SUBMENU_BUTTON.md).

The current catalog contains 104 admitted definitions, 6,227 properties and 171 callable
properties, of which 144 are native events, 22 are builders, 2 are predicates,
2 are formatters and 1 is a positioning delegate.
Native event rows appear once under Events; other properties
and callable builders/predicates/formatters/delegates remain under Properties. Slots retain
their separate tab where applicable. Widgets with no native events currently have
no Events tab; an explicit empty-state explanation remains a presentation gap.

G1–G5 close all known callable gaps within the **audited admitted-constructor
scope**; newest-slice acceptance is tracked in the linked contract. There are
33 callable widget types, but only 30 native-event-bearing types. This is
not full Flutter API or historical palette completion, and does not expand
TextField decoration/controller/style/formatter graphs, Scaffold's remaining
slots, ElevatedButton constructor/style variants or dynamic list/grid
constructors, or change the current
FD schema 16, Catalog API 15, Canvas model 19 or transport protocol 1.

SwitchListTile adds four events in both Standard and Adaptive: onChanged and
onFocusChange receive bool; the two image-error listeners receive Object and
nullable StackTrace. Required onChanged is stored as no-op, explicit null or a
strict ValueChanged<bool> reference, never bool?. Null disables activation.
Non-null image-error handlers require the corresponding declared asset provider.
The SDK forwards onFocusChange to the outer ListTile and inner Switch.
Its State producer controls value; six separate boolean consumers include the
retained, inactive-in-Standard applyCupertinoTheme binding. Callback bodies and
project FocusNode/controller lifecycle remain user-owned. See the
[full SwitchListTile contract](SWITCH_LIST_TILE.md).

RadioListTile adds optional onChanged<T?> and onFocusChange<bool> in both
Standard and Adaptive. A broad Object? generated stub safely accepts the
selected generic callback input; typed project callbacks additionally prove
that exact T? identity. Reset omits onChanged rather than recreating its
creation no-op. Modern RadioGroup notification occurs before the optional
legacy callback; the outer ListTile alone receives onFocusChange. Legacy
groupValue State and eight reviewed boolean consumers remain independent of
the option value and selected appearance. Enabled still has the SDK's runtime
activation requirement. See [RadioListTile](RADIO_LIST_TILE.md).

ExpansionTile adds optional onExpansionChanged(bool isExpanded), notified when
expansion starts through the header or controller. Omission/null does not disable
expansion. Callback creation, binding, rename and disconnect preserve user-owned
source and history. Its initiallyExpanded argument is a seed read at initialization,
not a controlled State value or consumer; six separate runtime boolean consumers
are reviewed, without adding a producer. ExpansibleController ownership remains
with the user when supplied. Shape/AnimationStyle objects remain Properties, not
Events; the pinned SDK ignores reverseDuration despite preserving its exact source.
See [ExpansionTile](EXPANSION_TILE.md).

Tooltip adds optional onTriggered() as its sole native Event. The pinned SDK
invokes it for accepted tap/long-press triggers, not hover or ensureTooltipVisible().
TooltipPositionDelegate is a separate DELEGATE classification with exact
Offset Function(TooltipPositionContext) signature: it remains Properties and
does not acquire Event create/rename actions. Strict references and independent
analyzer evidence preserve the expected types without executing user code.
Exactly one non-null Message/Rich message remains required; custom InlineSpan
trees are source-owned, not a newly invented local span-tree wire format.
Five boolean consumers do not create a visibility State producer.
See [Tooltip](TOOLTIP.md).

TooltipVisibility adds no callable property or Event. Its required visible
boolean is an inherited policy with nearest-scope precedence and one reviewed
State consumer; the required Child is wrapped atomically. It does not acquire
Tooltip's onTriggered callback or become a controlled visibility producer.
The shared State inventory is 168 consumers across 55 types. See
[TooltipVisibility](TOOLTIP_VISIBILITY.md) for the pinned RawTooltip/overlay and
semantics lifecycle boundary.

TooltipTheme adds no callable property or Event. Whole TooltipThemeData is a
strict non-null object reference, not a handler, and is exclusive with all 46
local fields including explicit null and State bindings. The three reviewed
nullable boolean consumers are rendered inside TooltipThemeData, not on the
outer widget. Empty local mode constructs fresh const data; nearest themes
replace rather than merge. No copyWith/lerp operation or State producer is
introduced. See [TooltipTheme](TOOLTIP_THEME.md).

The shared event editor displays the callback signature and current binding,
including omitted, explicit-null, no-op and disabled-widget states. It provides
Form methods and Reference pages, plus a State binding page for the reviewed
control actions described in [State/value bindings](STATE_VALUE_BINDINGS.md).

| Action | Implemented behavior and boundary |
| --- | --- |
| Create Handler | Submit one typed create-and-bind command; insert the typed method outside generated regions and bind the selected event only after candidate analysis accepts the complete source/model change. Stay in Designer; do not navigate or save automatically. |
| Bind Selected | Offer direct user-owned methods of the verified member-owner class as source candidates. Recheck the selected method against current source, then let the Dart analyzer decide callback assignability before applying the binding. |
| Apply Reference | Preserve the existing typed reference editor, including imported and qualified callables. Draft edits have no effect until Apply Reference; applying uses the same analyzed mutation path. |
| Go to Handler | Explicit read-only navigation to the bound direct method in the verified member-owner class, using an exact-source UTF-16 location. A staged pair does not require an intermediate Save. External/qualified references require ordinary Dart source navigation. |
| Rename Handler | Submit one typed rename-and-rebind command for a direct user-owned member-owner method. Reject ambiguous or unowned source changes instead of performing a guessed text replacement. Stay in Designer; do not navigate or save automatically. |
| Disconnect | Reset an optional callback, or restore the reviewed disconnected value for a required callback. Retain the user method and do not change Enabled. |

Source candidates are lexical discoveries, not analyzer-verified recommendations.
An inherited, top-level, imported or qualified callable can still be entered
through Reference when the property's typed contract permits it. It is not
silently treated as an owned method for Go to Handler or Rename Handler.

Actions retain the selected revision and widget identity, reject stale editors
and asynchronous results, and preserve stable property rows during refresh.
Constructor-specific callbacks and required image-provider companions are checked
without silently changing other widget properties. For example,
`RefreshIndicator.onStatusChange` requires the `noSpinner` constructor.

## Source and persistence safety

The `.fd` model owns event bindings. User Dart handler bodies belong outside
managed generation regions. Disconnecting an event never deletes its method.
Layout regeneration must preserve handler bodies byte-for-byte.

Create-and-bind and rename-and-rebind must enter the existing analyzed paired
source/model transaction as single commands. They must also participate in
Undo/Redo, save/reopen, stale-revision rejection and external-source conflict
checks. Do not implement them as an editor insertion followed by a separate
property mutation, or relax existing unmanaged-source checks to make them pass.

`CreateEventHandler` and `RenameEventHandler` now carry the event operation through
the command session and paired staging path. A bounded user-source transition
proof is retained by the command revision and source transition plan. Ordinary
unmanaged-source protection remains in force outside the exact admitted edit.
The UI does not insert source directly or run a second independent binding edit.

### Current Save boundary

The supported sequence is:

1. Create, bind, rename or disconnect in Events.
2. Use Go to Handler and edit the handler body in Source immediately.
3. Save once: analyze the current Source and commit the exact source/model pair.
4. Continue editing in Designer.

No intermediate Save is required before Go to Handler. Create/Rename stay in
Designer; navigation is explicit and no event action performs an autosave.

For a staged pair with manually edited Source, Save captures the exact live
document identity/version and derives a bounded observed-source projection.
Generated sections, including marker bytes, must remain unchanged. The full
candidate is analyzed again; only a passing result bound to that same live
document can replace the staged physical pair. This does not add a logical
command or a native undo edit. Native Source edits and semantic Designer edits
remain separate chronological Undo/Redo steps.

Analysis failure, a concurrent Source edit, external file changes, or disposal
prevents the pair write and leaves the user's live text intact. Analysis is
cancelled when its authority becomes stale. An EDT Save caller uses a secondary
event loop while analysis runs off the EDT; the analyzer does not block editor
callbacks needed for exact admission. Existing transaction recovery conflicts
are not bypassed by restaging.

This slice supports Source edits before the first Save, not arbitrary Designer
mutations over unanalysed manual Source. Save those edits before making another
Designer mutation. Typed state/value binding is covered by the subsequent
[State/value binding slice](STATE_VALUE_BINDINGS.md).

For Stateful forms the verified member owner is the associated `State<Widget>`
class, not the immutable widget class. Discovery, creation, rename, navigation and
source projections all use scanner-owned evidence for this same actual class.
See [Stateful forms](STATEFUL_FORMS_IMPLEMENTATION.md) for creation and the
supported source shapes.

## State and Canvas behavior

Event binding alone does not provide value/state binding. Flutter's
[Checkbox.onChanged contract](https://api.flutter.dev/flutter/material/Checkbox/onChanged.html)
requires the owner to supply the changed value on rebuild. Editable state fields,
verified `State<Owner>` source ownership and `setState` support are required parts
of interactive form authoring, not implicit capabilities of an empty handler.
The Stateful ownership/template prerequisite is now implemented: user-written
fields and synchronous `setState` handler bodies are preserved outside generated
regions. The subsequent [State/value binding slice](STATE_VALUE_BINDINGS.md)
adds typed fields and updating handlers for nine reviewed control families,
shared fields and dependent-property transforms. Currently bound fields support
explicit Go to Field and one-command whole-form Rename State Field, including
owned TextEditingController listeners. This does not introduce arbitrary
expressions or make every event a state-update event.

Designer Canvas must not execute arbitrary user event handlers. Run/Debug owns
application event execution. Tests must distinguish Canvas preview behavior from
real runtime behavior and must not label a stub or successful compile as an
end-to-end event test.

## Verification entry points

The catalog and source-operation tests are ordinary Java unit tests.
`EventHandlerCommandSessionTest` covers atomic command admission and history;
`DartSourceTransitionPlannerTest` covers bounded source transition proof behavior.
`EventCallbackSdkContractTest` is an additional opt-in SDK contract check. It
creates an isolated temporary Flutter project, resolves dependencies offline,
inserts every inventoried stub into a class, and asks the real Dart analyzer to
check assignments to the corresponding Flutter typedefs. It does not execute
callbacks or modify the user's Flutter project.

From the repository root (PowerShell), run:

```powershell
mvn -pl flutter-designer -am "-Dtest=WidgetEventCatalogTest,DartEventHandlerSourceTest,EventCallbackSdkContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dflutter.events.sdk=G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter" test
```

Without `flutter.events.sdk`, the SDK test is explicitly skipped. An SDK
dependency-cache failure is a failed prerequisite, not permission to bypass the
analyzer. The UI contract suite is `FlutterWidgetEventsTest`, alongside the
existing `FlutterWidgetPropertiesNodeTest` regression suite.
`FlutterDesignerEventsBridgeTest` covers all native-event command dispatch,
candidate rechecks, reference preservation, disconnect semantics, stale context,
navigation scope and navigation without an intermediate Save.
`FlutterDesignerMutationControllerIntegrationTest` and
`LiveDartDocumentBridgeTest` cover the paired staging/live-source boundary.
`EventHandlerCommandSdkContractTest` adds an opt-in real-SDK atomic-command check.
Passing these checks does not imply State/value binding or arbitrary interleaved
unsaved Source/Designer mutations are implemented.

### Atomic event action verification — 2026-09-08 (before Source-restage follow-up)

- Full `flutter-designer` unit suite: 1,763 tests, no failures, errors or skips,
  including the opt-in Flutter 3.44.8 SDK checks.
- Full mutation-controller integration suite: 144 tests passed; atomic
  live-document bridge suite: 21 tests passed.
- Final targeted plugin verification after physical history proof accounting
  and navigation updates: 325 tests passed; packaged NBM metadata: 9 tests passed.
- The SDK command check analyzes 32 complete candidates across eight event
  contracts, including disconnect and widget removal. Retained user methods and
  signature imports can produce unused-element or duplicate-import warnings;
  these are not reported as a warning-free analyzer run.
- `verify`, local Maven `install`, and `nbm:cluster` completed in the canonical
  G: checkout. The resulting NBM and development cluster include the Events UI
  and command/source-proof classes. No running IDE was restarted, and physical
  interactive IDE behavior was not verified in this run.

Logical command revisions and retained physical Source-history variants both
count their source-proof arrays against the existing history byte limit. The
aggregate-budget tests continue to reject before analyzer/CES/transaction work;
the production limits were not increased.

### Source-restage follow-up verification — 2026-09-08

- Full `flutter-designer` suite: 1,772 tests passed, no skips, including real
  Flutter 3.44.8 SDK checks.
- Broad plugin regression run: 485 tests passed, including all 149 then-current
  mutation-controller integration tests, pair-save/Source/Undo regression suites,
  51 command-orchestrator tests and the updated Events UI/bridge tests.
- Two additional isolated integration tests passed: external `.fd` change during
  stalled analysis preserves conflict and live user text; a first-saved handler
  body reopens in a fresh DataObject as an editable exact pair. This brings the
  covered mutation-controller tests to 151 and the covered plugin tests to 487.
- Covered mixed workflow: Create -> edit body -> first Save from EDT -> native
  Source Undo -> semantic Undo -> both Redos -> next property edit and Save.
  Rejected analysis permits retry; stale Source and disposal cancel pending
  analysis without writing either paired file. Transaction failure retains the
  user body and the existing recovery guard.
- `verify` passed, including all nine packaged NBM metadata tests. Local Maven
  `install` completed. Both development clusters (`target/netbeans_clusters`
  and `netbeans-plugin/target/netbeans_clusters`) were rebuilt from the canonical
  G: checkout. All 1,208 module classes match the built JAR and NBM; both clusters'
  `flutter-designer.jar` matches the build byte-for-byte. NBM size: 7,954,757 bytes.
  A running IDE is not automatically restarted or hot-reloaded.
- Physical interactive IDE behavior is not claimed as verified by these tests.
