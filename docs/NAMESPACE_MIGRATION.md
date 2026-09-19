# Maven and Java namespace migration

The project uses `io.github.vgrytsenko2022` for both Maven coordinates and Java
packages. The reactor parent is `io.github.vgrytsenko2022:netbeans-flutter-plugin:0.1.3`;
the installable module remains the `netbeans-plugin` artifact. Its distributable
filename is `netbeans-flutter-plugin-0.1.3.nbm`.

This change follows the complete Chip implementation, committed separately as
`3f17426`. It does not add a new palette widget or change the FD schema version.

## Scope

- All ten reactor modules inherit the new parent coordinates and use the new
  group for internal dependencies. Runtime-test classpath exclusions and NBM
  exported package declarations use the same namespace.
- Java packages/imports, source and resource directories, annotation-generated
  actions/services, layers, bundle paths, reflection targets, icons and packaged
  Canvas resources use `io/github/vgrytsenko2022`.
- NBM metadata, runtime fixture expectations and PowerShell release/smoke tools
  use the new module identity `io.github.vgrytsenko2022.netbeans.plugin`.
- Native/Dart palette-drop channel names agree; runner source manifests and the
  release Web artifact hashes are refreshed after rebuilding.
- Tests check source-directory/package agreement for every module and reject
  old Java/resource entries inside packaged runtime JARs.

## Existing data and upgrade boundary

Java package names are not persistent document format identities. The existing
`urn:dev.flutter.netbeans:project-metadata:1` and
`urn:dev.flutter.netbeans:move-handoff:1` XML namespaces intentionally remain
unchanged. Existing `.fd` files and generated Dart do not need a namespace rewrite.

The new module imports known SDK and legacy global run-target preferences once
from the old module preference node. Existing new-namespace choices take
precedence; old values are never deleted, unknown keys are not copied, and a
later reset does not resurrect old choices. Private project run-target settings
are similarly imported from old auxiliary properties/configuration. Shared
project settings are not imported as private device choices.

The NetBeans code-name base also changed when the Maven group was changed.
NetBeans therefore treats the old and new code-name bases as distinct modules,
not an ordinary same-module upgrade. Do not enable both at once: use a separate
test profile, or remove the old Flutter plugin through Plugin Manager before
installing the renamed one. No installed IDE/userdir is modified by this refactor.
External Java SPI contributors must rebuild against the new exported packages;
there is no binary alias layer for the old classes. Customized action shortcuts
that reference old action class names may need to be reassigned.

## Build discipline

Old generated classes, test reports, NBM staging and release outputs were moved
to `target/pre-namespace-20260919/` before rebuilding. This prevents old classes
from hiding missing registrations and preserves previous outputs; IDE userdirs
were not moved or cleared. Previously signed artifacts remain historical, not
signatures for the newly built JARs/NBM.

Normal verification does not activate `central-release`, sign or publish artifacts.
The Maven NetBeans API baseline remains the user's current `RELEASE300`; this
namespace change does not independently upgrade the API baseline or installed IDE.

## Verification receipt (2026-09-19)

- Fresh-output full Java reactor run plus focused reruns after correcting the
  source manifest and a new test's filesystem fixture: 6,367 current test
  results, 279 environment-gated skips, zero failures/errors.
- Real-SDK Chip gate after the package rename: 460 generated/native cases,
  all 50 typed fields and 42 unsafe/wrong source cases rejected.
- All ten NBM metadata/package checks passed, including absence of stale
  Java/resource namespaces inside runtime JARs.
- Four isolated NetBeans runtime gates passed: module registrations, external
  catalog contributor loading, Designer DataObject integration and settings /
  project lifecycle. This is not a manual test of the user's installed IDE.
- 66 PowerShell release/smoke-helper tests passed. Flutter analysis had no
  issues; 150 targeted Canvas runtime, icon-registry and Chip tests passed.
- Release Web Canvas rebuilt; `main.dart.js` is 4,909,331 bytes, SHA-256
  `e1caa4cefa26c08351ebb11105d527493e5d2a979be523b86b757e37b36544d7`.
  Source extraction and the real Web artifact contract checks passed with the
  refreshed manifests.
- Final reactor `install` and `nbm:cluster` succeeded. All eight internal JARs
  in the NBM match the development cluster. The seven library JARs match their
  freshly built targets; the main-module payload matches apart from the normal
  NBM conversion of Maven classpath manifest attributes.
- NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3.nbm`, 9,476,856 bytes,
  SHA-256 `cb7a67f73a3288f843ce04a2b707e5b039a4714d9ba94bdce8a301695c6d3d5e`.

No artifact was signed, published or installed into the user's IDE.
