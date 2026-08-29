# netbeans_flutter_canvas_runner

Isolated Windows child process for the NetBeans Flutter Designer's first native
read-only Canvas slice. NetBeans builds and launches this versioned runner for
each open `.fd` Design tab and embeds its real `FlutterView` as a verified child
window. The Canvas is painted by Flutter directly; the protocol never transfers
PNG, screenshots or raw pixel frames.

After the bounded version 1 lifecycle handshake, stdin/stdout NBFC frames carry
strict runtime control and one digest-described canonical model payload. The
`CORE_V1` decoder accepts exactly `Scaffold`, `Column`, `Row`, `Text`, `Padding`
and `Center`, with reviewed typed properties and slots. It rejects unknown
widgets, fields and values instead of loading arbitrary project Dart code.
Model protocol v4 also carries the resolved project-theme id, seed, brightness,
46-role ColorScheme override table and 15-role TextTheme override table. The
runner applies the same seed → `ColorScheme.copyWith` → `ThemeData.from` →
`TextTheme.copyWith` order as generated Dart before applying form-local Text
properties.

The runner renders exact compatible responsive/adaptive profiles on its bound
Windows Flutter engine. Android, iOS, macOS and Linux targets are applied through
Flutter's `ThemeData.platform`; they test Flutter adaptive widget appearance but
do not claim a device operating system, plugin or platform-channel runtime. A
Web target uses its browser-sized responsive viewport with Windows adaptive
controls as an explicit native-engine layout preview. It does not claim
`kIsWeb`, DOM, browser fonts, plugins or platform-channel behavior; those would
require a separately compiled browser backend. Each widget retains its stable `.fd` UUID;
revision-, presentation- and layout-bound messages synchronize read-only
selection between Flutter hit testing and the NetBeans Explorer tree.

The runner receives no project path, Dart source, file handle, `SaveCookie`,
Undo/Redo or Designer-command authority. NetBeans owns editable Properties and
the catalog-admitted Palette insertions for the exact six CORE_V1 widgets; the
runner only renders validated revisions, performs bounded hit testing and
returns revision-bound intents. Catalog JSON is reserved for a future versioned
catalog contract, and Linux/macOS native hosts remain separate work.
