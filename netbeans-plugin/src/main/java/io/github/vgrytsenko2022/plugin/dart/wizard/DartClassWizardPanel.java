package io.github.vgrytsenko2022.plugin.dart.wizard;

import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

final class DartClassWizardPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    static final String PROP_CLASS_NAME = "dart.class.name";
    static final String PROP_LOCATION = "dart.class.location";

    private final FlutterProject project;
    private final ChangeSupport changes = new ChangeSupport(this);
    private DartClassWizardVisual component;
    private WizardDescriptor wizard;
    private String initialLocation;

    DartClassWizardPanel(FlutterProject project, String initialLocation) {
        this.project = project;
        this.initialLocation = initialLocation;
    }

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new DartClassWizardVisual(changes::fireChange);
            component.setName(Bundle.LBL_DartClassWizardStep());
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
        DartClassWizardVisual visual = (DartClassWizardVisual) getComponent();
        if (!visual.isInitialized()) {
            visual.initialize(projectRoot(), "NewClass", initialLocation);
        }
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        DartClassWizardVisual visual = (DartClassWizardVisual) getComponent();
        settings.putProperty(PROP_CLASS_NAME, visual.className());
        settings.putProperty(PROP_LOCATION, visual.relativeLocation());
    }

    @Override
    public boolean isValid() {
        if (wizard == null || !project.getProjectDirectory().isValid()) {
            return false;
        }
        DartClassWizardVisual visual = (DartClassWizardVisual) getComponent();
        if (!DartClassNaming.isValidClassName(visual.className())) {
            return invalid("Class name must use UpperCamelCase, for example OrderRepository. "
                    + "A private class may start with '_'.");
        }

        final Path targetFolder;
        try {
            targetFolder = DartClassWizardIterator.resolveLocation(
                    projectRoot(),
                    visual.relativeLocation());
        } catch (IOException ex) {
            return invalid(ex.getMessage());
        }
        String relative = projectRoot().relativize(targetFolder).toString().replace('\\', '/');

        String fileName = DartClassNaming.fileName(visual.className());
        FileObject existingFolder = FileUtil.toFileObject(targetFolder.toFile());
        if (existingFolder != null && existingFolder.getFileObject(fileName) != null) {
            return invalid("Dart class file already exists: " + targetFolder.resolve(fileName) + ".");
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

    private Path projectRoot() {
        return FileUtil.toFile(project.getProjectDirectory()).toPath().toAbsolutePath().normalize();
    }
}
