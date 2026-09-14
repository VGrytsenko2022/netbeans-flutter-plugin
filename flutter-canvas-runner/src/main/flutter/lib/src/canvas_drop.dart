import 'canvas_model.dart';

/// Resolves one normalized native surface point to a semantic drop target.
typedef CanvasDropResolver =
    CanvasDropTarget? Function(
      int surfaceXMicros,
      int surfaceYMicros, [
      CanvasPaletteDragSource? source,
    ]);

typedef LegacyCanvasDropResolver =
    CanvasDropTarget? Function(int surfaceXMicros, int surfaceYMicros);

/// Host-authoritative identity of the single Palette source currently being
/// dragged over the native Canvas.
///
/// The opaque token is still consumed and revalidated by Java before a model
/// mutation. Flutter receives only the reviewed type and traits required to
/// hide incompatible semantic drop zones.
class CanvasPaletteDragSource {
  CanvasPaletteDragSource({
    required this.token,
    required this.widgetType,
    required Set<String> traits,
  }) : traits = Set.unmodifiable(traits);

  final String token;
  final String widgetType;
  final Set<String> traits;
}

enum CanvasDropAcceptanceKind { any, requiredTrait, exactTypes }

/// Closed source-type acceptance rule for one reviewed slot.
class CanvasDropAcceptance {
  const CanvasDropAcceptance.any()
    : kind = CanvasDropAcceptanceKind.any,
      requiredTrait = null,
      exactTypes = const {};

  const CanvasDropAcceptance.requiredTrait(this.requiredTrait)
    : kind = CanvasDropAcceptanceKind.requiredTrait,
      exactTypes = const {};

  const CanvasDropAcceptance.exactTypes(this.exactTypes)
    : kind = CanvasDropAcceptanceKind.exactTypes,
      requiredTrait = null;

  final CanvasDropAcceptanceKind kind;
  final String? requiredTrait;
  final Set<String> exactTypes;

  bool accepts(CanvasPaletteDragSource source) => switch (kind) {
    CanvasDropAcceptanceKind.any => !source.traits.contains(canvasSliverWidgetTrait),
    CanvasDropAcceptanceKind.requiredTrait => source.traits.contains(
      requiredTrait,
    ),
    CanvasDropAcceptanceKind.exactTypes => exactTypes.contains(
      source.widgetType,
    ),
  };
}

const canvasPreferredSizeWidgetTrait = 'flutter.widgets.PreferredSizeWidget';
const canvasAnyDropAcceptance = CanvasDropAcceptance.any();
const canvasSliverWidgetTrait = 'flutter.widgets.Sliver';
const canvasSliverConstrainedCrossAxisType = 'flutter.widgets.SliverConstrainedCrossAxis';
const canvasSliverCrossAxisExpandedType = 'flutter.widgets.SliverCrossAxisExpanded';
bool isCanvasSliverCrossAxisExpandedDestination(String parentType, String slotName) =>
    parentType == 'flutter.widgets.SliverCrossAxisGroup' && slotName == 'slivers';
bool isCanvasPrototypeSliverType(String type) => const {
  'flutter.widgets.SliverPrototypeExtentList',
  'flutter.widgets.SliverPrototypeExtentList.builder',
  'flutter.widgets.SliverPrototypeExtentList.delegate',
}.contains(type);
bool isCanvasDynamicSliverType(String type) => isCanvasPrototypeSliverType(type) || const {
  'flutter.widgets.SliverVariedExtentList',
  'flutter.widgets.SliverVariedExtentList.builder',
  'flutter.widgets.SliverVariedExtentList.delegate',
  'flutter.widgets.SliverFixedExtentList',
  'flutter.widgets.SliverFixedExtentList.builder',
  'flutter.widgets.SliverFixedExtentList.delegate',
  'flutter.widgets.SliverList.builder',
  'flutter.widgets.SliverList.separated',
  'flutter.widgets.SliverList.delegate',
  'flutter.widgets.SliverGrid.builder',
  'flutter.widgets.SliverGrid.list',
  'flutter.widgets.SliverGrid.delegate',
}.contains(type);
bool isCanvasSliverVisibilityType(String type) =>
    type == 'flutter.widgets.SliverVisibility' || type == 'flutter.widgets.SliverVisibility.maintain';
bool isCanvasSliverWidgetType(String type) => isCanvasSliverVisibilityType(type) || isCanvasDynamicSliverType(type) || const {
  'flutter.widgets.SliverMainAxisGroup',
  'flutter.widgets.SliverCrossAxisGroup',
  canvasSliverConstrainedCrossAxisType,
  canvasSliverCrossAxisExpandedType,
  'flutter.widgets.SliverToBoxAdapter',
  'flutter.widgets.SliverPadding',
  'flutter.widgets.SliverOpacity',
  'flutter.widgets.SliverAnimatedOpacity',
  'flutter.widgets.SliverFadeTransition',
  'flutter.widgets.SliverLayoutBuilder',
  'flutter.widgets.DeviceOrientationBuilder.sliver',
  'flutter.widgets.ListenableBuilder.sliver',
  'flutter.widgets.AnimatedBuilder.sliver',
  'flutter.widgets.ValueListenableBuilder.sliver',
  'flutter.widgets.TweenAnimationBuilder.sliver',
  'flutter.widgets.SliverPersistentHeader',
  'flutter.widgets.SliverFloatingHeader',
  'flutter.material.SliverAppBar',
  'flutter.material.SliverAppBar.medium',
  'flutter.material.SliverAppBar.large',
  'flutter.widgets.PinnedHeaderSliver',
  'flutter.widgets.SliverResizingHeader',
  'flutter.widgets.SliverIgnorePointer',
  'flutter.widgets.SliverOffstage',
  'flutter.widgets.SliverSafeArea',
  'flutter.widgets.SliverFillRemaining',
  'flutter.widgets.SliverFillViewport',
  'flutter.widgets.SliverFillViewport.delegate',
  'flutter.widgets.SliverList',
  'flutter.widgets.SliverGrid',
  'flutter.widgets.SliverGrid.extent',
}.contains(type);
const canvasSliverDropAcceptance = CanvasDropAcceptance.requiredTrait(
  canvasSliverWidgetTrait,
);
const canvasPreferredSizeDropAcceptance = CanvasDropAcceptance.requiredTrait(
  canvasPreferredSizeWidgetTrait,
);

/// Resolves one Java-host-authorized move placement to current Flutter
/// geometry. Compatibility is intentionally not decided here: the exact
/// source, parent, slot and index have already passed the host catalog matrix.
typedef CanvasMovePreviewResolver =
    CanvasDropTarget? Function(
      String sourceWidgetId,
      String parentWidgetId,
      String slotName,
      int insertionIndex,
    );

/// Visual meaning of one semantic insertion zone on the Canvas.
///
/// Existing-widget moves are deliberately distinct from palette insertion and
/// the blue selection outline, while preserving the same exact zone geometry.
enum CanvasDropIndicatorKind { paletteInsertion, widgetMove }

/// Structural rules shared by Flutter geometry and protocol validation.
///
/// A widget type must still opt into one of these rules explicitly. Merely
/// having a slot named `child` does not make that widget a drop container.
class CanvasDropSlotSemantics {
  const CanvasDropSlotSemantics.append({
    required this.slotName,
    required this.maximumChildren,
    this.overlapPriority = 0,
    this.acceptance = canvasAnyDropAcceptance,
    this.zonePlacement = CanvasDropZonePlacement.terminalList,
    this.wrapsExistingChild = false,
  }) : cardinality = CanvasDropSlotCardinality.list,
       assert(maximumChildren > 0);

  const CanvasDropSlotSemantics.emptySingle({
    required this.slotName,
    this.zonePlacement = CanvasDropZonePlacement.fullNode,
    this.overlapPriority = 0,
    this.acceptance = canvasAnyDropAcceptance,
    this.wrapsExistingChild = false,
  }) : maximumChildren = 1,
       cardinality = CanvasDropSlotCardinality.single;

  const CanvasDropSlotSemantics.wrapExisting({
    required this.slotName,
    this.overlapPriority = 0,
    this.acceptance = canvasAnyDropAcceptance,
  }) : maximumChildren = 10000,
       cardinality = CanvasDropSlotCardinality.list,
       zonePlacement = CanvasDropZonePlacement.existingChild,
       wrapsExistingChild = true;

  const CanvasDropSlotSemantics.wrapExistingSingle({
    required this.slotName,
    this.overlapPriority = 0,
    this.acceptance = canvasAnyDropAcceptance,
  }) : maximumChildren = 1,
       cardinality = CanvasDropSlotCardinality.single,
       zonePlacement = CanvasDropZonePlacement.existingChild,
       wrapsExistingChild = true;

  final String slotName;
  final int maximumChildren;
  final CanvasDropSlotCardinality cardinality;
  final CanvasDropZonePlacement zonePlacement;
  final int overlapPriority;
  final CanvasDropAcceptance acceptance;
  final bool wrapsExistingChild;

  bool get fillsEmptySingleChild =>
      cardinality == CanvasDropSlotCardinality.single;

  String get modelSlotKind => switch (cardinality) {
    CanvasDropSlotCardinality.single => 'single',
    CanvasDropSlotCardinality.list => 'list',
  };

  int? insertionIndexFor(int currentChildCount) {
    if (wrapsExistingChild) {
      return null;
    }
    if (currentChildCount < 0 || currentChildCount >= maximumChildren) {
      return null;
    }
    if (fillsEmptySingleChild && currentChildCount != 0) {
      return null;
    }
    return fillsEmptySingleChild ? 0 : currentChildCount;
  }

  bool accepts({required int currentChildCount, required int insertionIndex}) =>
      wrapsExistingChild
      ? insertionIndex >= 0 && insertionIndex < currentChildCount
      : insertionIndexFor(currentChildCount) == insertionIndex;

  bool acceptsSource(CanvasPaletteDragSource source) =>
      acceptance.accepts(source);
}

enum CanvasDropSlotCardinality { single, list }

/// How the Flutter render tree exposes one reviewed semantic slot.
enum CanvasDropZonePlacement {
  menuItemChild,
  listTileLeading,
  listTileTitle,
  listTileSubtitle,
  listTileTrailing,
  badgeLabel,

  /// The complete rendered parent is the insertion target.
  fullNode,

  /// Only the visual terminal edge of a Row, Column, ListBody, ListView,
  /// row-major GridView, or adaptive OverflowBar is exposed.
  terminalList,

  /// The rendered bounds of one existing direct list child.
  existingChild,

  /// A concise lower-right target reserved for Scaffold's FAB slot.
  bottomRightCompact,

  /// Logical leading portion of an AppBar toolbar.
  appBarLeading,

  /// AppBar title portion between leading and actions.
  appBarTitle,

  /// Logical terminal edge of AppBar.actions.
  appBarActions,

  /// Full AppBar body behind the toolbar-specific zones.
  appBarFlexibleSpace,

  /// Bottom band occupied by AppBar.bottom.
  appBarBottom,

  /// Lower title band of a native FlexibleSpaceBar.
  flexibleSpaceBarTitle,
}

const canvasChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
);

const canvasSliversAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'slivers',
  maximumChildren: 10000,
  zonePlacement: CanvasDropZonePlacement.fullNode,
  acceptance: canvasSliverDropAcceptance,
);

const canvasPrototypeItemDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'prototypeItem',
);
const canvasSliverPaddingDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'sliver',
  acceptance: canvasSliverDropAcceptance,
);

const canvasFlexWrapDropSlot = CanvasDropSlotSemantics.wrapExisting(
  slotName: 'children',
  overlapPriority: 6,
);

const canvasExpandedWrapDropSlot = canvasFlexWrapDropSlot;

const canvasFlexibleWrapDropSlot = canvasFlexWrapDropSlot;

const canvasExpandedWidgetType = 'flutter.widgets.Expanded';

const canvasFlexibleWidgetType = 'flutter.widgets.Flexible';

const canvasSpacerWidgetType = 'flutter.widgets.Spacer';

const canvasSafeAreaWidgetType = 'flutter.widgets.SafeArea';

const canvasDirectionalityWidgetType = 'flutter.widgets.Directionality';

bool isCanvasFlexParentDataWidgetType(String widgetType) =>
    widgetType == canvasExpandedWidgetType ||
    widgetType == canvasFlexibleWidgetType;

bool isCanvasFlexRestrictedWidgetType(String widgetType) =>
    isCanvasFlexParentDataWidgetType(widgetType) ||
    widgetType == canvasSpacerWidgetType;

bool isCanvasRequiredChildWrapperWidgetType(String widgetType) =>
    !isCanvasFlexParentDataWidgetType(widgetType) &&
    isCanvasReviewedRequiredChildWrapperWidgetType(widgetType);

bool isCanvasPaletteWrapperWidgetType(String widgetType) =>
    isCanvasFlexParentDataWidgetType(widgetType) ||
    isCanvasRequiredChildWrapperWidgetType(widgetType);

const canvasStackChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
  zonePlacement: CanvasDropZonePlacement.fullNode,
);

/// IndexedStack children overlap, so the complete rendered stack is the
/// deterministic append zone rather than a visually ambiguous terminal edge.
const canvasIndexedStackChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
  zonePlacement: CanvasDropZonePlacement.fullNode,
);

/// Wrap can break children into multiple runs, so there is no stable linear
/// terminal edge. The complete rendered Wrap is the deterministic append zone.
const canvasWrapChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
  zonePlacement: CanvasDropZonePlacement.fullNode,
);

const canvasEmptyChildDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'child',
);

// A proportional corner leaves a separate child target even on a tiny dot.
const canvasBadgeLabelDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'label',
  zonePlacement: CanvasDropZonePlacement.badgeLabel,
  overlapPriority: 1,
);

const canvasSliverVisibilityReplacementDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'replacementSliver', acceptance: canvasSliverDropAcceptance,
);
const canvasVisibilityReplacementDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'replacement',
);
const canvasTextButtonIconDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'icon',
);
const canvasSelectedIconDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'selectedIcon',
);

const canvasScaffoldBodyDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'body',
);

const canvasScaffoldAppBarDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'appBar',
  acceptance: canvasPreferredSizeDropAcceptance,
  overlapPriority: 3,
);

const canvasScaffoldFloatingActionButtonDropSlot =
    CanvasDropSlotSemantics.emptySingle(
      slotName: 'floatingActionButton',
      zonePlacement: CanvasDropZonePlacement.bottomRightCompact,
      overlapPriority: 1,
    );

const canvasAppBarLeadingDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'leading',
  zonePlacement: CanvasDropZonePlacement.appBarLeading,
  overlapPriority: 4,
);

const canvasAppBarTitleDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'title',
  zonePlacement: CanvasDropZonePlacement.appBarTitle,
  overlapPriority: 4,
);

const canvasAppBarActionsDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'actions',
  maximumChildren: 10000,
  zonePlacement: CanvasDropZonePlacement.appBarActions,
  overlapPriority: 5,
);

const canvasAppBarFlexibleSpaceDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'flexibleSpace',
  zonePlacement: CanvasDropZonePlacement.appBarFlexibleSpace,
);

const canvasAppBarBottomDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'bottom',
  zonePlacement: CanvasDropZonePlacement.appBarBottom,
  acceptance: canvasPreferredSizeDropAcceptance,
  overlapPriority: 5,
);

/// Returns the reviewed native Canvas drop contract for one widget type.
///
/// Keep this explicit. Trait-constrained zones are exposed only after the host
/// binds the opaque token to its reviewed source type and traits.
List<CanvasDropSlotSemantics> canvasDropSlotsForWidgetType(String widgetType) =>
    switch (widgetType) {
      'flutter.material.Scaffold' => const [
        canvasScaffoldAppBarDropSlot,
        canvasScaffoldBodyDropSlot,
        canvasScaffoldFloatingActionButtonDropSlot,
      ],
      'flutter.material.FlexibleSpaceBar' => const [
        CanvasDropSlotSemantics.emptySingle(slotName: 'title', overlapPriority: 2,
          zonePlacement: CanvasDropZonePlacement.flexibleSpaceBarTitle),
        CanvasDropSlotSemantics.emptySingle(slotName: 'background', overlapPriority: 1),
      ],
      'flutter.material.AppBar' ||
      'flutter.material.SliverAppBar' ||
      'flutter.material.SliverAppBar.medium' ||
      'flutter.material.SliverAppBar.large' => const [
        canvasAppBarLeadingDropSlot,
        canvasAppBarTitleDropSlot,
        canvasAppBarActionsDropSlot,
        canvasAppBarFlexibleSpaceDropSlot,
        canvasAppBarBottomDropSlot,
      ],
      'flutter.material.ExpansionTile' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'title',
          zonePlacement: CanvasDropZonePlacement.listTileTitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'leading',
          zonePlacement: CanvasDropZonePlacement.listTileLeading,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'subtitle',
          zonePlacement: CanvasDropZonePlacement.listTileSubtitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'trailing',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
        CanvasDropSlotSemantics.append(
          slotName: 'children',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.ListTile' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'leading',
          zonePlacement: CanvasDropZonePlacement.listTileLeading,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'title',
          zonePlacement: CanvasDropZonePlacement.listTileTitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'subtitle',
          zonePlacement: CanvasDropZonePlacement.listTileSubtitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'trailing',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
      ],
      'flutter.material.CheckboxListTile' ||
      'flutter.material.SwitchListTile' ||
      'flutter.material.RadioListTile' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'title',
          zonePlacement: CanvasDropZonePlacement.listTileTitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'subtitle',
          zonePlacement: CanvasDropZonePlacement.listTileSubtitle,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'secondary',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
      ],
      'flutter.material.Badge' => const [
        canvasBadgeLabelDropSlot,
        canvasEmptyChildDropSlot,
      ],
      'flutter.widgets.Column' ||
      'flutter.widgets.Row' ||
      'flutter.widgets.ListBody' ||
      'flutter.widgets.ListView' ||
      'flutter.widgets.GridView' ||
      'flutter.widgets.GridView.extent' ||
      'flutter.widgets.PageView' ||
      'flutter.widgets.ListWheelScrollView' ||
      'flutter.widgets.SliverList' ||
      'flutter.widgets.SliverGrid' ||
      'flutter.widgets.SliverGrid.extent' ||
      'flutter.widgets.SliverGrid.list' ||
      'flutter.widgets.SliverFillViewport' ||
      'flutter.widgets.SliverVariedExtentList' ||
      'flutter.widgets.SliverFixedExtentList' ||
      'flutter.widgets.OverflowBar' => const [canvasChildrenAppendDropSlot],
      'flutter.widgets.PinnedHeaderSliver' => const [canvasEmptyChildDropSlot],
      'flutter.widgets.SliverResizingHeader' => const [
        canvasEmptyChildDropSlot,
        CanvasDropSlotSemantics.emptySingle(slotName: 'minExtentPrototype'),
        CanvasDropSlotSemantics.emptySingle(slotName: 'maxExtentPrototype'),
      ],
      'flutter.widgets.SliverPrototypeExtentList' => const [canvasChildrenAppendDropSlot, canvasPrototypeItemDropSlot],
      'flutter.widgets.SliverPrototypeExtentList.builder' ||
      'flutter.widgets.SliverPrototypeExtentList.delegate' => const [canvasPrototypeItemDropSlot],
      'flutter.widgets.CustomScrollView' ||
      'flutter.widgets.SliverMainAxisGroup' ||
      'flutter.widgets.SliverCrossAxisGroup' => const [canvasSliversAppendDropSlot],
      'flutter.widgets.TweenAnimationBuilder.sliver' || 'flutter.widgets.ValueListenableBuilder.sliver' || 'flutter.widgets.AnimatedBuilder.sliver' || 'flutter.widgets.ListenableBuilder.sliver' => const [
        CanvasDropSlotSemantics.emptySingle(slotName: 'child', acceptance: canvasSliverDropAcceptance),
      ],
      'flutter.widgets.SliverPadding' ||
      'flutter.widgets.SliverOpacity' ||
      'flutter.widgets.SliverFadeTransition' ||
      'flutter.widgets.SliverAnimatedOpacity' ||
      'flutter.widgets.SliverIgnorePointer' ||
      'flutter.widgets.SliverOffstage' => const [canvasSliverPaddingDropSlot],
      'flutter.widgets.Wrap' => const [canvasWrapChildrenAppendDropSlot],
      'flutter.widgets.Stack' => const [canvasStackChildrenAppendDropSlot],
      'flutter.widgets.IndexedStack' => const [
        canvasIndexedStackChildrenAppendDropSlot,
      ],
      'flutter.widgets.SliverVisibility' ||
      'flutter.widgets.SliverVisibility.maintain' => const [canvasSliverVisibilityReplacementDropSlot],
      'flutter.widgets.Visibility' => const [
        canvasVisibilityReplacementDropSlot,
      ],
      'flutter.widgets.TweenAnimationBuilder' || 'flutter.widgets.ValueListenableBuilder' ||
      'flutter.widgets.AnimatedBuilder' ||
      'flutter.widgets.ListenableBuilder' ||
      'flutter.material.Card' ||
      'flutter.material.Material' ||
      'flutter.material.Drawer' ||
      'flutter.material.BottomAppBar' ||
      'flutter.material.Tooltip' ||
      'flutter.material.CircleAvatar' ||
      'flutter.widgets.Align' ||
      'flutter.widgets.AspectRatio' ||
      'flutter.widgets.Baseline' ||
      'flutter.widgets.IntrinsicHeight' ||
      'flutter.widgets.IntrinsicWidth' ||
      'flutter.widgets.Offstage' ||
      'flutter.widgets.RotatedBox' ||
      'flutter.widgets.SizedOverflowBox' ||
      'flutter.widgets.Transform' ||
      'flutter.widgets.Center' ||
      'flutter.widgets.ConstrainedBox' ||
      'flutter.widgets.UnconstrainedBox' ||
      'flutter.widgets.LimitedBox' ||
      'flutter.widgets.OverflowBox' ||
      'flutter.widgets.Placeholder' ||
      'flutter.widgets.ClipOval' ||
      'flutter.widgets.ClipRRect' ||
      'flutter.widgets.ClipRSuperellipse' ||
      'flutter.widgets.PhysicalModel' ||
      'flutter.widgets.PhysicalShape' ||
      'flutter.widgets.RepaintBoundary' ||
      'flutter.widgets.IgnorePointer' ||
      'flutter.widgets.GestureDetector' ||
      'flutter.widgets.Listener' ||
      'flutter.widgets.MouseRegion' ||
      'flutter.widgets.AbsorbPointer' ||
      'flutter.widgets.BlockSemantics' ||
      'flutter.widgets.MergeSemantics' ||
      'flutter.widgets.IndexedSemantics' ||
      'flutter.widgets.ClipPath' ||
      'flutter.widgets.ClipRect' ||
      'flutter.widgets.ColoredBox' ||
      'flutter.widgets.Container' ||
      'flutter.widgets.DecoratedBox' ||
      'flutter.widgets.ExcludeSemantics' ||
      'flutter.widgets.FittedBox' ||
      'flutter.widgets.FractionallySizedBox' ||
      'flutter.widgets.AnimatedAlign' ||
      'flutter.widgets.AnimatedFractionallySizedBox' ||
      'flutter.widgets.AnimatedPadding' ||
      'flutter.widgets.AnimatedSlide' ||
      'flutter.widgets.AnimatedScale' ||
      'flutter.widgets.AnimatedSwitcher' ||
      'flutter.widgets.AnimatedSize' ||
      'flutter.widgets.AnimatedContainer' ||
      'flutter.widgets.AnimatedRotation' ||
      'flutter.widgets.RotationTransition' ||
      'flutter.widgets.MatrixTransition' ||
      'flutter.widgets.SizeTransition' ||
      'flutter.widgets.ScaleTransition' ||
      'flutter.widgets.SlideTransition' ||
      'flutter.widgets.FadeTransition' ||
      'flutter.widgets.AnimatedOpacity' ||
      'flutter.widgets.Opacity' ||
      'flutter.widgets.Padding' ||
      'flutter.widgets.SingleChildScrollView' ||
      'flutter.widgets.SliverToBoxAdapter' ||
      'flutter.widgets.SliverFillRemaining' ||
      'flutter.widgets.SizedBox' ||
      'flutter.material.ElevatedButton' => const [canvasEmptyChildDropSlot],
      'flutter.material.TextButton' => const [canvasTextButtonIconDropSlot],
      'flutter.material.MenuItemButton' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'child',
          zonePlacement: CanvasDropZonePlacement.menuItemChild,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'leadingIcon',
          zonePlacement: CanvasDropZonePlacement.listTileLeading,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'trailingIcon',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
      ],
      'flutter.material.MenuAnchor' => const [
        canvasEmptyChildDropSlot,
        CanvasDropSlotSemantics.append(
          slotName: 'menuChildren',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.MenuBar' => const [
        CanvasDropSlotSemantics.append(
          slotName: 'children',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.NavigationBar' => const [
        CanvasDropSlotSemantics.append(
          slotName: 'destinations',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.BottomNavigationBar' => const [
        CanvasDropSlotSemantics.append(
          slotName: 'items',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.NavigationRail' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'leading',
          zonePlacement: CanvasDropZonePlacement.listTileLeading,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'trailing',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
        CanvasDropSlotSemantics.append(
          slotName: 'destinations',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.NavigationDrawer' => const [
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'header',
          zonePlacement: CanvasDropZonePlacement.fullNode,
          overlapPriority: 2,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'footer',
          zonePlacement: CanvasDropZonePlacement.fullNode,
          overlapPriority: 2,
        ),
        CanvasDropSlotSemantics.append(
          slotName: 'children',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.SubmenuButton' => const [
        canvasEmptyChildDropSlot,
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'leadingIcon',
          zonePlacement: CanvasDropZonePlacement.listTileLeading,
        ),
        CanvasDropSlotSemantics.emptySingle(
          slotName: 'trailingIcon',
          zonePlacement: CanvasDropZonePlacement.listTileTrailing,
        ),
        CanvasDropSlotSemantics.append(
          slotName: 'menuChildren',
          maximumChildren: 10000,
          overlapPriority: 1,
        ),
      ],
      'flutter.material.OutlinedButton' => const [canvasTextButtonIconDropSlot],
      'flutter.material.IconButton' => const [canvasSelectedIconDropSlot],
      'flutter.material.FilledButton' => const [
        canvasTextButtonIconDropSlot,
        canvasEmptyChildDropSlot,
      ],
      'flutter.material.FloatingActionButton' => const [
        canvasTextButtonIconDropSlot,
        canvasEmptyChildDropSlot,
      ],
      _ => const [],
    };

/// Resolves one exact reviewed slot without accepting arbitrary model slots.
CanvasDropSlotSemantics? canvasDropSlotForWidgetSlot(
  String widgetType,
  String slotName,
) {
  for (final slot in canvasDropSlotsForWidgetType(widgetType)) {
    if (slot.slotName == slotName) {
      return slot;
    }
  }
  return null;
}

/// Resolves an occupied reviewed parent slot that can be replaced by a
/// host-authorized required-child wrapper.
///
/// Required child slots are deliberately absent from the normal insertion
/// matrix because a valid model can never expose them empty. They are still
/// valid replacement targets when the palette operation wraps their current
/// child without creating an intermediate invalid model.
CanvasDropSlotSemantics? canvasExistingChildWrapTargetSlot({
  required String parentWidgetType,
  required String slotName,
}) {
  final insertionSlot = canvasDropSlotForWidgetSlot(parentWidgetType, slotName);
  final requiredChild =
      slotName == canvasReviewedRequiredWrapperSlot(parentWidgetType) ||
      parentWidgetType == 'flutter.widgets.AnimatedCrossFade' && const {'firstChild', 'secondChild'}.contains(slotName);
  final cardinality =
      insertionSlot?.cardinality ??
      (requiredChild ? CanvasDropSlotCardinality.single : null);
  final acceptance = insertionSlot?.acceptance ??
      (isCanvasSliverWidgetType(parentWidgetType) ? canvasSliverDropAcceptance : canvasAnyDropAcceptance);
  if (cardinality == null) {
    return null;
  }
  return cardinality == CanvasDropSlotCardinality.single
      ? CanvasDropSlotSemantics.wrapExistingSingle(
          slotName: slotName,
          overlapPriority: 6,
          acceptance: acceptance,
        )
      : CanvasDropSlotSemantics.wrapExisting(
          slotName: slotName,
          overlapPriority: 6,
          acceptance: acceptance,
        );
}

bool canvasWrapperAcceptsExistingChild({
  required String wrapperWidgetType,
  required String childWidgetType,
}) =>
    isCanvasPaletteWrapperWidgetType(wrapperWidgetType) &&
    (isCanvasSliverWidgetType(wrapperWidgetType)
      ? isCanvasSliverWidgetType(childWidgetType) && childWidgetType != canvasSliverCrossAxisExpandedType
      : !isCanvasFlexRestrictedWidgetType(childWidgetType) &&
        !isCanvasStackPositionedWidgetType(childWidgetType) &&
        (!(isCanvasStackPositionedWidgetType(wrapperWidgetType) || const {'flutter.widgets.DecoratedBoxTransition','flutter.widgets.AlignTransition'}.contains(wrapperWidgetType)) || !isCanvasSliverWidgetType(childWidgetType)));

/// Revalidates the complete source-aware semantic target independently of
/// rendered hit-test geometry.
bool canvasDropTargetAcceptsSource({
  required String parentWidgetType,
  required String slotName,
  required int currentChildCount,
  required int insertionIndex,
  required CanvasPaletteDragSource source,
}) {
  if (isCanvasStackPositionedWidgetType(source.widgetType) &&
      !(parentWidgetType == 'flutter.widgets.Stack' && slotName == 'children')) { return false; }
  if (source.widgetType == canvasSliverCrossAxisExpandedType &&
      !isCanvasSliverCrossAxisExpandedDestination(parentWidgetType, slotName)) {
    return false;
  }
  if (isCanvasFlexParentDataWidgetType(source.widgetType)) {
    return slotName == 'children' &&
        (parentWidgetType == 'flutter.widgets.Row' ||
            parentWidgetType == 'flutter.widgets.Column') &&
        canvasFlexWrapDropSlot.accepts(
          currentChildCount: currentChildCount,
          insertionIndex: insertionIndex,
        );
  }
  if (isCanvasRequiredChildWrapperWidgetType(source.widgetType)) {
    final slot = canvasExistingChildWrapTargetSlot(
      parentWidgetType: parentWidgetType,
      slotName: slotName,
    );
    return slot != null &&
        slot.accepts(
          currentChildCount: currentChildCount,
          insertionIndex: insertionIndex,
        ) &&
        slot.acceptsSource(source);
  }
  if (source.widgetType == canvasSpacerWidgetType &&
      (slotName != 'children' ||
          (parentWidgetType != 'flutter.widgets.Row' &&
              parentWidgetType != 'flutter.widgets.Column'))) {
    return false;
  }
  final slot = canvasDropSlotForWidgetSlot(parentWidgetType, slotName);
  return slot != null &&
      slot.accepts(
        currentChildCount: currentChildCount,
        insertionIndex: insertionIndex,
      ) &&
      slot.acceptsSource(source);
}

/// A read-only semantic intent produced by Flutter hit testing.
class CanvasDropTarget {
  const CanvasDropTarget({
    required this.parentWidgetId,
    required this.slotName,
    required this.insertionIndex,
    this.zone,
  });

  final String parentWidgetId;
  final String slotName;
  final int insertionIndex;

  /// Exact normalized insertion zone approved by Flutter hit testing.
  ///
  /// The native OLE edge never constructs this geometry. It is published only
  /// so the Canvas can render the same authoritative zone it approved.
  final CanvasDropZone? zone;
}

/// A bounded rectangle in FlutterView surface micros (`0..1000000`).
class CanvasDropZone {
  const CanvasDropZone({
    required this.leftMicros,
    required this.topMicros,
    required this.rightMicros,
    required this.bottomMicros,
  });

  final int leftMicros;
  final int topMicros;
  final int rightMicros;
  final int bottomMicros;

  bool get isEmpty => rightMicros <= leftMicros || bottomMicros <= topMicros;
}
