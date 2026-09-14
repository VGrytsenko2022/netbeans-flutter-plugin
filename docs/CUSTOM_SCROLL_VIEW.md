# CustomScrollView and SliverToBoxAdapter contract

Flutter Designer targets the pinned Flutter 3.44.8 `widgets.CustomScrollView`
constructor together with `widgets.SliverToBoxAdapter` as one complete sliver
vertical slice.

## Properties and slots

`CustomScrollView` exposes the reviewed scrolling surface: `scrollDirection`,
`reverse`, `controller`, `primary`, a closed `ScrollPhysics` preset,
`shrinkWrap`, `anchor`, `scrollCacheExtent`, `paintOrder`,
`semanticChildCount`, `dragStartBehavior`, `keyboardDismissBehavior`,
`restorationId`, `clipBehavior` and `hitTestBehavior`. The `slivers` list is
typed and accepts only widgets carrying the Designer's `flutter.widgets.Sliver`
trait.

`SliverToBoxAdapter` is the first supported sliver constructor. It has one
optional any-widget `child` slot. The adapter is valid in a `CustomScrollView`
slivers list; ordinary widgets are rejected there so the model cannot produce
the Flutter `RenderSliver`/`RenderBox` mismatch.

## Generation, Canvas and verification

Generated Dart emits `CustomScrollView(slivers: [...])` and
`SliverToBoxAdapter(child: ...)` with the stored typed values. Root scrolling
views receive the same bounded preview guard used by the other static scroll
views. Canvas mounts the native SDK viewport and adapter. Placement validation
rejects detached sliver document roots and slivers in ordinary box slots.
Adapter instrumentation avoids box-only wrappers around RenderSliver; external
viewport-clipped selection/drop handles also cover empty adapters.
See the [static SliverList/SliverGrid slice](SLIVER_CHILDREN.md) for the three
additional admitted constructors and their explicit dynamic-constructor limits.

`CustomScrollViewDartGenerationTest`, `canvas_model_test.dart` and
`canvas_view_test.dart` cover schema, trait acceptance, generated Dart, native
fields, ordered slivers and safe rendering. Full Maven and Flutter runner
contracts include the palette counts and source-manifest hashes.
