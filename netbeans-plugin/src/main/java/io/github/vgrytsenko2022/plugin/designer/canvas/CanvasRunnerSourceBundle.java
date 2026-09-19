package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.canvas.runner.CanvasRunnerBundle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Secure extractor for the immutable, versioned Flutter Canvas runner sources. */
public final class CanvasRunnerSourceBundle {
    static final int MAX_MANIFEST_BYTES = 128 * 1024;
    static final int MAX_ENTRIES = 512;
    static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    // The reviewed 249-widget schema now exceeds 4 MiB. Keep the larger bound
    // limited to its exact packaged path, not arbitrary runner inputs.
    static final long MAX_MODEL_BYTES = 8L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 32L * 1024 * 1024;
    private static final String COMPLETION_MARKER = ".netbeans-canvas-runner-source";
    private static final int MAX_CACHED_WORKSPACE_PATHS = 250_000;

    private final ResourceAccess resources;
    private final String version;
    private final List<SourceEntry> entries;
    private final String sourceDigest;

    public static CanvasRunnerSourceBundle packaged() throws IOException {
        return new CanvasRunnerSourceBundle(new ResourceAccess() {
            @Override
            public InputStream openManifest() {
                return CanvasRunnerBundle.openManifest();
            }

            @Override
            public InputStream openSource(String relativePath) {
                return CanvasRunnerBundle.openSource(relativePath);
            }
        }, CanvasRunnerBundle.VERSION);
    }

    CanvasRunnerSourceBundle(ResourceAccess resources, String version) throws IOException {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.version = requireToken(version, "bundle version");
        this.entries = parseManifest(resources);
        this.sourceDigest = digestManifest(entries);
    }

    public String version() {
        return version;
    }

    public String sourceDigest() {
        return sourceDigest;
    }

    /**
     * Extracts and verifies this source bundle below an identity-specific cache
     * directory. Existing corrupt entries are rejected, never reused.
     */
    Path extract(Path cacheRoot, CanvasRunnerCacheIdentity identity) throws IOException {
        Objects.requireNonNull(identity, "identity");
        if (!sourceDigest.equals(identity.sourceDigest())) {
            throw new IOException("Canvas runner cache identity does not match the packaged sources");
        }
        Path safeRoot = prepareDirectory(cacheRoot, "Canvas runner cache root");
        Path lockPath = safeRoot.resolve("CanvasRunnerSource-"
                + identity.cacheDirectoryName() + ".lock");
        ensureDirectChild(safeRoot, lockPath);
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                FileLock ignored = channel.lock()) {
            rejectSymlink(lockPath, "Canvas runner source lock");
            Path identityRoot = safeRoot.resolve(identity.cacheDirectoryName());
            ensureDirectChild(safeRoot, identityRoot);
            prepareDirectory(identityRoot, "Canvas runner identity cache");
            Path target = identityRoot.resolve("source-v" + version);
            ensureDirectChild(identityRoot, target);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                verifyExtracted(target, identity);
                return target;
            }
            Path temporary = Files.createTempDirectory(identityRoot, "CanvasRunnerSource-");
            rejectSymlink(temporary, "temporary Canvas runner source directory");
            Path staged = temporary.resolve("source");
            Files.createDirectory(staged);
            try {
                extractEntries(staged);
                writeMarker(staged, identity);
                verifyExtracted(staged, identity);
                moveAtomically(staged, target);
            } finally {
                deleteTemporaryTree(temporary, identityRoot);
            }
            verifyExtracted(target, identity);
            return target;
        }
    }

    private void extractEntries(Path staged) throws IOException {
        for (SourceEntry entry : entries) {
            Path output = resolveEntry(staged, entry.path());
            Path parent = output.getParent();
            createSafeDirectories(staged, parent);
            MessageDigest digest = sha256();
            long written = 0;
            try (InputStream input = resources.openSource(entry.path());
                    FileChannel destination = FileChannel.open(output,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (count == 0) {
                        continue;
                    }
                    written += count;
                    if (written > entry.size()) {
                        throw new IOException("packaged Canvas runner source exceeds manifest size: "
                                + entry.path());
                    }
                    digest.update(buffer, 0, count);
                    ByteBuffer bytes = ByteBuffer.wrap(buffer, 0, count);
                    while (bytes.hasRemaining()) {
                        destination.write(bytes);
                    }
                }
            } catch (RuntimeException ex) {
                throw new IOException("cannot read packaged Canvas runner source: " + entry.path(), ex);
            }
            if (written != entry.size() || !entry.sha256().equals(hex(digest.digest()))) {
                throw new IOException("packaged Canvas runner source failed manifest verification: "
                        + entry.path());
            }
        }
    }

    private void verifyExtracted(
            Path sourceRoot,
            CanvasRunnerCacheIdentity identity) throws IOException {
        requireSafeDirectory(sourceRoot, "Canvas runner source cache");
        Path marker = sourceRoot.resolve(COMPLETION_MARKER);
        requireNoLinkComponents(sourceRoot, marker, "Canvas runner source marker");
        String expectedMarker = version + "\n" + sourceDigest + "\n"
                + identity.cacheKey() + "\n";
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                || Files.size(marker) > 256
                || !expectedMarker.equals(Files.readString(marker, StandardCharsets.US_ASCII))) {
            throw new IOException("Canvas runner source cache is incomplete or belongs to another bundle");
        }
        for (SourceEntry entry : entries) {
            Path file = resolveEntry(sourceRoot, entry.path());
            requireNoLinkComponents(
                    sourceRoot, file, "cached Canvas runner source " + entry.path());
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file) != entry.size()
                    || !entry.sha256().equals(digestFile(file))) {
                throw new IOException("cached Canvas runner source is missing or modified: "
                        + entry.path());
            }
        }
        verifyMutableBuildPaths(sourceRoot);
    }

    /**
     * Rejects every link/reparse point in the cached tree before Flutter can
     * rewrite generated directories or files through it.
     */
    void verifyMutableBuildPaths(Path sourceRoot) throws IOException {
        Path root = sourceRoot.toAbsolutePath().normalize();
        requireSafeDirectory(root, "Canvas runner source cache");
        Path realRoot = root.toRealPath();
        int[] visited = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            private void count(Path path) throws IOException {
                if (++visited[0] > MAX_CACHED_WORKSPACE_PATHS) {
                    throw new IOException("Canvas runner cached workspace contains more than "
                            + MAX_CACHED_WORKSPACE_PATHS + " paths: " + root);
                }
                rejectSymlink(path, "Canvas runner cached workspace path");
            }

            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                count(directory);
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException(
                            "Canvas runner cached workspace contains a non-directory path: "
                            + directory);
                }
                if (!directory.toRealPath().startsWith(realRoot)) {
                    throw new IOException(
                            "Canvas runner cached workspace directory resolves outside cache: "
                            + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                count(file);
                if (!attributes.isRegularFile() || attributes.isOther()) {
                    throw new IOException(
                            "Canvas runner cached workspace contains a link or non-regular file: "
                            + file);
                }
                if (!file.toRealPath().startsWith(realRoot)) {
                    throw new IOException(
                            "Canvas runner cached workspace file resolves outside cache: " + file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException failure)
                    throws IOException {
                throw new IOException(
                        "cannot inspect Canvas runner cached workspace path: " + file, failure);
            }
        });
    }

    private void writeMarker(Path staged, CanvasRunnerCacheIdentity identity) throws IOException {
        Path marker = staged.resolve(COMPLETION_MARKER);
        Files.writeString(marker,
                version + "\n" + sourceDigest + "\n" + identity.cacheKey() + "\n",
                StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
    }

    private static List<SourceEntry> parseManifest(ResourceAccess resources) throws IOException {
        byte[] manifest;
        try (InputStream input = resources.openManifest()) {
            manifest = readBounded(input, MAX_MANIFEST_BYTES, "Canvas runner source manifest");
        } catch (RuntimeException ex) {
            throw new IOException("cannot read packaged Canvas runner source manifest", ex);
        }
        String text = StandardCharsets.US_ASCII.newDecoder().decode(ByteBuffer.wrap(manifest)).toString();
        List<SourceEntry> parsed = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        long total = 0;
        for (String rawLine : text.split("\\R", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\|", -1);
            if (fields.length != 3) {
                throw new IOException("malformed Canvas runner source manifest entry");
            }
            String path = validateRelativePath(fields[0]);
            long size;
            try {
                size = Long.parseLong(fields[1]);
            } catch (NumberFormatException ex) {
                throw new IOException("invalid size in Canvas runner source manifest: " + path, ex);
            }
            long maximum = path.equals("lib/src/canvas_model.dart") ? MAX_MODEL_BYTES : MAX_FILE_BYTES;
            if (size < 0 || size > maximum) {
                throw new IOException("Canvas runner source is outside the per-file limit: " + path);
            }
            String digest = fields[2].toLowerCase(java.util.Locale.ROOT);
            if (!digest.matches("[0-9a-f]{64}")) {
                throw new IOException("invalid digest in Canvas runner source manifest: " + path);
            }
            if (!paths.add(path)) {
                throw new IOException("duplicate Canvas runner source manifest path: " + path);
            }
            total = Math.addExact(total, size);
            if (total > MAX_TOTAL_BYTES || parsed.size() >= MAX_ENTRIES) {
                throw new IOException("Canvas runner source bundle exceeds extraction limits");
            }
            parsed.add(new SourceEntry(path, size, digest));
        }
        if (parsed.isEmpty()) {
            throw new IOException("Canvas runner source manifest is empty");
        }
        parsed.sort(Comparator.comparing(SourceEntry::path));
        return List.copyOf(parsed);
    }

    private static String validateRelativePath(String value) throws IOException {
        if (value == null || value.isBlank() || value.length() > 512
                || value.startsWith("/") || value.startsWith("\\")
                || value.contains("\\") || value.contains(":") || value.indexOf('\0') >= 0) {
            throw new IOException("unsafe Canvas runner source path in manifest");
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("unsafe Canvas runner source path in manifest: " + value);
            }
        }
        Path normalized = Path.of(value).normalize();
        if (normalized.isAbsolute() || !normalized.toString().replace('\\', '/').equals(value)) {
            throw new IOException("non-normal Canvas runner source path in manifest: " + value);
        }
        return value;
    }

    private static String digestManifest(List<SourceEntry> entries) {
        MessageDigest digest = sha256();
        for (SourceEntry entry : entries) {
            digest.update(entry.path().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Long.toString(entry.size()).getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) 0);
            digest.update(entry.sha256().getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) '\n');
        }
        return hex(digest.digest());
    }

    private static byte[] readBounded(InputStream input, int limit, String label) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8192));
        byte[] buffer = new byte[4096];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) >= 0) {
            if (count == 0) {
                continue;
            }
            total += count;
            if (total > limit) {
                throw new IOException(label + " exceeds " + limit + " bytes");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static Path prepareDirectory(Path directory, String label) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path absolute = directory.toAbsolutePath().normalize();
        Files.createDirectories(absolute);
        requireSafeDirectory(absolute, label);
        return absolute;
    }

    private static void createSafeDirectories(Path root, Path directory) throws IOException {
        Path relative = root.relativize(directory);
        Path cursor = root;
        for (Path segment : relative) {
            cursor = cursor.resolve(segment);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
                requireSafeDirectory(cursor, "Canvas runner source directory");
            } else {
                Files.createDirectory(cursor);
            }
        }
    }

    private static void requireSafeDirectory(Path directory, String label) throws IOException {
        rejectSymlink(directory, label);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a directory: " + directory);
        }
    }

    private static void rejectSymlink(Path path, String label) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
            return;
        }
        BasicFileAttributes noFollow = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (noFollow.isSymbolicLink() || noFollow.isOther() || Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link or junction: " + path);
        }
        BasicFileAttributes followed = Files.readAttributes(path, BasicFileAttributes.class);
        if (noFollow.fileKey() != null && followed.fileKey() != null
                && !noFollow.fileKey().equals(followed.fileKey())) {
            throw new IOException(label + " resolves through a link or junction: " + path);
        }
    }

    private static void requireNoLinkComponents(Path root, Path target, String label)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException(label + " escaped its cache directory");
        }
        requireSafeDirectory(normalizedRoot, "Canvas runner source cache");
        Path realRoot = normalizedRoot.toRealPath();
        Path current = normalizedRoot;
        for (Path segment : normalizedRoot.relativize(normalizedTarget)) {
            current = current.resolve(segment);
            rejectSymlink(current, label);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(label + " is missing: " + current);
            }
            Path real = current.toRealPath();
            if (!real.startsWith(realRoot)) {
                throw new IOException(label + " resolves outside its cache directory: " + current);
            }
        }
    }

    private static Path resolveEntry(Path root, String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Canvas runner source escaped its cache directory");
        }
        return resolved;
    }

    private static void ensureDirectChild(Path parent, Path child) throws IOException {
        if (!parent.equals(child.getParent())) {
            throw new IOException("Canvas runner cache path escaped its parent directory");
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private static void deleteTemporaryTree(Path temporary, Path expectedParent) throws IOException {
        if (!expectedParent.equals(temporary.getParent()) || Files.isSymbolicLink(temporary)) {
            throw new IOException("refusing to clean an unsafe Canvas runner temporary directory");
        }
        if (!Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var paths = Files.walk(temporary)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String digestFile(Path file) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
        }
        return hex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private static String requireToken(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return value;
    }

    interface ResourceAccess {
        InputStream openManifest() throws IOException;

        InputStream openSource(String relativePath) throws IOException;
    }

    private record SourceEntry(String path, long size, String sha256) {
    }
}
