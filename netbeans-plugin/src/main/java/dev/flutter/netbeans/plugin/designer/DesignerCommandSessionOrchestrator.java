package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandEdit;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionResult;
import dev.flutter.netbeans.designer.command.DesignerCommandStatus;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.openide.awt.UndoRedo;
import org.openide.util.ChangeSupport;

/**
 * NetBeans-facing owner of one immutable pure Designer command session.
 *
 * <p>This boundary deliberately performs no live-document apply, analyzer
 * request, staging or persistence. A command first produces an identity-bound
 * pending lease while the visible cursor remains at its exact predecessor.
 * Only a separately verified staging transition may adopt the candidate.
 * Durable persistence uses another identity-bound lease which pins the adopted
 * cursor and precomputes the saved anchor. The DataObject-owned internal
 * mutation controller joins those stages. The public mutation UI admits the
 * catalog-backed Properties slice and one deliberately narrow Palette DnD
 * slice: {@code Text} may be appended to {@code Row.children} or
 * {@code Column.children}. Other Palette mutations remain disabled.</p>
 */
final class DesignerCommandSessionOrchestrator
        implements UndoRedo, AutoCloseable {
    static final boolean PUBLIC_MUTATION_UI_ENABLED = true;
    static final boolean PUBLIC_PALETTE_TEXT_APPEND_DND_ENABLED = true;
    private static final Logger LOGGER = Logger.getLogger(
            DesignerCommandSessionOrchestrator.class.getName());

    private final Object monitor = new Object();
    private final ChangeSupport changes = new ChangeSupport(this);
    private final DesignerCombinedUndoRedo.SessionBinding undoBinding;
    private DesignerCommandSession session;
    private PendingCommandLease activePendingTransition;
    private DurableSaveLease activeDurableSave;
    private SourceAnchorLease activeSourceAnchor;
    private boolean operationRunning;
    private boolean closeRequested;
    private boolean closed;

    DesignerCommandSessionOrchestrator(
            DesignerCommandSession initialSession,
            DesignerCombinedUndoRedo combinedUndoRedo) {
        session = Objects.requireNonNull(initialSession, "initialSession");
        Objects.requireNonNull(combinedUndoRedo, "combinedUndoRedo");
        undoBinding = combinedUndoRedo.bindDesignerSession(this);
    }

    /**
     * Derives one prospective command revision without moving the owned
     * session cursor.
     *
     * <p>A changed result publishes a pending lease. While that lease is
     * active, commands, Undo/Redo and durable Save are blocked. The caller must
     * adopt only after the exact live/staged pair was replaced, abort when the
     * predecessor remains authoritative, or invalidate an uncertain session.</p>
     */
    PendingCommandAttempt beginCommand(DesignerCommand command) {
        return beginCommandInternal(
                null,
                Objects.requireNonNull(command, "command"),
                null);
    }

    /**
     * Derives a command only when the cursor still is the exact revision
     * observed by the caller. The identity check and operation claim happen
     * under the same monitor, closing the UI-token versus Undo/Redo race.
     */
    PendingCommandAttempt beginCommand(
            DesignerCommandRevision expectedRevision,
            DesignerCommand command) {
        return beginCommandInternal(
                Objects.requireNonNull(expectedRevision, "expectedRevision"),
                Objects.requireNonNull(command, "command"),
                null);
    }

    /**
     * Derives one prospective command from an exact noncanonical physical
     * representation of the owned logical cursor.
     *
     * <p>The pure session verifies that {@code exactEndpointPair} is attached
     * to its durable anchor and current semantic revision. A changed result
     * retains that exact pair identity in its pending lease, while the owned
     * cursor remains at the logical predecessor until joint adoption.</p>
     */
    PendingCommandAttempt beginCommandFromPhysicalEndpoint(
            DesignerCommand command,
            PreparedDesignerPair exactEndpointPair) {
        return beginCommandInternal(
                null,
                Objects.requireNonNull(command, "command"),
                Objects.requireNonNull(
                        exactEndpointPair, "exactEndpointPair"));
    }

    /** Exact-cursor variant used by staged replacement admission. */
    PendingCommandAttempt beginCommandFromPhysicalEndpoint(
            DesignerCommandRevision expectedRevision,
            DesignerCommand command,
            PreparedDesignerPair exactEndpointPair) {
        return beginCommandInternal(
                Objects.requireNonNull(expectedRevision, "expectedRevision"),
                Objects.requireNonNull(command, "command"),
                Objects.requireNonNull(
                        exactEndpointPair, "exactEndpointPair"));
    }

    private PendingCommandAttempt beginCommandInternal(
            DesignerCommandRevision expectedRevision,
            DesignerCommand command,
            PreparedDesignerPair exactEndpointPair) {
        Objects.requireNonNull(command, "command");
        DesignerCommandSession captured;
        synchronized (monitor) {
            requireOpenLocked();
            if (expectedRevision != null
                    && session.current() != expectedRevision) {
                throw new StaleRevisionException(
                        "The selected Flutter Designer revision changed before the command started.");
            }
            beginOperationLocked();
            captured = session;
        }

        DesignerCommandSessionResult result;
        try {
            result = Objects.requireNonNull(
                    exactEndpointPair == null
                            ? captured.apply(command)
                            : captured.applyFromPhysicalEndpoint(
                                    command, exactEndpointPair),
                    "command result");
        } catch (RuntimeException | Error failure) {
            finishFailedOperation(captured);
            throw failure;
        }

        PendingCommandLease lease = null;
        synchronized (monitor) {
            if (closed || session != captured) {
                operationRunning = false;
                throw new IllegalStateException(
                        "The Designer command session changed while deriving a candidate");
            }
            if (result.changed()) {
                try {
                    lease = new PendingCommandLease(
                            this,
                            PendingTransitionKind.APPLY,
                            captured,
                            result.session(),
                            result.edit().orElseThrow(),
                            exactEndpointPair);
                    activePendingTransition = lease;
                    // operationRunning deliberately remains true for the
                    // complete analyzer/live replacement lifetime.
                } catch (RuntimeException | Error invalidResult) {
                    operationRunning = false;
                    throw invalidResult;
                }
            } else {
                if (result.session() != captured) {
                    operationRunning = false;
                    throw new IllegalStateException(
                            "A rejected Designer command substituted the input session");
                }
                operationRunning = false;
            }
        }
        if (lease != null) {
            fireChangeSafely();
        }
        return new PendingCommandAttempt(result, Optional.ofNullable(lease));
    }

    /**
     * Derives the exact preceding command revision without moving the owned
     * live cursor. The returned transition must be adopted only after its
     * corresponding pair revision becomes authoritative, or otherwise aborted
     * or invalidated.
     */
    PendingTransitionAttempt beginUndoTransition() {
        return beginCursorTransition(
                PendingTransitionKind.UNDO,
                DesignerCommandSession::undo,
                DesignerCommandStatus.UNDONE);
    }

    /**
     * Derives the exact following command revision without moving the owned
     * live cursor. The returned transition has the same one-shot resolution
     * contract as a prospective command transition.
     */
    PendingTransitionAttempt beginRedoTransition() {
        return beginCursorTransition(
                PendingTransitionKind.REDO,
                DesignerCommandSession::redo,
                DesignerCommandStatus.REDONE);
    }

    private PendingTransitionAttempt beginCursorTransition(
            PendingTransitionKind kind,
            SessionOperation operation,
            DesignerCommandStatus expectedStatus) {
        DesignerCommandSession captured;
        synchronized (monitor) {
            beginOperationLocked();
            captured = session;
        }

        DesignerCommandSessionResult result;
        try {
            result = Objects.requireNonNull(
                    operation.run(captured), "session transition result");
        } catch (RuntimeException | Error failure) {
            finishFailedOperation(captured);
            throw failure;
        }

        PendingCommandLease lease = null;
        synchronized (monitor) {
            if (closed || session != captured) {
                operationRunning = false;
                throw new IllegalStateException(
                        "The Designer command session changed while deriving a cursor transition");
            }
            if (result.changed()) {
                if (result.status() != expectedStatus) {
                    operationRunning = false;
                    throw new IllegalStateException(
                            "A changed " + kind + " transition returned "
                            + result.status());
                }
                try {
                    lease = new PendingCommandLease(
                            this,
                            kind,
                            captured,
                            result.session(),
                            result.edit().orElseThrow(),
                            null);
                    activePendingTransition = lease;
                    // operationRunning remains true until explicit resolution.
                } catch (RuntimeException | Error invalidResult) {
                    operationRunning = false;
                    throw invalidResult;
                }
            } else {
                if (result.session() != captured) {
                    operationRunning = false;
                    throw new IllegalStateException(
                            "A rejected Designer cursor transition substituted the input session");
                }
                operationRunning = false;
            }
        }
        if (lease != null) {
            fireChangeSafely();
        }
        return new PendingTransitionAttempt(
                kind, result, Optional.ofNullable(lease));
    }

    DesignerCommandRevision currentRevision() {
        synchronized (monitor) {
            requireOpenLocked();
            return session.current();
        }
    }

    boolean dirty() {
        synchronized (monitor) {
            return !closed && session.dirty();
        }
    }

    /**
     * Reports whether this owner can still pin retained semantic history.
     * A deferred close request retires that authority immediately even though
     * the final binding callback may wait for an active lease to resolve.
     */
    boolean retainsOpenHistoryAuthority() {
        synchronized (monitor) {
            return !closed && !closeRequested;
        }
    }

    /**
     * Pins the exact dirty cursor and precomputes its post-commit durable
     * anchor before any caller may perform I/O.
     *
     * <p>The returned lease contains immutable evidence only. It grants no
     * write authority. While it is active, no command, cursor move or second
     * durable candidate can start. A caller must explicitly adopt, abort or
     * invalidate it.</p>
     */
    DurableSaveLease beginDurableSave() {
        return beginDurableSave(null);
    }

    /**
     * Pins the exact dirty cursor while precomputing the supplied physical
     * paired serialization as its durable anchor. The pair remains immutable
     * evidence only; this method performs no I/O.
     */
    DurableSaveLease beginDurableSave(PreparedDesignerPair exactPair) {
        DurableSaveLease lease;
        synchronized (monitor) {
            beginOperationLocked();
            if (!session.dirty()) {
                operationRunning = false;
                throw new IllegalStateException(
                        "Cannot begin a durable Designer save from a clean cursor");
            }
            DesignerCommandSession captured = session;
            DesignerCommandRevision revision = captured.current();
            try {
                DesignerCommandSession precomputedSaved = exactPair == null
                        ? captured.markSaved()
                        : captured.markSaved(exactPair);
                lease = new DurableSaveLease(
                        this,
                        captured,
                        revision,
                        precomputedSaved,
                        captured.durableFdAnchor().copyBytes(),
                        captured.durableDartAnchorBytes(),
                        exactPair);
                activeDurableSave = lease;
            } catch (RuntimeException | Error failure) {
                operationRunning = false;
                throw failure;
            }
        }
        fireChangeSafely();
        return lease;
    }

    /**
     * Pins the exact clean saved cursor and precomputes a source-only durable
     * re-anchor for one exact planned Dart serialization.
     *
     * <p>The returned lease is pure identity evidence. It performs no I/O and
     * grants no write authority. While active, commands, cursor moves and
     * every other save lease are blocked. The caller must adopt it only after
     * the exact planned Dart bytes commit, abort it before persistence, or
     * invalidate the owner when the durable outcome is uncertain.</p>
     */
    SourceAnchorLease beginSourceAnchor(byte[] exactPlannedDart) {
        Objects.requireNonNull(exactPlannedDart, "exactPlannedDart");
        SourceAnchorLease lease;
        synchronized (monitor) {
            beginOperationLocked();
            if (session.dirty()) {
                operationRunning = false;
                throw new IllegalStateException(
                        "Cannot begin a Source anchor from a dirty Designer cursor");
            }
            DesignerCommandSession captured = session;
            DesignerCommandRevision revision = captured.current();
            try {
                DesignerCommandSession precomputed =
                        captured.reanchorSavedSource(exactPlannedDart);
                lease = new SourceAnchorLease(
                        this,
                        captured,
                        revision,
                        precomputed,
                        captured.durableFdAnchor().copyBytes(),
                        captured.durableDartAnchorBytes(),
                        exactPlannedDart);
                activeSourceAnchor = lease;
            } catch (RuntimeException | Error failure) {
                operationRunning = false;
                throw failure;
            }
        }
        fireChangeSafely();
        return lease;
    }

    @Override
    public boolean canUndo() {
        synchronized (monitor) {
            return !closed && !operationRunning && session.canUndo();
        }
    }

    @Override
    public boolean canRedo() {
        synchronized (monitor) {
            return !closed && !operationRunning && session.canRedo();
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        DesignerCommandSessionResult result = runSessionOperation(
                DesignerCommandSession::undo);
        if (result.status() != DesignerCommandStatus.UNDONE) {
            CannotUndoException failure = new CannotUndoException();
            failure.initCause(new IllegalStateException(reason(result)));
            throw failure;
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        DesignerCommandSessionResult result = runSessionOperation(
                DesignerCommandSession::redo);
        if (result.status() != DesignerCommandStatus.REDONE) {
            CannotRedoException failure = new CannotRedoException();
            failure.initCause(new IllegalStateException(reason(result)));
            throw failure;
        }
    }

    @Override
    public String getUndoPresentationName() {
        synchronized (monitor) {
            return !closed && !operationRunning
                    ? presentation("Undo", session.undoEdit()) : "Undo";
        }
    }

    @Override
    public String getRedoPresentationName() {
        synchronized (monitor) {
            return !closed && !operationRunning
                    ? presentation("Redo", session.redoEdit()) : "Redo";
        }
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
    public void close() {
        boolean closeBinding = false;
        synchronized (monitor) {
            if (closed) {
                return;
            }
            if (activePendingTransition != null || activeDurableSave != null
                    || activeSourceAnchor != null) {
                closeRequested = true;
                return;
            }
            closed = true;
            closeBinding = true;
        }
        if (closeBinding) {
            closeUndoBindingSafely();
            fireChangeSafely();
        }
    }

    private void adoptPendingCommand(PendingCommandLease lease) {
        adoptPendingCommandDeferredEffects(lease).publish();
    }

    /**
     * Performs only an identity-checked pending cursor swap. The caller must
     * publish the returned effects after releasing every document/coordinator
     * lock used to verify and install the exact peer transition.
     */
    private DeferredLeaseEffects adoptPendingCommandDeferredEffects(
            PendingCommandLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ADOPTED) {
                return DeferredLeaseEffects.none();
            }
            requireUnclaimedPendingCommandLocked(lease);
            requireActivePendingCommandLocked(lease);
            session = lease.afterSessionIdentity;
            lease.resolution = LeaseResolution.ADOPTED;
            activePendingTransition = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    /**
     * Joins an exact staged-pair outcome to a pending command cursor swap.
     *
     * <p>The caller already owns the live-document and pair-coordinator locks.
     * The supplied peer commit therefore must be a non-throwing state
     * assignment which publishes no callbacks. It runs under this
     * orchestrator monitor immediately before the exact target session is
     * adopted, so no observer can acquire this monitor between the coordinator
     * outcome and the command cursor outcome. Outward callbacks remain in the
     * returned one-shot effects object.</p>
     */
    private DeferredLeaseEffects adoptPendingCommandDeferredEffects(
            PendingCommandLease lease,
            PairSaveEvidence exactCandidateEvidence,
            Runnable coordinatorCommitNoThrow) {
        return adoptPendingCommandDeferredEffects(
                lease,
                exactCandidateEvidence,
                coordinatorCommitNoThrow,
                null);
    }

    private DeferredLeaseEffects adoptPendingCommandDeferredEffects(
            PendingCommandLease lease,
            PairSaveEvidence exactCandidateEvidence,
            Runnable coordinatorCommitNoThrow,
            Object transitionClaim) {
        Objects.requireNonNull(coordinatorCommitNoThrow,
                "coordinatorCommitNoThrow");
        return adoptPendingCommandCloseAwareDeferredEffects(
                lease,
                exactCandidateEvidence,
                closePending -> coordinatorCommitNoThrow.run(),
                transitionClaim);
    }

    /**
     * Joins a staged peer transition while reporting an already requested
     * owner close before either side installs its final state.
     *
     * <p>The peer callback runs under this orchestrator's monitor after every
     * exact lease/evidence/claim check, but before the command cursor swap and
     * {@link #finishDeferredCloseLocked()}. It must perform only a no-throw
     * state assignment and must not publish callbacks. A {@code true}
     * argument means that this successful joint adoption will close the
     * command-session owner; the peer can close its matching authority in the
     * same indivisible transition.</p>
     */
    private DeferredLeaseEffects adoptPendingCommandCloseAwareDeferredEffects(
            PendingCommandLease lease,
            PairSaveEvidence exactCandidateEvidence,
            CloseAwareStagedPeerCommit coordinatorCommitNoThrow,
            Object transitionClaim) {
        Objects.requireNonNull(exactCandidateEvidence,
                "exactCandidateEvidence");
        Objects.requireNonNull(coordinatorCommitNoThrow,
                "coordinatorCommitNoThrow");
        DesignerCommandRevision exactTarget = lease.afterRevisionIdentity;
        var exactPreparedPair = exactTarget.preparedPair()
                .orElseThrow(() -> new IllegalStateException(
                "Only an exact PAIRED command target can adopt staged evidence"));
        if (exactCandidateEvidence.preparedPairIdentity()
                != exactPreparedPair) {
            throw new IllegalArgumentException(
                    "The staged evidence does not retain the command target's exact prepared pair");
        }
        return transitionClaim == null
                ? adoptUnclaimedExactTargetCloseAwareDeferredEffects(
                        lease, exactTarget, coordinatorCommitNoThrow)
                : adoptExactTargetCloseAwareDeferredEffects(
                        lease,
                        exactTarget,
                        coordinatorCommitNoThrow,
                        transitionClaim);
    }

    /**
     * Joins one exact claimed peer transition to the pending target cursor.
     * The peer assignment runs first and must publish no callbacks. If it
     * fails, the exact pending lease and its claim remain active and the live
     * command cursor is unchanged.
     */
    private DeferredLeaseEffects adoptExactTargetDeferredEffects(
            PendingCommandLease lease,
            DesignerCommandRevision exactTarget,
            Runnable peerCommitNoThrow,
            Object transitionClaim) {
        Objects.requireNonNull(exactTarget, "exactTarget");
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        Objects.requireNonNull(transitionClaim, "transitionClaim");
        return adoptExactTargetCloseAwareDeferredEffects(
                lease,
                exactTarget,
                closePending -> peerCommitNoThrow.run(),
                transitionClaim,
                true);
    }

    private DeferredLeaseEffects adoptUnclaimedExactTargetDeferredEffects(
            PendingCommandLease lease,
            DesignerCommandRevision exactTarget,
            Runnable peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        return adoptUnclaimedExactTargetCloseAwareDeferredEffects(
                lease,
                exactTarget,
                closePending -> peerCommitNoThrow.run());
    }

    private DeferredLeaseEffects adoptUnclaimedExactTargetCloseAwareDeferredEffects(
            PendingCommandLease lease,
            DesignerCommandRevision exactTarget,
            CloseAwareStagedPeerCommit peerCommitNoThrow) {
        return adoptExactTargetCloseAwareDeferredEffects(
                lease,
                exactTarget,
                peerCommitNoThrow,
                null,
                false);
    }

    private DeferredLeaseEffects adoptExactTargetCloseAwareDeferredEffects(
            PendingCommandLease lease,
            DesignerCommandRevision exactTarget,
            CloseAwareStagedPeerCommit peerCommitNoThrow,
            Object transitionClaim) {
        Objects.requireNonNull(exactTarget, "exactTarget");
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        Objects.requireNonNull(transitionClaim, "transitionClaim");
        return adoptExactTargetCloseAwareDeferredEffects(
                lease,
                exactTarget,
                peerCommitNoThrow,
                transitionClaim,
                true);
    }

    private DeferredLeaseEffects adoptExactTargetCloseAwareDeferredEffects(
            PendingCommandLease lease,
            DesignerCommandRevision exactTarget,
            CloseAwareStagedPeerCommit peerCommitNoThrow,
            Object transitionClaim,
            boolean claimed) {
        if (lease.afterRevisionIdentity != exactTarget) {
            throw new IllegalArgumentException(
                    "The supplied command target is not the pending transition's exact target revision");
        }
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ADOPTED) {
                return DeferredLeaseEffects.none();
            }
            requireActivePendingCommandLocked(lease);
            if (claimed) {
                requireTransitionClaimLocked(lease, transitionClaim);
            } else {
                requireUnclaimedPendingCommandLocked(lease);
            }
            peerCommitNoThrow.commit(closeRequested);
            session = lease.afterSessionIdentity;
            lease.resolution = LeaseResolution.ADOPTED;
            lease.transitionClaimIdentity = null;
            activePendingTransition = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private void abortPendingCommand(PendingCommandLease lease) {
        abortPendingCommandDeferredEffects(lease).publish();
    }

    private DeferredLeaseEffects abortPendingCommandDeferredEffects(
            PendingCommandLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution != LeaseResolution.ACTIVE) {
                return DeferredLeaseEffects.none();
            }
            requireUnclaimedPendingCommandLocked(lease);
            requireActivePendingCommandLocked(lease);
            lease.resolution = LeaseResolution.ABORTED;
            activePendingTransition = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    /**
     * Coordinator-only strict abort. Unlike the idempotent public lease abort,
     * this refuses an already-adopted/invalidated outcome so pair recovery can
     * never publish C1 while the command cursor has escaped to C2.
     */
    private DeferredLeaseEffects
            abortPendingCommandToExactPredecessorDeferredEffects(
                    PendingCommandLease lease,
                    Object transitionClaim) {
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ABORTED) {
                if (lease.owner != this
                        || session != lease.beforeSessionIdentity
                        || activePendingTransition != null
                        || activeDurableSave != null
                        || activeSourceAnchor != null
                        || operationRunning) {
                    throw new IllegalStateException(
                            "The aborted command lease no longer retains its exact predecessor session");
                }
                return DeferredLeaseEffects.none();
            }
            if (lease.resolution != LeaseResolution.ACTIVE) {
                throw new IllegalStateException(
                        "The pending command lease cannot return to its exact predecessor after "
                        + lease.resolution);
            }
            requireActivePendingCommandLocked(lease);
            requireTransitionClaimLocked(lease, transitionClaim);
            lease.resolution = LeaseResolution.ABORTED;
            lease.transitionClaimIdentity = null;
            activePendingTransition = null;
            operationRunning = false;
            boolean closeBinding = finishDeferredCloseLocked();
            return new DeferredLeaseEffects(this, closeBinding);
        }
    }

    private void invalidatePendingCommand(PendingCommandLease lease) {
        invalidatePendingCommandDeferredEffects(lease).publish();
    }

    private DeferredLeaseEffects invalidatePendingCommandDeferredEffects(
            PendingCommandLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.INVALIDATED) {
                return DeferredLeaseEffects.none();
            }
            if (lease.resolution == LeaseResolution.ADOPTED) {
                if (lease.owner != this
                        || session != lease.afterSessionIdentity
                        || operationRunning) {
                    throw new IllegalStateException(
                            "The adopted command lease no longer owns its exact candidate revision");
                }
                closeBinding = !closed;
            } else if (lease.resolution == LeaseResolution.ABORTED) {
                if (lease.owner != this
                        || session != lease.beforeSessionIdentity
                        || activePendingTransition != null
                        || activeDurableSave != null
                        || activeSourceAnchor != null
                        || operationRunning) {
                    throw new IllegalStateException(
                            "The aborted command lease no longer owns its exact predecessor revision");
                }
                closeBinding = !closed;
            } else {
                requireUnclaimedPendingCommandLocked(lease);
                requireActivePendingCommandLocked(lease);
                closeBinding = true;
            }
            lease.resolution = LeaseResolution.INVALIDATED;
            activePendingTransition = null;
            operationRunning = false;
            closeRequested = false;
            closed = true;
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private DeferredLeaseEffects invalidateClaimedPendingCommandDeferredEffects(
            PendingCommandLease lease,
            Object transitionClaim) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.INVALIDATED) {
                return DeferredLeaseEffects.none();
            }
            requireActivePendingCommandLocked(lease);
            requireTransitionClaimLocked(lease, transitionClaim);
            lease.resolution = LeaseResolution.INVALIDATED;
            lease.transitionClaimIdentity = null;
            activePendingTransition = null;
            operationRunning = false;
            closeRequested = false;
            closeBinding = !closed;
            closed = true;
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private void claimPendingCommandForTransition(
            PendingCommandLease lease,
            Object transitionClaim) {
        Objects.requireNonNull(transitionClaim, "transitionClaim");
        synchronized (monitor) {
            requireActivePendingCommandLocked(lease);
            if (lease.transitionClaimIdentity != null
                    && lease.transitionClaimIdentity != transitionClaim) {
                throw new IllegalStateException(
                        "The pending command is already claimed by another transition");
            }
            lease.transitionClaimIdentity = transitionClaim;
        }
    }

    private void requireUnclaimedPendingCommandLocked(
            PendingCommandLease lease) {
        if (lease.transitionClaimIdentity != null) {
            throw new IllegalStateException(
                    "The pending command is owned by an active transition");
        }
    }

    private void requireTransitionClaimLocked(
            PendingCommandLease lease,
            Object transitionClaim) {
        if (transitionClaim == null
                || lease.transitionClaimIdentity != transitionClaim) {
            throw new IllegalStateException(
                    "The pending command does not retain the exact transition claim");
        }
    }

    private void requireActivePendingCommandLocked(PendingCommandLease lease) {
        if (lease.owner != this
                || activePendingTransition != lease
                || lease.resolution != LeaseResolution.ACTIVE
                || !operationRunning
                || closed
                || session != lease.beforeSessionIdentity
                || session.current() != lease.beforeRevisionIdentity
                || lease.afterSessionIdentity.current()
                        != lease.afterRevisionIdentity) {
            throw new IllegalStateException(
                    "The pending command lease does not own its exact transition");
        }
    }

    private boolean ownsExactActiveTransition(PendingCommandLease lease) {
        synchronized (monitor) {
            return lease.owner == this
                    && activePendingTransition == lease
                    && lease.resolution == LeaseResolution.ACTIVE
                    && operationRunning
                    && !closed
                    && session == lease.beforeSessionIdentity
                    && session.current() == lease.beforeRevisionIdentity
                    && lease.afterSessionIdentity.current()
                            == lease.afterRevisionIdentity;
        }
    }

    private void adoptCommitted(DurableSaveLease lease) {
        adoptCommittedDeferredEffects(lease).publish();
    }

    /**
     * Performs only the identity-checked session swap while retaining all
     * listener/binding callbacks for a later, lock-free publication step.
     * Pair persistence uses this split so its own coordinator monitor and the
     * exact session re-anchor become one atomic state decision.
     */
    private DeferredLeaseEffects adoptCommittedDeferredEffects(
            DurableSaveLease lease) {
        return adoptCommittedDeferredEffects(lease, () -> { });
    }

    /**
     * Joins the exact durable cursor re-anchor to one non-throwing peer state
     * commit. The returned callbacks remain deferred until every caller lock
     * has been released.
     */
    private DeferredLeaseEffects adoptCommittedDeferredEffects(
            DurableSaveLease lease,
            Runnable peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        return adoptCommittedCloseAwareDeferredEffects(
                lease, closePending -> peerCommitNoThrow.run());
    }

    /**
     * Joins one exact durable command-session re-anchor to a monitor-only peer
     * assignment which can fail closed when owner shutdown raced the Save.
     */
    private DeferredLeaseEffects adoptCommittedCloseAwareDeferredEffects(
            DurableSaveLease lease,
            CloseAwareDurableSavePeerCommit peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ADOPTED) {
                return DeferredLeaseEffects.none();
            }
            requireActiveDurableSaveLocked(lease);
            peerCommitNoThrow.commit(closeRequested);
            session = lease.precomputedSaved;
            lease.resolution = LeaseResolution.ADOPTED;
            activeDurableSave = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private void abortDurableSave(DurableSaveLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution != LeaseResolution.ACTIVE) {
                return;
            }
            requireActiveDurableSaveLocked(lease);
            lease.resolution = LeaseResolution.ABORTED;
            activeDurableSave = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        finishLeaseEffects(closeBinding);
    }

    private void invalidateDurableSave(DurableSaveLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.INVALIDATED) {
                return;
            }
            if (lease.resolution == LeaseResolution.ADOPTED) {
                if (lease.owner != this
                        || session != lease.precomputedSaved
                        || operationRunning) {
                    throw new IllegalStateException(
                            "The adopted durable lease no longer owns the exact saved revision");
                }
                closeBinding = !closed;
            } else {
                requireActiveDurableSaveLocked(lease);
                closeBinding = true;
            }
            lease.resolution = LeaseResolution.INVALIDATED;
            activeDurableSave = null;
            operationRunning = false;
            closeRequested = false;
            closed = true;
        }
        if (closeBinding) {
            closeUndoBindingSafely();
        }
        fireChangeSafely();
    }

    private void requireActiveDurableSaveLocked(DurableSaveLease lease) {
        if (lease.owner != this
                || activeDurableSave != lease
                || lease.resolution != LeaseResolution.ACTIVE
                || !operationRunning
                || session != lease.sessionIdentity
                || session.current() != lease.revisionIdentity) {
            throw new IllegalStateException(
                    "The durable save lease does not own the exact current Designer revision");
        }
    }

    private boolean ownsExactActiveRevision(DurableSaveLease lease) {
        synchronized (monitor) {
            return lease.owner == this
                    && activeDurableSave == lease
                    && lease.resolution == LeaseResolution.ACTIVE
                    && operationRunning
                    && !closed
                    && session == lease.sessionIdentity
                    && session.current() == lease.revisionIdentity;
        }
    }

    private void adoptSourceAnchor(SourceAnchorLease lease) {
        adoptSourceAnchorDeferredEffects(lease).publish();
    }

    private DeferredLeaseEffects adoptSourceAnchorDeferredEffects(
            SourceAnchorLease lease) {
        return adoptSourceAnchorCloseAwareDeferredEffects(
                lease, closePending -> { });
    }

    private DeferredLeaseEffects adoptSourceAnchorDeferredEffects(
            SourceAnchorLease lease,
            Runnable peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        return adoptSourceAnchorCloseAwareDeferredEffects(
                lease, closePending -> peerCommitNoThrow.run());
    }

    /**
     * Joins the exact committed Source bytes to their precomputed command
     * re-anchor. The peer assignment runs under the orchestrator monitor after
     * every lease identity check and before the session swap. It must not
     * throw or publish callbacks; outward effects stay deferred in the result.
     */
    private DeferredLeaseEffects adoptSourceAnchorCloseAwareDeferredEffects(
            SourceAnchorLease lease,
            CloseAwareSourceAnchorPeerCommit peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ADOPTED) {
                return DeferredLeaseEffects.none();
            }
            requireActiveSourceAnchorLocked(lease);
            peerCommitNoThrow.commit(closeRequested);
            session = lease.precomputedReanchored;
            lease.resolution = LeaseResolution.ADOPTED;
            activeSourceAnchor = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private void abortSourceAnchor(SourceAnchorLease lease) {
        abortSourceAnchorDeferredEffects(lease).publish();
    }

    private DeferredLeaseEffects abortSourceAnchorDeferredEffects(
            SourceAnchorLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution != LeaseResolution.ACTIVE) {
                return DeferredLeaseEffects.none();
            }
            requireActiveSourceAnchorLocked(lease);
            lease.resolution = LeaseResolution.ABORTED;
            activeSourceAnchor = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    /**
     * Resolves an exact unchanged Source save without swapping the command
     * session, while joining the peer's no-throw state assignment under this
     * monitor. The peer sees whether owner close was already requested and
     * all outward effects remain deferred.
     */
    private DeferredLeaseEffects
            abortSourceAnchorUnchangedCloseAwareDeferredEffects(
                    SourceAnchorLease lease,
                    CloseAwareSourceAnchorPeerCommit peerCommitNoThrow) {
        Objects.requireNonNull(peerCommitNoThrow, "peerCommitNoThrow");
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.ABORTED) {
                return DeferredLeaseEffects.none();
            }
            requireActiveSourceAnchorLocked(lease);
            peerCommitNoThrow.commit(closeRequested);
            lease.resolution = LeaseResolution.ABORTED;
            activeSourceAnchor = null;
            operationRunning = false;
            closeBinding = finishDeferredCloseLocked();
        }
        return new DeferredLeaseEffects(this, closeBinding);
    }

    private void invalidateSourceAnchor(SourceAnchorLease lease) {
        boolean closeBinding;
        synchronized (monitor) {
            if (lease.resolution == LeaseResolution.INVALIDATED) {
                return;
            }
            if (lease.resolution == LeaseResolution.ADOPTED) {
                if (lease.owner != this
                        || session != lease.precomputedReanchored
                        || operationRunning) {
                    throw new IllegalStateException(
                            "The adopted Source anchor no longer owns its exact re-anchored session");
                }
                closeBinding = !closed;
            } else {
                requireActiveSourceAnchorLocked(lease);
                closeBinding = true;
            }
            lease.resolution = LeaseResolution.INVALIDATED;
            activeSourceAnchor = null;
            operationRunning = false;
            closeRequested = false;
            closed = true;
        }
        if (closeBinding) {
            closeUndoBindingSafely();
        }
        fireChangeSafely();
    }

    private void requireActiveSourceAnchorLocked(SourceAnchorLease lease) {
        if (lease.owner != this
                || activeSourceAnchor != lease
                || lease.resolution != LeaseResolution.ACTIVE
                || !operationRunning
                || closed
                || session != lease.sessionIdentity
                || session.current() != lease.revisionIdentity) {
            throw new IllegalStateException(
                    "The Source-anchor lease does not own the exact clean Designer revision");
        }
    }

    private boolean ownsExactActiveRevision(SourceAnchorLease lease) {
        synchronized (monitor) {
            return lease.owner == this
                    && activeSourceAnchor == lease
                    && lease.resolution == LeaseResolution.ACTIVE
                    && operationRunning
                    && !closed
                    && session == lease.sessionIdentity
                    && session.current() == lease.revisionIdentity;
        }
    }

    private boolean finishDeferredCloseLocked() {
        if (!closeRequested) {
            return false;
        }
        closeRequested = false;
        closed = true;
        return true;
    }

    private void finishLeaseEffects(boolean closeBinding) {
        if (closeBinding) {
            closeUndoBindingSafely();
        }
        fireChangeSafely();
    }

    private void closeUndoBindingSafely() {
        try {
            undoBinding.close();
        } catch (RuntimeException | Error listenerFailure) {
            LOGGER.log(Level.WARNING,
                    "A combined Undo/Redo listener failed while closing a Designer session",
                    listenerFailure);
        }
    }

    private void fireChangeSafely() {
        try {
            changes.fireChange();
        } catch (RuntimeException | Error listenerFailure) {
            LOGGER.log(Level.WARNING,
                    "A Designer command-session listener failed",
                    listenerFailure);
        }
    }

    private DesignerCommandSessionResult runSessionOperation(
            SessionOperation operation) {
        DesignerCommandSession captured;
        synchronized (monitor) {
            beginOperationLocked();
            captured = session;
        }

        DesignerCommandSessionResult result;
        try {
            result = Objects.requireNonNull(
                    operation.run(captured), "session operation result");
        } catch (RuntimeException | Error failure) {
            finishFailedOperation(captured);
            throw failure;
        }

        boolean changed;
        synchronized (monitor) {
            if (closed || session != captured) {
                operationRunning = false;
                throw new IllegalStateException(
                        "The Designer command session changed during an operation");
            }
            changed = result.changed();
            if (changed) {
                session = result.session();
            } else if (result.session() != captured) {
                operationRunning = false;
                throw new IllegalStateException(
                        "A rejected Designer command substituted the input session");
            }
            operationRunning = false;
        }
        if (changed) {
            fireChangeSafely();
        }
        return result;
    }

    private void beginOperationLocked() {
        requireOpenLocked();
        if (operationRunning) {
            throw new IllegalStateException(
                    "Another Designer command-session operation is already running");
        }
        operationRunning = true;
    }

    private void finishFailedOperation(DesignerCommandSession captured) {
        synchronized (monitor) {
            if (session == captured) {
                operationRunning = false;
            }
        }
    }

    private void requireOpenLocked() {
        if (closed) {
            throw new IllegalStateException("The Designer command session is closed");
        }
    }

    private static String presentation(
            String operation,
            Optional<DesignerCommandEdit> selected) {
        if (selected.isEmpty()) {
            return operation;
        }
        String action = switch (selected.orElseThrow().forward().kind()) {
            case ADD_WIDGET -> "Add Flutter Widget";
            case REMOVE_WIDGET -> "Remove Flutter Widget";
            case MOVE_WIDGET -> "Move Flutter Widget";
            case WRAP_WIDGET -> "Wrap Flutter Widget";
            case SET_PROPERTY -> "Set Flutter Property";
            case RESET_PROPERTY -> "Reset Flutter Property";
        };
        return operation + ' ' + action;
    }

    private static String reason(DesignerCommandSessionResult result) {
        return result.diagnostics().stream()
                .findFirst()
                .map(value -> value.code() + " at "
                        + (value.path().isEmpty() ? "/" : value.path())
                        + ": " + value.message())
                .orElse("Designer command returned " + result.status());
    }

    enum PendingTransitionKind {
        APPLY,
        UNDO,
        REDO
    }

    /**
     * Monitor-only staged peer assignment. The argument reports whether the
     * command-session owner will close as part of this successful adoption.
     * Implementations must not throw or publish outward callbacks.
     */
    @FunctionalInterface
    interface CloseAwareStagedPeerCommit {
        void commit(boolean ownerClosePending);
    }

    /** Monitor-only Source-save peer assignment with deferred publication. */
    @FunctionalInterface
    interface CloseAwareSourceAnchorPeerCommit {
        void commit(boolean ownerClosePending);
    }

    /** Monitor-only durable-save peer assignment with deferred publication. */
    @FunctionalInterface
    interface CloseAwareDurableSavePeerCommit {
        void commit(boolean ownerClosePending);
    }

    /**
     * Result of deriving a command while retaining the predecessor cursor.
     * Only a changed result contains a lease.
     */
    record PendingCommandAttempt(
            DesignerCommandSessionResult result,
            Optional<PendingCommandLease> lease) {
        PendingCommandAttempt {
            Objects.requireNonNull(result, "result");
            lease = Objects.requireNonNull(lease, "lease");
            if (result.changed() != lease.isPresent()) {
                throw new IllegalArgumentException(
                        "Only a changed command result may publish a pending lease");
            }
            lease.ifPresent(value -> {
                if (value.kind != PendingTransitionKind.APPLY
                        || result.session() != value.afterSessionIdentity
                        || result.edit().orElseThrow() != value.editIdentity) {
                    throw new IllegalArgumentException(
                            "The command result does not retain its exact pending lease identities");
                }
            });
        }
    }

    /** Concrete rejection used by identity-fenced UI admission. */
    static final class StaleRevisionException extends IllegalStateException {
        StaleRevisionException(String message) {
            super(message);
        }
    }

    /** Result of deriving an Undo or Redo target without moving the cursor. */
    record PendingTransitionAttempt(
            PendingTransitionKind kind,
            DesignerCommandSessionResult result,
            Optional<PendingCommandLease> lease) {
        PendingTransitionAttempt {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(result, "result");
            lease = Objects.requireNonNull(lease, "lease");
            if (kind == PendingTransitionKind.APPLY) {
                throw new IllegalArgumentException(
                        "A cursor transition attempt must be UNDO or REDO");
            }
            if (result.changed() != lease.isPresent()) {
                throw new IllegalArgumentException(
                        "Only a changed cursor result may publish a pending lease");
            }
            lease.ifPresent(value -> {
                DesignerCommandStatus expected = kind == PendingTransitionKind.UNDO
                        ? DesignerCommandStatus.UNDONE
                        : DesignerCommandStatus.REDONE;
                if (value.kind != kind
                        || result.status() != expected
                        || result.session() != value.afterSessionIdentity
                        || result.edit().orElseThrow() != value.editIdentity) {
                    throw new IllegalArgumentException(
                            "The cursor result does not retain its exact pending transition identities");
                }
            });
        }
    }

    /**
     * One-shot, identity-bound APPLY, UNDO or REDO capability. It contains
     * pure revision evidence only and performs no analyzer, editor or
     * filesystem operation.
     */
    static final class PendingCommandLease implements AutoCloseable {
        private final DesignerCommandSessionOrchestrator owner;
        private final PendingTransitionKind kind;
        private final DesignerCommandSession beforeSessionIdentity;
        private final DesignerCommandRevision beforeRevisionIdentity;
        private final DesignerCommandSession afterSessionIdentity;
        private final DesignerCommandRevision afterRevisionIdentity;
        private final DesignerCommandEdit editIdentity;
        private final PreparedDesignerPair physicalPredecessorPairIdentity;
        private LeaseResolution resolution = LeaseResolution.ACTIVE;
        /** Exact coordinator-owned peer-transition capability, while claimed. */
        private Object transitionClaimIdentity;

        private PendingCommandLease(
                DesignerCommandSessionOrchestrator owner,
                PendingTransitionKind kind,
                DesignerCommandSession beforeSessionIdentity,
                DesignerCommandSession afterSessionIdentity,
                DesignerCommandEdit editIdentity,
                PreparedDesignerPair physicalPredecessorPairIdentity) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.beforeSessionIdentity = Objects.requireNonNull(
                    beforeSessionIdentity, "beforeSessionIdentity");
            this.beforeRevisionIdentity = beforeSessionIdentity.current();
            this.afterSessionIdentity = Objects.requireNonNull(
                    afterSessionIdentity, "afterSessionIdentity");
            this.afterRevisionIdentity = afterSessionIdentity.current();
            this.editIdentity = Objects.requireNonNull(editIdentity, "editIdentity");
            this.physicalPredecessorPairIdentity =
                    physicalPredecessorPairIdentity;
            boolean adjacent = switch (kind) {
                case APPLY, REDO -> editIdentity.beforeRevision()
                                == beforeRevisionIdentity
                        && editIdentity.afterRevision() == afterRevisionIdentity;
                case UNDO -> editIdentity.afterRevision() == beforeRevisionIdentity
                        && editIdentity.beforeRevision() == afterRevisionIdentity;
            };
            if (beforeSessionIdentity == afterSessionIdentity
                    || beforeSessionIdentity.catalog()
                            != afterSessionIdentity.catalog()
                    || !adjacent) {
                throw new IllegalArgumentException(
                        "A pending transition lease must bind exact adjacent before and after identities");
            }
            if (physicalPredecessorPairIdentity != null) {
                if (kind != PendingTransitionKind.APPLY
                        || !physicalPredecessorPairIdentity
                                .prospectiveDocument()
                                .equals(beforeRevisionIdentity.document())
                        || !physicalPredecessorPairIdentity.prospectiveFd()
                                .equals(beforeRevisionIdentity.fdSnapshot())) {
                    throw new IllegalArgumentException(
                            "A physical-endpoint command lease must retain its exact predecessor pair");
                }
                if (afterRevisionIdentity.persistenceKind()
                        == DesignerRevisionPersistenceKind.PAIRED) {
                    PreparedDesignerPair candidatePair = afterRevisionIdentity
                            .preparedPair().orElseThrow();
                    if (candidatePair.baselineFd()
                                != physicalPredecessorPairIdentity.baselineFd()
                            || candidatePair.dartTransition().baseline()
                                != physicalPredecessorPairIdentity
                                        .dartTransition().baseline()
                            || !Arrays.equals(
                                    candidatePair.liveDartBytes(),
                                    physicalPredecessorPairIdentity
                                            .liveDartBytes())) {
                        throw new IllegalArgumentException(
                                "A physical-endpoint paired target must retain the exact durable anchor and Dart envelope");
                    }
                } else if (afterRevisionIdentity.persistenceKind()
                        == DesignerRevisionPersistenceKind.BASELINE) {
                    if (afterRevisionIdentity.preparedPair().isPresent()
                            || afterRevisionIdentity.sourceTransition().isPresent()
                            || afterRevisionIdentity.fdSnapshot()
                                != physicalPredecessorPairIdentity.baselineFd()
                            || !afterRevisionIdentity.document().equals(
                                    physicalPredecessorPairIdentity
                                            .baselineDocument())
                            || !Arrays.equals(
                                    afterRevisionIdentity.dartCandidateBytes(),
                                    physicalPredecessorPairIdentity
                                            .liveDartBytes())) {
                        throw new IllegalArgumentException(
                                "A physical-endpoint baseline target must return to the exact durable model and Dart envelope");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "A physical-endpoint command may produce only an exact paired or baseline target");
                }
            }
        }

        PendingTransitionKind kind() {
            return kind;
        }

        DesignerCommandRevision predecessorRevision() {
            return beforeRevisionIdentity;
        }

        DesignerCommandRevision candidateRevision() {
            return afterRevisionIdentity;
        }

        DesignerCommandSessionOrchestrator owner() {
            return owner;
        }

        DesignerCommandEdit edit() {
            return editIdentity;
        }

        /**
         * Exact noncanonical predecessor pair used for command derivation, if
         * this APPLY was admitted through the physical-endpoint seam.
         */
        Optional<PreparedDesignerPair> physicalPredecessorPairIdentity() {
            return Optional.ofNullable(physicalPredecessorPairIdentity);
        }

        WidgetCatalog catalogIdentity() {
            return beforeSessionIdentity.catalog();
        }

        long maxRetainedPairBytes() {
            return beforeSessionIdentity.limits().maxRetainedPairBytes();
        }

        boolean ownsExactActiveTransition() {
            return owner.ownsExactActiveTransition(this);
        }

        void adoptStaged() {
            owner.adoptPendingCommand(this);
        }

        DeferredLeaseEffects adoptStagedDeferredEffects() {
            return owner.adoptPendingCommandDeferredEffects(this);
        }

        DeferredLeaseEffects adoptStagedDeferredEffects(
                PairSaveEvidence exactCandidateEvidence,
                Runnable coordinatorCommitNoThrow) {
            return owner.adoptPendingCommandDeferredEffects(
                    this,
                    exactCandidateEvidence,
                    coordinatorCommitNoThrow);
        }

        void claimForReplacement(Object replacementClaim) {
            claimForTransition(replacementClaim);
        }

        void claimForTransition(Object transitionClaim) {
            owner.claimPendingCommandForTransition(this, transitionClaim);
        }

        DeferredLeaseEffects adoptExactTargetDeferredEffects(
                DesignerCommandRevision exactTarget,
                Runnable peerCommitNoThrow,
                Object transitionClaim) {
            return owner.adoptExactTargetDeferredEffects(
                    this,
                    exactTarget,
                    peerCommitNoThrow,
                    transitionClaim);
        }

        DeferredLeaseEffects adoptExactTargetCloseAwareDeferredEffects(
                DesignerCommandRevision exactTarget,
                CloseAwareStagedPeerCommit peerCommitNoThrow,
                Object transitionClaim) {
            return owner.adoptExactTargetCloseAwareDeferredEffects(
                    this,
                    exactTarget,
                    peerCommitNoThrow,
                    Objects.requireNonNull(
                            transitionClaim, "transitionClaim"));
        }

        DeferredLeaseEffects adoptStagedDeferredEffects(
                PairSaveEvidence exactCandidateEvidence,
                Runnable coordinatorCommitNoThrow,
                Object replacementClaim) {
            return owner.adoptPendingCommandDeferredEffects(
                    this,
                    exactCandidateEvidence,
                    coordinatorCommitNoThrow,
                    replacementClaim);
        }

        /**
         * Pair-coordinator joint adoption with an exact owner-close
         * handshake. The callback executes only after all evidence and claim
         * checks succeed and before either final owner state is installed.
         */
        DeferredLeaseEffects adoptStagedCloseAwareDeferredEffects(
                PairSaveEvidence exactCandidateEvidence,
                CloseAwareStagedPeerCommit coordinatorCommitNoThrow,
                Object transitionClaim) {
            return owner.adoptPendingCommandCloseAwareDeferredEffects(
                    this,
                    exactCandidateEvidence,
                    coordinatorCommitNoThrow,
                    transitionClaim);
        }

        void abort() {
            owner.abortPendingCommand(this);
        }

        DeferredLeaseEffects abortDeferredEffects() {
            return owner.abortPendingCommandDeferredEffects(this);
        }

        DeferredLeaseEffects abortToExactPredecessorDeferredEffects(
                Object replacementClaim) {
            return owner.abortPendingCommandToExactPredecessorDeferredEffects(
                    this, replacementClaim);
        }

        void invalidate() {
            owner.invalidatePendingCommand(this);
        }

        DeferredLeaseEffects invalidateDeferredEffects() {
            return owner.invalidatePendingCommandDeferredEffects(this);
        }

        DeferredLeaseEffects invalidateClaimedDeferredEffects(
                Object replacementClaim) {
            return owner.invalidateClaimedPendingCommandDeferredEffects(
                    this, replacementClaim);
        }

        @Override
        public void close() {
            abort();
        }
    }

    /**
     * Identity-bound pre-commit lease. Package-private accessors expose only
     * clone-safe evidence; this type performs no I/O and grants no write
     * authority.
     */
    static final class DurableSaveLease implements AutoCloseable {
        private final DesignerCommandSessionOrchestrator owner;
        private final DesignerCommandSession sessionIdentity;
        private final DesignerCommandRevision revisionIdentity;
        private final DesignerCommandSession precomputedSaved;
        private final byte[] durableFdBytes;
        private final byte[] durableDartBytes;
        private final PreparedDesignerPair exactPairIdentity;
        private LeaseResolution resolution = LeaseResolution.ACTIVE;

        private DurableSaveLease(
                DesignerCommandSessionOrchestrator owner,
                DesignerCommandSession sessionIdentity,
                DesignerCommandRevision revisionIdentity,
                DesignerCommandSession precomputedSaved,
                byte[] durableFdBytes,
                byte[] durableDartBytes,
                PreparedDesignerPair exactPairIdentity) {
            this.owner = owner;
            this.sessionIdentity = sessionIdentity;
            this.revisionIdentity = revisionIdentity;
            this.precomputedSaved = precomputedSaved;
            this.durableFdBytes = durableFdBytes.clone();
            this.durableDartBytes = durableDartBytes.clone();
            this.exactPairIdentity = exactPairIdentity;
            DesignerCommandRevision reanchoredLeaseRevision =
                    precomputedSaved.retainedRevision(
                            revisionIdentity.revisionId()).orElse(null);
            byte[] expectedSavedDart = exactPairIdentity == null
                    ? revisionIdentity.dartCandidateBytes()
                    : exactPairIdentity.prospectiveDartBytes();
            if (!sessionIdentity.dirty()
                    || sessionIdentity.current() != revisionIdentity
                    || precomputedSaved.dirty()
                    || precomputedSaved.savedRevisionId()
                            != revisionIdentity.revisionId()
                    || precomputedSaved.current().revisionId()
                            != revisionIdentity.revisionId()
                    || reanchoredLeaseRevision != precomputedSaved.current()
                    || precomputedSaved.current().persistenceKind()
                            != DesignerRevisionPersistenceKind.BASELINE
                    || precomputedSaved.current().sourceIntegrity()
                            != precomputedSaved.durableSourceIntegrity()
                    || !Arrays.equals(
                            precomputedSaved.durableFdAnchor().copyBytes(),
                            revisionIdentity.fdBytes())
                    || !Arrays.equals(
                            precomputedSaved.durableDartAnchorBytes(),
                            expectedSavedDart)
                    || !Arrays.equals(
                            precomputedSaved.current().dartCandidateBytes(),
                            expectedSavedDart)) {
                throw new IllegalArgumentException(
                        "A durable save lease must bind one exact dirty revision and its saved anchor");
            }
            if (exactPairIdentity != null
                    && (!exactPairIdentity.prospectiveDocument()
                            .equals(revisionIdentity.document())
                    || !exactPairIdentity.prospectiveFd()
                            .equals(revisionIdentity.fdSnapshot())
                    || !Arrays.equals(
                            exactPairIdentity.baselineFdBytes(),
                            this.durableFdBytes)
                    || !Arrays.equals(
                            exactPairIdentity.baselineDartBytes(),
                            this.durableDartBytes))) {
                throw new IllegalArgumentException(
                        "An exact-pair durable lease must retain its exact prior and prospective pairs");
            }
            if (revisionIdentity.persistenceKind()
                    == DesignerRevisionPersistenceKind.FD_ONLY) {
                if (!Arrays.equals(
                        revisionIdentity.dartCandidateBytes(), this.durableDartBytes)
                        || Arrays.equals(
                                revisionIdentity.fdBytes(), this.durableFdBytes)) {
                    throw new IllegalArgumentException(
                            "An FD_ONLY lease must retain exact Dart and change exact .fd bytes");
                }
            }
        }

        DesignerCommandRevision revision() {
            return revisionIdentity;
        }

        long savedRevisionId() {
            return precomputedSaved.savedRevisionId();
        }

        DesignerCommandRevision reanchoredRevision(long revisionId) {
            return precomputedSaved.retainedRevision(revisionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "The durable save lease does not retain revision id "
                    + revisionId));
        }

        DesignerCommandRevision reanchoredPhysicalRevision(
                long revisionId,
                byte[] exactLiveTemplate) {
            return precomputedSaved.rederiveRetainedRevision(
                    revisionId, exactLiveTemplate);
        }

        DesignerCommandRevision reanchoredPhysicalRevisionByProjectingAnchor(
                long revisionId,
                byte[] exactHistoricalTemplate) {
            return precomputedSaved
                    .rederiveRetainedRevisionByProjectingAnchor(
                            revisionId, exactHistoricalTemplate);
        }

        WidgetCatalog catalogIdentity() {
            return sessionIdentity.catalog();
        }

        byte[] durableFdBytes() {
            return durableFdBytes.clone();
        }

        byte[] durableDartBytes() {
            return durableDartBytes.clone();
        }

        long maxRetainedPairBytes() {
            return sessionIdentity.limits().maxRetainedPairBytes();
        }

        DartSourceIntegrityResult savedSourceIntegrityIdentity() {
            return precomputedSaved.durableSourceIntegrity();
        }

        DartThreeWayIntegrityResult savedThreeWayIntegrityIdentity() {
            return precomputedSaved.durableThreeWayIntegrity();
        }

        boolean ownsExactActiveRevision() {
            return owner.ownsExactActiveRevision(this);
        }

        void adoptCommitted() {
            owner.adoptCommitted(this);
        }

        DeferredLeaseEffects adoptCommittedDeferredEffects() {
            return owner.adoptCommittedDeferredEffects(this);
        }

        DeferredLeaseEffects adoptCommittedDeferredEffects(
                Runnable peerCommitNoThrow) {
            return owner.adoptCommittedDeferredEffects(
                    this, peerCommitNoThrow);
        }

        DeferredLeaseEffects adoptCommittedCloseAwareDeferredEffects(
                CloseAwareDurableSavePeerCommit peerCommitNoThrow) {
            return owner.adoptCommittedCloseAwareDeferredEffects(
                    this, peerCommitNoThrow);
        }

        void abort() {
            owner.abortDurableSave(this);
        }

        void invalidate() {
            owner.invalidateDurableSave(this);
        }

        @Override
        public void close() {
            abort();
        }
    }

    /**
     * Identity-bound pre-commit Source-anchor capability. It retains the exact
     * clean command session, its pure precomputed re-anchor, and clone-safe old
     * and planned durable bytes. It performs no I/O and grants no persistence
     * authority.
     */
    static final class SourceAnchorLease implements AutoCloseable {
        private final DesignerCommandSessionOrchestrator owner;
        private final DesignerCommandSession sessionIdentity;
        private final DesignerCommandRevision revisionIdentity;
        private final DesignerCommandSession precomputedReanchored;
        private final byte[] priorFdBytes;
        private final byte[] priorDartBytes;
        private final byte[] candidateFdBytes;
        private final byte[] candidateDartBytes;
        private LeaseResolution resolution = LeaseResolution.ACTIVE;

        private SourceAnchorLease(
                DesignerCommandSessionOrchestrator owner,
                DesignerCommandSession sessionIdentity,
                DesignerCommandRevision revisionIdentity,
                DesignerCommandSession precomputedReanchored,
                byte[] priorFdBytes,
                byte[] priorDartBytes,
                byte[] candidateDartBytes) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.sessionIdentity = Objects.requireNonNull(
                    sessionIdentity, "sessionIdentity");
            this.revisionIdentity = Objects.requireNonNull(
                    revisionIdentity, "revisionIdentity");
            this.precomputedReanchored = Objects.requireNonNull(
                    precomputedReanchored, "precomputedReanchored");
            this.priorFdBytes = Objects.requireNonNull(
                    priorFdBytes, "priorFdBytes").clone();
            this.priorDartBytes = Objects.requireNonNull(
                    priorDartBytes, "priorDartBytes").clone();
            this.candidateFdBytes = precomputedReanchored
                    .durableFdAnchor().copyBytes();
            this.candidateDartBytes = Objects.requireNonNull(
                    candidateDartBytes, "candidateDartBytes").clone();

            DesignerCommandRevision reanchoredCurrent =
                    precomputedReanchored.retainedRevision(
                            revisionIdentity.revisionId()).orElse(null);
            if (sessionIdentity.dirty()
                    || sessionIdentity.current() != revisionIdentity
                    || sessionIdentity.savedRevisionId()
                            != revisionIdentity.revisionId()
                    || revisionIdentity.persistenceKind()
                            != DesignerRevisionPersistenceKind.BASELINE
                    || !Arrays.equals(
                            sessionIdentity.durableFdAnchor().copyBytes(),
                            this.priorFdBytes)
                    || !Arrays.equals(
                            sessionIdentity.durableDartAnchorBytes(),
                            this.priorDartBytes)
                    || !Arrays.equals(
                            revisionIdentity.fdBytes(), this.priorFdBytes)
                    || !Arrays.equals(
                            revisionIdentity.dartCandidateBytes(),
                            this.priorDartBytes)
                    || precomputedReanchored.catalog()
                            != sessionIdentity.catalog()
                    || precomputedReanchored.cursor()
                            != sessionIdentity.cursor()
                    || precomputedReanchored.revisionCount()
                            != sessionIdentity.revisionCount()
                    || precomputedReanchored.dirty()
                    || precomputedReanchored.savedRevisionId()
                            != sessionIdentity.savedRevisionId()
                    || precomputedReanchored.current().revisionId()
                            != revisionIdentity.revisionId()
                    || reanchoredCurrent != precomputedReanchored.current()
                    || precomputedReanchored.current().persistenceKind()
                            != DesignerRevisionPersistenceKind.BASELINE
                    || precomputedReanchored.current().sourceIntegrity()
                            != precomputedReanchored.durableSourceIntegrity()
                    || !precomputedReanchored.current().document()
                            .equals(revisionIdentity.document())
                    || !Arrays.equals(
                            this.candidateFdBytes, this.priorFdBytes)
                    || !Arrays.equals(
                            precomputedReanchored.current().fdBytes(),
                            this.candidateFdBytes)
                    || !Arrays.equals(
                            precomputedReanchored.durableDartAnchorBytes(),
                            this.candidateDartBytes)
                    || !Arrays.equals(
                            precomputedReanchored.current()
                                    .dartCandidateBytes(),
                            this.candidateDartBytes)) {
                throw new IllegalArgumentException(
                        "A Source-anchor lease must bind one exact clean revision and an FD-stable Dart re-anchor");
            }
        }

        DesignerCommandRevision revision() {
            return revisionIdentity;
        }

        long savedRevisionId() {
            return precomputedReanchored.savedRevisionId();
        }

        DesignerCommandRevision reanchoredRevision(long revisionId) {
            return precomputedReanchored.retainedRevision(revisionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "The Source-anchor lease does not retain revision id "
                    + revisionId));
        }

        DesignerCommandRevision reanchoredPhysicalRevisionByProjectingAnchor(
                long revisionId,
                byte[] exactHistoricalTemplate) {
            return precomputedReanchored
                    .rederiveRetainedRevisionByProjectingAnchor(
                            revisionId, exactHistoricalTemplate);
        }

        WidgetCatalog catalogIdentity() {
            return sessionIdentity.catalog();
        }

        byte[] priorFdBytes() {
            return priorFdBytes.clone();
        }

        byte[] priorDartBytes() {
            return priorDartBytes.clone();
        }

        byte[] candidateFdBytes() {
            return candidateFdBytes.clone();
        }

        byte[] candidateDartBytes() {
            return candidateDartBytes.clone();
        }

        long maxRetainedPairBytes() {
            return sessionIdentity.limits().maxRetainedPairBytes();
        }

        DartSourceIntegrityResult savedSourceIntegrityIdentity() {
            return precomputedReanchored.durableSourceIntegrity();
        }

        DartThreeWayIntegrityResult savedThreeWayIntegrityIdentity() {
            return precomputedReanchored.durableThreeWayIntegrity();
        }

        boolean ownsExactActiveRevision() {
            return owner.ownsExactActiveRevision(this);
        }

        void adoptCommitted() {
            owner.adoptSourceAnchor(this);
        }

        DeferredLeaseEffects adoptCommittedDeferredEffects() {
            return owner.adoptSourceAnchorDeferredEffects(this);
        }

        DeferredLeaseEffects adoptCommittedDeferredEffects(
                Runnable peerCommitNoThrow) {
            return owner.adoptSourceAnchorDeferredEffects(
                    this, peerCommitNoThrow);
        }

        DeferredLeaseEffects adoptCommittedCloseAwareDeferredEffects(
                CloseAwareSourceAnchorPeerCommit peerCommitNoThrow) {
            return owner.adoptSourceAnchorCloseAwareDeferredEffects(
                    this, peerCommitNoThrow);
        }

        void abort() {
            owner.abortSourceAnchor(this);
        }

        DeferredLeaseEffects abortDeferredEffects() {
            return owner.abortSourceAnchorDeferredEffects(this);
        }

        DeferredLeaseEffects abortUnchangedCloseAwareDeferredEffects(
                CloseAwareSourceAnchorPeerCommit peerCommitNoThrow) {
            return owner.abortSourceAnchorUnchangedCloseAwareDeferredEffects(
                    this, peerCommitNoThrow);
        }

        void invalidate() {
            owner.invalidateSourceAnchor(this);
        }

        @Override
        public void close() {
            abort();
        }
    }

    private enum LeaseResolution {
        ACTIVE,
        ADOPTED,
        ABORTED,
        INVALIDATED
    }

    /** One-shot callback publication returned by monitor-only adoption. */
    static final class DeferredLeaseEffects {
        private final DesignerCommandSessionOrchestrator owner;
        private final boolean closeBinding;
        private boolean published;

        private DeferredLeaseEffects(
                DesignerCommandSessionOrchestrator owner,
                boolean closeBinding) {
            this.owner = owner;
            this.closeBinding = closeBinding;
        }

        static DeferredLeaseEffects none() {
            return new DeferredLeaseEffects(null, false);
        }

        void publish() {
            synchronized (this) {
                if (published) {
                    return;
                }
                published = true;
            }
            if (owner != null) {
                owner.finishLeaseEffects(closeBinding);
            }
        }
    }

    @FunctionalInterface
    private interface SessionOperation {
        DesignerCommandSessionResult run(DesignerCommandSession value);
    }
}
