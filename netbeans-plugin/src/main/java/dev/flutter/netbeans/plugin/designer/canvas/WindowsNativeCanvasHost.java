package dev.flutter.netbeans.plugin.designer.canvas;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * Heavyweight AWT host for a cross-process native Flutter child window.
 *
 * <p>The runner must create its window with this Canvas HWND as its parent.
 * This class deliberately never reparents a top-level window. It accepts only
 * one direct {@code WS_CHILD} window owned by the exact process launched for
 * this view.</p>
 */
public final class WindowsNativeCanvasHost extends JPanel implements AutoCloseable {
    private static final Logger LOGGER =
            Logger.getLogger(WindowsNativeCanvasHost.class.getName());
    private static final String RUNNER_WINDOW_CLASS = "FLUTTER_RUNNER_WIN32_WINDOW";
    private static final String FLUTTER_VIEW_WINDOW_CLASS = "FLUTTERVIEW";
    private final WindowsNativeCanvasApi windows;
    private final HostCanvas canvas;
    private NativeCanvasAttachment attachment;
    private Runnable peerReady = () -> { };
    private Runnable peerLost = () -> { };
    private Consumer<String> attachmentFailed = ignored -> { };

    public WindowsNativeCanvasHost() {
        this(new JnaWindowsNativeCanvasApi());
    }

    WindowsNativeCanvasHost(WindowsNativeCanvasApi windows) {
        super(new BorderLayout());
        this.windows = Objects.requireNonNull(windows, "windows");
        this.canvas = new HostCanvas();
        canvas.setBackground(new Color(0x20, 0x22, 0x24));
        canvas.setFocusable(true);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                resizeAttachedWindowFromEvent();
            }
        });
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
        attachment = verified;
        if (!resizeAttachedWindow()) {
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

    public Optional<NativeCanvasAttachment> attachment() {
        requireEventDispatchThread();
        return Optional.ofNullable(attachment);
    }

    public boolean isNativePeerReady() {
        requireEventDispatchThread();
        return windows.isComponentDisplayable(canvas);
    }

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

    /** Invoked after the heavyweight peer is created and can supply a fresh HWND. */
    public void onPeerReady(Runnable listener) {
        requireEventDispatchThread();
        peerReady = Objects.requireNonNull(listener, "listener");
    }

    /** Invoked after the heavyweight peer disappears; the owner must stop its runner. */
    public void onPeerLost(Runnable listener) {
        requireEventDispatchThread();
        peerLost = Objects.requireNonNull(listener, "listener");
    }

    /** Invoked when the AWT peer remains live but its verified child attachment fails. */
    public void onAttachmentFailed(Consumer<String> listener) {
        requireEventDispatchThread();
        attachmentFailed = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void close() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        attachment = null;
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
                    "Cannot hide the native Flutter Canvas child while closing its host.",
                    failure);
        }
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
    private void resizeAttachedWindowFromEvent() {
        try {
            resizeAttachedWindow();
        } catch (Throwable failure) {
            invalidateAttachment(
                    attachment,
                    "An unexpected native failure occurred while resizing the Flutter Canvas.",
                    failure);
        }
    }

    private boolean resizeAttachedWindow() {
        requireEventDispatchThread();
        NativeCanvasAttachment current = attachment;
        if (current == null) {
            return false;
        }
        if (!isAttachmentLive(current)) {
            invalidateAttachment(
                    current,
                    "The native Flutter Canvas window hierarchy was no longer live "
                    + "before resize.",
                    null);
            return false;
        }
        try {
            NativeCanvasWindowBounds bounds = windows.clientBounds(current.parentWindow());
            if (windows.moveWindow(current.runnerWindow(), bounds)) {
                return true;
            }
            boolean stillLive = isAttachmentLive(current);
            invalidateAttachment(
                    current,
                    stillLive
                            ? "MoveWindow failed for a verified live Flutter runner HWND."
                            : "The Flutter runner HWND disappeared while MoveWindow was resizing it.",
                    null);
            return false;
        } catch (RuntimeException | LinkageError failure) {
            boolean stillLive = isAttachmentLive(current);
            invalidateAttachment(
                    current,
                    stillLive
                            ? "The native Flutter Canvas resize failed while its HWND hierarchy "
                                    + "remained live."
                            : "The native Flutter Canvas HWND hierarchy disappeared during resize.",
                    failure);
            return false;
        }
    }

    private void invalidateAttachment(
            NativeCanvasAttachment expected,
            String reason,
            Throwable failure) {
        if (expected == null || attachment != expected) {
            return;
        }
        attachment = null;
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
                    WindowsNativeCanvasHost.this.close();
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
