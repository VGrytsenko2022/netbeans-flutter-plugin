package io.github.vgrytsenko2022.plugin.device;

import java.util.Objects;

/** An Android Virtual Device configuration and its current runtime state. */
public record AndroidVirtualDevice(
        String id,
        String displayName,
        String deviceProfile,
        String apiLevel,
        String architecture,
        State state,
        String connectedDeviceId,
        String detail) {

    public AndroidVirtualDevice {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        deviceProfile = normalize(deviceProfile);
        apiLevel = normalize(apiLevel);
        architecture = normalize(architecture);
        state = Objects.requireNonNull(state, "state");
        connectedDeviceId = normalize(connectedDeviceId);
        detail = normalize(detail);
    }

    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        OFFLINE,
        ERROR
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
