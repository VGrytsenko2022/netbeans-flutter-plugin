package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.CatalogBuildResult;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdInputLimitException;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.plugin.designer.catalog.NetBeansWidgetCatalogProvider;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.RequestProcessor;

/**
 * Owns the bounded background load lifecycle for one paired designer object.
 * The controller verifies bounded on-disk Dart source integrity, runs the
 * deterministic generator and evaluates read-only three-way evidence. It
 * exposes no Dart or FD write until live-editor/analyzer checks and a
 * transactional Save exist.
 */
public final class FlutterDesignerDocumentController implements Node.Cookie {
    public static final String PROP_STATE = "state";
    public static final String SOURCE_FILE_MISMATCH = "designer.source.dartFile.mismatch";

    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerDocumentController.class.getName());
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterDesignerDocumentController.class.getName(), 2, true);

    private final FileObject dartFile;
    private final FileObject modelFile;
    private final LoadOperation loadOperation;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final AtomicLong generation = new AtomicLong();
    private final FileChangeAdapter pairListener = new FileChangeAdapter() {
        @Override
        public void fileChanged(FileEvent event) {
            pairFileChanged(event);
        }

        @Override
        public void fileDeleted(FileEvent event) {
            pairFileChanged(event);
        }

        @Override
        public void fileRenamed(FileRenameEvent event) {
            pairFileChanged(event);
        }
    };

    private volatile FlutterDesignerDocumentState state =
            new FlutterDesignerDocumentState.Idle();
    private volatile PairSaveCoordinator pairSaveCoordinator;
    private int openViews;
    private boolean loadRunning;
    private boolean reloadRequested;

    FlutterDesignerDocumentController(FileObject dartFile, FileObject modelFile) {
        this(
                dartFile,
                modelFile,
                new FdDocumentCodec(),
                new WidgetTreeValidator(),
                new NetBeansWidgetCatalogProvider(),
                new DartSourceIntegrityScanner());
    }

    FlutterDesignerDocumentController(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            NetBeansWidgetCatalogProvider catalogProvider,
            DartSourceIntegrityScanner sourceScanner) {
        this(
                dartFile,
                modelFile,
                catalogLoadOperation(
                        dartFile, modelFile, codec, validator, catalogProvider,
                        sourceScanner));
    }

    FlutterDesignerDocumentController(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            WidgetCatalog catalog) {
        this(
                dartFile,
                modelFile,
                fixedCatalogLoadOperation(
                        dartFile, modelFile, codec, validator, catalog,
                        new DartSourceIntegrityScanner()));
    }

    FlutterDesignerDocumentController(
            FileObject dartFile,
            FileObject modelFile,
            LoadOperation loadOperation) {
        this.dartFile = Objects.requireNonNull(dartFile, "dartFile");
        this.modelFile = Objects.requireNonNull(modelFile, "modelFile");
        this.loadOperation = Objects.requireNonNull(loadOperation, "loadOperation");
        modelFile.addFileChangeListener(FileUtil.weakFileChangeListener(
                pairListener, modelFile));
        dartFile.addFileChangeListener(FileUtil.weakFileChangeListener(
                pairListener, dartFile));
    }

    public FlutterDesignerDocumentState state() {
        return state;
    }

    /**
     * Captures the exact loaded {@link FlutterDesignerDocumentState.Current}
     * identity and controller generation which a later durable pair adoption
     * must still own.
     *
     * <p>The generation is part of the authority even while {@link #state}
     * still names the same object: a reload requested after this capture may
     * already have an in-flight replacement which must not be silently
     * discarded by a late save finalizer.</p>
     */
    synchronized CurrentAdoptionTicket currentAdoptionTicket(
            FlutterDesignerDocumentState.Current expectedCurrent) {
        Objects.requireNonNull(expectedCurrent, "expectedCurrent");
        if (state != expectedCurrent) {
            throw new IllegalStateException(
                    "The expected Flutter Designer Current is no longer active");
        }
        return new CurrentAdoptionTicket(
                this, expectedCurrent, generation.get());
    }

    /**
     * Installs one already-derived durable Current without running listeners.
     * The returned one-shot effects object is the sole publication authority.
     */
    synchronized DeferredCurrentEffects adoptCurrentDeferred(
            CurrentAdoptionTicket ticket,
            FlutterDesignerDocumentState.Current savedCurrent) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(savedCurrent, "savedCurrent");
        if (ticket.owner != this
                || state != ticket.expectedCurrent
                || generation.get() != ticket.generation) {
            throw new IllegalStateException(
                    "The Flutter Designer Current or load generation changed before adoption");
        }

        FlutterDesignerDocumentState previous = state;
        // Invalidate both a worker result which has not yet reached the EDT and
        // a queued publication which already passed an earlier advisory check.
        generation.incrementAndGet();
        reloadRequested = false;
        state = savedCurrent;
        return new DeferredCurrentEffects(this, previous, savedCurrent);
    }

    void bindPairSaveCoordinator(PairSaveCoordinator coordinator) {
        Objects.requireNonNull(coordinator, "coordinator");
        if (pairSaveCoordinator != null) {
            throw new IllegalStateException("The pair-save coordinator is already bound");
        }
        pairSaveCoordinator = coordinator;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Registers an open Design clone and starts a fresh load for the first
     * clone.
     *
     * @return {@code true} when this opened the first clone and the caller must
     *         present Loading instead of any retained state from a closed view
     */
    public boolean viewOpened() {
        synchronized (this) {
            openViews++;
            if (openViews > 1) {
                return false;
            }
        }
        reload();
        return true;
    }

    /**
     * Unregisters an open Design clone. The last close suppresses pending
     * publications and prevents file events from scheduling more work.
     */
    public void viewClosed() {
        synchronized (this) {
            if (openViews == 0) {
                return;
            }
            openViews--;
            if (openViews == 0) {
                reloadRequested = false;
                generation.incrementAndGet();
            }
        }
    }

    /**
     * Requests a fresh bounded snapshot while at least one Design clone is
     * open. Repeated requests are coalesced behind at most one running load.
     */
    public void reload() {
        boolean scheduleWorker = false;
        synchronized (this) {
            if (openViews == 0) {
                return;
            }
            generation.incrementAndGet();
            reloadRequested = true;
            if (!loadRunning) {
                loadRunning = true;
                scheduleWorker = true;
            }
        }
        if (scheduleWorker) {
            WORKER.post(this::runLoads);
        }
    }

    /**
     * Invalidates every retained model/source identity after a committed pair
     * path change. The rename lease has already closed the shared editor, but
     * this also handles a late clone callback without exposing the old
     * {@code source.dartFile} descriptor.
     */
    void pairPathOperationCommitted() {
        FlutterDesignerDocumentState previous;
        FlutterDesignerDocumentState idle = new FlutterDesignerDocumentState.Idle();
        boolean scheduleWorker = false;
        synchronized (this) {
            generation.incrementAndGet();
            previous = state;
            state = idle;
            reloadRequested = openViews > 0;
            if (reloadRequested && !loadRunning) {
                loadRunning = true;
                scheduleWorker = true;
            }
        }
        Runnable publication = () -> changes.firePropertyChange(
                PROP_STATE, previous, idle);
        if (EventQueue.isDispatchThread()) {
            publication.run();
        } else {
            EventQueue.invokeLater(publication);
        }
        if (scheduleWorker) {
            WORKER.post(this::runLoads);
        }
    }

    FlutterDesignerDocumentState loadNow() {
        return loadOperation.load();
    }

    static FlutterDesignerDocumentState load(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            WidgetCatalog catalog) {
        return load(
                dartFile,
                modelFile,
                codec,
                validator,
                new CatalogBuildResult(catalog, List.of()),
                new DartSourceIntegrityScanner());
    }

    static FlutterDesignerDocumentState load(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            CatalogBuildResult catalogBuild) {
        return load(
                dartFile,
                modelFile,
                codec,
                validator,
                catalogBuild,
                new DartSourceIntegrityScanner());
    }

    static FlutterDesignerDocumentState load(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            CatalogBuildResult catalogBuild,
            DartSourceIntegrityScanner sourceScanner) {
        Objects.requireNonNull(dartFile, "dartFile");
        Objects.requireNonNull(modelFile, "modelFile");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(catalogBuild, "catalogBuild");
        Objects.requireNonNull(sourceScanner, "sourceScanner");

        int maximumBytes = codec.limits().maxDocumentBytes();
        try {
            byte[] bytes = readBounded(modelFile, maximumBytes);
            FdDecodeResult decoded = codec.decode(bytes);
            if (decoded instanceof FdDecodeResult.Current current) {
                ValidationResult validation = validator.validate(
                        current.document(), catalogBuild.catalog());
                List<FlutterDesignerDocumentState.ContextIssue> contextIssues =
                        sourceFileIssues(dartFile, current);
                Optional<DartSourceIntegrityResult> sourceIntegrity = Optional.empty();
                Optional<DartThreeWayIntegrityResult> threeWayIntegrity = Optional.empty();
                if (contextIssues.isEmpty()) {
                    int maximumSourceBytes = sourceScanner.limits().maxSourceBytes();
                    try {
                        byte[] dartBytes = readBounded(dartFile, maximumSourceBytes);
                        try {
                            sourceIntegrity = Optional.of(sourceScanner.scan(
                                    dartBytes, current.document().source()));
                        } catch (RuntimeException | LinkageError failure) {
                            LOGGER.log(Level.WARNING,
                                    "Unexpected Dart source-integrity failure for {0}",
                                    dartFile.getPath());
                            LOGGER.log(Level.FINE,
                                    "Flutter Designer source-integrity failure", failure);
                            return new FlutterDesignerDocumentState.Failure(
                                    "Verify paired Dart source",
                                    dartFile.getPath(),
                                    "The Dart source-integrity verifier failed unexpectedly.");
                        }
                    } catch (InputTooLargeException ex) {
                        sourceIntegrity = Optional.of(
                                DartSourceIntegrityResult.sourceTooLarge(
                                        ex.observedBytes(), maximumSourceBytes));
                    } catch (IOException ex) {
                        sourceIntegrity = Optional.of(
                                DartSourceIntegrityResult.readFailure(reason(ex)));
                    }
                    try {
                        DartGenerationResult generationResult =
                                new DartRegionGenerator().generate(
                                        current.document(), catalogBuild.catalog());
                        threeWayIntegrity = Optional.of(
                                new DartThreeWayIntegrityGate(sourceScanner).evaluate(
                                        sourceIntegrity.orElseThrow(),
                                        current.document().source(),
                                        generationResult));
                    } catch (RuntimeException | LinkageError failure) {
                        LOGGER.log(Level.WARNING,
                                "Unexpected Dart generation/integrity failure for {0}",
                                dartFile.getPath());
                        LOGGER.log(Level.FINE,
                                "Flutter Designer three-way integrity failure", failure);
                        return new FlutterDesignerDocumentState.Failure(
                                "Compare generated Dart source",
                                dartFile.getPath(),
                                "The deterministic Dart generator or three-way verifier "
                                + "failed unexpectedly.");
                    }
                }
                return new FlutterDesignerDocumentState.Current(
                        current,
                        validation,
                        catalogBuild.catalog(),
                        catalogBuild.diagnostics(),
                        contextIssues,
                        sourceIntegrity,
                        threeWayIntegrity);
            }
            if (decoded instanceof FdDecodeResult.UnsupportedNewer newer) {
                return new FlutterDesignerDocumentState.UnsupportedNewer(newer);
            }
            return new FlutterDesignerDocumentState.Invalid(
                    (FdDecodeResult.Invalid) decoded);
        } catch (InputTooLargeException ex) {
            return new FlutterDesignerDocumentState.InputTooLarge(
                    modelFile.getNameExt(), ex.observedBytes(), maximumBytes);
        } catch (FdInputLimitException ex) {
            return new FlutterDesignerDocumentState.InputTooLarge(
                    modelFile.getNameExt(), ex.actualBytes(), ex.maximumBytes());
        } catch (IOException ex) {
            return new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    modelFile.getPath(),
                    reason(ex));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Unexpected failure while decoding Flutter Designer model {0}",
                    modelFile.getPath());
            LOGGER.log(Level.FINE, "Flutter Designer decode failure", ex);
            return new FlutterDesignerDocumentState.Failure(
                    "Decode Flutter Designer model",
                    modelFile.getPath(),
                    "The model decoder failed unexpectedly.");
        }
    }

    private static List<FlutterDesignerDocumentState.ContextIssue> sourceFileIssues(
            FileObject dartFile,
            FdDecodeResult.Current current) {
        String declared = current.document().source().dartFile();
        String actual = dartFile.getNameExt();
        if (declared.equals(actual)) {
            return List.of();
        }
        return List.of(new FlutterDesignerDocumentState.ContextIssue(
                SOURCE_FILE_MISMATCH,
                "/source/dartFile",
                "The model names Dart source '" + declared
                + "', but the paired file is '" + actual + "'."));
    }

    private static byte[] readBounded(FileObject file, int maximumBytes)
            throws IOException, InputTooLargeException {
        long declaredSize = file.getSize();
        if (declaredSize > maximumBytes) {
            throw new InputTooLargeException(declaredSize);
        }

        int initialCapacity = declaredSize > 0
                ? (int) Math.min(declaredSize, maximumBytes)
                : Math.min(8_192, maximumBytes);
        try (InputStream input = file.getInputStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity)) {
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                long observed = (long) output.size() + read;
                if (observed > maximumBytes) {
                    throw new InputTooLargeException(observed);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private void reloadIfActive() {
        synchronized (this) {
            if (openViews == 0) {
                return;
            }
        }
        reload();
    }

    private void pairFileChanged(FileEvent event) {
        PairSaveCoordinator coordinator = pairSaveCoordinator;
        if (coordinator != null && coordinator.handleFileEvent(event)) {
            return;
        }
        reloadIfActive();
    }

    private void runLoads() {
        boolean scheduleFollowUp = false;
        try {
            while (true) {
                long ticket;
                synchronized (this) {
                    if (openViews == 0) {
                        reloadRequested = false;
                        return;
                    }
                    reloadRequested = false;
                    ticket = generation.get();
                }

                if (!isPublishable(ticket)) {
                    continue;
                }
                publish(ticket, new FlutterDesignerDocumentState.Loading(
                        modelFile.getNameExt()));
                if (!isPublishable(ticket)) {
                    continue;
                }
                FlutterDesignerDocumentState loaded;
                try {
                    loaded = loadNow();
                } catch (RuntimeException | LinkageError failure) {
                    LOGGER.log(Level.WARNING,
                            "Unexpected failure while loading Flutter Designer model {0}",
                            modelFile.getPath());
                    LOGGER.log(Level.FINE, "Flutter Designer load failure", failure);
                    loaded = new FlutterDesignerDocumentState.Failure(
                            "Load Flutter Designer model",
                            modelFile.getPath(),
                            "The background model load failed unexpectedly.");
                }
                publish(ticket, loaded);

                synchronized (this) {
                    if (openViews == 0) {
                        reloadRequested = false;
                        return;
                    }
                    // reloadRequested is the positive request to perform a
                    // follow-up load. A generation may also advance solely to
                    // invalidate this worker after exact Current adoption; in
                    // that case it must stop rather than overwrite the adopted
                    // state with an unsolicited load.
                    if (!reloadRequested) {
                        return;
                    }
                }
            }
        } finally {
            synchronized (this) {
                loadRunning = false;
                if (openViews == 0) {
                    reloadRequested = false;
                } else if (reloadRequested) {
                    loadRunning = true;
                    scheduleFollowUp = true;
                }
            }
            if (scheduleFollowUp) {
                WORKER.post(this::runLoads);
            }
        }
    }

    private void publish(long ticket, FlutterDesignerDocumentState next) {
        EventQueue.invokeLater(() -> {
            FlutterDesignerDocumentState previous;
            synchronized (FlutterDesignerDocumentController.this) {
                // The ticket decision and state swap are one monitor action.
                // An exact adoption can therefore invalidate a stale worker
                // between any earlier advisory check and this publication.
                if (!isPublishableLocked(ticket)) {
                    return;
                }
                previous = state;
                state = next;
            }
            changes.firePropertyChange(PROP_STATE, previous, next);
        });
    }

    private synchronized boolean isPublishable(long ticket) {
        return isPublishableLocked(ticket);
    }

    private boolean isPublishableLocked(long ticket) {
        return openViews > 0 && generation.get() == ticket;
    }

    /** Exact state/generation authority captured before durable persistence. */
    static final class CurrentAdoptionTicket {
        private final FlutterDesignerDocumentController owner;
        private final FlutterDesignerDocumentState.Current expectedCurrent;
        private final long generation;

        private CurrentAdoptionTicket(
                FlutterDesignerDocumentController owner,
                FlutterDesignerDocumentState.Current expectedCurrent,
                long generation) {
            this.owner = owner;
            this.expectedCurrent = expectedCurrent;
            this.generation = generation;
        }
    }

    /** One-shot property publication returned by callback-free adoption. */
    static final class DeferredCurrentEffects {
        private final FlutterDesignerDocumentController owner;
        private final FlutterDesignerDocumentState previous;
        private final FlutterDesignerDocumentState.Current current;
        private boolean published;

        private DeferredCurrentEffects(
                FlutterDesignerDocumentController owner,
                FlutterDesignerDocumentState previous,
                FlutterDesignerDocumentState.Current current) {
            this.owner = owner;
            this.previous = previous;
            this.current = current;
        }

        void publish() {
            synchronized (this) {
                if (published) {
                    return;
                }
                published = true;
            }
            Runnable publication = () -> owner.changes.firePropertyChange(
                    PROP_STATE, previous, current);
            if (EventQueue.isDispatchThread()) {
                publication.run();
            } else {
                EventQueue.invokeLater(publication);
            }
        }
    }

    private static String reason(IOException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.strip();
    }

    private static LoadOperation catalogLoadOperation(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            NetBeansWidgetCatalogProvider catalogProvider,
            DartSourceIntegrityScanner sourceScanner) {
        Objects.requireNonNull(dartFile, "dartFile");
        Objects.requireNonNull(modelFile, "modelFile");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(catalogProvider, "catalogProvider");
        Objects.requireNonNull(sourceScanner, "sourceScanner");
        return () -> load(
                dartFile,
                modelFile,
                codec,
                validator,
                catalogProvider.snapshotOffEdt(),
                sourceScanner);
    }

    private static LoadOperation fixedCatalogLoadOperation(
            FileObject dartFile,
            FileObject modelFile,
            FdDocumentCodec codec,
            WidgetTreeValidator validator,
            WidgetCatalog catalog,
            DartSourceIntegrityScanner sourceScanner) {
        Objects.requireNonNull(dartFile, "dartFile");
        Objects.requireNonNull(modelFile, "modelFile");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(sourceScanner, "sourceScanner");
        return () -> load(
                dartFile,
                modelFile,
                codec,
                validator,
                new CatalogBuildResult(catalog, List.of()),
                sourceScanner);
    }

    private static final class InputTooLargeException extends Exception {
        private final long observedBytes;

        InputTooLargeException(long observedBytes) {
            this.observedBytes = observedBytes;
        }

        long observedBytes() {
            return observedBytes;
        }
    }

    @FunctionalInterface
    interface LoadOperation {
        FlutterDesignerDocumentState load();
    }
}
