package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasRunnerContract;

import java.io.ByteArrayOutputStream;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Integrity boundary for a cached, provider-contracted Flutter Canvas runtime.
 *
 * <p>The generated manifest is deliberately limited to the executable runtime
 * allowlist. Debug symbols are not needed to launch the runner and therefore
 * are neither trusted nor required. Every Flutter asset is, however, recorded
 * by size and SHA-256 so a partially copied or modified runtime is never reused.
 */
final class CanvasRunnerRuntimeCache {
    private static final String MANIFEST_NAME = ".netbeans-canvas-runner-runtime-v1";
    private static final String GENERATIONS_DIRECTORY = ".netbeans-canvas-runtime-generations-v1";
    private static final String GENERATION_PREFIX = "runtime-";
    private static final String STAGING_PREFIX = ".staging-";
    static final String LEASE_FILE_SUFFIX = ".lease";
    private static final String FORMAT_LINE = "NETBEANS_CANVAS_RUNTIME|1";
    private static final int MAX_MANIFEST_BYTES = 512 * 1024;
    private static final int MAX_RUNTIME_FILES = 4_096;
    private static final int MAX_GENERATION_CACHE_ENTRIES = 1_024;
    private static final int MAX_RUNTIME_DEPTH = 32;
    private static final int MAX_PATH_CHARACTERS = 1_024;
    private static final long MAX_RUNTIME_FILE_BYTES = 512L * 1024 * 1024;
    private static final long MAX_RUNTIME_TOTAL_BYTES = 1024L * 1024 * 1024;
    private CanvasRunnerRuntimeCache() {
    }

    /** Creates and atomically publishes a manifest for a newly-built runtime. */
    static Path commit(
            Path sourceDirectory,
            Path executable,
            String cacheIdentity,
            NativeCanvasRunnerContract runnerContract) throws IOException {
        Objects.requireNonNull(runnerContract, "runnerContract");
        NativeCanvasRunnerContract.RuntimeLayout runtimeLayout =
                runnerContract.runtimeLayout();
        Path sourceRoot = requireSafeDirectory(sourceDirectory, "Canvas runner source cache");
        Path normalizedExecutable = requireInside(
                sourceRoot, executable, "Canvas runner executable");
        if (!runnerContract.buildTarget().executableName().equals(
                normalizedExecutable.getFileName().toString())) {
            throw new IOException("unexpected Canvas runner executable name: "
                    + normalizedExecutable.getFileName());
        }
        Path builtRuntimeRoot = requireSafeDirectory(
                normalizedExecutable.getParent(), "Canvas runner runtime directory");
        requireNoSymlinkComponents(
                sourceRoot, builtRuntimeRoot, "Canvas runner runtime directory");
        RuntimeSnapshot snapshot;
        try {
            snapshot = snapshot(builtRuntimeRoot, runtimeLayout);
        } catch (InvalidCacheException ex) {
            throw new IOException("Flutter created an incomplete Canvas runner runtime: "
                    + ex.getMessage(), ex);
        }
        Path runtimeRoot = publishGeneration(
                sourceRoot, builtRuntimeRoot, snapshot, runtimeLayout);
        String runtimeRelative = portableRelative(sourceRoot, runtimeRoot);
        StringBuilder manifest = new StringBuilder();
        manifest.append(FORMAT_LINE).append('\n');
        manifest.append("identity|").append(requireIdentity(cacheIdentity)).append('\n');
        manifest.append("runtime|").append(runtimeRelative).append('\n');
        snapshot.files().values().forEach(file -> manifest.append("file|")
                .append(file.relativePath()).append('|')
                .append(file.size()).append('|')
                .append(file.sha256()).append('\n'));
        byte[] bytes = manifest.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytes.length > MAX_MANIFEST_BYTES) {
            throw new IOException("Canvas runner runtime manifest exceeds "
                    + MAX_MANIFEST_BYTES + " bytes");
        }
        writeAtomically(sourceRoot.resolve(MANIFEST_NAME), bytes,
                "Canvas runner runtime manifest");
        return runtimeRoot.resolve(runnerContract.buildTarget().executableName())
                .toAbsolutePath().normalize();
    }

    /** Removes old immutable generations only when no process holds their lease. */
    static void pruneUnusedGenerations(Path sourceDirectory, Path keepExecutable)
            throws IOException {
        Path sourceRoot = requireSafeDirectory(sourceDirectory, "Canvas runner source cache");
        Path generations = sourceRoot.resolve(GENERATIONS_DIRECTORY);
        if (!Files.exists(generations, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        requireNoSymlinkComponents(sourceRoot, generations,
                "Canvas runner runtime generations directory");
        requireSafeDirectory(generations, "Canvas runner runtime generations directory");
        Path keepGeneration = requireInside(
                generations,
                Objects.requireNonNull(keepExecutable, "keepExecutable")
                        .toAbsolutePath().normalize().getParent().getParent(),
                "Canvas runner retained runtime generation");
        List<Path> candidates = new ArrayList<>();
        try (var entries = Files.list(generations)) {
            for (Path candidate : entries.limit(MAX_GENERATION_CACHE_ENTRIES + 1L).toList()) {
                if (candidates.size() >= MAX_GENERATION_CACHE_ENTRIES) {
                    throw new IOException(
                            "Canvas runner runtime cache contains too many generations");
                }
                candidates.add(candidate.toAbsolutePath().normalize());
            }
        }
        candidates.sort(Comparator.comparing(Path::toString));
        for (Path generation : candidates) {
            if (generation.equals(keepGeneration)) {
                continue;
            }
            if (!Files.exists(generation, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            String name = generation.getFileName().toString();
            if (name.endsWith(LEASE_FILE_SUFFIX)) {
                if (!Files.isRegularFile(generation, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(generation)) {
                    throw new IOException(
                            "unsafe Canvas runner runtime lease path: " + generation);
                }
                String generationName = name.substring(
                        0, name.length() - LEASE_FILE_SUFFIX.length());
                if (!generationName.startsWith(GENERATION_PREFIX)) {
                    throw new IOException(
                            "unexpected lease path in Canvas runner runtime cache: "
                            + generation);
                }
                if (!Files.exists(
                        generations.resolve(generationName), LinkOption.NOFOLLOW_LINKS)) {
                    deleteSafeFile(generation,
                            "orphaned Canvas runner runtime generation lease");
                }
                continue;
            }
            if (name.startsWith(STAGING_PREFIX)) {
                requireNoSymlinkComponents(generations, generation,
                        "staged Canvas runner runtime generation");
                deleteSafeTree(generation, generations,
                        "abandoned staged Canvas runner runtime generation");
                continue;
            }
            if (!name.startsWith(GENERATION_PREFIX)) {
                throw new IOException(
                        "unexpected path in Canvas runner runtime generations cache: "
                        + generation);
            }
            requireNoSymlinkComponents(generations, generation,
                    "Canvas runner runtime generation");
            requireSafeDirectory(generation, "Canvas runner runtime generation");
            Path leaseFile = generation.resolveSibling(name + LEASE_FILE_SUFFIX);
            requireSafeRegularFile(generations, leaseFile,
                    "Canvas runner runtime generation lease");
            try (CanvasRunnerRuntimeLease.CleanupLease cleanup =
                    CanvasRunnerRuntimeLease.tryAcquireCleanup(generation)) {
                if (cleanup == null) {
                    continue;
                }
                deleteSafeTree(generation, generations,
                        "unused Canvas runner runtime generation");
                cleanup.deleteLeaseFileOnClose();
            }
        }
    }

    private static Path publishGeneration(
            Path sourceRoot,
            Path builtRuntimeRoot,
            RuntimeSnapshot expected,
            NativeCanvasRunnerContract.RuntimeLayout runtimeLayout) throws IOException {
        Path generations = sourceRoot.resolve(GENERATIONS_DIRECTORY);
        if (!Files.exists(generations, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(generations);
        }
        requireNoSymlinkComponents(sourceRoot, generations,
                "Canvas runner runtime generations directory");
        requireSafeDirectory(generations, "Canvas runner runtime generations directory");
        String id = UUID.randomUUID().toString();
        Path staging = generations.resolve(STAGING_PREFIX + id);
        Path generation = generations.resolve(GENERATION_PREFIX + id);
        Path leaseFile = generation.resolveSibling(
                generation.getFileName() + LEASE_FILE_SUFFIX);
        Files.createDirectory(staging);
        boolean published = false;
        try {
            Path runtime = staging.resolve("runtime");
            Files.createDirectory(runtime);
            for (RuntimeFile file : expected.files().values()) {
                Path source = resolvePortableRelative(builtRuntimeRoot, file.relativePath());
                requireSafeRegularFile(
                        builtRuntimeRoot, source, "built Canvas runner runtime file");
                Path target = resolvePortableRelative(runtime, file.relativePath());
                createSafeParentDirectories(runtime, target.getParent());
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            }
            RuntimeSnapshot copied;
            try {
                copied = snapshot(runtime, runtimeLayout);
            } catch (InvalidCacheException exception) {
                throw new IOException("published Canvas runtime failed verification: "
                        + exception.getMessage(), exception);
            }
            if (!expected.files().equals(copied.files())) {
                throw new IOException("published Canvas runtime differs from the verified build");
            }
            Files.write(leaseFile, new byte[] {1},
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
            try {
                Files.move(staging, generation, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(staging, generation);
            }
            published = true;
            Path publishedRuntime = generation.resolve("runtime");
            requireNoSymlinkComponents(sourceRoot, publishedRuntime,
                    "published Canvas runner runtime");
            return requireSafeDirectory(
                    publishedRuntime, "published Canvas runner runtime");
        } finally {
            if (!published && Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
                deleteSafeTree(staging, generations,
                        "staged Canvas runner runtime generation");
            }
            if (!published) {
                deleteSafeFile(leaseFile, "staged Canvas runner runtime lease");
            }
        }
    }

    private static void createSafeParentDirectories(Path root, Path directory)
            throws IOException {
        Path safeDirectory = requireInside(root, directory,
                "published Canvas runner runtime directory");
        Path current = root;
        for (Path segment : root.relativize(safeDirectory)) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(current);
            }
            requireSafeDirectory(current, "published Canvas runner runtime directory");
        }
    }

    /**
     * Returns the verified executable, or {@code null} when an ordinary cache
     * truncation/corruption requires a clean rebuild. Unsafe links and paths are
     * rejected rather than followed.
     */
    static Path validate(
            Path sourceDirectory,
            String cacheIdentity,
            NativeCanvasRunnerContract runnerContract) throws IOException {
        Objects.requireNonNull(runnerContract, "runnerContract");
        NativeCanvasRunnerContract.RuntimeLayout runtimeLayout =
                runnerContract.runtimeLayout();
        Path sourceRoot = requireSafeDirectory(sourceDirectory, "Canvas runner source cache");
        Path manifestPath = sourceRoot.resolve(MANIFEST_NAME);
        rejectSymlink(manifestPath, "Canvas runner runtime manifest");
        if (!Files.exists(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        RuntimeManifest manifest;
        try {
            manifest = parseManifest(
                    readBounded(manifestPath), cacheIdentity, runtimeLayout);
        } catch (InvalidCacheException ex) {
            return null;
        }
        Path runtimeRoot;
        try {
            runtimeRoot = resolvePortableRelative(sourceRoot, manifest.runtimeRelative());
        } catch (InvalidCacheException ex) {
            throw new IOException("unsafe Canvas runner runtime path in cache manifest", ex);
        }
        if (!isPublishedRuntimeLayout(sourceRoot, runtimeRoot)) {
            return null;
        }
        requireNoSymlinkComponents(sourceRoot, runtimeRoot, "Canvas runner runtime directory");
        if (!Files.isDirectory(runtimeRoot, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        RuntimeSnapshot actual;
        try {
            actual = snapshot(runtimeRoot, runtimeLayout);
        } catch (InvalidCacheException ex) {
            return null;
        }
        if (!manifest.files().equals(actual.files())) {
            return null;
        }
        Path executable = runtimeRoot.resolve(
                runnerContract.buildTarget().executableName())
                .toAbsolutePath().normalize();
        try {
            requireSafeRegularFile(runtimeRoot, executable, "Canvas runner executable");
        } catch (InvalidCacheException ex) {
            return null;
        }
        return executable;
    }

    private static boolean isPublishedRuntimeLayout(Path sourceRoot, Path runtimeRoot)
            throws IOException {
        Path generations = sourceRoot.resolve(GENERATIONS_DIRECTORY)
                .toAbsolutePath().normalize();
        Path generation = runtimeRoot.getParent();
        if (generation == null
                || !"runtime".equals(runtimeRoot.getFileName().toString())
                || !generations.equals(generation.getParent())
                || !generation.getFileName().toString().startsWith(GENERATION_PREFIX)) {
            return false;
        }
        requireNoSymlinkComponents(sourceRoot, generation,
                "Canvas runner runtime generation");
        Path leaseFile = generation.resolveSibling(
                generation.getFileName() + LEASE_FILE_SUFFIX);
        rejectSymlink(leaseFile, "Canvas runner runtime generation lease");
        return Files.isRegularFile(leaseFile, LinkOption.NOFOLLOW_LINKS);
    }

    /** Removes only provider-contracted generated build state after cache damage. */
    static void discardIncompleteBuild(
            Path sourceDirectory,
            String buildMarkerName,
            NativeCanvasRunnerContract runnerContract)
            throws IOException {
        Objects.requireNonNull(runnerContract, "runnerContract");
        Path sourceRoot = requireSafeDirectory(sourceDirectory, "Canvas runner source cache");
        deleteSafeFile(sourceRoot.resolve(buildMarkerName), "Canvas runner build marker");
        deleteSafeFile(sourceRoot.resolve(MANIFEST_NAME), "Canvas runner runtime manifest");
        Path buildOutput = runnerContract.buildTarget().outputRoot(sourceRoot);
        requireNoSymlinkComponents(sourceRoot, buildOutput,
                "Canvas runner build output directory");
        if (!Files.exists(buildOutput, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isDirectory(buildOutput, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Canvas runner build output path is not a directory: "
                    + buildOutput);
        }
        Files.walkFileTree(buildOutput, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                rejectSymlink(directory, "cached Canvas runner build directory");
                if (!attributes.isDirectory()) {
                    throw new IOException("non-directory in cached Canvas runner build: " + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                if (attributes.isSymbolicLink() || Files.isSymbolicLink(file)) {
                    throw new IOException("cached Canvas runner build must not contain a symbolic link: "
                            + file);
                }
                if (!attributes.isRegularFile()) {
                    throw new IOException("cached Canvas runner build contains a non-regular file: "
                            + file);
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

    static void writeAtomically(Path target, byte[] bytes, String label) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        Path parent = requireSafeDirectory(target.toAbsolutePath().normalize().getParent(),
                label + " parent");
        Path normalizedTarget = requireInside(parent, target, label);
        rejectSymlink(normalizedTarget, label);
        Path temporary = Files.createTempFile(parent, ".canvas-runner-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, normalizedTarget,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, normalizedTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            rejectSymlink(normalizedTarget, label);
            if (!Files.isRegularFile(normalizedTarget, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(label + " was not published as a regular file");
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static RuntimeSnapshot snapshot(
            Path runtimeRoot,
            NativeCanvasRunnerContract.RuntimeLayout runtimeLayout)
            throws IOException, InvalidCacheException {
        Objects.requireNonNull(runtimeLayout, "runtimeLayout");
        Path safeRoot = requireSafeDirectory(runtimeRoot, "Canvas runner runtime directory");
        Map<String, RuntimeFile> files = new TreeMap<>();
        long[] totalBytes = {0L};
        int[] visited = {0};
        Files.walkFileTree(safeRoot, Set.of(), MAX_RUNTIME_DEPTH, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                if (Files.isSymbolicLink(directory) || attributes.isSymbolicLink()) {
                    throw new IOException("Canvas runner runtime must not contain a symbolic link: "
                            + directory);
                }
                if (!attributes.isDirectory()) {
                    throw new InvalidCacheException("non-directory runtime path: " + directory);
                }
                String relative = portableRelative(safeRoot, directory);
                if (!runtimeLayout.allowsDirectory(relative)) {
                    throw new InvalidCacheException("unexpected runtime directory: " + relative);
                }
                if (++visited[0] > MAX_RUNTIME_FILES) {
                    throw new InvalidCacheException("runtime contains too many paths");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                if (Files.isSymbolicLink(file) || attributes.isSymbolicLink()) {
                    throw new IOException("Canvas runner runtime must not contain a symbolic link: "
                            + file);
                }
                if (!attributes.isRegularFile()) {
                    throw new InvalidCacheException("non-regular runtime file: " + file);
                }
                if (++visited[0] > MAX_RUNTIME_FILES) {
                    throw new InvalidCacheException("runtime contains too many paths");
                }
                String relative = portableRelative(safeRoot, file);
                if (runtimeLayout.ignoresFile(relative)) {
                    return FileVisitResult.CONTINUE;
                }
                if (!runtimeLayout.allowsFile(relative)) {
                    throw new InvalidCacheException("unexpected runtime file: " + relative);
                }
                long size = attributes.size();
                if (size < 0 || size > MAX_RUNTIME_FILE_BYTES) {
                    throw new InvalidCacheException("runtime file has unsafe size: " + relative);
                }
                if (size == 0 && runtimeLayout.requiredFiles().contains(relative)) {
                    throw new InvalidCacheException(
                            "required runtime file is empty: " + relative);
                }
                totalBytes[0] = Math.addExact(totalBytes[0], size);
                if (totalBytes[0] > MAX_RUNTIME_TOTAL_BYTES) {
                    throw new InvalidCacheException("runtime exceeds the total size limit");
                }
                RuntimeFile entry = new RuntimeFile(relative, size, sha256(file, size));
                if (files.put(relative, entry) != null) {
                    throw new InvalidCacheException("duplicate runtime file: " + relative);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException failure)
                    throws IOException {
                throw new IOException("cannot inspect Canvas runner runtime file: " + file, failure);
            }
        });
        if (!files.keySet().containsAll(runtimeLayout.requiredFiles())) {
            Set<String> missing = new java.util.HashSet<>(
                    runtimeLayout.requiredFiles());
            missing.removeAll(files.keySet());
            throw new InvalidCacheException("missing required runtime files: " + missing);
        }
        return new RuntimeSnapshot(Map.copyOf(files));
    }

    private static RuntimeManifest parseManifest(
            byte[] bytes,
            String expectedIdentity,
            NativeCanvasRunnerContract.RuntimeLayout runtimeLayout)
            throws InvalidCacheException {
        Objects.requireNonNull(runtimeLayout, "runtimeLayout");
        String text = new String(bytes, java.nio.charset.StandardCharsets.US_ASCII);
        String[] lines = text.split("\\n", -1);
        if (lines.length < 5 || !lines[lines.length - 1].isEmpty()) {
            throw new InvalidCacheException("runtime manifest is truncated");
        }
        if (!FORMAT_LINE.equals(lines[0])) {
            throw new InvalidCacheException("unsupported runtime manifest format");
        }
        if (!lines[1].equals("identity|" + requireIdentity(expectedIdentity))) {
            throw new InvalidCacheException("runtime manifest belongs to another SDK identity");
        }
        if (!lines[2].startsWith("runtime|")) {
            throw new InvalidCacheException("runtime manifest has no runtime directory");
        }
        String runtimeRelative = validatePortableRelative(lines[2].substring("runtime|".length()));
        Map<String, RuntimeFile> files = new TreeMap<>();
        long totalBytes = 0L;
        for (int index = 3; index < lines.length - 1; index++) {
            String[] fields = lines[index].split("\\|", -1);
            if (fields.length != 4 || !"file".equals(fields[0])) {
                throw new InvalidCacheException("malformed runtime manifest entry");
            }
            if (files.size() >= MAX_RUNTIME_FILES) {
                throw new InvalidCacheException("runtime manifest contains too many files");
            }
            String relative = validatePortableRelative(fields[1]);
            if (!runtimeLayout.allowsFile(relative)) {
                throw new InvalidCacheException("runtime manifest contains a non-runtime path");
            }
            long size;
            try {
                size = Long.parseLong(fields[2]);
            } catch (NumberFormatException ex) {
                throw new InvalidCacheException("invalid runtime file size", ex);
            }
            if (size < 0 || size > MAX_RUNTIME_FILE_BYTES) {
                throw new InvalidCacheException("runtime manifest file exceeds its size limit");
            }
            if (size == 0 && runtimeLayout.requiredFiles().contains(relative)) {
                throw new InvalidCacheException(
                        "runtime manifest contains an empty required file: " + relative);
            }
            try {
                totalBytes = Math.addExact(totalBytes, size);
            } catch (ArithmeticException ex) {
                throw new InvalidCacheException("runtime manifest total size overflow", ex);
            }
            if (totalBytes > MAX_RUNTIME_TOTAL_BYTES) {
                throw new InvalidCacheException("runtime manifest exceeds its total size limit");
            }
            if (!fields[3].matches("[0-9a-f]{64}")) {
                throw new InvalidCacheException("invalid runtime file SHA-256");
            }
            RuntimeFile entry = new RuntimeFile(relative, size, fields[3]);
            if (files.put(relative, entry) != null) {
                throw new InvalidCacheException("duplicate runtime manifest file: " + relative);
            }
        }
        if (!files.keySet().containsAll(runtimeLayout.requiredFiles())) {
            throw new InvalidCacheException("runtime manifest omits required files");
        }
        return new RuntimeManifest(runtimeRelative, Map.copyOf(files));
    }

    private static byte[] readBounded(Path file) throws IOException, InvalidCacheException {
        long declaredSize = Files.size(file);
        if (declaredSize < 1 || declaredSize > MAX_MANIFEST_BYTES) {
            throw new InvalidCacheException("runtime manifest has an unsafe size");
        }
        try (InputStream input = Files.newInputStream(file,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
                ByteArrayOutputStream output = new ByteArrayOutputStream((int) declaredSize)) {
            byte[] buffer = new byte[8_192];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) {
                    continue;
                }
                total = Math.addExact(total, count);
                if (total > MAX_MANIFEST_BYTES) {
                    throw new InvalidCacheException("runtime manifest exceeds its size limit");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static String sha256(Path file, long expectedSize)
            throws IOException, InvalidCacheException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError("SHA-256 is required by the Java platform", ex);
        }
        long total = 0L;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(file,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) {
                    continue;
                }
                total = Math.addExact(total, count);
                if (total > expectedSize || total > MAX_RUNTIME_FILE_BYTES) {
                    throw new InvalidCacheException("runtime file changed while hashing: " + file);
                }
                digest.update(buffer, 0, count);
            }
        }
        if (total != expectedSize) {
            throw new InvalidCacheException("runtime file changed while hashing: " + file);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String portableRelative(Path parent, Path child) throws IOException {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        Path normalizedChild = requireInside(normalizedParent, child, "Canvas runner cache path");
        String value = normalizedParent.relativize(normalizedChild).toString().replace('\\', '/');
        if (value.length() > MAX_PATH_CHARACTERS || value.indexOf('|') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IOException("Canvas runner cache path cannot be represented safely");
        }
        return value;
    }

    private static String validatePortableRelative(String value) throws InvalidCacheException {
        if (value.isEmpty() || value.length() > MAX_PATH_CHARACTERS
                || value.startsWith("/") || value.startsWith("\\")
                || value.indexOf('\\') >= 0 || value.indexOf('|') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new InvalidCacheException("unsafe relative path in runtime manifest");
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new InvalidCacheException("unsafe relative path in runtime manifest");
            }
        }
        return value;
    }

    private static Path resolvePortableRelative(Path root, String value)
            throws InvalidCacheException, IOException {
        String relative = validatePortableRelative(value);
        Path resolved = root;
        for (String segment : relative.split("/")) {
            resolved = resolved.resolve(segment);
        }
        return requireInside(root, resolved, "Canvas runner runtime directory");
    }

    private static Path requireSafeDirectory(Path path, String label) throws IOException {
        Path normalized = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        rejectSymlink(normalized, label);
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a safe directory: " + normalized);
        }
        return normalized;
    }

    private static void requireSafeRegularFile(
            Path root, Path file, String label) throws IOException, InvalidCacheException {
        Path normalized = requireInside(root, file, label);
        requireNoSymlinkComponents(root, normalized, label);
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new InvalidCacheException(label + " is missing or non-regular");
        }
    }

    private static Path requireInside(Path root, Path candidate, String label) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new IOException(label + " escaped its cache directory");
        }
        return normalizedCandidate;
    }

    private static void requireNoSymlinkComponents(Path root, Path target, String label)
            throws IOException {
        Path safeTarget = requireInside(root, target, label);
        Path current = root.toAbsolutePath().normalize();
        rejectSymlink(current, label);
        for (Path segment : current.relativize(safeTarget)) {
            current = current.resolve(segment);
            rejectSymlink(current, label);
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

    private static void deleteSafeFile(Path path, String label) throws IOException {
        rejectSymlink(path, label);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a regular file: " + path);
        }
        Files.delete(path);
    }

    private static void deleteSafeTree(Path tree, Path expectedParent, String label)
            throws IOException {
        Path root = tree.toAbsolutePath().normalize();
        Path parent = expectedParent.toAbsolutePath().normalize();
        if (!parent.equals(root.getParent())) {
            throw new IOException("refusing to delete " + label + " outside its parent");
        }
        rejectSymlink(root, label);
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        requireSafeDirectory(root, label);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory, BasicFileAttributes attributes) throws IOException {
                rejectSymlink(directory, label);
                if (!attributes.isDirectory() || attributes.isOther()) {
                    throw new IOException(label + " contains an unsafe directory: " + directory);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                rejectSymlink(file, label);
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

    private static String requireIdentity(String value) {
        Objects.requireNonNull(value, "cacheIdentity");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("cache identity must be a lowercase SHA-256 digest");
        }
        return value;
    }

    private record RuntimeSnapshot(Map<String, RuntimeFile> files) {
    }

    private record RuntimeManifest(String runtimeRelative, Map<String, RuntimeFile> files) {
    }

    private record RuntimeFile(String relativePath, long size, String sha256) {
    }

    private static final class InvalidCacheException extends IOException {
        private InvalidCacheException(String message) {
            super(message);
        }

        private InvalidCacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
