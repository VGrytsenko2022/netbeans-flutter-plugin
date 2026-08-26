package dev.flutter.netbeans.plugin.tooling;

/** Injectable boundary around NetBeans process execution. */
interface FlutterExecutionBackend {
    FlutterExecutionHandle start(FlutterExecutionRequest request);
}
