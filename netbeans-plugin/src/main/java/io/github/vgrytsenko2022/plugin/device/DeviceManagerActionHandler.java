package io.github.vgrytsenko2022.plugin.device;

/** Non-blocking bridge from the Swing panel to Android SDK/AVD orchestration. */
@FunctionalInterface
public interface DeviceManagerActionHandler {
    DeviceManagerActionHandler NO_OP = (action, selection) -> { };

    /**
     * Submits an action. Implementations must return quickly and publish resulting state through
     * {@link FlutterDeviceManagerPanel#setSnapshot(DeviceManagerSnapshot)}.
     */
    void perform(DeviceManagerAction action, DeviceManagerSelection selection);
}
