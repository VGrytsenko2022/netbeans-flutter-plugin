package io.github.vgrytsenko2022.plugin.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeviceManagerPresentationTest {

    @Test
    void reportsConcreteIncompleteToolchainState() {
        AndroidToolchainStatus toolchain = new AndroidToolchainStatus(
                AndroidToolchainStatus.State.INCOMPLETE,
                "C:/Android/sdk",
                AndroidToolchainStatus.ToolState.AVAILABLE,
                AndroidToolchainStatus.ToolState.MISSING,
                AndroidToolchainStatus.ToolState.MISSING,
                AndroidToolchainStatus.ToolState.AVAILABLE,
                "Install Android SDK command-line tools and Emulator.");

        assertEquals("Android SDK: incomplete", DeviceManagerPresentation.toolchainTitle(toolchain));
        assertEquals(
                "C:/Android/sdk — Missing: emulator, avdmanager — Install Android SDK command-line tools and Emulator.",
                DeviceManagerPresentation.toolchainDetail(toolchain));

        DeviceManagerActionState create = DeviceManagerPresentation.actionState(
                snapshot(toolchain, List.of(), List.of()),
                DeviceManagerSelection.none(),
                DeviceManagerAction.CREATE);
        assertFalse(create.enabled());
        assertEquals(
                "Cannot create an Android Virtual Device because avdmanager is unavailable.",
                create.reason());
    }

    @Test
    void enablesStoppedAvdActionsThatAreSafeWhileStopped() {
        AndroidVirtualDevice avd = avd(AndroidVirtualDevice.State.STOPPED, "");
        DeviceManagerSnapshot snapshot = snapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(),
                List.of(avd));
        DeviceManagerSelection selection = DeviceManagerSelection.avd(avd.id());

        assertEnabled(snapshot, selection, DeviceManagerAction.START);
        assertEnabled(snapshot, selection, DeviceManagerAction.WIPE);
        assertEnabled(snapshot, selection, DeviceManagerAction.DELETE);
        assertDisabled(snapshot, selection, DeviceManagerAction.STOP, "because it is stopped");
        assertDisabled(snapshot, selection, DeviceManagerAction.RESTART, "because it is stopped");
        assertDisabled(snapshot, selection, DeviceManagerAction.SELECT_TARGET, "before selecting it");
    }

    @Test
    void enablesRunningAvdControlAndTargetSelectionWhenAdbTargetIsOnline() {
        ConnectedAndroidDevice connected = connected(
                "emulator-5554",
                ConnectedAndroidDevice.State.ONLINE,
                "");
        AndroidVirtualDevice avd = avd(AndroidVirtualDevice.State.RUNNING, connected.id());
        DeviceManagerSnapshot snapshot = snapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(connected),
                List.of(avd));
        DeviceManagerSelection selection = DeviceManagerSelection.avd(avd.id());

        assertEnabled(snapshot, selection, DeviceManagerAction.STOP);
        assertEnabled(snapshot, selection, DeviceManagerAction.RESTART);
        assertEnabled(snapshot, selection, DeviceManagerAction.SELECT_TARGET);
        assertDisabled(snapshot, selection, DeviceManagerAction.START, "because it is running");
        assertDisabled(snapshot, selection, DeviceManagerAction.WIPE, "before wiping its data");
        assertDisabled(snapshot, selection, DeviceManagerAction.DELETE, "before deleting it");
    }

    @Test
    void explainsWhyRunningAvdCannotBecomeTargetUntilAdbIsOnline() {
        ConnectedAndroidDevice connected = connected(
                "emulator-5554",
                ConnectedAndroidDevice.State.OFFLINE,
                "adb has not completed boot discovery");
        AndroidVirtualDevice avd = avd(AndroidVirtualDevice.State.RUNNING, connected.id());
        DeviceManagerSnapshot snapshot = snapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(connected),
                List.of(avd));

        assertDisabled(
                snapshot,
                DeviceManagerSelection.avd(avd.id()),
                DeviceManagerAction.SELECT_TARGET,
                "is not online");
    }

    @Test
    void connectedPhysicalDeviceOffersOnlyTargetSelection() {
        ConnectedAndroidDevice device = connected(
                "R58M1234",
                ConnectedAndroidDevice.State.ONLINE,
                "");
        DeviceManagerSnapshot snapshot = snapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(device),
                List.of());
        DeviceManagerSelection selection = DeviceManagerSelection.connectedDevice(device.id());

        assertEnabled(snapshot, selection, DeviceManagerAction.SELECT_TARGET);
        assertDisabled(snapshot, selection, DeviceManagerAction.START, "Select an Android Virtual Device");
        assertDisabled(snapshot, selection, DeviceManagerAction.DELETE, "Select an Android Virtual Device");
    }

    @Test
    void unavailableConnectedDeviceHasConcreteTargetReason() {
        ConnectedAndroidDevice device = connected(
                "R58M1234",
                ConnectedAndroidDevice.State.UNAUTHORIZED,
                "Accept the USB debugging prompt on the device");
        DeviceManagerSnapshot snapshot = snapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(device),
                List.of());

        DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                snapshot,
                DeviceManagerSelection.connectedDevice(device.id()),
                DeviceManagerAction.SELECT_TARGET);

        assertFalse(state.enabled());
        assertTrue(state.reason().contains("R58M1234"));
        assertTrue(state.reason().contains("unauthorized"));
        assertTrue(state.reason().contains("accept the usb debugging prompt"));
    }

    @Test
    void refreshAndBusyOperationsDisableConflictingActions() {
        AndroidVirtualDevice avd = avd(AndroidVirtualDevice.State.STOPPED, "");
        DeviceManagerSelection selection = DeviceManagerSelection.avd(avd.id());
        DeviceManagerSnapshot refreshing = new DeviceManagerSnapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(),
                List.of(avd),
                "",
                true,
                Set.of(),
                "Discovering devices");
        assertDisabled(refreshing, selection, DeviceManagerAction.REFRESH, "already running");
        assertDisabled(refreshing, selection, DeviceManagerAction.START, "Discovering devices");

        DeviceManagerSnapshot busy = new DeviceManagerSnapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(),
                List.of(avd),
                "",
                false,
                Set.of(selection),
                "Starting Pixel_API_35");
        assertDisabled(busy, selection, DeviceManagerAction.START, "already running for Pixel 8");
    }

    @Test
    void snapshotRejectsAmbiguousRowIds() {
        ConnectedAndroidDevice device = connected(
                "emulator-5554",
                ConnectedAndroidDevice.State.ONLINE,
                "");
        assertThrows(IllegalArgumentException.class, () -> new DeviceManagerSnapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                List.of(device, device),
                List.of(),
                "",
                false,
                Set.of(),
                ""));
    }

    private static DeviceManagerSnapshot snapshot(
            AndroidToolchainStatus toolchain,
            List<ConnectedAndroidDevice> devices,
            List<AndroidVirtualDevice> avds) {
        return new DeviceManagerSnapshot(
                toolchain,
                devices,
                avds,
                "",
                false,
                Set.of(),
                "");
    }

    private static ConnectedAndroidDevice connected(
            String id,
            ConnectedAndroidDevice.State state,
            String detail) {
        return new ConnectedAndroidDevice(
                id,
                "Pixel 8",
                "15",
                "35",
                id.startsWith("emulator-"),
                state,
                detail);
    }

    private static AndroidVirtualDevice avd(
            AndroidVirtualDevice.State state,
            String connectedDeviceId) {
        return new AndroidVirtualDevice(
                "Pixel_API_35",
                "Pixel 8",
                "pixel_8",
                "35",
                "x86_64",
                state,
                connectedDeviceId,
                "");
    }

    private static void assertEnabled(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection,
            DeviceManagerAction action) {
        DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                snapshot,
                selection,
                action);
        assertTrue(state.enabled(), state.reason());
    }

    private static void assertDisabled(
            DeviceManagerSnapshot snapshot,
            DeviceManagerSelection selection,
            DeviceManagerAction action,
            String expectedReasonPart) {
        DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                snapshot,
                selection,
                action);
        assertFalse(state.enabled());
        assertTrue(
                state.reason().contains(expectedReasonPart),
                () -> "Expected reason containing '" + expectedReasonPart + "' but was '" + state.reason() + "'");
    }
}
