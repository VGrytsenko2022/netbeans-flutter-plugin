# AnimatedCrossFade

Pinned API: Flutter 3.44.8 `widgets/animated_cross_fade.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/AnimatedCrossFade/AnimatedCrossFade.html),
[default layout](https://api.flutter.dev/flutter/widgets/AnimatedCrossFade/defaultLayoutBuilder.html).
The online class page now mentions clipBehavior, but our pinned SDK has no such
constructor argument. It always uses ClipRect. No unsupported parameter is generated.

## Model and UI

Layout order 460. All 10 constructor properties (key remains stable node identity):
firstCurve, secondCurve, sizeCurve, alignment, crossFadeState, durationUs,
reverseDurationUs, layoutBuilder, excludeBottomFocus and onEnd.

The required firstChild and secondChild slots are created as explicit SizedBox
nodes (48 x 48 and 48 x 80). These are Designer presets, not Flutter defaults.
Child IDs are deterministic from owner and slot; creation consumes one supplied ID.
Both children are visible in the tree and replaceable atomically through Slots.
Removing/moving a required child without replacement is rejected. Compatible
wrappers may wrap the current child. This two-child widget is an ordinary palette
insertion, not a single-child automatic wrapper.

Creation state is showFirst and duration is 300000 microseconds. All 43 Curves
presets and strict Curve references/factories are available independently.
Physical and directional AlignmentGeometry permits finite coordinates outside
[-1,1]; omission uses topCenter. Duration uses nonnegative portable integer
microseconds or a Duration reference; reverse duration additionally permits null.
Zero completes immediately. Boolean rows use the shared centered checkbox editor
while omission remains distinct.

## Events and source ownership

onEnd is a nullable VoidCallback, raised on completed or dismissed cross-fades,
not initial mount. layoutBuilder is an optional non-null AnimatedCrossFadeBuilder:
Widget Function(Widget topChild, Key topChildKey, Widget bottomChild, Key bottomChildKey).
Properties accepts typed references/factories; the Events callable editor creates
a user-owned method with an editable native-default layout scaffold. It preserves
both keyed children. Existing handlers can be bound/renamed/unbound through the
shared source lifecycle. Project method bodies remain outside managed regions.
Strict analyzer evidence rejects wrong arguments, void and dynamic original
return types. Duration symbol navigation remains bound to the trusted SDK core.

## Native Canvas behavior

Both children stay mounted, including the inactive branch. First curve is
inverted by Flutter; second curve fades in. Cross-fade reversal retains state
and uses reverseDuration when supplied. Native AnimatedSize handles layout size;
prefer equal child widths, and expect changing heights to be clipped.
excludeBottomFocus defaults true; bottom pointer/semantics behavior stays native.
The top branch alone receives Designer surface selection/drop, while both remain
editable through tree/Slots. External selection handles cover zero-size results
and follow layout changes without a second model edit.

Canvas never executes application handlers or project references. Presence-only
references have explicit labeled defaults: linear curves, topCenter alignment,
300 ms duration, null reverseDuration, native defaultLayoutBuilder, absent onEnd.
Stored values and generated application Dart remain unchanged. No generic raw-code
execution or format/version migration is introduced.

## Verification

- Dedicated Canvas suite: 51 passing tests; full Canvas runner: 7980 passing
  tests. Flutter analyzer: no issues.
- Full core and selected NetBeans/analyzer/resource regressions: 3792 cases
  in 513 fresh Surefire reports, 72 conditional skips, zero failures/errors
  (3720 executed). This is not every unfiltered repository Java test.
- Real SDK: six generated forms mounted in LTR/RTL (12 runtime cases),
  including defaults, typed references, custom builder factory, second state,
  reset/null values and generated layout scaffold. Exact analyzer proof,
  wrong-type/dynamic/void/argument rejection, save/reopen and Undo passed.
- Catalog: 189 definitions; 181 typed-property definitions; 7113 writable rows,
  7095 outside Scaffold; 156 const-capable definitions; 54 Layout items;
  646 BOOLEAN-only rows. Events: 171 rows/57 types; all callables: 231 rows/81 types.
- Insertion matrix: 32886 candidates, 21744 accepted, 11142 rejected.
  The 174 ordinary insertion destinations (155 any-widget destinations) and
  34 required-child wrappers (24 root-compatible) remain unchanged.
  The two CrossFade required slots are atomic replacement/wrap destinations,
  not empty ordinary insertion destinations.
- Web release build, Maven install and nbm:cluster succeeded. Four SVG variants
  were rendered and visually reviewed. All 40 packaged runner sources and all
  35 local Web release files match their manifests; all four cluster JARs match
  the NBM, including the updated builder proof/editor/source lifecycle classes.
- NBM: 8939775 bytes; SHA-256
  `c747cdbe934935de12829e8718e972d889bf88942893f58108db98e7ca0c5dfb`.
  Built at 2026-09-13T20:55:37.0332615Z.
- main.dart.js: 3819324 bytes; SHA-256
  `4fc8b2115ec8ae868496c03fd3062ee511886f3b449f41b487c5eb615fff107e`.
- Formats remain FD 16 / Catalog API 15 / Canvas model 19 / transport 1.
  Working checkout: G:. No IDE restart or user Flutter-project changes.
  An interactive NetBeans desktop smoke test was not performed.
