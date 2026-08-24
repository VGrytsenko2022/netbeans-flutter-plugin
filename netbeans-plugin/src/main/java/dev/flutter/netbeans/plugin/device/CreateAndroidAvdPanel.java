package dev.flutter.netbeans.plugin.device;

import dev.flutter.netbeans.run.AndroidAvdCreateRequest;
import dev.flutter.netbeans.run.AndroidDeviceDefinition;
import dev.flutter.netbeans.run.AndroidSystemImage;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotificationLineSupport;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/** NetBeans-native form for creating an Android Virtual Device. */
@Messages({
    "TTL_CreateAndroidAvd=Create Android Virtual Device",
    "LBL_AvdName=AVD &Name:",
    "LBL_AvdSystemImage=&System Image:",
    "LBL_AvdDeviceProfile=&Device Profile:",
    "LBL_AvdSdCard=&SD Card Size:",
    "TXT_DefaultDeviceProfile=<Default hardware profile>",
    "TXT_OptionalSdCard=Optional, for example 512M",
    "ACS_AvdName=Android Virtual Device name",
    "ACD_AvdName=Stable AVD name containing letters, digits, dot, underscore or dash.",
    "ACS_AvdSystemImage=Installed Android system image",
    "ACD_AvdSystemImage=The installed Android system image used by the new virtual device.",
    "ACS_AvdDeviceProfile=Android hardware profile",
    "ACD_AvdDeviceProfile=Optional hardware profile used by the new virtual device.",
    "ACS_AvdSdCard=Virtual SD card size",
    "ACD_AvdSdCard=Optional positive size in K or M, for example 512M."
})
final class CreateAndroidAvdPanel extends JPanel {
    private final JTextField name = new JTextField(30);
    private final JComboBox<AndroidSystemImage> systemImage = new JComboBox<>();
    private final JComboBox<DeviceChoice> deviceProfile = new JComboBox<>();
    private final JTextField sdCardSize = new JTextField(14);
    private final Set<String> existingIds;
    private DialogDescriptor descriptor;
    private NotificationLineSupport notifications;

    CreateAndroidAvdPanel(AndroidDeviceManagerBackend.CreationOptions options) {
        super(new GridBagLayout());
        existingIds = options.existingAvdIds();
        systemImage.setModel(new DefaultComboBoxModel<>(
                options.systemImages().toArray(AndroidSystemImage[]::new)));
        deviceProfile.addItem(DeviceChoice.defaultProfile());
        options.deviceDefinitions().forEach(definition ->
                deviceProfile.addItem(DeviceChoice.of(definition)));
        configureRenderers();
        buildUi();
        installValidationListeners();
        name.setText(suggestName(selectedImage(), existingIds));
    }

    static Optional<AndroidAvdCreateRequest> showDialog(
            AndroidDeviceManagerBackend.CreationOptions options) {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Create Android Virtual Device dialog must be opened on the EDT.");
        }
        CreateAndroidAvdPanel panel = new CreateAndroidAvdPanel(options);
        DialogDescriptor dialog = new DialogDescriptor(
                panel,
                Bundle.TTL_CreateAndroidAvd(),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);
        panel.attach(dialog);
        Object result = DialogDisplayer.getDefault().notify(dialog);
        return result == DialogDescriptor.OK_OPTION
                ? Optional.of(panel.request())
                : Optional.empty();
    }

    AndroidAvdCreateRequest request() {
        String error = validationError(
                name.getText(),
                selectedImage(),
                selectedDeviceId(),
                sdCardSize.getText(),
                existingIds);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return new AndroidAvdCreateRequest(
                name.getText().strip(),
                selectedImage().packageId(),
                selectedDeviceId(),
                optionalText(sdCardSize.getText()),
                false);
    }

    static String validationError(
            String name,
            AndroidSystemImage image,
            Optional<String> deviceDefinitionId,
            String sdCardSize,
            Set<String> existingIds) {
        if (image == null || !image.installed()) {
            return "Cannot create an Android Virtual Device: no installed system image is selected.";
        }
        final AndroidAvdCreateRequest request;
        try {
            request = new AndroidAvdCreateRequest(
                    name == null ? "" : name,
                    image.packageId(),
                    deviceDefinitionId,
                    optionalText(sdCardSize),
                    false);
        } catch (IllegalArgumentException failure) {
            return failure.getMessage() + ".";
        }
        if (existingIds.stream().anyMatch(id -> id.equalsIgnoreCase(request.name()))) {
            return "Android Virtual Device '" + request.name()
                    + "' already exists. Choose a different AVD name.";
        }
        return null;
    }

    private void attach(DialogDescriptor descriptor) {
        this.descriptor = descriptor;
        notifications = descriptor.createNotificationLineSupport();
        updateValidation();
    }

    private void buildUi() {
        addRow(0, Bundle.LBL_AvdName(), name);
        addRow(1, Bundle.LBL_AvdSystemImage(), systemImage);
        addRow(2, Bundle.LBL_AvdDeviceProfile(), deviceProfile);
        addRow(3, Bundle.LBL_AvdSdCard(), sdCardSize);

        JLabel hint = new JLabel(Bundle.TXT_OptionalSdCard());
        GridBagConstraints hintConstraints = constraints(1, 4);
        hintConstraints.gridwidth = 2;
        hintConstraints.weightx = 1;
        hintConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(hint, hintConstraints);

        name.getAccessibleContext().setAccessibleName(Bundle.ACS_AvdName());
        name.getAccessibleContext().setAccessibleDescription(Bundle.ACD_AvdName());
        systemImage.getAccessibleContext().setAccessibleName(Bundle.ACS_AvdSystemImage());
        systemImage.getAccessibleContext().setAccessibleDescription(Bundle.ACD_AvdSystemImage());
        deviceProfile.getAccessibleContext().setAccessibleName(Bundle.ACS_AvdDeviceProfile());
        deviceProfile.getAccessibleContext().setAccessibleDescription(Bundle.ACD_AvdDeviceProfile());
        sdCardSize.getAccessibleContext().setAccessibleName(Bundle.ACS_AvdSdCard());
        sdCardSize.getAccessibleContext().setAccessibleDescription(Bundle.ACD_AvdSdCard());
    }

    private void addRow(int row, String labelText, java.awt.Component field) {
        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, labelText);
        label.setLabelFor(field);
        add(label, constraints(0, row));

        GridBagConstraints fieldConstraints = constraints(1, row);
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(field, fieldConstraints);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.LINE_START;
        return constraints;
    }

    private void configureRenderers() {
        systemImage.setRenderer(renderer((image) -> image == null
                ? ""
                : "API " + image.apiLevel() + " — " + image.tag() + " — "
                        + image.abi() + " (" + image.packageId() + ")"));
        deviceProfile.setRenderer(renderer(choice -> choice == null
                ? ""
                : choice.displayName()));
    }

    private static <T> ListCellRenderer<T> renderer(
            java.util.function.Function<T, String> label) {
        return (JList<? extends T> list, T value, int index,
                boolean selected, boolean focused) -> {
            JLabel component = new JLabel(label.apply(value));
            component.setOpaque(true);
            component.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
            component.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            component.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return component;
        };
    }

    private void installValidationListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateValidation(); }
            @Override public void removeUpdate(DocumentEvent event) { updateValidation(); }
            @Override public void changedUpdate(DocumentEvent event) { updateValidation(); }
        };
        name.getDocument().addDocumentListener(listener);
        sdCardSize.getDocument().addDocumentListener(listener);
        systemImage.addActionListener(event -> updateValidation());
        deviceProfile.addActionListener(event -> updateValidation());
    }

    private void updateValidation() {
        if (descriptor == null) {
            return;
        }
        String error = validationError(
                name.getText(),
                selectedImage(),
                selectedDeviceId(),
                sdCardSize.getText(),
                existingIds);
        descriptor.setValid(error == null);
        if (error == null) {
            notifications.clearMessages();
        } else {
            notifications.setErrorMessage(error);
        }
    }

    private AndroidSystemImage selectedImage() {
        return (AndroidSystemImage) systemImage.getSelectedItem();
    }

    private Optional<String> selectedDeviceId() {
        DeviceChoice choice = (DeviceChoice) deviceProfile.getSelectedItem();
        return choice == null ? Optional.empty() : choice.id();
    }

    private static Optional<String> optionalText(String value) {
        return value == null || value.isBlank()
                ? Optional.empty()
                : Optional.of(value.strip());
    }

    private static String suggestName(AndroidSystemImage image, Set<String> existingIds) {
        String base = image == null
                ? "Android_Device"
                : "Android_API_" + image.apiLevel() + "_" + image.abi();
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String candidate = base;
        int suffix = 2;
        while (existingIds.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private record DeviceChoice(Optional<String> id, String displayName) {
        private DeviceChoice {
            id = id == null ? Optional.empty() : id;
            displayName = displayName == null ? "" : displayName;
        }

        static DeviceChoice defaultProfile() {
            return new DeviceChoice(Optional.empty(), Bundle.TXT_DefaultDeviceProfile());
        }

        static DeviceChoice of(AndroidDeviceDefinition definition) {
            String manufacturer = definition.manufacturer().isBlank()
                    ? ""
                    : definition.manufacturer() + " — ";
            return new DeviceChoice(
                    Optional.of(definition.id()),
                    manufacturer + definition.displayName() + " (" + definition.id() + ")");
        }
    }
}
