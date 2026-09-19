package io.github.vgrytsenko2022.plugin.theme;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/** Resolves one exact Flutter project without silently picking among candidates. */
final class FlutterThemeProjectResolver {
    private final Lookup context;
    private final Supplier<Project> mainProject;
    private final Supplier<Project[]> openProjects;

    FlutterThemeProjectResolver(Lookup context) {
        this(
                context,
                () -> OpenProjects.getDefault().getMainProject(),
                () -> OpenProjects.getDefault().getOpenProjects());
    }

    FlutterThemeProjectResolver(
            Lookup context,
            Supplier<Project> mainProject,
            Supplier<Project[]> openProjects) {
        this.context = Objects.requireNonNull(context, "context");
        this.mainProject = Objects.requireNonNull(mainProject, "mainProject");
        this.openProjects = Objects.requireNonNull(openProjects, "openProjects");
    }

    Resolution resolve() {
        Map<Path, FlutterProjectInfo> contextual = new LinkedHashMap<>();
        for (Project project : context.lookupAll(Project.class)) {
            add(contextual, project);
        }
        if (contextual.size() == 1) {
            return Resolution.success(contextual.values().iterator().next().root());
        }
        if (contextual.size() > 1) {
            return Resolution.failure(
                    "multiple Flutter projects are selected; select exactly one project");
        }

        FileObject selectedFile = context.lookup(FileObject.class);
        if (selectedFile == null) {
            DataObject selectedDataObject = context.lookup(DataObject.class);
            selectedFile = selectedDataObject == null
                    ? null
                    : selectedDataObject.getPrimaryFile();
        }
        if (selectedFile != null) {
            FlutterProjectInfo selectedOwner = info(FileOwnerQuery.getOwner(selectedFile));
            if (selectedOwner != null) {
                return Resolution.success(selectedOwner.root());
            }
        }

        FlutterProjectInfo main = info(mainProject.get());
        if (main != null) {
            return Resolution.success(main.root());
        }

        Map<Path, FlutterProjectInfo> opened = new LinkedHashMap<>();
        for (Project project : openProjects.get()) {
            add(opened, project);
        }
        if (opened.size() == 1) {
            return Resolution.success(opened.values().iterator().next().root());
        }
        if (opened.size() > 1) {
            return Resolution.failure(
                    "more than one Flutter project is open and none is selected or main");
        }
        return Resolution.failure("no recognized Flutter project is selected or open");
    }

    private static void add(Map<Path, FlutterProjectInfo> result, Project project) {
        FlutterProjectInfo info = info(project);
        if (info != null) {
            Path root = info.root().toAbsolutePath().normalize();
            result.putIfAbsent(root, info);
        }
    }

    private static FlutterProjectInfo info(Project project) {
        return project == null
                ? null
                : project.getLookup().lookup(FlutterProjectInfo.class);
    }

    record Resolution(Path projectRoot, String reason) {
        Resolution {
            if ((projectRoot == null) == (reason == null)) {
                throw new IllegalArgumentException(
                        "Theme project resolution must contain exactly one outcome");
            }
        }

        static Resolution success(Path projectRoot) {
            return new Resolution(
                    projectRoot.toAbsolutePath().normalize(), null);
        }

        static Resolution failure(String reason) {
            return new Resolution(null, Objects.requireNonNull(reason, "reason"));
        }

        boolean resolved() {
            return projectRoot != null;
        }
    }
}
