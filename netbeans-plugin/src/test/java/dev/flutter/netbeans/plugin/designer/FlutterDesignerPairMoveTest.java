package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.template.DesignerFormTemplate;
import dev.flutter.netbeans.designer.template.DesignerFormTemplateFactory;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/** Transaction and recovery contract for moving one mirrored Designer pair. */
class FlutterDesignerPairMoveTest {
    private static final String TRANSACTION_ID = "test-transaction-0001";
    private static final byte[] TARGET_RACE_BYTES =
            "foreign target mutation during source cleanup\n"
                    .getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temporaryDirectory;

    @Test
    void movingFromDartPublishesExactBytesInMirroredFolders()
            throws Exception {
        PairFixture fixture = pair("move_from_dart");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairMove.MoveResult result = transaction(backend)
                .moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder());

        assertCommittedMove(fixture, result, exactDart, exactModel);
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void movingFromModelPublishesExactBytesInMirroredFolders()
            throws Exception {
        PairFixture fixture = pair("move_from_model");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();

        FlutterDesignerPairMove.MoveResult result = transaction(backend)
                .moveResolved(
                        fixture.pair(), fixture.pair().modelFile(),
                        fixture.targetModelFolder());

        assertCommittedMove(fixture, result, exactDart, exactModel);
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void sourceAndEitherTargetCollisionRejectBeforeMutation()
            throws Exception {
        PairFixture sourceCollision = pair("source_collision");
        byte[] exactDart = Files.readAllBytes(sourceCollision.dartPath());
        byte[] exactModel = Files.readAllBytes(sourceCollision.modelPath());
        Files.writeString(
                sourceCollision.dartPath().resolveSibling(
                        ".nb-flutter-move-" + TRANSACTION_ID + ".nbmove"),
                "foreign source collision\n", StandardCharsets.UTF_8);
        refresh(sourceCollision.projectPath());
        FailureBackend sourceBackend = new FailureBackend();

        assertThrows(IOException.class, () -> transaction(sourceBackend)
                .moveResolved(
                        sourceCollision.pair(),
                        sourceCollision.pair().dartFile(),
                        sourceCollision.targetDartFolder()));

        assertExactSource(sourceCollision, exactDart, exactModel);
        assertEquals(0, sourceBackend.lockAttempts());
        assertEquals(0, sourceBackend.createAttempts());

        PairFixture dartCollision = pair("target_dart_collision");
        byte[] occupiedDart = "foreign target Dart\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(dartCollision.targetDartPath(), occupiedDart);
        refresh(dartCollision.projectPath());
        FailureBackend dartBackend = new FailureBackend();

        assertThrows(IOException.class, () -> transaction(dartBackend)
                .moveResolved(
                        dartCollision.pair(),
                        dartCollision.pair().dartFile(),
                        dartCollision.targetDartFolder()));

        assertArrayEquals(occupiedDart,
                Files.readAllBytes(dartCollision.targetDartPath()));
        assertFalse(Files.exists(dartCollision.targetModelPath()));
        assertEquals(0, dartBackend.lockAttempts());
        assertEquals(0, dartBackend.createAttempts());

        PairFixture modelCollision = pair("target_model_collision");
        byte[] occupiedModel = "foreign target model\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(modelCollision.targetModelPath(), occupiedModel);
        refresh(modelCollision.projectPath());
        FailureBackend modelBackend = new FailureBackend();

        assertThrows(IOException.class, () -> transaction(modelBackend)
                .moveResolved(
                        modelCollision.pair(),
                        modelCollision.pair().modelFile(),
                        modelCollision.targetModelFolder()));

        assertArrayEquals(occupiedModel,
                Files.readAllBytes(modelCollision.targetModelPath()));
        assertFalse(Files.exists(modelCollision.targetDartPath()));
        assertEquals(0, modelBackend.lockAttempts());
        assertEquals(0, modelBackend.createAttempts());
    }

    @Test
    void sameFolderAndMissingMirrorRejectBeforeMutation()
            throws Exception {
        PairFixture sameFolder = pair("same_folder");
        FailureBackend sameBackend = new FailureBackend();

        IOException sameFailure = assertThrows(IOException.class,
                () -> transaction(sameBackend).moveResolved(
                        sameFolder.pair(), sameFolder.pair().dartFile(),
                        sameFolder.pair().dartFile().getParent()));

        assertTrue(sameFailure.getMessage().contains("current folder"));
        assertEquals(0, sameBackend.lockAttempts());
        assertEquals(0, sameBackend.createAttempts());

        PairFixture missingMirror = pair("missing_mirror");
        Path unmatched = missingMirror.projectPath()
                .resolve("lib/unmatched");
        Files.createDirectories(unmatched);
        refresh(missingMirror.projectPath());
        FileObject unmatchedFolder = FileUtil.toFileObject(unmatched.toFile());
        assertNotNull(unmatchedFolder);
        FailureBackend missingBackend = new FailureBackend();

        IOException missingFailure = assertThrows(IOException.class,
                () -> transaction(missingBackend).moveResolved(
                        missingMirror.pair(),
                        missingMirror.pair().dartFile(), unmatchedFolder));

        assertTrue(missingFailure.getMessage().contains(
                "mirrored destination folder does not exist"));
        assertEquals(0, missingBackend.lockAttempts());
        assertEquals(0, missingBackend.createAttempts());
    }

    @Test
    void dirtySharedDartOwnerRejectsMoveBeforeMutation()
            throws Exception {
        PairFixture fixture = pair("dirty_source");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FlutterDesignerDataObject owner = FlutterDesignerTestProject
                .dataObject(fixture.pair().dartFile(), fixture.project());
        var document = owner.getEditorSupport().openDocument();
        FailureBackend backend = new FailureBackend();
        FlutterDesignerPairMove transaction = transaction(backend);

        document.insertString(document.getLength(), "// unsaved\n", null);
        try {
            assertTrue(owner.isModified());
            assertFalse(transaction.canMove(fixture.pair()));
            assertThrows(IOException.class, () -> transaction.moveMember(
                    fixture.pair().dartFile(), fixture.targetDartFolder()));
            assertExactSource(fixture, exactDart, exactModel);
            assertNoMoveArtifacts(fixture);
            assertEquals(0, backend.lockAttempts());
            assertEquals(0, backend.createAttempts());
        } finally {
            owner.setModified(false);
            assertTrue(owner.getEditorSupport().close(),
                    "dirty Move fixture must close without prompting");
        }
    }

    @Test
    void readOnlySourceDisablesMoveWhenExposedByFilesystem()
            throws Exception {
        PairFixture fixture = pair("read_only_source");
        FlutterDesignerTestProject.dataObject(
                fixture.pair().dartFile(), fixture.project());

        setReadOnlyOrSkip(fixture.dartPath(), fixture.pair().dartFile());
        try {
            assertFalse(transaction(new FailureBackend())
                    .canMove(fixture.pair()));
            assertNoMoveArtifacts(fixture);
        } finally {
            restoreWritable(fixture.dartPath());
        }
    }

    @Test
    void dependencyGuardRejectsBeforeFilesystemMutation()
            throws Exception {
        PairFixture fixture = pair("dependency_rejection");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = new FailureBackend();
        AtomicBoolean guardCalled = new AtomicBoolean();
        FlutterDesignerPairMove transaction = new FlutterDesignerPairMove(
                backend,
                () -> TRANSACTION_ID,
                (pair, relative, dartBytes) -> {
                    guardCalled.set(true);
                    assertSame(fixture.pair(), pair);
                    assertEquals("destination", relative);
                    assertArrayEquals(exactDart, dartBytes);
                    throw new IOException("injected dependency rejection");
                });

        IOException failure = assertThrows(IOException.class,
                () -> transaction.moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()));

        assertTrue(failure.getMessage().contains(
                "injected dependency rejection"));
        assertTrue(guardCalled.get());
        assertExactSource(fixture, exactDart, exactModel);
        assertNoMoveArtifacts(fixture);
        assertEquals(2, backend.lockAttempts());
        assertEquals(0, backend.sourceRenameAttempts());
        assertEquals(0, backend.createAttempts());
        assertEquals(0, backend.atomicActionAttempts());
    }

    @Test
    void secondTargetCreateFailureRollsBackExactPairAndTombstones()
            throws Exception {
        PairFixture fixture = pair("create_rollback");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FileObject originalDart = fixture.pair().dartFile();
        FileObject originalModel = fixture.pair().modelFile();
        FailureBackend backend = FailureBackend.failCreate(2);

        IOException failure = assertThrows(IOException.class,
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()));

        assertTrue(failure.getMessage().contains(
                "injected create failure at attempt 2"));
        assertExactSource(fixture, exactDart, exactModel);
        assertSame(originalDart, FileUtil.toFileObject(
                fixture.dartPath().toFile()));
        assertSame(originalModel, FileUtil.toFileObject(
                fixture.modelPath().toFile()));
        assertNoMoveArtifacts(fixture);
        assertEquals(0, backend.sourceRenameAttempts(),
                "target creation fails before either source is retired");
        assertEquals(2, backend.createAttempts());
        assertEquals(1, backend.writeAttempts());
        assertEquals(1, backend.targetDeleteAttempts());
        assertEquals(0, backend.sourceDeleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void atomicWrapperFailureAfterDelegateRetainsCommittedMove()
            throws Exception {
        PairFixture fixture = pair("post_delegate_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend.failAtomicAfterDelegate();

        FlutterDesignerPairMove.MoveResult result = transaction(backend)
                .moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder());

        assertCommittedMove(fixture, result, exactDart, exactModel);
        assertSuccessfulBackendCounts(backend);
    }

    @Test
    void cleanupFailureAfterOneDeleteRetainsCommittedPairAndExactTombstone()
            throws Exception {
        PairFixture fixture = pair("source_cleanup_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FileObject originalDart = fixture.pair().dartFile();
        FileObject originalModel = fixture.pair().modelFile();
        FailureBackend backend = FailureBackend.failSourceDelete(2);

        FlutterDesignerPairMove.MoveResult result = transaction(backend)
                .moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder());

        assertTrue(result.committed());
        assertArrayEquals(exactDart, result.dartFile().asBytes());
        assertArrayEquals(exactModel, result.modelFile().asBytes());
        assertFalse(Files.exists(fixture.dartPath()));
        assertFalse(Files.exists(fixture.modelPath()));
        Path retainedDartTombstone = fixture.dartPath().resolveSibling(
                ".nb-flutter-move-" + TRANSACTION_ID + ".nbmove");
        Path deletedModelTombstone = fixture.modelPath().resolveSibling(
                ".nb-flutter-move-" + TRANSACTION_ID + ".nbmove");
        assertTrue(Files.exists(retainedDartTombstone));
        assertArrayEquals(exactDart,
                Files.readAllBytes(retainedDartTombstone));
        assertFalse(Files.exists(deletedModelTombstone));
        assertTrue(originalDart.isValid());
        assertEquals(retainedDartTombstone.getFileName().toString(),
                originalDart.getNameExt());
        assertFalse(originalModel.isValid());
        assertEquals(2, backend.createAttempts());
        assertEquals(2, backend.writeAttempts());
        assertEquals(2, backend.publishAttempts());
        assertEquals(0, backend.targetDeleteAttempts());
        assertEquals(2, backend.sourceDeleteAttempts());
        assertEquals(1, backend.atomicActionAttempts());
    }

    @Test
    void targetPairRemainsLockedUntilSourceCleanupAndFinalVerification()
            throws Exception {
        PairFixture fixture = pair("target_lock_boundary");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend
                .mutateTargetDuringSourceCleanup();

        FlutterDesignerPairMove.MoveResult result = assertDoesNotThrow(
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()),
                "target mutation must be excluded until source retirement is durable");

        assertTrue(backend.targetMutationBlockedByLock(),
                "source cleanup must observe a retained target FileLock");
        assertFalse(backend.targetMutationSucceeded());
        assertCommittedMove(fixture, result, exactDart, exactModel);
    }

    @Test
    void rollbackRetainsExactTargetsWhenADeletedSourceWasRecreated()
            throws Exception {
        PairFixture fixture = pair("recreated_source_retains_target");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FileObject originalModel = fixture.pair().modelFile();
        FailureBackend backend = FailureBackend.deleteSourceDuringRename(1);

        IOException failure = assertThrows(IOException.class,
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()));

        assertTrue(failure.getMessage().contains(
                "injected source disappearance during rename"));
        assertExactSource(fixture, exactDart, exactModel);
        assertTrue(Files.exists(fixture.targetDartPath()),
                "the verified target Dart must remain as recovery authority");
        assertTrue(Files.exists(fixture.targetModelPath()),
                "the verified target model must remain as recovery authority");
        assertArrayEquals(exactDart,
                Files.readAllBytes(fixture.targetDartPath()));
        assertArrayEquals(exactModel,
                Files.readAllBytes(fixture.targetModelPath()));
        assertNotNull(backend.recreatedSourceFile(),
                "rollback must have published a replacement source identity");
        assertNotSame(originalModel, backend.recreatedSourceFile());
        assertFalse(originalModel.isValid());
        assertEquals(0, backend.targetDeleteAttempts(),
                "recreated source identities cannot authorize target deletion");
        assertNoPrivateArtifacts(fixture);
    }

    @Test
    void relocatedDestinationFolderIsRejectedBeforeTargetCreationOrWrite()
            throws Exception {
        PairFixture fixture = pair("destination_identity_race");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend
                .relocateDestinationBeforeAtomic(
                        fixture.targetDartFolder());

        assertThrows(IOException.class,
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()));

        assertTrue(backend.destinationRelocated());
        assertEquals(0, backend.createAttempts(),
                "destination identity must be revalidated before create");
        assertEquals(0, backend.writeAttempts(),
                "no target bytes may be written through a moved folder identity");
        assertExactSource(fixture, exactDart, exactModel);
        assertFalse(Files.exists(fixture.targetModelPath()));
        Path relocated = fixture.targetDartPath().getParent()
                .resolveSibling("destination-relocated");
        assertTrue(Files.isDirectory(relocated));
        try (var files = Files.list(relocated)) {
            assertTrue(files.findAny().isEmpty(),
                    "rejected Move must not write into the relocated folder");
        }
    }

    @Test
    void foreignOwnerOfMirroredModelDestinationRejectsBeforeLocksOrWrites()
            throws Exception {
        PairFixture fixture = pair("foreign_mirrored_owner");
        Path foreignRoot = fixture.targetModelPath().getParent();
        refresh(foreignRoot);
        FileObject foreignDirectory = FileUtil.toFileObject(
                foreignRoot.toFile());
        assertNotNull(foreignDirectory);
        Project foreign = new Project() {
            @Override
            public FileObject getProjectDirectory() {
                return foreignDirectory;
            }

            @Override
            public Lookup getLookup() {
                return Lookup.EMPTY;
            }
        };
        TestOwnerQuery.register(foreignRoot, foreign);
        FailureBackend backend = new FailureBackend();

        IOException failure = assertThrows(IOException.class,
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()));

        assertTrue(failure.getMessage().contains("different project"));
        assertEquals(0, backend.lockAttempts());
        assertEquals(0, backend.createAttempts());
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
    }

    @Test
    void lateVerificationFailureReturnsCommittedRecoveryOutcome()
            throws Exception {
        PairFixture fixture = pair("late_verification_failure");
        byte[] exactDart = Files.readAllBytes(fixture.dartPath());
        byte[] exactModel = Files.readAllBytes(fixture.modelPath());
        FailureBackend backend = FailureBackend
                .failVerificationAfterFirstSourceDelete();

        FlutterDesignerPairMove.MoveResult result = assertDoesNotThrow(
                () -> transaction(backend).moveResolved(
                        fixture.pair(), fixture.pair().dartFile(),
                        fixture.targetDartFolder()),
                "a late verifier fault must not report an already-committed Move as uncommitted");

        assertTrue(result.committed());
        assertArrayEquals(exactDart, result.dartFile().asBytes());
        assertArrayEquals(exactModel, result.modelFile().asBytes());
        assertFalse(Files.exists(fixture.dartPath()));
        assertFalse(Files.exists(fixture.modelPath()));
        Path retainedDartTombstone = fixture.dartPath().resolveSibling(
                ".nb-flutter-move-" + TRANSACTION_ID + ".nbmove");
        assertTrue(Files.exists(retainedDartTombstone),
                "cleanup must stop and retain an exact recovery tombstone after late verification fails");
        assertArrayEquals(exactDart, Files.readAllBytes(retainedDartTombstone));
    }

    private PairFixture pair(String name) throws Exception {
        Path projectPath = temporaryDirectory.resolve(name);
        Path dartPath = projectPath.resolve("lib/screens/home.dart");
        Path modelPath = projectPath.resolve(
                ".fd_templates/screens/home.fd");
        Path targetDartFolderPath = projectPath.resolve(
                "lib/destination");
        Path targetModelFolderPath = projectPath.resolve(
                ".fd_templates/destination");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.createDirectories(targetDartFolderPath);
        Files.createDirectories(targetModelFolderPath);
        Files.writeString(projectPath.resolve("pubspec.yaml"), """
                name: pair_move_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        DesignerFormTemplate template = new DesignerFormTemplateFactory()
                .create("home.dart", "HomePage");
        Files.write(dartPath, template.dartBytes());
        Files.write(modelPath, template.fdBytes());
        refresh(projectPath);
        FlutterProject project = FlutterDesignerTestProject.own(projectPath);
        TestOwnerQuery.register(projectPath, project);
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        FileObject targetDartFolder = FileUtil.toFileObject(
                targetDartFolderPath.toFile());
        FileObject targetModelFolder = FileUtil.toFileObject(
                targetModelFolderPath.toFile());
        assertNotNull(dartFile);
        assertNotNull(targetDartFolder);
        assertNotNull(targetModelFolder);
        assertSame(project, FileOwnerQuery.getOwner(targetDartFolder),
                "Move fixtures require exact FileOwnerQuery ownership");
        FlutterDesignerPairLayout.Pair resolved = FlutterDesignerPairLayout
                .findCompletePair(dartFile, project)
                .orElseThrow();
        return new PairFixture(
                projectPath, dartPath, modelPath,
                targetDartFolderPath.resolve("home.dart"),
                targetModelFolderPath.resolve("home.fd"), project, resolved,
                targetDartFolder, targetModelFolder);
    }

    private static FlutterDesignerPairMove transaction(
            FailureBackend backend) {
        return new FlutterDesignerPairMove(
                backend, () -> TRANSACTION_ID,
                (pair, targetRelativeFolder, exactMovedDartBytes) -> { });
    }

    private static void assertCommittedMove(
            PairFixture fixture,
            FlutterDesignerPairMove.MoveResult result,
            byte[] exactDart,
            byte[] exactModel) throws IOException {
        assertTrue(result.committed());
        assertEquals("home.dart", result.dartFile().getNameExt());
        assertEquals("home.fd", result.modelFile().getNameExt());
        assertEquals(fixture.targetDartFolder(),
                result.dartFile().getParent());
        assertEquals(fixture.targetModelFolder(),
                result.modelFile().getParent());
        assertArrayEquals(exactDart, result.dartFile().asBytes(),
                "Move must preserve every Dart byte");
        assertArrayEquals(exactModel, result.modelFile().asBytes(),
                "Move must preserve the complete .fd JSON byte stream");
        assertFalse(Files.exists(fixture.dartPath()));
        assertFalse(Files.exists(fixture.modelPath()));
        assertNoPrivateArtifacts(fixture);
    }

    private static void assertSuccessfulBackendCounts(
            FailureBackend backend) {
        assertEquals(2, backend.lockAttempts());
        assertEquals(2, backend.sourceRenameAttempts());
        assertEquals(2, backend.createAttempts());
        assertEquals(2, backend.writeAttempts());
        assertEquals(2, backend.publishAttempts());
        assertEquals(2, backend.sourceDeleteAttempts());
        assertEquals(0, backend.targetDeleteAttempts());
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

    private static void assertNoMoveArtifacts(PairFixture fixture)
            throws IOException {
        assertFalse(Files.exists(fixture.targetDartPath()));
        assertFalse(Files.exists(fixture.targetModelPath()));
        assertNoPrivateArtifacts(fixture);
    }

    private static void assertNoPrivateArtifacts(PairFixture fixture)
            throws IOException {
        try (var paths = Files.walk(fixture.projectPath())) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString()
                            .startsWith(".nb-flutter-move-")),
                    "Move must not leave a source tombstone, target staging, or restore file");
        }
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
            Path projectPath,
            Path dartPath,
            Path modelPath,
            Path targetDartPath,
            Path targetModelPath,
            FlutterProject project,
            FlutterDesignerPairLayout.Pair pair,
            FileObject targetDartFolder,
            FileObject targetModelFolder) {
    }

    /** Owner provider for plain Surefire, which does not load project UI. */
    @ServiceProvider(service = FileOwnerQueryImplementation.class, position = 1)
    public static final class TestOwnerQuery
            implements FileOwnerQueryImplementation {
        private static final Map<Path, Project> OWNERS =
                new ConcurrentHashMap<>();

        public TestOwnerQuery() {
        }

        static void register(Path root, Project owner) {
            OWNERS.put(root.toAbsolutePath().normalize(), owner);
        }

        @Override
        public Project getOwner(URI uri) {
            if (uri == null || !"file".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            try {
                return find(Path.of(uri));
            } catch (RuntimeException invalidUri) {
                return null;
            }
        }

        @Override
        public Project getOwner(FileObject file) {
            File local = FileUtil.toFile(file);
            return local == null ? null : find(local.toPath());
        }

        private static Project find(Path candidate) {
            Path normalized = candidate.toAbsolutePath().normalize();
            Path best = null;
            Project owner = null;
            for (Map.Entry<Path, Project> entry : OWNERS.entrySet()) {
                if (normalized.startsWith(entry.getKey())
                        && (best == null
                        || entry.getKey().getNameCount()
                        > best.getNameCount())) {
                    best = entry.getKey();
                    owner = entry.getValue();
                }
            }
            return owner;
        }
    }

    private static final class FailureBackend
            implements FlutterDesignerPairMove.Backend {
        private int failingCreateAttempt = -1;
        private int deleteSourceDuringRenameAttempt = -1;
        private int failingSourceDeleteAttempt = -1;
        private boolean failAtomicAfterDelegate;
        private boolean failVerificationAfterFirstSourceDelete;
        private boolean rejectReads;
        private boolean mutateTargetDuringSourceCleanup;
        private FileObject relocateDestinationBeforeAtomic;
        private boolean targetMutationBlockedByLock;
        private boolean targetMutationSucceeded;
        private boolean destinationRelocated;
        private FileObject publishedTargetDart;
        private FileObject deletedSourceFile;
        private FileObject recreatedSourceFile;
        private int lockAttempts;
        private int createAttempts;
        private int writeAttempts;
        private int publishAttempts;
        private int targetDeleteAttempts;
        private int sourceRenameAttempts;
        private int sourceDeleteAttempts;
        private int atomicActionAttempts;

        static FailureBackend failCreate(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.failingCreateAttempt = attempt;
            return backend;
        }

        static FailureBackend failSourceDelete(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.failingSourceDeleteAttempt = attempt;
            return backend;
        }

        static FailureBackend deleteSourceDuringRename(int attempt) {
            FailureBackend backend = new FailureBackend();
            backend.deleteSourceDuringRenameAttempt = attempt;
            return backend;
        }

        static FailureBackend mutateTargetDuringSourceCleanup() {
            FailureBackend backend = new FailureBackend();
            backend.mutateTargetDuringSourceCleanup = true;
            return backend;
        }

        static FailureBackend relocateDestinationBeforeAtomic(
                FileObject folder) {
            FailureBackend backend = new FailureBackend();
            backend.relocateDestinationBeforeAtomic = folder;
            return backend;
        }

        static FailureBackend failVerificationAfterFirstSourceDelete() {
            FailureBackend backend = new FailureBackend();
            backend.failVerificationAfterFirstSourceDelete = true;
            return backend;
        }

        static FailureBackend failAtomicAfterDelegate() {
            FailureBackend backend = new FailureBackend();
            backend.failAtomicAfterDelegate = true;
            return backend;
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
            lockAttempts++;
            return file.lock();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            if (rejectReads) {
                throw new IOException(
                        "injected late committed-state verification failure");
            }
            return file.asBytes();
        }

        @Override
        public FileObject create(
                FileObject parent,
                String name,
                String extension) throws IOException {
            createAttempts++;
            if (createAttempts == failingCreateAttempt) {
                throw new IOException(
                        "injected create failure at attempt " + createAttempts);
            }
            return parent.createData(name, extension);
        }

        @Override
        public void writeOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] bytes) throws IOException {
            writeAttempts++;
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "write");
                if (file.getSize() != 0L || file.asBytes().length != 0) {
                    throw new IOException(
                            "refusing test write because staging bytes changed");
                }
                try (OutputStream output = file.getOutputStream(lock)) {
                    output.write(bytes);
                }
            }
        }

        @Override
        public FileLock publishOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            publishAttempts++;
            FileLock lock = file.lock();
            try {
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "publish");
                requireExactBytes(file, expectedBytes, "publish");
                if (expectedParent.getFileObject(
                        targetName, targetExtension) != null) {
                    throw new IOException(
                            "refusing test publish because target became occupied");
                }
                file.rename(lock, targetName, targetExtension);
                if (publishAttempts <= 2 && targetExtension.equals("dart")) {
                    publishedTargetDart = file;
                }
                if (deletedSourceFile != null && publishAttempts > 2) {
                    recreatedSourceFile = file;
                }
                return lock;
            } catch (IOException | RuntimeException failure) {
                lock.releaseLock();
                throw failure;
            }
        }

        @Override
        public void deleteOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException {
            targetDeleteAttempts++;
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "target delete");
                requireExactBytes(file, expectedBytes, "target delete");
                file.delete(lock);
            }
        }

        @Override
        public void renameSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            sourceRenameAttempts++;
            requireOwnedIdentity(file, expectedParent, expectedName,
                    expectedExtension, "source rename");
            requireExactBytes(file, expectedBytes, "source rename");
            if (sourceRenameAttempts == deleteSourceDuringRenameAttempt) {
                deletedSourceFile = file;
                file.delete(lock);
                lock.releaseLock();
                throw new IOException(
                        "injected source disappearance during rename at attempt "
                        + sourceRenameAttempts);
            }
            if (expectedParent.getFileObject(
                    targetName, targetExtension) != null) {
                throw new IOException(
                        "refusing test source rename because target became occupied");
            }
            file.rename(lock, targetName, targetExtension);
        }

        @Override
        public void deleteSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException {
            sourceDeleteAttempts++;
            if (mutateTargetDuringSourceCleanup
                    && sourceDeleteAttempts == 1) {
                attemptTargetMutation();
            }
            if (sourceDeleteAttempts == failingSourceDeleteAttempt) {
                throw new IOException(
                        "injected source delete failure at attempt "
                        + sourceDeleteAttempts);
            }
            requireOwnedIdentity(file, expectedParent, expectedName,
                    expectedExtension, "source delete");
            requireExactBytes(file, expectedBytes, "source delete");
            file.delete(lock);
            if (failVerificationAfterFirstSourceDelete
                    && sourceDeleteAttempts == 1) {
                rejectReads = true;
            }
        }

        @Override
        public void runAtomicAction(
            FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            atomicActionAttempts++;
            if (relocateDestinationBeforeAtomic != null) {
                FileObject folder = relocateDestinationBeforeAtomic;
                try (FileLock lock = folder.lock()) {
                    folder.rename(lock, "destination-relocated", "");
                }
                destinationRelocated = true;
                relocateDestinationBeforeAtomic = null;
            }
            fileSystem.runAtomicAction(action);
            if (failAtomicAfterDelegate) {
                throw new IOException(
                        "injected atomic-action failure after delegate success");
            }
        }

        private void attemptTargetMutation() throws IOException {
            if (publishedTargetDart == null) {
                throw new IOException(
                        "test target Dart was not published before source cleanup");
            }
            try (FileLock targetLock = publishedTargetDart.lock();
                    OutputStream output = publishedTargetDart
                            .getOutputStream(targetLock)) {
                output.write(TARGET_RACE_BYTES);
                targetMutationSucceeded = true;
            } catch (FileAlreadyLockedException expected) {
                targetMutationBlockedByLock = true;
            }
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
                throw new IOException("refusing test " + operation
                        + ": FileObject no longer owns its expected path");
            }
        }

        private static void requireExactBytes(
                FileObject file,
                byte[] expected,
                String operation) throws IOException {
            if (!Arrays.equals(expected, file.asBytes())) {
                throw new IOException("refusing test " + operation
                        + ": owned bytes changed");
            }
        }

        boolean targetMutationBlockedByLock() {
            return targetMutationBlockedByLock;
        }

        boolean targetMutationSucceeded() {
            return targetMutationSucceeded;
        }

        boolean destinationRelocated() {
            return destinationRelocated;
        }

        FileObject recreatedSourceFile() {
            return recreatedSourceFile;
        }

        int lockAttempts() { return lockAttempts; }
        int createAttempts() { return createAttempts; }
        int writeAttempts() { return writeAttempts; }
        int publishAttempts() { return publishAttempts; }
        int targetDeleteAttempts() { return targetDeleteAttempts; }
        int sourceRenameAttempts() { return sourceRenameAttempts; }
        int sourceDeleteAttempts() { return sourceDeleteAttempts; }
        int atomicActionAttempts() { return atomicActionAttempts; }
    }
}
