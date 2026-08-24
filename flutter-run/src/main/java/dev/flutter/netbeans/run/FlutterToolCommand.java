package dev.flutter.netbeans.run;

import java.util.List;
import java.util.Objects;

/**
 * Immutable command line for a supported one-shot Flutter project operation.
 *
 * <p>The executable and working directory deliberately are not part of this
 * value. The IDE resolves both from the owning project and configured SDK.</p>
 */
public final class FlutterToolCommand {
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

    public static FlutterToolCommand pubGet() {
        return new FlutterToolCommand(FlutterToolCommandType.PUB_GET, PUB_GET_ARGUMENTS);
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
