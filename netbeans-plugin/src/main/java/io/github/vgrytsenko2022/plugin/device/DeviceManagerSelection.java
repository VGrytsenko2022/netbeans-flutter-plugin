package io.github.vgrytsenko2022.plugin.device;

import java.util.Objects;

/** Stable row identity passed from the Device Manager UI to its controller. */
public record DeviceManagerSelection(Kind kind, String id) {
    private static final DeviceManagerSelection NONE =
            new DeviceManagerSelection(Kind.NONE, "");

    public DeviceManagerSelection {
        kind = Objects.requireNonNull(kind, "kind");
        id = id == null ? "" : id.strip();
        if (kind == Kind.NONE && !id.isEmpty()) {
            throw new IllegalArgumentException("NONE selection must not have an id");
        }
        if (kind != Kind.NONE && id.isEmpty()) {
            throw new IllegalArgumentException(kind + " selection id must not be blank");
        }
    }

    public static DeviceManagerSelection none() {
        return NONE;
    }

    public static DeviceManagerSelection connectedDevice(String id) {
        return new DeviceManagerSelection(Kind.CONNECTED_DEVICE, id);
    }

    public static DeviceManagerSelection avd(String id) {
        return new DeviceManagerSelection(Kind.AVD, id);
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }

    public enum Kind {
        NONE,
        CONNECTED_DEVICE,
        AVD
    }
}
