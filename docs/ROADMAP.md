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
  errors or plugin-owned ordering warnings. This checkpoint supplied the
  command/pair-save release boundary later consumed by the bounded typed
  Properties slice; it did not authorize Palette insertion or DnD.
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
  child-surface hosting is feasible. Mutation is admitted only through the
  explicit bounded host-side slices listed below.
- [x] Establish the pure bounded Canvas lifecycle and version 1 transport
  foundation.
  - [x] Add host-issued session/presentation/frame/layout identities, exact
    validated-revision and resolved render-profile binding, plus pure stale and
    replay admission rules. Exact Android/iOS/desktop adaptive targets now remain
    distinct from the concrete Windows engine identity and reach
    `ThemeData.platform`. Web renders its browser-sized responsive viewport on
    the native engine as a bounded layout preview without claiming `kIsWeb` or
    browser-runtime fidelity.
  - [x] Add the per-MultiView lifecycle controller with fresh-session restart,
    one in-flight render plus latest-only coalescing, stale callback fencing,
    atomic frame/layout admission and bounded failure states, verified with a
    fake backend.
  - [x] Add the already-framed strict UTF-8 JSON control codec/session gate for
    `host.hello`, `runner.hello`, `host.close`, `runner.closed` and
    `runner.failure`, including exact version/capability/limit negotiation,
    contiguous runner sequences and startup-close races.
  - [x] Add bounded digest-verified process framing. The current post-handshake
    whitelist is control JSON, canonical bounded model JSON, negotiated NBFC
    kind 4 `IMAGE_BYTES` and reserved catalog JSON. Under
    `asset.imageBytes.v1`, one revision-scoped model
    descriptor is followed by the exact ordered SHA/size-checked compressed
    image-resource frames. This transports referenced asset resources, never a
    screenshot or rendered-Canvas pixel surface.
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
- [x] Export the provider-neutral native-surface SPI for attach/detach,
  resize/DPR, visibility, focus, peer lifecycle, native-surface liveness and
  final native-handle cleanup; select the concrete Windows provider through that
  contract, keep unsupported providers fail-closed, and retain runner-process
  crash authority in the owning session through `Process.onExit()`.
- [x] Establish the provider-owned runner/build/runtime/cache/launch contract
  foundation. The immutable contract binds one concrete platform to its Flutter
  build target and options, output/executable boundary, closed required/allowed
  runtime layout, complete deterministic cache fingerprint and exact launch.
  Shared build, cache and session orchestration consume only that contract and
  contain no Windows provisioning literals. Selection admits one complete,
  matching supported provider and fails closed on ambiguity or an incomplete
  contract. The Windows provider owns the current exact behavior and artifact
  layout, including the existing cache-manifest format.
- [ ] Implement and physically accept real Linux and macOS native-surface
  providers, each with its own complete build/runtime/cache/launch contract and
  assembled-runtime lifecycle gate. Their current providers remain explicit
  unavailable placeholders without runner contracts; they are not emulation
  and do not claim support.
- [x] Render the Web responsive viewport through the existing native Flutter
  Canvas as an explicitly bounded layout preview. It does not emulate `kIsWeb`
  or browser-only behavior.
- [ ] Complete the optional runtime-faithful Flutter Web Canvas backend. The
  selected Windows architecture is the bounded Flutter Web release bundle in a
  windowed Microsoft Edge WebView2 child controller owned through a narrow
  native Win32 adapter, with direct DOM multi-view embedding, an isolated HTTPS
  virtual host and JSON web messages carrying the existing framed protocol.
  Its shared runtime now consumes the same negotiated revision-scoped
  `IMAGE_BYTES` resources as native Canvas for exact `DecorationImage` parity;
  it still has no screenshot/framebuffer transfer path.
  - [x] Compile the same bounded Canvas runtime for Web through `main_web.dart`,
    separate process-only I/O from the shared runtime, and attach one Flutter
    widget root per browser-managed view through Flutter multi-view.
  - [x] Add the authenticated JavaScript transport foundation. The page bridge
    requires WebView2 plus one exact host-issued 256-bit session nonce, fixes the
    bridge format/version/direction, bounds canonical base64 chunks, diagnostics
    and pre-registration buffering, and enforces contiguous host and runner
    sequences while preserving the existing NBFC frame bytes. Exact-session
    malformed/oversized/wrong-envelope input is terminal; foreign nonces are
    ignored. Focused Dart tests and a manually executable production-JavaScript
    headless-browser harness cover nonce drift, replay/gaps/out-of-order delivery,
    malformed or oversize chunks, ordered output, bounded pending input and
    idempotent close.
  - [x] Separate Web host capabilities from process-I/O ownership: Web observes
    Flutter binding metrics and invalidates stale geometry after resize, while
    native Windows OLE Palette DnD remains uninstalled and unadvertised.
  - [x] Prove the pinned Flutter release bundle builds from the offline dependency
    cache with the Web entry point, locally packaged CanvasKit and registered
    local Roboto plus its Apache-2.0 license for the current English-only scope.
    Two clean builds are byte-for-byte deterministic. This proves compiler
    closure and the static bundle boundary only; it is not a browser-runtime,
    origin/CSP, lifecycle or product acceptance gate.
  - [x] Implement the internal native WebView2 host foundation: actionable
    Runtime detection, a manifest-pinned and integrity-checked x64 loader/native
    adapter with the Microsoft license and notice, native COM STA lifecycle and
    message pump, a windowed child-HWND controller, read-back-verified bounds/
    visibility and focus-command routing,
    exclusive owned user-data admission, leased private artifact publication,
    ABI-v3 native rehash plus immutable in-memory resource snapshots, one
    nonce/generation-derived `.invalid` HTTPS origin without a disk fallback,
    frozen CSP/navigation/resource policy, authenticated bootstrap and Java NBFC
    bridge, process-failure reporting and bounded teardown.
  - [x] Pass the standalone physical Windows x64 host smoke gate against an
    admitted WebView2 Runtime `100.0.1185.39+`: exact release-page and
    authenticated bridge readiness, an authenticated NBFC `host.hello` →
    digest-verified `runner.hello` round trip, read-back-verified bounds and
    visibility, the focus API, private parking-parent handoff, parent-HWND
    release before peer destruction, matching-PID browser resource release,
    one-millisecond bounded destroy with exact-handle retry when it expires,
    close-during-start and deadline-bounded native
    teardown all complete.
    This is an internal host gate, not the assembled NetBeans product route or
    its model/layout/selection, DPI, Retry/crash and cleanup acceptance matrix.
  - [ ] Route the WebView2 backend through the provider/product selection,
    build/cache/session lifecycle and exact Web `CanvasEngineIdentity`. Until
    this passes, the existing native-engine Web responsive preview remains the
    only product-routed Web choice and still does not claim `kIsWeb`.
    - [x] Decouple the assembled MultiView from `NativeCanvasHost` behind one
      backend-neutral Canvas-session contract and a deterministic backend
      selector. Production keeps every target on the existing native route;
      the test-only exact-Web decision fails closed until an admitted Web
      session exists and never silently falls back to native rendering.
    - [x] Give the assembled MultiView one backend-neutral owner/factory envelope
      for the session, component and exact focus surface. Failed construction
      closes whichever side already transferred ownership, and the product still
      creates only the admitted native owner.
    - [x] Add the default-off exact-Web build/cache prerequisite. It reads one
      strict bounded Flutter SDK identity, validates the framework revision,
      cross-checks engine evidence and optional SDK-root Flutter-version
      evidence, includes all four identity fields in the cache contract, passes
      them to the Web compiler, validates every cache hit, and publishes a fresh
      private immutable generation. Timeout cleanup retires the captured process
      tree; owner and final-lease deletion remain retryable.
    - [x] Provide an asynchronous pre-peer-loss barrier that fences future AWT
      attachment and reparents the controller to a private parking HWND before
      the heavyweight parent may be removed. The assembled owner coordinator now
      awaits this contract before component replacement; every other physical
      peer-removal path must do the same before the route can be enabled.
    - [x] Treat an unconfirmed or failed native destroy as terminal/poisoned,
      retain the exact live handle for bounded retry and reject restart until
      native ownership is proven released.
    - [x] Synchronize UDF deletion with the exact WebView2 environment identity
      and matching-PID `BrowserProcessExited` result; missing proof
      releases native ownership as `S_FALSE` but preserves the UDF.
    - [x] Make partial UDF cleanup retry-safe with a per-session ownership marker
      outside the incrementally deleted UDF tree, stable Win32 FileId handles
      that block concurrent root/marker replacement, and handle-based deletion
      of that evidence last.
    - [x] Implement the internal admitted exact-Web session and backend-neutral
      factory/owner-transition foundation. The Web session uses the authenticated
      byte-stream transport, compares the compiled `CanvasEngineIdentity` with
      `runner.hello`, applies the same model/layout/selection/viewport/interaction
      fences, exposes explicit visibility and focus parity, and deliberately
      leaves native OLE Palette insertion unavailable. The transition coordinator
      coalesces rapid route changes, retires the prior peer asynchronously before
      creating its replacement, fences callbacks by epoch and retains poisoned
      cleanup for explicit retry. The Web runner binds metrics to its exact
      `FlutterView` and rejects a second distinct view instead of reading an
      ambient `implicitView`.
    - [x] Connect that coordinator to the assembled MultiView component path.
      Owner activation installs the current component, retirement keeps the old
      heavyweight component attached until its asynchronous pre-peer-loss barrier
      succeeds, and only then removes it and creates the replacement. Presentation,
      focus, visibility and interaction callbacks are admitted only for the exact
      active owner epoch; a poisoned transition retains its owner and exposes
      explicit Retry.
    - [x] Implement the asynchronous close-handler foundation. A Canvas close
      state vetoes the current close stack, starts coordinator retirement and
      schedules a fresh TopComponent close only after peer-safe completion while
      preserving ordinary Save/Discard/Cancel handling. The handler is deliberately
      not installed in production by default while exact-Web selection remains
      off.
    - [x] Audit the actual NetBeans 30 split and clone-close implementation.
      `TabsComponent` reparents with synchronous `removeAll()`, non-last clone
      close short-circuits before `closeLast()`, and no exported asynchronous
      pre-removal hook can cover both paths from a plugin. Add a dormant
      plugin-owned `CloneableEditor` shell foundation that reuses the one CES
      Source pane, owns an explicit Design/Source lifecycle, forwards the
      Design close retry without fabricating a `MultiViewElementCallback`, is
      cloneable, and deliberately does not implement internal `Splitable`.
      The foundation is product-disabled and `PERSISTENCE_NEVER` until the
      gates below prove behavior rather than trading peer safety for editor
      regressions.
    - [x] Add the dormant shell-owned asynchronous close permit. One
      support-level reservation serializes clone construction and every close,
      binds the exact clone identities plus live-document version and one
      atomic pair coordinator/state/external-event/Source-state revision,
      resolves the dirty last-clone Save/Discard/Cancel decision before
      Canvas retirement, and carries one attempt token through retirement,
      retry and `closeLast(false)`. Stale retries and failed retirement revoke
      authority; successful authority remains reserved through
      `componentClosed()`. Raw synchronous CES batch admission remains
      fail-closed except inside the exact permit scope; the public support-wide
      path is superseded by the asynchronous batch below.
    - [x] Recover Canvas after an abandoned or stale shell close without reviving
      retired authority. Wait for the captured owner coordinator to finish,
      rebuild through a fresh generation, fence factory/owner/observer/close
      callbacks by that generation, and retain the latest requested backend.
      Permanently set `TopComponent.PROP_CLOSING_DISABLED` and route the shell's
      own Close action through the permit so RELEASE300 `Close Mode` cannot
      bypass asynchronous retirement.
    - [x] Complete the dedicated-shell `CloseCookie`/shell-owned Close All
      vertical slice.
      One support-wide batch asks Save/Discard/Cancel exactly once, then binds
      the exact clone topology and post-decision live-document/pair revision.
      Clone creation and ordinary close admission remain excluded while the
      batch retires each clone-local Canvas sequentially through an exact
      per-owner shell-incarnation permit. The irreversible post-unregister state
      and final internal close are fenced by exact pre/post topology and
      document/pair revisions plus explicit success acknowledgement. Sibling
      lifecycle drift may finish only physical cleanup; same-owner close/reopen
      ABA revokes old authority without removing the new shell. Other topology
      or revision drift and retirement failure abort fail-closed; the synchronous
      `CloseCookie` entry point reports no early
      success while asynchronous retirement is still running. The permanent
      `PROP_CLOSING_DISABLED` latch still makes NetBeans' stock
      global Close All skip this shell, so the shell exposes its own permit-aware
      Close All action. This does not change either production flag: the
      dedicated shell and exact-Web routing both remain disabled.
    - [x] Complete the operation-owned post-close continuation for pair-aware
      Rename, Delete and Cut-Move. Each command reserves its exact pair lease
      before requesting shell retirement. After the final admitted
      `componentClosed()`, the support-close reservation stays active while one
      identity-bound proof dispatches one callback off the EDT. The callback
      claims that proof and lease and replays the existing synchronous operation
      exactly once; Rename/Delete keep the normal `DataObject` events and
      binding updates. Cut-Move owns an exact `CutSession`, rejects duplicate
      paste, clears only the same current clipboard value after commit and
      remains retryable after cancel/failure. Cancel, topology/revision drift,
      stale/foreign/reused proof, close failure and operation failure are
      mutation-free and release both reservations. A direct synchronous caller
      refuses an open dedicated shell rather than reporting early success.
    - [ ] Prove the dedicated shell's Source/Save/Undo/navigation, clone-local
      Canvas ownership, platform History/action parity and restart
      reconstruction in the NetBeans runtime suite before changing its product
      gate or persistence policy. Physically accept programmatic mode movement.
      The dormant shell disables tab dragging, undocking, sliding, maximization
      and drag-copy because those stock UI paths can reparent the AWT subtree
      without consulting `canClose()`.
    - [ ] Add a proven peer-removal gate for every NetBeans path outside the
      permit-aware shell Close action. RELEASE300 `Close Mode` is protected by
      the permanent closing-disabled latch, but `New Tab Group`, `Collapse Tab
      Group`, public `Mode.dockInto()` and direct post-removal callbacks can still
      reparent or remove the heavyweight hierarchy without a universal exported
      asynchronous veto. Production exact-Web selection stays off until those
      paths, exact-Web product binding, restart/runtime acceptance and the
      physical gate below are closed.
  - [ ] Pass the assembled Windows NetBeans physical gate for load/readiness,
    model/layout/selection round trips, resize/DPI/focus, hide/resume, close,
    Retry/crash cleanup, origin/navigation/resource isolation and rejection of
    stale or unauthenticated web messages.
    Current input acceptance remains English-only; physical CJK IME and other
    language-specific input are deferred to the final internationalization
    phase and are not implied by the standalone host smoke.
- [ ] Complete Windows child-window acceptance in the assembled NetBeans
  MultiView. Do not introduce a PNG, screenshot or raw-pixel fallback.
  - [x] Exercise production Designer DataObjects and assembled MultiViews with
    distinct runner/child HWND and PID ownership, exact current-`FLUTTERVIEW`
    DPR, one tab peer-loss recreation, forced crash → explicit Retry with a
    fresh PID, authenticated `host.close` → natural process exit, and final
    runner/child-HWND cleanup with every captured runner generation physically
    exited.
  - [x] Route Design activation to the exact verified `FLUTTERVIEW`, retain that
    activation across asynchronous build/attach and fresh Retry generations,
    cancel it on deactivation, hidden views or a real Swing interaction, and
    bound normal foreground-policy retries by runner generation, process and
    timer ticket. Fail closed on native identity drift or unresolved input-queue
    detachment. Admit only explicit Swing mouse/key input, show the child without
    activation, keep runner root focus non-autofocus, and reconcile a late native
    child-focus transfer during attach or first-frame delivery through an
    eight-attempt typed-rendered window for the retained exact Swing target.
    Fence claims through strict authenticated pointer-down events across the
    complete native surface plus a host-owned monotonic interaction epoch. Send
    that epoch through a priority/coalesced exact-layout command, gate runner
    input behind a visible synchronization state, and require a strict runner
    application acknowledgement before current-epoch input can supersede Swing.
    Bound missing acknowledgements and fail closed to the retained Swing target;
    consume older cross-queue events without focus authority, revalidate exact
    HWND identity again after joining input queues and before `SetFocus`, and
    accept the physical Canvas click → Swing → Canvas click → Swing focus
    round-trip by foreground HWND/PID, stable beyond the complete retry window.
    Programmatic MultiView activation alone is not physical foreground authority
    and must never steal focus from an unrelated process.
  - [x] Accept live resize through the assembled physical production-divider
    runtime gate: keep embedded bounds parent-owned, coalesce host resize bursts
    to the latest target without replacing the runner generation, require
    negotiated `surface.presentation.v1` exact post-frame width/height/DPR, and
    keep input and viewport publication fenced until the exact final frame.
    Deterministic host/session/runner tests additionally cover burst
    convergence, B → A replacement-layout convergence and stale/intermediate
    rejection; the physical gate verifies exact
    parent/runner/`FLUTTERVIEW` bounds and restored input synchronization after
    the divider settles.
  - [ ] Accept a real per-monitor DPI transition by moving the embedded child
    between two physical monitors with different scaling. The deterministic
    parent-DPI and `WM_DPICHANGED`/`WM_DPICHANGED_AFTERPARENT` contracts are not
    physical acceptance; the current gate has no second mixed-DPI monitor and
    deliberately does not synthesize the transition.
  - [x] Accept two simultaneous production Split Document Design surfaces with
    independent runner PID, AWT-parent/runner/`FLUTTERVIEW` HWND, exact metrics,
    rendering and focus. Clear Split must physically retire the removed runner,
    `FLUTTERVIEW` and AWT-parent HWND while the survivor stays healthy; a second
    split/clear cycle proves bounded heavyweight-peer churn without process or
    native-handle reuse. Together with the tab peer-loss cycle above, this is the
    accepted bounded heavyweight-peer recreation matrix.
  - [x] Accept the local Preview selector popup above the embedded child. Keep
    only that `JComboBox` popup heavyweight, physically prove its popup window
    overlaps the `FLUTTERVIEW`, select another available platform profile, wait
    for exact rendered/idle convergence, then restore Canvas focus without
    runner PID or AWT-parent/runner/`FLUTTERVIEW` HWND replacement or Retry.
  - [x] Accept the standard NetBeans **Window → Services** menu over the
    embedded child without changing the global popup policy. Physically prove
    overlap and armed-item interaction, transfer focus from the exact runner to
    the JVM while the menu is active, close it without executing the command,
    and restore exact `FLUTTERVIEW` focus with unchanged runner PID, three HWNDs,
    exact metrics, rendered/idle state and no Retry.
  - [ ] Physically accept CJK IME composition and broader native menu/popup
    paths beyond the accepted local Preview selector and standard
    **Window → Services** menu in the assembled Windows runtime. The product
    now supplies the selected-Text Flutter `TextInputClient` described below,
    but the current physical gate host has no composition-capable input method.
    Deterministic composition tests do not close this physical acceptance item.
    The current acceptance scope is English input; CJK and other languages are
    intentionally deferred until the final internationalization phase.
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
- [x] Implement the capability-gated Windows inline `Text.data` vertical slice.
  Double-click or F2 on the selected existing `flutter.widgets.Text` opens a
  real Flutter `TextField`/`TextInputClient`; ordinary Enter remains newline,
  while Ctrl+Enter commits and Escape cancels only with an empty composing
  range. Keep preedit runner-local, disable AWT input methods on the carrier and
  never relay `WM_IME`. Admit one bounded `runner.textEditCommit` only for the
  exact current session/revision/layout/fence/selection and map it to at most
  one existing `SetProperty(data)` command; unchanged text is a no-op, while
  changed text uses the established Save/Undo/Redo pipeline. Deterministic
  Flutter and Java tests are complete; physical CJK IME acceptance remains
  open, and Linux/macOS/Web implementations are not claimed.
- [x] Publish a context-sensitive standard NetBeans Palette for the exact six
  `CORE_V1` definitions and selected-node standard Properties baseline.
- [x] Enable catalog-driven typed read/write Properties for the 76 properties
  reviewed at that historical `CORE_V1` stage for `Column`, `Row`, `Padding`,
  `Center` and `Text`. The 59 Text leaves are grouped into Text, Accessibility, Locale and
  scaling, Text style, Paint and effects, Advanced typography and Strut style,
  with type-appropriate editors and exact
  Dart/native-Canvas assembly into `TextStyle`, `StrutStyle`, `Locale`,
  `TextScaler` and `TextHeightBehavior`. Emit revision-bound one-shot
  `SetProperty`/`ResetProperty` commands, use native Restore Default for
  optional values, and route every admitted edit through the existing
  pair-save/Undo lifecycle. At that stage `Scaffold` remained read-only for its
  separate property-design task, which is completed below. Schema v2 supplies closed theme-token, `Paint`, Shadow,
  font-feature and font-variation value graphs with transactional custom
  editors and Canvas/generator parity. The deprecated `Text.textScaleFactor`
  argument, `key` and arbitrary Dart/shader/filter graphs remain excluded.
- [x] Implement and narrowly enable the first safe DnD vertical slice. This is
  a historical milestone, whose public surface was exactly
  `Text` from the Palette → terminal append to `Row.children` or
  `Column.children`. Its Text-only source/slot restriction is superseded by the
  completed six-`CORE_V1` matrix milestone below. Native OLE `MOVE` is only the
  immutable-Palette transport contract; the Designer intent is `ADD`.
  - [x] Freeze ADR-025: Windows native OLE bridge, Flutter-authoritative hit
    test, one bounded process-local one-shot opaque token, and exact
    session/presentation/document/logical-revision/frame/layout/intent fencing.
  - [x] Complete Palette → OLE token publication and child-HWND coordinate/drop
    delivery without exposing widget JSON, Dart source, paths or file authority.
    Preserve the exact active-view token across hover/prepare/commit/cancel;
    revoke cancel/failure/non-`MOVE` immediately and bound successful `MOVE`
    grace to three seconds.
  - [x] Complete native Flutter terminal-zone hit testing, same-channel FIFO
    fast release and the 250 ms prepare → commit/cancel protocol. Prepare emits
    no Java event; timeout/`NONE` cannot mutate later. Commit may publish only
    capability-gated `runner.paletteDrop` with `operation = ADD`,
    `slotName = children` and the exact terminal insertion index.
  - [x] Atomically consume the token on Java admission; reject stale, foreign,
    duplicate, expired or structurally invalid responses; revalidate the current
    `Row`/`Column` parent and `insertionIndex == children.size()`. Treat OLE
    `MOVE` as commit dispatch, not proof of Java admission.
  - [x] Route the single admitted `AddWidget(Text)` through deterministic
    generation, analysis, paired `.fd`/Dart replacement, Save and one native
    chronological Undo/Redo edit.
  - [x] Pass lifecycle, runner restart/close, stale-layout/revision,
    duplicate-drop, rollback, pair-save/Undo and assembled Windows NetBeans 30
    acceptance, including the live drop → Save → Undo → Redo → Save sequence,
    before enabling this drag publicly. This does not claim the separately
    discovered saved-history Undo → Save cycle.
- [x] Supersede the historical Text-only vertical slice with catalog-driven
  insertion for all six exact `CORE_V1` Palette sources. Accept the complete
  36-cell matrix: empty `Scaffold.body`, `Scaffold.floatingActionButton`,
  `Padding.child` and `Center.child`, plus terminal `Row.children` and
  `Column.children`. Keep occupied singles, non-terminal list indices,
  `Scaffold.appBar`, non-`CORE_V1` sources, existing-widget move/reorder and
  Linux/macOS/Web DnD disabled.
- [x] Supersede that six-widget milestone with the first complete expansion
  slice: `SizedBox` now has exact Create/Canvas/DnD/Properties capabilities,
  nullable non-negative width/height editors, its named `child` slot,
  deterministic generation, native rendering and Palette/tree/Canvas DnD.
  At that milestone the Palette/runner contract was seven widgets and the
  compatibility matrix was 49 cells, including `SizedBox.child`. Java and Dart independently
  fingerprint the exact property/slot schema and fail closed on an altered or
  merely same-id definition. Same-tree existing-widget move/reorder remains
  enabled through its separately reviewed compatibility planner.
- [x] Supersede the SizedBox milestone with the complete `Icon` vertical slice.
  Schema v4 adds typed nullable `IconData` without arbitrary Dart expressions;
  Canvas payload protocol v6 and deterministic generation construct the same
  real Flutter `Icon`, including inheritance for its theme-backed fields while
  keeping `blendMode` and `fontWeight` local. `Icon` exposes its 13
  typed constructor properties, a built-in chooser for **None** or one of 8,825
  bundled Material Icons locked to Flutter 3.44.8, and requires
  `flutter.uses-material-design: true` for Material glyphs. It is a leaf with no
  slots. The active surface is eight Create/Canvas/DnD sources, 91 writable
  property rows across seven non-`Scaffold` widgets, and exactly 56 insertion
  cells across the unchanged seven valid destination slots.
- [x] Supersede the Icon milestone with the complete `AppBar` vertical slice.
  Canvas payload protocol v7, deterministic Dart generation and the native
  runner assemble the same 120 typed AppBar leaves without arbitrary Dart
  expressions. Properties exposes nine enterprise groups and five exact slots:
  `leading`, `title`, `actions`, `flexibleSpace` and `bottom`. `AppBar` carries
  the canonical `PreferredSizeWidget` trait, so only AppBar is accepted by
  `Scaffold.appBar` and `AppBar.bottom`; all nine sources remain valid in the
  eleven any-widget destinations. At that milestone the active surface was nine
  Create/Canvas/DnD sources, 211 writable rows across eight non-`Scaffold`
  widgets and 117 compatibility candidates: exactly 101 accepted and 16
  rejected. Source-aware Palette
  authorization binds an opaque token to the exact current type and traits for
  Canvas hover, while Java repeats the canonical planner before mutation.
- [x] Supersede the AppBar milestone with the complete `ElevatedButton`
  vertical slice. The slice originally landed on Canvas payload protocol v8
  (the current aggregate model is v18); deterministic generation and the
  native runner share 286 typed leaves: seven direct behavior/callback fields,
  five 54-leaf default/disabled/pressed/hovered/focused style groups and nine
  common layout/feedback fields. Callback values are strict Dart identifiers;
  the Canvas receives only `callbackPresence` and cannot execute project code.
  Direct sparse `ButtonStyle` maps preserve the local → `ElevatedButtonTheme`
  → framework fallback. The closed contract includes every
  `SystemMouseCursor`, six shape presets including `roundedSuperellipse`, four
  splash presets and deliberately omits ineffective `TextStyle.color` plus
  runtime-only keys/controllers/builders. Its optional-single,
  required-named-nullable `child` slot emits `child: null` when empty. The
  that milestone surface was ten Create/Canvas/DnD sources, 497 writable rows across
  nine non-`Scaffold` widgets and 140 compatibility candidates across twelve
  any-widget plus two trait-bound destinations: exactly 122 accepted and 18
  rejected. The `.fd` schema remains v4.
- [x] Complete the closed writable `Scaffold` scalar slice without expanding the
  Palette or slot matrix. Seventeen independently resettable fields cover
  Layout, Floating action button, Appearance, Drawer behavior and State
  restoration with reviewed public static presets, literal/semantic colors,
  optional booleans, a non-negative numeric value, strict callback identifiers
  and a bounded restoration ID. The existing `appBar`, `body` and
  `floatingActionButton` slots remain unchanged. Widget-valued
  `persistentFooterButtons`, `drawer`, `endDrawer`, `bottomNavigationBar` and
  `bottomSheet`, plus `persistentFooterDecoration`, `bottomSheetScrimBuilder`
  and `key`, remain deliberately excluded. That milestone total was 514 writable
  rows across all ten built-ins; Canvas, deterministic Dart, Pair Save,
  reopen and one-step Undo/Redo use the same closed mapping.
- [x] Extend exact named-slot management with true atomic compound commands.
  An occupied single slot now offers explicit Replace with a fresh reviewed
  prototype or an existing non-root subtree, plus Clear; a non-empty list slot
  with `minChildren == 0` offers Clear All. Exact revision and current-child
  fences, compatibility, cycle/root, cardinality and minimum-child checks are
  repeated before one `ReplaceSlotChild` or `ClearSlotChildren` admission. No
  implicit replacement and no sequence of partial remove commands is allowed;
  each accepted mutation has one Pair Save and one chronological Undo/Redo
  step. This does not change the Palette compatibility matrix.
- [x] Add `AspectRatio` as the first resumed complete Palette vertical slice.
  Its required finite positive `aspectRatio` double starts at `1.0`, its
  optional `child` slot accepts any reviewed source, and its generated Dart,
  native Canvas, Properties, Slots, Palette/tree DnD, persistence and Undo/Redo
  paths share one exact catalog contract. The active surface is eleven
  Create/Canvas/DnD sources, 515 writable rows across all widgets and 165
  compatibility candidates across thirteen any-widget plus two trait-bound
  destinations: exactly 145 accepted and 20 rejected. `.fd` remains v4 and the
  Canvas payload remains v9.
- [x] Add `Container` as one complete structured Palette vertical slice. Its
  optional single any-widget `child` slot accompanies exactly 13 reviewed
  constructor properties in Flutter order: `alignment`, `padding`, `color`,
  `isAntiAlias`, `decoration`, `foregroundDecoration`, `width`, `height`,
  `constraints`, `margin`, `transform`, `transformAlignment` and
  `clipBehavior`. AlignmentGeometry, BoxConstraints, column-major Matrix4 and
  the schema-v5 BoxDecoration branch was a closed typed value with transactional
  editors,
  exact layout/paint invariants, semantic `ColorScheme` integration and atomic
  dependent-property patches. Generated Dart and the real native Canvas share
  the same contract; Canvas keeps selection outside the paint transform, draws
  padding/margin guides and retains an IDE-only zero-size target. The active
  surface is twelve Create/Canvas/DnD sources, 528 writable rows and 192
  compatibility candidates across fourteen any-widget plus two trait-bound
  destinations: exactly 170 accepted and 22 rejected. This slice raises `.fd`
  to v5, Catalog API to 4 and Canvas payload to v10. ADR-039 completes the
  deliberately deferred image branch in the following checked item.
- [x] Complete shared typed image assets and `Container.DecorationImage`
  end-to-end. `.fd` schema v6 adds asset-only `ImageProviderValue` plus the full
  13-argument `DecorationImageValue`; Catalog API 5, deterministic Dart,
  accessible typed Properties, migration, chronological Undo/Redo and theme
  detection use the same fail-closed contract. The project inventory resolves
  app/package `pubspec.yaml` declarations through `package_config.json`, accepts
  verified PNG/JPEG/GIF/WebP variants, applies the exact Flutter 3.44.8 DPR
  selection algorithm and publishes referenced immutable bytes only. Canvas
  model v11 over NBFC framing v1 negotiates `asset.imageBytes.v1`, orders
  `CONTROL` → `MODEL` → `IMAGE`, and
  validates exact descriptors, payload SHA-256, size and order. Native and
  internal exact Web construct the same real `DecorationImage`; authenticated
  corrupt resources are quarantined independently, while wire/identity/coverage
  violations remain fatal. Unavailable resources use deterministic accessible
  placeholders without removing
  selection/layout/drop overlays. File, memory, network and custom model
  providers remain deferred; there is no filesystem-path, URL or raw-Dart
  escape hatch.
- [x] Complete `Opacity` as the next bounded vertical slice across the catalog,
  model validation/codecs, migration fixtures, Properties, Create,
  Palette/tree/native-Canvas DnD, deterministic Dart generation, Save/reopen and
  chronological Undo/Redo. The exact Flutter 3.44.8 const contract is required
  finite named `opacity` in inclusive `[0, 1]`, optional named
  `alwaysIncludeSemantics` with omitted default `false`, and one optional single
  any-widget `child`; `key` and raw Dart remain excluded. New prototypes store
  only `opacity: 1.0` plus an empty child. Native and exact-Web projections build
  real Flutter `Opacity`; zero opacity preserves hit testing, normally suppresses
  child semantics and retains them only when `alwaysIncludeSemantics` is true.
  IDE-owned selection/hit/drop overlays remain outside the effect. Existing
  double/boolean/single-slot encodings keep `.fd` schema v6, Catalog API 5,
  Canvas model v11 and version-1 framing/control unchanged. At that milestone
  the active surface was 13 widgets and 530 writable rows. Thirteen sources across 15 any-widget plus
  two trait-bound destinations form 221 candidates: 197 accepted and 24 rejected.
- [x] Complete `Align` as the next bounded Layout vertical slice. The exact
  Flutter 3.44.8 const contract contains optional `AlignmentGeometry alignment`
  with omitted `Alignment.center` default, nullable finite non-negative
  `widthFactor` and `heightFactor`, and one optional single any-widget `child`;
  `key` and raw Dart remain excluded. New prototypes store no property values
  and keep an empty child. Native and exact-Web projections build real Flutter
  `Align`, preserve physical and directional alignment under LTR/RTL, and keep
  selection/drop feedback outside the widget, including an IDE-only target when
  an empty factor-driven Align has zero size. Existing alignment, numeric and
  single-slot encodings keep `.fd` schema v6, Catalog API 5, Canvas model v11 and
  version-1 framing/control unchanged. At that milestone the active surface was
  14 widgets and 533
  writable rows. Fourteen sources across 16 any-widget plus two trait-bound
  destinations form 252 candidates: 226 accepted and 26 rejected.
- [x] Complete `FractionallySizedBox` as a const Layout slice with optional
  physical/directional alignment, non-negative width/height factors, an optional
  child and real bounded/unbounded plus LTR/RTL Canvas behavior. This milestone
  reached 15 widgets, 536 rows and 285 DnD candidates: 257 accepted and 28
  rejected.
- [x] Complete `Stack` with alignment, text direction, fit, exact Flutter clip
  behavior and ordered terminal-append non-positioned children. `Positioned` is
  not implied. This milestone reached 16 widgets, 540 rows and 320 DnD
  candidates: 290 accepted and 30 rejected.
- [x] Complete `Expanded` as a required-child wrapper with optional non-negative
  flex. Palette creation wraps an existing direct Row/Column child atomically,
  never creates a terminal placeholder, and exposes replacement-only child
  editing. This milestone reached 17 widgets, 541 rows and 340 DnD candidates:
  292 accepted and 48 rejected.
- [x] Complete the const `Image` leaf with one required asset-only provider and
  21 optional reviewed fields. Creation resolves a deterministic declared asset
  when available and otherwise persists an editable unresolved provider backed
  by a safe built-in Canvas/generated-Dart placeholder; centerSlice uses an
  all-or-none strict rectangle.
  This milestone reached 18 widgets, 15 const definitions, 563 rows (546 outside
  Scaffold) and 360 DnD candidates: 310 accepted and 50 rejected.
- [x] Complete the const Material `TextField` leaf with 54 optional grouped rows,
  closed presets, strict callback identifiers, atomic radius/padding compounds
  and no stored runtime text/controller/focus state. Generated Dart and Canvas
  use the robust `LayoutBuilder`/`SizedBox` guard for unbounded width and
  expanding unbounded height. At that milestone the surface was 19 widgets, 16
  const definitions and 617 rows (600 outside Scaffold). Nineteen sources across 18
  any-widget plus two trait destinations formed 380 candidates: 328 accepted and
  52 rejected. All schema/protocol versions remain unchanged.
- [x] Complete non-const `ListView(children: ...)` as the final item in the
  originally agreed core Palette. Its 17 optional constructor-intent rows cover
  scrolling (5), layout (4), caching/children (4), semantics (2) and restoration
  (2), with one ordered any-widget `children` slot. Keep controller-owned state,
  builders/delegates, `itemExtentBuilder`, `prototypeItem`, deprecated
  `cacheExtent`, `key` and raw Dart outside the slice. Generate the six reviewed
  physics presets and `ScrollCacheExtent.pixels`, enforce
  `semanticChildCount <= children.length`, and render the real Flutter ListView
  with vertical/horizontal, reverse and LTR/RTL drop geometry plus a generated/
  Canvas 120-high or 240-wide constraint guard for every unbounded viewport
  cross axis and for a non-shrink-wrapped unbounded main axis. At that core
  milestone the surface was 20 widgets, 16 const definitions and 634 rows (617
  outside Scaffold).
  Twenty sources across 19 any-widget plus two trait destinations form 420
  candidates: 365 accepted and 55 rejected. `.fd` schema v6, Catalog API 5,
  Canvas model v11 and version-1 framing/control stay unchanged. The originally
  agreed list—`Container`, `Row`, `Column`, `Text`, `Image`, Button through
  `ElevatedButton`, `TextField` and `ListView`—is complete 8/8; this explicitly
  does not mean that every Flutter widget is implemented.
- [x] Complete const `Wrap(children: ...)` as the first post-core Palette
  vertical slice. Expose all nine non-`key` constructor arguments with closed
  axis/alignment/direction/clip enums and finite signed `spacing`/`runSpacing`,
  plus one ordered any-widget `children` slot. Generated Dart and Canvas build
  the real Flutter Wrap. Empty instances retain the bounded 36-pixel Designer
  target, while both empty and populated instances use the complete rendered
  rectangle for deterministic terminal append because wrapped runs have no
  single stable terminal edge. At that milestone the surface was 21 widgets, 17 const
  definitions and 643 rows (626 outside Scaffold), with 13 Layout items.
  Twenty-one sources across 20 any-widget plus two trait destinations form 462
  candidates: 404 accepted and 58 rejected. All schema/protocol versions remain
  unchanged. This begins a practical 92-widget Material/Base Designer backlog;
  71 remain after Wrap. The number is a planning target, not a normative full
  Flutter widget list.
- [x] Complete const `FittedBox` as the second post-core Palette vertical slice.
  Expose optional `BoxFit fit`, physical/directional `AlignmentGeometry
  alignment`, `Clip clipBehavior` and one optional single any-widget `child`,
  preserving the omitted `contain`, centered and unclipped framework defaults.
  Generated Dart and both Canvas projections build the real Flutter FittedBox,
  including all seven fits, LTR/RTL directional alignment and all four clip
  behaviors. Empty zero-size instances retain the bounded 36-pixel Designer
  selection/drop target without changing Flutter layout. Complete Properties,
  Create, Palette/tree/Canvas DnD, exact-slot editing, same-tree movement,
  deterministic generation, Save/reopen, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 22 widgets, 18 const
  definitions and 646 rows (629 outside Scaffold), with 14 Layout items.
  Twenty-two sources across 21 any-widget plus two trait destinations form 506
  candidates: 445 accepted and 61 rejected. The practical 92-widget backlog is
  22/92 complete with 70 remaining. All schema/protocol versions remain
  unchanged.
- [x] Complete non-const `ConstrainedBox` as the third post-core Palette vertical
  slice. Expose required typed `BoxConstraints constraints` and one optional
  single any-widget `child`. Support finite, unbounded and expanding states on
  each axis, including `double.infinity` minima only when the corresponding
  maximum is also infinite; reject negative finite bounds and finite minimums
  above maximums. Generated Dart and both Canvas projections build the real
  Flutter ConstrainedBox. Empty zero-size instances retain a bounded,
  non-layout-affecting Designer selection/drop target. Complete Properties,
  Create, Palette/tree/Canvas DnD, exact-slot editing, same-tree movement,
  deterministic generation, Save/reopen, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 23 widgets, 18 const
  definitions and 647 rows (630 outside Scaffold), with 15 Layout items.
  Twenty-three sources across 22 any-widget plus two trait destinations form 552
  candidates: 488 accepted and 64 rejected. The practical 92-widget backlog is
  23/92 complete with 69 remaining. Canonical nullable infinity for all four
  BoxConstraints bounds advances `.fd` to schema v7, the exported value domain
  advances Catalog API to 6 and Canvas model to v12; NBFC framing and
  control/wire remain v1.
- [x] Complete const
  [`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
  as the fourth post-core Palette vertical slice at Layout order 107. Expose
  optional `textDirection`, `alignment`, `constrainedAxis` and `clipBehavior`
  plus one optional single any-widget `child`, with no persisted creation
  defaults. Omission preserves centered alignment, no retained axis and
  `Clip.none`; ambient `Directionality` resolves directional alignment when
  `textDirection` is omitted. Generated Dart and both Canvas projections build
  the real Flutter UnconstrainedBox. Empty zero-size instances retain a bounded,
  non-layout-affecting Designer selection/drop target. Complete Properties,
  Create, Palette/tree/Canvas DnD, exact-slot editing, same-tree movement,
  deterministic generation, Save/reopen, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 24 widgets, 19 const
  definitions and 651 rows (634 outside Scaffold), with 16 Layout items.
  Twenty-four sources across 23 any-widget plus two trait destinations form 600
  candidates: 533 accepted and 67 rejected. The practical 92-widget backlog was
  24/92 complete with 68 remaining. `.fd` schema v7, Catalog API 6, Canvas model
  v12 and NBFC framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
  as the fifth post-core Palette vertical slice at Layout order 108. Expose
  optional non-negative finite `maxWidth` and `maxHeight` plus one optional
  single any-widget `child`. Persist no creation defaults; omission canonically
  preserves each `double.infinity` framework default. Generated Dart and both
  Canvas projections build the real Flutter LimitedBox and prove that a selected
  maximum applies only when the incoming maximum constraint on that axis is
  unbounded. Complete Properties, Create, Palette/tree/Canvas DnD, exact-slot
  editing, same-tree movement, deterministic generation, Save/reopen, Undo/Redo,
  reviewed light/dark SVG icons and focused contract tests. At that milestone
  the surface was 25 widgets, 20 const definitions and 653 rows (636 outside Scaffold), with
  17 Layout items. Twenty-five sources across 24 any-widget plus two trait
  destinations form 650 candidates: 580 accepted and 70 rejected. The practical
  92-widget backlog was 25/92 complete with 67 remaining. `.fd` schema v7,
  Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- [x] Complete const
  [`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
  as the sixth post-core Palette vertical slice at Layout order 109. Expose
  optional physical/directional alignment, finite non-negative double
  `minWidth`, `maxWidth`, `minHeight` and `maxHeight` overrides, exact
  `OverflowBoxFit.max`/`deferToChild`, and one optional single any-widget
  `child`. Persist no creation defaults: omitted bounds inherit the
  corresponding parent constraints, while omitted alignment and fit preserve
  `Alignment.center` and `OverflowBoxFit.max`. Reject non-normalized pairs and
  explicit non-finite overrides. Generated Dart and both Canvas projections
  build the real Flutter OverflowBox and prove constraint override, overflow,
  LTR/RTL directional alignment and both fit modes. Complete Properties,
  Create, Palette/tree/Canvas DnD, exact-slot editing, same-tree movement,
  deterministic generation, Save/reopen, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 26 widgets, 21 const
  definitions and 659 rows (642 outside Scaffold), with 18 Layout items.
  Twenty-six sources across 25 any-widget plus two trait destinations form 702
  candidates: 629 accepted and 73 rejected. The practical 92-widget backlog is
  26/92 complete with 66 remaining. `.fd` schema v7, Catalog API 6, Canvas model
  v12 and NBFC framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
  as the seventh post-core Palette vertical slice at Layout order 130,
  immediately after Expanded. Expose optional non-negative portable integer
  `flex`, optional `FlexFit.loose`/`tight`, and one required single any-widget
  `child`. Persist no constructor defaults: omission preserves Flutter's
  `flex: 1` and `FlexFit.loose`. Palette/tree/Canvas creation atomically wraps
  an existing direct `Row.children` or `Column.children` child and never creates
  a terminal required-child placeholder; Flexible and Expanded cannot wrap
  either wrapper type because the inner parent-data widget would cease to be a
  direct Flex child. The occupied required child is replacement-only, cannot be
  cleared and is excluded from insertable DnD destinations. Generated Dart and
  both Canvas projections construct
  the real Flutter Flexible, including zero-flex inflexible layout and loose or
  tight positive-flex allocation. Complete Properties, creation,
  Palette/tree/Canvas DnD, required-child replacement, same-tree movement,
  deterministic generation, Save/reopen, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 27
  widgets, 22 const definitions and 661 rows (644 outside Scaffold), with 19
  Layout items. Twenty-seven sources across the unchanged 25 insertable
  any-widget plus two trait destinations formed 729 candidates: 631 accepted
  and 98 rejected. The practical 92-widget backlog was 27/92 complete with 65
  remaining. `.fd` schema
  v7, Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- [x] Complete const
  [`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html) as the
  eighth post-core Palette vertical slice at Layout order 140, immediately
  after Flexible. Expose one optional positive portable integer `flex` and no
  slots. Persist no constructor default: omission preserves Flutter's `flex: 1`,
  while zero, negative and over-limit values fail closed. Palette/tree/Canvas
  creation inserts a childless Spacer prototype only into direct
  `Row.children` or `Column.children`; it never wraps an existing child.
  Expanded and Flexible cannot wrap Spacer because Spacer internally builds an
  Expanded parent-data path that must remain directly below Row or Column.
  Generated Dart emits the real Flutter Spacer. Both Canvas projections keep
  the real Spacer directly under the Flex and expose Designer selection and
  outlines through surface overlay geometry instead of an invalid outer
  render-object wrapper. Complete Properties, creation, Palette/tree/Canvas
  DnD, same-tree movement, deterministic generation, Save/reopen, Undo/Redo,
  reviewed light/dark SVG icons and focused contract tests. At that milestone the surface
  was 28 widgets, 23 const definitions and 662 rows (645 outside Scaffold), with
  20 Layout items. Twenty-eight sources across the unchanged 25 insertable
  any-widget plus two trait destinations form 756 candidates: 633 accepted and
  123 rejected. The practical 92-widget backlog is 28/92 complete with 64
  remaining. `.fd` schema v7, Catalog API 6, Canvas model v12 and NBFC
  framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html)
  as the ninth post-core Palette vertical slice at Layout order 150,
  immediately after Spacer. Expose required finite-double `baseline`, required
  `TextBaseline.alphabetic`/`ideographic` `baselineType`, and one optional
  single any-widget `child`. Flutter supplies no constructor defaults, so a
  detached Designer prototype persists the reviewed visible starting values
  `baseline: 24.0` and `baselineType: TextBaseline.alphabetic`; reject NaN,
  infinity, wrong enum types and raw Dart. Generated Dart and both Canvas
  projections construct the real Flutter Baseline. Preserve childless framework
  `constraints.smallest` layout (often zero) while supplying selection and
  insertion through a bounded, non-layout-affecting Designer target. Complete
  Properties, exact-slot
  editing, Palette/tree/Canvas DnD, same-tree movement, deterministic
  generation, Save/reopen, Undo/Redo, reviewed light/dark SVG icons and focused
  contract tests. At that milestone the surface was 29 widgets, 24 const definitions and
  664 rows (647 outside Scaffold), with 21 Layout items. Twenty-nine sources
  across 26 insertable any-widget plus two trait destinations form 812
  candidates: 684 accepted and 128 rejected. The practical 92-widget backlog is
  29/92 complete with 63 remaining. `.fd` schema v7, Catalog API 6, Canvas model
  v12 and NBFC framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
  as the tenth post-core Palette vertical slice at Layout order 160,
  immediately after Baseline. Its complete non-`key` Flutter 3.44.8
  constructor surface has no writable properties and one optional single
  any-widget `child`. Generated Dart and both Canvas projections construct the
  real Flutter IntrinsicHeight, preserving parent constraints and the
  speculative intrinsic-height layout pass. Keep the framework performance
  warning visible because intrinsic measurement is relatively expensive and
  can be O(N²) in tree depth. Empty or collapsed nodes retain real layout while
  a bounded, non-layout-affecting Designer target supplies selection and
  insertion. Complete exact-slot editing, Palette/tree/Canvas DnD, same-tree
  movement, deterministic generation, Save/reopen, Undo/Redo, reviewed
  light/dark SVG icons and focused contract tests. At that milestone the surface was 30
  widgets, 25 const definitions and 664 rows (647 outside Scaffold), with 22
  Layout items. Thirty sources across 27 insertable any-widget plus two trait
  destinations formed 870 candidates: 737 accepted and 133 rejected. The
  practical 92-widget backlog was 30/92 complete with 62 remaining. `.fd`
  schema v7, Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1
  remain unchanged.
- [x] Complete const
  [`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
  as the eleventh post-core Palette vertical slice at Layout order 170,
  immediately after IntrinsicHeight. Its complete non-`key` Flutter 3.44.8
  constructor surface exposes optional finite non-negative `stepWidth` and
  `stepHeight` values plus one optional single any-widget `child`. Preserve
  null and explicit zero as distinct model/Dart values while matching Flutter's
  no-snapping behavior for either; positive values snap the corresponding
  intrinsic extent upward to a multiple of the step. Generated Dart and both
  Canvas projections construct the real IntrinsicWidth under parent
  constraints. Keep the relatively expensive speculative-layout and worst-case
  O(N²) warning visible. Empty or collapsed nodes retain real layout behind a
  bounded, non-layout-affecting Designer target. Complete property and slot
  editing, Palette/tree/Canvas DnD, same-tree movement, deterministic
  generation, Save/reopen, Undo/Redo, reviewed light/dark SVG icons and focused
  contract tests. At that milestone the surface was 31 widgets, 26 const
  definitions and 666 rows (649 outside Scaffold), with 23 Layout items.
  Thirty-one sources across 28 insertable any-widget plus two trait destinations
  formed 930 candidates: 792 accepted and 138 rejected. The practical 92-widget
  backlog was 31/92 complete with 61 remaining. `.fd` schema v7, Catalog API 6, Canvas
  model v12 and NBFC framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html)
  as the twelfth post-core Palette vertical slice at Layout order 180,
  immediately after IntrinsicWidth. Its complete non-`key` Flutter 3.44.8
  constructor surface exposes optional boolean `offstage` with constructor
  default `true`, plus one optional single any-widget `child`. Preserve omission
  and explicit `true` as distinct model/Dart/history values even though both
  hide the child, and support explicit `false` for normal participation.
  Generated Dart and both Canvas projections construct the real Offstage. When
  hidden, the child is still laid out, remains active and focusable, and keeps
  animations running, while paint, hit testing and semantics are suppressed
  and the widget normally contributes no parent space. Expose that resource
  warning and recommend subtree removal for long-term hiding. Keep the
  selection/drop overlay outside the Offstage effect and use a bounded 36x36
  target only for a real zero-sized result. Complete property and exact-slot
  editing, Palette/tree/Canvas DnD, same-tree movement, deterministic
  generation, Save/reopen, further editing, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 32
  widgets, 27 const definitions and 667 rows (650 outside Scaffold), with 24
  Layout items.
  Thirty-two sources across 29 insertable any-widget plus two trait destinations
  form 992 candidates: 849 accepted and 143 rejected. The practical 92-widget
  backlog was 32/92 complete with 60 remaining. `.fd` schema v7, Catalog API 6,
  Canvas model v12 and NBFC framing/control/wire v1 remained unchanged.
- [x] Complete const
  [`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
  as the thirteenth post-core Palette vertical slice at Layout order 190,
  immediately after Offstage. Its complete non-`key` Flutter 3.44.8 constructor
  surface exposes required structured `Size size`, optional
  physical/directional `AlignmentGeometry alignment` with framework default
  `Alignment.center`, and one optional single any-widget `child`. Add the first
  atomic Size value to persistence with finite non-negative width and height;
  detached prototypes start at the visible `Size(100, 100)`. Generated Dart
  and both Canvas projections construct the real SizedOverflowBox, constrain
  only its requested outer size, pass the original incoming constraints to the
  child, preserve aligned visual overflow and keep hit testing within the
  parent box. Use a bounded 36x36 Designer target only for a true zero-size
  result. Complete property and exact-slot editing, Palette/tree/Canvas DnD,
  same-tree movement, deterministic generation, Save/reopen and further
  editing, Undo/Redo, reviewed light/dark SVG icons and focused contract tests.
  At that milestone the surface was 33 widgets, 28 const definitions and 669 rows (652
  outside Scaffold), with 25 Layout items. Thirty-three sources across 30
  insertable any-widget plus two trait destinations form 1,056 candidates: 908
  accepted and 148 rejected. The practical 92-widget backlog was 33/92 complete
  with 59 remaining. `.fd` schema was v8, Catalog API was 7 and Canvas model was
  v13; NBFC framing/control/wire remained v1.
- [x] Complete const
  [`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
  as the fourteenth post-core Palette vertical slice at Layout order 200,
  immediately after SizedOverflowBox in Flutter's canonical Layout catalog.
  Its complete non-`key` Flutter 3.44.8 `Transform.new` surface exposes required
  structured `Matrix4 transform`, optional signed finite `Offset origin`,
  optional physical/directional `AlignmentGeometry alignment`, optional boolean
  `transformHitTests` with framework default `true`, optional
  `FilterQuality.none/low/medium/high` and one optional single any-widget
  `child`. Add the first atomic Offset value to persistence; detached prototypes
  store only `Matrix4.identity()`, while all optional values remain omitted.
  Generated Dart and both Canvas projections construct the real paint-time
  Transform without changing layout size, compose origin and alignment, apply
  optional filtering and transform child hit tests exactly when
  `transformHitTests` resolves to true. Keep
  Designer selection/drop geometry on the same effective transform without
  changing layout, and use a bounded 36x36 target only for a true zero-size
  result. Complete property and exact-slot editing, Palette/tree/Canvas DnD,
  same-tree movement, deterministic
  generation, Save/reopen and further editing, Undo/Redo, reviewed light/dark
  SVG icons and focused contract tests. The named `.rotate`, `.translate`,
  `.scale` and `.flip` convenience constructors remain explicitly outside this
  slice. At that milestone the surface was 34 widgets, 29 const definitions and 674 rows
  (657 outside Scaffold), with 26 Layout items. Thirty-four sources across 31
  insertable any-widget plus two trait destinations form 1,122 candidates: 969
  accepted and 153 rejected. The practical 92-widget backlog is 34/92 complete
  with 58 remaining. `.fd` schema is v9, Catalog API is 8 and Canvas model is
  v14; NBFC framing/control/wire remains v1.
- [x] Complete const
  [`RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
  as the fifteenth post-core Palette vertical slice at Layout order 210,
  immediately after Transform. Its complete non-`key` Flutter 3.44.8 constructor
  surface exposes required signed portable-integer `quarterTurns` and one
  optional single any-widget `child`. Detached prototypes store `1`; validation
  accepts exactly `-9007199254740991..9007199254740991` so native and Web emit
  the same integer. Generated Dart and both Canvas projections construct the
  real layout-time RotatedBox: odd turns exchange the child's axes, even turns
  retain them, and Flutter paints the modulo-four equivalent without replacing
  the exact stored value. Complete property and exact-slot editing,
  Palette/tree/Canvas DnD, same-tree movement, deterministic generation,
  Save/reopen and further signed editing, Undo/Redo, reviewed light/dark SVG
  icons and focused contract tests. At that milestone the surface was 35 widgets, 30 const
  definitions and 675 rows (658 outside Scaffold), with 27 Layout items.
  Thirty-five sources across 32 insertable any-widget plus two trait
  destinations form 1,190 candidates: 1,032 accepted and 158 rejected. The
  practical 92-widget backlog is 35/92 complete with 57 remaining. `.fd` stays
  at schema v9, Catalog API at 8 and Canvas model at v14; NBFC
  framing/control/wire remains v1.
- [x] Complete const
  [`ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html) as
  the sixteenth post-core Palette vertical slice at Layout order 220,
  immediately after RotatedBox. Its complete non-`key` Flutter 3.44.8
  constructor surface exposes optional `Axis mainAxis` and `bool reverse`, with
  defaults `Axis.vertical` and `false`, plus one ordered any-widget `children`
  list. Detached prototypes omit both properties and begin with an empty list.
  Generate the real bare ListBody while hosting the real Canvas widget in an
  axis-matched design-time viewport that supplies its required unbounded main
  axis and bounded cross axis; never persist or generate that preview guard.
  Resolve empty and terminal list-drop geometry from axis, reversal and ambient
  directionality. Complete property and exact-list-slot editing,
  Palette/tree/Canvas DnD, same-tree movement/reordering, deterministic
  generation, Save/reopen and further editing, Undo/Redo, reviewed light/dark
  SVG icons and focused contract tests. At that milestone the surface was 36 widgets, 31
  const definitions and 677 rows (660 outside Scaffold), with 28 Layout items.
  Thirty-six sources across 33 insertable any-widget plus two trait destinations
  form 1,260 candidates: 1,097 accepted and 163 rejected. The practical
  92-widget backlog is 36/92 complete with 56 remaining. `.fd` stays at schema
  v9, Catalog API at 8 and Canvas model at v14; NBFC framing/control/wire remains
  v1.
- [x] Complete const
  [`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
  as the seventeenth post-core Palette vertical slice at Layout order 230,
  immediately after ListBody. Its complete non-`key` Flutter 3.44.8 constructor
  surface exposes optional finite signed `spacing`, nullable
  `MainAxisAlignment alignment`, finite signed `overflowSpacing`,
  `OverflowBarAlignment overflowAlignment`, `VerticalDirection
  overflowDirection`, nullable `TextDirection textDirection`, and one ordered
  any-widget `children` list. Detached prototypes omit all six properties and
  begin empty. Generate the real bare OverflowBar; for a nonempty node, bound an
  otherwise unbounded Canvas preview width only when `alignment` is non-null,
  retain natural width for null alignment, and retain a 36x36 empty
  selection/drop target. Never persist or generate those guards. Resolve
  insertion and move geometry from the real rendered mode: fitting horizontal
  rows follow effective LTR/RTL direction, while vertical overflow columns
  follow `overflowDirection`. Complete property and exact-list-slot editing,
  Palette/tree/Canvas DnD, same-tree movement/reordering, deterministic
  generation, Save/reopen and further editing, Undo/Redo, reviewed light/dark
  SVG icons and focused contract tests. At that milestone the surface was 37 widgets, 32
  const definitions and 683 rows (666 outside Scaffold), with 29 Layout items.
  Thirty-seven sources across 34 insertable any-widget plus two trait
  destinations form 1,332 candidates: 1,164 accepted and 168 rejected. The
  practical 92-widget backlog is 37/92 complete with 55 remaining. `.fd` stays
  at schema v9, Catalog API at 8 and Canvas model at v14; NBFC
  framing/control/wire remains v1.
- [x] Complete non-const
  [`GridView.count`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
  as the eighteenth post-core Palette vertical slice at Scrolling order 20,
  immediately after ListView. Expose the complete reviewed static-child
  Flutter 3.44.8 non-`key` named constructor as 21 typed rows: scroll direction,
  reverse, primary, a closed physics preset, shrink wrap, padding, required
  positive cross-axis count with creation value 2, main/cross spacing, child
  aspect ratio, optional main-axis extent, automatic keep-alives, repaint
  boundaries, semantic indexes, pixel cache extent, semantic child count, drag
  start, keyboard dismissal, restoration ID, clip behavior and hit-test
  behavior. Keep children in one exact ordered any-widget list slot. Exclude
  controller state, builders/delegates, other named constructors,
  `scrollBehavior`, `key` and deprecated raw `cacheExtent`. Complete grouped
  NetBeans Properties, Palette/tree/Canvas DnD, same-tree movement/reordering,
  deterministic named-constructor generation, Save/reopen and further editing,
  Undo/Redo, native and exact-Web Canvas, four reviewed SVGs and focused
  contracts. At this milestone, the surface had 38 widgets, 32 const definitions
  and 704 rows (687 outside Scaffold). Thirty-eight sources across 35 insertable
  any-widget plus two trait destinations formed 1,406 candidates: 1,233 accepted
  and 173 rejected. Layout had 29 items, Scrolling had 2, and the practical
  92-widget backlog was 38/92 complete with 54 remaining. `.fd` stayed at schema
  v9, Catalog API at 8 and Canvas model at v14; NBFC framing/control/wire
  remained v1.
- [x] Complete const
  [`SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
  as the nineteenth post-core Palette vertical slice at Scrolling order 30,
  immediately after `GridView.count`. Expose the complete reviewed Flutter
  3.44.8 non-`key`, non-controller constructor as 10 typed rows: scroll
  direction, reverse, non-negative padding, nullable primary policy, a closed
  physics preset, drag-start behavior, clip behavior, hit-test behavior,
  restoration ID and keyboard dismissal. Keep one optional any-widget `child`
  slot at its constructor position. Detached prototypes omit all properties and
  begin empty. Exclude controller-owned state and arbitrary physics graphs.
  Complete grouped NetBeans Properties, Palette/tree/Canvas DnD, same-tree
  movement, deterministic generation, Save/reopen and further editing,
  Undo/Redo, native and exact-Web Canvas, four reviewed SVGs and focused
  contracts. Preserve the real widget's deliberate two-axis shrink-wrapping:
  do not apply the ListView/GridView generated bounded-viewport guard, and use
  only a non-layout-affecting Canvas target for an empty or zero-size node. The
  surface at that milestone was 39 widgets, 33 const definitions and 714 rows (697 outside
  Scaffold). Thirty-nine sources across 36 insertable any-widget plus two trait
  destinations form 1,482 candidates: 1,304 accepted and 178 rejected. Layout
  remains at 29 items, Scrolling contains 3, and the practical 92-widget
  backlog is 39/92 complete with 53 remaining. `.fd` stays at schema v9,
  Catalog API at 8 and Canvas model at v14; NBFC framing/control/wire remains
  v1.
- [x] Complete const
  [`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
  as the twentieth post-core Palette vertical slice at Basic order 40, after
  `Image`. Expose the complete reviewed Flutter 3.44.8 non-`key` constructor as
  required theme-aware `color`, optional `isAntiAlias` with omitted default
  `true`, and one optional any-widget `child`. Create detached nodes with literal
  `Color(0xFF2196F3)`, anti-aliasing omitted and the child empty. Admit exact
  ARGB or reviewed Material `ColorScheme` tokens; preserve const for literals
  and emit non-const `Theme.of(context).colorScheme...` for tokens. Complete
  grouped NetBeans Properties, Palette/tree/Canvas DnD, same-tree movement,
  deterministic generation, Save/reopen and further editing, Undo/Redo, native
  and exact-Web Canvas, four reviewed SVGs and focused contracts. Keep only a
  non-layout-affecting 36x36 Canvas target for an empty zero-size node and never
  persist it. At that milestone the surface was 40 widgets, 34 const definitions and 716
  rows (699 outside Scaffold). Forty sources across 37 insertable any-widget
  plus two trait destinations form 1,560 candidates: 1,377 accepted and 183
  rejected. Layout remains at 29 items, Scrolling at 3, Basic contains 4, and the
  practical 92-widget backlog is 40/92 complete with 52 remaining. `.fd` stays
  at schema v9, Catalog API at 8 and Canvas model at v14; NBFC
  framing/control/wire remains v1.
- [x] Complete const
  [`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
  as the twenty-first post-core Palette vertical slice at Layout order 240,
  after `OverflowBar`. Expose all six optional non-`key` constructor properties:
  `left`, `top`, `right`, `bottom`, signed finite physical `EdgeInsets minimum`
  and `maintainBottomViewPadding`, plus one required any-widget child. Preserve
  Flutter defaults by omitting every property on creation and reject
  `EdgeInsetsDirectional`. Generalize atomic wrapper creation from the catalog's
  required-single-any-widget shape, so no incomplete SafeArea is inserted.
  Palette/tree may wrap an existing root or non-root widget; the current Canvas
  target wire wraps non-root children only and deliberately offers no root
  target. Reject wrapping Expanded, Flexible or Spacer because their ParentData
  must remain directly below Row/Column. Complete typed Properties,
  deterministic generation, native/exact-Web Canvas, Save/reopen and further
  editing, Undo/Redo and four reviewed SVGs. At that milestone the surface was 41 widgets,
  35 const definitions and 722 rows (705 outside Scaffold). Forty-one sources
  across 37 insertable any-widget plus two trait destinations form 1,599
  candidates: 1,414 accepted and 185 rejected. Layout contains 30 items,
  Scrolling 3, Basic 4 and Material 4; the practical backlog is 41/92 complete
  with 51 remaining. `.fd` stays at schema v9 and Canvas model at v14; adding
  the exported `EdgeInsetsValues.directionalAllowed` constraint advances
  Catalog API to 9. NBFC framing/control/wire remains v1.
- [x] Complete const
  [`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
  as the twenty-second post-core Palette vertical slice at Basic order 50,
  after `ColoredBox`. Expose the complete Flutter 3.44.8 non-`key` constructor:
  optional theme-aware `color`, finite non-negative `strokeWidth`,
  `fallbackWidth` and `fallbackHeight`, plus one optional any-widget `child`.
  Omit all four properties in a detached prototype to preserve Flutter's exact
  `Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults. Complete typed
  grouped Properties, ordinary Palette/tree/Canvas Create and DnD, same-tree
  movement, exact-slot management, deterministic const-aware Dart generation,
  Save/reopen and further editing, Undo/Redo, real native/exact-Web Canvas,
  accessibility and four reviewed SVGs. At that milestone the surface was 42 widgets, 36
  const definitions and 726 rows (709 outside Scaffold). Forty-two sources
  across 38 insertable any-widget plus two trait destinations formed 1,680
  candidates: 1,490 accepted and 190 rejected. Layout contained 30 items,
  Scrolling 3, Basic 5 and Material 4; the practical backlog was 42/92 complete
  with 50 remaining. Existing encodings kept `.fd` schema v9, Catalog API 9,
  Canvas model v14 and NBFC framing/control/wire v1 unchanged.
- [x] Complete const
  [`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
  as the twenty-third post-core Palette vertical slice at Basic order 60,
  after `Placeholder`. Expose the complete Flutter 3.44.8 non-`key`
  constructor: required `TextDirection textDirection` and one required
  any-widget `child`. Because Flutter has no direction default, detached
  wrapper creation persists the reviewed Designer value `TextDirection.ltr`
  and Palette/tree/Canvas creation atomically wraps an existing subtree rather
  than exposing an invalid empty node. Complete typed Properties, exact
  required-slot management, generic wrapper DnD, same-tree movement,
  deterministic const Dart generation, Save/reopen and further editing,
  Undo/Redo, real native/exact-Web inherited direction, accessibility and four
  reviewed SVGs. At that milestone the surface was 43 widgets, 37 const definitions and
  727 rows (710 outside Scaffold). Forty-three sources across 38 insertable
  any-widget plus two trait destinations form 1,720 candidates: 1,528 accepted
  and 192 rejected. Layout contains 30 items, Scrolling 3, Basic 6 and Material
  4; the practical backlog was 43/92 complete with 49 remaining. Existing
  encodings keep `.fd` schema v9, Catalog API 9, Canvas model v14 and NBFC
  framing/control/wire v1 unchanged.
- [x] Complete const
  [`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
  as the twenty-fourth post-core Palette vertical slice at Basic order 70,
  after `Directionality`. Expose the complete Flutter 3.44.8 non-`key`
  constructor: required typed `Decoration decoration`, optional closed
  `DecorationPosition position` with exact `background` default, and one
  optional any-widget `child`. Reuse the complete Designer-owned
  `BoxDecoration` algebra and editor; custom `Decoration` subclasses and raw
  Dart remain excluded. Detached creation persists an exact empty rectangular
  `BoxDecoration()` so the required property is valid. Complete typed
  Properties, Palette/tree/Canvas DnD, exact-slot management, same-tree
  movement, deterministic const Dart generation, Save/reopen and further
  editing, Undo/Redo, real native/exact-Web background and foreground paint,
  accessibility and four reviewed SVGs. At that milestone the surface was 44 widgets, 38
  const definitions and 729 rows (712 outside Scaffold). Forty-four sources
  across 39 insertable any-widget plus two trait destinations formed 1,804
  candidates: 1,607 accepted and 197 rejected. Layout contained 30 items,
  Scrolling 3, Basic 7 and Material 4; the practical backlog was 44/92 complete
  with 48 remaining. Existing encodings kept `.fd` schema v9, Catalog API 9,
  Canvas model v14 and NBFC framing/control/wire v1 unchanged.
- [x] Complete const
  [`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
  as the first Accessibility Palette vertical slice at category order 400 and
  item order 10. Expose the complete Flutter 3.44.8 non-`key` constructor:
  optional closed `bool excluding` with exact omitted default `true` and one
  optional any-widget `child`. Detached creation stores neither a property nor
  a child. Complete typed Properties, Palette/tree/Canvas DnD, exact-slot
  management, same-tree movement, deterministic const Dart generation,
  Save/reopen and further editing, Undo/Redo, real native/exact-Web semantics,
  accessibility and four reviewed SVGs. Omitted or explicit `true` removes the
  application child's semantics subtree, while explicit `false` preserves it;
  layout, paint and hit testing proxy the child. Keep the `ExcludeSemantics`
  node's own Designer selection, hit/drop and accessibility wrapper outside the
  effect; descendant Canvas semantics labels follow the real subtree exclusion,
  while the NetBeans widget tree remains separately accessible. At that
  milestone the surface was 45 widgets, 39 const definitions
  and 730 rows (713 outside Scaffold). Forty-five sources across 40 insertable
  any-widget plus two trait destinations formed 1,890 candidates: 1,688 accepted
  and 202 rejected. Layout contained 30 items, Scrolling 3, Basic 7, Material 4
  and Accessibility 1; the practical backlog was 45/92 complete with 47
  remaining. Existing encodings keep `.fd` schema v9, Catalog API 9, Canvas
  model v14 and NBFC framing/control/wire v1 unchanged.
- [x] Complete const
  [`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
  as the first remaining gap in the fixed practical inventory, at Layout order
  115 beside `Stack`. Expose the complete Flutter 3.44.8 non-`key` constructor:
  optional physical/directional `alignment`, `textDirection`, `clipBehavior`,
  `sizing`, nullable `index` and one ordered any-widget `children` slot. Keep
  omission distinct from explicit null: omitted index preserves the framework
  default `0`, an integer selects one existing child and typed `null` displays
  none. Enforce the live index/children relationship including Flutter's exact
  empty-list index-zero exception. Complete typed Properties,
  Palette/tree/Canvas DnD and movement, deterministic const Dart generation,
  Save/reopen and further editing, Undo/Redo, real native/exact-Web layout,
  paint, hit testing and semantics, accessibility, empty-target handling and
  four reviewed SVGs. The real widget sizes to its largest child while only the
  selected child paints, hits and contributes application semantics; all
  children remain ordered in the model and NetBeans tree. The resulting
  surface is 46 widgets, 40 const definitions and 735 rows (718 outside
  Scaffold). Forty-six sources across 41 insertable any-widget plus two trait
  destinations form 1,978 candidates: 1,771 accepted and 207 rejected. Layout
  contains 31 items, Scrolling 3, Basic 7, Material 4 and Accessibility 1; the
  practical backlog is 46/92 complete with 46 remaining. Exact typed null
  advances `.fd` schema to v10, Catalog API to 10 and Canvas model to v15;
  NBFC framing/control/wire remain v1.
- [x] Complete const
  [`ClipRect`](https://api.flutter.dev/flutter/widgets/ClipRect/ClipRect.html)
  as the next fixed practical-inventory slice, in Basic at item order 80 after
  `DecoratedBox`. Expose the safe Flutter 3.44.8 non-`key` constructor surface:
  optional closed `clipBehavior` and one optional single any-widget `child`
  slot. Preserve the framework's `Clip.hardEdge` default by omission and admit
  the other three exact `Clip` values. Complete typed Properties,
  Palette/tree/Canvas DnD and movement, deterministic const Dart generation,
  Save/reopen and further editing, Undo/Redo, real native/exact-Web clipping,
  accessibility, empty-target handling and four reviewed SVGs. Keep Designer
  selection and DnD affordances outside the paint clip. The initial v10 slice
  excluded non-null `CustomClipper<Rect>`; schema v12 now closes that branch
  with the shared typed Dart-object reference instead of arbitrary expressions. The
  resulting surface is 47 widgets, 41 const definitions and 736 rows (719
  outside Scaffold). Forty-seven sources across 42 insertable any-widget plus
  two trait destinations form 2,068 candidates: 1,856 accepted and 212
  rejected. Layout contains 31 items, Scrolling 3, Basic 8, Material 4 and
  Accessibility 1; the practical backlog is 47/92 complete with 45 remaining.
  `.fd` schema v10, Catalog API 10, Canvas model v15 and NBFC
  framing/control/wire v1 remain unchanged.
- [x] Complete const
  [`ClipOval`](https://api.flutter.dev/flutter/widgets/ClipOval/ClipOval.html)
  as the next fixed practical-inventory slice, in Basic at item order 90 after
  `ClipRect`. Expose the safe Flutter 3.44.8 non-`key` constructor surface:
  optional closed `clipBehavior` and one optional single any-widget `child`
  slot. Preserve the framework's `Clip.antiAlias` default by omission and
  admit the other three exact `Clip` values. Complete typed Properties,
  Palette/tree/Canvas DnD and movement, deterministic const Dart generation,
  Save/reopen and further editing, Undo/Redo, real native/exact-Web oval
  clipping, accessibility, empty-target handling and four reviewed SVGs. Keep
  Designer selection and DnD affordances outside the paint clip. The initial
  v10 slice excluded non-null `CustomClipper<Rect>`; schema v12 now closes that
  branch with the shared typed Dart-object reference instead of arbitrary expressions.
  At that milestone the resulting surface was 48 widgets, 42 const definitions and 737 rows (720
  outside Scaffold). Forty-eight sources across 43 insertable any-widget plus
  two trait destinations form 2,160 candidates: 1,943 accepted and 217
  rejected. Layout contained 31 items, Scrolling 3, Basic 9, Material 4 and
  Accessibility 1; the practical backlog was 48/92 complete with 44 remaining.
  `.fd` schema v10, Catalog API 10, Canvas model v15 and NBFC
  framing/control/wire v1 remained unchanged.
- [x] Complete const
  [`ClipRRect`](https://api.flutter.dev/flutter/widgets/ClipRRect/ClipRRect.html)
  as the next fixed practical-inventory slice, in Basic at item order 100 after
  `ClipOval`. Expose the safe Flutter 3.44.8 non-`key` constructor surface:
  optional typed `borderRadius`, optional typed `clipper`, optional closed
  `clipBehavior` and one optional single any-widget `child` slot. Support physical `BorderRadius` and
  directional `BorderRadiusDirectional` with finite, non-negative elliptical
  radii for all four corners. Preserve `BorderRadius.zero` and
  `Clip.antiAlias` by omission and admit the other three exact `Clip` values.
  Complete the structured typed Properties editor, Palette/tree/Canvas DnD and
  movement, deterministic Dart generation, Save/reopen and further
  editing, Undo/Redo, real native/exact-Web standard rounded clipping, accessibility,
  empty-target handling and four reviewed SVGs. Keep Designer selection and DnD
  affordances outside the paint clip. Represent a non-null
  `CustomClipper<RRect>` as a closed current-library or canonical package-config-declared `package:`
  Dart-object reference to an existing value or zero-argument
  constructor, factory or function, with an optional member and an explicit
  const or non-const zero-argument invocation. Validate the
  generated assignment with the analyzer and never persist raw Dart. Since the
  isolated Canvas cannot execute project code, transmit presence only and show
  an explicit accessible preview-unavailable state without faking the ignored
  `borderRadius`. Apply the same typed-reference, analyzer, persistence and
  presence-only Canvas contract to `ClipRect.clipper` and `ClipOval.clipper`,
  so all three clipping widgets are complete. The resulting surface is 49 widgets, 43 const definitions and
  742 rows (725 outside
  Scaffold). Forty-nine sources across 44 insertable any-widget plus two trait
  destinations form 2,254 candidates: 2,032 accepted and 222 rejected. Layout
  contains 31 items, Scrolling 3, Basic 10, Material 4 and Accessibility 1; the
  practical backlog is 49/92 complete with 43 remaining. The typed Dart-object
  reference advances `.fd` schema to v12, Catalog API to 12 and Canvas model to
  v17; NBFC framing/control/wire remain v1.
- [x] Add `ClipPath` after `ClipRRect` as the next reviewed clipping slice, including
  `ClipPath.shape` rather than postponing the ShapeBorder branch. Expose typed
  `clipper` (`CustomClipper<Path>`), typed `shape` (`ShapeBorder`), all four `Clip`
  values and optional `child`; reject simultaneous clipper/shape. Reuse the closed
  project/package reference and const/non-const zero-argument invocation contract,
  require exact analyzer assignment proof, and emit the static helper without
  const or const ancestors. Cover editors, Palette/Slots/tree/Canvas DnD, save/reopen,
  further editing, Undo/Redo, accessible preview limitations and four SVG variants.
  At that milestone: 50 widgets, 44 const-capable definitions, 745 rows (728 outside
  Scaffold), 45 any-widget plus two trait destinations, 2,350 cells (2,123 accepted /
  227 rejected). Basic has 11 items; other category counts are unchanged. Progress
  against the historical practical target is 50/92, 42 remaining. No full ordered
  92-item inventory was recovered; this successor is a reviewed API-based choice.
  Schema/API/model stay 12/12/17. The physical desktop acceptance gate remains
  deferred until the palette target is implemented.
- [x] Add `ClipRSuperellipse` after `ClipPath` (Basic order 120) with its complete
  non-key constructor surface: typed physical/directional elliptical `borderRadius`,
  `CustomClipper<RSuperellipse>` references, all four `clipBehavior` modes and nullable
  child. Use the actual Flutter superellipse widget, preserve radius while a custom
  clipper overrides it, and show an explicit custom-code preview limitation in the
  isolated Canvas. Include full editors, stable tree/Canvas selection, Palette/slot
  placement, deterministic generation, analyzer type proof, Save/reopen/further
  editing, Undo/Redo, accessibility and four SVG variants. At that milestone: 51 widgets,
  45 const-capable definitions, 748 rows (731 outside Scaffold), 46 any-widget plus
  two trait destinations; 2,448 candidates (2,216 accepted / 232 rejected).
  Basic has 12 items; historical target is 51/92 with 41 remaining. Schema/API/model
  remain 12/12/17; the full physical desktop gate remains deferred.
- [x] Add `PhysicalModel` after `ClipRSuperellipse` (Basic order 130) with its full
  constructor surface: shape, clipping, physical elliptical border radius, finite
  non-negative elevation, required fill color, optional shadow color and child.
  Both colors support literals/theme tokens; circle ignores but retains radius.
  Use real PhysicalModel rendering/shadows and keep empty selection/drop targets
  external. Include typed Properties/Slots, Palette/tree/Canvas insertion and
  movement, generation, validation, Save/reopen/further-edit, Undo/Redo, accessibility
  and four SVG variants. At that milestone: 52 widgets, 46 const definitions, 754 rows
  (737 outside Scaffold), 47 any-widget plus two trait destinations, 2,548 cells
  (2,311 accepted / 237 rejected). Basic 13; historical target 52/92, 40 remaining.
  Physical-only radius constraints advance Catalog API to 13; `.fd` 12 and Canvas
  model 17 stay unchanged. The full physical desktop gate remains deferred.
- [x] Add `PhysicalShape` after PhysicalModel (Basic order 140) with all five
  non-key properties and optional child. Support six typed ShapeBorderClipper presets
  with physical/directional elliptical radii and explicit direction, plus exact
  current/package CustomClipper<Path> references. Preserve ignored radius/direction,
  render real presets/shadows/theme colors, and show accessible custom-code preview
  warnings without fabricated geometry. Complete typed editors, slots, all placement
  routes, generation, save/reopen/further-edit, Undo/Redo, rollback and four SVGs.
  The closed value advances schema/API/model to 13/14/18; frozen v1-v12 schemas and
  NBFC framing/control/wire v1 stayed unchanged. At that milestone: 53 widgets, 47 const
  definitions, 759 rows (742 outside Scaffold), 48 any-widget plus two trait
  destinations, 2,650 cells (2,408 accepted / 242 rejected). Basic 14; historical
  target 53/92, 39 remaining. Full physical desktop acceptance remains deferred.
- [x] Add `RepaintBoundary` after PhysicalShape (Basic order 150), with optional
  child and managed key. It has no scalar constructor properties and uses exact
  structural capabilities like IntrinsicHeight. Render the real SDK repaint/layer
  boundary while preserving layout, semantics and hit testing; keep empty targets
  external. Complete Slots replace/clear, all insertion/move routes, generation,
  Save/reopen/further child/descendant edits, Undo/Redo, rollback and four SVGs.
  SDK wrap/wrapAll derive keys only and are not extra persisted properties.
  At that milestone: 54 widgets, 48 const definitions, 759 rows (742 outside Scaffold),
  49 any-widget plus two trait destinations, 2,754 cells (2,507 accepted / 247
  rejected), Basic 15; historical target 54/92 with 38 remaining. Schema/API/model
  remain 13/14/18. Full physical desktop acceptance remains deferred.
- [x] Add `IgnorePointer` after RepaintBoundary (Basic order 160) with both optional
  boolean constructor fields: ignoring (SDK true) and deprecated ignoringSemantics
  (SDK null), plus optional child and managed key. Keep false distinct from unset
  in the centered checkbox editors and document all three semantics modes. Render
  real pointer pass-through without changing layout/paint, retain the SDK semantics
  matrix and separate Designer selection/drop controls. Cover typed validation,
  generation, Palette/tree/Canvas placement and moves, Slots replace/clear,
  Save/reopen/further edits, Undo/Redo, rollback and four SVGs. Current surface:
  55 widgets, 49 const definitions, 761 rows (744 outside Scaffold), 50 any-widget
  plus two trait destinations, 2,860 cells (2,608 accepted / 252 rejected), Basic 16.
  Historical target 55/92 with 37 remaining; schema/API/model remain 13/14/18.
  Full physical desktop acceptance remains deferred.
- [ ] Pass the complete runner, persistence and cross-platform release gate.
  - [x] Pass assembled-Windows runner crash/Retry, authenticated natural close,
    every captured runner-generation cleanup and runner/`FLUTTERVIEW`
    handle-cleanup acceptance.
  - [x] Pass the pair Save and Undo/Redo persistence matrix through both the
    focused coordinator suite and the packaged NetBeans DataObject: stage C1,
    Save C1, Undo to B, Redo C1, Undo B, Save B, then prove the retained
    `Redo C1` is dirty/`STAGED_PAIR` without changing durable B and the final
    `Undo B` returns to the clean savepoint with no `SaveCookie`.
  - [ ] Pass the remaining Windows interaction acceptance above independently
    of persistence: physical mixed-DPI movement, physical CJK IME and broader
    native menu/popup paths beyond the accepted Preview selector and standard
    **Window → Services** menu. Two simultaneous surfaces plus bounded tab and
    Split Document heavyweight-peer teardown/recreation now pass the physical
    runtime gate.
  - [ ] Implement and verify the Linux and macOS SPI providers.
- [ ] Continue admitting the practical post-core backlog only as complete
  vertical slices. The current catalog presents all fifty-five admitted built-ins.
  The 53 definitions with scalar fields expose typed Properties; IntrinsicHeight
  and RepaintBoundary have structural child-slot editors. This does not
  imply Create, Canvas, DnD or Properties capability for any unreviewed widget.
- [x] Establish the project-wide theme foundation outside `.fd`: canonical
  schema-v1/v2/v3/v4 `.fd_templates/project.fdtheme`, hash-guarded generated
  `lib/theme/app_theme.dart`, default light/dark Material seed themes,
  transactional new-project wiring, a docked `Themes` editor with custom-theme
  CRUD plus project-wide and per-theme enable/disable switches, and Canvas
  payload-v4 synchronization with live refresh and fail-closed conflicts.
- [x] Extend project theme schema/editor beyond seed-derived ColorSchemes with
  typed per-role `ColorScheme` overrides and per-role `TextTheme`/`TextStyle`
  overrides. Schema v4 and the `General`/`Colors`/`Typography` editor cover 46
  ColorScheme roles and 15 TextTheme roles with 13 typed fields per text role.
  Generated Dart and Canvas use the same ordered `ColorScheme.copyWith` and
  `TextTheme.copyWith` construction; form-local Text leaves remain later
  overrides.
- [ ] Add typed component, shape and extension contracts without arbitrary
  Dart-expression escape hatches.
  - [x] First add schema-v5 Component Colors as one closed 36-leaf slice:
    Scaffold background, four AppBar colors, global Icon color, and six
    ElevatedButton colors across default/disabled/pressed/hovered/focused
    states. Reuse literal/semantic `FlutterThemeColorValue`, migrate v4 to empty
    components without changing legacy generated Dart bytes, bump the strict
    Canvas model protocol from 8 to 9, and preserve local widget value →
    component theme → framework precedence.
  - [ ] Specify shapes as a separate closed project-owned algebra with target
    validation, and choose a plugin-owned generated Dart type/API before
    admitting any `ThemeExtension` value.
  - [ ] Define explicit precedence for a theme Paint versus a local shorthand
    color before theme-level foreground or background Paint becomes writable.

## M5 — bidirectional RAD (later milestone)

- [ ] Dart AST ↔ designer model mapping
- [ ] Safe source rewriting
- [ ] Custom widget discovery
- [x] Typed seed/ColorScheme/TextTheme project-theme-aware preview
- [ ] Typed component/shape/extension ThemeData preview
- [ ] Multi-device preview
