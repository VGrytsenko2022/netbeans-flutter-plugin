package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasSurfaceMetrics;
import java.awt.EventQueue;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.swing.JComponent;
import org.openide.modules.Places;

/**
 * Narrow product boundary around one exact Windows Flutter Web runtime.
 *
 * <p>The Designer package never receives a mutable build directory, browser
 * controller, publication path, user-data folder, document URI, session nonce,
 * or native API handle. It receives only the heavyweight component, the exact
 * expected Flutter engine identity, and an authenticated NBFC byte-stream pair.
 * Every producer and filesystem/native resource is fenced by one generation;
 * peer removal succeeds only after that generation is quiescent and the EDT
 * has published the final state.</p>
 */
public final class WindowsWebCanvasRuntime implements AutoCloseable {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CACHE_DIRECTORY = "flutter-web-canvas";
    private static final String USER_DATA_DIRECTORY = "webview2-sessions";

    private final WindowsWebCanvasHost host;
    private final BuildStarter builds;
    private final AvailabilityProbe availabilityProbe;
    private final CacheRootResolver cacheRootResolver;
    private final Executor preparationExecutor;
    private final Executor uiExecutor;
    private final Object resultOwnershipLock = new Object();
    private final List<WebCanvasBuildResult> retainedResults = new ArrayList<>();

    private long generation;
    private long activeHostGeneration = -1;
    private long retiringHostGeneration = -1;
    private Phase phase = Phase.NEW;
    private boolean requestedVisible;
    private CompletableFuture<WebCanvasBuildResult> buildFuture;
    private CompletableFuture<Void> producerDrain =
            CompletableFuture.completedFuture(null);
    private WebCanvasBuildResult prepared;
    private WindowsWebCanvasHost.UserDataSessionsRoot preparedSessionsRoot;
    private StartingOwnership startingOwnership;
    private Listener listener = Listener.NOOP;
    private CompletableFuture<Void> retirement;
    private volatile CompletableFuture<Void> retryableRetirement;

    /** Creates the packaged Windows x64 implementation without probing it. */
    public static WindowsWebCanvasRuntime createDefault() throws IOException {
        return new WindowsWebCanvasRuntime(
                WindowsWebCanvasHost.createDefault(),
                sdk -> WebCanvasBuildService.createDefault().buildAsync(sdk),
                () -> availability(new WindowsWebView2RuntimeDetector().detect()),
                () -> Places.getCacheSubdirectory(CACHE_DIRECTORY)
                        .toPath().toAbsolutePath().normalize(),
                command -> Thread.ofVirtual()
                        .name("flutter-web-canvas-prepare")
                        .start(command),
                EventQueue::invokeLater);
    }

    WindowsWebCanvasRuntime(
            WindowsWebCanvasHost host,
            WebCanvasBuildService builds,
            WindowsWebView2RuntimeDetector detector,
            Path cacheRoot,
            Executor uiExecutor) {
        this(
                host,
                builds::buildAsync,
                () -> availability(detector.detect()),
                () -> cacheRoot,
                command -> Thread.ofVirtual()
                        .name("flutter-web-canvas-prepare")
                        .start(command),
                uiExecutor);
    }

    WindowsWebCanvasRuntime(
            WindowsWebCanvasHost host,
            BuildStarter builds,
            AvailabilityProbe availabilityProbe,
            CacheRootResolver cacheRootResolver,
            Executor preparationExecutor,
            Executor uiExecutor) {
        this.host = Objects.requireNonNull(host, "host");
        this.builds = Objects.requireNonNull(builds, "builds");
        this.availabilityProbe = Objects.requireNonNull(
                availabilityProbe, "availabilityProbe");
        this.cacheRootResolver = Objects.requireNonNull(
                cacheRootResolver, "cacheRootResolver");
        this.preparationExecutor = Objects.requireNonNull(
                preparationExecutor, "preparationExecutor");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        host.addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                    && EventQueue.isDispatchThread()) {
                startPreparedIfPossible();
            }
        });
    }

    public JComponent component() {
        requireEdt();
        return host;
    }

    /** Starts one exact SDK-keyed Web runtime generation. */
    public void start(FlutterSdk sdk, Listener listener) {
        requireEdt();
        if (phase != Phase.NEW && phase != Phase.CLOSED) {
            throw new IllegalStateException(
                    "Flutter Web Canvas runtime is already active");
        }
        FlutterSdk acceptedSdk = Objects.requireNonNull(sdk, "sdk");
        Listener acceptedListener = Objects.requireNonNull(listener, "listener");
        this.listener = acceptedListener;
        retirement = null;
        retryableRetirement = null;
        activeHostGeneration = -1;
        retiringHostGeneration = -1;
        phase = Phase.PREPARING;
        long startedGeneration = ++generation;
        CompletableFuture<Void> drain = new CompletableFuture<>();
        producerDrain = drain;
        safeListener(() -> acceptedListener.preparing(
                "Inspecting WebView2 and preparing the isolated browser-compiled "
                + "Flutter Canvas with " + acceptedSdk.flutterExecutable()
                + ". The SDK-keyed cache is reused."));
        try {
            executePreparation(() ->
                    prepare(startedGeneration, acceptedSdk, drain));
        } catch (RuntimeException | LinkageError failure) {
            fail(startedGeneration,
                    "Schedule exact Flutter Web Canvas preparation",
                    failureReason(failure));
            drain.complete(null);
        }
    }

    public void setVisible(boolean visible) {
        requireEdt();
        requestedVisible = visible;
        host.setControllerVisible(visible);
    }

    public void requestControllerFocus() {
        requireEdt();
        host.requestControllerFocus();
    }

    public boolean isControllerFocused() {
        requireEdt();
        return host.isControllerFocused();
    }

    public boolean releaseControllerFocus() {
        requireEdt();
        return host.releaseControllerFocus();
    }

    public boolean running() {
        requireEdt();
        return phase == Phase.RUNNING && host.isBridgeReady();
    }

    /** Installs the exact host-observed physical bounds/DPR sink. */
    public void setSurfaceMetricsListener(
            Consumer<CanvasSurfaceMetrics> listener) {
        requireEdt();
        host.setSurfaceMetricsListener(
                Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Fences the producer, drains every late result, and awaits browser,
     * user-data and publication release before Swing may remove the peer.
     * Failed ownership remains retryable; the returned future completes only
     * after the exact attempt has been finalized on the EDT.
     */
    public CompletionStage<Void> preparePeerRemovalAsync() {
        requireEdt();
        if (phase == Phase.CLOSED && retirement != null) {
            return retirement;
        }
        if (phase == Phase.RETIRING && retirement != null) {
            if (retirement != retryableRetirement || !retirement.isDone()) {
                return retirement;
            }
            // The previous attempt completed every raw ownership barrier, but
            // its final EDT publication was rejected. Re-open only that exact
            // attempt here on the EDT; caller cancellation must not create
            // overlapping cleanup attempts.
            retirement = null;
            retryableRetirement = null;
            producerDrain = CompletableFuture.completedFuture(null);
            phase = Phase.FAILED;
        }

        retiringHostGeneration = activeHostGeneration;
        phase = Phase.RETIRING;
        generation++;
        CompletableFuture<WebCanvasBuildResult> currentBuild = buildFuture;
        buildFuture = null;
        if (currentBuild != null) {
            currentBuild.cancel(true);
        }

        CompletableFuture<Void> published = new CompletableFuture<>();
        retirement = published;
        CompletableFuture<Void> generationDrain = producerDrain;
        CompletableFuture<Void> hostBarrier;
        try {
            hostBarrier = Objects.requireNonNull(
                    host.preparePeerRemovalAsync(), "host peer-removal future");
        } catch (RuntimeException | LinkageError failure) {
            hostBarrier = CompletableFuture.failedFuture(failure);
        }

        CompletableFuture<Void> resultBarrier = generationDrain
                .handle((ignored, failure) -> null)
                .thenCompose(ignored -> captureAndCleanupOwnedResults(published));
        CompletableFuture<Void> rawBarrier = aggregateBarriers(
                List.of(generationDrain, resultBarrier, hostBarrier));
        rawBarrier.whenComplete((ignored, failure) -> {
            Throwable outcome = unwrap(failure);
            if (!dispatchUi(() -> finalizeRetirement(
                    published, outcome))) {
                IllegalStateException dispatchFailure = new IllegalStateException(
                        "Finalize exact Flutter Web Canvas retirement on the EDT");
                if (outcome != null) {
                    dispatchFailure.addSuppressed(outcome);
                }
                retryableRetirement = published;
                published.completeExceptionally(dispatchFailure);
            }
        });
        return published;
    }

    @Override
    public void close() {
        if (EventQueue.isDispatchThread()) {
            preparePeerRemovalAsync();
        } else {
            EventQueue.invokeLater(this::preparePeerRemovalAsync);
        }
    }

    private void prepare(
            long expectedGeneration,
            FlutterSdk sdk,
            CompletableFuture<Void> drain) {
        final Availability availability;
        final WindowsWebCanvasHost.UserDataSessionsRoot sessionsRoot;
        final CompletableFuture<WebCanvasBuildResult> future;
        try {
            availability = Objects.requireNonNull(
                    availabilityProbe.detect(), "Web Canvas availability");
            if (!availability.available()) {
                deliverProducerOnUi(
                        expectedGeneration,
                        drain,
                        () -> unavailable(
                                expectedGeneration, availability.reason()),
                        null);
                return;
            }
            host.prepareNativeDependencies();
            Path cacheRoot = Objects.requireNonNull(
                    cacheRootResolver.resolve(), "Web Canvas cache root")
                    .toAbsolutePath().normalize();
            sessionsRoot = WindowsWebCanvasHost.UserDataSessionsRoot.openOrCreate(
                    cacheRoot.resolve(USER_DATA_DIRECTORY));
            future = Objects.requireNonNull(
                    builds.buildAsync(sdk),
                    "Web Canvas build service returned no future");
        } catch (IOException | RuntimeException | LinkageError failure) {
            deliverProducerOnUi(
                    expectedGeneration,
                    drain,
                    () -> fail(
                            expectedGeneration,
                            "Prepare exact Flutter Web Canvas runtime",
                            failureReason(failure)),
                    null);
            return;
        }

        if (!dispatchUi(() -> registerBuild(expectedGeneration, future))) {
            future.cancel(true);
            future.whenComplete((result, failure) -> retain(result));
            rejectProducerUiDispatch(
                    expectedGeneration,
                    drain,
                    null,
                    "Register exact Flutter Web Canvas build on the EDT");
            return;
        }
        future.whenComplete((result, failure) -> deliverProducerOnUi(
                expectedGeneration,
                drain,
                () -> buildCompleted(
                        expectedGeneration, sessionsRoot, result, failure),
                result));
    }

    private void unavailable(long expectedGeneration, String reason) {
        requireEdt();
        if (expectedGeneration != generation || phase != Phase.PREPARING) {
            return;
        }
        phase = Phase.FAILED;
        safeListener(() -> listener.failed(
                "Inspect exact Flutter Web Canvas runtime", reason));
    }

    private void registerBuild(
            long expectedGeneration,
            CompletableFuture<WebCanvasBuildResult> future) {
        requireEdt();
        if (expectedGeneration != generation || phase != Phase.PREPARING) {
            future.cancel(true);
            return;
        }
        buildFuture = future;
        phase = Phase.BUILDING;
    }

    private void buildCompleted(
            long expectedGeneration,
            WindowsWebCanvasHost.UserDataSessionsRoot sessionsRoot,
            WebCanvasBuildResult result,
            Throwable failure) {
        requireEdt();
        if (expectedGeneration != generation
                || (phase != Phase.BUILDING && phase != Phase.PREPARING)) {
            retain(result);
            return;
        }
        buildFuture = null;
        if (failure != null) {
            retain(result);
            fail(expectedGeneration, "Build exact Flutter Web Canvas",
                    failureReason(failure));
            return;
        }
        if (result == null) {
            fail(expectedGeneration, "Build exact Flutter Web Canvas",
                    "the build completed without a validated publication");
            return;
        }
        prepared = result;
        preparedSessionsRoot = Objects.requireNonNull(
                sessionsRoot, "sessionsRoot");
        phase = Phase.PREPARED;
        startPreparedIfPossible();
    }

    private void startPreparedIfPossible() {
        requireEdt();
        if (phase != Phase.PREPARED || prepared == null
                || !host.isCarrierDisplayable()) {
            return;
        }
        long startedGeneration = generation;
        WebCanvasBuildResult result = prepared;
        WindowsWebCanvasHost.UserDataSessionsRoot sessionsRoot =
                Objects.requireNonNull(
                        preparedSessionsRoot, "preparedSessionsRoot");
        CanvasEngineIdentity expectedIdentity = result.engineIdentity();
        StartingOwnership attempt = new StartingOwnership(
                startedGeneration, result);
        prepared = null;
        preparedSessionsRoot = null;
        startingOwnership = attempt;
        activeHostGeneration = startedGeneration;
        phase = Phase.STARTING;
        try {
            Path userData = sessionsRoot.sessionPath(
                    "session-" + randomHex(16));
            host.start(
                    new WindowsWebCanvasHost.StartRequest(
                            result.artifact(), sessionsRoot, userData,
                            randomHex(32)),
                    hostListener(startedGeneration, expectedIdentity));
            attempt.hostLeaseAcquired = true;
            if (startingOwnership == attempt
                    && startedGeneration == generation
                    && phase != Phase.RETIRING
                    && phase != Phase.CLOSED) {
                startingOwnership = null;
                closeTransferredOwner(result);
                if (phase == Phase.STARTING || phase == Phase.RUNNING) {
                    host.setControllerVisible(requestedVisible);
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            if (startingOwnership == attempt) {
                startingOwnership = null;
                retain(result);
            }
            fail(startedGeneration, "Start exact Flutter Web Canvas",
                    failureReason(failure));
        }
    }

    private void closeTransferredOwner(WebCanvasBuildResult result) {
        try {
            // host.start() acquired the publication lease synchronously. This
            // close only marks close-requested; actual deletion is deferred to
            // the host's off-EDT final-lease release.
            result.close();
        } catch (IOException | RuntimeException | LinkageError failure) {
            retain(result);
            fail(generation,
                    "Transfer exact Flutter Web Canvas publication ownership",
                    failureReason(failure));
        }
    }

    private WindowsWebCanvasHost.Listener hostListener(
            long expectedGeneration,
            CanvasEngineIdentity expectedIdentity) {
        return new WindowsWebCanvasHost.Listener() {
            @Override
            public void nativeStarted(String runtimeVersion, String documentUri) {
                if (expectedGeneration == generation
                        && phase == Phase.STARTING) {
                    safeListener(() -> listener.nativeStarted(runtimeVersion));
                }
            }

            @Override
            public void bridgeReady() {
                if (expectedGeneration != generation
                        || phase != Phase.STARTING) {
                    return;
                }
                try {
                    InputStream stdout = host.runnerStdout();
                    OutputStream stdin = host.runnerStdin();
                    phase = Phase.RUNNING;
                    safeListener(() -> listener.bridgeReady(
                            expectedIdentity, stdout, stdin));
                } catch (RuntimeException | LinkageError failure) {
                    fail(expectedGeneration,
                            "Open exact Flutter Web Canvas protocol",
                            failureReason(failure));
                }
            }

            @Override
            public void diagnostic(String message) {
                if (expectedGeneration == generation
                        && (phase == Phase.STARTING || phase == Phase.RUNNING)) {
                    safeListener(() -> listener.diagnostic(message));
                }
            }

            @Override
            public void bridgeTerminal(WebCanvasHostBridge.Terminal terminal) {
                if (expectedGeneration == generation
                        && (phase == Phase.STARTING || phase == Phase.RUNNING)) {
                    safeListener(() -> listener.bridgeTerminal(
                            terminal.kind().name().toLowerCase(Locale.ROOT)));
                }
            }

            @Override
            public void failed(String operation, String reason) {
                if (expectedGeneration == generation
                        && (phase == Phase.STARTING || phase == Phase.RUNNING)) {
                    fail(expectedGeneration, operation, reason);
                }
            }

            @Override
            public void closed(String teardownFailure) {
                boolean currentClose = phase == Phase.RETIRING
                        && expectedGeneration == retiringHostGeneration;
                boolean currentRuntime = expectedGeneration == generation
                        && (phase == Phase.STARTING
                                || phase == Phase.RUNNING
                                || phase == Phase.FAILED);
                if (currentClose || currentRuntime) {
                    safeListener(() -> listener.closed(teardownFailure));
                }
            }
        };
    }

    private CompletableFuture<Void> captureAndCleanupOwnedResults(
            CompletableFuture<Void> expectedRetirement) {
        CompletableFuture<List<WebCanvasBuildResult>> snapshot =
                new CompletableFuture<>();
        if (!dispatchUi(() -> {
            if (retirement != expectedRetirement || phase != Phase.RETIRING) {
                snapshot.completeExceptionally(new IllegalStateException(
                        "Web Canvas retirement attempt is no longer current"));
                return;
            }
            snapshot.complete(captureOwnedResults());
        })) {
            snapshot.completeExceptionally(new IllegalStateException(
                    "Capture exact Flutter Web Canvas ownership on the EDT"));
        }
        return snapshot.thenCompose(this::cleanupResultsAsync);
    }

    private List<WebCanvasBuildResult> captureOwnedResults() {
        requireEdt();
        Set<WebCanvasBuildResult> unique =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        if (prepared != null) {
            unique.add(prepared);
            prepared = null;
        }
        preparedSessionsRoot = null;
        if (startingOwnership != null) {
            unique.add(startingOwnership.result);
            startingOwnership = null;
        }
        synchronized (resultOwnershipLock) {
            unique.addAll(retainedResults);
            retainedResults.clear();
        }
        return List.copyOf(unique);
    }

    private CompletableFuture<Void> cleanupResultsAsync(
            List<WebCanvasBuildResult> candidates) {
        if (candidates.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> cleanup = new CompletableFuture<>();
        Runnable command = () -> {
            Throwable aggregate = null;
            for (WebCanvasBuildResult candidate : candidates) {
                try {
                    candidate.close();
                } catch (IOException | RuntimeException | LinkageError failure) {
                    retain(candidate);
                    aggregate = combine(aggregate, failure);
                }
            }
            if (aggregate == null) {
                cleanup.complete(null);
            } else {
                cleanup.completeExceptionally(aggregate);
            }
        };
        try {
            executePreparation(command);
        } catch (RuntimeException | LinkageError failure) {
            candidates.forEach(this::retain);
            cleanup.completeExceptionally(failure);
        }
        return cleanup;
    }

    private void finalizeRetirement(
            CompletableFuture<Void> expected,
            Throwable failure) {
        requireEdt();
        if (retirement != expected) {
            expected.completeExceptionally(new IllegalStateException(
                    "Web Canvas retirement attempt was superseded"));
            return;
        }
        producerDrain = CompletableFuture.completedFuture(null);
        retryableRetirement = null;
        if (failure == null) {
            phase = Phase.CLOSED;
            activeHostGeneration = -1;
            retiringHostGeneration = -1;
            listener = Listener.NOOP;
            expected.complete(null);
        } else {
            phase = Phase.FAILED;
            retirement = null;
            expected.completeExceptionally(failure);
        }
    }

    private void fail(long expectedGeneration, String operation, String reason) {
        requireEdt();
        if (expectedGeneration != generation
                || phase == Phase.RETIRING
                || phase == Phase.CLOSED) {
            return;
        }
        phase = Phase.FAILED;
        safeListener(() -> listener.failed(operation, reason));
    }

    private void deliverProducerOnUi(
            long expectedGeneration,
            CompletableFuture<Void> drain,
            Runnable command,
            WebCanvasBuildResult undeliveredResult) {
        if (!dispatchUi(() -> {
            try {
                command.run();
                drain.complete(null);
            } catch (RuntimeException | LinkageError failure) {
                retain(undeliveredResult);
                drain.completeExceptionally(failure);
            }
        })) {
            rejectProducerUiDispatch(
                    expectedGeneration,
                    drain,
                    undeliveredResult,
                    "Deliver exact Flutter Web Canvas producer result on the EDT");
        }
    }

    private void rejectProducerUiDispatch(
            long expectedGeneration,
            CompletableFuture<Void> drain,
            WebCanvasBuildResult undeliveredResult,
            String operation) {
        retain(undeliveredResult);
        IllegalStateException dispatchFailure = new IllegalStateException(
                operation + ": the configured UI dispatcher rejected the task");
        try {
            // EventQueue is the last-resort ownership/failure lane. It is used
            // only after the configured dispatcher rejected a producer result,
            // so the generation can fail visibly instead of remaining stuck in
            // PREPARING/BUILDING. The drain completes after that EDT decision.
            EventQueue.invokeLater(() -> {
                try {
                    fail(expectedGeneration, operation,
                            failureReason(dispatchFailure));
                    drain.complete(null);
                } catch (RuntimeException | LinkageError failure) {
                    drain.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException | LinkageError fallbackFailure) {
            dispatchFailure.addSuppressed(fallbackFailure);
            drain.completeExceptionally(dispatchFailure);
        }
    }

    private void executePreparation(Runnable command) {
        preparationExecutor.execute(() -> {
            if (EventQueue.isDispatchThread()) {
                Thread.ofVirtual()
                        .name("flutter-web-canvas-off-edt-guard")
                        .start(command);
            } else {
                command.run();
            }
        });
    }

    private boolean dispatchUi(Runnable command) {
        try {
            uiExecutor.execute(command);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private void retain(WebCanvasBuildResult result) {
        if (result == null) {
            return;
        }
        synchronized (resultOwnershipLock) {
            for (WebCanvasBuildResult retained : retainedResults) {
                if (retained == result) {
                    return;
                }
            }
            retainedResults.add(result);
        }
    }

    private static CompletableFuture<Void> aggregateBarriers(
            List<? extends CompletionStage<Void>> barriers) {
        List<CompletableFuture<Throwable>> outcomes = barriers.stream()
                .map(barrier -> barrier.toCompletableFuture()
                        .handle((ignored, failure) -> unwrap(failure)))
                .toList();
        return CompletableFuture.allOf(
                outcomes.toArray(CompletableFuture[]::new))
                .thenCompose(ignored -> {
                    Throwable aggregate = null;
                    for (CompletableFuture<Throwable> outcome : outcomes) {
                        aggregate = combine(aggregate, outcome.join());
                    }
                    return aggregate == null
                            ? CompletableFuture.completedFuture(null)
                            : CompletableFuture.failedFuture(aggregate);
                });
    }

    private static Availability availability(
            WindowsWebView2RuntimeDetector.Availability availability) {
        Objects.requireNonNull(availability, "availability");
        return availability.available()
                ? new Availability(true, availability.runtimeVersion(), "")
                : new Availability(false, "", availability.reason());
    }

    private static Throwable combine(Throwable first, Throwable second) {
        Throwable accepted = unwrap(second);
        if (first == null) {
            return accepted;
        }
        if (accepted != null && accepted != first) {
            first.addSuppressed(accepted);
        }
        return first;
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

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private static String failureReason(Throwable failure) {
        Throwable current = unwrap(Objects.requireNonNull(failure, "failure"));
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private static void safeListener(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException ignored) {
            // UI callbacks cannot poison native/browser resource ownership.
        }
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Flutter Web Canvas runtime operations must run on the EDT");
        }
    }

    enum Phase {
        NEW,
        PREPARING,
        BUILDING,
        PREPARED,
        STARTING,
        RUNNING,
        FAILED,
        RETIRING,
        CLOSED
    }

    public record Availability(
            boolean available,
            String runtimeVersion,
            String reason) {
        public Availability {
            runtimeVersion = Objects.requireNonNullElse(runtimeVersion, "");
            reason = Objects.requireNonNullElse(reason, "");
            if (available && runtimeVersion.isBlank()) {
                throw new IllegalArgumentException(
                        "Available Web Canvas runtime requires a version");
            }
            if (!available && reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Unavailable Web Canvas runtime requires a reason");
            }
        }
    }

    @FunctionalInterface
    interface BuildStarter {
        CompletableFuture<WebCanvasBuildResult> buildAsync(FlutterSdk sdk)
                throws IOException;
    }

    @FunctionalInterface
    interface AvailabilityProbe {
        Availability detect() throws IOException;
    }

    @FunctionalInterface
    interface CacheRootResolver {
        Path resolve() throws IOException;
    }

    public interface Listener {
        Listener NOOP = new Listener() {};

        default void preparing(String detail) {}

        default void nativeStarted(String runtimeVersion) {}

        default void bridgeReady(
                CanvasEngineIdentity expectedIdentity,
                InputStream stdout,
                OutputStream stdin) {}

        default void diagnostic(String message) {}

        default void bridgeTerminal(String reason) {}

        default void failed(String operation, String reason) {}

        default void closed(String teardownFailure) {}
    }

    private static final class StartingOwnership {
        private final long generation;
        private final WebCanvasBuildResult result;
        private boolean hostLeaseAcquired;

        private StartingOwnership(
                long generation,
                WebCanvasBuildResult result) {
            this.generation = generation;
            this.result = Objects.requireNonNull(result, "result");
        }
    }
}
