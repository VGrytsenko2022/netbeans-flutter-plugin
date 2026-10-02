# SliverAnimatedOpacity

Complete constructor reviewed against Flutter 3.44.8:
`const SliverAnimatedOpacity({Key? key, Widget? sliver, required double opacity,
Curve curve = Curves.linear, required Duration duration, VoidCallback? onEnd,
bool alwaysIncludeSemantics = false})`.

## Properties and Events

All constructor arguments except Designer-owned Key are implemented.

- Opacity: required finite target from zero to one, initially one. Integer
  endpoints and fractional doubles are supported; omission/null are rejected.
- Curve: all 43 pinned Curves presets, or an analyzer-verified Curve
  reference/member/zero-argument factory. Omission preserves linear;
  explicit null and arbitrary source strings are rejected.
- Duration (microseconds): required integer from zero through the exact portable
  limit 9007199254740991, or an analyzer-verified Duration reference/factory.
  Palette creation uses 300000 microseconds (300 ms). Zero completes without a
  timed transition. Negative literal durations are rejected for animation safety.
  Runtime validity/lifecycle of project-owned objects remains user-owned.
- On end: optional VoidCallback reference/member/factory or explicit null,
  integrated with the shared Events handler creation/navigation workflow.
  The event occurs on completed transitions, not initial mount or an unchanged
  target. Retargeting interrupts the unfinished transition.
- Always include semantics: optional centered checkbox; omitted means false.
  Restore Default removes the explicit argument.

Curve uses the shared preset/reference editor with all 43 choices; Duration
uses the typed literal/reference editor. Properties remain writable after
reopening, keep stable row identity and preserve omission versus explicit null.

The optional Sliver slot supports insertion, replacement, removal, moving and
nested wrapping. It accepts slivers, not boxes or SliverCrossAxisExpanded
detached from its required direct group parent. The wrapper cannot be root.
Empty/omitted Sliver remains empty in FD but generates a zero-extent
SliverToBoxAdapter, avoiding Flutter 3.44.8 proxy-sliver null-child layout failure.

## Canvas and source

Canvas mounts the native SliverAnimatedOpacity and SliverFadeTransition.
Transitions preserve child identity and layout/scroll extent; zero opacity does
not disable pointer hits. Semantics follow opacity and Always include semantics.
Designer selection and bounded sliver drop targets remain usable while invisible.

Project code is not executed in isolated Canvas. Curve references preview as
linear, Duration references as 300 ms and On end is disconnected. Only presence
is transported for project-owned references; names or bodies are not executed.
Generated application Dart preserves the actual typed expressions and callbacks.

Integer microseconds generate a real const Duration with exact symbol evidence.
The pair-save gate admits only the generator-owned SliverAnimatedOpacity Duration
probe ID, exact symbol/span/type metadata and trusted SDK core duration targets.
It does not admit arbitrary dart:core symbols or loosen navigation validation.
Adversarial tests cover forged IDs, widgets, families, spans, symbols, libraries,
type metadata, roots, target kinds, missing evidence and post-CAS target changes.

## Inventory at this slice

152 definitions: 146 typed, six structural; 6,516 scalar rows (6,498 outside
Scaffold), 122 const definitions and 41 Scrolling entries. 560 boolean-only rows
and 44 nullable boolean unions. Required-child wrappers remain 28.
There are 136 insertion destinations: 124 AnyWidget and twelve trait-restricted
(ten Sliver and two PreferredSize). The 20,672-pair matrix has 14,601 accepted
and 6,071 rejected combinations.

Native Events are 151 across 37 types; all callables total 197 across 50.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted constructor definitions, not a count of all Flutter widgets
or a verified remaining backlog.

## Verification

- Constructor, FD round-trip, source generation, strict domains and placement:
  endpoint/fractional opacity, semantic states and empty/populated slots; every
  one of 43 curve presets at four exact durations; typed references and commands.
- 276 focused Flutter tests, including 144 geometry/semantics cases across axes,
  reverse and RTL; all 43 curves with zero, one-microsecond and timed transitions;
  exact midpoint/end/retarget behavior, child identity, optional-slot drop,
  invisible selection and non-executing project-reference fallbacks.
- Full Flutter runner: 4,582 tests passed. Final Dart analysis: no issues.
- Full core: 2,308 tests, zero failures/errors, 20 conditional skips;
  318 fresh reports.
- Real Flutter SDK: one unskipped integration test passed in 25.013 seconds.
  Nine analyzer-accepted transitions and one intentionally rejected Curve-as-
  Duration assignment; paired save/reopen, preserved user members and exact
  history. Generated/native Flutter verification has 49 cases, including
  custom objects, callback completion/interruption/zero-duration behavior
  and pointer hits at zero opacity.

- Broad Java selection: 1304 tests, zero failures/errors, 18 conditional
  skips and 98 fresh reports. Includes palette/tree/drop, Properties,
  Events bridge, capability parity, strict bundle contracts and all 72 pair-save
  gate tests. The real-SDK test above was run separately, not counted as executed
  when conditionally skipped in this selection. This overlaps full-core tests.
  The complete NetBeans reactor is not claimed (recorded Windows AWT shutdown issue).
- Release Web build passed. main.dart.js: 3698077 bytes; SHA-256
  `554e03324fde988466f7117766981c72dc3642202e3e105a418ea60ee7a5f493`. Source/Web manifests retain strict byte/hash checks.

NBM install and dev-cluster creation succeeded. Strict verification matched the
schema class, four light/dark 16/32 SVG assets, forty runner sources and the
thirty-five-file Web artifact with current inputs. All three plugin, Designer
and runner dev-cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8737960 bytes; SHA-256
`d07e2c90ac3579a179261a06bbf7ac5415ccc2062d782e6ef76fb4dd75890fbc`.

No manual IDE session or IDE restart is claimed. CJK IME and Linux/macOS
Canvas providers remain deferred. Next palette candidate at this milestone: SliverLayoutBuilder; now implemented in [its dedicated slice](SLIVER_LAYOUT_BUILDER.md).

## Sources

- [Official constructor](https://api.flutter.dev/flutter/widgets/SliverAnimatedOpacity/SliverAnimatedOpacity.html)
- Pinned SDK: packages/flutter/lib/src/widgets/implicit_animations.dart,
  packages/flutter/lib/src/rendering/proxy_sliver.dart and proxy_box.dart.
