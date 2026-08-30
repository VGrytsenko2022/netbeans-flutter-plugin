package dev.flutter.netbeans.plugin.designer.canvas.spi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Exact immutable process launch authorized by one native Canvas provider. */
public record NativeCanvasRunnerLaunch(
        Path executable,
        Path workingDirectory,
        List<String> command) {
    private static final int MAX_ARGUMENTS = 64;
    private static final int MAX_ARGUMENT_CHARACTERS = 4_096;

    public NativeCanvasRunnerLaunch {
        executable = requireAbsolute(executable, "runner executable");
        workingDirectory = requireAbsolute(
                workingDirectory, "runner working directory");
        if (executable.getParent() == null
                || !executable.getParent().equals(workingDirectory)) {
            throw new IllegalArgumentException(
                    "runner working directory must contain the executable");
        }
        Objects.requireNonNull(command, "command");
        if (command.isEmpty() || command.size() > MAX_ARGUMENTS) {
            throw new IllegalArgumentException(
                    "runner command must contain between 1 and 64 arguments");
        }
        List<String> copy = new ArrayList<>(command.size());
        for (String argument : command) {
            Objects.requireNonNull(argument, "runner command argument");
            if (argument.isBlank() || argument.length() > MAX_ARGUMENT_CHARACTERS
                    || argument.indexOf('\0') >= 0 || argument.indexOf('\r') >= 0
                    || argument.indexOf('\n') >= 0) {
                throw new IllegalArgumentException(
                        "runner command contains an invalid argument");
            }
            copy.add(argument);
        }
        command = List.copyOf(copy);
        if (!command.getFirst().equals(executable.toString())) {
            throw new IllegalArgumentException(
                    "runner command must start with the exact executable");
        }
    }

    private static Path requireAbsolute(Path value, String label) {
        Path path = Objects.requireNonNull(value, label).normalize();
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(label + " must be absolute");
        }
        return path;
    }
}
