package dev.flutter.netbeans.plugin.designer.canvas;

import java.awt.Canvas;
import java.util.List;

/** Narrow Win32 boundary kept injectable for deterministic host tests. */
interface WindowsNativeCanvasApi {
    boolean isComponentDisplayable(Canvas component);

    long componentWindow(Canvas component);

    boolean isWindow(long window);

    long parentWindow(long window);

    long ownerProcessId(long window);

    boolean hasChildStyle(long window);

    String windowClass(long window);

    List<Long> descendantWindows(long parentWindow);

    NativeCanvasWindowBounds clientBounds(long window);

    /**
     * Queues a bounds update on the thread that owns {@code window} without
     * waiting for that thread to process Flutter's resize and paint messages.
     */
    boolean setWindowBoundsAsync(long window, NativeCanvasWindowBounds bounds);

    void showWindow(long window, boolean visible);
}
