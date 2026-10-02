# OrientationBuilder vertical slice

Reviewed against the pinned Flutter 3.44.8 SDK in
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native API and behavior

[Constructor](https://api.flutter.dev/flutter/widgets/OrientationBuilder/OrientationBuilder.html):
const OrientationBuilder({Key? key, required OrientationWidgetBuilder builder}).
The [callback](https://api.flutter.dev/flutter/widgets/OrientationWidgetBuilder.html)
is Widget Function(BuildContext, Orientation). Key uses shared Designer identity.
There are no named constructors, native Events or separate Child slots.

The pinned widgets/orientation_builder.dart implementation compares the parent's
maximum width and height: greater width means landscape, otherwise portrait.
Square and fully unbounded constraints therefore mean portrait; only an unbounded
width means landscape. This is distinct from MediaQuery/device orientation.
The native widget delegates layout to LayoutBuilder: intrinsic/dry layout remains
unsupported, and the callback must produce a RenderBox rather than a RenderSliver.
A Dart Widget return type cannot statically prove this rendering-protocol rule.

## Properties, source proof and lifecycle

- One required Builder row: Empty or a strict typed project function, getter,
  member or zero-argument factory returning OrientationWidgetBuilder, including
  declared-package members. Empty is a Designer creation value, not an SDK default.
- Empty emits (context, orientation) => const SizedBox.shrink(). Parent minimum
  and tight dimensions still apply; the result is not always physically zero.
- Missing/null, raw Dart, other strings and legacy event callbacks are rejected.
  Select Empty to clear a custom binding; Restore Default cannot omit Builder.
- Shared reference editor preserves writable property-row/group identity on edits
  and after reopen. Callable classification is BUILDER, not EVENT.
- Exact references, imports and source offsets feed the existing analyzer gate.
  A proof-only original call uses fixed SDK BuildContext and Orientation types.
  Typed initialization and strict call-result proof reject wrong signatures,
  nullable/dynamic callbacks and nullable/dynamic Widget results.
  No proof code is executed or persisted in the user's source.
- Add, edit, move, reset-to-empty, save/reopen and Undo/Redo retain identity and
  user-owned members. Rejected candidates do not mutate source bytes.

## Palette and isolated Canvas

Layout category order 260. Four reviewed portrait/landscape SVGs cover 16/32
and light/dark themes. Root and ordinary box slots admit the widget; raw sliver
and other constrained-trait destinations reject it. No synthetic Child is added.

Canvas mounts the real OrientationBuilder with a preview-owned empty callback.
Project functions/getters/factories remain presence-only and are never executed.
A custom binding has an explicit unavailable-content tooltip/semantics notice;
generated application Dart still invokes the actual project callback.
The zero-size selection handle does not change layout. Tight sizes use normal
selection, and drops resolve to a compatible parent, never to a fictional slot.
Changing dimensions or binding presence retains native render/element identity.

## Inventory

164 definitions: 156 scalar, eight structural; 6932 writable rows, 6914 outside
Scaffold; 134 const-capable definitions. Layout has 34 entries, Material 49,
Scrolling 46. The 157 ordinary destinations (142 AnyWidget, 15 constrained)
produce 25748 candidates: 17325 accepted, 8423 rejected.
Required wrappers remain 29, including 22 root-compatible box wrappers.
Boolean-only rows remain 631 and nullable-boolean unions 50.
Native Events remain 154 across 40 types; all callables total 203 across 56 types,
including 35 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Verification

- Full Designer core: 2346 tests, zero failures/errors, 20 conditional skips.
- Eleven dedicated Canvas tests cover required closed values, presence-only
  references, no invented slots, tight/loose geometry, LTR/RTL, parent insertion,
  selection, unbounded axes and portrait/square/landscape resize with retained
  native identity. Full Flutter regression: 5055 tests passed; analyze: no issues.
- Real SDK gate passed unskipped (61.702 seconds): exact source evidence for
  functions/getters/factories and package members, rejection of wrong signatures
  and nullable/dynamic callbacks/results, add/move/reset, Save/reopen and Undo/Redo.
  Nine generated forms run 35 native Flutter tests. One test covers 24 combinations
  of parent bounds and conflicting MediaQuery sizes, including square and singly/
  fully unbounded bounds; other tests cover light/dark, RTL and inherited rebuilds.
  Negative tests confirm native sliver-result and intrinsic-layout restrictions.
- Cross-module Java selection: 1469 tests in 127 report files, four conditional
  skips. The initial run caught the 32px SVG metadata/topology and an outdated
  Layout category count. These were corrected; a 161-test focused recheck exited
  zero, leaving all selected reports with zero failures/errors. These totals
  overlap the core run and must not be added. Optional SDK gates are skipped in
  this selection; OrientationBuilder's real SDK gate passed separately above.
- Source-bundle and Web artifact contract tests pass, including all 24 Web gate
  cases with canvas.runner.web.source explicitly enabled. Release Web build uses
  --no-web-resources-cdn and --no-wasm-dry-run. Exact sizes/hashes are regenerated;
  no integrity guard was relaxed.
- Full Java reactor and interactive desktop verification were not run.

## Package

Maven install and nbm:cluster both exited zero. Packaged schema, generator,
catalog, callback metadata, property/palette/icon classes and analyzer witnesses
match current compiled classes. All four SVGs and 40 packaged runner sources
match the checkout and pinned manifest. The packaged Web manifest matches the
checkout, and all 35 release Web files match its exact sizes and SHA-256 hashes.
All four development-cluster JARs are byte-identical to the NBM counterparts.

- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
- Size: 8800218 bytes
- SHA-256: e470763c1206eacc3002980e89e96d24812e5192b0b52b24958d247d9b5b4ac1
- Built: 2026-09-13T11:03:21.3104818Z

No manual desktop test or IDE restart is claimed. The unrelated existing JNA DPI
test is outside this slice. CJK IME and Linux/macOS Canvas providers remain deferred.

The next slice is implemented: [DeviceOrientationBuilder](DEVICE_ORIENTATION_BUILDER.md),
the native MediaQuery-based counterpart with box and sliver placement projections.
