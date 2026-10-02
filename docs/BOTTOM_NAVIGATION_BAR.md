# BottomNavigationBar contract

`flutter.material.BottomNavigationBar` is implemented as a complete Flutter
3.44.8 palette vertical slice.

## Reviewed surface

- Twenty direct constructor properties in SDK order: `onTap`, `currentIndex`,
  `elevation`, `type`, `backgroundColor`, `iconSize`, selected/unselected
  colors and icon themes, selected/unselected font sizes and label styles,
  label visibility, `mouseCursor`, `enableFeedback`, `landscapeLayout` and
  `useLegacyColorScheme`.
- `items` is a required ordered list slot with model cardinality `0..10000`.
  The model permits an empty list for incremental editing; Flutter's native
  widget requires at least two items and Canvas reports that condition without
  mounting an assertion-failing widget.
- The Properties model uses the collision-free key `barType` and maps it to
  the SDK constructor argument `type` during Dart generation. Colors accept
  literals, reviewed Material `ColorScheme` tokens, typed `Color` references
  and explicit null. Icon themes, label styles and mouse cursors accept strict
  typed references or null.

## Canvas boundary

Canvas mounts the native `BottomNavigationBar` for valid item lists, preserving
selection, type, colors, sizes, label policy, landscape layout, feedback and
legacy color behavior. Nested item widgets become deterministic
`BottomNavigationBarItem` icons so the isolated preview stays executable while
generated application Dart retains their original order and identities.
Project callbacks and application-owned icon-theme, label-style or cursor
references are never executed; the preview uses SDK/theme fallback and shows a
diagnostic while retaining the exact source values.

## Verification

`BottomNavigationBarContractTest` covers constructor order/defaults, the
internal `barType` to SDK `type` mapping, typed capability projection, slot
cardinality and generated item order. `canvas_bottom_navigation_bar_test.dart`
covers protocol parity, native rendering, field preservation, empty-list
diagnostics and reference fallback. Palette DnD, Properties, Events,
Save/reopen, history and the registered light/dark SVG icons use the same
contract.

Physical CJK IME acceptance and Linux/macOS Canvas SPI providers remain
deliberately deferred until the complete palette inventory, as planned.
