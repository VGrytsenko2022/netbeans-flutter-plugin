package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.api.FlutterSdk;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.openide.modules.Places;

/** Builds and reuses the isolated Windows Flutter Canvas runner in NetBeans' cache. */
public final class CanvasRunnerBuildService {
    static final Duration DEFAULT_BUILD_TIMEOUT = Duration.ofMinutes(10);
    static final int MAX_DIAGNOSTIC_CHARS = 64 * 1024;
    static final String EXPECTED_EXECUTABLE = "netbeans_flutter_canvas_runner.exe";
    private static final String BUILD_MARKER = ".netbeans-canvas-runner-build";
    private static final ConcurrentHashMap<String, BuildLock> JVM_BUILD_LOCKS =
            new ConcurrentHashMap<>();

    private final Path cacheRoot;
    private final CanvasRunnerSourceBundle sources;
    private final CanvasRunnerProcessStarter processStarter;
    private final Duration buildTimeout;
    private final Executor asyncExecutor;

    public static CanvasRunnerBuildService createDefault() throws IOException {
        return new CanvasRunnerBuildService(
                Places.getCacheSubdirectory("flutter-canvas-runner").toPath(),
                CanvasRunnerSourceBundle.packaged(),
                CanvasRunnerProcessStarter.system(),
                DEFAULT_BUILD_TIMEOUT,
                command -> Thread.ofVirtual().name("flutter-canvas-runner-build").start(command));
    }

    public CanvasRunnerBuildService(Path cacheRoot) throws IOException {
        this(cacheRoot,
                CanvasRunnerSourceBundle.packaged(),
                CanvasRunnerProcessStarter.system(),
                DEFAULT_BUILD_TIMEOUT,
                command -> Thread.ofVirtual().name("flutter-canvas-runner-build").start(command));
    }

    CanvasRunnerBuildService(
            Path cacheRoot,
            CanvasRunnerSourceBundle sources,
            CanvasRunnerProcessStarter processStarter,
            Duration buildTimeout,
            Executor asyncExecutor) {
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot").toAbsolutePath().normalize();
        this.sources = Objects.requireNonNull(sources, "sources");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
        this.buildTimeout = requirePositive(buildTimeout, "buildTimeout");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
    }

    /** Schedules the potentially expensive SDK build outside the Swing event thread. */
    public CompletableFuture<CanvasRunnerBuildResult> buildAsync(FlutterSdk sdk) {
        Objects.requireNonNull(sdk, "sdk");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return build(sdk);
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        }, asyncExecutor);
    }

    /** Blocking build helper. Call {@link #buildAsync(FlutterSdk)} from UI code. */
    public CanvasRunnerBuildResult build(FlutterSdk sdk) throws IOException {
        Objects.requireNonNull(sdk, "sdk");
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(sources, sdk);
        BuildLock processLock = retainBuildLock(identity.cacheKey());
        processLock.lock.lock();
        try {
            return buildWithCrossProcessLock(sdk, identity);
        } finally {
            processLock.lock.unlock();
            releaseBuildLock(identity.cacheKey(), processLock);
        }
    }

    private static BuildLock retainBuildLock(String cacheKey) {
        return JVM_BUILD_LOCKS.compute(cacheKey, (_key, current) -> {
            BuildLock retained = current == null ? new BuildLock() : current;
            retained.references++;
            return retained;
        });
    }

    private static void releaseBuildLock(String cacheKey, BuildLock released) {
        JVM_BUILD_LOCKS.compute(cacheKey, (_key, current) -> {
            if (current != released) {
                throw new IllegalStateException("Canvas runner JVM build lock identity changed");
            }
            if (--released.references < 0) {
                throw new IllegalStateException("Canvas runner JVM build lock reference underflow");
            }
            return released.references == 0 ? null : released;
        });
    }

    private CanvasRunnerBuildResult buildWithCrossProcessLock(
            FlutterSdk sdk,
            CanvasRunnerCacheIdentity identity) throws IOException {
        Files.createDirectories(cacheRoot);
        if (Files.isSymbolicLink(cacheRoot)
                || !Files.isDirectory(cacheRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Canvas runner cache root is not a safe directory: " + cacheRoot);
        }
        Path lockPath = cacheRoot.resolve("CanvasRunnerBuild-"
                + identity.cacheDirectoryName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                FileLock ignored = channel.lock()) {
            rejectSymlink(lockPath, "Canvas runner build lock");
            Path sourceDirectory = sources.extract(cacheRoot, identity);
            Path cached = completedExecutable(sourceDirectory, identity);
            if (cached != null) {
                return leasedResult(
                        cached, sourceDirectory, identity, false, "");
            }
            CanvasRunnerRuntimeCache.discardIncompleteBuild(sourceDirectory, BUILD_MARKER);
            sources.verifyMutableBuildPaths(sourceDirectory);

            List<String> command = List.of(
                    sdk.flutterExecutable().toString(),
                    "build",
                    "windows",
                    "--debug");
            Process process = processStarter.start(command, sourceDirectory);
            BoundedDiagnostics diagnostics = new BoundedDiagnostics(MAX_DIAGNOSTIC_CHARS);
            Thread drainer = Thread.ofVirtual()
                    .name("flutter-canvas-runner-build-output")
                    .start(() -> drain(process, diagnostics));
            boolean finished;
            try {
                finished = process.waitFor(buildTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                terminate(process);
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while building the Flutter Canvas runner", ex);
            }
            if (!finished) {
                terminate(process);
                awaitDrainer(drainer);
                throw new IOException("Flutter Canvas runner build exceeded "
                        + buildTimeout.toSeconds() + " seconds\n" + diagnostics.snapshot());
            }
            awaitDrainer(drainer);
            IOException readFailure = diagnostics.readFailure();
            if (readFailure != null) {
                throw new IOException("cannot read Flutter Canvas runner build diagnostics", readFailure);
            }
            if (process.exitValue() != 0) {
                throw new IOException("Flutter Canvas runner build failed with exit code "
                        + process.exitValue() + "\n" + diagnostics.snapshot());
            }
            Path executable = locateExactlyOneExpectedExecutable(sourceDirectory);
            Path publishedExecutable = CanvasRunnerRuntimeCache.commit(
                    sourceDirectory, executable, identity.cacheKey());
            writeBuildMarker(sourceDirectory, identity);
            return leasedResult(
                    publishedExecutable,
                    sourceDirectory,
                    identity,
                    true,
                    diagnostics.snapshot());
        }
    }

    private static CanvasRunnerBuildResult leasedResult(
            Path executable,
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity,
            boolean builtNow,
            String diagnostics) throws IOException {
        CanvasRunnerRuntimeLease lease = CanvasRunnerRuntimeLease.acquire(
                executable.getParent());
        try {
            CanvasRunnerRuntimeCache.pruneUnusedGenerations(
                    sourceDirectory, executable);
            return new CanvasRunnerBuildResult(
                    executable,
                    sourceDirectory,
                    identity,
                    builtNow,
                    diagnostics,
                    lease);
        } catch (IOException | RuntimeException failure) {
            lease.close();
            throw failure;
        }
    }

    private static Path completedExecutable(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity) throws IOException {
        Path marker = sourceDirectory.resolve(BUILD_MARKER);
        rejectSymlink(marker, "Canvas runner build marker");
        if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                || Files.size(marker) > 256) {
            return null;
        }
        String expected = identity.cacheKey() + "\n" + EXPECTED_EXECUTABLE + "\n";
        if (!expected.equals(Files.readString(marker, StandardCharsets.US_ASCII))) {
            return null;
        }
        return CanvasRunnerRuntimeCache.validate(sourceDirectory, identity.cacheKey());
    }

    private static void writeBuildMarker(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity) throws IOException {
        Path marker = sourceDirectory.resolve(BUILD_MARKER);
        rejectSymlink(marker, "Canvas runner build marker");
        byte[] bytes = (identity.cacheKey() + "\n" + EXPECTED_EXECUTABLE + "\n")
                .getBytes(StandardCharsets.US_ASCII);
        CanvasRunnerRuntimeCache.writeAtomically(
                marker, bytes, "Canvas runner build marker");
    }

    static Path locateExactlyOneExpectedExecutable(Path sourceDirectory) throws IOException {
        Path buildRoot = sourceDirectory.resolve("build").resolve("windows");
        rejectSymlink(buildRoot, "Canvas runner Windows build directory");
        if (!Files.isDirectory(buildRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Flutter did not create the Canvas runner Windows build directory");
        }
        List<Path> matches;
        try (Stream<Path> files = Files.find(buildRoot, 12,
                (path, attributes) -> attributes.isRegularFile()
                        && !Files.isSymbolicLink(path)
                        && EXPECTED_EXECUTABLE.equals(path.getFileName().toString()))) {
            matches = files.limit(2).map(Path::toAbsolutePath).map(Path::normalize).toList();
        }
        if (matches.size() != 1 || !matches.getFirst().startsWith(sourceDirectory)) {
            throw new IOException("expected exactly one " + EXPECTED_EXECUTABLE
                    + " in the Canvas runner build, found " + matches.size());
        }
        return matches.getFirst();
    }

    private static void drain(Process process, BoundedDiagnostics diagnostics) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                if (count > 0) {
                    diagnostics.append(buffer, count);
                }
            }
        } catch (IOException ex) {
            if (process.isAlive()) {
                diagnostics.fail(ex);
            }
        }
    }

    private static void awaitDrainer(Thread drainer) throws IOException {
        boolean interrupted = false;
        try {
            for (int attempt = 0; attempt < 20 && drainer.isAlive(); attempt++) {
                try {
                    drainer.join(100);
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (drainer.isAlive()) {
                drainer.interrupt();
                throw new IOException("Flutter Canvas runner build output did not close");
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void terminate(Process process) {
        Map<Long, ProcessHandle> descendants = new LinkedHashMap<>();
        boolean interrupted = false;
        try {
            captureDescendants(process, descendants);
            destroyDescendants(descendants, false);
            process.destroy();

            long gracefulDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (treeIsAlive(process, descendants)
                    && System.nanoTime() < gracefulDeadline) {
                captureDescendants(process, descendants);
                destroyDescendants(descendants, false);
                interrupted |= terminationPause();
            }

            captureDescendants(process, descendants);
            destroyDescendants(descendants, true);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            long forcedDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (treeIsAlive(process, descendants)
                    && System.nanoTime() < forcedDeadline) {
                captureDescendants(process, descendants);
                destroyDescendants(descendants, true);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                interrupted |= terminationPause();
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void captureDescendants(
            Process process,
            Map<Long, ProcessHandle> captured) {
        try (Stream<ProcessHandle> stream = process.descendants()) {
            stream.limit(1_024).forEach(handle -> captured.putIfAbsent(handle.pid(), handle));
        } catch (RuntimeException ignored) {
            // A test process or restrictive platform may not expose descendants.
        }
        // A child may outlive and become detached from the root process. Keep
        // discovering below every already-captured live handle until the tree
        // is physically gone.
        for (ProcessHandle handle : List.copyOf(captured.values())) {
            if (!handle.isAlive()) {
                continue;
            }
            try (Stream<ProcessHandle> stream = handle.descendants()) {
                stream.limit(1_024 - Math.min(1_024, captured.size()))
                        .forEach(child -> captured.putIfAbsent(child.pid(), child));
            } catch (RuntimeException ignored) {
                // Best effort on platforms that restrict process-tree queries.
            }
        }
    }

    private static void destroyDescendants(
            Map<Long, ProcessHandle> descendants,
            boolean forcibly) {
        descendants.values().stream()
                .filter(ProcessHandle::isAlive)
                .sorted(Comparator.reverseOrder())
                .forEach(handle -> {
                    try {
                        if (forcibly) {
                            handle.destroyForcibly();
                        } else {
                            handle.destroy();
                        }
                    } catch (RuntimeException ignored) {
                        // Rechecked during the bounded termination loop.
                    }
                });
    }

    private static boolean treeIsAlive(
            Process process,
            Map<Long, ProcessHandle> descendants) {
        return process.isAlive()
                || descendants.values().stream().anyMatch(ProcessHandle::isAlive);
    }

    private static boolean terminationPause() {
        try {
            Thread.sleep(25);
            return false;
        } catch (InterruptedException ex) {
            return true;
        }
    }

    private static void rejectSymlink(Path path, String label) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link: " + path);
        }
    }

    private static Duration requirePositive(Duration duration, String label) {
        Objects.requireNonNull(duration, label);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return duration;
    }

    interface CanvasRunnerProcessStarter {
        Process start(List<String> command, Path workingDirectory) throws IOException;

        static CanvasRunnerProcessStarter system() {
            return (command, workingDirectory) -> new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
        }
    }

    private static final class BuildLock {
        private final ReentrantLock lock = new ReentrantLock();
        /** Accessed only while the cache key is held by ConcurrentHashMap.compute. */
        private int references;
    }

    private static final class BoundedDiagnostics {
        private final int limit;
        private final StringBuilder tail = new StringBuilder();
        private IOException readFailure;

        private BoundedDiagnostics(int limit) {
            this.limit = limit;
        }

        synchronized void append(char[] value, int count) {
            if (count >= limit) {
                tail.setLength(0);
                tail.append(value, count - limit, limit);
                return;
            }
            int excess = tail.length() + count - limit;
            if (excess > 0) {
                tail.delete(0, excess);
            }
            tail.append(value, 0, count);
        }

        synchronized void fail(IOException failure) {
            if (readFailure == null) {
                readFailure = failure;
            }
        }

        synchronized IOException readFailure() {
            return readFailure;
        }

        synchronized String snapshot() {
            return tail.toString();
        }
    }
}
