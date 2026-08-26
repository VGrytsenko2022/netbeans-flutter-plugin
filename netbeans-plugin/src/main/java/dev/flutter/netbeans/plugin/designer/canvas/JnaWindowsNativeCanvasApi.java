package dev.flutter.netbeans.plugin.designer.canvas;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;

/** JNA implementation of the deliberately small Win32 Canvas host boundary. */
final class JnaWindowsNativeCanvasApi implements WindowsNativeCanvasApi {
    private static final int SW_HIDE = 0;
    private static final int SW_SHOW = 5;

    private final User32 user32;

    JnaWindowsNativeCanvasApi() {
        this(User32.INSTANCE);
    }

    JnaWindowsNativeCanvasApi(User32 user32) {
        this.user32 = user32;
    }

    @Override
    public boolean isComponentDisplayable(Canvas component) {
        return component.isDisplayable();
    }

    @Override
    public long componentWindow(Canvas component) {
        Pointer pointer = Native.getComponentPointer(component);
        long window = pointer == null ? 0 : Pointer.nativeValue(pointer);
        if (window == 0) {
            throw new IllegalStateException("The AWT Canvas has no native HWND");
        }
        return window;
    }

    @Override
    public boolean isWindow(long window) {
        return window != 0 && user32.IsWindow(hwnd(window));
    }

    @Override
    public long parentWindow(long window) {
        HWND parent = user32.GetParent(hwnd(window));
        return value(parent);
    }

    @Override
    public long ownerProcessId(long window) {
        IntByReference processId = new IntByReference();
        user32.GetWindowThreadProcessId(hwnd(window), processId);
        return Integer.toUnsignedLong(processId.getValue());
    }

    @Override
    public boolean hasChildStyle(long window) {
        int style = user32.GetWindowLong(hwnd(window), WinUser.GWL_STYLE);
        return (style & WinUser.WS_CHILD) != 0;
    }

    @Override
    public String windowClass(long window) {
        char[] name = new char[256];
        int length = user32.GetClassName(hwnd(window), name, name.length);
        if (length <= 0) {
            throw new IllegalStateException("GetClassName failed for a native Canvas HWND");
        }
        return new String(name, 0, length);
    }

    @Override
    public List<Long> descendantWindows(long parentWindow) {
        List<Long> descendants = new ArrayList<>();
        user32.EnumChildWindows(
                hwnd(parentWindow),
                (window, ignored) -> {
                    descendants.add(value(window));
                    return true;
                },
                null);
        return List.copyOf(descendants);
    }

    @Override
    public NativeCanvasWindowBounds clientBounds(long window) {
        RECT rect = new RECT();
        if (!user32.GetClientRect(hwnd(window), rect)) {
            throw new IllegalStateException("GetClientRect failed for the native Canvas HWND");
        }
        return new NativeCanvasWindowBounds(
                Math.max(0, rect.right - rect.left),
                Math.max(0, rect.bottom - rect.top));
    }

    @Override
    public boolean moveWindow(long window, NativeCanvasWindowBounds bounds) {
        return user32.MoveWindow(
                hwnd(window),
                0,
                0,
                Math.max(1, bounds.width()),
                Math.max(1, bounds.height()),
                true);
    }

    @Override
    public void showWindow(long window, boolean visible) {
        // ShowWindow returns the previous visibility, not operation success.
        user32.ShowWindow(hwnd(window), visible ? SW_SHOW : SW_HIDE);
    }

    private static HWND hwnd(long value) {
        return new HWND(new Pointer(value));
    }

    private static long value(HWND window) {
        return window == null || window.getPointer() == null
                ? 0
                : Pointer.nativeValue(window.getPointer());
    }
}
