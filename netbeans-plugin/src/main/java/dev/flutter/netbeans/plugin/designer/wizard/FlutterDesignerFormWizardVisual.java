package dev.flutter.netbeans.plugin.designer.wizard;

import dev.flutter.netbeans.plugin.dart.wizard.DartClassNaming;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.awt.Mnemonics;

final class FlutterDesignerFormWizardVisual extends JPanel {
    private final JTextField className = new JTextField(28);
    private final JTextField location = new JTextField(28);
    private final JTextField dartFile = new JTextField(28);
    private final JTextField modelFile = new JTextField(28);
    private final JButton browse = new JButton();
    private final Runnable changeCallback;
    private Path projectRoot;
    private Path libRoot;
    private boolean initialized;

    FlutterDesignerFormWizardVisual(Runnable changeCallback) {
        this.changeCallback = changeCallback;
        buildUi();
        installListeners();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints base = new GridBagConstraints();
        base.insets = new Insets(5, 6, 5, 6);
        base.anchor = GridBagConstraints.LINE_START;

        addRow(0, label(Bundle.LBL_DesignerClassName(), className),
                className, null, base);
        Mnemonics.setLocalizedText(browse, Bundle.LBL_BrowseDesignerLocation());
        addRow(1, label(Bundle.LBL_DesignerLocation(), location),
                location, browse, base);

        dartFile.setEditable(false);
        modelFile.setEditable(false);
        addRow(2, label(Bundle.LBL_DesignerDartFile(), dartFile),
                dartFile, null, base);
        addRow(3, label(Bundle.LBL_DesignerModelFile(), modelFile),
                modelFile, null, base);

        JLabel hint = new JLabel(Bundle.LBL_DesignerFormHint());
        GridBagConstraints hintConstraints = (GridBagConstraints) base.clone();
        hintConstraints.gridx = 0;
        hintConstraints.gridy = 4;
        hintConstraints.gridwidth = 3;
        hintConstraints.weightx = 1;
        hintConstraints.weighty = 1;
        hintConstraints.fill = GridBagConstraints.HORIZONTAL;
        hintConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        add(hint, hintConstraints);

        className.getAccessibleContext().setAccessibleName(
                Bundle.ACS_DesignerClassName());
        className.getAccessibleContext().setAccessibleDescription(
                Bundle.ACD_DesignerClassName());
        location.getAccessibleContext().setAccessibleName(
                Bundle.ACS_DesignerLocation());
        location.getAccessibleContext().setAccessibleDescription(
                Bundle.ACD_DesignerLocation());
        browse.getAccessibleContext().setAccessibleName(
                Bundle.ACS_BrowseDesignerLocation());
        browse.getAccessibleContext().setAccessibleDescription(
                Bundle.ACD_BrowseDesignerLocation());
        dartFile.getAccessibleContext().setAccessibleName(
                Bundle.ACS_DesignerDartFile());
        modelFile.getAccessibleContext().setAccessibleName(
                Bundle.ACS_DesignerModelFile());
    }

    private static JLabel label(String text, JTextField field) {
        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, text);
        label.setLabelFor(field);
        return label;
    }

    private void addRow(
            int row,
            JLabel label,
            JTextField field,
            JButton button,
            GridBagConstraints base) {
        GridBagConstraints labelConstraints = (GridBagConstraints) base.clone();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        add(label, labelConstraints);

        GridBagConstraints fieldConstraints = (GridBagConstraints) base.clone();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(field, fieldConstraints);

        if (button != null) {
            GridBagConstraints buttonConstraints = (GridBagConstraints) base.clone();
            buttonConstraints.gridx = 2;
            buttonConstraints.gridy = row;
            add(button, buttonConstraints);
        }
    }

    private void installListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { changed(); }
            @Override public void removeUpdate(DocumentEvent event) { changed(); }
            @Override public void changedUpdate(DocumentEvent event) { changed(); }
        };
        className.getDocument().addDocumentListener(listener);
        location.getDocument().addDocumentListener(listener);
        browse.addActionListener(event -> chooseLocation());
    }

    private void changed() {
        updateCreatedFiles();
        changeCallback.run();
    }

    private void updateCreatedFiles() {
        if (projectRoot == null || libRoot == null
                || !DartClassNaming.isValidClassName(className())) {
            dartFile.setText("");
            modelFile.setText("");
            return;
        }
        try {
            String relative = relativeLocation().replace('\\', '/');
            String fileName = DartClassNaming.fileName(className());
            Path relativeDart = relative.isBlank()
                    ? Path.of(fileName)
                    : Path.of(relative).resolve(fileName);
            dartFile.setText(libRoot.resolve(relativeDart).normalize().toString());
            String modelName = fileName.substring(0, fileName.length() - ".dart".length())
                    + ".fd";
            Path relativeModel = relative.isBlank()
                    ? Path.of(modelName)
                    : Path.of(relative).resolve(modelName);
            modelFile.setText(projectRoot.resolve(".fd_templates")
                    .resolve(relativeModel).normalize().toString());
        } catch (InvalidPathException ex) {
            dartFile.setText("");
            modelFile.setText("");
        }
    }

    private void chooseLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Bundle.TTL_SelectDesignerLocation());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        File initial = libRoot == null
                ? null
                : libRoot.resolve(relativeLocation()).normalize().toFile();
        if (initial != null && initial.isDirectory()) {
            chooser.setCurrentDirectory(initial);
            chooser.setSelectedFile(initial);
        } else if (libRoot != null) {
            chooser.setCurrentDirectory(libRoot.toFile());
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (libRoot != null && selected.startsWith(libRoot)) {
            location.setText(libRoot.relativize(selected).toString());
        }
    }

    void initialize(
            Path root,
            Path sourceRoot,
            String initialClassName,
            String initialLocation) {
        projectRoot = root == null ? null : root.toAbsolutePath().normalize();
        libRoot = sourceRoot == null
                ? null
                : sourceRoot.toAbsolutePath().normalize();
        browse.setEnabled(libRoot != null);
        className.setText(initialClassName);
        location.setText(initialLocation);
        initialized = true;
        updateCreatedFiles();
    }

    boolean isInitialized() {
        return initialized;
    }

    String className() {
        return className.getText().trim();
    }

    String relativeLocation() {
        return location.getText().trim();
    }

    String displayedDartFile() {
        return dartFile.getText();
    }

    String displayedModelFile() {
        return modelFile.getText();
    }
}
