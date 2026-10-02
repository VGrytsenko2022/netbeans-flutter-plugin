# SliverLayoutBuilder vertical slice

Reviewed against pinned Flutter 3.44.8. Canonical checkout:
`G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.

## Complete constructor and semantics

`SliverLayoutBuilder({Key? key, required Widget Function(BuildContext, SliverConstraints) builder})`.
The exported callback typedef is `SliverLayoutWidgetBuilder`. This is a const-capable
Sliver with no named constructors, child slots or native Events. Key remains the
Designer stable widget identity. Inherited RenderObject methods are not constructor
properties or user event callbacks.

Builder runs at layout time, including initial layout, changed constraints, a
changed builder and dependency changes. A reused builder with unchanged constraints
need not run again. Its returned widget must produce a RenderSliver. Returning
a box (even a statically valid Widget) fails native layout. Designer cannot prove
that render-object contract from Dart's Widget return type and does not claim to.

## Property and source contract

One required row, Builder:
- Closed `empty` preset (creation default):
  `(context, constraints) => const SliverToBoxAdapter()`.
- Strict typed project reference, getter, static/instance member, or zero-argument
  factory returning `SliverLayoutWidgetBuilder`. Current-library and explicitly
  declared package references use the shared provenance and import machinery.
- No omission, null, arbitrary string, raw Dart expression or legacy callback
  escape hatch. The user selects Empty to clear the binding; Restore Default is
  unavailable for this required constructor argument.
- The callback is inventoried as BUILDER, not EVENT; no fake interaction handler.
  Signature metadata carries Widgets and Rendering imports.

Properties uses the shared preset/reference editor. Scalar refresh preserves the
row and property-set instances; reopening retains writable typed values.
Source generation retains exact references and manifest/type-proof offsets. Empty
preset SliverToBoxAdapter has its own SDK symbol evidence; generated closures are
not const expressions. Function/getter/factory constness uses the shared proof rules.
Save/reopen, further edits, moves, Undo/Redo preserve user-owned Dart members.

Strict analyzer proof now admits only the exact non-null SliverLayoutWidgetBuilder
family from Widgets/Material. A proof-only Rendering import supplies SliverConstraints.
A typed call-result witness rejects dynamic Widget results in addition to wrong
signatures, nullable callbacks and dynamic references. It is never executed or
persisted. Existing strict-casts controls, navigation roots and pair-save CAS
requirements remain intact.

## Palette, tree and Canvas

Scrolling entry 410 with four dedicated reviewed SVG variants (16/32, light/dark).
Ordinary insertion preserves the empty preset and creates no subtree. All fifteen
Sliver-compatible SDK slots admit it; ten of those allow ordinary empty-slot/list
insertion. Box slots and document-root placement reject it. No new destination
slots or wrapper rules were introduced. Tree-drop tokens remain single-use.

Canvas constructs the real SliverLayoutBuilder with a preview-owned builder
returning a zero-extent SliverToBoxAdapter. Both the empty preset and custom
references remain selectable through the shared zero-size handle. Sibling drop
resolves to the owning Sliver list, never a fabricated child slot.

Project callbacks, getters and factories are never executed by isolated Canvas;
the wire payload is presence-only (no identifiers or executable bodies). A custom
builder shows a specific tooltip/semantics warning: its responsive subtree is
unavailable, and the application must be run to verify it. This preview is not a
claim to display the custom constraint-dependent content. The complete callback
still runs normally in generated project Dart.

## Current inventory

153 definitions: 147 typed and six structural; 6517 scalar rows (6499 outside
Scaffold); 123 const-capable constructors. Scrolling has 42 entries.
560 boolean-only fields and 44 nullable-boolean unions are unchanged.
136 empty-slot/list destinations form 20808 candidate placements: 14611 accepted,
6197 rejected. Required wrappers remain 28.
Native Events remain 151 across 37 types. All callables: 198 across 51 types
(33 builders, 151 events, three predicates, two formatters, nine delegates).
Formats remain FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

- Flutter runner: 4599 tests passed; focused new suite: 17 tests with both axes,
  reverse, LTR/RTL, zero geometry, retained identity, selection, sibling drop,
  strict decoding and explicit custom-reference warnings.
- flutter analyze: no issues.
- Real pinned SDK test: passed without skip (29.94 s), including seven
  accepted candidate transitions and four rejected typed bindings, seven
  generated files, 48 axis/reverse/RTL native cases with resize/scroll checks,
  plus a negative native box-result test. User code and source history retained.
- Full Java core: 2312 tests, zero failures/errors, 20 conditional skips.
- Extended cross-module NetBeans/analysis/Sliver regression selection:
  1040 tests, zero failures/errors, 20 conditional skips; Maven exit 0.
  This selection overlaps the core tests. It is not the full NetBeans reactor.
  Surefire reported the known Windows AWT fork shutdown timeout after System.exit(0);
  assertions and fresh XML reports completed successfully. The separate real SDK
  run above was unskipped and passed independently.
- Release Web build passed with the repository's offline/no-icon-shaking flags.
  main.dart.js: 3700024 bytes, SHA-256 `c894d40684664e8a5fb27e6eaa117400054e2f7a21582ebbc57becc720c49376`.
  Both source and Web manifests retain strict byte/hash checks.
- NBM install and dev-cluster succeeded. Strict verification matched the new
  schema and two analyzer classes, four SVG variants, forty runner source files
  and thirty-five Web artifact files. Plugin, Designer, runner and dart-analysis
  cluster JARs all match the NBM.
  Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
  8742145 bytes; SHA-256 `c1467682473abf45e414f9a7547b4236cd30cc45d6c27c24f88df2fe78b14843`.
- No manual IDE session or restart is claimed.

CJK IME and Linux/macOS Canvas providers remain deferred. Next palette candidate:
SliverPersistentHeader; now implemented in [its dedicated slice](SLIVER_PERSISTENT_HEADER.md).

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/SliverLayoutBuilder/SliverLayoutBuilder.html)
- [Official layout behavior](https://api.flutter.dev/flutter/widgets/SliverLayoutBuilder-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver_layout_builder.dart,
  layout_builder.dart and rendering/sliver.dart.
