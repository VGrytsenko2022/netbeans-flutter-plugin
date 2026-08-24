package dev.flutter.netbeans.project;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.api.ProcessResult;
import dev.flutter.netbeans.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Creates a Flutter application through the configured Flutter CLI. */
public final class FlutterProjectCreator {
    private static final Duration CREATE_TIMEOUT = Duration.ofMinutes(10);

    private final FlutterCommand command;
    private final FlutterProjectDetector detector;

    public FlutterProjectCreator(FlutterCli cli) {
        this(cli::execute, new FlutterProjectDetector());
    }

    FlutterProjectCreator(FlutterCommand command, FlutterProjectDetector detector) {
        this.command = Objects.requireNonNull(command, "command");
        this.detector = Objects.requireNonNull(detector, "detector");
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
        if (Files.exists(target)) {
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
                target.toString());

        if (!result.success()) {
            throw new IOException("Flutter application was not created at " + target
                    + ": flutter create exited with code " + result.exitCode() + ". "
                    + commandFailure(result.stderr(), result.stdout()));
        }

        return detector.detect(target).orElseThrow(() -> new IOException(
                "Flutter application creation reported success, but the generated project at "
                + target + " has no valid pubspec.yaml and lib directory."));
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
