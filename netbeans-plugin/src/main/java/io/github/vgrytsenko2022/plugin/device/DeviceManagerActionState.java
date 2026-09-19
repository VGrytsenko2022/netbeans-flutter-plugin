package io.github.vgrytsenko2022.plugin.device;

/** Whether a Device Manager action is currently available and, when not, its concrete cause. */
public record DeviceManagerActionState(boolean enabled, String reason) {
    public DeviceManagerActionState {
        reason = reason == null ? "" : reason.strip();
        if (!enabled && reason.isEmpty()) {
            throw new IllegalArgumentException("A disabled action must have a reason");
        }
    }

    public static DeviceManagerActionState enabledState() {
        return new DeviceManagerActionState(true, "");
    }

    public static DeviceManagerActionState disabledState(String reason) {
        return new DeviceManagerActionState(false, reason);
    }
}
