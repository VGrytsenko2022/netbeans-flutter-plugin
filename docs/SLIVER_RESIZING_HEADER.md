# SliverResizingHeader vertical slice

Pinned Flutter 3.44.8; canonical checkout:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

## API and full slot contract

`SliverResizingHeader({Key? key, Widget? minExtentPrototype, Widget? maxExtentPrototype, Widget? child})`.

One const-capable constructor, three nullable box slots, no scalar properties or
native Events. Key follows the existing Designer identity policy. No delegate,
numeric extent fields, pinned/floating selectors or fake callback were invented.

- Min extent prototype: measurement-only box; absent/null means zero minimum.
- Max extent prototype: measurement-only box; absent/null uses the visible child's
  intrinsic size along the scroll axis.
- Child: painted box subtree; absent/null uses the SDK's SizedBox.shrink().

Use finite SizedBox prototypes for explicit extents. Both vertical and horizontal
axes are supported, including reverse and RTL. The header stays pinned when its
minimum extent is nonzero; shrinking behavior is the native SDK implementation.
The source lays prototypes out with scroll-axis-unbounded box constraints and
measures the child with dry layout when maximum prototype is absent. An arbitrary
child that cannot supply intrinsic/dry layout should receive an explicit maximum
prototype. The Designer does not promise intrinsic sizing for every Flutter widget.

No guessed min <= max invariant is added: the pinned SDK clamps child constraints
to the measured minimum, even when a prototype's minimum exceeds its maximum.
It can also have nonzero scroll extent but zero painted content. These cases are
covered by native/Canvas tests, not silently normalized in the file model.

## Model, generation and edit lifecycle

The creation prototype has three empty SingleSlots and no scalar values.
Slot order follows the constructor: minimum, maximum, child. FD encoding, canonical
Canvas fingerprints and generic Dart generation use the existing slot contracts.
An explicitly empty stored slot emits null; a missing optional slot can be omitted.
Both retain native defaults, rather than fabricated measurement boxes.

All three slots accept ordinary box widgets and reject Slivers and directly invalid
Flex parent-data children. The header itself requires a Sliver slot; document-root
and box placement are rejected. It is directly insertable, not a required-child
wrapper. Fifteen SDK Sliver slots are compatible; ten allow normal empty/list drops.

The Properties Slots editor admits offered box types only, preserves the row and
property-set instances on refresh, and stages add/replace/move/clear operations
without publishing on Cancel. Source changes remain guarded by normal .fd/.dart
pair-save, exact source integrity and analyzer gates. Save/reopen, further scalar
edits within each child, replacement, clearing, cross-slot moves and Undo/Redo
preserve user Dart and subtree identities. No source helper is created.

## Canvas and accessibility

The runner constructs the real SliverResizingHeader. Each supplied measurement
subtree is wrapped only in the shared prototype-geometry marker; sizes and null
defaults remain native. The SDK excludes prototypes from focus and does not paint
them. The geometry marker prevents hidden prototype nodes and their descendants
from entering Canvas selection, drop, move or zero-size overlay geometry.

Canvas pointer drops on the header address Child only. Measurement slots remain
explicitly editable through Slots and the widget tree. A direct tree drop onto the
multi-slot parent is deliberately ambiguous under the existing tree-drop policy;
choose an exact slot instead. Blank/zero-painted headers retain the shared compact
selection/drop handle. Four unique SVG variants cover 16/32 pixels and light/dark.

## Inventory

155 admitted definitions: 148 with scalar properties and seven structural;
6520 scalar rows (6502 outside Scaffold); 125 const-capable constructors.
Scrolling: 44 entries. 139 ordinary drop destinations (127 AnyWidget, 12 trait-based)
produce 21545 candidates: 14976 accepted and 6569 rejected. Required wrappers: 28.
Boolean fields and callable inventories are unchanged: 562 boolean-only, 44 nullable
boolean unions; 151 native Events across 37 types, 198 callables across 51 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- All 4701 Flutter runner tests passed; the focused header suite contributes 69
  cases for nullable slots, both axes, reverse/RTL, native extents, pinned layout,
  prototype relayout and exclusion from Canvas hit/drop/selection geometry.
  Flutter analyze reports no issues.
- Full Java core: 2320 tests, zero failures/errors, 20 conditional skips.
- Real pinned-SDK suite and slot editor: 122 tests passed with no skips. The SDK
  test took 27.774 s and checks all eight slot-presence combinations through
  analysis and pair-save, save/reopen, replacement/clearing, cross-slot moves,
  child edits and Undo/Redo while preserving user source. Eight generated Dart
  files execute 64 axis/reverse/RTL native cases plus a focus-exclusion case.
- Extended cross-module regression: 1202 tests, zero failures/errors, 22 conditional
  skips; Maven exit 0. This overlaps the core and editor suites, not an additional
  disjoint total or a full NetBeans reactor run. The separate SDK suite above ran
  unskipped. The known Windows AWT/Surefire shutdown timeout occurred after
  System.exit(0), after the completed assertions and fresh XML reports.
- Release Web build passed using no-CDN/no-icon-tree-shaking flags.
  main.dart.js: 3715003 bytes, SHA-256
  `30eeef730420ddaffda085eeeae5d18f750cb2e91353729d9f2085e9c725c955`.
  Strict manifests retain all 40 runner sources and 35 Web entries.

NBM install and development-cluster builds completed successfully. Package
verification matches the new schema class, four SVG variants, source-lifecycle
and analyzer classes, all 40 runner sources and the 35-file Web manifest.
All four development-cluster JARs match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`;
8752800 bytes; SHA-256
`ab967bf5d4431dcdcba897b7c20453737f0a1a557edf4a5420f41f6ba89fbfc0`.

No installation into an independently running IDE or manual native desktop
verification is claimed.

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/SliverResizingHeader/SliverResizingHeader.html)
- [Official widget API](https://api.flutter.dev/flutter/widgets/SliverResizingHeader-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver_resizing_header.dart.

No manual IDE restart or native desktop session is claimed. CJK IME and Linux/macOS
Canvas providers remain deferred. PinnedHeaderSliver is now implemented in
[its dedicated slice](PINNED_HEADER_SLIVER.md).
