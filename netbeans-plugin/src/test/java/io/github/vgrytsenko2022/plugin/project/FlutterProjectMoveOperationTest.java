package io.github.vgrytsenko2022.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import io.github.vgrytsenko2022.plugin.tooling.FlutterToolingController;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.ProjectOperations;
import org.openide.filesystems.AbstractFileSystem;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;

class FlutterProjectMoveOperationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void restoresPrivateStateAfterCrossFileSystemMoveLosesRootAttributes()
            throws Exception {
        Path sourcePath = Files.createDirectories(
                temporaryDirectory.resolve("source-provider"));
        AttributeDroppingLocalFileSystem sourceFileSystem =
                new AttributeDroppingLocalFileSystem(sourcePath);
        FileObject sourceDirectory = FileUtil.createFolder(
                sourceFileSystem.getRoot(),
                "source_app");
        write(sourceDirectory, "pubspec.yaml", "name: source_app\nflutter:\n");
        write(sourceDirectory, "lib/main.dart", "void main() {}\n");

        FlutterProjectMetadata sourceMetadata = new FlutterProjectMetadata(sourceDirectory);
        sourceMetadata.put("selectedDeviceId", "windows", false);
        String legacyQuarantine = FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                + "legacy-payload";
        String externalQuarantine = FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX
                + "external-payload";
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + legacyQuarantine,
                42);
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + externalQuarantine,
                "raw-corrupt-payload");
        assertNotNull(sourceDirectory.getAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX
                        + FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

        TrackingProjectState originalState = new TrackingProjectState();
        FlutterProject original = project(
                sourceDirectory,
                originalState,
                sourceMetadata,
                sourcePath.resolve("source_app"));
        MoveOrRenameOperationImplementation sourceMove = original.getLookup()
                .lookup(MoveOrRenameOperationImplementation.class);
        assertNotNull(sourceMove);
        assertTrue(ProjectOperations.isMoveOperationSupported(original));
        sourceMove.notifyMoving();

        Path targetPath = Files.createDirectories(temporaryDirectory.resolve("target-provider"));
        AttributeDroppingLocalFileSystem targetFileSystem =
                new AttributeDroppingLocalFileSystem(targetPath);
        FileObject movedDirectory;
        try (FileLock lock = sourceDirectory.lock()) {
            movedDirectory = sourceDirectory.move(
                    lock,
                    targetFileSystem.getRoot(),
                    "moved_app",
                    null);
        }

        assertFalse(sourceDirectory.isValid());
        assertNull(movedDirectory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        assertNull(movedDirectory.getAttribute(legacyQuarantine));
        assertNull(movedDirectory.getAttribute(externalQuarantine));

        FlutterProjectMetadata movedMetadata = new FlutterProjectMetadata(movedDirectory);
        TrackingProjectState movedState = new TrackingProjectState();
        FlutterProject moved = project(
                movedDirectory,
                movedState,
                movedMetadata,
                targetPath.resolve("moved_app"));
        MoveOrRenameOperationImplementation targetMove = moved.getLookup()
                .lookup(MoveOrRenameOperationImplementation.class);
        assertNotNull(targetMove);

        sourceMove.notifyMoved(null, null, "moved_app");
        assertFalse(originalState.deleted,
                "The source must remain valid until the target confirms restoration");
        targetMove.notifyMoved(original, null, "moved_app");

        assertTrue(originalState.deleted);
        assertFalse(movedState.deleted);
        assertEquals("windows", movedMetadata.get("selectedDeviceId", false));
        assertEquals(42, movedDirectory.getAttribute(legacyQuarantine));
        assertEquals("raw-corrupt-payload",
                movedDirectory.getAttribute(externalQuarantine));
        ProjectInformation movedInformation = moved.getLookup()
                .lookup(ProjectInformation.class);
        assertEquals("source_app", movedInformation.getName());
        assertEquals("moved_app", movedInformation.getDisplayName());
        assertEquals("moved_app", movedMetadata.get(
                FlutterProjectInformation.DISPLAY_NAME_PROPERTY,
                false));

        FlutterProject reloaded = project(
                movedDirectory,
                new TrackingProjectState(),
                new FlutterProjectMetadata(movedDirectory),
                targetPath.resolve("moved_app"));
        assertEquals("moved_app", reloaded.getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());

        closeProjectServices(original);
        closeProjectServices(moved);
        closeProjectServices(reloaded);
    }

    @Test
    void renamePersistsDisplayNameWithoutChangingFlutterPackageName()
            throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("rename-project"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");

        FlutterProjectMetadata metadata = new FlutterProjectMetadata(directory);
        FlutterProject project = project(
                directory,
                new TrackingProjectState(),
                metadata,
                root);
        FlutterProjectInformation information = project.getLookup()
                .lookup(FlutterProjectInformation.class);
        FlutterProjectMoveOperation move = project.getLookup()
                .lookup(FlutterProjectMoveOperation.class);
        List<PropertyChangeEvent> events = new ArrayList<>();
        information.addPropertyChangeListener(events::add);

        move.notifyRenamed("Friendly Flutter App");

        assertEquals("source_app", information.getName());
        assertEquals("Friendly Flutter App", information.getDisplayName());
        assertEquals("Friendly Flutter App", metadata.get(
                FlutterProjectInformation.DISPLAY_NAME_PROPERTY,
                false));
        assertEquals(1, events.size());
        assertEquals(ProjectInformation.PROP_DISPLAY_NAME,
                events.get(0).getPropertyName());
        assertEquals("source_app", events.get(0).getOldValue());
        assertEquals("Friendly Flutter App", events.get(0).getNewValue());

        FlutterProject reloaded = project(
                directory,
                new TrackingProjectState(),
                new FlutterProjectMetadata(directory),
                root);
        ProjectInformation reloadedInformation = reloaded.getLookup()
                .lookup(ProjectInformation.class);
        assertEquals("source_app", reloadedInformation.getName());
        assertEquals("Friendly Flutter App", reloadedInformation.getDisplayName());

        closeProjectServices(project);
        closeProjectServices(reloaded);
    }

    @Test
    void failedRestoreKeepsSourceAliveAndSnapshotCanBeRetried()
            throws Exception {
        MoveFixture fixture = createMoveFixture("restore-retry");
        fixture.sourceMove().notifyMoved(null, null, "retried_app");
        assertFalse(fixture.originalState().deleted);

        AtomicInteger writes = new AtomicInteger();
        FlutterProjectInformation targetInformation = fixture.moved().getLookup()
                .lookup(FlutterProjectInformation.class);
        FlutterProjectMoveOperation failingTarget = new FlutterProjectMoveOperation(
                fixture.moved(),
                targetInformation,
                (destination, attribute, value) -> {
                    if (writes.incrementAndGet() == 2) {
                        return;
                    }
                    destination.setAttribute(
                            FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + attribute,
                            value);
                });

        assertThrows(
                IOException.class,
                () -> failingTarget.notifyMoved(
                        fixture.original(),
                        null,
                        "retried_app"));
        assertFalse(fixture.originalState().deleted,
                "A failed target read-back must not delete the source project state");
        assertEquals("source_app", targetInformation.getDisplayName(),
                "The display-name update must wait for a complete state restore");

        fixture.targetMove().notifyMoved(
                fixture.original(),
                null,
                "retried_app");

        assertTrue(fixture.originalState().deleted);
        assertEquals("windows", fixture.movedMetadata().get(
                "selectedDeviceId",
                false));
        assertEquals(42, fixture.movedDirectory().getAttribute(
                fixture.legacyQuarantine()));
        assertEquals("raw-corrupt-payload", fixture.movedDirectory().getAttribute(
                fixture.externalQuarantine()));
        assertArrayEquals(new byte[]{0, 1, 2, -1},
                (byte[]) fixture.movedDirectory().getAttribute(
                        fixture.byteArrayQuarantine()));
        assertEquals("https://example.invalid/flutter?q=move",
                ((URL) fixture.movedDirectory().getAttribute(
                        fixture.urlQuarantine())).toExternalForm());
        assertEquals("retried_app", targetInformation.getDisplayName());
        assertEquals("retried_app", fixture.movedMetadata().get(
                FlutterProjectInformation.DISPLAY_NAME_PROPERTY,
                false));

        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
    }

    @Test
    void pendingHandoffRecoversAfterOriginalMoveSnapshotIsUnavailable()
            throws Exception {
        MoveFixture fixture = createMoveFixture("open-recovery");
        AtomicInteger writes = new AtomicInteger();
        FlutterProjectInformation targetInformation = fixture.moved().getLookup()
                .lookup(FlutterProjectInformation.class);
        FlutterProjectMoveOperation failingTarget = new FlutterProjectMoveOperation(
                fixture.moved(),
                targetInformation,
                (destination, attribute, value) -> {
                    if (writes.incrementAndGet() == 2) {
                        return;
                    }
                    destination.setAttribute(
                            FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + attribute,
                            value);
                });
        assertThrows(IOException.class, () -> failingTarget.notifyMoved(
                fixture.original(),
                null,
                "moved_after_restart"));
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH));
        assertFalse(fixture.originalState().deleted);

        fixture.movedDirectory().setAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                null);
        fixture.movedDirectory().setAttribute(fixture.legacyQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.externalQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.byteArrayQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.urlQuarantine(), null);
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());

        Path movedPath = FileUtil.toFile(fixture.movedDirectory()).toPath();
        FlutterProjectMetadata restartedMetadata = new FlutterProjectMetadata(
                fixture.movedDirectory());
        FlutterProject restarted = project(
                fixture.movedDirectory(),
                new TrackingProjectState(),
                restartedMetadata,
                movedPath);
        FlutterProjectInformation restartedInformation = restarted.getLookup()
                .lookup(FlutterProjectInformation.class);
        assertEquals("source_app", restartedInformation.getDisplayName());

        restarted.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH));
        assertEquals("windows", restartedMetadata.get("selectedDeviceId", false));
        assertEquals(42, fixture.movedDirectory().getAttribute(
                fixture.legacyQuarantine()));
        assertEquals("raw-corrupt-payload", fixture.movedDirectory().getAttribute(
                fixture.externalQuarantine()));
        assertArrayEquals(new byte[]{0, 1, 2, -1},
                (byte[]) fixture.movedDirectory().getAttribute(
                        fixture.byteArrayQuarantine()));
        assertEquals("https://example.invalid/flutter?q=move",
                ((URL) fixture.movedDirectory().getAttribute(
                        fixture.urlQuarantine())).toExternalForm());
        assertEquals("moved_after_restart", restartedInformation.getDisplayName(),
                "Recovery must honor the target-name intent persisted before restore");
        closeProjectServices(restarted);
    }

    @Test
    void projectOpenRecoveryCompletesPendingSourceAfterSameProcessFailure()
            throws Exception {
        AtomicInteger writes = new AtomicInteger();
        MoveFixture fixture = createMoveFixture(
                "same-process-recovery",
                (destination, attribute, value) -> {
                    if (writes.incrementAndGet() == 2) {
                        return;
                    }
                    destination.setAttribute(
                            FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + attribute,
                            value);
                });

        assertThrows(IOException.class, () -> fixture.targetMove().notifyMoved(
                fixture.original(),
                null,
                "recovered_target"));
        assertFalse(fixture.originalState().deleted);
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.TARGET_HANDOFF_PATH));

        assertEquals(
                FlutterProjectMoveOperation.RecoveryResult.RECOVERED,
                fixture.moved().getLookup().lookup(FlutterProjectLifecycle.class)
                        .recoverMetadataOnOpen());

        assertTrue(fixture.originalState().deleted,
                "The retained target operation must finish source deletion on open");
        assertNoHandoff(fixture.movedDirectory());
        assertEquals("recovered_target", fixture.moved().getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
    }

    @Test
    void committedRecoveryDoesNotRollbackNewerSettingsAfterCleanupFailure()
            throws Exception {
        MoveFixture fixture = createMoveFixture("post-rename-crash");
        fixture.originalState().failNextDeletion();

        assertThrows(IllegalStateException.class, () -> fixture.targetMove().notifyMoved(
                fixture.original(),
                null,
                "durable_target_name"));
        assertFalse(fixture.originalState().deleted);
        assertEquals("durable_target_name", fixture.moved().getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.COMMITTED_HANDOFF_PATH));

        fixture.movedMetadata().put("selectedDeviceId", "linux", false);
        fixture.moved().getLookup().lookup(FlutterProjectInformation.class)
                .renameTo("Newer User Display");
        fixture.movedDirectory().setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX
                        + fixture.byteArrayQuarantine(),
                new byte[]{9, 8, 7});
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
        Path movedPath = FileUtil.toFile(fixture.movedDirectory()).toPath();
        FlutterProjectMetadata restartedMetadata = new FlutterProjectMetadata(
                fixture.movedDirectory());
        FlutterProject restarted = project(
                fixture.movedDirectory(),
                new TrackingProjectState(),
                restartedMetadata,
                movedPath);

        restarted.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertNoHandoff(fixture.movedDirectory());
        assertTrue(fixture.originalState().deleted);
        assertEquals("Newer User Display", restarted.getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());
        assertEquals("linux", restartedMetadata.get("selectedDeviceId", false));
        assertArrayEquals(new byte[]{9, 8, 7},
                (byte[]) fixture.movedDirectory().getAttribute(
                        fixture.byteArrayQuarantine()));
        assertEquals("https://example.invalid/flutter?q=move",
                ((URL) fixture.movedDirectory().getAttribute(
                        fixture.urlQuarantine())).toExternalForm());
        closeProjectServices(restarted);
    }

    @Test
    void preparedTargetWithoutDisplayIntentRestoresStateButStaysPending()
            throws Exception {
        MoveFixture fixture = createMoveFixture("missing-target-intent");
        assertNull(fixture.movedDirectory().getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

        assertEquals(
                FlutterProjectMoveOperation.RecoveryResult.BLOCKED,
                fixture.moved().getLookup().lookup(FlutterProjectLifecycle.class)
                        .recoverMetadataOnOpen());

        assertFalse(fixture.originalState().deleted);
        assertEquals("windows", fixture.movedMetadata().get(
                "selectedDeviceId",
                false));
        assertEquals("Original Display", fixture.moved().getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH));
        assertNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.TARGET_HANDOFF_PATH));
        assertNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.COMMITTED_HANDOFF_PATH));
        FlutterProjectActionProvider actions = fixture.moved().getLookup()
                .lookup(FlutterProjectActionProvider.class);
        assertFalse(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_RUN,
                org.openide.util.Lookup.EMPTY));
        assertFalse(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_MOVE,
                org.openide.util.Lookup.EMPTY));
        assertTrue(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_RENAME,
                org.openide.util.Lookup.EMPTY));

        fixture.targetMove().notifyRenamed("Resolved Display");

        assertTrue(fixture.originalState().deleted);
        assertNoHandoff(fixture.movedDirectory());
        assertEquals("Resolved Display", fixture.moved().getLookup()
                .lookup(ProjectInformation.class)
                .getDisplayName());
        assertEquals("windows", fixture.movedMetadata().get(
                "selectedDeviceId",
                false));
        assertFalse(fixture.targetMove().isRecoveryBlocked());
        assertTrue(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_MOVE,
                org.openide.util.Lookup.EMPTY));
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
    }

    @Test
    void rejectedSourceCannotBeTrustedLaterByTargetRecovery()
            throws Exception {
        MoveFixture fixture = createMoveFixture("rejected-source");
        FlutterMoveHandoff.Transaction prepared = new FlutterMoveHandoff(
                fixture.movedDirectory()).read();

        Path fakeRoot = Files.createDirectories(
                temporaryDirectory.resolve("rejected-source-fake"));
        LocalFileSystem fakeFileSystem = new LocalFileSystem();
        fakeFileSystem.setRootDirectory(fakeRoot.toFile());
        FileObject fakeDirectory = fakeFileSystem.getRoot();
        write(fakeDirectory, "pubspec.yaml", "name: source_app\nflutter:\n");
        for (var entry : prepared.snapshot().entrySet()) {
            fakeDirectory.setAttribute(
                    FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + entry.getKey(),
                    entry.getValue());
        }
        TrackingProjectState fakeState = new TrackingProjectState();
        FlutterProject fakeSource = project(
                fakeDirectory,
                fakeState,
                new FlutterProjectMetadata(fakeDirectory),
                fakeRoot);
        fakeSource.getLookup().lookup(FlutterProjectMoveOperation.class)
                .notifyMoving();

        assertThrows(IOException.class, () -> fixture.targetMove().notifyMoved(
                fakeSource,
                null,
                "must_not_apply"));
        assertNull(fixture.movedDirectory().getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));

        fixture.moved().getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertNull(fixture.movedDirectory().getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH));
        assertFalse(fixture.originalState().deleted);
        assertFalse(fakeState.deleted);
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
        closeProjectServices(fakeSource);
    }

    @Test
    void alteredSourceUriIsRejectedBeforeTargetMutation()
            throws Exception {
        MoveFixture fixture = createMoveFixture("altered-source-uri");
        FlutterMoveHandoff.Transaction prepared = new FlutterMoveHandoff(
                fixture.movedDirectory()).read();
        FileObject handoffFile = fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH);
        String altered = handoffFile.asText().replace(
                encoded(prepared.sourceUri()),
                encoded("file:///untrusted-altered-source"));
        write(fixture.movedDirectory(), FlutterMoveHandoff.HANDOFF_PATH, altered);

        assertThrows(IOException.class, () -> fixture.targetMove().notifyMoved(
                fixture.original(),
                null,
                "must_not_apply"));
        fixture.moved().getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertNull(fixture.movedDirectory().getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        assertNotNull(fixture.movedDirectory().getFileObject(
                FlutterMoveHandoff.HANDOFF_PATH));
        assertFalse(fixture.originalState().deleted);
        closeProjectServices(fixture.original());
        closeProjectServices(fixture.moved());
    }

    @Test
    void notifyMovingFlushesPendingPrivatePreferencesBeforeSnapshot()
            throws Exception {
        Path root = Files.createDirectories(
                temporaryDirectory.resolve("flush-before-snapshot"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(directory);
        AtomicBoolean flushed = new AtomicBoolean();
        FlutterProject project = new FlutterProject(
                directory,
                new TrackingProjectState(),
                new FlutterProjectInfo(
                        root,
                        "source_app",
                        root.resolve("pubspec.yaml")),
                metadata,
                null,
                ignored -> {
                    metadata.put("immediatePendingSetting", "captured", false);
                    flushed.set(true);
                });

        project.getLookup().lookup(FlutterProjectMoveOperation.class)
                .notifyMoving();

        assertTrue(flushed.get());
        FlutterMoveHandoff.Transaction transaction = new FlutterMoveHandoff(
                directory).read();
        assertTrue(((String) transaction.snapshot().get(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE))
                .contains(encoded("immediatePendingSetting")));
        assertTrue(((String) transaction.snapshot().get(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE))
                .contains(encoded("captured")));
        project.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();
        closeProjectServices(project);
    }

    @Test
    void ownedOrphanTempsAreRemovedWithoutTouchingForeignFiles()
            throws Exception {
        Path root = Files.createDirectories(
                temporaryDirectory.resolve("orphan-handoff-temps"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        for (String phase : List.of("prepared", "target", "committed")) {
            write(
                    directory,
                    ".netbeans/.flutter-move-handoff-v1-"
                            + phase + "-" + uuid + ".tmp",
                    "orphan-private-snapshot");
        }
        String foreignPath = ".netbeans/.flutter-move-handoff-v1-user.tmp";
        write(directory, foreignPath, "foreign-sentinel");
        FlutterProject project = project(
                directory,
                new TrackingProjectState(),
                new FlutterProjectMetadata(directory),
                root);

        project.getLookup().lookup(FlutterProjectMoveOperation.class)
                .notifyMoving();

        for (String phase : List.of("prepared", "target", "committed")) {
            assertNull(directory.getFileObject(
                    ".netbeans/.flutter-move-handoff-v1-"
                            + phase + "-" + uuid + ".tmp"));
        }
        assertEquals("foreign-sentinel", directory.getFileObject(foreignPath).asText());
        project.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();
        assertEquals("foreign-sentinel", directory.getFileObject(foreignPath).asText());
        closeProjectServices(project);
    }

    @Test
    void streamingParserRejectsDenseEventsDepthAndEntryOverflow()
            throws Exception {
        Path root = Files.createDirectories(
                temporaryDirectory.resolve("streaming-parser-limits"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");

        write(directory, FlutterMoveHandoff.HANDOFF_PATH,
                handoffXml("<!--dense-->".repeat(8_300)));
        assertThrows(IOException.class,
                () -> new FlutterMoveHandoff(directory).read());

        directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH).delete();
        String nested = "<entry xmlns=\"urn:dev.flutter.netbeans:move-handoff:1\" "
                + "name=\"" + encoded(
                        FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX + "depth")
                + "\" type=\"string\">"
                + "<nested>".repeat(10)
                + "</nested>".repeat(10)
                + "</entry>";
        write(directory, FlutterMoveHandoff.HANDOFF_PATH, handoffXml(nested));
        assertThrows(IOException.class,
                () -> new FlutterMoveHandoff(directory).read());

        directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH).delete();
        StringBuilder entries = new StringBuilder();
        for (int index = 0; index <= 1_024; index++) {
            entries.append("<entry name=\"")
                    .append(encoded(
                            FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                                    + "entry-" + index))
                    .append("\" type=\"string\"></entry>");
        }
        write(directory, FlutterMoveHandoff.HANDOFF_PATH,
                handoffXml(entries.toString()));
        assertThrows(IOException.class,
                () -> new FlutterMoveHandoff(directory).read());
    }

    @Test
    void unsupportedPrivateValuePreventsMoveBeforeHandoffIsCreated()
            throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("unsupported-handoff"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        directory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX
                        + FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                        + "unsupported",
                new Date(0));
        FlutterProject project = project(
                directory,
                new TrackingProjectState(),
                new FlutterProjectMetadata(directory),
                root);

        IOException failure = assertThrows(
                IOException.class,
                () -> project.getLookup().lookup(FlutterProjectMoveOperation.class)
                        .notifyMoving());

        assertTrue(failure.getMessage().contains("java.util.Date"));
        assertNull(directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH));
        closeProjectServices(project);
    }

    @Test
    void moveHandoffRejectsNetBeansDirectorySymlinkOutsideProject()
            throws Exception {
        Path root = Files.createDirectories(
                temporaryDirectory.resolve("handoff-symlink-project"));
        Path outside = Files.createDirectories(
                temporaryDirectory.resolve("handoff-symlink-outside"));
        Path externalHandoff = outside.resolve(
                ".flutter-move-handoff-v1.xml");
        Path externalTemp = outside.resolve(
                ".flutter-move-handoff-v1-prepared-"
                        + "123e4567-e89b-12d3-a456-426614174000.tmp");
        String sentinel = "outside-sentinel";
        Files.writeString(externalHandoff, sentinel, StandardCharsets.UTF_8);
        Files.writeString(externalTemp, sentinel, StandardCharsets.UTF_8);
        boolean symlinkCreated;
        try {
            Files.createSymbolicLink(root.resolve(".netbeans"), outside);
            symlinkCreated = true;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            symlinkCreated = false;
        }
        Assumptions.assumeTrue(
                symlinkCreated,
                "Symbolic-link creation is unavailable on this Windows installation");

        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(directory);
        metadata.put("selectedDeviceId", "windows", false);
        TrackingProjectState state = new TrackingProjectState();
        FlutterProject project = project(directory, state, metadata, root);

        IOException failure = assertThrows(
                IOException.class,
                () -> project.getLookup().lookup(FlutterProjectMoveOperation.class)
                        .notifyMoving());
        assertTrue(failure.getMessage().contains("outside project"));
        assertEquals(sentinel, Files.readString(
                externalHandoff,
                StandardCharsets.UTF_8));
        assertEquals(sentinel, Files.readString(
                externalTemp,
                StandardCharsets.UTF_8));

        project.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertEquals(sentinel, Files.readString(
                externalHandoff,
                StandardCharsets.UTF_8));
        assertEquals(sentinel, Files.readString(
                externalTemp,
                StandardCharsets.UTF_8));
        assertFalse(state.deleted);
        closeProjectServices(project);
    }

    @Test
    void preparedHandoffOnOriginalPathIsCleanedAsAbortedMove()
            throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("aborted-move"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        FlutterProjectMetadata metadata = new FlutterProjectMetadata(directory);
        metadata.put("selectedDeviceId", "windows", false);
        TrackingProjectState state = new TrackingProjectState();
        FlutterProject project = project(directory, state, metadata, root);
        project.getLookup().lookup(FlutterProjectMoveOperation.class)
                .notifyMoving();
        assertNotNull(directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH));

        project.getLookup().lookup(FlutterProjectLifecycle.class)
                .recoverMetadataOnOpen();

        assertNoHandoff(directory);
        assertFalse(state.deleted);
        assertEquals("windows", metadata.get("selectedDeviceId", false));
        closeProjectServices(project);
    }

    @Test
    void malformedHandoffRemainsWithoutChangingPrivateAttributes()
            throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("malformed-handoff"));
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        write(directory, "pubspec.yaml", "name: source_app\nflutter:\n");
        write(directory, FlutterMoveHandoff.HANDOFF_PATH, """
                <!DOCTYPE move-handoff [<!ENTITY xxe SYSTEM "file:///C:/Windows/win.ini">]>
                <move-handoff xmlns="urn:dev.flutter.netbeans:move-handoff:1"
                              version="1">&xxe;</move-handoff>
                """);
        FlutterProject project = project(
                directory,
                new TrackingProjectState(),
                new FlutterProjectMetadata(directory),
                root);

        assertEquals(
                FlutterProjectMoveOperation.RecoveryResult.BLOCKED,
                project.getLookup().lookup(FlutterProjectLifecycle.class)
                        .recoverMetadataOnOpen());

        assertNotNull(directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH));
        assertNull(directory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE));
        FlutterProjectActionProvider actions = project.getLookup()
                .lookup(FlutterProjectActionProvider.class);
        assertFalse(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_RENAME,
                org.openide.util.Lookup.EMPTY));
        assertTrue(actions.isActionEnabled(
                org.netbeans.spi.project.ActionProvider.COMMAND_DELETE,
                org.openide.util.Lookup.EMPTY));
        closeProjectServices(project);
    }

    private FlutterProject project(
            FileObject directory,
            ProjectState state,
            FlutterProjectMetadata metadata,
            Path modelRoot) throws Exception {
        return project(directory, state, metadata, modelRoot, null);
    }

    private FlutterProject project(
            FileObject directory,
            ProjectState state,
            FlutterProjectMetadata metadata,
            Path modelRoot,
            FlutterProjectMoveOperation.PrivateStateWriter moveStateWriter)
            throws Exception {
        Files.createDirectories(modelRoot);
        return new FlutterProject(
                directory,
                state,
                new FlutterProjectInfo(
                        modelRoot,
                        "source_app",
                        modelRoot.resolve("pubspec.yaml")),
                metadata,
                moveStateWriter,
                ignored -> {
                });
    }

    private MoveFixture createMoveFixture(String name) throws Exception {
        return createMoveFixture(name, null);
    }

    private MoveFixture createMoveFixture(
            String name,
            FlutterProjectMoveOperation.PrivateStateWriter targetStateWriter)
            throws Exception {
        Path sourcePath = Files.createDirectories(
                temporaryDirectory.resolve(name + "-source"));
        AttributeDroppingLocalFileSystem sourceFileSystem =
                new AttributeDroppingLocalFileSystem(sourcePath);
        FileObject sourceDirectory = FileUtil.createFolder(
                sourceFileSystem.getRoot(),
                "source_app");
        write(sourceDirectory, "pubspec.yaml", "name: source_app\nflutter:\n");
        write(sourceDirectory, "lib/main.dart", "void main() {}\n");

        FlutterProjectMetadata sourceMetadata = new FlutterProjectMetadata(sourceDirectory);
        sourceMetadata.put("selectedDeviceId", "windows", false);
        String legacyQuarantine = FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                + name;
        String externalQuarantine = FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX
                + name;
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + legacyQuarantine,
                42);
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + externalQuarantine,
                "raw-corrupt-payload");

        TrackingProjectState originalState = new TrackingProjectState();
        FlutterProject original = project(
                sourceDirectory,
                originalState,
                sourceMetadata,
                sourcePath.resolve("source_app"));
        FlutterProjectMoveOperation sourceMove = original.getLookup()
                .lookup(FlutterProjectMoveOperation.class);
        sourceMove.notifyRenamed("Original Display");
        String byteArrayQuarantine = FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX
                + name + "-bytes";
        String urlQuarantine = FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX
                + name + "-url";
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + byteArrayQuarantine,
                new byte[]{0, 1, 2, -1});
        sourceDirectory.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + urlQuarantine,
                new URI("https://example.invalid/flutter?q=move").toURL());
        sourceMove.notifyMoving();

        Path targetPath = Files.createDirectories(
                temporaryDirectory.resolve(name + "-target"));
        AttributeDroppingLocalFileSystem targetFileSystem =
                new AttributeDroppingLocalFileSystem(targetPath);
        FileObject movedDirectory;
        try (FileLock lock = sourceDirectory.lock()) {
            movedDirectory = sourceDirectory.move(
                    lock,
                    targetFileSystem.getRoot(),
                    "moved_app",
                    null);
        }
        FlutterProjectMetadata movedMetadata = new FlutterProjectMetadata(movedDirectory);
        FlutterProject moved = project(
                movedDirectory,
                new TrackingProjectState(),
                movedMetadata,
                targetPath.resolve("moved_app"),
                targetStateWriter);
        return new MoveFixture(
                original,
                moved,
                originalState,
                sourceMove,
                moved.getLookup().lookup(FlutterProjectMoveOperation.class),
                movedMetadata,
                movedDirectory,
                legacyQuarantine,
                externalQuarantine,
                byteArrayQuarantine,
                urlQuarantine);
    }

    private static void clearPrivateState(MoveFixture fixture) throws IOException {
        fixture.movedDirectory().setAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                null);
        fixture.movedDirectory().setAttribute(fixture.legacyQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.externalQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.byteArrayQuarantine(), null);
        fixture.movedDirectory().setAttribute(fixture.urlQuarantine(), null);
    }

    private static void assertNoHandoff(FileObject directory) {
        assertNull(directory.getFileObject(FlutterMoveHandoff.HANDOFF_PATH));
        assertNull(directory.getFileObject(FlutterMoveHandoff.TARGET_HANDOFF_PATH));
        assertNull(directory.getFileObject(FlutterMoveHandoff.COMMITTED_HANDOFF_PATH));
    }

    private static void write(FileObject root, String relativePath, String value)
            throws Exception {
        FileObject file = FileUtil.createData(root, relativePath);
        try (OutputStream output = file.getOutputStream()) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String handoffXml(String body) {
        return "<move-handoff xmlns=\"urn:dev.flutter.netbeans:move-handoff:1\""
                + " version=\"1\" phase=\"prepared\""
                + " transaction-id=\"123e4567-e89b-12d3-a456-426614174000\""
                + " source-uri=\"" + encoded("file:///stream-source") + "\">"
                + body
                + "</move-handoff>";
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void closeProjectServices(FlutterProject project) {
        project.getLookup().lookup(FlutterRunController.class).close();
        project.getLookup().lookup(FlutterToolingController.class).close();
    }

    private static final class TrackingProjectState implements ProjectState {
        private boolean deleted;
        private boolean failNextDeletion;

        @Override
        public void markModified() {
        }

        @Override
        public void notifyDeleted() {
            if (failNextDeletion) {
                failNextDeletion = false;
                throw new IllegalStateException("Injected source deletion failure");
            }
            deleted = true;
        }

        void failNextDeletion() {
            failNextDeletion = true;
        }
    }

    private record MoveFixture(
            FlutterProject original,
            FlutterProject moved,
            TrackingProjectState originalState,
            FlutterProjectMoveOperation sourceMove,
            FlutterProjectMoveOperation targetMove,
            FlutterProjectMetadata movedMetadata,
            FileObject movedDirectory,
            String legacyQuarantine,
            String externalQuarantine,
            String byteArrayQuarantine,
            String urlQuarantine) {
    }

    /** Transfer provider that moves file content but owns attributes out-of-band. */
    private static final class AttributeDroppingLocalFileSystem extends LocalFileSystem {
        AttributeDroppingLocalFileSystem(Path root) throws Exception {
            setRootDirectory(root.toFile());
            transfer = new AttributeDroppingTransfer(root);
        }
    }

    private static final class AttributeDroppingTransfer
            implements AbstractFileSystem.Transfer, Serializable {
        private static final long serialVersionUID = 1L;

        private final String root;

        AttributeDroppingTransfer(Path root) {
            this.root = root.toAbsolutePath().normalize().toString();
        }

        @Override
        public boolean move(
                String name,
                AbstractFileSystem.Transfer target,
                String targetName) throws IOException {
            if (!(target instanceof AttributeDroppingTransfer destination)) {
                return false;
            }
            Files.move(resolve(name), destination.resolve(targetName));
            return true;
        }

        @Override
        public boolean copy(
                String name,
                AbstractFileSystem.Transfer target,
                String targetName) {
            return false;
        }

        private Path resolve(String relativePath) {
            String normalized = relativePath.replace('\\', '/');
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return Path.of(root).resolve(normalized).normalize();
        }
    }
}
