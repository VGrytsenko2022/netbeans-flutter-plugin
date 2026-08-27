# Roadmap

## 0.1.2 — Stability & NetBeans Integration

Version 0.1.2 is a release-hardening milestone. It does not add Inspector, embedded DevTools, or Designer features. Automated gates now cover deterministic Run/Debug and DevTools lifecycle contracts, packaged registrations and functional Dart editor behavior in an assembled NetBeans 30 runtime, strict release verification, isolated clean-install/update smoke, persisted SDK settings, Flutter-project reopen lifecycle, and disable/uninstall cleanup. Mobile evidence remains an explicit release requirement below.

- [x] Harden Run/Debug lifecycle handling for start, cancel, confirmed restart, Stop, process exit, project close, and late asynchronous callbacks, with consistent `ActionProgress`, `ProgressHandle`, Output, and debugger cleanup.
- [x] Add controller-level DevTools lifecycle tests for startup cancellation, repeated Open URL reuse, explicit Stop, Flutter-session replacement/termination, project close, process failure, and stale completion rejection.
- [x] Add an assembled NetBeans 30 runtime integration gate for module activation, Dart MIME/editor/LSP/DAP registrations, and Flutter action resolution.
- [x] Add a strict release verifier covering fresh Surefire/Failsafe reports, package metadata and licensing, NBM/update-catalog hashes, installed update tracking, and activation logs.
- [x] Add a Dart editor end-to-end gate in a real NetBeans 30 runtime covering Analysis Server startup, diagnostics, completion/import edits, navigation, rename, formatting, Quick Fixes, restart, and project close.
- [x] Automate isolated NetBeans 30 NBM smoke: clean-install activation of `0.1.2`, activated `0.1.1` baseline plus offline exact-payload verification after update to `0.1.2`, staged local catalogs, headless launchers, and owned-process cleanup.
- [x] Extend the assembled-runtime and isolated NBM smoke gates to deterministic persisted Flutter/Dart SDK settings, Flutter project open/close/reopen lifecycle and reopen records, module disable state, exact tracked-payload/backup cleanup, preserved user settings, fresh-cache confirmation that the removed module is absent, and all-session log validation.
- [x] Remove the known NetBeans 30 warning sources for Dart's custom `$/analyzerStatus` notifications, legacy slash-containing auxiliary attributes, and conflicting or partial MIME layer positions while preserving standard LSP traffic and consolidating private project state into one crash-recoverable, move-safe `dev.flutter.netbeans.projectMetadata` root attribute.
- [ ] Run and record a mobile compatibility matrix for Android physical devices and AVDs plus iOS simulators where macOS is available, covering discovery, target selection, Run, Debug, Hot Reload, Hot Restart, Stop, and DevTools lifecycle.
- [ ] Record the mobile matrix and complete the environment-dependent SDK/device gates before broadly publishing the `0.1.2` binary.

## M1 — usable Flutter workflow

- [x] Multi-module architecture
- [x] Flutter SDK discovery contract
- [x] Flutter CLI process abstraction
- [x] Flutter project detection
- [x] Device discovery and Desktop/Mobile/Web target classification
- [x] `Select Run Target...` action with remembered selection
- [x] Launch an already configured Android or iOS emulator
- [x] Managed `flutter run --machine` sessions
- [x] Run and Debug actions wired to the active project
- [x] Native NetBeans `ProgressHandle` and `ActionProgress` lifecycle for Run and Debug
- [x] Cancel-to-Stop from the Run/Debug progress indicator
- [x] Confirmed Run/Debug restart while a session is starting or running
- [x] Flutter DAP attach for breakpoints, stepping and variables
- [x] Hot Reload / Hot Restart / Stop actions
- [x] NetBeans Output window integration
- [x] Lifecycle-safe Dart Language Server LSP transport
- [x] NetBeans Options page for Flutter and Dart SDKs
- [x] First-start SDK discovery and persistent settings
- [x] Open existing Flutter project as first-class project
- [x] Delete a Flutter project through NetBeans' native keep-sources/delete-sources workflow
- [x] Create a basic Flutter application from the New Project wizard
- [x] Add a second New Project wizard step for an explicit non-empty
  `android`/`ios`/`web`/`windows`/`macos`/`linux` platform selection, preserving
  today's all-platform default. Provide Recommended/Mobile/Desktop/Web/All
  presets and explain which selected platforms require another host OS or
  toolchain to build without disabling cross-host project generation.
- [x] Pass the deterministic New Project selection to
  `flutter create --platforms=...` and provide a separate
  `Add Flutter Platforms...` action for missing platform scaffolding in an
  existing project.
- [x] `New File > Dart > Dart Class` wizard with project-aware location and naming
- [x] Device selector in the standard NetBeans project-configuration toolbar combo,
  filtered and revalidated against the active application's generated platforms
- [x] Automatic coalesced toolbar-device refresh with bounded failure backoff
- [x] Cancellable native progress and lifecycle-safe matching for configured emulator launch
- [x] Native NetBeans Build, Clean, and Clean and Build actions with target-specific Flutter artifacts

## M2 — Dart editor intelligence

- [x] Dart MIME type registration (`text/x-dart`)
- [x] NetBeans DAP debugger registration for Dart sources
- [x] Dart lexer and theme-aware syntax highlighting
- [x] NetBeans LSP provider and project-scoped Analysis Server lifecycle
- [x] Live Analysis diagnostics validation
- [x] Completion
- [x] Go to definition
- [x] Find references
- [x] Rename
- [x] Formatting
- [x] Quick fixes
- [x] Import assistance from completion and missing-import diagnostics
- [x] Organize Imports source action
- [x] Lexer-aware indentation and Dart typing hooks
- [x] Visible Analysis Server start/restart/failure status

## M3 — Flutter tooling

- [x] `pubspec.yaml` completion and semantic diagnostics
- [x] `flutter pub get` through native Output/Stop/Progress
- [x] `flutter analyze` with clickable Dart source locations
- [x] `flutter test` for project/file/test with machine-protocol Test Results and rerun
- [x] Android Device Manager with SDK discovery, AVD creation and lifecycle management
- [x] Project-scoped DevTools launcher connected to the active VM Service
- [ ] Embedded DevTools panel — postponed beyond 0.1.2
- [ ] Flutter Inspector / widget tree — postponed beyond 0.1.2
- [ ] Direct Dart VM Service tooling beyond the implemented DAP handoff — postponed beyond 0.1.2

## 0.1.3 / M4 — Matisse-like Designer foundation

The designer work starts only after the tagged 0.1.2 stability baseline. The
accepted architecture is documented in
[`FLUTTER_DESIGNER_ARCHITECTURE.md`](FLUTTER_DESIGNER_ARCHITECTURE.md).

- [x] Choose `.fd` as the versioned JSON visual-model format.
- [x] Choose mirrored `lib/<relative>/<name>.dart` ↔
  `.fd_templates/<relative>/<name>.fd` pairing with guarded generated regions;
  keep schema-v1 `source.dartFile` as the Dart basename.
- [x] Add `New File > Flutter Designer > Flutter Designer Form`; restrict its
  target to `lib` or a subfolder and create both mirrored files atomically.
- [x] Expose one pair-aware Delete from both mirrored file nodes. Reserve the
  clean coordinator, close an open clean editor, stage both names reversibly,
  and verify rollback; keep unsafe/read-only/unsaved pairs and single-file
  deletion disabled.
- [x] Expose one pair-aware Rename from both mirrored file nodes. Accept only a
  canonical lower-snake-case basename for a complete, clean, writable
  current-version pair; rename both paths and update only `source.dartFile`,
  preserving the Dart bytes and `source.className`. Stage and verify the path
  and metadata transition under deterministic locks with exact-byte in-process
  rollback.
- [x] Expose pair-aware Copy/Paste from both mirrored file nodes. Duplicate the
  exact Dart bytes and a canonical `.fd` with a new `documentId` and retargeted
  `source.dartFile`, only within the existing mirrored relative folder. Allocate
  a jointly free collision suffix across both trees, keep a clean open editor
  alive, publish no one-file loader or OS clipboard flavor, and provide verified
  in-process rollback without a crash journal. Cross-directory Copy remains
  blocked pending relative-URI rebasing semantics.
- [x] Expose pair-aware Cut/Move from both mirrored file nodes as a one-shot
  custom `NodeTransfer.CLIPBOARD_CUT` paste. Move exact bytes, without changing
  the basename, only inside one Flutter project and into an already existing
  mirrored destination. Prove project Dart directives with bounded
  `pubspec.yaml`/`package_config.json`-bound inventory scanning; close the clean
  source editor, run the final proof and commit under the exact NetBeans 30
  MasterFS data locks and folder child-cache admission, create fresh target owners, hold both target locks through
  reversible `.nbmove` source retirement, and retain verified recovery targets
  when exact source recreation cannot be completed. Keep generic DataObject
  Move unavailable and document the in-process/no-crash-journal boundary. Fix
  schema-v1 asset paths at the project/pubspec root and keep opaque extensions
  location-independent so byte-preserving Move has no `.fd`-relative state.
- [x] Define the initial version 1 JSON Schema and conflict-safety invariants.
- [x] Validate NetBeans 30 DataObject/MultiView and guarded-section integration.
- [x] Implement the widget metadata catalog and typed widget-tree model.
- [x] Implement bounded version 1 JSON decoding/encoding, unsupported-version
  read-only handling, and deterministic golden round-trip tests. Package the
  canonical schema in `flutter-designer` runtime resources and verify that its
  checked-in documentation copy remains identical.
- [x] Connect the bounded codec to the NetBeans `DataObject` and `Design`
  MultiView through a clone-safe, reload-coalescing background session. Compose
  built-in and NetBeans Lookup-provided catalogs off the EDT, surface isolated
  contributor diagnostics, and keep every loaded Design model read-only.
  Passing pair and managed-source integrity gates publishes evidence only and
  never enables mutation.
- [x] Add bounded on-disk Dart source-integrity checks with exact source
  baselines/region byte ranges, marker/guard parser parity, normalized hashes,
  stateless class/scope binding, balanced structural delimiters, bounded
  interpolation nesting and accessible conflict/unsupported states. This is
  explicitly read-only; stateful or shadowed Flutter-symbol binding is not
  guessed.
- [x] Implement bounded deterministic Dart-region generation for the supported
  stateless version 1 catalog and a read-only on-disk
  `actual == declared == generated` gate that reconstructs and rescans the
  complete candidate source without writing either file.
- [x] Add the non-authorizing writable prerequisites: a prospective managed
  Dart transition plan, a side-effect-free revision-bound live
  `StyledDocument` snapshot with atomic apply/rollback primitives, and an
  isolated no-disk-write native-analyzer overlay for bounded diagnostics and
  unique real-target navigation evidence.
- [x] Add the immutable canonical `PreparedDesignerPair`, strict live/analyzer
  evidence gate, mandatory pre-apply preparation lease, one
  `PairSaveCoordinator` owning the stable pair-aware `SaveCookie`, two-phase
  guarded persistence, and an ordered two-lock
  Dart/`.fd` transaction with exact baseline rechecks, verified
  rollback/reread and exact owned-event correlation. This production
  persistence edge has no Designer mutation caller yet.
- [x] Complete the writable-command release boundary. Exact probes, chained
  unsaved model/source transitions, one native cross-file Undo/Redo cursor and
  Pair/Source Save re-anchoring are implemented. Pre-persistence recovery now
  revokes only unprovable Designer authority while retaining native Source
  history. The assembled NetBeans 30 runtime, strict NBM verifier and isolated
  install/activation/reopen/disable/uninstall smoke now pass without critical
  errors or plugin-owned ordering warnings. Public mutation remains disabled
  until the visual-surface contracts below are agreed and implemented.
  - [x] Emit and validate the exact 1:1 generator-owned managed-region Flutter
    occurrence manifest, including strict candidate UTF-16 mapping and exact
    analyzer probe-set equality.
  - [x] Implement the bounded pure Add/Remove/Move/Wrap/Set/Reset command
    session, exact inverse history, saved cursor, branch semantics and
    `PAIRED`/`FD_ONLY` revision classification, plus a stable NetBeans combined
    Undo/Redo identity. It remains disconnected from writable UI.
  - [x] Add scanner-owned exact `StatelessWidget` superclass evidence and make
    it a mandatory analyzer probe beside the generator-owned occurrences.
  - [x] Analyze the exact candidate in a unique no-disk overlay before the
    first live mutation, then perform one EDT predecessor compare-and-set and
    bind the applied revision separately. Rejected, cancelled, replayed or
    transferred evidence leaves the live Dart revision unchanged. Analyzer
    authority is fixed to the bound Dart file/configured Flutter SDK, and
    reservation rechecks disk bytes across one external-event epoch.
  - [x] Pin a dirty command cursor with a durable pre-commit lease and support
    an internal exact `FD_ONLY` transaction that verifies both baselines,
    the exact loaded catalog plus complete writable facts, writes only `.fd`,
    leaves the Dart document/Undo state untouched and
    re-anchors the command session only after a verified commit.
  - [x] Make restore/lease release one document-atomic, no-reload barrier and
    keep any observed apply/rollback dirty until ordinary Source Save; serialize
    NetBeans state/cookie callbacks through a lock-free callback drain. Keep
    the native editor Undo manager, expose one combined Source/Design identity,
    and defer internal transaction notifications until the document lock is
    released without erasing a later edit. Isolate fatal presentation callback
    failures and serialize delegate actions against binding transitions.
  - [x] Add the command-side pending C1→C2 lease. It derives and binds the exact
    predecessor session/revision, candidate session/revision, edit and catalog
    identities without moving C1 or truncating its redo branch; it blocks a
    second command, Undo/Redo and durable Save until explicit adopt, abort or
    invalidation. It deliberately grants no analyzer, live-editor or write
    authority.
  - [x] Add coordinator-owned analyzed replacement of the exact staged C1 pair
    by C2. Claim and fence the pending command before transition planning,
    analyze before live apply, jointly publish document/pair/cursor C2, retain
    exact C1 on rejection, and restore/rebind fresh C1 evidence after a failed
    apply across chained unsaved commands. External or user edits fail closed
    without losing their content.
  - [x] Unify the command/generator/analyzer candidate-byte and symbol-probe
    capacity budget so an accepted command cannot become unsaveable only at
    staging. The exact policy identity now travels with generation, revision,
    analyzer ticket/request and analyzer limits.
  - [x] Add one chronological Source/model Undo/Redo cursor. Mutation remains
    separately gated by the visual-surface contracts.
    - [x] Keep the native CES manager as the sole delegate and admit one
      non-merging Designer semantic edit through the Dart MIME
      `UndoableEditWrapper`; prove `Source → Model → Source` ordering, exact
      branch-truncation release, and zero phantom entries after atomic rollback.
    - [x] Join native semantic Undo/Redo to exact retained pair evidence and
      command cursor transitions without rerunning the analyzer.
    - [x] Re-anchor successful paired Save at the exact model/native savepoint,
      retain the native edge, and preserve a newer unmanaged Source entry above
      it without a successful-Save `discardAllEdits()` barrier. The retained
      `B→C1→C2` chain now completes two-step semantic Undo/Redo around the exact
      clean `C2` savepoint without rewriting the durable pair.
    - [x] Re-anchor ordinary Source Save over saved semantic history. Require
      exact managed-payload byte equality under an identity-bound
      `SourceAnchorLease`, make `S2` the durable command baseline, rebuild
      historical proof identities against it, and retain the native `C1`
      underlay so Undo/Redo follows `S2→C1→B→C1→S2`. Adopt controller, command
      and Pair state atomically; any post-CES failed outcome or split-authority risk
      fails closed without discarding native history.
    - [x] Preserve two-axis semantic/Source history across a later Pair Save:
      `(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and full Redo, with exact physical
      endpoint proofs, aggregate byte bounds, `UNCHANGED` savepoint handling,
      repeated Source re-anchoring across every retained physical variant, and
      safe last-edge owner re-anchoring or retirement after owner close.
    - [x] Admit the next Designer command from an exact noncanonical physical
      endpoint such as `(C1,S0)`. An opaque staged command-source token pins the
      logical owner, endpoint-specific `SavedHistoryProof`, live identity and
      both coordinator epochs. The pending lease derives `C3/S0` from that
      exact pair, analyzer/replacement/recovery accept the generalized saved
      proof without fabricating predecessor analysis, and aggregate physical
      budget admission happens before mutation. Successful adoption truncates
      the old `S2/C2` redo suffix, preserves the exact `B/S0→C1/S0→C3/S0`
      branch, and keeps `S0` sticky for the next ordinary command.
    - [x] Replace the remaining recovery-only history discard used when staged
      authority is already unprovable before persistence. Clear the exact
      semantic graph and invalidate its durable command lease, but retain the
      live Source content, native Undo/Redo cursor and stable `SaveCookie` in a
      sticky recovery conflict with zero pair I/O.
  - [x] Verify the packaged Design/Source shared Undo identity and public CES
    dirty→Save→Undo→Redo lifecycle in an assembled NetBeans 30 runtime, then
    pass strict NBM metadata/freshness verification and a fresh isolated
    install/activation/reopen/disable/uninstall smoke with clean logs.
- [x] Discuss and freeze ADR-021: NetBeans-native Palette, Widget Tree and
  Properties around a real embedded native `FlutterView`, never a Swing
  PNG/pixel-transfer surface. Target a Windows native child surface first,
  retain a Windows/Linux/macOS platform SPI, and isolate the runner wherever
  child-surface hosting is feasible. Public mutation remains disabled.
- [x] Establish the pure bounded Canvas lifecycle and version 1 transport
  foundation.
  - [x] Add host-issued session/presentation/frame/layout identities, exact
    validated-revision and resolved render-profile binding, plus pure stale and
    replay admission rules. Exact Android/iOS/desktop adaptive targets now remain
    distinct from the concrete Windows engine identity and reach
    `ThemeData.platform`. Web fails explicitly until a separately compiled
    browser Canvas backend is implemented; no Windows frame is relabeled as Web.
  - [x] Add the per-MultiView lifecycle controller with fresh-session restart,
    one in-flight render plus latest-only coalescing, stale callback fencing,
    atomic frame/layout admission and bounded failure states, verified with a
    fake backend.
  - [x] Add the already-framed strict UTF-8 JSON control codec/session gate for
    `host.hello`, `runner.hello`, `host.close`, `runner.closed` and
    `runner.failure`, including exact version/capability/limit negotiation,
    contiguous runner sequences and startup-close races.
  - [x] Add bounded digest-verified process framing. The complete current
    channel whitelist is control JSON plus post-handshake model and catalog
    JSON. The model channel now carries the canonical bounded `CORE_V1`
    projection; catalog JSON remains reserved for a future versioned contract.
  - [x] Reject physical surfaces above 4096 pixels per dimension or 8,388,608
    total pixels before native surface allocation. This is not a raster-transfer
    budget.
  - [x] Package the versioned Windows Flutter runner, embed its real
    `FLUTTERVIEW` child through a heavyweight AWT host in each Design MultiView,
    and verify the exact parent/PID/style/class hierarchy before display.
  - [x] Harden each per-`.fd` runner lifecycle against close-during-build,
    in-flight launch duplication, peer loss, exit-during-attach, stale `onExit`
    callbacks and re-entrant attach/visibility loss. Deterministic tests cover
    two simultaneous independent Design sessions.
  - [x] Commit and reuse the SDK-keyed Windows runner cache only after an atomic,
    bounded SHA-256 manifest verifies the executable, Flutter DLL, ICU data and
    complete `flutter_assets`; incomplete or modified generated output is
    rebuilt without trusting symlinks or paths outside the cache.
- [ ] Complete the native-surface platform SPI: attach/detach, resize/DPR,
  visibility, focus, liveness/crash notification and final native-handle
  cleanup for Windows, Linux and macOS. The first injectable Windows HWND edge
  and per-MultiView lifecycle now exist; Linux/macOS and the common contract do
  not.
- [ ] Implement the separate Flutter Web Canvas backend: compile the bounded
  runner for Web, host it in an embedded browser surface, bridge the existing
  session/revision/selection protocol without image transfer, and prove browser
  lifecycle, origin, resource and teardown boundaries. Until then Web remains
  explicitly unavailable.
- [ ] Complete Windows child-window acceptance in the assembled NetBeans
  MultiView. The automated standalone Win32 spike already proves the actual
  `AWT Canvas HWND → runner HWND → FLUTTERVIEW HWND` hierarchy, resize and
  isolated-process feasibility, while deterministic host/session tests cover
  resize races, stale exits and two independent views. Focus, DPI, IME,
  menus/popups, peer recreation and real-process multi-view/crash cleanup still
  require assembled-NetBeans acceptance. Do not introduce a PNG, screenshot or
  raw-pixel fallback.
- [x] Define the canonical bounded `CORE_V1` model payload and bundled
  allowlisted runner projection for exactly `Scaffold`, `Column`, `Row`, `Text`,
  `Padding` and `Center`. The runner receives no project paths, Dart source, file
  handles or persistence capability; catalog JSON remains reserved.
- [x] Render one exact validated revision in the embedded Windows `FlutterView`;
  bind native paint/layout epochs to session/presentation/revision identities,
  and prove resize/viewport/DPR and stale-callback rejection.
- [x] Synchronize stable-ID selection between the native Canvas and the
  read-only Explorer/Nodes widget tree, publishing the selected Node through the
  standard lookup without enabling document mutation.
- [x] Publish a context-sensitive standard NetBeans Palette for the exact six
  `CORE_V1` definitions and selected-node standard read-only Properties.
- [ ] Implement editable Properties integration plus the DnD bridge:
  Java drag → Flutter hit test → revision/layout-bound semantic intent → Java
  validation. Keep all mutation admission behind
  `PUBLIC_MUTATION_UI_ENABLED=false`.
- [ ] Pass runner crash/restart/close, native-handle cleanup, pair Save,
  Undo/Redo and assembled-Windows-runtime gates; then implement and verify the
  Linux and macOS SPI providers.
- [ ] Enable the first public writable ten-widget slice only after those gates
  pass.

## M5 — bidirectional RAD (later milestone)

- [ ] Dart AST ↔ designer model mapping
- [ ] Safe source rewriting
- [ ] Custom widget discovery
- [ ] Theme-aware preview
- [ ] Multi-device preview
