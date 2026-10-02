# SliverConstrainedCrossAxis

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor

`const SliverConstrainedCrossAxis({Key? key, required double maxExtent, required Widget sliver})`.

The key is Designer-owned stable identity. Max extent is required and accepts
nonnegative integer/finite decimal values or the closed positive Infinity constant.
Zero, fractional values and finite values larger than the viewport are valid.
Negative values, NaN, negative Infinity, null, omission and arbitrary Dart are
rejected. Portable integer bounds remain the shared +/-9,007,199,254,740,991
storage/Web range; finite decimal overflow is rejected before generation.

Positive Infinity is stored using the existing typed double enum and generated
as the exact const expression `(1.0 / 0.0)`. Import planning does not emit a new
prefixed dart:core import for this widget, preserving implicit core types and
annotations in user-owned members. No wire-format or expression escape hatch
was introduced.

There are no other constructor fields or event callbacks.

## Creation and Properties

Scrolling → SliverConstrainedCrossAxis wraps an existing compatible sliver in
one atomic WrapWidget command, retaining the child's identity. Creation starts
at 120 logical pixels: a visible Designer preset, not an SDK default.
Empty/terminal insertion cannot create a wrapper without its required child.

The wrapper is admitted in sliver slots of CustomScrollView, MainAxisGroup,
CrossAxisGroup, SliverPadding and compatible required wrappers, including nested
ConstrainedCrossAxis. It cannot be a document root or a child in ordinary box slots.
An Expanded child cannot be wrapped inside it: that would remove Expanded's
required direct CrossAxisGroup parent. The opposite nesting, Expanded containing
ConstrainedCrossAxis, is supported by the native SDK and retains native ParentData
behavior.

Max extent has a typed number editor with Infinity support. It remains writable
after reopen; editing refreshes the row value without replacing row/property-set
identities. Required values cannot be reset to omission/null. Invalid editor text
does not publish a command.

The Sliver slot always contains exactly one child. That child cannot be removed,
cleared or moved out alone. Atomic slot replacement and moving the whole wrapper
are supported. Undo reverses the original wrap; no new generic Unwrap action is
claimed.

## Native Canvas and layout boundaries

Canvas uses the actual SDK SliverConstrainedCrossAxis. Its child receives the
smaller of maxExtent and the inherited cross-axis extent. Native axis/reverse/RTL
and main-axis geometry are retained; the wrapper creates no new scroll controller
or viewport. In a CrossAxisGroup the constrained lane uses zero flex, with the
remaining extent available to expanding siblings.

Flutter 3.44.8 requires remaining space for each later CrossAxisGroup lane.
A constrained lane that consumes all available space while siblings remain is
an invalid runtime layout in the SDK, including Infinity in that composition.
The Designer does not silently clamp the saved number, discard siblings or
reinterpret Infinity. Use a smaller limit, a sole constrained lane or a viewport/
main-axis destination. This is an inherited-layout condition, not a missing
constructor parameter.

Hit/wrap geometry uses RenderSliverConstrainedCrossAxis.geometry.crossAxisExtent,
not the larger incoming constraint. Groups continue to use their own allocation
rather than inheriting a child's reported width for their selection zone.
Neighboring lanes do not steal one another's nested wrap targets. Zero-width
descendants retain bounded selection handles; offscreen slivers expose no stale
targets. Actual native sizes are not changed to create these handles.

Descendant project-owned callbacks retain their existing isolated-preview limits.
No arbitrary project code runs inside Canvas.

## Inventory

145 admitted constructor definitions: 139 typed and six structural. 6,494 writable
scalar rows (6,476 outside Scaffold), 545 boolean rows, 115 const-capable definitions.
Scrolling contains 34 entries; there are 25 required-child creation wrappers.
130 insertion destinations remain: 124 AnyWidget and six trait-restricted. The new
required Sliver slot is replacement-only, not an empty insertion target.
18,850 source/destination candidates: 14,375 accepted and 4,475 rejected.
Native Events remain 150 across 36 types; all callables remain 196 across 49 types
(150 Events, 32 Builders, nine Delegates, three Predicates, two Formatters).
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted definitions, not all Flutter widgets or a remaining-work count.

## Verification

The 65 dedicated Flutter tests cover exact schema acceptance/rejection, native
zero/fractional/large/Infinity constraints across both axes/reverse/RTL, mixed
constrained/expanded lanes, native parent-data ownership in viewport/main/padding/
cross/expanded/nested parents, bounded wrap targets, invalid moves, required-child
protection, live limit edits, Undo/Redo-style wrapping and offscreen/zero geometry.
The full Flutter runner passed 3,378 tests. Dart analysis of lib/test reports no issues.

The separately enabled real-SDK JUnit test passed: one test, zero skips/failures/
errors, 15.298 seconds and successful Maven exit. It checks five candidate-analysis
transitions, exact FD/source reopening, retained user members, Undo/Redo, invalid
edits, movement to a viewport and positive Infinity without a core-import
regression, then executes 32 generated native Dart cases.

The complete core reactor passed 2,277 tests with 20 conditional skips, no
failures/errors and successful Maven exit. Fresh Surefire XML supplied the totals.
The broad targeted core/NetBeans suite passed 1,147 tests (340 core, 807 NetBeans),
with 12 conditional skips and no failures/errors. It includes palette/drop/wrap,
Properties, Slots, icons, accessibility, exact Java/Dart capability parity and
strict source/Web artifact checks. The real-SDK test ran separately as above.
The unrelated full NetBeans reactor was not repeated because of its recorded
Windows AWT shutdown issue.

Release Web build succeeded. main.dart.js: 3,684,483 bytes; SHA-256
`2b2a1248326e89341be06afdd019a91ee3cf66ddb2b518ccd9abf3cc6ab68481`. Source and Web manifests remain strict.

NBM install and development-cluster creation succeeded. Verification compared
the packaged schema class, four light/dark 16/32 SVG variants, forty runner sources
and thirty-five Web artifact files against current inputs. All three plugin/
Designer/runner cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,706,262 bytes; SHA-256
`f8bfc2f4d5ec31b230cfe4e53b810660db127b5cb2987c0b6867a42eeadffa9e`.

Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Historical next slice: SliverOpacity, now documented in [SliverOpacity](SLIVER_OPACITY.md).

## Sources

- [Official class and layout contract](https://api.flutter.dev/flutter/widgets/SliverConstrainedCrossAxis-class.html)
- [Complete constructor](https://api.flutter.dev/flutter/widgets/SliverConstrainedCrossAxis/SliverConstrainedCrossAxis.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart,
  packages/flutter/lib/src/rendering/proxy_sliver.dart and sliver_group.dart.
