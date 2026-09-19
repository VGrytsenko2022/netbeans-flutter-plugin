package io.github.vgrytsenko2022.plugin.dart.wizard;

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

final class DartClassWizardVisual extends JPanel {
    private final JTextField className = new JTextField(28);
    private final JTextField location = new JTextField(28);
    private final JTextField createdFile = new JTextField(28);
    private final JButton browse = new JButton();
    private final Runnable changeCallback;
    private Path projectRoot;
    private boolean initialized;

    DartClassWizardVisual(Runnable changeCallback) {
        this.changeCallback = changeCallback;
        buildUi();
        installListeners();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints base = new GridBagConstraints();
        base.insets = new Insets(5, 6, 5, 6);
        base.anchor = GridBagConstraints.LINE_START;

        addRow(0, label(Bundle.LBL_DartClassName(), className), className, null, base);
        Mnemonics.setLocalizedText(browse, Bundle.LBL_BrowseDartLocation());
        addRow(1, label(Bundle.LBL_DartClassLocation(), location), location, browse, base);

        createdFile.setEditable(false);
        addRow(2, label(Bundle.LBL_DartCreatedFile(), createdFile), createdFile, null, base);

        JLabel hint = new JLabel(Bundle.LBL_DartClassHint());
        GridBagConstraints hintConstraints = (GridBagConstraints) base.clone();
        hintConstraints.gridx = 0;
        hintConstraints.gridy = 3;
        hintConstraints.gridwidth = 3;
        hintConstraints.weightx = 1;
        hintConstraints.weighty = 1;
        hintConstraints.fill = GridBagConstraints.HORIZONTAL;
        hintConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        add(hint, hintConstraints);

        className.getAccessibleContext().setAccessibleName(Bundle.ACS_DartClassName());
        className.getAccessibleContext().setAccessibleDescription(Bundle.ACD_DartClassName());
        location.getAccessibleContext().setAccessibleName(Bundle.ACS_DartClassLocation());
        location.getAccessibleContext().setAccessibleDescription(Bundle.ACD_DartClassLocation());
        browse.getAccessibleContext().setAccessibleName(Bundle.ACS_BrowseDartLocation());
        browse.getAccessibleContext().setAccessibleDescription(Bundle.ACD_BrowseDartLocation());
        createdFile.getAccessibleContext().setAccessibleName(Bundle.ACS_DartCreatedFile());
        createdFile.getAccessibleContext().setAccessibleDescription(Bundle.ACD_DartCreatedFile());
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
        updateCreatedFile();
        changeCallback.run();
    }

    private void updateCreatedFile() {
        if (projectRoot == null || relativeLocation().isBlank()
                || !DartClassNaming.isValidClassName(className())) {
            createdFile.setText("");
            return;
        }
        try {
            createdFile.setText(projectRoot.resolve(relativeLocation()).normalize()
                    .resolve(DartClassNaming.fileName(className())).toString());
        } catch (InvalidPathException ex) {
            createdFile.setText("");
        }
    }

    private void chooseLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Bundle.TTL_SelectDartClassLocation());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        File initial = projectRoot == null
                ? null
                : projectRoot.resolve(relativeLocation()).normalize().toFile();
        if (initial != null && initial.isDirectory()) {
            chooser.setCurrentDirectory(initial);
            chooser.setSelectedFile(initial);
        } else if (projectRoot != null) {
            chooser.setCurrentDirectory(projectRoot.toFile());
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (projectRoot != null && selected.startsWith(projectRoot)) {
            location.setText(projectRoot.relativize(selected).toString());
        } else {
            location.setText(selected.toString());
        }
    }

    void initialize(Path root, String initialClassName, String initialLocation) {
        projectRoot = root.toAbsolutePath().normalize();
        className.setText(initialClassName);
        location.setText(initialLocation);
        initialized = true;
        updateCreatedFile();
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
}
