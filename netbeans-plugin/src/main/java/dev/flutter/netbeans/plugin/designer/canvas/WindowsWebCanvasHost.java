package dev.flutter.netbeans.plugin.designer.canvas;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
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
    static final String USER_DATA_MARKER =
            ".netbeans-flutter-webview2-owned";
    static final String USER_DATA_SESSIONS_ROOT_MARKER =
            ".netbeans-flutter-webview2-sessions-root";
    private static final String USER_DATA_MARKER_FORMAT =
            "NETBEANS_FLUTTER_WEBVIEW2_USER_DATA|1|";
    private static final String USER_DATA_SESSIONS_ROOT_MARKER_FORMAT =
            "NETBEANS_FLUTTER_WEBVIEW2_SESSIONS_ROOT|1|";

    private final WindowsWebView2NativeApi nativeApi;
    private final Executor nativeExecutor;
    private final ParentHandleResolver parentHandleResolver;
    private final StartupDeadlineScheduler startupDeadlineScheduler;
    private final ClientBoundsResolver clientBoundsResolver;
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
    private boolean documentReady;
    private boolean bridgeAuthenticated;
    private volatile WindowsWebView2NativeApi.NativeSession nativeSession;
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
        return new WindowsWebCanvasHost(
                JnaWindowsWebView2NativeApi.loadPackaged(),
                command -> Thread.ofVirtual()
                        .name("flutter-webview2-native-lifecycle")
                        .start(command),
                ParentHandleResolver.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                StartupDeadlineScheduler.system(), ClientBoundsResolver.system(),
                CleanupRetryDelay.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, ClientBoundsResolver.system(),
                CleanupRetryDelay.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver) {
        this(nativeApi, nativeExecutor, parentHandleResolver,
                startupDeadlineScheduler, clientBoundsResolver,
                CleanupRetryDelay.system());
    }

    WindowsWebCanvasHost(
            WindowsWebView2NativeApi nativeApi,
            Executor nativeExecutor,
            ParentHandleResolver parentHandleResolver,
            StartupDeadlineScheduler startupDeadlineScheduler,
            ClientBoundsResolver clientBoundsResolver,
            CleanupRetryDelay cleanupRetryDelay) {
        super(new BorderLayout());
        this.nativeApi = Objects.requireNonNull(nativeApi, "nativeApi");
        this.nativeExecutor = Objects.requireNonNull(nativeExecutor, "nativeExecutor");
        this.parentHandleResolver = Objects.requireNonNull(
                parentHandleResolver, "parentHandleResolver");
        this.startupDeadlineScheduler = Objects.requireNonNull(
                startupDeadlineScheduler, "startupDeadlineScheduler");
        this.clientBoundsResolver = Objects.requireNonNull(
                clientBoundsResolver, "clientBoundsResolver");
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
            public void componentShown(ComponentEvent event) {
                publishVisibility(true);
            }

            @Override
            public void componentHidden(ComponentEvent event) {
                publishVisibility(false);
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
        if (phase != Phase.NEW && phase != Phase.CLOSED) {
            throw new IllegalStateException("Web Canvas host is already active");
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
        NativeCanvasWindowBounds clientBounds = physicalClientBounds(parent);
        WindowsWebView2NativeApi.CreateRequest nativeRequest =
                new WindowsWebView2NativeApi.CreateRequest(
                        parent, 0, 0, clientBounds.width(), clientBounds.height(),
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
        documentReady = false;
        bridgeAuthenticated = false;
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

    CompletableFuture<Void> closeAsync() {
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
        try {
            requireSafeArtifactDirectory(request.contentRoot());
            createdUserData = OwnedUserDataFolder.create(
                    userDataSessionsRoot, request.userDataFolder(),
                    request.contentRoot(), request.sessionNonce());
            runtime = nativeApi.runtimeVersion();
            createdUserData.verify();
            created = nativeApi.create(
                    request,
                    event -> acceptNativeEvent(expectedGeneration, event));
        } catch (IOException | RuntimeException | LinkageError problem) {
            failure = problem;
        }
        WindowsWebView2NativeApi.NativeSession result = created;
        OwnedUserDataFolder resultUserData = createdUserData;
        String detectedRuntime = runtime;
        Throwable detectedFailure = failure;
        EventQueue.invokeLater(() -> finishInitialization(
                expectedGeneration, result, resultUserData, retainedArtifact,
                detectedRuntime, originPolicy.indexUri(), detectedFailure));
    }

    private void finishInitialization(
            long expectedGeneration,
            WindowsWebView2NativeApi.NativeSession created,
            OwnedUserDataFolder createdUserData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact,
            String runtime,
            String documentUri,
            Throwable failure) {
        requireEdt();
        initializationInFlight = false;
        if (expectedGeneration != generation) {
            destroyDetachedOffEdt(created, createdUserData, retainedArtifact);
            return;
        }
        ownedUserDataFolder = createdUserData;
        if (phase == Phase.CLOSING || phase == Phase.CLOSED) {
            pendingNativeEvents.clear();
            pendingNativeEventCharacters = 0;
            nativeSession = created;
            startTeardownIfReady(expectedGeneration);
            return;
        }
        if (failure != null || created == null) {
            failOnEdt(expectedGeneration, "Start Web Canvas", failure == null
                    ? new IOException("native WebView2 session was not created") : failure);
            return;
        }
        nativeSession = created;
        try {
            listener.nativeStarted(runtime, documentUri);
        } catch (RuntimeException ignored) {
            // UI callbacks cannot poison native lifetime.
        }
        publishBounds();
        publishVisibility(isShowing());
        drainPendingNativeEvents(expectedGeneration);
    }

    private void acceptNativeEvent(
            long expectedGeneration,
            WindowsWebView2NativeApi.Event event) {
        Objects.requireNonNull(event, "event");
        if (expectedGeneration != generation || phase == Phase.CLOSED
                || phase == Phase.CLOSING) {
            return;
        }
        long characters = eventCharacters(event);
        boolean scheduleDrain = false;
        long drainEpoch = 0;
        synchronized (nativeEventLock) {
            if (expectedGeneration != generation || phase == Phase.CLOSED
                    || phase == Phase.CLOSING || nativeEventOverflow) {
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
        if (expectedGeneration != generation || phase == Phase.CLOSED
                || phase == Phase.CLOSING) {
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
                && phase != Phase.CLOSING
                && phase != Phase.CLOSED
                && !pendingNativeEvents.isEmpty()) {
            WindowsWebView2NativeApi.Event event = pendingNativeEvents.removeFirst();
            pendingNativeEventCharacters -= eventCharacters(event);
            dispatchNativeEvent(expectedGeneration, event);
        }
        if (phase == Phase.CLOSING || phase == Phase.CLOSED) {
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
        if (expectedGeneration == generation && phase != Phase.CLOSED) {
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
        if (expectedGeneration != generation || phase == Phase.CLOSED) {
            return;
        }
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
            NativeCanvasWindowBounds bounds = physicalClientBounds(parentWindow);
            session.setBounds(0, 0, bounds.width(), bounds.height());
        } catch (IOException | RuntimeException failure) {
            failOnEdt(generation, "Resize Web Canvas", failure);
        }
    }

    private NativeCanvasWindowBounds physicalClientBounds(long window) {
        if (window == 0) {
            throw new IllegalStateException(
                    "The Web Canvas AWT carrier has no native HWND");
        }
        NativeCanvasWindowBounds raw = Objects.requireNonNull(
                clientBoundsResolver.resolve(window), "native client bounds");
        int width = Math.max(1, raw.width());
        int height = Math.max(1, raw.height());
        if (width > 32_767 || height > 32_767) {
            throw new IllegalStateException(
                    "Web Canvas physical client bounds exceed the native range");
        }
        return new NativeCanvasWindowBounds(width, height);
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
                || phase == Phase.CLOSED) {
            return;
        }
        phase = Phase.FAILED;
        String reason = boundedReason(failure);
        safeListener(() -> listener.failed(operation, reason));
        if (bridge != null) {
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
        nativeSession = null;
        ownedUserDataFolder = null;
        artifactLease = null;
        if (session == null && userData == null && retainedArtifact == null) {
            completeClose(expectedGeneration, null);
            return;
        }
        teardownInFlight = true;
        executeOffEdt("flutter-webview2-native-destroy", () -> {
            Throwable result = cleanupResources(
                    session, userData, retainedArtifact);
            EventQueue.invokeLater(() -> completeClose(expectedGeneration, result));
        }, failure -> completeClose(expectedGeneration, failure));
    }

    private void destroyDetachedOffEdt(
            WindowsWebView2NativeApi.NativeSession session,
            OwnedUserDataFolder userData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact) {
        if (session == null && userData == null && retainedArtifact == null) {
            return;
        }
        executeOffEdt("flutter-webview2-native-detached-destroy", () ->
                cleanupResources(session, userData, retainedArtifact),
                ignored -> { });
    }

    private Throwable cleanupResources(
            WindowsWebView2NativeApi.NativeSession session,
            OwnedUserDataFolder userData,
            WebCanvasArtifactPublisher.PublishedArtifact.Lease retainedArtifact) {
        Throwable failure = null;
        boolean nativeReleased = true;
        if (session != null) {
            try {
                session.destroy();
            } catch (IOException | RuntimeException | LinkageError problem) {
                failure = appendFailure(failure, problem);
                nativeReleased = false;
            }
        }
        // A failed destroy cannot prove that the browser stopped using either
        // tree. Leaking the owned UDF and publication lease is deliberate:
        // deleting them under a possibly live WebView2 process would be a
        // use-after-delete and origin-integrity failure.
        if (nativeReleased && userData != null) {
            try {
                deleteOwnedUserDataWithRetry(userData);
            } catch (IOException | RuntimeException problem) {
                failure = appendFailure(failure, problem);
            }
        }
        if (nativeReleased && retainedArtifact != null) {
            try {
                retainedArtifact.close();
            } catch (IOException | RuntimeException problem) {
                failure = appendFailure(failure, problem);
            }
        }
        return failure;
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

    private void completeClose(long expectedGeneration, Throwable failure) {
        requireEdt();
        if (expectedGeneration != generation || phase == Phase.CLOSED) {
            return;
        }
        phase = Phase.CLOSED;
        cancelStartupDeadline();
        initializationInFlight = false;
        teardownInFlight = false;
        documentReady = false;
        bridgeAuthenticated = false;
        parentWindow = 0;
        nativeSession = null;
        ownedUserDataFolder = null;
        artifactLease = null;
        bridge = null;
        clearNativeEventQueues();
        CompletableFuture<Void> completedFuture = closeCompletion;
        Listener completedListener = listener;
        safeListener(() -> completedListener.closed(
                failure == null ? "" : boundedReason(failure)));
        if (expectedGeneration == generation
                && phase == Phase.CLOSED
                && listener == completedListener) {
            listener = Listener.NOOP;
        }
        if (failure == null) {
            completedFuture.complete(null);
        } else {
            completedFuture.completeExceptionally(failure);
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
        Runnable guarded = () -> {
            if (EventQueue.isDispatchThread()) {
                startFallbackThread(threadName, command, schedulingFailure);
            } else {
                command.run();
            }
        };
        try {
            nativeExecutor.execute(guarded);
        } catch (RuntimeException rejected) {
            startFallbackThread(threadName, command, schedulingFailure);
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

    private static void requireSameFileKey(
            Object expected, BasicFileAttributes actual, String label) throws IOException {
        if (expected != null && !expected.equals(actual.fileKey())) {
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
            Path root, Object expectedRootKey, String label) throws IOException {
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
                    requireSameFileKey(expectedRootKey, attributes, label);
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
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void requireDisjoint(Path first, Path second, String label)
            throws IOException {
        Path left = first.toAbsolutePath().normalize();
        Path right = second.toAbsolutePath().normalize();
        if (left.equals(right) || left.startsWith(right) || right.startsWith(left)) {
            throw new IOException(label + " must be disjoint");
        }
    }

    static final class UserDataSessionsRoot {
        private final Path parent;
        private final Path root;
        private final Path marker;
        private final Object parentFileKey;
        private final Object rootFileKey;
        private final Object markerFileKey;
        private final byte[] markerBytes;

        private UserDataSessionsRoot(
                Path parent,
                Path root,
                Path marker,
                Object parentFileKey,
                Object rootFileKey,
                Object markerFileKey,
                byte[] markerBytes) {
            this.parent = parent;
            this.root = root;
            this.marker = marker;
            this.parentFileKey = parentFileKey;
            this.rootFileKey = rootFileKey;
            this.markerFileKey = markerFileKey;
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
            BasicFileAttributes parentAttributes = requireSafeDirectory(
                    safeParent, "WebView2 sessions-root parent");
            rejectLinkOrReparse(root, "WebView2 sessions root");
            boolean created = false;
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(root);
                created = true;
            }
            BasicFileAttributes rootAttributes = null;
            try {
                rootAttributes = requireSafeDirectory(
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
                BasicFileAttributes markerAttributes = requireRegularFile(
                        marker, "WebView2 sessions-root ownership marker");
                return new UserDataSessionsRoot(
                        safeParent,
                        root,
                        marker,
                        parentAttributes.fileKey(),
                        rootAttributes.fileKey(),
                        markerAttributes.fileKey(),
                        markerBytes);
            } catch (IOException | RuntimeException | Error failure) {
                if (created) {
                    try {
                        deleteSafeOwnedTree(root,
                                rootAttributes == null ? null : rootAttributes.fileKey(),
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
            requireSameFileKey(
                    parentFileKey,
                    requireSafeDirectory(parent, "WebView2 sessions-root parent"),
                    "WebView2 sessions-root parent");
            requireSameFileKey(
                    rootFileKey,
                    requireSafeDirectory(root, "WebView2 sessions root"),
                    "WebView2 sessions root");
            BasicFileAttributes markerAttributes = requireRegularFile(
                    marker, "WebView2 sessions-root ownership marker");
            requireSameFileKey(
                    markerFileKey, markerAttributes,
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
        private final Object parentFileKey;
        private final Object rootFileKey;
        private final Object markerFileKey;
        private final byte[] markerBytes;

        private OwnedUserDataFolder(
                UserDataSessionsRoot sessionsRoot,
                Path parent,
                Path root,
                Path marker,
                Object parentFileKey,
                Object rootFileKey,
                Object markerFileKey,
                byte[] markerBytes) {
            this.sessionsRoot = sessionsRoot;
            this.parent = parent;
            this.root = root;
            this.marker = marker;
            this.parentFileKey = parentFileKey;
            this.rootFileKey = rootFileKey;
            this.markerFileKey = markerFileKey;
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
            BasicFileAttributes parentAttributes = requireSafeDirectory(
                    safeParent, "WebView2 user-data parent");
            rejectLinkOrReparse(root, "WebView2 user-data folder");
            if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "WebView2 user-data folder must not already exist: " + root);
            }

            Files.createDirectory(root);
            BasicFileAttributes rootAttributes = null;
            try {
                rootAttributes = requireSafeDirectory(
                        root, "WebView2 owned user-data folder");
                Path marker = root.resolve(USER_DATA_MARKER);
                byte[] markerBytes = (USER_DATA_MARKER_FORMAT + sessionNonce + "|"
                        + UUID.randomUUID().toString().replace("-", "") + "\n")
                        .getBytes(StandardCharsets.UTF_8);
                writeMarker(marker, markerBytes);
                BasicFileAttributes markerAttributes = requireRegularFile(
                        marker, "WebView2 user-data ownership marker");
                OwnedUserDataFolder owned = new OwnedUserDataFolder(
                        sessionsRoot,
                        safeParent,
                        root,
                        marker,
                        parentAttributes.fileKey(),
                        rootAttributes.fileKey(),
                        markerAttributes.fileKey(),
                        markerBytes);
                owned.verify();
                return owned;
            } catch (IOException | RuntimeException | Error failure) {
                try {
                    deleteSafeOwnedTree(root,
                            rootAttributes == null ? null : rootAttributes.fileKey(),
                            "Incomplete WebView2 user-data folder");
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
        }

        void delete() throws IOException {
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            verify();
            deleteSafeOwnedTree(
                    root, rootFileKey, "WebView2 owned user-data folder");
        }

        void verify() throws IOException {
            sessionsRoot.verify();
            Path currentParent = requireSafeDirectoryComponents(
                    parent, "WebView2 user-data parent");
            if (!currentParent.equals(root.getParent())) {
                throw new IOException(
                        "WebView2 user-data folder is no longer a direct child");
            }
            requireSameFileKey(
                    parentFileKey,
                    requireSafeDirectory(parent, "WebView2 user-data parent"),
                    "WebView2 user-data parent");
            requireSameFileKey(
                    rootFileKey,
                    requireSafeDirectory(root, "WebView2 owned user-data folder"),
                    "WebView2 owned user-data folder");
            BasicFileAttributes markerAttributes = requireRegularFile(
                    marker, "WebView2 user-data ownership marker");
            requireSameFileKey(
                    markerFileKey, markerAttributes,
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
        CLOSED
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

        static ClientBoundsResolver system() {
            JnaWindowsNativeCanvasApi windows = new JnaWindowsNativeCanvasApi();
            return parentWindow -> {
                if (windows.windowDpi(parentWindow) <= 0) {
                    throw new IllegalStateException(
                            "GetDpiForWindow failed for the Web Canvas HWND");
                }
                return windows.clientBounds(parentWindow);
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
            // Fence the generation before AWT destroys the heavyweight parent.
            // Native destroy remains off-EDT; late callbacks are rejected by
            // the CLOSING phase even if the OS tears down the parent first.
            if (EventQueue.isDispatchThread()) {
                beginClose(generation);
            } else {
                WindowsWebCanvasHost.this.close();
            }
            super.removeNotify();
        }
    }
}
