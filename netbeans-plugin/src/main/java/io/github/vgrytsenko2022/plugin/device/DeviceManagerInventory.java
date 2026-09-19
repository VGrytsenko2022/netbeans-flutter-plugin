package io.github.vgrytsenko2022.plugin.device;

import java.util.List;
import java.util.Objects;

record DeviceManagerInventory(
        AndroidToolchainStatus toolchain,
        List<ConnectedAndroidDevice> connectedDevices,
        List<AndroidVirtualDevice> avds,
        String selectedTargetId,
        String message) {

    DeviceManagerInventory {
        toolchain = Objects.requireNonNull(toolchain, "toolchain");
        connectedDevices = List.copyOf(Objects.requireNonNull(connectedDevices, "connectedDevices"));
        avds = List.copyOf(Objects.requireNonNull(avds, "avds"));
        selectedTargetId = selectedTargetId == null ? "" : selectedTargetId.strip();
        message = message == null ? "" : message.strip();
    }

    DeviceManagerSnapshot snapshot(boolean busy, String overrideMessage) {
        String visibleMessage = overrideMessage == null || overrideMessage.isBlank()
                ? message : overrideMessage.strip();
        return new DeviceManagerSnapshot(
                toolchain,
                connectedDevices,
                avds,
                selectedTargetId,
                busy,
                java.util.Set.of(),
                visibleMessage);
    }
}
