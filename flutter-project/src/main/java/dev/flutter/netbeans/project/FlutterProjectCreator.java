package dev.flutter.netbeans.project;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.ProcessResult;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeProvisioner;
import dev.flutter.netbeans.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Creates a Flutter application through the configured Flutter CLI. */
public final class FlutterProjectCreator {
    private static final Duration CREATE_TIMEOUT = Duration.ofMinutes(10);

    private final FlutterCommand command;
    private final FlutterProjectDetector detector;
    private final FlutterProjectThemeProvisioner themeProvisioner;

    public FlutterProjectCreator(FlutterCli cli) {
        this(cli::execute, new FlutterProjectDetector(),
                new FlutterProjectThemeProvisioner());
    }

    FlutterProjectCreator(FlutterCommand command, FlutterProjectDetector detector) {
        this(command, detector, new FlutterProjectThemeProvisioner());
    }

    FlutterProjectCreator(
            FlutterCommand command,
            FlutterProjectDetector detector,
            FlutterProjectThemeProvisioner themeProvisioner) {
        this.command = Objects.requireNonNull(command, "command");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.themeProvisioner = Objects.requireNonNull(themeProvisioner, "themeProvisioner");
    }

    public FlutterProjectInfo create(FlutterProjectCreationRequest request)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        Path parent = request.parentDirectory();
        Path target = request.targetDirectory();

        if (!Files.isDirectory(parent)) {
            throw new IOException("Flutter application cannot be created at " + target
                    + ": parent directory does not exist: " + parent + ".");
        }
        if (!Files.isWritable(parent)) {
            throw new IOException("Flutter application cannot be created at " + target
                    + ": parent directory is not writable: " + parent + ".");
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(target)) {
            throw new IOException("Flutter application cannot be created at " + target
                    + ": the target already exists.");
        }

        var result = command.execute(
                parent,
                CREATE_TIMEOUT,
                "create",
                "--template", "app",
                "--project-name", request.projectName(),
                "--org", request.organization(),
                "--description", request.description(),
                "--platforms=" + request.platformsArgument(),
                target.toString());

        if (!result.success()) {
            throw new IOException("Flutter application was not created at " + target
                    + ": flutter create exited with code " + result.exitCode() + ". "
                    + commandFailure(result.stderr(), result.stdout()));
        }

        List<String> missingPlatforms = request.platforms().stream()
                .filter(platform -> {
                    Path platformDirectory = target.resolve(platform.id());
                    return !Files.isDirectory(
                            platformDirectory, LinkOption.NOFOLLOW_LINKS)
                            || Files.isSymbolicLink(platformDirectory);
                })
                .map(FlutterProjectPlatform::id)
                .toList();
        if (!missingPlatforms.isEmpty()) {
            throw new IOException("Flutter application creation reported success at " + target
                    + ", but these selected platform directories were not generated: "
                    + String.join(", ", missingPlatforms)
                    + ". Ensure the corresponding Flutter platform feature is enabled. "
                    + "The partially generated project was preserved for inspection.");
        }

        FlutterProjectInfo created = detector.detect(target).orElseThrow(() -> new IOException(
                "Flutter application creation reported success, but the generated project at "
                + target + " has no valid pubspec.yaml and lib directory."));
        try {
            themeProvisioner.provisionNewProject(target);
        } catch (IOException ex) {
            throw new IOException("Flutter application was generated at " + target
                    + ", but its default light and dark project themes could not be provisioned. "
                    + "The generated project was preserved for inspection. " + ex.getMessage(), ex);
        }
        return created;
    }

    private static String commandFailure(String stderr, String stdout) {
        String detail = stderr == null || stderr.isBlank() ? stdout : stderr;
        if (detail == null || detail.isBlank()) {
            return "Flutter did not provide an error message.";
        }
        String compact = detail.trim().replaceAll("\\s+", " ");
        int maxLength = 1200;
        return compact.length() <= maxLength
                ? compact
                : compact.substring(0, maxLength) + "…";
    }

    @FunctionalInterface
    interface FlutterCommand {
        ProcessResult execute(Path workingDirectory, Duration timeout, String... arguments)
                throws IOException, InterruptedException;
    }
}
