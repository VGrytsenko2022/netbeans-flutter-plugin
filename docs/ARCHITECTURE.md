# Architecture

## Principles

1. NetBeans-specific APIs stay at the edge.
2. Flutter SDK and Dart tooling are wrapped behind Java interfaces.
3. CLI subprocesses are never invoked directly from UI code.
4. Run/debug state is represented as domain state, not button state.
5. Designer code cannot become a dependency of core Flutter support.

## High-level structure

```text
NetBeans UI
   |
   +--> Project integration ------> flutter-project
   +--> SDK settings -------------> flutter-sdk
   +--> Run/Debug/DevTools -------> flutter-run + NetBeans DAP/browser
   +--> Pub/Analyze/Test ---------> flutter-run + NetBeans execution/Test Results
   +--> Editor integration -------> dart-analysis
   +--> Pubspec editor -----------> NetBeans YAML + plugin semantic layer
   |
   +--> Future Designer ----------> flutter-designer

Shared contracts -----------------> flutter-core-api
```

## SDK configuration

`netbeans-plugin` owns NetBeans Preferences and the `Tools > Options > Flutter` UI. On the first module start, it asks `flutter-sdk` to discover Flutter and Dart from system properties, environment variables, bundled Flutter tooling, and `PATH`, then persists the result. A shared toolchain resolver is used by both the Options page and SDK-check action so runtime consumers see the same configuration.

## Project integration

`netbeans-plugin` registers a NetBeans `ProjectFactory2` for local directories recognized by `flutter-project`. A loaded Flutter project publishes its `FlutterProjectInfo`, project information, Dart source groups, logical view and standard lifecycle actions through the project lookup. No NetBeans metadata is written into the user's Flutter directory.

The `Flutter Application` New Project wizard validates its inputs and configured SDK on the UI thread, then runs project generation asynchronously through `FlutterProjectCreator` and `FlutterCli`. The generated project directory and `lib/main.dart` are returned to NetBeans for opening.

## Dart analysis

The NetBeans module registers `.dart` as `text/x-dart` and supplies an incremental Lexer API language, EditorKit, theme-derived syntax categories, and a Fonts & Colors preview. The lexer remains responsive on incomplete edits and preserves restart state for nested block comments, raw/triple strings, and `${...}` interpolation. A MIME-scoped `IndentTask` and typing hooks provide Dart's two-space block indentation, `{|}` line expansion, and leading-closing-brace alignment. They operate against the document's live incremental `TokenHierarchy` and line-local text segments, so delimiters in strings, comments, and interpolation text cannot corrupt indentation without a full-document copy or re-lex on each keystroke. Prefix analysis has explicit token/character budgets and falls back to a bounded lexer-aware scan of nearby meaningful lines in large files; closing-brace whitespace changes run as one atomic user edit. The task handles only typing indentation; the Analysis Server remains responsible for explicit document/range formatting.

Semantic editor services use the Dart Language Server supplied by the configured Dart/Flutter SDK. `dart-analysis` owns a raw `dart language-server --protocol=lsp` process connection, keeps stderr separate from framed stdout, and provides bounded idempotent termination. A MIME-scoped `LanguageServerProvider` gives those streams to the built-in NetBeans 30 LSP client with language id `dart`; NetBeans then owns initialize and document synchronization and adapts the advertised server capabilities into diagnostics, completion, navigation, refactoring, formatting, and Quick Fix UI.

NetBeans RELEASE300 implements reverse `workspace/applyEdit` requests but omits `workspace.applyEdit` from its initialize capabilities. Its completion adapter also skips `completionItem/resolve` when the initial item already contains `textEdit`, while Dart can add auto-import `additionalTextEdits` or a `command` during that resolve call. The plugin therefore has two deliberately narrow framed-stream compatibility transforms:

- The outgoing initializer changes only the first `initialize` request to advertise `workspace.applyEdit=true` and updates its content length. Dart can then expose command-based missing-import fixes, Organize Imports, and their workspace edits through NetBeans' existing standard adapters.
- A request correlator identifies `textDocument/completion` responses. For an item with resolvable `data` and an object `textEdit`, the incoming transform serializes the exact original JSON values into Base64 fields inside a private `data` envelope and removes the top-level `textEdit`. Before NetBeans' `completionItem/resolve` request reaches Dart, the paired outgoing transform decodes and restores both original values. Dart therefore receives its original main edit and adds any `additionalTextEdits`; it does not reconstruct that main edit. Frames that do not match a recorded completion request remain byte-for-byte unchanged. A non-resolvable item retains its own `textEdit`, but if a sibling item is transformed the containing JSON frame is reserialized and is not promised to remain byte-exact.

NetBeans 30's standard `CompletionProviderImpl` applies resolved text edits but does not execute `resolved.command`. Dart uses that field for some part-file and other multi-file import completions, so those cases remain a RELEASE300 limitation; the same missing import can be applied through the diagnostic Quick Fix path.

The working directory and initialize root follow the owning project. A project lookup lifecycle owner gives every startup an attempt token, rejects superseded or late results after close, deduplicates failures by generation and reason, and asynchronously terminates the accepted server when the Flutter project closes. A NetBeans status reporter uses the status bar for start/restart and a clickable, project-specific notification for startup failure; normal close is silent. A RELEASE300 feature-contract test guards the required adapters, framed-stream tests cover both compatibility transforms, and an optional raw-protocol real-SDK test exercises diagnostics, completion-added imports through a NetBeans-30-shaped resolve, missing-import Quick Fixes, Organize Imports, definition, references, rename, and formatting. A second optional real-SDK test runs the packaged plugin in an assembled NetBeans 30 runtime, drives the headless EditorRegistry and standard LSP editor adapters, and verifies completion/import edits, diagnostics/Quick Fixes, formatting, navigation/refactoring requests, process restart, and project-close cleanup.

The same MIME type is registered with the NetBeans DAP debugger for runtime breakpoints, stepping, and variables; DAP execution remains separate from the editor LSP process.

## Flutter execution

`flutter-run` keeps Flutter and Android process/protocol code independent of NetBeans UI APIs. `FlutterDeviceService` discovers devices through `flutter devices --machine`, and `FlutterTargetKind` classifies supported targets as Desktop, Mobile, or Web. `FlutterEmulatorService` retains the cross-platform path for definitions exposed by `flutter emulators`. The Android-specific `AndroidSdkDiscovery` and `AndroidAvdService` APIs discover command-line tools, list installed images and hardware profiles, map AVD names to exact ADB serials, and implement create/start/boot-wait/stop/restart/wipe/delete without importing NetBeans APIs.

Each loaded Flutter project publishes a project-scoped `FlutterRunController` and `ProjectConfigurationProvider`. The provider exposes cached `FlutterTargetConfiguration` values through NetBeans' built-in project-configuration combo—the same toolbar control used by Java projects. Device discovery starts asynchronously from the project lifecycle and then self-reschedules on a five-second fixed delay. Repeated requests coalesce into one follow-up query; failures preserve the last good snapshot and retry after 2, 5, 15, and then at most 30 seconds. The SPI getters only return immutable cached state because NetBeans calls them under the project mutex. Target identity is stable by device id, selection is stored in per-project private preferences, and the controller serializes every project-scoped `flutter devices` query used by passive refresh, Select, Run/Debug, and emulator waiting. The same controller backs actions in the top-level `Flutter` menu and the project context menu: Select Run Target, Launch Mobile Emulator, Run, Debug, Hot Reload, Hot Restart, Stop, Open DevTools, and Stop DevTools. Closing the project advances a lifecycle generation, invalidates pending discovery and scheduled refreshes, cancels a pending launch operation, and closes its run, debug, and DevTools processes.

Configured emulator launch owns a separate cancellable `ProgressHandle`. It reports loading definitions, capturing the pre-launch device snapshot, launching, waiting with elapsed time, selecting the resolved target, and readiness. Matching first uses normalized configured id/name and otherwise accepts only one new mobile-emulator device relative to the mandatory pre-launch snapshot; it never selects an unrelated sole emulator or the first of several ambiguous devices. Cancellation interrupts the owned Flutter CLI process, terminates that process tree, suppresses stale preference writes/dialogs across project close/reopen, and documents that an already requested emulator can continue starting.

The Android Device Manager is a global, non-modal NetBeans `TopComponent`, because emulator inventory and processes are machine-wide rather than owned by one Flutter project. Its controller serializes mutations on a background request processor, publishes immutable snapshots to the EDT, uses NetBeans Output/Progress/Dialogs, and selects a target only through the active Flutter project's `FlutterRunController`. ADB mapping uses `adb -s <serial> emu avd name`, and boot readiness uses `sys.boot_completed`; it never guesses by list order. Closing the window or cancelling boot polling detaches from an already started emulator. Only explicit Stop terminates it. Wipe and Delete are confirmed with the exact AVD id and consequences; creation uses installed stable-channel images and never auto-accepts SDK licenses.

Every Run or Debug session owns a native, cancellable NetBeans `ProgressHandle`. It follows the domain state with Starting, Running, and Stopping phases and finishes when the underlying Flutter process exits. The shorter-lived `ActionProgress` reports whether the standard project action successfully reached Running (or reached the verified Debug readiness milestone); completing it at that boundary lets the standard Run/Debug actions become invokable again while the separate session progress remains visible. Cancelling the progress handle delegates to the same `quit()` flow as Stop: it sends `app.stop` after Flutter has supplied an app id, otherwise terminates the starting process, and retains the bounded termination fallback.

Run and Debug remain invokable while a session is `STARTING` or `RUNNING`. The controller first displays a confirmation naming the current project, state, the captured target of the existing session, the toolbar target selected for the replacement, and the requested mode. On confirmation it closes the current DAP adapter, stops the Flutter process, waits for termination, and starts a fresh session in Run or Debug mode; declining keeps the current session. A session already `STOPPING` cannot be restarted.

`FlutterRunManager` starts a selected target with `flutter run --machine --debug --device-id`. Debug adds `--start-paused`. A `FlutterRunSession` drains stdout and stderr, interprets Flutter machine events, publishes application output, tracks `STARTING`, `RUNNING`, `STOPPING`, `STOPPED`, and `FAILED`, and sends `app.restart` or `app.stop` requests for Hot Reload, Hot Restart, and Stop.

For Debug, the controller waits for the machine protocol's VM service URI, starts `flutter debug-adapter --no-dds`, and attaches it to NetBeans through `DAPConfiguration`. A frame-aware compatibility stream supplies DAP's specified `console` default when Flutter omits or sends an unsupported output category, preventing the NetBeans 30 DAP output handler from receiving `null`. Debug readiness requires successful `attach` and `configurationDone` responses plus Flutter's `flutter.appStarted` event, which follows debugger initialization against the VM service. A watchdog starts before NetBeans enters its attach call and destroys an adapter that cannot complete the handshake within the deadline, including an adapter stalled during initialization. The DAP registration enables Dart breakpoints and the debugger UI; it is not a general-purpose direct VM Service integration.

The same controller exposes the VM Service URI of the exact active `FlutterRunSession` to the DevTools launcher. Open DevTools is enabled only while that session is running and its URI is available. The launcher resolves the configured Dart SDK, normalizes the machine protocol's WebSocket VM URI to the service-protocol HTTP form, and starts `dart devtools --machine --no-launch-browser --host=127.0.0.1 --port=0 <service-uri>`. It parses the machine server-start event and opens a connected DevTools URL through NetBeans' configured browser. A second Open request reuses that URL and server rather than starting a duplicate process.

The DevTools process is project-scoped but owned by the concrete Flutter run session. It has its own Output tab and cancellable `ProgressHandle`. Stop DevTools terminates only that server; Flutter session stop/replacement and project close terminate it automatically, while Hot Reload and Hot Restart preserve it. Identity and lifecycle checks around asynchronous process startup prevent a late server or browser launch from attaching to a replacement application.

The controller creates a named NetBeans Output tab for each launched target and forwards Flutter logs, lifecycle state, emulator progress, failures, and debug-adapter diagnostics to it. DevTools uses a separate named Output tab so its server diagnostics do not mix with application output.

## One-shot Flutter tooling

Each loaded project also publishes a separate `FlutterToolingController`; long-lived application state remains owned by `FlutterRunController`. Immutable command values in `flutter-run` describe `pub get`, `analyze --no-pub`, and `test --no-pub --reporter=json` without shell quoting. The NetBeans edge starts them with `ExecutionService` and the local external-execution `ProcessBuilder`, which supplies named Output, native progress, Stop, streaming stdout/stderr, and process-tree termination. One tooling process is active per project. Lifecycle generation tokens prevent a completion from a closed/reopened project from finishing or clearing a newer operation.

Analyze output is parsed in both current and legacy Flutter formats and source locations become Output listeners. Test stdout remains separate from stderr so the newline-JSON protocol cannot be corrupted. An ID-based model accepts interleaved suite/test events, late errors, skipped/hidden tests, and abnormal EOF, then builds the public GSF `TestSession`, `TestSuite`, `Testcase`, `Trouble`, and `Report` models. The UI is reached through a registered public `CoreManager`; the plugin does not link the friend-only `gsf.testrunner.ui` implementation packages.

## Pubspec editor

NetBeans 30's bundled YAML language continues to recognize `pubspec.yaml` as `text/x-yaml` and owns YAML syntax errors. MIME-registered plugin providers first verify the exact filename and an owning Flutter project, then add explicit Ctrl+Space completion and semantic diagnostics. The validator uses a bounded private SnakeYAML Engine representation tree with duplicate keys, aliases, recursion, and document size constrained. Completion scans local packages only below the project root with fixed depth/directory limits and skips build/cache/VCS folders and symbolic links.

## Designer boundary

The future visual designer will depend on the stable project, SDK and analysis services. Its rendering side will eventually use an embedded Flutter Engine/native surface. The current browser DevTools launcher does not provide an embedded DevTools surface, Flutter Inspector panel, or widget-tree model; those remain future UI layers. No designer-specific model is allowed to leak into the basic Dart/Flutter language stack.
