# netbeans_flutter_canvas_runner

ImageFiltered maps blur/bounds, morphology, matrix and composition to native
filters and respects Enabled without replacing the render object. Source filters,
matrices, bounds and shader preview with explicit safe fallbacks; shader execution
remains project-owned and Impeller-only. See [ImageFiltered](../../../../docs/IMAGE_FILTERED.md).

ColorFiltered uses native mode/matrix/gamma/saturation filters, with all 29
BlendModes and all 20 matrix coefficients. Source-owned ColorFilter values
preview as identity, source Color as transparent only in the active mode.
Inactive drafts are never executed. Empty Child remains legal and selectable.
See [ColorFiltered](../../../../docs/COLOR_FILTERED.md).

RawImage maps all 16 painting parameters to the native renderer. Project-owned
decoded images, nullable opacity animations and Rect center slices preview as
null, with an explicit field-specific tooltip; no handle or executable project
code crosses into Canvas. See [RawImage](../../../../docs/RAW_IMAGE.md).

FadeInImage uses the native two-phase image transition with independently
resolved resources, scaling/resizing, error reporting, colors/fits/filters and
image semantics. Project providers/builders are not executed; substitutions
name the affected fields. The 43 curve presets remain exact; native image
painting clamps overshooting opacity. See [FadeInImage](../../../../docs/FADE_IN_IMAGE.md).

AnimatedIcon renders all 14 SDK glyphs with stopped local progress, native
IconTheme opacity/size and direction behavior. Source icon/progress/color/size
use menu_close/0/inherited values with field-specific diagnostics; project code
never executes. See [AnimatedIcon](../../../../docs/ANIMATED_ICON.md).


AnimatedModalBarrier uses its real native widget with a required stopped local
color or an explicitly labelled stopped-null substitute for project animation.
Native nullable semantics defaults are preserved; route changes, callbacks and
alert sounds are isolated. Transparent selection and unbounded-layout recovery
remain available. See [AnimatedModalBarrier](../../../../docs/ANIMATED_MODAL_BARRIER.md).


ModalBarrier renders the native childless barrier in bounded space behind a
selection-preserving input shield. Canvas suppresses route changes, callbacks and
alert sounds; source colors/notifiers receive exact transparent/null diagnostics.
An unbounded axis receives an unavailable-preview message without invented stored
dimensions. See [ModalBarrier](../../../../docs/MODAL_BARRIER.md).

MatrixTransition uses the native transform widget with a fresh fixed local matrix
callback, stopped scalar animation, physical Alignment and optional Child.
Typed project sources never execute: animation previews as 0, callback as identity
and alignment as center, with exact per-field diagnostics. Non-finite effective
matrix entries use the shared paint/hit/semantics guard while retaining State,
child and values. Zero-size/empty widgets remain selectable. See
[MatrixTransition](../../../../docs/MATRIX_TRANSITION.md).

AlignTransition uses the real native layout widget, local physical/directional
stopped AlignmentGeometry and a required Child. Optional factors reset immediately
on null or omission. Source animation/factor references receive a named safe
center/null preview without executing application code. A non-finite local
offset quarantines paint, hits and semantics while preserving the widget State
and child; corrected coordinates restore preview. See
[AlignTransition](../../../../docs/ALIGN_TRANSITION.md).

DecoratedBoxTransition uses the native widget with a required child and stopped
local BoxDecoration animation, including shared image/theme/gradient conversion.
Position chooses background/foreground without padding or clipping. Typed
project Animation<Decoration> sources use an explicitly labelled empty-decoration
preview; application Dart retains live Box/Shape/custom decoration animations.
Required child slots are wrap/replacement targets, never empty insertion zones.


RelativePositionedTransition renders native physical Rect/Size parent-data outside
Canvas instrumentation. Required local LTWH, null-value stopped Animation<Rect?>
and independent reference Size modes follow the pinned SDK, including zero and
negative dimensions. References never execute: animation preview uses 0,0,48,48
and Size preview uses 48,48; each substitution is named in the tooltip.
Reference Size is not a scale or actual Stack size. Direct Stack-child wrapping,
null fallback geometry, source/local changes, zero-size selection and native
State identity are covered in
[RelativePositionedTransition](../../../../docs/RELATIVE_POSITIONED_TRANSITION.md).


PositionedTransition renders the real Stack parent-data animation outside Canvas
render instrumentation. Local physical LTRB insets use a stopped RelativeRect;
sources preview at fill with an explicit tooltip. Required Child is created by
wrapping a direct Stack child, with exact parent-data rejection, coincident
zero-size handles and unbounded-Stack diagnostics. See
[PositionedTransition](../../../../docs/POSITIONED_TRANSITION.md).


SizeTransition uses native ClipRect/Align layout with signed stopped size factors,
both axes, modern physical/directional or deprecated axis alignment (exclusive),
and a nullable cross-axis factor. Negative main-axis factors collapse to zero;
tight parent constraints still win. Source references preview at factor 1 and
null optional values, with per-property diagnostics; user code is never executed.
Native axis-dependent directional defaults, clipping, child/State preservation
and zero-size selection/drop are covered. See
[SizeTransition](../../../../docs/SIZE_TRANSITION.md).

RotationTransition uses native signed-turns rotation with a constant
AlwaysStoppedAnimation, physical Alignment and nullable FilterQuality.
Zero is identity, positive turns are clockwise even in RTL, and layout is
unchanged. Source references remain inert at zero turns / center, with explicit
diagnostics. Constant-value forward status still enables filtering. Non-finite
rotation/pivot matrices are quarantined at the preview boundary; source values,
native State and Child identity survive recovery. See
[RotationTransition](../../../../docs/ROTATION_TRANSITION.md).

ScaleTransition uses the native widget with a constant AlwaysStoppedAnimation.
Signed/zero scale, physical Alignment and nullable FilterQuality are preserved.
AlwaysStoppedAnimation reports forward, so its filter remains active despite
its constant value. Project references stay inert with scale-1 / center
fallbacks and explicit diagnostics. Empty and zero-scale selection/drop targets
remain available. See [ScaleTransition](../../../../docs/SCALE_TRANSITION.md).

SlideTransition mounts the native AnimatedWidget with a stopped local
Animation<Offset>, exact hit-test flag and explicit/null TextDirection.
Null does not inherit ambient RTL. Local edits retain State and child identity;
project references are never executed and report their stopped-zero preview.
Optional empty children retain drop targets. See [SlideTransition](../../../../docs/SLIDE_TRANSITION.md).

FadeTransition and SliverFadeTransition mount native opacity render objects with
stopped local Animation<double> values, preserving child/scroll geometry and
native hit testing and semantics. Project animations are never executed: the
preview reports its stopped-opacity-1 fallback. Both optional slots remain
editable even when empty or fully transparent. See [Fade transitions](../../../../docs/FADE_TRANSITION.md).

DefaultTextStyleTransition mounts the native widget with a stopped local
TextStyle or an explicitly labeled stopped-empty fallback for project-owned
Animation<TextStyle> references. All five scalar arguments and 31 local style
fields are supported, with required Child, stable native State/child identity
and zero-size selection. Project timing/controllers are never executed in
Canvas. See [DefaultTextStyleTransition](../../../../docs/DEFAULT_TEXT_STYLE_TRANSITION.md).

DefaultTextStyle and its static merge method use native immediate inheritance,
with 41 writable rows each and required Child. Merge null/unset inherits the
ancestor (including Max lines); ordinary defaults replace it. All local style
fields and paragraph/height settings are rendered. Project references remain
inert with explicit per-variant fallback diagnostics. Zero-sized children
remain selectable. See [DefaultTextStyle](../../../../docs/DEFAULT_TEXT_STYLE.md).

Native Theme applies its required ThemeData immediately to its required Child.
Six shared M2/M3 light/dark/fallback factories are rendered exactly; a project
ThemeData reference remains inert with an explicit light Material 3 fallback
diagnostic. Required-child wrapping uses the reviewed creation contract.
No animation fields or events are admitted. See [Theme](../../../../docs/THEME.md).

AnimatedTheme uses native ThemeDataTween with all four constructor properties,
required Child and the exact optional 200 ms Duration. Light/dark/fallback
factory presets cover Material 3 and Material 2. The nearest theme replaces its
parent data. Project ThemeData/Curve/Duration/On end references are inert and
labeled (fallback light Material 3, linear, 200 ms, no callback); arbitrary
ThemeData fields remain source-owned. See
[the complete contract](../../../../docs/ANIMATED_THEME.md).

AnimatedSwitcher supports all six constructor properties and an optional Child.
Native animation entries have separate Canvas geometry, menu and Tooltip keys;
outgoing entries cannot steal pointer/focus/selection. Same-type unkeyed children
update in place. Flutter 3.44.8 defaultTransitionBuilder reuses transition keys:
Canvas uses an explicitly labeled unkeyed outer transition wrapper to retain
independent outgoing entries. Generated Dart retains SDK defaults. Custom
project builders/references are stored and analyzed, never executed in Canvas.
See [the complete contract](../../../../docs/ANIMATED_SWITCHER.md).

AnimatedCrossFade uses the full Flutter 3.44.8 constructor, two required keyed
subtrees, independent first/second/size curves, forward/reverse Duration and
physical/directional alignment. Canvas executes no project code; custom builders
use the labeled native default layout. Only the top branch accepts Designer
surface selection/drop; both branches remain editable in tree/Slots.
See [the complete contract](../../../../docs/ANIMATED_CROSS_FADE.md).


AnimatedSize mounts the full native constructor with physical/directional alignment,
all curves, exact duration/reverseDuration, clipping and optional Child.
Project references are inert and labeled. Empty nodes retain selection/drop handles.
Pinned Flutter 3.44.8 runs normal size growth/shrink forward using duration;
reverseDuration is retained unchanged. See docs/ANIMATED_SIZE.md at repository root.


AnimatedContainer mounts the full native constructor: eight geometry/decoration
tweens, nullable local values, all 43 curves, Duration and optional Child.
Color/decoration and clipping dependencies match Flutter; width/height tighten
constraints natively. Project-owned values are inert and labeled (null geometry/
decoration, 300 ms, linear, no callback; unavailable background disables preview clipping).
A shared matrix guard contains non-finite Matrix4Tween frames without changing source.
See [the complete contract](../../../../docs/ANIMATED_CONTAINER.md).

AnimatedRotation uses native signed turns (1 = 360 degrees clockwise, also in RTL),
physical Alignment, nullable Filter quality, Duration, all 43 curves and optional Child.
Targets are not normalized: 0 to 1 animates a complete revolution, not a no-op.
Layout, Child state, transformed hits and animation-only filtering stay native.
Project double/Alignment/Curve/Duration/On end references are inert and labeled
(zero turns, center, linear, 300 ms, no callback).
A per-frame guard retains the native tween/Child but withholds paint, semantics and
hits if extreme angles/pivots make the matrix non-finite; it labels the limitation
and recovers automatically when the matrix becomes finite. Stored/source values
are unchanged. See [the complete contract](../../../../docs/ANIMATED_ROTATION.md).

AnimatedScale mounts the native signed scale tween, physical Alignment and nullable
FilterQuality, Duration, all 43 curves and optional Child. Layout and child identity
are preserved; negative scales flip both axes and zero disables child hit testing.
Only scale animates: alignment/quality changes do not start separate tweens.
The native filter is applied during animation only. Project double/Alignment/Curve/
Duration/On end bindings stay inert and labeled (1, center, linear, 300 ms, no callback).
Empty nodes retain selection and Child drop handles.
See [the complete contract](../../../../docs/ANIMATED_SCALE.md).

AnimatedSlide mounts the native fractional Offset tween without changing layout.
Signed offsets are physical in both LTR and RTL, and all 43 curves preserve
native overshoot. Child identity, size-dependent translation, hit testing and
zero-size drop targets are tested. Project Offset/Curve/Duration/On end bindings
stay inert and labeled: zero offset, linear curve, 300 ms, no completion callback.
See [the complete contract](../../../../docs/ANIMATED_SLIDE.md).

AnimatedPadding uses native nonnegative physical/directional padding animation,
all 43 Curve presets, Duration and optional Child. Mixed geometry transitions,
RTL, overshoot clamping and child identity follow Flutter. Project-owned
EdgeInsetsGeometry/Curve/Duration/On end references stay inert and labeled;
custom geometry uses 16 pixels per side. Empty zero-padding targets remain
selectable and droppable. See [the complete contract](../../../../docs/ANIMATED_PADDING.md).

AnimatedAlign uses native physical/directional alignment and size-factor tweens,
with full RTL and nullable controls. Custom AlignmentGeometry/Curve/Duration/On end
bindings stay inert and labeled. Child state and native null-factor behavior are
preserved. See [the complete contract](../../../../docs/ANIMATED_ALIGN.md).

AnimatedOpacity mounts the native implicit animation with full scalar controls,
optional Child, zero-opacity semantics and hit testing. Project Curve, Duration
and On end references stay inert with an explicit preview limitation label;
generated source keeps exact bindings. Empty Child drop targets remain usable.
See [the complete contract](../../../../docs/ANIMATED_OPACITY.md).

TweenAnimationBuilder has box/sliver previews with a fresh preview-owned Tween<double>
and pass-through Child. Project Tween/T/builder/curve/duration/onEnd references
remain inert and explicitly labeled. Native callbacks are not executed in Canvas.
See [the complete contract](../../../../docs/TWEEN_ANIMATION_BUILDER.md).

ValueListenableBuilder has native box/sliver previews with an immutable
preview-owned source and pass-through Child. Project T/source/builder identities
remain inert, with an explicit limitation label. The application uses the exact
paired ValueListenable<T>/ValueWidgetBuilder<T> bindings and owns disposal.
See [the complete contract](../../../../docs/VALUE_LISTENABLE_BUILDER.md).

AnimatedBuilder mounts the native widget with a preview-owned immutable
animation source and pass-through Child callback in box/sliver projections.
Typed project source/builder references are inert and explicitly labeled;
custom animation is not simulated. Child drop zones, zero-size selection,
native layout and retained identity are tested.
See [the complete contract](../../../../docs/ANIMATED_BUILDER.md).

ListenableBuilder has native box/sliver projections with an optional matching
Child slot. Its preview-owned immutable Listenable and pass-through callback
never run project objects/getters/factories or builders. Either custom binding
produces an explicit warning: displayed Child is only a design-time fallback.
Native geometry, retained identity, empty selection and Child drop zones are
covered. See [the complete contract](../../../../docs/LISTENABLE_BUILDER.md).

DeviceOrientationBuilder has native box and sliver placement projections of the
same unnamed constructor, using MediaQuery.orientationOf rather than parent bounds.
Empty previews return SizedBox.shrink or SliverToBoxAdapter respectively.
The explicit sliver classifier keeps selection/semantics/drop wrappers in the
correct rendering protocol. Profile rotation propagates through native MediaQuery.
Project builder references remain inert and presence-only with an unavailable
preview notice. See [the complete contract](../../../../docs/DEVICE_ORIENTATION_BUILDER.md).

OrientationBuilder mounts the actual native widget with a preview-owned empty
box builder. Parent orientation (not device MediaQuery) and intrinsic-layout
restrictions are preserved. Project builder presence gets an explicit unavailable
preview notice; no user function/getter/factory is executed. Empty selection,
parent insertion and native element identity use the shared Designer paths.
See [the complete contract](../../../../docs/ORIENTATION_BUILDER.md).

LayoutBuilder mounts the real native layout-time builder with a preview-owned
SizedBox.shrink() result. All parent constraints remain authoritative, including
tight and unbounded axes. Empty zero-sized instances retain the shared selection
handle. Typed project builder bindings are presence-only and explicitly labeled
unavailable: no project callback/getter/factory is executed in isolated Canvas.
See [the complete contract](../../../../docs/LAYOUT_BUILDER.md).

FlexibleSpaceBarSettings mounts the actual inherited widget and all six native
values. It has one required box Child, with no injected sizing or scroll behavior.
Java and Dart both enforce minExtent <= currentExtent <= maxExtent. A toolbarOpacity
above 1 remains stored and is valid for Settings; a FlexibleSpaceBar descendant
labels its unavailable color preview because native title colors require 0..1.
See [the complete contract](../../../../docs/FLEXIBLE_SPACE_BAR_SETTINGS.md).

FlexibleSpaceBar uses native inherited FlexibleSpaceBarSettings, all three
collapse modes, all eight stretch-effect combinations, nullable directional
padding, title scale and independent Title/Background slots. Project padding
and effect lists remain presence-only and inert with an explicit warning.
Without inherited settings or finite geometry, Canvas labels the unavailable
preview and preserves modeled children; generated source is never wrapped.
See [the complete contract](../../../../docs/FLEXIBLE_SPACE_BAR.md).

SliverAppBar, medium and large use their actual native constructors, five slots
and reviewed scroll modes. The mirrored title retains one canonical model identity.
PreferredSize bottom geometry is forwarded to the native header. Project styles
and event code are not executed; stored/generated references remain unchanged.
See [the complete contract](../../../../docs/SLIVER_APP_BAR.md).

SliverFloatingHeader mounts the native required-child floating header. Both snap
modes and full AnimationStyle fields use SDK defaults when omitted/null.
Presence-only project references are never executed. Negative stored durations
use zero only in the preview, with an explicit warning; generated Dart is unchanged.
See [the complete contract](../../../../docs/SLIVER_FLOATING_HEADER.md).

PinnedHeaderSliver uses the native content-sized pinned header with one nullable
Child. Both scroll axes, reverse/RTL, child-size edits, semantics and precise
selection/drop geometry retain the existing shared Canvas behavior.
See [the complete contract](../../../../docs/PINNED_HEADER_SLIVER.md).

SliverResizingHeader uses the native pinned sliver and all three nullable box slots.
Its prototype subtrees are measured under a hidden-geometry marker, with no fake
Canvas hit targets. Pointer drops go to Child; explicit Slots/tree operations retain
access to both measurement prototypes.
See [the complete contract](../../../../docs/SLIVER_RESIZING_HEADER.md).


SliverPersistentHeader is admitted with required presence-only Delegate and optional
Pinned/Floating booleans. A preview-owned native delegate uses 56–112 extents;
every header displays an explicit project-code limitation. The runner never
evaluates delegate getters, build, vsync, snap or stretch.
See [the complete contract](../../../../docs/SLIVER_PERSISTENT_HEADER.md).


Isolated Windows child process for the NetBeans Flutter Designer's first native
read-only Canvas slice. NetBeans builds and launches this versioned runner for
each open `.fd` Design tab and embeds its real `FlutterView` as a verified child
window. The Canvas is painted by Flutter directly; the protocol never transfers
screenshots or framebuffer pixels. Model protocol v19 may additionally carry
bounded, content-addressed compressed project-image bytes for typed asset
previews.

## ListView per-item extent builder (Flutter 3.44.8)

`itemExtentBuilder` accepts omission, explicit null or a strict nullable
`ItemExtentBuilder?` reference. Canvas receives anonymous presence only and
never executes project functions, getters or factories. A configured reference
uses natural child sizing with an explicit warning: callback nullability,
per-index sizes and out-of-range results are unknown, not reproduced.
No constant builder is fabricated, and configured children remain intact.

Omission/null permits existing fixed `itemExtent` or natural sizing; any project
reference conflicts with fixed `itemExtent`, even if the callback might be null.
The SDK callback receives current `SliverLayoutDimensions` and may be called
repeatedly during layout. It returns null beyond available items; existing
children require usable nonnegative extents. Tests cover real varied extents,
current dimensions, both axes, reverse/shrink-wrap and retained Canvas scrolling,
geometry and selection across reference/reset changes.

## CustomScrollView and SliverToBoxAdapter (Flutter 3.44.8)

The runner mounts the native `CustomScrollView` with its reviewed scrolling,
viewport, semantics, restoration, clipping and hit-testing fields. Its `slivers`
list is trait-validated by the model; `SliverToBoxAdapter` is the first accepted
sliver and hosts an ordinary Designer child through the real SDK adapter.

Sliver adapters must remain direct children of the viewport's `RenderSliver`
list. The Canvas therefore skips the box-only semantics, gesture and custom-paint
wrappers around the adapter itself while retaining instrumentation on its child
and the owning `CustomScrollView`. A detached adapter receives a bounded box
fallback so a newly dropped palette item remains previewable until it is placed
under a compatible sliver parent.

## TextField counter and context-menu builders (Flutter 3.44.8)

`buildCounter` and `contextMenuBuilder` accept omission, explicit null, or strict
nullable project references (`InputCounterWidgetBuilder?` and
`EditableTextContextMenuBuilder?`). Canvas receives only anonymous presence;
project functions, getters and factories never execute.

Omitted/null counter uses the actual SDK default. A configured counter might
itself be null or return null/different content, so the warning explicitly labels
the SDK default counter as an approximation, not the unknown project result.
Omitted menu retains the SDK platform default; explicit null disables it exactly.
A menu reference might be null or return different content; its warning labels
the retained SDK platform menu as an approximation.

The existing noninteractive Canvas pointer/focus guard is unchanged. Tests seed
the real controller to verify text, grapheme counts, selection and element-state
retention; isolated rehosting of captured SDK fields verifies focused counter and
actual platform-menu behavior without expanding Designer input or IME support.

## ElevatedButton style layer builders (Flutter 3.44.8)

`styleBackgroundBuilder` and `styleForegroundBuilder` accept optional non-null
`ButtonLayerBuilder` project references. Canvas receives only anonymous presence
and does not execute project functions, getters or factories. Its explicit
limitation tooltip explains the same identity-layer approximation used for
TextButton: retain the real SDK child (or an empty non-null widget for a null
child), not invented project content or geometry.

Omission/reset preserves theme inheritance. The SDK foreground builder receives
the original nullable child; the background builder wraps the padded result.
Effective local or inherited builders automatically select `Clip.antiAlias`;
without builders the SDK selects `Clip.none`. An explicit clip setting wins.
The real button, its hover/press/focus/disabled states and hit testing remain
active, while project callbacks and layer code remain isolated.

## AppBar scroll notification predicate (Flutter 3.44.8)

`notificationPredicate` preserves `default`, `depthZero` and `all` presets and
also accepts a non-null project `ScrollNotificationPredicate` reference.
Canvas receives anonymous presence only and never executes project code. A
configured reference shows an explicit limitation tooltip and uses the actual
SDK depth-zero predicate as an approximation. Omission and `default` use that
same SDK default; `all` also accepts nested-depth notifications.

AppBar uses accepted scroll updates to determine scrolled-under elevation;
this predicate does not consume notifications or stop their propagation.
Changing presence or resetting it preserves the real AppBar state, preferred
size, child identity and existing scroll position.

## Scaffold bottom-sheet scrim builder (Flutter 3.44.8)

`bottomSheetScrimBuilder` accepts an optional non-null project reference with the
exact type `Widget? Function(BuildContext, Animation<double>)`. The nullable
return can suppress a scrim in generated application code; explicit callback
null is invalid. Canvas receives anonymous presence and never executes the
builder. A configured reference adds an explicit preview limitation and retains
Flutter's actual default builder. Omission uses that same SDK default directly.

The default scrim remains a non-dismissable black ModalBarrier controlled by the
SDK animation: bottom-sheet coverage above 70% maps to animation values up to
1 at 100%, with the SDK opacity bounds of 0.1 and 0.6. Preview changes do not add
a scrim when no sheet requests one, hide an active default scrim, replace the
Scaffold state or change the existing three child slots. Tests exercise the
captured SDK builder, live animation and ScaffoldState.showBottomSheet.

## NotificationListener preview (Flutter 3.44.8)

The reviewed `flutter.widgets.NotificationListener` projection contains the
required `notificationType`, optional nullable `onNotification` and required
child. All fourteen SDK Notification filter types use actual typed
`NotificationListener<T>` widgets. These include base/layout/scroll families,
metrics, overscroll indicators, draggable sheets, keep-alive and navigation.
The exact child layout and model identity remain intact across type changes.

Present handlers become local callbacks that return false; omitted/null handlers
stay null. Application code never runs, and preview callbacks cannot consume
framework or Designer notifications. A custom project subtype is reduced to
anonymous presence. Its explicit tooltip explains the Notification-base preview
approximation, because Canvas cannot reproduce project-specific type filtering.
The saved model and generated Dart preserve the exact project type and callback.
Required-child wrapping is atomic; zero-sized children retain native geometry
and external selection handles. Model protocol 19 and transport 1 remain unchanged.

## MouseRegion preview (Flutter 3.44.8)

The reviewed MouseRegion projection covers all three mouse callbacks, cursor,
opaque, nullable hitTestBehavior and optional child without changing model
protocol 19. Callback and Dart object identities become presence markers, never
executable application code. The real MouseRegion installs inert local callbacks
only for present handlers; omitted/null handlers stay absent. Real mouse movement,
exit and unmount do not change Designer selection or override child selection.

All 41 closed cursor presets are supported. WidgetStateMouseCursor presets resolve
against the SDK's empty state set. A custom project MouseCursor reference has an
explicit preview-unavailable tooltip and uses MouseCursor.defer as a preview-only
approximation; the exact stored reference and generated Dart are unchanged.
Designer-only cursor/opaque annotations are suppressed within MouseRegion and its
subtree so real cursor inheritance, overlapping regions, opaque and hit testing
remain observable. Leaving that subtree restores ordinary Designer cursor hints.

Opaque defaults to true and accepts both literal values; State consumer metadata
never enters the runner or changes its stored literal preview. Omitted/null
hitTestBehavior retains the native MouseRegion default, while all three explicit
HitTestBehavior values remain exact. Empty nodes preserve native layout and have
external zero-size selection/drop handles. Renderer identity survives callback,
cursor and preview-availability changes. Native and internal Web share this path.

## Listener preview (Flutter 3.44.8)

The reviewed `flutter.widgets.Listener` projection covers all nine raw pointer
callbacks, `behavior` and its optional `child`, without changing model protocol
19. Callback values are reduced to typed presence markers; explicit null and
omission install no callback. The real Listener receives inert local no-op
callbacks, never application handlers, objects or source code. Raw hover,
cancel, scroll and pan/zoom notifications do not change Designer selection or
compete in the gesture arena. The existing Designer selection handlers retain
the deepest child selection.

All three HitTestBehavior values use the real RenderPointerListener behavior;
omission retains `deferToChild` and explicit null is rejected. Empty Listener
nodes keep their actual layout and get external selection/drop handles when
zero-sized. Render object identity survives callback changes. The offline Web
build uses the same decoder, renderer and drop logic.

## GestureDetector preview (Flutter 3.44.8)

The reviewed `flutter.widgets.GestureDetector` projection supports all 58
constructor callbacks, all six configuration properties and the optional
`child`. Model protocol 19 adds the closed `pointerDeviceKindSet` value:
`touch`, `mouse`, `stylus`, `invertedStylus`, `trackpad`, `unknown`.
An omitted or explicitly null set admits SDK-default devices; an explicitly
empty set remains empty. Unknown names, duplicate entries, extra JSON keys
and executable callback metadata are rejected.

The renderer constructs a real GestureDetector. Application callbacks and
object references are reduced to presence markers; only inert local Designer
selection callbacks are installed. Cancellation callbacks are no-ops so a
losing ancestor recognizer cannot overwrite a child's Designer selection.
Designer selection remains available even when the application-supported
device set excludes the current pointer. The SDK's pan/scale and competing
axis recognizer assertions are checked before rendering. Configuration defaults,
nullable behavior, semantics exclusion, drag start and trackpad scale options
follow the pinned SDK. Empty zero-sized detectors retain real zero-size layout
and use external Designer selection/drop handles, never persisted placeholder
children.

The internal offline Web artifact can be reproduced from this directory with
the pinned SDK (this does not enable the Web provider):

```powershell
flutter build web --release --target lib/main_web.dart --no-tree-shake-icons --no-web-resources-cdn --pwa-strategy none --no-wasm-dry-run
```

The explicit `--no-wasm-dry-run` keeps the generated bootstrap at exactly one
reviewed dart2js/CanvasKit build. The SDK's default dry run adds an empty second
build record, which the strict artifact admission correctly rejects.

After changing runner sources, regenerate exact byte sizes/SHA-256 values in
`CanvasRunnerBundle.manifest`; after rebuilding Web, refresh every existing
file entry in `WebCanvasArtifact.manifest`. Do not weaken the packaged-source
size/hash or Web artifact admission checks.

## Internal browser-host foundation (not product-routed)

This package now also has a `main_web.dart` entry point for the optional
runtime-faithful Web Canvas. It reuses the same bounded runtime and model decoder
through browser-specific I/O and uses Flutter direct-DOM multi-view so the host
can add and remove a browser-managed view without an iframe or image transfer.
The Windows hosting architecture is fixed to a windowed Microsoft Edge WebView2
child controller owned by a narrow native Win32 adapter beneath the heavyweight
AWT carrier. That internal host foundation is implemented and physically smoke-
gated on Windows x64, but it is not selected by the product yet.

`web/canvas_bridge.js` deliberately becomes available only when
`window.chrome.webview` and an exact host-injected lowercase 256-bit session
nonce are present. Its JSON envelopes fix the bridge format, version, direction
and nonce; canonical base64 chunks are bounded and sequenced, pre-registration
buffering is capped, diagnostics are truncated and close is terminal. Foreign
nonces are ignored, while a malformed, oversized or non-contiguous envelope
carrying the exact session nonce terminates the channel and reports failure to
both Dart and the host. The Dart
transport independently requires the same nonce, contiguous host/runner
sequences and a one-megabyte decoded chunk bound before feeding the unchanged
NBFC stream. These transport checks authenticate one host-created session; they
do not grant model, file, Save, Undo/Redo or mutation authority.

The Web host profile observes Flutter binding metrics so every browser resize
invalidates stale interaction geometry and republishes an exact layout. It never
installs or advertises the Windows native OLE Palette DnD method channel.

Focused Dart tests cover invalid nonces, nonce drift, replay/gaps/out-of-order
input, malformed/non-canonical/oversize chunks, ordered immutable output and
idempotent close. The manual headless-browser harness in
`test/web/canvas_bridge_browser_case.html` executes the production JavaScript
bridge for foreign, malformed, oversized, sequence, envelope and pending-queue
cases. Flutter 3.44.8 also completes a deterministic release Web build from the
offline dependency cache with local CanvasKit output. The current English-only
bundle registers local Roboto, carries its Apache-2.0 license and routes the
fallback-font base to packaged assets. That remains only a compiler/static-bundle
proof until paired with the separately implemented host boundary.

The internal boundary now includes installed-Runtime detection, a manifest-
pinned WebView2 loader/native adapter with Microsoft license and notice, native
COM STA/controller lifecycle, leased private artifact publication, ABI-v2
native rehash and immutable in-memory resource snapshots, one nonce/generation-
derived HTTPS `.invalid` origin without a disk fallback, exclusive per-session
user-data ownership, exact resource/navigation and frozen CSP policy,
authenticated Java NBFC streams, process-failure reporting and bounded teardown.
Runtime admission requires `100.0.1185.39+`, uses the same native compatible
target and retains COM capability probes. Its standalone physical smoke accepts
exact page/bridge authentication, an NBFC hello round trip with frame-digest
verification, read-back-verified bounds/visibility, focus API completion and a
deadline-bounded close. Product provider/build/cache/
session routing, exact Web `CanvasEngineIdentity`, production Retry and the full
assembled NetBeans model/layout/selection, DPI, isolation and cleanup matrix are
still pending. Until those gates pass, the product continues to route Web only
to the native Windows-engine responsive layout preview described below. Input
acceptance is English-only; physical CJK IME and other language-specific input
remain deferred to the final internationalization phase.

After the bounded version 1 lifecycle handshake, stdin/stdout NBFC frames carry
strict runtime control and one digest-described canonical model payload. When
`asset.imageBytes.v1` is negotiated, `host.render` then carries its sorted image
descriptors and one NBFC `IMAGE_BYTES` frame per descriptor. Each frame and the
aggregate are bounded by `maxEncodedImageBytes`; identity, order, size and
SHA-256 are verified before admission. Media signature, declared dimensions and
a real Flutter decode are then checked per resource: a failure quarantines that
resource while the valid peers remain admissible. The reviewed
decoder accepts exactly `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`,
`Column`, `Row`, `Text`, `Icon`, `Image`, `Padding`, `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`,
`ListView`, `GridView.count`, `SingleChildScrollView`, `Wrap`, `Container`, `Opacity`, `Transform`, `RotatedBox`, `PreferredSize`, `ListBody`,
`OverflowBar`, `SafeArea`, `ColoredBox`, `Placeholder`, `Directionality`,
`DecoratedBox`, `Builder`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge`, `CircleAvatar`, `LinearProgressIndicator`, `CircularProgressIndicator`, `RefreshProgressIndicator`, `RefreshIndicator`, `TextButton`, `OutlinedButton`, `FilledButton`, `FloatingActionButton`, `IconButton`, `Checkbox`, `Switch` and `Slider`,
with reviewed typed properties and slots. It
rejects unknown widgets, fields and values instead of loading arbitrary project
Dart code.
Model protocol v18 carries the resolved project-theme id, seed, brightness,
46-role ColorScheme override table, 15-role TextTheme override table and the
closed 36-leaf component-color table. Version 17 adds the presence-only typed
project Dart-object reference used by the clipping widgets' `clipper` rows and
`ClipPath.shape`; version 16 added the top-level
physical/directional finite non-negative elliptical border-radius value used by
`ClipRRect`; version 15 added the exact payload-free null used by
`IndexedStack.index`. The
runner applies the same seed → `ColorScheme.copyWith` → `ThemeData.from` →
`TextTheme.copyWith` → component-theme order as generated Dart before applying
form-local widget properties. It preserves physical `EdgeInsets` versus text-direction-aware
`EdgeInsetsDirectional` for exact Padding preview parity and adds a strict
typed nullable `IconData` value. The runner constructs the real Flutter
`Icon` with the same 13 positional/named constructor arguments as generated
Dart. For the reviewed built-in, strict admission permits only **None** or an
exact bundled Material glyph; arbitrary/custom font metadata is rejected.
`Icon` is a leaf. Omitted size, fill, weight, grade, optical size, color,
shadows, and text-scaling behavior inherit `IconTheme`; `blendMode` and
`fontWeight` remain direct local arguments. Invisible or zero-size results
retain a designer-only selectable target without changing Flutter layout. The
host's searchable Material registry has
8,825 entries locked to Flutter 3.44.8; Material-font glyphs require the
generated application to keep `flutter.uses-material-design: true`.

`AppBar` adds the same 120 typed flattened leaves and five named slots used by
the Java catalog and generated Dart. The runner assembles the closed
notification-predicate, shape, icon-theme, text-style and system-UI-overlay
projections into real Flutter objects. AppBar also carries the reviewed
`PreferredSizeWidget` trait used by `Scaffold.appBar` and `AppBar.bottom`.

`Container` supports the reviewed `BoxDecoration` contract including a typed
asset-only `DecorationImage`. Logical `AssetImage` and `ExactAssetImage`
identity remains in the model, while host resolution supplies either one raw
SHA-256 resource id plus its chosen variant scale or a closed unavailable
status with a concrete reason. The runner constructs `MemoryImage(bytes,
scale: resolvedScale)` and, when present, one bounded `ResizeImage` wrapper.
It maps fit, directional alignment, nine-patch center slice, repeat,
text-direction matching, decoration scale, opacity, filter quality, inversion
and anti-aliasing, plus mode/matrix/gamma/saturation color filters. Serialized
`onError` is presence-only and is never executed. Authenticated image frames
that fail signature, media, dimension, full-decode, resize-target, or
center-slice checks are quarantined per resource instead of closing the Canvas
session. Missing, unreadable, and quarantined corrupt assets use a deterministic
checker preview and a fixed path-free accessibility status; framing, digest,
identity, ordering, bounds, and exact model-resource coverage remain fatal.
Selection outlines, inset guides and Palette/move overlays stay outside the
decorated/transformed widget.

`ElevatedButton` adds the same 286 typed leaves and optional-single
required-named-nullable child slot as the Java catalog. Protocol v12 carries
callback presence only, never callback identifiers, and the runner installs
inert typed closures. It constructs direct sparse default/disabled/pressed/
hovered/focused `ButtonStyle` values. Scalar leaves use disabled, pressed,
hovered, focused and enabled/default precedence. Compound `Size`, `BorderSide`,
shape and `TextStyle` leaves layer the enabled/default fragment, then active
focused, hovered and pressed fragments independently over the atomic
`ElevatedButtonTheme` or Flutter-default value; disabled uses only its own
fragment over that inherited value. Partial axes, locale/decoration members,
explicitly empty lists and background Paint/color replacement therefore remain
sparse instead of acquiring Canvas-only defaults. If no inherited `fixedSize`
exists, a missing axis uses Flutter's infinity sentinel; a finite effective
maximum clamps it exactly as `ButtonStyleButton` does. When a local minimum or
maximum applies, Canvas resolves the pair together and widens each maximum axis
to its effective minimum; states without a local bound return a null pair to
Flutter's normal theme/default fallback. Each active-state font
package is applied after the current layered `TextStyle`, including TextTheme
and lower active-state leaves. A fallback-only package replaces one prior
package prefix without manufacturing `.../null`. Local text theme/inherit
configuration must publish one transition-safe inherit mode across all
reachable states. An empty button renders with `child: null`.

The runner renders exact compatible responsive/adaptive profiles on its bound
Windows Flutter engine. Android, iOS, macOS and Linux targets are applied through
Flutter's `ThemeData.platform`; they test Flutter adaptive widget appearance but
do not claim a device operating system, plugin or platform-channel runtime. A
Web target uses its browser-sized responsive viewport with Windows adaptive
controls as an explicit native-engine layout preview. It does not claim
`kIsWeb`, DOM, browser fonts, plugins or platform-channel behavior; those require
the separately compiled browser runner plus the still-pending WebView2 product
route and assembled acceptance gate. Each widget retains its stable `.fd` UUID;
revision-, presentation- and layout-bound messages synchronize read-only
selection between Flutter hit testing and the NetBeans Explorer tree. Every
rendered widget has a non-layout-affecting thin dashed outline; the selected
widget replaces it with the solid blue selection outline. Physical and
direction-aware `Padding` values also paint thin orange distance guides from the
outer bounds to the resolved inner bounds. Outline widths, dash lengths and
guide caps compensate for the current viewport scale, so their on-screen weight
remains stable across Fit and manual zoom.

Every physical pointer-down anywhere on the native Canvas surface—including
the surrounding field outside the logical Flutter viewport and the runner-owned
scrollbars—emits one strict `runner.interaction` event. The event binds the exact
session, presentation, document, logical revision, frame, layout and monotonic
intent identity. Java advances a host-owned unsigned monotonic interaction fence
whenever a pending native-focus request is cleared, projects the exact fence to
the current presentation as `host.interactionFence`, and the runner first emits
the strict six-field `runner.interactionFenceApplied` acknowledgement. Until
that exact acknowledgement has been flushed, the runner shows a concise
`Synchronizing input…` overlay and gates pointer selection, scrolling, Delete
and native Palette drop admission. It echoes the applied epoch in every later
interaction. Java first authenticates the process session and then admits the
one-shot intent only when its exact visible layout and echoed fence are both
current; stale-fence, foreign, out-of-order and replayed events are consumed
without focus or model authority. The current fence is priority-coalesced and
replayed after a replacement presentation or runner becomes authoritative,
without overtaking a frame already being written. A bounded missing-ACK timeout
keeps input fail-closed and publishes typed synchronization state to the host;
a late exact current ACK may recover, while replacement and teardown ACKs stay
inert.

The logical viewport never follows the native child-window size. The optional
`viewport.presentation.v1` control keeps `MediaQuery` fixed while applying a
view-only `Fit` or 25–200% manual paint transform. When manual zoom overflows,
Flutter draws and owns both scrollbars and handles wheel, Shift+wheel and
Ctrl+wheel interaction in the same geometry used for hit testing and native
Palette drops. Runner metrics echo the exact host command sequence; presentation
state is per Design tab and is not persisted in the model.

The negotiated `surface.presentation.v1` edge makes each post-frame
`runner.presented` acknowledgement carry the exact implicit `FlutterView`
physical width, physical height and device-pixel ratio in millionths. The
runner reads these only after the replacement frame, validates them against the
negotiated physical-dimension and pixel budgets, and coalesces a rapid native
resize burst into one contiguous layout sequence. On Windows an embedded
`WS_CHILD` always refits to its parent client bounds; per-monitor DPI messages
never apply a top-level suggested screen rectangle to that child, while a
standalone runner retains the normal suggested-rectangle behavior.

An empty Row, Column, ListView, GridView, ListBody, OverflowBar, Wrap, FittedBox, ConstrainedBox,
UnconstrainedBox, LimitedBox, OverflowBox, RotatedBox, PreferredSize or ColoredBox keeps its real Flutter layout (including
zero-size outcomes) but receives a non-layout-affecting 36-pixel-minimum
selection outline and hit rectangle. Its whole bounded rectangle is insertion
index zero; a populated Row, Column, ListView, GridView, ListBody or OverflowBar exposes only its terminal append
edge. Stack uses its full rendered rectangle for both empty and populated
z-order appends. Wrap also uses its full rendered rectangle for every terminal
append because run formation has no single stable edge. ListView and ListBody
resolve their terminal edge from vertical/horizontal, reverse and LTR/RTL visual
order. GridView resolves incomplete-group insertion along its cross axis and
complete-group insertion along its main axis, including `reverse` and ambient
LTR/RTL direction. OverflowBar derives whether its real render object is in a fitting row or
vertical overflow column, then uses effective text direction or
`overflowDirection` for that edge.

The optional host-driven `widget.movePreview.v1` route projects an existing-
widget tree move without giving the runner mutation authority. A command carries
the source stable ID, destination parent/slot/post-removal index, exact session/
presentation/document/revision/frame/layout identities and a monotonic preview
sequence. The runner excludes the source subtree while resolving list geometry
and paints a thin amber before/between/after marker or compatible empty-single/
container zone, distinct from blue selection and Palette feedback. A newer
command or explicit clear wins; clear, render/layout replacement, hide and
shutdown remove the overlay. If the capability is absent, the Swing-tree move
still works without this disposable projection.

The runner receives no project path, Dart source, file handle, `SaveCookie`,
Undo/Redo or Designer-command authority. NetBeans owns editable Properties and
the catalog-admitted Palette insertions and existing-widget moves for the exact
89 reviewed Canvas widgets. Palette insertion evaluates 6,675 exact
source/destination cells across 89 draggable sources and 75 insertable reviewed
slots; 6,286 are accepted and 389 cells are rejected. Expanded and Flexible are
admitted only as atomic wrappers over an existing direct Row/Column child,
cannot wrap either wrapper type, and expose required replacement-only child
slots that are excluded from the insertion matrix. They also cannot wrap
Spacer; Spacer is inserted only into direct Row/Column children and never wraps
another widget. SafeArea, Directionality, ExcludeFocus, ExcludeFocusTraversal, Visibility, TickerMode, DefaultTextHeightBehavior, DefaultSelectionStyle, IconTheme, RefreshIndicator, TextButton, OutlinedButton, IconButton and RadioGroup are generic atomic wrappers around
an existing widget,
never an empty required-slot prototype. The current Canvas target wire exposes
non-root child targets only and intentionally offers no root target; root
wrapping remains available through the NetBeans tree. None of these wrappers can wrap
Expanded, Flexible or Spacer because their ParentData must remain attached
directly to Row or Column. The negotiated
source-aware command binds the opaque token to the current reviewed type and
traits before Flutter exposes compatible hover zones. The
runner only renders validated revisions, performs
bounded hit testing, returns revision-bound Palette intents and paints optional
move feedback. Catalog JSON is reserved for a future versioned
catalog contract, and Linux/macOS native hosts remain separate work.

`flutter.widgets.Opacity` is decoded without a protocol-version change: required
finite `opacity` is in inclusive `[0, 1]`, optional
`alwaysIncludeSemantics` defaults to false when omitted, and `child` is one
optional single any-widget slot. The runner builds the real Flutter `Opacity`,
not `AnimatedOpacity` or a paint approximation. Opacity zero does not disable
child hit testing; it normally suppresses child semantics, while explicit
`alwaysIncludeSemantics: true` retains them. Designer selection, hit and drop
overlays wrap the Opacity and remain visible outside its paint/semantics effect,
including for an empty zero-size child target. The same contract is used by the
native and exact-Web renderers.

`flutter.widgets.Align` is also decoded without a protocol-version change. Its
optional alignment is the existing finite physical/directional
`AlignmentGeometry` value; omission resolves to `Alignment.center`. Nullable
width and height factors reuse the finite non-negative numeric contract, with
zero and values greater than one admitted, and `child` is one optional single
any-widget slot. The runner builds a real Flutter `Align`, resolves directional
coordinates through the current LTR/RTL `Directionality`, and keeps an IDE-only
selection/drop target when an empty factor-driven instance has zero layout size.

`flutter.widgets.ListView` is decoded as the non-const static
`ListView(children: ...)` contract without a protocol-version change. Its 17
optional leaves cover axis/reverse/primary, six closed physics presets,
shrink-wrap, non-negative padding/item/cache extents, child keep-alive/repaint/
semantic-index flags, bounded semantic child count, drag and keyboard behavior,
restoration ID, clipping and hit testing. Numeric cache extent becomes
`ScrollCacheExtent.pixels`, and semantic child count cannot exceed the decoded
static list length. Controller-owned state, builders/delegates,
`itemExtentBuilder`, `prototypeItem`, deprecated `cacheExtent`, `key` and raw
Dart are absent from the wire contract. The renderer constructs the real
ListView. Its constraint guard supplies width 240 or height 120 whenever the
viewport cross axis is unbounded, and on an unbounded main axis only when
`shrinkWrap` is false. The
originally agreed eight-item core Palette is complete 8/8 with this slice; that
does not claim every Flutter widget.

`flutter.widgets.Wrap` is the first post-core Canvas widget. Its const default
constructor is decoded with nine optional typed leaves: axis, child/run
alignment, finite signed spacing and run spacing, cross-axis alignment, text
and vertical direction, and clipping. The optional ordered `children` list is
the twentieth any-widget destination. The renderer constructs a real Flutter
Wrap, so run formation and directionality remain framework-owned. Empty Wraps
keep their real zero-size layout behind the 36-pixel Designer target. Empty and
populated Wrap nodes expose the full rendered rectangle for deterministic
terminal append at `children.length`. No model or wire version changes. At the
Wrap milestone the aggregate catalog had 21 widgets, 17 reviewed const
constructors and 643 writable properties, including 626 outside Scaffold.

`flutter.widgets.FittedBox` is the second post-core Canvas widget. The decoder
admits exactly three optional fields: all seven `BoxFit` values, finite physical
or directional `AlignmentGeometry`, and the four reviewed `Clip` values; its
`child` is one optional single any-widget slot. Omission resolves to
`BoxFit.contain`, `Alignment.center` and `Clip.none`. Native and exact-Web
renderers construct the real Flutter FittedBox, so Flutter owns scaling,
directional alignment resolves through the active LTR/RTL `Directionality`, and
overflow clipping follows the selected clip behavior. An empty zero-size node
retains its real layout behind the non-layout-affecting 36-pixel Designer
selection/drop target. No model or wire version changed at that milestone.

`flutter.widgets.ConstrainedBox` is the third post-core Canvas widget. The
decoder requires one typed `constraints` field and admits one optional single
any-widget `child`. Each width and height axis supports `0..infinity`, a finite
upper bound, a finite lower bound, a finite range, a tight value or expanding
`infinity..infinity`; an infinite minimum paired with a finite maximum is
rejected. Native and exact-Web renderers construct the real Flutter
ConstrainedBox, preserving authentic additional-constraint behavior. Empty
zero-size nodes retain their real layout behind a bounded, non-layout-affecting
Designer selection/drop target. Nullable minima advance the Canvas model to
v12; NBFC framing and control/wire stay v1. At that milestone the aggregate
catalog had 23 widgets, 18 reviewed const constructors and 647 writable
properties, including 630 outside Scaffold. Twenty-three sources across 22
any-widget and two trait destinations formed 552 candidates: 488 accepted and
64 rejected. The practical Material/Base Designer backlog was 23/92 complete
with 69 remaining, and the Palette Layout category contained 15 items.

[`flutter.widgets.UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
is the fourth post-core Canvas widget, at Palette Layout order 107. Its pinned
Flutter 3.44.8 const constructor has an optional `child` first, followed by
optional `textDirection`, `alignment`, `constrainedAxis` and `clipBehavior`.
The decoder admits those four optional fields plus one optional single
any-widget child. A new model stores no property defaults. Omission preserves
`Alignment.center`, no retained axis and `Clip.none`; ambient `Directionality`
resolves directional alignment when `textDirection` is omitted. Native and
exact-Web renderers construct the real Flutter UnconstrainedBox, removing
constraints on both axes or retaining exactly the selected horizontal or
vertical axis. Empty zero-size nodes retain their real layout behind a bounded,
non-layout-affecting Designer selection/drop target. At that milestone the
aggregate catalog had 24 widgets, 19 reviewed const constructors and 651
writable properties, including 634 outside Scaffold. Twenty-four sources across
23 any-widget and two trait destinations formed 600 candidates: 533 accepted
and 67 rejected. The practical Material/Base Designer backlog was 24/92 complete
with 68 remaining, and the Palette Layout category contained 16 items.

[`flutter.widgets.LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
is the fifth post-core Canvas widget, at Palette Layout order 108. Its pinned
Flutter 3.44.8 const constructor has optional `maxWidth`, optional `maxHeight`
and an optional `child` in that order. The decoder admits finite non-negative
double limits; omission canonically resolves to `double.infinity`. Native and
exact-Web renderers construct the real Flutter LimitedBox and prove that a
configured maximum affects only an axis whose incoming maximum constraint is
unbounded. Empty zero-size nodes retain their real layout behind a bounded,
non-layout-affecting Designer selection/drop target. At that milestone the
aggregate catalog had 25 widgets, 20 reviewed const constructors and 653 writable properties,
including 636 outside Scaffold. Twenty-five sources across 24 any-widget and two
trait destinations formed 650 candidates: 580 accepted and 70 rejected. The
practical Material/Base Designer backlog was 25/92 complete with 67 remaining,
and the Palette Layout category contained 17 items. `.fd` schema v7, Catalog API
6, Canvas model v12 and NBFC
framing/control/wire v1 remain unchanged.

[`flutter.widgets.OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
is the sixth post-core Canvas widget, at Palette Layout order 109 between
LimitedBox and Stack. Its pinned Flutter 3.44.8 const constructor has optional
alignment, four optional finite non-negative double constraint overrides,
exact `OverflowBoxFit.max`/`deferToChild`, and an optional child. The decoder
rejects integer wire kinds, non-finite or negative values, and any present
minimum above its matching maximum. Omitted bounds inherit the corresponding
parent constraints; omitted alignment and fit preserve `Alignment.center` and
`OverflowBoxFit.max`. The enum is owned by `package:flutter/rendering.dart`.
Native and exact-Web renderers construct the real Flutter OverflowBox and prove
constraint override, overflow, physical/directional LTR/RTL alignment and both
fit modes. Empty or zero-size nodes retain their real layout behind a bounded,
non-layout-affecting Designer selection/drop target. At that milestone the
aggregate catalog had 26 widgets, 21 reviewed const constructors and 659
writable properties, including 642 outside Scaffold. Twenty-six sources across
25 any-widget and two trait destinations formed 702 candidates: 629 accepted
and 73 rejected. The practical Material/Base Designer backlog was 26/92
complete with 65 remaining, and the Palette Layout category contained 18
items. `.fd` schema v7, Catalog API 6, Canvas model v12 and NBFC
framing/control/wire v1 remain unchanged.

[`flutter.widgets.Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
is the seventh post-core Canvas widget, at Palette Layout order 130 immediately
after Expanded. Its pinned Flutter 3.44.8 const constructor exposes optional
non-negative portable integer `flex`, optional exact
`FlexFit.loose`/`FlexFit.tight`, and one required single any-widget `child`.
Omission preserves `flex: 1` and `FlexFit.loose`; zero flex is valid and remains
inflexible. The renderer constructs the real Flutter Flexible directly below
Row or Column, placing Designer instrumentation inside its child so Flutter's
ParentDataWidget path remains valid. Palette, tree and Canvas creation wrap an
existing direct Row/Column child atomically; Flexible and Expanded cannot wrap
either wrapper type. The occupied required child is replacement-only, cannot be
cleared and is not an insertable destination. Positive loose flex may use less
than its allocation, while positive tight flex fills it. At that milestone the
aggregate catalog had 27 widgets, 22 reviewed const constructors and 661
writable properties, including 644 outside Scaffold. Twenty-seven sources
across the unchanged 25 insertable any-widget and two trait destinations formed
729 candidates: 631 accepted and 98 rejected. The practical Material/Base
Designer backlog was 27/92 complete with 65 remaining, and the Palette Layout
category contained 19 items. `.fd` schema v7, Catalog API
6, Canvas model v12 and NBFC framing/control/wire v1 remain unchanged.

[`flutter.widgets.Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html)
is the eighth post-core Canvas widget, at Palette Layout order 140 immediately
after Flexible. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes one optional
positive portable integer `flex`; omission preserves `flex: 1`, while zero,
negative and over-limit values are rejected. Spacer has no child or other slot.
Palette, tree and Canvas creation insert it only into direct `Row.children` or
`Column.children`; it never wraps an existing child. Expanded and Flexible
cannot wrap Spacer because Spacer internally creates the Expanded parent-data
path that must remain directly below Row or Column. Native and exact-Web
renderers construct the real Flutter Spacer directly under the Flex. Because an
outer render-object wrapper would invalidate that path, Designer selection, hit
testing and outlines use only the surface overlay. At that milestone the
aggregate catalog had 28 widgets, 23 reviewed const constructors and 662
writable properties, including 645 outside Scaffold. Twenty-eight sources
across the unchanged 25 insertable any-widget and two trait destinations formed
756 candidates: 633 accepted and 123 rejected. The practical Material/Base
Designer backlog was 28/92 complete with 64 remaining, the Palette Layout
category contained 20 items, and no later widget had an explicit order. `.fd`
schema v7, Catalog API
6, Canvas model v12 and NBFC framing/control/wire v1 remain unchanged.

[`flutter.widgets.Baseline`](https://api.flutter.dev/flutter/widgets/Baseline-class.html)
is the ninth post-core Canvas widget, at Palette Layout order 150 immediately
after Spacer. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required finite signed `double baseline`,
required exact `TextBaseline.alphabetic`/`TextBaseline.ideographic`, and one
optional single any-widget `child`. Designer prototype creation uses
`baseline: 24.0` and `TextBaseline.alphabetic`; wrong kinds, non-finite values
and raw Dart are rejected. Palette, tree and Canvas creation admit Baseline as
an ordinary widget, while generic placement rules reject Expanded, Flexible and
Spacer in its child slot. Native and exact-Web renderers construct the real
Flutter Baseline; an empty or collapsed instance retains a bounded 36 x 36
Designer selection target without changing generated Dart. The aggregate
catalog at that milestone had 29 widgets, 24 reviewed const constructors and 664 writable
properties, including 647 outside Scaffold. Twenty-nine sources across 26
insertable any-widget and two trait destinations formed 812 candidates: 684
accepted and 128 rejected. The practical Material/Base Designer backlog was
29/92 complete with 63 remaining, and the Palette Layout category contained 21
items. `.fd` schema v7, Catalog API
6, Canvas model v12 and NBFC framing/control/wire v1 remain unchanged.

[`flutter.widgets.IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight-class.html)
is the tenth post-core Canvas widget, at Palette Layout order 160 immediately
after Baseline. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` has no writable properties and accepts one
optional single any-widget `child`. Palette, tree and Canvas creation admit it
as an ordinary widget, while generic placement rules reject Expanded, Flexible
and Spacer in its child slot. Native and exact-Web renderers construct the real
Flutter IntrinsicHeight, preserving parent constraints and the speculative
intrinsic-height pass. The Palette contract warns that intrinsic measurement is
relatively expensive and can be O(N²) in tree depth. An empty or collapsed
instance retains a bounded 36 x 36 Designer selection/drop target without
changing generated Dart or Flutter layout. At that milestone the aggregate
catalog had 30 widgets, 25 reviewed const constructors and 664 writable
properties, including 647 outside Scaffold. Thirty sources across 27 insertable
any-widget and two trait destinations formed 870 candidates: 737 accepted and
133 rejected. The practical Material/Base Designer backlog was 30/92 complete
with 62 remaining, and the Palette Layout category contained 22 items.

[`flutter.widgets.IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth-class.html)
is the eleventh post-core Canvas widget, at Palette Layout order 170 immediately
after IntrinsicHeight. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional finite non-negative
`stepWidth`, optional finite non-negative `stepHeight`, and one optional single
any-widget `child`. Omission and explicit zero are preserved as distinct model,
history and Dart states even though Flutter treats both as unsnapped on the
corresponding axis. Positive values snap the child's extent upward to a multiple
of that step, while incoming parent constraints remain authoritative. Palette,
tree and Canvas creation admit IntrinsicWidth as an ordinary widget; generic
placement rules reject Expanded, Flexible and Spacer in its child slot. Native
and exact-Web renderers construct the real Flutter IntrinsicWidth and preserve
its speculative intrinsic-width pass. The Palette contract warns that intrinsic
measurement is relatively expensive and can be O(N²) in tree depth. An empty or
collapsed instance retains a bounded 36 x 36 Designer selection/drop target
without changing generated Dart or Flutter layout. At that milestone the
aggregate catalog had 31 widgets, 26 reviewed const constructors and 666
writable properties, including 649 outside Scaffold. Thirty-one sources across
28 insertable any-widget and two trait destinations formed 930 candidates: 792
accepted and 138 rejected. The practical Material/Base Designer backlog was
31/92 complete with 61 remaining, and the Palette Layout category contained 23
items. `.fd` schema v7, Catalog API 6, Canvas model v12 and NBFC
framing/control/wire v1 remain unchanged.

[`flutter.widgets.Offstage`](https://api.flutter.dev/flutter/widgets/Offstage-class.html)
is the twelfth post-core Canvas widget, at Palette Layout order 180 immediately
after IntrinsicWidth. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional boolean `offstage` with runtime
default `true`, plus one optional single any-widget `child`. Omission and
explicit `true` remain distinct model, history and Dart states even though both
hide the child; explicit `false` participates normally. Palette, tree and Canvas
creation admit Offstage as an ordinary widget; generic placement rules reject
Expanded, Flexible and Spacer in its child slot. Native and exact-Web renderers
construct the real Flutter Offstage. When hidden, Flutter still lays the child
out and keeps it active and focusable, including animations, but suppresses
paint, hit testing and semantics and normally reports zero size under loose
constraints. The Palette contract exposes that resource cost and recommends
removing long-hidden subtrees when ongoing work is undesirable. Selection and
drop instrumentation remains outside the Offstage effect; an actual zero-sized
result alone receives a bounded 36 x 36 non-layout-affecting target. At that
milestone the aggregate catalog had 32 widgets, 27 reviewed const constructors
and 667 writable properties, including 650 outside Scaffold. Thirty-two sources
across 29 insertable any-widget and two trait destinations formed 992
candidates: 849 accepted and 143 rejected. The practical Material/Base Designer
backlog was 32/92 complete with 60 remaining, and the Palette Layout category
contained 24 items. `.fd` schema v7, Catalog API 6, Canvas model v12 and NBFC
framing/control/wire v1 remained unchanged.

[`flutter.widgets.SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox-class.html)
is the thirteenth post-core Canvas widget, at Palette Layout order 190
immediately after Offstage. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required structured `Size size`, optional
`AlignmentGeometry alignment` with runtime default `Alignment.center`, and one
optional single any-widget `child`. A new Designer instance persists the
reviewed visible `Size(100, 100)` value; both dimensions are finite,
non-negative and edited atomically. Palette, tree and Canvas creation admit
SizedOverflowBox as an ordinary widget, while generic placement rules reject
Expanded, Flexible and Spacer in its child slot. Native and exact-Web renderers
construct the real Flutter SizedOverflowBox. Its own requested size is
constrained by the parent, while the child receives the original incoming
constraints and may paint outside according to alignment; hit testing remains
inside the parent's bounds. A true zero-sized result retains a bounded 36 x 36
non-layout-affecting Designer selection/drop target. At that milestone the aggregate catalog had
33 widgets, 28 reviewed const constructors and 669 writable properties,
including 652 outside Scaffold. Thirty-three sources across 30 insertable
any-widget and two trait destinations form 1,056 candidates: 908 accepted and
148 rejected. The practical Material/Base Designer backlog was 33/92 complete
with 59 remaining, and the Palette Layout category contains 25 items. The new
closed atomic Size wire value advances `.fd` to schema v8, Catalog API to 7 and
Canvas model to v13; NBFC framing/control/wire v1 remain unchanged.

[`flutter.widgets.Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
is the fourteenth post-core Canvas widget. Model protocol v14 adds the first
atomic signed finite `Offset` value for its optional `origin`. The exact
`Transform.new` projection requires a finite column-major 16-entry `Matrix4`,
optionally resolves physical or directional `AlignmentGeometry`, preserves
`transformHitTests` with its Flutter default of `true`, accepts nullable
`FilterQuality.none/low/medium/high`, and exposes one optional single any-widget
`child`. New prototypes send `Matrix4.identity()` while leaving every optional
argument absent.

Native and exact-Web views construct the real paint-time `Transform` outermost
over Designer instrumentation. Layout therefore remains the child's real
untransformed size, while painted content, selection geometry and drop-target
geometry share Flutter's effective origin/alignment matrix. Child hit testing
shares that matrix only when `transformHitTests` is true; false preserves the
untransformed hit-test coordinate space.
Non-finite projected bounds, projective-horizon crossings and non-invertible
point projection fail closed without an invented drop region; finite
rotated/skewed targets additionally require exact local containment rather than
an axis-aligned bounding-box corner. Surface hover feedback is still outside
the transformed subtree. Only a true
zero-size result receives the bounded 36 x 36 non-layout-affecting Designer
selection/drop target. The named rotate, translate, scale and flip convenience
constructors are intentionally outside this slice. At that milestone the aggregate catalog had
34 widgets; 34 sources across 33 insertable destinations formed 1,122 cells,
with 969 accepted and 153 rejected. Canvas model protocol is v14; NBFC framing,
control and wire remain v1.

[`flutter.widgets.RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
is the fifteenth post-core Canvas widget. Its exact constructor projection
requires one signed integer `quarterTurns` value and exposes one optional
single any-widget `child`. Palette creation starts at one clockwise quarter
turn. The runner accepts the full portable signed-integer range from
`-9007199254740991` through `9007199254740991`; missing, mistyped and
out-of-range values fail closed before rendering.

Native and exact-Web views construct the real layout-time `RotatedBox`. Odd
quarter turns therefore exchange the child's width and height during layout,
negative values rotate counter-clockwise, and multiples of four preserve its
orientation. Empty or truly zero-size instances receive only the bounded
Designer selection/drop target, while populated instances expose the real child
geometry and the optional `child` drop slot. At that milestone the aggregate catalog had 35
widgets; 35 sources across 34 insertable destinations form 1,190 cells, with
1,032 accepted and 158 rejected. The practical Material/Base backlog was 35/92
complete with 57 remaining, and Layout contains 27 items. Canvas model protocol
remains v14; NBFC framing, control and wire remain v1.

[`flutter.widgets.PreferredSize`](https://api.flutter.dev/flutter/widgets/PreferredSize/PreferredSize.html)
is the next complete Layout Canvas widget. Its exact projection requires the
finite non-negative `Size preferredSize` and a required single `child`; palette
creation supplies `Size(100, 56)`. The native and exact-Web views construct the
real `PreferredSize`, advertising the stored size without constraining the
child. It is a required-child wrapper source, so an existing non-root child is
wrapped atomically and the required slot is not offered as an empty insertion
target. The `PreferredSizeWidget` trait remains available to AppBar/Scaffold
slots. The renderer and runtime schema are shared with the native process and
the packaged Web artifact.

[`flutter.widgets.ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html)
is the sixteenth post-core Canvas widget. Its exact constructor projection
accepts optional `mainAxis` (`Axis.vertical` or `Axis.horizontal`) and optional
`reverse`, preserving Flutter's vertical/non-reversed defaults when absent, plus
one ordered any-widget `children` list. Unknown values, wrong kinds and unknown
properties fail closed before rendering.

Generated Dart remains a bare real `ListBody`. Because `RenderListBody` requires
an unbounded main axis and bounded cross axis, the native and exact-Web Canvas
host the real widget inside an axis-matched design-time viewport. That guard is
Canvas-only and never changes the model or generated application source. An
empty ListBody exposes its bounded rectangle at insertion index zero; a populated
instance exposes only its visual terminal append edge, resolved from main axis,
reversal and ambient `Directionality`. At that milestone the aggregate catalog had 36 widgets;
36 sources across 35 insertable destinations form 1,260 cells, with 1,097
accepted and 163 rejected. The practical Material/Base backlog was 36/92
complete with 55 remaining, and Layout contains 28 items. Canvas model protocol
remains v14; NBFC framing, control and wire remain v1.

[`flutter.widgets.OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
is the seventeenth post-core Canvas widget. Its exact constructor projection
accepts optional finite signed `spacing`, nullable `alignment`, finite signed
`overflowSpacing`, `overflowAlignment`, `overflowDirection`, nullable
`textDirection`, and one ordered any-widget `children` list. Unknown values,
wrong kinds and unknown properties fail closed before rendering.

Native and exact-Web views construct the real `OverflowBar`. For a nonempty
node, a Canvas-only `LayoutBuilder` supplies a finite 240-pixel width only when
the incoming width is unbounded and `alignment` is non-null; null alignment
retains Flutter's natural width. A 36x36 minimum retains an empty selection/drop
target; neither guard changes the model or generated application source. The real renderer uses
a horizontal row while child widths plus spacing fit and a vertical overflow
column otherwise. DnD resolves fitting-row order through explicit or ambient
LTR/RTL direction and overflow-column order through `overflowDirection`. The
aggregate catalog at that milestone had 37 widgets; 37 sources across 36 insertable
destinations form 1,332 cells, with 1,164 accepted and 168 rejected. The
practical Material/Base backlog was 37/92 complete with 55 remaining, and Layout
contains 29 items. Canvas model protocol remains v14; NBFC framing, control and
wire remain v1.

[`flutter.widgets.GridView`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
is decoded as the exact static `GridView.count(children: ...)` projection for
Flutter 3.44.8 without a protocol-version change. Its 21 writable leaves cover
axis/reverse/primary, six closed physics presets, shrink wrapping, non-negative
physical or directional padding, required positive `crossAxisCount` (Designer
creation default 2), non-negative main/cross spacing, positive
`childAspectRatio`, nullable non-negative `mainAxisExtent`, the three child
delegate flags, pixel scroll cache extent, bounded semantic child count, drag
and keyboard behavior, restoration ID, clipping and hit testing. One optional
ordered any-widget `children` list is the only slot. Controller-owned state,
delegates/builders, deprecated `cacheExtent`, other constructors, `key` and raw
Dart are absent from the closed model.

Native and exact-Web views construct the real `GridView.count`; Flutter owns
RTL/reverse/scroll-direction placement, and a non-null `mainAxisExtent`
naturally overrides aspect-derived tile extent in its fixed-count delegate. A
shared generated/Canvas guard supplies width 240 whenever a vertical grid lacks a bounded
cross axis, height 120 whenever a horizontal grid lacks one, and the matching
main-axis bound for a non-shrink-wrapped viewport. An empty grid keeps a bounded
index-zero insertion target. A populated grid exposes a 36-pixel terminal band
beside the logical last tile; move preview uses a 12-pixel row-major marker at
the referenced tile. Partial groups advance on the cross axis, complete groups
advance on the main axis, with `reverse` and ambient LTR/RTL resolved from the
real render tree. At the `GridView.count` milestone, the aggregate catalog had
38 widgets; 38 sources across 37 insertable destinations formed 1,406 cells,
with 1,233 accepted and 173 rejected. Canvas model protocol remained v14; NBFC
framing, control and wire remained v1.

[`flutter.widgets.SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
is decoded as the exact const default-constructor projection for Flutter 3.44.8
without a protocol-version change. Its 10 optional leaves cover scroll axis,
reversal, non-negative physical or directional padding, nullable primary
policy, six closed physics presets, drag-start behavior, clipping, hit testing,
restoration ID and keyboard dismissal. One optional any-widget `child` is the
only slot. Controller-owned state, arbitrary physics graphs, `key` and raw Dart
are absent from the closed model.

Native and exact-Web views construct the real `SingleChildScrollView`. Flutter
owns axis/reverse/directionality behavior and deliberately shrink-wraps the
widget in both axes, so the ListView/GridView bounded-viewport guard is not
applied. Canvas retains only a non-layout-affecting 36x36 selection/drop target
when the real empty or zero-size widget has no usable bounds. The aggregate
catalog at that milestone had 39 widgets; 39 sources across 38 insertable destinations formed
1,482 cells, with 1,304 accepted and 178 rejected. Canvas model protocol remains
v14; NBFC framing, control and wire remain v1.

[`flutter.widgets.ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is decoded as the exact const default-constructor projection for Flutter 3.44.8
without a protocol-version change. Its required `color` accepts an exact ARGB
literal or one reviewed Material `ColorScheme` theme token; detached creation
uses `Color(0xFF2196F3)`. Optional `isAntiAlias` remains absent to preserve its
Flutter default `true`, and one optional any-widget `child` is the only slot.
`key` and arbitrary color expressions are absent from the closed model.

Native and exact-Web views construct the real `ColoredBox`. Literal generation
remains const; a theme token resolves through `Theme.of(context).colorScheme...`
and produces a non-const widget. An empty ColoredBox has no intrinsic size, so
Canvas retains only a non-layout-affecting 36x36 selection/drop target when its
real bounds collapse to zero; that overlay never enters the model or generated
Dart. At the `ColoredBox` milestone, the aggregate catalog had 40 widgets; 40
sources across 39 insertable destinations formed 1,560 cells, with 1,377
accepted and 183 rejected. Canvas model protocol remained v14; NBFC framing,
control and wire remained v1.

[`flutter.widgets.SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
is decoded as the exact const default-constructor projection for Flutter 3.44.8
without a protocol-version change. Optional `left`, `top`, `right` and `bottom`
preserve `true` when omitted; optional `minimum` preserves `EdgeInsets.zero`;
and optional `maintainBottomViewPadding` preserves `false`. `minimum` accepts
signed finite physical `EdgeInsets` only. `EdgeInsetsDirectional`, non-finite
components and arbitrary Dart expressions are rejected. The required
any-widget `child` slot has exact cardinality one; `key` is excluded.

Native and exact-Web views construct the real `SafeArea`. Host creation wraps an
existing widget atomically, so Canvas never decodes a half-empty required child.
The current target wire publishes only non-root children as wrapper targets and
deliberately publishes no root target. Expanded, Flexible and Spacer are not
targets because their ParentData must remain directly attached to Row or Column.
At the SafeArea milestone the aggregate catalog had 41 widgets and 35 reviewed const definitions, with
722 writable rows (705 outside Scaffold). Palette contained 30 Layout, three
Scrolling, four Basic and four Material items; the backlog was 41/92 complete
with 51 remaining. The 41 sources across 39 insertable destinations formed 1,599
cells, with 1,414 accepted and 185 rejected. `.fd` remains v9 and Canvas model
protocol remains v14; the exported `EdgeInsetsValues.directionalAllowed`
constraint advances Catalog API to v9. NBFC framing, control and wire remain v1.

[`flutter.widgets.Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is decoded as the complete const default-constructor projection for Flutter
3.44.8 without a protocol-version change. Optional `color` preserves
`Color(0xFF455A64)` when omitted and accepts either an exact ARGB literal or one
reviewed Material `ColorScheme` theme token. Optional `strokeWidth`,
`fallbackWidth` and `fallbackHeight` preserve `2.0`, `400.0` and `400.0`
respectively; each accepts only a finite, non-negative integer or double. One
optional any-widget `child` is the only slot. `key`, non-finite or negative
numbers and arbitrary Dart expressions are excluded from the closed model.

Native and exact-Web views construct the real Flutter `Placeholder`. Its real
fallback dimensions apply only on unbounded axes. Literal values retain const
generation; resolving a theme color through `Theme.of(context).colorScheme`
removes const only from that generated widget. The empty widget remains
selectable and exposes its optional-child drop target; after population the
child remains selectable and movable, while the occupied single slot rejects a
second child. If an explicit zero fallback dimension collapses a real layout
axis, Canvas supplies only a transient selection/drop target and does not alter
the model or generated Dart.

At the Placeholder milestone the aggregate catalog had 42 widgets and 36
reviewed const definitions, with 726 writable rows (709 outside Scaffold).
Palette contained 30 Layout, three Scrolling, five Basic and four Material
items; the backlog was 42/92 complete with 50 remaining. The 42 sources across
40 insertable destinations formed 1,680 cells, with 1,490 accepted and 190
rejected. `.fd` remained v9, Canvas model protocol remained v14, Catalog API
remained v9 and NBFC framing, control and wire remained v1.

[`flutter.widgets.Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is decoded as the complete const default-constructor projection for Flutter
3.44.8 without a protocol-version change. Required `textDirection` admits only
`TextDirection.ltr` or `TextDirection.rtl`; Flutter supplies no constructor
default, so host creation persists explicit `ltr`. The required any-widget
`child` is always present in admitted Canvas payloads. `key`, arbitrary Dart
expressions and half-empty wrappers are excluded from the closed model.

Native and exact-Web views construct the real inherited `Directionality` around
the decoded child. Directional alignment, padding and text beneath it therefore
resolve through the selected value; Designer selection, hit and drop overlays
remain outside the wrapper. Host Palette creation is one atomic wrap around an
existing root or non-root widget. The current Canvas target wire exposes only
non-root wrapper targets, while the tree retains root wrapping. Expanded,
Flexible and Spacer are rejected as targets because their ParentData must remain
directly attached to Row or Column. The occupied required-child slot is
replacement-only and excluded from the insertion matrix.

At the Directionality milestone the aggregate catalog had 43 widgets and 37 reviewed const definitions,
with 727 writable rows (710 outside Scaffold). Palette contained 30 Layout,
three Scrolling, six Basic and four Material items; the backlog was 43/92
complete with 49 remaining. The 43 sources across 40 insertable destinations
formed 1,720 cells, with 1,528 accepted and 192 rejected. `.fd` remained v9,
Canvas model protocol remained v14, Catalog API remained v9 and NBFC framing,
control and wire remained v1.

[`flutter.widgets.DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is decoded as the complete reviewed const default-constructor projection for
Flutter 3.44.8 without a protocol-version change. Required `decoration` is the
existing complete typed BoxDecoration value; host creation persists an exact
empty rectangular `BoxDecoration()`. Optional `position` admits only
`DecorationPosition.background` and `DecorationPosition.foreground`, with
omission preserving the exact background default. `child` is one optional
single any-widget slot. Custom Decoration subclasses, raw expressions and a
missing required decoration are rejected.

Native and exact-Web views construct the real `DecoratedBox` around its optional
child. Background and foreground paint use the decoded color, asset image,
border, physical/directional radii, shadows, gradients, blend mode and shape;
Designer selection, hit and drop affordances remain outside the paint effect.
An empty zero-size widget retains only a transient Designer target and does not
change the model or generated Dart.

At the DecoratedBox milestone the aggregate catalog had 44 widgets and 38
reviewed const definitions, with 729 writable rows (712 outside Scaffold).
Palette contained 30 Layout, three Scrolling, seven Basic and four Material
items; the backlog was 44/92 complete with 48 remaining. The 44 sources across
41 insertable destinations formed 1,804 cells, with 1,607 accepted and 197
rejected. `.fd` remained v9, Canvas model protocol remained v14, Catalog API
remained v9 and NBFC framing, control and wire remained v1.

[`flutter.widgets.ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
is decoded as the complete const default-constructor projection for Flutter
3.44.8 without a protocol-version change. Optional `excluding` admits only a
boolean and omission preserves the exact `true` default. `child` is one optional
single any-widget slot; `key` and arbitrary expressions remain outside the
closed model.

Native and exact-Web views construct the real `ExcludeSemantics` around its
optional child. Omitted or explicit `true` removes the application child's
semantics subtree; explicit `false` passes it through. Layout, paint and hit
testing continue to proxy the child. The `ExcludeSemantics` node's own Designer
selection, hit/drop and accessibility wrapper stays outside the semantics
effect. Descendant Canvas semantics labels follow the real subtree exclusion;
the separate NetBeans widget tree remains accessible. An empty zero-size widget
retains only a transient Designer target.

At that milestone the aggregate catalog had 45 widgets and 39 reviewed const definitions,
with 730 writable rows (713 outside Scaffold). Palette contained 30 Layout,
three Scrolling, seven Basic, four Material and one Accessibility item; the
backlog was 45/92 complete with 47 remaining. The 45 sources across 42
insertable destinations formed 1,890 cells, with 1,688 accepted and 202 rejected.
`.fd` remained v9, Canvas model protocol remained v14, Catalog API remained v9 and
NBFC framing, control and wire remain v1.

[`flutter.widgets.IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
is decoded as the complete const Flutter 3.44.8 projection. Optional
`alignment`, `textDirection`, `clipBehavior` and `sizing` preserve their exact
framework defaults when omitted. Nullable `index` has three distinct states:
omission uses default zero, a non-negative integer selects one existing child,
and the exact payload-free null value selects none. The ordered `children` list
remains complete even though only the selected child paints, hits and contributes
application semantics; layout uses the largest child.

Native and exact-Web views construct the real IndexedStack. Every concrete
index is revalidated against the live child list, including Flutter's empty-list
index-zero exception. The full rendered rectangle supplies overlapping-list DnD;
an empty zero-size node retains only the bounded transient Designer target.

At that milestone the aggregate catalog had 46 widgets and 40 reviewed const definitions,
with 735 writable rows (718 outside Scaffold). Palette contained 31 Layout,
three Scrolling, seven Basic, four Material and one Accessibility item; the
backlog was 46/92 complete with 45 remaining. The 46 sources across 43
insertable destinations formed 1,978 cells, with 1,771 accepted and 207 rejected.
Exact typed null advances `.fd` to v10, Canvas model protocol to v15 and Catalog
API to v10; NBFC framing, control and wire remain v1.

`ClipRect` is decoded as the real const Flutter 3.44.8 widget. Its optional
`clipper` accepts only `dartObjectReferencePresence`; its optional `clipBehavior` accepts only the four exact `Clip` values and defaults to
`Clip.hardEdge` when omitted; its optional single child participates in normal
catalog insertion. With no clipper presence, native and exact-Web routes
apply the real layout, paint, hit-test and semantics behavior. With presence,
the runner preserves the child and shows an accessible preview-unavailable overlay without
pretending to execute project Dart. Selection and
empty-target feedback remain transient Designer overlays outside the clip. If
the child is omitted and the real node is therefore zero-size, the external
36x36 Designer target carries the visible warning, full tooltip and semantics
reason without fabricating Flutter layout geometry.

`ClipOval` is decoded as the real const Flutter 3.44.8 widget. Its optional
`clipper` accepts only `dartObjectReferencePresence`; its optional `clipBehavior` accepts only the four exact `Clip` values and defaults to
`Clip.antiAlias` when omitted; its optional single child participates in normal
catalog insertion. With no clipper presence, native and exact-Web routes
apply the real oval layout, paint, hit-test and semantics behavior, with the
default oval inscribed in the child's layout bounds. Selection and empty-target
feedback remain transient Designer overlays outside the clip. With presence,
the runner preserves the child and shows the same accessible unavailable preview;
an empty node exposes that reason on its external 36x36 Designer target.

`ClipRRect` is decoded as the real Flutter 3.44.8 widget. Its optional
`borderRadius` accepts physical `BorderRadius` or directional
`BorderRadiusDirectional` with finite, non-negative elliptical X/Y values for
all four corners and defaults to `BorderRadius.zero` when omitted. Its optional
`clipper` accepts only `dartObjectReferencePresence`; the runner never receives
the library URI, symbol, member or invocation details and never executes project
Dart. When presence is set, it preserves the child and renders an explicit
accessible preview-unavailable overlay without applying `borderRadius`, which
Flutter ignores for a non-null custom clipper. Its optional `clipBehavior`
accepts only the four exact `Clip` values and defaults to `Clip.antiAlias` when
omitted; its optional single child participates in normal catalog insertion.
Without clipper presence, native and exact-Web routes construct the real widget
and apply its layout, paint, hit-test and semantics behavior. Selection and
empty-target feedback remain transient Designer overlays outside the clip; an
empty custom-clipper node remains zero-size while that external target carries
the warning and complete reason.

`ClipPath` adds the real default rectangular path clip with all four `Clip` values
and optional child. Typed `clipper` (`CustomClipper<Path>`) and `shape`
(`ShapeBorder`, selecting `ClipPath.shape` in generated Dart) arrive only as
`dartObjectReferencePresence`; both together are rejected. Custom geometry cannot
run in this isolated process, so its child remains visible with an explicit,
accessible preview-unavailable message. Selection/drop frames stay outside clipping,
and zero-size targets retain the complete branch-specific reason. Windows/Web
profile tests cover the default clip and both custom branches.

`ClipRSuperellipse` constructs the real Flutter widget/RenderClipRSuperellipse,
not a rounded-rectangle approximation. Windows/Web profile tests cover physical,
directional, elliptical and oversized radii, Flutter clamping, all four clip
behaviors and an optional zero-size child. Exact typed custom delegates arrive
only as presence: the child stays visible with an accessible preview-unavailable
warning and the otherwise retained radius is ignored. Selection/drop affordances
remain external even when the custom node has no child. This extends the reviewed
v17 model contract without adding project-code execution or a protocol bump.

`PhysicalModel` adds a real physical layer with required fill color, shadow color,
finite non-negative elevation, rectangle/circle shape, all four clip modes and
physical-only elliptical radii. Both colors resolve from literal ARGB or reviewed
theme tokens. Decoder admission and the exact fingerprint reject directional radii,
including on a circle. The renderer preserves the radius value when the circle
ignores it; non-square circle bounds form an oval. Windows/Web tests exercise
native paint/shadows, translucent occluders, radius clamping, theme colors, property
changes and optional child. Empty nodes keep external selection/drop affordances
without fabricated paint or layout. That milestone retained Canvas model v17.

`PhysicalShape` uses real PhysicalShape and ShapeBorderClipper with six reviewed
ShapeBorder classes: rounded rectangle, beveled rectangle, continuous rectangle,
rounded superellipse, circle and stadium. The v18 `shapeBorderClipper` payload has
exactly kind, shape, borderRadius and nullable textDirection fields. Cornered shapes
accept physical/directional elliptical radii; directional corners require explicit
LTR/RTL because the SDK helper does not read ambient directionality. Circle/stadium
retain ignored radii/direction. Colors, all four clip modes, elevation/shadows, theme
resolution and SDK radius clamping are real Flutter paint, on Windows/Web profiles.
The alternative project clipper is presence-only: preserve its child with an
accessible preview-unavailable warning, never fabricate fill, shadow or clipping.
Childless targets retain full warning text outside actual zero-size widget layout.

RepaintBoundary constructs the actual Flutter RepaintBoundary/RenderRepaintBoundary
with its optional child and no scalar properties. It owns a real independent
OffsetLayer: descendant paint dirtiness stops at the boundary, and ancestor repaint
can reuse the clean inner layer. Windows/Web profile tests verify retained layers,
layout/constraints/intrinsics, semantics/hit testing, live descendant edits,
child replacement/removal and external zero-size selection/drop targets. No helper
child, clipping, forced caching policy or performance guarantee is introduced. SDK
wrap/wrapAll only derive keys; the Designer's default constructor keeps stable
model identity. This slice keeps model 18, schema 13 and Catalog API 14 unchanged.

IgnorePointer constructs the real Flutter IgnorePointer/RenderIgnorePointer with
optional child, ignoring default true and ignoringSemantics default null. The
deprecated semantics boolean is supported deliberately, with a narrow analyzer
suppression only at the SDK call site. Ignored subtrees still paint and lay out,
but hit testing may reach widgets behind them. Null retains semantic labels and
blocks actions when ignoring; false preserves actions; true excludes the subtree.
Native/Web profile tests cover the complete boolean/semantics matrix, live updates,
layout, pass-through hit testing and external Designer selection/drop affordances.
The ignored node's generic body selection/opaque mouse region is disabled; its
36px external Designer handle tries all adjacent placements to avoid covering even
a small body at a viewport corner. If no outside placement fits, only the compact
explicit handle overlaps the body, not a full-body selection layer. Geometric DnD
and tree-selected Text F2/keyboard commit remain independent of pointer filtering.
No replacement AbsorbPointer, fake child or protocol change is introduced.

AbsorbPointer uses actual AbsorbPointer/RenderAbsorbPointer, absorbing default true,
deprecated ignoringSemantics default null and optional child. The deprecated SDK
call is narrowly suppressed rather than omitting the compatibility branch. Unlike
IgnorePointer, absorbing terminates hit testing at its own size, so both child and
lower Stack sibling are blocked. Layout/paint are unchanged. Semantics null retains
labels but blocks actions while absorbing, false keeps actions, true excludes the
subtree. Native/Web model-profile tests cover retained updates, complete semantics,
absorption versus pass-through, nullable/tight layout, empty external targets,
child replacement/removal, geometric DnD and tree-selected Text F2/keyboard commit.
The AbsorbPointer body retains normal Designer selection; IgnorePointer's external
nonempty-handle/transparency policy is not applied to it. Versions stay unchanged.

BlockSemantics constructs actual BlockSemantics/RenderBlockSemantics with optional
blocking (omitted true) and nullable child. Its semantics flag removes previously
painted nodes below the same semantic boundary, retaining its child and later
content. The complete Canvas tests verify paint order and container scope through
Designer instrumentation, live updates, childless/tight layout, ordinary hit testing,
selection, child edits, DnD and F2 editing. It is not a pointer blocker or descendant
semantics exclusion. Empty targets remain external; no fake content is introduced.

MergeSemantics uses actual MergeSemantics/RenderMergeSemantics with an optional
child and no scalar fields. The render object declares a semantic boundary and
merges descendant semantics into one node, with normal layout, paint, intrinsic
sizing and ordinary hits. Labels concatenate with newlines; competing handlers
for the same action follow the first in tree order. Conflicting states retain SDK
behavior rather than being rejected or reconciled by the Designer. Identity,
external empty targets, child editing and tree-selected descendant editing remain
available. Inside the merged subtree, omit only synthetic Designer identity/
selected annotations and GestureDetector semantics so they cannot replace real
widget actions; keep pointer editing and actionable preview diagnostics. Scope
propagates through nested descendants and disappears when content is moved outside.
Full Canvas semantics tests include Designer instrumentation and the existing
isolation of project callbacks, not merely a standalone SDK factory.

IndexedSemantics uses actual IndexedSemantics/RenderIndexedSemantics with required
signed integer index and an optional child. The exact projection uses the shared
native/Web integer range -9007199254740991..9007199254740991 and rejects missing,
null, noninteger and out-of-range values. Index 0 is a host prototype value, not
a decoder fallback or SDK default. The render object annotates the first child
semantic node and otherwise proxies layout, paint, intrinsic sizing and hits.
Like MergeSemantics, its descendants omit only synthetic Designer semantics.
Unlike MergeSemantics, IndexedSemantics has no isolating SDK boundary, so its own
outer Designer annotations/actions must also be omitted; accessible wrapper
identity and editing remain in the NetBeans widget tree. No boundary is inserted;
an indexed anonymous parent for several real semantic children remains valid SDK
topology, with no forced leaf indexes. Outside-scope Designer ancestors keep their
own semantics, which the SDK may combine with a non-boundary child. Test scope
ownership rather than claiming the entire Designer tree equals an uninstrumented
application tree. Actual widget semantics,
preview diagnostics, pointer/F2/tree editing and geometric DnD remain available.
Moving content outside these wrappers restores ordinary Designer contributions.
Manual ListView indexes require explicit parent addSemanticIndexes/semanticChildCount
settings; no silent parent changes or automatic sibling renumbering are introduced.

ExcludeFocus uses actual ExcludeFocus with optional excluding (omitted true) and
a required child. Its internal SDK Focus cannot request focus, skips traversal,
adds no semantics and sets descendantsAreFocusable to !excluding. Enabling
exclusion unfocuses a focused descendant; disabling it does not automatically
refocus that descendant. Descendant FocusNode settings are not rewritten. Normal
layout, paint, pointer hits and semantic labels remain. Required-child wrapping
uses existing non-root Canvas targets, never an empty prototype or a fake child;
root wrapping remains a NetBeans tree operation. The required slot is replaceable,
not an insertable destination, and cannot be cleared. Exact Boolean/slot validation
and existing payload versions apply. Only the temporary F2 Text editor attaches
its service focus branch to the outer Designer FocusNode, leaving real application
controls excluded. Move-preview geometry rejects dragging a direct required child
out of its wrapper; intact wrappers and optional/list children remain movable,
and final mutation validation remains host-authoritative.

ExcludeFocusTraversal uses the actual SDK widget with optional excluding (omitted
true) and a required child. Its internal Focus sets descendantsAreTraversable to
!excluding, canRequestFocus false, skipTraversal true and includeSemantics false.
It removes descendants from Tab/Shift+Tab traversal but permits direct focus
requests and retains existing focus, subject to other focus restrictions. Local
configured skipTraversal flags are not rewritten; the effective getter observes
ancestor exclusions. Nested false does not cancel an outer exclusion. Labels,
layout, paint, pointer hits and F2 service editing remain. The same generic wrapper
classification covers geometry, required-child move rejection and atomic wrapping;
no synthetic child, root target, new authority or semantic boundary is introduced.

Visibility uses the real SDK widget with required child, optional non-null
replacement and all seven booleans. Missing/empty replacement maps to the SDK
SizedBox.shrink default. Decode validates the five maintenance implications even
when visible; all six maintenance flags true exactly match Visibility.maintain.
The actual widget controls subtree disposal/retention, ticker muting, hidden size,
paint, semantics, pointer hits and focusability. Generic wrapping accepts one
required child plus valid-empty optional slots, and replacement is an insertable
destination. Synthetic selection/drop geometry follows only the active visible
branch: child when visible, replacement when hidden without maintained state.
Hidden retained content still has its real SDK behavior but no synthetic child
handles/drop targets or F2 resurrection. Tree/property editing remains available.
Changing maintenance flags can discard runtime state, as documented by Flutter;
that is not deletion of the saved Designer subtree.

Pinned Flutter 3.44.8's _RenderVisibility.visible setter marks paint but not
semantics dirty. A raw SDK regression reproduces stale semantics with maintained
size and interactivity; rebuilding children can additionally assert when revealing
them. A Canvas-only state holder invalidates the existing SDK render object's
semantics before visible changes from a maintained-size branch. It adds no
semantic boundary and does not replace paint/layout/hit/focus behavior. Dynamic
Canvas tests require correct semantics rather than swallowing SDK exceptions.
Generated application Dart still uses the upstream Visibility directly.

TickerMode renders the actual SDK widget with required enabled and child plus
optional forceFrames (omitted false). Decoder validation rejects a missing or
non-Boolean enabled, but false with forceFrames=true is valid. Effective enabled
is the AND of local and ancestor values; effective forceFrames is their OR.
Widget-aware SingleTickerProviderStateMixin/TickerProviderStateMixin tickers are
muted, not paused: callbacks stop while elapsed time continues. The SDK retains
child state, paint/layout, labels, pointer hits and focus. Designer does not hide
disabled ticker subtrees, suppress F2 or fabricate an empty required child. Generic
wrapping/reparenting and nested Visibility use the same actual inherited context.
The static TickerMode.merge helper introduces no additional editable behavior:
its null requests have equivalent effective values to enabled=true and omitted
forceFrames in the bare constructor (the helper itself adds a Builder).

DefaultTextHeightBehavior renders the actual inherited theme with a required child
and an always-present TextHeightBehavior. Its three optional flattened leaves reuse
Text's names and codecs, with true/true/proportional defaults. The Text helper keeps
returning null for an entirely unset local Text value; only the required wrapper
falls back to const TextHeightBehavior(). Nearest wrappers replace the whole value,
not merge leaves. Descendant Text local/default-style precedence and EditableText
inheritance use the actual SDK. Dynamic text geometry, ordinary selection/semantics,
focus, F2, wrapping, reparenting and inherited-theme capture preserve the same
behavior without fake layout or a Canvas protocol change.

DefaultSelectionStyle renders the actual SDK constructor or .merge helper from
the required Designer merge flag, initially false. False replaces all fields, including
nulls; true inherits each omitted field. Both colors resolve the same typed
literal/semantic theme values as generation. The cursor mapper covers all 41
predefined SDK values, including defer/uncontrolled and the three WidgetState
cursors. adaptiveClickable follows kIsWeb, not a simulated viewport platform;
the Web build retains Flutter's real web branch. Existing TextField's domain is
unchanged. TextField cursor-color and selectable Text selection/cursor precedence,
inherited notifications and capture/wrap use the real SDK; the wrapper does not
make plain Text selectable or override TextField's own mouse cursor. Required-child
tree/Canvas wrapping, selection, F2 and lifecycle remain available in both modes.
The invalid-child fallback constructor is not rendered as an insertable widget.

IconTheme builds the real SDK widget or static merge helper with required
IconThemeData and child. All nine optional fields pass through unchanged, including
raw finite opacity outside 0–1; the SDK clamps effective opacity and multiplies
both inherited and explicit Icon color alpha, without fading shadows or the whole
subtree. Direct empty data replaces the outer theme with consumer fallback values;
merge uses nearest raw inherited data, preserving Flutter resolution and capture.
Explicit empty shadows and false text scaling override inherited values. Icon
continues forwarding null local arguments so its normal inherited/default/local
precedence remains intact. Required-child wrapping, semantics, selection and F2
remain available in both modes. No new protocol or host-side inheritance engine
is introduced; custom base-data subclasses are not serialized by this editor.

ImageIcon renders the actual pinned SDK widget with either explicit null or a
resolved typed provider. Null reserves the inherited/local size without loading
bytes or showing an asset error. Non-null image bindings reuse content-addressed
resources, exact scale, DPR-aware selection, ResizeImage policy/upscaling and safe
unavailable/rejected placeholders. Both relationship resource closure and runtime
image-use validation include ImageIcon, with no centerSlice and scale 1; the
Image-only center-slice rules remain unchanged. Real ImageIcon uses scaleDown,
one outer semantic label and an excluded child Image semantic node. It inherits
size/color and theme opacity, not shadows/font axes/text scaling; explicit local
color still receives theme opacity. SDK fallback color is black even though older
field prose suggests otherwise. No synthetic errorBuilder is added to ImageIcon;
late framework image-stream failures retain framework diagnostics. Ordinary DnD,
null/provider transitions and zero-size Designer selection use existing authority.

Divider uses the actual pinned Material Divider, with all six optional fields:
height, thickness, indent, endIndent, color and radius. Decoding reuses finite
non-negative numeric, theme-aware color and physical/directional radius contracts.
No resources, child slots, text editor or model-version changes are introduced.
Theme defaults remain real SDK defaults: local values then DividerTheme, then
M2/M3; space 16/zero indents, M2 hairline/dividerColor and M3 thickness 1/outlineVariant.
RTL changes leading/trailing margins and resolves directional elliptical corners.

The pinned bottom-only Border cannot paint a nonzero radius with a hairline in
debug; release ignores radius. Raw SDK and Canvas characterization retain that
limitation rather than silently changing geometry. Use positive thickness for a
rounded divider. Existing framework error forwarding remains authoritative; there
is no synthetic release-only guard or extra constructor rejection.

VerticalDivider renders the actual SDK vertical separator with optional width,
thickness, indent, endIndent, color and radius. The top/bottom margins are unchanged
by RTL, whereas directional corner geometry is resolved by Directionality.
DividerTheme/M2/M3 defaults and the left-only Border remain authoritative. Parent
height determines vertical extent; bounded Row/horizontal ListView and unbounded
layout behavior are tested without inventing a widget height or implicit wrapper.
Zero-width Designer handles preserve selection/drop while the widget remains a leaf
with no resources or text editor. The same rounded-hairline debug assertion and
release radius omission are characterized rather than replaced by a workaround.

Card renders actual Card/Card.filled/Card.outlined, preserving optional field
omission, CardTheme/M2/M3 precedence, optional child, margin, clipping, border paint
order and semantic-container behavior. Its strict 31-row projection admits ten
fully editable outlined shape constructors: rounded/beveled/continuous/superellipse
rectangles, circle, oval, stadium, linear, star and polygon. Directional elliptical
radii, side parameters, physical/directional insets and linear-edge alignment are
resolved by Flutter. Fractional points/sides and degree rotations remain unchanged.

Shape-reference presence and built-in fields are mutually exclusive; reject orphan
or inapplicable shape details and star rounding sums above one. Typed custom shape
code is never executed in this isolated runner: display explicit preview unavailable
while preserving child and selection. Apply the same explicit state before path
allocation for star/polygon counts above 4096, naming the count and operational
budget. This is not an SDK, generation or persisted-value restriction and no
synthetic simpler shape is presented as the requested one. Ordinary empty-child
and zero-size targets remain selectable/insertable; F2 is not text editing for Card.

Badge renders actual Badge or Badge.count from optional Count presence, preserving
all 41 fields and separate optional Label/Child slots. Count mode rejects a stored
Label; Max count requires Count. BadgeTheme falls back to M3 defaults in both M2
and M3 applications. Local textColor overrides style color, while foreground Paint
retains actual TextStyle.copyWith precedence. Physical/directional padding and
alignment, label offset plus the SDK's `(0, 8)` compatibility adjustment, small-dot
offset omission, intrinsic stadium sizing and count truncation remain SDK behavior.
Hidden label/dot leaves Child mounted; hidden Label subtrees remain in the model
but publish no mounted geometry or inline editor target. Both slots have usable
empty targets for tiny or hidden normal badges; count mode explicitly rejects Label
drop/move targets. No version or wire-value additions are required.
The pinned SDK stadium render object lacks updateRenderObject for minSize. Keying
only the actual Badge by its active effective largeSize refreshes that SDK render
object, while model GlobalKeys retain Child/Label Element, State, input and focus.
Raw SDK reproduction plus local/theme changes, both constructors and active F2
tests cover this preview-only workaround; no SDK file or generated Dart is patched.

CircleAvatar admits all nine optional constructor fields and Child. Render the real
SDK CircleAvatar, including M2/M3 color/text/icon inheritance, no child text scaling,
finite geometry/color animation and circular image decorations without clipping
arbitrary Child. Foreground paints above Child, background behind it. Resolve both
providers through the shared bounded resource closure, but omit an unavailable
layer with a property-specific diagnostic instead of masking fallback content with
the ordinary image checker. Decode failures report diagnostics and never invoke
user callback identifiers. Radius enum `double.infinity` is allowed only as that
exact pair; effective doubled min/max bounds and radius exclusivity are validated.
Changing constraint finiteness keys only the SDK shell to avoid unsupported
BoxConstraints interpolation, preserving the keyed child's state/focus. Empty and
zero-sized avatars keep external selection and Child insertion affordances.

LinearProgressIndicator renders the actual SDK widget for all 13 optional fields
at Material/order 100. No default Value or synthetic width is inserted; omitted
Value animates, finite Value uses SDK clamping. Color/theme/stopped-null animations
are projected locally; project valueColor/controller references retain explicit
preview-unavailable diagnostics and are never executed. The core capability union
admits signed finite gap/stop values and positive Infinity for height/stop/gap.
Value/controller conflicts are rejected. Preserve ProgressIndicatorTheme/M2/M3,
year2023 precedence, directional corners, RTL painting, TickerMode and semantics.
Unbounded width, unbounded infinite minimum height and the pinned M2 stop-color or
determinate semantics-value failure paths receive transparent diagnostics. No
diagnostic rewrites stored Dart values or supplies an invented size/color/string.

CircularProgressIndicator renders both material and adaptive constructors from a
single Material/order 110 model type. Its required variant plus 14 optional Material
fields reuse existing wire values; adaptive/color and value/controller conflicts
are rejected rather than repaired during decoding. Stroke width and alignment
accept signed finite numbers; gap additionally accepts positive Infinity. Actual
SDK paint/layout, M2/M3/theme precedence, stroke caps, directional padding,
constraints, semantics and TickerMode are retained. Computed nonfinite geometry,
overflowing padding or unbounded infinite minima produce explicit diagnostics.
Zero-sized arcs, signed strokes and infinite gaps are not arbitrarily rejected.
On Theme.platform iOS/macOS, adaptive uses real CupertinoActivityIndicator and
passes only backgroundColor/value/key as the pinned implementation does. Ignored
Material fields and project references are preserved without false preview errors;
on Material paths, project animation/controller code remains explicitly unavailable.

RefreshProgressIndicator renders all 12 constructor fields through the real SDK
at Material/order 120, with no stored defaults or slots. Omitted strokeWidth passes
the constructor default 2.5, while an explicit null wire value passes null for
theme/default 4 inheritance. It retains separate nonnegative physical/directional
indicatorMargin and indicatorPadding, nonnegative elevation, signed finite stroke
geometry/caps, colors and semantics. Stopped literal/theme/null color animations
render; project Animation<Color?> references remain explicitly unavailable.
The SDK owns the arc, refresh arrow, value-to-indeterminate state, theme defaults
and the foreground-opacity/Material-background split. Contextual paint/layout
diagnostics reject nonfinite resolved geometry or a visible arrow in a non-square
inner paint area without fabricating square constraints or changing model values.
This leaf does not implement the separate RefreshIndicator gesture wrapper.

RefreshIndicator adds Material/order 130 with 13 scalar rows and a required Child
wrapper slot. It renders actual material/adaptive/noSpinner SDK constructors and
preserves the child instead of replacing it with a callback-unavailable placeholder.
Reviewed notification presets and an unset refresh handler use the same async no-op
as generated Dart. Explicit project onRefresh/custom predicate disables preview
refresh activation with a concrete diagnostic; the actual required callback does
not pretend that project work completed. A project onStatusChange is reported as
unexecuted while the SDK cycle remains enabled. Programmatic show() is not exposed
as a Designer capability. Child scroll physics are never rewritten.
Variant/status/spinner conflicts fail closed during decoding. Displacement and
elevation are nonnegative finite; edgeOffset and strokeWidth retain signed finite
values. Resolved checks apply to active geometry and the actual selected SDK path,
not ignored Apple/noSpinner parameters. All three constructors, SDK notification/
status/async cycles, child preservation, themes, semantics, profiles and wrapping
have dedicated coverage without stored synthetic progress or application code.

IconButton extends Material at order 180 with standard, filled, filledTonal and
outlined const constructors. Its 524 typed rows comprise the complete direct
constructor surface, two required Designer selectors, all 498 local ButtonStyle
leaves, and a mutually exclusive strict whole-style project reference. The
required any-widget Icon slot is the palette wrapper target; Selected icon is an
optional destination in every constructor, including null/unset Selected.
Neither switching constructors nor changing selection deletes either subtree.

Actual public SDK IconButton constructors retain Material 2/3 and component-theme
semantics. Partial local size/density fields preserve the counterpart direct
constraint/density axis before consulting IconButtonTheme and a minimal pinned
public-API projection of compound defaults. Atomic style fields still use SDK
property-level merging; this is not an invented per-state scalar merge. Variant
colors, overlays and IconTheme behavior remain owned by the real SDK. Material 2
ignores style/selectedIcon/isSelected/statesController exactly as documented;
onLongPress is inactive without onPressed in both Material versions. The isolated
Canvas never executes project callbacks, controllers, styles or builders.

Catalog metadata now permits 1024 property descriptors per widget so this widget can
describe 524 mutually exclusive rows. Stored per-node properties, validation and
atomic patches remain limited to 512; legal dense maps fit within 505 entries.
The 2048-probe, 2 MiB and 45-second candidate limits and formats 13/14/18 stay
unchanged. Full typed editors, stable cells, native history, save/reopen/continued
editing, wrapper/slot guards and four SVGs are part of the same vertical slice.

FloatingActionButton extends Material at order 170 with all four const
constructors (standard/small/large/extended) and 78 typed rows. Standard/small/large
allow empty Child; extended requires the same Child as Label and alone admits
an optional Icon. It uses ordinary empty-destination palette creation, not a
required-child wrapper. All slot, source-detachment and constructor mutations
preserve the appropriate child contract.

The slice includes all direct parameters, all five elevation states, the full
22-field ShapeBorder reference/built-in projection, and 31 extended TextStyle
leaves, including paint, shadows, font features/variations, locale and fallback.
Hero tags use a closed unset/null/literal/non-null Object-reference union with
strict real-SDK witnesses; no raw Dart expressions are accepted. Canvas preserves
the real SDK default/literal Hero tags, diagnoses duplicates, and disables only
an unresolved custom Hero wrapper rather than fabricating a project object.
The actual SDK controls theme defaults, geometry and collapsed extended mode;
application callbacks and focus objects remain isolated. All four constructors
participate in generation, const proof, Save/reopen, editing and native history.
See [ADR-108](../../../../docs/DECISIONS.md#adr-108--floatingactionbutton-four-constructors-and-typed-hero-tags)
for the exact contract and verification evidence.

FilledButton adds Material/order 160 with all four standard/icon/tonal/tonalIcon
constructors and 510 typed fields: 11 direct controls, nine 54-leaf style buckets,
12 common fields and one strict whole ButtonStyle reference. It creates normally
without requiring an existing wrap target. Standard/tonal allow empty Child and
emit child:null; icon modes require a stable Label and accept an optional Icon.
Conditional label guards apply to remove, move and replacement-source operations.
Icon-to-tonalIcon transitions preserve subtrees/alignment; non-icon transitions
never delete an occupied Icon. All constructors omit Clip as Clip.none, distinguish
explicit null, and expose no isSemanticButton. Standard/tonal are const-capable.
Shared style assembly uses FilledButtonTheme and actual filled/tonal defaults.
All state/common styles, typed callbacks/controllers/builders, atomic edits,
Save/reopen/history, DnD and four SVGs belong to the same slice. Canvas preserves
local child state across modes, never executes project code and explicitly marks
whole-reference styles as an approximate SDK-default preview. Dense legal families
contain 490 standard/tonal, 491 rounded-icon or 464 circle-icon fields. Existing
button contracts, formats 13/14/18 and 512-property/2048-probe limits are unchanged.

OutlinedButton adds Material/order 150 with standard and icon constructors,
510 fields (11 direct, nine 54-leaf state buckets, 12 common style fields and a
whole ButtonStyle reference), required stable Child and optional Icon. It reuses
the full-style assembly and exact callback/controller/builder proof paths, while
resolving OutlinedButtonTheme and the actual OutlinedButton defaults. Both
constructors omit Clip behavior as null; neither accepts isSemanticButton. The
standard constructor is const-capable and icon is non-const. Its default outline
side overrides the shape's side; both are independently editable. Icon alignment
resolves constructor, theme, local style, then start. Whole/local style transitions
and occupied-icon constructor checks are atomic and preserve all child data.
Dense legal families contain 490 standard, 491 rounded-icon or 464 circle-icon
properties. Isolated Canvas
keeps real local interaction and child state, explicitly diagnoses project-code
and whole-style preview limits, and never executes project code. Properties,
DnD, persistence, further editing, native history and four SVGs complete the slice;
existing TextButton/ElevatedButton behavior, formats and capacity bounds remain.

TextButton adds Material/order 140 with standard and icon constructors. Its 511
scalar rows comprise 12 direct controls, nine 54-leaf state style buckets, 12 common
style fields and a strict whole ButtonStyle reference. Child is required and stable;
the optional Icon slot is admitted only by the icon constructor. Standard is
const-capable when all emitted arguments are const; the SDK icon factory is not.
Required Enabled/Constructor controls select valid activation and construction.
Disabled activation references remain stored without generated occurrences or
evidence obligations. Long-press-only emits null onPressed; an enabled button with
neither activation reference gets an explicitly documented no-op.

All eight WidgetStates plus default are editable, with disabled/error/dragged/
pressed/selected/scrolledUnder/hovered/focused/default priority. Arbitrary combined
state constraints and custom styles remain supported through strict project
ButtonStyle references, not raw Dart text. Whole style and all 498 local leaves
are mutually exclusive, with atomic Properties transitions. ButtonLayerBuilder,
VoidCallback, ValueChanged<bool>, FocusNode and WidgetStatesController references
retain exact current/imported/factory type proofs. Standard semantic role and
constructor-dependent Clip behavior distinguish omission, explicit null and
concrete values. Nullable Boolean values keep centered checkbox rendering.

Switching to icon clears standard-only semantics; setting iconAlignment selects
icon. Returning to standard rejects an occupied Icon with a clear move/remove-first
diagnostic instead of deleting it. The actual SDK Canvas preserves local button
interaction and child state across constructor/icon/diagnostic changes, with unique
per-button retained keys. It never executes project code: callbacks/controllers/
focus/builders are explicitly isolated, and a project-defined whole style is an
explicitly approximate SDK-default preview. ElevatedButton's existing 286-row
contract remains unchanged; shared style assembly uses the correct button's theme
and constructor defaults.

Dense legal 491-row/464-row style families round-trip; not all 511 fields may
coexist because of constructor and style exclusivity. The default per-widget codec
limit rises to 512 and the shared candidate budget to 2048 symbol probes, preserving
every occurrence, explicit smaller caller limits and the existing 2 MiB/45-second
bounds. Structured-list proof IDs now include property paths so independent state
buckets do not collide. Every typed proof uses a collision-free dart:core alias for
its dynamic control; bool callback arguments use the same qualified core scope.
Original implicit/explicit imports, including adjacent and multiline URI literals,
remain unchanged. Formats remain .fd 13, Catalog API 14 and Canvas model 18.

The aggregate catalog now has 89 widgets and 82 reviewed const definitions,
with 3894 writable rows (3877 outside Scaffold). Palette contains 31 Layout,
three Scrolling, twenty-three Basic, twenty-six Material and six Accessibility items.
The historical 92-widget target is not an exhaustive inventory and does not
establish a remaining-widget count. The 89 sources across 75 insertable
destinations form 6,675 cells, with 6,286 accepted and 389 rejected.
PhysicalShape's structured clipper previously established Catalog API 14, `.fd`
schema v13 and Canvas model protocol v18; these versions remain unchanged.
NBFC framing, control and wire remain v1.

## ListTile typed Canvas

`flutter.material.ListTile` adds all 37 non-key constructor arguments as 176
typed property rows and four independent optional slots: Leading, Title,
Subtitle and Trailing. Sparse creation omits every argument. Only explicit
`isThreeLine: true` requires Subtitle; a theme's value does not add a model
constraint. Four separate RTL-aware insertion handles remain outside SDK paint.

The preview constructs the actual SDK ListTile, with its existing Material
ancestry, M2/M3 defaults, whole text-style replacement, dense sizing, title
alignment, state colors/cursors and ink placement. The three local text-style
families use the shared complete Text projection. Whole project references
remain explicitly unresolved; they are never executed and do not enter a local
typed decoder. The generated source retains all references and callbacks,
including callbacks stored while Enabled is false.

An opaque intermediate ColoredBox/DecoratedBox can hide ink on the actual
ancestor Material. The SDK's nonfatal warning remains in the normal diagnostic
pipeline; no local Material is injected to change this behavior. The normal
Canvas surface already supplies Material. A defensive missing-Material branch
reports the unavailable tile and child preview rather than constructing an
invalid SDK subtree or silently inventing ancestry.

Signed geometry and the closed infinity/negativeInfinity/nan choices are not
globally rejected. Contextual render guards handle unbounded width, unsafe
height/offset results and full-width side children, retaining child State under
valid Material ancestry while withholding unsafe paint, semantics and pointer
input. Safe geometry restores the same mounted child. These guards never change
the model, generated Dart or pair-save authority. Missing Material is a separate
unavailable-preview case, not a State-retention guarantee.

Catalog and Java/Dart metadata, protocol-v18 decoding, all slot combinations,
actual SDK comparisons and lifecycle regressions are covered by the ListTile
tests. Final counts and package evidence are recorded in ADR-116.

## CheckboxListTile typed Canvas

The protocol-v18 projection adds `flutter.material.CheckboxListTile` with both
the standard and adaptive const constructors. Its 38 direct constructor
arguments expand to 154 typed property rows, while `title`, `subtitle` and
`secondary` remain three optional single any-widget slots. Sparse creation
preserves SDK defaults; explicit `value: null` requires `tristate: true`, and
explicit `isThreeLine: true` requires `subtitle`.

The preview constructs the actual SDK `CheckboxListTile` or
`CheckboxListTile.adaptive`, retaining the platform-specific Cupertino branch
on Apple targets. State-aware colors/cursors, checkbox and tile shapes, side,
density, callbacks and all slot combinations are validated against the same
closed model contract. Project references and callbacks remain isolated from
the runner. See ADR-117 for the complete constructor, DnD and lifecycle gate.

## RadioGroup typed Canvas

The protocol-v18 projection adds `flutter.widgets.RadioGroup` with four property
rows and one required child; there are no new value kinds or wire versions.
Built-in String/int/double/num/bool/Object and their nullable forms dispatch
actual `RadioGroup<T>` SDK widgets. Controlled callbacks never persist a new
selection or execute project code. Exact-type inherited scope, nested groups,
keyboard behavior and SDK focus/semantics remain authoritative for valid groups.

Runtime-invalid selected-client conflicts and unresolved project identity are
reported locally, without clamping values, rewriting descendants, editing the
model/source or treating project equality as known. See ADR-115 for the precise
mounted-client and semantic-scope policy and its verified recovery behavior.

## Radio standard/adaptive Canvas

Both constructors expose 107 reviewed property rows and dispatch the real typed
Radio<T>/Radio<T?> for String, int, double, num, bool and Object. Group values
remain nullable. The SDK's nullable Enabled field is preserved: false disables
without dropping callbacks, while omission/null can use a matching typed inherited
RadioGroup. Explicit project registries and unresolved project selection types or
values remain editable/selectable with a scoped preview-unavailable diagnostic;
Canvas never executes project code or invents project equality.

Presence-aware color, inner-radius and side state maps preserve explicit null and
SDK resolution order. Adaptive Apple uses the actual Cupertino branch, including
its forwarded mouse cursor and ignored Material-only visual fields. Gestures do
not change stored values. Project callbacks receive a diagnosed benign preview
replacement only when active; disabled or inherited-group-ignored callbacks do
not produce false warnings. Ordinary edits retain the SDK state and focus.

Fixed positive/negative infinity and NaN are valid double/num/Object identity
values. NaN remains unequal to itself and therefore unselected; this does not
admit NaN geometry. Signed finite and both infinite splash/inner radii retain
the pinned SDK behavior without a speculative clamp. Model/wire versions and
existing budgets are unchanged.

## MenuItemButton Canvas

The native Flutter 3.44.8 MenuItemButton is rendered with all 520 reviewed
properties: fourteen direct fields, eight shortcut fields, and all 498 shared
ButtonStyle leaves. The optional Child, Leading Icon and Trailing Icon slots
are real model children; none is fabricated. The catalog now has 102 widgets /
5300 properties. Model v19, transport v1, and the source-bundle inventory are
unchanged.

The enabled value controls a benign native onPressed callback. Menu activation,
hover/focus, disabled behavior, closeOnActivate, label semantics and the parent
menu orientation remain SDK behavior. Designer selection uses pointer-down only
within MenuItemButton so that the real native button gesture can win through all
three child slots. No global shortcut registration, project callback, controller,
shortcut factory or layer builder is executed. The 432 reviewed non-modifier
LogicalKeyboardKey constants have independently checked exact SDK key IDs.
SingleActivator and CharacterActivator generate native shortcut hints. The SDK
menu serializer requires a CharacterActivator string of exactly one UTF-16 code
unit; other valid constructor strings remain unchanged in the model and source,
but their unavailable preview hint is omitted with an explicit diagnostic.

Style composition uses MenuButtonTheme and the native MenuItemButton defaults,
including when Material 2 is selected. The SDK property-object merge remains
intact; a sparse scalar resolver that returns null can expose the underlying
TextButtonTheme exactly as in a native menu. Bound project styles use a labeled
default/theme approximation. Bound layer builders use identity layers around
the real child; native Clip.none remains the default and explicit clip wins.

Three non-overlapping empty-slot insertion zones follow Directionality. Selection,
ordinary style/shortcut edits and diagnostic appearance preserve the native menu,
child state, baseline and intrinsic geometry. Only actual native wrapper changes
(hover-region presence, enabled accelerator binding, label semantics wrapper or
label axis) invalidate a nested Tooltip preview when needed for safe overlay
disposal. The source model is never changed by these preview transitions.

A Menu-only geometry guard retains the real SDK subtree when its native vertical
label receives unbounded width or a native Flex exceeds the available extent, including RTL
overflow. Unsafe paint, semantics and pointer input are withheld with an explicit
diagnostic, and blocked descendant rectangles are excluded from Designer drop
resolution. Existing keyboard focus behavior is not replaced by this paint guard.
Naturally sized horizontal labels and native finite style-width constraints remain
unchanged; the guard uses public render topology to include actual menu-axis overrides.
Restoring safe constraints restores the native presentation without fabricating
nodes or changing stored values.

## TooltipTheme Canvas

The real const TooltipTheme constructor wraps its required Child with a fresh
TooltipThemeData. All 15 pinned Flutter 3.44.8 theme fields, 31 local TextStyle
fields and an exclusive nonnull whole Data reference are independently reviewed:
47 properties; current catalog 102 widgets / 5300 properties. Empty locals create
const TooltipThemeData(). No copyWith or lerp is used: the pinned copyWith drops
exitDuration, and lerp omits all five duration/trigger/feedback behavior fields.

The nearest native theme replaces the entire outer tooltip theme, not a fieldwise
merge. Local null values stay null and therefore use Tooltip's SDK/platform
defaults, not an outer scope's fields. Explicit nonnull Tooltip fields retain
the SDK's precedence. Trigger modes, hover wait, touch show and hover exit timing,
signed duration microseconds and semantics follow actual SDK behavior. The scope
also applies to implicit IconButton/FAB tooltips and composes with TooltipVisibility.

Isolated preview never evaluates project Data, TextStyle, Decoration, Duration or
other references. Unknown whole Data uses an explicitly diagnosed empty nearest
theme; unknown local reference fields use null defaults. A stable diagnostic
sibling preserves the actual child, baseline, intrinsic size, selection and open
overlay when warnings appear/disappear. Structured decoration/image/gradient and
all local TextStyle families retain the same exact bounded contracts as Tooltip.

Negative or overflowing overlay insets use visibly diagnosed zero insets;
negative/NaN minimum height uses zero; NaN/negative-infinite vertical offset uses
24. These safeguards apply at the theme provider so implicit native tooltips are
safe too; model values and generated Dart are never normalized. Ordinary theme
edits retain the tested Tooltip and editor State/focus. Structural wrap/unwrap/move
closes only affected preview overlays under the existing scoped lifecycle rule.
Required-child wrapping adds no empty-child insertion target. The shared direct
ParentData policy and model v19 / transport v1 remain unchanged.

## TooltipVisibility Canvas

The real Flutter 3.44.8 const TooltipVisibility wraps its required Child with one
required Boolean visible property (creation default true). The independent
catalog now contains 102 widgets / 5300 properties. Model v19 and transport v1
remain unchanged. Palette creation wraps the existing child; no placeholder child
is invented. The shared direct-parent ParentData policy remains unchanged: place
TooltipVisibility inside Expanded/Flexible, not between one and its Row/Column.

Native SDK inheritance controls explicit Tooltip and the implicit Tooltips of
IconButton/FloatingActionButton. The closest scope overrides, rather than ANDs
with, outer scopes. False removes RawTooltip and any open overlay, prevents new
hover/tap/long-press/manual activation, and removes the tooltip's own semantics
annotation in this pinned implementation (despite contrary API prose). Actual
child semantics and layout remain. No compatibility semantics shim is added.

Ordinary single-tooltip visibility updates retain the tested Tooltip/TextField
and inline-editor State, focus, selection and geometry. For a nested open Tooltip
branch, changing an outer Tooltip's effective RawTooltip presence retires only
that nested preview branch to avoid reparenting an open SDK OverlayPortal during
Canvas layout; the outer TooltipState and all model IDs/values remain intact.
The nested tooltip can reopen under an inner true scope. Explicit and nonempty
implicit SDK tooltip owners share this safeguard; diagnostic badges do not count
as model tooltip owners. Structural wrap/unwrap/move uses the same existing scoped
preview cleanup. There is no State producer or project-code execution.

## Tooltip Canvas

The real Flutter 3.44.8 const Tooltip constructor has 53 independently checked
properties: all 22 non-child SDK arguments plus all 31 shared local TextStyle
fields. Its optional Child is never fabricated. The current catalog has 102
widgets / 5300 properties; model v19 and transport v1 are unchanged. Plain message
and rich InlineSpan reference obey the exact nonnull XOR. Empty plain text really
disables the SDK tooltip. Explicit nulls, signed microsecond wait/show/exit
durations, all cursor presets, structured BoxDecoration and bounded image resources
retain their reviewed contracts.

Canvas uses the actual SDK Tooltip/RawTooltip overlay, hover delays, touch gesture
arena, tap/longPress/manual modes, dismissal, placement and semantics. Manual mode
still admits mouse hover. Only accepted native tap/longPress triggers invoke the
benign preview callback; hover and ensureTooltipVisible do not invoke it in this
pinned SDK. The anchor uses narrow pointer-down Designer selection without adding
a competing synthetic tap recognizer. Nested tooltips select the deepest node
once, and actual child button recognizers can win normally.

Project references, callbacks, factories and position delegates never execute.
Unknown appearance, durations and placement inherit the SDK theme/defaults with
an explicit small diagnostic badge. Unknown rich InlineSpan content uses a labeled
unavailable TextSpan surrogate: project text, WidgetSpan interaction and semantics
are not claimed to be reproduced. The diagnostic sibling does not resize or
replace the real anchor, participate in its baseline, or become a model/drop child.
Adding/removing diagnostics and changing selection preserve open Tooltip and child
State, including the inline editor's focus. Structural wrapping, unwrapping and
reparenting close affected preview overlays and may reset their transient SDK
State; changing a Container ancestor's actual wrapper topology (for example,
adding/removing padding) does the same only in the affected subtree. Scalar changes
within that same wrapper topology retain State. Persistent model IDs, properties
and generated Dart remain unchanged.

The same scoped rule covers reviewed generic constructor, custom-clipper
availability, Badge/FAB preview-key and native wrapper branches. ExpansionTile
shape presence and zero/nonzero background-alpha endpoints select different
Material wrappers; those transitions retire affected open previews, while RGB
and positive-to-positive alpha edits retain State. Existing diagnostic wrappers
use stable sibling badges only in Tooltip-containing branches, including warnings
derived from layout/theme, so merely adding a warning never reparents a portal.

Unsafe negative/overflowing overlay Container padding and margins use explicitly
diagnosed zero insets. Negative/NaN minimum height uses zero; negative-infinite or
NaN vertical offset uses the SDK default 24. Positive infinity for these two fields
remains exact where the pinned SDK safely bounds it. Invalid inherited constraints
are diagnosed and replaced only for preview. Negative durations remain signed and
follow the SDK's immediate timer behavior. These guards are tested while actually
showing overlays, not only while mounting the anchor. Empty anchors keep zero SDK
geometry and the existing compact Designer-only Child drop handle.

## ExpansionTile Canvas

The real Flutter 3.44.8 const constructor has 76 independently checked properties
and five slots: required title, optional leading/subtitle/trailing and a children
list. The current catalog has 102 widgets / 5300 properties. Palette creation wraps
an existing widget as Title; it never fabricates a title or admits a titleless node.
Both 21-field shape families and all four AnimationStyle fields are retained,
including 43 curve presets, signed microsecond durations, explicit nulls and
anonymous typed-reference presence. Protocol v19 and transport v1 are unchanged.

Canvas owns a persistent ExpansibleController and uses the actual SDK tile, header,
body, focus, keyboard actions and semantics. Ordinary model edits and selection
changes retain transient expansion. An explicit Initially expanded edit synchronizes
only the local preview controller; generated runtime treats this field solely as
an initialization seed. It is not a State producer/consumer. Project controllers,
state controllers, callbacks and factories are never executed. Unknown appearance
and animation use explicitly diagnosed SDK-theme/default approximations.

The SDK owns maintainState disposal versus Offstage/TickerMode retention. Collapsed
children have no Canvas hit, selection-outline, zero-size handle or drop geometry;
tree selection does not silently expand them. Show trailing icon=false also hides
a supplied Trailing slot. Header drop zones use its real ListTile bounds, while
Children append/reorder zones use the visible body. An expanded empty body gets a
compact Designer-only append band at the header edge without fake layout children.

Pinned ExpansionTile/Expansible ignore AnimationStyle.reverseDuration; its stored
value is retained without invented collapse timing or a warning for negative values.
Negative active durations assert in the SDK, so Canvas explicitly substitutes zero.
easeInBack, easeInOutBack, elasticIn and elasticInOut can produce invalid negative
height factors; only affected active preview curves use easeIn. Negative/overflowing
padding and positive-infinite/NaN minimum header height similarly use explicit zero
approximations, retaining source values and mounted children. Finite signed heights
and negativeInfinity remain exact. Unbounded-width paint/semantics are withheld
without disposing the SDK subtree. Missing model Material is separately unavailable;
the editor chrome is not counted as ancestry, and actual SDK shape/background
Material branches are honored without inserting a synthetic Material.

## RadioListTile standard/adaptive Canvas

Both real Flutter 3.44.8 constructors have 153 independently checked properties
and optional title/subtitle/secondary slots (102 widgets / 5300 properties).
Six scalar generic types and their nullable variants retain exact RadioGroup
membership. The actual SDK tile is the group's single public RadioClient; its
inner Radio remains hidden by the SDK registry. A synchronous public-registry
observer participates in the existing duplicate-selection quarantine without
creating proxy clients or extra groups. It mirrors all direct pinned SDK inherited
dependencies and flushes a dirty cached tile before restoring guarded membership,
including independent RadioTheme/ListTileTheme updates. Group moves and generic
changes unregister old membership; the SDK owns native keyboard traversal.

Modern null groupValue genuinely falls back to the deprecated local groupValue.
Omitted/null onChanged disables only when no matching group activates the tile;
explicit enabled without a callback or group is diagnosed with slot State retained.
Project selection/type references never invent equality or a checked state.
Other project objects/callbacks remain anonymous, inert, labeled approximations.
The four nullable state-map families, renamed stateful side, local cursor buckets,
ten tile shapes and signed geometry preserve the reviewed SDK contracts. Nonfinite
scale/reaction geometry uses an explicit zero approximation, while existing tile
geometry guards retain child State and source values. Selected is independent of
checked; native merged radio semantics do not add a button flag by default.
Adaptive-only useCupertinoCheckmarkStyle is retained but inactive in Standard.

## SwitchListTile standard/adaptive Canvas

Both actual Flutter 3.44.8 constructors have 236 independently checked properties
and optional title/subtitle/secondary single slots (current catalog: 102 widgets / 5300 properties).
Four state-color families, all nine complete thirteen-field thumb-icon buckets,
stateful cursors, bounded image resources, all tile shapes and local density are
retained. No standalone-Switch-only arguments or synthetic Enabled row are added.
Null onChanged disables the entire SDK tile; local callbacks select the Designer
node without changing the controlled Value. Native MergeSemantics yields a switch
toggled state, with the additional button flag false by default.

Adaptive uses the pinned SDK's unified MaterialSwitch with Cupertino-style painting
on Apple platforms, not a separate CupertinoSwitch widget. Adaptive-only
applyCupertinoTheme is retained but inactive in Standard. Project objects and
callbacks are anonymous presence only and never executed; unknown appearance uses
explicitly labeled SDK-theme/default approximations. Nonfinite reaction radii are
diagnosed and only that reaction uses radius zero; finite signed values remain exact.
Shared ListTile guards withhold unavailable geometry/slot paint without inventing
children, changing stored values or losing child State. The known pinned Switch
Cupertino painter lifecycle boundary resets only the SDK tile shell, preserving
the outer FocusNode and globally keyed slot children; ordinary updates retain State.

## Switch standard/adaptive Canvas

Both constructors use the real public Switch with 201 capability-checked fields.
State color/width/icon maps preserve ordered matching, omission and explicit null;
all thirteen Icon fields are passed through even where the SDK painter ignores
them. Both image providers use bounded revision-scoped resources. Project object
references, factories and callbacks remain unavailable rather than executed.

Tap/drag selects the Designer control without changing stored Value. The actual
SDK owns its semantics, focus, theme/adaptations, Material 2/3 and adaptive Apple
configuration. Equal infinite outline endpoints are exact; unequal finite/infinite
endpoints are diagnosed and previewed using SDK default width two because the
pinned SDK asserts during interpolation. Stored values and generated Dart do not
change. A configuration-boundary public-shell key avoids the pinned SDK's sticky
isCupertino State flag while retaining the Canvas-owned FocusNode and outer stable
identity. Ordinary edits within a configuration retain SDK State; no key is emitted
into user Dart. These are preview-only boundaries, not repairs to generated apps.
Finite padding whose sum overflows on an unbounded parent axis receives an explicit
preview-unavailable status and the existing selectable handle; bounded layout keeps
the exact SDK behavior. No stored padding is clamped.

## MenuAnchor Canvas

The runner constructs the real pinned Flutter 3.44.8 MenuAnchor with all 219
reviewed properties: 16 direct fields and 203 MenuStyle leaves. MenuChildren is
a required list that may be empty; Child is optional. Neither slot receives a
fabricated model child. Current catalog: 102 widgets / 5300 properties; model v19,
transport v1 and public schema versions are unchanged.

Each mounted anchor owns a stable isolated MenuController. Selecting an anchor,
or a child it owns, exposes an accessible **Preview menu / Close preview**
Designer action beside its actual geometry. This action does not change selection,
source, model values or child dimensions. Ordinary child taps are not repurposed
as app openers. Project builders/controllers/focus nodes/callbacks/factories never
execute: reference presence is anonymous and diagnosed. Create Menu Builder in
the IDE produces user-owned app code; this separate Canvas action does not claim
to execute that code.

MenuStyle uses MenuTheme and pinned menu defaults, not ButtonStyle defaults. All
nine resolver buckets remain exact source data, while the native panel and mouse
cursor resolve with the empty WidgetState set. Local size/side/shape components
inherit missing components from the menu theme/default; a local density axis
constructs a complete VisualDensity with an omitted peer equal to zero. Resetting
both axes restores native inherited density. Native root/nested alignment fallback
is left to MenuAnchor when no complete local alignment is supplied. Nonfinite or
inconsistent inherited geometry is explicitly diagnosed and previewed with safe
native-default geometry, without rewriting the model.

A stable Overlay inside the logical viewport preserves native menu coordinates
under zoom and Canvas clipping. It deliberately has no LookupBoundary, which would
hide View.of and break EditableText. Consequently source useRootOverlay=true is
preserved but diagnosed as an isolated preview approximation: both root/nearest
menu placement use this local viewport Overlay. Bound LayerLink uses a stable
isolated target/follower when possible, not the project's connection. If menu
descendants own native Tooltip overlays (including IconButton/FAB tooltip), that
surrogate link is omitted with an explicit warning because the pinned SDK cannot
compute the Tooltip transform through a follower. Diagnostic badges under an
active surrogate follower retain their accessible warning text without opening
their own unsafe diagnostic Tooltip popup.

Open menu children retain actual IDs, selection, inline editing, focus and SDK
State through ordinary style, callback-presence, State-payload and warning edits.
Native activation closes the menu according to closeOnActivate, and keyboard
traversal skips disabled entries. No shortcut registration is invented. The
deprecated anchorTapClosesMenu field remains stored but is inert in this SDK.
Structural changes such as animated/layerLink/cross-axis wrapper branches, model
reparenting, and adding/removing a Tooltip under a linked menu may close/reset only
the affected transient preview subtree. Stored children and source remain exact.

MenuChildren drop/move zones use real overlay panel geometry and disappear when
closing animation begins, even while native controller.isOpen remains true.
Closed menu children are neither hit nor drop targets. With an empty zero-sized
anchor, the existing selection handle accepts Child while closed and the empty
MenuChildren list while explicitly previewing; no fake menu item is mounted.

## Checkbox standard/adaptive Canvas

The runner constructs the real Checkbox or Checkbox.adaptive with controlled
stored Value, not a preview-generated application variable. Enabled callbacks are
local no-ops; project callbacks/objects are diagnosed and never executed. Both
constructors retain all explicit properties. Adaptive Cupertino ignores fillColor,
hoverColor, overlayColor, splashRadius, materialTapTargetSize, visualDensity and
isError exactly as the SDK does, but retains the metadata and forwards shape/side.

Presence-aware fill/overlay maps return explicit null without falling through to
lower local buckets; the actual SDK then resolves component themes/defaults. Plain
BorderSide only applies unselected; stateful sides use public WidgetStateBorderSide
and preserve explicit inherit buckets. Shared outlined shapes and resource guards
remain unchanged. Density omitted peers are zero when the other axis is explicit.
Signed finite and positive-infinite splashRadius values are preserved without
clamping. Platform selection follows Theme.platform; Cupertino tap sizing also
depends on the SDK host defaultTargetPlatform, which parity tests vary separately.
### Checkbox SDK shape limitation

The pinned Flutter 3.44.8 Checkbox painters omit TextDirection for shape painting.
LinearBorder and directional/mixed corner radii therefore assert in raw SDK controls.
Canvas emits a Checkbox.shape diagnostic and uses the SDK-default rounded shape
(M2 radius 1, M3 radius 2, Cupertino radius 4) only when the effective local/theme
shape requires the missing direction. Safe explicit shapes override theme shapes;
adaptive Apple ignores CheckboxTheme. Model/source remain exact, and real Checkbox
State, controlled value and semantics are retained. This path is an explicit
preview approximation, not exact rendering of an unsupported SDK shape.

## Slider standard/adaptive Canvas

The 33-field closed decoder forwards the real SDK constructors. Material resolves
actual SliderTheme, platform and Material 2/3 defaults; Apple adaptive rendering
uses CupertinoSlider inside the SDK's infinite-width wrapper. Only Value, Min/Max,
Divisions, the three activation callbacks, Active color and Thumb color reach that
Cupertino branch. Other stored fields remain intact and generate normally.

Canvas controls Value and never runs project callbacks, formatter or factories.
An isolated persistent focus owner and Slider-only pointer selection preserve
designer selection without writing gesture values into the document. Unsupported
project references on the active branch receive precise diagnostics, not fabricated
success; Apple-ignored references do not report false unavailable warnings.
Constructor-legal equal bounds work in Material; the raw Apple SDK normalization
bug and other unsafe normalization/layout/theme contexts are diagnosed without
changing model/source. There is no global tick-count cap on Divisions.

## Scrollbar Canvas

The runner now mounts the real Flutter 3.44.8 `Scrollbar` with all eight typed
constructor fields and its required single `child`. Nullable visibility and
interaction values preserve omission versus explicit `false`; orientation uses
the SDK enum (`left`/`right` for vertical scrolling, `top`/`bottom` for
horizontal scrolling). Controller, notification-predicate and `Radius`
references remain model/source data and are diagnosed instead of being executed
by the isolated runner.

Because a palette child may not own a `ScrollPosition`, the preview adds a
non-serialized `SingleChildScrollView` on the selected axis. This gives the
native scrollbar a valid position without changing generated Dart, child IDs or
state. The current aggregate catalog is 116 widgets and 6,362 writable rows;
the source manifest records the updated runner hashes. CJK IME and Linux/macOS
Canvas providers remain outside this milestone.

## PreferredSize Canvas

The runner mounts the native Flutter 3.44.8 `PreferredSize` with its required
finite non-negative `preferredSize` and required single `child`. Palette
creation starts at `Size(100, 56)`; the stored size is advertised to parents
without constraining the nested child. The required child remains a real Slots
entry and is preserved through Canvas DnD, Save/reopen and history. PreferredSize
is a wrapper source, not an empty-slot insertion destination, and its
`PreferredSizeWidget` trait remains available to the typed AppBar/Scaffold
slots. Native and exact-Web rendering use the same metadata and renderer.

## Builder Canvas

The runner mounts the native Flutter 3.44.8 `Builder` with its required
`WidgetBuilder` callback represented as presence-only metadata. Palette-created
nodes use a reviewed no-op callback, and the isolated preview renders a bounded
48 × 36 `SizedBox`; project callback bodies and symbols are never executed by
Canvas. Generated Dart retains the typed callback identifier, Properties keeps
the callback editable, and Builder has no child slots.

## ListWheelScrollView Canvas

The runner mounts the native Flutter 3.44.8 `ListWheelScrollView` with its
reviewed 18-property contract, required positive `itemExtent` and ordered
`children` list. Wheel geometry, clipping, hit testing, restoration, physics
presets and change-reporting behavior are passed through the model. Controller,
scroll-behavior and callback references are retained and diagnosed; project
code is never executed in the isolated runner. `renderChildrenOutsideViewport`
is normalized only for the preview when the SDK requires `Clip.none`; stored
values and generated Dart remain unchanged. CustomScrollView/sliver branches
are not part of this slice.
