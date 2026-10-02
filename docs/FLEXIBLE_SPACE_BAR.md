# FlexibleSpaceBar (Flutter 3.44.8)

## Native contract

All seven constructor arguments beyond Key are supported: five typed properties
and two independent optional single-child slots. This is the native const
Material FlexibleSpaceBar, not a Sliver and not a generated wrapper.

Sources: [constructor](https://api.flutter.dev/flutter/material/FlexibleSpaceBar/FlexibleSpaceBar.html)
and [class](https://api.flutter.dev/flutter/material/FlexibleSpaceBar-class.html);
checked against the locally installed Flutter 3.44.8 SDK.

| Argument | Supported representation |
| --- | --- |
| centerTitle | Unset, null, true/false checkbox |
| titlePadding | Unset, null, nonnegative EdgeInsets/EdgeInsetsDirectional, current/imported EdgeInsetsGeometry? value or zero-argument factory |
| collapseMode | Unset or parallax, pin, none |
| stretchModes | Unset, all eight combinations of zoomBackground/blurBackground/fadeTitle, or typed current/imported List<StretchMode> value/factory |
| expandedTitleScale | Unset or finite number >= 1, including exactly 1 per the native constructor assertion |
| title | Optional single box child |
| background | Optional single box child |

No native constructor callbacks exist, so no invented Events are exposed.
Key remains Designer-managed stable identity as for the existing palette.

Unset values omit arguments and preserve SDK defaults. The explicit 'none'
stretch preset emits const <StretchMode>[]; it is distinct from unset, whose
native default is [StretchMode.zoomBackground]. Effect order/duplicates do not
change native effects because the SDK checks list membership. A project-owned
list retains its exact expression and runtime semantics.

Strings are a closed preset grammar, not raw Dart. Generated StretchMode symbols
participate in import aliasing and navigation evidence. Source references have
strict generic/nullable Analyzer proof; wrong types, dynamic, List<dynamic>,
List<String> and a nullable list at the non-null list argument are rejected.
Nullable EdgeInsetsGeometry producers are accepted. Project code is never run
by Canvas.

## Designer and Canvas

Material category order 510, five stable/resettable Properties rows, independent
Title and Background Slots, ordinary box placement, all four light/dark 16/32 SVG
variants, exact FD/Dart round-trip, undo/redo and pair-save admission.

Canvas uses native FlexibleSpaceBar inside AppBar or any SliverAppBar constructor.
It preserves collapse, stretch, RTL/directional padding and live property changes.
Both empty slots have reachable drop regions: Background uses the full box while
Title has a higher-priority bottom band. Move previews use host-authorized
placement; Java remains responsible for structural/cycle validation.

FlexibleSpaceBar requires inherited FlexibleSpaceBarSettings. A root or other
box placement is not rejected because an outer application may supply them.
Canvas without those settings, with unbounded geometry or non-finite scaled
title geometry shows an explicit unavailable-preview message and retains its
modeled children. No fake settings are inserted into generated Dart.

Project padding/effect references have labeled native-default preview fallbacks
and stay inert. The shared optional padding decoder also accepts the existing
SliverAppBar actionsPadding presence marker without casting it as local insets.
The companion [FlexibleSpaceBarSettings](FLEXIBLE_SPACE_BAR_SETTINGS.md) is now
admitted for box children. Static createSettings is a separate convenience helper;
the direct Settings constructor is used for explicit stored values.

## Inventory

161 definitions: 153 scalar-property and eight structural definitions; 6924
writable rows (6906 outside Scaffold); 131 const-capable definitions. Material
48, Scrolling 46. The 157 ordinary destinations (142 AnyWidget plus 15 constrained)
produce 25277 candidates: 16899 accepted and 8378 rejected. Required wrappers 28.
631 boolean-only rows and 48 nullable boolean unions. Native Events remain 154
across 40 types; all callables 201 across 54 types.
FD 16, Catalog API 15, Canvas model 19 and transport 1 remain unchanged.

## Verification

- FlexibleSpaceBarRealSdkTest passes with Flutter 3.44.8 explicitly enabled:
  four owning AppBar variants, all eight list presets, current/imported values
  and factories, nullable/generic strict rejection checks, save/reopen and history.
  Twelve generated native Flutter widget runtime cases also pass.
- The dedicated Canvas contract has 89 passing tests, including all 72
  owner/collapse/effects combinations, eight actual overscroll-effect checks,
  RTL, live edits, drop/move, missing settings and reference fallbacks.
- Full Designer core: 2335 tests, 20 conditional skips, no failures or errors.
- Focused Java regression: 1345 tests, two conditional skips, no failures or
  errors, including palette/drop, stable Properties, capability/constructor
  inventories, canonical Canvas parity, Events and four SVG variants.
- Complete Canvas suite: 5010 tests passed. Flutter analysis reports no issues;
  the established offline main_web.dart release build passes.
- The unrelated dirty-worktree JnaWindowsNativeCanvasApiTest DPI assertion
  expects 145 while its injected stub returns 144. It remains untouched and
  is explicitly excluded from this slice's full Java regression command.
- Four light/dark SVG variants were rasterized for visual review; runtime
  resources remain SVG only.
- Full Java regression command (all *Test classes except the named legacy DPI
  class): 5711 tests, 208 conditional skips, no failures or errors; Maven exit 0.
  Surefire reported a 30-second fork shutdown timeout after System.exit(0)
  in NetBeans filesystem-lock cleanup; this is not reported as a clean JVM shutdown.
- All other 21 JnaWindowsNativeCanvasApiTest methods pass in an explicit
  method-filtered follow-up. Combined fresh Java reports: 5732 tests,
  208 conditional skips, no failures or errors; only the known unrelated
  DPI method remains unrun.
- Maven install and nbm:cluster pass. Packaged/current source verification
  passes for the schema and evidence classes, all four SVG variants, all
  40 runner source entries and all 35 offline Web artifact entries. The module,
  Designer, runner and Analyzer JARs in the development cluster match the NBM
  byte for byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8791918 bytes. SHA-256:
`752cc8793e4a61a62c23ba1ff98655c5642f45050a275d2553588b40677545b0`.
- No manual test in the user's interactive desktop IDE has been performed.
