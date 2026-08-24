package dev.flutter.netbeans.plugin.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.project.FlutterProjectActionProvider;
import dev.flutter.netbeans.run.FlutterToolCommand;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterToolingControllerTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    Path temporaryDirectory;

    private Path projectRoot;
    private Path flutterExecutable;
    private FakeBackend backend;
    private FakeTestSessionFactory testSessions;
    private FlutterToolingController controller;

    @BeforeEach
    void setUp() throws Exception {
        projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path pubspec = Files.writeString(projectRoot.resolve("pubspec.yaml"), "name: project\n");
        Path flutterHome = Files.createDirectories(temporaryDirectory.resolve("flutter-sdk"));
        flutterExecutable = Files.writeString(
                Files.createDirectories(flutterHome.resolve("bin")).resolve("flutter-test"),
                "");
        FlutterSdk sdk = new FlutterSdk(flutterHome, flutterExecutable);

        backend = new FakeBackend();
        testSessions = new FakeTestSessionFactory();
        controller = new FlutterToolingController(
                new FakeProject(fileObject(projectRoot)),
                new FlutterProjectInfo(projectRoot, "project", pubspec),
                backend,
                testSessions,
                ignoredOperation -> sdk);
    }

    @AfterEach
    void tearDown() {
        controller.close();
    }

    @Test
    void buildsExactPubGetAndAnalyzeRequestsAndMapsExitCodesToProgress() throws Exception {
        RecordingProgress pubProgress = new RecordingProgress();
        ControlledFuture pubFuture = backend.plan(true);

        controller.invoke(
                FlutterProjectActionProvider.COMMAND_PUB_GET,
                Lookup.EMPTY,
                pubProgress);
        FlutterExecutionRequest pubRequest = backend.awaitRequest();

        assertEquals("Flutter Pub Get: project", pubRequest.displayName());
        assertEquals(flutterExecutable.toAbsolutePath().normalize(), pubRequest.executable());
        assertEquals(projectRoot.toAbsolutePath().normalize(), pubRequest.workingDirectory());
        assertEquals(List.of("pub", "get"), pubRequest.arguments());
        assertTrue(pubRequest.echoStandardOutput());
        assertNull(pubRequest.outputConvertor());

        pubFuture.complete(0);
        assertEquals(List.of(true), pubProgress.awaitFinished());

        RecordingProgress analyzeProgress = new RecordingProgress();
        ControlledFuture analyzeFuture = backend.plan(true);
        controller.invoke(
                FlutterProjectActionProvider.COMMAND_ANALYZE,
                Lookup.EMPTY,
                analyzeProgress);
        FlutterExecutionRequest analyzeRequest = backend.awaitRequest();

        assertEquals(
                List.of("analyze", "--no-pub", "--no-congratulate"),
                analyzeRequest.arguments());
        assertTrue(analyzeRequest.echoStandardOutput());
        assertTrue(analyzeRequest.outputConvertor() instanceof FlutterAnalyzeLineConvertor);

        analyzeFuture.complete(2);
        assertEquals(List.of(false), analyzeProgress.awaitFinished());
    }

    @Test
    void buildsExactAllFileAndNamedTestArguments() throws Exception {
        assertTestRequest(
                ActionProvider.COMMAND_TEST,
                Lookup.EMPTY,
                List.of("test", "--no-pub", "--reporter=json"));

        Path file = Files.createDirectories(projectRoot.resolve("test/widgets"))
                .resolve("counter_test.dart");
        Files.writeString(file, "void main() {}\n");
        FileObject fileObject = fileObject(file);
        assertTestRequest(
                FlutterProjectActionProvider.COMMAND_TEST_FILE,
                Lookups.singleton(fileObject),
                List.of(
                        "test",
                        "--no-pub",
                        "--reporter=json",
                        "test/widgets/counter_test.dart"));

        assertTestRequest(
                FlutterProjectActionProvider.COMMAND_TEST_AT_CARET,
                Lookups.singleton(new SingleMethod(fileObject, "increments counter")),
                List.of(
                        "test",
                        "--no-pub",
                        "--reporter=json",
                        "test/widgets/counter_test.dart",
                        "--plain-name",
                        "increments counter"));
    }

    @Test
    void allowsOnlyOneActiveCommandAndReenablesCommandsAfterCompletion() throws Exception {
        ControlledFuture future = backend.plan(true);
        RecordingProgress progress = new RecordingProgress();

        controller.invoke(
                FlutterProjectActionProvider.COMMAND_ANALYZE,
                Lookup.EMPTY,
                progress);
        backend.awaitRequest();

        assertFalse(controller.isCommandEnabled(FlutterProjectActionProvider.COMMAND_PUB_GET));
        assertEquals(1, backend.startCount());

        future.complete(0);
        assertEquals(List.of(true), progress.awaitFinished());
        awaitCondition(() -> controller.isCommandEnabled(
                FlutterProjectActionProvider.COMMAND_PUB_GET));
        assertEquals(1, backend.startCount());
    }

    @Test
    void closeCancelsTheProcessAndFinishesActionAndTestSessionExactlyOnce() throws Exception {
        ControlledFuture future = backend.plan(true);
        RecordingProgress progress = new RecordingProgress();

        controller.invoke(ActionProvider.COMMAND_TEST, Lookup.EMPTY, progress);
        backend.awaitRequest();
        FakeTestSessionBridge session = testSessions.awaitSession();

        controller.close();

        assertTrue(future.awaitCancelRequested());
        assertEquals(List.of(false), progress.awaitFinished());
        assertEquals(List.of(new FinishCall(-1, true, null)), session.awaitFinishCalls());
        future.awaitGetReturned();
        assertEquals(1, progress.finishedValues().size());
        assertEquals(1, session.finishCalls().size());
        assertFalse(controller.isCommandEnabled(ActionProvider.COMMAND_TEST));
    }

    @Test
    void staleCompletionCannotFinishANewActionOrDoubleFinishTheOldAction() throws Exception {
        ControlledFuture staleFuture = backend.plan(false);
        RecordingProgress staleProgress = new RecordingProgress();
        controller.invoke(
                FlutterProjectActionProvider.COMMAND_ANALYZE,
                Lookup.EMPTY,
                staleProgress);
        backend.awaitRequest();

        controller.close();
        assertTrue(staleFuture.awaitCancelRequested());
        assertEquals(List.of(false), staleProgress.awaitFinished());

        controller.open();
        ControlledFuture currentFuture = backend.plan(true);
        RecordingProgress currentProgress = new RecordingProgress();
        controller.invoke(
                FlutterProjectActionProvider.COMMAND_PUB_GET,
                Lookup.EMPTY,
                currentProgress);
        backend.awaitRequest();

        staleFuture.complete(0);
        staleFuture.awaitGetReturned();
        assertEquals(1, staleProgress.finishedValues().size());
        assertTrue(currentProgress.finishedValues().isEmpty());
        assertFalse(controller.isCommandEnabled(ActionProvider.COMMAND_TEST));

        currentFuture.complete(0);
        assertEquals(List.of(true), currentProgress.awaitFinished());
        awaitCondition(() -> controller.isCommandEnabled(ActionProvider.COMMAND_TEST));
    }

    @Test
    void nonzeroTestExitIsReportedToTestSessionAndFinishesProgressAsFailed() throws Exception {
        ControlledFuture future = backend.plan(true);
        RecordingProgress progress = new RecordingProgress();

        controller.invoke(ActionProvider.COMMAND_TEST, Lookup.EMPTY, progress);
        backend.awaitRequest();
        FakeTestSessionBridge session = testSessions.awaitSession();
        future.complete(3);

        assertEquals(List.of(false), progress.awaitFinished());
        List<FinishCall> finishes = session.awaitFinishCalls();
        assertEquals(1, finishes.size());
        assertEquals(3, finishes.get(0).exitCode());
        assertFalse(finishes.get(0).cancelled());
        assertNull(finishes.get(0).failure());
    }

    private void assertTestRequest(
            String command,
            Lookup context,
            List<String> expectedArguments) throws Exception {
        ControlledFuture future = backend.plan(true);
        RecordingProgress progress = new RecordingProgress();
        controller.invoke(command, context, progress);

        FlutterExecutionRequest request = backend.awaitRequest();
        FakeTestSessionBridge session = testSessions.awaitSession();
        assertEquals(expectedArguments, request.arguments());
        assertFalse(request.echoStandardOutput());
        assertNull(request.outputConvertor());

        request.standardOutput().accept("stdout");
        request.standardError().accept("stderr");
        assertEquals(List.of("stdout"), session.standardOutput());
        assertEquals(List.of("stderr"), session.standardError());

        future.complete(0);
        assertEquals(List.of(true), progress.awaitFinished());
        assertEquals(List.of(new FinishCall(0, false, null)), session.awaitFinishCalls());
        awaitCondition(() -> controller.isCommandEnabled(ActionProvider.COMMAND_TEST));
    }

    private static FileObject fileObject(Path path) {
        FileUtil.refreshFor(path.toFile());
        FileObject result = FileUtil.toFileObject(path.toFile());
        if (result == null) {
            throw new AssertionError("No FileObject for " + path);
        }
        return result;
    }

    private static void awaitCondition(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Condition was not satisfied within " + TIMEOUT);
            }
            Thread.sleep(10);
        }
    }

    private record FakeProject(FileObject projectDirectory) implements Project {
        @Override
        public FileObject getProjectDirectory() {
            return projectDirectory;
        }

        @Override
        public Lookup getLookup() {
            return Lookup.EMPTY;
        }
    }

    private static final class FakeBackend implements FlutterExecutionBackend {
        private final BlockingQueue<ControlledFuture> planned = new LinkedBlockingQueue<>();
        private final BlockingQueue<FlutterExecutionRequest> requests = new LinkedBlockingQueue<>();
        private final AtomicInteger starts = new AtomicInteger();

        ControlledFuture plan(boolean honorCancellation) {
            ControlledFuture future = new ControlledFuture(honorCancellation);
            planned.add(future);
            return future;
        }

        @Override
        public Future<Integer> start(FlutterExecutionRequest request) {
            ControlledFuture future = planned.poll();
            if (future == null) {
                throw new AssertionError("No future was planned for " + request.displayName());
            }
            starts.incrementAndGet();
            requests.add(request);
            return future;
        }

        FlutterExecutionRequest awaitRequest() throws Exception {
            FlutterExecutionRequest request = requests.poll(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (request == null) {
                throw new AssertionError("Flutter execution was not started within " + TIMEOUT);
            }
            return request;
        }

        int startCount() {
            return starts.get();
        }
    }

    private static final class ControlledFuture implements Future<Integer> {
        private final boolean honorCancellation;
        private final CompletableFuture<Integer> completion = new CompletableFuture<>();
        private final CompletableFuture<Void> cancelRequested = new CompletableFuture<>();
        private final CompletableFuture<Void> getReturned = new CompletableFuture<>();

        ControlledFuture(boolean honorCancellation) {
            this.honorCancellation = honorCancellation;
        }

        void complete(int exitCode) {
            completion.complete(exitCode);
        }

        boolean awaitCancelRequested() throws Exception {
            cancelRequested.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        }

        void awaitGetReturned() throws Exception {
            getReturned.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelRequested.complete(null);
            return honorCancellation && completion.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return completion.isCancelled();
        }

        @Override
        public boolean isDone() {
            return completion.isDone();
        }

        @Override
        public Integer get() throws InterruptedException, ExecutionException {
            try {
                return completion.get();
            } catch (CancellationException ex) {
                throw ex;
            } finally {
                getReturned.complete(null);
            }
        }

        @Override
        public Integer get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return completion.get(timeout, unit);
            } finally {
                getReturned.complete(null);
            }
        }
    }

    private static final class RecordingProgress extends ActionProgress {
        private final List<Boolean> finished = new ArrayList<>();
        private final CompletableFuture<Void> firstFinish = new CompletableFuture<>();

        @Override
        protected void started() {
        }

        @Override
        public synchronized void finished(boolean success) {
            finished.add(success);
            firstFinish.complete(null);
        }

        List<Boolean> awaitFinished() throws Exception {
            firstFinish.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return finishedValues();
        }

        synchronized List<Boolean> finishedValues() {
            return List.copyOf(finished);
        }
    }

    private static final class FakeTestSessionFactory implements FlutterTestSessionFactory {
        private final BlockingQueue<FakeTestSessionBridge> sessions = new LinkedBlockingQueue<>();
        private final List<FlutterToolCommand> commands = new ArrayList<>();
        private Consumer<FlutterToolCommand> rerun;

        @Override
        public synchronized FlutterTestSessionBridge create(
                Project project,
                Path projectRoot,
                String displayName,
                FlutterToolCommand command,
                Consumer<FlutterToolCommand> rerun) {
            FakeTestSessionBridge bridge = new FakeTestSessionBridge();
            commands.add(command);
            this.rerun = rerun;
            sessions.add(bridge);
            return bridge;
        }

        FakeTestSessionBridge awaitSession() throws Exception {
            FakeTestSessionBridge bridge = sessions.poll(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (bridge == null) {
                throw new AssertionError("Test Results session was not created within " + TIMEOUT);
            }
            return bridge;
        }
    }

    private static final class FakeTestSessionBridge implements FlutterTestSessionBridge {
        private final List<String> stdout = new ArrayList<>();
        private final List<String> stderr = new ArrayList<>();
        private final List<FinishCall> finishes = new ArrayList<>();
        private final CompletableFuture<Void> firstFinish = new CompletableFuture<>();

        @Override
        public synchronized void standardOutput(String line) {
            stdout.add(line);
        }

        @Override
        public synchronized void standardError(String line) {
            stderr.add(line);
        }

        @Override
        public synchronized void finish(int exitCode, boolean cancelled, Throwable failure) {
            finishes.add(new FinishCall(exitCode, cancelled, failure));
            firstFinish.complete(null);
        }

        synchronized List<String> standardOutput() {
            return List.copyOf(stdout);
        }

        synchronized List<String> standardError() {
            return List.copyOf(stderr);
        }

        List<FinishCall> awaitFinishCalls() throws Exception {
            firstFinish.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return finishCalls();
        }

        synchronized List<FinishCall> finishCalls() {
            return List.copyOf(finishes);
        }
    }

    private record FinishCall(int exitCode, boolean cancelled, Throwable failure) {
    }
}
