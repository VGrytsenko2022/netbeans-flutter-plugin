package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.io.File;
import java.io.IOException;
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

/** Contract for deleting one mirrored Flutter Designer form as one unit. */
class FlutterDesignerPairDeleteTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void completeWritablePairIsAnAllowedDeleteCandidate() throws Exception {
        RawPairFixture fixture = rawPair(
                "delete_enabled",
                "class Home {}\n".getBytes(StandardCharsets.UTF_8),
                "{}\n".getBytes(StandardCharsets.UTF_8));
        FlutterDesignerPairDelete transaction = transaction(
                new RenameFailureBackend(-1));

        assertTrue(fixture.pair().dartFile().isValid());
        assertTrue(fixture.pair().modelFile().isValid());
        assertTrue(fixture.pair().dartFile().canWrite());
        assertTrue(fixture.pair().modelFile().canWrite());
        FlutterDesignerDataObject sourceOwner = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.pair().project());
        assertFalse(sourceOwner.isModified());
        assertTrue(transaction.canDelete(fixture.pair()));
    }

    @Test
    void deletingFromDartRemovesBothPhysicalFiles() throws Exception {
        RawPairFixture fixture = rawPair(
                "delete_from_dart",
                "class Home {}\n".getBytes(StandardCharsets.UTF_8),
                "{}\n".getBytes(StandardCharsets.UTF_8));
        RenameFailureBackend backend = new RenameFailureBackend(-1);
        FlutterDesignerPairDelete transaction = transaction(backend);

        FlutterDesignerPairDelete.DeleteResult result = transaction.deleteResolved(
                fixture.pair(), fixture.pair().dartFile());

        assertTrue(result.committed());
        assertTrue(result.cleanupFailures().isEmpty());
        assertEquals(2, backend.renameAttempts());
        assertEquals(2, backend.deleteAttempts());
        assertFalse(Files.exists(fixture.dartPath()),
                "deleting the Dart side must remove the Dart source");
        assertFalse(Files.exists(fixture.modelPath()),
                "deleting the Dart side must remove its mirrored .fd model");
    }

    @Test
    void deletingFromModelRemovesBothPhysicalFiles() throws Exception {
        RawPairFixture fixture = rawPair(
                "delete_from_model",
                "class Home {}\n".getBytes(StandardCharsets.UTF_8),
                "{}\n".getBytes(StandardCharsets.UTF_8));
        RenameFailureBackend backend = new RenameFailureBackend(-1);
        FlutterDesignerPairDelete transaction = transaction(backend);

        FlutterDesignerPairDelete.DeleteResult result = transaction.deleteResolved(
                fixture.pair(), fixture.pair().modelFile());

        assertTrue(result.committed());
        assertTrue(result.cleanupFailures().isEmpty());
        assertEquals(2, backend.renameAttempts());
        assertEquals(2, backend.deleteAttempts());
        assertFalse(Files.exists(fixture.dartPath()),
                "deleting the .fd side must remove its mirrored Dart source");
        assertFalse(Files.exists(fixture.modelPath()),
                "deleting the .fd side must remove the .fd model");
    }

    @Test
    void orphanModelFailsClosedAndCannotDeleteUnrelatedSource()
            throws Exception {
        ProjectFixture project = project("orphan_model");
        Path unrelatedDart = project.path().resolve("lib/unrelated.dart");
        Path orphanModel = project.path().resolve(
                ".fd_templates/orphan.fd");
        Files.createDirectories(orphanModel.getParent());
        Files.writeString(unrelatedDart, "void main() {}\n",
                StandardCharsets.UTF_8);
        Files.writeString(orphanModel, "{}\n", StandardCharsets.UTF_8);
        refresh(project.path());
        FileObject modelFile = FileUtil.toFileObject(orphanModel.toFile());
        assertNotNull(modelFile);

        assertTrue(FlutterDesignerPairLayout.findCompletePair(
                modelFile, project.project()).isEmpty());
        assertTrue(Files.exists(unrelatedDart),
                "an orphan .fd model must never infer and delete another Dart file");
    }

    @Test
    void pairWhoseModelRootEscapesThroughLinkFailsClosed() throws Exception {
        ProjectFixture project = project("unsafe_link");
        Path dartPath = project.path().resolve("lib/screens/home.dart");
        Files.createDirectories(dartPath.getParent());
        Files.writeString(dartPath, "class Home {}\n", StandardCharsets.UTF_8);
        Path externalModels = temporaryDirectory.resolve("external_models");
        Files.createDirectories(externalModels.resolve("screens"));
        Path modelPath = externalModels.resolve("screens/home.fd");
        Files.writeString(modelPath, "{}\n", StandardCharsets.UTF_8);
        createDirectoryLinkOrSkip(
                project.path().resolve(".fd_templates"), externalModels);
        refresh(project.path());
        project.project().getProjectDirectory().refresh();
        FileObject modelFile = project.project().getProjectDirectory()
                .getFileObject(".fd_templates/screens/home.fd");
        Assumptions.assumeTrue(modelFile != null,
                "the local filesystem provider does not expose linked files");
        assertFalse(FlutterDesignerPairLayout.findCompletePair(
                modelFile, project.project()).isPresent());
        assertTrue(Files.exists(dartPath));
        assertTrue(Files.exists(modelPath));
    }

    @Test
    void pairWhoseMembersAreHardLinksToSameFileFailsClosed()
            throws Exception {
        ProjectFixture project = project("hard_link_pair");
        Path dartPath = project.path().resolve("lib/screens/home.dart");
        Path modelPath = project.path().resolve(
                ".fd_templates/screens/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.writeString(dartPath, "class Home {}\n", StandardCharsets.UTF_8);
        createHardLinkOrSkip(modelPath, dartPath);
        refresh(project.path());
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dartFile);

        assertTrue(Files.isSameFile(dartPath, modelPath));
        assertTrue(FlutterDesignerPairLayout.findCompletePair(
                dartFile, project.project()).isEmpty());
        assertTrue(Files.exists(dartPath));
        assertTrue(Files.exists(modelPath));
    }

    @Test
    void eitherReadOnlyMemberFailsDeleteCandidateClosed() throws Exception {
        RawPairFixture fixture = rawPair(
                "read_only",
                "class Home {}\n".getBytes(StandardCharsets.UTF_8),
                "{}\n".getBytes(StandardCharsets.UTF_8));
        FlutterDesignerTestProject.dataObject(
                fixture.pair().dartFile(), fixture.pair().project());
        FlutterDesignerPairDelete transaction = transaction(
                new RenameFailureBackend(-1));

        setReadOnlyOrSkip(fixture.modelPath(), fixture.pair().modelFile());
        try {
            assertFalse(transaction.canDelete(fixture.pair()));
        } finally {
            restoreWritable(fixture.modelPath());
        }

        setReadOnlyOrSkip(fixture.dartPath(), fixture.pair().dartFile());
        try {
            assertFalse(transaction.canDelete(fixture.pair()));
        } finally {
            restoreWritable(fixture.dartPath());
        }
    }

    @Test
    void unsavedDartDocumentFailsDeleteCandidateClosed() throws Exception {
        RawPairFixture fixture = rawPair(
                "dirty_source",
                "class Home {}\n".getBytes(StandardCharsets.UTF_8),
                "{}\n".getBytes(StandardCharsets.UTF_8));
        FlutterDesignerDataObject dartObject = FlutterDesignerTestProject
                .dataObject(
                        fixture.pair().dartFile(), fixture.pair().project());
        var document = dartObject.getEditorSupport().openDocument();
        FlutterDesignerPairDelete transaction = transaction(
                new RenameFailureBackend(-1));

        document.insertString(document.getLength(), "// unsaved edit\n", null);
        try {
            assertTrue(dartObject.isModified());
            assertFalse(transaction.canDelete(fixture.pair()));
            assertTrue(Files.exists(fixture.dartPath()));
            assertTrue(Files.exists(fixture.modelPath()));
        } finally {
            dartObject.setModified(false);
            assertTrue(dartObject.getEditorSupport().close(),
                    "the dirty test editor must close without prompting");
        }
        assertFalse(dartObject.isModified(),
                "the dirty fixture must not leak into later filesystem tests");
    }

    @Test
    void secondStageRenameFailureRestoresBothOriginalPathsAndExactBytes()
            throws Exception {
        byte[] exactDart = new byte[]{
            (byte) 0xef, (byte) 0xbb, (byte) 0xbf,
            'c', 'l', 'a', 's', 's', ' ', 'H', 'o', 'm', 'e', ' ', '{', '}',
            '\r', '\n', 0, (byte) 0xff
        };
        byte[] exactModel = new byte[]{
            '{', '\r', '\n', ' ', ' ', '"', 'o', 'p', 'a', 'q', 'u', 'e', '"',
            ':', ' ', '"', (byte) 0xe2, (byte) 0x82, (byte) 0xac, '"',
            '\r', '\n', '}', '\r', '\n', 0
        };
        RawPairFixture fixture = rawPair(
                "second_stage_rollback", exactDart, exactModel);
        RenameFailureBackend backend = new RenameFailureBackend(2);
        FlutterDesignerPairDelete transaction = new FlutterDesignerPairDelete(
                backend, () -> "test-transaction-0001");

        IOException failure = assertThrows(IOException.class,
                () -> transaction.deleteResolved(
                        fixture.pair(), fixture.pair().modelFile()));

        assertTrue(failure.getMessage().contains(
                "injected rename failure at attempt 2"));
        assertEquals(3, backend.renameAttempts(),
                "model stage, failing Dart stage, then model rollback");
        assertEquals(0, backend.deleteAttempts(),
                "cleanup must not begin before both stage renames commit");
        assertEquals(1, backend.atomicActionAttempts());
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()),
                "Dart bytes must remain bit-for-bit identical after rollback");
        assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()),
                ".fd bytes must be restored bit-for-bit after rollback");
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                ".nb-flutter-delete-test-transaction-0001-dart.nbdelete")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                ".nb-flutter-delete-test-transaction-0001-model.nbdelete")));
    }

    @Test
    void secondStageRenameSuccessThenFailureStillRestoresExactPair()
            throws Exception {
        byte[] exactDart = "dart-before\r\n\u0000"
                .getBytes(StandardCharsets.UTF_8);
        byte[] exactModel = "{\r\n  \"fd\": \"before\"\r\n}\r\n\u0000"
                .getBytes(StandardCharsets.UTF_8);
        RawPairFixture fixture = rawPair(
                "second_stage_post_rename_failure", exactDart, exactModel);
        RenameFailureBackend backend = new RenameFailureBackend(2, true);
        FlutterDesignerPairDelete transaction = transaction(backend);

        IOException failure = assertThrows(IOException.class,
                () -> transaction.deleteResolved(
                        fixture.pair(), fixture.pair().dartFile()));

        assertTrue(failure.getMessage().contains(
                "injected rename failure at attempt 2 after physical rename"));
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath()));
        assertArrayEquals(exactModel, Files.readAllBytes(fixture.modelPath()));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                ".nb-flutter-delete-test-transaction-0001-dart.nbdelete")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                ".nb-flutter-delete-test-transaction-0001-model.nbdelete")));
    }

    private RawPairFixture rawPair(
            String name,
            byte[] dartBytes,
            byte[] modelBytes) throws Exception {
        ProjectFixture project = project(name);
        Path dartPath = project.path().resolve("lib/screens/home.dart");
        Path modelPath = project.path().resolve(
                ".fd_templates/screens/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.write(dartPath, dartBytes);
        Files.write(modelPath, modelBytes);
        refresh(project.path());
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dartFile);
        FlutterDesignerPairLayout.Pair resolved = FlutterDesignerPairLayout
                .findCompletePair(dartFile, project.project())
                .orElseThrow();
        return new RawPairFixture(dartPath, modelPath, resolved);
    }

    private ProjectFixture project(String name) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        Files.createDirectories(path.resolve("lib"));
        Files.writeString(path.resolve("pubspec.yaml"), """
                name: pair_delete_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(path);
        return new ProjectFixture(path, project);
    }

    private static FlutterDesignerPairDelete transaction(
            RenameFailureBackend backend) {
        return new FlutterDesignerPairDelete(
                backend, () -> "test-transaction-0001");
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

    private static void createDirectoryLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false,
                    "directory symbolic links are unavailable: " + ex.getMessage());
        }
    }

    private static void createHardLinkOrSkip(Path link, Path target) {
        try {
            Files.createLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false,
                    "hard links are unavailable: " + ex.getMessage());
        }
    }

    private static void refresh(Path path) {
        FileUtil.refreshFor(path.toFile());
    }

    private record ProjectFixture(Path path, FlutterProject project) {
    }

    private record RawPairFixture(
            Path dartPath,
            Path modelPath,
            FlutterDesignerPairLayout.Pair pair) {
    }

    private static final class RenameFailureBackend
            implements FlutterDesignerPairDelete.Backend {
        private final int failingRenameAttempt;
        private final boolean failAfterRename;
        private int renameAttempts;
        private int deleteAttempts;
        private int atomicActionAttempts;

        RenameFailureBackend(int failingRenameAttempt) {
            this(failingRenameAttempt, false);
        }

        RenameFailureBackend(
                int failingRenameAttempt,
                boolean failAfterRename) {
            this.failingRenameAttempt = failingRenameAttempt;
            this.failAfterRename = failAfterRename;
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
        public void rename(
                FileObject file,
                FileLock lock,
                String name,
                String extension) throws IOException {
            renameAttempts++;
            boolean injected = renameAttempts == failingRenameAttempt;
            if (injected && !failAfterRename) {
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
        public void delete(FileObject file, FileLock lock) throws IOException {
            deleteAttempts++;
            file.delete(lock);
        }

        @Override
        public void runAtomicAction(
                FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            atomicActionAttempts++;
            fileSystem.runAtomicAction(action);
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
