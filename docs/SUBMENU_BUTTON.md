# SubmenuButton — native submenu, two styles, Events and State

Baseline: Flutter **3.44.8**, checked against installed `material/menu_anchor.dart`,
`material/menu_style.dart`, `material/button_style.dart` and `widgets/raw_menu_anchor.dart`.
References: [constructor](https://api.flutter.dev/flutter/material/SubmenuButton/SubmenuButton.html),
[submenuIcon](https://api.flutter.dev/flutter/material/SubmenuButton/submenuIcon.html),
[hoverOpenDelay](https://api.flutter.dev/flutter/material/SubmenuButton/hoverOpenDelay.html).

## Complete constructor and palette contract

Material `flutter.material.SubmenuButton` covers all **21 constructor arguments**:
Designer-owned Key, **16 direct properties**, and four slots. Child is a required
named argument of nullable type: an empty slot emits `child: null`. Menu children
is a required list which may be empty; an empty list disables the native button.
Leading icon and Trailing icon are optional single Widget slots. All slots accept
ordinary Widgets under the existing parent-data placement rules. This is an
ordinary palette Add, not a required-child wrapper. The constructor is const-capable.

The direct properties are On hover, On focus change, On open, On close, Controller,
Style, Menu style, Alignment offset, Clip behavior, Focus node, States controller,
Submenu icon, Use root overlay, Hover open delay, Animated and On animation status changed.
There is no invented Enabled, On pressed, Autofocus, Shortcut or Builder argument.
SubmenuButton provides its own native menu-opening button.

**721 editable properties** comprise 16 direct properties, **498 local ButtonStyle
fields**, **203 local MenuStyle fields**, and four local submenu-icon state buckets.
Each compound is independent: whole Style is exclusive only with `style...`
locals; whole Menu style is exclusive only with `menuStyle...` locals; whole
Submenu icon is exclusive only with the four local icon buckets. Explicit null
is a whole value for these exclusivity rules. Switching one compound does not
clear another compound, an Event, a State binding or a slot.

Nullable constructor arguments permit omission or explicit null. Clip behavior,
Use root overlay, Hover open delay and Animated permit omission but not null.
Hover open delay uses **signed integer microseconds** (`hoverOpenDelayUs`), or a
strict Duration reference/member/zero-argument factory. No millisecond rounding is
performed. Negative durations are valid and retain their exact source value;
Dart timers treat negative delay as zero. The pinned SDK asserts against a
**positive** hover delay under a horizontal MenuBar. Click activation is immediate;
delay applies to native hover/focus opening.

References retain strict non-null target proof even for nullable constructor
parameters: explicit null is a separate value. The families include MenuController,
FocusNode, WidgetStatesController, ButtonStyle, MenuStyle, Offset, Duration,
WidgetStateProperty<Widget?>, Widget, ButtonLayerBuilder and the native callbacks.
Wrong, dynamic and nullable targets cannot acquire pair-save authority merely
because ordinary Dart assignment would allow dynamic. Structured Offset remains
available alongside an exact typed Offset reference.

## Submenu icon states

`submenuIconDefault`, `submenuIconDisabled`, `submenuIconHovered` and
`submenuIconFocused` each accept an IconData value, an exact Widget reference/member/
zero-argument factory, or explicit null. The local resolver checks configured
states in priority **disabled → hovered → focused → default**. An omitted bucket
continues to the next applicable configured bucket. Explicit null is terminal and
requests native MenuTheme/default-arrow fallback. **IconData None generates
`Icon(null)`**, a real empty Icon widget which hides the glyph instead of requesting
fallback. Widget references provide arbitrary custom icon widgets, size, color and
styling; a whole WidgetStateProperty<Widget?> supports custom combined-state logic.

Native SubmenuButton decides whether to show this decoration from the parent menu
orientation. A standalone/horizontal opener may hide the arrow; a vertical parent
menu shows it. Native icon states are disabled, hovered and focused; the external
States controller is not the source of these state flags.

## Native style details

Button Style and Menu style remain separate native objects. Button fallback uses
MenuButtonTheme and `SubmenuButton.defaultStyleOf(context)`. Flutter's object-level
WidgetStateProperty merging and scalar null fall-through are preserved, including
possible TextButtonTheme fallback. MenuStyle normally resolves the panel against
the native empty state set. All local state buckets remain available in source.

SubmenuButton has an additional menu-padding prepass. It resolves the selected
padding property against `statesController?.value ?? {}` and requires a non-null
result. For structured local Menu style padding, unconfigured states therefore
resolve to the current MenuTheme padding or the pinned SDK default
`EdgeInsetsDirectional.symmetric(vertical: 8)`. This prevents a sparse Hovered-only
padding setting from crashing when the state is empty. An exact whole MenuStyle
reference is never rewritten: a type-valid reference with runtime-null padding can
still be unsuitable for this native constructor at mount time.

The external States controller participates in this padding prepass, but the pinned
SDK **does not forward it to the internal TextButton**. Designer does not invent
controlled hover/pressed behavior. Alignment offset is added to native padding
correction: a vertical menu subtracts top padding from y; a horizontal menu adjusts
x by left/right padding according to text direction.

Clip behavior controls the submenu panel. It is not forwarded to the internal
TextButton: ButtonStyle layer builders retain their own native effective clipping.
Use root overlay and Animated retain native behavior in generated application code.
Controller, Focus node and States controller references remain application-owned.
Dispose application-owned objects that expose disposal APIs; MenuController has none.

## Events, State and source ownership

Five Events are supported: On hover / On focus change (`ValueChanged<bool>`),
On open / On close (`VoidCallback`), and On animation status changed
(`ValueChanged<AnimationStatus>`). Creation, binding, rename, navigation and reset
use the existing typed source workflow. Reset removes a binding, not the user's
method body. User-owned code remains outside managed generated regions.

Use root overlay and Animated are two boolean State consumers. SubmenuButton is not
a controlled State producer: opening/closing is native menu/controller state.
Do not connect hover/focus/controller references as arbitrary writable State slots.
Save/reopen and Undo/Redo must preserve all four slots, independent style compounds,
Event references, literal boolean fallbacks and user-owned callback bodies.

## Bounded persistence

The default per-widget persisted-property and semantic-validation limit is **1024**
(previously 512), matching the existing catalog schema bound. This admits the dense
legal SubmenuButton without splitting or silently dropping its styles. Per-command
PatchProperties and State-binding limits remain **512**; an individual style
compound fits those unchanged command limits. File, tree, depth and byte limits
are unchanged, as are **2048 analyzer probes** and the **45-second analysis budget**.
FD 16, API 15, Canvas model 19 and transport 1 remain unchanged: no new value format.

## Verification status

Verified against the installed Flutter **3.44.8** SDK and the NetBeans module build:

- `SubmenuButtonCandidateRealSdkTest` and `SubmenuButtonStateRealSdkTest` pass. The
  nested activation assertion uses the SDK's deterministic non-animated path;
  Flutter 3.44.8 keeps the animated root overlay controller open in this nested
  fixture after activation. The separate animation-status test still covers
  `forward/completed/reverse/dismissed` transitions.
- Java property/editor/palette/state/mutation contracts pass: **326 tests** in the
  focused run (including 176 mutation-controller cases).
- Flutter Canvas runner: `flutter analyze` reports **No issues found**; the complete
  runner suite passes (**2085 tests**), including all SubmenuButton interaction,
  nested-menu focus, F2 inline editing, state and geometry cases.
- Web runner build succeeds with the pinned release command; the resulting artifact
  manifest is refreshed with the current sizes and SHA-256 digests.
- `WebCanvasArtifactContractTest` (24 tests), `CanvasRunnerSourceBundleTest`
  (9 tests), and packaged `PluginPackageMetadataIT` (9 tests) pass against
  `netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`.

Canvas intentionally keeps the SDK's native menu/controller behavior while the
designer preview remains bounded: root overlay and animated rendering are isolated
to the logical viewport, local reference/default icon buckets are resolved from the
designer model, empty menu children disable the native opener, and invalid inherited
menu geometry is quarantined to finite preview defaults without rewriting the model
or generated Dart. CJK IME and Linux/macOS Canvas providers remain outside this
palette slice and are deferred until the complete planned palette is finished.
