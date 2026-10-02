# SwitchListTile — Standard and Adaptive

Baseline: Flutter **3.44.8**, reviewed on 2026-09-08 against the pinned
`material/switch_list_tile.dart`, shared `switch.dart`, `list_tile.dart`
and their actual constructor/runtime branches. The online
[class documentation](https://api.flutter.dev/flutter/material/SwitchListTile-class.html),
[standard constructor](https://api.flutter.dev/flutter/material/SwitchListTile/SwitchListTile.html)
and [adaptive constructor](https://api.flutter.dev/flutter/material/SwitchListTile/SwitchListTile.adaptive.html)
are navigation references; the pinned source is authoritative for this release.

## Admitted surface

Material palette order **270**, type `flutter.material.SwitchListTile`.
Both constructors are const-capable. The adaptive signature has 45 arguments
including Designer-owned key: **41 non-slot SDK leaves**, three optional slots,
plus one Designer constructor selector produce **42 direct fields** and
**236 editable property rows** after the shared compound projections.

| Projection | Count | Parameter orders |
|---|---:|---|
| Optional title/subtitle/secondary slots | 3 | 0–2 |
| Direct SDK leaves and variant selector | 42 | 3–44 |
| Local VisualDensity axes | 2 | 45–46 |
| Local tile ShapeBorder family | 21 | 47–67 |
| Four state-color families, nine entries each | 36 | 68–103 |
| Nine thumb-Icon buckets, mode plus thirteen Icon fields | 126 | 104–229 |
| Nine local mouse-cursor entries | 9 | 230–238 |

The SDK leaves are value, onChanged, activeColor, activeThumbColor,
activeTrackColor, inactiveThumbColor, inactiveTrackColor, activeThumbImage,
onActiveThumbImageError, inactiveThumbImage, onInactiveThumbImageError,
thumbColor, trackColor, trackOutlineColor, thumbIcon, materialTapTargetSize,
dragStartBehavior, mouseCursor, overlayColor, splashRadius, focusNode,
statesController, onFocusChange, autofocus, applyCupertinoTheme, tileColor,
isThreeLine, dense, contentPadding, selected, controlAffinity, shape,
selectedTileColor, visualDensity, enableFeedback, horizontalTitleGap,
minVerticalPadding, minLeadingWidth, minTileHeight, hoverColor and
internalAddSemanticForOnTap.

Creation stores only `value: false`, no-op `onChanged` and
`variant: standard`; slots are empty. Required bool value is never nullable.
There is no synthetic Enabled, Tristate, padding, trackOutlineWidth, focusColor
or titleAlignment field. The tile's enabled state follows onChanged.
An explicit null callback is valid and disables activation; resetting required
onChanged is rejected instead of silently inventing enabled state.

## Constructors, slots and coupled constraints

- The selector admits only Standard and Adaptive. Adaptive uses the pinned
  SDK's Cupertino-style rendering on Apple platforms. That does not imply
  the widget tree contains a separate CupertinoSwitch; the pinned Switch
  implementation uses its current unified rendering branch.
- applyCupertinoTheme is nullable and Adaptive-only. Stored values and State
  bindings remain visible but inactive in Standard, and do not generate an
  argument, import or proof. Returning to Adaptive restores the same data.
  Setting this inactive property never switches constructors or clears it.
- Title, Subtitle and Secondary are independent optional single any-widget
  slots. Explicit `isThreeLine: true` requires a nonempty Subtitle. Removing
  the needed subtitle or applying an incomplete edit is rejected atomically.
- Non-null onActiveThumbImageError/onInactiveThumbImageError requires its own
  image provider. Omitted or explicitly null error callbacks do not.
- Whole and local shape, density, state-color, thumb-Icon and cursor modes are
  mutually exclusive. A local cursor map requires an explicit non-null Default.
  A thumb-Icon inherit bucket cannot also contain local Icon fields.

Selected is independent of Value. Selected title color follows
`activeThumbColor ?? activeColor ?? SwitchTheme.thumbColor.resolve(...)`,
with the selected state only when selected, then colorScheme.secondary.
The widget's local thumbColor map does **not** color its selected title.
Shape/density/content padding apply to ListTile, not to the switch.
MouseCursor applies to the inner switch. The tile supplies shrinkWrap for an
omitted materialTapTargetSize. Omitted nullable fields preserve SDK/theme rules.

## Typed styles, references and images

The direct colors admit literal/theme colors and strict Color references.
Whole state colors use WidgetStateProperty<Color?>; whole thumbIcon uses
WidgetStateProperty<Icon?>. Nine local state buckets reuse the standalone Switch
priority and explicit-null inheritance behavior. Each local Icon retains all
thirteen admitted SDK Icon fields and its independent empty/inherit mode.
Shape reuses all ten reviewed local Card shape variants or a ShapeBorder
reference. Density supports a typed VisualDensity or bounded local axes.
Cursor supports all 41 existing closed presets, strict MouseCursor references
including stateful subtypes, or the nine-state local cursor map.

Object and callback references retain the existing closed current/imported
root/member/getter/zero-argument-factory representation, exact SDK expected type,
source provenance and strict static proof. Raw Dart, arbitrary expressions,
dynamic/wrong-type references and unproved factory results are not admitted.
Project FocusNode and WidgetStatesController lifecycle remains user-owned;
creating or disposing those objects is not delegated to the Designer.

Image providers remain the **existing declared Flutter asset contract**:
AssetImage, ExactAssetImage with explicit scale, package assets and optional
ResizeImage configuration. This slice does not add NetworkImage, FileImage,
arbitrary custom project ImageProvider references or image downloading.
Image-error events remain usable with each admitted provider branch.

Signed finite tile geometry and splash radius retain the shared SDK-facing
contracts; splashRadius also admits explicit null, Infinity, negative Infinity
and NaN. Unsafe resolved mounted geometry is diagnosed in preview instead of
silently rewriting the stored value or generated source.

## Events and State

All four callbacks appear once in Events, not duplicated as property rows:

| Event | Exact callback contract |
|---|---|
| onChanged | ValueChanged<bool>, void Function(bool value) |
| onFocusChange | ValueChanged<bool>, void Function(bool hasFocus) |
| onActiveThumbImageError | ImageErrorListener, void Function(Object exception, StackTrace? stackTrace) |
| onInactiveThumbImageError | ImageErrorListener, void Function(Object exception, StackTrace? stackTrace) |

No-op, explicit null and strict references remain distinct; omission is admitted
for the three optional callbacks. Event handlers are user-owned source outside
managed regions. Creating/binding, disconnecting, history and save/reopen use the
shared exact-pair workflow. The SDK forwards onFocusChange to **both outer
ListTile and the inner Switch**, with the inner switch inside ExcludeFocus.

The controlled State producer uses a bool field and bool update argument.
Selected does not automatically follow Value. The six reviewed consumer fields
are dense, autofocus, enableFeedback, selected, internalAddSemanticForOnTap and
applyCupertinoTheme; DIRECT, NOT and EQUALS transforms use the existing typed
State model. Literal preview values, field ownership and user handler bodies
are preserved. Inactive Standard applyCupertinoTheme bindings remain stored
without participating in active source/import/proof requirements.

## Isolated Canvas and boundaries

Canvas constructs the actual standard/adaptive SDK widget, with its real tile,
switch, three slots and callback-presence semantics. No-op callbacks used by the
isolated runner are not user handlers and never mutate the persisted model.
Project callbacks, getters, factories, FocusNode/controllers and object
references are never executed. Unsupported custom-reference appearance or
runtime behavior is disclosed as an explicit approximation/unavailable detail,
not claimed as the project result. Declared image assets use the reviewed
resource transport and real SDK provider decoding.

The SDK's MergeSemantics behavior remains in force; rich children needing
independent semantics can conflict with that contract. Material ink can be
obscured by intervening painting according to SDK behavior. Adaptive rendering
can ignore Material-only visual options on Apple platforms without deleting
those source values.

This completes the public constructor surface through the existing admitted
typed representations, not every possible implementation of an arbitrary
ImageProvider, controller, state object or custom shape. It does not modify
custom clippers or any deferred platform/input provider work.

## Inventory and verification

Current catalog: **95 widgets, 4,231 properties, 87 const definitions**.
Supported callables: **150 across 27 widget types**, including **129 native
Events across 24 types**, 17 builders, 2 predicates and 2 formatters.
Controlled State families: **10**; reviewed State consumers: **135 properties
across 48 types**. The bounded admitted-constructor callable audit still has
zero known gaps; this is not full Flutter/palette completion.

Wire formats remain **FD schema 16 / Catalog API 15 / Canvas model 19 /
transport protocol 1**. No migration or new value kind is introduced.

Measured results received from the coordinating run:

- Production compile passed.
- Full Designer core regression: 2,053 tests passed, zero failures/errors/skips.
- Five real-SDK Java tests passed, including 33 generated/analyzed candidates
  (four dense, one 61-tile matrix, two whole-reference forms and 26 adversarial
  candidates) and two State-generated applications with six Flutter runtime
  cases.
- Isolated Canvas: 1,883 Flutter tests passed; final Web resources are ready.
- Selected NetBeans plugin regression: 1,255 tests in 52 suites passed, including
  the seven new property-editor cases and the new exact save/reopen/history
  scenario. The selected analyzer regression added 40 passing tests; package
  integration checks added nine. No failures, errors or skips in these runs.
- `flutter analyze` passed. All 40 source-manifest and 35 Web-manifest entries
  were independently checked for exact size and SHA-256 after the final build.
- Reactor `verify`, `install`, root `nbm:cluster` and module `nbm:cluster` passed.
  All 1,672 module payload entries match across the built JAR, NBM and both
  launch clusters; the Designer, analyzer and Canvas dependency JARs also match.
  The SwitchListTile schema class, SVG and FD schema 16 are present.

Automated acceptance covers all 236 leaves and ten local shapes, constructor/slot
combinations, typed proof positives/adversarial references, four Events,
producer/consumer State, retained inactive bindings, undo/redo/save/reopen,
actual Android/iOS adaptive rendering, SDK semantics and packaged resource
identity. Platform rendering tests run under Flutter's widget-test harness;
this is not a claim of a new manual run inside the installed NetBeans IDE.
The running IDE and user Flutter project were not restarted or modified.

Packaged snapshot, measured 2026-09-08:
`netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
**8,252,433 bytes**, SHA-256
`5e23094b585d71ec63437056ac8c58d3e48d8ed0af1e2477f1c7bcfb95d5b538`.
Both launch clusters under the G: checkout contain this matching payload.
