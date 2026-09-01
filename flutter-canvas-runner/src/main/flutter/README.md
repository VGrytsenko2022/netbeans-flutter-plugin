# netbeans_flutter_canvas_runner

Isolated Windows child process for the NetBeans Flutter Designer's first native
read-only Canvas slice. NetBeans builds and launches this versioned runner for
each open `.fd` Design tab and embeds its real `FlutterView` as a verified child
window. The Canvas is painted by Flutter directly; the protocol never transfers
screenshots or framebuffer pixels. Model protocol v11 may additionally carry
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
decoder accepts exactly `Scaffold`, `AppBar`, `Column`, `Row`, `Text`, `Icon`,
`Padding`, `Center`, `Align`, `Container`, `Opacity`, `SizedBox`, `AspectRatio` and
`ElevatedButton`,
with reviewed typed properties and slots. It
rejects unknown widgets, fields and values instead of loading arbitrary project
Dart code.
Model protocol v11 carries the resolved project-theme id, seed, brightness,
46-role ColorScheme override table, 15-role TextTheme override table and the
closed 36-leaf component-color table. The
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
required-named-nullable child slot as the Java catalog. Protocol v11 carries
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

An empty Row or Column keeps its real Flutter layout (including a zero
cross-axis extent) but receives a non-layout-affecting 36-pixel-minimum
selection outline and hit rectangle. Its whole bounded rectangle is insertion
index zero; a populated Row or Column exposes only its terminal append edge.

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
14 reviewed Canvas widgets. Palette insertion evaluates 252 exact
source/destination cells across 14 draggable sources and 18 reviewed slots;
226 are accepted and 26 trait-incompatible cells are rejected. The negotiated
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
