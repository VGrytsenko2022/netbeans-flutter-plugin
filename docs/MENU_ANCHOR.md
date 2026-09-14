# MenuAnchor — native menu, builder, Events and State

Baseline: Flutter **3.44.8**, checked against installed
`material/menu_anchor.dart`, `material/menu_style.dart` and `widgets/raw_menu_anchor.dart`.
References: [MenuAnchor constructor](https://api.flutter.dev/flutter/material/MenuAnchor/MenuAnchor.html),
[MenuStyle constructor](https://api.flutter.dev/flutter/material/MenuStyle/MenuStyle.html),
[MenuAnchorChildBuilder](https://api.flutter.dev/flutter/material/MenuAnchorChildBuilder.html).

## Constructor and palette

Material `flutter.material.MenuAnchor` covers all **19 constructor arguments**:
Designer-owned Key, **16 direct properties**, optional single **Child**, and
required **Menu children** list. Required means the named argument must be emitted:
the SDK permits an empty list. Both prototype slots start empty. Menu children may
be any Widget; Designer does not restrict them to MenuItemButton or invent a
required label. This is an ordinary Add operation, not a required-child wrapper.

The const constructor remains available. There are **219 editable properties**:
16 native direct properties and **203 local MenuStyle leaves**. Whole Style,
including explicit null, is exclusive with every local style field. No FD, API,
Canvas model or transport version change is necessary.

Current catalog: **102 widgets**, **5,300 properties** (5,282 outside Scaffold),
**94 const-capable types**. Callables: **164 across 32 types** — 139 Events across
29 types, 20 builders, two predicates, two formatters and one delegate. State:
**166 consumers across 55 types**, with the same 11 controlled producer families.
These are verified implemented totals, not a claim that Flutter's full catalog
is finished or a count of remaining widgets.

The direct properties are Controller, Child focus node, Style, Alignment offset,
Reserved padding, Layer link, Clip behavior, Anchor tap closes menu,
Consume outside tap, On open, On close, Cross-axis unconstrained, Use root overlay,
Animated, On animation status changed and Builder.

Nullable SDK values support omission and explicit null; non-null Clip and boolean
arguments permit omission but reject explicit null. Actual Dart references,
members/getters and zero-argument factories require strict non-null target types:
MenuController, FocusNode, MenuStyle, Offset, EdgeInsetsGeometry, LayerLink,
VoidCallback, ValueChanged<AnimationStatus> and MenuAnchorChildBuilder.
Imported references use the same exact analyzer gate. Wrong, dynamic and nullable
reference targets cannot acquire pair-save authority even if ordinary Dart
assignment would permit dynamic.

Alignment offset also offers the shared structured Offset editor. Reserved
padding offers physical/directional EdgeInsetsGeometry. Nonfinite Offset values
can be represented by a whole typed reference, but valid constructor/type
analysis does not promise runtime layout validity for every value.

## Opening the application menu

A Child alone does **not** open a native MenuAnchor. Designer does not attach
hidden click behavior or silently replace an existing interactive child.

**Create Menu Builder…** is a scoped, explicitly confirmed action for an unbound
MenuAnchor Builder. It creates one ordinary user-owned instance method with this
reviewed body and binds its typed method reference in the same analyzed change:

```dart
Widget _buildMenu(BuildContext context, MenuController controller, Widget? child) {
  return TextButton(
    onPressed: () {
      if (controller.isOpen) {
        controller.close();
      } else {
        controller.open();
      }
    },
    child: child ?? const Text('Menu'),
  );
}
```

Child becomes the TextButton's content. An already interactive child is **not**
rewired; it can consume pointer input itself. Use a label/icon as Child for this
template or edit the generated user-owned method for a different design.

The action inserts into the actual owning StatelessWidget or State class,
outside managed regions. It does not add a controller field, accept arbitrary
Dart code as a command argument, or broaden generic Event creation to builders.
No existing Builder is overwritten. Name collisions, stale source, cancelled
drafts and failed exact analyzer evidence leave both files unchanged.

Edit the method in Source or navigate to a verified current-library method from
the property editor. Later property edits, save/reopen and Undo/Redo preserve
user-written bodies. Reset Builder removes only the binding; it retains the
user-owned method. A second creation with that retained name is rejected instead
of overwriting it. Whole typed Builder references remain supported independently
of this optional template workflow.

The native builder-supplied context/controller/child are used directly.
Controller, FocusNode and LayerLink references supplied by application code
remain application-owned, including allocation and reuse. Application code disposes
objects that expose a disposal API, such as FocusNode; MenuController has none.
For application-specific trigger focus restoration/traversal, supply the matching
Child focus node and builder FocusNode. This simple opener does not allocate a
shared focus node or install global keyboard shortcuts. Native focused menu-item
keyboard handling, including Escape, remains available.

## Events and State

Three native Events are exposed:

- On open: VoidCallback.
- On close: VoidCallback.
- On animation status changed: ValueChanged<AnimationStatus>.

Builder is a Properties builder, **not** an Event. Generic Event actions cannot
create an invented onPressed or convert Builder into a void callback.

Four boolean State consumers are supported: Consume outside tap,
Cross-axis unconstrained, Use root overlay and Animated. There is no MenuAnchor
State producer.

**Pinned SDK deprecation:** Anchor tap closes menu is still an admitted
constructor argument, but Flutter 3.44.8 does not use it at runtime. It is stored
and generated faithfully with explicit help; it is not offered as a live State
consumer and Designer does not synthesize the old behavior.

Native lifecycle is retained: On open begins opening; On close runs after hiding.
An animated menu controller can remain isOpen during its closing animation.
Animated false yields completed/dismissed status notifications; Animated true
also produces transition statuses. Disposal does not invent project callbacks.

## MenuStyle, not ButtonStyle

All 13 native MenuStyle fields are supported. Eleven WidgetStateProperty fields
have nine source-representable buckets:

- Background, shadow and surface-tint colors; elevation.
- Padding; minimum, fixed and maximum sizes.
- Side, shape and mouse cursor.

Those form **9 × 22 = 198** local leaves. Visual density horizontal/vertical and
alignment kind/X/Y add five common leaves, for **203** total.

MenuAnchor resolves its panel WidgetStateProperties with an **empty state set**.
Hovered, pressed, disabled and other nonempty buckets are preserved in generated
source, but do not create live hovered/pressed menu-panel styles in this SDK.
WidgetStateMouseCursor also resolves its panel cursor with empty states.
No fake panel state is introduced.

Native value fallback is local resolved value, then MenuTheme's resolved value,
then native defaults. This differs from MenuItemButton's ButtonStyle object merge.
Sparse compound size/side/shape generation uses MenuTheme and the reviewed native
menu fallback, never TextButtonTheme, MenuButtonTheme, a ButtonStyle or a fabricated
MenuAnchor.defaultStyleOf API. Default panel shape is rounded radius four.

Sparse local size editing follows the existing Designer compound-size contract:
if a local minimum exceeds an inherited theme maximum, the generated maximum is
raised to that minimum. Contradictory local minimum/maximum values are rejected.
This normalization is intentional and is not raw native literal behavior for
contradictory runtime themes. Use a whole typed MenuStyle for exact project-owned
size resolution, including its native runtime assertion behavior.

Local VisualDensity creates a complete native VisualDensity object: an omitted
peer axis defaults to zero, just like the constructor. It does not inherit a
missing axis from a private parent-dependent menu theme. Alignment is a complete
physical/directional constructor with both axes.

Whole typed MenuStyle supports custom resolvers, custom shapes and other exact
application-owned combinations. The local editor does not add ButtonStyle-only
typography, foreground/overlay/icon colors, layer builders or feedback properties.

## Isolated Canvas

Canvas uses an owned, stable MenuController and the native MenuAnchor, plus an
explicit external **Preview menu / Close preview** Designer action. This chrome
does not become a model child, alter generated Dart or hijack child click handlers.
It can operate on a selected anchor or the owner of a selected open menu child.

Project builders, callbacks, controllers, focus nodes, layer links, getters and
factories are never executed. A project Builder uses the real Child/SDK empty
fallback with a diagnostic. A supplied controller is represented by the owned
preview controller; unavailable focus and layer-link behavior is diagnosed.
The generated user-owned opener is also project code, so it is not executed by
Canvas. Use Preview menu for isolated editing.

Open menu children keep their actual ordered slot identities for selection and
drag/drop. Closed or closing children are not offered as invisible insertion
targets. The ordinary child slot remains independent. Native close disposes
overlay child State; scalar edits while open must retain the active menu when
the native topology allows it.

Animated/layer-link topology changes use a narrow native identity boundary so
Flutter does not reparent an active OverlayPortal through a layout pass. The
affected menu is closed/recreated safely; this is not a claim of preserving its
overlay child State across structural changes. Ordinary scalar, style, selection
and diagnostic changes preserve the open native menu, focus and inline draft.

**Follower/Tooltip boundary:** in this SDK a descendant Tooltip's OverlayPortal
cannot compute its transform through the external LayerLink follower. When an
actual model/implicit Tooltip owner is present, isolated Canvas omits its surrogate
LayerLink and reports the approximation; the source reference is untouched.
Removing that Tooltip restores the surrogate branch. Diagnostic badges retain
accessible warning text but suppress only their own conflicting popup under a
follower. No private Flutter transform APIs are used.

A stable viewport-local Overlay keeps popup geometry inside the logical preview
viewport under Fit/zoom. It intentionally has no LookupBoundary: hiding Flutter's
private View ancestry breaks EditableText and IME/focus lookup.

**Explicit Canvas approximation:** root and nearest menu overlay placement both
use that isolated viewport overlay. A source Use root overlay=true is preserved
in FD and generated Dart, but the native isolated preview uses local placement
and reports this limitation. Source applications retain full native root-overlay
behavior. Nested Overlay host architecture is not silently redesigned by this
widget slice.

## Verification

Verified on **2026-09-09** in the canonical G: checkout:

- Full core: **2,172 tests / 261 suites**, no failures, errors or skips, including
  the installed-SDK callable audit and 12 new MenuAnchor core/session tests.
- Exact candidate SDK gate: **4 Java tests / 42 candidates**, including the
  23-node constructor matrix, dense local styles, all ten strict reference
  families, direct/member/factory access, explicit nulls, and 30 wrong/dynamic/
  nullable negative candidates. Baseline FD/source/package files remain unchanged.
- Generated builder/State SDK workflow: **1 Java test / 7 Flutter tests**, plus
  four accepted exact pair-save transitions. Covers native menu/controller/context,
  three Events, four live State consumers, animation statuses, outside-tap
  consumption, focused Escape, local style/theme fallback and inherited-size
  normalization. Edited user bodies survive reset, save/reopen and Undo/Redo.
- Full Canvas: **2,069 Flutter tests**, including **19 new MenuAnchor tests**;
  Flutter analyze reports no issues. Independent tests cover retained open menu,
  native focus/State, F2 draft/View ancestry and structural overlay cleanup.
- Final selected Maven verify: **1,507 plugin tests / 75 suites**, **40 analyzer
  tests / 2 suites**, and **9 Failsafe integration tests**; no failures/errors/skips.
  The 19 new UI/lifecycle cases include the final staged Reference-draft guard,
  cancellation and exact Offset local/null/reference round trips.
- Release Web build passed. All **40 source** and **35 Web** manifest entries
  match exact file sizes and SHA-256; source hashes stayed frozen during Web build.
  Main JS: **3,513,110 bytes**, SHA-256
  `22497976e0167d46c3289fd64c2c8c2f817c6f05617198f1b4508d8c9b621009`.
- Maven install and both root/module `nbm:cluster` runs passed. All **1,708
  non-manifest module entries**, all three dependency JARs and four new SVGs match
  the NBM and both G: dev clusters. The MenuAnchor schema, builder command and
  unchanged FD 16 schema are present. NBM rewriting of its module manifest is
  intentionally excluded from the module-entry hash comparison.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
**8,431,056 bytes**, modified **2026-09-09T10:12:53.4171677Z**, SHA-256:

`fa4fafde9b546fb7027bf5c9e188f885939745e18b8088f769ba95f5afb94bff`

No running IDE was restarted and no manual interactive-IDE smoke test is claimed.
Restart the development NetBeans instance to load the new module/runner bytes.
