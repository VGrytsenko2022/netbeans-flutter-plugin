package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
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

/** Contract for renaming one mirrored Flutter Designer form as one unit. */
class FlutterDesignerPairRenameTest {
    private static final String TRANSACTION_ID = "test-transaction-0001";
    private static final String UNOWNED_MODEL_STEM =
            ".foreign-unowned-model";
    private static final byte[] UNOWNED_MODEL_BYTES =
            "foreign bytes must survive rollback\n"
                    .getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temporaryDirectory;

    @Test
    void completeWritableCleanPairIsAnAllowedRenameCandidate()
            throws Exception {
        PairFixture fixture = pair("rename_enabled");
        FlutterDesignerDataObject sourceOwner = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.project());
        FlutterDesignerPairRename transaction = transaction(
                new FailureBackend());

        assertFalse(sourceOwner.isModified());
        assertTrue(transaction.canRename(fixture.pair()));
    }

    @Test
    void renamingFromDartCommitsBothPathsAndOnlyChangesSourceFilename()
            throws Exception {
        PairFixture fixture = pair("rename_from_dart");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        DesignerDocument original = fixture.template().document();
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairRename.RenameResult result = transaction(backend)
                .renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "account_screen");

        assertCommittedRename(
                fixture, result, exactDart, original, "account_screen");
        assertEquals(4, backend.renameAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void renamingFromModelCommitsBothPathsAndPreservesExactDartBytes()
            throws Exception {
        PairFixture fixture = pair("rename_from_model");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        DesignerDocument original = fixture.template().document();
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairRename.RenameResult result = transaction(backend)
                .renameResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        "settings_screen");

        assertCommittedRename(
                fixture, result, exactDart, original, "settings_screen");
        assertEquals(4, backend.renameAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void invalidTargetNamesFailBeforeAnyFilesystemMutation()
            throws Exception {
        PairFixture fixture = pair("invalid_target");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();
        FlutterDesignerPairRename transaction = transaction(backend);

        for (String invalid : new String[]{
            "", "HomePage", "home-page", "home.dart", "../home", " home"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transaction.renameResolved(
                            fixture.pair(),
                            fixture.pair().dartFile(),
                            invalid),
                    "invalid target should fail closed: " + invalid);
        }

        assertUntouched(fixture, exactDart, exactModel, "profile_screen");
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.writeAttempts());
        assertEquals(0, backend.atomicActionAttempts());
    }

    @Test
    void targetCollisionFailsClosedWithoutChangingEitherOriginal()
            throws Exception {
        PairFixture fixture = pair("target_collision");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        Path occupiedModel = fixture.modelPath().resolveSibling(
                "profile_screen.fd");
        byte[] occupiedBytes = "occupied\n".getBytes(StandardCharsets.UTF_8);
        Files.write(occupiedModel, occupiedBytes);
        refresh(occupiedModel);
        FailureBackend backend = new FailureBackend();

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "profile_screen"));

        assertTrue(failure.getMessage().contains("target already exists"));
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
        assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                "profile_screen.dart")));
        assertArrayEquals(occupiedBytes, Files.readAllBytes(occupiedModel));
        assertNoTombstones(fixture);
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.writeAttempts());
        assertEquals(0, backend.atomicActionAttempts());
    }

    @Test
    void modelSourceReferenceMismatchFailsClosed()
            throws Exception {
        PairFixture fixture = pair(
                "source_mismatch",
                new DesignerFormTemplateFactory().create(
                        "different.dart", "HomePage"));
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).renameResolved(
                        fixture.pair(),
                        fixture.pair().modelFile(),
                        "profile_screen"));

        assertTrue(failure.getMessage().contains(
                "SOURCE_REFERENCE_MISMATCH"));
        assertUntouched(fixture, exactDart, exactModel, "profile_screen");
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.writeAttempts());
        assertEquals(0, backend.atomicActionAttempts());
    }

    @Test
    void dirtyDartDocumentDisablesAndRejectsThePairRename()
            throws Exception {
        PairFixture fixture = pair("dirty_source");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FlutterDesignerDataObject dartObject = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.project());
        var document = dartObject.getEditorSupport().openDocument();
        FailureBackend backend = new FailureBackend();
        FlutterDesignerPairRename transaction = transaction(backend);

        document.insertString(document.getLength(), "// unsaved edit\n", null);
        try {
            assertTrue(dartObject.isModified());
            assertFalse(transaction.canRename(fixture.pair()));
            assertUntouched(fixture, exactDart, exactModel, "profile_screen");
        } finally {
            dartObject.setModified(false);
            assertTrue(dartObject.getEditorSupport().close(),
                    "the dirty test editor must close without prompting");
        }
        assertEquals(0, backend.renameAttempts());
        assertEquals(0, backend.writeAttempts());
        assertEquals(0, backend.atomicActionAttempts());
    }

    @Test
    void eitherReadOnlyMemberDisablesThePairRename() throws Exception {
        PairFixture fixture = pair("read_only");
        FlutterDesignerTestProject.dataObject(
                fixture.pair().dartFile(), fixture.project());
        FlutterDesignerPairRename transaction = transaction(
                new FailureBackend());

        setReadOnlyOrSkip(fixture.modelPath(), fixture.pair().modelFile());
        try {
            assertFalse(transaction.canRename(fixture.pair()));
        } finally {
            restoreWritable(fixture.modelPath());
        }

        setReadOnlyOrSkip(fixture.dartPath(), fixture.pair().dartFile());
        try {
            assertFalse(transaction.canRename(fixture.pair()));
        } finally {
            restoreWritable(fixture.dartPath());
        }
    }

    @Test
    void metadataWriteFailureRestoresExactPairAndLeavesNoTombstones()
            throws Exception {
        PairFixture fixture = pair("write_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failWrite(1, false);

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "profile_screen"));

        assertTrue(failure.getMessage().contains(
                "injected write failure at attempt 1"));
        assertUntouched(fixture, exactDart, exactModel, "profile_screen");
        assertNoTombstones(fixture);
        assertEquals(4, backend.renameAttempts(),
                "two stage renames and two original-name restores are expected");
        assertEquals(2, backend.writeAttempts(),
                "the failed metadata write is followed by exact-byte restore");
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void finalRenameSuccessThenFailureStillRollsBackExactPair()
            throws Exception {
        PairFixture fixture = pair("post_rename_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failRename(4, true);

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "profile_screen"));

        assertTrue(failure.getMessage().contains(
                "injected rename failure at attempt 4 after physical rename"));
        assertUntouched(fixture, exactDart, exactModel, "profile_screen");
        assertNoTombstones(fixture);
        assertEquals(8, backend.renameAttempts());
        assertEquals(2, backend.writeAttempts(),
                "the canonical target metadata must be replaced by the exact baseline");
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void atomicActionSuccessThenProviderFailureReturnsCommittedExactPair()
            throws Exception {
        PairFixture fixture = pair("atomic_post_success_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        DesignerDocument original = fixture.template().document();
        FailureBackend backend = FailureBackend.failAtomicAfterDelegate();

        FlutterDesignerPairRename.RenameResult result = transaction(backend)
                .renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "profile_screen");

        assertCommittedRename(
                fixture, result, exactDart, original, "profile_screen");
        assertEquals(4, backend.renameAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void rollbackNeverWritesBaselineToUnknownUnownedModelPath()
            throws Exception {
        PairFixture fixture = pair("unknown_model_rollback");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.abandonModelAtRename(3);
        Path unownedModel = fixture.modelPath().resolveSibling(
                UNOWNED_MODEL_STEM + ".fd");

        IOException failure = assertThrows(
                IOException.class,
                () -> transaction(backend).renameResolved(
                        fixture.pair(),
                        fixture.pair().dartFile(),
                        "profile_screen"));

        assertTrue(failure.getMessage().contains(
                "injected unowned model path at rename attempt 3"));
        assertTrue(failure.getSuppressed().length > 0,
                "an incomplete rollback must report a recovery conflict");
        assertTrue(failure.getSuppressed()[0].getMessage().contains(
                "pair-rename recovery conflict"));
        assertTrue(Files.exists(unownedModel));
        assertArrayEquals(UNOWNED_MODEL_BYTES, Files.readAllBytes(unownedModel),
                "rollback must never write trusted baseline bytes to an unowned path");
        assertFalse(Files.exists(fixture.modelPath()),
                "the test deliberately makes exact model-path recovery impossible");
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                "profile_screen.fd")));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                "profile_screen.dart")));
        assertNoTombstones(fixture);
        assertFalse(java.util.Arrays.equals(
                exactModel, Files.readAllBytes(unownedModel)));
        assertEquals(4, backend.renameAttempts(),
                "the unknown model is not renamed again during recovery");
        assertEquals(1, backend.writeAttempts(),
                "only the prepared metadata write is allowed");
        assertEquals(1, backend.atomicActionAttempts());
    }

    private void assertCommittedRename(
            PairFixture fixture,
            FlutterDesignerPairRename.RenameResult result,
            byte[] exactDart,
            DesignerDocument original,
            String targetStem) throws Exception {
        Path targetDart = fixture.dartPath().resolveSibling(
                targetStem + ".dart");
        Path targetModel = fixture.modelPath().resolveSibling(
                targetStem + ".fd");
        assertTrue(result.committed());
        assertEquals(targetStem + ".dart", result.dartFile().getNameExt());
        assertEquals(targetStem + ".fd", result.modelFile().getNameExt());
        assertFalse(Files.exists(fixture.dartPath()));
        assertFalse(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(targetDart),
                "a file rename must never rewrite user-owned Dart bytes");

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(Files.readAllBytes(targetModel)));
        DesignerDocument renamed = decoded.document();
        assertEquals(targetStem + ".dart", renamed.source().dartFile());
        assertEquals(original.source().className(),
                renamed.source().className());
        assertEquals(original.source().widgetKind(),
                renamed.source().widgetKind());
        assertEquals(original.source().generatorVersion(),
                renamed.source().generatorVersion());
        assertEquals(original.source().managedRegions(),
                renamed.source().managedRegions());
        assertEquals(original.schemaReference(), renamed.schemaReference());
        assertEquals(original.documentId(), renamed.documentId());
        assertEquals(original.canvas(), renamed.canvas());
        assertEquals(original.root(), renamed.root());
        assertEquals(original.extensions(), renamed.extensions());
        assertFalse(decoded.migrated());
        assertNoTombstones(fixture);
    }

    private void assertUntouched(
            PairFixture fixture,
            byte[] exactDart,
            byte[] exactModel,
            String targetStem) throws IOException {
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
        assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                targetStem + ".dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                targetStem + ".fd")));
        assertNoTombstones(fixture);
    }

    private void assertNoTombstones(PairFixture fixture) {
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                ".nb-flutter-rename-" + TRANSACTION_ID + ".dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                ".nb-flutter-rename-" + TRANSACTION_ID + ".fd")));
    }

    private PairFixture pair(String name) throws Exception {
        return pair(name, new DesignerFormTemplateFactory().create(
                "home.dart", "HomePage"));
    }

    private PairFixture pair(
            String name,
            DesignerFormTemplate template) throws Exception {
        Path projectPath = temporaryDirectory.resolve(name);
        Path dartPath = projectPath.resolve("lib/screens/home.dart");
        Path modelPath = projectPath.resolve(
                ".fd_templates/screens/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.writeString(projectPath.resolve("pubspec.yaml"), """
                name: pair_rename_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        Files.write(dartPath, template.dartBytes());
        Files.write(modelPath, template.fdBytes());
        refresh(projectPath);
        FlutterProject project = FlutterDesignerTestProject.own(projectPath);
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dartFile);
        FlutterDesignerPairLayout.Pair resolved = FlutterDesignerPairLayout
                .findCompletePair(dartFile, project)
                .orElseThrow();
        return new PairFixture(
                dartPath, modelPath, project, resolved, template);
    }

    private static FlutterDesignerPairRename transaction(
            FailureBackend backend) {
        return new FlutterDesignerPairRename(
                backend, () -> TRANSACTION_ID);
    }

    private static void setReadOnlyOrSkip(Path path, FileObject fileObject)
            throws IOException {
        File file = path.toFile();
        boolean changed = file.setWritable(false, false);
        refresh(path);
        Assumptions.assumeTrue(changed && !fileObject.canWrite(),
                "the current filesystem does not expose a read-only file to NetBeans");
    }

    private static void restoreWritable(Path path) {
        path.toFile().setWritable(true, false);
        refresh(path);
    }

    private static void refresh(Path path) {
        FileUtil.refreshFor(path.toFile());
    }

    private record PairFixture(
            Path dartPath,
            Path modelPath,
            FlutterProject project,
            FlutterDesignerPairLayout.Pair pair,
            DesignerFormTemplate template) {
    }

    private static final class FailureBackend
            implements FlutterDesignerPairRename.Backend {
        private final int failingRenameAttempt;
        private final boolean failRenameAfterMutation;
        private final int failingWriteAttempt;
        private final boolean failWriteAfterMutation;
        private final boolean failAtomicAfterDelegate;
        private final int abandonModelAtRenameAttempt;
        private int renameAttempts;
        private int writeAttempts;
        private int atomicActionAttempts;

        FailureBackend() {
            this(-1, false, -1, false, false, -1);
        }

        private FailureBackend(
                int failingRenameAttempt,
                boolean failRenameAfterMutation,
                int failingWriteAttempt,
                boolean failWriteAfterMutation,
                boolean failAtomicAfterDelegate,
                int abandonModelAtRenameAttempt) {
            this.failingRenameAttempt = failingRenameAttempt;
            this.failRenameAfterMutation = failRenameAfterMutation;
            this.failingWriteAttempt = failingWriteAttempt;
            this.failWriteAfterMutation = failWriteAfterMutation;
            this.failAtomicAfterDelegate = failAtomicAfterDelegate;
            this.abandonModelAtRenameAttempt = abandonModelAtRenameAttempt;
        }

        static FailureBackend failRename(int attempt, boolean afterMutation) {
            return new FailureBackend(
                    attempt, afterMutation, -1, false, false, -1);
        }

        static FailureBackend failWrite(int attempt, boolean afterMutation) {
            return new FailureBackend(
                    -1, false, attempt, afterMutation, false, -1);
        }

        static FailureBackend failAtomicAfterDelegate() {
            return new FailureBackend(
                    -1, false, -1, false, true, -1);
        }

        static FailureBackend abandonModelAtRename(int attempt) {
            return new FailureBackend(
                    -1, false, -1, false, false, attempt);
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
        public FileLock lock(FileObject file) throws IOException {
            return file.lock();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            return file.asBytes();
        }

        @Override
        public void write(
                FileObject file,
                FileLock lock,
                byte[] bytes) throws IOException {
            writeAttempts++;
            boolean injected = writeAttempts == failingWriteAttempt;
            if (injected && !failWriteAfterMutation) {
                throw new IOException(
                        "injected write failure at attempt " + writeAttempts);
            }
            try (OutputStream output = file.getOutputStream(lock)) {
                output.write(bytes);
            }
            if (injected) {
                throw new IOException(
                        "injected write failure at attempt " + writeAttempts
                        + " after physical write");
            }
        }

        @Override
        public void rename(
                FileObject file,
                FileLock lock,
                String name,
                String extension) throws IOException {
            renameAttempts++;
            if (renameAttempts == abandonModelAtRenameAttempt
                    && extension.equals("fd")) {
                file.rename(lock, UNOWNED_MODEL_STEM, extension);
                try (OutputStream output = file.getOutputStream(lock)) {
                    output.write(UNOWNED_MODEL_BYTES);
                }
                throw new IOException(
                        "injected unowned model path at rename attempt "
                        + renameAttempts);
            }
            boolean injected = renameAttempts == failingRenameAttempt;
            if (injected && !failRenameAfterMutation) {
                throw new IOException(
                        "injected rename failure at attempt " + renameAttempts);
            }
            file.rename(lock, name, extension);
            if (injected) {
                throw new IOException(
                        "injected rename failure at attempt " + renameAttempts
                        + " after physical rename");
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

        int renameAttempts() {
            return renameAttempts;
        }

        int writeAttempts() {
            return writeAttempts;
        }

        int atomicActionAttempts() {
            return atomicActionAttempts;
        }
    }
}
