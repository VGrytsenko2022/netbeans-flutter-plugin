# NavigationDrawer contract

The `flutter.material.NavigationDrawer` palette item is a complete Flutter
3.44.8 vertical slice across the Java catalog, generated Dart, native Canvas,
palette DnD, Properties and persistence paths.

## Scope

- Nine typed constructor properties in SDK order: theme-aware background,
  shadow and surface-tint colors, elevation, indicator color and shape,
  destination callback, nullable selected index and tile padding.
- `selectedIndex` is a required nullable argument and starts at `0`; explicit
  null means no destination is selected. Values outside the current list are
  diagnosed in preview without clamping or rewriting the stored value.
- `children` is a required `0..10000` list slot and may be empty while the
  drawer is assembled. `header` and `footer` are optional single any-widget
  slots. Canvas adapts each nested child to a deterministic
  `NavigationDrawerDestination` with a synthetic label and bounded icon box;
  generated application Dart retains the original nested widget identities.
- `onDestinationSelected` accepts a typed `ValueChanged<int>` callback/reference,
  explicit null or the Designer no-op stub. Project callbacks are never
  executed by Canvas.
- Omitted/null colors, elevation, indicator shape and tile padding retain the
  NavigationDrawer theme/SDK defaults. Application-owned ShapeBorder and
  callback references remain represented in the model and are disclosed as
  isolated-preview boundaries.

## Verification

`NavigationDrawerContractTest` covers constructor order, nullable selection,
slot cardinality, callback handling and deterministic generation.
`canvas_navigation_drawer_test.dart` covers protocol schema parity, all three
drop slots, valid native rendering, synthetic destination labels, empty lists,
nullable selection and invalid-index safety. The full palette and NetBeans
plugin suites include the definition and its light/dark SVG registry icons.

Physical CJK IME acceptance and Linux/macOS Canvas SPI providers are
intentionally outside this slice and remain deferred until the complete
palette inventory.
