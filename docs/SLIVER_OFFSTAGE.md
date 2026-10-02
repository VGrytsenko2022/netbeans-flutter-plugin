# SliverOffstage

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor and property editor

`const SliverOffstage({Key? key, bool offstage = true, Widget? sliver})`.

Key remains Designer-owned identity. Offstage is an optional typed boolean:
omission uses Flutter's true default; explicit true/false use the shared
centered checkbox; Restore Default removes the explicit argument. Null,
numbers and coercion strings are rejected. Property-row identity survives
refresh and remains editable after reopening. The constructor has no other
fields or native event callbacks.

The optional single Sliver slot accepts sliver widgets only. It supports
insertion, nested wrappers, removal, replacement and moving the child out.
SliverOffstage belongs in sliver destinations, never the root or a box slot.
A direct SliverCrossAxisExpanded child remains rejected because it requires
its direct CrossAxisGroup parent. Existing command, evidence, save and history
boundaries are reused, without raw Dart fragments.

## Empty-slot safety

Flutter 3.44.8 accepts a null sliver in the public constructor, but
RenderSliverOffstage.performLayout asserts/dereferences a non-null child.
Like SliverOpacity and SliverIgnorePointer, empty and omitted model slots
therefore generate `sliver: const SliverToBoxAdapter()` in Dart and Canvas.
This has zero extent and no FD child identity. It disappears when a real
child is inserted. The generated framework symbol is tracked for analyzer
evidence and import/const handling; no validation boundary is relaxed.

## Native behavior and Canvas geometry

Canvas uses the real SliverOffstage. With Offstage=true, its child remains
mounted and laid out, but the wrapper has zero geometry: no painting,
scroll space, pointer hits or child semantics. State is retained, animations
continue and programmatic keyboard focus is not disabled. Offstage is not
a power-saving or focus-blocking mechanism. False restores normal native
layout, painting, hits and semantics.

Hidden descendants remain in the widget tree and can be edited there.
Canvas excludes both sliver and box descendants beneath a hidden
RenderSliverOffstage from synthetic selection/drop geometry. The owner
retains its compact 24-pixel Designer handle, without revealing a hidden
descendant or substituting visible layout. Showing the wrapper restores
descendant targets. Empty owner slots have insertion handles in horizontal
and vertical viewports, reverse and RTL; offscreen wrappers do not expose
stale nested targets. Nested hidden wrappers are covered explicitly.
Descendant project callbacks keep the existing isolated-preview restrictions.

## Current inventory

148 admitted definitions: 142 typed and six structural; 6,499 scalar rows
(6,481 outside Scaffold), 118 const-capable definitions, 37 Scrolling entries.
There are 548 boolean-only rows and 44 nullable boolean unions.
The 25 required-child creation wrappers remain unchanged.
133 insertion destinations: 124 AnyWidget and nine trait-restricted
(seven Sliver, two PreferredSize). The full 19,684 source/destination matrix
contains 14,474 accepted and 5,210 rejected combinations.
Events remain 150 across 36 types; all callables remain 196 across 49 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted definitions, not a claim to cover every Flutter widget;
the historical ordered 92-widget list is not available as a remaining count.

## Verification

Seventy dedicated Flutter tests passed: strict decoding, defaults and all
boolean states, empty slots, native geometry in both axes/reverse/RTL, live
hide/show render identity, retained animation and focus, pointer/semantics
suppression, hidden descendant handles/drop zones and offscreen targets.
The complete Flutter runner passed 3,575 tests. Analysis of lib/test found
no issues.

The separately enabled real-SDK test passed: one JUnit, zero skips/failures/
errors, 23.766 seconds, six candidate-analysis transitions and 48 generated
native Dart cases. It covers exact FD/source reopening, preserved user code,
insertion, property transitions/reset, child movement and Undo/Redo.

The complete Java core reactor passed 2,292 tests, with 20 conditional skips,
zero failures/errors and 312 fresh Surefire XML reports.

The broad core/NetBeans integration suite passed 1,176 tests (355 core, 821
NetBeans), with 15 conditional skips and zero failures/errors in 84 fresh XML
reports. The real-SDK test ran separately without a skip, as recorded above.
Coverage includes palette/tree insertion, all placement and wrap destinations,
stable checkbox editors, slot editors, SVG contracts, accessibility, capability
parity and strict source/Web artifact validation. The unrelated full NetBeans
reactor was not rerun because of its recorded Windows AWT shutdown issue.

The release Web build succeeded: main.dart.js is 3,689,577 bytes, SHA-256
`524d1acf27b172061bf929b8417d3ef5c3d6ded9480b7fa5422c969f288294e6`.
Source and Web manifests retain strict byte/hash validation.

NBM install and development-cluster creation succeeded. Strict package checks
verified the new schema class, four light/dark 16/32 SVG variants, forty runner
sources and thirty-five Web files against current inputs. All three plugin/
Designer/runner development-cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,719,491 bytes; SHA-256
`811bb22a718e0363e849369c423dccfaafd094973e6d013791a5d060be5f45a0`.

Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Follow-up completed: [SliverVisibility](SLIVER_VISIBILITY.md), including its maintain constructor.
Counts and build evidence above remain historical to this slice.

## Sources

- [Complete constructor](https://api.flutter.dev/flutter/widgets/SliverOffstage/SliverOffstage.html)
- [Native behavior](https://api.flutter.dev/flutter/widgets/SliverOffstage-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and
  packages/flutter/lib/src/rendering/proxy_sliver.dart.
