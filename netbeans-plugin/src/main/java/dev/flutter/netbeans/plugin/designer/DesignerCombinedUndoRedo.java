package dev.flutter.netbeans.plugin.designer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.openide.awt.UndoRedo;
import org.openide.util.ChangeSupport;

/**
 * Stable MultiView Undo/Redo identity over the native Dart editor history.
 *
 * <p>The native editor manager is the sole public Undo/Redo delegate for the
 * complete DataObject lifetime. A Designer command session may acquire one
 * exclusive package-private lifetime token, but that token neither selects a
 * second history nor installs a listener on it. Designer semantic callbacks
 * are joined to native document barriers separately.</p>
 */
final class DesignerCombinedUndoRedo implements UndoRedo, ChangeListener {
    private static final Logger LOGGER = Logger.getLogger(
            DesignerCombinedUndoRedo.class.getName());

    private final Object monitor = new Object();
    private final UndoRedo sourceHistory;
    private final ChangeSupport changes = new ChangeSupport(this);
    private SessionBinding activeBinding;
    private int notificationDeferralDepth;
    private boolean notificationPending;
    private boolean notificationPublishing;
    private boolean historyActionRunning;
    private final ArrayDeque<DesignerSemanticUndoableEdit.DeferredPublication>
            semanticPublications = new ArrayDeque<>();

    DesignerCombinedUndoRedo(UndoRedo sourceHistory) {
        this.sourceHistory = Objects.requireNonNull(sourceHistory, "sourceHistory");
        sourceHistory.addChangeListener(this);
    }

    /**
     * Acquires one exact Designer command-session lifetime token. Public
     * Undo/Redo delegation remains bound to {@link #sourceHistory}.
     */
    SessionBinding bindDesignerSession(UndoRedo sessionOwnerIdentity) {
        Objects.requireNonNull(sessionOwnerIdentity, "sessionOwnerIdentity");
        if (sessionOwnerIdentity == sourceHistory) {
            throw new IllegalArgumentException(
                    "Designer session identity must be distinct from native history");
        }

        SessionBinding binding;
        synchronized (monitor) {
            boolean rejected = activeBinding != null
                    || historyActionRunning
                    || notificationDeferralDepth > 0
                    || notificationPublishing;
            if (rejected) {
                throw new IllegalStateException(
                        "A Designer command session already owns its lifetime token");
            }
            binding = new SessionBinding();
            activeBinding = binding;
        }
        return binding;
    }

    /**
     * Coalesces outward Undo/Redo notifications until an internal document
     * transaction has released its NetBeans document lock.
     *
     * <p>The native editor manager still receives and records every edit at
     * the exact document boundary. Only this DataObject-owned bridge delays
     * the UI notification, so Source and Design retain the standard NetBeans
     * undo manager semantics without invoking MultiView listeners under the
     * guarded document lock.</p>
     */
    NotificationDeferral deferNotifications() {
        synchronized (monitor) {
            if (notificationPublishing || historyActionRunning) {
                throw new IllegalStateException(
                        "Cannot start a document transaction from an active "
                        + "Undo/Redo notification or action");
            }
            notificationDeferralDepth++;
        }
        return new NotificationDeferral();
    }

    /**
     * Queues a completed pair/model publication until the native history or
     * document barrier has released every internal lock.
     */
    void enqueueSemanticPublication(
            DesignerSemanticUndoableEdit.DeferredPublication publication) {
        Objects.requireNonNull(publication, "publication");
        synchronized (monitor) {
            if (notificationPublishing
                    || (!historyActionRunning
                        && notificationDeferralDepth <= 0)) {
                throw new IllegalStateException(
                        "A Designer semantic publication requires an active Undo/Redo or document barrier");
            }
            semanticPublications.addLast(publication);
            notificationPending = true;
        }
    }

    boolean designerSessionActive() {
        synchronized (monitor) {
            return activeBinding != null;
        }
    }

    @Override
    public boolean canUndo() {
        UndoRedo history = queryHistory();
        return history != null && history.canUndo();
    }

    @Override
    public boolean canRedo() {
        UndoRedo history = queryHistory();
        return history != null && history.canRedo();
    }

    @Override
    public void undo() throws CannotUndoException {
        UndoRedo history = beginHistoryAction(true);
        try {
            history.undo();
        } finally {
            finishHistoryAction();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        UndoRedo history = beginHistoryAction(false);
        try {
            history.redo();
        } finally {
            finishHistoryAction();
        }
    }

    @Override
    public String getUndoPresentationName() {
        UndoRedo history = queryHistory();
        return history == null
                ? "Undo" : history.getUndoPresentationName();
    }

    @Override
    public String getRedoPresentationName() {
        UndoRedo history = queryHistory();
        return history == null
                ? "Redo" : history.getRedoPresentationName();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() == sourceHistory) {
            fireOrDeferChange();
        }
    }

    private void fireOrDeferChange() {
        boolean publish;
        synchronized (monitor) {
            if (notificationDeferralDepth > 0 || notificationPublishing) {
                notificationPending = true;
                return;
            }
            notificationPublishing = true;
            publish = true;
        }
        if (publish) {
            drainNotifications();
        }
    }

    private void drainNotifications() {
        boolean again = true;
        while (again) {
            try {
                changes.fireChange();
            } catch (Throwable failure) {
                logCallbackFailureSafely(
                        "A Flutter Designer Undo/Redo presentation listener failed",
                        failure);
            }
            synchronized (monitor) {
                again = notificationDeferralDepth == 0 && notificationPending;
                if (again) {
                    notificationPending = false;
                } else {
                    notificationPublishing = false;
                }
            }
        }
    }

    private UndoRedo queryHistory() {
        synchronized (monitor) {
            return historyActionRunning ? null : sourceHistory;
        }
    }

    private UndoRedo beginHistoryAction(boolean undo) {
        synchronized (monitor) {
            if (historyActionRunning
                    || notificationDeferralDepth > 0
                    || notificationPublishing) {
                if (undo) {
                    throw new CannotUndoException();
                }
                throw new CannotRedoException();
            }
            historyActionRunning = true;
            notificationDeferralDepth++;
            return sourceHistory;
        }
    }

    private void finishHistoryAction() {
        boolean publish = false;
        List<DesignerSemanticUndoableEdit.DeferredPublication> semantic = List.of();
        synchronized (monitor) {
            if (!historyActionRunning || notificationDeferralDepth <= 0) {
                throw new IllegalStateException(
                        "Undo/Redo action ownership is not active");
            }
            historyActionRunning = false;
            if (activeBinding != null && activeBinding.closeRequested) {
                activeBinding = null;
            }
            notificationDeferralDepth--;
            if (notificationDeferralDepth == 0
                    && (notificationPending || !semanticPublications.isEmpty())) {
                notificationPending = false;
                if (notificationPublishing) {
                    throw new IllegalStateException(
                            "Undo/Redo publication overlapped an owned action");
                }
                notificationPublishing = true;
                publish = true;
                semantic = drainSemanticPublicationsLocked();
            }
        }
        if (publish) {
            publishSemanticThenNotifications(semantic);
        }
    }

    private List<DesignerSemanticUndoableEdit.DeferredPublication>
            drainSemanticPublicationsLocked() {
        if (semanticPublications.isEmpty()) {
            return List.of();
        }
        ArrayList<DesignerSemanticUndoableEdit.DeferredPublication> drained =
                new ArrayList<>(semanticPublications.size());
        while (!semanticPublications.isEmpty()) {
            drained.add(semanticPublications.removeFirst());
        }
        return List.copyOf(drained);
    }

    private void publishSemanticThenNotifications(
            List<DesignerSemanticUndoableEdit.DeferredPublication> semantic) {
        try {
            for (DesignerSemanticUndoableEdit.DeferredPublication publication
                    : semantic) {
                try {
                    publication.publish();
                } catch (Throwable failure) {
                    // One presentation callback cannot cancel later committed
                    // semantic publications from the same barrier. Reporting
                    // is itself fail-safe because a broken logging handler must
                    // not retain notificationPublishing ownership either.
                    logCallbackFailureSafely(
                            "A committed Flutter Designer semantic publication failed",
                            failure);
                }
            }
        } finally {
            synchronized (monitor) {
                // Native-history and every semantic callback from this barrier
                // collapse into the one presentation edge below. Reentrant
                // native changes raised while semantics ran are represented by
                // that same edge, while changes raised from the presentation
                // callback itself remain pending for drainNotifications().
                notificationPending = false;
            }
            drainNotifications();
        }
    }

    private static void logCallbackFailureSafely(
            String message, Throwable failure) {
        try {
            LOGGER.log(Level.WARNING, message, failure);
        } catch (Throwable loggingFailure) {
            try {
                if (loggingFailure != failure) {
                    failure.addSuppressed(loggingFailure);
                }
            } catch (Throwable ignored) {
                // Nothing else is allowed to escape a callback-failure path.
            }
        }
    }

    final class SessionBinding implements AutoCloseable {
        private boolean closed;
        private boolean closeRequested;

        private SessionBinding() {
        }

        @Override
        public void close() {
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                closed = true;
                if (activeBinding != this) {
                    return;
                }
                if (historyActionRunning || notificationDeferralDepth > 0) {
                    closeRequested = true;
                    return;
                }
                activeBinding = null;
            }
        }
    }

    final class NotificationDeferral implements AutoCloseable {
        private boolean closed;

        private NotificationDeferral() {
        }

        @Override
        public void close() {
            boolean publish = false;
            List<DesignerSemanticUndoableEdit.DeferredPublication> semantic =
                    List.of();
            synchronized (monitor) {
                if (closed) {
                    return;
                }
                closed = true;
                if (notificationDeferralDepth <= 0) {
                    throw new IllegalStateException(
                            "Undo/Redo notification deferral is not active");
                }
                notificationDeferralDepth--;
                if (notificationDeferralDepth == 0
                        && activeBinding != null
                        && activeBinding.closeRequested) {
                    activeBinding = null;
                }
                if (notificationDeferralDepth == 0
                        && (notificationPending
                            || !semanticPublications.isEmpty())) {
                    notificationPending = false;
                    if (notificationPublishing) {
                        throw new IllegalStateException(
                                "Undo/Redo notification publication overlapped "
                                + "a document transaction");
                    }
                    notificationPublishing = true;
                    publish = true;
                    semantic = drainSemanticPublicationsLocked();
                }
            }
            if (publish) {
                // The document transaction has already completed. A broken
                // presentation listener must not turn its successful semantic
                // result into an unverifiable apply/restore failure.
                publishSemanticThenNotifications(semantic);
            }
        }
    }
}
