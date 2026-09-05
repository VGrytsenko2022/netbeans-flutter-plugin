# Getting Started

## 1. Install prerequisites

Install JDK 21+, Apache NetBeans IDE 31, Maven 3.9+ and a current Flutter SDK.
The current development runtime is NetBeans IDE 31; the plugin intentionally
continues to compile against the `RELEASE300` API baseline.

Verify:

```bash
java -version
mvn -version
flutter --version
flutter doctor
```

## 2. Open the root Maven project

Open the root directory in NetBeans. The reactor contains all modules.

## 3. Configure Flutter and Dart SDKs

On the first plugin start, Flutter is imported from the first valid source in this order:

1. JVM property `flutter.sdk`
2. environment variable `FLUTTER_HOME`
3. environment variable `FLUTTER_ROOT`
4. `flutter` executable on `PATH`

For Dart, explicit configuration wins in this order: `dart.sdk`, `DART_HOME`, and `DART_SDK`. The plugin then uses the SDK bundled with Flutter when available and finally checks `PATH`.

The detected configuration is saved in the NetBeans user directory. To inspect, change, or validate it, open `Tools > Options > Flutter`. Invalid explicit paths are reported with the exact missing executable and are not silently replaced by another SDK.

To pass a Flutter SDK only to the development IDE, use:

```powershell
mvn nbm:run-ide -Dnetbeans.installation=G:/netbeans -Dnetbeans.run.params=-J-Dflutter.sdk=C:/dev/flutter
```

If no SDK is found automatically, leave the automatic fields blank or choose the Flutter and Dart root folders manually in the Options page.

## 4. Create or open an application

Use `File > New Project > Flutter > Flutter Application` to run `flutter create --template app` with the configured SDK. The wizard validates the package name, organization, location and exact target directory before generation.

Use `File > Open Project` to open an existing Flutter directory. A directory with a valid `pubspec.yaml`, a Flutter declaration and a `lib` folder is recognized as a native Flutter project.

To add a class, select its destination folder in the Flutter project and use `File > New File > Dart > Dart Class`. Enter an UpperCamelCase name; for example, `OrderRepository` creates `order_repository.dart` with a const class skeleton and opens it in the Dart editor. The wizard blocks duplicate files, generated folders and locations outside the project.

To add a visual form, select `lib` or one of its subfolders and use `File > New File > Flutter Designer > Flutter Designer Form`. NetBeans creates the Dart source under `lib/<relative>` and its mirrored JSON model under `.fd_templates/<relative>`. Rename either member through the standard node action to rename the complete pair. The new basename must use canonical lower_snake_case; Rename updates only the model's `source.dartFile`, while the Dart bytes and `source.className` remain unchanged. Save or close unsaved edits first.

The Design toolbar offers only exact previews compatible with the project's generated platform folders: Android enables Android Phone and Android Tablet; iOS enables iPhone and iPad; Windows, macOS and Linux each enable their named Desktop target; and `web` enables Web. Adding or removing a platform updates every open Design tab without reopening the file. NetBeans retains the exact target, then the same viewport mode, and otherwise selects the first canonical choice. Android/iOS/macOS/Linux use Flutter adaptive appearance inside the native Windows Canvas; they are not device or emulator runtimes. Web renders a browser-sized responsive layout in that native Canvas. It is a design-time layout preview and does not emulate `kIsWeb`, browser fonts, DOM, plugins or platform channels. If the project has no real platform directory, Preview is disabled.

The current capability-gated Palette and native Canvas admit exactly fifty-three
widgets: `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`,
`Wrap`, `Padding`, `Center`, `SizedBox`, `AspectRatio`, `Container`, `Opacity`,
`Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`,
`LimitedBox`, `OverflowBox`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`,
`Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `ListView`, `GridView.count`, `SingleChildScrollView`, `Text`, `Icon`, `Image`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape` and `ExcludeSemantics`.
Forty-seven definitions use reviewed const constructors. Their `General`
Properties expose exactly 759 typed writable rows: 742 across the fifty-two
non-`Scaffold` definitions and 17 closed scalar `Scaffold` fields. `Icon` is a
leaf and exposes all 13 reviewed
constructor properties; its Icon data editor admits **None** or searches 8,825
bundled Material Icons locked to Flutter 3.44.8. The Properties value and every
search-result row include the real glyph preview beside the readable `Icons.*`
name. Preview loading is asynchronous and accepts only the exact
manifest-verified Material font in the currently resolved Flutter SDK; when that font
is unavailable, a neutral placeholder is shown and the text selector remains
fully usable. Keep
`flutter.uses-material-design: true` in `pubspec.yaml` when using those Material
glyphs. Omitted theme-backed properties inherit the active `IconTheme`, while
`blendMode` and `fontWeight` remain local; the native Canvas previews the same
typed values emitted by generated Dart.

`AspectRatio` exposes its complete scalar constructor contract: a required,
finite `aspectRatio > 0` value and one optional `child` slot. A newly inserted
instance starts at `1.0`; the value is local layout data and does not inherit
from the project theme.

`Opacity` exposes the pinned const Flutter 3.44.8 constructor contract: required
finite `opacity` in inclusive `[0, 1]`, optional
`alwaysIncludeSemantics` with omitted default `false`, and one optional
any-widget `child`; `key` is excluded. A newly inserted Opacity stores
`opacity: 1.0` and an empty child. At opacity zero the real Flutter widget still
hit-tests its child, normally removes the child's semantics, and retains them
only when **Always include semantics** is true. Canvas selection, hit and drop
overlays stay visible outside the effect, including for an empty zero-size
target. Opacity does not inherit Theme or Directionality values.

`Align` exposes optional physical or directional alignment, optional finite
non-negative width/height factors and one optional any-widget `child`. A new
instance keeps all properties omitted, so Flutter supplies `Alignment.center`
and null factors. Null is not the same as factor `1`: on a bounded axis null
expands, while a factor shrink-wraps to the child's size. Directional horizontal
coordinates flip under LTR/RTL, physical coordinates do not, and values outside
`[-1, 1]` intentionally extrapolate. An empty zero-size Align remains selectable
and accepts a drop through an IDE-only target that does not alter generated Dart.

`FractionallySizedBox` exposes the same optional alignment domain plus optional
finite non-negative width and height factors and one optional child. `Stack`
exposes alignment, text direction, fit, clip behavior and an ordered list of
non-positioned children. `Expanded` and `Flexible` are required-child wrappers;
drop either on an existing direct Row/Column child to wrap that child atomically.
They are unavailable as terminal Add operations, cannot wrap either wrapper
type or Spacer, and their child editors support replacement but not add, remove
or clear. `Spacer` is a childless insertion-only leaf available only in direct
`Row.children` or `Column.children`; it never wraps another widget.
`Flexible` additionally exposes optional non-negative `flex` and optional
`FlexFit.loose`/`tight`; omission preserves `flex: 1` and loose fit.

`Image` is a leaf with a required asset-only provider and 21 optional reviewed
fields. Add chooses the deterministic first sorted declared asset when one is
available. If the inventory is empty, refreshing, verifying or unavailable, the
Palette source remains draggable: dropping it into a compatible slot creates an
editable placeholder and the `image` property reads `<choose asset>`. Canvas and
generated Dart use a built-in placeholder, so the document stays renderable and
valid across Save/reopen. To replace it, add a PNG/JPEG/GIF/WebP file such as
`assets/example.png`, list it in the existing `flutter:` block in
`pubspec.yaml`, then open **Image data** and select it:

```yaml
flutter:
  uses-material-design: true
  assets:
    - assets/example.png
```

Android launcher PNGs, Apple `Assets.xcassets` entries and `web/icons/...` do
not become Flutter runtime assets for `Image` merely by existing in the project.
The planner checks the latest inventory again at commit and either chooses its
first current asset or keeps the placeholder. Its four center-slice coordinates
must be supplied together, form a strict rectangle and cannot be combined with
`BoxFit.cover` or `BoxFit.none`.

`TextField` is a Material leaf with 54 optional Properties across Input, Layout,
Behavior, Cursor and selection, Callbacks and Restoration. Creation opens no
dialog and stores no constructor defaults. Runtime typed text, selection,
controller state and focus state are not stored by Designer. Editing either
unset cursor-radius axis seeds both axes from that value; editing any unset
scroll-padding edge seeds all four edges. Restore Default removes the complete
compound value. Generated Dart and Canvas keep direct Row/Column placement safe
with a `LayoutBuilder`/`SizedBox`: unbounded width receives 240 logical pixels,
and `expands: true` under unbounded height receives 120.

`ListView` is the non-const static `ListView(children: ...)` slice. Its 17
optional rows cover scrolling (5), layout (4), caching and children (4),
semantics (2), and restoration (2); its ordered `children` list is managed in
the `Slots` tab. Closed presets cover axis, six physics choices, drag start,
keyboard dismissal, clipping and hit testing. Padding and item/cache extents are
non-negative, `semanticChildCount` cannot exceed the current child count, and a
numeric cache extent is emitted as `ScrollCacheExtent.pixels`. Controller-owned
state, builders/delegates, `itemExtentBuilder`, `prototypeItem`, deprecated
`cacheExtent`, `key` and raw Dart are not admitted. Generated Dart and Canvas use
a real ListView, including horizontal/vertical, reverse and LTR/RTL behavior.
Their constraint guard supplies width 240 or height 120 for an unbounded
viewport cross axis, and for an unbounded main axis only when `shrinkWrap` is
false.
The originally agreed eight-item core Palette—`Container`, `Row`, `Column`,
`Text`, `Image`, Button through `ElevatedButton`, `TextField` and `ListView`—is
complete 8/8. This is completion of that agreed list, not of every Flutter
widget.

`Wrap` is the first Palette slice after that core milestone. It exposes all
nine non-`key` constructor properties: direction, child/run alignment, finite
signed spacing and run spacing, cross-axis alignment, text/vertical direction
and clipping. Its ordered `children` list appears in `Slots`. Generated Dart and
Canvas build the real Flutter Wrap. An empty Wrap retains a 36-pixel Designer
selection target, and Palette/tree drops on either an empty or populated Wrap
append at `children.length` through the complete visible Wrap rectangle.

`FittedBox` is the second post-core Palette slice. It exposes optional `fit`,
physical/directional `alignment`, `clipBehavior` and one optional single
any-widget `child`. Leaving them unset preserves `BoxFit.contain`, centered
alignment and no clipping. The fit chooser contains all seven `BoxFit` values,
and the clip chooser contains all four reviewed `Clip` values. Generated Dart
and Canvas build the real Flutter FittedBox, so scaling and clipping match the
framework and directional alignment flips under LTR/RTL. An empty zero-size
instance retains a 36-pixel Designer selection/drop target.

`ConstrainedBox` is the third post-core Palette slice. Its required
`constraints` row cannot be left unset; a new instance starts with `0..∞` for
both width and height. Open its structured editor to select finite, unbounded
or expanding axes. Finite bounds must be non-negative and each minimum must not
exceed its maximum; an expanding axis is `∞..∞`. The optional `child` is edited
through Slots. Generated Dart and Canvas build the real Flutter ConstrainedBox,
  and an empty zero-size instance keeps a bounded Designer selection/drop target.

[`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
is the fourth post-core Palette slice, at Layout order 107 immediately after
ConstrainedBox. Its four optional Properties are `textDirection`, `alignment`,
`constrainedAxis` and `clipBehavior`; its optional `child` is edited through
Slots. New instances store no property defaults. Leaving values unset preserves
centered alignment, no retained constrained axis and `Clip.none`, and ambient
`Directionality` resolves directional alignment when `textDirection` is unset.
Generated Dart and Canvas build the real Flutter UnconstrainedBox. An empty
zero-size instance keeps a bounded Designer selection/drop target. At that
milestone the practical Material/Base backlog was 24/92 complete with 68
remaining, and Layout contained 16 items.

[`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
is the fifth post-core Palette slice, at Layout order 108. Its optional
`maxWidth` and `maxHeight` Properties accept finite non-negative values; leaving
either unset canonically preserves Flutter's `double.infinity` default. Its
optional `child` is edited through Slots. Generated Dart and Canvas build the
real Flutter LimitedBox, so a configured maximum affects an axis only when its
incoming maximum constraint is unbounded. At that milestone the practical
Material/Base backlog was 25/92 complete with 67 remaining, and Layout contained
17 items.

[`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
is the sixth post-core Palette slice, at Layout order 109. Its optional
`alignment`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight` and `fit`
Properties are followed by an optional `child` in Slots. The four override
bounds accept finite non-negative doubles; each unset value inherits that bound
from the parent, and a present minimum cannot exceed its matching maximum.
Leaving alignment and fit unset preserves `Alignment.center` and
`OverflowBoxFit.max`; `deferToChild` is also available. Explicit non-finite
overrides are outside this bounded contract. Generated Dart and Canvas build
the real Flutter OverflowBox, preserving overflow, physical/directional
alignment and both fit modes. At that milestone the practical Material/Base
backlog was 26/92 complete with 66 remaining, and Layout contained 18 items.

[`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html) is
the seventh post-core Palette slice, at Layout order 130 immediately after
Expanded. Its optional `flex` accepts portable non-negative integers, including
zero; optional `fit` accepts only `FlexFit.loose` or `FlexFit.tight`; and its
single any-widget `child` is required. Leaving `flex` and `fit` unset preserves
Flutter's defaults of `1` and `loose`. Create it by dropping Flexible on an
existing direct `Row.children` or `Column.children` child. Palette, tree and
Canvas use one atomic wrap command, never an empty terminal prototype. Flexible
and Expanded cannot wrap either wrapper type because the inner ParentDataWidget
would no longer be a direct Flex child. The occupied child is replacement-only,
cannot be cleared and is not an insertable DnD destination. Generated Dart and
Canvas build the real Flutter Flexible; zero flex is inflexible, positive loose
flex may remain smaller than its allocation, and positive tight flex fills it.
At that milestone the practical Material/Base backlog was 27/92 complete with
65 remaining, and Layout contained 19 items.

[`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html) is the
eighth post-core Palette slice, at Layout order 140 immediately after Flexible.
Its optional `flex` accepts positive portable integers; leaving it unset
preserves Flutter's default of `1`, while zero, negative and over-limit values
are rejected. Spacer has no slots. Create it only by inserting it directly into
`Row.children` or `Column.children`; Palette, tree and Canvas use ordinary
insertion rather than wrapping an existing child. Expanded and Flexible cannot
wrap Spacer because Spacer's internal Expanded parent-data path must remain
directly below Row or Column. Generated Dart and Canvas build the real Flutter
Spacer directly under the Flex, while Canvas selection and outlines come from
the surface overlay. At that milestone the practical Material/Base backlog was
28/92 complete with 64 remaining, and Layout contained 20 items.

[`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html) is
the ninth post-core Palette slice, at Layout order 150 immediately after
Spacer. Set its required finite-double `baseline` and required
`TextBaseline.alphabetic`/`ideographic` `baselineType`; its `child` is optional.
Flutter supplies no defaults, so a new Designer Baseline starts with
`baseline: 24.0` and `baselineType: TextBaseline.alphabetic`. Both values remain
editable, but neither required argument can be restored to `<not set>`. Add,
replace or clear the optional child through the ordinary single-child slot.
Generated Dart and Canvas use the real Flutter Baseline; an empty node retains
framework `constraints.smallest` layout (often zero) and receives only the
Designer selection/drop target. The
practical Material/Base backlog was then 29/92 complete with 63 remaining. It is
a planning backlog rather than a normative full Flutter widget list. Layout
contained 21 items.

[`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
is the tenth post-core Palette slice, at Layout order 160 immediately after
Baseline. It has no writable values: add, replace or clear its optional
single-child slot. Generated Dart and Canvas construct the real Flutter
IntrinsicHeight, so the child is measured through Flutter's speculative
intrinsic-height pass while the parent's constraints remain authoritative.
This pass is relatively expensive and can be O(N²) in tree depth; the warning
is shown in the Palette and slot descriptions. An empty or collapsed node
retains a bounded Designer selection/drop target without changing Flutter
layout. The practical Material/Base backlog was then 30/92 complete with 62
remaining. It is a planning backlog rather than a normative full Flutter widget
list. Layout contained 22 items.

[`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
is the eleventh post-core Palette slice, at Layout order 170 immediately after
IntrinsicHeight. Its optional `stepWidth` and `stepHeight` values accept finite
non-negative doubles or `<not set>`, and its optional child uses the ordinary
single-child slot editor. Null and explicit zero are saved distinctly; Flutter
treats either as no snapping on that axis, while a positive value rounds the
child's corresponding intrinsic extent up to a multiple of the step. Generated
Dart and Canvas construct the real IntrinsicWidth while parent constraints
remain authoritative. Palette and Properties show the relatively expensive
speculative-layout and worst-case O(N²) warning. An empty or collapsed node
retains a bounded Designer selection/drop target without changing Flutter
layout. The practical Material/Base backlog was then 31/92 complete with 61
remaining. It is a planning backlog rather than a normative full Flutter widget
list. Layout contained 23 items.

[`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html) is
the twelfth post-core Palette slice, at Layout order 180 immediately after
IntrinsicWidth. Its optional `offstage` value accepts `<not set>`, `true` or
`false`, and its optional child uses the ordinary single-child slot editor.
Omission and explicit `true` are saved distinctly even though both hide the
child; explicit `false` restores ordinary participation. Generated Dart and
Canvas construct the real Offstage. While hidden, Flutter still lays the child
out and keeps it active and focusable, including running animations, but omits
painting, hit testing and semantics and normally contributes no parent space.
For long-term hiding, remove the subtree when that background activity would
waste resources. A real zero-sized result retains a bounded 36x36 Designer
selection/drop target outside the Offstage effect without changing layout. The
practical Material/Base backlog was then 32/92 complete with 60 remaining.
Layout contained 24 items.

[`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
is the thirteenth post-core Palette slice, at Layout order 190 immediately after
Offstage. Its required `size` is one structured value with finite non-negative
width and height; a new prototype starts at `Size(100, 100)`. Its optional
physical/directional `alignment` preserves the framework default
`Alignment.center`, and its optional child uses the ordinary single-child slot
editor. Generated Dart and Canvas construct the real SizedOverflowBox: the
parent constrains the widget's requested size, but the child receives the
original incoming constraints and can paint outside the resulting box according
to alignment. Hit testing remains bounded by the parent box. A true zero-size
result retains only the bounded 36x36 Designer selection/drop target. The
practical Material/Base backlog was then 33/92 complete with 59 remaining, and
Layout contained 25 items.

[`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
is the fourteenth post-core Palette slice, at Layout order 200 immediately after
SizedOverflowBox. Its required `transform` is a finite column-major 4x4 Matrix4
and new prototypes start at `Matrix4.identity()`. Optional atomic signed finite
`origin`, physical/directional `alignment`, `transformHitTests` and
`FilterQuality.none/low/medium/high` values preserve their null/null/true/null
Flutter defaults when omitted; `child` uses the ordinary optional single-child
slot editor. Generated Dart and Canvas construct the real paint-time Transform,
so the child keeps its layout size while paint and, when enabled, hit testing
follow the matrix. A true zero-size node retains only the bounded 36x36 Designer
selection/drop target. The named rotate/translate/scale/flip constructors are
outside this slice; equivalent matrices can be entered through `Transform.new`.
The practical Material/Base backlog is now 34/92 complete with 58 remaining,
and Layout contains 26 items.

[`RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
is the fifteenth post-core Palette slice, at Layout order 210 immediately after
Transform. Its required `quarterTurns` accepts the exact signed native/Web range
from `-9007199254740991` through `9007199254740991`; new nodes start at `1`.
Its optional `child` uses the standard single-child slot editor. Unlike the
paint-time Transform, Flutter applies RotatedBox before layout, so odd turns
swap the child's width and height constraints while even turns retain them.
Generated Dart and both Canvas projections build the real widget, preserve the
stored signed integer and paint its equivalent modulo-four rotation. The
practical Material/Base backlog is now 35/92 complete with 57 remaining, and
Layout contains 27 items.

[`ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html) is
the sixteenth post-core Palette slice, at Layout order 220 immediately after
RotatedBox. Its optional **Main axis** and **Reverse** Properties preserve
Flutter's `Axis.vertical` and `false` defaults while left as `<not set>`, and its
ordered `children` slot starts empty. Drop children on its tree row or Canvas
target; an empty node accepts index zero, while a populated node appends at the
visual end selected by its axis, reversal and current text direction. Generated
Dart contains the real bare `ListBody`. The Canvas alone supplies an
axis-matched design-time viewport so the real widget receives the unbounded
main-axis and bounded cross-axis constraints required by `RenderListBody`; that
guard is not saved into the form or application source. The practical
Material/Base backlog is now 36/92 complete with 56 remaining, and Layout
contains 28 items.

[`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
is the seventeenth post-core Palette slice, at Layout order 230 immediately
after ListBody. Its optional **Spacing**, **Alignment**, **Overflow spacing**,
**Overflow alignment**, **Overflow direction** and **Text direction** Properties
remain `<not set>` on a new node and therefore preserve Flutter's exact
constructor defaults. Its ordered `children` slot starts empty. A fitting bar
places children horizontally in effective LTR/RTL order; when their widths plus
spacing exceed the available width, Flutter places them in a vertical column
ordered by **Overflow direction**. Generated Dart contains the real bare
`OverflowBar`. For a nonempty node, the Canvas alone bounds an otherwise
unbounded preview width when **Alignment** is not `<not set>`; the default null
alignment retains Flutter's natural width. It also retains a 36x36 empty target;
those guards are not saved into the form or application source. The practical Material/Base backlog is now 37/92 complete
with 55 remaining, and Layout contains 29 items.

[`GridView.count`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
is the eighteenth post-core Palette slice, at Scrolling order 20 immediately
after ListView. A new node contains required **Cross-axis count** `2` and an
empty ordered `children` slot. Its other 20 rows remain `<not set>` and preserve
Flutter's vertical direction, forward order, inferred primary/controller and
physics behavior, unit tile ratio, zero spacing, normal clipping, semantics and
restoration defaults. Use **Main-axis extent** to request a fixed tile extent;
when present it takes precedence over the extent derived from **Child aspect
ratio**. Use the same `children` slot editor to add or reorder tiles in exact
source, paint and semantic order. Controller-owned state, builders/delegates,
the other named constructors, `scrollBehavior`, `key` and deprecated raw
`cacheExtent` are outside this static slice. Generated Dart and Canvas apply the
same 240-wide/120-high guard only where unbounded constraints require it. At the
`GridView.count` milestone, the practical Material/Base backlog was 38/92
complete with 54 remaining; Layout had 29 items and Scrolling had 2.

[`SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
is the nineteenth post-core Palette slice, at Scrolling order 30 immediately
after `GridView.count`. A new node has an empty optional `child` and all 10
Properties rows remain `<not set>`, preserving Flutter's exact vertical,
forward, unpadded, inferred primary/physics, start-drag, hard-edge clip, opaque
hit-test, null restoration and inherited keyboard-dismiss defaults. The rows
are **Scroll direction**, **Reverse**, **Padding**, **Primary**, **Scroll
physics**, **Drag start behavior**, **Clip behavior**, **Hit-test behavior**,
**Restoration ID** and **Keyboard dismissal**. Controller-owned state and
arbitrary physics graphs are outside the slice. Generated Dart and Canvas use
the real widget without the ListView/GridView bounded-viewport guard because
SingleChildScrollView intentionally shrink-wraps in both axes. Canvas alone
retains a 36x36 drop/selection target when an empty or zero-size widget has no
usable rendered area. The practical Material/Base backlog is now 39/92
complete with 53 remaining; Layout remains at 29 items and Scrolling contains
3.

[`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is the twentieth post-core Palette slice, at Basic order 40 after `Image`. A new
node starts with required **Color** `0xFF2196F3`, optional **Anti-alias** left
`<not set>` so Flutter keeps its `true` default, and an empty optional `child`.
Choose either an exact ARGB value or a reviewed Material `ColorScheme` role for
the color. A literal generates a const `ColoredBox`; a theme role generates
`Theme.of(context).colorScheme...` and therefore a non-const widget. The
required color cannot be restored to omission. Explicit anti-alias true/false
uses the centered checkbox; **Restore Default** returns it to `<not set>`.
Generated Dart and Canvas use the real widget. Canvas alone retains a 36x36
selection/drop target when the empty box has zero size, and that overlay is not
saved. At the `ColoredBox` milestone, the practical Material/Base backlog was
40/92 complete with 52 remaining; Layout had 29 items, Scrolling 3 and Basic 4.

[`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
is the twenty-first post-core Palette slice, at Layout order 240 after
`OverflowBar`. All six properties start as `<not set>`: **Left**, **Top**,
**Right**, **Bottom**, **Minimum** and **Maintain bottom view padding**. Omission
preserves Flutter's `true`, `EdgeInsets.zero` and `false` defaults. **Minimum**
accepts signed finite physical `EdgeInsets`; direction-aware
`EdgeInsetsDirectional` is not accepted by this constructor. SafeArea has a
required child, so it is created only by wrapping an existing widget atomically,
never by adding an empty prototype. Drop it on a widget-tree row to wrap either
the root or a non-root subtree. The current Canvas route exposes SafeArea only
over non-root children and intentionally has no root drop target; use the tree
for root wrapping. Expanded, Flexible and Spacer cannot be wrapped because they
must remain direct Row/Column children. At that milestone the practical backlog
was 41/92 complete with 51 remaining; Layout contained 30 items, Scrolling 3, Basic 4 and
Material 4.

[`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is the twenty-second post-core Palette slice, at Basic order 50 after
`ColoredBox`. Its **Color**, **Stroke width**, **Fallback width** and **Fallback
height** rows all start as `<not set>`, preserving Flutter's exact
`Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults. Explicit dimensions
and stroke width must be finite and non-negative; color accepts exact ARGB or a
reviewed Material `ColorScheme` role. A theme role makes generated Dart
non-const. The optional `child` is available in Slots and through ordinary
Palette/tree/Canvas insertion. Generated Dart and both Canvas routes construct
the real Flutter widget; Designer selection/drop affordances are not saved.
At that milestone the practical backlog was 42/92 complete with 50 remaining;
Layout contained 30 items, Scrolling 3, Basic 5 and Material 4.

[`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is the twenty-third post-core Palette slice, at Basic order 60 after
`Placeholder`. It has one required **Text direction** row and one required
`child` slot. The Flutter constructor has no direction default, so Designer
creation atomically wraps an existing root or non-root widget and explicitly
stores `TextDirection.ltr`; choose `rtl` in the enum editor when needed. The
occupied child is replacement-only in Slots. Expanded, Flexible and Spacer
cannot be wrapped because they must remain direct Row/Column children.
Generated Dart and both Canvas routes construct the real inherited widget, so
directional descendants resolve through the selected value while Designer
selection/drop overlays stay outside it. At the Directionality milestone the
practical backlog was 43/92 complete with 49 remaining; Layout contained 30
items, Scrolling 3, Basic 6 and Material 4.

[`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is the twenty-fourth post-core Palette slice, at Basic order 70 after
`Directionality`. Its required **Decoration** row is initialized to an empty
rectangular `BoxDecoration()` and opens the same complete typed editor used by
Container: colors, asset image, border, physical/directional elliptical radii,
ordered shadows, gradients, blend mode and shape. **Position** may remain
`<not set>` for Flutter's exact `background` default or be set to `background`
or `foreground`. The optional `child` is available through Slots and ordinary
Palette/tree/Canvas insertion. Generated Dart and both Canvas routes construct
the real widget; custom `Decoration` subclasses and raw Dart expressions remain
outside the closed model. At the DecoratedBox milestone the practical backlog
was 44/92 complete with 48 remaining; Layout contained 30 items, Scrolling 3,
Basic 7 and Material 4.

[`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
opens the Accessibility Palette category at category order 400 and item order
10. **Excluding** may remain `<not set>` for Flutter's exact `true` default or
be edited with the optional boolean checkbox; its optional `child` is managed
through Slots and ordinary Palette/tree/Canvas insertion. Omitted or explicit
`true` removes the application child's semantics subtree, while explicit
`false` preserves it. Generated Dart and both Canvas routes construct the real
widget; layout, paint and hit testing still proxy the child. The
`ExcludeSemantics` node's own Designer selection, hit/drop and accessibility
wrapper remains outside the effect; descendant Canvas semantics labels follow
the real exclusion, while the NetBeans widget tree remains separately accessible.
At that milestone the practical backlog was 45/92 complete with 47 remaining;
Layout contained 30 items, Scrolling 3, Basic 7, Material 4 and Accessibility 1.

[`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
appears beside `Stack` in Layout. **Alignment**, **Text direction**, **Clip
behavior** and **Sizing** use the existing closed Stack editors. **Index** has
three distinct states: `<not set>` preserves Flutter's default `0`, a
non-negative integer selects that existing child, and `null` displays no child.
The editor and every child-list mutation enforce the live range relation;
effective index zero is still valid while the list is empty. The **children**
Slots list remains fully ordered even though the real native/exact-Web widget
paints, hits and exposes application semantics only for the selected child and
sizes itself to the largest child. At that milestone the practical backlog was
46/92 complete with 46 remaining; Layout contained 31 items, Scrolling 3, Basic 7, Material 4
and Accessibility 1. Typed null advances `.fd` to v10, Catalog API to 10 and
Canvas model to v15; NBFC framing and Canvas control/wire remain v1.

[`ClipRect`](https://api.flutter.dev/flutter/widgets/ClipRect/ClipRect.html)
appears in Basic after `DecoratedBox`. Its typed **Clipper** editor and closed **Clip behavior** editor store
only `none`, `hardEdge`, `antiAlias` or `antiAliasWithSaveLayer`; leaving it
`<not set>` preserves Flutter's `Clip.hardEdge` default. Add at most one child
through Slots or ordinary Palette/tree/Canvas insertion. Generated Dart and the
native/exact-Web Canvas construct the real `ClipRect`, while selection, empty
drop affordances and tree accessibility remain outside its paint clip. A
non-null `CustomClipper<Rect>` uses the same analyzed current-library or
declared-package reference described below; the isolated Canvas shows the explicit unavailable preview.
At that milestone the practical backlog was 47/92 complete with 45 remaining;
Layout contained 31 items, Scrolling 3, Basic 8, Material 4 and Accessibility 1. The aggregate was
41 reviewed const definitions and 736 writable rows, including 719 outside
`Scaffold`; all schema, Catalog, Canvas and NBFC protocol versions remain
unchanged.

[`ClipOval`](https://api.flutter.dev/flutter/widgets/ClipOval/ClipOval.html)
appears in Basic after `ClipRect`. Its typed **Clipper** editor and closed **Clip behavior** editor store
only `none`, `hardEdge`, `antiAlias` or `antiAliasWithSaveLayer`; leaving it
`<not set>` preserves Flutter's `Clip.antiAlias` default. Add at most one child
through Slots or ordinary Palette/tree/Canvas insertion. Generated Dart and the
native/exact-Web Canvas construct the real `ClipOval`, inscribed in the child's
layout bounds by default, while selection, empty drop affordances and tree
accessibility remain outside its paint clip. A non-null `CustomClipper<Rect>`
uses the same analyzed current-library or declared-package reference; the isolated
Canvas preserves the child and shows the explicit unavailable preview. At that milestone the practical backlog was 48/92
complete with 44 remaining; Layout contained 31 items, Scrolling 3, Basic 9,
Material 4 and Accessibility 1. The aggregate was 42 reviewed const definitions
and 737 writable rows, including 720 outside `Scaffold`; all schema, Catalog,
Canvas and NBFC protocol versions remained unchanged.

[`ClipRRect`](https://api.flutter.dev/flutter/widgets/ClipRRect/ClipRRect.html)
appears in Basic after `ClipOval`, at item order 100. Its structured **Border
radius** editor supports physical `BorderRadius` and directional
`BorderRadiusDirectional` geometry with finite, non-negative elliptical X/Y
radii for all four corners; `<not set>` preserves `BorderRadius.zero`. Its
closed **Clip behavior** editor stores only `none`, `hardEdge`, `antiAlias` or
`antiAliasWithSaveLayer`; leaving it `<not set>` preserves `Clip.antiAlias`.
Add at most one child through Slots or ordinary Palette/tree/Canvas insertion.
Use **Clipper** to reference an existing value or zero-argument
constructor, factory or function from the current Dart library or an imported canonical
`package:` library declared by `.dart_tool/package_config.json`. An optional
member and an explicit const or non-const zero-argument invocation are supported; the
preview shows the exact emitted syntax, and Dart analysis must prove it is a
`CustomClipper<RRect>`. For configured arguments, expose a project-owned getter,
field/getter or zero-argument factory/function rather than entering raw code. Flutter ignores
**Border radius** while **Clipper** is set. Generated Dart constructs the real
`ClipRRect`; the isolated native/exact-Web Canvas does not execute project Dart,
so it preserves the child and shows an explicit preview-unavailable warning for
that branch instead of faking the radius. With no custom clipper, both Canvas
routes construct the real rounded clip. Selection, empty drop affordances and
tree accessibility remain outside its paint clip.
Basic also offers **ClipPath**. Leave `clipper` and `shape` unset for the default
rectangle. Use the Clipper editor for a `CustomClipper<Path>` or the Shape editor
for a `ShapeBorder`; accepting either branch atomically clears the other as one
undoable change. Cancelling the dialog does not switch branches.
Shape selects the real `ClipPath.shape` static helper. Editors accept existing
current/package values and zero-argument calls; put configured arguments in a
project getter/factory. Invalid, nullable or dynamic types are rejected by analyzer
proof before applying the change. Custom geometry is supported in generated Dart,
but the isolated Canvas displays a preview-unavailable warning and preserves the
child instead of executing project code. Defaults, all four clip behaviors, optional
child, save/reopen and further editing use the standard workflow.

Basic also offers **ClipRSuperellipse**. Its Border radius editor supports physical
or directional corners and independent X/Y radii. Choose any of the four clip
behaviors and optionally assign a child. The Clipper editor accepts an exact
`CustomClipper<RSuperellipse>` reference or zero-argument call, including const
construction. A custom clipper overrides the radius without discarding its value;
Restore Default on Clipper restores radius-based clipping. Default geometry uses
the real Flutter superellipse on Canvas. Project delegates generate correctly but
show an explicit preview-unavailable warning rather than execute in Canvas.
Save/reopen, further edits and Undo/Redo retain these typed editors.

Basic → **PhysicalModel** adds shape, clipping, border radius, elevation, fill
color and shadow color, plus an optional child. Its required Color starts at
`0xFF2196F3`; both color editors offer ARGB and theme tokens. Elevation must be finite
and non-negative. Radius accepts physical corners only, with separate X/Y axes.
Changing Shape to circle preserves the radius while Flutter ignores it; changing
back restores its effect. A non-square circle occupies an oval. All four clip modes
and real fill/shadow painting are supported. A childless node can remain zero-size;
its external Designer target still permits selection and child insertion. Save,
reopen, further property/child edits and Undo/Redo use the normal transaction flow.

**PhysicalShape** adds required clipper/color, clip behavior, elevation, shadow
color and optional child. Its Clipper editor switches transactionally between six
built-in ShapeBorderClipper presets and a typed project CustomClipper<Path>
reference. Presets offer physical/directional corner radii and explicit direction;
directional radii require LTR/RTL for the four cornered shapes. Circle/stadium keep
but ignore the radius/direction draft. Presets preview real shapes and shadows;
project clippers are saved/generated but show an accessible preview-unavailable
warning because Canvas cannot execute project code. The default rounded rectangle
and blue literal color make Palette creation usable without a project helper.

The practical backlog is now 53/92 complete with 39 remaining; Layout contains
31 items, Scrolling 3, Basic 14, Material 4 and Accessibility 1. The aggregate
is 47 reviewed const definitions and 759 writable rows, including 742 outside
`Scaffold`.

`Container` exposes all 13 reviewed non-widget constructor properties:
`alignment`, `padding`, `color`, `isAntiAlias`, `decoration`,
`foregroundDecoration`, `width`, `height`, `constraints`, `margin`, `transform`,
`transformAlignment` and `clipBehavior`, plus one optional `child` slot. Use its
structured editors for physical/directional alignment, bounded or unbounded
constraints, the column-major 4×4 matrix and complete BoxDecoration fill,
image, border, elliptical radius, ordered shadows and linear/radial/sweep
gradients.
Literal colors and reviewed Material theme roles work at the top level and at
every nested decoration color, including `ColorFilter.mode`. The editor rejects invalid Flutter combinations,
including `color` with `decoration` and non-`none` clipping without a decoration,
and applies required multi-property transitions as one Undo/Redo operation.
Canvas keeps the layout outline outside `transform`, shows distinct margin and
padding guides and retains a selectable/drop target when an empty Container has
zero layout size.

In the BoxDecoration editor, open the accessible **Image** tab and enable
`DecorationImage`, then choose an application or package image from the declared
asset list. The model accepts only `AssetImage` or `ExactAssetImage`, optionally
with one `ResizeImage`; it never accepts a typed path, URL or Dart expression.
The remaining typed controls cover all 13 pinned SDK arguments and mode,
matrix, linear-to-sRGB gamma, sRGB-to-linear gamma and saturation color filters.
If you enable `centerSlice`, enter a non-negative positive-area rectangle. Fit
may be unset, `fill`, `contain`, `fitWidth`, `fitHeight` or `scaleDown`;
`cover` and `none` are rejected. An `onError` value is only a validated Dart
identifier for a compatible `(Object, StackTrace?)` handler. The status below
the asset selector names why inventory is unavailable, and all image controls
expose accessible names/descriptions. OK publishes the complete structured
value as one chronological Undo/Redo edit; invalid drafts remain local.

The asset list comes only from app/package `pubspec.yaml` declarations resolved
through `.dart_tool/package_config.json`. PNG/JPEG/GIF/WebP candidates are
checked for safe POSIX-relative identity, root/symlink confinement, magic and
dimensions. Canvas receives no filesystem path or callback name: Canvas model
protocol v18 over NBFC framing v1 negotiates `asset.imageBytes.v1` and transfers
only referenced immutable compressed bytes under exact revision, order, size
and SHA-256 checks. Native
preview and the internal exact-Web runtime build the same real
`DecorationImage`. A media/decode/resize/center-slice failure quarantines only
that resource; wire, identity and exact-coverage failures still reject the
render. A missing or quarantined asset shows a deterministic
non-interactive placeholder that names its logical identity, code and reason
without removing Container selection/layout/drop overlays. Exact-Web product
selection is still gated; the routed Web choice remains the native-engine
responsive layout preview.

The current surface uses `.fd` schema v13, contributor Catalog API 14 and Canvas
model protocol 18. SafeArea's exported
`EdgeInsetsValues.directionalAllowed` constraint established API 9, while the
exact payload-free null value used by `IndexedStack.index` establishes API 10,
and ClipRRect's top-level typed radius geometry establishes API 11, while its
typed Dart-object reference establishes API 12. PhysicalModel's physical-only
`BorderRadiusValues.directionalAllowed` establishes API 13; the typed
ShapeBorderClipper value establishes API 14. API-1 through API-13 contributors
fail closed. Schema v1-v12 files migrate in memory and are written as v13
only after an admitted edit. Version 7 represents positive infinity as `null`
in all four BoxConstraints bounds; older finite minima and nullable maxima
migrate losslessly, version 8 adds the atomic finite non-negative `Size` wire
value, version 9 adds the atomic finite signed `Offset` wire value, and version
10 adds the exact payload-free null property value; version 11 adds the
physical/directional finite non-negative elliptical border-radius value;
version 12 adds the closed current/package Dart-object reference; version 13 adds
the structured ShapeBorderClipper value with reviewed shapes, radius and direction.
Fifty-three sources across forty-eight insertable any-widget and two trait-bound
slots produce 2,650 compatibility candidates: 2,408 accepted and 242
rejected. Expanded and Flexible enter only direct
`Row.children` and `Column.children` wrapper targets, while Spacer inserts only
into those same two list slots; the wrappers' required child slots are
replacement-only and excluded from the destination matrix. SafeArea and
Directionality use the
same generic atomic required-child wrapper mode without a Row/Column-only outer
placement rule; their required slots are also excluded, and neither can wrap
Expanded, Flexible or Spacer. Placeholder, DecoratedBox, ClipRect, ClipOval, ClipRRect, ClipPath, ClipRSuperellipse, PhysicalModel and ExcludeSemantics
contribute optional insertable `child` destinations; IndexedStack contributes
the insertable ordered `children` destination. NBFC framing
and Canvas control/wire remain v1.

`Scaffold` Properties are grouped as Floating action button, Appearance,
Layout, Drawer behavior and Restoration. They cover the closed location and
animator presets, persistent-footer alignment, drawer callbacks and gestures,
background and drawer-scrim colors, body/app-bar extension and inset behavior,
primary state, drawer edge width and restoration id. Its existing `appBar`,
`body` and `floatingActionButton` widget slots are unchanged. Deliberately not
exposed as scalar properties are `key`; the widget-valued
`persistentFooterButtons`, `drawer`, `endDrawer`, `bottomNavigationBar` and
`bottomSheet`; and the open/runtime-valued `persistentFooterDecoration` and
`bottomSheetScrimBuilder`. These require separately reviewed persistence or
named-slot designs; this Properties slice adds no Palette widget or DnD route.

For a slot-capable selected widget, use the `Slots` tab to choose the exact
named slot instead of relying on an ambiguous flattened-tree drop. Empty slots
offer compatible add/move operations. An occupied single slot explicitly
offers replacement with a fresh canonical widget, replacement with an existing
same-document non-root widget, and `Clear`. A list slot offers `Clear All` for
its complete ordered child set alongside its existing add, move, reorder and
single-child removal actions. Replace and `Clear All` are each one atomic model
command and one Undo/Redo edit, never a series of partial removes. The editor is
bound to the exact presented revision and child ids; stale, incompatible,
root, cyclic, cardinality-violating or minimum-violating requests are disabled
or rejected without changing the model, generated Dart, saved pair or history.
OK submits at most once, while Cancel submits nothing.

Expanded and Flexible are the exceptions to ordinary Palette prototype
insertion: each wraps one existing direct Row/Column child and exposes a
required replacement-only `child` slot. Neither wrapper can wrap Expanded,
Flexible or Spacer, and neither required child slot is an insertable DnD
destination. Spacer is an ordinary childless prototype only for direct
Row/Column list insertion; other destinations fail closed.
Image Add and Replace New Widget allocate a normal stable ID even without a
current declared asset. In that case they store the reserved unresolved
provider, show `<choose asset>` in Properties and render the built-in
placeholder until a declared asset is selected. Commit still repeats inventory
resolution for race safety. TextField is an ordinary immediate leaf insertion
and never captures runtime editable state.

Every application newly created by this plugin also receives the shared
`.fd_templates/project.fdtheme` catalog and generated
`lib/theme/app_theme.dart`. Use `Flutter > Edit Flutter Themes...`, the project
context action, or open `project.fdtheme`. The docked `Themes` tab appears beside
`Palette`; it edits the default mode, selects the application light/dark
definitions, and creates custom themes. Use `General` for catalog/application
settings, `Colors` for the 46 supported Material `ColorScheme` roles, and
`Typography` for all 15 Material 3 `TextTheme` roles and their 13 typed optional
fields. `Use default`/inherit removes an override and restores the seed-derived
Material value. Each catalog definition can be enabled or disabled
independently; a disabled definition stays editable but is omitted from
generated Dart. While project themes are enabled, the selected light and dark
definitions must also be enabled. Clear `Enable project themes` to use Flutter's
defaults while preserving all selections and per-theme states for later
re-enabling.
The same catalog applies to every Designer form; `.fd` stores no copied theme
definitions. Save rewrites the descriptor and generated Dart as one
hash-guarded pair, while Reload explicitly discards the current draft. If
`app_theme.dart` changed outside the editor, NetBeans reports the conflict
instead of overwriting it. For an older application with no catalog, the command
offers an explicit initialization only when its `main.dart` has the safely
recognized Flutter template shape.

To duplicate a clean pair, Copy either its Dart or `.fd` node, select that node's current physical parent folder, and Paste. NetBeans chooses `_copy`, `_copy_2`, and so on using both mirrored folders, copies the Dart bytes exactly, and creates an independent `.fd` with a new `documentId` and matching `source.dartFile`. A clean open source editor stays open on the original. Cross-directory Copy remains unavailable because relative-URI rebasing semantics are not defined.

To move a clean pair, Cut either member, select an already existing destination folder under the same physical tree, and Paste. The matching destination folder under the other mirrored root must also already exist. Move is limited to the same Flutter project, keeps the basename and both exact file contents unchanged, and consumes its private one-shot Cut transfer after a successful Paste. A clean open Designer/Source editor is closed; the old path-bound objects retire and fresh owners are created at the destination. Generic one-file DataObject Move remains disabled.

Move fails closed without changing either member when a destination collides, either mirrored destination belongs to another project, a path is read-only or linked, any project Dart editor is unsaved, or the pair/evidence changes during the operation. Its bounded project-wide directive check requires a canonical `pubspec.yaml` name and strict matching `.dart_tool/package_config.json`; it rejects nested packages, aliases of the project's `lib`, outgoing relative directives from the moved source, incoming references to its old path, references that could acquire the destination, unsupported URI forms, and conservative case/Unicode path aliases. The final proof and filesystem commit share one EDT admission under the exact NetBeans 30 MasterFS proof-file locks and child-cache mutexes for every relevant and physical ancestor folder; an incompatible runtime fails closed. The rollback guarantee applies only within the running NetBeans process; there is no durable crash journal, and files changed concurrently outside NetBeans remain a residual race. If a provider reports a late failure after the exact targets and source tombstones already establish commit, NetBeans consumes the Cut and reports a recovery warning instead of falsely offering the same Move again.

Schema-v1 asset paths are always relative to the Flutter project/pubspec root, never to the `.fd` file. The location-independent opaque `extensions` metadata must not encode `.fd`-relative meaning; Move deliberately preserves those values byte-for-byte.

## 5. Select a target and run or debug

Make the Flutter project active in the Projects window, or set it as the main project. The same execution actions are available from the top-level `Flutter` menu and the Flutter project's context menu.

The standard NetBeans configuration selector in the Run toolbar lists the connected targets reported by Flutter. Its compact entries show the Desktop, Mobile, or Web kind and the concrete device name; the detailed chooser also shows platform and id. The list is discovered in the background when the project opens and refreshes automatically about five seconds after each successful discovery. Failed passive refreshes preserve the last good list and retry with a bounded backoff. The selected device is remembered separately for each project. Use `Flutter > Select Run Target...` for an immediate refresh or the detailed chooser. If a remembered device is unavailable, the toolbar selects the first available target deterministically; Run and Debug still verify that it is connected before launching.

Use NetBeans' standard `Build Project`, `Clean Project`, and `Clean and Build Project` actions from the Run menu or the Flutter project's context menu. Build captures the current toolbar target and runs the matching release build: Windows, Linux, macOS, Web, Android APK, or iOS. Clean runs `flutter clean`. Clean and Build validates the target before deleting anything, runs both stages sequentially under one project-action lifecycle, and skips Build when Clean fails or is cancelled. Each current stage has native NetBeans Output, progress, and Stop integration.

For Android, use `Flutter > Device Manager`. Android SDK discovery checks the JVM property `android.sdk`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, Flutter's saved Android SDK, and platform-default locations. The window shows detected tools, connected physical/emulated devices, configured AVDs and their concrete boot/runtime states. It can create an AVD from an already installed system image, start it, wait for boot, stop, restart, wipe user data, delete it, refresh inventory and select the exact online ADB serial as the Flutter Run/Debug target. Wipe starts the reset AVD and does not wipe an attached SD card; Wipe and Delete both require a concrete confirmation. Cancelling a boot wait or closing Device Manager stops the NetBeans operation but leaves an already launched emulator running.

`Flutter > Launch Mobile Emulator...` remains available for Android and iOS definitions already exposed by Flutter. Device discovery is serialized per project so toolbar polling, Run/Debug validation, target selection and emulator waiting never execute competing `flutter devices` commands.

Choose `Flutter > Run Flutter Project` for a normal debug-mode Flutter run, or `Flutter > Debug Flutter Project` to start paused and connect the NetBeans debugger through Flutter's DAP adapter. Debug sessions support Dart breakpoints, stepping, and variables.

Double-click an ordinary `.dart` file, or choose `Open` as the first item in its context menu, to open the standard NetBeans Dart editor. Opening the file activates the incremental Dart lexer, theme-aware syntax highlighting, and Dart-aware two-space indentation. Enter between `{}` creates an indented body and a leading `}` is realigned; braces inside strings and comments do not affect indentation. These typing operations use the live token hierarchy and line-local text rather than copying or re-lexing the complete document on each keystroke.

The first editor request that needs semantic information starts `dart language-server --protocol=lsp` from the configured Dart SDK for the owning project. NetBeans supplies the project root, synchronizes open and changed documents, and consumes the server capabilities through its standard LSP client. Diagnostics appear as standard editor hints and error-stripe marks; use completion, Go to Declaration/Ctrl-click, Find Usages, Rename, and `Source > Format` exactly as for other NetBeans languages. Completion inserts an import when Dart supplies it as resolved `additionalTextEdits`. For part files and other multi-file cases, Dart can instead return the import as `resolved.command`; NetBeans 30's standard `CompletionProviderImpl` does not execute that command, so apply the diagnostic's Quick Fix to add the import. Organize Imports is available among the editor's Dart source actions.

The status bar reports when the Dart analysis process starts or restarts. If the SDK is unavailable or the process cannot be launched, one deduplicated notification names the affected project and concrete cause; click it to open `Tools > Options > Flutter`. Closing the Flutter project stops the language-server process without an error notification. Headless contract tests, the real-SDK protocol test, and an optional assembled-NetBeans editor E2E gate cover diagnostics, completion imports delivered as `additionalTextEdits`, missing-import Quick Fix/workspace edits, Organize Imports, definition, references, rename, document formatting, server restart, and project-close cleanup.

The plugin normalizes Flutter DAP output events for NetBeans 30 and reports the debugger as connected only after successful `attach` and `configurationDone` responses plus Flutter's `flutter.appStarted` event. Output events without a category are routed to the debugger console instead of producing an `Unexpected Exception` notification. If the complete DAP handshake cannot finish within its bounded deadline, the plugin terminates the adapter and reports a failed Debug action instead of leaving it stuck in Starting.

Run and Debug display a native NetBeans progress indicator for the lifetime of the Flutter process. Its text changes through Starting, Running, and Stopping and names the project and selected target. Choose Cancel in that progress indicator to request an orderly application stop; progress completes after the process exits.

If you choose Run or Debug again while the application is still Starting or Running, NetBeans names both the target of the current session and the currently selected toolbar target, then asks whether to stop and restart. Choose No to keep the existing session. Changing the toolbar target alone does not move a running application, and restart is unavailable after the session has entered Stopping.

While the Flutter app is running, `Hot Reload`, `Hot Restart`, and `Stop Flutter Application` become available. Flutter application output, session state, emulator progress, errors, progress cancellation, restart activity, and debugger diagnostics appear in a named NetBeans Output tab.

When the running application has published its VM Service URI, choose `Flutter > Open DevTools`. The action uses the Dart SDK saved in `Tools > Options > Flutter` to run DevTools on loopback (`127.0.0.1`) with an automatically selected port, connects it to that exact Flutter session, and opens the connected URL with NetBeans' configured browser. Until an active Run or Debug session and its VM Service URI are available, Open DevTools remains unavailable and reports the concrete missing state.

DevTools startup and server output appear in a separate `Flutter DevTools: <project>` Output tab, and a native cancellable progress indicator remains visible for the server lifetime. Choosing `Flutter > Open DevTools` again reopens the already running server URL; it does not launch another server. Choose `Flutter > Stop DevTools` to stop DevTools without stopping the application. DevTools is also stopped automatically when its owning Flutter session stops or is replaced, or when the project closes. Hot Reload and Hot Restart keep the same DevTools session.

The current integration opens the SDK-provided browser DevTools. A DevTools panel embedded inside NetBeans and the DevTools Flutter Inspector/widget tree are not implemented yet; those are separate from the Designer's Windows read-only native Canvas and Explorer widget tree.

The selector is the same project-configuration combo used by Java projects. It follows NetBeans' main/active project rules and standard Run and Debug actions reuse its selected Flutter target without opening an extra dialog.

## 6. Resolve packages, analyze, and test

Use `Flutter > Flutter Pub Get` after changing dependencies. Use `Flutter > Flutter Analyze` to run a project-wide analysis without an implicit package download; click a reported issue in its Output tab to open the Dart file at the reported line and column.

Use `Flutter > Flutter Test` or the project's standard Test action for all tests. With a Dart test file active, use `Test Current Dart File`; place the caret in a literal `test(...)` or `testWidgets(...)` declaration and use `Test at Caret` for one test. Flutter's machine events are mapped into the standard NetBeans Test Results session, including failures, errors, skipped tests, cancellation/abnormal termination, and rerun. Each tooling command has native Output, Progress, and Stop controls and closing the project cancels its active tooling process.

In `pubspec.yaml`, press Ctrl+Space for pub/Flutter keys, SDK dependencies, and local packages under the project. Semantic diagnostics supplement the bundled YAML syntax checks with required fields, type checks, dependency-source conflicts, and missing local paths/assets. Completion and diagnostics intentionally do nothing in other YAML files.

## 7. Run the plugin during development

After a successful build, assemble the plugin cluster and start a separate
NetBeans IDE 31 instance from the installed `G:/netbeans` runtime:

```powershell
mvn nbm:cluster
mvn nbm:run-ide -Dnetbeans.installation=G:/netbeans
```

The launched IDE uses `target/userdir`, so its imported SDK settings are separate from those of the development NetBeans instance.
