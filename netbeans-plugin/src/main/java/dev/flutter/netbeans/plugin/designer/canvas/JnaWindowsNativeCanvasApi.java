package dev.flutter.netbeans.plugin.designer.canvas;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.GUITHREADINFO;
import com.sun.jna.ptr.IntByReference;
import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;

/** JNA implementation of the deliberately small Win32 Canvas host boundary. */
final class JnaWindowsNativeCanvasApi implements WindowsNativeCanvasApi {
    private static final String RUNNER_WINDOW_CLASS = "FLUTTER_RUNNER_WIN32_WINDOW";
    private static final String FLUTTER_VIEW_WINDOW_CLASS = "FLUTTERVIEW";
    private static final int SW_HIDE = 0;
    private static final int SW_SHOWNOACTIVATE = 4;
    private static final int ASYNC_RESIZE_FLAGS = WinUser.SWP_ASYNCWINDOWPOS
            | WinUser.SWP_NOACTIVATE
            | WinUser.SWP_NOZORDER;

    private final User32 user32;
    private final Kernel32 kernel32;
    private final WindowsDpiApi dpiApi;

    JnaWindowsNativeCanvasApi() {
        this(User32.INSTANCE, Kernel32.INSTANCE, WindowsDpiApi.system());
    }

    JnaWindowsNativeCanvasApi(User32 user32) {
        // Deterministic seam used by the narrow SetWindowPos test. Production
        // always uses the no-argument constructor with the real DPI/focus APIs.
        this(user32, null, ignored -> 96);
    }

    JnaWindowsNativeCanvasApi(
            User32 user32,
            Kernel32 kernel32,
            WindowsDpiApi dpiApi) {
        this.user32 = user32;
        this.kernel32 = kernel32;
        this.dpiApi = dpiApi;
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
    public int windowDpi(long window) {
        int dpi = dpiApi.getDpiForWindow(hwnd(window));
        if (dpi <= 0) {
            throw new IllegalStateException("GetDpiForWindow failed for the Canvas HWND");
        }
        return dpi;
    }

    @Override
    public boolean setWindowBoundsAsync(long window, NativeCanvasWindowBounds bounds) {
        return user32.SetWindowPos(
                hwnd(window),
                null,
                0,
                0,
                Math.max(1, bounds.width()),
                Math.max(1, bounds.height()),
                ASYNC_RESIZE_FLAGS);
    }

    @Override
    public void showWindow(long window, boolean visible) {
        // ShowWindow returns the previous visibility, not operation success.
        // Visibility is not focus intent: a late native attachment must not
        // override a newer Swing interaction. Explicit focus is transferred
        // separately through the revalidated requestFocus contract.
        user32.ShowWindow(
                hwnd(window),
                visible ? SW_SHOWNOACTIVATE : SW_HIDE);
    }

    @Override
    public FocusResult requestFocus(NativeCanvasAttachment attachment) {
        if (kernel32 == null) {
            throw new IllegalStateException(
                    "The injected Windows API has no focus-thread provider");
        }
        long parentProcessId = ProcessHandle.current().pid();
        if (!isExpectedFocusTarget(attachment, parentProcessId)) {
            return FocusResult.TARGET_INVALID;
        }
        long parentWindow = attachment.parentWindow();
        long targetWindow = attachment.flutterViewWindow();
        HWND parent = hwnd(parentWindow);
        HWND target = hwnd(targetWindow);
        IntByReference parentOwnerProcessId = new IntByReference();
        int parentThread = user32.GetWindowThreadProcessId(
                parent, parentOwnerProcessId);
        IntByReference targetOwnerProcessId = new IntByReference();
        int targetThread = user32.GetWindowThreadProcessId(
                target, targetOwnerProcessId);
        if (parentThread == 0
                || targetThread == 0
                || Integer.toUnsignedLong(parentOwnerProcessId.getValue())
                        != parentProcessId
                || Integer.toUnsignedLong(targetOwnerProcessId.getValue())
                        != attachment.runnerProcessId()) {
            return FocusResult.TARGET_INVALID;
        }
        long focusedWindow = foregroundFocusedWindow();
        if (focusedWindow == targetWindow) {
            return FocusResult.FOCUSED;
        }
        if (!isAuthorizedFocusSource(
                focusedWindow, parentProcessId, attachment.runnerProcessId())) {
            return FocusResult.POLICY_REFUSED;
        }

        int currentThread = kernel32.GetCurrentThreadId();
        boolean parentInputAttached = false;
        boolean targetInputAttached = false;
        FocusResult result = FocusResult.POLICY_REFUSED;
        Throwable focusFailure = null;
        boolean detachedInput = true;
        try {
            boolean inputQueuesReady = true;
            if (currentThread != parentThread) {
                parentInputAttached = user32.AttachThreadInput(
                        new DWORD(Integer.toUnsignedLong(currentThread)),
                        new DWORD(Integer.toUnsignedLong(parentThread)),
                        true);
                inputQueuesReady = parentInputAttached;
            }
            if (inputQueuesReady
                    && currentThread != targetThread
                    && targetThread != parentThread) {
                targetInputAttached = user32.AttachThreadInput(
                        new DWORD(Integer.toUnsignedLong(currentThread)),
                        new DWORD(Integer.toUnsignedLong(targetThread)),
                        true);
                inputQueuesReady = targetInputAttached;
            }
            if (inputQueuesReady) {
                if (!isExpectedFocusTarget(
                        attachment,
                        parentProcessId,
                        parentThread,
                        targetThread)) {
                    result = FocusResult.TARGET_INVALID;
                } else if (!isAuthorizedFocusSource(
                        foregroundFocusedWindow(),
                        parentProcessId,
                        attachment.runnerProcessId())) {
                    // A newer foreground application wins over this retained
                    // activation intent. Joining input queues must never turn
                    // a stale request into a later focus steal.
                    result = FocusResult.POLICY_REFUSED;
                } else {
                    user32.SetFocus(target);
                    if (!isExpectedFocusTarget(
                            attachment,
                            parentProcessId,
                            parentThread,
                            targetThread)) {
                        result = FocusResult.TARGET_INVALID;
                    } else {
                        result = foregroundFocusedWindow() == targetWindow
                                ? FocusResult.FOCUSED
                                : FocusResult.POLICY_REFUSED;
                    }
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            focusFailure = failure;
        } finally {
            if (targetInputAttached) {
                try {
                    detachedInput = user32.AttachThreadInput(
                            new DWORD(Integer.toUnsignedLong(currentThread)),
                            new DWORD(Integer.toUnsignedLong(targetThread)),
                            false);
                } catch (RuntimeException | LinkageError failure) {
                    detachedInput = false;
                    if (focusFailure != null) {
                        focusFailure.addSuppressed(failure);
                    }
                }
            }
            if (parentInputAttached) {
                try {
                    boolean parentDetached = user32.AttachThreadInput(
                            new DWORD(Integer.toUnsignedLong(currentThread)),
                            new DWORD(Integer.toUnsignedLong(parentThread)),
                            false);
                    detachedInput = detachedInput && parentDetached;
                } catch (RuntimeException | LinkageError failure) {
                    detachedInput = false;
                    if (focusFailure != null) {
                        focusFailure.addSuppressed(failure);
                    }
                }
            }
        }
        if (!detachedInput) {
            return FocusResult.INPUT_QUEUE_DETACH_FAILED;
        }
        if (focusFailure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (focusFailure instanceof LinkageError linkageFailure) {
            throw linkageFailure;
        }
        if (result == FocusResult.FOCUSED) {
            // Detaching the joined queues can change both identity and the
            // physical foreground focus. Re-read authority after detach,
            // exactly as the inverse release transfer does.
            if (!isExpectedFocusTarget(
                    attachment,
                    parentProcessId,
                    parentThread,
                    targetThread)) {
                return FocusResult.TARGET_INVALID;
            }
            if (foregroundFocusedWindow() != targetWindow) {
                return FocusResult.POLICY_REFUSED;
            }
        }
        return result;
    }

    @Override
    public FocusResult releaseFocus(NativeCanvasAttachment attachment) {
        if (kernel32 == null) {
            throw new IllegalStateException(
                    "The injected Windows API has no focus-thread provider");
        }
        long parentProcessId = ProcessHandle.current().pid();
        if (!isExpectedReleaseTarget(attachment, parentProcessId)) {
            return FocusResult.TARGET_INVALID;
        }
        long parentWindow = attachment.parentWindow();
        long runnerFocusWindow = attachment.flutterViewWindow();
        HWND parent = hwnd(parentWindow);
        HWND runnerFocus = hwnd(runnerFocusWindow);
        IntByReference parentOwnerProcessId = new IntByReference();
        int parentThread = user32.GetWindowThreadProcessId(
                parent, parentOwnerProcessId);
        IntByReference runnerOwnerProcessId = new IntByReference();
        int runnerThread = user32.GetWindowThreadProcessId(
                runnerFocus, runnerOwnerProcessId);
        if (parentThread == 0
                || runnerThread == 0
                || Integer.toUnsignedLong(parentOwnerProcessId.getValue())
                        != parentProcessId
                || Integer.toUnsignedLong(runnerOwnerProcessId.getValue())
                        != attachment.runnerProcessId()) {
            return FocusResult.TARGET_INVALID;
        }
        long focusedWindow = foregroundFocusedWindow();
        if (focusedWindow == parentWindow) {
            return FocusResult.FOCUSED;
        }
        if (focusedWindow != runnerFocusWindow) {
            // Do not steal focus from a newer unrelated application/control.
            return FocusResult.POLICY_REFUSED;
        }

        int currentThread = kernel32.GetCurrentThreadId();
        boolean parentInputAttached = false;
        boolean runnerInputAttached = false;
        FocusResult result = FocusResult.POLICY_REFUSED;
        Throwable focusFailure = null;
        boolean detachedInput = true;
        try {
            boolean inputQueuesReady = true;
            if (currentThread != parentThread) {
                parentInputAttached = user32.AttachThreadInput(
                        new DWORD(Integer.toUnsignedLong(currentThread)),
                        new DWORD(Integer.toUnsignedLong(parentThread)),
                        true);
                inputQueuesReady = parentInputAttached;
            }
            if (inputQueuesReady
                    && currentThread != runnerThread
                    && runnerThread != parentThread) {
                runnerInputAttached = user32.AttachThreadInput(
                        new DWORD(Integer.toUnsignedLong(currentThread)),
                        new DWORD(Integer.toUnsignedLong(runnerThread)),
                        true);
                inputQueuesReady = runnerInputAttached;
            }
            if (inputQueuesReady) {
                if (!isExpectedReleaseTarget(
                        attachment,
                        parentProcessId,
                        parentThread,
                        runnerThread)) {
                    result = FocusResult.TARGET_INVALID;
                } else {
                    focusedWindow = foregroundFocusedWindow();
                    if (focusedWindow == parentWindow) {
                        result = FocusResult.FOCUSED;
                    } else if (focusedWindow != runnerFocusWindow) {
                        result = FocusResult.POLICY_REFUSED;
                    } else {
                        user32.SetFocus(parent);
                        if (!isExpectedReleaseTarget(
                                attachment,
                                parentProcessId,
                                parentThread,
                                runnerThread)) {
                            result = FocusResult.TARGET_INVALID;
                        } else {
                            result = foregroundFocusedWindow() == parentWindow
                                    ? FocusResult.FOCUSED
                                    : FocusResult.POLICY_REFUSED;
                        }
                    }
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            focusFailure = failure;
        } finally {
            if (runnerInputAttached) {
                try {
                    detachedInput = user32.AttachThreadInput(
                            new DWORD(Integer.toUnsignedLong(currentThread)),
                            new DWORD(Integer.toUnsignedLong(runnerThread)),
                            false);
                } catch (RuntimeException | LinkageError failure) {
                    detachedInput = false;
                    if (focusFailure != null) {
                        focusFailure.addSuppressed(failure);
                    }
                }
            }
            if (parentInputAttached) {
                try {
                    boolean parentDetached = user32.AttachThreadInput(
                            new DWORD(Integer.toUnsignedLong(currentThread)),
                            new DWORD(Integer.toUnsignedLong(parentThread)),
                            false);
                    detachedInput = detachedInput && parentDetached;
                } catch (RuntimeException | LinkageError failure) {
                    detachedInput = false;
                    if (focusFailure != null) {
                        focusFailure.addSuppressed(failure);
                    }
                }
            }
        }
        if (!detachedInput) {
            return FocusResult.INPUT_QUEUE_DETACH_FAILED;
        }
        if (focusFailure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (focusFailure instanceof LinkageError linkageFailure) {
            throw linkageFailure;
        }
        if (result == FocusResult.FOCUSED) {
            // Detaching the joined queues is part of the transfer boundary.
            // Re-read both identity and foreground focus afterwards rather
            // than trusting state observed while the queues were shared.
            if (!isExpectedReleaseTarget(
                    attachment,
                    parentProcessId,
                    parentThread,
                    runnerThread)) {
                return FocusResult.TARGET_INVALID;
            }
            if (foregroundFocusedWindow() != parentWindow) {
                return FocusResult.POLICY_REFUSED;
            }
        }
        return result;
    }

    @Override
    public boolean isFocused(NativeCanvasAttachment attachment) {
        if (!isExpectedFocusTarget(attachment)) {
            return false;
        }
        IntByReference processId = new IntByReference();
        int targetThread = user32.GetWindowThreadProcessId(
                hwnd(attachment.flutterViewWindow()), processId);
        return targetThread != 0
                && Integer.toUnsignedLong(processId.getValue())
                        == attachment.runnerProcessId()
                && foregroundFocusedWindow() == attachment.flutterViewWindow();
    }

    private boolean isExpectedFocusTarget(NativeCanvasAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        long parent = attachment.parentWindow();
        long runner = attachment.runnerWindow();
        long flutterView = attachment.flutterViewWindow();
        long processId = attachment.runnerProcessId();
        try {
            return isWindow(parent)
                    && isWindow(runner)
                    && isWindow(flutterView)
                    && parentWindow(runner) == parent
                    && parentWindow(flutterView) == runner
                    && ownerProcessId(runner) == processId
                    && ownerProcessId(flutterView) == processId
                    && hasChildStyle(runner)
                    && hasChildStyle(flutterView)
                    && RUNNER_WINDOW_CLASS.equals(windowClass(runner))
                    && FLUTTER_VIEW_WINDOW_CLASS.equals(windowClass(flutterView));
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private boolean isExpectedReleaseTarget(
            NativeCanvasAttachment attachment,
            long parentProcessId) {
        return isExpectedFocusTarget(attachment, parentProcessId);
    }

    private boolean isExpectedFocusTarget(
            NativeCanvasAttachment attachment,
            long parentProcessId) {
        return isExpectedFocusTarget(attachment)
                && ownerProcessId(attachment.parentWindow()) == parentProcessId;
    }

    private boolean isExpectedFocusTarget(
            NativeCanvasAttachment attachment,
            long parentProcessId,
            int parentThread,
            int targetThread) {
        if (!isExpectedFocusTarget(attachment, parentProcessId)) {
            return false;
        }
        IntByReference revalidatedParentProcessId = new IntByReference();
        int revalidatedParentThread = user32.GetWindowThreadProcessId(
                hwnd(attachment.parentWindow()),
                revalidatedParentProcessId);
        IntByReference revalidatedTargetProcessId = new IntByReference();
        int revalidatedTargetThread = user32.GetWindowThreadProcessId(
                hwnd(attachment.flutterViewWindow()),
                revalidatedTargetProcessId);
        return revalidatedParentThread == parentThread
                && revalidatedTargetThread == targetThread
                && Integer.toUnsignedLong(revalidatedParentProcessId.getValue())
                        == parentProcessId
                && Integer.toUnsignedLong(revalidatedTargetProcessId.getValue())
                        == attachment.runnerProcessId();
    }

    private boolean isExpectedReleaseTarget(
            NativeCanvasAttachment attachment,
            long parentProcessId,
            int parentThread,
            int runnerThread) {
        return isExpectedFocusTarget(
                attachment, parentProcessId, parentThread, runnerThread);
    }

    private boolean isAuthorizedFocusSource(
            long focusedWindow,
            long parentProcessId,
            long runnerProcessId) {
        if (focusedWindow == 0) {
            return true;
        }
        try {
            if (!isWindow(focusedWindow)) {
                return false;
            }
            long ownerProcessId = ownerProcessId(focusedWindow);
            return ownerProcessId == parentProcessId
                    || ownerProcessId == runnerProcessId;
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private long foregroundFocusedWindow() {
        GUITHREADINFO information = new GUITHREADINFO();
        // idThread == 0 asks Win32 for the foreground thread. Querying the
        // runner thread directly would return its remembered queue-local
        // hwndFocus even after the user has moved the real focus back to AWT.
        return user32.GetGUIThreadInfo(0, information)
                ? value(information.hwndFocus)
                : 0L;
    }

    @FunctionalInterface
    interface WindowsDpiApi {
        int getDpiForWindow(HWND window);

        static WindowsDpiApi system() {
            DpiUser32 api = Native.load("user32", DpiUser32.class);
            return api::GetDpiForWindow;
        }
    }

    private interface DpiUser32 extends com.sun.jna.win32.StdCallLibrary {
        int GetDpiForWindow(HWND window);
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
