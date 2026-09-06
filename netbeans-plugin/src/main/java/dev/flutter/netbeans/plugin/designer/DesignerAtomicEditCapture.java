package dev.flutter.netbeans.plugin.designer;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.undo.UndoableEdit;
import org.openide.awt.UndoRedo;

/**
 * One exact EDT/document token consumed by the Dart MIME Undo wrapper.
 *
 * <p>The native CES manager remains attached. The token is visible only
 * through an identity-valued document property and the owning EDT thread. A
 * candidate finalizer seals it before the outer atomic unlock; the MIME
 * wrapper then substitutes one semantic edit. A private one-shot manager
 * listener acknowledges native admission before the joint pair/model state
 * swap is allowed to become authoritative.</p>
 */
final class DesignerAtomicEditCapture implements ChangeListener, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(
            DesignerAtomicEditCapture.class.getName());
    private static final Object DOCUMENT_PROPERTY = new Object();
    private static final ThreadLocal<DesignerAtomicEditCapture> ACTIVE =
            new ThreadLocal<>();

    private final AbstractDocument document;
    private final UndoRedo.Manager nativeHistory;
    private final AtomicReference<IOException> failure = new AtomicReference<>();
    private State state = State.ACTIVE;
    private ArmedSemanticEdit armed;
    private DesignerSemanticUndoableEdit semanticEdit;
    /** Retained only when the queue rejected ownership before enqueue. */
    private DesignerSemanticUndoableEdit.DeferredPublication
            postWriteFallbackPublication;
    private Throwable publicationEnqueueFailure;
    private boolean managerListenerAttached;

    private DesignerAtomicEditCapture(
            AbstractDocument document,
            UndoRedo.Manager nativeHistory) {
        this.document = document;
        this.nativeHistory = nativeHistory;
    }

    static DesignerAtomicEditCapture begin(
            Document document,
            UndoRedo.Manager nativeHistory) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(nativeHistory, "nativeHistory");
        requireEdt();
        if (ACTIVE.get() != null) {
            throw new IOException(
                    "Another Designer atomic history capture is already active on the EDT");
        }
        if (!(document instanceof AbstractDocument abstractDocument)) {
            throw new IOException(
                    "The live Dart document does not expose exact Undo listener identities");
        }
        requireSoleNativeListener(abstractDocument, nativeHistory,
                "before Designer capture");
        if (!nativeHistory.isInProgress()) {
            throw new IOException(
                    "The native NetBeans Undo manager no longer accepts document edits");
        }
        if (abstractDocument.getProperty(DOCUMENT_PROPERTY) != null) {
            throw new IOException(
                    "The live Dart document already owns a Designer capture token");
        }
        DesignerAtomicEditCapture capture = new DesignerAtomicEditCapture(
                abstractDocument, nativeHistory);
        abstractDocument.putProperty(DOCUMENT_PROPERTY, capture);
        ACTIVE.set(capture);
        try {
            nativeHistory.addChangeListener(capture);
            capture.managerListenerAttached = true;
            return capture;
        } catch (RuntimeException | Error installFailure) {
            // An adversarial/custom manager can register and then throw. Never
            // leave either half of the exact EDT/document token installed.
            try {
                nativeHistory.removeChangeListener(capture);
            } catch (RuntimeException | Error cleanupFailure) {
                addSuppressedSafely(installFailure, cleanupFailure);
            }
            try {
                if (abstractDocument.getProperty(DOCUMENT_PROPERTY) == capture) {
                    abstractDocument.putProperty(DOCUMENT_PROPERTY, null);
                }
            } catch (RuntimeException | Error cleanupFailure) {
                addSuppressedSafely(installFailure, cleanupFailure);
            } finally {
                if (ACTIVE.get() == capture) {
                    ACTIVE.remove();
                }
            }
            throw installFailure;
        }
    }

    /**
     * Seals the semantic edge after exact candidate verification, before
     * unlock. The publication queue must provide an all-or-none ownership
     * handoff: normal return means it retained the publication, while any
     * thrown failure means it retained nothing and will never publish it.
     */
    void seal(
            long beforeRevisionId,
            long afterRevisionId,
            String presentationName,
            DesignerSemanticUndoableEdit.ReplayController replayController,
            DesignerSemanticUndoableEdit.PublicationQueue publicationQueue,
            JointCommit jointCommitNoThrow) {
        requireOwner();
        if (state != State.ACTIVE || armed != null) {
            throw new IllegalStateException(
                    "The Designer atomic capture can be sealed exactly once");
        }
        armed = new ArmedSemanticEdit(
                beforeRevisionId,
                afterRevisionId,
                presentationName,
                replayController,
                publicationQueue,
                Objects.requireNonNull(jointCommitNoThrow,
                        "jointCommitNoThrow"));
        state = State.READY;
    }

    /**
     * Marks a pre-publication rejection before it is rethrown to BaseDocument.
     * The enclosing atomic owner must immediately perform its exact atomicUndo;
     * any resulting rollback event is passed through without semantic wrapping.
     */
    void abort() {
        requireOwner();
        if (state != State.ACTIVE && state != State.READY) {
            throw new IllegalStateException(
                    "Only an unpublished Designer capture can be aborted");
        }
        state = State.ABORTED;
        detachManagerListener();
    }

    /** Admit a semantic-only edge without manufacturing a document edit. */
    void admitUnchangedSource() {
        requireOwner();
        if (state != State.READY || armed == null || semanticEdit != null) {
            throw new IllegalStateException("A metadata edge requires one sealed capture");
        }
        semanticEdit = new DesignerSemanticUndoableEdit(
                armed.beforeRevisionId(), armed.afterRevisionId(),
                armed.presentationName(), null, armed.replayController(),
                armed.publicationQueue());
        state = State.WRAPPED;
        nativeHistory.undoableEditHappened(new UndoableEditEvent(document, semanticEdit));
    }

    /** Called only by the registered Dart MIME wrapper at atomic unlock. */
    UndoableEdit wrap(UndoableEdit raw, Document eventDocument) {
        requireOwner();
        Objects.requireNonNull(raw, "raw");
        if (eventDocument == document && state == State.ABORTED) {
            // BaseDocument may still deliver its killed/rollback compound at
            // outer unlock. It is not a forward semantic edge and the capture
            // listener was already detached by abort().
            return raw;
        }
        if (eventDocument != document
                || state != State.READY
                || armed == null
                || semanticEdit != null) {
            fail("The Designer wrapper observed a stale, foreign or repeated atomic edit",
                    null);
            state = State.POISONED;
            return raw;
        }
        semanticEdit = new DesignerSemanticUndoableEdit(
                armed.beforeRevisionId(),
                armed.afterRevisionId(),
                armed.presentationName(),
                raw,
                armed.replayController(),
                armed.publicationQueue());
        state = State.WRAPPED;
        return semanticEdit;
    }

    /** Native manager acknowledgement; performs no outward callback. */
    @Override
    public void stateChanged(ChangeEvent event) {
        requireOwner();
        if (event.getSource() != nativeHistory
                || state != State.WRAPPED
                || semanticEdit == null
                || !nativeHistory.isInProgress()) {
            fail("The native NetBeans history acknowledgement did not match the wrapped Designer edge",
                    null);
            state = State.POISONED;
            detachManagerListener();
            return;
        }

        DesignerSemanticUndoableEdit.DeferredPublication publication;
        try {
            publication = Objects.requireNonNull(
                    armed.jointCommitNoThrow().commit(),
                    "joint commit publication");
        } catch (RuntimeException | Error commitFailure) {
            fail("The armed Designer joint commit failed after native history admission",
                    commitFailure);
            state = State.POISONED;
            detachManagerListener();
            return;
        }
        state = State.PUBLISHED;
        detachManagerListener();
        postWriteFallbackPublication = publication;
        try {
            armed.publicationQueue().enqueue(publication);
            postWriteFallbackPublication = null;
        } catch (Throwable enqueueFailure) {
            // The all-or-none queue retained nothing. stateChanged still runs
            // under BaseDocument's outer write lock, so only remember the
            // exact publication here. verifyCompleted() owns the post-write
            // fallback and contains every presentation-side failure.
            publicationEnqueueFailure = enqueueFailure;
        }
    }

    void verifyCompleted() throws IOException {
        requireOwner();
        publishPostWriteFallbackSafely();
        requireSoleNativeListener(document, nativeHistory,
                "after Designer capture");
        IOException capturedFailure = failure.get();
        if (capturedFailure != null) {
            throw capturedFailure;
        }
        if (state != State.PUBLISHED || semanticEdit == null) {
            throw new IOException(
                    "The sealed Designer transition was not admitted to native history");
        }
    }

    /**
     * Publishes a queue-rejected committed effect after the caller has left
     * the document write transaction. Fields are cleared before invocation so
     * a failing callback is contained and can never be retried or duplicated.
     */
    private void publishPostWriteFallbackSafely() {
        DesignerSemanticUndoableEdit.DeferredPublication publication =
                postWriteFallbackPublication;
        Throwable enqueueFailure = publicationEnqueueFailure;
        postWriteFallbackPublication = null;
        publicationEnqueueFailure = null;
        if (publication == null) {
            return;
        }
        if (enqueueFailure != null) {
            logWarningSafely(
                    "The Designer publication queue rejected a committed apply; publishing after the document barrier",
                    enqueueFailure);
        }
        try {
            publication.publish();
        } catch (Throwable publicationFailure) {
            logWarningSafely(
                    "A committed Designer apply publication failed after the document barrier",
                    publicationFailure);
        }
    }

    boolean committed() {
        return state == State.PUBLISHED;
    }

    boolean aborted() {
        return state == State.ABORTED;
    }

    @Override
    public void close() throws IOException {
        requireOwner();
        try {
            detachManagerListener();
        } finally {
            try {
                if (document.getProperty(DOCUMENT_PROPERTY) == this) {
                    document.putProperty(DOCUMENT_PROPERTY, null);
                }
            } finally {
                if (ACTIVE.get() == this) {
                    ACTIVE.remove();
                }
            }
        }
        requireSoleNativeListener(document, nativeHistory,
                "while closing Designer capture");
    }

    static UndoableEdit wrapActive(UndoableEdit edit, Document document) {
        DesignerAtomicEditCapture capture = ACTIVE.get();
        if (capture == null
                || document.getProperty(DOCUMENT_PROPERTY) != capture) {
            return edit;
        }
        return capture.wrap(edit, document);
    }

    private void detachManagerListener() {
        if (managerListenerAttached) {
            nativeHistory.removeChangeListener(this);
            managerListenerAttached = false;
        }
    }

    private void requireOwner() {
        requireEdt();
        if (ACTIVE.get() != this
                || document.getProperty(DOCUMENT_PROPERTY) != this) {
            throw new IllegalStateException(
                    "The Designer atomic capture no longer owns this EDT/document");
        }
    }

    private void fail(String message, Throwable cause) {
        failure.compareAndSet(null, cause == null
                ? new IOException(message) : new IOException(message, cause));
    }

    private static void requireSoleNativeListener(
            AbstractDocument document,
            UndoableEditListener nativeHistory,
            String operation) throws IOException {
        UndoableEditListener[] listeners = document.getUndoableEditListeners();
        if (listeners.length != 1 || listeners[0] != nativeHistory) {
            throw new IOException(
                    "The live Dart document must retain exactly one native Undo listener "
                    + operation + "; found " + listeners.length);
        }
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Designer atomic history state is EDT-owned");
        }
    }

    private static void logWarningSafely(String message, Throwable failure) {
        try {
            LOGGER.log(Level.WARNING, message, failure);
        } catch (Throwable ignored) {
            // A custom logging Handler is presentation infrastructure. It must
            // not reopen an already committed native/semantic edge.
        }
    }

    private static void addSuppressedSafely(
            Throwable primary,
            Throwable cleanupFailure) {
        if (primary == cleanupFailure) {
            return;
        }
        try {
            primary.addSuppressed(cleanupFailure);
        } catch (RuntimeException | Error ignored) {
            // Preserve the original installation failure.
        }
    }

    private enum State {
        ACTIVE,
        READY,
        WRAPPED,
        PUBLISHED,
        ABORTED,
        POISONED
    }

    private record ArmedSemanticEdit(
            long beforeRevisionId,
            long afterRevisionId,
            String presentationName,
            DesignerSemanticUndoableEdit.ReplayController replayController,
            DesignerSemanticUndoableEdit.PublicationQueue publicationQueue,
            JointCommit jointCommitNoThrow) {
        ArmedSemanticEdit {
            Objects.requireNonNull(presentationName, "presentationName");
            Objects.requireNonNull(replayController, "replayController");
            Objects.requireNonNull(publicationQueue, "publicationQueue");
            Objects.requireNonNull(jointCommitNoThrow, "jointCommitNoThrow");
        }
    }

    /** Must perform one monitor-only, non-throwing pair/model state swap. */
    @FunctionalInterface
    interface JointCommit {
        DesignerSemanticUndoableEdit.DeferredPublication commit();
    }
}
