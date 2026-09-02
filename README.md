# Flutter and Dart Support for Apache NetBeans

Starter architecture for first-class Dart + Flutter support in Apache NetBeans 31.

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
    accepts the thirty-two capability-authorized Palette widgets (`Scaffold`,
    `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`, `Wrap`, `Padding`, `Center`,
    `SizedBox`, `AspectRatio`, `Container`, `Opacity`, `Align`,
    `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
    `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`,
    `ListView`, `Text`, `Icon` and `Image`) through a
    fail-closed 992-cell catalog matrix with 849 accepted and 143 rejected
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
  the JSON model `.fd_templates/account/profile.fd`; the current schema-v7
  `source.dartFile` value remains the Dart basename `profile.dart`. Schema v5
  added structured `AlignmentGeometry`, `BoxConstraints`, `Matrix4` and
  `BoxDecoration` values for `Container`; schema v6 adds the shared asset-only
  `ImageProviderValue` and complete typed `DecorationImage`; schema v7 represents
  positive infinity in every BoxConstraints bound as JSON `null`. Schema v4's
  closed nullable `IconData` remains supported. Schema v1-v6 forms migrate in
  memory and become canonical v7 only after an admitted edit. The model stores a safe
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
bounded validated protocol-v12 model restricted by the exact built-in capability
gate to `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`,
`Wrap`, `Text`, `Icon`, `Image`, `Padding`, `Center`, `Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `ListView`, `Container`
and `Opacity`.
The toolbar now preserves exact Android Phone,
Android Tablet, iPhone, iPad, Windows Desktop, macOS Desktop and Linux Desktop
targets and carries each target into Flutter's adaptive theme semantics on the
bound Windows engine. These are appearance previews, not device runtimes. Web
uses the same native engine with an exact browser-sized responsive viewport;
it does not claim `kIsWeb`, browser fonts, DOM or plugin behavior. Stable widget
IDs synchronize selection between the Canvas, the revision-bound Explorer widget
tree and standard Properties. The thirty-one non-`Scaffold` widgets expose 650 typed
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
with the exact thirty-two widgets listed above. `ElevatedButton` adds 286 typed leaves: seven direct
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
`NetworkImage` and custom providers remain deferred. The IDE selects only
declared app/package assets discovered from `pubspec.yaml` and
`.dart_tool/package_config.json`, verifies PNG/JPEG/GIF/WebP bytes and their
dimensions, rejects absolute/backslash/traversal/symlink-escape paths and applies
the exact Flutter 3.44.8 DPR algorithm pinned to framework revision
`058e0af2c2b57e369d905a03ac9748b0ebf543c6`.

Canvas model protocol v12 over NBFC framing v1 negotiates
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
rather than a property row. The current catalog therefore exposes exactly 667
writable rows across thirty-two widgets, including 650 across the thirty-one
non-`Scaffold` definitions; twenty-seven definitions use reviewed const constructors.
`.fd` is v7, the contributor Catalog API
is 6 and the Canvas model protocol is 12. Exact-Web product selection remains
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
provider is selected from the deterministic first sorted declared project asset
before a stable ID is allocated; an empty or unavailable inventory rejects Add
without changing the document. The four center-slice coordinates are
all-or-none, strictly ordered and incompatible with `BoxFit.cover`/`none`.

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
share the same closed contract. The practical backlog is now 32/92 complete
with 60 remaining, and Layout contains 24 items. No later backlog widget has an
explicit order. `.fd` remains v7, Catalog API remains 6, Canvas model remains
v12, and NBFC framing plus Canvas control/wire remain version 1.

Twenty-nine any-widget slots provide the reusable destination contract:
`Scaffold.body`, `Scaffold.floatingActionButton`, `Column.children`,
`Row.children`, `ListView.children`, `Wrap.children`, `Center.child`, `Align.child`,
`FractionallySizedBox.child`, `FittedBox.child`, `ConstrainedBox.child`,
`UnconstrainedBox.child`, `LimitedBox.child`, `OverflowBox.child`,
`Padding.child`, `SizedBox.child`, `AspectRatio.child`, `Container.child`,
`Opacity.child`, `Baseline.child`, `IntrinsicHeight.child`, `IntrinsicWidth.child`, `Offstage.child`, `Stack.children`,
`ElevatedButton.child`, and AppBar's `leading`, `title`, `actions` and
`flexibleSpace`. `Scaffold.appBar` and `AppBar.bottom` accept only
`PreferredSizeWidget`, currently the reviewed AppBar. The 31 insertable
destinations and 32 sources form 992 candidate cells: 849 accepted and 143
rejected. Expanded and Flexible each wrap only an existing direct
`Row.children`/`Column.children` child; Spacer inserts only into those same two
list slots. The other 29 sources enter all 29 any-widget slots, and only AppBar
enters the two trait-bound slots. Expanded and Flexible's required `child` slots
are replacement-only and are therefore not insertable matrix destinations.
`ElevatedButton.child` is an optional-single, required-named-but-nullable slot;
an empty button deterministically emits `child: null`. An empty `Row`, `Column`
or `ListView` exposes its complete bounded design-time area as insertion index
`0`; once populated, only its terminal append zone is admitted. `Stack.children`
uses the complete Stack rectangle for both empty and populated z-order appends;
`Wrap.children` likewise uses the complete Wrap rectangle for terminal appends;
AppBar actions use their dedicated logical actions zone. The standard
widget tree accepts the same Palette operations on an exact row: `Row`, `Column`,
`Stack`, `ListView` and `Wrap` append to `children`, while an empty `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`,
`Padding`, `SizedBox`, `AspectRatio`, `Container`,
`Opacity` or `ElevatedButton`
receives its
`child`. Parents with several compatible catalog slots, including AppBar and
Scaffold, remain rejected as ambiguous by flattened-tree drop; select the
parent and use its `Slots`
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
