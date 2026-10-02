# SliverMainAxisGroup

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor and editing contract

`SliverMainAxisGroup({Key? key, required List<Widget> slivers})` is const-capable.
There are no additional scalar constructor parameters or interaction callbacks.
The stable-identity key is generator-owned. The required ordered `slivers` slot
may be empty and accepts only sliver widgets, including nested main-axis groups.
The Designer's shared list limit is 10,000 children; this is not an SDK limit.

Scrolling → SliverMainAxisGroup creates an empty group. Slots supports adding,
reordering, moving between compatible parents, removing and clearing children.
A box widget must be wrapped in SliverToBoxAdapter. Root/box-slot placement,
ancestor cycles, stale sources and invalid post-removal indices are rejected.
Properties rows and sets retain identity across refreshes and remain writable
after reopen. Cancelling the slot editor publishes no mutation.

Generated Dart preserves exact child order and nested ownership, using the native
`slivers:` argument, not `children:`. Empty lists remain explicit. Const output
requires const-capable children; no fabricated child or event handler is persisted.
The shared command pipeline preserves user members and supports Undo/Redo.

## Canvas behavior

Canvas constructs native SliverMainAxisGroup. The group inherits the enclosing
viewport's main axis, reverse/growth direction and text direction. It is not a
second scroll view and has no independent scroll controller or orientation field.
Native layout combines child scroll extents and bounds paint within the group.
The existing sliver hit/drop handling clips to the viewport; offscreen groups do
not expose phantom handles, and a group scrolled back into view regains its handle.
An empty group has a selectable/drop handle without a persisted placeholder.

Nested layout, fractional and zero extents, overflow, both axes, reverse and RTL
are covered. Live model replacement updates child order/ownership, removes stale
move targets and refreshes empty geometry. Project-owned callbacks/builders in
descendant widgets keep those widgets' existing isolated-preview limitations;
the group does not execute arbitrary project code or add new sliver types.

## Inventory at this milestone

142 admitted definitions: 137 typed and five structural. 6,492 writable scalar
rows (6,474 outside Scaffold), 545 boolean rows and 112 const-capable definitions.
129 insertion destinations: 124 AnyWidget and five trait-restricted slots.
18,318 source/destination candidates: 14,342 accepted and 3,976 rejected.
Native Events remain 150 across 36 types. All callables remain 196 across 49 types
(150 Events, 32 Builders, nine Delegates, three Predicates and two Formatters).
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted constructor definitions, not a count of unique Flutter classes
or a verified remaining-work total.

## Sources

- [Flutter class contract](https://api.flutter.dev/flutter/widgets/SliverMainAxisGroup-class.html)
- [Native constructor](https://api.flutter.dev/flutter/widgets/SliverMainAxisGroup/SliverMainAxisGroup.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and
  packages/flutter/lib/src/rendering/sliver_group.dart.

## Verification

- Full Flutter runner: **3,204 tests passed**, including 58 dedicated group tests.
  Dart analysis of lib and test reports no issues.
- Full core reactor: **2,266 tests**, 20 conditional skips, no failures/errors,
  successful Maven exit. Counts are from fresh Surefire reports.
- Broad targeted NetBeans/core contracts: **1,125 tests**, nine conditional skips,
  no failures/errors, successful Maven exit. Includes property editors, palette/
  drop/wrap planning, accessibility, icon resources, Java/Dart capability parity,
  strict source bundle and Web artifact checks.
- Separately enabled real-SDK test: one test, zero skips/failures/errors,
  15.748 seconds, successful Maven exit. It checks five candidate-analysis
  transitions, exact save/reopen bytes and FD identity, preserved user members,
  Undo/Redo and rejected box/cycle placements, then executes **48 generated Dart
  runtime cases** across six structural states and axis/reverse/RTL combinations.
- The unrelated full NetBeans reactor was not repeated because of its recorded
  Windows AWT shutdown problem.

Web main.dart.js: 3,677,304 bytes; SHA-256
`3150361daf5bb4d527de45e34434088cb902111f9fa51519faefa05c769a579f`.

NBM install and development cluster creation succeeded. Verification compared
the packaged schema class, all four light/dark 16/32 SVG variants, forty runner
sources and thirty-five Web artifact entries against their current local inputs.
The three plugin/Designer/runner JARs in the dev cluster exactly match the NBM.
No integrity check was relaxed.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,696,940 bytes; SHA-256
`2c3057da2db1b2794a1735f5004b576836d46ca5a30125672868b20da9bfe44b`.

Manual NetBeans desktop smoke testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
