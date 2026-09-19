package io.github.vgrytsenko2022.plugin.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure presentation and action-enablement rules shared by Swing UI and tests. */
public final class DeviceManagerPresentation {
    private DeviceManagerPresentation() {
    }

    public static DeviceManagerActionState actionState(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection,
            DeviceManagerAction action) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(action, "action");

        if (action == DeviceManagerAction.REFRESH) {
            return snapshot.refreshing()
                    ? disabled(busyReason(snapshot))
                    : enabled();
        }
        if (snapshot.refreshing()) {
            return disabled(busyReason(snapshot));
        }
        if (action == DeviceManagerAction.CREATE) {
            return snapshot.toolchain().canCreateAvd()
                    ? enabled()
                    : disabled(missingCreateTools(snapshot.toolchain()));
        }
        if (selection.isNone()) {
            return disabled("Select a connected Android device or Android Virtual Device.");
        }
        if (snapshot.busySelections().contains(selection)) {
            return disabled("An Android device operation is already running for "
                    + selectionLabel(snapshot, selection) + ".");
        }
        return switch (selection.kind()) {
            case CONNECTED_DEVICE -> connectedDeviceAction(snapshot, selection, action);
            case AVD -> avdAction(snapshot, selection, action);
            case NONE -> throw new IllegalStateException("NONE was handled above");
        };
    }

    public static String toolchainTitle(AndroidToolchainStatus toolchain) {
        return switch (toolchain.state()) {
            case DISCOVERING -> "Android SDK: discovering";
            case READY -> "Android SDK: ready";
            case INCOMPLETE -> "Android SDK: incomplete";
            case UNAVAILABLE -> "Android SDK: unavailable";
            case ERROR -> "Android SDK: discovery failed";
        };
    }

    public static String toolchainDetail(AndroidToolchainStatus toolchain) {
        List<String> parts = new ArrayList<>();
        if (!toolchain.androidSdkPath().isBlank()) {
            parts.add(toolchain.androidSdkPath());
        }
        List<String> missing = missingTools(toolchain);
        if (!missing.isEmpty()) {
            parts.add("Missing: " + String.join(", ", missing));
        }
        if (!toolchain.detail().isBlank()) {
            parts.add(toolchain.detail());
        }
        return parts.isEmpty() ? "No Android SDK details are available." : String.join(" — ", parts);
    }

    public static String connectedDeviceState(ConnectedAndroidDevice device) {
        String state = switch (device.state()) {
            case ONLINE -> "Online";
            case BOOTING -> "Booting";
            case OFFLINE -> "Offline";
            case UNAUTHORIZED -> "Unauthorized";
            case DISCONNECTED -> "Disconnected";
            case ERROR -> "Error";
        };
        return device.detail().isBlank() ? state : state + ": " + device.detail();
    }

    public static String avdState(AndroidVirtualDevice avd) {
        String state = switch (avd.state()) {
            case STOPPED -> "Stopped";
            case STARTING -> "Starting";
            case RUNNING -> "Running";
            case STOPPING -> "Stopping";
            case OFFLINE -> "Offline";
            case ERROR -> "Error";
        };
        return avd.detail().isBlank() ? state : state + ": " + avd.detail();
    }

    public static String selectionLabel(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection) {
        return switch (selection.kind()) {
            case NONE -> "no selection";
            case CONNECTED_DEVICE -> {
                ConnectedAndroidDevice device = snapshot.connectedDevice(selection.id());
                yield device == null
                        ? "connected device " + selection.id()
                        : device.displayName() + " (" + device.id() + ")";
            }
            case AVD -> {
                AndroidVirtualDevice avd = snapshot.avd(selection.id());
                yield avd == null
                        ? "Android Virtual Device " + selection.id()
                        : avd.displayName() + " (" + avd.id() + ")";
            }
        };
    }

    private static DeviceManagerActionState connectedDeviceAction(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection,
            DeviceManagerAction action) {
        ConnectedAndroidDevice device = snapshot.connectedDevice(selection.id());
        if (device == null) {
            return disabled("Connected Android device " + selection.id() + " is no longer available.");
        }
        if (action != DeviceManagerAction.SELECT_TARGET) {
            return disabled("Select an Android Virtual Device to perform " + actionLabel(action) + ".");
        }
        if (device.state() != ConnectedAndroidDevice.State.ONLINE) {
            return disabled("Cannot select " + device.displayName() + " (" + device.id()
                    + ") because it is " + connectedDeviceState(device).toLowerCase() + ".");
        }
        return enabled();
    }

    private static DeviceManagerActionState avdAction(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection,
            DeviceManagerAction action) {
        AndroidVirtualDevice avd = snapshot.avd(selection.id());
        if (avd == null) {
            return disabled("Android Virtual Device " + selection.id() + " is no longer available.");
        }
        return switch (action) {
            case START -> avd.state() == AndroidVirtualDevice.State.STOPPED
                    ? require(snapshot.toolchain().canStartAvd(), "Android SDK emulator is unavailable.")
                    : disabled("Cannot start " + avd.displayName() + " because it is "
                            + avdState(avd).toLowerCase() + ".");
            case STOP -> canStop(avd.state())
                    ? require(snapshot.toolchain().canStopAvd(), "Android SDK adb is unavailable.")
                    : disabled("Cannot stop " + avd.displayName() + " because it is "
                            + avdState(avd).toLowerCase() + ".");
            case RESTART -> avd.state() == AndroidVirtualDevice.State.RUNNING
                    ? require(
                            snapshot.toolchain().canStartAvd() && snapshot.toolchain().canStopAvd(),
                            "Android SDK emulator and adb are required to restart an AVD.")
                    : disabled("Cannot restart " + avd.displayName() + " because it is "
                            + avdState(avd).toLowerCase() + ".");
            case WIPE -> avd.state() == AndroidVirtualDevice.State.STOPPED
                    ? require(snapshot.toolchain().canWipeAvd(), "Android SDK emulator is unavailable.")
                    : disabled("Stop " + avd.displayName() + " before wiping its data.");
            case DELETE -> avd.state() == AndroidVirtualDevice.State.STOPPED
                    ? require(snapshot.toolchain().canDeleteAvd(), "Android SDK avdmanager is unavailable.")
                    : disabled("Stop " + avd.displayName() + " before deleting it.");
            case SELECT_TARGET -> selectAvdTarget(snapshot, avd);
            case CREATE, REFRESH -> throw new IllegalStateException(action + " was handled before row dispatch");
        };
    }

    private static DeviceManagerActionState selectAvdTarget(
            DeviceManagerSnapshot snapshot,
            AndroidVirtualDevice avd) {
        if (avd.state() != AndroidVirtualDevice.State.RUNNING) {
            return disabled("Start " + avd.displayName() + " before selecting it as the Flutter run target.");
        }
        if (avd.connectedDeviceId().isBlank()) {
            return disabled("Waiting for adb to report a device id for " + avd.displayName() + ".");
        }
        if (!snapshot.isConnectedTargetOnline(avd.connectedDeviceId())) {
            return disabled("Connected target " + avd.connectedDeviceId() + " for "
                    + avd.displayName() + " is not online.");
        }
        return enabled();
    }

    private static boolean canStop(AndroidVirtualDevice.State state) {
        return state == AndroidVirtualDevice.State.STARTING
                || state == AndroidVirtualDevice.State.RUNNING;
    }

    private static DeviceManagerActionState require(boolean available, String reason) {
        return available ? enabled() : disabled(reason);
    }

    private static DeviceManagerActionState enabled() {
        return DeviceManagerActionState.enabledState();
    }

    private static DeviceManagerActionState disabled(String reason) {
        return DeviceManagerActionState.disabledState(reason);
    }

    private static String missingCreateTools(AndroidToolchainStatus toolchain) {
        List<String> tools = new ArrayList<>();
        if (toolchain.avdManager() != AndroidToolchainStatus.ToolState.AVAILABLE) {
            tools.add("avdmanager");
        }
        if (toolchain.sdkManager() != AndroidToolchainStatus.ToolState.AVAILABLE) {
            tools.add("sdkmanager");
        }
        return "Cannot create an Android Virtual Device because " + String.join(" and ", tools)
                + (tools.size() == 1 ? " is" : " are") + " unavailable.";
    }

    private static List<String> missingTools(AndroidToolchainStatus toolchain) {
        List<String> tools = new ArrayList<>();
        addMissing(tools, "adb", toolchain.adb());
        addMissing(tools, "emulator", toolchain.emulator());
        addMissing(tools, "avdmanager", toolchain.avdManager());
        addMissing(tools, "sdkmanager", toolchain.sdkManager());
        return tools;
    }

    private static void addMissing(
            List<String> tools,
            String name,
            AndroidToolchainStatus.ToolState state) {
        if (state == AndroidToolchainStatus.ToolState.MISSING) {
            tools.add(name);
        }
    }

    private static String actionLabel(DeviceManagerAction action) {
        return switch (action) {
            case CREATE -> "Create";
            case REFRESH -> "Refresh";
            case START -> "Start";
            case STOP -> "Stop";
            case RESTART -> "Restart";
            case WIPE -> "Wipe Data";
            case DELETE -> "Delete";
            case SELECT_TARGET -> "Select Target";
        };
    }

    private static String busyReason(DeviceManagerSnapshot snapshot) {
        return snapshot.message().isBlank()
                ? "Wait for the current Flutter Device Manager operation to finish."
                : "A Flutter Device Manager operation is already running: "
                        + snapshot.message();
    }
}
