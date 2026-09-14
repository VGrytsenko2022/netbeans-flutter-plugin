# ImageFiltered

Pinned SDK: Flutter 3.44.8. Catalog type: `flutter.widgets.ImageFiltered`, Basic.
All constructor parameters except the shared Key are supported: required
ImageFilter, optional Child and Enabled. The filter has 17 editable property
rows, with no new FD wire kind or protocol version.

## Factories and local values

- **blur:** sigma X/Y, all four TileMode values or native null/omission, and
  optional bounded blur. Local bounds expose left/top/width/height; when any
  coordinate is set, omitted coordinates use zero. A Rect? source or explicit
  null overrides all local bounds without deleting those drafts.
- **dilate / erode:** independent X/Y morphology radii.
- **matrix:** complete 16-entry column-major editor and all four FilterQuality
  values (default medium). Local values become a fresh Float64List. A strict
  Float64List source is also supported; caller-owned lists must have exactly
  16 finite entries. This is not the 4 x 5 color matrix from ColorFiltered.
- **compose:** independent, non-null inner and outer ImageFilter sources.
  The result is outer(inner(input)). Each omitted source uses zero-sigma blur.
  Source-owned compositions may nest arbitrarily, including ColorFilter values.
- **shader:** a non-null caller-owned FragmentShader source. Configure Shader
  before selecting this factory. It requires Impeller; the shader must reserve
  its first vec2 uniform and a sampler2D for the engine. Project code owns asset
  compilation, uniforms, lifetime and disposal. Unsupported backends retain
  Flutter's UnsupportedError; generated application code is not silently changed.

Numeric controls preserve finite signed SDK values, including negative radii
and empty/inverted bounds, without Designer clamping. Matrix storage is finite;
the engine retains responsibility for native degenerate-filter behavior.

All six factories are non-const. ImageFiltered itself is const-capable, for
example with a const source-owned ColorFilter subtype.

## Properties, source and history

The required selector is a six-preset editor or a typed ImageFilter source.
Enabled uses the shared centered checkbox, permits omission (native true), and
does not permit null. False bypasses the filter layer, but does not prevent the
Dart filter expression from being evaluated; shader/platform requirements still
apply when constructing that expression.

Only the active filter branch is emitted and analyzed. Inactive drafts stay in
FD through save/reopen, source override, reset and undo/redo. Activating a bad
source draft is rejected before pair save. Current-class/package references,
members, getters and zero-argument factories all receive exact static proofs:
ImageFilter, nullable Rect, Float64List and FragmentShader. Dynamic and wrong
types are not treated as safe witnesses.

## Canvas and native semantics

Canvas never executes project source or serializes expression/member identities.
A whole filter source or shader is shown as zero-sigma blur. Source bounds use
null, source matrix uses identity, and source composition members use identity
filters. Tooltips identify only substitutions relevant to the selected branch.
All local blur/morphology/matrix settings use the native ImageFiltered widget.

Enabled false removes the filter's compositing requirement without replacing
the render object. Filters affect pixels, not layout, hit testing or semantics.
Empty Child is valid and remains selectable in Designer. There are no native
Events; input belongs to Child or an explicit input wrapper.

Verification covers the 17 Properties and row identity, all factory/enum
branches, source privacy, strict source proofs, negative type probes, inactive
draft activation, save/reopen/history, native pixels, composition order, matrix
storage copying, Enabled layer behavior and the shader backend contract.

## Verification record (2026-09-15)

- Flutter Canvas: 11,356 tests passed; 39 ImageFiltered-specific tests passed
  again after switching compositing assertions to the public render API.
- `flutter analyze`: no issues; release Web Canvas built successfully.
- Real pinned SDK: 103 generated/native runtime scenarios plus one shader
  backend contract passed, including strict source proof and pair-save history.
- Combined core and selected plugin gate reports: 3,911 passed, 102 conditional
  tests skipped, zero failures/errors. This is not a claim that every optional
  SDK/desktop suite was enabled.
- Final NBM: schema and property classes, four icon variants, all 40 runner
  sources and 35 Web artifact files verified; all four development-cluster
  JARs match the package. The running IDE was not restarted.

References: [ImageFiltered constructor](https://api.flutter.dev/flutter/widgets/ImageFiltered/ImageFiltered.html),
[ImageFilter factories](https://api.flutter.dev/flutter/dart-ui/ImageFilter-class.html),
[blur bounds](https://api.flutter.dev/flutter/dart-ui/ImageFilter/ImageFilter.blur.html),
[shader requirements](https://api.flutter.dev/flutter/dart-ui/ImageFilter/ImageFilter.shader.html).
