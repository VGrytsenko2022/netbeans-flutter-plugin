# SliverPrototypeExtentList

Reviewed against pinned Flutter 3.44.8 `widgets/sliver_prototype_extent_list.dart`,
`widgets/scroll_delegate.dart` and `rendering/sliver_fixed_extent_list.dart`;
also the official [class](https://api.flutter.dev/flutter/widgets/SliverPrototypeExtentList-class.html),
[list](https://api.flutter.dev/flutter/widgets/SliverPrototypeExtentList/SliverPrototypeExtentList.list.html),
[builder](https://api.flutter.dev/flutter/widgets/SliverPrototypeExtentList/SliverPrototypeExtentList.builder.html)
and [unnamed constructor](https://api.flutter.dev/flutter/widgets/SliverPrototypeExtentList/SliverPrototypeExtentList.html).

## Complete constructor surface

| Palette entry | Native constructor | Fields | Slots |
| --- | --- | --- | --- |
| SliverPrototypeExtentList.list | SliverPrototypeExtentList.list | Three delegate booleans | Prototype item, ordered children |
| SliverPrototypeExtentList.builder | SliverPrototypeExtentList.builder | Required itemBuilder, nullable itemCount/findChildIndexCallback, three booleans | Prototype item |
| SliverPrototypeExtentList.new | SliverPrototypeExtentList | Required delegate | Prototype item |

The stable type IDs are `flutter.widgets.SliverPrototypeExtentList`,
`.builder` and `.delegate`. The internal .delegate variant emits the unnamed
constructor; it does not invent an SDK constructor. Only this native constructor
is const-capable, subject to actual child/delegate const evidence. Shared
Designer stable identity remains the key policy.

All variants require a non-null native `prototypeItem`. Designer exposes it as
a single box-widget slot; an explicitly empty slot uses the reviewed
`const SizedBox(width: 48.0, height: 48.0)` creation preset in both Dart and
Canvas. This is a Designer preset, not an SDK default or null. No synthetic
prototype node is persisted. Add, replace, move or clear the measurement widget
through the tree/Slots independently of visible children. Clearing a populated
prototype restores the preset and preserves all visible child identities.
Its tooltip explains this behavior.

The SDK lays the prototype out with the viewport's cross-axis constraint.
Its main-axis size determines every list item's extent. It is not painted and
cannot respond to input. Zero extent is valid; the measured widget may be
arbitrarily composed from admitted box widgets. Prototype and visual children
reject slivers and direct Expanded/Flexible/Spacer parent-data widgets.

`addAutomaticKeepAlives`, `addRepaintBoundaries` and `addSemanticIndexes`
use the shared centered checkbox with unset support; their native default is
true. The builder's `itemCount` accepts a non-negative integer or null;
omitted/null count is unbounded. The nullable-returning item builder may stop
early by returning null even before a finite count is reached.

The builder accepts exact `Widget? Function(BuildContext, int)` references,
getters and zero-argument factories. Optional `findChildIndexCallback` accepts
`int? Function(Key)` or null and maps keys to new indexes to preserve state
when data reorder. These are Builder/Delegate callables, not native Events.
Unlike SliverFixedExtentList.builder, this SDK constructor has no
`semanticIndexOffset` parameter; neither that field nor numeric `itemExtent`
is exposed.

The unnamed variant accepts typed SliverChildListDelegate,
SliverChildBuilderDelegate and custom SliverChildDelegate subclasses through
project references/getters/factories. Required builder/delegate fields support
an explicit empty preset but cannot be unset/null. Optional reset removes its
Dart argument. Static-type evidence rejects dynamic, incompatible or invalid
nullable references even with analyzer diagnostics suppressed.

## Canvas and editing behavior

Only Sliver-accepting parent slots admit these entries. The native
SliverPrototypeExtentList performs prototype measurement and child layout in
both axes, reverse and RTL. No fixed-size rendering substitute is used.
Runtime changes to the prototype resize the visible list.

The Canvas measurement render marker prevents the prototype subtree from
acquiring selection outlines, zero-size handles or pointer drop targets.
Pointer drops on a visual list address visible children. The explicit
Prototype item destination remains available through tree/Slots editing;
a hidden descendant is not a Canvas move destination. Occupied prototype
moves are rejected; replacement remains an explicit atomic slot operation.

Isolated Canvas never executes project builders, delegates or key-index code.
A visible limitation message names those references. The prototype is still
measured, but project-generated items are not previewed. Generated Dart retains
the exact project references and executes them in the application.

All ten fields and four slot definitions participate in validation, deterministic
Dart generation, FD round-trip, candidate evidence and normal history. The
fallback SizedBox carries a real SDK symbol probe. Property row/group identity
and checkbox presentation survive refresh/reopen. No format version changes:
FD 16, Catalog 15, Canvas 19, transport 1.

## Verification and inventory

Current inventory: 138 admitted definitions (134 typed, four structural),
6,479 writable rows (6,461 outside Scaffold), 539 boolean rows and 110
const-capable definitions. The insertion matrix has 127 destinations
(123 any-widget, four trait-constrained), 17,526 candidates, 14,195 accepted
and 3,331 rejected. Native Events remain 150 across 36 types; all callables
total 191 across 46 types: 150 Events, 28 Builders, eight Delegates,
three Predicates and two Formatters.

Verification includes:

- Constructor, requiredness, slot cardinality, preset and admission contracts;
  absence of unsupported itemExtent/semanticIndexOffset arguments.
- Exact generation and FD round-trip with independent prototype/visible child
  trees, typed references/factories and explicit nulls.
- Three native palette creation paths and independent slot admission;
  real property-slot editor clear/cancel behavior.
- 178 dedicated Canvas tests: empty/default prototype, zero/fractional/large
  extents, all axes/reverse/RTL and delegate flags, scroll geometry, real
  selection/drop/move, occupied/cleared prototype and measurement relayout.
- Full Flutter suite: 3,065 passed; Dart analyzer `lib test`: no issues.
- Real SDK evidence/runtime test: 18 generated applications; prototype edits
  and removal restoring preset, source preservation, save/reopen/Undo/Redo,
  builder early-null and nullable/known/unbounded counts, exact key lookup,
  project getters/factories/custom delegates and ten unsafe signatures rejected.
  The enabled JUnit test passed in 90.919 seconds.

Automated verification on 2026-09-12: the complete Designer-core suite finished
with 2,260 tests, 20 conditional skips, zero failures/errors and successful Maven
exit. Targeted NetBeans regression (including real Swing slot-editor tests)
finished with 790 tests, seven conditional skips, zero failures/errors and
successful Maven exit. The separately enabled real-SDK test passed as recorded
above. The unrelated full NetBeans reactor was not repeated after the previously
recorded Windows AWT shutdown issue.

NBM package/install and development cluster creation succeeded. Package checks
confirmed the current schema class, twelve distinct light/dark 16/32 SVG variants,
all 40 runner sources and all 35 Web artifact files. The module, Designer and
runner JARs in the development cluster exactly match the NBM. Strict integrity
checks were not relaxed.

Web main.dart.js: 3,672,290 bytes; SHA-256
`7ae8f31aa0ee091f8a3d1bea3e9265de1f07f5cbfc590cc1473f4b812c4e617e`.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(8,685,955 bytes); SHA-256
`73e715591031a2b1c4e2b0c1394642bbe177fcd4d231ba6f984048a1a8fc4632`.

Manual NetBeans desktop smoke testing and IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
