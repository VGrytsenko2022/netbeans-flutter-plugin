import 'package:flutter/material.dart';

import 'canvas_drop.dart';
import 'canvas_model.dart';
import 'canvas_runtime.dart';

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
    this.dropHoverTarget,
    this.onDropResolverChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
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
    this.dropHoverTarget,
    this.onDropResolverChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final CanvasDropTarget? dropHoverTarget;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;

  @override
  State<CanvasDocumentView> createState() => _CanvasDocumentViewState();
}

class _CanvasDocumentViewState extends State<CanvasDocumentView> {
  static const int _microsPerSurface = 1000000;
  static const double _minimumTerminalBand = 36;

  final GlobalKey _surfaceKey = GlobalKey();
  final Map<String, GlobalKey> _nodeKeys = <String, GlobalKey>{};

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
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final profile = widget.model.profile;
    final viewport = Size(profile.logicalWidth, profile.logicalHeight);
    final dark = profile.brightness == 'dark';
    return Scaffold(
      backgroundColor: dark ? const Color(0xff202124) : const Color(0xffe7e9ed),
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
                          color: dark ? const Color(0xff121212) : Colors.white,
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
                              onSelected: widget.onSelected,
                              nodeKey: _nodeKey,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                if (widget.dropHoverTarget?.zone case final zone?)
                  _DropZoneOverlay(zone: zone, constraints: constraints),
              ],
            );
          },
        ),
      ),
    );
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
    _collectDropCandidates(widget.model.root, point, 0, candidates);
    if (candidates.isEmpty) {
      return null;
    }
    candidates.sort((left, right) {
      final depth = right.depth.compareTo(left.depth);
      return depth != 0 ? depth : left.area.compareTo(right.area);
    });
    final selected = candidates.first;
    return CanvasDropTarget(
      parentWidgetId: selected.node.id,
      slotName: 'children',
      insertionIndex: selected.childCount,
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
    int depth,
    List<_DropCandidate> result,
  ) {
    if ((node.type == 'flutter.widgets.Column' ||
            node.type == 'flutter.widgets.Row') &&
        (node.slot('children')?.children.length ?? 0) < 10000) {
      final box = _renderBox(_nodeKeys[node.id]);
      if (box != null) {
        final rect = _globalRect(box);
        final terminal = _terminalZone(node, rect);
        if (terminal.contains(point)) {
          result.add(
            _DropCandidate(
              node,
              depth,
              rect.width * rect.height,
              node.slot('children')?.children.length ?? 0,
              terminal,
            ),
          );
        }
      }
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        _collectDropCandidates(child, point, depth + 1, result);
      }
    }
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
    this.childCount,
    this.zone,
  );

  final CanvasNode node;
  final int depth;
  final double area;
  final int childCount;
  final Rect zone;
}

class _DropZoneOverlay extends StatelessWidget {
  const _DropZoneOverlay({required this.zone, required this.constraints});

  final CanvasDropZone zone;
  final BoxConstraints constraints;

  @override
  Widget build(BuildContext context) {
    const denominator = 1000000.0;
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
          label: 'Text append drop zone',
          child: DecoratedBox(
            key: const ValueKey('canvas-text-append-drop-zone'),
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
      'flutter.widgets.Text' => _text(),
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
    final value = node.properties['padding']?.value;
    final insets = value is CanvasEdgeInsets
        ? EdgeInsets.fromLTRB(value.left, value.top, value.right, value.bottom)
        : const EdgeInsets.all(16);
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

  Widget _text() => Text(
    _string('data') ?? 'Text',
    textAlign: _textAlign(),
    textDirection: _textDirection(),
    softWrap: _boolean('softWrap'),
    overflow: _textOverflow(),
    maxLines: _integer('maxLines'),
    semanticsLabel: _string('semanticsLabel'),
    semanticsIdentifier: _string('semanticsIdentifier'),
    textWidthBasis: _textWidthBasis(),
    selectionColor: _color('selectionColor'),
  );

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

  TextBaseline? _textBaseline() => switch (_enum('textBaseline')) {
    'alphabetic' => TextBaseline.alphabetic,
    'ideographic' => TextBaseline.ideographic,
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

  TextOverflow? _textOverflow() => switch (_enum('overflow')) {
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

ThemeData _theme(CanvasProfile profile, Brightness brightness) => ThemeData(
  brightness: brightness,
  platform: canvasAdaptiveTargetPlatform(profile.targetPlatform),
  colorScheme: ColorScheme.fromSeed(
    seedColor: const Color(0xff6750a4),
    brightness: brightness,
  ),
);

/// Resolves Flutter adaptive widget semantics inside the native desktop engine.
///
/// Web has no [TargetPlatform] value. The Windows-hosted runner therefore uses
/// Windows appearance only as an explicit fallback; the NetBeans edge does not
/// present this fallback as a real browser Canvas backend.
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
