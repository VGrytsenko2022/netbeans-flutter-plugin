# Flutter and Dart Support 0.1.1

Release date: 2026-08-24

Version 0.1.1 adds lifecycle-managed Flutter DevTools integration and completes the metadata and licensing of the distributable NetBeans plugin package.

## Compatibility

- Apache NetBeans 30
- JDK 21 or newer
- Maven 3.9 or newer when building from source
- A separately installed Flutter SDK; its bundled Dart SDK is supported, as is an explicitly configured standalone Dart SDK

## Highlights

### Flutter DevTools

With a Flutter application running or debugging and its VM Service URI available, choose `Flutter > Open DevTools`. The plugin starts the DevTools version supplied by the configured Dart SDK on `127.0.0.1` with an automatically assigned port, connects it to the exact active application, and opens the connected URL in the browser configured in NetBeans.

The server has a dedicated `Flutter DevTools: <project>` Output tab and a native cancellable progress indicator. Invoking Open DevTools again reopens the current URL instead of launching a second server. `Flutter > Stop DevTools` stops only DevTools; stopping or replacing the owning Flutter session or closing its project stops the server automatically. Hot Reload and Hot Restart preserve it.

This release provides the SDK-hosted browser DevTools. An embedded NetBeans DevTools panel, Flutter Inspector, and native widget tree remain future work.

### Plugin packaging

The NBM includes the plugin display metadata and Apache License 2.0 information required by the NetBeans plugin installer. Package integration checks verify those release artifacts during the Maven build.

## Build the release package

From the repository root, run:

```powershell
mvn clean install
```

The stable release package is generated at:

```text
netbeans-plugin/target/netbeans-plugin-0.1.1.nbm
```

## Install in NetBeans 30

1. Open `Tools > Plugins`.
2. Select the `Downloaded` tab and choose `Add Plugins...`.
3. Select `netbeans-plugin-0.1.1.nbm` and complete the installer.
4. Open `Tools > Options > Flutter` and validate the detected or manually selected Flutter and Dart SDKs.

For the full feature history, see [CHANGELOG.md](../CHANGELOG.md). For development setup and workflows, see [Getting Started](GETTING_STARTED.md).
