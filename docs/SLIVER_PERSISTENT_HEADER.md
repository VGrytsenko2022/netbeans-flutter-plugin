# SliverPersistentHeader vertical slice

Reviewed against pinned Flutter 3.44.8 in the canonical G: checkout.

## Full constructor contract

`SliverPersistentHeader({Key? key, required SliverPersistentHeaderDelegate delegate, bool pinned = false, bool floating = false})`.

Three typed rows plus shared stable Key identity. No named constructor, native Events,
or child/sliver slots: delegate.build owns its box subtree. The widget itself must
be placed in a Sliver slot (15 compatible SDK slots, 10 ordinary drop destinations);
root/box placement is rejected. All four combinations of Pinned and Floating are
supported, with centered checkbox editors and unchanged property-row identity.

Delegate is a non-null, strictly analyzed current-library or declared-package
reference, getter, member or zero-argument factory. No raw Dart string, dynamic,
untyped callback, omission or null escape hatch. The generator uses existing import,
symbol-manifest and static-type proof machinery without weakening provenance.

This is an abstract-class contract, not a callback typedef. The concrete delegate
supports build(context, shrinkOffset, overlapsContent), minExtent, maxExtent,
shouldRebuild, vsync, snapConfiguration, stretchConfiguration and
showOnScreenConfiguration. These are implemented in the project delegate, not
invented widget constructor parameters or Events. Build must produce a box widget,
extents must be stable and minExtent <= maxExtent. Floating snap/show-on-screen
animation needs an appropriate TickerProvider. Static typing does not prove
render-object kind, geometry, ticker lifecycle or runtime behavior.

## Initial insertion and user-source ownership

A new palette prototype references the const zero-argument
`_FlutterDesignerPersistentHeaderDelegate()`. On the transition from no such
reference to the first starter reference, the command session adds one concrete
top-level class and its Widgets import through DartUserSourceProjection.
The closed starter uses minExtent 56, maxExtent 112, SizedBox.expand and
shouldRebuild=false; it does not throw or instantiate an abstract class.

The helper is outside all managed regions. This is user-owned Source code, explicitly
documented in the UI and declaration comment. Users may edit it or select their own
delegate. Subsequent generation does not overwrite it. Additional initial headers
share the same class; customize individual headers by binding distinct delegates.
Removing a widget does not delete user code. Undo of the original creation reverses
the exact contribution only when safe; edits to the created class block destructive
undo. Redo restores the exact class, not a fresh approximation.

Existing top-level classes with that name are retained and analyzed, never replaced.
Colliding non-class names and duplicate/ambiguous declarations fail closed. Comments
and strings cannot spoof a declaration. BOM, CRLF, managed bytes and unrelated user
members are preserved. No arbitrary source-injection interface or new generated
region was introduced. Every candidate still requires the existing exact live revision,
source-integrity, analyzer and paired .fd/.dart save gates before disk writes.

## Canvas and UI

Scrolling entry 420, with four unique SVG variants (16/32, light/dark).
Canvas runs the real SliverPersistentHeader using a preview-owned delegate with
56–112 logical-pixel extents and the exact Pinned/Floating flags. It never executes
project constructors, getters, delegate.build, animation or callbacks. Wire data
contains only reference presence, not identifiers or executable bodies.

Every header has a specific tooltip/semantics limitation: custom content, extents,
rebuild, snap, stretch and show-on-screen behavior are unavailable in isolated
Canvas. Run the application to verify them. Even the editable starter is treated as
project code; the preview does not claim to reflect later edits to that class.
Selection and placement use the existing Sliver identity/drop machinery.

## Inventory

154 admitted definitions: 148 typed and six structural; 6520 scalar rows
(6502 outside Scaffold); 124 const constructors; 43 Scrolling entries.
562 boolean-only fields, 44 nullable-boolean unions. 136 destinations produce
20944 candidate placements: 14621 accepted and 6323 rejected. Required wrappers: 28.
Native Events remain 151 across 37 types; callables 198 across 51 types. This object
delegate is not an additional callable. FD 16, Catalog API 15, Canvas model 19 and
transport 1 are unchanged.

## Verification

- All 4632 Flutter runner tests passed; the new focused Canvas suite has 33 cases
  covering the four modes, both axes, reverse, LTR/RTL, strict decoding, warnings
  and retained render identity. Flutter analyze reports no issues.
- Full Java core: 2317 tests, zero failures/errors, 20 conditional skips.
- Real pinned-SDK suite: one unskipped test passed (38.618 s), including initial
  source scaffolding, duplicate/reused delegates, getter/factory references,
  four boolean modes, rejected nullable/dynamic/wrong-type bindings, save/reopen,
  source edits and Undo/Redo. Seven generated files execute 56 native layout
  cases with pointer-driven floating reveal. Project-owned extents are retained.
- Extended cross-module regression: 1074 tests, zero failures/errors, 21 conditional
  skips; Maven exit 0. This overlaps core tests and is not the whole NetBeans
  reactor. The known Windows AWT/Surefire shutdown timeout occurred after
  System.exit(0), after all assertions and fresh XML reports had completed.
  The separate real-SDK test above ran unskipped.
- Additional shared-editor regression: 133 tests passed, zero failures/errors/skips.
  Four older editor tests still expected the pre-existing 186-callable/26-builder
  milestone; their inventory assertions now match the separately audited 198
  callables, 33 builders and 51 callable-owning types. No event behavior was changed.
- Release Web build passed using offline/no-icon-shaking flags. main.dart.js:
  3711302 bytes, SHA-256 `118d7fb4cdd912c73dab2781a4ee3c8453a18dc1acd88ec07a4e6d5257bf8398`.
  Strict source and Web manifests retain all 40 source and 35 Web entries.

NBM install and dev-cluster completed successfully. Exact package verification
matched the schema and three source-lifecycle classes, two analyzer classes,
four SVG variants, all 40 runner sources and 35 Web files. All four cluster JARs
(plugin, Designer, runner and dart-analysis) match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`;
8749445 bytes; SHA-256
`44833009373cc9bd2f3fc46feb46b77ce772652dadd5e6ebe5fbe7795e352360`.

No IDE restart, installation into an independently running IDE, or manual native
desktop verification is claimed.

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/SliverPersistentHeader/SliverPersistentHeader.html)
- [Official delegate API](https://api.flutter.dev/flutter/widgets/SliverPersistentHeaderDelegate-class.html)
- Pinned SDK: widgets/sliver_persistent_header.dart and rendering/sliver_persistent_header.dart.

CJK IME and Linux/macOS Canvas providers remain deferred. Next palette candidate:
SliverResizingHeader; now implemented in [its dedicated slice](SLIVER_RESIZING_HEADER.md).
