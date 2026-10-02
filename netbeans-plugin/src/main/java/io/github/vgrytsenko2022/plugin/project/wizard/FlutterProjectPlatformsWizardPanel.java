package io.github.vgrytsenko2022.plugin.project.wizard;

import io.github.vgrytsenko2022.project.FlutterProjectPlatform;
import java.awt.Component;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

final class FlutterProjectPlatformsWizardPanel
        implements WizardDescriptor.Panel<WizardDescriptor> {
    static final String PROP_PLATFORMS = "flutter.project.platforms";

    private final ChangeSupport changes = new ChangeSupport(this);
    private FlutterProjectPlatformsVisual component;
    private WizardDescriptor wizard;

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new FlutterProjectPlatformsVisual(changes::fireChange);
            component.setName(Bundle.LBL_FlutterProjectPlatformsWizardStep());
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
        FlutterProjectPlatformsVisual visual =
                (FlutterProjectPlatformsVisual) getComponent();
        Object stored = settings.getProperty(PROP_PLATFORMS);
        if (stored instanceof Set<?> values && values.stream()
                .allMatch(FlutterProjectPlatform.class::isInstance)) {
            java.util.EnumSet<FlutterProjectPlatform> selected =
                    java.util.EnumSet.noneOf(FlutterProjectPlatform.class);
            values.forEach(value -> selected.add((FlutterProjectPlatform) value));
            visual.setSelectedPlatforms(selected);
        } else if (!visual.isInitialized()) {
            visual.setSelectedPlatforms(FlutterProjectPlatform.all());
        }
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        FlutterProjectPlatformsVisual visual =
                (FlutterProjectPlatformsVisual) getComponent();
        settings.putProperty(PROP_PLATFORMS, visual.selectedPlatforms());
    }

    @Override
    public boolean isValid() {
        if (wizard == null) {
            return false;
        }
        FlutterProjectPlatformsVisual visual =
                (FlutterProjectPlatformsVisual) getComponent();
        if (visual.selectedPlatforms().isEmpty()) {
            wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                    "Select at least one Flutter target platform.");
            return false;
        }
        wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, " ");
        return true;
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
