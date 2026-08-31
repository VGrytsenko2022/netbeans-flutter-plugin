package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.modules.Places;

/**
 * Builds, validates and privately publishes the exact Flutter Web Canvas.
 *
 * <p>The Flutter output remains mutable cache input and is never served to a
 * browser. Every call publishes a new immutable generation after validation,
 * including when the SDK-keyed build itself is reused.
 */
final class WebCanvasBuildService {
    private static final Logger LOG = Logger.getLogger(
            WebCanvasBuildService.class.getName());
    static final Duration DEFAULT_BUILD_TIMEOUT = Duration.ofMinutes(10);
    static final int MAX_DIAGNOSTIC_CHARACTERS = 64 * 1024;
    private static final int MAX_GENERATED_CACHE_PATHS = 200_000;
    private static final String BUILD_MARKER = ".netbeans-web-canvas-build";
    private static final String BUILD_PROFILE =
            "flutter-web-release-main-web-local-resources-full-icons-no-wasm-pwa-none-identity-v3";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, BuildLock> JVM_BUILD_LOCKS =
            new ConcurrentHashMap<>();

    private final Path cacheRoot;
    private final CanvasRunnerSourceBundle sources;
    private final ArtifactValidator artifactValidator;
    private final WebCanvasArtifactPublisher publisher;
    private final CanvasRunnerBuildService.CanvasRunnerProcessStarter processStarter;
    private final Duration buildTimeout;
    private final Executor asyncExecutor;
    private final Supplier<String> generationIds;

    static WebCanvasBuildService createDefault() throws IOException {
        Path root = Places.getCacheSubdirectory("flutter-web-canvas")
                .toPath().toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path buildRoot = root.resolve("build-cache");
        Path publicationRoot = root.resolve("published");
        Files.createDirectories(buildRoot);
        return new WebCanvasBuildService(
                buildRoot,
                CanvasRunnerSourceBundle.packaged(),
                ArtifactValidator.production(new WebCanvasArtifactContract()),
                new WebCanvasArtifactPublisher(publicationRoot),
                CanvasRunnerBuildService.CanvasRunnerProcessStarter.system(),
                DEFAULT_BUILD_TIMEOUT,
                command -> Thread.ofVirtual()
                        .name("flutter-web-canvas-build")
                        .start(command),
                WebCanvasBuildService::randomGenerationId);
    }

    WebCanvasBuildService(
            Path cacheRoot,
            CanvasRunnerSourceBundle sources,
            ArtifactValidator artifactValidator,
            WebCanvasArtifactPublisher publisher,
            CanvasRunnerBuildService.CanvasRunnerProcessStarter processStarter,
            Duration buildTimeout,
            Executor asyncExecutor,
            Supplier<String> generationIds) {
        this.cacheRoot = Objects.requireNonNull(
                cacheRoot, "cacheRoot").toAbsolutePath().normalize();
        this.sources = Objects.requireNonNull(sources, "sources");
        this.artifactValidator = Objects.requireNonNull(
                artifactValidator, "artifactValidator");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
        this.buildTimeout = requirePositive(buildTimeout, "buildTimeout");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.generationIds = Objects.requireNonNull(generationIds, "generationIds");
    }

    CompletableFuture<WebCanvasBuildResult> buildAsync(FlutterSdk sdk) {
        FlutterSdk acceptedSdk = Objects.requireNonNull(sdk, "sdk");
        CancellableBuildFuture future = new CancellableBuildFuture();
        asyncExecutor.execute(() -> runAsyncBuild(acceptedSdk, future));
        return future;
    }

    private void runAsyncBuild(
            FlutterSdk sdk,
            CancellableBuildFuture future) {
        if (!future.beginWorker()) {
            return;
        }
        try {
            WebCanvasBuildResult result = build(sdk);
            if (!future.complete(result)) {
                closeUnclaimedResult(result);
            }
        } catch (Throwable failure) {
            future.completeExceptionally(failure);
        } finally {
            future.endWorker();
        }
    }

    private static void closeUnclaimedResult(WebCanvasBuildResult result) {
        boolean interrupted = Thread.interrupted();
        IOException cleanupFailure = null;
        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    result.close();
                    return;
                } catch (IOException failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            }
            LOG.log(Level.WARNING,
                    "Cancelled Flutter Web Canvas build left an unclaimed "
                    + "publication after three cleanup attempts",
                    cleanupFailure);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    WebCanvasBuildResult build(FlutterSdk sdk) throws IOException {
        Objects.requireNonNull(sdk, "sdk");
        CanvasEngineIdentity engineIdentity = WebCanvasSdkIdentity.read(sdk);
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                sources, sdk, buildContractFingerprint(engineIdentity));
        if (!engineIdentity.engineRevision().equals(identity.engineRevision())) {
            throw new IOException(
                    "Flutter SDK engine identity changed while creating the Web Canvas cache key");
        }
        BuildLock processLock = retainBuildLock(identity.cacheKey());
        processLock.lock.lock();
        try {
            return buildWithCrossProcessLock(sdk, identity, engineIdentity);
        } finally {
            processLock.lock.unlock();
            releaseBuildLock(identity.cacheKey(), processLock);
        }
    }

    private WebCanvasBuildResult buildWithCrossProcessLock(
            FlutterSdk sdk,
            CanvasRunnerCacheIdentity identity,
            CanvasEngineIdentity engineIdentity) throws IOException {
        prepareCacheRoot();
        Path lockPath = cacheRoot.resolve(
                "WebCanvasBuild-" + identity.cacheDirectoryName() + ".lock");
        requireDirectChild(cacheRoot, lockPath, "Web Canvas build lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS);
                FileLock ignored = channel.lock()) {
            rejectLink(lockPath, "Web Canvas build lock");
            requireIdentityUnchanged(sdk, engineIdentity);
            Path sourceDirectory = sources.extract(cacheRoot, identity);
            WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                    completedSnapshot(sourceDirectory, identity);
            boolean builtNow = false;
            String diagnostics = "";
            if (snapshot == null) {
                cleanBuildOutput(sourceDirectory);
                sources.verifyMutableBuildPaths(sourceDirectory);
                BuildExecution execution = executeBuild(
                        sdk, sourceDirectory, engineIdentity);
                diagnostics = execution.diagnostics();
                requireIdentityUnchanged(sdk, engineIdentity);
                snapshot = artifactValidator.validate(sourceDirectory);
                writeBuildMarker(sourceDirectory, identity, snapshot.sha256());
                builtNow = true;
            }

            String generationId = requireGenerationId(generationIds.get());
            WebCanvasArtifactPublisher.PublishedArtifact artifact =
                    publisher.publish(snapshot, generationId);
            try {
                return new WebCanvasBuildResult(
                        sourceDirectory,
                        identity,
                        engineIdentity,
                        builtNow,
                        diagnostics,
                        artifact);
            } catch (RuntimeException failure) {
                try {
                    artifact.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        }
    }

    private WebCanvasArtifactContract.ArtifactSnapshot completedSnapshot(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity) throws IOException {
        Path marker = sourceDirectory.resolve(BUILD_MARKER);
        String markerText = readStableBuildMarker(marker);
        if (markerText == null) {
            return null;
        }
        String[] lines = markerText.split("\\n", -1);
        if (lines.length != 3
                || !lines[2].isEmpty()
                || !identity.cacheKey().equals(lines[0])
                || !lines[1].matches("[0-9a-f]{64}")) {
            return null;
        }
        try {
            WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                    artifactValidator.validate(sourceDirectory);
            return lines[1].equals(snapshot.sha256()) ? snapshot : null;
        } catch (IOException invalidCachedArtifact) {
            return null;
        }
    }

    private BuildExecution executeBuild(
            FlutterSdk sdk,
            Path sourceDirectory,
            CanvasEngineIdentity engineIdentity) throws IOException {
        String flutter = sdk.flutterExecutable().toAbsolutePath().normalize().toString();
        BoundedDiagnostics diagnostics = new BoundedDiagnostics(
                MAX_DIAGNOSTIC_CHARACTERS);
        executeCommand(
                "Flutter Web Canvas offline dependency resolution",
                List.of(flutter, "pub", "get", "--offline"),
                sourceDirectory,
                diagnostics);
        sources.verifyMutableBuildPaths(sourceDirectory);
        executeCommand(
                "Flutter Web Canvas build",
                List.of(
                        flutter,
                        "build",
                        "web",
                        "--release",
                        "--target=lib/main_web.dart",
                        "--no-pub",
                        "--no-tree-shake-icons",
                        "--no-web-resources-cdn",
                        "--no-wasm-dry-run",
                        "--pwa-strategy=none",
                        dartDefine("NBFC_FLUTTER_VERSION", engineIdentity.flutterVersion()),
                        dartDefine("NBFC_FRAMEWORK_REVISION",
                                engineIdentity.frameworkRevision()),
                        dartDefine("NBFC_ENGINE_REVISION", engineIdentity.engineRevision()),
                        dartDefine("NBFC_DART_SDK_VERSION", engineIdentity.dartSdkVersion())),
                sourceDirectory,
                diagnostics);
        return new BuildExecution(diagnostics.snapshot());
    }

    private void executeCommand(
            String operation,
            List<String> command,
            Path sourceDirectory,
            BoundedDiagnostics diagnostics) throws IOException {
        Process process = processStarter.start(command, sourceDirectory);
        Thread drainer = Thread.ofVirtual()
                .name("flutter-web-canvas-command-output")
                .start(() -> drain(process, diagnostics));
        boolean finished;
        try {
            finished = process.waitFor(buildTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            IOException interruptedFailure = new IOException(
                    "interrupted during " + operation, exception);
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
            throw new IOException(operation + " exceeded "
                    + buildTimeout.toSeconds() + " seconds\n" + diagnostics.snapshot());
        }
        awaitDrainer(drainer);
        if (diagnostics.readFailure() != null) {
            throw new IOException("cannot read diagnostics during " + operation,
                    diagnostics.readFailure());
        }
        if (process.exitValue() != 0) {
            throw new IOException(operation + " failed with exit code "
                    + process.exitValue() + "\n" + diagnostics.snapshot());
        }
    }

    private void prepareCacheRoot() throws IOException {
        Files.createDirectories(cacheRoot);
        rejectLink(cacheRoot, "Web Canvas build cache root");
        if (!Files.isDirectory(cacheRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Web Canvas build cache root is not a safe directory: " + cacheRoot);
        }
    }

    private static void cleanBuildOutput(Path sourceDirectory) throws IOException {
        Path source = sourceDirectory.toAbsolutePath().normalize();
        for (String generatedName : List.of("build", ".dart_tool")) {
            Path generated = source.resolve(generatedName);
            requireInside(source, generated, "Web Canvas generated cache");
            rejectLink(generated, "Web Canvas generated cache");
            if (Files.exists(generated, LinkOption.NOFOLLOW_LINKS)) {
                deleteSafeTree(
                        generated,
                        source,
                        "Web Canvas generated cache " + generatedName);
            }
        }
        Files.deleteIfExists(source.resolve(BUILD_MARKER));
    }

    private static void deleteSafeTree(
            Path root,
            Path expectedParent,
            String label) throws IOException {
        Path tree = root.toAbsolutePath().normalize();
        Path parent = expectedParent.toAbsolutePath().normalize();
        if (!parent.equals(tree.getParent())) {
            throw new IOException("refusing to delete " + label + " outside its parent");
        }
        int[] visited = {0};
        Files.walkFileTree(tree, new SimpleFileVisitor<>() {
            private void count(Path path) throws IOException {
                if (++visited[0] > MAX_GENERATED_CACHE_PATHS) {
                    throw new IOException(label + " contains more than "
                            + MAX_GENERATED_CACHE_PATHS + " paths: " + path);
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory,
                    BasicFileAttributes attributes) throws IOException {
                count(directory);
                rejectLink(directory, label);
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException(label + " contains an unsafe directory: " + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attributes) throws IOException {
                count(file);
                rejectLink(file, label);
                if (!attributes.isRegularFile() || attributes.isOther()) {
                    throw new IOException(label + " contains an unsafe file: " + file);
                }
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path directory,
                    IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String readStableBuildMarker(Path marker) throws IOException {
        rejectLink(marker, "Web Canvas build marker");
        if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        BasicFileAttributes before = Files.readAttributes(
                marker, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.isOther()
                || before.size() < 1 || before.size() > 256) {
            return null;
        }

        ByteBuffer content = ByteBuffer.allocate(257);
        try (FileChannel channel = FileChannel.open(
                marker, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            while (content.hasRemaining() && channel.read(content) >= 0) {
                // Continue until EOF or the strict upper bound is exceeded.
            }
        }
        if (content.position() != before.size() || content.position() > 256) {
            throw new IOException(
                    "Web Canvas build marker changed while it was being read");
        }

        rejectLink(marker, "Web Canvas build marker");
        BasicFileAttributes after = Files.readAttributes(
                marker, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile()
                || after.isOther()
                || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !sameFileKey(before.fileKey(), after.fileKey())) {
            throw new IOException(
                    "Web Canvas build marker changed while it was being read");
        }
        content.flip();
        byte[] bytes = new byte[content.remaining()];
        content.get(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static void writeBuildMarker(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity,
            String snapshotSha256) throws IOException {
        String digest = requireDigest(snapshotSha256, "Web Canvas artifact digest");
        byte[] bytes = (identity.cacheKey() + "\n" + digest + "\n")
                .getBytes(StandardCharsets.US_ASCII);
        CanvasRunnerRuntimeCache.writeAtomically(
                sourceDirectory.resolve(BUILD_MARKER),
                bytes,
                "Web Canvas build marker");
    }

    private String buildContractFingerprint(CanvasEngineIdentity engineIdentity) {
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        MessageDigest digest = sha256();
        updateFingerprint(digest, "buildProfile", BUILD_PROFILE);
        updateFingerprint(
                digest,
                "artifactContract",
                requireFingerprintValue(artifactValidator.fingerprint()));
        updateFingerprint(digest, "flutterVersion", engineIdentity.flutterVersion());
        updateFingerprint(digest, "frameworkRevision", engineIdentity.frameworkRevision());
        updateFingerprint(digest, "engineRevision", engineIdentity.engineRevision());
        updateFingerprint(digest, "dartSdkVersion", engineIdentity.dartSdkVersion());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void requireIdentityUnchanged(
            FlutterSdk sdk,
            CanvasEngineIdentity expected) throws IOException {
        CanvasEngineIdentity current = WebCanvasSdkIdentity.read(sdk);
        if (!expected.equals(current)) {
            throw new IOException(
                    "Flutter SDK identity changed during the Web Canvas build operation");
        }
    }

    private static String dartDefine(String name, String value) {
        return "--dart-define=" + name + "=" + value;
    }

    private static void updateFingerprint(
            MessageDigest digest,
            String name,
            String value) {
        digest.update(name.getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) 0);
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static String requireFingerprintValue(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 512
                || value.indexOf('\0') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "Web Canvas artifact contract fingerprint is invalid");
        }
        return value;
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
                throw new IllegalStateException("Web Canvas JVM build lock identity changed");
            }
            if (--released.references < 0) {
                throw new IllegalStateException("Web Canvas JVM build lock reference underflow");
            }
            return released.references == 0 ? null : released;
        });
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
        } catch (IOException exception) {
            diagnostics.fail(exception);
        }
    }

    private static void awaitDrainer(Thread drainer) throws IOException {
        boolean interrupted = false;
        try {
            for (int attempt = 0; attempt < 50 && drainer.isAlive(); attempt++) {
                try {
                    drainer.join(100);
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
            if (drainer.isAlive()) {
                drainer.interrupt();
                throw new IOException(
                        "Flutter Web Canvas diagnostics did not close after build exit");
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

    private static Path requireInside(Path root, Path candidate, String label)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new IOException(label + " escaped its cache directory");
        }
        return normalizedCandidate;
    }

    private static void requireDirectChild(
            Path parent,
            Path candidate,
            String label) throws IOException {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedParent.equals(normalizedCandidate.getParent())) {
            throw new IOException(label + " must be a direct cache-root child");
        }
    }

    private static void rejectLink(Path path, String label) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path)) {
            return;
        }
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attributes.isSymbolicLink()
                || attributes.isOther()
                || Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a link or reparse point: " + path);
        }
    }

    private static boolean sameFileKey(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static Duration requirePositive(Duration value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    private static String requireGenerationId(String value) {
        return requireDigest(value, "Web Canvas publication generation");
    }

    private static String requireDigest(String value, String label) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " must be a lowercase SHA-256 value");
        }
        return value;
    }

    private static String randomGenerationId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java platform", exception);
        }
    }

    interface ArtifactValidator {
        String fingerprint();

        WebCanvasArtifactContract.ArtifactSnapshot validate(Path sourceDirectory)
                throws IOException;

        static ArtifactValidator production(WebCanvasArtifactContract contract) {
            Objects.requireNonNull(contract, "contract");
            return new ArtifactValidator() {
                @Override
                public String fingerprint() {
                    return contract.fingerprint();
                }

                @Override
                public WebCanvasArtifactContract.ArtifactSnapshot validate(
                        Path sourceDirectory) throws IOException {
                    return contract.validate(sourceDirectory);
                }
            };
        }
    }

    private static final class BuildLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int references;
    }

    private static final class CancellableBuildFuture
            extends CompletableFuture<WebCanvasBuildResult> {
        private final AtomicReference<Thread> worker = new AtomicReference<>();

        boolean beginWorker() {
            Thread current = Thread.currentThread();
            if (!worker.compareAndSet(null, current)) {
                throw new IllegalStateException(
                        "Flutter Web Canvas async build worker was already assigned");
            }
            if (isCancelled()) {
                worker.compareAndSet(current, null);
                return false;
            }
            return true;
        }

        void endWorker() {
            worker.compareAndSet(Thread.currentThread(), null);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled && mayInterruptIfRunning) {
                Thread active = worker.get();
                if (active != null) {
                    active.interrupt();
                }
            }
            return cancelled;
        }
    }

    private static final class BoundedDiagnostics {
        private final int maximum;
        private final StringBuilder value = new StringBuilder();
        private IOException readFailure;

        private BoundedDiagnostics(int maximum) {
            this.maximum = maximum;
        }

        synchronized void append(char[] characters, int count) {
            value.append(characters, 0, count);
            if (value.length() > maximum) {
                value.delete(0, value.length() - maximum);
            }
        }

        synchronized void fail(IOException failure) {
            readFailure = failure;
        }

        synchronized String snapshot() {
            return value.toString();
        }

        synchronized IOException readFailure() {
            return readFailure;
        }
    }

    private record BuildExecution(String diagnostics) {
    }
}
