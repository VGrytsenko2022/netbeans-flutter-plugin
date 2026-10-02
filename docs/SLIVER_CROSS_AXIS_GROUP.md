# SliverCrossAxisGroup

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor

`SliverCrossAxisGroup({Key? key, required List<Widget> slivers})` is const-capable.
The key is the Designer-owned stable identity; there are no scalar constructor
fields or interaction callbacks. The required ordered Slivers slot can be empty
and accepts sliver widgets, including nested cross-axis or main-axis groups.
The shared Designer list limit is 10,000 children, not an SDK maximum.

Scrolling → SliverCrossAxisGroup creates an empty group. Slots supports insertion,
reordering, moving, removing and clearing children. Box widgets require an explicit
SliverToBoxAdapter. Root/box-slot placement, cycles, missing/stale sources and
invalid post-removal indices are rejected. Slot property rows/sets retain identity
on refresh, remain writable after reopen and do not publish drafts on Cancel.

Dart generation uses the native named `slivers:` argument and preserves order,
nested ownership, user members, exact save/reopen identities and Undo/Redo.
Const output requires const-capable children. No placeholder child is persisted.

## Native layout and Canvas

Children are arranged across the viewport's cross axis. Ordinary children have
native crossAxisFlex 1 and share the available cross-axis extent equally.
Scroll extent follows the longest child, not the sum. Canvas constructs the
actual native widget, retaining Flutter's axis/reverse/growth/RTL behavior rather
than implementing an independent layout algorithm.

The shared native geometry resolver uses each sliver's allocated cross-axis
constraints. Adjacent lanes have disjoint hit/drop zones; nested MainAxisGroup
lanes receive their own compatible sliver insertions. Empty lanes remain selectable
and accept slivers without synthetic model data. Scrolled-out geometry cannot
produce stale handles. Live updates preserve native child size/position when
reordering, moving between cross/main-axis parents, removing and clearing.

The group creates no additional viewport or scroll controller. Its descendants
retain their existing project-owned callback/builder preview limitations. The
group does not execute arbitrary project Dart or pretend to support unadmitted
sliver types.

## Companion widgets are separate palette slices

Unequal cross-axis shares are configured through Flutter's SliverCrossAxisExpanded
parent-data widget. Bounded cross-axis widths use SliverConstrainedCrossAxis.
Neither is a parameter of SliverCrossAxisGroup, and neither companion is admitted
to this historical palette milestone. The subsequent
[SliverCrossAxisExpanded](SLIVER_CROSS_AXIS_EXPANDED.md) slice now supplies unequal
flex in the UI; [SliverConstrainedCrossAxis](SLIVER_CONSTRAINED_CROSS_AXIS.md) now provides the constrained-width companion.

## Inventory at this milestone

143 admitted constructor definitions: 137 typed and six structural.
6,492 writable scalar rows (6,474 outside Scaffold), 545 boolean rows and
113 const-capable definitions. 130 insertion destinations: 124 AnyWidget and six
trait-restricted slots. 18,590 source/destination candidates: 14,370 accepted and
4,220 rejected. Native Events remain 150 across 36 types; all callables remain
196 across 49 types (150 Events, 32 Builders, nine Delegates, three Predicates,
two Formatters). FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted definitions, not a count of all Flutter widgets or remaining work.

## Sources

- [Flutter class and layout contract](https://api.flutter.dev/flutter/widgets/SliverCrossAxisGroup-class.html)
- [Native constructor](https://api.flutter.dev/flutter/widgets/SliverCrossAxisGroup/SliverCrossAxisGroup.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and
  packages/flutter/lib/src/rendering/sliver_group.dart.

## Verification

The 66 dedicated Canvas tests cover exact schema rejection, empty/nested groups,
fractional and zero main-axis extents, overflow, axis/reverse/RTL, live reorder/
removal/clear, mixed main-axis lanes, disjoint drop zones, and both directions of
cross/main-axis reparenting with correct allocated size and no stale targets.

The separately enabled real-SDK JUnit test passed (one test, zero skips/failures/
errors, 15.088 seconds, successful Maven exit). It checks five candidate-analysis
transitions, exact FD/source reopen bytes, preserved user members, Undo/Redo and
rejected box/cycle placements, then runs 48 generated Dart cases over six
structural states and axis/reverse/RTL configurations.

The full core reactor passed: 2,268 tests, 20 conditional skips,
no failures/errors and successful Maven exit. Totals come from fresh Surefire XML.
The unrelated full NetBeans reactor is not repeated because of its recorded
Windows AWT shutdown issue.

The full Flutter runner passed all **3,270 tests** with successful exit.
Dart analysis of lib and test reports no issues. An older exact-contract test now
finds the next schema record generically; its SizedOverflowBox expected record
is unchanged, so adding an alphabetically adjacent widget cannot widen that record.

Release Web build succeeded. main.dart.js: 3,680,123 bytes; SHA-256
`b54be2da3f9b8db896c274f4251d393ebcbb87d2e89609d4eed8c53f53f48ec8`.

The broad targeted NetBeans/core suite passed **1,130 tests** with 10 conditional
skips, no failures/errors and successful Maven exit (fresh Surefire totals).
It includes palette/drop/wrap admission, property editors, accessibility, icon
registry, Java/Dart capability parity, strict runner-source and Web artifact checks.

NBM install and development-cluster creation succeeded. Verification compared the
packaged schema class, four light/dark 16/32 SVG variants, forty runner sources
and thirty-five Web artifact files against the current inputs. All three plugin/
Designer/runner cluster JARs exactly match the NBM. Integrity checks remain strict.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,699,426 bytes; SHA-256
`b1d07521e0e6f65bd74a5efcb6ce4cc5c9f27fca388d352ee1e1a774591edbf5`.


Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
