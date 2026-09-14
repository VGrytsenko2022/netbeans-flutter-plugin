# DeviceOrientationBuilder vertical slice

Reviewed against the pinned Flutter 3.44.8 SDK in
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native API and placement projections

[Constructor](https://api.flutter.dev/flutter/widgets/DeviceOrientationBuilder/DeviceOrientationBuilder.html):
const DeviceOrientationBuilder({Key? key, required OrientationWidgetBuilder builder}).
The exact callback is Widget Function(BuildContext, Orientation). Shared identity
supplies Key; there are no native Events, named constructors or Child slots.

Unlike OrientationBuilder, the pinned native implementation builds immediately
from MediaQuery.orientationOf(context), not parent layout constraints. The nearest
MediaQuery wins; changes to orientation rebuild its dependent subtree. Parent
dimensions may be bounded, square or unbounded without selecting the orientation.
It requires MediaQuery, normally supplied by the application. Build-time
construction permits intrinsic/dry layout if the returned box supports it.

The callback can return either a box or a sliver. Both are supported through
explicit, fixed Designer placement projections:

- flutter.widgets.DeviceOrientationBuilder: Layout order 270, box placement.
  Empty generates (context, orientation) => const SizedBox.shrink().
- flutter.widgets.DeviceOrientationBuilder.sliver: Scrolling order 470,
  labeled DeviceOrientationBuilder (sliver), sliver placement.
  Empty generates (context, orientation) => const SliverToBoxAdapter().

Both generate the same unnamed DeviceOrientationBuilder constructor. The .sliver
catalog ID is not an SDK constructor or a generated property. A fixed projection
lets Java validation and native Canvas select the correct rendering protocol
without executing arbitrary user code. Box slots/root reject the sliver projection;
sliver slots reject the box projection. Neither owns a synthetic Child slot.
The result of a custom builder must match its selected parent protocol.
Widget return typing alone cannot statically prove RenderBox versus RenderSliver.

## Editor, source proof and lifecycle

Each projection exposes one required Builder row: Empty or a strict typed project
function/getter/member/zero-argument factory, including declared-package members.
Empty is the Designer creation choice, not a native SDK default. Missing/null,
raw code, unreviewed strings and legacy event callback values are rejected.
Use Empty to clear a binding; Restore Default cannot omit a required argument.

The shared editor preserves writable row/group identity through refresh/reopen.
Both callable descriptors are BUILDER with exact native parameters and imports.
Source analysis reuses the existing OrientationWidgetBuilder witness with fixed
SDK BuildContext and Orientation types, strict initialization and an original
call-result check. Wrong signatures and nullable/dynamic references/results are
rejected; proof code is neither executed nor persisted in user source.
Add/edit/move, reset-to-empty, Save/reopen and Undo/Redo retain identity, imports,
source offsets and user-owned members. Rejected analysis leaves source bytes intact.

## Canvas and icons

Both paths mount a real DeviceOrientationBuilder with a preview-owned callback.
No project function/getter/factory is executed. Custom binding presence carries
an explicit tooltip/semantics warning that its content is unavailable; application
Dart retains the actual reference. Native result sizes are not faked.

Box selection uses tight geometry or the shared zero-size handle. Sliver zero
extent remains selectable through its overlay; sibling insertion targets a real
parent sliver slot, never a fabricated builder child. Mobile/tablet profile
rotation updates native MediaQuery without replacing the builder's element.
Eight reviewed SVG resources cover distinct box/sliver icons in light/dark, 16/32.

## Inventory

166 catalog definitions (the two additions represent one native constructor):
158 scalar and eight structural; 6934 writable rows, 6916 outside Scaffold;
136 const-capable definitions. Layout has 35 entries, Material 49, Scrolling 47.
The 157 ordinary destinations produce 26062 candidates: 17477 accepted, 8585 rejected.
Required wrappers remain 29, including 22 root-compatible box wrappers.
Boolean-only rows remain 631 and nullable-boolean unions 50.
Native Events remain 154 across 40 types; all callables total 205 across 58 catalog
types: 37 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Verification

- Full Designer core: 2354 tests, zero failures/errors, 20 conditional skips.
- Thirty dedicated Canvas tests exercise both protocols, closed decoding, inert
  references, zero-size selection, normal parent insertion, tight/unbounded sizes,
  RTL/reverse/scroll axes and retained identity. Mobile/tablet profile rotation
  updates the nearest native MediaQuery through portrait, landscape and square
  dimensions for both projections.
- Full Flutter regression: 5085 tests passed. Flutter analyze: no issues.
  Release Web build passed with --no-web-resources-cdn and --no-wasm-dry-run.
- Both real-SDK gates passed unskipped: box in 39.610 seconds and sliver in
  59.814 seconds. Eighteen generated forms run 74 native Flutter tests:
  exact function/getter/factory/package-member source evidence; nullable/dynamic/
  wrong-signature rejection; add/move/reset, Save/reopen, Undo/Redo; LTR/RTL,
  light/dark and retained elements on MediaQuery/inherited updates.
  Box cases verify nearest MediaQuery against 36 conflicting bounded/square/
  unbounded parent combinations, intrinsic and dry layout. Sliver cases verify
  both scroll axes and reverse. Missing MediaQuery is tested without the test
  framework's default View wrapper. Wrong-protocol results fail at runtime as
  expected rather than being falsely accepted as render-safe by Dart type proof.
- Cross-module Java selection: 1483 tests across 133 report files, six conditional
  skips, final reports with zero failures/errors. The first run caught two test
  expectations (the accessibility palette list and the unchanged local TextField
  property count). Focused rechecks passed after correcting them: 238 tests for
  accessibility/palette/icons/new editors, then six TextField tests. These totals
  overlap the selection/core runs and must not be added together.
- CanvasRunnerSourceBundleTest and all 24 WebCanvasArtifactContractTest cases pass,
  including the explicitly enabled real Web artifact gate. All exact source/Web
  manifest sizes and hashes are regenerated without weakening integrity checks.
  Optional SDK gates are skipped in the general selection and passed separately
  above. The entire Java reactor and interactive desktop IDE were not exercised.

## Package

Maven install and nbm:cluster both exited zero. The packaged schema, catalog,
generator, callback metadata, property/palette/icon classes and shared analyzer
witnesses match the current compiled classes. All eight SVGs and 40 packaged
runner sources match the checkout and pinned manifest. The packaged Web manifest
matches the checkout; all 35 release Web files match its exact sizes and hashes.
All four development-cluster JARs are byte-identical to their NBM counterparts.

- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
- Size: 8806344 bytes
- SHA-256: 6a7df312597bbfa05f3e86dad9e9c10693aa1b0e6583db92a272990c45bea288
- Built: 2026-09-13T11:27:38.6039729Z

No manual desktop test or IDE restart is claimed. CJK IME and Linux/macOS Canvas
providers remain deferred. The unrelated existing JNA DPI test is outside this slice.

Next candidate: [ListenableBuilder](https://api.flutter.dev/flutter/widgets/ListenableBuilder/ListenableBuilder.html).
