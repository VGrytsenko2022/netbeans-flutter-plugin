package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.designer.canvas.CanvasAdmission;
import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentAdmission;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentReplayGate;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentReplayPolicy;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResourceBundle;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasPresentationGate;
import io.github.vgrytsenko2022.designer.canvas.CanvasPreviewMode;
import io.github.vgrytsenko2022.designer.canvas.CanvasPreviewProfileResolver;
import io.github.vgrytsenko2022.designer.canvas.CanvasRenderRequest;
import io.github.vgrytsenko2022.designer.canvas.CanvasResolvedTheme;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.canvas.CanvasSurfaceMetrics;
import io.github.vgrytsenko2022.designer.canvas.CanvasTargetPlatform;
import io.github.vgrytsenko2022.designer.canvas.CanvasThemeBrightness;
import io.github.vgrytsenko2022.designer.canvas.ValidatedCanvasRevisionSnapshot;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportMetrics;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportPresentation;
import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCapability;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCloseReason;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireProtocol;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCapabilityCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCapability;
import io.github.vgrytsenko2022.designer.command.WidgetPlacement;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.CanvasOrientation;
import io.github.vgrytsenko2022.designer.model.DesignerThemeMode;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerBuildResult;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerBuildService;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerProcessChannel;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeLease;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerLaunch;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Timer;

/** Per-MultiView lifecycle for one provider-owned native Flutter surface. */
final class FlutterDesignerNativeCanvasSession
        implements FlutterDesignerCanvasSession {

    private static final Duration ATTACH_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CLOSE_ACK_BEFORE_PEER_TEARDOWN_TIMEOUT =
            Duration.ofMillis(500);
    private static final Duration CLOSE_HANDSHAKE_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CLOSE_ACK_DRAIN_TIMEOUT = Duration.ofMillis(500);
    private static final Duration TERMINATE_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_RUNTIME_DIAGNOSTIC_CHARS = 64 * 1024;
    private static final int VIEWPORT_RETRY_DELAY_MILLIS = 50;
    private static final int MAX_VIEWPORT_RETRY_ATTEMPTS = 20;
    private static final int WIDGET_MOVE_PREVIEW_RETRY_DELAY_MILLIS = 50;
    private static final int MAX_WIDGET_MOVE_PREVIEW_RETRY_ATTEMPTS = 20;
    private static final int FOCUS_RETRY_DELAY_MILLIS = 50;
    static final int MAX_FOCUS_RETRY_ATTEMPTS = 8;
    private static final int INTERACTION_FENCE_RETRY_DELAY_MILLIS = 50;
    private static final int MAX_INTERACTION_FENCE_RETRY_ATTEMPTS = 20;
    private static final int INTERACTION_FENCE_ACK_TIMEOUT_MILLIS = 1_500;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final WidgetTypeId TEXT_WIDGET_TYPE =
            new WidgetTypeId("flutter.widgets.Text");
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerNativeCanvasSession.class.getName());

    private final NativeCanvasHost host;
    private final NativeCanvasPlatformProvider platformProvider;
    private final NativeCanvasRunnerContract runnerContract;
    private final RuntimeServices runtime;
    private final Consumer<FlutterDesignerNativeCanvasStatus> listener;
    private final Consumer<StableId> selectionListener;
    private final Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver;
    private final Consumer<AdmittedPaletteDrop> paletteDropListener;
    private final Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
            deleteSelectionListener;
    private final Set<Process> retiringProcesses = ConcurrentHashMap.newKeySet();
    private final Set<Process> retirementTerminations =
            ConcurrentHashMap.newKeySet();
    private final Map<Process, Cancellable> gracefulRetirementPolls =
            new ConcurrentHashMap<>();
    private final Set<Long> inFlightLaunches = ConcurrentHashMap.newKeySet();
    private Consumer<CanvasViewportMetrics> viewportMetricsListener = ignored -> { };
    private Runnable interactionListener = () -> { };
    private Consumer<CanvasRunnerRuntimeEvent.TextEditCommit>
            textEditCommitListener = ignored -> { };
    private Consumer<InteractionBarrierState> interactionBarrierListener =
            ignored -> { };
    private InteractionBarrierState interactionBarrierState =
            new InteractionBarrierState(
                    InteractionBarrierPhase.INACTIVE,
                    0,
                    Optional.empty());
    private long interactionFenceSequence;
    private boolean interactionFenceAccepting = true;
    private CanvasLayoutKey lastInteractionFenceLayout;
    private long lastInteractionFenceSequence = -1;
    private boolean interactionFenceRetryPending;
    private int interactionFenceRetryAttempts;
    private CanvasLayoutKey pendingInteractionFenceLayout;
    private long pendingInteractionFenceSequence = -1;
    private CanvasLayoutKey appliedInteractionFenceLayout;
    private long appliedInteractionFenceSequence = -1;
    private final CanvasModelPayloadCodec payloadCodec = new CanvasModelPayloadCodec();
    private CanvasRunnerBuildResult prepared;
    private CompletableFuture<CanvasRunnerBuildResult> buildFuture;
    private Process process;
    private CanvasRunnerProcessChannel processChannel;
    private CanvasRunnerRuntimeLease processRuntimeLease;
    private NativeCanvasSurfaceMetrics nativeSurfaceMetrics;
    private CanvasRunnerRuntimeEvent.Presented latestRunnerPresentation;
    private CanvasLayoutKey confirmedSurfaceLayout;
    private CanvasSurfaceMetrics confirmedSurfaceMetrics;
    private CanvasRevisionKey renderedRevision;
    private long minimumSurfaceLayoutSequence;
    private BoundedDiagnostics runtimeDiagnostics;
    private Cancellable attachPoll;
    private Cancellable closePoll;
    private long attachDeadlineNanos;
    private long closeDeadlineNanos;
    private long generation;
    private long surfaceEpoch;
    private long presentationGeneration;
    private CanvasSessionId canvasSessionId;
    private CanvasEngineIdentity canvasEngineIdentity;
    private CanvasPresentationGate presentationGate;
    private StableId presentationDocumentId;
    private long nextPresentationSequence;
    private CanvasIntentReplayGate intentReplayGate;
    private CanvasRenderRequest currentRenderRequest;
    private CanvasLayoutKey currentLayout;
    private Set<StableId> currentWidgetIds = Set.of();
    private StableId desiredSelection;
    private CanvasViewportPresentation desiredViewportPresentation =
            CanvasViewportPresentation.fit();
    private CanvasRevisionKey lastViewportCommandRevision;
    private CanvasViewportPresentation lastViewportCommand;
    private long nextViewportCommandSequence = 1;
    private long lastViewportCommandSequence;
    private boolean viewportCommandPending;
    private boolean viewportAckDeferredBySurfaceTransition;
    private boolean viewportCommandRetryPending;
    private int viewportCommandRetryAttempts;
    private final Timer viewportCommandRetryTimer;
    private long nextWidgetMovePreviewSequence = 1;
    private WidgetMovePreviewPlacement widgetMovePreviewPlacement;
    private WidgetMovePreviewCommand desiredWidgetMovePreviewCommand;
    private boolean widgetMovePreviewRetryPending;
    private int widgetMovePreviewRetryAttempts;
    private final Timer widgetMovePreviewRetryTimer;
    private final Timer interactionFenceRetryTimer;
    private final Timer interactionFenceAckTimer;
    private final Timer focusRetryTimer;
    private int focusRetryAttempts;
    private long focusRetryGeneration = -1;
    private Process focusRetryProcess;
    private long nextFocusRetryTicket;
    private long scheduledFocusRetryTicket = -1;
    private PendingPresentation pendingPresentation;
    private boolean requestedVisible;
    private boolean focusRequested;
    private boolean paletteCatalogInsertDropAvailable;
    private boolean launchPending;
    private boolean restartAvailable;
    private boolean closeEscalationScheduled;
    private boolean runnerCloseAcknowledged;
    private PeerLossCloseAuthority peerLossCloseAuthority;
    private boolean closeExitObserved;
    private boolean closeAckDrainPending;
    private CanvasSessionId closingCanvasSessionId;
    private long closingProcessGeneration = -1;
    private boolean restartAfterRetirement;
    private String closingHostFailure;
    private String closingEscalationDetail;
    private String peerLossCloseDetail;
    private boolean peerLossCloseEscalated;
    private String peerLossEscalationDetail;
    private String retiringCleanupFailure;
    private FlutterDesignerNativeCanvasStatus lastPublishedStatus;
    private FlutterDesignerNativeCanvasStatus lastTerminalFailureStatus;
    private boolean closed;

    static FlutterDesignerNativeCanvasSession createDefault(
            NativeCanvasPlatformProvider platformProvider,
            NativeCanvasHost host,
            Consumer<FlutterDesignerNativeCanvasStatus> listener,
            Consumer<StableId> selectionListener,
            Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver,
            Consumer<AdmittedPaletteDrop> paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener)
            throws IOException {
        NativeCanvasRunnerContract runnerContract = requireRunnerContract(
                platformProvider);
        return new FlutterDesignerNativeCanvasSession(
                platformProvider,
                runnerContract,
                host,
                defaultRuntime(
                        CanvasRunnerBuildService.createDefault(runnerContract),
                        new FlutterToolchainService()),
                listener,
                selectionListener,
                paletteDropTokenResolver,
                paletteDropListener,
                deleteSelectionListener);
    }

    FlutterDesignerNativeCanvasSession(
            NativeCanvasPlatformProvider platformProvider,
            NativeCanvasHost host,
            RuntimeServices runtime,
            Consumer<FlutterDesignerNativeCanvasStatus> listener,
            Consumer<StableId> selectionListener,
            Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver,
            Consumer<AdmittedPaletteDrop> paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener) {
        this(platformProvider, requireRunnerContract(platformProvider), host, runtime,
                listener, selectionListener, paletteDropTokenResolver,
                paletteDropListener, deleteSelectionListener);
    }

    private FlutterDesignerNativeCanvasSession(
            NativeCanvasPlatformProvider platformProvider,
            NativeCanvasRunnerContract runnerContract,
            NativeCanvasHost host,
            RuntimeServices runtime,
            Consumer<FlutterDesignerNativeCanvasStatus> listener,
            Consumer<StableId> selectionListener,
            Function<String, Optional<WidgetTypeId>> paletteDropTokenResolver,
            Consumer<AdmittedPaletteDrop> paletteDropListener,
            Consumer<CanvasRunnerRuntimeEvent.DeleteSelection>
                    deleteSelectionListener) {
        requireEventDispatchThread();
        this.platformProvider = Objects.requireNonNull(
                platformProvider, "platformProvider");
        this.runnerContract = Objects.requireNonNull(
                runnerContract, "runnerContract");
        if (runnerContract.platform() != platformProvider.platform()) {
            throw new IllegalArgumentException(
                    "Native Canvas provider and runner contract platforms differ");
        }
        this.host = Objects.requireNonNull(host, "host");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.selectionListener = Objects.requireNonNull(
                selectionListener, "selectionListener");
        this.paletteDropTokenResolver = Objects.requireNonNull(
                paletteDropTokenResolver, "paletteDropTokenResolver");
        this.paletteDropListener = Objects.requireNonNull(
                paletteDropListener, "paletteDropListener");
        this.deleteSelectionListener = Objects.requireNonNull(
                deleteSelectionListener, "deleteSelectionListener");
        viewportCommandRetryTimer = new Timer(
                VIEWPORT_RETRY_DELAY_MILLIS,
                event -> retryViewportPresentation());
        viewportCommandRetryTimer.setRepeats(false);
        widgetMovePreviewRetryTimer = new Timer(
                WIDGET_MOVE_PREVIEW_RETRY_DELAY_MILLIS,
                event -> retryWidgetMovePreview());
        widgetMovePreviewRetryTimer.setRepeats(false);
        interactionFenceRetryTimer = new Timer(
                INTERACTION_FENCE_RETRY_DELAY_MILLIS,
                event -> retryInteractionFence());
        interactionFenceRetryTimer.setRepeats(false);
        interactionFenceAckTimer = new Timer(
                INTERACTION_FENCE_ACK_TIMEOUT_MILLIS,
                event -> interactionFenceAckTimedOut());
        interactionFenceAckTimer.setRepeats(false);
        focusRetryTimer = new Timer(
                FOCUS_RETRY_DELAY_MILLIS,
                event -> retryHostFocus(event.getActionCommand()));
        focusRetryTimer.setRepeats(false);
        host.onPeerReady(this::startIfPossible);
        host.onPeerWillBeLost(this::peerWillBeLost);
        host.onPeerLost(this::peerLost);
        host.onAttachmentFailed(this::attachmentFailed);
        host.onSurfaceMetricsChanged(this::surfaceMetricsChanged);
    }

    @Override
    public JComponent component() {
        requireEventDispatchThread();
        return host.component();
    }

    @Override
    public boolean isSurfaceFocused() {
        requireEventDispatchThread();
        return host.isRunnerFocused();
    }

    @Override
    public boolean releaseSurfaceFocus() {
        requireEventDispatchThread();
        return host.releaseRunnerFocus();
    }

    @Override
    public void show() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        clearWidgetMovePreview();
        requestedVisible = true;
        try {
            if (process != null && host.isRunnerAttached()) {
                host.setRunnerVisible(true);
                requestHostFocusIfPossible();
                return;
            }
        } catch (RuntimeException | LinkageError failure) {
            failCurrentProcess("Show native Flutter Canvas", failureReason(failure));
            return;
        }
        startIfPossible();
    }

    @Override
    public void hide() {
        requireEventDispatchThread();
        clearWidgetMovePreview();
        clearFocusRequest();
        requestedVisible = false;
        try {
            host.setRunnerVisible(false);
        } catch (RuntimeException | LinkageError failure) {
            failCurrentProcess("Hide native Flutter Canvas", failureReason(failure));
        }
    }

    /** Transfers activation to the currently verified provider-owned surface. */
    @Override
    public void requestFocus() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        focusRequested = true;
        cancelFocusRetry();
        requestHostFocusIfPossible();
    }

    /** Clears deferred activation so a late native attachment cannot steal focus. */
    @Override
    public void clearFocusRequest() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        focusRequested = false;
        cancelFocusRetry();
        advanceInteractionFence();
        if (!interactionFenceAccepting) {
            timeOutInteractionBarrier(Optional.ofNullable(currentLayout));
            return;
        }
        beginInteractionBarrier(Optional.ofNullable(currentLayout));
        sendInteractionFenceIfPossible();
    }

    private void advanceInteractionFence() {
        if (closed || !interactionFenceAccepting) {
            return;
        }
        if (interactionFenceSequence == CanvasWireProtocol.MAX_SEQUENCE) {
            // Fail closed rather than wrapping an epoch and admitting an old
            // physical interaction under a reused identity.
            interactionFenceAccepting = false;
            interactionFenceRetryTimer.stop();
            interactionFenceRetryPending = false;
            return;
        }
        interactionFenceSequence++;
        interactionFenceRetryAttempts = 0;
    }

    private void sendInteractionFenceIfPossible() {
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        if (closed
                || channel == null
                || layout == null
                || !layout.equals(confirmedSurfaceLayout)
                || !interactionFenceAccepting
                || interactionBarrierState.phase()
                        == InteractionBarrierPhase.TIMED_OUT) {
            return;
        }
        if (interactionBarrierState.phase()
                != InteractionBarrierPhase.SYNCHRONIZING
                || interactionBarrierState.fenceSequence()
                        != interactionFenceSequence
                || !interactionBarrierState.layoutKey().filter(layout::equals)
                        .isPresent()) {
            beginInteractionBarrier(Optional.of(layout));
        }
        if (layout.equals(lastInteractionFenceLayout)
                && interactionFenceSequence == lastInteractionFenceSequence
                && interactionBarrierState.inputEnabled()) {
            interactionFenceRetryTimer.stop();
            interactionFenceRetryPending = false;
            interactionFenceRetryAttempts = 0;
            return;
        }
        try {
            if (channel.interactionFence(layout, interactionFenceSequence)) {
                lastInteractionFenceLayout = layout;
                lastInteractionFenceSequence = interactionFenceSequence;
                pendingInteractionFenceLayout = layout;
                pendingInteractionFenceSequence = interactionFenceSequence;
                interactionFenceAckTimer.restart();
                interactionFenceRetryTimer.stop();
                interactionFenceRetryPending = false;
                interactionFenceRetryAttempts = 0;
                return;
            }
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.FINE,
                    "Native Flutter Canvas interaction fence could not be queued",
                    failure);
        }
        scheduleInteractionFenceRetry();
    }

    private void scheduleInteractionFenceRetry() {
        if (closed || processChannel == null || currentLayout == null) {
            interactionFenceRetryPending = false;
            return;
        }
        interactionFenceRetryAttempts++;
        if (interactionFenceRetryAttempts
                >= MAX_INTERACTION_FENCE_RETRY_ATTEMPTS) {
            interactionFenceRetryPending = false;
            pendingInteractionFenceLayout = null;
            pendingInteractionFenceSequence = -1;
            timeOutInteractionBarrier(Optional.ofNullable(currentLayout));
            LOGGER.log(
                    Level.FINE,
                    "Native Flutter Canvas interaction fence remained queued "
                    + "out after {0} bounded attempts.",
                    interactionFenceRetryAttempts);
            return;
        }
        interactionFenceRetryPending = true;
        interactionFenceRetryTimer.restart();
    }

    private void retryInteractionFence() {
        requireEventDispatchThread();
        if (!interactionFenceRetryPending) {
            return;
        }
        interactionFenceRetryPending = false;
        sendInteractionFenceIfPossible();
    }

    private void beginInteractionBarrier(Optional<CanvasLayoutKey> layout) {
        interactionFenceAckTimer.stop();
        pendingInteractionFenceLayout = layout.orElse(null);
        pendingInteractionFenceSequence = interactionFenceSequence;
        appliedInteractionFenceLayout = null;
        appliedInteractionFenceSequence = -1;
        publishInteractionBarrierState(new InteractionBarrierState(
                InteractionBarrierPhase.SYNCHRONIZING,
                interactionFenceSequence,
                layout));
    }

    private void synchronizeInteractionBarrier(CanvasLayoutKey layout) {
        interactionFenceAckTimer.stop();
        pendingInteractionFenceLayout = null;
        pendingInteractionFenceSequence = -1;
        appliedInteractionFenceLayout = layout;
        appliedInteractionFenceSequence = interactionFenceSequence;
        publishInteractionBarrierState(new InteractionBarrierState(
                InteractionBarrierPhase.SYNCHRONIZED,
                interactionFenceSequence,
                Optional.of(layout)));
    }

    private void interactionFenceAckTimedOut() {
        requireEventDispatchThread();
        if (closed
                || interactionBarrierState.phase()
                        != InteractionBarrierPhase.SYNCHRONIZING
                || pendingInteractionFenceSequence != interactionFenceSequence) {
            return;
        }
        timeOutInteractionBarrier(
                Optional.ofNullable(pendingInteractionFenceLayout));
    }

    private void timeOutInteractionBarrier(Optional<CanvasLayoutKey> layout) {
        interactionFenceAckTimer.stop();
        interactionFenceRetryTimer.stop();
        interactionFenceRetryPending = false;
        appliedInteractionFenceLayout = null;
        appliedInteractionFenceSequence = -1;
        publishInteractionBarrierState(new InteractionBarrierState(
                InteractionBarrierPhase.TIMED_OUT,
                interactionFenceSequence,
                layout));
    }

    private void deactivateInteractionBarrier() {
        interactionFenceAckTimer.stop();
        pendingInteractionFenceLayout = null;
        pendingInteractionFenceSequence = -1;
        appliedInteractionFenceLayout = null;
        appliedInteractionFenceSequence = -1;
        publishInteractionBarrierState(new InteractionBarrierState(
                InteractionBarrierPhase.INACTIVE,
                interactionFenceSequence,
                Optional.empty()));
    }

    private void publishInteractionBarrierState(
            InteractionBarrierState next) {
        if (next.equals(interactionBarrierState)) {
            return;
        }
        interactionBarrierState = next;
        interactionBarrierListener.accept(next);
    }

    private void requestHostFocusIfPossible() {
        Process current = process;
        long currentGeneration = generation;
        if (!focusRequested || !requestedVisible || current == null) {
            cancelFocusRetry();
            return;
        }
        if (focusRetryGeneration == currentGeneration
                && focusRetryProcess == current
                && focusRetryAttempts >= MAX_FOCUS_RETRY_ATTEMPTS) {
            return;
        }
        final boolean attached;
        try {
            attached = host.isRunnerAttached();
        } catch (RuntimeException | LinkageError failure) {
            logFocusFailure(failure);
            scheduleFocusRetry(currentGeneration, current);
            return;
        }
        if (!attached) {
            // Attachment polling replays the retained focus intent once the
            // exact provider-owned surface is available. Do not spin a second
            // timer while attachment ownership is still unresolved.
            cancelFocusRetry();
            return;
        }
        final boolean focused;
        try {
            focused = host.requestRunnerFocus();
        } catch (RuntimeException | LinkageError failure) {
            // OS focus transfer is best-effort and may be rejected by
            // foreground-window policy. It is not evidence that the owned
            // process or native surface is dead, so activation must not turn
            // a healthy generation into a terminal failure.
            logFocusFailure(failure);
            scheduleFocusRetry(currentGeneration, current);
            return;
        }
        if (focused) {
            cancelFocusRetry();
        } else {
            scheduleFocusRetry(currentGeneration, current);
        }
    }

    private void retryHostFocus(String actionCommand) {
        requireEventDispatchThread();
        long deliveredTicket = parseFocusRetryTicket(actionCommand);
        if (deliveredTicket < 0
                || deliveredTicket != scheduledFocusRetryTicket) {
            return;
        }
        scheduledFocusRetryTicket = -1;
        long expectedGeneration = focusRetryGeneration;
        Process expectedProcess = focusRetryProcess;
        focusRetryTimer.stop();
        if (expectedGeneration < 0
                || expectedGeneration != generation
                || expectedProcess == null
                || expectedProcess != process
                || !expectedProcess.isAlive()
                || !focusRequested
                || !requestedVisible
                || closed) {
            cancelFocusRetry();
            return;
        }
        requestHostFocusIfPossible();
    }

    private void scheduleFocusRetry(long expectedGeneration, Process expectedProcess) {
        if (closed
                || !focusRequested
                || !requestedVisible
                || expectedGeneration != generation
                || expectedProcess == null
                || expectedProcess != process) {
            cancelFocusRetry();
            return;
        }
        if (focusRetryGeneration != expectedGeneration
                || focusRetryProcess != expectedProcess) {
            focusRetryGeneration = expectedGeneration;
            focusRetryProcess = expectedProcess;
            focusRetryAttempts = 0;
        }
        focusRetryAttempts++;
        if (focusRetryAttempts >= MAX_FOCUS_RETRY_ATTEMPTS) {
            focusRetryTimer.stop();
            LOGGER.log(
                    Level.FINE,
                    "Native Flutter Canvas focus remained unavailable after {0} "
                    + "bounded attempts for runner generation {1}.",
                    new Object[] {focusRetryAttempts, expectedGeneration});
            return;
        }
        scheduledFocusRetryTicket = ++nextFocusRetryTicket;
        focusRetryTimer.setActionCommand(
                Long.toUnsignedString(scheduledFocusRetryTicket));
        focusRetryTimer.restart();
    }

    private void cancelFocusRetry() {
        focusRetryTimer.stop();
        scheduledFocusRetryTicket = -1;
        focusRetryAttempts = 0;
        focusRetryGeneration = -1;
        focusRetryProcess = null;
    }

    private static long parseFocusRetryTicket(String actionCommand) {
        if (actionCommand == null) {
            return -1;
        }
        try {
            return Long.parseUnsignedLong(actionCommand);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void logFocusFailure(Throwable failure) {
        LOGGER.log(
                Level.WARNING,
                "The native Flutter Canvas focus request failed; the current "
                + "runner generation remains active and a bounded retry may follow.",
                failure);
    }

    /**
     * Explicitly retries one terminal session failure with a fresh process and
     * protocol-session generation. Failures never schedule this retry
     * themselves, preventing a crash/restart loop on the event-dispatch thread.
     */
    @Override
    public boolean restart() {
        requireEventDispatchThread();
        if (closed || !restartAvailable || process != null || launchPending
                || !inFlightLaunches.isEmpty() || !retiringProcesses.isEmpty()) {
            return false;
        }
        restartAvailable = false;
        retiringCleanupFailure = null;
        lastTerminalFailureStatus = null;
        generation++;
        stopAttachTimer();
        abortProcessChannel();
        clearPresentationAuthority();
        runtimeDiagnostics = null;
        requestedVisible = true;
        startIfPossible();
        return launchPending || process != null;
    }

    @Override
    public boolean canRestart() {
        requireEventDispatchThread();
        return !closed && restartAvailable && process == null && !launchPending
                && inFlightLaunches.isEmpty() && retiringProcesses.isEmpty();
    }

    /**
     * Compatibility seam for protocol tests and legacy callers without a
     * verified project theme. Production project-backed Design views always
     * call the exact-theme overload below.
     */
    void present(
            DesignerDocument document,
            WidgetCatalog catalog,
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform) {
        DesignerThemeMode requestedMode = Objects.requireNonNull(document, "document")
                .canvas()
                .flatMap(preferences -> preferences.themeMode())
                .orElse(DesignerThemeMode.LIGHT);
        CanvasThemeBrightness brightness = requestedMode == DesignerThemeMode.DARK
                ? CanvasThemeBrightness.DARK
                : CanvasThemeBrightness.LIGHT;
        present(
                document,
                catalog,
                previewMode,
                targetPlatform,
                CanvasPreviewProfileResolver.legacyTheme(brightness));
    }

    /** Publishes the latest validated read-only document for this Design view. */
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
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        Objects.requireNonNull(orientationOverride, "orientationOverride");
        clearWidgetMovePreview();
        final ValidatedCanvasRevisionSnapshot snapshot;
        try {
            snapshot = ValidatedCanvasRevisionSnapshot.captureReadOnly(
                    0L,
                    Objects.requireNonNull(document, "document"),
                    Objects.requireNonNull(catalog, "catalog"),
                    ValidationLimits.defaults());
        } catch (IllegalArgumentException failure) {
            withdraw();
            publishFailure(
                    "Validate native Flutter Canvas model",
                    failureReason(failure));
            return;
        }
        Set<StableId> widgetIds = collectWidgetIds(snapshot.document().root());
        if (desiredSelection == null || !widgetIds.contains(desiredSelection)) {
            desiredSelection = snapshot.document().root().id();
        }
        long publication = ++presentationGeneration;
        pendingPresentation = new PendingPresentation(
                publication,
                snapshot,
                Objects.requireNonNull(previewMode, "previewMode"),
                Objects.requireNonNull(targetPlatform, "targetPlatform"),
                Objects.requireNonNull(resolvedTheme, "resolvedTheme"),
                Objects.requireNonNull(imageResources, "imageResources"),
                orientationOverride,
                widgetIds);
        currentRenderRequest = null;
        currentLayout = null;
        currentWidgetIds = Set.of();
        beginInteractionBarrier(Optional.empty());
        if (process == null) {
            if (!retiringProcesses.isEmpty()) {
                restartAfterRetirement = requestedVisible;
            }
            startIfPossible();
        } else {
            ensureProcessChannel();
            publishPendingPresentation();
        }
    }

    /** Invalidates old Canvas interaction authority and removes stale pixels. */
    @Override
    public void withdraw() {
        requireEventDispatchThread();
        clearWidgetMovePreview();
        presentationGeneration++;
        pendingPresentation = null;
        desiredSelection = null;
        restartAfterRetirement = false;
        restartAvailable = false;
        lastTerminalFailureStatus = null;
        clearPresentationAuthority();
        if (process == null) {
            if (launchPending) {
                generation++;
                launchPending = false;
            }
            publishWithdrawnStatus(!inFlightLaunches.isEmpty()
                    || !retiringProcesses.isEmpty());
            return;
        }
        generation++;
        launchPending = false;
        stopAttachTimer();
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        abortProcessChannel();
        runtimeDiagnostics = null;
        try {
            host.detachRunner();
        } catch (RuntimeException | LinkageError ignored) {
            // The process is still terminated below and cannot retain authority.
        }
        terminateAndRelease(current, currentRuntimeLease);
        publishWithdrawnStatus(true);
    }

    /** Mirrors trusted NetBeans tree selection into the current Flutter overlay. */
    @Override
    public void selectWidget(StableId widgetId) {
        requireEventDispatchThread();
        Objects.requireNonNull(widgetId, "widgetId");
        Set<StableId> availableWidgetIds = pendingPresentation == null
                ? currentWidgetIds
                : pendingPresentation.widgetIds();
        if (!availableWidgetIds.contains(widgetId)) {
            return;
        }
        desiredSelection = widgetId;
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        if (channel == null || layout == null) {
            return;
        }
        try {
            channel.select(layout, widgetId);
        } catch (RuntimeException failure) {
            failCurrentProcess(
                    "Select widget in native Flutter Canvas",
                    failureReason(failure));
        }
    }

    /** Installs the per-view presentation feedback sink without changing the model. */
    @Override
    public void setViewportMetricsListener(
            Consumer<CanvasViewportMetrics> listener) {
        requireEventDispatchThread();
        viewportMetricsListener = Objects.requireNonNull(listener, "listener");
    }

    /** Installs the per-view sink for an admitted native-surface pointer-down. */
    @Override
    public void setInteractionListener(Runnable listener) {
        requireEventDispatchThread();
        interactionListener = Objects.requireNonNull(listener, "listener");
    }

    /** Installs the per-view sink for a revision-fenced inline Text edit. */
    @Override
    public void setTextEditCommitListener(
            Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener) {
        requireEventDispatchThread();
        textEditCommitListener = Objects.requireNonNull(listener, "listener");
    }

    /** Publishes typed runner-input synchronization state on the EDT. */
    @Override
    public void setInteractionBarrierListener(
            Consumer<InteractionBarrierState> listener) {
        requireEventDispatchThread();
        interactionBarrierListener = Objects.requireNonNull(
                listener, "listener");
        interactionBarrierListener.accept(interactionBarrierState);
    }

    @Override
    public InteractionBarrierState interactionBarrierState() {
        requireEventDispatchThread();
        return interactionBarrierState;
    }

    /**
     * Changes only the in-IDE view transform. The logical Flutter viewport and
     * persisted {@code .fd} document remain unchanged.
     */
    @Override
    public void setViewportPresentation(
            CanvasViewportPresentation presentation) {
        requireEventDispatchThread();
        Objects.requireNonNull(presentation, "presentation");
        if (closed) {
            return;
        }
        if (!presentation.equals(desiredViewportPresentation)) {
            viewportCommandRetryAttempts = 0;
        }
        desiredViewportPresentation = presentation;
        sendViewportPresentationIfPossible();
    }

    @Override
    public void close() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        closed = true;
        requestedVisible = false;
        focusRequested = false;
        launchPending = false;
        restartAvailable = false;
        lastTerminalFailureStatus = null;
        restartAfterRetirement = false;
        stopAttachTimer();
        viewportCommandRetryTimer.stop();
        widgetMovePreviewRetryTimer.stop();
        cancelFocusRetry();
        closingHostFailure = null;
        closingEscalationDetail = null;
        CanvasRunnerBuildResult currentPrepared = prepared;
        prepared = null;
        Process current = process;
        if (current == null) {
            closeEscalationScheduled = peerLossCloseEscalated;
            closingEscalationDetail = appendFailure(
                    peerLossCloseDetail, peerLossEscalationDetail);
        }
        clearPresentationAuthority();
        pendingPresentation = null;
        desiredSelection = null;
        CanvasRunnerProcessChannel channel = null;
        String closeRequestFailure = null;
        if (current != null) {
            runnerCloseAcknowledged = false;
            peerLossCloseAuthority = null;
            closeExitObserved = false;
            closeAckDrainPending = false;
            closeEscalationScheduled = false;
            channel = processChannel;
            closingCanvasSessionId = channel == null ? null : channel.sessionId();
            closingProcessGeneration = generation;
            if (channel == null) {
                closeRequestFailure = "host.close could not be sent because the "
                        + "lifecycle protocol channel was not available";
            } else {
                try {
                    channel.close();
                    if (channel.awaitAuthenticatedClose(
                            CLOSE_ACK_BEFORE_PEER_TEARDOWN_TIMEOUT)) {
                        runnerCloseAcknowledged = true;
                    } else if (!channel.awaitCloseRequestWritten(Duration.ZERO)) {
                        closingEscalationDetail = appendFailure(
                                closingEscalationDetail,
                                "The host.close frame was not confirmed written before "
                                + "native peer teardown");
                    } else {
                        closingEscalationDetail = appendFailure(
                                closingEscalationDetail,
                                "An authenticated runner.closed acknowledgement was not "
                                + "observed before native peer teardown");
                    }
                } catch (RuntimeException | LinkageError failure) {
                    closeRequestFailure = "host.close could not be sent to the runner: "
                            + failureReason(failure);
                }
            }
        }
        if (currentPrepared != null) {
            try {
                currentPrepared.close();
            } catch (RuntimeException | LinkageError failure) {
                closingHostFailure = appendFailure(
                        closingHostFailure,
                        "runtime-generation cleanup: " + failureReason(failure));
            }
        }
        try {
            host.close();
        } catch (RuntimeException | LinkageError failure) {
            closingHostFailure = failureReason(failure);
        }
        if (current == null) {
            generation++;
            CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
            processRuntimeLease = null;
            abortProcessChannel();
            runtimeDiagnostics = null;
            closeLease(currentRuntimeLease);
            if (!maybePublishStopped(runnerCloseAcknowledged)) {
                publishClosedCleanupActive();
            }
            return;
        }
        if (closeRequestFailure != null) {
            scheduleCloseEscalation(
                    generation,
                    current,
                    closeRequestFailure);
            return;
        }
        try {
            closeDeadlineNanos = runtime.nanoTime().getAsLong()
                    + CLOSE_HANDSHAKE_TIMEOUT.toNanos();
            closePoll = runtime.pollScheduler().schedule(
                    () -> pollGracefulClose(generation, current));
        } catch (RuntimeException | LinkageError failure) {
            scheduleCloseEscalation(
                    generation,
                    current,
                    "Graceful-close monitoring could not start: "
                    + failureReason(failure));
        }
    }

    private void startIfPossible() {
        requireEventDispatchThread();
        if (closed || restartAvailable || !requestedVisible
                || !retiringProcesses.isEmpty()
                || !inFlightLaunches.isEmpty()
                || process != null || launchPending) {
            if (!closed && requestedVisible && process == null
                    && (!retiringProcesses.isEmpty()
                            || !inFlightLaunches.isEmpty())
                    && pendingPresentation != null) {
                restartAfterRetirement = true;
            }
            return;
        }
        try {
            if (!host.isNativePeerReady()) {
                return;
            }
        } catch (RuntimeException | LinkageError failure) {
            publishTerminalFailure(
                    "Inspect native Flutter Canvas host", failureReason(failure));
            return;
        }
        if (prepared != null) {
            launch(prepared);
            return;
        }
        if (buildFuture != null) {
            return;
        }
        final SdkResolution toolchain;
        try {
            toolchain = runtime.sdkResolver().resolve();
        } catch (RuntimeException failure) {
            publishTerminalFailure(
                    "Resolve Flutter SDK for native Canvas", failureReason(failure));
            return;
        }
        FlutterSdk sdk = toolchain.sdk();
        if (sdk == null) {
            publish(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Flutter SDK is required to build the isolated Canvas runner. "
                    + toolchain.message()));
            return;
        }
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.PREPARING,
                "Preparing native Flutter Canvas...",
                "Building or verifying the isolated native Flutter runner with "
                + sdk.flutterExecutable() + ". The cached build is reused."));
        try {
            buildFuture = Objects.requireNonNull(
                    runtime.buildStarter().buildAsync(sdk),
                    "Native Canvas build starter returned no future");
        } catch (RuntimeException failure) {
            publishTerminalFailure(
                    "Schedule native Flutter Canvas build", failureReason(failure));
            return;
        }
        buildFuture.whenComplete((result, failure) -> {
            try {
                runtime.uiExecutor().execute(() -> buildCompleted(result, failure));
            } catch (RuntimeException | LinkageError exception) {
                if (result != null) {
                    result.close();
                }
            }
        });
    }

    private void buildCompleted(CanvasRunnerBuildResult result, Throwable failure) {
        requireEventDispatchThread();
        buildFuture = null;
        if (closed) {
            if (result != null) {
                result.close();
            }
            return;
        }
        if (failure != null) {
            if (result != null) {
                result.close();
            }
            publishTerminalFailure(
                    "Build native Flutter Canvas runner",
                    failureReason(failure));
            return;
        }
        if (result == null) {
            publishTerminalFailure(
                    "Build native Flutter Canvas runner",
                    "the build completed without a runtime result");
            return;
        }
        if (!runnerContract.fingerprint().equals(
                    result.identity().runnerContractIdentity())
                || !runnerContract.buildTarget().executableName().equals(
                        result.executable().getFileName().toString())) {
            result.close();
            publishTerminalFailure(
                    "Build native Flutter Canvas runner",
                    "the build result does not belong to the selected provider contract");
            return;
        }
        prepared = result;
        if (requestedVisible) {
            startIfPossible();
        }
    }

    private void launch(CanvasRunnerBuildResult runner) {
        requireEventDispatchThread();
        if (closed || process != null) {
            return;
        }
        nativeSurfaceMetrics = null;
        runnerCloseAcknowledged = false;
        peerLossCloseAuthority = null;
        peerLossCloseDetail = null;
        peerLossCloseEscalated = false;
        peerLossEscalationDetail = null;
        closeEscalationScheduled = false;
        closingEscalationDetail = null;
        final NativeCanvasParentHandle parentHandle;
        final long epoch;
        final long launchGeneration;
        final NativeCanvasRunnerLaunch launch;
        final CanvasRunnerRuntimeLease launchRuntimeLease;
        try {
            if (!host.isNativePeerReady()) {
                return;
            }
            parentHandle = host.parentHandle();
            long hostProcessId = ProcessHandle.current().pid();
            epoch = ++surfaceEpoch;
            launchGeneration = ++generation;
            launch = platformProvider.createLaunch(
                    runner.executable(), parentHandle, hostProcessId, epoch, newNonce());
            if (!runner.executable().equals(launch.executable())) {
                throw new IllegalArgumentException(
                        "Native Canvas launch replaced the contracted runner executable");
            }
            launchRuntimeLease = runner.retainRuntimeLease();
        } catch (RuntimeException | LinkageError exception) {
            publishTerminalFailure(
                    "Prepare native Flutter Canvas launch",
                    failureReason(exception));
            return;
        }
        launchPending = true;
        inFlightLaunches.add(launchGeneration);
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                "Starting native Flutter Canvas...",
                "Launching the isolated native surface for Canvas epoch " + epoch
                + ". It remains hidden until the provider verifies its parent handle, "
                + "surface hierarchy and PID ownership."));
        try {
            runtime.launchExecutor().execute(() -> {
                Process launched = null;
                try {
                    launched = runtime.processStarter().start(
                            launch.command(), launch.workingDirectory());
                    Process started = launched;
                    BoundedDiagnostics diagnostics = new BoundedDiagnostics();
                    drainErrorOutput(started, diagnostics);
                    try {
                        runtime.uiExecutor().execute(() -> processStarted(
                                launchGeneration, epoch, parentHandle, started, diagnostics,
                                launchRuntimeLease));
                    } catch (RuntimeException | LinkageError exception) {
                        inFlightLaunches.remove(launchGeneration);
                        terminateAndRelease(started, launchRuntimeLease);
                    }
                } catch (IOException | RuntimeException | LinkageError exception) {
                    if (launched == null) {
                        closeLease(launchRuntimeLease);
                    } else {
                        terminateAndRelease(launched, launchRuntimeLease);
                    }
                    try {
                        runtime.uiExecutor().execute(
                                () -> launchFailed(launchGeneration, exception));
                    } catch (RuntimeException | LinkageError ignored) {
                        inFlightLaunches.remove(launchGeneration);
                        // The launch never acquired a process. Its lease is
                        // already released; no UI callback can be delivered.
                    }
                }
            });
        } catch (RuntimeException | LinkageError exception) {
            closeLease(launchRuntimeLease);
            inFlightLaunches.remove(launchGeneration);
            launchPending = false;
            generation++;
            publishTerminalFailure(
                    "Schedule native Flutter Canvas runner",
                    failureReason(exception));
        }
    }

    private void processStarted(
            long launchGeneration,
            long epoch,
            NativeCanvasParentHandle expectedParent,
            Process launched,
            BoundedDiagnostics diagnostics,
            CanvasRunnerRuntimeLease launchRuntimeLease) {
        requireEventDispatchThread();
        inFlightLaunches.remove(launchGeneration);
        if (closed || launchGeneration != generation) {
            launchPending = inFlightLaunches.contains(generation);
            terminateAndRelease(launched, launchRuntimeLease);
            return;
        }
        launchPending = false;
        final boolean peerReady;
        final NativeCanvasParentHandle currentParent;
        try {
            peerReady = host.isNativePeerReady();
            currentParent = peerReady ? host.parentHandle() : null;
        } catch (RuntimeException | LinkageError exception) {
            generation++;
            terminateAndRelease(launched, launchRuntimeLease);
            publishTerminalFailure(
                    "Verify native Flutter Canvas host",
                    failureReason(exception));
            return;
        }
        if (!peerReady) {
            terminateAndRelease(launched, launchRuntimeLease);
            return;
        }
        if (!expectedParent.equals(currentParent)) {
            generation++;
            terminateAndRelease(launched, launchRuntimeLease);
            if (requestedVisible) {
                startIfPossible();
            }
            return;
        }
        process = launched;
        processRuntimeLease = launchRuntimeLease;
        runtimeDiagnostics = diagnostics;
        ensureProcessChannel();
        attachDeadlineNanos = runtime.nanoTime().getAsLong()
                + ATTACH_TIMEOUT.toNanos();
        try {
            attachPoll = runtime.pollScheduler().schedule(
                    () -> pollAttachment(launchGeneration, epoch));
            launched.onExit().whenComplete((ignored, failure) -> {
                if (failure == null) {
                    deliverPhysicalExit(
                            launchGeneration, launched, launchRuntimeLease);
                } else {
                    observeSessionExitAfterOnExitFailure(
                            launchGeneration, launched, launchRuntimeLease);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            failCurrentProcess(
                    "Monitor native Flutter Canvas process",
                    failureReason(failure));
        }
    }

    private void pollAttachment(long launchGeneration, long epoch) {
        requireEventDispatchThread();
        Process current = process;
        if (closed || launchGeneration != generation || current == null) {
            stopAttachTimer();
            return;
        }
        if (!current.isAlive()) {
            processExited(launchGeneration, current);
            return;
        }
        try {
            if (!host.isNativePeerReady()) {
                peerLost();
                return;
            }
            if (host.isRunnerAttached()) {
                if (!host.isRunnerSurfaceLive()) {
                    attachmentFailed(
                            "The verified native Flutter Canvas surface is no longer live.");
                }
                return;
            }
            boolean attached = host.attachRunner(current.pid());
            if (closed || launchGeneration != generation || current != process) {
                return;
            }
            if (attached) {
                host.setRunnerVisible(requestedVisible);
                host.surfaceMetrics().ifPresent(this::surfaceMetricsChanged);
                if (closed || launchGeneration != generation
                        || current != process || !host.isRunnerAttached()) {
                    return;
                }
                requestHostFocusIfPossible();
                if (closed || launchGeneration != generation
                        || current != process || !host.isRunnerAttached()) {
                    return;
                }
                publishRunningStatus(epoch, current);
                publishPendingPresentation();
                return;
            }
        } catch (RuntimeException | LinkageError exception) {
            failCurrentProcess(
                    "Verify native Flutter Canvas attachment",
                    failureReason(exception));
            return;
        }
        if (runtime.nanoTime().getAsLong() >= attachDeadlineNanos) {
            failCurrentProcess(
                    "Attach native Flutter Canvas surface",
                    "The runner did not create one verified provider-owned native "
                    + "child surface within "
                    + ATTACH_TIMEOUT.toSeconds() + " seconds.");
        }
    }

    private void publishRunningStatus(long epoch, Process current) {
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                "Native Flutter Canvas is running.",
                "Verified provider-owned native Flutter surface " + epoch
                + " in isolated process " + current.pid()
                + "." + surfaceMetricsDetail()
                + " Rendering is native; no PNG or pixel-frame transport is used."));
    }

    private String surfaceMetricsDetail() {
        CanvasSurfaceMetrics confirmed = confirmedSurfaceMetrics;
        if (confirmed != null) {
            return " Confirmed Flutter surface " + confirmed.physicalWidth()
                    + "×" + confirmed.physicalHeight()
                    + " physical pixels at "
                    + formatDevicePixelRatio(confirmed.devicePixelRatioMicros())
                    + "× native scale.";
        }
        return nativeSurfaceMetrics == null
                ? ""
                : " Host surface target " + nativeSurfaceMetrics.width()
                + "×" + nativeSurfaceMetrics.height()
                + " physical pixels at "
                + formatDevicePixelRatio(nativeSurfaceMetrics)
                + "× native scale; awaiting the matching post-frame confirmation.";
    }

    private void processExited(long launchGeneration, Process exited) {
        requireEventDispatchThread();
        if (exited != process || launchGeneration != generation) {
            return;
        }
        stopAttachTimer();
        stopCloseTimer();
        generation++;
        process = null;
        CanvasRunnerRuntimeLease exitedRuntimeLease = processRuntimeLease;
        processRuntimeLease = null;
        clearPresentationAuthority();
        launchPending = false;
        closeLease(exitedRuntimeLease);
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        if (closed) {
            closeExitObserved = true;
            if (runnerCloseAcknowledged || processChannel == null) {
                abortProcessChannel();
                maybePublishStopped(runnerCloseAcknowledged);
            } else {
                beginCloseAckDrain();
            }
            return;
        }
        abortProcessChannel();
        String hostDetachFailure = null;
        try {
            host.detachRunner();
        } catch (RuntimeException | LinkageError failure) {
            hostDetachFailure = failureReason(failure);
        }
        String exitDescription;
        try {
            exitDescription = "code " + exited.exitValue();
        } catch (RuntimeException | LinkageError failure) {
            exitDescription = "an unavailable exit code ("
                    + failureReason(failure) + ")";
        }
        String cleanupSuffix = hostDetachFailure == null
                ? ""
                : " Native host detach also failed: " + hostDetachFailure + ".";
        publishTerminalFailure(
                "Run native Flutter Canvas",
                "The isolated runner exited with " + exitDescription
                + diagnosticSuffix(output) + cleanupSuffix);
    }

    private void pollGracefulClose(long closeGeneration, Process closingProcess) {
        requireEventDispatchThread();
        if (!closed || closeGeneration != generation || closingProcess != process) {
            stopCloseTimer();
            return;
        }
        final boolean alive;
        try {
            alive = closingProcess.isAlive();
        } catch (RuntimeException | LinkageError failure) {
            scheduleCloseEscalation(
                    closeGeneration,
                    closingProcess,
                    "Runner liveness could not be inspected: "
                    + failureReason(failure));
            return;
        }
        if (!alive) {
            processExited(closeGeneration, closingProcess);
            return;
        }
        final long now;
        try {
            now = runtime.nanoTime().getAsLong();
        } catch (RuntimeException | LinkageError failure) {
            scheduleCloseEscalation(
                    closeGeneration,
                    closingProcess,
                    "Graceful-close clock failed: " + failureReason(failure));
            return;
        }
        if (now >= closeDeadlineNanos) {
            scheduleCloseEscalation(
                    closeGeneration,
                    closingProcess,
                    "The runner did not exit within the "
                    + CLOSE_HANDSHAKE_TIMEOUT.toSeconds()
                    + "-second graceful-close deadline.");
        }
    }

    private void beginCloseAckDrain() {
        requireEventDispatchThread();
        closeAckDrainPending = true;
        try {
            closeDeadlineNanos = runtime.nanoTime().getAsLong()
                    + CLOSE_ACK_DRAIN_TIMEOUT.toNanos();
            closePoll = runtime.pollScheduler().schedule(this::pollCloseAckDrain);
        } catch (RuntimeException | LinkageError failure) {
            closeAckDrainPending = false;
            closingEscalationDetail = appendFailure(
                    closingEscalationDetail,
                    "Authenticated runner.closed drain could not be monitored: "
                    + failureReason(failure));
            abortProcessChannel();
            maybePublishStopped(false);
        }
    }

    private void pollCloseAckDrain() {
        requireEventDispatchThread();
        if (!closeAckDrainPending) {
            stopCloseTimer();
            return;
        }
        if (runnerCloseAcknowledged) {
            closeAckDrainPending = false;
            stopCloseTimer();
            abortProcessChannel();
            maybePublishStopped(true);
            return;
        }
        final long now;
        try {
            now = runtime.nanoTime().getAsLong();
        } catch (RuntimeException | LinkageError failure) {
            closingEscalationDetail = appendFailure(
                    closingEscalationDetail,
                    "Authenticated runner.closed drain clock failed: "
                    + failureReason(failure));
            closeAckDrainPending = false;
            stopCloseTimer();
            abortProcessChannel();
            maybePublishStopped(false);
            return;
        }
        if (now >= closeDeadlineNanos) {
            closeAckDrainPending = false;
            stopCloseTimer();
            abortProcessChannel();
            maybePublishStopped(false);
        }
    }

    private void scheduleCloseEscalation(
            long closeGeneration,
            Process closingProcess,
            String detail) {
        requireEventDispatchThread();
        if (!closed || closeEscalationScheduled
                || closeGeneration != generation || closingProcess != process) {
            return;
        }
        closeEscalationScheduled = true;
        closingEscalationDetail = Objects.requireNonNull(detail, "detail");
        stopCloseTimer();
        abortProcessChannel();
        try {
            runtime.launchExecutor().execute(() -> {
                CompletableFuture<Void> termination;
                try {
                    termination = runtime.terminator().terminate(closingProcess);
                } catch (RuntimeException | LinkageError failure) {
                    deliverOnUi(() -> closeEscalationFailed(
                            closeGeneration, closingProcess, failure));
                    return;
                }
                termination.whenComplete((ignored, failure) -> {
                    if (failure != null) {
                        deliverOnUi(() -> closeEscalationFailed(
                                closeGeneration,
                                closingProcess,
                                unwrapCompletionFailure(failure)));
                    }
                });
            });
        } catch (RuntimeException | LinkageError failure) {
            closeEscalationFailed(closeGeneration, closingProcess, failure);
        }
    }

    private void closeEscalationFailed(
            long closeGeneration,
            Process closingProcess,
            Throwable failure) {
        requireEventDispatchThread();
        if (!closed || closeGeneration != generation || closingProcess != process) {
            return;
        }
        String reason = failureReason(failure);
        closingEscalationDetail = closingEscalationDetail
                + " Bounded termination also failed: " + reason + ".";
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                "Native Flutter Canvas cleanup failed.",
                "Target: " + processDescription(closingProcess)
                + ". Reason: " + closingEscalationDetail
                + " The runtime lease remains retained until physical exit."));
    }

    private void publishStopped(boolean acknowledged) {
        StringBuilder detail = new StringBuilder(160);
        if (acknowledged && !closeEscalationScheduled) {
            detail.append("The runner acknowledged host.close with an authenticated "
                    + "runner.closed response, and the isolated process exited "
                    + "naturally without bounded termination.");
        } else if (closeEscalationScheduled) {
            detail.append(acknowledged
                    ? "The runner acknowledged host.close but did not physically exit "
                            + "until bounded close escalation."
                    : "The isolated runner physically exited after bounded close "
                            + "escalation.");
        } else {
            detail.append("The isolated Flutter runner for this Design view was closed.");
        }
        if (closingEscalationDetail != null) {
            detail.append(' ').append(closingEscalationDetail);
        }
        if (closingHostFailure != null) {
            detail.append(" Native host cleanup reported: ")
                    .append(closingHostFailure).append('.');
        }
        if (retiringCleanupFailure != null) {
            detail.append(" An earlier bounded termination attempt reported: ")
                    .append(retiringCleanupFailure)
                    .append("; every retained process has now physically exited.");
        }
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                "Native Flutter Canvas stopped.",
                detail.toString()));
    }

    /**
     * Publishes the terminal close state only after every process claim and
     * every process-start operation that can still materialize a PID is gone.
     */
    private boolean maybePublishStopped(boolean acknowledged) {
        requireEventDispatchThread();
        if (!closed || process != null || !retiringProcesses.isEmpty()
                || !inFlightLaunches.isEmpty() || closeAckDrainPending) {
            return false;
        }
        publishStopped(acknowledged);
        return true;
    }

    private void publishClosedCleanupActive() {
        String targets = retiringProcesses.isEmpty()
                ? "an in-flight native runner launch"
                : retiringProcessDescription()
                        + (inFlightLaunches.isEmpty()
                                ? ""
                                : " and an in-flight native runner launch");
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                "Native Flutter Canvas cleanup is still active.",
                "Target: " + targets
                + ". The Design view is closed, but STOPPED will not be "
                + "reported until every owned runner PID physically exits and "
                + "every pending process launch completes."
                + (retiringCleanupFailure == null
                        ? ""
                        : " Last bounded termination failure: "
                        + retiringCleanupFailure + ".")));
    }

    private void publishWithdrawnStatus(boolean cleanupActive) {
        publish(new FlutterDesignerNativeCanvasStatus(
                cleanupActive
                        ? FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE
                        : FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                cleanupActive
                        ? "Native Flutter Canvas model withdrawn; cleanup is active."
                        : "Native Flutter Canvas model withdrawn.",
                "The previous read-only presentation was invalidated because the "
                + "current .fd state is not eligible for Canvas rendering."
                + (cleanupActive
                        ? " A replacement cannot start until every previous runner "
                                + "PID physically exits."
                        : " No runner process remains.")));
    }

    private void launchFailed(long launchGeneration, Throwable exception) {
        requireEventDispatchThread();
        inFlightLaunches.remove(launchGeneration);
        launchPending = inFlightLaunches.contains(generation);
        if (closed) {
            maybePublishStopped(false);
            return;
        }
        if (launchGeneration == generation) {
            publishTerminalFailure(
                    "Start native Flutter Canvas runner", failureReason(exception));
        } else {
            reconcileOwnershipDrained();
        }
    }

    private void peerLost() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        Process current = process;
        CanvasRunnerProcessChannel currentChannel = processChannel;
        long currentGeneration = generation;
        PeerLossCloseAuthority closeAuthority = peerLossCloseAuthority;
        boolean authenticatedClose = closeAuthority != null
                && closeAuthority.matches(
                        current,
                        currentGeneration,
                        currentChannel == null ? null : currentChannel.sessionId());
        generation++;
        launchPending = false;
        stopAttachTimer();
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        peerLossCloseAuthority = null;
        abortProcessChannel();
        clearPresentationAuthority();
        runtimeDiagnostics = null;
        restartAfterRetirement = requestedVisible;
        try {
            host.detachRunner();
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.WARNING,
                    "Native Flutter Canvas host detach failed after peer loss",
                    failure);
        }
        if (current != null) {
            if (authenticatedClose) {
                retireAfterAuthenticatedClose(current, currentRuntimeLease);
            } else {
                peerLossCloseEscalated = true;
                peerLossEscalationDetail = appendFailure(
                        peerLossEscalationDetail,
                        "The runner did not complete an authenticated peer-loss "
                                + "close; bounded termination was required.");
                terminateAndRelease(current, currentRuntimeLease);
            }
        } else {
            closeLease(currentRuntimeLease);
        }
        // A real AWT peer recreation publishes onPeerReady after addNotify.
        // Do not relaunch synchronously from the loss callback: that would turn
        // an incorrectly classified native-child failure into a process storm.
    }

    /**
     * Gives the exact runner a bounded chance to acknowledge host.close while
     * the AWT parent HWND is still live. NetBeans can remove a heavyweight tab
     * peer before componentClosed(), so this is the last reliable lifecycle
     * fence for both tab switches and final form close.
     */
    private void peerWillBeLost() {
        requireEventDispatchThread();
        if (closed || process == null || processChannel == null) {
            return;
        }
        peerLossCloseDetail = null;
        CanvasRunnerProcessChannel channel = processChannel;
        Process closingProcess = process;
        long closingGeneration = generation;
        CanvasSessionId closingSessionId = channel.sessionId();
        try {
            channel.requestClose(CanvasWireCloseReason.BACKEND_REPLACED);
            if (channel.awaitAuthenticatedClose(
                    CLOSE_ACK_BEFORE_PEER_TEARDOWN_TIMEOUT)
                    && process == closingProcess
                    && generation == closingGeneration
                    && processChannel == channel
                    && channel.sessionId().equals(closingSessionId)) {
                runnerCloseAcknowledged = true;
                peerLossCloseAuthority = new PeerLossCloseAuthority(
                        closingProcess, closingGeneration, closingSessionId);
            } else if (!channel.awaitCloseRequestWritten(Duration.ZERO)) {
                peerLossCloseDetail = "The host.close frame was not confirmed "
                        + "written before native peer loss.";
            } else {
                peerLossCloseDetail = "An authenticated runner.closed "
                        + "acknowledgement was not observed before native peer loss.";
            }
        } catch (RuntimeException | LinkageError failure) {
            peerLossCloseDetail = "Native peer-loss preparation failed: "
                    + failureReason(failure) + ".";
        }
    }

    private void attachmentFailed(String reason) {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        generation++;
        launchPending = false;
        stopAttachTimer();
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        abortProcessChannel();
        clearPresentationAuthority();
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        restartAfterRetirement = false;
        String hostDetachFailure = null;
        try {
            host.detachRunner();
        } catch (RuntimeException | LinkageError failure) {
            hostDetachFailure = failureReason(failure);
        }
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        publishTerminalFailure(
                "Maintain native Flutter Canvas attachment",
                Objects.requireNonNullElse(reason, "Native child-window attachment failed")
                        + diagnosticSuffix(output)
                        + (hostDetachFailure == null
                                ? ""
                                : " Native host detach also failed: "
                                + hostDetachFailure + "."));
    }

    private void surfaceMetricsChanged(NativeCanvasSurfaceMetrics metrics) {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        NativeCanvasSurfaceMetrics next = Objects.requireNonNull(metrics, "metrics");
        boolean changed = !next.equals(nativeSurfaceMetrics);
        nativeSurfaceMetrics = next;
        if (changed) {
            invalidateSurfacePresentationForHostTransition(next);
        }
        if (process == null) {
            return;
        }
        if (!confirmCurrentSurfacePresentationIfExact()
                && process != null
                && lastPublishedStatus != null
                && lastPublishedStatus.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.RUNNING) {
            publishRunningStatus(surfaceEpoch, process);
        }
    }

    /**
     * Fences input before an asynchronously resized native child can expose a
     * geometry that is newer than the last runner-confirmed Flutter frame.
     */
    private void invalidateSurfacePresentationForHostTransition(
            NativeCanvasSurfaceMetrics target) {
        CanvasLayoutKey layout = currentLayout;
        confirmedSurfaceLayout = null;
        confirmedSurfaceMetrics = null;
        if (layout == null) {
            minimumSurfaceLayoutSequence = 0;
            return;
        }
        CanvasRunnerRuntimeEvent.Presented latest = latestRunnerPresentation;
        boolean latestAlreadyDescribesTarget = latest != null
                && latest.layoutKey().equals(layout)
                && surfaceMetricsEqual(latest.metrics(), target);
        if (!latestAlreadyDescribesTarget) {
            if (layout.layoutSequence() == CanvasWireProtocol.MAX_SEQUENCE) {
                failCurrentProcess(
                        "Resize native Flutter Canvas",
                        "the runner exhausted the bounded layout sequence before "
                        + "it could confirm the new physical surface");
                return;
            }
            minimumSurfaceLayoutSequence = Math.max(
                    minimumSurfaceLayoutSequence,
                    layout.layoutSequence() + 1);
        }
        resetWidgetMovePreviewState(false);
        resetInteractionFenceTransport();
        advanceInteractionFence();
        if (interactionFenceAccepting) {
            beginInteractionBarrier(Optional.of(layout));
        } else {
            timeOutInteractionBarrier(Optional.of(layout));
        }
    }

    private void failCurrentProcess(String operation, String reason) {
        requireEventDispatchThread();
        generation++;
        launchPending = false;
        stopAttachTimer();
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        abortProcessChannel();
        clearPresentationAuthority();
        restartAfterRetirement = false;
        String hostDetachFailure = null;
        try {
            host.detachRunner();
        } catch (RuntimeException | LinkageError failure) {
            hostDetachFailure = failureReason(failure);
        }
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        String closeSuffix = hostDetachFailure == null
                ? ""
                : " Native host detach also failed: " + hostDetachFailure + ".";
        publishTerminalFailure(
                operation, reason + diagnosticSuffix(output) + closeSuffix);
    }

    private void ensureProcessChannel() {
        requireEventDispatchThread();
        if (closed || process == null || pendingPresentation == null
                || processChannel != null) {
            return;
        }
        CanvasSessionId sessionId = CanvasSessionId.random();
        long channelGeneration = generation;
        resetInteractionFenceEpoch();
        canvasSessionId = sessionId;
        nextPresentationSequence = 0;
        intentReplayGate = new CanvasIntentReplayGate(sessionId);
        CanvasRunnerProcessChannel created = new CanvasRunnerProcessChannel(
                process,
                sessionId,
                runtime.uiExecutor(),
                new CanvasRunnerProcessChannel.Listener() {
                    @Override
                    public void ready(CanvasEngineIdentity engineIdentity) {
                        processChannelReady(sessionId, engineIdentity);
                    }

                    @Override
                    public void presented(
                            CanvasRunnerRuntimeEvent.Presented presented) {
                        processChannelPresented(sessionId, presented);
                    }

                    @Override
                    public void selection(
                            io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey intentKey,
                            StableId widgetId) {
                        processChannelSelection(sessionId, intentKey, widgetId);
                    }

                    @Override
                    public void interaction(
                            CanvasRunnerRuntimeEvent.Interaction interaction) {
                        processChannelInteraction(sessionId, interaction);
                    }

                    @Override
                    public void interactionFenceApplied(
                            CanvasRunnerRuntimeEvent.InteractionFenceApplied
                                    applied) {
                        processChannelInteractionFenceApplied(
                                sessionId, applied);
                    }

                    @Override
                    public void paletteDrop(CanvasRunnerRuntimeEvent.PaletteDrop drop) {
                        processChannelPaletteDrop(sessionId, drop);
                    }

                    @Override
                    public void deleteSelection(
                            CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
                        processChannelDeleteSelection(sessionId, deletion);
                    }

                    @Override
                    public void textEditCommit(
                            CanvasRunnerRuntimeEvent.TextEditCommit commit) {
                        processChannelTextEditCommit(sessionId, commit);
                    }

                    @Override
                    public void viewportMetrics(CanvasViewportMetrics metrics) {
                        processChannelViewportMetrics(sessionId, metrics);
                    }

                    @Override
                    public void closed() {
                        processChannelClosed(sessionId, channelGeneration);
                    }

                    @Override
                    public void warning(String reason) {
                        LOGGER.log(
                                Level.WARNING,
                                "Native Flutter Canvas runner reported a non-fatal "
                                + "diagnostic for session {0}: {1}",
                                new Object[] {sessionId, reason});
                    }

                    @Override
                    public void failed(String reason) {
                        processChannelFailed(sessionId, reason);
                    }
                });
        processChannel = created;
        try {
            created.start();
        } catch (RuntimeException failure) {
            if (processChannel == created) {
                processChannel = null;
            }
            created.abort();
            failCurrentProcess(
                    "Start native Flutter Canvas protocol",
                    failureReason(failure));
        }
    }

    private void processChannelReady(
            CanvasSessionId sessionId,
            CanvasEngineIdentity engineIdentity) {
        requireEventDispatchThread();
        if (!isCurrentChannel(sessionId)) {
            return;
        }
        canvasEngineIdentity = Objects.requireNonNull(engineIdentity, "engineIdentity");
        CanvasRunnerProcessChannel channel = processChannel;
        paletteCatalogInsertDropAvailable = channel != null
                && channel.supports(CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1)
                && channel.supports(CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1);
        publishPendingPresentation();
    }

    private void publishPendingPresentation() {
        requireEventDispatchThread();
        PendingPresentation pending = pendingPresentation;
        CanvasRunnerProcessChannel channel = processChannel;
        if (pending == null || channel == null || !channel.isReady()
                || canvasEngineIdentity == null || !host.isRunnerAttached()) {
            return;
        }
        StableId documentId = pending.snapshot().document().documentId();
        if (presentationGate == null || !documentId.equals(presentationDocumentId)) {
            if (presentationGate != null) {
                presentationGate.close();
            }
            presentationGate = new CanvasPresentationGate(
                    channel.sessionId(), documentId, nextPresentationSequence);
            presentationDocumentId = documentId;
        }
        final CanvasRenderRequest request;
        try {
            request = presentationGate.present(
                    CanvasPreviewProfileResolver.resolve(
                            pending.previewMode(),
                            pending.targetPlatform(),
                            pending.snapshot().document().canvas(),
                            canvasEngineIdentity,
                            pending.resolvedTheme(),
                            pending.orientationOverride()),
                    pending.snapshot(),
                    pending.imageResources());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            publishFailure(
                    "Resolve native Flutter Canvas presentation",
                    failureReason(failure));
            return;
        }
        nextPresentationSequence = request.revisionKey().presentationSequence() + 1;
        currentRenderRequest = request;
        currentLayout = null;
        latestRunnerPresentation = null;
        confirmedSurfaceLayout = null;
        confirmedSurfaceMetrics = null;
        renderedRevision = null;
        minimumSurfaceLayoutSequence = 0;
        currentWidgetIds = pending.widgetIds();
        beginInteractionBarrier(Optional.empty());
        resetViewportCommandFence();
        try {
            channel.expectPresentation(request.revisionKey());
        } catch (RuntimeException failure) {
            publishFailure(
                    "Fence native Flutter Canvas presentation",
                    failureReason(failure));
            return;
        }
        long publication = pending.publicationGeneration();
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                "Rendering native Flutter Canvas...",
                "Publishing validated .fd revision "
                + request.revisionKey().logicalRevisionId() + " as "
                + request.renderProfile().previewMode().name().toLowerCase(
                        java.util.Locale.ROOT)
                + " / " + request.renderProfile().targetPlatform().name().toLowerCase(
                        java.util.Locale.ROOT)
                + " viewport " + (int) request.renderProfile().viewport().logicalWidth()
                + "×" + (int) request.renderProfile().viewport().logicalHeight()
                + " logical pixels."));
        try {
            runtime.launchExecutor().execute(() -> encodePresentation(
                    publication, channel, request));
        } catch (RuntimeException | LinkageError failure) {
            publishFailure(
                    "Schedule native Flutter Canvas model encoding",
                    failureReason(failure));
        }
    }

    private void encodePresentation(
            long publication,
            CanvasRunnerProcessChannel channel,
            CanvasRenderRequest request) {
        final byte[] payload;
        try {
            payload = payloadCodec.encode(request);
        } catch (Exception failure) {
            deliverOnUi(() -> presentationFailed(
                    publication,
                    channel,
                    "Project validated .fd model for native Canvas",
                    failureReason(failure)));
            return;
        }
        deliverOnUi(() -> sendEncodedPresentation(
                publication, channel, request, payload));
    }

    private void sendEncodedPresentation(
            long publication,
            CanvasRunnerProcessChannel channel,
            CanvasRenderRequest request,
            byte[] payload) {
        requireEventDispatchThread();
        if (!isCurrentPublication(publication, channel, request)) {
            return;
        }
        try {
            boolean sent = channel.present(request, payload);
            if (!sent) {
                presentationFailed(
                        publication,
                        channel,
                        "Send validated model to native Flutter Canvas",
                        "the runner protocol is not ready or the publication is stale");
            } else {
                sendViewportPresentationIfPossible();
            }
        } catch (RuntimeException | LinkageError failure) {
            presentationFailed(
                    publication,
                    channel,
                    "Queue native Flutter Canvas model publication",
                    failureReason(failure));
        }
    }

    private void presentationFailed(
            long publication,
            CanvasRunnerProcessChannel channel,
            String operation,
            String reason) {
        requireEventDispatchThread();
        if (pendingPresentation == null
                || pendingPresentation.publicationGeneration() != publication
                || processChannel != channel) {
            return;
        }
        publishFailure(operation, reason);
    }

    private void processChannelPresented(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.Presented presented) {
        requireEventDispatchThread();
        if (!isCurrentChannel(sessionId) || presentationGate == null) {
            return;
        }
        Objects.requireNonNull(presented, "presented");
        CanvasLayoutKey layoutKey = presented.layoutKey();
        boolean initialPresentation = currentLayout == null;
        CanvasAdmission admission = initialPresentation
                ? presentationGate.admitPresentation(layoutKey.frameKey(), layoutKey)
                : presentationGate.admitLayout(layoutKey);
        if (admission != CanvasAdmission.ACCEPTED) {
            return;
        }
        if (!initialPresentation && !layoutKey.equals(currentLayout)) {
            // Flutter invalidates layout-bound overlays before publishing the
            // replacement layout. Do not retain a host-side placement tied to
            // the superseded geometry.
            resetWidgetMovePreviewState(false);
        }
        currentLayout = layoutKey;
        latestRunnerPresentation = presented;
        beginInteractionBarrier(Optional.of(layoutKey));
        interactionFenceRetryAttempts = 0;
        if (!confirmCurrentSurfacePresentationIfExact()) {
            if (process != null) {
                publishRunningStatus(surfaceEpoch, process);
            }
            return;
        }
    }

    /**
     * Completes presentation authority only when Flutter's post-frame physical
     * metrics match the latest provider-owned host target. Intermediate live
     * resize frames still advance the contiguous layout gate, but can never
     * reopen input or mutation authority.
     */
    private boolean confirmCurrentSurfacePresentationIfExact() {
        CanvasRunnerRuntimeEvent.Presented presented = latestRunnerPresentation;
        NativeCanvasSurfaceMetrics target = nativeSurfaceMetrics;
        CanvasLayoutKey layout = currentLayout;
        if (presented == null
                || target == null
                || layout == null
                || !presented.layoutKey().equals(layout)
                || layout.layoutSequence() < minimumSurfaceLayoutSequence
                || !surfaceMetricsEqual(presented.metrics(), target)) {
            return false;
        }
        if (layout.equals(confirmedSurfaceLayout)
                && presented.metrics().equals(confirmedSurfaceMetrics)) {
            return true;
        }
        confirmedSurfaceLayout = layout;
        confirmedSurfaceMetrics = presented.metrics();
        minimumSurfaceLayoutSequence = layout.layoutSequence();
        sendInteractionFenceIfPossible();
        CanvasRenderRequest request = currentRenderRequest;
        StableId selection = desiredSelection;
        CanvasRunnerProcessChannel channel = processChannel;
        boolean firstConfirmationForRevision = request != null
                && !request.revisionKey().equals(renderedRevision);
        if (firstConfirmationForRevision
                && selection != null
                && currentWidgetIds.contains(selection)
                && channel != null) {
            try {
                channel.select(layout, selection);
            } catch (RuntimeException failure) {
                failCurrentProcess(
                        "Restore selection in native Flutter Canvas",
                        failureReason(failure));
                return false;
            }
        }
        if (request != null) {
            renderedRevision = request.revisionKey();
            publishRenderedStatus(request);
        }
        sendViewportPresentationIfPossible();
        return true;
    }

    private static boolean surfaceMetricsEqual(
            CanvasSurfaceMetrics runner,
            NativeCanvasSurfaceMetrics host) {
        return runner.physicalWidth() == host.width()
                && runner.physicalHeight() == host.height()
                && runner.devicePixelRatioMicros()
                        == host.devicePixelRatioMicros();
    }

    private void publishRenderedStatus(CanvasRenderRequest request) {
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                "Native Flutter Canvas rendered.",
                "Presented " + request.renderProfile().previewMode().name()
                        .toLowerCase(java.util.Locale.ROOT)
                + " / " + request.renderProfile().targetPlatform().name()
                        .toLowerCase(java.util.Locale.ROOT)
                 + " viewport "
                 + (int) request.renderProfile().viewport().logicalWidth()
                 + "×" + (int) request.renderProfile().viewport().logicalHeight()
                 + " with read-only widget selection enabled."
                 + surfaceMetricsDetail(),
                true));
    }

    private void processChannelViewportMetrics(
            CanvasSessionId sessionId,
            CanvasViewportMetrics metrics) {
        requireEventDispatchThread();
        Objects.requireNonNull(metrics, "metrics");
        CanvasRenderRequest request = currentRenderRequest;
        if (!isCurrentChannel(sessionId)
                || request == null
                || !request.revisionKey().equals(metrics.revisionKey())) {
            return;
        }
        long commandSequence = metrics.commandSequence();
        if (commandSequence < lastViewportCommandSequence) {
            // A delayed metric for an older transform must never overwrite a
            // newer toolbar command.
            return;
        }
        if (commandSequence > lastViewportCommandSequence) {
            failCurrentProcess(
                    "Confirm native Flutter Canvas zoom and scroll",
                    "the runner acknowledged unseen viewport command sequence "
                    + commandSequence + " while the latest host command is "
                    + lastViewportCommandSequence);
            return;
        }
        if (viewportCommandPending) {
            if (!metrics.presentation().equals(lastViewportCommand)) {
                failCurrentProcess(
                        "Confirm native Flutter Canvas zoom and scroll",
                        "the runner acknowledged viewport command sequence "
                        + commandSequence + " with a different presentation");
                return;
            }
        }
        if (currentLayout == null
                || !currentLayout.equals(confirmedSurfaceLayout)) {
            if (viewportCommandPending) {
                viewportAckDeferredBySurfaceTransition = true;
            }
            return;
        }
        if (viewportAckDeferredBySurfaceTransition) {
            // The runner acknowledged this sequence while the physical surface
            // was not confirmed. Until a replacement command is actually
            // accepted by the bounded outbound queue, a duplicate of that old
            // acknowledgement must not acquire post-transition authority.
            return;
        }
        if (viewportCommandPending) {
            viewportCommandPending = false;
            viewportAckDeferredBySurfaceTransition = false;
        }
        if (viewportCommandRetryPending) {
            // The exact older command may now be acknowledged, but the queue
            // has not accepted the latest desired transform yet. Do not revert
            // the toolbar while its bounded retry is pending.
            return;
        }
        desiredViewportPresentation = metrics.presentation();
        lastViewportCommandRevision = metrics.revisionKey();
        lastViewportCommand = metrics.presentation();
        viewportMetricsListener.accept(metrics);
    }

    private void sendViewportPresentationIfPossible() {
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasRenderRequest request = currentRenderRequest;
        if (closed
                || channel == null
                || request == null
                || !channel.supports(CanvasWireCapability.VIEWPORT_PRESENTATION_V1)) {
            return;
        }
        CanvasRevisionKey revision = request.revisionKey();
        boolean replayDeferredAck = viewportAckDeferredBySurfaceTransition;
        if (revision.equals(lastViewportCommandRevision)
                && desiredViewportPresentation.equals(lastViewportCommand)
                && !replayDeferredAck) {
            viewportCommandRetryPending = false;
            viewportCommandRetryTimer.stop();
            return;
        }
        if (nextViewportCommandSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            failCurrentProcess(
                    "Change native Flutter Canvas zoom and scroll",
                    "the per-presentation viewport command sequence was exhausted");
            return;
        }
        long commandSequence = nextViewportCommandSequence;
        try {
            if (channel.viewport(
                    revision,
                    commandSequence,
                    desiredViewportPresentation)) {
                if (replayDeferredAck) {
                    viewportAckDeferredBySurfaceTransition = false;
                }
                lastViewportCommandRevision = revision;
                lastViewportCommand = desiredViewportPresentation;
                lastViewportCommandSequence = commandSequence;
                nextViewportCommandSequence = commandSequence + 1;
                viewportCommandPending = true;
                viewportCommandRetryPending = false;
                viewportCommandRetryAttempts = 0;
                viewportCommandRetryTimer.stop();
            } else {
                scheduleViewportPresentationRetry(channel, request);
            }
        } catch (RuntimeException failure) {
            failCurrentProcess(
                    "Change native Flutter Canvas zoom and scroll",
                    failureReason(failure));
        }
    }

    private void scheduleViewportPresentationRetry(
            CanvasRunnerProcessChannel channel,
            CanvasRenderRequest request) {
        if (closed
                || processChannel != channel
                || currentRenderRequest != request) {
            return;
        }
        viewportCommandRetryAttempts++;
        if (viewportCommandRetryAttempts >= MAX_VIEWPORT_RETRY_ATTEMPTS) {
            viewportCommandRetryPending = false;
            failCurrentProcess(
                    "Queue native Flutter Canvas zoom and scroll",
                    "the bounded protocol queue rejected the latest viewport "
                    + "presentation " + viewportCommandRetryAttempts
                    + " consecutive times");
            return;
        }
        viewportCommandRetryPending = true;
        viewportCommandRetryTimer.restart();
    }

    private void retryViewportPresentation() {
        requireEventDispatchThread();
        if (!viewportCommandRetryPending) {
            return;
        }
        viewportCommandRetryPending = false;
        sendViewportPresentationIfPossible();
    }

    private void resetViewportCommandFence() {
        viewportCommandRetryTimer.stop();
        lastViewportCommandRevision = null;
        lastViewportCommand = null;
        nextViewportCommandSequence = 1;
        lastViewportCommandSequence = 0;
        viewportCommandPending = false;
        viewportAckDeferredBySurfaceTransition = false;
        viewportCommandRetryPending = false;
        viewportCommandRetryAttempts = 0;
    }

    private void processChannelSelection(
            CanvasSessionId sessionId,
            io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey intentKey,
            StableId widgetId) {
        requireEventDispatchThread();
        if (!isCurrentChannel(sessionId)
                || intentReplayGate == null) {
            return;
        }
        CanvasIntentAdmission replay = intentReplayGate.consume(
                intentKey,
                CanvasIntentReplayPolicy.IDEMPOTENT);
        if (!replay.firstDelivery()
                || presentationGate == null
                || currentLayout == null
                || !interactionInputEnabledFor(intentKey.layoutKey())
                || !currentWidgetIds.contains(widgetId)
                || presentationGate.admitSelection(intentKey.layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        desiredSelection = widgetId;
        selectionListener.accept(widgetId);
    }

    private void processChannelInteraction(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.Interaction interaction) {
        requireEventDispatchThread();
        Objects.requireNonNull(interaction, "interaction");
        if (!isCurrentChannel(sessionId)) {
            return;
        }
        CanvasIntentReplayGate replayGate = intentReplayGate;
        CanvasLayoutKey layout = currentLayout;
        if (replayGate == null || layout == null) {
            return;
        }
        CanvasIntentAdmission admission = replayGate.admit(
                layout,
                interaction.intentKey(),
                CanvasIntentReplayPolicy.ONE_SHOT);
        if (!admission.firstDelivery()
                || interaction.interactionFenceSequence()
                        != interactionFenceSequence
                || !interactionFenceAccepting
                || !interactionInputEnabledFor(
                        interaction.intentKey().layoutKey())
                || !requestedVisible
                || presentationGate == null
                || presentationGate.admitSelection(
                        interaction.intentKey().layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        interactionListener.run();
    }

    private void processChannelInteractionFenceApplied(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.InteractionFenceApplied applied) {
        requireEventDispatchThread();
        Objects.requireNonNull(applied, "applied");
        CanvasLayoutKey layout = currentLayout;
        if (!isCurrentChannel(sessionId)
                || layout == null
                || !layout.equals(confirmedSurfaceLayout)
                || (interactionBarrierState.phase()
                                != InteractionBarrierPhase.SYNCHRONIZING
                        && interactionBarrierState.phase()
                                != InteractionBarrierPhase.TIMED_OUT)
                || pendingInteractionFenceLayout == null
                || !layout.equals(pendingInteractionFenceLayout)
                || !layout.equals(applied.layoutKey())
                || pendingInteractionFenceSequence != interactionFenceSequence
                || applied.interactionFenceSequence()
                        != interactionFenceSequence
                || presentationGate == null
                || presentationGate.admitSelection(applied.layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        synchronizeInteractionBarrier(layout);
    }

    private void processChannelPaletteDrop(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.PaletteDrop drop) {
        requireEventDispatchThread();
        Objects.requireNonNull(drop, "drop");
        if (!isCurrentChannel(sessionId)) {
            return;
        }
        Optional<WidgetTypeId> authoritativeWidgetType = Optional.empty();
        try {
            authoritativeWidgetType = Objects.requireNonNull(
                    paletteDropTokenResolver.apply(drop.token()),
                    "paletteDropTokenResolver result");
        } catch (RuntimeException | LinkageError failure) {
            // The replay sequence is still consumed below so one bad token
            // cannot wedge all later interaction intents for this session.
        }
        CanvasIntentReplayGate replayGate = intentReplayGate;
        if (replayGate == null) {
            return;
        }
        CanvasIntentAdmission replay = replayGate.consume(
                drop.intentKey(), CanvasIntentReplayPolicy.ONE_SHOT);
        if (authoritativeWidgetType.isEmpty()
                || !replay.firstDelivery()
                || !requestedVisible
                || presentationGate == null
                || currentLayout == null
                || !interactionInputEnabledFor(drop.intentKey().layoutKey())
                || !currentWidgetIds.contains(drop.parentWidgetId())
                || presentationGate.admitSelection(drop.intentKey().layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        paletteDropListener.accept(new AdmittedPaletteDrop(
                authoritativeWidgetType.orElseThrow(), drop));
    }

    private void processChannelDeleteSelection(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.DeleteSelection deletion) {
        requireEventDispatchThread();
        Objects.requireNonNull(deletion, "deletion");
        if (!isCurrentChannel(sessionId)) {
            return;
        }
        CanvasIntentReplayGate replayGate = intentReplayGate;
        if (replayGate == null) {
            return;
        }
        CanvasIntentAdmission replay = replayGate.consume(
                deletion.intentKey(), CanvasIntentReplayPolicy.ONE_SHOT);
        if (!replay.firstDelivery()
                || !requestedVisible
                || presentationGate == null
                || currentLayout == null
                || !interactionInputEnabledFor(
                        deletion.intentKey().layoutKey())
                || currentRenderRequest == null
                || !currentWidgetIds.contains(deletion.widgetId())
                || deletion.widgetId().equals(currentRenderRequest.snapshot()
                        .document().root().id())
                || !Objects.equals(desiredSelection, deletion.widgetId())
                || presentationGate.admitSelection(
                        deletion.intentKey().layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        deleteSelectionListener.accept(deletion);
    }

    private void processChannelTextEditCommit(
            CanvasSessionId sessionId,
            CanvasRunnerRuntimeEvent.TextEditCommit commit) {
        requireEventDispatchThread();
        Objects.requireNonNull(commit, "commit");
        if (!isCurrentChannel(sessionId)) {
            return;
        }
        CanvasIntentReplayGate replayGate = intentReplayGate;
        CanvasRenderRequest request = currentRenderRequest;
        CanvasLayoutKey layout = currentLayout;
        if (replayGate == null || request == null || layout == null) {
            return;
        }
        CanvasIntentAdmission replay = replayGate.consume(
                commit.intentKey(), CanvasIntentReplayPolicy.ONE_SHOT);
        WidgetNode widget = findWidget(
                request.snapshot().document().root(), commit.widgetId());
        if (!replay.firstDelivery()
                || commit.interactionFenceSequence()
                        != interactionFenceSequence
                || !requestedVisible
                || presentationGate == null
                || !interactionInputEnabledFor(
                        commit.intentKey().layoutKey())
                || widget == null
                || !widget.type().equals(TEXT_WIDGET_TYPE)
                || !Objects.equals(desiredSelection, commit.widgetId())
                || presentationGate.admitSelection(
                        commit.intentKey().layoutKey())
                        != CanvasAdmission.ACCEPTED) {
            return;
        }
        textEditCommitListener.accept(commit);
    }

    private static WidgetNode findWidget(WidgetNode root, StableId widgetId) {
        if (root.id().equals(widgetId)) {
            return root;
        }
        for (WidgetSlot slot : root.slots().values()) {
            List<WidgetNode> children = switch (slot) {
                case WidgetSlot.SingleSlot single -> single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            };
            for (WidgetNode child : children) {
                WidgetNode found = findWidget(child, widgetId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean interactionInputEnabledFor(CanvasLayoutKey layout) {
        return interactionFenceAccepting
                && layout.equals(confirmedSurfaceLayout)
                && interactionBarrierState.inputEnabled()
                && interactionBarrierState.fenceSequence()
                        == interactionFenceSequence
                && interactionBarrierState.layoutKey().filter(layout::equals)
                        .isPresent()
                && appliedInteractionFenceSequence
                        == interactionFenceSequence
                && layout.equals(appliedInteractionFenceLayout);
    }

    @Override
    public boolean paletteCatalogInsertDropAvailable() {
        requireEventDispatchThread();
        return paletteCatalogInsertDropAvailable;
    }

    /**
     * Binds one opaque Palette token to the exact reviewed catalog definition
     * for Flutter-side trait-aware hover. Mutation authority remains entirely
     * in the Java token consumer and canonical drop planner.
     */
    @Override
    public boolean authorizePaletteDragSource(
            String token,
            WidgetTypeId widgetType) {
        requireEventDispatchThread();
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(widgetType, "widgetType");
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        CanvasRenderRequest request = currentRenderRequest;
        if (closed
                || !requestedVisible
                || !paletteCatalogInsertDropAvailable
                || channel == null
                || layout == null
                || request == null
                || !channel.supports(
                        CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1)) {
            return false;
        }
        var definition = request.snapshot().catalog().find(widgetType)
                .filter(candidate -> BuiltInWidgetCapabilityCatalog.supports(
                        candidate, WidgetCapability.DND))
                .orElse(null);
        if (definition == null) {
            return false;
        }
        return channel.authorizePaletteDragSource(
                layout,
                token,
                definition.typeId(),
                definition.traits());
    }

    /** True only when the exact isolated runner negotiated move preview. */
    boolean widgetMovePreviewAvailable() {
        requireEventDispatchThread();
        CanvasRunnerProcessChannel channel = processChannel;
        return !closed
                && requestedVisible
                && currentLayout != null
                && channel != null
                && channel.supports(
                        CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1);
    }

    /**
     * Shows one host-planned move placement in the native Canvas. The method
     * never plans or applies a mutation; callers must first admit the target
     * through the canonical catalog compatibility matrix.
     */
    @Override
    public void showWidgetMovePreview(
            StableId sourceWidgetId,
            WidgetPlacement destination) {
        requireEventDispatchThread();
        Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
        Objects.requireNonNull(destination, "destination");
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        CanvasRenderRequest request = currentRenderRequest;
        if (closed
                || !requestedVisible
                || channel == null
                || layout == null
                || request == null
                || !channel.supports(
                        CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)
                || !currentWidgetIds.contains(sourceWidgetId)
                || !currentWidgetIds.contains(destination.parentId())
                || sourceWidgetId.equals(destination.parentId())
                || sourceWidgetId.equals(
                        request.snapshot().document().root().id())
                || destination.index() < 0
                || destination.index() > WidgetSlot.MAX_LIST_CHILDREN) {
            return;
        }
        WidgetMovePreviewPlacement next = new WidgetMovePreviewPlacement(
                layout,
                sourceWidgetId,
                destination);
        WidgetMovePreviewShow command = new WidgetMovePreviewShow(next);
        if (command.equals(desiredWidgetMovePreviewCommand)) {
            return;
        }
        if (next.equals(widgetMovePreviewPlacement)) {
            // A previously rejected, newer hover can be cancelled without a
            // wire round-trip when the runner still shows this exact target.
            resetWidgetMovePreviewRetry();
            return;
        }
        desiredWidgetMovePreviewCommand = command;
        widgetMovePreviewRetryAttempts = 0;
        widgetMovePreviewRetryPending = false;
        widgetMovePreviewRetryTimer.stop();
        sendDesiredWidgetMovePreviewIfPossible();
    }

    /**
     * Clears the last successfully queued move placement. Repeated cleanup is
     * idempotent, which lets drag exit, cancel, drop and view teardown all call
     * this same edge.
     */
    @Override
    public void clearWidgetMovePreview() {
        requireEventDispatchThread();
        WidgetMovePreviewPlacement active = widgetMovePreviewPlacement;
        WidgetMovePreviewCommand desired = desiredWidgetMovePreviewCommand;
        if (active == null && !(desired instanceof WidgetMovePreviewShow)) {
            return;
        }
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        CanvasLayoutKey previewLayout = active != null
                ? active.layout()
                : desired.layout();
        if (closed
                || channel == null
                || layout == null
                || !layout.equals(previewLayout)
                || !channel.supports(
                        CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)) {
            resetWidgetMovePreviewState(false);
            return;
        }
        if (active == null) {
            // The latest show was rejected before queue admission, so there is
            // no runner overlay to clear. Cancelling the coalesced show is
            // sufficient and must not allow it to reappear later.
            resetWidgetMovePreviewRetry();
            return;
        }
        WidgetMovePreviewClear command = new WidgetMovePreviewClear(layout);
        if (command.equals(desiredWidgetMovePreviewCommand)) {
            return;
        }
        desiredWidgetMovePreviewCommand = command;
        widgetMovePreviewRetryAttempts = 0;
        widgetMovePreviewRetryPending = false;
        widgetMovePreviewRetryTimer.stop();
        sendDesiredWidgetMovePreviewIfPossible();
    }

    /**
     * Sends the latest coalesced show/clear command. Queue rejection retains
     * only this last command and retries it on the EDT; an exhausted retry
     * closes the runner so a stale overlay cannot survive drag cleanup.
     */
    private void sendDesiredWidgetMovePreviewIfPossible() {
        requireEventDispatchThread();
        WidgetMovePreviewCommand desired = desiredWidgetMovePreviewCommand;
        if (desired == null) {
            resetWidgetMovePreviewRetry();
            return;
        }
        CanvasRunnerProcessChannel channel = processChannel;
        CanvasLayoutKey layout = currentLayout;
        if (closed
                || (!requestedVisible
                        && desired instanceof WidgetMovePreviewShow)
                || channel == null
                || layout == null
                || !layout.equals(desired.layout())
                || !channel.supports(
                        CanvasWireCapability.WIDGET_MOVE_PREVIEW_V1)) {
            resetWidgetMovePreviewState(false);
            return;
        }
        if (nextWidgetMovePreviewSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            resetWidgetMovePreviewState(false);
            failCurrentProcess(
                    "Update native Flutter Canvas widget move preview",
                    "the per-presentation widget move preview sequence was exhausted");
            return;
        }
        long sequence = nextWidgetMovePreviewSequence;
        final boolean admitted;
        try {
            if (desired instanceof WidgetMovePreviewShow show) {
                WidgetMovePreviewPlacement placement = show.placement();
                admitted = channel.previewWidgetMove(
                        placement.layout(),
                        sequence,
                        placement.sourceWidgetId(),
                        placement.destination().parentId(),
                        placement.destination().slotName(),
                        placement.destination().index());
            } else {
                admitted = channel.clearWidgetMovePreview(
                        desired.layout(), sequence);
            }
        } catch (RuntimeException failure) {
            resetWidgetMovePreviewState(false);
            failCurrentProcess(
                    "Update native Flutter Canvas widget move preview",
                    failureReason(failure));
            return;
        }
        if (!admitted) {
            scheduleWidgetMovePreviewRetry(channel, layout);
            return;
        }
        nextWidgetMovePreviewSequence = sequence + 1;
        widgetMovePreviewPlacement = desired instanceof WidgetMovePreviewShow show
                ? show.placement()
                : null;
        desiredWidgetMovePreviewCommand = null;
        widgetMovePreviewRetryAttempts = 0;
        widgetMovePreviewRetryPending = false;
        widgetMovePreviewRetryTimer.stop();
    }

    private void scheduleWidgetMovePreviewRetry(
            CanvasRunnerProcessChannel channel,
            CanvasLayoutKey layout) {
        if (closed
                || processChannel != channel
                || currentLayout == null
                || !currentLayout.equals(layout)
                || desiredWidgetMovePreviewCommand == null) {
            return;
        }
        widgetMovePreviewRetryAttempts++;
        if (widgetMovePreviewRetryAttempts
                >= MAX_WIDGET_MOVE_PREVIEW_RETRY_ATTEMPTS) {
            resetWidgetMovePreviewState(false);
            failCurrentProcess(
                    "Queue native Flutter Canvas widget move preview",
                    "the bounded protocol queue rejected the latest widget move "
                    + "preview " + widgetMovePreviewRetryAttempts
                    + " consecutive times");
            return;
        }
        widgetMovePreviewRetryPending = true;
        widgetMovePreviewRetryTimer.restart();
    }

    private void retryWidgetMovePreview() {
        requireEventDispatchThread();
        if (!widgetMovePreviewRetryPending) {
            return;
        }
        widgetMovePreviewRetryPending = false;
        sendDesiredWidgetMovePreviewIfPossible();
    }

    private void resetWidgetMovePreviewRetry() {
        desiredWidgetMovePreviewCommand = null;
        widgetMovePreviewRetryPending = false;
        widgetMovePreviewRetryAttempts = 0;
        widgetMovePreviewRetryTimer.stop();
    }

    private void resetWidgetMovePreviewState(boolean resetSequence) {
        resetWidgetMovePreviewRetry();
        widgetMovePreviewPlacement = null;
        if (resetSequence) {
            nextWidgetMovePreviewSequence = 1;
        }
    }

    private void processChannelClosed(
            CanvasSessionId sessionId,
            long channelGeneration) {
        requireEventDispatchThread();
        if (!closed || channelGeneration != closingProcessGeneration
                || closingCanvasSessionId == null
                || !closingCanvasSessionId.equals(sessionId)) {
            return;
        }
        runnerCloseAcknowledged = true;
        if (closeExitObserved) {
            // Process.onExit and the protocol listener are independent EDT
            // publications. If physical exit won the race, correct the final
            // status once the already-authenticated acknowledgement arrives.
            closeAckDrainPending = false;
            stopCloseTimer();
            abortProcessChannel();
            maybePublishStopped(true);
        }
    }

    private void processChannelFailed(CanvasSessionId sessionId, String reason) {
        requireEventDispatchThread();
        if (isCurrentChannel(sessionId)) {
            failCurrentProcess("Run native Flutter Canvas protocol", reason);
        }
    }

    private boolean isCurrentChannel(CanvasSessionId sessionId) {
        return !closed
                && processChannel != null
                && canvasSessionId != null
                && canvasSessionId.equals(sessionId)
                && processChannel.sessionId().equals(sessionId);
    }

    private boolean isCurrentPublication(
            long publication,
            CanvasRunnerProcessChannel channel,
            CanvasRenderRequest request) {
        return !closed
                && processChannel == channel
                && pendingPresentation != null
                && pendingPresentation.publicationGeneration() == publication
                && currentRenderRequest == request;
    }

    private void clearPresentationAuthority() {
        currentRenderRequest = null;
        currentLayout = null;
        latestRunnerPresentation = null;
        confirmedSurfaceLayout = null;
        confirmedSurfaceMetrics = null;
        renderedRevision = null;
        minimumSurfaceLayoutSequence = 0;
        currentWidgetIds = Set.of();
        paletteCatalogInsertDropAvailable = false;
        resetWidgetMovePreviewState(true);
        resetViewportCommandFence();
        resetInteractionFenceTransport();
        deactivateInteractionBarrier();
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

    private void resetInteractionFenceTransport() {
        interactionFenceRetryTimer.stop();
        interactionFenceRetryPending = false;
        interactionFenceRetryAttempts = 0;
        lastInteractionFenceLayout = null;
        lastInteractionFenceSequence = -1;
    }

    /** Starts an independent interaction epoch for a fresh protocol session. */
    private void resetInteractionFenceEpoch() {
        resetInteractionFenceTransport();
        interactionFenceAckTimer.stop();
        interactionFenceSequence = 0;
        interactionFenceAccepting = true;
        pendingInteractionFenceLayout = null;
        pendingInteractionFenceSequence = -1;
        appliedInteractionFenceLayout = null;
        appliedInteractionFenceSequence = -1;
    }

    private void abortProcessChannel() {
        CanvasRunnerProcessChannel current = processChannel;
        processChannel = null;
        canvasSessionId = null;
        canvasEngineIdentity = null;
        paletteCatalogInsertDropAvailable = false;
        resetWidgetMovePreviewState(true);
        nextPresentationSequence = 0;
        if (current != null) {
            try {
                current.abort();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.WARNING,
                        "Native Flutter Canvas protocol transport cleanup failed",
                        failure);
            }
        }
    }

    private void deliverOnUi(Runnable task) {
        try {
            runtime.uiExecutor().execute(task);
        } catch (RuntimeException | LinkageError ignored) {
            // A closing NetBeans window owns cancellation of this publication.
        }
    }

    private static Set<StableId> collectWidgetIds(WidgetNode root) {
        HashSet<StableId> result = new HashSet<>();
        java.util.ArrayDeque<WidgetNode> pending = new java.util.ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            WidgetNode widget = pending.removeFirst();
            if (!result.add(widget.id())) {
                throw new IllegalArgumentException(
                        "Canvas widget ids must be unique: " + widget.id());
            }
            for (WidgetSlot slot : widget.slots().values()) {
                switch (slot) {
                    case WidgetSlot.SingleSlot single ->
                        single.child().ifPresent(pending::addLast);
                    case WidgetSlot.ListSlot list -> pending.addAll(list.children());
                }
            }
        }
        return Set.copyOf(result);
    }

    private void publishTerminalFailure(String operation, String reason) {
        if (!closed) {
            restartAvailable = true;
        }
        String cleanupSuffix = retiringCleanupFailure == null
                ? ""
                : " Previous runner cleanup also failed: "
                + retiringCleanupFailure
                + ". Retry remains unavailable while that process is alive.";
        lastTerminalFailureStatus = failureStatus(operation, reason);
        publish(failureStatus(operation, reason + cleanupSuffix));
    }

    private void publishFailure(String operation, String reason) {
        publish(failureStatus(operation, reason));
    }

    private FlutterDesignerNativeCanvasStatus failureStatus(
            String operation,
            String reason) {
        return new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                operation + " failed.",
                "Target: " + platformTargetDescription()
                + ". Reason: " + reason);
    }

    private String platformTargetDescription() {
        try {
            String description = platformProvider.targetDescription();
            if (description != null && !description.isBlank()) {
                return description;
            }
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(Level.WARNING,
                    "Native Canvas provider target description failed", failure);
        }
        return "provider-owned native Flutter surface";
    }

    private void publish(FlutterDesignerNativeCanvasStatus status) {
        requireEventDispatchThread();
        lastPublishedStatus = Objects.requireNonNull(status, "status");
        try {
            listener.accept(status);
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(
                    Level.WARNING,
                    "Native Flutter Canvas status listener failed while publishing "
                    + status.stage(),
                    failure);
        }
    }

    private void stopAttachTimer() {
        if (attachPoll != null) {
            try {
                attachPoll.cancel();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.WARNING,
                        "Native Flutter Canvas attachment monitor cancellation failed",
                        failure);
            }
            attachPoll = null;
        }
    }

    private void stopCloseTimer() {
        if (closePoll != null) {
            try {
                closePoll.cancel();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.WARNING,
                        "Native Flutter Canvas close monitor cancellation failed",
                        failure);
            }
            closePoll = null;
        }
    }

    /**
     * Keeps the immutable runtime generation leased until the child process is
     * physically gone. A failed terminator may leak the lease conservatively,
     * but can never allow another build to delete files under a live process.
     */
    private void terminateAndRelease(
            Process current,
            CanvasRunnerRuntimeLease runtimeLease) {
        if (current == null) {
            closeLease(runtimeLease);
            return;
        }
        registerRetirement(current, runtimeLease);
        startRetiringTermination(current);
    }

    /**
     * Lets a runner that authenticated {@code runner.closed} complete its own
     * cleanup and exit naturally before bounded termination is considered.
     */
    private void retireAfterAuthenticatedClose(
            Process current,
            CanvasRunnerRuntimeLease runtimeLease) {
        if (current == null) {
            closeLease(runtimeLease);
            return;
        }
        registerRetirement(current, runtimeLease);
        final long deadline;
        try {
            deadline = runtime.nanoTime().getAsLong()
                    + CLOSE_HANDSHAKE_TIMEOUT.toNanos();
        } catch (RuntimeException | LinkageError failure) {
            escalateAuthenticatedRetirement(
                    current,
                    "The natural-exit grace clock failed: "
                            + failureReason(failure) + ".");
            return;
        }
        try {
            Cancellable poll = runtime.pollScheduler().schedule(
                    () -> pollAuthenticatedRetirement(current, deadline));
            Cancellable previous = gracefulRetirementPolls.put(current, poll);
            if (previous != null) {
                previous.cancel();
            }
            if (!retiringProcesses.contains(current)
                    && gracefulRetirementPolls.remove(current, poll)) {
                poll.cancel();
            }
        } catch (RuntimeException | LinkageError failure) {
            escalateAuthenticatedRetirement(
                    current,
                    "Natural-exit grace monitoring could not start: "
                            + failureReason(failure) + ".");
        }
    }

    private void registerRetirement(
            Process current,
            CanvasRunnerRuntimeLease runtimeLease) {
        retiringProcesses.add(current);
        try {
            current.onExit().whenComplete((ignored, failure) -> {
                if (failure == null) {
                    retirementFinished(current, runtimeLease);
                } else {
                    observePhysicalExitAfterOnExitFailure(
                            current,
                            runtimeLease,
                            () -> retirementFinished(current, null));
                }
            });
        } catch (RuntimeException | LinkageError ignored) {
            observePhysicalExitAfterOnExitFailure(
                    current,
                    runtimeLease,
                    () -> retirementFinished(current, null));
        }
    }

    private void pollAuthenticatedRetirement(Process retiring, long deadline) {
        requireEventDispatchThread();
        if (!retiringProcesses.contains(retiring)) {
            cancelGracefulRetirementPoll(retiring);
            return;
        }
        final boolean alive;
        try {
            alive = retiring.isAlive();
        } catch (RuntimeException | LinkageError failure) {
            escalateAuthenticatedRetirement(
                    retiring,
                    "Runner liveness could not be inspected during natural-exit grace: "
                            + failureReason(failure) + ".");
            return;
        }
        if (!alive) {
            // The Process.onExit authority releases the runtime lease and claim.
            return;
        }
        final long now;
        try {
            now = runtime.nanoTime().getAsLong();
        } catch (RuntimeException | LinkageError failure) {
            escalateAuthenticatedRetirement(
                    retiring,
                    "The natural-exit grace clock failed: "
                            + failureReason(failure) + ".");
            return;
        }
        if (now >= deadline) {
            escalateAuthenticatedRetirement(
                    retiring,
                    "The runner authenticated host.close but did not exit naturally "
                            + "within the " + CLOSE_HANDSHAKE_TIMEOUT.toSeconds()
                            + "-second grace; bounded termination was required.");
        }
    }

    private void escalateAuthenticatedRetirement(Process retiring, String detail) {
        requireEventDispatchThread();
        if (!retiringProcesses.contains(retiring)
                || retirementTerminations.contains(retiring)) {
            return;
        }
        cancelGracefulRetirementPoll(retiring);
        peerLossCloseEscalated = true;
        peerLossEscalationDetail = appendFailure(
                peerLossEscalationDetail, Objects.requireNonNull(detail, "detail"));
        if (closed) {
            closeEscalationScheduled = true;
            closingEscalationDetail = appendFailure(
                    closingEscalationDetail, detail);
        }
        startRetiringTermination(retiring);
    }

    private void startRetiringTermination(Process retiring) {
        if (!retiringProcesses.contains(retiring)
                || !retirementTerminations.add(retiring)) {
            return;
        }
        try {
            runtime.terminator().terminate(retiring).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    deliverOnUi(() -> retirementTerminationFailed(
                            retiring, unwrapCompletionFailure(failure)));
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            deliverOnUi(() -> retirementTerminationFailed(retiring, failure));
        }
    }

    private void cancelGracefulRetirementPoll(Process retiring) {
        Cancellable poll = gracefulRetirementPolls.remove(retiring);
        if (poll != null) {
            try {
                poll.cancel();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(
                        Level.WARNING,
                        "Native Flutter Canvas natural-exit monitor cancellation failed",
                        failure);
            }
        }
    }

    private void retirementFinished(
            Process retired,
            CanvasRunnerRuntimeLease runtimeLease) {
        if (!retiringProcesses.remove(retired)) {
            return;
        }
        cancelGracefulRetirementPoll(retired);
        retirementTerminations.remove(retired);
        closeLease(runtimeLease);
        deliverOnUi(this::reconcileOwnershipDrained);
    }

    private void reconcileOwnershipDrained() {
        requireEventDispatchThread();
        if (process != null || !retiringProcesses.isEmpty()
                || !inFlightLaunches.isEmpty()) {
            return;
        }
        if (closed) {
            maybePublishStopped(runnerCloseAcknowledged);
        } else if (restartAvailable) {
            publishRetryReadyAfterRetirement();
        } else if (restartAfterRetirement && requestedVisible) {
            restartAfterRetirement = false;
            retiringCleanupFailure = null;
            startIfPossible();
        } else if (pendingPresentation == null) {
            publishWithdrawnStatus(false);
        }
    }

    private void publishRetryReadyAfterRetirement() {
        FlutterDesignerNativeCanvasStatus terminal = lastTerminalFailureStatus;
        String priorDetail = terminal == null
                ? "The previous runner failed."
                : terminal.detail();
        String cleanupDetail = retiringCleanupFailure == null
                ? ""
                : " The bounded cleanup attempt also reported: "
                + retiringCleanupFailure + ".";
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                terminal == null
                        ? "Native Flutter Canvas can be retried."
                        : terminal.summary(),
                priorDetail + cleanupDetail
                + " Every previous runner PID has now physically exited; Retry "
                + "is available and will create a fresh process generation."));
    }

    private void retirementTerminationFailed(Process retiring, Throwable failure) {
        requireEventDispatchThread();
        if (!retiringProcesses.contains(retiring)) {
            return;
        }
        retiringCleanupFailure = failureReason(failure);
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                "Native Flutter Canvas cleanup failed.",
                "Target: " + processDescription(retiring)
                + ". Reason: bounded termination failed: "
                + retiringCleanupFailure
                + ". Retry remains unavailable while the old process is alive."));
    }

    private String retiringProcessDescription() {
        return retiringProcesses.stream()
                .map(FlutterDesignerNativeCanvasSession::processDescription)
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private void deliverPhysicalExit(
            long processGeneration,
            Process exited,
            CanvasRunnerRuntimeLease runtimeLease) {
        try {
            runtime.uiExecutor().execute(
                    () -> processExited(processGeneration, exited));
        } catch (RuntimeException | LinkageError failure) {
            // Physical exit is already known. Release the per-process claim
            // even when the UI callback can no longer be delivered.
            closeLease(runtimeLease);
            LOGGER.log(Level.WARNING,
                    "Native Flutter Canvas physical-exit callback could not reach the EDT",
                    failure);
        }
    }

    private void observeSessionExitAfterOnExitFailure(
            long processGeneration,
            Process current,
            CanvasRunnerRuntimeLease runtimeLease) {
        if (!isAliveConservatively(current)) {
            deliverPhysicalExit(processGeneration, current, runtimeLease);
            return;
        }
        try {
            Thread.ofVirtual().name("flutter-native-canvas-session-exit-fallback")
                    .start(() -> {
                        boolean interrupted = false;
                        try {
                            while (true) {
                                try {
                                    current.waitFor();
                                    deliverPhysicalExit(
                                            processGeneration,
                                            current,
                                            runtimeLease);
                                    return;
                                } catch (InterruptedException exception) {
                                    interrupted = true;
                                } catch (RuntimeException | LinkageError failure) {
                                    if (!isAliveConservatively(current)) {
                                        deliverPhysicalExit(
                                                processGeneration,
                                                current,
                                                runtimeLease);
                                    }
                                    return;
                                }
                            }
                        } finally {
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
        } catch (RuntimeException | LinkageError failure) {
            if (!isAliveConservatively(current)) {
                deliverPhysicalExit(processGeneration, current, runtimeLease);
            }
        }
    }

    private static boolean isAliveConservatively(Process process) {
        try {
            return process.isAlive();
        } catch (RuntimeException | LinkageError failure) {
            return true;
        }
    }

    /**
     * A failed onExit future proves nothing about process liveness. Wait on a
     * background virtual thread and release only after the process is confirmed
     * physically gone; if even the fallback cannot observe that fact, retain
     * the generation conservatively.
     */
    private static void observePhysicalExitAfterOnExitFailure(
            Process current,
            CanvasRunnerRuntimeLease runtimeLease,
            Runnable confirmedExit) {
        if (closeLeaseIfProcessExited(current, runtimeLease)) {
            confirmedExit.run();
            return;
        }
        try {
            Thread.ofVirtual().name("flutter-native-canvas-exit-fallback").start(() -> {
                boolean interrupted = false;
                try {
                    while (true) {
                        try {
                            current.waitFor();
                            if (closeLeaseIfProcessExited(current, runtimeLease)) {
                                confirmedExit.run();
                            }
                            return;
                        } catch (InterruptedException exception) {
                            interrupted = true;
                        } catch (RuntimeException | LinkageError ignored) {
                            if (closeLeaseIfProcessExited(current, runtimeLease)) {
                                confirmedExit.run();
                            }
                            return;
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        } catch (RuntimeException | LinkageError ignored) {
            // A scheduling race may have coincided with physical exit. If not,
            // retaining the generation is safer than deleting it under a live process.
            if (closeLeaseIfProcessExited(current, runtimeLease)) {
                confirmedExit.run();
            }
        }
    }

    private static boolean closeLeaseIfProcessExited(
            Process current,
            CanvasRunnerRuntimeLease runtimeLease) {
        try {
            if (!current.isAlive()) {
                closeLease(runtimeLease);
                return true;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Process liveness could not be confirmed; retain the generation.
        }
        return false;
    }

    private static void closeLease(CanvasRunnerRuntimeLease runtimeLease) {
        if (runtimeLease != null) {
            try {
                runtimeLease.close();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.WARNING,
                        "Native Flutter Canvas runtime lease cleanup failed",
                        failure);
            }
        }
    }

    private static RuntimeServices defaultRuntime(
            CanvasRunnerBuildService builds,
            FlutterToolchainService toolchains) {
        Objects.requireNonNull(builds, "builds");
        Objects.requireNonNull(toolchains, "toolchains");
        return new RuntimeServices(
                () -> {
                    var status = toolchains.resolve();
                    return new SdkResolution(
                            status.flutterSdk().orElse(null), status.flutterMessage());
                },
                builds::buildAsync,
                (command, workingDirectory) -> new ProcessBuilder(command)
                        .directory(workingDirectory.toFile())
                        .redirectErrorStream(false)
                        .start(),
                command -> Thread.ofVirtual()
                        .name("flutter-native-canvas-launch")
                        .start(command),
                EventQueue::invokeLater,
                FlutterDesignerNativeCanvasSession::scheduleAttachPoll,
                FlutterDesignerNativeCanvasSession::terminateAsync,
                System::nanoTime);
    }

    private static NativeCanvasRunnerContract requireRunnerContract(
            NativeCanvasPlatformProvider provider) {
        Objects.requireNonNull(provider, "platformProvider");
        if (!provider.isSupported()) {
            throw new IllegalArgumentException(
                    "Native Canvas provider is unavailable: "
                    + provider.availabilityReason());
        }
        NativeCanvasRunnerContract contract = provider.runnerContract()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supported native Canvas provider has no runner contract"));
        if (contract.platform() != provider.platform()) {
            throw new IllegalArgumentException(
                    "Native Canvas provider and runner contract platforms differ");
        }
        return contract;
    }

    private static Cancellable scheduleAttachPoll(Runnable poll) {
        requireEventDispatchThread();
        Timer timer = new Timer(50, event -> poll.run());
        timer.setRepeats(true);
        timer.start();
        return timer::stop;
    }

    private static void drainErrorOutput(
            Process process,
            BoundedDiagnostics diagnostics) {
        Thread.ofVirtual().name("flutter-native-canvas-stderr").start(() -> {
            try {
                copyBoundedDiagnostics(process.getErrorStream(), diagnostics);
            } catch (IOException exception) {
                diagnostics.append("Cannot read runner diagnostics: "
                        + failureReason(exception));
            }
        });
    }

    private static void copyBoundedDiagnostics(
            InputStream input,
            BoundedDiagnostics diagnostics) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            char[] chunk = new char[2048];
            int read;
            while ((read = reader.read(chunk)) != -1) {
                if (read > 0) {
                    diagnostics.append(chunk, 0, read);
                }
            }
        }
    }

    static String readBoundedDiagnostics(InputStream input) throws IOException {
        BoundedDiagnostics diagnostics = new BoundedDiagnostics();
        copyBoundedDiagnostics(input, diagnostics);
        return diagnostics.snapshot();
    }

    private static CompletableFuture<Void> terminateAsync(Process process) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        try {
            Thread.ofVirtual().name("flutter-native-canvas-stop").start(() -> {
                try {
                    process.destroy();
                    if (!process.waitFor(
                            TERMINATE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                        if (!process.waitFor(
                                TERMINATE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                            throw new IllegalStateException(
                                    "The Canvas runner remained alive after graceful and "
                                    + "forcible termination deadlines");
                        }
                    }
                    completion.complete(null);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    try {
                        process.destroyForcibly();
                    } catch (RuntimeException | LinkageError forcedFailure) {
                        exception.addSuppressed(forcedFailure);
                    }
                    completion.completeExceptionally(exception);
                } catch (RuntimeException | LinkageError failure) {
                    completion.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            completion.completeExceptionally(failure);
        }
        return completion;
    }

    private static Throwable unwrapCompletionFailure(Throwable failure) {
        Throwable current = Objects.requireNonNull(failure, "failure");
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String newNonce() {
        byte[] nonce = new byte[32];
        RANDOM.nextBytes(nonce);
        return HexFormat.of().formatHex(nonce);
    }

    private static String failureReason(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private static String formatDevicePixelRatio(
            NativeCanvasSurfaceMetrics metrics) {
        return formatDevicePixelRatio(metrics.devicePixelRatioMicros());
    }

    private static String formatDevicePixelRatio(int devicePixelRatioMicros) {
        return java.math.BigDecimal.valueOf(devicePixelRatioMicros, 6)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String appendFailure(String current, String next) {
        return current == null || current.isBlank()
                ? next
                : current + "; " + next;
    }

    private static String processDescription(Process process) {
        try {
            return "isolated Canvas process " + process.pid();
        } catch (RuntimeException | LinkageError failure) {
            return "isolated Canvas process (PID unavailable: "
                    + failureReason(failure) + ')';
        }
    }

    private static String diagnosticSuffix(String diagnostics) {
        return diagnostics == null || diagnostics.isBlank()
                ? "."
                : ". Runner output: " + diagnostics;
    }

    private static void requireEventDispatchThread() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Native Flutter Canvas session operations must run on the EDT");
        }
    }

    record SdkResolution(FlutterSdk sdk, String message) {
        SdkResolution {
            message = Objects.requireNonNullElse(message, "Flutter SDK is unavailable.");
            if (message.isBlank()) {
                throw new IllegalArgumentException("SDK resolution message cannot be blank");
            }
        }
    }

    private record WidgetMovePreviewPlacement(
            CanvasLayoutKey layout,
            StableId sourceWidgetId,
            WidgetPlacement destination) {
        private WidgetMovePreviewPlacement {
            Objects.requireNonNull(layout, "layout");
            Objects.requireNonNull(sourceWidgetId, "sourceWidgetId");
            Objects.requireNonNull(destination, "destination");
        }
    }

    private sealed interface WidgetMovePreviewCommand
            permits WidgetMovePreviewShow, WidgetMovePreviewClear {
        CanvasLayoutKey layout();
    }

    private record WidgetMovePreviewShow(WidgetMovePreviewPlacement placement)
            implements WidgetMovePreviewCommand {
        private WidgetMovePreviewShow {
            Objects.requireNonNull(placement, "placement");
        }

        @Override
        public CanvasLayoutKey layout() {
            return placement.layout();
        }
    }

    private record WidgetMovePreviewClear(CanvasLayoutKey layout)
            implements WidgetMovePreviewCommand {
        private WidgetMovePreviewClear {
            Objects.requireNonNull(layout, "layout");
        }
    }

    /** Exact process authority granted by an authenticated peer-loss close. */
    private record PeerLossCloseAuthority(
            Process process,
            long generation,
            CanvasSessionId sessionId) {

        private PeerLossCloseAuthority {
            Objects.requireNonNull(process, "process");
            Objects.requireNonNull(sessionId, "sessionId");
        }

        boolean matches(
                Process candidateProcess,
                long candidateGeneration,
                CanvasSessionId candidateSessionId) {
            return process == candidateProcess
                    && generation == candidateGeneration
                    && sessionId.equals(candidateSessionId);
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
            widgetIds = Set.copyOf(Objects.requireNonNull(widgetIds, "widgetIds"));
        }
    }

    @FunctionalInterface
    interface SdkResolver {
        SdkResolution resolve();
    }

    @FunctionalInterface
    interface BuildStarter {
        CompletableFuture<CanvasRunnerBuildResult> buildAsync(FlutterSdk sdk);
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(List<String> command, Path workingDirectory) throws IOException;
    }

    @FunctionalInterface
    interface PollScheduler {
        Cancellable schedule(Runnable poll);
    }

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }

    @FunctionalInterface
    interface ProcessTerminator {
        /** Completes when the bounded termination attempt finishes or fails. */
        CompletableFuture<Void> terminate(Process process);
    }

    record RuntimeServices(
            SdkResolver sdkResolver,
            BuildStarter buildStarter,
            ProcessStarter processStarter,
            Executor launchExecutor,
            Executor uiExecutor,
            PollScheduler pollScheduler,
            ProcessTerminator terminator,
            LongSupplier nanoTime) {
        RuntimeServices(
                SdkResolver sdkResolver,
                BuildStarter buildStarter,
                ProcessStarter processStarter,
                Executor launchExecutor,
                Executor uiExecutor,
                PollScheduler pollScheduler,
                ProcessTerminator terminator) {
            this(
                    sdkResolver,
                    buildStarter,
                    processStarter,
                    launchExecutor,
                    uiExecutor,
                    pollScheduler,
                    terminator,
                    System::nanoTime);
        }

        RuntimeServices {
            Objects.requireNonNull(sdkResolver, "sdkResolver");
            Objects.requireNonNull(buildStarter, "buildStarter");
            Objects.requireNonNull(processStarter, "processStarter");
            Objects.requireNonNull(launchExecutor, "launchExecutor");
            Objects.requireNonNull(uiExecutor, "uiExecutor");
            Objects.requireNonNull(pollScheduler, "pollScheduler");
            Objects.requireNonNull(terminator, "terminator");
            Objects.requireNonNull(nanoTime, "nanoTime");
        }
    }

    private static final class BoundedDiagnostics {
        private static final String TRUNCATED_MARKER = " [output truncated]";
        private final StringBuilder value = new StringBuilder();
        private boolean truncated;

        synchronized void append(String line) {
            char[] characters = line.toCharArray();
            append(characters, 0, characters.length);
        }

        synchronized void append(char[] characters, int offset, int length) {
            if (truncated) {
                return;
            }
            int remaining = MAX_RUNTIME_DIAGNOSTIC_CHARS - value.length();
            if (length <= remaining) {
                value.append(characters, offset, length);
            } else {
                int contentLimit = MAX_RUNTIME_DIAGNOSTIC_CHARS
                        - TRUNCATED_MARKER.length();
                if (value.length() > contentLimit) {
                    value.setLength(contentLimit);
                }
                int contentLength = Math.min(
                        length,
                        Math.max(0, contentLimit - value.length()));
                value.append(characters, offset, contentLength);
                value.append(TRUNCATED_MARKER);
                truncated = true;
            }
        }

        synchronized String snapshot() {
            return value.toString();
        }
    }
}
