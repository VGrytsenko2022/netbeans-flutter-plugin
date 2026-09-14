package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.plugin.designer.guard.GuardedPersistenceRejectionSink;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransaction;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionIssue;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionRequest;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionResult;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionStatus;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.StyledDocument;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;

/**
 * The single exact save owner for one Dart/{@code .fd} data object.
 *
 * <p>A visual mutation must first acquire a {@link PairPreparation}. The lease
 * binds the loaded model, exact prepared bytes and pre-apply live document,
 * blocks every save until analysis finishes, and restores the exact live
 * managed regions when it is abandoned. Both visual and ordinary Source saves
 * finally pass through {@link PairFileTransaction}; NetBeans atomic actions are
 * event grouping only and are never treated as rollback.</p>
 */
final class PairSaveCoordinator implements Node.Cookie,
        DesignerSemanticUndoableEdit.ReplayController {
    static final String PROP_STATE = "pairSaveState";

    private static final Logger LOGGER = Logger.getLogger(
            PairSaveCoordinator.class.getName());
    /** Independent from the much smaller Designer analysis/evidence bound. */
    static final int MAX_SOURCE_PERSISTENCE_BYTES = 64 * 1024 * 1024;
    static final int MAX_MODEL_PERSISTENCE_BYTES = 16 * 1024 * 1024;

    private final FlutterDesignerDataObject dataObject;
    private final FlutterDesignerEditorSupport editor;
    private final FlutterDesignerDocumentController controller;
    private final FileObject dartFile;
    private final FileObject designerFile;
    private final CookieSet cookies;
    private final PairFileTransaction transaction;
    private volatile PairTransaction pairTransaction;
    private final FdOnlyTransaction fdOnlyTransaction;
    private final SaveCookie pairSaveCookie = new PairSaveCookie();
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final Object effectsMonitor = new Object();
    private final ArrayDeque<EffectsPublication> effectsQueue = new ArrayDeque<>();
    private boolean effectsDraining;
    private int effectsDeferralDepth;

    private long epoch;
    private long externalEventEpoch;
    /** Monotonic authority for CES modified/unmodified transitions. */
    private long sourceStateEpoch;
    private PairPreparation preparation;
    private PairReplacement replacement;
    private SourceRestageClaim sourceRestage;
    private StagedPairAuthority staged;
    private final Map<HistoryEdgeKey, UnsavedPairHistoryEdge>
            unsavedPairHistory = new HashMap<>();
    private UnsavedPairHistoryCursor unsavedHistoryCursor;
    /** Owner retained even when NetBeans trims the last native semantic edit. */
    private DesignerCommandSessionOrchestrator unsavedHistoryOwner;
    private ActiveHistoryTransition historyTransition;
    private ArmedForwardAdmission forwardAdmission;
    private ActivePairSave activePairSave;
    private ActiveSourceSave activeSourceSave;
    private ActiveFdOnlySave activeFdOnlySave;
    private PairPathOperationLease activePairPathOperation;
    /** Exact worker-thread handoff for a post-close DataObject operation. */
    private final ThreadLocal<PairPathOperationLease> pairPathReplay =
            new ThreadLocal<>();
    /** Retained until all enclosing NetBeans atomic actions publish their events. */
    private FileSystem.AtomicAction pairPathOperationProvenance;
    private boolean sourceDirty;
    private boolean failedSavePending;
    private boolean suppressConflictSaveCookie;
    private DiskBaseline diskBaseline;
    private boolean cookieInstalled;
    private volatile HistoryPreparationHook historyPreparationHook =
            (lease, claim) -> { };
    private volatile ForwardAdmissionHook forwardAdmissionHook =
            (lease, replacement) -> { };
    private volatile SourceHistoryOwnerSelectionHook
            sourceHistoryOwnerSelectionHook = owner -> { };
    private PairSaveCoordinatorSnapshot state = snapshot(
            PairSaveCoordinatorStatus.CLEAN, 0, null);

    PairSaveCoordinator(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerEditorSupport editor,
            FlutterDesignerDocumentController controller,
            FileObject dartFile,
            FileObject designerFile,
            CookieSet cookies) {
        this(dataObject, editor, controller, dartFile, designerFile, cookies,
                new PairFileTransaction());
    }

    PairSaveCoordinator(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerEditorSupport editor,
            FlutterDesignerDocumentController controller,
            FileObject dartFile,
            FileObject designerFile,
            CookieSet cookies,
            PairFileTransaction transaction) {
        this(dataObject, editor, controller, dartFile, designerFile, cookies,
                transaction, transaction::commit);
    }

    PairSaveCoordinator(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerEditorSupport editor,
            FlutterDesignerDocumentController controller,
            FileObject dartFile,
            FileObject designerFile,
            CookieSet cookies,
            PairFileTransaction transaction,
            FdOnlyTransaction fdOnlyTransaction) {
        this.dataObject = Objects.requireNonNull(dataObject, "dataObject");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.dartFile = Objects.requireNonNull(dartFile, "dartFile");
        this.designerFile = Objects.requireNonNull(designerFile, "designerFile");
        this.cookies = Objects.requireNonNull(cookies, "cookies");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.pairTransaction = transaction::commit;
        this.fdOnlyTransaction = Objects.requireNonNull(
                fdOnlyTransaction, "fdOnlyTransaction");
    }

    /** Test-only package seam retaining the exact production object binding. */
    PairSaveCoordinator(
            PairSaveCoordinator binding,
            FdOnlyTransaction fdOnlyTransaction) {
        this(binding.dataObject,
                binding.editor,
                binding.controller,
                binding.dartFile,
                binding.designerFile,
                binding.cookies,
                binding.transaction,
                fdOnlyTransaction);
    }

    PairSaveCoordinatorSnapshot state() {
        synchronized (this) {
            return state;
        }
    }

    /**
     * Atomically samples the visible pair state and the stronger monotonic
     * revision used to reject external-event and source-state ABA races.
     */
    BindingRevision bindingRevision() {
        synchronized (this) {
            return new BindingRevision(
                    state,
                    new CloseRevision(
                            this, epoch, externalEventEpoch, sourceStateEpoch));
        }
    }

    /**
     * One atomic, monotonic pair-authority token for an asynchronous editor
     * close.  The document itself is deliberately sampled outside this
     * monitor: entering CES while holding the pair coordinator would invert
     * the established document/coordinator lock order.  A torn cross-domain
     * sample therefore fails stale on the next comparison through either the
     * document version or one of these monotonic epochs.
     */
    CloseRevision closeRevision() {
        synchronized (this) {
            return new CloseRevision(
                    this, epoch, externalEventEpoch, sourceStateEpoch);
        }
    }

    /** Caller must hold this coordinator's monitor. */
    private boolean ownsCloseRevisionLocked(CloseRevision expected) {
        return expected.coordinatorIdentity() == this
                && expected.stateEpoch() == epoch
                && expected.externalEventEpoch() == externalEventEpoch
                && expected.sourceStateEpoch() == sourceStateEpoch;
    }

    boolean canBeginPairDelete() {
        PairPathOperationLease replay = pairPathReplay.get();
        synchronized (this) {
            return (replay instanceof PairDeleteLease
                    && activePairPathOperation == replay
                    && replay.replayReadyLocked())
                    || pairPathOperationAvailableLocked();
        }
    }

    PairDeleteLease beginPairDelete() throws IOException {
        PairDeleteLease replay = replayLease(PairDeleteLease.class);
        if (replay != null) {
            return replay;
        }
        return beginPairPathOperation(new PairDeleteLease(), true);
    }

    PairDeleteLease reservePairDeleteForEditorClose() throws IOException {
        return reservePairPathOperation(new PairDeleteLease());
    }

    boolean canBeginPairRename() {
        PairPathOperationLease replay = pairPathReplay.get();
        synchronized (this) {
            return (replay instanceof PairRenameLease
                    && activePairPathOperation == replay
                    && replay.replayReadyLocked())
                    || pairPathOperationAvailableLocked();
        }
    }

    PairRenameLease beginPairRename() throws IOException {
        PairRenameLease replay = replayLease(PairRenameLease.class);
        if (replay != null) {
            return replay;
        }
        return beginPairPathOperation(new PairRenameLease(), true);
    }

    PairRenameLease reservePairRenameForEditorClose() throws IOException {
        return reservePairPathOperation(new PairRenameLease());
    }

    boolean canBeginPairMove() {
        PairPathOperationLease replay = pairPathReplay.get();
        synchronized (this) {
            return (replay instanceof PairMoveLease
                    && activePairPathOperation == replay
                    && replay.replayReadyLocked())
                    || pairPathOperationAvailableLocked();
        }
    }

    PairMoveLease beginPairMove() throws IOException {
        PairMoveLease replay = replayLease(PairMoveLease.class);
        if (replay != null) {
            return replay;
        }
        return beginPairPathOperation(new PairMoveLease(), true);
    }

    PairMoveLease reservePairMoveForEditorClose() throws IOException {
        return reservePairPathOperation(new PairMoveLease());
    }

    boolean canBeginPairCopy() {
        synchronized (this) {
            return pairPathOperationAvailableLocked();
        }
    }

    PairCopyLease beginPairCopy() throws IOException {
        return beginPairPathOperation(new PairCopyLease(), false);
    }

    <T> T replayPairPathOperation(
            PairPathOperationLease lease,
            PairPathReplay<T> replay) throws IOException {
        Objects.requireNonNull(lease, "lease");
        Objects.requireNonNull(replay, "replay");
        if (pairPathReplay.get() != null) {
            throw new IOException("a paired path-operation replay is already active");
        }
        synchronized (this) {
            if (activePairPathOperation != lease
                    || !lease.replayReadyLocked()) {
                throw new IOException(
                        "the paired " + lease.operation()
                        + " replay lease is no longer active");
            }
        }
        pairPathReplay.set(lease);
        try {
            return replay.run();
        } finally {
            pairPathReplay.remove();
        }
    }

    private <T extends PairPathOperationLease> T replayLease(Class<T> type)
            throws IOException {
        PairPathOperationLease lease = pairPathReplay.get();
        if (lease == null) {
            return null;
        }
        synchronized (this) {
            if (!type.isInstance(lease)
                    || activePairPathOperation != lease
                    || !lease.replayReadyLocked()) {
                throw new IOException(
                        "the active paired path-operation replay does not match "
                        + type.getSimpleName());
            }
            return type.cast(lease);
        }
    }

    private <T extends PairPathOperationLease> T beginPairPathOperation(
            T lease,
            boolean closeEditor)
            throws IOException {
        reservePairPathOperation(lease, false);

        try {
            // Destructive path operations close the clean CES view because it
            // otherwise observes staging as a read-only transition. Copy owns
            // only a stable source snapshot and deliberately keeps that view
            // open while the same lease blocks save/command admission.
            if (closeEditor
                    && !editor.closeForSynchronousPairPathOperation()) {
                throw new IOException(
                        "the clean Designer/Source editor refused to close, "
                        + "or an open dedicated shell requires its pair-aware "
                        + "post-close command");
            }
            synchronized (this) {
                if (activePairPathOperation != lease
                        || sourceDirty
                        || failedSavePending
                        || dataObject.isModified()
                        || editor.sourceModified()) {
                    throw new IOException(
                            "the paired editor state changed while "
                            + lease.operation()
                            + " was acquiring exclusive authority");
                }
            }
            return lease;
        } catch (IOException | RuntimeException failure) {
            lease.finish(false);
            if (failure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw failure;
        }
    }

    private <T extends PairPathOperationLease> T reservePairPathOperation(
            T lease) throws IOException {
        return reservePairPathOperation(lease, true);
    }

    private <T extends PairPathOperationLease> T reservePairPathOperation(
            T lease,
            boolean awaitingEditorClose) throws IOException {
        synchronized (this) {
            if (!pairPathOperationAvailableLocked()) {
                throw new IOException(
                        "Cannot " + lease.operation().toLowerCase(Locale.ROOT)
                        + " the Flutter Designer form while it has "
                        + "unsaved changes, a conflict, or another active pair operation");
            }
            lease.awaitingEditorClose(awaitingEditorClose);
            activePairPathOperation = lease;
            return lease;
        }
    }

    private boolean pairPathOperationAvailableLocked() {
        return activePairPathOperation == null
                && pairPathOperationStateCleanLocked();
    }

    private boolean pairPathOperationStateCleanLocked() {
        return sourceRestage == null && preparation == null
                && replacement == null
                && staged == null
                && historyTransition == null
                && forwardAdmission == null
                && activePairSave == null
                && activeSourceSave == null
                && activeFdOnlySave == null
                && !sourceDirty
                && !failedSavePending
                && !dataObject.isModified()
                && !editor.sourceModified()
                && !conflictStatusLocked();
    }

    /** Package-private exact staged identity for the internal command bridge/tests. */
    PairSaveEvidence stagedEvidence() {
        synchronized (this) {
            return staged == null ? null : staged.evidence();
        }
    }

    /**
     * Returns whether one presentation revision still owns the exact staged
     * logical and physical history endpoint published at {@code expectedEpoch}.
     *
     * <p>This identity query deliberately does not reconstruct authority from
     * serialized bytes.  A saved-history Undo can retain a valid staged pair
     * without retaining the original analyzer evidence, so the command owner,
     * revision, proof and live cursor must all be checked together while the
     * coordinator monitor is held.</p>
     */
    boolean ownsExactStagedRevision(
            long expectedEpoch,
            DesignerCommandSessionOrchestrator expectedOwner,
            DesignerCommandRevision expectedRevision) {
        Objects.requireNonNull(expectedOwner, "expectedOwner");
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        synchronized (this) {
            return epoch == expectedEpoch
                    && state.status() == PairSaveCoordinatorStatus.STAGED_PAIR
                    && preparation == null
                    && replacement == null
                    && historyTransition == null
                    && activePairSave == null
                    && activeSourceSave == null
                    && activeFdOnlySave == null
                    && staged != null
                    && staged.owner() == expectedOwner
                    && staged.revision() == expectedRevision
                    && unsavedHistoryOwner == expectedOwner
                    && unsavedHistoryCursor != null
                    && unsavedHistoryCursor.endpoint().revision()
                            == expectedRevision
                    && unsavedHistoryCursor.stagedProof() == staged.proof()
                    && staged.proof().liveCandidateIdentity().sameEvidence(
                            unsavedHistoryCursor.liveIdentity())
                    && sourceDirty;
        }
    }

    /**
     * Returns whether DIRTY_SOURCE is an exact semantic BASELINE cursor created
     * by a Designer command, rather than an arbitrary user edit in Dart Source.
     */
    boolean ownsExactSemanticBaselineRevision(
            long expectedEpoch,
            DesignerCommandSessionOrchestrator expectedOwner,
            DesignerCommandRevision expectedRevision) {
        Objects.requireNonNull(expectedOwner, "expectedOwner");
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        // A clean command owner plus DIRTY_SOURCE means an ordinary native
        // Source edit sits above the semantic cursor. It must never be
        // mistaken for a Designer-owned BASELINE command.
        if (!expectedOwner.dirty()) {
            return false;
        }
        long observedSourceStateEpoch;
        synchronized (this) {
            if (!ownsExactSemanticBaselineRevisionLocked(
                    expectedEpoch, expectedOwner, expectedRevision)) {
                return false;
            }
            observedSourceStateEpoch = sourceStateEpoch;
        }

        LiveDartDocumentSnapshot freshLive;
        try {
            // Never enter CES while holding the coordinator monitor.  The
            // second locked validation below rejects edits (including an
            // edit-then-revert) that race this read.
            freshLive = editor.liveSnapshot();
        } catch (IOException unavailableLive) {
            return false;
        }

        synchronized (this) {
            if (sourceStateEpoch != observedSourceStateEpoch
                    || editor.liveDocumentVersion(
                            freshLive.documentIdentity())
                        != freshLive.documentVersion()
                    || !ownsExactSemanticBaselineRevisionLocked(
                            expectedEpoch,
                            expectedOwner,
                            expectedRevision)) {
                return false;
            }
            HistoryEndpoint endpoint = unsavedHistoryCursor.endpoint();
            return unsavedHistoryCursor.liveIdentity()
                            .sameEvidence(freshLive)
                    && historyCursorMatchesLiveLocked(endpoint, freshLive);
        }
    }

    private boolean ownsExactSemanticBaselineRevisionLocked(
            long expectedEpoch,
            DesignerCommandSessionOrchestrator expectedOwner,
            DesignerCommandRevision expectedRevision) {
        HistoryEndpoint endpoint = unsavedHistoryCursor == null
                ? null : unsavedHistoryCursor.endpoint();
        return epoch == expectedEpoch
                && state.status() == PairSaveCoordinatorStatus.DIRTY_SOURCE
                && preparation == null
                && replacement == null
                && historyTransition == null
                && activePairSave == null
                && activeSourceSave == null
                && activeFdOnlySave == null
                && staged == null
                && sourceDirty
                && unsavedHistoryOwner == expectedOwner
                && unsavedHistoryCursor != null
                && exactSemanticBaselineEndpoint(endpoint, expectedRevision)
                && unsavedHistoryCursor.stagedProof() == null
                && expectedRevision.persistenceKind()
                    == DesignerRevisionPersistenceKind.BASELINE;
    }

    private static boolean exactSemanticBaselineEndpoint(
            HistoryEndpoint endpoint,
            DesignerCommandRevision revision) {
        if (endpoint instanceof MetadataHistoryEndpoint metadata) {
            return metadata.revision() == revision
                    && revision.persistenceKind() == DesignerRevisionPersistenceKind.FD_ONLY;
        }
        if (endpoint == null
                || endpoint.revision() != revision
                || revision.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE) {
            return false;
        }
        return endpoint instanceof BaselineHistoryEndpoint
                || endpoint
                    instanceof SourceOverlayBaselineHistoryEndpoint overlay
                    && Arrays.equals(
                            revision.dartCandidateBytes(),
                        overlay.dartBytes());
    }

    /**
     * Returns whether the coordinator still retains any exact history identity
     * owned by {@code expectedOwner}.
     */
    boolean retainsHistoryOwner(
            DesignerCommandSessionOrchestrator expectedOwner) {
        Objects.requireNonNull(expectedOwner, "expectedOwner");
        synchronized (this) {
            return staged != null && staged.owner() == expectedOwner
                    || unsavedHistoryOwner == expectedOwner
                    || unsavedPairHistory.values().stream()
                            .anyMatch(edge -> edge.owner() == expectedOwner);
        }
    }

    /**
     * Releases a retired command owner only when no staged or retained pair
     * history still depends on it.
     *
     * <p>The controller uses this after an owner reports that it can no longer
     * grant history authority.  Returning {@code false} is a fail-closed
     * result: the controller must keep the owner identity and expose the
     * retained-history conflict instead of silently discarding Undo/Redo.</p>
     */
    boolean releaseRetiredHistoryOwner(
            DesignerCommandSessionOrchestrator expectedOwner) {
        Objects.requireNonNull(expectedOwner, "expectedOwner");
        if (expectedOwner.retainsOpenHistoryAuthority()) {
            return false;
        }
        synchronized (this) {
            if (preparation != null
                    || replacement != null
                    || historyTransition != null
                    || forwardAdmission != null
                    || activePairSave != null
                    || activeSourceSave != null
                    || activeFdOnlySave != null) {
                return false;
            }
            if (staged != null && staged.owner() == expectedOwner) {
                return false;
            }
            if (unsavedPairHistory.values().stream()
                    .anyMatch(edge -> edge.owner() == expectedOwner)) {
                return false;
            }
            if (unsavedHistoryOwner == expectedOwner) {
                return retireClosedEdgeLessHistoryOwnerLocked(expectedOwner);
            }
            return true;
        }
    }

    /** Package-private immutable proof inventory used by exact integration tests. */
    StagedPairProofSnapshot stagedProofSnapshot() {
        try {
            refreshPairedVariantCursorFromLiveIfExact();
        } catch (IOException refreshFailure) {
            LOGGER.log(Level.FINE,
                    "Cannot refresh a retained physical Source-envelope proof",
                    refreshFailure);
        }
        synchronized (this) {
            return staged == null ? null : staged.snapshot();
        }
    }

    /**
     * Captures one exact staged command source without granting replacement
     * authority.  The returned token pins the logical owner and the physical
     * history endpoint independently; replacement revalidates both identities
     * after the pure command candidate has been derived.
     */
    StagedCommandSource captureStagedCommandSource() throws IOException {
        return captureStagedCommandSourceInternal(null);
    }

    /**
     * Captures the staged command source only when the pair still owns the
     * exact presentation epoch supplied by the mutation token.
     */
    StagedCommandSource captureStagedCommandSource(long expectedEpoch)
            throws IOException {
        if (expectedEpoch < 0) {
            throw new IllegalArgumentException(
                    "expectedEpoch must not be negative");
        }
        return captureStagedCommandSourceInternal(expectedEpoch);
    }

    private StagedCommandSource captureStagedCommandSourceInternal(
            Long expectedEpoch) throws IOException {
        refreshPairedVariantCursorFromLiveIfExact();
        long observedSourceStateEpoch;
        synchronized (this) {
            rejectConflictSaveLocked();
            if (expectedEpoch != null && epoch != expectedEpoch) {
                throw new StalePairEpochException(
                        "The selected Flutter Designer pair epoch changed before staged command capture");
            }
            if (sourceRestage != null || preparation != null || replacement != null
                    || historyTransition != null || activePairSave != null
                    || activeSourceSave != null || activeFdOnlySave != null
                    || staged == null || unsavedHistoryCursor == null
                    || unsavedHistoryOwner != staged.owner()
                    || unsavedHistoryCursor.endpoint().revision()
                        != staged.revision()
                    || unsavedHistoryCursor.stagedProof() != staged.proof()
                    || !staged.proof().liveCandidateIdentity().sameEvidence(
                            unsavedHistoryCursor.liveIdentity())) {
                throw new IOException(
                        "Cannot capture a Flutter Designer command source: the exact "
                        + "staged physical endpoint is unavailable or another pair operation is active");
            }
            observedSourceStateEpoch = sourceStateEpoch;
        }

        // Never enter CES while holding the coordinator monitor.  The epoch
        // below makes an edit (including edit-then-revert) between this read
        // and token publication observable rather than hash-equivalent.
        LiveDartDocumentSnapshot freshLive = editor.liveSnapshot();
        synchronized (this) {
            rejectConflictSaveLocked();
            if (expectedEpoch != null && epoch != expectedEpoch) {
                throw new StalePairEpochException(
                        "The selected Flutter Designer pair epoch changed during staged command capture");
            }
            if (sourceStateEpoch != observedSourceStateEpoch
                    || editor.liveDocumentVersion(
                            freshLive.documentIdentity())
                        != freshLive.documentVersion()
                    || sourceRestage != null || preparation != null || replacement != null
                    || historyTransition != null || activePairSave != null
                    || activeSourceSave != null || activeFdOnlySave != null
                    || staged == null || unsavedHistoryCursor == null
                    || unsavedHistoryOwner != staged.owner()
                    || unsavedHistoryCursor.endpoint().revision()
                        != staged.revision()
                    || unsavedHistoryCursor.stagedProof() != staged.proof()
                    || !unsavedHistoryCursor.liveIdentity()
                            .sameEvidence(freshLive)
                    || !staged.proof().liveCandidateIdentity()
                            .sameEvidence(freshLive)) {
                throw new IOException(
                        "Cannot capture a Flutter Designer command source: the exact "
                        + "live staged endpoint changed while it was being pinned");
            }
            return new StagedCommandSource(
                    this,
                    staged,
                    unsavedHistoryCursor,
                    externalEventEpoch,
                    epoch,
                    sourceStateEpoch);
        }
    }

    /** Stable semantic-history capability installed into every retained edge. */
    DesignerSemanticUndoableEdit.ReplayController
            unsavedPairHistoryReplayController() {
        return this;
    }

    /** Test-only pre-raw seam; production keeps the no-op hook. */
    void setHistoryPreparationHookForTests(HistoryPreparationHook hook) {
        historyPreparationHook = hook == null ? (lease, claim) -> { } : hook;
    }

    /** Test-only seam at the exact pre-seal ARMED forward-admission edge. */
    void setForwardAdmissionHookForTests(ForwardAdmissionHook hook) {
        forwardAdmissionHook = hook == null
                ? (lease, replacement) -> { } : hook;
    }

    /** Test-only seam after history-owner selection and before lease pinning. */
    void setSourceHistoryOwnerSelectionHookForTests(
            SourceHistoryOwnerSelectionHook hook) {
        sourceHistoryOwnerSelectionHook = hook == null ? owner -> { } : hook;
    }

    /** Package-private history inventory used only by exact integration tests. */
    int unsavedPairHistoryEdgeCount() {
        synchronized (this) {
            return unsavedPairHistory.size();
        }
    }

    /** Package-private deterministic transaction seam for Pair Save tests. */
    void setPairTransactionForTests(PairTransaction replacement) {
        synchronized (this) {
            if (activePairSave != null) {
                throw new IllegalStateException(
                        "Cannot replace the Pair Save transaction while Save is active");
            }
            pairTransaction = replacement == null
                    ? transaction::commit
                    : replacement;
        }
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    /** Reserves the pair pipeline before the first live-document mutation. */
    PairPreparation beginPairPreparation(
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        return beginPairPreparationInternal(
                null, null, expectedCurrent, commandLease, initialLive);
    }

    /**
     * Reserves the CLEAN pair pipeline only if it still owns the exact epoch
     * published with the writable presentation token.
     */
    PairPreparation beginPairPreparation(
            long expectedEpoch,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        if (expectedEpoch < 0) {
            throw new IllegalArgumentException(
                    "expectedEpoch must not be negative");
        }
        return beginPairPreparationInternal(
                expectedEpoch, null, expectedCurrent, commandLease, initialLive);
    }

    /**
     * Reserves the CLEAN pair pipeline only if every authority counter which
     * minted the writable presentation token is still exact. Unlike the
     * compatibility epoch overload, this also rejects clean external-event and
     * source-state ABA before the command lease is claimed.
     */
    PairPreparation beginPairPreparation(
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        return beginPairPreparationInternal(
                expectedRevision.stateEpoch(),
                expectedRevision,
                expectedCurrent,
                commandLease,
                initialLive);
    }

    private PairPreparation beginPairPreparationInternal(
            Long expectedEpoch,
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        Objects.requireNonNull(expectedCurrent, "expectedCurrent");
        Objects.requireNonNull(commandLease, "commandLease");
        Objects.requireNonNull(initialLive, "initialLive");
        Object commandClaim = new Object();
        PairPreparation lease = null;
        StateChange reservationChange = null;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                failedClaimEffects = null;
        IOException claimOrIdentityFailure = null;
        boolean commandClaimed = false;
        synchronized (this) {
            try {
                if (expectedRevision != null
                        && !ownsCloseRevisionLocked(expectedRevision)) {
                    throw new StalePairEpochException(
                            "The selected Flutter Designer pair epoch changed "
                            + "before command reservation, or its full pair "
                            + "authority revision became stale");
                }
                if (expectedEpoch != null && epoch != expectedEpoch) {
                    throw new StalePairEpochException(
                            "The selected Flutter Designer pair epoch changed before command reservation");
                }
                ensureStageableLocked();
                commandLease.claimForReplacement(commandClaim);
                commandClaimed = true;
                InitialPreparationInputs inputs = verifyInitialPreparationInputs(
                        expectedCurrent, commandLease, initialLive);
                lease = new PairPreparation(
                        expectedCurrent,
                        inputs.prepared(),
                        initialLive,
                        externalEventEpoch,
                        sourceDirty,
                        commandLease,
                        commandClaim,
                        inputs.predecessor(),
                        inputs.candidate());
                preparation = lease;
                reservationChange = transitionLocked(
                        PairSaveCoordinatorStatus.PREPARING_PAIR, null);
            } catch (IOException | RuntimeException | Error failure) {
                claimOrIdentityFailure = asIOException(failure);
                try {
                    failedClaimEffects = commandClaimed
                            ? commandLease.abortToExactPredecessorDeferredEffects(
                                    commandClaim)
                            : commandLease.abortDeferredEffects();
                } catch (RuntimeException abortFailure) {
                    claimOrIdentityFailure.addSuppressed(abortFailure);
                    if (commandClaimed) {
                        try {
                            failedClaimEffects = commandLease
                                    .invalidateClaimedDeferredEffects(
                                            commandClaim);
                        } catch (RuntimeException invalidationFailure) {
                            claimOrIdentityFailure.addSuppressed(
                                    invalidationFailure);
                        }
                    }
                }
            }
        }
        if (claimOrIdentityFailure != null) {
            if (failedClaimEffects != null) {
                publishEffects(null, failedClaimEffects);
            }
            throw claimOrIdentityFailure;
        }
        publishEffects(reservationChange);

        try {
            DiskBaseline observedDisk = readDiskBaseline();
            if (!Arrays.equals(
                        observedDisk.dartBytes(), lease.prepared.baselineDartBytes())
                    || !Arrays.equals(
                        observedDisk.fdBytes(), lease.prepared.baselineFdBytes())) {
                throw new IOException(
                        "Cannot prepare Flutter Designer pair: the exact Dart or "
                        + ".fd disk baseline changed after the command lease was claimed");
            }
            LiveDartDocumentSnapshot currentLive = editor.liveSnapshot();
            if (!initialLive.sameEvidence(currentLive)) {
                throw new IOException(
                        "Cannot prepare Flutter Designer pair: the live Dart document "
                        + "changed before the preparation lease was verified");
            }
            rejectForeignSaveCookie();
            synchronized (this) {
                requireActivePreparationLocked(lease);
                if (externalEventEpoch != lease.eventTicket
                        || controller.state() != expectedCurrent
                        || !commandLease.ownsExactActiveTransition()) {
                    throw new IOException(
                            "Cannot prepare Flutter Designer pair: file epoch, "
                            + "Current or pending B-to-C1 command changed during baseline verification");
                }
                diskBaseline = observedDisk;
            }
            return lease;
        } catch (IOException | RuntimeException | Error reservationFailure) {
            try {
                cancelPreparation(lease);
            } catch (IOException cleanupFailure) {
                reservationFailure.addSuppressed(cleanupFailure);
            }
            if (reservationFailure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw reservationFailure;
        }
    }

    /** Compatibility seam for a canonical freshly analyzed predecessor. */
    PairReplacement beginStagedReplacement(
            PairSaveEvidence expectedStaged,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease)
            throws IOException {
        Objects.requireNonNull(expectedStaged, "expectedStaged");
        StagedCommandSource source = captureStagedCommandSource();
        if (source.predecessorProof().analyzedEvidence() != expectedStaged) {
            throw new IOException(
                    "Only the exact currently staged analyzer evidence may be replaced");
        }
        return beginStagedReplacement(source, commandLease);
    }

    /**
     * Reserves replacement of one exact staged PAIRED physical endpoint by its
     * exact prospective PAIRED successor or by the exact durable BASELINE.
     * The durable disk baseline remains unchanged while the live predecessor
     * may be a retained noncanonical Source envelope.
     */
    PairReplacement beginStagedReplacement(
            StagedCommandSource expectedSource,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease)
            throws IOException {
        Objects.requireNonNull(expectedSource, "expectedSource");
        Objects.requireNonNull(commandLease, "commandLease");
        Object commandClaim = new Object();
        long reservationEventEpoch = -1L;
        StateChange reservationChange = null;
        PairReplacement lease = null;
        IOException claimOrIdentityFailure = null;
        boolean commandClaimed = false;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                failedClaimEffects = null;
        synchronized (this) {
            try {
                ensureReplaceableLocked(expectedSource);
                reservationEventEpoch = externalEventEpoch;
                commandLease.claimForReplacement(commandClaim);
                commandClaimed = true;
                // Keep the coordinator monitor while deriving the pure C1-to-C2
                // transition. This closes the last pre-reservation event gap;
                // the work performs no document or filesystem operation.
                ReplacementInputs inputs = verifyReplacementInputs(
                        expectedSource, commandLease);
                // Claiming the command may synchronously notify listeners.
                // Recheck the opaque source token after that callback window
                // so an already-dirty Source edit cannot reach PREPARING.
                ensureReplaceableLocked(expectedSource);
                lease = new PairReplacement(
                        expectedSource.predecessorProof(),
                        expectedSource.cursorIdentity(),
                        expectedSource.predecessorEndpoint(),
                        commandLease,
                        commandClaim,
                        inputs.predecessor(),
                        inputs.candidate(),
                        inputs.candidatePair(),
                        inputs.liveTransition(),
                        expectedSource.liveIdentity(),
                        reservationEventEpoch);
                replacement = lease;
                reservationChange = transitionLocked(
                        PairSaveCoordinatorStatus.PREPARING_REPLACEMENT, null);
            } catch (IOException | RuntimeException | Error failure) {
                claimOrIdentityFailure = asIOException(failure);
                try {
                    failedClaimEffects = commandClaimed
                            ? commandLease
                                    .abortToExactPredecessorDeferredEffects(
                                            commandClaim)
                            : commandLease.abortDeferredEffects();
                } catch (RuntimeException abortFailure) {
                    claimOrIdentityFailure.addSuppressed(abortFailure);
                }
            }
        }
        if (claimOrIdentityFailure != null) {
            if (failedClaimEffects != null) {
                publishEffects(null, failedClaimEffects);
            }
            throw claimOrIdentityFailure;
        }
        publishEffects(reservationChange);

        try {
            DiskBaseline observedDisk = readDiskBaseline();
            if (!Arrays.equals(
                        observedDisk.dartBytes(),
                        lease.predecessorProof.baselineDartBytes())
                    || !Arrays.equals(
                        observedDisk.fdBytes(),
                        lease.predecessorProof.baselineFdBytes())) {
                IOException mismatch = new IOException(
                        "Cannot replace the staged Flutter Designer revision: the "
                        + "exact durable Dart or .fd baseline changed");
                terminalizeKnownReplacementConflict(lease, mismatch);
                throw mismatch;
            }
            LiveDartDocumentSnapshot currentLive = editor.liveSnapshot();
            if (!lease.initialLive.sameEvidence(currentLive)) {
                throw new IOException(
                        "Cannot replace the staged Flutter Designer revision: the "
                        + "exact live C1 evidence changed during reservation");
            }
            try {
                rejectForeignSaveCookie();
            } catch (IOException foreignOwner) {
                terminalizeKnownReplacementConflict(lease, foreignOwner);
                throw foreignOwner;
            }

            synchronized (this) {
                requireActiveReplacementLocked(lease);
                if (externalEventEpoch != reservationEventEpoch
                        || !ownsStagedProofLocked(lease.predecessorProof)
                        || unsavedHistoryCursor
                            != lease.predecessorCursor
                        || controller.state()
                            != lease.predecessorProof.loadedCurrentIdentity()
                        || editor.liveDocumentVersion(
                                currentLive.documentIdentity())
                            != currentLive.documentVersion()
                        || !commandLease.ownsExactActiveTransition()) {
                    throw new IOException(
                            "Cannot replace the staged Flutter Designer revision: "
                            + "file epoch, Current, staged C1 or pending command "
                            + "changed during baseline verification");
                }
                diskBaseline = observedDisk;
            }
            return lease;
        } catch (IOException | RuntimeException | Error reservationFailure) {
            try {
                cancelReplacement(lease);
            } catch (IOException cleanupFailure) {
                reservationFailure.addSuppressed(cleanupFailure);
            }
            if (reservationFailure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw reservationFailure;
        }
    }

    /**
     * Verifies the exact READY-side authority immediately before an FD-only
     * command crosses its adoption boundary. The durable commit repeats this
     * check while reserving the coordinator, so an event between the two
     * checks also fails stale before disk I/O.
     */
    void requireFdOnlyAdoptionFence(
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent)
            throws IOException {
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        Objects.requireNonNull(expectedCurrent, "expectedCurrent");
        synchronized (this) {
            if (!ownsCloseRevisionLocked(expectedRevision)) {
                throw new StalePairEpochException(
                        "The selected Flutter Designer pair epoch changed "
                        + "before Designer-only command adoption, or its full "
                        + "pair authority revision became stale");
            }
            ensureStageableLocked();
            if (controller.state() != expectedCurrent) {
                throw new StalePairEpochException(
                        "The exact loaded Designer revision changed before "
                        + "Designer-only command adoption");
            }
        }
    }

    boolean retainsMetadataHistory(
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision revision) {
        synchronized (this) {
            return retainsMetadataHistoryLocked(owner, revision);
        }
    }

    private boolean retainsMetadataHistoryLocked(
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision revision) {
        return unsavedHistoryOwner == owner && staged == null
                && unsavedHistoryCursor != null
                && unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint metadata
                && metadata.revision() == revision
                && metadata.currentIdentity() == controller.state();
    }

    /** Exact retained unchanged-Source cursors which can accept native metadata edits. */
    boolean retainsUnchangedSourceHistory(DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision revision) {
        synchronized (this) {
            return retainsUnchangedSourceHistoryLocked(owner, revision);
        }
    }

    private boolean retainsUnchangedSourceHistoryLocked(DesignerCommandSessionOrchestrator owner,
            DesignerCommandRevision revision) {
        if (retainsMetadataHistoryLocked(owner, revision)) return true;
        return unsavedHistoryOwner == owner && staged == null && unsavedHistoryCursor != null
                && unsavedHistoryCursor.endpoint() instanceof BaselineHistoryEndpoint baseline
                && baseline.revision() == revision && baseline.currentIdentity() == controller.state()
                && revision.persistenceKind() == DesignerRevisionPersistenceKind.BASELINE;
    }

    /**
     * Extends an already retained metadata cursor without touching Source or
     * persisting implicitly. The validated command and exact durable/live
     * anchors, not a fabricated Dart transition, authorize this native edit.
     */
    void applyRetainedMetadataCommand(
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        DiskBaseline observed = readDiskBaseline();
        rejectForeignSaveCookie();
        Object claim = new Object();
        onEdt(() -> {
            boolean claimed = false;
            boolean adopted = false;
            try (EffectsDeferral effects = deferEffects()) {
                UnsavedPairHistoryCursor predecessorCursor;
                UnsavedPairHistoryEdge edge;
                UnsavedPairHistoryCursor targetCursor;
                ArmedForwardAdmission admission;
                synchronized (PairSaveCoordinator.this) {
                    ensureStageableLocked();
                    DesignerCommandRevision before = commandLease.predecessorRevision();
                    DesignerCommandRevision after = commandLease.candidateRevision();
                    if (!ownsCloseRevisionLocked(expectedRevision)
                            || controller.state() != expectedCurrent
                            || !retainsUnchangedSourceHistoryLocked(commandLease.owner(), before)
                            || commandLease.catalogIdentity() != expectedCurrent.catalog()
                            || commandLease.kind() != DesignerCommandSessionOrchestrator.PendingTransitionKind.APPLY
                            || !commandLease.ownsExactActiveTransition()
                            || sourceDirty || editor.sourceModified() || failedSavePending
                            || diskBaseline == null
                            || !Arrays.equals(observed.dartBytes(), diskBaseline.dartBytes())
                            || !Arrays.equals(observed.fdBytes(), diskBaseline.fdBytes())
                            || !Arrays.equals(initialLive.markerBearingUtf8(), observed.dartBytes())
                            || !initialLive.sameEvidence(unsavedHistoryCursor.liveIdentity())
                            || !Arrays.equals(after.dartCandidateBytes(), initialLive.markerBearingUtf8())
                            || !(after.persistenceKind() == DesignerRevisionPersistenceKind.FD_ONLY
                                || after.persistenceKind() == DesignerRevisionPersistenceKind.BASELINE)) {
                        throw new IOException("Cannot extend retained metadata: the exact command, source or durable anchor changed");
                    }
                    predecessorCursor = unsavedHistoryCursor;
                    HistoryEndpoint target = after.persistenceKind() == DesignerRevisionPersistenceKind.BASELINE
                            ? new BaselineHistoryEndpoint(after, expectedCurrent,
                                    observed.dartBytes(), observed.fdBytes())
                            : new MetadataHistoryEndpoint(after, expectedCurrent,
                                    observed.dartBytes(), observed.fdBytes(), after,
                                    new ValidatedMetadataHistorySeed(after));
                    edge = new UnsavedPairHistoryEdge(predecessorCursor.endpoint(), target, commandLease.owner());
                    requireRegisterableHistoryEdgeLocked(edge);
                    verifyReplacementPhysicalBudgetLocked(commandLease, null);
                    targetCursor = new UnsavedPairHistoryCursor(target, initialLive, (StagedPairProof) null);
                    commandLease.claimForTransition(claim);
                    claimed = true;
                    admission = armForwardAdmissionLocked(claim, true);
                }
                forwardAdmissionHook.afterArmed(commandLease, true);
                editor.applyUnchangedSourceAndFinalize(initialLive,
                        initialLive.markerBearingUtf8(), forwardSemanticEdge(commandLease), fresh -> {
                    synchronized (PairSaveCoordinator.this) {
                        if (forwardAdmission != admission
                                || admission.poisoned || externalEventEpoch != admission.eventTicket
                                || epoch != admission.coordinatorEpoch
                                || controller.state() != expectedCurrent
                                || unsavedHistoryCursor != predecessorCursor
                                || !initialLive.sameEvidence(fresh)
                                || sourceDirty || editor.sourceModified()
                                || !commandLease.ownsExactActiveTransition()) {
                            throw new IOException("The retained metadata authority changed before native admission");
                        }
                    }
                    return () -> {
                        StateChange[] change = new StateChange[1];
                        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
                        synchronized (PairSaveCoordinator.this) {
                            commandEffects = commandLease.adoptExactTargetCloseAwareDeferredEffects(
                                    commandLease.candidateRevision(), ownerClosePending -> {
                                boolean exact = forwardAdmission == admission
                                        && epoch == admission.coordinatorEpoch
                                        && unsavedHistoryCursor == predecessorCursor
                                        && controller.state() == expectedCurrent;
                                boolean poisoned = admission.poisoned || externalEventEpoch != admission.eventTicket;
                                forwardAdmission = null;
                                if (exact && !poisoned && !ownerClosePending) {
                                    unsavedPairHistory.put(edge.key(), edge);
                                    unsavedHistoryCursor = targetCursor;
                                    unsavedHistoryOwner = edge.owner();
                                    // CLEAN describes Source. Pending FD metadata is
                                    // represented independently by the shared SaveCookie.
                                    change[0] = transitionLocked(PairSaveCoordinatorStatus.CLEAN, null);
                                } else {
                                    failClosedForwardAdmissionLocked(admission, poisoned,
                                            "Retained metadata authority changed during native admission", change);
                                }
                            }, claim);
                        }
                        return deferredForwardPublication(change[0], commandEffects, effects);
                    };
                });
                adopted = true;
            } finally {
                StateChange release = disarmForwardAdmission(claim);
                if (claimed && !adopted && commandLease.ownsExactActiveTransition()) {
                    publishEffects(release, commandLease.abortToExactPredecessorDeferredEffects(claim));
                } else {
                    publishEffects(release);
                }
            }
            return null;
        });
    }

    /** Compatibility seam for direct coordinator callers without a UI token. */
    void commitFdOnly(
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        commitFdOnlyInternal(null, expectedCurrent, lease);
    }

    /**
     * Commits one identity-bound Designer revision which changes only the
     * exact {@code .fd} bytes and still owns the READY-side pair authority.
     * This is an internal synchronous persistence boundary; it deliberately
     * does not invoke the NetBeans editor serializer, source analyzer,
     * live-document apply or source Undo/Redo.
     */
    void commitFdOnly(
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        commitFdOnlyInternal(expectedRevision, expectedCurrent, lease);
    }

    private void commitFdOnlyInternal(
            CloseRevision expectedRevision,
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        Objects.requireNonNull(expectedCurrent, "expectedCurrent");
        Objects.requireNonNull(lease, "lease");

        FdOnlyEvidence evidence;
        try {
            if (expectedRevision != null) {
                synchronized (this) {
                    if (!ownsCloseRevisionLocked(expectedRevision)) {
                        throw new StalePairEpochException(
                                "The selected Flutter Designer pair epoch changed "
                                + "before Designer-only save preflight, or its full "
                                + "pair authority revision became stale");
                    }
                }
            }
            evidence = verifyFdOnlyEvidence(expectedCurrent, lease);
            rejectForeignSaveCookie();
        } catch (IOException | RuntimeException failure) {
            abortFdOnlyLeaseAfterPreflightFailure(lease, failure);
            throw asIOException(failure);
        }

        ActiveFdOnlySave attempt;
        StateChange change;
        try {
            synchronized (this) {
                if (expectedRevision != null
                        && !ownsCloseRevisionLocked(expectedRevision)) {
                    throw new StalePairEpochException(
                            "The selected Flutter Designer pair epoch changed "
                            + "before Designer-only save reservation, or its full "
                            + "pair authority revision became stale");
                }
                ensureStageableLocked();
                if (sourceDirty || failedSavePending || editor.sourceModified()) {
                    throw new IOException(
                            "Cannot save the Designer model only: the Dart source "
                            + "has a pending or failed live edit");
                }
                if (controller.state() != expectedCurrent) {
                    throw new IOException(
                            "Cannot save the Designer model only: the exact loaded "
                            + "model revision changed before reservation");
                }
                if (!lease.ownsExactActiveRevision()) {
                    throw new IOException(
                            "Cannot save the Designer model only: the exact "
                            + "durable lease is no longer active");
                }
                attempt = new ActiveFdOnlySave(
                        expectedCurrent, lease, evidence, externalEventEpoch);
                activeFdOnlySave = attempt;
                suppressConflictSaveCookie = false;
                change = transitionLocked(
                        PairSaveCoordinatorStatus.SAVING_FD_ONLY, null);
            }
        } catch (IOException | RuntimeException failure) {
            abortFdOnlyLeaseAfterPreflightFailure(lease, failure);
            throw asIOException(failure);
        }
        publishEffects(change);

        try {
            LiveDartDocumentSnapshot reservedLive = onEdt(editor::liveSnapshot);
            synchronized (this) {
                requireActiveFdOnlySaveLocked(attempt);
                if (externalEventEpoch != attempt.eventTicket
                        || controller.state() != expectedCurrent
                        || sourceDirty || editor.sourceModified()
                        || attempt.liveMutationObserved
                        || !evidence.liveIdentity.sameEvidence(reservedLive)) {
                    throw new IOException(
                            "Cannot save the Designer model only: exact source/model "
                            + "evidence changed after reservation");
                }
            }
        } catch (IOException | RuntimeException failure) {
            finishFdOnlyBeforeWriteFailure(attempt, failure);
            throw asIOException(failure);
        }

        PairFileTransactionResult result;
        try {
            result = fdOnlyTransaction.commit(new PairFileTransactionRequest(
                    dartFile,
                    designerFile,
                    evidence.durableDart,
                    evidence.durableFd,
                    evidence.durableDart,
                    evidence.candidateFd));
            synchronized (this) {
                requireActiveFdOnlySaveLocked(attempt);
                attempt.result = result;
            }
        } catch (IOException failure) {
            finishUncertainFdOnlyFailure(attempt, failure);
            throw failure;
        } catch (RuntimeException failure) {
            IOException wrapped = new IOException(
                    "The Designer-only pair transaction failed without a "
                    + "verifiable outcome: " + reason(failure), failure);
            finishUncertainFdOnlyFailure(attempt, wrapped);
            throw wrapped;
        }

        if (acceptsFdOnlyTransaction(result)) {
            finishCommittedFdOnly(attempt);
            return;
        }

        IOException failure = new IOException(
                "Designer-only Save did not commit exactly one .fd write: "
                + (result == null
                        ? "no transaction result"
                        : result.status() + " - " + transactionReason(result)));
        finishRejectedFdOnly(attempt, result, failure);
        throw failure;
    }

    /** Revalidates a manually edited Source overlay without applying another editor edit. */
    private void restageObservedSourceBeforeSave() throws IOException {
        StagedPairAuthority expected;
        UnsavedPairHistoryCursor cursor;
        long expectedEpoch;
        long eventEpoch;
        synchronized (this) {
            // CES confirmation reentry belongs to the already pinned serializer.
            if (activePairSave != null || activeSourceSave != null) return;
            if (sourceRestage != null) throw new IOException("Save is already analyzing this form's Source changes.");
            if (staged == null) return;
            rejectConflictSaveLocked();
            if (preparation != null || replacement != null || historyTransition != null
                    || activeFdOnlySave != null || activePairPathOperation != null) {
                throw new IOException("Cannot analyze Source while another Designer operation owns the form.");
            }
            expected = staged;
            cursor = unsavedHistoryCursor;
            expectedEpoch = epoch;
            eventEpoch = externalEventEpoch;
        }
        LiveDartDocumentSnapshot observed = editor.liveSnapshot();
        if (expected.proof().liveCandidateIdentity().sameEvidence(observed)) return;
        if (cursor == null || cursor.endpoint().revision() != expected.revision()
                || !expected.proof().liveCandidateIdentity().sameManagedContent(observed)) {
            throw new IOException("Cannot save Source changes: the document identity or generated sections changed. No files were written.");
        }
        DesignerSourceRestageWork.run(() -> {
            restageObservedSource(expected, cursor, observed, expectedEpoch, eventEpoch);
            return null;
        });
    }

    private void restageObservedSource(StagedPairAuthority expected,
            UnsavedPairHistoryCursor cursor, LiveDartDocumentSnapshot observed,
            long expectedEpoch, long eventEpoch) throws IOException {
        var analyzer = dataObject.mutationController().sourceAnalyzerContext();
        try (var lease = expected.owner().beginSourceRestage(expected.revision(),
                expected.proof().preparedPairIdentity(), observed.markerBearingUtf8())) {
            SourceRestageClaim claim = new SourceRestageClaim(expected, cursor, observed, eventEpoch);
            StateChange started;
            synchronized (this) {
                if (staged != expected || unsavedHistoryCursor != cursor || epoch != expectedEpoch
                        || externalEventEpoch != eventEpoch || sourceRestage != null
                        || preparation != null || replacement != null || historyTransition != null
                        || activePairSave != null || activeSourceSave != null || activeFdOnlySave != null
                        || activePairPathOperation != null || !lease.ownsExactActiveRevision()
                        || controller.state() != expected.proof().loadedCurrentIdentity()) {
                    throw new IOException("Cannot analyze Source: the staged form changed before admission.");
                }
                started = transitionLocked(PairSaveCoordinatorStatus.PREPARING_REPLACEMENT,
                        null);
                sourceRestage = claim;
                claim.coordinatorEpoch = epoch;
            }
            publishEffects(started);
            try {
                requireSourceRestageCurrent(claim, lease);
                var ticket = PairSaveEvidenceGate.prepareAnalysis(expected.proof().loadedCurrentIdentity(),
                        lease.preparedPair(), analyzer.projectRoot(), FileUtil.toFile(dartFile).toPath(),
                        dev.flutter.netbeans.dart.DartCandidateWarningPolicy.ALLOW, trustedFlutterSdkRoot());
                claim.analysis = analyzer.analyze(ticket);
                dev.flutter.netbeans.dart.DartCandidateAnalysisResult result;
                try {
                    var pending = claim.analysis.result().toCompletableFuture();
                    // A stalled analyzer must not retain the owner after Source,
                    // external files, or the owning DataObject has changed.
                    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(2);
                    while (true) {
                        requireSourceRestageCurrent(claim, lease);
                        try {
                            result = pending.get(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                            break;
                        } catch (java.util.concurrent.TimeoutException waiting) {
                            if (System.nanoTime() - deadline >= 0) {
                                throw new IOException("Source analysis before Save timed out. No files were written.", waiting);
                            }
                        }
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Source analysis before Save was interrupted. No files were written.", interrupted);
                } catch (java.util.concurrent.ExecutionException failed) {
                    throw new IOException("Source analysis before Save failed. No files were written.", failed.getCause());
                } catch (java.util.concurrent.CancellationException cancelled) {
                    throw new IOException("Source analysis before Save was cancelled. No files were written.", cancelled);
                }
                PairAnalyzedCandidateResult evaluated = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
                if (!(evaluated instanceof PairAnalyzedCandidateResult.Ready ready)) {
                    throw new IOException("Cannot save current Source: " + evaluated.diagnostics().getFirst().message());
                }
                DesignerCommandSessionOrchestrator.DeferredLeaseEffects effects = onEdt(() -> {
                    requireSourceRestageCurrent(claim, lease);
                    PairSaveEvidenceResult bound = PairSaveEvidenceGate.bindApplied(ready.analyzed(), observed);
                    if (!(bound instanceof PairSaveEvidenceResult.Ready accepted)) {
                        throw new IOException("Cannot bind current Source analysis: " + bound.diagnostics().getFirst().message());
                    }
                    PairSaveEvidence evidence = accepted.evidence();
                    var proof = new AnalyzedStagedPairProof(evidence);
                    var authority = new StagedPairAuthority(proof, expected.owner(), expected.revision(), lease);
                    var endpoint = new ReanchoredPairedHistoryEndpoint(expected.revision(),
                            expected.proof().loadedCurrentIdentity(), evidence.baselineDartBytes(),
                            evidence.baselineFdBytes(), evidence.preparedPairIdentity(),
                            new ReanchoredAnalyzedHistorySeed(evidence));
                    var nextCursor = new UnsavedPairHistoryCursor(endpoint, observed, proof);
                    var committedEffects = new java.util.concurrent.atomic.AtomicReference<DesignerCommandSessionOrchestrator.DeferredLeaseEffects>();
                    editor.verifyAndFinalizePreparation(observed, false, () -> {
                      synchronized (PairSaveCoordinator.this) {
                        try {
                        requireSourceRestageAuthorityLocked(claim, lease);
                        RetainedPhysicalEndpointBudget budget = new RetainedPhysicalEndpointBudget(lease.maxRetainedPairBytes());
                        for (var edge : unsavedPairHistory.values()) {
                            budget.retain(edge.before());
                            budget.retain(edge.after());
                        }
                        budget.retain(cursor.endpoint());
                        budget.retain(endpoint);
                        committedEffects.set(lease.adoptAnalyzedDeferredEffects(evidence, () -> {
                            staged = authority;
                            unsavedHistoryCursor = nextCursor;
                            sourceRestage = null;
                            claim.completed = transitionLocked(PairSaveCoordinatorStatus.STAGED_PAIR,
                                    null);
                        }));
                        } catch (IOException failure) {
                            throw new java.io.UncheckedIOException(failure);
                        }
                      }
                    });
                    return Objects.requireNonNull(committedEffects.get());
                });
                effects.publish();
                publishEffects(claim.completed);
            } finally {
                if (claim.analysis != null) {
                    try {
                        claim.analysis.cancel();
                    } catch (RuntimeException cancellationFailure) {
                        LOGGER.log(Level.WARNING, "Cannot cancel completed Source analysis", cancellationFailure);
                    }
                }
                StateChange aborted = null;
                synchronized (this) {
                    if (sourceRestage == claim) {
                        sourceRestage = null;
                        if (externalEventEpoch == claim.eventEpoch && staged == expected
                                && state.status() == PairSaveCoordinatorStatus.PREPARING_REPLACEMENT) {
                            aborted = transitionLocked(PairSaveCoordinatorStatus.STAGED_PAIR,
                                    null);
                        }
                    }
                }
                if (aborted != null) publishEffects(aborted);
            }
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new IOException("Cannot prepare current Source changes for Save: " + reason(failure), failure);
        }
    }

    private void requireSourceRestageCurrent(SourceRestageClaim claim,
            DesignerCommandSessionOrchestrator.SourceRestageLease lease) throws IOException {
        LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
        synchronized (this) {
            requireSourceRestageAuthorityLocked(claim, lease);
            if (!claim.observed.sameEvidence(fresh)) {
                throw new IOException("Source changed while Save was analyzing it. Retry Save; no files were written.");
            }
        }
    }

    private void requireSourceRestageAuthorityLocked(SourceRestageClaim claim,
            DesignerCommandSessionOrchestrator.SourceRestageLease lease) throws IOException {
        if (sourceRestage != claim || staged != claim.authority || unsavedHistoryCursor != claim.cursor
                || epoch != claim.coordinatorEpoch || externalEventEpoch != claim.eventEpoch
                || controller.state() != claim.authority.proof().loadedCurrentIdentity()
                || !lease.ownsExactActiveRevision()
                || !claim.authority.owner().retainsOpenHistoryAuthority()) {
            throw new IOException("The form or Source analysis authority changed. No files were written.");
        }
    }

    private static final class SourceRestageClaim {
        final StagedPairAuthority authority;
        final UnsavedPairHistoryCursor cursor;
        final LiveDartDocumentSnapshot observed;
        final long eventEpoch;
        long coordinatorEpoch;
        volatile dev.flutter.netbeans.dart.DartCandidateAnalysisOperation analysis;
        StateChange completed;

        SourceRestageClaim(StagedPairAuthority authority, UnsavedPairHistoryCursor cursor,
                LiveDartDocumentSnapshot observed, long eventEpoch) {
            this.authority = authority;
            this.cursor = cursor;
            this.observed = observed;
            this.eventEpoch = eventEpoch;
        }
    }

    /** Called by the only SaveCookie and every direct editor save entry point. */
    void save() throws IOException {
        // Revalidate ownership at the entry edge; a foreign cookie must never
        // be allowed to serialize through this coordinator's exact streams.
        rejectForeignSaveCookie();
        // Native Source Undo/Redo may move between two physical envelopes of
        // one already-dirty semantic revision without publishing another CES
        // modified edge. Rebind that exact retained endpoint before pinning
        // Save authority.
        refreshPairedVariantCursorFromLiveIfExact();
        restageObservedSourceBeforeSave();
        ActiveSourceSave sourceAttempt = null;
        DesignerCommandSessionOrchestrator sourceHistoryOwner = null;
        boolean semanticBaselineHistory = false;
        StagedPairAuthority pairAuthority = null;
        long pairStageEpoch = -1;
        boolean confirmationReentry = false;
        StateChange change = null;

        synchronized (this) {
            if (activePairPathOperation != null) {
                throw new IOException(
                        "Cannot save " + dartFile.getNameExt()
                        + ": paired " + activePairPathOperation.operation()
                        + " currently owns the form");
            }
            Thread currentThread = Thread.currentThread();
            if (activeFdOnlySave != null) {
                throw new IOException("Cannot save " + dartFile.getNameExt()
                        + ": a synchronous Designer-only save is already running");
            }
            if (historyTransition != null || forwardAdmission != null) {
                throw new IOException("Cannot save " + dartFile.getNameExt()
                        + ": an exact unsaved Designer history transition is active");
            }
            if (activeSourceSave != null || activePairSave != null) {
                Thread owner = activeSourceSave != null
                        ? activeSourceSave.owner : activePairSave.owner;
                if (owner == currentThread) {
                    confirmationReentry = true;
                } else {
                    throw new IOException("Cannot save " + dartFile.getNameExt()
                            + ": another save is already running");
                }
            } else {
                if (sourceRestage != null || preparation != null || replacement != null) {
                    throw new IOException(
                            "Cannot save the Flutter Designer pair while a visual "
                            + "change is being applied or analyzed");
                }
                rejectConflictSaveLocked();
                if (staged == null) {
                    if (!sourceDirty && !failedSavePending
                            && !(unsavedHistoryCursor != null
                                && unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint)) {
                        return;
                    }
                    if (diskBaseline == null) {
                        throw new IOException(
                                "Cannot save Dart source: its exact paired disk "
                                + "baseline is unavailable; reload the document first");
                    }
                    sourceHistoryOwner = savedSourceHistoryOwnerLocked();
                    if (sourceHistoryOwner == null) {
                        sourceAttempt = new ActiveSourceSave(
                                diskBaseline,
                                currentThread,
                                externalEventEpoch,
                                null,
                                null);
                        activeSourceSave = sourceAttempt;
                        change = transitionLocked(
                                PairSaveCoordinatorStatus.SAVING_SOURCE, null);
                    }
                } else {
                    pairAuthority = staged;
                    pairStageEpoch = epoch;
                }
            }
        }

        if (confirmationReentry) {
            editor.saveDocumentThroughNetBeans();
            return;
        }
        if (sourceHistoryOwner != null) {
            // Query the command-session monitor only after releasing the pair
            // monitor. The selected lease will revalidate the exact identity.
            semanticBaselineHistory = sourceHistoryOwner.dirty();
        }
        if (sourceHistoryOwner != null) {
            boolean saved = semanticBaselineHistory
                    ? saveSemanticBaselineWithHistory(sourceHistoryOwner)
                    : saveSourceWithHistory(sourceHistoryOwner);
            if (!saved) {
                // The last native semantic edge was already trimmed and its
                // retained command owner closed before it could pin a Source
                // anchor. Re-enter through the ordinary Source-only path after
                // the exact stale authority was retired.
                save();
            }
            return;
        }
        if (sourceAttempt != null) {
            publishEffects(change);
            saveSourceOnly(sourceAttempt);
            return;
        }

        DesignerCommandSessionOrchestrator.DurableSaveLease pairLease;
        try {
            pairLease = pairAuthority.owner().beginDurableSave(
                    pairAuthority.proof().preparedPairIdentity());
        } catch (RuntimeException leaseFailure) {
            throw new IOException(
                    "Cannot pin the exact staged Designer command revision for Save: "
                    + reason(leaseFailure), leaseFailure);
        }

        IOException staleAuthority = null;
        synchronized (this) {
            if (staged != pairAuthority || epoch != pairStageEpoch
                    || preparation != null || replacement != null
                    || activePairSave != null || activeSourceSave != null
                    || activeFdOnlySave != null) {
                staleAuthority = new IOException(
                        "Cannot save the Flutter Designer pair: its exact staged "
                        + "authority changed while the command revision was being pinned");
            }
        }
        if (staleAuthority != null) {
            abortLeaseSafely(pairLease, staleAuthority);
            throw staleAuthority;
        }
        try {
            verifyStagedAuthority(pairAuthority, pairLease);
            verifyStagedStillCurrent(pairAuthority.proof());
        } catch (IOException invalidEvidence) {
            invalidateStagedPair(pairAuthority, pairLease, invalidEvidence);
            throw invalidEvidence;
        }
        SavedHistoryReanchorPlan reanchorPlan;
        try {
            reanchorPlan = prepareSavedHistoryReanchorPlan(
                    pairAuthority, pairLease);
        } catch (IOException | RuntimeException planningFailure) {
            IOException wrapped = asIOException(planningFailure);
            abortLeaseSafely(pairLease, wrapped);
            throw wrapped;
        }
        // The evidence check may open/snapshot the document and run listeners.
        // Confirm sole SaveCookie ownership again immediately before activation.
        try {
            rejectForeignSaveCookie();
        } catch (IOException foreignOwner) {
            invalidateStagedPair(pairAuthority, pairLease, foreignOwner);
            throw foreignOwner;
        }
        ActivePairSave pairAttempt;
        synchronized (this) {
            if (preparation != null || replacement != null
                    || staged != pairAuthority
                    || epoch != pairStageEpoch || activePairSave != null
                    || activeSourceSave != null || activeFdOnlySave != null
                    || !pairLease.ownsExactActiveRevision()
                    || pairLease.revision() != pairAuthority.revision()
                    || reanchorPlan.stagedIdentity() != pairAuthority
                    || reanchorPlan.leaseIdentity() != pairLease
                    || reanchorPlan.coordinatorEpoch() != epoch
                    || reanchorPlan.eventEpoch() != externalEventEpoch
                    || reanchorPlan.priorCursorIdentity()
                        != unsavedHistoryCursor) {
                staleAuthority = new IOException(
                        "Cannot save the Flutter Designer pair: its staged epoch "
                        + "or exact command authority changed during the final "
                        + "live-document check");
                pairAttempt = null;
            } else {
                pairAttempt = new ActivePairSave(
                        pairAuthority,
                        pairLease,
                        reanchorPlan,
                        Thread.currentThread(),
                        externalEventEpoch);
                activePairSave = pairAttempt;
                change = transitionLocked(
                        PairSaveCoordinatorStatus.SAVING_PAIR, null);
            }
        }
        if (staleAuthority != null) {
            abortLeaseSafely(pairLease, staleAuthority);
            throw staleAuthority;
        }
        publishEffects(change);
        saveStagedPair(pairAttempt);
    }

    /** Returns the exact transaction stream selected for the active save. */
    OutputStream openActiveOutput() throws IOException {
        synchronized (this) {
            if (activeFdOnlySave != null) {
                throw new IOException(
                        "The source serializer is unavailable during a "
                        + "Designer-only .fd transaction");
            }
            if (activePairSave != null) {
                requireOutputOwner(activePairSave.owner);
                if (activePairSave.outputOpened) {
                    throw new IOException(
                            "The paired Dart output stream was requested more than once");
                }
                activePairSave.outputOpened = true;
                return new ExactPairOutput(activePairSave);
            }
            if (activeSourceSave != null) {
                requireOutputOwner(activeSourceSave.owner);
                if (activeSourceSave.outputOpened) {
                    throw new IOException(
                            "The source Dart output stream was requested more than once");
                }
                activeSourceSave.outputOpened = true;
                return new ExactSourceOutput(activeSourceSave);
            }
            return null;
        }
    }

    /** Rejects a foreign save owner before CES accepts a document edit. */
    void beforeSourceModification() throws IOException {
        synchronized (this) {
            if (sourceRestage != null) {
                throw new IOException("Cannot edit Dart source while Save is analyzing its exact current version.");
            }
            if (activePairPathOperation != null) {
                throw new IOException(
                        "Cannot edit Dart source while paired "
                        + activePairPathOperation.operation()
                        + " owns the form");
            }
            if (historyTransition != null) {
                throw new IOException(
                        "Cannot edit Dart source while an exact unsaved Designer "
                        + "history transition owns the live pair");
            }
            if (activeFdOnlySave != null) {
                throw new IOException(
                        "Cannot modify Dart source while a Designer-only .fd "
                        + "transaction owns the exact clean source baseline");
            }
        }
        rejectForeignSaveCookie();
    }

    void sourceBecameModified() {
        DiskBaseline observed = null;
        long eventTicket;
        boolean captureBaseline;
        synchronized (this) {
            eventTicket = externalEventEpoch;
            captureBaseline = diskBaseline == null && preparation == null
                    && replacement == null
                    && activeFdOnlySave == null;
        }
        if (captureBaseline) {
            try {
                observed = readDiskBaseline();
            } catch (IOException failure) {
                LOGGER.log(Level.FINE,
                        "Cannot capture the modified source baseline", failure);
            }
        }
        StateChange change = null;
        synchronized (this) {
            sourceDirty = true;
            sourceStateEpoch++;
            if (activeFdOnlySave != null) {
                activeFdOnlySave.liveMutationObserved = true;
            }
            if (observed != null && diskBaseline == null
                    && eventTicket == externalEventEpoch) {
                diskBaseline = observed;
            }
            if (preparation == null && replacement == null && staged == null
                    && historyTransition == null
                    && activePairSave == null && activeSourceSave == null
                    && activeFdOnlySave == null
                    && !conflictStatusLocked()) {
                change = transitionLocked(
                        PairSaveCoordinatorStatus.DIRTY_SOURCE, null);
            }
        }
        publishEffects(change);
    }

    void sourceBecameUnmodified() {
        StateChange change = null;
        UnsavedPairHistoryCursor overlayCursor = null;
        synchronized (this) {
            sourceDirty = false;
            sourceStateEpoch++;
            if (staged == null
                    && historyTransition == null
                    && activePairSave == null
                    && activeSourceSave == null
                    && activeFdOnlySave == null
                    && unsavedHistoryCursor != null
                    && unsavedHistoryCursor.endpoint()
                        instanceof SourceOverlayBaselineHistoryEndpoint) {
                overlayCursor = unsavedHistoryCursor;
            }
            if (preparation == null && replacement == null && staged == null
                    && historyTransition == null
                    && activePairSave == null && activeSourceSave == null
                    && activeFdOnlySave == null
                    && !failedSavePending && !conflictStatusLocked()) {
                change = transitionLocked(PairSaveCoordinatorStatus.CLEAN, null);
            }
        }
        publishEffects(change);
        if (overlayCursor != null) {
            normalizeSavedSourceOverlayCursor(overlayCursor);
        }
    }

    /**
     * Native Redo may return from the semantic underlay to the exact saved
     * Source anchor.  Refresh only the coordinator cursor; the native edit and
     * command cursor remain untouched.
     */
    private void normalizeSavedSourceOverlayCursor(
            UnsavedPairHistoryCursor expectedCursor) {
        LiveDartDocumentSnapshot fresh;
        try {
            fresh = editor.liveSnapshot();
        } catch (IOException unavailable) {
            LOGGER.log(Level.FINE,
                    "Cannot refresh the exact saved Source history cursor",
                    unavailable);
            return;
        }
        SourceOverlayBaselineHistoryEndpoint overlay =
                (SourceOverlayBaselineHistoryEndpoint)
                        expectedCursor.endpoint();
        if (editor.sourceModified()
                || fresh.documentIdentity()
                    != expectedCursor.liveIdentity().documentIdentity()
                || !Arrays.equals(
                        overlay.revision().dartCandidateBytes(),
                        overlay.baselineDartBytes())
                || !Arrays.equals(
                        overlay.baselineDartBytes(),
                        fresh.markerBearingUtf8())) {
            return;
        }
        BaselineHistoryEndpoint saved = new BaselineHistoryEndpoint(
                overlay.revision(),
                overlay.currentIdentity(),
                overlay.baselineDartBytes(),
                overlay.baselineFdBytes());
        synchronized (this) {
            if (unsavedHistoryCursor == expectedCursor
                    && !sourceDirty
                    && staged == null
                    && historyTransition == null
                    && activePairSave == null
                    && activeSourceSave == null
                    && activeFdOnlySave == null
                    && controller.state() == overlay.currentIdentity()) {
                unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                        saved, fresh, (StagedPairProof) null);
            }
        }
    }

    /** Captures exact bytes used to rebuild the live source editor. */
    void sourceBaselineLoaded(byte[] exactSource) {
        Objects.requireNonNull(exactSource, "exactSource");
        long ticket;
        synchronized (this) {
            ticket = externalEventEpoch;
        }
        byte[] exactFd;
        try {
            exactFd = readBounded(
                    designerFile,
                    MAX_MODEL_PERSISTENCE_BYTES,
                    "paired .fd model");
        } catch (IOException failure) {
            LOGGER.log(Level.FINE,
                    "Cannot capture the paired .fd baseline after source load", failure);
            return;
        }
        synchronized (this) {
            if (ticket == externalEventEpoch && !sourceDirty
                    && preparation == null && replacement == null && staged == null
                    && activePairSave == null && activeSourceSave == null
                    && activeFdOnlySave == null) {
                diskBaseline = new DiskBaseline(exactSource, exactFd);
            }
        }
    }

    /** @return true when the controller must suppress reload for this event. */
    boolean handleFileEvent(FileEvent event) {
        synchronized (this) {
            if ((activePairPathOperation != null
                    && activePairPathOperation.owns(event))
                    || (pairPathOperationProvenance != null
                        && event.firedFrom(pairPathOperationProvenance))) {
                return true;
            }
        }
        if (transaction.owns(event)) {
            return true;
        }
        StateChange change = null;
        PairCandidateAnalysisTicket preparationTicket = null;
        PairCandidateAnalysisTicket replacementTicket = null;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                commandEffects = null;
        boolean suppress;
        synchronized (this) {
            externalEventEpoch++;
            diskBaseline = null;
            suppress = sourceDirty || failedSavePending || preparation != null
                    || replacement != null || historyTransition != null
                    || forwardAdmission != null
                    || staged != null || activePairSave != null
                    || activeSourceSave != null || activeFdOnlySave != null
                    || activePairPathOperation != null;
            if (forwardAdmission != null) {
                // The raw candidate already passed every fallible evidence
                // check and is about to be sealed into native history. Do not
                // invalidate its claimed command lease here: capture ACK must
                // still adopt the same command target so native history and
                // the command cursor cannot diverge. The ACK peer commit will
                // consume this poison evidence and publish one fail-closed
                // EXTERNAL_CONFLICT with no staged/history authority.
                String operation = event.getFile().isValid()
                        ? "changed" : "was deleted or renamed";
                forwardAdmission.poison(
                        "Paired file " + event.getFile().getPath() + " "
                        + operation + " during native forward admission");
            } else if (suppress) {
                if (preparation != null) {
                    PairPreparation invalidated = preparation;
                    preparation = null;
                    invalidated.closed = true;
                    preparationTicket = invalidated.analysisTicket;
                    try {
                        commandEffects = invalidated.commandLease
                                .invalidateClaimedDeferredEffects(
                                        invalidated.commandClaim);
                    } catch (RuntimeException invalidationFailure) {
                        commandEffects = DesignerCommandSessionOrchestrator
                                .DeferredLeaseEffects.none();
                        LOGGER.log(Level.WARNING,
                                "Cannot invalidate the exact first Designer "
                                + "command after an external preparation event",
                                invalidationFailure);
                    }
                } else if (replacement != null) {
                    PairReplacement invalidated = replacement;
                    replacement = null;
                    staged = null;
                    invalidated.closed = true;
                    replacementTicket = invalidated.analysisTicket;
                    try {
                        commandEffects = invalidated.commandLease
                                .invalidateClaimedDeferredEffects(
                                        invalidated.commandClaim);
                    } catch (RuntimeException invalidationFailure) {
                        commandEffects =
                                DesignerCommandSessionOrchestrator
                                        .DeferredLeaseEffects.none();
                        LOGGER.log(Level.WARNING,
                                "Cannot invalidate the exact Designer command "
                                + "after an external replacement event",
                                invalidationFailure);
                    }
                }
                if (activeFdOnlySave != null) {
                    suppressConflictSaveCookie = true;
                }
                if (activePairPathOperation != null) {
                    activePairPathOperation.markRecoveryConflict(
                            "a paired file changed outside the active "
                            + activePairPathOperation.operation()
                            + " transaction");
                }
                String operation = event.getFile().isValid()
                        ? "changed" : "was deleted or renamed";
                change = transitionLocked(
                        PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        "Paired file " + event.getFile().getPath() + " " + operation
                        + " outside the active exact save transaction");
            }
        }
        if (preparationTicket != null) {
            preparationTicket.cancel();
        }
        if (replacementTicket != null) {
            replacementTicket.cancel();
        }
        publishEffects(change, commandEffects);
        return suppress;
    }

    private PairCandidateAnalysisTicket prepareAnalysis(
            PairPreparation lease,
            Path projectRoot,
            DartCandidateWarningPolicy warningPolicy) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(warningPolicy, "warningPolicy");

        synchronized (this) {
            requireActivePreparationLocked(lease);
            if (lease.analysisTicket != null) {
                throw new IOException(
                        "The prepared Flutter Designer pair already owns an "
                        + "exact analyzer ticket");
            }
        }

        // Analysis authority is resolved only from this coordinator's bound
        // Dart FileObject and the configured Flutter SDK. A caller cannot
        // transfer the ticket to another same-named path or trust root.
        Path realDartPath = realLocalPath(dartFile, "paired Dart source");
        Path trustedFlutterSdkRealRoot = trustedFlutterSdkRoot();
        PairCandidateAnalysisTicket ticket = PairSaveEvidenceGate.prepareAnalysis(
                lease.expectedCurrent,
                lease.prepared,
                projectRoot,
                realDartPath,
                warningPolicy,
                trustedFlutterSdkRealRoot);
        try {
            // Ticket creation resolves real paths and may run arbitrary file
            // infrastructure. Register it only after an EDT recapture proves
            // that the exact pre-apply document revision is still present.
            rejectForeignSaveCookie();
            onEdt(() -> {
                LiveDartDocumentSnapshot currentLive = editor.liveSnapshot();
                synchronized (PairSaveCoordinator.this) {
                    requireActivePreparationLocked(lease);
                    if (lease.analysisTicket != null) {
                        throw new IOException(
                                "The preparation acquired another analyzer "
                                + "ticket while paths were being resolved");
                    }
                    if (externalEventEpoch != lease.eventTicket) {
                        throw new IOException(
                                "Cannot analyze Flutter Designer candidate: a "
                                + "paired file changed before ticket publication");
                    }
                    if (controller.state() != lease.expectedCurrent) {
                        throw new IOException(
                                "Cannot analyze Flutter Designer candidate: the "
                                + "loaded model revision changed before ticket publication");
                    }
                    if (!lease.initialLive.sameEvidence(currentLive)) {
                        throw new IOException(
                                "Cannot analyze Flutter Designer candidate: the "
                                + "live Dart revision changed before ticket publication");
                    }
                    lease.analysisTicket = ticket;
                }
                return null;
            });
            return ticket;
        } catch (IOException | RuntimeException | Error failure) {
            ticket.cancel();
            throw failure;
        }
    }

    private PairCandidateAnalysisTicket prepareReplacementAnalysis(
            PairReplacement lease,
            Path projectRoot,
            DartCandidateWarningPolicy warningPolicy) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(warningPolicy, "warningPolicy");
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (lease.analysisTicket != null) {
                throw new IOException(
                        "The staged Flutter Designer replacement already owns "
                        + "an exact analyzer ticket");
            }
        }

        Path realDartPath = realLocalPath(dartFile, "paired Dart source");
        Path trustedFlutterSdkRealRoot = trustedFlutterSdkRoot();
        PairCandidateAnalysisTicket ticket = PairSaveEvidenceGate.prepareAnalysis(
                lease.predecessorProof.loadedCurrentIdentity(),
                lease.candidatePair,
                projectRoot,
                realDartPath,
                warningPolicy,
                trustedFlutterSdkRealRoot);
        try {
            rejectForeignSaveCookie();
            onEdt(() -> {
                LiveDartDocumentSnapshot currentLive = editor.liveSnapshot();
                synchronized (PairSaveCoordinator.this) {
                    requireActiveReplacementLocked(lease);
                    if (lease.analysisTicket != null
                            || externalEventEpoch != lease.eventTicket
                            || !ownsStagedProofLocked(
                                    lease.predecessorProof)
                            || unsavedHistoryCursor
                                != lease.predecessorCursor
                            || controller.state()
                                != lease.predecessorProof
                                        .loadedCurrentIdentity()
                            || !lease.initialLive.sameEvidence(currentLive)
                            || !lease.commandLease
                                    .ownsExactActiveTransition()) {
                        throw new IOException(
                                "Cannot publish the staged replacement analyzer "
                                + "ticket: exact C1, Current, file epoch or pending command changed");
                    }
                    lease.analysisTicket = ticket;
                }
                return null;
            });
            return ticket;
        } catch (IOException | RuntimeException | Error failure) {
            ticket.cancel();
            throw failure;
        }
    }

    private PairSaveEvidenceResult acceptAnalysisAndReplace(
            PairReplacement lease,
            PairCandidateAnalysisTicket ticket,
            DartCandidateAnalysisResult analysis) throws IOException {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(analysis, "analysis");
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (lease.analysisTicket != ticket) {
                throw new IOException(
                        "The analyzer result does not belong to this exact "
                        + "staged Flutter Designer replacement");
            }
            if (lease.applied != null) {
                throw new IOException(
                        "The staged Flutter Designer successor was already applied");
            }
        }

        PairAnalyzedCandidateResult analyzedResult = ticket.accept(analysis);
        if (!(analyzedResult instanceof PairAnalyzedCandidateResult.Ready ready)) {
            PairSaveEvidenceResult rejected = new PairSaveEvidenceResult.Rejected(
                    analyzedResult.diagnostics());
            cancelReplacement(lease);
            return rejected;
        }
        PairAnalyzedCandidate analyzed = ready.analyzed();
        if (!analyzed.retainsTicket(ticket)) {
            IOException failure = new IOException(
                    "The analyzed staged Flutter Designer successor lost its "
                    + "exact ticket identity");
            recoverFailedReplacement(lease, failure);
            throw failure;
        }

        rejectForeignSaveCookie();
        ReplacementPublication publication;
        try {
            publication = onEdt(() -> applyAnalyzedReplacementOnEdt(
                    lease, ticket, analyzed, analysis));
        } catch (IOException failure) {
            if (isActiveReplacement(lease)) {
                recoverFailedReplacement(lease, failure);
            }
            throw failure;
        } catch (RuntimeException failure) {
            IOException wrapped = new IOException(
                    "Cannot replace the analyzed staged Flutter Designer revision: "
                    + reason(failure), failure);
            if (isActiveReplacement(lease)) {
                recoverFailedReplacement(lease, wrapped);
            }
            throw wrapped;
        }
        return publication.result();
    }

    /**
     * Applies a staged semantic successor whose exact outputs are the already
     * validated durable baseline. No analyzer run is needed: both candidate
     * files, the loaded Current and the retained baseline proof are identical.
     */
    private void replaceWithExactBaseline(PairReplacement lease)
            throws IOException {
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (!lease.baselineTarget()
                    || lease.analysisTicket != null
                    || lease.applied != null) {
                throw new IOException(
                        "Only one unanalyzed exact BASELINE target may collapse a staged pair");
            }
        }

        rejectForeignSaveCookie();
        try {
            onEdt(() -> {
                applyExactBaselineReplacementOnEdt(lease);
                return null;
            });
        } catch (IOException failure) {
            if (isActiveReplacement(lease)) {
                recoverFailedReplacement(lease, failure);
            }
            throw failure;
        } catch (RuntimeException failure) {
            IOException wrapped = new IOException(
                    "Cannot apply the exact Flutter Designer baseline target: "
                    + reason(failure), failure);
            if (isActiveReplacement(lease)) {
                recoverFailedReplacement(lease, wrapped);
            }
            throw wrapped;
        }
    }

    private void applyExactBaselineReplacementOnEdt(PairReplacement lease)
            throws IOException {
        IOException pendingFailure = null;
        try (EffectsDeferral effectsDeferral = deferEffects()) {
            try {
                LiveDartDocumentSnapshot before = editor.liveSnapshot();
                synchronized (this) {
                    requireActiveReplacementLocked(lease);
                    if (!lease.baselineTarget()
                            || externalEventEpoch != lease.eventTicket
                            || !ownsStagedProofLocked(lease.predecessorProof)
                            || unsavedHistoryCursor
                                != lease.predecessorCursor
                            || controller.state()
                                != lease.predecessorProof
                                        .loadedCurrentIdentity()
                            || !lease.initialLive.sameEvidence(before)
                            || !lease.commandLease
                                    .ownsExactActiveTransition()) {
                        throw new IOException(
                                "Cannot restore the exact Designer baseline: C1, Current, file epoch or pending command changed");
                    }
                }

                GeneratedDartRegions generated = lease.candidate.generation()
                        .generated().orElseThrow();
                PreparedDesignerPair previousPair = lease.predecessorProof.preparedPairIdentity();
                var previousLiveBaseline = new DartThreeWayIntegrityGate().evaluate(
                        previousPair.dartTransition().candidateIntegrity(),
                        lease.predecessor.document().source(), lease.predecessor.generation());
                var baselineTransition = new DartSourceTransitionPlanner().plan(
                        previousLiveBaseline, lease.initialLive.markerBearingUtf8(),
                        lease.predecessor.document().source(), lease.candidate.generation(),
                        previousPair.dartTransition().userSourceProjection().inverse());
                if (baselineTransition.status() != DartSourceTransitionStatus.READY
                        || !Arrays.equals(baselineTransition.plan().orElseThrow().candidateBytes(),
                                lease.candidate.dartCandidateBytes())) {
                    throw new IOException("Cannot restore baseline: the proved source transition differs from the retained candidate.");
                }
                editor.applyPreparedRegionsAndFinalize(
                        lease.initialLive,
                        generated,
                        lease.candidate.dartCandidateBytes(),
                        baselineTransition.plan().orElseThrow(),
                        forwardSemanticEdge(lease.commandLease),
                        applied -> prepareBaselineReplacementAdmissionWithinDocumentLock(
                                lease, applied, effectsDeferral));
            } catch (IOException | RuntimeException | Error failure) {
                pendingFailure = asIOException(failure);
                StateChange armedConflict = disarmForwardAdmission(lease);
                if (isActiveReplacement(lease)) {
                    ReplacementRelease recovery =
                            recoverFailedReplacementOnEdtDeferred(
                                    lease, pendingFailure);
                    publishEffects(
                            armedConflict == null
                                    ? recovery.change() : armedConflict,
                            recovery.commandEffects());
                }
            }
        }
        if (pendingFailure != null) {
            throw pendingFailure;
        }
        synchronized (this) {
            if (!lease.replaced) {
                throw new IOException(
                        "The exact BASELINE replacement did not publish its joint pair/command outcome");
            }
        }
    }

    /** Verifies and prepares a callback-free PAIRED-to-BASELINE joint commit. */
    private DesignerAtomicEditCapture.JointCommit
            prepareBaselineReplacementAdmissionWithinDocumentLock(
            PairReplacement lease,
            LiveDartDocumentSnapshot applied,
            EffectsDeferral effectsDeferral) throws IOException {
        ArmedForwardAdmission admission;
        UnsavedPairHistoryEdge historyEdge;
        UnsavedPairHistoryCursor targetCursor;
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (!lease.baselineTarget()
                    || externalEventEpoch != lease.eventTicket
                    || !ownsStagedProofLocked(lease.predecessorProof)
                    || unsavedHistoryCursor != lease.predecessorCursor
                    || controller.state()
                        != lease.predecessorProof.loadedCurrentIdentity()
                    || !sourceDirty
                    || !editor.sourceModified()
                    || !lease.commandLease.ownsExactActiveTransition()
                    || !Arrays.equals(
                            applied.markerBearingUtf8(),
                            lease.candidate.dartCandidateBytes())
                    || !Arrays.equals(
                            lease.candidate.dartCandidateBytes(),
                            lease.predecessorProof.baselineDartBytes())
                    || !Arrays.equals(
                            lease.candidate.fdBytes(),
                            lease.predecessorProof.baselineFdBytes())) {
                throw new IOException(
                        "Cannot jointly publish the exact BASELINE target: file, Current, dirty live or pending-command evidence changed");
            }
            BaselineHistoryEndpoint candidateEndpoint =
                    new BaselineHistoryEndpoint(
                            lease.candidate,
                            lease.predecessorProof.loadedCurrentIdentity(),
                            lease.predecessorProof.baselineDartBytes(),
                            lease.predecessorProof.baselineFdBytes());
            historyEdge = new UnsavedPairHistoryEdge(
                    lease.predecessorEndpoint,
                    candidateEndpoint,
                    lease.commandLease.owner());
            requireRegisterableHistoryEdgeLocked(historyEdge);
            targetCursor = new UnsavedPairHistoryCursor(
                    candidateEndpoint, applied, (StagedPairProof) null);
            admission = armForwardAdmissionLocked(lease, true);
        }
        forwardAdmissionHook.afterArmed(lease.commandLease, true);
        return () -> commitBaselineReplacementAdmission(
                admission,
                lease,
                applied,
                historyEdge,
                targetCursor,
                effectsDeferral);
    }

    private ReplacementPublication applyAnalyzedReplacementOnEdt(
            PairReplacement lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis) throws IOException {
        ReplacementPublication publication = null;
        ReplacementRelease recovery = null;
        IOException pendingFailure = null;
        try (EffectsDeferral effectsDeferral = deferEffects()) {
            try {
                publication = applyAnalyzedReplacementWithinDeferral(
                        lease,
                        ticket,
                        analyzed,
                        analysis,
                        effectsDeferral);
            } catch (IOException | RuntimeException | Error failure) {
                pendingFailure = asIOException(failure);
                StateChange armedConflict = disarmForwardAdmission(lease);
                if (isActiveReplacement(lease)) {
                    recovery = recoverFailedReplacementOnEdtDeferred(
                            lease, pendingFailure);
                    // Any queued DIRTY/EXTERNAL transition and this terminal
                    // recovery outcome remain hidden until pair and command
                    // semantic state have both been finalized and the document
                    // lock has been released.
                    publishEffects(
                            armedConflict == null
                                    ? recovery.change() : armedConflict,
                            recovery.commandEffects());
                }
            }
        }
        if (pendingFailure != null) {
            throw pendingFailure;
        }
        if (publication == null) {
            throw new IOException(
                    "The staged Flutter Designer replacement publication is unavailable");
        }
        return publication;
    }

    private ReplacementPublication applyAnalyzedReplacementWithinDeferral(
            PairReplacement lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis,
            EffectsDeferral effectsDeferral) throws IOException {
        LiveDartDocumentSnapshot before = editor.liveSnapshot();
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (lease.analysisTicket != ticket
                    || externalEventEpoch != lease.eventTicket
                    || !ownsStagedProofLocked(lease.predecessorProof)
                    || unsavedHistoryCursor != lease.predecessorCursor
                    || controller.state()
                        != lease.predecessorProof.loadedCurrentIdentity()
                    || !lease.initialLive.sameEvidence(before)
                    || !lease.commandLease.ownsExactActiveTransition()) {
                throw new IOException(
                        "Cannot apply analyzed staged successor: exact C1, "
                        + "Current, file epoch or pending command changed during analysis");
            }
        }

        GeneratedDartRegions generated = lease.liveTransition.generation()
                .generated().orElseThrow();
        var result = new java.util.concurrent.atomic.AtomicReference<
                PairSaveEvidenceResult>();
        FlutterDesignerEditorSupport.AppliedSemanticFinalizer finalizer =
                applied -> prepareReplacementAdmissionWithinDocumentLock(
                        lease,
                        ticket,
                        analyzed,
                        analysis,
                        applied,
                        result,
                        effectsDeferral);
        if (lease.liveTransition.status() == DartSourceTransitionStatus.NO_CHANGES) {
            editor.applyUnchangedSourceAndFinalize(lease.initialLive,
                    lease.candidatePair.prospectiveDartBytes(),
                    forwardSemanticEdge(lease.commandLease), finalizer);
        } else {
            editor.applyPreparedRegionsAndFinalize(lease.initialLive, generated,
                    lease.candidatePair.prospectiveDartBytes(),
                    lease.liveTransition.plan().orElseThrow(),
                    forwardSemanticEdge(lease.commandLease), finalizer);
        }
        PairSaveEvidenceResult evaluated = result.get();
        if (evaluated == null) {
            throw new IOException(
                    "The staged Flutter Designer replacement did not publish "
                    + "its joint pair/command outcome");
        }
        return new ReplacementPublication(evaluated);
    }

    /** Verifies under the exact live lock and arms a callback-free ACK commit. */
    private DesignerAtomicEditCapture.JointCommit
            prepareReplacementAdmissionWithinDocumentLock(
            PairReplacement lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis,
            LiveDartDocumentSnapshot applied,
            java.util.concurrent.atomic.AtomicReference<PairSaveEvidenceResult> result,
            EffectsDeferral effectsDeferral) throws IOException {
        PairSaveEvidenceResult evaluated = PairSaveEvidenceGate.bindApplied(
                analyzed, applied);
        if (!(evaluated instanceof PairSaveEvidenceResult.Ready ready)) {
            PairSaveEvidenceDiagnostic diagnostic = evaluated.diagnostics().getFirst();
            throw new IOException(
                    "Cannot bind applied C2 evidence: " + diagnostic.code()
                    + " at " + diagnostic.subject() + ": "
                    + diagnostic.message());
        }
        PairSaveEvidence evidence = ready.evidence();
        ArmedForwardAdmission admission;
        UnsavedPairHistoryEdge historyEdge;
        StagedPairAuthority targetAuthority;
        UnsavedPairHistoryCursor targetCursor;
        synchronized (this) {
            requireActiveReplacementLocked(lease);
            if (externalEventEpoch != lease.eventTicket
                    || !ownsStagedProofLocked(lease.predecessorProof)
                    || unsavedHistoryCursor != lease.predecessorCursor
                    || controller.state()
                        != lease.predecessorProof.loadedCurrentIdentity()
                    || !sourceDirty
                    || !lease.commandLease.ownsExactActiveTransition()
                    || !evidence.retainsExactAnalysis(ticket, analyzed)
                    || !evidence.retainsExactInputs(
                            lease.predecessorProof.loadedCurrentIdentity(),
                            lease.candidatePair,
                            applied,
                            analysis)) {
                throw new IOException(
                        "Cannot jointly publish staged C2: exact file, Current, "
                        + "dirty live, analysis or pending-command evidence changed");
            }
            historyEdge = prepareReplacementHistoryEdgeLocked(lease, evidence);
            StagedPairProof targetProof =
                    new AnalyzedStagedPairProof(evidence);
            targetAuthority = new StagedPairAuthority(
                    targetProof,
                    lease.commandLease.owner(),
                    lease.commandLease.candidateRevision());
            targetCursor = new UnsavedPairHistoryCursor(
                    historyEdge.after(), applied, targetProof);
            admission = armForwardAdmissionLocked(lease, true);
            result.set(evaluated);
        }
        forwardAdmissionHook.afterArmed(lease.commandLease, true);
        return () -> commitReplacementAdmission(
                admission,
                lease,
                applied,
                evidence,
                historyEdge,
                targetAuthority,
                targetCursor,
                effectsDeferral);
    }

    private void cancelReplacement(PairReplacement lease) throws IOException {
        PairCandidateAnalysisTicket ticket;
        synchronized (this) {
            if (lease.replaced || lease.closed && replacement != lease) {
                return;
            }
            requireActiveReplacementLocked(lease);
            ticket = lease.analysisTicket;
        }
        if (ticket != null) {
            ticket.cancel();
        }

        try {
            ReplacementRelease release = onEdt(() ->
                    releaseUnchangedReplacementOnEdt(lease));
            publishEffects(release.change(), release.commandEffects());
        } catch (IOException | RuntimeException | Error failure) {
            IOException wrapped = asIOException(failure);
            failReplacementRecovery(lease, wrapped);
            throw wrapped;
        }
    }

    /**
     * Clears staged authority when preflight has positively disproved durable B
     * or the sole SaveCookie owner. This is not a retryable preparation abort:
     * exact C1 is already stale relative to the pair's known external state.
     */
    private void terminalizeKnownReplacementConflict(
            PairReplacement lease,
            IOException primary) {
        StateChange change;
        PairCandidateAnalysisTicket ticket;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        RuntimeException invalidationFailure = null;
        synchronized (this) {
            if (replacement != lease) {
                return;
            }
            ticket = lease.analysisTicket;
            try {
                commandEffects = lease.commandLease
                        .invalidateClaimedDeferredEffects(lease.commandClaim);
            } catch (RuntimeException failure) {
                invalidationFailure = failure;
                primary.addSuppressed(failure);
                commandEffects = DesignerCommandSessionOrchestrator
                        .DeferredLeaseEffects.none();
            }
            replacement = null;
            staged = null;
            diskBaseline = null;
            lease.closed = true;
            if (invalidationFailure == null) {
                change = transitionLocked(
                        PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        primary.getMessage());
            } else {
                failedSavePending = true;
                change = transitionLocked(
                        PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        "Cannot invalidate the exact staged command after a "
                        + "known replacement conflict: "
                        + reason(invalidationFailure));
            }
        }
        if (ticket != null) {
            ticket.cancel();
        }
        publishEffects(change, commandEffects);
    }

    private ReplacementRelease releaseUnchangedReplacementOnEdt(
            PairReplacement lease) throws IOException {
        LiveDartDocumentSnapshot current = editor.liveSnapshot();
        if (!lease.initialLive.sameEvidence(current)) {
            throw new IOException(
                    "Cannot release staged replacement as unchanged: live C1 "
                    + "no longer has its exact evidence identity");
        }
        var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
        var commandEffects = new java.util.concurrent.atomic.AtomicReference<
                DesignerCommandSessionOrchestrator.DeferredLeaseEffects>();
        // DesignerAtomicEditCapture already atomic-undoes an unadmitted
        // semantic mutation before unlock.  Do not discard the pre-existing
        // native semantic graph while rebinding the freshly versioned C1.
        editor.verifyAndFinalizePreparation(
                current,
                false,
                () -> {
                    synchronized (PairSaveCoordinator.this) {
                        requireActiveReplacementUnchecked(lease);
                        if (externalEventEpoch != lease.eventTicket
                                || !ownsStagedProofLocked(
                                        lease.predecessorProof)
                                || unsavedHistoryCursor
                                    != lease.predecessorCursor
                                || controller.state()
                                    != lease.predecessorProof
                                            .loadedCurrentIdentity()) {
                            throw new IllegalStateException(
                                    "Exact C1 authority changed before replacement release");
                        }
                        commandEffects.set(
                                lease.commandLease
                                        .abortToExactPredecessorDeferredEffects(
                                                lease.commandClaim));
                        replacement = null;
                        lease.closed = true;
                        change.set(transitionLocked(
                                PairSaveCoordinatorStatus.STAGED_PAIR, null));
                    }
                });
        return new ReplacementRelease(change.get(), commandEffects.get());
    }

    private void recoverFailedReplacement(
            PairReplacement lease, IOException primary) {
        try {
            ReplacementRelease release = onEdt(() ->
                    recoverFailedReplacementOnEdtDeferred(lease, primary));
            publishEffects(release.change(), release.commandEffects());
        } catch (IOException | RuntimeException | Error unexpectedFailure) {
            primary.addSuppressed(unexpectedFailure);
            failReplacementRecovery(lease, primary);
        }
    }

    private ReplacementRelease recoverFailedReplacementOnEdtDeferred(
            PairReplacement lease, IOException primary) {
        if (!(primary instanceof LiveDartDocumentBridge.RecoveryFailure)) {
            try {
                return rebindRestoredReplacementOnEdt(lease);
            } catch (IOException | RuntimeException | Error recoveryFailure) {
                primary.addSuppressed(recoveryFailure);
            }
        }
        return failReplacementRecoveryDeferred(lease, primary);
    }

    private ReplacementRelease rebindRestoredReplacementOnEdt(
            PairReplacement lease) throws IOException {
        LiveDartDocumentSnapshot current = editor.liveSnapshot();
        if (!sameRestoredLiveContent(lease.initialLive, current)) {
            throw new IOException(
                    "Cannot recover staged replacement: live Dart is neither "
                    + "exact C1 nor a verified C2 to C1 rollback");
        }
        boolean mutationObserved = current.documentVersion()
                > lease.initialLive.documentVersion();
        StagedPairProof fresh = mutationObserved
                ? bindHistoryEndpoint(lease.predecessorEndpoint, current)
                : lease.predecessorProof;
        if (fresh == null) {
            throw new IOException(
                    "Cannot rebind restored C1: the exact predecessor is not paired");
        }
        var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
        var commandEffects = new java.util.concurrent.atomic.AtomicReference<
                DesignerCommandSessionOrchestrator.DeferredLeaseEffects>();
        editor.verifyAndFinalizePreparation(
                current,
                false,
                () -> {
                    synchronized (PairSaveCoordinator.this) {
                        requireActiveReplacementUnchecked(lease);
                        if (externalEventEpoch != lease.eventTicket
                                || !ownsStagedProofLocked(
                                        lease.predecessorProof)
                                || unsavedHistoryCursor
                                    != lease.predecessorCursor
                                || controller.state()
                                    != lease.predecessorProof
                                            .loadedCurrentIdentity()) {
                            throw new IllegalStateException(
                                    "Pair authority changed while restored C1 was being rebound");
                        }
                        commandEffects.set(
                                lease.commandLease
                                        .abortToExactPredecessorDeferredEffects(
                                                lease.commandClaim));
                        staged = new StagedPairAuthority(
                                fresh,
                                lease.commandLease.owner(),
                                lease.commandLease.predecessorRevision());
                        unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                                lease.predecessorEndpoint,
                                current,
                                fresh);
                        replacement = null;
                        lease.closed = true;
                        change.set(transitionLocked(
                                PairSaveCoordinatorStatus.STAGED_PAIR, null));
                    }
                });
        return new ReplacementRelease(change.get(), commandEffects.get());
    }

    private void failReplacementRecovery(
            PairReplacement lease, IOException failure) {
        ReplacementRelease release = failReplacementRecoveryDeferred(
                lease, failure);
        publishEffects(release.change(), release.commandEffects());
    }

    private ReplacementRelease failReplacementRecoveryDeferred(
            PairReplacement lease, IOException failure) {
        StateChange change;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            if (replacement == lease) {
                replacement = null;
            }
            staged = null;
            unsavedPairHistory.clear();
            unsavedHistoryCursor = null;
            unsavedHistoryOwner = null;
            lease.closed = true;
            failedSavePending = true;
            try {
                commandEffects = lease.commandLease
                        .invalidateClaimedDeferredEffects(lease.commandClaim);
            } catch (RuntimeException invalidationFailure) {
                failure.addSuppressed(invalidationFailure);
                commandEffects = DesignerCommandSessionOrchestrator
                        .DeferredLeaseEffects.none();
            }
            change = state.status() == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                    ? null
                    : transitionLocked(
                            PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                            "Cannot establish an exact C1 or C2 Flutter Designer "
                            + "replacement outcome: " + reason(failure));
        }
        return new ReplacementRelease(change, commandEffects);
    }

    private PairSaveEvidenceResult acceptAnalysisAndStage(
            PairPreparation lease,
            PairCandidateAnalysisTicket ticket,
            DartCandidateAnalysisResult analysis) throws IOException {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(analysis, "analysis");
        synchronized (this) {
            requireActivePreparationLocked(lease);
            if (lease.analysisTicket != ticket) {
                throw new IOException(
                        "The analyzer result does not belong to this exact "
                        + "Flutter Designer preparation");
            }
            if (lease.applied != null) {
                throw new IOException(
                        "The prepared Flutter Designer candidate was already applied");
            }
        }

        // This is the authorization edge: all path, hash, version, diagnostic
        // and symbol-navigation evidence is consumed before the editor may be
        // changed. A rejected/cancelled result therefore leaves B untouched.
        PairAnalyzedCandidateResult analyzedResult = ticket.accept(analysis);
        if (!(analyzedResult instanceof PairAnalyzedCandidateResult.Ready ready)) {
            PairSaveEvidenceResult rejected = new PairSaveEvidenceResult.Rejected(
                    analyzedResult.diagnostics());
            cancelPreparation(lease);
            return rejected;
        }
        PairAnalyzedCandidate analyzed = ready.analyzed();
        if (!analyzed.retainsTicket(ticket)) {
            IOException failure = new IOException(
                    "The analyzed Flutter Designer candidate lost its exact "
                    + "ticket identity");
            cancelAfterFailure(lease, failure);
            throw failure;
        }

        rejectForeignSaveCookie();
        StagePublication publication;
        try {
            publication = onEdt(() -> applyAnalyzedAndStageOnEdt(
                    lease, ticket, analyzed, analysis));
        } catch (IOException failure) {
            if (isActivePreparation(lease)) {
                abortFailedApply(lease, failure);
            }
            throw failure;
        } catch (RuntimeException failure) {
            IOException wrapped = new IOException(
                    "Cannot apply the analyzed Flutter Designer candidate: "
                    + reason(failure), failure);
            if (isActivePreparation(lease)) {
                abortFailedApply(lease, wrapped);
            }
            throw wrapped;
        }
        return publication.result();
    }

    /**
     * One non-interleavable EDT transition from exact predecessor B to exact
     * candidate C and staged evidence. Analyzer I/O has already completed.
     */
    private StagePublication applyAnalyzedAndStageOnEdt(
            PairPreparation lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis) throws IOException {
        StagePublication publication = null;
        PreparationRelease recovery = null;
        IOException pendingFailure = null;
        try (EffectsDeferral effectsDeferral = deferEffects()) {
            try {
                publication = applyAnalyzedAndStageWithinDeferral(
                        lease,
                        ticket,
                        analyzed,
                        analysis,
                        effectsDeferral);
            } catch (IOException | RuntimeException | Error failure) {
                pendingFailure = asIOException(failure);
                StateChange armedConflict = disarmForwardAdmission(lease);
                if (isActivePreparation(lease)) {
                    recovery = recoverFailedPreparationOnEdtDeferred(
                            lease, pendingFailure);
                    // Dirty/file-event callbacks raised by apply or rollback
                    // stay queued until the exact command lease has either
                    // returned to B or been invalidated fail-closed.
                    publishEffects(
                            armedConflict == null
                                    ? recovery.change() : armedConflict,
                            recovery.commandEffects());
                }
            }
        }
        if (pendingFailure != null) {
            throw pendingFailure;
        }
        if (publication == null) {
            throw new IOException(
                    "The first Flutter Designer command publication is unavailable");
        }
        return publication;
    }

    private StagePublication applyAnalyzedAndStageWithinDeferral(
            PairPreparation lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis,
            EffectsDeferral effectsDeferral) throws IOException {
        LiveDartDocumentSnapshot before = editor.liveSnapshot();
        synchronized (this) {
            requireActivePreparationLocked(lease);
            if (lease.analysisTicket != ticket
                    || externalEventEpoch != lease.eventTicket
                    || controller.state() != lease.expectedCurrent
                    || !lease.initialLive.sameEvidence(before)
                    || !lease.commandLease.ownsExactActiveTransition()) {
                throw new IOException(
                        "Cannot apply analyzed Flutter Designer candidate: the "
                        + "exact source/model/command predecessor changed during analysis");
            }
        }

        GeneratedDartRegions generated = lease.prepared.dartTransition()
                .generation().generated().orElseThrow();
        var result = new java.util.concurrent.atomic.AtomicReference<
                PairSaveEvidenceResult>();
        editor.applyPreparedRegionsAndFinalize(
                lease.initialLive,
                generated,
                lease.prepared.prospectiveDartBytes(),
                lease.prepared.dartTransition(),
                forwardSemanticEdge(lease.commandLease),
                applied -> prepareInitialAdmissionWithinDocumentLock(
                        lease,
                        ticket,
                        analyzed,
                        analysis,
                        applied,
                        result,
                        effectsDeferral));
        PairSaveEvidenceResult evaluated = result.get();
        if (evaluated == null) {
            throw new IOException(
                    "The first Flutter Designer command did not publish its "
                    + "joint pair/command outcome");
        }
        return new StagePublication(evaluated);
    }

    /** Verifies under the exact live lock and arms a callback-free ACK commit. */
    private DesignerAtomicEditCapture.JointCommit
            prepareInitialAdmissionWithinDocumentLock(
            PairPreparation lease,
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed,
            DartCandidateAnalysisResult analysis,
            LiveDartDocumentSnapshot applied,
            java.util.concurrent.atomic.AtomicReference<PairSaveEvidenceResult> result,
            EffectsDeferral effectsDeferral) throws IOException {
        PairSaveEvidenceResult evaluated = PairSaveEvidenceGate.bindApplied(
                analyzed, applied);
        if (!(evaluated instanceof PairSaveEvidenceResult.Ready ready)) {
            PairSaveEvidenceDiagnostic diagnostic = evaluated.diagnostics().getFirst();
            throw new IOException(
                    "Cannot bind applied C1 evidence: " + diagnostic.code()
                    + " at " + diagnostic.subject() + ": "
                    + diagnostic.message());
        }
        PairSaveEvidence evidence = ready.evidence();
        ArmedForwardAdmission admission;
        UnsavedPairHistoryEdge historyEdge;
        StagedPairAuthority targetAuthority;
        UnsavedPairHistoryCursor targetCursor;
        synchronized (this) {
            requireActivePreparationLocked(lease);
            if (externalEventEpoch != lease.eventTicket
                    || controller.state() != lease.expectedCurrent
                    || !sourceDirty
                    || !lease.commandLease.ownsExactActiveTransition()
                    || !evidence.retainsExactAnalysis(ticket, analyzed)
                    || !evidence.retainsExactInputs(
                            lease.expectedCurrent,
                            lease.prepared,
                            applied,
                            analysis)) {
                throw new IOException(
                        "Cannot jointly publish staged C1: exact file, Current, "
                        + "dirty live, analysis or pending-command evidence changed");
            }
            historyEdge = prepareInitialHistoryEdgeLocked(lease, evidence);
            StagedPairProof targetProof =
                    new AnalyzedStagedPairProof(evidence);
            targetAuthority = new StagedPairAuthority(
                    targetProof,
                    lease.commandLease.owner(),
                    lease.commandLease.candidateRevision());
            targetCursor = new UnsavedPairHistoryCursor(
                    historyEdge.after(), applied, targetProof);
            admission = armForwardAdmissionLocked(lease, false);
            result.set(evaluated);
        }
        forwardAdmissionHook.afterArmed(lease.commandLease, false);
        return () -> commitInitialAdmission(
                admission,
                lease,
                applied,
                evidence,
                historyEdge,
                targetAuthority,
                targetCursor,
                effectsDeferral);
    }

    private void cancelAfterFailure(PairPreparation lease, IOException primary)
            throws IOException {
        try {
            cancelPreparation(lease);
        } catch (IOException rollbackFailure) {
            primary.addSuppressed(rollbackFailure);
            throw primary;
        }
    }

    private void cancelPreparation(PairPreparation lease) throws IOException {
        LiveDartDocumentSnapshot applied;
        PairCandidateAnalysisTicket ticket;
        synchronized (this) {
            if (lease.staged || lease.closed && preparation != lease) {
                return;
            }
            requireActivePreparationLocked(lease);
            applied = lease.applied;
            ticket = lease.analysisTicket;
            lease.closed = true;
        }
        if (ticket != null) {
            ticket.cancel();
        }

        if (applied != null) {
            try {
                PreparationRelease release = restoreAndReleasePreparationOnEdt(
                        lease, applied);
                publishEffects(release.change(), release.commandEffects());
            } catch (IOException failure) {
                failPreparationRecovery(lease, failure);
                throw failure;
            }
            return;
        }
        // Even before Designer apply, the user may have edited Source while
        // the lease was open. Snapshot and release on the EDT so that close
        // never resets that newer live revision to the lease's initial state.
        try {
            PreparationRelease release = onEdt(() ->
                    abortFailedApplyOnEdt(lease));
            publishEffects(release.change(), release.commandEffects());
        } catch (IOException snapshotFailure) {
            failPreparationRecovery(lease, snapshotFailure);
            throw snapshotFailure;
        }
    }

    /**
     * Exact live restore, Undo/savepoint cleanup and lease release form one EDT
     * critical section. No user edit or Undo can land between restoration and
     * the decision whether the source returns to its initial clean/dirty state.
     */
    private PreparationRelease restoreAndReleasePreparationOnEdt(
            PairPreparation lease, LiveDartDocumentSnapshot applied)
            throws IOException {
        return onEdt(() -> {
            var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
            var commandEffects = new java.util.concurrent.atomic.AtomicReference<
                    DesignerCommandSessionOrchestrator.DeferredLeaseEffects>();
            try (EffectsDeferral ignored = deferEffects()) {
                editor.restoreAndFinalizePreparation(
                        applied,
                        lease.initialLive,
                        true,
                        () -> releasePreparationWithinDocumentLock(
                                lease, true, change, commandEffects));
            }
            return new PreparationRelease(change.get(), commandEffects.get());
        });
    }

    /** Called only while the exact live document revision is atomically locked. */
    private void releasePreparationWithinDocumentLock(
            PairPreparation lease,
            boolean mutationObserved,
            java.util.concurrent.atomic.AtomicReference<StateChange> change,
            java.util.concurrent.atomic.AtomicReference<
                    DesignerCommandSessionOrchestrator.DeferredLeaseEffects>
                    commandEffects) {
        synchronized (this) {
            commandEffects.set(lease.commandLease
                    .abortToExactPredecessorDeferredEffects(
                            lease.commandClaim));
            if (preparation == lease) {
                preparation = null;
            }
            lease.closed = true;
            boolean exactEvent = externalEventEpoch == lease.eventTicket;
            if (exactEvent) {
                sourceDirty = sourceDirty
                        || lease.initiallySourceDirty
                        || mutationObserved;
            }
            change.set(conflictStatusLocked()
                    ? null
                    : transitionLocked(
                            sourceDirty
                                    ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                    : PairSaveCoordinatorStatus.CLEAN,
                            null));
        }
    }

    private void failPreparationRecovery(
            PairPreparation lease, IOException failure) {
        PreparationRelease release = failPreparationRecoveryDeferred(
                lease, failure);
        publishEffects(release.change(), release.commandEffects());
    }

    private PreparationRelease failPreparationRecoveryDeferred(
            PairPreparation lease, IOException failure) {
        StateChange change;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            if (preparation == lease) {
                preparation = null;
            }
            lease.closed = true;
            failedSavePending = true;
            try {
                commandEffects = lease.commandLease
                        .invalidateClaimedDeferredEffects(lease.commandClaim);
            } catch (RuntimeException invalidationFailure) {
                failure.addSuppressed(invalidationFailure);
                commandEffects = DesignerCommandSessionOrchestrator
                        .DeferredLeaseEffects.none();
            }
            change = transitionLocked(
                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    "Cannot restore the exact live Dart snapshot after abandoning "
                    + "the prepared pair: " + reason(failure));
        }
        return new PreparationRelease(change, commandEffects);
    }

    /**
     * Releases a lease when apply failed before publishing {@code applied}.
     * LiveDartDocumentBridge distinguishes an unverifiable rollback from a
     * pre-mutation rejection or an exactly restored mutation.
     */
    private void abortFailedApply(
            PairPreparation lease, IOException failure) {
        if (failure instanceof LiveDartDocumentBridge.RecoveryFailure) {
            failPreparationRecovery(lease, failure);
            return;
        }

        PreparationRelease release;
        try {
            release = onEdt(() -> abortFailedApplyOnEdt(lease));
        } catch (IOException snapshotFailure) {
            failure.addSuppressed(snapshotFailure);
            failPreparationRecovery(lease, failure);
            return;
        }
        publishEffects(release.change(), release.commandEffects());
    }

    /**
     * Completes first-command recovery while outward effects remain deferred.
     * An exact restored B releases the claim back to its predecessor; every
     * unverifiable result invalidates the claimed command session.
     */
    private PreparationRelease recoverFailedPreparationOnEdtDeferred(
            PairPreparation lease, IOException primary) {
        if (!(primary instanceof LiveDartDocumentBridge.RecoveryFailure)) {
            try {
                return abortFailedApplyOnEdt(lease);
            } catch (IOException | RuntimeException | Error recoveryFailure) {
                primary.addSuppressed(recoveryFailure);
            }
        }
        return failPreparationRecoveryDeferred(lease, primary);
    }

    private PreparationRelease abortFailedApplyOnEdt(PairPreparation lease)
            throws IOException {
        LiveDartDocumentSnapshot current = editor.liveSnapshot();
        boolean exactInitial = sameRestoredLiveContent(
                lease.initialLive, current);
        boolean mutationObserved = current.documentVersion()
                > lease.initialLive.documentVersion();
        if (exactInitial) {
            var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
            var commandEffects = new java.util.concurrent.atomic.AtomicReference<
                    DesignerCommandSessionOrchestrator.DeferredLeaseEffects>();
            // The MIME capture guarantees that a rejected forward mutation
            // contributed no native entry. Preserve any older Source history.
            editor.verifyAndFinalizePreparation(
                    current,
                    false,
                    () -> releasePreparationWithinDocumentLock(
                            lease, mutationObserved, change, commandEffects));
            return new PreparationRelease(change.get(), commandEffects.get());
        }

        synchronized (this) {
            if (preparation == lease) {
                preparation = null;
            }
            lease.closed = true;
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects;
            try {
                commandEffects = lease.commandLease
                        .invalidateClaimedDeferredEffects(lease.commandClaim);
            } catch (RuntimeException invalidationFailure) {
                commandEffects = DesignerCommandSessionOrchestrator
                        .DeferredLeaseEffects.none();
                LOGGER.log(Level.WARNING,
                        "Cannot invalidate the first Designer command after its "
                        + "exact B predecessor was lost",
                        invalidationFailure);
            }
            StateChange change = conflictStatusLocked()
                    ? null
                    : transitionLocked(
                            sourceDirty
                                    ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                    : PairSaveCoordinatorStatus.CLEAN,
                            null);
            return new PreparationRelease(change, commandEffects);
        }
    }

    private static boolean sameRestoredLiveContent(
            LiveDartDocumentSnapshot expected,
            LiveDartDocumentSnapshot actual) {
        return expected.documentIdentity() == actual.documentIdentity()
                && Arrays.equals(
                        expected.markerBearingUtf8(), actual.markerBearingUtf8())
                && expected.imports().sectionIdentity()
                        == actual.imports().sectionIdentity()
                && expected.build().sectionIdentity()
                        == actual.build().sectionIdentity();
    }

    /** Called only under this coordinator monitor. */
    private DesignerCommandSessionOrchestrator
            savedSourceHistoryOwnerLocked() throws IOException {
        if (unsavedHistoryCursor == null) {
            if (!unsavedPairHistory.isEmpty()) {
                throw new IOException(
                        "Cannot save Dart source: retained Designer edges have no exact cursor");
            }
            unsavedHistoryOwner = null;
            return null;
        }
        if (unsavedHistoryOwner == null
                || unsavedHistoryCursor.stagedProof() != null
                || !(unsavedHistoryCursor.endpoint().revision().persistenceKind()
                        == DesignerRevisionPersistenceKind.BASELINE
                    || unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint)) {
            throw new IOException(
                    "Cannot save Dart source: retained Designer history has no exact saved cursor");
        }
        if (!unsavedHistoryOwner.retainsOpenHistoryAuthority()) {
            DesignerCommandSessionOrchestrator staleOwner =
                    unsavedHistoryOwner;
            if (retireClosedEdgeLessHistoryOwnerLocked(staleOwner)) {
                return null;
            }
            throw new IOException(
                    "Cannot save Dart source: retained Designer history belongs to a closed command owner");
        }
        for (UnsavedPairHistoryEdge edge : unsavedPairHistory.values()) {
            if (unsavedHistoryOwner != edge.owner()) {
                throw new IOException(
                        "Cannot save Dart source: retained Designer edges have different owners");
            }
        }
        return unsavedHistoryOwner;
    }

    /**
     * Retires only an edge-less, clean semantic cursor whose command owner can
     * no longer grant a Source-anchor lease. Durable bytes and native Source
     * history remain untouched; any non-baseline or still-referenced authority
     * is rejected instead of being guessed away.
     */
    private boolean retireClosedEdgeLessHistoryOwner(
            DesignerCommandSessionOrchestrator expectedOwner) {
        if (expectedOwner.retainsOpenHistoryAuthority()) {
            return false;
        }
        synchronized (this) {
            return retireClosedEdgeLessHistoryOwnerLocked(expectedOwner);
        }
    }

    /** Called only under this coordinator monitor. */
    private boolean retireClosedEdgeLessHistoryOwnerLocked(
            DesignerCommandSessionOrchestrator expectedOwner) {
        if (unsavedHistoryOwner != expectedOwner
                || !unsavedPairHistory.isEmpty()
                || staged != null
                || preparation != null
                || replacement != null
                || historyTransition != null
                || forwardAdmission != null
                || activePairSave != null
                || activeSourceSave != null
                || activeFdOnlySave != null) {
            return false;
        }
        if (unsavedHistoryCursor != null) {
            HistoryEndpoint endpoint = unsavedHistoryCursor.endpoint();
            if (unsavedHistoryCursor.stagedProof() != null
                    || endpoint.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                    || controller.state() != endpoint.currentIdentity()
                    || diskBaseline == null
                    || !Arrays.equals(
                            diskBaseline.dartBytes(),
                            endpoint.baselineDartBytes())
                    || !Arrays.equals(
                            diskBaseline.fdBytes(),
                            endpoint.baselineFdBytes())) {
                return false;
            }
        }
        unsavedHistoryCursor = null;
        unsavedHistoryOwner = null;
        return true;
    }

    /**
     * Plans and activates a Source save above retained semantic history before
     * CES may move its private savepoint.
     */
    private boolean saveSourceWithHistory(
            DesignerCommandSessionOrchestrator commandOwner)
            throws IOException {
        SourceSaveCandidate candidate = captureHistorySourceCandidate();
        if (retireClosedEdgeLessHistoryOwner(commandOwner)) {
            return false;
        }
        sourceHistoryOwnerSelectionHook.afterSelection(commandOwner);
        DesignerCommandSessionOrchestrator.SourceAnchorLease lease;
        try {
            lease = commandOwner.beginSourceAnchor(
                    candidate.serializedDartBytes());
        } catch (RuntimeException planningFailure) {
            if (retireClosedEdgeLessHistoryOwner(commandOwner)) {
                return false;
            }
            throw new IOException(
                    "Cannot pin the clean Designer revision for exact Source Save: "
                    + reason(planningFailure), planningFailure);
        }

        SourceHistoryReanchorPlan plan;
        try {
            plan = prepareSourceHistoryReanchorPlan(
                    commandOwner, lease, candidate);
            rejectForeignSaveCookie();
        } catch (IOException | RuntimeException planningFailure) {
            IOException wrapped = asIOException(planningFailure);
            abortSourceAnchorSafely(lease, wrapped);
            throw wrapped;
        }

        Thread saveThread = Thread.currentThread();
        StateChange[] activatedChange = new StateChange[1];
        ActiveSourceSave attempt;
        try {
            attempt = onEdt(() -> {
                LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
                boolean editorDirty = editor.sourceModified();
                synchronized (PairSaveCoordinator.this) {
                    if (!fresh.sameEvidence(candidate.liveIdentity())
                            || editorDirty != candidate.editorDirty()
                            || !sourceDirty
                            || sourceStateEpoch != plan.sourceStateEpoch()
                            || epoch != plan.coordinatorEpoch()
                            || externalEventEpoch != plan.eventEpoch()
                            || diskBaseline != plan.priorBaselineIdentity()
                            || unsavedHistoryCursor
                                != plan.priorCursorIdentity()
                            || controller.state()
                                != plan.priorCurrentIdentity()
                            || preparation != null
                            || replacement != null
                            || staged != null
                            || historyTransition != null
                            || activeSourceSave != null
                            || activePairSave != null
                            || activeFdOnlySave != null
                            || !lease.ownsExactActiveRevision()
                            || lease.revision()
                                != plan.priorCursorIdentity()
                                        .endpoint().revision()
                            || !retainsExactSourceHistoryPlanLocked(plan)) {
                        throw new IOException(
                                "Cannot start Source Save: its exact live, command or history authority changed during planning");
                    }
                    ActiveSourceSave activated = new ActiveSourceSave(
                            plan.priorBaselineIdentity(),
                            saveThread,
                            plan.eventEpoch(),
                            plan,
                            null);
                    activeSourceSave = activated;
                    activatedChange[0] = transitionLocked(
                            PairSaveCoordinatorStatus.SAVING_SOURCE, null);
                    return activated;
                }
            });
        } catch (IOException | RuntimeException activationFailure) {
            IOException wrapped = asIOException(activationFailure);
            abortSourceAnchorSafely(lease, wrapped);
            throw wrapped;
        }
        publishEffects(activatedChange[0]);
        saveSourceOnly(attempt);
        return true;
    }

    /**
     * Saves a Designer-owned dirty BASELINE cursor whose physical bytes already
     * equal the durable pair. CES must still establish its native savepoint,
     * while the command session must durably adopt the new semantic revision.
     */
    private boolean saveSemanticBaselineWithHistory(
            DesignerCommandSessionOrchestrator commandOwner)
            throws IOException {
        SourceSaveCandidate candidate = captureHistorySourceCandidate();
        if (retireClosedEdgeLessHistoryOwner(commandOwner)) {
            return false;
        }
        sourceHistoryOwnerSelectionHook.afterSelection(commandOwner);

        DesignerCommandSessionOrchestrator.DurableSaveLease lease;
        try {
            boolean metadata;
            synchronized (this) {
                metadata = unsavedHistoryCursor != null
                        && unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint;
            }
            lease = metadata ? commandOwner.beginMetadataDurableSave(candidate.serializedDartBytes())
                    : commandOwner.beginDurableSave();
        } catch (RuntimeException planningFailure) {
            if (retireClosedEdgeLessHistoryOwner(commandOwner)) {
                return false;
            }
            throw new IOException(
                    "Cannot pin the dirty semantic BASELINE revision for exact Save: "
                    + reason(planningFailure), planningFailure);
        }

        SemanticBaselineHistorySavePlan plan;
        try {
            plan = prepareSemanticBaselineHistorySavePlan(
                    commandOwner, lease, candidate);
            rejectForeignSaveCookie();
        } catch (IOException | RuntimeException planningFailure) {
            IOException wrapped = asIOException(planningFailure);
            abortLeaseSafely(lease, wrapped);
            throw wrapped;
        }

        Thread saveThread = Thread.currentThread();
        StateChange[] activatedChange = new StateChange[1];
        ActiveSourceSave attempt;
        try {
            attempt = onEdt(() -> {
                LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
                boolean editorDirty = editor.sourceModified();
                synchronized (PairSaveCoordinator.this) {
                    if (!fresh.sameEvidence(candidate.liveIdentity())
                            || editorDirty != candidate.editorDirty()
                            || !sourceDirty && !(plan.priorCursorIdentity().endpoint() instanceof MetadataHistoryEndpoint)
                            || sourceStateEpoch != plan.sourceStateEpoch()
                            || epoch != plan.coordinatorEpoch()
                            || externalEventEpoch != plan.eventEpoch()
                            || diskBaseline != plan.priorBaselineIdentity()
                            || unsavedHistoryCursor
                                != plan.priorCursorIdentity()
                            || unsavedHistoryOwner != commandOwner
                            || controller.state()
                                != plan.priorCurrentIdentity()
                            || preparation != null
                            || replacement != null
                            || staged != null
                            || historyTransition != null
                            || activeSourceSave != null
                            || activePairSave != null
                            || activeFdOnlySave != null
                            || !lease.ownsExactActiveRevision()
                            || lease.revision()
                                != plan.priorCursorIdentity()
                                        .endpoint().revision()
                            || !retainsExactSemanticBaselinePlanLocked(plan)) {
                        throw new IOException(
                                "Cannot start semantic BASELINE Save: its exact live, command or history authority changed during planning");
                    }
                    ActiveSourceSave activated = new ActiveSourceSave(
                            plan.priorBaselineIdentity(),
                            saveThread,
                            plan.eventEpoch(),
                            null,
                            plan);
                    activeSourceSave = activated;
                    activatedChange[0] = transitionLocked(
                            PairSaveCoordinatorStatus.SAVING_SOURCE, null);
                    return activated;
                }
            });
        } catch (IOException | RuntimeException activationFailure) {
            IOException wrapped = asIOException(activationFailure);
            abortLeaseSafely(lease, wrapped);
            throw wrapped;
        }
        publishEffects(activatedChange[0]);
        if (!candidate.editorDirty()
                && plan.priorCursorIdentity().endpoint() instanceof MetadataHistoryEndpoint) {
            // Explicit Save of metadata must not manufacture a source edit or
            // advance CES's source savepoint. Retain the same native history.
            try {
                if (!Arrays.equals(plan.priorBaselineIdentity().dartBytes(),
                        plan.savedBaseline().dartBytes())) {
                    throw new IOException("Clean-source metadata Save cannot replace a Source envelope");
                }
                attempt.result = fdOnlyTransaction.commit(new PairFileTransactionRequest(
                        dartFile, designerFile, plan.priorBaselineIdentity().dartBytes(),
                        plan.priorBaselineIdentity().fdBytes(), plan.savedBaseline().dartBytes(),
                        plan.savedBaseline().fdBytes()));
                attempt.committedDart = plan.savedBaseline().dartBytes();
                if (!acceptsFdOnlyTransaction(attempt.result)) {
                    throw new IOException("Retained metadata Save did not commit exactly one .fd write");
                }
                finishCommittedSemanticBaselineSource(attempt, null);
            } catch (IOException | RuntimeException failure) {
                finishFailedSource(attempt, attempt.result, failure);
                throw asIOException(failure);
            }
        } else {
            saveSourceOnly(attempt);
        }
        return true;
    }

    private void saveSourceOnly(ActiveSourceSave attempt) throws IOException {
        Throwable failure = null;
        try {
            attempt.cesEntered = true;
            editor.saveDocumentThroughNetBeans();
        } catch (IOException | RuntimeException caught) {
            failure = caught;
        }
        PairFileTransactionResult result = attempt.result;
        boolean committed = acceptsSourceTransaction(result, failure);
        if (committed) {
            if (attempt.semanticBaselinePlan != null) {
                if (result.status()
                        == PairFileTransactionStatus.UNCHANGED) {
                    finishUnchangedSemanticBaselineSource(attempt);
                } else {
                    finishCommittedSemanticBaselineSource(attempt, failure);
                }
                return;
            }
            if (attempt.historyPlan != null
                    && result.status()
                        == PairFileTransactionStatus.UNCHANGED) {
                finishUnchangedHistorySource(attempt);
                return;
            }
            finishCommittedSource(attempt, failure);
            return;
        }
        if (failure == null) {
            failure = new IOException(
                    "The source serializer did not publish one exact transaction result");
        }
        finishFailedSource(attempt, result, failure);
        throw asIOException(failure);
    }

    private void finishCommittedSource(
            ActiveSourceSave attempt, Throwable postCommitFailure) {
        if (attempt.historyPlan != null) {
            finishCommittedHistorySource(attempt, postCommitFailure);
            return;
        }
        IOException recoveryConflict = postCommitRecoveryConflict(
                "Dart source", postCommitFailure, attempt.committedDart);
        StateChange change;
        synchronized (this) {
            if (activeSourceSave != attempt) {
                LOGGER.warning("The committed source-save attempt was no longer active");
            }
            activeSourceSave = null;
            failedSavePending = recoveryConflict != null;
            sourceDirty = editor.sourceModified();
            if (externalEventEpoch != attempt.eventTicket) {
                diskBaseline = null;
                change = state.status() == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? null
                        : transitionLocked(
                                PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                                "A paired file changed immediately after the exact "
                                + "Dart source transaction committed");
            } else if (recoveryConflict != null) {
                diskBaseline = new DiskBaseline(
                        attempt.committedDart, attempt.baseline.fdBytes());
                change = transitionLocked(
                        PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        recoveryConflict.getMessage());
            } else {
                diskBaseline = new DiskBaseline(
                        attempt.committedDart, attempt.baseline.fdBytes());
                change = transitionLocked(
                        sourceDirty
                                ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                : PairSaveCoordinatorStatus.CLEAN,
                        null);
            }
        }
        publishEffects(change);
        if (postCommitFailure != null) {
            Level level = recoveryConflict == null ? Level.FINE : Level.WARNING;
            LOGGER.log(level,
                    recoveryConflict == null
                            ? "Dart bytes committed and the exact clean live revision "
                                    + "was recovered in place after editor finalization failed"
                            : "Dart bytes committed, but the newer or unverified live "
                                    + "revision was retained in recovery conflict",
                    postCommitFailure);
        }
        reloadPresentation();
    }

    private void finishCommittedHistorySource(
            ActiveSourceSave attempt, Throwable postCommitFailure) {
        SourceHistoryReanchorPlan plan = attempt.historyPlan;
        IOException recoveryConflict = postCommitRecoveryConflict(
                "Dart source", postCommitFailure, attempt.committedDart);
        CommittedLiveState committedLive = null;
        if (recoveryConflict == null) {
            try {
                committedLive = onEdt(() -> {
                    LiveDartDocumentSnapshot snapshot =
                            editor.verifyCommittedPairWithoutHistoryMutation(
                                    plan.candidate().liveIdentity(),
                                    LiveDartDocumentBridge
                                            .CommittedPairContentPolicy
                                            .EXACT_MANAGED_CONTENT);
                    boolean editorDirty = editor.sourceModified();
                    synchronized (PairSaveCoordinator.this) {
                        return new CommittedLiveState(
                                snapshot,
                                editorDirty,
                                sourceDirty,
                                sourceStateEpoch);
                    }
                });
            } catch (IOException verificationFailure) {
                recoveryConflict = verificationFailure;
                LOGGER.log(Level.WARNING,
                        "The Source bytes committed, but the live Dart revision "
                        + "could not be verified without changing native history",
                        verificationFailure);
            }
        }
        if (recoveryConflict == null) {
            boolean fullMatch = Arrays.equals(
                    committedLive.snapshot().markerBearingUtf8(),
                    attempt.committedDart);
            if (committedLive.editorDirty()
                    != committedLive.coordinatorDirty()) {
                recoveryConflict = new IOException(
                        "The Source bytes committed, but CES and the Pair coordinator disagree about the savepoint");
            } else if (fullMatch == committedLive.editorDirty()) {
                recoveryConflict = new IOException(fullMatch
                        ? "The exact committed Source bytes remain dirty in CES"
                        : "Newer live Source bytes exist while CES reports a clean savepoint");
            } else if (committedLive.snapshot().documentIdentity()
                    != plan.candidate().liveIdentity().documentIdentity()) {
                recoveryConflict = new IOException(
                        "The live Dart document identity changed before Source-history adoption");
            }
        }

        CommittedLiveState exactCommittedLive = committedLive;
        var terminalConflict = new java.util.concurrent.atomic.AtomicReference<
                IOException>(recoveryConflict);
        var change = new java.util.concurrent.atomic.AtomicReference<
                StateChange>();
        var controllerEffects = new java.util.concurrent.atomic.AtomicReference<
                FlutterDesignerDocumentController.DeferredCurrentEffects>();
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                null;
        synchronized (this) {
            if (terminalConflict.get() == null
                    && (activeSourceSave != attempt
                    || attempt.historyPlan != plan
                    || !plan.leaseIdentity().ownsExactActiveRevision()
                    || plan.leaseIdentity().revision()
                        != plan.priorCursorIdentity()
                                .endpoint().revision()
                    || epoch != plan.coordinatorEpoch() + 1
                    || state.status()
                        != PairSaveCoordinatorStatus.SAVING_SOURCE
                    || externalEventEpoch != plan.eventEpoch()
                    || diskBaseline != plan.priorBaselineIdentity()
                    || unsavedHistoryCursor
                        != plan.priorCursorIdentity()
                    || controller.state()
                        != plan.priorCurrentIdentity()
                    || !retainsExactSourceHistoryPlanLocked(plan)
                    || !Arrays.equals(
                            attempt.committedDart,
                            plan.savedBaseline().dartBytes()))) {
                terminalConflict.set(new IOException(
                        "The Source bytes committed, but exact command, model or history authority changed before adoption"));
            }
            if (terminalConflict.get() == null
                    && (sourceStateEpoch
                            != exactCommittedLive.sourceStateEpoch()
                        || sourceDirty
                            != exactCommittedLive.coordinatorDirty()
                        || editor.sourceModified()
                            != exactCommittedLive.editorDirty())) {
                terminalConflict.set(new IOException(
                        "The live Source savepoint changed after verification and before joint adoption"));
            }
            if (terminalConflict.get() != null) {
                activeSourceSave = null;
                staged = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = externalEventEpoch == plan.eventEpoch()
                        ? plan.savedBaseline() : null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                PairSaveCoordinatorStatus conflictStatus =
                        externalEventEpoch != plan.eventEpoch()
                                || state.status()
                                    == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change.set(state.status() == conflictStatus
                        ? null
                        : transitionLocked(
                                conflictStatus,
                                terminalConflict.get().getMessage()));
            } else {
                try {
                    commandEffects = plan.leaseIdentity()
                            .adoptCommittedCloseAwareDeferredEffects(
                                    ownerClosePending -> {
                                controllerEffects.set(
                                        controller.adoptCurrentDeferred(
                                                plan.adoptionTicket(),
                                                plan.savedCurrentIdentity()));
                                activeSourceSave = null;
                                staged = null;
                                sourceDirty = exactCommittedLive.editorDirty();
                                diskBaseline = plan.savedBaseline();
                                if (ownerClosePending) {
                                    failedSavePending = true;
                                    unsavedPairHistory.clear();
                                    unsavedHistoryCursor = null;
                                    unsavedHistoryOwner = null;
                                    change.set(transitionLocked(
                                            PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                                            "The Source bytes committed while the Designer command-session owner was closing"));
                                } else {
                                    failedSavePending = false;
                                    unsavedPairHistory.clear();
                                    unsavedPairHistory.putAll(
                                            plan.reanchoredEdges());
                                    unsavedHistoryCursor =
                                            plan.reanchoredCursor();
                                    change.set(transitionLocked(
                                            sourceDirty
                                                ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                                : PairSaveCoordinatorStatus.CLEAN,
                                            null));
                                }
                            });
                } catch (RuntimeException adoptionFailure) {
                    terminalConflict.set(new IOException(
                            "The Source bytes committed, but saved command/model/history state could not be adopted atomically: "
                            + reason(adoptionFailure), adoptionFailure));
                    activeSourceSave = null;
                    staged = null;
                    failedSavePending = true;
                    sourceDirty = editor.sourceModified();
                    diskBaseline = externalEventEpoch == plan.eventEpoch()
                            ? plan.savedBaseline() : null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    change.set(state.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            ? null
                            : transitionLocked(
                                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                                    terminalConflict.get().getMessage()));
                }
            }
        }
        if (terminalConflict.get() != null) {
            invalidateSourceAnchorSafely(
                    plan.leaseIdentity(), terminalConflict.get());
            publishEffects(change.get());
            LOGGER.log(Level.WARNING,
                    terminalConflict.get().getMessage(),
                    terminalConflict.get());
        } else {
            publishCommittedPairEffects(
                    controllerEffects.get(), commandEffects, change.get());
        }
        if (postCommitFailure != null) {
            LOGGER.log(
                    terminalConflict.get() == null ? Level.FINE : Level.WARNING,
                    terminalConflict.get() == null
                            ? "Dart Source bytes committed and exact saved history was re-anchored"
                            : "Dart Source bytes committed, but exact saved history entered recovery conflict",
                    postCommitFailure);
        }
    }

    /**
     * Resolves a history-aware Source save whose exact Dart/.fd candidates
     * already equal disk. CES has still moved its private native savepoint, so
     * only the live cursor evidence is refreshed. The command session,
     * controller Current, semantic revisions and retained edges remain the
     * exact pre-save identities.
     */
    private void finishUnchangedHistorySource(ActiveSourceSave attempt) {
        SourceHistoryReanchorPlan plan = attempt.historyPlan;
        IOException verificationConflict = null;
        CommittedLiveState unchangedLive = null;
        try {
            unchangedLive = onEdt(() -> {
                LiveDartDocumentSnapshot snapshot =
                        editor.verifyCommittedPairWithoutHistoryMutation(
                                plan.candidate().liveIdentity(),
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_FULL_CONTENT);
                boolean editorDirty = editor.sourceModified();
                synchronized (PairSaveCoordinator.this) {
                    return new CommittedLiveState(
                            snapshot,
                            editorDirty,
                            sourceDirty,
                            sourceStateEpoch);
                }
            });
        } catch (IOException verificationFailure) {
            verificationConflict = verificationFailure;
            LOGGER.log(Level.WARNING,
                    "The unchanged Source savepoint could not be verified "
                    + "without changing native history",
                    verificationFailure);
        }
        if (verificationConflict == null) {
            boolean exactCandidate = Arrays.equals(
                    unchangedLive.snapshot().markerBearingUtf8(),
                    plan.candidate().serializedDartBytes());
            boolean exactDurableBaseline = Arrays.equals(
                    plan.candidate().serializedDartBytes(),
                    plan.priorBaselineIdentity().dartBytes())
                    && Arrays.equals(
                            plan.savedBaseline().dartBytes(),
                            plan.priorBaselineIdentity().dartBytes())
                    && Arrays.equals(
                            plan.savedBaseline().fdBytes(),
                            plan.priorBaselineIdentity().fdBytes());
            if (!exactCandidate || !exactDurableBaseline) {
                verificationConflict = new IOException(
                        "The Source transaction reported UNCHANGED without one exact live and durable byte identity");
            } else if (!plan.candidate().liveIdentity()
                    .sameEvidence(unchangedLive.snapshot())) {
                verificationConflict = new IOException(
                        "The live Dart revision changed while the unchanged Source savepoint was being established");
            } else if (unchangedLive.editorDirty()
                    || unchangedLive.coordinatorDirty()) {
                verificationConflict = new IOException(
                        "The Source transaction reported UNCHANGED, but CES did not establish one exact clean savepoint");
            } else if (unchangedLive.sourceStateEpoch()
                    != plan.sourceStateEpoch() + 1) {
                verificationConflict = new IOException(
                        "The Source modified-state epoch changed unexpectedly while establishing the unchanged savepoint");
            }
        }

        CommittedLiveState exactUnchangedLive = unchangedLive;
        var terminalConflict = new java.util.concurrent.atomic.AtomicReference<
                IOException>(verificationConflict);
        var change = new java.util.concurrent.atomic.AtomicReference<
                StateChange>();
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                null;
        synchronized (this) {
            PairFileTransactionResult result = attempt.result;
            if (terminalConflict.get() == null
                    && (activeSourceSave != attempt
                    || attempt.historyPlan != plan
                    || result == null
                    || result.status()
                        != PairFileTransactionStatus.UNCHANGED
                    || result.forwardWriteAttempts() != 0
                    || result.rollbackAttempted()
                    || !plan.leaseIdentity().ownsExactActiveRevision()
                    || plan.leaseIdentity().revision()
                        != plan.priorCursorIdentity()
                                .endpoint().revision()
                    || epoch != plan.coordinatorEpoch() + 1
                    || state.status()
                        != PairSaveCoordinatorStatus.SAVING_SOURCE
                    || externalEventEpoch != plan.eventEpoch()
                    || diskBaseline != plan.priorBaselineIdentity()
                    || unsavedHistoryCursor
                        != plan.priorCursorIdentity()
                    || controller.state()
                        != plan.priorCurrentIdentity()
                    || !retainsExactSourceHistoryPlanLocked(plan)
                    || !Arrays.equals(
                            attempt.committedDart,
                            plan.candidate().serializedDartBytes())
                    || !Arrays.equals(
                            plan.priorCursorIdentity()
                                    .endpoint().dartBytes(),
                            plan.candidate().serializedDartBytes()))) {
                terminalConflict.set(new IOException(
                        "The Source transaction reported UNCHANGED, but exact command, model, disk or history authority changed before completion"));
            }
            if (terminalConflict.get() == null
                    && (sourceStateEpoch
                            != exactUnchangedLive.sourceStateEpoch()
                        || sourceDirty
                            != exactUnchangedLive.coordinatorDirty()
                        || editor.sourceModified()
                            != exactUnchangedLive.editorDirty())) {
                terminalConflict.set(new IOException(
                        "The unchanged Source savepoint changed after verification and before joint completion"));
            }
            if (terminalConflict.get() != null) {
                activeSourceSave = null;
                staged = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = externalEventEpoch == plan.eventEpoch()
                        ? plan.priorBaselineIdentity() : null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                PairSaveCoordinatorStatus conflictStatus =
                        externalEventEpoch != plan.eventEpoch()
                                || state.status()
                                    == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change.set(state.status() == conflictStatus
                        ? null
                        : transitionLocked(
                                conflictStatus,
                                terminalConflict.get().getMessage()));
            } else {
                try {
                    commandEffects = plan.leaseIdentity()
                            .abortUnchangedCloseAwareDeferredEffects(
                                    ownerClosePending -> {
                                activeSourceSave = null;
                                staged = null;
                                sourceDirty = false;
                                diskBaseline = plan.priorBaselineIdentity();
                                if (ownerClosePending) {
                                    failedSavePending = true;
                                    unsavedPairHistory.clear();
                                    unsavedHistoryCursor = null;
                                    unsavedHistoryOwner = null;
                                    change.set(transitionLocked(
                                            PairSaveCoordinatorStatus
                                                    .RECOVERY_CONFLICT,
                                            "The unchanged Source savepoint completed while the Designer command-session owner was closing"));
                                } else {
                                    failedSavePending = false;
                                    unsavedHistoryCursor =
                                            new UnsavedPairHistoryCursor(
                                                    plan.priorCursorIdentity()
                                                            .endpoint(),
                                                    exactUnchangedLive
                                                            .snapshot(),
                                                    (StagedPairProof) null);
                                    change.set(transitionLocked(
                                            PairSaveCoordinatorStatus.CLEAN,
                                            null));
                                }
                            });
                } catch (RuntimeException completionFailure) {
                    terminalConflict.set(new IOException(
                            "The unchanged Source savepoint could not retain its exact command/history authority: "
                            + reason(completionFailure), completionFailure));
                    activeSourceSave = null;
                    staged = null;
                    failedSavePending = true;
                    sourceDirty = editor.sourceModified();
                    diskBaseline = externalEventEpoch == plan.eventEpoch()
                            ? plan.priorBaselineIdentity() : null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    change.set(state.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            ? null
                            : transitionLocked(
                                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                                    terminalConflict.get().getMessage()));
                }
            }
        }
        if (terminalConflict.get() != null) {
            invalidateSourceAnchorSafely(
                    plan.leaseIdentity(), terminalConflict.get());
            publishEffects(change.get());
            LOGGER.log(Level.WARNING,
                    terminalConflict.get().getMessage(),
                    terminalConflict.get());
        } else {
            publishEffects(change.get(), commandEffects);
        }
    }

    /**
     * Adopts a historical semantic BASELINE whose exact Source-overlay bytes
     * have become the new durable Dart anchor. The .fd bytes are unchanged,
     * but the command session, loaded Current and every retained semantic edge
     * must be re-anchored in the same monitor-only peer commit.
     */
    private void finishCommittedSemanticBaselineSource(
            ActiveSourceSave attempt, Throwable postCommitFailure) {
        SemanticBaselineHistorySavePlan plan =
                attempt.semanticBaselinePlan;
        IOException recoveryConflict = postCommitRecoveryConflict(
                "historical semantic BASELINE source",
                postCommitFailure,
                attempt.committedDart);
        CommittedLiveState committedLive = null;
        if (recoveryConflict == null) {
            try {
                committedLive = onEdt(() -> {
                    LiveDartDocumentSnapshot snapshot =
                            editor.verifyCommittedPairWithoutHistoryMutation(
                                    plan.candidate().liveIdentity(),
                                    LiveDartDocumentBridge
                                            .CommittedPairContentPolicy
                                            .EXACT_MANAGED_CONTENT);
                    boolean editorDirty = editor.sourceModified();
                    synchronized (PairSaveCoordinator.this) {
                        return new CommittedLiveState(
                                snapshot,
                                editorDirty,
                                sourceDirty,
                                sourceStateEpoch);
                    }
                });
            } catch (IOException verificationFailure) {
                recoveryConflict = verificationFailure;
                LOGGER.log(Level.WARNING,
                        "The historical semantic BASELINE bytes committed, "
                        + "but the live Dart revision could not be verified "
                        + "without changing native history",
                        verificationFailure);
            }
        }
        if (recoveryConflict == null) {
            boolean fullMatch = Arrays.equals(
                    committedLive.snapshot().markerBearingUtf8(),
                    attempt.committedDart);
            if (committedLive.editorDirty()
                    != committedLive.coordinatorDirty()) {
                recoveryConflict = new IOException(
                        "The historical semantic BASELINE bytes committed, but CES and the Pair coordinator disagree about the savepoint");
            } else if (fullMatch == committedLive.editorDirty()) {
                recoveryConflict = new IOException(fullMatch
                        ? "The exact committed historical BASELINE bytes remain dirty in CES"
                        : "Newer live Source bytes exist while CES reports a clean savepoint");
            } else if (committedLive.snapshot().documentIdentity()
                    != plan.candidate().liveIdentity().documentIdentity()) {
                recoveryConflict = new IOException(
                        "The live Dart document identity changed before historical BASELINE adoption");
            }
        }

        CommittedLiveState exactCommittedLive = committedLive;
        var terminalConflict = new java.util.concurrent.atomic.AtomicReference<
                IOException>(recoveryConflict);
        var change = new java.util.concurrent.atomic.AtomicReference<
                StateChange>();
        var controllerEffects = new java.util.concurrent.atomic.AtomicReference<
                FlutterDesignerDocumentController.DeferredCurrentEffects>();
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                null;
        synchronized (this) {
            PairFileTransactionResult result = attempt.result;
            if (terminalConflict.get() == null
                    && (activeSourceSave != attempt
                    || attempt.semanticBaselinePlan != plan
                    || result == null
                    || result.status()
                        != PairFileTransactionStatus.COMMITTED
                    || !plan.leaseIdentity().ownsExactActiveRevision()
                    || plan.leaseIdentity().revision()
                        != plan.priorCursorIdentity()
                                .endpoint().revision()
                    || epoch != plan.coordinatorEpoch() + 1
                    || state.status()
                        != PairSaveCoordinatorStatus.SAVING_SOURCE
                    || externalEventEpoch != plan.eventEpoch()
                    || diskBaseline != plan.priorBaselineIdentity()
                    || unsavedHistoryCursor
                        != plan.priorCursorIdentity()
                    || controller.state()
                        != plan.priorCurrentIdentity()
                    || !retainsExactSemanticBaselinePlanLocked(plan)
                    || !Arrays.equals(
                            attempt.committedDart,
                            plan.savedBaseline().dartBytes()))) {
                terminalConflict.set(new IOException(
                        "The historical semantic BASELINE bytes committed, but exact command, model or history authority changed before adoption"));
            }
            if (terminalConflict.get() == null
                    && (sourceStateEpoch
                            != exactCommittedLive.sourceStateEpoch()
                        || sourceDirty
                            != exactCommittedLive.coordinatorDirty()
                        || editor.sourceModified()
                            != exactCommittedLive.editorDirty())) {
                terminalConflict.set(new IOException(
                        "The historical semantic BASELINE savepoint changed after verification and before joint adoption"));
            }
            if (terminalConflict.get() != null) {
                activeSourceSave = null;
                staged = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = externalEventEpoch == plan.eventEpoch()
                        ? plan.savedBaseline() : null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                PairSaveCoordinatorStatus conflictStatus =
                        externalEventEpoch != plan.eventEpoch()
                                || state.status()
                                    == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change.set(state.status() == conflictStatus
                        ? null
                        : transitionLocked(
                                conflictStatus,
                                terminalConflict.get().getMessage()));
            } else {
                try {
                    commandEffects = plan.leaseIdentity()
                            .adoptCommittedCloseAwareDeferredEffects(
                                    ownerClosePending -> {
                                controllerEffects.set(
                                        controller.adoptCurrentDeferred(
                                                plan.adoptionTicket(),
                                                plan.savedCurrentIdentity()));
                                activeSourceSave = null;
                                staged = null;
                                sourceDirty = exactCommittedLive.editorDirty();
                                diskBaseline = plan.savedBaseline();
                                if (ownerClosePending) {
                                    failedSavePending = true;
                                    unsavedPairHistory.clear();
                                    unsavedHistoryCursor = null;
                                    unsavedHistoryOwner = null;
                                    change.set(transitionLocked(
                                            PairSaveCoordinatorStatus
                                                    .RECOVERY_CONFLICT,
                                            "The historical semantic BASELINE committed while the Designer command-session owner was closing"));
                                } else {
                                    failedSavePending = false;
                                    unsavedPairHistory.clear();
                                    unsavedPairHistory.putAll(
                                            plan.reanchoredEdges());
                                    unsavedHistoryCursor = plan.savedCursor();
                                    change.set(transitionLocked(
                                            sourceDirty
                                                ? PairSaveCoordinatorStatus
                                                        .DIRTY_SOURCE
                                                : PairSaveCoordinatorStatus
                                                        .CLEAN,
                                            null));
                                }
                            });
                } catch (RuntimeException adoptionFailure) {
                    terminalConflict.set(new IOException(
                            "The historical semantic BASELINE committed, but saved command/model/history state could not be adopted atomically: "
                            + reason(adoptionFailure), adoptionFailure));
                    activeSourceSave = null;
                    staged = null;
                    failedSavePending = true;
                    sourceDirty = editor.sourceModified();
                    diskBaseline = externalEventEpoch == plan.eventEpoch()
                            ? plan.savedBaseline() : null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    change.set(state.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            ? null
                            : transitionLocked(
                                    PairSaveCoordinatorStatus
                                            .RECOVERY_CONFLICT,
                                    terminalConflict.get().getMessage()));
                }
            }
        }
        if (terminalConflict.get() != null) {
            invalidateLeaseSafely(
                    plan.leaseIdentity(), terminalConflict.get());
            publishEffects(change.get());
            LOGGER.log(Level.WARNING,
                    terminalConflict.get().getMessage(),
                    terminalConflict.get());
        } else {
            publishCommittedPairEffects(
                    controllerEffects.get(), commandEffects, change.get());
        }
        if (postCommitFailure != null) {
            LOGGER.log(
                    terminalConflict.get() == null ? Level.FINE : Level.WARNING,
                    terminalConflict.get() == null
                            ? "Historical semantic BASELINE bytes committed and exact saved history was re-anchored"
                            : "Historical semantic BASELINE bytes committed, but exact saved history entered recovery conflict",
                    postCommitFailure);
        }
    }

    /**
     * Completes an UNCHANGED source transaction for a new semantic revision
     * whose exact Dart/.fd outputs already equal disk. Unlike an ordinary
     * Source-anchor revert, the dirty command lease must be durably adopted.
     */
    private void finishUnchangedSemanticBaselineSource(
            ActiveSourceSave attempt) {
        SemanticBaselineHistorySavePlan plan =
                attempt.semanticBaselinePlan;
        IOException verificationConflict = null;
        CommittedLiveState unchangedLive = null;
        try {
            unchangedLive = onEdt(() -> {
                LiveDartDocumentSnapshot snapshot =
                        editor.verifyCommittedPairWithoutHistoryMutation(
                                plan.candidate().liveIdentity(),
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_FULL_CONTENT);
                boolean editorDirty = editor.sourceModified();
                synchronized (PairSaveCoordinator.this) {
                    return new CommittedLiveState(
                            snapshot,
                            editorDirty,
                            sourceDirty,
                            sourceStateEpoch);
                }
            });
        } catch (IOException verificationFailure) {
            verificationConflict = verificationFailure;
            LOGGER.log(Level.WARNING,
                    "The semantic BASELINE savepoint could not be verified without changing native history",
                    verificationFailure);
        }
        if (verificationConflict == null) {
            boolean exactCandidate = Arrays.equals(
                    unchangedLive.snapshot().markerBearingUtf8(),
                    plan.candidate().serializedDartBytes());
            boolean exactDurableBaseline = Arrays.equals(
                    plan.candidate().serializedDartBytes(),
                    plan.priorBaselineIdentity().dartBytes())
                    && Arrays.equals(
                            plan.savedBaseline().dartBytes(),
                            plan.priorBaselineIdentity().dartBytes())
                    && Arrays.equals(
                            plan.savedBaseline().fdBytes(),
                            plan.priorBaselineIdentity().fdBytes());
            if (!exactCandidate || !exactDurableBaseline) {
                verificationConflict = new IOException(
                        "The semantic BASELINE transaction reported UNCHANGED without exact live and durable byte identity");
            } else if (!plan.candidate().liveIdentity()
                    .sameEvidence(unchangedLive.snapshot())) {
                verificationConflict = new IOException(
                        "The live Dart revision changed while the semantic BASELINE savepoint was being established");
            } else if (unchangedLive.editorDirty()
                    || unchangedLive.coordinatorDirty()) {
                verificationConflict = new IOException(
                        "The semantic BASELINE transaction reported UNCHANGED, but CES did not establish one exact clean savepoint");
            } else if (unchangedLive.sourceStateEpoch()
                    != plan.sourceStateEpoch() + 1) {
                verificationConflict = new IOException(
                        "The Source modified-state epoch changed unexpectedly while saving the semantic BASELINE revision");
            }
        }

        CommittedLiveState exactUnchangedLive = unchangedLive;
        var terminalConflict = new java.util.concurrent.atomic.AtomicReference<
                IOException>(verificationConflict);
        var change = new java.util.concurrent.atomic.AtomicReference<
                StateChange>();
        var controllerEffects = new java.util.concurrent.atomic.AtomicReference<
                FlutterDesignerDocumentController.DeferredCurrentEffects>();
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                null;
        synchronized (this) {
            PairFileTransactionResult result = attempt.result;
            if (terminalConflict.get() == null
                    && (activeSourceSave != attempt
                    || attempt.semanticBaselinePlan != plan
                    || result == null
                    || result.status()
                        != PairFileTransactionStatus.UNCHANGED
                    || result.forwardWriteAttempts() != 0
                    || result.rollbackAttempted()
                    || !plan.leaseIdentity().ownsExactActiveRevision()
                    || plan.leaseIdentity().revision()
                        != plan.priorCursorIdentity()
                                .endpoint().revision()
                    || epoch != plan.coordinatorEpoch() + 1
                    || state.status()
                        != PairSaveCoordinatorStatus.SAVING_SOURCE
                    || externalEventEpoch != plan.eventEpoch()
                    || diskBaseline != plan.priorBaselineIdentity()
                    || unsavedHistoryCursor
                        != plan.priorCursorIdentity()
                    || controller.state()
                        != plan.priorCurrentIdentity()
                    || !retainsExactSemanticBaselinePlanLocked(plan)
                    || !Arrays.equals(
                            attempt.committedDart,
                            plan.candidate().serializedDartBytes())
                    || !Arrays.equals(
                            plan.priorCursorIdentity()
                                    .endpoint().dartBytes(),
                            plan.candidate().serializedDartBytes()))) {
                terminalConflict.set(new IOException(
                        "The semantic BASELINE transaction reported UNCHANGED, but exact command, model, disk or history authority changed before completion"));
            }
            if (terminalConflict.get() == null
                    && (sourceStateEpoch
                            != exactUnchangedLive.sourceStateEpoch()
                        || sourceDirty
                            != exactUnchangedLive.coordinatorDirty()
                        || editor.sourceModified()
                            != exactUnchangedLive.editorDirty())) {
                terminalConflict.set(new IOException(
                        "The semantic BASELINE savepoint changed after verification and before joint completion"));
            }
            if (terminalConflict.get() != null) {
                activeSourceSave = null;
                staged = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = externalEventEpoch == plan.eventEpoch()
                        ? plan.priorBaselineIdentity() : null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                PairSaveCoordinatorStatus conflictStatus =
                        externalEventEpoch != plan.eventEpoch()
                                || state.status()
                                    == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change.set(state.status() == conflictStatus
                        ? null
                        : transitionLocked(
                                conflictStatus,
                                terminalConflict.get().getMessage()));
            } else {
                UnsavedPairHistoryCursor savedCursor =
                        new UnsavedPairHistoryCursor(
                                plan.savedEndpoint(),
                                exactUnchangedLive.snapshot(),
                                (StagedPairProof) null);
                try {
                    commandEffects = plan.leaseIdentity()
                            .adoptCommittedCloseAwareDeferredEffects(
                                    ownerClosePending -> {
                                controllerEffects.set(
                                        controller.adoptCurrentDeferred(
                                                plan.adoptionTicket(),
                                                plan.savedCurrentIdentity()));
                                activeSourceSave = null;
                                staged = null;
                                failedSavePending = false;
                                sourceDirty = false;
                                diskBaseline = plan.savedBaseline();
                                if (ownerClosePending) {
                                    failedSavePending = true;
                                    unsavedPairHistory.clear();
                                    unsavedHistoryCursor = null;
                                    unsavedHistoryOwner = null;
                                    change.set(transitionLocked(
                                            PairSaveCoordinatorStatus
                                                    .RECOVERY_CONFLICT,
                                            "The semantic BASELINE savepoint completed while the Designer command-session owner was closing"));
                                    return;
                                }
                                unsavedPairHistory.clear();
                                unsavedPairHistory.putAll(
                                        plan.reanchoredEdges());
                                unsavedHistoryCursor = savedCursor;
                                change.set(transitionLocked(
                                        PairSaveCoordinatorStatus.CLEAN,
                                        null));
                            });
                } catch (RuntimeException adoptionFailure) {
                    terminalConflict.set(new IOException(
                            "The semantic BASELINE savepoint was established, but saved command/model/history state could not be adopted atomically: "
                            + reason(adoptionFailure), adoptionFailure));
                    activeSourceSave = null;
                    staged = null;
                    failedSavePending = true;
                    sourceDirty = editor.sourceModified();
                    diskBaseline = externalEventEpoch == plan.eventEpoch()
                            ? plan.priorBaselineIdentity() : null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    change.set(state.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            ? null
                            : transitionLocked(
                                    PairSaveCoordinatorStatus
                                            .RECOVERY_CONFLICT,
                                    terminalConflict.get().getMessage()));
                }
            }
        }
        if (terminalConflict.get() != null) {
            invalidateLeaseSafely(
                    plan.leaseIdentity(), terminalConflict.get());
            publishEffects(change.get());
            LOGGER.log(Level.WARNING,
                    terminalConflict.get().getMessage(),
                    terminalConflict.get());
        } else {
            publishCommittedPairEffects(
                    controllerEffects.get(), commandEffects, change.get());
        }
    }

    private void finishFailedSource(
            ActiveSourceSave attempt,
            PairFileTransactionResult result,
            Throwable failure) {
        if (attempt.semanticBaselinePlan != null) {
            finishFailedSemanticBaselineSource(attempt, result, failure);
            return;
        }
        if (attempt.historyPlan != null) {
            finishFailedHistorySource(attempt, result, failure);
            return;
        }
        StateChange change;
        synchronized (this) {
            if (activeSourceSave == attempt) {
                activeSourceSave = null;
            }
            failedSavePending = true;
            change = failureTransitionLocked(
                    result,
                    "Dart source Save failed for " + dartFile.getNameExt()
                    + ": " + reason(failure));
        }
        publishEffects(change);
    }

    private void finishFailedSemanticBaselineSource(
            ActiveSourceSave attempt,
            PairFileTransactionResult result,
            Throwable failure) {
        SemanticBaselineHistorySavePlan plan =
                attempt.semanticBaselinePlan;
        if (!attempt.cesEntered && result != null
                && (result.status() == PairFileTransactionStatus.ROLLED_BACK
                    || result.status() == PairFileTransactionStatus.FAILED
                        && result.forwardWriteAttempts() == 0 && !result.rollbackAttempted())) {
            StateChange retryable = null;
            synchronized (this) {
                if (activeSourceSave == attempt && externalEventEpoch == plan.eventEpoch()
                        && controller.state() == plan.priorCurrentIdentity()
                        && unsavedHistoryCursor == plan.priorCursorIdentity()
                        && retainsExactSemanticBaselinePlanLocked(plan)) {
                    activeSourceSave = null;
                    retryable = transitionLocked(PairSaveCoordinatorStatus.SAVE_FAILED,
                            "Metadata Save made no durable change; the exact history is retained: " + reason(failure));
                }
            }
            if (retryable != null) {
                abortLeaseSafely(plan.leaseIdentity(), failure);
                publishEffects(retryable);
                return;
            }
        }
        IOException terminal = new IOException(
                "Semantic BASELINE Save did not complete after CES entered its persistence barrier: "
                + reason(failure), failure);
        StateChange change;
        synchronized (this) {
            if (activeSourceSave == attempt) {
                activeSourceSave = null;
            }
            staged = null;
            failedSavePending = true;
            sourceDirty = editor.sourceModified();
            unsavedPairHistory.clear();
            unsavedHistoryCursor = null;
            unsavedHistoryOwner = null;
            diskBaseline = result != null
                    && result.status() == PairFileTransactionStatus.UNCHANGED
                    && externalEventEpoch == plan.eventEpoch()
                    ? plan.priorBaselineIdentity() : null;
            PairSaveCoordinatorStatus conflictStatus =
                    externalEventEpoch != plan.eventEpoch()
                            || result != null
                                && result.status()
                                    == PairFileTransactionStatus.REJECTED
                    ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                    : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
            change = state.status() == conflictStatus
                    ? null
                    : transitionLocked(conflictStatus, terminal.getMessage());
        }
        invalidateLeaseSafely(plan.leaseIdentity(), terminal);
        publishEffects(change);
    }

    private void finishFailedHistorySource(
            ActiveSourceSave attempt,
            PairFileTransactionResult result,
            Throwable failure) {
        SourceHistoryReanchorPlan plan = attempt.historyPlan;
        IOException terminal = new IOException(
                "Dart Source Save did not commit after CES entered its persistence barrier: "
                + reason(failure), failure);
        StateChange change;
        synchronized (this) {
            if (activeSourceSave == attempt) {
                activeSourceSave = null;
            }
            staged = null;
            failedSavePending = true;
            sourceDirty = editor.sourceModified();
            unsavedPairHistory.clear();
            unsavedHistoryCursor = null;
            unsavedHistoryOwner = null;
            diskBaseline = null;
            PairSaveCoordinatorStatus conflictStatus =
                    externalEventEpoch != plan.eventEpoch()
                            || result != null
                                && result.status()
                                    == PairFileTransactionStatus.REJECTED
                    ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                    : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
            change = state.status() == conflictStatus
                    ? null
                    : transitionLocked(conflictStatus, terminal.getMessage());
        }
        invalidateSourceAnchorSafely(plan.leaseIdentity(), terminal);
        publishEffects(change);
    }

    private void saveStagedPair(ActivePairSave attempt) throws IOException {
        Throwable failure = null;
        try {
            // CloneableEditorSupport establishes its private Undo savepoint
            // before it requests our output stream. Once this call begins, an
            // output failure cannot be retried safely: CES does not restore the
            // preceding savepoint even when it re-marks the document modified.
            editor.saveDocumentThroughNetBeans();
        } catch (IOException | RuntimeException caught) {
            failure = caught;
        }
        PairFileTransactionResult result = attempt.result;
        if (acceptsPairTransaction(result)) {
            finishCommittedPair(attempt, failure);
            return;
        }
        if (failure == null) {
            failure = new IOException(
                    "The pair serializer did not publish one exact transaction result");
        }
        finishFailedPair(attempt, result, failure);
        throw asIOException(failure);
    }

    private void finishCommittedPair(
            ActivePairSave attempt, Throwable postCommitFailure) {
        StagedPairProof proof = attempt.proof;
        IOException recoveryConflict = postCommitRecoveryConflict(
                "Flutter Designer pair",
                postCommitFailure,
                proof.candidateDartBytes());
        CommittedLiveState committedLive = null;
        if (recoveryConflict == null) {
            try {
                committedLive = onEdt(() -> {
                    LiveDartDocumentSnapshot snapshot =
                            editor.verifyCommittedPairWithoutHistoryMutation(
                                    proof.liveCandidateIdentity(),
                                    LiveDartDocumentBridge
                                            .CommittedPairContentPolicy
                                            .EXACT_MANAGED_CONTENT);
                    boolean editorDirty = editor.sourceModified();
                    synchronized (PairSaveCoordinator.this) {
                        return new CommittedLiveState(
                                snapshot,
                                editorDirty,
                                sourceDirty,
                                sourceStateEpoch);
                    }
                });
            } catch (IOException verificationFailure) {
                recoveryConflict = verificationFailure;
                LOGGER.log(Level.WARNING,
                        "The exact Flutter Designer pair committed, but the live "
                        + "managed Dart regions could not be verified without "
                        + "mutating native history", verificationFailure);
            }
        }
        if (recoveryConflict == null) {
            boolean fullMatch = Arrays.equals(
                    committedLive.snapshot().markerBearingUtf8(),
                    proof.candidateDartBytes());
            if (committedLive.editorDirty()
                    != committedLive.coordinatorDirty()) {
                recoveryConflict = new IOException(
                        "The Flutter Designer pair committed durably, but CES and "
                        + "the Pair coordinator disagree about the live Source "
                        + "savepoint");
            } else if (fullMatch == committedLive.editorDirty()) {
                recoveryConflict = new IOException(fullMatch
                        ? "The Flutter Designer pair committed durably, but CES "
                            + "reports the exact committed source as dirty"
                        : "The Flutter Designer pair committed durably, but "
                            + "unmanaged live Source bytes differ while CES "
                            + "reports a clean savepoint");
            } else if (committedLive.snapshot().documentIdentity()
                    != attempt.reanchorPlan.documentIdentity()) {
                recoveryConflict = new IOException(
                        "The live Dart document identity changed before saved-history adoption");
            }
        }
        CommittedLiveState exactCommittedLive = committedLive;
        UnsavedPairHistoryCursor committedCursor = null;
        if (recoveryConflict == null) {
            committedCursor = Arrays.equals(
                        exactCommittedLive.snapshot().markerBearingUtf8(),
                        attempt.reanchorPlan.savedEndpoint().dartBytes())
                    ? new UnsavedPairHistoryCursor(
                            attempt.reanchorPlan.savedEndpoint(),
                            exactCommittedLive.snapshot(),
                            (StagedPairProof) null)
                    : attempt.reanchorPlan.savedCursor();
        }
        UnsavedPairHistoryCursor exactCommittedCursor = committedCursor;
        var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects = null;
        var controllerEffects = new java.util.concurrent.atomic.AtomicReference<
                FlutterDesignerDocumentController.DeferredCurrentEffects>();
        var terminalConflict = new java.util.concurrent.atomic.AtomicReference<
                IOException>(recoveryConflict);
        synchronized (this) {
            if (terminalConflict.get() == null
                    && (activePairSave != attempt
                    || staged != attempt.authority
                    || !attempt.lease.ownsExactActiveRevision()
                    || attempt.lease.revision()
                        != attempt.authority.revision()
                    || attempt.reanchorPlan.stagedIdentity()
                        != attempt.authority
                    || attempt.reanchorPlan.leaseIdentity() != attempt.lease
                    || attempt.reanchorPlan.priorCursorIdentity()
                        != unsavedHistoryCursor
                    || attempt.reanchorPlan.documentIdentity()
                        != proof.liveCandidateIdentity().documentIdentity()
                    || attempt.reanchorPlan.priorCurrentIdentity()
                        != proof.loadedCurrentIdentity()
                    || controller.state()
                        != attempt.reanchorPlan.priorCurrentIdentity()
                    || epoch != attempt.reanchorPlan.coordinatorEpoch() + 1
                    || state.status()
                        != PairSaveCoordinatorStatus.SAVING_PAIR
                    || !retainsExactHistoryPlanLocked(attempt.reanchorPlan))) {
                terminalConflict.set(new IOException(
                        "The Flutter Designer pair committed durably, but its "
                        + "exact staged command authority changed before re-anchor"));
            }
            if (terminalConflict.get() == null
                    && externalEventEpoch != attempt.eventTicket) {
                terminalConflict.set(new IOException(
                        "A paired file changed immediately after the exact Flutter "
                        + "Designer transaction committed"));
            }
            if (terminalConflict.get() == null
                    && (sourceStateEpoch
                            != exactCommittedLive.sourceStateEpoch()
                        || sourceDirty
                            != exactCommittedLive.coordinatorDirty()
                        || editor.sourceModified()
                            != exactCommittedLive.editorDirty())) {
                terminalConflict.set(new IOException(
                        "The live Source savepoint changed after committed-pair "
                        + "verification and before joint adoption"));
            }
            if (terminalConflict.get() != null) {
                activePairSave = null;
                staged = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = externalEventEpoch == attempt.eventTicket
                        ? attempt.reanchorPlan.savedBaseline() : null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                PairSaveCoordinatorStatus conflictStatus =
                        externalEventEpoch != attempt.eventTicket
                                || state.status()
                                    == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change.set(state.status() == conflictStatus
                        ? null
                        : transitionLocked(
                                conflictStatus,
                                terminalConflict.get().getMessage()));
            } else {
                try {
                    commandEffects = attempt.lease
                            .adoptCommittedDeferredEffects(() -> {
                                controllerEffects.set(
                                        controller.adoptCurrentDeferred(
                                                attempt.reanchorPlan
                                                        .adoptionTicket(),
                                                attempt.reanchorPlan
                                                        .savedCurrentIdentity()));
                                activePairSave = null;
                                staged = null;
                                failedSavePending = false;
                                sourceDirty = exactCommittedLive.editorDirty();
                                diskBaseline = attempt.reanchorPlan
                                        .savedBaseline();
                                unsavedPairHistory.clear();
                                unsavedPairHistory.putAll(
                                        attempt.reanchorPlan.reanchoredEdges());
                                unsavedHistoryCursor = exactCommittedCursor;
                                change.set(transitionLocked(
                                        sourceDirty
                                                ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                                : PairSaveCoordinatorStatus.CLEAN,
                                        null));
                            });
                } catch (RuntimeException adoptionFailure) {
                    terminalConflict.set(new IOException(
                            "The Flutter Designer pair committed durably, but its "
                            + "saved command/model/history state could not be "
                            + "adopted atomically: " + reason(adoptionFailure),
                            adoptionFailure));
                    activePairSave = null;
                    staged = null;
                    failedSavePending = true;
                    sourceDirty = editor.sourceModified();
                    diskBaseline = null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    change.set(state.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            ? null
                            : transitionLocked(
                                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                                    terminalConflict.get().getMessage()));
                }
            }
        }
        if (terminalConflict.get() != null) {
            invalidateLeaseSafely(attempt.lease, terminalConflict.get());
            publishEffects(change.get());
            LOGGER.log(Level.WARNING,
                    terminalConflict.get().getMessage(), terminalConflict.get());
        } else {
            publishCommittedPairEffects(
                    controllerEffects.get(), commandEffects, change.get());
        }
        if (postCommitFailure != null) {
            Level level = terminalConflict.get() == null
                    ? Level.FINE : Level.WARNING;
            LOGGER.log(level,
                    terminalConflict.get() == null
                            ? "Flutter Designer pair committed and the exact clean "
                                    + "live revision was recovered in place"
                            : "Flutter Designer pair committed, but the newer or "
                                    + "unverified live revision was retained in "
                                    + "recovery conflict",
                    postCommitFailure);
        }
    }

    private boolean retainsExactHistoryPlanLocked(
            SavedHistoryReanchorPlan plan) {
        if (unsavedPairHistory.size() != plan.priorEdgesIdentity().size()) {
            return false;
        }
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : plan.priorEdgesIdentity().entrySet()) {
            if (unsavedPairHistory.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean retainsExactSourceHistoryPlanLocked(
            SourceHistoryReanchorPlan plan) {
        if (unsavedPairHistory.size()
                != plan.priorEdgesIdentity().size()) {
            return false;
        }
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : plan.priorEdgesIdentity().entrySet()) {
            if (unsavedPairHistory.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean retainsExactSemanticBaselinePlanLocked(
            SemanticBaselineHistorySavePlan plan) {
        if (unsavedPairHistory.size()
                != plan.priorEdgesIdentity().size()) {
            return false;
        }
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : plan.priorEdgesIdentity().entrySet()) {
            if (unsavedPairHistory.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void publishCommittedPairEffects(
            FlutterDesignerDocumentController.DeferredCurrentEffects
                    controllerEffects,
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects,
            StateChange change) {
        try {
            onEdt(() -> {
                try {
                    controllerEffects.publish();
                } catch (RuntimeException | Error callbackFailure) {
                    LOGGER.log(Level.WARNING,
                            "A Flutter Designer saved-Current callback failed",
                            callbackFailure);
                }
                publishEffects(change, commandEffects);
                return null;
            });
        } catch (IOException publicationFailure) {
            LOGGER.log(Level.WARNING,
                    "Cannot publish the committed Flutter Designer state on the EDT",
                    publicationFailure);
        }
    }

    /**
     * A durable transaction followed by editor finalization failure is adopted
     * only when the current live document is still clean and serializes to the
     * exact committed Dart bytes. Otherwise the live revision is retained and
     * the caller publishes a sticky recovery conflict.
     */
    private IOException postCommitRecoveryConflict(
            String operation,
            Throwable postCommitFailure,
            byte[] exactCommittedDart) {
        if (postCommitFailure == null) {
            return null;
        }
        try {
            FlutterDesignerEditorSupport.CommittedRevisionRecovery recovery =
                    onEdt(() -> editor.recoverCommittedRevisionIfCleanAndExact(
                            exactCommittedDart));
            if (recovery.recovered()) {
                return null;
            }
            return new IOException(
                    operation + " committed durably, but editor finalization "
                    + "failed and the live revision was retained: "
                    + recovery.reason(), postCommitFailure);
        } catch (IOException recoveryFailure) {
            IOException conflict = new IOException(
                    operation + " committed durably, but the live revision could "
                    + "not be proven clean and exact after editor finalization "
                    + "failed: " + reason(recoveryFailure), postCommitFailure);
            conflict.addSuppressed(recoveryFailure);
            return conflict;
        }
    }

    /**
     * A staged live revision that no longer matches its analyzed evidence
     * cannot remain retryable: its managed Dart candidate is beside the old
     * on-disk .fd model. Fail closed until an explicit reload/recovery while
     * preserving native Source history and any newer user-owned live edit.
     */
    private void invalidateStagedPair(
            StagedPairAuthority authority,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            IOException failure) {
        StateChange change = null;
        boolean invalidateLease = false;
        synchronized (this) {
            if (staged == authority && activePairSave == null) {
                staged = null;
                unsavedPairHistory.clear();
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                failedSavePending = true;
                sourceDirty = editor.sourceModified();
                diskBaseline = null;
                invalidateLease = true;
                change = transitionLocked(
                        PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        "The staged Flutter Designer candidate was invalidated "
                        + "before Save and cannot be persisted beside the old .fd "
                        + "model. Native Source content and Undo/Redo were retained "
                        + "for explicit recovery: " + reason(failure));
            }
        }
        if (invalidateLease) {
            // The semantic lease is the only unprovable authority. Native CES
            // history remains authoritative for the live Source document and
            // must not be rewritten merely to close the Designer session.
            invalidateLeaseSafely(lease, failure);
        } else {
            abortLeaseSafely(lease, failure);
        }
        publishEffects(change);
    }

    private void finishFailedPair(
            ActivePairSave attempt,
            PairFileTransactionResult result,
            Throwable failure) {
        StateChange change;
        synchronized (this) {
            if (activePairSave == attempt) {
                activePairSave = null;
            }
            // Even a zero-write or fully verified filesystem rollback is not
            // a retryable editor outcome here. CES already moved its hidden
            // Undo savepoint before opening ExactPairOutput, so retaining the
            // command/staged authority could later expose a false clean marker.
            staged = null;
            failedSavePending = true;
            diskBaseline = null;
            PairSaveCoordinatorStatus conflictStatus = result != null
                            && result.status()
                                == PairFileTransactionStatus.REJECTED
                    ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                    : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
            change = state.status() == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                    ? null
                    : transitionLocked(
                            conflictStatus,
                            "Pair Save entered NetBeans persistence but cannot "
                            + "retain a safe Undo savepoint or exact durable pair "
                            + "authority: " + (result == null
                                    ? reason(failure)
                                    : transactionReason(result)));
        }
        // Invalidate only the command lease. Native Source history is retained
        // exactly as CES left it; no extra discard/barrier is introduced here.
        invalidateLeaseSafely(attempt.lease, failure);
        publishEffects(change);
    }

    private FdOnlyEvidence verifyFdOnlyEvidence(
            FlutterDesignerDocumentState.Current expectedCurrent,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        DesignerCommandRevision revision = lease.revision();
        if (!lease.ownsExactActiveRevision()) {
            throw new IOException(
                    "Cannot save the Designer model only from an inactive "
                    + "durable session lease");
        }
        byte[] durableFd = lease.durableFdBytes();
        byte[] durableDart = lease.durableDartBytes();
        byte[] candidateFd = revision.fdBytes();
        byte[] candidateDart = revision.dartCandidateBytes();

        if (revision.persistenceKind()
                    != DesignerRevisionPersistenceKind.FD_ONLY
                || revision.sourceTransition().isPresent()
                || revision.preparedPair().isPresent()) {
            throw new IOException(
                    "Cannot save the Designer model only: the leased revision "
                    + "is not an exact FD_ONLY candidate");
        }
        if (!Arrays.equals(candidateDart, durableDart)
                || Arrays.equals(candidateFd, durableFd)) {
            throw new IOException(
                    "Cannot save the Designer model only: FD_ONLY must retain "
                    + "exact Dart bytes and change exact .fd bytes");
        }
        if (durableDart.length > MAX_SOURCE_PERSISTENCE_BYTES
                || durableFd.length > MAX_MODEL_PERSISTENCE_BYTES
                || candidateFd.length > MAX_MODEL_PERSISTENCE_BYTES) {
            throw new IOException(
                    "Cannot save the Designer model only: a durable or candidate "
                    + "snapshot exceeds the pair-persistence safety limit");
        }
        if (controller.state() != expectedCurrent) {
            throw new IOException(
                    "Cannot save the Designer model only from a stale Current identity");
        }
        if (!expectedCurrent.validation().valid()
                || !expectedCurrent.catalogDiagnostics().isEmpty()
                || !expectedCurrent.contextIssues().isEmpty()
                || expectedCurrent.threeWayIntegrity().isEmpty()
                || !expectedCurrent.threeWayIntegrity().orElseThrow()
                        .onDiskThreeWayMatch()) {
            throw new IOException(
                    "Cannot save the Designer model only: Current is not a "
                    + "fully validated writable three-way revision");
        }
        if (lease.catalogIdentity() != expectedCurrent.catalog()) {
            throw new IOException(
                    "Cannot save the Designer model only: the command session "
                    + "uses a different widget-catalog identity");
        }
        if (!revision.document().source().equals(
                expectedCurrent.decoded().document().source())) {
            throw new IOException(
                    "Cannot save the Designer model only: the command revision "
                    + "changed the paired Dart source descriptor");
        }
        if (!expectedCurrent.decoded().original().contentEquals(durableFd)) {
            throw new IOException(
                    "Cannot save the Designer model only from a different .fd baseline");
        }
        if (expectedCurrent.sourceIntegrity().isEmpty()
                || !expectedCurrent.sourceIntegrity().orElseThrow()
                        .onDiskDeclaredMatch()
                || expectedCurrent.sourceIntegrity().orElseThrow()
                        .original().isEmpty()
                || !expectedCurrent.sourceIntegrity().orElseThrow()
                        .original().orElseThrow().contentEquals(durableDart)
                || !revision.sourceIntegrity().onDiskDeclaredMatch()
                || revision.sourceIntegrity().original().isEmpty()
                || !revision.sourceIntegrity().original().orElseThrow()
                        .contentEquals(durableDart)) {
            throw new IOException(
                    "Cannot save the Designer model only from a different or "
                    + "unclean Dart baseline");
        }

        synchronized (this) {
            if (sourceDirty || failedSavePending || editor.sourceModified()) {
                throw new IOException(
                        "Cannot save the Designer model only while Dart source "
                        + "persistence is pending");
            }
        }
        LiveDartDocumentSnapshot live = onEdt(editor::liveSnapshot);
        if (!Arrays.equals(live.markerBearingUtf8(), durableDart)
                || editor.sourceModified()) {
            throw new IOException(
                    "Cannot save the Designer model only: the clean live Dart "
                    + "bytes differ from the durable Dart anchor");
        }
        if (controller.state() != expectedCurrent) {
            throw new IOException(
                    "Cannot save the Designer model only: Current changed while "
                    + "the clean live Dart evidence was captured");
        }
        DesignerCommandRevision savedRevision = lease.reanchoredRevision(
                lease.savedRevisionId());
        FlutterDesignerDocumentState.Current savedCurrentIdentity = savedCurrent(
                expectedCurrent,
                savedRevision,
                lease.savedSourceIntegrityIdentity(),
                lease.savedThreeWayIntegrityIdentity());
        FlutterDesignerDocumentController.CurrentAdoptionTicket adoptionTicket =
                controller.currentAdoptionTicket(expectedCurrent);
        return new FdOnlyEvidence(
                durableDart,
                durableFd,
                candidateFd,
                live,
                savedCurrentIdentity,
                adoptionTicket);
    }

    private void finishCommittedFdOnly(ActiveFdOnlySave attempt)
            throws IOException {
        LiveDartDocumentSnapshot finalLive;
        try {
            finalLive = onEdt(editor::liveSnapshot);
        } catch (IOException | RuntimeException failure) {
            IOException conflict = new IOException(
                    "The .fd candidate committed, but the clean live Dart "
                    + "revision could not be recaptured: " + reason(failure), failure);
            finishUncertainFdOnlyFailure(attempt, conflict);
            throw conflict;
        }

        DesignerCommandSessionOrchestrator.DeferredLeaseEffects leaseEffects = null;
        var controllerEffects = new java.util.concurrent.atomic.AtomicReference<
                FlutterDesignerDocumentController.DeferredCurrentEffects>();
        var change = new java.util.concurrent.atomic.AtomicReference<StateChange>();
        IOException conflict = null;
        synchronized (this) {
            String uncertainty = fdOnlyUncertaintyLocked(attempt, finalLive);
            if (uncertainty == null) {
                try {
                    // Monitor-only: no listener, binding, CookieSet or controller
                    // callback may run while the coordinator decision is locked.
                    leaseEffects = attempt.lease
                            .adoptCommittedDeferredEffects(() -> {
                                controllerEffects.set(
                                        controller.adoptCurrentDeferred(
                                                attempt.evidence.adoptionTicket(),
                                                attempt.evidence
                                                        .savedCurrentIdentity()));
                                activeFdOnlySave = null;
                                failedSavePending = false;
                                suppressConflictSaveCookie = false;
                                diskBaseline = new DiskBaseline(
                                        attempt.evidence.durableDart,
                                        attempt.evidence.candidateFd);
                                change.set(transitionLocked(
                                        PairSaveCoordinatorStatus.CLEAN, null));
                            });
                } catch (RuntimeException adoptionFailure) {
                    uncertainty = "the saved Current and exact command-session "
                            + "lease could not be adopted atomically: "
                            + reason(adoptionFailure);
                }
            }
            if (uncertainty != null) {
                activeFdOnlySave = null;
                diskBaseline = null;
                suppressConflictSaveCookie = true;
                conflict = new IOException(
                        "The .fd candidate committed durably, but " + uncertainty);
                change.set(stickyFdOnlyConflictLocked(
                        PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        conflict.getMessage()));
            }
        }

        if (conflict != null) {
            invalidateLeaseSafely(attempt.lease, conflict);
            publishEffects(change.get());
            throw conflict;
        }
        publishCommittedPairEffects(
                controllerEffects.get(), leaseEffects, change.get());
    }

    private String fdOnlyUncertaintyLocked(
            ActiveFdOnlySave attempt,
            LiveDartDocumentSnapshot finalLive) {
        if (activeFdOnlySave != attempt) {
            return "its coordinator reservation identity changed";
        }
        if (attempt.result == null || !acceptsFdOnlyTransaction(attempt.result)) {
            return "the transaction did not publish exactly one committed .fd write";
        }
        if (externalEventEpoch != attempt.eventTicket) {
            return "a paired-file event changed its exact event epoch";
        }
        if (controller.state() != attempt.expectedCurrent) {
            return "the exact loaded Current identity changed";
        }
        if (!attempt.lease.ownsExactActiveRevision()) {
            return "the exact command-session lease was resolved before adoption";
        }
        if (sourceDirty || editor.sourceModified()
                || attempt.liveMutationObserved) {
            return "the Dart source became modified during the .fd transaction";
        }
        if (!attempt.evidence.liveIdentity.sameEvidence(finalLive)
                || !Arrays.equals(
                        finalLive.markerBearingUtf8(),
                        attempt.evidence.durableDart)) {
            return "the clean live Dart document identity, revision, or bytes changed";
        }
        return null;
    }

    private void finishFdOnlyBeforeWriteFailure(
            ActiveFdOnlySave attempt, Throwable failure) {
        boolean invalidate;
        StateChange change;
        synchronized (this) {
            invalidate = externalEventEpoch != attempt.eventTicket
                    || controller.state() != attempt.expectedCurrent
                    || conflictStatusLocked();
            if (activeFdOnlySave == attempt) {
                activeFdOnlySave = null;
            }
            if (invalidate) {
                diskBaseline = null;
                suppressConflictSaveCookie = true;
                change = stickyFdOnlyConflictLocked(
                        PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        "Designer-only Save reservation was invalidated before "
                        + "its transaction: " + reason(failure));
            } else {
                change = transitionLocked(
                        PairSaveCoordinatorStatus.SAVE_FAILED,
                        "Designer-only Save stopped before disk I/O: "
                        + reason(failure));
            }
        }
        if (invalidate) {
            invalidateLeaseSafely(attempt.lease, failure);
        } else {
            abortLeaseSafely(attempt.lease, failure);
        }
        publishEffects(change);
    }

    private void finishRejectedFdOnly(
            ActiveFdOnlySave attempt,
            PairFileTransactionResult result,
            IOException failure) {
        boolean retryable = result != null
                && (result.status() == PairFileTransactionStatus.FAILED
                        && result.forwardWriteAttempts() == 0
                        && !result.rollbackAttempted()
                    || result.status() == PairFileTransactionStatus.ROLLED_BACK
                        && result.rollbackAttempted());
        StateChange change;
        synchronized (this) {
            boolean identityUncertain = externalEventEpoch != attempt.eventTicket
                    || controller.state() != attempt.expectedCurrent
                    || conflictStatusLocked();
            retryable &= !identityUncertain;
            if (activeFdOnlySave == attempt) {
                activeFdOnlySave = null;
            }
            if (retryable) {
                change = transitionLocked(
                        PairSaveCoordinatorStatus.SAVE_FAILED,
                        "Designer-only Save failed with no durable change: "
                        + transactionReason(result));
            } else {
                diskBaseline = null;
                suppressConflictSaveCookie = true;
                PairSaveCoordinatorStatus conflictStatus = result != null
                                && result.status()
                                    == PairFileTransactionStatus.REJECTED
                        ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
                change = stickyFdOnlyConflictLocked(
                        conflictStatus,
                        "Designer-only Save cannot prove one exact durable .fd "
                        + "commit: " + (result == null
                                ? reason(failure) : transactionReason(result)));
            }
        }
        if (retryable) {
            abortLeaseSafely(attempt.lease, failure);
        } else {
            invalidateLeaseSafely(attempt.lease, failure);
        }
        publishEffects(change);
    }

    private void finishUncertainFdOnlyFailure(
            ActiveFdOnlySave attempt, IOException failure) {
        StateChange change;
        synchronized (this) {
            if (activeFdOnlySave == attempt) {
                activeFdOnlySave = null;
            }
            diskBaseline = null;
            suppressConflictSaveCookie = true;
            change = stickyFdOnlyConflictLocked(
                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    reason(failure));
        }
        invalidateLeaseSafely(attempt.lease, failure);
        publishEffects(change);
    }

    private StateChange stickyFdOnlyConflictLocked(
            PairSaveCoordinatorStatus requestedStatus, String reason) {
        if (conflictStatusLocked()) {
            return null;
        }
        return transitionLocked(requestedStatus, reason);
    }

    private static boolean acceptsFdOnlyTransaction(
            PairFileTransactionResult result) {
        return result != null
                && result.status() == PairFileTransactionStatus.COMMITTED
                && result.forwardWriteAttempts() == 1
                && !result.rollbackAttempted();
    }

    private void abortFdOnlyLeaseAfterPreflightFailure(
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            Throwable failure) {
        synchronized (this) {
            if (activeFdOnlySave != null
                    && activeFdOnlySave.lease == lease) {
                // A duplicate caller must not resolve the exact lease owned by
                // the already-running transaction.
                return;
            }
        }
        abortLeaseSafely(lease, failure);
    }

    private static void abortLeaseSafely(
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            Throwable primary) {
        try {
            lease.abort();
        } catch (RuntimeException resolutionFailure) {
            primary.addSuppressed(resolutionFailure);
        }
    }

    private static void invalidateLeaseSafely(
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            Throwable primary) {
        try {
            lease.invalidate();
        } catch (RuntimeException resolutionFailure) {
            primary.addSuppressed(resolutionFailure);
        }
    }

    private static void abortSourceAnchorSafely(
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease,
            Throwable primary) {
        try {
            lease.abort();
        } catch (RuntimeException resolutionFailure) {
            primary.addSuppressed(resolutionFailure);
        }
    }

    private static void invalidateSourceAnchorSafely(
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease,
            Throwable primary) {
        try {
            lease.invalidate();
        } catch (RuntimeException resolutionFailure) {
            primary.addSuppressed(resolutionFailure);
        }
    }

    private StateChange failureTransitionLocked(
            PairFileTransactionResult result, String fallbackReason) {
        if (state.status() == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT) {
            return null;
        }
        if (result != null
                && result.status() == PairFileTransactionStatus.RECOVERY_CONFLICT) {
            return transitionLocked(
                    PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    transactionReason(result));
        }
        if (result != null
                && result.status() == PairFileTransactionStatus.REJECTED) {
            return transitionLocked(
                    PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                    transactionReason(result));
        }
        return transitionLocked(PairSaveCoordinatorStatus.SAVE_FAILED, fallbackReason);
    }

    static boolean acceptsSourceTransaction(
            PairFileTransactionResult result, Throwable serializerFailure) {
        return result != null
                && (result.status() == PairFileTransactionStatus.COMMITTED
                    || (result.status() == PairFileTransactionStatus.UNCHANGED
                        && serializerFailure == null));
    }

    static boolean acceptsPairTransaction(PairFileTransactionResult result) {
        // A prepared pair changes at least one candidate by construction;
        // retained event methods can make the transition source-only.
        // UNCHANGED is therefore not a valid publication for this path.
        return result != null
                && result.status() == PairFileTransactionStatus.COMMITTED;
    }

    private void verifyStagedAuthority(
            StagedPairAuthority authority,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        StagedPairProof proof = authority.proof();
        DesignerCommandRevision revision = authority.revision();
        DesignerCommandRevision saved = lease.reanchoredRevision(
                lease.savedRevisionId());
        if (!lease.ownsExactActiveRevision()
                || lease.revision() != revision
                || revision.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || !proof.preparedPairIdentity().prospectiveDocument()
                        .equals(revision.document())
                || !Arrays.equals(
                        revision.fdBytes(),
                        proof.preparedPairIdentity().prospectiveFdBytes())
                || saved.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE
                || saved.revisionId() != revision.revisionId()
                || !saved.document().equals(revision.document())
                || !Arrays.equals(
                        saved.dartCandidateBytes(),
                        proof.candidateDartBytes())
                || !Arrays.equals(
                        saved.fdBytes(),
                        proof.preparedPairIdentity().prospectiveFdBytes())
                || !Arrays.equals(
                        lease.durableDartBytes(),
                        proof.baselineDartBytes())
                || !Arrays.equals(
                        lease.durableFdBytes(),
                        proof.baselineFdBytes())) {
            throw new IOException(
                    "Cannot save the Flutter Designer pair: the durable command "
                    + "lease does not retain its exact staged authority and disk anchors");
        }
    }

    private void verifyStagedStillCurrent(StagedPairProof proof)
            throws IOException {
        if (proof == null) {
            throw new IOException("The staged pair evidence is unavailable");
        }
        if (controller.state() != proof.loadedCurrentIdentity()) {
            throw new IOException(
                    "Cannot save the Flutter Designer pair: the loaded model "
                    + "revision changed after staging");
        }
        LiveDartDocumentSnapshot current = editor.liveSnapshot();
        if (!proof.liveCandidateIdentity().sameEvidence(current)) {
            throw new IOException(
                    "Cannot save the Flutter Designer pair: the live Dart "
                    + "document identity, revision or candidate changed after analysis");
        }
    }

    private boolean ownsStagedProofLocked(StagedPairProof proof) {
        return staged != null && staged.proof() == proof;
    }

    @Override
    public DesignerSemanticUndoableEdit.PreparedReplay prepare(
            DesignerSemanticUndoableEdit.Direction direction,
            long currentRevisionId,
            long targetRevisionId) throws IOException {
        Objects.requireNonNull(direction, "direction");
        refreshSavedBaselineCursorIfExact(
                direction, currentRevisionId, targetRevisionId);
        refreshPairedVariantCursorIfExact(
                direction, currentRevisionId, targetRevisionId);
        UnsavedPairHistoryEdge edge;
        HistoryEndpoint sourceEndpoint;
        HistoryEndpoint targetEndpoint;
        long reservedEventEpoch;
        long reservedCoordinatorEpoch;
        synchronized (this) {
            edge = requireHistoryEdgeLocked(
                    direction, currentRevisionId, targetRevisionId);
            sourceEndpoint = direction
                    == DesignerSemanticUndoableEdit.Direction.UNDO
                            ? edge.after() : edge.before();
            targetEndpoint = direction
                    == DesignerSemanticUndoableEdit.Direction.UNDO
                            ? edge.before() : edge.after();
            requireHistoryCursorLocked(edge, sourceEndpoint);
            reservedEventEpoch = externalEventEpoch;
            reservedCoordinatorEpoch = epoch;
        }

        DesignerCommandSessionOrchestrator.PendingTransitionAttempt attempt;
        try {
            attempt = direction == DesignerSemanticUndoableEdit.Direction.UNDO
                    ? edge.owner().beginUndoTransition()
                    : edge.owner().beginRedoTransition();
        } catch (RuntimeException | Error failure) {
            throw new IOException(
                    "Cannot derive the exact unsaved Designer history target: "
                    + reason(failure), failure);
        }
        DesignerCommandSessionOrchestrator.PendingCommandLease commandLease =
                attempt.lease().orElseThrow(() -> new IOException(
                "The Designer command cursor has no exact adjacent history target"));
        Object commandClaim = new Object();
        boolean claimed = false;
        EffectsDeferral effectsDeferral = null;
        ActiveHistoryTransition transition = null;
        try {
            commandLease.claimForTransition(commandClaim);
            claimed = true;
            effectsDeferral = deferEffects();
            synchronized (this) {
                requireHistoryCursorLocked(edge, sourceEndpoint);
                requireHistoryLeaseLocked(
                        direction,
                        commandLease,
                        edge,
                        sourceEndpoint,
                        targetEndpoint);
                if (externalEventEpoch != reservedEventEpoch
                        || epoch != reservedCoordinatorEpoch) {
                    throw new IOException(
                            "The pair epoch changed before the unsaved history edge was reserved");
                }
                transition = new ActiveHistoryTransition(
                        edge,
                        sourceEndpoint,
                        targetEndpoint,
                        commandLease,
                        commandClaim,
                        reservedEventEpoch,
                        reservedCoordinatorEpoch,
                        unsavedHistoryCursor,
                        effectsDeferral);
                historyTransition = transition;
            }

            historyPreparationHook.afterClaim(commandLease, commandClaim);
            DiskBaseline observedDisk = readDiskBaseline();
            LiveDartDocumentSnapshot observedLive = editor.liveSnapshot();
            synchronized (this) {
                requireActiveHistoryTransitionLocked(transition);
                requireHistoryCursorLocked(edge, sourceEndpoint);
                requireHistoryLeaseLocked(
                        direction,
                        commandLease,
                        edge,
                        sourceEndpoint,
                        targetEndpoint);
                if (externalEventEpoch != reservedEventEpoch
                        || epoch != reservedCoordinatorEpoch
                        || !sameDiskBaseline(edge, observedDisk)
                        || !historyCursorMatchesLiveLocked(
                                sourceEndpoint, observedLive)) {
                    throw new IOException(
                            "The exact pair epoch, durable B or live Source cursor "
                            + "changed before native history replay");
                }
                transition.observedDisk = observedDisk;
                transition.sourceLive = observedLive;
            }
            FlutterDesignerEditorSupport.NativeHistoryReplay replay =
                    editor.beginNativeHistoryReplay(
                            observedLive.documentIdentity());
            synchronized (this) {
                requireActiveHistoryTransitionLocked(transition);
                if (externalEventEpoch != reservedEventEpoch
                        || epoch != reservedCoordinatorEpoch
                        || unsavedHistoryCursor != transition.sourceCursor
                        || !commandLease.ownsExactActiveTransition()) {
                    replay.close();
                    throw new IOException(
                            "The exact unsaved history authority changed before the raw move");
                }
                transition.nativeReplay = replay;
            }
            return new PreparedHistoryReplay(transition);
        } catch (IOException | RuntimeException | Error failure) {
            IOException wrapped = asIOException(failure);
            failHistoryPreparation(
                    transition,
                    commandLease,
                    commandClaim,
                    claimed,
                    effectsDeferral,
                    wrapped);
            throw wrapped;
        }
    }

    /**
     * A normal Source edit may sit chronologically above a saved semantic edge.
     * Undoing that Source edit restores the same saved bytes in a newer document
     * revision, so refresh only the live identity of the immutable BASELINE
     * cursor before reserving the older semantic replay.
     */
    private void refreshSavedBaselineCursorIfExact(
            DesignerSemanticUndoableEdit.Direction direction,
            long currentRevisionId,
            long targetRevisionId) throws IOException {
        UnsavedPairHistoryEdge edge;
        HistoryEndpoint sourceEndpoint;
        UnsavedPairHistoryCursor expectedCursor;
        long eventTicket;
        long coordinatorEpoch;
        synchronized (this) {
            edge = requireHistoryEdgeLocked(
                    direction, currentRevisionId, targetRevisionId);
            sourceEndpoint = direction
                    == DesignerSemanticUndoableEdit.Direction.UNDO
                            ? edge.after() : edge.before();
            expectedCursor = unsavedHistoryCursor;
            boolean sourceOverlay = sourceEndpoint
                    instanceof SourceOverlayBaselineHistoryEndpoint;
            if (expectedCursor == null
                    || !compatibleSavedBaselineCursor(
                            expectedCursor.endpoint(), sourceEndpoint)
                    || sourceEndpoint.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                        && !(sourceEndpoint instanceof MetadataHistoryEndpoint)
                    || staged != null
                    || sourceOverlay && !sourceDirty
                    || controller.state() != sourceEndpoint.currentIdentity()) {
                return;
            }
            eventTicket = externalEventEpoch;
            coordinatorEpoch = epoch;
        }

        LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
        boolean sourceOverlay = sourceEndpoint
                instanceof SourceOverlayBaselineHistoryEndpoint;
        if (sourceOverlay && !editor.sourceModified()
                || fresh.documentIdentity()
                    != expectedCursor.liveIdentity().documentIdentity()
                || !Arrays.equals(
                        sourceEndpoint.dartBytes(),
                        fresh.markerBearingUtf8())) {
            return;
        }
        synchronized (this) {
            if (unsavedHistoryCursor == expectedCursor
                    && compatibleSavedBaselineCursor(
                            expectedCursor.endpoint(), sourceEndpoint)
                    && externalEventEpoch == eventTicket
                    && epoch == coordinatorEpoch
                    && (!sourceOverlay || sourceDirty)
                    && staged == null
                    && controller.state()
                        == sourceEndpoint.currentIdentity()) {
                unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                        sourceEndpoint, fresh, (StagedPairProof) null);
            }
        }
    }

    private static boolean compatibleSavedBaselineCursor(
            HistoryEndpoint cursorEndpoint,
            HistoryEndpoint semanticEndpoint) {
        if (cursorEndpoint == semanticEndpoint) {
            return true;
        }
        return cursorEndpoint instanceof BaselineHistoryEndpoint
                && semanticEndpoint
                    instanceof SourceOverlayBaselineHistoryEndpoint overlay
                && cursorEndpoint.revision() == overlay.revision()
                && cursorEndpoint.currentIdentity() == overlay.currentIdentity()
                && Arrays.equals(
                        cursorEndpoint.baselineDartBytes(),
                        overlay.baselineDartBytes())
                && Arrays.equals(
                        cursorEndpoint.baselineFdBytes(),
                        overlay.baselineFdBytes());
    }

    /**
     * A native Source move can switch the unmanaged envelope while CES is
     * already dirty at one semantic PAIRED revision. In that case NetBeans may
     * publish no modified/unmodified edge. Rebind only when the exact live
     * bytes identify another retained physical endpoint of the same canonical
     * revision.
     */
    private void refreshPairedVariantCursorIfExact(
            DesignerSemanticUndoableEdit.Direction direction,
            long currentRevisionId,
            long targetRevisionId) throws IOException {
        UnsavedPairHistoryEdge edge;
        HistoryEndpoint sourceEndpoint;
        UnsavedPairHistoryCursor expectedCursor;
        StagedPairAuthority expectedAuthority;
        long eventTicket;
        long coordinatorEpoch;
        synchronized (this) {
            edge = requireHistoryEdgeLocked(
                    direction, currentRevisionId, targetRevisionId);
            sourceEndpoint = direction
                    == DesignerSemanticUndoableEdit.Direction.UNDO
                            ? edge.after() : edge.before();
            expectedCursor = unsavedHistoryCursor;
            expectedAuthority = staged;
            if (!isPairedPhysicalEndpoint(sourceEndpoint)
                    || expectedCursor == null
                    || expectedCursor.endpoint() == sourceEndpoint
                    || !isPairedPhysicalEndpoint(expectedCursor.endpoint())
                    || expectedCursor.endpoint().revision()
                        != sourceEndpoint.revision()
                    || expectedAuthority == null
                    || expectedAuthority.owner() != edge.owner()
                    || expectedAuthority.revision()
                        != sourceEndpoint.revision()
                    || expectedCursor.stagedProof()
                        != expectedAuthority.proof()
                    || !sourceDirty
                    || state.status()
                        != PairSaveCoordinatorStatus.STAGED_PAIR
                    || controller.state()
                        != sourceEndpoint.currentIdentity()) {
                return;
            }
            eventTicket = externalEventEpoch;
            coordinatorEpoch = epoch;
        }

        LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
        if (!editor.sourceModified()
                || fresh.documentIdentity()
                    != expectedCursor.liveIdentity().documentIdentity()
                || !Arrays.equals(
                        sourceEndpoint.dartBytes(),
                        fresh.markerBearingUtf8())) {
            return;
        }
        StagedPairProof rebound = bindHistoryEndpoint(sourceEndpoint, fresh);
        StagedPairAuthority reboundAuthority = new StagedPairAuthority(
                rebound, edge.owner(), sourceEndpoint.revision());
        synchronized (this) {
            if (unsavedHistoryCursor == expectedCursor
                    && staged == expectedAuthority
                    && expectedCursor.stagedProof()
                        == expectedAuthority.proof()
                    && externalEventEpoch == eventTicket
                    && epoch == coordinatorEpoch
                    && sourceDirty
                    && editor.sourceModified()
                    && state.status()
                        == PairSaveCoordinatorStatus.STAGED_PAIR
                    && controller.state()
                        == sourceEndpoint.currentIdentity()) {
                staged = reboundAuthority;
                unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                        sourceEndpoint, fresh, rebound);
            }
        }
    }

    /**
     * Reconciles a native Source move between retained physical variants when
     * no semantic direction is available (notably Save and diagnostics).
     * Exact live bytes must identify one variant of the already-authoritative
     * semantic revision; otherwise this method leaves authority untouched so
     * the caller's normal exact-evidence check can reject it.
     */
    private void refreshPairedVariantCursorFromLiveIfExact()
            throws IOException {
        UnsavedPairHistoryCursor expectedCursor;
        StagedPairAuthority expectedAuthority;
        List<HistoryEndpoint> variants = new java.util.ArrayList<>();
        long eventTicket;
        long coordinatorEpoch;
        synchronized (this) {
            expectedCursor = unsavedHistoryCursor;
            expectedAuthority = staged;
            if (expectedCursor == null
                    || expectedAuthority == null
                    || !isPairedPhysicalEndpoint(expectedCursor.endpoint())
                    || expectedCursor.stagedProof()
                        != expectedAuthority.proof()
                    || expectedCursor.endpoint().revision()
                        != expectedAuthority.revision()
                    || !sourceDirty
                    || state.status()
                        != PairSaveCoordinatorStatus.STAGED_PAIR
                    || controller.state()
                        != expectedCursor.endpoint().currentIdentity()) {
                return;
            }
            for (UnsavedPairHistoryEdge edge : unsavedPairHistory.values()) {
                if (edge.owner() != expectedAuthority.owner()) {
                    throw new IOException(
                            "Retained physical Source variants belong to different command owners");
                }
                addPhysicalVariantIfMatching(
                        variants, edge.before(), expectedAuthority.revision());
                addPhysicalVariantIfMatching(
                        variants, edge.after(), expectedAuthority.revision());
            }
            addPhysicalVariantIfMatching(
                    variants,
                    expectedCursor.endpoint(),
                    expectedAuthority.revision());
            eventTicket = externalEventEpoch;
            coordinatorEpoch = epoch;
        }

        LiveDartDocumentSnapshot fresh = editor.liveSnapshot();
        if (!editor.sourceModified()
                || fresh.documentIdentity()
                    != expectedCursor.liveIdentity().documentIdentity()) {
            return;
        }
        if (expectedCursor.liveIdentity().sameEvidence(fresh)) {
            return;
        }
        HistoryEndpoint exactVariant = null;
        for (HistoryEndpoint variant : variants) {
            if (!Arrays.equals(
                    variant.dartBytes(), fresh.markerBearingUtf8())) {
                continue;
            }
            if (exactVariant == null || variant == expectedCursor.endpoint()) {
                exactVariant = variant;
            } else if (exactVariant != expectedCursor.endpoint()) {
                // Distinct endpoint identities with identical semantic/source
                // coordinates are indistinguishable without a semantic replay
                // direction. Do not guess which native position owns them.
                return;
            }
        }
        if (exactVariant == null) {
            return;
        }
        StagedPairProof rebound = bindHistoryEndpoint(exactVariant, fresh);
        StagedPairAuthority reboundAuthority = new StagedPairAuthority(
                rebound,
                expectedAuthority.owner(),
                exactVariant.revision());
        synchronized (this) {
            if (unsavedHistoryCursor == expectedCursor
                    && staged == expectedAuthority
                    && expectedCursor.stagedProof()
                        == expectedAuthority.proof()
                    && externalEventEpoch == eventTicket
                    && epoch == coordinatorEpoch
                    && sourceDirty
                    && editor.sourceModified()
                    && state.status()
                        == PairSaveCoordinatorStatus.STAGED_PAIR
                    && controller.state()
                        == exactVariant.currentIdentity()) {
                staged = reboundAuthority;
                unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                        exactVariant, fresh, rebound);
            }
        }
    }

    private static void addPhysicalVariantIfMatching(
            List<HistoryEndpoint> variants,
            HistoryEndpoint endpoint,
            DesignerCommandRevision revision) {
        if (!isPairedPhysicalEndpoint(endpoint)
                || endpoint.revision() != revision) {
            return;
        }
        for (HistoryEndpoint retained : variants) {
            if (retained == endpoint) {
                return;
            }
        }
        variants.add(endpoint);
    }

    private static boolean isPairedPhysicalEndpoint(HistoryEndpoint endpoint) {
        return endpoint instanceof PairedHistoryEndpoint
                || endpoint instanceof ReanchoredPairedHistoryEndpoint;
    }

    @Override
    public void release(long beforeRevisionId, long afterRevisionId) {
        synchronized (this) {
            HistoryEdgeKey key = new HistoryEdgeKey(
                    beforeRevisionId, afterRevisionId);
            UnsavedPairHistoryEdge edge = unsavedPairHistory.get(key);
            if (edge != null
                    && edge.before().revision().revisionId()
                        == beforeRevisionId
                    && edge.after().revision().revisionId()
                        == afterRevisionId) {
                unsavedPairHistory.remove(key, edge);
            }
        }
    }

    private UnsavedPairHistoryEdge requireHistoryEdgeLocked(
            DesignerSemanticUndoableEdit.Direction direction,
            long currentRevisionId,
            long targetRevisionId) throws IOException {
        if (sourceRestage != null || historyTransition != null || preparation != null
                || replacement != null || activePairSave != null
                || activeSourceSave != null || activeFdOnlySave != null) {
            throw new IOException(
                    "Another pair operation already owns unsaved Designer history");
        }
        rejectConflictSaveLocked();
        HistoryEdgeKey key = direction
                == DesignerSemanticUndoableEdit.Direction.UNDO
                        ? new HistoryEdgeKey(targetRevisionId, currentRevisionId)
                        : new HistoryEdgeKey(currentRevisionId, targetRevisionId);
        UnsavedPairHistoryEdge edge = unsavedPairHistory.get(key);
        if (edge == null
                || edge.before().revision().revisionId()
                    != key.beforeRevisionId()
                || edge.after().revision().revisionId()
                    != key.afterRevisionId()) {
            throw new IOException(
                    "The requested native history entry is not one retained exact pair edge");
        }
        return edge;
    }

    private void requireHistoryCursorLocked(
            UnsavedPairHistoryEdge edge,
            HistoryEndpoint expectedSource) throws IOException {
        if (unsavedHistoryCursor == null
                || unsavedHistoryCursor.endpoint() != expectedSource
                || unsavedHistoryOwner != edge.owner()
                || controller.state() != expectedSource.currentIdentity()) {
            throw new IOException(
                    "The command, model or pair cursor is not the exact history source endpoint");
        }
        if (expectedSource.revision().persistenceKind()
                == DesignerRevisionPersistenceKind.PAIRED) {
            if (staged == null
                    || staged.owner() != edge.owner()
                    || staged.revision() != expectedSource.revision()
                    || unsavedHistoryCursor.stagedProof() != staged.proof()) {
                throw new IOException(
                        "The exact staged PAIRED authority differs from the history cursor");
            }
        } else if (staged != null
                || unsavedHistoryCursor.stagedProof() != null) {
            throw new IOException(
                    "The BASELINE history cursor cannot retain staged pair authority");
        }
    }

    private void requireHistoryLeaseLocked(
            DesignerSemanticUndoableEdit.Direction direction,
            DesignerCommandSessionOrchestrator.PendingCommandLease lease,
            UnsavedPairHistoryEdge edge,
            HistoryEndpoint sourceEndpoint,
            HistoryEndpoint targetEndpoint) throws IOException {
        DesignerCommandSessionOrchestrator.PendingTransitionKind expectedKind =
                direction == DesignerSemanticUndoableEdit.Direction.UNDO
                        ? DesignerCommandSessionOrchestrator.PendingTransitionKind.UNDO
                        : DesignerCommandSessionOrchestrator.PendingTransitionKind.REDO;
        if (lease.owner() != edge.owner()
                || lease.kind() != expectedKind
                || lease.predecessorRevision() != sourceEndpoint.revision()
                || lease.candidateRevision() != targetEndpoint.revision()
                || !lease.ownsExactActiveTransition()) {
            throw new IOException(
                    "The claimed command lease is not the exact adjacent history transition");
        }
    }

    private boolean historyCursorMatchesLiveLocked(
            HistoryEndpoint endpoint,
            LiveDartDocumentSnapshot live) {
        if (unsavedHistoryCursor == null
                || unsavedHistoryCursor.endpoint() != endpoint
                || !unsavedHistoryCursor.liveIdentity().sameEvidence(live)
                || !Arrays.equals(endpoint.dartBytes(),
                        live.markerBearingUtf8())) {
            return false;
        }
        return endpoint.revision().persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || staged != null
                && staged.proof().liveCandidateIdentity().sameEvidence(live);
    }

    private static boolean sameDiskBaseline(
            UnsavedPairHistoryEdge edge, DiskBaseline observed) {
        return Arrays.equals(edge.baselineDartBytes(), observed.dartBytes())
                && Arrays.equals(edge.baselineFdBytes(), observed.fdBytes());
    }

    private void requireActiveHistoryTransitionLocked(
            ActiveHistoryTransition expected) throws IOException {
        if (expected == null || historyTransition != expected
                || expected.resolution != HistoryTransitionResolution.ACTIVE) {
            throw new IOException(
                    "The exact unsaved Designer history reservation is no longer active");
        }
    }

    private void failHistoryPreparation(
            ActiveHistoryTransition transition,
            DesignerCommandSessionOrchestrator.PendingCommandLease lease,
            Object claim,
            boolean claimed,
            EffectsDeferral effectsDeferral,
            IOException failure) {
        if (transition != null && transition.nativeReplay != null) {
            try {
                transition.nativeReplay.close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            } finally {
                transition.nativeReplay = null;
            }
        }
        boolean invalidate;
        StateChange change = null;
        synchronized (this) {
            if (historyTransition == transition) {
                historyTransition = null;
            }
            invalidate = transition != null
                    && (externalEventEpoch != transition.eventTicket
                        || epoch != transition.coordinatorEpoch
                        || unsavedHistoryCursor != transition.sourceCursor
                        || conflictStatusLocked());
            if (invalidate) {
                staged = null;
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                failedSavePending = true;
                diskBaseline = null;
                if (!conflictStatusLocked()) {
                    change = transitionLocked(
                            PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                            "Unsaved Designer history authority changed before "
                            + "the native move: " + reason(failure));
                }
            }
        }

        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                DesignerCommandSessionOrchestrator.DeferredLeaseEffects.none();
        try {
            if (claimed) {
                commandEffects = invalidate
                        ? lease.invalidateClaimedDeferredEffects(claim)
                        : lease.abortToExactPredecessorDeferredEffects(claim);
            } else {
                commandEffects = lease.abortDeferredEffects();
            }
        } catch (RuntimeException resolutionFailure) {
            failure.addSuppressed(resolutionFailure);
        }
        publishEffects(change, commandEffects);
        if (effectsDeferral != null) {
            try {
                effectsDeferral.close();
            } catch (RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    /**
     * Captures the exact virtual CES serialization without advancing its
     * private savepoint.  The before/after evidence check rejects a live edit
     * racing this pure preflight.
     */
    private SourceSaveCandidate captureHistorySourceCandidate()
            throws IOException {
        return onEdt(() -> {
            LiveDartDocumentSnapshot before = editor.liveSnapshot();
            byte[] serialized;
            try (InputStream input = editor.getInputStream()) {
                serialized = readBounded(
                        input,
                        MAX_SOURCE_PERSISTENCE_BYTES,
                        "planned Dart source serialization");
            }
            LiveDartDocumentSnapshot after = editor.liveSnapshot();
            if (!before.sameEvidence(after)) {
                throw new IOException(
                        "The live Dart source changed while its exact Source Save was being planned");
            }
            boolean editorDirty = editor.sourceModified();
            synchronized (PairSaveCoordinator.this) {
                return new SourceSaveCandidate(
                        after,
                        serialized,
                        editorDirty,
                        sourceDirty,
                        sourceStateEpoch,
                        retainedMetadataCursorLocked());
            }
        });
    }

    private SourceHistoryReanchorPlan prepareSourceHistoryReanchorPlan(
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease,
            SourceSaveCandidate candidate) throws IOException {
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> retained;
        UnsavedPairHistoryCursor retainedCursor;
        DiskBaseline retainedBaseline;
        FlutterDesignerDocumentState.Current retainedCurrent;
        long coordinatorEpoch;
        long eventEpoch;
        synchronized (this) {
            if (!sourceDirty
                    || staged != null
                    || activeSourceSave != null
                    || activePairSave != null
                    || activeFdOnlySave != null
                    || preparation != null
                    || replacement != null
                    || historyTransition != null
                    || unsavedHistoryCursor == null
                    || unsavedHistoryOwner != owner
                    || diskBaseline == null
                    || sourceStateEpoch != candidate.sourceStateEpoch()
                    || !lease.ownsExactActiveRevision()
                    || lease.revision()
                        != unsavedHistoryCursor.endpoint().revision()
                    || lease.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                    || candidate.liveIdentity().documentIdentity()
                        != unsavedHistoryCursor.liveIdentity()
                                .documentIdentity()) {
                throw new IOException(
                        "Cannot precompute Source history from a stale command, model or native cursor");
            }
            for (UnsavedPairHistoryEdge edge : unsavedPairHistory.values()) {
                if (edge.owner() != owner) {
                    throw new IOException(
                            "Retained Designer history belongs to another command-session owner");
                }
            }
            retained = Map.copyOf(unsavedPairHistory);
            retainedCursor = unsavedHistoryCursor;
            retainedBaseline = diskBaseline;
            FlutterDesignerDocumentState currentState = controller.state();
            if (!(currentState
                    instanceof FlutterDesignerDocumentState.Current current)) {
                throw new IOException(
                        "The Flutter Designer controller is no longer at one exact Current state");
            }
            retainedCurrent = current;
            coordinatorEpoch = epoch;
            eventEpoch = externalEventEpoch;
        }
        if (!Arrays.equals(
                    lease.priorDartBytes(), retainedBaseline.dartBytes())
                || !Arrays.equals(
                    lease.priorFdBytes(), retainedBaseline.fdBytes())) {
            throw new IOException(
                    "The clean command anchor differs from the exact paired disk baseline");
        }

        DesignerCommandRevision savedRevision = lease.reanchoredRevision(
                lease.savedRevisionId());
        FlutterDesignerDocumentState.Current savedCurrent = savedCurrent(
                retainedCurrent,
                savedRevision,
                lease.savedSourceIntegrityIdentity(),
                lease.savedThreeWayIntegrityIdentity());
        FlutterDesignerDocumentController.CurrentAdoptionTicket adoptionTicket;
        try {
            adoptionTicket = controller.currentAdoptionTicket(retainedCurrent);
        } catch (IllegalStateException staleCurrent) {
            throw new IOException(
                    "Cannot pin the loaded Flutter Designer Current for Source-anchor adoption",
                    staleCurrent);
        }
        DiskBaseline savedBaseline = new DiskBaseline(
                lease.candidateDartBytes(), lease.candidateFdBytes());
        IdentityHashMap<HistoryEndpoint, HistoryEndpoint> endpointByIdentity =
                new IdentityHashMap<>();
        RetainedPhysicalEndpointBudget endpointBudget =
                new RetainedPhysicalEndpointBudget(
                        lease.maxRetainedPairBytes());
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchored =
                new HashMap<>();
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : retained.entrySet()) {
            UnsavedPairHistoryEdge oldEdge = entry.getValue();
            HistoryEndpoint before = sourceReanchoredEndpoint(
                    oldEdge.before(), lease, savedCurrent, savedBaseline,
                    endpointByIdentity, endpointBudget);
            HistoryEndpoint after = sourceReanchoredEndpoint(
                    oldEdge.after(), lease, savedCurrent, savedBaseline,
                    endpointByIdentity, endpointBudget);
            UnsavedPairHistoryEdge next = new UnsavedPairHistoryEdge(
                    before, after, oldEdge.owner());
            if (!entry.getKey().equals(next.key())) {
                throw new IOException(
                        "Source history re-anchor changed a stable semantic edge id");
            }
            reanchored.put(entry.getKey(), next);
        }
        HistoryEndpoint cursorEndpoint = sourceReanchoredEndpoint(
                retainedCursor.endpoint(), lease, savedCurrent, savedBaseline,
                endpointByIdentity, endpointBudget);
        if (cursorEndpoint == null
                || cursorEndpoint.revision() != savedRevision
                || cursorEndpoint.revision().persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE) {
            throw new IOException(
                    "Source history lost its exact saved semantic underlay cursor");
        }
        UnsavedPairHistoryCursor reanchoredCursor =
                new UnsavedPairHistoryCursor(
                        cursorEndpoint,
                        retainedCursor.liveIdentity(),
                        (StagedPairProof) null);
        return new SourceHistoryReanchorPlan(
                lease,
                coordinatorEpoch,
                eventEpoch,
                candidate.sourceStateEpoch(),
                retainedBaseline,
                candidate,
                retainedCurrent,
                savedCurrent,
                adoptionTicket,
                savedBaseline,
                retained,
                reanchored,
                retainedCursor,
                reanchoredCursor);
    }

    private SemanticBaselineHistorySavePlan
            prepareSemanticBaselineHistorySavePlan(
            DesignerCommandSessionOrchestrator owner,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            SourceSaveCandidate candidate) throws IOException {
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> retained;
        UnsavedPairHistoryCursor retainedCursor;
        DiskBaseline retainedBaseline;
        FlutterDesignerDocumentState.Current retainedCurrent;
        long coordinatorEpoch;
        long eventEpoch;
        synchronized (this) {
            boolean metadata = retainedMetadataCursorLocked();
            if (!sourceDirty && !metadata
                    || !candidate.editorDirty() && !metadata
                    || staged != null
                    || activeSourceSave != null
                    || activePairSave != null
                    || activeFdOnlySave != null
                    || preparation != null
                    || replacement != null
                    || historyTransition != null
                    || unsavedHistoryCursor == null
                    || unsavedHistoryOwner != owner
                    || !exactSemanticBaselineEndpoint(
                            unsavedHistoryCursor.endpoint(),
                            lease.revision())
                    || diskBaseline == null
                    || sourceStateEpoch != candidate.sourceStateEpoch()
                    || !lease.ownsExactActiveRevision()
                    || lease.revision()
                        != unsavedHistoryCursor.endpoint().revision()
                    || lease.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE && !metadata
                    || candidate.liveIdentity().documentIdentity()
                        != unsavedHistoryCursor.liveIdentity()
                                .documentIdentity()
                    || !metadata && !candidate.liveIdentity().sameEvidence(
                            unsavedHistoryCursor.liveIdentity())) {
                throw new IOException(
                        "Cannot precompute semantic BASELINE Save from stale command, model or native cursor evidence");
            }
            for (UnsavedPairHistoryEdge edge : unsavedPairHistory.values()) {
                if (edge.owner() != owner) {
                    throw new IOException(
                            "Retained Designer history belongs to another command-session owner");
                }
            }
            retained = Map.copyOf(unsavedPairHistory);
            retainedCursor = unsavedHistoryCursor;
            retainedBaseline = diskBaseline;
            FlutterDesignerDocumentState currentState = controller.state();
            if (!(currentState
                    instanceof FlutterDesignerDocumentState.Current current)
                    || current != retainedCursor.endpoint()
                            .currentIdentity()) {
                throw new IOException(
                        "The Flutter Designer controller no longer owns the exact semantic BASELINE Current");
            }
            retainedCurrent = current;
            coordinatorEpoch = epoch;
            eventEpoch = externalEventEpoch;
        }

        HistoryEndpoint retainedEndpoint = retainedCursor.endpoint();
        if (!Arrays.equals(
                    lease.durableDartBytes(), retainedBaseline.dartBytes())
                || !Arrays.equals(
                    lease.durableFdBytes(), retainedBaseline.fdBytes())
                || !Arrays.equals(
                    lease.reanchoredRevision(lease.savedRevisionId()).dartCandidateBytes(),
                    candidate.serializedDartBytes())
                || !(retainedEndpoint instanceof MetadataHistoryEndpoint) && !Arrays.equals(
                    retainedEndpoint.dartBytes(),
                    candidate.serializedDartBytes())
                || !Arrays.equals(
                    retainedEndpoint.baselineDartBytes(),
                    retainedBaseline.dartBytes())
                || !Arrays.equals(lease.revision().fdBytes(), retainedEndpoint.revision().fdBytes())
                || !Arrays.equals(
                    retainedEndpoint.baselineFdBytes(),
                    retainedBaseline.fdBytes())) {
            throw new IOException(
                    "The dirty semantic BASELINE cursor differs from the exact live and durable pair");
        }

        DesignerCommandRevision savedRevision = lease.reanchoredRevision(
                lease.savedRevisionId());
        FlutterDesignerDocumentState.Current savedCurrent = savedCurrent(
                retainedCurrent,
                savedRevision,
                lease.savedSourceIntegrityIdentity(),
                lease.savedThreeWayIntegrityIdentity());
        FlutterDesignerDocumentController.CurrentAdoptionTicket adoptionTicket;
        try {
            adoptionTicket = controller.currentAdoptionTicket(retainedCurrent);
        } catch (IllegalStateException staleCurrent) {
            throw new IOException(
                    "Cannot pin the loaded Flutter Designer Current for semantic BASELINE Save",
                    staleCurrent);
        }
        DiskBaseline savedBaseline = new DiskBaseline(
                savedRevision.dartCandidateBytes(), savedRevision.fdBytes());
        IdentityHashMap<HistoryEndpoint, HistoryEndpoint> endpointByIdentity =
                new IdentityHashMap<>();
        RetainedPhysicalEndpointBudget endpointBudget =
                new RetainedPhysicalEndpointBudget(
                        lease.maxRetainedPairBytes());
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchored =
                new HashMap<>();
        HistoryEndpoint savedSourceEndpoint = Arrays.equals(
                retainedCursor.endpoint().dartBytes(), candidate.serializedDartBytes())
                ? retainedCursor.endpoint() : null;
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : retained.entrySet()) {
            UnsavedPairHistoryEdge oldEdge = entry.getValue();
            HistoryEndpoint before = reanchoredEndpoint(
                    oldEdge.before(), lease, savedCurrent, savedBaseline,
                    savedSourceEndpoint, endpointByIdentity,
                    endpointBudget);
            HistoryEndpoint after = reanchoredEndpoint(
                    oldEdge.after(), lease, savedCurrent, savedBaseline,
                    savedSourceEndpoint, endpointByIdentity,
                    endpointBudget);
            UnsavedPairHistoryEdge next = new UnsavedPairHistoryEdge(
                    before, after, oldEdge.owner());
            if (!entry.getKey().equals(next.key())) {
                throw new IOException(
                        "Semantic BASELINE Save changed a stable history edge id");
            }
            reanchored.put(entry.getKey(), next);
        }
        HistoryEndpoint savedEndpoint;
        if (savedSourceEndpoint == null) {
            // Keep the old native edge's physical Source underlay intact.
            // The saved live overlay is a separate cursor of the same revision.
            savedEndpoint = new BaselineHistoryEndpoint(savedRevision, savedCurrent,
                    savedBaseline.dartBytes(), savedBaseline.fdBytes());
            endpointBudget.retain(savedEndpoint);
        } else {
            savedEndpoint = reanchoredEndpoint(
                    retainedCursor.endpoint(), lease, savedCurrent, savedBaseline,
                    savedSourceEndpoint, endpointByIdentity, endpointBudget);
        }
        if (savedEndpoint == null
                || savedEndpoint.revision() != savedRevision
                || savedEndpoint.revision().persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE) {
            throw new IOException(
                    "Semantic BASELINE Save lost its exact durable endpoint");
        }
        UnsavedPairHistoryCursor savedCursor =
                new UnsavedPairHistoryCursor(
                        savedEndpoint,
                        candidate.liveIdentity(),
                        (StagedPairProof) null);
        return new SemanticBaselineHistorySavePlan(
                lease,
                coordinatorEpoch,
                eventEpoch,
                candidate.sourceStateEpoch(),
                retainedBaseline,
                candidate,
                retainedCurrent,
                savedCurrent,
                adoptionTicket,
                savedBaseline,
                retained,
                reanchored,
                savedEndpoint,
                retainedCursor,
                savedCursor);
    }

    private static HistoryEndpoint sourceReanchoredEndpoint(
            HistoryEndpoint oldEndpoint,
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease,
            FlutterDesignerDocumentState.Current savedCurrent,
            DiskBaseline savedBaseline,
            IdentityHashMap<HistoryEndpoint, HistoryEndpoint>
                    endpointByIdentity,
            RetainedPhysicalEndpointBudget endpointBudget) throws IOException {
        HistoryEndpoint existing = endpointByIdentity.get(oldEndpoint);
        if (existing != null) {
            return existing;
        }
        DesignerCommandRevision revision;
        try {
            revision = lease.reanchoredRevision(
                    oldEndpoint.revision().revisionId());
        } catch (IllegalArgumentException absentRevision) {
            throw new IOException(
                    "A retained native edge names a command revision absent from the Source graph",
                    absentRevision);
        }
        HistoryEndpoint next;
        if (revision.persistenceKind()
                == DesignerRevisionPersistenceKind.BASELINE) {
            boolean durableCandidate = Arrays.equals(
                    revision.dartCandidateBytes(),
                    savedBaseline.dartBytes());
            boolean historicalCandidate = Arrays.equals(
                    revision.dartCandidateBytes(),
                    oldEndpoint.dartBytes());
            if (!(durableCandidate || historicalCandidate)
                    || !Arrays.equals(
                        revision.fdBytes(), savedBaseline.fdBytes())) {
                throw new IOException(
                        "A retained BASELINE revision is detached from both its historical bytes and the new Source anchor");
            }
            if (Arrays.equals(
                    oldEndpoint.dartBytes(), savedBaseline.dartBytes())) {
                if (!durableCandidate) {
                    throw new IOException(
                            "A clean BASELINE history endpoint does not describe the new Source anchor");
                }
                next = new BaselineHistoryEndpoint(
                            revision,
                            savedCurrent,
                            savedBaseline.dartBytes(),
                            savedBaseline.fdBytes());
            } else {
                next = new SourceOverlayBaselineHistoryEndpoint(
                            revision,
                            savedCurrent,
                            oldEndpoint.dartBytes(),
                            savedBaseline.dartBytes(),
                            savedBaseline.fdBytes());
            }
        } else if (revision.persistenceKind() == DesignerRevisionPersistenceKind.FD_ONLY) {
            DesignerCommandRevision physical = lease.reanchoredPhysicalRevisionByProjectingAnchor(
                    revision.revisionId(), oldEndpoint.dartBytes());
            next = metadataHistoryEndpoint(oldEndpoint, revision, physical,
                    savedCurrent, savedBaseline);
        } else if (revision.persistenceKind()
                == DesignerRevisionPersistenceKind.PAIRED) {
            SavedHistorySeed seed;
            if (oldEndpoint instanceof BaselineHistoryEndpoint
                    || oldEndpoint
                        instanceof SourceOverlayBaselineHistoryEndpoint) {
                // A schema migration may canonicalize the retained semantic
                // revision while preserving its exact Dart envelope.  The
                // re-anchored revision is the authoritative post-migration
                // .fd candidate; retaining the legacy raw .fd bytes here
                // would make an otherwise proven physical endpoint
                // impossible to derive from the new durable anchor.
                seed = new FormerDurableHistorySeed(
                        oldEndpoint.dartBytes(), revision.fdBytes());
            } else if (oldEndpoint instanceof PairedHistoryEndpoint paired) {
                seed = new ReanchoredAnalyzedHistorySeed(paired.seed());
            } else if (oldEndpoint
                    instanceof ReanchoredPairedHistoryEndpoint paired) {
                seed = paired.seed();
            } else if (oldEndpoint instanceof MetadataHistoryEndpoint metadata) {
                seed = metadata.seed();
            } else {
                throw new IOException(
                        "Unsupported retained endpoint during Source history re-anchor");
            }
            try {
                DesignerCommandRevision physical =
                        lease.reanchoredPhysicalRevisionByProjectingAnchor(
                                revision.revisionId(),
                                oldEndpoint.dartBytes());
                if (physical.persistenceKind()
                            != DesignerRevisionPersistenceKind.PAIRED
                        || physical.preparedPair().isEmpty()
                        || !physical.document().equals(revision.document())
                        || !Arrays.equals(
                                physical.fdBytes(), revision.fdBytes())
                        || !Arrays.equals(
                                physical.dartCandidateBytes(),
                                seed.candidateDartBytes())
                        || !Arrays.equals(
                                physical.fdBytes(),
                                seed.candidateFdBytes())) {
                    throw new IOException(
                            "A physical history variant cannot be derived from the new Source anchor");
                }
                next = new ReanchoredPairedHistoryEndpoint(
                        revision,
                        savedCurrent,
                        savedBaseline.dartBytes(),
                        savedBaseline.fdBytes(),
                        physical.preparedPair().orElseThrow(),
                        seed);
            } catch (IllegalArgumentException invalidHistory) {
                throw new IOException(
                        "Retained semantic bytes cannot be proven against the new Source anchor",
                        invalidHistory);
            }
        } else {
            throw new IOException(
                    "Unsupported retained revision kind during Source history re-anchor: "
                    + revision.persistenceKind());
        }
        endpointBudget.retain(next);
        endpointByIdentity.put(oldEndpoint, next);
        return next;
    }

    private SavedHistoryReanchorPlan prepareSavedHistoryReanchorPlan(
            StagedPairAuthority authority,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            throws IOException {
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> retained;
        UnsavedPairHistoryCursor retainedCursor;
        long coordinatorEpoch;
        long eventEpoch;
        synchronized (this) {
            if (staged != authority
                    || unsavedHistoryCursor == null
                    || unsavedHistoryOwner != authority.owner()
                    || unsavedHistoryCursor.endpoint().revision()
                        != authority.revision()
                    || unsavedHistoryCursor.stagedProof()
                        != authority.proof()
                    || !lease.ownsExactActiveRevision()
                    || lease.revision() != authority.revision()) {
                throw new IOException(
                        "Cannot precompute saved history from a stale staged cursor");
            }
            retained = Map.copyOf(unsavedPairHistory);
            retainedCursor = unsavedHistoryCursor;
            coordinatorEpoch = epoch;
            eventEpoch = externalEventEpoch;
        }

        DesignerCommandRevision savedRevision = lease.reanchoredRevision(
                lease.savedRevisionId());
        FlutterDesignerDocumentState.Current savedCurrent = savedCurrent(
                authority.proof().loadedCurrentIdentity(),
                savedRevision,
                lease.savedSourceIntegrityIdentity(),
                lease.savedThreeWayIntegrityIdentity());
        FlutterDesignerDocumentController.CurrentAdoptionTicket adoptionTicket;
        try {
            adoptionTicket = controller.currentAdoptionTicket(
                    authority.proof().loadedCurrentIdentity());
        } catch (IllegalStateException staleCurrent) {
            throw new IOException(
                    "Cannot pin the loaded Flutter Designer Current for saved-history adoption",
                    staleCurrent);
        }
        DiskBaseline savedBaseline = new DiskBaseline(
                savedRevision.dartCandidateBytes(), savedRevision.fdBytes());
        IdentityHashMap<HistoryEndpoint, HistoryEndpoint> endpointByIdentity =
                new IdentityHashMap<>();
        RetainedPhysicalEndpointBudget endpointBudget =
                new RetainedPhysicalEndpointBudget(
                        lease.maxRetainedPairBytes());
        Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchored =
                new HashMap<>();
        for (Map.Entry<HistoryEdgeKey, UnsavedPairHistoryEdge> entry
                : retained.entrySet()) {
            UnsavedPairHistoryEdge oldEdge = entry.getValue();
            HistoryEndpoint before = reanchoredEndpoint(
                    oldEdge.before(), lease, savedCurrent, savedBaseline,
                    retainedCursor.endpoint(), endpointByIdentity,
                    endpointBudget);
            HistoryEndpoint after = reanchoredEndpoint(
                    oldEdge.after(), lease, savedCurrent, savedBaseline,
                    retainedCursor.endpoint(), endpointByIdentity,
                    endpointBudget);
            UnsavedPairHistoryEdge next = new UnsavedPairHistoryEdge(
                    before, after, oldEdge.owner());
            if (!entry.getKey().equals(next.key())) {
                throw new IOException(
                        "Saved history re-anchor changed a stable semantic edge id");
            }
            reanchored.put(entry.getKey(), next);
        }
        HistoryEndpoint savedEndpoint = reanchoredEndpoint(
                retainedCursor.endpoint(), lease, savedCurrent, savedBaseline,
                retainedCursor.endpoint(), endpointByIdentity,
                endpointBudget);
        if (savedEndpoint == null
                || savedEndpoint.revision() != savedRevision
                || savedEndpoint.revision().persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE) {
            throw new IOException(
                    "Saved history does not retain the exact durable semantic endpoint");
        }
        UnsavedPairHistoryCursor savedCursor = new UnsavedPairHistoryCursor(
                savedEndpoint,
                authority.proof().liveCandidateIdentity(),
                (StagedPairProof) null);
        return new SavedHistoryReanchorPlan(
                authority,
                lease,
                coordinatorEpoch,
                eventEpoch,
                authority.proof().liveCandidateIdentity().documentIdentity(),
                authority.proof().loadedCurrentIdentity(),
                savedCurrent,
                adoptionTicket,
                savedBaseline,
                retained,
                reanchored,
                savedEndpoint,
                retainedCursor,
                savedCursor);
    }

    private static HistoryEndpoint reanchoredEndpoint(
            HistoryEndpoint oldEndpoint,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease,
            FlutterDesignerDocumentState.Current savedCurrent,
            DiskBaseline savedBaseline,
            HistoryEndpoint savedSourceEndpoint,
            IdentityHashMap<HistoryEndpoint, HistoryEndpoint>
                    endpointByIdentity,
            RetainedPhysicalEndpointBudget endpointBudget) throws IOException {
        HistoryEndpoint existing = endpointByIdentity.get(oldEndpoint);
        if (existing != null) {
            return existing;
        }
        long revisionId = oldEndpoint.revision().revisionId();
        DesignerCommandRevision revision;
        try {
            revision = lease.reanchoredRevision(revisionId);
        } catch (IllegalArgumentException failure) {
            throw new IOException(
                    "A retained native edge names a command revision absent from the saved graph",
                    failure);
        }
        HistoryEndpoint next;
        if (revision.persistenceKind()
                == DesignerRevisionPersistenceKind.BASELINE) {
            if (!Arrays.equals(
                        revision.dartCandidateBytes(),
                        savedBaseline.dartBytes())
                    || !Arrays.equals(
                        revision.fdBytes(), savedBaseline.fdBytes())) {
                throw new IOException(
                        "A retained BASELINE revision differs from the new durable anchor");
            }
            if (oldEndpoint == savedSourceEndpoint
                    || Arrays.equals(
                            oldEndpoint.dartBytes(), savedBaseline.dartBytes())) {
                next = new BaselineHistoryEndpoint(
                        revision,
                        savedCurrent,
                        savedBaseline.dartBytes(),
                        savedBaseline.fdBytes());
            } else {
                next = new SourceOverlayBaselineHistoryEndpoint(
                        revision,
                        savedCurrent,
                        oldEndpoint.dartBytes(),
                        savedBaseline.dartBytes(),
                        savedBaseline.fdBytes());
            }
        } else if (revision.persistenceKind() == DesignerRevisionPersistenceKind.FD_ONLY) {
            DesignerCommandRevision physical = lease.reanchoredPhysicalRevisionByProjectingAnchor(
                    revisionId, oldEndpoint.dartBytes());
            next = metadataHistoryEndpoint(oldEndpoint, revision, physical,
                    savedCurrent, savedBaseline);
        } else if (revision.persistenceKind()
                == DesignerRevisionPersistenceKind.PAIRED) {
            SavedHistorySeed seed;
            if (oldEndpoint instanceof BaselineHistoryEndpoint
                    || oldEndpoint
                        instanceof SourceOverlayBaselineHistoryEndpoint) {
                // A schema migration may canonicalize the retained semantic
                // revision while preserving its exact Dart envelope.  The
                // re-anchored revision is the authoritative post-migration
                // .fd candidate; retaining the legacy raw .fd bytes here
                // would make an otherwise proven physical endpoint
                // impossible to derive from the new durable anchor.
                seed = new FormerDurableHistorySeed(
                        oldEndpoint.dartBytes(), revision.fdBytes());
            } else if (oldEndpoint instanceof PairedHistoryEndpoint paired) {
                seed = new ReanchoredAnalyzedHistorySeed(paired.seed());
            } else if (oldEndpoint
                    instanceof ReanchoredPairedHistoryEndpoint paired) {
                seed = paired.seed();
            } else if (oldEndpoint instanceof MetadataHistoryEndpoint metadata) {
                seed = metadata.seed();
            } else {
                throw new IOException(
                        "Unsupported retained history endpoint during durable re-anchor");
            }
            try {
                DesignerCommandRevision physical =
                        lease.reanchoredPhysicalRevisionByProjectingAnchor(
                                revisionId, oldEndpoint.dartBytes());
                if (physical.persistenceKind()
                            != DesignerRevisionPersistenceKind.PAIRED
                        || physical.preparedPair().isEmpty()
                        || !physical.document().equals(revision.document())
                        || !Arrays.equals(
                                physical.fdBytes(), revision.fdBytes())
                        || !Arrays.equals(
                                physical.dartCandidateBytes(),
                                seed.candidateDartBytes())
                        || !Arrays.equals(
                                physical.fdBytes(), seed.candidateFdBytes())) {
                    throw new IOException(
                            "A physical history variant cannot be derived from the new durable anchor");
                }
                next = new ReanchoredPairedHistoryEndpoint(
                        revision,
                        savedCurrent,
                        savedBaseline.dartBytes(),
                        savedBaseline.fdBytes(),
                        physical.preparedPair().orElseThrow(),
                        seed);
            } catch (IllegalArgumentException failure) {
                throw new IOException(
                        "Retained history bytes cannot be proven against the new durable anchor",
                        failure);
            }
        } else {
            throw new IOException(
                    "Unsupported retained revision kind during durable re-anchor: "
                    + revision.persistenceKind());
        }
        endpointBudget.retain(next);
        endpointByIdentity.put(oldEndpoint, next);
        return next;
    }

    private static MetadataHistoryEndpoint metadataHistoryEndpoint(
            HistoryEndpoint previous, DesignerCommandRevision revision,
            DesignerCommandRevision physical, FlutterDesignerDocumentState.Current current,
            DiskBaseline baseline) throws IOException {
        if (physical.persistenceKind() != DesignerRevisionPersistenceKind.FD_ONLY
                || !physical.document().equals(revision.document())
                || !Arrays.equals(physical.fdBytes(), revision.fdBytes())
                || !Arrays.equals(physical.dartCandidateBytes(), previous.dartBytes())) {
            throw new IOException("Retained metadata history lost its exact physical Source envelope");
        }
        SavedHistorySeed seed;
        if (previous instanceof PairedHistoryEndpoint paired) {
            seed = new ReanchoredAnalyzedHistorySeed(paired.seed());
        } else if (previous instanceof ReanchoredPairedHistoryEndpoint paired) {
            seed = paired.seed();
        } else if (previous instanceof MetadataHistoryEndpoint metadata) {
            seed = metadata.seed();
        } else {
            seed = new FormerDurableHistorySeed(previous.dartBytes(), revision.fdBytes());
        }
        return new MetadataHistoryEndpoint(revision, current, baseline.dartBytes(),
                baseline.fdBytes(), physical, seed);
    }

    /** Incremental fail-fast accounting for unique projected output identities. */
    private static final class RetainedPhysicalEndpointBudget {
        private final long maximumBytes;
        private final IdentityHashMap<Object, Boolean> retained =
                new IdentityHashMap<>();
        private long retainedBytes;

        RetainedPhysicalEndpointBudget(long maximumBytes) throws IOException {
            if (maximumBytes <= 0) {
                throw new IOException(
                        "The retained physical history byte limit is invalid");
            }
            this.maximumBytes = maximumBytes;
        }

        void retain(HistoryEndpoint endpoint) throws IOException {
            Objects.requireNonNull(endpoint, "endpoint");
            // A Source-history variant owns an exact source proof as well as
            // its pair bytes; its logical revision may retain another envelope.
            retainBytes(
                    endpoint,
                    endpoint.dartBytes(),
                    endpoint.revision().fdBytes(),
                    endpoint.userSourceProofBytes());
        }

        void retainBytes(Object identity, byte[] dart, byte[] fd, long userSourceProofBytes)
                throws IOException {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(dart, "dart");
            Objects.requireNonNull(fd, "fd");
            if (retained.put(identity, Boolean.TRUE) != null) {
                return;
            }
            long endpointBytes;
            try {
                endpointBytes = Math.addExact(
                        (long) dart.length, (long) fd.length);
                endpointBytes = Math.addExact(endpointBytes, userSourceProofBytes);
                retainedBytes = Math.addExact(retainedBytes, endpointBytes);
            } catch (ArithmeticException overflow) {
                retained.remove(identity);
                throw new IOException(
                        "Retained physical history byte accounting overflowed",
                        overflow);
            }
            if (retainedBytes > maximumBytes) {
                retained.remove(identity);
                throw new IOException(
                        "Retained physical Source-envelope history requires "
                        + retainedBytes + " bytes, exceeding the "
                        + maximumBytes + " byte command-history limit");
            }
        }
    }

    private static FlutterDesignerDocumentState.Current savedCurrent(
            FlutterDesignerDocumentState.Current prior,
            DesignerCommandRevision savedRevision,
            DartSourceIntegrityResult source,
            DartThreeWayIntegrityResult threeWay) throws IOException {
        FdDecodeResult decoded = new FdDocumentCodec().decode(
                savedRevision.fdSnapshot());
        if (!(decoded instanceof FdDecodeResult.Current exact)) {
            throw new IOException(
                    "The saved command revision does not decode as a current FD document");
        }
        if (!exact.original().equals(savedRevision.fdSnapshot())
                || !exact.document().equals(savedRevision.document())) {
            throw new IOException(
                    "The saved command revision does not retain its exact decoded FD document");
        }
        ValidationResult validation = new WidgetTreeValidator().validate(
                exact.document(), prior.catalog());
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult verifiedSource = scanner.scan(
                savedRevision.dartCandidateBytes(), exact.document().source());
        DartThreeWayIntegrityResult verifiedThreeWay =
                new DartThreeWayIntegrityGate(scanner).evaluate(
                        source,
                        exact.document().source(),
                        savedRevision.generation());
        if (!validation.valid()
                || source != savedRevision.sourceIntegrity()
                || threeWay.source() != source
                || !source.equals(verifiedSource)
                || !threeWay.onDiskThreeWayMatch()
                || !verifiedThreeWay.onDiskThreeWayMatch()
                || !threeWay.generation().equals(
                        verifiedThreeWay.generation())
                || !threeWay.comparisons().equals(
                        verifiedThreeWay.comparisons())
                || !threeWay.diagnostics().equals(
                        verifiedThreeWay.diagnostics())) {
            throw new IOException(
                    "The saved command revision cannot become the exact loaded Current");
        }
        return new FlutterDesignerDocumentState.Current(
                exact,
                validation,
                prior.catalog(),
                prior.catalogDiagnostics(),
                List.of(),
                Optional.of(source),
                Optional.of(threeWay));
    }

    private UnsavedPairHistoryEdge prepareInitialHistoryEdgeLocked(
            PairPreparation lease,
            PairSaveEvidence evidence) throws IOException {
        HistoryEndpoint baseline;
        if (unsavedHistoryCursor == null) {
            baseline = new BaselineHistoryEndpoint(
                    lease.predecessor,
                    lease.expectedCurrent,
                    lease.prepared.baselineDartBytes(),
                    lease.prepared.baselineFdBytes());
        } else if (unsavedHistoryCursor.endpoint()
                instanceof SourceOverlayBaselineHistoryEndpoint overlay) {
            if (overlay.revision() != lease.predecessor
                    || overlay.currentIdentity() != lease.expectedCurrent
                    || lease.initiallySourceDirty
                    || staged != null
                    || lease.initialLive.documentIdentity()
                        != unsavedHistoryCursor.liveIdentity()
                                .documentIdentity()
                    || !Arrays.equals(
                            lease.initialLive.markerBearingUtf8(),
                            lease.prepared.baselineDartBytes())
                    || !Arrays.equals(
                            overlay.baselineDartBytes(),
                            lease.prepared.baselineDartBytes())
                    || !Arrays.equals(
                            overlay.baselineFdBytes(),
                            lease.prepared.baselineFdBytes())) {
                throw new IOException(
                        "A new Designer command cannot advance a stale Source-overlay cursor");
            }
            baseline = new BaselineHistoryEndpoint(
                    lease.predecessor,
                    lease.expectedCurrent,
                    lease.prepared.baselineDartBytes(),
                    lease.prepared.baselineFdBytes());
        } else if (unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint metadata
                && metadata.revision() == lease.predecessor
                && metadata.currentIdentity() == lease.expectedCurrent
                && staged == null
                && Arrays.equals(metadata.dartBytes(), lease.initialLive.markerBearingUtf8())
                && Arrays.equals(metadata.baselineDartBytes(), lease.prepared.baselineDartBytes())
                && Arrays.equals(metadata.baselineFdBytes(), lease.prepared.baselineFdBytes())) {
            baseline = metadata;
        } else {
            baseline = requireBaselineEndpointLocked(lease.predecessor);
        }
        PairedHistoryEndpoint candidate = new PairedHistoryEndpoint(
                lease.candidate, evidence);
        UnsavedPairHistoryEdge edge = new UnsavedPairHistoryEdge(
                baseline, candidate, lease.commandLease.owner());
        requireRegisterableHistoryEdgeLocked(edge);
        return edge;
    }

    private UnsavedPairHistoryEdge prepareReplacementHistoryEdgeLocked(
            PairReplacement lease,
            PairSaveEvidence evidence) throws IOException {
        if (unsavedHistoryCursor == null
                || unsavedHistoryCursor != lease.predecessorCursor
                || unsavedHistoryCursor.endpoint()
                    != lease.predecessorEndpoint
                || lease.predecessorEndpoint.revision()
                    != lease.predecessor
                || unsavedHistoryCursor.stagedProof()
                    != lease.predecessorProof) {
            throw new IOException(
                    "The staged replacement does not extend the exact retained history cursor");
        }
        PairedHistoryEndpoint candidate = new PairedHistoryEndpoint(
                lease.candidate, evidence);
        UnsavedPairHistoryEdge edge = new UnsavedPairHistoryEdge(
                lease.predecessorEndpoint,
                candidate,
                lease.commandLease.owner());
        requireRegisterableHistoryEdgeLocked(edge);
        return edge;
    }

    private BaselineHistoryEndpoint requireBaselineEndpointLocked(
            DesignerCommandRevision revision) throws IOException {
        if (!(unsavedHistoryCursor.endpoint()
                instanceof BaselineHistoryEndpoint baseline)
                || baseline.revision() != revision
                || staged != null) {
            throw new IOException(
                    "A branched first command requires the exact retained BASELINE cursor");
        }
        return baseline;
    }

    private void requireRegisterableHistoryEdgeLocked(
            UnsavedPairHistoryEdge edge) throws IOException {
        HistoryEdgeKey key = edge.key();
        UnsavedPairHistoryEdge existing = unsavedPairHistory.get(key);
        if (existing != null && existing != edge) {
            throw new IOException(
                    "A different unsaved pair edge already owns these revision ids");
        }
    }

    private FlutterDesignerEditorSupport.ForwardSemanticEdge
            forwardSemanticEdge(
                    DesignerCommandSessionOrchestrator.PendingCommandLease
                            commandLease) {
        return new FlutterDesignerEditorSupport.ForwardSemanticEdge(
                commandLease.predecessorRevision().revisionId(),
                commandLease.candidateRevision().revisionId(),
                forwardPresentation(commandLease),
                this);
    }

    private static String forwardPresentation(
            DesignerCommandSessionOrchestrator.PendingCommandLease
                    commandLease) {
        return DesignerCommandPresentation.title(
                commandLease.edit().forward().kind());
    }

    private ArmedForwardAdmission armForwardAdmissionLocked(
            Object leaseIdentity, boolean replacementAdmission)
            throws IOException {
        if (forwardAdmission != null) {
            throw new IOException(
                    "Another native Flutter Designer forward admission is already armed");
        }
        ArmedForwardAdmission admission = new ArmedForwardAdmission(
                Objects.requireNonNull(leaseIdentity, "leaseIdentity"),
                replacementAdmission,
                externalEventEpoch,
                epoch);
        forwardAdmission = admission;
        return admission;
    }

    private StateChange disarmForwardAdmission(Object leaseIdentity) {
        synchronized (this) {
            if (forwardAdmission != null
                    && forwardAdmission.leaseIdentity == leaseIdentity) {
                ArmedForwardAdmission admission = forwardAdmission;
                forwardAdmission = null;
                if (admission.poisoned
                        || externalEventEpoch != admission.eventTicket) {
                    staged = null;
                    unsavedPairHistory.clear();
                    unsavedHistoryCursor = null;
                    unsavedHistoryOwner = null;
                    failedSavePending = true;
                    diskBaseline = null;
                    String exactReason = admission.externalConflictReason == null
                            ? "A paired file changed during native forward admission"
                            : admission.externalConflictReason;
                    return transitionLocked(
                            PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                            exactReason);
                }
            }
            return null;
        }
    }

    private DesignerSemanticUndoableEdit.DeferredPublication
            commitInitialAdmission(
                    ArmedForwardAdmission admission,
                    PairPreparation lease,
                    LiveDartDocumentSnapshot applied,
                    PairSaveEvidence evidence,
                    UnsavedPairHistoryEdge historyEdge,
                    StagedPairAuthority targetAuthority,
                    UnsavedPairHistoryCursor targetCursor,
                    EffectsDeferral effectsDeferral) {
        StateChange[] change = new StateChange[1];
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            commandEffects = lease.commandLease
                    .adoptStagedCloseAwareDeferredEffects(
                    evidence,
                    ownerClosePending -> {
                        boolean exact = forwardAdmission == admission
                                && admission.leaseIdentity == lease
                                && !admission.replacementAdmission
                                && preparation == lease
                                && !lease.closed
                                && controller.state() == lease.expectedCurrent
                                && epoch == admission.coordinatorEpoch;
                        boolean externalPoison = admission.poisoned
                                || externalEventEpoch != admission.eventTicket;
                        forwardAdmission = null;
                        preparation = null;
                        lease.applied = applied;
                        lease.closed = true;
                        if (exact && !externalPoison
                                && !ownerClosePending) {
                            staged = targetAuthority;
                            unsavedPairHistory.put(
                                    historyEdge.key(), historyEdge);
                            unsavedHistoryCursor = targetCursor;
                            unsavedHistoryOwner = historyEdge.owner();
                            lease.staged = true;
                            change[0] = transitionLocked(
                                    PairSaveCoordinatorStatus.STAGED_PAIR,
                                    null);
                            return;
                        }
                        lease.staged = false;
                        failClosedForwardAdmissionLocked(
                                admission,
                                externalPoison,
                                ownerClosePending
                                        ? "The Designer command owner closed "
                                            + "during native forward admission"
                                        : "The exact first-command pair "
                                            + "authority changed during native "
                                            + "forward admission",
                                change);
                    },
                    lease.commandClaim);
        }
        return deferredForwardPublication(
                change[0], commandEffects, effectsDeferral);
    }

    private DesignerSemanticUndoableEdit.DeferredPublication
            commitReplacementAdmission(
                    ArmedForwardAdmission admission,
                    PairReplacement lease,
                    LiveDartDocumentSnapshot applied,
                    PairSaveEvidence evidence,
                    UnsavedPairHistoryEdge historyEdge,
                    StagedPairAuthority targetAuthority,
                    UnsavedPairHistoryCursor targetCursor,
                    EffectsDeferral effectsDeferral) {
        StateChange[] change = new StateChange[1];
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            commandEffects = lease.commandLease
                    .adoptStagedCloseAwareDeferredEffects(
                    evidence,
                    ownerClosePending -> {
                        boolean exact = forwardAdmission == admission
                                && admission.leaseIdentity == lease
                                && admission.replacementAdmission
                                && replacement == lease
                                && !lease.closed
                                && staged != null
                                && staged.proof()
                                    == lease.predecessorProof
                                && unsavedHistoryCursor
                                    == lease.predecessorCursor
                                && staged.owner()
                                    == lease.commandLease.owner()
                                && staged.revision()
                                    == lease.commandLease.predecessorRevision()
                                && controller.state()
                                    == lease.predecessorProof
                                            .loadedCurrentIdentity()
                                && epoch == admission.coordinatorEpoch;
                        boolean externalPoison = admission.poisoned
                                || externalEventEpoch != admission.eventTicket;
                        forwardAdmission = null;
                        replacement = null;
                        lease.applied = applied;
                        lease.closed = true;
                        if (exact && !externalPoison
                                && !ownerClosePending) {
                            staged = targetAuthority;
                            unsavedPairHistory.put(
                                    historyEdge.key(), historyEdge);
                            unsavedHistoryCursor = targetCursor;
                            unsavedHistoryOwner = historyEdge.owner();
                            lease.replaced = true;
                            change[0] = transitionLocked(
                                    PairSaveCoordinatorStatus.STAGED_PAIR,
                                    null);
                            return;
                        }
                        lease.replaced = false;
                        failClosedForwardAdmissionLocked(
                                admission,
                                externalPoison,
                                ownerClosePending
                                        ? "The Designer command owner closed "
                                            + "during native forward admission"
                                        : "The exact replacement pair "
                                            + "authority changed during native "
                                            + "forward admission",
                                change);
                    },
                    lease.commandClaim);
        }
        return deferredForwardPublication(
                change[0], commandEffects, effectsDeferral);
    }

    private DesignerSemanticUndoableEdit.DeferredPublication
            commitBaselineReplacementAdmission(
                    ArmedForwardAdmission admission,
                    PairReplacement lease,
                    LiveDartDocumentSnapshot applied,
                    UnsavedPairHistoryEdge historyEdge,
                    UnsavedPairHistoryCursor targetCursor,
                    EffectsDeferral effectsDeferral) {
        StateChange[] change = new StateChange[1];
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            commandEffects = lease.commandLease
                    .adoptExactTargetCloseAwareDeferredEffects(
                    lease.candidate,
                    ownerClosePending -> {
                        boolean exact = forwardAdmission == admission
                                && admission.leaseIdentity == lease
                                && admission.replacementAdmission
                                && replacement == lease
                                && !lease.closed
                                && lease.baselineTarget()
                                && staged != null
                                && staged.proof()
                                    == lease.predecessorProof
                                && unsavedHistoryCursor
                                    == lease.predecessorCursor
                                && staged.owner()
                                    == lease.commandLease.owner()
                                && staged.revision()
                                    == lease.commandLease.predecessorRevision()
                                && controller.state()
                                    == lease.predecessorProof
                                            .loadedCurrentIdentity()
                                && epoch == admission.coordinatorEpoch;
                        boolean externalPoison = admission.poisoned
                                || externalEventEpoch != admission.eventTicket;
                        forwardAdmission = null;
                        replacement = null;
                        lease.applied = applied;
                        lease.closed = true;
                        if (exact && !externalPoison
                                && !ownerClosePending) {
                            staged = null;
                            unsavedPairHistory.put(
                                    historyEdge.key(), historyEdge);
                            unsavedHistoryCursor = targetCursor;
                            unsavedHistoryOwner = historyEdge.owner();
                            sourceDirty = true;
                            lease.replaced = true;
                            change[0] = transitionLocked(
                                    PairSaveCoordinatorStatus.DIRTY_SOURCE,
                                    null);
                            return;
                        }
                        lease.replaced = false;
                        failClosedForwardAdmissionLocked(
                                admission,
                                externalPoison,
                                ownerClosePending
                                        ? "The Designer command owner closed during exact baseline admission"
                                        : "The exact baseline replacement authority changed during native forward admission",
                                change);
                    },
                    lease.commandClaim);
        }
        return deferredForwardPublication(
                change[0], commandEffects, effectsDeferral);
    }

    private void failClosedForwardAdmissionLocked(
            ArmedForwardAdmission admission,
            boolean externalPoison,
            String fallbackReason,
            StateChange[] change) {
        staged = null;
        unsavedPairHistory.clear();
        unsavedHistoryCursor = null;
        unsavedHistoryOwner = null;
        sourceDirty = true;
        failedSavePending = true;
        diskBaseline = null;
        String exactReason = admission.externalConflictReason == null
                ? fallbackReason : admission.externalConflictReason;
        PairSaveCoordinatorStatus status = externalPoison
                ? PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                : PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
        change[0] = transitionLocked(status, exactReason);
    }

    private DesignerSemanticUndoableEdit.DeferredPublication
            deferredForwardPublication(
                    StateChange change,
                    DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                            commandEffects,
                    EffectsDeferral effectsDeferral) {
        return () -> {
            try {
                // drainEffects always publishes command callbacks before the
                // pair callbacks represented by the same joint state change.
                publishEffects(change, commandEffects);
            } finally {
                // Combined invokes this after the native manager barrier and
                // before its one presentation notification. The outer
                // try-with-resources close is deliberately idempotent.
                effectsDeferral.close();
            }
        };
    }

    private DesignerSemanticUndoableEdit.DeferredPublication
            commitHistoryTransition(ActiveHistoryTransition transition)
            throws IOException {
        FlutterDesignerEditorSupport.NativeHistoryReplay replay =
                transition.nativeReplay;
        boolean verified = false;
        try {
            editor.verifyNativeReplayAndFinalize(
                    transition.sourceCursor.liveIdentity().documentIdentity(),
                    transition.targetEndpoint.dartBytes(),
                    freshTarget -> finalizeHistoryCommitWithinDocumentLock(
                            transition, freshTarget));
            verified = true;
        } finally {
            if (verified && replay != null) {
                replay.close();
            }
            if (verified) {
                // A failed verification deliberately retains the token across
                // the semantic edit's raw inverse. abort() verifies and closes
                // it only after exact predecessor recovery.
                transition.nativeReplay = null;
            }
        }
        synchronized (this) {
            if (transition.resolution
                    != HistoryTransitionResolution.COMMITTED) {
                throw new IOException(
                        "The native history move did not publish one exact pair/command target");
            }
        }
        return transition::publishAfterNativeBarrier;
    }

    /** Runs under the fresh native target's exact Dart document lock. */
    private void finalizeHistoryCommitWithinDocumentLock(
            ActiveHistoryTransition transition,
            LiveDartDocumentSnapshot freshTarget) throws IOException {
        StagedPairProof targetProof = bindHistoryEndpoint(
                transition.targetEndpoint, freshTarget);
        StateChange[] change = new StateChange[1];
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            requireActiveHistoryTransitionLocked(transition);
            requireHistoryCursorLocked(
                    transition.edge, transition.sourceEndpoint);
            if (externalEventEpoch != transition.eventTicket
                    || epoch != transition.coordinatorEpoch
                    || unsavedHistoryCursor != transition.sourceCursor
                    || !sameDiskBaseline(
                            transition.edge, transition.observedDisk)
                    || !transition.commandLease.ownsExactActiveTransition()
                    || transition.commandLease.candidateRevision()
                        != transition.targetEndpoint.revision()
                    || controller.state()
                        != transition.targetEndpoint.currentIdentity()) {
                throw new IOException(
                        "Exact pair, command, model, disk or epoch authority "
                        + "changed during the native history move");
            }
            boolean pairedTarget = targetProof != null;
            boolean sourceOverlayTarget = transition.targetEndpoint
                    instanceof SourceOverlayBaselineHistoryEndpoint;
            // An UNCHANGED Source save may establish a newer native savepoint
            // at byte-identical BASELINE content. Replaying the semantic edge
            // can then reach the same bytes below that savepoint, which is
            // physically dirty even though no Source-overlay endpoint is
            // needed. CES already exposes that distinction here; a genuinely
            // clean target publishes its delayed unmodified callback after
            // the document-locked delegate returns.
            boolean expectedDirty = pairedTarget || sourceOverlayTarget
                    || editor.sourceModified();
            // CES may publish its matching unmodified callback only after the
            // native Undo manager leaves this document-locked delegate. Dirty
            // targets, however, must already have been admitted before their
            // first mutation.
            if (expectedDirty && !editor.sourceModified()) {
                throw new IOException(
                        "CES savepoint state differs from the exact Designer history target");
            }
            commandEffects = transition.commandLease
                    .adoptExactTargetDeferredEffects(
                            transition.targetEndpoint.revision(),
                            () -> {
                                if (pairedTarget) {
                                    staged = new StagedPairAuthority(
                                            targetProof,
                                            transition.edge.owner(),
                                            transition.targetEndpoint.revision());
                                } else {
                                    staged = null;
                                }
                                unsavedHistoryCursor =
                                        new UnsavedPairHistoryCursor(
                                                transition.targetEndpoint,
                                                freshTarget,
                                                targetProof);
                                sourceDirty = expectedDirty;
                                historyTransition = null;
                                transition.resolution =
                                        HistoryTransitionResolution.COMMITTED;
                                change[0] = transitionLocked(
                                        pairedTarget
                                                ? PairSaveCoordinatorStatus.STAGED_PAIR
                                                : expectedDirty
                                                    ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                                    : PairSaveCoordinatorStatus.CLEAN,
                                        null);
                            },
                            transition.commandClaim);
        }
        // Effects stay queued until the semantic edit's native action barrier
        // invokes the returned DeferredPublication.
        publishEffects(change[0], commandEffects);
    }

    private StagedPairProof bindHistoryEndpoint(
            HistoryEndpoint endpoint,
            LiveDartDocumentSnapshot freshTarget) throws IOException {
        if (!Arrays.equals(endpoint.dartBytes(),
                freshTarget.markerBearingUtf8())) {
            throw new IOException(
                    "The native history target differs from its exact Dart endpoint bytes");
        }
        if (endpoint instanceof MetadataHistoryEndpoint
                || endpoint instanceof BaselineHistoryEndpoint
                || endpoint instanceof SourceOverlayBaselineHistoryEndpoint) {
            return null;
        }
        if (endpoint instanceof ReanchoredPairedHistoryEndpoint paired) {
            PreparedDesignerPair prepared = paired.endpointPair();
            return new SavedHistoryProof(
                    paired.seed().kind(),
                    paired.currentIdentity(),
                    prepared,
                    freshTarget,
                    paired.baselineDartBytes(),
                    paired.baselineFdBytes(),
                    paired.dartBytes());
        }
        PairedHistoryEndpoint paired = (PairedHistoryEndpoint) endpoint;
        PairSaveEvidenceResult rebound = PairSaveEvidenceGate.bindApplied(
                paired.seed().analyzedCandidateIdentity(), freshTarget);
        if (!(rebound instanceof PairSaveEvidenceResult.Ready ready)) {
            PairSaveEvidenceDiagnostic diagnostic =
                    rebound.diagnostics().getFirst();
            throw new IOException(
                    "Cannot rebind analyzed unsaved history endpoint: "
                    + diagnostic.code() + " at " + diagnostic.subject()
                    + ": " + diagnostic.message());
        }
        PairSaveEvidence evidence = ready.evidence();
        if (evidence.analyzedCandidateIdentity()
                    != paired.seed().analyzedCandidateIdentity()
                || evidence.preparedPairIdentity()
                    != endpoint.revision().preparedPair().orElse(null)) {
            throw new IOException(
                    "The rebound history evidence lost its exact analyzer or revision identity");
        }
        return new AnalyzedStagedPairProof(evidence);
    }

    private void abortHistoryTransition(ActiveHistoryTransition transition) {
        synchronized (this) {
            if (transition.resolution != HistoryTransitionResolution.ACTIVE) {
                return;
            }
        }
        try {
            verifyRecoveredHistorySourceAndAbort(transition);
        } catch (IOException | RuntimeException | Error failure) {
            invalidateHistoryTransition(transition, failure);
        }
    }

    private void verifyRecoveredHistorySourceAndAbort(
            ActiveHistoryTransition transition) throws IOException {
        FlutterDesignerEditorSupport.NativeHistoryReplay recoveryReplay =
                transition.nativeReplay;
        if (recoveryReplay == null) {
            recoveryReplay = editor.beginNativeHistoryReplay(
                    transition.sourceCursor.liveIdentity().documentIdentity());
            // Ownership must move to the active transition immediately. If
            // verification below fails, invalidateHistoryTransition is the
            // sole fail-closed cleanup path and must be able to close this
            // newly opened EDT-local context.
            transition.nativeReplay = recoveryReplay;
        }
        boolean verified = false;
        try {
            editor.verifyNativeReplayAndFinalize(
                    transition.sourceCursor.liveIdentity().documentIdentity(),
                    transition.sourceEndpoint.dartBytes(),
                    freshSource -> finalizeHistoryAbortWithinDocumentLock(
                            transition, freshSource));
            verified = true;
        } finally {
            if (verified) {
                recoveryReplay.close();
                transition.nativeReplay = null;
            }
        }
    }

    /** Exact delegate recovery retained the current endpoint; rebind it only. */
    private void finalizeHistoryAbortWithinDocumentLock(
            ActiveHistoryTransition transition,
            LiveDartDocumentSnapshot freshSource) throws IOException {
        StagedPairProof reboundSource = bindHistoryEndpoint(
                transition.sourceEndpoint, freshSource);
        StateChange change;
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects;
        synchronized (this) {
            requireActiveHistoryTransitionLocked(transition);
            requireHistoryCursorLocked(
                    transition.edge, transition.sourceEndpoint);
            if (externalEventEpoch != transition.eventTicket
                    || epoch != transition.coordinatorEpoch
                    || unsavedHistoryCursor != transition.sourceCursor
                    || !transition.commandLease.ownsExactActiveTransition()
                    || controller.state()
                        != transition.sourceEndpoint.currentIdentity()) {
                throw new IOException(
                        "The current pair authority changed while native history was recovered");
            }
            boolean pairedSource = reboundSource != null;
            boolean sourceOverlaySource = transition.sourceEndpoint
                    instanceof SourceOverlayBaselineHistoryEndpoint;
            boolean expectedDirty = pairedSource || sourceOverlaySource
                    || editor.sourceModified();
            if (expectedDirty && !editor.sourceModified()) {
                throw new IOException(
                        "CES did not retain the modified state of the recovered Designer source");
            }
            commandEffects = transition.commandLease
                    .abortToExactPredecessorDeferredEffects(
                            transition.commandClaim);
            if (pairedSource) {
                staged = new StagedPairAuthority(
                        reboundSource,
                        transition.edge.owner(),
                        transition.sourceEndpoint.revision());
            } else {
                staged = null;
            }
            unsavedHistoryCursor = new UnsavedPairHistoryCursor(
                    transition.sourceEndpoint, freshSource, reboundSource);
            sourceDirty = expectedDirty;
            historyTransition = null;
            transition.resolution = HistoryTransitionResolution.ABORTED;
            change = transitionLocked(
                    pairedSource
                            ? PairSaveCoordinatorStatus.STAGED_PAIR
                            : expectedDirty
                                ? PairSaveCoordinatorStatus.DIRTY_SOURCE
                                : PairSaveCoordinatorStatus.CLEAN,
                    null);
        }
        publishEffects(change, commandEffects);
        enqueueHistoryPublication(transition::publishAfterNativeBarrier);
    }

    private void invalidateHistoryTransition(
            ActiveHistoryTransition transition, Throwable failure) {
        if (transition.nativeReplay != null) {
            try {
                transition.nativeReplay.close();
            } catch (RuntimeException closeFailure) {
                if (failure != closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            } finally {
                transition.nativeReplay = null;
            }
        }
        DesignerCommandSessionOrchestrator.DeferredLeaseEffects commandEffects =
                DesignerCommandSessionOrchestrator.DeferredLeaseEffects.none();
        try {
            commandEffects = transition.commandLease
                    .invalidateClaimedDeferredEffects(
                            transition.commandClaim);
        } catch (RuntimeException invalidationFailure) {
            if (failure != invalidationFailure) {
                failure.addSuppressed(invalidationFailure);
            }
        }
        StateChange change = null;
        synchronized (this) {
            if (historyTransition == transition) {
                historyTransition = null;
            }
            if (transition.resolution == HistoryTransitionResolution.ACTIVE) {
                transition.resolution = HistoryTransitionResolution.INVALIDATED;
                staged = null;
                unsavedHistoryCursor = null;
                unsavedHistoryOwner = null;
                sourceDirty = editor.sourceModified();
                failedSavePending = true;
                diskBaseline = null;
                if (!conflictStatusLocked()) {
                    change = transitionLocked(
                            PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                            "Unsaved Designer history could not recover one exact "
                            + "pair/command cursor: " + reason(failure));
                }
            }
        }
        publishEffects(change, commandEffects);
        enqueueHistoryPublication(transition::publishAfterNativeBarrier);
    }

    private void enqueueHistoryPublication(
            DesignerSemanticUndoableEdit.DeferredPublication publication) {
        try {
            dataObject.getCombinedUndoRedo()
                    .enqueueSemanticPublication(publication);
        } catch (RuntimeException unavailableBarrier) {
            // Direct package tests may exercise the replay controller without
            // routing through the outward bridge. Preserve the production
            // post-action ordering by scheduling presentation on the EDT tail.
            EventQueue.invokeLater(publication::publish);
        }
    }

    private void verifyPreparedBaseline(
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        if (controller.state() != current) {
            throw new IOException(
                    "Cannot prepare Flutter Designer pair from a stale loaded model");
        }
        if (current.decoded().original() != prepared.baselineFd()
                || !current.decoded().original().contentEquals(
                        prepared.baselineFdBytes())) {
            throw new IOException(
                    "Cannot prepare Flutter Designer pair from a different .fd baseline");
        }
        if (current.sourceIntegrity().isEmpty()
                || current.sourceIntegrity().orElseThrow().original().isEmpty()
                || !current.sourceIntegrity().orElseThrow().original().orElseThrow()
                        .contentEquals(prepared.baselineDartBytes())) {
            throw new IOException(
                    "Cannot prepare Flutter Designer pair from a different Dart baseline");
        }
        if (!Arrays.equals(
                initialLive.markerBearingUtf8(), prepared.liveDartBytes())) {
            throw new IOException(
                    "Cannot prepare Flutter Designer pair: its live Dart snapshot "
                    + "differs from the prepared transition input");
        }
    }

    private InitialPreparationInputs verifyInitialPreparationInputs(
            FlutterDesignerDocumentState.Current current,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            LiveDartDocumentSnapshot initialLive) throws IOException {
        DesignerCommandRevision predecessor = commandLease.predecessorRevision();
        DesignerCommandRevision candidate = commandLease.candidateRevision();
        boolean metadataPredecessor = retainsMetadataHistoryLocked(commandLease.owner(), predecessor);
        if (!commandLease.ownsExactActiveTransition()
                || commandLease.kind()
                    != DesignerCommandSessionOrchestrator.PendingTransitionKind.APPLY) {
            throw new IOException(
                    "Cannot prepare the first Flutter Designer revision: the "
                    + "command lease is not the exact active APPLY transition");
        }
        if (controller.state() != current
                || commandLease.catalogIdentity() != current.catalog()) {
            throw new IOException(
                    "Cannot prepare the first Flutter Designer revision: the "
                    + "loaded Current or catalog identity differs from the command session");
        }
        if ((!metadataPredecessor && predecessor.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE)
                || candidate.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || candidate.preparedPair().isEmpty()) {
            throw new IOException(
                    "The first Flutter Designer transition requires exact BASELINE B "
                    + "and PAIRED C1 command revisions");
        }
        PreparedDesignerPair prepared = candidate.preparedPair().orElseThrow();
        if ((!metadataPredecessor && (predecessor.fdSnapshot() != current.decoded().original()
                || predecessor.sourceIntegrity()
                    != current.sourceIntegrity().orElse(null)))
                || current.threeWayIntegrity().isEmpty()
                || prepared.baselineFd() != current.decoded().original()
                || prepared.dartTransition().baseline()
                    != current.threeWayIntegrity().orElseThrow()
                || !metadataPredecessor && !Arrays.equals(
                        predecessor.fdBytes(), prepared.baselineFdBytes())
                || !Arrays.equals(
                        predecessor.dartCandidateBytes(),
                        prepared.baselineDartBytes())
                || !Arrays.equals(
                        candidate.fdBytes(), prepared.prospectiveFdBytes())
                || !Arrays.equals(
                        candidate.dartCandidateBytes(),
                        prepared.prospectiveDartBytes())) {
            throw new IOException(
                    "Cannot prepare the first Flutter Designer revision: the exact "
                    + "B/C1 command identities differ from the loaded and prepared pair");
        }
        verifyPreparedBaseline(current, prepared, initialLive);
        return new InitialPreparationInputs(predecessor, candidate, prepared);
    }

    private ReplacementInputs verifyReplacementInputs(
            StagedCommandSource expectedSource,
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease)
            throws IOException {
        StagedPairProof expectedStaged = expectedSource.predecessorProof();
        FlutterDesignerDocumentState.Current current =
                expectedStaged.loadedCurrentIdentity();
        DesignerCommandRevision predecessor = commandLease.predecessorRevision();
        DesignerCommandRevision candidate = commandLease.candidateRevision();
        if (!commandLease.ownsExactActiveTransition()
                || staged == null
                || staged != expectedSource.authorityIdentity()
                || staged.proof() != expectedStaged
                || unsavedHistoryCursor
                    != expectedSource.cursorIdentity()
                || staged.owner() != commandLease.owner()
                || staged.revision() != predecessor) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: the "
                    + "pending command lease is not the exact active C1 authority "
                    + "and C1 to C2 transition");
        }
        if (controller.state() != current
                || commandLease.catalogIdentity() != current.catalog()) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: the "
                    + "loaded Current or catalog identity differs from the command session");
        }
        if (predecessor.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || predecessor.preparedPair().isEmpty()) {
            throw new IOException(
                    "A staged replacement requires one exact PAIRED predecessor revision");
        }

        PreparedDesignerPair predecessorPair = commandLease
                .physicalPredecessorPairIdentity()
                .orElseGet(() -> predecessor.preparedPair().orElseThrow());
        if (predecessorPair != expectedStaged.preparedPairIdentity()
                || !Arrays.equals(
                        predecessorPair.prospectiveDartBytes(),
                        expectedStaged.candidateDartBytes())
                || !Arrays.equals(
                        predecessor.fdBytes(),
                        predecessorPair.prospectiveFdBytes())
                || !predecessorPair.prospectiveDocument()
                        .equals(predecessor.document())) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: the "
                    + "pending predecessor is not the exact staged C1 pair");
        }

        if (candidate.persistenceKind()
                == DesignerRevisionPersistenceKind.BASELINE) {
            if (candidate.preparedPair().isPresent()
                    || candidate.sourceTransition().isPresent()
                    || candidate.fdSnapshot()
                        != expectedStaged.preparedPairIdentity().baselineFd()
                    || !candidate.document().equals(
                            expectedStaged.preparedPairIdentity()
                                    .baselineDocument())
                    || !Arrays.equals(
                            candidate.dartCandidateBytes(),
                            predecessorPair.liveDartBytes())
                    || !Arrays.equals(
                            candidate.dartCandidateBytes(),
                            expectedStaged.baselineDartBytes())
                    || !Arrays.equals(
                            candidate.fdBytes(),
                            expectedStaged.baselineFdBytes())) {
                throw new IOException(
                        "Cannot return the staged Flutter Designer revision to "
                        + "BASELINE: the target differs from the exact durable pair");
            }
            verifyReplacementPhysicalBudgetLocked(commandLease, null);
            return new ReplacementInputs(
                    predecessor, candidate, null, null);
        }
        if (candidate.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || candidate.preparedPair().isEmpty()) {
            throw new IOException(
                    "A staged replacement target must be an exact PAIRED or BASELINE revision");
        }

        PreparedDesignerPair candidatePair = candidate.preparedPair().orElseThrow();
        if (candidatePair != candidate.preparedPair().orElseThrow()
                || candidatePair.baselineFd()
                    != expectedStaged.preparedPairIdentity().baselineFd()
                || candidatePair.dartTransition().baseline()
                    != expectedStaged.preparedPairIdentity()
                            .dartTransition().baseline()
                || !Arrays.equals(
                        candidatePair.liveDartBytes(),
                        predecessorPair.liveDartBytes())
                || candidatePair.baselineFd()
                    != current.decoded().original()
                || current.threeWayIntegrity().isEmpty()
                || candidatePair.dartTransition().baseline()
                    != current.threeWayIntegrity().orElseThrow()
                || !Arrays.equals(
                        candidatePair.baselineFdBytes(),
                        expectedStaged.baselineFdBytes())
                || !Arrays.equals(
                        candidatePair.baselineDartBytes(),
                        expectedStaged.baselineDartBytes())
                || !Arrays.equals(
                        candidatePair.prospectiveFdBytes(),
                        candidate.fdBytes())
                || !Arrays.equals(
                        candidatePair.prospectiveDartBytes(),
                        candidate.dartCandidateBytes())) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: C2 "
                    + "does not retain the exact durable B and command candidate identities");
        }

        DartThreeWayIntegrityResult liveBaseline =
                new DartThreeWayIntegrityGate().evaluate(
                        predecessorPair.dartTransition().candidateIntegrity(),
                        predecessor.document().source(),
                        predecessor.generation());
        if (!liveBaseline.onDiskThreeWayMatch()) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: C1 "
                    + "does not form an exact actual/declared/generated live baseline");
        }
        DartSourceTransitionResult liveResult =
                new DartSourceTransitionPlanner().plan(
                        liveBaseline,
                        expectedStaged.candidateDartBytes(),
                        predecessor.document().source(),
                        candidate.generation(),
                        predecessorPair.dartTransition().userSourceProjection().inverse()
                                .then(candidatePair.dartTransition().userSourceProjection()));
        boolean unchanged = liveResult.status() == DartSourceTransitionStatus.NO_CHANGES;
        if (!unchanged && (liveResult.status() != DartSourceTransitionStatus.READY
                || liveResult.plan().isEmpty())) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: C1 "
                    + "cannot be transformed deterministically to C2 ("
                    + liveResult.status() + ")");
        }
        if (!Arrays.equals(
                    unchanged ? expectedStaged.candidateDartBytes()
                            : liveResult.plan().orElseThrow().candidateBytes(),
                    candidatePair.prospectiveDartBytes())
                || !(unchanged ? predecessor.document().source()
                        : liveResult.plan().orElseThrow().prospectiveDescriptor()).equals(
                        candidatePair.prospectiveDocument().source())) {
            throw new IOException(
                    "Cannot replace the staged Flutter Designer revision: the "
                    + "live C1 to C2 transition differs from the durable-B candidate");
        }
        verifyReplacementPhysicalBudgetLocked(commandLease, candidatePair);
        return new ReplacementInputs(
                predecessor, candidate, candidatePair, liveResult);
    }

    /**
     * The pure command session sees one logical revision per id; the pair
     * coordinator additionally owns every retained physical Source envelope.
     * Count the latter before analyzer admission or any CES mutation.
     */
    private void verifyReplacementPhysicalBudgetLocked(
            DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
            PreparedDesignerPair candidatePair) throws IOException {
        RetainedPhysicalEndpointBudget budget =
                new RetainedPhysicalEndpointBudget(
                        commandLease.maxRetainedPairBytes());
        for (UnsavedPairHistoryEdge edge : unsavedPairHistory.values()) {
            budget.retain(edge.before());
            budget.retain(edge.after());
        }
        if (unsavedHistoryCursor != null) {
            budget.retain(unsavedHistoryCursor.endpoint());
        }
        if (candidatePair == null) {
            DesignerCommandRevision candidate = commandLease.candidateRevision();
            budget.retainBytes(
                    candidate,
                    candidate.dartCandidateBytes(),
                    candidate.fdBytes(),
                    candidate.userSourceProjection().retainedBytes());
        } else {
            budget.retainBytes(
                    candidatePair,
                    candidatePair.prospectiveDartBytes(),
                    candidatePair.prospectiveFdBytes(),
                    candidatePair.dartTransition().userSourceProjection().retainedBytes());
        }
    }

    private void ensureStageableLocked() throws IOException {
        if (sourceRestage != null || preparation != null || replacement != null || staged != null
                || historyTransition != null || forwardAdmission != null
                || activePairSave != null
                || activeSourceSave != null || activeFdOnlySave != null
                || activePairPathOperation != null) {
            throw new IOException(
                    "Only one Flutter Designer preparation or save may own the pair");
        }
        rejectConflictSaveLocked();
    }

    private void ensureReplaceableLocked(StagedCommandSource expectedSource)
            throws IOException {
        if (sourceRestage != null || preparation != null || replacement != null
                || historyTransition != null
                || activePairSave != null || activeSourceSave != null
                || activeFdOnlySave != null || activePairPathOperation != null
                || expectedSource.coordinatorIdentity()
                    != this
                || staged != expectedSource.authorityIdentity()
                || unsavedHistoryCursor
                    != expectedSource.cursorIdentity()
                || staged == null
                || staged.proof()
                    != expectedSource.predecessorProof()
                || staged.owner()
                    != expectedSource.ownerIdentity()
                || staged.revision()
                    != expectedSource.predecessorRevision()
                || externalEventEpoch
                    != expectedSource.externalEventEpoch()
                || epoch != expectedSource.coordinatorEpoch()
                || sourceStateEpoch
                    != expectedSource.sourceStateEpoch()
                || editor.liveDocumentVersion(
                        expectedSource.liveIdentity().documentIdentity())
                    != expectedSource.liveIdentity().documentVersion()) {
            throw new IOException(
                    "Only the exact currently staged Flutter Designer pair may "
                    + "be replaced, and no other preparation or save may be active");
        }
        rejectConflictSaveLocked();
    }

    private void rejectConflictSaveLocked() throws IOException {
        if (conflictStatusLocked()) {
            throw new IOException(
                    "Cannot save the Flutter Designer pair: "
                    + state.reason().orElse("the pair is in conflict"));
        }
    }

    private boolean conflictStatusLocked() {
        return isStickyConflict(state.status());
    }

    static boolean isStickyConflict(PairSaveCoordinatorStatus status) {
        return status == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                || status == PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
    }

    private void requireActivePreparationLocked(PairPreparation lease)
            throws IOException {
        if (preparation != lease || lease.closed) {
            throw new IOException(
                    "The Flutter Designer preparation lease is no longer active");
        }
    }

    private void requireActiveReplacementLocked(PairReplacement lease)
            throws IOException {
        if (replacement != lease || lease.closed) {
            throw new IOException(
                    "The staged Flutter Designer replacement lease is no longer active");
        }
    }

    private void requireActiveReplacementUnchecked(PairReplacement lease) {
        if (replacement != lease || lease.closed) {
            throw new IllegalStateException(
                    "The staged Flutter Designer replacement lease is no longer active");
        }
    }

    private void requireActiveFdOnlySaveLocked(ActiveFdOnlySave attempt)
            throws IOException {
        if (activeFdOnlySave != attempt) {
            throw new IOException(
                    "The Designer-only save no longer owns its exact coordinator reservation");
        }
    }

    private boolean isActivePreparation(PairPreparation lease) {
        synchronized (this) {
            return preparation == lease && !lease.closed;
        }
    }

    private boolean isActiveReplacement(PairReplacement lease) {
        synchronized (this) {
            return replacement == lease && !lease.closed;
        }
    }

    /**
     * Serializes lease operations without ever retaining a lease monitor while
     * an operation crosses the EDT or performs analysis/path I/O. A concurrent
     * caller is rejected immediately instead of blocking the EDT behind a
     * worker that is itself waiting for the EDT.
     */
    private boolean beginPreparationOperation(
            PairPreparation lease, String operation, boolean closedIsNoOp)
            throws IOException {
        synchronized (this) {
            if (closedIsNoOp
                    && (lease.staged || lease.closed && preparation != lease)) {
                return false;
            }
            requireActivePreparationLocked(lease);
            if (lease.operation != null) {
                throw new IOException(
                        "Cannot " + operation + " the prepared Flutter Designer "
                        + "pair while lease operation '" + lease.operation
                        + "' is still running");
            }
            lease.operation = operation;
            return true;
        }
    }

    private void endPreparationOperation(PairPreparation lease, String operation) {
        synchronized (this) {
            if (operation.equals(lease.operation)) {
                lease.operation = null;
            }
        }
    }

    private boolean beginReplacementOperation(
            PairReplacement lease, String operation, boolean closedIsNoOp)
            throws IOException {
        synchronized (this) {
            if (closedIsNoOp
                    && (lease.replaced
                        || lease.closed && replacement != lease)) {
                return false;
            }
            requireActiveReplacementLocked(lease);
            if (lease.operation != null) {
                throw new IOException(
                        "Cannot " + operation + " the staged Flutter Designer "
                        + "replacement while lease operation '" + lease.operation
                        + "' is still running");
            }
            lease.operation = operation;
            return true;
        }
    }

    private void endReplacementOperation(
            PairReplacement lease, String operation) {
        synchronized (this) {
            if (operation.equals(lease.operation)) {
                lease.operation = null;
            }
        }
    }

    private void requireOutputOwner(Thread owner) throws IOException {
        if (Thread.currentThread() != owner) {
            throw new IOException(
                    "The Dart serializer is running outside its save-owner thread");
        }
    }

    private Path trustedFlutterSdkRoot() throws IOException {
        var status = new FlutterToolchainService().resolve();
        var flutter = status.flutterSdk().orElseThrow(() -> new IOException(
                "Cannot authorize Flutter Designer pair Save: "
                + status.flutterMessage()));
        Path root = flutter.home();
        try {
            return root.toRealPath();
        } catch (IOException | RuntimeException failure) {
            throw new IOException(
                    "Cannot resolve the trusted Flutter SDK root " + root
                    + ": " + reason(failure), failure);
        }
    }

    private static Path realLocalPath(FileObject file, String label)
            throws IOException {
        File local = FileUtil.toFile(file);
        if (local == null) {
            throw new IOException("Cannot resolve local path for " + label
                    + " " + file.getPath());
        }
        return local.toPath().toRealPath();
    }

    private DiskBaseline readDiskBaseline() throws IOException {
        return new DiskBaseline(
                readBounded(
                        dartFile,
                        MAX_SOURCE_PERSISTENCE_BYTES,
                        "paired Dart source"),
                readBounded(
                        designerFile,
                        MAX_MODEL_PERSISTENCE_BYTES,
                        "paired .fd model"));
    }

    private static byte[] readBounded(
            FileObject file, int maximumBytes, String label) throws IOException {
        long declaredSize = file.getSize();
        if (declaredSize > maximumBytes) {
            throw new IOException("The " + label + " contains " + declaredSize
                    + " bytes, exceeding the " + maximumBytes
                    + " byte pair-persistence safety limit");
        }
        int initialCapacity = declaredSize > 0
                ? (int) Math.min(declaredSize, maximumBytes)
                : Math.min(8_192, maximumBytes);
        try (InputStream input = file.getInputStream();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream(initialCapacity)) {
            byte[] chunk = new byte[8_192];
            int read;
            while ((read = input.read(chunk)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (output.size() > maximumBytes - read) {
                    throw new IOException("The " + label + " grew beyond the "
                            + maximumBytes
                            + " byte pair-persistence safety limit while reading");
                }
                output.write(chunk, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static byte[] readBounded(
            InputStream input, int maximumBytes, String label)
            throws IOException {
        Objects.requireNonNull(input, "input");
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(
                Math.min(8_192, maximumBytes))) {
            byte[] chunk = new byte[8_192];
            int read;
            while ((read = input.read(chunk)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (output.size() > maximumBytes - read) {
                    throw new IOException("The " + label + " exceeds the "
                            + maximumBytes
                            + " byte pair-persistence safety limit");
                }
                output.write(chunk, 0, read);
            }
            return output.toByteArray();
        }
    }

    private void rejectForeignSaveCookie() throws IOException {
        SaveCookie existing = dataObject.getCookie(SaveCookie.class);
        if (existing != null && existing != pairSaveCookie) {
            throw new IOException(
                    "Cannot modify " + dartFile.getNameExt()
                    + ": another SaveCookie already owns the paired object");
        }
    }

    private boolean saveCookieRequiredLocked() {
        return sourceDirty || failedSavePending || staged != null || retainedMetadataCursorLocked()
                || activePairSave != null || activeSourceSave != null
                || preparation != null && preparation.applied != null
                || replacement != null
                || !suppressConflictSaveCookie
                    && (state.status() == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                        || state.status()
                            == PairSaveCoordinatorStatus.RECOVERY_CONFLICT);
    }

    private boolean retainedMetadataCursorLocked() {
        return unsavedHistoryCursor != null
                && unsavedHistoryCursor.endpoint() instanceof MetadataHistoryEndpoint;
    }

    /**
     * Framework callbacks run without either coordinator/effects monitor.
     * Reentrant or concurrent publications enqueue and return; the active
     * drainer preserves ordering without making a listener part of a lock.
     */
    private void publishEffects(StateChange change) {
        publishEffects(change, null);
    }

    private void publishEffects(
            StateChange change,
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects) {
        boolean drain;
        synchronized (effectsMonitor) {
            effectsQueue.addLast(new EffectsPublication(change, commandEffects));
            drain = !effectsDraining && effectsDeferralDepth == 0;
            if (drain) {
                effectsDraining = true;
            }
        }
        if (drain) {
            drainEffects();
        }
    }

    private void drainEffects() {
        while (true) {
            EffectsPublication publication;
            synchronized (effectsMonitor) {
                publication = effectsQueue.pollFirst();
                if (publication == null) {
                    effectsDraining = false;
                    break;
                }
            }
            if (publication.commandEffects() != null) {
                try {
                    publication.commandEffects().publish();
                } catch (RuntimeException | Error callbackFailure) {
                    LOGGER.log(Level.WARNING,
                            "A Designer command presentation callback failed",
                            callbackFailure);
                }
            }
            try {
                publishOne(publication.change());
            } catch (RuntimeException | Error callbackFailure) {
                LOGGER.log(Level.WARNING,
                        "A NetBeans pair-save presentation callback failed",
                        callbackFailure);
            }
        }
    }

    /** Defers framework callbacks until the enclosing document lock is gone. */
    private EffectsDeferral deferEffects() throws IOException {
        synchronized (effectsMonitor) {
            if (effectsDraining) {
                throw new IOException(
                        "Cannot enter the Flutter Designer document-atomic edge "
                        + "while a NetBeans state/cookie callback is active");
            }
            effectsDeferralDepth++;
        }
        return new EffectsDeferral();
    }

    private void endEffectsDeferral() {
        boolean drain;
        synchronized (effectsMonitor) {
            if (effectsDeferralDepth <= 0) {
                throw new IllegalStateException(
                        "Flutter Designer effects deferral is unbalanced");
            }
            effectsDeferralDepth--;
            drain = effectsDeferralDepth == 0
                    && !effectsDraining
                    && !effectsQueue.isEmpty();
            if (drain) {
                effectsDraining = true;
            }
        }
        if (drain) {
            drainEffects();
        }
    }

    private void publishOne(StateChange change) {
        boolean currentChange = false;
        if (change != null) {
            synchronized (this) {
                currentChange = change.current().epoch() == state.epoch();
            }
        }
        try {
            if (currentChange) {
                changes.firePropertyChange(
                        PROP_STATE, change.previous(), change.current());
            }
        } catch (RuntimeException listenerFailure) {
            LOGGER.log(Level.WARNING,
                    "A pair-save state listener failed", listenerFailure);
        } finally {
            // A failing state listener must not prevent the matching dirty/
            // SaveCookie reconciliation for the already-committed state.
            reconcileSaveCookieWithinEffects();
        }
    }

    /** CookieSet/DataObject callbacks deliberately run without this monitor. */
    private void reconcileSaveCookieWithinEffects() {
        boolean required;
        synchronized (this) {
            required = saveCookieRequiredLocked();
        }
        SaveCookie existing = dataObject.getCookie(SaveCookie.class);
        try {
            if (required) {
                if (existing == null) {
                    cookies.add(pairSaveCookie);
                    cookieInstalled = true;
                } else if (existing != pairSaveCookie) {
                    LOGGER.severe("A foreign SaveCookie owns paired object "
                            + dartFile.getPath());
                    return;
                } else {
                    cookieInstalled = true;
                }
                dataObject.setModified(true);
            } else {
                if (cookieInstalled || existing == pairSaveCookie) {
                    cookies.remove(pairSaveCookie);
                    cookieInstalled = false;
                }
                if (dataObject.getCookie(SaveCookie.class) == null) {
                    dataObject.setModified(false);
                }
            }
        } catch (RuntimeException callbackFailure) {
            LOGGER.log(Level.WARNING,
                    "Cannot reconcile Flutter Designer SaveCookie state",
                    callbackFailure);
        }
    }

    private StateChange transitionLocked(
            PairSaveCoordinatorStatus status, String reason) {
        PairSaveCoordinatorSnapshot previous = state;
        state = snapshot(status, ++epoch, reason);
        return new StateChange(previous, state);
    }

    private static PairSaveCoordinatorSnapshot snapshot(
            PairSaveCoordinatorStatus status, long epoch, String reason) {
        return new PairSaveCoordinatorSnapshot(
                status, epoch, Optional.ofNullable(reason));
    }

    private static String transactionReason(PairFileTransactionResult result) {
        return result.issues().stream().findFirst()
                .map(PairFileTransactionIssue::message)
                .orElse("The exact Dart or .fd disk baseline changed before Save");
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message.strip();
    }

    private static IOException asIOException(Throwable failure) {
        return failure instanceof IOException io
                ? io : new IOException(reason(failure), failure);
    }

    private void reloadPresentation() {
        try {
            controller.reload();
        } catch (RuntimeException presentationFailure) {
            LOGGER.log(Level.WARNING,
                    "The durable Flutter Designer pair committed, but its "
                    + "presentation reload could not be scheduled",
                    presentationFailure);
        }
    }

    private static <T> T onEdt(IoOperation<T> operation) throws IOException {
        if (EventQueue.isDispatchThread()) {
            return operation.run();
        }
        Object[] result = new Object[1];
        IOException[] failure = new IOException[1];
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    result[0] = operation.run();
                } catch (IOException caught) {
                    failure[0] = caught;
                } catch (RuntimeException caught) {
                    failure[0] = new IOException(reason(caught), caught);
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while waiting for the live Dart document", interrupted);
        } catch (InvocationTargetException invocationFailure) {
            throw new IOException(
                    "Cannot access the live Dart document on the Event Dispatch Thread",
                    invocationFailure.getCause());
        }
        if (failure[0] != null) {
            throw failure[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }

    final class PairPreparation implements AutoCloseable {
        private final FlutterDesignerDocumentState.Current expectedCurrent;
        private final PreparedDesignerPair prepared;
        private final LiveDartDocumentSnapshot initialLive;
        private final long eventTicket;
        private final boolean initiallySourceDirty;
        private final DesignerCommandSessionOrchestrator.PendingCommandLease
                commandLease;
        private final Object commandClaim;
        private final DesignerCommandRevision predecessor;
        private final DesignerCommandRevision candidate;
        private PairCandidateAnalysisTicket analysisTicket;
        private LiveDartDocumentSnapshot applied;
        private boolean staged;
        private boolean closed;
        /** Guarded only by the enclosing coordinator monitor. */
        private String operation;

        private PairPreparation(
                FlutterDesignerDocumentState.Current expectedCurrent,
                PreparedDesignerPair prepared,
                LiveDartDocumentSnapshot initialLive,
                long eventTicket,
                boolean initiallySourceDirty,
                DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
                Object commandClaim,
                DesignerCommandRevision predecessor,
                DesignerCommandRevision candidate) {
            this.expectedCurrent = expectedCurrent;
            this.prepared = prepared;
            this.initialLive = initialLive;
            this.eventTicket = eventTicket;
            this.initiallySourceDirty = initiallySourceDirty;
            this.commandLease = commandLease;
            this.commandClaim = commandClaim;
            this.predecessor = predecessor;
            this.candidate = candidate;
        }

        PairCandidateAnalysisTicket prepareAnalysis(
                Path projectRoot,
                DartCandidateWarningPolicy warningPolicy) throws IOException {
            String operationName = "prepare-analysis";
            beginPreparationOperation(this, operationName, false);
            try {
                return PairSaveCoordinator.this.prepareAnalysis(
                        this,
                        projectRoot,
                        warningPolicy);
            } catch (IOException failure) {
                closeAfterOperationFailure(failure);
                throw failure;
            } catch (RuntimeException | Error failure) {
                IOException wrapped = new IOException(
                        "Cannot prepare exact Flutter Designer analysis: "
                        + reason(failure), failure);
                closeAfterOperationFailure(wrapped);
                throw wrapped;
            } finally {
                endPreparationOperation(this, operationName);
            }
        }

        PairSaveEvidenceResult acceptAnalysisAndStage(
                PairCandidateAnalysisTicket ticket,
                DartCandidateAnalysisResult analysis)
                throws IOException {
            String operationName = "accept-analysis-and-stage";
            beginPreparationOperation(this, operationName, false);
            try {
                return PairSaveCoordinator.this.acceptAnalysisAndStage(
                        this, ticket, analysis);
            } catch (IOException failure) {
                if (isActivePreparation(this)) {
                    closeAfterOperationFailure(failure);
                }
                throw failure;
            } catch (RuntimeException | Error failure) {
                IOException wrapped = new IOException(
                        "Cannot bind analyzed Flutter Designer candidate: "
                        + reason(failure), failure);
                if (isActivePreparation(this)) {
                    closeAfterOperationFailure(wrapped);
                }
                throw wrapped;
            } finally {
                endPreparationOperation(this, operationName);
            }
        }

        boolean initiallySourceDirty() {
            return initiallySourceDirty;
        }

        @Override
        public void close() throws IOException {
            String operationName = "close";
            if (!beginPreparationOperation(this, operationName, true)) {
                return;
            }
            try {
                cancelPreparation(this);
            } finally {
                endPreparationOperation(this, operationName);
            }
        }

        private void closeAfterOperationFailure(IOException primary) {
            try {
                cancelPreparation(this);
            } catch (IOException rollbackFailure) {
                primary.addSuppressed(rollbackFailure);
            }
        }
    }

    /**
     * Opaque snapshot of one exact staged logical/physical command source.
     * Capturing it performs no mutation; the replacement reservation is the
     * authority boundary and rejects a token after any cursor or epoch move.
     */
    final class StagedCommandSource {
        private final PairSaveCoordinator coordinatorIdentity;
        private final StagedPairAuthority authorityIdentity;
        private final UnsavedPairHistoryCursor cursorIdentity;
        private final long externalEventEpoch;
        private final long coordinatorEpoch;
        private final long sourceStateEpoch;

        private StagedCommandSource(
                PairSaveCoordinator coordinatorIdentity,
                StagedPairAuthority authorityIdentity,
                UnsavedPairHistoryCursor cursorIdentity,
                long externalEventEpoch,
                long coordinatorEpoch,
                long sourceStateEpoch) {
            this.coordinatorIdentity = Objects.requireNonNull(
                    coordinatorIdentity, "coordinatorIdentity");
            this.authorityIdentity = Objects.requireNonNull(
                    authorityIdentity, "authorityIdentity");
            this.cursorIdentity = Objects.requireNonNull(
                    cursorIdentity, "cursorIdentity");
            this.externalEventEpoch = externalEventEpoch;
            this.coordinatorEpoch = coordinatorEpoch;
            this.sourceStateEpoch = sourceStateEpoch;
            if (cursorIdentity.endpoint().revision()
                        != authorityIdentity.revision()
                    || cursorIdentity.stagedProof()
                        != authorityIdentity.proof()) {
                throw new IllegalArgumentException(
                        "A staged command source must bind one exact authority and physical cursor");
            }
        }

        DesignerCommandSessionOrchestrator.PendingCommandAttempt beginCommand(
                DesignerCommand command) {
            return ownerIdentity().beginCommandFromPhysicalEndpoint(
                    Objects.requireNonNull(command, "command"),
                    physicalPairIdentity());
        }

        DesignerCommandSessionOrchestrator.PendingCommandAttempt beginCommand(
                DesignerCommandRevision expectedRevision,
                DesignerCommand command) {
            return ownerIdentity().beginCommandFromPhysicalEndpoint(
                    Objects.requireNonNull(expectedRevision, "expectedRevision"),
                    Objects.requireNonNull(command, "command"),
                    physicalPairIdentity());
        }

        PreparedDesignerPair physicalPairIdentity() {
            return predecessorProof().preparedPairIdentity();
        }

        private PairSaveCoordinator coordinatorIdentity() {
            return coordinatorIdentity;
        }

        private StagedPairAuthority authorityIdentity() {
            return authorityIdentity;
        }

        private UnsavedPairHistoryCursor cursorIdentity() {
            return cursorIdentity;
        }

        private StagedPairProof predecessorProof() {
            return authorityIdentity.proof();
        }

        private HistoryEndpoint predecessorEndpoint() {
            return cursorIdentity.endpoint();
        }

        private DesignerCommandSessionOrchestrator ownerIdentity() {
            return authorityIdentity.owner();
        }

        private DesignerCommandRevision predecessorRevision() {
            return authorityIdentity.revision();
        }

        private LiveDartDocumentSnapshot liveIdentity() {
            return cursorIdentity.liveIdentity();
        }

        private long externalEventEpoch() {
            return externalEventEpoch;
        }

        private long coordinatorEpoch() {
            return coordinatorEpoch;
        }

        private long sourceStateEpoch() {
            return sourceStateEpoch;
        }
    }

    /**
     * One-shot coordinator-owned staged replacement. The exact logical and
     * physical predecessor remains published until analyzer acceptance and the
     * joint document/pair/command critical section completes.
     */
    final class PairReplacement implements AutoCloseable {
        private final StagedPairProof predecessorProof;
        private final UnsavedPairHistoryCursor predecessorCursor;
        private final HistoryEndpoint predecessorEndpoint;
        private final DesignerCommandSessionOrchestrator.PendingCommandLease
                commandLease;
        private final Object commandClaim;
        private final DesignerCommandRevision predecessor;
        private final DesignerCommandRevision candidate;
        private final PreparedDesignerPair candidatePair;
        private final DartSourceTransitionResult liveTransition;
        private final LiveDartDocumentSnapshot initialLive;
        private final long eventTicket;
        private PairCandidateAnalysisTicket analysisTicket;
        private LiveDartDocumentSnapshot applied;
        private boolean replaced;
        private boolean closed;
        /** Guarded only by the enclosing coordinator monitor. */
        private String operation;

        private PairReplacement(
                StagedPairProof predecessorProof,
                UnsavedPairHistoryCursor predecessorCursor,
                HistoryEndpoint predecessorEndpoint,
                DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
                Object commandClaim,
                DesignerCommandRevision predecessor,
                DesignerCommandRevision candidate,
                PreparedDesignerPair candidatePair,
                DartSourceTransitionResult liveTransition,
                LiveDartDocumentSnapshot initialLive,
                long eventTicket) {
            this.predecessorProof = Objects.requireNonNull(
                    predecessorProof, "predecessorProof");
            this.predecessorCursor = Objects.requireNonNull(
                    predecessorCursor, "predecessorCursor");
            this.predecessorEndpoint = Objects.requireNonNull(
                    predecessorEndpoint, "predecessorEndpoint");
            this.commandLease = Objects.requireNonNull(
                    commandLease, "commandLease");
            this.commandClaim = Objects.requireNonNull(
                    commandClaim, "commandClaim");
            this.predecessor = predecessor;
            this.candidate = candidate;
            this.candidatePair = candidatePair;
            this.liveTransition = liveTransition;
            this.initialLive = initialLive;
            this.eventTicket = eventTicket;
            boolean pairedTarget = candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.PAIRED;
            boolean baselineTarget = candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.BASELINE;
            if (!pairedTarget && !baselineTarget
                    || pairedTarget && (candidatePair == null
                        || liveTransition == null)
                    || baselineTarget && (candidatePair != null
                        || liveTransition != null)) {
                throw new IllegalArgumentException(
                        "A replacement lease must target exactly PAIRED or BASELINE state");
            }
        }

        PairCandidateAnalysisTicket prepareAnalysis(
                Path projectRoot,
                DartCandidateWarningPolicy warningPolicy) throws IOException {
            if (!pairedTarget()) {
                throw new IOException(
                        "The exact durable BASELINE target does not require analyzer admission");
            }
            String operationName = "prepare-replacement-analysis";
            beginReplacementOperation(this, operationName, false);
            try {
                return PairSaveCoordinator.this.prepareReplacementAnalysis(
                        this, projectRoot, warningPolicy);
            } catch (IOException failure) {
                closeAfterOperationFailure(failure);
                throw failure;
            } catch (RuntimeException | Error failure) {
                IOException wrapped = new IOException(
                        "Cannot prepare exact staged replacement analysis: "
                        + reason(failure), failure);
                closeAfterOperationFailure(wrapped);
                throw wrapped;
            } finally {
                endReplacementOperation(this, operationName);
            }
        }

        PairSaveEvidenceResult acceptAnalysisAndReplace(
                PairCandidateAnalysisTicket ticket,
                DartCandidateAnalysisResult analysis) throws IOException {
            if (!pairedTarget()) {
                throw new IOException(
                        "The exact durable BASELINE target cannot accept paired analyzer evidence");
            }
            String operationName = "accept-analysis-and-replace";
            beginReplacementOperation(this, operationName, false);
            try {
                return PairSaveCoordinator.this.acceptAnalysisAndReplace(
                        this, ticket, analysis);
            } catch (IOException failure) {
                if (isActiveReplacement(this)) {
                    closeAfterOperationFailure(failure);
                }
                throw failure;
            } catch (RuntimeException | Error failure) {
                IOException wrapped = new IOException(
                        "Cannot bind analyzed staged replacement: "
                        + reason(failure), failure);
                if (isActiveReplacement(this)) {
                    closeAfterOperationFailure(wrapped);
                }
                throw wrapped;
            } finally {
                endReplacementOperation(this, operationName);
            }
        }

        void replaceWithExactBaseline() throws IOException {
            if (!baselineTarget()) {
                throw new IOException(
                        "Only an exact BASELINE replacement can use the analyzer-free collapse path");
            }
            String operationName = "replace-with-exact-baseline";
            beginReplacementOperation(this, operationName, false);
            try {
                PairSaveCoordinator.this.replaceWithExactBaseline(this);
            } catch (IOException failure) {
                if (isActiveReplacement(this)) {
                    closeAfterOperationFailure(failure);
                }
                throw failure;
            } catch (RuntimeException | Error failure) {
                IOException wrapped = new IOException(
                        "Cannot return the staged Flutter Designer revision to its exact baseline: "
                        + reason(failure), failure);
                if (isActiveReplacement(this)) {
                    closeAfterOperationFailure(wrapped);
                }
                throw wrapped;
            } finally {
                endReplacementOperation(this, operationName);
            }
        }

        boolean pairedTarget() {
            return candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.PAIRED;
        }

        boolean baselineTarget() {
            return candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.BASELINE;
        }

        PairSaveEvidence predecessorEvidence() {
            return predecessorProof.analyzedEvidence();
        }

        DesignerCommandRevision predecessorRevision() {
            return predecessor;
        }

        DesignerCommandRevision candidateRevision() {
            return candidate;
        }

        @Override
        public void close() throws IOException {
            String operationName = "close-replacement";
            if (!beginReplacementOperation(this, operationName, true)) {
                return;
            }
            try {
                cancelReplacement(this);
            } finally {
                endReplacementOperation(this, operationName);
            }
        }

        private void closeAfterOperationFailure(IOException primary) {
            try {
                cancelReplacement(this);
            } catch (IOException rollbackFailure) {
                primary.addSuppressed(rollbackFailure);
            }
        }
    }

    private final class PairSaveCookie implements SaveCookie {
        @Override
        public void save() throws IOException {
            PairSaveCoordinator.this.save();
        }

        @Override
        public String toString() {
            return dartFile.getNameExt() + " + " + designerFile.getNameExt();
        }
    }

    private record DiskBaseline(byte[] dartBytes, byte[] fdBytes) {
        DiskBaseline {
            dartBytes = Objects.requireNonNull(dartBytes, "dartBytes").clone();
            fdBytes = Objects.requireNonNull(fdBytes, "fdBytes").clone();
        }

        @Override
        public byte[] dartBytes() {
            return dartBytes.clone();
        }

        @Override
        public byte[] fdBytes() {
            return fdBytes.clone();
        }
    }

    /**
     * Pure, precomputed successful-Save state.  No native history, controller,
     * command-session or coordinator mutation is allowed while this value is
     * being derived; the identities below are rechecked at the joint commit.
     */
    private record SourceSaveCandidate(
            LiveDartDocumentSnapshot liveIdentity,
            byte[] serializedDartBytes,
            boolean editorDirty,
            boolean coordinatorDirty,
            long sourceStateEpoch,
            boolean retainedMetadata) {
        SourceSaveCandidate {
            Objects.requireNonNull(liveIdentity, "liveIdentity");
            serializedDartBytes = Objects.requireNonNull(
                    serializedDartBytes, "serializedDartBytes").clone();
            if (sourceStateEpoch < 0
                    || editorDirty != coordinatorDirty
                    || !editorDirty && !retainedMetadata
                    || !Arrays.equals(
                            liveIdentity.markerBearingUtf8(),
                            serializedDartBytes)) {
                throw new IllegalArgumentException(
                        "A planned history-aware Source Save must bind one exact dirty serialization");
            }
        }

        @Override
        public byte[] serializedDartBytes() {
            return serializedDartBytes.clone();
        }
    }

    /** Pure identity graph to adopt after one exact ordinary Source commit. */
    private record SourceHistoryReanchorPlan(
            DesignerCommandSessionOrchestrator.SourceAnchorLease leaseIdentity,
            long coordinatorEpoch,
            long eventEpoch,
            long sourceStateEpoch,
            DiskBaseline priorBaselineIdentity,
            SourceSaveCandidate candidate,
            FlutterDesignerDocumentState.Current priorCurrentIdentity,
            FlutterDesignerDocumentState.Current savedCurrentIdentity,
            FlutterDesignerDocumentController.CurrentAdoptionTicket
                    adoptionTicket,
            DiskBaseline savedBaseline,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> priorEdgesIdentity,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges,
            UnsavedPairHistoryCursor priorCursorIdentity,
            UnsavedPairHistoryCursor reanchoredCursor) {
        SourceHistoryReanchorPlan {
            Objects.requireNonNull(leaseIdentity, "leaseIdentity");
            Objects.requireNonNull(priorBaselineIdentity,
                    "priorBaselineIdentity");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(priorCurrentIdentity,
                    "priorCurrentIdentity");
            Objects.requireNonNull(savedCurrentIdentity,
                    "savedCurrentIdentity");
            Objects.requireNonNull(adoptionTicket, "adoptionTicket");
            Objects.requireNonNull(savedBaseline, "savedBaseline");
            priorEdgesIdentity = Map.copyOf(Objects.requireNonNull(
                    priorEdgesIdentity, "priorEdgesIdentity"));
            reanchoredEdges = Map.copyOf(Objects.requireNonNull(
                    reanchoredEdges, "reanchoredEdges"));
            Objects.requireNonNull(priorCursorIdentity,
                    "priorCursorIdentity");
            Objects.requireNonNull(reanchoredCursor,
                    "reanchoredCursor");
            if (coordinatorEpoch < 0 || eventEpoch < 0
                    || sourceStateEpoch < 0
                    || sourceStateEpoch != candidate.sourceStateEpoch()
                    || priorEdgesIdentity.size() != reanchoredEdges.size()
                    || !priorEdgesIdentity.keySet()
                            .equals(reanchoredEdges.keySet())
                    || reanchoredCursor.endpoint().revision()
                        != leaseIdentity.reanchoredRevision(
                                leaseIdentity.savedRevisionId())
                    || reanchoredCursor.endpoint().currentIdentity()
                        != savedCurrentIdentity
                    || reanchoredCursor.liveIdentity()
                        != priorCursorIdentity.liveIdentity()
                    || reanchoredCursor.stagedProof() != null
                    || !Arrays.equals(
                            leaseIdentity.priorDartBytes(),
                            priorBaselineIdentity.dartBytes())
                    || !Arrays.equals(
                            leaseIdentity.priorFdBytes(),
                            priorBaselineIdentity.fdBytes())
                    || !Arrays.equals(
                            leaseIdentity.candidateDartBytes(),
                            candidate.serializedDartBytes())
                    || !Arrays.equals(
                            savedBaseline.dartBytes(),
                            candidate.serializedDartBytes())
                    || !Arrays.equals(
                            savedBaseline.fdBytes(),
                            priorBaselineIdentity.fdBytes())) {
                throw new IllegalArgumentException(
                        "A Source-history plan must bind one exact old and new durable graph");
            }
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge> priorEdgesIdentity() {
            return priorEdgesIdentity;
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges() {
            return reanchoredEdges;
        }
    }

    /**
     * Pure identity graph for saving a dirty Designer command revision whose
     * exact physical target already equals the durable BASELINE pair.
     */
    private record SemanticBaselineHistorySavePlan(
            DesignerCommandSessionOrchestrator.DurableSaveLease leaseIdentity,
            long coordinatorEpoch,
            long eventEpoch,
            long sourceStateEpoch,
            DiskBaseline priorBaselineIdentity,
            SourceSaveCandidate candidate,
            FlutterDesignerDocumentState.Current priorCurrentIdentity,
            FlutterDesignerDocumentState.Current savedCurrentIdentity,
            FlutterDesignerDocumentController.CurrentAdoptionTicket
                    adoptionTicket,
            DiskBaseline savedBaseline,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> priorEdgesIdentity,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges,
            HistoryEndpoint savedEndpoint,
            UnsavedPairHistoryCursor priorCursorIdentity,
            UnsavedPairHistoryCursor savedCursor) {
        SemanticBaselineHistorySavePlan {
            Objects.requireNonNull(leaseIdentity, "leaseIdentity");
            Objects.requireNonNull(priorBaselineIdentity,
                    "priorBaselineIdentity");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(priorCurrentIdentity,
                    "priorCurrentIdentity");
            Objects.requireNonNull(savedCurrentIdentity,
                    "savedCurrentIdentity");
            Objects.requireNonNull(adoptionTicket, "adoptionTicket");
            Objects.requireNonNull(savedBaseline, "savedBaseline");
            priorEdgesIdentity = Map.copyOf(Objects.requireNonNull(
                    priorEdgesIdentity, "priorEdgesIdentity"));
            reanchoredEdges = Map.copyOf(Objects.requireNonNull(
                    reanchoredEdges, "reanchoredEdges"));
            Objects.requireNonNull(savedEndpoint, "savedEndpoint");
            Objects.requireNonNull(priorCursorIdentity,
                    "priorCursorIdentity");
            Objects.requireNonNull(savedCursor, "savedCursor");
            if (coordinatorEpoch < 0 || eventEpoch < 0
                    || sourceStateEpoch < 0
                    || sourceStateEpoch != candidate.sourceStateEpoch()
                    || !candidate.editorDirty() && !(priorCursorIdentity.endpoint() instanceof MetadataHistoryEndpoint)
                    || !candidate.coordinatorDirty() && !(priorCursorIdentity.endpoint() instanceof MetadataHistoryEndpoint)
                    || priorEdgesIdentity.size() != reanchoredEdges.size()
                    || !priorEdgesIdentity.keySet()
                            .equals(reanchoredEdges.keySet())
                    || !exactSemanticBaselineEndpoint(
                            priorCursorIdentity.endpoint(),
                            leaseIdentity.revision())
                    || priorCursorIdentity.stagedProof() != null
                    || savedEndpoint.revision()
                        != leaseIdentity.reanchoredRevision(
                                leaseIdentity.savedRevisionId())
                    || savedEndpoint.currentIdentity()
                        != savedCurrentIdentity
                    || savedEndpoint.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                    || savedCursor.endpoint() != savedEndpoint
                    || savedCursor.liveIdentity()
                        != candidate.liveIdentity()
                    || savedCursor.stagedProof() != null
                    || !Arrays.equals(
                            leaseIdentity.durableDartBytes(),
                            priorBaselineIdentity.dartBytes())
                    || !Arrays.equals(
                            leaseIdentity.durableFdBytes(),
                            priorBaselineIdentity.fdBytes())
                    || !Arrays.equals(
                            candidate.serializedDartBytes(),
                            leaseIdentity.reanchoredRevision(leaseIdentity.savedRevisionId())
                                    .dartCandidateBytes())
                    || !Arrays.equals(
                            savedBaseline.dartBytes(),
                            candidate.serializedDartBytes())
                    || !Arrays.equals(
                            savedBaseline.fdBytes(),
                            leaseIdentity.revision().fdBytes())
                    || !Arrays.equals(
                            priorCursorIdentity.endpoint()
                                    .baselineDartBytes(),
                            priorBaselineIdentity.dartBytes())
                    || !Arrays.equals(
                            priorCursorIdentity.endpoint()
                                    .baselineFdBytes(),
                            priorBaselineIdentity.fdBytes())
                    || !Arrays.equals(
                            savedEndpoint.dartBytes(),
                            savedBaseline.dartBytes())
                    || !Arrays.equals(
                            savedEndpoint.baselineFdBytes(),
                            savedBaseline.fdBytes())) {
                throw new IllegalArgumentException(
                        "A semantic BASELINE Save plan must bind one exact dirty revision and its durable graph");
            }
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge>
                priorEdgesIdentity() {
            return priorEdgesIdentity;
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges() {
            return reanchoredEdges;
        }
    }

    private record SavedHistoryReanchorPlan(
            StagedPairAuthority stagedIdentity,
            DesignerCommandSessionOrchestrator.DurableSaveLease leaseIdentity,
            long coordinatorEpoch,
            long eventEpoch,
            StyledDocument documentIdentity,
            FlutterDesignerDocumentState.Current priorCurrentIdentity,
            FlutterDesignerDocumentState.Current savedCurrentIdentity,
            FlutterDesignerDocumentController.CurrentAdoptionTicket
                    adoptionTicket,
            DiskBaseline savedBaseline,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> priorEdgesIdentity,
            Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges,
            HistoryEndpoint savedEndpoint,
            UnsavedPairHistoryCursor priorCursorIdentity,
            UnsavedPairHistoryCursor savedCursor) {
        SavedHistoryReanchorPlan {
            Objects.requireNonNull(stagedIdentity, "stagedIdentity");
            Objects.requireNonNull(leaseIdentity, "leaseIdentity");
            Objects.requireNonNull(documentIdentity, "documentIdentity");
            Objects.requireNonNull(priorCurrentIdentity,
                    "priorCurrentIdentity");
            Objects.requireNonNull(savedCurrentIdentity,
                    "savedCurrentIdentity");
            Objects.requireNonNull(adoptionTicket, "adoptionTicket");
            Objects.requireNonNull(savedBaseline, "savedBaseline");
            priorEdgesIdentity = Map.copyOf(Objects.requireNonNull(
                    priorEdgesIdentity, "priorEdgesIdentity"));
            reanchoredEdges = Map.copyOf(Objects.requireNonNull(
                    reanchoredEdges, "reanchoredEdges"));
            Objects.requireNonNull(savedEndpoint, "savedEndpoint");
            Objects.requireNonNull(priorCursorIdentity,
                    "priorCursorIdentity");
            Objects.requireNonNull(savedCursor, "savedCursor");
            if (coordinatorEpoch < 0 || eventEpoch < 0
                    || savedEndpoint.revision().revisionId()
                        != leaseIdentity.savedRevisionId()
                    || savedEndpoint.currentIdentity() != savedCurrentIdentity
                    || savedEndpoint.revision().persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                    || savedCursor.endpoint() != savedEndpoint
                    || savedCursor.stagedProof() != null
                    || priorEdgesIdentity.size() != reanchoredEdges.size()
                    || !priorEdgesIdentity.keySet()
                            .equals(reanchoredEdges.keySet())
                    || !Arrays.equals(
                            savedEndpoint.dartBytes(),
                            savedBaseline.dartBytes())
                    || !Arrays.equals(
                            savedEndpoint.baselineFdBytes(),
                            savedBaseline.fdBytes())) {
                throw new IllegalArgumentException(
                        "A saved-history re-anchor plan must bind one exact durable endpoint");
            }
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge> reanchoredEdges() {
            return reanchoredEdges;
        }

        @Override
        public Map<HistoryEdgeKey, UnsavedPairHistoryEdge> priorEdgesIdentity() {
            return priorEdgesIdentity;
        }
    }

    private record CommittedLiveState(
            LiveDartDocumentSnapshot snapshot,
            boolean editorDirty,
            boolean coordinatorDirty,
            long sourceStateEpoch) {
        CommittedLiveState {
            Objects.requireNonNull(snapshot, "snapshot");
            if (sourceStateEpoch < 0) {
                throw new IllegalArgumentException(
                        "sourceStateEpoch must be non-negative");
            }
        }
    }

    /** Exact pair-side authority retained by a pending editor close. */
    record CloseRevision(
            Object coordinatorIdentity,
            long stateEpoch,
            long externalEventEpoch,
            long sourceStateEpoch) {
        CloseRevision {
            Objects.requireNonNull(coordinatorIdentity,
                    "coordinatorIdentity");
            if (stateEpoch < 0 || externalEventEpoch < 0
                    || sourceStateEpoch < 0) {
                throw new IllegalArgumentException(
                        "Pair close-revision epochs must be non-negative");
            }
        }

        boolean sameRevision(CloseRevision other) {
            Objects.requireNonNull(other, "other");
            return coordinatorIdentity == other.coordinatorIdentity
                    && stateEpoch == other.stateEpoch
                    && externalEventEpoch == other.externalEventEpoch
                    && sourceStateEpoch == other.sourceStateEpoch;
        }
    }

    /** One untorn pair snapshot for identity-sensitive controller binding. */
    record BindingRevision(
            PairSaveCoordinatorSnapshot state,
            CloseRevision closeRevision) {
        BindingRevision {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(closeRevision, "closeRevision");
            if (state.epoch() != closeRevision.stateEpoch()) {
                throw new IllegalArgumentException(
                        "Pair binding state and close revision must share one epoch");
            }
        }
    }

    enum StagedPairProofKind {
        ANALYZED,
        REANCHORED_ANALYZED,
        REANCHORED_METADATA,
        FORMER_DURABLE
    }

    record StagedPairProofSnapshot(
            StagedPairProofKind kind,
            FlutterDesignerDocumentState.Current loadedCurrentIdentity,
            PreparedDesignerPair preparedPairIdentity,
            LiveDartDocumentSnapshot liveCandidateIdentity,
            byte[] baselineDartBytes,
            byte[] baselineFdBytes,
            byte[] candidateDartBytes) {
        StagedPairProofSnapshot {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(loadedCurrentIdentity,
                    "loadedCurrentIdentity");
            Objects.requireNonNull(preparedPairIdentity,
                    "preparedPairIdentity");
            Objects.requireNonNull(liveCandidateIdentity,
                    "liveCandidateIdentity");
            baselineDartBytes = Objects.requireNonNull(
                    baselineDartBytes, "baselineDartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(
                    baselineFdBytes, "baselineFdBytes").clone();
            candidateDartBytes = Objects.requireNonNull(
                    candidateDartBytes, "candidateDartBytes").clone();
        }

        @Override
        public byte[] baselineDartBytes() {
            return baselineDartBytes.clone();
        }

        @Override
        public byte[] baselineFdBytes() {
            return baselineFdBytes.clone();
        }

        @Override
        public byte[] candidateDartBytes() {
            return candidateDartBytes.clone();
        }
    }

    private sealed interface StagedPairProof permits
            AnalyzedStagedPairProof, SavedHistoryProof {
        StagedPairProofKind kind();

        PairSaveEvidence analyzedEvidence();

        FlutterDesignerDocumentState.Current loadedCurrentIdentity();

        PreparedDesignerPair preparedPairIdentity();

        LiveDartDocumentSnapshot liveCandidateIdentity();

        byte[] baselineDartBytes();

        byte[] baselineFdBytes();

        byte[] candidateDartBytes();

        default StagedPairProofSnapshot snapshot() {
            return new StagedPairProofSnapshot(
                    kind(),
                    loadedCurrentIdentity(),
                    preparedPairIdentity(),
                    liveCandidateIdentity(),
                    baselineDartBytes(),
                    baselineFdBytes(),
                    candidateDartBytes());
        }
    }

    private record AnalyzedStagedPairProof(PairSaveEvidence evidence)
            implements StagedPairProof {
        AnalyzedStagedPairProof {
            Objects.requireNonNull(evidence, "evidence");
        }

        @Override
        public StagedPairProofKind kind() {
            return StagedPairProofKind.ANALYZED;
        }

        @Override
        public PairSaveEvidence analyzedEvidence() {
            return evidence;
        }

        @Override
        public FlutterDesignerDocumentState.Current loadedCurrentIdentity() {
            return evidence.loadedCurrentIdentity();
        }

        @Override
        public PreparedDesignerPair preparedPairIdentity() {
            return evidence.preparedPairIdentity();
        }

        @Override
        public LiveDartDocumentSnapshot liveCandidateIdentity() {
            return evidence.liveCandidateIdentity();
        }

        @Override
        public byte[] baselineDartBytes() {
            return evidence.baselineDartBytes();
        }

        @Override
        public byte[] baselineFdBytes() {
            return evidence.baselineFdBytes();
        }

        @Override
        public byte[] candidateDartBytes() {
            return evidence.candidateDartBytes();
        }
    }

    /** Analyzer-free authority derived only from an immutable retained edge. */
    private record SavedHistoryProof(
            StagedPairProofKind kind,
            FlutterDesignerDocumentState.Current loadedCurrentIdentity,
            PreparedDesignerPair preparedPairIdentity,
            LiveDartDocumentSnapshot liveCandidateIdentity,
            byte[] baselineDartBytes,
            byte[] baselineFdBytes,
            byte[] candidateDartBytes) implements StagedPairProof {
        SavedHistoryProof {
            Objects.requireNonNull(kind, "kind");
            if (kind == StagedPairProofKind.ANALYZED) {
                throw new IllegalArgumentException(
                        "Saved history proof cannot claim fresh analyzer admission");
            }
            Objects.requireNonNull(loadedCurrentIdentity,
                    "loadedCurrentIdentity");
            Objects.requireNonNull(preparedPairIdentity,
                    "preparedPairIdentity");
            Objects.requireNonNull(liveCandidateIdentity,
                    "liveCandidateIdentity");
            baselineDartBytes = Objects.requireNonNull(
                    baselineDartBytes, "baselineDartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(
                    baselineFdBytes, "baselineFdBytes").clone();
            candidateDartBytes = Objects.requireNonNull(
                    candidateDartBytes, "candidateDartBytes").clone();
            if (!Arrays.equals(
                        preparedPairIdentity.baselineDartBytes(),
                        baselineDartBytes)
                    || !Arrays.equals(
                        preparedPairIdentity.baselineFdBytes(),
                        baselineFdBytes)
                    || !Arrays.equals(
                        preparedPairIdentity.prospectiveDartBytes(),
                        candidateDartBytes)
                    || !Arrays.equals(
                        liveCandidateIdentity.markerBearingUtf8(),
                        candidateDartBytes)) {
                throw new IllegalArgumentException(
                        "Saved history proof must bind one exact re-anchored pair candidate");
            }
        }

        @Override
        public PairSaveEvidence analyzedEvidence() {
            return null;
        }

        @Override
        public byte[] baselineDartBytes() {
            return baselineDartBytes.clone();
        }

        @Override
        public byte[] baselineFdBytes() {
            return baselineFdBytes.clone();
        }

        @Override
        public byte[] candidateDartBytes() {
            return candidateDartBytes.clone();
        }
    }

    private static final class StagedPairAuthority {
        private final StagedPairProof proof;
        private final DesignerCommandSessionOrchestrator owner;
        private final DesignerCommandRevision revision;

        StagedPairAuthority(
                PairSaveEvidence evidence,
                DesignerCommandSessionOrchestrator owner,
                DesignerCommandRevision revision) {
            this(new AnalyzedStagedPairProof(evidence), owner, revision);
        }

        StagedPairAuthority(
                StagedPairProof proof,
                DesignerCommandSessionOrchestrator owner,
                DesignerCommandRevision revision) {
            this(proof, owner, revision, null);
        }

        StagedPairAuthority(StagedPairProof proof, DesignerCommandSessionOrchestrator owner,
                DesignerCommandRevision revision, DesignerCommandSessionOrchestrator.SourceRestageLease restageLease) {
            this.proof = Objects.requireNonNull(proof, "proof");
            this.owner = Objects.requireNonNull(owner, "owner");
            this.revision = Objects.requireNonNull(revision, "revision");
            boolean canonicalVariant = revision.preparedPair().orElse(null)
                            == proof.preparedPairIdentity()
                    && Arrays.equals(
                            revision.dartCandidateBytes(),
                            proof.candidateDartBytes());
            boolean retainedPhysicalVariant = proof instanceof SavedHistoryProof
                    && proof.preparedPairIdentity().prospectiveDocument()
                            .equals(revision.document());
            boolean observedPhysicalVariant = restageLease != null && restageLease.owner() == owner
                    && restageLease.revision() == revision && restageLease.ownsExactActiveRevision()
                    && restageLease.preparedPair() == proof.preparedPairIdentity()
                    && proof.analyzedEvidence() != null;
            if (revision.persistenceKind()
                        != DesignerRevisionPersistenceKind.PAIRED
                    || !(canonicalVariant || retainedPhysicalVariant || observedPhysicalVariant)
                    || !Arrays.equals(
                            revision.fdBytes(),
                            proof.preparedPairIdentity()
                                    .prospectiveFdBytes())) {
                throw new IllegalArgumentException(
                        "Staged pair authority must bind one exact PAIRED command revision");
            }
        }

        PairSaveEvidence evidence() {
            return proof.analyzedEvidence();
        }

        StagedPairProof proof() {
            return proof;
        }

        DesignerCommandSessionOrchestrator owner() {
            return owner;
        }

        DesignerCommandRevision revision() {
            return revision;
        }

        StagedPairProofSnapshot snapshot() {
            return proof.snapshot();
        }
    }

    @FunctionalInterface
    interface HistoryPreparationHook {
        void afterClaim(
                DesignerCommandSessionOrchestrator.PendingCommandLease lease,
                Object exactClaim) throws IOException;
    }

    @FunctionalInterface
    interface ForwardAdmissionHook {
        void afterArmed(
                DesignerCommandSessionOrchestrator.PendingCommandLease lease,
                boolean replacementAdmission);
    }

    @FunctionalInterface
    interface SourceHistoryOwnerSelectionHook {
        void afterSelection(DesignerCommandSessionOrchestrator owner);
    }

    /**
     * One monotonic pre-seal authority token. External events may only poison
     * it; they cannot replace its lease identity or unpoison it.
     */
    private static final class ArmedForwardAdmission {
        private final Object leaseIdentity;
        private final boolean replacementAdmission;
        private final long eventTicket;
        private final long coordinatorEpoch;
        private boolean poisoned;
        private String externalConflictReason;

        ArmedForwardAdmission(
                Object leaseIdentity,
                boolean replacementAdmission,
                long eventTicket,
                long coordinatorEpoch) {
            this.leaseIdentity = Objects.requireNonNull(
                    leaseIdentity, "leaseIdentity");
            this.replacementAdmission = replacementAdmission;
            this.eventTicket = eventTicket;
            this.coordinatorEpoch = coordinatorEpoch;
        }

        void poison(String reason) {
            if (poisoned) {
                return;
            }
            poisoned = true;
            externalConflictReason = Objects.requireNonNull(
                    reason, "reason");
        }
    }

    private interface HistoryEndpoint {
        DesignerCommandRevision revision();

        default long userSourceProofBytes() {
            return revision().userSourceProjection().retainedBytes();
        }

        FlutterDesignerDocumentState.Current currentIdentity();

        byte[] dartBytes();

        byte[] baselineDartBytes();

        byte[] baselineFdBytes();
    }

    private record BaselineHistoryEndpoint(
            DesignerCommandRevision revision,
            FlutterDesignerDocumentState.Current currentIdentity,
            byte[] dartBytes,
            byte[] baselineFdBytes) implements HistoryEndpoint {
        BaselineHistoryEndpoint {
            Objects.requireNonNull(revision, "revision");
            Objects.requireNonNull(currentIdentity, "currentIdentity");
            dartBytes = Objects.requireNonNull(dartBytes, "dartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(
                    baselineFdBytes, "baselineFdBytes").clone();
            if (revision.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE
                    || !Arrays.equals(revision.dartCandidateBytes(), dartBytes)
                    || !Arrays.equals(revision.fdBytes(), baselineFdBytes)) {
                throw new IllegalArgumentException(
                        "A BASELINE history endpoint must retain exact command bytes");
            }
        }

        @Override
        public byte[] dartBytes() {
            return dartBytes.clone();
        }

        @Override
        public byte[] baselineDartBytes() {
            return dartBytes();
        }

        @Override
        public byte[] baselineFdBytes() {
            return baselineFdBytes.clone();
        }
    }

    /**
     * One saved semantic position which physically sits below an ordinary
     * native Source edit. The semantic revision may describe either the newer
     * durable Source anchor or an older byte-identical-widget baseline; in
     * both cases {@link #dartBytes()} retains the exact older native-timeline
     * bytes reached after undoing that Source edit.
     */
    private record SourceOverlayBaselineHistoryEndpoint(
            DesignerCommandRevision revision,
            FlutterDesignerDocumentState.Current currentIdentity,
            byte[] dartBytes,
            byte[] baselineDartBytes,
            byte[] baselineFdBytes) implements HistoryEndpoint {
        SourceOverlayBaselineHistoryEndpoint {
            Objects.requireNonNull(revision, "revision");
            Objects.requireNonNull(currentIdentity, "currentIdentity");
            dartBytes = Objects.requireNonNull(
                    dartBytes, "dartBytes").clone();
            baselineDartBytes = Objects.requireNonNull(
                    baselineDartBytes, "baselineDartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(
                    baselineFdBytes, "baselineFdBytes").clone();
            if (revision.persistenceKind()
                        != DesignerRevisionPersistenceKind.BASELINE
                    || !(Arrays.equals(
                            revision.dartCandidateBytes(), baselineDartBytes)
                        || Arrays.equals(
                            revision.dartCandidateBytes(), dartBytes))
                    || !Arrays.equals(
                            revision.fdBytes(), baselineFdBytes)
                    || Arrays.equals(dartBytes, baselineDartBytes)) {
                throw new IllegalArgumentException(
                        "A Source-overlay endpoint must separate exact native and durable bytes");
            }
        }

        @Override
        public byte[] dartBytes() {
            return dartBytes.clone();
        }

        @Override
        public byte[] baselineDartBytes() {
            return baselineDartBytes.clone();
        }

        @Override
        public byte[] baselineFdBytes() {
            return baselineFdBytes.clone();
        }
    }

    private record PairedHistoryEndpoint(
            DesignerCommandRevision revision,
            PairSaveEvidence seed) implements HistoryEndpoint {
        PairedHistoryEndpoint {
            Objects.requireNonNull(revision, "revision");
            Objects.requireNonNull(seed, "seed");
            if (revision.persistenceKind()
                        != DesignerRevisionPersistenceKind.PAIRED
                    || revision.preparedPair().orElse(null)
                        != seed.preparedPairIdentity()
                    || !Arrays.equals(
                            revision.dartCandidateBytes(),
                            seed.candidateDartBytes())) {
                throw new IllegalArgumentException(
                        "A PAIRED history endpoint must retain exact analyzed command evidence");
            }
        }

        @Override
        public FlutterDesignerDocumentState.Current currentIdentity() {
            return seed.loadedCurrentIdentity();
        }

        @Override
        public byte[] dartBytes() {
            return seed.candidateDartBytes();
        }

        @Override
        public byte[] baselineDartBytes() {
            return seed.baselineDartBytes();
        }

        @Override
        public byte[] baselineFdBytes() {
            return seed.baselineFdBytes();
        }
    }

    private sealed interface SavedHistorySeed permits
            FormerDurableHistorySeed, ReanchoredAnalyzedHistorySeed,
            ValidatedMetadataHistorySeed {
        StagedPairProofKind kind();

        byte[] candidateDartBytes();

        byte[] candidateFdBytes();
    }

    /** Validated FD-only bytes whose generated Source exactly matched its durable anchor. */
    private record ValidatedMetadataHistorySeed(DesignerCommandRevision revision)
            implements SavedHistorySeed {
        ValidatedMetadataHistorySeed {
            Objects.requireNonNull(revision, "revision");
            if (revision.persistenceKind() != DesignerRevisionPersistenceKind.FD_ONLY) {
                throw new IllegalArgumentException("Metadata provenance requires an exact FD_ONLY revision");
            }
        }
        @Override
        public StagedPairProofKind kind() {
            return StagedPairProofKind.REANCHORED_METADATA;
        }

        @Override
        public byte[] candidateDartBytes() {
            return revision.dartCandidateBytes();
        }

        @Override
        public byte[] candidateFdBytes() {
            return revision.fdBytes();
        }
    }

    /**
     * Exact post-reanchor representation of the former durable semantic
     * position. The Dart bytes preserve its historical physical envelope;
     * the .fd bytes use the proven representation under the new durable
     * anchor, so an admitted schema migration is not resurrected by Undo.
     */
    private record FormerDurableHistorySeed(
            byte[] candidateDartBytes,
            byte[] candidateFdBytes) implements SavedHistorySeed {
        FormerDurableHistorySeed {
            candidateDartBytes = Objects.requireNonNull(
                    candidateDartBytes, "candidateDartBytes").clone();
            candidateFdBytes = Objects.requireNonNull(
                    candidateFdBytes, "candidateFdBytes").clone();
        }

        @Override
        public StagedPairProofKind kind() {
            return StagedPairProofKind.FORMER_DURABLE;
        }

        @Override
        public byte[] candidateDartBytes() {
            return candidateDartBytes.clone();
        }

        @Override
        public byte[] candidateFdBytes() {
            return candidateFdBytes.clone();
        }
    }

    /** A prior analyzer result whose exact candidate bytes survived re-anchor. */
    private record ReanchoredAnalyzedHistorySeed(PairSaveEvidence evidence)
            implements SavedHistorySeed {
        ReanchoredAnalyzedHistorySeed {
            Objects.requireNonNull(evidence, "evidence");
        }

        @Override
        public StagedPairProofKind kind() {
            return StagedPairProofKind.REANCHORED_ANALYZED;
        }

        @Override
        public byte[] candidateDartBytes() {
            return evidence.candidateDartBytes();
        }

        @Override
        public byte[] candidateFdBytes() {
            return evidence.preparedPairIdentity().prospectiveFdBytes();
        }
    }

    private record ReanchoredPairedHistoryEndpoint(
            DesignerCommandRevision revision,
            FlutterDesignerDocumentState.Current currentIdentity,
            byte[] baselineDartBytes,
            byte[] baselineFdBytes,
            PreparedDesignerPair endpointPair,
            SavedHistorySeed seed) implements HistoryEndpoint {
        @Override
        public long userSourceProofBytes() {
            return endpointPair.dartTransition().userSourceProjection().retainedBytes();
        }

        ReanchoredPairedHistoryEndpoint {
            Objects.requireNonNull(revision, "revision");
            Objects.requireNonNull(currentIdentity, "currentIdentity");
            baselineDartBytes = Objects.requireNonNull(
                    baselineDartBytes, "baselineDartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(
                    baselineFdBytes, "baselineFdBytes").clone();
            Objects.requireNonNull(endpointPair, "endpointPair");
            Objects.requireNonNull(seed, "seed");
            if (revision.persistenceKind()
                        != DesignerRevisionPersistenceKind.PAIRED
                    || revision.preparedPair().isEmpty()
                    || !Arrays.equals(
                        endpointPair.baselineDartBytes(),
                        baselineDartBytes)
                    || !Arrays.equals(
                        endpointPair.baselineFdBytes(),
                        baselineFdBytes)
                    || !endpointPair.prospectiveDocument()
                            .equals(revision.document())
                    || !Arrays.equals(
                        endpointPair.prospectiveFdBytes(),
                        revision.fdBytes())
                    || !Arrays.equals(
                        endpointPair.prospectiveDartBytes(),
                        seed.candidateDartBytes())
                    || !Arrays.equals(
                        endpointPair.prospectiveFdBytes(),
                        seed.candidateFdBytes())) {
                throw new IllegalArgumentException(
                        "A saved history endpoint must retain one exact physical pair variant and provenance");
            }
        }

        @Override
        public byte[] dartBytes() {
            return seed.candidateDartBytes();
        }

        @Override
        public byte[] baselineDartBytes() {
            return baselineDartBytes.clone();
        }

        @Override
        public byte[] baselineFdBytes() {
            return baselineFdBytes.clone();
        }
    }

    /** A retained, proven metadata revision; no PreparedDesignerPair is fabricated. */
    private record MetadataHistoryEndpoint(
            DesignerCommandRevision revision,
            FlutterDesignerDocumentState.Current currentIdentity,
            byte[] baselineDartBytes,
            byte[] baselineFdBytes,
            DesignerCommandRevision physicalRevision,
            SavedHistorySeed seed) implements HistoryEndpoint {
        @Override
        public long userSourceProofBytes() {
            return physicalRevision.userSourceProjection().retainedBytes();
        }

        MetadataHistoryEndpoint {
            Objects.requireNonNull(revision, "revision");
            Objects.requireNonNull(currentIdentity, "currentIdentity");
            Objects.requireNonNull(physicalRevision, "physicalRevision");
            Objects.requireNonNull(seed, "seed");
            baselineDartBytes = Objects.requireNonNull(baselineDartBytes, "baselineDartBytes").clone();
            baselineFdBytes = Objects.requireNonNull(baselineFdBytes, "baselineFdBytes").clone();
            if (revision.persistenceKind() != DesignerRevisionPersistenceKind.FD_ONLY
                    || physicalRevision.persistenceKind() != DesignerRevisionPersistenceKind.FD_ONLY
                    || !physicalRevision.document().equals(revision.document())
                    || !Arrays.equals(physicalRevision.fdBytes(), revision.fdBytes())
                    || !Arrays.equals(physicalRevision.dartCandidateBytes(), seed.candidateDartBytes())
                    || !Arrays.equals(physicalRevision.fdBytes(), seed.candidateFdBytes())
                    || Arrays.equals(revision.fdBytes(), baselineFdBytes)) {
                throw new IllegalArgumentException("Metadata history requires exact retained FD_ONLY provenance");
            }
        }
        @Override public byte[] dartBytes() { return physicalRevision.dartCandidateBytes(); }
        @Override public byte[] baselineDartBytes() { return baselineDartBytes.clone(); }
        @Override public byte[] baselineFdBytes() { return baselineFdBytes.clone(); }
    }

    private record HistoryEdgeKey(
            long beforeRevisionId,
            long afterRevisionId) {
        HistoryEdgeKey {
            if (beforeRevisionId < 0 || afterRevisionId < 0
                    || beforeRevisionId == afterRevisionId) {
                throw new IllegalArgumentException(
                        "History edge revision ids must be distinct and non-negative");
            }
        }
    }

    private record UnsavedPairHistoryEdge(
            HistoryEndpoint before,
            HistoryEndpoint after,
            DesignerCommandSessionOrchestrator owner) {
        UnsavedPairHistoryEdge {
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
            Objects.requireNonNull(owner, "owner");
            if (before.revision().persistenceKind()
                        == DesignerRevisionPersistenceKind.BASELINE
                        && after.revision().persistenceKind()
                            == DesignerRevisionPersistenceKind.BASELINE
                    || before.currentIdentity() != after.currentIdentity()
                    || !Arrays.equals(
                            before.baselineDartBytes(),
                            after.baselineDartBytes())
                    || !Arrays.equals(
                            before.baselineFdBytes(),
                            after.baselineFdBytes())) {
                throw new IllegalArgumentException(
                        "A pair history edge must retain one Current and durable anchor");
            }
        }

        HistoryEdgeKey key() {
            return new HistoryEdgeKey(
                    before.revision().revisionId(),
                    after.revision().revisionId());
        }

        byte[] baselineDartBytes() {
            return before.baselineDartBytes();
        }

        byte[] baselineFdBytes() {
            return before.baselineFdBytes();
        }
    }

    private record UnsavedPairHistoryCursor(
            HistoryEndpoint endpoint,
            LiveDartDocumentSnapshot liveIdentity,
            StagedPairProof stagedProof) {
        UnsavedPairHistoryCursor(
                HistoryEndpoint endpoint,
                LiveDartDocumentSnapshot liveIdentity,
                PairSaveEvidence pairedEvidence) {
            this(endpoint, liveIdentity, pairedEvidence == null
                    ? null : new AnalyzedStagedPairProof(pairedEvidence));
        }

        UnsavedPairHistoryCursor {
            Objects.requireNonNull(endpoint, "endpoint");
            Objects.requireNonNull(liveIdentity, "liveIdentity");
            if (!Arrays.equals(
                    endpoint.dartBytes(), liveIdentity.markerBearingUtf8())) {
                throw new IllegalArgumentException(
                        "The unsaved history cursor live bytes differ from its endpoint");
            }
            if (endpoint.revision().persistenceKind()
                    == DesignerRevisionPersistenceKind.PAIRED) {
                Objects.requireNonNull(stagedProof, "stagedProof");
                if (!stagedProof.liveCandidateIdentity()
                        .sameEvidence(liveIdentity)) {
                    throw new IllegalArgumentException(
                            "The PAIRED history cursor requires exact fresh evidence");
                }
            } else if (stagedProof != null) {
                throw new IllegalArgumentException(
                        "A BASELINE history cursor cannot retain paired evidence");
            }
        }

        PairSaveEvidence pairedEvidence() {
            return stagedProof == null ? null : stagedProof.analyzedEvidence();
        }
    }

    private enum HistoryTransitionResolution {
        ACTIVE,
        COMMITTED,
        ABORTED,
        INVALIDATED
    }

    private final class ActiveHistoryTransition {
        private final UnsavedPairHistoryEdge edge;
        private final HistoryEndpoint sourceEndpoint;
        private final HistoryEndpoint targetEndpoint;
        private final DesignerCommandSessionOrchestrator.PendingCommandLease
                commandLease;
        private final Object commandClaim;
        private final long eventTicket;
        private final long coordinatorEpoch;
        private final UnsavedPairHistoryCursor sourceCursor;
        private final EffectsDeferral effectsDeferral;
        private DiskBaseline observedDisk;
        private LiveDartDocumentSnapshot sourceLive;
        private FlutterDesignerEditorSupport.NativeHistoryReplay nativeReplay;
        private HistoryTransitionResolution resolution =
                HistoryTransitionResolution.ACTIVE;
        private boolean publicationFinished;

        ActiveHistoryTransition(
                UnsavedPairHistoryEdge edge,
                HistoryEndpoint sourceEndpoint,
                HistoryEndpoint targetEndpoint,
                DesignerCommandSessionOrchestrator.PendingCommandLease commandLease,
                Object commandClaim,
                long eventTicket,
                long coordinatorEpoch,
                UnsavedPairHistoryCursor sourceCursor,
                EffectsDeferral effectsDeferral) {
            this.edge = Objects.requireNonNull(edge, "edge");
            this.sourceEndpoint = Objects.requireNonNull(
                    sourceEndpoint, "sourceEndpoint");
            this.targetEndpoint = Objects.requireNonNull(
                    targetEndpoint, "targetEndpoint");
            this.commandLease = Objects.requireNonNull(
                    commandLease, "commandLease");
            this.commandClaim = Objects.requireNonNull(
                    commandClaim, "commandClaim");
            this.eventTicket = eventTicket;
            this.coordinatorEpoch = coordinatorEpoch;
            this.sourceCursor = Objects.requireNonNull(
                    sourceCursor, "sourceCursor");
            this.effectsDeferral = Objects.requireNonNull(
                    effectsDeferral, "effectsDeferral");
        }

        void publishAfterNativeBarrier() {
            synchronized (this) {
                if (publicationFinished) {
                    return;
                }
                publicationFinished = true;
            }
            effectsDeferral.close();
        }
    }

    private final class PreparedHistoryReplay
            implements DesignerSemanticUndoableEdit.PreparedReplay {
        private final ActiveHistoryTransition transition;

        PreparedHistoryReplay(ActiveHistoryTransition transition) {
            this.transition = Objects.requireNonNull(
                    transition, "transition");
        }

        @Override
        public DesignerSemanticUndoableEdit.DeferredPublication commit()
                throws IOException {
            return commitHistoryTransition(transition);
        }

        @Override
        public void abort() {
            abortHistoryTransition(transition);
        }

        @Override
        public void invalidate(Throwable failure) {
            invalidateHistoryTransition(
                    transition,
                    Objects.requireNonNull(failure, "failure"));
        }
    }

    private record StateChange(
            PairSaveCoordinatorSnapshot previous,
            PairSaveCoordinatorSnapshot current) {
    }

    private record EffectsPublication(
            StateChange change,
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects) {
    }

    private final class EffectsDeferral implements AutoCloseable {
        private boolean closed;

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            endEffectsDeferral();
        }
    }

    private record StagePublication(
            PairSaveEvidenceResult result) {
        StagePublication {
            Objects.requireNonNull(result, "result");
        }
    }

    private record InitialPreparationInputs(
            DesignerCommandRevision predecessor,
            DesignerCommandRevision candidate,
            PreparedDesignerPair prepared) {
        InitialPreparationInputs {
            Objects.requireNonNull(predecessor, "predecessor");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(prepared, "prepared");
        }
    }

    private record PreparationRelease(
            StateChange change,
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects) {
        PreparationRelease {
            Objects.requireNonNull(commandEffects, "commandEffects");
        }
    }

    private record ReplacementInputs(
            DesignerCommandRevision predecessor,
            DesignerCommandRevision candidate,
            PreparedDesignerPair candidatePair,
            DartSourceTransitionResult liveTransition) {
        ReplacementInputs {
            Objects.requireNonNull(predecessor, "predecessor");
            Objects.requireNonNull(candidate, "candidate");
            boolean paired = candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.PAIRED;
            boolean baseline = candidate.persistenceKind()
                    == DesignerRevisionPersistenceKind.BASELINE;
            if (!paired && !baseline
                    || paired && (candidatePair == null
                        || liveTransition == null)
                    || baseline && (candidatePair != null
                        || liveTransition != null)) {
                throw new IllegalArgumentException(
                        "A staged replacement target must be exactly PAIRED or BASELINE");
            }
        }
    }

    private record ReplacementPublication(
            PairSaveEvidenceResult result) {
        ReplacementPublication {
            Objects.requireNonNull(result, "result");
        }
    }

    private record ReplacementRelease(
            StateChange change,
            DesignerCommandSessionOrchestrator.DeferredLeaseEffects
                    commandEffects) {
        ReplacementRelease {
            Objects.requireNonNull(commandEffects, "commandEffects");
        }
    }

    private record FdOnlyEvidence(
            byte[] durableDart,
            byte[] durableFd,
            byte[] candidateFd,
            LiveDartDocumentSnapshot liveIdentity,
            FlutterDesignerDocumentState.Current savedCurrentIdentity,
            FlutterDesignerDocumentController.CurrentAdoptionTicket
                    adoptionTicket) {
        FdOnlyEvidence {
            durableDart = Objects.requireNonNull(
                    durableDart, "durableDart").clone();
            durableFd = Objects.requireNonNull(durableFd, "durableFd").clone();
            candidateFd = Objects.requireNonNull(
                    candidateFd, "candidateFd").clone();
            Objects.requireNonNull(liveIdentity, "liveIdentity");
            Objects.requireNonNull(savedCurrentIdentity,
                    "savedCurrentIdentity");
            Objects.requireNonNull(adoptionTicket, "adoptionTicket");
        }

        @Override
        public byte[] durableDart() {
            return durableDart.clone();
        }

        @Override
        public byte[] durableFd() {
            return durableFd.clone();
        }

        @Override
        public byte[] candidateFd() {
            return candidateFd.clone();
        }
    }

    private static final class ActivePairSave {
        private final StagedPairAuthority authority;
        private final StagedPairProof proof;
        private final DesignerCommandSessionOrchestrator.DurableSaveLease lease;
        private final SavedHistoryReanchorPlan reanchorPlan;
        private final Thread owner;
        private final long eventTicket;
        private boolean outputOpened;
        private PairFileTransactionResult result;

        ActivePairSave(
                StagedPairAuthority authority,
                DesignerCommandSessionOrchestrator.DurableSaveLease lease,
                SavedHistoryReanchorPlan reanchorPlan,
                Thread owner,
                long eventTicket) {
            this.authority = authority;
            this.proof = authority == null ? null : authority.proof();
            this.lease = lease;
            this.reanchorPlan = reanchorPlan;
            this.owner = owner;
            this.eventTicket = eventTicket;
        }
    }

    private static final class ActiveSourceSave {
        private final DiskBaseline baseline;
        private final Thread owner;
        private final long eventTicket;
        private final SourceHistoryReanchorPlan historyPlan;
        private final SemanticBaselineHistorySavePlan semanticBaselinePlan;
        private boolean cesEntered;
        private boolean outputOpened;
        private PairFileTransactionResult result;
        private byte[] committedDart;

        ActiveSourceSave(
                DiskBaseline baseline,
                Thread owner,
                long eventTicket,
                SourceHistoryReanchorPlan historyPlan,
                SemanticBaselineHistorySavePlan semanticBaselinePlan) {
            this.baseline = baseline;
            this.owner = owner;
            this.eventTicket = eventTicket;
            this.historyPlan = historyPlan;
            this.semanticBaselinePlan = semanticBaselinePlan;
            if (historyPlan != null && semanticBaselinePlan != null) {
                throw new IllegalArgumentException(
                        "A Source Save cannot own two history plans");
            }
        }
    }

    private static final class ActiveFdOnlySave {
        private final FlutterDesignerDocumentState.Current expectedCurrent;
        private final DesignerCommandSessionOrchestrator.DurableSaveLease lease;
        private final FdOnlyEvidence evidence;
        private final long eventTicket;
        private boolean liveMutationObserved;
        private PairFileTransactionResult result;

        ActiveFdOnlySave(
                FlutterDesignerDocumentState.Current expectedCurrent,
                DesignerCommandSessionOrchestrator.DurableSaveLease lease,
                FdOnlyEvidence evidence,
                long eventTicket) {
            this.expectedCurrent = expectedCurrent;
            this.lease = lease;
            this.evidence = evidence;
            this.eventTicket = eventTicket;
        }
    }

    /** Exclusive admission and file-event provenance for one paired path operation. */
    abstract class PairPathOperationLease {
        private final String operation;
        private final boolean mutatesSourcePair;
        private final boolean invalidatesRetainedPair;
        private FileSystem.AtomicAction action;
        private String recoveryConflict;
        private boolean awaitingEditorClose;
        private boolean closed;

        PairPathOperationLease(
                String operation,
                boolean mutatesSourcePair,
                boolean invalidatesRetainedPair) {
            this.operation = Objects.requireNonNull(operation, "operation");
            this.mutatesSourcePair = mutatesSourcePair;
            this.invalidatesRetainedPair = invalidatesRetainedPair;
        }

        final String operation() {
            return operation;
        }

        final void awaitingEditorClose(boolean awaiting) {
            awaitingEditorClose = awaiting;
        }

        private boolean replayReadyLocked() {
            return !closed
                    && !awaitingEditorClose
                    && recoveryConflict == null
                    && pairPathOperationStateCleanLocked();
        }

        final void bind(FileSystem.AtomicAction pathAction) throws IOException {
            Objects.requireNonNull(pathAction, "pathAction");
            synchronized (PairSaveCoordinator.this) {
                if (closed
                        || awaitingEditorClose
                        || activePairPathOperation != this
                        || action != null
                        || recoveryConflict != null
                        || !pairPathOperationStateCleanLocked()) {
                    throw new IOException(
                            "The paired " + operation
                            + " lease lost its clean exclusive authority before "
                            + "the filesystem transaction began");
                }
                action = pathAction;
            }
        }

        /**
         * Converts an exact completed shell-close proof into transaction
         * authority without calling {@code editor.close()} a second time.
         */
        final void claimAfterEditorClose(
                FlutterDesignerEditorClosePermitCoordinator.PostCloseProof proof)
                throws IOException {
            Objects.requireNonNull(proof, "proof");
            if (!proof.claim(editor.editorClosePermits(), this)) {
                throw new IOException(
                        "the post-close proof is stale, foreign, or already consumed");
            }
            synchronized (PairSaveCoordinator.this) {
                if (closed
                        || activePairPathOperation != this
                        || !awaitingEditorClose
                        || !pairPathOperationStateCleanLocked()) {
                    throw new IOException(
                            "the paired editor state changed while "
                            + operation
                            + " was waiting for every Designer shell to close");
                }
                awaitingEditorClose = false;
            }
        }

        final void markRecoveryConflict(String reason) {
            Objects.requireNonNull(reason, "reason");
            synchronized (PairSaveCoordinator.this) {
                if (!closed && activePairPathOperation == this) {
                    recoveryConflict = reason;
                }
            }
        }

        final void finish(boolean committed) {
            StateChange change = null;
            boolean invalidateRetainedPair = false;
            synchronized (PairSaveCoordinator.this) {
                if (closed) {
                    return;
                }
                closed = true;
                if (activePairPathOperation != this) {
                    LOGGER.log(Level.WARNING,
                            "The paired {0} lease lost coordinator ownership before finish",
                            operation);
                    return;
                }
                activePairPathOperation = null;
                if (action != null) {
                    pairPathOperationProvenance = action;
                }
                if (committed && mutatesSourcePair) {
                    diskBaseline = null;
                    invalidateRetainedPair = invalidatesRetainedPair;
                    if (invalidateRetainedPair) {
                        externalEventEpoch++;
                        sourceStateEpoch++;
                        unsavedPairHistory.clear();
                        unsavedHistoryCursor = null;
                        unsavedHistoryOwner = null;
                        suppressConflictSaveCookie = false;
                        if (recoveryConflict == null) {
                            change = transitionLocked(
                                    PairSaveCoordinatorStatus.CLEAN, null);
                        }
                    }
                }
                if (recoveryConflict != null) {
                    change = transitionLocked(
                            PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                            "Paired " + operation
                            + " lost exact filesystem authority: "
                            + recoveryConflict);
                }
            }
            if (invalidateRetainedPair) {
                try {
                    controller.pairPathOperationCommitted();
                } catch (RuntimeException presentationFailure) {
                    LOGGER.log(Level.WARNING,
                            "The Flutter Designer pair path operation committed, "
                            + "but its retained presentation could not be invalidated",
                            presentationFailure);
                }
            }
            publishEffects(change);
        }

        private boolean owns(FileEvent event) {
            return action != null && event.firedFrom(action);
        }
    }

    @FunctionalInterface
    interface PairPathReplay<T> {
        T run() throws IOException;
    }

    final class PairDeleteLease extends PairPathOperationLease {
        PairDeleteLease() {
            super("Delete", true, false);
        }
    }

    final class PairRenameLease extends PairPathOperationLease {
        PairRenameLease() {
            super("Rename", true, true);
        }
    }

    /**
     * A cross-folder move retires the old path-bound DataObjects.  Their file
     * deletion events perform invalidation, so unlike Rename this lease must
     * not ask the retained controller to reload obsolete FileObjects.
     */
    final class PairMoveLease extends PairPathOperationLease {
        PairMoveLease() {
            super("Move", true, false);
        }
    }

    final class PairCopyLease extends PairPathOperationLease {
        PairCopyLease() {
            super("Copy", false, false);
        }

        void verifySourceStillClean() throws IOException {
            synchronized (PairSaveCoordinator.this) {
                if (activePairPathOperation != this
                        || sourceDirty
                        || failedSavePending
                        || dataObject.isModified()
                        || editor.sourceModified()
                        || conflictStatusLocked()) {
                    throw new IOException(
                            "the source pair changed while its copy was being prepared");
                }
            }
        }
    }

    private final class ExactPairOutput extends OutputStream
            implements GuardedPersistenceRejectionSink {
        private final ActivePairSave attempt;
        private final byte[] expected;
        private final byte[] buffer;
        private int count;
        private boolean closed;
        private IOException closeFailure;
        private String guardedRejection;

        ExactPairOutput(ActivePairSave attempt) {
            this.attempt = attempt;
            expected = attempt.proof.candidateDartBytes();
            buffer = new byte[expected.length];
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            buffer[count++] = (byte) value;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            ensureCapacity(length);
            System.arraycopy(bytes, offset, buffer, count, length);
            count += length;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                if (closeFailure != null) {
                    throw closeFailure;
                }
                return;
            }
            closed = true;
            try {
                rejectGuardedFallbackIfPresent();
                if (count != expected.length || !Arrays.equals(buffer, expected)) {
                    throw new IOException(
                            "NetBeans serialized Dart bytes differ from the exact "
                            + "analyzed pair candidate");
                }
                StagedPairProof proof = attempt.proof;
                synchronized (PairSaveCoordinator.this) {
                    if (activePairSave != attempt) {
                        throw new IOException(
                                "The active pair-save authority changed before transaction start");
                    }
                }
                PairFileTransactionResult result = pairTransaction.commit(
                        new PairFileTransactionRequest(
                                dartFile,
                                designerFile,
                                proof.baselineDartBytes(),
                                proof.baselineFdBytes(),
                                expected,
                                proof.preparedPairIdentity()
                                        .prospectiveFdBytes()));
                synchronized (PairSaveCoordinator.this) {
                    if (activePairSave != attempt) {
                        throw new IOException(
                                "The active pair-save epoch changed during output");
                    }
                    attempt.result = result;
                }
                if (!result.committed()) {
                    throw new IOException(transactionReason(result));
                }
            } catch (IOException failure) {
                closeFailure = failure;
                throw failure;
            } catch (RuntimeException failure) {
                closeFailure = new IOException(
                        "Cannot commit the exact Flutter Designer file pair: "
                        + reason(failure), failure);
                throw closeFailure;
            }
        }

        @Override
        public void rejectGuardedPersistence(String reason) {
            guardedRejection = Objects.requireNonNull(reason, "reason");
        }

        private void rejectGuardedFallbackIfPresent() throws IOException {
            if (guardedRejection != null) {
                throw new IOException(
                        "Cannot commit the Flutter Designer pair because guarded "
                        + "Dart serialization was rejected: " + guardedRejection);
            }
        }

        private void ensureCapacity(int additional) throws IOException {
            if (closed) {
                throw new IOException("The paired Dart output stream is closed");
            }
            if (additional < 0 || count > expected.length - additional) {
                throw new IOException(
                        "NetBeans serialized more Dart bytes than the exact "
                        + "analyzed pair candidate");
            }
        }
    }

    private final class ExactSourceOutput extends OutputStream
            implements GuardedPersistenceRejectionSink {
        private final ActiveSourceSave attempt;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean closed;
        private IOException closeFailure;
        private String guardedRejection;

        ExactSourceOutput(ActiveSourceSave attempt) {
            this.attempt = attempt;
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            buffer.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            ensureCapacity(length);
            buffer.write(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                if (closeFailure != null) {
                    throw closeFailure;
                }
                return;
            }
            closed = true;
            try {
                rejectGuardedFallbackIfPresent();
                // Ordinary Source save retains the editor's exact serialized
                // bytes, including supported BOM/newline conventions. Strict
                // LF/BOM-free validation belongs only to Designer generation.
                byte[] candidate = buffer.toByteArray();
                if (attempt.historyPlan != null
                        && !Arrays.equals(
                                candidate,
                                attempt.historyPlan.candidate()
                                        .serializedDartBytes())) {
                    throw new IOException(
                            "NetBeans serialized Dart bytes differ from the exact planned Source-history anchor");
                }
                if (attempt.semanticBaselinePlan != null
                        && !Arrays.equals(
                                candidate,
                                attempt.semanticBaselinePlan.candidate()
                                        .serializedDartBytes())) {
                    throw new IOException(
                            "NetBeans serialized Dart bytes differ from the exact planned semantic BASELINE revision");
                }
                requireBoundStateFieldsForSourceSave(attempt, candidate);
                synchronized (PairSaveCoordinator.this) {
                    if (activeSourceSave != attempt) {
                        throw new IOException(
                                "The active source-save authority changed before transaction start");
                    }
                }
                PairFileTransactionResult result = transaction.commit(
                        new PairFileTransactionRequest(
                                dartFile,
                                designerFile,
                                attempt.baseline.dartBytes(),
                                attempt.baseline.fdBytes(),
                                candidate,
                                attempt.semanticBaselinePlan == null ? attempt.baseline.fdBytes()
                                        : attempt.semanticBaselinePlan.savedBaseline().fdBytes()));
                // Retain a durable outcome even when a post-transaction
                // identity check fails; COMMITTED must never be misreported as
                // a retryable serializer failure.
                attempt.result = result;
                attempt.committedDart = candidate;
                synchronized (PairSaveCoordinator.this) {
                    if (activeSourceSave != attempt) {
                        throw new IOException(
                            "The active source-save epoch changed during output");
                    }
                }
                if (!result.committed()) {
                    throw new IOException(transactionReason(result));
                }
            } catch (IOException failure) {
                closeFailure = failure;
                throw failure;
            } catch (RuntimeException failure) {
                closeFailure = new IOException(
                        "Cannot commit exact Dart source bytes: "
                        + reason(failure), failure);
                throw closeFailure;
            }
        }

        @Override
        public void rejectGuardedPersistence(String reason) {
            guardedRejection = Objects.requireNonNull(reason, "reason");
        }

        private void rejectGuardedFallbackIfPresent() throws IOException {
            if (guardedRejection != null) {
                throw new IOException(
                        "Cannot commit Dart source because guarded serialization "
                        + "was rejected: " + guardedRejection);
            }
        }

        private void ensureCapacity(int additional) throws IOException {
            if (closed) {
                throw new IOException("The source Dart output stream is closed");
            }
            if (additional < 0 || buffer.size()
                    > MAX_SOURCE_PERSISTENCE_BYTES - additional) {
                throw new IOException(
                        "The serialized Dart source exceeds the "
                        + MAX_SOURCE_PERSISTENCE_BYTES
                        + " byte writable safety limit");
            }
        }
    }

    /** Checks the exact serialized source against the exact .fd bytes the transaction retains. */
    private static void requireBoundStateFieldsForSourceSave(ActiveSourceSave attempt, byte[] candidate)
            throws IOException {
        byte[] fd = attempt.semanticBaselinePlan == null ? attempt.baseline.fdBytes()
                : attempt.semanticBaselinePlan.savedBaseline().fdBytes();
        FdDecodeResult decoded;
        try {
            decoded = new FdDocumentCodec().decode(fd);
        } catch (dev.flutter.netbeans.designer.codec.FdInputLimitException tooLarge) {
            throw new IOException("Cannot validate State-bound Source against the retained .fd: "
                    + tooLarge.getMessage(), tooLarge);
        }
        if (!(decoded instanceof FdDecodeResult.Current current)) return;
        var bindings = new java.util.ArrayList<dev.flutter.netbeans.designer.model.StateBinding>();
        var propertyBindings = new java.util.ArrayList<dev.flutter.netbeans.designer.model.StatePropertyBinding>();
        var pending = new ArrayDeque<dev.flutter.netbeans.designer.model.WidgetNode>();
        pending.push(current.document().root());
        while (!pending.isEmpty()) {
            var node = pending.pop();
            node.stateBinding().ifPresent(bindings::add);
            propertyBindings.addAll(node.propertyBindings().values());
            for (var slot : node.slots().values()) {
                switch (slot) {
                    case dev.flutter.netbeans.designer.model.WidgetSlot.SingleSlot single -> single.child().ifPresent(pending::push);
                    case dev.flutter.netbeans.designer.model.WidgetSlot.ListSlot list -> list.children().forEach(pending::push);
                }
            }
        }
        if (bindings.isEmpty() && propertyBindings.isEmpty()) return;
        try {
            if (current.document().source().widgetKind() != dev.flutter.netbeans.designer.model.WidgetClassKind.STATEFUL) {
                throw new IllegalArgumentException("Bound values require a Stateful form.");
            }
            var scanner = new DartSourceIntegrityScanner();
            var original = scanner.scan(attempt.baseline.dartBytes(), current.document().source());
            var prospective = scanner.scan(candidate, current.document().source());
            String owner = original.verifiedMemberClassName().orElseThrow(() ->
                    new IllegalArgumentException("The durable State field owner is not verified."));
            if (!prospective.onDiskDeclaredMatch()
                    || !prospective.verifiedMemberClassName().filter(owner::equals).isPresent()) {
                throw new IllegalArgumentException("Source must retain the verified State owner and managed regions while values are bound.");
            }
            dev.flutter.netbeans.designer.events.DartEventHandlerSource.requireStateBindingFields(candidate, owner, bindings);
            dev.flutter.netbeans.designer.events.DartEventHandlerSource.requirePropertyBindingFields(candidate, owner, propertyBindings);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Cannot save State-bound source: " + invalid.getMessage(), invalid);
        }
    }

    @FunctionalInterface
    private interface IoOperation<T> {
        T run() throws IOException;
    }

    /** Exact presentation epoch changed before a pair mutation was reserved. */
    static final class StalePairEpochException extends IOException {
        private StalePairEpochException(String message) {
            super(message);
        }
    }

    /** Package-private injection seam for deterministic persistence tests. */
    @FunctionalInterface
    interface PairTransaction {
        PairFileTransactionResult commit(PairFileTransactionRequest request)
                throws IOException;
    }

    /** Package-private injection seam for deterministic persistence tests. */
    @FunctionalInterface
    interface FdOnlyTransaction {
        PairFileTransactionResult commit(PairFileTransactionRequest request)
                throws IOException;
    }
}
