package io.github.vgrytsenko2022.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import io.github.vgrytsenko2022.plugin.tooling.FlutterToolingController;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.project.DataFilesProviderImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.ProjectOperations;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;

class FlutterProjectDeleteOperationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void lookupExposesOneDeleteAndMoveProviderWithRootOnlyDataInventory()
            throws Exception {
        DeleteFixture fixture = project("delete-inventory");
        try {
            DeleteOperationImplementation deletion = fixture.project()
                    .getLookup().lookup(DeleteOperationImplementation.class);
            MoveOrRenameOperationImplementation move = fixture.project()
                    .getLookup().lookup(
                            MoveOrRenameOperationImplementation.class);

            assertNotNull(deletion,
                    "NetBeans 30 DefaultProjectOperations requires DeleteOperationImplementation in Lookup");
            assertSame(move, deletion,
                    "move and delete must share one DataFilesProvider to avoid duplicate root inventory");
            assertEquals(1, fixture.project().getLookup()
                    .lookupAll(DataFilesProviderImplementation.class).size());
            assertTrue(ProjectOperations.isDeleteOperationSupported(
                    fixture.project()));
            assertEquals(List.of(), deletion.getMetadataFiles(),
                    "Flutter private attributes/preferences are not separate physical metadata files");
            assertEquals(List.of(fixture.directory()),
                    deletion.getDataFiles(),
                    "the complete Flutter root is the sole user-data deletion unit");
            assertEquals(List.of(), ProjectOperations.getMetadataFiles(
                    fixture.project()));
            assertEquals(List.of(fixture.directory()),
                    ProjectOperations.getDataFiles(fixture.project()),
                    "NetBeans aggregation must expose the root exactly once");
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void metadataInventoryContainsOnlyExistingPluginOwnedFiles()
            throws Exception {
        DeleteFixture fixture = project("delete-metadata-inventory");
        try {
            createFile(fixture.directory(),
                    FlutterProjectMetadata.SHARED_METADATA_PATH);
            createFile(fixture.directory(), FlutterMoveHandoff.HANDOFF_PATH);
            createFile(fixture.directory(),
                    FlutterMoveHandoff.TARGET_HANDOFF_PATH);
            FileObject committed = createFile(fixture.directory(),
                    FlutterMoveHandoff.COMMITTED_HANDOFF_PATH);
            createFile(fixture.directory(), ".netbeans/foreign-plugin.xml");
            createFile(fixture.directory(),
                    ".netbeans/nested/user-owned.txt");

            DeleteOperationImplementation deletion = fixture.project()
                    .getLookup().lookup(DeleteOperationImplementation.class);

            assertNotNull(deletion);
            assertEquals(Set.of(
                            FlutterProjectMetadata.SHARED_METADATA_PATH,
                            FlutterMoveHandoff.HANDOFF_PATH,
                            FlutterMoveHandoff.TARGET_HANDOFF_PATH,
                            FlutterMoveHandoff.COMMITTED_HANDOFF_PATH),
                    metadataPaths(deletion),
                    "delete metadata inventory must contain exact plugin-owned "
                            + "files, never the whole .netbeans folder");
            assertFalse(deletion.getMetadataFiles().contains(
                    fixture.directory().getFileObject(".netbeans")));

            committed.delete();

            assertEquals(Set.of(
                            FlutterProjectMetadata.SHARED_METADATA_PATH,
                            FlutterMoveHandoff.HANDOFF_PATH,
                            FlutterMoveHandoff.TARGET_HANDOFF_PATH),
                    metadataPaths(deletion),
                    "missing plugin metadata must not be returned as a synthetic FileObject");
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void keepSourcesDeletesOnlyMetadataAndPreservesFlutterAndDesignerFiles()
            throws Exception {
        DeleteFixture fixture = project("delete-keep-sources");
        try {
            createFile(fixture.directory(),
                    FlutterProjectMetadata.SHARED_METADATA_PATH);
            createFile(fixture.directory(), FlutterMoveHandoff.HANDOFF_PATH);
            createFile(fixture.directory(),
                    FlutterMoveHandoff.TARGET_HANDOFF_PATH);
            createFile(fixture.directory(),
                    FlutterMoveHandoff.COMMITTED_HANDOFF_PATH);
            FileObject unrelated = createFile(fixture.directory(),
                    ".netbeans/foreign-plugin.xml");

            List<FileObject> metadata = ProjectOperations.getMetadataFiles(
                    fixture.project());
            assertEquals(4, metadata.size(),
                    "precondition: every physical Flutter metadata phase is inventoried");

            ProjectOperations.notifyDeleting(fixture.project());
            for (FileObject metadataFile : metadata) {
                metadataFile.delete();
            }
            ProjectOperations.notifyDeleted(fixture.project());

            assertTrue(fixture.state().deleted());
            assertEquals(1, fixture.state().deleteCalls());
            assertProjectFilesRemain(fixture,
                    "keeping sources must preserve Dart and Flutter Designer content");
            assertTrue(unrelated.isValid(),
                    "metadata owned by another feature must remain untouched");
            assertNull(fixture.directory().getFileObject(
                    FlutterProjectMetadata.SHARED_METADATA_PATH));
            assertNull(fixture.directory().getFileObject(
                    FlutterMoveHandoff.HANDOFF_PATH));
            assertNull(fixture.directory().getFileObject(
                    FlutterMoveHandoff.TARGET_HANDOFF_PATH));
            assertNull(fixture.directory().getFileObject(
                    FlutterMoveHandoff.COMMITTED_HANDOFF_PATH));
            assertNull(fixture.directory().getAttribute(
                    FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE),
                    "keep-sources completion must detach private NetBeans project state");
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void repeatedDeleteCallbacksAreIdempotent() throws Exception {
        DeleteFixture fixture = project("delete-idempotent");
        try {
            ProjectOperations.notifyDeleting(fixture.project());
            ProjectOperations.notifyDeleting(fixture.project());
            ProjectOperations.notifyDeleted(fixture.project());
            ProjectOperations.notifyDeleted(fixture.project());

            assertTrue(fixture.state().deleted());
            assertEquals(1, fixture.state().deleteCalls(),
                    "NetBeans callback retries must notify ProjectState exactly once");
            assertProjectFilesRemain(fixture,
                    "idempotent lifecycle callbacks do not own source deletion");
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void moveAndRenameAreRejectedAfterDeletePreparation() throws Exception {
        DeleteFixture fixture = project("delete-blocks-move");
        try {
            DeleteOperationImplementation deletion = fixture.project()
                    .getLookup().lookup(DeleteOperationImplementation.class);
            MoveOrRenameOperationImplementation move = fixture.project()
                    .getLookup().lookup(
                            MoveOrRenameOperationImplementation.class);
            assertNotNull(deletion);
            assertSame(deletion, move);

            deletion.notifyDeleting();

            IOException moveFailure = assertThrows(
                    IOException.class, move::notifyMoving);
            IOException renamePrepareFailure = assertThrows(
                    IOException.class, move::notifyRenaming);
            IOException renameCompletionFailure = assertThrows(
                    IOException.class,
                    () -> move.notifyRenamed("renamed_after_delete"));
            assertTrue(moveFailure.getMessage().contains("deletion was prepared"));
            assertTrue(renamePrepareFailure.getMessage().contains(
                    "deletion was prepared"));
            assertTrue(renameCompletionFailure.getMessage().contains(
                    "deletion was prepared"));

            deletion.notifyDeleted();
            assertEquals(1, fixture.state().deleteCalls());
            assertNull(fixture.directory().getFileObject(
                    FlutterMoveHandoff.HANDOFF_PATH),
                    "a rejected move must not create a handoff after delete preparation");
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void notifyDeletingStopsServicesAndNotifyDeletedMarksStateWithoutDeletingFiles()
            throws Exception {
        DeleteFixture fixture = project("delete-lifecycle");
        FlutterProjectLifecycle lifecycle = fixture.project().getLookup()
                .lookup(FlutterProjectLifecycle.class);
        FlutterRunController runController = fixture.project().getLookup()
                .lookup(FlutterRunController.class);
        FlutterToolingController toolingController = fixture.project()
                .getLookup().lookup(FlutterToolingController.class);
        try {
            lifecycle.projectOpened();
            assertTrue(runController.isCommandEnabled(
                    org.netbeans.spi.project.ActionProvider.COMMAND_RUN));
            assertTrue(toolingController.isCommandEnabled(
                    org.netbeans.spi.project.ActionProvider.COMMAND_TEST));

            ProjectOperations.notifyDeleting(fixture.project());

            assertEquals(0, fixture.state().deleteCalls());
            assertFalse(runController.isCommandEnabled(
                    org.netbeans.spi.project.ActionProvider.COMMAND_RUN),
                    "pre-delete must release project-scoped run resources before NetBeans removes Windows files");
            assertFalse(toolingController.isCommandEnabled(
                    org.netbeans.spi.project.ActionProvider.COMMAND_TEST));
            assertProjectFilesRemain(fixture,
                    "notifyDeleting is a lifecycle hook; NetBeans owns filesystem removal");

            ProjectOperations.notifyDeleted(fixture.project());

            assertTrue(fixture.state().deleted());
            assertEquals(1, fixture.state().deleteCalls());
            assertProjectFilesRemain(fixture,
                    "notifyDeleted must invalidate ProjectState without deleting data itself");
        } finally {
            lifecycle.projectClosed();
        }
    }

    @Test
    void notifyDeletedBeforePreDeleteFailsWithoutInvalidatingProject()
            throws Exception {
        DeleteFixture fixture = project("delete-out-of-order");
        try {
            DeleteOperationImplementation deletion = fixture.project()
                    .getLookup().lookup(DeleteOperationImplementation.class);
            assertNotNull(deletion);

            IOException failure = assertThrows(
                    IOException.class, deletion::notifyDeleted);

            assertTrue(failure.getMessage().contains("notifyDeleting"),
                    () -> "unexpected lifecycle rejection: "
                    + failure.getMessage());
            assertFalse(fixture.state().deleted());
            assertEquals(0, fixture.state().deleteCalls());
            assertProjectFilesRemain(fixture,
                    "out-of-order completion must preserve project data");

            deletion.notifyDeleting();
            deletion.notifyDeleted();
            assertTrue(fixture.state().deleted(),
                    "a rejected out-of-order call must not poison a valid retry");
            assertEquals(1, fixture.state().deleteCalls());
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    @Test
    void projectStateDeletionFailureIsIOExceptionAndCompletionMayRetry()
            throws Exception {
        DeleteFixture fixture = project("delete-state-failure");
        FlutterProjectLifecycle lifecycle = fixture.project().getLookup()
                .lookup(FlutterProjectLifecycle.class);
        DeleteOperationImplementation deletion = fixture.project()
                .getLookup().lookup(DeleteOperationImplementation.class);
        try {
            assertNotNull(deletion);
            lifecycle.projectOpened();
            deletion.notifyDeleting();
            fixture.state().failNextDeletion();

            IOException failure = assertThrows(
                    IOException.class, deletion::notifyDeleted);

            IllegalStateException stateFailure = assertInstanceOf(
                    IllegalStateException.class, failure.getCause());
            assertEquals("Injected Flutter project deletion failure",
                    stateFailure.getMessage());
            assertFalse(fixture.state().deleted());
            assertEquals(1, fixture.state().deleteCalls());
            assertFalse(fixture.project().getLookup()
                    .lookup(FlutterRunController.class)
                    .isCommandEnabled(
                            org.netbeans.spi.project.ActionProvider.COMMAND_RUN),
                    "a failed final notification must not restart pre-delete services");
            assertProjectFilesRemain(fixture,
                    "ProjectState failure must preserve filesystem ownership for retry");

            deletion.notifyDeleted();

            assertTrue(fixture.state().deleted());
            assertEquals(2, fixture.state().deleteCalls(),
                    "the exact prepared delete lifecycle must remain retryable");
            assertProjectFilesRemain(fixture,
                    "successful retry still leaves physical deletion to NetBeans");
        } finally {
            lifecycle.projectClosed();
        }
    }

    @Test
    void metadataCleanupFailureDoesNotNotifyProjectStateAndMayRetry()
            throws Exception {
        DeleteFixture fixture = project("delete-cleanup-failure");
        FlutterProjectInformation information = fixture.project().getLookup()
                .lookup(FlutterProjectInformation.class);
        FlutterProjectMetadata metadata = fixture.project().getLookup()
                .lookup(FlutterProjectMetadata.class);
        AtomicInteger cleanupCalls = new AtomicInteger();
        FlutterProjectMoveOperation deletion = new FlutterProjectMoveOperation(
                fixture.project(),
                information,
                (destination, attribute, value) -> {
                },
                ignored -> {
                },
                currentMetadata -> {
                    if (cleanupCalls.incrementAndGet() == 1) {
                        throw new IOException(
                                "Injected Flutter metadata cleanup failure");
                    }
                    currentMetadata.clearPrivateMetadataAfterDelete();
                });
        try {
            assertNotNull(information);
            assertNotNull(metadata);
            deletion.notifyDeleting();

            IOException failure = assertThrows(
                    IOException.class,
                    deletion::notifyDeleted);

            assertEquals("Injected Flutter metadata cleanup failure",
                    failure.getMessage());
            assertEquals(1, cleanupCalls.get());
            assertEquals(0, fixture.state().deleteCalls(),
                    "ProjectState must not be finalized before metadata cleanup succeeds");
            assertNotNull(fixture.directory().getAttribute(
                    FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

            deletion.notifyDeleted();

            assertEquals(2, cleanupCalls.get(),
                    "the failed cleanup stage must remain retryable");
            assertEquals(1, fixture.state().deleteCalls());
            assertTrue(fixture.state().deleted());
            assertNull(fixture.directory().getAttribute(
                    FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        } finally {
            closeProjectServices(fixture.project());
        }
    }

    private DeleteFixture project(String name) throws Exception {
        Path root = Files.createDirectories(
                temporaryDirectory.resolve(name));
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve("pubspec.yaml"),
                "name: " + name.replace('-', '_') + "\nflutter:\n");
        Files.writeString(root.resolve("lib/main.dart"),
                "void main() {}\n");
        Files.writeString(root.resolve("lib/home.fd"),
                "{\"format\":\"flutter-designer\"}\n");
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        TrackingProjectState state = new TrackingProjectState();
        FlutterProjectMetadata metadata =
                new FlutterProjectMetadata(directory);
        metadata.put("selectedDeviceId", "windows", false);
        FlutterProject project = new FlutterProject(
                directory,
                state,
                new FlutterProjectInfo(
                        root,
                        name.replace('-', '_'),
                        root.resolve("pubspec.yaml")),
                metadata,
                null,
                ignored -> {
                });
        return new DeleteFixture(
                project,
                state,
                directory,
                root.resolve("pubspec.yaml"),
                root.resolve("lib/main.dart"),
                root.resolve("lib/home.fd"));
    }

    private static FileObject createFile(FileObject root, String path)
            throws IOException {
        FileObject file = FileUtil.createData(root, path);
        try (var output = file.getOutputStream()) {
            output.write("test metadata\n".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        return file;
    }

    private static Set<String> metadataPaths(
            DeleteOperationImplementation deletion) {
        return deletion.getMetadataFiles().stream()
                .map(FileObject::getPath)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static void assertProjectFilesRemain(
            DeleteFixture fixture,
            String message) {
        assertTrue(fixture.directory().isValid(), message);
        assertTrue(Files.exists(fixture.pubspec()), message);
        assertTrue(Files.exists(fixture.mainDart()), message);
        assertTrue(Files.exists(fixture.designerFile()), message);
    }

    private static void closeProjectServices(FlutterProject project) {
        project.getLookup().lookup(FlutterProjectLifecycle.class)
                .projectClosed();
    }

    private static final class TrackingProjectState implements ProjectState {
        private int deleteCalls;
        private boolean deleted;
        private boolean failNextDeletion;

        @Override
        public void markModified() {
        }

        @Override
        public void notifyDeleted() {
            deleteCalls++;
            if (failNextDeletion) {
                failNextDeletion = false;
                throw new IllegalStateException(
                        "Injected Flutter project deletion failure");
            }
            deleted = true;
        }

        void failNextDeletion() {
            failNextDeletion = true;
        }

        int deleteCalls() {
            return deleteCalls;
        }

        boolean deleted() {
            return deleted;
        }
    }

    private record DeleteFixture(
            FlutterProject project,
            TrackingProjectState state,
            FileObject directory,
            Path pubspec,
            Path mainDart,
            Path designerFile) {
    }
}
