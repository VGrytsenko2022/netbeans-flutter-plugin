package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class WindowsNativeCanvasHostTest {
    @Test
    void attachesOnlyTheOneDirectChildOwnedByTheExactRunner() throws Exception {
        FakeWindowsApi windows = new FakeWindowsApi();
        windows.descendants.put(10L, List.of(20L, 21L, 22L));
        windows.descendants.put(22L, List.of(30L));
        windows.parents.put(20L, 10L);
        windows.parents.put(21L, 20L);
        windows.parents.put(22L, 10L);
        windows.parents.put(30L, 22L);
        windows.processes.put(20L, 200L);
        windows.processes.put(21L, 77L);
        windows.processes.put(22L, 77L);
        windows.processes.put(30L, 77L);
        windows.childStyles.put(22L, true);
        windows.childStyles.put(30L, true);
        windows.classes.put(22L, "FLUTTER_RUNNER_WIN32_WINDOW");
        windows.classes.put(30L, "FLUTTERVIEW");
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));

        NativeCanvasAttachment attached = onEdt(() -> host.tryAttach(77L).orElseThrow());

        assertEquals(new NativeCanvasAttachment(10L, 22L, 30L, 77L), attached);
        assertEquals(List.of(new NativeCanvasWindowBounds(640, 480)), windows.moves);
        assertEquals(List.of("22:false"), windows.visibility);
    }

    @Test
    void reportsPendingUntilTheRunnerCreatesItsWindow() throws Exception {
        FakeWindowsApi windows = new FakeWindowsApi();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));

        assertTrue(onEdt(() -> host.tryAttach(77L).isEmpty()));
        assertTrue(windows.moves.isEmpty());
    }

    @Test
    void keepsTheWrapperHiddenUntilTheRealFlutterViewExists() throws Exception {
        FakeWindowsApi windows = new FakeWindowsApi();
        windows.descendants.put(10L, List.of(20L));
        windows.parents.put(20L, 10L);
        windows.processes.put(20L, 77L);
        windows.childStyles.put(20L, true);
        windows.classes.put(20L, "FLUTTER_RUNNER_WIN32_WINDOW");
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));

        assertTrue(onEdt(() -> host.tryAttach(77L).isEmpty()));
        assertTrue(windows.moves.isEmpty());
        assertTrue(windows.visibility.isEmpty());
    }

    @Test
    void rejectsAmbiguousRunnerWindowsAndNonChildStyles() throws Exception {
        FakeWindowsApi ambiguous = new FakeWindowsApi();
        ambiguous.descendants.put(10L, List.of(20L, 21L));
        ambiguous.parents.put(20L, 10L);
        ambiguous.parents.put(21L, 10L);
        ambiguous.processes.put(20L, 77L);
        ambiguous.processes.put(21L, 77L);
        WindowsNativeCanvasHost ambiguousHost =
                onEdt(() -> new WindowsNativeCanvasHost(ambiguous));

        IllegalStateException multiple = assertThrows(
                IllegalStateException.class,
                () -> onEdt(() -> ambiguousHost.tryAttach(77L)));
        assertTrue(multiple.getMessage().contains("exactly one"));

        FakeWindowsApi topLevel = new FakeWindowsApi();
        topLevel.descendants.put(10L, List.of(20L));
        topLevel.parents.put(20L, 10L);
        topLevel.processes.put(20L, 77L);
        topLevel.childStyles.put(20L, false);
        WindowsNativeCanvasHost topLevelHost =
                onEdt(() -> new WindowsNativeCanvasHost(topLevel));

        IllegalStateException style = assertThrows(
                IllegalStateException.class,
                () -> onEdt(() -> topLevelHost.tryAttach(77L)));
        assertTrue(style.getMessage().contains("WS_CHILD"));
    }

    @Test
    void closeHidesAndForgetsTheNativeChild() throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        onEdt(() -> host.tryAttach(77L));

        onEdt(() -> {
            host.close();
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(List.of("20:false", "20:false"), windows.visibility);
    }

    @Test
    void nativeOperationsAreFencedToTheEventDispatchThread() {
        WindowsNativeCanvasHost host = new WindowsNativeCanvasHost(new FakeWindowsApi());

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, host::parentWindowHandle);

        assertTrue(failure.getMessage().contains("EDT"));
    }

    @Test
    void rejectsADeadAwtParentBeforePassingItsHandleToTheRunner() throws Exception {
        FakeWindowsApi windows = new FakeWindowsApi();
        windows.liveParent = false;
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> onEdt(host::parentWindowHandle));

        assertTrue(failure.getMessage().contains("not a live"));
        assertFalse(onEdt(() -> host.attachment().isPresent()));
    }

    @Test
    void resizeWindowDeathAfterLivenessCheckInvalidatesAndNotifiesOnce() throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.killRunnerDuringNextBoundsRequest = true;

        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(1, failures.get());
        assertEquals(2, windows.moves.size());

        onEdt(() -> {
            dispatchResize(host);
            return null;
        });
        assertEquals(1, failures.get());
    }

    @Test
    void liveAsynchronousBoundsRequestFailureStillInvalidatesAndNotifiesSession()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.failNextBoundsRequest = true;

        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(1, failures.get());
    }

    @Test
    void changedNativeClassInvalidatesAttachmentBeforeResize() throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.classes.put(30L, "UNTRUSTED_WINDOW");

        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(1, failures.get());
        assertEquals(1, windows.moves.size());
    }

    @Test
    void resizeListenerContainsFailuresFromTheAttachmentFailureCallback() throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> {
                throw new AssertionError("simulated session callback failure");
            });
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.failNextBoundsRequest = true;

        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
    }

    @Test
    void finalShowLinkageFailureRejectsAttachmentWithoutEscapingTheEdt()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        windows.failNextShowWithLinkageError = true;

        Optional<NativeCanvasAttachment> result = onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            return host.tryAttach(77L);
        });

        assertTrue(result.isEmpty());
        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(1, failures.get());
    }

    @Test
    void visibilityLinkageFailureInvalidatesAttachmentWithoutEscapingTheEdt()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.failNextShowWithLinkageError = true;

        onEdt(() -> {
            host.setRunnerVisible(true);
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
        assertEquals(1, failures.get());
    }

    @Test
    void closeClearsAttachmentEvenWhenNativeHideThrowsLinkageError()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        onEdt(() -> {
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.failNextShowWithLinkageError = true;

        onEdt(() -> {
            host.close();
            return null;
        });

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
    }

    private static void dispatchResize(WindowsNativeCanvasHost host) {
        Component canvas = host.getComponent(0);
        canvas.dispatchEvent(new ComponentEvent(canvas, ComponentEvent.COMPONENT_RESIZED));
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return callable.call();
        }
        Holder<T> value = new Holder<>();
        Holder<Throwable> failure = new Holder<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    value.value = callable.call();
                } catch (Throwable throwable) {
                    failure.value = throwable;
                }
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return value.value;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class FakeWindowsApi implements WindowsNativeCanvasApi {
        private boolean displayable = true;
        private boolean liveParent = true;
        private final Map<Long, List<Long>> descendants = new HashMap<>();
        private final Map<Long, Long> parents = new HashMap<>();
        private final Map<Long, Long> processes = new HashMap<>();
        private final Map<Long, Boolean> childStyles = new HashMap<>();
        private final Map<Long, String> classes = new HashMap<>();
        private final Set<Long> deadWindows = new HashSet<>();
        private final List<NativeCanvasWindowBounds> moves = new ArrayList<>();
        private final List<String> visibility = new ArrayList<>();
        private boolean failNextBoundsRequest;
        private boolean killRunnerDuringNextBoundsRequest;
        private boolean failNextShowWithLinkageError;

        static FakeWindowsApi attachable() {
            FakeWindowsApi windows = new FakeWindowsApi();
            windows.descendants.put(10L, List.of(20L));
            windows.descendants.put(20L, List.of(30L));
            windows.parents.put(20L, 10L);
            windows.parents.put(30L, 20L);
            windows.processes.put(20L, 77L);
            windows.processes.put(30L, 77L);
            windows.childStyles.put(20L, true);
            windows.childStyles.put(30L, true);
            windows.classes.put(20L, "FLUTTER_RUNNER_WIN32_WINDOW");
            windows.classes.put(30L, "FLUTTERVIEW");
            return windows;
        }

        @Override
        public boolean isComponentDisplayable(Canvas component) {
            return displayable;
        }

        @Override
        public long componentWindow(Canvas component) {
            return 10L;
        }

        @Override
        public boolean isWindow(long window) {
            if (deadWindows.contains(window)) {
                return false;
            }
            return window == 10L ? liveParent : descendants.values().stream()
                    .flatMap(List::stream)
                    .anyMatch(candidate -> candidate == window);
        }

        @Override
        public long parentWindow(long window) {
            return parents.getOrDefault(window, 0L);
        }

        @Override
        public long ownerProcessId(long window) {
            return processes.getOrDefault(window, 0L);
        }

        @Override
        public boolean hasChildStyle(long window) {
            return childStyles.getOrDefault(window, false);
        }

        @Override
        public String windowClass(long window) {
            return classes.getOrDefault(window, "");
        }

        @Override
        public List<Long> descendantWindows(long parentWindow) {
            return descendants.getOrDefault(parentWindow, List.of());
        }

        @Override
        public NativeCanvasWindowBounds clientBounds(long window) {
            return new NativeCanvasWindowBounds(640, 480);
        }

        @Override
        public boolean setWindowBoundsAsync(
                long window,
                NativeCanvasWindowBounds bounds) {
            moves.add(bounds);
            if (killRunnerDuringNextBoundsRequest) {
                killRunnerDuringNextBoundsRequest = false;
                deadWindows.add(window);
                return false;
            }
            if (failNextBoundsRequest) {
                failNextBoundsRequest = false;
                return false;
            }
            return true;
        }

        @Override
        public void showWindow(long window, boolean visible) {
            if (failNextShowWithLinkageError) {
                failNextShowWithLinkageError = false;
                throw new UnsatisfiedLinkError("simulated ShowWindow linkage failure");
            }
            visibility.add(window + ":" + visible);
        }
    }
}
