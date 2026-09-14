# Static SliverList and SliverGrid constructors

Pinned SDK: Flutter 3.44.8, `packages/flutter/lib/src/widgets/sliver.dart`.
Official API: [SliverList](https://api.flutter.dev/flutter/widgets/SliverList-class.html)
and [SliverGrid](https://api.flutter.dev/flutter/widgets/SliverGrid-class.html).

## Admitted constructor surface

| Palette entry | Properties | Slot |
| --- | --- | --- |
| SliverList.list | addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes | required children list, may be empty |
| SliverGrid.count | required crossAxisCount; mainAxisSpacing, crossAxisSpacing, childAspectRatio | optional children list |
| SliverGrid.extent | required maxCrossAxisExtent; mainAxisSpacing, crossAxisSpacing, childAspectRatio | optional children list |

All parameters of these three constructors are supported except the shared
framework `key`, for which Designer uses stable model identity. The grid
creation values (2 columns / 200 logical pixels) are Designer presets, not
Flutter defaults. Counts, extents and aspect ratio must be positive; spacing
must be nonnegative. Optional unset values use Flutter defaults.

This document records the static slice. The remaining constructors are now
admitted in the [builder/delegate slice](SLIVER_DYNAMIC.md), with explicit
dynamic-preview limitations. They are never serialized as these static variants.

## Structure, rendering and persistence

All four admitted sliver definitions (including SliverToBoxAdapter) carry the
Sliver trait. They enter CustomScrollView.slivers, never document roots or
ordinary box slots. Their children accept ordinary widgets, including nested
scroll views, but reject direct slivers and Flex ParentData widgets.

Java placement validation and Flutter payload/DnD checks enforce the same
boundary. Dart generation uses the exact non-const named constructors.
FD save/decode retains all values, ordered children and stable IDs.

Canvas uses native SDK slivers without box-oriented instrumentation between
RenderSliver and viewport. Viewport-clipped surface geometry provides drop,
move and small selection handles, including empty slivers, horizontal and
vertical axes, and reverse scrolling. Handles do not add application widgets
or change generated layout. Property refresh preserves row and group identity;
optional booleans retain the shared checkbox/unset behavior.

## Verification

- SliverChildrenContractTest: constructor properties, defaults, placement.
- CustomScrollViewDartGenerationTest: all stored fields, exact constructors,
  non-const generation and FD round-trip.
- SliverChildrenPropertyContractTest: all editable fields, stable refresh,
  checkbox painting and reopened properties.
- FlutterDesignerPaletteDropPlannerTest: create slivers, populate children,
  reject incompatible destinations.
- canvas_model_test.dart: closed types, numeric boundaries, slot/root rejection.
- canvas_view_test.dart: 24 SDK rendering, drop, selection and move combinations.
- Shared catalog/schema parity and four-variant SVG resource tests.

Historical static-slice milestone: 122 definitions, 6,409 property rows, 506 boolean rows,
118 typed Properties definitions plus four structural slot-only definitions,
and 103 const-capable definitions. No file/protocol version changed.
