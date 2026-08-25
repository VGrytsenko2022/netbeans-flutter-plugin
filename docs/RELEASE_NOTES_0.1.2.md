# Flutter and Dart Support 0.1.2

Release date: 2026-08-25

Version 0.1.2 hardens the plugin's NetBeans 30 lifecycle integration, adds native build actions, and removes the known plugin-owned warning sources without reducing Dart or Flutter functionality.

## Compatibility

- Apache NetBeans 30
- JDK 21 or newer
- Maven 3.9 or newer when building from source
- A separately installed Flutter SDK; its bundled Dart SDK is supported, as is an explicitly configured standalone Dart SDK

## Highlights

### Native project lifecycle

Flutter Run, Debug, tooling, target discovery, progress, cancellation, restart, project close, and DevTools ownership now use explicit project-scoped lifecycle contracts. The standard NetBeans Build, Clean, and Clean and Build actions follow the selected Desktop, Mobile, or Web target.

Flutter projects now own their `AuxiliaryConfiguration` and `AuxiliaryProperties` storage. Private IDE state is consolidated into a transient slash-free attribute, preventing NetBeans 30 from interpreting auxiliary metadata as filesystem ordering rules. Legacy state is migrated or quarantined without silently discarding malformed values.

Move and Rename preserve private state through a bounded, explicitly typed, append-only transaction. Preference data is flushed before capture, phase files are read back and verified, and source deletion waits for a verified target. Recovery uses real-path containment and a DTD-free bounded streaming parser. An ambiguous interrupted Move remains blocked until Rename supplies the missing target name; a committed transaction is cleanup-only and cannot replay older settings.

### Dart and NetBeans warning cleanup

The LSP bridge consumes only Dart's id-less `$/analyzerStatus` notification that the generic NetBeans 30 client cannot handle. Diagnostics, requests, responses, and unrelated custom traffic continue unchanged. MIME services no longer publish meaningless shared layer positions, removing the corresponding duplicate and partial ordering warnings.

### Release verification

The Maven reactor includes an assembled NetBeans 30 runtime gate for module activation and Dart/Flutter registrations. Release tooling verifies fresh test reports, NBM metadata and licensing, update-catalog hashes, installed payload tracking, project reopen state, disable/uninstall cleanup, and plugin-owned warning patterns in current and retained NetBeans logs.

The environment-independent release suite is automated. Real-SDK editor checks and the physical-device/AVD/iOS matrix remain environment-dependent validation steps and should be recorded before broad binary distribution.

## Build the release package

From the repository root, run:

```powershell
mvn clean verify
```

The stable release package is generated at:

```text
netbeans-plugin/target/netbeans-plugin-0.1.2.nbm
```

## Install in NetBeans 30

1. Open `Tools > Plugins`.
2. Select `Downloaded` and choose `Add Plugins...`.
3. Select `netbeans-plugin-0.1.2.nbm` and complete the installer.
4. Open `Tools > Options > Flutter` and validate the detected or manually selected Flutter and Dart SDKs.

For the full feature history, see [CHANGELOG.md](../CHANGELOG.md). For development setup and workflows, see [Getting Started](GETTING_STARTED.md).
