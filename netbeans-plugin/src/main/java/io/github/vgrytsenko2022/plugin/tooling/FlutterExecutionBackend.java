package io.github.vgrytsenko2022.plugin.tooling;

/** Injectable boundary around NetBeans process execution. */
interface FlutterExecutionBackend {
    FlutterExecutionHandle start(FlutterExecutionRequest request);
}
