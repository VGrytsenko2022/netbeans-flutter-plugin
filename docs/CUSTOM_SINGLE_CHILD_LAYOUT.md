# CustomSingleChildLayout

Pinned API: Flutter 3.44.8. Layout order 590 admits
`flutter.widgets.CustomSingleChildLayout` with the complete constructor:
required non-null Delegate, optional box Child and shared Key.

## Delegate and source ownership

Delegate accepts an exact `SingleChildLayoutDelegate` project/package reference,
getter, static member or zero-argument factory, including const constructors.
Null, unset, dynamic and unrelated source types cannot satisfy the constructor.
The analyzer witness is the SDK Rendering type, not a locally shadowable name.

First insertion creates `_FlutterDesignerSingleChildLayoutDelegate` outside
managed Dart regions, atomically with the widget. The starter has a const
constructor with optional `relayout`; all four native methods are visible and
editable. It prefers 128 × 96 logical pixels, constrains this size to incoming
constraints, gives Child loose constraints bounded by that size, and centers Child.
This finite starter policy is not Flutter's default delegate behavior
(`getSize` normally returns `constraints.biggest`).

Removing the widget does not delete a user-owned class. Reinsertion reuses it;
source edits are not regenerated. Undo of first insertion restores the exact
original source; redo restores the contribution. Conflicting top-level names
fail without changing either FD or Dart. Custom references add no starter.

Dart owns all getSize/getConstraintsForChild/getPositionForChild/shouldRelayout
logic and the relayout Listenable's lifetime. Native parent size cannot depend on
Child size. Constraints, finite sizing, overflow, and notification disposal remain
the delegate author's responsibility. These methods are not widget Events.

## Canvas boundary

The isolated Canvas never executes project delegates. It uses a native
CustomSingleChildLayout with the reviewed starter's finite centered layout for
every source reference, including edited starters. An explicit tooltip says this
is a 128 × 96 preview, not the project's layout.

Child rendering, selection, empty-child insertion and stable render identity
remain active. There is no implicit clipping or invented child. A source delegate
that sizes, constrains or positions differently must be tested in the generated
application. FD 17, Catalog API 16, Canvas model 20 and transport 1 are unchanged.

## Verification

Contract tests cover required-property validation, all eight source access shapes,
exact static proof, FD roundtrip, stable Properties rows/reopen, source collisions,
insert/remove/reinsert and exact undo/redo. Shared SliverPersistentHeader source
scaffolding retains regression coverage.

The real-SDK suite checks eight reference forms, two const constructors, strict
nullable/dynamic/unrelated type rejection, editable starter preservation,
28 native scenarios (13 saved forms in LTR/RTL plus replacement and constraints),
live relayout notifications and listener detach. Canvas tests cover strict wire
decoding, finite preview/child geometry, disclosure, drop and refresh identity.

Verified on 2026-09-15:

- 28 generated/native SDK scenarios passed, plus the SliverPersistentHeader
  real-SDK regression after shared starter-source refactoring.
- All 11,526 Canvas tests passed, including five new layout-specific tests.
- Fresh Java reports: 4,055 tests, 3,953 passed, 102 conditionally skipped,
  zero failures/errors. Scope: full designer core, selected analyzer/plugin
  regressions and source/Web artifact contracts. SDK results are recorded
  separately because ungated regression runs mark SDK-only suites skipped.
- Flutter analyze: no issues. Web release build passed.
- NBM install/package and development cluster passed. Verified exact schema,
  source-lifecycle, editor and analyzer classes; four SVGs, all 40 runner sources,
  35 Web artifact files, and four cluster JARs matching the NBM.
  Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.
- Inventory: 219 definitions, 7,442 writable rows (7,424 outside Scaffold),
  211 typed Properties definitions, 184 const-capable definitions; Layout 67.
- Optional insertion matrix: 189 destinations (169 any/20 trait),
  41,391 candidates, 28,274 accepted and 13,117 rejected.
- No interactive IDE restart or manual desktop verification was performed.

## Official references

- [CustomSingleChildLayout](https://api.flutter.dev/flutter/widgets/CustomSingleChildLayout-class.html)
- [SingleChildLayoutDelegate](https://api.flutter.dev/flutter/rendering/SingleChildLayoutDelegate-class.html)
- Pinned SDK source: `packages/flutter/lib/src/widgets/basic.dart` and
  `packages/flutter/lib/src/rendering/shifted_box.dart`.
