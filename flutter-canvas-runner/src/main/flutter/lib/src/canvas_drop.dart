/// Resolves one normalized native surface point to a semantic drop target.
typedef CanvasDropResolver =
    CanvasDropTarget? Function(int surfaceXMicros, int surfaceYMicros);

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
  }) : cardinality = CanvasDropSlotCardinality.list,
       zonePlacement = CanvasDropZonePlacement.terminalList;

  const CanvasDropSlotSemantics.emptySingle({
    required this.slotName,
    this.zonePlacement = CanvasDropZonePlacement.fullNode,
    this.overlapPriority = 0,
  }) : maximumChildren = 1,
       cardinality = CanvasDropSlotCardinality.single;

  final String slotName;
  final int maximumChildren;
  final CanvasDropSlotCardinality cardinality;
  final CanvasDropZonePlacement zonePlacement;
  final int overlapPriority;

  bool get fillsEmptySingleChild =>
      cardinality == CanvasDropSlotCardinality.single;

  String get modelSlotKind => switch (cardinality) {
    CanvasDropSlotCardinality.single => 'single',
    CanvasDropSlotCardinality.list => 'list',
  };

  int? insertionIndexFor(int currentChildCount) {
    if (currentChildCount < 0 || currentChildCount >= maximumChildren) {
      return null;
    }
    if (fillsEmptySingleChild && currentChildCount != 0) {
      return null;
    }
    return fillsEmptySingleChild ? 0 : currentChildCount;
  }

  bool accepts({required int currentChildCount, required int insertionIndex}) =>
      insertionIndexFor(currentChildCount) == insertionIndex;
}

enum CanvasDropSlotCardinality { single, list }

/// How the Flutter render tree exposes one reviewed semantic slot.
enum CanvasDropZonePlacement {
  /// The complete rendered parent is the insertion target.
  fullNode,

  /// Only the visual terminal edge of a Row or Column is exposed.
  terminalList,

  /// A concise lower-right target reserved for Scaffold's FAB slot.
  bottomRightCompact,
}

const canvasChildrenAppendDropSlot = CanvasDropSlotSemantics.append(
  slotName: 'children',
  maximumChildren: 10000,
);

const canvasEmptyChildDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'child',
);

const canvasScaffoldBodyDropSlot = CanvasDropSlotSemantics.emptySingle(
  slotName: 'body',
);

const canvasScaffoldFloatingActionButtonDropSlot =
    CanvasDropSlotSemantics.emptySingle(
      slotName: 'floatingActionButton',
      zonePlacement: CanvasDropZonePlacement.bottomRightCompact,
      overlapPriority: 1,
    );

/// Returns the reviewed native Canvas drop contract for one widget type.
///
/// Keep this explicit. In particular, Scaffold.appBar is not exposed even
/// though it is present in the model: its trait acceptance cannot be resolved
/// from the opaque native drag token.
List<CanvasDropSlotSemantics> canvasDropSlotsForWidgetType(String widgetType) =>
    switch (widgetType) {
      'flutter.material.Scaffold' => const [
        canvasScaffoldBodyDropSlot,
        canvasScaffoldFloatingActionButtonDropSlot,
      ],
      'flutter.widgets.Column' ||
      'flutter.widgets.Row' => const [canvasChildrenAppendDropSlot],
      'flutter.widgets.Padding' ||
      'flutter.widgets.Center' => const [canvasEmptyChildDropSlot],
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
