import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';

import 'src/canvas_runtime.dart';
import 'src/canvas_view.dart';

void main() {
  void diagnostic(String message) => stderr.writeln(message);
  runZonedGuarded<void>(
    () {
      WidgetsFlutterBinding.ensureInitialized();
      FlutterError.onError = (details) {
        diagnostic(
          'Native Flutter Canvas framework failure: ${details.exception}',
        );
      };
      PlatformDispatcher.instance.onError = (error, stack) {
        diagnostic('Native Flutter Canvas asynchronous failure: $error');
        return true;
      };

      final runtime = CanvasRuntimeController();
      runApp(NativeCanvasApp(runtime: runtime));
      unawaited(runtime.start());
    },
    (error, stack) {
      diagnostic('Native Flutter Canvas uncaught failure: $error');
    },
    zoneSpecification: protocolOnlyStdoutZone(diagnostic),
  );
}
