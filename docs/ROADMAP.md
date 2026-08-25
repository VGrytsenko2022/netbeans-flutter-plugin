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
- [ ] Run and record a mobile compatibility matrix for Android physical devices and AVDs plus iOS simulators where macOS is available, covering discovery, target selection, Run, Debug, Hot Reload, Hot Restart, Stop, and DevTools lifecycle.
- [ ] Require the complete Maven/runtime-registration gates, strict release verifier, isolated clean-install/update smoke, functional editor E2E gate, persisted-settings/project-reopen/disable/uninstall smoke, and recorded mobile matrix before tagging `0.1.2`.

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
- [x] Create a basic Flutter application from the New Project wizard
- [x] `New File > Dart > Dart Class` wizard with project-aware location and naming
- [x] Device selector in the standard NetBeans project-configuration toolbar combo
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

## M4 — Designer foundation (postponed beyond 0.1.2)

Designer implementation remains out of scope until the 0.1.2 stability gates are complete.

- [ ] Native Flutter Engine canvas
- [ ] Widget metadata catalog
- [ ] Widget tree model
- [ ] Palette and Properties
- [ ] Selection/hit testing
- [ ] Drag & drop
- [ ] Layout guides

## M5 — bidirectional RAD (postponed beyond 0.1.2)

- [ ] Dart AST ↔ designer model mapping
- [ ] Safe source rewriting
- [ ] Custom widget discovery
- [ ] Theme-aware preview
- [ ] Multi-device preview
