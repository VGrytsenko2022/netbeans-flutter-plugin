# PinnedHeaderSliver vertical slice

Pinned Flutter 3.44.8; canonical checkout:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

## Complete constructor and placement

`PinnedHeaderSliver({Key? key, Widget? child})` is a const-capable constructor
with one optional box Child. There are no scalar arguments, native Events,
delegate, height/minimum/maximum settings or pinned/floating selectors.
Key uses the established Designer stable identity policy.

The header requires a Sliver destination, never a document root or box slot.
Its Child accepts ordinary box widgets and rejects Slivers and directly invalid
Flex parent-data children. Fifteen SDK Sliver slots are compatible; ten support
ordinary empty/list insertion. It is directly insertable, not a required-child wrapper.

The creation prototype contains an empty SingleSlot and no fabricated child or
height. An explicitly empty stored slot emits `child: null`; an absent optional
slot may be omitted. The native Semantics wrapper retains zero extent for a
null child under normal viewport constraints.

## Editor and source lifecycle

Scrolling category order 440; four distinct 16/32-pixel light/dark SVG variants.
Slots exposes writable Child with English help, offered box choices only,
staged edits, stable property row/group instances and save/reopen support.
Add, replace, remove, move and child-property changes use the shared command
history and exact .fd/.dart pair-save analyzer gates. User Dart is preserved
through Save/reopen and Undo/Redo. No delegate source scaffold or new file format
is introduced.

## Native Canvas

Canvas constructs the real PinnedHeaderSliver, including its native child
Semantics wrapper. The child is laid out with the viewport's box constraints;
its scroll-axis size defines scroll extent, maximum paint extent and maximum
scroll obstruction. Size changes are measured on layout, including while pinned.
There is no prediction, prototype measurement or static-height approximation.

Vertical/horizontal axes, reverse and RTL use the SDK's axis/growth direction.
Oversized content is clipped by available paint extent, not silently rewritten
in the model. Existing viewport overlays retain header selection and child-slot
drop/move geometry for empty headers and after scrolling past preceding content.

Shared pointer targeting still prefers a deeper compatible child when areas
overlap. For an empty header overlapping another empty box, choose the exact
Child slot through the tree/Slots instead. Explicit move preview and command
validation retain exact targets; no global drop-priority policy is changed.

Native content semantics remain present when pinned. Canvas additionally exposes
Designer identity/selection labels; those labels are not generated into user Dart.

## Inventory

156 admitted definitions: 148 with scalar properties and eight structural;
6520 scalar rows (6502 outside Scaffold); 126 const-capable constructors.
Scrolling: 45 entries. 140 ordinary drop destinations (128 AnyWidget, 12 trait-based)
produce 21840 candidates: 15101 accepted and 6739 rejected. Required wrappers: 28.
Boolean and callable totals remain 562 boolean-only, 44 nullable boolean unions,
151 native Events across 37 types and 198 callables across 51 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- All 4751 Flutter runner tests passed, including 50 new focused cases: strict
  schema, nullable/zero/ordinary/oversized child extents, both axes, reverse/RTL,
  dynamic child edits, post-scroll hit/drop geometry, exact empty-slot preview
  and pinned content semantics. Flutter analyze reports no issues.
- Full Java core: 2323 tests, zero failures/errors, 20 conditional skips.
- Real pinned-SDK and Slots editor: 123 tests passed with no skips. The SDK test
  completed in 32.921 s and checks empty/filled insertion, replacement, child
  edits, clearing, moves, pair-save analysis, save/reopen and Undo/Redo with exact
  user-source preservation. Six generated Dart files run 48 native axis/mode
  cases, plus a dynamic-size/semantics case.
- Extended cross-module regression: 1233 tests, zero failures/errors, 23 conditional
  skips; Maven exit 0. This overlaps core/editor tests and is not a full NetBeans
  reactor run. The separate SDK suite above ran unskipped. The known Windows
  AWT/Surefire shutdown timeout occurred after System.exit(0), after assertions
  and fresh XML reports completed.
- Release Web build passed with no-CDN/no-icon-tree-shaking flags.
  main.dart.js: 3716427 bytes; SHA-256
  `7d24bb7e0d109f1c43268be085b48b0112e4bdcec0b943eb9e002d2fc7748b10`.
  Strict manifests retain 40 runner sources and 35 Web entries.
- The PhysicalShape schema snapshot now ends at the next widget record rather
  than assuming that Placeholder is alphabetically adjacent. Its contract did
  not change; PinnedHeaderSliver now occupies that position.

NBM install and development-cluster builds completed successfully. Exact package
verification matches the new schema class, four SVG variants, source-lifecycle
and analyzer classes, all 40 runner sources and the 35-file Web manifest.
All four development-cluster JARs match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`;
8756241 bytes; SHA-256
`b5d975d8fb2b35f09b77b1ab763000ec75173794a2129ae000d456c810ec0c7e`.

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/PinnedHeaderSliver/PinnedHeaderSliver.html)
- [Official widget API](https://api.flutter.dev/flutter/widgets/PinnedHeaderSliver-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/pinned_header_sliver.dart.

No manual IDE restart, running-IDE installation or native desktop verification
is claimed. The subsequent [SliverFloatingHeader slice](SLIVER_FLOATING_HEADER.md)
is now implemented and verified separately.
