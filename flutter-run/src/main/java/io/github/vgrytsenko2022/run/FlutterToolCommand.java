package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.FlutterDevice;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable command line for a supported one-shot Flutter project operation.
 *
 * <p>The executable and working directory deliberately are not part of this
 * value. The IDE resolves both from the owning project and configured SDK.</p>
 */
public final class FlutterToolCommand {
    private static final List<String> CLEAN_ARGUMENTS = List.of("clean");
    private static final List<String> PUB_GET_ARGUMENTS = List.of("pub", "get");
    private static final List<String> ANALYZE_ARGUMENTS =
            List.of("analyze", "--no-pub", "--no-congratulate");
    private static final List<String> TEST_ARGUMENTS =
            List.of("test", "--no-pub", "--reporter=json");

    private final FlutterToolCommandType type;
    private final List<String> arguments;

    private FlutterToolCommand(FlutterToolCommandType type, List<String> arguments) {
        this.type = Objects.requireNonNull(type, "type");
        this.arguments = List.copyOf(arguments);
    }

    public static FlutterToolCommand clean() {
        return new FlutterToolCommand(FlutterToolCommandType.CLEAN, CLEAN_ARGUMENTS);
    }

    /** Builds the artifact matching a connected Flutter target's platform. */
    public static FlutterToolCommand build(FlutterDevice device) {
        return new FlutterToolCommand(
                FlutterToolCommandType.BUILD,
                List.of("build", buildTarget(device)));
    }

    public static FlutterToolCommand pubGet() {
        return new FlutterToolCommand(FlutterToolCommandType.PUB_GET, PUB_GET_ARGUMENTS);
    }

    /** Adds canonical platform scaffolding to the current Flutter project. */
    public static FlutterToolCommand addPlatforms(String platformsArgument) {
        if (platformsArgument == null || platformsArgument.isBlank()) {
            throw new IllegalArgumentException("Flutter project platforms are required");
        }
        String value = platformsArgument.strip();
        List<String> platforms = List.of(value.split(",", -1));
        List<String> canonical = List.of(
                "android", "ios", "web", "windows", "macos", "linux");
        Set<String> selected = Set.copyOf(platforms);
        if (selected.size() != platforms.size()
                || platforms.stream().anyMatch(platform -> !canonical.contains(platform))
                || !canonical.stream().filter(selected::contains).toList().equals(platforms)) {
            throw new IllegalArgumentException(
                    "Flutter project platforms must be unique canonical ids in stable order");
        }
        return new FlutterToolCommand(
                FlutterToolCommandType.ADD_PLATFORMS,
                List.of("create", "--platforms=" + value, "."));
    }

    public static FlutterToolCommand analyze() {
        return new FlutterToolCommand(FlutterToolCommandType.ANALYZE, ANALYZE_ARGUMENTS);
    }

    public static FlutterToolCommand test() {
        return new FlutterToolCommand(FlutterToolCommandType.TEST, TEST_ARGUMENTS);
    }

    /** Runs all tests below a project-relative file or directory. */
    public static FlutterToolCommand test(String projectRelativePath) {
        String path = normalizeRelativePath(projectRelativePath);
        return new FlutterToolCommand(
                FlutterToolCommandType.TEST,
                List.of("test", "--no-pub", "--reporter=json", path));
    }

    /** Runs the named test from a project-relative test file. */
    public static FlutterToolCommand test(String projectRelativePath, String plainName) {
        String path = normalizeRelativePath(projectRelativePath);
        if (plainName == null || plainName.isBlank()) {
            throw new IllegalArgumentException("Flutter test name is required");
        }
        return new FlutterToolCommand(
                FlutterToolCommandType.TEST,
                List.of("test", "--no-pub", "--reporter=json", path, "--plain-name", plainName));
    }

    public FlutterToolCommandType type() {
        return type;
    }

    public List<String> arguments() {
        return arguments;
    }

    private static String buildTarget(FlutterDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Flutter build device is required");
        }
        String id = normalize(device.id());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Flutter build device id is required");
        }
        String platform = normalize(device.platform());
        if (platform.startsWith("windows")) {
            return "windows";
        }
        if (platform.startsWith("linux")) {
            return "linux";
        }
        if (platform.startsWith("macos") || platform.startsWith("darwin")) {
            return "macos";
        }
        if (platform.startsWith("web")
                || id.equals("chrome")
                || id.equals("edge")
                || id.equals("web-server")) {
            return "web";
        }
        if (platform.startsWith("android")) {
            return "apk";
        }
        if (platform.startsWith("ios")) {
            return "ios";
        }
        String displayedPlatform = device.platform() == null
                ? "<missing>"
                : device.platform().strip();
        if (displayedPlatform.isEmpty()) {
            displayedPlatform = "<blank>";
        }
        throw new IllegalArgumentException(
                "Unsupported Flutter build device '" + device.id().strip()
                + "' with platform '" + displayedPlatform + "'");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Flutter test path is required");
        }
        String path = value.strip().replace('\\', '/');
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (path.isEmpty()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:/.*")
                || List.of(path.split("/", -1)).contains("..")) {
            throw new IllegalArgumentException(
                    "Flutter test path must stay inside the project: " + value);
        }
        return path;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof FlutterToolCommand command
                && type == command.type
                && arguments.equals(command.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arguments);
    }

    @Override
    public String toString() {
        return "FlutterToolCommand[type=" + type + ", arguments=" + arguments + "]";
    }
}
