package dev.flutter.netbeans.plugin.tooling;

import java.util.concurrent.Future;

/** Injectable boundary around NetBeans process execution. */
interface FlutterExecutionBackend {
    Future<Integer> start(FlutterExecutionRequest request);
}
