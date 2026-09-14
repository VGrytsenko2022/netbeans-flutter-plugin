# FadeInImage

Pinned API: Flutter 3.44.8. One Basic palette definition, item order 290.
All 23 general-constructor parameters are editable; shared Key is unchanged.
Required placeholder and target image are independently configured. No child
slots and no fabricated native Events.

## Constructor coverage

| Group | Parameters |
| --- | --- |
| Images | placeholder, placeholderErrorBuilder, image, imageErrorBuilder |
| Animation | fadeOutDuration, fadeOutCurve, fadeInDuration, fadeInCurve |
| Appearance | color, colorBlendMode, placeholderColor, placeholderColorBlendMode, filterQuality, placeholderFilterQuality |
| Layout | width, height, fit, placeholderFit, alignment, repeat, matchTextDirection |
| Accessibility | excludeFromSemantics, imageSemanticLabel |

FD stores durations as fadeOutDurationUs/fadeInDurationUs; generation emits
Duration(microseconds: ...), or the exact typed Duration source reference.
Both curves expose the 43 reviewed Curves presets or a Curve source.

## Providers and constructor variants

Local mode reuses declared assets: AssetImage (DPR aware), ExactAssetImage
(explicit positive scale), package assets, and optional ResizeImage with
independent width/height, exact/fit policy and allowUpscaling. Project asset
inventory is used at drop time; absent assets do not block adding the widget.
Both required fields start with the editable built-in unresolved provider.

Project mode accepts ImageProvider<Object> reference/getter or zero-argument
factory, in current or declared project packages, optionally a class member.
NetworkImage (including headers), MemoryImage, FileImage, custom providers,
AssetBundle overrides, and provider composition remain in source code. No raw
URL, arbitrary path or byte-array expression is accepted by the local editor.

The assetNetwork and memoryNetwork factory combinations can be expressed with
these providers in the general constructor, including scales and decode/cache
sizes. They are **not separate named-constructor palette aliases** and there is
no URL/bytes form editor. Such source-owned providers are not executed in Canvas.

## Semantics and defaults

- Omission preserves native defaults: 300ms fade-out, 700ms fade-in, easeOut/
  easeIn, centered alignment, no repeat, medium target filtering, false
  excludeFromSemantics and matchTextDirection.
- Placeholder fit/filter quality inherit target values when null or omitted.
- Colors and blend modes are independent. ARGB, theme tokens, explicit null
  and typed Color? references are supported; width/height also accept double?.
- Both phases must be at least 1000 microseconds in the local editor.
  Flutter's TweenSequence uses strictly positive integer-millisecond weights;
  zero, negative or sub-millisecond phases fail native assertions. Project
  Duration values remain source-owned, including their runtime validity.
- Synchronous cached targets skip fading. Async targets fade the placeholder
  out first, then fade the target in. Native gapless replacement state stays
  intact across provider changes. No extra controller or timer is generated.
- Image painting natively clamps curve overshoot to valid opacity. Stored
  curves and generated Dart are not modified.
- Internal images are excluded from semantics; one image label describes the
  result unless excludeFromSemantics is true. No button/tap action is invented.

## Builders and analyzer proof

Both ImageErrorWidgetBuilder? rows are Properties, not Events. The existing
create, bind, navigate, rename and disconnect lifecycle applies. Signature:
Widget Function(BuildContext context, Object error, StackTrace? stackTrace).
Unset and explicit null preserve Flutter's native error reporting.

Strict analyzer witnesses reject dynamic values, nullable required providers,
raw/dynamic provider keys, wrong generic/signature types and dynamic builder
results. Proofs are analyzer-only: no project factory or callback is invoked
to validate it. Literal Duration symbols require exact trusted SDK core evidence.

## Isolated Canvas

Native FadeInImage is used with separately resolved, content-addressed image
resources, scale/resize configuration, color, layout and semantics. Both
resource IDs are tracked; diagnostics distinguish placeholder from target.
Project providers use the built-in image, source durations/curves use native
defaults, source alignment uses center, nullable dimensions/colors use native
fallbacks, and error builders use a safe preview. Tooltips name substituted
fields. Canvas does not run project code or fetch arbitrary URLs.

Zero-size images remain selectable. Boolean rows use existing checkboxes and
property refresh retains row identity. Source and local editor drafts commit
only on confirmation; cancellation retains the previous value.

## Verification

Coverage includes all 23 property rows, FD round trips, all 104 typed-reference
forms, independent assets/package/scale/resize, resource collection and source
anonymization, all 43 curve transitions, cached/async replacement, cleanup,
error builders, semantics, exact source proofs, undo/redo and reopen.
The real SDK fixture executes 30 generated/native cases; dedicated Canvas/native
tests execute 51 cases. Global suite/build/package results belong to the current
build report, not this fixed contract.

Formats unchanged: FD 16, Catalog API 15, Canvas model 19, transport 1.
Catalog after this slice: 210 definitions, 7325 property rows, 202 typed
definitions, 176 const-capable definitions; native Events 174/60 types,
all callables 239/87 types including 50 builders.

## Official references

- [General constructor](https://api.flutter.dev/flutter/widgets/FadeInImage/FadeInImage.html)
- [assetNetwork constructor](https://api.flutter.dev/flutter/widgets/FadeInImage/FadeInImage.assetNetwork.html)
- [memoryNetwork constructor](https://api.flutter.dev/flutter/widgets/FadeInImage/FadeInImage.memoryNetwork.html)
