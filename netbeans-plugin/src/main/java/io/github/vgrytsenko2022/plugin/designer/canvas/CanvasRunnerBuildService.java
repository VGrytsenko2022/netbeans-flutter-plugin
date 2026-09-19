package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
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
import java.util.List;
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

/** Builds and reuses one provider-contracted isolated Flutter Canvas runner. */
public final class CanvasRunnerBuildService {
    static final Duration DEFAULT_BUILD_TIMEOUT = Duration.ofMinutes(10);
    static final int MAX_DIAGNOSTIC_CHARS = 64 * 1024;
    private static final String BUILD_MARKER = ".netbeans-canvas-runner-build";
    private static final ConcurrentHashMap<String, BuildLock> JVM_BUILD_LOCKS =
            new ConcurrentHashMap<>();

    private final Path cacheRoot;
    private final NativeCanvasRunnerContract runnerContract;
    private final CanvasRunnerSourceBundle sources;
    private final CanvasRunnerProcessStarter processStarter;
    private final Duration buildTimeout;
    private final Executor asyncExecutor;

    public static CanvasRunnerBuildService createDefault(
            NativeCanvasRunnerContract runnerContract) throws IOException {
        Objects.requireNonNull(runnerContract, "runnerContract");
        Path cacheRoot = resolveDefaultCacheRoot(
                runnerContract,
                Places.getCacheSubdirectory("flutter-canvas-runner").toPath(),
                Path.of(System.getProperty("java.io.tmpdir")));
        return new CanvasRunnerBuildService(
                runnerContract,
                cacheRoot,
                CanvasRunnerSourceBundle.packaged(),
                CanvasRunnerProcessStarter.system(),
                DEFAULT_BUILD_TIMEOUT,
                command -> Thread.ofVirtual().name("flutter-canvas-runner-build").start(command));
    }

    static Path resolveDefaultCacheRoot(
            NativeCanvasRunnerContract runnerContract,
            Path netBeansCacheRoot,
            Path userTemporaryDirectory) {
        return Objects.requireNonNull(runnerContract, "runnerContract")
                .cachePolicy().resolveRoot(
                        Objects.requireNonNull(netBeansCacheRoot,
                                "netBeansCacheRoot").toAbsolutePath().normalize(),
                        Objects.requireNonNull(userTemporaryDirectory,
                                "userTemporaryDirectory").toAbsolutePath().normalize());
    }

    public CanvasRunnerBuildService(
            NativeCanvasRunnerContract runnerContract,
            Path cacheRoot) throws IOException {
        this(runnerContract, cacheRoot,
                CanvasRunnerSourceBundle.packaged(),
                CanvasRunnerProcessStarter.system(),
                DEFAULT_BUILD_TIMEOUT,
                command -> Thread.ofVirtual().name("flutter-canvas-runner-build").start(command));
    }

    CanvasRunnerBuildService(
            NativeCanvasRunnerContract runnerContract,
            Path cacheRoot,
            CanvasRunnerSourceBundle sources,
            CanvasRunnerProcessStarter processStarter,
            Duration buildTimeout,
            Executor asyncExecutor) {
        this.runnerContract = Objects.requireNonNull(
                runnerContract, "runnerContract");
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
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                sources, sdk, runnerContract);
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
            CanvasRunnerRuntimeCache.discardIncompleteBuild(
                    sourceDirectory, BUILD_MARKER, runnerContract);
            sources.verifyMutableBuildPaths(sourceDirectory);

            List<String> command = runnerContract.buildTarget().command(
                    sdk.flutterExecutable().toAbsolutePath().normalize());
            Process process = processStarter.start(command, sourceDirectory);
            BoundedDiagnostics diagnostics = new BoundedDiagnostics(MAX_DIAGNOSTIC_CHARS);
            Thread drainer = Thread.ofVirtual()
                    .name("flutter-canvas-runner-build-output")
                    .start(() -> drain(process, diagnostics));
            boolean finished;
            try {
                finished = process.waitFor(buildTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                IOException interruptedFailure = new IOException(
                        "interrupted while building the Flutter Canvas runner", ex);
                try {
                    retireProcessAndAwaitDrainer(process, drainer);
                } catch (IOException cleanupFailure) {
                    interruptedFailure.addSuppressed(cleanupFailure);
                }
                Thread.currentThread().interrupt();
                throw interruptedFailure;
            }
            if (!finished) {
                retireProcessAndAwaitDrainer(process, drainer);
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
            Path executable = locateExactlyOneExpectedExecutable(
                    sourceDirectory, runnerContract);
            Path publishedExecutable = CanvasRunnerRuntimeCache.commit(
                    sourceDirectory, executable, identity.cacheKey(), runnerContract);
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

    private Path completedExecutable(
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
        String expected = identity.cacheKey() + "\n"
                + runnerContract.buildTarget().executableName() + "\n";
        if (!expected.equals(Files.readString(marker, StandardCharsets.US_ASCII))) {
            return null;
        }
        return CanvasRunnerRuntimeCache.validate(
                sourceDirectory, identity.cacheKey(), runnerContract);
    }

    private void writeBuildMarker(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity) throws IOException {
        Path marker = sourceDirectory.resolve(BUILD_MARKER);
        rejectSymlink(marker, "Canvas runner build marker");
        byte[] bytes = (identity.cacheKey() + "\n"
                + runnerContract.buildTarget().executableName() + "\n")
                .getBytes(StandardCharsets.US_ASCII);
        CanvasRunnerRuntimeCache.writeAtomically(
                marker, bytes, "Canvas runner build marker");
    }

    static Path locateExactlyOneExpectedExecutable(
            Path sourceDirectory,
            NativeCanvasRunnerContract runnerContract) throws IOException {
        Objects.requireNonNull(runnerContract, "runnerContract");
        Path buildRoot = runnerContract.buildTarget().outputRoot(
                sourceDirectory.toAbsolutePath().normalize());
        rejectSymlink(buildRoot, "Canvas runner build output directory");
        if (!Files.isDirectory(buildRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Flutter did not create the contracted Canvas runner "
                    + "build output directory");
        }
        String expectedExecutable = runnerContract.buildTarget().executableName();
        List<Path> matches;
        try (Stream<Path> files = Files.find(
                buildRoot,
                runnerContract.buildTarget().executableSearchDepth(),
                (path, attributes) -> attributes.isRegularFile()
                        && !Files.isSymbolicLink(path)
                        && expectedExecutable.equals(path.getFileName().toString()))) {
            matches = files.limit(2).map(Path::toAbsolutePath).map(Path::normalize).toList();
        }
        if (matches.size() != 1 || !matches.getFirst().startsWith(sourceDirectory)) {
            throw new IOException("expected exactly one " + expectedExecutable
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

    private static void retireProcessAndAwaitDrainer(
            Process process,
            Thread drainer) throws IOException {
        IOException cleanupFailure = null;
        try {
            CanvasProcessTreeRetirement.retire(process);
        } catch (IOException retirementFailure) {
            cleanupFailure = retirementFailure;
            // A still-live process can keep the redirected pipe open forever.
            // Relinquish diagnostics ownership before the bounded join so the
            // virtual reader can terminate even when tree retirement failed.
            try {
                process.getInputStream().close();
            } catch (IOException streamFailure) {
                cleanupFailure.addSuppressed(streamFailure);
            }
        }
        try {
            awaitDrainer(drainer);
        } catch (IOException drainerFailure) {
            if (cleanupFailure == null) {
                cleanupFailure = drainerFailure;
            } else {
                cleanupFailure.addSuppressed(drainerFailure);
            }
        }
        if (cleanupFailure != null) {
            throw cleanupFailure;
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
