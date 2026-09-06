# flutter-designer

NetBeans-independent domain layer for the Matisse-like Flutter Designer.

The NetBeans integration binds each Designer form through mirrored project
paths:

```text
lib/<relative>/<name>.dart
.fd_templates/<relative>/<name>.fd
```

Schema v1 keeps `source.dartFile` as the exact Dart basename; the NetBeans
adapter, not this domain module, enforces the two roots and matching relative
path. Its `Flutter Designer Form` New File wizard accepts only `lib` or its
subfolders and creates both entries as one complete pair.

This module owns the immutable `.fd` document model, the built-in widget
metadata catalog, semantic validation, the bounded deterministic JSON codec,
deterministic stateless Dart-region generation, and read-only Dart
marker/class/hash plus three-way candidate verification. It also owns the pure
prospective managed-source transition planner and canonical prepared-pair
planner used before any live editor or filesystem operation, plus the bounded
pair-rename planner that canonically changes only `source.dartFile` while
preserving `source.className` and every other document field. The bounded
pair-copy planner assigns a caller-supplied fresh `documentId`, retargets only
`source.dartFile`, and proves canonical exact round-trip parity while preserving
all other semantics. The pure bounded Dart directive scanner and pair-Move
dependency planner reject path-sensitive bindings before the NetBeans adapter
may move exact pair bytes between mirrored directories. It now also owns
the bounded pure Add/Remove/Move/Wrap/Set/Reset command session, immutable
revision candidates, exact inverse history, saved cursor, branch semantics and
paired versus `.fd`-only persistence classification, plus Canvas identities,
responsive render profiles and the bounded canonical seventy-two-widget Canvas
model projection. It deliberately has no dependency on NetBeans APIs
or Swing.

The NetBeans adapter lives in `netbeans-plugin`. On Windows it now exposes the
Design/status surface, Explorer widget tree, exact viewport/adaptive-
target preview toolbar and a real embedded native `FlutterView`, together with the
transactional pair-save edge. Stable widget IDs synchronize selection between
the tree and Flutter surface. The standard Properties window now exposes a
bounded catalog-backed presentation for all seventy-two canonical widgets: `Scaffold`,
`ElevatedButton`, `AppBar`, `TextField`, `Column`, `Row`, `Wrap`, `Padding`, `Center`,
`Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`,
`ListView`, `GridView.count`, `SingleChildScrollView`, `Container`, `Opacity`, `Text`, `Icon`, `Image`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge` and `CircleAvatar`. Sixty-six
definitions use reviewed const constructors. IntrinsicHeight, RepaintBoundary and MergeSemantics
have no scalar constructor properties; identity and child-slot editing remain
available. The exact catalog currently contains 890 writable property
rows. The historical first mutating Palette vertical slice admitted
only a terminal `Text` append. It is superseded by the current catalog-driven
4,392-cell candidate matrix: seventy-two exact capability-reviewed sources target
fifty-nine insertable any-widget and two `PreferredSizeWidget` destinations,
with exactly 4,079 accepted and 313 rejected cells, subject to
empty-single or terminal-list admission. Existing-widget
reparenting and list reordering use the same catalog compatibility planner and
transactional command path; catalog-incompatible and non-reviewed operations remain
disabled.

Expanded and Flexible are wrapper-source exceptions: Palette creation wraps an
existing direct Row/Column child instead of inserting a terminal prototype.
Neither wrapper may wrap Expanded, Flexible or Spacer, because the inner
ParentDataWidget would cease to be a direct Row/Column child. Their occupied
required `child` slots are replacement-only and are not insertable matrix
destinations. Spacer is the insertion-only source exception: it is appended as
a childless leaf only to direct `Row.children` or `Column.children` and never
wraps an existing child. SafeArea is the generic required-child wrapper: its
Palette/tree route atomically wraps an existing root or non-root widget, while
the current Canvas target wire intentionally exposes only non-root children and
offers no root target. It never creates an empty required child and cannot wrap
Expanded, Flexible or Spacer because their ParentData must remain attached
directly to Row or Column. Image creation selects the first declared asset when
available and otherwise stores an editable unresolved provider before ID
allocation. TextField remains an
ordinary leaf source because its generated and Canvas constraint guards handle
unbounded flex layouts without a placement rule.

The Properties `Slots` tab also uses catalog-authorized atomic commands. An
occupied single slot is replaced as one fenced edit, rather than as a visible
remove-then-add sequence. Optional slots can be cleared, and list slots expose
`Clear All`; each operation validates the exact source revision and expected
ordered children and is one Undo/Redo unit.

ADR-021 explicitly forbids implementing
the Canvas as Swing-painted widgets or a transferred PNG/JPEG/raw-pixel surface.
A planned platform SPI will generalize the Windows host to Linux and macOS; the
current provider keeps the Flutter engine/view in an isolated child runner.

This module owns only the NetBeans-independent host-issued presentation
identities and stale/replay admission rules, the explicit Mobile, Tablet,
Desktop and Web preview-mode identity, native-surface request validation, pure
backend contracts and per-MultiView lifecycle controller, plus the bounded
version 1 lifecycle control codec/session gate, process framing and canonical
reviewed Canvas model payload. Current model payload version 18 carries the exact resolved
project-theme identity, seed, brightness and typed ColorScheme/TextTheme
override tables; generated Dart and Flutter Canvas apply those tables in the
same order before form-local overrides. Version 18 adds the closed structured
ShapeBorderClipper value with six reviewed shapes, radius and explicit direction.
Version 17 added presence-only transport
for the typed project Dart-object references used by ClipRect, ClipOval and
ClipRRect; version 16 added ClipRRect's top-level
physical/directional finite non-negative elliptical border-radius value;
version 15 added the exact payload-free null used by `IndexedStack.index`. It
also carries Container's structured
`AlignmentGeometry`, `BoxConstraints`, `Matrix4`, complete `BoxDecoration` and
asset-only `DecorationImage` unions, including finite or positive-infinity
values for every constraint bound. Version 14 adds the atomic finite signed
`Offset` value used by Transform; version 13 added the atomic finite
non-negative `Size` value used by SizedOverflowBox, and version 12 added positive
infinity to the constraint graph. Version 10 introduced the initial
Container projection before v11 completed its image branch; version 9
introduced the resolved project-theme tables. Version 8 retains the closed typed
`IconData` and AppBar projections, adds ElevatedButton's sparse state contract
and carries callback presence without callback identifiers.
The payload admits exactly `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`,
`Column`, `Row`, `Wrap`, `Padding`, `Center`, `Align`, `FractionallySizedBox`,
`FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `SizedBox`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`,
`ListView`, `GridView.count`, `SingleChildScrollView`, `Container`, `Opacity`, `Text`, `Icon`, `Image`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge` and `CircleAvatar` from the reviewed
built-in catalog and excludes project paths, Dart source, callback identifiers,
extensions and persistence authority. The frame-kind whitelist is control JSON,
model JSON, negotiated NBFC kind 4 `IMAGE_BYTES`, and reserved catalog JSON.
Under `asset.imageBytes.v1`, one revision-scoped model descriptor is followed by
the exact ordered immutable compressed resource frames; each descriptor/payload
size and lowercase raw SHA-256 must match. This is referenced asset transport,
not a screenshot or framebuffer-pixel Canvas proxy. This module does not own
Swing, native window handles, a platform host implementation, concrete
processes, SDK discovery or persistence.

The NetBeans edge now has the first real Windows read-only Canvas slice: a
heavyweight AWT host inside the Design MultiView, verified
runner/`FLUTTERVIEW` HWND chain, bounded SDK-keyed runner build cache and
per-view isolated process lifecycle. Each open `.fd` Design view owns its host
and process independently; cache reuse is accepted only after a bounded runtime
SHA-256 manifest matches all launch artifacts. Resize/peer-loss and late
build/launch/exit races are fenced and covered together with simultaneous-view
tests. The isolated runner decodes the canonical seventy-two-widget model, renders it
directly in Flutter for the compatible native adaptive targets, acknowledges the
exact layout identity and exchanges only revision-bound stable-ID selection.
Android/iOS/macOS/Linux appearance uses `ThemeData.platform` while the physical
host remains Windows. Web uses its exact browser-sized responsive viewport on
that native engine as a layout preview. It does not claim `kIsWeb`, DOM,
browser fonts, plugins or platform-channel behavior; those require an optional
future browser-compiled backend.
It receives no project paths, Dart source, file handles or
file/Save/Undo/Redo authority; Java remains the only command-admission and
persistence owner. The Java → Flutter hit-test → revision-bound DnD bridge is
implemented for the exact catalog matrix and remains fail closed outside it.

### Current typed Properties slice

Property rows and editors are derived from the same immutable widget catalog
used for validation and Dart generation. One accepted edit emits exactly one
revision-bound `SetProperty` command; NetBeans' native **Restore Default** emits
`ResetProperty` for optional constructor arguments. The handler is one-shot and
the resulting candidate follows the existing analyzed pair-save/Undo lifecycle.
Required arguments, including `Text.data` and `Padding.padding`, cannot be
reset to omission.

| Widget | Writable properties | Flutter API |
| --- | --- | --- |
| `Column` | `mainAxisAlignment`, `mainAxisSize`, `crossAxisAlignment`, `textDirection`, `verticalDirection`, `textBaseline`, `spacing` | [Column](https://api.flutter.dev/flutter/widgets/Column/Column.html) |
| `Row` | `mainAxisAlignment`, `mainAxisSize`, `crossAxisAlignment`, `textDirection`, `verticalDirection`, `textBaseline`, `spacing` | [Row](https://api.flutter.dev/flutter/widgets/Row/Row.html) |
| `Padding` | `padding` | [Padding](https://api.flutter.dev/flutter/widgets/Padding/Padding.html) |
| `Center` | `widthFactor`, `heightFactor` | [Center](https://api.flutter.dev/flutter/widgets/Center/Center.html) |
| `SizedBox` | `width`, `height` | [SizedBox](https://api.flutter.dev/flutter/widgets/SizedBox/SizedBox.html) |
| `ColoredBox` | required literal/reviewed-theme `color`, optional `isAntiAlias`; optional single `child` slot | [ColoredBox](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html) |
| `SafeArea` | optional `left`, `top`, `right`, `bottom`, signed finite physical `minimum`, `maintainBottomViewPadding`; required single `child` slot | [SafeArea](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html) |
| `Placeholder` | optional literal/reviewed-theme `color`, finite non-negative `strokeWidth`, `fallbackWidth`, `fallbackHeight`; optional single `child` slot | [Placeholder](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html) |
| `Directionality` | required `textDirection` (`ltr` or `rtl`); required atomic single `child` wrapper slot | [Directionality](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html) |
| `DecoratedBox` | required complete typed `BoxDecoration`, optional `background`/`foreground` position; optional single `child` slot | [DecoratedBox](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html) |
| `ExcludeSemantics` | optional `excluding` with omitted default `true`; optional single `child` slot | [ExcludeSemantics](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html) |
| `Text` | 59 typed leaves in the seven sets below | [Text](https://api.flutter.dev/flutter/widgets/Text/Text.html), [TextStyle](https://api.flutter.dev/flutter/painting/TextStyle/TextStyle.html), [StrutStyle](https://api.flutter.dev/flutter/painting/StrutStyle/StrutStyle.html) |
| `Icon` | `icon`, `size`, `fill`, `weight`, `grade`, `opticalSize`, `color`, `shadows`, `semanticLabel`, `textDirection`, `applyTextScaling`, `blendMode`, `fontWeight` | [Icon](https://api.flutter.dev/flutter/widgets/Icon/Icon.html), [IconData](https://api.flutter.dev/flutter/widgets/IconData-class.html) |
| `AppBar` | 120 typed leaves in the nine sets below; `leading`, `title`, `actions`, `flexibleSpace`, `bottom` slots | [AppBar](https://api.flutter.dev/flutter/material/AppBar/AppBar.html), [AppBarTheme](https://api.flutter.dev/flutter/material/AppBarTheme-class.html) |
| `ElevatedButton` | 286 typed leaves in the state-aware sets below; optional-single `child` slot | [ElevatedButton](https://api.flutter.dev/flutter/material/ElevatedButton/ElevatedButton.html), [ButtonStyle](https://api.flutter.dev/flutter/material/ButtonStyle-class.html) |
| `ListView` | 17 typed static-constructor leaves; ordered `children` list slot | [ListView](https://api.flutter.dev/flutter/widgets/ListView/ListView.html) |
| `GridView.count` | 21 typed static-grid leaves; ordered `children` list slot | [GridView.count](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html) |
| `Wrap` | 9 typed run-layout leaves; ordered `children` list slot | [Wrap](https://api.flutter.dev/flutter/widgets/Wrap/Wrap.html) |
| `FittedBox` | `fit`, `alignment`, `clipBehavior`; optional single `child` slot | [FittedBox](https://api.flutter.dev/flutter/widgets/FittedBox/FittedBox.html) |
| `ConstrainedBox` | required finite/unbounded/expanding `constraints`; optional single `child` slot | [ConstrainedBox](https://api.flutter.dev/flutter/widgets/ConstrainedBox/ConstrainedBox.html) |
| `UnconstrainedBox` | `textDirection`, `alignment`, `constrainedAxis`, `clipBehavior`; optional single `child` slot | [UnconstrainedBox](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html) |
| `LimitedBox` | optional finite non-negative `maxWidth`, `maxHeight`; optional single `child` slot | [LimitedBox](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html) |
| `OverflowBox` | optional physical/directional `alignment`, finite non-negative `minWidth`, `maxWidth`, `minHeight`, `maxHeight`, and `OverflowBoxFit`; optional single `child` slot | [OverflowBox](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html) |
| `IndexedStack` | optional physical/directional `alignment`, `textDirection`, `clipBehavior`, `sizing`, nullable `index`; ordered `children` list slot | [IndexedStack](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html) |

| `ElevatedButton` Properties set | Count | Projection |
| --- | ---: | --- |
| Direct behavior and callbacks | 7 | `enabled`, strict callback IDs for press/long-press/hover/focus, `autofocus`, `clipBehavior` |
| Enabled/default style | 54 | 26 ButtonStyle leaves plus 28 effective TextStyle leaves |
| Disabled style | 54 | Sparse disabled-state overrides; omission inherits theme/default |
| Pressed style | 54 | Sparse pressed-state overrides |
| Hovered style | 54 | Sparse hovered-state overrides |
| Focused style | 54 | Sparse focused-state overrides |
| Common layout and feedback | 9 | Visual density, tap target, animation duration, feedback, alignment and splash factory |

ElevatedButton generation uses a direct sparse `ButtonStyle`, not lossy
`styleFrom` reconstruction. Scalar state resolution is deterministic; compound
leaves layer default, focused, hovered and pressed fragments independently,
while disabled remains isolated from enabled fragments. Omitted leaves fall
through to the non-null `ElevatedButtonTheme` compound as one atomic value,
then framework defaults. A locally omitted `fixedSize` axis uses Flutter's
infinity sentinel only when neither inherited source supplies a size, and a
finite effective maximum clamps that sentinel. Minimum and maximum constraints
resolve as a paired state projection whenever a local bound applies, with each
maximum axis widened to the effective minimum. The effective minimum is the
authoritative bound in that pair; otherwise both properties return `null` and
defer normally.
Text family, fallback and
package leaves layer together; each package applies to the current layered
`TextStyle`, including TextTheme, and fallback-only packages replace one prior
prefix without constructing a synthetic `.../null` family. Local Text
theme/inherit fields require one explicit
transition-safe inherit mode across reachable states. The
closed contract includes all `SystemMouseCursors`, six shape presets
(`roundedRectangle`, `roundedSuperellipse`, `stadium`, `circle`,
`beveledRectangle`, `continuousRectangle`) and four splash presets
(`InkSplash`, `InkRipple`, `InkSparkle`, `NoSplash`). `TextStyle.color` is not a
leaf because effective label color belongs to `ButtonStyle.foregroundColor`.
`ButtonStyle.iconAlignment` is not a leaf either: pinned Flutter 3.44.8 consumes
it only inside the icon/label child created by `ElevatedButton.icon`; this slice
models the ordinary arbitrary-child `ElevatedButton` constructor.
Runtime-only `key`, `focusNode`, `statesController` and layer builders are also
excluded. Callback properties admit strict Dart identifiers only. That slice
originally landed on payload v8; the current aggregate protocol is v18 and still
sends `callbackPresence` instead of an identifier, so the runner installs inert
typed closures. The optional-single required-named-nullable child slot emits
`child: null` while empty.

| `AppBar` Properties set | Count | Projection |
| --- | ---: | --- |
| Behavior | 9 | Automatic controls, notification preset, semantics, clipping and color animation |
| Layout | 7 | Centering, spacing, opacities, toolbar/leading dimensions and actions padding |
| Colors and elevation | 6 | Theme-aware colors plus resting and scrolled-under elevation |
| Shape | 10 | Closed serializable `ShapeBorder`, `BorderSide` and corner/eccentricity subset |
| Leading icon theme | 9 | Optional `IconThemeData` fields |
| Actions icon theme | 9 | Optional `IconThemeData` fields |
| Toolbar text style | 31 | Optional theme base plus complete reviewed local `TextStyle` leaves |
| Title text style | 31 | Optional theme base plus complete reviewed local `TextStyle` leaves |
| System UI overlay | 8 | Typed status/navigation bar `SystemUiOverlayStyle` fields |

| `Text` Properties set | Count | Typed leaf names |
| --- | ---: | --- |
| Text | 8 | `data`, `textAlign`, `textDirection`, `softWrap`, `overflow`, `maxLines`, `textWidthBasis`, `selectionColor` |
| Accessibility | 2 | `semanticsLabel`, `semanticsIdentifier` |
| Locale and scaling | 7 | `localeLanguageCode`, `localeScriptCode`, `localeCountryCode`, `textScalerFactor`, `textHeightApplyFirstAscent`, `textHeightApplyLastDescent`, `textHeightLeadingDistribution` |
| Text style | 25 | `styleThemeTextStyle`, `styleInherit`, `styleColor`, `styleBackgroundColor`, `styleFontSize`, `styleFontWeight`, `styleFontStyle`, `styleLetterSpacing`, `styleWordSpacing`, `styleTextBaseline`, `styleHeight`, `styleLeadingDistribution`, `styleLocaleLanguageCode`, `styleLocaleScriptCode`, `styleLocaleCountryCode`, `styleDecorationUnderline`, `styleDecorationOverline`, `styleDecorationLineThrough`, `styleDecorationStyle`, `styleDecorationThickness`, `styleDebugLabel`, `styleFontFamily`, `styleFontFamilyFallback`, `stylePackage`, `styleOverflow` |
| Paint and effects | 4 | `styleForeground`, `styleBackground`, `styleShadows`, `styleDecorationColor` |
| Advanced typography | 2 | `styleFontFeatures`, `styleFontVariations` |
| Strut style | 11 | `strutFontFamily`, `strutFontFamilyFallback`, `strutFontSize`, `strutHeight`, `strutLeadingDistribution`, `strutLeading`, `strutFontWeight`, `strutFontStyle`, `strutForceHeight`, `strutDebugLabel`, `strutPackage` |

The tables above plus `AspectRatio.aspectRatio` account for 498 catalog-backed
property rows across the ten pre-Container non-`Scaffold` widgets. `Container`
adds 13 reviewed rows, `Opacity` two, Align and FractionallySizedBox three each,
Stack four, IndexedStack five, Expanded one, Image 22, TextField 54, ListView 17, Wrap nine,
FittedBox three, ConstrainedBox one, UnconstrainedBox four, LimitedBox two,
OverflowBox six, Flexible two, Spacer one, Baseline two, IntrinsicHeight zero,
IntrinsicWidth two, Offstage one, SizedOverflowBox two, Transform five,
RotatedBox one, ListBody two, OverflowBar six, SafeArea six, GridView.count 21,
SingleChildScrollView 10, ColoredBox two, Placeholder four, Directionality one,
DecoratedBox two, ClipRect two, ClipOval two, ClipRRect three, ClipPath three, ClipRSuperellipse three, PhysicalModel six, PhysicalShape five, RepaintBoundary zero, IgnorePointer two, AbsorbPointer two, ExcludeSemantics one, BlockSemantics one, MergeSemantics zero, IndexedSemantics one, ExcludeFocus one, ExcludeFocusTraversal one, Visibility seven, TickerMode two, DefaultTextHeightBehavior three, DefaultSelectionStyle four, IconTheme ten, ImageIcon four, Divider six, VerticalDivider six, Card 31, Badge 41 and CircleAvatar nine, bringing the non-`Scaffold` total to
873; `Scaffold` adds 17 reviewed scalar rows, so the current exact total is 890 writable rows across seventy-two
canonical widgets. `AspectRatio` requires one
finite positive double,
uses a creation value of `1.0`, owns one optional `child` slot and has no theme
dependency.

`SafeArea` is the exact const `flutter.widgets.SafeArea` default-constructor
contract from Flutter 3.44.8. Its optional `left`, `top`, `right` and `bottom`
booleans omit to `true`; optional `minimum` omits to `EdgeInsets.zero`; and
optional `maintainBottomViewPadding` omits to `false`. `minimum` accepts only
signed finite physical `EdgeInsets`; `EdgeInsetsDirectional`, non-finite
components and arbitrary Dart expressions fail closed. The any-widget `child`
slot is required with cardinality one. Creation therefore uses a generic atomic
wrap around an existing widget rather than an empty prototype.

`Opacity` is the exact const `flutter.widgets.Opacity` contract from
`package:flutter/widgets.dart`: required named finite `double opacity` in
inclusive `[0, 1]`, optional named `bool alwaysIncludeSemantics` with omitted
Flutter default `false`, and one optional single any-widget `child`; `key` is
excluded. The prototype stores only `opacity: 1.0` and an empty child. The
native and exact-Web projections build real Flutter `Opacity`. At opacity zero
the child remains hit-testable; its semantics are normally suppressed and are
retained only when `alwaysIncludeSemantics` is true. IDE-owned selection,
hit-testing and drop overlays remain outside the effect, including the empty
zero-size target. Opacity reads neither Theme nor Directionality. Existing
double/boolean/single-slot encodings keep `.fd` v6, Catalog API 5, Canvas model
v11 and the version-1 framing/control contracts unchanged.

`Align` is the exact const `flutter.widgets.Align` contract from
`package:flutter/widgets.dart`: optional physical/directional
`AlignmentGeometry alignment`, optional finite non-negative `widthFactor` and
`heightFactor`, and one optional single any-widget `child`; `key` is excluded.
The prototype stores no properties, preserving the omitted `Alignment.center`
and null-factor behavior. Directional alignment resolves through LTR/RTL,
physical alignment does not, and coordinates may extrapolate beyond `[-1, 1]`.
Native and exact-Web projections build real Flutter `Align`; a factor-driven
empty zero-size instance receives only an IDE-owned selection/drop target.
Existing alignment/numeric/single-slot encodings keep all versions unchanged.

`FractionallySizedBox` adds optional alignment and non-negative width/height
factors plus an optional child. `Stack` adds alignment, text direction, fit,
clip behavior and ordered non-positioned children. `Expanded` adds optional flex
and a required child; Palette creation atomically wraps an existing direct
Row/Column child rather than creating a terminal placeholder. `Image` is a
22-property leaf whose required provider uses the first declared asset when
available. With no usable inventory it stores a reserved unresolved value,
renders a built-in placeholder and remains editable after Save/reopen.

The const Material `TextField` leaf adds 54 optional rows grouped as Input (14),
Layout (9), Behavior (11), Cursor and selection (11), Callbacks (8) and
Restoration (1). It stores constructor intent but not typed text, selection,
controller or focus state. Radius and scroll-padding compounds mutate
atomically. Generated Dart and Canvas use `LayoutBuilder`/`SizedBox`, supplying
width 240 only for unbounded width and height 120 only when `expands: true` meets
unbounded height; no TextField placement exception is required.

The non-const static `ListView(children: ...)` adds 17 optional rows grouped as
Scrolling (5), Layout (4), Caching and children (4), Semantics (2), and
Restoration (2), plus one ordered any-widget `children` list. Closed axis,
physics, drag, keyboard-dismiss, clip and hit-test presets, non-negative
padding/item/cache extents and the three child-delegate flags map directly to
Flutter; `scrollCacheExtent` generates `ScrollCacheExtent.pixels` and
`semanticChildCount` cannot exceed the static child count. Controller-owned
state, builders/delegates, `itemExtentBuilder`, `prototypeItem`, deprecated
`cacheExtent`, `key` and raw Dart remain excluded. Generated Dart and Canvas
build a real ListView and preserve vertical/horizontal, reverse and LTR/RTL
insertion geometry. Their shared guard supplies height 120 or width 240 for an
unbounded viewport cross axis, and for an unbounded main axis only when
`shrinkWrap` is false. This completes the originally agreed eight-item core Palette—
`Container`, `Row`, `Column`, `Text`, `Image`, Button through `ElevatedButton`,
`TextField` and `ListView`—at 8/8, not every Flutter widget.

The const `Wrap(children: ...)` is the first post-core vertical slice. Its nine
optional non-`key` rows cover axis, child/run alignment, finite signed spacing
and run spacing, cross-axis alignment, text and vertical direction, and
clipping. The ordered any-widget `children` list is the twentieth reusable
destination. Generated Dart and both Canvas projections build the real Flutter
Wrap. Empty Wraps retain a 36-pixel non-layout-affecting selection target; both
empty and populated instances expose their full rectangle as the deterministic
terminal append zone because wrapped runs have no single stable linear edge.

The const `FittedBox` is the second post-core vertical slice. Its three optional
rows are `fit`, `alignment` and `clipBehavior`; its optional `child` is the
twenty-first reusable any-widget destination. Omitted values preserve
`BoxFit.contain`, `Alignment.center` and `Clip.none`. The fit domain contains
`fill`, `contain`, `cover`, `fitWidth`, `fitHeight`, `none` and `scaleDown`;
alignment preserves finite physical or directional coordinates; and clipping
contains `none`, `hardEdge`, `antiAlias` and `antiAliasWithSaveLayer`. Generated
Dart and both Canvas projections build the real Flutter FittedBox, preserving
framework scaling, LTR/RTL resolution and clipping. Empty zero-size instances
retain only the 36-pixel Designer selection/drop target. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo, and reviewed light/dark 16- and 32-pixel SVG icons complete the same
vertical slice. At that milestone the practical Material/Base backlog was 22/92
complete.

The non-const `ConstrainedBox` is the third post-core vertical slice. Its one
required row is `constraints`; its optional `child` is the twenty-second
reusable any-widget destination. The structured editor admits finite,
unbounded and expanding width/height axes. Finite bounds are non-negative and a
minimum cannot exceed its maximum; an infinite minimum requires an infinite
maximum. A prototype stores `0..∞` on both axes. Generated Dart and both Canvas
projections construct the real Flutter ConstrainedBox. Empty zero-size instances
retain only the bounded Designer selection/drop target. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG icons complete the slice. At that
milestone the practical backlog was 23/92 complete with 69 remaining, and
Layout contained 15 items.

The const
[`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
is the fourth post-core vertical slice, at Layout order 107. Its four optional
rows are `textDirection`, `alignment`, `constrainedAxis` and `clipBehavior`; its
optional `child` is the twenty-third reusable any-widget destination. A new
prototype stores no properties. Omission preserves `Alignment.center`, no
retained axis and `Clip.none`, while ambient `Directionality` resolves
directional alignment when `textDirection` is omitted. Generated Dart and both
Canvas projections construct the real Flutter UnconstrainedBox, removing both
incoming axes or retaining exactly the selected horizontal or vertical axis.
Empty zero-size instances retain only the bounded Designer selection/drop
target. Properties, creation, Palette/tree/Canvas DnD, exact-slot editing,
same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG icons
complete the slice. At that milestone the practical backlog was 24/92 complete
with 68 remaining, and Layout contained 16 items.

The const
[`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
is the fifth post-core vertical slice, at Layout order 108. Its optional
`maxWidth` and `maxHeight` rows admit finite non-negative doubles, and its
optional `child` is the twenty-fourth reusable any-widget destination. A new
prototype stores no properties. Omission canonically preserves each
`double.infinity` default; finite values are emitted explicitly. Generated Dart
and both Canvas projections construct the real Flutter LimitedBox, applying a
maximum only when the incoming maximum constraint on that axis is unbounded.
Properties, creation, Palette/tree/Canvas DnD, exact-slot editing, same-tree
movement, Save/reopen, Undo/Redo and reviewed light/dark SVG icons complete the
slice. At that milestone the practical backlog was 25/92 complete with 67
remaining, no later widget had an explicit order, and Layout contained 17 items.

The const
[`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
is the sixth post-core vertical slice, at Layout order 109. Its six optional
rows are physical/directional `alignment`, finite non-negative `minWidth`,
`maxWidth`, `minHeight`, `maxHeight`, and the exact `OverflowBoxFit.max` or
`OverflowBoxFit.deferToChild` enum; its optional `child` is the twenty-fifth
reusable any-widget destination. A new prototype stores no properties. Omission
preserves `Alignment.center`, inherits each corresponding parent constraint,
and preserves `OverflowBoxFit.max`. When both values of one axis are present,
its minimum cannot exceed its maximum. Generated Dart imports the widget from
`package:flutter/widgets.dart` and the pinned enum from
`package:flutter/rendering.dart`; both Canvas projections construct the real
Flutter `OverflowBox`. Properties, creation, Palette/tree/Canvas DnD,
exact-slot editing, same-tree movement, Save/reopen, Undo/Redo and reviewed
light/dark SVG icons complete the slice. At that milestone the practical backlog
was 26/92 complete with 66 remaining, and Layout contained 18 items.

The const
[`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
is the seventh post-core vertical slice, at Layout order 130 immediately after
Expanded. Its optional `flex` row admits portable non-negative integers and its
optional `fit` row admits only `FlexFit.loose` or `FlexFit.tight`; omission
preserves Flutter's `flex: 1` and `FlexFit.loose` defaults. Its one required
single any-widget `child` is never represented by an empty terminal prototype.
Palette, tree and Canvas creation instead wrap an existing direct
`Row.children` or `Column.children` child atomically. Flexible and Expanded
cannot wrap either wrapper type, and the occupied required child is
replacement-only, not clearable and not an insertable matrix destination.
Generated Dart and both Canvas projections construct the real Flutter
Flexible, keeping it directly below Row or Column while Designer
instrumentation remains inside its child. Zero flex is admitted as inflexible
layout; positive loose flex may use less than its allocation, while positive
tight flex fills it. Properties, creation, Palette/tree/Canvas DnD, same-tree
movement, deterministic generation, Save/reopen, Undo/Redo and reviewed
light/dark SVG icons complete the slice. At that milestone the practical backlog
was 27/92 complete with 65 remaining, and Layout contained 19 items.

The const [`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html)
is the eighth post-core vertical slice, at Layout order 140 immediately after
Flexible. Its const `package:flutter/widgets.dart` constructor's only non-`key`
row is optional positive portable
integer `flex`; omission preserves Flutter's `flex: 1`, while zero, negative and
over-limit values fail closed. Spacer has no slots. Palette, tree and Canvas
creation insert it only into direct `Row.children` or `Column.children`; unlike
Expanded and Flexible, it never wraps an existing child. Expanded and Flexible
cannot wrap Spacer because Spacer's internal Expanded parent-data path must
remain directly below Row or Column. Generated Dart emits the real Flutter
Spacer, and both Canvas projections keep that real widget direct while exposing
selection, hit testing and outlines through surface-overlay instrumentation.
Properties, insertion, same-tree movement, deterministic generation,
Save/reopen, Undo/Redo and reviewed light/dark SVG icons complete the slice. The
practical backlog was 28/92 complete with 64 remaining; Layout contained 20
items.

The const [`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html)
is the ninth post-core vertical slice, at Layout order 150 immediately after
Spacer. Its complete non-`key` constructor surface is required finite-double
`baseline`, required `TextBaseline.alphabetic`/`ideographic` `baselineType` and
one optional single any-widget `child`. Flutter has no constructor defaults, so
the Designer prototype stores the reviewed visible values `baseline: 24.0` and
`baselineType: TextBaseline.alphabetic`. Generated Dart and both Canvas
projections construct the real Flutter Baseline. A childless node retains
framework `constraints.smallest` layout (often zero), while a bounded
non-layout-affecting target supplies selection and child insertion. Properties,
exact-slot editing,
Palette/tree/Canvas DnD, same-tree movement, Save/reopen, Undo/Redo and reviewed
light/dark SVG icons complete the slice. At that milestone the practical backlog
was 29/92 complete with 63 remaining; Layout contained 21 items. `.fd` remains v7, Catalog API
remains 6, Canvas model remains v12, and NBFC framing plus Canvas control/wire
remain version 1.

The const
[`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
is the tenth post-core vertical slice, at Layout order 160 immediately after
Baseline. Its complete non-`key` constructor surface has no writable
properties and one optional single any-widget `child`. Generated Dart and both
Canvas projections construct the real Flutter IntrinsicHeight, preserving
parent constraints and the speculative intrinsic-height pass. Palette and slot
descriptions expose the framework performance warning: intrinsic measurement is
relatively expensive and can be O(N²) in tree depth. An empty or collapsed node
receives only the bounded, non-layout-affecting Designer selection and child
insertion target. Exact-slot editing, Palette/tree/Canvas DnD, same-tree
movement, Save/reopen, Undo/Redo and reviewed light/dark SVG icons complete the
slice. At that milestone the practical backlog was 30/92 complete with 62
remaining; Layout contained 22 items. `.fd` remains v7, Catalog API remains 6,
Canvas model remains v12, and NBFC framing plus Canvas control/wire remain
version 1.

The const
[`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
is the eleventh post-core vertical slice, at Layout order 170 immediately after
IntrinsicHeight. Its complete non-`key` constructor surface exposes optional
finite non-negative `stepWidth` and `stepHeight` doubles plus one optional
single any-widget `child`. Null and explicit zero remain distinct stored and
generated values; Flutter treats either as no snapping on that axis, while a
positive step rounds the corresponding intrinsic child extent upward to its
next multiple. Generated Dart and both Canvas projections construct the real
Flutter IntrinsicWidth under parent constraints. Palette, property and slot
descriptions expose the relatively expensive speculative layout pass and
worst-case O(N²) tree-depth behavior. An empty or collapsed node receives only
the bounded, non-layout-affecting Designer selection and child insertion target.
Property and exact-slot editing, Palette/tree/Canvas DnD, same-tree movement,
Save/reopen, Undo/Redo and reviewed light/dark SVG icons complete the slice. At
that milestone the practical backlog was 31/92 complete with 61 remaining;
Layout contained 23 items. `.fd` remains v7, Catalog API
remains 6, Canvas model remains v12, and NBFC framing plus Canvas control/wire
remain version 1.

The const
[`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html) is
the twelfth post-core vertical slice, at Layout order 180 immediately after
IntrinsicWidth. Its complete non-`key` constructor surface exposes optional
boolean `offstage` with default `true` plus one optional single any-widget
`child`. Omission and explicit `true` remain distinct stored and generated
values even though both hide the child; explicit `false` restores normal
participation. Generated Dart and both Canvas projections construct the real
Flutter Offstage. While hidden, Flutter still lays the child out and keeps it
active and focusable, including animations, but suppresses paint, hit testing
and semantics and normally contributes no parent space. Palette, property and
slot descriptions expose this resource warning and recommend removing a
long-hidden subtree when ongoing work is undesirable. A zero-sized result keeps
a bounded 36x36, non-layout-affecting Designer selection and child insertion
target outside the Offstage effect. Property and exact-slot editing,
Palette/tree/Canvas DnD, same-tree movement, Save/reopen, further editing,
Undo/Redo and reviewed light/dark SVG icons complete the slice. The practical
backlog was 32/92 complete with 60 remaining; Layout contained 24 items. `.fd`
remained v7, Catalog API remained 6, Canvas model remained v12, and NBFC framing
plus Canvas control/wire remained version 1.

The const
[`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
is the thirteenth post-core vertical slice, at Layout order 190 immediately
after Offstage. Its complete non-`key` constructor surface exposes required
structured `Size size`, optional `AlignmentGeometry alignment` with default
`Alignment.center`, plus one optional single any-widget `child`. New instances
persist `Size(100, 100)`, and both finite non-negative dimensions are edited as
one atomic property. Generated Dart and both Canvas projections construct the
real Flutter SizedOverflowBox: the parent constrains its requested outer size,
while the child receives the original incoming constraints and may paint
outside according to alignment; hit testing remains bounded by the parent.
A true zero-sized result retains only the bounded, non-layout-affecting Designer
selection and child-insertion target. Property and exact-slot editing,
Palette/tree/Canvas DnD, same-tree movement, Save/reopen and further editing,
Undo/Redo and reviewed light/dark SVG icons complete the slice. The practical
backlog was then 33/92 complete with 59 remaining; Layout contained 25 items.
The new atomic Size wire value advanced `.fd` to v8, Catalog API to 7 and Canvas
model to v13; NBFC framing plus Canvas control/wire remained version 1.

The const
[`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
is the fourteenth post-core vertical slice, at Layout order 200 immediately
after SizedOverflowBox in Flutter's Layout catalog. Its complete `Transform.new`
non-`key` surface exposes required structured `Matrix4 transform`, optional
atomic signed finite `Offset origin`, optional `AlignmentGeometry alignment`,
optional boolean `transformHitTests` with default `true`, optional closed
`FilterQuality.none/low/medium/high`, plus one optional single any-widget
`child`. New instances persist only `Matrix4.identity()`. Generated Dart and
both Canvas projections construct the real paint-time Transform: layout size is
unchanged, origin and alignment compose, filtering remains optional, and hit
testing follows the matrix only when enabled. Designer selection/drop geometry
uses the same effective transform without changing layout; only a true
zero-sized node gets the bounded 36x36 target. Property and exact-slot editing,
Palette/tree/Canvas DnD, same-tree movement, Save/reopen and further editing,
Undo/Redo and reviewed light/dark SVG icons complete the slice. Named
rotate/translate/scale/flip constructors remain outside it. The practical
backlog is now 34/92 complete with 58 remaining; Layout contains 26 items. The
new atomic Offset wire value advances `.fd` to v9, Catalog API to 8 and Canvas
model to v14; NBFC framing plus Canvas control/wire remain version 1.

The const
[`RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
is the fifteenth post-core vertical slice, at Layout order 210 immediately after
Transform. Its complete non-`key` surface exposes required signed portable
integer `quarterTurns` and one optional any-widget `child`. Detached prototypes
store `1`; exact native/Web validation admits
`-9007199254740991..9007199254740991`. Generated Dart and both Canvas
projections construct the real layout-time RotatedBox, exchanging axes on odd
turns and retaining them on even turns while Flutter paints the modulo-four
equivalent. Properties, exact-slot editing, Palette/tree/Canvas DnD,
same-tree movement, Save/reopen and further signed editing, Undo/Redo and
reviewed light/dark SVG icons complete the slice. The practical backlog is now
35/92 complete with 57 remaining; Layout contains 27 items. Existing value and
wire types keep `.fd` v9, Catalog API 8 and Canvas model v14 unchanged; NBFC
framing plus Canvas control/wire remain version 1.

The const
[`ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html) is
the sixteenth post-core vertical slice, at Layout order 220 immediately after
RotatedBox. Its complete non-`key` surface exposes optional `Axis mainAxis` and
`bool reverse`, preserving `Axis.vertical` and `false` when omitted, plus one
ordered any-widget `children` list. Detached prototypes omit both properties and
begin with an empty list. Generated Dart constructs a real bare ListBody. Native
and exact-Web Canvas projections wrap that real widget only in an axis-matched
design-time viewport so `RenderListBody` receives an unbounded main axis and a
bounded cross axis; the preview guard is not persisted or generated. Empty and
terminal list-drop geometry follows main axis, reversal and ambient directionality.
Properties, exact-list-slot editing, Palette/tree/Canvas DnD, same-tree
movement/reordering, Save/reopen and further editing, Undo/Redo and reviewed
light/dark SVG icons complete the slice. The practical backlog is now 36/92
complete with 56 remaining; Layout contains 28 items. Existing value and wire
types keep `.fd` v9, Catalog API 8 and Canvas model v14 unchanged; NBFC framing
plus Canvas control/wire remain version 1.

The const
[`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
is the seventeenth post-core vertical slice, at Layout order 230 immediately
after ListBody. Its complete non-`key` surface exposes optional finite signed
`spacing`, nullable `MainAxisAlignment alignment`, finite signed
`overflowSpacing`, `OverflowBarAlignment overflowAlignment`,
`VerticalDirection overflowDirection`, nullable `TextDirection textDirection`,
and one ordered any-widget `children` list. Detached prototypes omit all six
properties and begin empty. Generated Dart constructs a real bare OverflowBar.
For a nonempty node, native and exact-Web Canvas projections bound an otherwise
unbounded preview width only when `alignment` is non-null; null alignment retains
Flutter's natural width. They also retain a 36x36 empty target; neither guard is
persisted or generated. Fitting horizontal DnD follows effective LTR/RTL order, while a
vertical overflow column follows `overflowDirection`. Properties,
exact-list-slot editing, Palette/tree/Canvas DnD, same-tree
movement/reordering, Save/reopen and further editing, Undo/Redo and reviewed
light/dark SVG icons complete the slice. The practical backlog is now 37/92
complete with 55 remaining; Layout contains 29 items. Existing value and wire
types keep `.fd` v9, Catalog API 8 and Canvas model v14 unchanged; NBFC framing
plus Canvas control/wire remain version 1.

The non-const
[`GridView.count`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
is the eighteenth post-core vertical slice, at Scrolling order 20 immediately
after ListView. Its catalog type is `flutter.widgets.GridView`; class and named
constructor are stored separately. The 21 rows cover scrolling policy, shrink
wrapping and padding, required positive `crossAxisCount` (creation value 2),
main/cross spacing, child aspect ratio, optional main-axis extent, delegate
lifecycle flags, pixel cache extent, semantics, drag/keyboard behavior,
restoration, clipping and hit testing. Its exact ordered any-widget `children`
slot starts empty. `mainAxisExtent` determines tile extent when present instead
of the aspect-ratio-derived value. Controller state, builder/delegate inputs,
other named constructors, `scrollBehavior`, `key` and deprecated raw
`cacheExtent` remain excluded. Properties, exact-list-slot editing,
Palette/tree/Canvas DnD, same-tree movement/reordering, Save/reopen and further
editing, Undo/Redo, the shared generated/Canvas 240-wide/120-high unbounded-
constraint guard and four reviewed light/dark 16/32 px SVG icons complete the
slice. At the `GridView.count` milestone, the practical backlog was 38/92
complete with 54 remaining; Layout had 29 items and Scrolling had two. Existing
value and wire types kept `.fd` v9, Catalog API 8 and Canvas model v14 unchanged;
NBFC framing plus Canvas control/wire remained version 1.

The const
[`SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
is the nineteenth post-core vertical slice, at Scrolling order 30 immediately
after `GridView.count`. Its 10 rows cover scrolling direction, reversal,
non-negative padding, nullable primary policy, one closed physics preset,
drag-start behavior, clipping, hit testing, restoration ID and keyboard
dismissal. Its optional any-widget `child` slot starts empty, and every property
is omitted from a detached prototype. Controller-owned state, arbitrary physics
graphs, `key` and raw Dart remain excluded. Properties, exact-single-slot
editing, Palette/tree/Canvas DnD, same-tree movement, Save/reopen and further
editing, Undo/Redo, and four reviewed light/dark 16/32 px SVG icons complete the
slice. Generated Dart and Canvas construct the real widget without the
ListView/GridView bounded-viewport guard because SingleChildScrollView
deliberately shrink-wraps both axes. Canvas alone provides a
non-layout-affecting 36x36 target for an empty or zero-size node. The practical
backlog is now 39/92 complete with 53 remaining; Layout remains at 29 items and
Scrolling contains three. Existing value and wire types keep `.fd` v9, Catalog
API 8 and Canvas model v14 unchanged; NBFC framing plus Canvas control/wire
remain version 1.

The const
[`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is the twentieth post-core vertical slice, at Basic order 40 after `Image`. Its
required `color` starts as literal `Color(0xFF2196F3)`; optional `isAntiAlias`
remains omitted to preserve Flutter's `true` default, and its optional any-widget
`child` starts empty. The color domain admits exact ARGB or a reviewed Material
`ColorScheme` token. Literal output remains const; a token emits
`Theme.of(context).colorScheme...` and removes const from that constructor.
Properties, exact-single-slot editing, Palette/tree/Canvas DnD, same-tree
movement, Save/reopen and further editing, Undo/Redo, and four reviewed
light/dark 16/32 px SVG icons complete the slice. Generated Dart and Canvas
construct the real widget. Canvas alone provides a non-layout-affecting 36x36
target for an empty zero-size node, and never persists it. At the `ColoredBox`
milestone, the practical backlog was 40/92 complete with 52 remaining; Layout
had 29 items, Scrolling three and Basic four. Existing value and wire types
remained `.fd` v9, Catalog API 8 and Canvas model v14; NBFC framing plus Canvas
control/wire remained version 1.

The const
[`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html) is
the twenty-first post-core vertical slice, at Layout order 240 immediately after
`OverflowBar`. Its six optional properties are `left`, `top`, `right`, `bottom`,
`minimum` and `maintainBottomViewPadding`; omission preserves Flutter's exact
`true`, `true`, `true`, `true`, `EdgeInsets.zero` and `false` defaults.
`minimum` is signed finite physical `EdgeInsets` only. Directional insets,
non-finite components and arbitrary expressions are rejected. The required
any-widget `child` is populated only by a generic atomic wrap around an existing
widget. Palette/tree can wrap root and non-root widgets; the current Canvas
target wire can wrap non-root children only and intentionally offers no root
target. Root wrapping remains available through the tree. SafeArea cannot wrap
Expanded, Flexible or Spacer because their ParentData must stay attached
directly to Row or Column. Properties, Save/reopen/further-edit, Undo/Redo,
deterministic Dart generation and Palette/tree/Canvas wrapper admission complete
the slice. At that milestone the practical backlog was 41/92 complete with 51
remaining. The catalog contained 41 widgets and 35 reviewed const definitions, with 722 writable
rows (705 outside Scaffold); Palette categories contained 30 Layout, three
Scrolling, four Basic and four Material items. The 41 sources and 39 insertable
destinations formed 1,599 cells, with 1,414 admitted and 185 rejected. `.fd` v9,
Canvas model v14, and NBFC framing plus Canvas control/wire v1 remain unchanged;
the exported `EdgeInsetsValues.directionalAllowed` constraint advances Catalog
API to 9.

The const
[`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is the twenty-second post-core vertical slice, at Basic order 50 immediately
after `ColoredBox`. Its optional `color`, `strokeWidth`, `fallbackWidth` and
`fallbackHeight` properties all remain omitted on creation, preserving
Flutter's exact `Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults. The
three numeric fields accept only finite non-negative integers or doubles.
Color accepts exact ARGB or a reviewed Material `ColorScheme` token; literals
preserve const while theme lookup removes it. The optional any-widget `child`
starts empty and is an ordinary insertable destination. Properties,
Palette/tree/Canvas DnD, exact slot editing, same-tree movement,
Save/reopen/further-edit, Undo/Redo, deterministic generation, real
native/exact-Web Canvas, accessibility and reviewed light/dark 16/32 px SVGs
complete the slice. At that milestone the practical backlog was 42/92 complete
with 50 remaining. The catalog contained 42 widgets and 36 reviewed const definitions,
with 726 writable rows (709 outside Scaffold); Palette categories contain 30
Layout, three Scrolling, five Basic and four Material items. The 42 sources and
40 insertable destinations formed 1,680 cells, with 1,490 admitted and 190
rejected. Existing encodings keep `.fd` v9, Catalog API 9, Canvas model v14 and
NBFC framing plus Canvas control/wire v1 unchanged.

The const
[`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is the twenty-third post-core vertical slice, at Basic order 60 immediately
after `Placeholder`. Its exact Flutter 3.44.8 constructor has required
`TextDirection textDirection` and required `child`, with no framework default
for either argument. Designer creation therefore atomically wraps an existing
root or non-root widget and persists the explicit reviewed creation value
`TextDirection.ltr`; Properties may change it to `rtl`, but cannot Restore
Default. The occupied required-child slot is replacement-only. The generic
wrapper admission rejects `Expanded`, `Flexible` and `Spacer`, whose ParentData
must stay directly below Row or Column. Native and exact-Web Canvas construct
the real inherited `Directionality`; directional descendants resolve through
the selected value while IDE-owned selection, hit and drop overlays remain
outside it. Properties, Palette/tree/Canvas DnD, exact slot replacement,
same-tree movement, Save/reopen/further-edit, Undo/Redo, generation,
accessibility and reviewed light/dark 16/32 px SVGs complete the slice. The
practical backlog was then 43/92 complete with 49 remaining. The catalog contained
43 widgets and 37 reviewed const definitions, with 727 writable rows (710
outside Scaffold); Palette categories contain 30 Layout, three Scrolling, six
Basic and four Material items. The 43 sources and 40 insertable destinations
form 1,720 cells, with 1,528 admitted and 192 rejected. Existing encodings keep
`.fd` v9, Catalog API 9, Canvas model v14 and NBFC framing plus Canvas
control/wire v1 unchanged.

The const
[`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is the twenty-fourth post-core vertical slice, at Basic order 70 immediately
after `Directionality`. Its exact Flutter 3.44.8 constructor has required
`Decoration decoration`, optional `DecorationPosition position` with omitted
`background`, and optional `child`. Designer creation persists the exact empty
rectangular `BoxDecoration()` and the complete existing BoxDecoration value,
constraint, codec, generator, Canvas and editor stack supplies the reviewed
closed `Decoration` branch. Custom Decoration subclasses and raw Dart remain
excluded. Native and exact-Web Canvas construct the real widget and preserve
background/foreground paint while IDE-owned selection and drop affordances
remain outside it. Properties, Palette/tree/Canvas DnD, exact slot editing,
same-tree movement, Save/reopen/further-edit, Undo/Redo, generation,
accessibility and reviewed light/dark 16/32 px SVGs complete the slice. The
practical backlog at that milestone was 44/92 complete with 48 remaining. The
catalog contained 44 widgets and 38 reviewed const definitions, with 729
writable rows (712 outside Scaffold); Palette categories contained 30 Layout,
three Scrolling, seven Basic and four Material items. The 44 sources and 41
insertable destinations formed 1,804 cells, with 1,607 admitted and 197
rejected. Existing encodings kept `.fd` v9, Catalog API 9, Canvas model v14 and
NBFC framing plus Canvas control/wire v1 unchanged.

The const
[`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
opens the Accessibility Palette category at category order 400 and item order
10. Its complete Flutter 3.44.8 constructor has optional `bool excluding` with
omitted default `true` and optional `child`; only `key` is excluded. The
prototype stores neither a property nor a child. Omitted or explicit `true`
removes the application child's semantics subtree; explicit `false` preserves
it, while layout, paint and hit testing continue to proxy the child. Native and
exact-Web Canvas build the real widget with the `ExcludeSemantics` node's own
Designer selection, hit/drop and accessibility wrapper outside the effect.
Descendant Canvas semantics labels follow the real subtree exclusion; the
separate NetBeans widget tree remains accessible. Properties,
Palette/tree/Canvas DnD, exact slot editing, same-tree movement,
Save/reopen/further-edit, Undo/Redo, generation and four reviewed SVGs complete
the slice. At that milestone the practical backlog was 45/92 complete with 47 remaining. The
catalog contained 45 widgets and 39 reviewed const definitions, with 730
writable rows (713 outside Scaffold); Palette categories contained 30 Layout,
three Scrolling, seven Basic, four Material and one Accessibility item. The 45
sources and 42 insertable destinations formed 1,890 cells, with 1,688 admitted and
202 rejected. Existing encodings kept `.fd` v9, Catalog API 9, Canvas model v14
and NBFC framing plus Canvas control/wire v1 unchanged.

The const
[`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
fills the next fixed-inventory Layout gap at item order 115 beside `Stack`. Its
complete Flutter 3.44.8 non-key surface has optional `alignment`,
`textDirection`, `clipBehavior`, `sizing`, nullable `index` and ordered
`children`. The prototype stores neither properties nor children. Omitted index
uses Flutter's default zero, a non-negative integer selects one existing child,
and the exact typed null value selects none. Validation rechecks that relation
after property and child-list changes, including Flutter's empty-list
index-zero exception. Native and exact-Web Canvas construct the real widget:
layout uses the largest child while paint, hit testing and application semantics
use only the selected child; every child remains ordered in the Designer model
and NetBeans tree. Properties, Palette/tree/Canvas DnD and movement, exact slot
editing, Save/reopen/further-edit, Undo/Redo, generation, accessibility and four
reviewed SVGs complete the slice. At that milestone the practical backlog was 46/92 complete
with 46 remaining. The catalog contained 46 widgets and 40 reviewed const
definitions, with 735 writable rows (718 outside Scaffold); Palette categories
contained 31 Layout, three Scrolling, seven Basic, four Material and one
Accessibility item. The 46 sources and 43 insertable destinations formed 1,978
cells, with 1,771 admitted and 207 rejected. Exact typed null advances `.fd` to
v10, Catalog API to 10 and Canvas model to v15; NBFC framing plus Canvas
control/wire remain v1.

The const
[`ClipRect`](https://api.flutter.dev/flutter/widgets/ClipRect/ClipRect.html)
is the next fixed-inventory Basic slice, at item order 80 after `DecoratedBox`.
Its safe Flutter 3.44.8 non-key surface has optional typed `clipper`, optional closed `clipBehavior` and
one optional single `child`. Omission preserves `Clip.hardEdge`; the other
admitted values are `none`, `antiAlias` and `antiAliasWithSaveLayer`. Generated
Dart and native/exact-Web Canvas construct the real widget, with Designer
selection and empty-target overlays outside its paint clip. Schema v12 stores a
non-null `CustomClipper<Rect>` as an analyzed current-library or declared-package
reference; isolated Canvas receives only presence and shows an unavailable preview. Properties, Slots,
Palette/tree/Canvas DnD and movement, generation, validation,
Save/reopen/further-edit, Undo/Redo, accessibility and four reviewed SVGs
complete the slice. At that milestone the practical backlog was 47/92 complete
with 45 remaining. The catalog contained 47 widgets and 41 reviewed const definitions,
with 736 writable rows (719 outside Scaffold); Palette categories contain 31
Layout, three Scrolling, eight Basic, four Material and one Accessibility item.
The 47 sources and 44 insertable destinations form 2,068 cells, with 1,856
admitted and 212 rejected. Existing schema and protocol versions remain
unchanged.

The const
[`ClipOval`](https://api.flutter.dev/flutter/widgets/ClipOval/ClipOval.html)
is the next fixed-inventory Basic slice, at item order 90 after `ClipRect`.
Its safe Flutter 3.44.8 non-key surface has optional typed `clipper`, optional closed `clipBehavior` and
one optional single `child`. Omission preserves `Clip.antiAlias`; the other
admitted values are `none`, `hardEdge` and `antiAliasWithSaveLayer`. Generated
Dart and native/exact-Web Canvas construct the real widget, with the default
oval inscribed in the child's layout bounds and Designer selection plus
empty-target overlays outside its paint clip. Schema v12 stores a non-null
`CustomClipper<Rect>` as the same analyzed current-library or declared-package
reference; isolated Canvas receives only presence and shows an unavailable preview. Properties, Slots, Palette/tree/Canvas DnD and
movement, generation, validation, Save/reopen/further-edit, Undo/Redo,
accessibility and four reviewed SVGs complete the slice. At that milestone the practical backlog
was 48/92 complete with 44 remaining. The catalog contained 48 widgets and 42
reviewed const definitions, with 737 writable rows (720 outside Scaffold);
Palette categories contained 31 Layout, three Scrolling, nine Basic, four
Material and one Accessibility item. The 48 sources and 45 insertable
destinations formed 2,160 cells, with 1,943 admitted and 217 rejected. Existing
schema and protocol versions remained unchanged.

The
[`ClipRRect`](https://api.flutter.dev/flutter/widgets/ClipRRect/ClipRRect.html)
is the next fixed-inventory Basic slice, at item order 100 after `ClipOval`. Its
safe Flutter 3.44.8 non-key surface has optional typed `borderRadius`, optional
typed `clipper`, optional closed `clipBehavior` and one optional single `child`. The radius value supports
physical `BorderRadius` and directional `BorderRadiusDirectional` with finite,
non-negative elliptical X/Y components for all four corners. Omission preserves
`BorderRadius.zero`; omitted clip behavior preserves `Clip.antiAlias`, and the
other admitted values are `none`, `hardEdge` and `antiAliasWithSaveLayer`.
Generated Dart constructs the real widget, with Designer selection and
empty-target overlays outside its paint clip. A non-null
`CustomClipper<RRect>` is represented by a closed current-library or canonical
`package:` reference to an existing value or zero-argument constructor, factory or function,
with an optional member and an explicit const or non-const zero-argument
invocation. Generation plans the import and the
analyzer proves assignment compatibility. The isolated Canvas receives only
presence, preserves the child and shows an explicit preview-unavailable overlay
instead of faking the `borderRadius` Flutter ignores. With no custom clipper,
native/exact-Web Canvas constructs the real rounded clip. Properties, Slots,
Palette/tree/Canvas DnD and movement, generation, validation,
Save/reopen/further-edit, Undo/Redo, accessibility and four reviewed SVGs
complete that slice. The following `ClipPath` slice adds optional typed
`CustomClipper<Path>` and `ShapeBorder` references, optional `clipBehavior` and
optional `child`. Setting `shape` selects `ClipPath.shape`; the default constructor
and helper are mutually exclusive and the static helper/its ancestors are never
const. Both references reuse the analyzer-proven current/package reference editor,
including const/non-const zero-argument calls. The isolated Canvas renders the real
default clip and preserves custom geometry's child with an explicit preview warning.
`ClipRSuperellipse` completes the next clipping slice with physical/directional
elliptical `borderRadius`, exact `CustomClipper<RSuperellipse>`, all four clip
behaviors and an optional child. It reuses closed current/package references,
including const/non-const calls, and analyzer-backed save transactions. The real
Canvas widget uses continuous superellipse geometry and Flutter radius clamping;
custom delegates retain the child with an explicit preview warning, ignoring but
preserving the saved radius as Flutter does. Reopen/further-edit and Undo/Redo
preserve the typed properties and reference.
`PhysicalModel` adds six typed properties: rectangle/circle shape, all four clip
behaviors, physical-only elliptical border radius, finite non-negative elevation,
required fill color and optional shadow color. Both colors admit literals/theme
tokens; the required color's creation value is `0xFF2196F3`. Its child is optional.
The SDK ignores radius for a circle without discarding its value, and non-square
circle bounds paint an oval. Shared `BorderRadiusValues.directionalAllowed` keeps
this concrete BorderRadius API distinct from the earlier directional clip widgets.
Native/exact-Web Canvas constructs real PhysicalModel painting and shadows. Typed
editors, generation, all placement routes, configured Save/reopen/further-edit and
Undo/Redo preserve the same contract, including empty external drop affordances.
`PhysicalShape` follows with required clipper/color, clip behavior, elevation,
shadow color and optional child. Its closed ShapeBorderClipperValue covers rounded,
beveled, continuous and superellipse rectangles, circle and stadium. Cornered shapes
use physical/directional elliptical radii, with mandatory explicit direction for
directional geometry; circle/stadium retain ignored radius/direction. The alternative
CustomClipper<Path> reference supports current/declared-package values and
const/non-const zero-argument calls with strict analyzer proof. Presets render real
SDK geometry/shadows; custom code gets presence-only preview warnings. The required
default clipper is a zero-radius rounded rectangle, enabling immediate Palette use.
All property, slot, DnD, persistence, further-edit and history paths are covered.

RepaintBoundary adds a const STATIC_STRUCTURAL definition with no scalar properties
and one optional any-widget child at constructor order zero. Like IntrinsicHeight,
it publishes identity/child management but does not claim scalar PROPERTIES
capability. The SDK key remains managed identity; wrap/wrapAll are key-only helper
APIs rather than distinct paint behavior or childIndex fields. The real Canvas
boundary owns an independent paint layer without changing layout or semantics.
The slice includes every placement route, move, child replace/clear, generation,
Save/reopen/further child and descendant edits, Undo/Redo and rejected-change rollback.
No value-algebra or schema/API/model change is necessary.

IgnorePointer adds a const STATIC_EDITABLE definition with two optional BOOLEAN
fields (ignoring order 0, ignoringSemantics order 1) and optional any-widget child
at order 2. Omission preserves SDK true/null defaults; both explicit boolean values
are stored independently from unset. The deprecated semantics field is not omitted:
null follows ignoring for action blocking while preserving labels, false preserves
actions, true removes the semantic subtree. Real SDK rendering preserves layout and
paint while ignored pointer hits can pass through. Properties/Slots, all placement
routes, generated const propagation, save/reopen/further edits, history and rollback
share the existing typed boundary. No schema/API/model version changes are needed.

AbsorbPointer adds the complete const STATIC_EDITABLE counterpart at Basic order
170: optional BOOLEAN absorbing at 0, optional deprecated BOOLEAN ignoringSemantics
at 1, and optional any-widget child at 2. No defaults are materialized: omitted
absorbing stays SDK true and omitted semantics stays SDK null. Both explicit
boolean values remain distinct from omission across generation, codec, payload,
Properties/Slots, placement/move, save/reopen/further edits and history. Actual SDK
hit testing blocks descendants and lower Stack siblings when absorbing, without
changing layout or paint. The complete deprecated semantics matrix is retained;
Designer selection and editing are separate from runtime pointer delivery.

BlockSemantics adds the exact const STATIC_EDITABLE contract at Accessibility
order 20: optional BOOLEAN blocking at order 0, optional any-widget child at 1.
Omission preserves SDK true, with both explicit booleans retained independently.
The real widget suppresses earlier-painted semantics under the same semantic
boundary, not its own child/later content, and does not alter layout, paint or
pointer hit testing. The full typed/editor/placement/history/persistence contract
and external empty-node targets apply without any new value or protocol encoding.

MergeSemantics adds exact STATIC_STRUCTURAL capability at Accessibility order 30.
Its complete const constructor has only optional any-widget child at order 0 and
managed key, with zero scalar properties. It therefore exposes identity/Slots and
descendant editing, not a scalar Properties mutation capability. All child forms,
const/non-const generation/provenance, codec/payload, placement/movement,
Save/reopen/further edits and history follow the existing structural contract.
Actual SDK semantics merge the subtree into one node; layout, paint and ordinary
hits are preserved. Labels combine, the first tree-order handler wins for a shared
action, and conflicting states are not reconciled by invented Designer rules.

IndexedSemantics adds exact STATIC_EDITABLE capability at Accessibility order 40,
required INTEGER index at order 0 and optional any-widget child at order 1.
The prototype persists index 0; required omission/null fail validation and reset
cannot erase the argument. Its shared native/Web safe-integer range includes
negative values, matching the SDK's lack of a nonnegative constructor invariant.
Exact schema/projection drift is rejected. Generation/provenance, codec/payload,
Properties/Slots, placement/moves, Save/reopen/further edits, history and rollback
cover every constructor field. Actual Canvas semantics index the first genuine
child semantic node; Designer does not rewrite parent scroll metadata or sibling
indexes. Existing encodings and versions suffice.

ExcludeFocus adds exact STATIC_EDITABLE capability at Accessibility order 50:
optional BOOLEAN excluding at order 0 and required any-widget child at order 1.
Omission preserves Flutter's true default without persisting a prototype value.
Explicit false/true uses the shared centered checkbox and reset removes the field.
Generic required-child wrapping reuses the existing subtree atomically; an empty
prototype is not insertable and the occupied required slot cannot be cleared.
Exact validation/projection, const/non-const generation/provenance, codec/payload,
wrapping/moves, replacement, Save/reopen/further edits, history and rollback cover
the complete constructor. Existing model/API/protocol encodings suffice.

ExcludeFocusTraversal follows at Accessibility order 60 with the same exact
optional BOOLEAN excluding/order 0 and required any-widget child/order 1 shape.
It reuses generic atomic required-child wrapping/replacement and centered
checkbox/unset/reset. The actual widget excludes only traversal: direct requests
remain possible, current focus is retained, and other focus restrictions still
apply. A descendant's configured skipTraversal is not rewritten, although the
effective getter includes ancestor exclusion. Constructor/schema drift, malformed
payloads, generation/provenance, complete history, root/mixed focus wrapping and
ParentData restrictions are tested without new model/API/protocol versions.

Visibility adds a complete Basic/order 180 slice: required any-widget child at
order 0, optional non-null replacement at order 1, visible at order 2 and six
maintenance booleans at orders 3–8. Omission preserves true/false SDK defaults;
no creation values are stored. Empty replacement must omit its Dart argument,
not emit null. All five SDK maintenance implications are validated independently
of visible. Checkbox prerequisite/dependent edits form one atomic history unit.
Visibility.maintain is covered as the equivalent default constructor with all
six maintenance flags true. Generic required-child wrapping now admits additional
valid-empty optional slots, with no new fake children or root Canvas target.
Both slots, ignored replacement retention, nullable rejection, nonconst propagation,
Save/reopen/further edits and full history use the existing typed model.

TickerMode adds a complete Basic/order 190 const slice: required enabled at Dart
order 0, required any-widget child at order 1 and optional forceFrames at order 2.
The Designer creates enabled=true because the SDK requires an explicit value, not
because Flutter supplies a default. Enabled accepts true/false but cannot be reset;
forceFrames accepts unset/false/true and omission preserves false. No cross-field
constraint is imposed. Generic atomic wrapping already supports a required scalar
with a creation value; required-child removal/movement remains forbidden. Generation,
codec/payload, slot replacement, save/reopen/further edits, Undo/Redo and rollback
cover both flags without changing format versions.

DefaultTextHeightBehavior adds a complete Basic/order 200 const slice. Its three
optional leaves reuse Text's model names: textHeightApplyFirstAscent,
textHeightApplyLastDescent and textHeightLeadingDistribution. Catalog leaf orders
are 0/1/2 and required child order is 3. Generation collapses the leaves into the
required textHeightBehavior argument before child; all-unset emits the default
TextHeightBehavior() instead of omitting it or emitting null. Text's optional
composite path remains unchanged. Both booleans retain unset/false/true, and the
enum retains unset/even/proportional. Exact schema/capability, provenance/const,
codec/payload, required-child wrapping/replacement, history and rollback keep the
existing format versions and do not add arbitrary Dart expressions.

DefaultSelectionStyle adds a complete Basic/order 210 insertable slice. Optional
cursorColor/selectionColor/mouseCursor occupy orders 0/1/2, required child is 3,
and the Designer-only required merge Boolean is 4, initially false. Generation strips that mode
field: false uses the direct constructor, true calls static .merge with
separate symbol evidence and non-const propagation. Direct null shadows the outer
style; merge inherits unset fields independently. Colors retain literal/semantic
typing. Its closed cursor strings cover 36 SystemMouseCursors, defer/uncontrolled,
and clickable/adaptiveClickable/textable with their correct SDK owners. Existing
TextField's 36-cursor domain is untouched. Required-child wrapping/replacement,
Properties, lifecycle/history and both Canvas profiles preserve the actual SDK.
SDK fallback has an invalid child and is deliberately not an insertable type.

IconTheme adds Basic/order 220 with all nine optional IconThemeData fields and a
required child. A required Designer merge flag starts false and selects either
the ordinary const-capable constructor or non-const static merge. Required data
is emitted even when every field is unset. Exact data/factory and nested shadow/
theme provenance preserve deterministic source mapping. Size and font axes reuse
Icon's safe domains; opacity accepts any finite double and retains the raw value
while Flutter clamps its effective getter. Explicit empty shadows and false text
scaling override inheritance; all nine SDK leaves remain independently resettable.
The fallback value is fully expressible through explicit leaves; arbitrary custom
IconThemeData subclasses/resolver expressions are outside the closed value model.
Required-child wrapping, replacement protection and all editing/history paths use
the shared contracts without broadening retained FD_ONLY persistence.

ImageIcon adds Basic/order 230 with required positional image at ordinal 0, accepting
existing NullValue or ImageProviderValue with explicit null creation default. Size,
color and semanticLabel use named ordinals 0/1/2 and remain optional/resettable.
Missing image is invalid even though null is valid. Preserve const positional
generation, asset/exact/package/resize provenance and existing reserved-unresolved
handling. No new slots or value shapes are needed. Null contributes no asset
dependency/resource; typed providers use the same closed inventory and validation.
The UI chooses first available declared asset or null, distinguishes None from
unset and preserves all creation/edit/history routes. The actual SDK applies
IconTheme size/color/opacity, not axes/shadows/scaling. Arbitrary network/file/custom
providers are not represented, and the newer useOriginalColors field is absent from
the pinned Flutter 3.44.8 constructor. Formats remain 13/14/18 and NBFC1.

Divider adds Material/order 50 as a const-capable ordinary leaf. Six optional
named fields retain ordinals 0..5: height, thickness, indent, endIndent, color,
radius. The first four reuse finite non-negative integer/double constraints; color
uses literal/semantic values and radius uses existing physical/directional elliptical
geometry. No slot, required argument, creation default, value type or format bump is
needed. Default generation is const Divider(); radius provenance is generated by
the existing BorderRadius/BorderRadiusDirectional/Radius machinery.

No cross-constraint forces thickness<=height or limits indent by unknown layout.
The constructor accepts rounded hairlines; the pinned SDK paint stage asserts in
debug and ignores radius in release. Theme-dependent omitted thickness stays legal;
help recommends a positive thickness for rounded lines. Core tests cover exact
capabilities, generation/provenance, codecs/payload and transactional history/reset.

VerticalDivider adds Material/order 60 as an ordinary const-capable leaf with all
six optional named arguments at ordinals 0..5: width, thickness, indent, endIndent,
color, radius. Creation is const VerticalDivider() with no explicit defaults.
Reuse existing finite non-negative geometry, literal/semantic colors and physical/
directional elliptical radius values plus their exact generated symbol provenance.
No parent-height or width/thickness cross-invariant is invented. Indents are top/
bottom, not leading/trailing. The SDK left-only rounded-hairline paint limitation
remains a render-time concern, not a source rejection or silent geometry coercion.
Codec/payload, all-field reset, save/reopen/further editing, Undo/Redo and rollback
reuse the current model and pair-save authority; formats stay 13/14/18.

Card adds Material/order 70 with 31 properties and optional child: eight direct
optional SDK scalars, required Designer variant and 22 shape rows. The three const
constructors Card/Card.filled/Card.outlined share one stable widget type. All ten
reviewed outlined shape constructors have their full finite typed parameters,
including physical/directional elliptical radii, complete BorderSide, oval/circle
eccentricity, LinearBorder edges and fractional star/polygon geometry. Other
ShapeBorders use analyzed project/package references, never opaque Dart expressions.

Shared schema helpers expose kind applicability and detail-first mode selection.
Strict validation rejects mixed or inactive shape fields and invalid rounding sums.
One atomic patch switches branches, removes incompatible details and preserves
compatible/unrelated fields, exact history and save/reopen/further editing. No
inactive data or synthetic SDK defaults are introduced. Canvas renders actual Card
variants with ambient themes or reports explicit unavailable custom-code/over-budget
path previews; the 4096-point operational budget does not constrain source/model.
Existing value encodings and persistence authority remain unchanged.

Badge admits all 41 fields and optional Label/Child slots at Material/order 80.
The complete 31-leaf TextStyle projection reuses typed theme/paint/typography/locale
values and exact generation; no raw expression fallback is introduced. Count
presence selects non-const Badge.count and makes Label insertion unavailable.
Max count requires Count; count mode rejects nonempty Label without deleting it.
Reset Count also resets Max count atomically, preserving shared fields and Child.
Prototype creation is equivalent to const Badge(), with empty optional slots
emitted as null. Full command/history, strict
payload, state-aware slot admission, property identity and save/reopen cases apply.
The existing staged-source guard still refuses a model-only change with identical
generated Dart while a paired revision is unsaved. This includes redundant explicit
false decoration leaves: Save/Undo first, then retry through the saved FD_ONLY path.

`CircleAvatar` follows at Material/order 90 with all nine optional properties and
one any-widget Child slot. No scalar creation defaults are stored. Both colors use
the shared literal/theme type; each optional image uses the existing provider
algebra and requires its own image-error callback to be absent when unset. Fixed
radius excludes min/max bounds. Each radius accepts finite nonnegative numbers or
the exact reviewed enum `double.infinity` from `dart:core`. Compare effective
double-precision diameters, preserving positive infinity and finite multiplication
overflow without inventing an extra scalar cap. Lower that exact constant to
`(1.0 / 0.0)` without adding a core import or changing user scope; no arbitrary
expression can enter this closed value. Generate exact named arguments,
normal const propagation and owner-library provenance; callback identities stay
out of the Canvas payload. Atomic UI normalization is separate from strict model
validation and never deletes Child. The existing formats remain sufficient.

The current practical backlog is 72/92 complete with 20
remaining. The catalog contains 72 widgets and 66 reviewed const definitions,
with 890 writable rows (873 outside Scaffold); Palette categories contain 31
Layout, three Scrolling, twenty-three Basic, nine Material and six Accessibility items.
The 72 sources and 61 insertable destinations form 4,392 cells, with 4,079
admitted and 313 rejected. The new ShapeBorderClipper value advances Catalog
API to 14, `.fd` to v13 and Canvas model to v18. NBFC framing/control/wire remain
v1.

`Container` exposes its complete reviewed non-widget constructor surface; its
optional single `child` remains a slot at constructor position 12 and is not
counted as a property row.

| `Container` property | Flutter argument | Reviewed contract |
| --- | --- | --- |
| `alignment` | `AlignmentGeometry?` | Physical or directional coordinates; omission uses Flutter layout behavior. |
| `padding` | `EdgeInsetsGeometry?` | Physical/directional, finite and non-negative. |
| `color` | `Color?` | Literal ARGB or reviewed `ColorScheme` role; mutually exclusive with `decoration`. |
| `isAntiAlias` | `bool?` | Omission preserves Flutter's `true` constructor default. |
| `decoration` | `Decoration?` | Reviewed `BoxDecoration`, including typed asset-only `DecorationImage`, behind the child. |
| `foregroundDecoration` | `Decoration?` | The same reviewed `BoxDecoration` domain in front of the child. |
| `width` | `double?` | Finite and non-negative. |
| `height` | `double?` | Finite and non-negative. |
| `constraints` | `BoxConstraints?` | Non-negative minima and finite-or-infinite maxima, with each maximum at least its minimum. |
| `margin` | `EdgeInsetsGeometry?` | Physical/directional, finite and non-negative, matching Flutter's constructor assertion. |
| `transform` | `Matrix4?` | Exactly 16 finite column-major entries. |
| `transformAlignment` | `AlignmentGeometry?` | Physical or directional transform origin. |
| `clipBehavior` | `Clip` | Closed enum; non-`none` requires `decoration`. |

The structured decoration covers literal/theme color, physical or directional
borders with exact side fields, physical or directional elliptical radii,
ordered stable-ID shadows, linear/radial/sweep gradients with ordered stable-ID
color stops, tile mode and optional rotation, background blend mode and shape.
Its model rejects Flutter paint hazards: blend without a fill, radius on a
circle, unsafe non-uniform border combinations, unordered/duplicate gradient
stops, invalid sweep angles, a negative focal radius or a nonzero focal radius
without a focal alignment. The paint-rect-dependent degenerate conical case is
not guessed from abstract alignments.

`BoxDecoration.image` uses the reusable top-level `ImageProviderValue` and
`DecorationImageValue` contracts. Providers are exactly `AssetImage` or
`ExactAssetImage` with a normalized relative POSIX asset name, optional Dart
package and exact positive scale only for the exact provider. Either may have at
most one `ResizeImage` with at least one `width`/`height` in `1..16384`, policy
`exact`/`fit` and `allowUpscaling`. File, memory, network and custom providers
remain deferred as user-selectable model choices. One reserved unqualified ASSET
identity represents a direct Image awaiting selection and cannot carry package,
scale or resize state. Canvas/generated Dart map it to reviewed built-in
placeholder bytes rather than resolving it as an asset path; the Canvas
runtime's internal `MemoryImage` otherwise projects host-verified bytes.

The complete pinned Flutter 3.44.8 `DecorationImage` argument set is `image`,
`onError`, `colorFilter`, `fit`, `alignment`, `centerSlice`, `repeat`,
`matchTextDirection`, `scale`, `opacity`, `filterQuality`, `invertColors` and
`isAntiAlias`. Its filter union is mode, exactly 20 matrix values,
linear-to-sRGB gamma, sRGB-to-linear gamma or saturation; mode accepts literal
and reviewed theme colors. A center slice must be non-negative with
`left < right` and `top < bottom`; fit may be omitted, `fill`, `contain`,
`fitWidth`, `fitHeight` or `scaleDown`, while `cover` and `none` are rejected.
`onError` stores only a validated identifier that must bind to a compatible
`void Function(Object, StackTrace?)`; neither raw Dart nor callback source is
accepted. Dart generation emits `image` immediately after `color`, uses
`AssetImage`/`ExactAssetImage` plus optional `ResizeImage`, and preserves
constructor/enumeration order. The saturation factory and callback tear-off keep
the enclosing expression non-const where required.

The project resolver reads app/package `pubspec.yaml` declarations and
`.dart_tool/package_config.json`, then snapshots only PNG/JPEG/GIF/WebP after
normalization, real-root/symlink, magic and dimension checks. It rejects
absolute paths, backslashes, dot/traversal segments and package-root escapes.
Inventory defaults are 4,096 logical assets, 16 MiB per file, 64 MiB total,
4,096 per dimension and 8,388,608 pixels. Variant choice matches Flutter 3.44.8
framework revision `058e0af2c2b57e369d905a03ac9748b0ebf543c6`: exact DPR
wins, an out-of-range DPR clamps to an endpoint, values below DPR 2.0 choose the
upper neighbor, and at DPR 2.0 or above only a value strictly above the midpoint
chooses upper (a midpoint tie chooses lower).

One Canvas presentation projects referenced assets only: at most 256 logical
assets and 256 resources, with 16 MiB total encoded image transport and each resource
at most 16,384 per dimension and 67,108,864 pixels. Its `resourceId` is the
lowercase raw SHA-256 of immutable compressed bytes. Canvas model v13 over NBFC
framing v1 binds capability `asset.imageBytes.v1` to `CONTROL` → `MODEL` →
`IMAGE` order; exact descriptor/payload digest, size and order are checked for
the same revision. No filesystem path or callback identifier crosses the
Canvas boundary. Native and internal exact-Web runtimes both create
`MemoryImage(bytes, scale: resolvedScale)`,
at most one `ResizeImage`, and the real `DecorationImage`. Missing, corrupt or
otherwise unavailable logical assets are isolated per resource and show a
deterministic non-interactive
placeholder/status naming the asset identity, code and reason while selection,
layout and drop overlays remain outside `Container`.

Properties provides transactional alignment presets/coordinates,
finite-or-unbounded constraint fields, a visual column-major 4×4 matrix with
identity/translate/scale/rotate helpers, and a tabbed decoration editor for fill,
image, border, radius, shadows and gradients. The Image tab chooses a declared
app/package asset and exposes typed provider, resize, callback, filter, fit,
alignment, center-slice, repeat, scale/opacity and rendering controls. Its
inventory status and individual controls have stable accessible names and
descriptions, including the positive-area and `fit` dependency. Optional
editors have an explicit default state, active table edits are included before
OK, and invalid drafts cannot publish. Top-level and nested colors, including
`ColorFilter.mode`, share the reviewed Material theme-role allowlist, so project
light/dark/custom themes flow through generated Dart and Canvas while explicit
literals remain local overrides. One accepted structured image edit and any
dependent transition use one atomic command and one Undo/Redo unit.
The native Canvas builds the real Container, keeps its IDE-owned selection/layout
frame outside the paint transform, draws distinct padding and margin guides and
retains an IDE-only selectable/drop target for an empty zero-size Container.
Editors cover single-line strings, newline-delimited font fallback lists,
accessible optional boolean checkboxes for every explicit `true`/`false` value
with `<not set>` retained as a separate Restore Default state, exact constrained
integer/double controls, reviewed enums, physical/directional non-negative edge insets,
ARGB/theme-aware color editors, structured Paint/Container editors and ordered
Shadow/OpenType/decoration tables.
Cross-property validation also requires `textBaseline` when
flex alignment is `baseline`, keeps `Text.semanticsIdentifier` unique in one
designer tree, rejects mutually exclusive color/Paint pairs and rejects a font
`package` without a family or fallback. `styleFontSize` is finite and
non-negative because Flutter passes it through `TextScaler.scale`.

The `.fd` model stores those 59 values as independently editable typed leaves;
each optional leaf can also be reset to omission.
Deterministic Dart generation groups them into optional `TextStyle`,
`StrutStyle`, `Locale.fromSubtags`, `TextScaler.linear` and
`TextHeightBehavior` constructor values; the isolated native Canvas performs
the same grouping into real Flutter objects. An absent group remains an omitted
constructor argument, so Properties, generated Dart and Canvas have the same
semantics.

Schema v2 adds closed structured values for Material theme tokens, the safe
serializable `Paint` subset, ordered `Shadow` values, OpenType features and
variable-font axes. Semantic `ColorScheme` and `TextTheme` roles follow the
active light, dark or custom project theme without coupling a form to a theme
definition id; literal ARGB values remain available. Missing list properties
mean inheritance/omission, while explicitly empty lists clear an inherited
list. Schema v3 distinguishes physical from directional edge insets. Schema v4
adds one closed typed nullable `IconData` value containing only a validated
Unicode scalar and font metadata. Schema v5 adds the four initial Container
structured kinds. Schema v6 adds top-level `IMAGE_PROVIDER` and the complete
optional `BoxDecoration.image` graph. Schema v7 additionally represents positive
infinity in all four BoxConstraints bounds as JSON `null`. Schema v8 adds one
closed atomic `Size` value with finite non-negative `width` and `height`.
Schema v9 adds one closed atomic `Offset` value with signed finite `dx` and
`dy`. Schema v10 adds the exact payload-free `null` property value used by
`IndexedStack.index`. Schema v11 adds the top-level typed physical or
directional finite non-negative elliptical border-radius value used by
`ClipRRect.borderRadius`. Schema v12 adds the closed Dart-object
reference used by `ClipRect.clipper`, `ClipOval.clipper` and `ClipRRect.clipper`: current library or canonical package-config-declared `package:`
URI, root identifier, optional member, and reference or zero-argument invocation.
Arbitrary Dart expressions are not used. Current schema v13 adds the closed
`shapeBorderClipper` union for six SDK shapes, retained radius and nullable explicit
textDirection. Schema-v1 through schema-v12 documents migrate in memory and are
written as canonical v13, referencing
[`fd-v13.schema.json`](../docs/flutter-designer/fd-v13.schema.json), only on their next admitted Designer edit; the frozen v1-v12
schema resources remain unchanged.

`Icon` is a leaf with no slots. Its 13 constructor properties preserve Flutter
theme behavior: omitted theme-backed fields inherit from `IconTheme`, while
explicit values override them identically in generated Dart and native Canvas.
`blendMode` and `fontWeight` are direct local arguments and do not inherit from
`IconTheme`; an explicit variable-font `weight` axis overrides `fontWeight` at
render time.
The built-in `icon` editor admits only **None** or one of 8,825 bundled Material
Icons locked to Flutter 3.44.8. Schema v4 keeps `IconData` generic and typed for
future reviewed definitions, but the current built-in exposes no custom metadata
entry. Material font glyphs require
`flutter.uses-material-design: true` in the application `pubspec.yaml`.

`Scaffold` exposes only its 17 reviewed scalar constructor fields. Runtime
controllers/builders, `key`, the `BoxDecoration` graph and widget-valued fields
which do not yet have an admitted catalog slot remain intentionally excluded.

The deprecated `Text.textScaleFactor` argument and `key` are not exposed;
`textScalerFactor` targets the current `textScaler` API. Arbitrary shaders,
image filters, ColorFilter forms outside DecorationImage's closed five and raw
Dart escape expressions remain excluded. Broader Palette expansion, exact-Web
product binding/acceptance and Linux/macOS native-surface providers remain
pending; the internal exact-Web runtime is already model-compatible.

Important current document semantics:

- an omitted property or slot means "omit the Dart constructor argument";
- an empty string is a real value, not omission;
- an explicitly present empty single slot means `null` and remains distinct
  from an omitted slot;
- catalog creation defaults are applied only when creating a new widget and
  are never injected while loading a document;
- optional positional constructor arguments must be present as one contiguous
  prefix; an explicitly present `null` slot still counts as present;
- exact JSON numbers remain in the model, while catalog validation rejects
  values that cannot be emitted portably as Dart integer/double literals;
- constructor and enum symbols carry an explicit exporting library URI;
  generator-selected import aliases are not persisted;
- asset paths are relative to the Flutter project/pubspec root, never to the
  `.fd` file, and are therefore not rebased by pair Move;
- supported-version `extensions` are location-independent opaque metadata and must not
  encode `.fd`-relative semantics;
- Dart expressions are opaque source values and are never evaluated here.

The source-integrity scanner accepts one independently bounded (2 MiB by
default) strict UTF-8 on-disk Dart
snapshot, retains its exact bytes, locates the exact `imports` and `build`
payload byte ranges, normalizes only line separators/trailing LF for hashing,
and reports stable `ON_DISK_DECLARED_MATCH`, `CONFLICT`, `UNSUPPORTED` or
`UNAVAILABLE` status. The current contract binds only an unqualified,
top-level stateless designer class and direct build scope; `{}`, `()` and `[]`
must balance, generic bounds cannot impersonate the class superclass, and
string-interpolation nesting is capped before scanning can exhaust the Java
stack. Stateful, import-prefixed and locally shadowed Flutter base types stay
explicitly unsupported. A declared on-disk match is read-only evidence. The
core can additionally compare it with bounded deterministic generator output
and rescan a fully reconstructed in-memory candidate. The NetBeans edge now
binds scanner/generator probes to analyzer evidence before live apply, then
binds the exact applied editor revision and both exact baselines for Save. That
persistence evidence, internal staged-replacement transaction and shared
candidate-capacity identity remain non-authorizing. The chronological native
Undo boundary and durable Pair/Source Save re-anchoring now exist, but public
Designer mutation remains gated by visual-surface work.

Successful generation additionally publishes a source-ordered occurrence
manifest for every emitted external Dart type. Each entry identifies exact
managed-region UTF-16 text, library URI and model origin; it does not contain a
filesystem trust root or authorize analyzer/navigation work. The NetBeans
adapter maps every `package:flutter` occurrence through the exact prospective
candidate region bytes and rejects any analyzer probe list that differs from
that complete manifest. The core scanner now also publishes the exact
outside-guard `StatelessWidget` superclass byte/UTF-16 occurrence for the same
retained source identity; the NetBeans edge requires its dedicated
`package:flutter/widgets.dart` class probe. The command, generator, probe
planner and analyzer now retain one exact shared candidate-capacity identity.
Third-party contributor symbol roots are still required before contributed
widgets can participate in writable commands; the built-in catalog's
pre-writable-UI runtime/release matrix now passes.

`DartSourceTransitionPlanner` handles the expected old-to-new drift after a
validated visual-model change. It requires the old on-disk three-way proof,
cross-checks its descriptor, validates the exact live marker-bearing bytes
against that descriptor, verifies successful internally consistent generation,
updates both prospective managed hashes, reverse-splices `build` then
`imports`, and scans the complete result again. It preserves all bytes outside
the payload ranges and emits either an immutable candidate or a stable
fail-closed diagnostic. Equal generated hashes produce `NO_CHANGES` and no
candidate, preventing a view/open operation from silently canonicalizing the
source. This first prospective writable contract accepts only strict UTF-8,
LF-only source without a BOM. A transition plan is still not write permission:
it contains neither a live NetBeans document revision, analyzer evidence,
prospective canonical `.fd` bytes nor lock-time baselines.

`DesignerPairPreparationPlanner` accepts only a ready source transition and
the exact original `.fd` snapshot. It re-decodes that baseline, proves the old
descriptor and stable `documentId`, canonicalizes the prospective document,
and requires deterministic decode/encode parity. Its immutable
`PreparedDesignerPair` carries defensive copies of the exact old/new Dart and
`.fd` bytes, but deliberately contains no live editor identity, analyzer proof,
filesystem lock or save authorization. The NetBeans adapter is solely
responsible for binding those remaining facts and committing the pair.

`DesignerPairCopyPlanner` accepts the exact bounded original `.fd`, its exact
current Dart basename, a canonical target Dart basename and a distinct stable
target document id. It rejects migrated/future/invalid models and substituted
source references. Its public immutable plan validates both exact non-migrated
snapshots and guarantees that the target differs only in `documentId` and
`source.dartFile`; class/generator metadata, managed hashes, Canvas preferences,
widget tree and stable ids, extensions and every other semantic remain equal.
Canonical encode plus exact decode/encode round-trip is mandatory. Mirrored
folder resolution, exact Dart-byte copying, collision allocation, coordinator
leasing, staged publication and rollback belong to the NetBeans adapter rather
than this pure module.

`DesignerPairMoveDependencyPlanner` accepts a canonical package name, distinct
old and target paths below `lib` with the same filename, and an exact bounded
project Dart-source inventory. Its scanner recognizes `import`, `export`,
`part` and URI `part of`. It rejects outgoing relative directives from the
moved source, incoming references to the old location, references that could
bind to the destination, unsupported URI forms, occupied targets, and
case-folded/NFC-equivalent path aliases. A successful immutable plan is only a
pure dependency proof; it contains no project owner, editor state, filesystem
lock or write authority. The NetBeans adapter must bind the inventory to exact
`pubspec.yaml` and `.dart_tool/package_config.json` ownership, reject nested
packages, project-`lib` aliases, links and unsaved editors, and repeat the proof
at the mutation boundary.

At that adapter edge, a private one-shot `NodeTransfer.CLIPBOARD_CUT` paste keeps
the pair in the same Flutter project, requires both already existing mirrored
destination folders and preserves the basename plus exact Dart/`.fd` bytes.
Generic DataObject Move stays disabled. The adapter closes the clean source
editor, repeats the proof and commits on the EDT under exact NetBeans 30
MasterFS proof-file locks and child-cache mutexes for all relevant/ancestor
folders, publishes `.fd` before Dart, and holds exact target locks through
reversible `.nbmove` source retirement. Rollback restores sources before
removing owned targets and retains verified recovery targets if source
recreation cannot be proved. The adapter fails closed if the exact NetBeans 30
MasterFS admission mechanism is unavailable. This guarantee is in-process only,
without a durable crash journal; an external non-NetBeans writer remains a
residual race.

At the NetBeans edge, one `PairSaveCoordinator` owns the stable pair-aware
`SaveCookie` and acquires a mandatory lease before analysis. A unique one-shot
ticket analyzes exact candidate C while the live editor remains at predecessor
B. Its Dart path comes only from the coordinator-bound file and its trust root
only from the configured Flutter SDK. Only passing ticket evidence can enter one EDT predecessor compare-and-set,
candidate apply and separate applied-live binding; rejection/cancellation does
not change the document revision or Undo state. Pair Save then commits through
the exact two-file transaction. A failure after an observed apply restores B
under one document-atomic lease-release barrier and keeps it dirty with the
stable `SaveCookie`; it never reloads over a queued user edit.
The Source and Design MultiView elements share one stable combined Undo/Redo
identity backed by the native NetBeans editor manager. Internal apply, restore,
verification and Undo-barrier changes are coalesced at that bridge and exposed
only after the document lock is released; a callback cannot re-enter a new
Designer document transaction or invalidate completed work. Each Undo/Redo
action pins one delegate; binding is rejected and binding close waits while an
action or internal document barrier owns that history.

The command adapter first derives a prospective C2 under an identity-bound
pending lease while retaining the exact C1 cursor and redo branch. The lease
binds the exact before/after revisions, edit and catalog identity, blocks a
second command, Undo/Redo and durable Save, and can only be adopted after a
separate exact staged replacement; abort retains C1 and invalidation closes an
uncertain session. Adoption separates the monitor-only exact cursor swap from
one-shot callback publication so the owner can release document/coordinator
locks first. It performs no analyzer, editor or persistence operation.
The adapter additionally pins an exact dirty cursor in a durable lease. For
`FD_ONLY`, the coordinator locks and verifies both file baselines, writes only
`.fd`, requires the exact loaded widget-catalog identity and complete writable
validation/source/three-way facts, proves the Dart document
identity/version/bytes stayed untouched, then re-anchors the session. Failure
before a durable change remains retryable; stale or uncertain outcomes freeze
the session. The coordinator now claims that pending lease, proves the exact
staged C1/durable-B identities, analyzes C2 before applying it, and jointly
publishes the applied document, fresh staged evidence and C2 command cursor.
Rejection keeps exact C1; apply failure restores and rebinds fresh C1 evidence;
an unprovable external/user-edit race preserves content and invalidates the
session. The exact shared candidate-capacity policy now travels by identity
from command generation through the generated manifest, revision, analyzer
ticket/request and analyzer limits, so an accepted command cannot first exceed
the byte/probe budget at staging.

The command core also supports durable Source re-anchoring of saved semantic
history. It scans the exact candidate `S2`, requires both managed payloads to
remain byte-for-byte equal, and rebuilds every retained historical revision and
edit proof against `S2` while preserving stable logical revision ids and exact
candidate bytes. Equal normalized hashes are not enough. At the NetBeans edge,
an identity-bound `SourceAnchorLease` pins that operation and blocks commands,
Undo/Redo and competing saves. The saved semantic endpoint keeps native `C1`
bytes underneath the Source entry even though its command/durable baseline is
`S2`, preserving the exact `S2→C1→B→C1→S2` chronology. Controller, command and
Pair authority adopt jointly; any failed outcome after CES entry or committed
split-authority risk invalidates semantic history into sticky conflict without
discarding native Source Undo or reloading user content.

A later Pair Save keeps semantic and unmanaged Source coordinates separate:
`(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and full Redo. Endpoint-specific proofs use
scanner-owned byte offsets to project the new durable managed payloads into each
historical unmanaged envelope before deriving its semantic candidate. Unique
physical variants are bounded together. A Source `UNCHANGED` result refreshes
only the clean CES cursor and preserves exact command/Current/edge identities;
trimming the last semantic edge still leaves its owner available for the next
exact Source re-anchor. A subsequent Source Save projects its new durable anchor
through every retained physical variant; byte accounting fails incrementally
before persistence, and a closed zero-edge owner is retired only under exact
saved-baseline evidence without discarding native Undo history.

Command admission now consumes those physical variants directly. The
coordinator captures an opaque staged command-source token for the exact
logical owner, `SavedHistoryProof`, physical cursor/live identity and epochs.
At `C1/S0`, its `PreparedDesignerPair` provides the durable managed payloads in
the exact S0 envelope; the pending command therefore derives `C3/S0` and pins
that pair identity without moving logical C1 before joint adoption. Replacement
and recovery use the common staged-proof contract, so a saved-history
predecessor never fabricates analyzer evidence. Aggregate physical byte
accounting runs before analyzer or document mutation. Adoption preserves
durable C2/S2, replaces the obsolete redo suffix with the exact
`B/S0→C1/S0→C3/S0` branch, and carries S0 into the following ordinary command.

These command and pair-save paths originally served only the bounded typed
Properties UI; at that historical stage Palette insertion/DnD was still
disconnected. That stage is superseded by the seventy-two-source insertion matrix
described above. Pre-persistence loss of exact
staged authority now clears only semantic Designer state while retaining live
Source content and native Undo/Redo. The assembled Apache NetBeans IDE 31 runtime,
strict NBM verifier and isolated install lifecycle have an established passing
baseline; this does not imply physical acceptance of every new palette widget.
The global physical gate remains deferred until the palette target is complete.
The accepted ADR-021
Windows native read-only `FlutterView`, seventy-two-widget projection, responsive
profiles, stable-ID tree selection, exact seventy-two-item context Palette and
selected-node typed Properties are implemented. Properties expose exactly
890 catalog-backed writable fields across `Scaffold`, `ElevatedButton`,
`AppBar`, `TextField`, `Column`, `Row`, `Padding`, `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`,
`ListView`, `GridView.count`, `SingleChildScrollView`, `Wrap`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge`, `CircleAvatar`, `Container`, `Opacity`, `Text`, `Icon` and `Image`,
including the Scaffold, ElevatedButton, AppBar, Text and Icon projections
above.
Palette DnD is enabled for the seventy-two exact capability-reviewed source
definitions and sixty-one insertable catalog-authorized slots, for 4,392
candidate cells: 4,079 admitted and 313 rejected. Expanded and Flexible each
admit only direct `Row.children` or `Column.children` wrapping, while Spacer is
inserted only into those two slots; the wrappers' required child slots are
replacement-only and excluded from the destination matrix. SafeArea,
Directionality, ExcludeFocus, ExcludeFocusTraversal, Visibility, TickerMode, DefaultTextHeightBehavior, DefaultSelectionStyle and IconTheme use the same atomic generic wrapper command for any existing
non-ParentData target; their required child slots are likewise excluded.
Palette/tree can wrap root and
non-root widgets, while the current Canvas target wire intentionally exposes
only non-root children.
Exact-Web product routing/assembled acceptance, cross-platform
providers and the broader unreviewed widget contracts remain outstanding; the
internal exact-Web runtime already has image-model parity. The native-engine
Web responsive layout preview is the currently routed choice.

The `.fd` document codec accepts strict UTF-8 JSON (with an optional input BOM), rejects
duplicates and trailing content, and keeps the exact bounded input snapshot.
Current version 13 data maps directly to the domain model; versions 1 through 12
migrate in memory without an open-time write, and a completely parsed newer version
remains raw/read-only and cannot be down-saved. Canonical output is
UTF-8 without BOM, two-space/LF formatted, has one final LF, fixed core-field
order and lexically sorted dynamic keys. `$schema` is never fetched.

Catalog contributors use a reverse-DNS id, target catalog API version 14, and
own only widget type ids below `<contributorId>.`. Composition is atomic per
contributor: invalid metadata never partially enters the effective catalog.
Every effective Palette category id also has one stable category order;
conflicting contributors are rejected before type resolution. API-1 through
API-13 contributors are rejected explicitly because successive exported
contracts changed incompatibly for directional edge insets, typed `IconData`,
the initial four Container structured kinds, the reusable `IMAGE_PROVIDER`
kind, finite-or-positive-infinity BoxConstraints bounds, atomic Size, atomic
signed Offset, SafeArea's physical-only
`EdgeInsetsValues.directionalAllowed` constraint, the exact payload-free null
property value, the top-level typed border-radius geometry, the analyzed
typed Dart-object reference used by custom clippers and PhysicalModel's
physical-only `BorderRadiusValues.directionalAllowed` constraint, followed by
PhysicalShape's typed ShapeBorderClipper value and constraint.

The accepted contract and module boundaries are documented in
`docs/FLUTTER_DESIGNER_ARCHITECTURE.md`.
