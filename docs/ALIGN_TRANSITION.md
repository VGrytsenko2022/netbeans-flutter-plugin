# AlignTransition

Pinned contract: Flutter 3.44.8, `widgets/transitions.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/AlignTransition/AlignTransition.html)
and [class](https://api.flutter.dev/flutter/widgets/AlignTransition-class.html).

## Complete native constructor

- Required `alignment: Animation<AlignmentGeometry>`: structured local physical
  Alignment or RTL-aware AlignmentDirectional, or a typed project
  reference/getter/zero-argument factory.
- Required single box Child, with atomic wrap/replacement including root.
  This is not an empty-child drop target. Sliver, Flex and Stack parent-data
  children remain rejected by shared placement rules.
- Optional Width factor and Height factor: omitted, explicit null, finite
  nonnegative local number, or typed `double?` project reference/getter/factory.
- Shared Key/identity support is unchanged. Native parameter order is alignment,
  child, widthFactor, heightFactor. There is no Duration, Curve, onEnd,
  Clip behavior or Text direction parameter.

Local signed coordinates are not restricted to [-1, 1]. A local alignment emits
`AlwaysStoppedAnimation<AlignmentGeometry>`, with correct const propagation
and analyzer symbol offsets. Animation<Alignment> and
Animation<AlignmentDirectional> are valid covariant project sources.
Mixed/custom geometry is supported through Animation<AlignmentGeometry>, not
through a separate local mixed-geometry editor. Flutter's AlignmentGeometryTween
has a nullable result type; an application must adapt it to non-null geometry
before passing it to AlignTransition. The SDK runtime test exercises this adapter
in both directions and both text directions. All 24 source shapes across
the three fields retain exact static-type evidence. Nullable animation objects,
nullable geometry values, wrong types, dynamic and non-animation sources are
rejected. Controllers, tweens and animation ownership stay in application code.

Local factor editors deliberately accept finite values only. Null or omission
means native unconstrained-axis/shrink-wrap behavior, not zero. Factor changes
take effect immediately; only alignment is animated. Layout still obeys the
parent's constraints. Overflowing paint is not clipped; pointer hits outside
parent bounds do not become available merely because the child paints there.
Native invalid layout combinations (including factor overflow on an unbounded
axis) are not made valid by the Designer.

## Editing and persistence

The required alignment editor has local/project modes without null or unset.
Each optional factor has local/project/null/unset modes. Draft mode switching,
invalid local input, row identity, presentation refresh and save/reopen are
covered. FD encoding, generation, undo/redo, wrapping, movement and required
Child protection use the shared command and pair-save paths.

Canvas renders the real AlignTransition and retained child. Project sources
are never executed: alignment previews at center and source factors preview as
null, with an explicit diagnostic listing substituted fields. Exported Dart
retains the original source expressions.

Zero-factor widgets remain externally selectable. If very large finite local
coordinates produce a non-finite native paint offset, Canvas withholds unsafe
paint, hit testing and child semantics and reports the reason. It preserves the
native State, child, stored values and generated Dart; correcting the values
restores the preview.

## Inventory

205 definitions, 7,278 writable rows (7,260 outside Scaffold), 171 const-capable
definitions, 197 typed Properties definitions and eight structural editors.
Layout now has 65 definitions. Required wrappers: 43, of which 31 are
root-compatible. Optional candidate matrix: 37,105 candidates, 24,716 accepted
and 12,389 rejected.

Events remain 172 rows / 58 widget types, callables 234 / 83 types and builders
48. No format bump: FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

The focused Canvas suite passes 485 cases, including 480 native constraint /
alignment / RTL combinations, source-only substitutions, factor resets, actual
zero-size selection and recovery from non-finite offsets without replacing State
or Child. The entire Canvas suite passes 10,715 tests. Flutter analyze reports
no issues.

The opt-in pinned SDK test passes without being skipped: 90 generated/native
runtime cases (41 saved forms in LTR and RTL, plus eight lifecycle/layout/paint/
input cases). It also checks all 24 source shapes and rejected source domains
through actual analyzer evidence and the pair-save gate. Factor references accept
both double and double? results. Save/reopen preserves user members and generated
bytes; undo/redo and required-child commands are exercised.

Full core and broad/focused plugin contracts pass. Fresh Surefire reports cover
3,922 Java tests in 585 reports, with zero failures/errors and 102 skipped tests.
This is not a claim that every plugin integration test ran. Four SVG variants
were rendered and visually inspected.

The release Web build and artifact contracts pass. main.dart.js is 3,878,623
bytes, SHA-256 `4c89b4f63dbfd2d282b8a4ee2e1d99314732f3789d1711a18a0ac4e72aa24fbb`.
The 40-source and 35-Web-file manifests reflect actual file bytes. Maven install
and nbm:cluster both pass, without cleaning or resetting the checkout.
Reopening the NBM verified current schema, catalog/generator, editor/evidence
classes, four SVG variants, runner sources and the Web manifest. The 35 Web
artifact files were checked against that manifest; all four development-cluster
JARs match the NBM byte-for-byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
under `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.
9,024,932 bytes, modified UTC 2026-09-14T15:49:05.2407261Z.
SHA-256: `32bd3f0e0475b6554101879fcdb7f7f793462cbb3f060bac1a801d16fc3bc0c9`.

Whitespace checks pass. The IDE was not restarted and an interactive desktop
smoke test is not claimed. The next planned slice is
[MatrixTransition](https://api.flutter.dev/flutter/widgets/MatrixTransition/MatrixTransition.html).
CJK IME and Linux/macOS Canvas provider work remains deferred.
