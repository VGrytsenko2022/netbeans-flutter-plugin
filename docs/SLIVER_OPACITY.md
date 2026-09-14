# SliverOpacity

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructor

`const SliverOpacity({Key? key, required double opacity, bool alwaysIncludeSemantics = false, Widget? sliver})`.

Key remains Designer-owned stable identity. Opacity is required: finite decimal
values in [0, 1] and integer endpoints 0/1 are supported. Creation starts at 1,
a Designer preset rather than a Flutter constructor default. Omission, null,
negative/greater-than-one values, Infinity, NaN and arbitrary Dart are rejected.
Always include semantics is optional; omission uses false. Explicit true/false
uses the shared centered checkbox editor. There are no additional fields or
native event callbacks.

The optional single Sliver slot accepts slivers, not boxes. SliverOpacity belongs
in CustomScrollView, sliver groups, padding and compatible sliver wrappers, not
at the document root or in ordinary box slots. SliverCrossAxisExpanded cannot
be its direct child: Expanded requires its direct CrossAxisGroup parent.
Nesting opacity wrappers is supported.

## Empty slot safety

Flutter 3.44.8's public constructor permits a null sliver, but RenderSliverOpacity
inherits RenderProxySliver.performLayout(), which asserts child != null and then
dereferences it. This was reproduced by the native Flutter tests.

An empty or omitted model slot therefore lowers to
`sliver: const SliverToBoxAdapter()` in generated Dart and Canvas. This is an
explicit Designer safety preset, not an SDK default. It has zero scroll/layout
extent and adds no widget identity to the FD tree. Generation records the
framework-symbol occurrence for candidate analysis, respecting import prefixes
and const generation. Adding a real sliver replaces the preset; removing/moving
the last child restores it.

## Editing and native Canvas

Palette insertion creates an opaque widget with an empty slot. Sliver insertion,
nested insertion, replacement, removal, moving a child out and moving the wrapper
retain the existing typed command/evidence/history contracts. Required opacity
cannot be reset to omission. Optional semantics can be reset. Property rows and
property-set identities survive refresh; values remain editable after reopen.

Canvas uses the native SliverOpacity without a RenderBox between slivers.
Opacity zero does not remove layout/scroll extent or disable native pointer hit
testing. Fractional opacity uses Flutter's intermediate opacity layer. Flutter
quantizes alpha to eight bits: sufficiently small positive values can also paint
nothing and omit semantics unless Always include semantics is true.
The Designer's selection overlay can still select transparent descendants.
Empty zero-extent slivers retain bounded insertion handles in both axes,
reverse scrolling and RTL; those handles do not change native layout.

Descendant project callbacks retain their existing isolated-preview limits.
No new project-code execution, raw Dart property, callback preset or protocol
version is introduced.

## Inventory at this slice

146 admitted constructor definitions: 140 typed and six structural.
6,496 writable scalar rows (6,478 outside Scaffold), 546 boolean rows,
116 const-capable definitions and 35 Scrolling entries.
There are 25 required-child creation wrappers; SliverOpacity is an ordinary
prototype insertion with an optional child.
131 insertion destinations: 124 AnyWidget and seven trait-restricted
(five Sliver, two PreferredSize). 19,126 source/destination candidates:
14,406 accepted, 4,720 rejected. This includes the new optional sliver destination.
Events remain 150 across 36 types; all callables remain 196 across 49 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These counts describe admitted definitions, not all Flutter widgets or remaining work.

## Verification

The 60 dedicated Flutter tests cover strict decoding, numeric/boolean rejection,
optional/empty slivers, native geometry in both axes/reverse/RTL, zero/fractional
opacity layers, live render-object identity, eight-bit semantics behavior,
native pointer hits, transparent selection/movement and bounded empty-slot drops.
The full Flutter runner passed 3,438 tests. Dart analysis of lib/test found no issues.

The complete core reactor passed 2,282 tests with 20 conditional skips and no
failures/errors (308 fresh Surefire XML reports, successful Maven exit).
The separately enabled real-SDK test passed with no skips/failures/errors:
one JUnit test, 18.228 seconds, seven candidate-analysis transitions and 48
generated native Dart cases. It verifies insertion, property edits, semantics
reset, moving the child out, exact FD/source reopening, preserved user members,
Undo/Redo, and the empty-child safety preset.

The broad core/NetBeans integration suite passed 1,157 tests (345 core, 812
NetBeans), with 13 conditional skips and no failures/errors. The real SDK test
ran separately as above. Coverage includes palette/tree/native drop planning,
property editors, slots, wrap rejection matrices, icons, accessibility,
Java/Dart capability parity and strict source/Web artifacts.
The unrelated full NetBeans reactor was not rerun because of its recorded
Windows AWT shutdown issue.

Release Web build succeeded. main.dart.js: 3,686,121 bytes; SHA-256
`83c6ac11b15ec2e47c579867fd16e706691dd0354fdf5cd31bf41113b78a7a31`. Source and Web manifests remain strict.

NBM install and development-cluster creation succeeded. Strict package checks
verified the new schema class, four light/dark 16/32 SVG variants, forty runner
sources and thirty-five Web artifact files against current inputs. All three
plugin/Designer/runner development-cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,711,280 bytes; SHA-256
`cf43464f4d829941e3aaa4de4d9687e54b79cbfbc175f297036398839e9b87cd`.

Manual NetBeans desktop testing and an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Historical next slice: SliverIgnorePointer, now documented in [SliverIgnorePointer](SLIVER_IGNORE_POINTER.md).

## Sources

- [Complete constructor](https://api.flutter.dev/flutter/widgets/SliverOpacity/SliverOpacity.html)
- [Native renderer](https://api.flutter.dev/flutter/rendering/RenderSliverOpacity-class.html)
- Pinned SDK: packages/flutter/lib/src/widgets/sliver.dart and
  packages/flutter/lib/src/rendering/proxy_sliver.dart (layout, paint, hit testing
  and semantics traversal).
