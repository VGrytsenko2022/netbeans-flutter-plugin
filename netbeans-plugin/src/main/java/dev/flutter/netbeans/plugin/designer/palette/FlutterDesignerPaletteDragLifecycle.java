package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.openide.util.RequestProcessor;

/**
 * Owns the process-global AWT drag-end listener for one open Designer view.
 *
 * <p>The listener accepts only the exact bounded opaque token format of its
 * registry. Canceled and failed drags are revoked synchronously. A successful
 * native MOVE is allowed a short asynchronous grace interval so the Canvas
 * runner can return the admitted drop before the token is revoked.</p>
 */
public final class FlutterDesignerPaletteDragLifecycle implements AutoCloseable {
    static final Duration DEFAULT_SUCCESS_GRACE = Duration.ofSeconds(3);
    private static final Duration MAX_SUCCESS_GRACE = Duration.ofSeconds(10);

    private final FlutterDesignerPaletteDragRegistry registry;
    private final Duration successGrace;
    private final GlobalListenerAccess globalListenerAccess;
    private final GraceScheduler scheduler;
    private final DragSourceListener listener = new OwningDragSourceListener();
    private final Map<String, PendingExpiry> pendingExpiries = new LinkedHashMap<>();
    private long expirySequence;
    private boolean installed;
    private boolean closed;

    public FlutterDesignerPaletteDragLifecycle(
            FlutterDesignerPaletteDragRegistry registry) {
        this(
                registry,
                DEFAULT_SUCCESS_GRACE,
                new AwtGlobalListenerAccess(),
                new RequestProcessorGraceScheduler());
    }

    FlutterDesignerPaletteDragLifecycle(
            FlutterDesignerPaletteDragRegistry registry,
            Duration successGrace,
            GlobalListenerAccess globalListenerAccess,
            GraceScheduler scheduler) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.successGrace = requireSuccessGrace(successGrace);
        this.globalListenerAccess = Objects.requireNonNull(
                globalListenerAccess, "globalListenerAccess");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** Installs exactly one global listener, returning false when AWT DnD is unavailable. */
    public synchronized boolean install() {
        if (closed) {
            return false;
        }
        if (installed) {
            return true;
        }
        installed = globalListenerAccess.install(listener);
        if (!installed) {
            cancelPendingAndRevoke();
        }
        return installed;
    }

    /** True only while this view owns its installed process-global listener. */
    public synchronized boolean isInstalled() {
        return installed && !closed;
    }

    /** Consumes a token exactly once and cancels its no-longer-needed grace task. */
    public synchronized Optional<WidgetTypeId> consume(String token) {
        PendingExpiry pending = pendingExpiries.remove(token);
        if (pending != null) {
            pending.cancellable().cancel();
        }
        return registry.consume(token);
    }

    /** Cancels pending grace tasks and revokes all authority without uninstalling. */
    public synchronized void revokeAll() {
        cancelPendingAndRevoke();
    }

    /**
     * Removes the listener and invalidates all outstanding drag authority.
     * The lifecycle may be installed again if the same view is reopened.
     */
    public synchronized void uninstall() {
        if (installed) {
            globalListenerAccess.uninstall(listener);
            installed = false;
        }
        cancelPendingAndRevoke();
    }

    /** Permanently releases the listener, pending tasks and scheduler. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        uninstall();
        closed = true;
        scheduler.close();
    }

    void dragEnded(Transferable transferable, int dropAction, boolean dropSuccess) {
        Optional<String> token = extractOwnBoundedToken(transferable);
        if (token.isEmpty()) {
            return;
        }
        String exactToken = token.orElseThrow();
        if (!dropSuccess || dropAction != DnDConstants.ACTION_MOVE) {
            revokeImmediately(exactToken);
            return;
        }
        retainForSuccessGrace(exactToken);
    }

    private Optional<String> extractOwnBoundedToken(Transferable transferable) {
        if (transferable == null || registry.outstandingCount() == 0) {
            return Optional.empty();
        }
        try {
            if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return Optional.empty();
            }
            Object value = transferable.getTransferData(DataFlavor.stringFlavor);
            if (!(value instanceof String token)
                    || token.length() != FlutterDesignerPaletteDragRegistry.TOKEN_LENGTH
                    || !registry.hasLocalTokenShape(token)) {
                return Optional.empty();
            }
            return Optional.of(token);
        } catch (IOException | java.awt.datatransfer.UnsupportedFlavorException
                | RuntimeException failure) {
            return Optional.empty();
        }
    }

    private synchronized void revokeImmediately(String token) {
        PendingExpiry pending = pendingExpiries.remove(token);
        if (pending != null) {
            pending.cancellable().cancel();
        }
        registry.revoke(token);
    }

    private synchronized void retainForSuccessGrace(String token) {
        if (!installed || closed) {
            registry.revoke(token);
            return;
        }
        PendingExpiry previous = pendingExpiries.remove(token);
        if (previous != null) {
            previous.cancellable().cancel();
        }
        long generation = ++expirySequence;
        try {
            Cancellable cancellable = scheduler.schedule(
                    () -> expire(token, generation), successGrace);
            pendingExpiries.put(token, new PendingExpiry(generation, cancellable));
        } catch (RuntimeException rejected) {
            registry.revoke(token);
        }
    }

    private synchronized void expire(String token, long generation) {
        PendingExpiry pending = pendingExpiries.get(token);
        if (pending == null || pending.generation() != generation) {
            return;
        }
        pendingExpiries.remove(token);
        registry.revoke(token);
    }

    private void cancelPendingAndRevoke() {
        pendingExpiries.values().forEach(pending -> pending.cancellable().cancel());
        pendingExpiries.clear();
        registry.revokeAll();
    }

    private static Duration requireSuccessGrace(Duration grace) {
        Objects.requireNonNull(grace, "successGrace");
        if (grace.isNegative()
                || grace.isZero()
                || grace.compareTo(MAX_SUCCESS_GRACE) > 0) {
            throw new IllegalArgumentException(
                    "successGrace must be positive and no longer than "
                    + MAX_SUCCESS_GRACE);
        }
        return grace;
    }

    interface GlobalListenerAccess {
        boolean install(DragSourceListener listener);

        void uninstall(DragSourceListener listener);
    }

    interface GraceScheduler extends AutoCloseable {
        Cancellable schedule(Runnable task, Duration delay);

        @Override
        void close();
    }

    interface Cancellable {
        void cancel();
    }

    private record PendingExpiry(long generation, Cancellable cancellable) {
        private PendingExpiry {
            Objects.requireNonNull(cancellable, "cancellable");
        }
    }

    private final class OwningDragSourceListener implements DragSourceListener {
        @Override
        public void dragDropEnd(DragSourceDropEvent event) {
            if (event == null || event.getDragSourceContext() == null) {
                return;
            }
            dragEnded(
                    event.getDragSourceContext().getTransferable(),
                    event.getDropAction(),
                    event.getDropSuccess());
        }

        @Override
        public void dragEnter(DragSourceDragEvent event) {
        }

        @Override
        public void dragOver(DragSourceDragEvent event) {
        }

        @Override
        public void dropActionChanged(DragSourceDragEvent event) {
        }

        @Override
        public void dragExit(DragSourceEvent event) {
        }
    }

    static final class AwtGlobalListenerAccess
            implements GlobalListenerAccess {
        private final BooleanSupplier headless;
        private final Supplier<DragSource> dragSourceSupplier;
        private DragSource dragSource;

        AwtGlobalListenerAccess() {
            this(GraphicsEnvironment::isHeadless, DragSource::getDefaultDragSource);
        }

        AwtGlobalListenerAccess(
                BooleanSupplier headless,
                Supplier<DragSource> dragSourceSupplier) {
            this.headless = Objects.requireNonNull(headless, "headless");
            this.dragSourceSupplier = Objects.requireNonNull(
                    dragSourceSupplier, "dragSourceSupplier");
        }

        @Override
        public boolean install(DragSourceListener listener) {
            if (headless.getAsBoolean()) {
                return false;
            }
            try {
                dragSource = Objects.requireNonNull(
                        dragSourceSupplier.get(), "dragSourceSupplier result");
                dragSource.addDragSourceListener(listener);
                return true;
            } catch (HeadlessException | SecurityException unavailable) {
                dragSource = null;
                return false;
            }
        }

        @Override
        public void uninstall(DragSourceListener listener) {
            DragSource source = dragSource;
            dragSource = null;
            if (source != null) {
                source.removeDragSourceListener(listener);
            }
        }
    }

    private static final class RequestProcessorGraceScheduler
            implements GraceScheduler {
        private static final RequestProcessor WORKER = new RequestProcessor(
                FlutterDesignerPaletteDragLifecycle.class.getName(), 1, true);

        @Override
        public Cancellable schedule(Runnable task, Duration delay) {
            RequestProcessor.Task scheduled = WORKER.create(task);
            scheduled.schedule(Math.toIntExact(delay.toMillis()));
            return scheduled::cancel;
        }

        @Override
        public void close() {
        }
    }
}
