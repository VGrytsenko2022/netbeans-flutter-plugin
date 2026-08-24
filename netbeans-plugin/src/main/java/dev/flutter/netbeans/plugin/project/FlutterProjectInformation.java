package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;

final class FlutterProjectInformation implements ProjectInformation {
    private final FlutterProject project;
    private final FlutterProjectInfo info;
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    FlutterProjectInformation(FlutterProject project, FlutterProjectInfo info) {
        this.project = project;
        this.info = info;
    }

    @Override
    public String getName() {
        return info.name();
    }

    @Override
    public String getDisplayName() {
        return info.name();
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
}
