# ListenableBuilder vertical slice

Pinned Flutter 3.44.8; canonical checkout:
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Native API and projections

[Constructor](https://api.flutter.dev/flutter/widgets/ListenableBuilder/ListenableBuilder.html):
const ListenableBuilder({Key? key, required Listenable listenable,
required TransitionBuilder builder, Widget? child}).
[TransitionBuilder](https://api.flutter.dev/flutter/widgets/TransitionBuilder.html)
is exactly Widget Function(BuildContext, Widget?).

Both required properties support strict typed current-library or declared-package
references, member/getter references and zero-argument factories. A callback
function is a Builder, not an Event. Listenable itself is an object, not a callable.
ChangeNotifier, ValueNotifier, Animation and other assignable Listenable subtypes
use the same source proof; dynamic, nullable and wrong types are rejected.
Builder proof also checks the original invocation result so dynamic or nullable
Widget results cannot hide behind function assignability.

Two fixed Designer placement projections share the SAME unnamed SDK constructor:

- Layout: flutter.widgets.ListenableBuilder, order 280. Child/result are boxes.
- Scrolling: flutter.widgets.ListenableBuilder.sliver, order 480. Child/result
  are slivers. The .sliver suffix is only a catalog ID, not a native constructor.

Both expose Listenable and Builder plus an optional single Child slot. Shared
identity supplies Key. No invented notification event or native extra argument.
The No notifications creation preset emits const AlwaysStoppedAnimation<double>(0.0).
The Child preset emits (context, child) => child ?? const SizedBox.shrink()
or SliverToBoxAdapter() for the sliver projection. These are Designer creation
choices, not Flutter defaults. Missing/null required arguments are forbidden;
restore a preset instead of omitting them. Optional Child may be added or removed.

The fixed projection restricts Child to the same rendering protocol as its
default callback result. Arbitrary cross-protocol custom builders (for example a
box Child transformed into a sliver result) are not represented by this projection.
The return type Widget cannot prove RenderBox versus RenderSliver; custom results
must match the selected parent. This is an explicit Designer constraint, not a
Flutter SDK limitation.

## Ownership and lifecycle

Pinned AnimatedWidget subscribes in initState, replaces the subscription when
listenable changes, and removes it in dispose. It does NOT dispose the Listenable.
The application's owner must manage source lifetime. A getter/factory must return
the intended stable source; allocating a new notifier on every application rebuild
has application-owned lifecycle consequences, not automatic Designer disposal.

The native builder receives the same optional Child instance; notifications
rebuild the builder while an unchanged prebuilt child may retain its element.
Build-time construction supports intrinsic/dry layout for suitable box results.
No change is made to the owning application class or state ownership.

## Editor, Canvas and source safety

Stable typed property rows retain group/row identity across refresh and reopen.
The shared Slots editor and palette planner admit only matching child protocols.
FD round-trip, source symbol offsets, declared-package imports, add/move/reset,
Save/reopen and Undo/Redo use the existing guarded pair-save pipeline.
Failed source evidence never changes the saved source.

Canvas mounts native ListenableBuilder with a preview-owned immutable source
and pass-through callback. It never evaluates project objects, getters, factories
or builders and does not simulate their notifications. When either binding is
custom, tooltip/semantics explicitly label the displayed Child as a design-time
fallback, NOT user builder output. Both zero-size selection handles and native
empty Child drop zones are supported. No fake intrinsic size is assigned.
Box/sliver SVG families cover light/dark and 16/32 pixels.

## Inventory

168 definitions, 160 scalar and eight structural; 6938 writable rows, 6920 outside
Scaffold; 138 const-capable definitions. Layout 36, Material 49, Scrolling 48.
159 ordinary destinations produce 26712 candidates: 17793 accepted, 8919 rejected.
Required wrappers remain 29 (22 root-compatible). Boolean rows remain 631 plus
50 nullable unions. Native Events remain 154 across 40 types.
207 callable rows across 60 catalog types: 39 builders, three predicates, two
formatters and nine delegates. FD 16, Catalog API 15, Canvas model 19, transport 1.

## Verification

- Four core contract tests cover both projections, full constructor/slot/callable
  shape, presets, FD round-trip, every reference/factory/member/library shape,
  symbol proofs and invalid values.
- Stable editor test covers both required properties on both projections through
  refresh and reopen, reset to preset, and rejected unset/null/raw input.
- Forty new Canvas tests passed; full Flutter regression: 5125 passed.
  Flutter analyze: no issues. Release Web build passed without external CDN
  resources or a Wasm dry-run.
- Both real-SDK gates passed unskipped: box 51.329 s, sliver 53.244 s.
  Twenty-two generated forms run 92 native Flutter tests: real notification-driven
  updates through package getters/factories, prebuilt Child, null Child, source
  replacement, no stale listener, unmount cleanup without disposing owner sources,
  RTL/light/dark and inherited rebuilds with retained element identity.
  Source evidence rejects wrong signature, non-nullable Child parameter, nullable/
  dynamic builder and return, nullable/dynamic/wrong Listenable and uninvoked
  factory. Add/Child insertion/move/reset, Save/reopen and Undo/Redo preserve
  user-owned source and identity.
- Full Designer core passed: 2358 tests, 20 conditional skips. The broad Java
  integration run initially found nine stale inventory expectations (destination
  and builder counts); all nine corrected classes passed their focused rerun.
  The combined fresh XML snapshot contains 3403 tests in 444 reports, zero
  failures/errors and 27 conditional skips. This is not a claim that the entire
  reactor, including unrelated JNA tests, was run. SDK gates above ran separately
  with the required flag and without skips.
- Maven install and nbm:cluster passed without clean or an IDE restart.
  Packaged schema/catalog/generator/analyzer/editor classes match current compiled
  classes; eight SVGs and all 40 packaged source entries match local bytes and
  manifest hashes. The packaged Web manifest matches all 35 locally built release
  artifact files. All four dev-cluster JARs are byte-identical to their NBM entries.
  NBM: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm,
  8808521 bytes, SHA-256
  f8574e24098e6e3a982cb21b64f0019d1fa7512cdeed1083659a4c9c7a360f9c,
  built 2026-09-13T12:09:14.2661947Z.
  Running NetBeans was not restarted; interactive visual acceptance remains a
  separate check after loading the new artifact.

## Next candidate

AnimatedBuilder: native animation Listenable, TransitionBuilder and optional
Child, reviewed independently despite sharing native ListenableBuilder behavior.
No CJK IME or Linux/macOS Canvas-provider implementation is included.
