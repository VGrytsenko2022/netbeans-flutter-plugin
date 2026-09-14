package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.designer.canvas.CanvasAdmission;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasIntentAdmission;
import dev.flutter.netbeans.designer.canvas.CanvasIntentReplayGate;
import dev.flutter.netbeans.designer.canvas.CanvasIntentReplayPolicy;
import dev.flutter.netbeans.designer.canvas.CanvasImageResourceBundle;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasPresentationGate;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewProfileResolver;
import dev.flutter.netbeans.designer.canvas.CanvasRenderRequest;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasSurfaceMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.ValidatedCanvasRevisionSnapshot;
import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCapability;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCloseReason;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerProcessChannel;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsWebCanvasRuntime;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Exact browser-compiled Flutter Canvas session hosted by Windows WebView2.
 *
 * <p>The browser transport is different from the native runner process, but
 * model publication, identity, layout, replay, selection and mutation gates
 * remain the same bounded Canvas wire protocol. No event becomes authoritative
 * until the compiled SDK identity equals {@code runner.hello}, the latest
 * layout is admitted, and Flutter's physical presentation exactly equals the
 * independently observed WebView2 host bounds and DPI.</p>
 */
final class FlutterDesignerWebCanvasSession
        implements FlutterDesignerCanvasSession {
    private static final Duration CLOSE_ACK_TIMEOUT = Duration.ofSeconds(2);
    private static final int COMMAND_RETRY_DELAY_MILLIS = 50;
    private static final int MAX_COMMAND_RETRY_ATTEMPTS = 20;
    private static final int INTERACTION_ACK_TIMEOUT_MILLIS = 1_500;
    private static final WidgetTypeId TEXT_WIDGET_TYPE =
            new WidgetTypeId("flutter.widgets.Text");

    private final Runtime runtime;
    private final SdkResolver sdkResolver;
    private final Executor backgroundExecutor;
    private final Executor uiExecutor;
    private final Consumer<FlutterDesignerNativeCanvasStatus> statusListener;
    private final Consumer<StableId> selectionListener;
    private final Consumer<AdmittedPaletteDrop> paletteDropListener;
    private final Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
            deleteSelectionListener;
    private final CanvasModelPayloadCodec payloadCodec =
            new CanvasModelPayloadCodec();
    private final Timer interactionRetryTimer;
    private final Timer interactionAckTimer;
    private final Timer viewportRetryTimer;
    private final Timer widgetMoveRetryTimer;

    private Consumer<CanvasViewportMetrics> viewportMetricsListener = ignored -> {};
    private Runnable interactionListener = () -> {};
    private Consumer<CanvasRunnerRuntimeEvent.TextEditCommit>
            textEditCommitListener = ignored -> {};
    private Consumer<InteractionBarrierState> interactionBarrierListener =
            ignored -> {};
    private InteractionBarrierState interactionBarrierState =
            new InteractionBarrierState(
                    InteractionBarrierPhase.INACTIVE, 0, Optional.empty());

    private CanvasRunnerProcessChannel channel;
    private InputStream ownedBridgeStdout;
    private OutputStream ownedBridgeStdin;
    private CanvasSessionId sessionId;
    private CanvasEngineIdentity expectedEngineIdentity;
    private CanvasEngineIdentity admittedEngineIdentity;
    private CanvasPresentationGate presentationGate;
    private CanvasIntentReplayGate intentReplayGate;
    private StableId presentationDocumentId;
    private long nextPresentationSequence;
    private long presentationGeneration;
    private PendingPresentation pendingPresentation;
    private CanvasRenderRequest currentRequest;
    private CanvasLayoutKey currentLayout;
    private CanvasLayoutKey confirmedLayout;
    private CanvasSurfaceMetrics latestRunnerMetrics;
    private CanvasSurfaceMetrics hostSurfaceMetrics;
    private CanvasRevisionKey renderedRevision;
    private Set<StableId> currentWidgetIds = Set.of();
    private StableId desiredSelection;

    private CanvasViewportPresentation desiredViewport =
            CanvasViewportPresentation.fit();
    private CanvasRevisionKey lastViewportRevision;
    private CanvasViewportPresentation lastViewport;
    private long nextViewportSequence = 1;
    private long lastViewportSequence;
    private boolean viewportPending;
    private boolean viewportRetryPending;
    private int viewportRetryAttempts;

    private long interactionFenceSequence;
    private CanvasLayoutKey pendingInteractionLayout;
    private long pendingInteractionSequence = -1;
    private boolean interactionAccepting = true;
    private boolean interactionRetryPending;
    private int interactionRetryAttempts;

    private long nextMoveSequence = 1;
    private MoveCommand desiredMoveCommand;
    private boolean moveRetryPending;
    private int moveRetryAttempts;

    private boolean requestedVisible;
    private boolean focusRequested;
    private boolean runtimeStarted;
    private boolean terminalFailure;
    private boolean closing;
    private CompletionStage<Void> retirement;

    static FlutterDesignerWebCanvasSession createDefault(
            Consumer<FlutterDesignerNativeCanvasStatus> statusListener,
            Consumer<StableId> selectionListener,
            Consumer<AdmittedPaletteDrop> paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener) throws java.io.IOException {
        WindowsWebCanvasRuntime windows = WindowsWebCanvasRuntime.createDefault();
        FlutterToolchainService toolchains = new FlutterToolchainService();
        return new FlutterDesignerWebCanvasSession(
                new WindowsRuntime(windows),
                () -> {
                    var status = toolchains.resolve();
                    return new SdkResolution(
                            status.flutterSdk().orElse(null),
                            status.flutterMessage());
                },
                command -> Thread.ofVirtual()
                        .name("flutter-web-canvas-session")
                        .start(command),
                EventQueue::invokeLater,
                statusListener,
                selectionListener,
                paletteDropListener,
                deleteSelectionListener);
    }

    FlutterDesignerWebCanvasSession(
            Runtime runtime,
            SdkResolver sdkResolver,
            Executor backgroundExecutor,
            Executor uiExecutor,
            Consumer<FlutterDesignerNativeCanvasStatus> statusListener,
            Consumer<StableId> selectionListener,
            Consumer<AdmittedPaletteDrop> paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener) {
        requireEdt();
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.sdkResolver = Objects.requireNonNull(sdkResolver, "sdkResolver");
        this.backgroundExecutor = Objects.requireNonNull(
                backgroundExecutor, "backgroundExecutor");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.statusListener = Objects.requireNonNull(
                statusListener, "statusListener");
        this.selectionListener = Objects.requireNonNull(
                selectionListener, "selectionListener");
        this.paletteDropListener = Objects.requireNonNull(
                paletteDropListener, "paletteDropListener");
        this.deleteSelectionListener = Objects.requireNonNull(
                deleteSelectionListener, "deleteSelectionListener");
        interactionRetryTimer = singleShotTimer(
                this::retryInteractionFence);
        interactionAckTimer = singleShotTimer(
                this::interactionFenceTimedOut,
                INTERACTION_ACK_TIMEOUT_MILLIS);
        viewportRetryTimer = singleShotTimer(this::retryViewport);
        widgetMoveRetryTimer = singleShotTimer(this::retryWidgetMove);
        runtime.setSurfaceMetricsListener(this::surfaceMetricsChanged);
    }

    @Override
    public JComponent component() {
        requireEdt();
        return runtime.component();
    }

    @Override
    public boolean isSurfaceFocused() {
        requireEdt();
        return !closing && runtime.isControllerFocused();
    }

    @Override
    public boolean releaseSurfaceFocus() {
        requireEdt();
        return !closing && runtime.releaseControllerFocus();
    }

    @Override
    public void show() {
        requireEdt();
        if (closing) {
            return;
        }
        requestedVisible = true;
        runtime.setVisible(true);
        startIfNecessary();
        requestFocusIfPossible();
    }

    @Override
    public void hide() {
        requireEdt();
        requestedVisible = false;
        clearFocusRequest();
        runtime.setVisible(false);
        clearWidgetMovePreview();
    }

    @Override
    public void requestFocus() {
        requireEdt();
        if (closing) {
            return;
        }
        focusRequested = true;
        requestFocusIfPossible();
    }

    @Override
    public void clearFocusRequest() {
        requireEdt();
        focusRequested = false;
        advanceInteractionFence();
    }

    @Override
    public boolean restart() {
        requireEdt();
        return false;
    }

    @Override
    public boolean canRestart() {
        requireEdt();
        return false;
    }

    @Override
    public void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources) {
        present(document, catalog, previewMode, targetPlatform, resolvedTheme,
                imageResources, Optional.empty());
    }

    @Override
    public void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources,
            Optional<CanvasOrientation> orientationOverride) {
        requireEdt();
        if (closing) {
            return;
        }
        Objects.requireNonNull(orientationOverride, "orientationOverride");
        if (targetPlatform != CanvasTargetPlatform.WEB) {
            withdraw();
            publishFailure(
                    "Resolve exact Flutter Web Canvas target",
                    "the browser-compiled backend accepts only the Web target");
            return;
        }
        final ValidatedCanvasRevisionSnapshot snapshot;
        try {
            snapshot = ValidatedCanvasRevisionSnapshot.captureReadOnly(
                    0L,
                    Objects.requireNonNull(document, "document"),
                    Objects.requireNonNull(catalog, "catalog"),
                    ValidationLimits.defaults());
        } catch (IllegalArgumentException failure) {
            withdraw();
            publishFailure("Validate exact Flutter Web Canvas model",
                    failureReason(failure));
            return;
        }
        Set<StableId> ids = collectWidgetIds(snapshot.document().root());
        if (desiredSelection == null || !ids.contains(desiredSelection)) {
            desiredSelection = snapshot.document().root().id();
        }
        pendingPresentation = new PendingPresentation(
                ++presentationGeneration,
                snapshot,
                Objects.requireNonNull(previewMode, "previewMode"),
                targetPlatform,
                Objects.requireNonNull(resolvedTheme, "resolvedTheme"),
                Objects.requireNonNull(imageResources, "imageResources"),
                orientationOverride,
                ids);
        clearCurrentPresentation();
        beginInteractionBarrier(Optional.empty());
        startIfNecessary();
        publishPendingPresentation();
    }

    @Override
    public void withdraw() {
        requireEdt();
        presentationGeneration++;
        pendingPresentation = null;
        desiredSelection = null;
        clearPresentationAuthority();
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                "Flutter Web Canvas withdrawn.",
                "No validated Designer document is currently published to the "
                + "browser runtime."));
    }

    @Override
    public void selectWidget(StableId widgetId) {
        requireEdt();
        Objects.requireNonNull(widgetId, "widgetId");
        Set<StableId> ids = pendingPresentation == null
                ? currentWidgetIds : pendingPresentation.widgetIds();
        if (!ids.contains(widgetId)) {
            return;
        }
        desiredSelection = widgetId;
        sendSelectionIfPossible("Select widget in exact Flutter Web Canvas");
    }

    @Override
    public void setViewportMetricsListener(
            Consumer<CanvasViewportMetrics> listener) {
        requireEdt();
        viewportMetricsListener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void setInteractionListener(Runnable listener) {
        requireEdt();
        interactionListener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void setTextEditCommitListener(
            Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener) {
        requireEdt();
        textEditCommitListener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void setInteractionBarrierListener(
            Consumer<InteractionBarrierState> listener) {
        requireEdt();
        interactionBarrierListener = Objects.requireNonNull(listener, "listener");
        interactionBarrierListener.accept(interactionBarrierState);
    }

    @Override
    public InteractionBarrierState interactionBarrierState() {
        requireEdt();
        return interactionBarrierState;
    }

    @Override
    public void setViewportPresentation(
            CanvasViewportPresentation presentation) {
        requireEdt();
        desiredViewport = Objects.requireNonNull(presentation, "presentation");
        sendViewportIfPossible();
    }

    @Override
    public boolean paletteCatalogInsertDropAvailable() {
        requireEdt();
        // WebView2 does not yet expose a verified host-authoritative native
        // drag source. Tree/canvas moves remain protocol-projected below.
        return false;
    }

    @Override
    public boolean authorizePaletteDragSource(
            String token,
            WidgetTypeId widgetType) {
        requireEdt();
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(widgetType, "widgetType");
        return false;
    }

    @Override
    public void showWidgetMovePreview(
            StableId sourceWidgetId,
            WidgetPlacement destination) {
        requireEdt();
        Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
        Objects.requireNonNull(destination, "destination");
        CanvasLayoutKey layout = confirmedLayout;
        if (layout == null || !currentWidgetIds.contains(sourceWidgetId)) {
            return;
        }
        desiredMoveCommand = new MoveShow(layout, sourceWidgetId, destination);
        sendWidgetMoveIfPossible();
    }

    @Override
    public void clearWidgetMovePreview() {
        requireEdt();
        CanvasLayoutKey layout = confirmedLayout;
        if (layout == null) {
            desiredMoveCommand = null;
            widgetMoveRetryTimer.stop();
            return;
        }
        desiredMoveCommand = new MoveClear(layout);
        sendWidgetMoveIfPossible();
    }

    @Override
    public CompletionStage<Void> preparePeerRemovalAsync() {
        requireEdt();
        if (retirement != null) {
            if (!retirement.toCompletableFuture().isCompletedExceptionally()) {
                return retirement;
            }
            retirement = null;
        }
        closing = true;
        requestedVisible = false;
        focusRequested = false;
        stopTimers();
        clearPresentationAuthority();
        pendingPresentation = null;
        CanvasRunnerProcessChannel retiringChannel = channel;
        channel = null;
        CompletableFuture<Void> protocolClosed = new CompletableFuture<>();
        if (retiringChannel == null) {
            protocolClosed.complete(null);
        } else {
            try {
                retiringChannel.requestClose(CanvasWireCloseReason.FORM_CLOSED);
                backgroundExecutor.execute(() -> {
                    retiringChannel.awaitAuthenticatedClose(CLOSE_ACK_TIMEOUT);
                    retiringChannel.abort();
                    protocolClosed.complete(null);
                });
            } catch (RuntimeException | LinkageError failure) {
                retiringChannel.abort();
                protocolClosed.completeExceptionally(failure);
            }
        }
        CompletableFuture<Void> completion = new CompletableFuture<>();
        CompletionStage<Void> publishedRetirement =
                completion.minimalCompletionStage();
        retirement = publishedRetirement;
        completion.whenComplete((ignored, failure) -> {
            if (failure != null) {
                dispatchOnUi(() -> {
                    if (retirement == publishedRetirement) {
                        retirement = null;
                    }
                });
            }
        });
        protocolClosed.whenComplete((ignored, protocolFailure) -> {
            if (!dispatchOnUi(() -> {
                CompletionStage<Void> runtimeRetirement;
                try {
                    runtimeRetirement = Objects.requireNonNull(
                            runtime.preparePeerRemovalAsync(),
                            "Web Canvas runtime returned no retirement completion");
                } catch (RuntimeException | LinkageError failure) {
                    completion.completeExceptionally(combine(
                            protocolFailure, failure));
                    return;
                }
                runtimeRetirement.whenComplete((nothing, runtimeFailure) -> {
                    Throwable failure = combine(
                            protocolFailure, runtimeFailure);
                    if (failure == null) {
                        completion.complete(null);
                    } else {
                        completion.completeExceptionally(failure);
                    }
                });
            })) {
                completion.completeExceptionally(combine(
                        protocolFailure,
                        new IllegalStateException(
                                "Dispatch exact Flutter Web Canvas retirement "
                                + "on the EDT")));
            }
        });
        return publishedRetirement;
    }

    @Override
    public void close() {
        if (EventQueue.isDispatchThread()) {
            preparePeerRemovalAsync();
        } else {
            EventQueue.invokeLater(this::preparePeerRemovalAsync);
        }
    }

    private void startIfNecessary() {
        if (runtimeStarted || closing || terminalFailure || !requestedVisible) {
            return;
        }
        SdkResolution resolution;
        try {
            resolution = Objects.requireNonNull(
                    sdkResolver.resolve(), "SDK resolver result");
        } catch (RuntimeException | LinkageError failure) {
            failSession("Resolve Flutter SDK for exact Web Canvas",
                    failureReason(failure));
            return;
        }
        FlutterSdk sdk = resolution.sdk();
        if (sdk == null) {
            terminalFailure = true;
            publish(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Exact Flutter Web Canvas is unavailable.",
                    "A Flutter SDK is required to compile the isolated Web "
                    + "Canvas. " + resolution.message()));
            return;
        }
        runtimeStarted = true;
        runtime.start(sdk, runtimeListener());
    }

    private Runtime.Listener runtimeListener() {
        return new Runtime.Listener() {
            @Override
            public void preparing(String detail) {
                if (!acceptRuntimeCallback()) {
                    return;
                }
                publish(new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.PREPARING,
                        "Preparing exact Flutter Web Canvas...",
                        detail));
            }

            @Override
            public void nativeStarted(String runtimeVersion) {
                if (!acceptRuntimeCallback()) {
                    return;
                }
                publish(new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                        "Starting exact Flutter Web Canvas...",
                        "Microsoft Edge WebView2 " + runtimeVersion
                        + " loaded the isolated Canvas document."));
            }

            @Override
            public void bridgeReady(
                    CanvasEngineIdentity expectedIdentity,
                    InputStream stdout,
                    OutputStream stdin) {
                protocolBridgeReady(expectedIdentity, stdout, stdin);
            }

            @Override
            public void diagnostic(String message) {
                // Authenticated browser diagnostics are deliberately not user
                // authority. Terminal failures arrive through failed().
            }

            @Override
            public void bridgeTerminal(String reason) {
                if (acceptRuntimeCallback()) {
                    failSession("Run exact Flutter Web Canvas bridge", reason);
                }
            }

            @Override
            public void failed(String operation, String reason) {
                if (acceptRuntimeCallback()) {
                    failSession(operation, reason);
                }
            }
        };
    }

    private void protocolBridgeReady(
            CanvasEngineIdentity expectedIdentity,
            InputStream stdout,
            OutputStream stdin) {
        requireEdt();
        if (closing || terminalFailure || channel != null) {
            closeRejectedBridgeStreams(stdout, stdin);
            return;
        }
        if (expectedIdentity == null || stdout == null || stdin == null) {
            closeRejectedBridgeStreams(stdout, stdin);
            failSession("Start exact Flutter Web Canvas protocol",
                    "the authenticated bridge supplied incomplete protocol ownership");
            return;
        }
        expectedEngineIdentity = expectedIdentity;
        CanvasSessionId openedSession = CanvasSessionId.random();
        final CanvasRunnerProcessChannel created;
        try {
            created = CanvasRunnerProcessChannel.overStreams(
                    stdout,
                    stdin,
                    openedSession,
                    uiExecutor,
                    channelListener(openedSession));
        } catch (RuntimeException | LinkageError failure) {
            closeRejectedBridgeStreams(stdout, stdin);
            failSession("Start exact Flutter Web Canvas protocol",
                    failureReason(failure));
            return;
        }
        ownedBridgeStdout = stdout;
        ownedBridgeStdin = stdin;
        sessionId = openedSession;
        intentReplayGate = new CanvasIntentReplayGate(openedSession);
        channel = created;
        try {
            created.start();
        } catch (RuntimeException | LinkageError failure) {
            channel = null;
            created.abort();
            failSession("Start exact Flutter Web Canvas protocol",
                    failureReason(failure));
        }
    }

    private CanvasRunnerProcessChannel.Listener channelListener(
            CanvasSessionId openedSession) {
        return new CanvasRunnerProcessChannel.Listener() {
            @Override
            public void ready(CanvasEngineIdentity engineIdentity) {
                channelReady(openedSession, engineIdentity);
            }

            @Override
            public void presented(CanvasRunnerRuntimeEvent.Presented presented) {
                channelPresented(openedSession, presented);
            }

            @Override
            public void selection(
                    dev.flutter.netbeans.designer.canvas.CanvasIntentKey intentKey,
                    StableId widgetId) {
                channelSelection(openedSession, intentKey, widgetId);
            }

            @Override
            public void interaction(CanvasRunnerRuntimeEvent.Interaction event) {
                channelInteraction(openedSession, event);
            }

            @Override
            public void interactionFenceApplied(
                    CanvasRunnerRuntimeEvent.InteractionFenceApplied event) {
                channelFenceApplied(openedSession, event);
            }

            @Override
            public void paletteDrop(CanvasRunnerRuntimeEvent.PaletteDrop drop) {
                // The Web host deliberately advertises no trusted Palette
                // source; consume nothing from an unverified browser drag.
            }

            @Override
            public void deleteSelection(
                    CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
                channelDelete(openedSession, deletion);
            }

            @Override
            public void textEditCommit(
                    CanvasRunnerRuntimeEvent.TextEditCommit commit) {
                channelTextEdit(openedSession, commit);
            }

            @Override
            public void viewportMetrics(CanvasViewportMetrics metrics) {
                channelViewport(openedSession, metrics);
            }

            @Override
            public void failed(String reason) {
                if (currentSession(openedSession)) {
                    failSession("Run exact Flutter Web Canvas protocol", reason);
                }
            }
        };
    }

    private void channelReady(
            CanvasSessionId openedSession,
            CanvasEngineIdentity actualIdentity) {
        requireEdt();
        if (!currentSession(openedSession)) {
            return;
        }
        if (!Objects.equals(expectedEngineIdentity, actualIdentity)) {
            failSession(
                    "Authenticate exact Flutter Web Canvas engine",
                    "the browser-compiled Flutter engine identity differs from "
                    + "the authenticated runner.hello identity");
            return;
        }
        admittedEngineIdentity = actualIdentity;
        publishPendingPresentation();
    }

    private void publishPendingPresentation() {
        if (closing || terminalFailure) {
            return;
        }
        PendingPresentation pending = pendingPresentation;
        CanvasRunnerProcessChannel activeChannel = channel;
        if (pending == null || activeChannel == null
                || !activeChannel.isReady() || admittedEngineIdentity == null) {
            return;
        }
        StableId documentId = pending.snapshot().document().documentId();
        if (presentationGate == null
                || !documentId.equals(presentationDocumentId)) {
            if (presentationGate != null) {
                presentationGate.close();
            }
            presentationGate = new CanvasPresentationGate(
                    activeChannel.sessionId(),
                    documentId,
                    nextPresentationSequence);
            presentationDocumentId = documentId;
        }
        final CanvasRenderRequest request;
        try {
            request = presentationGate.present(
                    CanvasPreviewProfileResolver.resolve(
                            pending.previewMode(),
                            pending.targetPlatform(),
                            pending.snapshot().document().canvas(),
                            admittedEngineIdentity,
                            pending.resolvedTheme(),
                            pending.orientationOverride()),
                    pending.snapshot(),
                    pending.imageResources());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            publishFailure("Resolve exact Flutter Web Canvas presentation",
                    failureReason(failure));
            return;
        }
        nextPresentationSequence =
                request.revisionKey().presentationSequence() + 1;
        currentRequest = request;
        currentLayout = null;
        confirmedLayout = null;
        latestRunnerMetrics = null;
        renderedRevision = null;
        currentWidgetIds = pending.widgetIds();
        resetViewportFence();
        beginInteractionBarrier(Optional.empty());
        try {
            activeChannel.expectPresentation(request.revisionKey());
        } catch (RuntimeException failure) {
            publishFailure("Fence exact Flutter Web Canvas presentation",
                    failureReason(failure));
            return;
        }
        long publication = pending.publicationGeneration();
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                "Rendering exact Flutter Web Canvas...",
                "Publishing validated .fd revision "
                + request.revisionKey().logicalRevisionId() + " as a "
                + (int) request.renderProfile().viewport().logicalWidth()
                + "×"
                + (int) request.renderProfile().viewport().logicalHeight()
                + " Web viewport."));
        try {
            backgroundExecutor.execute(() -> encodePresentation(
                    publication, activeChannel, request));
        } catch (RuntimeException | LinkageError failure) {
            publishFailure("Schedule exact Flutter Web Canvas model encoding",
                    failureReason(failure));
        }
    }

    private void encodePresentation(
            long publication,
            CanvasRunnerProcessChannel activeChannel,
            CanvasRenderRequest request) {
        byte[] payload;
        try {
            payload = payloadCodec.encode(request);
        } catch (Exception failure) {
            deliverOnUi(() -> publicationFailed(
                    publication,
                    activeChannel,
                    "Encode exact Flutter Web Canvas model",
                    failureReason(failure)));
            return;
        }
        deliverOnUi(() -> {
            if (!currentPublication(publication, activeChannel, request)) {
                return;
            }
            try {
                if (!activeChannel.present(request, payload)) {
                    publicationFailed(publication, activeChannel,
                            "Send exact Flutter Web Canvas model",
                            "the bounded protocol queue rejected the current publication");
                } else {
                    sendViewportIfPossible();
                }
            } catch (RuntimeException failure) {
                publicationFailed(publication, activeChannel,
                        "Queue exact Flutter Web Canvas model",
                        failureReason(failure));
            }
        });
    }

    private void publicationFailed(
            long publication,
            CanvasRunnerProcessChannel activeChannel,
            String operation,
            String reason) {
        if (pendingPresentation != null
                && pendingPresentation.publicationGeneration() == publication
                && channel == activeChannel) {
            publishFailure(operation, reason);
        }
    }

    private void channelPresented(
            CanvasSessionId openedSession,
            CanvasRunnerRuntimeEvent.Presented presented) {
        requireEdt();
        if (!currentSession(openedSession) || presentationGate == null) {
            return;
        }
        CanvasLayoutKey layout = presented.layoutKey();
        CanvasAdmission admission = currentLayout == null
                ? presentationGate.admitPresentation(layout.frameKey(), layout)
                : presentationGate.admitLayout(layout);
        if (admission != CanvasAdmission.ACCEPTED) {
            return;
        }
        currentLayout = layout;
        latestRunnerMetrics = presented.metrics();
        confirmedLayout = null;
        beginInteractionBarrier(Optional.of(layout));
        confirmPhysicalPresentationIfExact();
    }

    private void surfaceMetricsChanged(CanvasSurfaceMetrics metrics) {
        requireEdt();
        if (!acceptRuntimeCallback()) {
            return;
        }
        hostSurfaceMetrics = Objects.requireNonNull(metrics, "metrics");
        if (confirmedLayout != null
                && !metrics.equals(latestRunnerMetrics)) {
            confirmedLayout = null;
            beginInteractionBarrier(Optional.ofNullable(currentLayout));
        }
        confirmPhysicalPresentationIfExact();
    }

    private void confirmPhysicalPresentationIfExact() {
        CanvasLayoutKey layout = currentLayout;
        if (layout == null
                || latestRunnerMetrics == null
                || hostSurfaceMetrics == null
                || !latestRunnerMetrics.equals(hostSurfaceMetrics)) {
            return;
        }
        confirmedLayout = layout;
        interactionRetryAttempts = 0;
        sendInteractionFenceIfPossible();
        sendSelectionIfPossible(
                "Restore selection in exact Flutter Web Canvas");
        sendViewportIfPossible();
        sendWidgetMoveIfPossible();
        CanvasRenderRequest request = currentRequest;
        if (request != null
                && !request.revisionKey().equals(renderedRevision)) {
            renderedRevision = request.revisionKey();
            publish(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    "Exact Flutter Web Canvas rendered.",
                    "Presented "
                    + request.renderProfile().previewMode().name().toLowerCase(
                            java.util.Locale.ROOT)
                    + " / web viewport "
                    + (int) request.renderProfile().viewport().logicalWidth()
                    + "×"
                    + (int) request.renderProfile().viewport().logicalHeight()
                    + " with exact browser surface identity and read-only "
                    + "selection enabled.",
                    true));
        }
    }

    private void sendSelectionIfPossible(String operation) {
        CanvasRunnerProcessChannel active = channel;
        CanvasLayoutKey layout = confirmedLayout;
        StableId selection = desiredSelection;
        if (active == null || layout == null || selection == null
                || !currentWidgetIds.contains(selection)) {
            return;
        }
        try {
            active.select(layout, selection);
        } catch (RuntimeException failure) {
            failSession(operation, failureReason(failure));
        }
    }

    private void advanceInteractionFence() {
        if (interactionFenceSequence >= CanvasWireProtocol.MAX_SEQUENCE) {
            interactionAccepting = false;
            publishBarrier(new InteractionBarrierState(
                    InteractionBarrierPhase.TIMED_OUT,
                    interactionFenceSequence,
                    Optional.ofNullable(currentLayout)));
            return;
        }
        interactionFenceSequence++;
        beginInteractionBarrier(Optional.ofNullable(currentLayout));
        sendInteractionFenceIfPossible();
    }

    private void beginInteractionBarrier(Optional<CanvasLayoutKey> layout) {
        interactionAckTimer.stop();
        pendingInteractionLayout = null;
        pendingInteractionSequence = -1;
        interactionAccepting = true;
        publishBarrier(new InteractionBarrierState(
                InteractionBarrierPhase.SYNCHRONIZING,
                interactionFenceSequence,
                layout));
    }

    private void sendInteractionFenceIfPossible() {
        CanvasRunnerProcessChannel active = channel;
        CanvasLayoutKey layout = confirmedLayout;
        if (!interactionAccepting || active == null || layout == null) {
            return;
        }
        try {
            if (active.interactionFence(layout, interactionFenceSequence)) {
                pendingInteractionLayout = layout;
                pendingInteractionSequence = interactionFenceSequence;
                interactionRetryPending = false;
                interactionRetryAttempts = 0;
                interactionRetryTimer.stop();
                interactionAckTimer.restart();
            } else {
                scheduleInteractionRetry();
            }
        } catch (RuntimeException failure) {
            failSession("Fence exact Flutter Web Canvas interaction",
                    failureReason(failure));
        }
    }

    private void scheduleInteractionRetry() {
        if (++interactionRetryAttempts >= MAX_COMMAND_RETRY_ATTEMPTS) {
            interactionAccepting = false;
            publishBarrier(new InteractionBarrierState(
                    InteractionBarrierPhase.TIMED_OUT,
                    interactionFenceSequence,
                    Optional.ofNullable(currentLayout)));
            return;
        }
        interactionRetryPending = true;
        interactionRetryTimer.restart();
    }

    private void retryInteractionFence() {
        if (interactionRetryPending) {
            interactionRetryPending = false;
            sendInteractionFenceIfPossible();
        }
    }

    private void interactionFenceTimedOut() {
        if (interactionBarrierState.phase()
                == InteractionBarrierPhase.SYNCHRONIZING) {
            interactionAccepting = false;
            publishBarrier(new InteractionBarrierState(
                    InteractionBarrierPhase.TIMED_OUT,
                    interactionFenceSequence,
                    Optional.ofNullable(currentLayout)));
        }
    }

    private void channelFenceApplied(
            CanvasSessionId openedSession,
            CanvasRunnerRuntimeEvent.InteractionFenceApplied applied) {
        CanvasLayoutKey layout = confirmedLayout;
        if (!currentSession(openedSession)
                || layout == null
                || !layout.equals(applied.layoutKey())
                || !layout.equals(pendingInteractionLayout)
                || applied.interactionFenceSequence()
                        != pendingInteractionSequence
                || pendingInteractionSequence != interactionFenceSequence
                || presentationGate == null
                || presentationGate.admitSelection(layout)
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        interactionAckTimer.stop();
        interactionAccepting = true;
        publishBarrier(new InteractionBarrierState(
                InteractionBarrierPhase.SYNCHRONIZED,
                interactionFenceSequence,
                Optional.of(layout)));
    }

    private void channelSelection(
            CanvasSessionId openedSession,
            dev.flutter.netbeans.designer.canvas.CanvasIntentKey intentKey,
            StableId widgetId) {
        if (!currentSession(openedSession) || intentReplayGate == null) {
            return;
        }
        CanvasIntentAdmission replay = intentReplayGate.consume(
                intentKey, CanvasIntentReplayPolicy.IDEMPOTENT);
        if (!replay.firstDelivery()
                || !inputEnabledFor(intentKey.layoutKey())
                || !currentWidgetIds.contains(widgetId)) {
            return;
        }
        desiredSelection = widgetId;
        selectionListener.accept(widgetId);
    }

    private void channelInteraction(
            CanvasSessionId openedSession,
            CanvasRunnerRuntimeEvent.Interaction interaction) {
        if (!currentSession(openedSession)
                || intentReplayGate == null
                || confirmedLayout == null) {
            return;
        }
        CanvasIntentAdmission replay = intentReplayGate.admit(
                confirmedLayout,
                interaction.intentKey(),
                CanvasIntentReplayPolicy.ONE_SHOT);
        if (replay.firstDelivery()
                && interaction.interactionFenceSequence()
                        == interactionFenceSequence
                && inputEnabledFor(interaction.intentKey().layoutKey())
                && requestedVisible) {
            interactionListener.run();
        }
    }

    private void channelDelete(
            CanvasSessionId openedSession,
            CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
        if (!currentSession(openedSession)
                || intentReplayGate == null
                || confirmedLayout == null
                || currentRequest == null) {
            return;
        }
        CanvasIntentAdmission replay = intentReplayGate.consume(
                deletion.intentKey(), CanvasIntentReplayPolicy.ONE_SHOT);
        if (replay.firstDelivery()
                && requestedVisible
                && inputEnabledFor(deletion.intentKey().layoutKey())
                && currentWidgetIds.contains(deletion.widgetId())
                && !deletion.widgetId().equals(currentRequest.snapshot()
                        .document().root().id())
                && Objects.equals(desiredSelection, deletion.widgetId())) {
            deleteSelectionListener.accept(deletion);
        }
    }

    private void channelTextEdit(
            CanvasSessionId openedSession,
            CanvasRunnerRuntimeEvent.TextEditCommit commit) {
        if (!currentSession(openedSession)
                || intentReplayGate == null
                || currentRequest == null
                || confirmedLayout == null) {
            return;
        }
        CanvasIntentAdmission replay = intentReplayGate.consume(
                commit.intentKey(), CanvasIntentReplayPolicy.ONE_SHOT);
        WidgetNode widget = findWidget(
                currentRequest.snapshot().document().root(), commit.widgetId());
        if (replay.firstDelivery()
                && commit.interactionFenceSequence()
                        == interactionFenceSequence
                && requestedVisible
                && inputEnabledFor(commit.intentKey().layoutKey())
                && widget != null
                && widget.type().equals(TEXT_WIDGET_TYPE)
                && Objects.equals(desiredSelection, commit.widgetId())) {
            textEditCommitListener.accept(commit);
        }
    }

    private void sendViewportIfPossible() {
        CanvasRunnerProcessChannel active = channel;
        CanvasRenderRequest request = currentRequest;
        if (active == null || request == null || confirmedLayout == null
                || !active.supports(
                        CanvasWireCapability.VIEWPORT_PRESENTATION_V1)) {
            return;
        }
        CanvasRevisionKey revision = request.revisionKey();
        if (revision.equals(lastViewportRevision)
                && desiredViewport.equals(lastViewport)
                && !viewportRetryPending) {
            return;
        }
        if (nextViewportSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            failSession("Change exact Flutter Web Canvas zoom and scroll",
                    "the viewport command sequence is exhausted");
            return;
        }
        long sequence = nextViewportSequence;
        try {
            if (active.viewport(revision, sequence, desiredViewport)) {
                lastViewportRevision = revision;
                lastViewport = desiredViewport;
                lastViewportSequence = sequence;
                nextViewportSequence++;
                viewportPending = true;
                viewportRetryPending = false;
                viewportRetryAttempts = 0;
                viewportRetryTimer.stop();
            } else {
                scheduleViewportRetry();
            }
        } catch (RuntimeException failure) {
            failSession("Change exact Flutter Web Canvas zoom and scroll",
                    failureReason(failure));
        }
    }

    private void scheduleViewportRetry() {
        if (++viewportRetryAttempts >= MAX_COMMAND_RETRY_ATTEMPTS) {
            viewportRetryPending = false;
            failSession("Queue exact Flutter Web Canvas zoom and scroll",
                    "the bounded protocol queue rejected the latest viewport command");
            return;
        }
        viewportRetryPending = true;
        viewportRetryTimer.restart();
    }

    private void retryViewport() {
        if (viewportRetryPending) {
            viewportRetryPending = false;
            sendViewportIfPossible();
        }
    }

    private void channelViewport(
            CanvasSessionId openedSession,
            CanvasViewportMetrics metrics) {
        if (!currentSession(openedSession)
                || currentRequest == null
                || !metrics.revisionKey().equals(
                        currentRequest.revisionKey())) {
            return;
        }
        if (metrics.commandSequence() < lastViewportSequence) {
            return;
        }
        if (metrics.commandSequence() > lastViewportSequence
                || (viewportPending
                && !metrics.presentation().equals(lastViewport))) {
            failSession("Confirm exact Flutter Web Canvas zoom and scroll",
                    "the runner acknowledged a viewport command that the host "
                    + "did not issue exactly");
            return;
        }
        if (confirmedLayout == null || viewportRetryPending) {
            return;
        }
        viewportPending = false;
        desiredViewport = metrics.presentation();
        viewportMetricsListener.accept(metrics);
    }

    private void sendWidgetMoveIfPossible() {
        MoveCommand command = desiredMoveCommand;
        CanvasRunnerProcessChannel active = channel;
        if (command == null || active == null || confirmedLayout == null
                || !active.supports(CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                || !command.layout().equals(confirmedLayout)) {
            return;
        }
        if (nextMoveSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            desiredMoveCommand = null;
            return;
        }
        long sequence = nextMoveSequence;
        boolean sent;
        try {
            sent = switch (command) {
                case MoveShow show -> active.previewWidgetMove(
                        show.layout(),
                        sequence,
                        show.sourceWidgetId(),
                        show.destination().parentId(),
                        show.destination().slotName(),
                        show.destination().index());
                case MoveClear clear -> active.clearWidgetMovePreview(
                        clear.layout(), sequence);
            };
        } catch (RuntimeException failure) {
            failSession("Project exact Flutter Web Canvas widget move",
                    failureReason(failure));
            return;
        }
        if (sent) {
            nextMoveSequence++;
            desiredMoveCommand = null;
            moveRetryPending = false;
            moveRetryAttempts = 0;
            widgetMoveRetryTimer.stop();
        } else if (++moveRetryAttempts >= MAX_COMMAND_RETRY_ATTEMPTS) {
            desiredMoveCommand = null;
            moveRetryPending = false;
        } else {
            moveRetryPending = true;
            widgetMoveRetryTimer.restart();
        }
    }

    private void retryWidgetMove() {
        if (moveRetryPending) {
            moveRetryPending = false;
            sendWidgetMoveIfPossible();
        }
    }

    private boolean inputEnabledFor(CanvasLayoutKey layout) {
        return interactionAccepting
                && interactionBarrierState.inputEnabled()
                && confirmedLayout != null
                && confirmedLayout.equals(layout)
                && presentationGate != null
                && presentationGate.admitSelection(layout)
                        == CanvasAdmission.ACCEPTED;
    }

    private void requestFocusIfPossible() {
        if (focusRequested && requestedVisible && runtime.running()) {
            runtime.requestControllerFocus();
        }
    }

    private void resetViewportFence() {
        viewportRetryTimer.stop();
        lastViewportRevision = null;
        lastViewport = null;
        nextViewportSequence = 1;
        lastViewportSequence = 0;
        viewportPending = false;
        viewportRetryPending = false;
        viewportRetryAttempts = 0;
    }

    private void clearCurrentPresentation() {
        currentRequest = null;
        currentLayout = null;
        confirmedLayout = null;
        latestRunnerMetrics = null;
        renderedRevision = null;
        currentWidgetIds = Set.of();
        desiredMoveCommand = null;
        resetViewportFence();
    }

    private void clearPresentationAuthority() {
        clearCurrentPresentation();
        interactionRetryTimer.stop();
        interactionAckTimer.stop();
        interactionAccepting = false;
        pendingInteractionLayout = null;
        pendingInteractionSequence = -1;
        publishBarrier(new InteractionBarrierState(
                InteractionBarrierPhase.INACTIVE,
                interactionFenceSequence,
                Optional.empty()));
        presentationDocumentId = null;
        if (presentationGate != null) {
            presentationGate.close();
            presentationGate = null;
        }
        if (intentReplayGate != null) {
            intentReplayGate.close();
            intentReplayGate = null;
        }
    }

    private void stopTimers() {
        interactionRetryTimer.stop();
        interactionAckTimer.stop();
        viewportRetryTimer.stop();
        widgetMoveRetryTimer.stop();
    }

    private void failSession(String operation, String reason) {
        if (closing || terminalFailure) {
            return;
        }
        terminalFailure = true;
        pendingPresentation = null;
        stopTimers();
        CanvasRunnerProcessChannel failedChannel = channel;
        channel = null;
        if (failedChannel != null) {
            failedChannel.abort();
        }
        clearPresentationAuthority();
        publishFailure(operation, reason);
    }

    private void publishFailure(String operation, String reason) {
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                "Exact Flutter Web Canvas failed.",
                "Operation: " + requireText(operation, "operation")
                + ". Target: browser-compiled Flutter Web Canvas. Reason: "
                + requireText(reason, "reason") + "."));
    }

    private void publishBarrier(InteractionBarrierState state) {
        if (!state.equals(interactionBarrierState)) {
            interactionBarrierState = state;
            interactionBarrierListener.accept(state);
        }
    }

    private void publish(FlutterDesignerNativeCanvasStatus status) {
        if (!closing) {
            statusListener.accept(status);
        }
    }

    private boolean currentSession(CanvasSessionId candidate) {
        return !closing && !terminalFailure
                && channel != null && sessionId != null
                && sessionId.equals(candidate)
                && channel.sessionId().equals(candidate);
    }

    private boolean currentPublication(
            long publication,
            CanvasRunnerProcessChannel active,
            CanvasRenderRequest request) {
        return !closing && !terminalFailure
                && channel == active
                && currentRequest == request
                && pendingPresentation != null
                && pendingPresentation.publicationGeneration() == publication;
    }

    private boolean acceptRuntimeCallback() {
        requireEdt();
        return !closing && !terminalFailure;
    }

    private void closeRejectedBridgeStreams(
            InputStream stdout,
            OutputStream stdin) {
        if (stdout != ownedBridgeStdout) {
            closeQuietly(stdout);
        }
        if (stdin != ownedBridgeStdin) {
            closeQuietly(stdin);
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // A rejected bridge has no session owner left to report cleanup.
        }
    }

    private static void closeQuietly(OutputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // A rejected bridge has no session owner left to report cleanup.
        }
    }

    private boolean dispatchOnUi(Runnable task) {
        try {
            uiExecutor.execute(task);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private void deliverOnUi(Runnable task) {
        dispatchOnUi(task);
    }

    private static WidgetNode findWidget(WidgetNode root, StableId id) {
        if (root.id().equals(id)) {
            return root;
        }
        for (WidgetSlot slot : root.slots().values()) {
            List<WidgetNode> children = switch (slot) {
                case WidgetSlot.SingleSlot single ->
                    single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            };
            for (WidgetNode child : children) {
                WidgetNode found = findWidget(child, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Set<StableId> collectWidgetIds(WidgetNode root) {
        java.util.HashSet<StableId> ids = new java.util.HashSet<>();
        collectWidgetIds(root, ids);
        return Set.copyOf(ids);
    }

    private static void collectWidgetIds(WidgetNode node, Set<StableId> ids) {
        ids.add(node.id());
        for (WidgetSlot slot : node.slots().values()) {
            switch (slot) {
                case WidgetSlot.SingleSlot single ->
                    single.child().ifPresent(child -> collectWidgetIds(child, ids));
                case WidgetSlot.ListSlot list ->
                    list.children().forEach(child -> collectWidgetIds(child, ids));
            }
        }
    }

    private static Timer singleShotTimer(Runnable command) {
        return singleShotTimer(command, COMMAND_RETRY_DELAY_MILLIS);
    }

    private static Timer singleShotTimer(Runnable command, int delay) {
        Timer timer = new Timer(delay, event -> command.run());
        timer.setRepeats(false);
        return timer;
    }

    private static Throwable combine(Throwable first, Throwable second) {
        Throwable left = unwrap(first);
        Throwable right = unwrap(second);
        if (left == null) {
            return right;
        }
        if (right != null && right != left) {
            left.addSuppressed(right);
        }
        return left;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String failureReason(Throwable failure) {
        Throwable current = unwrap(Objects.requireNonNull(failure, "failure"));
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private static String requireText(String value, String label) {
        String accepted = Objects.requireNonNull(value, label).trim();
        if (accepted.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return accepted;
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Flutter Web Canvas session operations must run on the EDT");
        }
    }

    record SdkResolution(FlutterSdk sdk, String message) {
        SdkResolution {
            message = Objects.requireNonNullElse(
                    message, "Flutter SDK is unavailable.");
            if (message.isBlank()) {
                throw new IllegalArgumentException(
                        "SDK resolution message cannot be blank");
            }
        }
    }

    @FunctionalInterface
    interface SdkResolver {
        SdkResolution resolve();
    }

    interface Runtime {
        JComponent component();

        void start(FlutterSdk sdk, Listener listener);

        void setVisible(boolean visible);

        void requestControllerFocus();

        boolean isControllerFocused();

        boolean releaseControllerFocus();

        boolean running();

        void setSurfaceMetricsListener(Consumer<CanvasSurfaceMetrics> listener);

        CompletionStage<Void> preparePeerRemovalAsync();

        interface Listener {
            default void preparing(String detail) {}

            default void nativeStarted(String runtimeVersion) {}

            default void bridgeReady(
                    CanvasEngineIdentity expectedIdentity,
                    InputStream stdout,
                    OutputStream stdin) {}

            default void diagnostic(String message) {}

            default void bridgeTerminal(String reason) {}

            default void failed(String operation, String reason) {}
        }
    }

    private static final class WindowsRuntime implements Runtime {
        private final WindowsWebCanvasRuntime delegate;

        private WindowsRuntime(WindowsWebCanvasRuntime delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public JComponent component() {
            return delegate.component();
        }

        @Override
        public void start(FlutterSdk sdk, Listener listener) {
            delegate.start(sdk, new WindowsWebCanvasRuntime.Listener() {
                @Override
                public void preparing(String detail) {
                    listener.preparing(detail);
                }

                @Override
                public void nativeStarted(String runtimeVersion) {
                    listener.nativeStarted(runtimeVersion);
                }

                @Override
                public void bridgeReady(
                        CanvasEngineIdentity expectedIdentity,
                        InputStream stdout,
                        OutputStream stdin) {
                    listener.bridgeReady(expectedIdentity, stdout, stdin);
                }

                @Override
                public void diagnostic(String message) {
                    listener.diagnostic(message);
                }

                @Override
                public void bridgeTerminal(String reason) {
                    listener.bridgeTerminal(reason);
                }

                @Override
                public void failed(String operation, String reason) {
                    listener.failed(operation, reason);
                }
            });
        }

        @Override
        public void setVisible(boolean visible) {
            delegate.setVisible(visible);
        }

        @Override
        public void requestControllerFocus() {
            delegate.requestControllerFocus();
        }

        @Override
        public boolean isControllerFocused() {
            return delegate.isControllerFocused();
        }

        @Override
        public boolean releaseControllerFocus() {
            return delegate.releaseControllerFocus();
        }

        @Override
        public boolean running() {
            return delegate.running();
        }

        @Override
        public void setSurfaceMetricsListener(
                Consumer<CanvasSurfaceMetrics> listener) {
            delegate.setSurfaceMetricsListener(listener);
        }

        @Override
        public CompletionStage<Void> preparePeerRemovalAsync() {
            return delegate.preparePeerRemovalAsync();
        }
    }

    private record PendingPresentation(
            long publicationGeneration,
            ValidatedCanvasRevisionSnapshot snapshot,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform,
            CanvasResolvedTheme resolvedTheme,
            CanvasImageResourceBundle imageResources,
            Optional<CanvasOrientation> orientationOverride,
            Set<StableId> widgetIds) {
        private PendingPresentation {
            if (publicationGeneration <= 0) {
                throw new IllegalArgumentException(
                        "publicationGeneration must be positive");
            }
            Objects.requireNonNull(snapshot, "snapshot");
            Objects.requireNonNull(previewMode, "previewMode");
            Objects.requireNonNull(targetPlatform, "targetPlatform");
            Objects.requireNonNull(resolvedTheme, "resolvedTheme");
            Objects.requireNonNull(imageResources, "imageResources");
            Objects.requireNonNull(orientationOverride, "orientationOverride");
            widgetIds = Set.copyOf(widgetIds);
        }
    }

    private sealed interface MoveCommand permits MoveShow, MoveClear {
        CanvasLayoutKey layout();
    }

    private record MoveShow(
            CanvasLayoutKey layout,
            StableId sourceWidgetId,
            WidgetPlacement destination) implements MoveCommand {
        private MoveShow {
            Objects.requireNonNull(layout, "layout");
            Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
            Objects.requireNonNull(destination, "destination");
        }
    }

    private record MoveClear(CanvasLayoutKey layout) implements MoveCommand {
        private MoveClear {
            Objects.requireNonNull(layout, "layout");
        }
    }
}
