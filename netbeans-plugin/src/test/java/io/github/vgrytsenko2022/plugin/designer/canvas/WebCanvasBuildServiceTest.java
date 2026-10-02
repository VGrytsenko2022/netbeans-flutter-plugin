package io.github.vgrytsenko2022.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebCanvasBuildServiceTest {
    @TempDir
    Path temporary;

    @Test
    void buildsValidatesPublishesAndReusesExactSdkArtifact() throws Exception {
        CanvasEngineIdentity expectedIdentity = identity("engine-one");
        FlutterSdk sdk = sdk("engine-one", expectedIdentity);
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    starts.incrementAndGet();
                    if ("pub".equals(command.get(1))) {
                        assertEquals(List.of(
                                sdk.flutterExecutable().toString(),
                                "pub", "get", "--offline"), command);
                        return new TestProcess(0, "pub get ok\n", true);
                    }
                    assertEquals(List.of(
                            sdk.flutterExecutable().toString(),
                            "build",
                            "web",
                            "--release",
                            "--target=lib/main_web.dart",
                            "--no-pub",
                            "--no-tree-shake-icons",
                            "--no-web-resources-cdn",
                            "--no-wasm-dry-run",
                            "--pwa-strategy=none",
                            "--dart-define=NBFC_FLUTTER_VERSION="
                                    + expectedIdentity.flutterVersion(),
                            "--dart-define=NBFC_FRAMEWORK_REVISION="
                                    + expectedIdentity.frameworkRevision(),
                            "--dart-define=NBFC_ENGINE_REVISION="
                                    + expectedIdentity.engineRevision(),
                            "--dart-define=NBFC_DART_SDK_VERSION="
                                    + expectedIdentity.dartSdkVersion()), command);
                    writeArtifact(workingDirectory, "first-build");
                    return new TestProcess(0, "web build ok\n", true);
                },
                Duration.ofSeconds(1));

        Path firstPublication;
        Path sourceDirectory;
        try (WebCanvasBuildResult first = service.buildAsync(sdk).get()) {
            assertTrue(first.builtNow());
            assertEquals(expectedIdentity, first.engineIdentity());
            assertEquals("pub get ok\nweb build ok\n", first.diagnostics());
            assertEquals("first-build", Files.readString(
                    first.artifact().root().resolve("index.html")));
            firstPublication = first.artifact().root();
            sourceDirectory = first.sourceDirectory();
            assertFalse(firstPublication.startsWith(sourceDirectory));
            assertFalse(sourceDirectory.startsWith(firstPublication));
        }
        assertFalse(Files.exists(firstPublication));

        try (WebCanvasBuildResult reused = service.build(sdk)) {
            assertFalse(reused.builtNow());
            assertEquals("", reused.diagnostics());
            assertEquals(sourceDirectory, reused.sourceDirectory());
            assertNotEquals(firstPublication, reused.artifact().root());
            assertEquals("first-build", Files.readString(
                    reused.artifact().root().resolve("index.html")));
        }
        assertEquals(2, starts.get());
    }

    @Test
    void cancelledAsyncBuildClosesAnEventuallyPublishedGeneration()
            throws Exception {
        CountDownLatch generationRequested = new CountDownLatch(1);
        CountDownLatch allowPublication = new CountDownLatch(1);
        Path publication = publicationRoot();
        Files.createDirectories(publication.getParent());

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            WebCanvasBuildService service = new WebCanvasBuildService(
                    temporary.resolve("cancelled-publication-build-cache"),
                    bundle(),
                    new TestArtifactValidator(),
                    new WebCanvasArtifactPublisher(publication),
                    (command, workingDirectory) -> {
                        if ("build".equals(command.get(1))) {
                            writeArtifact(workingDirectory, "cancelled-publication");
                        }
                        return new TestProcess(0, "ok\n", true);
                    },
                    Duration.ofSeconds(5),
                    executor,
                    () -> {
                        generationRequested.countDown();
                        try {
                            allowPublication.await();
                        } catch (InterruptedException failure) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(failure);
                        }
                        return sha256("cancelled-publication-generation");
                    });

            CompletableFuture<WebCanvasBuildResult> future = service.buildAsync(
                    sdk("cancelled-publication-sdk"));
            assertTrue(generationRequested.await(5, TimeUnit.SECONDS));
            assertTrue(future.cancel(false));
            allowPublication.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(future.isCancelled());
        } finally {
            allowPublication.countDown();
        }

        try (var children = Files.list(publication)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void interruptingAsyncCancellationRetiresTheActiveFlutterProcess()
            throws Exception {
        CountDownLatch processStarted = new CountDownLatch(1);
        CancellableProcess blocked = new CancellableProcess();
        Path publication = publicationRoot();
        Files.createDirectories(publication.getParent());

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            WebCanvasBuildService service = new WebCanvasBuildService(
                    temporary.resolve("cancelled-process-build-cache"),
                    bundle(),
                    new TestArtifactValidator(),
                    new WebCanvasArtifactPublisher(publication),
                    (_command, _workingDirectory) -> {
                        processStarted.countDown();
                        return blocked;
                    },
                    Duration.ofMinutes(1),
                    executor,
                    () -> sha256("cancelled-process-generation"));

            CompletableFuture<WebCanvasBuildResult> future = service.buildAsync(
                    sdk("cancelled-process-sdk"));
            assertTrue(processStarted.await(5, TimeUnit.SECONDS));
            assertTrue(future.cancel(true));
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(future.isCancelled());
        }

        assertTrue(blocked.destroyed);
        try (var children = Files.list(publication)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void tamperedCachedBuildIsCleanedAndRebuiltBeforePublication() throws Exception {
        AtomicInteger processStarts = new AtomicInteger();
        AtomicInteger builds = new AtomicInteger();
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    processStarts.incrementAndGet();
                    if ("pub".equals(command.get(1))) {
                        if (builds.get() == 1) {
                            assertFalse(Files.exists(workingDirectory.resolve(
                                    ".dart_tool/flutter_build/stale.intermediate")));
                        }
                        return new TestProcess(0, "pub ok\n", true);
                    }
                    int build = builds.incrementAndGet();
                    writeArtifact(workingDirectory, "build-" + build);
                    return new TestProcess(0, "rebuilt\n", true);
                },
                Duration.ofSeconds(1));

        Path sourceDirectory;
        try (WebCanvasBuildResult first = service.build(sdk("engine-one"))) {
            sourceDirectory = first.sourceDirectory();
        }
        Files.writeString(
                sourceDirectory.resolve("build/web/index.html"),
                "tampered",
                StandardCharsets.UTF_8);
        Files.writeString(
                sourceDirectory.resolve("build/web/unexpected.txt"),
                "stale",
                StandardCharsets.UTF_8);
        Path staleIntermediate = sourceDirectory.resolve(
                ".dart_tool/flutter_build/stale.intermediate");
        Files.createDirectories(staleIntermediate.getParent());
        Files.writeString(staleIntermediate, "stale", StandardCharsets.UTF_8);

        try (WebCanvasBuildResult rebuilt = service.build(sdk("engine-one"))) {
            assertTrue(rebuilt.builtNow());
            assertEquals("build-2", Files.readString(
                    rebuilt.artifact().root().resolve("index.html")));
            assertFalse(Files.exists(
                    rebuilt.artifact().root().resolve("unexpected.txt")));
        }
        assertEquals(4, processStarts.get());
        assertEquals(2, builds.get());
    }

    @Test
    void concurrentCallsSerializeOneBuildButPublishIndependentGenerations()
            throws Exception {
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    starts.incrementAndGet();
                    if ("build".equals(command.get(1))) {
                        writeArtifact(workingDirectory, "concurrent");
                    }
                    return new TestProcess(0, command.get(1) + " ok\n", true);
                },
                Duration.ofSeconds(2));
        FlutterSdk sdk = sdk("engine-one");

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<WebCanvasBuildResult> left = pool.submit(() -> service.build(sdk));
            Future<WebCanvasBuildResult> right = pool.submit(() -> service.build(sdk));
            try (WebCanvasBuildResult first = left.get(5, TimeUnit.SECONDS);
                    WebCanvasBuildResult second = right.get(5, TimeUnit.SECONDS)) {
                assertNotEquals(first.artifact().root(), second.artifact().root());
                assertEquals(first.sourceDirectory(), second.sourceDirectory());
                assertTrue(first.builtNow() ^ second.builtNow());
            }
        }
        assertEquals(2, starts.get());
    }

    @Test
    void failedBuildRetainsOnlyBoundedDiagnosticTailAndPublishesNothing()
            throws Exception {
        String output = "x".repeat(
                WebCanvasBuildService.MAX_DIAGNOSTIC_CHARACTERS + 4096)
                + "FINAL-WEB-DIAGNOSTIC";
        WebCanvasBuildService service = service(
                (command, _workingDirectory) -> "pub".equals(command.get(1))
                        ? new TestProcess(0, "pub ok\n", true)
                        : new TestProcess(23, output, true),
                Duration.ofSeconds(1));

        IOException failure = assertThrows(
                IOException.class,
                () -> service.build(sdk("engine-one")));

        assertTrue(failure.getMessage().contains("build failed with exit code 23"));
        assertTrue(failure.getMessage().endsWith("FINAL-WEB-DIAGNOSTIC"));
        assertTrue(failure.getMessage().length()
                <= WebCanvasBuildService.MAX_DIAGNOSTIC_CHARACTERS + 128);
        try (var children = Files.list(publicationRoot())) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void timeoutTerminatesBuildAndDoesNotCreateReusableMarker() throws Exception {
        TestProcess blocked = new TestProcess(0, "still building", false);
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    starts.incrementAndGet();
                    if (starts.get() == 1) {
                        return blocked;
                    }
                    if ("build".equals(command.get(1))) {
                        writeArtifact(workingDirectory, "recovered");
                    }
                    return new TestProcess(0, "ok\n", true);
                },
                Duration.ofMillis(1));

        IOException timeout = assertThrows(
                IOException.class,
                () -> service.build(sdk("engine-one")));
        assertTrue(timeout.getMessage().contains("offline dependency resolution"));
        assertTrue(timeout.getMessage().contains("exceeded"));
        assertTrue(blocked.destroyed);

        try (WebCanvasBuildResult recovered = service.build(sdk("engine-one"))) {
            assertTrue(recovered.builtNow());
            assertEquals("recovered", Files.readString(
                    recovered.artifact().root().resolve("index.html")));
        }
        assertEquals(3, starts.get());
    }

    @Test
    void invalidGenerationIdFailsBeforePublishingMutableArtifact()
            throws Exception {
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    if ("build".equals(command.get(1))) {
                        writeArtifact(workingDirectory, "built");
                    }
                    return new TestProcess(0, "ok\n", true);
                },
                Duration.ofSeconds(1),
                () -> "not-a-generation");

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> service.build(sdk("engine-one")));

        assertTrue(failure.getMessage().contains("publication generation"));
        try (var children = Files.list(publicationRoot())) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void sdkIdentityMismatchFailsBeforeStartingAnyFlutterProcess() throws Exception {
        CanvasEngineIdentity expected = identity("mismatched-sdk");
        FlutterSdk sdk = sdk("mismatched-sdk", expected);
        Files.writeString(
                sdk.home().resolve("bin/internal/engine.version"),
                "f".repeat(40) + "\n",
                StandardCharsets.UTF_8);
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (_command, _workingDirectory) -> {
                    starts.incrementAndGet();
                    return new TestProcess(0, "must not start", true);
                },
                Duration.ofSeconds(1));

        IOException failure = assertThrows(IOException.class, () -> service.build(sdk));

        assertTrue(failure.getMessage().contains("engineRevision"));
        assertTrue(failure.getMessage().contains("engine.version"));
        assertEquals(0, starts.get());
    }

    @Test
    void sdkRootVersionMismatchFailsBeforeStartingAnyFlutterProcess() throws Exception {
        CanvasEngineIdentity expected = identity("root-version-mismatch");
        FlutterSdk sdk = sdk("root-version-mismatch", expected);
        Files.writeString(
                sdk.home().resolve("version"),
                "different-version\n",
                StandardCharsets.UTF_8);
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (_command, _workingDirectory) -> {
                    starts.incrementAndGet();
                    return new TestProcess(0, "must not start", true);
                },
                Duration.ofSeconds(1));

        IOException failure = assertThrows(IOException.class, () -> service.build(sdk));

        assertTrue(failure.getMessage().contains("flutterVersion"));
        assertTrue(failure.getMessage().contains("root version"));
        assertEquals(0, starts.get());
    }

    @Test
    void duplicateSdkIdentityFieldIsRejectedBeforeStartingAnyFlutterProcess()
            throws Exception {
        CanvasEngineIdentity expected = identity("duplicate-metadata");
        FlutterSdk sdk = sdk("duplicate-metadata", expected);
        Path metadata = sdk.home().resolve("bin/cache/flutter.version.json");
        String duplicate = Files.readString(metadata, StandardCharsets.UTF_8)
                .replace(
                        "\"flutterVersion\": \"" + expected.flutterVersion() + "\"",
                        "\"flutterVersion\": \"" + expected.flutterVersion() + "\",\n"
                        + "  \"flutterVersion\": \"" + expected.flutterVersion() + "\"");
        Files.writeString(metadata, duplicate, StandardCharsets.UTF_8);
        AtomicInteger starts = new AtomicInteger();
        WebCanvasBuildService service = service(
                (_command, _workingDirectory) -> {
                    starts.incrementAndGet();
                    return new TestProcess(0, "must not start", true);
                },
                Duration.ofSeconds(1));

        IOException failure = assertThrows(IOException.class, () -> service.build(sdk));

        assertTrue(failure.getMessage().contains("strict bounded JSON"));
        assertEquals(0, starts.get());
    }

    @Test
    void trailingSdkIdentityJsonIsRejected() throws Exception {
        CanvasEngineIdentity expected = identity("trailing-metadata");
        FlutterSdk sdk = sdk("trailing-metadata", expected);
        Path metadata = sdk.home().resolve("bin/cache/flutter.version.json");
        Files.writeString(
                metadata,
                Files.readString(metadata, StandardCharsets.UTF_8) + "\n{}\n",
                StandardCharsets.UTF_8);

        IOException failure = assertThrows(
                IOException.class,
                () -> WebCanvasSdkIdentity.read(sdk));

        assertTrue(failure.getMessage().contains("strict bounded JSON"));
    }

    @Test
    void nonLowercaseSdkRevisionIsRejected() throws Exception {
        CanvasEngineIdentity expected = identity("uppercase-revision");
        FlutterSdk sdk = sdk("uppercase-revision", expected);
        Path metadata = sdk.home().resolve("bin/cache/flutter.version.json");
        String invalid = Files.readString(metadata, StandardCharsets.UTF_8)
                .replace(expected.frameworkRevision(), "A".repeat(40));
        Files.writeString(metadata, invalid, StandardCharsets.UTF_8);

        IOException failure = assertThrows(
                IOException.class,
                () -> WebCanvasSdkIdentity.read(sdk));

        assertTrue(failure.getMessage().contains("frameworkRevision"));
        assertTrue(failure.getMessage().contains("lowercase 40-digit commit"));
    }

    @Test
    void linkedSdkIdentityMetadataIsRejectedWhenLinksAreAvailable() throws Exception {
        CanvasEngineIdentity expected = identity("linked-metadata");
        FlutterSdk sdk = sdk("linked-metadata", expected);
        Path metadata = sdk.home().resolve("bin/cache/flutter.version.json");
        Path external = temporary.resolve("external-flutter.version.json");
        Files.copy(metadata, external);
        Files.delete(metadata);
        try {
            Files.createSymbolicLink(metadata, external);
        } catch (UnsupportedOperationException | IOException | SecurityException failure) {
            Assumptions.assumeTrue(false,
                    "symbolic links are unavailable: " + failure.getMessage());
        }

        IOException failure = assertThrows(
                IOException.class,
                () -> WebCanvasSdkIdentity.read(sdk));

        assertTrue(failure.getMessage().contains("symbolic link")
                || failure.getMessage().contains("reparse point"));
    }

    @Test
    void sdkRootVersionFileIsOptionalForCurrentFlutterDistributions() throws Exception {
        CanvasEngineIdentity expected = identity("no-root-version");
        FlutterSdk sdk = sdk("no-root-version", expected);
        Files.delete(sdk.home().resolve("version"));

        assertEquals(expected, WebCanvasSdkIdentity.read(sdk));
    }

    @Test
    void nonEngineSdkIdentityChangeUsesADistinctBuildCache() throws Exception {
        CanvasEngineIdentity firstIdentity = identity("changing-sdk");
        CanvasEngineIdentity secondIdentity = new CanvasEngineIdentity(
                firstIdentity.flutterVersion(),
                firstIdentity.frameworkRevision(),
                firstIdentity.engineRevision(),
                "3.test.2");
        FlutterSdk sdk = sdk("changing-sdk", firstIdentity);
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger builds = new AtomicInteger();
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    starts.incrementAndGet();
                    if ("build".equals(command.get(1))) {
                        writeArtifact(
                                workingDirectory,
                                "identity-build-" + builds.incrementAndGet());
                    }
                    return new TestProcess(0, "ok\n", true);
                },
                Duration.ofSeconds(1));

        Path firstSource;
        String firstCacheKey;
        try (WebCanvasBuildResult first = service.build(sdk)) {
            firstSource = first.sourceDirectory();
            firstCacheKey = first.identity().cacheKey();
            assertEquals(firstIdentity, first.engineIdentity());
        }

        writeSdkIdentity(sdk, secondIdentity);
        try (WebCanvasBuildResult second = service.build(sdk)) {
            assertTrue(second.builtNow());
            assertEquals(secondIdentity, second.engineIdentity());
            assertNotEquals(firstSource, second.sourceDirectory());
            assertNotEquals(firstCacheKey, second.identity().cacheKey());
        }
        assertEquals(4, starts.get());
        assertEquals(2, builds.get());
    }

    @Test
    void failedBuildResultCloseCanRetryPublicationCleanup() throws Exception {
        AtomicInteger deletionAttempts = new AtomicInteger();
        Files.createDirectories(publicationRoot().getParent());
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                publicationRoot(),
                _generation -> {
                    if (deletionAttempts.incrementAndGet() == 1) {
                        throw new IOException("transient publication cleanup failure");
                    }
                });
        WebCanvasBuildService service = service(
                (command, workingDirectory) -> {
                    if ("build".equals(command.get(1))) {
                        writeArtifact(workingDirectory, "retry-close");
                    }
                    return new TestProcess(0, "ok\n", true);
                },
                Duration.ofSeconds(1),
                () -> sha256("retry-close-generation"),
                publisher);

        WebCanvasBuildResult result = service.build(sdk("retry-close-sdk"));
        Path publication = result.artifact().root();

        IOException failure = assertThrows(IOException.class, result::close);
        assertTrue(failure.getMessage().contains("transient publication cleanup failure"));
        assertTrue(Files.exists(publication));

        result.close();
        assertFalse(Files.exists(publication));
        assertEquals(2, deletionAttempts.get());
        assertThrows(IllegalStateException.class, result::artifact);
    }

    @Test
    void buildsPackagedWebRunnerWithConfiguredFlutterSdkWhenRequested()
            throws Exception {
        String configuredHome = System.getProperty("web.canvas.flutter.sdk", "");
        Assumptions.assumeFalse(configuredHome.isBlank(),
                "set -Dweb.canvas.flutter.sdk to run the exact Web build smoke test");
        Path sdkHome = Path.of(configuredHome).toAbsolutePath().normalize();
        Path flutter = sdkHome.resolve("bin").resolve(
                System.getProperty("os.name", "").startsWith("Windows")
                        ? "flutter.bat"
                        : "flutter");
        Path publicationParent = temporary.resolve("real-publication-parent");
        Files.createDirectories(publicationParent);
        AtomicInteger publicationGenerations = new AtomicInteger();
        WebCanvasBuildService service = new WebCanvasBuildService(
                temporary.resolve("real-build-cache"),
                CanvasRunnerSourceBundle.packaged(),
                WebCanvasBuildService.ArtifactValidator.production(
                        new WebCanvasArtifactContract()),
                new WebCanvasArtifactPublisher(
                        publicationParent.resolve("published")),
                CanvasRunnerBuildService.CanvasRunnerProcessStarter.system(),
                Duration.ofMinutes(5),
                Runnable::run,
                () -> sha256("real-web-generation-"
                        + publicationGenerations.incrementAndGet()));

        Path sourceDirectory;
        Path firstPublication;
        try (WebCanvasBuildResult result = service.build(
                new FlutterSdk(sdkHome, flutter))) {
            assertTrue(result.builtNow());
            assertTrue(Files.isRegularFile(
                    result.artifact().root().resolve("main.dart.js")));
            assertEquals(WebCanvasSdkIdentity.read(
                    new FlutterSdk(sdkHome, flutter)), result.engineIdentity());
            sourceDirectory = result.sourceDirectory();
            firstPublication = result.artifact().root();
        }
        assertFalse(Files.exists(firstPublication));

        try (WebCanvasBuildResult cached = service.build(
                new FlutterSdk(sdkHome, flutter))) {
            assertFalse(cached.builtNow());
            assertEquals(sourceDirectory, cached.sourceDirectory());
            assertNotEquals(firstPublication, cached.artifact().root());
            assertTrue(Files.isRegularFile(
                    cached.artifact().root().resolve("main.dart.js")));
        }
    }

    private WebCanvasBuildService service(
            CanvasRunnerBuildService.CanvasRunnerProcessStarter starter,
            Duration timeout) throws IOException {
        AtomicInteger generations = new AtomicInteger();
        return service(starter, timeout,
                () -> sha256("generation-" + generations.incrementAndGet()));
    }

    private WebCanvasBuildService service(
            CanvasRunnerBuildService.CanvasRunnerProcessStarter starter,
            Duration timeout,
            Supplier<String> generationIds) throws IOException {
        Files.createDirectories(publicationRoot().getParent());
        return service(
                starter,
                timeout,
                generationIds,
                new WebCanvasArtifactPublisher(publicationRoot()));
    }

    private WebCanvasBuildService service(
            CanvasRunnerBuildService.CanvasRunnerProcessStarter starter,
            Duration timeout,
            Supplier<String> generationIds,
            WebCanvasArtifactPublisher publisher) throws IOException {
        Files.createDirectories(publicationRoot().getParent());
        return new WebCanvasBuildService(
                temporary.resolve("build-cache"),
                bundle(),
                new TestArtifactValidator(),
                publisher,
                starter,
                timeout,
                Runnable::run,
                generationIds);
    }

    private Path publicationRoot() {
        return temporary.resolve("publication-parent/published");
    }

    private FlutterSdk sdk(String engineRevision) throws IOException {
        return sdk(engineRevision, identity(engineRevision));
    }

    private FlutterSdk sdk(String name, CanvasEngineIdentity identity) throws IOException {
        Path home = temporary.resolve("flutter-sdk-" + name);
        Path executable = home.resolve("bin/flutter.bat");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable, "@echo off\n");
        FlutterSdk sdk = new FlutterSdk(home, executable);
        writeSdkIdentity(sdk, identity);
        return sdk;
    }

    private static void writeSdkIdentity(
            FlutterSdk sdk,
            CanvasEngineIdentity identity) throws IOException {
        Path engine = sdk.home().resolve("bin/internal/engine.version");
        Files.createDirectories(engine.getParent());
        Files.writeString(
                engine,
                identity.engineRevision() + "\n",
                StandardCharsets.UTF_8);
        Path metadata = sdk.home().resolve("bin/cache/flutter.version.json");
        Files.createDirectories(metadata.getParent());
        Files.writeString(
                metadata,
                """
                {
                  "frameworkVersion": "%s",
                  "channel": "test",
                  "frameworkRevision": "%s",
                  "engineRevision": "%s",
                  "dartSdkVersion": "%s",
                  "flutterVersion": "%s"
                }
                """.formatted(
                        identity.flutterVersion(),
                        identity.frameworkRevision(),
                        identity.engineRevision(),
                        identity.dartSdkVersion(),
                        identity.flutterVersion()),
                StandardCharsets.UTF_8);
        Files.writeString(
                sdk.home().resolve("version"),
                identity.flutterVersion() + "\n",
                StandardCharsets.UTF_8);
    }

    private static CanvasEngineIdentity identity(String seed) {
        return new CanvasEngineIdentity(
                "3.test",
                sha256("framework-" + seed).substring(0, 40),
                sha256("engine-" + seed).substring(0, 40),
                "3.test.1");
    }

    private static CanvasRunnerSourceBundle bundle() throws IOException {
        Map<String, String> sourceFiles = Map.of(
                "lib/main_web.dart", "void main() {}\n",
                "pubspec.yaml", "name: netbeans_flutter_canvas_runner\n",
                "web/index.html", "trusted\n");
        StringBuilder manifest = new StringBuilder();
        sourceFiles.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                    manifest.append(entry.getKey())
                            .append('|')
                            .append(bytes.length)
                            .append('|')
                            .append(sha256(bytes))
                            .append('\n');
                });
        return new CanvasRunnerSourceBundle(
                new CanvasRunnerSourceBundle.ResourceAccess() {
                    @Override
                    public InputStream openManifest() {
                        return new ByteArrayInputStream(manifest.toString()
                                .getBytes(StandardCharsets.US_ASCII));
                    }

                    @Override
                    public InputStream openSource(String relativePath) {
                        return new ByteArrayInputStream(sourceFiles.get(relativePath)
                                .getBytes(StandardCharsets.UTF_8));
                    }
                },
                "test-web");
    }

    private static void writeArtifact(Path sourceDirectory, String index)
            throws IOException {
        Path root = sourceDirectory.resolve("build/web");
        Files.createDirectories(root);
        Files.writeString(root.resolve("index.html"), index, StandardCharsets.UTF_8);
        Files.writeString(root.resolve(".last_build_id"), "metadata",
                StandardCharsets.UTF_8);
    }

    private static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class TestArtifactValidator
            implements WebCanvasBuildService.ArtifactValidator {
        @Override
        public String fingerprint() {
            return sha256("test-web-artifact-contract");
        }

        @Override
        public WebCanvasArtifactContract.ArtifactSnapshot validate(
                Path sourceDirectory) throws IOException {
            Path root = sourceDirectory.resolve("build/web")
                    .toAbsolutePath().normalize();
            Path index = root.resolve("index.html");
            Path metadata = root.resolve(".last_build_id");
            byte[] indexBytes = Files.readAllBytes(index);
            byte[] metadataBytes = Files.readAllBytes(metadata);
            WebCanvasArtifactContract.ArtifactFile indexFile =
                    new WebCanvasArtifactContract.ArtifactFile(
                            "index.html", indexBytes.length, sha256(indexBytes));
            WebCanvasArtifactContract.ArtifactFile metadataFile =
                    new WebCanvasArtifactContract.ArtifactFile(
                            ".last_build_id", metadataBytes.length,
                            sha256(metadataBytes));
            return new WebCanvasArtifactContract.ArtifactSnapshot(
                    root,
                    Map.of("index.html", indexFile),
                    Map.of(".last_build_id", metadataFile),
                    indexBytes.length + metadataBytes.length,
                    sha256(indexFile.sha256() + "\n" + metadataFile.sha256()));
        }
    }

    private static final class TestProcess extends Process {
        private final InputStream output;
        private final int exitCode;
        private final boolean finishes;
        private final ByteArrayOutputStream input = new ByteArrayOutputStream();
        private volatile boolean destroyed;

        private TestProcess(int exitCode, String output, boolean finishes) {
            this.exitCode = exitCode;
            this.output = new ByteArrayInputStream(
                    output.getBytes(StandardCharsets.UTF_8));
            this.finishes = finishes;
        }

        @Override
        public OutputStream getOutputStream() {
            return input;
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
        public int waitFor() throws InterruptedException {
            if (!finishes && !destroyed) {
                Thread.sleep(Long.MAX_VALUE);
            }
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return finishes || destroyed;
        }

        @Override
        public int exitValue() {
            if (!finishes && !destroyed) {
                throw new IllegalThreadStateException();
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyed = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return !finishes && !destroyed;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }
    }

    private static final class CancellableProcess extends Process {
        private final ByteArrayOutputStream input = new ByteArrayOutputStream();
        private volatile boolean destroyed;

        @Override
        public OutputStream getOutputStream() {
            return input;
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            while (!destroyed) {
                Thread.sleep(1_000);
            }
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit)
                throws InterruptedException {
            while (!destroyed) {
                Thread.sleep(Math.min(1_000, Math.max(1,
                        unit.toMillis(timeout))));
            }
            return true;
        }

        @Override
        public int exitValue() {
            if (!destroyed) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyed = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return !destroyed;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }
    }
}
