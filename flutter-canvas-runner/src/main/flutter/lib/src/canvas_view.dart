import 'package:flutter/material.dart';

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
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;

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
      ),
    );
  }
}

class CanvasDocumentView extends StatelessWidget {
  const CanvasDocumentView({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    final profile = model.profile;
    final viewport = Size(profile.logicalWidth, profile.logicalHeight);
    final dark = profile.brightness == 'dark';
    return Scaffold(
      backgroundColor: dark ? const Color(0xff202124) : const Color(0xffe7e9ed),
      body: LayoutBuilder(
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
          return Center(
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
                      textScaler: TextScaler.linear(profile.textScaleFactor),
                      platformBrightness: dark
                          ? Brightness.dark
                          : Brightness.light,
                    ),
                    child: ClipRect(
                      child: _CanvasNodeView(
                        node: model.root,
                        selectedWidgetId: selectedWidgetId,
                        onSelected: onSelected,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _CanvasNodeView extends StatelessWidget {
  const _CanvasNodeView({
    required this.node,
    required this.selectedWidgetId,
    required this.onSelected,
  });

  final CanvasNode node;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;

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
        child: GestureDetector(
          key: ValueKey('canvas-widget-${node.id}'),
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
