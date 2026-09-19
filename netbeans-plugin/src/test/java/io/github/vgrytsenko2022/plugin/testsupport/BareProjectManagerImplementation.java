package io.github.vgrytsenko2022.plugin.testsupport;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectManagerImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;

/**
 * Minimal project manager for unit tests running without the NetBeans module
 * system. The real application supplies this service from the NetBeans runtime.
 */
public final class BareProjectManagerImplementation
        implements ProjectManagerImplementation {
    private static final ConcurrentMap<URI, Project> PROJECTS =
            new ConcurrentHashMap<>();

    private final Mutex mutex = new Mutex();

    /** Registers a project that classpath-only tests expect owner queries to find. */
    public static <P extends Project> P register(P project) {
        PROJECTS.put(
                project.getProjectDirectory().toURI().normalize(),
                project);
        return project;
    }

    @Override
    public void init(ProjectManagerCallBack callback) {
        // No project cache is needed by the classpath-only unit tests.
    }

    @Override
    public Mutex getMutex() {
        return mutex;
    }

    @Override
    public Mutex getMutex(
            boolean autoSave,
            Project project,
            Project... otherProjects) {
        return mutex;
    }

    @Override
    public Project findProject(FileObject projectDirectory) {
        return PROJECTS.get(projectDirectory.toURI().normalize());
    }

    @Override
    public ProjectManager.Result isProject(FileObject projectDirectory) {
        return findProject(projectDirectory) == null
                ? null
                : new ProjectManager.Result((Icon) null);
    }

    @Override
    public void clearNonProjectCache() {
    }

    @Override
    public Set<Project> getModifiedProjects() {
        return Set.of();
    }

    @Override
    public boolean isModified(Project project) {
        return false;
    }

    @Override
    public boolean isValid(Project project) {
        return true;
    }

    @Override
    public void saveProject(Project project) throws IOException {
    }

    @Override
    public void saveAllProjects() throws IOException {
    }
}
