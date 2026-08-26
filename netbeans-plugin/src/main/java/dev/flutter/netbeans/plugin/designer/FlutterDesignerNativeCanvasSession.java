package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerBuildResult;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerBuildService;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeLease;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasHost;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.Timer;

/** Per-MultiView lifecycle for the isolated native Windows FlutterView. */
final class FlutterDesignerNativeCanvasSession implements AutoCloseable {
    private static final Duration ATTACH_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration TERMINATE_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_RUNTIME_DIAGNOSTIC_CHARS = 64 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NativeCanvasHost host;
    private final RuntimeServices runtime;
    private final Consumer<FlutterDesignerNativeCanvasStatus> listener;
    private CanvasRunnerBuildResult prepared;
    private CompletableFuture<CanvasRunnerBuildResult> buildFuture;
    private Process process;
    private CanvasRunnerRuntimeLease processRuntimeLease;
    private BoundedDiagnostics runtimeDiagnostics;
    private Cancellable attachPoll;
    private long attachDeadlineNanos;
    private long generation;
    private long surfaceEpoch;
    private boolean requestedVisible;
    private boolean launchPending;
    private boolean closed;

    static FlutterDesignerNativeCanvasSession createDefault(
            WindowsNativeCanvasHost host,
            Consumer<FlutterDesignerNativeCanvasStatus> listener) throws IOException {
        return new FlutterDesignerNativeCanvasSession(
                host,
                CanvasRunnerBuildService.createDefault(),
                new FlutterToolchainService(),
                listener);
    }

    FlutterDesignerNativeCanvasSession(
            WindowsNativeCanvasHost host,
            CanvasRunnerBuildService builds,
            FlutterToolchainService toolchains,
            Consumer<FlutterDesignerNativeCanvasStatus> listener) {
        this(new WindowsHostAdapter(host), defaultRuntime(builds, toolchains), listener);
    }

    FlutterDesignerNativeCanvasSession(
            NativeCanvasHost host,
            RuntimeServices runtime,
            Consumer<FlutterDesignerNativeCanvasStatus> listener) {
        requireEventDispatchThread();
        this.host = Objects.requireNonNull(host, "host");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.listener = Objects.requireNonNull(listener, "listener");
        host.onPeerReady(this::startIfPossible);
        host.onPeerLost(this::peerLost);
        host.onAttachmentFailed(this::attachmentFailed);
    }

    void show() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        requestedVisible = true;
        try {
            if (process != null && host.isAttached()) {
                host.setRunnerVisible(true);
                return;
            }
        } catch (RuntimeException | LinkageError failure) {
            failCurrentProcess("Show native Flutter Canvas", failureReason(failure));
            return;
        }
        startIfPossible();
    }

    void hide() {
        requireEventDispatchThread();
        requestedVisible = false;
        try {
            host.setRunnerVisible(false);
        } catch (RuntimeException | LinkageError failure) {
            failCurrentProcess("Hide native Flutter Canvas", failureReason(failure));
        }
    }

    @Override
    public void close() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        closed = true;
        requestedVisible = false;
        launchPending = false;
        generation++;
        stopAttachTimer();
        String hostCloseFailure = null;
        try {
            host.close();
        } catch (RuntimeException | LinkageError failure) {
            hostCloseFailure = failureReason(failure);
        }
        CanvasRunnerBuildResult currentPrepared = prepared;
        prepared = null;
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        runtimeDiagnostics = null;
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        if (currentPrepared != null) {
            currentPrepared.close();
        }
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                "Native Flutter Canvas stopped.",
                hostCloseFailure == null
                        ? "The isolated Flutter runner for this Design view was closed."
                        : "The isolated Flutter runner was stopped, but native host cleanup "
                        + "reported: " + hostCloseFailure + "."));
    }

    private void startIfPossible() {
        requireEventDispatchThread();
        if (closed || !requestedVisible || process != null || launchPending) {
            return;
        }
        try {
            if (!host.isNativePeerReady()) {
                return;
            }
        } catch (RuntimeException | LinkageError failure) {
            publishFailure("Inspect native Flutter Canvas host", failureReason(failure));
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
            publishFailure("Resolve Flutter SDK for native Canvas", failureReason(failure));
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
                "Building or verifying the isolated Windows Flutter runner with "
                + sdk.flutterExecutable() + ". The cached build is reused."));
        try {
            buildFuture = Objects.requireNonNull(
                    runtime.buildStarter().buildAsync(sdk),
                    "Native Canvas build starter returned no future");
        } catch (RuntimeException failure) {
            publishFailure("Schedule native Flutter Canvas build", failureReason(failure));
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
            publishFailure(
                    "Build native Flutter Canvas runner",
                    failureReason(failure));
            return;
        }
        if (result == null) {
            publishFailure(
                    "Build native Flutter Canvas runner",
                    "the build completed without a runtime result");
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
        final long parentWindow;
        final long epoch;
        final long launchGeneration;
        final List<String> command;
        final CanvasRunnerRuntimeLease launchRuntimeLease;
        try {
            if (!host.isNativePeerReady()) {
                return;
            }
            parentWindow = host.parentWindowHandle();
            long hostProcessId = ProcessHandle.current().pid();
            epoch = ++surfaceEpoch;
            launchGeneration = ++generation;
            command = launchCommand(
                    runner.executable(), parentWindow, hostProcessId, epoch, newNonce());
            launchRuntimeLease = runner.retainRuntimeLease();
        } catch (RuntimeException | LinkageError exception) {
            publishFailure(
                    "Prepare native Flutter Canvas launch",
                    failureReason(exception));
            return;
        }
        launchPending = true;
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                "Starting native Flutter Canvas...",
                "Launching the isolated FlutterView for Canvas surface " + epoch
                + ". The child window remains hidden until its HWND and PID are verified."));
        try {
            runtime.launchExecutor().execute(() -> {
                Process launched = null;
                try {
                    launched = runtime.processStarter().start(
                            command, runner.executable().getParent());
                    Process started = launched;
                    BoundedDiagnostics diagnostics = new BoundedDiagnostics();
                    drainOutput(started, diagnostics);
                    try {
                        runtime.uiExecutor().execute(() -> processStarted(
                                launchGeneration, epoch, parentWindow, started, diagnostics,
                                launchRuntimeLease));
                    } catch (RuntimeException | LinkageError exception) {
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
                        // The launch never acquired a process. Its lease is
                        // already released; no UI callback can be delivered.
                    }
                }
            });
        } catch (RuntimeException | LinkageError exception) {
            closeLease(launchRuntimeLease);
            launchPending = false;
            generation++;
            publishFailure(
                    "Schedule native Flutter Canvas runner",
                    failureReason(exception));
        }
    }

    private void processStarted(
            long launchGeneration,
            long epoch,
            long expectedParent,
            Process launched,
            BoundedDiagnostics diagnostics,
            CanvasRunnerRuntimeLease launchRuntimeLease) {
        requireEventDispatchThread();
        if (closed || launchGeneration != generation) {
            terminateAndRelease(launched, launchRuntimeLease);
            return;
        }
        launchPending = false;
        final boolean peerReady;
        final long currentParent;
        try {
            peerReady = host.isNativePeerReady();
            currentParent = peerReady ? host.parentWindowHandle() : 0L;
        } catch (RuntimeException | LinkageError exception) {
            generation++;
            terminateAndRelease(launched, launchRuntimeLease);
            publishFailure(
                    "Verify native Flutter Canvas host",
                    failureReason(exception));
            return;
        }
        if (!peerReady) {
            terminateAndRelease(launched, launchRuntimeLease);
            return;
        }
        if (currentParent != expectedParent) {
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
        attachDeadlineNanos = System.nanoTime() + ATTACH_TIMEOUT.toNanos();
        try {
            attachPoll = runtime.pollScheduler().schedule(
                    () -> pollAttachment(launchGeneration, epoch));
            launched.onExit().thenRun(() -> {
                try {
                    runtime.uiExecutor().execute(
                            () -> processExited(launchGeneration, launched));
                } catch (RuntimeException | LinkageError exception) {
                    // Physical exit is already known. Release the per-process
                    // claim even when the UI callback cannot be delivered.
                    closeLease(launchRuntimeLease);
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
            boolean attached = host.tryAttach(current.pid());
            if (closed || launchGeneration != generation || current != process) {
                return;
            }
            if (attached) {
                stopAttachTimer();
                host.setRunnerVisible(requestedVisible);
                if (closed || launchGeneration != generation
                        || current != process || !host.isAttached()) {
                    return;
                }
                publish(new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                        "Native Flutter Canvas is running.",
                        "Verified embedded FlutterView surface " + epoch
                        + " in isolated process " + current.pid()
                        + ". Rendering is native; no PNG or pixel-frame transport is used."));
                return;
            }
        } catch (RuntimeException | LinkageError exception) {
            failCurrentProcess("Verify native Flutter Canvas HWND", failureReason(exception));
            return;
        }
        if (System.nanoTime() >= attachDeadlineNanos) {
            failCurrentProcess(
                    "Attach native Flutter Canvas HWND",
                    "The runner did not create one verified direct WS_CHILD window within "
                    + ATTACH_TIMEOUT.toSeconds() + " seconds.");
        }
    }

    private void processExited(long launchGeneration, Process exited) {
        requireEventDispatchThread();
        if (exited != process || launchGeneration != generation) {
            return;
        }
        stopAttachTimer();
        process = null;
        CanvasRunnerRuntimeLease exitedRuntimeLease = processRuntimeLease;
        processRuntimeLease = null;
        launchPending = false;
        closeLease(exitedRuntimeLease);
        host.close();
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        int exitCode = exited.exitValue();
        publishFailure(
                "Run native Flutter Canvas",
                "The isolated runner exited with code " + exitCode
                + diagnosticSuffix(output));
    }

    private void launchFailed(long launchGeneration, Throwable exception) {
        requireEventDispatchThread();
        if (!closed && launchGeneration == generation) {
            launchPending = false;
            publishFailure("Start native Flutter Canvas runner", failureReason(exception));
        }
    }

    private void peerLost() {
        requireEventDispatchThread();
        generation++;
        launchPending = false;
        stopAttachTimer();
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        runtimeDiagnostics = null;
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        // A real AWT peer recreation publishes onPeerReady after addNotify.
        // Do not relaunch synchronously from the loss callback: that would turn
        // an incorrectly classified native-child failure into a process storm.
    }

    private void attachmentFailed(String reason) {
        requireEventDispatchThread();
        generation++;
        launchPending = false;
        stopAttachTimer();
        Process current = process;
        CanvasRunnerRuntimeLease currentRuntimeLease = processRuntimeLease;
        process = null;
        processRuntimeLease = null;
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        if (!closed) {
            publishFailure(
                    "Maintain native Flutter Canvas attachment",
                    Objects.requireNonNullElse(reason, "Native child-window attachment failed")
                            + diagnosticSuffix(output));
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
        String hostCloseFailure = null;
        try {
            host.close();
        } catch (RuntimeException | LinkageError failure) {
            hostCloseFailure = failureReason(failure);
        }
        if (current != null) {
            terminateAndRelease(current, currentRuntimeLease);
        } else {
            closeLease(currentRuntimeLease);
        }
        String output = runtimeDiagnostics == null ? "" : runtimeDiagnostics.snapshot();
        runtimeDiagnostics = null;
        String closeSuffix = hostCloseFailure == null
                ? ""
                : " Native host cleanup also failed: " + hostCloseFailure + ".";
        publishFailure(operation, reason + diagnosticSuffix(output) + closeSuffix);
    }

    private void publishFailure(String operation, String reason) {
        publish(new FlutterDesignerNativeCanvasStatus(
                FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                operation + " failed.",
                "Target: embedded Windows FlutterView. Reason: " + reason));
    }

    private void publish(FlutterDesignerNativeCanvasStatus status) {
        requireEventDispatchThread();
        listener.accept(status);
    }

    private void stopAttachTimer() {
        if (attachPoll != null) {
            attachPoll.cancel();
            attachPoll = null;
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
        boolean observesExit = false;
        try {
            current.onExit().whenComplete((ignored, failure) -> {
                if (failure == null) {
                    closeLease(runtimeLease);
                } else {
                    observePhysicalExitAfterOnExitFailure(current, runtimeLease);
                }
            });
            observesExit = true;
        } catch (RuntimeException | LinkageError ignored) {
            // If the process API cannot observe exit, retain the generation
            // unless the process is already known to be physically stopped.
        }
        try {
            runtime.terminator().accept(current);
        } catch (RuntimeException | LinkageError ignored) {
            // The onExit callback still owns the lease if the process later
            // exits naturally. Do not release while it may still be alive.
        }
        if (!observesExit) {
            try {
                if (!current.isAlive()) {
                    closeLease(runtimeLease);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Conservatively retain the runtime generation.
            }
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
            CanvasRunnerRuntimeLease runtimeLease) {
        if (closeLeaseIfProcessExited(current, runtimeLease)) {
            return;
        }
        try {
            Thread.ofVirtual().name("flutter-native-canvas-exit-fallback").start(() -> {
                boolean interrupted = false;
                try {
                    while (true) {
                        try {
                            current.waitFor();
                            closeLeaseIfProcessExited(current, runtimeLease);
                            return;
                        } catch (InterruptedException exception) {
                            interrupted = true;
                        } catch (RuntimeException | LinkageError ignored) {
                            closeLeaseIfProcessExited(current, runtimeLease);
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
            closeLeaseIfProcessExited(current, runtimeLease);
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
            runtimeLease.close();
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
                        .redirectErrorStream(true)
                        .start(),
                command -> Thread.ofVirtual()
                        .name("flutter-native-canvas-launch")
                        .start(command),
                EventQueue::invokeLater,
                FlutterDesignerNativeCanvasSession::scheduleAttachPoll,
                FlutterDesignerNativeCanvasSession::terminateAsync);
    }

    private static Cancellable scheduleAttachPoll(Runnable poll) {
        requireEventDispatchThread();
        Timer timer = new Timer(50, event -> poll.run());
        timer.setRepeats(true);
        timer.start();
        return timer::stop;
    }

    private static void drainOutput(Process process, BoundedDiagnostics diagnostics) {
        Thread.ofVirtual().name("flutter-native-canvas-output").start(() -> {
            try {
                copyBoundedDiagnostics(process.getInputStream(), diagnostics);
            } catch (IOException exception) {
                diagnostics.append("Cannot read runner output: " + failureReason(exception));
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

    private static void terminateAsync(Process process) {
        Thread.ofVirtual().name("flutter-native-canvas-stop").start(() -> {
            process.destroy();
            try {
                if (!process.waitFor(TERMINATE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        });
    }

    static List<String> launchCommand(
            Path executable,
            long parentWindow,
            long hostProcessId,
            long surfaceEpoch,
            String nonce) {
        Objects.requireNonNull(executable, "executable");
        if (!executable.isAbsolute() || parentWindow == 0
                || hostProcessId <= 0 || hostProcessId > 0xffff_ffffL
                || surfaceEpoch <= 0 || nonce == null
                || !nonce.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid native Canvas launch identity");
        }
        List<String> command = new ArrayList<>(5);
        command.add(executable.normalize().toString());
        command.add("--netbeans-parent-hwnd=0x"
                + String.format(java.util.Locale.ROOT, "%016X", parentWindow));
        command.add("--netbeans-host-pid=" + hostProcessId);
        command.add("--netbeans-surface-epoch=" + surfaceEpoch);
        command.add("--netbeans-session-nonce=" + nonce);
        return List.copyOf(command);
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

    interface NativeCanvasHost {
        long parentWindowHandle();

        boolean tryAttach(long runnerProcessId);

        boolean isAttached();

        boolean isNativePeerReady();

        void setRunnerVisible(boolean visible);

        void onPeerReady(Runnable listener);

        void onPeerLost(Runnable listener);

        void onAttachmentFailed(Consumer<String> listener);

        void close();
    }

    record SdkResolution(FlutterSdk sdk, String message) {
        SdkResolution {
            message = Objects.requireNonNullElse(message, "Flutter SDK is unavailable.");
            if (message.isBlank()) {
                throw new IllegalArgumentException("SDK resolution message cannot be blank");
            }
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

    record RuntimeServices(
            SdkResolver sdkResolver,
            BuildStarter buildStarter,
            ProcessStarter processStarter,
            Executor launchExecutor,
            Executor uiExecutor,
            PollScheduler pollScheduler,
            Consumer<Process> terminator) {
        RuntimeServices {
            Objects.requireNonNull(sdkResolver, "sdkResolver");
            Objects.requireNonNull(buildStarter, "buildStarter");
            Objects.requireNonNull(processStarter, "processStarter");
            Objects.requireNonNull(launchExecutor, "launchExecutor");
            Objects.requireNonNull(uiExecutor, "uiExecutor");
            Objects.requireNonNull(pollScheduler, "pollScheduler");
            Objects.requireNonNull(terminator, "terminator");
        }
    }

    private static final class WindowsHostAdapter implements NativeCanvasHost {
        private final WindowsNativeCanvasHost host;

        private WindowsHostAdapter(WindowsNativeCanvasHost host) {
            this.host = Objects.requireNonNull(host, "host");
        }

        @Override
        public long parentWindowHandle() {
            return host.parentWindowHandle();
        }

        @Override
        public boolean tryAttach(long runnerProcessId) {
            return host.tryAttach(runnerProcessId).isPresent();
        }

        @Override
        public boolean isAttached() {
            return host.attachment().isPresent();
        }

        @Override
        public boolean isNativePeerReady() {
            return host.isNativePeerReady();
        }

        @Override
        public void setRunnerVisible(boolean visible) {
            host.setRunnerVisible(visible);
        }

        @Override
        public void onPeerReady(Runnable listener) {
            host.onPeerReady(listener);
        }

        @Override
        public void onPeerLost(Runnable listener) {
            host.onPeerLost(listener);
        }

        @Override
        public void onAttachmentFailed(Consumer<String> listener) {
            host.onAttachmentFailed(listener);
        }

        @Override
        public void close() {
            host.close();
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
