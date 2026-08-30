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

    /** Returns the native DPI for the exact host HWND. */
    int windowDpi(long window);

    /**
     * Queues a bounds update on the thread that owns {@code window} without
     * waiting for that thread to process Flutter's resize and paint messages.
     */
    boolean setWindowBoundsAsync(long window, NativeCanvasWindowBounds bounds);

    void showWindow(long window, boolean visible);

    /**
     * Transfers keyboard focus to the exact verified attachment identity.
     * The native boundary must revalidate that identity immediately around the
     * focus operation instead of trusting a raw, potentially reused HWND.
     */
    FocusResult requestFocus(NativeCanvasAttachment attachment);

    /**
     * Transfers keyboard focus from the exact verified runner surface to its
     * exact JVM-owned native parent HWND.
     */
    FocusResult releaseFocus(NativeCanvasAttachment attachment);

    /** Reports exact focus without mutating either GUI thread's input queue. */
    boolean isFocused(NativeCanvasAttachment attachment);

    enum FocusResult {
        FOCUSED,
        POLICY_REFUSED,
        TARGET_INVALID,
        INPUT_QUEUE_DETACH_FAILED
    }
}
