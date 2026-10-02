package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.ProcessResult;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Lists and launches emulators through the Flutter CLI. */
public final class FlutterEmulatorService {
    private static final Duration LIST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration LAUNCH_TIMEOUT = Duration.ofMinutes(2);
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern COLUMN_SEPARATOR = Pattern.compile("\\s*\\u2022\\s*");

    private final FlutterCommandExecutor command;

    public FlutterEmulatorService(FlutterCli cli) {
        this(Objects.requireNonNull(cli, "cli")::execute);
    }

    FlutterEmulatorService(FlutterCommandExecutor command) {
        this.command = Objects.requireNonNull(command, "command");
    }

    public List<FlutterEmulator> list() throws IOException, InterruptedException {
        return list(Path.of("").toAbsolutePath());
    }

    public List<FlutterEmulator> list(Path workingDirectory) throws IOException, InterruptedException {
        Path directory = normalizeWorkingDirectory(workingDirectory);
        ProcessResult result = command.execute(directory, LIST_TIMEOUT, "emulators");
        requireSuccess("list Flutter emulators", directory, null, result);

        List<FlutterEmulator> emulators = new ArrayList<>();
        for (String rawLine : result.stdout().lines().toList()) {
            String line = ANSI_ESCAPE.matcher(rawLine).replaceAll("").strip();
            if (line.isEmpty() || !line.contains("\u2022")) {
                continue;
            }
            String[] columns = COLUMN_SEPARATOR.split(line, 4);
            if (columns.length != 4 || isHeader(columns)) {
                continue;
            }
            if (!columns[0].isBlank()) {
                emulators.add(new FlutterEmulator(columns[0], columns[1], columns[2], columns[3]));
            }
        }
        return List.copyOf(emulators);
    }

    public void launch(Path workingDirectory, String emulatorId)
            throws IOException, InterruptedException {
        Path directory = normalizeWorkingDirectory(workingDirectory);
        if (emulatorId == null || emulatorId.isBlank()) {
            throw new IllegalArgumentException("Emulator id is required");
        }
        String id = emulatorId.strip();
        ProcessResult result = command.execute(
                directory, LAUNCH_TIMEOUT, "emulators", "--launch", id);
        requireSuccess("launch Flutter emulator", directory, id, result);
        requireNoReportedLaunchFailure(directory, id, result);
    }

    private static void requireNoReportedLaunchFailure(
            Path workingDirectory,
            String emulatorId,
            ProcessResult result) throws IOException {
        String output = ((result.stdout() == null ? "" : result.stdout()) + "\n"
                + (result.stderr() == null ? "" : result.stderr()))
                .toLowerCase(java.util.Locale.ROOT);
        if (output.contains("no emulator found")
                || output.contains("no emulators found")
                || output.contains("failed to launch")
                || output.contains("could not launch")) {
            throw new IOException("Unable to launch Flutter emulator '" + emulatorId
                    + "' from " + workingDirectory + ": Flutter reported failure despite exit "
                    + "code 0. " + failureDetail(result));
        }
    }

    private static boolean isHeader(String[] columns) {
        return columns[0].equalsIgnoreCase("Id")
                && columns[1].equalsIgnoreCase("Name")
                && columns[3].equalsIgnoreCase("Platform");
    }

    private static Path normalizeWorkingDirectory(Path workingDirectory) {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        return workingDirectory.toAbsolutePath().normalize();
    }

    private static void requireSuccess(
            String operation,
            Path workingDirectory,
            String target,
            ProcessResult result) throws IOException {
        if (result.success()) {
            return;
        }
        String targetText = target == null ? "" : " '" + target + "'";
        throw new IOException("Unable to " + operation + targetText + " from " + workingDirectory
                + ": flutter exited with code " + result.exitCode() + ". " + failureDetail(result));
    }

    private static String failureDetail(ProcessResult result) {
        String detail = result.stderr() == null || result.stderr().isBlank()
                ? result.stdout() : result.stderr();
        if (detail == null || detail.isBlank()) {
            return "Flutter did not provide an error message.";
        }
        String compact = detail.strip().replaceAll("\\s+", " ");
        return compact.length() <= 1200 ? compact : compact.substring(0, 1200) + "…";
    }
}
