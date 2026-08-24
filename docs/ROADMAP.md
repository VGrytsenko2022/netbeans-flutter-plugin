# Roadmap

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
- [ ] Embedded DevTools panel
- [ ] Flutter Inspector / widget tree
- [ ] Direct Dart VM Service tooling beyond the implemented DAP handoff

## M4 — Designer foundation

- [ ] Native Flutter Engine canvas
- [ ] Widget metadata catalog
- [ ] Widget tree model
- [ ] Palette and Properties
- [ ] Selection/hit testing
- [ ] Drag & drop
- [ ] Layout guides

## M5 — bidirectional RAD

- [ ] Dart AST ↔ designer model mapping
- [ ] Safe source rewriting
- [ ] Custom widget discovery
- [ ] Theme-aware preview
- [ ] Multi-device preview
