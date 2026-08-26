package dev.flutter.netbeans.plugin.designer.canvas;

/** Physical client-area bounds used at the native child-window boundary. */
record NativeCanvasWindowBounds(int width, int height) {
    NativeCanvasWindowBounds {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Native window dimensions cannot be negative");
        }
    }
}
