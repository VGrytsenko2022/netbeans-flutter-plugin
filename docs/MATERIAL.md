# Material contract and verification status

`flutter.material.Material` is admitted as a complete Flutter 3.44.8 vertical
slice.

- 12 typed constructor properties, in SDK order: `type`, `elevation`, `color`,
  `shadowColor`, `surfaceTintColor`, `textStyle`, `borderRadius`, `shape`,
  `borderOnForeground`, `clipBehavior`, `animationDuration` and `animateColor.
- Optional `child` is represented by the ordinary Slots editor and can contain
  any reviewed widget.
- `MaterialType` and `Clip` use strict enum editors. Colors accept ARGB values
  and reviewed Material `ColorScheme` roles. Shape/text-style/duration objects
  accept strict project/package references or explicit null.
- `borderRadius` and `shape` are mutually exclusive. Circle materials reject a
  non-zero radius; invalid combinations are diagnosed without mutating stored
  values.
- Generated Dart maps the collision-free designer `materialType` field to the
  SDK `type` argument and emits exact signed microseconds as
  `const Duration(microseconds: ...)`.
- Native Canvas mounts Flutter's `Material` widget. Application-owned object
  references remain visible and are diagnosed; project code is never executed
  inside the isolated preview.

Verification is covered by `MaterialContractTest`, `canvas_material_test.dart`,
the Canvas model/schema parity suite, palette/icon registry tests, and the full
`netbeans-plugin` module test suite. CJK IME and Linux/macOS Canvas providers
remain deliberately deferred until the complete palette inventory is finished.
