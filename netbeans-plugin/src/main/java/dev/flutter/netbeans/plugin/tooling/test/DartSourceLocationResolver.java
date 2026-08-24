package dev.flutter.netbeans.plugin.tooling.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.plugin.tooling.DartSourceLocation;
import dev.flutter.netbeans.run.FlutterTestCase;
import dev.flutter.netbeans.run.FlutterTestSuite;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves Dart test metadata and stack frames to files on disk. */
public final class DartSourceLocationResolver {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern STACK_LOCATION = Pattern.compile(
            "(?<source>package:[^\\s()]+\\.dart"
            + "|file:[^\\s()]+\\.dart"
            + "|[A-Za-z]:[\\\\/][^()\\r\\n]*?\\.dart"
            + "|(?<!\\S)[^()\\s]+\\.dart)"
            + "(?::|\\s+)(?<line>\\d+):(?<column>\\d+)");
    private static final Pattern WINDOWS_ABSOLUTE = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    private final Path projectRoot;
    private volatile Map<String, Path> packageRoots;

    public DartSourceLocationResolver(Path projectRoot) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
    }

    public Optional<DartSourceLocation> resolve(
            FlutterTestCase test,
            FlutterTestSuite suite) {
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(suite, "suite");
        int line = test.line().orElse(1);
        int column = test.column().orElse(1);
        if (test.url().isPresent()) {
            Optional<DartSourceLocation> location = resolve(
                    test.url().get(), line, column);
            if (location.isPresent()) {
                return location;
            }
        }
        return suite.path().flatMap(path -> resolve(path, line, column));
    }

    public Optional<DartSourceLocation> resolveStackFrame(String frame) {
        if (frame == null || frame.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = STACK_LOCATION.matcher(frame);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return resolve(
                    matcher.group("source"),
                    Integer.parseInt(matcher.group("line")),
                    Integer.parseInt(matcher.group("column")));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public Optional<DartSourceLocation> resolve(String source, int line, int column) {
        if (source == null || source.isBlank() || line < 1 || column < 1) {
            return Optional.empty();
        }
        Optional<Path> path = resolvePath(source.strip());
        if (path.isEmpty() || !Files.isRegularFile(path.get())) {
            return Optional.empty();
        }
        return Optional.of(new DartSourceLocation(
                projectRoot,
                path.get().toString(),
                line,
                column));
    }

    public Optional<String> projectRelativePath(
            FlutterTestCase test,
            FlutterTestSuite suite) {
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(suite, "suite");
        if (suite.path().isPresent()) {
            Optional<String> relative = projectRelativePath(suite.path().get());
            if (relative.isPresent()) {
                return relative;
            }
        }
        if (test.url().isPresent()) {
            return projectRelativePath(test.url().get());
        }
        return Optional.empty();
    }

    public Optional<String> projectRelativePath(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        return resolvePath(source.strip())
                .filter(path -> path.startsWith(projectRoot))
                .map(projectRoot::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/'));
    }

    private Optional<Path> resolvePath(String source) {
        try {
            if (source.startsWith("package:")) {
                return resolvePackageUri(source);
            }
            if (source.startsWith("file:")) {
                return Optional.of(Path.of(new URI(source)).toAbsolutePath().normalize());
            }
            Path path;
            if (WINDOWS_ABSOLUTE.matcher(source).matches()) {
                path = Path.of(source.replace('/', java.io.File.separatorChar));
            } else {
                path = Path.of(source);
                if (!path.isAbsolute()) {
                    path = projectRoot.resolve(path);
                    Path normalized = path.toAbsolutePath().normalize();
                    return normalized.startsWith(projectRoot)
                            ? Optional.of(normalized)
                            : Optional.empty();
                }
            }
            return Optional.of(path.toAbsolutePath().normalize());
        } catch (IllegalArgumentException | URISyntaxException ex) {
            return Optional.empty();
        }
    }

    private Optional<Path> resolvePackageUri(String source) {
        String value = source.substring("package:".length());
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) {
            return Optional.empty();
        }
        Path root = packageRoots().get(value.substring(0, slash));
        if (root == null) {
            return Optional.empty();
        }
        try {
            URI relative = new URI(null, null, value.substring(slash + 1), null);
            Path resolved = root.resolve(relative.getPath()).toAbsolutePath().normalize();
            return resolved.startsWith(root.toAbsolutePath().normalize())
                    ? Optional.of(resolved)
                    : Optional.empty();
        } catch (URISyntaxException | InvalidPathException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Path> packageRoots() {
        Map<String, Path> result = packageRoots;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            if (packageRoots == null) {
                packageRoots = loadPackageRoots();
            }
            return packageRoots;
        }
    }

    private Map<String, Path> loadPackageRoots() {
        Path config = projectRoot.resolve(".dart_tool/package_config.json");
        if (!Files.isRegularFile(config)) {
            return Map.of();
        }
        try {
            JsonNode root = JSON.readTree(config.toFile());
            JsonNode packages = root.path("packages");
            if (!packages.isArray()) {
                return Map.of();
            }
            Map<String, Path> result = new LinkedHashMap<>();
            URI configDirectory = config.getParent().toUri();
            for (JsonNode entry : packages) {
                String name = entry.path("name").asText("").strip();
                String rootUri = entry.path("rootUri").asText("").strip();
                String packageUri = entry.path("packageUri").asText("").strip();
                if (name.isEmpty() || rootUri.isEmpty()) {
                    continue;
                }
                URI resolvedRoot = configDirectory.resolve(rootUri);
                if (!"file".equalsIgnoreCase(resolvedRoot.getScheme())) {
                    continue;
                }
                Path packageRoot = Path.of(resolvedRoot).toAbsolutePath().normalize();
                if (!packageUri.isEmpty()) {
                    String packagePath = new URI(packageUri).getPath();
                    if (packagePath == null) {
                        continue;
                    }
                    packageRoot = packageRoot.resolve(packagePath).normalize();
                }
                result.put(name, packageRoot);
            }
            return Map.copyOf(result);
        } catch (IOException | IllegalArgumentException | URISyntaxException ex) {
            return Map.of();
        }
    }
}
