# NavigationRail contract

The `flutter.material.NavigationRail` palette item is a complete Flutter
3.44.8 vertical slice across the Java catalog, generated Dart, native Canvas,
palette DnD, Properties and persistence paths.

## Scope

- 20 typed constructor properties in SDK order: theme-aware background color,
  extension and selection, destination callback, elevation/alignment, label and
  icon themes, minimum widths, indicator controls, pinned leading/trailing
  behavior, scrolling and main-axis alignment.
- `selectedIndex` is a required nullable argument and starts at `0`; explicit
  null means no destination is selected. Values outside the current destination
  list are rejected by validation and previewed as no selection, without
  clamping or rewriting the stored value.
- `leading` and `trailing` are optional single any-widget slots. `destinations`
  is a required `0..10000` list slot and may be empty in the model. The actual
  Flutter constructor receives `NavigationRailDestination` objects; Canvas
  adapts each nested slot widget as its icon and supplies a deterministic
  `Destination N` label. Generated application Dart retains the exact nested
  widget identities and source-owned destination construction.
- `onDestinationSelected` accepts a typed `ValueChanged<int>` callback/reference,
  explicit null or the Designer no-op stub. Project callbacks are never
  executed by Canvas.
- Canvas reports the SDK constructor conflicts for `extended` with a non-null,
  non-`none` `labelType`, and for `minExtendedWidth < minWidth`. It normalizes
  only the isolated preview; saved values and generated source remain exact.
- TextStyle, IconThemeData and ShapeBorder references remain application-owned:
  Canvas uses the NavigationRail theme/SDK fallback and discloses that boundary.
  Literal/theme colors, booleans, enum values and numeric ranges are edited
  directly with nullable omission preserved.

## Verification

`NavigationRailContractTest` covers constructor order, the required nullable
selection default, slot cardinality, reference contracts, generation and tree
validation. `canvas_navigation_rail_test.dart` covers protocol schema parity,
three drop slots, valid native rendering, synthetic destination labels, empty
lists, nullable selection, invalid-index safety and extended-label normalization.
The full palette drop matrix and NetBeans plugin suite include the definition
and its light/dark SVG registry icons.

Physical CJK IME acceptance and Linux/macOS Canvas SPI providers are intentionally
outside this slice and remain deferred until the complete palette inventory.
