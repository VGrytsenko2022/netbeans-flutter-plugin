# NavigationBar contract

The `flutter.material.NavigationBar` palette item is a complete Flutter 3.44.8
vertical slice across the Java catalog, generated Dart, native Canvas, palette
DnD, Properties and persistence paths.

## Scope

- 15 typed constructor properties, in SDK order: animation duration,
  selected index, destination callback, four colors, elevation, indicator
  shape, height, label behavior, overlay color, label text style, label
  padding and bottom-view-padding preservation.
- `selectedIndex` is required and starts at `0`; edits remain non-negative and
  are validated against the destination list without clamping.
- `destinations` is a required list slot with a `0..10000` model cardinality;
  nested destinations are ordinary widgets. Flutter requires at least two
  destinations at runtime, so the Canvas reports a deterministic diagnostic
  for shorter lists instead of mounting an SDK assertion.
- `onDestinationSelected` accepts a typed callback/reference, explicit null or
  the Designer no-op stub. Project callbacks are never executed by Canvas.
- Theme-aware omission/null behavior is preserved for colors, shape, height,
  labels, animation duration and bottom view padding. Exact signed duration
  values are retained through source generation and the model.

## Verification

The Java contract test covers property order/defaults, slot cardinality,
callback kinds, generation and tree validation. The Flutter runner test covers
protocol schema parity, valid native rendering/selection and the empty-list
diagnostic. The full palette drop matrix and NetBeans plugin suite include the
new definition and its SVG registry icon.

The implementation intentionally does not include physical CJK IME acceptance
or Linux/macOS Canvas SPI providers; those remain after the complete palette
inventory, as planned.
