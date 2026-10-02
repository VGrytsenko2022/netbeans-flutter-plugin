# FadeTransition and SliverFadeTransition

Pinned SDK: Flutter 3.44.8. Official constructors:
[FadeTransition](https://api.flutter.dev/flutter/widgets/FadeTransition/FadeTransition.html),
[SliverFadeTransition](https://api.flutter.dev/flutter/widgets/SliverFadeTransition/SliverFadeTransition.html).
Verified against packages/flutter/lib/src/widgets/transitions.dart in the pinned SDK.

## Complete constructor projection

Both definitions expose the required Opacity animation and optional Always include
semantics. FadeTransition has an optional box Child; SliverFadeTransition has an
optional Sliver accepting sliver-compatible widgets only. Identity/Key follows the
existing shared Designer policy. Neither constructor has Duration, Curve, On end,
builder or animation-controller arguments.

Opacity offers exactly two editor sources:

- Local finite number in [0,1], default 1, generated as
  `const AlwaysStoppedAnimation<double>(value)`. Integer endpoints are accepted.
  Editing this local value is immediate, not an implicit transition.
- Non-null `Animation<double>` from a current-library or imported-package
  getter/reference or zero-argument factory, including optional member access.
  The analyzer verifies the exact type; plain double, nullable animations,
  nullable element types, Animation<int>, Animation<dynamic>, dynamic and
  ValueNotifier<double> are not accepted substitutes.

Null and omission are not allowed for Opacity. Always include semantics supports
unset (Flutter false) and centered explicit true/false checkboxes. Custom-editor
drafts, source selection and invalid values stay local until commit. Refresh keeps
property/group identities; saved and reopened values remain editable.

The generator records stopped-animation symbols and strict reference evidence,
retains const where supported, and leaves source-owned members untouched. Empty
Sliver slots lower to a zero-extent SliverToBoxAdapter: this follows the existing
pinned RenderProxySliver safety policy without adding a phantom FD child.

## Native behavior and Canvas limits

Generated Dart uses the real FadeTransition / SliverFadeTransition. Source-owned
AnimationController, Tween, CurvedAnimation, listener registration and disposal
remain ordinary Dart; no code customizer or hidden Designer controller is added.

Isolated Canvas uses the native render objects with a stopped animation. Project
animation references are presence-only, never executed: Canvas explicitly reports
a stopped opacity-1 fallback while generated Dart retains live source values.
Local edits update the existing render object and retain the child.

Zero opacity stops painting but does not remove layout, scroll extent or pointer
hit testing. Semantics follows the SDK's quantized alpha unless Always include
semantics is true. IgnorePointer / SliverIgnorePointer can be composed separately;
the Designer does not silently insert them. Paint-dependent layouts such as Flow
retain Flutter's zero-opacity caveat.

Optional slots support insertion, replacement, removal, movement and wrapping.
Box/sliver placement and parent-data constraints are enforced. Empty Canvas nodes
retain bounded drop targets; transparent child geometry remains in the model.

## Inventory

197 admitted definitions, 7,246 writable rows (7,228 outside Scaffold),
189 definitions with typed rows, 163 const-capable definitions.
Layout: 57; Scrolling: 52. Boolean-only rows: 666; nullable booleans: 51.
The optional-destination matrix covers 34,869 candidates across 177 destinations:
23,176 accepted and 11,693 rejected. Required-wrapper counts are unchanged.
Events remain 172 rows / 58 types, callables 234 / 83 types, builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Verification

- Full core Maven suite passed; the targeted broad NetBeans regression suite
  also passed. This is not a claim that every opt-in SDK test was run.
- Full Canvas suite: 8,233 passed, including 87 new transition cases.
- Flutter analyze: no issues. All eight light/dark 16/32 SVG variants were
  rendered and visually reviewed; registry topology/safety tests passed.
- The source manifest was refreshed from actual byte lengths and SHA-256 hashes.
- No manual IDE interaction or restart was performed.

Release Web build passed. main.dart.js: 3865643 bytes,
SHA-256 `c75930d5c8d40cf4ab108e3b6f86fc05ce48983a23e44977ec1550dc6232deee`.
The Web manifest was refreshed for all 35 actual output files.

FadeTransitionRealSdkTest passed with the pinned SDK enabled. Its generated Dart
passed 58 runtime cases: 52 saved/reopened source/RTL cases, two live source
replacement/listener-detachment cases, two actual AnimationController frame cases
and two native zero-opacity hit-test cases. The strict analyzer also rejected
every incompatible animation type listed above. AnimatedOpacityRealSdkTest and
SliverAnimatedOpacityRealSdkTest executed and passed as regressions.

SDK tests scope their widget search to the generated Sample, excluding the
MaterialApp route's own FadeTransition, and explicitly include offstage
zero-extent slivers.

Fresh Surefire reports after this slice: 3860 cases across 560 reports,
3763 executed, 97 conditional skips, zero failures/errors.
This combines the full core suite with selected broad NetBeans, actual artifact
and three explicitly enabled SDK tests; it is not an unfiltered all-SDK run.
Source-bundle, Web-artifact and SVG registry contracts passed.

Logs: ignored `flutter-designer/target/fade-*.log`; the final new SDK result is
`fade-sdk-complete.log`. Build/test commands use the canonical G: checkout.
Artifact tests supply `-Dcanvas.runner.web.source` pointing to the actual local
Canvas runner; SDK tests supply `-Dflutter.events.sdk` pointing to pinned 3.44.8.

Packaging succeeded with `mvn -q -pl netbeans-plugin -am -DskipTests install`
and `mvn -q -pl netbeans-plugin nbm:cluster`.
Final archive inspection matched compiled schema/generator/editor/evidence classes,
all eight SVG variants and all 40 runner sources. All 35 Web files match the
packaged manifest; all four development-cluster JARs match the NBM.
The whitespace diff check passed.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,977,699 bytes; built 2026-09-14T10:23:24.2015907Z.
SHA-256: `296bb26f870432afeca13fbe8fffc4cfb7f89f3b39b608765c4125fe66f7cacc`.

Next palette slice: SlideTransition (explicit Animation<Offset> position).
