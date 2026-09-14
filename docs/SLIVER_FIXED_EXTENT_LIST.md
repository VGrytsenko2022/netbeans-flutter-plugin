# SliverFixedExtentList

Reviewed against pinned Flutter 3.44.8 `widgets/sliver.dart`,
`widgets/scroll_delegate.dart` and `rendering/sliver_fixed_extent_list.dart`,
plus the official [class](https://api.flutter.dev/flutter/widgets/SliverFixedExtentList-class.html),
[list](https://api.flutter.dev/flutter/widgets/SliverFixedExtentList/SliverFixedExtentList.list.html),
[builder](https://api.flutter.dev/flutter/widgets/SliverFixedExtentList/SliverFixedExtentList.builder.html)
and [unnamed constructor](https://api.flutter.dev/flutter/widgets/SliverFixedExtentList/SliverFixedExtentList.html)
documentation.

## All three constructors

| Palette entry | Native Dart constructor | Content ownership | Writable fields |
| --- | --- | --- | --- |
| SliverFixedExtentList.list | SliverFixedExtentList.list | Ordered visual box children, empty allowed | 4 |
| SliverFixedExtentList.builder | SliverFixedExtentList.builder | Required nullable-returning project item builder | 8 |
| SliverFixedExtentList.new | SliverFixedExtentList | Required project SliverChildDelegate | 2 |

The stable Designer type IDs are `flutter.widgets.SliverFixedExtentList`,
`.builder` and `.delegate`. The last suffix is an internal Designer variant,
not an invented native named constructor. Only the unnamed native constructor
is const-capable; actual const emission still requires valid delegate evidence.

Every constructor requires finite non-negative `itemExtent`. It is a double
in logical pixels along the scroll axis; cross-axis size follows the viewport.
Zero is valid in Flutter's rendering contract and is not artificially rejected.
New palette items start at 48, explicitly a Designer creation preset, not an SDK
default. It cannot be unset/null. Ordinary shared stable widget identity remains
the key policy; no arbitrary key-expression editor is introduced.

Both list and builder expose `addAutomaticKeepAlives`,
`addRepaintBoundaries` and `addSemanticIndexes` as centered boolean checkboxes
with unset support (native default true). The list's required `children` slot
is ordered and may be empty; it accepts box widgets, not slivers or direct
Expanded/Flexible/Spacer parent-data widgets.

Builder additionally exposes:

- Required `itemBuilder`: exact `Widget? Function(BuildContext, int)`.
  The empty preset returns null immediately; a project builder may return null
  before a known item count is exhausted.
- Optional `itemCount`: non-negative integer, or explicit null. Omission/null
  means no predefined count; a finite count improves extent estimates.
- Optional `findChildIndexCallback`: `int? Function(Key)`, or null.
  Returning the new index for a key preserves child state when data reorder.
- Optional non-negative `semanticIndexOffset`, default zero.

Builders are classified as Builder callables and key-index callbacks as Delegate
callables, not native interaction Events. All shared reference editors accept
typed functions/getters/zero-argument factories as applicable. Required builder
and delegate values cannot be unset/null; they may return to the explicit empty
preset. Optional fields reset by removing the Dart argument.

The unnamed variant accepts `SliverChildListDelegate`,
`SliverChildBuilderDelegate` and custom subclasses through typed project
references/getters/factories. Their full custom configuration stays in user Dart;
there is no visual child slot competing with the delegate. Dynamic, incompatible
and impermissibly nullable values fail candidate static-type evidence even when
the corresponding analyzer diagnostics are suppressed.

## Canvas, placement and persistence

All variants are slivers and require a Sliver-accepting parent slot such as
CustomScrollView.slivers or SliverPadding.sliver. The visual list accepts box
children, including an empty-list drop target. Canvas constructs the actual SDK
SliverFixedExtentList and keeps native fixed-extent sizing, axis/reverse/RTL and
scroll geometry. Selection and drop outlines remain clipped to the viewport;
move indexes are checked after removing the source child.

Isolated Canvas does not execute project builders, delegates or key-index code.
A visible preview-limit message names those fields; project items are not
previewed, while the extent and original references are retained. The generated
application executes the real references. No placeholder child is saved to FD.
At zero extent Flutter may avoid mounting even a non-empty child list; the
generated delegate and FD still retain the children.

All 14 fields participate in command validation, exact Dart generation, FD
round-trip and pair-save evidence. Property rows/groups retain identity after
edits, booleans use the shared checkbox editor, and reopened documents remain
editable. No FD/Catalog/Canvas/transport format bump is required.

## Inventory and verification

- 135 admitted palette definitions: 131 typed and four structural.
- 6,469 writable rows (6,451 outside Scaffold), 533 boolean rows,
  109 const-capable definitions.
- 123 insertion destinations (119 any-widget, four trait-constrained);
  16,605 placement candidates: 13,729 accepted, 2,876 rejected.
- Native Events unchanged: 150 across 36 widget types. All callables:
  189 across 45 types (150 Events, 27 Builders, seven Delegates,
  three Predicates, two Formatters).
- FD 16 / Catalog 15 / Canvas 19 / transport 1 unchanged.
- Twelve light/dark, 16/32 SVG icon resources distinguish list, builder and
  project-delegate entries.
- 171 dedicated native Canvas tests cover schema rejection, all axes/reverse/RTL,
  zero/default/large extents, default and explicit delegate flags, empty
  content, actual drop/selection, reordering, trailing scroll geometry and
  project-reference limitations.
- Full Flutter test run: 2,887 passed. Dart analyzer `lib test`: no issues.
- Real SDK test covers 18 generated application cases plus accepted/rejected
  static evidence, project getters/factories/custom delegates, builder early-null,
  nullable/known/unbounded counts, key lookup, source-preserving analysis,
  save/reopen, optional resets and Undo/Redo.

Automated verification on 2026-09-12: the full Designer-core suite completed
with 2,256 tests, 20 conditional skips, zero failures/errors and a successful
Maven exit. The targeted NetBeans regression completed with 669 tests, six
conditional skips, zero failures/errors and a successful Maven exit. The
separate enabled real-SDK test passed in 82.999 seconds and ran all 18 generated
application cases. The unrelated full NetBeans reactor was not repeated after
the previously recorded Windows AWT shutdown issue.

NBM install/package and development cluster build succeeded. The packaged schema
class and all twelve SVG variants match the current source/build. All 40 runner
sources and 35 Web artifact manifest entries were checked without relaxing
integrity rules. Web main.dart.js is 3,667,184 bytes, SHA-256
`7f7c07a61e374d66df4637b937cba67db67400b54bc764b135284b29432fc88f`.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(8,676,823 bytes), SHA-256:
`d017364eacae4bc6a0fd9ae38a12f8688ee83953186e37818ee22af833598ad9`.

Manual NetBeans desktop smoke testing and IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
