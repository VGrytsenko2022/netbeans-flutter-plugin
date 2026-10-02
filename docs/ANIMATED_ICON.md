# AnimatedIcon

Pinned implementation: Flutter 3.44.8, material/animated_icons/animated_icons.dart.
[Constructor](https://api.flutter.dev/flutter/material/AnimatedIcon/AnimatedIcon.html)
and [AnimatedIcons](https://api.flutter.dev/flutter/material/AnimatedIcons-class.html).

## Full constructor

All six non-Key constructor parameters are represented in SDK order. Shared
Key/identity remains unchanged. This is a const-capable, childless Material item.

- Icon: required AnimatedIconData. All 14 AnimatedIcons presets or a typed
  project reference/getter/factory. Creation uses menu_close.
- Progress: required Animation<double>. A local signed finite number generates
  AlwaysStoppedAnimation<double>; creation uses 0. Project animations remain live.
  Flutter clamps progress at paint time to 0..1; stored values are not clamped.
- Color: optional ARGB, ColorScheme token, null/unset or Color? source.
- Size: optional signed finite logical pixels, null/unset or double? source.
- Semantic label: optional string, including empty, null or unset.
- Text direction: optional ltr/rtl/null/unset; omission inherits Directionality.

Icon and Progress cannot be omitted or reset. Source bindings support all eight
current/package, root/member, reference/zero-argument factory combinations.
Strict analyzer evidence rejects wrong types, nullable required outer types and
dynamic sources. Nullable optional Color/Size sources may return null.

## Previews and native behavior

The preset selector and property row use SDK-derived SVG previews at progress
0%, 50% and 100%, in light and dark colors. All 14 presets are covered:
add_event, arrow_menu, close_menu, ellipsis_search, event_add, home_menu,
list_view, menu_arrow, menu_close, menu_home, pause_play, play_pause,
search_ellipsis and view_list.

The generator at scripts/generate-animated-icon-previews.mjs reads pinned SDK
path frames and opacities; it emits deterministic SVG artifacts without font
glyph substitutions. The two 96x96 source icons are normalized along with the
48x48 icons. Flutter's license is retained beside the preview resources.

Unset/null Color and Size inherit IconTheme. IconTheme opacity also applies to
explicit colors. Other IconTheme fields are not invented as AnimatedIcon inputs.
Zero/negative sizes follow native CustomPaint constraints and paint scaling;
Designer retains a selectable zero-size target. Ambient/explicit direction and
the pinned SDK's actual RTL transform are preserved.

## Project animation and SDK restriction

The application's animation/controller owns its lifetime. Generated Dart keeps
native repaint subscription, forward/reverse playback, source replacement and
listener detachment. Designer does not create or dispose user controllers.

AnimatedIconData is public but Flutter paints it using a private implementation
cast. A project reference must return one of Flutter's AnimatedIcons data
objects. An arbitrary custom AnimatedIconData subclass fails that SDK cast:
custom vector data is not supported by this Flutter API. The property editor
states this restriction instead of promising custom vector rendering.

## Canvas safety and Events

Canvas uses a real AnimatedIcon and stopped local progress. Project Dart never
executes in Designer. Source Icon previews menu_close, source Progress previews 0,
and source Color/Size inherit IconTheme. Tooltips name only substituted fields;
exported Dart retains the original typed source.

AnimatedIcon is not a button, has no child slot and has no native event callback,
Duration, Curve or On end parameter. Existing GestureDetector/Listener/MouseRegion/
Focus/Tooltip wrappers can provide interaction without inventing widget Events.
Semantic labels are retained without adding button or tap semantics.

## Inventory and verification

209 built-in definitions, 7,302 writable rows (7,284 outside Scaffold),
201 typed property definitions, 175 const-capable definitions and 52 Material
entries. Native Events remain 174 rows across 60 types; callables remain 237
across 86 types, including 48 builders and 10 delegates. FD 16, Catalog API 15,
Canvas model 19 and transport 1 are unchanged.

Tests cover all presets and constructor branches, typed sources, invalid types,
stable property rows, cancel-safe editing, save/reopen and undo/redo, native
pixel parity in LTR/RTL, live animation repaint/listener ownership, theme opacity,
zero-size selection, semantics and the SDK custom-data limitation.
The pinned SDK integration test passed 138 generated/native runtime scenarios.
The focused Canvas suite includes 143 scenarios, including all 14 glyphs in
LTR/RTL and actual pointer selection of a zero-size transparent icon.

Full Canvas suite: 11,215 passed. Flutter analyze: no issues. Fresh Java reports:
3,981 tests across 594 suites, zero failures/errors, 102 conditional skips.
The AnimatedIcon real-SDK test ran explicitly; other SDK-dependent gates remain
conditional. Web release build, Maven install and nbm:cluster passed.

Packed verification matched the schema/editor/evidence classes, four palette
SVG variants, all 28 preview SVGs and Flutter license, 40 runner sources and
35 Web artifact files. All four development-cluster JARs match the NBM.
The active IDE was not restarted.

Artifact: netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm
(9083969 bytes, 2026-09-14T18:20:11.4783948Z).
SHA-256: 9ca597138dc4b60236b16768c96b6b4e22e38042bb021f2ea84e7a0843656adf.
