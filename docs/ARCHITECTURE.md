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
   +--> Build/Clean/Tooling ------> flutter-run + NetBeans execution/Test Results
   +--> Editor integration -------> dart-analysis
   +--> Pubspec editor -----------> NetBeans YAML + plugin semantic layer
   |
   +--> Flutter Designer ---------> flutter-designer

Shared contracts -----------------> flutter-core-api
```

## SDK configuration

`netbeans-plugin` owns NetBeans Preferences and the `Tools > Options > Flutter` UI. On the first module start, it asks `flutter-sdk` to discover Flutter and Dart from system properties, environment variables, bundled Flutter tooling, and `PATH`, then persists the result. A shared toolchain resolver is used by both the Options page and SDK-check action so runtime consumers see the same configuration.

## Project integration

`netbeans-plugin` registers a NetBeans `ProjectFactory2` for local directories recognized by `flutter-project`. A loaded Flutter project publishes its `FlutterProjectInfo`, project information, Dart source groups, logical view, standard lifecycle actions, `AuxiliaryConfiguration`, and `AuxiliaryProperties` through the project lookup. All private fragments and properties, including project preferences, are serialized into one transient, slash-free `dev.flutter.netbeans.projectMetadata` `FileObject` attribute on the project root. NetBeans' transient-attribute convention prevents attribute-aware copies from inheriting this private state, and the attribute disappears with the project directory. A project-scoped `MoveOrRenameOperationImplementation` flushes the private preferences node and snapshots the primary container and quarantine attributes before Move. Because some filesystem providers implement Move as copy-and-delete and omit transient attributes, the operation writes a secure, bounded, explicitly typed handoff into `.netbeans/` using append-only `PREPARED`, `TARGET_READY`, and `COMMITTED` phase files. The phases share a transaction UUID and complete snapshot, are confined by local real-path checks, and are decoded by a DTD-free streaming parser whose byte, event, depth, entry, and payload limits apply while parsing. Every new phase is read back before use; the target display-name intent is durable before rename, source deletion happens only after full target verification, and the newest phase is deleted last. The project-open hook runs recovery under the project write mutex before any project service reads preferences. A verified `TARGET_READY` phase is completed idempotently; `COMMITTED` is cleanup-only and never replays an older snapshot. If a target contains only `PREPARED`, its private state is restored but the phase is preserved because the target name cannot be inferred safely; analysis, Run, Tooling, configuration mutation, Copy, and Move remain blocked until the user completes Rename. Malformed or inconsistent handoffs are likewise preserved without accepting unverified state. Rename stores a private NetBeans display-name override and fires the standard project-information change without modifying the Flutter package name in `pubspec.yaml`. The separate shared store, `<project>/.netbeans/flutter-metadata.xml`, is created only when a caller explicitly performs a shared write. After successful handoff recovery, the same open hook migrates legacy private fallback attributes and removes each source attribute only after the replacement is durably readable. Malformed legacy XML is normally preserved inside the container's quarantine; payloads whose Base64 encoding would exceed the bounded container and unexpected non-XML values are moved unchanged to verified transient slash-free quarantine attributes. This preserves recoverable open-file, bookmark, target-selection, and other project state while preventing NetBeans 30's folder-ordering implementation from interpreting the former slash-containing URL attribute names as relative ordering rules.

The `Flutter Application` New Project wizard validates its inputs and configured SDK on the UI thread. Its second step captures an explicit non-empty canonical platform set, offers host-aware and purpose-specific presets without forbidding cross-host project generation, and then runs `flutter create --template app --platforms=...` asynchronously through `FlutterProjectCreator` and `FlutterCli`. Success additionally requires every selected platform directory to exist before the project directory and `lib/main.dart` are returned to NetBeans for opening. Existing Flutter application projects (`project_type: app`) expose `Add Flutter Platforms...` in both the top-level Flutter menu and project context menu; modules, packages, plugins, and unknown types fail closed. The project-scoped tooling controller accepts only canonical platform paths that are completely absent, treats directories/files/symbolic links as occupied rather than overwriting them, uses the configured SDK with native Output/progress/cancellation, and verifies every requested real directory after the CLI exits successfully.

After a new application passes those checks, `flutter-project` provisions the
project-global theme pair: canonical bounded JSON at
`.fd_templates/project.fdtheme` and deterministic generated Dart at
`lib/theme/app_theme.dart`. A strict template-aware transform wires the fresh
root `MaterialApp` to the generated light, dark and mode accessors without
removing the counter sample. Provisioning stages all artifacts, verifies exact
bytes and restores the original `main.dart` on a pre-commit failure; it rejects
collisions and symbolic paths. The descriptor's generated SHA-256 is the shared
read/write guard. `netbeans-plugin` contributes the `.fdtheme` DataObject,
project action and a singleton `Themes` TopComponent registered in NetBeans'
`commonpalette` mode. Its paired persistence compares the exact opened baseline
before publishing a new descriptor/generated-Dart pair. Schema v2 adds a
project-portable `enabled` switch. Schema v3 adds an `enabled` state to every
catalog entry. Schema v4 adds typed override tables for all 46 supported
nondeprecated Material `ColorScheme` roles and all 15 Material 3 `TextTheme`
roles. Each text role admits 13 optional typed `TextStyle` fields; omitted
fields inherit from the seed-derived Material theme. Current schema v5 adds
exactly 36 typed component-color leaves for Scaffold, AppBar, Icon and the
default/disabled/pressed/hovered/focused ElevatedButton states. Schema v1-v3
descriptors remain readable and materialize empty override tables; schema v4
materializes empty components. An explicit save writes canonical v5. Disabled entries remain in the
descriptor but are omitted from the generated Dart map. When the global switch
is enabled, the selected light/dark references must resolve to enabled entries.
Disabling the project themes preserves those references and all entry states
while generated nullable accessors make `MaterialApp` use Flutter defaults.
The generator constructs each customized definition in one deterministic order:
`ColorScheme.fromSeed`, `ColorScheme.copyWith`, `ThemeData.from`, then
`TextTheme.copyWith` and the component themes. Canvas model protocol v9 carries
the same resolved role and component tables and applies that order before
form-local widget overrides. Theme
definitions remain independent of every form `.fd` model.

## Dart analysis

The NetBeans module registers `.dart` as `text/x-dart` and supplies an incremental Lexer API language, EditorKit, theme-derived syntax categories, and a Fonts & Colors preview. The lexer remains responsive on incomplete edits and preserves restart state for nested block comments, raw/triple strings, and `${...}` interpolation. A MIME-scoped `IndentTask` and typing hooks provide Dart's two-space block indentation, `{|}` line expansion, and leading-closing-brace alignment. They operate against the document's live incremental `TokenHierarchy` and line-local text segments, so delimiters in strings, comments, and interpolation text cannot corrupt indentation without a full-document copy or re-lex on each keystroke. Prefix analysis has explicit token/character budgets and falls back to a bounded lexer-aware scan of nearby meaningful lines in large files; closing-brace whitespace changes run as one atomic user edit. The task handles only typing indentation; the Analysis Server remains responsible for explicit document/range formatting. The main MIME registrations implement different service types and therefore intentionally omit layer position attributes; this avoids partial and duplicate folder-ordering rules in NetBeans 30 without changing lookup semantics.

Semantic editor services use the Dart Language Server supplied by the configured Dart/Flutter SDK. `dart-analysis` owns a raw `dart language-server --protocol=lsp` process connection, keeps stderr separate from framed stdout, and provides bounded idempotent termination. A MIME-scoped `LanguageServerProvider` gives those streams to the built-in NetBeans 30 LSP client with language id `dart`; NetBeans then owns initialize and document synchronization and adapts the advertised server capabilities into diagnostics, completion, navigation, refactoring, formatting, and Quick Fix UI.

NetBeans RELEASE300 implements reverse `workspace/applyEdit` requests but omits `workspace.applyEdit` from its initialize capabilities. Its completion adapter also skips `completionItem/resolve` when the initial item already contains `textEdit`, while Dart can add auto-import `additionalTextEdits` or a `command` during that resolve call. The generic client also reports Dart's custom `$/analyzerStatus` notification as unhandled. The plugin therefore has a deliberately narrow framed-stream compatibility layer:

- The outgoing initializer changes only the first `initialize` request to advertise `workspace.applyEdit=true` and updates its content length. Dart can then expose command-based missing-import fixes, Organize Imports, and their workspace edits through NetBeans' existing standard adapters.
- A request correlator identifies `textDocument/completion` responses. For an item with resolvable `data` and an object `textEdit`, the incoming transform serializes the exact original JSON values into Base64 fields inside a private `data` envelope and removes the top-level `textEdit`. Before NetBeans' `completionItem/resolve` request reaches Dart, the paired outgoing transform decodes and restores both original values. Dart therefore receives its original main edit and adds any `additionalTextEdits`; it does not reconstruct that main edit. Frames that do not match a recorded completion request remain byte-for-byte unchanged. A non-resolvable item retains its own `textEdit`, but if a sibling item is transformed the containing JSON frame is reserialized and is not promised to remain byte-exact.
- The incoming stream consumes a message only when it is an id-less JSON-RPC notification whose method is exactly `$/analyzerStatus`. A message carrying an id, malformed protocol data, diagnostics, and all other standard or custom LSP traffic continue through the normal compatibility path.

NetBeans 30's standard `CompletionProviderImpl` applies resolved text edits but does not execute `resolved.command`. Dart uses that field for some part-file and other multi-file import completions, so those cases remain a RELEASE300 limitation; the same missing import can be applied through the diagnostic Quick Fix path.

The working directory and initialize root follow the owning project. A project lookup lifecycle owner gives every startup an attempt token, rejects superseded or late results after close, deduplicates failures by generation and reason, and asynchronously terminates the accepted server when the Flutter project closes. A NetBeans status reporter uses the status bar for start/restart and a clickable, project-specific notification for startup failure; normal close is silent. A RELEASE300 feature-contract test guards the required adapters, framed-stream tests cover every compatibility behavior including exact analyzer-status filtering and pass-through, and an optional raw-protocol real-SDK test exercises diagnostics, completion-added imports through a NetBeans-30-shaped resolve, missing-import Quick Fixes, Organize Imports, definition, references, rename, and formatting. A second optional real-SDK test runs the packaged plugin in an assembled NetBeans 30 runtime, drives the headless EditorRegistry and standard LSP editor adapters, and verifies completion/import edits, diagnostics/Quick Fixes, formatting, navigation/refactoring requests, process restart, and project-close cleanup.

The same MIME type is registered with the NetBeans DAP debugger for runtime breakpoints, stepping, and variables; DAP execution remains separate from the editor LSP process.

## Flutter execution

`flutter-run` keeps Flutter and Android process/protocol code independent of NetBeans UI APIs. `FlutterDeviceService` discovers devices through `flutter devices --machine`, and `FlutterTargetKind` classifies supported targets as Desktop, Mobile, or Web. `FlutterEmulatorService` retains the cross-platform path for definitions exposed by `flutter emulators`. The Android-specific `AndroidSdkDiscovery` and `AndroidAvdService` APIs discover command-line tools, list installed images and hardware profiles, map AVD names to exact ADB serials, and implement create/start/boot-wait/stop/restart/wipe/delete without importing NetBeans APIs.

Each loaded Flutter project publishes a project-scoped `FlutterRunController` and `ProjectConfigurationProvider`. The provider exposes cached `FlutterTargetConfiguration` values through NetBeans' built-in project-configuration combo—the same toolbar control used by Java projects. Device discovery starts asynchronously from the project lifecycle and then self-reschedules on a five-second fixed delay. Every global device result is mapped to one exact canonical project platform and intersected with the active application's real non-symlink platform directories; modules, packages, plugins, unknown targets, and targets for missing scaffolding fail closed. The same compatibility check runs again immediately before Run/Debug, and verified platform addition requests an immediate provider refresh. Repeated requests coalesce into one follow-up query; failures preserve the last good snapshot and retry after 2, 5, 15, and then at most 30 seconds. The SPI getters only return immutable cached state because NetBeans calls them under the project mutex. Target identity is stable by device id, selection is stored in per-project private preferences, and the controller serializes every project-scoped `flutter devices` query used by passive refresh, Select, Run/Debug, and emulator waiting. The same controller backs actions in the top-level `Flutter` menu and the project context menu: Select Run Target, Launch Mobile Emulator, Run, Debug, Hot Reload, Hot Restart, Stop, Open DevTools, and Stop DevTools. Closing the project advances a lifecycle generation, invalidates pending discovery and scheduled refreshes, cancels a pending launch operation, and closes its run, debug, and DevTools processes.

The same project lookup publishes an observable `FlutterProjectPlatformProvider` whose immutable snapshot accepts only real non-symlink canonical `android`/`ios`/`web`/`windows`/`macos`/`linux` directories. It watches direct project-root folder creation, deletion and rename for the project lifecycle, and Add Platforms explicitly refreshes it after MasterFS synchronization. Flutter Designer maps these folders to exact preview targets: Android Phone/Tablet, iPhone/iPad, Windows/macOS/Linux Desktop, and Web. Open Design views retain the exact target, then the same viewport mode, and finally fall back to the first canonical choice; Preview is disabled when the authoritative set becomes empty. The selected Android/iOS/macOS/Linux target is carried through the render profile to Flutter adaptive widget semantics. Web uses its exact browser-sized responsive viewport on the native Flutter engine as a design-time layout preview. It does not emulate `kIsWeb`, DOM, browser fonts, plugins or platform channels; those require an optional future browser-compiled backend.

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

Each loaded project also publishes a separate `FlutterToolingController`; long-lived application state remains owned by `FlutterRunController`. Immutable command values in `flutter-run` describe `clean`, target-specific `build`, `pub get`, `analyze --no-pub`, and `test --no-pub --reporter=json` without shell quoting. Standard NetBeans Build and Clean actions use this controller. Build captures the active cached toolbar configuration and maps its device platform to `windows`, `linux`, `macos`, `web`, `apk`, or `ios`; Clean and Build validates that complete plan before starting and then executes `clean` followed by `build` under one action lifecycle. A nonzero exit, cancellation, project close, or stale generation stops the sequence before another process can start.

The NetBeans edge starts each stage with `ExecutionService` and the local external-execution `ProcessBuilder`, which supplies named Output, native progress, Stop, streaming stdout/stderr, and process-tree termination. One tooling operation is active per project. A captured immutable build target prevents a toolbar change from redirecting an in-flight operation, while lifecycle generation tokens prevent a completion from a closed/reopened project from finishing or clearing a newer operation.

Analyze output is parsed in both current and legacy Flutter formats and source locations become Output listeners. Test stdout remains separate from stderr so the newline-JSON protocol cannot be corrupted. An ID-based model accepts interleaved suite/test events, late errors, skipped/hidden tests, and abnormal EOF, then builds the public GSF `TestSession`, `TestSuite`, `Testcase`, `Trouble`, and `Report` models. The UI is reached through a registered public `CoreManager`; the plugin does not link the friend-only `gsf.testrunner.ui` implementation packages.

## Pubspec editor

NetBeans 30's bundled YAML language continues to recognize `pubspec.yaml` as `text/x-yaml` and owns YAML syntax errors. MIME-registered plugin providers first verify the exact filename and an owning Flutter project, then add explicit Ctrl+Space completion and semantic diagnostics. The validator uses a bounded private SnakeYAML Engine representation tree with duplicate keys, aliases, recursion, and document size constrained. Completion scans local packages only below the project root with fixed depth/directory limits and skips build/cache/VCS folders and symbolic links.

## Designer boundary

The 0.1.3 designer foundation follows the accepted contract in [Flutter Designer Architecture](FLUTTER_DESIGNER_ARCHITECTURE.md). A complete form maps `lib/<relative>/<name>.dart` to `.fd_templates/<relative>/<name>.fd`: the `.fd` JSON document owns the visual widget model, while guarded `imports` and `build` regions in the paired Dart file own only generated source. Current schema v4 keeps `source.dartFile` as the exact Dart basename, without a path separator, and adds a closed typed nullable `IconData` value; schema-v1/v2/v3 documents are migrated in memory and become canonical v4 only on an admitted edit. The mirrored relative directory is enforced by the NetBeans project adapter. `AssetValue` paths resolve from the Flutter project/pubspec root, never from the `.fd` location, and supported-version `extensions` are location-independent opaque metadata that must not encode `.fd`-relative semantics. User code outside the guarded regions is preserved.

The NetBeans edge keeps `.dart` as the technical primary of the single designer editing session, with one Dart `DataEditorSupport` backing the dedicated `Design`/`Source` MultiView and guarded-section persistence. The mirrored `.fd` is not hidden as a cross-folder secondary entry: a separate visible, non-editing model DataObject appears under `.fd_templates` and delegates Open plus the shared pair-aware Copy, Cut, Rename and Delete operations to that one Dart-owned session. Generic DataObject Copy/Move remains disabled so no one-file operation can escape one pair member.

Rename and Delete close a clean editor, stage both paths under deterministic locks and provide exact-byte in-process rollback rather than a durable crash-recovery journal. Pair-aware Copy/Paste uses a custom node transfer without generic loader or OS file flavors and duplicates both members only in their current mirrored relative folder. It copies the Dart bytes exactly and assigns the canonical target `.fd` a fresh `documentId` while changing only `source.dartFile`; cross-directory Copy remains blocked until relative-URI rebasing is specified.

Pair-aware Cut/Move uses a private one-shot `NodeTransfer.CLIPBOARD_CUT` paste. It keeps the basename and exact pair bytes, stays within one Flutter project, and accepts only an already existing writable destination with an already existing mirrored counterpart folder owned by that exact project. A bounded, strict `pubspec.yaml`/`package_config.json`-bound project Dart inventory blocks outgoing relative directives, incoming references to the old location, references that could acquire the destination, unsafe URI/package aliases, nested packages and conservative case/Unicode collisions. At the final boundary, exact proof-file locks and the actual NetBeans 30 MasterFS child-cache mutexes cover every proof/source/target directory and its physical ancestor chain; inventory verification and commit share one EDT admission, and an unavailable/different MasterFS shape fails closed. Move closes a clean source editor, publishes `.fd` before Dart, locks both targets, retires the source identities through reversible `.nbmove` tombstones, then creates fresh target DataObjects. Rollback restores both exact sources before removing owned targets; if safe recreation cannot be proved, verified targets are retained for recovery. Once exact targets and both source tombstones establish commit, late cleanup/provider failures are recovery warnings rather than a false uncommitted result. This is an in-process guarantee with no durable crash journal, and an external non-NetBeans writer remains a residual race.

Only complete mirrored Dart entries are claimed; ordinary Dart files remain on the normal language path. `File > New File > Flutter Designer > Flutter Designer Form` creates both files atomically and accepts targets only in `lib` or its subfolders. `flutter-designer` owns the implemented NetBeans-independent schema, model, validation, generation, preparation, pair-rename, pair-copy and Dart Move-dependency planners, bounded undoable command session and canonical Canvas model projection, including migration of schema v1-v3 to v4. `netbeans-plugin` owns the paired UI, pair operation transactions, native Canvas/tree selection edge, ten-item context Palette, selected-node Properties and the installed `PairSaveCoordinator`/`SaveCookie` persistence edge. Properties are writable for exactly 514 catalog-backed fields across `Scaffold`, `ElevatedButton`, `AppBar`, `Column`, `Row`, `Padding`, `Center`, `SizedBox`, `Text` and `Icon`. Scaffold contributes a closed 17-field scalar slice grouped as Layout, Floating action button, Appearance, Drawer behavior and State restoration; its existing `appBar`, `body` and `floatingActionButton` slots remain unchanged. ElevatedButton contributes 286 typed leaves and one optional-single required-named-nullable child slot; AppBar contributes 120 grouped typed leaves and five exact slots; Text contributes 59 independently editable typed leaves across Text, Accessibility, Locale and scaling, Text style, Paint and effects, Advanced typography and Strut style sets; Icon contributes its typed nullable positional `IconData` plus all 12 supported named constructor properties. ElevatedButton generation uses direct sparse state maps and preserves local → `ElevatedButtonTheme` → framework fallback; Canvas receives callback presence only and never a callback identifier. Semantic Material `ColorScheme`, `TextTheme` and the theme-backed subsets of `ElevatedButtonTheme`, `AppBarTheme` and `IconTheme` inheritance are preserved identically by generated Dart and native Canvas; explicit local values remain local overrides. The bundled searchable Material Icons registry contains 8,825 entries locked to Flutter 3.44.8 and requires `flutter.uses-material-design: true` in generated applications. ADR-025 was first accepted as a historical Text-only terminal-append vertical slice and then as a six-source matrix; the historical SizedBox, Icon, AppBar and ElevatedButton expansions followed. The current contract admits ten exact capability-gated Palette sources across twelve any-widget and two `PreferredSizeWidget` destination slots: 140 candidates, of which 122 are accepted and 18 rejected. Same-tree existing-widget move/reorder is enabled through the shared compatibility planner. The Slots editor exposes explicit atomic replacement for an occupied single slot and atomic clear for a single or list slot, with exact revision/child fences and one chronological Undo step; these commands do not change the Palette compatibility matrix. Cross-form/native-surface moves, definitions without the exact reviewed capability and unreviewed slots remain disabled. No designer-specific model is allowed to leak into the basic Dart/Flutter language stack, and disabling designer UI must not affect ordinary editing, analysis, project or execution support.

The paired DataObject also owns one read-only document controller shared by
all Design clones. It reads and decodes the bounded `.fd` snapshot outside the
EDT, coalesces reload storms, rejects stale background completions, stops
loading after the last clone closes, and reloads after external changes to
either paired file. Every actual load composes built-in and NetBeans Lookup-provided widget
catalog entries off the EDT and carries isolated contributor diagnostics into
the accessible current/future/invalid/oversized/I/O Design status. Opening
Design preserves exact `.fd` and Dart byte baselines and does not modify either
file, the Dart editor document, guarded sections or the Save lifecycle. For a
matching filename it also performs a separate bounded strict-UTF-8 scan of the
on-disk Dart source, verifies exact marker topology, stateless root/scope
binding and normalized declared hashes, and retains payload byte ranges. The
same background load now runs the bounded deterministic stateless generator,
independently rehashes actual and generated payloads, compares both with the
`.fd` declarations, reconstructs the complete candidate in memory and scans it
again. The resulting on-disk three-way state is explicitly
`ON_DISK_THREE_WAY_MATCH`, `CONFLICT`, `UNSUPPORTED` or `UNAVAILABLE`. Even a
three-way match remains read-only. ADR-015 through ADR-018 now supply the
revision-bound live snapshot, scanner- and generator-owned analyzer evidence,
an analyze-before-apply preparation lease, the coordinator, command-side
pending C1→C2 identity lease, durable command lease and lock-time baseline
rechecks plus coordinator-owned analyzed replacement of an exact staged C1 by
C2. ADR-019 additionally supplies one identity-bound candidate-byte/probe
capacity policy from command generation through analyzer admission, while
ADR-020 installs the one native chronological Source/model cursor, successful
Pair-Save re-anchoring and Source-Save durable-anchor overlay. Pre-persistence
loss of staged authority now clears only the semantic graph and retains native
Source history. These remain non-authorizing prerequisites; the runtime/release
matrix now passes in the assembled NetBeans 30 runtime and isolated installed
NBM smoke. The separately discussed visual mutation surface still gates
mutation.

The next non-authorizing layer is now present and connected only through an
internal command/replacement boundary. `flutter-designer` can plan a prospective
old-to-new
managed-source transition from a proven baseline, preserving every
user-owned byte and publishing updated descriptor hashes only after the full
candidate re-scans. The NetBeans adapter reconstructs the exact marker-bearing
UTF-8 content from the marker-masked `StyledDocument` and binds it to document
identity/version and guard identities. `dart-analysis` validates the complete
candidate first through a uniquely versioned isolated native overlay without
writing it to disk. The coordinator derives the real Dart path from its bound
`FileObject` and the navigation trust root from the configured Flutter SDK.
Only an exact passing ticket may enter one EDT operation
which compares the live predecessor, atomically applies both payloads, verifies
the complete candidate and publishes separately bound live evidence. Rejected,
cancelled, replayed or stale analysis never changes the editor. Writable
preparation is currently restricted to strict UTF-8, LF-only, BOM-free Dart
source.

For a staged C1, the coordinator claims the exact pending command before pure
C1→C2 transition planning and registers one replacement reservation before any
disk or live preflight. Only the claim token may adopt, abort or invalidate that
lease. Passing analysis enters one document-atomic apply and joint pair/cursor
finalizer; rejection keeps exact C1. Failed apply restores C1 and binds a fresh
live-evidence identity, while a user edit or external event that makes either
outcome unprovable preserves the content and closes the staged command in
conflict. Deferred command-binding effects are published before pair/cookie
callbacks, after all semantic locks are released.

The same replacement boundary now admits a command from a retained physical
history variant. The coordinator first captures an opaque staged command-source
token containing the exact logical authority, physical cursor/proof identities
and both event epochs; capture itself grants no mutation authority. Reservation
rejects the token after any native cursor, proof or epoch move. At `(C1,S0)` the
token exposes the endpoint-specific `PreparedDesignerPair`, whose durable-side
live template has the durable managed payloads projected into the exact `S0`
unmanaged envelope. The pending command lease retains that pair identity and
derives `C3/S0`, while the logical C1 cursor remains authoritative until the
joint analyzer/document/pair/command adoption. Replacement and recovery consume
the generalized staged-proof contract, so an immutable `SavedHistoryProof` may
be the predecessor without pretending that it has fresh analyzer evidence.

These proofs remain separate by design. The installed NetBeans persistence
edge adds a canonical prepared pair, a mandatory coordinator lease acquired
before analysis, exact ticket/analyzer/applied-live evidence binding, one
pair-aware `SaveCookie`, and an ordered two-lock transaction with exact
baseline checks, verified rollback/reread and owned-event correlation. The
state/cookie side effects use an ordered drain whose queue monitor is never
held across NetBeans listeners, `CookieSet`, or `DataObject` callbacks. The
drain logs and isolates even fatal presentation callback failures after
restoring publication ownership, so they cannot retroactively change a
completed document or persistence result. The
Source and Design MultiView elements expose the same DataObject-owned combined
Undo/Redo identity while the native NetBeans editor manager retains savepoints,
grouping and document locking underneath. Internal apply, restore, verification
and post-commit Undo barriers defer and coalesce only the outward presentation
event until the document lock is released; an active callback cannot start a
new Designer document transaction. Undo/Redo actions own their exact delegate;
binding is rejected and binding close is deferred until that action or internal
document barrier completes. The
scanner-owned `StatelessWidget` occurrence and the complete generator-owned
manifest are mandatory probes. A durable command lease pins one exact dirty
cursor; `FD_ONLY` commits verify both files while writing only `.fd`, preserve
the Dart document and Source Undo state, and re-anchor only after a verified
commit. Abandoning preparation before apply remains clean; after an observed
apply/rollback mutation it restores the exact pre-apply live snapshot under one
document-atomic lease-release barrier but deliberately keeps Source dirty with
the stable `SaveCookie` until an ordinary Save. No reload can erase a newer
queued edit. A durable commit becomes the only new disk baseline. Successful
Pair Save retains semantic native-history entries and re-anchors their
immutable endpoints without fabricating analyzer proof. When the loaded `.fd`
baseline is schema v1, the first admitted edit already owns a canonical v2
command revision; the first Pair Save derives every re-anchored durable `.fd`
endpoint from that proven canonical revision while preserving each exact
historical Dart envelope. Undo therefore cannot resurrect the raw v1 model or
mix it with the new durable anchor. Ordinary Source Save
above a saved semantic edge opens an identity-bound `SourceAnchorLease`, accepts
the new durable Source anchor only when both managed payloads are byte-for-byte
unchanged, and rebuilds retained revision proof identities against that anchor.
Its saved semantic endpoint is an overlay: the command/durable side is the new
`S2`, while the native bytes immediately below the Source edit remain `C1`.
The controller adoption ticket, command lease, durable baseline, edge graph and
cursor are checked as one operation before controller, command and Pair effects
publish in order. A post-CES failed outcome or committed split-authority risk clears
semantic authority and enters sticky conflict without discarding native Source
history or reloading user content. Pair Save after a Source overlay retains
endpoint-specific physical proofs, preserving the two-axis chronology
`(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and its reverse. Exact anchor payloads are
projected into each historical unmanaged envelope before semantic derivation;
unique physical variants share the command-history byte budget. A byte-identical
`UNCHANGED` Source Save preserves command/Current/edge identities and refreshes
only the CES cursor, while a trimmed final semantic edge still leaves its owner
available for the next exact Source re-anchor. A new command at `C1/S0` counts
its candidate against that same aggregate physical budget before analyzer or
CES work. Adoption creates the exact branch `B/S0→C1/S0→C3/S0`, truncates the
old native `S2/C2` redo suffix and leaves durable `C2/S2` unchanged. Undo/Redo
restores the endpoint-specific saved proof, while rejection, cancellation and
pre-apply recovery retain the same predecessor proof and bytes. The adopted
revision carries its `S0` live envelope forward, so a following ordinary C4
command cannot silently fall back to the canonical `S2` envelope.

The edge admits only capability-gated writable Designer callers. The internal
C1→C2 analyzed replacement, noncanonical physical-endpoint admission,
chronological replay, Pair/Source Save re-anchoring and shared capacity budget
are complete. Properties, Palette/tree insertion, selected-widget Delete,
same-tree move/reorder and exact-slot management enter that one pipeline; every
unreviewed writable-UI contract remains gated. The packaged runtime, strict
release verifier and isolated NetBeans 30 install lifecycle now pass.

The Canvas boundary follows ADR-021. NetBeans owns the Swing Palette,
Explorer/Nodes widget tree, Properties window and MultiView chrome. The Canvas
inside that chrome is a real embedded native `FlutterView`, not a Swing-painted
projection or a PNG/JPEG/raw-pixel stream. The Flutter engine owns painting,
designer overlays and widget hit testing. The platform SPI isolates
native attach/detach, resize/DPR, focus, visibility, peer lifecycle, liveness
and destruction. That narrow provider-neutral SPI is now exported; Windows has
the concrete provider and host. The same provider now owns one strict immutable
runner contract covering the Flutter build target/options, output and executable
boundary, required/allowed runtime files and directories, cache policy/identity
and exact process launch. Shared build, runtime-cache and session orchestration
consume that contract and contain no Windows provisioning literals. The current
Windows provider publishes the exact existing Windows build, runtime artifact,
cache-manifest and opaque HWND launch behavior. The deterministic selector
activates exactly one complete supported provider whose contract matches the
detected platform and fails closed on ambiguity, missing contracts or platform
mismatch. Linux and macOS still expose explicit fail-closed unavailable
placeholders with no runner contract; support requires separate real providers
and assembled physical acceptance rather than placeholders or emulation. An
isolated runner process is preferred wherever the platform can safely host and
supervise its child surface.

The first Java-to-Flutter drag/drop vertical slice was Windows-only and
Text-only. It established that a native OLE drag carries only the bounded
process-local one-shot opaque token issued by the active Designer view. That
historical source restriction is superseded by the current ten-definition
capability-gated catalog matrix; the transport and identity fencing remain
unchanged. Before native transfer, the source-aware control binds that token to
the exact current canonical type and traits for compatible hover filtering. The
same exact token is then carried unchanged through hover, prepare and the terminal
commit or cancel; only Java can resolve it to the retained prototype. The
historical first slice used the live-tree hit test for a terminal
`Row.children` or `Column.children` zone; the current resolver also exposes the
reviewed empty-single targets. Windows reports OLE `MOVE` because the NetBeans Palette
offers `ACTION_MOVE`; Palette entries are immutable prototypes, so this
transport effect removes nothing. The Designer operation is nevertheless
always semantic `ADD`, never an existing-widget Move or reorder.

Native hover starts fail-closed and reports `MOVE` only after the exact latest
Flutter probe is approved. A fast release may enter final validation while the
exact latest already-sent probe remains in flight: hover and prepare share one
FIFO `MethodChannel`, so Flutter observes and resolves the hover first. Drop
then sends an exact prepare which stores one semantic candidate but emits no
Java event. The Windows STA waits at most 250 ms while pumping COM/window
messages. Timeout, error, reentrant cancellation or shutdown sends the matching
cancel/leave when the channel remains alive and returns OLE `NONE`; a late
prepare result therefore cannot mutate Java state. Only a timely positive
prepare sends the single-use commit and returns OLE `MOVE`; commit revalidates
the presentation, layout, token, probe, point and semantic target before it may
emit `runner.paletteDrop` with `operation = ADD`.

The runtime response is bound to the exact session, presentation, document,
logical revision, frame, layout and intent sequence. A successful OLE `MOVE`
retains the token for a bounded three-second asynchronous grace so the committed
runner event can reach Java; cancel, failure and non-`MOVE` completion revoke it
immediately, and consumption or grace expiry is final. OLE `MOVE` confirms only
timely Flutter prepare and commit dispatch, not Java command admission. Java
still consumes the token atomically, rejects stale/replayed/foreign or
no-longer-valid responses, revalidates the current parent and slot, and
remains the only path to an `AddWidget` command. That command uses the
established generation, analysis, paired replacement, Save and chronological
Undo/Redo pipeline. The runner receives no widget payload from the drag,
project paths, Dart source, file handles, Save, Undo/Redo or persistence
authority. DnD outside the ten-source, 140-candidate matrix (122 accepted,
18 rejected) remains disabled; process separation is not described as an OS security
sandbox.

The current internal slice implements the NetBeans-independent Canvas
identities/admission gates, bounded native-surface request validation, a pure
per-MultiView lifecycle controller, the exact version 1 lifecycle handshake and
fail-stop bounded process framing. After the handshake, bounded runtime control
publishes one exact validated revision, acknowledges its layout, synchronizes
selection and capability-gates the narrowly typed `runner.paletteDrop` and
`runner.textEditCommit` intents.
Protocol negotiation and decoding do not authorize mutation. The canonical
protocol-v9 model payload admits only exact reviewed built-in definitions with
the Canvas capability: `Scaffold`, `AppBar`, `Column`, `Row`, `Text`, `Icon`,
`Padding`, `Center`, `SizedBox` and `ElevatedButton`. Java fingerprints the complete property/slot schema and the
isolated Flutter runner independently enforces the same reviewed contract; it
cannot execute arbitrary project code. `CATALOG_JSON` remains reserved for a
future versioned catalog contract.

The Windows edge embeds the runner's real child `FlutterView` in a heavyweight
AWT host inside each Design MultiView, validates the exact
parent/runner/`FLUTTERVIEW` HWND and PID hierarchy, and owns an SDK-keyed bounded
build cache and per-view process lifecycle. The Design toolbar resolves exact
viewport/platform pairs filtered by the owning project's configured platforms.
Android/iOS/macOS/Linux choices apply Flutter's adaptive `ThemeData.platform`
on that bound Windows engine; they do not claim a device OS runtime. Web renders
its exact browser-sized responsive viewport on the same engine as an explicit
layout preview, using Windows adaptive controls because Flutter has no
`TargetPlatform.web`. It does not claim browser-runtime fidelity.
Platform-folder changes reconcile open Design views on the Swing event thread;
an empty configured set leaves the selector empty and Canvas unavailable.

The separately selected runtime-faithful Windows Web architecture compiles the
same bounded runner from `main_web.dart` as a static Flutter Web release bundle.
It embeds Flutter directly into one browser-managed DOM host through multi-view;
it does not introduce an iframe, transferred image or second model protocol. A
narrow native Win32 adapter now creates a windowed Microsoft Edge WebView2
controller as a child of the heavyweight AWT peer and owns the COM STA, message
pump, focus, bounds, visibility and teardown edges. The adapter and Microsoft's
architecture-matched loader are packaged with an exact size/SHA-256 manifest,
license and notice; Java extracts and revalidates that closed x64 bundle before
JNA loads it. JavaFX WebView is not an alternative backend, and Java never owns
WebView2 COM objects directly.

The internal host foundation now extends through the browser page boundary. The
shared Canvas runtime has process-specific and browser-specific I/O adapters,
and the Web entry point creates one widget root per browser-managed Flutter view.
The JavaScript bridge is available only when WebView2 and the exact host-injected
256-bit session nonce are present. It fixes the bridge format, version and
direction, bounds canonical base64 chunks, diagnostics and pre-registration
buffering, and carries the existing NBFC bytes with contiguous host/runner
sequences. Foreign nonces are ignored; malformed, oversized, out-of-order or
wrong-envelope messages carrying the exact session nonce terminate the channel
and publish one bounded failure. Dart independently checks the nonce, sequence
and decoded byte bound and fails closed on drift or replay. Focused Dart tests
and a manually executable headless-browser harness cover both sides of that
boundary. The explicit Web runtime profile observes binding metrics so resize
invalidates stale geometry and republishes layout, but it neither installs nor
advertises the Windows OLE Palette DnD channel. A twice-reproduced pinned Flutter
release build from the offline dependency cache proves the Web compiler closure
and locally packaged CanvasKit boundary. For the current English-only scope it
also registers local Roboto, ships its Apache-2.0 license and routes fallback
fonts to packaged assets.

Before hosting, the validated mutable `build/web` evidence is copied file by
file into a private leased per-generation publication. Every source and
published file is rechecked by size and SHA-256; links, reparse points, hard
links, traversal, case aliases and source mutation fail closed, while
`.last_build_id` is verified but never served. ABI v2 passes the sorted
path/size/SHA-256 manifest to native code, which rehashes every file and freezes
the complete bounded resource set in memory before returning from session
creation. Browser requests therefore never reread executable files and no
project/publication folder remains as a WebView2 fallback mapping.

A nonce/generation digest selects one lowercase `.invalid` HTTPS origin. Only
exact known snapshot paths and the exact `/index.html` navigation are admitted.
The frozen CSP and response security headers block workers, off-origin execution
and unapproved resource classes; the native host also requires exclusive
per-session user-data ownership and denies permissions, downloads, new windows,
external drop, DevTools and context menus. The Java bridge revalidates exact
document source, nonce, direction, canonical base64 bounds and contiguous
sequences before exposing the existing NBFC byte streams.

Runtime admission requires `100.0.1185.39` or newer, sets that same native
target-compatible version and retains COM `QueryInterface` capability checks.
The standalone native smoke has physically accepted installed-Runtime probing,
exact release-page/authenticated bridge readiness, an authenticated NBFC
`host.hello` → digest-verified `runner.hello` round trip, read-back-verified
bounds and visibility changes, the focus API, and deadline-bounded closed
teardown. That proof is deliberately not product routing or the assembled
NetBeans browser acceptance matrix. No Web provider build/cache/session route,
Web `CanvasEngineIdentity`, production Retry lifecycle, or product model/layout/
selection and mixed-DPI gate is enabled. Product routing additionally requires
the AWT parent HWND to outlive confirmed native teardown, a terminal poisoned
host state after an unconfirmed native destroy, browser-process-exit
synchronization before user-data-folder deletion, and retry-safe partial UDF
cleanup that preserves ownership evidence. Until those separate gates pass,
selecting Web still uses only the native-engine responsive layout preview above
and must not claim `kIsWeb`, DOM, browser fonts, plugins or platform channels.
Web input acceptance is English-only; physical CJK IME and other language-
specific input remain deferred to the final internationalization phase.

Windows live resize is a latest-only transaction over parent-owned geometry.
The verified heavyweight AWT parent client bounds and its DPI are authoritative;
only one asynchronous child resize may be in flight, and an AWT resize burst
retains only its newest target until both the runner window and its
`FLUTTERVIEW` have settled. An embedded runner is a `WS_CHILD`: it always
refits the parent client bounds, ignores the screen-coordinate suggested rect
from `WM_DPICHANGED`, and refits again on `WM_DPICHANGED_AFTERPARENT` while
resizing Flutter content. The suggested rect remains valid only for a
standalone top-level runner.

The Java handshake requires `surface.presentation.v1`. With that capability,
every `runner.presented` is emitted after Flutter's post-frame callback and
carries the exact implicit `FlutterView` `physicalWidth`, `physicalHeight` and
`devicePixelRatioMicros`. Both dimensions are strictly positive, remain within
the negotiated per-dimension and total-pixel limits, and the DPR is a strictly
positive integer no greater than 2,147,483,647. A changed host surface
immediately invalidates the old confirmation. Intermediate frames may advance
the monotonic layout sequence, but the host keeps Canvas input and viewport
publication fenced until a newer post-frame presentation exactly matches the
latest host width, height and DPR; only that final layout can receive and
acknowledge the current interaction fence. Deterministic host/session/runner
tests cover rapid latest-only bursts, a size B → A return that still requires a
newer layout, and rejection of stale callbacks and intermediate metrics. The
assembled Windows physical-divider runtime gate drives a production divider
burst and requires the same live runner generation, exact
parent/runner/`FLUTTERVIEW` bounds, exact post-frame metrics and restored input
synchronization at the final target.
Stable widget IDs synchronize selection between the native
Canvas and the Explorer/Nodes tree; the selected Node is published through the
standard Explorer lookup with catalog-driven typed Properties, while the active
Design lookup supplies a Palette filtered to the exact ten widgets carrying
the Create capability. All ten built-ins, including `Scaffold`, admit the
reviewed 514-property Set/Reset slice through an exact revision token and
analyzed pair-save. The 120 AppBar and 59
Text leaves use typed string/list, optional checkbox, numeric, enum,
literal/theme color, structured Paint and ordered Shadow/OpenType editors, then
reach generated Dart and the runner through one shared composite mapping. The
286 ElevatedButton leaves use strict callback identifiers and direct sparse
state-aware `ButtonStyle` projections; Canvas receives callback presence only.
The 17 Scaffold leaves use closed static presets, literal/theme colors,
optional booleans, a non-negative numeric editor, strict callback identifiers
and a bounded restoration ID. Widget-valued Scaffold constructor parameters,
`persistentFooterDecoration` and `bottomSheetScrimBuilder` remain outside this
scalar slice.

The Windows-only inline Text slice is activated only for the selected existing
`flutter.widgets.Text`, by double-click or F2. Flutter temporarily owns a real
multiline `TextField` and `TextInputClient`; ordinary Enter inserts a newline,
Ctrl+Enter requests final commit, and Escape requests cancel. Commit and cancel
are ignored while Flutter reports a non-empty composing range, so preedit stays
inside the runner and is never projected as a Java property mutation. The AWT
carrier disables input methods and the native host neither interprets nor
relays `WM_IME`; the embedded Flutter engine owns OS text input directly.

`widget.inlineTextEdit.v1` gates one final `runner.textEditCommit`. Its strict
body carries the exact presentation/document/logical-revision/frame/layout and
intent identities, the current interaction-fence sequence, selected widget ID,
well-formed bounded Unicode text and `compositionObserved` metadata. Java
rejects missing/extra/malformed or oversize bodies, unnegotiated, stale,
foreign, replayed or hidden events, a mismatched fence/layout/selection, and
every non-`Text` widget. One admitted event becomes at most one existing
`SetProperty(data)` command; unchanged text is a no-op. Generation, analyzer
admission, Pair Save and chronological Undo/Redo remain the sole persistence
path. Deterministic Flutter and Java codec/channel/session plus
view/mutation-bridge tests accept this product slice. Current acceptance is
English input only. Physical CJK IME and other language-specific acceptance are
deferred to the final internationalization phase because the current assembled
gate host has no composition-capable input method; Linux/macOS and
runtime-faithful Web inline editing are not implemented by this decision.

A standalone automated Win32 smoke proves the three-window hierarchy and resize
path; its `JFrame` is only a test harness and is not part of the plugin UI. The
assembled Windows gate additionally opens production Designer DataObjects and
MultiViews and proves distinct runner/child HWND and PID ownership, exact DPR
from the current verified `FLUTTERVIEW`, one tab peer-loss recreation, forced
crash → explicit Retry with a fresh PID, authenticated close followed by
natural process exit, and final runner/child-HWND cleanup after every captured
runner generation physically exits. Through NetBeans' production Split Document
Horizontally and Clear Split actions, the same gate runs two simultaneous Design
surfaces with independent runner PID, AWT-parent/runner/`FLUTTERVIEW` HWND,
exact metrics, rendering and focus. Clear Split must destroy the removed runner,
`FLUTTERVIEW` and AWT-parent HWND while the retained surface stays rendered and
interactive; a second split/clear cycle rejects retired PID reuse and proves the
bounded tab-and-split heavyweight-peer recreation matrix. Peer loss uses the first immutable
`BACKEND_REPLACED` close reason;
teardown authority requires both physical `host.close` write completion and an
exact-session `runner.closed`. Acknowledged runners receive a bounded
natural-exit grace before truthful termination escalation; their runtime lease
is retained until physical exit. Design activation is retained while the runner
builds and attaches, then transferred only to the current revalidated
`FLUTTERVIEW`; deactivation, hiding, closing or a real Swing interaction cancels
that intent. A normal foreground-policy refusal receives at most eight
generation-, process- and timer-ticket-fenced retries at 50 ms intervals. The
foreground GUI thread is queried for the current focus HWND; the runner thread's
remembered queue-local focus is not accepted as foreground focus. The Win32 edge
distinguishes policy refusal from target-identity drift and from an unresolved
`AttachThreadInput` detachment, retiring the generation for either unsafe case.
The exact parent/PID/class/style and target thread are revalidated again after
the input queues join and before `SetFocus`, then once more after focus.
Because AWT may retain a stale Java focus owner after a physical native click, a
filtered Swing mouse/key bridge records only explicit user input, clears that
bookkeeping and requests focus only for the clicked or typed-into enabled
component inside the current Design or toolbar roots; newer input elsewhere in
NetBeans fences the old Design claim. Programmatic AWT focus changes never
establish a user claim. Every pointer-down across the complete Flutter surface,
including margins outside the modeled viewport, emits a strict
`runner.interaction` event bound to the exact current session/revision/layout.
Clearing native-focus intent advances a host-owned monotonic interaction fence;
the exact current presentation receives a priority/coalesced
`host.interactionFence`. The runner gates pointer, wheel and keyboard input,
renders a concise synchronization overlay, and publishes the strict exact-layout
`runner.interactionFenceApplied` acknowledgement before it re-enables input.
Only a later runner interaction echoing that acknowledged current fence may
supersede the Swing claim. Older cross-queue events are consumed without focus
authority, the acknowledgement wait is bounded and fail-closed to Swing, and
the fence is replayed for replacement presentations. The IDE shows this barrier
as a separate compact status without replacing the primary render or failure
state. Windows shows the embedded child with
`SW_SHOWNOACTIVATE`, and the runner's root Focus never uses implicit autofocus.
If Windows still transfers native focus to the child FlutterView during attach
or first-frame delivery after an explicit Swing interaction, a typed
confirmed-render state starts an eight-attempt, 50 ms identity-, target-,
owner-, barrier- and lifecycle-fenced reconciliation window for the retained
Swing target. An exact fence
acknowledgement resumes that window through the next normal delayed ticket, so a
following authenticated Canvas interaction can cancel the retained Swing claim
before any focus repair executes.
The assembled Windows gate accepts the physical Canvas → Swing → Canvas → Swing
round-trip, verifies the foreground focus HWND is owned by the NetBeans JVM, and
holds that state beyond the complete bounded retry window.
When the runner already owns Win32 focus, a verified Swing interaction first
revalidates the exact parent/runner/`FLUTTERVIEW` identities and transfers focus
to the JVM-owned AWT parent through a bounded, detach-safe input-queue join.
An already exact retained Swing owner is preserved across that native release;
an absent or different AWT owner is cleared before the epoch-, target-, barrier-
and lifecycle-fenced deferred request. Only a release verified for that exact
repair epoch can be reused by a later bounded target retry. A physical Canvas
press received from the exact heavyweight host or its AWT ancestor can restore
the current `FLUTTERVIEW` only when its screen point is inside that showing host,
the Design is active and rendered, the interaction barrier is synchronized, no
menu path remains, and the deferred input epoch is still current.

The Preview selector is an intentionally local heavyweight-popup exception:
its `JComboBox` disables lightweight popups without changing the global Swing
`PopupFactory` policy. The assembled Windows gate physically opens the actual
popup `Window`/`JList` over the `FLUTTERVIEW`, selects another available platform
profile, waits for exact rendered/idle convergence, and restores Canvas focus.
The runner PID and AWT-parent/runner/`FLUTTERVIEW` HWND remain unchanged and no
Retry state appears.

The same physical gate separately accepts the standard NetBeans
**Window → Services** menu over the embedded child. Its popup remains the
platform's existing lightweight Swing popup in the owning `JFrame`; no global
`PopupFactory` override is introduced. Opening the menu actively releases the
verified runner focus to the JVM, the armed menu item receives interaction over
the overlapping Canvas, Escape closes it without executing the command, and an
exact Canvas click restores the same `FLUTTERVIEW`. The runner PID, all three
AWT-parent/runner/`FLUTTERVIEW` HWNDs, exact surface metrics and rendered/idle
state remain unchanged, with no Retry.

There is no image or pixel-transfer channel. The narrow ADR-025 path is publicly
enabled after live assembled NetBeans 30 acceptance of drop → Save → Undo →
Redo → Save. The transport-independent packaged DataObject gate additionally
accepts the complete retained-history matrix: stage C1, Save C1, Undo B,
Redo C1, Undo B, Save B, then Redo C1 as dirty `STAGED_PAIR` without changing
durable B and Undo B back to a clean savepoint with no `SaveCookie`. This
Properties-driven persistence proof does not claim a second physical Palette
drop path. A real per-monitor DPI transition
between two physical monitors with different scaling remains unaccepted because
the current gate has no second mixed-DPI monitor and does not synthesize
`WM_DPICHANGED`. Physical CJK IME composition and broader menu/popup paths
beyond the accepted local Preview selector and standard **Window → Services**
menu remain unaccepted. The inline editor supplies the real Flutter
`TextInputClient`, but the current physical gate still lacks a
composition-capable input method; deterministic composition tests are not a
substitute for that acceptance.
Linux/macOS providers, every
broader drag-and-drop operation, unreviewed shader/filter graphs and the broader
Designer mutation surface remain outstanding. The deprecated
`Text.textScaleFactor` argument, `key` and raw-Dart
substitutes are not exposed. `PUBLIC_MUTATION_UI_ENABLED`
authorizes only the closed property allowlist above, and
`PUBLIC_PALETTE_CATALOG_INSERT_DND_ENABLED` authorizes only exact definitions
carrying the reviewed DnD capability and catalog-compatible placements.
