import 'dart:ui' show FlutterView;

import 'package:flutter/widgets.dart';

/// Hosts one independent widget root for every browser-managed Flutter view.
class CanvasMultiViewApp extends StatefulWidget {
  const CanvasMultiViewApp({required this.viewBuilder, super.key});

  final WidgetBuilder viewBuilder;

  @override
  State<CanvasMultiViewApp> createState() => _CanvasMultiViewAppState();
}

class _CanvasMultiViewAppState extends State<CanvasMultiViewApp>
    with WidgetsBindingObserver {
  Map<Object, Widget> _views = <Object, Widget>{};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _updateViews();
  }

  @override
  void didUpdateWidget(CanvasMultiViewApp oldWidget) {
    super.didUpdateWidget(oldWidget);
    _views.clear();
    _updateViews();
  }

  @override
  void didChangeMetrics() => _updateViews();

  void _updateViews() {
    final updated = <Object, Widget>{};
    for (final FlutterView view
        in WidgetsBinding.instance.platformDispatcher.views) {
      updated[view.viewId] =
          _views[view.viewId] ??
          View(
            view: view,
            child: Builder(builder: widget.viewBuilder),
          );
    }
    setState(() => _views = updated);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) =>
      ViewCollection(views: _views.values.toList(growable: false));
}
