# CustomMultiChildLayout and LayoutId

Pinned API: Flutter 3.44.8. Layout palette orders 600 and 610 add the complete
CustomMultiChildLayout constructor (required Delegate, optional Children, shared
Key) and LayoutId constructor (required ID, required Child, shared Key).

## Typed identity and placement

CustomMultiChildLayout.children admits only direct LayoutId nodes. LayoutId is
not valid at the root, in another parent's slot, or through an intervening render
wrapper. Its ID accepts exact strings (including empty strings), portable integers,
finite doubles, booleans and strict non-null Object sources. Project/package
getters, static members and zero-argument factories cover enum values and custom
objects without raw-code fields. Nullable, dynamic and unrelated delegate types
cannot satisfy analyzer evidence. LayoutId itself has no const constructor.
Without an explicit shared Key, Flutter uses ValueKey<Object>(id).

Palette creation derives a unique string ID from the immutable widget StableId
and seeds a 48 × 48 SizedBox child. It is immediately insertable into Children.
Use the Child slot editor for atomic replacement, or drop content into the seed's
optional Child. Removing/moving the required child out is rejected; moving or
reordering the complete LayoutId is supported. Copying equal literal IDs into one
parent is rejected, not silently renamed.

Sibling IDs must be unique under Dart equality. Known literal conflicts are
rejected before save, including equal int/double values and doubles that round
to the same IEEE-754 value. Source-defined equality and distinct references that
alias the same object remain runtime responsibilities.

## Delegate and generated source

Delegate accepts a strict MultiChildLayoutDelegate reference/getter/member/factory.
The first starter insertion atomically adds
_FlutterDesignerMultiChildLayoutDelegate outside managed regions. It prefers
256 × 192 logical pixels, constrained by the parent, and lays children out in
vertical cells with loose cell constraints. This is an explicit Designer starter,
not Flutter's default layout algorithm.

The starter exposes a writable `ids` list. Generated code evaluates the native
CustomMultiChildLayout and each LayoutId once, reads the actual IDs from the
constructed children, then sets the list before mounting. An ID factory therefore
runs once, including factories returning fresh identity objects. Adding, removing
or reordering children updates this list without rewriting the user-owned class.

All layout methods remain editable source: getSize, performLayout, shouldRelayout
and the optional relayout Listenable. Keep the starter's `ids` member when using
the starter reference. A separately selected project delegate receives no implicit
list initialization and owns its ID contract. Native layout must call layoutChild
exactly once for every child; hasChild, arbitrary layout order, dependent child
constraints and positionChild remain available. Parent size cannot depend on
children. Delegate methods are not synthetic widget Events.

Removal retains the user class; reinsertion reuses it. Name collisions fail
without partial FD/Dart changes. Save/reopen and undo/redo preserve user code.
Factories marked const are accepted only when actual SDK analysis permits them;
MultiChildLayoutDelegate's base constructor is not const.

## Canvas boundary

Isolated Canvas never executes project delegates or ID expressions. It uses a
native CustomMultiChildLayout with private StableId-based IDs and the disclosed
finite vertical-cell preview, even for custom or edited starter delegates.
The native LayoutId wraps Canvas measurement infrastructure, preserving Flutter
ParentData. Selection, nested content, empty/populated insertion and refresh
identity remain available. Custom layout, equality and live relayout behavior
must be verified in the generated app.

FD 17, Catalog API 16, Canvas model 20 and transport 1 remain unchanged.
Native Events remain 174 rows across 60 types; callables remain 240 across 88.

## Verification

Automated coverage includes exact constructors, all eight typed-source access
shapes, finite/required ID constraints, sibling equality, FD roundtrip, source
scaffolding and collisions, property editor cancel/commit/reopen, stable rows,
child replacement and reorder history, native Canvas ParentData and drop rules.
The real-SDK suite analyzes generated candidates and exercises live delegates,
layout ordering, single-evaluation ID factories and listener detachment.

Verified on 2026-09-15:

- 54 generated/native SDK scenarios passed: 26 saved forms in LTR/RTL plus
  delegate replacement and unbounded/tight-zero constraints. Coverage includes
  enum/const/custom-object IDs, actual native ValueKey identity, eight ID and
  eight delegate source forms, negative nullable/dynamic evidence, relayout
  notification/detachment and source edits.
- All 11,533 Canvas tests passed, including seven new coordinated-layout tests.
  A reorder regression found and fixed outer instrumentation remounting; the
  test now verifies retained native child identity, updated parent data and
  rejection of moving the required Child out of LayoutId.
- Fresh Java reports: 4,070 tests, 3,968 passed, 102 conditionally skipped,
  zero failures/errors. Scope: full designer core, selected analyzer/plugin
  regressions and source/Web artifact contracts. SDK success is recorded
  separately because ungated regression runs skip SDK-only suites.
- Flutter analyze: no issues. Web release build passed.
- Current inventory: 221 definitions, 7,444 writable rows (7,426 outside
  Scaffold), 213 typed definitions and 185 const-capable definitions; Layout 69.
  Optional insertion matrix: 190 destinations (169 any/21 trait), 41,990
  candidates, 28,444 accepted and 13,546 rejected.
- Eight SVG variants were rendered and visually reviewed (16/32, light/dark).
- NBM install/package and development cluster passed. Verified the new schemas,
  editor/analyzer/source-lifecycle classes, eight SVGs, all 40 runner sources,
  35 Web artifact files and four cluster JARs matching the NBM.
  Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm.
- No interactive IDE restart or manual desktop verification was performed.

## Official references

- [CustomMultiChildLayout](https://api.flutter.dev/flutter/widgets/CustomMultiChildLayout-class.html)
- [LayoutId](https://api.flutter.dev/flutter/widgets/LayoutId-class.html)
- [MultiChildLayoutDelegate](https://api.flutter.dev/flutter/rendering/MultiChildLayoutDelegate-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/basic.dart and
  packages/flutter/lib/src/rendering/custom_layout.dart.
