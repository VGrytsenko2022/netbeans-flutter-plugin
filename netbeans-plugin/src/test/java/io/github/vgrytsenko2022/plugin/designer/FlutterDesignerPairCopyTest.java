package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.template.DesignerFormTemplate;
import io.github.vgrytsenko2022.designer.template.DesignerFormTemplateFactory;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/** Contract for publishing one independent copy of a Designer pair. */
class FlutterDesignerPairCopyTest {
    private static final String TRANSACTION_ID = "test-transaction-0001";
    private static final StableId TARGET_DOCUMENT_ID = StableId.parse(
            "5fa2ed52-a5e3-4e48-a627-130ca90e13d1");
    private static final byte[] UNOWNED_CREATE_BYTES =
            "unowned create bytes must survive rollback\n"
                    .getBytes(StandardCharsets.UTF_8);
    private static final byte[] FOREIGN_RACE_BYTES =
            "foreign bytes must not be mutated by Copy\n"
                    .getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temporaryDirectory;

    @Test
    void netBeans30DoesNotReuseDeletedFileObjectIdentityAtTheSamePath()
            throws Exception {
        Path folderPath = temporaryDirectory.resolve("file_object_aba");
        Files.createDirectories(folderPath);
        FileObject folder = FileUtil.toFileObject(folderPath.toFile());
        assertNotNull(folder);
        FileObject original = folder.createData("staging", "dart");
        try (FileLock lock = original.lock()) {
            original.delete(lock);
        }

        FileObject replacement = folder.createData("staging", "dart");

        assertFalse(original.isValid());
        assertFalse(original == replacement,
                "same-path recreation must not inherit the owned FileObject identity");
    }

    @Test
    void completeCleanPairIsAnAllowedCopyCandidate() throws Exception {
        PairFixture fixture = pair("copy_enabled");
        FlutterDesignerDataObject sourceOwner = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.project());
        FlutterDesignerPairCopy transaction = transaction(
                new FailureBackend());

        assertFalse(sourceOwner.isModified());
        assertTrue(transaction.canCopy(fixture.pair()));
    }

    @Test
    void copyingFromDartPublishesIndependentExactPair() throws Exception {
        PairFixture fixture = pair("copy_from_dart");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairCopy.CopyResult result = transaction(backend)
                .copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent());

        assertCommittedCopy(
                fixture, result, exactDart, exactModel, "home_copy");
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void copyingFromModelPublishesIndependentExactPair() throws Exception {
        PairFixture fixture = pair("copy_from_model");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairCopy.CopyResult result = transaction(backend)
                .copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent());

        assertCommittedCopy(
                fixture, result, exactDart, exactModel, "home_copy");
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void eitherMirrorCollisionSelectsOneSharedFreeSuffix() throws Exception {
        PairFixture dartCollision = pair("dart_collision");
        byte[] occupiedDart = "foreign dart\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(
                dartCollision.dartPath().resolveSibling("home_copy.dart"),
                occupiedDart);
        refresh(dartCollision.projectPath());

        FlutterDesignerPairCopy.CopyResult dartResult = transaction(
                new FailureBackend()).copyResolved(
                        dartCollision.pair(),
                        dartCollision.pair().dartFile(),
                        dartCollision.pair().dartFile().getParent());

        assertEquals("home_copy_2.dart", dartResult.dartFile().getNameExt());
        assertEquals("home_copy_2.fd", dartResult.modelFile().getNameExt());
        assertArrayEquals(occupiedDart, Files.readAllBytes(
                dartCollision.dartPath().resolveSibling("home_copy.dart")));
        assertEquals("home_copy_2.dart",
                decode(dartResult.modelFile()).source().dartFile());
        assertNoStagingFiles(dartCollision);

        PairFixture modelCollision = pair("model_collision");
        byte[] occupiedModel = "foreign model\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(
                modelCollision.modelPath().resolveSibling("home_copy.fd"),
                occupiedModel);
        refresh(modelCollision.projectPath());

        FlutterDesignerPairCopy.CopyResult modelResult = transaction(
                new FailureBackend()).copyResolved(
                        modelCollision.pair(),
                        modelCollision.pair().modelFile(),
                        modelCollision.pair().modelFile().getParent());

        assertEquals("home_copy_2.dart", modelResult.dartFile().getNameExt());
        assertEquals("home_copy_2.fd", modelResult.modelFile().getNameExt());
        assertArrayEquals(occupiedModel, Files.readAllBytes(
                modelCollision.modelPath().resolveSibling("home_copy.fd")));
        assertEquals("home_copy_2.dart",
                decode(modelResult.modelFile()).source().dartFile());
        assertNoStagingFiles(modelCollision);
    }

    @Test
    void invalidAndSourceMismatchedModelsFailBeforeMutation()
            throws Exception {
        PairFixture invalid = pair(
                "invalid_model",
                new DesignerFormTemplateFactory().create(
                        "home.dart", "HomePage"),
                "{ invalid json\n".getBytes(StandardCharsets.UTF_8));
        FailureBackend invalidBackend = new FailureBackend();

        IOException invalidFailure = assertThrows(
                IOException.class,
                () -> transaction(invalidBackend).copyResolved(
                        invalid.pair(),
                        invalid.pair().dartFile(),
                        invalid.pair().dartFile().getParent()));

        assertTrue(invalidFailure.getMessage().contains("MODEL_NOT_CURRENT"));
        assertNoPublishedCopy(invalid);
        assertNoStagingFiles(invalid);
        assertEquals(0, invalidBackend.createAttempts());

        DesignerFormTemplate mismatchTemplate =
                new DesignerFormTemplateFactory().create(
                        "different.dart", "HomePage");
        PairFixture mismatch = pair(
                "source_mismatch", mismatchTemplate,
                mismatchTemplate.fdBytes());
        FailureBackend mismatchBackend = new FailureBackend();

        IOException mismatchFailure = assertThrows(
                IOException.class,
                () -> transaction(mismatchBackend).copyResolved(
                        mismatch.pair(),
                        mismatch.pair().modelFile(),
                        mismatch.pair().modelFile().getParent()));

        assertTrue(mismatchFailure.getMessage().contains(
                "SOURCE_REFERENCE_MISMATCH"));
        assertNoPublishedCopy(mismatch);
        assertNoStagingFiles(mismatch);
        assertEquals(0, mismatchBackend.createAttempts());
    }

    @Test
    void dirtySharedDartOwnerDisablesCopyWithoutChangingDisk()
            throws Exception {
        PairFixture fixture = pair("dirty_source");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FlutterDesignerDataObject dartObject = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.project());
        var document = dartObject.getEditorSupport().openDocument();
        FlutterDesignerPairCopy transaction = transaction(
                new FailureBackend());

        document.insertString(document.getLength(), "// unsaved edit\n", null);
        try {
            assertTrue(dartObject.isModified());
            assertFalse(transaction.canCopy(fixture.pair()));
            assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()));
            assertNoPublishedCopy(fixture);
            assertNoStagingFiles(fixture);
        } finally {
            dartObject.setModified(false);
            assertTrue(dartObject.getEditorSupport().close(),
                    "the dirty Copy fixture must close without prompting");
        }
    }

    @Test
    void readOnlySourceFilesRemainCopyableWhenTheirParentsAreWritable()
            throws Exception {
        PairFixture fixture = pair("read_only_source");
        FlutterDesignerDataObject sourceOwner = FlutterDesignerTestProject.dataObject(
                fixture.pair().dartFile(), fixture.project());
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());

        setReadOnlyOrSkip(fixture.dartPath(), fixture.pair().dartFile());
        try {
            setReadOnlyOrSkip(
                    fixture.modelPath(), fixture.pair().modelFile());
            try {
                FlutterDesignerPairCopy transaction = transaction(
                        new FailureBackend());
                assertTrue(fixture.pair().dartFile().getParent().canWrite());
                assertTrue(fixture.pair().modelFile().getParent().canWrite());
                assertTrue(org.openide.loaders.DataObject.find(
                        fixture.pair().dartFile()) == sourceOwner,
                        "the read-only source must retain its Designer DataObject owner");
                assertFalse(sourceOwner.isModified());
                assertTrue(sourceOwner.getPairSaveCoordinator()
                        .canBeginPairCopy(),
                        () -> "copy lease rejected in state "
                        + sourceOwner.getPairSaveCoordinator().state());
                assertTrue(transaction.canCopy(fixture.pair()));

                FlutterDesignerPairCopy.CopyResult result = transaction
                        .copyResolved(
                                fixture.pair(),
                                fixture.pair().dartFile(),
                                fixture.pair().dartFile().getParent());

                assertCommittedCopy(
                        fixture, result, exactDart, exactModel, "home_copy");
            } finally {
                restoreWritable(fixture.modelPath());
            }
        } finally {
            restoreWritable(fixture.dartPath());
        }
    }

    @Test
    void readOnlyDestinationFolderDisablesCopyWhenExposedByFilesystem()
            throws Exception {
        PairFixture fixture = pair("read_only_destination");
        FlutterDesignerTestProject.dataObject(
                fixture.pair().dartFile(), fixture.project());
        Path destination = fixture.dartPath().getParent();
        FileObject destinationFolder = fixture.pair().dartFile().getParent();

        setDirectoryReadOnlyOrSkip(destination, destinationFolder);
        try {
            assertFalse(transaction(new FailureBackend())
                    .canCopy(fixture.pair()));
            assertNoPublishedCopy(fixture);
        } finally {
            restoreWritable(destination);
        }
    }

    @Test
    void secondCreateFailureRollsBackTheExactFirstStagingFile()
            throws Exception {
        PairFixture fixture = pair("create_rollback");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failCreate(2, false);

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "injected create failure at attempt 2"));
        assertExactSource(fixture, exactDart, exactModel);
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(2, backend.createAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void postWriteVerificationFailureRollsBackBothExactStagingFiles()
            throws Exception {
        PairFixture fixture = pair("write_rollback");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend =
                FailureBackend.failVerificationAfterWrite(2);

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "injected post-write verification failure"));
        assertExactSource(fixture, exactDart, exactModel);
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(2, backend.createAttempts());
        assertEquals(2, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(2, backend.deleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void secondPublishFailureRollsBackTargetAndStagingFiles()
            throws Exception {
        PairFixture fixture = pair("publish_rollback");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failRename(2, false);

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "injected rename failure at attempt 2"));
        assertExactSource(fixture, exactDart, exactModel);
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(2, backend.createAttempts());
        assertEquals(2, backend.writeAttempts());
        assertEquals(2, backend.renameAttempts());
        assertEquals(2, backend.deleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void exactWriteSuccessThenProviderFailureStillCommitsCopy()
            throws Exception {
        PairFixture fixture = pair("post_write_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failWrite(2, true);

        FlutterDesignerPairCopy.CopyResult result = transaction(backend)
                .copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent());

        assertCommittedCopy(
                fixture, result, exactDart, exactModel, "home_copy");
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void exactPublishSuccessThenProviderFailureStillCommitsCopy()
            throws Exception {
        PairFixture fixture = pair("post_publish_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failRename(2, true);

        FlutterDesignerPairCopy.CopyResult result = transaction(backend)
                .copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent());

        assertCommittedCopy(
                fixture, result, exactDart, exactModel, "home_copy");
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void atomicDelegateSuccessThenWrapperFailureRetainsExactCommittedCopy()
            throws Exception {
        PairFixture fixture = pair("post_atomic_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failAtomicAfterDelegate();

        FlutterDesignerPairCopy.CopyResult result = transaction(backend)
                .copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent());

        assertCommittedCopy(
                fixture, result, exactDart, exactModel, "home_copy");
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void createSuccessThenFailureNeverDeletesUnownedObservedPath()
            throws Exception {
        PairFixture fixture = pair("unowned_create");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failCreate(2, true);
        Path unownedModel = fixture.modelPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".fd");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "injected create failure at attempt 2 after physical create"));
        assertTrue(failure.getSuppressed().length > 0,
                "refusing unowned cleanup must report a recovery conflict");
        assertTrue(failure.getSuppressed()[0].getMessage().contains(
                "pair-Copy recovery conflict"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(unownedModel));
        assertArrayEquals(
                UNOWNED_CREATE_BYTES, Files.readAllBytes(unownedModel));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                "home_copy.dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                "home_copy.fd")));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart")));
        assertEquals(2, backend.createAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts(),
                "rollback may delete only the exact returned Dart artifact");
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void movedStagingFileIsNotWrittenAtAnUnknownPath() throws Exception {
        PairFixture fixture = pair("ownership_bound_write");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.moveBeforeWrite(1);
        Path foreign = fixture.dartPath().resolveSibling(
                ".foreign-copy-write-1.dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "no longer owns its expected path"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(foreign));
        assertArrayEquals(FOREIGN_RACE_BYTES, Files.readAllBytes(foreign));
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.deleteAttempts());
    }

    @Test
    void movedStagingFileIsNotPublishedFromAnUnknownPath() throws Exception {
        PairFixture fixture = pair("ownership_bound_publish");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.moveBeforeRename(1);
        Path foreign = fixture.dartPath().resolveSibling(
                ".foreign-copy-publish-1.dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent()));

        assertTrue(failure.getMessage().contains(
                "no longer owns its expected path"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(foreign));
        assertArrayEquals(FOREIGN_RACE_BYTES, Files.readAllBytes(foreign));
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(2, backend.writeAttempts());
        assertEquals(1, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts(),
                "rollback may delete only the still-owned model staging file");
    }

    @Test
    void movedArtifactIsNotDeletedAtAnUnknownPath() throws Exception {
        PairFixture fixture = pair("ownership_bound_delete");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failCreate(2, false)
                .moveBeforeDelete(1);
        Path foreign = fixture.dartPath().resolveSibling(
                ".foreign-copy-delete-1.dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getSuppressed().length > 0,
                "ownership refusal must remain visible as a recovery conflict");
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(foreign));
        assertArrayEquals(FOREIGN_RACE_BYTES, Files.readAllBytes(foreign));
        assertNoPublishedCopy(fixture);
        assertNoStagingFiles(fixture);
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts());
    }

    @Test
    void changedStagingBytesAreNotOverwrittenDuringOwnedWrite()
            throws Exception {
        PairFixture fixture = pair("ownership_bound_write_bytes");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.changeBeforeWrite(1);
        Path changedStaging = fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getMessage().contains("staging bytes changed"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(changedStaging));
        assertArrayEquals(
                FOREIGN_RACE_BYTES, Files.readAllBytes(changedStaging));
        assertNoPublishedCopy(fixture);
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.deleteAttempts());
    }

    @Test
    void changedStagingBytesAreNotPublishedAsAnOwnedCopy()
            throws Exception {
        PairFixture fixture = pair("ownership_bound_publish_bytes");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.changeBeforeRename(1);
        Path changedStaging = fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent()));

        assertTrue(failure.getMessage().contains("owned bytes changed"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(changedStaging));
        assertArrayEquals(
                FOREIGN_RACE_BYTES, Files.readAllBytes(changedStaging));
        assertNoPublishedCopy(fixture);
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".fd")));
        assertEquals(2, backend.writeAttempts());
        assertEquals(1, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts());
    }

    @Test
    void samePathReplacementBeforeWriteIsNotOverwritten()
            throws Exception {
        PairFixture fixture = pair("ownership_bound_write_aba");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.replaceBeforeWrite(1);
        Path replacement = fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getSuppressed().length > 0,
                "same-path replacement must be reported as a recovery conflict");
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(replacement));
        assertArrayEquals(FOREIGN_RACE_BYTES, Files.readAllBytes(replacement));
        assertNoPublishedCopy(fixture);
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.deleteAttempts());
    }

    @Test
    void targetCreatedDuringPublishIsNotOverwritten()
            throws Exception {
        PairFixture fixture = pair("ownership_bound_target_collision");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.occupyBeforeRename(1);
        Path occupiedTarget = fixture.dartPath().resolveSibling(
                "home_copy.dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        fixture.pair().modelFile().getParent()));

        assertTrue(failure.getMessage().contains("target became occupied"));
        assertExactSource(fixture, exactDart, exactModel);
        assertArrayEquals(
                FOREIGN_RACE_BYTES, Files.readAllBytes(occupiedTarget));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                "home_copy.fd")));
        assertNoStagingFiles(fixture);
        assertEquals(2, backend.writeAttempts());
        assertEquals(1, backend.renameAttempts());
        assertEquals(2, backend.deleteAttempts());
    }

    @Test
    void samePathReplacementBeforeDeleteIsNotDeleted()
            throws Exception {
        PairFixture fixture = pair("ownership_bound_delete_aba");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failCreate(2, false)
                .replaceBeforeDelete(1);
        Path replacement = fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).copyResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        fixture.pair().dartFile().getParent()));

        assertTrue(failure.getSuppressed().length > 0,
                "same-path replacement must be reported as a recovery conflict");
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(replacement));
        assertArrayEquals(FOREIGN_RACE_BYTES, Files.readAllBytes(replacement));
        assertNoPublishedCopy(fixture);
        assertEquals(1, backend.writeAttempts());
        assertEquals(0, backend.renameAttempts());
        assertEquals(1, backend.deleteAttempts());
    }

    private void assertCommittedCopy(
            PairFixture fixture,
            FlutterDesignerPairCopy.CopyResult result,
            byte[] exactDart,
            byte[] exactModel,
            String targetStem) throws Exception {
        Path targetDart = fixture.dartPath().resolveSibling(
                targetStem + ".dart");
        Path targetModel = fixture.modelPath().resolveSibling(
                targetStem + ".fd");
        assertTrue(result.committed());
        assertEquals(targetStem + ".dart", result.dartFile().getNameExt());
        assertEquals(targetStem + ".fd", result.modelFile().getNameExt());
        assertExactSource(fixture, exactDart, exactModel);
        assertArrayEquals(exactDart, Files.readAllBytes(targetDart),
                "Copy must preserve user-owned Dart bytes exactly");

        DesignerDocument original = fixture.template().document();
        DesignerDocument copied = decode(result.modelFile());
        assertEquals(TARGET_DOCUMENT_ID, copied.documentId());
        assertNotEquals(original.documentId(), copied.documentId());
        assertEquals(targetStem + ".dart", copied.source().dartFile());
        assertEquals(original.source().className(), copied.source().className());
        assertEquals(original.source().widgetKind(), copied.source().widgetKind());
        assertEquals(original.source().generatorVersion(),
                copied.source().generatorVersion());
        assertEquals(original.source().managedRegions(),
                copied.source().managedRegions());
        assertEquals(original.schemaReference(), copied.schemaReference());
        assertEquals(original.canvas(), copied.canvas());
        assertEquals(original.root(), copied.root());
        assertEquals(original.extensions(), copied.extensions());
        assertNoStagingFiles(fixture);
    }

    private static DesignerDocument decode(FileObject model)
            throws Exception {
        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(model.asBytes()));
        assertFalse(decoded.migrated());
        return decoded.document();
    }

    private static void assertSuccessfulBackendCounts(
            FailureBackend backend) {
        assertEquals(2, backend.createAttempts());
        assertEquals(2, backend.writeAttempts());
        assertEquals(2, backend.renameAttempts());
        assertEquals(0, backend.deleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    private static void assertExactSource(
            PairFixture fixture,
            byte[] exactDart,
            byte[] exactModel) throws IOException {
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
        assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()));
    }

    private static void assertNoPublishedCopy(PairFixture fixture) {
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                "home_copy.dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                "home_copy.fd")));
    }

    private static void assertNoStagingFiles(PairFixture fixture) {
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                ".nb-flutter-copy-" + TRANSACTION_ID + ".fd")));
    }

    private PairFixture pair(String name) throws Exception {
        DesignerFormTemplate template = new DesignerFormTemplateFactory()
                .create("home.dart", "HomePage");
        return pair(name, template, template.fdBytes());
    }

    private PairFixture pair(
            String name,
            DesignerFormTemplate template,
            byte[] modelBytes) throws Exception {
        Path projectPath = temporaryDirectory.resolve(name);
        Path dartPath = projectPath.resolve("lib/screens/home.dart");
        Path modelPath = projectPath.resolve(
                ".fd_templates/screens/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.writeString(projectPath.resolve("pubspec.yaml"), """
                name: pair_copy_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        Files.write(dartPath, template.dartBytes());
        Files.write(modelPath, modelBytes);
        refresh(projectPath);
        FlutterProject project = FlutterDesignerTestProject.own(projectPath);
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dartFile);
        FlutterDesignerPairLayout.Pair resolved = FlutterDesignerPairLayout
                .findCompletePair(dartFile, project)
                .orElseThrow();
        return new PairFixture(
                projectPath, dartPath, modelPath, project, resolved, template);
    }

    private static FlutterDesignerPairCopy transaction(
            FailureBackend backend) {
        return new FlutterDesignerPairCopy(
                backend,
                () -> TRANSACTION_ID,
                () -> TARGET_DOCUMENT_ID);
    }

    private static void setReadOnlyOrSkip(Path path, FileObject fileObject)
            throws IOException {
        File file = path.toFile();
        boolean changed = file.setWritable(false, false);
        refresh(path);
        Assumptions.assumeTrue(changed && !fileObject.canWrite(),
                "the current filesystem does not expose a read-only file to NetBeans");
    }

    private static void setDirectoryReadOnlyOrSkip(
            Path path,
            FileObject folder) throws IOException {
        boolean changed = path.toFile().setWritable(false, false);
        refresh(path);
        Assumptions.assumeTrue(changed && !folder.canWrite(),
                "the current filesystem does not expose a read-only folder to NetBeans");
    }

    private static void restoreWritable(Path path) {
        path.toFile().setWritable(true, false);
        refresh(path);
    }

    private static void refresh(Path path) {
        FileUtil.refreshFor(path.toFile());
    }

    private record PairFixture(
            Path projectPath,
            Path dartPath,
            Path modelPath,
            FlutterProject project,
            FlutterDesignerPairLayout.Pair pair,
            DesignerFormTemplate template) {
    }

    private static final class FailureBackend
            implements FlutterDesignerPairCopy.Backend {
        private int failingCreateAttempt = -1;
        private boolean failCreateAfterMutation;
        private int failingWriteAttempt = -1;
        private boolean failWriteAfterMutation;
        private int failingRenameAttempt = -1;
        private boolean failRenameAfterMutation;
        private int verificationFailureAfterWriteAttempt = -1;
        private boolean failNextRead;
        private boolean failAtomicAfterDelegate;
        private int moveBeforeWriteAttempt = -1;
        private int moveBeforeRenameAttempt = -1;
        private int moveBeforeDeleteAttempt = -1;
        private int changeBeforeWriteAttempt = -1;
        private int changeBeforeRenameAttempt = -1;
        private int replaceBeforeWriteAttempt = -1;
        private int replaceBeforeDeleteAttempt = -1;
        private int occupyBeforeRenameAttempt = -1;
        private int createAttempts;
        private int writeAttempts;
        private int renameAttempts;
        private int deleteAttempts;
        private int atomicActionAttempts;

        static FailureBackend failCreate(int attempt, boolean afterMutation) {
            FailureBackend backend = new FailureBackend();
            backend.failingCreateAttempt = attempt;
            backend.failCreateAfterMutation = afterMutation;
            return backend;
        }

        static FailureBackend failWrite(int attempt, boolean afterMutation) {
            FailureBackend backend = new FailureBackend();
            backend.failingWriteAttempt = attempt;
            backend.failWriteAfterMutation = afterMutation;
            return backend;
        }

        static FailureBackend failRename(int attempt, boolean afterMutation) {
            FailureBackend backend = new FailureBackend();
            backend.failingRenameAttempt = attempt;
            backend.failRenameAfterMutation = afterMutation;
            return backend;
        }

        static FailureBackend failVerificationAfterWrite(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.verificationFailureAfterWriteAttempt = attempt;
            return backend;
        }

        static FailureBackend failAtomicAfterDelegate() {
            FailureBackend backend = new FailureBackend();
            backend.failAtomicAfterDelegate = true;
            return backend;
        }

        static FailureBackend moveBeforeWrite(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.moveBeforeWriteAttempt = attempt;
            return backend;
        }

        static FailureBackend moveBeforeRename(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.moveBeforeRenameAttempt = attempt;
            return backend;
        }

        static FailureBackend changeBeforeWrite(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.changeBeforeWriteAttempt = attempt;
            return backend;
        }

        static FailureBackend changeBeforeRename(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.changeBeforeRenameAttempt = attempt;
            return backend;
        }

        static FailureBackend replaceBeforeWrite(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.replaceBeforeWriteAttempt = attempt;
            return backend;
        }

        static FailureBackend occupyBeforeRename(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.occupyBeforeRenameAttempt = attempt;
            return backend;
        }

        FailureBackend moveBeforeDelete(int attempt) {
            moveBeforeDeleteAttempt = attempt;
            return this;
        }

        FailureBackend replaceBeforeDelete(int attempt) {
            replaceBeforeDeleteAttempt = attempt;
            return this;
        }

        @Override
        public Path realPath(FileObject file) throws IOException {
            File local = FileUtil.toFile(file);
            if (local == null) {
                throw new IOException("test file is not local");
            }
            return local.toPath().toRealPath();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            if (failNextRead) {
                failNextRead = false;
                throw new IOException(
                        "injected post-write verification failure");
            }
            return file.asBytes();
        }

        @Override
        public FileObject create(
                FileObject parent,
                String name,
                String extension) throws IOException {
            createAttempts++;
            boolean injected = createAttempts == failingCreateAttempt;
            if (injected && !failCreateAfterMutation) {
                throw new IOException(
                        "injected create failure at attempt " + createAttempts);
            }
            FileObject created = parent.createData(name, extension);
            if (injected) {
                try (FileLock lock = created.lock();
                        OutputStream output = created.getOutputStream(lock)) {
                    output.write(UNOWNED_CREATE_BYTES);
                }
                throw new IOException(
                        "injected create failure at attempt " + createAttempts
                        + " after physical create");
            }
            return created;
        }

        @Override
        public void writeOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] bytes) throws IOException {
            writeAttempts++;
            boolean injected = writeAttempts == failingWriteAttempt;
            if (injected && !failWriteAfterMutation) {
                throw new IOException(
                        "injected write failure at attempt " + writeAttempts);
            }
            if (writeAttempts == moveBeforeWriteAttempt) {
                moveToForeign(file, "write", writeAttempts);
            }
            if (writeAttempts == changeBeforeWriteAttempt) {
                writeForeignBytes(file);
            }
            if (writeAttempts == replaceBeforeWriteAttempt) {
                replaceAtExpectedPath(
                        file, expectedParent, expectedName, expectedExtension);
            }
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "write");
                if (file.getSize() != 0L || file.asBytes().length != 0) {
                    throw new IOException(
                            "refusing test write because staging bytes changed");
                }
                try (OutputStream output = file.getOutputStream(lock)) {
                    output.write(bytes);
                }
            }
            if (writeAttempts == verificationFailureAfterWriteAttempt) {
                failNextRead = true;
            }
            if (injected) {
                throw new IOException(
                        "injected write failure at attempt " + writeAttempts
                        + " after physical write");
            }
        }

        @Override
        public void publishOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            renameAttempts++;
            boolean injected = renameAttempts == failingRenameAttempt;
            if (injected && !failRenameAfterMutation) {
                throw new IOException(
                        "injected rename failure at attempt " + renameAttempts);
            }
            if (renameAttempts == moveBeforeRenameAttempt) {
                moveToForeign(file, "publish", renameAttempts);
            }
            if (renameAttempts == changeBeforeRenameAttempt) {
                writeForeignBytes(file);
            }
            if (renameAttempts == occupyBeforeRenameAttempt) {
                FileObject occupied = expectedParent.createData(
                        targetName, targetExtension);
                writeForeignBytes(occupied);
            }
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "publish");
                if (!java.util.Arrays.equals(expectedBytes, file.asBytes())) {
                    throw new IOException(
                            "refusing test publish because owned bytes changed");
                }
                if (expectedParent.getFileObject(
                        targetName, targetExtension) != null) {
                    throw new IOException(
                            "refusing test publish because target became occupied");
                }
                file.rename(lock, targetName, targetExtension);
            }
            if (injected) {
                throw new IOException(
                        "injected rename failure at attempt " + renameAttempts
                        + " after physical rename");
            }
        }

        @Override
        public void deleteOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException {
            deleteAttempts++;
            if (deleteAttempts == moveBeforeDeleteAttempt) {
                moveToForeign(file, "delete", deleteAttempts);
            }
            if (deleteAttempts == replaceBeforeDeleteAttempt) {
                replaceAtExpectedPath(
                        file, expectedParent, expectedName, expectedExtension);
            }
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "delete");
                if (!java.util.Arrays.equals(expectedBytes, file.asBytes())) {
                    throw new IOException(
                            "refusing test delete because owned bytes changed");
                }
                file.delete(lock);
            }
        }

        private static void moveToForeign(
                FileObject file,
                String operation,
                int attempt) throws IOException {
            try (FileLock lock = file.lock()) {
                file.rename(
                        lock,
                        ".foreign-copy-" + operation + "-" + attempt,
                        file.getExt());
            }
            writeForeignBytes(file);
        }

        private static void writeForeignBytes(FileObject file)
                throws IOException {
            try (FileLock lock = file.lock();
                    OutputStream output = file.getOutputStream(lock)) {
                output.write(FOREIGN_RACE_BYTES);
            }
        }

        private static void replaceAtExpectedPath(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension) throws IOException {
            try (FileLock lock = file.lock()) {
                file.delete(lock);
            }
            FileObject replacement = expectedParent.createData(
                    expectedName, expectedExtension);
            writeForeignBytes(replacement);
        }

        private static void requireOwnedIdentity(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                String operation) throws IOException {
            if (!file.isValid()
                    || file.getParent() != expectedParent
                    || !file.getName().equals(expectedName)
                    || !file.getExt().equals(expectedExtension)
                    || expectedParent.getFileObject(
                            expectedName, expectedExtension) != file) {
                throw new IOException(
                        "refusing test " + operation
                        + ": FileObject no longer owns its expected path");
            }
        }

        @Override
        public void runAtomicAction(
                FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            atomicActionAttempts++;
            fileSystem.runAtomicAction(action);
            if (failAtomicAfterDelegate) {
                throw new IOException(
                        "injected atomic-action failure after delegate success");
            }
        }

        int createAttempts() {
            return createAttempts;
        }

        int writeAttempts() {
            return writeAttempts;
        }

        int renameAttempts() {
            return renameAttempts;
        }

        int deleteAttempts() {
            return deleteAttempts;
        }

        int atomicActionAttempts() {
            return atomicActionAttempts;
        }
    }
}
