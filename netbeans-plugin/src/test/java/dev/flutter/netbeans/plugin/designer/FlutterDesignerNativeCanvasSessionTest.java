package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerBuildResult;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerCacheIdentity;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerLeaseTestSupport;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterDesignerNativeCanvasSessionTest {
    private static final String NONCE = "01".repeat(32);

    @Test
    void launchCommandUsesOpaqueHexHwndAndExactProcessIdentity() {
        Path executable = Path.of("C:\\Canvas Cache\\runner.exe").toAbsolutePath();

        List<String> command = FlutterDesignerNativeCanvasSession.launchCommand(
                executable, 0x12ab34L, 4321L, 7L, NONCE);

        assertEquals(List.of(
                executable.normalize().toString(),
                "--netbeans-parent-hwnd=0x000000000012AB34",
                "--netbeans-host-pid=4321",
                "--netbeans-surface-epoch=7",
                "--netbeans-session-nonce=" + NONCE), command);
    }

    @Test
    void launchCommandRejectsInvalidOrNumericJsonUnsafeIdentities() {
        Path executable = Path.of("runner.exe").toAbsolutePath();

        assertThrows(IllegalArgumentException.class, () ->
                FlutterDesignerNativeCanvasSession.launchCommand(
                        executable, 0, 1, 1, NONCE));
        assertThrows(IllegalArgumentException.class, () ->
                FlutterDesignerNativeCanvasSession.launchCommand(
                        executable, 1, 0x1_0000_0000L, 1, NONCE));
        assertThrows(IllegalArgumentException.class, () ->
                FlutterDesignerNativeCanvasSession.launchCommand(
                        executable, 1, 1, 0, NONCE));
        assertThrows(IllegalArgumentException.class, () ->
                FlutterDesignerNativeCanvasSession.launchCommand(
                        executable, 1, 1, 1, "not-a-256-bit-nonce"));
    }

    @Test
    void deadParentHandleDuringLaunchPreparationFailsWithoutStartingAProcess()
            throws Exception {
        Harness harness = Harness.create("dead-parent-prepare", 0x107L);

        onEdt(() -> {
            harness.host.failParentWindowHandleOnCall(1);
            harness.session.show();
            harness.build.complete(harness.runner);
        });

        assertEquals(0, harness.launches.pendingCount());
        assertTrue(harness.processes.commands.isEmpty());
        assertTrue(harness.terminated.isEmpty());
        assertFalse(harness.polls.hasActivePoll());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains("native peer disappeared"));
    }

    @Test
    void deadParentHandleAfterProcessStartTerminatesBeforeOwnership()
            throws Exception {
        Harness harness = Harness.create("dead-parent-started", 0x108L);
        FakeProcess process = new FakeProcess(1108L);

        onEdt(() -> {
            harness.host.failParentWindowHandleOnCall(2);
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertTrue(harness.host.attachPids.isEmpty());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains("native peer disappeared"));
    }

    @Test
    void newlineFreeRunnerOutputIsBoundedBeforeAWholeLineCanBeAllocated()
            throws Exception {
        byte[] output = "x".repeat(512 * 1024).getBytes(StandardCharsets.UTF_8);

        String diagnostics = FlutterDesignerNativeCanvasSession.readBoundedDiagnostics(
                new ByteArrayInputStream(output));

        assertTrue(diagnostics.length() <= 64 * 1024);
        assertTrue(diagnostics.endsWith("[output truncated]"));
    }

    @Test
    void closeDuringBuildFencesTheLateBuildCompletion() throws Exception {
        Harness harness = Harness.create("close-build", 0x101L);

        onEdt(harness.session::show);
        onEdt(harness.session::close);
        onEdt(() -> harness.build.complete(harness.runner));

        assertEquals(List.of(
                FlutterDesignerNativeCanvasStatus.Stage.PREPARING,
                FlutterDesignerNativeCanvasStatus.Stage.STOPPED), harness.stages());
        assertEquals(0, harness.launches.pendingCount());
        assertTrue(harness.processes.commands.isEmpty());
        assertTrue(harness.terminated.isEmpty());
        assertEquals(1, harness.host.closeCalls);
        assertTrue(harness.runner.runtimeLease().isClosed());
    }

    @Test
    void closeKeepsTheRuntimeGenerationLeasedUntilTheProcessPhysicallyExits(
            @TempDir Path temporaryDirectory) throws Exception {
        CanvasRunnerBuildResult leasedRunner =
                CanvasRunnerLeaseTestSupport.createResult(temporaryDirectory);
        Harness harness = Harness.create("leased-process", 0x10bL, leasedRunner);
        FakeProcess process = new FakeProcess(1111L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.session.close();
        });

        assertTrue(leasedRunner.runtimeLease().isClosed());
        assertTrue(process.isAlive());
        assertTrue(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertEquals(List.of(process), harness.terminated);

        onEdt(() -> process.exit(0));

        assertFalse(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
    }

    @Test
    void exceptionalOnExitObservationCannotReleaseTheLeaseOfALiveProcess(
            @TempDir Path temporaryDirectory) throws Exception {
        CanvasRunnerBuildResult leasedRunner =
                CanvasRunnerLeaseTestSupport.createResult(temporaryDirectory);
        Harness harness = Harness.create("failed-exit-observation", 0x10cL, leasedRunner);
        FakeProcess process = new FakeProcess(1112L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            process.failNextOnExitObservation();
            harness.session.close();
        });

        assertTrue(process.isAlive());
        assertTrue(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertEquals(List.of(process), harness.terminated);

        onEdt(() -> process.exit(0));
        awaitCondition(
                () -> !CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory),
                "runtime generation lease was not released after physical process exit");
    }

    @Test
    void peerLossWhileLaunchIsInFlightTerminatesTheLateProcess() throws Exception {
        Harness harness = Harness.create("peer-loss-launch", 0x102L);
        FakeProcess process = new FakeProcess(1102L);

        onEdt(() -> {
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            assertEquals(1, harness.launches.pendingCount());
            harness.host.firePeerLost();
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertTrue(harness.host.attachPids.isEmpty());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
    }

    @Test
    void peerLossDuringAttachCancelsPollingAndTerminatesOnlyThatGeneration() throws Exception {
        Harness harness = Harness.create("peer-loss-attach", 0x103L);
        FakeProcess process = new FakeProcess(1103L);

        onEdt(() -> {
            harness.startAttaching(process);
            assertTrue(harness.polls.hasActivePoll());
            harness.host.firePeerLost();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
    }

    @Test
    void processExitDuringAttachFailsOnceAndClosesTheHostAttachment() throws Exception {
        Harness harness = Harness.create("exit-attach", 0x104L);
        FakeProcess process = new FakeProcess(1104L);

        onEdt(() -> {
            harness.startAttaching(process);
            process.exit(23);
        });

        assertFalse(harness.polls.hasActivePoll());
        assertEquals(1, harness.host.closeCalls);
        assertTrue(harness.terminated.isEmpty());
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.FAILED)
                .count());
        FlutterDesignerNativeCanvasStatus failure = harness.statuses.getLast();
        assertTrue(failure.detail().contains("exited with code 23"));
    }

    @Test
    void staleOnExitCannotTearDownTheNewGenerationAfterPeerRestart() throws Exception {
        Harness harness = Harness.create("stale-exit", 0x105L);
        FakeProcess first = new FakeProcess(1105L);
        FakeProcess second = new FakeProcess(2105L);

        onEdt(() -> {
            harness.startAttaching(first);
            harness.host.firePeerLost();
            harness.processes.enqueue(second);
            harness.host.attachAvailable = true;
            harness.host.firePeerReady();
            harness.launches.runNext();

            first.exit(91);
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(first), harness.terminated);
        assertEquals(List.of(second.pid()), harness.host.attachPids);
        assertEquals(0, harness.host.closeCalls);
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.RUNNING)
                .count());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "isolated process " + second.pid()));
        assertTrue(harness.host.attached);
    }

    @Test
    void persistentAttachmentFailureFailsOnceWithoutAutomaticRelaunch()
            throws Exception {
        Harness harness = Harness.create("persistent-attachment-failure", 0x109L);
        FakeProcess process = new FakeProcess(1109L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.host.failAttachmentOnNextVisibility = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(process), harness.terminated);
        assertEquals(0, harness.launches.pendingCount());
        assertFalse(harness.polls.hasActivePoll());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.FAILED)
                .count());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated persistent attachment failure"));
    }

    @Test
    void nativeLinkageFailureDuringAttachmentTerminatesAndReportsConcreteCause()
            throws Exception {
        Harness harness = Harness.create("attachment-linkage", 0x10aL);
        FakeProcess process = new FakeProcess(1110L);

        onEdt(() -> {
            harness.host.attachFailure = new UnsatisfiedLinkError(
                    "simulated EnumChildWindows linkage failure");
            harness.startAttaching(process);
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated EnumChildWindows linkage failure"));
    }

    @Test
    void hideAndShowToggleTheAttachedWindowWithoutRelaunching() throws Exception {
        Harness harness = Harness.create("visibility", 0x106L);
        FakeProcess process = new FakeProcess(1106L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.session.hide();
            harness.session.show();
        });

        assertEquals(List.of(true, false, true), harness.host.visibility);
        assertEquals(1, harness.processes.commands.size());
        assertEquals(0, harness.launches.pendingCount());
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.RUNNING)
                .count());
    }

    @Test
    void twoSimultaneousSessionsKeepProcessesHostsAndTerminationIndependent()
            throws Exception {
        Harness first = Harness.create("independent-a", 0x201L);
        Harness second = Harness.create("independent-b", 0x202L);
        FakeProcess firstProcess = new FakeProcess(1201L);
        FakeProcess secondProcess = new FakeProcess(1202L);

        onEdt(() -> {
            first.host.attachAvailable = true;
            second.host.attachAvailable = true;
            first.processes.enqueue(firstProcess);
            second.processes.enqueue(secondProcess);
            first.session.show();
            second.session.show();
            first.build.complete(first.runner);
            second.build.complete(second.runner);
            first.session.show();
            second.session.show();
            assertEquals(1, first.launches.pendingCount());
            assertEquals(1, second.launches.pendingCount());
            first.launches.runNext();
            second.launches.runNext();
            first.polls.runActivePoll();
            second.polls.runActivePoll();
        });

        assertEquals(1, first.processes.commands.size());
        assertEquals(1, second.processes.commands.size());
        assertTrue(first.processes.commands.getFirst().contains(
                "--netbeans-parent-hwnd=0x0000000000000201"));
        assertTrue(second.processes.commands.getFirst().contains(
                "--netbeans-parent-hwnd=0x0000000000000202"));
        assertEquals(List.of(firstProcess.pid()), first.host.attachPids);
        assertEquals(List.of(secondProcess.pid()), second.host.attachPids);
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                first.statuses.getLast().stage());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                second.statuses.getLast().stage());

        onEdt(first.session::close);

        assertEquals(List.of(firstProcess), first.terminated);
        assertTrue(second.terminated.isEmpty());
        assertTrue(second.host.attached);
        onEdt(() -> {
            second.session.hide();
            second.session.show();
        });
        assertEquals(List.of(true, false, true), second.host.visibility);
        onEdt(second.session::close);
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        onEdt(() -> {
            runnable.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return callable.call();
        }
        Holder<T> result = new Holder<>();
        Holder<Throwable> failure = new Holder<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    result.value = callable.call();
                } catch (Throwable throwable) {
                    failure.value = throwable;
                }
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return result.value;
    }

    private static void awaitCondition(BooleanSupplier condition, String message)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class Harness {
        private final FakeHost host;
        private final CompletableFuture<CanvasRunnerBuildResult> build =
                new CompletableFuture<>();
        private final FakeProcessStarter processes = new FakeProcessStarter();
        private final ManualExecutor launches = new ManualExecutor();
        private final ManualPollScheduler polls = new ManualPollScheduler();
        private final List<Process> terminated = new ArrayList<>();
        private final List<FlutterDesignerNativeCanvasStatus> statuses = new ArrayList<>();
        private final CanvasRunnerBuildResult runner;
        private final FlutterDesignerNativeCanvasSession session;

        private Harness(String name, long parentWindow) {
            this(name, parentWindow, null);
        }

        private Harness(
                String name,
                long parentWindow,
                CanvasRunnerBuildResult suppliedRunner) {
            host = new FakeHost(parentWindow);
            Path root = Path.of("target", "native-canvas-session-tests", name)
                    .toAbsolutePath().normalize();
            Path sdkHome = root.resolve("flutter-sdk");
            FlutterSdk sdk = new FlutterSdk(
                    sdkHome, sdkHome.resolve("bin").resolve("flutter.bat"));
            CanvasRunnerCacheIdentity identity = new CanvasRunnerCacheIdentity(
                    "a".repeat(64), "test-engine", "b".repeat(64));
            Path runnerRoot = root.resolve("runner-cache");
            runner = suppliedRunner == null
                    ? new CanvasRunnerBuildResult(
                            runnerRoot.resolve("runner.exe"),
                            runnerRoot,
                            identity,
                            false,
                            "")
                    : suppliedRunner;
            FlutterDesignerNativeCanvasSession.RuntimeServices runtime =
                    new FlutterDesignerNativeCanvasSession.RuntimeServices(
                            () -> new FlutterDesignerNativeCanvasSession.SdkResolution(
                                    sdk, "test Flutter SDK"),
                            ignored -> build,
                            processes,
                            launches,
                            Runnable::run,
                            polls,
                            terminated::add);
            session = new FlutterDesignerNativeCanvasSession(host, runtime, statuses::add);
        }

        static Harness create(String name, long parentWindow) throws Exception {
            return onEdt(() -> new Harness(name, parentWindow));
        }

        static Harness create(
                String name,
                long parentWindow,
                CanvasRunnerBuildResult runner) throws Exception {
            return onEdt(() -> new Harness(name, parentWindow, runner));
        }

        void startAttaching(FakeProcess process) {
            processes.enqueue(process);
            session.show();
            build.complete(runner);
            launches.runNext();
        }

        boolean hasStage(FlutterDesignerNativeCanvasStatus.Stage stage) {
            return statuses.stream().anyMatch(status -> status.stage() == stage);
        }

        List<FlutterDesignerNativeCanvasStatus.Stage> stages() {
            return statuses.stream().map(FlutterDesignerNativeCanvasStatus::stage).toList();
        }
    }

    private static final class FakeHost
            implements FlutterDesignerNativeCanvasSession.NativeCanvasHost {
        private final long parentWindow;
        private final List<Long> attachPids = new ArrayList<>();
        private final List<Boolean> visibility = new ArrayList<>();
        private boolean ready = true;
        private boolean attachAvailable;
        private LinkageError attachFailure;
        private boolean attached;
        private boolean failAttachmentOnNextVisibility;
        private int parentWindowHandleCalls;
        private int failingParentWindowHandleCall = -1;
        private int closeCalls;
        private Runnable peerReady = () -> { };
        private Runnable peerLost = () -> { };
        private Consumer<String> attachmentFailed = ignored -> { };

        private FakeHost(long parentWindow) {
            this.parentWindow = parentWindow;
        }

        @Override
        public long parentWindowHandle() {
            parentWindowHandleCalls++;
            if (parentWindowHandleCalls == failingParentWindowHandleCall) {
                throw new IllegalStateException("native peer disappeared");
            }
            if (!ready) {
                throw new IllegalStateException("peer is not ready");
            }
            return parentWindow;
        }

        @Override
        public boolean tryAttach(long runnerProcessId) {
            if (attachFailure != null) {
                throw attachFailure;
            }
            if (!attachAvailable) {
                return false;
            }
            attachPids.add(runnerProcessId);
            attached = true;
            return true;
        }

        @Override
        public boolean isAttached() {
            return attached;
        }

        @Override
        public boolean isNativePeerReady() {
            return ready;
        }

        @Override
        public void setRunnerVisible(boolean visible) {
            if (attached && failAttachmentOnNextVisibility) {
                attached = false;
                attachmentFailed.accept("simulated persistent attachment failure");
                return;
            }
            if (attached) {
                visibility.add(visible);
            }
        }

        @Override
        public void onPeerReady(Runnable listener) {
            peerReady = listener;
        }

        @Override
        public void onPeerLost(Runnable listener) {
            peerLost = listener;
        }

        @Override
        public void onAttachmentFailed(Consumer<String> listener) {
            attachmentFailed = listener;
        }

        @Override
        public void close() {
            closeCalls++;
            attached = false;
        }

        void firePeerLost() {
            ready = false;
            attached = false;
            peerLost.run();
        }

        void firePeerReady() {
            ready = true;
            peerReady.run();
        }

        void failParentWindowHandleOnCall(int call) {
            failingParentWindowHandleCall = call;
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.addLast(command);
        }

        int pendingCount() {
            return pending.size();
        }

        void runNext() {
            Runnable command = pending.pollFirst();
            if (command == null) {
                throw new AssertionError("No pending launch");
            }
            command.run();
        }
    }

    private static final class ManualPollScheduler
            implements FlutterDesignerNativeCanvasSession.PollScheduler {
        private final List<ScheduledPoll> polls = new ArrayList<>();

        @Override
        public FlutterDesignerNativeCanvasSession.Cancellable schedule(Runnable poll) {
            ScheduledPoll scheduled = new ScheduledPoll(poll);
            polls.add(scheduled);
            return () -> scheduled.active = false;
        }

        boolean hasActivePoll() {
            return polls.stream().anyMatch(poll -> poll.active);
        }

        void runActivePoll() {
            ScheduledPoll scheduled = polls.reversed().stream()
                    .filter(poll -> poll.active)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No active attachment poll"));
            scheduled.action.run();
        }

        private static final class ScheduledPoll {
            private final Runnable action;
            private boolean active = true;

            private ScheduledPoll(Runnable action) {
                this.action = action;
            }
        }
    }

    private static final class FakeProcessStarter
            implements FlutterDesignerNativeCanvasSession.ProcessStarter {
        private final Deque<FakeProcess> queued = new ArrayDeque<>();
        private final List<List<String>> commands = new ArrayList<>();

        void enqueue(FakeProcess process) {
            queued.addLast(process);
        }

        @Override
        public Process start(List<String> command, Path workingDirectory) {
            FakeProcess process = queued.pollFirst();
            if (process == null) {
                throw new AssertionError("No fake process queued for " + command);
            }
            commands.add(List.copyOf(command));
            return process;
        }
    }

    private static final class FakeProcess extends Process {
        private final long pid;
        private final CompletableFuture<Process> onExit = new CompletableFuture<>();
        private final OutputStream output = new ByteArrayOutputStream();
        private final InputStream input = new ByteArrayInputStream(new byte[0]);
        private boolean alive = true;
        private int exitCode;
        private RuntimeException nextOnExitFailure;

        private FakeProcess(long pid) {
            this.pid = pid;
        }

        void exit(int code) {
            if (!alive) {
                return;
            }
            exitCode = code;
            alive = false;
            onExit.complete(this);
        }

        void failNextOnExitObservation() {
            nextOnExitFailure = new IllegalStateException(
                    "simulated exceptional Process.onExit completion");
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            try {
                return onExit.thenApply(ignored -> exitCode).get();
            } catch (ExecutionException exception) {
                throw new AssertionError(exception.getCause());
            }
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            try {
                onExit.get(timeout, unit);
                return true;
            } catch (ExecutionException exception) {
                throw new AssertionError(exception.getCause());
            } catch (TimeoutException exception) {
                return false;
            }
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("process is still alive");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            exit(143);
        }

        @Override
        public Process destroyForcibly() {
            exit(137);
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            if (nextOnExitFailure != null) {
                RuntimeException failure = nextOnExitFailure;
                nextOnExitFailure = null;
                return CompletableFuture.failedFuture(failure);
            }
            return onExit;
        }
    }
}
