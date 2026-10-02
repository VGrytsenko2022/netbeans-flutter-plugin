import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';

import 'src/canvas_multi_view.dart';
import 'src/canvas_runtime.dart';
import 'src/canvas_view.dart';
import 'src/canvas_web_transport.dart';
import 'src/canvas_web_transport_core.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  CanvasWebTransport? transport;

  void diagnostic(String message) {
    transport?.diagnostic(message);
  }

  runZonedGuarded<void>(
    () {
      FlutterError.onError = (details) {
        diagnostic(
          'Flutter Web Canvas framework failure: ${details.exception}',
        );
      };
      PlatformDispatcher.instance.onError = (error, stack) {
        diagnostic('Flutter Web Canvas asynchronous failure: $error');
        return true;
      };

      try {
        transport = CanvasWebTransport.open(
          JavaScriptCanvasWebBridgeAdapter.connect(),
        );
      } on Object catch (error) {
        runWidget(
          CanvasMultiViewApp(
            viewBuilder: (_) => _UnavailableWebCanvas(reason: '$error'),
          ),
        );
        return;
      }

      final runtime = CanvasRuntimeController(
        input: transport!.input,
        output: transport!.output,
        flush: transport!.flush,
        diagnostic: diagnostic,
        hostProfile: CanvasRuntimeHostProfile.webView,
      );
      runWidget(
        CanvasMultiViewApp(
          viewBuilder: (_) => NativeCanvasApp(runtime: runtime),
        ),
      );
      unawaited(runtime.start().whenComplete(transport!.close));
    },
    (error, stack) {
      diagnostic('Flutter Web Canvas uncaught failure: $error');
    },
    zoneSpecification: protocolOnlyStdoutZone(diagnostic),
  );
}

class _UnavailableWebCanvas extends StatelessWidget {
  const _UnavailableWebCanvas({required this.reason});

  final String reason;

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    home: ColoredBox(
      color: const Color(0xff202224),
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'Flutter Web Canvas is unavailable.\n$reason',
            textAlign: TextAlign.center,
            style: const TextStyle(color: Color(0xffe6e6e6)),
          ),
        ),
      ),
    ),
  );
}
