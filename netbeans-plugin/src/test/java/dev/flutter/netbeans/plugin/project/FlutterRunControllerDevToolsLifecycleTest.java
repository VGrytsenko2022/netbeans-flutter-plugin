package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.api.RunState;
import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

class FlutterRunControllerDevToolsLifecycleTest {
    private static final FlutterDevice TARGET =
            new FlutterDevice("windows", "Windows", "windows-x64", false);
    private static final URI VM_SERVICE = URI.create("ws://127.0.0.1:4321/token/ws");
    private static final URI BROWSER = URI.create("http://127.0.0.1:9100/?uri=service");
    private static final URI REPLACEMENT_BROWSER =
            URI.create("http://127.0.0.1:9200/?uri=replacement");

    @TempDir
    Path temporaryDirectory;

    private FakeDependencies dependencies;
    private FlutterRunController controller;
    private FakeRunSession runSession;

    @BeforeEach
    void setUp() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("project"));
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve("windows"));
        Files.writeString(root.resolve("lib/main.dart"), "void main() {}\n");
        Path pubspec = Files.writeString(root.resolve("pubspec.yaml"),
                "name: devtools_lifecycle_test\ndependencies:\n  flutter:\n    sdk: flutter\n");
        FileObject directory = FileUtil.toFileObject(root.toFile());
        FlutterProjectInfo info = new FlutterProjectInfo(root, "devtools_lifecycle_test", pubspec);
        FlutterProject project = new FlutterProject(directory, new TestProjectState(), info);
        dependencies = new FakeDependencies(root, TARGET);
        controller = new FlutterRunController(project, info, dependencies);
        runSession = startRunningSession();
    }

    @AfterEach
    void tearDown() throws Exception {
        controller.close();
        dependencies.executor.runAll();
        flushEdt();
    }

    @Test
    void readyServerOpensBrowserAndCloseFinishesResourcesOnce() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();

        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();

        assertEquals(1, dependencies.devToolsStartCalls);
        assertEquals(1, server.outputListenerCalls);
        assertEquals(1, progress.startCalls);
        assertEquals(0, progress.finishCalls);

        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        assertEquals(List.of(BROWSER), dependencies.openedBrowsers);
        assertEquals(0, progress.finishCalls);

        controller.close();
        dependencies.executor.runAll();
        controller.close();
        assertFalse(controller.awaitQuiescence(Duration.ZERO),
                "DevTools close request may return before its process exits");
        server.completeExit(0);
        assertTrue(controller.awaitQuiescence(Duration.ofSeconds(1)));

        assertEquals(1, server.closeCalls);
        assertEquals(1, runSession.closeCalls);
        assertEquals(1, progress.finishCalls);
        assertEquals(List.of(BROWSER), dependencies.openedBrowsers);
    }

    @Test
    void cancelBeforeQueuedStartPreventsLaunchAndFinishesProgressOnce() {
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();

        assertTrue(progress.cancel());
        assertFalse(progress.cancel());
        dependencies.executor.runAll();

        assertEquals(0, dependencies.devToolsStartCalls);
        assertEquals(1, progress.startCalls);
        assertEquals(1, progress.finishCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
    }

    @Test
    void cancelAfterLaunchStopsServerAndExitFinishesProgressOnce() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();
        RecordingProgress progress = dependencies.latestDevToolsProgress();

        assertTrue(progress.cancel());
        assertFalse(progress.cancel());
        assertEquals(1, server.stopCalls);
        assertEquals(0, progress.finishCalls);

        server.completeExit(0);

        assertEquals(1, progress.finishCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));

        server.completeBrowser(BROWSER);
        dependencies.executor.runAll();
        flushEdt();

        assertEquals(1, server.closeCalls);
        assertEquals(1, server.stopCalls);
        assertEquals(1, progress.finishCalls);
        assertTrue(dependencies.openedBrowsers.isEmpty());
    }

    @Test
    void closeBeforeQueuedStartPreventsLaunchAndFinishesProgressOnce() {
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();

        controller.close();
        dependencies.executor.runAll();
        controller.close();

        assertEquals(0, dependencies.devToolsStartCalls);
        assertEquals(1, progress.startCalls);
        assertEquals(1, progress.finishCalls);
        assertEquals(1, runSession.closeCalls);
    }

    @Test
    void staleExitAfterReopenDoesNotClearReplacementServer() throws Exception {
        FakeDevToolsServer stale = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();
        RecordingProgress staleProgress = dependencies.latestDevToolsProgress();

        controller.close();
        dependencies.executor.runAll();
        assertEquals(1, stale.closeCalls);
        assertEquals(1, staleProgress.finishCalls);

        controller.open();
        runSession = startRunningSession();
        FakeDevToolsServer replacement = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();
        RecordingProgress replacementProgress = dependencies.latestDevToolsProgress();

        stale.completeExit(0);
        stale.completeBrowser(BROWSER);
        dependencies.executor.runAll();
        flushEdt();

        assertEquals(0, replacementProgress.finishCalls);
        assertEquals(0, replacement.closeCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));
        assertTrue(dependencies.openedBrowsers.isEmpty());

        replacement.completeBrowser(REPLACEMENT_BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        assertEquals(List.of(REPLACEMENT_BROWSER), dependencies.openedBrowsers);
    }

    @Test
    void repeatedOpenReusesReadyServerAndBrowserUri() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();
        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        flushEdt();

        assertEquals(1, dependencies.devToolsStartCalls);
        assertEquals(1, server.outputListenerCalls);
        assertSame(progress, dependencies.latestDevToolsProgress());
        assertEquals(0, server.stopCalls);
        assertEquals(0, progress.finishCalls);
        assertEquals(List.of(BROWSER, BROWSER), dependencies.openedBrowsers);
    }

    @Test
    void explicitStopStopsReadyServerAndReleasesCommandsOnExit() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();
        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        controller.invoke(FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS);

        assertEquals(1, server.stopCalls);
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));

        server.completeExit(0);

        assertEquals(1, progress.finishCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));
        assertEquals(List.of(BROWSER), dependencies.openedBrowsers);
    }

    @Test
    void flutterSessionTerminationStopsOwnedDevTools() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();
        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        runSession.completeExit(0);

        assertEquals(1, server.stopCalls);
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));

        server.completeExit(0);
        dependencies.executor.runAll();

        assertEquals(1, server.stopCalls);
        assertEquals(1, progress.finishCalls);
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));
    }

    @Test
    void flutterSessionReplacementStopsOldDevToolsAndAllowsNewOwner() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();
        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        flushEdt();

        FakeRunSession replacement = dependencies.planRunSession();
        runSession.completeOnQuit(0);
        controller.invoke(ActionProvider.COMMAND_RUN, null, TARGET);
        dependencies.executor.runNext();

        assertEquals(1, dependencies.restartConfirmations);
        assertEquals(1, server.stopCalls);
        assertEquals(RunState.STARTING, replacement.state());

        replacement.completeVmService(VM_SERVICE);
        replacement.transition(RunState.RUNNING);
        server.completeExit(0);
        dependencies.executor.runAll();

        int linesBeforeStaleOutput = dependencies.outputLineCount();
        runSession.emitOutput("stale Flutter output from replaced session");

        assertEquals(linesBeforeStaleOutput, dependencies.outputLineCount());
        runSession = replacement;

        assertEquals(1, progress.finishCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));
    }

    @Test
    void devToolsProcessFailureReleasesControllerAndRejectsLateReady() throws Exception {
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        RecordingProgress progress = dependencies.latestDevToolsProgress();
        dependencies.executor.runNext();

        server.completeExit(17);
        flushEdt();

        assertEquals(1, progress.finishCalls);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
        assertFalse(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS));
        assertTrue(dependencies.openedBrowsers.isEmpty());

        server.completeBrowser(BROWSER);
        dependencies.executor.runAll();
        flushEdt();

        assertEquals(1, server.closeCalls);
        assertEquals(1, progress.finishCalls);
        assertTrue(dependencies.openedBrowsers.isEmpty());

        FakeDevToolsServer replacement = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();
        int linesBeforeStaleOutput = dependencies.outputLineCount();

        server.emitOutput("stale DevTools output from failed process");

        assertEquals(linesBeforeStaleOutput, dependencies.outputLineCount());
        assertEquals(0, replacement.closeCalls);
    }

    @Test
    void lateOutputAfterCloseAndReopenCannotRecreateOrWriteTabs() {
        FakeRunSession staleRun = runSession;
        FakeDevToolsServer staleDevTools = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();

        controller.close();
        dependencies.executor.runAll();
        int tabsAfterClose = dependencies.outputTabCount();
        int linesAfterClose = dependencies.outputLineCount();

        staleRun.emitOutput("late Flutter output after close");
        staleDevTools.emitOutput("late DevTools output after close");

        assertEquals(tabsAfterClose, dependencies.outputTabCount());
        assertEquals(linesAfterClose, dependencies.outputLineCount());
        assertTrue(dependencies.outputTabs.stream()
                .allMatch(output -> output.closeCalls == 1));

        controller.open();
        runSession = startRunningSession();
        int tabsAfterReopen = dependencies.outputTabCount();
        int linesAfterReopen = dependencies.outputLineCount();

        staleRun.emitOutput("stale Flutter output after reopen");
        staleDevTools.emitOutput("stale DevTools output after reopen");

        assertEquals(tabsAfterReopen, dependencies.outputTabCount());
        assertEquals(linesAfterReopen, dependencies.outputLineCount());
    }

    @Test
    void queuedControlFromClosedLifecycleCannotAffectReopenedController() {
        FakeRunSession staleRun = runSession;
        controller.invoke(FlutterProjectActionProvider.COMMAND_STOP);
        controller.close();
        controller.open();
        int tabsBeforeStaleControl = dependencies.outputTabCount();
        int linesBeforeStaleControl = dependencies.outputLineCount();

        dependencies.executor.runNext();

        assertEquals(0, staleRun.quitCalls);
        assertEquals(tabsBeforeStaleControl, dependencies.outputTabCount());
        assertEquals(linesBeforeStaleControl, dependencies.outputLineCount());
        dependencies.executor.runAll();
    }

    @Test
    void queuedBrowserOpenIsDiscardedWhenDevToolsStopsBeforeDispatch() {
        dependencies.deferBrowserCallbacks = true;
        FakeDevToolsServer server = dependencies.planDevToolsServer();
        controller.invoke(FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS);
        dependencies.executor.runNext();

        server.completeBrowser(BROWSER);
        dependencies.executor.runNext();
        assertEquals(1, dependencies.pendingBrowserCallbackCount());
        assertTrue(dependencies.openedBrowsers.isEmpty());

        controller.invoke(FlutterProjectActionProvider.COMMAND_STOP_DEVTOOLS);
        dependencies.runBrowserCallbacks();

        assertEquals(1, server.stopCalls);
        assertTrue(dependencies.openedBrowsers.isEmpty());
    }

    private FakeRunSession startRunningSession() {
        FakeRunSession created = dependencies.planRunSession();
        controller.invoke(ActionProvider.COMMAND_RUN, null, TARGET);
        dependencies.executor.runNext();
        created.completeVmService(VM_SERVICE);
        created.transition(RunState.RUNNING);
        assertTrue(controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_OPEN_DEVTOOLS));
        return created;
    }

    private static void flushEdt() throws Exception {
        EventQueue.invokeAndWait(() -> {
        });
    }

    private static final class FakeDependencies extends FlutterRunController.Dependencies {
        private final ManualExecutor executor = new ManualExecutor();
        private final Path projectRoot;
        private final FlutterDevice target;
        private final FlutterSdk flutterSdk;
        private final DartSdk dartSdk;
        private final Deque<FakeRunSession> plannedRunSessions = new ArrayDeque<>();
        private final Deque<FakeDevToolsServer> plannedDevToolsServers = new ArrayDeque<>();
        private final List<RecordingProgress> progress = new ArrayList<>();
        private final List<URI> openedBrowsers = new ArrayList<>();
        private final List<RecordingOutput> outputTabs = new ArrayList<>();
        private final Deque<Runnable> pendingBrowserCallbacks = new ArrayDeque<>();
        private boolean deferBrowserCallbacks;
        private int devToolsStartCalls;
        private int restartConfirmations;

        FakeDependencies(Path projectRoot, FlutterDevice target) {
            this.projectRoot = projectRoot;
            this.target = target;
            this.flutterSdk = new FlutterSdk(projectRoot.resolve("fake-flutter-sdk"),
                    projectRoot.resolve("fake-flutter-sdk/bin/flutter"));
            this.dartSdk = new DartSdk(projectRoot.resolve("fake-dart-sdk"),
                    projectRoot.resolve("fake-dart-sdk/bin/dart"));
        }

        FakeRunSession planRunSession() {
            FakeRunSession session = new FakeRunSession();
            plannedRunSessions.addLast(session);
            return session;
        }

        FakeDevToolsServer planDevToolsServer() {
            FakeDevToolsServer server = new FakeDevToolsServer();
            plannedDevToolsServers.addLast(server);
            return server;
        }

        RecordingProgress latestDevToolsProgress() {
            return progress.stream()
                    .filter(item -> item.displayName.contains("DevTools"))
                    .reduce((_first, second) -> second)
                    .orElseThrow();
        }

        int outputTabCount() {
            return outputTabs.size();
        }

        int outputLineCount() {
            return outputTabs.stream().mapToInt(output -> output.lines.size()).sum();
        }

        int pendingBrowserCallbackCount() {
            return pendingBrowserCallbacks.size();
        }

        void runBrowserCallbacks() {
            while (!pendingBrowserCallbacks.isEmpty()) {
                pendingBrowserCallbacks.removeFirst().run();
            }
        }

        @Override
        Executor executor() {
            return executor;
        }

        @Override
        FlutterSdk requireFlutterSdk(String operation) {
            return flutterSdk;
        }

        @Override
        DartSdk requireDartSdk(String operation) {
            return dartSdk;
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
                boolean debug) {
            assertEquals(target.id(), deviceId);
            assertFalse(debug);
            return plannedRunSessions.removeFirst();
        }

        @Override
        FlutterRunController.DevToolsServer startDevTools(
                DartSdk sdk,
                Path root,
                URI vmServiceUri) {
            devToolsStartCalls++;
            assertSame(dartSdk, sdk);
            assertEquals(projectRoot, root);
            assertEquals(VM_SERVICE, vmServiceUri);
            return plannedDevToolsServers.removeFirst();
        }

        @Override
        void showBrowser(URI browserUri) {
            openedBrowsers.add(browserUri);
        }

        @Override
        void invokeBrowserLater(Runnable action) {
            if (deferBrowserCallbacks) {
                pendingBrowserCallbacks.addLast(action);
            } else {
                EventQueue.invokeLater(action);
            }
        }

        @Override
        boolean confirmRestart(org.openide.NotifyDescriptor.Confirmation descriptor) {
            restartConfirmations++;
            return true;
        }

        @Override
        FlutterRunController.RunProgress createProgress(
                String displayName,
                BooleanSupplier cancel) {
            RecordingProgress created = new RecordingProgress(displayName, cancel);
            progress.add(created);
            return created;
        }

        @Override
        FlutterRunController.OutputTab createOutput(String displayName) {
            RecordingOutput created = new RecordingOutput(displayName);
            outputTabs.add(created);
            return created;
        }
    }

    private static final class FakeRunSession implements FlutterRunController.RunSession {
        private final List<Consumer<String>> outputListeners = new ArrayList<>();
        private final List<Consumer<RunState>> stateListeners = new ArrayList<>();
        private final CompletableFuture<URI> vmService = new CompletableFuture<>();
        private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();
        private RunState state = RunState.STARTING;
        private Integer exitCodeOnQuit;
        private int quitCalls;
        private int closeCalls;

        @Override
        public RunState state() {
            return state;
        }

        @Override
        public void addOutputListener(Consumer<String> listener) {
            outputListeners.add(listener);
        }

        @Override
        public void addStateListener(Consumer<RunState> listener) {
            stateListeners.add(listener);
            listener.accept(state);
        }

        @Override
        public CompletableFuture<URI> vmServiceUri() {
            return vmService.copy();
        }

        @Override
        public Optional<URI> currentVmServiceUri() {
            return vmService.isDone() && !vmService.isCompletedExceptionally()
                    ? Optional.ofNullable(vmService.getNow(null))
                    : Optional.empty();
        }

        @Override
        public CompletableFuture<Integer> exitCode() {
            return exitCode.copy();
        }

        @Override
        public void hotReload() {
        }

        @Override
        public void hotRestart() {
        }

        @Override
        public void quit() {
            quitCalls++;
            transition(RunState.STOPPING);
            if (exitCodeOnQuit != null) {
                completeExit(exitCodeOnQuit);
            }
        }

        @Override
        public void close() {
            closeCalls++;
            transition(RunState.STOPPED);
            exitCode.complete(143);
        }

        void completeVmService(URI uri) {
            vmService.complete(uri);
        }

        void emitOutput(String line) {
            List.copyOf(outputListeners).forEach(listener -> listener.accept(line));
        }

        void completeOnQuit(int code) {
            exitCodeOnQuit = code;
        }

        void completeExit(int code) {
            transition(RunState.STOPPED);
            exitCode.complete(code);
        }

        void transition(RunState next) {
            state = next;
            List.copyOf(stateListeners).forEach(listener -> listener.accept(next));
        }
    }

    private static final class FakeDevToolsServer
            implements FlutterRunController.DevToolsServer {
        private final CompletableFuture<URI> browserUri = new CompletableFuture<>();
        private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();
        private final AtomicBoolean stopRequested = new AtomicBoolean();
        private final List<Consumer<String>> outputListeners = new ArrayList<>();
        private boolean alive = true;
        private int outputListenerCalls;
        private int stopCalls;
        private int closeCalls;

        @Override
        public void addOutputListener(Consumer<String> listener) {
            outputListenerCalls++;
            outputListeners.add(listener);
        }

        @Override
        public CompletableFuture<URI> browserUri() {
            return browserUri.copy();
        }

        @Override
        public CompletableFuture<Integer> exitCode() {
            return exitCode.copy();
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public boolean stopRequested() {
            return stopRequested.get();
        }

        @Override
        public void stop() {
            if (stopRequested.compareAndSet(false, true)) {
                stopCalls++;
            }
        }

        @Override
        public void close() {
            closeCalls++;
            stop();
        }

        void completeBrowser(URI uri) {
            browserUri.complete(uri);
        }

        void emitOutput(String line) {
            List.copyOf(outputListeners).forEach(listener -> listener.accept(line));
        }

        void completeExit(int code) {
            alive = false;
            exitCode.complete(code);
        }
    }

    private static final class RecordingProgress implements FlutterRunController.RunProgress {
        private final String displayName;
        private final BooleanSupplier cancel;
        private int startCalls;
        private int finishCalls;

        RecordingProgress(String displayName, BooleanSupplier cancel) {
            this.displayName = displayName;
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

    private static final class RecordingOutput implements FlutterRunController.OutputTab {
        private final String displayName;
        private final List<String> lines = new ArrayList<>();
        private int resetCalls;
        private int selectCalls;
        private int closeCalls;

        RecordingOutput(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public void reset() {
            resetCalls++;
        }

        @Override
        public void select() {
            selectCalls++;
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

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.addLast(command);
        }

        void runNext() {
            assertFalse(tasks.isEmpty(), "expected a queued controller task");
            tasks.removeFirst().run();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.removeFirst().run();
            }
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
}
