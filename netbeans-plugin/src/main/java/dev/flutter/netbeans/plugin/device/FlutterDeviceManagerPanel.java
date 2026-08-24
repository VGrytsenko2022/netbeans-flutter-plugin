package dev.flutter.netbeans.plugin.device;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle.Messages;

/** Reusable Swing view for Android devices and Android Virtual Devices. */
@Messages({
    "LBL_AndroidSdk=Android SDK",
    "LBL_ConnectedDevices=Connected Android Devices",
    "LBL_VirtualDevices=Android Virtual Devices",
    "LBL_CreateAvd=Create...",
    "LBL_RefreshDevices=Refresh",
    "LBL_StartAvd=Start",
    "LBL_StopAvd=Stop",
    "LBL_RestartAvd=Restart",
    "LBL_WipeAvd=Wipe Data...",
    "LBL_DeleteAvd=Delete...",
    "LBL_SelectTarget=Select Target",
    "COL_Device=Device",
    "COL_Id=ID",
    "COL_Android=Android",
    "COL_State=State",
    "COL_Target=Flutter Target",
    "COL_Avd=AVD",
    "COL_Profile=Device Profile",
    "COL_Api=API",
    "COL_Architecture=Architecture",
    "COL_ConnectedTarget=Connected Target",
    "TXT_SelectedTarget=Selected",
    "TXT_NoDevices=No connected Android devices were found.",
    "TXT_NoAvds=No Android Virtual Devices were found.",
    "TTL_WipeAvd=Wipe Android Virtual Device Data",
    "# {0} - AVD display name",
    "# {1} - stable AVD id",
    "MSG_WipeAvd=Permanently reset user data for {0} ({1})? The AVD will start after the reset. Its configuration and emulator SD card data will be retained.",
    "TTL_DeleteAvd=Delete Android Virtual Device",
    "# {0} - AVD display name",
    "# {1} - stable AVD id",
    "MSG_DeleteAvd=Permanently delete Android Virtual Device {0} ({1})? This cannot be undone.",
    "# {0} - action name",
    "# {1} - selected device or AVD",
    "# {2} - failure reason",
    "MSG_ActionSubmissionFailed=Could not submit {0} for {1}: {2}"
})
public final class FlutterDeviceManagerPanel extends JPanel {
    private final JLabel toolchainTitle = new JLabel();
    private final JLabel toolchainDetail = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final ConnectedDevicesTableModel connectedDevicesModel =
            new ConnectedDevicesTableModel();
    private final AvdsTableModel avdsModel = new AvdsTableModel();
    private final JTable connectedDevicesTable = new JTable(connectedDevicesModel);
    private final JTable avdsTable = new JTable(avdsModel);
    private final Map<DeviceManagerAction, JButton> buttons =
            new EnumMap<>(DeviceManagerAction.class);

    private DeviceManagerSnapshot snapshot = DeviceManagerSnapshot.initial();
    private DeviceManagerSelection selection = DeviceManagerSelection.none();
    private DeviceManagerActionHandler actionHandler;
    private ConfirmationHandler confirmationHandler;
    private boolean synchronizingSelection;

    public FlutterDeviceManagerPanel() {
        this(DeviceManagerActionHandler.NO_OP, ConfirmationHandler.DEFAULT);
    }

    FlutterDeviceManagerPanel(
            DeviceManagerActionHandler actionHandler,
            ConfirmationHandler confirmationHandler) {
        super(new BorderLayout(0, 8));
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
        this.confirmationHandler = Objects.requireNonNull(confirmationHandler, "confirmationHandler");
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(createHeader(), BorderLayout.NORTH);
        add(createTables(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        configureTable(connectedDevicesTable, Bundle.LBL_ConnectedDevices());
        configureTable(avdsTable, Bundle.LBL_VirtualDevices());
        installSelectionListeners();
        applySnapshot(snapshot);
    }

    /** Replaces the non-blocking action bridge used by this panel. */
    public void setActionHandler(DeviceManagerActionHandler actionHandler) {
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
    }

    /** Publishes a complete UI state. Calls from worker threads are marshalled to the EDT. */
    public void setSnapshot(DeviceManagerSnapshot snapshot) {
        DeviceManagerSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> setSnapshot(checked));
            return;
        }
        applySnapshot(checked);
    }

    public DeviceManagerSnapshot snapshot() {
        return snapshot;
    }

    public DeviceManagerSelection selection() {
        return selection;
    }

    public void requestRefresh() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::requestRefresh);
            return;
        }
        submit(DeviceManagerAction.REFRESH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        JPanel sdk = new JPanel(new GridLayout(0, 1, 0, 2));
        toolchainTitle.setFont(toolchainTitle.getFont().deriveFont(
                toolchainTitle.getFont().getStyle() | java.awt.Font.BOLD));
        sdk.add(toolchainTitle);
        sdk.add(toolchainDetail);
        header.add(sdk, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(createButton(DeviceManagerAction.CREATE, Bundle.LBL_CreateAvd()));
        actions.add(createButton(DeviceManagerAction.REFRESH, Bundle.LBL_RefreshDevices()));
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JSplitPane createTables() {
        JScrollPane connected = titledScrollPane(
                connectedDevicesTable,
                Bundle.LBL_ConnectedDevices(),
                Bundle.TXT_NoDevices());
        JScrollPane avds = titledScrollPane(
                avdsTable,
                Bundle.LBL_VirtualDevices(),
                Bundle.TXT_NoAvds());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, connected, avds);
        split.setResizeWeight(0.45);
        split.setContinuousLayout(true);
        split.setBorder(null);
        return split;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        status.setHorizontalAlignment(SwingConstants.LEADING);
        footer.add(status, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(createButton(DeviceManagerAction.START, Bundle.LBL_StartAvd()));
        actions.add(createButton(DeviceManagerAction.STOP, Bundle.LBL_StopAvd()));
        actions.add(createButton(DeviceManagerAction.RESTART, Bundle.LBL_RestartAvd()));
        actions.add(createButton(DeviceManagerAction.WIPE, Bundle.LBL_WipeAvd()));
        actions.add(createButton(DeviceManagerAction.DELETE, Bundle.LBL_DeleteAvd()));
        actions.add(createButton(DeviceManagerAction.SELECT_TARGET, Bundle.LBL_SelectTarget()));
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private JButton createButton(DeviceManagerAction action, String text) {
        JButton button = new JButton(text);
        button.addActionListener(event -> submit(action));
        button.getAccessibleContext().setAccessibleName(text.replace("...", ""));
        buttons.put(action, button);
        return button;
    }

    private JScrollPane titledScrollPane(JTable table, String title, String emptyDescription) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        scrollPane.getAccessibleContext().setAccessibleName(title);
        scrollPane.getAccessibleContext().setAccessibleDescription(emptyDescription);
        return scrollPane;
    }

    private void configureTable(JTable table, String accessibleName) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getAccessibleContext().setAccessibleName(accessibleName);
    }

    private void installSelectionListeners() {
        connectedDevicesTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || synchronizingSelection) {
                return;
            }
            int row = connectedDevicesTable.getSelectedRow();
            if (row < 0) {
                if (avdsTable.getSelectedRow() < 0) {
                    setSelection(DeviceManagerSelection.none());
                }
                return;
            }
            int modelRow = connectedDevicesTable.convertRowIndexToModel(row);
            setSelection(DeviceManagerSelection.connectedDevice(
                    connectedDevicesModel.deviceAt(modelRow).id()));
        });
        avdsTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || synchronizingSelection) {
                return;
            }
            int row = avdsTable.getSelectedRow();
            if (row < 0) {
                if (connectedDevicesTable.getSelectedRow() < 0) {
                    setSelection(DeviceManagerSelection.none());
                }
                return;
            }
            int modelRow = avdsTable.convertRowIndexToModel(row);
            setSelection(DeviceManagerSelection.avd(avdsModel.avdAt(modelRow).id()));
        });
    }

    private void setSelection(DeviceManagerSelection selection) {
        this.selection = Objects.requireNonNull(selection, "selection");
        synchronizingSelection = true;
        try {
            if (selection.kind() != DeviceManagerSelection.Kind.CONNECTED_DEVICE) {
                connectedDevicesTable.clearSelection();
            }
            if (selection.kind() != DeviceManagerSelection.Kind.AVD) {
                avdsTable.clearSelection();
            }
        } finally {
            synchronizingSelection = false;
        }
        updateActionStates();
        updateStatus();
    }

    private void applySnapshot(DeviceManagerSnapshot snapshot) {
        DeviceManagerSelection previousSelection = selection;
        this.snapshot = snapshot;
        toolchainTitle.setText(DeviceManagerPresentation.toolchainTitle(snapshot.toolchain()));
        toolchainDetail.setText(DeviceManagerPresentation.toolchainDetail(snapshot.toolchain()));
        toolchainDetail.setToolTipText(toolchainDetail.getText());
        connectedDevicesModel.setRows(snapshot.connectedDevices(), snapshot.selectedTargetId());
        avdsModel.setRows(snapshot.avds(), snapshot.selectedTargetId());
        restoreSelection(previousSelection);
        updateActionStates();
        updateStatus();
    }

    private void restoreSelection(DeviceManagerSelection previousSelection) {
        synchronizingSelection = true;
        try {
            connectedDevicesTable.clearSelection();
            avdsTable.clearSelection();
            switch (previousSelection.kind()) {
                case CONNECTED_DEVICE -> selectConnectedDevice(previousSelection.id());
                case AVD -> selectAvd(previousSelection.id());
                case NONE -> selection = DeviceManagerSelection.none();
            }
        } finally {
            synchronizingSelection = false;
        }
    }

    private void selectConnectedDevice(String id) {
        int modelRow = connectedDevicesModel.indexOf(id);
        if (modelRow < 0) {
            selection = DeviceManagerSelection.none();
            return;
        }
        int viewRow = connectedDevicesTable.convertRowIndexToView(modelRow);
        connectedDevicesTable.setRowSelectionInterval(viewRow, viewRow);
        selection = DeviceManagerSelection.connectedDevice(id);
    }

    private void selectAvd(String id) {
        int modelRow = avdsModel.indexOf(id);
        if (modelRow < 0) {
            selection = DeviceManagerSelection.none();
            return;
        }
        int viewRow = avdsTable.convertRowIndexToView(modelRow);
        avdsTable.setRowSelectionInterval(viewRow, viewRow);
        selection = DeviceManagerSelection.avd(id);
    }

    private void updateActionStates() {
        for (Map.Entry<DeviceManagerAction, JButton> entry : buttons.entrySet()) {
            DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                    snapshot,
                    selection,
                    entry.getKey());
            entry.getValue().setEnabled(state.enabled());
            entry.getValue().setToolTipText(state.enabled() ? null : state.reason());
            entry.getValue().getAccessibleContext().setAccessibleDescription(
                    state.enabled() ? entry.getValue().getText() : state.reason());
        }
    }

    private void updateStatus() {
        String message = snapshot.message();
        if (message.isBlank()) {
            message = switch (selection.kind()) {
                case NONE -> snapshot.refreshing()
                        ? "Refreshing connected devices and Android Virtual Devices."
                        : "Select a connected Android device or Android Virtual Device.";
                case CONNECTED_DEVICE -> connectedDeviceStatus(selection.id());
                case AVD -> avdStatus(selection.id());
            };
        }
        status.setText(message.isBlank() ? " " : message);
        status.setToolTipText(message.isBlank() ? null : message);
    }

    private String connectedDeviceStatus(String id) {
        ConnectedAndroidDevice device = snapshot.connectedDevice(id);
        return device == null
                ? "Connected Android device " + id + " is no longer available."
                : DeviceManagerPresentation.selectionLabel(snapshot, selection) + " — "
                        + DeviceManagerPresentation.connectedDeviceState(device);
    }

    private String avdStatus(String id) {
        AndroidVirtualDevice avd = snapshot.avd(id);
        return avd == null
                ? "Android Virtual Device " + id + " is no longer available."
                : DeviceManagerPresentation.selectionLabel(snapshot, selection) + " — "
                        + DeviceManagerPresentation.avdState(avd);
    }

    private void submit(DeviceManagerAction action) {
        DeviceManagerActionState state = DeviceManagerPresentation.actionState(
                snapshot,
                selection,
                action);
        if (!state.enabled()) {
            status.setText(state.reason());
            return;
        }
        if (!confirmIfDestructive(action)) {
            return;
        }
        DeviceManagerSelection submittedSelection = isGlobal(action)
                ? DeviceManagerSelection.none()
                : selection;
        try {
            actionHandler.perform(action, submittedSelection);
        } catch (RuntimeException failure) {
            String cause = failure.getMessage() == null || failure.getMessage().isBlank()
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage();
            status.setText(Bundle.MSG_ActionSubmissionFailed(
                    action.name(),
                    DeviceManagerPresentation.selectionLabel(snapshot, submittedSelection),
                    cause));
        }
    }

    private boolean confirmIfDestructive(DeviceManagerAction action) {
        if (action != DeviceManagerAction.WIPE && action != DeviceManagerAction.DELETE) {
            return true;
        }
        AndroidVirtualDevice avd = snapshot.avd(selection.id());
        if (avd == null) {
            return false;
        }
        String title = action == DeviceManagerAction.WIPE
                ? Bundle.TTL_WipeAvd()
                : Bundle.TTL_DeleteAvd();
        String message = action == DeviceManagerAction.WIPE
                ? Bundle.MSG_WipeAvd(avd.displayName(), avd.id())
                : Bundle.MSG_DeleteAvd(avd.displayName(), avd.id());
        return confirmationHandler.confirm(this, title, message);
    }

    private static boolean isGlobal(DeviceManagerAction action) {
        return action == DeviceManagerAction.CREATE || action == DeviceManagerAction.REFRESH;
    }

    JButton button(DeviceManagerAction action) {
        return buttons.get(action);
    }

    JTable connectedDevicesTable() {
        return connectedDevicesTable;
    }

    JTable avdsTable() {
        return avdsTable;
    }

    JLabel statusLabel() {
        return status;
    }

    @FunctionalInterface
    interface ConfirmationHandler {
        ConfirmationHandler DEFAULT = (parent, title, message) -> {
            NotifyDescriptor.Confirmation confirmation = new NotifyDescriptor.Confirmation(
                        message,
                        title,
                        NotifyDescriptor.YES_NO_OPTION,
                        NotifyDescriptor.WARNING_MESSAGE);
            return DialogDisplayer.getDefault().notify(confirmation)
                    == NotifyDescriptor.YES_OPTION;
        };

        boolean confirm(Component parent, String title, String message);
    }

    private static final class ConnectedDevicesTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
            Bundle.COL_Device(),
            Bundle.COL_Id(),
            Bundle.COL_Android(),
            Bundle.COL_State(),
            Bundle.COL_Target()
        };
        private List<ConnectedAndroidDevice> rows = List.of();
        private String selectedTargetId = "";

        void setRows(List<ConnectedAndroidDevice> rows, String selectedTargetId) {
            this.rows = List.copyOf(rows);
            this.selectedTargetId = selectedTargetId;
            fireTableDataChanged();
        }

        ConnectedAndroidDevice deviceAt(int row) {
            return rows.get(row);
        }

        int indexOf(String id) {
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).id().equals(id)) {
                    return index;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ConnectedAndroidDevice device = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> device.displayName();
                case 1 -> device.id();
                case 2 -> androidLabel(device);
                case 3 -> DeviceManagerPresentation.connectedDeviceState(device);
                case 4 -> device.id().equals(selectedTargetId) ? Bundle.TXT_SelectedTarget() : "";
                default -> throw new IndexOutOfBoundsException("column " + columnIndex);
            };
        }

        private static String androidLabel(ConnectedAndroidDevice device) {
            if (device.androidVersion().isBlank()) {
                return device.apiLevel().isBlank() ? "" : "API " + device.apiLevel();
            }
            return device.apiLevel().isBlank()
                    ? device.androidVersion()
                    : device.androidVersion() + " (API " + device.apiLevel() + ")";
        }
    }

    private static final class AvdsTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
            Bundle.COL_Avd(),
            Bundle.COL_Profile(),
            Bundle.COL_Api(),
            Bundle.COL_Architecture(),
            Bundle.COL_State(),
            Bundle.COL_ConnectedTarget()
        };
        private List<AndroidVirtualDevice> rows = List.of();
        private String selectedTargetId = "";

        void setRows(List<AndroidVirtualDevice> rows, String selectedTargetId) {
            this.rows = List.copyOf(rows);
            this.selectedTargetId = selectedTargetId;
            fireTableDataChanged();
        }

        AndroidVirtualDevice avdAt(int row) {
            return rows.get(row);
        }

        int indexOf(String id) {
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).id().equals(id)) {
                    return index;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AndroidVirtualDevice avd = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> avd.displayName();
                case 1 -> avd.deviceProfile();
                case 2 -> avd.apiLevel();
                case 3 -> avd.architecture();
                case 4 -> DeviceManagerPresentation.avdState(avd);
                case 5 -> connectedTarget(avd);
                default -> throw new IndexOutOfBoundsException("column " + columnIndex);
            };
        }

        private String connectedTarget(AndroidVirtualDevice avd) {
            if (avd.connectedDeviceId().isBlank()) {
                return "";
            }
            return avd.connectedDeviceId().equals(selectedTargetId)
                    ? avd.connectedDeviceId() + " — " + Bundle.TXT_SelectedTarget()
                    : avd.connectedDeviceId();
        }
    }
}
