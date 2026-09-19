package io.github.vgrytsenko2022.plugin.options;

import io.github.vgrytsenko2022.api.DartSdk;
import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainConfig;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainStatus;
import io.github.vgrytsenko2022.sdk.DartSdkLocator;
import io.github.vgrytsenko2022.sdk.FlutterSdkLocator;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

@OptionsPanelController.Keywords(
        keywords = {"#FlutterOptionsPanel_Keywords"},
        location = FlutterOptionsPanelController.ID,
        tabTitle = "#OptionsCategory_Name_Flutter")
final class FlutterOptionsPanel extends JPanel {
    private final JTextField flutterHome = new JTextField(36);
    private final JTextField dartHome = new JTextField(36);
    private final JCheckBox useBundledDart = new JCheckBox();
    private final JLabel dartLocationLabel = new JLabel();
    private final JTextArea dartHint = wrappingArea(2, false);
    private final JButton browseFlutter = new JButton();
    private final JButton browseDart = new JButton();
    private final JButton detect = new JButton();
    private final JButton validate = new JButton();
    private final JTextArea flutterStatus = wrappingArea(3, true);
    private final JTextArea dartStatus = wrappingArea(3, true);

    private final Runnable changeCallback;
    private final FlutterToolchainService toolchainService = new FlutterToolchainService();
    private boolean loading;
    private volatile boolean configurationValid = true;

    FlutterOptionsPanel(Runnable changeCallback) {
        this.changeCallback = changeCallback;
        initComponents();
        connectListeners();
    }

    void load(FlutterToolchainConfig config) {
        loading = true;
        try {
            flutterHome.setText(config.flutterHome());
            dartHome.setText(config.dartHome());
            useBundledDart.setSelected(config.useBundledDart());
            updateDartControls();
            refreshStatus();
        } finally {
            loading = false;
        }
    }

    FlutterToolchainConfig configuration() {
        return new FlutterToolchainConfig(
                flutterHome.getText(),
                useBundledDart.isSelected(),
                dartHome.getText());
    }

    boolean isConfigurationValid() {
        return configurationValid;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = constraints();

        JTextArea introduction = wrappingArea(2, false);
        introduction.setText(text("FlutterOptions_Introduction"));
        addRow(form, introduction, c, 0, 0, 3, 1.0);

        JLabel flutterHeading = heading("FlutterOptions_FlutterHeading");
        addRow(form, flutterHeading, c, 0, 1, 3, 1.0);

        JLabel flutterLabel = new JLabel();
        Mnemonics.setLocalizedText(flutterLabel, text("FlutterOptions_FlutterLocation"));
        flutterLabel.setLabelFor(flutterHome);
        addRow(form, flutterLabel, c, 0, 2, 1, 0.0);
        addRow(form, flutterHome, c, 1, 2, 1, 1.0);
        Mnemonics.setLocalizedText(browseFlutter, text("FlutterOptions_BrowseFlutter"));
        addRow(form, browseFlutter, c, 2, 2, 1, 0.0);

        JTextArea flutterHint = wrappingArea(2, false);
        flutterHint.setText(text("FlutterOptions_FlutterHint"));
        addRow(form, flutterHint, c, 1, 3, 2, 1.0);
        addRow(form, flutterStatus, c, 1, 4, 2, 1.0);

        JLabel dartHeading = heading("FlutterOptions_DartHeading");
        addRow(form, dartHeading, c, 0, 5, 3, 1.0);

        Mnemonics.setLocalizedText(useBundledDart, text("FlutterOptions_UseBundledDart"));
        addRow(form, useBundledDart, c, 1, 6, 2, 1.0);

        Mnemonics.setLocalizedText(dartLocationLabel, text("FlutterOptions_DartLocation"));
        dartLocationLabel.setLabelFor(dartHome);
        addRow(form, dartLocationLabel, c, 0, 7, 1, 0.0);
        addRow(form, dartHome, c, 1, 7, 1, 1.0);
        Mnemonics.setLocalizedText(browseDart, text("FlutterOptions_BrowseDart"));
        addRow(form, browseDart, c, 2, 7, 1, 0.0);

        dartHint.setText(text("FlutterOptions_DartHint"));
        addRow(form, dartHint, c, 1, 8, 2, 1.0);
        addRow(form, dartStatus, c, 1, 9, 2, 1.0);

        JPanel actions = new JPanel();
        actions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));
        Mnemonics.setLocalizedText(detect, text("FlutterOptions_Detect"));
        Mnemonics.setLocalizedText(validate, text("FlutterOptions_Validate"));
        actions.add(detect);
        actions.add(javax.swing.Box.createHorizontalStrut(8));
        actions.add(validate);
        addRow(form, actions, c, 1, 10, 2, 1.0);

        JPanel topAligned = new JPanel(new BorderLayout());
        topAligned.add(form, BorderLayout.NORTH);
        add(topAligned, BorderLayout.CENTER);

        flutterHome.getAccessibleContext().setAccessibleDescription(text("FlutterOptions_FlutterAccessible"));
        dartHome.getAccessibleContext().setAccessibleDescription(text("FlutterOptions_DartAccessible"));
        useBundledDart.getAccessibleContext().setAccessibleDescription(text("FlutterOptions_BundledAccessible"));
        browseFlutter.getAccessibleContext().setAccessibleDescription(text("FlutterOptions_BrowseFlutterAccessible"));
        browseDart.getAccessibleContext().setAccessibleDescription(text("FlutterOptions_BrowseDartAccessible"));
        flutterStatus.getAccessibleContext().setAccessibleName(text("FlutterOptions_FlutterStatusAccessible"));
        dartStatus.getAccessibleContext().setAccessibleName(text("FlutterOptions_DartStatusAccessible"));
    }

    private void connectListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { contentChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { contentChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { contentChanged(); }
        };
        flutterHome.getDocument().addDocumentListener(listener);
        dartHome.getDocument().addDocumentListener(listener);

        useBundledDart.addActionListener(e -> {
            updateDartControls();
            contentChanged();
        });
        browseFlutter.addActionListener(e -> chooseDirectory(flutterHome, "FlutterOptions_SelectFlutter"));
        browseDart.addActionListener(e -> chooseDirectory(dartHome, "FlutterOptions_SelectDart"));
        detect.addActionListener(e -> detectAutomatically());
        validate.addActionListener(e -> validatePaths());
    }

    private void contentChanged() {
        if (loading) return;
        refreshStatus();
        changeCallback.run();
    }

    private void refreshStatus() {
        FlutterToolchainStatus status = toolchainService.resolve(configuration());
        flutterStatus.setText(status.flutterMessage());
        dartStatus.setText(status.dartMessage());
        flutterStatus.setCaretPosition(0);
        dartStatus.setCaretPosition(0);
        flutterStatus.setToolTipText(status.flutterMessage());
        dartStatus.setToolTipText(status.dartMessage());
        flutterHome.setToolTipText(status.flutterMessage());
        dartHome.setToolTipText(status.dartMessage());
        configurationValid = status.validForSave();
    }

    private void validatePaths() {
        refreshStatus();
        changeCallback.run();
        focusInvalidField();
    }

    private void detectAutomatically() {
        Optional<FlutterSdk> flutter = new FlutterSdkLocator().locate();
        flutter.ifPresent(sdk -> flutterHome.setText(sdk.home().toString()));

        DartSdkLocator dartLocator = new DartSdkLocator();
        Optional<DartSdk> bundled = flutter.flatMap(dartLocator::fromFlutterSdk);
        dartLocator.detect(flutter).ifPresent(detection -> {
            DartSdk sdk = detection.sdk();
            boolean isBundled = bundled.isPresent() && bundled.get().home().equals(sdk.home());
            useBundledDart.setSelected(isBundled);
            if (!isBundled) {
                dartHome.setText(sdk.home().toString());
            }
        });
        updateDartControls();
        contentChanged();
    }

    private void chooseDirectory(JTextField target, String titleKey) {
        JFileChooser chooser = new JFileChooser(initialDirectory(target.getText()));
        chooser.setDialogTitle(text(titleKey));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
        }
    }

    private java.io.File initialDirectory(String value) {
        if (value != null && !value.isBlank()) {
            try {
                Path path = Path.of(value);
                if (java.nio.file.Files.isDirectory(path)) return path.toFile();
            } catch (java.nio.file.InvalidPathException ex) {
                // Fall back to the user's home directory.
            }
        }
        return new java.io.File(System.getProperty("user.home", "."));
    }

    private void updateDartControls() {
        boolean custom = !useBundledDart.isSelected();
        dartHome.setEnabled(custom);
        browseDart.setEnabled(custom);
        dartLocationLabel.setVisible(custom);
        dartHome.setVisible(custom);
        browseDart.setVisible(custom);
        dartHint.setVisible(custom);
        revalidate();
        repaint();
    }

    private void focusInvalidField() {
        FlutterToolchainStatus status = toolchainService.resolve(configuration());
        if (!configuration().flutterHome().isBlank() && status.flutterSdk().isEmpty()) {
            flutterHome.requestFocusInWindow();
            flutterHome.selectAll();
        } else if (!configuration().useBundledDart()
                && !configuration().dartHome().isBlank()
                && status.dartSdk().isEmpty()) {
            dartHome.requestFocusInWindow();
            dartHome.selectAll();
        }
    }

    private static JLabel heading(String key) {
        JLabel label = new JLabel(text(key));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private static JTextArea wrappingArea(int rows, boolean focusable) {
        JTextArea area = new JTextArea(rows, 20);
        area.setEditable(false);
        area.setFocusable(focusable);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setFont(javax.swing.UIManager.getFont("Label.font"));
        area.setForeground(javax.swing.UIManager.getColor("Label.foreground"));
        return area;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        return c;
    }

    private static void addRow(
            JPanel panel,
            java.awt.Component component,
            GridBagConstraints template,
            int x,
            int y,
            int width,
            double weightX) {
        GridBagConstraints c = (GridBagConstraints) template.clone();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.weightx = weightX;
        panel.add(component, c);
    }

    private static String text(String key) {
        return NbBundle.getMessage(FlutterOptionsPanel.class, key);
    }
}
