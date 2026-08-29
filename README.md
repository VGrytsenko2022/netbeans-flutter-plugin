# Flutter and Dart Support for Apache NetBeans

Starter architecture for first-class Dart + Flutter support in Apache NetBeans 30.

## Direction

The project intentionally implements **IDE support first** and keeps the visual designer isolated until the core plugin is useful by itself.

The current usable workflow is:

1. Open/detect a Flutter project.
2. Locate and validate the Flutter SDK.
3. Edit highlighted Dart sources with the SDK Analysis Server and NetBeans DAP integration.
4. Discover and select Desktop, Mobile, or Web run targets.
5. Create and manage Android Virtual Devices, or launch an already configured iOS simulator.
6. Run or debug a Flutter application.
7. Open Flutter DevTools for the active application from NetBeans.
8. Use Hot Reload, Hot Restart, Stop, and the NetBeans Output window.
9. Build, clean, resolve packages, analyze sources, and run all/file/single tests through native NetBeans tooling UI.
10. Edit `pubspec.yaml` with Flutter-aware completion and semantic diagnostics.
11. On Windows, open a valid paired `.dart`/`.fd` Designer document in a
    read-only native Flutter Canvas, switch among exact Android/iOS/desktop
    adaptive preview targets allowed by the project, and synchronize stable
    widget selection between the Canvas and the widget tree. The tree
    remains fully expanded, hides its redundant expansion controls and uses
    scoped one-pixel golden parent-child connectors, without changing the look
    of other NetBeans trees. A project
    with `web/` also receives a browser-sized Web layout preview on the native
    engine; browser-only runtime behavior is not emulated. The Windows Canvas
    accepts the six reviewed CORE_V1 Palette widgets through the catalog
    compatibility matrix, with paired generation, analysis, Save and Undo/Redo.
    The same Palette token may be dropped on an exact widget-tree row when that
    parent has one unambiguous compatible slot; ambiguous multi-slot parents
    such as `Scaffold` fail closed instead of guessing a destination.
    Existing non-root widgets may also be dragged within the same tree: drop on
    a uniquely compatible container or on a before/after insertion line. The
    exact immutable subtree and every stable ID are preserved, and the drop is
    replanned against the latest revision before one `MoveWidget` is admitted.
    While the tree drag is active, an available Canvas paints the exact future
    destination with a thin amber marker; the tree operation does not depend on
    Canvas readiness.
    Each Design tab defaults to `Fit`, also offers 25–200% manual zoom, and
    exposes native Canvas scrollbars whenever the fixed logical profile no
    longer fits. Zoom and scroll are view-only and are never written to `.fd`.

With the core IDE workflow stable, version 0.1.3 is now building the
Matisse-like Flutter Designer in staged, non-authorizing slices.

## Modules

- `flutter-core-api` — stable Java contracts and shared domain objects.
- `dart-analysis` — lifecycle-safe Dart Language Server process and raw LSP transport.
- `flutter-sdk` — SDK discovery, validation and Flutter CLI process execution.
- `flutter-project` — Flutter project recognition and project metadata.
- `flutter-run` — target discovery, Android SDK/AVD lifecycle services, configured emulator launch, managed machine-mode run sessions, DevTools process integration, immutable Build/Clean and tooling commands, and Analyze/Test protocol parsers.
- `flutter-canvas-runner` — versioned Flutter/Windows sources for the isolated
  native Canvas child process, its bounded read-only model protocol and stable-ID
  selection bridge, optional exact widget-move destination projection, packaged
  with the plugin.
- `netbeans-plugin` — NetBeans UI integration and actions.
- `netbeans-runtime-it` — assembled NetBeans 30 gates for the packaged module, persisted SDK settings, Flutter project lifecycle, Dart MIME/editor registrations, Flutter actions, and optional real-SDK editor behavior.
- `flutter-designer` — NetBeans-independent `.fd` schema, model, validation,
  generation, command, persistence-planning and bounded read-only Canvas payload
  boundary for the Designer.

## Requirements

- Apache NetBeans 30
- JDK 21+
- Maven 3.9+
- Flutter SDK installed separately

## Build

```bash
mvn clean install
```

The Java-only modules can also be worked on independently. The NetBeans module uses `RELEASE300` APIs.

For version highlights and installation instructions, see the [0.1.2 release notes](docs/RELEASE_NOTES_0.1.2.md). The complete release history is in [CHANGELOG.md](CHANGELOG.md).

## Configure Flutter and Dart

The plugin adds a dedicated `Flutter` category to `Tools > Options`. On its first start it imports a valid Flutter SDK from `flutter.sdk`, `FLUTTER_HOME`, `FLUTTER_ROOT`, or `PATH`. For Dart it respects `dart.sdk`, `DART_HOME`, and `DART_SDK`, then uses Flutter's bundled Dart SDK when available, and finally checks `PATH`.

If discovery finds nothing, open `Tools > Options > Flutter`, select the SDK folders manually, and validate them before applying the settings. `Tools > Check Flutter and Dart SDKs` uses the same saved configuration.

## Create or open a Flutter project

- Choose `File > New Project > Flutter > Flutter Application` to generate the standard base application with the configured SDK. Its `Target Platforms` step supports Android, iOS, Web, Windows, macOS, and Linux with Recommended, Mobile, Desktop, Web, All, and custom selections. The wizard passes the non-empty selection to `flutter create --template app --platforms=...`, verifies the requested platform directories, adds the project-wide Light/Dark theme catalog described below, opens the generated project, and selects `lib/main.dart`.
- Choose `File > Open Project` and select any existing Flutter directory containing `pubspec.yaml` and `lib`. It opens as a native NetBeans project with Flutter identity, a logical file tree, Dart source groups, and standard project operations.
- To extend an existing Flutter application (`project_type: app`), choose `Flutter > Add Flutter Platforms...` or the same command in the project's context menu. Only canonical paths that are completely absent can be selected; an existing directory, file, or symbolic link is treated as occupied and is never overwritten. The operation uses the configured Flutter SDK, native Output and progress, then verifies that Flutter created every selected real directory. Flutter module, package, plugin, and unknown project types are rejected with a concrete reason.
- In an open Flutter project choose `File > New File > Dart > Dart Class`. The wizard uses the selected project folder, converts an UpperCamelCase name such as `OrderRepository` to `order_repository.dart`, writes the class and opens it in the Dart editor.
- Choose `File > New File > Flutter Designer > Flutter Designer Form` to create
  a complete Designer pair. The target is restricted to `lib` or one of its
  subfolders. A Dart target such as `lib/account/profile.dart` is paired with
  the JSON model `.fd_templates/account/profile.fd`; the current schema-v3
  `source.dartFile` value remains the Dart basename `profile.dart`.
- Dart sources and Flutter Designer `.fd` models use distinct theme-aware file
  icons in Projects, Files, and the corresponding New File wizard entries.
- NetBeans `Delete` is available on either member of a complete Designer pair
  and removes both the `lib/.../*.dart` source and mirrored `.fd_templates/.../*.fd`
  model. The action closes a clean shared Designer/Source editor before touching
  disk; incomplete, unsafe, read-only or unsaved pairs fail closed.
- NetBeans `Rename` is also available from either member of a complete, clean
  current-version pair. A canonical lower-snake-case basename renames both
  mirrored files and updates only the current `.fd` `source.dartFile`; the Dart bytes and
  `source.className` are deliberately unchanged. The implementation stages both
  paths, writes and verifies the canonical model metadata, and exact-byte rolls
  back a failed operation. This is an in-process rollback guarantee rather than
  a durable crash-recovery journal.
- NetBeans `Copy`/`Paste` is available from either physical member of a complete,
  clean pair. Paste currently duplicates the Dart and `.fd` files only inside
  that pair's existing mirrored relative folder, choosing a jointly free
  `_copy`, `_copy_2`, ... basename across both trees. The Dart bytes remain
  exact; the canonical duplicate model receives a new `documentId` and changes
  only `source.dartFile`. A clean open Source/Designer editor remains open.
  Clipboard transfer uses the pair-aware NetBeans node flavor only, without a
  one-file loader flavor or operating-system file-list flavor. Cross-directory
  Copy awaits defined relative-URI rebasing semantics.
- NetBeans `Cut`/`Paste` moves a complete clean Designer pair between already
  existing mirrored folders in the same Flutter project. The basename and the
  exact Dart/`.fd` bytes are unchanged. A successful Paste consumes the
  pair-only clipboard transfer, closes a clean source editor, retires the old
  path-bound DataObjects and creates fresh owners at the destination. Missing
  mirrored folders, collisions, read-only or linked paths, unsaved files and
  Dart directives whose binding could change all fail closed. The final proof
  and commit share an EDT admission under exact NetBeans 30 MasterFS file locks
  and folder child-cache mutexes; another runtime shape is rejected rather than
  guessed. Generic DataObject Move remains unavailable. Schema-v1 asset paths are relative to the
  Flutter project/pubspec root, never to the `.fd` location, and opaque
  `extensions` metadata must remain location-independent.

## Project-wide Flutter themes

New Flutter applications contain one shared theme source at
`.fd_templates/project.fdtheme` and its deterministic generated Dart API at
`lib/theme/app_theme.dart`. The defaults are Light and Dark Material seed
themes with `ThemeMode.system`; `lib/main.dart` is wired to `AppTheme.light`,
`AppTheme.dark` and `AppTheme.mode`. Individual `.fd` files do not duplicate
theme definitions, so every Designer form and runtime screen can consume the
same project catalog.

Choose `Flutter > Edit Flutter Themes...` (also available from a Flutter
project's context menu), or open `project.fdtheme`. NetBeans opens the docked
`Themes` tab beside `Palette`, with explicit Save and Reload actions. The editor
changes the default mode and active light/dark definitions, and can add,
duplicate, edit, enable, disable or remove custom definitions. A disabled
definition remains in the descriptor catalog but is omitted from the generated
Dart map. `Enable project themes` can make `MaterialApp` use Flutter's defaults
without deleting the catalog; enabling it again restores the selected project
definitions. The editor's `General`, `Colors` and `Typography` tabs expose all
46 supported non-deprecated Material `ColorScheme` roles and all 15 Material 3
`TextTheme` roles. Each text role has 13 typed optional fields for colors,
font metrics/family, weight/style and decoration; omission means inherit the
seed-derived Material value, while text colors may use either an exact ARGB
literal or another semantic `ColorScheme` role.

Schema v2 adds the portable project-wide switch, schema v3 adds per-definition
switches, and the current schema v4 adds the typed color and typography role
overrides. Schema v1-v3 descriptors remain readable and acquire empty override
tables in memory; an explicit Save writes canonical v4. Generated Dart applies
the same ordered `ColorScheme.copyWith` and `TextTheme.copyWith` construction as
the native Canvas. The generated Dart file is marked as generated and guarded
by the SHA-256 stored in the descriptor; if it was edited outside the theme
editor, Save reports the conflict and leaves those bytes untouched.

Opening an older project does not create or rewrite theme files automatically.
Invoking the editor offers explicit default-theme initialization only when the
existing `lib/main.dart` matches the safely recognized Flutter application
template; unsupported or occupied paths fail without partial writes. The file
formats are documented by the frozen
[`project-theme-v1.schema.json`](docs/flutter-designer/project-theme-v1.schema.json),
[`project-theme-v2.schema.json`](docs/flutter-designer/project-theme-v2.schema.json)
and [`project-theme-v3.schema.json`](docs/flutter-designer/project-theme-v3.schema.json)
contracts, plus the current
[`project-theme-v4.schema.json`](docs/flutter-designer/project-theme-v4.schema.json).

## Run and debug a Flutter application

Flutter execution actions are available from the top-level `Flutter` menu and from a Flutter project's context menu.

1. Make a Flutter application active (or set it as the main project). NetBeans' standard configuration selector in the Run toolbar is populated asynchronously from `flutter devices --machine`, but exposes only targets whose Android, iOS, Web, Windows, macOS, or Linux platform directory is actually configured in that project. Run and Debug revalidate the match immediately before launch. The open project refreshes the cached list automatically on a five-second fixed delay, while a successful `Add Flutter Platforms...` refreshes it immediately; transient failures use a bounded 2/5/15/30-second backoff without blocking the UI or discarding the last good list. Choosing an entry remembers the target per project. `Flutter > Select Run Target...` remains available for an immediate refresh and the detailed target dialog.
2. For Android, open `Flutter > Device Manager`. It discovers the Android SDK from `android.sdk`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, Flutter configuration, or platform defaults; lists exact ADB devices and AVD states; and provides Create, Start, Stop, Restart, Wipe Data, Delete, Refresh, and Select Target. Create uses only installed stable-channel system images and never accepts licenses implicitly. Start, Restart, and Wipe wait for the exact AVD to boot and then select its exact ADB serial in the Flutter toolbar. Wipe and Delete require explicit confirmation. Closing the window or cancelling the boot wait does not terminate an emulator that has already started.
3. `Flutter > Launch Mobile Emulator...` is available only when the active application contains Android or iOS platform scaffolding. It starts an already configured definition, waits for a uniquely matching new Flutter device, and selects it. The operation has a native cancellable NetBeans progress indicator with Loading, Checking devices, Launching, Waiting, Selecting, and Ready phases.
4. The standard NetBeans `Build Project`, `Clean Project`, and `Clean and Build Project` commands are available from the project context menu and main Run menu. Build captures the selected toolbar target and creates its release artifact with `flutter build windows|linux|macos|web|apk|ios`; Android produces an APK. Clean runs `flutter clean`. Clean and Build validates the target first, then runs Clean and Build sequentially as one cancellable NetBeans action and does not build after a failed or cancelled Clean.
5. Choose `Run Flutter Project` or `Debug Flutter Project`. Run uses a managed `flutter run --machine` session. Debug starts the app paused, waits for its VM service, starts Flutter's debug adapter, and attaches the NetBeans DAP debugger. NetBeans shows native progress for the complete session, including its Starting, Running, and Stopping phases; Cancel in the progress indicator requests an orderly Stop.
6. Invoking Run or Debug again while the current application is Starting or Running asks whether to stop that session and restart in the requested mode and toolbar target. Declining leaves the current session untouched. Changing the toolbar target does not move an already running session; it selects the destination for the next Run, Debug, Build, or confirmed restart.
7. While the app is running, use `Hot Reload`, `Hot Restart`, or `Stop Flutter Application`. Flutter logs, lifecycle messages, emulator progress, and debugger diagnostics are written to a named NetBeans Output tab.
8. After the running application publishes its VM Service URI, choose `Flutter > Open DevTools`. The plugin starts DevTools with the configured Dart SDK on `127.0.0.1` and an automatically assigned port, connects it to that exact application, and opens the resulting URL in the browser configured in NetBeans. Choosing Open again reopens the current URL instead of starting a duplicate server. `Flutter > Stop DevTools` stops only the DevTools server; stopping or replacing the Flutter session, or closing the project, also stops its server automatically.

This integration launches the SDK-provided browser DevTools with native NetBeans actions, Output, and cancellable progress. An embedded DevTools surface and Flutter Inspector/widget-tree UI are separate future work.

Dart files are registered as `text/x-dart` with an incremental lexer, theme-aware syntax categories, a NetBeans EditorKit, a Fonts & Colors preview, and lexer-aware two-space typing indentation. The lexer handles Dart keywords, built-in types, numbers, nested comments, raw and triple strings, interpolation, malformed input recovery, and Unicode identifiers. Enter between `{}` expands an indented body and a leading `}` is aligned without treating delimiters inside strings or comments as code. Typing support reads the live incremental token hierarchy and affected line text instead of copying and re-lexing the whole document for each keystroke.

Opening a Dart source lazily starts the configured SDK's `dart language-server --protocol=lsp` through the NetBeans 30 LSP client; the connection uses the `dart` language id and the Flutter project root, keeps stderr outside the protocol stream, and is stopped when the project closes. NetBeans' standard editor UI consumes diagnostics, completion, definition, references, rename, document/range formatting, Quick Fixes, and source actions. Completion can add an import when Dart returns it as resolved `additionalTextEdits`; missing-import diagnostics offer the SDK fix, and Organize Imports applies the server-provided workspace edit. A narrow NetBeans 30 compatibility bridge preserves the exact original completion `data` and `textEdit` in a private Base64 envelope, temporarily hides the top-level `textEdit`, and restores both before forwarding `completionItem/resolve`; Dart then adds `additionalTextEdits` rather than reconstructing the main edit. NetBeans 30's standard `CompletionProviderImpl` does not execute `resolved.command`, so part-file and other multi-file imports returned in that form require the diagnostic Quick Fix. The status bar reports process start or restart; a deduplicated project-specific notification names a startup failure and opens `Tools > Options > Flutter`. Headless adapter tests, a real-Dart-SDK protocol test, and an optional assembled-runtime editor E2E gate cover the supported paths.

DAP breakpoints, stepping, and variables are available in debug sessions. The NetBeans 30 DAP bridge normalizes omitted or unsupported output categories to the protocol's `console` default and declares Debug ready only after successful `attach` and `configurationDone` responses plus Flutter's `flutter.appStarted` event. A bounded attach watchdog terminates an adapter that cannot complete that handshake.

## Packages, analysis, tests, and pubspec

The standard NetBeans project actions provide Build, Clean and Clean and Build, while the top-level `Flutter` menu and the Flutter project context menu provide `Flutter Pub Get`, `Flutter Analyze`, and `Flutter Test`. The same test support is exposed through NetBeans' standard project `Test` and `Test Single` commands. The top menu also provides `Test Current Dart File` and `Test at Caret` for a literal `test(...)` or `testWidgets(...)` declaration.

These one-shot processes use NetBeans' execution infrastructure rather than a hidden buffered CLI call. Each command gets a named Output tab, native progress, and a Stop control that terminates the process tree. Only one one-shot tooling command runs per Flutter project at a time, and closing the project cancels its active command without blocking the UI.

`Flutter Analyze` runs without an implicit package download and converts reported Dart locations into clickable Output links. `Flutter Test` consumes the public newline-JSON reporter protocol, preserves interleaved suite/test events, maps passed, failed, errored, skipped, and aborted tests into the standard NetBeans Test Results model, and supports rerun from the completed session. The adapter uses NetBeans 30's public `CoreManager` boundary; friend-only Test Results UI classes are intentionally not linked by the plugin.

For `pubspec.yaml`, NetBeans' bundled YAML editor continues to own YAML syntax support. The plugin adds Ctrl+Space completion for pub/Dart/Flutter keys, SDK dependencies, and bounded local-package discovery. Semantic diagnostics cover required package/SDK fields, section types, conflicting dependency sources, Flutter option types, and missing local dependency or asset paths. Other YAML files are not affected.

## Run the plugin in a development IDE

This repository builds a NetBeans plugin, not a standalone Java application. Build the plugin, assemble its development cluster, and then launch a separate NetBeans instance:

```powershell
mvn clean install
mvn nbm:cluster
mvn nbm:run-ide -Dnetbeans.installation=G:/netbeans
```

The development instance uses `target/userdir`, so it does not reuse the settings of the NetBeans instance in which the project is open. The current development package is `netbeans-plugin/target/netbeans-plugin-0.1.3-SNAPSHOT.nbm`; the latest stable package remains `netbeans-plugin/target/netbeans-plugin-0.1.2.nbm`.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

## Status

This is an architectural starter, not yet a production Flutter plugin. Flutter/Dart SDK settings, first-start discovery, platform-selective application creation and later platform addition, Dart-class creation, native project recognition, Dart lexer/highlighting and typing indentation, validated diagnostics/completion/import assistance/navigation/refactoring/formatting/Quick Fixes through the Dart LSP bridge, visible Analysis Server lifecycle, the automatically refreshed standard NetBeans toolbar selector for Desktop/Mobile/Web targets, target-aware Build/Clean/Clean and Build, Android Device Manager, cancellable configured-emulator launch, Run/Debug through Flutter's machine and DAP protocols, cancellable native progress, confirmed session restart, Hot Reload/Restart/Stop, project-scoped browser DevTools launch, native Pub Get/Analyze/Test execution, standard Test Results mapping, and `pubspec.yaml` completion/semantic diagnostics are implemented. Version 0.1.2 focused on lifecycle hardening and native NetBeans integration.

The unreleased 0.1.3 Designer now includes the first Windows native Canvas
slice. Each eligible `.fd` Design tab embeds an isolated real
`FlutterView` without PNG, screenshot or pixel-frame transport and publishes one
bounded validated `CORE_V1` model restricted to `Scaffold`, `Column`, `Row`,
`Text`, `Padding` and `Center`. The toolbar now preserves exact Android Phone,
Android Tablet, iPhone, iPad, Windows Desktop, macOS Desktop and Linux Desktop
targets and carries each target into Flutter's adaptive theme semantics on the
bound Windows engine. These are appearance previews, not device runtimes. Web
uses the same native engine with an exact browser-sized responsive viewport;
it does not claim `kIsWeb`, browser fonts, DOM or plugin behavior. Stable widget
IDs synchronize selection between the Canvas, the revision-bound Explorer widget
tree and standard Properties. The five non-`Scaffold` widgets expose 76 typed
read/write properties. `Text` contributes 59 independently editable leaves in
seven sections; every optional leaf supports Restore Default, and generated
Dart and native Canvas assemble them identically into `TextStyle`,
`StrutStyle`, `Locale`, `TextScaler` and `TextHeightBehavior`. Text colors,
paints and shadows may use either exact ARGB values or semantic Material
`ColorScheme` roles; a `TextTheme` role can be selected as the base style and
all explicit leaves remain local overrides. Structured editors cover the safe
serializable `Paint` subset, ordered `Shadow` values, OpenType `FontFeature`
tags and `FontVariation` axes. The deprecated `Text.textScaleFactor` argument,
`key`, arbitrary Dart expressions and unsupported shader/filter object graphs
remain outside this slice. The active Design lookup supplies the standard NetBeans Palette
with those exact six items. The catalog compatibility matrix admits every one
of those six sources into empty `Scaffold.body`,
`Scaffold.floatingActionButton`, `Center.child` and `Padding.child` slots, or
at the terminal index of `Row.children` and `Column.children`. An empty
`Row`/`Column` exposes its complete bounded design-time area as insertion index
`0`; once populated, only its terminal append zone is admitted. The standard
widget tree accepts the same Palette prototypes on an exact row: `Row` and
`Column` append to `children`, while an empty `Center` or `Padding` receives its
`child`. Parents with several compatible catalog slots remain rejected as
ambiguous by flattened-tree drop; select the parent and use its `Slots`
Properties tab to choose the exact named destination.
An existing non-root widget can be moved within the same widget tree by dropping
on a uniquely compatible container (`ON`) or at a visible before/after boundary
(`INSERT`) of a list slot. The planner applies the catalog acceptance and
cardinality matrix after source removal, rejects root moves, cycles, required-
source violations, full or incompatible slots, invalid indices and exact
no-ops, and preserves the complete subtree and all stable IDs. Commit re-reads
the latest immutable document and rejects a target whose exact command changed
after hover. Selection returns to the moved stable ID. When generation proves
that Dart bytes are unchanged, the same command uses the exact `FD_ONLY` path:
only canonical `.fd` is committed while live/disk Dart and Source Undo remain
byte-for-byte unchanged. A negotiated `widget.movePreview.v1` runner paints the
future destination in amber, but missing Canvas capability never disables the
Swing-tree move. Every slot-capable widget exposes compact native `General` and
`Slots` tabs in the standard Properties window; leaf widgets remain untabbed.
Every catalog-declared slot, including an empty optional slot, is projected in
`Slots`. Its transactional custom editor can add a reviewed Palette widget, move/reorder an existing
widget into the exact named slot, or remove one direct child (`Clear` for an
occupied single slot). It re-plans against the bound immutable revision and
commits one command through the existing Save/Undo pipeline only after OK;
Cancel is a no-op. An occupied single slot is never replaced implicitly. List
insertion is append-only in this first safe slice, while existing list children
can be moved to any catalog-valid post-removal position; list clear-all is
deliberately not exposed as a sequence of partial deletes.
The standard
pair-aware Copy/Paste, Cut/Move, Rename and Delete
described above are also enabled; Linux/macOS native hosts and the broader
writable UI remain future work. Embedded DevTools and its Flutter Inspector are
a separate future milestone.

Canvas presentation is independent from responsive layout: Android Phone
remains 390×844 logical pixels, Web remains 1440×900, and so on, regardless of
the current IDE pane size. `Fit` changes only the paint transform; `100%` shows
the profile at its logical size. Manual zoom can overflow the embedded surface,
in which case Flutter-owned horizontal/vertical scrolling, mouse-wheel
scrolling, Shift+wheel horizontal scrolling and Ctrl+wheel zoom remain aligned
with Flutter hit testing and Palette drop coordinates.
