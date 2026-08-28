/// Resolves one normalized native surface point to a semantic drop target.
typedef CanvasDropResolver =
    CanvasDropTarget? Function(int surfaceXMicros, int surfaceYMicros);

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

  /// Exact normalized terminal append band approved by Flutter hit testing.
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
