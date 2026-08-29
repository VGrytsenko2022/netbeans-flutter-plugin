import 'dart:math' as math;

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
          onDropResolverChanged: runtime.setDropResolver,
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
    this.onDropResolverChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final bool Function() onDeleteSelected;
  final CanvasDropTarget? dropHoverTarget;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;

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
        onDropResolverChanged: onDropResolverChanged,
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
    this.onDropResolverChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final bool Function() onDeleteSelected;
  final CanvasDropTarget? dropHoverTarget;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;

  @override
  State<CanvasDocumentView> createState() => _CanvasDocumentViewState();
}

class _CanvasDocumentViewState extends State<CanvasDocumentView> {
  static const int _microsPerSurface = 1000000;
  static const double _minimumTerminalBand = 36;
  static const double _scaffoldFabDropExtent = 72;

  final GlobalKey _surfaceKey = GlobalKey();
  final Map<String, GlobalKey> _nodeKeys = <String, GlobalKey>{};
  final FocusNode _focusNode = FocusNode(debugLabel: 'native-canvas');

  @override
  void initState() {
    super.initState();
    widget.onDropResolverChanged?.call(_resolveDrop);
  }

  @override
  void didUpdateWidget(CanvasDocumentView oldWidget) {
    super.didUpdateWidget(oldWidget);
    _nodeKeys.removeWhere((id, _) => !widget.model.widgetIds.contains(id));
    if (oldWidget.onDropResolverChanged != widget.onDropResolverChanged) {
      oldWidget.onDropResolverChanged?.call(null);
      widget.onDropResolverChanged?.call(_resolveDrop);
    }
  }

  @override
  void dispose() {
    widget.onDropResolverChanged?.call(null);
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final profile = widget.model.profile;
    final viewport = Size(profile.logicalWidth, profile.logicalHeight);
    final dark = profile.brightness == 'dark';
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
              final availableWidth = (constraints.maxWidth - 32).clamp(
                1.0,
                double.infinity,
              );
              final availableHeight = (constraints.maxHeight - 32).clamp(
                1.0,
                double.infinity,
              );
              final scale = [
                1.0,
                availableWidth / viewport.width,
                availableHeight / viewport.height,
              ].reduce((left, right) => left < right ? left : right);
              return Stack(
                children: [
                  Center(
                    child: SizedBox(
                      width: viewport.width * scale,
                      height: viewport.height * scale,
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
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  if (widget.dropHoverTarget case final target?
                      when target.zone != null)
                    _DropZoneOverlay(target: target, constraints: constraints),
                ],
              );
            },
          ),
        ),
      ),
    );
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
      final band = _minimumTerminalBand.clamp(
        1.0,
        node.type == 'flutter.widgets.Column' ? parent.height : parent.width,
      );
      if (node.type == 'flutter.widgets.Column') {
        return _enumValue(node, 'verticalDirection') == 'up'
            ? Rect.fromLTRB(
                parent.left,
                parent.top,
                parent.right,
                parent.top + band,
              )
            : Rect.fromLTRB(
                parent.left,
                parent.bottom - band,
                parent.right,
                parent.bottom,
              );
      }
      return _enumValue(node, 'textDirection') == 'rtl'
          ? Rect.fromLTRB(
              parent.left,
              parent.top,
              parent.left + band,
              parent.bottom,
            )
          : Rect.fromLTRB(
              parent.right - band,
              parent.top,
              parent.right,
              parent.bottom,
            );
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

class _DropZoneOverlay extends StatelessWidget {
  const _DropZoneOverlay({required this.target, required this.constraints});

  final CanvasDropTarget target;
  final BoxConstraints constraints;

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
    return Positioned(
      left: left,
      top: top,
      width: width,
      height: height,
      child: IgnorePointer(
        child: Semantics(
          label: 'Flutter widget insertion target for ${target.slotName}',
          child: DecoratedBox(
            key: const ValueKey('canvas-widget-insert-drop-zone'),
            decoration: BoxDecoration(
              color: const Color(0x261A73E8),
              border: Border.all(color: const Color(0xff1a73e8), width: 2),
              borderRadius: BorderRadius.circular(4),
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
  });

  final CanvasNode node;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final GlobalKey Function(String id) nodeKey;

  @override
  Widget build(BuildContext context) {
    final child = switch (node.type) {
      'flutter.material.Scaffold' => _scaffold(),
      'flutter.widgets.Column' => _column(),
      'flutter.widgets.Row' => _row(),
      'flutter.widgets.Padding' => _padding(),
      'flutter.widgets.Center' => _center(),
      'flutter.widgets.Text' => _text(context),
      _ => const SizedBox.shrink(),
    };
    final selected = selectedWidgetId == node.id;
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
            child: DecoratedBox(
              position: DecorationPosition.foreground,
              decoration: selected
                  ? BoxDecoration(
                      border: Border.all(
                        color: const Color(0xff1a73e8),
                        width: 2,
                      ),
                    )
                  : const BoxDecoration(),
              child: child,
            ),
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

  Widget _padding() {
    final value = node.properties['padding']!.value as CanvasEdgeInsets;
    final insets = EdgeInsets.fromLTRB(
      value.left,
      value.top,
      value.right,
      value.bottom,
    );
    return Padding(
      padding: insets,
      child: _single('child') ?? const SizedBox.shrink(),
    );
  }

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
