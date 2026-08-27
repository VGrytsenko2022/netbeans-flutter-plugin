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

The runner renders exact compatible responsive/adaptive profiles on its bound
Windows Flutter engine. Android, iOS, macOS and Linux targets are applied through
Flutter's `ThemeData.platform`; they test Flutter adaptive widget appearance but
do not claim a device operating system, plugin or platform-channel runtime. A
true Web Canvas requires a separately compiled browser backend, so the NetBeans
edge never publishes the runner's explicit Windows fallback as Web. Each widget
retains its stable `.fd` UUID;
revision-, presentation- and layout-bound messages synchronize read-only
selection between Flutter hit testing and the NetBeans Explorer tree.

The runner receives no project path, Dart source, file handle, `SaveCookie`,
Undo/Redo or Designer-command authority. Palette drag-and-drop, editable
Properties, document mutation and persistence are not implemented. Catalog JSON
is reserved for a future versioned catalog contract, and Linux/macOS native
hosts remain separate work.
