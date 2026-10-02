# AnimatedBuilder vertical slice

Pinned Flutter 3.44.8; canonical checkout:
G:/MyProjects/java/project/netbeans-flutter-plugin-starter.

## Reviewed native contract

[Constructor](https://api.flutter.dev/flutter/widgets/AnimatedBuilder/AnimatedBuilder.html):
const AnimatedBuilder({Key? key, required Listenable animation,
required TransitionBuilder builder, Widget? child}).

The pinned implementation extends ListenableBuilder and forwards animation as
listenable. The constructor parameter is animation; the inherited listenable
getter is not another constructor argument. Animation is ANY Listenable, not only
Animation<double>. AnimationController, Animation, ChangeNotifier and ValueNotifier
therefore retain normal Flutter semantics.

[TransitionBuilder](https://api.flutter.dev/flutter/widgets/TransitionBuilder.html)
is Widget Function(BuildContext, Widget?). Builder is classified as BUILDER, not
EVENT. There is no native onEnd, duration or curve parameter on AnimatedBuilder;
the application owns/configures its controller, ticker and other animation state.

## Designer projections and Properties

Two fixed placements generate the same unnamed AnimatedBuilder constructor:

- Layout order 290: flutter.widgets.AnimatedBuilder, box Child/result.
- Scrolling order 490: flutter.widgets.AnimatedBuilder.sliver, sliver Child/result.

The .sliver suffix is only a Designer catalog ID, never a generated SDK constructor.
Both definitions expose two required typed rows, Animation and Builder, plus the
optional single Child slot. Shared identity handles Key.
Animation accepts the No notifications preset or a typed Listenable project
reference/getter/member/zero-argument factory, including declared-package sources.
Builder accepts Child or a typed TransitionBuilder reference/getter/member/factory.
The required arguments reject unset/null/raw code and legacy event callback values.
To clear a binding, restore its preset rather than omit the required parameter.

No notifications emits const AlwaysStoppedAnimation<double>(0.0).
Child emits (context, child) => child ?? const SizedBox.shrink(), or
SliverToBoxAdapter() for sliver placement. These are Designer creation defaults,
not SDK defaults. Child remains optional and can be inserted/removed through
normal Slots/DnD. The native callback receives that same optional Child instance.

The fixed projection requires Child to match its output protocol. Arbitrary
cross-protocol user builders (for example transforming a box Child into a sliver
result) are outside this projection, not prohibited by Flutter itself.
Widget return typing cannot prove RenderBox/RenderSliver; the application must
return a result matching the selected parent. No fabricated layout wrappers,
controller, event producer or application State are introduced.

## Reuse, ownership and source proof

Generation reuses the reviewed ListenableBuilder preset path while emitting
animation only for AnimatedBuilder. Existing ListenableBuilder source contracts
remain unchanged. Both strict Listenable assignability and the original
TransitionBuilder call-result witness are reused without relaxing type checks.
Nullable/dynamic references, wrong source types, wrong callback signatures and
nullable/dynamic Widget returns are rejected before pair-save acceptance.

Native AnimatedWidget subscribes, replaces its subscription on source change,
and unsubscribes on removal. It does not dispose the supplied source/controller.
The application's owner must dispose controllers and their tickers. A factory or
getter should return the intended stable owned instance, not allocate an
unmanaged controller on each rebuild.

Typed Properties preserve writable row/group identity across refresh and reopen.
FD/source round-trip, add/Child insertion/move/reset, Save/reopen, Undo/Redo,
package imports, symbol evidence and user-owned source use the guarded pipeline.

## Isolated Canvas and icons

Canvas mounts an actual AnimatedBuilder with immutable preview-owned
AlwaysStoppedAnimation and a pass-through Child callback. It never evaluates
project animation objects, getters, factories or builders. If either binding is
custom, tooltip/semantics explicitly identify Child as a design-time fallback.
Custom animations are not played in isolated Canvas; run the application to see
their behavior. Empty geometry, zero-size selection, Child insertion and retained
element identity use the existing box/sliver paths. Native layout is not faked.

Eight reviewed SVG assets (light/dark, 16/32, distinct box/sliver families) use
a play/repeat motif, with code chevrons for box and horizontal strips for sliver.

## Inventory

170 admitted definitions, 162 scalar and eight structural; 6942 writable rows,
6924 outside Scaffold; 140 const-capable definitions.
Layout 37, Material 49, Scrolling 49. The 161 ordinary destinations (144 any-widget,
17 constrained) produce 27370 candidates: 18113 accepted and 9257 rejected.
Required wrappers remain 29, including 22 root-compatible box wrappers.
Boolean rows remain 631 plus 50 nullable unions.
Native Events: 154 across 40 types. All callables: 209 across 62 catalog types,
including 41 builders, three predicates, two formatters and nine delegates.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.

## Verification

- Forty new Canvas tests passed: closed fields, both independent reference
  presences, Child/empty geometry, selection and DnD, both scroll axes, reverse,
  RTL, resizing, mobile/tablet profile rotation and retained elements.
- Both real-SDK gates passed without skips: box 53.553 s and sliver 51.294 s.
  Twenty-two generated forms run 94 native Flutter tests. These cover
  guarded source/history and strict type rejection, real package-backed
  notifications, subscription replacement/removal without disposing owner sources,
  prebuilt and null Child, light/dark/RTL and inherited updates.
  A real AnimationController test runs forward/reverse and replaces the controller
  while retaining the native element/Child, uses the generated Child callback,
  verifies old-source silence and performs owner disposal with no ticker left.
- Full Flutter regression passed: 5165 tests; flutter analyze reports no issues.
  Release Web build also passed with the pinned artifact settings.
- Fresh Java reports after the full designer-core and focused integration runs:
  3412 tests, zero failures/errors, 29 opt-in/environment skips across 447 reports.
  Breakdown: designer-core 2363 (20 skipped), analyzer 47 (none skipped),
  NetBeans integration 1002 (nine skipped). The two real-SDK gates above were
  separately enabled and passed; this is not an unfiltered reactor test claim.
- Maven reactor install (tests already verified above) and nbm:cluster passed.
  The NBM was checked against compiled schema/catalog/generator/analyzer/editor
  classes, all eight new SVG variants and all 40 packaged runner source files.
  The packaged Web manifest matches all 35 files in the local release Web build;
  Web build files themselves are not embedded in the runner JAR.
  All four development-cluster JARs match their packaged NBM entries byte for byte.
  The IDE was not restarted and interactive desktop behavior was not re-tested.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm

- Bytes: 8816909
- SHA-256: 7cfa03aa3ac6862abfb2b6870d3976353da732e0ef5080b63a02943baf8b018c
- Built UTC: 2026-09-13T12:56:04.7205974Z

Next reviewed candidate: [ValueListenableBuilder](https://api.flutter.dev/flutter/widgets/ValueListenableBuilder/ValueListenableBuilder.html),
with paired ValueListenable<T>/ValueWidgetBuilder<T> typing and optional Child.
It is not implemented by this slice.
