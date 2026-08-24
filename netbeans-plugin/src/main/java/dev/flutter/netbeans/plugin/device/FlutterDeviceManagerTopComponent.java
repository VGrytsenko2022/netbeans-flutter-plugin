package dev.flutter.netbeans.plugin.device;

import java.awt.BorderLayout;
import java.util.Objects;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** NetBeans window hosting the Flutter Android Device Manager. */
@TopComponent.Description(
        preferredID = FlutterDeviceManagerTopComponent.PREFERRED_ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false, position = 3490)
@ActionID(
        category = "Window",
        id = "dev.flutter.netbeans.plugin.device.FlutterDeviceManagerTopComponent")
@ActionReference(path = "Menu/Flutter", position = 130)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_FlutterDeviceManagerAction",
        preferredID = FlutterDeviceManagerTopComponent.PREFERRED_ID)
@Messages({
    "CTL_FlutterDeviceManagerAction=Device Manager",
    "CTL_FlutterDeviceManagerTopComponent=Flutter Device Manager",
    "HINT_FlutterDeviceManagerTopComponent=Manage connected Android devices and Android Virtual Devices"
})
public final class FlutterDeviceManagerTopComponent extends TopComponent {
    public static final String PREFERRED_ID = "FlutterDeviceManagerTopComponent";

    private final FlutterDeviceManagerPanel panel = new FlutterDeviceManagerPanel();
    private final FlutterDeviceManagerController controller;

    public FlutterDeviceManagerTopComponent() {
        controller = new FlutterDeviceManagerController(panel);
        setName(Bundle.CTL_FlutterDeviceManagerTopComponent());
        setToolTipText(Bundle.HINT_FlutterDeviceManagerTopComponent());
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName(Bundle.CTL_FlutterDeviceManagerTopComponent());
        getAccessibleContext().setAccessibleDescription(Bundle.HINT_FlutterDeviceManagerTopComponent());
    }

    public void setActionHandler(DeviceManagerActionHandler handler) {
        panel.setActionHandler(Objects.requireNonNull(handler, "handler"));
    }

    public void setSnapshot(DeviceManagerSnapshot snapshot) {
        panel.setSnapshot(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public FlutterDeviceManagerPanel panel() {
        return panel;
    }

    /** Finds the registered component without creating a second window instance. */
    public static FlutterDeviceManagerTopComponent findInstance() {
        TopComponent component = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        return component instanceof FlutterDeviceManagerTopComponent manager ? manager : null;
    }

    @Override
    protected void componentOpened() {
        controller.open();
    }

    @Override
    protected void componentClosed() {
        controller.close();
    }
}
