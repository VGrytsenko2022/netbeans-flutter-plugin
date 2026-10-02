package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import io.github.vgrytsenko2022.plugin.project.FlutterProjectFactory;
import java.nio.file.Path;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataLoader;

/** Loads real temporary Flutter projects for pair-aware loader tests. */
final class FlutterDesignerTestProject {
    private FlutterDesignerTestProject() {
    }

    static FlutterProject own(Path projectRoot) throws Exception {
        FileUtil.refreshFor(projectRoot.toFile());
        FileObject projectDirectory = FileUtil.toFileObject(projectRoot.toFile());
        assertNotNull(projectDirectory);
        FlutterProject project = (FlutterProject) new FlutterProjectFactory()
                .loadProject(projectDirectory, new TestProjectState());
        assertNotNull(project);
        return project;
    }

    static FlutterDesignerDataObject dataObject(
            FileObject dartFile,
            FlutterProject project) throws Exception {
        FlutterDesignerDataLoader loader = DataLoader.getLoader(
                FlutterDesignerDataLoader.class);
        return (FlutterDesignerDataObject) loader.findDataObject(
                dartFile, project);
    }

    private static final class TestProjectState implements ProjectState {
        @Override public void markModified() { }
        @Override public void notifyDeleted() { }
    }
}
