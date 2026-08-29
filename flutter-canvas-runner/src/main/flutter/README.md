# netbeans_flutter_canvas_runner

Isolated Windows child process for the NetBeans Flutter Designer's first native
read-only Canvas slice. NetBeans builds and launches this versioned runner for
each open `.fd` Design tab and embeds its real `FlutterView` as a verified child
window. The Canvas is painted by Flutter directly; the protocol never transfers
PNG, screenshots or raw pixel frames.

After the bounded version 1 lifecycle handshake, stdin/stdout NBFC frames carry
strict runtime control and one digest-described canonical model payload. The
reviewed decoder accepts exactly `Scaffold`, `AppBar`, `Column`, `Row`, `Text`,
`Icon`, `Padding`, `Center` and `SizedBox`, with reviewed typed properties and slots. It
rejects unknown widgets, fields and values instead of loading arbitrary project
Dart code.
Model protocol v7 carries the resolved project-theme id, seed, brightness,
46-role ColorScheme override table and 15-role TextTheme override table. The
runner applies the same seed → `ColorScheme.copyWith` → `ThemeData.from` →
`TextTheme.copyWith` order as generated Dart before applying form-local Text
properties. It preserves physical `EdgeInsets` versus text-direction-aware
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

The runner renders exact compatible responsive/adaptive profiles on its bound
Windows Flutter engine. Android, iOS, macOS and Linux targets are applied through
Flutter's `ThemeData.platform`; they test Flutter adaptive widget appearance but
do not claim a device operating system, plugin or platform-channel runtime. A
Web target uses its browser-sized responsive viewport with Windows adaptive
controls as an explicit native-engine layout preview. It does not claim
`kIsWeb`, DOM, browser fonts, plugins or platform-channel behavior; those would
require a separately compiled browser backend. Each widget retains its stable `.fd` UUID;
revision-, presentation- and layout-bound messages synchronize read-only
selection between Flutter hit testing and the NetBeans Explorer tree. Every
rendered widget has a non-layout-affecting thin dashed outline; the selected
widget replaces it with the solid blue selection outline. Physical and
direction-aware `Padding` values also paint thin orange distance guides from the
outer bounds to the resolved inner bounds. Outline widths, dash lengths and
guide caps compensate for the current viewport scale, so their on-screen weight
remains stable across Fit and manual zoom.

The logical viewport never follows the native child-window size. The optional
`viewport.presentation.v1` control keeps `MediaQuery` fixed while applying a
view-only `Fit` or 25–200% manual paint transform. When manual zoom overflows,
Flutter draws and owns both scrollbars and handles wheel, Shift+wheel and
Ctrl+wheel interaction in the same geometry used for hit testing and native
Palette drops. Runner metrics echo the exact host command sequence; presentation
state is per Design tab and is not persisted in the model.

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
nine reviewed Canvas widgets. Palette insertion evaluates 117 exact
source/destination cells across nine draggable sources and 13 reviewed slots;
101 are accepted and 16 trait-incompatible cells are rejected. The negotiated
source-aware command binds the opaque token to the current reviewed type and
traits before Flutter exposes compatible hover zones. The
runner only renders validated revisions, performs
bounded hit testing, returns revision-bound Palette intents and paints optional
move feedback. Catalog JSON is reserved for a future versioned
catalog contract, and Linux/macOS native hosts remain separate work.
