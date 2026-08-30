package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
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
        windows.bounds = new NativeCanvasWindowBounds(641, 481);
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
        windows.bounds = new NativeCanvasWindowBounds(641, 481);
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
        windows.bounds = new NativeCanvasWindowBounds(641, 481);
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
    void closeClearsAttachmentAndReportsNativeHideLinkageError()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        onEdt(() -> {
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.failNextShowWithLinkageError = true;

        assertThrows(UnsatisfiedLinkError.class, () -> onEdt(() -> {
                host.close();
                return null;
            }));

        assertTrue(onEdt(() -> host.attachment().isEmpty()));
    }

    @Test
    void implementsPlatformContractWithOpaqueParentAndIdempotentDetach()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        NativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));

        NativeCanvasParentHandle parent = onEdt(host::parentHandle);
        assertEquals(NativeCanvasPlatform.WINDOWS, parent.platform());
        assertEquals("0x000000000000000A", parent.encodedValue());
        assertEquals("WINDOWS:<opaque>", parent.toString());
        assertTrue(onEdt(() -> host.attachRunner(77L)));
        assertTrue(onEdt(host::isRunnerAttached));
        assertTrue(onEdt(host::isRunnerSurfaceLive));

        onEdt(() -> {
            host.detachRunner();
            host.detachRunner();
            host.close();
            host.close();
            return null;
        });

        assertFalse(onEdt(host::isRunnerAttached));
        assertEquals(List.of("20:false", "20:false"), windows.visibility);
    }

    @Test
    void transfersFocusAcrossTheVerifiedPairWithoutInvalidatingOnPolicyRefusal()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });

        assertTrue(onEdt(host::requestRunnerFocus));
        assertEquals(List.of(30L), windows.focusRequests);
        assertTrue(onEdt(host::isRunnerFocused));
        assertTrue(onEdt(host::isRunnerAttached));

        assertTrue(onEdt(host::releaseRunnerFocus));
        assertEquals(List.of(10L), windows.focusReleases);
        assertFalse(onEdt(host::isRunnerFocused));
        assertTrue(onEdt(host::isRunnerAttached));

        windows.focusResult = WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED;
        windows.focused = false;
        assertFalse(onEdt(host::requestRunnerFocus));
        assertEquals(List.of(30L, 30L), windows.focusRequests);
        assertFalse(onEdt(host::isRunnerFocused));
        assertTrue(onEdt(host::isRunnerAttached));
        assertTrue(onEdt(host::isRunnerSurfaceLive));
        assertEquals(0, failures.get());

        windows.releaseFocusResult =
                WindowsNativeCanvasApi.FocusResult.POLICY_REFUSED;
        assertFalse(onEdt(host::releaseRunnerFocus));
        assertEquals(List.of(10L, 10L), windows.focusReleases);
        assertTrue(onEdt(host::isRunnerAttached));
        assertEquals(0, failures.get());

        windows.deadWindows.add(30L);
        assertFalse(onEdt(host::requestRunnerFocus));
        assertFalse(onEdt(host::isRunnerAttached));
        assertEquals(1, failures.get());
    }

    @Test
    void invalidatesIdentityDriftAndInputQueueDetachFailureReportedByNativeBoundary()
            throws Exception {
        FakeWindowsApi identityDrift = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost driftHost = onEdt(
                () -> new WindowsNativeCanvasHost(identityDrift));
        AtomicInteger driftFailures = new AtomicInteger();
        onEdt(() -> {
            driftHost.onAttachmentFailed(ignored -> driftFailures.incrementAndGet());
            driftHost.tryAttach(77L).orElseThrow();
            return null;
        });
        identityDrift.focusResult = WindowsNativeCanvasApi.FocusResult.TARGET_INVALID;

        assertFalse(onEdt(driftHost::requestRunnerFocus));
        assertFalse(onEdt(driftHost::isRunnerAttached));
        assertEquals(1, driftFailures.get());

        FakeWindowsApi detachFailure = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost detachHost = onEdt(
                () -> new WindowsNativeCanvasHost(detachFailure));
        AtomicInteger detachFailures = new AtomicInteger();
        onEdt(() -> {
            detachHost.onAttachmentFailed(ignored -> detachFailures.incrementAndGet());
            detachHost.tryAttach(77L).orElseThrow();
            return null;
        });
        detachFailure.focusResult =
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED;

        assertFalse(onEdt(detachHost::requestRunnerFocus));
        assertFalse(onEdt(detachHost::isRunnerAttached));
        assertEquals(1, detachFailures.get());

        FakeWindowsApi releaseDetachFailure = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost releaseDetachHost = onEdt(
                () -> new WindowsNativeCanvasHost(releaseDetachFailure));
        AtomicInteger releaseDetachFailures = new AtomicInteger();
        onEdt(() -> {
            releaseDetachHost.onAttachmentFailed(
                    ignored -> releaseDetachFailures.incrementAndGet());
            releaseDetachHost.tryAttach(77L).orElseThrow();
            return null;
        });
        releaseDetachFailure.releaseFocusResult =
                WindowsNativeCanvasApi.FocusResult.INPUT_QUEUE_DETACH_FAILED;

        assertFalse(onEdt(releaseDetachHost::releaseRunnerFocus));
        assertFalse(onEdt(releaseDetachHost::isRunnerAttached));
        assertEquals(1, releaseDetachFailures.get());
    }

    @Test
    void publishesExactBoundsAndDprChangesWithoutDuplicateEvents()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        List<NativeCanvasSurfaceMetrics> metrics = new ArrayList<>();
        onEdt(() -> {
            host.onSurfaceMetricsChanged(metrics::add);
            host.tryAttach(77L).orElseThrow();
            return null;
        });

        assertEquals(List.of(new NativeCanvasSurfaceMetrics(
                640, 480, NativeCanvasSurfaceMetrics.MICROS_PER_UNIT)), metrics);
        assertEquals(metrics.getFirst(), onEdt(() -> host.surfaceMetrics().orElseThrow()));
        assertFalse(windows.dpiRequests.isEmpty());
        assertTrue(windows.dpiRequests.stream().allMatch(window -> window == 10L),
                "Target DPR must be read from the verified AWT parent HWND");

        windows.bounds = new NativeCanvasWindowBounds(800, 600);
        windows.dpi = 144;
        windows.dpiRequests.clear();
        onEdt(() -> {
            dispatchMove(host);
            dispatchMove(host);
            return null;
        });

        assertEquals(List.of(
                new NativeCanvasSurfaceMetrics(640, 480, 1_000_000),
                new NativeCanvasSurfaceMetrics(800, 600, 1_500_000)), metrics);
        assertFalse(windows.dpiRequests.isEmpty());
        assertTrue(windows.dpiRequests.stream().allMatch(window -> window == 10L),
                "DPR updates must remain bound to the verified AWT parent HWND");
    }

    @Test
    void invalidNativeDpiInvalidatesAttachmentAndReportsOneFailure()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        AtomicInteger failures = new AtomicInteger();
        windows.dpi = 0;

        Optional<NativeCanvasAttachment> result = onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            return host.tryAttach(77L);
        });

        assertTrue(result.isEmpty());
        assertFalse(onEdt(host::isRunnerAttached));
        assertEquals(1, failures.get());
    }

    @Test
    void serializesOneResizeInFlightAndCoalescesAwtBurstToLatestTarget()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        ManualResizeObservationScheduler scheduler =
                new ManualResizeObservationScheduler();
        WindowsNativeCanvasHost host = onEdt(
                () -> new WindowsNativeCanvasHost(windows, scheduler, 8));
        List<NativeCanvasSurfaceMetrics> metrics = new ArrayList<>();
        onEdt(() -> {
            host.onSurfaceMetricsChanged(metrics::add);
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.applyBoundsRequestsImmediately = false;

        windows.bounds = new NativeCanvasWindowBounds(700, 500);
        onEdt(() -> {
            dispatchResize(host);
            return null;
        });
        windows.bounds = new NativeCanvasWindowBounds(800, 600);
        onEdt(() -> {
            dispatchResize(host);
            return null;
        });
        windows.bounds = new NativeCanvasWindowBounds(900, 700);
        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        assertEquals(List.of(
                new NativeCanvasWindowBounds(640, 480),
                new NativeCanvasWindowBounds(700, 500)), windows.moves);
        assertEquals(new NativeCanvasSurfaceMetrics(900, 700, 1_000_000),
                metrics.getLast(),
                "The latest target must be published before native delivery");
        assertEquals(1, scheduler.pendingCount());

        windows.runnerBounds = new NativeCanvasWindowBounds(700, 500);
        onEdt(() -> {
            scheduler.runNext();
            return null;
        });
        assertEquals(2, windows.moves.size(),
                "FlutterView must settle too before the latest target is delivered");

        windows.flutterViewBounds = new NativeCanvasWindowBounds(700, 500);
        onEdt(() -> {
            scheduler.runNext();
            return null;
        });
        assertEquals(List.of(
                new NativeCanvasWindowBounds(640, 480),
                new NativeCanvasWindowBounds(700, 500),
                new NativeCanvasWindowBounds(900, 700)), windows.moves,
                "Only the newest queued target may follow the settled in-flight target");

        windows.runnerBounds = new NativeCanvasWindowBounds(900, 700);
        windows.flutterViewBounds = new NativeCanvasWindowBounds(900, 700);
        onEdt(() -> {
            scheduler.runNext();
            return null;
        });
        assertEquals(0, scheduler.pendingCount());
        assertTrue(onEdt(host::isRunnerAttached));
    }

    @Test
    void canceledOldGenerationObservationCannotAffectDetachedOrClosedHost()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        ManualResizeObservationScheduler scheduler =
                new ManualResizeObservationScheduler();
        WindowsNativeCanvasHost host = onEdt(
                () -> new WindowsNativeCanvasHost(windows, scheduler, 8));
        onEdt(() -> {
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.applyBoundsRequestsImmediately = false;
        windows.bounds = new NativeCanvasWindowBounds(700, 500);
        onEdt(() -> {
            dispatchResize(host);
            return null;
        });
        assertEquals(1, scheduler.pendingCount());
        onEdt(() -> {
            host.close();
            return null;
        });

        assertEquals(0, scheduler.pendingCount());
        onEdt(() -> {
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        assertEquals(1, scheduler.pendingCount());
        int movesBeforeStaleCallback = windows.moves.size();
        onEdt(() -> {
            scheduler.runFirstEvenIfCanceled();
            return null;
        });
        assertEquals(movesBeforeStaleCallback, windows.moves.size());
        assertEquals(1, scheduler.pendingCount(),
                "An old generation callback must not clear the new generation timer");
        assertTrue(onEdt(host::isRunnerAttached));

        onEdt(() -> {
            host.close();
            return null;
        });
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    void invalidatesOnceWhenBothChildWindowsCannotSettleWithinBound()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        ManualResizeObservationScheduler scheduler =
                new ManualResizeObservationScheduler();
        WindowsNativeCanvasHost host = onEdt(
                () -> new WindowsNativeCanvasHost(windows, scheduler, 3));
        AtomicInteger failures = new AtomicInteger();
        onEdt(() -> {
            host.onAttachmentFailed(ignored -> failures.incrementAndGet());
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.applyBoundsRequestsImmediately = false;
        windows.bounds = new NativeCanvasWindowBounds(700, 500);
        onEdt(() -> {
            dispatchResize(host);
            return null;
        });

        onEdt(() -> {
            scheduler.runNext();
            scheduler.runNext();
            return null;
        });

        assertFalse(onEdt(host::isRunnerAttached));
        assertEquals(1, failures.get());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    void pureParentDpiChangePublishesTargetAndReassertsBounds()
            throws Exception {
        FakeWindowsApi windows = FakeWindowsApi.attachable();
        windows.flutterViewDpi = 0;
        WindowsNativeCanvasHost host = onEdt(() -> new WindowsNativeCanvasHost(windows));
        List<NativeCanvasSurfaceMetrics> metrics = new ArrayList<>();
        onEdt(() -> {
            host.onSurfaceMetricsChanged(metrics::add);
            host.tryAttach(77L).orElseThrow();
            return null;
        });
        windows.dpiRequests.clear();
        windows.dpi = 144;

        onEdt(() -> {
            dispatchMove(host);
            return null;
        });

        assertEquals(List.of(
                new NativeCanvasWindowBounds(640, 480),
                new NativeCanvasWindowBounds(640, 480)), windows.moves);
        assertEquals(new NativeCanvasSurfaceMetrics(640, 480, 1_500_000),
                metrics.getLast());
        assertTrue(windows.dpiRequests.stream().allMatch(window -> window == 10L));
        assertTrue(onEdt(host::isRunnerAttached),
                "FlutterView DPI must not be consulted for the parent target");
    }

    @Test
    void normalizesZeroParentClientAreaAndRejectsExcessiveDpr()
            throws Exception {
        FakeWindowsApi zeroBounds = FakeWindowsApi.attachable();
        zeroBounds.bounds = new NativeCanvasWindowBounds(0, 0);
        zeroBounds.runnerBounds = new NativeCanvasWindowBounds(1, 1);
        zeroBounds.flutterViewBounds = new NativeCanvasWindowBounds(1, 1);
        WindowsNativeCanvasHost zeroHost = onEdt(
                () -> new WindowsNativeCanvasHost(zeroBounds));
        List<NativeCanvasSurfaceMetrics> metrics = new ArrayList<>();

        onEdt(() -> {
            zeroHost.onSurfaceMetricsChanged(metrics::add);
            zeroHost.tryAttach(77L).orElseThrow();
            return null;
        });

        assertEquals(List.of(new NativeCanvasWindowBounds(1, 1)), zeroBounds.moves);
        assertEquals(List.of(new NativeCanvasSurfaceMetrics(1, 1, 1_000_000)), metrics);

        FakeWindowsApi excessiveDpr = FakeWindowsApi.attachable();
        excessiveDpr.dpi = 961;
        WindowsNativeCanvasHost excessiveHost = onEdt(
                () -> new WindowsNativeCanvasHost(excessiveDpr));
        AtomicInteger failures = new AtomicInteger();
        Optional<NativeCanvasAttachment> result = onEdt(() -> {
            excessiveHost.onAttachmentFailed(ignored -> failures.incrementAndGet());
            return excessiveHost.tryAttach(77L);
        });
        assertTrue(result.isEmpty());
        assertEquals(1, failures.get());
    }

    private static void dispatchResize(WindowsNativeCanvasHost host) {
        Component canvas = host.getComponent(0);
        canvas.dispatchEvent(new ComponentEvent(canvas, ComponentEvent.COMPONENT_RESIZED));
    }

    private static void dispatchMove(WindowsNativeCanvasHost host) {
        Component canvas = host.getComponent(0);
        canvas.dispatchEvent(new ComponentEvent(canvas, ComponentEvent.COMPONENT_MOVED));
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
        if (failure.value instanceof Error error) {
            throw error;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return value.value;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class ManualResizeObservationScheduler
            implements WindowsNativeCanvasHost.ResizeObservationScheduler {
        private final List<ScheduledObservation> observations = new ArrayList<>();

        @Override
        public WindowsNativeCanvasHost.ResizeObservationHandle schedule(
                int delayMillis,
                Runnable task) {
            assertEquals(15, delayMillis);
            ScheduledObservation observation = new ScheduledObservation(task);
            observations.add(observation);
            return () -> observation.canceled = true;
        }

        int pendingCount() {
            return (int) observations.stream()
                    .filter(observation -> !observation.completed && !observation.canceled)
                    .count();
        }

        void runNext() {
            ScheduledObservation observation = observations.stream()
                    .filter(candidate -> !candidate.completed && !candidate.canceled)
                    .findFirst()
                    .orElseThrow();
            observation.completed = true;
            observation.task.run();
        }

        void runFirstEvenIfCanceled() {
            ScheduledObservation observation = observations.stream()
                    .filter(candidate -> !candidate.completed)
                    .findFirst()
                    .orElseThrow();
            observation.completed = true;
            observation.task.run();
        }

        private static final class ScheduledObservation {
            private final Runnable task;
            private boolean canceled;
            private boolean completed;

            private ScheduledObservation(Runnable task) {
                this.task = task;
            }
        }
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
        private final List<Long> focusRequests = new ArrayList<>();
        private final List<Long> focusReleases = new ArrayList<>();
        private final List<Long> dpiRequests = new ArrayList<>();
        private NativeCanvasWindowBounds bounds =
                new NativeCanvasWindowBounds(640, 480);
        private NativeCanvasWindowBounds runnerBounds = bounds;
        private NativeCanvasWindowBounds flutterViewBounds = bounds;
        private int dpi = 96;
        private int flutterViewDpi = 96;
        private boolean applyBoundsRequestsImmediately = true;
        private WindowsNativeCanvasApi.FocusResult focusResult =
                WindowsNativeCanvasApi.FocusResult.FOCUSED;
        private WindowsNativeCanvasApi.FocusResult releaseFocusResult =
                WindowsNativeCanvasApi.FocusResult.FOCUSED;
        private boolean focused;
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
            return switch ((int) window) {
                case 20 -> runnerBounds;
                case 30 -> flutterViewBounds;
                default -> bounds;
            };
        }

        @Override
        public int windowDpi(long window) {
            dpiRequests.add(window);
            return window == 10L ? dpi : flutterViewDpi;
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
            if (applyBoundsRequestsImmediately) {
                runnerBounds = bounds;
                flutterViewBounds = bounds;
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

        @Override
        public WindowsNativeCanvasApi.FocusResult requestFocus(
                NativeCanvasAttachment attachment) {
            focusRequests.add(attachment.flutterViewWindow());
            focused = focusResult == WindowsNativeCanvasApi.FocusResult.FOCUSED;
            return focusResult;
        }

        @Override
        public WindowsNativeCanvasApi.FocusResult releaseFocus(
                NativeCanvasAttachment attachment) {
            focusReleases.add(attachment.parentWindow());
            if (releaseFocusResult == WindowsNativeCanvasApi.FocusResult.FOCUSED) {
                focused = false;
            }
            return releaseFocusResult;
        }

        @Override
        public boolean isFocused(NativeCanvasAttachment attachment) {
            return focused && attachment.flutterViewWindow() == 30L;
        }
    }
}
