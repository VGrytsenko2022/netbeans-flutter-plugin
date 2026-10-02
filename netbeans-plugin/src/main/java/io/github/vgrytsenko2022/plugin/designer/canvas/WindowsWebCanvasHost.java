package io.github.vgrytsenko2022.plugin.designer.canvas;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import io.github.vgrytsenko2022.designer.canvas.CanvasSurfaceMetrics;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.JPanel;

/**
 * Internal heavyweight Swing carrier for one isolated windowed WebView2
 * controller. Product preview routing remains a separate feature gate.
 */
final class WindowsWebCanvasHost extends JPanel implements AutoCloseable {
    private static final int MAXIMUM_PENDING_NATIVE_EVENTS = 64;
    private static final long MAXIMUM_PENDING_NATIVE_EVENT_CHARACTERS =
            8L * 1024 * 1024;
    private static final int MAXIMUM_USER_DATA_DELETE_ATTEMPTS = 4;
    static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);
    static final Duration PARENT_RELEASE_TIMEOUT = Duration.ofSeconds(5);
    static final Duration DESTROY_TIMEOUT = Duration.ofSeconds(30);
    static final String USER_DATA_MARKER =
            ".netbeans-flutter-webview2-owned-";
    static final String USER_DATA_SESSIONS_ROOT_MARKER =
            ".netbeans-flutter-webview2-sessions-root";
    private static final String USER_DATA_MARKER_FORMAT =
            "NETBEANS_FLUTTER_WEBVIEW2_USER_DATA|1|";
    private static final String USER_DATA_SESSIONS_ROOT_MARKER_FORMAT =
            "NETBEANS_FLUTTER_WEBVIEW2_SESSIONS_ROOT|1|";

    private volatile NativeDependencies nativeDependencies;
    private final Executor nativeExecutor;
    private final ParentHandleResolver parentHandleResolver;
    private final StartupDeadlineScheduler startupDeadlineScheduler;
    private final NativeDependencyLoader nativeDependencyLoader;
    private final Object dependencyPreparationLock = new Object();
    private final CleanupRetryDelay cleanupRetryDelay;
    private final HostCanvas canvas = new HostCanvas();
    private final Object nativeEventLock = new Object();
    private final ArrayDeque<QueuedNativeEvent> nativeEventInbox =
            new ArrayDeque<>();

    private volatile long generation;
    private long parentWindow;
    private volatile Phase phase = Phase.NEW;
    private boolean initializationInFlight;
    private boolean teardownInFlight;
    private boolean parentReleaseAttempted;
    private boolean parentReleaseConfirmed;
    private boolean browserProcessReleaseConfirmed;
    private boolean peerRemovalBarrierComplete;
    private boolean documentReady;
    private boolean bridgeAuthenticated;
    private boolean transportFailureTerminalPending;
    private volatile WindowsWebView2NativeApi.NativeSession nativeSession;
    private boolean requestedControllerVisible;
    private Consumer<CanvasSurfaceMetrics> surfaceMetricsListener = ignored -> {};
    private OwnedUserDataFolder ownedUserDataFolder;
    private WebCanvasArtifactPublisher.PublishedArtifact.Lease artifactLease;
    private WebCanvasHostBridge bridge;
    private StartupDeadlineScheduler.Cancellation startupDeadline;
    private final ArrayDeque<WindowsWebView2NativeApi.Event> pendingNativeEvents =
            new ArrayDeque<>();
    private long nativeEventInboxCharacters;
    private long pendingNativeEventCharacters;
    private long nativeEventInboxEpoch;
    private long nativeEventOverflowGeneration;
    private boolean nativeEventDrainScheduled;
    private boolean nativeEventOverflow;
    private Listener listener = Listener.NOOP;
    private CompletableFuture<Void> closeCompletion =
            CompletableFuture.completedFuture(null);

    static WindowsWebCanvasHost createDefault() throws IOException {
        return createDeferred(
                command -> Thread.ofVirtual()
                        .name("flutter-webview2-native-lifecycle")
                        .start(command),
                ParentHandleResolver.system(),
                StartupDeadlineScheduler.system(),
                CleanupRetryDelay.system(),
                NativeDependencyLoader.system());
    }

    static WindowsWebCanvasHost createDeferred(
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            CleanupRetryDelay cleanupRetryDelay,
            NativeDependencyLoader nativeDependencyLoader) {
        return new WindowsWebCanvasHost(
                null,
                nativeExecutor,
                parentHandleResolver,
                startupDeadlineScheduler,
                null,
                cleanupRetryDelay,
                null,
                Objects.requireNonNull(
                        nativeDependencyLoader, "nativeDependencyLoader"));
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                StartupDeadlineScheduler.system(), ClientBoundsResolver.system(),
                CleanupRetryDelay.system(),
                WindowsWebCanvasFocusController.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, ClientBoundsResolver.system(),
                CleanupRetryDelay.system(),
                WindowsWebCanvasFocusController.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, clientBoundsResolver,
                CleanupRetryDelay.system(),
                WindowsWebCanvasFocusController.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver,
            CleanupRetryDelay cleanupRetryDelay) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, clientBoundsResolver,
                cleanupRetryDelay, WindowsWebCanvasFocusController.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver,
            CleanupRetryDelay cleanupRetryDelay,
            WindowsWebCanvasFocusController focusController) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, clientBoundsResolver,
                cleanupRetryDelay, focusController, null);
    }

    private WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver,
            CleanupRetryDelay cleanupRetryDelay,
            WindowsWebCanvasFocusController focusController,
            NativeDependencyLoader nativeDependencyLoader) {
        super(new BorderLayout());
        this.nativeDependencyLoader = nativeDependencyLoader;
        this.nativeDependencies = nativeDependencyLoader == null
                ? new NativeDependencies(
                        nativeApi, clientBoundsResolver, focusController)
                : null;
        this.nativeExecutor = Objects.requireNonNull(nativeExecutor, "nativeExecutor");
        this.parentHandleResolver = Objects.requireNonNull(
                parentHandleResolver, "parentHandleResolver");
        this.startupDeadlineScheduler = Objects.requireNonNull(
                startupDeadlineScheduler, "startupDeadlineScheduler");
        this.cleanupRetryDelay = Objects.requireNonNull(
                cleanupRetryDelay, "cleanupRetryDelay");
        canvas.setBackground(new Color(0x20, 0x22, 0x24));
        canvas.setFocusable(true);
        // The WebView2 child HWND, not AWT, owns browser keyboard input.
        canvas.enableInputMethods(false);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                publishBounds();
            }

            @Override
            public void componentMoved(ComponentEvent event) {
                publishBounds();
            }
        });
        add(canvas, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Flutter Web Canvas host");
        getAccessibleContext().setAccessibleDescription(
                "Heavyweight host for the isolated Microsoft Edge WebView2 Canvas surface.");
        canvas.getAccessibleContext().setAccessibleName("Flutter Web Canvas surface");
    }

    void start(StartRequest request, Listener listener) {
        requireEdt();
        Objects.requireNonNull(request, "request");
        requirePreparedDependencies();
        if (phase != Phase.NEW && phase != Phase.CLOSED) {
            throw new IllegalStateException(phase == Phase.POISONED
                    ? "Web Canvas host cleanup is incomplete; restart is forbidden"
                    : "Web Canvas host is already active");
        }
        long parent = parentHandleResolver.resolve(canvas);
        if (parent == 0) {
            throw new IllegalStateException("The Web Canvas AWT carrier has no native HWND");
        }
        Listener acceptedListener = Objects.requireNonNull(listener, "listener");
        long startedGeneration = generation + 1;
        WebCanvasOriginPolicy originPolicy = new WebCanvasOriginPolicy(
                request.sessionNonce(),
                request.artifact().generationId(),
                request.artifact().files().keySet());
        WebCanvasHostBridge createdBridge = new WebCanvasHostBridge(
                originPolicy.indexUri(),
                request.sessionNonce(),
                json -> postBridgeJson(startedGeneration, json),
                new WebCanvasHostBridge.Listener() {
                    @Override
                    public void ready() {
                        EventQueue.invokeLater(() ->
                                acceptBridgeReady(startedGeneration));
                    }

                    @Override
                    public void diagnostic(String message) {
                        EventQueue.invokeLater(() ->
                                acceptDiagnostic(startedGeneration, message));
                    }

                    @Override
                    public void terminal(WebCanvasHostBridge.Terminal terminal) {
                        EventQueue.invokeLater(() ->
                                acceptBridgeTerminal(startedGeneration, terminal));
                    }
                });
        CanvasSurfaceMetrics initialMetrics = physicalSurfaceMetrics(parent);
        WindowsWebView2NativeApi.CreateRequest nativeRequest =
                new WindowsWebView2NativeApi.CreateRequest(
                        parent, 0, 0,
                        initialMetrics.physicalWidth(),
                        initialMetrics.physicalHeight(),
                        request.userDataFolder(), request.artifact().root(),
                        originPolicy, request.artifact().files(),
                        request.sessionNonce());
        WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact =
                request.artifact().acquireLease();

        // Commit the new generation only after every synchronous validation
        // above succeeds. A rejected request must leave a NEW/CLOSED host
        // reusable instead of stranding it in STARTING.
        this.listener = acceptedListener;
        generation = startedGeneration;
        parentWindow = parent;
        phase = Phase.STARTING;
        initializationInFlight = true;
        teardownInFlight = false;
        parentReleaseAttempted = false;
        parentReleaseConfirmed = false;
        browserProcessReleaseConfirmed = false;
        peerRemovalBarrierComplete = false;
        documentReady = false;
        bridgeAuthenticated = false;
        transportFailureTerminalPending = false;
        nativeSession = null;
        ownedUserDataFolder = null;
        artifactLease = retainedArtifact;
        bridge = createdBridge;
        clearNativeEventQueues();
        closeCompletion = new CompletableFuture<>();
        try {
            armStartupDeadline(startedGeneration);
        } catch (RuntimeException | LinkageError failure) {
            initializationInFlight = false;
            failOnEdt(startedGeneration,
                    "Schedule Web Canvas startup deadline", failure);
            return;
        }
        executeOffEdt(
                "flutter-webview2-native-start",
                () -> initializeNative(
                        startedGeneration, nativeRequest,
                        request.userDataSessionsRoot(), originPolicy,
                        retainedArtifact),
                failure -> {
                    if (startedGeneration == generation
                            && phase == Phase.STARTING) {
                        initializationInFlight = false;
                        failOnEdt(startedGeneration,
                                "Schedule Web Canvas startup", failure);
                    }
                });
    }

    InputStream runnerStdout() {
        requireEdt();
        if (bridge == null) {
            throw new IllegalStateException("Web Canvas bridge has not been created");
        }
        return bridge.runnerStdout();
    }

    OutputStream runnerStdin() {
        requireEdt();
        if (bridge == null || phase != Phase.RUNNING || !bridge.isReady()) {
            throw new IllegalStateException(
                    "Web Canvas runner input requires an authenticated running bridge");
        }
        return bridge.runnerStdin();
    }

    boolean isBridgeReady() {
        requireEdt();
        return bridge != null && bridge.isReady() && phase == Phase.RUNNING;
    }

    void requestControllerFocus() {
        requireEdt();
        WindowsWebView2NativeApi.NativeSession session = nativeSession;
        if (session == null || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return;
        }
        try {
            session.requestFocus();
        } catch (IOException | RuntimeException failure) {
            failOnEdt(generation, "Focus Web Canvas", failure);
        }
    }

    /**
     * Publishes the backend-neutral requested visibility explicitly. A late
     * WebView2 initialization replays the same state instead of inferring a
     * stale value from an earlier Swing component event.
     */
    void setControllerVisible(boolean visible) {
        requireEdt();
        requestedControllerVisible = visible;
        publishEffectiveVisibility();
    }

    /**
     * Loads every packaged/JNA dependency outside the Swing EDT. A default
     * host refuses to start until this exact preparation completed.
     */
    void prepareNativeDependencies() throws IOException {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Web Canvas native dependencies must be prepared off the EDT");
        }
        if (nativeDependencyLoader == null || dependenciesPrepared()) {
            return;
        }
        synchronized (dependencyPreparationLock) {
            if (dependenciesPrepared()) {
                return;
            }
            NativeDependencies prepared = Objects.requireNonNull(
                    nativeDependencyLoader.load(),
                    "nativeDependencyLoader.load()");
            nativeDependencies = prepared;
        }
    }

    void setSurfaceMetricsListener(Consumer<CanvasSurfaceMetrics> listener) {
        requireEdt();
        surfaceMetricsListener = Objects.requireNonNull(listener, "listener");
        publishSurfaceMetricsIfPossible();
    }

    boolean isControllerFocused() {
        requireEdt();
        NativeDependencies dependencies = nativeDependencies;
        if (nativeSession == null
                || dependencies == null
                || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return false;
        }
        try {
            return dependencies.focusController()
                    .isControllerFocused(parentWindow);
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    boolean releaseControllerFocus() {
        requireEdt();
        NativeDependencies dependencies = nativeDependencies;
        if (nativeSession == null
                || dependencies == null
                || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return false;
        }
        try {
            WindowsWebCanvasFocusController.FocusReleaseResult result =
                    dependencies.focusController()
                            .releaseControllerFocus(parentWindow);
            if (result == WindowsWebCanvasFocusController.FocusReleaseResult
                    .INPUT_QUEUE_DETACH_FAILED) {
                failOnEdt(generation, "Release Web Canvas focus", new IOException(
                        "Windows could not detach the AWT and WebView2 input "
                        + "queues after releasing controller focus"));
                return false;
            }
            return result
                    == WindowsWebCanvasFocusController.FocusReleaseResult.RELEASED;
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    boolean isCarrierDisplayable() {
        requireEdt();
        return canvas.isDisplayable();
    }

    private boolean dependenciesPrepared() {
        return nativeDependencies != null;
    }

    private NativeDependencies requirePreparedDependencies() {
        NativeDependencies prepared = nativeDependencies;
        if (prepared == null) {
            throw new IllegalStateException(
                    "Web Canvas native dependencies were not prepared off the EDT");
        }
        return prepared;
    }

    CompletableFuture<Void> closeAsync() {
        requireEdt();
        beginClose(generation);
        return closeCompletion;
    }

    /**
     * Begins the bounded off-EDT native parent-release and full teardown
     * barrier. The component owner must await this future before removing the
     * heavyweight Canvas from its displayable AWT hierarchy.
     */
    CompletableFuture<Void> preparePeerRemovalAsync() {
        requireEdt();
        beginClose(generation);
        return closeCompletion;
    }

    @Override
    public void close() {
        if (EventQueue.isDispatchThread()) {
            closeAsync();
        } else {
            EventQueue.invokeLater(this::closeAsync);
        }
    }

    private void initializeNative(
            long expectedGeneration,
            WindowsWebView2NativeApi.CreateRequest request,
            UserDataSessionsRoot userDataSessionsRoot,
            WebCanvasOriginPolicy originPolicy,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact) {
        WindowsWebView2NativeApi.NativeSession created = null;
        OwnedUserDataFolder createdUserData = null;
        String runtime = "";
        Throwable failure = null;
        boolean failedCreateReleaseConfirmed = true;
        try {
            WindowsWebView2NativeApi nativeApi =
                    requirePreparedDependencies().nativeApi();
            requireSafeArtifactDirectory(request.contentRoot());
            createdUserData = OwnedUserDataFolder.create(
                    userDataSessionsRoot, request.userDataFolder(),
                    request.contentRoot(), request.sessionNonce());
            runtime = nativeApi.runtimeVersion();
            createdUserData.verify();
            failedCreateReleaseConfirmed = false;
            created = nativeApi.create(
                    request,
                    event -> acceptNativeEvent(expectedGeneration, event));
        } catch (WindowsWebView2NativeApi.CreateException problem) {
            created = problem.retainedSession().orElse(null);
            failedCreateReleaseConfirmed = problem.releaseConfirmed();
            failure = problem;
        } catch (IOException | RuntimeException | LinkageError problem) {
            failure = problem;
        }
        WindowsWebView2NativeApi.NativeSession result = created;
        OwnedUserDataFolder resultUserData = createdUserData;
        String detectedRuntime = runtime;
        Throwable detectedFailure = failure;
        boolean releaseConfirmed = failedCreateReleaseConfirmed;
        EventQueue.invokeLater(() -> finishInitialization(
                expectedGeneration, result, resultUserData, retainedArtifact,
                detectedRuntime, originPolicy.indexUri(), detectedFailure,
                releaseConfirmed));
    }

    private void finishInitialization(
            long expectedGeneration,
            WindowsWebView2NativeApi.NativeSession created,
            OwnedUserDataFolder createdUserData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact,
            String runtime,
            String documentUri,
            Throwable failure,
            boolean failedCreateReleaseConfirmed) {
        requireEdt();
        initializationInFlight = false;
        if (expectedGeneration != generation) {
            destroyDetachedOffEdt(
                    created, createdUserData, retainedArtifact,
                    failedCreateReleaseConfirmed);
            return;
        }
        if (created == null && failedCreateReleaseConfirmed) {
            browserProcessReleaseConfirmed = true;
            parentReleaseConfirmed = true;
        }
        ownedUserDataFolder = createdUserData;
        nativeSession = created;
        if (phase == Phase.CLOSING || phase == Phase.CLOSED) {
            pendingNativeEvents.clear();
            pendingNativeEventCharacters = 0;
            startTeardownIfReady(expectedGeneration);
            return;
        }
        if (failure != null || created == null) {
            failOnEdt(expectedGeneration, "Start Web Canvas", failure == null
                    ? new IOException("native WebView2 session was not created") : failure);
            return;
        }
        try {
            listener.nativeStarted(runtime, documentUri);
        } catch (RuntimeException ignored) {
            // UI callbacks cannot poison native lifetime.
        }
        publishBounds();
        publishEffectiveVisibility();
        drainPendingNativeEvents(expectedGeneration);
    }

    private void acceptNativeEvent(
            long expectedGeneration,
            WindowsWebView2NativeApi.Event event) {
        Objects.requireNonNull(event, "event");
        if (expectedGeneration != generation || !acceptsNativeEvents()) {
            return;
        }
        long characters = eventCharacters(event);
        boolean scheduleDrain = false;
        long drainEpoch = 0;
        synchronized (nativeEventLock) {
            if (expectedGeneration != generation || !acceptsNativeEvents()
                    || nativeEventOverflow) {
                return;
            }
            if (nativeEventInbox.size() >= MAXIMUM_PENDING_NATIVE_EVENTS
                    || characters > MAXIMUM_PENDING_NATIVE_EVENT_CHARACTERS
                    || nativeEventInboxCharacters
                    > MAXIMUM_PENDING_NATIVE_EVENT_CHARACTERS - characters) {
                nativeEventInbox.clear();
                nativeEventInboxCharacters = 0;
                nativeEventOverflow = true;
                nativeEventOverflowGeneration = expectedGeneration;
            } else {
                nativeEventInbox.addLast(new QueuedNativeEvent(
                        expectedGeneration, event, characters));
                nativeEventInboxCharacters += characters;
            }
            if (!nativeEventDrainScheduled) {
                nativeEventDrainScheduled = true;
                scheduleDrain = true;
                drainEpoch = nativeEventInboxEpoch;
            }
        }
        if (scheduleDrain) {
            long expectedEpoch = drainEpoch;
            EventQueue.invokeLater(() -> drainNativeEventInbox(expectedEpoch));
        }
    }

    private void drainNativeEventInbox(long expectedEpoch) {
        requireEdt();
        while (true) {
            QueuedNativeEvent queued;
            long overflowGeneration = 0;
            synchronized (nativeEventLock) {
                if (expectedEpoch != nativeEventInboxEpoch) {
                    return;
                }
                if (nativeEventOverflow) {
                    overflowGeneration = nativeEventOverflowGeneration;
                    nativeEventOverflow = false;
                    nativeEventInbox.clear();
                    nativeEventInboxCharacters = 0;
                    nativeEventDrainScheduled = false;
                    queued = null;
                } else {
                    queued = nativeEventInbox.pollFirst();
                    if (queued == null) {
                        nativeEventDrainScheduled = false;
                        return;
                    }
                    nativeEventInboxCharacters -= queued.characters();
                }
            }
            if (overflowGeneration != 0) {
                failOnEdt(
                        overflowGeneration,
                        "Receive native WebView2 events",
                        new IOException(
                                "Native WebView2 event inbox exceeded 64 events or 8 MiB"));
                return;
            }
            acceptNativeEventOnEdt(
                    queued.expectedGeneration(), queued.event(), queued.characters());
        }
    }

    private void acceptNativeEventOnEdt(
            long expectedGeneration,
            WindowsWebView2NativeApi.Event event,
            long characters) {
        requireEdt();
        if (expectedGeneration != generation || !acceptsNativeEvents()) {
            return;
        }
        if (initializationInFlight || nativeSession == null) {
            if (pendingNativeEvents.size() >= MAXIMUM_PENDING_NATIVE_EVENTS
                    || characters > MAXIMUM_PENDING_NATIVE_EVENT_CHARACTERS
                    || pendingNativeEventCharacters
                    > MAXIMUM_PENDING_NATIVE_EVENT_CHARACTERS - characters) {
                pendingNativeEvents.clear();
                pendingNativeEventCharacters = 0;
                failOnEdt(
                        expectedGeneration,
                        "Start Web Canvas",
                        new IOException(
                                "Native WebView2 startup events exceeded 64 events or 8 MiB"));
            } else {
                pendingNativeEvents.addLast(event);
                pendingNativeEventCharacters += characters;
            }
            return;
        }
        dispatchNativeEvent(expectedGeneration, event);
    }

    private void drainPendingNativeEvents(long expectedGeneration) {
        requireEdt();
        while (expectedGeneration == generation
                && acceptsNativeEvents()
                && !pendingNativeEvents.isEmpty()) {
            WindowsWebView2NativeApi.Event event = pendingNativeEvents.removeFirst();
            pendingNativeEventCharacters -= eventCharacters(event);
            dispatchNativeEvent(expectedGeneration, event);
        }
        if (!acceptsNativeEvents()) {
            pendingNativeEvents.clear();
            pendingNativeEventCharacters = 0;
        }
    }

    private void dispatchNativeEvent(
            long expectedGeneration,
            WindowsWebView2NativeApi.Event event) {
        requireEdt();
        switch (event.kind()) {
            case CONTROLLER_READY -> {
                if (!documentReady) {
                    documentReady = true;
                    safeListener(() -> listener.documentReady(event.source()));
                    enterRunningWhenReady(expectedGeneration);
                }
            }
            case WEB_MESSAGE -> {
                if (bridge != null) {
                    bridge.acceptWebMessage(event.source(), event.payload());
                }
            }
            case DIAGNOSTIC -> safeListener(() ->
                    listener.diagnostic(event.payload()));
            case FAILED -> failOnEdt(
                    expectedGeneration,
                    "Native WebView2 host",
                    new IOException(event.payload() + " (status 0x"
                            + String.format(java.util.Locale.ROOT, "%08X",
                                    event.statusCode()) + ")"));
            case PROCESS_FAILED -> failOnEdt(
                    expectedGeneration,
                    "WebView2 renderer/browser process",
                    new IOException(processFailureDescription(event)));
            case CLOSED -> failOnEdt(
                    expectedGeneration,
                    "Native WebView2 host",
                    new IOException("Native WebView2 host closed unexpectedly"));
        }
    }

    private static String processFailureDescription(
            WindowsWebView2NativeApi.Event event) {
        int value = event.statusCode();
        String kind = switch (value) {
            case 0 -> "BROWSER_PROCESS_EXITED";
            case 1 -> "RENDER_PROCESS_EXITED";
            case 2 -> "RENDER_PROCESS_UNRESPONSIVE";
            case 3 -> "FRAME_RENDER_PROCESS_EXITED";
            case 4 -> "UTILITY_PROCESS_EXITED";
            case 5 -> "SANDBOX_HELPER_PROCESS_EXITED";
            case 6 -> "GPU_PROCESS_EXITED";
            case 7 -> "PPAPI_PLUGIN_PROCESS_EXITED";
            case 8 -> "PPAPI_BROKER_PROCESS_EXITED";
            case 9 -> "UNKNOWN_PROCESS_EXITED";
            default -> "UNRECOGNIZED_PROCESS_FAILED_KIND";
        };
        String detail = event.payload().isBlank()
                ? "" : ": " + event.payload();
        return "WebView2 process failure kind " + kind + " (" + value + ")"
                + detail;
    }

    private void postBridgeJson(long expectedGeneration, String json) throws IOException {
        WindowsWebView2NativeApi.NativeSession session;
        synchronized (this) {
            if (expectedGeneration != generation || phase != Phase.RUNNING
                    || bridge == null || !bridge.isReady() || nativeSession == null) {
                throw new IOException(
                        "Web Canvas authenticated running session is unavailable");
            }
            session = nativeSession;
        }
        session.postWebMessageJson(json);
    }

    private void acceptBridgeReady(long expectedGeneration) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> acceptBridgeReady(expectedGeneration));
            return;
        }
        if (expectedGeneration != generation || phase != Phase.STARTING) {
            return;
        }
        bridgeAuthenticated = true;
        enterRunningWhenReady(expectedGeneration);
    }

    private void enterRunningWhenReady(long expectedGeneration) {
        requireEdt();
        if (expectedGeneration != generation || phase != Phase.STARTING
                || nativeSession == null || !documentReady || !bridgeAuthenticated) {
            return;
        }
        phase = Phase.RUNNING;
        cancelStartupDeadline();
        safeListener(listener::bridgeReady);
    }

    private void acceptDiagnostic(long expectedGeneration, String message) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> acceptDiagnostic(expectedGeneration, message));
            return;
        }
        if (expectedGeneration == generation && acceptsNativeEvents()) {
            safeListener(() -> listener.diagnostic(message));
        }
    }

    private void acceptBridgeTerminal(
            long expectedGeneration, WebCanvasHostBridge.Terminal terminal) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> acceptBridgeTerminal(
                    expectedGeneration, terminal));
            return;
        }
        boolean localCloseDuringTeardown = phase == Phase.CLOSING
                && terminal.kind() == WebCanvasHostBridge.TerminalKind.LOCAL_CLOSE;
        boolean expectedFailureDuringTeardown = phase == Phase.CLOSING
                && transportFailureTerminalPending
                && terminal.kind()
                == WebCanvasHostBridge.TerminalKind.TRANSPORT_FAILURE;
        if (expectedGeneration != generation
                || (!acceptsNativeEvents()
                && !localCloseDuringTeardown
                && !expectedFailureDuringTeardown)) {
            return;
        }
        transportFailureTerminalPending = false;
        safeListener(() -> listener.bridgeTerminal(terminal));
        if (terminal.kind() != WebCanvasHostBridge.TerminalKind.LOCAL_CLOSE) {
            beginClose(expectedGeneration);
        }
    }

    private void publishBounds() {
        if (!EventQueue.isDispatchThread()) {
            return;
        }
        WindowsWebView2NativeApi.NativeSession session = nativeSession;
        if (session == null || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return;
        }
        try {
            CanvasSurfaceMetrics metrics = physicalSurfaceMetrics(parentWindow);
            session.setBounds(
                    0,
                    0,
                    metrics.physicalWidth(),
                    metrics.physicalHeight());
            publishSurfaceMetrics(metrics);
        } catch (IOException | RuntimeException failure) {
            failOnEdt(generation, "Resize Web Canvas", failure);
        }
    }

    private void publishSurfaceMetricsIfPossible() {
        if (!EventQueue.isDispatchThread() || parentWindow == 0
                || nativeSession == null
                || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return;
        }
        try {
            publishSurfaceMetrics(physicalSurfaceMetrics(parentWindow));
        } catch (RuntimeException failure) {
            failOnEdt(generation, "Observe Web Canvas surface metrics", failure);
        }
    }

    private void publishSurfaceMetrics(CanvasSurfaceMetrics metrics) {
        try {
            surfaceMetricsListener.accept(metrics);
        } catch (RuntimeException ignored) {
            // UI/session callbacks cannot poison native browser ownership.
        }
    }

    private boolean acceptsNativeEvents() {
        Phase current = phase;
        return current == Phase.STARTING || current == Phase.RUNNING;
    }

    private CanvasSurfaceMetrics physicalSurfaceMetrics(long window) {
        if (window == 0) {
            throw new IllegalStateException(
                    "The Web Canvas AWT carrier has no native HWND");
        }
        ClientBoundsResolver clientBoundsResolver =
                requirePreparedDependencies().clientBoundsResolver();
        NativeCanvasWindowBounds raw = Objects.requireNonNull(
                clientBoundsResolver.resolve(window), "native client bounds");
        int width = Math.max(1, raw.width());
        int height = Math.max(1, raw.height());
        int devicePixelRatioMicros =
                clientBoundsResolver.devicePixelRatioMicros(window);
        return new CanvasSurfaceMetrics(
                width, height, devicePixelRatioMicros);
    }

    private void publishEffectiveVisibility() {
        publishVisibility(requestedControllerVisible);
    }

    private void publishVisibility(boolean visible) {
        if (!EventQueue.isDispatchThread()) {
            return;
        }
        WindowsWebView2NativeApi.NativeSession session = nativeSession;
        if (session == null || (phase != Phase.STARTING && phase != Phase.RUNNING)) {
            return;
        }
        try {
            session.setVisible(visible);
        } catch (IOException | RuntimeException failure) {
            failOnEdt(generation, "Change Web Canvas visibility", failure);
        }
    }

    private void armStartupDeadline(long expectedGeneration) {
        requireEdt();
        startupDeadline = Objects.requireNonNull(
                startupDeadlineScheduler.schedule(
                        STARTUP_TIMEOUT,
                        () -> EventQueue.invokeLater(() ->
                                startupTimedOut(expectedGeneration))),
                "startup deadline cancellation");
    }

    private void startupTimedOut(long expectedGeneration) {
        requireEdt();
        if (expectedGeneration != generation || phase != Phase.STARTING) {
            return;
        }
        final String waitingFor;
        if (initializationInFlight || nativeSession == null) {
            waitingFor = "native WebView2 session creation";
        } else if (!documentReady) {
            waitingFor = "the WebView2 controller/document readiness event";
        } else {
            waitingFor = "authenticated Web Canvas bridge readiness";
        }
        failOnEdt(expectedGeneration, "Start Web Canvas", new IOException(
                "Timed out after " + STARTUP_TIMEOUT.toSeconds()
                + " seconds waiting for " + waitingFor));
    }

    private void cancelStartupDeadline() {
        StartupDeadlineScheduler.Cancellation cancellation = startupDeadline;
        startupDeadline = null;
        if (cancellation != null) {
            try {
                cancellation.cancel();
            } catch (RuntimeException ignored) {
                // Cancellation is an advisory generation fence. The timeout
                // callback also checks phase and generation on the EDT.
            }
        }
    }

    private void failOnEdt(long expectedGeneration, String operation, Throwable failure) {
        requireEdt();
        if (expectedGeneration != generation || phase == Phase.CLOSING
                || phase == Phase.POISONED || phase == Phase.CLOSED) {
            return;
        }
        phase = Phase.FAILED;
        String reason = boundedReason(failure);
        safeListener(() -> listener.failed(operation, reason));
        if (bridge != null) {
            transportFailureTerminalPending = true;
            bridge.failTransport(operation + " failed: " + reason);
        }
        beginClose(expectedGeneration);
    }

    private void beginClose(long expectedGeneration) {
        requireEdt();
        if (expectedGeneration != generation || phase == Phase.CLOSED
                || phase == Phase.CLOSING) {
            return;
        }
        if (phase == Phase.POISONED) {
            closeCompletion = new CompletableFuture<>();
        }
        phase = Phase.CLOSING;
        cancelStartupDeadline();
        clearNativeEventQueues();
        if (bridge != null) {
            bridge.close();
        }
        startTeardownIfReady(expectedGeneration);
    }

    private void startTeardownIfReady(long expectedGeneration) {
        requireEdt();
        if (expectedGeneration != generation || phase != Phase.CLOSING
                || initializationInFlight || teardownInFlight) {
            return;
        }
        WindowsWebView2NativeApi.NativeSession session = nativeSession;
        OwnedUserDataFolder userData = ownedUserDataFolder;
        WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact =
                artifactLease;
        long expectedParent = parentWindow;
        boolean attemptParentRelease = session != null && !parentReleaseAttempted;
        boolean parentAlreadyReleased = parentReleaseConfirmed;
        boolean browserAlreadyReleased = browserProcessReleaseConfirmed;
        if (session == null && userData == null && retainedArtifact == null) {
            completeClose(expectedGeneration, CleanupResult.empty());
            return;
        }
        teardownInFlight = true;
        executeOffEdt("flutter-webview2-native-destroy", () -> {
            CleanupResult result = cleanupResources(
                    session, userData, retainedArtifact,
                    expectedParent, attemptParentRelease,
                    parentAlreadyReleased, browserAlreadyReleased);
            EventQueue.invokeLater(() -> completeClose(expectedGeneration, result));
        }, failure -> completeClose(
                expectedGeneration,
                CleanupResult.notStarted(
                        failure, session, userData, retainedArtifact,
                        parentReleaseAttempted, parentReleaseConfirmed,
                        browserProcessReleaseConfirmed)));
    }

    private void destroyDetachedOffEdt(
            WindowsWebView2NativeApi.NativeSession session,
            OwnedUserDataFolder userData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact,
            boolean browserReleaseConfirmed) {
        if (session == null && userData == null && retainedArtifact == null) {
            return;
        }
        executeOffEdt("flutter-webview2-native-detached-destroy", () ->
                cleanupResources(
                        session, userData, retainedArtifact,
                        0, false, session == null,
                        browserReleaseConfirmed),
                ignored -> { });
    }

    private CleanupResult cleanupResources(
            WindowsWebView2NativeApi.NativeSession session,
            OwnedUserDataFolder userData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact,
            long expectedParent,
            boolean attemptParentRelease,
            boolean parentAlreadyReleased,
            boolean browserAlreadyReleased) {
        Throwable failure = null;
        boolean nativeReleased = session == null;
        boolean parentAttempted = !attemptParentRelease;
        boolean parentReleased = parentAlreadyReleased || session == null;
        boolean browserReleased = browserAlreadyReleased;
        boolean userDataReleased = userData == null;
        boolean artifactReleased = retainedArtifact == null;
        if (session != null) {
            Throwable parentReleaseFailure = null;
            if (attemptParentRelease) {
                parentAttempted = true;
                try {
                    session.prepareParentRelease(
                            expectedParent, PARENT_RELEASE_TIMEOUT);
                    parentReleased = true;
                } catch (IOException | RuntimeException | LinkageError problem) {
                    parentReleaseFailure = problem;
                }
            }
            try {
                WindowsWebView2NativeApi.DestroyResult destroyed =
                        session.destroy(DESTROY_TIMEOUT);
                nativeReleased = true;
                parentReleased |= hasDestroyFlag(
                        destroyed,
                        WindowsWebView2NativeApi.DestroyResult.PARENT_RELEASED);
                browserReleased |= destroyed.udfReleaseConfirmed();
                if (!parentReleased) {
                    IOException missingParentProof = new IOException(
                            "Native WebView2 teardown did not confirm parent release");
                    if (parentReleaseFailure != null) {
                        missingParentProof.addSuppressed(parentReleaseFailure);
                    }
                    failure = appendFailure(failure, missingParentProof);
                }
                if (!browserReleased) {
                    failure = appendFailure(failure,
                            unconfirmedBrowserRelease(destroyed));
                }
            } catch (IOException | RuntimeException | LinkageError problem) {
                failure = appendFailure(failure, problem);
                nativeReleased = false;
                if (parentReleaseFailure != null) {
                    failure = appendFailure(failure, parentReleaseFailure);
                }
            }
        }
        // Native host retirement and BrowserProcessExited are deliberately
        // separate facts. The UDF remains owned and intact until the exact
        // browser PID is observed exiting (or native proves no browser began).
        if (browserReleased && userData != null) {
            try {
                deleteOwnedUserDataWithRetry(userData);
                userDataReleased = true;
            } catch (IOException | RuntimeException | LinkageError problem) {
                failure = appendFailure(failure, problem);
            }
        }
        if (nativeReleased && retainedArtifact != null) {
            try {
                retainedArtifact.close();
                artifactReleased = true;
            } catch (IOException | RuntimeException | LinkageError problem) {
                failure = appendFailure(failure, problem);
            }
        }
        return new CleanupResult(
                failure,
                session == null || nativeReleased,
                parentAttempted,
                parentReleased,
                browserReleased,
                userDataReleased,
                artifactReleased);
    }

    private static boolean hasDestroyFlag(
            WindowsWebView2NativeApi.DestroyResult result, int flag) {
        return (result.flags() & flag) != 0;
    }

    private static IOException unconfirmedBrowserRelease(
            WindowsWebView2NativeApi.DestroyResult result) {
        return new IOException(
                "Native WebView2 teardown released the host but did not confirm "
                + "browser-process/UDF release (expected PID "
                + Long.toUnsignedString(result.expectedBrowserProcessId())
                + ", observed PID "
                + Long.toUnsignedString(result.observedBrowserProcessId())
                + ", exit kind " + result.browserExitKind()
                + ", terminal HRESULT 0x"
                + String.format(java.util.Locale.ROOT, "%08X",
                        result.terminalHresult()) + ")");
    }

    private void deleteOwnedUserDataWithRetry(OwnedUserDataFolder userData)
            throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAXIMUM_USER_DATA_DELETE_ATTEMPTS; attempt++) {
            try {
                userData.delete();
                return;
            } catch (IOException failure) {
                lastFailure = failure;
                if (attempt == MAXIMUM_USER_DATA_DELETE_ATTEMPTS) {
                    break;
                }
                cleanupRetryDelay.awaitRetry(attempt, failure);
            }
        }
        throw lastFailure == null
                ? new IOException("WebView2 user-data cleanup failed") : lastFailure;
    }

    private static Throwable appendFailure(Throwable current, Throwable addition) {
        if (current == null) {
            return addition;
        }
        current.addSuppressed(addition);
        return current;
    }

    private void clearNativeEventQueues() {
        requireEdt();
        pendingNativeEvents.clear();
        pendingNativeEventCharacters = 0;
        synchronized (nativeEventLock) {
            nativeEventInbox.clear();
            nativeEventInboxCharacters = 0;
            nativeEventOverflow = false;
            nativeEventOverflowGeneration = 0;
            nativeEventDrainScheduled = false;
            nativeEventInboxEpoch++;
        }
    }

    private static long eventCharacters(WindowsWebView2NativeApi.Event event) {
        return (long) event.source().length() + event.payload().length();
    }

    private void completeClose(long expectedGeneration, CleanupResult result) {
        requireEdt();
        if (expectedGeneration != generation || phase == Phase.CLOSED) {
            return;
        }
        CleanupResult completed = Objects.requireNonNull(result, "result");
        if (completed.nativeReleased()) {
            nativeSession = null;
            parentWindow = 0;
        }
        parentReleaseAttempted |= completed.parentReleaseAttempted();
        parentReleaseConfirmed |= completed.parentReleaseConfirmed();
        browserProcessReleaseConfirmed |=
                completed.browserProcessReleaseConfirmed();
        if (completed.userDataReleased()) {
            ownedUserDataFolder = null;
        }
        if (completed.artifactReleased()) {
            artifactLease = null;
        }
        boolean fullyReleased = nativeSession == null
                && ownedUserDataFolder == null
                && artifactLease == null;
        phase = fullyReleased ? Phase.CLOSED : Phase.POISONED;
        peerRemovalBarrierComplete = fullyReleased;
        cancelStartupDeadline();
        initializationInFlight = false;
        teardownInFlight = false;
        documentReady = false;
        bridgeAuthenticated = false;
        transportFailureTerminalPending = false;
        bridge = null;
        clearNativeEventQueues();
        CompletableFuture<Void> completedFuture = closeCompletion;
        Listener completedListener = listener;
        Throwable failure = completed.failure();
        if (!fullyReleased && failure == null) {
            failure = new IOException(
                    "Web Canvas cleanup cannot release the remaining resources "
                    + "without browser-process/UDF release proof");
        }
        Throwable terminalFailure = failure;
        if (fullyReleased) {
            safeListener(() -> completedListener.closed(
                    terminalFailure == null ? "" : boundedReason(terminalFailure)));
        } else {
            String reason = boundedReason(terminalFailure);
            safeListener(() -> completedListener.failed(
                    "Release Web Canvas resources", reason));
        }
        if (expectedGeneration == generation
                && phase == Phase.CLOSED
                && listener == completedListener) {
            listener = Listener.NOOP;
        }
        if (terminalFailure == null && fullyReleased) {
            completedFuture.complete(null);
        } else {
            completedFuture.completeExceptionally(terminalFailure);
        }
    }

    /**
     * Executes blocking native lifecycle work away from the EDT even when an
     * injected executor runs commands inline. Rejection receives one bounded
     * virtual-thread fallback so teardown cannot leak the retained JNA
     * callback merely because an application executor is shutting down.
     */
    private void executeOffEdt(
            String threadName,
            Runnable command,
            Consumer<Throwable> schedulingFailure) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(schedulingFailure, "schedulingFailure");
        Runnable failureReportingCommand = () -> {
            try {
                command.run();
            } catch (RuntimeException | LinkageError failure) {
                EventQueue.invokeLater(() -> schedulingFailure.accept(failure));
            }
        };
        Runnable guarded = () -> {
            if (EventQueue.isDispatchThread()) {
                startFallbackThread(
                        threadName, failureReportingCommand, schedulingFailure);
            } else {
                failureReportingCommand.run();
            }
        };
        try {
            nativeExecutor.execute(guarded);
        } catch (RuntimeException | LinkageError rejected) {
            startFallbackThread(
                    threadName, failureReportingCommand, schedulingFailure);
        }
    }

    private static void startFallbackThread(
            String threadName,
            Runnable command,
            Consumer<Throwable> schedulingFailure) {
        try {
            Thread.ofVirtual().name(threadName).start(command);
        } catch (RuntimeException | LinkageError failure) {
            EventQueue.invokeLater(() -> schedulingFailure.accept(failure));
        }
    }

    private static void requireSafeArtifactDirectory(Path artifact) throws IOException {
        requireSafeDirectoryComponents(
                artifact, "Published Web Canvas artifact");
    }

    private static Path requireSafeDirectoryComponents(Path path, String label)
            throws IOException {
        Path normalized = Objects.requireNonNull(path, "path")
                .toAbsolutePath().normalize();
        Path root = normalized.getRoot();
        if (root == null) {
            throw new IOException(label + " has no filesystem root");
        }
        Path current = root;
        requireSafeDirectory(current, label);
        for (Path segment : root.relativize(normalized)) {
            current = current.resolve(segment);
            requireSafeDirectory(current, label);
        }
        return normalized;
    }

    private static BasicFileAttributes requireSafeDirectory(Path path, String label)
            throws IOException {
        rejectLinkOrReparse(path, label);
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isOther()) {
            throw new IOException(label + " is not a safe directory: " + path);
        }
        return attributes;
    }

    private static BasicFileAttributes requireRegularFile(Path path, String label)
            throws IOException {
        rejectLinkOrReparse(path, label);
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isOther()) {
            throw new IOException(label + " is not a regular file: " + path);
        }
        return attributes;
    }

    private static void rejectLinkOrReparse(Path path, String label) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path)) {
            return;
        }
        BasicFileAttributes noFollow = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (noFollow.isSymbolicLink() || noFollow.isOther()
                || Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a link or reparse point: " + path);
        }
        BasicFileAttributes followed = Files.readAttributes(
                path, BasicFileAttributes.class);
        if (noFollow.fileKey() != null && followed.fileKey() != null
                && !noFollow.fileKey().equals(followed.fileKey())) {
            throw new IOException(
                    label + " resolves through a link or reparse point: " + path);
        }
        try {
            Object attributes = Files.getAttribute(
                    path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            if (attributes instanceof Number value
                    && (value.intValue() & 0x400) != 0) {
                throw new IOException(
                        label + " must not be a Windows reparse point: " + path);
            }
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            // Non-Windows providers do not expose the raw DOS reparse bit.
        }
    }

    private static StableFileIdentity requireStableIdentity(Path path, String label)
            throws IOException {
        if (usesWindowsStableIdentity()) {
            return WindowsStableFileIdentity.read(path, label);
        }
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object key = attributes.fileKey();
        if (key == null) {
            throw new IOException(label
                    + " filesystem does not expose a stable file identity");
        }
        return new StableFileIdentity("nio", 0, new byte[0], key);
    }

    private static boolean usesWindowsStableIdentity() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    private static void requireSameIdentity(
            StableFileIdentity expected, Path path, String label) throws IOException {
        StableFileIdentity actual = requireStableIdentity(path, label);
        if (!Objects.requireNonNull(expected, "expected identity").equals(actual)) {
            throw new IOException(label + " identity changed during its lifetime");
        }
    }

    private static void writeMarker(Path marker, byte[] content) throws IOException {
        try (FileChannel output = FileChannel.open(
                marker,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer bytes = ByteBuffer.wrap(content);
            while (bytes.hasRemaining()) {
                output.write(bytes);
            }
            output.force(true);
        }
    }

    private static byte[] readBoundedFile(Path path, int maximum, String label)
            throws IOException {
        BasicFileAttributes attributes = requireRegularFile(path, label);
        if (attributes.size() < 0 || attributes.size() > maximum) {
            throw new IOException(label + " exceeds its size bound");
        }
        try (InputStream input = Files.newInputStream(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            byte[] value = input.readNBytes(maximum + 1);
            if (value.length > maximum || input.read() != -1) {
                throw new IOException(label + " exceeds its size bound");
            }
            return value;
        }
    }

    private static void deleteSafeOwnedTree(
            Path root, StableFileIdentity expectedRootIdentity, String label)
            throws IOException {
        if (usesWindowsStableIdentity()) {
            Path parent = root.getParent();
            if (parent == null) {
                throw new IOException(label + " has no lockable parent");
            }
            try (WindowsStableFileIdentity.LockedPath parentLock =
                    WindowsStableFileIdentity.lockParent(
                            parent, label + " parent");
                    WindowsStableFileIdentity.LockedPath rootLock =
                            WindowsStableFileIdentity.lockOwnedDirectory(root, label)) {
                rootLock.requireIdentity(expectedRootIdentity);
                deleteSafeOwnedTree(
                        root, expectedRootIdentity, label, false);
                rootLock.markForDeletion();
            }
            if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(label
                        + " remained after handle-based cleanup");
            }
            return;
        }
        deleteSafeOwnedTree(root, expectedRootIdentity, label, true);
    }

    private static void deleteSafeOwnedTree(
            Path root,
            StableFileIdentity expectedRootIdentity,
            String label,
            boolean deleteRootByPath)
            throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(directory, label);
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException(label + " contains an unsafe directory: "
                            + directory);
                }
                if (directory.equals(root)) {
                    requireSameIdentity(expectedRootIdentity, directory, label);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(file, label);
                if (!attributes.isRegularFile() || attributes.isOther()) {
                    throw new IOException(label + " contains a non-regular file: " + file);
                }
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path directory, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                if (deleteRootByPath || !directory.equals(root)) {
                    Files.delete(directory);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private record StableFileIdentity(
            String provider,
            long volumeSerialNumber,
            byte[] fileId,
            Object portableKey) {
        private StableFileIdentity {
            provider = Objects.requireNonNull(provider, "provider");
            fileId = Objects.requireNonNull(fileId, "fileId").clone();
        }

        @Override
        public byte[] fileId() {
            return fileId.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StableFileIdentity identity
                    && provider.equals(identity.provider)
                    && volumeSerialNumber == identity.volumeSerialNumber
                    && Arrays.equals(fileId, identity.fileId)
                    && Objects.equals(portableKey, identity.portableKey);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(provider, volumeSerialNumber, portableKey);
            return 31 * result + Arrays.hashCode(fileId);
        }
    }

    private static final class WindowsStableFileIdentity {
        private static StableFileIdentity read(Path path, String label)
                throws IOException {
            try (LockedPath locked = open(
                    path,
                    label,
                    WinNT.FILE_READ_ATTRIBUTES,
                    WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE
                            | WinNT.FILE_SHARE_DELETE)) {
                return locked.identity();
            }
        }

        private static LockedPath lockParent(Path path, String label)
                throws IOException {
            return open(
                    path,
                    label,
                    WinNT.FILE_READ_ATTRIBUTES,
                    WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE);
        }

        private static LockedPath lockOwnedDirectory(Path path, String label)
                throws IOException {
            return open(
                    path,
                    label,
                    WinNT.DELETE | WinNT.FILE_READ_ATTRIBUTES,
                    WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE);
        }

        private static LockedPath lockOwnedMarker(Path path, String label)
                throws IOException {
            return open(
                    path,
                    label,
                    WinNT.DELETE | WinNT.FILE_READ_ATTRIBUTES,
                    WinNT.FILE_SHARE_READ);
        }

        private static LockedPath open(
                Path path,
                String label,
                int desiredAccess,
                int shareMode)
                throws IOException {
            HANDLE handle = Kernel32.INSTANCE.CreateFile(
                    path.toString(),
                    desiredAccess,
                    shareMode,
                    null,
                    WinNT.OPEN_EXISTING,
                    WinNT.FILE_FLAG_BACKUP_SEMANTICS
                            | WinNT.FILE_FLAG_OPEN_REPARSE_POINT,
                    null);
            if (handle == null || handle.getPointer() == null
                    || Pointer.nativeValue(handle.getPointer()) == -1L) {
                throw windowsIdentityFailure(label, "CreateFile", Native.getLastError());
            }
            Throwable failure = null;
            try {
                return new LockedPath(
                        handle, queryIdentity(handle, path, label), path, label);
            } catch (IOException | RuntimeException | Error problem) {
                failure = problem;
                throw problem;
            } finally {
                if (failure != null && !Kernel32.INSTANCE.CloseHandle(handle)) {
                    IOException closeFailure = windowsIdentityFailure(
                            label, "CloseHandle", Native.getLastError());
                    failure.addSuppressed(closeFailure);
                }
            }
        }

        private static StableFileIdentity queryIdentity(
                HANDLE handle, Path path, String label) throws IOException {
            WinBase.FILE_ATTRIBUTE_TAG_INFO tag =
                    new WinBase.FILE_ATTRIBUTE_TAG_INFO();
            if (!Kernel32.INSTANCE.GetFileInformationByHandleEx(
                    handle, WinBase.FileAttributeTagInfo,
                    tag.getPointer(), new DWORD(tag.size()))) {
                throw windowsIdentityFailure(
                        label, "GetFileInformationByHandleEx(FileAttributeTagInfo)",
                        Native.getLastError());
            }
            tag.read();
            if ((tag.FileAttributes & WinNT.FILE_ATTRIBUTE_REPARSE_POINT) != 0) {
                throw new IOException(label
                        + " must not be a Windows reparse point: " + path);
            }
            WinBase.FILE_ID_INFO information = new WinBase.FILE_ID_INFO();
            if (!Kernel32.INSTANCE.GetFileInformationByHandleEx(
                    handle, WinBase.FileIdInfo,
                    information.getPointer(), new DWORD(information.size()))) {
                throw windowsIdentityFailure(
                        label, "GetFileInformationByHandleEx(FileIdInfo)",
                        Native.getLastError());
            }
            information.read();
            byte[] fileId = new byte[information.FileId.Identifier.length];
            for (int index = 0; index < fileId.length; index++) {
                fileId[index] = information.FileId.Identifier[index].byteValue();
            }
            return new StableFileIdentity(
                    "win32-file-id-128",
                    information.VolumeSerialNumber,
                    fileId,
                    null);
        }

        private static final class LockedPath implements AutoCloseable {
            private HANDLE handle;
            private final StableFileIdentity identity;
            private final Path path;
            private final String label;

            private LockedPath(
                    HANDLE handle,
                    StableFileIdentity identity,
                    Path path,
                    String label) {
                this.handle = handle;
                this.identity = identity;
                this.path = path;
                this.label = label;
            }

            private StableFileIdentity identity() {
                return identity;
            }

            private void requireIdentity(StableFileIdentity expected)
                    throws IOException {
                if (!Objects.requireNonNull(expected, "expected identity")
                        .equals(identity)) {
                    throw new IOException(label + " identity changed during its lifetime");
                }
            }

            private void markForDeletion() throws IOException {
                HANDLE current = handle;
                if (current == null) {
                    throw new IOException(label + " handle is already closed");
                }
                WinBase.FILE_DISPOSITION_INFO disposition =
                        new WinBase.FILE_DISPOSITION_INFO(true);
                disposition.write();
                if (!Kernel32.INSTANCE.SetFileInformationByHandle(
                        current,
                        WinBase.FileDispositionInfo,
                        disposition.getPointer(),
                        new DWORD(disposition.size()))) {
                    throw windowsIdentityFailure(
                            label, "SetFileInformationByHandle(FileDispositionInfo)",
                            Native.getLastError());
                }
            }

            @Override
            public void close() throws IOException {
                HANDLE current = handle;
                handle = null;
                if (current != null && !Kernel32.INSTANCE.CloseHandle(current)) {
                    throw windowsIdentityFailure(
                            label + " (" + path + ")",
                            "CloseHandle",
                            Native.getLastError());
                }
            }
        }

        private static IOException windowsIdentityFailure(
                String label, String operation, int error) {
            return new IOException(label + " stable identity query " + operation
                    + " failed with Win32 error " + Integer.toUnsignedString(error));
        }
    }

    private static void requireDisjoint(Path first, Path second, String label)
            throws IOException {
        Path left = first.toAbsolutePath().normalize();
        Path right = second.toAbsolutePath().normalize();
        if (left.equals(right) || left.startsWith(right) || right.startsWith(left)) {
            throw new IOException(label + " must be disjoint");
        }
    }

    static Path userDataMarkerPath(Path userDataFolder) {
        Path root = Objects.requireNonNull(userDataFolder, "userDataFolder")
                .toAbsolutePath().normalize();
        Path parent = root.getParent();
        Path name = root.getFileName();
        if (parent == null || name == null
                || !name.toString().matches("[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException(
                    "invalid WebView2 user-data session path");
        }
        return parent.resolve(USER_DATA_MARKER + name);
    }

    static final class UserDataSessionsRoot {
        private final Path parent;
        private final Path root;
        private final Path marker;
        private final StableFileIdentity parentIdentity;
        private final StableFileIdentity rootIdentity;
        private final StableFileIdentity markerIdentity;
        private final byte[] markerBytes;

        private UserDataSessionsRoot(
                Path parent,
                Path root,
                Path marker,
                StableFileIdentity parentIdentity,
                StableFileIdentity rootIdentity,
                StableFileIdentity markerIdentity,
                byte[] markerBytes) {
            this.parent = parent;
            this.root = root;
            this.marker = marker;
            this.parentIdentity = parentIdentity;
            this.rootIdentity = rootIdentity;
            this.markerIdentity = markerIdentity;
            this.markerBytes = markerBytes;
        }

        static UserDataSessionsRoot openOrCreate(Path requested) throws IOException {
            Path root = Objects.requireNonNull(requested, "sessionsRoot")
                    .toAbsolutePath().normalize();
            Path parent = root.getParent();
            if (parent == null || root.getFileName() == null) {
                throw new IOException("WebView2 sessions root has no safe parent");
            }
            Path safeParent = requireSafeDirectoryComponents(
                    parent, "WebView2 sessions-root parent");
            requireSafeDirectory(safeParent, "WebView2 sessions-root parent");
            StableFileIdentity parentIdentity = requireStableIdentity(
                    safeParent, "WebView2 sessions-root parent");
            rejectLinkOrReparse(root, "WebView2 sessions root");
            boolean created = false;
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(root);
                created = true;
            }
            StableFileIdentity rootIdentity = null;
            try {
                requireSafeDirectory(root, "WebView2 sessions root");
                rootIdentity = requireStableIdentity(
                        root, "WebView2 sessions root");
                Path marker = root.resolve(USER_DATA_SESSIONS_ROOT_MARKER);
                byte[] markerBytes;
                if (created) {
                    markerBytes = (USER_DATA_SESSIONS_ROOT_MARKER_FORMAT
                            + UUID.randomUUID().toString().replace("-", "") + "\n")
                            .getBytes(StandardCharsets.UTF_8);
                    writeMarker(marker, markerBytes);
                } else {
                    markerBytes = readBoundedFile(
                            marker, 128, "WebView2 sessions-root ownership marker");
                    String value = new String(markerBytes, StandardCharsets.UTF_8);
                    if (!value.matches(java.util.regex.Pattern.quote(
                            USER_DATA_SESSIONS_ROOT_MARKER_FORMAT)
                            + "[0-9a-f]{32}\\n")) {
                        throw new IOException(
                                "Existing WebView2 sessions root is not plugin-owned");
                    }
                }
                requireRegularFile(marker, "WebView2 sessions-root ownership marker");
                StableFileIdentity markerIdentity = requireStableIdentity(
                        marker, "WebView2 sessions-root ownership marker");
                return new UserDataSessionsRoot(
                        safeParent,
                        root,
                        marker,
                        parentIdentity,
                        rootIdentity,
                        markerIdentity,
                        markerBytes);
            } catch (IOException | RuntimeException | Error failure) {
                if (created && rootIdentity != null) {
                    try {
                        deleteSafeOwnedTree(root, rootIdentity,
                                "Incomplete WebView2 sessions root");
                    } catch (IOException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            }
        }

        Path root() {
            return root;
        }

        Path sessionPath(String name) {
            Objects.requireNonNull(name, "name");
            if (!name.matches("[a-z0-9][a-z0-9-]{0,63}")) {
                throw new IllegalArgumentException(
                        "invalid WebView2 user-data session name");
            }
            return root.resolve(name);
        }

        void verify() throws IOException {
            Path currentParent = requireSafeDirectoryComponents(
                    parent, "WebView2 sessions-root parent");
            if (!currentParent.equals(root.getParent())) {
                throw new IOException("WebView2 sessions root is no longer a direct child");
            }
            requireSafeDirectory(parent, "WebView2 sessions-root parent");
            requireSameIdentity(
                    parentIdentity, parent, "WebView2 sessions-root parent");
            requireSafeDirectory(root, "WebView2 sessions root");
            requireSameIdentity(rootIdentity, root, "WebView2 sessions root");
            BasicFileAttributes markerAttributes = requireRegularFile(
                    marker, "WebView2 sessions-root ownership marker");
            requireSameIdentity(markerIdentity, marker,
                    "WebView2 sessions-root ownership marker");
            if (markerAttributes.size() != markerBytes.length
                    || !java.util.Arrays.equals(
                            markerBytes,
                            readBoundedFile(
                                    marker, markerBytes.length + 1,
                                    "WebView2 sessions-root ownership marker"))) {
                throw new IOException(
                        "WebView2 sessions-root ownership marker changed");
            }
        }
    }

    private static final class OwnedUserDataFolder {
        private final UserDataSessionsRoot sessionsRoot;
        private final Path parent;
        private final Path root;
        private final Path marker;
        private final StableFileIdentity parentIdentity;
        private final StableFileIdentity rootIdentity;
        private final StableFileIdentity markerIdentity;
        private final byte[] markerBytes;

        private OwnedUserDataFolder(
                UserDataSessionsRoot sessionsRoot,
                Path parent,
                Path root,
                Path marker,
                StableFileIdentity parentIdentity,
                StableFileIdentity rootIdentity,
                StableFileIdentity markerIdentity,
                byte[] markerBytes) {
            this.sessionsRoot = sessionsRoot;
            this.parent = parent;
            this.root = root;
            this.marker = marker;
            this.parentIdentity = parentIdentity;
            this.rootIdentity = rootIdentity;
            this.markerIdentity = markerIdentity;
            this.markerBytes = markerBytes;
        }

        static OwnedUserDataFolder create(
                UserDataSessionsRoot sessionsRoot,
                Path requested,
                Path artifactRoot,
                String sessionNonce) throws IOException {
            Objects.requireNonNull(sessionsRoot, "sessionsRoot").verify();
            Path root = Objects.requireNonNull(requested, "userDataFolder")
                    .toAbsolutePath().normalize();
            Path parent = root.getParent();
            if (parent == null || root.getFileName() == null) {
                throw new IOException("WebView2 user-data folder has no safe parent");
            }
            Path safeParent = sessionsRoot.root();
            if (!safeParent.equals(parent)) {
                throw new IOException(
                        "WebView2 user-data folder is not a direct sessions-root child");
            }
            Path safeArtifact = requireSafeDirectoryComponents(
                    artifactRoot, "Published Web Canvas artifact");
            requireDisjoint(
                    safeParent, safeArtifact,
                    "WebView2 sessions root and Web Canvas publication");
            requireDisjoint(
                    root, safeArtifact,
                    "WebView2 user-data folder and Web Canvas publication");
            requireSafeDirectory(safeParent, "WebView2 user-data parent");
            StableFileIdentity parentIdentity = requireStableIdentity(
                    safeParent, "WebView2 user-data parent");
            rejectLinkOrReparse(root, "WebView2 user-data folder");
            if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "WebView2 user-data folder must not already exist: " + root);
            }

            Files.createDirectory(root);
            StableFileIdentity rootIdentity = null;
            Path createdMarker = null;
            StableFileIdentity createdMarkerIdentity = null;
            byte[] createdMarkerBytes = null;
            try {
                requireSafeDirectory(root, "WebView2 owned user-data folder");
                rootIdentity = requireStableIdentity(
                        root, "WebView2 owned user-data folder");
                Path marker = userDataMarkerPath(root);
                rejectLinkOrReparse(marker, "WebView2 user-data ownership marker");
                if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException(
                            "WebView2 user-data ownership marker already exists: " + marker);
                }
                byte[] markerBytes = (USER_DATA_MARKER_FORMAT + sessionNonce + "|"
                        + UUID.randomUUID().toString().replace("-", "") + "\n")
                        .getBytes(StandardCharsets.UTF_8);
                writeMarker(marker, markerBytes);
                createdMarker = marker;
                createdMarkerBytes = markerBytes;
                requireRegularFile(marker, "WebView2 user-data ownership marker");
                createdMarkerIdentity = requireStableIdentity(
                        marker, "WebView2 user-data ownership marker");
                OwnedUserDataFolder owned = new OwnedUserDataFolder(
                        sessionsRoot,
                        safeParent,
                        root,
                        marker,
                        parentIdentity,
                        rootIdentity,
                        createdMarkerIdentity,
                        markerBytes);
                owned.verify();
                return owned;
            } catch (IOException | RuntimeException | Error failure) {
                if (rootIdentity != null) {
                    try {
                        deleteSafeOwnedTree(root, rootIdentity,
                                "Incomplete WebView2 user-data folder");
                    } catch (IOException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                if (createdMarker != null) {
                    try {
                        deleteMarkerIfExactlyOwned(
                                createdMarker,
                                createdMarkerIdentity,
                                createdMarkerBytes);
                    } catch (IOException cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            }
        }

        private static void deleteMarkerIfExactlyOwned(
                Path marker,
                StableFileIdentity expectedIdentity,
                byte[] expectedBytes)
                throws IOException {
            if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            if (expectedIdentity == null) {
                throw new IOException(
                        "Incomplete WebView2 ownership marker has no stable identity");
            }
            if (usesWindowsStableIdentity()) {
                Path parent = marker.getParent();
                if (parent == null) {
                    throw new IOException(
                            "Incomplete WebView2 ownership marker has no parent");
                }
                try (WindowsStableFileIdentity.LockedPath parentLock =
                        WindowsStableFileIdentity.lockParent(
                                parent,
                                "Incomplete WebView2 ownership-marker parent");
                        WindowsStableFileIdentity.LockedPath markerLock =
                                WindowsStableFileIdentity.lockOwnedMarker(
                                        marker,
                                        "Incomplete WebView2 ownership marker")) {
                    markerLock.requireIdentity(expectedIdentity);
                    requireExactMarkerBytes(marker, expectedBytes);
                    markerLock.markForDeletion();
                }
                if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException(
                            "Incomplete WebView2 ownership marker remained after "
                            + "handle-based cleanup");
                }
                return;
            }
            BasicFileAttributes attributes = requireRegularFile(
                    marker, "Incomplete WebView2 ownership marker");
            requireSameIdentity(expectedIdentity, marker,
                    "Incomplete WebView2 ownership marker");
            if (expectedBytes == null || attributes.size() != expectedBytes.length) {
                throw new IOException(
                        "Incomplete WebView2 ownership marker changed");
            }
            requireExactMarkerBytes(marker, expectedBytes);
            Files.delete(marker);
        }

        private static void requireExactMarkerBytes(
                Path marker, byte[] expectedBytes) throws IOException {
            if (expectedBytes == null
                    || !java.util.Arrays.equals(
                            expectedBytes,
                            readBoundedFile(
                                    marker,
                                    expectedBytes.length + 1,
                                    "Incomplete WebView2 ownership marker"))) {
                throw new IOException(
                        "Incomplete WebView2 ownership marker changed");
            }
        }

        void delete() throws IOException {
            if (usesWindowsStableIdentity()) {
                deleteOnWindowsWithLockedIdentity();
                return;
            }
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                verifyMarkerOnly();
                Files.delete(marker);
                return;
            }
            verify();
            deleteSafeOwnedTree(
                    root, rootIdentity, "WebView2 owned user-data folder");
            if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "WebView2 owned user-data folder reappeared during cleanup");
            }
            verifyMarkerOnly();
            Files.delete(marker);
        }

        private void deleteOnWindowsWithLockedIdentity() throws IOException {
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)
                    && !Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            try (WindowsStableFileIdentity.LockedPath parentLock =
                    WindowsStableFileIdentity.lockParent(
                            parent, "WebView2 user-data parent")) {
                parentLock.requireIdentity(parentIdentity);
                try (WindowsStableFileIdentity.LockedPath markerLock =
                        WindowsStableFileIdentity.lockOwnedMarker(
                                marker, "WebView2 user-data ownership marker")) {
                    markerLock.requireIdentity(markerIdentity);
                    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                        try (WindowsStableFileIdentity.LockedPath rootLock =
                                WindowsStableFileIdentity.lockOwnedDirectory(
                                        root, "WebView2 owned user-data folder")) {
                            rootLock.requireIdentity(rootIdentity);
                            verify();
                            deleteSafeOwnedTree(
                                    root,
                                    rootIdentity,
                                    "WebView2 owned user-data folder",
                                    false);
                            rootLock.markForDeletion();
                        }
                        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                            throw new IOException(
                                    "WebView2 owned user-data folder remained after "
                                    + "handle-based cleanup");
                        }
                    }
                    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException(
                                "WebView2 owned user-data folder reappeared during cleanup");
                    }
                    verifyMarkerOnly();
                    markerLock.markForDeletion();
                }
            }
            if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "WebView2 ownership marker remained after handle-based cleanup");
            }
        }

        void verify() throws IOException {
            sessionsRoot.verify();
            Path currentParent = requireSafeDirectoryComponents(
                    parent, "WebView2 user-data parent");
            if (!currentParent.equals(root.getParent())) {
                throw new IOException(
                        "WebView2 user-data folder is no longer a direct child");
            }
            requireSafeDirectory(parent, "WebView2 user-data parent");
            requireSameIdentity(
                    parentIdentity, parent, "WebView2 user-data parent");
            requireSafeDirectory(root, "WebView2 owned user-data folder");
            requireSameIdentity(
                    rootIdentity, root, "WebView2 owned user-data folder");
            verifyMarkerOnly();
        }

        private void verifyMarkerOnly() throws IOException {
            sessionsRoot.verify();
            requireSafeDirectory(parent, "WebView2 user-data parent");
            requireSameIdentity(
                    parentIdentity, parent, "WebView2 user-data parent");
            BasicFileAttributes markerAttributes = requireRegularFile(
                    marker, "WebView2 user-data ownership marker");
            requireSameIdentity(markerIdentity, marker,
                    "WebView2 user-data ownership marker");
            if (markerAttributes.size() != markerBytes.length
                    || !java.util.Arrays.equals(
                            markerBytes,
                            readBoundedFile(
                                    marker, markerBytes.length + 1,
                                    "WebView2 user-data ownership marker"))) {
                throw new IOException(
                        "WebView2 user-data ownership marker changed");
            }
        }

    }

    private static String boundedReason(Throwable failure) {
        if (failure == null) {
            return "unknown failure";
        }
        String message = failure.getMessage();
        String reason = message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
        reason = reason.replace('\r', ' ').replace('\n', ' ').strip();
        return reason.length() <= 2_048 ? reason : reason.substring(0, 2_048);
    }

    private void safeListener(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException ignored) {
            // UI callbacks cannot poison native/session lifetime.
        }
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("Web Canvas host lifecycle must run on the EDT");
        }
    }

    enum Phase {
        NEW,
        STARTING,
        RUNNING,
        FAILED,
        CLOSING,
        POISONED,
        CLOSED
    }

    private record CleanupResult(
            Throwable failure,
            boolean nativeReleased,
            boolean parentReleaseAttempted,
            boolean parentReleaseConfirmed,
            boolean browserProcessReleaseConfirmed,
            boolean userDataReleased,
            boolean artifactReleased) {
        private static CleanupResult empty() {
            return new CleanupResult(null, true, true, true, true, true, true);
        }

        private static CleanupResult notStarted(
                Throwable failure,
                WindowsWebView2NativeApi.NativeSession session,
                OwnedUserDataFolder userData,
                WebCanvasArtifactPublisher.PublishedArtifact.Lease artifact,
                boolean parentReleaseAttempted,
                boolean parentReleaseConfirmed,
                boolean browserProcessReleaseConfirmed) {
            return new CleanupResult(
                    Objects.requireNonNull(failure, "failure"),
                    session == null,
                    parentReleaseAttempted,
                    parentReleaseConfirmed,
                    browserProcessReleaseConfirmed,
                    userData == null,
                    artifact == null);
        }
    }

    private record QueuedNativeEvent(
            long expectedGeneration,
            WindowsWebView2NativeApi.Event event,
            long characters) {
    }

    record StartRequest(
            WebCanvasArtifactPublisher.PublishedArtifact artifact,
            UserDataSessionsRoot userDataSessionsRoot,
            Path userDataFolder,
            String sessionNonce) {
        StartRequest {
            artifact = Objects.requireNonNull(artifact, "artifact");
            userDataSessionsRoot = Objects.requireNonNull(
                    userDataSessionsRoot, "userDataSessionsRoot");
            userDataFolder = Objects.requireNonNull(userDataFolder, "userDataFolder")
                    .toAbsolutePath().normalize();
            if (!userDataSessionsRoot.root().equals(userDataFolder.getParent())) {
                throw new IllegalArgumentException(
                        "WebView2 user-data folder must be a direct sessions-root child");
            }
            if (sessionNonce == null || !sessionNonce.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "Web Canvas session nonce must be 64 lowercase hexadecimal characters");
            }
        }
    }

    interface Listener {
        Listener NOOP = new Listener() {};

        default void nativeStarted(String runtimeVersion, String documentUri) {}

        default void documentReady(String documentUri) {}

        default void bridgeReady() {}

        default void diagnostic(String message) {}

        default void bridgeTerminal(WebCanvasHostBridge.Terminal terminal) {}

        default void failed(String operation, String reason) {}

        default void closed(String teardownFailure) {}
    }

    @FunctionalInterface
    interface NativeDependencyLoader {
        NativeDependencies load() throws IOException;

        static NativeDependencyLoader system() {
            return () -> new NativeDependencies(
                    JnaWindowsWebView2NativeApi.loadPackaged(),
                    ClientBoundsResolver.system(),
                    WindowsWebCanvasFocusController.system());
        }
    }

    record NativeDependencies(
            WindowsWebView2NativeApi nativeApi,
            ClientBoundsResolver clientBoundsResolver,
            WindowsWebCanvasFocusController focusController) {
        NativeDependencies {
            Objects.requireNonNull(nativeApi, "nativeApi");
            Objects.requireNonNull(clientBoundsResolver, "clientBoundsResolver");
            Objects.requireNonNull(focusController, "focusController");
        }
    }

    interface StartupDeadlineScheduler {
        Cancellation schedule(Duration timeout, Runnable callback);

        interface Cancellation {
            void cancel();
        }

        static StartupDeadlineScheduler system() {
            return (timeout, callback) -> {
                Objects.requireNonNull(timeout, "timeout");
                Objects.requireNonNull(callback, "callback");
                if (timeout.isZero() || timeout.isNegative()) {
                    throw new IllegalArgumentException(
                            "Web Canvas startup timeout must be positive");
                }
                AtomicBoolean armed = new AtomicBoolean(true);
                Thread timer = Thread.ofVirtual()
                        .name("flutter-webview2-startup-deadline")
                        .unstarted(() -> {
                            try {
                                Thread.sleep(timeout.toMillis());
                            } catch (InterruptedException cancelled) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            if (armed.compareAndSet(true, false)) {
                                callback.run();
                            }
                        });
                timer.start();
                return () -> {
                    if (armed.compareAndSet(true, false)) {
                        timer.interrupt();
                    }
                };
            };
        }
    }

    interface ClientBoundsResolver {
        NativeCanvasWindowBounds resolve(long parentWindow);

        default int devicePixelRatioMicros(long parentWindow) {
            return CanvasSurfaceMetrics.MICROS_PER_UNIT;
        }

        static ClientBoundsResolver system() {
            JnaWindowsNativeCanvasApi windows = new JnaWindowsNativeCanvasApi();
            return new ClientBoundsResolver() {
                @Override
                public NativeCanvasWindowBounds resolve(long parentWindow) {
                    return windows.clientBounds(parentWindow);
                }

                @Override
                public int devicePixelRatioMicros(long parentWindow) {
                    int dpi = windows.windowDpi(parentWindow);
                    long micros = Math.round(
                            dpi * (double) CanvasSurfaceMetrics.MICROS_PER_UNIT
                            / 96.0d);
                    if (micros < CanvasSurfaceMetrics.MIN_DEVICE_PIXEL_RATIO_MICROS
                            || micros > CanvasSurfaceMetrics.MAX_DEVICE_PIXEL_RATIO_MICROS) {
                        throw new IllegalStateException(
                                "GetDpiForWindow returned an unsupported Web Canvas DPI");
                    }
                    return (int) micros;
                }
            };
        }
    }

    interface CleanupRetryDelay {
        void awaitRetry(int failedAttempt, IOException failure) throws IOException;

        static CleanupRetryDelay system() {
            return (failedAttempt, failure) -> {
                Objects.requireNonNull(failure, "failure");
                long delayMillis = switch (failedAttempt) {
                    case 1 -> 25L;
                    case 2 -> 100L;
                    default -> 250L;
                };
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Interrupted while retrying WebView2 user-data cleanup",
                            interrupted);
                }
            };
        }
    }

    interface ParentHandleResolver {
        long resolve(Canvas canvas);

        static ParentHandleResolver system() {
            return canvas -> {
                if (!canvas.isDisplayable()) {
                    throw new IllegalStateException(
                            "The Web Canvas AWT carrier is not displayable");
                }
                Pointer pointer = Native.getComponentPointer(canvas);
                long value = pointer == null ? 0 : Pointer.nativeValue(pointer);
                if (value == 0) {
                    throw new IllegalStateException(
                            "The Web Canvas AWT carrier has no native HWND");
                }
                return value;
            };
        }
    }

    private final class HostCanvas extends Canvas {
        @Override
        public void addNotify() {
            super.addNotify();
        }

        @Override
        public void removeNotify() {
            // Never block the EDT/tree lock on the WebView2 STA: reparenting
            // can synchronously send Win32 messages back to AWT. Product code
            // must await preparePeerRemovalAsync() before reaching this guard.
            boolean unexpectedPeerLoss = phase != Phase.NEW
                    && phase != Phase.CLOSED
                    && !peerRemovalBarrierComplete;
            try {
                if (unexpectedPeerLoss) {
                    if (EventQueue.isDispatchThread()) {
                        safeListener(() -> listener.failed(
                                "Release Web Canvas before AWT peer loss",
                                "The heavyweight AWT peer was removed before its "
                                + "asynchronous WebView2 teardown barrier completed"));
                        beginClose(generation);
                    } else {
                        WindowsWebCanvasHost.this.close();
                    }
                }
            } finally {
                super.removeNotify();
            }
        }
    }
}
