package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.GUITHREADINFO;
import com.sun.jna.ptr.IntByReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JnaWindowsNativeCanvasApiTest {
    @Test
    void showingAnAttachedWindowNeverActivatesItImplicitly() {
        AtomicReference<Object[]> showWindowArguments = new AtomicReference<>();
        User32 user32 = proxy(User32.class, (method, arguments) -> {
            if ("ShowWindow".equals(method.getName())) {
                showWindowArguments.set(arguments.clone());
            }
            return defaultValue(method.getReturnType());
        });
        JnaWindowsNativeCanvasApi windows = new JnaWindowsNativeCanvasApi(user32);

        windows.showWindow(0x1234L, true);

        Object[] arguments = showWindowArguments.get();
        assertEquals(0x1234L, windowValue(arguments[0]));
        assertEquals(4, arguments[1],
                "SW_SHOWNOACTIVATE must keep visibility separate from focus intent");

        windows.showWindow(0x1234L, false);
        assertEquals(0, showWindowArguments.get()[1]);
    }

    @Test
    void resizeUsesAsynchronousSetWindowPosWithoutImmediateRepaint() {
        AtomicReference<Object[]> setWindowPosArguments = new AtomicReference<>();
        AtomicInteger moveWindowCalls = new AtomicInteger();
        User32 user32 = (User32) Proxy.newProxyInstance(
                User32.class.getClassLoader(),
                new Class<?>[]{User32.class},
                (proxy, method, arguments) -> {
                    if ("SetWindowPos".equals(method.getName())) {
                        setWindowPosArguments.set(arguments.clone());
                        return true;
                    }
                    if ("MoveWindow".equals(method.getName())) {
                        moveWindowCalls.incrementAndGet();
                        return true;
                    }
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "FakeUser32";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            default -> null;
                        };
                    }
                    return defaultValue(method.getReturnType());
                });
        JnaWindowsNativeCanvasApi windows = new JnaWindowsNativeCanvasApi(user32);

        assertTrue(windows.setWindowBoundsAsync(
                0x1234L,
                new NativeCanvasWindowBounds(0, 0)));

        Object[] arguments = setWindowPosArguments.get();
        assertEquals(0x1234L, Pointer.nativeValue(((HWND) arguments[0]).getPointer()));
        assertNull(arguments[1]);
        assertEquals(0, arguments[2]);
        assertEquals(0, arguments[3]);
        assertEquals(1, arguments[4]);
        assertEquals(1, arguments[5]);
        assertEquals(
                WinUser.SWP_ASYNCWINDOWPOS
                        | WinUser.SWP_NOACTIVATE
                        | WinUser.SWP_NOZORDER,
                arguments[6]);
        assertEquals(0, moveWindowCalls.get(),
                "the AWT EDT must not synchronously MoveWindow a cross-process Flutter HWND");
    }

    @Test
    void readsDpiFromTheExactHwndThroughTheInjectedNativeBoundary() {
        AtomicReference<HWND> dpiWindow = new AtomicReference<>();
        JnaWindowsNativeCanvasApi windows = new JnaWindowsNativeCanvasApi(
                proxy(User32.class, (method, arguments) ->
                        defaultValue(method.getReturnType())),
                proxy(Kernel32.class, (method, arguments) ->
                        defaultValue(method.getReturnType())),
                window -> {
                    dpiWindow.set(window);
                    return 144;
                });

        assertEquals(144, windows.windowDpi(0x3456L));
        assertEquals(0x3456L, Pointer.nativeValue(dpiWindow.get().getPointer()));
    }

    @Test
    void focusTemporarilyJoinsInputQueuesAndVerifiesTheExactAttachment() {
        FocusNativeState state = new FocusNativeState();
        JnaWindowsNativeCanvasApi windows = focusApi(state);
        NativeCanvasAttachment attachment = attachment();

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.FOCUSED,
                windows.requestFocus(attachment));
        assertTrue(windows.isFocused(attachment));
        assertEquals(30L, state.focusedWindow);
        assertEquals(1, state.setFocusCalls);
        assertEquals(List.of(true, true, false, false), state.inputAttachments);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->22:false",
                "11->33:false"), state.inputAttachmentEdges);
        assertTrue(state.guiThreadQueries.stream().allMatch(thread -> thread == 0),
                "focus verification must query the actual foreground thread");
    }

    @Test
    void foreignForegroundIsRefusedBeforeJoiningInputQueues() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 40L;
        state.otherFocusProcessId = 99L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertTrue(state.inputAttachmentEdges.isEmpty());
    }

    @Test
    void foreignForegroundRefusalCanBeRetriedAfterJvmRegainsAuthority() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 40L;
        state.otherFocusProcessId = 99L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertTrue(state.inputAttachmentEdges.isEmpty());

        // A later real user activation moves foreground focus into the JVM.
        // Only that new physical authority may make the retained request safe.
        state.focusedWindow = 10L;

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.FOCUSED,
                windows.requestFocus(attachment()));
        assertEquals(30L, state.focusedWindow);
        assertEquals(1, state.setFocusCalls);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->22:false",
                "11->33:false"), state.inputAttachmentEdges);
    }

    @Test
    void foreignForegroundDriftDuringQueueJoinDoesNotReceiveSetFocus() {
        FocusNativeState state = new FocusNativeState();
        state.moveFocusToForeignAfterParentAttach = true;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->22:false",
                "11->33:false"), state.inputAttachmentEdges);
    }

    @Test
    void failedParentQueueJoinDoesNotJoinRunnerOrCallSetFocus() {
        FocusNativeState state = new FocusNativeState();
        state.failedAttachThread = state.parentThread;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertEquals(List.of("11->33:true"), state.inputAttachmentEdges);
    }

    @Test
    void failedRunnerQueueJoinDetachesParentWithoutCallingSetFocus() {
        FocusNativeState state = new FocusNativeState();
        state.failedAttachThread = state.runnerFocusThread;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->33:false"), state.inputAttachmentEdges);
        assertFalse(state.parentQueueAttached);
    }

    @Test
    void throwingRunnerQueueJoinDetachesParentBeforePropagating() {
        FocusNativeState state = new FocusNativeState();
        state.attachFailureThread = state.runnerFocusThread;
        state.attachFailure = new UnsatisfiedLinkError("synthetic runner attach failure");
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        UnsatisfiedLinkError failure = assertThrows(
                UnsatisfiedLinkError.class,
                () -> windows.requestFocus(attachment()));
        assertEquals("synthetic runner attach failure", failure.getMessage());
        assertEquals(0, state.setFocusCalls);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->33:false"), state.inputAttachmentEdges);
        assertFalse(state.parentQueueAttached);
    }

    @Test
    void alreadyFocusedTargetAvoidsAttachThreadInputAndItsKeyboardStateReset() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 30L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.FOCUSED,
                windows.requestFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertTrue(state.inputAttachments.isEmpty());
    }

    @Test
    void rememberedRunnerQueueFocusDoesNotMasqueradeAsForegroundFocus() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 40L;
        state.targetThreadRememberedFocus = 30L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.FOCUSED,
                windows.requestFocus(attachment()));

        assertEquals(1, state.setFocusCalls,
                "the remembered queue-local focus must not suppress SetFocus");
        assertEquals(30L, state.focusedWindow);
        assertTrue(state.guiThreadQueries.stream().allMatch(thread -> thread == 0));
    }

    @Test
    void rejectsForeignPidBeforeAttachingInputOrCallingSetFocus() {
        FocusNativeState state = new FocusNativeState();
        state.flutterViewProcessId = 99L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                windows.requestFocus(attachment()));
        assertFalse(windows.isFocused(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertTrue(state.inputAttachments.isEmpty());
    }

    @Test
    void identityDriftAfterInputQueueAttachIsRejectedBeforeSetFocus() {
        FocusNativeState state = new FocusNativeState();
        state.invalidateAfterAttach = true;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                windows.requestFocus(attachment()));
        assertEquals(List.of(true, true, false, false), state.inputAttachments,
                "the joined queues must still be detached after identity drift");
        assertEquals(0, state.setFocusCalls,
                "a target that drifted after attach must never receive SetFocus");
    }

    @Test
    void revalidatesTargetIdentityAndPhysicalFocusAfterDetachingJoinedQueue() {
        FocusNativeState parentDrift = new FocusNativeState();
        parentDrift.invalidateParentAfterDetach = true;
        JnaWindowsNativeCanvasApi parentDriftApi = focusApi(parentDrift);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                parentDriftApi.requestFocus(attachment()));
        assertEquals(30L, parentDrift.focusedWindow);
        assertEquals(List.of(true, true, false, false),
                parentDrift.inputAttachments);

        FocusNativeState identityDrift = new FocusNativeState();
        identityDrift.invalidateFocusTargetAfterDetach = true;
        JnaWindowsNativeCanvasApi identityDriftApi = focusApi(identityDrift);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                identityDriftApi.requestFocus(attachment()));
        assertEquals(30L, identityDrift.focusedWindow);
        assertEquals(List.of(true, true, false, false),
                identityDrift.inputAttachments);

        FocusNativeState focusDrift = new FocusNativeState();
        focusDrift.moveRunnerFocusAfterDetach = true;
        JnaWindowsNativeCanvasApi focusDriftApi = focusApi(focusDrift);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                focusDriftApi.requestFocus(attachment()));
        assertEquals(40L, focusDrift.focusedWindow);
        assertEquals(List.of(true, true, false, false),
                focusDrift.inputAttachments);
    }

    @Test
    void reportsInputQueueDetachFailureSeparatelyFromPolicyRefusal() {
        FocusNativeState state = new FocusNativeState();
        state.detachSucceeds = false;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED,
                windows.requestFocus(attachment()));
        assertEquals(List.of(true, true, false, false), state.inputAttachments);
        assertEquals(1, state.setFocusCalls);
    }

    @Test
    void detachFailureWinsWhenSetFocusAlsoThrows() {
        FocusNativeState state = new FocusNativeState();
        state.setFocusFailure = new UnsatisfiedLinkError("synthetic SetFocus failure");
        state.detachSucceeds = false;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED,
                windows.requestFocus(attachment()));
        assertEquals(List.of(true, true, false, false), state.inputAttachments);
    }

    @Test
    void detachLinkageFailureIsReportedAsUnsafeQueueState() {
        FocusNativeState state = new FocusNativeState();
        state.detachFailure = new UnsatisfiedLinkError("synthetic detach failure");
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED,
                windows.requestFocus(attachment()));
        assertEquals(List.of(true, true, false, false), state.inputAttachments);
    }

    @Test
    void releaseFocusJoinsBothQueuesAndTargetsTheExactJvmParent() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 30L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.FOCUSED,
                windows.releaseFocus(attachment()));

        assertEquals(10L, state.focusedWindow);
        assertEquals(1, state.setFocusCalls);
        assertEquals(List.of(
                "11->33:true",
                "11->22:true",
                "11->22:false",
                "11->33:false"), state.inputAttachmentEdges);
        assertTrue(state.guiThreadQueries.stream().allMatch(thread -> thread == 0),
                "release verification must query the actual foreground thread");
    }

    @Test
    void rejectsForeignOrReusedParentPidWithoutJoiningInputQueues() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 30L;
        state.parentProcessId = 99L;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                windows.releaseFocus(attachment()));
        assertEquals(0, state.setFocusCalls);
        assertTrue(state.inputAttachmentEdges.isEmpty());
    }

    @Test
    void revalidatesParentPidAndPhysicalFocusAfterDetachingJoinedQueues() {
        FocusNativeState parentDrift = new FocusNativeState();
        parentDrift.focusedWindow = 30L;
        parentDrift.invalidateParentAfterDetach = true;
        JnaWindowsNativeCanvasApi parentDriftApi = focusApi(parentDrift);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.TARGET_INVALID,
                parentDriftApi.releaseFocus(attachment()));
        assertEquals(10L, parentDrift.focusedWindow);
        assertEquals(List.of(true, true, false, false),
                parentDrift.inputAttachments);

        FocusNativeState focusDrift = new FocusNativeState();
        focusDrift.focusedWindow = 30L;
        focusDrift.moveFocusAfterDetach = true;
        JnaWindowsNativeCanvasApi focusDriftApi = focusApi(focusDrift);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED,
                focusDriftApi.releaseFocus(attachment()));
        assertEquals(40L, focusDrift.focusedWindow);
        assertEquals(List.of(true, true, false, false),
                focusDrift.inputAttachments);
    }

    @Test
    void releaseDetachFailureWinsAndLeavesNoTrustedAttachmentResult() {
        FocusNativeState state = new FocusNativeState();
        state.focusedWindow = 30L;
        state.detachSucceeds = false;
        JnaWindowsNativeCanvasApi windows = focusApi(state);

        assertEquals(
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED,
                windows.releaseFocus(attachment()));
        assertEquals(List.of(true, true, false, false), state.inputAttachments);
    }

    private static NativeCanvasAttachment attachment() {
        return new NativeCanvasAttachment(10L, 20L, 30L, 77L);
    }

    private static JnaWindowsNativeCanvasApi focusApi(FocusNativeState state) {
        User32 user32 = proxy(User32.class, (method, arguments) -> {
            return switch (method.getName()) {
                case "IsWindow" -> true;
                case "GetParent" -> hwnd(windowValue(arguments[0]) == 30L ? 20L : 10L);
                case "GetWindowThreadProcessId" -> {
                    long window = windowValue(arguments[0]);
                    IntByReference processId = (IntByReference) arguments[1];
                    if (processId != null) {
                        long owner = switch ((int) window) {
                            case 10 -> state.parentProcessId;
                            case 30 -> state.flutterViewProcessId;
                            case 40 -> state.otherFocusProcessId;
                            default -> state.runnerProcessId;
                        };
                        processId.setValue((int) owner);
                    }
                    yield switch ((int) window) {
                        case 10 -> state.parentThread;
                        case 30 -> state.runnerFocusThread;
                        default -> 21;
                    };
                }
                case "GetWindowLong" -> WinUser.WS_CHILD;
                case "GetClassName" -> writeClassName(
                        (char[]) arguments[1],
                        windowValue(arguments[0]) == 30L
                                ? "FLUTTERVIEW"
                                : "FLUTTER_RUNNER_WIN32_WINDOW");
                case "AttachThreadInput" -> {
                    int sourceThread = (int) ((DWORD) arguments[0]).longValue();
                    int targetThread = (int) ((DWORD) arguments[1]).longValue();
                    boolean attach = (Boolean) arguments[2];
                    boolean parentEdge = targetThread == state.parentThread;
                    boolean runnerEdge = targetThread == state.runnerFocusThread;
                    state.inputAttachments.add(attach);
                    state.inputAttachmentEdges.add(
                            sourceThread + "->" + targetThread + ":" + attach);
                    if (attach
                            && targetThread == state.attachFailureThread
                            && state.attachFailure != null) {
                        throw state.attachFailure;
                    }
                    if (!attach && parentEdge) {
                        if (state.invalidateParentAfterDetach) {
                            state.parentProcessId = 99L;
                        }
                        if (state.moveFocusAfterDetach) {
                            state.focusedWindow = 40L;
                        }
                    }
                    if (!attach && runnerEdge) {
                        if (state.invalidateFocusTargetAfterDetach) {
                            state.flutterViewProcessId = 99L;
                        }
                        if (state.moveRunnerFocusAfterDetach) {
                            state.focusedWindow = 40L;
                        }
                    }
                    if (!attach && state.detachFailure != null) {
                        throw state.detachFailure;
                    }
                    boolean succeeded = attach
                            ? state.attachSucceeds
                                    && targetThread != state.failedAttachThread
                            : state.detachSucceeds;
                    if (succeeded) {
                        if (parentEdge) {
                            state.parentQueueAttached = attach;
                        }
                        if (runnerEdge) {
                            state.runnerQueueAttached = attach;
                        }
                        if (attach && state.invalidateAfterAttach) {
                            state.flutterViewProcessId = 99L;
                        }
                        if (attach && parentEdge
                                && state.moveFocusToForeignAfterParentAttach) {
                            state.focusedWindow = 40L;
                            state.otherFocusProcessId = 99L;
                        }
                    }
                    yield succeeded;
                }
                case "SetFocus" -> {
                    state.setFocusCalls++;
                    if (state.setFocusFailure != null) {
                        throw state.setFocusFailure;
                    }
                    boolean parentQueueReady = state.currentThread
                            == state.parentThread || state.parentQueueAttached;
                    boolean runnerQueueReady = state.currentThread
                            == state.runnerFocusThread || state.runnerQueueAttached;
                    if (state.setFocusSucceeds
                            && parentQueueReady
                            && runnerQueueReady) {
                        state.focusedWindow = windowValue(arguments[0]);
                        state.targetThreadRememberedFocus = state.focusedWindow;
                    }
                    yield null;
                }
                case "GetGUIThreadInfo" -> {
                    int thread = (Integer) arguments[0];
                    state.guiThreadQueries.add(thread);
                    long focused = thread == 0
                            ? state.focusedWindow
                            : state.targetThreadRememberedFocus;
                    ((GUITHREADINFO) arguments[1]).hwndFocus =
                            focused == 0L
                                    ? null
                                    : hwnd(focused);
                    yield true;
                }
                default -> defaultValue(method.getReturnType());
            };
        });
        Kernel32 kernel32 = proxy(Kernel32.class, (method, arguments) ->
                "GetCurrentThreadId".equals(method.getName())
                        ? state.currentThread
                        : defaultValue(method.getReturnType()));
        return new JnaWindowsNativeCanvasApi(user32, kernel32, ignored -> 96);
    }

    private static int writeClassName(char[] target, String value) {
        value.getChars(0, value.length(), target, 0);
        return value.length();
    }

    private static HWND hwnd(long window) {
        return new HWND(new Pointer(window));
    }

    private static long windowValue(Object window) {
        return Pointer.nativeValue(((HWND) window).getPointer());
    }

    private static final class FocusNativeState {
        private final List<Boolean> inputAttachments = new ArrayList<>();
        private final List<String> inputAttachmentEdges = new ArrayList<>();
        private final List<Integer> guiThreadQueries = new ArrayList<>();
        private long parentProcessId = ProcessHandle.current().pid();
        private long runnerProcessId = 77L;
        private long flutterViewProcessId = 77L;
        private long otherFocusProcessId = ProcessHandle.current().pid();
        private int currentThread = 11;
        private int parentThread = 33;
        private int runnerFocusThread = 22;
        private long focusedWindow;
        private long targetThreadRememberedFocus;
        private boolean attachSucceeds = true;
        private boolean detachSucceeds = true;
        private boolean setFocusSucceeds = true;
        private boolean parentQueueAttached;
        private boolean runnerQueueAttached;
        private int failedAttachThread = -1;
        private int attachFailureThread = -1;
        private boolean invalidateAfterAttach;
        private boolean invalidateFocusTargetAfterDetach;
        private boolean invalidateParentAfterDetach;
        private boolean moveFocusAfterDetach;
        private boolean moveRunnerFocusAfterDetach;
        private boolean moveFocusToForeignAfterParentAttach;
        private LinkageError attachFailure;
        private LinkageError setFocusFailure;
        private LinkageError detachFailure;
        private int setFocusCalls;
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "Fake" + type.getSimpleName();
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, arguments);
                }));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments)
                throws Throwable;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        return null;
    }
}
