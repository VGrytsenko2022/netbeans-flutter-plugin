# PreferredSize contract and verification status

`PreferredSize` is the next complete Layout palette slice after `RotatedBox`.
It is implemented against Flutter 3.44.8's const
`PreferredSize({required Size preferredSize, required Widget child})`
constructor from `package:flutter/widgets.dart`.

## Model and Properties

- `preferredSize` is one required typed `Size` value. The editor keeps width
  and height atomic and accepts only finite, non-negative values. New palette
  nodes start at `Size(100, 56)`; resetting an optional value is not offered
  because the SDK constructor requires this argument.
- `child` is one required single-widget slot. It is shown in the Slots tab and
  is never fabricated by the prototype or by a failed drop.
- The Properties sheet groups the size under **Preferred size** and uses the
  existing typed Size editor. The row remains writable while preserving the
  required-value invariant.

`PreferredSize` advertises a size to parents such as `AppBar` and `Scaffold`;
it does not impose constraints on its child. The saved model and generated
Dart preserve that distinction.

## Palette, DnD and generation

The widget is registered in the Layout palette after `RotatedBox`. It is a
required-child wrapper source: dropping it on an existing non-root widget
wraps that subtree atomically, while the required `child` slot is not treated
as an empty-list insertion destination. Slot editing, tree movement,
Save/reopen and Undo/Redo retain the child identity.

Generated code uses the native constructor and the exact stored size:

```dart
PreferredSize(
  preferredSize: Size(120.0, 64.0),
  child: child,
)
```

## Canvas

The native and exact-Web runners construct the real Flutter `PreferredSize`.
The advertised `Size` is passed through unchanged; the child is rendered as
the required nested widget. A zero-sized or otherwise non-painting result
still receives the existing bounded Designer selection/drop target outside
the Flutter layout, with no persisted wrapper or placeholder.

PreferredSize has no Events and no executable project callbacks. Its
`PreferredSizeWidget` trait remains available to the existing typed
`Scaffold.appBar` and `AppBar.bottom` destinations.

## Verification

Coverage is provided by:

- `PreferredSizeWidgetPropertySchemaTest` and
  `PreferredSizeDartGenerationTest` in `flutter-designer`;
- `canvas_preferred_size_test.dart` plus the runtime/static schema parity test
  in `flutter-canvas-runner`;
- the NetBeans palette, icon, Properties and capability-parity suites;
- the pinned source manifests for the native runner and the packaged Web
  artifact.

The intentionally deferred CJK IME and Linux/macOS Canvas provider work is
outside this widget slice and remains scheduled after the complete palette.
