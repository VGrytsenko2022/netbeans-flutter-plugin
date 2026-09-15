# Dialog and Dialog.fullscreen

Pinned API: Flutter 3.44.8. Material palette orders 680 and 690.
[Dialog constructor](https://api.flutter.dev/flutter/material/Dialog/Dialog.html);
[fullscreen constructor](https://api.flutter.dev/flutter/material/Dialog/Dialog.fullscreen.html).

All 14 ordinary constructor arguments and all six fullscreen arguments are
implemented. Both are const-capable. Standard Dialog exposes 34 editable rows
(13 native properties plus 21 local ShapeBorder details); fullscreen exposes
five native properties. Each has one optional Child slot.

## Constructor coverage

| Argument | Standard | Fullscreen | Designer representation |
| --- | --- | --- | --- |
| key | yes | yes | String ValueKey, verified Key?, null, omission |
| backgroundColor | yes | yes | Literal/theme color, verified Color?, null, omission |
| elevation | yes | no | Non-negative number, verified double?, null, omission |
| shadowColor, surfaceTintColor | yes | no | Literal/theme color, verified Color?, null, omission |
| insetAnimationDuration | yes | yes | Non-negative portable integer microseconds or verified Duration |
| insetAnimationCurve | yes | yes | All 43 Curves presets or verified Curve |
| insetPadding | yes | no | Non-negative physical EdgeInsets, verified EdgeInsets?, null, omission |
| clipBehavior | yes | no | All four Clip values, null, omission |
| shape | yes | no | Ten local shapes or verified ShapeBorder?, null, omission |
| alignment | yes | no | Physical/directional alignment, verified AlignmentGeometry?, null, omission |
| semanticsRole | yes | yes | All 33 native dart:ui SemanticsRole values |
| constraints | yes | no | Structured BoxConstraints, verified BoxConstraints?, null, omission |
| child | yes | yes | Optional single box-widget slot |

The duration row is stored as insetAnimationDurationUs and generated using
insetAnimationDuration: Duration(microseconds: ...). Native omission preserves
100ms for ordinary Dialog and zero for fullscreen; the native curve is decelerate.
Negative duration/elevation, directional insetPadding and null non-nullable
arguments reject the whole edit, without changing FD or Dart.

Local ShapeBorder families: RoundedRectangleBorder, BeveledRectangleBorder,
ContinuousRectangleBorder, RoundedSuperellipseBorder, CircleBorder, OvalBorder,
StadiumBorder, LinearBorder, StarBorder and StarBorder.polygon. Every applicable
BorderSide, physical/directional radius, linear edge and star/polygon field is
exposed. Local/whole-shape edits are atomic: selecting a family clears only
incompatible details; a whole source/null clears local fields; reset restores
native inheritance. Point/valley rounding constraints are validated together.

Sources support current/imported references, getters, static members and
zero-argument factories. Strict analyzer proof checks exact static type,
symbol span, provenance, invocation and nullability. EdgeInsets? is deliberately
narrower than EdgeInsetsGeometry?: directional or general geometry sources,
Object and dynamic are not silently accepted. Type proof does not execute a getter:
project values must still satisfy Flutter runtime assertions, including non-negative
insets, duration and elevation. Sources are never copied into
the Canvas payload; only opaque presence is sent.

## Native layout, routes and accessibility

Dialog is a StatelessWidget; ordinary property updates are not initial-only
state. Omission/null retains DialogTheme and Material 2/3 defaults. Default
constraints are minWidth 280, not an invented maxWidth 560. Physical insetPadding
is added to MediaQuery.viewInsets and AnimatedPadding animates changes;
viewInsets are removed from the MediaQuery seen by the child.

Dialog.fullscreen fixes zero elevation/insets, Clip.none, no shadow/surface tint,
shape/alignment/constraints. These fields are not editable on that constructor.
The constructor alone does not create a fullscreen route or a modal barrier.
Use showDialog/DialogRoute in application code; route dismissal, restoration,
focus policy and result are not constructor parameters.

Dialog itself has no Events or controlled State bindings. Child buttons retain
their usual Events; a handler can call Navigator.pop(context, result). No
onConfirm/onCancel/onChanged properties are invented. Existing callable totals
remain 266 across 99 types (196 Events across 70); State remains 178 consumers
across 60 types.

SemanticsRole is preserved exactly in generated Dart. Roles such as row, cell,
tabBar and menuItem require an appropriate accessibility hierarchy; some
Flutter 3.44.8 debug checks also reject roles without implemented validators.
Choosing an enum value does not create that supporting hierarchy.

## Isolated Canvas boundary

Canvas renders the real native Dialog constructors and local shapes. Local
ValueKey changes recreate the native subtree; ordinary property changes preserve
its identity. A zero-sized Dialog retains an editor-only selection/drop marker;
no artificial width, height or child is added to generated Dart. Project
sources are not executed: the preview discloses affected fields and uses native
defaults/theme for source-backed colors, insets, alignment, constraints and
elevation; source animation uses 100ms/zero and decelerate. A source-backed
ShapeBorder or a star above the existing isolated point budget produces an
explicit unavailable shape preview, not a fabricated border.

For non-dialog roles (anything other than none/dialog/alertDialog), Canvas
excludes only the semantic subtree and discloses the reason, preserving native
Dialog.semanticsRole, visual rendering, children and editing. Run/Debug keeps the
requested semantic tree and Flutter's native validation. Preview does not create
a modal route or execute child application handlers.

## Verification

Dedicated tests cover both schemas, requiredness and optional slots, all curves
and roles, ten shape families with every applicable leaf, all 100 shape-kind
transitions, nullable sources, native themes, keyboard insets/animation, property
updates, route result ownership, rejection atomicity, Undo/Redo and reopen.
Light/dark 16/32 SVG icon families were visually reviewed.

Release gates completed on 2026-09-15 with Flutter 3.44.8:

- Full Java reactor: 6,290 tests, 6,015 passed, 275 conditionally skipped,
  zero failures or errors. Existing NetBeans shutdown-hook/fork-exit warnings
  remain; Maven completed successfully.
- Dedicated Dialog Canvas tests: 182 passed; final full Canvas suite: 11,950
  passed. Flutter analyze reported no issues.
- Real-SDK generated-code gate: 48 native rendering cases passed; 36 unsafe or
  incompatible source cases were rejected. All 33 SemanticsRole values passed
  static analysis; roles requiring supporting accessibility trees are not
  mounted as isolated roots.
- Final install and all nine package metadata integration tests passed. The NBM
  contains the current schema/classes, 56 checked icon variants, all 41 runner
  sources and 35 Web artifact files. All four development-cluster JARs match it.
- NBM SHA-256:
  `0f56c9628126986e5b6ff4bea50a8651551f2b9fdeb398aaf1738fa2ee3da5d2`.

Manual desktop testing in the installed IDE was not performed. No installed
IDE, userdir or sample application was modified.
