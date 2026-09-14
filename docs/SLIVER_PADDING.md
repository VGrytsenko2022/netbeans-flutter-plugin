# SliverPadding

Reviewed against Flutter 3.44.8:
[constructor](https://api.flutter.dev/flutter/widgets/SliverPadding/SliverPadding.html),
[class](https://api.flutter.dev/flutter/widgets/SliverPadding-class.html),
and the pinned SDK widgets/basic.dart / rendering/sliver_padding.dart.

## Complete constructor contract

- Required `EdgeInsetsGeometry padding`: one atomic typed row with physical
  left/top/right/bottom, directional start/top/end/bottom, or a project-owned
  typed reference. References support current-library/imported symbols,
  member/getter access, and zero-argument factories, with real analyzer proof.
- All literal components must be finite and non-negative. Null and unset are
  rejected. Creation supplies `EdgeInsets.all(16)` as a Designer preset,
  not an SDK default.
- Optional `Widget? sliver`: an empty or single Sliver-trait slot.
  Box widgets cannot be inserted here. Nested SliverPadding is allowed;
  cycles and second children are rejected. SliverPadding itself is prohibited
  at document root and in ordinary box-only slots.
- Framework `key` remains shared Designer stable identity rather than a
  separate editable constructor row. The constructor is const-capable.
  No native callbacks are introduced.

Generated Dart preserves exact `padding:` and `sliver:` arguments, imports,
directionality, null child omission, reference invocation and constness.
FD save/reopen and undo/redo use the existing typed command/generation pipeline;
no format or transport version bump is required.

## Editor and Canvas

Scrolling palette entry, tree/Canvas drop admission and light/dark 16/32 SVG
icons use the same reviewed schema. Required padding uses the existing typed
inset/reference editor. Committing a value refreshes the existing row rather
than rebuilding all property groups; reopening retains editability.

Native SliverPadding handles LTR/RTL, horizontal/vertical scrolling and reverse.
Selection/drop geometry uses actual RenderSliver bounds and viewport clipping;
empty slivers retain an external handle without changing application layout.
Nested sliver move previews use slot-trait checks and reject cycles.

Project code is never executed by isolated Canvas. A project reference uses
explicit zero insets plus a visible preview-only notice; the nested sliver is
retained and remains scrollable. Generated applications use the exact project
geometry, including mixed EdgeInsetsGeometry. Type evidence cannot prove that
an arbitrary project getter returns non-negative values at runtime: the app
must respect Flutter's assertion. Main-axis padding around a pinned header has
Flutter's native scrolling behavior; padding itself is not pinned.

## Verification

Contract tests cover every constructor field, required/default/null rules,
finite non-negative geometry, sliver-only admission across the full catalog,
nested/occupied slots, stable property rows, generation and FD round-trip.

Canvas tests cover all physical/directional × LTR/RTL × vertical/horizontal ×
reverse × empty/nonempty combinations, exact child geometry under preview zoom,
selection, drop and move/cycle behavior; zero and referenced nested insets are
also covered. Real-SDK tests exercise accepted local/imported getters and
factories, reject nullable/dynamic/wrong/async results even with diagnostic
suppression, verify candidate evidence and unchanged source on analysis, reopen
generated files, and run generated Flutter widgets in LTR and RTL.

Current inventory: 129 admitted definitions, 125 typed + 4 structural,
6,441 writable rows (6,423 outside Scaffold), 106 const-capable definitions.
The exhaustive drop matrix has 15,480 candidates: 13,372 accepted, 2,108 rejected.

Automated run on 2026-09-12: Dart analyzer clean; final Flutter suite 2,241
passed; targeted Java contracts and real-SDK gate/runtime checks passed
(the generated application suite contains eight LTR/RTL cases).
The full Java reactor wrote reports for 5,539 tests: 5,353 passed, 186 skipped,
zero assertion failures/errors. Its fork then hung in native Windows
`WToolkit.shutdown` after `System.exit(0)` and a NetBeans `LockForFile`
shutdown exception. The completed test fork was stopped after capturing a
thread dump; Maven consequently returned 1. This is not recorded as a clean
full-reactor exit. No running IDE was stopped, and no shutdown workaround was
added to production code.

Packaging and development cluster both completed successfully. The resulting
`netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm` is 8,656,400
bytes, SHA-256 `84d6a864531dc36579e0be77b4f8e8302f7af158072135aff2dc9b577f723a89`.
Inspection of the NBM confirmed the current schema class, all four new SVG
variants, all 40 runner source files against the strict bundled manifest, and
the bundled Web manifest against all 35 files of the fresh Web build.
Native Events and callable counts remain unchanged. Manual NetBeans desktop
smoke testing is not claimed by the automated tests.
