package io.github.vgrytsenko2022.plugin.options;

import io.github.vgrytsenko2022.plugin.settings.FlutterSettings;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainConfig;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.TopLevelRegistration(
        id = FlutterOptionsPanelController.ID,
        categoryName = "#OptionsCategory_Name_Flutter",
        iconBase = "io/github/vgrytsenko2022/plugin/options/flutter32.svg",
        keywords = "#OptionsCategory_Keywords_Flutter",
        keywordsCategory = FlutterOptionsPanelController.ID,
        position = 620)
public final class FlutterOptionsPanelController extends OptionsPanelController {
    public static final String ID = "Flutter";

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final FlutterSettings settings = FlutterSettings.getDefault();

    private FlutterOptionsPanel panel;
    private volatile FlutterToolchainConfig loaded = FlutterToolchainConfig.defaults();
    private volatile FlutterToolchainConfig candidate = FlutterToolchainConfig.defaults();
    private volatile boolean valid = true;
    private volatile boolean changed;

    @Override
    public void update() {
        loaded = settings.load();
        candidate = loaded;
        changed = false;
        if (panel != null) {
            panel.load(loaded);
            valid = panel.isConfigurationValid();
        }
    }

    @Override
    public void applyChanges() {
        FlutterToolchainConfig snapshot = candidate;
        settings.save(snapshot);
        loaded = snapshot;
        boolean oldChanged = changed;
        changed = false;
        if (oldChanged) changes.firePropertyChange(PROP_CHANGED, true, false);
    }

    @Override
    public void cancel() {
        boolean oldChanged = changed;
        candidate = loaded;
        changed = false;
        if (panel != null) {
            panel.load(loaded);
            valid = panel.isConfigurationValid();
        }
        if (oldChanged) changes.firePropertyChange(PROP_CHANGED, true, false);
        changes.firePropertyChange(PROP_VALID, null, null);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        if (panel == null) {
            panel = new FlutterOptionsPanel(this::panelChanged);
            panel.load(loaded);
            valid = panel.isConfigurationValid();
        }
        return panel;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private void panelChanged() {
        FlutterToolchainConfig newCandidate = panel.configuration();
        boolean oldChanged = changed;
        boolean oldValid = valid;

        candidate = newCandidate;
        changed = !newCandidate.equals(loaded);
        valid = panel.isConfigurationValid();

        if (oldChanged != changed) {
            changes.firePropertyChange(PROP_CHANGED, oldChanged, changed);
        }
        // NetBeans refreshes the OK/Apply state on PROP_VALID. The standard
        // Options controller pattern fires it after every UI change, including
        // valid-to-valid edits.
        changes.firePropertyChange(PROP_VALID, oldValid, valid);
        if (oldValid == valid) changes.firePropertyChange(PROP_VALID, null, null);
    }
}
