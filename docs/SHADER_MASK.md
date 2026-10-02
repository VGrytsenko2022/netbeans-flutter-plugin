# ShaderMask

Pinned API: Flutter 3.44.8. Palette: Basic → ShaderMask.

## Complete constructor and editor

The native constructor has Key, required ShaderCallback, BlendMode and optional
Child. Key remains the shared Designer identity policy. The two property rows
are Shader callback and Blend mode; Child is edited through its slot.

Shader callback offers either a structured gradient or a strictly typed source
reference. Its gradient-only editor reuses the existing typed geometry/color
controls, without exposing decoration-only fill, border, image or shadow fields.

- Linear: begin/end.
- Radial: center, radius, optional focal point and focal radius.
- Sweep: center, start/end angles.
- Every family: 2–256 stable-ID ordered stops, literal or theme colors, physical
  or directional alignment, clamp/repeated/mirror/decal tiling and rotation.
- All 29 BlendModes, with Flutter's modulate default; omission is preserved.

The local contract requires finite geometry, stops ordered within 0–1,
nonnegative radial radii, focalRadius zero when focal is absent, and startAngle
less than endAngle. Other shader constructions or custom GradientTransform
implementations remain available through ShaderCallback source code, rather
than being silently approximated by a local preset.

## Source callback and ownership

ShaderCallback is Shader Function(Rect bounds): a computation delegate in
Properties, not a native Event. Current-class/package references, getters,
members and zero-argument factories receive exact analyzer evidence against
the Rendering alias. Direct functions and compatible broader parameters work.
Wrong parameter/result types, nullable callbacks/results and dynamic result
paths are rejected before pair save.

Create Handler makes a working opaque-white gradient callback. Navigate, bind,
rename and disconnect preserve user source. Disconnect restores the structured
white gradient instead of invalid null; the detached method stays in user code.

Project callbacks can return gradient, image or fragment shaders. Project code
owns resource loading, uniforms, shader lifetime and backend requirements.
Designer does not add a general custom-code editor or a shader asset pipeline.

## Native painting and Canvas

ShaderMask treats the shader as the blend source and Child as the destination.
It changes pixels, not layout, hit testing or semantics. The callback receives
current local paint bounds; no child means no callback and no compositing.
Transparency does not remove the child's hit-test area.

Generated local gradients use an inner Builder to capture the nearest Theme
and Directionality during build. The callback closes over that gradient and
direction; it does not perform inherited-widget lookups during paint.
Theme/direction changes update the shader and retain the native element.

Canvas uses native ShaderMask for local gradients. It never executes project
callbacks: those receive an explicitly described opaque-white gradient while
retaining the chosen BlendMode. This fallback is not identity for every mode
(for example clear and src); no claim of exact project-shader preview is made.

## Storage and compatibility

FD 17 adds a closed standalone gradient value using the existing gradient
algebra. It is not persisted as a fake BoxDecoration. Catalog API is 16 and
Canvas model is 20; transport remains 1. Older schemas are frozen and migrate
to FD 17; pre-v17 documents cannot contain the new value. Old consumers reject
newer versions rather than losing data. Canvas source references remain
presence-only metadata without project library or member names.

## Verification

- Full Flutter Canvas suite: 11,503 passed; the 83 ShaderMask-specific tests also
  passed after final formatting. Flutter analyze reported no issues.
- Real Flutter 3.44.8 SDK: 192 saved/generated runtime scenarios passed,
  including all three gradient families, tiling, literal/theme colors, all
  blend modes, eight reference forms, direct/broader functions, source-handler
  lifecycle, nested Theme/Directionality, geometry changes and native identity.
  Nine invalid type/signature branches were rejected by the pair-save gate.
- FD tests preserve exact original bytes and migration results for all 216
  previous definitions, reject the new kind in legacy formats, and keep
  historical schemas frozen. Source callback identities stay out of Canvas.
- Both property rows retain identity on refresh; gradient-only edits stay local
  until confirmation. Four SVG variants pass geometry/paint/safety contracts.
- Combined fresh full-core and selected plugin/artifact reports: 3,938 passed,
  102 conditional tests skipped, zero failures/errors. This is not a claim
  that every optional SDK/desktop suite was enabled.
- Release Web Canvas built successfully and its 35-file manifest matches the
  output. The NBM contains the exact current schema/property/analyzer classes,
  frozen FD 16 plus FD 17 schemas, all four icon variants and all 40 runner
  sources. Its 35 Web artifact files match the manifest; all four development
  cluster JARs match the NBM. The running IDE was not restarted and interactive
  IDE behavior was not retested.

References: [ShaderMask](https://api.flutter.dev/flutter/widgets/ShaderMask-class.html),
[ShaderCallback](https://api.flutter.dev/flutter/rendering/ShaderCallback.html),
[Gradient.createShader](https://api.flutter.dev/flutter/painting/Gradient/createShader.html).
