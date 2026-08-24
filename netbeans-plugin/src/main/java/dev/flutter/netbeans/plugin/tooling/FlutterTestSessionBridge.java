package dev.flutter.netbeans.plugin.tooling;

/** Streaming boundary between the process backend and NetBeans Test Results. */
public interface FlutterTestSessionBridge {
    void standardOutput(String line);

    void standardError(String line);

    void finish(int exitCode, boolean cancelled, Throwable failure);

    FlutterTestSessionBridge NONE = new FlutterTestSessionBridge() {
        @Override
        public void standardOutput(String line) {
        }

        @Override
        public void standardError(String line) {
        }

        @Override
        public void finish(int exitCode, boolean cancelled, Throwable failure) {
        }
    };
}
