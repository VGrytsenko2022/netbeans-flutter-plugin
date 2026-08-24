package dev.flutter.netbeans.plugin.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class FlutterDeviceManagerPanelTest {

    @Test
    void selectionUpdatesButtonsAndSubmitsStableAvdIdentity() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        FlutterDeviceManagerPanel panel = onEdt(() -> new FlutterDeviceManagerPanel(
                (action, selection) -> invocations.add(new Invocation(action, selection)),
                (parent, title, message) -> true));

        onEdt(() -> {
            panel.setSnapshot(snapshot(AndroidVirtualDevice.State.STOPPED));
            panel.avdsTable().setRowSelectionInterval(0, 0);

            assertTrue(panel.button(DeviceManagerAction.START).isEnabled());
            assertTrue(panel.button(DeviceManagerAction.WIPE).isEnabled());
            assertFalse(panel.button(DeviceManagerAction.STOP).isEnabled());
            panel.button(DeviceManagerAction.START).doClick();
        });

        assertEquals(
                List.of(new Invocation(
                        DeviceManagerAction.START,
                        DeviceManagerSelection.avd("Pixel_API_35"))),
                invocations);
    }

    @Test
    void destructiveActionRequiresExplicitConfirmation() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        AtomicReference<Boolean> allow = new AtomicReference<>(false);
        AtomicReference<String> confirmation = new AtomicReference<>();
        FlutterDeviceManagerPanel panel = onEdt(() -> new FlutterDeviceManagerPanel(
                (action, selection) -> invocations.add(new Invocation(action, selection)),
                (parent, title, message) -> {
                    confirmation.set(title + ": " + message);
                    return allow.get();
                }));

        onEdt(() -> {
            panel.setSnapshot(snapshot(AndroidVirtualDevice.State.STOPPED));
            panel.avdsTable().setRowSelectionInterval(0, 0);
            panel.button(DeviceManagerAction.WIPE).doClick();
            assertTrue(invocations.isEmpty());
            assertTrue(confirmation.get().contains("will start after the reset"));
            assertTrue(confirmation.get().contains("SD card data will be retained"));

            panel.button(DeviceManagerAction.DELETE).doClick();
            assertTrue(invocations.isEmpty());
            assertTrue(confirmation.get().contains("Pixel 8 (Pixel_API_35)"));

            allow.set(true);
            panel.button(DeviceManagerAction.DELETE).doClick();
        });

        assertEquals(
                List.of(new Invocation(
                        DeviceManagerAction.DELETE,
                        DeviceManagerSelection.avd("Pixel_API_35"))),
                invocations);
    }

    @Test
    void refreshUsesNoSelectionAndPreservesSelectionAcrossSnapshots() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        FlutterDeviceManagerPanel panel = onEdt(() -> new FlutterDeviceManagerPanel(
                (action, selection) -> invocations.add(new Invocation(action, selection)),
                (parent, title, message) -> true));

        onEdt(() -> {
            panel.setSnapshot(snapshot(AndroidVirtualDevice.State.STOPPED));
            panel.avdsTable().setRowSelectionInterval(0, 0);
            panel.setSnapshot(snapshot(AndroidVirtualDevice.State.RUNNING));

            assertEquals(DeviceManagerSelection.avd("Pixel_API_35"), panel.selection());
            assertTrue(panel.button(DeviceManagerAction.STOP).isEnabled());
            assertTrue(panel.statusLabel().getText().contains("Running"));
            panel.button(DeviceManagerAction.REFRESH).doClick();
        });

        assertEquals(
                List.of(new Invocation(
                        DeviceManagerAction.REFRESH,
                        DeviceManagerSelection.none())),
                invocations);
    }

    @Test
    void concreteDisabledReasonIsExposedAsTooltipAndAccessibleDescription() throws Exception {
        FlutterDeviceManagerPanel panel = onEdt(
                (ThrowingSupplier<FlutterDeviceManagerPanel>) FlutterDeviceManagerPanel::new);

        onEdt(() -> {
            panel.setSnapshot(snapshot(AndroidVirtualDevice.State.RUNNING));
            panel.avdsTable().setRowSelectionInterval(0, 0);

            String reason = panel.button(DeviceManagerAction.DELETE).getToolTipText();
            assertTrue(reason.contains("Stop Pixel 8 before deleting it"));
            assertEquals(
                    reason,
                    panel.button(DeviceManagerAction.DELETE)
                            .getAccessibleContext()
                            .getAccessibleDescription());
        });
    }

    private static DeviceManagerSnapshot snapshot(AndroidVirtualDevice.State state) {
        String connectedId = state == AndroidVirtualDevice.State.RUNNING
                ? "emulator-5554"
                : "";
        List<ConnectedAndroidDevice> connected = connectedId.isBlank()
                ? List.of()
                : List.of(new ConnectedAndroidDevice(
                        connectedId,
                        "Pixel 8",
                        "15",
                        "35",
                        true,
                        ConnectedAndroidDevice.State.ONLINE,
                        ""));
        return new DeviceManagerSnapshot(
                AndroidToolchainStatus.ready("C:/Android/sdk"),
                connected,
                List.of(new AndroidVirtualDevice(
                        "Pixel_API_35",
                        "Pixel 8",
                        "pixel_8",
                        "35",
                        "x86_64",
                        state,
                        connectedId,
                        "")),
                "",
                false,
                Set.of(),
                "");
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("EDT assertion failed", failure.get());
        }
    }

    private static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        onEdt(() -> result.set(supplier.get()));
        return result.get();
    }

    private record Invocation(DeviceManagerAction action, DeviceManagerSelection selection) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
