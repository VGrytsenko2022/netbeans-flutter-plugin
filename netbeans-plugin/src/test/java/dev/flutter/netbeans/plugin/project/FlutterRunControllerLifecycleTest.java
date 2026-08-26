package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.api.RunState;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

class FlutterRunControllerLifecycleTest {
    private static final FlutterDevice TARGET =
            new FlutterDevice("windows", "Windows", "windows-x64", false);
    private static final URI VM_SERVICE = URI.create("ws://127.0.0.1:4321/token/ws");

    @TempDir
    Path temporaryDirectory;

    private FakeDependencies dependencies;
    private FlutterRunController controller;

    @BeforeEach
    void setUp() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve("lib/main.dart"), "void main() {}\n");
        Path pubspec = Files.writeString(root.resolve("pubspec.yaml"),
                "name: lifecycle_test\ndependencies:\n  flutter:\n    sdk: flutter\n");
        FileObject directory = FileUtil.toFileObject(root.toFile());
        FlutterProjectInfo info = new FlutterProjectInfo(root, "lifecycle_test", pubspec);
        FlutterProject project = new FlutterProject(directory, new TestProjectState(), info);
        dependencies = new FakeDependencies(root, TARGET);
        controller = new FlutterRunController(project, info, dependencies);
    }

    @AfterEach
    void tearDown() {
        controller.close();
        dependencies.executor.runAll();
    }

    @Test
    void restartNoKeepsCurrentSessionAndFinishesRejectedActionOnce() {
        FakeRunSession current = dependencies.planSession();
        RecordingActionProgress initial = startRun(current);
        assertEquals(List.of(true), initial.finishedValues());

        dependencies.confirmRestart = false;
        RecordingActionProgress restart = new RecordingActionProgress();
        controller.invoke(ActionProvider.COMMAND_RUN, restart, TARGET);

        assertEquals(List.of(false), restart.finishedValues());
        assertEquals(1, dependencies.startedSessions.size());
        assertEquals(0, current.quitCalls);
        assertEquals(1, dependencies.confirmationCalls);
        assertEquals("Restart Flutter Application", dependencies.lastConfirmation.getTitle());
        assertFalse(dependencies.executor.hasTasks());
    }

    @Test
    void restartYesReplacesSessionAndIgnoresStaleExitCompletion() {
        FakeRunSession previous = dependencies.planSession();
        RecordingActionProgress initial = startRun(previous);
        FakeRunSession replacement = dependencies.planSession();
        dependencies.confirmRestart = true;
        RecordingActionProgress restart = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_RUN, restart, TARGET);
        dependencies.executor.runNext();
        replacement.transition(RunState.RUNNING);

        assertEquals(1, previous.quitCalls);
        assertEquals(2, dependencies.startedSessions.size());
        assertEquals(1, dependencies.confirmationCalls);
        assertEquals("Restart Flutter Application", dependencies.lastConfirmation.getTitle());
        assertEquals(List.of(true), initial.finishedValues());
        assertEquals(List.of(true), restart.finishedValues());

        dependencies.executor.runNext();

        assertEquals(List.of(true), initial.finishedValues());
        assertEquals(List.of(true), restart.finishedValues());
        assertEquals(1, dependencies.progress.get(0).finishCalls);
        assertEquals(0, dependencies.progress.get(1).finishCalls);
        assertTrue(controller.isCommandEnabled(FlutterProjectActionProvider.COMMAND_STOP));

        controller.invoke(FlutterProjectActionProvider.COMMAND_STOP);
        dependencies.executor.runAll();

        assertEquals(1, replacement.quitCalls,
                "the stale completion must not replace the active session identity");
        assertEquals(List.of(true), restart.finishedValues());
        assertEquals(1, dependencies.progress.get(1).finishCalls);
    }

    @Test
    void hotReloadAndHotRestartDispatchToTheRunningSession() {
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RELOAD));
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RESTART));

        FakeRunSession session = dependencies.planSession();
        startRun(session);

        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RELOAD));
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RESTART));

        controller.invoke(FlutterProjectActionProvider.COMMAND_HOT_RELOAD);
        controller.invoke(FlutterProjectActionProvider.COMMAND_HOT_RESTART);
        dependencies.executor.runAll();

        assertEquals(1, session.hotReloadCalls);
        assertEquals(1, session.hotRestartCalls);

        session.transition(RunState.STOPPING);
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RELOAD));
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RESTART));
    }

    @Test
    void queuedHotReloadAndRestartDoNotReachAClosedSession() {
        FakeRunSession session = dependencies.planSession();
        startRun(session);

        controller.invoke(FlutterProjectActionProvider.COMMAND_HOT_RELOAD);
        controller.invoke(FlutterProjectActionProvider.COMMAND_HOT_RESTART);
        controller.close();
        controller.open();
        dependencies.executor.runAll();

        assertEquals(0, session.hotReloadCalls);
        assertEquals(0, session.hotRestartCalls);
    }

    @Test
    void debugCompletesActionOnlyAfterAttachAndCloseFinishesResourcesOnce() {
        FakeRunSession session = dependencies.planSession();
        FakeDebugLauncher debugger = dependencies.planDebugger();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_DEBUG, action, TARGET);
        dependencies.executor.runNext();
        session.transition(RunState.RUNNING);
        assertTrue(action.finishedValues().isEmpty());

        session.vmService.complete(VM_SERVICE);
        dependencies.executor.runNext();

        assertEquals(1, debugger.attachCalls);
        assertEquals(List.of(true), dependencies.startedDebugModes);
        assertEquals(List.of(true), action.finishedValues());
        session.transition(RunState.RUNNING);
        assertEquals(List.of(true), action.finishedValues());

        controller.close();
        dependencies.executor.runAll();
        controller.close();

        assertEquals(1, debugger.closeCalls);
        assertEquals(1, session.closeCalls);
        assertEquals(1, dependencies.progress.getFirst().finishCalls);
        assertEquals(List.of(true), action.finishedValues());
    }

    @Test
    void progressCancellationStopsStartupAndFinishesBothProgressKindsOnce() {
        FakeRunSession session = dependencies.planSession();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_RUN, action, TARGET);
        dependencies.executor.runNext();
        RecordingRunProgress progress = dependencies.progress.getFirst();

        assertTrue(progress.cancel());
        assertFalse(progress.cancel());
        assertEquals(1, dependencies.executor.taskCount());
        dependencies.executor.runAll();

        assertEquals(1, session.quitCalls);
        assertEquals(List.of(false), action.finishedValues());
        assertEquals(1, progress.startCalls);
        assertEquals(1, progress.finishCalls);
    }

    @Test
    void closeDuringStartupFailsActionAndFinishesProgressExactlyOnce() {
        FakeRunSession session = dependencies.planSession();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_RUN, action, TARGET);
        dependencies.executor.runNext();
        RecordingRunProgress progress = dependencies.progress.getFirst();

        controller.close();
        controller.close();
        dependencies.executor.runAll();

        assertEquals(List.of(false), action.finishedValues());
        assertEquals(1, progress.startCalls);
        assertEquals(1, progress.finishCalls);
        assertEquals(1, session.closeCalls);
    }

    @Test
    void closeIsNotQuiescentUntilTheRunProcessActuallyExits() throws Exception {
        FakeRunSession session = dependencies.planSession();
        session.delayExitAfterClose();
        startRun(session);

        controller.close();
        dependencies.executor.runAll();

        assertFalse(controller.awaitQuiescence(Duration.ZERO));

        session.completeExit(143);
        dependencies.executor.runAll();

        assertTrue(controller.awaitQuiescence(Duration.ofSeconds(1)));
    }

    @Test
    void closeWhileSessionFactoryIgnoresInterruptDoesNotRetainStartedResources()
            throws Exception {
        FakeRunSession session = dependencies.planSession();
        dependencies.blockSessionStart();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_RUN, action, TARGET);
        Thread startWorker = Thread.ofVirtual().start(dependencies.executor::runNext);
        dependencies.awaitSessionStart();

        controller.close();
        assertFalse(controller.awaitQuiescence(Duration.ZERO),
                "delete preparation must still see the blocked startup worker");
        dependencies.releaseSessionStart();
        startWorker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(startWorker.isAlive(), "blocked session factory did not finish");
        dependencies.executor.runAll();

        assertEquals(List.of(false), action.finishedValues());
        assertEquals(1, dependencies.startedSessions.size());
        assertEquals(1, session.closeCalls);
        assertTrue(dependencies.progress.isEmpty(),
                "a ProgressHandle must not be created after project close");
        assertFalse(controller.isCommandEnabled(ActionProvider.COMMAND_RUN));
        assertFalse(dependencies.executor.hasTasks());
        assertTrue(controller.awaitQuiescence(Duration.ofSeconds(1)),
                "all Run/Debug cleanup work must become observable as quiescent");
    }

    @Test
    void unexpectedDapExitAfterAttachStopsSessionWithoutRefinishingAction() {
        FakeRunSession session = dependencies.planSession();
        FakeDebugLauncher debugger = dependencies.planDebugger();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_DEBUG, action, TARGET);
        dependencies.executor.runNext();
        session.transition(RunState.RUNNING);
        session.vmService.complete(VM_SERVICE);
        dependencies.executor.runNext();
        assertEquals(List.of(true), action.finishedValues());

        debugger.reportExit(5);
        assertEquals(1, session.quitCalls);
        assertEquals(List.of(true), action.finishedValues());
        dependencies.executor.runAll();

        assertEquals(List.of(true), action.finishedValues());
        assertEquals(1, dependencies.progress.getFirst().finishCalls);
    }

    @Test
    void failedOperationFromClosedLifecycleCannotWriteAfterReopen() throws Exception {
        dependencies.blockSessionStart();
        dependencies.failSessionStart(new IOException("old lifecycle session failure"));
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_RUN, action, TARGET);
        Thread startWorker = Thread.ofVirtual().start(dependencies.executor::runNext);
        dependencies.awaitSessionStart();

        controller.close();
        controller.open();
        int tabsBeforeFailure = dependencies.outputTabCount();
        int linesBeforeFailure = dependencies.outputLineCount();
        dependencies.releaseSessionStart();
        startWorker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(startWorker.isAlive(), "blocked session factory did not finish");
        dependencies.executor.runAll();

        assertEquals(List.of(false), action.finishedValues());
        assertEquals(tabsBeforeFailure, dependencies.outputTabCount());
        assertEquals(linesBeforeFailure, dependencies.outputLineCount());
        assertFalse(dependencies.containsOutput("old lifecycle session failure"));
    }

    @Test
    void lateDebuggerDiagnosticCannotWriteAfterCloseAndReopen() {
        FakeRunSession session = dependencies.planSession();
        FakeDebugLauncher debugger = dependencies.planDebugger();
        RecordingActionProgress action = new RecordingActionProgress();

        controller.invoke(ActionProvider.COMMAND_DEBUG, action, TARGET);
        dependencies.executor.runNext();
        session.transition(RunState.RUNNING);
        session.vmService.complete(VM_SERVICE);
        dependencies.executor.runNext();
        assertEquals(List.of(true), action.finishedValues());

        controller.close();
        dependencies.executor.runAll();
        controller.open();
        int tabsAfterReopen = dependencies.outputTabCount();
        int linesAfterReopen = dependencies.outputLineCount();

        debugger.emitDiagnostic("late diagnostic from the closed debugger");

        assertEquals(tabsAfterReopen, dependencies.outputTabCount());
        assertEquals(linesAfterReopen, dependencies.outputLineCount());
        assertFalse(dependencies.containsOutput("late diagnostic from the closed debugger"));
    }

    @Test
    void targetChoiceFromClosedLifecycleCannotReplaceReopenedSelection() throws Exception {
        FlutterDevice baseline = new FlutterDevice(
                "baseline-device", "Baseline", "windows-x64", false);
        controller.selectTarget(baseline);
        dependencies.blockTargetChoice(TARGET);

        controller.invoke(FlutterProjectActionProvider.COMMAND_SELECT_TARGET);
        Thread choiceWorker = Thread.ofVirtual().start(dependencies.executor::runNext);
        dependencies.awaitTargetChoice();

        controller.close();
        controller.open();
        dependencies.releaseTargetChoice();
        choiceWorker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(choiceWorker.isAlive(), "blocked target picker did not finish");
        dependencies.executor.runAll();

        assertEquals(baseline.id(), controller.selectedTargetId());
        assertFalse(dependencies.executor.hasTasks());
    }

    private RecordingActionProgress startRun(FakeRunSession session) {
        RecordingActionProgress action = new RecordingActionProgress();
        controller.invoke(ActionProvider.COMMAND_RUN, action, TARGET);
        dependencies.executor.runNext();
        session.transition(RunState.RUNNING);
        return action;
    }

    private static final class FakeDependencies extends FlutterRunController.Dependencies {
        private final ManualExecutor executor = new ManualExecutor();
        private final FlutterSdk sdk;
        private final FlutterDevice target;
        private final Preferences projectPreferences = new MemoryPreferences();
        private final Deque<FakeRunSession> plannedSessions = new ArrayDeque<>();
        private final Deque<FakeDebugLauncher> plannedDebuggers = new ArrayDeque<>();
        private final List<FakeRunSession> startedSessions = new ArrayList<>();
        private final List<Boolean> startedDebugModes = new ArrayList<>();
        private final List<RecordingRunProgress> progress = new ArrayList<>();
        private final List<RecordingOutput> outputTabs = new ArrayList<>();
        private boolean confirmRestart;
        private int confirmationCalls;
        private org.openide.NotifyDescriptor.Confirmation lastConfirmation;
        private CountDownLatch sessionStartEntered;
        private CountDownLatch releaseSessionStart;
        private IOException sessionStartFailure;
        private CountDownLatch targetChoiceEntered;
        private CountDownLatch releaseTargetChoice;
        private FlutterDevice targetChoice;

        FakeDependencies(Path root, FlutterDevice target) {
            this.sdk = new FlutterSdk(root.resolve("fake-flutter-sdk"),
                    root.resolve("fake-flutter-sdk/bin/flutter"));
            this.target = target;
            projectPreferences.putBoolean("selectedTargetPreferencesMigrated", true);
        }

        FakeRunSession planSession() {
            FakeRunSession session = new FakeRunSession();
            plannedSessions.addLast(session);
            return session;
        }

        FakeDebugLauncher planDebugger() {
            FakeDebugLauncher debugger = new FakeDebugLauncher();
            plannedDebuggers.addLast(debugger);
            return debugger;
        }

        void blockSessionStart() {
            sessionStartEntered = new CountDownLatch(1);
            releaseSessionStart = new CountDownLatch(1);
        }

        void awaitSessionStart() throws InterruptedException {
            assertTrue(sessionStartEntered.await(2, TimeUnit.SECONDS),
                    "session factory was not entered");
        }

        void releaseSessionStart() {
            releaseSessionStart.countDown();
        }

        void failSessionStart(IOException failure) {
            sessionStartFailure = failure;
        }

        void blockTargetChoice(FlutterDevice choice) {
            targetChoice = choice;
            targetChoiceEntered = new CountDownLatch(1);
            releaseTargetChoice = new CountDownLatch(1);
        }

        void awaitTargetChoice() throws InterruptedException {
            assertTrue(targetChoiceEntered.await(2, TimeUnit.SECONDS),
                    "target picker was not entered");
        }

        void releaseTargetChoice() {
            releaseTargetChoice.countDown();
        }

        int outputTabCount() {
            return outputTabs.size();
        }

        int outputLineCount() {
            return outputTabs.stream().mapToInt(output -> output.lines.size()).sum();
        }

        boolean containsOutput(String text) {
            return outputTabs.stream()
                    .flatMap(output -> output.lines.stream())
                    .anyMatch(line -> line.contains(text));
        }

        @Override
        Executor executor() {
            return executor;
        }

        @Override
        FlutterSdk requireFlutterSdk(String operation) {
            return sdk;
        }

        @Override
        List<FlutterDevice> listDevices(FlutterSdk ignoredSdk, Path ignoredRoot) {
            return List.of(target);
        }

        @Override
        FlutterRunController.RunSession startSession(
                FlutterSdk ignoredSdk,
                Path ignoredRoot,
                String deviceId,
                boolean debug) throws IOException {
            if (sessionStartEntered != null) {
                sessionStartEntered.countDown();
                awaitUninterruptibly(releaseSessionStart);
            }
            if (sessionStartFailure != null) {
                throw sessionStartFailure;
            }
            FakeRunSession session = plannedSessions.removeFirst();
            assertEquals(target.id(), deviceId);
            startedSessions.add(session);
            startedDebugModes.add(debug);
            return session;
        }

        @Override
        FlutterRunController.DebugLauncher createDebugLauncher(
                Consumer<String> diagnosticOutput,
                Consumer<Integer> unexpectedExit) {
            FakeDebugLauncher debugger = plannedDebuggers.removeFirst();
            debugger.diagnosticOutput = diagnosticOutput;
            debugger.unexpectedExit = unexpectedExit;
            return debugger;
        }

        @Override
        Optional<FlutterDevice> chooseRunTarget(
                String projectName,
                String title,
                List<FlutterDevice> devices,
                String selectedId) {
            if (targetChoiceEntered != null) {
                targetChoiceEntered.countDown();
                awaitUninterruptibly(releaseTargetChoice);
            }
            return Optional.ofNullable(targetChoice);
        }

        @Override
        boolean confirmRestart(org.openide.NotifyDescriptor.Confirmation descriptor) {
            confirmationCalls++;
            lastConfirmation = descriptor;
            return confirmRestart;
        }

        @Override
        Preferences projectPreferences(FlutterProject project) {
            return projectPreferences;
        }

        @Override
        FlutterRunController.RunProgress createProgress(
                String displayName,
                BooleanSupplier cancel) {
            RecordingRunProgress created = new RecordingRunProgress(cancel);
            progress.add(created);
            return created;
        }

        @Override
        FlutterRunController.OutputTab createOutput(String displayName) {
            RecordingOutput created = new RecordingOutput();
            outputTabs.add(created);
            return created;
        }

        private static void awaitUninterruptibly(CountDownLatch latch) {
            boolean interrupted = false;
            while (true) {
                try {
                    latch.await();
                    break;
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class FakeRunSession implements FlutterRunController.RunSession {
        private final List<Consumer<RunState>> stateListeners = new ArrayList<>();
        private final CompletableFuture<Integer> exit = new CompletableFuture<>();
        private final CompletableFuture<URI> vmService = new CompletableFuture<>();
        private RunState state = RunState.STARTING;
        private int quitCalls;
        private int closeCalls;
        private int hotReloadCalls;
        private int hotRestartCalls;
        private boolean completeExitOnClose = true;

        @Override
        public RunState state() {
            return state;
        }

        @Override
        public void addOutputListener(Consumer<String> listener) {
        }

        @Override
        public void addStateListener(Consumer<RunState> listener) {
            stateListeners.add(listener);
            listener.accept(state);
        }

        @Override
        public CompletableFuture<URI> vmServiceUri() {
            return vmService;
        }

        @Override
        public Optional<URI> currentVmServiceUri() {
            return vmService.isDone() && !vmService.isCompletedExceptionally()
                    ? Optional.ofNullable(vmService.getNow(null))
                    : Optional.empty();
        }

        @Override
        public CompletableFuture<Integer> exitCode() {
            return exit;
        }

        @Override
        public void hotReload() {
            hotReloadCalls++;
        }

        @Override
        public void hotRestart() {
            hotRestartCalls++;
        }

        @Override
        public void quit() {
            quitCalls++;
            transition(RunState.STOPPING);
            transition(RunState.STOPPED);
            exit.complete(0);
        }

        @Override
        public void close() {
            closeCalls++;
            transition(RunState.STOPPED);
            if (completeExitOnClose) {
                exit.complete(143);
            }
        }

        void delayExitAfterClose() {
            completeExitOnClose = false;
        }

        void completeExit(int exitCode) {
            exit.complete(exitCode);
        }

        void transition(RunState next) {
            state = next;
            List.copyOf(stateListeners).forEach(listener -> listener.accept(next));
        }
    }

    private static final class FakeDebugLauncher implements FlutterRunController.DebugLauncher {
        private Consumer<String> diagnosticOutput;
        private Consumer<Integer> unexpectedExit;
        private int attachCalls;
        private int closeCalls;

        @Override
        public void attach(
                FlutterSdk sdk,
                Path projectRoot,
                String projectName,
                String deviceId,
                URI vmServiceUri) throws IOException {
            attachCalls++;
            assertEquals(VM_SERVICE, vmServiceUri);
        }

        @Override
        public void close() {
            closeCalls++;
        }

        void reportExit(int exitCode) {
            unexpectedExit.accept(exitCode);
        }

        void emitDiagnostic(String line) {
            diagnosticOutput.accept(line);
        }
    }

    private static final class RecordingOutput implements FlutterRunController.OutputTab {
        private final List<String> lines = new ArrayList<>();
        private int closeCalls;

        @Override
        public void reset() {
        }

        @Override
        public void select() {
        }

        @Override
        public void println(String line) {
            lines.add(line);
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }

    private static final class RecordingRunProgress implements FlutterRunController.RunProgress {
        private final BooleanSupplier cancel;
        private int startCalls;
        private int finishCalls;

        RecordingRunProgress(BooleanSupplier cancel) {
            this.cancel = cancel;
        }

        @Override
        public void setInitialDelay(int milliseconds) {
        }

        @Override
        public void start() {
            startCalls++;
        }

        @Override
        public void switchToIndeterminate() {
        }

        @Override
        public void progress(String message) {
        }

        @Override
        public void setDisplayName(String name) {
        }

        @Override
        public void finish() {
            finishCalls++;
        }

        boolean cancel() {
            return cancel.getAsBoolean();
        }
    }

    private static final class RecordingActionProgress extends ActionProgress {
        private final List<Boolean> finished = new ArrayList<>();

        @Override
        protected void started() {
        }

        @Override
        public synchronized void finished(boolean success) {
            finished.add(success);
        }

        synchronized List<Boolean> finishedValues() {
            return List.copyOf(finished);
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public synchronized void execute(Runnable command) {
            tasks.addLast(command);
        }

        void runNext() {
            Runnable task;
            synchronized (this) {
                assertFalse(tasks.isEmpty(), "expected a queued controller task");
                task = tasks.removeFirst();
            }
            task.run();
        }

        void runAll() {
            while (hasTasks()) {
                runNext();
            }
        }

        synchronized boolean hasTasks() {
            return !tasks.isEmpty();
        }

        synchronized int taskCount() {
            return tasks.size();
        }
    }

    private static final class TestProjectState implements ProjectState {
        @Override
        public void markModified() {
        }

        @Override
        public void notifyDeleted() {
        }
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();

        MemoryPreferences() {
            super(null, "");
        }

        private MemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() throws BackingStoreException {
            values.clear();
            children.clear();
        }

        @Override
        protected String[] keysSpi() throws BackingStoreException {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() throws BackingStoreException {
            return children.keySet().toArray(String[]::new);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return children.computeIfAbsent(name,
                    childName -> new MemoryPreferences(this, childName));
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
        }
    }
}
