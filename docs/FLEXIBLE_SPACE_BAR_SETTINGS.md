# FlexibleSpaceBarSettings (Flutter 3.44.8)

## Native constructor and explicit scope

All six constructor values and the required Child are admitted for the existing
Designer box-wrapper model. Stable Key remains Designer-managed. Native references:
[constructor](https://api.flutter.dev/flutter/material/FlexibleSpaceBarSettings/FlexibleSpaceBarSettings.html)
and [class](https://api.flutter.dev/flutter/material/FlexibleSpaceBarSettings-class.html);
verified against the installed Flutter 3.44.8 SDK.

| Argument | Representation |
| --- | --- |
| toolbarOpacity | Required finite nonnegative number |
| minExtent | Required finite nonnegative number |
| maxExtent | Required finite nonnegative number |
| currentExtent | Required finite nonnegative number |
| isScrolledUnder | Unset, null, true or false checkbox |
| hasLeading | Unset, null, true or false checkbox |
| child | Required single box child, wrapped atomically |

The native constructor requires minExtent <= currentExtent <= maxExtent.
Both Java validation and the isolated Canvas decoder enforce this ordering and
all nonnegative bounds. Zero and equal extents are valid. Numeric storage follows
the existing portable integer/finite double contract, not Infinity or NaN.

Settings asserts only toolbarOpacity >= 0, so the schema deliberately accepts 2.
The native FlexibleSpaceBar title color requires opacity in 0..1. Canvas retains
the native Settings value and labels that descendant's unavailable preview,
preserving its modeled children; it does not silently clamp generated values.
A Settings node with an ordinary child, or a FlexibleSpaceBar without a title,
still uses its native value directly.

This InheritedWidget has no native constructor callbacks and no Events are invented.
Its native Widget child could wrap a sliver in a Flutter application. The current
Designer uses fixed box/sliver placement contracts and does not admit that
transparent raw-Sliver branch here. Such a branch requires a separate protocol-aware
provider design; no fake SliverToBoxAdapter or native named constructor is emitted.
Static FlexibleSpaceBar.createSettings is a convenience helper, not an additional
constructor represented by this palette entry.

## Creation, Properties, source and Canvas

Material order 520, six typed Properties rows and required Child in Slots.
Creation persists toolbarOpacity=1, minExtent=56, maxExtent=200, currentExtent=200.
These are Designer starter values, not SDK constructor defaults.

Palette creation wraps an existing compatible child atomically, including root
wrapping through the shared command path. An empty destination is rejected with
a concrete add-a-child-first explanation. Required Child cannot be cleared or
removed, and required numeric fields cannot be reset to unset. Nullable flags
retain the shared unset/null/checkbox behavior and stable property row identities.

Valid individual property changes use the shared SetProperty pipeline. Invalid
intermediate ranges are rejected without changing FD/Dart snapshots. A multi-field
PatchProperties transition validates the entire new range atomically. In ordinary
Properties, raise Max extent first when increasing the range; lower Min extent
first when reducing it, then edit Current extent and the remaining bound.

Generated source uses the exact const FlexibleSpaceBarSettings constructor,
all required arguments and the existing child identity. Source navigation proof,
pair-save admission, Save/reopen and Undo/Redo preserve user members and FD identity.

Canvas mounts the native InheritedWidget, retaining nearest-provider override and
updateShouldNotify behavior. It neither assigns a Child size nor creates scroll
animation. Layout comes from the surrounding tree. Occupied required Child is not
an append/drop destination; replacement remains available through the shared Slots
workflow. All four light/dark 16/32 SVG variants use the reviewed registry.

## Current inventory

162 definitions: 154 with scalar Properties and eight structural. 6930 writable
rows, 6912 outside Scaffold, 132 const-capable definitions; Material 49, Scrolling 46.
The 157 ordinary destinations (142 AnyWidget and 15 constrained) generate 25434
candidates: 17041 accepted and 8393 rejected. Required wrappers: 29, including
22 root-compatible box wrappers. Boolean-only rows: 631; nullable boolean unions: 50.
Native Events remain 154 across 40 types; all callables remain 201 across 54 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- Dedicated core schema/round-trip/boundary tests and stable six-row Properties test.
- FlexibleSpaceBarSettingsRealSdkTest passes with the pinned SDK explicitly
  enabled: wrapping, strict source evidence, all six edits, atomic equal-zero/large
  range transitions, required-child/unset rejection, rollback, save/reopen and
  history. Ten generated native Flutter runtime cases also pass.
- Dedicated Canvas tests cover all 18 opacity/nullable-leading/LTR-RTL combinations,
  stable current-extent edits, all six inherited updates, nested override,
  native equal-zero extents, opacity-above-one diagnostics and wrapper drop behavior.
- Full Designer core: 2338 tests, 20 conditional skips, zero failures/errors.
- Broad focused Java regression: 1404 tests, four conditional skips, zero
  failures/errors. This overlaps the core run; the two totals must not be added.
  The real-SDK test above was explicitly enabled and passed in a separate run.
- Full Flutter Canvas regression: 5033 tests passed; flutter analyze reports no
  issues. The final dedicated 23-test suite also passes, including native
  background-only FlexibleSpaceBar with toolbarOpacity above one.
- Final source-bundle and Web artifact gates: 33 tests, zero skips/failures/errors.
  The optional real-artifact test was explicitly enabled with canvas.runner.web.source.
  A preliminary manual Web build used incomplete flags and was correctly rejected;
  the final build uses the same offline profile as WebCanvasBuildService:
  --release --target lib/main_web.dart --no-tree-shake-icons
  --no-web-resources-cdn --pwa-strategy none --no-wasm-dry-run.
  No integrity checks were relaxed.
- This slice did not rerun the entire Java reactor suite or the unrelated existing
  JNA DPI test. No claim is made that those checks passed in this run.
- Interactive desktop IDE testing was not performed; the user's IDE is not restarted.

## Verified package

Maven install and nbm:cluster both completed with exit 0 on 2026-09-13.
The NBM contains the current schema, validator, catalog, property editor and
generation classes, all four SVG variants and all 40 runner sources with exact
manifest sizes/SHA-256 values. The final 35-file Web artifact matches its manifest.
All four development-cluster JARs are byte-identical to their counterparts in the NBM.

- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
- Size: 8796204 bytes
- SHA-256: cad289c439fd47d48a27a92ecbe78a275dc12693e2a1c870f83122f2ddbd44cf
- Built: 2026-09-13T10:02:42.5398523Z

Next palette candidate: [LayoutBuilder](https://api.flutter.dev/flutter/widgets/LayoutBuilder/LayoutBuilder.html),
which is not yet in the current catalog.
