package dev.flutter.netbeans.plugin.designer.canvas.spi;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable provider-owned build, runtime-artifact and cache contract for one
 * isolated native Canvas runner.
 */
public record NativeCanvasRunnerContract(
        NativeCanvasPlatform platform,
        BuildTarget buildTarget,
        RuntimeLayout runtimeLayout,
        CachePolicy cachePolicy) {
    private static final int MAX_VALUE_CHARACTERS = 512;
    private static final int MAX_BUILD_OPTIONS = 32;
    private static final int MAX_RUNTIME_PATHS = 8_192;

    public NativeCanvasRunnerContract {
        platform = Objects.requireNonNull(platform, "platform");
        buildTarget = Objects.requireNonNull(buildTarget, "buildTarget");
        runtimeLayout = Objects.requireNonNull(runtimeLayout, "runtimeLayout");
        cachePolicy = Objects.requireNonNull(cachePolicy, "cachePolicy");
        if (platform == NativeCanvasPlatform.UNKNOWN) {
            throw new IllegalArgumentException(
                    "a native Canvas runner contract requires a concrete platform");
        }
        if (!runtimeLayout.requiredFiles().contains(buildTarget.executableName())) {
            throw new IllegalArgumentException(
                    "the runner executable must be a required runtime file");
        }
    }

    /** Canonical fingerprint of every field that can affect build or reuse. */
    public String fingerprint() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java platform", exception);
        }
        update(digest, "platform", platform.name());
        update(digest, "build.target", buildTarget.flutterTarget());
        update(digest, "build.mode", buildTarget.buildMode());
        update(digest, "build.output", buildTarget.outputDirectory());
        update(digest, "build.executable", buildTarget.executableName());
        update(digest, "build.search-depth",
                Integer.toString(buildTarget.executableSearchDepth()));
        updateList(digest, "build.option", buildTarget.buildOptions());
        updateSet(digest, "runtime.required", runtimeLayout.requiredFiles());
        updateSet(digest, "runtime.allowed-file", runtimeLayout.allowedFiles());
        updateSet(digest, "runtime.allowed-file-prefix",
                runtimeLayout.allowedFilePrefixes());
        updateSet(digest, "runtime.allowed-directory",
                runtimeLayout.allowedDirectories());
        updateSet(digest, "runtime.allowed-directory-prefix",
                runtimeLayout.allowedDirectoryPrefixes());
        updateSet(digest, "runtime.ignored", runtimeLayout.ignoredFiles());
        update(digest, "cache.root", cachePolicy.cacheRoot().name());
        update(digest, "cache.directory", cachePolicy.directoryName());
        update(digest, "cache.identity", cachePolicy.identity());
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Exact Flutter build invocation and verified executable search boundary. */
    public record BuildTarget(
            String flutterTarget,
            String buildMode,
            List<String> buildOptions,
            String outputDirectory,
            String executableName,
            int executableSearchDepth) {
        public BuildTarget {
            flutterTarget = requireToken(flutterTarget, "Flutter build target");
            buildMode = requireToken(buildMode, "Flutter build mode");
            buildOptions = immutableBuildOptions(buildOptions);
            outputDirectory = requirePortablePath(
                    outputDirectory, "Flutter build output directory", false);
            executableName = requireSingleSegment(
                    executableName, "Canvas runner executable name");
            if (executableSearchDepth < 1 || executableSearchDepth > 32) {
                throw new IllegalArgumentException(
                        "Canvas runner executable search depth must be between 1 and 32");
            }
        }

        public List<String> command(Path flutterExecutable) {
            Path executable = requireAbsolutePath(
                    flutterExecutable, "Flutter executable");
            List<String> command = new ArrayList<>(4 + buildOptions.size());
            command.add(executable.toString());
            command.add("build");
            command.add(flutterTarget);
            command.add("--" + buildMode);
            command.addAll(buildOptions);
            return List.copyOf(command);
        }

        public Path outputRoot(Path sourceDirectory) {
            Path source = requireAbsolutePath(
                    sourceDirectory, "Canvas runner source directory");
            return resolvePortable(source, outputDirectory);
        }
    }

    /** Closed required/allowed runtime file and directory surface. */
    public record RuntimeLayout(
            Set<String> requiredFiles,
            Set<String> allowedFiles,
            Set<String> allowedFilePrefixes,
            Set<String> allowedDirectories,
            Set<String> allowedDirectoryPrefixes,
            Set<String> ignoredFiles) {
        public RuntimeLayout {
            requiredFiles = immutablePaths(
                    requiredFiles, "required runtime file", false, false);
            allowedFiles = immutablePaths(
                    allowedFiles, "allowed runtime file", false, false);
            allowedFilePrefixes = immutablePaths(
                    allowedFilePrefixes, "allowed runtime file prefix", false, true);
            allowedDirectories = immutablePaths(
                    allowedDirectories, "allowed runtime directory", true, false);
            allowedDirectoryPrefixes = immutablePaths(
                    allowedDirectoryPrefixes,
                    "allowed runtime directory prefix", false, true);
            ignoredFiles = immutablePaths(
                    ignoredFiles, "ignored runtime file", false, false);
            int paths = requiredFiles.size() + allowedFiles.size()
                    + allowedFilePrefixes.size() + allowedDirectories.size()
                    + allowedDirectoryPrefixes.size() + ignoredFiles.size();
            if (paths > MAX_RUNTIME_PATHS) {
                throw new IllegalArgumentException(
                        "native Canvas runtime contract contains too many paths");
            }
            if (requiredFiles.isEmpty()) {
                throw new IllegalArgumentException(
                        "native Canvas runtime contract requires at least one file");
            }
            if (!allowedDirectories.contains("")) {
                throw new IllegalArgumentException(
                        "native Canvas runtime contract must allow its root directory");
            }
            for (String required : requiredFiles) {
                if (!NativeCanvasRunnerContract.allowsFile(
                        required, allowedFiles, allowedFilePrefixes)) {
                    throw new IllegalArgumentException(
                            "required runtime file is not allowed: " + required);
                }
                if (ignoredFiles.contains(required)) {
                    throw new IllegalArgumentException(
                            "required runtime file cannot be ignored: " + required);
                }
            }
            for (String allowed : allowedFiles) {
                requireAllowedParents(allowed, allowedDirectories,
                        allowedDirectoryPrefixes);
            }
            for (String prefix : allowedFilePrefixes) {
                requireAllowedDirectoryAndParents(
                        prefix.substring(0, prefix.length() - 1),
                        allowedDirectories, allowedDirectoryPrefixes);
            }
            for (String prefix : allowedDirectoryPrefixes) {
                requireAllowedDirectoryAndParents(
                        prefix.substring(0, prefix.length() - 1),
                        allowedDirectories, allowedDirectoryPrefixes);
            }
        }

        public boolean allowsFile(String relativePath) {
            try {
                String path = requirePortablePath(
                        relativePath, "runtime file", false);
                return NativeCanvasRunnerContract.allowsFile(
                        path, allowedFiles, allowedFilePrefixes);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
        }

        public boolean allowsDirectory(String relativePath) {
            try {
                String path = requirePortablePath(
                        relativePath, "runtime directory", true);
                return allowedDirectories.contains(path)
                        || allowedDirectoryPrefixes.stream().anyMatch(path::startsWith);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
        }

        public boolean ignoresFile(String relativePath) {
            try {
                return ignoredFiles.contains(requirePortablePath(
                        relativePath, "ignored runtime file", false));
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
        }
    }

    /** Cache namespace and the exact identity material included in the SDK key. */
    public record CachePolicy(
            CacheRoot cacheRoot,
            String directoryName,
            String identity) {
        public CachePolicy {
            cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
            directoryName = requireSingleSegment(
                    directoryName, "Canvas runner cache directory name");
            identity = requireBoundedValue(identity, "Canvas runner cache identity");
        }

        public Path resolveRoot(
                Path netBeansCacheRoot,
                Path userTemporaryDirectory) {
            Path netBeans = requireAbsolutePath(
                    netBeansCacheRoot, "NetBeans Canvas runner cache root");
            Path temporary = requireAbsolutePath(
                    userTemporaryDirectory, "user temporary directory");
            return switch (cacheRoot) {
                case NETBEANS_CACHE -> netBeans;
                case USER_TEMPORARY -> temporary.resolve(directoryName)
                        .toAbsolutePath().normalize();
            };
        }
    }

    public enum CacheRoot {
        NETBEANS_CACHE,
        USER_TEMPORARY
    }

    private static List<String> immutableBuildOptions(List<String> values) {
        Objects.requireNonNull(values, "buildOptions");
        if (values.size() > MAX_BUILD_OPTIONS) {
            throw new IllegalArgumentException("too many Flutter build options");
        }
        List<String> copy = new ArrayList<>(values.size());
        Set<String> unique = new HashSet<>();
        for (String value : values) {
            String option = requireBoundedValue(value, "Flutter build option");
            if (!option.matches("--[a-z0-9][a-z0-9-]*(?:=[A-Za-z0-9._/-]+)?")
                    || hasUnsafeOptionPath(option)
                    || !unique.add(option)) {
                throw new IllegalArgumentException(
                        "Flutter build options must be unique bounded long options");
            }
            copy.add(option);
        }
        return List.copyOf(copy);
    }

    private static Set<String> immutablePaths(
            Set<String> values,
            String label,
            boolean allowEmpty,
            boolean prefix) {
        Objects.requireNonNull(values, label + "s");
        TreeSet<String> copy = new TreeSet<>();
        for (String value : values) {
            String path = requirePortablePath(value, label, allowEmpty);
            if (prefix && !path.endsWith("/")) {
                throw new IllegalArgumentException(label + " must end with '/'");
            }
            if (!prefix && path.endsWith("/")) {
                throw new IllegalArgumentException(label + " must not end with '/'");
            }
            copy.add(path);
        }
        return Set.copyOf(copy);
    }

    private static void requireAllowedParents(
            String file,
            Set<String> directories,
            Set<String> directoryPrefixes) {
        int slash = file.lastIndexOf('/');
        String parent = slash < 0 ? "" : file.substring(0, slash);
        while (true) {
            boolean allowed = directories.contains(parent)
                    || directoryPrefixes.stream().anyMatch(parent::startsWith);
            if (!allowed) {
                throw new IllegalArgumentException(
                        "runtime file has a directory outside the allowlist: " + file);
            }
            if (parent.isEmpty()) {
                return;
            }
            slash = parent.lastIndexOf('/');
            parent = slash < 0 ? "" : parent.substring(0, slash);
        }
    }

    private static void requireAllowedDirectoryAndParents(
            String directory,
            Set<String> directories,
            Set<String> directoryPrefixes) {
        String current = directory;
        while (true) {
            boolean allowed = directories.contains(current)
                    || directoryPrefixes.stream().anyMatch(current::startsWith);
            if (!allowed) {
                throw new IllegalArgumentException(
                        "runtime prefix has a directory outside the allowlist: "
                        + directory);
            }
            if (current.isEmpty()) {
                return;
            }
            int slash = current.lastIndexOf('/');
            current = slash < 0 ? "" : current.substring(0, slash);
        }
    }

    private static boolean allowsFile(
            String path,
            Set<String> allowedFiles,
            Set<String> allowedFilePrefixes) {
        return allowedFiles.contains(path)
                || allowedFilePrefixes.stream().anyMatch(path::startsWith);
    }

    private static String requireToken(String value, String label) {
        String token = requireBoundedValue(value, label);
        if (!token.matches("[a-z0-9][a-z0-9_-]*")) {
            throw new IllegalArgumentException(label + " is not a safe Flutter token");
        }
        return token;
    }

    private static String requireSingleSegment(String value, String label) {
        String segment = requirePortablePath(
                requireBoundedValue(value, label), label, false);
        if (segment.indexOf('/') >= 0) {
            throw new IllegalArgumentException(label + " must be one safe path segment");
        }
        return segment;
    }

    private static String requirePortablePath(
            String value,
            String label,
            boolean allowEmpty) {
        Objects.requireNonNull(value, label);
        if ((!allowEmpty && value.isEmpty()) || value.length() > MAX_VALUE_CHARACTERS
                || value.startsWith("/") || value.startsWith("\\")
                || value.indexOf('\\') >= 0 || value.indexOf('|') >= 0
                || value.indexOf('\0') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(label + " is not a safe portable path");
        }
        if (value.isEmpty()) {
            return value;
        }
        String inspected = value.endsWith("/")
                ? value.substring(0, value.length() - 1) : value;
        for (String segment : inspected.split("/", -1)) {
            if (!safePortableSegment(segment)) {
                throw new IllegalArgumentException(label + " is not a safe portable path");
            }
        }
        return value;
    }

    private static String requireBoundedValue(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank() || value.length() > MAX_VALUE_CHARACTERS
                || value.indexOf('\0') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return value;
    }

    private static Path requireAbsolutePath(Path value, String label) {
        Path path = Objects.requireNonNull(value, label).normalize();
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(label + " must be absolute");
        }
        return path;
    }

    private static Path resolvePortable(Path root, String relative) {
        Path result = root;
        for (String segment : relative.split("/")) {
            result = result.resolve(segment);
        }
        Path normalized = result.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("portable path escaped its root");
        }
        return normalized;
    }

    private static boolean hasUnsafeOptionPath(String option) {
        int equals = option.indexOf('=');
        if (equals < 0) {
            return false;
        }
        String value = option.substring(equals + 1);
        if (value.startsWith("/") || value.endsWith("/")) {
            return true;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..") || segment.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean safePortableSegment(String segment) {
        if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                || segment.endsWith(".") || segment.endsWith(" ")) {
            return false;
        }
        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (character < 0x20 || character == 0x7f
                    || ":*?<>\"\\|".indexOf(character) >= 0) {
                return false;
            }
        }
        String base = segment;
        int dot = base.indexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        String upper = base.toUpperCase(java.util.Locale.ROOT);
        return !upper.equals("CON") && !upper.equals("PRN")
                && !upper.equals("AUX") && !upper.equals("NUL")
                && !upper.matches("COM[1-9]") && !upper.matches("LPT[1-9]");
    }

    private static void updateList(
            MessageDigest digest,
            String name,
            List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            update(digest, name + '[' + index + ']', values.get(index));
        }
    }

    private static void updateSet(
            MessageDigest digest,
            String name,
            Set<String> values) {
        List<String> sorted = values.stream().sorted().toList();
        updateList(digest, name, sorted);
    }

    private static void update(
            MessageDigest digest,
            String name,
            String value) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(java.nio.ByteBuffer.allocate(Integer.BYTES)
                .putInt(nameBytes.length).array());
        digest.update(nameBytes);
        digest.update(java.nio.ByteBuffer.allocate(Integer.BYTES)
                .putInt(valueBytes.length).array());
        digest.update(valueBytes);
    }
}
