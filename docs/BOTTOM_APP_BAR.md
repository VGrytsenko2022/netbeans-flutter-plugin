# BottomAppBar contract

`flutter.material.BottomAppBar` is implemented as a complete palette vertical
slice for Flutter 3.44.8.

## Reviewed surface

- Nine constructor properties: nullable theme-aware `color`, nullable
  non-negative `elevation`, nullable `NotchedShape` `shape`, non-null `Clip`
  `clipBehavior`, non-negative `notchMargin`, nullable `EdgeInsetsGeometry`
  `padding`, nullable `surfaceTintColor`, nullable `shadowColor` and nullable
  non-negative `height`.
- Optional `child` single-widget slot, accepted by the ordinary `AnyWidget`
  placement rule.
- Color fields accept literals, reviewed Material `ColorScheme` tokens, typed
  `Color` references and explicit null. `shape` accepts a typed nullable
  `NotchedShape` reference. Padding accepts physical/directional insets, a
  typed `EdgeInsetsGeometry` reference or null.

## Canvas boundary

The native Flutter `BottomAppBar` is rendered with stored colors, elevation,
clipping, notch margin, padding, shadow and height, together with its child.
Application-owned `NotchedShape` references are preserved for generated Dart
and persistence, but are not executed by the isolated Canvas; a visible
diagnostic accompanies the rectangular SDK fallback.

## Verification

The slice is covered by `BottomAppBarContractTest` and
`canvas_bottom_app_bar_test.dart`, including catalog/protocol parity, palette
DnD, typed generation, child identity, null/default behavior and the reference
fallback diagnostic. SVG light/dark palette icons are registered separately.
