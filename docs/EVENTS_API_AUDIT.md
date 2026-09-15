# Widget events API audit

## ShaderMask update (2026-09-15)

ShaderCallback is a required nonnull computation delegate, Shader Function(Rect).
It belongs to Properties, not Events. Local gradients and exact source references
support the existing handler actions. Create/disconnect use an opaque-white
gradient rather than throwing or passing null. Static proof uses Flutter's
Rendering ShaderCallback alias. Nine invalid signature/type branches fail closed.

Inventory: 217 definitions; 240 callables across 88 types (50 builders, eleven
delegates). Native Events remain 174 rows across 60 types.
See [ShaderMask](SHADER_MASK.md).

## BackdropFilter and grouping update (2026-09-15)

BackdropFilter, BackdropFilter.grouped and BackdropGroup expose no native
callback parameters. Filtering, clipping, enabled state and grouping do not
replace Child input/semantics. Config/source references are typed object
properties, not invented Events.

Inventory: 216 definitions; 239 callables across 87 types (50 builders, ten
delegates); native Events remain 174 rows across 60 types.
See [BackdropFilter](BACKDROP_FILTER.md).

## ImageFiltered update (2026-09-14)

ImageFiltered has no callback parameters. Its filter affects pixels while Child
retains layout, input and semantics. Enabled changes compositing, not event
wiring. All six factories, including source-owned composition and shader, are
covered without inventing Events or executing project code in Canvas.

Inventory: 213 definitions; 239 callables across 87 types (50 builders, ten
delegates); native Events remain 174 rows across 60 types.
See [ImageFiltered](IMAGE_FILTERED.md).

## ColorFiltered update (2026-09-14)

ColorFiltered has no callback constructor parameters. Its filter transforms
child pixels; layout, hit testing and semantics remain owned by the child.
All five local filter families and strict non-null ColorFilter source overrides
are supported. No synthetic Events or project-code execution in Canvas.

Inventory: 212 definitions; 239 callables across 87 types (50 builders, ten
delegates); native Events remain 174 rows across 60 types.
See [ColorFiltered](COLOR_FILTERED.md).

## RawImage update (2026-09-14)

All 16 constructor parameters are Properties. Decoded Image, Rect and nullable
Animation<double> references are values, not Events; no callbacks are invented.
The same interaction wrappers remain available. The image witness explicitly
uses dart:ui.Image, preventing confusion with the Flutter Widget Image.
Inventory: 211 definitions; 239 callables across 87 types (50 builders, ten
delegates); native Events remain 174 across 60 types.
See [RawImage](RAW_IMAGE.md).

## FadeInImage update (2026-09-14)

Both optional ImageErrorWidgetBuilder? callbacks are Properties builders:
BuildContext, Object error, StackTrace? -> Widget. The existing create/bind/
navigate/rename/disconnect lifecycle applies. They are not native Events.
Local and typed ImageProvider<Object> values never execute project code in
Canvas. Exact analyzer proofs reject nullable/dynamic providers, raw dynamic
provider keys, wrong callback signatures and dynamic callback results.
Inventory: 210 definitions, 239 callables across 87 types (50 builders, ten
delegates); native Events remain 174 across 60 types.
See [FadeInImage](FADE_IN_IMAGE.md).


## AnimatedIcon update (2026-09-14)

All six constructor inputs are Properties; there are no native event callbacks.
Animation<double> is a source value, not an Event. Existing interaction wrappers
remain available. No onPressed, onEnd, Duration or Curve is invented.
Inventory: 209 definitions; Events unchanged at 174 / 60 types, callables 237 /
86 types. See [AnimatedIcon](ANIMATED_ICON.md).


## AnimatedModalBarrier update (2026-09-14)

On dismiss is an optional nullable native Event with create/bind/navigate/rename/
disconnect and analyzed source persistence. Color is Animation<Color?>, not an
Event; Clip details notifier remains a typed project object. Dismissible false
retains the callback; unset/null restores Navigator.maybePop when dismissal is
enabled. Canvas suppresses runtime effects.
Inventory: 208 definitions; Events 174 / 60 types; callables 237 / 86 types.
See [AnimatedModalBarrier](ANIMATED_MODAL_BARRIER.md).


## ModalBarrier update (2026-09-14)

On dismiss is an optional nullable VoidCallback? native Event. Null/unset uses
Navigator.maybePop only when Dismissible is true; a bound handler owns dismissal.
Create/bind/navigate/rename/disconnect preserve user source. Clip details notifier
is a ValueNotifier<EdgeInsets>? property, not an Event. Canvas suppresses callback,
route and sound actions while exported Dart retains native behavior.
Inventory: 207 definitions; Events 173 / 59 types; callables 236 / 85 types;
48 builders and 10 delegates. See [ModalBarrier](MODAL_BARRIER.md).

## MatrixTransition update (2026-09-14)

onTransform is a required non-null TransformCallback computation delegate:
Matrix4 Function(double animationValue). It belongs in Properties, not Events.
Create Handler supplies an identity matrix return; binding, navigation, rename
and disconnect use analyzed transactions and preserve user-owned method code.
Animation is a value source, not an event. No duration, curve or onEnd is invented.

Inventory: 206 definitions. Native Events remain 172 rows / 58 types.
Callables total 235 / 84 types, including 48 builders and 10 delegates.
See [MatrixTransition](MATRIX_TRANSITION.md).

## AlignTransition update (2026-09-14)

The complete native constructor exposes alignment animation, required Child and
optional width/height factors. There are no native event callbacks or implicit
timing controls. Local stopped Animation<AlignmentGeometry> and typed project
animations, including physical/directional covariance, are supported. Factors
also support typed double? references. Canvas does not execute project code.
Totals remain 172 Events / 58 widget types, 234 callables / 83 types, 48 builders.
See [AlignTransition](ALIGN_TRANSITION.md).

## DecoratedBoxTransition update (2026-09-14)

Flutter 3.44.8 exposes required Animation<Decoration> and Child, plus Position.
There is no onEnd or other native event. Local BoxDecoration is emitted through
AlwaysStoppedAnimation<Decoration>; project animation references remain
strictly analyzer-verified. ShapeDecoration/custom decorations are supported
through project animations. Canvas never executes project-owned code.
Totals remain 172 Events / 58 widget types, 234 callables / 83 types, 48 builders.
See [DecoratedBoxTransition](DECORATED_BOX_TRANSITION.md).

## RelativePositionedTransition update (2026-09-14)

All three inputs are supported: Animation<Rect?>, reference Size and Child.
Animation controllers/tweens are source-owned rather than callable Event fields.
The widget adds no Duration, Curve, onEnd, textDirection or hit-test parameter.
Inventory: 203 definitions. Events remain 172 rows / 58 types, callables 234 / 83
types and builders 48. See [RelativePositionedTransition](RELATIVE_POSITIONED_TRANSITION.md).

## PositionedTransition update (2026-09-14)

The constructor has required Animation<RelativeRect> and Child, with shared Key
identity. No callbacks/builders are exposed; controller/tween/listener lifetime
stays in source Dart. Duration, Curve, On end and text direction are not invented.
Inventory: 202 definitions. Events remain 172 rows / 58 types, callables 234 / 83
types and builders 48. See [PositionedTransition](POSITIONED_TRANSITION.md).


## SizeTransition update (2026-09-14)

SizeTransition exposes no constructor callbacks/builders. Its required sizeFactor
is Animation<double>; controller/listener lifetime stays in source Dart.
All five scalar arguments, including deprecated axisAlignment, are projected.
No Duration, Curve, On end or clipping/hit-test control is invented.
Inventory: 201 definitions; Events remain 172 rows / 58 types, callables 234 / 83
types and builders 48. See [SizeTransition](SIZE_TRANSITION.md).


## RotationTransition update (2026-09-14)

RotationTransition exposes no constructor callbacks/builders. Its required turns
is Animation<double>; controller/status-listener lifecycle stays in source Dart.
The inherited onTransform function is fixed by the constructor, not an event.
No Duration, Curve, On end or transformHitTests constructor argument is invented.
Inventory: 200 definitions; Events stay 172 rows / 58 types, callables 234 / 83
types and builders 48. See [RotationTransition](ROTATION_TRANSITION.md).

## ScaleTransition update (2026-09-14)

ScaleTransition exposes no constructor callbacks/builders. Its required scale
is Animation<double>; controller/status-listener lifecycle remains source-owned.
The inherited onTransform field is bound internally by the constructor, not a
customizable event. No Duration, Curve or On end is invented.
Inventory: 199 definitions; Events stay 172 rows / 58 types, callables 234 / 83
types and builders 48. See [ScaleTransition](SCALE_TRANSITION.md).

## SlideTransition update (2026-09-14)

The constructor has no callbacks or builders. Required Animation<Offset>
position is a value, not an Event; controller/status/listener lifetime stays
source-owned. No Duration, Curve or On end is invented. Inventory is 198
definitions; Events remain 172 rows / 58 types, callables 234 / 83 types and
builders 48. See [SlideTransition](SLIDE_TRANSITION.md).

## Fade transitions update (2026-09-14)

FadeTransition and SliverFadeTransition have no constructor callbacks or builders.
Opacity is a required Animation<double> value, not an Event; controller lifecycle
and animation status listeners remain source-owned. No On end, Duration or Curve
is invented. Inventory is 197 definitions; Events stay 172 rows / 58 types,
callables 234 / 83 types and builders 48. See [Fade transitions](FADE_TRANSITION.md).

## DefaultTextStyleTransition update (2026-09-14)

The constructor has no callbacks/builders. Its Animation<TextStyle> style value
is not an Event; status listeners and controller disposal remain source-owned.
There is no On end, Duration or Curve argument. Inventory: 195 definitions;
Events remain 172 rows across 58 types, 234 callables across 83 types and 48
builders. See [DefaultTextStyleTransition](DEFAULT_TEXT_STYLE_TRANSITION.md).

## DefaultTextStyle update (2026-09-14)

DefaultTextStyle and its static merge method have no callbacks or builders.
The internal SDK Builder used by merge is not a public callback property.
Style, nullable Max lines and TextHeightBehavior are typed values, not Events.
Inventory: 194 definitions; Events remain 172 rows across 58 types, 234
callables across 83 types and 48 builders. See [DefaultTextStyle](DEFAULT_TEXT_STYLE.md).

## Theme update (2026-09-14)

Theme has no constructor callbacks or builders. Required ThemeData is an object
value with material.dart type proof, not an Event. Do not invent On end,
Duration or Curve properties from AnimatedTheme.
Inventory: 192 definitions; native Events remain 172 rows across 58 types;
234 callables across 83 types and 48 builders. See [Theme](THEME.md).

## AnimatedTheme update (2026-09-14)

Native optional nullable VoidCallback On end uses the shared source lifecycle.
Completion does not fire on initial mount or an unchanged target theme.
ThemeData is a typed object reference, not a builder or Event; its proof must
import material.dart. Duration is optional (200 ms SDK default), not required.
Current inventory: 191 definitions, 172 Event rows across 58 types,
234 callables across 83 types; builder count remains 48.
See [AnimatedTheme](ANIMATED_THEME.md).

## AnimatedSwitcher update (2026-09-14)

Two optional non-null builders, no new Event rows:

- AnimatedSwitcherTransitionBuilder: Widget Function(Widget child, Animation<double> animation).
- AnimatedSwitcherLayoutBuilder: Widget Function(Widget? currentChild, List<Widget> previousChildren).

Both accept typed references/factories and generate editable user-owned methods.
Analyzer witnesses consume the original call result with exact Flutter/core
arguments; wrong signatures and void/dynamic results are rejected. Canvas does
not execute these methods. Current inventory: 190 definitions, 171 Event rows
across 57 types, 233 callables across 82 types, including 48 builders.
See [AnimatedSwitcher](ANIMATED_SWITCHER.md) for the pinned SDK key limitation.

## AnimatedCrossFade update (2026-09-13)

Native nullable VoidCallback onEnd fires on completed/dismissed transitions, not
initial mount. Non-null optional AnimatedCrossFadeBuilder receives
(Widget topChild, Key topChildKey, Widget bottomChild, Key bottomChildKey).
Its editable generated scaffold delegates to AnimatedCrossFade.defaultLayoutBuilder,
preserving both keyed subtrees. A strict proof-only invocation checks the original
return type; Canvas never executes project handlers/builders.
Current inventory: 189 definitions, 171 Event rows across 57 types,
231 callables across 81 types (46 builders).
See [AnimatedCrossFade](ANIMATED_CROSS_FADE.md).


## AnimatedFractionallySizedBox update (2026-09-13)

Native nullable VoidCallback onEnd uses the shared Events/source lifecycle.
No event is invented for initial mount or an unchanged target. Canvas never
executes project handlers. Generated forms retain the real callback reference.

Current inventory: 188 definitions, 170 Event rows across 56 types,
229 callables across 80 types.
See [AnimatedFractionallySizedBox](ANIMATED_FRACTIONALLY_SIZED_BOX.md).


## AnimatedPhysicalModel update (2026-09-13)

Native nullable VoidCallback onEnd uses shared Events/source ownership.
The SDK tracks both color tweens even when their animation flags are false:
a changed color can therefore complete a transition while its visual change
is immediate. Initial mount, unchanged targets and shape/clip/flag-only edits
do not start a transition. Canvas handlers remain inert. Current catalog:
187 definitions, 169 Event rows across 55 types, 228 callables across 79 types.
See [AnimatedPhysicalModel](ANIMATED_PHYSICAL_MODEL.md).


## AnimatedDefaultTextStyle update (2026-09-13)

Native nullable VoidCallback onEnd uses the shared Events/source lifecycle.
Initial mount and unchanged style do not complete a transition; paragraph changes
are immediate. Interrupted transitions complete only at their replacement target.
Canvas never executes project handlers. Current catalog: 186 definitions,
168 native Event rows across 54 types, 227 callable rows across 78 types.
See [AnimatedDefaultTextStyle](ANIMATED_DEFAULT_TEXT_STYLE.md).


## AnimatedPositioned update (2026-09-13)

Ordinary, fromRect and directional constructors each expose native nullable
VoidCallback onEnd. Shared Events editing/source proof applies to all three;
Canvas handlers remain inert. Current catalog: 185 definitions; 167 native Event
rows across 53 types, 226 supported callable rows across 77 types. Initial and
unchanged targets emit no completion; interrupted targets complete only at the
replacement target. See [AnimatedPositioned](ANIMATED_POSITIONED.md).

## AnimatedSize update (2026-09-13)

AnimatedSize adds its native nullable VoidCallback onEnd. No invented events:
completion handlers use the shared Events/source lifecycle and stay inert in
Canvas. Current catalog: 182 definitions; 164 native Event rows across 50 types,
223 supported callable rows across 74 types. Historical counts below are snapshots.


Audit date: 2026-09-09. Baseline: Flutter 3.44.8; 104 admitted built-in Designer definitions. This audit concerns callable widget constructor parameters and the already exposed flattened ButtonStyle callbacks. It does not equate the current catalog with the entire Flutter widget API or completion of the historical 92-widget target. Automated acceptance and remaining manual checks for the interaction wrappers are recorded in [GestureDetector](GESTURE_DETECTOR.md), [Listener](LISTENER.md), [MouseRegion](MOUSE_REGION.md), [Focus](FOCUS.md) and [NotificationListener](NOTIFICATION_LISTENER.md).

## Latest palette admission: AnimatedContainer

AnimatedContainer adds native onEnd: VoidCallback?. Eight native tweens complete
through one animation controller; no initial/unchanged/clip-only event. Null
properties do not animate. Total: 181 definitions, 222 callables across 73 types;
163 Events across 49 types, 45 builders, three predicates, two formatters and nine
delegates. Canvas handlers are inert. See [AnimatedContainer](ANIMATED_CONTAINER.md).

## Previous palette admission: AnimatedRotation

AnimatedRotation adds optional native `onEnd: VoidCallback?`. Full revolutions
are real transitions even when their final painted orientation matches the start.
No event on initial mount, unchanged turns or alignment/filter-only changes.
Total: 180 definitions, 221 callables across 72 types; 162 Events across 48 types,
45 builders, three predicates, two formatters and nine delegates.
Canvas keeps project handlers inert. See [AnimatedRotation](ANIMATED_ROTATION.md).

## Previous palette admission: AnimatedScale

AnimatedScale adds optional native `onEnd: VoidCallback?`. Only completed scale
transitions emit it: no initial, unchanged-target or alignment/filter-only event.
Interrupted transitions complete at the replacement target. Total: 179 definitions,
220 callables across 71 types; 161 Events across 47 types, 45 builders, three
predicates, two formatters and nine delegates. Canvas never executes handlers.
See [AnimatedScale](ANIMATED_SCALE.md).

## Previous palette admission: AnimatedSlide

AnimatedSlide adds the native optional `onEnd: VoidCallback?` Event. Initial
mount and unchanged targets do not emit it; interrupted transitions complete
at the replacement target. Total: 178 definitions, 219 callables across 70 types;
160 Events across 46 types, 45 builders, three predicates, two formatters and
nine delegates. Canvas keeps project callbacks inert.
See [AnimatedSlide](ANIMATED_SLIDE.md).

## Previous palette admission: AnimatedPadding

AnimatedPadding adds the optional native `onEnd: VoidCallback?` Event. Initial
mount and unchanged padding do not emit it; interrupted transitions complete
only at the new target. Total: 177 definitions, 218 callables across 69 types;
159 Events across 45 types, 45 builders, three predicates, two formatters and
nine delegates. Canvas does not execute project callbacks.
See [AnimatedPadding](ANIMATED_PADDING.md).

## Previous palette admission: AnimatedAlign

AnimatedAlign adds the optional native `onEnd: VoidCallback?` Event, with
completion rather than initial-mount semantics. Inventory: 176 definitions,
217 callables across 68 types; 158 Events across 44 types, 45 builders, three
predicates, two formatters and nine delegates. Project code remains inert in
Canvas. See [AnimatedAlign](ANIMATED_ALIGN.md).

## Previous palette admission: AnimatedOpacity

AnimatedOpacity adds the native optional `onEnd: VoidCallback?` Event. Initial
mount and unchanged targets do not emit it. Total: 175 definitions, 216 callables
across 67 types; 157 Events across 43 types, 45 builders, three predicates, two
formatters and nine delegates. Canvas never executes application handlers.
See [AnimatedOpacity](ANIMATED_OPACITY.md).

## Previous palette admission: TweenAnimationBuilder

Both placements add required ValueWidgetBuilder<T> (BUILDER) and optional
VoidCallback onEnd (EVENT). Tween<T> is an owned mutable object, not an Event.
There is no invented value-change callback. Both source/builder proofs use
the same selected T. Total: 174 definitions, 215 callables across 66 types:
156 Events, 45 builders, three predicates, two formatters and nine delegates.
Native Events cover 42 types. See [TweenAnimationBuilder](TWEEN_ANIMATION_BUILDER.md).

## Previous palette admission: ValueListenableBuilder

Two box/sliver placements add required ValueWidgetBuilder<T>:
Widget Function(BuildContext, T, Widget?). It is BUILDER, not EVENT.
ValueListenable<T> is an application-owned source. Both references share the
independently proven selected T, including its nullability.
Current total: 172 definitions, 211 callables across 64 catalog types:
154 Events, 43 builders, three predicates, two formatters and nine delegates.
Native Events remain 154 across 40 types.
See [ValueListenableBuilder](VALUE_LISTENABLE_BUILDER.md).

## Previous palette admission: AnimatedBuilder

Two fixed box/sliver projections add required TransitionBuilder:
Widget Function(BuildContext, Widget?). Builder is not EVENT, and animation is
a typed Listenable source, not an onEnd producer or an Animation<double>-only field.
Current total: 170 definitions, 209 callables across 62 catalog types:
154 Events, 41 builders, three predicates, two formatters and nine delegates.
Native Events remain 154 across 40 types. See [AnimatedBuilder](ANIMATED_BUILDER.md).

## Previous palette admission: ListenableBuilder

Two box/sliver placement projections add required TransitionBuilder with exact
Widget Function(BuildContext, Widget?). It is BUILDER, not EVENT; Listenable is
a separately typed source object. Both required fields reject omission/null.
Current total: 168 definitions, 207 callables across 60 catalog types:
154 Events, 39 builders, three predicates, two formatters and nine delegates.
Native Events remain 154 across 40 types. See [ListenableBuilder](LISTENABLE_BUILDER.md).

## Previous palette admission: DeviceOrientationBuilder

Two explicit box/sliver placement projections share the same unnamed native
constructor and required OrientationWidgetBuilder. Both are BUILDER, not EVENT,
with Widget Function(BuildContext, Orientation); omission/null are rejected.
There are 166 definitions and 205 callable rows across 58 catalog types:
154 Events, 37 builders, three predicates, two formatters and nine delegates.
The two new definitions represent one native constructor, not two SDK variants.
Native Events remain 154 across 40 types. See [DeviceOrientationBuilder](DEVICE_ORIENTATION_BUILDER.md).

## Previous palette admission: OrientationBuilder

Required OrientationWidgetBuilder is BUILDER, not EVENT, with the exact
Widget Function(BuildContext, Orientation) signature. Unset/null are rejected.
There are 164 definitions and 203 callables across 56 types: 154 Events,
35 builders, three predicates, two formatters and nine delegates.
Native Events remain 154 across 40 types. See [OrientationBuilder](ORIENTATION_BUILDER.md).

## Previous palette admission: LayoutBuilder

The required LayoutWidgetBuilder is classified as BUILDER, not EVENT.
It returns Widget from BuildContext and BoxConstraints; omission/null are rejected.
The catalog now has 163 definitions and 202 callables across 55 types:
154 Events, 34 builders, three predicates, two formatters and nine delegates.
Native Events remain 154 across 40 types. See [LayoutBuilder](LAYOUT_BUILDER.md).

## Previous palette admission: FlexibleSpaceBarSettings

All six values and required box Child are admitted; no native callbacks exist.
The catalog now has 162 definitions. Native Events and callable totals remain
unchanged. See [FlexibleSpaceBarSettings](FLEXIBLE_SPACE_BAR_SETTINGS.md).

## Previous palette admission: FlexibleSpaceBar

FlexibleSpaceBar has no native constructor callbacks. All five properties and
both child slots are supported; the catalog now has 162 definitions. Event and
callable totals below are unchanged. See [FlexibleSpaceBar](FLEXIBLE_SPACE_BAR.md).

## Previous event-bearing admission: SliverAppBar

All three SliverAppBar constructors add the native `onStretchTrigger` Event,
with `AsyncCallback` / `Future<void> Function()` signature. Null and omission
disable the trigger; project handlers stay source-owned. The current complete
catalog has 162 definitions, 154 native Events across 40 types, 33 builders,
3 predicates, 2 formatters and 9 delegates: 201 callables across 54 types.
See [SliverAppBar](SLIVER_APP_BAR.md) for the constructor, Canvas and source proof.
The dated 104-definition tables below are historical audit snapshots.

## Evidence and method

The implementation inventory is `WidgetEventCatalog`, checked against all properties in `BuiltInWidgetCatalog`. Legacy `CALLBACK` values and typed `DART_OBJECT_REFERENCE` constraints are both included. Classification uses explicit reviewed widget/property entries, never just an `on...` prefix: for example `TextField.onTapAlwaysCalled` is a boolean, and `Switch.thumbColor` is a state-property object, not a callback.

API evidence was read from the installed official SDK under `G:/MyProjects/java/libraries/flutter_windows_3.44.8-stable/flutter/packages/flutter/lib/src`. Widget class declarations, callable fields and constructors were examined, including the inherited `material/button_style_button.dart` declarations and the alternate constructors in `widgets/scroll_view.dart`. The per-widget links below lead to the official class documentation; online documentation can move beyond the pinned SDK, so the pinned SDK signature is authoritative for this release.

Representative online signature checks: [ImageFrameBuilder](https://api.flutter.dev/flutter/widgets/ImageFrameBuilder.html), [ButtonLayerBuilder](https://api.flutter.dev/flutter/material/ButtonLayerBuilder.html), [RadioGroup.onChanged](https://api.flutter.dev/flutter/widgets/RadioGroup/onChanged.html), and [TextField.onAppPrivateCommand](https://api.flutter.dev/flutter/material/TextField/onAppPrivateCommand.html).

## Auditable totals

Current palette totals are 104 admitted definitions and 6,227 writable properties, including
MenuAnchor, MenuItemButton, SubmenuButton, MenuBar, TooltipTheme, TooltipVisibility, Tooltip, ExpansionTile and complete Standard/Adaptive SwitchListTile
and RadioListTile slices. MenuAnchor adds three native Events and one child builder;
MenuItemButton adds three native Events and two style builders.
TooltipTheme and TooltipVisibility have no callable parameters; the
closed G1–G5 callable gap list is not reopened.

| Measure | Current inventory |
|---|---:|
| Admitted palette definitions audited | 104 |
| Definitions with directly bindable events | 30 |
| Definitions without direct event parameters | 74 |
| Event properties | 144 |
| Builder properties | 22 |
| Predicate properties | 2 |
| Formatter properties | 2 |
| Position delegate properties | 1 |
| All currently supported callable properties | 171 |
| Definitions with any supported callable property | 33 |
| Known missing callable surfaces on already admitted constructors | 0 within this bounded audit |
| Missing direct EVENT parameters found for admitted constructors | 0 |

The last row applies to constructor event callbacks, not arbitrary pointer, keyboard, focus, lifecycle, controller or animation listeners. Such capabilities require their actual Flutter wrapper/controller APIs. A widget with no event constructor argument must not be given fictitious Swing-style event parameters. GestureDetector has all 58 callbacks, Listener all 9 raw pointer callbacks, and MouseRegion all 3 mouse-region callbacks. Focus adds onFocusChange plus modern onKeyEvent and deprecated onKey in its standard constructor; the external-node constructor uses the node's key callbacks. NotificationListener adds onNotification with exact selected-type and propagation semantics. All five have explicit Wrap actions.

## Per-widget coverage

Every row has been included explicitly. “None (wrapper)” means the admitted widget has no direct event callback parameter; adding interaction requires an appropriate wrapper or controller. “None” in the callable column excludes event callbacks, which have their own column.

| Palette category | Widget / official API | Supported events | Other supported callables | Scope / known gap |
|---|---|---|---|---|
| Material | [MenuAnchor](https://api.flutter.dev/flutter/material/MenuAnchor-class.html) | onOpen, onClose, onAnimationStatusChanged | builder | All constructor parameters, optional Child and required-empty-valid Menu children; whole MenuStyle plus 203 local leaves, exact MenuAnchorChildBuilder and explicit scoped Create Menu Builder workflow. Native empty-state resolution, inert deprecated anchorTapClosesMenu, source-owned controller/builder lifecycle. [Full contract](MENU_ANCHOR.md) |
| Material | [MenuItemButton](https://api.flutter.dev/flutter/material/MenuItemButton-class.html) | onPressed, onHover, onFocusChange | styleBackgroundBuilder, styleForegroundBuilder | All constructor parameters and three optional slots; native MenuButtonTheme, all 498 local style leaves, whole style, SingleActivator/CharacterActivator and strict shortcut references. Shortcuts display hints, not action registration. [Full contract](MENU_ITEM_BUTTON.md) |
| Material | [SubmenuButton](https://api.flutter.dev/flutter/material/SubmenuButton-class.html) | onHover, onFocusChange, onOpen, onClose, onAnimationStatusChanged | styleBackgroundBuilder, styleForegroundBuilder | All 21 constructor arguments, required menu-children list plus optional child/icon slots, whole/local style contracts, strict shortcut/controller/state references and bounded native nested-menu Canvas. [Full contract](SUBMENU_BUTTON.md) |
| Interaction | [NotificationListener](https://api.flutter.dev/flutter/widgets/NotificationListener/NotificationListener.html) | onNotification | None | All 14 SDK filter types and strict project class/typedef references, exact bool callback, independent Notification bound, required child; [full contract and custom-type preview limitation](NOTIFICATION_LISTENER.md) |
| Interaction | [GestureDetector](https://api.flutter.dev/flutter/widgets/GestureDetector/GestureDetector.html) | All 58: primary/secondary/tertiary tap and long press, double tap, vertical/horizontal drag, pan, scale, force press | None | All 6 behavior/trackpad/device properties; optional child; exact SDK gesture-conflict checks; [full contract](GESTURE_DETECTOR.md) |
| Interaction | [Listener](https://api.flutter.dev/flutter/widgets/Listener/Listener.html) | All 9: onPointerDown, onPointerMove, onPointerUp, onPointerHover, onPointerCancel, onPointerPanZoomStart, onPointerPanZoomUpdate, onPointerPanZoomEnd, onPointerSignal | None | All behavior values; optional child; raw events do not impose gesture-recognizer conflicts; [full contract](LISTENER.md) |
| Interaction | [MouseRegion](https://api.flutter.dev/flutter/widgets/MouseRegion/MouseRegion.html) | onEnter, onExit, onHover | None | All 41 cursor presets and custom references/factories, opacity/State, nullable hitTestBehavior and optional child; [full contract and custom-cursor preview limitation](MOUSE_REGION.md) |
| Interaction | [Focus](https://api.flutter.dev/flutter/widgets/Focus/Focus.html) | onFocusChange, onKeyEvent, deprecated onKey | None | Both constructors, all 12 SDK properties plus selector, strict nullable/required node references, 6 State consumers and required child; key Events are inactive and retained in external mode; [full contract and node preview limitation](FOCUS.md) |
| Material | [Scaffold](https://api.flutter.dev/flutter/material/Scaffold-class.html) | onDrawerChanged, onEndDrawerChanged | bottomSheetScrimBuilder (builder) | G1 complete: exact non-null callback with nullable Widget result; typed references/getters/members/factories; [full contract and Canvas approximation](SCAFFOLD_BOTTOM_SHEET_SCRIM_BUILDER.md) |
| Material | [AppBar](https://api.flutter.dev/flutter/material/AppBar-class.html) | None (wrapper) | notificationPredicate (predicate) | G2 complete: default/depthZero/all presets and strict project references/getters/members/factories; [full contract and Canvas approximation](APP_BAR_NOTIFICATION_PREDICATE.md) |
| Material | [ElevatedButton](https://api.flutter.dev/flutter/material/ElevatedButton-class.html) | onPressed, onLongPress, onHover, onFocusChange | styleBackgroundBuilder, styleForegroundBuilder (builders) | G3 complete for the admitted unnamed constructor and five-state projection; strict layer references, independent reset/theme inheritance; [scope and Canvas limitation](ELEVATED_BUTTON_LAYER_BUILDERS.md) |
| Material | [TextField](https://api.flutter.dev/flutter/material/TextField-class.html) | onChanged, onEditingComplete, onSubmitted, onAppPrivateCommand, onTap, onTapOutside, onTapUpOutside | buildCounter, contextMenuBuilder (builders) | G4 implemented: exact named counter parameters, nullable typed references/factories and explicit null; [scope, null semantics and verification status](TEXT_FIELD_BUILDERS.md) |
| Material | [Divider](https://api.flutter.dev/flutter/material/Divider-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [VerticalDivider](https://api.flutter.dev/flutter/material/VerticalDivider-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [Card](https://api.flutter.dev/flutter/material/Card-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [Badge](https://api.flutter.dev/flutter/material/Badge-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [CircleAvatar](https://api.flutter.dev/flutter/material/CircleAvatar-class.html) | onBackgroundImageError, onForegroundImageError | None | Each error listener requires its corresponding image |
| Material | [LinearProgressIndicator](https://api.flutter.dev/flutter/material/LinearProgressIndicator-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [CircularProgressIndicator](https://api.flutter.dev/flutter/material/CircularProgressIndicator-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [RefreshProgressIndicator](https://api.flutter.dev/flutter/material/RefreshProgressIndicator-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Material | [RefreshIndicator](https://api.flutter.dev/flutter/material/RefreshIndicator-class.html) | onRefresh, onStatusChange | notificationPredicate (predicate) | onStatusChange only in noSpinner; onRefresh required in SDK |
| Material | [TextButton](https://api.flutter.dev/flutter/material/TextButton-class.html) | onPressed, onLongPress, onHover, onFocusChange | styleBackgroundBuilder, styleForegroundBuilder (builders) | Standard/icon; inherited ButtonStyleButton events |
| Material | [OutlinedButton](https://api.flutter.dev/flutter/material/OutlinedButton-class.html) | onPressed, onLongPress, onHover, onFocusChange | styleBackgroundBuilder, styleForegroundBuilder (builders) | Standard/icon; inherited ButtonStyleButton events |
| Material | [FilledButton](https://api.flutter.dev/flutter/material/FilledButton-class.html) | onPressed, onLongPress, onHover, onFocusChange | styleBackgroundBuilder, styleForegroundBuilder (builders) | Standard/icon/tonal/tonalIcon; inherited ButtonStyleButton events |
| Material | [FloatingActionButton](https://api.flutter.dev/flutter/material/FloatingActionButton-class.html) | onPressed | None | Standard/small/large/extended |
| Material | [IconButton](https://api.flutter.dev/flutter/material/IconButton-class.html) | onPressed, onHover, onLongPress | styleBackgroundBuilder, styleForegroundBuilder (builders) | Standard/filled/filledTonal/outlined; no SDK onFocusChange parameter |
| Material | [Checkbox](https://api.flutter.dev/flutter/material/Checkbox-class.html) | onChanged | None | Standard/adaptive; bool? argument |
| Material | [Switch](https://api.flutter.dev/flutter/material/Switch-class.html) | onChanged, onActiveThumbImageError, onInactiveThumbImageError, onFocusChange | None | Standard/adaptive; bool argument; image-error prerequisites |
| Material | [Slider](https://api.flutter.dev/flutter/material/Slider-class.html) | onChanged, onChangeStart, onChangeEnd | semanticFormatterCallback (formatter) | Standard/adaptive; double argument |
| Material | [RangeSlider](https://api.flutter.dev/flutter/material/RangeSlider-class.html) | onChanged, onChangeStart, onChangeEnd | semanticFormatterCallback (formatter) | Standard; RangeValues argument |
| Material | [Radio](https://api.flutter.dev/flutter/material/Radio-class.html) | onChanged | None | Standard/adaptive; legacy onChanged deprecated; prefer RadioGroup |
| Material | [RadioGroup](https://api.flutter.dev/flutter/widgets/RadioGroup-class.html) | onChanged | None | Required non-null callback; T? value; generated stub uses Object? |
| Material | [ListTile](https://api.flutter.dev/flutter/material/ListTile-class.html) | onTap, onLongPress, onFocusChange | None | Optional gestures and focus callback |
| Material | [CheckboxListTile](https://api.flutter.dev/flutter/material/CheckboxListTile-class.html) | onChanged, onFocusChange | None | Standard/adaptive; required nullable callback |
| Material | [SwitchListTile](https://api.flutter.dev/flutter/material/SwitchListTile-class.html) | onChanged, onFocusChange, onActiveThumbImageError, onInactiveThumbImageError | None | Standard/adaptive, 236 properties and three optional slots; bool callback/value, required nullable onChanged, image-error prerequisites; [full contract](SWITCH_LIST_TILE.md) |
| Material | [RadioListTile](https://api.flutter.dev/flutter/material/RadioListTile-class.html) | onChanged, onFocusChange | None | Standard/adaptive, 153 properties and three slots; generic T? callback and bool focus callback, optional legacy group State, modern RadioGroup ordering, original prefixed compound paths; [full contract](RADIO_LIST_TILE.md) |
| Material | [ExpansionTile](https://api.flutter.dev/flutter/material/ExpansionTile-class.html) | onExpansionChanged | None | All 28 SDK scalar fields, 76 rows and five slots; required Title wrapping, full shapes/AnimationStyle, six State consumers, seed-only initiallyExpanded and user-owned controllers; [full contract](EXPANSION_TILE.md) |
| Material | [Tooltip](https://api.flutter.dev/flutter/material/Tooltip-class.html) | onTriggered | positionDelegate (delegate) | All 22 SDK scalar fields, 53 rows and optional Child; exact plain/rich XOR, source-owned InlineSpan trees, full shared text style/decoration, three durations and five State consumers. Pinned tap/long-press triggers notify; hover/programmatic visibility do not. [Full contract](TOOLTIP.md) |
| Material | [TooltipVisibility](https://api.flutter.dev/flutter/material/TooltipVisibility-class.html) | None (wrapper) | None | Required visible bool and required Child; nearest inherited scope wins, one boolean State consumer, no invented visibility Event/producer. [Pinned SDK lifecycle and semantics contract](TOOLTIP_VISIBILITY.md) |
| Material | [TooltipTheme](https://api.flutter.dev/flutter/material/TooltipTheme-class.html) | None (wrapper) | None | Strict whole TooltipThemeData reference or all 15 local SDK fields plus 31 TextStyle leaves; required Child, three nullable boolean State consumers, exact whole/local conflicts and nearest-theme replacement. [Full contract](TOOLTIP_THEME.md) |
| Layout | [Column](https://api.flutter.dev/flutter/widgets/Column-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Row](https://api.flutter.dev/flutter/widgets/Row-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Wrap](https://api.flutter.dev/flutter/widgets/Wrap-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Padding](https://api.flutter.dev/flutter/widgets/Padding-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Center](https://api.flutter.dev/flutter/widgets/Center-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [SizedBox](https://api.flutter.dev/flutter/widgets/SizedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [AspectRatio](https://api.flutter.dev/flutter/widgets/AspectRatio-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Container](https://api.flutter.dev/flutter/widgets/Container-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Opacity](https://api.flutter.dev/flutter/widgets/Opacity-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Align](https://api.flutter.dev/flutter/widgets/Align-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [FractionallySizedBox](https://api.flutter.dev/flutter/widgets/FractionallySizedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [FittedBox](https://api.flutter.dev/flutter/widgets/FittedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [ConstrainedBox](https://api.flutter.dev/flutter/widgets/ConstrainedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [UnconstrainedBox](https://api.flutter.dev/flutter/widgets/UnconstrainedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [LimitedBox](https://api.flutter.dev/flutter/widgets/LimitedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [OverflowBox](https://api.flutter.dev/flutter/widgets/OverflowBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Stack](https://api.flutter.dev/flutter/widgets/Stack-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [IndexedStack](https://api.flutter.dev/flutter/widgets/IndexedStack-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Expanded](https://api.flutter.dev/flutter/widgets/Expanded-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Flexible](https://api.flutter.dev/flutter/widgets/Flexible-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Spacer](https://api.flutter.dev/flutter/widgets/Spacer-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Baseline](https://api.flutter.dev/flutter/widgets/Baseline-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [IntrinsicHeight](https://api.flutter.dev/flutter/widgets/IntrinsicHeight-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [IntrinsicWidth](https://api.flutter.dev/flutter/widgets/IntrinsicWidth-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Offstage](https://api.flutter.dev/flutter/widgets/Offstage-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [SizedOverflowBox](https://api.flutter.dev/flutter/widgets/SizedOverflowBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [Transform](https://api.flutter.dev/flutter/widgets/Transform-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [RotatedBox](https://api.flutter.dev/flutter/widgets/RotatedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [ListBody](https://api.flutter.dev/flutter/widgets/ListBody-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [OverflowBar](https://api.flutter.dev/flutter/widgets/OverflowBar-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Layout | [SafeArea](https://api.flutter.dev/flutter/widgets/SafeArea-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Scrolling | [ListView](https://api.flutter.dev/flutter/widgets/ListView-class.html) | None (wrapper) | itemExtentBuilder (builder) | G5 implemented for the admitted static constructor: nullable typed references/factories, explicit null, fixed-extent conflict check; [scope and verification status](LIST_VIEW_ITEM_EXTENT_BUILDER.md). Dynamic constructors remain separate backlog |
| Scrolling | [GridView.count](https://api.flutter.dev/flutter/widgets/GridView-class.html) | None (wrapper) | None | Admitted count constructor; dynamic constructors separate backlog |
| Scrolling | [SingleChildScrollView](https://api.flutter.dev/flutter/widgets/SingleChildScrollView-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Text](https://api.flutter.dev/flutter/widgets/Text-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Icon](https://api.flutter.dev/flutter/widgets/Icon-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Image](https://api.flutter.dev/flutter/widgets/Image-class.html) | None (wrapper) | frameBuilder, loadingBuilder, errorBuilder (builders) | Three builders, no direct event callback |
| Basic | [ColoredBox](https://api.flutter.dev/flutter/widgets/ColoredBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Placeholder](https://api.flutter.dev/flutter/widgets/Placeholder-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Directionality](https://api.flutter.dev/flutter/widgets/Directionality-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [DecoratedBox](https://api.flutter.dev/flutter/widgets/DecoratedBox-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ClipRect](https://api.flutter.dev/flutter/widgets/ClipRect-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ClipOval](https://api.flutter.dev/flutter/widgets/ClipOval-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ClipRRect](https://api.flutter.dev/flutter/widgets/ClipRRect-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ClipPath](https://api.flutter.dev/flutter/widgets/ClipPath-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ClipRSuperellipse](https://api.flutter.dev/flutter/widgets/ClipRSuperellipse-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [PhysicalModel](https://api.flutter.dev/flutter/widgets/PhysicalModel-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [PhysicalShape](https://api.flutter.dev/flutter/widgets/PhysicalShape-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [RepaintBoundary](https://api.flutter.dev/flutter/widgets/RepaintBoundary-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [IgnorePointer](https://api.flutter.dev/flutter/widgets/IgnorePointer-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [AbsorbPointer](https://api.flutter.dev/flutter/widgets/AbsorbPointer-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [Visibility](https://api.flutter.dev/flutter/widgets/Visibility-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [TickerMode](https://api.flutter.dev/flutter/widgets/TickerMode-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [DefaultTextHeightBehavior](https://api.flutter.dev/flutter/widgets/DefaultTextHeightBehavior-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [DefaultSelectionStyle](https://api.flutter.dev/flutter/widgets/DefaultSelectionStyle-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [IconTheme](https://api.flutter.dev/flutter/widgets/IconTheme-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Basic | [ImageIcon](https://api.flutter.dev/flutter/widgets/ImageIcon-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [ExcludeSemantics](https://api.flutter.dev/flutter/widgets/ExcludeSemantics-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [BlockSemantics](https://api.flutter.dev/flutter/widgets/BlockSemantics-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [MergeSemantics](https://api.flutter.dev/flutter/widgets/MergeSemantics-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [IndexedSemantics](https://api.flutter.dev/flutter/widgets/IndexedSemantics-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [ExcludeFocus](https://api.flutter.dev/flutter/widgets/ExcludeFocus-class.html) | None (wrapper) | None | No direct callable constructor parameter |
| Accessibility | [ExcludeFocusTraversal](https://api.flutter.dev/flutter/widgets/ExcludeFocusTraversal-class.html) | None (wrapper) | None | No direct callable constructor parameter |

## Closure of the known callable gaps in admitted constructors

G1, `Scaffold.bottomSheetScrimBuilder`, is implemented across the schema, persistence, generation, strict analyzer proof, isolated Canvas and property editor. It added one builder under Properties → Appearance, not another native Event. Scaffold has 18 properties and the unchanged three slots; the catalog at that stage had 3,990 properties. [Contract and verification status](SCAFFOLD_BOTTOM_SHEET_SCRIM_BUILDER.md).

G2, `AppBar.notificationPredicate`, retains all three existing presets and also accepts strict project callable references, getters/members and zero-argument factories. It remains a non-null predicate under Properties → Behavior, not an Event or a notification consumer. No property or slot was added; that stage retained the 3,990-property inventory. [Contract and verification status](APP_BAR_NOTIFICATION_PREDICATE.md).

G3 adds `ElevatedButton.styleBackgroundBuilder/styleForegroundBuilder` as ordinary common-style BUILDER properties. Typed non-null bindings preserve the nullable SDK fields' omission/theme fallback; resetting one does not clear the other or the existing five state buckets. ElevatedButton now has 288 properties, including 11 common-style rows, while TextButton/OutlinedButton/FilledButton/IconButton retain their existing property counts and orders. This is not a claim of complete ElevatedButton SDK coverage. [Contract and verification status](ELEVATED_BUTTON_LAYER_BUILDERS.md).

G4 adds `TextField.buildCounter/contextMenuBuilder` under Properties → Builders, bringing TextField to 56 properties while retaining the first 54 orders. Both accept exact nullable typed references/getters/members/factories and explicit null. Counter omission/null retains the SDK counter policy, while a callback returning null hides the counter; menu omission uses the SDK default but null disables the menu. Required named metadata retains nullable int? maxLength without treating it as optional. [Contract and verification status](TEXT_FIELD_BUILDERS.md).

G5 adds `ListView.itemExtentBuilder` under Properties → Layout on the admitted unnamed/static constructor. Its exact nullable ItemExtentBuilder reference receives int and Rendering-owned SliverLayoutDimensions and returns double?. Explicit null/omission are distinct from a callback returning null; the latter is for out-of-range indexes, not default sizing or truncation of actual children. Fixed itemExtent cannot coexist with any builder reference, even a nullable one; explicit null is allowed without silently clearing either field. [Contract and verification status](LIST_VIEW_ITEM_EXTENT_BUILDER.md).

The current 171-callable / 6,227-property inventory has **zero known callable gaps within the audited admitted-constructor scope**. G1–G5 are implemented; measured acceptance for the newest slice is tracked in its linked contract. This closes the enumerated gaps, not the entire Flutter API, historical palette target, dynamic constructors or nested object-owned listeners.

The current signature metadata covers 171 callables, including the one exact anonymous function signature admitted for the Scaffold scrim builder, the non-event TooltipPositionDelegate and required/optional named parameters. InputCounterWidgetBuilder retains its required named currentLength, maxLength and isFocused arguments. The required maxLength parameter has nullable int? type: requiredness and nullability are separate. Optional named parameters without explicit default metadata are admitted only with nullable types; positional parameters must precede named ones.

Dynamic constructors are a separate, explicitly unimplemented surface: ListView.builder/separated accept itemBuilder and index lookup callbacks; separated also accepts separatorBuilder and the newer findItemIndexCallback. GridView.builder accepts itemBuilder and findChildIndexCallback. ListView.custom/GridView.custom accept delegates, whose builders are object-owned callbacks. These are not counted as missing direct events on the currently admitted ListView()/GridView.count constructors.

Nested object listener/configuration surfaces are also outside this direct-constructor inventory: TextEditingController/FocusNode listeners, Animation listeners, ImageProvider resolution, content insertion, spelling configuration, TextSpan recognizers, and CustomClipper methods. They must not be advertised as completed just because their owning widget is present.

## Signature and behavior contracts

- `Switch.onChanged` is `void Function(bool)`; Checkbox/CheckboxListTile take `bool?`, independently of whether the current model has tristate enabled.
- Radio/RadioGroup APIs use `T?`. The existing catalog analyzer contract is `ValueChanged<Object?>`; generated form handlers use `Object?`, which remains assignable for supported concrete radio value types and avoids emitting an undefined `T`.
- `TextField.onAppPrivateCommand` takes `String action, Map<String, dynamic> data`; `onTapOutside` and `onTapUpOutside` take PointerDownEvent and PointerUpEvent respectively.
- TextField builders use `InputCounterWidgetBuilder?` and `EditableTextContextMenuBuilder?`, with actual nullable callback references/factories admitted. Counter Widget? results and menu non-null Widget results remain distinct. Exact typedef assignment plus an independent null-aware original-expression Widget? result witness reject incompatible or dynamic results without executing user code. Canvas preserves explicit null and explicitly approximates custom references, including nullable ones, with isolated SDK defaults.
- ListView.itemExtentBuilder uses nullable `ItemExtentBuilder? = double? Function(int, SliverLayoutDimensions)`, with a fixed Rendering import for the actual SDK typedef/dimensions and a strict dart:core double? invocation-result witness. Every actual child needs a valid extent: the pinned rendering code force-unwraps actual-child results, so null is only the out-of-range marker. Omission/null callback retains normal or configured fixed sizing. Any reference conflicts with itemExtent; explicit null does not. Canvas never executes the builder and explicitly approximates custom extents with normal SDK child sizing.
- Image frame/loading/error builders retain nullable frame, progress and stack-trace arguments. Button layer builders retain the nullable child and `Set<WidgetState>`.
- ElevatedButton's two layer builders use `ButtonLayerBuilder = Widget Function(BuildContext, Set<WidgetState>, Widget?)`: the return Widget is non-null although each optional SDK style field is nullable. Designer accepts non-null typed references/getters/members/factories, with Reset for omission/theme inheritance rather than an explicit-null mode. An independent Material-SDK Widget-result witness rejects dynamic-return builders across the shared ButtonLayerBuilder family. Omitted clipBehavior retains Flutter's effective local/theme-builder choice of antiAlias or none; explicit Clip values win. Builders are not restricted to the five Designer style buckets and receive Flutter's actual runtime state set.
- `Scaffold.bottomSheetScrimBuilder` is exactly `Widget? Function(BuildContext, Animation<double>)`: the callback is non-null, but its result may be null to suppress the scrim. Omission uses the SDK's actual default builder; explicit callback null is rejected. The pinned non-null field declaration is authoritative over the adjacent prose suggesting a null callback fallback. An independent strict call-result witness rejects dynamic-return functions even when ordinary function assignability would accept them. Canvas never invokes this project builder; it explicitly previews the SDK default instead.
- `AppBar.notificationPredicate` is the non-null `ScrollNotificationPredicate = bool Function(ScrollNotification)`. Omission/default/depthZero preserve the depth-zero policy; all accepts every notification presented to the predicate. Its result filters AppBar's scroll-under processing, not notification propagation. AppBar processes ScrollUpdateNotification, retains horizontal scroll-under state and uses extentAfter for upward/reverse vertical scrolling. An SDK-qualified bool-result witness rejects dynamic-return predicates, including the shared RefreshIndicator proof family. Canvas preserves exact presets and explicitly approximates custom references with the SDK depth-zero default.
- `RefreshIndicator.onRefresh` returns `Future<void>`, is required and non-null in Flutter, but the Designer may omit the model property because the generator supplies an async no-op. `onStatusChange` is exposed only by the noSpinner variant.
- Callback nullability, SDK constructor requiredness, model requiredness, an omitted property, an explicit null and a generated no-op are distinct metadata. Existing control enabled/disabled rules remain authoritative.
- CheckboxListTile/RadioGroup model creation defaults retain the explicit `"noop"` value. Required callback bindings cannot be disconnected by simply removing a required model property.
- Image error events require their corresponding provider properties. The existing candidate analysis must reject impossible combinations.
- The four ButtonStyleButton families inherit onPressed/onLongPress/onHover/onFocusChange. IconButton independently has onPressed/onLongPress/onHover; adding an onFocusChange constructor argument to IconButton would be invalid.
- One handler may be shared by compatible event signatures. Handler bodies belong to the user's Dart source. Reopening or changing palette properties must not regenerate those bodies.
- New non-void callable stubs deliberately throw UnimplementedError until edited, instead of pretending to provide a meaningful builder/predicate/formatter result. Canvas must not invoke user event code.

## Acceptance boundary

Unit verification must cover all 171 callable metadata entries, preserve the admitted binding representations for each property, validate identifiers and signature imports, and distinguish builders/predicates/formatters/delegates from events. SDK verification should type-check generated handler tear-offs against the actual pinned Flutter typedefs or exact anonymous signatures, without inventing SDK typedefs. UI/transaction tests must additionally cover the applicable create, choose existing, navigation, rename, disconnect, save/reopen, Undo/Redo, disabled controls and source-preservation workflows. G1–G5 use their ordinary property editors, not the Events create/rename workflow. Completing this inventory alone does not complete those UI and state-management workflows.
