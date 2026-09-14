# Drawer contract

`flutter.material.Drawer` is implemented as a complete palette vertical slice
for Flutter 3.44.8.

## Reviewed surface

- Eight nullable constructor properties: `backgroundColor`, `elevation`,
  `shadowColor`, `surfaceTintColor`, `shape`, `width`, `semanticLabel` and
  `clipBehavior`.
- Optional `child` single-widget slot, accepted by the ordinary `AnyWidget`
  placement rule.
- Color fields accept literals, reviewed Material `ColorScheme` tokens,
  typed `Color` references and explicit null. Numeric fields accept finite
  non-negative integer/double values or null. `shape` is a typed nullable
  `ShapeBorder` reference; `clipBehavior` is the SDK `Clip` enum.

## Canvas boundary

The native Flutter `Drawer` is rendered with stored colors, elevation, width,
semantic label, clipping and child content. Application-owned `ShapeBorder`
references are preserved for generated Dart and persistence, but are not
executed by the isolated Canvas; a visible diagnostic accompanies the SDK
fallback shape.

## Verification

The slice is covered by `DrawerContractTest` and
`canvas_drawer_test.dart`, including catalog/protocol parity, palette DnD,
typed generation, child identity, null/default behavior and the reference
fallback diagnostic. SVG light/dark palette icons are registered separately.
