# Release verification tools

`verify-release.ps1` performs a read-only release check. It does not build,
install, delete, or modify the repository, the NBM, or a NetBeans user
directory.

Run it after `mvn clean install`:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1
```

By default the script reads the version from the root `pom.xml` and verifies
`netbeans-plugin/target/netbeans-flutter-plugin-<version>.nbm`. It exits with code `1`
if any release contract fails and prints the artifact SHA-256 on success or
failure.

The checks cover:

- the NBM and `Info/info.xml`, including distribution name, code name, product
  name, Flutter category, specification/implementation versions, and the full
  Apache 2.0 license;
- the module configuration and module JAR entries inside the NBM;
- every Maven `*Test.java`/`*IT.java` source having a corresponding
  Surefire/Failsafe XML report, zero recorded failures/errors, only explicitly
  classified SDK-backed, Web Canvas, or platform-filesystem probes being
  skipped, and a passing `PluginPackageMetadataIT` report;
- the SHA-256, optionally against a previously recorded expected value;
- optionally, an isolated installed NetBeans userdir: enabled module config,
  update tracking and its files, installed JAR versions, a NetBeans 30 log,
  the module activation/version line, and absence of `SEVERE`,
  `Unexpected Exception`, `LinkageError`, `NoClassDefFoundError`, and
  `ClassNotFoundException`.

Verify an installation smoke userdir as part of the same check:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1 `
  -InstalledUserdir target/nbm-install-smoke-userdir-0.1.2
```

Require the optional real Flutter, Dart, and Android SDK tests to have run
instead of being skipped. This includes Flutter project creation, real Dart
analysis and assembled-NetBeans editor E2E, Android AVD discovery, the pinned
Material icon font preview, the packaged Canvas runner build smoke, and the
physical Windows Canvas acceptance. The physical gate also requires a Windows
desktop host; without either prerequisite its report is skipped and strict SDK
verification fails:

```powershell
mvn clean install `
  "-Dflutter.it.sdk=<Flutter SDK root>" `
  "-Ddart.executable=<absolute path to Dart executable>" `
  "-Dandroid.sdk.integration=<Android SDK root>" `
  "-Dmaterial.icon.preview.flutter.sdk=<Flutter SDK root>" `
  "-Dcanvas.runner.flutter.sdk=<Flutter SDK root>" `
  "-Dcanvas.runner.acceptance.flutter.sdk=<Flutter SDK root>" `
  "-Dnetbeans.runtime.it.fork.timeout.seconds=900"

pwsh -NoProfile -File tools/verify-release.ps1 -RequireOptionalSdkTests
```

The SDK properties belong to the Maven run that produces the XML reports. The
verifier is read-only and validates that the two Canvas gate cases and the
pinned Material icon font case are present in those reports, and that no
SDK-gated case skipped.

Web Canvas prerequisites are classified independently from the general SDK and
filesystem probes. By default their exact known skips are reported but allowed.
Use `-RequireOptionalWebCanvasTests` only for a Windows x64 release-evidence run
that has all of the following prerequisites:

- a prepared Flutter SDK passed as
  `-Dweb.canvas.flutter.sdk=<Flutter SDK root>` for the packaged exact-Web build;
- an already built runner source root passed as
  `-Dcanvas.runner.web.source=<absolute runner source root>`; that root must
  contain the validated `build/web` output;
- Microsoft Edge WebView2 Runtime installed and the physical probe enabled with
  `-Dnetbeans.flutter.webview2.physical=true`;
- an interactive Windows environment whose filesystem permits the tested
  symbolic-link/reparse-point and stable-handle probes.

The strict switch requires every exact Web Canvas gate case to be present in the
Maven XML reports and to have run without a skip. It covers the real build and
artifact contract, packaged JNA/installed-Runtime probe, and the Web publication,
SDK metadata, user-data and Windows deletion safety probes. For example, after
building the exact runner artifact:

```powershell
mvn clean install `
  "-Dweb.canvas.flutter.sdk=<Flutter SDK root>" `
  "-Dcanvas.runner.web.source=<absolute runner source root>" `
  "-Dnetbeans.flutter.webview2.physical=true"

pwsh -NoProfile -File tools/verify-release.ps1 `
  -RequireOptionalWebCanvasTests
```

This verifies the currently recorded prerequisite and physical-probe evidence;
it does not turn the standalone WebView2 host smoke into an assembled NetBeans
exact-Web product acceptance gate.

`FlutterDesignerPairCopyTest` has two read-only filesystem probes whose native
permission behavior is not an SDK prerequisite. They are reported separately
as optional platform-dependent skips. Require those probes too when producing
platform-specific release evidence; strict platform mode requires both exact
test cases to be present in the Maven XML reports and to have run without a
skip:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1 `
  -RequireOptionalSdkTests `
  -RequireOptionalWebCanvasTests `
  -RequireOptionalPlatformTests
```

Pin the artifact to a separately recorded checksum:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1 `
  -ExpectedSha256 '<64 hexadecimal characters>'
```

To audit an older artifact after the checkout has advanced, provide its
version and path. `-SkipFreshnessCheck` is intentionally required in this
case because current source timestamps and newly added test classes no longer
describe that artifact. Report totals, failures/errors, allowed skips, and the
mandatory package metadata integration test are still verified:

An explicitly supplied `-NbmPath` and `-Version` different from the checkout's
version may retain the historical `netbeans-plugin-<version>.nbm` name. Current
builds must use `netbeans-flutter-plugin-<version>.nbm`; there is no automatic
fallback to a stale legacy-named file. Maven repository coordinates and internal
NetBeans module filenames remain unchanged.

```powershell
pwsh -NoProfile -File tools/verify-release.ps1 `
  -Version 0.1.1 `
  -NbmPath netbeans-plugin/target/netbeans-plugin-0.1.1.nbm `
  -InstalledUserdir target/nbm-install-smoke-userdir-6 `
  -SkipFreshnessCheck
```

The verifier tests use Pester and synthetic archives/userdirs under Pester's
temporary test drive:

```powershell
Invoke-Pester tools/tests/verify-release.Tests.ps1
```

## Isolated NetBeans 30 install and upgrade smoke

`smoke-netbeans30-nbm.ps1` exercises the real NetBeans command-line module
service. It always creates a new userdir, cache, immutable local update-center
catalog, and command evidence below the repository `target` directory. It does
not write to the NetBeans installation or the user's normal NetBeans userdir.
Custom paths are rejected if any existing component below the repository root
is a junction, symbolic link, or other reparse point. The same isolated cache is
passed to the host and every secondary CLI command. Every NetBeans process
stopped by the runner must have an exact parsed `--userdir` or
`-Dnetbeans.user` value, be a validated descendant of the recorded launcher,
and retain the same executable and start identity immediately before it is
stopped. Child termination uses the same retained process handle that passed
identity validation, and cleanup refuses to continue if the recorded launcher
PID has been reused. The child sweep runs in a `finally` block even if the
launcher exits during cleanup. An atomically created owner sentinel is held
with exclusive sharing for the full run, so concurrent invocations cannot
share an explicitly selected probe root.
Before starting any secondary NetBeans CLI process, readiness waits for the
primary launcher to publish the four-byte server-port marker in the isolated
userdir lock. This prevents the readiness command from winning the native
single-instance startup race. Offline payload removal also requires the exact
identity-validated handle of that userdir's stopped host.
The probe userdir masks the three bundled update centers so installation and
upgrade resolve only from the staged local catalog and do not depend on the
network. The clean scenario also creates deterministic offline Flutter and
Dart SDK stubs plus a minimal Flutter project. A separate userdir, which never
opens that project, exercises the NetBeans 30 direct-disable transition and
uninstall cleanup without racing an active project lifecycle. Direct disable
is used because the standard headless CLI disable leaves this active,
non-reloadable module enabled. All launcher processes force Java headless mode,
so an unexpected userdir-lock condition fails the smoke instead of displaying
a desktop dialog.

Build the NBM, then run a clean-install smoke against Apache NetBeans 30:

```powershell
pwsh -NoProfile -File tools/smoke-netbeans30-nbm.ps1 `
  -NetBeansHome G:/netbeans
```

The current NBM defaults to
`netbeans-plugin/target/netbeans-flutter-plugin-<root-pom-version>.nbm`. Override it for
an explicitly selected artifact:

```powershell
pwsh -NoProfile -File tools/smoke-netbeans30-nbm.ps1 `
  -NetBeansHome G:/netbeans `
  -CurrentNbmPath artifacts/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
```

Add `-PreviousNbmPath` to run two independent scenarios: a clean installation
of the current NBM and an upgrade from the previous NBM to the current one.
For example, with the locally installed 0.1.1 release artifact:

```powershell
pwsh -NoProfile -File tools/smoke-netbeans30-nbm.ps1 `
  -NetBeansHome G:/netbeans `
  -PreviousNbmPath C:/Users/vhadmin/.m2/repository/dev/flutter/netbeans/netbeans-plugin/0.1.1/netbeans-plugin-0.1.1.nbm
```

`NETBEANS_HOME` can be used instead of `-NetBeansHome`. A custom `-ProbeRoot`
is accepted only when it is a new strict descendant of this repository's
`target` directory; existing paths are never deleted or reused.

Success requires all of the following:

- the secondary `--modules --list` service becomes ready and reports the exact
  module specification version as `Enabled`; bounded host-start retries use a
  fresh, exclusively validated isolated-userdir lock;
- `--install` or `--update` positively reports the requested module/version;
- CLI stderr is empty or contains only the four explicitly allowlisted JDK
  missing-package warnings emitted by NetBeans 30; any other stderr line fails
  the scenario (the Windows launcher `-252` connected status remains
  informational);
- module config is enabled, update tracking identifies the staged update
  catalog and exactly lists the NBM payload, every immutable installed payload
  file has the same length and SHA-256 as the NBM, updater-owned tracking is
  validated semantically, obsolete previous-version payload is absent after an
  upgrade, and the installed JAR manifest matches the metadata;
- the clean-install scenario proves current-version activation in Apache
  NetBeans IDE 30, exact auto-discovered standalone Flutter/Dart SDK settings,
  and the standard NetBeans reopen record for the isolated Flutter project;
- a separate project-free userdir proves `Enabled` to `Installed` disable,
  requires every unrelated module name, version, and state to remain unchanged,
  then removes only the exact tracked current payload, updater backup copies,
  and tracking file while retaining SDK preferences and unrelated files; a
  fresh-cache restart must no longer report the Flutter module code name;
- the upgrade scenario first proves previous-version activation, then stops
  NetBeans and verifies the persisted current NBM payload, update tracking,
  catalog origin, JAR metadata, and removal of obsolete previous-version files
  offline; current-version activation is independently covered by the clean
  scenario;
- every retained `messages.log*` session is free of the configured critical
  exception/linkage patterns;
- no process remains for the dedicated probe userdir.

The runner leaves its target-only userdirs, staged catalogs, and captured
command output in place as release evidence. Its helper tests require no real
NetBeans installation or network access:

```powershell
Invoke-Pester tools/tests/smoke-netbeans30-nbm.Tests.ps1
```
