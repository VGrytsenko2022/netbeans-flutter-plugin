package io.github.vgrytsenko2022.plugin.designer.canvas;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebCanvasArtifactPublisherTest {
    private static final String GENERATION = "1".repeat(64);

    @TempDir
    Path temporary;

    @Test
    void stableFileIdentityRejectsOneSidedUnavailableKeys() {
        Object key = new Object();

        assertTrue(WebCanvasArtifactPublisher.sameFileKey(null, null));
        assertTrue(WebCanvasArtifactPublisher.sameFileKey(key, key));
        assertFalse(WebCanvasArtifactPublisher.sameFileKey(null, key));
        assertFalse(WebCanvasArtifactPublisher.sameFileKey(key, null));
        assertFalse(WebCanvasArtifactPublisher.sameFileKey(key, new Object()));
    }

    @Test
    void publishesOnlyServeableSnapshotFilesAndCleansOwnedGeneration() throws Exception {
        Path source = artifactSource();
        WebCanvasArtifactContract.ArtifactSnapshot snapshot = snapshot(source);
        Path owned = temporary.resolve("published");
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(owned);

        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot, GENERATION);

        assertEquals("generation-" + GENERATION, artifact.root().getFileName().toString());
        assertEquals(GENERATION, artifact.generationId());
        assertEquals(snapshot.sha256(), artifact.sourceSnapshotSha256());
        assertEquals(snapshot.files(), artifact.files());
        assertEquals("<html>canvas</html>", Files.readString(
                artifact.root().resolve("index.html"), StandardCharsets.UTF_8));
        assertEquals("license", Files.readString(
                artifact.root().resolve("assets/NOTICES"), StandardCharsets.UTF_8));
        assertFalse(Files.exists(artifact.root().resolve(".last_build_id")));

        Path generationRoot = artifact.root();
        artifact.close();
        artifact.close();
        assertFalse(Files.exists(generationRoot));
        assertTrue(Files.isDirectory(owned));
    }

    @Test
    void publishedTreeRejectsAnUnexpectedEmptyDirectory() throws Exception {
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);
        Path unexpected = Files.createDirectory(
                artifact.root().resolve("unexpected-empty"));

        IOException failure = assertThrows(IOException.class, () ->
                WebCanvasArtifactPublisher.verifyPublishedTree(
                        artifact.root(), artifact.files()));

        assertTrue(failure.getMessage().contains("unexpected directory"));
        Files.delete(unexpected);
        artifact.close();
    }

    @Test
    void cleanupRejectsExcessiveDepthAndRemainsRetryable() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<Path> deepest = new AtomicReference<>();
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"),
                generation -> {
                    if (attempts.incrementAndGet() != 1) {
                        return;
                    }
                    Path current = generation;
                    for (int depth = 0;
                            depth <= WebCanvasArtifactPublisher.MAX_PUBLICATION_DEPTH;
                            depth++) {
                        current = Files.createDirectory(
                                current.resolve("depth-" + depth));
                    }
                    deepest.set(current);
                });
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);

        IOException failure = assertThrows(IOException.class, artifact::close);

        assertTrue(failure.getMessage().contains("maximum depth"));
        Path current = deepest.get();
        while (current != null && !current.equals(artifact.root())) {
            Files.deleteIfExists(current);
            current = current.getParent();
        }
        artifact.close();
        assertFalse(Files.exists(artifact.root()));
        assertEquals(2, attempts.get());
    }

    @Test
    void cleanupRejectsExcessivePathCountAndRemainsRetryable()
            throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<Path> overflowDirectory = new AtomicReference<>();
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"),
                generation -> {
                    if (attempts.incrementAndGet() != 1) {
                        return;
                    }
                    Path overflow = Files.createDirectory(
                            generation.resolve("overflow"));
                    overflowDirectory.set(overflow);
                    for (int index = 0;
                            index < WebCanvasArtifactPublisher.MAX_PUBLICATION_PATHS;
                            index++) {
                        Files.createFile(overflow.resolve("path-" + index));
                    }
                });
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);

        IOException failure = assertThrows(IOException.class, artifact::close);

        assertTrue(failure.getMessage().contains("contains more than"));
        Path overflow = overflowDirectory.get();
        if (Files.exists(overflow)) {
            try (var children = Files.list(overflow)) {
                for (Path child : children.toList()) {
                    Files.deleteIfExists(child);
                }
            }
            Files.deleteIfExists(overflow);
        }
        artifact.close();
        assertFalse(Files.exists(artifact.root()));
        assertEquals(2, attempts.get());
    }

    @Test
    void activeLeaseDefersOwnerCloseAndRejectsNewConsumers() throws Exception {
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);
        WebCanvasArtifactPublisher.PublishedArtifact.Lease lease =
                artifact.acquireLease();
        Path generationRoot = artifact.root();

        artifact.close();

        assertTrue(Files.isDirectory(generationRoot));
        assertThrows(IllegalStateException.class, artifact::acquireLease);
        lease.close();
        lease.close();
        assertFalse(Files.exists(generationRoot));
        artifact.close();
    }

    @Test
    void ownerCloseAndFinalLeaseReleaseAreRaceSafe() throws Exception {
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);
        WebCanvasArtifactPublisher.PublishedArtifact.Lease lease =
                artifact.acquireLease();
        Path generationRoot = artifact.root();
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> ownerFailure = new AtomicReference<>();
        AtomicReference<Throwable> leaseFailure = new AtomicReference<>();
        Thread owner = Thread.ofVirtual().start(() -> {
            await(start);
            try {
                artifact.close();
            } catch (Throwable failure) {
                ownerFailure.set(failure);
            }
        });
        Thread consumer = Thread.ofVirtual().start(() -> {
            await(start);
            try {
                lease.close();
            } catch (Throwable failure) {
                leaseFailure.set(failure);
            }
        });

        start.countDown();
        owner.join();
        consumer.join();

        assertNull(ownerFailure.get());
        assertNull(leaseFailure.get());
        assertFalse(Files.exists(generationRoot));
        assertThrows(IllegalStateException.class, artifact::acquireLease);
    }

    @Test
    void finalLeaseReleaseRetriesTransientDeletionAndRemainsIdempotent()
            throws Exception {
        AtomicInteger deletionAttempts = new AtomicInteger();
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"),
                ignored -> {
                    if (deletionAttempts.incrementAndGet() == 1) {
                        throw new IOException("transient deletion failure");
                    }
                });
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(artifactSource()), GENERATION);
        WebCanvasArtifactPublisher.PublishedArtifact.Lease lease =
                artifact.acquireLease();
        Path generationRoot = artifact.root();
        artifact.close();

        IOException firstFailure = assertThrows(IOException.class, lease::close);

        assertTrue(firstFailure.getMessage().contains("transient deletion failure"));
        assertTrue(Files.isDirectory(generationRoot));

        AtomicReference<Throwable> leftFailure = new AtomicReference<>();
        AtomicReference<Throwable> rightFailure = new AtomicReference<>();
        CountDownLatch retry = new CountDownLatch(1);
        Thread left = Thread.ofVirtual().start(() -> closeLease(
                lease, retry, leftFailure));
        Thread right = Thread.ofVirtual().start(() -> closeLease(
                lease, retry, rightFailure));
        retry.countDown();
        left.join();
        right.join();

        assertNull(leftFailure.get());
        assertNull(rightFailure.get());
        assertEquals(2, deletionAttempts.get());
        assertFalse(Files.exists(generationRoot));
        lease.close();
        artifact.close();
    }

    @Test
    void rejectsAFileChangedAfterSnapshotAndRemovesStagingTree() throws Exception {
        Path source = artifactSource();
        WebCanvasArtifactContract.ArtifactSnapshot snapshot = snapshot(source);
        Files.writeString(source.resolve("main.dart.js"), "tampered-script",
                StandardCharsets.UTF_8);
        Path owned = temporary.resolve("published");
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(owned);

        IOException error = assertThrows(IOException.class,
                () -> publisher.publish(snapshot, GENERATION));

        assertTrue(error.getMessage().contains("changed after validation"));
        try (var entries = Files.list(owned)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void verifiesExcludedBuildMetadataWithoutPublishingIt() throws Exception {
        Path source = artifactSource();
        WebCanvasArtifactContract.ArtifactSnapshot snapshot = snapshot(source);
        Files.writeString(source.resolve(".last_build_id"), "different-build",
                StandardCharsets.UTF_8);
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));

        IOException error = assertThrows(IOException.class,
                () -> publisher.publish(snapshot, GENERATION));

        assertTrue(error.getMessage().contains("changed after validation"));
    }

    @Test
    void rejectsCaseInsensitiveSnapshotPathCollision() throws Exception {
        Path source = artifactSource();
        byte[] index = Files.readAllBytes(source.resolve("index.html"));
        WebCanvasArtifactContract.ArtifactFile lower = file("index.html", index);
        WebCanvasArtifactContract.ArtifactFile upper = file("Index.html", index);
        WebCanvasArtifactContract.ArtifactFile metadata = metadata(source);
        Map<String, WebCanvasArtifactContract.ArtifactFile> files = new TreeMap<>();
        files.put(lower.relativePath(), lower);
        files.put(upper.relativePath(), upper);
        WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                new WebCanvasArtifactContract.ArtifactSnapshot(
                        source, files, Map.of(".last_build_id", metadata),
                        lower.size() + upper.size() + metadata.size(), "a".repeat(64));
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));

        IOException error = assertThrows(IOException.class,
                () -> publisher.publish(snapshot, GENERATION));

        assertTrue(error.getMessage().contains("case-insensitive path collision"));
    }

    @Test
    void rejectsSourceSymlinkWithoutReadingItsTarget() throws Exception {
        Path source = artifactSource();
        WebCanvasArtifactContract.ArtifactSnapshot snapshot = snapshot(source);
        Path external = temporary.resolve("external.js");
        Files.writeString(external, "trusted-script", StandardCharsets.UTF_8);
        Path script = source.resolve("main.dart.js");
        Files.delete(script);
        try {
            Files.createSymbolicLink(script, external);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "symbolic links are not available: " + exception.getMessage());
        }
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));

        IOException error = assertThrows(IOException.class,
                () -> publisher.publish(snapshot, GENERATION));

        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("reparse"));
        assertEquals("trusted-script", Files.readString(external));
    }

    @Test
    void cleanupFailsClosedWhenPublishedTreeIsReplacedByLink() throws Exception {
        Path source = artifactSource();
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));
        WebCanvasArtifactPublisher.PublishedArtifact artifact =
                publisher.publish(snapshot(source), GENERATION);
        Path external = temporary.resolve("external.js");
        Files.writeString(external, "keep-me", StandardCharsets.UTF_8);
        Path script = artifact.root().resolve("main.dart.js");
        Files.delete(script);
        try {
            Files.createSymbolicLink(script, external);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "symbolic links are not available: " + exception.getMessage());
        }

        IOException error = assertThrows(IOException.class, artifact::close);

        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("reparse"));
        assertEquals("keep-me", Files.readString(external));
    }

    @Test
    void requiresLowercaseDigestGenerationIdentity() throws Exception {
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                temporary.resolve("published"));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(snapshot(artifactSource()), "ABC"));
    }

    private Path artifactSource() throws IOException {
        Path source = Files.createTempDirectory(temporary, "source-");
        Files.writeString(source.resolve("index.html"), "<html>canvas</html>",
                StandardCharsets.UTF_8);
        Files.writeString(source.resolve("main.dart.js"), "trusted-script",
                StandardCharsets.UTF_8);
        Path assets = Files.createDirectory(source.resolve("assets"));
        Files.writeString(assets.resolve("NOTICES"), "license", StandardCharsets.UTF_8);
        Files.writeString(source.resolve(".last_build_id"), "build-1",
                StandardCharsets.UTF_8);
        return source;
    }

    private static void closeLease(
            WebCanvasArtifactPublisher.PublishedArtifact.Lease lease,
            CountDownLatch start,
            AtomicReference<Throwable> failure) {
        await(start);
        try {
            lease.close();
        } catch (Throwable problem) {
            failure.set(problem);
        }
    }

    private static WebCanvasArtifactContract.ArtifactSnapshot snapshot(Path source)
            throws IOException {
        Map<String, WebCanvasArtifactContract.ArtifactFile> files = new TreeMap<>();
        try (var paths = Files.walk(source)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(".last_build_id"))
                    .toList()) {
                String relative = source.relativize(path).toString().replace('\\', '/');
                byte[] bytes = Files.readAllBytes(path);
                files.put(relative, file(relative, bytes));
            }
        }
        WebCanvasArtifactContract.ArtifactFile metadata = metadata(source);
        long total = metadata.size();
        for (WebCanvasArtifactContract.ArtifactFile file : files.values()) {
            total += file.size();
        }
        return new WebCanvasArtifactContract.ArtifactSnapshot(
                source, files, Map.of(".last_build_id", metadata), total, "b".repeat(64));
    }

    private static WebCanvasArtifactContract.ArtifactFile metadata(Path source)
            throws IOException {
        byte[] bytes = Files.readAllBytes(source.resolve(".last_build_id"));
        return file(".last_build_id", bytes);
    }

    private static WebCanvasArtifactContract.ArtifactFile file(String relative, byte[] bytes) {
        return new WebCanvasArtifactContract.ArtifactFile(
                relative, bytes.length, sha256(bytes));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
