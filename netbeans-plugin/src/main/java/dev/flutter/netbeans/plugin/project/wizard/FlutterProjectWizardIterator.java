package dev.flutter.netbeans.plugin.project.wizard;

import dev.flutter.netbeans.plugin.project.FlutterProjectFactory;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import dev.flutter.netbeans.project.FlutterProjectCreationRequest;
import dev.flutter.netbeans.project.FlutterProjectCreator;
import dev.flutter.netbeans.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

/** New Project wizard that delegates generation to {@code flutter create}. */
@TemplateRegistration(
        folder = "Project/Flutter",
        position = 100,
        displayName = "#LBL_FlutterApplication",
        iconBase = FlutterProjectFactory.ICON_PATH,
        description = "FlutterApplicationDescription.html",
        requireProject = false)
@Messages({
    "LBL_FlutterApplication=Flutter Application",
    "LBL_FlutterProjectWizardStep=Name and Location",
    "LBL_FlutterProjectWizardTitle=New Flutter Application",
    "LBL_ProjectName=Project &Name:",
    "LBL_ProjectLocation=Project &Location:",
    "LBL_BrowseProjectLocation=&Browse...",
    "LBL_ProjectFolder=Project &Folder:",
    "LBL_Organization=&Organization:",
    "LBL_Description=&Description:",
    "LBL_CreateHint=The wizard runs the configured Flutter SDK's \"flutter create --template app\" command and opens lib/main.dart.",
    "TTL_SelectProjectLocation=Select Flutter Project Location",
    "ACS_ProjectName=Flutter project name",
    "ACD_ProjectName=Lowercase Dart package name for the new Flutter application.",
    "ACS_ProjectLocation=Flutter project location",
    "ACD_ProjectLocation=Existing parent directory in which the Flutter project folder will be created.",
    "ACS_BrowseProjectLocation=Browse for Flutter project location",
    "ACD_BrowseProjectLocation=Choose the existing parent directory for the new Flutter application.",
    "ACS_ProjectFolder=Generated Flutter project folder",
    "ACD_ProjectFolder=Exact directory that flutter create will generate.",
    "ACS_Organization=Flutter organization identifier",
    "ACD_Organization=Reverse-domain organization used for generated application identifiers, for example com.example.",
    "ACS_Description=Flutter project description",
    "ACD_Description=Description written to the generated pubspec.yaml."
})
public final class FlutterProjectWizardIterator
        implements WizardDescriptor.ProgressInstantiatingIterator<WizardDescriptor> {

    private WizardDescriptor wizard;
    private FlutterProjectWizardPanel panel;

    @Override
    public Set<?> instantiate() throws IOException {
        throw new IOException("Flutter project creation requires a progress handle.");
    }

    @Override
    public Set<?> instantiate(ProgressHandle handle) throws IOException {
        handle.start(4);
        try {
            handle.progress("Validating the Flutter SDK", 1);
            var toolchain = new FlutterToolchainService().resolve();
            var flutterSdk = toolchain.flutterSdk().orElseThrow(() -> new IOException(
                    "Flutter application cannot be created: " + toolchain.flutterMessage()
                    + " Configure Flutter in Tools > Options > Flutter."));

            FlutterProjectCreationRequest request = new FlutterProjectCreationRequest(
                    pathProperty(FlutterProjectWizardPanel.PROP_PARENT_DIRECTORY),
                    stringProperty(FlutterProjectWizardPanel.PROP_PROJECT_NAME),
                    stringProperty(FlutterProjectWizardPanel.PROP_ORGANIZATION),
                    stringProperty(FlutterProjectWizardPanel.PROP_DESCRIPTION));

            handle.progress("Running flutter create for " + request.projectName(), 2);
            try {
                new FlutterProjectCreator(new FlutterCli(flutterSdk)).create(request);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Flutter application creation was interrupted for "
                        + request.targetDirectory() + ".", ex);
            } catch (IllegalArgumentException ex) {
                throw new IOException("Flutter application cannot be created at "
                        + request.targetDirectory() + ": " + ex.getMessage(), ex);
            }

            handle.progress("Opening Flutter project " + request.projectName(), 3);
            FileUtil.refreshFor(request.targetDirectory().toFile());
            FileObject projectDirectory = FileUtil.toFileObject(request.targetDirectory().toFile());
            if (projectDirectory == null) {
                throw new IOException("Flutter application was created at "
                        + request.targetDirectory()
                        + ", but NetBeans could not access the generated directory.");
            }

            ProjectManager.getDefault().clearNonProjectCache();
            if (ProjectManager.getDefault().findProject(projectDirectory) == null) {
                throw new IOException("Flutter application was created at "
                        + request.targetDirectory()
                        + ", but NetBeans did not recognize it as a Flutter project.");
            }

            ProjectChooser.setProjectsFolder(request.parentDirectory().toFile());
            Set<FileObject> result = new LinkedHashSet<>();
            result.add(projectDirectory);
            FileObject mainFile = projectDirectory.getFileObject("lib/main.dart");
            if (mainFile != null) {
                result.add(mainFile);
            }
            handle.progress("Flutter application is ready", 4);
            return result;
        } finally {
            handle.finish();
        }
    }

    @Override
    public void initialize(WizardDescriptor descriptor) {
        wizard = descriptor;
        panel = new FlutterProjectWizardPanel();
        descriptor.putProperty("NewProjectWizard_Title", Bundle.LBL_FlutterProjectWizardTitle());
        JComponent component = (JComponent) panel.getComponent();
        component.putClientProperty(
                WizardDescriptor.PROP_CONTENT_DATA,
                new String[]{Bundle.LBL_FlutterProjectWizardStep()});
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        component.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
    }

    @Override
    public void uninitialize(WizardDescriptor descriptor) {
        wizard = null;
        panel = null;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return panel;
    }

    @Override
    public String name() {
        return Bundle.LBL_FlutterProjectWizardStep();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void nextPanel() {
        throw new NoSuchElementException();
    }

    @Override
    public void previousPanel() {
        throw new NoSuchElementException();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        // A single immutable panel is used; its own ChangeSupport drives wizard validity.
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        // A single immutable panel is used; its own ChangeSupport drives wizard validity.
    }

    private String stringProperty(String key) throws IOException {
        Object value = wizard.getProperty(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IOException("Flutter application cannot be created: wizard value '"
                    + key + "' is missing.");
        }
        return text.trim();
    }

    private Path pathProperty(String key) throws IOException {
        String value = stringProperty(key);
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException ex) {
            throw new IOException("Flutter application cannot be created: parent directory is invalid: "
                    + value + ".", ex);
        }
    }
}
