package dev.flutter.netbeans.run;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Immutable Android Virtual Device configuration and runtime snapshot. */
public record AndroidAvd(
        String id,
        String displayName,
        String deviceProfile,
        String apiLevel,
        String abi,
        String target,
        String tag,
        AndroidAvdState state,
        Optional<String> connectedDeviceId,
        Optional<Path> dataDirectory,
        String detail,
        Optional<String> error) {

    public AndroidAvd {
        id = required(id, "AVD id");
        displayName = valueOr(displayName, id);
        deviceProfile = clean(deviceProfile);
        apiLevel = valueOr(apiLevel, "unknown");
        abi = valueOr(abi, "unknown");
        target = clean(target);
        tag = clean(tag);
        state = Objects.requireNonNull(state, "state");
        connectedDeviceId = cleanOptional(connectedDeviceId);
        dataDirectory = dataDirectory == null ? Optional.empty()
                : dataDirectory.map(path -> path.toAbsolutePath().normalize());
        detail = clean(detail);
        error = cleanOptional(error);
    }

    public OptionalInt numericApiLevel() {
        try {
            return OptionalInt.of(Integer.parseInt(apiLevel));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    AndroidAvd withRuntime(
            AndroidAvdState runtimeState,
            String deviceId,
            String runtimeDetail,
            String runtimeError) {
        return new AndroidAvd(
                id, displayName, deviceProfile, apiLevel, abi, target, tag, runtimeState,
                Optional.ofNullable(deviceId), dataDirectory,
                runtimeDetail == null || runtimeDetail.isBlank() ? detail : runtimeDetail,
                Optional.ofNullable(runtimeError));
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.strip();
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static Optional<String> cleanOptional(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().strip());
    }
}
