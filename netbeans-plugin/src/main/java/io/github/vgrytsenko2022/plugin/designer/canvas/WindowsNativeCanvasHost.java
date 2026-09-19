package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatform;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Heavyweight AWT host for a cross-process native Flutter child window.
 *
 * <p>The runner must create its window with this Canvas HWND as its parent.
 * This class deliberately never reparents a top-level window. It accepts only
 * one direct {@code WS_CHILD} window owned by the exact process launched for
 * this view.</p>
 */
public final class WindowsNativeCanvasHost extends JPanel implements NativeCanvasHost {
    private static final Logger LOGGER =
            Logger.getLogger(WindowsNativeCanvasHost.class.getName());
    private static final String RUNNER_WINDOW_CLASS = "FLUTTER_RUNNER_WIN32_WINDOW";
    private static final String FLUTTER_VIEW_WINDOW_CLASS = "FLUTTERVIEW";
    private static final int RESIZE_SETTLE_POLL_MILLIS = 15;
    private static final int RESIZE_SETTLE_MAX_OBSERVATIONS = 48;
    private static final int MAX_DEVICE_PIXEL_RATIO_MICROS = 10_000_000;
    private final WindowsNativeCanvasApi windows;
    private final HostCanvas canvas;
    private final ResizeObservationScheduler resizeObservationScheduler;
    private final int resizeSettleMaxObservations;
    private NativeCanvasAttachment attachment;
    private long attachmentGeneration;
    private long resizeRequestSequence;
    private ResizeTarget lastObservedResizeTarget;
    private ResizeTarget desiredResizeTarget;
    private ResizeRequest activeResizeRequest;
    private ResizeObservationHandle scheduledResizeObservation;
    private Runnable peerReady = () -> { };
    private Runnable peerWillBeLost = () -> { };
    private Runnable peerLost = () -> { };
    private Consumer<String> attachmentFailed = ignored -> { };
    private Consumer<NativeCanvasSurfaceMetrics> surfaceMetricsChanged = ignored -> { };
    private NativeCanvasSurfaceMetrics lastSurfaceMetrics;

    public WindowsNativeCanvasHost() {
        this(new JnaWindowsNativeCanvasApi());
    }

    WindowsNativeCanvasHost(WindowsNativeCanvasApi windows) {
        this(
                windows,
                WindowsNativeCanvasHost::scheduleResizeObservationOnEdt,
                RESIZE_SETTLE_MAX_OBSERVATIONS);
    }

    WindowsNativeCanvasHost(
            WindowsNativeCanvasApi windows,
            ResizeObservationScheduler resizeObservationScheduler,
            int resizeSettleMaxObservations) {
        super(new BorderLayout());
        this.windows = Objects.requireNonNull(windows, "windows");
        this.resizeObservationScheduler = Objects.requireNonNull(
                resizeObservationScheduler,
                "resizeObservationScheduler");
        if (resizeSettleMaxObservations <= 0) {
            throw new IllegalArgumentException(
                    "Resize settle observation count must be positive");
        }
        this.resizeSettleMaxObservations = resizeSettleMaxObservations;
        this.canvas = new HostCanvas();
        canvas.setBackground(new Color(0x20, 0x22, 0x24));
        canvas.setFocusable(true);
        // The heavyweight carrier is not a Java text component. The child
        // FLUTTERVIEW owns the native IME client and composing state, so an
        // overlapping AWT input-method pipeline would risk duplicate commits.
        canvas.enableInputMethods(false);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                refreshResizeTargetFromEvent();
            }

            @Override
            public void componentMoved(ComponentEvent event) {
                refreshResizeTargetFromEvent();
            }
        });
        canvas.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent event) {
                refreshResizeTargetFromEvent();
            }

            @Override
            public void ancestorResized(HierarchyEvent event) {
                refreshResizeTargetFromEvent();
            }
        });
        canvas.addPropertyChangeListener(
                "graphicsConfiguration",
                event -> refreshResizeTargetFromEvent());
        add(canvas, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Native Flutter Canvas host");
        getAccessibleContext().setAccessibleDescription(
                "Heavyweight host for the isolated native Flutter rendering surface.");
        canvas.getAccessibleContext().setAccessibleName("Native Flutter Canvas surface");
    }

    /** Returns the realized AWT Canvas HWND. Must be called on the EDT. */
    public long parentWindowHandle() {
        requireEventDispatchThread();
        if (!windows.isComponentDisplayable(canvas)) {
            throw new IllegalStateException(
                    "The native Flutter Canvas host is not displayable");
        }
        long parent = windows.componentWindow(canvas);
        if (!windows.isWindow(parent)) {
            throw new IllegalStateException(
                    "The AWT Canvas HWND is not a live native window");
        }
        return parent;
    }

    @Override
    public JPanel component() {
        return this;
    }

    @Override
    public NativeCanvasParentHandle parentHandle() {
        long window = parentWindowHandle();
        return new NativeCanvasParentHandle(
                NativeCanvasPlatform.WINDOWS,
                "0x" + String.format(java.util.Locale.ROOT, "%016X", window));
    }

    /**
     * Attaches the one direct child created by {@code runnerProcessId}.
     *
     * @return empty while the runner has not created its child window yet
     */
    public Optional<NativeCanvasAttachment> tryAttach(long runnerProcessId) {
        requireEventDispatchThread();
        if (runnerProcessId <= 0 || runnerProcessId > 0xffff_ffffL) {
            throw new IllegalArgumentException("Runner PID is outside the Win32 range");
        }
        long parent = parentWindowHandle();
        List<Long> candidates = windows.descendantWindows(parent).stream()
                .filter(window -> windows.parentWindow(window) == parent)
                .filter(window -> windows.ownerProcessId(window) == runnerProcessId)
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() != 1) {
            throw new IllegalStateException(
                    "The Flutter runner created " + candidates.size()
                    + " direct child windows; exactly one is required");
        }
        long child = candidates.get(0);
        verifyCandidate(parent, child, runnerProcessId);
        List<Long> flutterViews = windows.descendantWindows(child).stream()
                .filter(window -> windows.parentWindow(window) == child)
                .filter(window -> windows.ownerProcessId(window) == runnerProcessId)
                .toList();
        if (flutterViews.isEmpty()) {
            // CreateWindowEx publishes the wrapper HWND before Flutter finishes
            // creating its engine view. Keep the native surface hidden.
            return Optional.empty();
        }
        if (flutterViews.size() != 1) {
            throw new IllegalStateException(
                    "The Flutter runner created " + flutterViews.size()
                    + " direct engine view windows; exactly one is required");
        }
        long flutterView = flutterViews.get(0);
        verifyFlutterView(child, flutterView, runnerProcessId);
        NativeCanvasAttachment verified =
                new NativeCanvasAttachment(parent, child, flutterView, runnerProcessId);
        clearResizeState();
        attachmentGeneration = nextAttachmentGeneration(attachmentGeneration);
        attachment = verified;
        if (!refreshResizeTarget(verified, true)) {
            return Optional.empty();
        }
        try {
            windows.showWindow(child, isShowing());
        } catch (RuntimeException | LinkageError failure) {
            invalidateAttachment(
                    verified,
                    "The native Flutter Canvas child could not be shown after attachment.",
                    failure);
            return Optional.empty();
        }
        return Optional.of(verified);
    }

    @Override
    public boolean attachRunner(long runnerProcessId) {
        return tryAttach(runnerProcessId).isPresent();
    }

    public Optional<NativeCanvasAttachment> attachment() {
        requireEventDispatchThread();
        return Optional.ofNullable(attachment);
    }

    @Override
    public boolean isRunnerAttached() {
        requireEventDispatchThread();
        return attachment != null;
    }

    @Override
    public boolean isRunnerSurfaceLive() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        return current != null && isAttachmentLive(current);
    }

    @Override
    public boolean isNativePeerReady() {
        requireEventDispatchThread();
        return windows.isComponentDisplayable(canvas);
    }

    @Override
    public Optional<NativeCanvasSurfaceMetrics> surfaceMetrics() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return Optional.empty();
        }
        if (!isAttachmentLive(current)) {
            invalidateAttachment(
                    current,
                    "The native Flutter Canvas window hierarchy became invalid "
                    + "while reading surface metrics.",
                    null);
            return Optional.empty();
        }
        try {
            return Optional.of(readResizeTarget(current).surfaceMetrics());
        } catch (RuntimeException | LinkageError failure) {
            invalidateAttachment(
                    current,
                    "The native Flutter Canvas bounds or device-pixel ratio could not be read.",
                    failure);
            return Optional.empty();
        }
    }

    @Override
    public void setRunnerVisible(boolean visible) {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return;
        }
        try {
            if (!isAttachmentLive(current)) {
                invalidateAttachment(
                        current,
                        "The native Flutter Canvas window hierarchy became invalid "
                        + "while changing its visibility.",
                        null);
                return;
            }
            windows.showWindow(current.runnerWindow(), visible);
        } catch (RuntimeException | LinkageError failure) {
            invalidateAttachment(
                    current,
                    "The native Flutter Canvas child visibility operation failed.",
                    failure);
        }
    }

    @Override
    public boolean requestRunnerFocus() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return false;
        }
        try {
            if (!isAttachmentLive(current)) {
                invalidateAttachment(
                        current,
                        "The native Flutter Canvas window hierarchy became invalid "
                        + "while requesting focus.",
                        null);
                return false;
            }
            canvas.requestFocusInWindow();
            WindowsNativeCanvasApi.FocusResult result = windows.requestFocus(current);
            return switch (result) {
                case FOCUSED -> true;
                case POLICY_REFUSED -> {
                    // SetFocus is governed by Windows activation/input policy.
                    // A temporary refusal says nothing about HWND liveness.
                    LOGGER.log(
                            Level.FINE,
                            "Windows temporarily refused focus for the verified "
                            + "FlutterView HWND.");
                    yield false;
                }
                case TARGET_INVALID -> {
                    invalidateAttachment(
                            current,
                            "The native Flutter Canvas focus target no longer matched "
                            + "its verified parent, process, style or class identity.",
                            null);
                    yield false;
                }
                case INPUT_QUEUE_DETACH_FAILED -> {
                    // Until the target thread exits, a failed detach can leave
                    // AWT and Flutter sharing keyboard/focus state. Retire this
                    // generation instead of pretending this is a policy refusal.
                    invalidateAttachment(
                            current,
                            "Windows could not detach the AWT and Flutter input queues "
                            + "after a Canvas focus transfer.",
                            null);
                    yield false;
                }
            };
        } catch (RuntimeException | LinkageError failure) {
            // Focus is best-effort. Attachment ownership is invalidated only
            // by an explicit liveness failure, never merely by focus policy or
            // a transient native focus-call failure.
            LOGGER.log(
                    Level.WARNING,
                    "The native Flutter Canvas focus operation failed; the verified "
                    + "attachment remains owned by this host.",
                    failure);
            return false;
        }
    }

    @Override
    public boolean releaseRunnerFocus() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return false;
        }
        try {
            if (!isAttachmentLive(current)) {
                invalidateAttachment(
                        current,
                        "The native Flutter Canvas window hierarchy became invalid "
                        + "while releasing focus.",
                        null);
                return false;
            }
            WindowsNativeCanvasApi.FocusResult result = windows.releaseFocus(current);
            return switch (result) {
                case FOCUSED -> true;
                case POLICY_REFUSED -> {
                    // A newer unrelated foreground target wins over the menu
                    // transfer; this says nothing about HWND liveness.
                    LOGGER.log(
                            Level.FINE,
                            "Windows temporarily refused to release FlutterView "
                            + "focus to the verified AWT parent HWND.");
                    yield false;
                }
                case TARGET_INVALID -> {
                    invalidateAttachment(
                            current,
                            "The native Flutter Canvas focus-release target no longer "
                            + "matched its verified parent, JVM/runner process, style "
                            + "or class identity.",
                            null);
                    yield false;
                }
                case INPUT_QUEUE_DETACH_FAILED -> {
                    // A failed detach can leave AWT and Flutter sharing
                    // keyboard/focus state. Retire this generation fail-closed.
                    invalidateAttachment(
                            current,
                            "Windows could not detach the AWT and Flutter input queues "
                            + "after releasing Canvas focus.",
                            null);
                    yield false;
                }
            };
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.WARNING,
                    "The native Flutter Canvas focus-release operation failed; the "
                    + "verified attachment remains owned by this host.",
                    failure);
            return false;
        }
    }

    @Override
    public boolean isRunnerFocused() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return false;
        }
        try {
            if (!isAttachmentLive(current)) {
                invalidateAttachment(
                        current,
                        "The native Flutter Canvas window hierarchy became invalid "
                        + "while reading keyboard focus.",
                        null);
                return false;
            }
            return windows.isFocused(current);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.FINE,
                    "The native Flutter Canvas focus state could not be read.",
                    failure);
            return false;
        }
    }

    /** Invoked after the heavyweight peer is created and can supply a fresh HWND. */
    @Override
    public void onPeerReady(Runnable listener) {
        requireEventDispatchThread();
        peerReady = Objects.requireNonNull(listener, "listener");
    }

    /** Invoked before removeNotify invalidates the AWT HWND and its child. */
    @Override
    public void onPeerWillBeLost(Runnable listener) {
        requireEventDispatchThread();
        peerWillBeLost = Objects.requireNonNull(listener, "listener");
    }

    /** Invoked after the heavyweight peer disappears; the owner must stop its runner. */
    @Override
    public void onPeerLost(Runnable listener) {
        requireEventDispatchThread();
        peerLost = Objects.requireNonNull(listener, "listener");
    }

    /** Invoked when the AWT peer remains live but its verified child attachment fails. */
    @Override
    public void onAttachmentFailed(Consumer<String> listener) {
        requireEventDispatchThread();
        attachmentFailed = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void onSurfaceMetricsChanged(
            Consumer<NativeCanvasSurfaceMetrics> listener) {
        requireEventDispatchThread();
        surfaceMetricsChanged = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void detachRunner() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        attachment = null;
        attachmentGeneration = nextAttachmentGeneration(attachmentGeneration);
        clearResizeState();
        if (current == null) {
            return;
        }
        try {
            if (windows.isWindow(current.runnerWindow())) {
                windows.showWindow(current.runnerWindow(), false);
            }
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.WARNING,
                    "Cannot hide the native Flutter Canvas child while detaching its host.",
                    failure);
            // Ownership is already cleared, so propagation is safe and lets
            // the session surface an exact native-host cleanup failure.
            throw failure;
        }
    }

    @Override
    public void close() {
        detachRunner();
    }

    private void verifyCandidate(long parent, long child, long runnerProcessId) {
        if (!windows.isWindow(child)) {
            throw new IllegalStateException("The Flutter runner child HWND is no longer live");
        }
        if (windows.parentWindow(child) != parent) {
            throw new IllegalStateException(
                    "The Flutter runner HWND is not a direct child of this Canvas");
        }
        if (windows.ownerProcessId(child) != runnerProcessId) {
            throw new IllegalStateException(
                    "The Flutter runner HWND owner PID does not match the launched process");
        }
        if (!windows.hasChildStyle(child)) {
            throw new IllegalStateException("The Flutter runner HWND is not WS_CHILD");
        }
        if (!RUNNER_WINDOW_CLASS.equals(windows.windowClass(child))) {
            throw new IllegalStateException(
                    "The Flutter runner HWND has an unexpected window class");
        }
    }

    private void verifyFlutterView(long runner, long flutterView, long runnerProcessId) {
        if (!windows.isWindow(flutterView)
                || windows.parentWindow(flutterView) != runner
                || windows.ownerProcessId(flutterView) != runnerProcessId
                || !windows.hasChildStyle(flutterView)
                || !FLUTTER_VIEW_WINDOW_CLASS.equals(windows.windowClass(flutterView))) {
            throw new IllegalStateException(
                    "The native FlutterView HWND failed parent, PID, style or class verification");
        }
    }

    private boolean isAttachmentLive(NativeCanvasAttachment current) {
        try {
            return windows.isWindow(current.parentWindow())
                    && windows.isWindow(current.runnerWindow())
                    && windows.isWindow(current.flutterViewWindow())
                    && windows.parentWindow(current.runnerWindow()) == current.parentWindow()
                    && windows.parentWindow(current.flutterViewWindow())
                            == current.runnerWindow()
                    && windows.ownerProcessId(current.runnerWindow())
                            == current.runnerProcessId()
                    && windows.ownerProcessId(current.flutterViewWindow())
                            == current.runnerProcessId()
                    && windows.hasChildStyle(current.runnerWindow())
                    && windows.hasChildStyle(current.flutterViewWindow())
                    && RUNNER_WINDOW_CLASS.equals(
                            windows.windowClass(current.runnerWindow()))
                    && FLUTTER_VIEW_WINDOW_CLASS.equals(
                            windows.windowClass(current.flutterViewWindow()));
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    /** Event-listener fence: a native race must never escape onto the AWT EDT. */
    private void refreshResizeTargetFromEvent() {
        NativeCanvasAttachment expected = attachment;
        try {
            refreshResizeTarget(expected, false);
        } catch (Throwable failure) {
            invalidateAttachment(
                    expected,
                    "An unexpected native failure occurred while refreshing the Flutter "
                    + "Canvas resize or DPI target.",
                    failure);
        }
    }

    /**
     * Captures the latest physical parent-client target and publishes it before
     * any asynchronous native delivery. This lets the session close its input
     * fence for the new geometry while the runner is still processing the
     * previous resize.
     */
    private boolean refreshResizeTarget(
            NativeCanvasAttachment expected,
            boolean forceNativeDelivery) {
        if (expected == null || attachment != expected) {
            return false;
        }
        if (!isAttachmentLive(expected)) {
            invalidateAttachment(
                    expected,
                    "The native Flutter Canvas window hierarchy was no longer live "
                    + "while refreshing surface metrics.",
                    null);
            return false;
        }
        final ResizeTarget target;
        try {
            target = readResizeTarget(expected);
        } catch (RuntimeException | LinkageError failure) {
            invalidateAttachment(
                    expected,
                    "The native Flutter Canvas parent bounds or device-pixel ratio could "
                    + "not be read.",
                    failure);
            return false;
        }
        if (!publishSurfaceMetrics(expected, target.surfaceMetrics())) {
            return false;
        }
        boolean targetChanged = !target.equals(lastObservedResizeTarget);
        if (!forceNativeDelivery && !targetChanged) {
            return true;
        }
        lastObservedResizeTarget = target;
        if (activeResizeRequest != null
                && activeResizeRequest.target().equals(target)) {
            // A later AWT notification returned to the target already in
            // flight. Any intermediate desired target is obsolete.
            desiredResizeTarget = null;
        } else {
            desiredResizeTarget = target;
        }
        return issueLatestResizeIfIdle(expected);
    }

    private boolean publishSurfaceMetrics(
            NativeCanvasAttachment expected,
            NativeCanvasSurfaceMetrics metrics) {
        if (expected == null || attachment != expected) {
            return false;
        }
        if (metrics.equals(lastSurfaceMetrics)) {
            return true;
        }
        lastSurfaceMetrics = metrics;
        try {
            surfaceMetricsChanged.accept(metrics);
        } catch (Throwable listenerFailure) {
            LOGGER.log(
                    Level.WARNING,
                    "The native Flutter Canvas surface-metrics listener failed.",
                    listenerFailure);
        }
        return true;
    }

    private ResizeTarget readResizeTarget(
            NativeCanvasAttachment current) {
        NativeCanvasWindowBounds rawBounds = windows.clientBounds(current.parentWindow());
        NativeCanvasWindowBounds bounds = new NativeCanvasWindowBounds(
                Math.max(1, rawBounds.width()),
                Math.max(1, rawBounds.height()));
        int dpi = windows.windowDpi(current.parentWindow());
        if (dpi <= 0) {
            throw new IllegalStateException("The AWT Canvas HWND reported an invalid DPI");
        }
        long ratioMicros = Math.multiplyExact(
                (long) dpi,
                NativeCanvasSurfaceMetrics.MICROS_PER_UNIT) / 96L;
        if (ratioMicros <= 0 || ratioMicros > MAX_DEVICE_PIXEL_RATIO_MICROS) {
            throw new IllegalStateException(
                    "The AWT Canvas HWND reported a device-pixel ratio outside the "
                    + "supported (0, 10] range");
        }
        return new ResizeTarget(
                attachmentGeneration,
                bounds,
                dpi,
                new NativeCanvasSurfaceMetrics(
                        bounds.width(),
                        bounds.height(),
                        (int) ratioMicros));
    }

    /** Starts at most one asynchronous SetWindowPos request at a time. */
    private boolean issueLatestResizeIfIdle(NativeCanvasAttachment expected) {
        requireEventDispatchThread();
        if (expected == null || attachment != expected) {
            return false;
        }
        if (activeResizeRequest != null || desiredResizeTarget == null) {
            return true;
        }
        if (!isAttachmentLive(expected)) {
            invalidateAttachment(
                    expected,
                    "The native Flutter Canvas window hierarchy was no longer live "
                    + "before resize.",
                    null);
            return false;
        }
        ResizeTarget target = desiredResizeTarget;
        desiredResizeTarget = null;
        if (target.attachmentGeneration() != attachmentGeneration) {
            return false;
        }
        ResizeRequest request = new ResizeRequest(
                ++resizeRequestSequence,
                target,
                0);
        activeResizeRequest = request;
        try {
            if (!windows.setWindowBoundsAsync(
                    expected.runnerWindow(),
                    target.bounds())) {
                activeResizeRequest = null;
                boolean stillLive = isAttachmentLive(expected);
                invalidateAttachment(
                        expected,
                        stillLive
                                ? "SetWindowPos could not queue an asynchronous resize for a "
                                        + "verified live Flutter runner HWND."
                                : "The Flutter runner HWND disappeared while an asynchronous "
                                        + "SetWindowPos resize was being queued.",
                        null);
                return false;
            }
            observeActiveResize(expected, request);
            return attachment == expected;
        } catch (RuntimeException | LinkageError failure) {
            activeResizeRequest = null;
            boolean stillLive = isAttachmentLive(expected);
            invalidateAttachment(
                    expected,
                    stillLive
                            ? "The native Flutter Canvas resize failed while its HWND hierarchy "
                                    + "remained live."
                            : "The native Flutter Canvas HWND hierarchy disappeared during resize.",
                    failure);
            return false;
        }
    }

    /**
     * Waits until both the runner wrapper and the engine view expose the exact
     * in-flight target. A newer target remains latest-only and cannot be sent
     * until this request settles.
     */
    private void observeActiveResize(
            NativeCanvasAttachment expectedAttachment,
            ResizeRequest expectedRequest) {
        requireEventDispatchThread();
        if (attachment != expectedAttachment
                || activeResizeRequest != expectedRequest
                || expectedRequest.target().attachmentGeneration()
                        != attachmentGeneration) {
            return;
        }
        if (!isAttachmentLive(expectedAttachment)) {
            invalidateAttachment(
                    expectedAttachment,
                    "The native Flutter Canvas window hierarchy became invalid while "
                    + "settling an asynchronous resize.",
                    null);
            return;
        }
        NativeCanvasWindowBounds wrapperBounds;
        NativeCanvasWindowBounds flutterViewBounds;
        try {
            wrapperBounds = windows.clientBounds(expectedAttachment.runnerWindow());
            flutterViewBounds = windows.clientBounds(
                    expectedAttachment.flutterViewWindow());
        } catch (RuntimeException | LinkageError failure) {
            invalidateAttachment(
                    expectedAttachment,
                    "The native Flutter Canvas child bounds could not be observed while "
                    + "settling an asynchronous resize.",
                    failure);
            return;
        }
        NativeCanvasWindowBounds targetBounds = expectedRequest.target().bounds();
        if (targetBounds.equals(wrapperBounds)
                && targetBounds.equals(flutterViewBounds)) {
            activeResizeRequest = null;
            cancelScheduledResizeObservation();
            issueLatestResizeIfIdle(expectedAttachment);
            return;
        }
        int observationCount = expectedRequest.observationCount() + 1;
        if (observationCount >= resizeSettleMaxObservations) {
            activeResizeRequest = null;
            invalidateAttachment(
                    expectedAttachment,
                    "The native Flutter Canvas resize did not settle to "
                    + targetBounds.width() + "x" + targetBounds.height()
                    + " for both the runner wrapper and FlutterView within the bounded "
                    + "observation window.",
                    null);
            return;
        }
        ResizeRequest nextObservation = new ResizeRequest(
                expectedRequest.sequence(),
                expectedRequest.target(),
                observationCount);
        activeResizeRequest = nextObservation;
        scheduleResizeObservation(expectedAttachment, nextObservation);
    }

    private void scheduleResizeObservation(
            NativeCanvasAttachment expectedAttachment,
            ResizeRequest expectedRequest) {
        cancelScheduledResizeObservation();
        scheduledResizeObservation = resizeObservationScheduler.schedule(
                RESIZE_SETTLE_POLL_MILLIS,
                () -> {
                    // A canceled callback from an old attachment generation
                    // must not clear the handle of the current generation.
                    if (attachment == expectedAttachment
                            && activeResizeRequest == expectedRequest) {
                        scheduledResizeObservation = null;
                    }
                    try {
                        observeActiveResize(expectedAttachment, expectedRequest);
                    } catch (Throwable failure) {
                        invalidateAttachment(
                                expectedAttachment,
                                "An unexpected native failure occurred while observing a "
                                + "Flutter Canvas resize.",
                                failure);
                    }
                });
    }

    private void cancelScheduledResizeObservation() {
        ResizeObservationHandle scheduled = scheduledResizeObservation;
        scheduledResizeObservation = null;
        if (scheduled != null) {
            scheduled.cancel();
        }
    }

    private void clearResizeState() {
        cancelScheduledResizeObservation();
        activeResizeRequest = null;
        desiredResizeTarget = null;
        lastObservedResizeTarget = null;
        lastSurfaceMetrics = null;
    }

    private void invalidateAttachment(
            NativeCanvasAttachment expected,
            String reason,
            Throwable failure) {
        if (expected == null || attachment != expected) {
            return;
        }
        attachment = null;
        attachmentGeneration = nextAttachmentGeneration(attachmentGeneration);
        clearResizeState();
        if (failure == null) {
            LOGGER.log(Level.WARNING, reason);
        } else {
            LOGGER.log(Level.WARNING, reason, failure);
        }
        try {
            attachmentFailed.accept(reason);
        } catch (Throwable listenerFailure) {
            LOGGER.log(
                    Level.WARNING,
                    "The native Flutter Canvas attachment-failure listener failed after: "
                            + reason,
                    listenerFailure);
        }
    }

    private static void requireEventDispatchThread() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Native Flutter Canvas host operations must run on the EDT");
        }
    }

    private static long nextAttachmentGeneration(long current) {
        return current == Long.MAX_VALUE ? 1L : current + 1L;
    }

    private static ResizeObservationHandle scheduleResizeObservationOnEdt(
            int delayMillis,
            Runnable task) {
        Timer timer = new Timer(delayMillis, event -> task.run());
        timer.setRepeats(false);
        timer.start();
        return timer::stop;
    }

    @FunctionalInterface
    interface ResizeObservationScheduler {
        ResizeObservationHandle schedule(int delayMillis, Runnable task);
    }

    @FunctionalInterface
    interface ResizeObservationHandle {
        void cancel();
    }

    private record ResizeTarget(
            long attachmentGeneration,
            NativeCanvasWindowBounds bounds,
            int dpi,
            NativeCanvasSurfaceMetrics surfaceMetrics) {
    }

    private record ResizeRequest(
            long sequence,
            ResizeTarget target,
            int observationCount) {
    }

    private final class HostCanvas extends Canvas {
        @Override
        public void addNotify() {
            super.addNotify();
            try {
                peerReady.run();
            } catch (Throwable listenerFailure) {
                LOGGER.log(
                        Level.WARNING,
                        "The native Flutter Canvas peer-ready listener failed.",
                        listenerFailure);
            }
        }

        @Override
        public void removeNotify() {
            boolean hadPeer = isDisplayable();
            try {
                if (hadPeer) {
                    try {
                        peerWillBeLost.run();
                    } catch (Throwable listenerFailure) {
                        // The parent peer must still be released even when the
                        // bounded protocol-preparation callback fails.
                        LOGGER.log(
                                Level.WARNING,
                                "The native Flutter Canvas pre-peer-loss listener failed.",
                                listenerFailure);
                    }
                    try {
                        WindowsNativeCanvasHost.this.close();
                    } catch (RuntimeException | LinkageError failure) {
                        // A native hide/detach failure is diagnostic, but AWT
                        // peer teardown must still complete on the EDT.
                        LOGGER.log(
                                Level.WARNING,
                                "Native Flutter Canvas cleanup failed while its "
                                + "AWT peer was being removed.",
                                failure);
                    }
                }
            } finally {
                try {
                    super.removeNotify();
                } finally {
                    if (hadPeer) {
                        try {
                            peerLost.run();
                        } catch (Throwable listenerFailure) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "The native Flutter Canvas peer-loss listener failed.",
                                    listenerFailure);
                        }
                    }
                }
            }
        }
    }
}
