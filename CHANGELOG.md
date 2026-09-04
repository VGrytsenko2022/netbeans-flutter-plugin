# Changelog

All notable changes to the NetBeans Flutter plugin are documented in this file.

## [0.1.3] - Unreleased

### Added

- The accepted const
  [`flutter.widgets.Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
  vertical slice is the fourteenth post-core Palette addition, at Layout order
  200 immediately after SizedOverflowBox. Its complete pinned Flutter 3.44.8
  `Transform.new` non-`key` contract exposes required `Matrix4 transform`,
  optional signed finite `Offset origin`, optional physical/directional
  `AlignmentGeometry alignment`, optional `transformHitTests`, optional
  `FilterQuality.none/low/medium/high` and one optional single any-widget
  `child`. New instances persist only an identity matrix; omission preserves
  Flutter's null origin/alignment/filter quality and `transformHitTests: true`
  defaults. Generated Dart and native and exact-Web Canvas construct the real
  paint-time Transform without changing the child's layout size. Origin and
  alignment compose exactly as Flutter defines them, filtering remains opt-in,
  and hit testing follows the matrix only when `transformHitTests` is true.
  Designer selection and drop geometry follow the same effective transform
  without changing Flutter layout, with a bounded 36x36 non-layout-affecting
  target only for a real zero-sized node. Non-finite, projective-horizon and
  non-invertible surface geometry fails closed, while finite rotated/skewed DnD
  checks exact local containment instead of accepting AABB corner triangles.
  Exact property and slot editing, Palette/tree/Canvas DnD, same-tree movement,
  deterministic generation, Save/reopen and further editing, Undo/Redo and
  reviewed light/dark SVG icons share one closed contract. The named
  `Transform.rotate`, `.translate`, `.scale` and `.flip` convenience
  constructors remain outside this slice; their resulting matrices are still
  expressible through `Transform.new`. The catalog now contains 34 widgets, 29
  reviewed const constructors and 674 writable rows, including 657 across the
  33 non-`Scaffold` definitions. Thirty-four sources across 31 insertable
  any-widget and two trait-bound destinations form 1,122 DnD candidates: 969
  accepted and 153 rejected. Layout contains 26 items, and the practical
  92-widget backlog is 34/92 complete with 58 remaining. The new closed atomic
  `Offset` value advances `.fd` to schema v9, Catalog API to 8 and Canvas model
  to v14; NBFC framing/control/wire remain v1.
- The accepted const
  [`flutter.widgets.SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
  vertical slice is the thirteenth post-core Palette addition, at Layout order
  190 immediately after Offstage. Its complete pinned Flutter 3.44.8 non-`key`
  contract exposes required structured `Size size`, optional
  `AlignmentGeometry alignment` (constructor default `Alignment.center`) and
  one optional single any-widget `child`. New instances persist the reviewed
  visible default `Size(100, 100)`; width and height are finite, non-negative
  and edited atomically. Generated Dart and native and exact-Web Canvas
  construct the real Flutter SizedOverflowBox: its requested outer size is
  constrained by the parent, while the child receives the original incoming
  constraints and may paint outside according to alignment; Flutter keeps hit
  testing inside the parent's bounds. A real zero-sized result keeps only a
  bounded 36x36, non-layout-affecting Designer selection/drop target. Exact
  property and slot editing, Palette/tree/Canvas DnD, same-tree movement,
  deterministic generation, Save/reopen and further editing, Undo/Redo and
  reviewed light/dark SVG icons share one closed contract. At that milestone the catalog contained
  33 widgets, 28 reviewed const constructors and 669 writable rows,
  including 652 across the 32 non-`Scaffold` definitions. Thirty-three sources
  across 30 insertable any-widget and two trait-bound destinations form 1,056
  DnD candidates: 908 accepted and 148 rejected. Layout contains 25 items, and
  the practical 92-widget backlog is 33/92 complete with 59 remaining. The new
  closed atomic `Size` value advanced `.fd` to schema v8, Catalog API to 7 and
  Canvas model to v13; NBFC framing/control/wire remained v1.
- The accepted const
  [`flutter.widgets.Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html)
  vertical slice is the twelfth post-core Palette addition, at Layout order
  180 immediately after IntrinsicWidth. Its complete pinned Flutter 3.44.8
  non-`key` contract exposes the optional boolean `offstage` value (constructor
  default `true`) plus one optional single any-widget `child`. Omission and
  explicit `true` remain distinct Designer states even though both hide the
  child; explicit `false` participates in layout, painting, hit testing and
  semantics normally. Generated Dart and native and exact-Web Canvas construct
  the real Flutter Offstage. When hidden, Flutter still lays the child out and
  keeps it active and focusable, including running animations, while suppressing
  paint, hit testing and semantics and normally contributing no parent space.
  Palette and Properties descriptions therefore recommend removing a
  long-hidden subtree instead of using Offstage when ongoing work would waste
  resources. A real zero-sized result keeps a bounded 36x36,
  non-layout-affecting Designer selection/drop target outside the Offstage
  effect. Exact property and slot editing, Palette/tree/Canvas DnD, same-tree
  movement, deterministic generation, Save/reopen, Undo/Redo and reviewed
  light/dark SVG icons share one closed contract. At that milestone the catalog contained 32
  widgets, 27 reviewed const constructors and 667 writable rows, including 650
  across the 31 non-`Scaffold` definitions. Thirty-two sources across 29
  insertable any-widget and two trait-bound destinations form 992 DnD
  candidates: 849 accepted and 143 rejected. Layout contains 24 items, and the
  practical 92-widget backlog is 32/92 complete with 60 remaining. `.fd` schema
  v7, Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- The accepted const
  [`flutter.widgets.IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
  vertical slice is the eleventh post-core Palette addition, at Layout order
  170 immediately after IntrinsicHeight. Its complete pinned Flutter 3.44.8
  non-`key` contract exposes optional non-negative `stepWidth` and `stepHeight`
  values plus one optional single any-widget `child`. Null and explicit zero
  remain distinct Designer values even though Flutter treats zero as no
  snapping; positive values snap the corresponding child extent upward to a
  multiple of the step. Generated Dart and native and exact-Web Canvas
  construct the real Flutter IntrinsicWidth while preserving parent
  constraints. Palette and Properties descriptions expose the speculative
  intrinsic-layout performance warning, including worst-case O(N²) tree-depth
  behavior. Empty or collapsed nodes keep real layout behind a bounded,
  non-layout-affecting Designer selection/drop target. Exact property and slot
  editing, Palette/tree/Canvas DnD, same-tree movement, deterministic
  generation, Save/reopen, Undo/Redo and reviewed light/dark SVG icons share
  one closed contract. At that milestone the catalog contained 31 widgets, 26
  reviewed const constructors and 666 writable rows, including 649 across the
  30 non-`Scaffold` definitions. Thirty-one sources across 28 insertable
  any-widget and two trait-bound destinations formed 930 DnD candidates: 792
  accepted and 138 rejected. Layout contained 23 items, and the practical
  92-widget backlog was 31/92 complete with 61 remaining. `.fd` schema v7,
  Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- The accepted const
  [`flutter.widgets.IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
  vertical slice is the tenth post-core Palette addition, at Layout order 160
  immediately after Baseline. Its complete pinned Flutter 3.44.8 non-`key`
  contract has no writable properties and exposes one optional single
  any-widget `child`. Generated Dart and native and exact-Web Canvas construct
  the real Flutter IntrinsicHeight, preserving parent constraints and the
  framework-owned intrinsic-height layout pass. Palette and Properties
  descriptions warn that speculative intrinsic layout is relatively expensive
  and can be O(N²) in tree depth. An empty or collapsed node keeps its real
  layout while a bounded, non-layout-affecting Designer target supplies
  selection and child insertion. Exact-slot editing, Palette/tree/Canvas DnD,
  same-tree movement, deterministic generation, Save/reopen, Undo/Redo and
  reviewed light/dark SVG icons share one closed contract. At that milestone
  the catalog contained 30 widgets, 25 reviewed const constructors and 664 writable rows,
  including 647 across the 29 non-`Scaffold` definitions. Thirty sources
  across 27 insertable any-widget and two trait-bound destinations formed 870
  DnD candidates: 737 accepted and 133 rejected. Layout contained 22 items, and
  the practical 92-widget backlog was 30/92 complete with 62 remaining. `.fd`
  schema v7, Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1
  remain unchanged.
- The accepted const
  [`flutter.widgets.Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html)
  vertical slice is the ninth post-core Palette addition, at Layout order 150
  immediately after Spacer. Its complete pinned Flutter 3.44.8 non-`key`
  contract exposes required finite-double `baseline`, required
  `TextBaseline.alphabetic`/`ideographic` `baselineType`, and one optional
  single any-widget `child`. Flutter defines no constructor defaults, so a new
  Designer prototype stores the reviewed visible starting values
  `baseline: 24.0` and `baselineType: TextBaseline.alphabetic`; NaN, infinity,
  wrong enum types and raw Dart remain rejected. Generated Dart and native and
  exact-Web Canvas construct the real Flutter Baseline. An empty Baseline keeps
  framework `constraints.smallest` layout (often zero) while a
  non-layout-affecting Designer target supplies selection and child insertion.
  Properties, exact-slot editing,
  Palette/tree/Canvas DnD, same-tree movement, deterministic generation,
  Save/reopen, Undo/Redo and reviewed light/dark SVG icons share one closed
  contract. At that milestone the catalog contained 29 widgets, 24 reviewed const constructors
  and 664 writable rows, including 647 across the 28 non-`Scaffold`
  definitions. Twenty-nine sources across 26 insertable any-widget and two
  trait-bound destinations formed 812 DnD candidates: 684 accepted and 128
  rejected. Layout contained 21 items, and the practical 92-widget backlog was
  29/92 complete with 63 remaining. `.fd` schema v7, Catalog API 6, Canvas model
  v12 and NBFC framing/control/wire v1 remain unchanged.
- The accepted const
  [`flutter.widgets.Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html)
  vertical slice is the eighth post-core Palette addition, at Layout order 140
  immediately after Flexible. Its pinned Flutter 3.44.8 contract exposes one
  optional positive portable integer `flex`; omission preserves `flex: 1`,
  while zero, negative and over-limit values are rejected. Spacer is a childless leaf that
  is inserted only into direct `Row.children` or `Column.children` slots.
  Expanded and Flexible cannot wrap Spacer because Spacer internally creates
  the parent-data path that must remain directly below the Flex. Generated Dart
  emits the real Flutter Spacer; native and exact-Web Canvas keep that real
  widget direct while exposing selection and outline geometry through the
  surface overlay. Properties, Palette/tree/Canvas insertion, same-tree
  movement, deterministic generation, Save/reopen, Undo/Redo and reviewed
  light/dark SVG icons share one closed contract. The catalog now contains 28
  widgets, 23 reviewed const constructors and 662 writable rows, including 645
  across the 27 non-`Scaffold` definitions. Twenty-eight sources across the
  unchanged 25 insertable any-widget and two trait-bound destinations form 756
  DnD candidates: 633 accepted and 123 rejected. Layout contains 20 items, and
  the practical 92-widget backlog is 28/92 complete with 64 remaining. `.fd`
  schema v7, Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1
  remain unchanged.
- The accepted const
  [`flutter.widgets.Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html)
  vertical slice is the seventh post-core Palette addition, at Layout order 130
  immediately after Expanded. Its pinned Flutter 3.44.8 contract exposes
  optional non-negative portable integer `flex`, optional
  `FlexFit.loose`/`tight`, and one required single any-widget `child`; omitted
  values preserve `flex: 1` and `FlexFit.loose`. Palette/tree/Canvas creation
  atomically wraps an existing direct Row or Column child and never creates an
  empty required-child placeholder. Expanded and Flexible cannot wrap either
  wrapper type because the inner ParentDataWidget would no longer be a direct
  Flex child. Generated Dart and native/exact-Web Canvas construct the real
  Flutter Flexible, including zero-flex inflexible layout and loose or tight
  positive-flex allocation. Properties, required-child replacement, creation,
  Palette/tree/Canvas DnD, same-tree movement, deterministic generation,
  Save/reopen, Undo/Redo and reviewed light/dark SVG icons share one closed
  contract. At that milestone the catalog contained 27 widgets, 22 reviewed const constructors
  and 661 writable rows, including 644 across the 26 non-`Scaffold`
  definitions. Twenty-seven sources across the unchanged 25 insertable
  any-widget and two trait-bound destinations formed 729 DnD candidates: 631
  accepted and 98 rejected. Layout contained 19 items, and the practical
  92-widget backlog was 27/92 complete with 65 remaining. `.fd` schema v7,
  Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- The accepted const
  [`flutter.widgets.OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
  vertical slice is the sixth post-core Palette addition, at Layout order 109
  between LimitedBox and Stack. Its pinned Flutter 3.44.8 contract exposes
  optional alignment, four finite non-negative double constraint overrides,
  `OverflowBoxFit.max`/`deferToChild`, and one optional single any-widget
  `child`. A new prototype stores no defaults: omitted bounds inherit the
  corresponding parent constraints, while omitted alignment and fit preserve
  `Alignment.center` and `OverflowBoxFit.max`. Present minimum/maximum pairs
  must remain normalized; explicit non-finite overrides are rejected by the
  bounded Designer contract. Generated Dart and native/exact-Web Canvas
  construct the real Flutter OverflowBox, including physical/directional
  alignment and both fit modes. Properties, creation, Palette/tree/Canvas DnD,
  exact-slot editing, same-tree movement, deterministic generation,
  Save/reopen, Undo/Redo and reviewed light/dark SVG icons share one closed
  contract. The catalog now contains 26 widgets, 21 reviewed const constructors
  and 659 writable rows, including 642 across the 25 non-`Scaffold`
  definitions. Twenty-six sources across 25 any-widget and two trait-bound
  destinations form 702 DnD candidates: 629 accepted and 73 rejected. Layout
  contains 18 items, and the practical 92-widget backlog is 26/92 complete with
  66 remaining. `.fd` schema v7, Catalog API 6, Canvas model v12 and NBFC
  framing/control/wire v1 remain unchanged.
- The accepted const
  [`flutter.widgets.LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
  vertical slice is the fifth post-core Palette addition, at Layout order 108
  between UnconstrainedBox and Stack. Its pinned Flutter 3.44.8 contract exposes
  optional finite non-negative `maxWidth` and `maxHeight` Properties plus one optional
  single any-widget `child`. A new prototype stores no property defaults;
  omission canonically preserves each `double.infinity` framework default.
  Generated Dart and native/exact-Web Canvas construct the real Flutter
  LimitedBox, applying a selected maximum only while the incoming axis is
  unbounded. Properties, creation, Palette/tree/Canvas DnD, exact-slot editing,
  same-tree movement, deterministic generation, Save/reopen, Undo/Redo and
  reviewed light/dark SVG icons share one closed contract. At that milestone
  the catalog contained 25 widgets, 20 reviewed const constructors and 653 writable rows,
  including 636 across the 24 non-`Scaffold` definitions. Twenty-five sources
  across 24 any-widget and two trait-bound destinations form 650 DnD candidates:
  580 accepted and 70 rejected. Layout contained 17 items, and the practical
  92-widget backlog was 25/92 complete with 67 remaining. `.fd` schema v7,
  Catalog API 6, Canvas model v12 and NBFC framing/control/wire v1 remain
  unchanged.
- The accepted const
  [`flutter.widgets.UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
  vertical slice is the fourth post-core Palette addition, at Layout order 107
  immediately after ConstrainedBox. Its pinned Flutter 3.44.8 contract exposes
  optional `textDirection`, `alignment`, `constrainedAxis` and `clipBehavior`
  Properties plus one optional single any-widget `child`. A new prototype stores
  no property defaults: omission preserves centered alignment, no retained axis
  and `Clip.none`, while omitted `textDirection` resolves directional alignment
  through the ambient `Directionality`. Generated Dart and native/exact-Web
  Canvas construct the real Flutter UnconstrainedBox; an empty zero-size instance
  keeps only the bounded Designer selection/drop target. Properties, creation,
  Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, deterministic
  generation, Save/reopen, Undo/Redo and reviewed light/dark SVG icons share one
  closed contract. At that milestone the catalog contained 24 widgets, 19 reviewed const
  constructors and 651 writable rows, including 634 across the 23
  non-`Scaffold` definitions. Twenty-four sources across 23 any-widget and two
  trait-bound destinations formed 600 DnD candidates: 533 accepted and 67
  rejected. Layout contained 16 items, and the practical 92-widget backlog was
  24/92 complete with 68 remaining. `.fd` schema v7, Catalog API 6, Canvas model
  v12 and NBFC framing/control/wire v1 remain unchanged.
- The accepted non-const `flutter.widgets.ConstrainedBox` vertical slice is the
  third post-core Palette addition. It exposes required typed `constraints` and
  one optional single any-widget `child`. The shared BoxConstraints model/editor
  now represents finite, unbounded and expanding axes, rejects negative finite
  bounds, finite minima above maxima, and an infinite minimum paired with a
  finite maximum. Generated Dart and native/exact-Web Canvas construct the real
  Flutter ConstrainedBox; an empty zero-size instance keeps only the bounded
  Designer selection/drop target. Properties, creation, Palette/tree/Canvas
  DnD, exact-slot editing, same-tree movement, deterministic generation,
  Save/reopen, Undo/Redo and reviewed light/dark SVG icons share one closed
  contract. At that milestone the catalog contained 23 widgets, 18 reviewed const constructors
  and 647 writable rows, including 630 across the 22 non-`Scaffold`
  definitions. Twenty-three sources across 22 any-widget and two trait-bound
  destinations form 552 DnD candidates: 488 accepted and 64 rejected. Layout
  contained 15 items, and the practical 92-widget backlog was 23/92 complete with
  69 remaining. Nullable infinity in all four canonical constraint bounds
  advances `.fd` to schema v7, the exported semantic domain advances Catalog
  API to 6, and the Canvas model advances to v12; NBFC framing and Canvas
  control/wire remain v1.
- Typed `IconData` values now show the exact Material glyph beside their
  readable `Icons.*` name in both the NetBeans Properties value cell and every
  row of the searchable chooser. One shared UI renderer loads the pinned
  `MaterialIcons-Regular.otf` asynchronously from the currently resolved
  Flutter SDK, verifies its fixed normalized cache path, size and SHA-256 against the packaged Web
  Canvas artifact manifest, and never falls back to a platform icon font.
  Missing, linked, changed or mismatched font content produces a neutral vector
  placeholder while the searchable text and accessibility labels remain usable.
- The accepted const `flutter.widgets.FittedBox` vertical slice is the second
  post-core Palette addition. It exposes optional `fit`, `alignment` and
  `clipBehavior` Properties plus one optional single any-widget `child`, with
  omission preserving Flutter's `BoxFit.contain`, `Alignment.center` and
  `Clip.none` defaults. The closed fit domain contains all seven `BoxFit`
  values; alignment admits finite physical and directional coordinates; and
  clipping uses all four reviewed `Clip` values. Generated Dart and native/
  exact-Web Canvas construct the real Flutter FittedBox, preserving framework
  scaling, LTR/RTL directional alignment and overflow clipping. An empty
  zero-size instance receives only the 36-pixel Designer selection/drop target.
  Standard Properties, Palette/tree/Canvas DnD, exact-slot editing, same-tree
  movement, Save/reopen and Undo/Redo share the same catalog contract, and four
  reviewed SVG resources cover light/dark 16- and 32-pixel Palette icons. The
  catalog at that milestone contained 22 widgets, 18 reviewed const constructors and 646
  writable rows, including 629 across the 21 non-`Scaffold` definitions.
  Twenty-two sources across 21 any-widget and two trait-bound destinations form
  506 DnD candidates: 445 accepted and 61 rejected. The Layout category contains
  14 items. The practical 92-widget Material/Base backlog was 22/92 complete,
  with 70 remaining; `ConstrainedBox` was the next complete slice. Existing
  schema, Catalog API, Canvas model and framing/control versions remain
  unchanged.
- The accepted const `flutter.widgets.Wrap` vertical slice is the first
  post-core Palette addition. It exposes all nine non-`key` constructor
  arguments (`direction`, `alignment`, `spacing`, `runAlignment`, `runSpacing`,
  `crossAxisAlignment`, `textDirection`, `verticalDirection` and
  `clipBehavior`) plus one ordered any-widget `children` slot. Spacing values
  are finite and may be negative, matching pinned Flutter 3.44.8. Generated
  Dart and native/exact-Web Canvas build the real Flutter `Wrap`; horizontal or
  vertical runs, alignment, directionality and clipping remain framework-owned.
  Empty Wrap instances retain a 36-pixel Designer selection target, and both
  empty and populated instances expose their full rendered rectangle as the
  deterministic terminal append zone because a wrapped run has no single
  stable terminal edge. At the Wrap milestone the catalog contained 21 widgets, 17 reviewed
  const constructors and 643 writable rows, including 626 across the 20
  non-`Scaffold` definitions. Twenty-one sources across 20 any-widget and two
  trait-bound destinations form 462 DnD candidates: 404 accepted and 58
  rejected, and the Layout category contained 13 items. The practical
  Material/Base Designer backlog targets 92 widgets;
  71 remain after Wrap. This is a planning target, not a normative complete
  list of Flutter widgets; `FittedBox` was the next planned slice. Existing
  schema, Catalog API, Canvas model and framing/control versions remain
  unchanged.
- The accepted non-const `flutter.widgets.ListView` vertical slice completes the
  originally agreed eight-item core Palette, with Button represented by
  `ElevatedButton`; that agreed scope is now 8/8 and does not claim support for
  every Flutter widget. The static `ListView(children: ...)` definition exposes
  17 optional constructor-intent rows and one ordered any-widget `children`
  slot. It admits closed axis, physics, drag, keyboard-dismiss, clip and hit-test
  presets; non-negative padding/item/cache extents; child-delegate flags;
  bounded restoration metadata; and `semanticChildCount` no greater than the
  current static child count. Numeric cache extent generates
  `ScrollCacheExtent.pixels`; controller-owned state, builders/delegates,
  `itemExtentBuilder`, `prototypeItem`, deprecated `cacheExtent`, `key` and raw
  Dart remain excluded. Generated Dart, native Canvas and exact-Web Canvas build
  a real ListView and preserve horizontal/vertical, reverse and LTR/RTL
  insertion geometry. Their shared constraint guard supplies width 240 or
  height 120 for an unbounded viewport cross axis, and for an unbounded main
  axis only when `shrinkWrap` is false. At that core-completion milestone the
  catalog contained 20 widgets, 16 const-constructor
  definitions and 634 writable rows, including 617 across the 19
  non-`Scaffold` definitions. Twenty sources across 19 any-widget and two
  trait-bound destinations form 420 DnD candidates: 365 accepted and 55
  rejected. `.fd` schema v6, Catalog API 5, Canvas model v11 and version-1
  framing/control contracts remain unchanged.
- The accepted Material `flutter.material.TextField` vertical slice is a const,
  leaf Palette definition named **Text Field** with 54 optional named constructor
  leaves and no creation dialog or stored creation defaults. Properties group the
  reviewed fields as Input (14), Layout (9), Behavior (11), Cursor and selection
  (11), Callbacks (8) and Restoration (1), with closed keyboard-type,
  `TextAlignVertical` and system-mouse-cursor presets. Callback values are strict
  identifiers; controller, focus-node, formatter, decoration/style/builder graphs,
  typed text, selection and runtime controller/focus state are not persisted by
  Designer. Cursor-radius and scroll-padding leaves are set/reset as atomic
  pairs/quartets. Generated Dart and the real non-interactive Canvas TextField use
  a `LayoutBuilder`/`SizedBox` constraint guard: unbounded width receives 240
  logical pixels, while `expands: true` under unbounded height receives 120.
  At the TextField milestone the catalog contained 19 widgets, 16
  const-constructor definitions and 617 writable rows, including 600 across the
  18 non-`Scaffold` definitions. Nineteen sources across 18 any-widget and two
  trait-bound destinations formed 380 DnD candidates: 328 accepted and 52
  rejected. `.fd` schema v6, Catalog API
  5, Canvas model v11 and version-1 framing/control contracts remain unchanged.
- The accepted const `flutter.widgets.Image` leaf exposes its required typed
  asset-only `ImageProvider` plus 21 reviewed callback, accessibility, sizing,
  paint and quality leaves. Palette/tree/Canvas drag remains available when the
  declared-image inventory is empty, refreshing, verifying or unavailable:
  accepted Add stores a reserved unresolved provider, presents `<choose asset>`
  in Properties and uses a built-in Canvas/generated-Dart placeholder. The
  placeholder survives Save/reopen and remains editable without ever becoming a
  fake project `AssetImage` path. Its custom editor now renders the empty
  selection explicitly as `<choose asset> — keep editable placeholder`; `OK`
  preserves that valid incomplete state, while `Container.DecorationImage`
  still requires a real declared asset when enabled. When choices are available,
  Add still selects the deterministic first sorted declared asset, and commit
  resolves the latest inventory for race safety. Four center-slice
  coordinates are all-or-none, form a strict non-empty rectangle and reject
  `BoxFit.cover` or `BoxFit.none`; Canvas and generated Dart share the same
  provider and decode bounds.
- The accepted const `flutter.widgets.Expanded` slice exposes optional
  non-negative `flex` and one required `child`. Its Palette affordance never
  creates a terminal placeholder: it atomically wraps an existing direct
  `Row.children` or `Column.children` child with one new stable ID. Existing
  Expanded nodes may move only between those direct flex destinations, and the
  required-child slot editor is replacement-only.
- The accepted const `flutter.widgets.Stack` slice exposes `alignment`,
  `textDirection`, `fit`, `clipBehavior` and an ordered any-widget `children`
  slot. Designer currently supports non-positioned children only. Palette,
  tree, Canvas, slot-editor and same-tree move routes share terminal append and
  compatibility admission.
- The accepted const `flutter.widgets.FractionallySizedBox` slice exposes
  optional physical/directional `alignment`, non-negative `widthFactor` and
  `heightFactor`, plus one optional any-widget `child`. Native and exact-Web
  Canvas use the real Flutter layout under bounded/unbounded and LTR/RTL inputs,
  while an IDE-only overlay retains selection and drop access for zero-size
  empty layouts.
- The accepted `Align` vertical slice completes `flutter.widgets.Align` across
  the catalog, model validation/codecs, Properties, Create,
  Palette/tree/native-Canvas DnD, deterministic Dart generation, Save/reopen and
  chronological Undo/Redo. The pinned Flutter 3.44.8 const contract exposes an
  optional `AlignmentGeometry alignment` with omitted `Alignment.center`
  default, nullable finite non-negative `widthFactor` and `heightFactor`, and
  one optional single any-widget `child`; `key`, raw Dart and unreviewed
  arguments remain excluded. New prototypes keep all three properties omitted.
  Native and exact-Web projections build the real Flutter `Align`, preserve
  physical versus directional alignment under LTR/RTL, and keep an IDE-only
  selectable/drop target for a zero-size empty widget. At the Align milestone the catalog had
  533 writable rows across 14 widgets. Fourteen sources across 16 any-widget
  and two trait-bound destinations form 252 DnD candidates: 226 accepted and 26
  rejected. Existing alignment, numeric and single-slot encodings are
  sufficient, so `.fd` schema v6, Catalog API 5, Canvas model v11 and version-1
  framing/control contracts do not change.
- The accepted `Opacity` vertical slice completes `flutter.widgets.Opacity`
  across the catalog, model validation/codecs, migration fixtures, Properties,
  Palette/tree/native-Canvas DnD, deterministic Dart generation, Save/reopen and
  chronological Undo/Redo. The pinned Flutter 3.44.8 contract contains required
  named finite `opacity` in inclusive `[0, 1]`, optional named
  `alwaysIncludeSemantics` with omitted default `false`, and one optional single
  any-widget `child`; `key`, raw Dart and unreviewed arguments remain excluded.
  New prototypes store only `opacity: 1.0` plus an empty child. Native and
  exact-Web Canvas projections build the real Flutter `Opacity`; zero opacity
  preserves hit testing, normally suppresses child semantics, and retains them
  only when `alwaysIncludeSemantics` is true. IDE-owned selection/hit/drop
  overlays remain outside the effect. At that milestone the catalog had 530
  writable rows across 13 widgets. Thirteen sources across 15 any-widget and two trait-bound
  destinations form 221 DnD candidates: 197 accepted and 24 rejected. Existing
  value kinds and payload shapes are sufficient, so `.fd` schema v6, Catalog API
  5, Canvas model v11 and version-1 framing/control contracts do not change.
- The completed shared typed asset/`Container.DecorationImage` slice advances
  form `.fd` documents to schema v6, contributor Catalog API to 5 and Canvas
  model payload to v11 over NBFC framing v1. `ImageProviderValue` is a closed logical
  app/package identity for `AssetImage` or `ExactAssetImage`, optionally wrapped
  once by bounded `ResizeImage`; `FileImage`, `MemoryImage`, `NetworkImage`,
  custom providers, filesystem paths, URLs and raw-Dart expressions remain excluded.
  `DecorationImageValue` covers all 13 Flutter 3.44.8 arguments and mode,
  20-value matrix, linear-to-sRGB gamma, sRGB-to-linear gamma and saturation
  `ColorFilter` variants, including reviewed theme colors for mode. Its
  positive-area `centerSlice` permits fit omitted, `fill`, `contain`,
  `fitWidth`, `fitHeight` or `scaleDown` and rejects `cover`/`none`; `onError`
  is one validated identifier for a compatible `(Object, StackTrace?)` handler.
  The accessible tabbed BoxDecoration editor selects only declared inventory,
  exposes typed provider/resize/filter/layout/paint controls and concrete status,
  and publishes one structured/dependent change as one chronological Undo/Redo
  unit. Schema v1-v5 forms migrate in memory with no image and become canonical
  v6 only after an admitted edit.
- The shared project asset resolver reads application/package `pubspec.yaml`
  declarations through `.dart_tool/package_config.json`, snapshots verified
  PNG/JPEG/GIF/WebP bytes and rejects absolute/backslash/traversal identities,
  symlink/root escape, bad magic/dimensions and bounded inventory violations.
  Variant choice exactly matches Flutter 3.44.8 framework revision
  `058e0af2c2b57e369d905a03ac9748b0ebf543c6`: exact DPR, endpoint clamping,
  upper neighbor below DPR 2.0, otherwise strict midpoint comparison with ties
  downward. Inventory defaults are 4,096 logical assets, 16 MiB/file, 64 MiB
  total, dimension 4,096 and 8,388,608 pixels. Only parsed, name-matched package
  roots receive recursive listeners; listener replacement/cleanup runs off the
  EDT behind a generation fence, and a changed watch set forces a fresh
  inventory before any image bytes are published.
- Negotiated `asset.imageBytes.v1` adds NBFC kind 4 `IMAGE_BYTES` after the
  revision-scoped model descriptor. A presentation projects referenced assets
  only (at most 256 logical assets, 256 resources and 16 MiB total); resource IDs are
  lowercase raw SHA-256 of immutable compressed bytes, and exact order, size,
  digest, format and dimensions are revalidated. Native and internal exact-Web
  runtimes both create `MemoryImage(bytes, scale: resolvedScale)`, optionally one
  `ResizeImage`, and the real `DecorationImage`. No filesystem path or callback
  identifier crosses Canvas. Authenticated media/decode/resize/center-slice
  failures quarantine only the affected resource; framing, identity, digest,
  ordering and exact model-resource coverage failures remain fatal. Unavailable
  or quarantined assets show a deterministic
  non-interactive placeholder/status with logical identity, issue code and
  reason while Container selection, layout, guides and drop overlays remain
  usable outside the decorated widget. Center-slice bounds mirror the pinned
  codecs: native preserves Flutter's asymmetric exact missing-axis arithmetic
  and explicit upscale, Web rounds a missing axis but forces decode no-upscale,
  and a `fit` result with a zero axis fails closed instead of being invented as
  one pixel.
- The complete `ElevatedButton` vertical slice expands the exact
  Palette/Canvas surface to ten widgets and the writable surface to 497 rows
  across nine non-`Scaffold` widgets. Its 286 typed leaves comprise seven direct
  fields, five 54-leaf default/disabled/pressed/hovered/focused style groups and
  nine common layout/feedback fields. Strict callback identifiers are never
  arbitrary Dart expressions; Canvas payload protocol v9 transmits only
  `callbackPresence` and cannot execute a project handler. Deterministic Dart
  uses direct sparse `ButtonStyle` state maps so omitted values fall through
  from the local button to `ElevatedButtonTheme` and Flutter defaults.
  `TextStyle.color` is intentionally excluded because effective text color is
  owned by `foregroundColor`; runtime-only keys, focus/state controllers and
  builders remain excluded. The closed editors include every
  `SystemMouseCursor`, six shape presets including `roundedSuperellipse`, and
  `InkSplash`, `InkRipple`, `InkSparkle` and `NoSplash`. Its optional-single
  required-named-nullable `child` slot emits `child: null` when empty. Ten
  sources across twelve any-widget and two `PreferredSizeWidget` destinations
  form 140 candidates: exactly 122 are admitted and 18 rejected. The `.fd`
  document schema remains v4.
- The earlier complete `AppBar` vertical slice expanded the exact Palette/Canvas surface
  to nine widgets. It exposes 120 typed, independently resettable Properties in
  nine enterprise groups and exact `leading`, `title`, `actions`,
  `flexibleSpace` and `bottom` slots. Deterministic Dart generation and the
  native runner assemble the same closed notification, shape, icon-theme,
  text-style and system-UI-overlay projections while omitted groups preserve
  `AppBarTheme` inheritance. Canvas payload protocol v7 fingerprints slot
  acceptance, and negotiated source-aware DnD binds an opaque token to the
  exact current widget type and traits before hover. `Scaffold.appBar` and
  `AppBar.bottom` accept only `PreferredSizeWidget`; the complete matrix has
  117 candidate cells, of which 101 are admitted and 16 rejected. At that
  milestone the writable surface was 211 rows across eight non-`Scaffold` widgets.
- The earlier complete capability-gated vertical slice added `Icon` as the eighth
  Palette/Canvas widget. It exposes the positional typed nullable `IconData`
  value and all 12 supported named constructor properties, for 91 writable
  property rows across the seven non-`Scaffold` widgets. Schema v4 stores
  `IconData` metadata without executable Dart expressions; Canvas payload
  protocol v6 renders the same real Flutter `Icon` arguments and theme-backed
  `IconTheme` inheritance as generated Dart; `blendMode` and `fontWeight` remain
  direct local arguments. The leaf has no slots. Its built-in chooser
  admits only **None** or an exact entry from the searchable bundled Material
  Icons registry of 8,825 entries locked to Flutter 3.44.8 and
  requires `flutter.uses-material-design: true` in the generated application.
  Eight draggable sources across the seven reviewed destination slots form the
  complete fail-closed 56-cell insertion matrix. The exported contributor
  contract advances to catalog API 3 for the new sealed `IconDataValue` kind;
  API-1 and API-2 contributors are rejected before loading. Legacy v1-v3 exact
  `Icons.<registeredName>` values migrate through the locked registry, and the
  searchable selector exposes its dynamic result status to assistive tools.
- A fail-closed capability gate now independently admits exact canonical
  built-ins to Properties, Canvas, Create and DnD and checks the complete Java
  schema fingerprint against an independently declared Dart runtime contract.
  The first complete expansion adds `SizedBox` end to end: nullable
  non-negative width/height Properties, exact `child` Slots management,
  dedicated SVG icons, strict model decoding, native rendering and zero-size
  selection overlay, deterministic generation, Palette/tree/Canvas insertion,
  stable-id-preserving same-tree movement, Save/reopen and Undo/Redo. At that
  milestone the Palette/Canvas surface was seven widgets and its compatibility
  matrix was 49 cells, including `SizedBox.child`.
- The earlier catalog-driven native Canvas insertion milestone covered all six CORE_V1 Palette widgets:
  `Scaffold`, `Column`, `Row`, `Padding`, `Center` and `Text`. Java preserves the
  exact one-shot Palette type through the native round trip and revalidates the
  complete 36-cell compatibility matrix before creating an `AddWidget` command.
  Flutter exposes only empty `Scaffold.body`,
  `Scaffold.floatingActionButton`, `Padding.child` and `Center.child` targets or
  terminal `Row/Column.children`; occupied, non-terminal, stale, replayed,
  trait-incompatible and non-CORE_V1 drops remain fail-closed.
- Project-wide Flutter theme foundations. Every newly created application now
  receives a strict versioned `.fd_templates/project.fdtheme`, deterministic
  hash-guarded `lib/theme/app_theme.dart`, built-in Light and Dark Material seed
  themes, `ThemeMode.system`, and transactional `MaterialApp` wiring while
  retaining Flutter's standard counter sample and widget test. Theme
  definitions are never stored in an individual `.fd` form.
- A NetBeans `Edit Flutter Themes...` workflow and dedicated `.fdtheme` file
  type. The custom-theme editor is a singleton `Themes` tab beside `Palette`,
  with Save/Reload, default mode, active light/dark references,
  Add/Duplicate/Remove, stable id, display name, brightness and a seed color
  chooser. Its compact `General`, `Colors`, `Typography` and `Components` tabs expose all 46
  supported non-deprecated Material `ColorScheme` roles and all 15 Material 3
  `TextTheme` roles, with 13 typed optional `TextStyle` fields per role, plus
  the closed 36-leaf Scaffold/AppBar/Icon/ElevatedButton color contract. Schema
  v2 adds `Enable project themes`; schema v3 adds an enable switch to every
  definition; schema v4 adds the typed role overrides; schema v5 adds component
  colors. Schema v1-v4 remains readable and explicit Save emits canonical v5. Disabled
  definitions remain in the descriptor and are omitted from generated Dart.
  Disabling project themes preserves the complete catalog while runtime and
  Canvas use Flutter defaults. Descriptor and generated Dart saves are
  exact-baseline, hash-conflict guarded and paired; manually changed generated
  Dart is never overwritten. Existing applications without the descriptor can
  explicitly initialize the same default pair when their `main.dart` still has
  the recognized Flutter template shape.
- Native Canvas theme synchronization now resolves the verified project theme
  shared by every Designer form, carries its exact id/ARGB seed/brightness,
  complete ColorScheme/TextTheme/component override tables and semantic digest over the
  isolated model protocol-v9 boundary, and assembles them in the same order as
  generated Dart: seed scheme, `ColorScheme.copyWith`, `ThemeData.from`, then
  `TextTheme.copyWith`. Form-local Text properties are applied last and remain
  intentional overrides. Open Canvas tabs coalesce theme file changes and
  re-render; a present invalid descriptor, missing generated Dart file or hash
  mismatch withdraws preview with the concrete reason. Older projects with no
  descriptor retain the bounded legacy preview and are not modified merely by
  opening a form.
- A `Target Platforms` step in the Flutter Application wizard with Recommended, Mobile, Desktop, Web, All, and custom selections. The exact non-empty canonical selection is passed to `flutter create --platforms=...` and every requested real directory is verified before the project is opened.
- Safe later platform scaffolding for existing Flutter applications through `Flutter > Add Flutter Platforms...` and the project context menu. Only absent canonical paths are offered, occupied directories/files/symbolic links are never overwritten, unsupported Flutter project types fail closed, and execution uses native Output, progress, cancellation, and postcondition checks.
- A capability-gated Windows inline editor for the selected existing
  `flutter.widgets.Text`. Double-click or F2 replaces the rendered label with a
  real Flutter `TextField`/`TextInputClient`; ordinary Enter inserts a newline,
  while Ctrl+Enter commits and Escape cancels only when the composing range is
  empty. Preedit never crosses into Java. The final
  `runner.textEditCommit` carries the exact session, presentation, document,
  revision, frame, layout, intent, interaction-fence and selected widget
  identity, the bounded well-formed Unicode text and `compositionObserved`.
  After `widget.inlineTextEdit.v1` negotiation, Java admits that one-shot event
  only for the current visible selected `Text` and maps it to at most one
  existing `SetProperty(data)` command; unchanged text is a no-op. Changed
  text preserves the established generation, analyzer, Pair Save and
  chronological Undo/Redo path. The AWT carrier has
  input methods disabled and does not relay `WM_IME`; Flutter owns native text
  input. Deterministic Flutter and Java codec/channel/session plus
  view/mutation-bridge tests are accepted. Physical CJK IME acceptance remains
  open because the current gate
  host has no composition-capable input method; this entry claims no
  Linux/macOS or runtime-faithful Web implementation.
- The first isolated native Flutter Canvas host foundation for each open `.fd` Design tab: versioned runner sources are packaged in the NBM, built outside the EDT, integrity-checked and cached by SDK/source identity, launched as a separate process, and attached as a verified Windows child window without PNG or pixel-frame transport.
- The first Windows native read-only Canvas projection: one bounded validated
  `.fd` revision is encoded as the canonical `CORE_V1` model and rendered by a
  hardcoded allowlist of exactly `Scaffold`, `Column`, `Row`, `Text`, `Padding`
  and `Center`. The Design toolbar selects Mobile, Tablet, Desktop or Web
  responsive viewport profiles, while stable widget IDs synchronize selection
  between the real Flutter surface and the read-only Explorer tree. The runner
  receives no project paths, Dart source or persistence authority. Later
  host-authorized Properties and DnD slices retain that boundary.
- Expanded `Text` Properties from ten direct arguments to 59 typed leaves in
  seven sets: Text, Accessibility, Locale and scaling, Text style, Paint and
  effects, Advanced typography and Strut style. NetBeans supplies bounded
  string and newline-list editors, optional boolean checkboxes, constrained
  numeric controls, closed enum lists, literal/theme-aware color editing, a
  transactional safe-subset `Paint` editor and ordered Shadow/OpenType tables.
  Deterministic Dart generation and the native Canvas share exact assembly into
  `TextStyle`, `StrutStyle`, `Locale.fromSubtags`, `TextScaler.linear` and
  `TextHeightBehavior`, including decoration combining and font fallback lists.
  Semantic Material `ColorScheme` and `TextTheme` roles follow the active
  project theme; explicit fields remain local overrides. The deprecated
  `Text.textScaleFactor` argument, `key` and arbitrary Dart/shader/filter graphs
  remain outside the closed typed slice.
- Added canonical Flutter Designer `.fd` schema v2 with typed `themeToken`,
  `paint`, `shadowList`, `fontFeatureList` and `fontVariationList` values.
  Existing v1 documents migrate in memory, canonical schema references advance
  to v2, malformed or future inputs remain fail-closed, and the next admitted
  edit persists canonical v2 without silently changing a file merely on open.
- Flutter Designer preview availability now follows the active project's real
  generated platform directories and preserves the exact adaptive target:
  Android exposes Android Phone/Tablet, iOS exposes iPhone/iPad, each desktop
  folder exposes its matching desktop target, and `web` exposes Web. Open Design
  tabs update immediately when platforms are added, deleted or renamed, retain
  the exact choice (then the same viewport mode), and fall back deterministically
  when it disappears. Android/iOS/macOS/Linux choices now reach Flutter's
  `ThemeData.platform` instead of being hardcoded to Windows. A project with no
  configured platform disables Preview. Web now renders its exact browser-sized
  responsive viewport through the native Canvas as an explicit layout preview;
  it does not claim `kIsWeb` or browser-only runtime fidelity.
- Native NetBeans 30 Flutter-project deletion through the standard confirmation dialog, with one shared Move/Delete data provider, exact plugin-owned metadata inventory for the keep-sources path, project-service shutdown, private-state cleanup, retry-safe lifecycle callbacks, and regression coverage that preserves Dart, Flutter Designer, and foreign metadata files.
- Pair-aware Flutter Designer form deletion from either visible `.dart` or `.fd` node. Only a complete, clean, writable mirrored pair is admitted; unsafe symlink/junction escapes and hard-linked identities fail closed. The operation closes an open clean shared editor, locks both paths deterministically, stages reversible private tombstones, removes both members, and exact-byte verifies any pre-commit rollback.
- Pair-aware Flutter Designer form rename from either visible `.dart` or `.fd` node. A canonical lower-snake-case target renames both mirrored files and updates only schema-v1 `source.dartFile`, preserving the exact Dart bytes, `source.className`, document identity, widget tree, canvas preferences, extensions, and managed-region hashes. Complete, clean, writable current-version pairs are staged under deterministic locks, verified at their target paths, and exact-byte restored on a pre-commit failure; this is an in-process rollback guarantee, not a durable crash-recovery journal.
- Pair-aware Flutter Designer form Copy/Paste from either visible `.dart` or `.fd` node. The first safe slice duplicates both physical members only in their existing mirrored relative folder and allocates one collision-free `_copy`, `_copy_2`, ... basename across both trees. It preserves the exact Dart bytes and every `.fd` semantic except the fresh `documentId` and retargeted `source.dartFile`; an open clean shared editor stays open. The clipboard publishes `NodeTransfer` plus the private pair paste provider without `LoaderTransfer` or operating-system file-list flavors. The two-file publish has verified in-process rollback and removes every still-owned exact staging artifact, but deliberately refuses to delete a path or bytes that lost transaction ownership and has no durable crash journal. Cross-directory Copy remains blocked until relative-URI rebasing is specified.
- Pair-aware Flutter Designer form Cut/Move from either visible `.dart` or `.fd` node. A one-shot custom `NodeTransfer.CLIPBOARD_CUT` paste moves the exact Dart and `.fd` bytes, with the basename unchanged, only between already existing mirrored folders owned by the same Flutter project. A bounded, fail-closed project Dart-directive proof rejects outgoing relative URIs, incoming or destination-binding references, ambiguous case/Unicode identities, unsafe package ownership, nested packages, aliases, links, collisions, read-only paths, unsaved editors, malformed configuration and changed evidence. Its final scan and commit share one EDT admission under exact proof-file locks and the real NetBeans 30 MasterFS child-cache mutexes for all proof/source/target folders and physical ancestors; another MasterFS shape fails closed. The operation closes a clean source editor, publishes `.fd` before Dart, locks both targets, retires both source identities through reversible `.nbmove` tombstones, and creates fresh target DataObjects. Exact rollback restores both sources before removing owned targets; if safe source recreation cannot be proved, the verified targets are retained for recovery. A failure after proven commit becomes an explicit recovery warning rather than a reusable Cut. The guarantee is in-process only, with no durable crash journal, and external non-NetBeans file changes remain a residual race. Generic DataObject Move stays disabled.
- The schema-v1 location contract now explicitly defines `AssetValue` paths as Flutter project/pubspec-root-relative, never `.fd`-relative, and requires opaque `extensions` metadata to remain location-independent. Pair Move therefore preserves both without interpretation or rebasing.

- The first NetBeans 30 Flutter Designer integration slice: mirrored `lib/<relative>/<name>.dart` ↔ `.fd_templates/<relative>/<name>.fd` pairing, a dedicated `Design`/`Source` MultiView, and a single Dart editor document shared with language services. `File > New File > Flutter Designer > Flutter Designer Form` now creates a canonical stateless starter pair atomically, accepts targets only inside `lib`, and mirrors every nested relative folder under `.fd_templates`.
- Theme-aware 16×16 file-type icons now distinguish ordinary and Designer-owned Dart sources from `.fd` models in the Projects and Files trees. The Dart Class and Flutter Designer Form entries in New File use the same matching icons, while the pair-aware Designer loader retains priority but declines every Dart source without its exact mirrored `.fd`. Such files are owned by the dedicated ordinary `DartDataObject`, which opens a standard `text/x-dart` `CloneableEditor` by double-click or the first `Open` context action and retains lexer highlighting, diagnostics, completion, navigation, refactoring, Quick Fixes and formatting without a Designer MultiView.
- At the earlier Icon milestone, the eight then-rendered Designer widgets received distinct semantic SVG icons shared by the Palette and Design tree: Scaffold, Column, Row, Padding, Center, SizedBox, Text and Icon. Each family includes reviewed 16×16 and 32×32 light/dark resources; unknown contributor widgets remain safely unmapped instead of receiving a misleading built-in identity.
- Guarded generated Dart regions with strict marker parsing in Dart's real lexical state, stable source offsets, marker round-trip persistence, and rejection of malformed, nested, unmatched, or duplicate markers. Marker-looking text inside strings and block comments remains ordinary Dart content.
- Fail-closed guarded persistence: an invalid, removed, renamed, overlapping, or otherwise unexpected live guard set cannot be saved as masked marker placeholders; the last known-good marker-bearing source is retained and the save is rejected.
- Unit and assembled-runtime gates for MIME/loader registration, orphan handling, both pair-open orders, late safe revalidation, an unsaved orphan buffer, the real `Design`/`Source` MultiView, Dart EditorKit reuse, guarded user edits, Save lifecycle, and marker persistence. The ordinary-Dart runtime gate now rejects fallback `DefaultDataObject` ownership and any test-only MIME substitution, and proves the Dart lexer, parser-error stripe, completion with auto-import, diagnostics with Quick Fix, definition, references, rename and LSP formatting against a real SDK.
- A NetBeans-independent immutable `.fd` domain model with exact numeric values, typed properties and named slots, plus deterministic metadata for the first ten Flutter widgets, explicit Dart constructor/enum library ownership, cross-target numeric emission guards, and bounded semantic validation with stable issue paths.
- A strict bounded streaming `.fd` version 1 codec with exact input snapshots, fail-closed read-only handling for completely parsed future versions, packaged JSON Schema parity, canonical UTF-8/LF output, stable diagnostics, deterministic key ordering and adversarial/golden round-trip coverage.
- Read-only NetBeans codec integration for the Designer `DataObject`: opening `Design` loads and validates `.fd` off the EDT with bounded streaming I/O, preserves the exact byte baseline, composes built-in and Lookup-provided widget catalogs, reports contributor/current/future/invalid/oversized/I/O states through an accessible non-modal surface, coalesces external-change storms, and stops background work after the last Design clone closes without modifying the Dart editor or either file.
- A separate catalog-contributor fixture NBM verifies the public Designer SPI across real NetBeans module classloaders, including specification dependency, default-Lookup discovery, shared API identity, composed extension widgets, and exactly one host-owned `flutter-designer.jar`.
- Bounded read-only Dart source-integrity verification for designer pairs: strict UTF-8 snapshots, exact `imports`/`build` marker topology, normalized SHA-256 checks, top-level stateless class/scope binding, stable conflict/unsupported/unavailable diagnostics, external Dart-file reloads, and exact byte baselines consumed by the installed transactional pair-save gate. Stateful and import-prefixed class binding remain explicitly unsupported instead of being guessed.
- Source-integrity hardening now caps Dart snapshots at a heap-practical 2 MiB, bounds nested interpolation, balances `{}`, `()` and `[]`, rejects generic-bound superclass spoofing and local Flutter-base shadows, preserves unsupported status under diagnostic truncation, and recovers the background reload lifecycle after fatal scanner errors.
- Cross-parser adversarial tests keep the NetBeans guarded reader and the NetBeans-independent source scanner aligned for BOM, LF/CRLF, strings, interpolation, block comments, malformed markers and ambiguous topology.
- A NetBeans-independent, bounded and deterministic Dart-region generator for the supported stateless version 1 widget catalog. It validates the complete model internally, emits both `imports` and `build` payloads atomically with canonical LF/UTF-8 output and normalized hashes, and fails closed for opaque Dart expressions or unsupported symbol bindings instead of copying unchecked source into generated regions.
- A read-only on-disk three-way integrity gate now compares independently recomputed `actual`, `.fd`-declared and generated hashes for both managed regions, reconstructs the candidate source in memory, and sends it back through the structural Dart scanner. The gate retains both exact input baselines and never exposes a write authorization. ADR-015 and ADR-016 now combine live-editor, analyzer and save-time TOCTOU evidence at the pair-save edge; that persistence evidence still does not enable Designer mutation without the command/probe/Undo boundary.
- A separate prospective Dart transition planner now accepts only a proven old three-way baseline, an exact live marker-bearing snapshot and successful new generation. It preserves every byte outside the two managed payloads, updates both prospective `.fd` hashes, re-scans the complete candidate, rejects stale/substituted evidence and oversized output, and publishes no write authorization. The first writable contract is deliberately limited to strict UTF-8, LF-only source without a BOM.
- A package-private NetBeans live-document bridge reconstructs the real marker-bearing bytes from the marker-masked `StyledDocument` without advancing the guarded persistence fallback. Its immutable snapshot binds document identity, `DocumentUtilities` revision, UTF-8 hash, guard identities and UTF-16 character ranges; the internal atomic apply path replaces `build` then `imports`, verifies the exact planned candidate and rolls both regions back on failure. It has no Designer command caller; the installed `PairSaveCoordinator` may invoke it only while holding the mandatory preparation lease for a staged pair Save.
- Candidate validation can now use an isolated `dart language-server --protocol=analyzer` overlay that never writes candidate content to disk. Bounded diagnostics, warning policy, cancellation, timeout, stale-content handling and navigation probes produce immutable `PASSED`, `REJECTED`, `STALE`, `UNAVAILABLE` or `TIMEOUT` evidence for one path/version/SHA snapshot. The current native protocol proves unique real-path target containment and optional target kind; it does not independently prove an export graph for the probe's expected library URI.
- A pure prepared-pair layer now revalidates the exact original `.fd`, stable document identity and source descriptors, canonicalizes and round-trips the prospective `.fd`, and publishes defensive copies of the exact old/new Dart and `.fd` bytes without granting write authority.
- A fail-closed NetBeans pair-save edge now binds the prepared pair to one loaded model identity, live guarded-document identity/version/SHA and exact `PASSED` analyzer evidence. It requires non-empty accepted Flutter symbol probes, including real `Widget` and `BuildContext` occurrences whose unique targets remain below the configured real Flutter SDK root. That exact root intentionally covers both framework sources and re-exported `dart:ui` declarations in `sky_engine`, but never trusts the SDK's parent directory.
- One stable pair-aware `SaveCookie` now owns ordinary Source saves, pre-apply preparation and staged pair saves. Preparation blocks Source-only Save during asynchronous analysis and rolls back the exact live snapshot when abandoned. Restore and lease release share one document-atomic barrier; after any observed apply/rollback mutation the exact predecessor deliberately remains dirty with the stable `SaveCookie` until an ordinary Source Save, so no destructive reload can erase a queued user edit. Guarded persistence uses an explicit prepare/commit/abort lifecycle, while pair writes acquire both files in canonical path order, recheck exact baselines, write Dart then `.fd`, reread both results, and perform reverse verified rollback. Exact owned file events are suppressed; foreign events become a conflict and retain the save owner. If editor finalization fails after a durable commit, guarded state is adopted only after an EDT-atomic clean exact-byte proof; a newer or unverified live edit is retained with the same `SaveCookie` in `RECOVERY_CONFLICT` and is never reloaded away.
- The deterministic generator now publishes a source-ordered occurrence manifest for every emitted Flutter type token. A strict planner maps those region-relative UTF-16 offsets through the exact prospective candidate, including non-BMP user-owned prefixes, and the pair-save gate requires exact ordered analyzer evidence for the complete manifest instead of accepting caller-selected probes.
- The Dart source scanner now retains an exact byte/UTF-16 occurrence for the unique unqualified top-level Designer class `StatelessWidget` superclass. Pair analysis requires that scanner-owned `package:flutter/widgets.dart` class probe together with the complete generator-owned manifest and rejects qualified, shadowed, nested, stateful or identity-substituted evidence.
- Pair preparation now analyzes exact candidate C before touching live predecessor B. A globally unique one-shot overlay ticket binds the loaded/prepared identities, coordinator-bound real Dart path, configured real Flutter SDK root, bytes, hash, probe manifest and analyzer result; only passing evidence can enter one non-interleavable EDT compare-and-set, managed-region apply and separate applied-live binding. Reservation re-reads both disk baselines across a fixed external-event epoch, so a clean event cannot be adopted as fresh authority for stale `Current`. Rejected, cancelled, replayed, transferred or stale analysis leaves the Dart document version, modified state and Undo history unchanged.
- A bounded pure Designer command foundation now models Add, Remove, Move, Wrap, Set Property and Reset Property with semantic validation, immutable exact before/after pair revisions, exact inverse Undo/Redo, saved-cursor and branch semantics, and distinct paired versus `.fd`-only persistence classifications. A stable NetBeans combined-Undo identity is present but remains disconnected from writable UI.
- Dirty command revisions can now be pinned by an identity-bound durable save lease that freezes commands and cursor moves until explicit adoption, abort or invalidation. The internal `FD_ONLY` path requires the exact loaded catalog identity plus complete writable validation/source/three-way facts, locks and verifies both Dart/`.fd` baselines, writes exactly one `.fd` candidate, preserves the Dart document identity/version/modified/Undo state, and re-anchors the command session only after a verified commit; stale or uncertain outcomes freeze the unsafe session.
- Pair state and `SaveCookie` effects now drain through an ordered queue whose monitor is held only for enqueue/dequeue. NetBeans listeners, `CookieSet`, and `DataObject` callbacks run outside coordinator/effects locks; reentrant and cross-thread publication cannot deadlock the active callback, and even a fatal presentation-listener failure is logged without escaping, stranding the drainer or changing the completed semantic result.
- `Source` and `Design` now expose the same stable Undo/Redo identity with NetBeans 30's native editor manager as the only public delegate. A Designer command session owns only an exclusive lifetime token and never replaces the Source history or installs a competing listener. Internal document barriers coalesce presentation notifications; semantic callbacks drain independently after locks are released, including when another callback fails.
- Added the native chronological Designer-edit foundation: a Dart MIME `UndoableEditWrapper`, one non-merging semantic edit carrying stable model revision ids, EDT/document-identity capture fencing, exact branch-truncation cleanup, and `AtomicLockDocument.atomicUndo()` rollback which produces no phantom native Undo entry on failed apply or finalization. Writable Designer actions remain gated until this foundation is joined to pair replay and Save.
- Staged Pair Save now accounts for NetBeans 30's private pre-output Undo savepoint: any non-committed result after entering `CloneableEditorSupport.saveDocument()` invalidates staged command authority in a sticky conflict, even when the filesystem transaction wrote nothing or verified a full rollback. Native Source history is retained untouched; no `discardAllEdits()` workaround is used on this failure path.
- Command derivation now publishes an identity-bound pending C1-to-C2 lease without moving the owned cursor from the exact C1 session or discarding its redo branch. The lease binds the exact predecessor, candidate, edit and catalog identities; blocks another command, Undo/Redo and durable Save; and requires explicit staged adoption, safe abort or fail-closed invalidation. Staged adoption has a monitor-only swap plus one-shot deferred callback publication so a future coordinator can release document/coordinator locks first. The lease performs no analyzer, editor, filesystem or persistence operation and remains internal while writable Designer UI is disabled.
- The pair coordinator can now replace an exact staged `PAIRED` C1 revision with its analyzed C2 successor without changing durable baseline B. It claims the pending command before deriving the pure transition, fences independent lease resolution, analyzes before live mutation, and publishes the document, staged evidence and command cursor as one C2 outcome. Rejection preserves byte- and identity-exact C1; a failed apply restores C1 and binds fresh live evidence; an external event or user edit that prevents proof clears staged authority, invalidates the command session and preserves the user content in conflict. Command-binding and pair/cookie callbacks share one deferred queue and cannot observe a half-published result.
- Command generation, generated-region evidence, command revisions, analyzer tickets/requests and analyzer limits now share one identity-bound Dart candidate capacity profile. The generator rejects the 257th total probe before publishing a command, the planner has no independent hard-coded limit, candidate bytes share the same 2 MiB policy, and a detached analyzer policy fails before process startup.
- Successful paired Save now preserves NetBeans native Undo history and re-anchors retained semantic endpoints by stable logical revision id. Saved `C1` becomes the exact CES/command `BASELINE`; Undo exposes former durable `B` as an analyzer-free `FORMER_DURABLE` pair and Redo returns cleanly to `C1`. The longer saved `B→C1→C2` graph also survives a complete two-step Undo/Redo round trip: historical `C1` uses analyzer-free `REANCHORED_ANALYZED` authority, `B` uses `FORMER_DURABLE`, and durable `C2` bytes never change. A normal or finalization-racing unmanaged Source `S2` remains chronologically above that edge and dirty, while stale controller generations, false clean markers and post-commit split authority fail closed without partially adopting the command cursor. Saved-Current, command and Pair effects publish in EDT order after their joint identity checks.
- Ordinary Source Save can now preserve that saved semantic graph. An identity-bound `SourceAnchorLease` pins the exact candidate and blocks commands, Undo/Redo and competing saves; re-anchoring requires byte-exact unchanged managed payloads rather than normalized-hash equality. A committed `S2` becomes the new durable command baseline, retained revisions rebuild their proof identities against it, and a native-history overlay keeps the exact `C1` underlay so chronology remains `S2→C1→B→C1→S2`. Controller `Current`, command session, disk baseline, edge graph and cursor adopt together before controller/command/Pair effects publish. Every failed outcome after CES entry, or committed split-authority risk, clears semantic authority into sticky conflict without calling `discardAllEdits()` or overwriting newer Source content.
- Pair Save now preserves both semantic and unmanaged Source-envelope coordinates after that overlay: `(C2,S2)→(C1,S2)→(C1,S0)→(B,S0)` and full Redo. Re-anchoring maps exact endpoint identities, projects the new anchor's managed bytes into each retained unmanaged envelope before semantic derivation, and binds endpoint-specific pair proofs. A following Source Save repeats that projection for all variants, preserving `C2/S3→C2/S2→C1/S2→C1/S0→B/S0` and back. Unique physical Dart-plus-`.fd` candidates are bounded incrementally before CES/transaction. History-aware Source `UNCHANGED` keeps the same command revision, loaded `Current` and edges while refreshing only the clean CES cursor; byte-identical native positions retain their real savepoint dirty state. Trimming the last semantic edit retains its exact owner for re-anchor, while a closed zero-edge owner is retired under exact baseline evidence without discarding native Undo history; a close racing after owner selection revalidates that proof and re-enters exactly one ordinary CES Save.
- Pre-persistence staged-pair invalidation no longer uses a global native-history discard. If the pinned Designer candidate loses exact live authority, the coordinator atomically clears only its semantic graph, invalidates the exact durable command lease outside the coordinator lock, retains the newer user-owned Source bytes and native Undo/Redo cursor, and keeps the stable `SaveCookie` in a sticky `RECOVERY_CONFLICT` without starting pair I/O. Regression coverage performs the retained Source Undo/Redo and proves that exact candidate bytes cannot revive the invalidated authority.
- Packaged NetBeans 30 coverage now proves one shared Design/Source Undo identity and the public CES dirty→Save→Undo→Redo savepoint lifecycle. The strict release verifier and isolated clean-install lifecycle pass through activation, SDK auto-discovery, project reopen, disable, uninstall and fresh-cache cleanup with no critical log entries or plugin-owned ordering warnings.
- A new Designer command may now branch from an exact noncanonical saved-history endpoint such as `C1/S0`. An opaque staged command-source token binds the logical owner, endpoint-specific `SavedHistoryProof`, live identity, monotonic NetBeans document version and coordinator epochs; even edit-to-exact-revert ABA is rejected before replacement publication. The pending lease derives and pins `C3/S0` from that exact pair until joint analyzer/document/pair/command adoption. The generalized replacement/recovery path accepts analyzer-free saved predecessors without fabricating analyzer evidence, rejects canonical-pair or stale-token substitution without mutation, preserves the older native semantic graph across an atomically rolled-back failed apply, and counts the candidate with all physical history variants before analyzer or CES work. Adoption preserves durable `C2/S2`, truncates the obsolete `S2/C2` redo suffix, installs the exact `B/S0→C1/S0→C3/S0` branch, and keeps `S0` sticky for the following ordinary command.

- At that milestone, the active Flutter Designer `Design` lookup published the standard NetBeans Palette filtered by the exact Create capability to `Scaffold`, `AppBar`, `Column`, `Row`, `Padding`, `Center`, `SizedBox`, `Text`, `Icon` and `ElevatedButton`. Stable-ID tree/Canvas selection drove standard selected-Node Properties: 497 catalog-backed fields were writable across the nine non-`Scaffold` widgets, including the 286-leaf ElevatedButton, 120-leaf AppBar, 59-leaf Text and 13-property Icon projections. The historical first DnD vertical slice admitted only built-in `Text`, followed by the six-, seven-, eight- and nine-source matrices; all were superseded at that milestone by the ten-source capability-gated matrix described above. The widget catalog remained the Java authority for source type, traits, slot cardinality and acceptance. Palette and Properties opened once on the first Design activation without taking focus from the editor.

### Fixed

- Transform insertion and Matrix4 property edits no longer fail pair-save with
  `UNTRUSTED_NAVIGATION_TARGET`. Generated symbol evidence now records
  `Matrix4` under its real `package:vector_math/vector_math_64.dart` owner even
  though Flutter's `widgets.dart` re-exports it. The pair-save analyzer keeps
  its existing Flutter-SDK trust boundary and simply excludes this external
  occurrence from Flutter-owned probes instead of broadening trusted roots.
- Property-only Designer revisions now refresh the existing selected Explorer
  `Node` and its stable `Node.Property` instances in place. The standard
  NetBeans PropertySheet therefore retains its active editor, selected row,
  keyboard focus, scroll position and tab instead of reloading every property
  set for the intermediate `APPLYING` and confirmed `READY` snapshots. Exact
  named repaint events make each cell read the atomically swapped immutable
  values and revision-bound mutation handlers; real widget-tree topology or
  catalog-schema changes still use a complete Explorer rebuild.
- Catalog-backed boolean Properties now render and edit explicit `true`/`false`
  values as native checkboxes across every supported widget instead of exposing
  the editor tags as a combo box. Optional constructor arguments retain the
  distinct `<not set>` state and return to it through **Restore Default**;
  unchecked therefore remains the explicit Flutter value `false`.
- Native Windows Canvas activation now joins the Java caller to both the exact
  AWT parent input queue and the verified `FLUTTERVIEW` queue before `SetFocus`,
  then detaches them in reverse order and revalidates HWND, PID, thread and
  physical foreground authority. This closes the initial activation race where
  the NetBeans top-level HWND was foreground but the cross-process Flutter child
  could not receive focus. Foreign-foreground, partial-attach, detach-failure
  and identity-drift paths remain fail-closed and cannot turn a retained
  activation intent into a later focus steal.
- At the schema-v2 milestone, the first Pair Save after editing a schema-v1 `.fd` model re-anchored
  retained semantic history to the proven canonical schema-v2 revision instead
  of mixing the raw v1 baseline into a new durable endpoint. Repeated Properties
  edits can therefore Save and traverse Undo history without the former
  `physical history variant` / `durable anchor` derivation error; exact
  historical Dart envelopes and strict byte-identity checks remain intact.
- Run-target and emulator discovery is now intersected with the real platform scaffolding of the active Flutter application. An Android-only project no longer advertises Windows, Web, or iOS targets, stale toolbar selections are revalidated before Run/Debug, non-application project types fail closed, and a successful `Add Flutter Platforms...` refreshes the target combo immediately.

### Changed

- The Flutter Designer footer is now a single compact status row. Normal state shows only `Designer ready.` and the current Canvas summary; full model/source/hash diagnostics remain available through tooltips and accessibility metadata, while the existing Canvas `Details...` dialog remains reserved for failed or unavailable native rendering.
- `.dart` is the technical primary NetBeans entry for a designer pair while `.fd` remains the canonical visual-model source of truth. Dart-only Save As remains withheld until a dedicated pair-aware Save As flow is implemented.
- Designer mutation remains closed except for the admitted revision-bound 497-property Set/Reset path, selected-widget Delete, same-tree compatibility-planned move/reorder, exact-slot management and the separately fenced catalog-driven insertion path for the ten DnD-capable Palette sources. Insertion accepts exactly 122 of the 140 reviewed source/destination cells, with trait-bound AppBar slots enforced on both host and Canvas. Scanner/generator probes, analyzed replacement, exact rollback/rebind, native Source/model replay, Pair/Source Save re-anchoring, the isolated native Canvas host and the NetBeans 30 runtime/release gate remain authoritative; `Scaffold` Properties, unreviewed Palette definitions and unreviewed object graphs stay disabled.
- Moved the `.fd` MIME resolver away from NetBeans 30's built-in position `350` and left the Dart `UndoableEditWrapper` unpositioned with the other heterogeneous Dart MIME services, eliminating both plugin-owned layer-ordering warnings found by isolated install smoke.
- Corrected pre-release version 1 schema bounds before a codec ships: widget type ids now accept the intended 1–255 characters, enum type names require non-empty dot-separated Dart identifiers, and the reserved Dart identifier `Function` is no longer accepted as a generated class name.

## [0.1.2] - 2026-08-25

### Added

- Deterministic controller coverage and lifecycle hardening for Run and Debug startup, cancellation, confirmed restart, Stop, process exit, project close, stale completions, and exactly-once progress cleanup.
- DevTools lifecycle coverage for connected-URL reuse, explicit Stop, owning Flutter-session termination or replacement, project close, process failure, and suppression of stale callbacks and Output writes.
- An assembled NetBeans 30 runtime integration gate for module activation, Dart MIME/editor/LSP/DAP registrations, and Flutter action resolution.
- An optional real-Dart-SDK editor end-to-end gate inside the assembled NetBeans 30 runtime, covering the EditorRegistry/LSP lifecycle, diagnostics and Quick Fixes, completion auto-imports, navigation/refactoring requests, formatting, Analysis Server restart, and project-close cleanup.
- Native NetBeans Build, Clean, and Clean and Build project actions. Build follows the selected Desktop/Mobile/Web toolbar target, while Clean and Build runs its validated `flutter clean` and target-specific `flutter build` stages under one cancellable lifecycle.
- A strict release verifier for fresh Surefire/Failsafe results, package metadata and licensing, NBM/update-catalog hashes, installed update tracking, and activation logs.
- A headless isolated NetBeans 30 smoke runner: clean-install activation of `0.1.2`, activated `0.1.1` baseline and offline exact-payload verification after updating to `0.1.2`, with strict process ownership and cleanup checks.

### Changed

- Started the 0.1.2 stability milestone with automated lifecycle and release verification gates for NetBeans 30.
- The Dart Analysis Server client version is now checked against the Maven project version during every test run.
- The Dart LSP compatibility stream now consumes only id-less custom `$/analyzerStatus` notifications that the generic NetBeans 30 client cannot handle. Standard LSP traffic and unrelated custom requests or notifications continue through the existing protocol path unchanged.
- Flutter projects now provide project-owned `AuxiliaryConfiguration` and `AuxiliaryProperties` services. Private NetBeans metadata is stored as one transient, slash-free `dev.flutter.netbeans.projectMetadata` attribute on the project root, is skipped by attribute-aware copy operations, and is removed with the project directory. A native NetBeans move/rename hook first flushes project preferences and then preserves that state, including quarantine attributes, through a bounded, explicitly typed, append-only `PREPARED`/`TARGET_READY`/`COMMITTED` handoff carried inside the project. Phase files have a transaction UUID, enforce real-path containment, use a DTD-free streaming parser with in-parse limits, and are read back before use. Restored values are verified before source deletion; a durable target intent is recovered idempotently, while an ambiguous target-side `PREPARED` phase is preserved and blocks Flutter services and unsafe project actions until Rename supplies the missing target name. `COMMITTED` recovery performs cleanup only and never replays an older metadata snapshot. Rename persists a NetBeans display-name override without changing the Flutter package name in `pubspec.yaml`. Shared metadata at `.netbeans/flutter-metadata.xml` is created only by an explicit shared write. Legacy private fallback attributes are migrated or safely quarantined when a project opens; oversized or non-XML values use verified raw slash-free quarantine attributes, removing `Ordering` warnings without discarding malformed persisted state.
- Heterogeneous Dart and pubspec MIME services no longer publish meaningless shared layer positions. This removes NetBeans 30 duplicate and partial folder-ordering warnings without changing service lookup semantics.

## [0.1.1] - 2026-08-24

### Added

- Project-scoped Flutter DevTools launch through `Flutter > Open DevTools` for an active Run or Debug session with an available VM Service URI.
- `Flutter > Stop DevTools`, a dedicated DevTools Output tab, native cancellable progress, and automatic server cleanup when the owning Flutter session stops or is replaced or its project closes.
- Reuse of an already running DevTools server: invoking Open DevTools again reopens its connected URL instead of starting a duplicate process.
- Complete Apache License 2.0 and NetBeans plugin metadata in the generated NBM package, with integration checks for the packaged metadata and license.

### Changed

- DevTools is started from the configured Dart SDK on loopback with an automatically assigned port and is opened through the browser configured in NetBeans.
- Flutter project command resolution now respects the project owning the active file or editor before falling back to the main or sole open project.

## [0.1.0] - 2026-08-24

### Added

- Initial Apache NetBeans 30 support for Flutter/Dart SDK discovery and configuration, Flutter project creation and recognition, and Dart-class creation.
- Dart syntax highlighting, typing indentation, Analysis Server diagnostics, completion, navigation, refactoring, formatting, Quick Fixes, and import assistance.
- Desktop, Mobile, and Web target selection, Android Device Manager, configured-emulator launch, managed Flutter Run/Debug sessions, DAP debugging, Hot Reload, Hot Restart, and Stop.
- Native NetBeans integration for Pub Get, Analyze, Flutter tests/Test Results, and `pubspec.yaml` completion and diagnostics.
