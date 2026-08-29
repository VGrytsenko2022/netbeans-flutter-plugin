import 'dart:math' as math;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'canvas_drop.dart';
import 'canvas_model.dart';
import 'canvas_runtime.dart';

bool _ignoreDeleteSelected() => false;

class NativeCanvasApp extends StatelessWidget {
  const NativeCanvasApp({required this.runtime, super.key});

  final CanvasRuntimeController runtime;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: runtime,
      builder: (context, _) {
        final model = runtime.model;
        if (model == null) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            home: _RuntimeStatus(
              message:
                  runtime.errorMessage ??
                  (runtime.closed
                      ? 'Flutter Canvas session ended'
                      : 'Waiting for the Flutter Designer model…'),
              failed: runtime.errorMessage != null,
            ),
          );
        }
        return CanvasModelApp(
          model: model,
          selectedWidgetId: runtime.selectedWidgetId,
          onSelected: runtime.selectFromCanvas,
          onDeleteSelected: runtime.deleteSelectedFromCanvas,
          dropHoverTarget: runtime.dropHoverTarget,
          dropIndicatorKind: runtime.hasWidgetMovePreview
              ? CanvasDropIndicatorKind.widgetMove
              : CanvasDropIndicatorKind.paletteInsertion,
          onDropResolverChanged: runtime.setDropResolver,
          onMovePreviewResolverChanged: runtime.setMovePreviewResolver,
          viewportPresentation: runtime.viewportPresentation,
          onViewportPresentationChanged: runtime.updateViewportFromCanvas,
          onViewportMetricsChanged: runtime.reportViewportMetrics,
        );
      },
    );
  }
}

/// One decoded presentation with its exact Flutter adaptive target applied.
class CanvasModelApp extends StatelessWidget {
  const CanvasModelApp({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.dropHoverTarget,
    this.dropIndicatorKind = CanvasDropIndicatorKind.paletteInsertion,
    this.onDropResolverChanged,
    this.onMovePreviewResolverChanged,
    this.viewportPresentation,
    this.onViewportPresentationChanged,
    this.onViewportMetricsChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final bool Function() onDeleteSelected;
  final CanvasDropTarget? dropHoverTarget;
  final CanvasDropIndicatorKind dropIndicatorKind;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;
  final ValueChanged<CanvasMovePreviewResolver?>? onMovePreviewResolverChanged;
  final CanvasViewportPresentation? viewportPresentation;
  final ValueChanged<CanvasViewportPresentation>? onViewportPresentationChanged;
  final ValueChanged<CanvasViewportMetrics>? onViewportMetricsChanged;

  @override
  Widget build(BuildContext context) {
    final dark = model.profile.brightness == 'dark';
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: dark ? ThemeMode.dark : ThemeMode.light,
      theme: _theme(model.profile, Brightness.light),
      darkTheme: _theme(model.profile, Brightness.dark),
      home: CanvasDocumentView(
        model: model,
        selectedWidgetId: selectedWidgetId,
        onSelected: onSelected,
        onDeleteSelected: onDeleteSelected,
        dropHoverTarget: dropHoverTarget,
        dropIndicatorKind: dropIndicatorKind,
        onDropResolverChanged: onDropResolverChanged,
        onMovePreviewResolverChanged: onMovePreviewResolverChanged,
        viewportPresentation: viewportPresentation,
        onViewportPresentationChanged: onViewportPresentationChanged,
        onViewportMetricsChanged: onViewportMetricsChanged,
      ),
    );
  }
}

class CanvasDocumentView extends StatefulWidget {
  const CanvasDocumentView({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.dropHoverTarget,
    this.dropIndicatorKind = CanvasDropIndicatorKind.paletteInsertion,
    this.onDropResolverChanged,
    this.onMovePreviewResolverChanged,
    this.viewportPresentation,
    this.onViewportPresentationChanged,
    this.onViewportMetricsChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final bool Function() onDeleteSelected;
  final CanvasDropTarget? dropHoverTarget;
  final CanvasDropIndicatorKind dropIndicatorKind;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;
  final ValueChanged<CanvasMovePreviewResolver?>? onMovePreviewResolverChanged;
  final CanvasViewportPresentation? viewportPresentation;
  final ValueChanged<CanvasViewportPresentation>? onViewportPresentationChanged;
  final ValueChanged<CanvasViewportMetrics>? onViewportMetricsChanged;

  @override
  State<CanvasDocumentView> createState() => _CanvasDocumentViewState();
}

class _CanvasDocumentViewState extends State<CanvasDocumentView> {
  static const int _microsPerSurface = 1000000;
  static const double _minimumTerminalBand = 36;
  static const double _moveInsertionMarkerExtent = 12;
  static const double _scaffoldFabDropExtent = 72;

  final GlobalKey _surfaceKey = GlobalKey();
  final Map<String, GlobalKey> _nodeKeys = <String, GlobalKey>{};
  final FocusNode _focusNode = FocusNode(debugLabel: 'native-canvas');
  _ViewportGeometry? _viewportGeometry;
  CanvasViewportMetrics? _lastReportedViewportMetrics;

  @override
  void initState() {
    super.initState();
    widget.onDropResolverChanged?.call(_resolveDrop);
    widget.onMovePreviewResolverChanged?.call(_resolveMovePreview);
  }

  @override
  void didUpdateWidget(CanvasDocumentView oldWidget) {
    super.didUpdateWidget(oldWidget);
    _nodeKeys.removeWhere((id, _) => !widget.model.widgetIds.contains(id));
    if (oldWidget.onDropResolverChanged != widget.onDropResolverChanged) {
      oldWidget.onDropResolverChanged?.call(null);
      widget.onDropResolverChanged?.call(_resolveDrop);
    }
    if (oldWidget.onMovePreviewResolverChanged !=
        widget.onMovePreviewResolverChanged) {
      oldWidget.onMovePreviewResolverChanged?.call(null);
      widget.onMovePreviewResolverChanged?.call(_resolveMovePreview);
    }
  }

  @override
  void dispose() {
    widget.onDropResolverChanged?.call(null);
    widget.onMovePreviewResolverChanged?.call(null);
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final profile = widget.model.profile;
    final viewport = Size(profile.logicalWidth, profile.logicalHeight);
    final dark = profile.brightness == 'dark';
    final presentation =
        widget.viewportPresentation ??
        CanvasViewportPresentation.fit(widget.model);
    return Focus(
      autofocus: true,
      focusNode: _focusNode,
      onKeyEvent: _onKeyEvent,
      child: Scaffold(
        backgroundColor: dark
            ? const Color(0xff202124)
            : const Color(0xffe7e9ed),
        body: SizedBox.expand(
          key: _surfaceKey,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final geometry = _ViewportGeometry.calculate(
                surface: constraints.biggest,
                viewport: viewport,
                presentation: presentation,
              );
              _viewportGeometry = geometry;
              _reportViewportAfterFrame(presentation, geometry);
              return Listener(
                behavior: HitTestBehavior.opaque,
                onPointerSignal: _onPointerSignal,
                child: Stack(
                  clipBehavior: Clip.hardEdge,
                  children: [
                    Positioned(
                      left: geometry.left,
                      top: geometry.top,
                      width: geometry.renderedWidth,
                      height: geometry.renderedHeight,
                      child: FittedBox(
                        fit: BoxFit.fill,
                        child: Container(
                          width: viewport.width,
                          height: viewport.height,
                          clipBehavior: Clip.hardEdge,
                          decoration: BoxDecoration(
                            color: dark
                                ? const Color(0xff121212)
                                : Colors.white,
                            border: Border.all(
                              color: dark
                                  ? const Color(0xff5f6368)
                                  : const Color(0xff9aa0a6),
                            ),
                            boxShadow: const [
                              BoxShadow(
                                color: Color(0x33000000),
                                blurRadius: 12,
                                offset: Offset(0, 4),
                              ),
                            ],
                          ),
                          child: MediaQuery(
                            data: MediaQuery.of(context).copyWith(
                              size: viewport,
                              devicePixelRatio: profile.devicePixelRatio,
                              textScaler: TextScaler.linear(
                                profile.textScaleFactor,
                              ),
                              platformBrightness: dark
                                  ? Brightness.dark
                                  : Brightness.light,
                            ),
                            child: ClipRect(
                              child: _CanvasNodeView(
                                node: widget.model.root,
                                selectedWidgetId: widget.selectedWidgetId,
                                onSelected: _selectWidget,
                                nodeKey: _nodeKey,
                                overlayScale: geometry.scale,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    if (widget.dropHoverTarget case final target?
                        when target.zone != null)
                      _DropZoneOverlay(
                        target: target,
                        constraints: constraints,
                        indicatorKind: widget.dropIndicatorKind,
                      ),
                    if (geometry.horizontalScrollable)
                      Positioned(
                        left: 8,
                        right: geometry.verticalScrollable ? 20 : 8,
                        bottom: 4,
                        height: _CanvasViewportScrollbar.hitThickness,
                        child: _CanvasViewportScrollbar(
                          key: const ValueKey(
                            'canvas-horizontal-viewport-scrollbar',
                          ),
                          axis: Axis.horizontal,
                          valueMicros: presentation.horizontalScrollMicros,
                          viewportFraction: geometry.horizontalViewportFraction,
                          onChanged: (value) => _changeViewport(
                            presentation.copyWith(
                              horizontalScrollMicros: value,
                            ),
                          ),
                        ),
                      ),
                    if (geometry.verticalScrollable)
                      Positioned(
                        top: 8,
                        bottom: geometry.horizontalScrollable ? 20 : 8,
                        right: 4,
                        width: _CanvasViewportScrollbar.hitThickness,
                        child: _CanvasViewportScrollbar(
                          key: const ValueKey(
                            'canvas-vertical-viewport-scrollbar',
                          ),
                          axis: Axis.vertical,
                          valueMicros: presentation.verticalScrollMicros,
                          viewportFraction: geometry.verticalViewportFraction,
                          onChanged: (value) => _changeViewport(
                            presentation.copyWith(verticalScrollMicros: value),
                          ),
                        ),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  void _reportViewportAfterFrame(
    CanvasViewportPresentation presentation,
    _ViewportGeometry geometry,
  ) {
    final metrics = CanvasViewportMetrics(
      presentation: presentation,
      effectiveScaleMicros: (geometry.scale * canvasViewportMicros).round(),
      horizontalScrollable: geometry.horizontalScrollable,
      verticalScrollable: geometry.verticalScrollable,
    );
    if (metrics == _lastReportedViewportMetrics) {
      return;
    }
    _lastReportedViewportMetrics = metrics;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _lastReportedViewportMetrics != metrics) {
        return;
      }
      widget.onViewportMetricsChanged?.call(metrics);
    });
  }

  void _onPointerSignal(PointerSignalEvent event) {
    if (event is! PointerScrollEvent) {
      return;
    }
    final geometry = _viewportGeometry;
    final presentation =
        widget.viewportPresentation ??
        CanvasViewportPresentation.fit(widget.model);
    if (geometry == null ||
        geometry.presentation != presentation ||
        !presentation.matchesModel(widget.model)) {
      return;
    }
    final keyboard = HardwareKeyboard.instance;
    if (keyboard.isControlPressed) {
      final effectiveZoom = presentation.mode == 'fit'
          ? (geometry.scale * canvasViewportMicros).round()
          : presentation.zoomMicros;
      final direction = event.scrollDelta.dy == 0
          ? event.scrollDelta.dx
          : event.scrollDelta.dy;
      if (direction == 0) {
        return;
      }
      final stepCount = math.max(1, (direction.abs() / 100).round());
      final delta = (direction < 0 ? 100000 : -100000) * stepCount;
      _changeViewport(
        presentation.copyWith(
          mode: 'manual',
          zoomMicros: (effectiveZoom + delta).clamp(
            minimumCanvasZoomMicros,
            maximumCanvasZoomMicros,
          ),
        ),
      );
      return;
    }
    final horizontal = keyboard.isShiftPressed;
    final delta = horizontal
        ? (event.scrollDelta.dy == 0
              ? event.scrollDelta.dx
              : event.scrollDelta.dy)
        : event.scrollDelta.dy;
    final overflow = horizontal
        ? geometry.horizontalOverflow
        : geometry.verticalOverflow;
    if (overflow <= 0 || delta == 0) {
      return;
    }
    final current = horizontal
        ? presentation.horizontalScrollMicros
        : presentation.verticalScrollMicros;
    final next = (current + delta / overflow * canvasViewportMicros)
        .round()
        .clamp(0, canvasViewportMicros);
    _changeViewport(
      horizontal
          ? presentation.copyWith(horizontalScrollMicros: next)
          : presentation.copyWith(verticalScrollMicros: next),
    );
  }

  void _changeViewport(CanvasViewportPresentation presentation) {
    _focusNode.requestFocus();
    widget.onViewportPresentationChanged?.call(presentation);
  }

  void _selectWidget(String widgetId) {
    _focusNode.requestFocus();
    widget.onSelected(widgetId);
  }

  KeyEventResult _onKeyEvent(FocusNode node, KeyEvent event) {
    final keyboard = HardwareKeyboard.instance;
    if (event is! KeyDownEvent ||
        event.logicalKey != LogicalKeyboardKey.delete ||
        keyboard.isControlPressed ||
        keyboard.isShiftPressed ||
        keyboard.isAltPressed ||
        keyboard.isMetaPressed) {
      return KeyEventResult.ignored;
    }
    return widget.onDeleteSelected()
        ? KeyEventResult.handled
        : KeyEventResult.ignored;
  }

  GlobalKey _nodeKey(String id) =>
      _nodeKeys.putIfAbsent(id, () => GlobalKey(debugLabel: 'canvas-$id'));

  CanvasDropTarget? _resolveDrop(int xMicros, int yMicros) {
    if (xMicros < 0 ||
        xMicros > _microsPerSurface ||
        yMicros < 0 ||
        yMicros > _microsPerSurface) {
      return null;
    }
    final surface = _renderBox(_surfaceKey);
    if (surface == null || surface.size.isEmpty) {
      return null;
    }
    final surfaceRect = _globalRect(surface);
    final point = Offset(
      surfaceRect.left + surfaceRect.width * xMicros / _microsPerSurface,
      surfaceRect.top + surfaceRect.height * yMicros / _microsPerSurface,
    );
    final candidates = <_DropCandidate>[];
    _collectDropCandidates(
      widget.model.root,
      point,
      surfaceRect,
      0,
      candidates,
    );
    if (candidates.isEmpty) {
      return null;
    }
    candidates.sort((left, right) {
      final depth = right.depth.compareTo(left.depth);
      if (depth != 0) {
        return depth;
      }
      final priority = right.slot.overlapPriority.compareTo(
        left.slot.overlapPriority,
      );
      return priority != 0 ? priority : left.area.compareTo(right.area);
    });
    final selected = candidates.first;
    return CanvasDropTarget(
      parentWidgetId: selected.node.id,
      slotName: selected.slot.slotName,
      insertionIndex: selected.insertionIndex,
      zone: _normalizeZone(surfaceRect, selected.zone),
    );
  }

  CanvasDropTarget? _resolveMovePreview(
    String sourceWidgetId,
    String parentWidgetId,
    String slotName,
    int insertionIndex,
  ) {
    final source = _findCanvasNode(widget.model.root, sourceWidgetId);
    final parentNode = _findCanvasNode(widget.model.root, parentWidgetId);
    final surface = _renderBox(_surfaceKey);
    final parentBox = _renderBox(_nodeKeys[parentWidgetId]);
    if (source == null ||
        parentNode == null ||
        surface == null ||
        surface.size.isEmpty ||
        parentBox == null) {
      return null;
    }
    final modelSlot = parentNode.slot(slotName);
    final reviewedSlot = canvasDropSlotForWidgetSlot(parentNode.type, slotName);
    final slotKind = modelSlot?.kind ?? reviewedSlot?.modelSlotKind;
    if (slotKind == null) {
      return null;
    }
    final surfaceRect = _globalRect(surface);
    final parentRect = _boundedDesignerHitRect(
      _globalRect(parentBox),
      surfaceRect,
    );
    if (parentRect.isEmpty) {
      return null;
    }

    final Rect zone;
    if (slotKind == 'single') {
      if (insertionIndex != 0) {
        return null;
      }
      final retainedChildren =
          modelSlot?.children
              .where((child) => child.id != sourceWidgetId)
              .length ??
          0;
      if (retainedChildren != 0) {
        return null;
      }
      zone =
          parentNode.type == 'flutter.material.Scaffold' &&
              slotName == 'floatingActionButton'
          ? _bottomRightCompactZone(parentRect)
          : parentRect;
    } else if (slotKind == 'list') {
      final children = <CanvasNode>[
        for (final child in modelSlot?.children ?? const <CanvasNode>[])
          if (child.id != sourceWidgetId) child,
      ];
      if (insertionIndex < 0 || insertionIndex > children.length) {
        return null;
      }
      zone = _listMoveInsertionZone(
        parentNode,
        parentRect,
        children,
        insertionIndex,
      );
    } else {
      return null;
    }
    if (zone.isEmpty) {
      return null;
    }
    return CanvasDropTarget(
      parentWidgetId: parentWidgetId,
      slotName: slotName,
      insertionIndex: insertionIndex,
      zone: _normalizeZone(surfaceRect, zone),
    );
  }

  Rect _listMoveInsertionZone(
    CanvasNode parentNode,
    Rect parentRect,
    List<CanvasNode> children,
    int insertionIndex,
  ) {
    if (children.isEmpty) {
      return parentRect;
    }
    final horizontal =
        parentNode.type == 'flutter.widgets.Row' ||
        parentNode.type == 'flutter.material.AppBar';
    final reverse = horizontal
        ? _enumValue(parentNode, 'textDirection') == 'rtl'
        : _enumValue(parentNode, 'verticalDirection') == 'up';
    final referenceIndex = insertionIndex < children.length
        ? insertionIndex
        : children.length - 1;
    final reference = _renderBox(_nodeKeys[children[referenceIndex].id]);
    if (reference == null) {
      return Rect.zero;
    }
    final referenceRect = _globalRect(reference).intersect(parentRect);
    if (referenceRect.isEmpty) {
      return Rect.zero;
    }
    final beforeExisting = insertionIndex < children.length;
    final edge = horizontal
        ? (beforeExisting
              ? (reverse ? referenceRect.right : referenceRect.left)
              : (reverse ? referenceRect.left : referenceRect.right))
        : (beforeExisting
              ? (reverse ? referenceRect.bottom : referenceRect.top)
              : (reverse ? referenceRect.top : referenceRect.bottom));
    if (horizontal) {
      final half = math.min(
        _moveInsertionMarkerExtent / 2,
        parentRect.width / 2,
      );
      final center = edge.clamp(
        parentRect.left + half,
        parentRect.right - half,
      );
      return Rect.fromLTRB(
        center - half,
        parentRect.top,
        center + half,
        parentRect.bottom,
      );
    }
    final half = math.min(
      _moveInsertionMarkerExtent / 2,
      parentRect.height / 2,
    );
    final center = edge.clamp(parentRect.top + half, parentRect.bottom - half);
    return Rect.fromLTRB(
      parentRect.left,
      center - half,
      parentRect.right,
      center + half,
    );
  }

  CanvasNode? _findCanvasNode(CanvasNode node, String id) {
    if (node.id == id) {
      return node;
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        final found = _findCanvasNode(child, id);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  CanvasDropZone _normalizeZone(Rect surface, Rect zone) {
    int micros(double value, double origin, double extent) =>
        ((value - origin) / extent * _microsPerSurface).round().clamp(
          0,
          _microsPerSurface,
        );
    return CanvasDropZone(
      leftMicros: micros(zone.left, surface.left, surface.width),
      topMicros: micros(zone.top, surface.top, surface.height),
      rightMicros: micros(zone.right, surface.left, surface.width),
      bottomMicros: micros(zone.bottom, surface.top, surface.height),
    );
  }

  void _collectDropCandidates(
    CanvasNode node,
    Offset point,
    Rect surfaceRect,
    int depth,
    List<_DropCandidate> result,
  ) {
    for (final dropSlot in canvasDropSlotsForWidgetType(node.type)) {
      final modelSlot = node.slot(dropSlot.slotName);
      if (modelSlot != null && modelSlot.kind != dropSlot.modelSlotKind) {
        continue;
      }
      final currentChildCount = modelSlot?.children.length ?? 0;
      final insertionIndex = dropSlot.insertionIndexFor(currentChildCount);
      final box = _renderBox(_nodeKeys[node.id]);
      if (insertionIndex != null && box != null) {
        final rect = _boundedDesignerHitRect(_globalRect(box), surfaceRect);
        final zone = switch (dropSlot.zonePlacement) {
          CanvasDropZonePlacement.fullNode => rect,
          CanvasDropZonePlacement.terminalList => _terminalZone(node, rect),
          CanvasDropZonePlacement.bottomRightCompact => _bottomRightCompactZone(
            rect,
          ),
        };
        if (!zone.isEmpty && zone.contains(point)) {
          result.add(
            _DropCandidate(
              node,
              depth,
              zone.width * zone.height,
              dropSlot,
              insertionIndex,
              zone,
            ),
          );
        }
      }
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        _collectDropCandidates(child, point, surfaceRect, depth + 1, result);
      }
    }
  }

  Rect _boundedDesignerHitRect(Rect rendered, Rect surface) {
    if (surface.isEmpty) {
      return Rect.zero;
    }
    final visible = rendered.intersect(surface);
    final rawCenter = visible.isEmpty ? rendered.center : visible.center;
    final width = math.min(
      surface.width,
      math.max(visible.width, _minimumTerminalBand),
    );
    final height = math.min(
      surface.height,
      math.max(visible.height, _minimumTerminalBand),
    );
    final centerX = rawCenter.dx.clamp(
      surface.left + width / 2,
      surface.right - width / 2,
    );
    final centerY = rawCenter.dy.clamp(
      surface.top + height / 2,
      surface.bottom - height / 2,
    );
    return Rect.fromCenter(
      center: Offset(centerX, centerY),
      width: width,
      height: height,
    );
  }

  Rect _bottomRightCompactZone(Rect parent) {
    if (parent.width <= 0 || parent.height <= 0) {
      return Rect.zero;
    }
    final width = _scaffoldFabDropExtent.clamp(1.0, parent.width);
    final height = _scaffoldFabDropExtent.clamp(1.0, parent.height);
    return Rect.fromLTRB(
      parent.right - width,
      parent.bottom - height,
      parent.right,
      parent.bottom,
    );
  }

  Rect _terminalZone(CanvasNode node, Rect parent) {
    if (parent.width <= 0 || parent.height <= 0) {
      return Rect.zero;
    }
    final children = node.slot('children')?.children ?? const <CanvasNode>[];
    if (children.isEmpty) {
      // With no siblings there is only one legal ordering result: index 0.
      // Expose the complete visible container instead of making users find a
      // synthetic terminal edge on an otherwise blank Row or Column.
      return parent;
    }
    final last = _renderBox(_nodeKeys[children.last.id]);
    if (last == null) {
      return Rect.zero;
    }
    final lastRect = _globalRect(last);
    if (node.type == 'flutter.widgets.Column') {
      final upward = _enumValue(node, 'verticalDirection') == 'up';
      final band = _minimumTerminalBand.clamp(1.0, parent.height);
      return upward
          ? Rect.fromLTRB(
              parent.left,
              parent.top,
              parent.right,
              (lastRect.top + band).clamp(parent.top, parent.bottom),
            )
          : Rect.fromLTRB(
              parent.left,
              (lastRect.bottom - band).clamp(parent.top, parent.bottom),
              parent.right,
              parent.bottom,
            );
    }
    final rightToLeft = _enumValue(node, 'textDirection') == 'rtl';
    final band = _minimumTerminalBand.clamp(1.0, parent.width);
    return rightToLeft
        ? Rect.fromLTRB(
            parent.left,
            parent.top,
            (lastRect.left + band).clamp(parent.left, parent.right),
            parent.bottom,
          )
        : Rect.fromLTRB(
            (lastRect.right - band).clamp(parent.left, parent.right),
            parent.top,
            parent.right,
            parent.bottom,
          );
  }

  static RenderBox? _renderBox(GlobalKey? key) {
    final renderObject = key?.currentContext?.findRenderObject();
    return renderObject is RenderBox && renderObject.attached
        ? renderObject
        : null;
  }

  static Rect _globalRect(RenderBox box) => MatrixUtils.transformRect(
    box.getTransformTo(null),
    Offset.zero & box.size,
  );

  static String? _enumValue(CanvasNode node, String propertyName) {
    final value = node.properties[propertyName]?.value;
    return value is CanvasEnumValue ? value.value : null;
  }
}

class _DropCandidate {
  const _DropCandidate(
    this.node,
    this.depth,
    this.area,
    this.slot,
    this.insertionIndex,
    this.zone,
  );

  final CanvasNode node;
  final int depth;
  final double area;
  final CanvasDropSlotSemantics slot;
  final int insertionIndex;
  final Rect zone;
}

class _ViewportGeometry {
  const _ViewportGeometry({
    required this.presentation,
    required this.scale,
    required this.left,
    required this.top,
    required this.renderedWidth,
    required this.renderedHeight,
    required this.availableWidth,
    required this.availableHeight,
    required this.horizontalOverflow,
    required this.verticalOverflow,
  });

  static const double _inset = 16;

  final CanvasViewportPresentation presentation;
  final double scale;
  final double left;
  final double top;
  final double renderedWidth;
  final double renderedHeight;
  final double availableWidth;
  final double availableHeight;
  final double horizontalOverflow;
  final double verticalOverflow;

  bool get horizontalScrollable => horizontalOverflow > 0.5;
  bool get verticalScrollable => verticalOverflow > 0.5;
  double get horizontalViewportFraction =>
      (availableWidth / renderedWidth).clamp(0.0, 1.0);
  double get verticalViewportFraction =>
      (availableHeight / renderedHeight).clamp(0.0, 1.0);

  static _ViewportGeometry calculate({
    required Size surface,
    required Size viewport,
    required CanvasViewportPresentation presentation,
  }) {
    final availableWidth = math.max(1.0, surface.width - _inset * 2);
    final availableHeight = math.max(1.0, surface.height - _inset * 2);
    final scale = presentation.mode == 'fit'
        ? math.min(
            1.0,
            math.min(
              availableWidth / viewport.width,
              availableHeight / viewport.height,
            ),
          )
        : presentation.zoomMicros / canvasViewportMicros;
    final renderedWidth = viewport.width * scale;
    final renderedHeight = viewport.height * scale;
    final horizontalOverflow = math.max(0.0, renderedWidth - availableWidth);
    final verticalOverflow = math.max(0.0, renderedHeight - availableHeight);
    final horizontalProgress =
        presentation.horizontalScrollMicros / canvasViewportMicros;
    final verticalProgress =
        presentation.verticalScrollMicros / canvasViewportMicros;
    return _ViewportGeometry(
      presentation: presentation,
      scale: scale,
      left: horizontalOverflow > 0
          ? _inset - horizontalOverflow * horizontalProgress
          : (surface.width - renderedWidth) / 2,
      top: verticalOverflow > 0
          ? _inset - verticalOverflow * verticalProgress
          : (surface.height - renderedHeight) / 2,
      renderedWidth: renderedWidth,
      renderedHeight: renderedHeight,
      availableWidth: availableWidth,
      availableHeight: availableHeight,
      horizontalOverflow: horizontalOverflow,
      verticalOverflow: verticalOverflow,
    );
  }
}

class _CanvasViewportScrollbar extends StatefulWidget {
  const _CanvasViewportScrollbar({
    required this.axis,
    required this.valueMicros,
    required this.viewportFraction,
    required this.onChanged,
    super.key,
  });

  final Axis axis;
  final int valueMicros;
  final double viewportFraction;
  final ValueChanged<int> onChanged;

  // Keep the pointer target comfortably usable while rendering a deliberately
  // restrained six-pixel enterprise scrollbar.
  static const double hitThickness = 12;
  static const double visualThickness = 6;

  @override
  State<_CanvasViewportScrollbar> createState() =>
      _CanvasViewportScrollbarState();
}

class _CanvasViewportScrollbarState extends State<_CanvasViewportScrollbar> {
  static const Duration _feedbackDuration = Duration(milliseconds: 90);

  bool _hovered = false;
  bool _dragging = false;

  void _setHovered(bool value) {
    if (_hovered != value) {
      setState(() => _hovered = value);
    }
  }

  void _setDragging(bool value) {
    if (_dragging != value) {
      setState(() => _dragging = value);
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final axis = widget.axis;
        final extent = axis == Axis.horizontal
            ? constraints.maxWidth
            : constraints.maxHeight;
        final thumbExtent = math.min(
          extent,
          math.max(24.0, extent * widget.viewportFraction),
        );
        final travel = math.max(0.0, extent - thumbExtent);
        final thumbOffset = travel * widget.valueMicros / canvasViewportMicros;
        int valueAt(Offset position) {
          if (travel == 0) {
            return 0;
          }
          final coordinate = axis == Axis.horizontal
              ? position.dx
              : position.dy;
          return ((coordinate - thumbExtent / 2) /
                  travel *
                  canvasViewportMicros)
              .round()
              .clamp(0, canvasViewportMicros);
        }

        void changeBy(int delta) {
          widget.onChanged(
            (widget.valueMicros + delta).clamp(0, canvasViewportMicros),
          );
        }

        final increasedValue = (widget.valueMicros + 100000).clamp(
          0,
          canvasViewportMicros,
        );
        final decreasedValue = (widget.valueMicros - 100000).clamp(
          0,
          canvasViewportMicros,
        );

        final dark = Theme.of(context).brightness == Brightness.dark;
        final emphasized = _hovered || _dragging;
        final trackColor = dark
            ? Color(emphasized ? 0x3dffffff : 0x24ffffff)
            : Color(emphasized ? 0x335f666d : 0x1f5f666d);
        final thumbColor = _dragging
            ? (dark ? const Color(0xff6ca5dc) : const Color(0xff3f78b5))
            : _hovered
            ? (dark ? const Color(0xbfe4e6e8) : const Color(0xbf50575e))
            : (dark ? const Color(0x99d0d3d6) : const Color(0x995f666d));
        final axisName = axis == Axis.horizontal ? 'horizontal' : 'vertical';

        return Semantics(
          label: axis == Axis.horizontal
              ? 'Canvas horizontal scroll'
              : 'Canvas vertical scroll',
          value: '${(widget.valueMicros / 10000).round()}%',
          slider: true,
          increasedValue: increasedValue == widget.valueMicros
              ? null
              : '${(increasedValue / 10000).round()}%',
          decreasedValue: decreasedValue == widget.valueMicros
              ? null
              : '${(decreasedValue / 10000).round()}%',
          onIncrease: increasedValue == widget.valueMicros
              ? null
              : () => changeBy(100000),
          onDecrease: decreasedValue == widget.valueMicros
              ? null
              : () => changeBy(-100000),
          child: MouseRegion(
            onEnter: (_) => _setHovered(true),
            onExit: (_) => _setHovered(false),
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onPanDown: (details) {
                _setDragging(true);
                widget.onChanged(valueAt(details.localPosition));
              },
              onPanUpdate: (details) =>
                  widget.onChanged(valueAt(details.localPosition)),
              onPanEnd: (_) => _setDragging(false),
              onPanCancel: () => _setDragging(false),
              child: Stack(
                children: [
                  Positioned(
                    left: axis == Axis.horizontal ? 0 : null,
                    right: 0,
                    top: axis == Axis.vertical ? 0 : null,
                    bottom: 0,
                    width: axis == Axis.vertical
                        ? _CanvasViewportScrollbar.visualThickness
                        : null,
                    height: axis == Axis.horizontal
                        ? _CanvasViewportScrollbar.visualThickness
                        : null,
                    child: AnimatedContainer(
                      key: ValueKey(
                        'canvas-$axisName-viewport-scrollbar-track',
                      ),
                      duration: _feedbackDuration,
                      curve: Curves.easeOut,
                      decoration: BoxDecoration(
                        color: trackColor,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  Positioned(
                    left: axis == Axis.horizontal ? thumbOffset : null,
                    right: axis == Axis.vertical ? 0 : null,
                    top: axis == Axis.vertical ? thumbOffset : null,
                    bottom: axis == Axis.horizontal ? 0 : null,
                    width: axis == Axis.horizontal
                        ? thumbExtent
                        : _CanvasViewportScrollbar.visualThickness,
                    height: axis == Axis.vertical
                        ? thumbExtent
                        : _CanvasViewportScrollbar.visualThickness,
                    child: AnimatedContainer(
                      key: ValueKey(
                        'canvas-$axisName-viewport-scrollbar-thumb',
                      ),
                      duration: _feedbackDuration,
                      curve: Curves.easeOut,
                      decoration: BoxDecoration(
                        color: thumbColor,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _DropZoneOverlay extends StatelessWidget {
  const _DropZoneOverlay({
    required this.target,
    required this.constraints,
    required this.indicatorKind,
  });

  final CanvasDropTarget target;
  final BoxConstraints constraints;
  final CanvasDropIndicatorKind indicatorKind;

  @override
  Widget build(BuildContext context) {
    const denominator = 1000000.0;
    final zone = target.zone!;
    final left = constraints.maxWidth * zone.leftMicros / denominator;
    final top = constraints.maxHeight * zone.topMicros / denominator;
    final width =
        constraints.maxWidth *
        (zone.rightMicros - zone.leftMicros) /
        denominator;
    final height =
        constraints.maxHeight *
        (zone.bottomMicros - zone.topMicros) /
        denominator;
    final widgetMove = indicatorKind == CanvasDropIndicatorKind.widgetMove;
    return Positioned(
      left: left,
      top: top,
      width: width,
      height: height,
      child: IgnorePointer(
        child: Semantics(
          label: widgetMove
              ? 'Flutter widget move target for ${target.slotName}'
              : 'Flutter widget insertion target for ${target.slotName}',
          child: DecoratedBox(
            key: ValueKey(
              widgetMove
                  ? 'canvas-widget-move-preview-zone'
                  : 'canvas-widget-insert-drop-zone',
            ),
            decoration: BoxDecoration(
              color: widgetMove
                  ? const Color(0x24D29A17)
                  : const Color(0x261A73E8),
              border: Border.all(
                color: widgetMove
                    ? const Color(0xffc58b08)
                    : const Color(0xff1a73e8),
                width: widgetMove ? 1.5 : 2,
              ),
              borderRadius: BorderRadius.circular(widgetMove ? 2 : 4),
            ),
          ),
        ),
      ),
    );
  }
}

class _CanvasNodeView extends StatelessWidget {
  const _CanvasNodeView({
    required this.node,
    required this.selectedWidgetId,
    required this.onSelected,
    required this.nodeKey,
    required this.overlayScale,
  });

  final CanvasNode node;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final GlobalKey Function(String id) nodeKey;
  final double overlayScale;

  @override
  Widget build(BuildContext context) {
    final paddingGeometry = node.type == 'flutter.widgets.Padding'
        ? _paddingGeometry()
        : null;
    final child = switch (node.type) {
      'flutter.material.Scaffold' => _scaffold(),
      'flutter.widgets.Column' => _column(),
      'flutter.widgets.Row' => _row(),
      'flutter.widgets.Padding' => _padding(paddingGeometry!),
      'flutter.widgets.Center' => _center(),
      'flutter.widgets.Text' => _text(context),
      _ => const SizedBox.shrink(),
    };
    final selected = selectedWidgetId == node.id;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final resolvedPadding = paddingGeometry?.resolve(
      Directionality.of(context),
    );
    final guidedChild = resolvedPadding == null
        ? child
        : CustomPaint(
            key: ValueKey('canvas-padding-guides-${node.id}'),
            foregroundPainter: _CanvasPaddingGuidesPainter(
              insets: resolvedPadding,
              visualScale: overlayScale,
              color: dark ? const Color(0xffffb74d) : const Color(0xffd97706),
            ),
            child: child,
          );
    final outlinedChild = CustomPaint(
      key: ValueKey('canvas-widget-outline-${node.id}'),
      foregroundPainter: _CanvasWidgetOutlinePainter(
        selected: selected,
        inflateEmptyLinearContainer: _isEmptyLinearContainer(node),
        visualScale: overlayScale,
        unselectedColor: dark
            ? const Color(0x99b0b8c1)
            : const Color(0x9974808a),
      ),
      child: guidedChild,
    );
    return Semantics(
      label: '${_displayType(node.type)} ${node.id}',
      selected: selected,
      child: MouseRegion(
        cursor: SystemMouseCursors.click,
        child: KeyedSubtree(
          key: ValueKey('canvas-widget-${node.id}'),
          child: GestureDetector(
            key: nodeKey(node.id),
            behavior: HitTestBehavior.translucent,
            onTap: () => onSelected(node.id),
            child: selected
                ? KeyedSubtree(
                    key: ValueKey('canvas-selection-outline-${node.id}'),
                    child: outlinedChild,
                  )
                : outlinedChild,
          ),
        ),
      ),
    );
  }

  Widget _scaffold() {
    final background = _color('backgroundColor');
    final resize = _boolean('resizeToAvoidBottomInset');
    return Scaffold(
      backgroundColor: background,
      resizeToAvoidBottomInset: resize,
      body: _single('body') ?? const SizedBox.expand(),
      floatingActionButton: _single('floatingActionButton'),
    );
  }

  Widget _column() => Column(
    mainAxisAlignment: _mainAxisAlignment(),
    mainAxisSize: _mainAxisSize(),
    crossAxisAlignment: _crossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    textBaseline: _textBaseline(),
    spacing: _number('spacing') ?? 0.0,
    children: _children('children'),
  );

  static bool _isEmptyLinearContainer(CanvasNode node) =>
      (node.type == 'flutter.widgets.Row' ||
          node.type == 'flutter.widgets.Column') &&
      (node.slot('children')?.children.isEmpty ?? false);

  Widget _row() => Row(
    mainAxisAlignment: _mainAxisAlignment(),
    mainAxisSize: _mainAxisSize(),
    crossAxisAlignment: _crossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    textBaseline: _textBaseline(),
    spacing: _number('spacing') ?? 0.0,
    children: _children('children'),
  );

  EdgeInsetsGeometry _paddingGeometry() {
    final value = node.properties['padding']!.value;
    return switch (value) {
      CanvasEdgeInsets physical => EdgeInsets.fromLTRB(
        physical.left,
        physical.top,
        physical.right,
        physical.bottom,
      ),
      CanvasEdgeInsetsDirectional directional => EdgeInsetsDirectional.fromSTEB(
        directional.start,
        directional.top,
        directional.end,
        directional.bottom,
      ),
      _ => throw StateError('Unsupported Canvas Padding value.'),
    };
  }

  Widget _padding(EdgeInsetsGeometry insets) =>
      Padding(padding: insets, child: _single('child'));

  Widget _center() => Center(
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _text(BuildContext context) => Text(
    _string('data')!,
    style: _textStyle(context),
    strutStyle: _strutStyle(),
    textAlign: _textAlign(),
    textDirection: _textDirection(),
    locale: _locale(
      languageCode: 'localeLanguageCode',
      scriptCode: 'localeScriptCode',
      countryCode: 'localeCountryCode',
    ),
    softWrap: _boolean('softWrap'),
    overflow: _textOverflow(),
    textScaler: _textScaler(),
    maxLines: _integer('maxLines'),
    semanticsLabel: _string('semanticsLabel'),
    semanticsIdentifier: _string('semanticsIdentifier'),
    textWidthBasis: _textWidthBasis(),
    textHeightBehavior: _textHeightBehavior(),
    selectionColor: _resolvedColor(context, 'selectionColor'),
  );

  TextStyle? _textStyle(BuildContext context) {
    if (!_hasPropertyPrefix('style')) {
      return null;
    }
    final themeBase = _themeTextStyle(context, 'styleThemeTextStyle');
    final hasOverrides = node.properties.keys.any(
      (name) => name.startsWith('style') && name != 'styleThemeTextStyle',
    );
    if (!hasOverrides) {
      return themeBase;
    }
    return (themeBase ?? const TextStyle()).copyWith(
      inherit: _boolean('styleInherit'),
      color: _resolvedColor(context, 'styleColor'),
      backgroundColor: _resolvedColor(context, 'styleBackgroundColor'),
      fontSize: _number('styleFontSize'),
      fontWeight: _fontWeight('styleFontWeight'),
      fontStyle: _fontStyle('styleFontStyle'),
      letterSpacing: _number('styleLetterSpacing'),
      wordSpacing: _number('styleWordSpacing'),
      textBaseline: _textBaseline('styleTextBaseline'),
      height: _number('styleHeight'),
      leadingDistribution: _textLeadingDistribution('styleLeadingDistribution'),
      locale: _locale(
        languageCode: 'styleLocaleLanguageCode',
        scriptCode: 'styleLocaleScriptCode',
        countryCode: 'styleLocaleCountryCode',
      ),
      foreground: _paint(context, 'styleForeground'),
      background: _paint(context, 'styleBackground'),
      shadows: _shadows(context, 'styleShadows'),
      fontFeatures: _fontFeatures('styleFontFeatures'),
      fontVariations: _fontVariations('styleFontVariations'),
      decoration: _textDecoration(),
      decorationColor: _resolvedColor(context, 'styleDecorationColor'),
      decorationStyle: _textDecorationStyle(),
      decorationThickness: _number('styleDecorationThickness'),
      debugLabel: _string('styleDebugLabel'),
      fontFamily: _string('styleFontFamily'),
      fontFamilyFallback: _newlineList('styleFontFamilyFallback'),
      package: _string('stylePackage'),
      overflow: _textOverflow('styleOverflow'),
    );
  }

  StrutStyle? _strutStyle() {
    if (!_hasPropertyPrefix('strut')) {
      return null;
    }
    return StrutStyle(
      fontFamily: _string('strutFontFamily'),
      fontFamilyFallback: _newlineList('strutFontFamilyFallback'),
      fontSize: _number('strutFontSize'),
      height: _number('strutHeight'),
      leadingDistribution: _textLeadingDistribution('strutLeadingDistribution'),
      leading: _number('strutLeading'),
      fontWeight: _fontWeight('strutFontWeight'),
      fontStyle: _fontStyle('strutFontStyle'),
      forceStrutHeight: _boolean('strutForceHeight'),
      debugLabel: _string('strutDebugLabel'),
      package: _string('strutPackage'),
    );
  }

  Locale? _locale({
    required String languageCode,
    required String scriptCode,
    required String countryCode,
  }) {
    if (!node.properties.containsKey(languageCode) &&
        !node.properties.containsKey(scriptCode) &&
        !node.properties.containsKey(countryCode)) {
      return null;
    }
    return Locale.fromSubtags(
      languageCode: _string(languageCode) ?? 'und',
      scriptCode: _string(scriptCode),
      countryCode: _string(countryCode),
    );
  }

  TextScaler? _textScaler() {
    final factor = _number('textScalerFactor');
    return factor == null ? null : TextScaler.linear(factor);
  }

  TextHeightBehavior? _textHeightBehavior() {
    if (!_hasPropertyPrefix('textHeight')) {
      return null;
    }
    return TextHeightBehavior(
      applyHeightToFirstAscent: _boolean('textHeightApplyFirstAscent') ?? true,
      applyHeightToLastDescent: _boolean('textHeightApplyLastDescent') ?? true,
      leadingDistribution:
          _textLeadingDistribution('textHeightLeadingDistribution') ??
          TextLeadingDistribution.proportional,
    );
  }

  TextDecoration? _textDecoration() {
    const names = {
      'styleDecorationUnderline',
      'styleDecorationOverline',
      'styleDecorationLineThrough',
    };
    if (!names.any(node.properties.containsKey)) {
      return null;
    }
    final decorations = <TextDecoration>[
      if (_boolean('styleDecorationUnderline') ?? false)
        TextDecoration.underline,
      if (_boolean('styleDecorationOverline') ?? false) TextDecoration.overline,
      if (_boolean('styleDecorationLineThrough') ?? false)
        TextDecoration.lineThrough,
    ];
    return switch (decorations.length) {
      0 => TextDecoration.none,
      1 => decorations.single,
      _ => TextDecoration.combine(decorations),
    };
  }

  List<String>? _newlineList(String name) {
    final value = _string(name);
    if (value == null) {
      return null;
    }
    return List.unmodifiable(
      value
          .split(RegExp(r'\r\n?|\n'))
          .map((entry) => entry.trim())
          .where((entry) => entry.isNotEmpty),
    );
  }

  bool _hasPropertyPrefix(String prefix) =>
      node.properties.keys.any((name) => name.startsWith(prefix));

  Widget? _single(String name) {
    final child = node.slot(name)?.child;
    return child == null ? null : _view(child);
  }

  List<Widget> _children(String name) => [
    for (final child in node.slot(name)?.children ?? const <CanvasNode>[])
      _view(child),
  ];

  Widget _view(CanvasNode child) => _CanvasNodeView(
    node: child,
    selectedWidgetId: selectedWidgetId,
    onSelected: onSelected,
    nodeKey: nodeKey,
    overlayScale: overlayScale,
  );

  String? _string(String name) {
    final property = node.properties[name];
    return property?.kind == 'string' ? property!.value as String : null;
  }

  bool? _boolean(String name) {
    final property = node.properties[name];
    return property?.kind == 'boolean' ? property!.value as bool : null;
  }

  int? _integer(String name) {
    final property = node.properties[name];
    return property?.kind == 'integer' ? property!.value as int : null;
  }

  double? _number(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is num ? value.toDouble() : null;
  }

  Color? _color(String name) {
    final property = node.properties[name];
    return property?.kind == 'color' ? Color(property!.value as int) : null;
  }

  Color? _resolvedColor(BuildContext context, String name) {
    final property = node.properties[name];
    if (property == null) {
      return null;
    }
    if (property.kind == 'color') {
      return Color(property.value as int);
    }
    final token = property.value;
    return token is CanvasThemeToken ? _themeColor(context, token) : null;
  }

  Color _colorSource(BuildContext context, CanvasColorSource source) =>
      switch (source) {
        CanvasLiteralColor(:final argb) => Color(argb),
        CanvasThemeColor(:final token) => _themeColor(context, token),
      };

  Color _themeColor(BuildContext context, CanvasThemeToken token) {
    final scheme = Theme.of(context).colorScheme;
    return switch (token.wireId) {
      'material.colorScheme.primary' => scheme.primary,
      'material.colorScheme.onPrimary' => scheme.onPrimary,
      'material.colorScheme.primaryContainer' => scheme.primaryContainer,
      'material.colorScheme.onPrimaryContainer' => scheme.onPrimaryContainer,
      'material.colorScheme.primaryFixed' => scheme.primaryFixed,
      'material.colorScheme.primaryFixedDim' => scheme.primaryFixedDim,
      'material.colorScheme.onPrimaryFixed' => scheme.onPrimaryFixed,
      'material.colorScheme.onPrimaryFixedVariant' =>
        scheme.onPrimaryFixedVariant,
      'material.colorScheme.secondary' => scheme.secondary,
      'material.colorScheme.onSecondary' => scheme.onSecondary,
      'material.colorScheme.secondaryContainer' => scheme.secondaryContainer,
      'material.colorScheme.onSecondaryContainer' =>
        scheme.onSecondaryContainer,
      'material.colorScheme.secondaryFixed' => scheme.secondaryFixed,
      'material.colorScheme.secondaryFixedDim' => scheme.secondaryFixedDim,
      'material.colorScheme.onSecondaryFixed' => scheme.onSecondaryFixed,
      'material.colorScheme.onSecondaryFixedVariant' =>
        scheme.onSecondaryFixedVariant,
      'material.colorScheme.tertiary' => scheme.tertiary,
      'material.colorScheme.onTertiary' => scheme.onTertiary,
      'material.colorScheme.tertiaryContainer' => scheme.tertiaryContainer,
      'material.colorScheme.onTertiaryContainer' => scheme.onTertiaryContainer,
      'material.colorScheme.tertiaryFixed' => scheme.tertiaryFixed,
      'material.colorScheme.tertiaryFixedDim' => scheme.tertiaryFixedDim,
      'material.colorScheme.onTertiaryFixed' => scheme.onTertiaryFixed,
      'material.colorScheme.onTertiaryFixedVariant' =>
        scheme.onTertiaryFixedVariant,
      'material.colorScheme.error' => scheme.error,
      'material.colorScheme.onError' => scheme.onError,
      'material.colorScheme.errorContainer' => scheme.errorContainer,
      'material.colorScheme.onErrorContainer' => scheme.onErrorContainer,
      'material.colorScheme.surface' => scheme.surface,
      'material.colorScheme.onSurface' => scheme.onSurface,
      'material.colorScheme.surfaceDim' => scheme.surfaceDim,
      'material.colorScheme.surfaceBright' => scheme.surfaceBright,
      'material.colorScheme.surfaceContainerLowest' =>
        scheme.surfaceContainerLowest,
      'material.colorScheme.surfaceContainerLow' => scheme.surfaceContainerLow,
      'material.colorScheme.surfaceContainer' => scheme.surfaceContainer,
      'material.colorScheme.surfaceContainerHigh' =>
        scheme.surfaceContainerHigh,
      'material.colorScheme.surfaceContainerHighest' =>
        scheme.surfaceContainerHighest,
      'material.colorScheme.onSurfaceVariant' => scheme.onSurfaceVariant,
      'material.colorScheme.outline' => scheme.outline,
      'material.colorScheme.outlineVariant' => scheme.outlineVariant,
      'material.colorScheme.shadow' => scheme.shadow,
      'material.colorScheme.scrim' => scheme.scrim,
      'material.colorScheme.inverseSurface' => scheme.inverseSurface,
      'material.colorScheme.onInverseSurface' => scheme.onInverseSurface,
      'material.colorScheme.inversePrimary' => scheme.inversePrimary,
      'material.colorScheme.surfaceTint' => scheme.surfaceTint,
      _ => throw StateError(
        'Unreviewed Canvas color theme token: ${token.wireId}',
      ),
    };
  }

  TextStyle? _themeTextStyle(BuildContext context, String propertyName) {
    final value = node.properties[propertyName]?.value;
    if (value is! CanvasThemeToken) {
      return null;
    }
    final textTheme = Theme.of(context).textTheme;
    return switch (value.wireId) {
      'material.textTheme.displayLarge' => textTheme.displayLarge,
      'material.textTheme.displayMedium' => textTheme.displayMedium,
      'material.textTheme.displaySmall' => textTheme.displaySmall,
      'material.textTheme.headlineLarge' => textTheme.headlineLarge,
      'material.textTheme.headlineMedium' => textTheme.headlineMedium,
      'material.textTheme.headlineSmall' => textTheme.headlineSmall,
      'material.textTheme.titleLarge' => textTheme.titleLarge,
      'material.textTheme.titleMedium' => textTheme.titleMedium,
      'material.textTheme.titleSmall' => textTheme.titleSmall,
      'material.textTheme.bodyLarge' => textTheme.bodyLarge,
      'material.textTheme.bodyMedium' => textTheme.bodyMedium,
      'material.textTheme.bodySmall' => textTheme.bodySmall,
      'material.textTheme.labelLarge' => textTheme.labelLarge,
      'material.textTheme.labelMedium' => textTheme.labelMedium,
      'material.textTheme.labelSmall' => textTheme.labelSmall,
      _ => throw StateError(
        'Unreviewed Canvas TextTheme token: ${value.wireId}',
      ),
    };
  }

  Paint? _paint(BuildContext context, String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasPaint) {
      return null;
    }
    return Paint()
      ..color = _colorSource(context, value.color)
      ..blendMode = _blendMode(value.blendMode)
      ..style = value.style == 'stroke'
          ? PaintingStyle.stroke
          : PaintingStyle.fill
      ..strokeWidth = value.strokeWidth
      ..strokeCap = switch (value.strokeCap) {
        'round' => StrokeCap.round,
        'square' => StrokeCap.square,
        _ => StrokeCap.butt,
      }
      ..strokeJoin = switch (value.strokeJoin) {
        'round' => StrokeJoin.round,
        'bevel' => StrokeJoin.bevel,
        _ => StrokeJoin.miter,
      }
      ..strokeMiterLimit = value.strokeMiterLimit
      ..isAntiAlias = value.antiAlias
      ..filterQuality = switch (value.filterQuality) {
        'low' => FilterQuality.low,
        'medium' => FilterQuality.medium,
        'high' => FilterQuality.high,
        _ => FilterQuality.none,
      }
      ..invertColors = value.invertColors
      ..maskFilter = value.maskFilter == null
          ? null
          : MaskFilter.blur(switch (value.maskFilter!.style) {
              'solid' => BlurStyle.solid,
              'outer' => BlurStyle.outer,
              'inner' => BlurStyle.inner,
              _ => BlurStyle.normal,
            }, value.maskFilter!.sigma);
  }

  List<Shadow>? _shadows(BuildContext context, String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasShadowValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final shadow in value)
        Shadow(
          color: _colorSource(context, shadow.color),
          offset: Offset(shadow.offsetX, shadow.offsetY),
          blurRadius: shadow.blurRadius,
        ),
    ]);
  }

  List<FontFeature>? _fontFeatures(String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasFontFeatureValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final feature in value) FontFeature(feature.tag, feature.value),
    ]);
  }

  List<FontVariation>? _fontVariations(String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasFontVariationValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final variation in value)
        FontVariation(variation.axis, variation.value),
    ]);
  }

  BlendMode _blendMode(String name) => switch (name) {
    'clear' => BlendMode.clear,
    'src' => BlendMode.src,
    'dst' => BlendMode.dst,
    'dstOver' => BlendMode.dstOver,
    'srcIn' => BlendMode.srcIn,
    'dstIn' => BlendMode.dstIn,
    'srcOut' => BlendMode.srcOut,
    'dstOut' => BlendMode.dstOut,
    'srcATop' => BlendMode.srcATop,
    'dstATop' => BlendMode.dstATop,
    'xor' => BlendMode.xor,
    'plus' => BlendMode.plus,
    'modulate' => BlendMode.modulate,
    'screen' => BlendMode.screen,
    'overlay' => BlendMode.overlay,
    'darken' => BlendMode.darken,
    'lighten' => BlendMode.lighten,
    'colorDodge' => BlendMode.colorDodge,
    'colorBurn' => BlendMode.colorBurn,
    'hardLight' => BlendMode.hardLight,
    'softLight' => BlendMode.softLight,
    'difference' => BlendMode.difference,
    'exclusion' => BlendMode.exclusion,
    'multiply' => BlendMode.multiply,
    'hue' => BlendMode.hue,
    'saturation' => BlendMode.saturation,
    'color' => BlendMode.color,
    'luminosity' => BlendMode.luminosity,
    _ => BlendMode.srcOver,
  };

  String? _enum(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is CanvasEnumValue ? value.value : null;
  }

  MainAxisAlignment _mainAxisAlignment() =>
      switch (_enum('mainAxisAlignment')) {
        'end' => MainAxisAlignment.end,
        'center' => MainAxisAlignment.center,
        'spaceBetween' => MainAxisAlignment.spaceBetween,
        'spaceAround' => MainAxisAlignment.spaceAround,
        'spaceEvenly' => MainAxisAlignment.spaceEvenly,
        _ => MainAxisAlignment.start,
      };

  MainAxisSize _mainAxisSize() =>
      _enum('mainAxisSize') == 'min' ? MainAxisSize.min : MainAxisSize.max;

  CrossAxisAlignment _crossAxisAlignment() =>
      switch (_enum('crossAxisAlignment')) {
        'start' => CrossAxisAlignment.start,
        'end' => CrossAxisAlignment.end,
        'stretch' => CrossAxisAlignment.stretch,
        'baseline' => CrossAxisAlignment.baseline,
        _ => CrossAxisAlignment.center,
      };

  TextDirection? _textDirection() => switch (_enum('textDirection')) {
    'rtl' => TextDirection.rtl,
    'ltr' => TextDirection.ltr,
    _ => null,
  };

  VerticalDirection _verticalDirection() => _enum('verticalDirection') == 'up'
      ? VerticalDirection.up
      : VerticalDirection.down;

  TextBaseline? _textBaseline([String name = 'textBaseline']) =>
      switch (_enum(name)) {
        'alphabetic' => TextBaseline.alphabetic,
        'ideographic' => TextBaseline.ideographic,
        _ => null,
      };

  FontWeight? _fontWeight(String name) => switch (_enum(name)) {
    'w100' => FontWeight.w100,
    'w200' => FontWeight.w200,
    'w300' => FontWeight.w300,
    'w400' => FontWeight.w400,
    'w500' => FontWeight.w500,
    'w600' => FontWeight.w600,
    'w700' => FontWeight.w700,
    'w800' => FontWeight.w800,
    'w900' => FontWeight.w900,
    _ => null,
  };

  FontStyle? _fontStyle(String name) => switch (_enum(name)) {
    'normal' => FontStyle.normal,
    'italic' => FontStyle.italic,
    _ => null,
  };

  TextLeadingDistribution? _textLeadingDistribution(String name) =>
      switch (_enum(name)) {
        'proportional' => TextLeadingDistribution.proportional,
        'even' => TextLeadingDistribution.even,
        _ => null,
      };

  TextDecorationStyle? _textDecorationStyle() =>
      switch (_enum('styleDecorationStyle')) {
        'solid' => TextDecorationStyle.solid,
        'double' => TextDecorationStyle.double,
        'dotted' => TextDecorationStyle.dotted,
        'dashed' => TextDecorationStyle.dashed,
        'wavy' => TextDecorationStyle.wavy,
        _ => null,
      };

  TextAlign? _textAlign() => switch (_enum('textAlign')) {
    'start' => TextAlign.start,
    'end' => TextAlign.end,
    'left' => TextAlign.left,
    'right' => TextAlign.right,
    'center' => TextAlign.center,
    'justify' => TextAlign.justify,
    _ => null,
  };

  TextOverflow? _textOverflow([String name = 'overflow']) =>
      switch (_enum(name)) {
        'clip' => TextOverflow.clip,
        'fade' => TextOverflow.fade,
        'ellipsis' => TextOverflow.ellipsis,
        'visible' => TextOverflow.visible,
        _ => null,
      };

  TextWidthBasis? _textWidthBasis() => switch (_enum('textWidthBasis')) {
    'parent' => TextWidthBasis.parent,
    'longestLine' => TextWidthBasis.longestLine,
    _ => null,
  };
}

class _CanvasWidgetOutlinePainter extends CustomPainter {
  const _CanvasWidgetOutlinePainter({
    required this.selected,
    required this.inflateEmptyLinearContainer,
    required this.visualScale,
    required this.unselectedColor,
  });

  final bool selected;
  final bool inflateEmptyLinearContainer;
  final double visualScale;
  final Color unselectedColor;

  double get debugStrokeWidth =>
      (selected ? 2 : 1) / math.max(visualScale, 0.000001);

  double get debugDashLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    const minimumExtent = 36.0;
    final width = inflateEmptyLinearContainer
        ? math.max(size.width, minimumExtent)
        : size.width;
    final height = inflateEmptyLinearContainer
        ? math.max(size.height, minimumExtent)
        : size.height;
    final rect = Rect.fromCenter(
      center: size.center(Offset.zero),
      width: width,
      height: height,
    );
    final safeScale = math.max(visualScale, 0.000001);
    final paint = Paint()
      ..color = selected ? const Color(0xff1a73e8) : unselectedColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    if (selected) {
      canvas.drawRect(rect, paint);
    } else {
      _drawDashedRect(
        canvas,
        rect,
        paint,
        dashLength: debugDashLength,
        gapLength: 3 / safeScale,
      );
    }
  }

  @override
  bool shouldRepaint(_CanvasWidgetOutlinePainter oldDelegate) =>
      selected != oldDelegate.selected ||
      inflateEmptyLinearContainer != oldDelegate.inflateEmptyLinearContainer ||
      visualScale != oldDelegate.visualScale ||
      unselectedColor != oldDelegate.unselectedColor;
}

class _CanvasPaddingGuidesPainter extends CustomPainter {
  const _CanvasPaddingGuidesPainter({
    required this.insets,
    required this.visualScale,
    required this.color,
  });

  final EdgeInsets insets;
  final double visualScale;
  final Color color;

  double get debugStrokeWidth => 1 / math.max(visualScale, 0.000001);

  double get debugCapLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    if ((size.width <= 0 && size.height <= 0) || insets == EdgeInsets.zero) {
      return;
    }
    final outer = Offset.zero & size;
    final inner = insets.deflateRect(outer);
    final innerLeft = inner.left.clamp(outer.left, outer.right).toDouble();
    final innerTop = inner.top.clamp(outer.top, outer.bottom).toDouble();
    final innerRight = inner.right.clamp(outer.left, outer.right).toDouble();
    final innerBottom = inner.bottom.clamp(outer.top, outer.bottom).toDouble();
    final guideX = ((innerLeft + innerRight) / 2)
        .clamp(outer.left, outer.right)
        .toDouble();
    final guideY = ((innerTop + innerBottom) / 2)
        .clamp(outer.top, outer.bottom)
        .toDouble();
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final capLength = debugCapLength;

    _drawMeasurement(
      canvas,
      Offset(outer.left, guideY),
      Offset(innerLeft, guideY),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(innerRight, guideY),
      Offset(outer.right, guideY),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(guideX, outer.top),
      Offset(guideX, innerTop),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(guideX, innerBottom),
      Offset(guideX, outer.bottom),
      paint,
      capLength,
    );
  }

  @override
  bool shouldRepaint(_CanvasPaddingGuidesPainter oldDelegate) =>
      insets != oldDelegate.insets ||
      visualScale != oldDelegate.visualScale ||
      color != oldDelegate.color;
}

void _drawDashedRect(
  Canvas canvas,
  Rect rect,
  Paint paint, {
  required double dashLength,
  required double gapLength,
}) {
  _drawDashedLine(
    canvas,
    rect.topLeft,
    rect.topRight,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.topRight,
    rect.bottomRight,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.bottomRight,
    rect.bottomLeft,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.bottomLeft,
    rect.topLeft,
    paint,
    dashLength,
    gapLength,
  );
}

void _drawDashedLine(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double dashLength,
  double gapLength,
) {
  final delta = end - start;
  final distance = delta.distance;
  if (distance <= 0) {
    return;
  }
  final direction = delta / distance;
  var offset = 0.0;
  while (offset < distance) {
    final dashEnd = math.min(offset + dashLength, distance);
    canvas.drawLine(
      start + direction * offset,
      start + direction * dashEnd,
      paint,
    );
    offset += dashLength + gapLength;
  }
}

void _drawMeasurement(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double capLength,
) {
  final delta = end - start;
  if (delta.distance <= 0.000001) {
    return;
  }
  canvas.drawLine(start, end, paint);
  final halfCap = capLength / 2;
  if (delta.dx.abs() >= delta.dy.abs()) {
    canvas.drawLine(
      start.translate(0, -halfCap),
      start.translate(0, halfCap),
      paint,
    );
    canvas.drawLine(
      end.translate(0, -halfCap),
      end.translate(0, halfCap),
      paint,
    );
  } else {
    canvas.drawLine(
      start.translate(-halfCap, 0),
      start.translate(halfCap, 0),
      paint,
    );
    canvas.drawLine(
      end.translate(-halfCap, 0),
      end.translate(halfCap, 0),
      paint,
    );
  }
}

class _RuntimeStatus extends StatelessWidget {
  const _RuntimeStatus({required this.message, required this.failed});

  final String message;
  final bool failed;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xfff5f6f7),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: failed ? const Color(0xffb3261e) : const Color(0xff5f6368),
              fontSize: 14,
            ),
          ),
        ),
      ),
    );
  }
}

ThemeData _theme(CanvasProfile profile, Brightness brightness) {
  final colorScheme = _applyColorSchemeOverrides(
    ColorScheme.fromSeed(
      seedColor: Color(profile.theme.seedArgb),
      brightness: brightness,
    ),
    profile.theme.colorScheme,
  );
  final base = ThemeData.from(colorScheme: colorScheme);
  return base.copyWith(
    textTheme: _applyTextThemeOverrides(
      base.textTheme,
      colorScheme,
      profile.theme.textTheme,
    ),
    platform: canvasAdaptiveTargetPlatform(profile.targetPlatform),
  );
}

ColorScheme _applyColorSchemeOverrides(
  ColorScheme base,
  Map<String, int> values,
) => base.copyWith(
  primary: _roleColor(values, 'primary'),
  onPrimary: _roleColor(values, 'onPrimary'),
  primaryContainer: _roleColor(values, 'primaryContainer'),
  onPrimaryContainer: _roleColor(values, 'onPrimaryContainer'),
  primaryFixed: _roleColor(values, 'primaryFixed'),
  primaryFixedDim: _roleColor(values, 'primaryFixedDim'),
  onPrimaryFixed: _roleColor(values, 'onPrimaryFixed'),
  onPrimaryFixedVariant: _roleColor(values, 'onPrimaryFixedVariant'),
  secondary: _roleColor(values, 'secondary'),
  onSecondary: _roleColor(values, 'onSecondary'),
  secondaryContainer: _roleColor(values, 'secondaryContainer'),
  onSecondaryContainer: _roleColor(values, 'onSecondaryContainer'),
  secondaryFixed: _roleColor(values, 'secondaryFixed'),
  secondaryFixedDim: _roleColor(values, 'secondaryFixedDim'),
  onSecondaryFixed: _roleColor(values, 'onSecondaryFixed'),
  onSecondaryFixedVariant: _roleColor(values, 'onSecondaryFixedVariant'),
  tertiary: _roleColor(values, 'tertiary'),
  onTertiary: _roleColor(values, 'onTertiary'),
  tertiaryContainer: _roleColor(values, 'tertiaryContainer'),
  onTertiaryContainer: _roleColor(values, 'onTertiaryContainer'),
  tertiaryFixed: _roleColor(values, 'tertiaryFixed'),
  tertiaryFixedDim: _roleColor(values, 'tertiaryFixedDim'),
  onTertiaryFixed: _roleColor(values, 'onTertiaryFixed'),
  onTertiaryFixedVariant: _roleColor(values, 'onTertiaryFixedVariant'),
  error: _roleColor(values, 'error'),
  onError: _roleColor(values, 'onError'),
  errorContainer: _roleColor(values, 'errorContainer'),
  onErrorContainer: _roleColor(values, 'onErrorContainer'),
  surface: _roleColor(values, 'surface'),
  onSurface: _roleColor(values, 'onSurface'),
  surfaceDim: _roleColor(values, 'surfaceDim'),
  surfaceBright: _roleColor(values, 'surfaceBright'),
  surfaceContainerLowest: _roleColor(values, 'surfaceContainerLowest'),
  surfaceContainerLow: _roleColor(values, 'surfaceContainerLow'),
  surfaceContainer: _roleColor(values, 'surfaceContainer'),
  surfaceContainerHigh: _roleColor(values, 'surfaceContainerHigh'),
  surfaceContainerHighest: _roleColor(values, 'surfaceContainerHighest'),
  onSurfaceVariant: _roleColor(values, 'onSurfaceVariant'),
  outline: _roleColor(values, 'outline'),
  outlineVariant: _roleColor(values, 'outlineVariant'),
  shadow: _roleColor(values, 'shadow'),
  scrim: _roleColor(values, 'scrim'),
  inverseSurface: _roleColor(values, 'inverseSurface'),
  onInverseSurface: _roleColor(values, 'onInverseSurface'),
  inversePrimary: _roleColor(values, 'inversePrimary'),
  surfaceTint: _roleColor(values, 'surfaceTint'),
);

Color? _roleColor(Map<String, int> values, String role) =>
    values.containsKey(role) ? Color(values[role]!) : null;

TextTheme _applyTextThemeOverrides(
  TextTheme base,
  ColorScheme colorScheme,
  Map<String, CanvasThemeTextStyleOverride> values,
) => base.copyWith(
  displayLarge: _textRole(
    base.displayLarge,
    colorScheme,
    values['displayLarge'],
  ),
  displayMedium: _textRole(
    base.displayMedium,
    colorScheme,
    values['displayMedium'],
  ),
  displaySmall: _textRole(
    base.displaySmall,
    colorScheme,
    values['displaySmall'],
  ),
  headlineLarge: _textRole(
    base.headlineLarge,
    colorScheme,
    values['headlineLarge'],
  ),
  headlineMedium: _textRole(
    base.headlineMedium,
    colorScheme,
    values['headlineMedium'],
  ),
  headlineSmall: _textRole(
    base.headlineSmall,
    colorScheme,
    values['headlineSmall'],
  ),
  titleLarge: _textRole(base.titleLarge, colorScheme, values['titleLarge']),
  titleMedium: _textRole(base.titleMedium, colorScheme, values['titleMedium']),
  titleSmall: _textRole(base.titleSmall, colorScheme, values['titleSmall']),
  bodyLarge: _textRole(base.bodyLarge, colorScheme, values['bodyLarge']),
  bodyMedium: _textRole(base.bodyMedium, colorScheme, values['bodyMedium']),
  bodySmall: _textRole(base.bodySmall, colorScheme, values['bodySmall']),
  labelLarge: _textRole(base.labelLarge, colorScheme, values['labelLarge']),
  labelMedium: _textRole(base.labelMedium, colorScheme, values['labelMedium']),
  labelSmall: _textRole(base.labelSmall, colorScheme, values['labelSmall']),
);

TextStyle? _textRole(
  TextStyle? base,
  ColorScheme colorScheme,
  CanvasThemeTextStyleOverride? value,
) {
  if (value == null) {
    return null;
  }
  return (base ?? const TextStyle()).copyWith(
    color: _themeOverrideColor(value.color, colorScheme),
    backgroundColor: _themeOverrideColor(value.backgroundColor, colorScheme),
    fontSize: value.fontSize,
    fontWeight: _fontWeight(value.fontWeight),
    fontStyle: switch (value.fontStyle) {
      'normal' => FontStyle.normal,
      'italic' => FontStyle.italic,
      _ => null,
    },
    letterSpacing: value.letterSpacing,
    wordSpacing: value.wordSpacing,
    height: value.height,
    fontFamily: value.fontFamily,
    decoration: _decoration(value.decoration),
    decorationColor: _themeOverrideColor(value.decorationColor, colorScheme),
    decorationStyle: switch (value.decorationStyle) {
      'solid' => TextDecorationStyle.solid,
      'double' => TextDecorationStyle.double,
      'dotted' => TextDecorationStyle.dotted,
      'dashed' => TextDecorationStyle.dashed,
      'wavy' => TextDecorationStyle.wavy,
      _ => null,
    },
    decorationThickness: value.decorationThickness,
  );
}

Color? _themeOverrideColor(
  CanvasThemeColorValue? value,
  ColorScheme colorScheme,
) => switch (value) {
  CanvasThemeLiteralColor(:final argb) => Color(argb),
  CanvasThemeRoleColor(:final role) => _colorSchemeRole(colorScheme, role),
  null => null,
};

Color _colorSchemeRole(ColorScheme scheme, String role) => switch (role) {
  'primary' => scheme.primary,
  'onPrimary' => scheme.onPrimary,
  'primaryContainer' => scheme.primaryContainer,
  'onPrimaryContainer' => scheme.onPrimaryContainer,
  'primaryFixed' => scheme.primaryFixed,
  'primaryFixedDim' => scheme.primaryFixedDim,
  'onPrimaryFixed' => scheme.onPrimaryFixed,
  'onPrimaryFixedVariant' => scheme.onPrimaryFixedVariant,
  'secondary' => scheme.secondary,
  'onSecondary' => scheme.onSecondary,
  'secondaryContainer' => scheme.secondaryContainer,
  'onSecondaryContainer' => scheme.onSecondaryContainer,
  'secondaryFixed' => scheme.secondaryFixed,
  'secondaryFixedDim' => scheme.secondaryFixedDim,
  'onSecondaryFixed' => scheme.onSecondaryFixed,
  'onSecondaryFixedVariant' => scheme.onSecondaryFixedVariant,
  'tertiary' => scheme.tertiary,
  'onTertiary' => scheme.onTertiary,
  'tertiaryContainer' => scheme.tertiaryContainer,
  'onTertiaryContainer' => scheme.onTertiaryContainer,
  'tertiaryFixed' => scheme.tertiaryFixed,
  'tertiaryFixedDim' => scheme.tertiaryFixedDim,
  'onTertiaryFixed' => scheme.onTertiaryFixed,
  'onTertiaryFixedVariant' => scheme.onTertiaryFixedVariant,
  'error' => scheme.error,
  'onError' => scheme.onError,
  'errorContainer' => scheme.errorContainer,
  'onErrorContainer' => scheme.onErrorContainer,
  'surface' => scheme.surface,
  'onSurface' => scheme.onSurface,
  'surfaceDim' => scheme.surfaceDim,
  'surfaceBright' => scheme.surfaceBright,
  'surfaceContainerLowest' => scheme.surfaceContainerLowest,
  'surfaceContainerLow' => scheme.surfaceContainerLow,
  'surfaceContainer' => scheme.surfaceContainer,
  'surfaceContainerHigh' => scheme.surfaceContainerHigh,
  'surfaceContainerHighest' => scheme.surfaceContainerHighest,
  'onSurfaceVariant' => scheme.onSurfaceVariant,
  'outline' => scheme.outline,
  'outlineVariant' => scheme.outlineVariant,
  'shadow' => scheme.shadow,
  'scrim' => scheme.scrim,
  'inverseSurface' => scheme.inverseSurface,
  'onInverseSurface' => scheme.onInverseSurface,
  'inversePrimary' => scheme.inversePrimary,
  'surfaceTint' => scheme.surfaceTint,
  _ => throw StateError('Unreviewed Canvas ColorScheme role: $role'),
};

FontWeight? _fontWeight(String? value) => switch (value) {
  'w100' => FontWeight.w100,
  'w200' => FontWeight.w200,
  'w300' => FontWeight.w300,
  'w400' => FontWeight.w400,
  'w500' => FontWeight.w500,
  'w600' => FontWeight.w600,
  'w700' => FontWeight.w700,
  'w800' => FontWeight.w800,
  'w900' => FontWeight.w900,
  _ => null,
};

TextDecoration? _decoration(Set<String>? value) {
  if (value == null) {
    return null;
  }
  if (value.isEmpty) {
    return TextDecoration.none;
  }
  final lines = <TextDecoration>[
    if (value.contains('underline')) TextDecoration.underline,
    if (value.contains('overline')) TextDecoration.overline,
    if (value.contains('lineThrough')) TextDecoration.lineThrough,
  ];
  return lines.length == 1 ? lines.single : TextDecoration.combine(lines);
}

/// Resolves Flutter adaptive widget semantics inside the native desktop engine.
///
/// Web has no [TargetPlatform] value. The Windows-hosted runner therefore uses
/// Windows adaptive controls while the exact Web preview mode supplies the
/// browser-sized responsive viewport. This is a layout preview, not an emulated
/// browser runtime, so browser-only behavior such as `kIsWeb` remains out of scope.
TargetPlatform canvasAdaptiveTargetPlatform(String platform) =>
    switch (platform) {
      'android' => TargetPlatform.android,
      'ios' => TargetPlatform.iOS,
      'macos' => TargetPlatform.macOS,
      'linux' => TargetPlatform.linux,
      'windows' => TargetPlatform.windows,
      'web' => TargetPlatform.windows,
      _ => throw ArgumentError.value(
        platform,
        'platform',
        'Unsupported target',
      ),
    };

String _displayType(String type) => type.substring(type.lastIndexOf('.') + 1);
