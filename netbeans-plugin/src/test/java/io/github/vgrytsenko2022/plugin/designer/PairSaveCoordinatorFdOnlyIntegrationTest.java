package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.command.DesignerCommand;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSession;
import io.github.vgrytsenko2022.designer.command.DesignerCommandSessionResult;
import io.github.vgrytsenko2022.designer.command.DesignerCommandStatus;
import io.github.vgrytsenko2022.designer.command.DesignerRevisionPersistenceKind;
import io.github.vgrytsenko2022.designer.command.MoveWidget;
import io.github.vgrytsenko2022.designer.command.WidgetPlacement;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.dart.DartEditorKit;
import io.github.vgrytsenko2022.plugin.designer.guard.DartGuardedSectionsProvider;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileRole;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileTransactionIssue;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileTransactionIssueCode;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileTransactionRequest;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileTransactionResult;
import io.github.vgrytsenko2022.plugin.designer.persistence.PairFileTransactionStatus;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.CloneableEditorSupport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact integration contract for the internal, non-UI FD_ONLY commit path. */
class PairSaveCoordinatorFdOnlyIntegrationTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId ROOT_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final StableId FIRST_ID = StableId.parse(
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final StableId SECOND_ID = StableId.parse(
            "dddddddd-dddd-4ddd-8ddd-dddddddddddd");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final PropertyName DATA = new PropertyName("data");

    @TempDir
    Path temporaryDirectory;

    @Test
    void commitsExactlyOneFdWriteAndReanchorsWithoutTouchingSourceState()
            throws Exception {
        try (FdOnlyFixture fixture = fixture("fd_only_success")) {
            AtomicInteger dartEvents = new AtomicInteger();
            AtomicInteger fdEvents = new AtomicInteger();
            fixture.dartFile.addFileChangeListener(new FileChangeAdapter() {
                @Override
                public void fileChanged(FileEvent event) {
                    dartEvents.incrementAndGet();
                }
            });
            fixture.designerFile.addFileChangeListener(new FileChangeAdapter() {
                @Override
                public void fileChanged(FileEvent event) {
                    fdEvents.incrementAndGet();
                }
            });
            byte[] exactDart = fixture.lease.durableDartBytes();
            byte[] exactFd = fixture.lease.revision().fdBytes();
            LiveDartDocumentSnapshot liveBefore = fixture.live;
            var sourceUndo = fixture.editor
                    .nativeUndoRedoManagerForCombinedBridge();
            boolean sourceCanUndo = sourceUndo.canUndo();
            boolean sourceCanRedo = sourceUndo.canRedo();
            String sourceUndoName = sourceUndo.getUndoPresentationName();
            String sourceRedoName = sourceUndo.getRedoPresentationName();
            long revisionId = fixture.lease.revision().revisionId();

            fixture.coordinator.commitFdOnly(fixture.current, fixture.lease);

            LiveDartDocumentSnapshot liveAfter = fixture.editor.liveSnapshot();
            assertArrayEquals(exactDart, Files.readAllBytes(fixture.dartPath));
            assertArrayEquals(exactFd, Files.readAllBytes(fixture.designerPath));
            assertEquals(0, dartEvents.get());
            assertEquals(1, fdEvents.get());
            assertTrue(liveBefore.sameEvidence(liveAfter));
            assertSame(liveBefore.documentIdentity(), liveAfter.documentIdentity());
            assertEquals(liveBefore.documentVersion(), liveAfter.documentVersion());
            assertFalse(fixture.editor.sourceModified());
            assertEquals(sourceCanUndo, sourceUndo.canUndo());
            assertEquals(sourceCanRedo, sourceUndo.canRedo());
            assertEquals(sourceUndoName, sourceUndo.getUndoPresentationName());
            assertEquals(sourceRedoName, sourceUndo.getRedoPresentationName());
            assertNull(fixture.dataObject.getCookie(SaveCookie.class));
            assertFalse(fixture.dataObject.isModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator.state().status());
            assertFalse(fixture.orchestrator.dirty());
            assertEquals(revisionId,
                    fixture.orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    fixture.orchestrator.currentRevision().persistenceKind());

            fixture.orchestrator.undo();
            assertTrue(fixture.orchestrator.dirty());
            fixture.orchestrator.redo();
            assertFalse(fixture.orchestrator.dirty());
        }
    }

    @Test
    void staleDartAndStaleFdRejectWithoutOverwritingEitherBaseline()
            throws Exception {
        assertStaleBaselineRejected("stale_dart", true);
        assertStaleBaselineRejected("stale_fd", false);
    }

    @Test
    void failedAndVerifiedRolledBackTransactionsPreserveDirtyRetry()
            throws Exception {
        assertRetryableFailure(
                "failed_before_write",
                new PairFileTransactionResult(
                        PairFileTransactionStatus.FAILED,
                        List.of(issue("Synthetic failure before write")),
                        0,
                        false));
        assertRetryableFailure(
                "verified_rollback",
                new PairFileTransactionResult(
                        PairFileTransactionStatus.ROLLED_BACK,
                        List.of(issue("Synthetic verified rollback")),
                        1,
                        true));
    }

    @Test
    void activeReservationBlocksSourceSaveEditAndDuplicateFdCommit()
            throws Exception {
        try (FdOnlyFixture fixture = fixture("active_reservation")) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<PairFileTransactionRequest> request =
                    new AtomicReference<>();
            PairSaveCoordinator blocking = new PairSaveCoordinator(
                    fixture.coordinator,
                    value -> {
                        request.set(value);
                        entered.countDown();
                        awaitRelease(release);
                        return new PairFileTransactionResult(
                                PairFileTransactionStatus.FAILED,
                                List.of(issue("Synthetic blocked failure")),
                                0,
                                false);
                    });

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<?> save = executor.submit(() -> {
                    try {
                        blocking.commitFdOnly(fixture.current, fixture.lease);
                    } catch (IOException failure) {
                        throw new RuntimeException(failure);
                    }
                });
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                assertEquals(PairSaveCoordinatorStatus.SAVING_FD_ONLY,
                        blocking.state().status());
                assertThrows(IOException.class, blocking::beforeSourceModification);
                assertThrows(IOException.class, blocking::save);
                assertThrows(IOException.class, () -> blocking.commitFdOnly(
                        fixture.current, fixture.lease));
                assertFalse(fixture.orchestrator.canUndo(),
                        "the running transaction must retain the exact lease");
                assertNull(fixture.dataObject.getCookie(SaveCookie.class));

                release.countDown();
                ExecutionException thrown = assertThrows(
                        ExecutionException.class, () -> save.get(10, TimeUnit.SECONDS));
                assertTrue(thrown.getCause() instanceof RuntimeException);
            }

            assertRequestIsExactFdOnly(request.get(), fixture);
            assertTrue(fixture.orchestrator.dirty());
            assertTrue(fixture.orchestrator.canUndo());
            assertEquals(PairSaveCoordinatorStatus.SAVE_FAILED,
                    blocking.state().status());
        }
    }

    @Test
    void externalEventDuringCommittedOutcomeFreezesSessionAndStaysSticky()
            throws Exception {
        try (FdOnlyFixture fixture = fixture("external_event")) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            PairSaveCoordinator blocking = new PairSaveCoordinator(
                    fixture.coordinator,
                    request -> {
                        entered.countDown();
                        awaitRelease(release);
                        return new PairFileTransactionResult(
                                PairFileTransactionStatus.COMMITTED,
                                List.of(),
                                1,
                                false);
                    });

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<?> save = executor.submit(() -> {
                    try {
                        blocking.commitFdOnly(fixture.current, fixture.lease);
                    } catch (IOException failure) {
                        throw new RuntimeException(failure);
                    }
                });
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                assertTrue(blocking.handleFileEvent(
                        new FileEvent(fixture.designerFile)));
                assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        blocking.state().status());
                assertNull(fixture.dataObject.getCookie(SaveCookie.class));
                release.countDown();
                assertThrows(ExecutionException.class,
                        () -> save.get(10, TimeUnit.SECONDS));
            }

            assertFalse(fixture.orchestrator.canUndo());
            assertFalse(fixture.orchestrator.canRedo());
            assertFalse(fixture.dataObject.getCombinedUndoRedo()
                    .designerSessionActive());
            assertTrue(PairSaveCoordinator.isStickyConflict(
                    blocking.state().status()));
            assertNull(fixture.dataObject.getCookie(SaveCookie.class));
        }
    }

    @Test
    void equalButDifferentCurrentIdentityIsRejectedBeforeIoAndRemainsRetryable()
            throws Exception {
        try (FdOnlyFixture fixture = fixture("current_identity")) {
            FlutterDesignerDocumentState.Current copy =
                    new FlutterDesignerDocumentState.Current(
                            fixture.current.decoded(),
                            fixture.current.validation(),
                            fixture.current.catalog(),
                            fixture.current.catalogDiagnostics(),
                            fixture.current.contextIssues(),
                            fixture.current.sourceIntegrity(),
                            fixture.current.threeWayIntegrity());
            assertEquals(fixture.current, copy);
            assertFalse(fixture.current == copy);

            assertThrows(IOException.class,
                    () -> fixture.coordinator.commitFdOnly(copy, fixture.lease));

            assertTrue(fixture.orchestrator.dirty());
            assertTrue(fixture.orchestrator.canUndo());
            assertArrayEquals(fixture.lease.durableFdBytes(),
                    Files.readAllBytes(fixture.designerPath));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator.state().status());
        }
    }

    @Test
    void equalButDifferentCatalogIdentityIsRejectedBeforeIoAndLeaseRemainsAbortable()
            throws Exception {
        try (FdOnlyFixture fixture = fixture("catalog_identity")) {
            WidgetCatalog foreignCatalog = WidgetCatalog.strict(
                    fixture.current.catalog().definitions());
            assertFalse(foreignCatalog == fixture.current.catalog());
            DesignerCommandSession foreignInitial = DesignerCommandSession.open(
                    fixture.current.decoded().original(),
                    fixture.lease.durableDartBytes(),
                    foreignCatalog).session().orElseThrow();
            DesignerCombinedUndoRedo foreignUndo = new DesignerCombinedUndoRedo(
                    org.openide.awt.UndoRedo.NONE);
            AtomicInteger transactionCalls = new AtomicInteger();
            PairSaveCoordinator probing = new PairSaveCoordinator(
                    fixture.coordinator,
                    request -> {
                        transactionCalls.incrementAndGet();
                        throw new AssertionError(
                                "Catalog identity rejection must precede pair I/O");
                    });

            try (DesignerCommandSessionOrchestrator foreignOrchestrator =
                    new DesignerCommandSessionOrchestrator(
                            foreignInitial, foreignUndo)) {
                var moved = applyAndAdopt(foreignOrchestrator, new MoveWidget(
                        FIRST_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 1)));
                assertEquals(DesignerCommandStatus.APPLIED, moved.status());
                try (DesignerCommandSessionOrchestrator.DurableSaveLease foreignLease =
                        foreignOrchestrator.beginDurableSave()) {
                    assertThrows(IOException.class, () -> probing.commitFdOnly(
                            fixture.current, foreignLease));

                    assertEquals(0, transactionCalls.get());
                    assertFalse(foreignLease.ownsExactActiveRevision(),
                            "an identity-invalid preflight lease must be released");
                    assertTrue(foreignOrchestrator.dirty());
                    assertArrayEquals(fixture.lease.durableFdBytes(),
                            Files.readAllBytes(fixture.designerPath));
                    assertArrayEquals(fixture.lease.durableDartBytes(),
                            Files.readAllBytes(fixture.dartPath));
                    assertEquals(PairSaveCoordinatorStatus.CLEAN,
                            probing.state().status());

                    assertTrue(foreignOrchestrator.dirty());
                    assertTrue(foreignOrchestrator.canUndo());
                }
                try (DesignerCommandSessionOrchestrator.DurableSaveLease retry =
                        foreignOrchestrator.beginDurableSave()) {
                    assertTrue(retry.ownsExactActiveRevision());
                    retry.abort();
                    assertFalse(retry.ownsExactActiveRevision());
                    assertTrue(foreignOrchestrator.dirty());
                    assertTrue(foreignOrchestrator.canUndo());
                }
            }
        }
    }

    private void assertStaleBaselineRejected(String name, boolean staleDart)
            throws Exception {
        try (FdOnlyFixture fixture = fixture(name)) {
            byte[] durableDart = fixture.lease.durableDartBytes();
            byte[] durableFd = fixture.lease.durableFdBytes();
            byte[] stale = (staleDart ? "// stale\n" : " \n")
                    .getBytes(StandardCharsets.UTF_8);
            if (staleDart) {
                Files.write(fixture.dartPath, concat(durableDart, stale));
            } else {
                Files.write(fixture.designerPath, concat(durableFd, stale));
            }

            assertThrows(IOException.class, () -> fixture.coordinator
                    .commitFdOnly(fixture.current, fixture.lease));

            assertArrayEquals(
                    staleDart ? concat(durableDart, stale) : durableDart,
                    Files.readAllBytes(fixture.dartPath));
            assertArrayEquals(
                    staleDart ? durableFd : concat(durableFd, stale),
                    Files.readAllBytes(fixture.designerPath));
            assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                    fixture.coordinator.state().status());
            assertFalse(fixture.orchestrator.canUndo());
            assertFalse(fixture.dataObject.getCombinedUndoRedo()
                    .designerSessionActive());
            assertNull(fixture.dataObject.getCookie(SaveCookie.class));
        }
    }

    private void assertRetryableFailure(
            String name, PairFileTransactionResult result) throws Exception {
        try (FdOnlyFixture fixture = fixture(name)) {
            AtomicReference<PairFileTransactionRequest> request =
                    new AtomicReference<>();
            PairSaveCoordinator failing = new PairSaveCoordinator(
                    fixture.coordinator,
                    value -> {
                        request.set(value);
                        return result;
                    });
            byte[] dartBefore = Files.readAllBytes(fixture.dartPath);
            byte[] fdBefore = Files.readAllBytes(fixture.designerPath);
            var candidate = fixture.lease.revision();

            assertThrows(IOException.class,
                    () -> failing.commitFdOnly(fixture.current, fixture.lease));

            assertRequestIsExactFdOnly(request.get(), fixture);
            assertArrayEquals(dartBefore, Files.readAllBytes(fixture.dartPath));
            assertArrayEquals(fdBefore, Files.readAllBytes(fixture.designerPath));
            assertSame(candidate, fixture.orchestrator.currentRevision());
            assertTrue(fixture.orchestrator.dirty());
            assertTrue(fixture.orchestrator.canUndo());
            assertEquals(PairSaveCoordinatorStatus.SAVE_FAILED,
                    failing.state().status());
            assertNull(fixture.dataObject.getCookie(SaveCookie.class));
            assertFalse(fixture.dataObject.isModified());
        }
    }

    private static void assertRequestIsExactFdOnly(
            PairFileTransactionRequest request, FdOnlyFixture fixture) {
        assertNotNull(request);
        assertSame(fixture.dartFile, request.dartFile());
        assertSame(fixture.designerFile, request.designerFile());
        assertArrayEquals(fixture.lease.durableDartBytes(),
                request.expectedDartBytes());
        assertArrayEquals(fixture.lease.durableDartBytes(),
                request.newDartBytes());
        assertArrayEquals(fixture.lease.durableFdBytes(),
                request.expectedDesignerBytes());
        assertArrayEquals(fixture.lease.revision().fdBytes(),
                request.newDesignerBytes());
        assertFalse(Arrays.equals(
                request.expectedDesignerBytes(), request.newDesignerBytes()));
    }

    private FdOnlyFixture fixture(String name) throws Exception {
        Path folder = Files.createDirectory(temporaryDirectory.resolve(name));
        Files.createDirectories(folder.resolve("lib"));
        Files.createDirectories(folder.resolve(".fd_templates"));
        Files.writeString(folder.resolve("pubspec.yaml"), """
                name: fd_only_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(folder);
        Path dartPath = folder.resolve("lib/" + name + ".dart");
        Path designerPath = folder.resolve(".fd_templates/" + name + ".fd");
        DesignerDocument provisional = document(descriptor(
                name + ".dart", "0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = document(descriptor(
                name + ".dart",
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        byte[] dart = source(generated).getBytes(StandardCharsets.UTF_8);
        byte[] fd = new FdDocumentCodec().encode(exact).copyBytes();
        Files.write(dartPath, dart);
        Files.write(designerPath, fd);
        FileUtil.refreshFor(folder.toFile());
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        FileObject designerFile = FileUtil.toFileObject(designerPath.toFile());
        assertNotNull(dartFile);
        assertNotNull(designerFile);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                .dataObject(dartFile, project);
        FlutterDesignerDocumentController controller =
                dataObject.getDocumentController();
        assertTrue(controller.viewOpened());
        FlutterDesignerDocumentState.Current current = awaitCurrent(controller);
        FlutterDesignerEditorSupport editor = dataObject.getEditorSupport();
        StyledDocument document = openGuardedSourceDocument(editor);
        LiveDartDocumentSnapshot live = editor.liveSnapshot();
        assertArrayEquals(dart, live.markerBearingUtf8());
        DesignerCommandSession initial = DesignerCommandSession.open(
                current.decoded().original(),
                dart,
                current.catalog()).session().orElseThrow();
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(
                        initial, dataObject.getCombinedUndoRedo());
        var moved = applyAndAdopt(orchestrator, new MoveWidget(
                FIRST_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 1)));
        assertEquals(DesignerCommandStatus.APPLIED, moved.status());
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                orchestrator.currentRevision().persistenceKind());
        DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                orchestrator.beginDurableSave();
        return new FdOnlyFixture(
                dartPath,
                designerPath,
                dartFile,
                designerFile,
                dataObject,
                controller,
                dataObject.getPairSaveCoordinator(),
                editor,
                document,
                current,
                live,
                orchestrator,
                lease);
    }

    private static DesignerCommandSessionResult applyAndAdopt(
            DesignerCommandSessionOrchestrator orchestrator,
            DesignerCommand command) {
        var attempt = orchestrator.beginCommand(command);
        var lease = attempt.lease().orElseThrow(() -> new AssertionError(
                "Expected an applied command lease: " + attempt.result().status()));
        lease.adoptStaged();
        return attempt.result();
    }

    private static FlutterDesignerDocumentState.Current awaitCurrent(
            FlutterDesignerDocumentController controller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (controller.state()
                    instanceof FlutterDesignerDocumentState.Current current) {
                return current;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Current: "
                + controller.state());
    }

    private static StyledDocument openGuardedSourceDocument(
            FlutterDesignerEditorSupport editor) throws Exception {
        assertNull(editor.getDocument());
        Field kitField = CloneableEditorSupport.class.getDeclaredField("kit");
        kitField.setAccessible(true);
        kitField.set(editor, new DartEditorKit());
        Class<?> bridgeClass = Class.forName(
                FlutterDesignerEditorSupport.class.getName()
                + "$GuardedDocumentBridge");
        Constructor<?> bridgeConstructor = bridgeClass.getDeclaredConstructor();
        bridgeConstructor.setAccessible(true);
        GuardedEditorSupport guardedEditor = (GuardedEditorSupport)
                bridgeConstructor.newInstance();
        DartGuardedSectionsProvider provider =
                new DartGuardedSectionsProvider(guardedEditor);
        Field editorField = FlutterDesignerEditorSupport.class
                .getDeclaredField("guardedEditor");
        editorField.setAccessible(true);
        editorField.set(editor, guardedEditor);
        Field providerField = FlutterDesignerEditorSupport.class
                .getDeclaredField("guardedProvider");
        providerField.setAccessible(true);
        providerField.set(editor, provider);
        return editor.openDocument();
    }

    private static DesignerDocument document(DartSourceDescriptor source) {
        WidgetNode first = new WidgetNode(
                FIRST_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode second = new WidgetNode(
                SECOND_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second))));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static DartSourceDescriptor descriptor(
            String dartFile, String importsHash, String buildHash) {
        return new DartSourceDescriptor(
                dartFile,
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static String source(GeneratedDartRegions generated) {
        return "// <netbeans-flutter-designer region=\"imports\">\n"
                + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
    }

    private static PairFileTransactionIssue issue(String message) {
        return new PairFileTransactionIssue(
                PairFileTransactionIssueCode.ATOMIC_ACTION_FAILED,
                PairFileRole.TRANSACTION,
                message);
    }

    private static void awaitRelease(CountDownLatch release) throws IOException {
        try {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IOException("Timed out awaiting test release");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while awaiting test release", interrupted);
        }
    }

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    private record FdOnlyFixture(
            Path dartPath,
            Path designerPath,
            FileObject dartFile,
            FileObject designerFile,
            FlutterDesignerDataObject dataObject,
            FlutterDesignerDocumentController controller,
            PairSaveCoordinator coordinator,
            FlutterDesignerEditorSupport editor,
            StyledDocument document,
            FlutterDesignerDocumentState.Current current,
            LiveDartDocumentSnapshot live,
            DesignerCommandSessionOrchestrator orchestrator,
            DesignerCommandSessionOrchestrator.DurableSaveLease lease)
            implements AutoCloseable {

        @Override
        public void close() {
            lease.abort();
            orchestrator.close();
            controller.viewClosed();
        }
    }
}
