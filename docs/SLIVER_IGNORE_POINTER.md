# SliverIgnorePointer

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor and editable values

`const SliverIgnorePointer({Key? key, bool ignoring = true, bool? ignoringSemantics, Widget? sliver})`.

Key remains Designer-owned identity. Both properties are optional and the
prototype has no explicit scalar values. Ignoring defaults to true; null,
non-booleans and coercion strings are rejected for that field.
Ignoring semantics is deprecated in the SDK but fully supported here for
compatibility: omission, explicit null, false and true are preserved separately
by the model, codec, property editor and Dart generation. The property label
and help explicitly mark it deprecated; leave it unset for new code. Explicit use
can produce the SDK's normal deprecation diagnostics; generation does not globally
suppress project analyzer warnings.

Explicit boolean values use the shared centered checkbox renderer/editor.
Both fields reset to omission. The nullable override additionally has the
existing typed null editor. Row/property-set identity and editability survive
refresh and reopening. There are no other constructor fields or native events.

## Sliver creation, editing and empty-state safety

The optional single Sliver slot accepts sliver widgets, not boxes. Insertion,
nesting, removal, replacement, moving the child out, wrapper movement and
Undo/Redo use the existing atomic commands and evidence boundaries.
SliverIgnorePointer can be inserted in viewport/sliver group/padding/compatible
wrapper destinations but not as the document root or in box slots.
A direct SliverCrossAxisExpanded child is forbidden because it requires its
direct CrossAxisGroup parent. The opposite nesting is valid.

Like SliverOpacity, Flutter 3.44.8's public constructor permits null but the
inherited RenderProxySliver.performLayout asserts/dereferences a non-null child.
Empty and omitted model slots therefore lower to
`sliver: const SliverToBoxAdapter()` in generated Dart and Canvas. This explicit
Designer safety preset has zero layout/scroll extent, introduces no FD widget
identity and disappears when a real child is inserted. The framework-symbol
occurrence is tracked for candidate analysis and import/const handling.

## Native behavior and Designer selection

Canvas uses the actual SliverIgnorePointer, without RenderBox instrumentation
between slivers. Ignoring=true makes native hitTest return false, rather than
absorbing pointer events. It preserves layout, painting and scroll extent.
Keyboard focus and programmatically delivered keyboard events are not blocked.

With a null/omitted semantics override, Ignoring blocks semantic user actions
while preserving labels and other non-action semantics. Explicit false retains
semantic actions even when pointer hits are ignored. Explicit true removes the
entire semantics subtree. These legacy branches remain SDK behavior, not a
Designer reinterpretation.

The body retains native pointer behavior in Canvas: clicking an ignored child
does not bypass the guard. A compact 24-pixel sliver handle allows selection;
coincident handles cycle among slivers, and the widget tree exposes deeper
descendants. Move and drop geometry remains available independently of pointer
delivery. Empty slots have bounded insertion handles across both axes/reverse/
RTL; offscreen slivers do not expose stale nested targets.
Descendant project callbacks retain the existing isolated-preview limitations.

## Inventory at this slice

147 admitted definitions: 141 typed and six structural. 6,498 scalar rows
(6,480 outside Scaffold), 117 const-capable definitions and 36 Scrolling entries.
There are 547 boolean-only rows and 44 nullable boolean unions, including the
new semantics override; both use the shared checkbox for explicit booleans.
There are still 25 required-child creation wrappers.
132 insertion destinations: 124 AnyWidget and eight trait-restricted
(six Sliver, two PreferredSize). 19,404 source/destination candidates:
14,439 accepted and 4,965 rejected.
Events remain 150 across 36 types; all callables remain 196 across 49 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted definitions, not all Flutter widgets or a remaining-work count.

## Verification

The 67 dedicated Flutter tests passed: strict decoding, optional/missing slots,
all axis/reverse/RTL/empty geometry combinations, live render identity, null and
legacy semantics branches, real pointer taps, accessible actions, keyboard focus,
compact handle selection, move geometry, empty-slot drops and offscreen targets.

The separately enabled real-SDK test passed: one JUnit, zero skips/failures/errors,
38.187 seconds, ten candidate-analysis transitions and 64 generated native Dart
cases. It covers exact FD/source reopening, preserved user members, all property
branches, resets, insertion, child movement and Undo/Redo.

The complete Flutter runner passed 3,505 tests. Dart analysis of lib/test found
no issues. The release Web build succeeded: main.dart.js is 3,687,681 bytes,
SHA-256 `3b9a3ba164ea8e43c602477e93a028bc1f4db2ff82c77860cc2d596e0ccdad31`.
Source and Web manifests remain strict.

The complete Java core reactor passed 2,287 tests, with 20 conditional skips,
zero failures/errors and successful Maven exit (310 fresh Surefire XML reports).

The broad core/NetBeans integration suite passed 1,166 tests (350 core, 816
NetBeans), with 14 conditional skips and no failures/errors. The real SDK test
ran separately as above. Coverage includes palette/tree/Canvas drop planning,
stable nullable checkbox editors, slots, all optional wrap destinations, icons,
accessibility, Java/Dart capability parity and strict source/Web artifact checks.
The unrelated full NetBeans reactor was not rerun because of its recorded
Windows AWT shutdown issue.

NBM install and development-cluster creation succeeded. Strict package checks
verified the new schema class, four light/dark 16/32 SVG variants, forty runner
sources and thirty-five Web artifact files against current inputs. All three
plugin/Designer/runner development-cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,715,696 bytes; SHA-256
`23c3ef588203442d75935a78dd47af8b8aeff31f718f12f04db719eb537c4045`.

Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Follow-up completed: [SliverOffstage](SLIVER_OFFSTAGE.md). Counts and build evidence above remain historical to this slice.

## Sources

- [Complete constructor](https://api.flutter.dev/flutter/widgets/SliverIgnorePointer/SliverIgnorePointer.html)
- [Pointer and semantics behavior](https://api.flutter.dev/flutter/widgets/SliverIgnorePointer-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and
  packages/flutter/lib/src/rendering/proxy_sliver.dart.
