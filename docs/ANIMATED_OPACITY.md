# AnimatedOpacity

Complete Flutter 3.44.8 constructor slice:
`const AnimatedOpacity({Key? key, Widget? child, required double opacity,
Curve curve = Curves.linear, required Duration duration, VoidCallback? onEnd,
bool alwaysIncludeSemantics = false})`.

Reviewed against the pinned SDK's `widgets/implicit_animations.dart` and the
[official constructor](https://api.flutter.dev/flutter/widgets/AnimatedOpacity/AnimatedOpacity.html).
The shared Designer identity manages Key; all other constructor arguments are
available as five typed property rows, one optional Child slot, and Events.

## Properties, child and events

- Opacity: required finite number in [0, 1], with creation default 1.
  Zero does not disable child hit testing; add IgnorePointer when needed.
- Duration (microseconds): required integer in [0, 9007199254740991] or an
  analyzer-verified Duration reference/member/zero-argument factory.
  Creation uses 300000 microseconds. Zero completes immediately.
- Curve: all 43 pinned Curves presets or a typed Curve reference/factory;
  omission retains the native linear default.
- On end: optional VoidCallback reference/factory or explicit null. The shared
  Events workflow supports handler creation, selection and navigation.
  It fires after a completed transition, not on initial mount or unchanged target.
- Always include semantics: optional centered boolean checkbox, omitted means
  false. Restore Default removes the explicit constructor argument.
- Child: optional single box widget; insert, replace, remove, move and nested
  wrapping are supported. Slivers and incompatible direct parent-data widgets
  are rejected. The widget itself can be a document root or a normal box child.

The FD codec and generated source preserve omitted and explicit null callbacks,
exact typed bindings, shared Key identity, child identity and user-owned code.
Edits survive save/reopen and Undo/Redo. Property updates preserve row identity
and boolean editors rather than rebuilding the entire sheet.

## Canvas behavior and boundaries

Canvas mounts the native AnimatedOpacity/FadeTransition. It animates opacity
changes, preserves child layout and identity, and supports invisible selection
and empty-child drop targets. Initial mounting uses the target without animation.
There is no new Canvas protocol, FD format or Catalog API version.

Project-owned Curve, Duration and completion handlers are never executed in
Canvas. A labeled preview uses linear for a custom Curve, 300 ms for a custom
Duration and no completion callback; generated Dart retains the exact bindings.
Run the app to verify project code and runtime validity of project-owned values.
The native opacity-zero hit-testing behavior is intentionally not rewritten.

## Verification

Dedicated core contracts cover native defaults, all 43 curves, portable duration
bounds, numeric/boolean domains, codec, generation, placement, commands and
property editor identity. Canvas tests cover curves, timing, intermediate and
terminal opacity, constraints, semantics, child identity, zero-opacity selection
and empty-child drops. A real Flutter SDK gate checks generated forms, analyzer
evidence, typed references (including rejection of the wrong type), callback
lifecycle, Undo/Redo and preservation after reopen.

Verified on 2026-09-13 with pinned Flutter 3.44.8:

- Flutter analyze reports no issues.
- All 5525 Flutter regression tests pass, including 276 dedicated Canvas cases.
- The explicitly enabled real-SDK gate passes without skips (eight generated
  forms and 13 native Flutter tests); analyzer evidence rejects a Curve bound
  as a Duration while preserving the baseline file.
- Full flutter-designer suite: 2384 cases, zero failures/errors, 20 conditional
  skips.
- Combined fresh Java core and focused NetBeans/integration reports: 3676
  cases across 465 suites, zero failures/errors, 61 conditional skips. The
  real-SDK gate above was enabled separately and passed. This is not an
  unfiltered full-reactor claim: the previously known unrelated Windows native
  Canvas pixel-metric test remains outside this slice.
- Release Web build passes with the repository's offline-safe build flags.
  The source bundle and Web manifest were regenerated and contract-tested.
- Maven install and nbm:cluster pass. Byte verification confirms the compiled
  schema/property/evidence classes, four SVG variants, 40 packaged runner
  sources, 35 Web artifact files, and equality of all four development-cluster
  JARs with the NBM. The user's IDE was not restarted.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
Size: 8845019 bytes; modified UTC: `2026-09-13T14:39:07.2524311Z`.
SHA-256: `39b1fa9a28b2711d4720753a7ee9b8e7dda6f6af68f13bdcee752d8574693bdc`.

Inventory: 175 definitions (167 typed scalar and eight structural), 6969
writable rows (6951 outside Scaffold), 145 const-capable definitions, 636
boolean rows. There are 166 ordinary destinations, 29050 placement candidates,
19036 accepted and 10014 rejected. Native Events: 157 rows across 43 types;
all callables: 216 rows across 67 types. Next planned slice: AnimatedAlign.
