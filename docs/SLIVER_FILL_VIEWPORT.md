# SliverFillViewport

Reviewed against the pinned Flutter 3.44.8 SDK widgets/sliver_fill.dart,
rendering/sliver_fill.dart and widgets/scroll_delegate.dart, plus the official
[constructor](https://api.flutter.dev/flutter/widgets/SliverFillViewport/SliverFillViewport.html)
and [class documentation](https://api.flutter.dev/flutter/widgets/SliverFillViewport-class.html).

## Constructor and delegate ownership

Both palette entries generate the native unnamed SliverFillViewport constructor.
The .delegate suffix is a Designer variant, not an invented Flutter constructor.

| SDK field | Designer surface | Omitted value |
| --- | --- | --- |
| viewportFraction | Finite positive double, including values above 1 | 1.0 |
| padEnds | Centered checkbox / unset | true |
| allowImplicitScrolling | Centered checkbox / unset | true |
| delegate | Visual children list, or required typed project delegate | Empty list preset in project-delegate variant |
| key | Shared stable Designer identity convention | Not an arbitrary key editor |

The visual variant builds a SliverChildListDelegate with an ordered box-child
list (empty allowed). It exposes all five optional list-delegate fields:
addAutomaticKeepAlives, addRepaintBoundaries, addSemanticIndexes (true defaults),
semanticIndexOffset (zero default), and semanticIndexCallback (native local-index
default). The semantic callback accepts a non-null SemanticIndexCallback
reference/getter/zero-argument factory with the exact (Widget, int) -> int?
signature. It is a delegate callable, not an interaction event.

The project-delegate variant has no Designer child slot. It accepts a non-null
SliverChildDelegate reference/getter/zero-argument factory, including
SliverChildListDelegate, SliverChildBuilderDelegate and custom subclasses.
All custom configuration remains in user Dart. Dynamic/nullable/wrong result
types are rejected by the candidate evidence gate even with suppressed analyzer
diagnostics. The required empty preset cannot be unset; optional fields reset
by removing their explicit argument, retaining SDK defaults.

Visual children use the normal mutable SliverChildListDelegate, preserving key
lookup when children reorder; this emitted configuration is not const. The
native constructor itself is const-capable in the project-delegate variant
when its value meets the existing const-evidence rules.

## Placement and Canvas

These are slivers: root/ordinary box slots reject them; CustomScrollView.slivers
and SliverPadding.sliver accept them. Visual children accept ordinary box widgets,
not slivers or direct Expanded/Flexible/Spacer parent-data widgets.

Canvas constructs the real SDK SliverFillViewport. Each child receives
viewport main-axis extent times viewportFraction, including fractions above one.
End padding centers first/last children only below one; both axes, reverse and
RTL use native constraints and geometry. allowImplicitScrolling reaches the
native render object. Sliver selection/drop/move previews remain viewport-clipped;
empty visual children have a bounded drop affordance. Index bounds for a move
are evaluated after removing the source child.

Project delegates and semantic callbacks never execute in isolated Canvas:
a visible preview-limit message states that delegate items are unavailable, or
that local semantic indexes are an approximation. Stored references and generated
Dart are not changed. Run the generated application to evaluate project code.
No artificial placeholder child is persisted.

## Verification and inventory

- Both variants: schema/default omission, finite positive fraction checks,
  boolean/null/type validation, root and child-slot admission.
- 108 Java generation/FD round-trip combinations; exact native constructor and
  nested delegate generation, flags and child preservation.
- Stable property row/group identity, paintable checkbox editors, reference
  editors, reset and editable values after reopen.
- 307 dedicated native Canvas cases: 288 fraction/padding/implicit/axis/reverse/
  RTL combinations; trailing scroll geometry; empty and project delegates;
  list-delegate flags; actual empty-child drop and selection in all directions.
- Real SDK evidence test: 24 generated application runtime variants; list/builder/
  custom delegates, callback/getter/factory references; seven invalid signatures
  rejected; source-preserving analysis, save/reopen, resets, Undo and Redo.
- Shared capability/Events inventory, placement matrix, all-boolean editors,
  accessibility, eight light/dark 16/32 SVG assets and strict bundled manifests.

Inventory: 132 palette definitions (128 typed, four structural), 6,455 writable
rows (6,437 outside Scaffold), 527 boolean rows, 108 const-capable definitions.
The placement matrix contains 16,104 candidates, 13,608 accepted and 2,496 rejected.
Native Events remain 150 across 36 widget types. All callable descriptors now
total 187 across 44 types (one new semantic-index delegate callable).
FD 16 / Catalog 15 / Canvas 19 / transport 1 remain unchanged.

Automated verification on 2026-09-12: Dart analyzer `lib test` clean;
all 2,716 Flutter tests passed (307 dedicated to this slice). Full Designer-core
suite: 2,252 tests, 20 conditional skips, zero failures/errors and successful
Maven exit. Final targeted NetBeans regression: 666 tests, five conditional
skips, zero failures/errors and successful Maven exit. The separate enabled
real-SDK test passed in 89.441 seconds and ran all 24 generated app cases.
The unrelated full NetBeans reactor was not repeated after the previously
recorded Windows AWT shutdown issue.

NBM install/package and development cluster build succeeded. The final package
was checked against the schema class, eight SVG variants, all 40 runner sources
and all 35 Web artifact manifest entries without relaxing integrity checks.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
(8,666,057 bytes), SHA-256:
`75d8de2980fe62c57e3eb25fafad193aae72a1c1ee503237ac76d789941e3815`.

Manual NetBeans desktop smoke testing and IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
