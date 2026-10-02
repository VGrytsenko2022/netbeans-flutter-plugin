# RawImage

Pinned SDK: Flutter 3.44.8. Catalog type: `flutter.widgets.RawImage`, Basic.
No child slots or native Events. All 16 constructor parameters excluding the
shared Key are editable; FD 16 / Catalog 15 / Canvas 19 / transport 1 remain unchanged.

## Coverage

- Decoded image: nullable typed `dart:ui.Image` getter/reference/zero-argument
  factory, local null or omission. Widget Image and ImageProvider are rejected.
- Debug label: string/null/unset, diagnostic only.
- Width/height: nonnegative local number or nullable double source.
- Scale: positive finite local number or non-null double source, default 1.
- Color: ARGB, theme token, nullable Color source, null/unset.
- Opacity: local 0..1 as AlwaysStoppedAnimation, nullable Animation<double>
  source, null/unset. Project code owns animation/controller lifetime.
- BlendMode, BoxFit, ImageRepeat and FilterQuality: complete SDK enum sets;
  exact nullable/default behavior. All three boolean flags use checkboxes.
- Alignment: physical/directional coordinates or typed AlignmentGeometry.
- Center slice: nullable Rect source (including source factories using
  Rect.fromLTRB/fromLTWH), null/unset. No separate local rectangle grid.

Project image decoding and the original image handle remain caller-owned.
RawImage uses its own render handle; the caller must dispose the original only
after it is no longer needed. Dynamic expressions, wrong image types and
raw/dynamic animation values cannot pass the strict analyzer gate.
Nine-patch bounds and source-dependent dimensions cannot be statically validated:
project code must keep them within the real image and use a compatible fit.

## Canvas and verification

Canvas never executes project factories or transfers native image handles.
Source image, opacity and center slice preview as null; other source inputs use
documented defaults. The tooltip identifies every substituted property.
An unconfigured RawImage paints nothing, as Flutter does, and remains selectable.

Coverage includes all 64 source-reference forms, local/null values, enum domains,
save/reopen/history, property-row identity, payload privacy and generated Dart
execution. Native tests check image replacement/clone lifetime, opacity listener
cleanup, all paint parameters, intrinsic scale and debug-label semantics.

References: [constructor](https://api.flutter.dev/flutter/widgets/RawImage/RawImage.html),
[image ownership](https://api.flutter.dev/flutter/widgets/RawImage/image.html).
