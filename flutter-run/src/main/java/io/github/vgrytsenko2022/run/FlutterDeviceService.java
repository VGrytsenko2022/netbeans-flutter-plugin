package io.github.vgrytsenko2022.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vgrytsenko2022.api.FlutterDevice;
import io.github.vgrytsenko2022.api.ProcessResult;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Discovers devices through {@code flutter devices --machine}. */
public final class FlutterDeviceService {
    private static final Duration DISCOVERY_TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final FlutterCommandExecutor command;

    public FlutterDeviceService(FlutterCli cli) {
        this(Objects.requireNonNull(cli, "cli")::execute);
    }

    FlutterDeviceService(FlutterCommandExecutor command) {
        this.command = Objects.requireNonNull(command, "command");
    }

    public List<FlutterDevice> list(Path workingDirectory)
            throws IOException, InterruptedException {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Path directory = workingDirectory.toAbsolutePath().normalize();
        ProcessResult result = command.execute(
                directory, DISCOVERY_TIMEOUT, "devices", "--machine");
        if (!result.success()) {
            throw new IOException("Unable to discover Flutter devices from " + directory
                    + ": flutter devices exited with code " + result.exitCode() + ". "
                    + failureDetail(result));
        }
        return parseDevices(result.stdout(), directory);
    }

    static List<FlutterDevice> parseDevices(String output, Path workingDirectory) throws IOException {
        if (output == null || output.isBlank()) {
            throw invalidResponse(workingDirectory, "the command returned no JSON");
        }

        final JsonNode root;
        try {
            root = JSON.readTree(output);
        } catch (JsonProcessingException ex) {
            throw invalidResponse(workingDirectory, "the response is not valid JSON", ex);
        }
        if (!root.isArray()) {
            throw invalidResponse(workingDirectory, "the top-level JSON value is not an array");
        }

        List<FlutterDevice> devices = new ArrayList<>();
        int index = 0;
        for (JsonNode item : root) {
            if (!item.isObject()) {
                throw invalidResponse(workingDirectory,
                        "device entry " + index + " is not a JSON object");
            }
            JsonNode supported = item.get("isSupported");
            if (supported != null && supported.isBoolean() && !supported.booleanValue()) {
                index++;
                continue;
            }
            String id = text(item, "id");
            if (id == null || id.isBlank()) {
                throw invalidResponse(workingDirectory,
                        "device entry " + index + " has no non-empty id");
            }
            id = id.strip();
            String name = text(item, "name");
            String platform = text(item, "targetPlatform");
            boolean emulator = item.path("emulator").asBoolean(false);
            devices.add(new FlutterDevice(
                    id,
                    name == null || name.isBlank() ? id : name,
                    platform == null || platform.isBlank() ? "unknown" : platform,
                    emulator));
            index++;
        }
        return List.copyOf(devices);
    }

    private static String text(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value == null || value.isNull() || !value.isValueNode() ? null : value.asText();
    }

    private static IOException invalidResponse(Path workingDirectory, String reason) {
        return new IOException("Unable to discover Flutter devices from " + workingDirectory
                + ": flutter devices --machine returned an invalid response because " + reason + ".");
    }

    private static IOException invalidResponse(
            Path workingDirectory, String reason, Throwable cause) {
        return new IOException("Unable to discover Flutter devices from " + workingDirectory
                + ": flutter devices --machine returned an invalid response because " + reason + ".",
                cause);
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
