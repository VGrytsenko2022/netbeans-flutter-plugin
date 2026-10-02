package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import io.github.vgrytsenko2022.plugin.project.FlutterProjectFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;

/** Fast checks for the project-relative mirrored pairing rules. */
class FlutterDesignerDataLoaderTest {
    private static final FlutterDesignerDataLoader LOADER =
            org.openide.loaders.DataLoader.getLoader(FlutterDesignerDataLoader.class);

    @TempDir
    Path temporaryDirectory;

    @Test
    void claimsOnlyDartPrimarySoTheMirroredModelRemainsVisible() throws Exception {
        ProjectFiles project = project("paired");
        FileObject dart = FileUtil.createData(project.root(), "lib/screens/home_page.dart");
        FileObject model = FileUtil.createData(project.root(),
                ".fd_templates/screens/home_page.fd");

        assertSame(dart, LOADER.findPrimaryFile(dart, project.project()));
        assertNull(LOADER.findPrimaryFile(model, project.project()));
    }

    @Test
    void doesNotClaimOrphanMismatchedOrOutOfRootFiles() throws Exception {
        ProjectFiles project = project("orphans");
        FileObject orphanDart = FileUtil.createData(project.root(), "lib/ordinary.dart");
        FileObject orphanModel = FileUtil.createData(project.root(),
                ".fd_templates/model_only.fd");
        FileObject mismatchedDart = FileUtil.createData(project.root(), "lib/account.dart");
        FileObject mismatchedModel = FileUtil.createData(project.root(),
                ".fd_templates/profile.fd");
        FileObject rootDart = FileUtil.createData(project.root(), "outside.dart");
        FileObject rootModel = FileUtil.createData(project.root(), "outside.fd");
        FileObject unrelated = FileUtil.createData(project.root(), "lib/account.json");

        assertNull(LOADER.findPrimaryFile(orphanDart, project.project()));
        assertNull(LOADER.findPrimaryFile(orphanModel, project.project()));
        assertNull(LOADER.findPrimaryFile(mismatchedDart, project.project()));
        assertNull(LOADER.findPrimaryFile(mismatchedModel, project.project()));
        assertNull(LOADER.findPrimaryFile(rootDart, project.project()));
        assertNull(LOADER.findPrimaryFile(rootModel, project.project()));
        assertNull(LOADER.findPrimaryFile(unrelated, project.project()));
        assertNull(LOADER.findPrimaryFile(project.root(), project.project()));
        assertNull(LOADER.findPrimaryFile(null, project.project()));
    }

    @Test
    void requiresTheSameRelativePathBelowBothRoots() throws Exception {
        ProjectFiles project = project("relative_path");
        FileObject dart = FileUtil.createData(project.root(), "lib/first/shared.dart");
        FileObject model = FileUtil.createData(project.root(),
                ".fd_templates/second/shared.fd");

        assertNull(LOADER.findPrimaryFile(dart, project.project()));
        assertNull(LOADER.findPrimaryFile(model, project.project()));
    }

    @Test
    void mapsNestedRelativePathsWithoutExposingTheModelRoot() throws Exception {
        ProjectFiles project = project("nested");
        FileObject dart = FileUtil.createData(project.root(), "lib/a/b/order_form.dart");
        FileObject model = FileUtil.createData(project.root(),
                ".fd_templates/a/b/order_form.fd");

        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(model, project.project())
                .orElseThrow();
        assertSame(dart, pair.dartFile());
        assertSame(model, pair.modelFile());
        assertEquals("a/b/order_form.dart", pair.relativeDartPath());
        assertEquals("a/b/order_form.fd", pair.relativeModelPath());
    }

    @Test
    void rejectsPairWhenModelRootResolvesOutsideProject() throws Exception {
        ProjectFiles project = project("linked_model_root");
        FileObject dart = FileUtil.createData(
                project.root(), "lib/screens/home_page.dart");
        Path externalModels = temporaryDirectory.resolve("external_models");
        Files.createDirectories(externalModels.resolve("screens"));
        Files.writeString(externalModels.resolve("screens/home_page.fd"), "{}");
        createDirectoryLinkOrSkip(
                temporaryDirectory.resolve("linked_model_root/.fd_templates"),
                externalModels);
        project.root().refresh();

        FileObject model = project.root().getFileObject(
                ".fd_templates/screens/home_page.fd");
        Assumptions.assumeTrue(model != null,
                "The local filesystem provider does not expose directory links.");
        assertNull(FlutterDesignerPairLayout.findCompletePair(
                dart, project.project()).orElse(null));
        assertNull(LOADER.findPrimaryFile(dart, project.project()));
        assertNull(LOADER.findPrimaryFile(model, project.project()));
    }

    private static void createDirectoryLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false,
                    "Directory symbolic links are unavailable: " + ex.getMessage());
        }
    }

    private ProjectFiles project(String name) throws Exception {
        Path directory = temporaryDirectory.resolve(name);
        Files.createDirectories(directory.resolve("lib"));
        Files.writeString(directory.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                """);
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(directory.toFile());
        FileObject root = fileSystem.getRoot();
        FlutterProject project = (FlutterProject) new FlutterProjectFactory()
                .loadProject(root, new TestProjectState());
        return new ProjectFiles(project, root);
    }

    private record ProjectFiles(FlutterProject project, FileObject root) {
    }

    private static final class TestProjectState implements ProjectState {
        @Override public void markModified() { }
        @Override public void notifyDeleted() { }
    }
}
