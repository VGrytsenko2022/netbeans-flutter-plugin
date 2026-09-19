package io.github.vgrytsenko2022.plugin.designer.canvas;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser.GUITHREADINFO;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Observes and releases focus owned by the exact windowed WebView2 subtree.
 *
 * <p>No controller HWND crosses the native ABI. Authority instead requires a
 * foreground focus HWND that is a strict descendant of the exact JVM-owned AWT
 * parent and whose owner process is the JVM or one of its live descendants.</p>
 */
final class WindowsWebCanvasFocusController {
    private static final int MAXIMUM_PROCESS_ANCESTRY_DEPTH = 64;

    private final NativeApi windows;
    private final LongSupplier currentProcessId;
    private final ProcessDescendantProbe processDescendantProbe;

    static WindowsWebCanvasFocusController system() {
        return new WindowsWebCanvasFocusController(
                NativeApi.system(),
                () -> ProcessHandle.current().pid(),
                ProcessDescendantProbe.system());
    }

    WindowsWebCanvasFocusController(
            NativeApi windows,
            LongSupplier currentProcessId,
            ProcessDescendantProbe processDescendantProbe) {
        this.windows = Objects.requireNonNull(windows, "windows");
        this.currentProcessId = Objects.requireNonNull(
                currentProcessId, "currentProcessId");
        this.processDescendantProbe = Objects.requireNonNull(
                processDescendantProbe, "processDescendantProbe");
    }

    boolean isControllerFocused(long exactParentWindow) {
        return captureControllerFocus(exactParentWindow) != null;
    }

    FocusReleaseResult releaseControllerFocus(long exactParentWindow) {
        FocusSnapshot authorized = captureControllerFocus(exactParentWindow);
        if (authorized == null) {
            return FocusReleaseResult.REFUSED;
        }

        long currentThread;
        try {
            currentThread = windows.currentThreadId();
        } catch (RuntimeException | LinkageError failure) {
            return FocusReleaseResult.REFUSED;
        }
        if (!validUnsignedDword(currentThread)) {
            return FocusReleaseResult.REFUSED;
        }

        List<Long> attachedThreads = new ArrayList<>(2);
        boolean operationSucceeded = false;
        boolean detachSucceeded = true;
        try {
            boolean queuesReady = attachIfRequired(
                    currentThread,
                    authorized.parentIdentity().threadId(),
                    attachedThreads);
            if (queuesReady
                    && !authorized.equals(captureControllerFocus(
                            exactParentWindow))) {
                queuesReady = false;
            }
            if (queuesReady
                    && authorized.focusIdentity().threadId()
                    != authorized.parentIdentity().threadId()) {
                queuesReady = attachIfRequired(
                        currentThread,
                        authorized.focusIdentity().threadId(),
                        attachedThreads);
            }
            if (queuesReady) {
                FocusSnapshot revalidated = captureControllerFocus(
                        exactParentWindow);
                if (authorized.equals(revalidated)) {
                    windows.setFocus(exactParentWindow);
                    operationSucceeded = isExactParentFocused(authorized);
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            operationSucceeded = false;
        } finally {
            for (int index = attachedThreads.size() - 1; index >= 0; index--) {
                try {
                    boolean detached = windows.attachThreadInput(
                            currentThread, attachedThreads.get(index), false);
                    detachSucceeded = detachSucceeded && detached;
                } catch (RuntimeException | LinkageError failure) {
                    detachSucceeded = false;
                }
            }
        }

        // Detaching can change both remembered queue focus and the physical
        // foreground target. Trust only a fresh post-detach observation.
        if (!detachSucceeded) {
            return FocusReleaseResult.INPUT_QUEUE_DETACH_FAILED;
        }
        return operationSucceeded && isExactParentFocused(authorized)
                ? FocusReleaseResult.RELEASED
                : FocusReleaseResult.REFUSED;
    }

    private boolean attachIfRequired(
            long currentThread,
            long targetThread,
            List<Long> attachedThreads) {
        if (targetThread == currentThread || attachedThreads.contains(targetThread)) {
            return true;
        }
        if (!windows.attachThreadInput(currentThread, targetThread, true)) {
            return false;
        }
        attachedThreads.add(targetThread);
        return true;
    }

    private FocusSnapshot captureControllerFocus(long exactParentWindow) {
        if (exactParentWindow == 0) {
            return null;
        }
        try {
            long processId = currentProcessId.getAsLong();
            if (!validUnsignedDword(processId)
                    || !windows.isWindow(exactParentWindow)) {
                return null;
            }
            WindowIdentity parentIdentity = windows.windowIdentity(
                    exactParentWindow);
            if (!parentIdentity.valid()
                    || parentIdentity.processId() != processId) {
                return null;
            }

            long focusedWindow = windows.foregroundFocusedWindow();
            if (focusedWindow == 0
                    || focusedWindow == exactParentWindow
                    || !windows.isWindow(focusedWindow)
                    || !windows.isChild(exactParentWindow, focusedWindow)) {
                return null;
            }
            WindowIdentity focusIdentity = windows.windowIdentity(focusedWindow);
            if (!focusIdentity.valid()
                    || !processDescendantProbe.isCurrentOrDescendant(
                            processId, focusIdentity.processId())) {
                return null;
            }

            // Fence HWND reuse, focus movement and process/thread identity drift
            // across the complete authorization read.
            if (currentProcessId.getAsLong() != processId
                    || !windows.isWindow(exactParentWindow)
                    || !windows.isWindow(focusedWindow)
                    || windows.foregroundFocusedWindow() != focusedWindow
                    || !windows.isChild(exactParentWindow, focusedWindow)
                    || !parentIdentity.equals(windows.windowIdentity(
                            exactParentWindow))
                    || !focusIdentity.equals(windows.windowIdentity(focusedWindow))
                    || !processDescendantProbe.isCurrentOrDescendant(
                            processId, focusIdentity.processId())) {
                return null;
            }
            return new FocusSnapshot(
                    exactParentWindow,
                    parentIdentity,
                    focusedWindow,
                    focusIdentity,
                    processId);
        } catch (RuntimeException | LinkageError failure) {
            return null;
        }
    }

    private boolean isExactParentFocused(FocusSnapshot authorized) {
        try {
            long parentWindow = authorized.parentWindow();
            long processId = authorized.currentProcessId();
            return currentProcessId.getAsLong() == processId
                    && windows.isWindow(parentWindow)
                    && authorized.parentIdentity().equals(
                            windows.windowIdentity(parentWindow))
                    && authorized.parentIdentity().processId() == processId
                    && windows.foregroundFocusedWindow() == parentWindow
                    && authorized.parentIdentity().equals(
                            windows.windowIdentity(parentWindow));
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private static boolean validUnsignedDword(long value) {
        return value > 0 && value <= 0xffff_ffffL;
    }

    record WindowIdentity(long threadId, long processId) {
        boolean valid() {
            return validUnsignedDword(threadId)
                    && validUnsignedDword(processId);
        }
    }

    enum FocusReleaseResult {
        RELEASED,
        REFUSED,
        INPUT_QUEUE_DETACH_FAILED
    }

    private record FocusSnapshot(
            long parentWindow,
            WindowIdentity parentIdentity,
            long focusedWindow,
            WindowIdentity focusIdentity,
            long currentProcessId) {
    }

    interface NativeApi {
        boolean isWindow(long window);

        boolean isChild(long parentWindow, long childWindow);

        WindowIdentity windowIdentity(long window);

        long foregroundFocusedWindow();

        long currentThreadId();

        boolean attachThreadInput(
                long sourceThread, long targetThread, boolean attach);

        void setFocus(long window);

        static NativeApi system() {
            return new JnaNativeApi(
                    User32.INSTANCE,
                    Kernel32.INSTANCE,
                    Native.load(
                            "user32",
                            ChildWindowUser32.class,
                            W32APIOptions.DEFAULT_OPTIONS));
        }
    }

    interface ProcessDescendantProbe {
        boolean isCurrentOrDescendant(
                long currentProcessId, long candidateProcessId);

        static ProcessDescendantProbe system() {
            return WindowsWebCanvasFocusController::isCurrentOrDescendantProcess;
        }
    }

    private static boolean isCurrentOrDescendantProcess(
            long currentProcessId, long candidateProcessId) {
        if (!validUnsignedDword(currentProcessId)
                || !validUnsignedDword(candidateProcessId)) {
            return false;
        }
        if (currentProcessId == candidateProcessId) {
            return true;
        }
        Optional<ProcessHandle> candidate = ProcessHandle.of(candidateProcessId);
        if (candidate.isEmpty() || !candidate.orElseThrow().isAlive()) {
            return false;
        }
        ProcessHandle cursor = candidate.orElseThrow();
        for (int depth = 0; depth < MAXIMUM_PROCESS_ANCESTRY_DEPTH; depth++) {
            Optional<ProcessHandle> parent = cursor.parent();
            if (parent.isEmpty()) {
                return false;
            }
            ProcessHandle ancestor = parent.orElseThrow();
            if (ancestor.pid() == currentProcessId) {
                return true;
            }
            if (ancestor.pid() == cursor.pid()) {
                return false;
            }
            cursor = ancestor;
        }
        return false;
    }

    private static final class JnaNativeApi implements NativeApi {
        private final User32 user32;
        private final Kernel32 kernel32;
        private final ChildWindowUser32 childWindowUser32;

        private JnaNativeApi(
                User32 user32,
                Kernel32 kernel32,
                ChildWindowUser32 childWindowUser32) {
            this.user32 = Objects.requireNonNull(user32, "user32");
            this.kernel32 = Objects.requireNonNull(kernel32, "kernel32");
            this.childWindowUser32 = Objects.requireNonNull(
                    childWindowUser32, "childWindowUser32");
        }

        @Override
        public boolean isWindow(long window) {
            return window != 0 && user32.IsWindow(hwnd(window));
        }

        @Override
        public boolean isChild(long parentWindow, long childWindow) {
            return parentWindow != 0
                    && childWindow != 0
                    && childWindowUser32.IsChild(
                            hwnd(parentWindow), hwnd(childWindow));
        }

        @Override
        public WindowIdentity windowIdentity(long window) {
            IntByReference processId = new IntByReference();
            int threadId = user32.GetWindowThreadProcessId(
                    hwnd(window), processId);
            return new WindowIdentity(
                    Integer.toUnsignedLong(threadId),
                    Integer.toUnsignedLong(processId.getValue()));
        }

        @Override
        public long foregroundFocusedWindow() {
            GUITHREADINFO information = new GUITHREADINFO();
            return user32.GetGUIThreadInfo(0, information)
                    ? value(information.hwndFocus)
                    : 0L;
        }

        @Override
        public long currentThreadId() {
            return Integer.toUnsignedLong(kernel32.GetCurrentThreadId());
        }

        @Override
        public boolean attachThreadInput(
                long sourceThread, long targetThread, boolean attach) {
            return user32.AttachThreadInput(
                    new DWORD(sourceThread),
                    new DWORD(targetThread),
                    attach);
        }

        @Override
        public void setFocus(long window) {
            user32.SetFocus(hwnd(window));
        }

        private static HWND hwnd(long value) {
            return new HWND(Pointer.createConstant(value));
        }

        private static long value(HWND window) {
            return window == null || window.getPointer() == null
                    ? 0L
                    : Pointer.nativeValue(window.getPointer());
        }
    }

    private interface ChildWindowUser32 extends StdCallLibrary {
        boolean IsChild(HWND parentWindow, HWND childWindow);
    }
}
