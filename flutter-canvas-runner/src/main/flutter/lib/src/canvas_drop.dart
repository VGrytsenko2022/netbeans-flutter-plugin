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
    CanvasDropAcceptanceKind.any => true,
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
  }) : maximumChildren = 10000,
       cardinality = CanvasDropSlotCardinality.list,
       zonePlacement = CanvasDropZonePlacement.existingChild,
       acceptance = canvasAnyDropAcceptance,
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
  /// The complete rendered parent is the insertion target.
  fullNode,

  /// Only the visual terminal edge of a linear Row, Column, or ListView is exposed.
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
}

const canvasChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
);

const canvasExpandedWrapDropSlot = CanvasDropSlotSemantics.wrapExisting(
  slotName: 'children',
  overlapPriority: 6,
);

const canvasExpandedWidgetType = 'flutter.widgets.Expanded';

const canvasStackChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
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
      'flutter.material.AppBar' => const [
        canvasAppBarLeadingDropSlot,
        canvasAppBarTitleDropSlot,
        canvasAppBarActionsDropSlot,
        canvasAppBarFlexibleSpaceDropSlot,
        canvasAppBarBottomDropSlot,
      ],
      'flutter.widgets.Column' ||
      'flutter.widgets.Row' ||
      'flutter.widgets.ListView' => const [canvasChildrenAppendDropSlot],
      'flutter.widgets.Wrap' => const [canvasWrapChildrenAppendDropSlot],
      'flutter.widgets.Stack' => const [canvasStackChildrenAppendDropSlot],
      'flutter.widgets.Align' ||
      'flutter.widgets.AspectRatio' ||
      'flutter.widgets.Center' ||
      'flutter.widgets.ConstrainedBox' ||
      'flutter.widgets.UnconstrainedBox' ||
      'flutter.widgets.LimitedBox' ||
      'flutter.widgets.OverflowBox' ||
      'flutter.widgets.Container' ||
      'flutter.widgets.FittedBox' ||
      'flutter.widgets.FractionallySizedBox' ||
      'flutter.widgets.Opacity' ||
      'flutter.widgets.Padding' ||
      'flutter.widgets.SizedBox' ||
      'flutter.material.ElevatedButton' => const [canvasEmptyChildDropSlot],
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

/// Revalidates the complete source-aware semantic target independently of
/// rendered hit-test geometry.
bool canvasDropTargetAcceptsSource({
  required String parentWidgetType,
  required String slotName,
  required int currentChildCount,
  required int insertionIndex,
  required CanvasPaletteDragSource source,
}) {
  if (source.widgetType == canvasExpandedWidgetType) {
    return slotName == 'children' &&
        (parentWidgetType == 'flutter.widgets.Row' ||
            parentWidgetType == 'flutter.widgets.Column') &&
        canvasExpandedWrapDropSlot.accepts(
          currentChildCount: currentChildCount,
          insertionIndex: insertionIndex,
        );
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
