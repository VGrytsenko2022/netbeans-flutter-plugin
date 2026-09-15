# AlertDialog: standard and adaptive

Pinned to Flutter 3.44.8 and audited against the installed SDK's
`material/dialog.dart` and official
[standard](https://api.flutter.dev/flutter/material/AlertDialog/AlertDialog.html) /
[adaptive](https://api.flutter.dev/flutter/material/AlertDialog/AlertDialog.adaptive.html)
constructor contracts.

## Inventory and native arguments

Standard: 28 constructor arguments = 24 properties + four widget slots.
Adaptive: 32 arguments = 28 properties + the same four slots.
The designer exposes 107 / 111 writable rows including 21 local shape rows and
31 local rows for each of Title text style and Content text style. Both are
const-capable when their actual values and descendants are const.

| Native argument(s) | Designer representation |
| --- | --- |
| key | String ValueKey, verified Key?, null, omission |
| icon, title, content | Independent optional single-widget slots; Icon accepts any box Widget, not only Icon |
| actions | Ordered optional list of box Widgets; add/move/remove normally |
| iconPadding, titlePadding, contentPadding, actionsPadding, buttonPadding | Non-negative physical/directional EdgeInsetsGeometry, verified EdgeInsetsGeometry?, null, omission |
| iconColor, backgroundColor, shadowColor, surfaceTintColor | Literal/theme Color, verified Color?, null, omission |
| titleTextStyle, contentTextStyle | Verified TextStyle?, null, omission, or complete local style family |
| actionsAlignment | All six MainAxisAlignment values, null, omission |
| actionsOverflowAlignment | start/end/center, null, omission |
| actionsOverflowDirection | up/down, null, omission |
| actionsOverflowButtonSpacing | Signed finite number, verified double?, null, omission; native OverflowBar allows negative spacing |
| elevation | Non-negative number, verified double?, null, omission |
| semanticLabel | String, verified String?, null, omission |
| insetPadding | Physical non-negative EdgeInsets only; standard allows nullable source/null, adaptive public factory requires non-null explicit EdgeInsets |
| clipBehavior | All four Clip values, null, omission |
| shape | Verified ShapeBorder?, null, omission or one of ten complete local shapes |
| alignment | Physical/directional AlignmentGeometry, verified AlignmentGeometry?, null, omission |
| constraints | BoxConstraints, verified BoxConstraints?, null, omission |
| scrollable | Non-null boolean; omission preserves false |
| scrollController, actionScrollController | Adaptive only: verified ScrollController?, null, omission |
| insetAnimationDuration | Adaptive only: non-negative microseconds row, verified non-null Duration; default 100ms |
| insetAnimationCurve | Adaptive only: all 43 Curves presets or verified non-null Curve; default decelerate |

No default properties or placeholder children are serialized at creation.
All optional slots can be empty. Omitted values preserve native theme/default
resolution, including adaptive's omitted inset padding and custom DialogTheme.
The adaptive public factory declares a non-null explicit insetPadding even
though its redirect target can inherit an omitted nullable value.

## Complete compound properties

Both text-style families reuse all 31 reviewed local fields: theme base, inherit,
foreground/background colors or paints, font metrics/weight/style, spacing,
baseline/height/distribution, locale components, decorations, shadows, font
features/variations, debug label, font family/fallback/package and overflow.
A whole style (including explicit null) is exclusive with local leaves; switching
representations is atomic. Color/Foreground and BackgroundColor/Background are
mutually exclusive; package needs a font family or fallback.

Shape supports roundedRectangle, beveledRectangle, continuousRectangle,
roundedSuperellipse, circle, oval, stadium, linear, star and polygon with all
applicable BorderSide/radius/linear/star leaves. Switching kind removes only
incompatible leaves; whole/null shape clears local shape fields. No custom
ShapeBorder source is evaluated inside Canvas.

## Material versus Cupertino

Adaptive follows ThemeData.platform (including the Canvas preview profile):
iOS/macOS use CupertinoAlertDialog; Android/Fuchsia/Linux/Windows use Material.
This is native platform adaptation, not a new Linux/macOS Canvas provider.

Cupertino uses Title, Content, Actions, scrollController, actionScrollController,
insetAnimationDuration and insetAnimationCurve; it ignores Icon and Material
appearance/layout arguments. Material ignores those two controllers and the
adaptive inset-animation arguments. All stored values remain editable and are
emitted unchanged; changing platform does not erase ignored values.
The existing Canvas profile supports Android/iOS/Linux/macOS/Windows; Fuchsia
behavior is covered through generated native SDK tests.

Material scrollable wraps title/content while retaining visible actions.
Unbounded lazy content still needs explicit intrinsic constraints; no automatic
SizedBox or invented size is inserted into the model.

Neither constructor adds Events or controlled State bindings. Child actions own
callbacks; the application calls showDialog/showAdaptiveDialog and awaits the
Navigator result. Barrier behavior, dismissal, restoration of routes and focus
traversal belong to that route. Supplied controllers are owned/disposed by the
application. Key changes reset native subtree identity; ordinary property edits
preserve it.

## Isolated preview boundaries

Canvas builds the real native AlertDialog constructor, preserves child editing
and advertises the active platform split. Project references/getters/factories
are not executed: unresolved appearance values use native defaults/theme;
unresolved controllers use native owned scrolling; animation uses native
100ms/decelerate. Typed source validation proves static types, not runtime values
or application assertions.

A Material custom ShapeBorder or shape above the isolated star-point budget has
an explicit unavailable preview. Cupertino ignores Material shape, so this does
not block its native preview. No modal route or application callback is executed
by the preview surface.

## Verification

Release gates completed on 2026-09-15 with Flutter 3.44.8:

- 82 dedicated Canvas tests and the full 12,032-test Canvas suite passed;
  Flutter analyze reported no issues.
- Real SDK: 312 generated native rendering cases across six platforms and both
  text directions passed; 87 unsafe/incompatible source cases were rejected.
- Java verification: full reactor followed by a focused rerun after correcting
  historical palette-snapshot filters; final reports cover 6,300 tests,
  6,025 passed, 275 conditionally skipped, zero outstanding failures/errors.
  All 129 palette drop-planner tests passed with their original historical
  counts preserved. Each of the 62 local style leaves was exercised through
  its typed property editor for both constructors.
- Final install and all nine package-metadata integration tests passed.
  NBM checks confirmed current classes, 64 icon variants, 41 runner sources,
  35 Web artifact files, and byte-identical development-cluster JARs.
- NBM SHA-256:
  `e3fd0b023328b5bb8aa6f80bc0806c84f2285bc13ab131318c27674c81a1ce00`.

Existing NetBeans shutdown-hook/fork-exit warnings were not treated as test
results; final outcomes above come from exit status and fresh test reports.
Manual desktop testing in the installed IDE was not performed. No installed
IDE, userdir or user sample application was modified.
