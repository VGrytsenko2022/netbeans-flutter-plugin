package dev.flutter.netbeans.plugin.device;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.util.Objects;
import java.util.function.Supplier;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/** Resolves the Flutter project selected in the IDE, then main, then the only open project. */
final class ActiveFlutterProjectResolver implements Supplier<FlutterProject> {
    private final Supplier<Lookup> context;

    ActiveFlutterProjectResolver() {
        this(Utilities::actionsGlobalContext);
    }

    ActiveFlutterProjectResolver(Supplier<Lookup> context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public FlutterProject get() {
        Lookup lookup = context.get();
        for (Project project : lookup.lookupAll(Project.class)) {
            if (project instanceof FlutterProject flutterProject) {
                return flutterProject;
            }
        }
        FileObject selectedFile = lookup.lookup(FileObject.class);
        if (selectedFile == null) {
            DataObject dataObject = lookup.lookup(DataObject.class);
            selectedFile = dataObject == null ? null : dataObject.getPrimaryFile();
        }
        if (selectedFile != null) {
            Project owner = FileOwnerQuery.getOwner(selectedFile);
            if (owner instanceof FlutterProject flutterProject) {
                return flutterProject;
            }
        }
        Project main = OpenProjects.getDefault().getMainProject();
        if (main instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        FlutterProject only = null;
        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            if (project instanceof FlutterProject flutterProject) {
                if (only != null) {
                    return null;
                }
                only = flutterProject;
            }
        }
        return only;
    }
}
