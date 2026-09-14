# DecoratedBoxTransition

Pinned contract: Flutter 3.44.8, `widgets/transitions.dart`.
[Constructor](https://api.flutter.dev/flutter/widgets/DecoratedBoxTransition/DecoratedBoxTransition.html)
and [class](https://api.flutter.dev/flutter/widgets/DecoratedBoxTransition-class.html).

## Complete native constructor

- Required `decoration: Animation<Decoration>`: local structured BoxDecoration
  or typed project reference/getter/zero-argument factory (unqualified/imported,
  top-level/instance/static member).
- Optional `position`: background or foreground; omission uses background.
- Required single box `child`: atomic wrapping or replacement, including root.
  Never an empty insertion target. Sliver/Flex/Stack parent-data children are
  rejected. Shared Key/identity handling is unchanged.

Local BoxDecoration includes colors and theme roles, images (provider/resize,
color filters, onError, fit/alignment/centerSlice/repeat, RTL, scale/opacity,
filter quality, inversion and anti-aliasing), borders, physical/directional
radii, shadows, linear/radial/sweep gradients, blend modes and rectangle/circle.
Native invalid combinations remain rejected by the shared typed validator.

Local decoration is emitted as `AlwaysStoppedAnimation<Decoration>`; nested
theme/callback expressions propagate non-const status and retain analyzer
symbol offsets. Full FD encoding, commands, undo/redo and save/reopen are used.
The required editor has only local/project modes: no null or omit. Position
supports Restore Default. Dialog changes remain drafts until commit, and row
identity survives presentation refresh.

## Project animation and preview limits

ShapeDecoration and custom Decoration subclasses are supported through project
`Animation<Decoration>`; they are not additional local decoration editors.
Animation<BoxDecoration> and Animation<ShapeDecoration> covariance is verified
against the pinned SDK. Controllers/tweens/listener ownership remain in Dart;
this slice does not add controller creation UI or arbitrary code execution.

Canvas constructs the native DecoratedBoxTransition with a stopped local value.
For project sources it displays an explicit diagnostic and empty BoxDecoration,
preserving its child. Generated application Dart retains the real animation.
Local/source mode changes do not retain two independent FD values: decoration
is a single union value. Dialog drafts are preserved while switching modes.

Decoration affects painting and native shape hit testing, not child layout:
it adds no padding, size constraints or clipping. Foreground paints over Child.
No invented duration, curve, onEnd, alignment or clipBehavior rows were added.
The widget has no Events. CJK IME and Linux/macOS providers remain deferred.

## Inventory and verification

204 definitions, 7,275 writable rows (7,257 outside Scaffold), 196 typed and
170 const-capable definitions. Layout has 64 entries. Optional-slot matrix:
36,924 candidates, 24,555 accepted, 12,369 rejected; 42 required wrappers,
30 root-compatible wrappers. Events/callables/builders and wire versions are
unchanged.

The new focused Canvas suite passes 20 cases: exact required domains, root and
occupied-child wrapping, invalid child kinds, 12 direction/position/constraint
combinations, all three gradient families, source/local state retention,
zero-size children and complete image placeholder settings.

The dedicated pinned-SDK test passes 46 generated/native runtime cases:
38 saved/reopened cases in both directions; complete nested image/theme/callback
conversion; live Box/Shape/custom decoration changes and listener detachment;
two real DecorationTween forward/reverse cases; four pixel/layout/hit-test cases.
All eight source forms, covariant Box/Shape animations and invalid nullable,
wrong-type, non-animation and dynamic sources are checked by actual analyzer
evidence. The source/local reset test reopens the saved source before editing,
matching the real user workflow.

Full core and broad plugin contracts pass. The entire Canvas suite passes
10,230 cases; Flutter analyze reports no issues. Four SVG variants were rendered
and visually checked.

Fresh Surefire reports cover 3,915 Java tests across 583 reports: zero failures,
zero errors, 102 skipped. The dedicated SDK test ran without being skipped.
This is full core plus focused/broad plugin, actual SDK and package contracts,
not every plugin integration test.

The Web release build and artifact contracts pass. main.dart.js is 3,875,132
bytes; SHA-256 `dd75ded2b283b0d151c8560ee8b1a6158432c04063d302d10cc4bcadaf79e5a9`.
The 40-file source and 35-file Web manifests were regenerated from actual bytes.
Both Maven install (without cleaning or resetting the checkout) and nbm:cluster
passed. Reopening the NBM verified current schema, catalog/generator, editor and
evidence classes, four icon variants, 40 runner sources and 35 Web files.
All four development-cluster JARs match the NBM byte-for-byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`
in `G:/MyProjects/java/project/netbeans-flutter-plugin-starter`.
9,019,676 bytes, modified UTC 2026-09-14T15:04:37.8117956Z.
SHA-256: `16686251c7347e475a75fabf5e850c34f28b506cfaa1577ddc029a21dcb8a98e`.

Whitespace checks passed. The IDE was not restarted; an interactive desktop
smoke test is not claimed. Next planned widget: AlignTransition.
