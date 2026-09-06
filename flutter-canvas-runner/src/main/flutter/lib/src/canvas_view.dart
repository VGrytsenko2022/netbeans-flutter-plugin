import 'dart:convert';
import 'dart:math' as math;
import 'dart:ui' as ui show BoxHeightStyle, BoxWidthStyle;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show OverflowBoxFit, ScrollCacheExtent;
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

import 'canvas_drop.dart';
import 'canvas_model.dart';
import 'canvas_runtime.dart';

bool _ignoreDeleteSelected() => false;
bool _ignoreInlineTextCommit(
  String widgetId,
  String text,
  bool compositionObserved,
) => false;

String _customClipperPreviewUnavailableMessage({
  required String widgetName,
  required String expectedType,
}) =>
    'Custom $widgetName preview unavailable. Generated Dart uses the '
    'configured $expectedType; isolated Canvas does not execute project '
    'or dependency Dart.';

String? _customClipperPreviewUnavailableMessageForNode(CanvasNode node) {
  if (node.type == 'flutter.material.Card') {
    return _cardShapePreviewUnavailableMessage(node);
  }
  if (node.type == 'flutter.widgets.ClipPath' &&
      node.properties['shape']?.kind == 'dartObjectReferencePresence') {
    return _customClipperPreviewUnavailableMessage(
      widgetName: 'ClipPath.shape',
      expectedType: 'ShapeBorder',
    );
  }
  if (node.properties['clipper']?.kind != 'dartObjectReferencePresence') {
    return null;
  }
  final expectedType = switch (node.type) {
    'flutter.widgets.ClipRect' ||
    'flutter.widgets.ClipOval' => 'CustomClipper<Rect>',
    'flutter.widgets.ClipRRect' => 'CustomClipper<RRect>',
    'flutter.widgets.ClipRSuperellipse' => 'CustomClipper<RSuperellipse>',
    'flutter.widgets.ClipPath' ||
    'flutter.widgets.PhysicalShape' => 'CustomClipper<Path>',
    _ => null,
  };
  return expectedType == null
      ? null
      : _customClipperPreviewUnavailableMessage(
          widgetName: _displayType(node.type),
          expectedType: expectedType,
        );
}

// This is an isolated-preview complexity budget, not an SDK/source domain limit.
const _maximumCanvasCardShapePoints = 4096;
String? _cardShapePreviewUnavailableMessage(CanvasNode node) {
  if (node.properties['shape']?.kind == 'dartObjectReferencePresence') {
    return _customClipperPreviewUnavailableMessage(
      widgetName: 'Card.shape',
      expectedType: 'ShapeBorder',
    );
  }
  final kind = node.properties['shapeKind']?.value;
  final points = node.properties['shapePoints']?.value as num? ?? 5;
  if ((kind == 'star' || kind == 'polygon') &&
      points > _maximumCanvasCardShapePoints) {
    return 'Card.shape $kind preview unavailable: requested $points points exceeds the isolated Canvas budget of $_maximumCanvasCardShapePoints. Generated Dart preserves the configured ShapeBorder; child and properties remain editable.';
  }
  return null;
}

final Uint8List _unavailableImageBytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);

TextDirection _canvasTextDirection(String locale) {
  final language = locale.split(RegExp('[-_]')).first.toLowerCase();
  return const {
        'ar',
        'arc',
        'ckb',
        'dv',
        'fa',
        'he',
        'ks',
        'ku',
        'nqo',
        'ps',
        'sd',
        'syr',
        'ug',
        'ur',
        'yi',
      }.contains(language)
      ? TextDirection.rtl
      : TextDirection.ltr;
}

class NativeCanvasApp extends StatelessWidget {
  const NativeCanvasApp({required this.runtime, super.key});

  final CanvasRuntimeController runtime;

  @override
  Widget build(BuildContext context) {
    runtime.bindSurfaceView(View.of(context));
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
          imageResources: runtime.imageResources,
          onImageError: runtime.reportImageRenderError,
          selectedWidgetId: runtime.selectedWidgetId,
          onSelected: runtime.selectFromCanvas,
          onInteraction: runtime.interactFromCanvas,
          interactionInputSynchronized: runtime.interactionInputSynchronized,
          onDeleteSelected: runtime.deleteSelectedFromCanvas,
          inlineTextEditEnabled: runtime.inlineTextEditNegotiated,
          onInlineTextCommit: runtime.commitInlineTextEdit,
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
    this.imageResources,
    this.onImageError,
    this.onInteraction,
    this.interactionInputSynchronized = true,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.inlineTextEditEnabled = false,
    this.onInlineTextCommit = _ignoreInlineTextCommit,
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
  final CanvasImageResourceBundle? imageResources;
  final CanvasImageErrorReporter? onImageError;
  final VoidCallback? onInteraction;
  final bool interactionInputSynchronized;
  final bool Function() onDeleteSelected;
  final bool inlineTextEditEnabled;
  final bool Function(String, String, bool) onInlineTextCommit;
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
      home: Directionality(
        textDirection: _canvasTextDirection(model.profile.locale),
        child: CanvasDocumentView(
          model: model,
          imageResources: imageResources,
          onImageError: onImageError,
          selectedWidgetId: selectedWidgetId,
          onSelected: onSelected,
          onInteraction: onInteraction,
          interactionInputSynchronized: interactionInputSynchronized,
          onDeleteSelected: onDeleteSelected,
          inlineTextEditEnabled: inlineTextEditEnabled,
          onInlineTextCommit: onInlineTextCommit,
          dropHoverTarget: dropHoverTarget,
          dropIndicatorKind: dropIndicatorKind,
          onDropResolverChanged: onDropResolverChanged,
          onMovePreviewResolverChanged: onMovePreviewResolverChanged,
          viewportPresentation: viewportPresentation,
          onViewportPresentationChanged: onViewportPresentationChanged,
          onViewportMetricsChanged: onViewportMetricsChanged,
        ),
      ),
    );
  }
}

class CanvasDocumentView extends StatefulWidget {
  const CanvasDocumentView({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    this.imageResources,
    this.onImageError,
    this.onInteraction,
    this.interactionInputSynchronized = true,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.inlineTextEditEnabled = false,
    this.onInlineTextCommit = _ignoreInlineTextCommit,
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
  final CanvasImageResourceBundle? imageResources;
  final CanvasImageErrorReporter? onImageError;
  final VoidCallback? onInteraction;
  final bool interactionInputSynchronized;
  final bool Function() onDeleteSelected;
  final bool inlineTextEditEnabled;
  final bool Function(String, String, bool) onInlineTextCommit;
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
  static const double _minimumZeroSizedWidgetTarget = 36;
  static const double _moveInsertionMarkerExtent = 12;
  static const double _scaffoldFabDropExtent = 72;

  final GlobalKey _surfaceKey = GlobalKey();
  final Map<String, GlobalKey> _nodeKeys = <String, GlobalKey>{};
  final FocusNode _focusNode = FocusNode(debugLabel: 'native-canvas');
  _ViewportGeometry? _viewportGeometry;
  CanvasViewportMetrics? _lastReportedViewportMetrics;
  List<_ZeroSizedWidgetTargetGroup> _zeroSizedWidgetTargets = const [];
  bool _zeroSizedWidgetTargetRefreshScheduled = false;
  _InlineTextEditSession? _inlineTextEditSession;

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
    if (_inlineTextEditSession case final session?
        when !_inlineTextEditStillCurrent(session)) {
      _inlineTextEditSession = null;
      _restoreCanvasFocusAfterFrame();
    }
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
              _refreshZeroSizedWidgetTargetsAfterFrame();
              return Listener(
                key: const ValueKey('canvas-interaction-surface'),
                behavior: HitTestBehavior.opaque,
                onPointerDown: (_) {
                  if (widget.interactionInputSynchronized) {
                    if (_inlineTextEditSession == null) {
                      _focusNode.requestFocus();
                    }
                    widget.onInteraction?.call();
                  }
                },
                onPointerSignal: widget.interactionInputSynchronized
                    ? _onPointerSignal
                    : null,
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
                                imageResources:
                                    widget.imageResources ??
                                    CanvasImageResourceBundle.empty,
                                onImageError: widget.onImageError,
                                selectedWidgetId: widget.selectedWidgetId,
                                onSelected: _selectWidget,
                                nodeKey: _nodeKey,
                                designerFocusParent: _focusNode,
                                overlayScale: geometry.scale,
                                inlineTextEditEnabled:
                                    widget.inlineTextEditEnabled &&
                                    widget.interactionInputSynchronized,
                                inlineTextEditingWidgetId:
                                    _inlineTextEditSession?.widgetId,
                                onBeginInlineTextEdit: _beginInlineTextEdit,
                                onCommitInlineTextEdit: _commitInlineTextEdit,
                                onCancelInlineTextEdit: _cancelInlineTextEdit,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    for (final target in _zeroSizedWidgetTargets)
                      Positioned.fromRect(
                        rect: target.rect,
                        child: _ZeroSizedWidgetTarget(
                          widgetIds: target.widgetIds,
                          widgetTypes: target.widgetTypes,
                          previewUnavailableMessages:
                              target.previewUnavailableMessages,
                          selectedWidgetId: widget.selectedWidgetId,
                          dark: dark,
                          onSelected: _selectWidget,
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
                    if (!widget.interactionInputSynchronized)
                      const Positioned.fill(
                        child: _InteractionBarrierOverlay(),
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

  void _refreshZeroSizedWidgetTargetsAfterFrame() {
    if (_zeroSizedWidgetTargetRefreshScheduled) {
      return;
    }
    _zeroSizedWidgetTargetRefreshScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _zeroSizedWidgetTargetRefreshScheduled = false;
      if (!mounted) {
        return;
      }
      final surface = _renderBox(_surfaceKey);
      final currentGeometry = _viewportGeometry;
      if (surface == null || currentGeometry == null) {
        return;
      }
      final surfaceRect = _finiteGlobalRect(surface);
      if (surfaceRect == null) {
        return;
      }
      final viewportRect = Rect.fromLTWH(
        currentGeometry.left,
        currentGeometry.top,
        currentGeometry.renderedWidth,
        currentGeometry.renderedHeight,
      );
      final coincidentTargets = <Rect, List<CanvasNode>>{};
      for (final node in _zeroSizedDesignerTargets(widget.model.root)) {
        final box = _renderBox(_nodeKeys[node.id]);
        // Spacer owns no child where instrumentation can safely live. An
        // external target is therefore required even when stretch gives its
        // internal SizedBox a non-zero cross-axis extent.
        final ignoresPointers = _ignoresPointersForNode(node);
        final alwaysUsesSurfaceOverlay =
            node.type == canvasSpacerWidgetType || ignoresPointers;
        if (box == null || (!alwaysUsesSurfaceOverlay && !box.size.isEmpty)) {
          continue;
        }
        final globalRect = _finiteGlobalRect(box);
        if (globalRect == null) {
          continue;
        }
        final rendered = globalRect.shift(-surfaceRect.topLeft);
        // The real IgnorePointer must remain transparent to pointer hits.
        // Put its Designer selection handle outside a nonempty body when
        // space permits; never replace the body with a full-area hit target.
        final handleRect = ignoresPointers && !box.size.isEmpty
            ? _ignorePointerHandleRect(rendered, viewportRect)
            : rendered;
        final target = _boundedDesignerHitRect(
          handleRect,
          viewportRect,
          minimumExtent: _minimumZeroSizedWidgetTarget,
        );
        coincidentTargets.putIfAbsent(target, () => <CanvasNode>[]).add(node);
      }
      final targets = <_ZeroSizedWidgetTargetGroup>[
        for (final target in coincidentTargets.entries)
          _ZeroSizedWidgetTargetGroup(
            rect: target.key,
            widgetIds: List.unmodifiable([
              for (final node in target.value) node.id,
            ]),
            widgetTypes: List.unmodifiable([
              for (final node in target.value) node.type,
            ]),
            previewUnavailableMessages: List.unmodifiable([
              for (final node in target.value)
                _customClipperPreviewUnavailableMessageForNode(node),
            ]),
          ),
      ];
      if (_sameTargets(_zeroSizedWidgetTargets, targets)) {
        return;
      }
      setState(() => _zeroSizedWidgetTargets = List.unmodifiable(targets));
    });
  }

  Iterable<CanvasNode> _zeroSizedDesignerTargets(CanvasNode node) sync* {
    if ((node.type == 'flutter.widgets.SizedBox' &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        (node.type == 'flutter.widgets.Container' &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        node.type == 'flutter.widgets.DecoratedBox' ||
        node.type == 'flutter.widgets.ExcludeSemantics' ||
        node.type == 'flutter.widgets.ExcludeFocus' ||
        node.type == 'flutter.widgets.ExcludeFocusTraversal' ||
        node.type == 'flutter.widgets.Visibility' ||
        node.type == 'flutter.widgets.TickerMode' ||
        node.type == 'flutter.widgets.DefaultTextHeightBehavior' ||
        node.type == 'flutter.widgets.DefaultSelectionStyle' ||
        node.type == 'flutter.widgets.IconTheme' ||
        node.type == 'flutter.widgets.IgnorePointer' ||
        node.type == 'flutter.widgets.AbsorbPointer' ||
        node.type == 'flutter.widgets.BlockSemantics' ||
        node.type == 'flutter.widgets.MergeSemantics' ||
        node.type == 'flutter.widgets.IndexedSemantics' ||
        node.type == 'flutter.widgets.RepaintBoundary' ||
        node.type == 'flutter.widgets.ColoredBox' ||
        (node.type == 'flutter.widgets.Opacity' &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        (node.type == 'flutter.widgets.Align' &&
            ((node.slot('child')?.children.isEmpty ?? true) ||
                node.properties['widthFactor']?.value == 0 ||
                node.properties['heightFactor']?.value == 0)) ||
        (node.type == 'flutter.widgets.FractionallySizedBox' &&
            ((node.slot('child')?.children.isEmpty ?? true) ||
                node.properties['widthFactor']?.value == 0 ||
                node.properties['heightFactor']?.value == 0)) ||
        node.type == 'flutter.widgets.Baseline' ||
        node.type == 'flutter.widgets.IntrinsicHeight' ||
        node.type == 'flutter.widgets.IntrinsicWidth' ||
        node.type == 'flutter.widgets.Offstage' ||
        node.type == canvasDirectionalityWidgetType ||
        node.type == 'flutter.widgets.Placeholder' ||
        node.type == 'flutter.widgets.ClipOval' ||
        node.type == 'flutter.widgets.ClipRRect' ||
        node.type == 'flutter.widgets.ClipRSuperellipse' ||
        node.type == 'flutter.widgets.PhysicalModel' ||
        node.type == 'flutter.widgets.PhysicalShape' ||
        node.type == 'flutter.widgets.ClipPath' ||
        node.type == 'flutter.widgets.ClipRect' ||
        node.type == 'flutter.widgets.RotatedBox' ||
        node.type == 'flutter.widgets.SizedOverflowBox' ||
        node.type == 'flutter.widgets.Transform' ||
        node.type == 'flutter.widgets.ConstrainedBox' ||
        node.type == 'flutter.widgets.UnconstrainedBox' ||
        node.type == 'flutter.widgets.LimitedBox' ||
        node.type == 'flutter.widgets.OverflowBox' ||
        node.type == 'flutter.widgets.FittedBox' ||
        node.type == 'flutter.widgets.Expanded' ||
        node.type == 'flutter.widgets.Flexible' ||
        node.type == canvasSafeAreaWidgetType ||
        node.type == canvasSpacerWidgetType ||
        node.type == 'flutter.widgets.Stack' ||
        node.type == 'flutter.widgets.IndexedStack' ||
        node.type == 'flutter.widgets.Wrap' ||
        node.type == 'flutter.widgets.ListBody' ||
        node.type == 'flutter.widgets.OverflowBar' ||
        node.type == 'flutter.widgets.ListView' ||
        node.type == 'flutter.widgets.GridView' ||
        node.type == 'flutter.widgets.SingleChildScrollView' ||
        node.type == 'flutter.widgets.Image' ||
        node.type == 'flutter.widgets.ImageIcon' ||
        node.type == 'flutter.material.Divider' ||
        node.type == 'flutter.material.VerticalDivider' ||
        node.type == 'flutter.material.Card' ||
        node.type == 'flutter.material.Badge' ||
        node.type == 'flutter.material.CircleAvatar' ||
        node.type == 'flutter.widgets.Icon') {
      yield node;
    }
    for (final slotEntry in node.slots.entries) {
      for (final index in _interactiveChildIndexes(
        node,
        slotEntry.key,
        slotEntry.value,
      )) {
        final child = slotEntry.value.children[index];
        yield* _zeroSizedDesignerTargets(child);
      }
    }
  }

  static bool _sameTargets(
    List<_ZeroSizedWidgetTargetGroup> left,
    List<_ZeroSizedWidgetTargetGroup> right,
  ) {
    if (left.length != right.length) {
      return false;
    }
    for (var targetIndex = 0; targetIndex < left.length; targetIndex++) {
      final leftTarget = left[targetIndex];
      final rightTarget = right[targetIndex];
      if (leftTarget.rect != rightTarget.rect ||
          leftTarget.widgetIds.length != rightTarget.widgetIds.length ||
          leftTarget.widgetTypes.length != rightTarget.widgetTypes.length ||
          leftTarget.previewUnavailableMessages.length !=
              rightTarget.previewUnavailableMessages.length) {
        return false;
      }
      for (
        var widgetIndex = 0;
        widgetIndex < leftTarget.widgetIds.length;
        widgetIndex++
      ) {
        if (leftTarget.widgetIds[widgetIndex] !=
                rightTarget.widgetIds[widgetIndex] ||
            leftTarget.widgetTypes[widgetIndex] !=
                rightTarget.widgetTypes[widgetIndex] ||
            leftTarget.previewUnavailableMessages[widgetIndex] !=
                rightTarget.previewUnavailableMessages[widgetIndex]) {
          return false;
        }
      }
    }
    return true;
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
    if (_inlineTextEditSession == null) {
      _focusNode.requestFocus();
    }
    widget.onViewportPresentationChanged?.call(presentation);
  }

  void _selectWidget(String widgetId) {
    if (_inlineTextEditSession != null &&
        SchedulerBinding.instance.schedulerPhase ==
            SchedulerPhase.persistentCallbacks) {
      return;
    }
    if (_inlineTextEditSession?.widgetId != widgetId) {
      _cancelInlineTextEdit();
    }
    if (_inlineTextEditSession != null) {
      return;
    }
    _focusNode.requestFocus();
    widget.onSelected(widgetId);
  }

  KeyEventResult _onKeyEvent(FocusNode node, KeyEvent event) {
    if (!widget.interactionInputSynchronized) {
      return KeyEventResult.handled;
    }
    if (event is! KeyDownEvent) {
      return KeyEventResult.ignored;
    }
    final keyboard = HardwareKeyboard.instance;
    final unmodified =
        !keyboard.isControlPressed &&
        !keyboard.isShiftPressed &&
        !keyboard.isAltPressed &&
        !keyboard.isMetaPressed;
    if (event.logicalKey == LogicalKeyboardKey.f2 && unmodified) {
      if (_inlineTextEditSession != null) {
        return KeyEventResult.handled;
      }
      final selected = widget.selectedWidgetId;
      return selected != null && _beginInlineTextEdit(selected)
          ? KeyEventResult.handled
          : KeyEventResult.ignored;
    }
    if (event.logicalKey == LogicalKeyboardKey.delete &&
        _inlineTextEditSession != null) {
      return KeyEventResult.ignored;
    }
    if (event.logicalKey != LogicalKeyboardKey.delete ||
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

  bool _beginInlineTextEdit(String widgetId) {
    if (!widget.inlineTextEditEnabled ||
        !widget.interactionInputSynchronized ||
        widget.selectedWidgetId != widgetId) {
      return false;
    }
    final node = _findCanvasNode(widget.model.root, widgetId);
    if (node == null ||
        node.type != 'flutter.widgets.Text' ||
        node.properties['data']?.value is! String ||
        !_isInteractiveDescendant(widget.model.root, widgetId)) {
      return false;
    }
    if (_inlineTextEditSession?.widgetId == widgetId) {
      return true;
    }
    setState(() {
      _inlineTextEditSession = _InlineTextEditSession(
        widgetId: widgetId,
        presentationSequence: widget.model.presentationSequence,
        documentId: widget.model.documentId,
        logicalRevisionId: widget.model.logicalRevisionId,
      );
    });
    return true;
  }

  bool _commitInlineTextEdit(
    String widgetId,
    String text,
    bool compositionObserved,
  ) {
    final session = _inlineTextEditSession;
    if (session == null ||
        session.widgetId != widgetId ||
        !_inlineTextEditStillCurrent(session)) {
      _cancelInlineTextEdit();
      return false;
    }
    final accepted = widget.onInlineTextCommit(
      widgetId,
      text,
      compositionObserved,
    );
    if (accepted) {
      _cancelInlineTextEdit();
    }
    return accepted;
  }

  void _cancelInlineTextEdit() {
    if (_inlineTextEditSession == null) {
      return;
    }
    setState(() => _inlineTextEditSession = null);
    _restoreCanvasFocusAfterFrame();
  }

  bool _inlineTextEditStillCurrent(_InlineTextEditSession session) {
    if (!widget.inlineTextEditEnabled ||
        !widget.interactionInputSynchronized ||
        widget.selectedWidgetId != session.widgetId ||
        widget.model.presentationSequence != session.presentationSequence ||
        widget.model.documentId != session.documentId ||
        widget.model.logicalRevisionId != session.logicalRevisionId) {
      return false;
    }
    return _findCanvasNode(widget.model.root, session.widgetId)?.type ==
            'flutter.widgets.Text' &&
        _isInteractiveDescendant(widget.model.root, session.widgetId);
  }

  void _restoreCanvasFocusAfterFrame() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted &&
          widget.interactionInputSynchronized &&
          _inlineTextEditSession == null) {
        _focusNode.requestFocus();
      }
    });
  }

  GlobalKey _nodeKey(String id) =>
      _nodeKeys.putIfAbsent(id, () => GlobalKey(debugLabel: 'canvas-$id'));

  CanvasDropTarget? _resolveDrop(
    int xMicros,
    int yMicros, [
    CanvasPaletteDragSource? source,
  ]) {
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
    final surfaceRect = _finiteGlobalRect(surface);
    if (surfaceRect == null) {
      return null;
    }
    final point = Offset(
      surfaceRect.left + surfaceRect.width * xMicros / _microsPerSurface,
      surfaceRect.top + surfaceRect.height * yMicros / _microsPerSurface,
    );
    final candidates = <_DropCandidate>[];
    final effectiveSource =
        source ??
        CanvasPaletteDragSource(
          token: '',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        );
    _collectDropCandidates(
      widget.model.root,
      point,
      surfaceRect,
      0,
      candidates,
      effectiveSource,
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
    final requiredChildOwner = _requiredChildOwner(
      widget.model.root,
      sourceWidgetId,
    );
    if (requiredChildOwner != null &&
        (requiredChildOwner.id != parentWidgetId || slotName != 'child')) {
      // A move cannot expose an invalid empty required slot. Keep same-slot
      // no-op previews; the host still owns final mutation validation.
      return null;
    }
    if (!_isInteractiveDescendant(widget.model.root, parentWidgetId) ||
        !_isEligibleDropSlot(parentNode, slotName)) {
      return null;
    }
    final modelSlot = parentNode.slot(slotName);
    final reviewedSlot = canvasDropSlotForWidgetSlot(parentNode.type, slotName);
    final slotKind = modelSlot?.kind ?? reviewedSlot?.modelSlotKind;
    if (slotKind == null) {
      return null;
    }
    final surfaceRect = _finiteGlobalRect(surface);
    final renderedParentRect = _finiteGlobalRect(parentBox);
    if (surfaceRect == null ||
        renderedParentRect == null ||
        !_hasFiniteGlobalInverse(parentBox)) {
      return null;
    }
    final zeroSizedParent = parentBox.size.isEmpty;
    final parentRect = zeroSizedParent
        ? _boundedDesignerHitRect(renderedParentRect, surfaceRect)
        : Offset.zero & parentBox.size;
    if (parentRect.isEmpty && !zeroSizedParent) {
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
      zone = switch (reviewedSlot?.zonePlacement) {
        CanvasDropZonePlacement.badgeLabel => _badgeLabelZone(parentRect),
        CanvasDropZonePlacement.fullNode || null => parentRect,
        CanvasDropZonePlacement.terminalList => _terminalZone(
          parentNode,
          parentBox,
          parentRect,
          slotName,
        ),
        CanvasDropZonePlacement.existingChild => Rect.zero,
        CanvasDropZonePlacement.bottomRightCompact => _bottomRightCompactZone(
          parentRect,
        ),
        CanvasDropZonePlacement.appBarLeading => _appBarLeadingZone(
          parentNode,
          parentBox,
          parentRect,
        ),
        CanvasDropZonePlacement.appBarTitle => _appBarTitleZone(
          parentNode,
          parentBox,
          parentRect,
        ),
        CanvasDropZonePlacement.appBarActions => _terminalZone(
          parentNode,
          parentBox,
          _appBarToolbarZone(parentNode, parentBox, parentRect),
          slotName,
        ),
        CanvasDropZonePlacement.appBarFlexibleSpace => parentRect,
        CanvasDropZonePlacement.appBarBottom => _appBarBottomZone(
          parentNode,
          parentBox,
          parentRect,
        ),
      };
    } else if (slotKind == 'list') {
      final children = <CanvasNode>[
        for (final child in modelSlot?.children ?? const <CanvasNode>[])
          if (child.id != sourceWidgetId) child,
      ];
      if (insertionIndex < 0 || insertionIndex > children.length) {
        return null;
      }
      if (zeroSizedParent && children.isNotEmpty) {
        return null;
      }
      final listParentRect =
          parentNode.type == 'flutter.material.AppBar' && slotName == 'actions'
          ? _appBarToolbarZone(parentNode, parentBox, parentRect)
          : parentRect;
      zone =
          parentNode.type == 'flutter.material.AppBar' &&
              slotName == 'actions' &&
              children.isEmpty
          ? _terminalZone(
              parentNode,
              parentBox,
              listParentRect,
              slotName,
              effectiveChildren: children,
            )
          : _listMoveInsertionZone(
              parentNode,
              parentBox,
              listParentRect,
              children,
              insertionIndex,
              slotName,
            );
    } else {
      return null;
    }
    if (zone.isEmpty) {
      return null;
    }
    final globalZone = zeroSizedParent
        ? zone
        : _finiteGlobalRect(parentBox, localRect: zone);
    if (globalZone == null) {
      return null;
    }
    final boundedGlobalZone = _boundedDesignerHitRect(globalZone, surfaceRect);
    if (boundedGlobalZone.isEmpty) {
      return null;
    }
    return CanvasDropTarget(
      parentWidgetId: parentWidgetId,
      slotName: slotName,
      insertionIndex: insertionIndex,
      zone: _normalizeZone(surfaceRect, boundedGlobalZone),
    );
  }

  Rect _listMoveInsertionZone(
    CanvasNode parentNode,
    RenderBox parentBox,
    Rect parentRect,
    List<CanvasNode> children,
    int insertionIndex,
    String slotName,
  ) {
    if (parentNode.type == 'flutter.widgets.GridView' &&
        slotName == 'children') {
      return _gridInsertionZone(
        parentNode,
        parentBox,
        parentRect,
        children,
        insertionIndex,
        markerExtent: _moveInsertionMarkerExtent,
      );
    }
    if ((parentNode.type == 'flutter.widgets.Stack' ||
            parentNode.type == 'flutter.widgets.IndexedStack' ||
            parentNode.type == 'flutter.widgets.Wrap') &&
        slotName == 'children') {
      return parentRect;
    }
    if (children.isEmpty) {
      return parentRect;
    }
    final overflowBarVertical =
        parentNode.type == 'flutter.widgets.OverflowBar' &&
        _isOverflowBarVertical(parentNode, parentBox);
    final horizontal =
        parentNode.type == 'flutter.widgets.Row' ||
        parentNode.type == 'flutter.material.AppBar' ||
        _isHorizontalListBody(parentNode) ||
        _isHorizontalListView(parentNode) ||
        (parentNode.type == 'flutter.widgets.OverflowBar' &&
            !overflowBarVertical);
    final reverse = switch (parentNode.type) {
      'flutter.widgets.ListBody' => _isVisuallyReversedListBody(parentNode),
      'flutter.widgets.ListView' => _isVisuallyReversedListView(parentNode),
      'flutter.widgets.OverflowBar' =>
        overflowBarVertical
            ? _enumValue(parentNode, 'overflowDirection') == 'up'
            : _resolvedTextDirection(parentNode) == TextDirection.rtl,
      _ =>
        horizontal
            ? _resolvedTextDirection(parentNode) == TextDirection.rtl
            : _enumValue(parentNode, 'verticalDirection') == 'up',
    };
    final referenceIndex = insertionIndex < children.length
        ? insertionIndex
        : children.length - 1;
    final reference = _renderBox(_nodeKeys[children[referenceIndex].id]);
    if (reference == null) {
      return Rect.zero;
    }
    final renderedReferenceRect = _finiteRectInAncestor(reference, parentBox);
    if (renderedReferenceRect == null) {
      return Rect.zero;
    }
    final referenceRect = renderedReferenceRect.intersect(parentRect);
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

  CanvasNode? _requiredChildOwner(CanvasNode node, String childId) {
    if (isCanvasReviewedRequiredChildWrapperWidgetType(node.type) &&
        node.slot('child')?.child?.id == childId) {
      return node;
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        final owner = _requiredChildOwner(child, childId);
        if (owner != null) return owner;
      }
    }
    return null;
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
    CanvasPaletteDragSource source,
  ) {
    if (isCanvasRequiredChildWrapperWidgetType(source.widgetType)) {
      // CanvasDropTarget identifies a parent slot, so only existing non-root
      // children can be represented on the current negotiated wire. Do not
      // invent a root sentinel: the host has no field it could revalidate.
      for (final slotEntry in node.slots.entries) {
        final modelSlot = slotEntry.value;
        final wrapSlot = canvasExistingChildWrapTargetSlot(
          parentWidgetType: node.type,
          slotName: slotEntry.key,
        );
        if (wrapSlot == null ||
            modelSlot.kind != wrapSlot.modelSlotKind ||
            !wrapSlot.acceptsSource(source)) {
          continue;
        }
        for (final index in _interactiveChildIndexes(
          node,
          slotEntry.key,
          modelSlot,
        )) {
          final child = modelSlot.children[index];
          if (!canvasWrapperAcceptsExistingChild(
            wrapperWidgetType: source.widgetType,
            childWidgetType: child.type,
          )) {
            continue;
          }
          final box = _renderBox(_nodeKeys[child.id]);
          if (box == null) {
            continue;
          }
          final zone = _resolvedGlobalDropZone(
            box,
            Offset.zero & box.size,
            point,
            surfaceRect,
          );
          if (zone != null) {
            result.add(
              _DropCandidate(
                node,
                depth + 1,
                zone.width * zone.height,
                wrapSlot,
                index,
                zone,
              ),
            );
          }
        }
      }
    } else if (isCanvasFlexParentDataWidgetType(source.widgetType) &&
        (node.type == 'flutter.widgets.Row' ||
            node.type == 'flutter.widgets.Column')) {
      final modelSlot = node.slot('children');
      if (modelSlot?.kind == 'list') {
        for (var index = 0; index < modelSlot!.children.length; index++) {
          final child = modelSlot.children[index];
          if (isCanvasFlexRestrictedWidgetType(child.type)) {
            continue;
          }
          final box = _renderBox(_nodeKeys[child.id]);
          if (box == null) {
            continue;
          }
          final zone = _resolvedGlobalDropZone(
            box,
            Offset.zero & box.size,
            point,
            surfaceRect,
          );
          if (zone != null) {
            result.add(
              _DropCandidate(
                node,
                depth + 1,
                zone.width * zone.height,
                canvasFlexWrapDropSlot,
                index,
                zone,
              ),
            );
          }
        }
      }
    } else {
      for (final dropSlot in canvasDropSlotsForWidgetType(node.type)) {
        if (!dropSlot.acceptsSource(source) ||
            !_isEligibleDropSlot(node, dropSlot.slotName)) {
          continue;
        }
        final modelSlot = node.slot(dropSlot.slotName);
        if (modelSlot != null && modelSlot.kind != dropSlot.modelSlotKind) {
          continue;
        }
        final currentChildCount = modelSlot?.children.length ?? 0;
        final insertionIndex = dropSlot.insertionIndexFor(currentChildCount);
        final box = _renderBox(_nodeKeys[node.id]);
        if (insertionIndex != null && box != null) {
          if (dropSlot.zonePlacement == CanvasDropZonePlacement.existingChild) {
            continue;
          }
          final localParent = Offset.zero & box.size;
          final localZone = switch (dropSlot.zonePlacement) {
            CanvasDropZonePlacement.badgeLabel => _badgeLabelZone(localParent),
            CanvasDropZonePlacement.fullNode => localParent,
            CanvasDropZonePlacement.terminalList => _terminalZone(
              node,
              box,
              localParent,
              dropSlot.slotName,
            ),
            CanvasDropZonePlacement.existingChild => Rect.zero,
            CanvasDropZonePlacement.bottomRightCompact =>
              _bottomRightCompactZone(localParent),
            CanvasDropZonePlacement.appBarLeading => _appBarLeadingZone(
              node,
              box,
              localParent,
            ),
            CanvasDropZonePlacement.appBarTitle => _appBarTitleZone(
              node,
              box,
              localParent,
            ),
            CanvasDropZonePlacement.appBarActions => _terminalZone(
              node,
              box,
              _appBarToolbarZone(node, box, localParent),
              dropSlot.slotName,
            ),
            CanvasDropZonePlacement.appBarFlexibleSpace => localParent,
            CanvasDropZonePlacement.appBarBottom => _appBarBottomZone(
              node,
              box,
              localParent,
            ),
          };
          final zone = _resolvedGlobalDropZone(
            box,
            localZone,
            point,
            surfaceRect,
            placement: dropSlot.zonePlacement,
          );
          if (zone != null) {
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
    }
    for (final slotEntry in node.slots.entries) {
      for (final index in _interactiveChildIndexes(
        node,
        slotEntry.key,
        slotEntry.value,
      )) {
        final child = slotEntry.value.children[index];
        _collectDropCandidates(
          child,
          point,
          surfaceRect,
          depth + 1,
          result,
          source,
        );
      }
    }
  }

  Iterable<int> _interactiveChildIndexes(
    CanvasNode node,
    String slotName,
    CanvasSlot slot,
  ) sync* {
    if (!_isInteractiveSlot(node, slotName)) return;
    if (node.type == 'flutter.widgets.IndexedStack' && slotName == 'children') {
      final indexValue = node.properties['index'];
      final index = indexValue == null
          ? 0
          : indexValue.kind == 'null'
          ? null
          : indexValue.value as int;
      if (index != null && index >= 0 && index < slot.children.length) {
        yield index;
      }
      return;
    }
    for (var index = 0; index < slot.children.length; index++) {
      yield index;
    }
  }

  bool _isInteractiveSlot(CanvasNode node, String slotName) {
    if (node.type == 'flutter.material.Badge' && slotName == 'label') {
      return !node.properties.containsKey('count') &&
          node.properties['isLabelVisible']?.value != false;
    }
    if (node.type != 'flutter.widgets.Visibility') return true;
    final visible = node.properties['visible']?.value != false;
    return slotName == 'child'
        ? visible
        : slotName == 'replacement' &&
              !visible &&
              node.properties['maintainState']?.value != true;
  }

  // Model-tree selection remains host-authoritative, but invisible branches
  // must not acquire geometry or open a Designer editor from an F2 request.
  bool _isInteractiveDescendant(CanvasNode node, String widgetId) {
    if (node.id == widgetId) return true;
    for (final entry in node.slots.entries) {
      for (final index in _interactiveChildIndexes(
        node,
        entry.key,
        entry.value,
      )) {
        if (_isInteractiveDescendant(entry.value.children[index], widgetId)) {
          return true;
        }
      }
    }
    return false;
  }

  Rect _ignorePointerHandleRect(Rect body, Rect viewport) {
    const extent = _minimumZeroSizedWidgetTarget;
    Rect? fallback;
    for (final origin in [
      body.topLeft - const Offset(extent, extent),
      body.topLeft - const Offset(extent, 0),
      body.topLeft - const Offset(0, extent),
      body.topRight,
      body.bottomLeft,
      body.topRight - const Offset(0, extent),
      body.bottomRight,
      body.bottomLeft - const Offset(extent, 0),
    ]) {
      final candidate = _boundedDesignerHitRect(
        origin & const Size.square(extent),
        viewport,
        minimumExtent: extent,
      );
      fallback ??= candidate;
      if (!candidate.overlaps(body)) return candidate;
    }
    // A body filling the viewport leaves no outside space. Only this compact
    // explicit Designer handle may overlap it; the remaining body stays inert.
    return fallback!;
  }

  Rect _boundedDesignerHitRect(
    Rect rendered,
    Rect surface, {
    double minimumExtent = _minimumTerminalBand,
  }) {
    if (!rendered.isFinite || !surface.isFinite || surface.isEmpty) {
      return Rect.zero;
    }
    final visible = rendered.intersect(surface);
    final rawCenter = visible.isEmpty ? rendered.center : visible.center;
    final width = math.min(
      surface.width,
      math.max(visible.width, minimumExtent),
    );
    final height = math.min(
      surface.height,
      math.max(visible.height, minimumExtent),
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

  Rect _badgeLabelZone(Rect parent) => Rect.fromLTRB(
    parent.center.dx,
    parent.top,
    parent.right,
    parent.center.dy,
  );

  bool _isEligibleDropSlot(CanvasNode node, String slotName) {
    if (node.type == 'flutter.material.Badge' && slotName == 'label') {
      // Visibility affects mounted descendants, not structural editing. Count
      // owns its generated label and must never advertise a label destination.
      return !node.properties.containsKey('count');
    }
    return _isInteractiveSlot(node, slotName);
  }

  Rect _terminalZone(
    CanvasNode node,
    RenderBox parentBox,
    Rect parent,
    String slotName, {
    List<CanvasNode>? effectiveChildren,
  }) {
    if (parent.width <= 0 || parent.height <= 0) {
      return Rect.zero;
    }
    final children =
        effectiveChildren ??
        node.slot(slotName)?.children ??
        const <CanvasNode>[];
    if (children.isEmpty) {
      // With no siblings there is only one legal ordering result: index 0.
      // Expose the complete visible container instead of making users find a
      // synthetic terminal edge on an otherwise blank linear container.
      if (node.type == 'flutter.material.AppBar' && slotName == 'actions') {
        final width = math.min(parent.width, math.max(72.0, parent.width / 3));
        return _resolvedTextDirection(node) == TextDirection.rtl
            ? Rect.fromLTRB(
                parent.left,
                parent.top,
                parent.left + width,
                parent.bottom,
              )
            : Rect.fromLTRB(
                parent.right - width,
                parent.top,
                parent.right,
                parent.bottom,
              );
      }
      return parent;
    }
    if (node.type == 'flutter.widgets.GridView' && slotName == 'children') {
      return _gridInsertionZone(
        node,
        parentBox,
        parent,
        children,
        children.length,
        markerExtent: _minimumTerminalBand,
      );
    }
    final last = _renderBox(_nodeKeys[children.last.id]);
    if (last == null) {
      return Rect.zero;
    }
    final lastRect = _finiteRectInAncestor(last, parentBox);
    if (lastRect == null) {
      return Rect.zero;
    }
    final overflowBarVertical =
        node.type == 'flutter.widgets.OverflowBar' &&
        _isOverflowBarVertical(node, parentBox);
    if (node.type == 'flutter.widgets.Column' ||
        overflowBarVertical ||
        (node.type == 'flutter.widgets.ListBody' &&
            !_isHorizontalListBody(node)) ||
        (node.type == 'flutter.widgets.ListView' &&
            !_isHorizontalListView(node))) {
      final upward = switch (node.type) {
        'flutter.widgets.ListBody' ||
        'flutter.widgets.ListView' => _booleanValue(node, 'reverse') == true,
        'flutter.widgets.OverflowBar' =>
          _enumValue(node, 'overflowDirection') == 'up',
        _ => _enumValue(node, 'verticalDirection') == 'up',
      };
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
    final rightToLeft = switch (node.type) {
      'flutter.widgets.ListBody' => _isVisuallyReversedListBody(node),
      'flutter.widgets.ListView' => _isVisuallyReversedListView(node),
      _ => _resolvedTextDirection(node) == TextDirection.rtl,
    };
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

  Rect _gridInsertionZone(
    CanvasNode node,
    RenderBox parentBox,
    Rect parent,
    List<CanvasNode> children,
    int insertionIndex, {
    required double markerExtent,
  }) {
    if (parent.isEmpty || children.isEmpty) {
      return children.isEmpty ? parent : Rect.zero;
    }
    final beforeExisting = insertionIndex < children.length;
    final referenceIndex = beforeExisting
        ? insertionIndex
        : children.length - 1;
    final reference = _renderBox(_nodeKeys[children[referenceIndex].id]);
    if (reference == null) {
      return Rect.zero;
    }
    final renderedReferenceRect = _finiteRectInAncestor(reference, parentBox);
    if (renderedReferenceRect == null) {
      return Rect.zero;
    }
    final referenceRect = renderedReferenceRect.intersect(parent);
    if (referenceRect.isEmpty) {
      return Rect.zero;
    }

    final crossAxisCount = _integerValue(node, 'crossAxisCount') ?? 2;
    final atGroupBoundary = beforeExisting
        ? insertionIndex % crossAxisCount == 0
        : children.length % crossAxisCount == 0;
    final horizontalMain = _isHorizontalGridView(node);
    final reverse = _booleanValue(node, 'reverse') == true;
    final direction = _resolvedTextDirection(node);
    final mainForwardPositive = horizontalMain
        ? (direction == TextDirection.ltr) != reverse
        : !reverse;
    final crossForwardPositive = horizontalMain
        ? true
        : direction == TextDirection.ltr;
    final useMainAxisEdge = atGroupBoundary;
    final forwardPositive = useMainAxisEdge
        ? mainForwardPositive
        : crossForwardPositive;
    final leading = beforeExisting;
    final useMinimumEdge = leading == forwardPositive;
    final verticalEdge = useMainAxisEdge == horizontalMain;
    final edge = verticalEdge
        ? (useMinimumEdge ? referenceRect.left : referenceRect.right)
        : (useMinimumEdge ? referenceRect.top : referenceRect.bottom);
    final extent = verticalEdge
        ? markerExtent.clamp(1.0, parent.width)
        : markerExtent.clamp(1.0, parent.height);
    final half = extent / 2;

    if (verticalEdge) {
      final center = edge.clamp(parent.left + half, parent.right - half);
      return Rect.fromLTRB(
        center - half,
        referenceRect.top,
        center + half,
        referenceRect.bottom,
      );
    }
    final center = edge.clamp(parent.top + half, parent.bottom - half);
    return Rect.fromLTRB(
      referenceRect.left,
      center - half,
      referenceRect.right,
      center + half,
    );
  }

  TextDirection _resolvedTextDirection(CanvasNode node) {
    final explicit = _enumValue(node, 'textDirection');
    if (explicit == 'rtl') {
      return TextDirection.rtl;
    }
    if (explicit == 'ltr') {
      return TextDirection.ltr;
    }
    final context = _nodeKeys[node.id]?.currentContext;
    return context == null
        ? TextDirection.ltr
        : Directionality.maybeOf(context) ?? TextDirection.ltr;
  }

  static bool _isHorizontalListView(CanvasNode node) =>
      node.type == 'flutter.widgets.ListView' &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalGridView(CanvasNode node) =>
      node.type == 'flutter.widgets.GridView' &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalListBody(CanvasNode node) =>
      node.type == 'flutter.widgets.ListBody' &&
      _enumValue(node, 'mainAxis') == 'horizontal';

  bool _isOverflowBarVertical(CanvasNode node, RenderBox parentBox) {
    final children = node.slot('children')?.children ?? const <CanvasNode>[];
    if (children.isEmpty || !parentBox.size.width.isFinite) {
      return false;
    }
    var actualWidth = _numberValue(node, 'spacing') ?? 0.0;
    actualWidth *= children.length - 1;
    for (final child in children) {
      final childBox = _renderBox(_nodeKeys[child.id]);
      if (childBox == null || !childBox.size.width.isFinite) {
        return false;
      }
      actualWidth += childBox.size.width;
    }
    // This is the exact branch used by Flutter's _RenderOverflowBar:
    // children and spacing remain a row at equality, and become a column only
    // when their combined width is strictly greater than the available width.
    return actualWidth > parentBox.size.width;
  }

  bool _isVisuallyReversedListBody(CanvasNode node) {
    final reversed = _booleanValue(node, 'reverse') == true;
    if (!_isHorizontalListBody(node)) {
      return reversed;
    }
    final rightToLeft = _resolvedTextDirection(node) == TextDirection.rtl;
    return rightToLeft != reversed;
  }

  bool _isVisuallyReversedListView(CanvasNode node) {
    final reversed = _booleanValue(node, 'reverse') == true;
    if (!_isHorizontalListView(node)) {
      return reversed;
    }
    final rightToLeft = _resolvedTextDirection(node) == TextDirection.rtl;
    return rightToLeft != reversed;
  }

  Rect _appBarToolbarZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final bottom = node.slot('bottom')?.child;
    final bottomBox = bottom == null ? null : _renderBox(_nodeKeys[bottom.id]);
    final bottomRect = bottomBox == null
        ? null
        : _finiteRectInAncestor(bottomBox, parentBox);
    final renderedBottomHeight = bottomRect?.intersect(parent).height ?? 0.0;
    final fallbackBottom = node.slot('bottom')?.children.isEmpty ?? true
        ? math.min(_minimumTerminalBand, parent.height / 3)
        : 0.0;
    final bottomHeight = math.max(renderedBottomHeight, fallbackBottom);
    return Rect.fromLTRB(
      parent.left,
      parent.top,
      parent.right,
      math.max(parent.top, parent.bottom - bottomHeight),
    );
  }

  Rect _appBarBottomZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    return Rect.fromLTRB(
      parent.left,
      toolbar.bottom,
      parent.right,
      parent.bottom,
    );
  }

  Rect _appBarLeadingZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    final width = math.min(
      toolbar.width,
      math.max(
        _minimumTerminalBand,
        _numberValue(node, 'leadingWidth') ?? kToolbarHeight,
      ),
    );
    return _resolvedTextDirection(node) == TextDirection.rtl
        ? Rect.fromLTRB(
            toolbar.right - width,
            toolbar.top,
            toolbar.right,
            toolbar.bottom,
          )
        : Rect.fromLTRB(
            toolbar.left,
            toolbar.top,
            toolbar.left + width,
            toolbar.bottom,
          );
  }

  Rect _appBarTitleZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    final leading = _appBarLeadingZone(node, parentBox, parent);
    final trailingWidth = math.min(
      toolbar.width / 3,
      math.max(_minimumTerminalBand, toolbar.width / 4),
    );
    return _resolvedTextDirection(node) == TextDirection.rtl
        ? Rect.fromLTRB(
            toolbar.left + trailingWidth,
            toolbar.top,
            leading.left,
            toolbar.bottom,
          )
        : Rect.fromLTRB(
            leading.right,
            toolbar.top,
            toolbar.right - trailingWidth,
            toolbar.bottom,
          );
  }

  static RenderBox? _renderBox(GlobalKey? key) {
    final renderObject = key?.currentContext?.findRenderObject();
    return renderObject is RenderBox && renderObject.attached
        ? renderObject
        : null;
  }

  Rect? _resolvedGlobalDropZone(
    RenderBox box,
    Rect localZone,
    Offset globalPoint,
    Rect surfaceRect, {
    CanvasDropZonePlacement? placement,
  }) {
    final renderedBox = _finiteGlobalRect(box);
    if (renderedBox == null || !_hasFiniteGlobalInverse(box)) {
      return null;
    }
    if (box.size.isEmpty) {
      final bounded = _boundedDesignerHitRect(renderedBox, surfaceRect);
      final synthetic = placement == CanvasDropZonePlacement.badgeLabel
          ? _badgeLabelZone(bounded)
          : bounded;
      return !synthetic.isEmpty && synthetic.contains(globalPoint)
          ? synthetic
          : null;
    }
    if (!localZone.isFinite || localZone.isEmpty) {
      return null;
    }
    final localPoint = _finiteLocalPoint(box, globalPoint);
    if (localPoint == null || !localZone.contains(localPoint)) {
      return null;
    }
    final renderedZone = _finiteGlobalRect(box, localRect: localZone);
    if (renderedZone == null) {
      return null;
    }
    final bounded = _boundedDesignerHitRect(renderedZone, surfaceRect);
    return bounded.isEmpty ? null : bounded;
  }

  static Rect? _finiteGlobalRect(RenderBox box, {Rect? localRect}) =>
      _finiteTransformedRect(
        box.getTransformTo(null),
        localRect ?? (Offset.zero & box.size),
      );

  static Rect? _finiteRectInAncestor(RenderBox box, RenderBox ancestor) =>
      _finiteTransformedRect(
        box.getTransformTo(ancestor),
        Offset.zero & box.size,
      );

  static Rect? _finiteTransformedRect(Matrix4 transform, Rect localRect) {
    if (!localRect.isFinite) {
      return null;
    }
    final storage = transform.storage;
    double homogeneousWeight(double x, double y) =>
        storage[3] * x + storage[7] * y + storage[15];
    final weights = <double>[
      homogeneousWeight(localRect.left, localRect.top),
      homogeneousWeight(localRect.right, localRect.top),
      homogeneousWeight(localRect.left, localRect.bottom),
      homogeneousWeight(localRect.right, localRect.bottom),
    ];
    final positiveWeight = weights.first > 0;
    if (weights.any(
      (weight) =>
          !weight.isFinite || weight == 0 || (weight > 0) != positiveWeight,
    )) {
      return null;
    }
    final rect = MatrixUtils.transformRect(transform, localRect);
    // RenderTransform permits singular and projective matrices whose painted
    // bounds contain NaN or infinity, or cross a projective horizon between
    // finite corners. Such geometry has no truthful finite IDE hit region, so
    // omit its synthetic target/drop zone instead of inventing one that could
    // steal interaction from a visible ancestor.
    return rect.isFinite ? rect : null;
  }

  static bool _hasFiniteGlobalInverse(RenderBox box) {
    final inverse = Matrix4.tryInvert(box.getTransformTo(null));
    return inverse != null && !inverse.storage.any((value) => !value.isFinite);
  }

  static Offset? _finiteLocalPoint(RenderBox box, Offset point) {
    final local = box.globalToLocal(point);
    return local.dx.isFinite && local.dy.isFinite ? local : null;
  }

  static String? _enumValue(CanvasNode node, String propertyName) {
    final value = node.properties[propertyName]?.value;
    return value is CanvasEnumValue ? value.value : null;
  }

  static bool? _booleanValue(CanvasNode node, String propertyName) {
    final property = node.properties[propertyName];
    return property?.kind == 'boolean' ? property!.value as bool : null;
  }

  static int? _integerValue(CanvasNode node, String propertyName) {
    final property = node.properties[propertyName];
    return property?.kind == 'integer' ? property!.value as int : null;
  }

  static double? _numberValue(CanvasNode node, String propertyName) {
    final value = node.properties[propertyName]?.value;
    return value is num ? value.toDouble() : null;
  }
}

class _InteractionBarrierOverlay extends StatelessWidget {
  const _InteractionBarrierOverlay();

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return AbsorbPointer(
      key: const ValueKey('canvas-interaction-fence-overlay'),
      absorbing: true,
      child: ColoredBox(
        color: dark ? const Color(0x26000000) : const Color(0x14000000),
        child: Center(
          child: Semantics(
            liveRegion: true,
            label: 'Synchronizing Canvas input',
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: dark ? const Color(0xff303134) : const Color(0xfff8f9fa),
                border: Border.all(
                  color: dark
                      ? const Color(0xff5f6368)
                      : const Color(0xffbdc1c6),
                ),
                borderRadius: BorderRadius.circular(3),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x26000000),
                    blurRadius: 4,
                    offset: Offset(0, 1),
                  ),
                ],
              ),
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                child: Text(
                  'Synchronizing input…',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ),
          ),
        ),
      ),
    );
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

@immutable
class _InlineTextEditSession {
  const _InlineTextEditSession({
    required this.widgetId,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
  });

  final String widgetId;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
}

/// Flutter 3.44.8's maintained-size Visibility marks only paint dirty when
/// visible changes. If IgnorePointer does not also change, semantics can stay
/// stale (or assert on the next child update). Invalidate the actual SDK render
/// object; do not replace its layout, paint, hit, focus or semantics behavior.
class _CanvasVisibility extends StatefulWidget {
  const _CanvasVisibility({required this.visibility});

  final Visibility visibility;

  @override
  State<_CanvasVisibility> createState() => _CanvasVisibilityState();
}

class _CanvasVisibilityState extends State<_CanvasVisibility> {
  final _sdkKey = GlobalKey();

  @override
  void didUpdateWidget(_CanvasVisibility oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.visibility.visible != widget.visibility.visible &&
        oldWidget.visibility.maintainSize) {
      _sdkKey.currentContext?.findRenderObject()?.markNeedsSemanticsUpdate();
    }
  }

  @override
  Widget build(BuildContext context) =>
      KeyedSubtree(key: _sdkKey, child: widget.visibility);
}

class _CanvasNodeView extends StatelessWidget implements PreferredSizeWidget {
  const _CanvasNodeView({
    required this.node,
    required this.imageResources,
    required this.onImageError,
    required this.selectedWidgetId,
    required this.onSelected,
    required this.nodeKey,
    required this.designerFocusParent,
    required this.overlayScale,
    required this.inlineTextEditEnabled,
    required this.inlineTextEditingWidgetId,
    required this.onBeginInlineTextEdit,
    required this.onCommitInlineTextEdit,
    required this.onCancelInlineTextEdit,
    this.suppressDesignerSemantics = false,
  });

  final CanvasNode node;
  final CanvasImageResourceBundle imageResources;
  final CanvasImageErrorReporter? onImageError;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final GlobalKey Function(String id) nodeKey;
  final FocusNode designerFocusParent;
  final double overlayScale;
  final bool inlineTextEditEnabled;
  final String? inlineTextEditingWidgetId;
  final bool Function(String) onBeginInlineTextEdit;
  final bool Function(String, String, bool) onCommitInlineTextEdit;
  final VoidCallback onCancelInlineTextEdit;
  final bool suppressDesignerSemantics;

  @override
  Size get preferredSize => node.type == 'flutter.material.AppBar'
      ? AppBar(
          toolbarHeight: _number('toolbarHeight'),
          bottom: _preferredSizeSingle('bottom'),
        ).preferredSize
      : Size.zero;

  @override
  Widget build(BuildContext context) {
    final paddingGeometry = switch (node.type) {
      'flutter.widgets.Padding' => _paddingGeometry(),
      'flutter.widgets.Container' => _edgeInsetsGeometry('padding'),
      _ => null,
    };
    final marginGeometry = node.type == 'flutter.widgets.Container'
        ? _edgeInsetsGeometry('margin')
        : null;
    final editing = inlineTextEditingWidgetId == node.id;
    final child = switch (node.type) {
      'flutter.material.Scaffold' => _scaffold(context),
      'flutter.material.AppBar' => _appBar(context),
      'flutter.material.Card' => _card(context),
      'flutter.material.Badge' => _badge(context),
      'flutter.material.CircleAvatar' => _circleAvatar(context),
      'flutter.material.Divider' => Divider(
        height: _number('height'),
        thickness: _number('thickness'),
        indent: _number('indent'),
        endIndent: _number('endIndent'),
        color: _resolvedColor(context, 'color'),
        radius: switch (node.properties['radius']?.value) {
          final CanvasBorderRadiusGeometryValue radius => _borderRadius(radius),
          _ => null,
        },
      ),
      'flutter.material.VerticalDivider' => VerticalDivider(
        width: _number('width'),
        thickness: _number('thickness'),
        indent: _number('indent'),
        endIndent: _number('endIndent'),
        color: _resolvedColor(context, 'color'),
        radius: switch (node.properties['radius']?.value) {
          final CanvasBorderRadiusGeometryValue radius => _borderRadius(radius),
          _ => null,
        },
      ),
      'flutter.material.ElevatedButton' => _elevatedButton(context),
      'flutter.widgets.Column' => _column(),
      'flutter.widgets.Row' => _row(),
      'flutter.widgets.Wrap' => _wrap(),
      'flutter.widgets.ListBody' => _listBody(),
      'flutter.widgets.OverflowBar' => _overflowBar(),
      'flutter.widgets.ListView' => _listView(),
      'flutter.widgets.GridView' => _gridView(),
      'flutter.widgets.SingleChildScrollView' => _singleChildScrollView(),
      'flutter.widgets.Stack' => _stack(),
      'flutter.widgets.IndexedStack' => _indexedStack(),
      'flutter.widgets.Expanded' => _single('child')!,
      'flutter.widgets.Flexible' => _single('child')!,
      'flutter.widgets.SafeArea' => _safeArea(),
      'flutter.widgets.Directionality' => _directionality(),
      'flutter.widgets.ExcludeFocus' => ExcludeFocus(
        excluding: _boolean('excluding') ?? true,
        child: _single('child')!,
      ),
      'flutter.widgets.ExcludeFocusTraversal' => ExcludeFocusTraversal(
        excluding: _boolean('excluding') ?? true,
        child: _single('child')!,
      ),
      'flutter.widgets.TickerMode' => TickerMode(
        enabled: _boolean('enabled')!,
        forceFrames: _boolean('forceFrames') ?? false,
        child: _single('child')!,
      ),
      'flutter.widgets.DefaultTextHeightBehavior' => DefaultTextHeightBehavior(
        textHeightBehavior: _textHeightBehavior() ?? const TextHeightBehavior(),
        child: _single('child')!,
      ),
      'flutter.widgets.DefaultSelectionStyle' => _defaultSelectionStyle(
        context,
      ),
      'flutter.widgets.IconTheme' => _iconThemeWidget(context),
      'flutter.widgets.Visibility' => _CanvasVisibility(
        visibility: Visibility(
          visible: _boolean('visible') ?? true,
          maintainState: _boolean('maintainState') ?? false,
          maintainAnimation: _boolean('maintainAnimation') ?? false,
          maintainSize: _boolean('maintainSize') ?? false,
          maintainSemantics: _boolean('maintainSemantics') ?? false,
          maintainInteractivity: _boolean('maintainInteractivity') ?? false,
          maintainFocusability: _boolean('maintainFocusability') ?? false,
          replacement: _single('replacement') ?? const SizedBox.shrink(),
          child: _single('child')!,
        ),
      ),
      'flutter.widgets.Spacer' => Spacer(flex: _integer('flex') ?? 1),
      'flutter.widgets.Padding' => _padding(paddingGeometry!),
      'flutter.widgets.Align' => _align(),
      'flutter.widgets.AspectRatio' => _aspectRatio(),
      'flutter.widgets.Baseline' => _baseline(),
      'flutter.widgets.IntrinsicHeight' => _intrinsicHeight(),
      'flutter.widgets.IntrinsicWidth' => _intrinsicWidth(),
      'flutter.widgets.Offstage' => _offstage(),
      'flutter.widgets.RotatedBox' => _rotatedBox(),
      'flutter.widgets.SizedOverflowBox' => _sizedOverflowBox(),
      'flutter.widgets.Transform' =>
        _single('child') ?? const SizedBox.shrink(),
      'flutter.widgets.Center' => _center(),
      'flutter.widgets.ConstrainedBox' => _constrainedBox(),
      'flutter.widgets.UnconstrainedBox' => _unconstrainedBox(),
      'flutter.widgets.LimitedBox' => _limitedBox(),
      'flutter.widgets.OverflowBox' => _overflowBox(),
      'flutter.widgets.Placeholder' => _placeholder(context),
      'flutter.widgets.ClipOval' => _clipOval(),
      'flutter.widgets.ClipRRect' => _clipRRect(context),
      'flutter.widgets.ClipRSuperellipse' => _clipRSuperellipse(context),
      'flutter.widgets.ClipPath' => _clipPath(),
      'flutter.widgets.ClipRect' => _clipRect(),
      'flutter.widgets.ColoredBox' => _coloredBox(context),
      'flutter.widgets.PhysicalModel' => _physicalModel(context),
      'flutter.widgets.PhysicalShape' => _physicalShape(context),
      'flutter.widgets.Container' => _container(context),
      'flutter.widgets.DecoratedBox' => _decoratedBox(context),
      'flutter.widgets.ExcludeSemantics' => _excludeSemantics(),
      'flutter.widgets.IgnorePointer' => _ignorePointer(),
      'flutter.widgets.AbsorbPointer' => _absorbPointer(),
      'flutter.widgets.BlockSemantics' => _blockSemantics(),
      'flutter.widgets.MergeSemantics' => MergeSemantics(
        child: _single('child'),
      ),
      'flutter.widgets.IndexedSemantics' => IndexedSemantics(
        index: _integer('index')!,
        child: _single('child'),
      ),
      'flutter.widgets.RepaintBoundary' => RepaintBoundary(
        child: _single('child'),
      ),
      'flutter.widgets.FittedBox' => _fittedBox(),
      'flutter.widgets.FractionallySizedBox' => _fractionallySizedBox(),
      'flutter.widgets.Opacity' => _opacity(),
      'flutter.widgets.SizedBox' => _sizedBox(),
      'flutter.widgets.Icon' => _icon(context),
      'flutter.widgets.Image' => _image(context),
      'flutter.widgets.ImageIcon' => _imageIcon(context),
      'flutter.material.TextField' => _textField(context),
      'flutter.widgets.Text' =>
        editing ? _inlineTextEditor(context) : _text(context),
      _ => const SizedBox.shrink(),
    };
    if (node.type == canvasSpacerWidgetType) {
      // Every wrapper here is a component widget. A RenderObjectWidget between
      // Spacer's internal Expanded and the enclosing Flex would invalidate its
      // ParentData path; selection and outlines live in the surface overlay.
      return KeyedSubtree(
        key: ValueKey('canvas-widget-${node.id}'),
        child: KeyedSubtree(key: nodeKey(node.id), child: child),
      );
    }
    final selected = selectedWidgetId == node.id;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final resolvedPadding = paddingGeometry?.resolve(
      Directionality.of(context),
    );
    final resolvedMargin = marginGeometry?.resolve(Directionality.of(context));
    final guidedChild = node.type == 'flutter.widgets.Container'
        ? resolvedPadding == null && resolvedMargin == null
              ? child
              : CustomPaint(
                  key: ValueKey('canvas-container-insets-guides-${node.id}'),
                  foregroundPainter: _CanvasContainerInsetsGuidesPainter(
                    padding: resolvedPadding ?? EdgeInsets.zero,
                    margin: resolvedMargin ?? EdgeInsets.zero,
                    visualScale: overlayScale,
                    paddingColor: dark
                        ? const Color(0xffffb74d)
                        : const Color(0xffd97706),
                    marginColor: dark
                        ? const Color(0xff80cbc4)
                        : const Color(0xff00897b),
                  ),
                  child: child,
                )
        : resolvedPadding == null
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
    final suppressNodeDesignerSemantics =
        suppressDesignerSemantics ||
        node.type == 'flutter.widgets.IndexedSemantics';
    final instrumented = Semantics(
      // Synthetic Designer labels, selected states and tap actions must not
      // contaminate merged nodes or consume an IndexedSemantics annotation.
      // Keep actionable preview diagnostics and pointer/keyboard editing.
      label: suppressNodeDesignerSemantics
          ? _imageStatusSemantics()
          : '${_displayType(node.type)} ${node.id}${_imageStatusSemantics()}',
      selected: suppressNodeDesignerSemantics ? null : selected,
      child: MouseRegion(
        cursor: editing ? SystemMouseCursors.text : SystemMouseCursors.click,
        opaque: !_ignoresPointersForNode(node),
        hitTestBehavior: _ignoresPointersForNode(node)
            ? HitTestBehavior.deferToChild
            : null,
        child: KeyedSubtree(
          key: ValueKey('canvas-widget-${node.id}'),
          child: GestureDetector(
            key: nodeKey(node.id),
            excludeFromSemantics: suppressNodeDesignerSemantics,
            behavior: HitTestBehavior.translucent,
            onTap: editing || _ignoresPointersForNode(node)
                ? null
                : () => onSelected(node.id),
            onDoubleTap:
                !editing &&
                    inlineTextEditEnabled &&
                    selected &&
                    node.type == 'flutter.widgets.Text'
                ? () => Future<void>.microtask(
                    () => onBeginInlineTextEdit(node.id),
                  )
                : null,
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
    return switch (node.type) {
      'flutter.widgets.Transform' => _transform(instrumented),
      'flutter.widgets.Expanded' => Expanded(
        flex: _integer('flex') ?? 1,
        child: instrumented,
      ),
      'flutter.widgets.Flexible' => Flexible(
        flex: _integer('flex') ?? 1,
        fit: _enum('fit') == 'tight' ? FlexFit.tight : FlexFit.loose,
        child: instrumented,
      ),
      _ => instrumented,
    };
  }

  Widget _scaffold(BuildContext context) {
    return Scaffold(
      appBar: _preferredSizeSingle('appBar'),
      body: _single('body') ?? const SizedBox.expand(),
      floatingActionButton: _single('floatingActionButton'),
      floatingActionButtonLocation: _scaffoldFloatingActionButtonLocation(),
      floatingActionButtonAnimator: _scaffoldFloatingActionButtonAnimator(),
      persistentFooterAlignment:
          _scaffoldPersistentFooterAlignment() ??
          AlignmentDirectional.centerEnd,
      onDrawerChanged: _callbackPresent('onDrawerChanged') ? (_) {} : null,
      onEndDrawerChanged: _callbackPresent('onEndDrawerChanged')
          ? (_) {}
          : null,
      backgroundColor: _resolvedColor(context, 'backgroundColor'),
      resizeToAvoidBottomInset: _boolean('resizeToAvoidBottomInset'),
      primary: _boolean('primary') ?? true,
      drawerDragStartBehavior: _scaffoldDrawerDragStartBehavior(),
      extendBody: _boolean('extendBody') ?? false,
      drawerBarrierDismissible: _boolean('drawerBarrierDismissible') ?? true,
      extendBodyBehindAppBar: _boolean('extendBodyBehindAppBar') ?? false,
      drawerScrimColor: _resolvedColor(context, 'drawerScrimColor'),
      drawerEdgeDragWidth: _number('drawerEdgeDragWidth'),
      drawerEnableOpenDragGesture:
          _boolean('drawerEnableOpenDragGesture') ?? true,
      endDrawerEnableOpenDragGesture:
          _boolean('endDrawerEnableOpenDragGesture') ?? true,
      restorationId: _string('restorationId'),
    );
  }

  FloatingActionButtonLocation? _scaffoldFloatingActionButtonLocation() =>
      switch (_string('floatingActionButtonLocation')) {
        'startTop' => FloatingActionButtonLocation.startTop,
        'miniStartTop' => FloatingActionButtonLocation.miniStartTop,
        'centerTop' => FloatingActionButtonLocation.centerTop,
        'miniCenterTop' => FloatingActionButtonLocation.miniCenterTop,
        'endTop' => FloatingActionButtonLocation.endTop,
        'miniEndTop' => FloatingActionButtonLocation.miniEndTop,
        'startFloat' => FloatingActionButtonLocation.startFloat,
        'miniStartFloat' => FloatingActionButtonLocation.miniStartFloat,
        'centerFloat' => FloatingActionButtonLocation.centerFloat,
        'miniCenterFloat' => FloatingActionButtonLocation.miniCenterFloat,
        'endFloat' => FloatingActionButtonLocation.endFloat,
        'miniEndFloat' => FloatingActionButtonLocation.miniEndFloat,
        'startDocked' => FloatingActionButtonLocation.startDocked,
        'miniStartDocked' => FloatingActionButtonLocation.miniStartDocked,
        'centerDocked' => FloatingActionButtonLocation.centerDocked,
        'miniCenterDocked' => FloatingActionButtonLocation.miniCenterDocked,
        'endDocked' => FloatingActionButtonLocation.endDocked,
        'miniEndDocked' => FloatingActionButtonLocation.miniEndDocked,
        'endContained' => FloatingActionButtonLocation.endContained,
        _ => null,
      };

  FloatingActionButtonAnimator? _scaffoldFloatingActionButtonAnimator() =>
      switch (_string('floatingActionButtonAnimator')) {
        'scaling' => FloatingActionButtonAnimator.scaling,
        'noAnimation' => FloatingActionButtonAnimator.noAnimation,
        _ => null,
      };

  AlignmentDirectional? _scaffoldPersistentFooterAlignment() =>
      switch (_string('persistentFooterAlignment')) {
        'topStart' => AlignmentDirectional.topStart,
        'topCenter' => AlignmentDirectional.topCenter,
        'topEnd' => AlignmentDirectional.topEnd,
        'centerStart' => AlignmentDirectional.centerStart,
        'center' => AlignmentDirectional.center,
        'centerEnd' => AlignmentDirectional.centerEnd,
        'bottomStart' => AlignmentDirectional.bottomStart,
        'bottomCenter' => AlignmentDirectional.bottomCenter,
        'bottomEnd' => AlignmentDirectional.bottomEnd,
        _ => null,
      };

  DragStartBehavior _scaffoldDrawerDragStartBehavior() =>
      switch (_enum('drawerDragStartBehavior')) {
        'down' => DragStartBehavior.down,
        _ => DragStartBehavior.start,
      };

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

  Widget _appBar(BuildContext context) => AppBar(
    leading: _single('leading'),
    automaticallyImplyLeading: _boolean('automaticallyImplyLeading') ?? true,
    title: _single('title'),
    actions: node.slots.containsKey('actions') ? _children('actions') : null,
    automaticallyImplyActions: _boolean('automaticallyImplyActions') ?? true,
    flexibleSpace: _single('flexibleSpace'),
    bottom: _preferredSizeSingle('bottom'),
    elevation: _number('elevation'),
    scrolledUnderElevation: _number('scrolledUnderElevation'),
    notificationPredicate: _notificationPredicate(),
    shadowColor: _resolvedColor(context, 'shadowColor'),
    surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
    shape: _appBarShape(context),
    backgroundColor: _resolvedColor(context, 'backgroundColor'),
    foregroundColor: _resolvedColor(context, 'foregroundColor'),
    iconTheme: _iconTheme(context, 'iconTheme'),
    actionsIconTheme: _iconTheme(context, 'actionsIconTheme'),
    primary: _boolean('primary') ?? true,
    centerTitle: _boolean('centerTitle'),
    excludeHeaderSemantics: _boolean('excludeHeaderSemantics') ?? false,
    titleSpacing: _number('titleSpacing'),
    toolbarOpacity: _number('toolbarOpacity') ?? 1.0,
    bottomOpacity: _number('bottomOpacity') ?? 1.0,
    toolbarHeight: _number('toolbarHeight'),
    leadingWidth: _number('leadingWidth'),
    toolbarTextStyle: _textStyle(context, 'toolbarTextStyle'),
    titleTextStyle: _textStyle(context, 'titleTextStyle'),
    systemOverlayStyle: _systemOverlayStyle(context),
    forceMaterialTransparency: _boolean('forceMaterialTransparency') ?? false,
    useDefaultSemanticsOrder: _boolean('useDefaultSemanticsOrder') ?? true,
    clipBehavior: _clipBehavior(),
    actionsPadding: _edgeInsetsGeometry('actionsPadding'),
    animateColor: _boolean('animateColor') ?? false,
  );

  Widget _elevatedButton(BuildContext context) {
    final enabled = _boolean('enabled') ?? true;
    final onPressedPresent = _callbackPresent('onPressed');
    final onLongPressPresent = _callbackPresent('onLongPress');
    final onHoverPresent = _callbackPresent('onHover');
    final onFocusChangePresent = _callbackPresent('onFocusChange');
    return ElevatedButton(
      onPressed: enabled && (onPressedPresent || !onLongPressPresent)
          ? () {}
          : null,
      onLongPress: enabled && onLongPressPresent ? () {} : null,
      onHover: onHoverPresent ? (_) {} : null,
      onFocusChange: onFocusChangePresent ? (_) {} : null,
      autofocus: _boolean('autofocus') ?? false,
      clipBehavior: _clipBehavior(),
      style: _elevatedButtonStyle(context),
      child: _single('child'),
    );
  }

  ButtonStyle? _elevatedButtonStyle(BuildContext context) {
    if (!node.properties.keys.any((name) => name.startsWith('style'))) {
      return null;
    }
    final themeStyle = ElevatedButtonTheme.of(context).style;
    final defaultStyle = ElevatedButton(
      onPressed: () {},
      child: null,
    ).defaultStyleOf(context);
    final constraintSizes = _buttonConstraintSizeStateProperties(
      themeMinimum: themeStyle?.minimumSize,
      defaultMinimum: defaultStyle.minimumSize,
      themeMaximum: themeStyle?.maximumSize,
      defaultMaximum: defaultStyle.maximumSize,
    );
    return ButtonStyle(
      textStyle: _buttonTextStyleStateProperty(
        context,
        themeStyle?.textStyle,
        defaultStyle.textStyle,
      ),
      backgroundColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}BackgroundColor'),
      ),
      foregroundColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}ForegroundColor'),
      ),
      overlayColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}OverlayColor'),
      ),
      shadowColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}ShadowColor'),
      ),
      surfaceTintColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}SurfaceTintColor'),
      ),
      elevation: _buttonStateProperty<double>(
        (prefix) => _number('${prefix}Elevation'),
      ),
      padding: _buttonStateProperty<EdgeInsetsGeometry>(
        (prefix) => _edgeInsetsGeometry('${prefix}Padding'),
      ),
      minimumSize: constraintSizes.minimum,
      fixedSize: _buttonSizeStateProperty(
        'Fixed',
        missingDimension: double.infinity,
        themeValue: themeStyle?.fixedSize,
        defaultValue: defaultStyle.fixedSize,
      ),
      maximumSize: constraintSizes.maximum,
      iconColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}IconColor'),
      ),
      iconSize: _buttonStateProperty<double>(
        (prefix) => _number('${prefix}IconSize'),
      ),
      side: _buttonBorderSideStateProperty(
        context,
        themeStyle?.side,
        defaultStyle.side,
        themeStyle?.shape,
        defaultStyle.shape,
      ),
      shape: _buttonShapeStateProperty(
        context,
        themeStyle?.shape,
        defaultStyle.shape,
      ),
      mouseCursor: _buttonStateProperty<MouseCursor>(
        (prefix) => _mouseCursor('${prefix}MouseCursor'),
      ),
      visualDensity: _buttonVisualDensity(
        themeStyle?.visualDensity,
        defaultStyle.visualDensity,
      ),
      tapTargetSize: _buttonTapTargetSize(),
      animationDuration: _buttonAnimationDuration(),
      enableFeedback: _boolean('styleEnableFeedback'),
      alignment: _buttonAlignment(),
      splashFactory: _buttonSplashFactory(),
    );
  }

  WidgetStateProperty<T?>? _buttonStateProperty<T>(
    T? Function(String prefix) resolve,
  ) {
    final disabled = resolve('styleDisabled');
    final pressed = resolve('stylePressed');
    final hovered = resolve('styleHovered');
    final focused = resolve('styleFocused');
    final fallback = resolve('style');
    if (disabled == null &&
        pressed == null &&
        hovered == null &&
        focused == null &&
        fallback == null) {
      return null;
    }
    return WidgetStateProperty<T?>.fromMap(<WidgetStatesConstraint, T?>{
      WidgetState.disabled: disabled,
      WidgetState.pressed: ?pressed,
      WidgetState.hovered: ?hovered,
      WidgetState.focused: ?focused,
      WidgetState.any: ?fallback,
    });
  }

  WidgetStateProperty<TextStyle?>? _buttonTextStyleStateProperty(
    BuildContext context,
    WidgetStateProperty<TextStyle?>? themeValue,
    WidgetStateProperty<TextStyle?>? defaultValue,
  ) {
    if (!_buttonHasAnyStateGroup(
      (prefix) => _hasPropertyPrefix('${prefix}Text'),
    )) {
      return null;
    }
    return WidgetStateProperty.resolveWith<TextStyle?>((states) {
      final layers = _buttonActiveStateLayers(
        states,
        (candidate) => _hasPropertyPrefix('${candidate}Text'),
      );
      if (layers.isEmpty) {
        return null;
      }
      TextStyle? effective =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      for (final prefix in layers) {
        final local = _buttonLocalTextStyle(context, prefix, effective);
        effective = _mergeButtonTextStyle(
          effective,
          local,
          inheritExplicit:
              _themeTextStyle(context, '${prefix}TextTheme') != null ||
              node.properties.containsKey('${prefix}TextInherit'),
        );
        final package = _string('${prefix}TextPackage');
        if (package != null && effective != null) {
          effective = _buttonApplyTextPackage(effective, package);
        }
      }
      return effective;
    });
  }

  TextStyle? _buttonLocalTextStyle(
    BuildContext context,
    String prefix,
    TextStyle? inherited,
  ) {
    final textPrefix = '${prefix}Text';
    if (!_hasPropertyPrefix(textPrefix)) {
      return null;
    }
    final themeBase = _buttonNormalizedThemeTextStyle(
      _themeTextStyle(context, '${textPrefix}Theme'),
    );
    final hasOverrides = node.properties.keys.any(
      (name) => name.startsWith(textPrefix) && name != '${textPrefix}Theme',
    );
    if (!hasOverrides) {
      return themeBase;
    }
    final backgroundColor = _resolvedColor(
      context,
      '${textPrefix}BackgroundColor',
    );
    final configuredBackground = _paint(context, '${textPrefix}Background');
    final background =
        configuredBackground ??
        (backgroundColor == null ? null : (Paint()..color = backgroundColor));
    final explicitInherit = _boolean('${textPrefix}Inherit');
    final structuredBase = _mergeButtonTextStyle(
      explicitInherit == false ? null : inherited,
      themeBase,
      inheritExplicit: themeBase != null,
    );
    final familyName = '${textPrefix}FontFamily';
    final fallbackName = '${textPrefix}FontFamilyFallback';
    final fontFamily = node.properties.containsKey(familyName)
        ? _string(familyName)
        : null;
    final fontFamilyFallback = node.properties.containsKey(fallbackName)
        ? _newlineList(fallbackName)
        : null;
    final localBase = themeBase ?? const TextStyle();
    return localBase.copyWith(
      inherit: explicitInherit,
      fontSize: _number('${textPrefix}FontSize'),
      fontWeight: _fontWeight('${textPrefix}FontWeight'),
      fontStyle: _fontStyle('${textPrefix}FontStyle'),
      letterSpacing: _number('${textPrefix}LetterSpacing'),
      wordSpacing: _number('${textPrefix}WordSpacing'),
      textBaseline: _textBaseline('${textPrefix}TextBaseline'),
      height: _number('${textPrefix}Height'),
      leadingDistribution: _textLeadingDistribution(
        '${textPrefix}LeadingDistribution',
      ),
      locale: _buttonTextLocale(textPrefix, structuredBase?.locale),
      background: background,
      shadows: _shadows(context, '${textPrefix}Shadows'),
      fontFeatures: _fontFeatures('${textPrefix}FontFeatures'),
      fontVariations: _fontVariations('${textPrefix}FontVariations'),
      decoration: _buttonTextDecoration(textPrefix, structuredBase?.decoration),
      decorationColor: _resolvedColor(context, '${textPrefix}DecorationColor'),
      decorationStyle: _textDecorationStyle('${textPrefix}DecorationStyle'),
      decorationThickness: _number('${textPrefix}DecorationThickness'),
      fontFamily: fontFamily,
      fontFamilyFallback: fontFamilyFallback,
      overflow: _textOverflow('${textPrefix}Overflow'),
    );
  }

  TextStyle _buttonApplyTextPackage(TextStyle value, String package) {
    final family = value.fontFamily;
    if (family != null && !_buttonSyntheticNullFontFamily(family)) {
      return value.copyWith(package: package);
    }
    final fallback = value.fontFamilyFallback;
    if (fallback == null || fallback.isEmpty) {
      throw StateError(
        'Validated Canvas button TextPackage has no effective font reference.',
      );
    }
    final packageFree = _buttonTextStyleWithoutPrivatePackage(
      value,
      includeFontReferences: false,
    );
    return packageFree.copyWith(
      fontFamilyFallback: fallback
          .map(_buttonRawFontFamily)
          .map((family) => 'packages/$package/$family')
          .toList(growable: false),
    );
  }

  String _buttonRawFontFamily(String value) {
    const prefix = 'packages/';
    if (!value.startsWith(prefix)) {
      return value;
    }
    final separator = value.indexOf('/', prefix.length);
    return separator < 0 ? value : value.substring(separator + 1);
  }

  bool _buttonSyntheticNullFontFamily(String value) =>
      value.startsWith('packages/') && value.endsWith('/null');

  TextStyle? _buttonNormalizedThemeTextStyle(TextStyle? value) {
    final backgroundColor = value?.backgroundColor;
    if (value == null || backgroundColor == null || value.background != null) {
      return value;
    }
    return value.copyWith(background: Paint()..color = backgroundColor);
  }

  TextStyle _buttonTextStyleWithoutPrivatePackage(
    TextStyle value, {
    bool includeFontReferences = true,
  }) => TextStyle(
    inherit: value.inherit,
    color: value.color,
    backgroundColor: value.backgroundColor,
    fontSize: value.fontSize,
    fontWeight: value.fontWeight,
    fontStyle: value.fontStyle,
    letterSpacing: value.letterSpacing,
    wordSpacing: value.wordSpacing,
    textBaseline: value.textBaseline,
    height: value.height,
    leadingDistribution: value.leadingDistribution,
    locale: value.locale,
    foreground: value.foreground,
    background: value.background,
    shadows: value.shadows,
    fontFeatures: value.fontFeatures,
    fontVariations: value.fontVariations,
    decoration: value.decoration,
    decorationColor: value.decorationColor,
    decorationStyle: value.decorationStyle,
    decorationThickness: value.decorationThickness,
    debugLabel: value.debugLabel,
    fontFamily: includeFontReferences ? value.fontFamily : null,
    fontFamilyFallback: includeFontReferences ? value.fontFamilyFallback : null,
    overflow: value.overflow,
  );

  TextStyle? _mergeButtonTextStyle(
    TextStyle? inherited,
    TextStyle? local, {
    required bool inheritExplicit,
  }) {
    if (local == null) {
      return inherited;
    }
    if (inherited == null || (inheritExplicit && !local.inherit)) {
      return local;
    }
    return inherited
        .merge(local)
        .copyWith(inherit: inheritExplicit ? local.inherit : inherited.inherit);
  }

  Locale? _buttonTextLocale(String textPrefix, Locale? inherited) {
    final languageName = '${textPrefix}LocaleLanguageCode';
    final scriptName = '${textPrefix}LocaleScriptCode';
    final countryName = '${textPrefix}LocaleCountryCode';
    if (!node.properties.containsKey(languageName) &&
        !node.properties.containsKey(scriptName) &&
        !node.properties.containsKey(countryName)) {
      return null;
    }
    return Locale.fromSubtags(
      languageCode: _string(languageName) ?? inherited?.languageCode ?? 'und',
      scriptCode: _string(scriptName) ?? inherited?.scriptCode,
      countryCode: _string(countryName) ?? inherited?.countryCode,
    );
  }

  TextDecoration? _buttonTextDecoration(
    String textPrefix,
    TextDecoration? inherited,
  ) {
    final underlineName = '${textPrefix}DecorationUnderline';
    final overlineName = '${textPrefix}DecorationOverline';
    final lineThroughName = '${textPrefix}DecorationLineThrough';
    if (!node.properties.containsKey(underlineName) &&
        !node.properties.containsKey(overlineName) &&
        !node.properties.containsKey(lineThroughName)) {
      return null;
    }
    final decorations = <TextDecoration>[
      if (_boolean(underlineName) ??
          (inherited?.contains(TextDecoration.underline) ?? false))
        TextDecoration.underline,
      if (_boolean(overlineName) ??
          (inherited?.contains(TextDecoration.overline) ?? false))
        TextDecoration.overline,
      if (_boolean(lineThroughName) ??
          (inherited?.contains(TextDecoration.lineThrough) ?? false))
        TextDecoration.lineThrough,
    ];
    return switch (decorations.length) {
      0 => TextDecoration.none,
      1 => decorations.single,
      _ => TextDecoration.combine(decorations),
    };
  }

  WidgetStateProperty<Size?>? _buttonSizeStateProperty(
    String role, {
    required double missingDimension,
    required WidgetStateProperty<Size?>? themeValue,
    required WidgetStateProperty<Size?>? defaultValue,
  }) {
    bool hasGroup(String prefix) =>
        node.properties.containsKey('$prefix${role}Width') ||
        node.properties.containsKey('$prefix${role}Height');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<Size?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inherited =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      var width = inherited?.width ?? missingDimension;
      var height = inherited?.height ?? missingDimension;
      for (final prefix in layers) {
        width = _number('$prefix${role}Width') ?? width;
        height = _number('$prefix${role}Height') ?? height;
      }
      return Size(width, height);
    });
  }

  ({WidgetStateProperty<Size?>? minimum, WidgetStateProperty<Size?>? maximum})
  _buttonConstraintSizeStateProperties({
    required WidgetStateProperty<Size?>? themeMinimum,
    required WidgetStateProperty<Size?>? defaultMinimum,
    required WidgetStateProperty<Size?>? themeMaximum,
    required WidgetStateProperty<Size?>? defaultMaximum,
  }) {
    bool hasGroup(String prefix) =>
        node.properties.containsKey('${prefix}MinimumWidth') ||
        node.properties.containsKey('${prefix}MinimumHeight') ||
        node.properties.containsKey('${prefix}MaximumWidth') ||
        node.properties.containsKey('${prefix}MaximumHeight');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return (minimum: null, maximum: null);
    }

    ({Size minimum, Size maximum})? resolve(Set<WidgetState> states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inheritedMinimum =
          themeMinimum?.resolve(states) ?? defaultMinimum?.resolve(states);
      final inheritedMaximum =
          themeMaximum?.resolve(states) ?? defaultMaximum?.resolve(states);
      var minimumWidth = inheritedMinimum?.width ?? 0;
      var minimumHeight = inheritedMinimum?.height ?? 0;
      var maximumWidth = inheritedMaximum?.width ?? double.infinity;
      var maximumHeight = inheritedMaximum?.height ?? double.infinity;
      for (final prefix in layers) {
        minimumWidth = _number('${prefix}MinimumWidth') ?? minimumWidth;
        minimumHeight = _number('${prefix}MinimumHeight') ?? minimumHeight;
        maximumWidth = _number('${prefix}MaximumWidth') ?? maximumWidth;
        maximumHeight = _number('${prefix}MaximumHeight') ?? maximumHeight;
      }
      maximumWidth = math.max(maximumWidth, minimumWidth);
      maximumHeight = math.max(maximumHeight, minimumHeight);
      return (
        minimum: Size(minimumWidth, minimumHeight),
        maximum: Size(maximumWidth, maximumHeight),
      );
    }

    return (
      minimum: WidgetStateProperty.resolveWith<Size?>(
        (states) => resolve(states)?.minimum,
      ),
      maximum: WidgetStateProperty.resolveWith<Size?>(
        (states) => resolve(states)?.maximum,
      ),
    );
  }

  WidgetStateProperty<BorderSide?>? _buttonBorderSideStateProperty(
    BuildContext context,
    WidgetStateProperty<BorderSide?>? themeValue,
    WidgetStateProperty<BorderSide?>? defaultValue,
    WidgetStateProperty<OutlinedBorder?>? themeShape,
    WidgetStateProperty<OutlinedBorder?>? defaultShape,
  ) {
    bool hasGroup(String prefix) => _hasPropertyPrefix('${prefix}Side');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<BorderSide?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inheritedShape =
          themeShape?.resolve(states) ?? defaultShape?.resolve(states);
      final inherited =
          themeValue?.resolve(states) ??
          defaultValue?.resolve(states) ??
          inheritedShape?.side ??
          BorderSide.none;
      var color = inherited.color;
      var width = inherited.width;
      var style = inherited.style;
      var strokeAlign = inherited.strokeAlign;
      for (final prefix in layers) {
        final sidePrefix = '${prefix}Side';
        color = _resolvedColor(context, '${sidePrefix}Color') ?? color;
        width = _number('${sidePrefix}Width') ?? width;
        final localStyle = _enumOrString('${sidePrefix}Style');
        if (localStyle != null) {
          style = localStyle == 'none' ? BorderStyle.none : BorderStyle.solid;
        }
        strokeAlign = _number('${sidePrefix}StrokeAlign') ?? strokeAlign;
      }
      return BorderSide(
        color: color,
        width: width,
        style: style,
        strokeAlign: strokeAlign,
      );
    });
  }

  WidgetStateProperty<OutlinedBorder?>? _buttonShapeStateProperty(
    BuildContext context,
    WidgetStateProperty<OutlinedBorder?>? themeValue,
    WidgetStateProperty<OutlinedBorder?>? defaultValue,
  ) {
    bool hasGroup(String prefix) => _hasPropertyPrefix('${prefix}Shape');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<OutlinedBorder?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inherited =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      final direction = Directionality.of(context);
      final inheritedRadius = _buttonShapeRadius(inherited, direction);
      var kind = _buttonShapeKind(inherited);
      var topLeft = inheritedRadius?.topLeft.x ?? 0;
      var topRight = inheritedRadius?.topRight.x ?? 0;
      var bottomRight = inheritedRadius?.bottomRight.x ?? 0;
      var bottomLeft = inheritedRadius?.bottomLeft.x ?? 0;
      var eccentricity = inherited is CircleBorder
          ? inherited.eccentricity
          : 0.0;
      for (final prefix in layers) {
        kind = _enumOrString('${prefix}ShapeKind') ?? kind;
        topLeft = _number('${prefix}ShapeRadiusTopLeft') ?? topLeft;
        topRight = _number('${prefix}ShapeRadiusTopRight') ?? topRight;
        bottomRight = _number('${prefix}ShapeRadiusBottomRight') ?? bottomRight;
        bottomLeft = _number('${prefix}ShapeRadiusBottomLeft') ?? bottomLeft;
        eccentricity =
            _number('${prefix}ShapeCircleEccentricity') ?? eccentricity;
      }
      if (kind == null) {
        return null;
      }
      final radiusValue = BorderRadius.only(
        topLeft: Radius.circular(topLeft),
        topRight: Radius.circular(topRight),
        bottomRight: Radius.circular(bottomRight),
        bottomLeft: Radius.circular(bottomLeft),
      );
      final side = inherited?.side ?? BorderSide.none;
      return switch (kind) {
        'roundedRectangle' => RoundedRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'roundedSuperellipse' => RoundedSuperellipseBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'stadium' => StadiumBorder(side: side),
        'circle' => CircleBorder(side: side, eccentricity: eccentricity),
        'beveledRectangle' => BeveledRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'continuousRectangle' => ContinuousRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        _ => throw StateError('Unreviewed Canvas button shape kind: $kind'),
      };
    });
  }

  BorderRadius? _buttonShapeRadius(
    OutlinedBorder? shape,
    TextDirection direction,
  ) => switch (shape) {
    RoundedRectangleBorder value => value.borderRadius.resolve(direction),
    RoundedSuperellipseBorder value => value.borderRadius.resolve(direction),
    BeveledRectangleBorder value => value.borderRadius.resolve(direction),
    ContinuousRectangleBorder value => value.borderRadius.resolve(direction),
    _ => null,
  };

  String? _buttonShapeKind(OutlinedBorder? shape) => switch (shape) {
    RoundedRectangleBorder() => 'roundedRectangle',
    RoundedSuperellipseBorder() => 'roundedSuperellipse',
    StadiumBorder() => 'stadium',
    CircleBorder() => 'circle',
    BeveledRectangleBorder() => 'beveledRectangle',
    ContinuousRectangleBorder() => 'continuousRectangle',
    _ => null,
  };

  bool _buttonHasAnyStateGroup(bool Function(String prefix) hasGroup) =>
      const <String>[
        'styleDisabled',
        'stylePressed',
        'styleHovered',
        'styleFocused',
        'style',
      ].any(hasGroup);

  List<String> _buttonActiveStateLayers(
    Set<WidgetState> states,
    bool Function(String prefix) hasGroup,
  ) {
    if (states.contains(WidgetState.disabled)) {
      return hasGroup('styleDisabled')
          ? const <String>['styleDisabled']
          : const <String>[];
    }
    return <String>[
      if (hasGroup('style')) 'style',
      if (states.contains(WidgetState.focused) && hasGroup('styleFocused'))
        'styleFocused',
      if (states.contains(WidgetState.hovered) && hasGroup('styleHovered'))
        'styleHovered',
      if (states.contains(WidgetState.pressed) && hasGroup('stylePressed'))
        'stylePressed',
    ];
  }

  VisualDensity? _buttonVisualDensity(
    VisualDensity? themeValue,
    VisualDensity? defaultValue,
  ) {
    final horizontal = _number('styleVisualDensityHorizontal');
    final vertical = _number('styleVisualDensityVertical');
    if (horizontal == null && vertical == null) {
      return null;
    }
    final inherited = themeValue ?? defaultValue;
    return VisualDensity(
      horizontal: horizontal ?? inherited?.horizontal ?? 0,
      vertical: vertical ?? inherited?.vertical ?? 0,
    );
  }

  MaterialTapTargetSize? _buttonTapTargetSize() =>
      switch (_enumOrString('styleTapTargetSize')) {
        'padded' => MaterialTapTargetSize.padded,
        'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
        _ => null,
      };

  Duration? _buttonAnimationDuration() {
    final milliseconds = _number('styleAnimationDurationMs');
    return milliseconds == null
        ? null
        : Duration(microseconds: (milliseconds * 1000).round());
  }

  AlignmentGeometry? _buttonAlignment() {
    final kind = _enumOrString('styleAlignmentKind');
    final x = _number('styleAlignmentX');
    final y = _number('styleAlignmentY');
    if (kind == null && x == null && y == null) {
      return null;
    }
    return kind == 'directional'
        ? AlignmentDirectional(x ?? 0, y ?? 0)
        : Alignment(x ?? 0, y ?? 0);
  }

  InteractiveInkFeatureFactory? _buttonSplashFactory() =>
      switch (_enumOrString('styleSplashFactory')) {
        'inkRipple' => InkRipple.splashFactory,
        'inkSplash' => InkSplash.splashFactory,
        'inkSparkle' => InkSparkle.splashFactory,
        'none' || 'noSplash' => NoSplash.splashFactory,
        _ => null,
      };

  static bool _isEmptyLinearContainer(CanvasNode node) =>
      (node.type == 'flutter.widgets.Row' ||
          node.type == 'flutter.widgets.Column' ||
          node.type == 'flutter.widgets.Wrap' ||
          node.type == 'flutter.widgets.ListBody' ||
          node.type == 'flutter.widgets.OverflowBar' ||
          node.type == 'flutter.widgets.ListView' ||
          node.type == 'flutter.widgets.GridView') &&
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

  Widget _wrap() => Wrap(
    direction: _enum('direction') == 'vertical'
        ? Axis.vertical
        : Axis.horizontal,
    alignment: _wrapAlignment('alignment'),
    spacing: _number('spacing') ?? 0.0,
    runAlignment: _wrapAlignment('runAlignment'),
    runSpacing: _number('runSpacing') ?? 0.0,
    crossAxisAlignment: _wrapCrossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    clipBehavior: _clipBehavior() ?? Clip.none,
    children: _children('children'),
  );

  Widget _listBody() {
    final mainAxis = _enum('mainAxis') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final reverse = _boolean('reverse') ?? false;

    Widget buildListBody() => ListBody(
      mainAxis: mainAxis,
      reverse: reverse,
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final fallbackWidth =
            mainAxis == Axis.vertical && !constraints.hasBoundedWidth
            ? 240.0
            : null;
        final fallbackHeight =
            mainAxis == Axis.horizontal && !constraints.hasBoundedHeight
            ? 120.0
            : null;
        // The viewport and RenderListBody intentionally share one axis
        // direction. `reverse` changes the viewport's scroll origin and the
        // ListBody's child placement; it does not reorder the child list twice.
        Widget viewport = SingleChildScrollView(
          scrollDirection: mainAxis,
          reverse: reverse,
          primary: false,
          child: buildListBody(),
        );
        if (node.slot('children')?.children.isEmpty ?? true) {
          viewport = ConstrainedBox(
            constraints: mainAxis == Axis.vertical
                ? const BoxConstraints(minHeight: 36)
                : const BoxConstraints(minWidth: 36),
            child: viewport,
          );
        }
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: viewport,
          );
        }
        return viewport;
      },
    );
  }

  Widget _overflowBar() {
    Widget buildOverflowBar() => OverflowBar(
      spacing: _number('spacing') ?? 0.0,
      alignment: _overflowBarMainAxisAlignment(),
      overflowSpacing: _number('overflowSpacing') ?? 0.0,
      overflowAlignment: _overflowBarAlignment(),
      overflowDirection: _enum('overflowDirection') == 'up'
          ? VerticalDirection.up
          : VerticalDirection.down,
      textDirection: _textDirection(),
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final empty = node.slot('children')?.children.isEmpty ?? true;
        if (!empty &&
            (constraints.hasBoundedWidth ||
                _overflowBarMainAxisAlignment() == null)) {
          return buildOverflowBar();
        }
        // RenderOverflowBar needs a finite width whenever a non-null
        // alignment expands the horizontal layout. The cap is Canvas-only:
        // generated Dart retains the user's bare OverflowBar constructor.
        return ConstrainedBox(
          constraints: BoxConstraints(
            minWidth: empty ? 36 : 0,
            minHeight: empty ? 36 : 0,
            maxWidth: constraints.hasBoundedWidth ? double.infinity : 240,
          ),
          child: buildOverflowBar(),
        );
      },
    );
  }

  Widget _listView() {
    final scrollDirection = _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final shrinkWrap = _boolean('shrinkWrap') ?? false;

    Widget buildListView() => ListView(
      scrollDirection: scrollDirection,
      reverse: _boolean('reverse') ?? false,
      primary: _boolean('primary'),
      physics: _scrollPhysics(),
      shrinkWrap: shrinkWrap,
      padding: _edgeInsetsGeometry('padding'),
      itemExtent: _number('itemExtent'),
      addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
      addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
      addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
      scrollCacheExtent: _scrollCacheExtent(),
      semanticChildCount: _integer('semanticChildCount'),
      dragStartBehavior: _dragStartBehavior(),
      keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
      restorationId: _string('restorationId'),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      hitTestBehavior: _scrollHitTestBehavior(),
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final fallbackWidth =
            !constraints.hasBoundedWidth &&
                (scrollDirection == Axis.vertical || !shrinkWrap)
            ? 240.0
            : null;
        final fallbackHeight =
            !constraints.hasBoundedHeight &&
                (scrollDirection == Axis.horizontal || !shrinkWrap)
            ? 120.0
            : null;
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: buildListView(),
          );
        }
        return buildListView();
      },
    );
  }

  Widget _gridView() {
    final scrollDirection = _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final shrinkWrap = _boolean('shrinkWrap') ?? false;

    Widget buildGridView() => GridView.count(
      scrollDirection: scrollDirection,
      reverse: _boolean('reverse') ?? false,
      primary: _boolean('primary'),
      physics: _scrollPhysics(),
      shrinkWrap: shrinkWrap,
      padding: _edgeInsetsGeometry('padding'),
      crossAxisCount: _integer('crossAxisCount') ?? 2,
      mainAxisSpacing: _number('mainAxisSpacing') ?? 0.0,
      crossAxisSpacing: _number('crossAxisSpacing') ?? 0.0,
      childAspectRatio: _number('childAspectRatio') ?? 1.0,
      mainAxisExtent: _number('mainAxisExtent'),
      addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
      addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
      addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
      scrollCacheExtent: _scrollCacheExtent(),
      semanticChildCount: _integer('semanticChildCount'),
      dragStartBehavior: _dragStartBehavior(),
      keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
      restorationId: _string('restorationId'),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      hitTestBehavior: _scrollHitTestBehavior(),
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        // A grid always needs a bounded cross axis. A non-shrink-wrapped
        // viewport additionally needs a bounded main axis. These finite caps
        // mirror the generated application's shared constraint guard.
        final fallbackWidth =
            !constraints.hasBoundedWidth &&
                (scrollDirection == Axis.vertical || !shrinkWrap)
            ? 240.0
            : null;
        final fallbackHeight =
            !constraints.hasBoundedHeight &&
                (scrollDirection == Axis.horizontal || !shrinkWrap)
            ? 120.0
            : null;
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: buildGridView(),
          );
        }
        return buildGridView();
      },
    );
  }

  Widget _singleChildScrollView() => SingleChildScrollView(
    scrollDirection: _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical,
    reverse: _boolean('reverse') ?? false,
    padding: _edgeInsetsGeometry('padding'),
    primary: _boolean('primary'),
    physics: _scrollPhysics(),
    dragStartBehavior: _dragStartBehavior(),
    clipBehavior: _clipBehavior() ?? Clip.hardEdge,
    hitTestBehavior: _scrollHitTestBehavior(),
    restorationId: _string('restorationId'),
    keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
    child: _single('child'),
  );

  ScrollPhysics? _scrollPhysics() => switch (_string('physics')) {
    'alwaysScrollable' => const AlwaysScrollableScrollPhysics(),
    'bouncing' => const BouncingScrollPhysics(),
    'clamping' => const ClampingScrollPhysics(),
    'neverScrollable' => const NeverScrollableScrollPhysics(),
    'page' => const PageScrollPhysics(),
    'rangeMaintaining' => const RangeMaintainingScrollPhysics(),
    _ => null,
  };

  ScrollCacheExtent? _scrollCacheExtent() {
    final pixels = _number('scrollCacheExtent');
    return pixels == null ? null : ScrollCacheExtent.pixels(pixels);
  }

  ScrollViewKeyboardDismissBehavior? _scrollKeyboardDismissBehavior() =>
      switch (_enum('keyboardDismissBehavior')) {
        'manual' => ScrollViewKeyboardDismissBehavior.manual,
        'onDrag' => ScrollViewKeyboardDismissBehavior.onDrag,
        _ => null,
      };

  HitTestBehavior _scrollHitTestBehavior() =>
      switch (_enum('hitTestBehavior')) {
        'deferToChild' => HitTestBehavior.deferToChild,
        'translucent' => HitTestBehavior.translucent,
        _ => HitTestBehavior.opaque,
      };

  Widget _stack() => Stack(
    alignment: _alignmentGeometry('alignment') ?? AlignmentDirectional.topStart,
    textDirection: _textDirection(),
    fit: _stackFit(),
    clipBehavior: _clipBehavior() ?? Clip.hardEdge,
    children: _children('children'),
  );

  Widget _indexedStack() {
    final indexValue = node.properties['index'];
    final index = indexValue == null
        ? 0
        : indexValue.kind == 'null'
        ? null
        : indexValue.value as int;
    return IndexedStack(
      alignment:
          _alignmentGeometry('alignment') ?? AlignmentDirectional.topStart,
      textDirection: _textDirection(),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      sizing: _stackFit('sizing'),
      index: index,
      children: _children('children'),
    );
  }

  Widget _safeArea() => SafeArea(
    left: _boolean('left') ?? true,
    top: _boolean('top') ?? true,
    right: _boolean('right') ?? true,
    bottom: _boolean('bottom') ?? true,
    minimum: _physicalEdgeInsets('minimum') ?? EdgeInsets.zero,
    maintainBottomViewPadding: _boolean('maintainBottomViewPadding') ?? false,
    child: _single('child')!,
  );

  Widget _directionality() => Directionality(
    textDirection: _textDirection()!,
    child: _single('child')!,
  );

  EdgeInsetsGeometry _paddingGeometry() {
    return _edgeInsetsGeometry('padding')!;
  }

  EdgeInsetsGeometry? _edgeInsetsGeometry(String name) {
    final value = node.properties[name]?.value;
    if (value == null) {
      return null;
    }
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
      _ => throw StateError('Unsupported Canvas EdgeInsets value.'),
    };
  }

  EdgeInsets? _physicalEdgeInsets(String name) {
    final value = node.properties[name]?.value;
    if (value == null) {
      return null;
    }
    if (value case CanvasEdgeInsets physical) {
      return EdgeInsets.fromLTRB(
        physical.left,
        physical.top,
        physical.right,
        physical.bottom,
      );
    }
    throw StateError('Unsupported physical Canvas EdgeInsets value.');
  }

  AlignmentGeometry? _alignmentGeometry(String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasAlignmentGeometryValue) {
      return null;
    }
    return value.basis == 'directional'
        ? AlignmentDirectional(value.horizontal, value.vertical)
        : Alignment(value.horizontal, value.vertical);
  }

  BoxConstraints? _boxConstraints(String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasBoxConstraintsValue) {
      return null;
    }
    return BoxConstraints(
      minWidth: value.minWidth ?? double.infinity,
      maxWidth: value.maxWidth ?? double.infinity,
      minHeight: value.minHeight ?? double.infinity,
      maxHeight: value.maxHeight ?? double.infinity,
    );
  }

  Matrix4? _matrix4(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasMatrix4Value ? Matrix4.fromList(value.storage) : null;
  }

  Offset? _offset(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasOffsetValue ? Offset(value.dx, value.dy) : null;
  }

  BoxDecoration? _boxDecoration(BuildContext context, String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasBoxDecorationValue) {
      return null;
    }
    return BoxDecoration(
      color: value.color == null ? null : _colorSource(context, value.color!),
      image: value.image == null
          ? null
          : _decorationImage(context, value.image!),
      border: value.border == null ? null : _boxBorder(context, value.border!),
      borderRadius: value.borderRadius == null
          ? null
          : _borderRadius(value.borderRadius!),
      boxShadow: [
        for (final shadow in value.boxShadow)
          BoxShadow(
            color: _colorSource(context, shadow.color),
            offset: Offset(shadow.offsetX, shadow.offsetY),
            blurRadius: shadow.blurRadius,
            spreadRadius: shadow.spreadRadius,
            blurStyle: _blurStyle(shadow.blurStyle),
          ),
      ],
      gradient: value.gradient == null
          ? null
          : _boxGradient(context, value.gradient!),
      backgroundBlendMode: value.backgroundBlendMode == null
          ? null
          : _blendMode(value.backgroundBlendMode!),
      shape: value.shape == 'circle' ? BoxShape.circle : BoxShape.rectangle,
    );
  }

  DecorationImage _decorationImage(
    BuildContext context,
    CanvasDecorationImageValue value,
  ) {
    final binding = _imageProvider(value.image);
    final resolution = binding.resolution;
    return DecorationImage(
      image: binding.provider,
      onError: value.onError && resolution is CanvasResolvedImageValue
          ? (error, stackTrace) =>
                onImageError?.call(resolution.resourceId, error, stackTrace)
          : null,
      colorFilter: value.colorFilter == null
          ? null
          : _colorFilter(context, value.colorFilter!),
      fit: value.fit == null ? null : _boxFit(value.fit!),
      alignment: _alignmentGeometryValue(value.alignment),
      // An unavailable image has no trustworthy intrinsic dimensions. The
      // status checker remains paint-safe while preserving every other visual
      // argument; a resolved image keeps the reviewed nine-patch contract.
      centerSlice: binding.placeholder || value.centerSlice == null
          ? null
          : Rect.fromLTRB(
              value.centerSlice!.left,
              value.centerSlice!.top,
              value.centerSlice!.right,
              value.centerSlice!.bottom,
            ),
      repeat: _imageRepeat(value.repeat),
      matchTextDirection: value.matchTextDirection,
      scale: value.scale,
      opacity: value.opacity,
      filterQuality: _filterQuality(value.filterQuality),
      invertColors: value.invertColors,
      isAntiAlias: value.isAntiAlias,
    );
  }

  ({
    ImageProvider<Object> provider,
    bool placeholder,
    CanvasImageResolutionValue resolution,
  })
  _imageProvider(CanvasImageProviderValue value) {
    final resolution = value.resolution;
    final resource = resolution is CanvasResolvedImageValue
        ? imageResources[resolution.resourceId]
        : null;
    final placeholder =
        resolution is! CanvasResolvedImageValue || resource == null;
    ImageProvider<Object> provider = placeholder
        ? MemoryImage(_unavailableImageBytes)
        : MemoryImage(resource.encodedBytes, scale: resolution.resolvedScale);
    final resize = value.resize;
    if (!placeholder && resize != null) {
      provider = ResizeImage(
        provider,
        width: resize.width,
        height: resize.height,
        policy: resize.policy == 'fit'
            ? ResizeImagePolicy.fit
            : ResizeImagePolicy.exact,
        allowUpscaling: resize.allowUpscaling,
      );
    }
    return (
      provider: provider,
      placeholder: placeholder,
      resolution: resolution,
    );
  }

  Widget _image(BuildContext context) {
    final provider = node.properties['image']!.value;
    if (provider is! CanvasImageProviderValue) {
      throw StateError('Canvas Image has no decoded image provider.');
    }
    final binding = _imageProvider(provider);
    final resolution = binding.resolution;
    final centerSlice = binding.placeholder ? null : _imageCenterSlice();
    final opacity = _number('opacity');
    return Image(
      image: binding.provider,
      frameBuilder: _callbackPresent('frameBuilder')
          ? (context, child, frame, wasSynchronouslyLoaded) => child
          : null,
      loadingBuilder: _callbackPresent('loadingBuilder')
          ? (context, child, loadingProgress) => child
          : null,
      errorBuilder: _callbackPresent('errorBuilder')
          ? (context, error, stackTrace) {
              if (resolution is CanvasResolvedImageValue) {
                onImageError?.call(resolution.resourceId, error, stackTrace);
              }
              return const SizedBox.shrink();
            }
          : null,
      semanticLabel: _string('semanticLabel'),
      excludeFromSemantics: _boolean('excludeFromSemantics') ?? false,
      width: _number('width'),
      height: _number('height'),
      color: _resolvedColor(context, 'color'),
      opacity: opacity == null ? null : AlwaysStoppedAnimation<double>(opacity),
      colorBlendMode: _enum('colorBlendMode') == null
          ? null
          : _blendMode(_enum('colorBlendMode')!),
      fit: _enum('fit') == null ? null : _boxFit(_enum('fit')!),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      repeat: _imageRepeat(_enum('repeat') ?? 'noRepeat'),
      centerSlice: centerSlice,
      matchTextDirection: _boolean('matchTextDirection') ?? false,
      gaplessPlayback: _boolean('gaplessPlayback') ?? false,
      isAntiAlias: _boolean('isAntiAlias') ?? false,
      filterQuality: _filterQuality(_enum('filterQuality') ?? 'medium'),
    );
  }

  Widget _imageIcon(BuildContext context) {
    final value = node.properties['image']!.value;
    return ImageIcon(
      value is CanvasImageProviderValue ? _imageProvider(value).provider : null,
      size: _number('size'),
      color: _resolvedColor(context, 'color'),
      semanticLabel: _string('semanticLabel'),
    );
  }

  Rect? _imageCenterSlice() {
    final left = _number('centerSliceLeft');
    final top = _number('centerSliceTop');
    final right = _number('centerSliceRight');
    final bottom = _number('centerSliceBottom');
    if (left == null || top == null || right == null || bottom == null) {
      return null;
    }
    return Rect.fromLTRB(left, top, right, bottom);
  }

  Widget _textField(BuildContext context) {
    final expands = _boolean('expands') ?? false;
    final maxLength = _integer('maxLength');
    final cursorRadiusX = _number('cursorRadiusX');
    final cursorRadiusY = _number('cursorRadiusY');
    final scrollPaddingLeft = _number('scrollPaddingLeft');
    final scrollPaddingTop = _number('scrollPaddingTop');
    final scrollPaddingRight = _number('scrollPaddingRight');
    final scrollPaddingBottom = _number('scrollPaddingBottom');

    final field = IgnorePointer(
      ignoring: true,
      child: ExcludeFocus(
        excluding: true,
        child: Focus(
          canRequestFocus: false,
          skipTraversal: true,
          descendantsAreFocusable: false,
          descendantsAreTraversable: false,
          child: TextField(
            keyboardType: _textInputType(),
            textInputAction: _textInputAction(),
            textCapitalization:
                _textCapitalization() ?? TextCapitalization.none,
            textAlign: _textAlign() ?? TextAlign.start,
            textAlignVertical: _textAlignVertical(),
            textDirection: _textDirection(),
            readOnly: _boolean('readOnly') ?? false,
            showCursor: _boolean('showCursor'),
            autofocus: _boolean('autofocus') ?? false,
            obscuringCharacter: _string('obscuringCharacter') ?? '•',
            obscureText: _boolean('obscureText') ?? false,
            autocorrect: _boolean('autocorrect'),
            smartDashesType: _smartDashesType(),
            smartQuotesType: _smartQuotesType(),
            enableSuggestions: _boolean('enableSuggestions') ?? true,
            maxLines: expands ? null : (_integer('maxLines') ?? 1),
            minLines: expands ? null : _integer('minLines'),
            expands: expands,
            maxLength: maxLength == -1 ? TextField.noMaxLength : maxLength,
            maxLengthEnforcement: _maxLengthEnforcement(),
            onChanged: _callbackPresent('onChanged') ? (value) {} : null,
            onEditingComplete: _callbackPresent('onEditingComplete')
                ? () {}
                : null,
            onSubmitted: _callbackPresent('onSubmitted') ? (value) {} : null,
            onAppPrivateCommand: _callbackPresent('onAppPrivateCommand')
                ? (action, data) {}
                : null,
            enabled: _boolean('enabled'),
            ignorePointers: _boolean('ignorePointers'),
            cursorWidth: _number('cursorWidth') ?? 2.0,
            cursorHeight: _number('cursorHeight'),
            cursorRadius: cursorRadiusX == null || cursorRadiusY == null
                ? null
                : Radius.elliptical(cursorRadiusX, cursorRadiusY),
            cursorOpacityAnimates: _boolean('cursorOpacityAnimates'),
            cursorColor: _resolvedColor(context, 'cursorColor'),
            cursorErrorColor: _resolvedColor(context, 'cursorErrorColor'),
            selectionHeightStyle: _boxHeightStyle(),
            selectionWidthStyle: _boxWidthStyle(),
            keyboardAppearance: _brightness('keyboardAppearance'),
            scrollPadding:
                scrollPaddingLeft == null ||
                    scrollPaddingTop == null ||
                    scrollPaddingRight == null ||
                    scrollPaddingBottom == null
                ? const EdgeInsets.all(20.0)
                : EdgeInsets.fromLTRB(
                    scrollPaddingLeft,
                    scrollPaddingTop,
                    scrollPaddingRight,
                    scrollPaddingBottom,
                  ),
            dragStartBehavior: _dragStartBehavior(),
            enableInteractiveSelection: _boolean('enableInteractiveSelection'),
            selectAllOnFocus: _boolean('selectAllOnFocus'),
            onTap: _callbackPresent('onTap') ? () {} : null,
            onTapAlwaysCalled: _boolean('onTapAlwaysCalled') ?? false,
            onTapOutside: _callbackPresent('onTapOutside') ? (event) {} : null,
            onTapUpOutside: _callbackPresent('onTapUpOutside')
                ? (event) {}
                : null,
            mouseCursor: _mouseCursor('mouseCursor'),
            clipBehavior: _clipBehavior() ?? Clip.hardEdge,
            restorationId: _string('restorationId'),
            stylusHandwritingEnabled:
                _boolean('stylusHandwritingEnabled') ??
                EditableText.defaultStylusHandwritingEnabled,
            enableIMEPersonalizedLearning:
                _boolean('enableIMEPersonalizedLearning') ?? true,
            enableInlinePrediction: _boolean('enableInlinePrediction'),
            canRequestFocus: _boolean('canRequestFocus') ?? true,
          ),
        ),
      ),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final guardWidth = constraints.maxWidth.isInfinite;
        final guardHeight = expands && constraints.maxHeight.isInfinite;
        return SizedBox(
          width: guardWidth ? 240 : null,
          height: guardHeight ? 120 : null,
          child: field,
        );
      },
    );
  }

  ColorFilter _colorFilter(
    BuildContext context,
    CanvasColorFilterValue value,
  ) => switch (value) {
    CanvasModeColorFilterValue() => ColorFilter.mode(
      _colorSource(context, value.color),
      _blendMode(value.blendMode),
    ),
    CanvasMatrixColorFilterValue() => ColorFilter.matrix(value.values),
    CanvasLinearToSrgbGammaColorFilterValue() =>
      const ColorFilter.linearToSrgbGamma(),
    CanvasSrgbToLinearGammaColorFilterValue() =>
      const ColorFilter.srgbToLinearGamma(),
    CanvasSaturationColorFilterValue() => ColorFilter.saturation(value.value),
  };

  BoxFit _boxFit(String value) => switch (value) {
    'fill' => BoxFit.fill,
    'contain' => BoxFit.contain,
    'cover' => BoxFit.cover,
    'fitWidth' => BoxFit.fitWidth,
    'fitHeight' => BoxFit.fitHeight,
    'none' => BoxFit.none,
    _ => BoxFit.scaleDown,
  };

  ImageRepeat _imageRepeat(String value) => switch (value) {
    'repeat' => ImageRepeat.repeat,
    'repeatX' => ImageRepeat.repeatX,
    'repeatY' => ImageRepeat.repeatY,
    _ => ImageRepeat.noRepeat,
  };

  FilterQuality _filterQuality(String value) => switch (value) {
    'none' => FilterQuality.none,
    'low' => FilterQuality.low,
    'high' => FilterQuality.high,
    _ => FilterQuality.medium,
  };

  String _imageStatusSemantics() {
    final statuses = <String>[];
    if (node.type == 'flutter.material.CircleAvatar') {
      for (final name in const ['backgroundImage', 'foregroundImage']) {
        final provider = node.properties[name]?.value;
        if (provider is CanvasImageProviderValue) {
          _appendImageStatus(statuses, provider, propertyName: name);
        }
      }
    }
    final directProvider = node.properties['image']?.value;
    if (directProvider is CanvasImageProviderValue) {
      _appendImageStatus(statuses, directProvider);
    }
    for (final name in const ['decoration', 'foregroundDecoration']) {
      final decoration = node.properties[name]?.value;
      if (decoration is! CanvasBoxDecorationValue || decoration.image == null) {
        continue;
      }
      _appendImageStatus(statuses, decoration.image!.image);
    }
    return statuses.isEmpty ? '' : '. ${statuses.join('. ')}';
  }

  void _appendImageStatus(
    List<String> statuses,
    CanvasImageProviderValue provider, {
    String? propertyName,
  }) {
    final resolution = provider.resolution;
    final identity =
        '${propertyName == null ? '' : '$propertyName: '}'
        '${_imageProviderIdentity(provider)}';
    if (resolution is CanvasUnavailableImageValue) {
      statuses.add(
        'Image preview unavailable for $identity. '
        'Status ${resolution.code}. Reason: ${resolution.reason}',
      );
    } else if (resolution is CanvasResolvedImageValue &&
        imageResources[resolution.resourceId] == null) {
      final rejection = imageResources.rejection(resolution.resourceId);
      statuses.add(
        'Image preview unavailable for $identity. '
        'Status ${rejection?.code ?? 'missing'}. Reason: '
        '${rejection?.reason ?? 'the content-addressed resource is not bound to this revision'}',
      );
    }
  }

  String _imageProviderIdentity(CanvasImageProviderValue provider) {
    final packageName = provider.packageName;
    return packageName == null
        ? 'app:${provider.assetName}'
        : 'package:$packageName:${provider.assetName}';
  }

  BoxBorder _boxBorder(BuildContext context, CanvasBoxBorderValue value) =>
      switch (value) {
        CanvasPhysicalBoxBorderValue() => Border(
          top: _borderSide(context, value.top),
          right: _borderSide(context, value.right),
          bottom: _borderSide(context, value.bottom),
          left: _borderSide(context, value.left),
        ),
        CanvasDirectionalBoxBorderValue() => BorderDirectional(
          top: _borderSide(context, value.top),
          start: _borderSide(context, value.start),
          end: _borderSide(context, value.end),
          bottom: _borderSide(context, value.bottom),
        ),
      };

  BorderSide _borderSide(BuildContext context, CanvasBorderSideValue value) =>
      BorderSide(
        color: _colorSource(context, value.color),
        width: value.width,
        style: value.style == 'none' ? BorderStyle.none : BorderStyle.solid,
        strokeAlign: value.strokeAlign,
      );

  BorderRadiusGeometry _borderRadius(CanvasBorderRadiusGeometryValue value) =>
      switch (value) {
        CanvasPhysicalBorderRadiusValue() => BorderRadius.only(
          topLeft: _radius(value.topLeft),
          topRight: _radius(value.topRight),
          bottomRight: _radius(value.bottomRight),
          bottomLeft: _radius(value.bottomLeft),
        ),
        CanvasDirectionalBorderRadiusValue() => BorderRadiusDirectional.only(
          topStart: _radius(value.topStart),
          topEnd: _radius(value.topEnd),
          bottomEnd: _radius(value.bottomEnd),
          bottomStart: _radius(value.bottomStart),
        ),
      };

  Radius _radius(CanvasRadiusValue value) =>
      Radius.elliptical(value.x, value.y);

  Gradient _boxGradient(BuildContext context, CanvasBoxGradientValue value) {
    final colors = [
      for (final stop in value.stops) _colorSource(context, stop.color),
    ];
    final stops = [for (final stop in value.stops) stop.stop];
    final transform = value.rotationRadians == null
        ? null
        : GradientRotation(value.rotationRadians!);
    return switch (value) {
      CanvasLinearGradientValue() => LinearGradient(
        begin: _alignmentGeometryValue(value.begin),
        end: _alignmentGeometryValue(value.end),
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        transform: transform,
      ),
      CanvasRadialGradientValue() => RadialGradient(
        center: _alignmentGeometryValue(value.center),
        radius: value.radius,
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        focal: value.focal == null
            ? null
            : _alignmentGeometryValue(value.focal!),
        focalRadius: value.focalRadius,
        transform: transform,
      ),
      CanvasSweepGradientValue() => SweepGradient(
        center: _alignmentGeometryValue(value.center),
        startAngle: value.startAngle,
        endAngle: value.endAngle,
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        transform: transform,
      ),
    };
  }

  AlignmentGeometry _alignmentGeometryValue(
    CanvasAlignmentGeometryValue value,
  ) => value.basis == 'directional'
      ? AlignmentDirectional(value.horizontal, value.vertical)
      : Alignment(value.horizontal, value.vertical);

  TileMode _tileMode(String value) => switch (value) {
    'repeated' => TileMode.repeated,
    'mirror' => TileMode.mirror,
    'decal' => TileMode.decal,
    _ => TileMode.clamp,
  };

  BlurStyle _blurStyle(String value) => switch (value) {
    'solid' => BlurStyle.solid,
    'outer' => BlurStyle.outer,
    'inner' => BlurStyle.inner,
    _ => BlurStyle.normal,
  };

  Widget _padding(EdgeInsetsGeometry insets) =>
      Padding(padding: insets, child: _single('child'));

  Widget _aspectRatio() => AspectRatio(
    aspectRatio: _number('aspectRatio')!,
    child: _single('child'),
  );

  Widget _baseline() => Baseline(
    baseline: _number('baseline')!,
    baselineType: _textBaseline('baselineType')!,
    child: _single('child'),
  );

  Widget _intrinsicHeight() => IntrinsicHeight(child: _single('child'));

  Widget _intrinsicWidth() => IntrinsicWidth(
    stepWidth: _number('stepWidth'),
    stepHeight: _number('stepHeight'),
    child: _single('child'),
  );

  Widget _offstage() =>
      Offstage(offstage: _boolean('offstage') ?? true, child: _single('child'));

  Widget _rotatedBox() => RotatedBox(
    quarterTurns: _integer('quarterTurns')!,
    child: _single('child'),
  );

  Widget _sizedOverflowBox() {
    final requestedSize = node.properties['size']!.value as CanvasSizeValue;
    return SizedOverflowBox(
      size: Size(requestedSize.width, requestedSize.height),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      child: _single('child'),
    );
  }

  Widget _transform(Widget child) {
    final filterQuality = _enum('filterQuality');
    return Transform(
      key: ValueKey('canvas-transform-${node.id}'),
      transform: _matrix4('transform')!,
      origin: _offset('origin'),
      alignment: _alignmentGeometry('alignment'),
      transformHitTests: _boolean('transformHitTests') ?? true,
      filterQuality: filterQuality == null
          ? null
          : _filterQuality(filterQuality),
      child: child,
    );
  }

  Widget _align() => Align(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _fractionallySizedBox() => FractionallySizedBox(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _fittedBox() => FittedBox(
    fit: _boxFit(_enum('fit') ?? 'contain'),
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _clipRect() {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRect',
        expectedType: 'CustomClipper<Rect>',
      );
    }
    return ClipRect(
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      child: _single('child'),
    );
  }

  Widget _clipOval() {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipOval',
        expectedType: 'CustomClipper<Rect>',
      );
    }
    return ClipOval(
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipRRect(BuildContext context) {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRRect',
        expectedType: 'CustomClipper<RRect>',
      );
    }
    final radius = node.properties['borderRadius']?.value;
    return ClipRRect(
      borderRadius: radius is CanvasBorderRadiusGeometryValue
          ? _borderRadius(radius).resolve(Directionality.of(context))
          : BorderRadius.zero,
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipRSuperellipse(BuildContext context) {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRSuperellipse',
        expectedType: 'CustomClipper<RSuperellipse>',
      );
    }
    final radius = node.properties['borderRadius']?.value;
    return ClipRSuperellipse(
      borderRadius: radius is CanvasBorderRadiusGeometryValue
          ? _borderRadius(radius).resolve(Directionality.of(context))
          : BorderRadius.zero,
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipPath() {
    if (node.properties['shape']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipPath.shape',
        expectedType: 'ShapeBorder',
        previewLabel: 'Custom shape\npreview unavailable',
      );
    }
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipPath',
        expectedType: 'CustomClipper<Path>',
      );
    }
    return ClipPath(
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _circleAvatar(BuildContext context) {
    ({ImageProvider<Object>? provider, ImageErrorListener? onError}) image(
      String name,
    ) {
      final value = node.properties[name]?.value;
      if (value is! CanvasImageProviderValue) {
        return (provider: null, onError: null);
      }
      final binding = _imageProvider(value);
      // A checkerboard foreground would hide the valid background/initials.
      // A missing/rejected layer is omitted with slot-specific diagnostics,
      // retaining CircleAvatar's actual SDK foreground/background fallback.
      if (binding.placeholder) return (provider: null, onError: null);
      final resource = binding.resolution as CanvasResolvedImageValue;
      return (
        provider: binding.provider,
        // This is a Canvas safety reporter, not execution of the model's Dart
        // callback reference. It also protects previews without a callback.
        onError: (error, stack) =>
            onImageError?.call(resource.resourceId, error, stack),
      );
    }

    double? radius(String name) {
      final value = node.properties[name]?.value;
      return value is CanvasEnumValue
          ? double.infinity
          : (value as num?)?.toDouble();
    }

    final fixed = radius('radius');
    final minimum = radius('minRadius');
    final maximum = radius('maxRadius');
    final defaults = fixed == null && minimum == null && maximum == null;
    final minDiameter = defaults ? 40.0 : 2 * (fixed ?? minimum ?? 0);
    final maxDiameter = defaults
        ? 40.0
        : 2 * (fixed ?? maximum ?? double.infinity);
    final background = image('backgroundImage');
    final foreground = image('foregroundImage');
    final avatar = CircleAvatar(
      // Flutter cannot interpolate finite and infinite BoxConstraints. Reset
      // only that SDK animation shell on this boundary; global Canvas child
      // keys preserve the existing subtree's state/focus across the reparent.
      key: ValueKey((
        'circle-avatar-constraint-finiteness',
        minDiameter.isInfinite,
        maxDiameter.isInfinite,
      )),
      backgroundColor: _resolvedColor(context, 'backgroundColor'),
      foregroundColor: _resolvedColor(context, 'foregroundColor'),
      backgroundImage: background.provider,
      foregroundImage: foreground.provider,
      onBackgroundImageError: background.onError,
      onForegroundImageError: foreground.onError,
      radius: fixed,
      minRadius: minimum,
      maxRadius: maximum,
      child: _single('child'),
    );
    // AnimatedContainer can reach zero after the model revision's first
    // layout. Refresh the surface-only selection handle when that animation
    // changes size, without inserting a visible child or changing SDK bounds.
    return NotificationListener<SizeChangedLayoutNotification>(
      onNotification: (_) {
        context
            .findAncestorStateOfType<_CanvasDocumentViewState>()
            ?._refreshZeroSizedWidgetTargetsAfterFrame();
        return false;
      },
      child: SizeChangedLayoutNotifier(child: avatar),
    );
  }

  Widget _badge(BuildContext context) {
    final backgroundColor = _resolvedColor(context, 'backgroundColor');
    final textColor = _resolvedColor(context, 'textColor');
    final smallSize = _number('smallSize');
    final largeSize = _number('largeSize');
    final textStyle = _textStyle(context, 'textStyle');
    final padding = _edgeInsetsGeometry('padding');
    final alignment = _alignmentGeometry('alignment');
    final offset = _offset('offset');
    final isLabelVisible = _boolean('isLabelVisible') ?? true;
    final child = _single('child');
    final count = _integer('count');
    // Flutter 3.44.8's _IntrinsicHorizontalStadium has no updateRenderObject:
    // its minSize otherwise remains stale after a local or BadgeTheme edit.
    // Recreate only the SDK Badge for a changed effective minimum. Existing
    // global node keys reparent actual child/label state, including edit focus.
    final hasLabel = count != null || node.slot('label')?.child != null;
    final sdkKey = ValueKey((
      'badge-stadium-minimum',
      isLabelVisible && hasLabel
          ? largeSize ?? BadgeTheme.of(context).largeSize ?? 16.0
          : null,
    ));
    if (count != null) {
      return Badge.count(
        key: sdkKey,
        backgroundColor: backgroundColor,
        textColor: textColor,
        smallSize: smallSize,
        largeSize: largeSize,
        textStyle: textStyle,
        padding: padding,
        alignment: alignment,
        offset: offset,
        count: count,
        maxCount: _integer('maxCount') ?? 999,
        isLabelVisible: isLabelVisible,
        child: child,
      );
    }
    return Badge(
      key: sdkKey,
      backgroundColor: backgroundColor,
      textColor: textColor,
      smallSize: smallSize,
      largeSize: largeSize,
      textStyle: textStyle,
      padding: padding,
      alignment: alignment,
      offset: offset,
      label: _single('label'),
      isLabelVisible: isLabelVisible,
      child: child,
    );
  }

  Widget _card(BuildContext context) {
    final unavailable = _cardShapePreviewUnavailableMessage(node);
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'Card.shape',
        expectedType: 'ShapeBorder',
        previewLabel: 'Card shape\npreview unavailable',
        messageOverride: unavailable,
      );
    }
    final color = _resolvedColor(context, 'color');
    final shadowColor = _resolvedColor(context, 'shadowColor');
    final surfaceTintColor = _resolvedColor(context, 'surfaceTintColor');
    final elevation = _number('elevation');
    final shape = _cardShape(context);
    final borderOnForeground = _boolean('borderOnForeground') ?? true;
    final margin = _edgeInsetsGeometry('margin');
    final clipBehavior = _clipBehavior();
    final semanticContainer = _boolean('semanticContainer') ?? true;
    final child = _single('child');
    return switch (_string('variant')) {
      'elevated' => Card(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      'filled' => Card.filled(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      'outlined' => Card.outlined(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      _ => throw StateError('Unreviewed Canvas Card variant.'),
    };
  }

  ShapeBorder? _cardShape(BuildContext context) {
    final kind = _string('shapeKind');
    if (kind == null) return null;
    final side = _appBarBorderSide(context);
    final value = node.properties['shapeRadius']?.value;
    final radius = value is CanvasBorderRadiusGeometryValue
        ? _borderRadius(value)
        : BorderRadius.zero;
    LinearBorderEdge? edge(String name) {
      final size = _number('shape${name}Size');
      final alignment = _number('shape${name}Alignment');
      return size == null && alignment == null
          ? null
          : LinearBorderEdge(size: size ?? 1, alignment: alignment ?? 0);
    }

    return switch (kind) {
      'roundedRectangle' => RoundedRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'beveledRectangle' => BeveledRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'continuousRectangle' => ContinuousRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'roundedSuperellipse' => RoundedSuperellipseBorder(
        side: side,
        borderRadius: radius,
      ),
      'circle' => CircleBorder(
        side: side,
        eccentricity: _number('shapeCircleEccentricity') ?? 0,
      ),
      'oval' => OvalBorder(
        side: side,
        eccentricity: _number('shapeCircleEccentricity') ?? 1,
      ),
      'stadium' => StadiumBorder(side: side),
      'linear' => LinearBorder(
        side: side,
        start: edge('Start'),
        end: edge('End'),
        top: edge('Top'),
        bottom: edge('Bottom'),
      ),
      'star' => StarBorder(
        side: side,
        points: _number('shapePoints') ?? 5,
        innerRadiusRatio: _number('shapeInnerRadiusRatio') ?? .4,
        pointRounding: _number('shapePointRounding') ?? 0,
        valleyRounding: _number('shapeValleyRounding') ?? 0,
        rotation: _number('shapeRotation') ?? 0,
        squash: _number('shapeSquash') ?? 0,
      ),
      'polygon' => StarBorder.polygon(
        side: side,
        sides: _number('shapePoints') ?? 5,
        pointRounding: _number('shapePointRounding') ?? 0,
        rotation: _number('shapeRotation') ?? 0,
        squash: _number('shapeSquash') ?? 0,
      ),
      _ => throw StateError('Unreviewed Canvas Card shape kind: $kind'),
    };
  }

  Widget _customClipperPreviewUnavailable({
    required String widgetName,
    required String expectedType,
    String previewLabel = 'Custom clipper\npreview unavailable',
    String? messageOverride,
  }) {
    final message =
        messageOverride ??
        _customClipperPreviewUnavailableMessage(
          widgetName: widgetName,
          expectedType: expectedType,
        );
    return Tooltip(
      message: message,
      excludeFromSemantics: true,
      child: Stack(
        fit: StackFit.passthrough,
        clipBehavior: Clip.none,
        children: [
          _single('child') ?? const SizedBox.shrink(),
          Positioned.fill(
            child: IgnorePointer(
              child: Semantics(
                key: ValueKey('canvas-custom-clipper-preview-${node.id}'),
                container: true,
                label: message,
                child: ExcludeSemantics(
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: Colors.amber.withValues(alpha: 0.22),
                      border: Border.all(
                        color: Colors.amber.shade800,
                        width: 1,
                      ),
                    ),
                    child: Center(
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: Colors.amber.shade100,
                          borderRadius: BorderRadius.circular(2),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(3),
                          child: Text(
                            previewLabel,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: Colors.black87,
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                              height: 1.05,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _opacity() => Opacity(
    opacity: _number('opacity')!,
    alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
    child: _single('child'),
  );

  Widget _placeholder(BuildContext context) => Placeholder(
    color: _resolvedColor(context, 'color') ?? const Color(0xFF455A64),
    strokeWidth: _number('strokeWidth') ?? 2.0,
    fallbackWidth: _number('fallbackWidth') ?? 400.0,
    fallbackHeight: _number('fallbackHeight') ?? 400.0,
    child: _single('child'),
  );

  Widget _coloredBox(BuildContext context) => ColoredBox(
    color: _resolvedColor(context, 'color')!,
    isAntiAlias: _boolean('isAntiAlias') ?? true,
    child: _single('child'),
  );

  Widget _physicalModel(BuildContext context) {
    final radius = node.properties['borderRadius']?.value;
    return PhysicalModel(
      shape: _enum('shape') == 'circle' ? BoxShape.circle : BoxShape.rectangle,
      clipBehavior: _clipBehavior() ?? Clip.none,
      borderRadius: radius is CanvasPhysicalBorderRadiusValue
          ? _borderRadius(radius) as BorderRadius
          : null,
      elevation: _number('elevation') ?? 0.0,
      color: _resolvedColor(context, 'color')!,
      shadowColor:
          _resolvedColor(context, 'shadowColor') ?? const Color(0xFF000000),
      child: _single('child'),
    );
  }

  Widget _physicalShape(BuildContext context) {
    final clipper = node.properties['clipper']!;
    if (clipper.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'PhysicalShape',
        expectedType: 'CustomClipper<Path>',
      );
    }
    final value = clipper.value as CanvasShapeBorderClipperValue;
    final radius = _borderRadius(value.borderRadius);
    final ShapeBorder shape = switch (value.shape) {
      'roundedRectangle' => RoundedRectangleBorder(borderRadius: radius),
      'beveledRectangle' => BeveledRectangleBorder(borderRadius: radius),
      'continuousRectangle' => ContinuousRectangleBorder(borderRadius: radius),
      'roundedSuperellipse' => RoundedSuperellipseBorder(borderRadius: radius),
      'circle' => const CircleBorder(),
      'stadium' => const StadiumBorder(),
      _ => throw StateError('Unreviewed ShapeBorderClipper shape.'),
    };
    return PhysicalShape(
      clipper: ShapeBorderClipper(
        shape: shape,
        textDirection: switch (value.textDirection) {
          'ltr' => TextDirection.ltr,
          'rtl' => TextDirection.rtl,
          _ => null,
        },
      ),
      clipBehavior: _clipBehavior() ?? Clip.none,
      elevation: _number('elevation') ?? 0.0,
      color: _resolvedColor(context, 'color')!,
      shadowColor:
          _resolvedColor(context, 'shadowColor') ?? const Color(0xFF000000),
      child: _single('child'),
    );
  }

  Widget _center() => Center(
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _constrainedBox() => ConstrainedBox(
    constraints: _boxConstraints('constraints')!,
    child: _single('child'),
  );

  Widget _unconstrainedBox() => UnconstrainedBox(
    textDirection: _textDirection(),
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    constrainedAxis: switch (_enum('constrainedAxis')) {
      'horizontal' => Axis.horizontal,
      'vertical' => Axis.vertical,
      _ => null,
    },
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _limitedBox() => LimitedBox(
    maxWidth: _number('maxWidth') ?? double.infinity,
    maxHeight: _number('maxHeight') ?? double.infinity,
    child: _single('child'),
  );

  Widget _overflowBox() => OverflowBox(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    minWidth: _number('minWidth'),
    maxWidth: _number('maxWidth'),
    minHeight: _number('minHeight'),
    maxHeight: _number('maxHeight'),
    fit: switch (_enum('fit')) {
      'deferToChild' => OverflowBoxFit.deferToChild,
      _ => OverflowBoxFit.max,
    },
    child: _single('child'),
  );

  Widget _container(BuildContext context) => Container(
    alignment: _alignmentGeometry('alignment'),
    padding: _edgeInsetsGeometry('padding'),
    color: _resolvedColor(context, 'color'),
    isAntiAlias: _boolean('isAntiAlias') ?? true,
    decoration: _boxDecoration(context, 'decoration'),
    foregroundDecoration: _boxDecoration(context, 'foregroundDecoration'),
    width: _number('width'),
    height: _number('height'),
    constraints: _boxConstraints('constraints'),
    margin: _edgeInsetsGeometry('margin'),
    transform: _matrix4('transform'),
    transformAlignment: _alignmentGeometry('transformAlignment'),
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _decoratedBox(BuildContext context) => DecoratedBox(
    decoration: _boxDecoration(context, 'decoration')!,
    position: _enum('position') == 'foreground'
        ? DecorationPosition.foreground
        : DecorationPosition.background,
    child: _single('child'),
  );

  Widget _blockSemantics() => BlockSemantics(
    blocking: _boolean('blocking') ?? true,
    child: _single('child'),
  );

  Widget _absorbPointer() => AbsorbPointer(
    absorbing: _boolean('absorbing') ?? true,
    // The pinned SDK still supports this deprecated compatibility branch.
    // ignore: deprecated_member_use
    ignoringSemantics: _boolean('ignoringSemantics'),
    child: _single('child'),
  );

  Widget _ignorePointer() => IgnorePointer(
    ignoring: _boolean('ignoring') ?? true,
    // The pinned SDK still supports this deprecated compatibility branch.
    // ignore: deprecated_member_use
    ignoringSemantics: _boolean('ignoringSemantics'),
    child: _single('child'),
  );

  Widget _excludeSemantics() => ExcludeSemantics(
    excluding: _boolean('excluding') ?? true,
    child: _single('child'),
  );

  Widget _sizedBox() => SizedBox(
    width: _number('width'),
    height: _number('height'),
    child: _single('child'),
  );

  Widget _icon(BuildContext context) {
    final value = node.properties['icon']!.value as CanvasIconDataValue;
    final icon = value.codePoint == null
        ? null
        : IconData(
            // The designer intentionally reconstructs reviewed wire metadata
            // at runtime; it never evaluates an arbitrary Dart expression.
            // ignore: non_const_argument_for_const_parameter
            value.codePoint!,
            // ignore: non_const_argument_for_const_parameter
            fontFamily: value.fontFamily,
            // ignore: non_const_argument_for_const_parameter
            fontPackage: value.fontPackage,
            matchTextDirection: value.matchTextDirection,
            fontFamilyFallback: value.fontFamilyFallback.isEmpty
                ? null
                : value.fontFamilyFallback,
          );
    return Icon(
      icon,
      size: _number('size'),
      fill: _number('fill'),
      weight: _number('weight'),
      grade: _number('grade'),
      opticalSize: _number('opticalSize'),
      color: _resolvedColor(context, 'color'),
      shadows: _shadows(context, 'shadows'),
      semanticLabel: _string('semanticLabel'),
      textDirection: _textDirection(),
      applyTextScaling: _boolean('applyTextScaling'),
      blendMode: _optionalBlendMode('blendMode'),
      fontWeight: _fontWeight('fontWeight'),
    );
  }

  ScrollNotificationPredicate _notificationPredicate() =>
      switch (_string('notificationPredicate')) {
        'depthZero' => (notification) => notification.depth == 0,
        'all' => (notification) => true,
        _ => defaultScrollNotificationPredicate,
      };

  IconThemeData? _iconTheme(BuildContext context, String prefix) {
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    return IconThemeData(
      size: _number('${prefix}Size'),
      fill: _number('${prefix}Fill'),
      weight: _number('${prefix}Weight'),
      grade: _number('${prefix}Grade'),
      opticalSize: _number('${prefix}OpticalSize'),
      color: _resolvedColor(context, '${prefix}Color'),
      opacity: _number('${prefix}Opacity'),
      shadows: _shadows(context, '${prefix}Shadows'),
      applyTextScaling: _boolean('${prefix}ApplyTextScaling'),
    );
  }

  Widget _iconThemeWidget(BuildContext context) {
    final data = IconThemeData(
      size: _number('size'),
      fill: _number('fill'),
      weight: _number('weight'),
      grade: _number('grade'),
      opticalSize: _number('opticalSize'),
      color: _resolvedColor(context, 'color'),
      opacity: _number('opacity'),
      shadows: _shadows(context, 'shadows'),
      applyTextScaling: _boolean('applyTextScaling'),
    );
    final child = _single('child')!;
    return _boolean('merge')!
        ? IconTheme.merge(data: data, child: child)
        : IconTheme(data: data, child: child);
  }

  ShapeBorder? _appBarShape(BuildContext context) {
    final kind = _string('shapeKind');
    if (kind == null) {
      return null;
    }
    final side = _appBarBorderSide(context);
    final borderRadius = BorderRadius.only(
      topLeft: Radius.circular(_number('shapeRadiusTopLeft') ?? 0),
      topRight: Radius.circular(_number('shapeRadiusTopRight') ?? 0),
      bottomRight: Radius.circular(_number('shapeRadiusBottomRight') ?? 0),
      bottomLeft: Radius.circular(_number('shapeRadiusBottomLeft') ?? 0),
    );
    return switch (kind) {
      'roundedRectangle' => RoundedRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      'stadium' => StadiumBorder(side: side),
      'circle' => CircleBorder(
        side: side,
        eccentricity: _number('shapeCircleEccentricity') ?? 0,
      ),
      'beveledRectangle' => BeveledRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      'continuousRectangle' => ContinuousRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      _ => throw StateError('Unreviewed Canvas AppBar shape kind: $kind'),
    };
  }

  BorderSide _appBarBorderSide(BuildContext context) {
    if (!node.properties.keys.any((name) => name.startsWith('shapeSide'))) {
      return BorderSide.none;
    }
    return BorderSide(
      color:
          _resolvedColor(context, 'shapeSideColor') ?? const Color(0xff000000),
      width: _number('shapeSideWidth') ?? 1.0,
      style: _enum('shapeSideStyle') == 'none'
          ? BorderStyle.none
          : BorderStyle.solid,
      strokeAlign:
          _number('shapeSideStrokeAlign') ?? BorderSide.strokeAlignInside,
    );
  }

  SystemUiOverlayStyle? _systemOverlayStyle(BuildContext context) {
    const prefix = 'systemOverlayStyle';
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    return SystemUiOverlayStyle(
      systemNavigationBarColor: _resolvedColor(
        context,
        '${prefix}SystemNavigationBarColor',
      ),
      systemNavigationBarDividerColor: _resolvedColor(
        context,
        '${prefix}SystemNavigationBarDividerColor',
      ),
      systemNavigationBarIconBrightness: _brightness(
        '${prefix}SystemNavigationBarIconBrightness',
      ),
      systemNavigationBarContrastEnforced: _boolean(
        '${prefix}SystemNavigationBarContrastEnforced',
      ),
      statusBarColor: _resolvedColor(context, '${prefix}StatusBarColor'),
      statusBarBrightness: _brightness('${prefix}StatusBarBrightness'),
      statusBarIconBrightness: _brightness('${prefix}StatusBarIconBrightness'),
      systemStatusBarContrastEnforced: _boolean(
        '${prefix}SystemStatusBarContrastEnforced',
      ),
    );
  }

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

  Widget _inlineTextEditor(BuildContext context) => _CanvasInlineTextEditor(
    key: ValueKey('canvas-inline-text-editor-${node.id}'),
    widgetId: node.id,
    designerFocusParent: designerFocusParent,
    initialText: _string('data')!,
    style: _textStyle(context),
    strutStyle: _strutStyle(),
    textAlign: _textAlign() ?? TextAlign.start,
    textDirection: _textDirection(),
    maxLines: _integer('maxLines') ?? 8,
    cursorColor:
        _resolvedColor(context, 'selectionColor') ??
        Theme.of(context).colorScheme.primary,
    onCommit: onCommitInlineTextEdit,
    onCancel: onCancelInlineTextEdit,
  );

  TextStyle? _textStyle(BuildContext context, [String prefix = 'style']) {
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    final themeBase = _themeTextStyle(context, '${prefix}ThemeTextStyle');
    final hasOverrides = node.properties.keys.any(
      (name) => name.startsWith(prefix) && name != '${prefix}ThemeTextStyle',
    );
    if (!hasOverrides) {
      return themeBase;
    }
    return (themeBase ?? const TextStyle()).copyWith(
      inherit: _boolean('${prefix}Inherit'),
      color: _resolvedColor(context, '${prefix}Color'),
      backgroundColor: _resolvedColor(context, '${prefix}BackgroundColor'),
      fontSize: _number('${prefix}FontSize'),
      fontWeight: _fontWeight('${prefix}FontWeight'),
      fontStyle: _fontStyle('${prefix}FontStyle'),
      letterSpacing: _number('${prefix}LetterSpacing'),
      wordSpacing: _number('${prefix}WordSpacing'),
      textBaseline: _textBaseline('${prefix}TextBaseline'),
      height: _number('${prefix}Height'),
      leadingDistribution: _textLeadingDistribution(
        '${prefix}LeadingDistribution',
      ),
      locale: _locale(
        languageCode: '${prefix}LocaleLanguageCode',
        scriptCode: '${prefix}LocaleScriptCode',
        countryCode: '${prefix}LocaleCountryCode',
      ),
      foreground: _paint(context, '${prefix}Foreground'),
      background: _paint(context, '${prefix}Background'),
      shadows: _shadows(context, '${prefix}Shadows'),
      fontFeatures: _fontFeatures('${prefix}FontFeatures'),
      fontVariations: _fontVariations('${prefix}FontVariations'),
      decoration: _textDecoration(prefix),
      decorationColor: _resolvedColor(context, '${prefix}DecorationColor'),
      decorationStyle: _textDecorationStyle('${prefix}DecorationStyle'),
      decorationThickness: _number('${prefix}DecorationThickness'),
      debugLabel: _string('${prefix}DebugLabel'),
      fontFamily: _string('${prefix}FontFamily'),
      fontFamilyFallback: _newlineList('${prefix}FontFamilyFallback'),
      package: _string('${prefix}Package'),
      overflow: _textOverflow('${prefix}Overflow'),
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

  Widget _defaultSelectionStyle(BuildContext context) {
    final cursorColor = _resolvedColor(context, 'cursorColor');
    final selectionColor = _resolvedColor(context, 'selectionColor');
    final mouseCursor = _mouseCursor('mouseCursor');
    final child = _single('child')!;
    return _boolean('merge')!
        ? DefaultSelectionStyle.merge(
            cursorColor: cursorColor,
            selectionColor: selectionColor,
            mouseCursor: mouseCursor,
            child: child,
          )
        : DefaultSelectionStyle(
            cursorColor: cursorColor,
            selectionColor: selectionColor,
            mouseCursor: mouseCursor,
            child: child,
          );
  }

  TextDecoration? _textDecoration(String prefix) {
    final names = {
      '${prefix}DecorationUnderline',
      '${prefix}DecorationOverline',
      '${prefix}DecorationLineThrough',
    };
    if (!names.any(node.properties.containsKey)) {
      return null;
    }
    final decorations = <TextDecoration>[
      if (_boolean('${prefix}DecorationUnderline') ?? false)
        TextDecoration.underline,
      if (_boolean('${prefix}DecorationOverline') ?? false)
        TextDecoration.overline,
      if (_boolean('${prefix}DecorationLineThrough') ?? false)
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

  PreferredSizeWidget? _preferredSizeSingle(String name) {
    final child = node.slot(name)?.child;
    return child == null ? null : _view(child);
  }

  List<Widget> _children(String name) => [
    for (final child in node.slot(name)?.children ?? const <CanvasNode>[])
      _view(child),
  ];

  _CanvasNodeView _view(CanvasNode child) => _CanvasNodeView(
    node: child,
    imageResources: imageResources,
    onImageError: onImageError,
    selectedWidgetId: selectedWidgetId,
    onSelected: onSelected,
    nodeKey: nodeKey,
    designerFocusParent: designerFocusParent,
    overlayScale: overlayScale,
    inlineTextEditEnabled: inlineTextEditEnabled,
    inlineTextEditingWidgetId: inlineTextEditingWidgetId,
    onBeginInlineTextEdit: onBeginInlineTextEdit,
    onCommitInlineTextEdit: onCommitInlineTextEdit,
    onCancelInlineTextEdit: onCancelInlineTextEdit,
    suppressDesignerSemantics:
        suppressDesignerSemantics ||
        node.type == 'flutter.widgets.MergeSemantics' ||
        node.type == 'flutter.widgets.IndexedSemantics',
  );

  String? _string(String name) {
    final property = node.properties[name];
    return property?.kind == 'string' ? property!.value as String : null;
  }

  bool? _boolean(String name) {
    final property = node.properties[name];
    return property?.kind == 'boolean' ? property!.value as bool : null;
  }

  bool _callbackPresent(String name) =>
      node.properties[name]?.kind == 'callbackPresence';

  int? _integer(String name) {
    final property = node.properties[name];
    return property?.kind == 'integer' ? property!.value as int : null;
  }

  double? _number(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is num ? value.toDouble() : null;
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

  BlendMode? _optionalBlendMode(String name) {
    final value = _enum(name);
    return value == null ? null : _blendMode(value);
  }

  String? _enum(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is CanvasEnumValue ? value.value : null;
  }

  String? _enumOrString(String name) => _enum(name) ?? _string(name);

  MouseCursor? _mouseCursor(String name) => switch (_enumOrString(name)) {
    'defer' => MouseCursor.defer,
    'uncontrolled' => MouseCursor.uncontrolled,
    'clickable' => WidgetStateMouseCursor.clickable,
    'adaptiveClickable' => WidgetStateMouseCursor.adaptiveClickable,
    'textable' => WidgetStateMouseCursor.textable,
    'none' => SystemMouseCursors.none,
    'basic' => SystemMouseCursors.basic,
    'click' => SystemMouseCursors.click,
    'forbidden' => SystemMouseCursors.forbidden,
    'wait' => SystemMouseCursors.wait,
    'progress' => SystemMouseCursors.progress,
    'contextMenu' => SystemMouseCursors.contextMenu,
    'help' => SystemMouseCursors.help,
    'text' => SystemMouseCursors.text,
    'verticalText' => SystemMouseCursors.verticalText,
    'cell' => SystemMouseCursors.cell,
    'precise' => SystemMouseCursors.precise,
    'move' => SystemMouseCursors.move,
    'grab' => SystemMouseCursors.grab,
    'grabbing' => SystemMouseCursors.grabbing,
    'noDrop' => SystemMouseCursors.noDrop,
    'alias' => SystemMouseCursors.alias,
    'copy' => SystemMouseCursors.copy,
    'disappearing' => SystemMouseCursors.disappearing,
    'allScroll' => SystemMouseCursors.allScroll,
    'resizeLeftRight' => SystemMouseCursors.resizeLeftRight,
    'resizeUpDown' => SystemMouseCursors.resizeUpDown,
    'resizeUpLeftDownRight' => SystemMouseCursors.resizeUpLeftDownRight,
    'resizeUpRightDownLeft' => SystemMouseCursors.resizeUpRightDownLeft,
    'resizeUp' => SystemMouseCursors.resizeUp,
    'resizeDown' => SystemMouseCursors.resizeDown,
    'resizeLeft' => SystemMouseCursors.resizeLeft,
    'resizeRight' => SystemMouseCursors.resizeRight,
    'resizeUpLeft' => SystemMouseCursors.resizeUpLeft,
    'resizeUpRight' => SystemMouseCursors.resizeUpRight,
    'resizeDownLeft' => SystemMouseCursors.resizeDownLeft,
    'resizeDownRight' => SystemMouseCursors.resizeDownRight,
    'resizeColumn' => SystemMouseCursors.resizeColumn,
    'resizeRow' => SystemMouseCursors.resizeRow,
    'zoomIn' => SystemMouseCursors.zoomIn,
    'zoomOut' => SystemMouseCursors.zoomOut,
    _ => null,
  };

  MainAxisAlignment _mainAxisAlignment() =>
      switch (_enum('mainAxisAlignment')) {
        'end' => MainAxisAlignment.end,
        'center' => MainAxisAlignment.center,
        'spaceBetween' => MainAxisAlignment.spaceBetween,
        'spaceAround' => MainAxisAlignment.spaceAround,
        'spaceEvenly' => MainAxisAlignment.spaceEvenly,
        _ => MainAxisAlignment.start,
      };

  MainAxisAlignment? _overflowBarMainAxisAlignment() =>
      switch (_enum('alignment')) {
        'start' => MainAxisAlignment.start,
        'end' => MainAxisAlignment.end,
        'center' => MainAxisAlignment.center,
        'spaceBetween' => MainAxisAlignment.spaceBetween,
        'spaceAround' => MainAxisAlignment.spaceAround,
        'spaceEvenly' => MainAxisAlignment.spaceEvenly,
        _ => null,
      };

  OverflowBarAlignment _overflowBarAlignment() =>
      switch (_enum('overflowAlignment')) {
        'end' => OverflowBarAlignment.end,
        'center' => OverflowBarAlignment.center,
        _ => OverflowBarAlignment.start,
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

  WrapAlignment _wrapAlignment(String name) => switch (_enum(name)) {
    'end' => WrapAlignment.end,
    'center' => WrapAlignment.center,
    'spaceBetween' => WrapAlignment.spaceBetween,
    'spaceAround' => WrapAlignment.spaceAround,
    'spaceEvenly' => WrapAlignment.spaceEvenly,
    _ => WrapAlignment.start,
  };

  WrapCrossAlignment _wrapCrossAxisAlignment() =>
      switch (_enum('crossAxisAlignment')) {
        'end' => WrapCrossAlignment.end,
        'center' => WrapCrossAlignment.center,
        _ => WrapCrossAlignment.start,
      };

  TextDirection? _textDirection() => switch (_enum('textDirection')) {
    'rtl' => TextDirection.rtl,
    'ltr' => TextDirection.ltr,
    _ => null,
  };

  VerticalDirection _verticalDirection() => _enum('verticalDirection') == 'up'
      ? VerticalDirection.up
      : VerticalDirection.down;

  StackFit _stackFit([String name = 'fit']) => switch (_enum(name)) {
    'expand' => StackFit.expand,
    'passthrough' => StackFit.passthrough,
    _ => StackFit.loose,
  };

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

  TextDecorationStyle? _textDecorationStyle(String name) =>
      switch (_enum(name)) {
        'solid' => TextDecorationStyle.solid,
        'double' => TextDecorationStyle.double,
        'dotted' => TextDecorationStyle.dotted,
        'dashed' => TextDecorationStyle.dashed,
        'wavy' => TextDecorationStyle.wavy,
        _ => null,
      };

  Brightness? _brightness(String name) => switch (_enum(name)) {
    'light' => Brightness.light,
    'dark' => Brightness.dark,
    _ => null,
  };

  Clip? _clipBehavior() => switch (_enum('clipBehavior')) {
    'none' => Clip.none,
    'hardEdge' => Clip.hardEdge,
    'antiAlias' => Clip.antiAlias,
    'antiAliasWithSaveLayer' => Clip.antiAliasWithSaveLayer,
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

  TextInputType? _textInputType() => switch (_string('keyboardType')) {
    'text' => TextInputType.text,
    'multiline' => TextInputType.multiline,
    'number' => TextInputType.number,
    'numberSigned' => const TextInputType.numberWithOptions(signed: true),
    'numberDecimal' => const TextInputType.numberWithOptions(decimal: true),
    'numberSignedDecimal' => const TextInputType.numberWithOptions(
      signed: true,
      decimal: true,
    ),
    'phone' => TextInputType.phone,
    'datetime' => TextInputType.datetime,
    'emailAddress' => TextInputType.emailAddress,
    'url' => TextInputType.url,
    'visiblePassword' => TextInputType.visiblePassword,
    'name' => TextInputType.name,
    'streetAddress' => TextInputType.streetAddress,
    'none' => TextInputType.none,
    'webSearch' => TextInputType.webSearch,
    'twitter' => TextInputType.twitter,
    _ => null,
  };

  TextInputAction? _textInputAction() => switch (_enum('textInputAction')) {
    'none' => TextInputAction.none,
    'unspecified' => TextInputAction.unspecified,
    'done' => TextInputAction.done,
    'go' => TextInputAction.go,
    'search' => TextInputAction.search,
    'send' => TextInputAction.send,
    'next' => TextInputAction.next,
    'previous' => TextInputAction.previous,
    'continueAction' => TextInputAction.continueAction,
    'join' => TextInputAction.join,
    'route' => TextInputAction.route,
    'emergencyCall' => TextInputAction.emergencyCall,
    'newline' => TextInputAction.newline,
    _ => null,
  };

  TextCapitalization? _textCapitalization() =>
      switch (_enum('textCapitalization')) {
        'words' => TextCapitalization.words,
        'sentences' => TextCapitalization.sentences,
        'characters' => TextCapitalization.characters,
        'none' => TextCapitalization.none,
        _ => null,
      };

  TextAlignVertical? _textAlignVertical() =>
      switch (_string('textAlignVertical')) {
        'top' => TextAlignVertical.top,
        'center' => TextAlignVertical.center,
        'bottom' => TextAlignVertical.bottom,
        _ => null,
      };

  SmartDashesType? _smartDashesType() => switch (_enum('smartDashesType')) {
    'disabled' => SmartDashesType.disabled,
    'enabled' => SmartDashesType.enabled,
    _ => null,
  };

  SmartQuotesType? _smartQuotesType() => switch (_enum('smartQuotesType')) {
    'disabled' => SmartQuotesType.disabled,
    'enabled' => SmartQuotesType.enabled,
    _ => null,
  };

  MaxLengthEnforcement? _maxLengthEnforcement() =>
      switch (_enum('maxLengthEnforcement')) {
        'none' => MaxLengthEnforcement.none,
        'enforced' => MaxLengthEnforcement.enforced,
        'truncateAfterCompositionEnds' =>
          MaxLengthEnforcement.truncateAfterCompositionEnds,
        _ => null,
      };

  ui.BoxHeightStyle? _boxHeightStyle() => switch (_enum(
    'selectionHeightStyle',
  )) {
    'tight' => ui.BoxHeightStyle.tight,
    'max' => ui.BoxHeightStyle.max,
    'includeLineSpacingMiddle' => ui.BoxHeightStyle.includeLineSpacingMiddle,
    'includeLineSpacingTop' => ui.BoxHeightStyle.includeLineSpacingTop,
    'includeLineSpacingBottom' => ui.BoxHeightStyle.includeLineSpacingBottom,
    'strut' => ui.BoxHeightStyle.strut,
    _ => null,
  };

  ui.BoxWidthStyle? _boxWidthStyle() => switch (_enum('selectionWidthStyle')) {
    'tight' => ui.BoxWidthStyle.tight,
    'max' => ui.BoxWidthStyle.max,
    _ => null,
  };

  DragStartBehavior _dragStartBehavior() => _enum('dragStartBehavior') == 'down'
      ? DragStartBehavior.down
      : DragStartBehavior.start;

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

class _CanvasInlineTextEditor extends StatefulWidget {
  const _CanvasInlineTextEditor({
    required this.widgetId,
    required this.designerFocusParent,
    required this.initialText,
    required this.style,
    required this.strutStyle,
    required this.textAlign,
    required this.textDirection,
    required this.maxLines,
    required this.cursorColor,
    required this.onCommit,
    required this.onCancel,
    super.key,
  });

  final String widgetId;
  final FocusNode designerFocusParent;
  final String initialText;
  final TextStyle? style;
  final StrutStyle? strutStyle;
  final TextAlign textAlign;
  final TextDirection? textDirection;
  final int maxLines;
  final Color cursorColor;
  final bool Function(String, String, bool) onCommit;
  final VoidCallback onCancel;

  @override
  State<_CanvasInlineTextEditor> createState() =>
      _CanvasInlineTextEditorState();
}

class _CanvasInlineTextEditorState extends State<_CanvasInlineTextEditor> {
  late final TextEditingController _controller;
  late final FocusNode _focusNode;
  bool _composing = false;
  bool _compositionObserved = false;
  String? _validationError;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController.fromValue(
      TextEditingValue(
        text: widget.initialText,
        selection: TextSelection.collapsed(offset: widget.initialText.length),
      ),
    )..addListener(_onEditingValueChanged);
    _focusNode = FocusNode(debugLabel: 'canvas-inline-text-${widget.widgetId}');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _focusNode.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_onEditingValueChanged)
      ..dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bindings = <ShortcutActivator, VoidCallback>{
      if (!_composing)
        const SingleActivator(LogicalKeyboardKey.enter, control: true): _commit,
      if (!_composing)
        const SingleActivator(LogicalKeyboardKey.escape): _cancel,
    };
    // This is Designer UI, not an application TextField. Keep its focus branch
    // under the outer Designer surface so application ExcludeFocus still gates
    // every real application descendant without disabling F2 editing.
    return Focus(
      parentNode: widget.designerFocusParent,
      canRequestFocus: false,
      skipTraversal: true,
      includeSemantics: false,
      child: CallbackShortcuts(
        bindings: bindings,
        child: TextField(
          controller: _controller,
          focusNode: _focusNode,
          autofocus: true,
          keyboardType: TextInputType.multiline,
          maxLines: widget.maxLines,
          style: widget.style,
          strutStyle: widget.strutStyle,
          textAlign: widget.textAlign,
          textDirection: widget.textDirection,
          cursorColor: widget.cursorColor,
          decoration: InputDecoration(
            isDense: true,
            contentPadding: EdgeInsets.zero,
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
            errorBorder: InputBorder.none,
            focusedErrorBorder: InputBorder.none,
            errorText: _validationError,
            errorStyle: const TextStyle(fontSize: 10, height: 1),
          ),
        ),
      ),
    );
  }

  void _onEditingValueChanged() {
    final composing = _hasActiveComposition(_controller.value);
    if (composing) {
      _compositionObserved = true;
    }
    final validationError = inlineTextValidationError(_controller.text);
    if ((composing != _composing || validationError != _validationError) &&
        mounted) {
      setState(() {
        _composing = composing;
        _validationError = validationError;
      });
    }
  }

  void _commit() {
    if (_hasActiveComposition(_controller.value)) {
      return;
    }
    if (_validationError != null) {
      return;
    }
    final accepted = widget.onCommit(
      widget.widgetId,
      _controller.text,
      _compositionObserved,
    );
    if (!accepted && mounted) {
      setState(() {
        _validationError =
            'The host rejected this edit because its Canvas authority changed.';
      });
      _focusNode.requestFocus();
    }
  }

  void _cancel() {
    if (_hasActiveComposition(_controller.value)) {
      return;
    }
    widget.onCancel();
  }

  bool _hasActiveComposition(TextEditingValue value) {
    final composing = value.composing;
    return composing.start >= 0 &&
        composing.start < composing.end &&
        composing.end <= value.text.length;
  }
}

bool _ignoresPointersForNode(CanvasNode node) =>
    node.type == 'flutter.widgets.IgnorePointer' &&
    (node.properties['ignoring']?.value as bool? ?? true);

class _ZeroSizedWidgetTargetGroup {
  const _ZeroSizedWidgetTargetGroup({
    required this.rect,
    required this.widgetIds,
    required this.widgetTypes,
    required this.previewUnavailableMessages,
  });

  final Rect rect;
  final List<String> widgetIds;
  final List<String> widgetTypes;
  final List<String?> previewUnavailableMessages;
}

class _ZeroSizedWidgetTarget extends StatelessWidget {
  const _ZeroSizedWidgetTarget({
    required this.widgetIds,
    required this.widgetTypes,
    required this.previewUnavailableMessages,
    required this.selectedWidgetId,
    required this.dark,
    required this.onSelected,
  }) : assert(widgetIds.length == widgetTypes.length),
       assert(widgetIds.length == previewUnavailableMessages.length);

  final List<String> widgetIds;
  final List<String> widgetTypes;
  final List<String?> previewUnavailableMessages;
  final String? selectedWidgetId;
  final bool dark;
  final ValueChanged<String> onSelected;

  bool get _grouped => widgetIds.length > 1;

  bool get _selected =>
      selectedWidgetId != null && widgetIds.contains(selectedWidgetId);

  String get _keySuffix =>
      _grouped ? 'group-${widgetIds.first}' : widgetIds.single;

  String get _cyclingMessage =>
      widgetTypes.every((type) => type == 'flutter.widgets.SizedBox')
      ? '${widgetIds.length} overlapping empty SizedBox widgets. '
            'Activate repeatedly to cycle selection.'
      : '${widgetIds.length} overlapping '
            '${widgetTypes.contains('flutter.widgets.IgnorePointer') ? 'Designer targets' : 'zero-size widgets'}. '
            'Activate repeatedly to cycle selection.';

  String? get _previewUnavailableMessage {
    final selectedIndex = selectedWidgetId == null
        ? -1
        : widgetIds.indexOf(selectedWidgetId!);
    if (selectedIndex >= 0) {
      final selectedMessage = previewUnavailableMessages[selectedIndex];
      if (selectedMessage != null) {
        return selectedMessage;
      }
    }
    for (final message in previewUnavailableMessages) {
      if (message != null) {
        return message;
      }
    }
    return null;
  }

  void _activate() {
    final selectedIndex = selectedWidgetId == null
        ? -1
        : widgetIds.indexOf(selectedWidgetId!);
    final nextIndex = selectedIndex < 0
        ? 0
        : (selectedIndex + 1) % widgetIds.length;
    onSelected(widgetIds[nextIndex]);
  }

  @override
  Widget build(BuildContext context) {
    final previewUnavailableMessage = _previewUnavailableMessage;
    Widget visual = CustomPaint(
      key: ValueKey('canvas-zero-size-widget-outline-$_keySuffix'),
      foregroundPainter: _CanvasWidgetOutlinePainter(
        selected: _selected,
        inflateEmptyLinearContainer: false,
        visualScale: 1,
        unselectedColor: dark
            ? const Color(0x99b0b8c1)
            : const Color(0x9974808a),
      ),
      child: const SizedBox.expand(),
    );
    if (previewUnavailableMessage != null) {
      visual = Stack(
        fit: StackFit.expand,
        children: [
          visual,
          IgnorePointer(
            child: Align(
              alignment: Alignment.topRight,
              child: SizedBox.square(
                dimension: 20,
                child: DecoratedBox(
                  key: ValueKey(
                    'canvas-zero-size-custom-clipper-warning-$_keySuffix',
                  ),
                  decoration: BoxDecoration(
                    color: Colors.amber.shade100,
                    border: Border.all(color: Colors.amber.shade800),
                    borderRadius: BorderRadius.circular(2),
                  ),
                  child: const Center(
                    child: Text(
                      '!',
                      style: TextStyle(
                        color: Colors.black87,
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        height: 1,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      );
    }
    final interaction = MouseRegion(
      cursor: SystemMouseCursors.click,
      child: GestureDetector(
        key: ValueKey('canvas-zero-size-widget-target-$_keySuffix'),
        behavior: HitTestBehavior.opaque,
        onTap: _activate,
        child: visual,
      ),
    );
    final baseLabel = _grouped
        ? _cyclingMessage
        : '${_displayType(widgetTypes.single)} ${widgetIds.single}';
    final semanticsLabel = previewUnavailableMessage == null
        ? baseLabel
        : '$baseLabel. $previewUnavailableMessage';
    final tooltipMessage = switch ((_grouped, previewUnavailableMessage)) {
      (true, final String warning) => '$_cyclingMessage $warning',
      (true, null) => _cyclingMessage,
      (false, final String warning) => warning,
      (false, null) =>
        widgetTypes.single == 'flutter.widgets.IgnorePointer'
            ? 'IgnorePointer Designer selection handle. '
                  'The widget body keeps the configured pointer behavior.'
            : null,
    };
    return Semantics(
      key: ValueKey('canvas-zero-size-widget-semantics-$_keySuffix'),
      label: semanticsLabel,
      selected: _selected,
      child: tooltipMessage == null
          ? interaction
          : Tooltip(
              message: tooltipMessage,
              excludeFromSemantics: true,
              child: interaction,
            ),
    );
  }
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

class _CanvasContainerInsetsGuidesPainter extends CustomPainter {
  const _CanvasContainerInsetsGuidesPainter({
    required this.padding,
    required this.margin,
    required this.visualScale,
    required this.paddingColor,
    required this.marginColor,
  });

  final EdgeInsets padding;
  final EdgeInsets margin;
  final double visualScale;
  final Color paddingColor;
  final Color marginColor;

  double get debugStrokeWidth => 1 / math.max(visualScale, 0.000001);

  double get debugCapLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    if ((size.width <= 0 && size.height <= 0) ||
        (padding == EdgeInsets.zero && margin == EdgeInsets.zero)) {
      return;
    }
    final layoutBounds = Offset.zero & size;
    final decorationBounds = _designerDeflateRect(layoutBounds, margin);
    final contentBounds = _designerDeflateRect(decorationBounds, padding);
    final marginPaint = Paint()
      ..color = marginColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final paddingPaint = Paint()
      ..color = paddingColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final safeScale = math.max(visualScale, 0.000001);
    _drawInsetsMeasurements(
      canvas,
      layoutBounds,
      decorationBounds,
      marginPaint,
      debugCapLength,
      dashLength: 3 / safeScale,
      gapLength: 2 / safeScale,
    );
    _drawInsetsMeasurements(
      canvas,
      decorationBounds,
      contentBounds,
      paddingPaint,
      debugCapLength,
    );
  }

  @override
  bool shouldRepaint(_CanvasContainerInsetsGuidesPainter oldDelegate) =>
      padding != oldDelegate.padding ||
      margin != oldDelegate.margin ||
      visualScale != oldDelegate.visualScale ||
      paddingColor != oldDelegate.paddingColor ||
      marginColor != oldDelegate.marginColor;
}

Rect _designerDeflateRect(Rect outer, EdgeInsets insets) {
  final left = outer.left + insets.left;
  final top = outer.top + insets.top;
  final right = outer.right - insets.right;
  final bottom = outer.bottom - insets.bottom;
  final horizontalMiddle = (left + right) / 2;
  final verticalMiddle = (top + bottom) / 2;
  return Rect.fromLTRB(
    math.min(left, horizontalMiddle),
    math.min(top, verticalMiddle),
    math.max(right, horizontalMiddle),
    math.max(bottom, verticalMiddle),
  );
}

void _drawInsetsMeasurements(
  Canvas canvas,
  Rect outer,
  Rect inner,
  Paint paint,
  double capLength, {
  double? dashLength,
  double? gapLength,
}) {
  final guideX = (inner.left + inner.right) / 2;
  final guideY = (inner.top + inner.bottom) / 2;
  void draw(Offset start, Offset end) {
    if (dashLength == null || gapLength == null) {
      _drawMeasurement(canvas, start, end, paint, capLength);
    } else {
      _drawDashedMeasurement(
        canvas,
        start,
        end,
        paint,
        capLength,
        dashLength,
        gapLength,
      );
    }
  }

  draw(Offset(outer.left, guideY), Offset(inner.left, guideY));
  draw(Offset(inner.right, guideY), Offset(outer.right, guideY));
  draw(Offset(guideX, outer.top), Offset(guideX, inner.top));
  draw(Offset(guideX, inner.bottom), Offset(guideX, outer.bottom));
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

void _drawDashedMeasurement(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double capLength,
  double dashLength,
  double gapLength,
) {
  final delta = end - start;
  if (delta.distance <= 0.000001) {
    return;
  }
  _drawDashedLine(canvas, start, end, paint, dashLength, gapLength);
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
  final roleTheme = base.copyWith(
    textTheme: _applyTextThemeOverrides(
      base.textTheme,
      colorScheme,
      profile.theme.textTheme,
    ),
    platform: canvasAdaptiveTargetPlatform(profile.targetPlatform),
  );
  return _applyComponentColorOverrides(
    roleTheme,
    colorScheme,
    profile.theme.components,
  );
}

ThemeData _applyComponentColorOverrides(
  ThemeData base,
  ColorScheme colorScheme,
  Map<String, CanvasThemeColorValue> values,
) {
  Color? color(String role) => _themeOverrideColor(values[role], colorScheme);
  final hasAppBar = values.keys.any((key) => key.startsWith('appBar.'));
  final hasIcon = values.containsKey('icon.color');
  final hasElevated = values.keys.any(
    (key) => key.startsWith('elevatedButton.'),
  );
  return base.copyWith(
    scaffoldBackgroundColor: color('scaffold.backgroundColor'),
    appBarTheme: hasAppBar
        ? base.appBarTheme.copyWith(
            backgroundColor: color('appBar.backgroundColor'),
            foregroundColor: color('appBar.foregroundColor'),
            shadowColor: color('appBar.shadowColor'),
            surfaceTintColor: color('appBar.surfaceTintColor'),
          )
        : null,
    iconTheme: hasIcon
        ? base.iconTheme.copyWith(color: color('icon.color'))
        : null,
    elevatedButtonTheme: hasElevated
        ? ElevatedButtonThemeData(
            style: (base.elevatedButtonTheme.style ?? const ButtonStyle())
                .copyWith(
                  backgroundColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'backgroundColor',
                  ),
                  foregroundColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'foregroundColor',
                  ),
                  overlayColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'overlayColor',
                  ),
                  shadowColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'shadowColor',
                  ),
                  surfaceTintColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'surfaceTintColor',
                  ),
                  iconColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'iconColor',
                  ),
                ),
          )
        : null,
  );
}

WidgetStateProperty<Color?>? _componentButtonColor(
  ColorScheme colorScheme,
  Map<String, CanvasThemeColorValue> values,
  String property,
) {
  CanvasThemeColorValue? value(String state) =>
      values['elevatedButton.$property.$state'];
  if (const [
    'default',
    'disabled',
    'pressed',
    'hovered',
    'focused',
  ].every((state) => value(state) == null)) {
    return null;
  }
  return WidgetStateProperty.resolveWith<Color?>((states) {
    if (states.contains(WidgetState.disabled)) {
      return _themeOverrideColor(value('disabled'), colorScheme);
    }
    for (final entry in const <(WidgetState, String)>[
      (WidgetState.pressed, 'pressed'),
      (WidgetState.hovered, 'hovered'),
      (WidgetState.focused, 'focused'),
    ]) {
      final configured = value(entry.$2);
      if (states.contains(entry.$1) && configured != null) {
        return _themeOverrideColor(configured, colorScheme);
      }
    }
    return _themeOverrideColor(value('default'), colorScheme);
  });
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
