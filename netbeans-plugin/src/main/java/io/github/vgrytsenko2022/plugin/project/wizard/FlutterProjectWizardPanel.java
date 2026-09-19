package io.github.vgrytsenko2022.plugin.project.wizard;

import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import io.github.vgrytsenko2022.project.FlutterProjectCreationRequest;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.swing.event.ChangeListener;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

final class FlutterProjectWizardPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    static final String PROP_PROJECT_NAME = "flutter.project.name";
    static final String PROP_PARENT_DIRECTORY = "flutter.project.parentDirectory";
    static final String PROP_ORGANIZATION = "flutter.project.organization";
    static final String PROP_DESCRIPTION = "flutter.project.description";

    private final ChangeSupport changes = new ChangeSupport(this);
    private FlutterProjectWizardVisual component;
    private WizardDescriptor wizard;
    private String flutterSdkProblem;

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new FlutterProjectWizardVisual(changes::fireChange);
            component.setName(Bundle.LBL_FlutterProjectWizardStep());
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
        wizard = settings;
        var status = new FlutterToolchainService().resolve();
        flutterSdkProblem = status.flutterSdk().isPresent()
                ? null
                : status.flutterMessage() + " Configure Flutter in Tools > Options > Flutter.";

        FlutterProjectWizardVisual visual = (FlutterProjectWizardVisual) getComponent();
        if (!visual.isInitialized()) {
            visual.initialize(
                    "flutter_app",
                    ProjectChooser.getProjectsFolder().getAbsolutePath(),
                    "com.example",
                    "A new Flutter application.");
        }
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        FlutterProjectWizardVisual visual = (FlutterProjectWizardVisual) getComponent();
        settings.putProperty(PROP_PROJECT_NAME, visual.projectName());
        settings.putProperty(PROP_PARENT_DIRECTORY, visual.parentDirectory());
        settings.putProperty(PROP_ORGANIZATION, visual.organization());
        settings.putProperty(PROP_DESCRIPTION, visual.description());
    }

    @Override
    public boolean isValid() {
        if (wizard == null) {
            return false;
        }
        if (flutterSdkProblem != null) {
            return invalid(flutterSdkProblem);
        }

        FlutterProjectWizardVisual visual = (FlutterProjectWizardVisual) getComponent();
        String projectName = visual.projectName();
        if (!FlutterProjectCreationRequest.isValidProjectName(projectName)) {
            return invalid("Project name must start with a lowercase letter and contain only lowercase letters, digits, and underscores.");
        }
        if (!FlutterProjectCreationRequest.isValidOrganization(visual.organization())) {
            return invalid("Organization must be a reverse-domain identifier such as com.example.");
        }
        if (visual.description().isBlank()) {
            return invalid("Project description is required.");
        }

        final Path parent;
        try {
            parent = Path.of(visual.parentDirectory()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            return invalid("Project location is invalid: " + ex.getReason() + ".");
        }
        if (!Files.isDirectory(parent)) {
            return invalid("Project location does not exist or is not a directory: " + parent + ".");
        }
        if (!Files.isWritable(parent)) {
            return invalid("Project location is not writable: " + parent + ".");
        }

        Path target = parent.resolve(projectName).normalize();
        if (!target.getParent().equals(parent)) {
            return invalid("Project name resolves outside the selected project location.");
        }
        if (Files.exists(target)) {
            return invalid("Target project directory already exists: " + target + ".");
        }

        wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, " ");
        return true;
    }

    private boolean invalid(String message) {
        wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, message);
        return false;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }
}
