package dev.flutter.netbeans.plugin.device;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable state rendered by {@link FlutterDeviceManagerPanel}. */
public record DeviceManagerSnapshot(
        AndroidToolchainStatus toolchain,
        List<ConnectedAndroidDevice> connectedDevices,
        List<AndroidVirtualDevice> avds,
        String selectedTargetId,
        boolean refreshing,
        Set<DeviceManagerSelection> busySelections,
        String message) {

    public DeviceManagerSnapshot {
        toolchain = Objects.requireNonNull(toolchain, "toolchain");
        connectedDevices = List.copyOf(Objects.requireNonNull(connectedDevices, "connectedDevices"));
        avds = List.copyOf(Objects.requireNonNull(avds, "avds"));
        selectedTargetId = normalize(selectedTargetId);
        busySelections = Set.copyOf(Objects.requireNonNull(busySelections, "busySelections"));
        message = normalize(message);
        requireUniqueDeviceIds(connectedDevices);
        requireUniqueAvdIds(avds);
        if (busySelections.stream().anyMatch(DeviceManagerSelection::isNone)) {
            throw new IllegalArgumentException("busySelections must identify a device or AVD");
        }
    }

    public static DeviceManagerSnapshot initial() {
        return new DeviceManagerSnapshot(
                AndroidToolchainStatus.unavailable(
                        "Android SDK discovery has not run. Use Refresh after the Device Manager controller is connected."),
                List.of(),
                List.of(),
                "",
                false,
                Set.of(),
                "");
    }

    public ConnectedAndroidDevice connectedDevice(String id) {
        return connectedDevices.stream()
                .filter(device -> device.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public AndroidVirtualDevice avd(String id) {
        return avds.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean isConnectedTargetOnline(String deviceId) {
        ConnectedAndroidDevice device = connectedDevice(deviceId);
        return device != null && device.state() == ConnectedAndroidDevice.State.ONLINE;
    }

    private static void requireUniqueDeviceIds(List<ConnectedAndroidDevice> devices) {
        Set<String> ids = new HashSet<>();
        for (ConnectedAndroidDevice device : devices) {
            if (!ids.add(device.id())) {
                throw new IllegalArgumentException("Duplicate connected device id: " + device.id());
            }
        }
    }

    private static void requireUniqueAvdIds(List<AndroidVirtualDevice> avds) {
        Set<String> ids = new HashSet<>();
        for (AndroidVirtualDevice avd : avds) {
            if (!ids.add(avd.id())) {
                throw new IllegalArgumentException("Duplicate AVD id: " + avd.id());
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
