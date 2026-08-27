package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterSdk;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CanvasRunnerBuildServiceTest {
    @TempDir
    Path temporary;

    @Test
    void defaultCacheRootUsesShortDeterministicUserTempOnWindows() {
        Path netBeansCache = temporary.resolve(
                "an-intentionally-long-netbeans-user-directory")
                .resolve("var/cache/flutter-canvas-runner");
        Path userTemp = temporary.resolve("user-temp");
        Path expected = userTemp.resolve("nb-fcr").toAbsolutePath().normalize();

        Path windows = CanvasRunnerBuildService.resolveDefaultCacheRoot(
                "Windows 11", netBeansCache, userTemp);
        Path windowsCaseInsensitive = CanvasRunnerBuildService.resolveDefaultCacheRoot(
                "wInDoWs Server 2025", netBeansCache, userTemp);

        assertEquals(expected, windows);
        assertEquals(expected, windowsCaseInsensitive);
        assertFalse(windows.startsWith(netBeansCache.toAbsolutePath().normalize()));
        assertTrue(windows.toString().length()
                < netBeansCache.toAbsolutePath().normalize().toString().length());
    }

    @Test
    void defaultCacheRootKeepsNetBeansPlacesCacheOutsideWindows() {
        Path netBeansCache = temporary.resolve("netbeans-cache");
        Path userTemp = temporary.resolve("user-temp");
        Path expected = netBeansCache.toAbsolutePath().normalize();

        assertEquals(expected, CanvasRunnerBuildService.resolveDefaultCacheRoot(
                "Linux", netBeansCache, userTemp));
        assertEquals(expected, CanvasRunnerBuildService.resolveDefaultCacheRoot(
                "Mac OS X", netBeansCache, userTemp));
    }

    @Test
    void asyncBuildUsesOnlyExtractedCacheAndThenReusesExactExecutable() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle();
        FlutterSdk sdk = sdk();
        AtomicInteger starts = new AtomicInteger();
        CanvasRunnerBuildService.CanvasRunnerProcessStarter starter = (command, workingDirectory) -> {
            starts.incrementAndGet();
            assertEquals(workingDirectory, workingDirectory.toAbsolutePath().normalize());
            assertTrue(workingDirectory.startsWith(temporary.resolve("cache").toAbsolutePath()));
            assertEquals(List.of(sdk.flutterExecutable().toString(),
                    "build", "windows", "--release"), command);
            writeRuntime(workingDirectory);
            return new TestProcess(0, "build ok\n", false);
        };
        CanvasRunnerBuildService service = service(bundle, starter, Duration.ofSeconds(1));

        CanvasRunnerBuildResult first = service.buildAsync(sdk).get();
        CanvasRunnerBuildResult second = service.build(sdk);
        try (first; second) {
            assertTrue(first.builtNow());
            assertFalse(second.builtNow());
            assertEquals(first.executable(), second.executable());
            assertEquals(1, starts.get());
            assertEquals("build ok\n", first.diagnostics());
        }
    }

    @Test
    void missingRequiredRuntimeFileInvalidatesCacheAndForcesCleanRebuild() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle();
        FlutterSdk sdk = sdk();
        AtomicInteger starts = new AtomicInteger();
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            starts.incrementAndGet();
            writeRuntime(workingDirectory);
            return new TestProcess(0, "rebuilt\n", false);
        }, Duration.ofSeconds(1));

        CanvasRunnerBuildResult first = service.build(sdk);
        Path runtime = first.executable().getParent();
        Files.delete(runtime.resolve("flutter_windows.dll"));
        CanvasRunnerBuildResult rebuilt = service.build(sdk);
        try {
            assertTrue(rebuilt.builtNow());
            assertEquals(2, starts.get());
            assertTrue(Files.isRegularFile(
                    rebuilt.executable().getParent().resolve("flutter_windows.dll")));
            assertNotEquals(runtime, rebuilt.executable().getParent());
            assertTrue(Files.isDirectory(runtime),
                    "active corrupt generation must not be deleted during rebuild");
        } finally {
            first.close();
        }
        try (rebuilt; CanvasRunnerBuildResult reused = service.build(sdk)) {
            assertFalse(Files.exists(runtime),
                    "unleased obsolete generation is pruned on later reuse");
            assertFalse(Files.exists(runtime.getParent().resolveSibling(
                    runtime.getParent().getFileName() + ".lease")),
                    "pruned generation sidecar lease must not accumulate");
        }
    }

    @Test
    void tamperedFlutterAssetInvalidatesCacheAndForcesCleanRebuild() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle();
        FlutterSdk sdk = sdk();
        AtomicInteger starts = new AtomicInteger();
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            starts.incrementAndGet();
            writeRuntime(workingDirectory);
            return new TestProcess(0, "rebuilt\n", false);
        }, Duration.ofSeconds(1));

        CanvasRunnerBuildResult first = service.build(sdk);
        Path application = first.executable().getParent().resolve("data/app.so");
        Files.writeString(application, "tampered");
        CanvasRunnerBuildResult rebuilt = service.build(sdk);
        try (first; rebuilt) {
            assertTrue(rebuilt.builtNow());
            assertEquals(2, starts.get());
            assertEquals("application", Files.readString(rebuilt.executable().getParent()
                    .resolve("data/app.so")));
            assertEquals("tampered", Files.readString(application),
                    "leased generation stays immutable and is not repaired in place");
        }
    }

    @Test
    void successfulBuildWithEmptyRequiredRuntimeFileIsRejected() throws Exception {
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            Path executable = writeRuntime(workingDirectory);
            Files.write(executable.getParent().resolve("data/app.so"),
                    new byte[0]);
            return new TestProcess(0, "partial build\n", false);
        }, Duration.ofSeconds(1));

        IOException error = assertThrows(IOException.class, () -> service.build(sdk()));

        assertTrue(error.getMessage().contains("required runtime file is empty"));
    }

    @Test
    void failedBuildKeepsOnlyBoundedDiagnosticTail() throws Exception {
        String output = "x".repeat(CanvasRunnerBuildService.MAX_DIAGNOSTIC_CHARS + 4096)
                + "LAST-DIAGNOSTIC";
        CanvasRunnerBuildService service = service(bundle(),
                (_command, _directory) -> new TestProcess(7, output, false),
                Duration.ofSeconds(1));

        IOException error = assertThrows(IOException.class, () -> service.build(sdk()));

        assertTrue(error.getMessage().contains("exit code 7"));
        assertTrue(error.getMessage().endsWith("LAST-DIAGNOSTIC"));
        assertTrue(error.getMessage().length()
                <= CanvasRunnerBuildService.MAX_DIAGNOSTIC_CHARS + 100);
    }

    @Test
    void timeoutTerminatesBuildProcess() throws Exception {
        TestProcess process = new TestProcess(0, "still building", true);
        CanvasRunnerBuildService service = service(bundle(),
                (_command, _directory) -> process,
                Duration.ofMillis(10));

        IOException error = assertThrows(IOException.class, () -> service.build(sdk()));

        assertTrue(error.getMessage().contains("exceeded"));
        assertFalse(process.isAlive());
    }

    @Test
    void concurrentBuildsShareOneStableJvmAndCrossProcessCriticalSection()
            throws Exception {
        int callers = 16;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger processStarts = new AtomicInteger();
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            processStarts.incrementAndGet();
            writeRuntime(workingDirectory);
            return new TestProcess(0, "built\n", false);
        }, Duration.ofSeconds(2));
        FlutterSdk sdk = sdk();
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        try {
            List<Future<CanvasRunnerBuildResult>> futures = new java.util.ArrayList<>();
            for (int index = 0; index < callers; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(2, TimeUnit.SECONDS));
                    return service.build(sdk);
                }));
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            int builtNow = 0;
            for (Future<CanvasRunnerBuildResult> future : futures) {
                try (CanvasRunnerBuildResult result = future.get(5, TimeUnit.SECONDS)) {
                    if (result.builtNow()) {
                        builtNow++;
                    }
                }
            }
            assertEquals(1, builtNow);
            assertEquals(1, processStarts.get());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void timeoutWaitsForLiveDescendantBeforeReturningFromBuild() throws Exception {
        DelayedProcessHandle child = new DelayedProcessHandle();
        TestProcess process = new TestProcess(
                0, "child owns output\n", true, List.of(child));
        CanvasRunnerBuildService service = service(bundle(),
                (_command, _directory) -> process,
                Duration.ofMillis(10));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<CanvasRunnerBuildResult> build = executor.submit(() -> service.build(sdk()));
            assertTrue(child.awaitDestroyRequested());
            assertThrows(TimeoutException.class,
                    () -> build.get(100, TimeUnit.MILLISECONDS));

            child.completeExit();
            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> build.get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof IOException);
            assertTrue(failure.getCause().getMessage().contains("exceeded"));
            assertFalse(process.isAlive());
            assertFalse(child.isAlive());
        } finally {
            child.completeExit();
            executor.shutdownNow();
        }
    }

    @Test
    void leaseAcquisitionRacingCleanupCannotReturnDeletedGeneration() throws Exception {
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            writeRuntime(workingDirectory);
            return new TestProcess(0, "built\n", false);
        }, Duration.ofSeconds(1));
        CanvasRunnerBuildResult result = service.build(sdk());
        Path runtime = result.executable().getParent();
        Path generation = runtime.getParent();
        result.close();

        CanvasRunnerRuntimeLease.CleanupLease cleanup =
                CanvasRunnerRuntimeLease.tryAcquireCleanup(generation);
        assertTrue(cleanup != null);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch acquiring = new CountDownLatch(1);
            Future<CanvasRunnerRuntimeLease> raced = executor.submit(() -> {
                acquiring.countDown();
                return CanvasRunnerRuntimeLease.acquire(runtime);
            });
            assertTrue(acquiring.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> raced.get(100, TimeUnit.MILLISECONDS));

            try (Stream<Path> paths = Files.walk(generation)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
            cleanup.close();
            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> raced.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof IOException);
            assertTrue(failure.getCause().getMessage().contains("disappeared"));
        } finally {
            cleanup.close();
            executor.shutdownNow();
        }
    }

    @Test
    void abandonedStagingGenerationIsCleanedOnCacheReuse() throws Exception {
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            writeRuntime(workingDirectory);
            return new TestProcess(0, "built\n", false);
        }, Duration.ofSeconds(1));
        CanvasRunnerBuildResult first = service.build(sdk());
        Path generations = first.executable().getParent().getParent().getParent();
        first.close();
        Path abandoned = Files.createDirectories(
                generations.resolve(".staging-crashed/runtime"));
        Files.writeString(abandoned.resolve("partial"), "partial");
        Path orphanLease = generations.resolve("runtime-crashed.lease");
        Files.write(orphanLease, new byte[] {1});

        try (CanvasRunnerBuildResult reused = service.build(sdk())) {
            assertFalse(reused.builtNow());
            assertFalse(Files.exists(abandoned.getParent()));
            assertFalse(Files.exists(orphanLease));
        }
    }

    @Test
    void runtimeLeaseRetainIsRefcountedAndCloseIsIdempotent() throws Exception {
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            writeRuntime(workingDirectory);
            return new TestProcess(0, "built\n", false);
        }, Duration.ofSeconds(1));
        CanvasRunnerBuildResult result = service.build(sdk());
        Path generation = result.executable().getParent().getParent();
        CanvasRunnerRuntimeLease child = result.retainRuntimeLease();

        result.close();
        result.close();
        assertTrue(result.runtimeLease().isClosed());
        assertFalse(child.isClosed());
        assertTrue(CanvasRunnerRuntimeLease.isLeasedInJvm(generation));
        assertThrows(IllegalStateException.class, result::retainRuntimeLease);

        child.close();
        child.close();
        assertTrue(child.isClosed());
        assertFalse(CanvasRunnerRuntimeLease.isLeasedInJvm(generation));

        CanvasRunnerRuntimeLease detached = CanvasRunnerRuntimeLease.detached();
        CanvasRunnerRuntimeLease detachedChild = detached.retain();
        detached.close();
        assertFalse(detachedChild.isClosed());
        assertThrows(IllegalStateException.class, detached::retain);
        detachedChild.close();
    }

    @Test
    void legacyManifestPointingIntoMutableBuildIsRebuiltIntoGeneration() throws Exception {
        AtomicInteger starts = new AtomicInteger();
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            starts.incrementAndGet();
            writeRuntime(workingDirectory);
            return new TestProcess(0, "built\n", false);
        }, Duration.ofSeconds(1));
        CanvasRunnerBuildResult first = service.build(sdk());
        Path source = first.sourceDirectory();
        first.close();
        Path manifest = source.resolve(".netbeans-canvas-runner-runtime-v1");
        String legacyRuntime = "build/windows/x64/runner/Debug";
        String text = Files.readString(manifest).replaceFirst(
                "(?m)^runtime\\|.*$", "runtime|" + legacyRuntime);
        Files.writeString(manifest, text);

        try (CanvasRunnerBuildResult rebuilt = service.build(sdk())) {
            assertTrue(rebuilt.builtNow());
            assertEquals(2, starts.get());
            assertTrue(rebuilt.executable().toString()
                    .contains(".netbeans-canvas-runtime-generations-v1"));
        }
    }

    @Test
    void moreThanOneExpectedExecutableIsRejected() throws Exception {
        CanvasRunnerBuildService service = service(bundle(), (_command, workingDirectory) -> {
            Path first = workingDirectory.resolve("build/windows/a/")
                    .resolve(CanvasRunnerBuildService.EXPECTED_EXECUTABLE);
            Path second = workingDirectory.resolve("build/windows/b/")
                    .resolve(CanvasRunnerBuildService.EXPECTED_EXECUTABLE);
            Files.createDirectories(first.getParent());
            Files.createDirectories(second.getParent());
            Files.writeString(first, "one");
            Files.writeString(second, "two");
            return new TestProcess(0, "", false);
        }, Duration.ofSeconds(1));

        IOException error = assertThrows(IOException.class, () -> service.build(sdk()));

        assertTrue(error.getMessage().contains("expected exactly one"));
    }

    @Test
    void buildsPackagedRunnerWithConfiguredFlutterSdkWhenRequested() throws Exception {
        String configuredHome = System.getProperty("canvas.runner.flutter.sdk", "");
        Assumptions.assumeFalse(configuredHome.isBlank(),
                "set -Dcanvas.runner.flutter.sdk to run the native build smoke test");
        Path sdkHome = Path.of(configuredHome);
        Path flutter = sdkHome.resolve("bin").resolve(
                System.getProperty("os.name", "").startsWith("Windows")
                        ? "flutter.bat"
                        : "flutter");
        CanvasRunnerBuildService service = new CanvasRunnerBuildService(
                temporary.resolve("real-cache"),
                CanvasRunnerSourceBundle.packaged(),
                CanvasRunnerBuildService.CanvasRunnerProcessStarter.system(),
                Duration.ofMinutes(5),
                Runnable::run);

        CanvasRunnerBuildResult result = service.build(new FlutterSdk(sdkHome, flutter));
        try (result) {
            assertTrue(Files.isRegularFile(result.executable()));
            assertEquals(CanvasRunnerBuildService.EXPECTED_EXECUTABLE,
                    result.executable().getFileName().toString());
        }
    }

    private CanvasRunnerBuildService service(
            CanvasRunnerSourceBundle bundle,
            CanvasRunnerBuildService.CanvasRunnerProcessStarter starter,
            Duration timeout) {
        return new CanvasRunnerBuildService(
                temporary.resolve("cache"), bundle, starter, timeout, Runnable::run);
    }

    private FlutterSdk sdk() throws IOException {
        Path sdk = temporary.resolve("flutter-sdk");
        Path executable = sdk.resolve("bin/flutter.bat");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable, "@echo off\n");
        Path engine = sdk.resolve("bin/internal/engine.version");
        Files.createDirectories(engine.getParent());
        Files.writeString(engine, "engine-test\n");
        Files.writeString(sdk.resolve("version"), "3.test\n");
        return new FlutterSdk(sdk, executable);
    }

    private static CanvasRunnerSourceBundle bundle() throws IOException {
        Map<String, String> sources = Map.of(
                "lib/main.dart", "void main() {}\n",
                "pubspec.yaml", "name: netbeans_flutter_canvas_runner\n");
        StringBuilder manifest = new StringBuilder();
        sources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            manifest.append(entry.getKey()).append('|').append(bytes.length).append('|')
                    .append(sha256(bytes)).append('\n');
        });
        return new CanvasRunnerSourceBundle(new CanvasRunnerSourceBundle.ResourceAccess() {
            @Override
            public InputStream openManifest() {
                return new ByteArrayInputStream(
                        manifest.toString().getBytes(StandardCharsets.US_ASCII));
            }

            @Override
            public InputStream openSource(String relativePath) {
                return new ByteArrayInputStream(
                        sources.get(relativePath).getBytes(StandardCharsets.UTF_8));
            }
        }, "test");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static Path writeRuntime(Path workingDirectory) throws IOException {
        Path runtime = workingDirectory.resolve("build/windows/x64/runner/Release");
        Map<String, String> files = Map.ofEntries(
                Map.entry(CanvasRunnerBuildService.EXPECTED_EXECUTABLE, "exe"),
                Map.entry("flutter_windows.dll", "engine"),
                Map.entry("data/icudtl.dat", "icu"),
                Map.entry("data/app.so", "application"),
                Map.entry("data/flutter_assets/AssetManifest.bin", "assets"),
                Map.entry("data/flutter_assets/FontManifest.json", "fonts"),
                Map.entry("data/flutter_assets/NativeAssetsManifest.json", "native-assets"),
                Map.entry("data/flutter_assets/NOTICES.Z", "notices"),
                Map.entry("data/flutter_assets/fonts/MaterialIcons-Regular.otf", "material-icons"),
                Map.entry("data/flutter_assets/shaders/ink_sparkle.frag", "shader"));
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path target = runtime.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue());
        }
        Files.writeString(runtime.resolve("netbeans_flutter_canvas_runner.pdb"), "debug-symbols");
        return runtime.resolve(CanvasRunnerBuildService.EXPECTED_EXECUTABLE);
    }

    private static final class TestProcess extends Process {
        private final InputStream output;
        private final int exitCode;
        private final List<ProcessHandle> descendants;
        private volatile boolean alive;

        private TestProcess(int exitCode, String output, boolean alive) {
            this(exitCode, output, alive, List.of());
        }

        private TestProcess(
                int exitCode,
                String output,
                boolean alive,
                List<ProcessHandle> descendants) {
            this.exitCode = exitCode;
            this.output = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
            this.alive = alive;
            this.descendants = List.copyOf(descendants);
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return output;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            alive = false;
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) {
            if (alive) {
                return false;
            }
            return true;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException();
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return descendants.stream();
        }
    }

    private static final class DelayedProcessHandle implements ProcessHandle {
        private static final AtomicLong NEXT_PID = new AtomicLong(100_000);
        private final long pid = NEXT_PID.incrementAndGet();
        private final CountDownLatch destroyRequested = new CountDownLatch(1);
        private final java.util.concurrent.CompletableFuture<ProcessHandle> exit =
                new java.util.concurrent.CompletableFuture<>();
        private volatile boolean alive = true;

        boolean awaitDestroyRequested() throws InterruptedException {
            return destroyRequested.await(2, TimeUnit.SECONDS);
        }

        void completeExit() {
            alive = false;
            exit.complete(this);
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Optional<ProcessHandle> parent() {
            return Optional.empty();
        }

        @Override
        public Stream<ProcessHandle> children() {
            return Stream.empty();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }

        @Override
        public Info info() {
            return ProcessHandle.current().info();
        }

        @Override
        public java.util.concurrent.CompletableFuture<ProcessHandle> onExit() {
            return exit;
        }

        @Override
        public boolean supportsNormalTermination() {
            return true;
        }

        @Override
        public boolean destroy() {
            destroyRequested.countDown();
            return alive;
        }

        @Override
        public boolean destroyForcibly() {
            destroyRequested.countDown();
            return alive;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public int compareTo(ProcessHandle other) {
            return Long.compare(pid, other.pid());
        }
    }
}
