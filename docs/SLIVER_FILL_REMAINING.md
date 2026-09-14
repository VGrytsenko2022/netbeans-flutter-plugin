# SliverFillRemaining

Reviewed against Flutter 3.44.8:
[constructor](https://api.flutter.dev/flutter/widgets/SliverFillRemaining/SliverFillRemaining.html),
[class](https://api.flutter.dev/flutter/widgets/SliverFillRemaining-class.html),
and the pinned SDK widgets/sliver_fill.dart, rendering/sliver_fill.dart and
widgets/sliver_fill_remaining_test.dart.

## Complete constructor

| Field | Designer editor | Omitted SDK value |
| --- | --- | --- |
| hasScrollBody | Boolean checkbox / unset | true |
| fillOverscroll | Boolean checkbox / unset | false |
| child | Optional single box-child slot | null |

Both booleans reject explicit null and non-boolean values. Creation leaves them
unset to retain Flutter defaults. Reset Default removes the named argument.
Checkboxes use the shared centered painted/in-place implementation; changing
values retains the existing row and property groups, including after reopen.
Shared stable Designer identity remains separate from framework key editing.
There are no native callback parameters.

The widget is a sliver: only Sliver-trait slots (CustomScrollView.slivers or
SliverPadding.sliver) accept it. Its own child accepts ordinary box widgets,
not slivers or direct Flex parent-data children such as Expanded/Flexible/Spacer.
The optional slot may remain empty; occupied-slot replacement is not implicit.

## Native layout

The Canvas creates Flutter's actual SliverFillRemaining, without a layout
substitute. Generated Dart preserves both flags and child identity/constness.

- Scroll-body mode retains Flutter's viewport sizing for a potentially
  scrollable child. The fillOverscroll value is preserved but has no effect.
- Non-scroll-body mode uses the child's intrinsic extent and preceding sliver
  extent. Large children can exceed the viewport; preceding slivers can consume
  more than a viewport. Zero intrinsic size is valid.
- Non-scroll-body plus fillOverscroll uses Flutter's overscroll-aware maximum
  constraints. Under BouncingScrollPhysics a child able to expand can stretch
  into trailing overscroll; a fixed-size child need not occupy that maximum.
  This is not a generic guarantee that any child grows for any overscroll.
- A viewport-based child that cannot compute intrinsic dimensions requires
  hasScrollBody=true (or an appropriate explicit size wrapper). The Designer
  does not silently override the selected flag or execute project code.

Selection, drop and move previews use native RenderSliver geometry and viewport
clipping for both axes, LTR/RTL, reverse and empty children. Reversed
scroll-body transforms are compared with a direct native reference widget,
including its distinction between full scrollExtent and reduced paintExtent.
A usual location is the last sliver, but the SDK does not require this and the
Designer adds no artificial last-child restriction.

## Verification scope

- Every flag in unset/true/false states, exact generation, FD round-trip,
  null/type rejection, box/sliver admission and empty/occupied slots.
- Checkbox presentation, stable row/group identity, reset and reopen.
- 144 native Canvas combinations of both flag states, axes, reverse, RTL and
  empty/nonempty child; 18 intrinsic/preceding-extent cases; overscroll branches,
  a real nested ListView, selection and move/drop rejection.
- Real SDK candidate analysis, file-preserving evidence gate, default/explicit
  flag generation, 18 saved/reopened application variants, reset/Undo/Redo and
  execution through Flutter widget tests.
- Shared catalog/capability/Events inventory, all-boolean editor, accessibility,
  icon registry, wrapper planners, protocol parity and strict resource manifests.

Current inventory: 130 admitted definitions, 126 typed + 4 structural,
6,443 writable rows (6,425 outside Scaffold), 520 boolean rows and 107
const-capable definitions. Drop matrix: 15,730 candidates, 13,489 accepted,
2,241 rejected. Native Events remain 150 rows across 36 widget types; all
callables remain 186 across 43 types. FD 16 / Catalog 15 / Canvas 19 /
transport 1 are unchanged.

Automated run on 2026-09-12: analyzer `lib test` clean; all 2,409 Flutter
tests passed. Full Designer-core tests passed. The final targeted NetBeans
regression wrote 653 tests (4 conditional skips), zero failures/errors and a
successful Maven exit. The separate enabled real-SDK test passed and ran all
18 generated application variants. The unrelated full NetBeans reactor was
not repeated after its previous Windows AWT shutdown hang.

Manual NetBeans desktop smoke testing and IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain outside this slice.
