package io.github.vgrytsenko2022.plugin.project.wizard;

import io.github.vgrytsenko2022.plugin.project.FlutterProjectFactory;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import io.github.vgrytsenko2022.project.FlutterProjectCreationRequest;
import io.github.vgrytsenko2022.project.FlutterProjectCreator;
import io.github.vgrytsenko2022.project.FlutterProjectPlatform;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.openide.util.ChangeSupport;
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
    "LBL_FlutterProjectPlatformsWizardStep=Target Platforms",
    "LBL_FlutterProjectWizardTitle=New Flutter Application",
    "LBL_ProjectName=Project &Name:",
    "LBL_ProjectLocation=Project &Location:",
    "LBL_BrowseProjectLocation=&Browse...",
    "LBL_ProjectFolder=Project &Folder:",
    "LBL_Organization=&Organization:",
    "LBL_Description=&Description:",
    "LBL_CreateHint=The next step selects target platform folders. The wizard runs the configured Flutter SDK's \"flutter create --template app --platforms=...\" command and opens lib/main.dart.",
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
    "ACD_Description=Description written to the generated pubspec.yaml.",
    "LBL_PlatformPreset=&Preset:",
    "LBL_PlatformPresetRecommended=Recommended",
    "LBL_PlatformPresetMobile=Mobile",
    "LBL_PlatformPresetDesktop=Desktop",
    "LBL_PlatformPresetWeb=Web",
    "LBL_PlatformPresetAll=All",
    "LBL_PlatformPresetCustom=Custom",
    "LBL_PlatformAndroid=&Android",
    "LBL_PlatformIos=&iOS",
    "LBL_PlatformWeb=&Web",
    "LBL_PlatformWindows=Wi&ndows",
    "LBL_PlatformMacos=&macOS",
    "LBL_PlatformLinux=&Linux",
    "LBL_PlatformSelectionHint=<html><p width='420'>Platform folders can be generated on any host. Building iOS/macOS requires macOS and Xcode; Windows requires Windows and Visual Studio; Linux requires Linux desktop packages; Android requires the Android SDK. Web has no host restriction.</p></html>",
    "ACS_PlatformPreset=Flutter platform preset",
    "ACD_PlatformPreset=Select a recommended platform group or customize the individual target platforms.",
    "ACS_PlatformSelection=Flutter target platforms",
    "ACD_PlatformSelection=Selected platform folders that flutter create will generate."
})
public final class FlutterProjectWizardIterator
        implements WizardDescriptor.ProgressInstantiatingIterator<WizardDescriptor> {

    private WizardDescriptor wizard;
    private final ChangeSupport changes = new ChangeSupport(this);
    private List<WizardDescriptor.Panel<WizardDescriptor>> panels = List.of();
    private int panelIndex;

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
                    stringProperty(FlutterProjectWizardPanel.PROP_DESCRIPTION),
                    platformProperty(FlutterProjectPlatformsWizardPanel.PROP_PLATFORMS));

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
        panelIndex = 0;
        panels = List.of(
                new FlutterProjectWizardPanel(),
                new FlutterProjectPlatformsWizardPanel());
        descriptor.putProperty("NewProjectWizard_Title", Bundle.LBL_FlutterProjectWizardTitle());
        String[] steps = {
            Bundle.LBL_FlutterProjectWizardStep(),
            Bundle.LBL_FlutterProjectPlatformsWizardStep()
        };
        for (int index = 0; index < panels.size(); index++) {
            JComponent component = (JComponent) panels.get(index).getComponent();
            component.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
            component.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);
            component.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
            component.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
            component.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
        }
    }

    @Override
    public void uninitialize(WizardDescriptor descriptor) {
        wizard = null;
        panels = List.of();
        panelIndex = 0;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        if (panels.isEmpty()) {
            throw new NoSuchElementException("Flutter project wizard is not initialized");
        }
        return panels.get(panelIndex);
    }

    @Override
    public String name() {
        return ((JComponent) current().getComponent()).getName();
    }

    @Override
    public boolean hasNext() {
        return panelIndex + 1 < panels.size();
    }

    @Override
    public boolean hasPrevious() {
        return panelIndex > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        panelIndex++;
        changes.fireChange();
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        panelIndex--;
        changes.fireChange();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
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

    private Set<FlutterProjectPlatform> platformProperty(String key) throws IOException {
        Object value = wizard.getProperty(key);
        if (!(value instanceof Set<?> values) || values.isEmpty()) {
            throw new IOException("Flutter application cannot be created: select at least one target platform.");
        }
        EnumSet<FlutterProjectPlatform> selected =
                EnumSet.noneOf(FlutterProjectPlatform.class);
        for (Object candidate : values) {
            if (!(candidate instanceof FlutterProjectPlatform platform)) {
                throw new IOException("Flutter application cannot be created: wizard platform selection is invalid.");
            }
            selected.add(platform);
        }
        return FlutterProjectPlatform.copyOf(selected);
    }
}
