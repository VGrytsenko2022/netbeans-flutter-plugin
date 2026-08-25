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

Only after these are stable do we build the Matisse-like Flutter Designer.

## Modules

- `flutter-core-api` — stable Java contracts and shared domain objects.
- `dart-analysis` — lifecycle-safe Dart Language Server process and raw LSP transport.
- `flutter-sdk` — SDK discovery, validation and Flutter CLI process execution.
- `flutter-project` — Flutter project recognition and project metadata.
- `flutter-run` — target discovery, Android SDK/AVD lifecycle services, configured emulator launch, managed machine-mode run sessions, DevTools process integration, immutable Build/Clean and tooling commands, and Analyze/Test protocol parsers.
- `netbeans-plugin` — NetBeans UI integration and actions.
- `netbeans-runtime-it` — assembled NetBeans 30 gates for the packaged module, persisted SDK settings, Flutter project lifecycle, Dart MIME/editor registrations, Flutter actions, and optional real-SDK editor behavior.
- `flutter-designer` — reserved boundary for the future native Flutter designer.

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

- Choose `File > New Project > Flutter > Flutter Application` to generate the standard base application with the configured SDK. The wizard runs `flutter create --template app`, opens the generated project, and selects `lib/main.dart`.
- Choose `File > Open Project` and select any existing Flutter directory containing `pubspec.yaml` and `lib`. It opens as a native NetBeans project with Flutter identity, a logical file tree, Dart source groups, and standard project operations.
- In an open Flutter project choose `File > New File > Dart > Dart Class`. The wizard uses the selected project folder, converts an UpperCamelCase name such as `OrderRepository` to `order_repository.dart`, writes the class and opens it in the Dart editor.

## Run and debug a Flutter application

Flutter execution actions are available from the top-level `Flutter` menu and from a Flutter project's context menu.

1. Make a Flutter project active (or set it as the main project). NetBeans' standard configuration selector in the Run toolbar is populated asynchronously with connected Desktop, Mobile, and Web targets reported by `flutter devices --machine`. The open project refreshes that cached list automatically on a five-second fixed delay; transient failures use a bounded 2/5/15/30-second backoff without blocking the UI or discarding the last good list. Choosing an entry selects the target for Run and Debug and remembers it per project. `Flutter > Select Run Target...` remains available for an immediate refresh and the detailed target dialog.
2. For Android, open `Flutter > Device Manager`. It discovers the Android SDK from `android.sdk`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, Flutter configuration, or platform defaults; lists exact ADB devices and AVD states; and provides Create, Start, Stop, Restart, Wipe Data, Delete, Refresh, and Select Target. Create uses only installed stable-channel system images and never accepts licenses implicitly. Start, Restart, and Wipe wait for the exact AVD to boot and then select its exact ADB serial in the Flutter toolbar. Wipe and Delete require explicit confirmation. Closing the window or cancelling the boot wait does not terminate an emulator that has already started.
3. `Flutter > Launch Mobile Emulator...` remains available for already configured Android and iOS definitions. It starts the selected definition, waits for a uniquely matching new Flutter device, and selects it. The operation has a native cancellable NetBeans progress indicator with Loading, Checking devices, Launching, Waiting, Selecting, and Ready phases.
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

This is an architectural starter, not yet a production Flutter plugin. Flutter/Dart SDK settings, first-start discovery, application and Dart-class creation, native project recognition, Dart lexer/highlighting and typing indentation, validated diagnostics/completion/import assistance/navigation/refactoring/formatting/Quick Fixes through the Dart LSP bridge, visible Analysis Server lifecycle, the automatically refreshed standard NetBeans toolbar selector for Desktop/Mobile/Web targets, target-aware Build/Clean/Clean and Build, Android Device Manager, cancellable configured-emulator launch, Run/Debug through Flutter's machine and DAP protocols, cancellable native progress, confirmed session restart, Hot Reload/Restart/Stop, project-scoped browser DevTools launch, native Pub Get/Analyze/Test execution, standard Test Results mapping, and `pubspec.yaml` completion/semantic diagnostics are implemented. Version 0.1.2 is focused on lifecycle hardening and native NetBeans integration; embedded DevTools, Inspector/widget-tree UI, and Designer work are explicitly postponed beyond it.
