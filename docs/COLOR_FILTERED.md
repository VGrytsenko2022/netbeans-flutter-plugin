# ColorFiltered

Pinned SDK: Flutter 3.44.8. Catalog type: `flutter.widgets.ColorFiltered`, Basic.
All constructor arguments other than the shared Key are covered: required
ColorFilter and optional Child. The filter is projected into 24 editable rows.
FD 16 / Catalog 15 / Canvas 19 / transport 1 remain unchanged.

## Local filters and Properties

- **Mode:** ARGB, Material color token or strict non-null Color source, with all
  29 BlendMode values. Omitted color is transparent; omitted mode is srcOver.
  The selected color is the source and child pixels are the destination.
- **Matrix:** all 20 finite signed coefficients, in row-major 4 x 5 order.
  Rows produce R/G/B/A; columns consume R/G/B/A/offset. Offset units are
  unnormalized 0..255, with negative offsets allowed. Omitted coefficients
  reconstruct identity (diagonal 1, everything else 0).
- **Linear to sRGB gamma** and **sRGB to linear gamma:** both native conversions.
- **Saturation:** finite signed value; default 1, zero is grayscale, values over
  1 increase saturation. Negative values are retained as accepted by the SDK.
  This native factory is non-const; the other four families are const-capable.

The required selector accepts a local preset or a typed non-null ColorFilter
getter/reference/zero-argument factory. Creation selects the identity matrix.
Null/unset is not valid for the selector. Optional local leaves can be reset
to omission, but do not accept explicit null.

Only the selected branch is generated and analyzed. Inactive fields remain
stored in the FD model, survive save/reopen and history, and become active again
when switching back. Activating a draft with a wrong/dynamic/nullable source
fails the strict analyzer gate. A whole ColorFilter source overrides every local
draft. No arbitrary inline Dart or runtime project code is accepted by Canvas.

## Source ownership and preview

ColorFilter and Color sources support current-class and package members, both
references and zero-argument factories. Wrong types, dynamic and nullable values
are rejected when active. Project code owns its filter values and lifecycle.

Canvas uses the same native ColorFiltered widget for all five local branches.
A project-owned ColorFilter previews as identity; active source Color previews
as transparent. Property-specific tooltips disclose these substitutions.
Source expressions, URIs and member names are not sent to Canvas; inactive
source values do not cause misleading preview warnings.

A filter changes painting/compositing only. Child constraints, hit testing and
semantics remain native. Child may be absent; the zero-size node remains
selectable in Designer. There are no native Events. Wrap the child with an
appropriate input widget when events are needed.

## Verification

Coverage includes 24-row schema and stable Properties identity, every filter
and BlendMode, signed matrix/saturation values, all 16 source-reference forms,
inactive draft activation and source override, FD save/reopen/reset/history,
generated Dart execution, and source-payload privacy. Native pixel tests cover
identity, blend, channel-swapping matrix, grayscale saturation and both gamma
conversions; layout, tap/semantics preservation and render-object reuse are
also checked.

References: [constructor](https://api.flutter.dev/flutter/widgets/ColorFiltered/ColorFiltered.html),
[ColorFilter factories](https://api.flutter.dev/flutter/dart-ui/ColorFilter-class.html),
[matrix](https://api.flutter.dev/flutter/dart-ui/ColorFilter/ColorFilter.matrix.html),
[saturation](https://api.flutter.dev/flutter/dart-ui/ColorFilter/ColorFilter.saturation.html).
