# Release verification tools

`verify-release.ps1` performs a read-only release check. It does not build,
install, delete, or modify the repository, the NBM, or a NetBeans user
directory.

Run it after `mvn clean install`:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1
```

By default the script reads the version from the root `pom.xml` and verifies
`netbeans-plugin/target/netbeans-plugin-<version>.nbm`. It exits with code `1`
if any release contract fails and prints the artifact SHA-256 on success or
failure.

The checks cover:

- the NBM and `Info/info.xml`, including distribution name, code name, product
  name, Flutter category, specification/implementation versions, and the full
  Apache 2.0 license;
- the module configuration and module JAR entries inside the NBM;
- every Maven `*Test.java`/`*IT.java` source having a corresponding
  Surefire/Failsafe XML report, zero recorded failures/errors, only the four
  known optional real-SDK tests being skipped, and a passing
  `PluginPackageMetadataIT` report;
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
instead of being skipped. This also requires the assembled-NetBeans Dart
editor E2E test, which uses the configured `dart.executable`:

```powershell
pwsh -NoProfile -File tools/verify-release.ps1 -RequireOptionalSdkTests
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
The probe userdir masks the three bundled update centers so installation and
upgrade resolve only from the staged local catalog and do not depend on the
network. All launcher processes also force Java headless mode, so an
unexpected userdir-lock condition fails the smoke instead of displaying a
desktop dialog.

Build the NBM, then run a clean-install smoke against Apache NetBeans 30:

```powershell
pwsh -NoProfile -File tools/smoke-netbeans30-nbm.ps1 `
  -NetBeansHome G:/netbeans
```

The current NBM defaults to
`netbeans-plugin/target/netbeans-plugin-<root-pom-version>.nbm`. Override it for
an explicitly selected artifact:

```powershell
pwsh -NoProfile -File tools/smoke-netbeans30-nbm.ps1 `
  -NetBeansHome G:/netbeans `
  -CurrentNbmPath artifacts/netbeans-plugin-0.1.2.nbm
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
  catalog and exactly lists the NBM payload, every installed payload file has
  the same length and SHA-256 as the NBM, obsolete previous-version payload is
  absent after an upgrade, and the installed JAR manifest matches the metadata;
- the clean-install scenario proves current-version activation in Apache
  NetBeans IDE 30 and a clean `messages.log`;
- the upgrade scenario first proves previous-version activation, then stops
  NetBeans and verifies the persisted current NBM payload, update tracking,
  catalog origin, JAR metadata, and removal of obsolete previous-version files
  offline; current-version activation is independently covered by the clean
  scenario;
- no process remains for the dedicated probe userdir.

The runner leaves its target-only userdirs, staged catalogs, and captured
command output in place as release evidence. Its helper tests require no real
NetBeans installation or network access:

```powershell
Invoke-Pester tools/tests/smoke-netbeans30-nbm.Tests.ps1
```
