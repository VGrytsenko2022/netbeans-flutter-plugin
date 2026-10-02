# BackdropFilter and BackdropGroup

Pinned SDK: Flutter 3.44.8. Basic admits BackdropFilter (26 property rows),
BackdropFilter.grouped (25), and BackdropGroup (one): 52 new editable rows.
Both filter constructors are const-capable; BackdropGroup is not. Shared Widget
Key handling is unchanged. FD 16, Catalog API 15, Canvas model 19 and transport 1
are unchanged.

## Constructor coverage

- BackdropFilter: Filter / Filter config, optional Child, Blend mode, Enabled,
  and nullable Backdrop group key.
- BackdropFilter.grouped: the same except there is no explicit group-key
  parameter. It uses the nearest BackdropGroup; without one it uses no key,
  exactly as Flutter does.
- BackdropGroup: required Child and nullable Backdrop key. Palette creation
  wraps an existing child atomically; an incomplete group cannot be inserted.
  Omission/null creates a native key. An application-owned key can be supplied
  through a typed reference.

## Filters and configurations

Filter reuses all six ImageFilter factories from [ImageFiltered](IMAGE_FILTERED.md):
blur with optional fixed bounds, dilate, erode, complete 16-entry matrix with
FilterQuality, typed inner/outer composition, and caller-owned FragmentShader.
Finite signed numeric values are preserved. TileMode retains null/omission for
low-level blur; the native engine decides degenerate-filter behavior.

Filter config supports all three ImageFilterConfig factories:

- wrap: adapts the current Filter, including any of its six factories or a typed
  ImageFilter source. Requires a non-null Filter.
- blur: independent sigma X/Y, all four non-null TileModes (default clamp), and
  Bounded sampling (default false). A bounded config resolves the current render
  bounds during painting; it does not reuse fixed Rect drafts.
- compose: typed non-null inner and outer ImageFilterConfig references,
  including arbitrary nested application-owned compositions. Omitted members
  use zero-sigma config blur. Order is outer(inner(input)).

A typed non-null ImageFilterConfig can replace the entire local config.
Non-null Filter config takes precedence over Filter: only filterConfig is
emitted, while Filter and its drafts are preserved. Null/omitted config activates
Filter; null/omitted Filter is then invalid. The generator never emits two
non-null native arguments. This precedence permits safe single-row switching.

Enabled is a centered checkbox, omission true, no null. Filter expressions
are still evaluated when false. Shader therefore still requires Impeller;
the application owns shader assets, uniform setup, lifetime and disposal.

## Painting, grouping and Canvas

BackdropFilter modifies content painted before it, then paints Child.
ImageFiltered instead modifies Child pixels. Backdrop output extends to the
ancestor clip (or the full canvas if there is no clip). Use ClipRect/ClipRRect
explicitly to limit output. Bounded sampling controls the input area, not output
clipping. Designer does not silently add clips or save layers.

All 29 BlendModes are offered, with srcOver as the default. Only srcOver is
guaranteed on all Flutter backends; src may be useful below Opacity/saveLayer.
Backend limitations, including platform-view restrictions, remain Flutter's.

Grouped filters share a backdrop input, not widget keys. Overlapping filters
should not share a BackdropKey. Nested BackdropGroups use their nearest key.
Native filtering preserves layout, hit testing, semantics and render identity.
Unlike ImageFiltered, the pinned RenderBackdropFilter keeps its compositing
requirement while it has a child even when disabled; disabled skips painting the
filter operation.

Canvas renders all local filters/configs with real Flutter widgets, including
group inheritance and layout-bounded blur. It never executes project sources:
whole filter/config, shader and source composition use neutral filters, source
matrix uses identity, source bounds use null. Explicit source-owned filter keys
are not shared in Canvas. BackdropGroup with a source key instead creates a local
key so its grouped descendants still work. Tooltips identify active substitutions.

## Source, storage and verification

Current-class and package references, members, getters and zero-argument
factories receive exact static proofs for ImageFilter, ImageFilterConfig,
FragmentShader, Float64List, Rect? and BackdropKey?. Config/key proofs resolve
through a fixed Rendering witness, not an ambient same-named type. Dynamic,
wrong types and nullable configs/filters are rejected before saving.

Inactive values and imported-source drafts survive FD save/reopen, reset,
override and history. Only active references create imports and symbol probes;
even an absent package in an inactive draft cannot block saving. Activation
requires proof before pair save. Canvas receives presence-only metadata, not
project library/member identities.

Tests cover all 52 stable writable rows, six/three factory branches, exact
schemas and source types, grouped key lookup, nullable keys, preservation of
drafts, save/reopen/undo/redo, native background/foreground pixels, clipping,
layout bounds, input and semantics. No native Events are invented.

## Verification record (2026-09-15)

- Flutter Canvas: all 11,420 tests passed, including 64 new Backdrop tests.
- `flutter analyze`: no issues; release Web Canvas built successfully.
- Real pinned SDK: both constructors passed 160 typed-source reference forms
  and 354 generated/native runtime scenarios in isolated projects. This includes
  negative type proofs, inactive missing-package drafts and pair-save history.
- Combined full-core and selected plugin/artifact reports: 3,919 passed,
  102 conditional tests skipped, zero failures/errors. Optional desktop/SDK
  suites were not all enabled.
- Final NBM: schema/property/analyzer classes, 12 icon variants and all 40
  packaged runner sources verified against the checkout; 35 Web artifact files
  match the manifest. All four development-cluster JARs match the NBM.
  The running IDE was not restarted; interactive IDE behavior was not retested.

References: [BackdropFilter](https://api.flutter.dev/flutter/widgets/BackdropFilter-class.html),
[BackdropGroup](https://api.flutter.dev/flutter/widgets/BackdropGroup-class.html),
[ImageFilterConfig](https://api.flutter.dev/flutter/rendering/ImageFilterConfig-class.html),
[bounded blur](https://api.flutter.dev/flutter/rendering/ImageFilterConfig/ImageFilterConfig.blur.html).
