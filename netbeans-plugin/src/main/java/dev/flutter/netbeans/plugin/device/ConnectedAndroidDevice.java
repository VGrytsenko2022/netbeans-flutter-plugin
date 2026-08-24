package dev.flutter.netbeans.plugin.device;

import java.util.Objects;

/** A physical or emulated Android device reported by adb/Flutter. */
public record ConnectedAndroidDevice(
        String id,
        String displayName,
        String androidVersion,
        String apiLevel,
        boolean emulator,
        State state,
        String detail) {

    public ConnectedAndroidDevice {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        androidVersion = normalize(androidVersion);
        apiLevel = normalize(apiLevel);
        state = Objects.requireNonNull(state, "state");
        detail = normalize(detail);
    }

    public enum State {
        ONLINE,
        BOOTING,
        OFFLINE,
        UNAUTHORIZED,
        DISCONNECTED,
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
