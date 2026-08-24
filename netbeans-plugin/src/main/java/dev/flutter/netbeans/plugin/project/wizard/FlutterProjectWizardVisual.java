package dev.flutter.netbeans.plugin.project.wizard;

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

final class FlutterProjectWizardVisual extends JPanel {
    private final JTextField projectName = new JTextField(28);
    private final JTextField projectLocation = new JTextField(28);
    private final JButton browseLocation = new JButton();
    private final JTextField projectFolder = new JTextField(28);
    private final JTextField organization = new JTextField(28);
    private final JTextField description = new JTextField(28);
    private final Runnable changeCallback;
    private boolean initialized;

    FlutterProjectWizardVisual(Runnable changeCallback) {
        this.changeCallback = changeCallback;
        buildUi();
        installListeners();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.LINE_START;

        JLabel nameLabel = label(Bundle.LBL_ProjectName(), projectName);
        addRow(0, nameLabel, projectName, null, c);

        JLabel locationLabel = label(Bundle.LBL_ProjectLocation(), projectLocation);
        Mnemonics.setLocalizedText(browseLocation, Bundle.LBL_BrowseProjectLocation());
        browseLocation.getAccessibleContext().setAccessibleName(Bundle.ACS_BrowseProjectLocation());
        browseLocation.getAccessibleContext().setAccessibleDescription(Bundle.ACD_BrowseProjectLocation());
        addRow(1, locationLabel, projectLocation, browseLocation, c);

        projectFolder.setEditable(false);
        projectFolder.setFocusable(true);
        JLabel folderLabel = label(Bundle.LBL_ProjectFolder(), projectFolder);
        addRow(2, folderLabel, projectFolder, null, c);

        JLabel organizationLabel = label(Bundle.LBL_Organization(), organization);
        addRow(3, organizationLabel, organization, null, c);

        JLabel descriptionLabel = label(Bundle.LBL_Description(), description);
        addRow(4, descriptionLabel, description, null, c);

        JLabel hint = new JLabel(Bundle.LBL_CreateHint());
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        add(hint, c);

        projectName.getAccessibleContext().setAccessibleName(Bundle.ACS_ProjectName());
        projectName.getAccessibleContext().setAccessibleDescription(Bundle.ACD_ProjectName());
        projectLocation.getAccessibleContext().setAccessibleName(Bundle.ACS_ProjectLocation());
        projectLocation.getAccessibleContext().setAccessibleDescription(Bundle.ACD_ProjectLocation());
        projectFolder.getAccessibleContext().setAccessibleName(Bundle.ACS_ProjectFolder());
        projectFolder.getAccessibleContext().setAccessibleDescription(Bundle.ACD_ProjectFolder());
        organization.getAccessibleContext().setAccessibleName(Bundle.ACS_Organization());
        organization.getAccessibleContext().setAccessibleDescription(Bundle.ACD_Organization());
        description.getAccessibleContext().setAccessibleName(Bundle.ACS_Description());
        description.getAccessibleContext().setAccessibleDescription(Bundle.ACD_Description());
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
        projectName.getDocument().addDocumentListener(listener);
        projectLocation.getDocument().addDocumentListener(listener);
        organization.getDocument().addDocumentListener(listener);
        description.getDocument().addDocumentListener(listener);
        browseLocation.addActionListener(event -> chooseProjectLocation());
    }

    private void changed() {
        updateProjectFolder();
        changeCallback.run();
    }

    private void updateProjectFolder() {
        String location = projectLocation.getText().trim();
        String name = projectName.getText().trim();
        if (location.isBlank() || name.isBlank()) {
            projectFolder.setText("");
            return;
        }
        try {
            projectFolder.setText(Path.of(location).toAbsolutePath().normalize().resolve(name).toString());
        } catch (InvalidPathException ex) {
            projectFolder.setText(location + File.separator + name);
        }
    }

    private void chooseProjectLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Bundle.TTL_SelectProjectLocation());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        File current = new File(projectLocation.getText().trim());
        if (current.isDirectory()) {
            chooser.setCurrentDirectory(current);
            chooser.setSelectedFile(current);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            projectLocation.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    void initialize(String name, String location, String org, String projectDescription) {
        projectName.setText(name);
        projectLocation.setText(location);
        organization.setText(org);
        description.setText(projectDescription);
        initialized = true;
        updateProjectFolder();
    }

    boolean isInitialized() {
        return initialized;
    }

    String projectName() {
        return projectName.getText().trim();
    }

    String parentDirectory() {
        return projectLocation.getText().trim();
    }

    String organization() {
        return organization.getText().trim();
    }

    String description() {
        return description.getText().trim();
    }
}
