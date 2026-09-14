# LayoutBuilder vertical slice

Reviewed against pinned Flutter 3.44.8 in
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native API and explicit scope

[Constructor](https://api.flutter.dev/flutter/widgets/LayoutBuilder/LayoutBuilder.html):
LayoutBuilder({Key? key, required LayoutWidgetBuilder builder}).
The exact callback is Widget Function(BuildContext, BoxConstraints).
Key uses the shared Designer stable identity; this is a const-capable box widget,
without named constructors, native Events or separate Child slots.
Inherited framework methods are not editable constructor properties.

[Native behavior](https://api.flutter.dev/flutter/widgets/LayoutBuilder-class.html):
the callback builds at layout time, receiving the parent's constraints rather
than the device's orientation or unrestricted screen dimensions. The resulting
box obeys those constraints. Changed constraints, builder or inherited
dependencies can rebuild the subtree; identical constraints alone do not require
another callback. Native intrinsic/dry layout is unsupported because speculative
callback execution may mutate the live tree.

The result must produce a RenderBox, not a RenderSliver. A Widget return type
cannot statically establish that rendering protocol. Source analysis deliberately
does not claim otherwise. Use SliverLayoutBuilder inside a raw sliver list.

## Required Builder property and source lifecycle

- One writable required Builder row: the closed empty preset or a typed project
  function, getter, static/instance member, or zero-argument factory returning
  LayoutWidgetBuilder.
- Empty is the explicit Designer creation value. It generates
  (context, constraints) => const SizedBox.shrink().
  Flutter itself has no default Builder. Empty honors tight/minimum dimensions;
  it is not always physically zero-sized.
- Unset, null, unreviewed strings, raw Dart and legacy event callbacks are rejected.
  Choose Empty to clear a custom binding; Restore Default cannot remove the required
  constructor argument. The callback is classified as BUILDER, not EVENT.
- The shared preset/reference editor keeps the row and property-set objects stable
  across edits and preserves writable values after reopening.
- Exact references, import provenance and source offsets remain authoritative.
  Symbol evidence includes SizedBox and shrink for the preset.
- An analyzer-only witness uses the pinned BoxConstraints type. Typed initialization,
  strict-casts controls and an original call-result witness reject wrong signatures,
  nullable/dynamic references and nullable/dynamic Widget results. The witness is
  never executed or persisted in user source.
- Save/reopen, further edits, imports, move and Undo/Redo preserve user members and
  widget identity. Rejected edits or analyzer candidates do not mutate saved bytes.

## Palette, Canvas and limitations

Layout category order 250, four reviewed SVG variants: light/dark, 16/32.
Regular box slots and root placement are allowed; raw sliver and other trait-only
slots reject it. It owns no drop destination or synthetic designer Child.

Isolated Canvas creates a real LayoutBuilder whose preview-owned callback returns
SizedBox.shrink(). It does not execute project functions, getters or factories.
A custom binding has an explicit tooltip/semantics warning that the responsive
subtree is unavailable; generated application Dart still calls the actual builder.

The shared empty-node overlay keeps zero-sized instances selectable without
changing their Flutter layout. Tight-size instances use normal selection.
Drop insertion resolves to a compatible parent, never a fabricated builder slot.
Changing preview dimensions and custom-binding presence retains the native element.
Unbounded scroll axes are valid when the returned box can satisfy those constraints.
The native no-intrinsic-layout restriction is not bypassed or disguised.

## Current inventory

163 definitions: 155 scalar and eight structural; 6931 writable rows, 6913 outside
Scaffold, 133 const-capable definitions. Layout has 33 entries; Material 49 and
Scrolling 46 are unchanged. 157 ordinary destinations (142 AnyWidget, 15 constrained)
produce 25591 candidates: 17183 accepted and 8408 rejected.
Required wrappers remain 29, including 22 root-compatible box wrappers.
Boolean-only rows remain 631 and nullable-boolean unions 50.
Native Events remain 154 across 40 types; all callables total 202 across 55 types,
including 34 builders. FD 16, Catalog API 15, Canvas model 19 and transport 1 remain
unchanged.

## Verification

- Full Designer core: 2342 tests, zero failures/errors, 20 conditional skips.
- Dedicated schema/generation/round-trip tests verify preset and typed references,
  manifest symbols, exact placement and rejection of invented slots.
- Properties test verifies a stable editable row across changes/reopen and required
  unset/null rejection. Palette tests cover list and single-box insertion, native
  creation values, no invented Child and raw-sliver rejection.
- Real pinned SDK gate passed unskipped: strict pair-save proof, function/getter/
  factory and declared-package static members, wrong-signature/nullable/dynamic
  rejection, source moves, reset-to-empty, Save/reopen and Undo/Redo.
  Nine generated forms run 34 native Flutter cases: width changes, LTR/RTL,
  light/dark inherited updates with retained element identity; explicit negative
  sliver-result and intrinsic-measurement cases.
- Full Flutter Canvas: 5044 tests passed. The new 11-test suite verifies tight/loose
  geometry, unchanged render identity on width/reference edits, pointer selection,
  zero-size handles, parent insertion and unbounded scroll axes. flutter analyze:
  no issues.
- Release Web build passed with --no-web-resources-cdn and --no-wasm-dry-run.
  Strict source/Web manifest sizes and SHA-256 were regenerated, not relaxed.
- Extended cross-module Java regression: 1461 tests, 3 conditional skips,
  zero failures/errors, Maven exit 0. This overlaps the core run and must not be
  added to its total. Real SDK was enabled separately; the regular selection
  skips optional SDK gates. CanvasRunnerSourceBundleTest and
  WebCanvasArtifactContractTest pass, including the explicitly enabled real Web
  artifact check (canvas.runner.web.source).
- The entire Java reactor was not rerun; no claim is made about unselected tests.
  Interactive desktop verification remains separate.

No manual desktop IDE test or IDE restart is claimed. The unrelated existing
JNA DPI test is outside this slice's focused regression.

## Verified package

Maven install and nbm:cluster completed with exit 0. Packaged schema, generator,
catalog, property editors and analyzer witnesses match current compiled classes.
All four SVG variants and 40 source files match the manifest and source checkout;
all 35 final Web artifact files match their pinned sizes and hashes.
The four development-cluster JARs are byte-identical to their NBM counterparts.

- Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
- Size: 8800191 bytes
- SHA-256: dd04d4c421dba0cd0be71fc74c7a4208c5b2213223a6b194ff9d75a83a2f8be7
- Built: 2026-09-13T10:43:54.7968023Z

The next slice is now implemented: [OrientationBuilder](ORIENTATION_BUILDER.md).
CJK IME and Linux/macOS Canvas providers remain deferred.
