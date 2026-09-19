package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.*;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Starts managed {@code flutter run --machine} sessions. */
public final class FlutterRunManager {
    private final FlutterProcessStarter processStarter;

    public FlutterRunManager(FlutterCli cli) {
        this(Objects.requireNonNull(cli, "cli")::start);
    }

    FlutterRunManager(FlutterProcessStarter processStarter) {
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
    }

    public FlutterRunSession run(Path projectRoot, String deviceId) throws IOException {
        return start(projectRoot, deviceId, false);
    }

    public FlutterRunSession debug(Path projectRoot, String deviceId) throws IOException {
        return start(projectRoot, deviceId, true);
    }

    private FlutterRunSession start(Path projectRoot, String deviceId, boolean startPaused)
            throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path directory = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IOException("Unable to start Flutter application from " + directory
                    + ": project directory does not exist.");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Flutter device id is required");
        }
        String targetDevice = deviceId.strip();
        List<String> arguments = new ArrayList<>(List.of(
                "run", "--machine", "--debug", "--device-id", targetDevice));
        if (startPaused) {
            arguments.add("--start-paused");
        }
        Process process = processStarter.start(directory, List.copyOf(arguments));
        return new FlutterRunSession(process, targetDevice, startPaused);
    }
}
