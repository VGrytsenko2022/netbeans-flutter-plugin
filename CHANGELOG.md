# Changelog

All notable changes to the NetBeans Flutter plugin are documented in this file.

## [Unreleased]

### Added

- Deterministic controller coverage and lifecycle hardening for Run and Debug startup, cancellation, confirmed restart, Stop, process exit, project close, stale completions, and exactly-once progress cleanup.
- DevTools lifecycle coverage for connected-URL reuse, explicit Stop, owning Flutter-session termination or replacement, project close, process failure, and suppression of stale callbacks and Output writes.
- An assembled NetBeans 30 runtime integration gate for module activation, Dart MIME/editor/LSP/DAP registrations, and Flutter action resolution.
- A strict release verifier for fresh Surefire/Failsafe results, package metadata and licensing, NBM/update-catalog hashes, installed update tracking, and activation logs.
- A headless isolated NetBeans 30 smoke runner: clean-install activation of `0.1.2`, activated `0.1.1` baseline and offline exact-payload verification after updating to `0.1.2`, with strict process ownership and cleanup checks.

### Changed

- Started the 0.1.2 stability milestone with automated lifecycle and release verification gates for NetBeans 30.
- The Dart Analysis Server client version is now checked against the Maven project version during every test run.

## [0.1.1] - 2026-08-24

### Added

- Project-scoped Flutter DevTools launch through `Flutter > Open DevTools` for an active Run or Debug session with an available VM Service URI.
- `Flutter > Stop DevTools`, a dedicated DevTools Output tab, native cancellable progress, and automatic server cleanup when the owning Flutter session stops or is replaced or its project closes.
- Reuse of an already running DevTools server: invoking Open DevTools again reopens its connected URL instead of starting a duplicate process.
- Complete Apache License 2.0 and NetBeans plugin metadata in the generated NBM package, with integration checks for the packaged metadata and license.

### Changed

- DevTools is started from the configured Dart SDK on loopback with an automatically assigned port and is opened through the browser configured in NetBeans.
- Flutter project command resolution now respects the project owning the active file or editor before falling back to the main or sole open project.

## [0.1.0] - 2026-08-24

### Added

- Initial Apache NetBeans 30 support for Flutter/Dart SDK discovery and configuration, Flutter project creation and recognition, and Dart-class creation.
- Dart syntax highlighting, typing indentation, Analysis Server diagnostics, completion, navigation, refactoring, formatting, Quick Fixes, and import assistance.
- Desktop, Mobile, and Web target selection, Android Device Manager, configured-emulator launch, managed Flutter Run/Debug sessions, DAP debugging, Hot Reload, Hot Restart, and Stop.
- Native NetBeans integration for Pub Get, Analyze, Flutter tests/Test Results, and `pubspec.yaml` completion and diagnostics.
