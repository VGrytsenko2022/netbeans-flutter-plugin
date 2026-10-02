package io.github.vgrytsenko2022.plugin.project;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;

final class FlutterProjectInformation implements ProjectInformation {
    static final String DISPLAY_NAME_PROPERTY = "projectDisplayName";

    private final FlutterProject project;
    private final FlutterProjectInfo info;
    private final FlutterProjectMetadata metadata;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private volatile String displayNameOverride;

    FlutterProjectInformation(
            FlutterProject project,
            FlutterProjectInfo info,
            FlutterProjectMetadata metadata) {
        this.project = project;
        this.info = info;
        this.metadata = metadata;
        this.displayNameOverride = normalizedOverride(
                metadata.get(DISPLAY_NAME_PROPERTY, false));
    }

    @Override
    public String getName() {
        return info.name();
    }

    @Override
    public String getDisplayName() {
        String override = displayNameOverride;
        return override == null ? info.name() : override;
    }

    @Override
    public Icon getIcon() {
        return FlutterProjectFactory.projectIcon();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    void renameTo(String newName) throws IOException {
        String override = normalizedOverride(newName);
        if (override == null) {
            throw new IOException("A Flutter project display name must not be blank");
        }
        String oldDisplayName = getDisplayName();
        try {
            metadata.put(DISPLAY_NAME_PROPERTY, override, false);
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
        displayNameOverride = override;
        changes.firePropertyChange(
                PROP_DISPLAY_NAME,
                oldDisplayName,
                getDisplayName());
    }

    boolean isDisplayNamePersisted(String expectedDisplayName) {
        String expectedOverride = normalizedOverride(expectedDisplayName);
        return expectedOverride != null
                && expectedOverride.equals(normalizedOverride(
                        metadata.get(DISPLAY_NAME_PROPERTY, false)));
    }

    void reloadDisplayName() {
        String oldDisplayName = getDisplayName();
        displayNameOverride = normalizedOverride(
                metadata.get(DISPLAY_NAME_PROPERTY, false));
        changes.firePropertyChange(
                PROP_DISPLAY_NAME,
                oldDisplayName,
                getDisplayName());
    }

    private static String normalizedOverride(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
