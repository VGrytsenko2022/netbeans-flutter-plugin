package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.plugin.dart.DartTokenId;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.core.api.multiview.MultiViews;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.netbeans.spi.editor.guards.GuardedSectionsFactory;
import org.netbeans.spi.editor.guards.GuardedSectionsProvider;
import org.openide.awt.UndoRedo;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.PrintCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.DataEditorSupport;
import org.openide.util.Mutex;
import org.openide.util.RequestProcessor;
import org.openide.windows.CloneableOpenSupport;
import org.openide.windows.CloneableTopComponent;
import org.openide.xml.XMLUtil;

/**
 * Dart editor support shared by the Source designer view and guarded-section
 * persistence.
 */
final class FlutterDesignerEditorSupport extends DataEditorSupport
        implements OpenCookie,
        EditCookie,
        EditorCookie.Observable,
        PrintCookie,
        CloseCookie,
        LineCookie {

    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerEditorSupport.class.getName());
    private static final RequestProcessor POST_CLOSE_OPERATIONS =
            new RequestProcessor(
                    FlutterDesignerEditorSupport.class.getName()
                    + "-post-close", 1, true);

    private PairSaveCoordinator pairSaveCoordinator;
    private DesignerCombinedUndoRedo combinedUndoRedo;
    private volatile boolean sourceModified;
    private volatile GuardedDocumentBridge guardedEditor;
    private volatile DartGuardedSectionsProvider guardedProvider;
    private volatile StyledDocument liveDocumentIdentity;
    private final ThreadLocal<DesignerMutationContext> designerMutation =
            new ThreadLocal<>();
    private final ThreadLocal<NativeHistoryReplay> nativeHistoryReplay =
            new ThreadLocal<>();
    private final ThreadLocal<DartGuardedSectionsProvider.PersistenceAttempt>
            activePersistenceAttempt = new ThreadLocal<>();
    private final ThreadLocal<Boolean> dedicatedPaneCreation =
            new ThreadLocal<>();
    private final FlutterDesignerEditorClosePermitCoordinator editorClosePermits =
            new FlutterDesignerEditorClosePermitCoordinator();
    /** EDT-confined owner of one support-wide asynchronous close attempt. */
    private FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch
            supportCloseBatch;
    /** EDT-confined request retaining clone admission through its callback. */
    private PostCloseRequest<?> postCloseRequest;
    private volatile PersistenceFinalizationHook persistenceFinalizationHook =
            () -> { };

    FlutterDesignerEditorSupport(
            FlutterDesignerDataObject dataObject,
            MultiDataObject.Entry dartEntry,
            CookieSet cookies) {
        super(dataObject, dataObject.getLookup(), new Environment(dataObject, dartEntry));
        setMIMEType(DartTokenId.MIME_TYPE);
    }

    void bindPairSaveCoordinator(PairSaveCoordinator coordinator) {
        Objects.requireNonNull(coordinator, "coordinator");
        if (pairSaveCoordinator != null) {
            throw new IllegalStateException("The pair-save coordinator is already bound");
        }
        pairSaveCoordinator = coordinator;
    }

    void bindCombinedUndoRedo(DesignerCombinedUndoRedo undoRedo) {
        Objects.requireNonNull(undoRedo, "undoRedo");
        if (combinedUndoRedo != null) {
            throw new IllegalStateException(
                    "The combined Undo/Redo bridge is already bound");
        }
        combinedUndoRedo = undoRedo;
    }

    @Override
    protected boolean asynchronousOpen() {
        return true;
    }

    @Override
    protected CloneableEditorSupport.Pane createPane() {
        if (FlutterDesignerEditorShellRoute.PRODUCTION_ENABLED
                || Boolean.TRUE.equals(dedicatedPaneCreation.get())) {
            return createDedicatedPane();
        }
        CloneableTopComponent component = MultiViews.createCloneableMultiView(
                FlutterDesignerMime.MIME_TYPE,
                getDataObject());
        if (getDataObject().isValid()) {
            // MIME-created MultiViewCloneableTopComponent deliberately starts
            // with an empty name. The active Design element is not a CES Pane,
            // so no Source pane exists yet to supply the initial editor title.
            component.setDisplayName(messageName());
            component.setHtmlDisplayName(messageHtmlName());
            component.setToolTipText(messageToolTip());
        }
        return (CloneableEditorSupport.Pane) component;
    }

    /** Phase-one construction seam for the plugin-owned, non-Splitable shell. */
    CloneableEditorSupport.Pane createDedicatedPane() {
        requireDedicatedPaneCreationOnEdt();
        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                reservation = null;
        boolean ownsConstruction = dedicatedPaneCreation.get() == null;
        if (ownsConstruction) {
            reservation = requireCloneCreationReservation();
            dedicatedPaneCreation.set(Boolean.TRUE);
        }
        try {
            FlutterDesignerCloneableEditor component =
                    new FlutterDesignerCloneableEditor(this);
            initializeCloneableEditor(component);
            if (getDataObject().isValid()) {
                component.updateName();
            }
            if (ownsConstruction) {
                releaseCloneCreationAfterReferenceRegistration(reservation);
                reservation = null;
            }
            return component;
        } finally {
            if (ownsConstruction) {
                dedicatedPaneCreation.remove();
                if (reservation != null) {
                    reservation.close();
                }
            }
        }
    }

    /** Creates a clone through CES so its private Pane identity is installed. */
    CloneableTopComponent createDedicatedCloneComponent() {
        requireDedicatedPaneCreationOnEdt();
        if (dedicatedPaneCreation.get() != null) {
            throw new IllegalStateException(
                    "Nested dedicated Designer pane construction is not allowed");
        }
        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                reservation = requireCloneCreationReservation();
        try {
            dedicatedPaneCreation.set(Boolean.TRUE);
            try {
                CloneableTopComponent component = createCloneableTopComponent();
                releaseCloneCreationAfterReferenceRegistration(reservation);
                reservation = null;
                return component;
            } finally {
                dedicatedPaneCreation.remove();
            }
        } finally {
            if (reservation != null) {
                reservation.close();
            }
        }
    }

    private static void requireDedicatedPaneCreationOnEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Flutter Designer editor panes must be created on the "
                    + "Event Dispatch Thread so clone registration is atomic");
        }
    }

    private static void releaseCloneCreationAfterReferenceRegistration(
            FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                    reservation) {
        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                retained = Objects.requireNonNull(reservation, "reservation");
        // NetBeans registers a clone in Ref only after createPane() or
        // createClonedObject() returns. Since both callers execute on this EDT,
        // the next event is the first safe point at which the exact topology is
        // externally visible to close admission.
        EventQueue.invokeLater(retained::close);
    }

    private FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
            requireCloneCreationReservation() {
        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                reservation = editorClosePermits.reserveCloneCreation();
        if (reservation == null) {
            throw new IllegalStateException(
                    "Cannot create or clone the Flutter Designer editor while "
                    + "another clone is completing a peer-safe close");
        }
        return reservation;
    }

    FlutterDesignerEditorClosePermitCoordinator editorClosePermits() {
        return editorClosePermits;
    }

    /** Whether the dormant dedicated-shell route currently owns any clone. */
    boolean hasDedicatedEditorShells() {
        return editorClosePermits.lifecycleShellCount() > 0;
    }

    /**
     * Atomic EDT gate used only by the legacy synchronous DataObject path.
     * It must never turn a newly observed dedicated shell into an asynchronous
     * close that the synchronous caller would mistake for cancellation.
     */
    boolean closeForSynchronousPairPathOperation() {
        if (!EventQueue.isDispatchThread()) {
            return Mutex.EVENT.writeAccess(
                    (Mutex.Action<Boolean>)
                            this::closeForSynchronousPairPathOperation);
        }
        if (editorClosePermits.lifecycleShellCount() > 0) {
            return false;
        }
        return close(true);
    }

    /**
     * Closes every dedicated shell and then runs one exact filesystem
     * continuation off the EDT while clone admission remains reserved.
     *
     * <p>The returned stage cannot be externally cancelled. A caller receiving
     * {@link PostCloseStatus#NOT_REQUIRED} must use its existing synchronous
     * DataObject path; no callback has run in that case.</p>
     */
    <T> CompletionStage<PostCloseOutcome<T>> requestPostCloseOperation(
            String operation,
            String target,
            PostCloseAction<T> action) {
        String admittedOperation = Objects.requireNonNull(operation, "operation");
        String admittedTarget = Objects.requireNonNull(target, "target");
        PostCloseAction<T> admittedAction = Objects.requireNonNull(action, "action");
        if (!EventQueue.isDispatchThread()) {
            return Mutex.EVENT.writeAccess(
                    (Mutex.Action<CompletionStage<PostCloseOutcome<T>>>) () ->
                            requestPostCloseOperation(
                                    admittedOperation,
                                    admittedTarget,
                                    admittedAction));
        }

        CompletableFuture<PostCloseOutcome<T>> completion =
                new CompletableFuture<>();
        if (editorClosePermits.lifecycleShellCount() == 0) {
            completion.complete(PostCloseOutcome.rejected(
                    PostCloseStatus.NOT_REQUIRED,
                    "no dedicated Flutter Designer editor shell is open"));
            return completion.minimalCompletionStage();
        }
        if (postCloseRequest != null || supportCloseBatch != null) {
            completion.complete(PostCloseOutcome.rejected(
                    PostCloseStatus.BUSY,
                    "another support close or post-close operation is active"));
            return completion.minimalCompletionStage();
        }

        PostCloseRequest<T> request = new PostCloseRequest<>(
                admittedOperation, admittedTarget, admittedAction, completion);
        final FlutterDesignerEditorClosePermitCoordinator.SupportCloseAdmission
                admission;
        try {
            admission = editorClosePermits.beginSupportCloseForContinuation(
                    this::editorShellCloneTopology,
                    this::editorShellDocumentRevision,
                    this::confirmEditorShellDocumentClose);
        } catch (RuntimeException | Error failure) {
            completion.complete(PostCloseOutcome.rejected(
                    PostCloseStatus.CLOSE_FAILED,
                    reason(failure)));
            if (failure instanceof Error error) {
                throw error;
            }
            return completion.minimalCompletionStage();
        }
        if (!admission.authorized()) {
            completion.complete(PostCloseOutcome.rejected(
                    mapPostCloseStatus(admission.status()),
                    supportCloseRejectionReason(admission.status())));
            return completion.minimalCompletionStage();
        }

        postCloseRequest = request;
        supportCloseBatch = admission.batch();
        dispatchSupportCloseOwner(admission.batch());
        return completion.minimalCompletionStage();
    }

    /** Acquires NetBeans' ordinary Save/Discard/Cancel decision exactly once. */
    boolean confirmEditorShellDocumentClose() {
        return super.canClose();
    }

    /** Exact document and pair identity retained by an asynchronous close. */
    FlutterDesignerEditorClosePermitCoordinator.DocumentRevision
            editorShellDocumentRevision() {
        StyledDocument document = getDocument();
        long documentVersion = document == null
                ? -1 : DocumentUtilities.getDocumentVersion(document);
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        PairSaveCoordinator.CloseRevision pairRevision = coordinator == null
                ? null : coordinator.closeRevision();
        return new FlutterDesignerEditorClosePermitCoordinator.DocumentRevision(
                document,
                documentVersion,
                pairRevision,
                isModified(),
                sourceModified);
    }

    /** The user-facing editor represents the visible model, not its technical Dart owner. */
    @Override
    protected String messageName() {
        FlutterDesignerDataObject dataObject = designerDataObject();
        if (!dataObject.isValid()) {
            return "";
        }
        return DataEditorSupport.annotateName(
                dataObject.getModelFile().getNameExt(),
                false,
                isModified(),
                pairIsReadOnly(dataObject));
    }

    @Override
    protected String messageHtmlName() {
        FlutterDesignerDataObject dataObject = designerDataObject();
        if (!dataObject.isValid()) {
            return null;
        }
        try {
            String escapedModelName = XMLUtil.toElementContent(
                    dataObject.getModelFile().getNameExt());
            return DataEditorSupport.annotateName(
                    escapedModelName,
                    true,
                    isModified(),
                    pairIsReadOnly(dataObject));
        } catch (CharConversionException invalidXmlCharacter) {
            return null;
        }
    }

    @Override
    protected String messageToolTip() {
        FlutterDesignerDataObject dataObject = designerDataObject();
        if (!dataObject.isValid()) {
            return "";
        }
        FileObject model = dataObject.getModelFile();
        FileObject source = dataObject.getPrimaryFile();
        StringBuilder tooltip = new StringBuilder(160)
                .append("Flutter Designer model: ")
                .append(FileUtil.getFileDisplayName(model))
                .append(". Paired Dart source: ")
                .append(FileUtil.getFileDisplayName(source))
                .append('.');
        if (isModified()) {
            tooltip.append(" The paired form has unsaved changes.");
        }
        if (!model.canWrite()) {
            tooltip.append(" The Flutter Designer model is read-only.");
        }
        if (!source.canWrite()) {
            tooltip.append(" The paired Dart source is read-only.");
        }
        return tooltip.toString();
    }

    private FlutterDesignerDataObject designerDataObject() {
        return (FlutterDesignerDataObject) getDataObject();
    }

    private static boolean pairIsReadOnly(FlutterDesignerDataObject dataObject) {
        return !dataObject.getModelFile().canWrite()
                || !dataObject.getPrimaryFile().canWrite();
    }

    @Override
    protected boolean notifyModified() {
        NativeHistoryReplay replay = nativeHistoryReplay.get();
        if (replay != null) {
            StyledDocument document = getDocument();
            if (document == null || document != replay.documentIdentity) {
                replay.identityRejected = true;
                return false;
            }
            // A raw delegate replay is still a real CES document edit. Let
            // CloneableEditorSupport and its private UndoRedoManager own the
            // modified/savepoint transition, but do not misclassify this
            // internal half of one semantic entry as an external Source edit.
            if (!super.notifyModified()) {
                replay.modifiedStateRejected = true;
                return false;
            }
            sourceModified = true;
            return true;
        }
        DesignerMutationContext internal = designerMutation.get();
        if (internal != null) {
            // Use CES' ordinary admission before the first guarded document
            // edit. A rejection must precede MIME-wrapper admission and the
            // pair/model commit. If a later apply step rolls back atomically,
            // the exact predecessor deliberately remains dirty/fail-closed
            // until an ordinary Source Save or explicit recovery.
            if (!super.notifyModified()) {
                return false;
            }
            sourceModified = true;
            if (!internal.modified) {
                internal.modified = true;
                PairSaveCoordinator coordinator = pairSaveCoordinator;
                if (coordinator != null) {
                    coordinator.sourceBecameModified();
                }
            }
            return true;
        }
        if (!super.notifyModified()) {
            return false;
        }
        sourceModified = true;
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (coordinator != null) {
            coordinator.sourceBecameModified();
        }
        return true;
    }

    @Override
    protected void notifyUnmodified() {
        super.notifyUnmodified();
        sourceModified = false;
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (coordinator != null) {
            coordinator.sourceBecameUnmodified();
        }
    }

    /** Routes every direct editor save through the single pair-aware owner. */
    @Override
    public void saveDocument() throws IOException {
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (coordinator == null) {
            saveDocumentThroughNetBeans();
            return;
        }
        coordinator.save();
    }

    /**
     * Executes the standard NetBeans serialization/savepoint lifecycle after
     * the coordinator has selected either the source-only or pair output edge.
     */
    void saveDocumentThroughNetBeans() throws IOException {
        if (activePersistenceAttempt.get() != null) {
            // DataEditorSupport may confirm CloneableEditorSupport's external
            // change question by re-entering the virtual saveDocument(). The
            // nested save must reuse, not nest, the same guarded persistence
            // attempt; the outer frame publishes it exactly once.
            super.saveDocument();
            return;
        }
        DartGuardedSectionsProvider provider = guardedProvider;
        if (provider == null || !provider.supportsExplicitPersistenceAttempt()) {
            super.saveDocument();
            return;
        }
        try (DartGuardedSectionsProvider.PersistenceAttempt attempt =
                provider.beginPersistenceAttempt()) {
            activePersistenceAttempt.set(attempt);
            try {
                super.saveDocument();
                persistenceFinalizationHook.afterDurableOutput();
                attempt.commit();
            } catch (IOException | RuntimeException failure) {
                attempt.abort();
                throw failure;
            } finally {
                activePersistenceAttempt.remove();
            }
        }
    }

    void setPersistenceFinalizationHook(PersistenceFinalizationHook hook) {
        persistenceFinalizationHook = Objects.requireNonNull(hook, "hook");
    }

    void clearPersistenceFinalizationHook() {
        persistenceFinalizationHook = () -> { };
    }

    /**
     * The stock CloseCookie path asks CES once and then closes every clone in a
     * synchronous batch. A dedicated Designer shell instead admits one exact
     * support-wide batch and retires each clone-local Canvas in sequence. The
     * initiating synchronous API must return {@code false}: completion is
     * deliberately asynchronous and is owned by the batch callbacks below.
     */
    @Override
    protected boolean close(boolean ask) {
        if (!EventQueue.isDispatchThread()) {
            // CloneableOpenSupport would otherwise perform its own later EDT
            // hop only after this override returned from authorization.  Keep
            // the exact gate check and the stock clone batch in one EDT turn
            // so an already-queued shell open/clone cannot cross between them.
            return Mutex.EVENT.writeAccess(
                    (Mutex.Action<Boolean>) () -> close(ask));
        }
        FlutterDesignerEditorClosePermitCoordinator.CloneTopology
                authorizationTopology = null;
        FlutterDesignerEditorClosePermitCoordinator.DocumentRevision
                authorizationRevision = null;
        if (editorClosePermits.lifecycleShellCount() > 0) {
            try {
                authorizationTopology = editorShellCloneTopology();
                authorizationRevision = editorShellDocumentRevision();
            } catch (RuntimeException | Error snapshotFailure) {
                LOGGER.log(Level.WARNING,
                        "Operation: authorize internal Flutter Designer support "
                        + "close. Target: "
                        + getDataObject().getPrimaryFile().getPath()
                        + ". Reason: the exact post-admission snapshot could "
                        + "not be read.",
                        snapshotFailure);
                return false;
            }
        }
        if (editorClosePermits.authorizeSupportClose(
                ask, authorizationTopology, authorizationRevision)) {
            if (!editorClosePermits
                    .authorizedSupportCloseRequiresCompletion()) {
                return super.close(ask);
            }
            boolean closed = false;
            try {
                closed = super.close(ask);
                return closed;
            } finally {
                FlutterDesignerEditorClosePermitCoordinator.CloneTopology
                        completionTopology = null;
                FlutterDesignerEditorClosePermitCoordinator.DocumentRevision
                        completionRevision = null;
                boolean completionSnapshotExact = false;
                try {
                    completionTopology = editorShellCloneTopology();
                    completionRevision = editorShellDocumentRevision();
                    completionSnapshotExact = true;
                } catch (RuntimeException | Error snapshotFailure) {
                    LOGGER.log(Level.WARNING,
                            "Operation: verify internal Flutter Designer support "
                            + "close. Target: "
                            + getDataObject().getPrimaryFile().getPath()
                            + ". Reason: the exact completion snapshot could "
                            + "not be read.",
                            snapshotFailure);
                }
                if (!editorClosePermits.completeSupportClose(
                        closed && completionSnapshotExact,
                        completionTopology,
                        completionRevision)) {
                    LOGGER.log(Level.SEVERE,
                            "Operation: acknowledge internal Flutter Designer "
                            + "support close. Target: {0}. Reason: the exact "
                            + "componentClosed authorization scope was lost.",
                            getDataObject().getPrimaryFile().getPath());
                }
            }
        }
        if (!ask) {
            return false;
        }

        FlutterDesignerEditorClosePermitCoordinator.SupportCloseAdmission
                admission = editorClosePermits.beginSupportClose(
                        this::editorShellCloneTopology,
                        this::editorShellDocumentRevision,
                        this::confirmEditorShellDocumentClose);
        if (!admission.authorized()) {
            return false;
        }
        FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch =
                admission.batch();
        supportCloseBatch = batch;
        dispatchSupportCloseOwner(batch);
        return false;
    }

    /** Snapshot of the CES clone registry in its canonical enumeration order. */
    private FlutterDesignerEditorClosePermitCoordinator.CloneTopology
            editorShellCloneTopology() {
        ArrayList<Object> members = new ArrayList<>();
        var components = allEditors.getComponents();
        while (components.hasMoreElements()) {
            members.add(components.nextElement());
        }
        return new FlutterDesignerEditorClosePermitCoordinator.CloneTopology(
                members);
    }

    private void dispatchSupportCloseOwner(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> dispatchSupportCloseOwner(batch));
            return;
        }
        if (supportCloseBatch != batch) {
            return;
        }
        Object nextOwner = editorClosePermits.nextSupportCloseOwner(batch);
        if (!(nextOwner instanceof FlutterDesignerCloneableEditor editor)
                || !editor.isOpened()) {
            abortSupportClose(batch,
                    "the next admitted editor clone was unavailable");
            return;
        }
        final boolean accepted;
        try {
            accepted = editor.requestSupportClose(batch);
        } catch (RuntimeException | Error failure) {
            abortSupportClose(batch,
                    "the next editor clone failed while starting its "
                    + "peer-safe close: " + failure.getMessage());
            if (failure instanceof Error error) {
                throw error;
            }
            return;
        }
        if (!accepted) {
            abortSupportClose(batch,
                    "the next admitted editor clone refused its peer-safe close");
        }
    }

    void supportCloseOwnerCompleted(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            boolean finalOwner) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() ->
                    supportCloseOwnerCompleted(batch, finalOwner));
            return;
        }
        if (supportCloseBatch != batch) {
            return;
        }
        if (finalOwner) {
            PostCloseRequest<?> request = postCloseRequest;
            if (request == null) {
                supportCloseBatch = null;
                return;
            }
            FlutterDesignerEditorClosePermitCoordinator.PostCloseProof proof =
                    editorClosePermits.beginPostCloseContinuation(batch);
            if (proof == null) {
                editorClosePermits.abortPostCloseContinuation(batch);
                supportCloseBatch = null;
                postCloseRequest = null;
                request.reject(
                        PostCloseStatus.CLOSE_FAILED,
                        "the exact post-close reservation could not be claimed");
                return;
            }
            if (!request.start()) {
                editorClosePermits.completePostCloseContinuation(batch, proof);
                supportCloseBatch = null;
                postCloseRequest = null;
                return;
            }
            POST_CLOSE_OPERATIONS.post(() ->
                    runPostCloseOperation(request, batch, proof));
            return;
        }
        // Leave the batch reservation active across this event boundary. This
        // avoids a recursive componentClosed stack while still preventing a
        // clone or an ordinary close from crossing between owners.
        EventQueue.invokeLater(() -> dispatchSupportCloseOwner(batch));
    }

    private <T> void runPostCloseOperation(
            PostCloseRequest<T> request,
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            FlutterDesignerEditorClosePermitCoordinator.PostCloseProof proof) {
        PostCloseOutcome<T> outcome;
        Error fatalFailure = null;
        try {
            T value = request.action.run(proof);
            outcome = PostCloseOutcome.succeeded(value);
        } catch (Exception failure) {
            outcome = PostCloseOutcome.rejected(
                    PostCloseStatus.OPERATION_FAILED,
                    reason(failure));
        } catch (Error failure) {
            // Reservation cleanup must still run on the EDT, but VM-fatal and
            // linkage failures must not be silently converted into an ordinary
            // command rejection on the RequestProcessor thread.
            fatalFailure = failure;
            outcome = PostCloseOutcome.rejected(
                    PostCloseStatus.OPERATION_FAILED,
                    reason(failure));
        }
        PostCloseOutcome<T> completedOutcome = outcome;
        EventQueue.invokeLater(() -> finishPostCloseOperation(
                request, batch, proof, completedOutcome));
        if (fatalFailure != null) {
            throw fatalFailure;
        }
    }

    private <T> void finishPostCloseOperation(
            PostCloseRequest<T> request,
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            FlutterDesignerEditorClosePermitCoordinator.PostCloseProof proof,
            PostCloseOutcome<T> outcome) {
        if (postCloseRequest != request || supportCloseBatch != batch) {
            LOGGER.log(Level.SEVERE,
                    "Operation: finish {0}. Target: {1}. Reason: the exact "
                    + "post-close request lost its editor-support identity; "
                    + "clone admission remains fail-closed.",
                    new Object[] {request.operation, request.target});
            request.complete(outcome);
            return;
        }
        if (!editorClosePermits.completePostCloseContinuation(batch, proof)) {
            LOGGER.log(Level.SEVERE,
                    "Operation: finish {0}. Target: {1}. Reason: the exact "
                    + "post-close reservation could not be released; clone "
                    + "admission remains fail-closed.",
                    new Object[] {request.operation, request.target});
        }
        postCloseRequest = null;
        supportCloseBatch = null;
        request.complete(outcome);
    }

    void supportCloseOwnerFailed(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            String reason) {
        Objects.requireNonNull(reason, "reason");
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> supportCloseOwnerFailed(batch, reason));
            return;
        }
        abortSupportClose(batch, reason);
    }

    private void abortSupportClose(
            FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch batch,
            String reason) {
        if (supportCloseBatch != batch) {
            return;
        }
        boolean released = editorClosePermits.abortSupportClose(batch);
        boolean stillActive = editorClosePermits.supportCloseActive(batch);
        PostCloseRequest<?> request = postCloseRequest;
        if (!stillActive) {
            supportCloseBatch = null;
            postCloseRequest = null;
        }
        LOGGER.log(stillActive ? Level.SEVERE : Level.WARNING,
                "Operation: close all Flutter Designer editor clones. Target: "
                + "{0}. Reason: {1}{2}",
                new Object[] {
                    getDataObject().getPrimaryFile().getPath(),
                    reason,
                    stillActive
                            ? "; the batch reservation remains fail-closed."
                            : released ? "." : "; the batch was already aborted."
                });
        if (request != null) {
            request.reject(PostCloseStatus.CLOSE_FAILED, reason);
        }
    }

    private static PostCloseStatus mapPostCloseStatus(
            FlutterDesignerEditorClosePermitCoordinator
                    .SupportCloseAdmissionStatus status) {
        return switch (status) {
            case ACQUIRED -> throw new IllegalArgumentException(
                    "An acquired support close is not a rejection");
            case BUSY -> PostCloseStatus.BUSY;
            case CANCELLED -> PostCloseStatus.CANCELLED;
            case STALE -> PostCloseStatus.STALE;
            case INVALID_TOPOLOGY -> PostCloseStatus.INVALID_TOPOLOGY;
        };
    }

    private static String supportCloseRejectionReason(
            FlutterDesignerEditorClosePermitCoordinator
                    .SupportCloseAdmissionStatus status) {
        return switch (status) {
            case ACQUIRED -> "the support close was acquired";
            case BUSY -> "another editor close or clone operation is active";
            case CANCELLED -> "the user cancelled the editor close";
            case STALE -> "the editor topology or document revision changed";
            case INVALID_TOPOLOGY ->
                    "the dedicated editor clone topology is incomplete or mixed";
        };
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    /** Virtual serialization must never advance the last persisted fallback. */
    @Override
    public InputStream getInputStream() throws IOException {
        DartGuardedSectionsProvider provider = guardedProvider;
        if (provider == null || !provider.supportsExplicitPersistenceAttempt()) {
            return super.getInputStream();
        }
        try (DartGuardedSectionsProvider.PersistenceAttempt attempt =
                provider.beginPersistenceAttempt()) {
            return super.getInputStream();
        }
    }

    @Override
    protected void loadFromStreamToKit(
            StyledDocument document,
            InputStream stream,
            EditorKit kit) throws IOException, BadLocationException {
        byte[] exactSource = readBoundedSource(stream);
        GuardedSectionsProvider provider = guardedProvider(document);
        if (provider == null) {
            super.loadFromStreamToKit(
                    document, new ByteArrayInputStream(exactSource), kit);
            publishLoadedBaseline(exactSource);
            return;
        }

        Charset charset = FileEncodingQuery.getEncoding(getDataObject().getPrimaryFile());
        try (Reader reader = provider.createGuardedReader(
                new ByteArrayInputStream(exactSource), charset)) {
            kit.read(reader, document, 0);
        }
        publishLoadedBaseline(exactSource);
    }

    @Override
    protected void saveFromKitToStream(
            StyledDocument document,
            EditorKit kit,
            OutputStream stream) throws IOException, BadLocationException {
        GuardedSectionsProvider provider = guardedProvider;
        if (provider == null) {
            super.saveFromKitToStream(document, kit, stream);
            return;
        }

        Charset charset = FileEncodingQuery.getEncoding(getDataObject().getPrimaryFile());
        try (Writer writer = provider.createGuardedWriter(stream, charset)) {
            kit.write(writer, document, 0, document.getLength());
        }
    }

    @Override
    protected void notifyClosed() {
        liveDocumentIdentity = null;
        super.notifyClosed();
        guardedProvider = null;
        guardedEditor = null;
    }

    /**
     * Lock-free NetBeans document CAS used by command-source tokens. The
     * editor-util version is backed by an {@code AtomicLong}, advances for
     * every text mutation (including edit-then-revert), and requires no extra
     * listener on the BaseDocument.
     */
    long liveDocumentVersion(StyledDocument expectedIdentity) {
        StyledDocument before = liveDocumentIdentity;
        if (before != expectedIdentity) {
            return Long.MIN_VALUE;
        }
        long version = DocumentUtilities.getDocumentVersion(before);
        return liveDocumentIdentity == before ? version : Long.MIN_VALUE;
    }

    LiveDartDocumentSnapshot liveSnapshot() throws IOException {
        StyledDocument document = openDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (provider == null || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException("The paired Dart guarded document is unavailable");
        }
        return LiveDartDocumentBridge.snapshot(document, provider);
    }

    /**
     * Opens the EDT-local context around the raw document delegate of one
     * semantic native-history Undo/Redo action.
     *
     * <p>The owning {@link DesignerCombinedUndoRedo} action already supplies
     * the outward history/notification barrier. This context deliberately
     * starts no second notification deferral: it only distinguishes the raw
     * CES document mutation from a user-authored Source edit. The returned
     * token must remain open through any exact raw-document recovery and be
     * closed by the replay owner on every outcome.</p>
     */
    NativeHistoryReplay beginNativeHistoryReplay(
            StyledDocument documentIdentity) throws IOException {
        Objects.requireNonNull(documentIdentity, "documentIdentity");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Native Flutter Designer history replay must run on the Event Dispatch Thread");
        }
        if (designerMutation.get() != null
                || nativeHistoryReplay.get() != null) {
            throw new IOException(
                    "Nested Flutter Designer native-history replay is not allowed");
        }
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != documentIdentity
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before native-history replay");
        }
        NativeHistoryReplay replay = new NativeHistoryReplay(document);
        nativeHistoryReplay.set(replay);
        return replay;
    }

    /**
     * True only for Environment.markModified reached from this editor's exact
     * current-thread raw semantic-history delegate. CES must still perform
     * its ordinary false-to-true modified transition, but that internal edge
     * is not a new user-authored Source modification.
     */
    private boolean ownsNativeHistoryModifiedAdmission() {
        if (!EventQueue.isDispatchThread()) {
            return false;
        }
        NativeHistoryReplay replay = nativeHistoryReplay.get();
        StyledDocument document = getDocument();
        return replay != null
                && !replay.closed
                && document != null
                && replay.documentIdentity == document;
    }

    /**
     * Verifies and publishes the exact raw native-history target while the
     * same Dart document atomic lock excludes another edit.
     *
     * <p>The supplied finalizer receives one fresh managed snapshot and may
     * update semantic ownership only. A document mutation by that callback is
     * detected before this method returns. This method deliberately does not
     * close the context: if it fails, the semantic edit must first reverse its
     * raw delegate under the same internal classification, then close the
     * token from its abort path.</p>
     */
    LiveDartDocumentSnapshot verifyNativeReplayAndFinalize(
            StyledDocument documentIdentity,
            byte[] expectedTargetBytes,
            NativeReplayFinalizer finalizer) throws IOException {
        Objects.requireNonNull(documentIdentity, "documentIdentity");
        Objects.requireNonNull(expectedTargetBytes, "expectedTargetBytes");
        Objects.requireNonNull(finalizer, "finalizer");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Native Flutter Designer history replay must be finalized on the Event Dispatch Thread");
        }

        NativeHistoryReplay replay = nativeHistoryReplay.get();
        if (replay == null) {
            throw new IOException(
                    "No Flutter Designer native-history replay context is active");
        }
        byte[] exactTarget = LiveDartDocumentSnapshot.strictWritableUtf8(
                expectedTargetBytes);
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (replay.documentIdentity != documentIdentity
                || document == null
                || document != documentIdentity
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed during native-history replay");
        }
        if (replay.identityRejected || replay.modifiedStateRejected) {
            throw new IOException(replay.identityRejected
                    ? "CES rejected a native replay mutation for a foreign Dart document"
                    : "CES rejected the native replay modified-state transition");
        }

        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<LiveDartDocumentSnapshot> verified =
                new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> verifyNativeReplayWithinDocumentLock(
                document,
                provider,
                exactTarget,
                finalizer,
                verified,
                failure));
        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot result = verified.get();
        if (result == null) {
            throw new IOException(
                    "The verified Flutter Designer native replay snapshot is unavailable");
        }
        return result;
    }

    private static void verifyNativeReplayWithinDocumentLock(
            StyledDocument document,
            DartGuardedSectionsProvider provider,
            byte[] exactTarget,
            NativeReplayFinalizer finalizer,
            AtomicReference<LiveDartDocumentSnapshot> verified,
            AtomicReference<IOException> failure) {
        LiveDartDocumentSnapshot target = null;
        Throwable primary = null;
        boolean finalizerStarted = false;
        try {
            target = LiveDartDocumentBridge.snapshot(document, provider);
            if (!Arrays.equals(exactTarget, target.markerBearingUtf8())) {
                throw new IOException(
                        "The raw native-history replay did not reach its exact expected Dart bytes");
            }
            finalizerStarted = true;
            finalizer.finish(target);
        } catch (IOException | RuntimeException | Error replayFailure) {
            primary = replayFailure;
        }

        if (finalizerStarted && target != null) {
            try {
                LiveDartDocumentSnapshot afterFinalizer =
                        LiveDartDocumentBridge.snapshot(document, provider);
                if (!target.sameEvidence(afterFinalizer)) {
                    throw new IOException(
                            "The native replay finalizer mutated the live Dart document");
                }
            } catch (IOException | RuntimeException | Error verificationFailure) {
                if (primary == null) {
                    primary = verificationFailure;
                } else if (primary != verificationFailure) {
                    primary.addSuppressed(verificationFailure);
                }
            }
        }

        if (primary != null) {
            failure.set(primary instanceof IOException io
                    ? io
                    : new IOException(
                            "Cannot finalize the exact Flutter Designer native replay",
                            primary));
            return;
        }
        verified.set(target);
    }

    LiveDartDocumentSnapshot applyPreparedRegions(
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes) throws IOException {
        return applyPreparedRegionsAndFinalize(
                expected,
                generated,
                expectedCandidateBytes,
                applied -> { });
    }

    /**
     * Applies an analyzed candidate and finalizes its exact fresh live
     * identity while the same document lock still excludes another edit.
     */
    LiveDartDocumentSnapshot applyPreparedRegionsAndFinalize(
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes,
            LiveDartDocumentBridge.AppliedSnapshotFinalizer finalizer)
            throws IOException {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(generated, "generated");
        Objects.requireNonNull(expectedCandidateBytes, "expectedCandidateBytes");
        Objects.requireNonNull(finalizer, "finalizer");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expected.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before apply");
        }
        return performDesignerDocumentMutation(
                true,
                () -> LiveDartDocumentBridge.applyManagedRegions(
                        provider,
                        expected,
                        generated,
                        expectedCandidateBytes,
                        finalizer));
    }

    /**
     * Applies one generated pair candidate as one native semantic history edge.
     *
     * <p>The capture token is installed before the bridge acquires its outer
     * document atomic lock. The supplied finalizer verifies exact pair/model
     * evidence under that same lock and returns a prevalidated, callback-free
     * joint commit. Sealing is deliberately the last operation in the bridge
     * finalizer. The native manager acknowledges the wrapped edit at atomic
     * unlock before the joint commit may move semantic authority, while
     * outward publication remains queued in the DataObject-owned combined
     * history barrier.</p>
     */
    LiveDartDocumentSnapshot applyPreparedRegionsAndFinalize(
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes,
            ForwardSemanticEdge semanticEdge,
            AppliedSemanticFinalizer finalizer) throws IOException {
        return applyPreparedRegionsAndFinalize(expected, generated, expectedCandidateBytes,
                null, semanticEdge, finalizer);
    }

    LiveDartDocumentSnapshot applyPreparedRegionsAndFinalize(
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes,
            dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan transition,
            ForwardSemanticEdge semanticEdge,
            AppliedSemanticFinalizer finalizer) throws IOException {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(generated, "generated");
        Objects.requireNonNull(expectedCandidateBytes, "expectedCandidateBytes");
        Objects.requireNonNull(semanticEdge, "semanticEdge");
        Objects.requireNonNull(finalizer, "finalizer");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expected.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before semantic apply");
        }

        DesignerCombinedUndoRedo outwardHistory = combinedUndoRedo;
        if (outwardHistory == null) {
            throw new IllegalStateException(
                    "The combined Undo/Redo bridge is not bound");
        }
        DesignerSemanticUndoableEdit.PublicationQueue publicationQueue =
                outwardHistory::enqueueSemanticPublication;
        UndoRedo.Manager nativeHistory = getUndoRedo();

        return performDesignerDocumentMutation(true, () -> {
            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                try {
                    LiveDartDocumentSnapshot applied =
                            LiveDartDocumentBridge.applyManagedRegions(
                                    provider,
                                    expected,
                                    generated,
                                    expectedCandidateBytes,
                                    transition,
                                    exactApplied -> {
                                        try {
                                            DesignerAtomicEditCapture.JointCommit
                                                    jointCommit = Objects.requireNonNull(
                                                            finalizer.verifyAndPrepare(
                                                                    exactApplied),
                                                            "semantic joint commit");
                                            // Keep this as the final fallible
                                            // action under the document lock.
                                            // Anything that can reject exact
                                            // evidence must happen before the
                                            // capture becomes READY.
                                            capture.seal(
                                                    semanticEdge.beforeRevisionId(),
                                                    semanticEdge.afterRevisionId(),
                                                    semanticEdge.presentationName(),
                                                    semanticEdge.replayController(),
                                                    publicationQueue,
                                                    jointCommit);
                                        } catch (IOException
                                                | RuntimeException
                                                | Error rejection) {
                                            // LiveDartDocumentBridge will now
                                            // atomicUndo before outer unlock.
                                            // Abort first so that its rollback
                                            // compound is never mistaken for a
                                            // forward semantic admission.
                                            capture.abort();
                                            throw rejection;
                                        }
                                    });
                    // BaseDocument delivers the MIME wrapper and native-manager
                    // acknowledgement from atomicUnlockImpl before runAtomic
                    // returns. Verification therefore runs only after the
                    // outer write lock has been released.
                    capture.verifyCompleted();
                    return applied;
                } catch (IOException | RuntimeException | Error failure) {
                    abortUnadmittedCapture(capture, failure);
                    throw failure;
                }
            }
        });
    }

    /** Exact semantic admission with no source mutation or synthetic Undo delegate. */
    LiveDartDocumentSnapshot applyUnchangedSourceAndFinalize(
            LiveDartDocumentSnapshot expected,
            byte[] exactBytes,
            ForwardSemanticEdge edge,
            AppliedSemanticFinalizer finalizer) throws IOException {
        Objects.requireNonNull(expected, "expected");
        DesignerCombinedUndoRedo outward = Objects.requireNonNull(combinedUndoRedo,
                "combined Undo/Redo bridge");
        try (var notifications = outward.deferNotifications();
                NativeHistoryReplay replay = beginNativeHistoryReplay(expected.documentIdentity());
                DesignerAtomicEditCapture capture = DesignerAtomicEditCapture.begin(
                expected.documentIdentity(), getUndoRedo())) {
            try {
                LiveDartDocumentSnapshot applied = verifyNativeReplayAndFinalize(
                        expected.documentIdentity(), exactBytes, fresh -> {
                            if (!expected.sameEvidence(fresh)) {
                                throw new IOException("The unchanged-source admission lost its exact live revision");
                            }
                            var commit = finalizer.verifyAndPrepare(fresh);
                            capture.seal(edge.beforeRevisionId(), edge.afterRevisionId(),
                                    edge.presentationName(), edge.replayController(),
                                    outward::enqueueSemanticPublication, commit);
                            capture.admitUnchangedSource();
                        });
                capture.verifyCompleted();
                return applied;
            } catch (IOException | RuntimeException | Error failure) {
                abortUnadmittedCapture(capture, failure);
                throw failure;
            }
        }
    }

    private static void abortUnadmittedCapture(
            DesignerAtomicEditCapture capture,
            Throwable primary) {
        if (capture.committed() || capture.aborted()) {
            return;
        }
        try {
            // ACTIVE/READY means LiveDartDocumentBridge rejected the apply and
            // completed atomicUndo before returning the failure. WRAPPED or
            // POISONED cannot be called an atomic rejection; abort() rejects
            // those states and preserves that fact as a suppressed cause for
            // the pair coordinator's fail-closed recovery path.
            capture.abort();
        } catch (RuntimeException | Error abortFailure) {
            if (abortFailure != primary) {
                try {
                    primary.addSuppressed(abortFailure);
                } catch (RuntimeException | Error ignored) {
                    // Preserve the original document/admission failure.
                }
            }
        }
    }

    LiveDartDocumentSnapshot restoreAndFinalizePreparation(
            LiveDartDocumentSnapshot expectedApplied,
            LiveDartDocumentSnapshot restore,
            boolean discardUndoHistory,
            Runnable finalizer) throws IOException {
        Objects.requireNonNull(expectedApplied, "expectedApplied");
        Objects.requireNonNull(restore, "restore");
        Objects.requireNonNull(finalizer, "finalizer");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expectedApplied.documentIdentity()
                || document != restore.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before restore");
        }
        return performDesignerDocumentMutation(
                false,
                () -> LiveDartDocumentBridge.restoreManagedRegions(
                        provider,
                        expectedApplied,
                        restore,
                        () -> finalizePreparationWithinDocumentLock(
                                discardUndoHistory, finalizer)));
    }

    /**
     * Restores a staged predecessor and binds its fresh document revision while
     * the same document lock still excludes another edit.
     */
    LiveDartDocumentSnapshot restoreAndFinalizeReplacement(
            LiveDartDocumentSnapshot expectedApplied,
            LiveDartDocumentSnapshot restore,
            LiveDartDocumentBridge.RestoredSnapshotFinalizer finalizer)
            throws IOException {
        Objects.requireNonNull(expectedApplied, "expectedApplied");
        Objects.requireNonNull(restore, "restore");
        Objects.requireNonNull(finalizer, "finalizer");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expectedApplied.documentIdentity()
                || document != restore.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before replacement rollback");
        }
        return performDesignerDocumentMutation(
                false,
                () -> LiveDartDocumentBridge.restoreManagedRegions(
                        provider,
                        expectedApplied,
                        restore,
                        restored -> {
                            getUndoRedo().discardAllEdits();
                            finalizer.finish(restored);
                        }));
    }

    void verifyAndFinalizePreparation(
            LiveDartDocumentSnapshot expected,
            boolean discardUndoHistory,
            Runnable finalizer) throws IOException {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(finalizer, "finalizer");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expected.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The paired Dart live document identity changed before finalization");
        }
        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                deferUndoRedoNotifications()) {
            LiveDartDocumentBridge.verifyAndFinalize(
                    provider,
                    expected,
                    () -> finalizePreparationWithinDocumentLock(
                            discardUndoHistory, finalizer));
        }
    }

    /**
     * Recovers a failed post-commit guarded finalization only when the live
     * document is still clean and serializes to the exact durable Dart bytes.
     *
     * <p>The proof and guarded-fallback adoption execute in one EDT/document
     * atomic section. A dirty or mismatching revision is retained untouched;
     * this method never reloads or discards it.</p>
     */
    CommittedRevisionRecovery recoverCommittedRevisionIfCleanAndExact(
            byte[] exactCommittedDart) throws IOException {
        Objects.requireNonNull(exactCommittedDart, "exactCommittedDart");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Committed editor recovery must run on the Event Dispatch Thread");
        }
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null || provider == null || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            return CommittedRevisionRecovery.retained(
                    "the guarded live Dart document is unavailable");
        }

        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<CommittedRevisionRecovery> result = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            if (sourceModified) {
                result.set(CommittedRevisionRecovery.retained(
                        "the live Dart document contains a newer dirty edit"));
                return;
            }
            try (DartGuardedSectionsProvider.PersistenceAttempt attempt =
                    provider.beginPersistenceAttempt()) {
                ByteArrayOutputStream serialized = new ByteArrayOutputStream(
                        Math.min(exactCommittedDart.length, 64 * 1024));
                saveFromKitToStream(
                        document, createEditorKit(), serialized);
                byte[] exactLive = serialized.toByteArray();
                if (!Arrays.equals(exactCommittedDart, exactLive)) {
                    result.set(CommittedRevisionRecovery.retained(
                            "the clean live Dart serialization differs from the "
                            + "durable committed revision"));
                    return;
                }
                attempt.commit();
                result.set(CommittedRevisionRecovery.success());
            } catch (BadLocationException | IOException | RuntimeException ex) {
                failure.set(ex instanceof IOException io
                        ? io
                        : new IOException(
                                "Cannot verify the committed live Dart revision",
                                ex));
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        CommittedRevisionRecovery recovery = result.get();
        if (recovery == null) {
            throw new IOException(
                    "The committed live Dart recovery result is unavailable");
        }
        return recovery;
    }

    private void finalizePreparationWithinDocumentLock(
            boolean discardUndoHistory,
            Runnable finalizer) {
        if (discardUndoHistory) {
            getUndoRedo().discardAllEdits();
        }
        // Never notify CES unmodified after an observed apply/rollback
        // mutation. Keeping the exact restored B revision dirty is fail-closed
        // and avoids arbitrary DataObject callbacks under the document lock.
        finalizer.run();
    }

    /**
     * Classifies the guarded mutation while retaining CES' ordinary modified
     * admission before its first document edit. Pair callbacks are still
     * hidden by the enclosing effects barrier until recovery or joint commit.
     */
    private <T> T performDesignerDocumentMutation(
            boolean requireWritable,
            DesignerMutationOperation<T> operation) throws IOException {
        Objects.requireNonNull(operation, "operation");
        if (designerMutation.get() != null) {
            throw new IOException(
                    "Nested Flutter Designer document mutation is not allowed");
        }
        if (requireWritable
                && !getDataObject().getPrimaryFile().canWrite()) {
            throw new IOException("Dart source is read-only: "
                    + getDataObject().getPrimaryFile().getPath());
        }
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (requireWritable && coordinator != null) {
            coordinator.beforeSourceModification();
        }

        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                deferUndoRedoNotifications()) {
            DesignerMutationContext context = new DesignerMutationContext();
            designerMutation.set(context);
            T result = null;
            Throwable primary = null;
            try {
                result = operation.run();
            } catch (Throwable failure) {
                primary = failure;
            } finally {
                designerMutation.remove();
            }
            if (primary instanceof IOException ioFailure) {
                throw ioFailure;
            }
            if (primary instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (primary instanceof Error error) {
                throw error;
            }
            if (primary != null) {
                throw new IOException(
                        "Cannot mutate the Flutter Designer live Dart document",
                        primary);
            }
            return result;
        }
    }

    /**
     * Verifies a committed live pair without changing native history or the
     * private CES savepoint. The fresh snapshot is returned after the document
     * lock has been released.
     */
    LiveDartDocumentSnapshot verifyCommittedPairWithoutHistoryMutation(
            LiveDartDocumentSnapshot expectedCommitted,
            LiveDartDocumentBridge.CommittedPairContentPolicy contentPolicy)
            throws IOException {
        Objects.requireNonNull(expectedCommitted, "expectedCommitted");
        Objects.requireNonNull(contentPolicy, "contentPolicy");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expectedCommitted.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The live Dart guarded document identity changed after pair commit");
        }
        return LiveDartDocumentBridge
                .verifyCommittedPairWithoutHistoryMutation(
                        provider, expectedCommitted, contentPolicy);
    }

    /** Verifies managed live text and seals the Undo boundary after pair commit. */
    LiveDartDocumentSnapshot sealCommittedPair(
            LiveDartDocumentSnapshot expectedManaged) throws IOException {
        Objects.requireNonNull(expectedManaged, "expectedManaged");
        StyledDocument document = getDocument();
        DartGuardedSectionsProvider provider = guardedProvider;
        if (document == null
                || document != expectedManaged.documentIdentity()
                || provider == null
                || guardedEditor == null
                || guardedEditor.getDocument() != document) {
            throw new IOException(
                    "The live Dart guarded document is unavailable after pair commit");
        }
        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                deferUndoRedoNotifications()) {
            return LiveDartDocumentBridge.sealCommittedPair(
                    provider,
                    expectedManaged,
                    () -> getUndoRedo().discardAllEdits());
        }
    }

    private DesignerCombinedUndoRedo.NotificationDeferral
            deferUndoRedoNotifications() {
        DesignerCombinedUndoRedo undoRedo = combinedUndoRedo;
        if (undoRedo == null) {
            throw new IllegalStateException(
                    "The combined Undo/Redo bridge is not bound");
        }
        return undoRedo.deferNotifications();
    }

    /** Construction-only access for the DataObject-owned outward bridge. */
    UndoRedo.Manager nativeUndoRedoManagerForCombinedBridge() {
        return getUndoRedo();
    }

    record CommittedRevisionRecovery(boolean recovered, String reason) {
        CommittedRevisionRecovery {
            reason = Objects.requireNonNull(reason, "reason");
            if (recovered && !reason.isEmpty()) {
                throw new IllegalArgumentException(
                        "A recovered revision cannot retain a conflict reason");
            }
            if (!recovered && reason.isBlank()) {
                throw new IllegalArgumentException(
                        "A retained revision requires a concrete reason");
            }
        }

        static CommittedRevisionRecovery success() {
            return new CommittedRevisionRecovery(true, "");
        }

        static CommittedRevisionRecovery retained(String reason) {
            return new CommittedRevisionRecovery(false, reason);
        }
    }

    @FunctionalInterface
    interface PersistenceFinalizationHook {
        void afterDurableOutput() throws IOException;
    }

    @FunctionalInterface
    interface NativeReplayFinalizer {
        void finish(LiveDartDocumentSnapshot target) throws IOException;
    }

    /** Immutable identity of one forward semantic native-history edge. */
    record ForwardSemanticEdge(
            long beforeRevisionId,
            long afterRevisionId,
            String presentationName,
            DesignerSemanticUndoableEdit.ReplayController replayController) {
        ForwardSemanticEdge {
            if (beforeRevisionId < 0 || afterRevisionId < 0
                    || beforeRevisionId == afterRevisionId) {
                throw new IllegalArgumentException(
                        "A forward semantic edge requires distinct non-negative revision ids");
            }
            presentationName = Objects.requireNonNull(
                    presentationName, "presentationName").strip();
            if (presentationName.isEmpty()) {
                throw new IllegalArgumentException(
                        "A forward semantic edge requires a presentation name");
            }
            Objects.requireNonNull(replayController, "replayController");
        }
    }

    /** Exact under-lock evidence verification followed by a no-throw commit. */
    @FunctionalInterface
    interface AppliedSemanticFinalizer {
        DesignerAtomicEditCapture.JointCommit verifyAndPrepare(
                LiveDartDocumentSnapshot applied) throws IOException;
    }

    @FunctionalInterface
    private interface DesignerMutationOperation<T> {
        T run() throws IOException;
    }

    private static final class DesignerMutationContext {
        private boolean modified;
    }

    final class NativeHistoryReplay implements AutoCloseable {
        private final StyledDocument documentIdentity;
        private boolean identityRejected;
        private boolean modifiedStateRejected;
        private boolean closed;

        private NativeHistoryReplay(StyledDocument documentIdentity) {
            this.documentIdentity = documentIdentity;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (!EventQueue.isDispatchThread()
                    || nativeHistoryReplay.get() != this) {
                throw new IllegalStateException(
                        "The native-history replay token must be closed by its owning EDT context");
            }
            closed = true;
            nativeHistoryReplay.remove();
        }
    }

    boolean sourceModified() {
        return sourceModified;
    }

    private void publishLoadedBaseline(byte[] exactSource) {
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (coordinator != null) {
            coordinator.sourceBaselineLoaded(exactSource);
        }
    }

    private static byte[] readBoundedSource(InputStream stream)
            throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8_192];
            int read;
            while ((read = stream.read(chunk)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (output.size()
                        > PairSaveCoordinator.MAX_SOURCE_PERSISTENCE_BYTES - read) {
                    throw new IOException(
                            "The Dart source exceeds the pair persistence safety "
                            + "limit of "
                            + PairSaveCoordinator.MAX_SOURCE_PERSISTENCE_BYTES
                            + " bytes");
                }
                output.write(chunk, 0, read);
            }
            return output.toByteArray();
        }
    }

    private GuardedSectionsProvider guardedProvider(StyledDocument document) {
        if (guardedEditor == null) {
            guardedEditor = new GuardedDocumentBridge();
            GuardedSectionsFactory factory = GuardedSectionsFactory.find(DartTokenId.MIME_TYPE);
            if (factory != null) {
                GuardedSectionsProvider created = factory.create(guardedEditor);
                if (created instanceof DartGuardedSectionsProvider dartProvider) {
                    guardedProvider = dartProvider;
                }
            }
        }
        guardedEditor.document = document;
        liveDocumentIdentity = document;
        return guardedProvider;
    }

    private static final class GuardedDocumentBridge implements GuardedEditorSupport {
        private StyledDocument document;

        @Override
        public StyledDocument getDocument() {
            return document;
        }
    }

    @FunctionalInterface
    interface PostCloseAction<T> {
        T run(FlutterDesignerEditorClosePermitCoordinator.PostCloseProof proof)
                throws Exception;
    }

    enum PostCloseStatus {
        SUCCEEDED,
        NOT_REQUIRED,
        CANCELLED,
        BUSY,
        STALE,
        INVALID_TOPOLOGY,
        CLOSE_FAILED,
        OPERATION_FAILED
    }

    record PostCloseOutcome<T>(
            PostCloseStatus status,
            T value,
            String reason) {
        PostCloseOutcome {
            Objects.requireNonNull(status, "status");
            if (status == PostCloseStatus.SUCCEEDED) {
                if (reason != null) {
                    throw new IllegalArgumentException(
                            "A successful post-close operation has no rejection reason");
                }
            } else if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "A rejected post-close operation requires a reason");
            }
        }

        static <T> PostCloseOutcome<T> succeeded(T value) {
            return new PostCloseOutcome<>(PostCloseStatus.SUCCEEDED, value, null);
        }

        static <T> PostCloseOutcome<T> rejected(
                PostCloseStatus status,
                String reason) {
            if (status == PostCloseStatus.SUCCEEDED) {
                throw new IllegalArgumentException(
                        "Use succeeded() for a successful post-close operation");
            }
            return new PostCloseOutcome<>(status, null, reason);
        }

        boolean succeeded() {
            return status == PostCloseStatus.SUCCEEDED;
        }
    }

    private static final class PostCloseRequest<T> {
        private final String operation;
        private final String target;
        private final PostCloseAction<T> action;
        private final CompletableFuture<PostCloseOutcome<T>> completion;
        private boolean started;
        private boolean completed;

        PostCloseRequest(
                String operation,
                String target,
                PostCloseAction<T> action,
                CompletableFuture<PostCloseOutcome<T>> completion) {
            this.operation = operation;
            this.target = target;
            this.action = action;
            this.completion = completion;
        }

        synchronized void reject(PostCloseStatus status, String reason) {
            complete(PostCloseOutcome.rejected(status, reason));
        }

        synchronized boolean start() {
            if (started || completed) {
                return false;
            }
            started = true;
            return true;
        }

        synchronized void complete(PostCloseOutcome<T> outcome) {
            if (completed) {
                return;
            }
            completed = true;
            completion.complete(Objects.requireNonNull(outcome, "outcome"));
        }
    }

    private static final class Environment extends DataEditorSupport.Env {
        private static final long serialVersionUID = 1L;

        private final MultiDataObject.Entry dartEntry;
        private final FlutterDesignerDataObject dataObject;

        Environment(DataObject dataObject, MultiDataObject.Entry dartEntry) {
            super(dataObject);
            this.dataObject = (FlutterDesignerDataObject) dataObject;
            this.dartEntry = dartEntry;
        }

        /**
         * Do not retain a Dart lock for the complete edit session. Pair Save
         * must acquire Dart and FD locks together in stable canonical order.
         */
        @Override
        public void markModified() throws IOException {
            FileObject file = getFile();
            if (!file.canWrite()) {
                throw new IOException("Dart source is read-only: " + file.getPath());
            }
            PairSaveCoordinator coordinator = dataObject.getPairSaveCoordinator();
            FlutterDesignerEditorSupport editor = dataObject.getEditorSupport();
            if (coordinator != null
                    && (editor == null
                        || !editor.ownsNativeHistoryModifiedAdmission())) {
                coordinator.beforeSourceModification();
            }
            dataObject.setModified(true);
        }

        @Override
        public OutputStream outputStream() throws IOException {
            PairSaveCoordinator coordinator = dataObject.getPairSaveCoordinator();
            OutputStream paired = coordinator == null
                    ? null : coordinator.openActiveOutput();
            if (paired != null) {
                return paired;
            }
            if (coordinator != null) {
                throw new IOException(
                        "The pair-save coordinator did not authorize a Dart output stream");
            }

            FileLock lock = takeLock();
            try {
                return new LockReleasingOutputStream(
                        getFile().getOutputStream(lock), lock);
            } catch (IOException | RuntimeException failure) {
                lock.releaseLock();
                throw failure;
            }
        }

        @Override
        protected FileObject getFile() {
            return dartEntry.getFile();
        }

        @Override
        protected FileLock takeLock() throws IOException {
            return dartEntry.takeLock();
        }

        @Override
        public String getMimeType() {
            return DartTokenId.MIME_TYPE;
        }

        @Override
        public CloneableOpenSupport findCloneableOpenSupport() {
            return getDataObject().getCookie(FlutterDesignerEditorSupport.class);
        }

        private static final class LockReleasingOutputStream
                extends FilterOutputStream {
            private final FileLock lock;
            private boolean closed;

            LockReleasingOutputStream(OutputStream delegate, FileLock lock) {
                super(delegate);
                this.lock = lock;
            }

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }
                closed = true;
                try {
                    super.close();
                } finally {
                    lock.releaseLock();
                }
            }
        }
    }
}
