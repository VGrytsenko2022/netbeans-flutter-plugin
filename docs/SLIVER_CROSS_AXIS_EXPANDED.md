# SliverCrossAxisExpanded

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor and creation

`const SliverCrossAxisExpanded({Key? key, required int flex, required Widget sliver})`.
The key is the Designer-owned stable identity. Flex is a required positive integer;
the Sliver slot contains exactly one sliver. There are no event callbacks or other
constructor arguments to omit. The portable Designer integer range is
1..9,007,199,254,740,991; this is a shared storage/Web constraint, not an SDK maximum.

Scrolling → SliverCrossAxisExpanded wraps an existing direct child of
SliverCrossAxisGroup.slivers. The detached prototype has an explicit creation
preset flex=1 (not an SDK default); WrapWidget fills its required Sliver slot in
one atomic command. Neither an empty group nor a terminal insertion can create
an orphan wrapper. Tree and Canvas use the same typed placement contract.

The wrapper must be a direct model child of CrossAxisGroup.slivers. Root,
CustomScrollView.slivers, MainAxisGroup.slivers, SliverPadding.sliver, ordinary
box slots and directly nested Expanded wrappers are rejected. A wrapped nested
CrossAxisGroup may itself contain Expanded children: each belongs to its own
nearest group. Ordinary boxes require SliverToBoxAdapter.

## Editing and native Canvas

Flex is a typed, writable, non-null integer row. Invalid/zero/negative/fractional
values are rejected without publishing a command. There is no Restore Default
that removes this required argument. Row and property-set identities remain
stable on refresh; editing remains available after reopen.

The required Sliver child cannot be cleared, removed or moved out on its own.
Use atomic slot replacement, move the whole wrapper within/between compatible
groups, or Undo the original wrapping operation. No new generic Unwrap action is
claimed in this slice.

Canvas creates the actual native ParentData widget, without inserting a RenderBox
between it and its RenderSliver child. Native crossAxisFlex controls proportional
cross-axis size while the group's main-axis extent remains the longest child.
Inherited horizontal/vertical axis, reverse, RTL and current viewport constraints
are retained. Palette wrap hit zones use actual bounded sliver geometry. Moving a
wrapper to a viewport/other incompatible parent produces no accepted preview.
Project-owned descendant callbacks keep their existing isolation limitations.

Dart generation, exact FD/source round trips, candidate-analysis proof, stable
child identities, preserved user members and Undo/Redo are covered. FD 16,
Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Inventory

144 admitted definitions: 138 typed and six structural. 6,493 writable scalar
rows (6,475 outside Scaffold), 545 boolean rows, 114 const-capable definitions.
Scrolling contains 33 entries. There are 24 required-child creation wrappers.
130 insertion destinations remain: 124 AnyWidget and six trait-restricted;
the new required Sliver slot is replacement-only, not an empty insertion target.
18,720 source/destination candidates: 14,371 accepted and 4,349 rejected.
Events remain 150 across 36 types; all callables remain 196 across 49 types
(150 Events, 32 Builders, nine Delegates, three Predicates, two Formatters).
These are admitted definitions, not a count of all Flutter widgets or remaining work.

## Verification

43 dedicated Canvas tests cover strict required schema, native proportional sizing
through the maximum portable flex, both axes/reverse/RTL, wrap hit geometry,
invalid move destinations, required-child protection, empty-group rejection,
live flex edits and Undo/Redo-style wrapper removal/reinstatement.
The full Flutter runner passed 3,313 tests. Dart analysis of lib/test reports
no issues. The complete core reactor passed 2,273 tests, with 20 conditional skips,
zero failures/errors and successful Maven exit; totals come from fresh Surefire XML.
The unrelated full NetBeans reactor is not repeated because of its recorded
Windows AWT shutdown issue.

The separately enabled real-SDK JUnit test passed: one test, no skips/failures/
errors, 26.148 seconds and successful Maven exit. It checks candidate analysis
through wrapping and flex changes, exact FD/source reopen bytes, preserved user
members, Undo/Redo and invalid edits, then runs 40 generated native Dart cases.

The broad targeted NetBeans/core suite passed 1,139 tests (336 core and 803
NetBeans), with 11 conditional skips, no failures/errors and successful Maven
exit. It includes palette/tree-drop/atomic-wrapper admission, property editors,
icons, accessibility, Java/Dart capability parity and strict source/Web manifests.
The real-SDK test was enabled separately as reported above.

Release Web build succeeded: main.dart.js is 3,681,845 bytes, SHA-256
`eb54def23873bd607dcfeb2f20e51b77246e71a49fd4bab03185a2c9683c6ae8`. Source and Web integrity checks remain strict.

NBM install and development-cluster creation succeeded. Verification compared
the packaged schema class, four light/dark 16/32 SVG icons, forty runner sources
and thirty-five Web files against current inputs. All three plugin/Designer/
runner cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,702,987 bytes; SHA-256
`07a68cd01f2eda3dc0c6449f327151ae3bbc317a9bf09e1ad43d80e2c01ca12a`.

Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
The subsequent [SliverConstrainedCrossAxis](SLIVER_CONSTRAINED_CROSS_AXIS.md) slice now provides maximum lane widths/heights.

## Sources

- [Official class and ParentData contract](https://api.flutter.dev/flutter/widgets/SliverCrossAxisExpanded-class.html)
- [Complete constructor](https://api.flutter.dev/flutter/widgets/SliverCrossAxisExpanded/SliverCrossAxisExpanded.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and packages/flutter/lib/src/rendering/sliver_group.dart.
