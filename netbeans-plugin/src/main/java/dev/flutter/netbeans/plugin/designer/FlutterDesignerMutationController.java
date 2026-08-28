package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalysisOperation;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalyzer;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandDiagnostic;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionOpenResult;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionResult;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.RemoveWidget;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainStatus;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 * DataObject-owned admission boundary for Flutter Designer mutations.
 *
 * <p>The durable {@link FlutterDesignerDocumentState.Current} remains an
 * on-disk fact. Unsaved command revisions are published separately through
 * {@link Snapshot}. The command session is acquired lazily by the first
 * mutation, so merely opening an {@code .fd} form never claims semantic
 * Undo/Redo ownership.</p>
 */
final class FlutterDesignerMutationController implements AutoCloseable {
    static final String PROP_SNAPSHOT = "mutationSnapshot";

    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerMutationController.class.getName());

    private final Object monitor = new Object();
    private final FlutterDesignerDataObject dataObject;
    private final FlutterDesignerDocumentController documentController;
    private final FlutterDesignerEditorSupport editor;
    private final PairSaveCoordinator pairCoordinator;
    private final DesignerCombinedUndoRedo combinedUndoRedo;
    private final FlutterToolchainService toolchains;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final AtomicLong operationIds = new AtomicLong();
    private final PropertyChangeListener documentListener;
    private final PropertyChangeListener pairListener;
    private final RequestProcessor worker = new RequestProcessor(
            FlutterDesignerMutationController.class.getName(), 1, true);

    private volatile Snapshot snapshot = Snapshot.waiting(
            "Waiting for a validated Flutter Designer model.");
    private AnalyzerFactory analyzerFactory = AnalyzerFactory.production();
    private volatile SessionAdmissionHook sessionAdmissionHook = () -> { };
    private volatile CommitBoundaryHook commitBoundaryHook = () -> { };
    private DesignerCommandSessionOrchestrator sessionOwner;
    /** Owner lifetime transferred from the closed UI to retained pair history. */
    private DesignerCommandSessionOrchestrator detachedHistoryOwner;
    private ChangeListener sessionListener;
    private DesignerCommandRevision boundRevision;
    private FlutterDesignerDocumentState.Current sessionCurrent;
    private FlutterDesignerDocumentState.Current readyCurrent;
    private AnalysisEnvironment analysisEnvironment;
    private DartCandidateAnalysisOperation activeAnalysis;
    private CompletableFuture<MutationResult> activeCompletion;
    /** Stable close result used by both close() and a racing worker. */
    private CompletableFuture<MutationResult> forcedCompletion;
    private MutationResult forcedCompletionResult;
    private long refreshGeneration;
    private long readyPairEpoch = -1L;
    private long activeOperationId = -1L;
    private boolean mutationRunning;
    /**
     * True only after analyzer success has been accepted as the last
     * cancellable edge and before pair admission starts.
     */
    private boolean commitBoundaryCrossed;
    /** Close was requested after the active mutation crossed its commit edge. */
    private boolean closeAfterActiveCommit;
    /** A deferred close still has to decide whether Pair history takes the owner. */
    private boolean historyOwnerTransferPending;
    /** DataObject disposal must release even an owner retained for Save/Undo. */
    private boolean disposalRequested;
    private boolean pairListenerRegistered = true;
    private boolean closed;

    FlutterDesignerMutationController(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerDocumentController documentController,
            FlutterDesignerEditorSupport editor,
            PairSaveCoordinator pairCoordinator,
            DesignerCombinedUndoRedo combinedUndoRedo) {
        this(
                dataObject,
                documentController,
                editor,
                pairCoordinator,
                combinedUndoRedo,
                new FlutterToolchainService());
    }

    FlutterDesignerMutationController(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerDocumentController documentController,
            FlutterDesignerEditorSupport editor,
            PairSaveCoordinator pairCoordinator,
            DesignerCombinedUndoRedo combinedUndoRedo,
            FlutterToolchainService toolchains) {
        this.dataObject = Objects.requireNonNull(dataObject, "dataObject");
        this.documentController = Objects.requireNonNull(
                documentController, "documentController");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.pairCoordinator = Objects.requireNonNull(
                pairCoordinator, "pairCoordinator");
        this.combinedUndoRedo = Objects.requireNonNull(
                combinedUndoRedo, "combinedUndoRedo");
        this.toolchains = Objects.requireNonNull(toolchains, "toolchains");
        documentListener = event -> {
            if (FlutterDesignerDocumentController.PROP_STATE
                    .equals(event.getPropertyName())) {
                scheduleRefresh(documentController.state());
            }
        };
        pairListener = event -> {
            if (PairSaveCoordinator.PROP_STATE.equals(event.getPropertyName())) {
                pairStateChanged((PairSaveCoordinatorSnapshot) event.getNewValue());
            }
        };
        documentController.addPropertyChangeListener(documentListener);
        pairCoordinator.addPropertyChangeListener(pairListener);
        scheduleRefresh(documentController.state());
    }

    Snapshot snapshot() {
        return snapshot;
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Starts one exact asynchronous semantic mutation.
     *
     * <p>The token must be the same object published by the current READY
     * snapshot. This identity check rejects stale property editors without
     * depending on a display revision number.</p>
     */
    CompletableFuture<MutationResult> submit(
            RevisionToken expectedToken,
            DesignerCommand command,
            String targetLabel) {
        Objects.requireNonNull(expectedToken, "expectedToken");
        Objects.requireNonNull(command, "command");
        String operation = operationName(command);
        String target = requireText(targetLabel, "targetLabel");
        CompletableFuture<MutationResult> completion = new CompletableFuture<>();
        PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
        long operationId;
        FlutterDesignerDocumentState.Current current;
        AnalysisEnvironment environment;
        synchronized (monitor) {
            if (closed) {
                return completed(MutationResult.cancelled(
                        operation, target, "The Flutter Designer form is closed."));
            }
            if (mutationRunning) {
                return completed(MutationResult.rejected(
                        operation,
                        target,
                        "Another Flutter Designer mutation is still being analyzed."));
            }
            if (snapshot.status() != Status.READY
                    || snapshot.token().orElse(null) != expectedToken
                    || expectedToken.ownerIdentity != this
                    || expectedToken.documentStateIdentity != readyCurrent
                    || expectedToken.commandRevisionIdentity != boundRevision
                    || expectedToken.pairEpoch != readyPairEpoch
                    || expectedToken.pairEpoch != pairState.epoch()
                    || readyCurrent == null
                    || analysisEnvironment == null) {
                return completed(MutationResult.rejected(
                        operation,
                        target,
                        "The selected Flutter Designer revision is stale; select the widget again."));
            }
            if (documentController.state() != readyCurrent) {
                scheduleRefresh(documentController.state());
                return completed(MutationResult.rejected(
                        operation,
                        target,
                        "The loaded Flutter Designer model changed; wait for the new revision."));
            }
            String pairUnavailable = pairMutationUnavailableReason(pairState);
            if (pairUnavailable != null) {
                publishLocked(snapshot.blocked(
                        operation, target, pairUnavailable));
                return completed(MutationResult.rejected(
                        operation, target, pairUnavailable));
            }

            operationId = operationIds.incrementAndGet();
            activeOperationId = operationId;
            mutationRunning = true;
            commitBoundaryCrossed = false;
            closeAfterActiveCommit = false;
            activeCompletion = completion;
            current = readyCurrent;
            environment = analysisEnvironment;
            publishLocked(snapshot.applying(
                    operation,
                    target,
                    "Analyzing the generated Dart candidate before applying the change."));
        }

        worker.post(() -> executeMutation(
                operationId,
                expectedToken,
                current,
                environment,
                command,
                operation,
                target,
                completion));
        return completion;
    }

    private void executeMutation(
            long operationId,
            RevisionToken expectedToken,
            FlutterDesignerDocumentState.Current current,
            AnalysisEnvironment environment,
            DesignerCommand command,
            String operation,
            String target,
            CompletableFuture<MutationResult> completion) {
        MutationResult result = null;
        DesignerCommandSessionOrchestrator owner = null;
        Error terminalError = null;
        try {
            sessionAdmissionHook.beforeAdmission();
            SessionAdmission admission = acquireSession(
                    operationId, expectedToken, current);
            owner = admission.owner();
            PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
            PairSaveCoordinatorStatus status = pairState.status();
            result = switch (status) {
                case CLEAN -> executeInitial(
                        operationId,
                        owner,
                        admission.expectedRevision(),
                        expectedToken.pairEpoch,
                        current,
                        environment,
                        command,
                        operation,
                        target);
                case DIRTY_SOURCE -> pairCoordinator
                        .ownsExactSemanticBaselineRevision(
                                pairState.epoch(),
                                owner,
                                admission.expectedRevision())
                        ? executeInitial(
                                operationId,
                                owner,
                                admission.expectedRevision(),
                                expectedToken.pairEpoch,
                                current,
                                environment,
                                command,
                                operation,
                                target)
                        : MutationResult.rejected(
                                operation,
                                target,
                                Objects.requireNonNullElse(
                                        pairMutationUnavailableReason(
                                                pairState),
                                        "The dirty Dart source is not an exact Designer baseline cursor."));
                case STAGED_PAIR -> executeReplacement(
                        operationId,
                        owner,
                        admission.expectedRevision(),
                        expectedToken.pairEpoch,
                        environment,
                        command,
                        operation,
                        target);
                default -> MutationResult.rejected(
                        operation,
                        target,
                        Objects.requireNonNullElse(
                                pairMutationUnavailableReason(pairState),
                                "The Flutter Designer pair changed before mutation admission."));
            };
        } catch (DesignerCommandSessionOrchestrator.StaleRevisionException
                | PairSaveCoordinator.StalePairEpochException stale) {
            result = MutationResult.rejected(
                    operation, target, stale.getMessage());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            result = MutationResult.cancelled(
                    operation, target, "Flutter Designer analysis was interrupted.");
        } catch (CancellationException cancelled) {
            result = MutationResult.cancelled(
                    operation, target, "Flutter Designer analysis was cancelled.");
        } catch (IOException | RuntimeException failure) {
            LOGGER.log(Level.FINE,
                    operation + " failed for " + target, failure);
            result = MutationResult.failed(
                    operation, target, concreteReason(failure));
            terminalError = fatalError(failure);
        } catch (Error fatal) {
            LOGGER.log(Level.SEVERE,
                    operation + " failed fatally for " + target, fatal);
            result = MutationResult.failed(
                    operation, target, concreteReason(fatal));
            terminalError = fatalError(fatal);
        }
        try {
            finishMutation(operationId, owner, Objects.requireNonNull(result));
        } catch (RuntimeException | Error finishFailure) {
            LOGGER.log(Level.SEVERE,
                    "Finish " + operation + " failed for " + target,
                    finishFailure);
            result = MutationResult.failed(
                    operation, target,
                    "The mutation finished, but its presentation state could not be reconciled: "
                    + concreteReason(finishFailure));
            Error finishTerminal = fatalError(finishFailure);
            if (finishTerminal != null) {
                if (terminalError == null) {
                    terminalError = finishTerminal;
                } else if (terminalError != finishTerminal) {
                    terminalError.addSuppressed(finishTerminal);
                }
            }
        } finally {
            completion.complete(completionResult(
                    completion, Objects.requireNonNull(result)));
        }
        if (terminalError != null) {
            throw terminalError;
        }
    }

    private SessionAdmission acquireSession(
            long operationId,
            RevisionToken expectedToken,
            FlutterDesignerDocumentState.Current current) throws IOException {
        synchronized (monitor) {
            requireActiveOperationLocked(operationId);
            if (sessionOwner != null) {
                if (!(expectedToken.commandRevisionIdentity
                        instanceof DesignerCommandRevision expectedRevision)) {
                    throw new IOException(
                            "The selected baseline token cannot address an existing Designer session.");
                }
                if (sessionCurrent != current
                        && !exactRevisionMatchesCurrent(boundRevision, current)) {
                    throw new IOException(
                            "The semantic Designer session belongs to another exact pair revision.");
                }
                sessionCurrent = current;
                return new SessionAdmission(sessionOwner, expectedRevision);
            }
            if (expectedToken.commandRevisionIdentity != null) {
                throw new IOException(
                        "The selected Designer session is no longer active.");
            }
        }

        DesignerCommandSessionOpenResult opened = openSession(current);
        if (!opened.ready()) {
            throw new IOException(commandDiagnostics(opened.diagnostics()));
        }
        DesignerCommandSessionOrchestrator candidate =
                new DesignerCommandSessionOrchestrator(
                        opened.session().orElseThrow(), combinedUndoRedo);
        DesignerCommandRevision candidateRevision = candidate.currentRevision();
        ChangeListener listener = event -> sessionChanged(candidate);
        candidate.addChangeListener(listener);
        boolean adopted = false;
        try {
            synchronized (monitor) {
                requireActiveOperationLocked(operationId);
                if (sessionOwner != null) {
                    throw new IOException(
                            "Another semantic Designer session became active.");
                }
                if (readyCurrent != current
                        || documentController.state() != current) {
                    throw new IOException(
                            "The exact loaded Designer revision changed while opening the command session.");
                }
                sessionOwner = candidate;
                sessionListener = listener;
                boundRevision = candidateRevision;
                sessionCurrent = current;
                adopted = true;
                return new SessionAdmission(candidate, candidateRevision);
            }
        } finally {
            if (!adopted) {
                candidate.removeChangeListener(listener);
                candidate.close();
            }
        }
    }

    private MutationResult executeInitial(
            long operationId,
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision expectedRevision,
            long expectedPairEpoch,
            FlutterDesignerDocumentState.Current current,
            AnalysisEnvironment environment,
            DesignerCommand command,
            String operation,
            String target) throws IOException, InterruptedException {
        LiveDartDocumentSnapshot live = editor.liveSnapshot();
        DesignerCommandSessionOrchestrator.PendingCommandAttempt attempt =
                owner.beginCommand(expectedRevision, command);
        if (!attempt.result().changed()) {
            return rejectedCommand(operation, target, attempt.result());
        }
        try (DesignerCommandSessionOrchestrator.PendingCommandLease commandLease =
                        attempt.lease().orElseThrow()) {
            MutationResult unsupported = rejectUnsupportedPersistence(
                    operation, target, commandLease);
            if (unsupported != null) {
                return unsupported;
            }
            try (PairSaveCoordinator.PairPreparation preparation =
                    pairCoordinator.beginPairPreparation(
                            expectedPairEpoch, current, commandLease, live)) {
                PairCandidateAnalysisTicket ticket = preparation.prepareAnalysis(
                        environment.projectRoot(), DartCandidateWarningPolicy.ALLOW);
                DartCandidateAnalysisResult analysis = analyze(
                        operationId, environment, ticket);
                crossCommitBoundary(operationId);
                PairSaveEvidenceResult evidence = preparation.acceptAnalysisAndStage(
                        ticket, analysis);
                return evidence.ready()
                        ? MutationResult.applied(operation, target)
                        : rejectedEvidence(operation, target, evidence);
            }
        }
    }

    private MutationResult executeReplacement(
            long operationId,
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision expectedRevision,
            long expectedPairEpoch,
            AnalysisEnvironment environment,
            DesignerCommand command,
            String operation,
            String target) throws IOException, InterruptedException {
        PairSaveCoordinator.StagedCommandSource source =
                pairCoordinator.captureStagedCommandSource(expectedPairEpoch);
        DesignerCommandSessionOrchestrator.PendingCommandAttempt attempt =
                source.beginCommand(expectedRevision, command);
        if (!attempt.result().changed()) {
            return rejectedCommand(operation, target, attempt.result());
        }
        try (DesignerCommandSessionOrchestrator.PendingCommandLease commandLease =
                        attempt.lease().orElseThrow()) {
            MutationResult unsupported = rejectUnsupportedPersistence(
                    operation, target, commandLease);
            if (unsupported != null) {
                return unsupported;
            }
            try (PairSaveCoordinator.PairReplacement replacement =
                    pairCoordinator.beginStagedReplacement(
                            source, commandLease)) {
                if (commandLease.candidateRevision().persistenceKind()
                        == DesignerRevisionPersistenceKind.BASELINE) {
                    crossCommitBoundary(operationId);
                    replacement.replaceWithExactBaseline();
                    return MutationResult.applied(operation, target);
                }
                PairCandidateAnalysisTicket ticket = replacement.prepareAnalysis(
                        environment.projectRoot(), DartCandidateWarningPolicy.ALLOW);
                DartCandidateAnalysisResult analysis = analyze(
                        operationId, environment, ticket);
                crossCommitBoundary(operationId);
                PairSaveEvidenceResult evidence = replacement.acceptAnalysisAndReplace(
                        ticket, analysis);
                return evidence.ready()
                        ? MutationResult.applied(operation, target)
                        : rejectedEvidence(operation, target, evidence);
            }
        }
    }

    private static MutationResult rejectUnsupportedPersistence(
            String operation,
            String target,
            DesignerCommandSessionOrchestrator.PendingCommandLease lease) {
        if (lease.candidateRevision().persistenceKind()
                != DesignerRevisionPersistenceKind.FD_ONLY) {
            return null;
        }
        return MutationResult.rejected(
                operation,
                target,
                "This Properties slice cannot apply an .fd-only change; "
                + "it currently accepts only an exact paired Dart and .fd candidate.");
    }

    private DartCandidateAnalysisResult analyze(
            long operationId,
            AnalysisEnvironment environment,
            PairCandidateAnalysisTicket ticket)
            throws IOException, InterruptedException {
        DartCandidateAnalysisOperation analysis = analyzerFactory.analyze(
                environment.dartExecutable(), ticket.request());
        boolean registered = false;
        try {
            synchronized (monitor) {
                requireActiveOperationLocked(operationId);
                activeAnalysis = analysis;
                registered = true;
            }
            return analysis.result().toCompletableFuture().get();
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw new IOException(
                    "The Dart analyzer failed for the exact generated candidate: "
                    + concreteReason(cause), cause);
        } finally {
            boolean cancelUnregistered = false;
            synchronized (monitor) {
                if (registered && activeOperationId == operationId
                        && activeAnalysis == analysis) {
                    activeAnalysis = null;
                }
                cancelUnregistered = !registered;
            }
            if (cancelUnregistered) {
                analysis.cancel();
            }
        }
    }

    /**
     * Atomically chooses between close-time cancellation and pair admission.
     * No live apply or pair authority change may occur before this edge.
     */
    private void crossCommitBoundary(long operationId)
            throws IOException, InterruptedException {
        synchronized (monitor) {
            requireActiveOperationLocked(operationId);
            if (commitBoundaryCrossed) {
                throw new IOException(
                        "The Flutter Designer mutation crossed its commit boundary twice.");
            }
            commitBoundaryCrossed = true;
        }
        commitBoundaryHook.afterCrossing();
    }

    private void finishMutation(
            long operationId,
            DesignerCommandSessionOrchestrator owner,
            MutationResult result) {
        FlutterDesignerDocumentState stateToRefresh = null;
        DesignerCommandSessionOrchestrator ownerToClose = null;
        ChangeListener listenerToRemove = null;
        boolean allowHistoryOwnerTransfer = false;
        PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
        synchronized (monitor) {
            if (activeOperationId != operationId) {
                return;
            }
            boolean deferredClose = closed && closeAfterActiveCommit;
            activeOperationId = -1L;
            activeAnalysis = null;
            activeCompletion = null;
            mutationRunning = false;
            commitBoundaryCrossed = false;
            closeAfterActiveCommit = false;
            if (closed) {
                if (deferredClose) {
                    ownerToClose = sessionOwner;
                    listenerToRemove = sessionListener;
                    allowHistoryOwnerTransfer = !disposalRequested;
                    sessionOwner = null;
                    sessionListener = null;
                    boundRevision = null;
                    sessionCurrent = null;
                    readyCurrent = null;
                    analysisEnvironment = null;
                    readyPairEpoch = -1L;
                }
            } else {
                String pairUnavailable = pairMutationUnavailableReason(pairState);
                DesignerCommandRevision finalRevision = null;
                if (owner != null && owner == sessionOwner) {
                    try {
                        finalRevision = owner.currentRevision();
                    } catch (IllegalStateException closedOwner) {
                        stateToRefresh = documentController.state();
                    }
                }
                if (pairUnavailable != null) {
                    if (owner == sessionOwner && finalRevision != null) {
                        boundRevision = finalRevision;
                    }
                    readyPairEpoch = -1L;
                    publishLocked(presentationSnapshotLocked().blocked(
                            result.operation(), result.target(), pairUnavailable));
                } else if (owner != null
                        && sessionOwner == owner
                        && sessionCurrent != null
                        && finalRevision != null) {
                    boolean unusedCleanOwner = result.outcome() != Outcome.APPLIED
                            && !owner.dirty()
                            && !owner.canUndo()
                            && !owner.canRedo();
                    if (unusedCleanOwner) {
                        ownerToClose = owner;
                        listenerToRemove = sessionListener;
                        sessionOwner = null;
                        sessionListener = null;
                        boundRevision = null;
                        sessionCurrent = null;
                        readyCurrent = null;
                        analysisEnvironment = null;
                        readyPairEpoch = -1L;
                        stateToRefresh = documentController.state();
                    } else {
                        boundRevision = finalRevision;
                        readyCurrent = sessionCurrent;
                        readyPairEpoch = pairState.epoch();
                        publishLocked(Snapshot.ready(
                                this, sessionCurrent, finalRevision, readyPairEpoch));
                    }
                } else {
                    stateToRefresh = documentController.state();
                }
                if (documentController.state() != readyCurrent) {
                    stateToRefresh = documentController.state();
                }
            }
        }
        if (ownerToClose != null) {
            releaseControllerOwner(
                    ownerToClose,
                    listenerToRemove,
                    allowHistoryOwnerTransfer);
        }
        if (stateToRefresh != null) {
            scheduleRefresh(stateToRefresh);
        }
    }

    /**
     * Detaches one owner from the Properties controller without closing
     * semantic history which the pair coordinator still needs for Save or
     * Undo/Redo. The closed controller keeps the lifetime reference and the
     * pair-state listener until that exact history is finally released.
     */
    private void releaseControllerOwner(
            DesignerCommandSessionOrchestrator owner,
            ChangeListener listener,
            boolean allowHistoryTransfer) {
        if (listener != null) {
            owner.removeChangeListener(listener);
        }
        boolean transfer = allowHistoryTransfer
                && pairCoordinator.retainsHistoryOwner(owner);
        if (transfer) {
            synchronized (monitor) {
                transfer = closed
                        && !disposalRequested
                        && (detachedHistoryOwner == null
                                || detachedHistoryOwner == owner);
                if (transfer) {
                    detachedHistoryOwner = owner;
                }
                historyOwnerTransferPending = false;
            }
        } else {
            synchronized (monitor) {
                historyOwnerTransferPending = false;
            }
        }
        if (!transfer) {
            owner.close();
        }
        cleanupDetachedHistoryOwnerIfReleased();
        removePairListenerIfUnused();
    }

    /** Closes a transferred owner once no exact pair-history identity retains it. */
    private void cleanupDetachedHistoryOwnerIfReleased() {
        DesignerCommandSessionOrchestrator retained;
        boolean force;
        synchronized (monitor) {
            retained = detachedHistoryOwner;
            force = disposalRequested;
        }
        if (retained == null) {
            removePairListenerIfUnused();
            return;
        }
        if (!force && pairCoordinator.retainsHistoryOwner(retained)) {
            return;
        }
        synchronized (monitor) {
            if (detachedHistoryOwner != retained) {
                return;
            }
            // The coordinator query above observed the post-publication state.
            // A closed controller cannot create new history from this owner.
            detachedHistoryOwner = null;
        }
        retained.close();
        removePairListenerIfUnused();
    }

    private void removePairListenerIfUnused() {
        boolean remove;
        synchronized (monitor) {
            remove = pairListenerRegistered
                    && closed
                    && detachedHistoryOwner == null
                    && !historyOwnerTransferPending;
            if (remove) {
                pairListenerRegistered = false;
            }
        }
        if (remove) {
            pairCoordinator.removePropertyChangeListener(pairListener);
        }
    }

    private void pairStateChanged(PairSaveCoordinatorSnapshot pairState) {
        FlutterDesignerDocumentState documentState;
        synchronized (monitor) {
            if (closed) {
                documentState = null;
            } else if (mutationRunning) {
                return;
            } else {
                ++refreshGeneration;
                String unavailable = pairMutationUnavailableReason(pairState);
                if (unavailable != null) {
                    readyCurrent = null;
                    analysisEnvironment = null;
                    readyPairEpoch = -1L;
                    publishLocked(presentationSnapshotLocked().blocked(
                            "Prepare Flutter Designer Properties",
                            dataObject.getModelFile().getNameExt(),
                            unavailable));
                    return;
                }
                documentState = documentController.state();
            }
        }
        if (documentState == null) {
            cleanupDetachedHistoryOwnerIfReleased();
            return;
        }
        scheduleRefresh(documentState);
    }

    private void scheduleRefresh(FlutterDesignerDocumentState documentState) {
        Objects.requireNonNull(documentState, "documentState");
        long generation;
        synchronized (monitor) {
            if (closed) {
                return;
            }
            generation = ++refreshGeneration;
            if (mutationRunning) {
                return;
            }
            if (!(documentState instanceof FlutterDesignerDocumentState.Current current)) {
                readyCurrent = null;
                analysisEnvironment = null;
                readyPairEpoch = -1L;
                publishUnavailableDocumentStateLocked(documentState);
                return;
            }
            publishLocked(snapshot.waitingFor(
                    "Prepare Flutter Designer Properties",
                    dataObject.getModelFile().getNameExt(),
                    "Validating the exact model, source and SDK identities."));
            worker.post(() -> bindCurrent(generation, current));
        }
    }

    private void bindCurrent(
            long generation,
            FlutterDesignerDocumentState.Current current) {
        try {
            DesignerCommandSessionOrchestrator retainedOwner;
            synchronized (monitor) {
                if (!refreshStillCurrentLocked(generation, current)) {
                    return;
                }
                retainedOwner = sessionOwner;
            }
            if (retainedOwner != null
                    && !retainedOwner.retainsOpenHistoryAuthority()) {
                handleRetiredSessionOwner(retainedOwner);
                return;
            }
            PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
            String unavailable = currentUnavailableReason(current);
            if (unavailable == null) {
                unavailable = pairMutationUnavailableReason(pairState);
            }
            if (unavailable != null) {
                publishRefreshBlocked(generation, current, unavailable);
                return;
            }
            AnalysisEnvironment environment = resolveAnalysisEnvironment();

            DesignerCommandSessionOrchestrator previous = null;
            ChangeListener previousListener = null;
            synchronized (monitor) {
                if (!refreshStillCurrentLocked(generation, current)) {
                    return;
                }
                if (pairCoordinator.state().epoch() != pairState.epoch()) {
                    return;
                }
                DesignerCommandSessionOrchestrator observedOwner = sessionOwner;
                DesignerCommandRevision observedRevision = observedOwner == null
                        ? null : observedOwner.currentRevision();
                boolean observedDirty = observedOwner != null
                        && observedOwner.dirty();
                if (observedOwner != null
                        && exactRevisionMatchesCurrent(observedRevision, current)) {
                    boundRevision = observedRevision;
                    sessionCurrent = current;
                    readyCurrent = current;
                    analysisEnvironment = environment;
                    readyPairEpoch = pairState.epoch();
                    publishLocked(Snapshot.ready(
                            this, current, observedRevision, readyPairEpoch));
                    return;
                }
                if (observedOwner != null
                        && pairState.status() == PairSaveCoordinatorStatus.STAGED_PAIR
                        && pairCoordinator.ownsExactStagedRevision(
                                pairState.epoch(),
                                observedOwner,
                                observedRevision)) {
                    boundRevision = observedRevision;
                    sessionCurrent = current;
                    readyCurrent = current;
                    analysisEnvironment = environment;
                    readyPairEpoch = pairState.epoch();
                    publishLocked(Snapshot.ready(
                            this, current, observedRevision, readyPairEpoch));
                    return;
                }
                if (observedOwner != null
                        && pairState.status() == PairSaveCoordinatorStatus.CLEAN) {
                    readyCurrent = null;
                    analysisEnvironment = null;
                    readyPairEpoch = -1L;
                    publishLocked(presentationSnapshotLocked().waitingFor(
                            "Reload saved Flutter Designer pair",
                            dataObject.getModelFile().getNameExt(),
                            "Waiting for the exact saved Dart and .fd snapshots to reload."));
                    return;
                }
                if (observedOwner != null && observedDirty) {
                    readyCurrent = null;
                    analysisEnvironment = null;
                    readyPairEpoch = -1L;
                    boundRevision = observedRevision;
                    publishLocked(presentationSnapshotLocked().blocked(
                            "Rebind Flutter Designer Properties",
                            dataObject.getModelFile().getNameExt(),
                            "The loaded pair changed while unsaved Designer history is active."));
                    return;
                }
                previous = sessionOwner;
                previousListener = sessionListener;
                sessionOwner = null;
                sessionListener = null;
                boundRevision = null;
                sessionCurrent = null;
                readyCurrent = null;
                analysisEnvironment = null;
                readyPairEpoch = -1L;
            }
            if (previous != null) {
                if (previousListener != null) {
                    previous.removeChangeListener(previousListener);
                }
                previous.close();
            }

            synchronized (monitor) {
                if (!refreshStillCurrentLocked(generation, current)) {
                    return;
                }
                readyCurrent = current;
                analysisEnvironment = environment;
                readyPairEpoch = pairState.epoch();
                publishLocked(Snapshot.ready(
                        this, current, readyPairEpoch));
            }
        } catch (IOException | RuntimeException failure) {
            publishRefreshBlocked(
                    generation, current, concreteReason(failure));
        }
    }

    private DesignerCommandSessionOpenResult openSession(
            FlutterDesignerDocumentState.Current current) {
        var source = current.sourceIntegrity().orElseThrow();
        var threeWay = current.threeWayIntegrity().orElseThrow();
        byte[] dartBytes = source.original().orElseThrow().copyBytes();
        return DesignerCommandSession.openVerified(
                current.decoded().original(),
                dartBytes,
                current.catalog(),
                source,
                threeWay);
    }

    private AnalysisEnvironment resolveAnalysisEnvironment() throws IOException {
        FlutterToolchainStatus status = toolchains.resolve();
        if (!status.isReady() || !status.validForSave()) {
            throw new IOException(
                    "Flutter/Dart SDK integration is unavailable. Flutter: "
                    + status.flutterMessage() + " Dart: " + status.dartMessage());
        }
        File projectDirectory = FileUtil.toFile(
                dataObject.getProject().getProjectDirectory());
        if (projectDirectory == null) {
            throw new IOException(
                    "The Flutter project root is not a local filesystem path.");
        }
        Path projectRoot = projectDirectory.toPath().toRealPath();
        Path dartExecutable = status.dartSdk().orElseThrow()
                .dartExecutable().toRealPath();
        return new AnalysisEnvironment(projectRoot, dartExecutable);
    }

    private String currentUnavailableReason(
            FlutterDesignerDocumentState.Current current) {
        if (!dataObject.getPrimaryFile().canWrite()) {
            return "The paired Dart source is read-only: "
                    + dataObject.getPrimaryFile().getPath();
        }
        if (!dataObject.getModelFile().canWrite()) {
            return "The Flutter Designer model is read-only: "
                    + dataObject.getModelFile().getPath();
        }
        if (!current.validation().valid()) {
            return "The Flutter Designer widget tree is invalid: "
                    + current.validation().issues();
        }
        if (!current.contextIssues().isEmpty()) {
            return "The Flutter Designer pair context is invalid: "
                    + current.contextIssues().getFirst().message();
        }
        if (current.sourceIntegrity().isEmpty()) {
            return "The paired Dart source has no exact integrity evidence.";
        }
        if (current.threeWayIntegrity().isEmpty()
                || !current.threeWayIntegrity().orElseThrow()
                        .onDiskThreeWayMatch()) {
            return "The .fd model, generated Dart and on-disk Dart source do not match.";
        }
        return null;
    }

    private void sessionChanged(DesignerCommandSessionOrchestrator owner) {
        if (!owner.retainsOpenHistoryAuthority()) {
            handleRetiredSessionOwner(owner);
            return;
        }
        PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
        synchronized (monitor) {
            if (closed || sessionOwner != owner || sessionCurrent == null) {
                return;
            }
            DesignerCommandRevision revision;
            try {
                revision = owner.currentRevision();
            } catch (IllegalStateException closedOwner) {
                return;
            }
            boundRevision = revision;
            String pairUnavailable = pairMutationUnavailableReason(pairState);
            if (pairUnavailable != null) {
                readyCurrent = null;
                analysisEnvironment = null;
                readyPairEpoch = -1L;
                publishLocked(presentationSnapshotLocked().blocked(
                        "Update Flutter Designer presentation",
                        dataObject.getModelFile().getNameExt(),
                        pairUnavailable));
                return;
            }
            readyPairEpoch = pairState.epoch();
            if (snapshot.status() == Status.APPLYING) {
                publishLocked(Snapshot.applying(
                        this,
                        sessionCurrent,
                        revision,
                        readyPairEpoch,
                        snapshot.operation(),
                        snapshot.target(),
                        snapshot.message()));
            } else {
                readyCurrent = sessionCurrent;
                publishLocked(Snapshot.ready(
                        this, sessionCurrent, revision, readyPairEpoch));
            }
        }
    }

    private void handleRetiredSessionOwner(
            DesignerCommandSessionOrchestrator owner) {
        boolean releasable = pairCoordinator.releaseRetiredHistoryOwner(owner);
        ChangeListener listener = null;
        FlutterDesignerDocumentState stateToRefresh = null;
        synchronized (monitor) {
            if (closed || sessionOwner != owner) {
                return;
            }
            if (!releasable) {
                readyCurrent = null;
                analysisEnvironment = null;
                readyPairEpoch = -1L;
                publishLocked(presentationSnapshotLocked().blocked(
                        "Recover Flutter Designer Properties",
                        dataObject.getModelFile().getNameExt(),
                        "The semantic Designer owner closed while retained "
                        + "Undo/Redo history still references this form."));
                return;
            }
            listener = sessionListener;
            sessionOwner = null;
            sessionListener = null;
            boundRevision = null;
            sessionCurrent = null;
            readyCurrent = null;
            analysisEnvironment = null;
            readyPairEpoch = -1L;
            stateToRefresh = documentController.state();
        }
        if (listener != null) {
            owner.removeChangeListener(listener);
        }
        owner.close();
        scheduleRefresh(stateToRefresh);
    }

    private void publishUnavailableDocumentStateLocked(
            FlutterDesignerDocumentState documentState) {
        if (sessionOwner != null) {
            publishLocked(presentationSnapshotLocked().blocked(
                    "Reload Flutter Designer Properties",
                    dataObject.getModelFile().getNameExt(),
                    "The loaded model became unavailable while unsaved Designer history is active."));
            return;
        }
        String message = switch (documentState) {
            case FlutterDesignerDocumentState.Idle ignored ->
                "Open the Design view to load the Flutter Designer model.";
            case FlutterDesignerDocumentState.Loading ignored ->
                "Loading the Flutter Designer model.";
            case FlutterDesignerDocumentState.UnsupportedNewer ignored ->
                "The .fd file uses a newer unsupported schema and is read-only.";
            case FlutterDesignerDocumentState.Invalid invalid ->
                "The .fd file is invalid: "
                        + invalid.decoded().diagnostics().getFirst().message();
            case FlutterDesignerDocumentState.InputTooLarge tooLarge ->
                "The .fd file exceeds the " + tooLarge.maximumBytes()
                        + " byte safety limit.";
            case FlutterDesignerDocumentState.Failure failure ->
                failure.operation() + " failed for " + failure.target()
                        + ": " + failure.reason();
            case FlutterDesignerDocumentState.Current ignored ->
                throw new IllegalArgumentException(
                        "Current must be refreshed asynchronously");
        };
        Status status = documentState instanceof FlutterDesignerDocumentState.Idle
                || documentState instanceof FlutterDesignerDocumentState.Loading
                ? Status.WAITING : Status.BLOCKED;
        publishLocked(Snapshot.unavailable(
                status,
                "Prepare Flutter Designer Properties",
                dataObject.getModelFile().getNameExt(),
                message));
    }

    private void publishRefreshBlocked(
            long generation,
            FlutterDesignerDocumentState.Current current,
            String reason) {
        synchronized (monitor) {
            if (!refreshStillCurrentLocked(generation, current)) {
                return;
            }
            readyCurrent = null;
            analysisEnvironment = null;
            readyPairEpoch = -1L;
            String blockedReason = requireText(reason, "reason");
            Snapshot blocked = retainedUnsavedPresentationIsExactLocked(current)
                    ? presentationSnapshotLocked().blocked(
                            "Prepare Flutter Designer Properties",
                            dataObject.getModelFile().getNameExt(),
                            blockedReason)
                    : Snapshot.blocked(
                            current,
                            "Prepare Flutter Designer Properties",
                            dataObject.getModelFile().getNameExt(),
                            blockedReason);
            publishLocked(blocked);
        }
    }

    /**
     * Retains an unsaved presentation across an SDK/readiness failure only
     * while the pair coordinator still owns that exact staged endpoint.  A
     * clean or otherwise changed Current must be shown from its own validated
     * read-only model rather than from an older command revision.
     */
    private boolean retainedUnsavedPresentationIsExactLocked(
            FlutterDesignerDocumentState.Current current) {
        if (sessionOwner == null || boundRevision == null
                || sessionCurrent != current
                || exactRevisionMatchesCurrent(boundRevision, current)) {
            return false;
        }
        PairSaveCoordinatorSnapshot pairState = pairCoordinator.state();
        return pairState.status() == PairSaveCoordinatorStatus.STAGED_PAIR
                && pairCoordinator.ownsExactStagedRevision(
                        pairState.epoch(), sessionOwner, boundRevision);
    }

    private boolean refreshStillCurrentLocked(
            long generation,
            FlutterDesignerDocumentState.Current current) {
        return !closed
                && !mutationRunning
                && refreshGeneration == generation
                && documentController.state() == current;
    }

    private Snapshot presentationSnapshotLocked() {
        if (boundRevision != null && sessionCurrent != null) {
            return Snapshot.ready(
                    this,
                    sessionCurrent,
                    boundRevision,
                    Math.max(0L, readyPairEpoch));
        }
        return snapshot;
    }

    private void requireActiveOperationLocked(long operationId)
            throws IOException {
        if (closed || !mutationRunning || activeOperationId != operationId) {
            throw new IOException(
                    "The Flutter Designer mutation was cancelled before analysis started.");
        }
    }

    private void publishLocked(Snapshot next) {
        Snapshot previous = snapshot;
        if (previous.equals(next)) {
            return;
        }
        snapshot = next;
        Runnable publication = () -> changes.firePropertyChange(
                PROP_SNAPSHOT, previous, next);
        EventQueue.invokeLater(() -> {
            try {
                publication.run();
            } catch (RuntimeException | Error listenerFailure) {
                LOGGER.log(Level.WARNING,
                        "A Flutter Designer mutation presentation listener failed",
                        listenerFailure);
            }
        });
    }

    private static boolean exactRevisionMatchesCurrent(
            DesignerCommandRevision revision,
            FlutterDesignerDocumentState.Current current) {
        if (revision == null) {
            return false;
        }
        var source = current.sourceIntegrity();
        return source.isPresent()
                && source.orElseThrow().original().isPresent()
                && revision.document().equals(current.decoded().document())
                && Arrays.equals(
                        revision.fdBytes(),
                        current.decoded().original().copyBytes())
                && Arrays.equals(
                        revision.dartCandidateBytes(),
                        source.orElseThrow().original().orElseThrow().copyBytes());
    }

    private String pairMutationUnavailableReason(
            PairSaveCoordinatorSnapshot state) {
        if (state.status() == PairSaveCoordinatorStatus.DIRTY_SOURCE) {
            DesignerCommandSessionOrchestrator owner;
            DesignerCommandRevision revision;
            synchronized (monitor) {
                owner = sessionOwner;
                if (owner == null) {
                    revision = null;
                } else {
                    try {
                        revision = owner.currentRevision();
                    } catch (IllegalStateException closedOwner) {
                        revision = null;
                    }
                }
            }
            if (owner != null && revision != null
                    && pairCoordinator.ownsExactSemanticBaselineRevision(
                            state.epoch(), owner, revision)) {
                return null;
            }
        }
        return switch (state.status()) {
            case CLEAN, STAGED_PAIR -> null;
            case DIRTY_SOURCE ->
                "The Dart source contains changes outside the Designer; save or revert them first.";
            case PREPARING_PAIR, PREPARING_REPLACEMENT ->
                "Another Flutter Designer pair transition is still in progress.";
            case SAVING_SOURCE, SAVING_PAIR, SAVING_FD_ONLY ->
                "The Flutter Designer pair is currently being saved.";
            case EXTERNAL_CONFLICT, SAVE_FAILED, RECOVERY_CONFLICT ->
                state.reason().orElse(
                        "The Flutter Designer pair is in conflict.");
        };
    }

    private static MutationResult rejectedCommand(
            String operation,
            String target,
            DesignerCommandSessionResult result) {
        return MutationResult.rejected(
                operation, target, commandDiagnostics(result.diagnostics()));
    }

    private static MutationResult rejectedEvidence(
            String operation,
            String target,
            PairSaveEvidenceResult result) {
        String reason = result.diagnostics().stream()
                .map(diagnostic -> diagnostic.code() + " at "
                        + diagnostic.subject() + ": " + diagnostic.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("The exact Dart analyzer evidence was rejected.");
        return MutationResult.rejected(operation, target, reason);
    }

    private static String commandDiagnostics(
            List<DesignerCommandDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic.code()
                        + (diagnostic.path().isEmpty()
                                ? "" : " at " + diagnostic.path())
                        + ": " + diagnostic.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("The Flutter Designer command was rejected.");
    }

    private static String operationName(DesignerCommand command) {
        return switch (command.kind()) {
            case ADD_WIDGET -> "Add Flutter widget";
            case REMOVE_WIDGET -> "Remove Flutter widget";
            case MOVE_WIDGET -> "Move Flutter widget";
            case WRAP_WIDGET -> "Wrap Flutter widget";
            case SET_PROPERTY -> "Set Flutter property";
            case RESET_PROPERTY -> "Reset Flutter property";
        };
    }

    private static String commandTarget(DesignerCommand command) {
        return switch (command) {
            case SetProperty set -> set.widgetId() + "." + set.propertyName();
            case ResetProperty reset ->
                reset.widgetId() + "." + reset.propertyName();
            case AddWidget add -> add.destination().parentId() + "."
                    + add.destination().slotName() + "["
                    + add.destination().index() + "]";
            case RemoveWidget remove -> remove.widgetId().toString();
            case MoveWidget move -> move.widgetId() + " -> "
                    + move.destination().parentId() + "."
                    + move.destination().slotName() + "["
                    + move.destination().index() + "]";
            case WrapWidget wrap -> wrap.widgetId().toString();
        };
    }

    private static String concreteReason(Throwable failure) {
        if (failure == null) {
            return "Unknown failure.";
        }
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.strip();
    }

    /**
     * Finds a fatal JVM termination signal even when recovery wrapped or
     * suppressed it while unwinding a pair lease.
     */
    private static Error fatalError(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(
                new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        if (failure != null) {
            pending.addLast(failure);
        }
        while (!pending.isEmpty()) {
            Throwable cursor = pending.removeFirst();
            if (!visited.add(cursor)) {
                continue;
            }
            if (cursor instanceof VirtualMachineError fatal) {
                return fatal;
            }
            if (cursor instanceof ThreadDeath fatal) {
                return fatal;
            }
            Throwable cause = cursor.getCause();
            if (cause != null) {
                pending.addLast(cause);
            }
            for (Throwable suppressed : cursor.getSuppressed()) {
                if (suppressed != null) {
                    pending.addLast(suppressed);
                }
            }
        }
        return null;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static CompletableFuture<MutationResult> completed(
            MutationResult result) {
        return CompletableFuture.completedFuture(result);
    }

    private MutationResult completionResult(
            CompletableFuture<MutationResult> completion,
            MutationResult workerResult) {
        synchronized (monitor) {
            return forcedCompletion == completion
                    ? Objects.requireNonNull(forcedCompletionResult)
                    : workerResult;
        }
    }

    void setAnalyzerFactoryForTests(AnalyzerFactory replacement) {
        synchronized (monitor) {
            if (mutationRunning) {
                throw new IllegalStateException(
                        "Cannot replace the analyzer while a mutation is active");
            }
            analyzerFactory = replacement == null
                    ? AnalyzerFactory.production() : replacement;
        }
    }

    void setSessionAdmissionHookForTests(SessionAdmissionHook replacement) {
        synchronized (monitor) {
            if (mutationRunning) {
                throw new IllegalStateException(
                        "Cannot replace the session-admission hook while a mutation is active");
            }
            sessionAdmissionHook = replacement == null
                    ? () -> { } : replacement;
        }
    }

    void setCommitBoundaryHookForTests(CommitBoundaryHook replacement) {
        synchronized (monitor) {
            if (mutationRunning) {
                throw new IllegalStateException(
                        "Cannot replace the commit-boundary hook while a mutation is active");
            }
            commitBoundaryHook = replacement == null
                    ? () -> { } : replacement;
        }
    }

    @Override
    public void close() {
        closeInternal(false);
    }

    /** Final DataObject disposal releases any owner transferred to pair history. */
    void closeForDataObjectDisposal() {
        closeInternal(true);
    }

    private void closeInternal(boolean disposingDataObject) {
        DesignerCommandSessionOrchestrator owner = null;
        DesignerCommandSessionOrchestrator detachedToClose = null;
        ChangeListener listener = null;
        DartCandidateAnalysisOperation analysis = null;
        CompletableFuture<MutationResult> completion = null;
        MutationResult cancellation = null;
        boolean firstClose = false;
        synchronized (monitor) {
            if (disposingDataObject) {
                disposalRequested = true;
            }
            if (closed) {
                if (!disposingDataObject) {
                    return;
                }
                detachedToClose = detachedHistoryOwner;
                detachedHistoryOwner = null;
                historyOwnerTransferPending = false;
            } else {
                firstClose = true;
                closed = true;
                refreshGeneration++;
                boolean deferCommitCleanup = mutationRunning
                        && commitBoundaryCrossed
                        && activeOperationId >= 0L;
                readyCurrent = null;
                analysisEnvironment = null;
                readyPairEpoch = -1L;
                if (deferCommitCleanup) {
                    // Pair admission already owns the outcome. Do not report
                    // cancellation while native apply is still committing.
                    // A non-disposal close transfers an admitted owner to Pair
                    // history in finishMutation; disposal closes it there.
                    closeAfterActiveCommit = true;
                    historyOwnerTransferPending = !disposingDataObject;
                } else {
                    owner = sessionOwner;
                    listener = sessionListener;
                    analysis = activeAnalysis;
                    completion = activeCompletion;
                    if (completion != null) {
                        cancellation = MutationResult.cancelled(
                                snapshot.operation(),
                                snapshot.target(),
                                "The Flutter Designer form was closed while the mutation was running.");
                        forcedCompletion = completion;
                        forcedCompletionResult = cancellation;
                    }
                    sessionOwner = null;
                    sessionListener = null;
                    boundRevision = null;
                    sessionCurrent = null;
                    activeOperationId = -1L;
                    activeAnalysis = null;
                    activeCompletion = null;
                    mutationRunning = false;
                    commitBoundaryCrossed = false;
                    closeAfterActiveCommit = false;
                    historyOwnerTransferPending = owner != null
                            && !disposingDataObject;
                }
                publishLocked(snapshot.closed(
                        "Close Flutter Designer Properties",
                        dataObject.getModelFile().getNameExt(),
                        "The Flutter Designer mutation controller is closed."));
            }
        }
        if (firstClose) {
            documentController.removePropertyChangeListener(documentListener);
        }
        Error terminal = null;
        if (analysis != null) {
            try {
                analysis.cancel();
            } catch (RuntimeException | Error cancellationFailure) {
                LOGGER.log(Level.WARNING,
                        "Cannot cancel the closing Flutter Designer analyzer",
                        cancellationFailure);
                terminal = fatalError(cancellationFailure);
            }
        }
        if (completion != null && cancellation != null) {
            completion.complete(cancellation);
        }
        if (owner != null) {
            try {
                releaseControllerOwner(
                        owner, listener, !disposingDataObject);
            } catch (RuntimeException | Error closeFailure) {
                LOGGER.log(Level.WARNING,
                        "Cannot close the Flutter Designer command owner",
                        closeFailure);
                Error closeTerminal = fatalError(closeFailure);
                if (terminal == null) {
                    terminal = closeTerminal;
                } else if (closeTerminal != null && closeTerminal != terminal) {
                    terminal.addSuppressed(closeTerminal);
                }
            }
        }
        if (detachedToClose != null) {
            try {
                detachedToClose.close();
            } catch (RuntimeException | Error closeFailure) {
                LOGGER.log(Level.WARNING,
                        "Cannot close the detached Flutter Designer history owner",
                        closeFailure);
                Error closeTerminal = fatalError(closeFailure);
                if (terminal == null) {
                    terminal = closeTerminal;
                } else if (closeTerminal != null && closeTerminal != terminal) {
                    terminal.addSuppressed(closeTerminal);
                }
            }
        }
        cleanupDetachedHistoryOwnerIfReleased();
        removePairListenerIfUnused();
        if (terminal != null) {
            throw terminal;
        }
    }

    enum Status {
        WAITING,
        READY,
        APPLYING,
        BLOCKED,
        CLOSED
    }

    enum Outcome {
        APPLIED,
        REJECTED,
        FAILED,
        CANCELLED
    }

    /** Opaque object identity for one exact presentation epoch. */
    static final class RevisionToken {
        private final FlutterDesignerMutationController ownerIdentity;
        private final Object documentStateIdentity;
        private final Object commandRevisionIdentity;
        private final long pairEpoch;

        private RevisionToken(
                FlutterDesignerMutationController ownerIdentity,
                Object documentStateIdentity,
                Object commandRevisionIdentity,
                long pairEpoch) {
            this.ownerIdentity = Objects.requireNonNull(
                    ownerIdentity, "ownerIdentity");
            this.documentStateIdentity = Objects.requireNonNull(
                    documentStateIdentity, "documentStateIdentity");
            this.commandRevisionIdentity = commandRevisionIdentity;
            if (pairEpoch < 0) {
                throw new IllegalArgumentException("pairEpoch must not be negative");
            }
            this.pairEpoch = pairEpoch;
        }
    }

    /** Immutable view consumed later by Properties, widget tree and Canvas. */
    record Snapshot(
            Status status,
            Optional<DesignerDocument> document,
            Optional<WidgetCatalog> catalog,
            Optional<RevisionToken> token,
            String operation,
            String target,
            String message) {
        Snapshot {
            Objects.requireNonNull(status, "status");
            document = Objects.requireNonNull(document, "document");
            catalog = Objects.requireNonNull(catalog, "catalog");
            token = Objects.requireNonNull(token, "token");
            operation = requireText(operation, "operation");
            target = requireText(target, "target");
            message = requireText(message, "message");
            if (document.isPresent() != catalog.isPresent()) {
                throw new IllegalArgumentException(
                        "document and catalog must be published together");
            }
            if ((status == Status.READY || status == Status.APPLYING)
                    && (document.isEmpty()
                    || catalog.isEmpty()
                    || token.isEmpty())) {
                throw new IllegalArgumentException(
                        status + " requires an exact presentation and token");
            }
            if (status != Status.READY && status != Status.APPLYING
                    && token.isPresent()) {
                throw new IllegalArgumentException(
                        status + " cannot expose a writable revision token");
            }
        }

        static Snapshot waiting(String message) {
            return unavailable(
                    Status.WAITING,
                    "Prepare Flutter Designer Properties",
                    "the paired Flutter Designer form",
                    message);
        }

        static Snapshot unavailable(
                Status status,
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    status,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    operation,
                    target,
                    message);
        }

        static Snapshot blocked(
                FlutterDesignerDocumentState.Current current,
                String operation,
                String target,
                String message) {
            Objects.requireNonNull(current, "current");
            return new Snapshot(
                    Status.BLOCKED,
                    Optional.of(current.decoded().document()),
                    Optional.of(current.catalog()),
                    Optional.empty(),
                    operation,
                    target,
                    message);
        }

        static Snapshot ready(
                FlutterDesignerMutationController owner,
                FlutterDesignerDocumentState.Current current,
                long pairEpoch) {
            return new Snapshot(
                    Status.READY,
                    Optional.of(current.decoded().document()),
                    Optional.of(current.catalog()),
                    Optional.of(new RevisionToken(
                            owner, current, null, pairEpoch)),
                    "Edit Flutter Designer Properties",
                    "the selected Flutter widget",
                    "Flutter Designer properties are ready.");
        }

        static Snapshot ready(
                FlutterDesignerMutationController owner,
                FlutterDesignerDocumentState.Current current,
                DesignerCommandRevision revision,
                long pairEpoch) {
            return new Snapshot(
                    Status.READY,
                    Optional.of(revision.document()),
                    Optional.of(current.catalog()),
                    Optional.of(new RevisionToken(
                            owner, current, revision, pairEpoch)),
                    "Edit Flutter Designer Properties",
                    "the selected Flutter widget",
                    "Flutter Designer properties are ready.");
        }

        static Snapshot applying(
                FlutterDesignerMutationController owner,
                FlutterDesignerDocumentState.Current current,
                DesignerCommandRevision revision,
                long pairEpoch,
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    Status.APPLYING,
                    Optional.of(revision.document()),
                    Optional.of(current.catalog()),
                    Optional.of(new RevisionToken(
                            owner, current, revision, pairEpoch)),
                    operation,
                    target,
                    message);
        }

        Snapshot applying(
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    Status.APPLYING,
                    document,
                    catalog,
                    token,
                    operation,
                    target,
                    message);
        }

        Snapshot waitingFor(
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    Status.WAITING,
                    document,
                    catalog,
                    Optional.empty(),
                    operation,
                    target,
                    message);
        }

        Snapshot blocked(
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    Status.BLOCKED,
                    document,
                    catalog,
                    Optional.empty(),
                    operation,
                    target,
                    message);
        }

        Snapshot closed(
                String operation,
                String target,
                String message) {
            return new Snapshot(
                    Status.CLOSED,
                    document,
                    catalog,
                    Optional.empty(),
                    operation,
                    target,
                    message);
        }
    }

    record MutationResult(
            Outcome outcome,
            String operation,
            String target,
            String reason) {
        MutationResult {
            Objects.requireNonNull(outcome, "outcome");
            operation = requireText(operation, "operation");
            target = requireText(target, "target");
            reason = requireText(reason, "reason");
        }

        static MutationResult applied(String operation, String target) {
            return new MutationResult(
                    Outcome.APPLIED,
                    operation,
                    target,
                    "The exact analyzed Flutter Designer revision was applied.");
        }

        static MutationResult rejected(
                String operation, String target, String reason) {
            return new MutationResult(
                    Outcome.REJECTED,
                    operation,
                    target,
                    requireText(reason, "reason"));
        }

        static MutationResult failed(
                String operation, String target, String reason) {
            return new MutationResult(
                    Outcome.FAILED,
                    operation,
                    target,
                    reason);
        }

        static MutationResult cancelled(
                String operation, String target, String reason) {
            return new MutationResult(
                    Outcome.CANCELLED,
                    operation,
                    target,
                    reason);
        }
    }

    @FunctionalInterface
    interface AnalyzerFactory {
        DartCandidateAnalysisOperation analyze(
                Path dartExecutable,
                DartCandidateAnalysisRequest request);

        static AnalyzerFactory production() {
            return (dartExecutable, request) -> new DartCandidateAnalyzer(
                    dartExecutable,
                    line -> LOGGER.log(Level.FINEST,
                            "Flutter Designer analyzer: {0}", line))
                    .analyze(request);
        }
    }

    private record AnalysisEnvironment(
            Path projectRoot,
            Path dartExecutable) {
        AnalysisEnvironment {
            Objects.requireNonNull(projectRoot, "projectRoot");
            Objects.requireNonNull(dartExecutable, "dartExecutable");
        }
    }

    private record SessionAdmission(
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision expectedRevision) {
        SessionAdmission {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(expectedRevision, "expectedRevision");
        }
    }

    @FunctionalInterface
    interface SessionAdmissionHook {
        void beforeAdmission() throws InterruptedException;
    }

    @FunctionalInterface
    interface CommitBoundaryHook {
        void afterCrossing() throws InterruptedException;
    }
}
