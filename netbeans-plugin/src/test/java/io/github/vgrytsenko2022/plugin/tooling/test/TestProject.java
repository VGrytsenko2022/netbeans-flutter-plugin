package io.github.vgrytsenko2022.plugin.tooling.test;

import io.github.vgrytsenko2022.plugin.testsupport.BareProjectManagerImplementation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/** Minimal disk-backed project for Test Results unit tests. */
final class TestProject implements Project {
    private final FileObject directory;

    private TestProject(FileObject directory) {
        this.directory = directory;
    }

    static TestProject create(Path projectRoot) throws Exception {
        Files.createDirectories(projectRoot);
        FileUtil.refreshFor(projectRoot.toFile());
        FileObject directory = FileUtil.toFileObject(projectRoot.toFile());
        if (directory == null) {
            throw new AssertionError("No FileObject for " + projectRoot);
        }
        return BareProjectManagerImplementation.register(new TestProject(directory));
    }

    @Override
    public FileObject getProjectDirectory() {
        return directory;
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }
}
