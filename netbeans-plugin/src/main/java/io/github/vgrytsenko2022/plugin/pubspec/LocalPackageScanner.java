package io.github.vgrytsenko2022.plugin.pubspec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;

/** Finds local Dart packages without following links or scanning outside the project root. */
public final class LocalPackageScanner {
    static final int DEFAULT_MAX_DEPTH = 8;
    static final int DEFAULT_MAX_DIRECTORIES = 512;

    private static final List<String> EXCLUDED_DIRECTORIES = List.of(
            ".dart_tool", ".git", "build");

    private final int maxDepth;
    private final int maxDirectories;

    public LocalPackageScanner() {
        this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_DIRECTORIES);
    }

    LocalPackageScanner(int maxDepth, int maxDirectories) {
        if (maxDepth < 1 || maxDirectories < 1) {
            throw new IllegalArgumentException("Scan bounds must be positive.");
        }
        this.maxDepth = maxDepth;
        this.maxDirectories = maxDirectories;
    }

    public List<LocalPackage> scan(
            Path searchRoot,
            Path currentPackageRoot,
            BooleanSupplier cancelled) {
        Objects.requireNonNull(cancelled, "cancelled");
        if (searchRoot == null || currentPackageRoot == null || cancelled.getAsBoolean()) {
            return List.of();
        }
        Path normalizedRoot = searchRoot.toAbsolutePath().normalize();
        Path normalizedCurrent = currentPackageRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            return List.of();
        }

        Map<String, LocalPackage> byName = new LinkedHashMap<>();
        int[] visitedDirectories = {0};
        try {
            Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) {
                    if (cancelled.getAsBoolean()) {
                        return FileVisitResult.TERMINATE;
                    }
                    Path relative = normalizedRoot.relativize(directory);
                    if (relative.getNameCount() > maxDepth) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!directory.equals(normalizedRoot)
                            && EXCLUDED_DIRECTORIES.contains(directory.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (++visitedDirectories[0] > maxDirectories) {
                        return FileVisitResult.TERMINATE;
                    }
                    inspectPackage(directory, normalizedCurrent, byName, cancelled);
                    return cancelled.getAsBoolean()
                            ? FileVisitResult.TERMINATE
                            : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ex) {
                    return cancelled.getAsBoolean()
                            ? FileVisitResult.TERMINATE
                            : FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException ex) {
            // Completion is best-effort. An unreadable package must not break the editor query.
        }
        if (cancelled.getAsBoolean()) {
            return List.of();
        }
        return byName.values().stream()
                .sorted(Comparator.comparing(LocalPackage::name)
                        .thenComparing(LocalPackage::relativePath))
                .toList();
    }

    private static void inspectPackage(
            Path directory,
            Path currentPackageRoot,
            Map<String, LocalPackage> byName,
            BooleanSupplier cancelled) {
        if (directory.equals(currentPackageRoot) || cancelled.getAsBoolean()) {
            return;
        }
        Path pubspec = directory.resolve(PubspecFiles.FILE_NAME);
        try {
            if (!Files.isRegularFile(pubspec)
                    || Files.size(pubspec) > PubspecValidator.MAX_DOCUMENT_LENGTH) {
                return;
            }
            String source = Files.readString(pubspec, StandardCharsets.UTF_8);
            if (cancelled.getAsBoolean()) {
                return;
            }
            Node root = PubspecYaml.compose(source).orElse(null);
            if (!(root instanceof MappingNode mapping)) {
                return;
            }
            PubspecYaml.Entry nameEntry = PubspecYaml.entries(mapping).get("name");
            String name = nameEntry == null ? null : PubspecYaml.scalar(nameEntry.value());
            if (!PubspecValidator.isValidPackageName(name)) {
                return;
            }
            Path relative = currentPackageRoot.relativize(directory.toAbsolutePath().normalize());
            String relativePath = toPortablePath(relative);
            LocalPackage candidate = new LocalPackage(name, relativePath);
            byName.merge(name, candidate, LocalPackageScanner::preferShorterPath);
        } catch (IOException | RuntimeException ex) {
            // Ignore malformed or concurrently removed candidate packages.
        }
    }

    private static LocalPackage preferShorterPath(LocalPackage first, LocalPackage second) {
        int firstParts = Path.of(first.relativePath()).getNameCount();
        int secondParts = Path.of(second.relativePath()).getNameCount();
        if (firstParts != secondParts) {
            return firstParts < secondParts ? first : second;
        }
        return first.relativePath().compareTo(second.relativePath()) <= 0 ? first : second;
    }

    private static String toPortablePath(Path path) {
        List<String> parts = new ArrayList<>();
        for (Path part : path) {
            parts.add(part.toString());
        }
        String joined = String.join("/", parts);
        return joined.isEmpty() ? "." : joined;
    }

    public record LocalPackage(String name, String relativePath) {
        public LocalPackage {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(relativePath, "relativePath");
        }
    }
}
