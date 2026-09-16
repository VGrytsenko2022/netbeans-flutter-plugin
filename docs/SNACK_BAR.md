# SnackBar and SnackBarAction

Pinned API: Flutter 3.44.8
([SnackBar](https://api.flutter.dev/flutter/material/SnackBar/SnackBar.html),
[SnackBarAction](https://api.flutter.dev/flutter/material/SnackBarAction/SnackBarAction.html)).
All 20 and seven constructor arguments are covered by two Material palette
entries. SnackBar has 39 property rows (18 direct plus 21 local shape leaves)
and two slots; SnackBarAction has seven property rows and no slots.

## Content, layout and appearance

- Required Content accepts a box widget, never a sliver. Creation inserts an
  explicit Text('Message') model child with a deterministic unique ID. Replace
  it through Slots; removing the final required child is rejected atomically.
- Optional Action accepts only SnackBarAction, not arbitrary buttons.
- Key, literal/theme/source colors, nullable elevation, physical/directional
  padding and margin, width, all native enums and all ten shared local
  ShapeBorder families are supported. Local shape leaves and whole ShapeBorder
  sources are mutually exclusive; switches are one history operation.
- Width and margin are exclusive when non-null. Both require explicit floating
  behavior in Designer because inherited project themes cannot be proven.
  Editing either selects floating and clears the opposite field atomically.
  Choosing/resetting behavior away from floating clears width and margin.
  This conservative admission rule is stricter than Flutter's theme fallback.
- Width must be positive and finite; local padding/margin and elevation must
  be nonnegative. Action overflow threshold accepts 0..1. Project sources must
  satisfy the corresponding runtime requirements, not just the static type.
  Opening an omitted/null/source width keeps its original state; the inactive
  local-number draft starts at a valid positive value and commits only on OK.
- Pinned Flutter resolves directional margin using LTR internally; directional
  content padding follows Directionality. Native action text measurement and
  overflow layout are used, including Material 2/3 and RTL.
- SnackBarAction's Color? sources can return WidgetStateColor. Native Flutter
  forbids disabledBackgroundColor with a stateful backgroundColor; this is a
  runtime source-value constraint and remains enforced by Flutter. Designer
  does not execute the getter to discover its concrete runtime subtype.

## Presentation, timing and events

SnackBar is a widget description, not a Scaffold slot. The application owns
ScaffoldMessenger.of(context).showSnackBar, queuing, removal and the returned
controller's closed Future. This slice adds no implicit messenger calls,
controller fields, routes, arbitrary source editor or invented onClosed Event.

Creation uses local animation 1, generated as AlwaysStoppedAnimation<double>,
so a direct palette insertion mounts safely. Local 0..1, Animation<double>?
references/getters/factories and explicit null/omission are supported.
ScaffoldMessenger replaces the supplied animation on presentation. Direct
mounting with null/omitted animation is invalid in Flutter; choose the local
preset for a directly mounted tree or let the messenger supply it.
Duration uses exact signed portable microseconds or a verified non-null
Duration. Omission preserves four seconds. Negative timer durations behave
as zero; project values are not evaluated or silently rewritten.

Nullable persist defaults to whether Action is supplied. True disables timeout;
false permits it. Accessibility and messenger state also affect timeouts.
Nullable showCloseIcon and closeIconColor retain theme/native fallback.

- onVisible: optional VoidCallback?, called on first full visibility.
- onPressed: required VoidCallback on Action, initially a no-action closure.
  Flutter calls the handler once and then hides the current bar with action
  reason. Reset/disconnect restores that closure; it does not disable the action.
- Events use create/select/open/rename/disconnect with source preserved through
  history and save/reopen. Optional callbacks disconnect by omission; existing
  methods remain in the user-owned source region.

## Isolated Canvas boundary

Canvas renders real native SnackBar/SnackBarAction and local Content. It never
executes project getters, factories, animations or handlers. Animation stays
at 1 even when the model requests another value. Pointer activation, focus
activation and dismissal are suppressed so edits do not remove the design.
No timeouts/queues are started by Canvas.

Custom shape previews are explicitly unavailable. Other source-owned
appearance uses native defaults; source labels use a visible unavailable
placeholder. Diagnostics identify those fields, including nested Action.
Root literal keys are retained; nested Action uses a private geometry key for
selection instead of its user key. The generated Dart and saved key are intact.

FD 17, Catalog API 16, Canvas model 20 and transport 1 are unchanged.
The catalog now contains 247 definitions, 8,260 rows, 203 const definitions,
238 typed definitions, 731 Boolean-only and 57 nullable-Boolean rows.
The insertable-slot matrix has 54,340 cells: 35,147 accepted, 19,193 rejected.

## Verification

Pinned SDK: 413 generated/native scenarios (Material 2/3, six platform themes,
LTR/RTL, all local shapes, nullable resets, getters/factories, events and
native messenger lifecycle); 40 unsafe or incorrectly typed source cases
rejected. These platform themes do not add native Linux/macOS Canvas providers.

Automated verification on 2026-09-16:

- Full Java regression followed by focused reruns of corrected inventories,
  insertion rules, property editors and packaged-resource gates: current fresh
  reports contain 6,332 tests, 279 environment/opt-in skips, zero failures/errors.
  All numeric-reference properties are checked for valid inactive local drafts.
- Canvas: all 12,128 tests passed, including 32 SnackBar/Action tests;
  `flutter analyze --no-pub` reported no issues; release Web Canvas rebuilt.
- `mvn install` and `nbm:cluster` succeeded; all nine package metadata checks
  passed. Packaged class bytes, 84 recent SVG variants, 41 runner source entries,
  35 Web files and all four development-cluster JARs were verified.
- NBM: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
  9,422,421 bytes, SHA-256
  `3cea6a1d7c2c6bfb6c202df11da661c4f2b75981a8b75cb73222e2e8a04b071d`.

Manual validation in the user's running IDE has not been performed.
