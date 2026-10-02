# SliverFloatingHeader vertical slice

Pinned Flutter 3.44.8; canonical checkout:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

## Complete constructor

`SliverFloatingHeader({Key? key, AnimationStyle? animationStyle,
FloatingHeaderSnapMode? snapMode, required Widget child})` is const-capable.
Key follows the established stable Designer identity policy. There are no native
Events, delegate, explicit header extents or additional constructor branches.

Six typed Properties expose the two constructor arguments and four AnimationStyle
members. Whole style accepts noAnimation, null or a project reference/getter/member/
zero-argument factory of type AnimationStyle?. Local duration/reverseDuration
accept signed portable microseconds, null or Duration? references. Curve and
reverseCurve accept all 43 pinned Curves presets, null or Curve? references.
Both snapMode values (overlay/scroll), omission and explicit null are preserved.

Whole style and local fields are mutually exclusive, including explicit null:
the editor clears the other representation in the same command. Model validation
also rejects conflicting persisted input. Restoring a leaf default does not reset
unrelated properties, slots or source. Rows/groups retain their identity.

The SDK defaults are overlay, 300 ms for both directions and easeInOut for both
curves. Reverse fields are honored by this widget, not ignored. Edits affect
subsequent snap animations; they do not rewrite an already captured animation.

## Placement and required child

The header requires a Sliver destination and owns one required ordinary box Child.
Direct insertion creates an explicit SizedBox(width: 48, height: 48) in the model.
This is a Designer creation preset, not an SDK default or a hidden renderer fallback.
The child ID is deterministically derived from the new owner ID; the root ID supplier
is consumed once. Creation, child replacement and Undo/Redo remain atomic.

Slots offers replacement with a new/existing box; it excludes clearing and rejects
moving the sole child out. Missing/null Child, a Sliver child and directly invalid
Flex parent-data children are rejected. The seeded header is not a required-child
wrapper: such wrapping cannot satisfy its Sliver parent and box Child simultaneously.
Dragging content into the starter SizedBox's own optional Child remains available.

## Native Canvas and source behavior

The actual SDK SliverFloatingHeader determines geometry from Child and user scroll
direction. It hides on reverse scrolling and reveals/snaps when scrolling forward.
Overlay grows over following content; scroll mode also contributes layout extent.
The shared pipeline preserves axis/reverse/RTL, stable child geometry and pointer
selection/drop targeting. Child size updates preserve the native element.

Project-owned getters/factories are not run in isolated Canvas. Each configured
reference has an explicit warning and uses the SDK default for that member.
Negative literal durations are source-preserved but unsafe when an SDK animation
starts; the preview alone uses zero and reports this limitation. These fallbacks
never alter .fd values or generated application Dart.

Generated source uses the native constructor, AnimationStyle, exact Curves symbols
and const Duration literals. Typed analyzer proof includes nullable project values
and rejects dynamic/wrong types. The two literal Duration probes have a dedicated
closed admission contract bound to generator ranges, names and trusted SDK files.
No broad dart:core exception or analysis bypass is introduced.

Save/reopen, replacement, child edits and Undo/Redo retain .fd/.dart identity and
user-owned Dart. FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Inventory

157 admitted definitions: 149 with scalar Properties and eight structural;
6526 writable rows (6508 outside Scaffold); 127 const constructors; Scrolling 46.
140 ordinary destinations produce 21980 candidates: 15111 accepted, 6869 rejected.
Required wrappers remain 28. Boolean and callable totals are unchanged:
562 boolean-only, 44 nullable boolean unions, 151 native Events across 37 types,
198 callables across 51 types.

## Verification

- All 4870 Flutter runner tests passed, including 119 dedicated floating-header
  cases. Coverage includes the 64 local-style/snap presence combinations,
  72 axis/reverse/RTL/snap/style gesture cases, all 43 curves, nullable constructor
  fields, inert project references, negative-duration warnings, child-source move
  protection, visible hit/drop geometry, native element identity and size updates.
- Flutter analyze: no issues. Release Web build passed with no CDN, no icon tree
  shaking and one reviewed dart2js/CanvasKit build.
- Full Java core: 2327 tests, zero failures/errors, 20 conditional skips.
- Extended cross-module regression: 1248 tests, zero failures/errors, 24 conditional
  skips; Maven exit 0. This overlaps the core/editor tests and is not a full
  NetBeans reactor run. The known Windows AWT/Surefire shutdown timeout was logged
  after System.exit(0), after assertions and fresh XML reports completed.
- Final SDK + Properties + Slots run: 127 tests passed, no skips. The SDK test
  completed in 43.63 s and covers insertion/replacement, required-child removal/move
  rejection, pair-save proofs, Save/reopen and Undo/Redo while preserving user Dart.
  Eleven generated Dart cases cover local styles, presets/null, current/imported
  getters/members/factories and nullable results; dynamic and wrong types are
  rejected. Generated widgets run native user-scroll/snap tests.
- Strict manifests retain 40 runner sources and 35 Web entries.
  main.dart.js: 3724070 bytes; SHA-256
  `265e8f7fdb251caa2234c918f07036a223ba1ffee8130fe4b1bfba816257e326`.

Native desktop manual verification is not claimed by the automated widget tests.
NBM install and development-cluster assembly completed successfully. The package
verifier matched the schema, property/evidence classes, four SVG variants,
all 40 runner sources and the 35-file Web manifest to the current checkout.
All four development-cluster JARs match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`;
8764574 bytes; SHA-256
`441c7c0e629011c3b610a02204f965d082e3b0d53e05f8393397d6504ee6556e`.

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/SliverFloatingHeader/SliverFloatingHeader.html)
- [Official widget](https://api.flutter.dev/flutter/widgets/SliverFloatingHeader-class.html)
- Pinned local SDK: packages/flutter/lib/src/widgets/sliver_floating_header.dart
  and packages/flutter/lib/src/animation/animation_style.dart.
