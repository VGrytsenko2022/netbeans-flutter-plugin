# Flutter and Dart Support for Apache NetBeans

Starter architecture for first-class Dart + Flutter support in Apache NetBeans 31.

Current palette milestone: **52 widgets / the historical 92-widget practical target**
(40 remaining), now including `PhysicalModel`. The repository does not preserve
the full ordered 92-item inventory; widgets are being admitted from the pinned
Flutter API rather than claiming a recovered fixed-order plan.
The current catalog has 754 writable rows (737 outside `Scaffold`), 46 const-capable
definitions, and 2,548 DnD candidates (2,311 accepted / 237 rejected).

### PhysicalModel: shape, clipping and elevation shadows

Basic → **PhysicalModel** exposes all six non-key properties from the
[Flutter constructor](https://api.flutter.dev/flutter/widgets/PhysicalModel/PhysicalModel.html):
`shape`, `clipBehavior`, `borderRadius`, `elevation`, required `color` and
`shadowColor`, plus an optional `child`. Palette creation supplies literal
`0xFF2196F3` for the required color; both colors can use literal ARGB or a reviewed
theme color. Elevation accepts finite non-negative values. All four clip modes,
both shapes, per-corner elliptical radii and translucent colors are supported.

The API takes concrete physical `BorderRadius`, not `BorderRadiusDirectional`:
the editor and validation enforce this distinction. Omission preserves the SDK's
null radius default. Circle ignores but retains the radius, so changing back to
rectangle restores it. On non-square bounds, Flutter's circle shape fills an oval.
The Canvas uses real `PhysicalModel`/`RenderPhysicalModel` painting and shadows;
empty nodes use external Designer selection/drop targets without fake children.

Properties/Slots, Palette/tree/Canvas placement, generation, Save/reopen/further
editing, Undo/Redo and four light/dark SVG variants are covered. The reusable
physical-only radius constraint advances contributor Catalog API to 13; persisted
`.fd` 12 and Canvas model 17 stay unchanged.

### ClipRSuperellipse: rounded-superellipse clipping

Basic → **ClipRSuperellipse** exposes all non-key constructor arguments:
`borderRadius`, `clipper`, `clipBehavior`, plus the optional `child` slot. Physical
and directional elliptical corner radii reuse the structured border-radius editor;
omission preserves `BorderRadius.zero` and `Clip.antiAlias`. All four clip modes
are supported. The real Flutter widget owns continuous corner geometry and radius
clamping, not a `ClipRRect` approximation.

`clipper` supports a closed `CustomClipper<RSuperellipse>` reference in the current
or a declared package library, including members and const/non-const zero-argument
invocations. Configured arguments belong in a project getter/factory, not raw Dart
inside `.fd`. Exact non-null analyzer proof rejects `dynamic`, nullable and wrong
generic clipper types. Flutter ignores `borderRadius` when a clipper is configured;
the radius remains editable and is preserved when the clipper is reset. The isolated
Canvas cannot execute that project code, so it retains the child with a clear,
accessible preview-unavailable warning. Default geometry renders as the real
`ClipRSuperellipse`.

Properties/Slots, Palette/tree/Canvas DnD, stable selection, Save/reopen/further edits,
Undo/Redo and four light/dark SVG variants share the same contract. This slice
introduced no value-format change; the current boundary is `.fd` 12 / Catalog API
13 / Canvas model 17.

### ClipPath: complete API branches within the typed Designer boundary

Basic → **ClipPath** exposes `clipper`, `shape`, `clipBehavior`, and the optional
`child` slot. Omission uses Flutter's default rectangular path and `Clip.antiAlias`.
`clipper` accepts a typed `CustomClipper<Path>` reference; `shape` accepts a typed
`ShapeBorder` reference and selects the real
[`ClipPath.shape`](https://api.flutter.dev/flutter/widgets/ClipPath/shape.html)
static helper. These two properties are mutually exclusive: accepting the other
branch switches them atomically as one undoable change. Each reference editor supports the current library or a
declared `package:` library, an optional member, and an existing value or a const /
non-const zero-argument invocation. Configure constructor arguments in a project
getter or factory; raw Dart text is not stored in `.fd`.

The analyzer must prove the exact non-null type before a change is accepted.
`ClipPath.shape` is never emitted as const, even for a const shape, and its ancestors
correctly lose const eligibility. Save/reopen, further edits, Undo/Redo, child-slot
editing and Palette/tree/Canvas placement use the same revision-bound workflow.
As with the earlier custom clippers, the isolated Canvas cannot execute project
code: a custom clipper or shape receives a visible, accessible **preview unavailable**
warning while preserving the child. Default `ClipPath` renders as the real widget.
The shared value algebra is unchanged: `.fd` 12 and Canvas model 17. Catalog API
is now 13 for the later physical-only radius constraint.

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
    native Flutter Canvas, switch among exact Android/iOS/desktop
    adaptive preview targets allowed by the project, and synchronize stable
    widget selection between the Canvas and the widget tree. The tree
    remains fully expanded, hides its redundant expansion controls and uses
    scoped one-pixel golden parent-child connectors, without changing the look
    of other NetBeans trees. A project
    with `web/` also receives a browser-sized Web layout preview on the native
    engine; browser-only runtime behavior is not emulated. The Windows Canvas
    accepts the fifty-two capability-authorized Palette widgets (`Scaffold`,
    `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`, `Wrap`, `Padding`, `Center`,
    `SizedBox`, `AspectRatio`, `Container`, `Opacity`, `Align`,
    `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
    `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`,
    `ListView`, `GridView.count`, `SingleChildScrollView`, `Text`, `Icon`, `Image`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel` and `ExcludeSemantics`) through a
    fail-closed 2,548-cell catalog matrix with 2,311 accepted and 237 rejected
    combinations, with paired generation,
    analysis, Save and Undo/Redo.
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
12. On the Windows Canvas, double-click or press F2 on the selected existing
    `Text` widget to edit its `data` through a real Flutter `TextField` and
    `TextInputClient`. Enter inserts a newline; Ctrl+Enter commits and Escape
    cancels only when Flutter reports no active composing range. IME preedit
    remains runner-local. One admitted final commit is bound to the exact
    session, revision, layout, interaction fence and selected stable ID, then
    produces at most one existing `SetProperty(data)` command; unchanged text
    is a no-op. Deterministic Flutter and Java protocol/session plus
    view/mutation-bridge tests cover this Windows slice, but physical CJK IME
    acceptance remains open because the current gate host has no
    composition-capable input method. This does not claim a Linux, macOS or
    runtime-faithful Web implementation.

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

- Apache NetBeans IDE 31 for the current development runtime
- JDK 21+
- Maven 3.9+
- Flutter SDK installed separately

## Build

```bash
mvn clean install
```

The Java-only modules can also be worked on independently. The NetBeans module
continues to compile against the `RELEASE300` APIs.

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
  the JSON model `.fd_templates/account/profile.fd`; the current schema-v12
  `source.dartFile` value remains the Dart basename `profile.dart`. Schema v5
  added structured `AlignmentGeometry`, `BoxConstraints`, `Matrix4` and
  `BoxDecoration` values for `Container`; schema v6 adds the shared asset-only
  `ImageProviderValue` and complete typed `DecorationImage`; schema v7 represents
  positive infinity in every BoxConstraints bound as JSON `null`; schema v8
  adds the atomic finite non-negative `Size` value; schema v9 adds the atomic
  finite signed `Offset` value; schema v10 adds the exact payload-free `null`
  property value used by `IndexedStack.index`; schema v11 adds a top-level
  typed `BorderRadiusGeometry` value for `ClipRRect.borderRadius`; schema v12
  adds the closed Dart-object reference used by the clipping widgets' `clipper` rows
  and `ClipPath.shape`, limited to
  a current-library or canonical `package:` root symbol declared by the
  project's `.dart_tool/package_config.json`, optional member and
  reference or zero-argument invocation. Schema v4's closed nullable `IconData`
  remains supported. Schema v1-v11 forms migrate in memory and become canonical
  v12 only
  after an admitted edit. The model stores a safe
  logical app/package asset identity, never image bytes, a filesystem path or
  an executable Dart expression.
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
definitions. The editor's `General`, `Colors`, `Typography` and `Components`
tabs expose all
46 supported non-deprecated Material `ColorScheme` roles and all 15 Material 3
`TextTheme` roles. Each text role has 13 typed optional fields for colors,
font metrics/family, weight/style and decoration; omission means inherit the
seed-derived Material value, while text colors may use either an exact ARGB
literal or another semantic `ColorScheme` role. The compact Components editor
adds exactly 36 typed color leaves for Scaffold, AppBar, Icon and
ElevatedButton states with the same literal/semantic/inherit modes.

Schema v2 adds the portable project-wide switch, schema v3 adds per-definition
switches, schema v4 adds typed color and typography role overrides, and the
current schema v5 adds the closed component-color table. Schema v1-v3
descriptors remain readable and acquire empty override tables; schema v4
acquires empty components in memory. An explicit Save writes canonical v5.
Generated Dart applies
the same ordered `ColorScheme.copyWith` and `TextTheme.copyWith` construction as
the native Canvas, then applies component themes before local widget values.
The generated Dart file is marked as generated and guarded
by the SHA-256 stored in the descriptor; if it was edited outside the theme
editor, Save reports the conflict and leaves those bytes untouched.

Opening an older project does not create or rewrite theme files automatically.
Invoking the editor offers explicit default-theme initialization only when the
existing `lib/main.dart` matches the safely recognized Flutter application
template; unsupported or occupied paths fail without partial writes. The file
formats are documented by the frozen
[`project-theme-v1.schema.json`](docs/flutter-designer/project-theme-v1.schema.json),
[`project-theme-v2.schema.json`](docs/flutter-designer/project-theme-v2.schema.json),
[`project-theme-v3.schema.json`](docs/flutter-designer/project-theme-v3.schema.json)
and [`project-theme-v4.schema.json`](docs/flutter-designer/project-theme-v4.schema.json)
contracts, plus the current
[`project-theme-v5.schema.json`](docs/flutter-designer/project-theme-v5.schema.json).

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

The command uses the installed Apache NetBeans IDE 31 runtime at `G:/netbeans`.
The development instance uses `target/userdir`, so it does not reuse the
settings of the NetBeans instance in which the project is open. The current
development package is `netbeans-plugin/target/netbeans-plugin-0.1.3-SNAPSHOT.nbm`;
the latest stable package remains `netbeans-plugin/target/netbeans-plugin-0.1.2.nbm`.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

## Status

This is an architectural starter, not yet a production Flutter plugin. Flutter/Dart SDK settings, first-start discovery, platform-selective application creation and later platform addition, Dart-class creation, native project recognition, Dart lexer/highlighting and typing indentation, validated diagnostics/completion/import assistance/navigation/refactoring/formatting/Quick Fixes through the Dart LSP bridge, visible Analysis Server lifecycle, the automatically refreshed standard NetBeans toolbar selector for Desktop/Mobile/Web targets, target-aware Build/Clean/Clean and Build, Android Device Manager, cancellable configured-emulator launch, Run/Debug through Flutter's machine and DAP protocols, cancellable native progress, confirmed session restart, Hot Reload/Restart/Stop, project-scoped browser DevTools launch, native Pub Get/Analyze/Test execution, standard Test Results mapping, and `pubspec.yaml` completion/semantic diagnostics are implemented. Version 0.1.2 focused on lifecycle hardening and native NetBeans integration.

The unreleased 0.1.3 Designer now includes the first Windows native Canvas
slice. Each eligible `.fd` Design tab embeds an isolated real
`FlutterView` without PNG, screenshot or pixel-frame transport and publishes one
bounded validated protocol-v17 model restricted by the exact built-in capability
gate to `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`,
`Wrap`, `Text`, `Icon`, `Image`, `Padding`, `Center`, `Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `ListView`, `GridView.count`, `SingleChildScrollView`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `ExcludeSemantics`, `Container`
and `Opacity`.
The toolbar now preserves exact Android Phone,
Android Tablet, iPhone, iPad, Windows Desktop, macOS Desktop and Linux Desktop
targets and carries each target into Flutter's adaptive theme semantics on the
bound Windows engine. These are appearance previews, not device runtimes. Web
uses the same native engine with an exact browser-sized responsive viewport;
it does not claim `kIsWeb`, browser fonts, DOM or plugin behavior. Stable widget
IDs synchronize selection between the Canvas, the revision-bound Explorer widget
tree and standard Properties. The fifty-one non-`Scaffold` widgets expose 737 typed
read/write property rows. `AppBar` contributes 120 independently resettable
leaves across behavior, layout, colors/elevation, shape, icon themes, text
styles and system-UI overlay groups, plus exact `leading`, `title`, `actions`,
`flexibleSpace` and `bottom` slots. `Text` contributes 59 independently editable leaves in
seven sections; every optional leaf supports Restore Default, and generated
Dart and native Canvas assemble them identically into `TextStyle`,
`StrutStyle`, `Locale`, `TextScaler` and `TextHeightBehavior`. Text colors,
paints and shadows may use either exact ARGB values or semantic Material
`ColorScheme` roles; a `TextTheme` role can be selected as the base style and
all explicit leaves remain local overrides. Structured editors cover the safe
serializable `Paint` subset, ordered `Shadow` values, OpenType `FontFeature`
tags and `FontVariation` axes. The deprecated `Text.textScaleFactor` argument,
`key`, arbitrary Dart expressions and unsupported shader/filter object graphs
remain outside this slice. `Icon` contributes its typed positional `icon`
value plus all 12 supported named constructor properties. Its searchable
bundled Material Icons catalog contains 8,825 entries locked to Flutter 3.44.8;
the built-in chooser admits only **None** or one exact registry glyph. The
Properties value cell and chooser rows display that glyph beside its readable
name by asynchronously loading the exact manifest-verified Material font from
the currently resolved Flutter SDK; there is no system-font fallback, and unavailable
preview bytes leave the text selector usable with a neutral placeholder. Generated
applications must keep `flutter.uses-material-design: true`
so those glyphs are available at runtime. `Icon` is a leaf; its omitted
theme-backed fields inherit from `IconTheme`, while `blendMode` and `fontWeight`
remain direct local arguments. Generated Dart and the native Canvas have exact
argument parity. The active Design lookup supplies the standard NetBeans Palette
with the exact fifty-two widgets listed above. `ElevatedButton` adds 286 typed leaves: seven direct
behavior/callback fields, five 54-leaf state groups for default, disabled,
pressed, hovered and focused values, and nine common layout/feedback fields.
Its callbacks store strict Dart identifiers only—never arbitrary expressions.
Generated Dart uses a direct sparse `ButtonStyle`; scalar state leaves use
disabled/pressed/hovered/focused/default precedence, while compound leaves are
combined independently from default through active focused, hovered and pressed
fragments. Disabled styling is isolated from enabled fragments. Omitted leaves
fall through to the non-null `ElevatedButtonTheme` value as one atomic object,
then Flutter defaults. When neither source supplies `fixedSize`, an omitted
axis uses Flutter's infinity sentinel; a finite effective maximum on that axis
clamps the sentinel exactly as `ButtonStyleButton` does. Local minimum and
maximum constraints resolve as one state-aware pair; each maximum axis widens
to its effective minimum, while states without local bounds keep Flutter's
theme/default fallback. Local Text
font family, fallback and package leaves follow the same active-state layering.
Each package applies to the current layered `TextStyle`, including TextTheme;
fallback-only package overrides replace one prior package prefix and never
synthesize a `.../null` family. Local Text theme/inherit configuration is admitted only
with one explicit transition-safe inherit mode across reachable states. The Canvas
receives only `callbackPresence`, never callback identifiers, and installs inert
typed closures that cannot execute project handlers. The state projections
cover all `SystemMouseCursors`, six closed shape presets (`roundedRectangle`,
`roundedSuperellipse`, `stadium`, `circle`, `beveledRectangle` and
`continuousRectangle`) and four splash presets (`InkSplash`, `InkRipple`,
`InkSparkle` and `NoSplash`). Effective text color is owned by
`ButtonStyle.foregroundColor`, so `TextStyle.color` is intentionally excluded;
`ButtonStyle.iconAlignment` is also intentionally excluded from this
arbitrary-child slice: pinned Flutter 3.44.8 reads it only from the private
icon/label child created by `ElevatedButton.icon`, while the ordinary
`ElevatedButton` constructor passes its required `child` through unchanged.
runtime-only keys, focus/state controllers and builders remain outside the
serializable contract. `Container` adds all 13 reviewed non-widget constructor
properties: `alignment`, `padding`, `color`, `isAntiAlias`, `decoration`,
`foregroundDecoration`, `width`, `height`, `constraints`, `margin`, `transform`,
`transformAlignment` and `clipBehavior`. Its structured Properties editors
preserve physical/directional alignment and radii, normalized constraints,
column-major Matrix4 storage, theme-aware decoration colors, exact borders,
ordered shadows and linear/radial/sweep gradients. `color` is mutually exclusive
with `decoration`, non-`none` clipping requires a decoration, and related changes
are committed atomically. Its typed `DecorationImage` covers the pinned 13 SDK
arguments—`image`, `onError`, `colorFilter`, `fit`, `alignment`, `centerSlice`,
`repeat`, `matchTextDirection`, `scale`, `opacity`, `filterQuality`,
`invertColors` and `isAntiAlias`—including all five reviewed `ColorFilter`
variants. `centerSlice` requires a positive-area non-negative rectangle and
permits fit omitted, `fill`, `contain`, `fitWidth`, `fitHeight` or `scaleDown`;
`cover` and `none` are rejected. `onError` is a validated two-argument callback
identifier, never callback source or raw Dart.

The provider matrix is deliberately closed: `AssetImage` or `ExactAssetImage`,
optionally wrapped once by bounded `ResizeImage`; `FileImage`, `MemoryImage`,
`NetworkImage` and custom user-selectable providers remain deferred. A reserved
unresolved ASSET identity is designer state, not a third provider choice; Canvas
and generated Dart map only that state to reviewed built-in placeholder bytes.
The IDE selects only
declared app/package assets discovered from `pubspec.yaml` and
`.dart_tool/package_config.json`, verifies PNG/JPEG/GIF/WebP bytes and their
dimensions, rejects absolute/backslash/traversal/symlink-escape paths and applies
the exact Flutter 3.44.8 DPR algorithm pinned to framework revision
`058e0af2c2b57e369d905a03ac9748b0ebf543c6`.

Canvas model protocol v17 over NBFC framing v1 negotiates
`asset.imageBytes.v1` and sends referenced,
revision-scoped compressed resources only, each addressed by the lowercase raw
SHA-256 of its immutable bytes and checked against exact descriptor size, digest
and order. No filesystem path or callback identifier crosses that boundary.
Native and the internal exact-Web runtime build the same real `DecorationImage`
from an internal `MemoryImage(bytes, scale: resolvedScale)` plus at most one `ResizeImage`;
their `centerSlice` admission mirrors the pinned codecs: native exact resize
uses Flutter's asymmetric missing-axis derivation and honors explicit upscale,
whereas Web rounds either missing axis and returns the intrinsic image whenever
the derived target would upscale. A `fit` target that collapses an axis to zero
is rejected rather than treated as a synthetic one-pixel decode.
Authenticated media/decode/resize/center-slice failures quarantine only the
affected resource; framing, identity, digest, ordering and exact model-resource
coverage failures remain fatal. Unresolved or quarantined assets render a
deterministic non-interactive placeholder with the
logical identity, code and reason. Selection/layout frames, guides and drop zones remain outside
the decorated/transformed `Container`. The Image tab exposes typed accessible
controls and inventory status, and one accepted structured/dependent edit is
one Undo/Redo unit. The optional `child` remains a named single any-widget slot
rather than a property row. The current catalog therefore exposes exactly 754
writable rows across fifty-two widgets, including 737 across the fifty-one
non-`Scaffold` definitions; forty-six definitions use reviewed const constructors.
`.fd` is v12 and the Canvas model protocol is 17. SafeArea's physical-insets
constraint adds the exported `EdgeInsetsValues.directionalAllowed` component,
and `IndexedStack.index` adds the exact payload-free null value; the top-level
typed border-radius geometry used by `ClipRRect` established contributor Catalog
API 11, and its typed project Dart-object reference establishes API 12. Exact-Web product selection remains
separately gated; the currently routed Web choice is the native-engine layout
preview.

`Opacity` is a supported complete vertical slice. Its exact Flutter 3.44.8
contract is the canonical `flutter.widgets.Opacity` const constructor from
`package:flutter/widgets.dart`: required named finite `double opacity` in the
inclusive range `[0, 1]`, optional named `bool alwaysIncludeSemantics` whose
omitted Flutter default is `false`, and one optional single any-widget `child`.
`key` is excluded. A new prototype stores only `opacity: 1.0` and an empty
`child`; omission of `alwaysIncludeSemantics` remains distinct from an explicit
value and generates no argument. The real native and exact-Web projections use
Flutter `Opacity`, not a paint approximation or `AnimatedOpacity`. Zero opacity
does not disable hit testing. It normally removes child semantics, while
`alwaysIncludeSemantics: true` retains them. IDE-owned selection, hit and drop
overlays remain outside the effect, including the zero-size empty-child target.
No Theme or Directionality input is added. This uses the existing double,
boolean and single-slot encodings, so `.fd` schema v6, Catalog API 5, Canvas
model v11, NBFC framing v1 and control/wire v1 remain unchanged.

`Align` is a supported complete vertical slice. Its exact Flutter 3.44.8
contract is the canonical const `flutter.widgets.Align` constructor from
`package:flutter/widgets.dart`: optional `AlignmentGeometry alignment` with
omitted `Alignment.center` default, optional finite non-negative `widthFactor`
and `heightFactor`, and one optional single any-widget `child`. New prototypes
store no properties; omission of either factor remains distinct from explicit
`1`, because a null factor expands a bounded axis. Physical alignment is
direction-independent, while directional alignment resolves against LTR/RTL;
coordinates may extrapolate outside `[-1, 1]`. Native and exact-Web projections
build real Flutter `Align`, and an empty factor-driven zero-size widget retains
an IDE-only selection/drop target. Existing alignment, numeric and single-slot
encodings keep all protocol versions unchanged.

`FractionallySizedBox` adds the corresponding const fractional-layout contract:
optional physical/directional alignment, optional finite non-negative width and
height factors, and one optional any-widget child. `Stack` adds four optional
layout leaves and an ordered list of non-positioned children; its clip value is
passed to Flutter exactly, while extrapolated non-positioned alignment and
descendant paint-only overflow do not become RenderStack visual overflow and are
not clipped. `Expanded` adds optional non-negative `flex` and a required child;
Palette creation atomically wraps an existing direct Row/Column child and never
creates a terminal placeholder. Its child editor is replacement-only.

`Image` is a const leaf with 22 reviewed properties. Its required asset-only
provider is initialized from the deterministic first sorted declared project
asset when one is available. When the inventory is empty, refreshing, verifying
or temporarily unavailable, Palette/tree/Canvas insertion still creates the
`Image` in a compatible slot with an explicit editable placeholder. The
Properties value is shown as `<choose asset>`; Canvas and generated Dart render
a small built-in placeholder without referring to a nonexistent project file.
After adding a PNG/JPEG/GIF/WebP file such as `assets/example.png`, declare it in
the existing `flutter:` block and choose it in **Image data**:

```yaml
flutter:
  uses-material-design: true
  assets:
    - assets/example.png
```

Platform launcher resources such as `android/.../ic_launcher.png`, Apple
`Assets.xcassets` entries and `web/icons/...` are not Flutter runtime assets for
this widget. The unresolved value is persisted as a reserved Designer sentinel,
so Save/reopen keeps the widget valid and its image provider editable; it is
never emitted as an `AssetImage` path. The planner rechecks the latest inventory
at commit: it selects the first current asset or retains the placeholder if none
is usable.
The four center-slice coordinates are all-or-none, strictly ordered and
incompatible with `BoxFit.cover`/`none`.

`TextField` is a const Material leaf named **Text Field** in the Palette. Its 54
optional rows are grouped as Input (14), Layout (9), Behavior (11), Cursor and
selection (11), Callbacks (8) and Restoration (1), with no creation dialog or
stored constructor defaults. Designer persists only reviewed constructor intent,
not typed text, selection, controller or focus state. Cursor-radius and
scroll-padding edits are atomic pair/quartet operations. Generated Dart and the
real non-interactive Canvas TextField use a `LayoutBuilder`/`SizedBox` guard:
unbounded width receives 240 logical pixels and an expanding field under
unbounded height receives 120.

`ListView` completes the originally agreed core Palette as a non-const static
`ListView(children: ...)` slice. It exposes 17 optional constructor-intent rows:
scroll axis/direction, primary-controller policy, six reviewed physics presets,
shrink-wrap, padding, fixed item extent, child lifecycle/repaint/semantic-index
flags, pixel cache extent, semantic child count, drag and keyboard behavior,
restoration ID, clipping and hit testing. Its ordered `children` slot is the
nineteenth any-widget destination. `semanticChildCount` cannot exceed the static
child count; numeric cache extent emits `ScrollCacheExtent.pixels`. Controller,
builders/delegates, `itemExtentBuilder`, `prototypeItem`, deprecated
`cacheExtent`, `key` and raw Dart are excluded. Generated Dart and Canvas build
a real ListView and preserve vertical/horizontal, reverse and LTR/RTL insertion
geometry. Their shared `LayoutBuilder`/`SizedBox` guard supplies width 240 or
height 120 whenever the viewport cross axis is unbounded, and supplies the same
fallback on an unbounded main axis only when `shrinkWrap` is false. The
originally agreed eight-item core list—`Container`,
`Row`, `Column`, `Text`, `Image`, Button through `ElevatedButton`, `TextField`
and `ListView`—is therefore complete 8/8; this does not mean that every Flutter
widget is implemented.

`Wrap` is the first post-core Palette slice. The const default constructor
exposes all nine non-`key` arguments: axis, child/run alignment, finite
`spacing` and `runSpacing` (including negative values), cross-axis alignment,
text and vertical direction, and clipping. Its ordered `children` list is the
twentieth any-widget destination. Generated Dart and Canvas construct the real
Flutter Wrap and preserve framework run layout. An empty Wrap retains an
IDE-only 36-pixel selection target; both empty and populated Wrap nodes use the
full rendered rectangle as a terminal append zone because wrapped runs do not
have one stable terminal edge.

`FittedBox` is the second post-core Palette slice. Its const default constructor
exposes optional `BoxFit fit`, physical/directional `AlignmentGeometry alignment`
and `Clip clipBehavior`, plus one optional single any-widget `child`. Omission
preserves Flutter's `BoxFit.contain`, `Alignment.center` and `Clip.none`
defaults. Generated Dart and Canvas construct the real Flutter FittedBox: Flutter
owns scaling, directional alignment resolves through LTR/RTL, and clipping is
applied only through the selected clip behavior. An empty zero-size FittedBox
retains an IDE-only 36-pixel selection/drop target without changing generated
Dart or Flutter layout. Properties, Palette/tree/Canvas DnD, slot editing,
same-tree movement, Save/reopen, Undo/Redo and the four reviewed light/dark SVG
icon variants use the same closed catalog contract. At that milestone the
practical Material/Base Designer backlog was 22/92 complete.

`ConstrainedBox` is the third complete post-core Palette slice. Its pinned
Flutter 3.44.8 constructor is non-const, requires one typed
`BoxConstraints constraints` value and accepts one optional single any-widget `child`. The
constraints editor represents finite, unbounded and expanding width/height axes:
each finite minimum must not exceed its maximum, positive infinity is valid for
an upper bound, and an infinite minimum is valid only with an infinite maximum.
New instances store the neutral `0..infinity` constraint on both axes. Generated
Dart and both Canvas projections construct the real Flutter ConstrainedBox, so
the framework combines the additional constraints with the incoming parent
constraints. An empty zero-size instance retains a non-layout-affecting Designer
selection/drop target. Properties, creation, Palette/tree/Canvas DnD, exact-slot
editing, same-tree movement, Save/reopen, Undo/Redo and reviewed SVG identity
share the same closed contract. At that milestone the practical backlog was
23/92 complete with 69 remaining, and the Layout category contained 15 items.

[`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
is the fourth complete post-core Palette slice and occupies Layout order 107,
immediately after ConstrainedBox. Its pinned Flutter 3.44.8 const constructor
exposes optional `textDirection`, `alignment`, `constrainedAxis` and
`clipBehavior` arguments plus one optional single any-widget `child`. New
instances persist no property defaults: omission preserves centered alignment,
no retained constrained axis and `Clip.none`; omitted `textDirection` uses the
ambient `Directionality` when directional alignment needs resolution. Generated
Dart and both Canvas projections construct the real Flutter UnconstrainedBox,
so Flutter removes both incoming axes or retains exactly the selected horizontal
or vertical axis. Empty zero-size instances retain only the bounded,
non-layout-affecting Designer selection/drop target. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 24/92 complete with 68 remaining,
and Layout contained 16 items.

[`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
is the fifth complete post-core Palette slice and occupies Layout order 108,
between UnconstrainedBox and Stack. Its pinned Flutter 3.44.8 const constructor
exposes optional non-negative `maxWidth` and `maxHeight` arguments followed by
one optional single any-widget `child`. New instances persist no property
defaults. Finite values are emitted explicitly, while omission canonically
preserves Flutter's `double.infinity` default. Generated Dart and both Canvas
projections construct the real Flutter LimitedBox, so a limit applies only when
the incoming maximum constraint on that axis is unbounded. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 25/92 complete with 67 remaining,
and Layout contained 17 items.

[`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
is the sixth complete post-core Palette slice and occupies Layout order 109,
between LimitedBox and Stack. Its pinned Flutter 3.44.8 const constructor
exposes optional `alignment`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight`
and `fit` arguments followed by one optional single any-widget `child`. New
instances persist no property defaults. Omitted bounds inherit the corresponding
parent constraint; explicit overrides are finite non-negative doubles and each
present minimum must not exceed its matching maximum. Omitted alignment and fit
preserve `Alignment.center` and `OverflowBoxFit.max`; `deferToChild` is also
supported. Explicit non-finite overrides remain outside the reviewed bounded
contract. Generated Dart and both Canvas projections construct the real Flutter
OverflowBox, including physical/directional alignment and `max` versus
`deferToChild` sizing. Properties, creation, Palette/tree/Canvas DnD, exact-slot
editing, same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG
identity share the same closed contract. At that milestone the practical backlog
was 26/92 complete with 66 remaining, and Layout contained 18 items.

[`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html) is
the seventh complete post-core Palette slice and occupies Layout order 130,
immediately after Expanded. Its pinned Flutter 3.44.8 const constructor exposes
optional non-negative portable integer `flex`, optional `FlexFit.loose`/`tight`
and one required single any-widget `child`. New detached prototypes persist no
property defaults; omission preserves `flex: 1` and `FlexFit.loose`. Palette,
tree and Canvas creation atomically wrap an existing direct Row or Column child,
never create an empty terminal placeholder, and reject wrapping either Expanded
or Flexible because the inner parent-data widget would cease to be a direct Flex
child. Generated Dart and both Canvas projections construct the real Flutter
Flexible, including zero-flex inflexible layout and loose or tight positive-flex
allocation. Properties, required-child replacement, Palette/tree/Canvas DnD,
same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG identity
share the same closed contract. At that milestone the practical backlog was
27/92 complete with 65 remaining, and Layout contained 19 items.

[`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html) is the
eighth complete post-core Palette slice and occupies Layout order 140,
immediately after Flexible. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes
one optional positive portable integer `flex`; omission preserves Flutter's
default `1`, while zero, negative and over-limit values are rejected. Spacer has
no slots and is inserted only as a direct `Row.children` or `Column.children` leaf.
Expanded and Flexible cannot wrap Spacer because Spacer internally builds the
flex parent-data path that must remain directly below Row or Column. Generated
Dart emits the real Flutter Spacer. Both Canvas projections keep that real
Spacer directly under the Flex and provide Designer selection through the
surface overlay, never through an invalid render-object wrapper. Properties,
Palette/tree/Canvas insertion, same-tree movement, Save/reopen, Undo/Redo and
reviewed light/dark SVG identity share the same closed contract. At that
milestone the practical backlog was 28/92 complete with 64 remaining, and
Layout contained 20 items.

[`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html) is
the ninth complete post-core Palette slice and occupies Layout order 150,
immediately after Spacer. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required finite-double `baseline`,
required `TextBaseline.alphabetic`/`ideographic` `baselineType`, and one
optional single any-widget `child`. Because Flutter provides no constructor
defaults, a detached Designer prototype stores the reviewed visible starting
values `baseline: 24.0` and `baselineType: TextBaseline.alphabetic`; both remain
fully editable, while NaN, infinity, wrong enum types and raw Dart fail closed.
Generated Dart and both Canvas projections construct the real Flutter Baseline.
A childless node retains framework `constraints.smallest` layout (often zero)
and receives only the non-layout-affecting Designer selection/drop target.
Properties, exact-slot
editing, Palette/tree/Canvas insertion, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 29/92 complete with 63 remaining,
and Layout contained 21 items. `.fd` remains v7, Catalog
API remains 6, Canvas model remains v12, and NBFC framing plus Canvas
control/wire remain version 1.

[`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
is the tenth complete post-core Palette slice and occupies Layout order 160,
immediately after Baseline. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes no writable arguments and one optional
single any-widget `child`. Generated Dart and both Canvas projections construct
the real Flutter IntrinsicHeight, so Flutter performs the speculative intrinsic
height pass while still honoring the parent's constraints. Because that pass is
relatively expensive and can be O(N²) in tree depth, the Palette and slot
descriptions keep the performance warning visible. An empty or collapsed node
receives only a bounded, non-layout-affecting Designer selection/drop target.
Exact-slot editing, Palette/tree/Canvas insertion, same-tree movement,
Save/reopen, Undo/Redo and reviewed light/dark SVG identity share the same
closed contract. At that milestone the practical backlog was 30/92 complete
with 62 remaining, and Layout contained 22 items. `.fd` remains v7, Catalog API
remains 6, Canvas model remains v12, and NBFC framing plus Canvas control/wire
remain version 1.

[`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
is the eleventh complete post-core Palette slice and occupies Layout order 170,
immediately after IntrinsicHeight. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes optional `stepWidth` and
`stepHeight` doubles plus one optional single any-widget `child`. Both steps
must be finite and non-negative. Null and explicit zero remain distinct saved
values; Flutter treats either as no snapping on that axis, while a positive
step rounds the corresponding intrinsic child extent up to its next multiple.
Generated Dart and both Canvas projections construct the real IntrinsicWidth
under the parent's constraints. Palette and Properties descriptions expose its
relatively expensive speculative layout pass and worst-case O(N²) tree-depth
behavior. Empty or collapsed nodes receive only a bounded,
non-layout-affecting Designer selection/drop target. Property and exact-slot
editing, Palette/tree/Canvas insertion, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 31/92 complete with 61 remaining,
and Layout contained 23 items. `.fd` remains
v7, Catalog API remains 6, Canvas model remains v12, and NBFC framing plus
Canvas control/wire remain version 1.

[`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html) is
the twelfth complete post-core Palette slice and occupies Layout order 180,
immediately after IntrinsicWidth. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes one optional boolean `offstage`
(default `true`) and one optional single any-widget `child`. Omission and
explicit `true` are distinct saved/history states even though both hide the
child; explicit `false` restores ordinary layout, painting, hit testing and
semantics. Generated Dart and both Canvas projections construct the real
Offstage. When hidden, Flutter still lays the child out and keeps it active and
focusable, including running animations, while suppressing paint, hit testing
and semantics and normally contributing no parent space. Properties and
Palette descriptions recommend removing a subtree for long-term hiding when
that ongoing work would waste resources. A real zero-sized result retains a
bounded 36x36, non-layout-affecting selection/drop target outside the Offstage
effect. Property and exact-slot editing, Palette/tree/Canvas insertion,
same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG identity
share the same closed contract. At that milestone the practical backlog was
32/92 complete with 60 remaining, and Layout contained 24 items. `.fd` remained
v7, Catalog API remained 6 and Canvas model remained v12; NBFC framing plus
Canvas control/wire remained version 1.

[`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
is the thirteenth complete post-core Palette slice and occupies Layout order
190 immediately after Offstage. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes the required structured `Size size`,
optional physical/directional `alignment` (default `Alignment.center`) and one
optional single any-widget `child`. A detached prototype stores the visible,
finite non-negative starting size `Size(100, 100)`; width and height remain one
atomic typed value in persistence, history and Properties. Generated Dart and
both Canvas projections construct the real SizedOverflowBox: its own size is
the requested size constrained by its parent, while its child receives the
original incoming constraints and may paint outside the box according to the
selected alignment. Flutter still clips hit testing to the parent's bounds.
A true zero-size result retains only the bounded 36x36 Designer selection/drop
target. Property and exact-slot editing, Palette/tree/Canvas insertion,
same-tree movement, Save/reopen and further editing, Undo/Redo and reviewed
light/dark SVG identity share the same closed contract. The practical backlog
was 33/92 complete with 59 remaining, and Layout contained 25 items. `.fd`
was v8, Catalog API was 7 and Canvas model was v13; NBFC framing plus Canvas
control/wire remained version 1.

[`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
is the fourteenth complete post-core Palette slice and occupies Layout order
200 immediately after SizedOverflowBox, matching Flutter's canonical Layout
catalog. Its pinned Flutter 3.44.8 const `Transform.new` constructor from
`package:flutter/widgets.dart` exposes required structured `Matrix4 transform`,
optional signed finite `Offset origin`, optional physical/directional
`alignment`, optional `transformHitTests` (default `true`), optional
`FilterQuality.none/low/medium/high` and one optional single any-widget `child`.
A detached prototype stores only `Matrix4.identity()`; all optional values stay
absent so their framework defaults remain distinct from explicit values. The
new Offset editor changes finite signed `dx` and `dy` atomically. Generated Dart
and both Canvas projections construct the real paint-time Transform without
changing the child's layout size. Origin and alignment compose, optional
filtering is preserved, and child hit tests follow the transform only when
`transformHitTests` is true. Designer selection/drop geometry follows the same
effective transform without changing Flutter layout, with a bounded 36x36
Designer target only for a real zero-size node. Non-finite, projective-horizon,
and non-invertible surface geometry fails closed instead of inventing an AABB
hit region; finite rotated/skewed DnD uses exact local containment. Property and exact-slot editing,
Palette/tree/Canvas insertion, same-tree
movement, Save/reopen and further editing, Undo/Redo and reviewed light/dark SVG
identity share the same closed contract. The named `.rotate`, `.translate`,
`.scale` and `.flip` convenience constructors remain explicitly outside this
slice; equivalent matrices remain expressible through `Transform.new`. The
practical backlog is now 34/92 complete with 58 remaining, and Layout contains
26 items. `.fd` is v9, Catalog API is 8 and Canvas model is v14; NBFC framing
plus Canvas control/wire remain version 1.

[`RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
is the fifteenth complete post-core Palette slice and occupies Layout order 210
immediately after Transform. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required signed integer `quarterTurns`
and one optional single any-widget `child`. A detached prototype stores `1`,
making the clockwise layout-time quarter turn immediately visible. The accepted
range is the exact shared native/Web interval `-9007199254740991` through
`9007199254740991`; larger magnitudes fail closed before generation. Generated
Dart and both Canvas projections construct the real Flutter RotatedBox. Odd
turns exchange the child's layout axes, even turns retain them, and Flutter
paints the equivalent rotation modulo four while the Designer preserves the
exact stored integer. Property and exact-slot editing, Palette/tree/Canvas
insertion, same-tree movement, Save/reopen and further signed editing, Undo/Redo
and reviewed light/dark SVG identity share the same closed contract. The
practical backlog is now 35/92 complete with 57 remaining, and Layout contains
27 items. `.fd` remains v9, Catalog API remains 8 and Canvas model remains v14;
NBFC framing plus Canvas control/wire remain version 1.

[`ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html)
is the sixteenth complete post-core Palette slice and occupies Layout order 220
immediately after RotatedBox. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional `mainAxis` and `reverse`, whose
omission preserves `Axis.vertical` and `false`, plus one ordered any-widget
`children` list. A detached prototype omits both properties and starts with an
empty list. Generated Dart emits the real bare `ListBody`, without a synthetic
wrapper. The native and exact-Web Canvas place that real widget in an
axis-matched design-time viewport so it receives the unbounded main-axis and
bounded cross-axis constraints required by `RenderListBody`; this preview guard
is neither saved nor generated. Empty nodes accept insertion index zero, while
populated terminal geometry follows the configured axis, reversal and ambient
text direction. Property and list-slot editing, Palette/tree/Canvas insertion,
same-tree movement/reordering, Save/reopen and further editing, Undo/Redo and
reviewed light/dark SVG identity share one closed contract. The practical
backlog is now 36/92 complete with 56 remaining, and Layout contains 28 items.
`.fd` remains v9, Catalog API remains 8 and Canvas model remains v14; NBFC
framing plus Canvas control/wire remain version 1.

[`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
is the seventeenth complete post-core Palette slice and occupies Layout order
230 immediately after ListBody. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional finite signed `spacing`,
nullable `alignment`, finite signed `overflowSpacing`, `overflowAlignment`,
`overflowDirection`, nullable `textDirection`, and one ordered any-widget
`children` list. A detached prototype omits all six properties and starts with
an empty list, preserving Flutter's `0.0`, null, `0.0`, `start`, `down` and
ambient-direction defaults. Generated Dart emits the real bare `OverflowBar`.
For a nonempty node, the native and exact-Web Canvas bound an otherwise
unbounded preview width only when `alignment` is non-null; null alignment keeps
Flutter's natural width. They retain a 36x36 empty selection/drop target; those
presentation guards are neither saved nor generated. The real rendered mode selects DnD geometry:
effective LTR/RTL direction orders a fitting horizontal bar, while
`overflowDirection` orders a vertical overflow column. Property and list-slot
editing, Palette/tree/Canvas insertion, same-tree movement/reordering,
Save/reopen and further editing, Undo/Redo and reviewed light/dark SVG identity
share one closed contract. The practical backlog is now 37/92 complete with 55
remaining, and Layout contains 29 items. `.fd` remains v9, Catalog API remains
8 and Canvas model remains v14; NBFC framing plus Canvas control/wire remain
version 1.

[`GridView.count`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
is the eighteenth complete post-core Palette slice and occupies Scrolling order
20 immediately after ListView. Its pinned Flutter 3.44.8 non-const named
constructor from `package:flutter/widgets.dart` exposes 21 typed rows plus one
ordered any-widget `children` list: scrolling axis, reversal, primary policy,
reviewed physics preset, shrink wrap, padding, required positive
`crossAxisCount`, main/cross spacing, child aspect ratio, optional main-axis
extent, child lifecycle flags, pixel cache extent, semantic child count, drag
and keyboard-dismiss behavior, restoration ID, clipping and hit testing. A new
node stores only `crossAxisCount: 2` and starts empty; all optional rows remain
unset so Flutter keeps its constructor defaults. Generated Dart and Canvas use
the real `GridView.count` behind the same 240-wide/120-high guard when required
by unbounded constraints; `mainAxisExtent`, when set, determines the tile main
extent instead of the aspect-ratio-derived value. Controller-owned state,
builders/delegates, other named constructors, `scrollBehavior`, `key` and the
deprecated raw `cacheExtent` are deliberately unsupported. At the
`GridView.count` milestone, the practical backlog was 38/92 complete with 54
remaining; Layout had 29 items and Scrolling had 2. `.fd` remained v9, Catalog
API remained 8 and Canvas model remained v14; NBFC framing plus Canvas
control/wire remained version 1.

[`SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
is the nineteenth complete post-core Palette slice and occupies Scrolling order
30 immediately after `GridView.count`. Its pinned Flutter 3.44.8 const
constructor from `package:flutter/widgets.dart` exposes 10 typed rows plus one
optional any-widget `child`: scrolling axis, reversal, padding, primary policy,
a reviewed physics preset, drag-start behavior, clipping, hit testing,
restoration ID and keyboard dismissal. A new node stores no explicit properties
and begins with an empty child slot, preserving Flutter's exact defaults.
Controller-owned state and arbitrary physics graphs are deliberately
unsupported. Generated Dart and Canvas construct the real
`SingleChildScrollView`. Because the widget deliberately shrink-wraps in both
axes, it does not receive the ListView/GridView generated viewport guard;
Canvas alone retains a non-layout-affecting 36x36 target when the real node is
empty or zero-size. The practical backlog is now 39/92 complete with 53
remaining. Layout remains at 29 items and Scrolling contains 3. `.fd` remains
v9, Catalog API remains 8 and Canvas model remains v14; NBFC framing plus Canvas
control/wire remain version 1.

[`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is the twentieth complete post-core Palette slice and occupies Basic order 40
after `Image`. Its pinned Flutter 3.44.8 const constructor exposes required
`color`, optional `isAntiAlias` whose omitted default is `true`, and one optional
any-widget `child`; only the common `key` argument is excluded. A new node stores
the required literal `Color(0xFF2196F3)`, leaves anti-aliasing omitted and begins
with an empty child. The color editor admits exact ARGB or one reviewed Material
`ColorScheme` token. Literal color output remains const; a token emits
`Theme.of(context).colorScheme...` and correctly makes the surrounding
`ColoredBox` non-const. Generated Dart and Canvas construct the real widget.
Canvas alone retains a non-layout-affecting 36x36 target when an empty box has
zero size; that overlay is never persisted. At the `ColoredBox` milestone, the
practical backlog was 40/92 complete with 52 remaining; Layout had 29 items,
Scrolling 3 and Basic 4. `.fd` remained v9, Catalog API 8 and Canvas model v14;
NBFC framing plus Canvas control/wire remained version 1.

[`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
is the twenty-first complete post-core Palette slice and occupies Layout order
240 after `OverflowBar`. Its pinned Flutter 3.44.8 const constructor exposes
optional `left`, `top`, `right`, `bottom`, signed finite physical
`EdgeInsets minimum`, and `maintainBottomViewPadding`, plus one required
any-widget `child`; only the common `key` is excluded. The detached wrapper
payload omits all six properties, preserving Flutter's `true`,
`EdgeInsets.zero` and `false` defaults, but is never admitted as a standalone
node.
`EdgeInsetsDirectional` is rejected because the public argument is physical
`EdgeInsets`. Palette creation never inserts the incomplete prototype: the
shared generic wrapper command surrounds one existing widget and fills the
required child in the same atomic command. The widget tree
can wrap either the document root or a non-root row; the current Canvas target
wire intentionally exposes only non-root children, so root wrapping remains
available through the tree. Expanded, Flexible and Spacer cannot be wrapped
because their ParentData must remain a direct Row/Column child. Generated Dart
and Canvas construct the real `SafeArea`. At that milestone the practical backlog
was 41/92 complete with 51 remaining; Layout contained 30 items, Scrolling 3, Basic 4 and
Material 4. `.fd` remains v9 and Canvas model remains v14; the exported
`EdgeInsetsValues.directionalAllowed` constraint advances Catalog API to 9.
NBFC framing plus Canvas control/wire remain version 1.

[`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is the twenty-second complete post-core Palette slice and occupies Basic order
50 after `ColoredBox`. Its pinned Flutter 3.44.8 const constructor exposes
optional `color`, `strokeWidth`, `fallbackWidth` and `fallbackHeight`, plus one
optional any-widget `child`; only the common `key` is excluded. A new node
stores no explicit properties and begins with an empty child, preserving
Flutter's exact `Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults.
Explicit numeric values must be finite and non-negative. The color editor
admits exact ARGB or one reviewed Material `ColorScheme` token; a token removes
`const` from generated Dart. Generated Dart and native/exact-Web Canvas
construct the real `Placeholder`, including its real fallback sizing and
optional child. Canvas-only selection and drop affordances remain outside the
widget. At that milestone the practical backlog was 42/92 complete with 50
remaining; Layout contained 30 items, Scrolling 3, Basic 5 and Material 4.
Existing value and
transport shapes keep `.fd` v9, Catalog API 9, Canvas model v14 and NBFC
framing/control/wire v1 unchanged.

[`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is the twenty-third complete post-core Palette slice and occupies Basic order
60 after `Placeholder`. Its pinned Flutter 3.44.8 const constructor exposes
required `TextDirection textDirection` and one required any-widget `child`;
only the common `key` is excluded. Flutter provides no direction default, so
Designer creation persists the explicit reviewed `TextDirection.ltr` value.
Palette, tree and Canvas creation never publish an incomplete node: the shared
generic wrapper command surrounds one existing subtree and fills the required
child in the same atomic command. The property remains editable between exact
`ltr` and `rtl`, and real generated Dart/native/exact-Web Canvas direction
is inherited by directional descendants. Canvas-only selection and drop
affordances remain outside the inherited widget, which owns no render object.
At the Directionality milestone the practical backlog was 43/92 complete with
49 remaining; Layout contained 30 items, Scrolling 3, Basic 6 and Material 4.
Existing value and transport shapes kept `.fd` v9, Catalog API 9, Canvas model
v14 and NBFC framing/control/wire v1 unchanged.

[`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is the twenty-fourth complete post-core Palette slice and occupies Basic order
70 after `Directionality`. Its pinned Flutter 3.44.8 const constructor exposes
required `Decoration decoration`, optional `DecorationPosition position` with
the exact `background` default, and one optional any-widget `child`; only the
common `key` is excluded. The closed Designer branch is the already complete
typed `BoxDecoration` algebra used by Container, including literal and reviewed
theme colors, asset images, borders, physical or directional radii, ordered
shadows, gradients, blend mode and box shape. Custom `Decoration` subclasses
and raw Dart expressions remain outside the reviewed model. New nodes persist
an empty rectangular `BoxDecoration()` so the required property is valid before
the first edit. Generated Dart and native/exact-Web Canvas construct the real
widget and preserve whether the decoration paints behind or in front of its
child; Canvas-only selection and drop affordances remain outside the painted
effect. At the DecoratedBox milestone the practical backlog was 44/92 complete
with 48 remaining; Layout contained 30 items, Scrolling 3, Basic 7 and Material
4. Existing value and transport shapes kept `.fd` v9, Catalog API 9, Canvas
model v14 and NBFC framing/control/wire v1 unchanged.

[`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
is the first Accessibility Palette item, at category order 400 and item order
10. Its complete Flutter 3.44.8 const constructor exposes optional
`bool excluding` with exact omitted default `true` and one optional any-widget
`child`; only the common `key` is excluded. New nodes keep `excluding` omitted.
Generated Dart and native/exact-Web Canvas construct the real widget: omitted or
explicit `true` removes the application child's semantics subtree, while
explicit `false` preserves it. Layout, paint and hit testing still proxy the
child. The `ExcludeSemantics` node's own Designer selection, hit/drop and
accessibility wrapper remains outside the effect, including a transient target
for an empty zero-size node. Descendant Canvas semantics labels follow the real
subtree exclusion; those widgets remain available in the NetBeans widget tree.
At that milestone the practical backlog was 45/92 complete with 47 remaining;
Layout contained 30 items, Scrolling 3, Basic 7, Material 4 and Accessibility 1. Existing value
and transport shapes kept `.fd` v9, Catalog API 9, Canvas model v14 and NBFC
framing/control/wire v1 unchanged.

[`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
is the next completed fixed-inventory Layout slice, at item order 115 beside
`Stack`. Its complete Flutter 3.44.8 const constructor contributes optional
`alignment`, `textDirection`, `clipBehavior`, `sizing` and nullable `index`,
plus ordered `children`. A new node stores no values and no children. **Index**
may remain `<not set>` for Flutter's default `0`, hold a non-negative child
index, or hold explicit `null` to display none; validation keeps every concrete
index within the current child list, while preserving Flutter's empty-list
index-zero exception. Generated Dart and both Canvas routes construct the real
widget. Its layout is the largest child's size, while paint, hit testing and
application semantics expose only the selected child; every child remains in
the ordered Designer model and NetBeans widget tree. At that milestone the
practical backlog was 46/92 complete with 46 remaining; Layout contained 31 items, Scrolling 3,
Basic 7, Material 4 and Accessibility 1. The exact null value advances `.fd`
to v10, Catalog API to 10 and Canvas model to v15; NBFC framing and Canvas
control/wire remain v1.

[`ClipRect`](https://api.flutter.dev/flutter/widgets/ClipRect/ClipRect.html)
is the next completed fixed-inventory Basic slice, at item order 80 after
`DecoratedBox`. Its safe Flutter 3.44.8 const contract exposes optional typed
`clipper`, optional `clipBehavior` and one optional single `child` slot. Omitting clip behavior keeps
Flutter's `Clip.hardEdge` default; the closed editor also admits `none`,
`antiAlias` and `antiAliasWithSaveLayer`. Generated Dart and both Canvas routes
construct the real widget when no custom clipper is set, while Designer selection and empty drop affordances
remain outside clipping. Schema v12 represents `CustomClipper<Rect>` by an
analyzed current-library or declared-package reference; the isolated Canvas
receives presence only and shows an explicit preview-unavailable state.
For an omitted child, the real node remains zero-size and the existing external
36x36 Designer target carries the warning, tooltip and semantics reason.
At that milestone the practical backlog was 47/92 complete with 45 remaining;
Layout contained 31 items, Scrolling 3, Basic 8, Material 4 and Accessibility 1. The surface had 41
reviewed const definitions and 736 writable rows, including 719 outside
`Scaffold`; `.fd` v10, Catalog API 10, Canvas model v15 and NBFC
framing/control/wire v1 remain unchanged.

[`ClipOval`](https://api.flutter.dev/flutter/widgets/ClipOval/ClipOval.html)
is the next completed fixed-inventory Basic slice, at item order 90 after
`ClipRect`. Its safe Flutter 3.44.8 const contract exposes optional typed
`clipper`, optional `clipBehavior` and one optional single `child` slot. Omitting clip behavior keeps
Flutter's `Clip.antiAlias` default; the closed editor also admits `none`,
`hardEdge` and `antiAliasWithSaveLayer`. Generated Dart and both Canvas routes
construct the real widget without a custom clipper, with the default oval inscribed in the child's
layout bounds, while Designer selection and empty drop affordances remain
outside clipping. Schema v12 represents `CustomClipper<Rect>` by the same
analyzed current-library or declared-package reference; the isolated Canvas
receives presence only and shows an explicit preview-unavailable state. For an
omitted child, the real node remains zero-size and the existing external 36x36
Designer target carries that unavailable-preview reason. At that
milestone the practical backlog was 48/92 complete with 44 remaining; Layout contained 31
items, Scrolling 3, Basic 9, Material 4 and Accessibility 1. The surface had 42
reviewed const definitions and 737 writable rows, including 720 outside
`Scaffold`; `.fd` v10, Catalog API 10, Canvas model v15 and NBFC
framing/control/wire v1 remained unchanged.

[`ClipRRect`](https://api.flutter.dev/flutter/widgets/ClipRRect/ClipRRect.html)
is the next completed fixed-inventory Basic slice, at item order 100 after
`ClipOval`. Its safe Flutter 3.44.8 contract exposes optional typed
`borderRadius`, optional typed `clipper`, optional closed `clipBehavior` and one
optional single `child` slot. The radius editor supports physical `BorderRadius` and directional
`BorderRadiusDirectional` geometry with finite, non-negative elliptical corner
radii; omission preserves `BorderRadius.zero`. Omitting clip behavior preserves
`Clip.antiAlias`; the editor also admits `none`, `hardEdge` and
`antiAliasWithSaveLayer`. The clipper editor references an existing value or a
zero-argument constructor, factory or function from the current library or an imported
`package:` library declared in `.dart_tool/package_config.json`, with an optional member and an explicit
const or non-const zero-argument invocation. Deterministic
generation adds its import, and Dart analysis proves the result is assignable to
`CustomClipper<RRect>`. Flutter ignores `borderRadius` when `clipper` is non-null.
Because the isolated runner never executes project Dart, both Canvas routes keep
the child and show an explicit preview-unavailable warning for that branch; they
do not fake the custom clip with `borderRadius`. Without a custom clipper, both
routes construct the real widget. Designer selection and empty drop affordances
remain outside clipping. With an omitted child, the zero-size real node uses
the same external 36x36 Designer target for the warning and complete reason.
The practical
backlog is now 52/92 complete with 40 remaining; Layout contains 31 items,
Scrolling 3, Basic 13, Material 4 and Accessibility 1. The surface has 46
reviewed const definitions and 754 writable rows, including 737 outside
`Scaffold`; `.fd` remains v12 and Canvas model v17. PhysicalModel's constrained
radius contract advances Catalog API to 13. NBFC framing/control/wire remains v1.

Forty-seven any-widget slots provide the reusable destination contract:
`Scaffold.body`, `Scaffold.floatingActionButton`, `Column.children`,
`Row.children`, `ListView.children`, `GridView.count.children`, `ListBody.children`, `OverflowBar.children`,
`Wrap.children`, `Center.child`, `Align.child`,
`FractionallySizedBox.child`, `FittedBox.child`, `ConstrainedBox.child`,
`UnconstrainedBox.child`, `LimitedBox.child`, `OverflowBox.child`,
`Padding.child`, `SizedBox.child`, `AspectRatio.child`, `Container.child`,
`Opacity.child`, `ColoredBox.child`, `Placeholder.child`, `DecoratedBox.child`, `ClipRect.child`, `ClipOval.child`, `ClipRRect.child`, `ClipPath.child`, `ClipRSuperellipse.child`, `PhysicalModel.child`, `ExcludeSemantics.child`, `Baseline.child`, `IntrinsicHeight.child`, `IntrinsicWidth.child`, `Offstage.child`, `SizedOverflowBox.child`, `Transform.child`, `RotatedBox.child`, `SingleChildScrollView.child`, `Stack.children`, `IndexedStack.children`,
`ElevatedButton.child`, and AppBar's `leading`, `title`, `actions` and
`flexibleSpace`. `Scaffold.appBar` and `AppBar.bottom` accept only
`PreferredSizeWidget`, currently the reviewed AppBar. The 49 insertable
destinations and 52 sources form 2,548 candidate cells: 2,311 accepted and 237
rejected. Expanded and Flexible each wrap only an existing direct
`Row.children`/`Column.children` child; Spacer inserts only into those same two
list slots. The other 49 sources enter all 47 any-widget slots, and only AppBar
enters the two trait-bound slots. Expanded and Flexible's required `child` slots
are replacement-only and are therefore not insertable matrix destinations.
SafeArea and Directionality use the generic required-child wrapper mode without
a Row/Column-only outer placement restriction. Their required children are
likewise excluded from the insertable matrix; both sources enter all 47
any-widget destinations, and neither can wrap Expanded, Flexible or Spacer because
their ParentData must remain directly under Row/Column.
`ElevatedButton.child` is an optional-single, required-named-but-nullable slot;
an empty button deterministically emits `child: null`. An empty `Row`, `Column`,
`ListView`, `GridView.count`, `ListBody` or `OverflowBar` exposes its complete bounded design-time area as
insertion index `0`; once populated, only its terminal append zone is admitted.
`ListBody` resolves that edge from `mainAxis`, `reverse` and ambient
`Directionality`. `OverflowBar` resolves its active horizontal or overflow-column
mode from rendered geometry, then applies effective text direction or
`overflowDirection`. `Stack.children`
uses the complete Stack rectangle for both empty and populated z-order appends;
`Wrap.children` likewise uses the complete Wrap rectangle for terminal appends;
AppBar actions use their dedicated logical actions zone. The standard
widget tree accepts the same Palette operations on an exact row: `Row`, `Column`,
`Stack`, `ListView`, `GridView.count`, `ListBody`, `OverflowBar` and `Wrap` append to `children`, while an empty `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`,
`Padding`, `SizedBox`, `AspectRatio`, `Container`, `SingleChildScrollView`,
`Opacity`, `ColoredBox`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect` or `ElevatedButton`
receives its
`child`. Parents with several compatible catalog slots, including AppBar and
Scaffold, remain rejected as ambiguous by flattened-tree drop; select the
parent and use its `Slots`
Properties tab to choose the exact named destination.
SafeArea and Directionality are wrapper sources rather than empty-slot
destinations: dropping either on an exact widget-tree row atomically wraps that
root or non-root subtree. The Canvas route exposes the same operation only for
non-root children and offers no synthetic root target.
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
