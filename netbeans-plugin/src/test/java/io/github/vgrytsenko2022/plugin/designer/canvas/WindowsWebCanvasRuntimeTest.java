package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WindowsWebCanvasRuntimeTest {
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.test", "framework-test", "engine-test", "3.test.1");

    @TempDir
    Path temporary;

    @Test
    void retirementBeforeBuildRegistrationCancelsTheLateBuildAndDrainsDelivery()
            throws Exception {
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("retire-before-register", ignored -> build);

        onEdt(() -> {
            fixture.runtime.start(sdk("retire-before-register"),
                    WindowsWebCanvasRuntime.Listener.NOOP);
            return null;
        });
        assertEquals(1, fixture.preparation.pendingCount());

        CompletableFuture<Void> retirement = retire(fixture.runtime);
        assertFalse(retirement.isDone());

        fixture.preparation.runNext();
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertTrue(build.isCancelled());
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertFalse(retirement.isDone());
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertFalse(retirement.isDone());
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertSuccessful(retirement);
        assertEquals(WindowsWebCanvasRuntime.Phase.CLOSED,
                phase(fixture.runtime));
    }

    @Test
    void completedBuildQueuedForUiDeliveryIsCleanedBeforeRetirementPublishes()
            throws Exception {
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("queued-delivery", ignored -> build);
        ResultFixture result = result("queued-delivery-result", ignored -> { });

        startAndRegisterBuild(fixture, build, "queued-delivery-sdk");
        assertTrue(build.complete(result.result));
        assertEquals(1, fixture.ui.pendingCount());

        CompletableFuture<Void> retirement = retire(fixture.runtime);
        assertFalse(retirement.isDone());
        assertTrue(Files.isDirectory(result.artifactRoot));

        runUiNext(fixture.ui);
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertEquals(1, fixture.preparation.pendingCount());
        assertFalse(retirement.isDone());
        fixture.preparation.runNext();

        assertFalse(retirement.isDone());
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertSuccessful(retirement);
        assertFalse(Files.exists(result.artifactRoot));
    }

    @Test
    void publicRetirementCompletesOnlyAfterEdtFinalization() throws Exception {
        RuntimeFixture fixture = fixture(
                "edt-finalization",
                ignored -> CompletableFuture.failedFuture(
                        new AssertionError("build must not start")));

        CompletableFuture<Void> retirement = retire(fixture.runtime);

        assertFalse(retirement.isDone());
        assertEquals(WindowsWebCanvasRuntime.Phase.RETIRING,
                phase(fixture.runtime));
        assertEquals(1, fixture.ui.pendingCount());

        runUiNext(fixture.ui);

        assertFalse(retirement.isDone());
        assertEquals(WindowsWebCanvasRuntime.Phase.RETIRING,
                phase(fixture.runtime));
        assertEquals(1, fixture.ui.pendingCount());

        runUiNext(fixture.ui);

        assertSuccessful(retirement);
        assertEquals(WindowsWebCanvasRuntime.Phase.CLOSED,
                phase(fixture.runtime));
    }

    @Test
    void oneExplicitRetryClearsAFailedCleanupWithoutHistoricalPoison()
            throws Exception {
        AtomicInteger deletionAttempts = new AtomicInteger();
        ResultFixture result = result("retry-result", ignored -> {
            if (deletionAttempts.incrementAndGet() == 1) {
                throw new IOException("planned first deletion failure");
            }
        });
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("retry-runtime", ignored -> build);

        startAndRegisterBuild(fixture, build, "retry-sdk");
        assertTrue(build.complete(result.result));
        runUiNext(fixture.ui);
        assertEquals(WindowsWebCanvasRuntime.Phase.PREPARED,
                phase(fixture.runtime));

        CompletableFuture<Void> first = retire(fixture.runtime);
        runUiNext(fixture.ui);
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.preparation.runNext();
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertTrue(first.isCompletedExceptionally());
        CompletionException firstFailure = assertThrows(
                CompletionException.class, first::join);
        assertTrue(firstFailure.getCause().getMessage().contains(
                "planned first deletion failure"));
        assertEquals(WindowsWebCanvasRuntime.Phase.FAILED,
                phase(fixture.runtime));
        assertEquals(1, deletionAttempts.get());
        assertTrue(Files.isDirectory(result.artifactRoot));

        CompletableFuture<Void> retry = retire(fixture.runtime);
        runUiNext(fixture.ui);
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.preparation.runNext();

        assertFalse(retry.isDone());
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertSuccessful(retry);
        assertEquals(2, deletionAttempts.get());
        assertFalse(Files.exists(result.artifactRoot));
        assertEquals(WindowsWebCanvasRuntime.Phase.CLOSED,
                phase(fixture.runtime));
    }

    @Test
    void rejectedUiDispatchCannotReportRetirementSuccess() throws Exception {
        RuntimeFixture fixture = fixture(
                "rejected-ui",
                ignored -> CompletableFuture.failedFuture(
                        new AssertionError("build must not start")));
        fixture.ui.rejectNewTasks();

        CompletableFuture<Void> retirement = retire(fixture.runtime);

        assertTrue(retirement.isCompletedExceptionally());
        CompletionException failure = assertThrows(
                CompletionException.class, retirement::join);
        assertTrue(failure.getCause().getMessage().contains(
                "Finalize exact Flutter Web Canvas retirement on the EDT"));
        assertFalse(phase(fixture.runtime)
                == WindowsWebCanvasRuntime.Phase.CLOSED);
    }

    @Test
    void rejectedRetirementFinalizationCanRetryAndReleaseRetainedResult()
            throws Exception {
        AtomicInteger deletionAttempts = new AtomicInteger();
        ResultFixture result = result("rejected-finalization-result", ignored -> {
            if (deletionAttempts.incrementAndGet() == 1) {
                throw new IOException("planned retained cleanup failure");
            }
        });
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("rejected-finalization", ignored -> build);

        startAndRegisterBuild(fixture, build, "rejected-finalization-sdk");
        assertTrue(build.complete(result.result));
        runUiNext(fixture.ui);

        CompletableFuture<Void> first = retire(fixture.runtime);
        runUiNext(fixture.ui);
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.ui.rejectNextTask();
        fixture.preparation.runNext();

        assertTrue(first.isCompletedExceptionally());
        assertEquals(WindowsWebCanvasRuntime.Phase.RETIRING,
                phase(fixture.runtime));
        assertEquals(1, deletionAttempts.get());
        assertTrue(Files.isDirectory(result.artifactRoot));

        CompletableFuture<Void> retry = retire(fixture.runtime);
        runUiNext(fixture.ui);
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.preparation.runNext();
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);

        assertSuccessful(retry);
        assertEquals(2, deletionAttempts.get());
        assertFalse(Files.exists(result.artifactRoot));
        assertEquals(WindowsWebCanvasRuntime.Phase.CLOSED,
                phase(fixture.runtime));
    }

    @Test
    void rejectedBuildRegistrationFailsVisiblyOnFallbackEdt() throws Exception {
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("rejected-registration", ignored -> build);
        RecordingListener listener = new RecordingListener();

        onEdt(() -> {
            fixture.runtime.start(sdk("rejected-registration-sdk"), listener);
            return null;
        });
        fixture.ui.rejectNextTask();
        fixture.preparation.runNext();
        flushEdt();

        assertTrue(build.isCancelled());
        assertEquals(WindowsWebCanvasRuntime.Phase.FAILED,
                phase(fixture.runtime));
        assertEquals(1, listener.failedCalls);
        assertEquals("Register exact Flutter Web Canvas build on the EDT",
                listener.failedOperation);
    }

    @Test
    void rejectedBuildDeliveryFailsVisiblyAndCleansResultExactlyOnce()
            throws Exception {
        AtomicInteger deletionAttempts = new AtomicInteger();
        ResultFixture result = result("rejected-delivery-result",
                ignored -> deletionAttempts.incrementAndGet());
        CompletableFuture<WebCanvasBuildResult> build = new CompletableFuture<>();
        RuntimeFixture fixture = fixture("rejected-delivery", ignored -> build);
        RecordingListener listener = new RecordingListener();

        onEdt(() -> {
            fixture.runtime.start(sdk("rejected-delivery-sdk"), listener);
            return null;
        });
        fixture.preparation.runNext();
        runUiNext(fixture.ui);
        fixture.ui.rejectNextTask();
        assertTrue(build.complete(result.result));
        flushEdt();

        assertEquals(WindowsWebCanvasRuntime.Phase.FAILED,
                phase(fixture.runtime));
        assertEquals(1, listener.failedCalls);
        assertEquals("Deliver exact Flutter Web Canvas producer result on the EDT",
                listener.failedOperation);
        assertTrue(Files.isDirectory(result.artifactRoot));

        CompletableFuture<Void> retirement = retire(fixture.runtime);
        runUiNext(fixture.ui);
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.preparation.runNext();
        runUiNext(fixture.ui);

        assertSuccessful(retirement);
        assertEquals(1, deletionAttempts.get());
        assertFalse(Files.exists(result.artifactRoot));
    }

    @Test
    void staleHostCallbacksCannotCrossTheGenerationFence() throws Exception {
        RuntimeFixture fixture = fixture(
                "stale-host-callback",
                ignored -> CompletableFuture.failedFuture(
                        new AssertionError("build must not start")));
        RecordingListener listener = new RecordingListener();
        setField(fixture.runtime, "listener", listener);

        setField(fixture.runtime, "generation", 7L);
        setField(fixture.runtime, "phase",
                WindowsWebCanvasRuntime.Phase.STARTING);
        WindowsWebCanvasHost.Listener stale = hostListener(fixture.runtime, 6L);
        WindowsWebCanvasHost.Listener current = hostListener(fixture.runtime, 7L);

        onEdt(() -> {
            stale.nativeStarted("stale", "https://stale.invalid/");
            current.nativeStarted("current", "https://current.invalid/");
            return null;
        });

        assertEquals(1, listener.nativeStartedCalls);
        assertEquals("current", listener.runtimeVersion);

        setField(fixture.runtime, "retiringHostGeneration", 7L);
        setField(fixture.runtime, "generation", 8L);
        setField(fixture.runtime, "phase",
                WindowsWebCanvasRuntime.Phase.RETIRING);
        WindowsWebCanvasHost.Listener older = hostListener(fixture.runtime, 6L);

        onEdt(() -> {
            older.closed("stale close");
            current.closed("");
            return null;
        });

        assertEquals(1, listener.closedCalls);
    }

    private void startAndRegisterBuild(
            RuntimeFixture fixture,
            CompletableFuture<WebCanvasBuildResult> build,
            String sdkName) throws Exception {
        onEdt(() -> {
            fixture.runtime.start(sdk(sdkName),
                    WindowsWebCanvasRuntime.Listener.NOOP);
            return null;
        });
        assertEquals(1, fixture.preparation.pendingCount());
        fixture.preparation.runNext();
        assertEquals(1, fixture.ui.pendingCount());
        runUiNext(fixture.ui);
        assertEquals(WindowsWebCanvasRuntime.Phase.BUILDING,
                phase(fixture.runtime));
        assertFalse(build.isDone());
    }

    private RuntimeFixture fixture(
            String name,
            WindowsWebCanvasRuntime.BuildStarter buildStarter) throws Exception {
        Path base = Files.createDirectory(temporary.resolve(name));
        Path cacheRoot = Files.createDirectory(base.resolve("cache"));
        ManualExecutor preparation = new ManualExecutor();
        ManualExecutor ui = new ManualExecutor();
        WindowsWebCanvasHost host = onEdt(() -> new WindowsWebCanvasHost(
                new NoopNativeApi(), Runnable::run, ignored -> 1L));
        WindowsWebCanvasRuntime runtime = new WindowsWebCanvasRuntime(
                host,
                buildStarter,
                () -> new WindowsWebCanvasRuntime.Availability(
                        true, "test-webview2", ""),
                () -> cacheRoot,
                preparation,
                ui);
        return new RuntimeFixture(runtime, preparation, ui);
    }

    private ResultFixture result(
            String name,
            WebCanvasArtifactPublisher.DeletionAttempt deletionAttempt)
            throws Exception {
        Path base = Files.createDirectory(temporary.resolve(name));
        Path artifactSource = Files.createDirectory(
                base.resolve("artifact-source"));
        byte[] index = "<html>canvas</html>".getBytes(StandardCharsets.UTF_8);
        byte[] metadata = "build-id".getBytes(StandardCharsets.UTF_8);
        Files.write(artifactSource.resolve("index.html"), index);
        Files.write(artifactSource.resolve(".last_build_id"), metadata);

        WebCanvasArtifactContract.ArtifactFile indexFile = artifactFile(
                "index.html", index);
        WebCanvasArtifactContract.ArtifactFile metadataFile = artifactFile(
                ".last_build_id", metadata);
        WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                new WebCanvasArtifactContract.ArtifactSnapshot(
                        artifactSource,
                        Map.of("index.html", indexFile),
                        Map.of(".last_build_id", metadataFile),
                        index.length + metadata.length,
                        "f".repeat(64));
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                base.resolve("published"), deletionAttempt);
        WebCanvasArtifactPublisher.PublishedArtifact artifact = publisher.publish(
                snapshot, "a".repeat(64));
        Path mutableBuild = Files.createDirectory(base.resolve("mutable-build"));
        CanvasRunnerCacheIdentity cacheIdentity = new CanvasRunnerCacheIdentity(
                "1".repeat(64),
                ENGINE.engineRevision(),
                "2".repeat(64),
                "3".repeat(64));
        WebCanvasBuildResult buildResult = new WebCanvasBuildResult(
                mutableBuild,
                cacheIdentity,
                ENGINE,
                true,
                "",
                artifact);
        return new ResultFixture(buildResult, artifact.root());
    }

    private FlutterSdk sdk(String name) throws IOException {
        Path home = Files.createDirectories(temporary.resolve("sdk-" + name));
        Path executable = home.resolve("bin/flutter.bat");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable, "@echo off\n", StandardCharsets.UTF_8);
        return new FlutterSdk(home, executable);
    }

    private static WebCanvasArtifactContract.ArtifactFile artifactFile(
            String path, byte[] bytes) throws NoSuchAlgorithmException {
        return new WebCanvasArtifactContract.ArtifactFile(
                path,
                bytes.length,
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    private static CompletableFuture<Void> retire(
            WindowsWebCanvasRuntime runtime) throws Exception {
        return onEdt(() -> runtime.preparePeerRemovalAsync()
                .toCompletableFuture());
    }

    private static void runUiNext(ManualExecutor executor) throws Exception {
        onEdt(() -> {
            executor.runNext();
            return null;
        });
    }

    private static void flushEdt() throws Exception {
        onEdt(() -> null);
    }

    private static void assertSuccessful(CompletableFuture<Void> future) {
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
        future.join();
    }

    private static WindowsWebCanvasRuntime.Phase phase(
            WindowsWebCanvasRuntime runtime) throws Exception {
        Field field = WindowsWebCanvasRuntime.class.getDeclaredField("phase");
        field.setAccessible(true);
        return (WindowsWebCanvasRuntime.Phase) field.get(runtime);
    }

    private static void setField(
            WindowsWebCanvasRuntime runtime,
            String name,
            Object value) throws Exception {
        Field field = WindowsWebCanvasRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(runtime, value);
    }

    private static WindowsWebCanvasHost.Listener hostListener(
            WindowsWebCanvasRuntime runtime,
            long generation) throws Exception {
        Method method = WindowsWebCanvasRuntime.class.getDeclaredMethod(
                "hostListener", long.class, CanvasEngineIdentity.class);
        method.setAccessible(true);
        try {
            return (WindowsWebCanvasHost.Listener) method.invoke(
                    runtime, generation, ENGINE);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
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
                } catch (Throwable problem) {
                    failure.value = problem;
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (InvocationTargetException impossible) {
            throw new AssertionError(impossible.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value instanceof Error error) {
            throw error;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return result.value;
    }

    private record RuntimeFixture(
            WindowsWebCanvasRuntime runtime,
            ManualExecutor preparation,
            ManualExecutor ui) {
    }

    private record ResultFixture(
            WebCanvasBuildResult result,
            Path artifactRoot) {
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
        private boolean rejectNewTasks;
        private int tasksToReject;

        @Override
        public synchronized void execute(Runnable command) {
            if (rejectNewTasks || tasksToReject > 0) {
                if (tasksToReject > 0) {
                    tasksToReject--;
                }
                throw new IllegalStateException("planned executor rejection");
            }
            pending.addLast(Objects.requireNonNull(command, "command"));
        }

        synchronized int pendingCount() {
            return pending.size();
        }

        synchronized void rejectNewTasks() {
            rejectNewTasks = true;
        }

        synchronized void rejectNextTask() {
            tasksToReject++;
        }

        void runNext() {
            Runnable command;
            synchronized (this) {
                command = pending.removeFirst();
            }
            command.run();
        }
    }

    private static final class RecordingListener
            implements WindowsWebCanvasRuntime.Listener {
        private int nativeStartedCalls;
        private int closedCalls;
        private int failedCalls;
        private String runtimeVersion = "";
        private String failedOperation = "";

        @Override
        public void nativeStarted(String value) {
            nativeStartedCalls++;
            runtimeVersion = value;
        }

        @Override
        public void closed(String teardownFailure) {
            closedCalls++;
        }

        @Override
        public void failed(String operation, String reason) {
            failedCalls++;
            failedOperation = operation;
        }
    }

    private static final class NoopNativeApi
            implements WindowsWebView2NativeApi {
        @Override
        public String runtimeVersion() {
            return "test-webview2";
        }

        @Override
        public NativeSession create(CreateRequest request, Listener listener) {
            throw new AssertionError(
                    "a non-displayable test carrier must not start WebView2");
        }
    }
}
