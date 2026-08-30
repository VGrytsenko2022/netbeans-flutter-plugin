package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Publishes a validated Web Canvas artifact into an isolated, owned directory.
 *
 * <p>The validated build tree remains an untrusted mutable input at this
 * boundary. Every source file is therefore opened without following links,
 * copied into a new staging generation, and checked against the validated size
 * and digest. The staging tree is independently rehashed before it is renamed
 * into its immutable generation name.
 */
final class WebCanvasArtifactPublisher {
    private static final String BUILD_METADATA = ".last_build_id";
    private static final String GENERATION_PREFIX = "generation-";
    private static final String STAGING_PREFIX = ".staging-";
    private static final int COPY_BUFFER_BYTES = 64 * 1024;

    private final Path ownedRoot;

    WebCanvasArtifactPublisher(Path ownedRoot) throws IOException {
        Path candidate = Objects.requireNonNull(ownedRoot, "ownedRoot")
                .toAbsolutePath().normalize();
        Path parent = candidate.getParent();
        if (parent == null) {
            throw new IOException("Web Canvas publication root has no parent");
        }
        requireSafeDirectory(parent, "Web Canvas publication parent");
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(candidate);
        }
        this.ownedRoot = requireSafeDirectory(candidate,
                "Web Canvas publication root").toRealPath();
    }

    PublishedArtifact publish(
            WebCanvasArtifactContract.ArtifactSnapshot snapshot,
            String generationId) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        String generation = requireGenerationId(generationId);
        Map<String, WebCanvasArtifactContract.ArtifactFile> files =
                validateSnapshotShape(snapshot);
        Path sourceRoot = requireSafeDirectory(snapshot.root(),
                "validated Web Canvas artifact root").toRealPath();
        if (ownedRoot.startsWith(sourceRoot) || sourceRoot.startsWith(ownedRoot)) {
            throw new IOException("Web Canvas publication and source roots must be disjoint");
        }

        Path finalRoot = ownedRoot.resolve(GENERATION_PREFIX + generation);
        requireDirectChild(finalRoot, GENERATION_PREFIX + generation);
        rejectLinkOrReparse(finalRoot, "Web Canvas publication generation");
        if (Files.exists(finalRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Web Canvas publication generation already exists: "
                    + generation);
        }

        Path stagingRoot = ownedRoot.resolve(STAGING_PREFIX + generation + "-"
                + UUID.randomUUID().toString().replace("-", ""));
        requireDirectChild(stagingRoot, stagingRoot.getFileName().toString());
        Files.createDirectory(stagingRoot);
        boolean moved = false;
        try {
            Set<Object> sourceFileKeys = new HashSet<>();
            for (WebCanvasArtifactContract.ArtifactFile file : files.values()) {
                copyVerified(sourceRoot, stagingRoot, file, sourceFileKeys);
            }
            for (WebCanvasArtifactContract.ArtifactFile metadata
                    : snapshot.excludedBuildMetadata().values()) {
                verifySourceOnly(sourceRoot, metadata, sourceFileKeys);
            }
            verifyPublishedTree(stagingRoot, files);
            try {
                Files.move(stagingRoot, finalRoot, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(stagingRoot, finalRoot);
            }
            moved = true;
            verifyPublishedTree(finalRoot, files);
            return new PublishedArtifact(this, generation, finalRoot.toRealPath(), files,
                    snapshot.sha256());
        } catch (IOException | RuntimeException failure) {
            Path cleanup = moved ? finalRoot : stagingRoot;
            try {
                deleteSafeTree(cleanup, "incomplete Web Canvas publication");
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private Map<String, WebCanvasArtifactContract.ArtifactFile> validateSnapshotShape(
            WebCanvasArtifactContract.ArtifactSnapshot snapshot) throws IOException {
        TreeMap<String, WebCanvasArtifactContract.ArtifactFile> files = new TreeMap<>();
        Set<String> foldedPaths = new HashSet<>();
        long total = 0;
        for (Map.Entry<String, WebCanvasArtifactContract.ArtifactFile> entry
                : snapshot.files().entrySet()) {
            String relative = requirePortableRelative(entry.getKey());
            WebCanvasArtifactContract.ArtifactFile file =
                    Objects.requireNonNull(entry.getValue(), "artifact file");
            if (!relative.equals(file.relativePath())) {
                throw new IOException("Web Canvas artifact path/key mismatch: " + relative);
            }
            if (BUILD_METADATA.equals(relative)) {
                throw new IOException("Web Canvas build metadata must not be published");
            }
            if (!foldedPaths.add(relative.toLowerCase(Locale.ROOT))) {
                throw new IOException("case-insensitive path collision in Web Canvas publication: "
                        + relative);
            }
            total = addExact(total, file.size());
            files.put(relative, file);
        }
        if (files.isEmpty()) {
            throw new IOException("Web Canvas publication cannot be empty");
        }
        if (!snapshot.excludedBuildMetadata().keySet().equals(Set.of(BUILD_METADATA))) {
            throw new IOException("Web Canvas snapshot has invalid excluded build metadata");
        }
        WebCanvasArtifactContract.ArtifactFile metadata =
                snapshot.excludedBuildMetadata().get(BUILD_METADATA);
        if (!BUILD_METADATA.equals(metadata.relativePath())) {
            throw new IOException("Web Canvas build metadata path/key mismatch");
        }
        total = addExact(total, metadata.size());
        if (total != snapshot.totalBytes()) {
            throw new IOException("Web Canvas snapshot byte total is inconsistent");
        }
        return Collections.unmodifiableMap(files);
    }

    private static void copyVerified(
            Path sourceRoot,
            Path targetRoot,
            WebCanvasArtifactContract.ArtifactFile expected,
            Set<Object> sourceFileKeys) throws IOException {
        Path source = resolvePortable(sourceRoot, expected.relativePath());
        Path target = resolvePortable(targetRoot, expected.relativePath());
        requireNoLinkedComponents(sourceRoot, source, "Web Canvas source file");
        createSafeParents(targetRoot, target.getParent());
        rejectLinkOrReparse(target, "Web Canvas publication file");

        BasicFileAttributes before = requireRegularFile(
                source, "Web Canvas source file " + expected.relativePath());
        requireExpectedSize(before.size(), expected);
        requireUniqueFileKey(before.fileKey(), sourceFileKeys, expected.relativePath());

        MessageDigest digest = sha256();
        long copied = 0;
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        try (InputStream input = Files.newInputStream(source,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
                FileChannel output = FileChannel.open(target,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE,
                        LinkOption.NOFOLLOW_LINKS)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                copied = addExact(copied, count);
                if (copied > expected.size()) {
                    throw changed(expected.relativePath());
                }
                digest.update(buffer, 0, count);
                ByteBuffer bytes = ByteBuffer.wrap(buffer, 0, count);
                while (bytes.hasRemaining()) {
                    output.write(bytes);
                }
            }
            output.force(true);
        }

        requireStableSource(source, before, expected, copied,
                HexFormat.of().formatHex(digest.digest()));
        verifyFile(target, expected, "published Web Canvas file");
    }

    private static void verifySourceOnly(
            Path sourceRoot,
            WebCanvasArtifactContract.ArtifactFile expected,
            Set<Object> sourceFileKeys) throws IOException {
        String relative = requirePortableRelative(expected.relativePath());
        if (!BUILD_METADATA.equals(relative)) {
            throw new IOException("unexpected excluded Web Canvas file: " + relative);
        }
        Path source = resolvePortable(sourceRoot, relative);
        requireNoLinkedComponents(sourceRoot, source, "Web Canvas build metadata");
        BasicFileAttributes before = requireRegularFile(source,
                "Web Canvas build metadata");
        requireExpectedSize(before.size(), expected);
        requireUniqueFileKey(before.fileKey(), sourceFileKeys, relative);
        HashResult hash = hashFile(source, expected.size());
        requireStableSource(source, before, expected, hash.size(), hash.sha256());
    }

    private static void requireStableSource(
            Path source,
            BasicFileAttributes before,
            WebCanvasArtifactContract.ArtifactFile expected,
            long copied,
            String sha256) throws IOException {
        BasicFileAttributes after = requireRegularFile(source,
                "Web Canvas source file " + expected.relativePath());
        if (copied != expected.size()
                || after.size() != before.size()
                || !after.lastModifiedTime().equals(before.lastModifiedTime())
                || !sameFileKey(before.fileKey(), after.fileKey())
                || !expected.sha256().equals(sha256)) {
            throw changed(expected.relativePath());
        }
    }

    private static void verifyPublishedTree(
            Path root,
            Map<String, WebCanvasArtifactContract.ArtifactFile> expected) throws IOException {
        Path safeRoot = requireSafeDirectory(root, "Web Canvas publication generation");
        Set<String> actual = new HashSet<>();
        Set<String> folded = new HashSet<>();
        Files.walkFileTree(safeRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(directory, "Web Canvas publication directory");
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException("unsafe directory in Web Canvas publication: "
                            + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(file, "Web Canvas publication file");
                if (!attributes.isRegularFile() || attributes.isOther()) {
                    throw new IOException("non-regular file in Web Canvas publication: " + file);
                }
                String relative = portableRelative(safeRoot, file);
                if (!actual.add(relative)
                        || !folded.add(relative.toLowerCase(Locale.ROOT))) {
                    throw new IOException("case-insensitive path collision in Web Canvas "
                            + "publication: " + relative);
                }
                WebCanvasArtifactContract.ArtifactFile artifact = expected.get(relative);
                if (artifact == null) {
                    throw new IOException("unexpected file in Web Canvas publication: "
                            + relative);
                }
                verifyFile(file, artifact, "published Web Canvas file");
                return FileVisitResult.CONTINUE;
            }
        });
        if (!actual.equals(expected.keySet())) {
            throw new IOException("Web Canvas publication file set differs from snapshot");
        }
    }

    private static void verifyFile(
            Path path,
            WebCanvasArtifactContract.ArtifactFile expected,
            String label) throws IOException {
        BasicFileAttributes before = requireRegularFile(path,
                label + " " + expected.relativePath());
        requireExpectedSize(before.size(), expected);
        HashResult hash = hashFile(path, expected.size());
        BasicFileAttributes after = requireRegularFile(path,
                label + " " + expected.relativePath());
        if (hash.size() != expected.size()
                || !hash.sha256().equals(expected.sha256())
                || after.size() != before.size()
                || !after.lastModifiedTime().equals(before.lastModifiedTime())
                || !sameFileKey(before.fileKey(), after.fileKey())) {
            throw new IOException(label + " failed verification: " + expected.relativePath());
        }
    }

    private static HashResult hashFile(Path path, long maximum) throws IOException {
        MessageDigest digest = sha256();
        long read = 0;
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        try (InputStream input = Files.newInputStream(path,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                read = addExact(read, count);
                if (read > maximum) {
                    throw new IOException("Web Canvas file grew while hashing: " + path);
                }
                digest.update(buffer, 0, count);
            }
        }
        return new HashResult(read, HexFormat.of().formatHex(digest.digest()));
    }

    private static BasicFileAttributes requireRegularFile(Path path, String label)
            throws IOException {
        rejectLinkOrReparse(path, label);
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isOther()) {
            throw new IOException(label + " is not a regular file: " + path);
        }
        return attributes;
    }

    private static void createSafeParents(Path root, Path targetParent) throws IOException {
        Path safeParent = requireInside(root, targetParent, "Web Canvas publication directory");
        Path current = root;
        for (Path segment : root.relativize(safeParent)) {
            current = current.resolve(segment);
            rejectLinkOrReparse(current, "Web Canvas publication directory");
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(current);
            }
            requireSafeDirectory(current, "Web Canvas publication directory");
        }
    }

    private static Path resolvePortable(Path root, String relative) throws IOException {
        String safeRelative = requirePortableRelative(relative);
        Path resolved = root;
        for (String segment : safeRelative.split("/")) {
            resolved = resolved.resolve(segment);
        }
        return requireInside(root, resolved, "Web Canvas artifact path");
    }

    private static String portableRelative(Path root, Path path) throws IOException {
        Path safe = requireInside(root, path, "Web Canvas publication path");
        return requirePortableRelative(root.relativize(safe).toString().replace('\\', '/'));
    }

    private static String requirePortableRelative(String value) throws IOException {
        Objects.requireNonNull(value, "relative path");
        if (value.isEmpty() || value.startsWith("/") || value.endsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf('%') >= 0) {
            throw new IOException("invalid Web Canvas artifact path: " + value);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                    || !segment.matches("[A-Za-z0-9._-]+")) {
                throw new IOException("invalid Web Canvas artifact path: " + value);
            }
        }
        return value;
    }

    private void requireDirectChild(Path path, String expectedName) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!ownedRoot.equals(normalized.getParent())
                || !expectedName.equals(normalized.getFileName().toString())) {
            throw new IOException("Web Canvas publication path escaped its owned root");
        }
    }

    private void delete(PublishedArtifact artifact) throws IOException {
        if (artifact.owner != this) {
            throw new IOException("Web Canvas publication belongs to another publisher");
        }
        requireDirectChild(artifact.root,
                GENERATION_PREFIX + artifact.generationId);
        deleteSafeTree(artifact.root, "Web Canvas publication generation");
    }

    private void deleteSafeTree(Path tree, String label) throws IOException {
        requireDirectChild(tree, tree.getFileName().toString());
        rejectLinkOrReparse(tree, label);
        if (!Files.exists(tree, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        requireSafeDirectory(tree, label);
        Files.walkFileTree(tree, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(directory, label);
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException(label + " contains an unsafe directory: " + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) throws IOException {
                rejectLinkOrReparse(file, label);
                if (!attributes.isRegularFile() || attributes.isOther()) {
                    throw new IOException(label + " contains a non-regular file: " + file);
                }
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                    throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path requireSafeDirectory(Path path, String label) throws IOException {
        Path normalized = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        rejectLinkOrReparse(normalized, label);
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a safe directory: " + normalized);
        }
        return normalized;
    }

    private static Path requireInside(Path root, Path candidate, String label)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedRoot)) {
            throw new IOException(label + " escaped its trusted root");
        }
        return normalized;
    }

    private static void requireNoLinkedComponents(Path root, Path target, String label)
            throws IOException {
        Path safeTarget = requireInside(root, target, label);
        Path current = root;
        rejectLinkOrReparse(current, label);
        for (Path segment : root.relativize(safeTarget)) {
            current = current.resolve(segment);
            rejectLinkOrReparse(current, label);
        }
    }

    private static void rejectLinkOrReparse(Path path, String label) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
            return;
        }
        BasicFileAttributes noFollow = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (noFollow.isSymbolicLink() || noFollow.isOther() || Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a link or reparse point: " + path);
        }
        BasicFileAttributes followed = Files.readAttributes(path, BasicFileAttributes.class);
        if (noFollow.fileKey() != null && followed.fileKey() != null
                && !noFollow.fileKey().equals(followed.fileKey())) {
            throw new IOException(label + " resolves through a link or reparse point: " + path);
        }
        try {
            Object attributes = Files.getAttribute(
                    path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            if (attributes instanceof Number value && (value.intValue() & 0x400) != 0) {
                throw new IOException(label + " must not be a Windows reparse point: " + path);
            }
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            // Non-Windows providers do not expose the raw DOS reparse bit.
        }
    }

    private static void requireExpectedSize(
            long actual, WebCanvasArtifactContract.ArtifactFile expected) throws IOException {
        if (actual != expected.size()) {
            throw changed(expected.relativePath());
        }
    }

    private static void requireUniqueFileKey(
            Object fileKey, Set<Object> fileKeys, String relative) throws IOException {
        if (fileKey != null && !fileKeys.add(fileKey)) {
            throw new IOException("hard-linked files are not allowed in Web Canvas publication: "
                    + relative);
        }
    }

    private static boolean sameFileKey(Object first, Object second) {
        return first == null || second == null || first.equals(second);
    }

    private static IOException changed(String relative) {
        return new IOException("Web Canvas artifact changed after validation: " + relative);
    }

    private static long addExact(long first, long second) throws IOException {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            throw new IOException("Web Canvas artifact byte total overflow", exception);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    private static String requireGenerationId(String value) {
        Objects.requireNonNull(value, "generationId");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Web Canvas generation id must be a lowercase SHA-256 value");
        }
        return value;
    }

    static final class PublishedArtifact implements AutoCloseable {
        private final WebCanvasArtifactPublisher owner;
        private final String generationId;
        private final Path root;
        private final Map<String, WebCanvasArtifactContract.ArtifactFile> files;
        private final String sourceSnapshotSha256;
        private int leaseCount;
        private boolean closeRequested;
        private boolean deleting;
        private boolean deleted;

        private PublishedArtifact(
                WebCanvasArtifactPublisher owner,
                String generationId,
                Path root,
                Map<String, WebCanvasArtifactContract.ArtifactFile> files,
                String sourceSnapshotSha256) {
            this.owner = owner;
            this.generationId = generationId;
            this.root = root;
            this.files = files;
            this.sourceSnapshotSha256 = sourceSnapshotSha256;
        }

        String generationId() {
            return generationId;
        }

        Path root() {
            return root;
        }

        Map<String, WebCanvasArtifactContract.ArtifactFile> files() {
            return files;
        }

        String sourceSnapshotSha256() {
            return sourceSnapshotSha256;
        }

        /**
         * Retains this immutable generation for one active native consumer.
         * Once the owning handle has been closed no new consumer may enter,
         * while existing leases keep the generation alive until their own
         * close completes.
         */
        synchronized Lease acquireLease() {
            if (closeRequested || deleted) {
                throw new IllegalStateException(
                        "Web Canvas publication is already closing or closed");
            }
            leaseCount++;
            return new Lease(this);
        }

        @Override
        public void close() throws IOException {
            boolean deleteNow;
            synchronized (this) {
                if (deleted) {
                    return;
                }
                closeRequested = true;
                awaitConcurrentDeletion();
                if (deleted || leaseCount > 0) {
                    return;
                }
                deleting = true;
                deleteNow = true;
            }
            if (deleteNow) {
                deleteGeneration();
            }
        }

        private void releaseLease() throws IOException {
            boolean deleteNow = false;
            synchronized (this) {
                if (leaseCount <= 0) {
                    throw new IllegalStateException(
                            "Web Canvas publication lease accounting underflow");
                }
                leaseCount--;
                if (closeRequested && leaseCount == 0 && !deleted && !deleting) {
                    deleting = true;
                    deleteNow = true;
                }
            }
            if (deleteNow) {
                deleteGeneration();
            }
        }

        private void deleteGeneration() throws IOException {
            IOException deletionFailure = null;
            boolean deletedSuccessfully = false;
            try {
                owner.delete(this);
                deletedSuccessfully = true;
            } catch (IOException failure) {
                deletionFailure = failure;
            } finally {
                synchronized (this) {
                    deleting = false;
                    deleted = deletedSuccessfully;
                    notifyAll();
                }
            }
            if (deletionFailure != null) {
                throw deletionFailure;
            }
        }

        private void awaitConcurrentDeletion() throws IOException {
            while (deleting) {
                try {
                    wait();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Interrupted while closing Web Canvas publication",
                            interrupted);
                }
            }
        }

        static final class Lease implements AutoCloseable {
            private PublishedArtifact artifact;

            private Lease(PublishedArtifact artifact) {
                this.artifact = artifact;
            }

            @Override
            public void close() throws IOException {
                PublishedArtifact retained;
                synchronized (this) {
                    retained = artifact;
                    artifact = null;
                }
                if (retained != null) {
                    retained.releaseLease();
                }
            }
        }
    }

    private record HashResult(long size, String sha256) {
    }
}
