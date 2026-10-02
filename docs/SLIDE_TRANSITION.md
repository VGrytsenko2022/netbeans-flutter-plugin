# SlideTransition

Pinned SDK: Flutter 3.44.8; verified against
packages/flutter/lib/src/widgets/transitions.dart.
Official [constructor](https://api.flutter.dev/flutter/widgets/SlideTransition/SlideTransition.html)
and [API](https://api.flutter.dev/flutter/widgets/SlideTransition-class.html).

## Complete constructor projection

All three scalar constructor arguments and the optional box Child are supported.
Key follows the shared Designer identity policy. The constructor has no Duration,
Curve, On end, builder or AnimationController arguments.

- Position animation is required. Local X/Y are finite signed fractions of child
  width/height, not pixels; they may exceed [-1,1]. Creation uses zero.
  Generation emits `const AlwaysStoppedAnimation<Offset>(const Offset(dx, dy))`.
  Local changes are immediate, not implicitly animated.
- Project mode accepts strict non-null `Animation<Offset>` through current-library
  or package-imported references/getters/zero-argument factories, with optional
  member access. Plain Offset, nullable animation/value types, wrong generic
  types, Animation<dynamic>, dynamic and ValueNotifier<Offset> are rejected by
  source evidence. The generator preserves symbol occurrence offsets and user code.
- Transform hit tests supports unset (Flutter true) and centered true/false
  checkboxes. False keeps the original pointer hit location, not the painted one.
- Text direction supports unset, explicit null, LTR and RTL. Unset/null applies
  physical X even under ambient RTL. Explicit RTL negates X, never Y.
- Child is optional. Box/parent-data constraints apply; add, replace, move,
  remove and wrapping use shared transactional commands.

The structured editor labels width/height fractions explicitly, validates both
coordinates, and stages local/source drafts until commit. Cancellation preserves
the prior property. Refresh retains group/row identity; reopened values remain
editable, with optional properties resettable.

## Native behavior and isolated preview

Generated source uses Flutter's SlideTransition / FractionalTranslation and live
project animation values. AnimatedWidget registers and replaces listeners and
detaches them on disposal. Controllers, status listeners, tween/curve composition
and ownership remain in ordinary user Dart; the Designer creates none implicitly.

Canvas uses the native widget with stopped local offsets. Project code crosses
the preview boundary only as presence, never executes, and explicitly previews
zero. Local changes retain State and child identity. Child resizing scales the
translation without changing layout, and empty nodes retain selection/drop handles.

Translation changes painting and semantic transforms, not the size reserved in
layout. Pointer behavior follows Transform hit tests, but ancestor bounds still
constrain hit testing. SlideTransition does not add clipping. Extreme offsets
remain subject to Flutter's finite device-geometry limits.

## Inventory

198 admitted definitions; 7,249 writable rows (7,231 outside Scaffold),
190 definitions with typed properties, 164 const-capable definitions.
Layout: 58; Material: 51; Basic: 26; Scrolling: 52; Accessibility: 6; Interaction: 5.
Boolean-only rows: 667; nullable boolean rows: 51.
Optional insertion: 35,244 candidates across 178 destinations, with
23,477 accepted and 11,767 rejected. Required wrappers remain unchanged.
Events: 172 rows / 58 types; callables: 234 / 83 types; builders: 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

Full core Maven suite and the full Canvas suite passed. Canvas: 8,333 cases,
including 100 new cases covering exact constructor domains, all direction and
hit-test flags, empty/tight layouts, signed out-of-unit offsets, retained State,
child resizing, inert project animation and empty selection/drop.

Flutter analyze reported no issues. Four light/dark 16/32 SVG variants were
rendered and visually reviewed. The release Web build passed; main.dart.js is
3866716 bytes, SHA-256 `93e3934e93b792216be2e9f19f0d04adc3439076a96a7e5718bed262946d0fc4`.
Both manifests were regenerated from actual byte lengths and SHA-256 hashes:
40 packaged runner sources and 35 Web files.

The pinned SDK accepted all eight reference/getter/factory forms and rejected
plain, nullable, wrong-generic, dynamic and non-animation alternatives. Native
runtime cases cover saved/reopened source, exact geometry and pointer hits,
listener replacement/detachment, real AnimationController forward/reverse
frames and ancestor hit bounds. AnimatedSlideRealSdkTest also passed as a
regression. The final SDK run explicitly asserts and reports **41 runtime cases
passed** (34 generated-source/ambient-direction cases, two live animation
lifecycle/frame cases, four pointer-routing cases and one ancestor-bound case).
The actual Web/source artifact contract tests and SVG registry tests passed.

Fresh Surefire totals after this slice: 3868 cases across 564 reports,
3769 executed, 99 conditional skips, zero failures/errors. This includes
the full core suite, selected broad NetBeans regression and explicitly enabled
SlideTransition/AnimatedSlide SDK tests; it is not a claim that all opt-in
SDK tests were executed.
The packaged NBM was checked against compiled schema/editor/generator classes,
all four SVG variants, all 40 runner sources and all 35 Web files.
All four development-cluster JARs match the NBM. No manual IDE interaction or
restart was performed.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`;
8,982,533 bytes; built 2026-09-14 10:50:53 UTC;
SHA-256 `52d05675e6ae234778263c3a47b876b80e587b3bb50ef8ca08715174d2c52871`.
Next reviewed palette candidate: ScaleTransition.
