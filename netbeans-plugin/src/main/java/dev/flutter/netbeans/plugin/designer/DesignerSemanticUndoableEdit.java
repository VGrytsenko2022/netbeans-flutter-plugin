package dev.flutter.netbeans.plugin.designer;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * One native NetBeans history entry for one exact Designer model edge.
 *
 * <p>The optional document delegate is the single atomic edit captured from
 * NetBeans' {@code BaseDocument}; its individual managed-region replacements
 * are never admitted to the native history. The semantic replay controller
 * prepares an exact pair/model transition before the document delegate moves
 * and commits it only after that move succeeds. Outward publication is merely
 * queued here and must be drained by the combined-history owner after the
 * native manager and document locks are released.</p>
 */
final class DesignerSemanticUndoableEdit extends AbstractUndoableEdit {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(
            DesignerSemanticUndoableEdit.class.getName());

    private final long beforeRevisionId;
    private final long afterRevisionId;
    private final String presentationName;
    private final UndoableEdit documentDelegate;
    private final ReplayController replayController;
    private final PublicationQueue publicationQueue;
    private boolean usable = true;
    private boolean released;

    DesignerSemanticUndoableEdit(
            long beforeRevisionId,
            long afterRevisionId,
            String presentationName,
            UndoableEdit documentDelegate,
            ReplayController replayController,
            PublicationQueue publicationQueue) {
        if (beforeRevisionId < 0 || afterRevisionId < 0
                || beforeRevisionId == afterRevisionId) {
            throw new IllegalArgumentException(
                    "A Designer timeline edge requires distinct non-negative revision ids");
        }
        this.beforeRevisionId = beforeRevisionId;
        this.afterRevisionId = afterRevisionId;
        this.presentationName = requirePresentationName(presentationName);
        this.documentDelegate = documentDelegate;
        this.replayController = Objects.requireNonNull(
                replayController, "replayController");
        this.publicationQueue = Objects.requireNonNull(
                publicationQueue, "publicationQueue");
    }

    long beforeRevisionId() {
        return beforeRevisionId;
    }

    long afterRevisionId() {
        return afterRevisionId;
    }

    boolean hasDocumentDelegate() {
        return documentDelegate != null;
    }

    @Override
    public String getPresentationName() {
        return presentationName;
    }

    @Override
    public synchronized boolean canUndo() {
        return usable && super.canUndo()
                && (documentDelegate == null || documentDelegate.canUndo());
    }

    @Override
    public synchronized boolean canRedo() {
        return usable && super.canRedo()
                && (documentDelegate == null || documentDelegate.canRedo());
    }

    @Override
    public synchronized boolean isSignificant() {
        return usable;
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit next) {
        return false;
    }

    @Override
    public boolean replaceEdit(UndoableEdit previous) {
        return false;
    }

    @Override
    public void undo() throws CannotUndoException {
        replay(Direction.UNDO);
    }

    @Override
    public void redo() throws CannotRedoException {
        replay(Direction.REDO);
    }

    @Override
    public void die() {
        synchronized (this) {
            if (released) {
                return;
            }
            usable = false;
            released = true;
            super.die();
        }

        if (documentDelegate != null) {
            try {
                documentDelegate.die();
            } catch (RuntimeException | Error delegateFailure) {
                // UndoManager invokes die() while trimming a branch or its
                // bounded history. Resource cleanup must not corrupt that
                // native cursor even when a foreign delegate is broken.
                logWarningSafely(
                        "Cannot release the native Dart Undo delegate",
                        delegateFailure);
            }
        }
        try {
            replayController.release(beforeRevisionId, afterRevisionId);
        } catch (RuntimeException | Error releaseFailure) {
            // release() is contractually no-throw and monitor-only. Contain a
            // broken implementation because this edit has already died and
            // must never make native history trimming fail halfway through.
            logWarningSafely(
                    "Cannot release the Flutter Designer revision edge",
                    releaseFailure);
        }
    }

    private void replay(Direction direction) {
        long currentRevisionId = direction == Direction.UNDO
                ? afterRevisionId : beforeRevisionId;
        long targetRevisionId = direction == Direction.UNDO
                ? beforeRevisionId : afterRevisionId;
        PreparedReplay prepared;
        try {
            prepared = Objects.requireNonNull(
                    replayController.prepare(
                            direction,
                            currentRevisionId,
                            targetRevisionId),
                    "prepared replay");
        } catch (Throwable failure) {
            throw unavailable(direction, failure);
        }

        boolean editStateMoved = false;
        boolean documentMoved = false;
        boolean committed = false;
        try {
            if (direction == Direction.UNDO) {
                super.undo();
                editStateMoved = true;
                if (documentDelegate != null) {
                    documentDelegate.undo();
                    documentMoved = true;
                }
            } else {
                super.redo();
                editStateMoved = true;
                if (documentDelegate != null) {
                    documentDelegate.redo();
                    documentMoved = true;
                }
            }

            DeferredPublication publication = Objects.requireNonNull(
                    prepared.commit(), "deferred replay publication");
            committed = true;
            try {
                publicationQueue.enqueue(publication);
            } catch (Throwable publicationFailure) {
                // Semantic and document state are already jointly committed.
                // A presentation failure must never make UndoManager retain
                // the old cursor while the pair has moved to the new one.
                logWarningSafely(
                        "Cannot queue Flutter Designer Undo/Redo publication",
                        publicationFailure);
            }
        } catch (Throwable failure) {
            if (!committed) {
                recoverUncommittedReplay(
                        direction,
                        prepared,
                        editStateMoved,
                        documentMoved,
                        failure);
            }
            throw unavailable(direction, failure);
        }
    }

    private void recoverUncommittedReplay(
            Direction direction,
            PreparedReplay prepared,
            boolean editStateMoved,
            boolean documentMoved,
            Throwable primary) {
        boolean exactRecovery = true;
        // Restore this wrapper's AbstractUndoableEdit state first. Its
        // canUndo/canRedo guards intentionally include the delegate state; if
        // the delegate were restored first, super.redo()/super.undo() would
        // observe the already-restored delegate and reject exact rollback.
        if (editStateMoved) {
            try {
                if (direction == Direction.UNDO) {
                    super.redo();
                } else {
                    super.undo();
                }
            } catch (Throwable recoveryFailure) {
                primary.addSuppressed(recoveryFailure);
                exactRecovery = false;
            }
        }
        if (documentMoved && documentDelegate != null) {
            try {
                if (direction == Direction.UNDO) {
                    documentDelegate.redo();
                } else {
                    documentDelegate.undo();
                }
            } catch (Throwable recoveryFailure) {
                primary.addSuppressed(recoveryFailure);
                exactRecovery = false;
            }
        }
        if (exactRecovery) {
            try {
                prepared.abort();
            } catch (Throwable abortFailure) {
                primary.addSuppressed(abortFailure);
                exactRecovery = false;
            }
        }
        if (!exactRecovery) {
            try {
                prepared.invalidate(primary);
            } catch (Throwable invalidationFailure) {
                primary.addSuppressed(invalidationFailure);
            }
        }
    }

    private static RuntimeException unavailable(
            Direction direction,
            Throwable cause) {
        RuntimeException failure = direction == Direction.UNDO
                ? new CannotUndoException() : new CannotRedoException();
        failure.initCause(cause);
        return failure;
    }

    private static String requirePresentationName(String value) {
        String normalized = Objects.requireNonNull(
                value, "presentationName").strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "A Designer timeline entry requires a presentation name");
        }
        return normalized;
    }

    private static void logWarningSafely(String message, Throwable failure) {
        try {
            LOGGER.log(Level.WARNING, message, failure);
        } catch (RuntimeException | Error ignored) {
            // Logging is presentation-only. In particular, it must not turn a
            // committed replay into CannotUndo/CannotRedo or prevent die()
            // from releasing its semantic revision edge.
        }
    }

    enum Direction {
        UNDO,
        REDO
    }

    /** Pure preflight; it must not move model, pair or document state. */
    @FunctionalInterface
    interface ReplayController {
        PreparedReplay prepare(
                Direction direction,
                long currentRevisionId,
                long targetRevisionId) throws Exception;

        /**
         * Releases an edge removed by native branch truncation, history limits
         * or document close. Implementations must only update owned monitor
         * state, must not publish callbacks, and must not throw.
         */
        default void release(long beforeRevisionId, long afterRevisionId) {
        }
    }

    /**
     * One exact prepared replay.
     *
     * <p>{@link #commit} must either perform one no-throw joint state swap or
     * fail before changing semantic state. {@link #abort} retains the exact
     * current revision. {@link #invalidate} closes unsafe writable authority
     * when document/edit-state recovery cannot be proven.</p>
     */
    interface PreparedReplay {
        DeferredPublication commit() throws Exception;

        void abort();

        void invalidate(Throwable failure);
    }

    @FunctionalInterface
    interface DeferredPublication {
        void publish();
    }

    /**
     * Must enqueue only; publication under the native manager lock is
     * forbidden. Ownership transfer is all-or-none: normal return means the
     * queue retained the publication, while a thrown failure means it retained
     * nothing and will never publish it.
     */
    @FunctionalInterface
    interface PublicationQueue {
        void enqueue(DeferredPublication publication);
    }
}
