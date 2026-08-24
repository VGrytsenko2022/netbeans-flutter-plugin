package dev.flutter.netbeans.plugin.project;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;

/** Source roots exposed to editor and tooling integrations. */
public final class FlutterProjectSources implements Sources {
    public static final String TYPE_DART = "dart";

    private final FlutterProject project;
    private final ChangeSupport changes = new ChangeSupport(this);

    FlutterProjectSources(FlutterProject project) {
        this.project = project;
    }

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        if (Sources.TYPE_GENERIC.equals(type)) {
            return new SourceGroup[]{GenericSources.group(
                project,
                project.getProjectDirectory(),
                "flutter-project-root",
                project.info().name(),
                null,
                null)};
        }
        if (!TYPE_DART.equals(type)) {
            return new SourceGroup[0];
        }

        List<SourceGroup> groups = new ArrayList<>();
        addGroup(groups, "lib", "Dart Sources");
        addGroup(groups, "test", "Dart Tests");
        return groups.toArray(SourceGroup[]::new);
    }

    private void addGroup(List<SourceGroup> groups, String folderName, String displayName) {
        FileObject folder = project.getProjectDirectory().getFileObject(folderName);
        if (folder != null && folder.isFolder()) {
            groups.add(GenericSources.group(
                    project,
                    folder,
                    "flutter-" + folderName,
                    displayName,
                    null,
                    null));
        }
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
