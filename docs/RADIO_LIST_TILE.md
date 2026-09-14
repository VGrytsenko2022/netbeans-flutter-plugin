# RadioListTile — Standard, Adaptive and typed group selection

Baseline: Flutter **3.44.8**, reviewed against the installed
`material/radio_list_tile.dart`, `material/radio.dart`, `widgets/radio_group.dart`
and `material/list_tile.dart`. Online [standard constructor](https://api.flutter.dev/flutter/material/RadioListTile/RadioListTile.html)
and [adaptive constructor](https://api.flutter.dev/flutter/material/RadioListTile/RadioListTile.adaptive.html)
are references; the pinned SDK implementation determines this release's contract.

## Constructor coverage

Material palette order **280**, type `flutter.material.RadioListTile`.
Both constructors are const-capable. Adaptive has 41 arguments including the
Designer-owned key: **37 scalar SDK arguments and three optional widget slots**.
The variant, valueType and nullableValueType selectors produce **40 direct
fields**, expanded to **153 editable properties** through shared compounds.

| Surface | Count | Orders |
|---|---:|---|
| Title, Subtitle, Secondary slots | 3 | 0–2 |
| Direct SDK arguments and Designer selectors | 40 | 3–42 |
| Local VisualDensity axes | 2 | 43–44 |
| Local tile ShapeBorder | 21 | 45–65 |
| Three nine-state color families | 27 | 66–92 |
| Nine radioInnerRadius entries | 9 | 93–101 |
| Local plain/stateful radioSide | 45 | 102–146 |
| Nine mouse-cursor entries | 9 | 147–155 |

Creation stores `value: 'option'`, `valueType: String`, `variant: standard`
and an explicit no-op `onChanged`. Only the first three are required model
fields: resetting onChanged truly omits it, and explicit null remains distinct.
No title or other child is fabricated. There is no groupRegistry or focusColor
constructor argument on this widget, and neither is synthesized.

All three slots are optional and independent. Explicit isThreeLine true requires
an existing Subtitle. Incomplete changes and removal of a needed subtitle are
rejected atomically. Omission/null preserves the SDK's ListTileTheme behavior.
ControlAffinity leading/platform puts the Radio before the title; trailing puts
it after, with Secondary on the opposite side. Selected tile appearance is
independent of group selection. Selected title color uses activeColor, then
RadioTheme.fillColor resolved with selected when applicable, then
colorScheme.secondary; the tile's local fillColor map does not color its title.

Adaptive-only useCupertinoCheckmarkStyle is a non-null boolean. Values and State
bindings stay stored but inactive and ungenerated in Standard. Returning to
Adaptive restores their effect; editing the inactive value does not select a
different constructor or silently erase it. Shape, padding, density, title
alignment and the tile's geometry apply to ListTile. Radio-specific style and
scale apply to the control; the default tap-target size is shrinkWrap.

## Generic values, styles and typed references

The explicit T admits String, int, double, num, bool and Object, or a simple
project class/enum/typedef reference, with optional nullable T. Value must match
T; legacy groupValue and callback parameters use T?. Arbitrary Dart expressions,
member/invoked type declarations, dynamic aliases and unproved references are
not accepted. A named typedef can represent a complex project generic type.
The type editor changes Type, nullability, Value, Group value and On changed
together; one Undo restores the previous values and Cancel publishes nothing.

Compound colors, state colors, shape, density, radioSide, radioInnerRadius and
cursor retain their reviewed shared types and explicit-null/default semantics.
All ten local tile shapes, nine state buckets and 41 cursor presets are exposed.
Whole references and local fields are exclusive; local cursor maps need an
explicit Default. Object getters/member references and zero-argument factories
must satisfy exact source-bound static type proofs, not merely be assignable
from dynamic. The source/probe paths retain the actual radioSide*,
radioBackgroundColor* and radioInnerRadius* model field names.

Finite signed geometry, scale zero and nonfinite SDK numeric arguments remain
subject to the declared property domains. radioScaleFactor accepts finite
numbers, Infinity, negative Infinity and NaN, but not null; splashRadius also
accepts explicit null. Unsafe mounted geometry is diagnosed in preview rather
than rewriting the model/source. Other numeric compounds retain their existing
shared Radio/ListTile constraints.

## Events and State

Two actual SDK events are exposed: onChanged (`ValueChanged<T?>`) and
onFocusChange (`ValueChanged<bool>`). Handler source remains user-owned outside
managed regions. The generated generic event adapter is type-safe; general
event stubs may accept Object? contravariantly, while State update handlers use
the exact selected nullable type. onFocusChange belongs to the outer ListTile;
the inner Radio is inside ExcludeFocus and does not receive that callback.

Modern RadioGroup ownership is preferred. RadioListTile registers itself as a
RadioClient; its SDK-owned inner Radio must not become a second group member.
The pinned SDK notifies the matching ancestor group first and then invokes an
optional legacy tile callback. Effective group value is
`registry?.groupValue ?? widget.groupValue`: a null modern group value can fall
back to an explicitly configured legacy value. Do not configure both selection
owners accidentally. An explicit null legacy callback does not disable a tile
that still has a matching RadioGroup.

Legacy State binds groupValue and onChanged to a typed nullable field and update
method. Shared fields, handler/field rename, literal previews, removal,
save/reopen and exact Undo/Redo use the existing State workflow. Modern forms
instead bind State on RadioGroup. The eight reviewed boolean consumers are
toggleable, dense, selected, autofocus, enableFeedback, enabled,
internalAddSemanticForOnTap and useCupertinoCheckmarkStyle; direct, NOT and
EQUALS transforms retain the existing typed consumer contract.

Enabled is the actual nullable SDK property. Omission/null infers activation
from the legacy callback or group. Explicit true, including a State-produced
true, requires a callback or matching ancestor at mount time. This runtime
assertion is not waived by the Designer. isThreeLine is not a State consumer
because it is coupled to the Subtitle slot. No fictitious Tristate property is
added; Toggleable requests null when an already selected radio is activated.

## Canvas isolation and boundaries

Canvas uses the actual SDK constructors and public radio-group contracts.
Project callbacks, getters, factories, controllers and custom equality code are
never executed. Closed scalar groups use the same exact T/nullability identity
rules as the existing Radio and RadioGroup preview. Unknown project types or
values are explicitly diagnosed rather than assigned invented equality.
Duplicate selected clients and unsafe enablement/geometry retain their guarded
preview behavior and preserved child slots.

The registry observer mirrors the pinned SDK tile's inherited group and theme
dependencies. It also flushes an independently dirty direct SDK child before
restoring its public registry. This preserves the duplicate-selection guard
through theme-only updates with a cached Canvas child, as well as group moves
and generic/nullability changes; the SDK-owned inner Radio is never registered
as a second ancestor-group client.

The SDK's MergeSemantics contract remains in effect. A rich child requiring an
independent semantic node can conflict with this framework constraint. Native
keyboard traversal must visit each tile only once and skip disabled clients,
including groups that mix standalone Radio and RadioListTile.

Wire formats remain FD schema 16, Catalog API 15, Canvas model 19 and transport
protocol 1. No migration, arbitrary-code editor or new platform provider is
introduced by this slice.

## Verification

Measured on 2026-09-08 in the canonical G: checkout with Flutter 3.44.8:

- Full Designer core: **2,072 tests**, no failures, errors or skips, including
  pinned-SDK constructor and generated Events/State contracts.
- RadioListTile real-SDK acceptance: **9 Java tests / 93 candidate analyses**
  (43 accepted, 50 deliberately rejected). One accepted candidate contains a
  74-tile constructor/slot/null/geometry matrix. These gates retain exact static
  type and source provenance checks; dynamic or incompatible references do not
  acquire save authority.
- Generated State forms: **1 Java test / 14 Flutter runtime tests**, covering
  standard/adaptive, legacy/modern ownership, Android/iOS, null toggling,
  focus, merged semantics, mixed Radio/RadioListTile keyboard traversal,
  handler preservation and save/reopen/history.
- Selected plugin regression: **1,289 tests**; analyzer regression: **40 tests**;
  package integration: **9 tests**. All passed with zero skips. The lifecycle
  regression explicitly verifies that an unsaved external Source edit cannot
  admit a State command, then saves the user body and continues editing,
  reopen, reset and exact Undo/Redo without altering that body.
- Canvas: **32 targeted RadioListTile tests** and the full **1,915-test Flutter
  suite** passed; `flutter analyze` reported no issues. Coverage includes all
  local shape families, state priority, LTR/RTL drop placement, guarded slots,
  first-frame/group-move membership and independently rebuilt theme regression.
- Release Web build passed. All **40 source** and **35 Web** manifest records
  were independently checked against actual sizes and SHA-256 values.
  `main.dart.js`: **3,377,744 bytes**, SHA-256
  `315f53ea1b35f951c19759599de0a0bc0b9bbd8673abd9809a99380bb1805e10`.
- Maven `verify`, `install` and both `nbm:cluster` runs passed. The **1,676 module
  payload entries** match between the built JAR, NBM and both root/module
  development clusters. Designer, analyzer and Canvas dependency JAR hashes
  also match; the new SVG/schema class and unchanged FD schema 16 are present.

Output: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
**8,277,780 bytes**, SHA-256
`22281ba2ad93683334e57f73707b37e9fb739b318e6f3886eccd38e22f3f2c28`.

No manual test in the installed NetBeans IDE is claimed. No IDE was restarted,
no user Flutter project was modified, and no new platform provider was added.
