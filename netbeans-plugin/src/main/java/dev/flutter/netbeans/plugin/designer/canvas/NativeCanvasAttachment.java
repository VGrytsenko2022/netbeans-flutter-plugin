package dev.flutter.netbeans.plugin.designer.canvas;

/** Verified Win32 identity for one native Flutter runner surface. */
record NativeCanvasAttachment(
        long parentWindow,
        long runnerWindow,
        long flutterViewWindow,
        long runnerProcessId) {
    NativeCanvasAttachment {
        if (parentWindow == 0 || runnerWindow == 0 || flutterViewWindow == 0
                || runnerProcessId <= 0) {
            throw new IllegalArgumentException("Native attachment identities must be positive");
        }
    }
}
