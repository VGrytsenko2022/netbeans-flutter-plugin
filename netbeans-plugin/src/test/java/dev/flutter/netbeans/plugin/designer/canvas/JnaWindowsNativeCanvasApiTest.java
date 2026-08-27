package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JnaWindowsNativeCanvasApiTest {
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
