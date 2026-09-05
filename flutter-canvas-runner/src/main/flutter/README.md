# netbeans_flutter_canvas_runner

Isolated Windows child process for the NetBeans Flutter Designer's first native
read-only Canvas slice. NetBeans builds and launches this versioned runner for
each open `.fd` Design tab and embeds its real `FlutterView` as a verified child
window. The Canvas is painted by Flutter directly; the protocol never transfers
screenshots or framebuffer pixels. Model protocol v18 may additionally carry
bounded, content-addressed compressed project-image bytes for typed asset
previews.

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
`ListView`, `GridView.count`, `SingleChildScrollView`, `Wrap`, `Container`, `Opacity`, `Transform`, `RotatedBox`, `ListBody`,
`OverflowBar`, `SafeArea`, `ColoredBox`, `Placeholder`, `Directionality`,
`DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer` and `ExcludeSemantics`,
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
UnconstrainedBox, LimitedBox, OverflowBox, RotatedBox or ColoredBox keeps its real Flutter layout (including
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
55 reviewed Canvas widgets. Palette insertion evaluates 2,860 exact
source/destination cells across 55 draggable sources and 52 insertable reviewed
slots; 2,608 are accepted and 252 cells are rejected. Expanded and Flexible are
admitted only as atomic wrappers over an existing direct Row/Column child,
cannot wrap either wrapper type, and expose required replacement-only child
slots that are excluded from the insertion matrix. They also cannot wrap
Spacer; Spacer is inserted only into direct Row/Column children and never wraps
another widget. SafeArea and Directionality are generic atomic wrappers around
an existing widget,
never an empty required-child prototype. The current Canvas target wire exposes
non-root child targets only and intentionally offers no root target; root
wrapping remains available through the NetBeans tree. Neither wrapper can wrap
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
complete with 66 remaining, and the Palette Layout category contained 18
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
complete with 56 remaining, and Layout contains 28 items. Canvas model protocol
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
backlog was 46/92 complete with 46 remaining. The 46 sources across 43
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

The aggregate catalog now has 55 widgets and 49 reviewed const definitions,
with 761 writable rows (744 outside Scaffold). Palette contains 31 Layout,
three Scrolling, sixteen Basic, four Material and one Accessibility item; the
backlog is 55/92 complete with 37 remaining. The 55 sources across 52
insertable destinations form 2,860 cells, with 2,608 accepted and 252 rejected.
The structured clipper advances Catalog API to 14, `.fd` schema to v13 and Canvas
model protocol to v18. NBFC framing, control and wire remain v1.
