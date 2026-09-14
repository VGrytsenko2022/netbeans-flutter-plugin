# SliverSafeArea

Complete constructor reviewed against the pinned Flutter 3.44.8 SDK and
[official constructor](https://api.flutter.dev/flutter/widgets/SliverSafeArea/SliverSafeArea.html).

`const SliverSafeArea({Key? key, bool left = true, bool top = true,
bool right = true, bool bottom = true, EdgeInsets minimum = EdgeInsets.zero,
required Widget sliver})`

## Properties and placement

The Scrolling palette exposes all five scalar arguments and the required
Sliver slot. Key remains Designer-owned stable identity. There are no native
event callbacks, alternative constructors or maintainBottomViewPadding parameter.

- Left, Top, Right and Bottom use the shared centered boolean checkbox;
  unset preserves the SDK's true default. Restore Default removes the argument.
- Minimum uses the typed physical EdgeInsets editor, preserving zero, signed
  finite and fractional components. Unset preserves EdgeInsets.zero.
  Directional insets, null, raw Dart strings and non-finite values are rejected.
- A new instance atomically wraps an existing compatible sliver in a list or
  occupied sliver slot. An empty destination cannot create an invalid required
  wrapper; no artificial child is stored.
- Sliver accepts sliver-trait widgets, not boxes or a direct
  SliverCrossAxisExpanded requiring CrossAxisGroup parent data.
  SliverSafeArea itself cannot be a document root.
- The required child cannot be removed or moved out. Atomic replacement,
  nested wrapping, moving the whole wrapper and exact Undo/Redo remain available.

Properties preserve row and property-set identity on refresh and stay editable
after reopening. Type validation, const/import generation, candidate symbol
analysis and paired FD/Dart persistence use the shared contracts, with no new
raw-code escape hatch or schema relaxation.

## Runtime behavior

Canvas uses Flutter's actual SliverSafeArea, SliverPadding and
MediaQuery.removePadding. Each physical side uses
`max(enabled ? MediaQuery.padding.side : 0, minimum.side)`.

Minimum is a lower bound, not an extra margin, and still applies when a side
is disabled. Signed minima never create negative applied padding because
the comparison includes nonnegative system padding or zero. Physical Left
and Right never swap in RTL.

Enabled sides remove consumed system padding for descendants; nested safe
areas do not double-count those system insets. A nested explicit Minimum is
independent and can apply again. Disabled sides leave their MediaQuery padding
available to descendants.

This constructor reads padding rather than viewPadding. If keyboard state
reduces bottom padding, SliverSafeArea does not preserve the old bottom
viewPadding. It does not consume viewInsets itself; the enclosing Canvas host
Scaffold may already have consumed keyboard viewInsets before the preview.
The slice adds no device-notch presets or keyboard simulator.

Designer selection, actual sliver geometry, nested wrapping targets, empty
child adapters and offscreen exclusion reuse the existing renderer-aware
contracts. There is no RenderBox shim around slivers or synthetic layout.

## Verified inventory

151 admitted definitions: 145 typed and six structural; 6,511 writable rows
(6,493 outside Scaffold), 121 const-capable definitions and 40 Scrolling entries.
559 boolean-only rows, 44 nullable boolean unions and 28 required-child wrappers.
The 135 insertion destinations remain 124 AnyWidget plus eleven trait-restricted
(nine Sliver, two PreferredSize). The 20,385 source/destination matrix contains
14,559 accepted and 5,826 rejected combinations.

Events remain 150 across 36 widget types; all callables remain 196 across 49.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Verification

- Core constructor/codec/generation tests cover all 81 omitted/false/true side
  combinations with omitted and signed Minimum (162 models), strict rejection,
  every admitted parent/child placement and required-child commands.
- 279 dedicated Flutter tests cover all 16 explicit side masks plus omissions,
  signed/omitted Minimum across both axes, reverse and RTL, native padding and
  scroll geometry, descendant MediaQuery, nested/keyboard-reduced padding,
  live child identity, required-wrapper drop targets and offscreen exclusion.
- Full runner: 4,306 tests passed; Dart analysis reported no issues.
- Full core: 2,302 tests, zero failures/errors, 20 conditional skips;
  316 fresh report files.
- Real pinned SDK: one unskipped JUnit integration test passed in 34.110 seconds.
  It checks 19 analyzer-backed edit transitions and 19 generated cases across
  eight axis/reverse/RTL layouts (152 native Flutter cases), paired save/reopen,
  user-member preservation, required-child guards and byte-exact Undo/Redo.

- Broad Java selection: 1,196 tests, zero failures/errors, 17 conditional skips;
  92 fresh reports. It covers all Sliver suites, palette/drop/tree, properties,
  icons, capability parity, events and strict native/Web bundle contracts.
  This overlaps the full core run; totals must not be added together.
  The complete NetBeans reactor was not run because of the recorded Windows
  AWT shutdown issue.
- Release Web build passed. main.dart.js is 3,695,713 bytes; SHA-256
  `d21865766f56efa6e9c38f0b3a550618757c7d952118f91e3c786ae58faca199`.
  Both manifests retain strict byte/hash validation.

NBM install and development-cluster creation passed. Verification checked the
packaged schema class, four light/dark 16/32 SVG assets, all forty runner
sources and the thirty-five-file Web artifact against current inputs.
All three plugin, Designer and runner development-cluster JARs exactly match
the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,732,106 bytes; SHA-256
`5d4a6015e0693f77da7006627802b98c8b5775d062cf432984cbe4a240fbf162`.

Manual NetBeans desktop testing or an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Next palette candidate at this milestone: SliverAnimatedOpacity (now completed; see [SliverAnimatedOpacity](SLIVER_ANIMATED_OPACITY.md)).

## Sources

- [SliverSafeArea API](https://api.flutter.dev/flutter/widgets/SliverSafeArea-class.html)
- [Constructor](https://api.flutter.dev/flutter/widgets/SliverSafeArea/SliverSafeArea.html)
- Pinned SDK: packages/flutter/lib/src/widgets/safe_area.dart.
