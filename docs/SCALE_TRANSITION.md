# ScaleTransition

Pinned SDK: Flutter 3.44.8, checked against
packages/flutter/lib/src/widgets/transitions.dart and animation/animations.dart.
Official [constructor](https://api.flutter.dev/flutter/widgets/ScaleTransition/ScaleTransition.html),
[API](https://api.flutter.dev/flutter/widgets/ScaleTransition-class.html) and
[AlwaysStoppedAnimation](https://api.flutter.dev/flutter/animation/AlwaysStoppedAnimation-class.html).

## Complete constructor projection

All three scalar constructor arguments and the optional box Child are supported.
Key follows the shared Designer identity policy.

- Required Scale animation: finite signed local number, default 1, generated as
  `const AlwaysStoppedAnimation<double>(value)`, or strict non-null
  `Animation<double>` reference/getter/zero-argument factory. Current-library and
  imported-package forms, with optional member access, retain exact source
  evidence. Local changes are immediate, not implicitly animated.
- Alignment: physical local Alignment or strict Alignment reference/getter/factory.
  Unset uses center; finite coordinates outside [-1,1] are valid. Directional,
  nullable and broader AlignmentGeometry types are not substitutes.
- Filter quality: omitted, explicit null, none, low, medium or high.
- Child: optional box slot, with normal parent-data/placement constraints;
  add, replace, remove, move and wrapping are transactional.

Scale accepts zero and negative values without clamping. Layout stays unchanged;
zero collapses paint and pointer hits, while negative scale flips both axes.
The transform pivots around physical Alignment independently of ambient RTL.
Ancestor hit bounds still apply. No implicit clipping is inserted.

The inherited animation, listenable and onTransform fields are not extra
constructor inputs: ScaleTransition itself binds its matrix function.
There is no Duration, Curve, On end, controller or builder argument.

## Source ownership and filter semantics

Source-owned controllers, Tween/CurvedAnimation and listener lifetime stay in
user Dart. Plain double, nullable animation/value types, Animation<int>,
Animation<dynamic>, dynamic and ValueNotifier<double> fail strict animation
evidence. User members survive generation, save/reopen and undo/redo.

Flutter's MatrixTransition chooses filtering using Animation.isAnimating.
Forward/reverse statuses normally enable the configured filter; dismissed and
completed disable it. Crucially, AlwaysStoppedAnimation has a constant value
but reports forward. Therefore local generated constants and isolated Canvas
previews apply the configured filter. The Designer preserves this SDK behavior.

Canvas never executes project references: custom animation previews at constant
scale 1, custom Alignment at center, with explicit diagnostics naming the
affected properties. The generated source retains live references. Local edits
retain native State and child identity; resizing recomputes the pivot, and empty
or zero-scaled nodes retain bounded selection/drop targets. Extreme finite
numbers remain subject to Flutter device-geometry limits.

Structured number/alignment editors validate local drafts and typed source
selection before commit. Cancel keeps the previous value. Optional properties
can be reset; refresh retains property/group identity and reopened rows remain
editable. SVG icons are supplied at 16/32 sizes in light/dark variants.

## Inventory

199 admitted definitions, 7,252 writable rows (7,234 outside Scaffold).
191 definitions with typed properties; 165 const-capable definitions.
Material 51; Layout 59; Scrolling 52; Basic 26; Accessibility 6; Interaction 5.
Boolean-only rows remain 667, nullable booleans 51.
Optional insertion: 35,621 candidates across 179 destinations, with 23,780
accepted and 11,841 rejected. Required-wrapper counts are unchanged.
Events remain 172 rows / 58 types, callables 234 / 83 types, builders 48.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

148 focused Canvas cases passed, covering signed/zero physical geometry, RTL,
alignment, tight/empty children, local filter status, source isolation,
retained State, resized children and pointer/selection/drop behavior.
Full core Maven and selected broad NetBeans regression suites passed.
The pinned SDK test passed **77 generated/native runtime cases**: 62
saved/reopened source/RTL cases, one live source replacement/alignment/listener
case, four real-controller frame cases, four status/filter matrices, five
pointer cases and one ancestor-bound case. The analyzer accepted all 16
animation/alignment reference shapes and rejected incompatible animation and
alignment types. AnimatedScaleRealSdkTest also executed and passed.

The controller test checks actual status at the final sampled frame and then
waits for completed status; it does not assume floating-point tick timing has
completed exactly at 100 ms. Constant-value forward status, all four native
statuses and signed/zero filtering remain explicitly covered.

The full Canvas suite passed: **8,481 cases**. Flutter analyze reported no issues.
The SafeArea schema test now stops at the next widget-record boundary instead of
a hard-coded neighbor; its exact expected SafeArea contract is unchanged.
All four SVG variants were rendered and visually reviewed.
Fresh Java reports: **3,876 tests in 568 suites**, zero failures/errors and
100 skipped opt-in cases (3,776 executed). Both ScaleTransition and AnimatedScale
pinned-SDK tests executed, without skips.

Web release build and source/artifact contract tests passed. The source manifest
covers 40 files and the Web manifest covers 35 files. The rebuilt main.dart.js
is 3,867,927 bytes, SHA-256
`d0a7c22ef7dcb82532889cec705a12af96c7e330dcda72bbf760ecdd33a1b098`.
Final Maven install/package and nbm:cluster both passed. Archive verification
matched the compiled schema/editor/generator classes, all four SVG variants,
all 40 packaged runner sources and all 35 Web artifact files against current
sources/manifests. All four checked development-cluster JARs match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,987,340 bytes, built 2026-09-14T11:32:12.3378336Z, SHA-256
`97d4f2ea0706b0f31e6e1a6dc6d501662443001dbd03a96c843f43e24430cf17`.
Whitespace diff check passed.
No manual IDE interaction or restart is performed.
