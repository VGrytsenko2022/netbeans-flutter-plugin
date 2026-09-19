package io.github.vgrytsenko2022.run;

import java.util.Objects;
import java.util.Optional;

/** Immutable ADB device snapshot, including the exact AVD mapping when available. */
public record AndroidConnectedDevice(
        String id,
        String displayName,
        String model,
        String product,
        String hardwareDevice,
        boolean emulator,
        Optional<String> avdId,
        AndroidConnectedDeviceState state,
        String detail,
        Optional<String> error) {

    public AndroidConnectedDevice {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Connected Android device id is required");
        }
        id = id.strip();
        model = clean(model);
        product = clean(product);
        hardwareDevice = clean(hardwareDevice);
        displayName = displayName == null || displayName.isBlank()
                ? (model.isBlank() ? id : model.replace('_', ' ')) : displayName.strip();
        avdId = cleanOptional(avdId);
        state = Objects.requireNonNull(state, "state");
        detail = detail == null ? "" : detail.strip();
        error = cleanOptional(error);
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
