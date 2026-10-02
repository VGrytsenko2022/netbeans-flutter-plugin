package io.github.vgrytsenko2022.plugin.designer.wizard;

import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.plugin.dart.wizard.DartClassNaming;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerPairLayout;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

final class FlutterDesignerFormWizardPanel
        implements WizardDescriptor.Panel<WizardDescriptor> {
    static final String PROP_CLASS_NAME = "flutter.designer.class.name";
    static final String PROP_LOCATION = "flutter.designer.class.location";
    static final String PROP_WIDGET_KIND = "flutter.designer.widget.kind";

    private final FlutterProject project;
    private final ChangeSupport changes = new ChangeSupport(this);
    private final String initialLocation;
    private FlutterDesignerFormWizardVisual component;
    private WizardDescriptor wizard;

    FlutterDesignerFormWizardPanel(
            FlutterProject project,
            String initialLocation) {
        this.project = project;
        this.initialLocation = initialLocation;
    }

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new FlutterDesignerFormWizardVisual(changes::fireChange);
            component.setName(Bundle.LBL_DesignerFormWizardStep());
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
        FlutterDesignerFormWizardVisual visual =
                (FlutterDesignerFormWizardVisual) getComponent();
        if (!visual.isInitialized()) {
            Path root = projectRoot();
            visual.initialize(
                    root,
                    libRoot(root),
                    "NewScreen",
                    initialLocation);
            if (settings.getProperty(PROP_WIDGET_KIND) instanceof WidgetClassKind kind) {
                visual.setWidgetKind(kind);
            }
        }
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        FlutterDesignerFormWizardVisual visual =
                (FlutterDesignerFormWizardVisual) getComponent();
        settings.putProperty(PROP_CLASS_NAME, visual.className());
        settings.putProperty(PROP_LOCATION, visual.relativeLocation());
        settings.putProperty(PROP_WIDGET_KIND, visual.widgetKind());
    }

    @Override
    public boolean isValid() {
        if (wizard == null || !project.getProjectDirectory().isValid()) {
            return false;
        }
        Path root = projectRoot();
        if (root == null) {
            return invalid("Flutter Designer Form requires a local Flutter project.");
        }
        FlutterDesignerFormWizardVisual visual =
                (FlutterDesignerFormWizardVisual) getComponent();
        if (!DartClassNaming.isValidClassName(visual.className())) {
            return invalid("Class name must use UpperCamelCase, for example ProfileScreen.");
        }

        final FlutterDesignerFormWizardIterator.TargetPaths paths;
        try {
            paths = FlutterDesignerFormWizardIterator.resolveTargets(
                    root,
                    visual.relativeLocation(),
                    DartClassNaming.fileName(visual.className()));
        } catch (IOException ex) {
            return invalid(ex.getMessage());
        }
        if (Files.exists(paths.dartFile(), LinkOption.NOFOLLOW_LINKS)) {
            return invalid("Dart source already exists: " + paths.dartFile() + ".");
        }
        if (Files.exists(paths.modelFile(), LinkOption.NOFOLLOW_LINKS)) {
            return invalid("Flutter Designer model already exists: "
                    + paths.modelFile() + ".");
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
        return localProjectRoot(project.getProjectDirectory());
    }

    static Path localProjectRoot(FileObject projectDirectory) {
        java.io.File local = FileUtil.toFile(projectDirectory);
        return local == null
                ? null
                : local.toPath().toAbsolutePath().normalize();
    }

    private static Path libRoot(Path projectRoot) {
        return projectRoot == null
                ? null
                : projectRoot.resolve(FlutterDesignerPairLayout.DART_ROOT);
    }
}
