package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalysisStatus;
import dev.flutter.netbeans.dart.DartCandidateSnapshot;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandLimits;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileRole;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionIssue;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionIssueCode;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionResult;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionStatus;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectSavePreflight;
import dev.flutter.netbeans.plugin.settings.FlutterSettings;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.actions.Savable;
import org.netbeans.editor.BaseDocument;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.CloneableEditorSupport;

/** Focused wiring contract for the one-owner Flutter Designer save lifecycle. */
class PairSaveCoordinatorIntegrationTest {
    private static final StableId ROOT_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final PropertyName DATA = new PropertyName("data");
    private static final String SOURCE_IMPORTS =
            "import 'package:flutter/widgets.dart';\n";
    private static final String SOURCE_BUILD =
            "  @override\n"
            + "  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n"
            + "  }\n";
    private static final String SOURCE =
            "// <netbeans-flutter-designer region=\"imports\">\n"
            + SOURCE_IMPORTS
            + "// </netbeans-flutter-designer>\n\n"
            + "class HomePage extends StatelessWidget {\n"
            + "  // <netbeans-flutter-designer region=\"build\">\n"
            + SOURCE_BUILD
            + "  // </netbeans-flutter-designer>\n"
            + "}\n";

    @TempDir
    Path temporaryDirectory;

    @Test
    void editorCloseRevisionTracksCleanExternalEventsAndSourceAba()
            throws Exception {
        TestPair pair = createPair("editor_close_revision");
        PairSaveCoordinator.CloseRevision initial =
                pair.coordinator().closeRevision();

        assertFalse(pair.coordinator().handleFileEvent(
                new FileEvent(pair.designerFile())),
                "a clean external event is reloadable rather than suppressed");
        PairSaveCoordinator.CloseRevision afterExternalEvent =
                pair.coordinator().closeRevision();
        assertEquals(initial.stateEpoch(), afterExternalEvent.stateEpoch(),
                "a clean event deliberately has no presentation-state transition");
        assertNotEquals(initial.externalEventEpoch(),
                afterExternalEvent.externalEventEpoch());
        assertFalse(initial.sameRevision(afterExternalEvent),
                "the close permit must still observe the clean external event");

        pair.coordinator().sourceBecameModified();
        pair.coordinator().sourceBecameUnmodified();
        PairSaveCoordinator.CloseRevision afterSourceAba =
                pair.coordinator().closeRevision();
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertTrue(afterSourceAba.sourceStateEpoch()
                        >= afterExternalEvent.sourceStateEpoch() + 2,
                "modified then unmodified remains a visible monotonic ABA");
        assertFalse(afterExternalEvent.sameRevision(afterSourceAba));
    }

    @Test
    void realDataObjectUsesOneSaveCookieForCookieAndDirectSourceSaves()
            throws Exception {
        TestPair pair = createPair("source_lifecycle");
        FlutterDesignerEditorSupport editor = pair.dataObject().getEditorSupport();
        StyledDocument document = editor.openDocument();
        assertNull(pair.dataObject().getCookie(SaveCookie.class));

        String firstEdit = "// first source edit\n";
        document.insertString(document.getLength(), firstEdit, null);
        SaveCookie firstCookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(firstCookie);
        assertEquals(1, pair.dataObject().getLookup()
                .lookupAll(SaveCookie.class).size());
        assertSame(firstCookie, pair.dataObject().getLookup()
                .lookup(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                pair.coordinator().state().status());

        // Environment.markModified must not retain a session-long Dart lock.
        try (FileLock ignored = pair.dartFile().lock()) {
            assertTrue(ignored.isValid());
        }

        firstCookie.save();

        assertEquals(SOURCE + firstEdit, Files.readString(
                pair.dartPath(), StandardCharsets.UTF_8));
        assertFalse(editor.sourceModified());
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));

        String secondEdit = "// direct editor save\n";
        document.insertString(document.getLength(), secondEdit, null);
        SaveCookie secondCookie = pair.dataObject().getCookie(SaveCookie.class);
        assertSame(firstCookie, secondCookie,
                "the coordinator must re-publish its one stable SaveCookie");

        editor.saveDocument();

        assertEquals(SOURCE + firstEdit + secondEdit, Files.readString(
                pair.dartPath(), StandardCharsets.UTF_8));
        assertFalse(editor.sourceModified());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
    }

    @Test
    void sourceOnlySavePreservesUtf8BomCrLfAndExactFdBytes() throws Exception {
        String crlfSource = SOURCE.replace("\n", "\r\n");
        byte[] initialDart = withUtf8Bom(crlfSource);
        byte[] exactFd = canonicalFdBytes("bom_crlf.dart");
        TestPair pair = createRawPair("bom_crlf", initialDart, exactFd);
        StyledDocument document = openGuardedSourceDocument(
                pair.dataObject().getEditorSupport());

        document.insertString(document.getLength(), "// source edit\n", null);
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);
        cookie.save();

        assertArrayEquals(withUtf8Bom(crlfSource + "// source edit\r\n"),
                Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(exactFd, Files.readAllBytes(pair.designerPath()));
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
    }

    @Test
    void sourceOnlySaveAcceptsFutureFdAndLeavesItByteIdentical()
            throws Exception {
        byte[] futureFd = ("{\r\n"
                + "  \"format\": \"netbeans-flutter-designer\",\r\n"
                + "  \"schemaVersion\": 999,\r\n"
                + "  \"opaque\": {\"keep\": \"exact\"}\r\n"
                + "}\r\n").getBytes(StandardCharsets.UTF_8);
        TestPair pair = createRawPair(
                "future_fd",
                SOURCE.getBytes(StandardCharsets.UTF_8),
                futureFd);
        StyledDocument document = openGuardedSourceDocument(
                pair.dataObject().getEditorSupport());

        document.insertString(document.getLength(), "// outside guards\n", null);
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);
        cookie.save();

        assertArrayEquals(
                (SOURCE + "// outside guards\n").getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(futureFd, Files.readAllBytes(pair.designerPath()));
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
    }

    @Test
    void sourceOnlySaveAcceptsMalformedFdAndLeavesItByteIdentical()
            throws Exception {
        byte[] malformedFd = ("{ this is deliberately not valid JSON\r\n"
                + "opaque bytes must survive exactly\u0000\r\n")
                .getBytes(StandardCharsets.UTF_8);
        TestPair pair = createRawPair(
                "malformed_fd",
                SOURCE.getBytes(StandardCharsets.UTF_8),
                malformedFd);
        StyledDocument document = openGuardedSourceDocument(
                pair.dataObject().getEditorSupport());

        document.insertString(document.getLength(), "// ordinary edit\n", null);
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);
        cookie.save();

        assertArrayEquals(
                (SOURCE + "// ordinary edit\n").getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(malformedFd, Files.readAllBytes(pair.designerPath()));
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
    }

    @Test
    void removedGuardRejectsSourceSaveAndRetryWithoutLosingLiveEdit()
            throws Exception {
        byte[] baselineDart = SOURCE.getBytes(StandardCharsets.UTF_8);
        byte[] baselineFd = canonicalFdBytes("invalid_guard.dart");
        TestPair pair = createRawPair(
                "invalid_guard", baselineDart, baselineFd);
        FlutterDesignerEditorSupport editor =
                pair.dataObject().getEditorSupport();
        StyledDocument document = openGuardedSourceDocument(editor);
        String userEdit = "// unsaved user edit\n";
        document.insertString(document.getLength(), userEdit, null);

        GuardedSectionManager manager = GuardedSectionManager.getInstance(document);
        assertNotNull(manager);
        SimpleSection build = manager.findSimpleSection(
                LiveDartDocumentSnapshot.BUILD_REGION);
        assertNotNull(build);
        build.removeSection();
        String exactLiveAfterDamage = document.getText(0, document.getLength());
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);

        assertThrows(IOException.class, cookie::save);

        assertSame(document, editor.getDocument(),
                "a rejected guarded save must not reload the live document");
        assertEquals(exactLiveAfterDamage,
                document.getText(0, document.getLength()));
        assertTrue(editor.sourceModified());
        assertEquals(PairSaveCoordinatorStatus.SAVE_FAILED,
                pair.coordinator().state().status());
        assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));
        assertArrayEquals(baselineDart, Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(baselineFd, Files.readAllBytes(pair.designerPath()));

        assertThrows(IOException.class, cookie::save,
                "retry must fail closed instead of silently reloading the edit");

        assertSame(document, editor.getDocument());
        assertEquals(exactLiveAfterDamage,
                document.getText(0, document.getLength()));
        assertTrue(editor.sourceModified());
        assertEquals(PairSaveCoordinatorStatus.SAVE_FAILED,
                pair.coordinator().state().status());
        assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));
        assertArrayEquals(baselineDart, Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(baselineFd, Files.readAllBytes(pair.designerPath()));
    }

    @Test
    void sourceOnlySaveAcceptsDartLargerThanDesignerAnalysisLimit()
            throws Exception {
        String largeSuffix = "// " + "x".repeat(2 * 1024 * 1024 + 256) + "\n";
        String initialSource = SOURCE + largeSuffix;
        byte[] exactFd = canonicalFdBytes("large_source.dart");
        TestPair pair = createRawPair(
                "large_source",
                initialSource.getBytes(StandardCharsets.UTF_8),
                exactFd);
        StyledDocument document = openGuardedSourceDocument(
                pair.dataObject().getEditorSupport());

        document.insertString(document.getLength(), "// saved\n", null);
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);
        cookie.save();

        assertArrayEquals(
                (initialSource + "// saved\n").getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(exactFd, Files.readAllBytes(pair.designerPath()));
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
    }

    @Test
    void virtualSerializationIsExactAndDoesNotConsumeTheSaveCookie()
            throws Exception {
        TestPair pair = createPair("virtual_serialization");
        FlutterDesignerEditorSupport editor = pair.dataObject().getEditorSupport();
        StyledDocument document = editor.openDocument();
        String edit = "// serialized but not persisted\n";
        document.insertString(document.getLength(), edit, null);
        SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);

        String serialized;
        try (InputStream input = editor.getInputStream()) {
            serialized = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(SOURCE + edit, serialized);
        assertEquals(SOURCE, Files.readString(
                pair.dartPath(), StandardCharsets.UTF_8));
        assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));
        assertTrue(editor.sourceModified());
        assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                pair.coordinator().state().status());
    }

    @Test
    void editorEnvironmentAndDesignViewExposeTheSingleOwnerContracts()
            throws Exception {
        TestPair pair = createPair("structural_contracts");
        Class<?> environment = Arrays.stream(
                FlutterDesignerEditorSupport.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("Environment"))
                .findFirst()
                .orElseThrow();
        Method markModified = environment.getDeclaredMethod("markModified");
        Method outputStream = environment.getDeclaredMethod("outputStream");
        Method directSave = FlutterDesignerEditorSupport.class
                .getDeclaredMethod("saveDocument");

        assertSame(environment, markModified.getDeclaringClass());
        assertSame(environment, outputStream.getDeclaringClass());
        assertSame(OutputStream.class, outputStream.getReturnType());
        assertTrue(Modifier.isPublic(markModified.getModifiers()));
        assertTrue(Modifier.isPublic(outputStream.getModifiers()));
        assertSame(FlutterDesignerEditorSupport.class,
                directSave.getDeclaringClass());
        assertTrue(Modifier.isPublic(directSave.getModifiers()));
        assertNotNull(FlutterDesignerEditorSupport.class
                .getDeclaredMethod("saveDocumentThroughNetBeans"));

        FlutterDesignerMultiViewDesign design =
                new FlutterDesignerMultiViewDesign(pair.dataObject().getLookup());
        assertSame(pair.dataObject().getCombinedUndoRedo(),
                design.getUndoRedo(),
                "Design must expose the DataObject's stable combined UndoRedo identity");
        assertSame(pair.dataObject().getCombinedUndoRedo(),
                FlutterDesignerMultiViewSource.resolveUndoRedo(
                        pair.dataObject().getLookup()),
                "Source must resolve the same stable combined UndoRedo identity");
        assertSame(FlutterDesignerMultiViewSource.class,
                FlutterDesignerMultiViewSource.class
                        .getDeclaredMethod("getUndoRedo")
                        .getDeclaringClass(),
                "Source must override the inherited raw editor UndoRedo exposure");
        assertFalse(pair.dataObject().getCombinedUndoRedo().designerSessionActive(),
                "Designer mutation must remain gated while Source history delegates");
        assertSame(pair.coordinator(), pair.dataObject().getPairSaveCoordinator());
    }

    @Test
    void stateListenerMayWaitForCrossThreadPublicationWithoutEffectsDeadlock()
            throws Exception {
        TestPair pair = createPair("effects_callback_deadlock");
        Field effectsField = PairSaveCoordinator.class
                .getDeclaredField("effectsMonitor");
        effectsField.setAccessible(true);
        Object effectsMonitor = effectsField.get(pair.coordinator());
        AtomicBoolean firstCallback = new AtomicBoolean(true);
        CountDownLatch workerFinished = new CountDownLatch(1);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        AtomicReference<Thread> workerReference = new AtomicReference<>();

        java.beans.PropertyChangeListener listener = event -> {
            if (!firstCallback.compareAndSet(true, false)) {
                return;
            }
            assertFalse(Thread.holdsLock(effectsMonitor),
                    "state callbacks must run outside the effects queue lock");
            Thread worker = new Thread(() -> {
                try {
                    pair.coordinator().sourceBecameUnmodified();
                    pair.coordinator().sourceBecameModified();
                } catch (Throwable failure) {
                    workerFailure.set(failure);
                } finally {
                    workerFinished.countDown();
                }
            }, "reentrant-pair-effects-publisher");
            worker.setDaemon(true);
            workerReference.set(worker);
            worker.start();
            try {
                assertTrue(workerFinished.await(2, TimeUnit.SECONDS),
                        "a listener must not deadlock a concurrent publication");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
        };
        pair.coordinator().addPropertyChangeListener(listener);
        try {
            pair.coordinator().sourceBecameModified();
        } finally {
            pair.coordinator().removePropertyChangeListener(listener);
        }

        Thread worker = workerReference.get();
        assertNotNull(worker);
        worker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(worker.isAlive());
        assertNull(workerFailure.get());
        assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                pair.coordinator().state().status());
        assertNotNull(pair.dataObject().getCookie(SaveCookie.class));

        pair.coordinator().sourceBecameUnmodified();
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
    }

    @Test
    void repeatedCachedFatalStateListenerReleasesEffectsDrainForNextDeferral()
            throws Exception {
        TestPair pair = createPair("cached_fatal_effects_drain");
        AssertionError cachedFailure = new AssertionError(
                "synthetic cached pair-state presentation failure");
        AtomicInteger fatalCallbacks = new AtomicInteger();
        java.beans.PropertyChangeListener fatalListener = event -> {
            int invocation = fatalCallbacks.incrementAndGet();
            if (invocation == 1) {
                pair.coordinator().sourceBecameUnmodified();
            }
            throw cachedFailure;
        };
        pair.coordinator().addPropertyChangeListener(fatalListener);

        pair.coordinator().sourceBecameModified();

        assertEquals(2, fatalCallbacks.get(),
                "the reentrant CLEAN publication must drain after the same "
                + "cached Error fails both queued publications");
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        pair.coordinator().removePropertyChangeListener(fatalListener);

        Method deferEffects = PairSaveCoordinator.class
                .getDeclaredMethod("deferEffects");
        deferEffects.setAccessible(true);
        AtomicInteger recoveredCallbacks = new AtomicInteger();
        pair.coordinator().addPropertyChangeListener(
                event -> recoveredCallbacks.incrementAndGet());
        try (AutoCloseable ignored = (AutoCloseable) deferEffects.invoke(
                pair.coordinator())) {
            pair.coordinator().sourceBecameModified();
            assertEquals(0, recoveredCallbacks.get(),
                    "the recovered deferral must retain its queued publication");
        }

        assertEquals(1, recoveredCallbacks.get());
        assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                pair.coordinator().state().status());
        assertNotNull(pair.dataObject().getCookie(SaveCookie.class));

        pair.coordinator().sourceBecameUnmodified();
        assertEquals(2, recoveredCallbacks.get());
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
    }

    @Test
    void externalEventDuringAnActivePairAttemptEntersConflictButCleanEventReloads()
            throws Exception {
        TestPair clean = createPair("clean_event");
        assertFalse(clean.coordinator().handleFileEvent(
                new FileEvent(clean.dartFile())));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                clean.coordinator().state().status());

        TestPair active = createPair("active_event");
        installSyntheticActiveAttempt(active.coordinator());

        assertTrue(active.coordinator().handleFileEvent(
                new FileEvent(active.designerFile())));
        PairSaveCoordinatorSnapshot conflicted = active.coordinator().state();
        assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                conflicted.status());
        assertTrue(conflicted.reason().orElseThrow()
                .contains(active.designerFile().getPath()));
        assertNotNull(active.dataObject().getCookie(SaveCookie.class),
                "a conflict must remain represented by the single save owner");
    }

    @Test
    void acceptedEvidenceCommitsExactDartAndFdThroughTheSingleSaveCookie()
            throws Exception {
        StagedPair staged = stageRealPair("committed_pair");
        SaveCookie cookie = staged.pair().dataObject()
                .getCookie(SaveCookie.class);
        assertNotNull(cookie);
        assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                staged.pair().coordinator().state().status());
        assertArrayEquals(staged.prepared().baselineDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().baselineFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));

        cookie.save();

        assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));
        assertFalse(staged.pair().dataObject().getEditorSupport().sourceModified());
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                staged.pair().coordinator().state().status());
        assertNull(staged.pair().dataObject().getCookie(SaveCookie.class));
    }

    @Test
    void runSavePreflightCommitsExactStagedDartAndFdPair() throws Exception {
        StagedPair staged = stageRealPair("run_save_preflight_pair");
        Path projectRoot = staged.pair().dartPath().getParent().getParent();
        assertTrue(staged.pair().dataObject().isModified());
        assertNotNull(staged.pair().dataObject().getCookie(SaveCookie.class));
        assertNotNull(staged.pair().dataObject().getLookup().lookup(Savable.class));
        assertArrayEquals(staged.prepared().baselineDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().baselineFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));

        FlutterProjectSavePreflight.save(projectRoot);

        assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                staged.pair().coordinator().state().status());
        assertFalse(staged.pair().dataObject().isModified());
        assertNull(staged.pair().dataObject().getCookie(SaveCookie.class));
    }

    @Test
    void confirmedPairCommitJointlyReanchorsCommandBeforePairCallbacks()
            throws Exception {
        StagedPair staged = stageRealPair("joint_commit_reanchor");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            DesignerCommandRevision stagedRevision = orchestrator.currentRevision();
            assertTrue(orchestrator.dirty());
            List<String> callbacks = new ArrayList<>();
            AtomicBoolean commandSawJointCommit = new AtomicBoolean();
            AtomicBoolean pairSawJointCommit = new AtomicBoolean();
            javax.swing.event.ChangeListener commandListener = event -> {
                if (!orchestrator.dirty()) {
                    callbacks.add("command");
                    commandSawJointCommit.set(
                            staged.pair().coordinator().stagedEvidence() == null
                            && staged.pair().coordinator().state().status()
                                == PairSaveCoordinatorStatus.CLEAN);
                }
            };
            java.beans.PropertyChangeListener pairListener = event -> {
                if (event.getNewValue()
                        instanceof PairSaveCoordinatorSnapshot snapshot
                        && snapshot.status()
                            == PairSaveCoordinatorStatus.CLEAN) {
                    callbacks.add("pair");
                    pairSawJointCommit.set(
                            !orchestrator.dirty()
                            && staged.pair().coordinator().stagedEvidence() == null);
                }
            };
            orchestrator.addChangeListener(commandListener);
            staged.pair().coordinator().addPropertyChangeListener(pairListener);
            try {
                staged.pair().dataObject().getCookie(SaveCookie.class).save();
            } finally {
                orchestrator.removeChangeListener(commandListener);
                staged.pair().coordinator().removePropertyChangeListener(pairListener);
            }

            DesignerCommandRevision savedRevision = orchestrator.currentRevision();
            assertFalse(orchestrator.dirty());
            assertNotSame(stagedRevision, savedRevision);
            assertEquals(stagedRevision.revisionId(), savedRevision.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedRevision.persistenceKind());
            assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                    savedRevision.dartCandidateBytes());
            assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                    savedRevision.fdBytes());
            assertEquals(List.of("command", "pair"), callbacks);
            assertTrue(commandSawJointCommit.get());
            assertTrue(pairSawJointCommit.get());
        }
    }

    @Test
    void savedC1RetainsSemanticBToC1EdgeAndMovesCesSavepointCleanliness()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_b_c1");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();

            assertTrue(combined.canUndo(),
                    "successful Pair Save must retain the exact native B-to-C1 semantic edge");
            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision oldB = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), oldB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    oldB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertFormerDurableBIsStaged(history, oldB);
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertTrue(editor.sourceModified());
            assertNotNull(pair.dataObject().getCookie(SaveCookie.class));
            assertSavedC1RemainsDurable(history);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision savedC1 = orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(), savedC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedC1.persistenceKind());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(history.savedCurrent(),
                    pair.dataObject().getDocumentController().state());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertFalse(editor.sourceModified(),
                    "redoing exact saved C1 must return CES to its savepoint");
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canUndo());
            assertFalse(combined.canRedo());
            assertSavedC1RemainsDurable(history);
        }
    }

    @Test
    void savedC1UndoToFormerDurablePairCanBeSavedAgain()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_undo_resave");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertFormerDurableBIsStaged(history, historicalB);
            var stagedPair = pair.coordinator().stagedProofSnapshot()
                    .preparedPairIdentity();
            assertSame(historicalB.generation(),
                    stagedPair.dartTransition().generation());
            SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
            assertNotNull(cookie);

            cookie.save();

            DesignerCommandRevision savedB = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), savedB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedB.persistenceKind());
            assertFalse(orchestrator.dirty());
            assertFalse(pair.dataObject().getEditorSupport().sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(history.oldDart(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.oldFd(),
                    Files.readAllBytes(pair.designerPath()));

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC1 = orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(), redoneC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    redoneC1.persistenceKind());
            assertTrue(orchestrator.dirty(),
                    "Redo above the newly saved B endpoint must be dirty");
            assertTrue(pair.dataObject().getEditorSupport().sourceModified());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertNotNull(pair.coordinator().stagedProofSnapshot(),
                    "Redo C1 must rebind its retained semantic edge against saved B");
            assertSame(cookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "Redo C1 must republish the stable pair SaveCookie");
            assertArrayEquals(history.savedDart(),
                    pair.dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
            assertArrayEquals(history.oldDart(),
                    Files.readAllBytes(pair.dartPath()),
                    "Redo C1 must not rewrite the newly saved B Dart bytes");
            assertArrayEquals(history.oldFd(),
                    Files.readAllBytes(pair.designerPath()),
                    "Redo C1 must not rewrite the newly saved B .fd bytes");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision returnedB = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), returnedB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    returnedB.persistenceKind());
            assertFalse(orchestrator.dirty());
            assertFalse(pair.dataObject().getEditorSupport().sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(history.oldDart(),
                    pair.dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
            assertArrayEquals(history.oldDart(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.oldFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void firstSaveAfterV1MigrationReanchorsCanonicalV4PropertyHistory()
            throws Exception {
        SetProperty foreground = new SetProperty(
                ROOT_ID,
                new PropertyName("styleColor"),
                new PropertyValue.ThemeTokenValue(new ThemeToken(
                        "material.colorScheme.primaryFixed")));
        StagedPair c1 = stageLegacyV1RealPair(
                "saved_history_v1_to_v4_property_migration",
                foreground);
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            assertTrue(new String(
                    c1.prepared().baselineFdBytes(), StandardCharsets.UTF_8)
                    .contains("\"schemaVersion\": 1"));
            assertTrue(new String(
                    c1.prepared().prospectiveFdBytes(), StandardCharsets.UTF_8)
                    .contains("\"schemaVersion\": 4"));

            SetProperty background = new SetProperty(
                    ROOT_ID,
                    new PropertyName("styleBackgroundColor"),
                    new PropertyValue.ThemeTokenValue(new ThemeToken(
                            "material.colorScheme.onPrimary")));
            PairSaveEvidence c2 = replaceOnce(
                    c1, orchestrator, c1.evidence(), background);
            SaveCookie stableCookie = c1.pair().dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);
            byte[] canonicalC0Fd = new FdDocumentCodec()
                    .encode(c1.current().decoded().document()).copyBytes();
            assertTrue(new String(
                    canonicalC0Fd, StandardCharsets.UTF_8)
                    .contains("\"schemaVersion\": 4"));
            assertFalse(Arrays.equals(
                    c1.prepared().baselineFdBytes(), canonicalC0Fd));

            stableCookie.save();

            FlutterDesignerDocumentState.Current savedC2Current =
                    awaitCurrentWithPair(
                    c1.pair().dataObject().getDocumentController(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    c2.candidateDartBytes());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertFalse(orchestrator.dirty());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    c1.pair().coordinator().state().status());
            assertEquals(2,
                    c1.pair().coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(c1.pair(), c2);

            replayPairHistory(
                    c1.pair(), DesignerSemanticUndoableEdit.Direction.UNDO);
            DesignerCommandRevision historicalC1 =
                    orchestrator.currentRevision();
            assertSavedHistoryProof(
                    c1.pair(),
                    savedC2Current,
                    historicalC1,
                    PairSaveCoordinator.StagedPairProofKind
                            .REANCHORED_ANALYZED,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    c1.evidence().candidateDartBytes(),
                    c1.evidence().preparedPairIdentity()
                            .prospectiveFdBytes());

            replayPairHistory(
                    c1.pair(), DesignerSemanticUndoableEdit.Direction.UNDO);
            DesignerCommandRevision historicalC0 =
                    orchestrator.currentRevision();
            assertSavedHistoryProof(
                    c1.pair(),
                    savedC2Current,
                    historicalC0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    c1.prepared().baselineDartBytes(),
                    canonicalC0Fd);
            assertFalse(Arrays.equals(
                    c1.prepared().baselineFdBytes(), historicalC0.fdBytes()),
                    "post-migration history must not resurrect raw schema-v1 bytes");
            assertSavedPairRemainsDurable(c1.pair(), c2);
        }
    }

    @Test
    void savedC2RetainsBothSemanticEdgesAcrossFullUndoRedoChain()
            throws Exception {
        StagedPair staged = stageRealPair("saved_history_b_c1_c2");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            TestPair pair = staged.pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            long oldBRevisionId = adjacentUndoTargetId(orchestrator);
            long c1RevisionId = orchestrator.currentRevision().revisionId();
            PairSaveEvidence c1 = staged.evidence();
            PairSaveEvidence c2 = replaceOnce(
                    staged, orchestrator, c1, "C2");
            long c2RevisionId = orchestrator.currentRevision().revisionId();
            SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
            assertNotNull(cookie);

            cookie.save();

            FlutterDesignerDocumentState.Current savedCurrent =
                    awaitCurrentWithFd(
                            pair.dataObject().getDocumentController(),
                            c2.preparedPairIdentity().prospectiveFdBytes());
            assertEquals(c2RevisionId,
                    orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertSavedPairRemainsDurable(pair, c2);
            assertTrue(combined.canUndo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalC1 =
                    orchestrator.currentRevision();
            assertEquals(c1RevisionId, historicalC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalC1.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertArrayEquals(c1.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    savedCurrent,
                    historicalC1,
                    PairSaveCoordinator.StagedPairProofKind
                            .REANCHORED_ANALYZED,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    c1.candidateDartBytes(),
                    c1.preparedPairIdentity().prospectiveFdBytes());
            assertSavedPairRemainsDurable(pair, c2);
            assertTrue(combined.canUndo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(oldBRevisionId, historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    savedCurrent,
                    historicalB,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    staged.prepared().baselineDartBytes(),
                    staged.prepared().baselineFdBytes());
            assertSavedPairRemainsDurable(pair, c2);
            assertFalse(combined.canUndo());
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC1 = orchestrator.currentRevision();
            assertEquals(c1RevisionId, redoneC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    redoneC1.persistenceKind());
            assertArrayEquals(c1.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    savedCurrent,
                    redoneC1,
                    PairSaveCoordinator.StagedPairProofKind
                            .REANCHORED_ANALYZED,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    c1.candidateDartBytes(),
                    c1.preparedPairIdentity().prospectiveFdBytes());
            assertSavedPairRemainsDurable(pair, c2);
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC2 = orchestrator.currentRevision();
            assertEquals(c2RevisionId, redoneC2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    redoneC2.persistenceKind());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertArrayEquals(c2.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedCurrent,
                    pair.dataObject().getDocumentController().state());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canUndo());
            assertFalse(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);
        }
    }

    @Test
    void ordinarySourceEditRemainsChronologicallyAboveSavedPairSemanticEdge()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_above_pair");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            String sourceEdit = "// ordinary Source S2 after saved C1\n";
            document.insertString(document.getLength(), sourceEdit, null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            assertFalse(Arrays.equals(history.savedDart(), sourceS2));
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertNotNull(pair.dataObject().getCookie(SaveCookie.class));

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the first Undo must remove only ordinary Source S2");
            assertEquals(history.savedRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertFalse(editor.sourceModified(),
                    "C1 is the exact CES savepoint below S2");
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canUndo(),
                    "the older B-to-C1 semantic edge must follow Source S2");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(history.oldRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertTrue(orchestrator.dirty());
            assertFormerDurableBIsStaged(
                    history, orchestrator.currentRevision());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertTrue(editor.sourceModified());
            assertNotNull(pair.dataObject().getCookie(SaveCookie.class));
            assertSavedC1RemainsDurable(history);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(history.savedRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertFalse(orchestrator.dirty());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertFalse(editor.sourceModified(),
                    "semantic Redo must return exactly to saved C1");
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "ordinary Source S2 must remain the next chronological Redo");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(history.savedRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertFalse(orchestrator.dirty());
            assertNull(pair.coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertTrue(editor.sourceModified());
            assertNotNull(pair.dataObject().getCookie(SaveCookie.class));
            assertFalse(combined.canRedo());
            assertSavedC1RemainsDurable(history);
        }
    }

    @Test
    void sourceSaveReanchorsSavedSemanticEdgeWithoutBreakingNativeChronology()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_save_overlay");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            String sourceEdit = "// saved unmanaged Source S2 above C1\n";
            document.insertString(document.getLength(), sourceEdit, null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo());

            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            assertNotSame(history.savedCurrent(), sourceCurrent,
                    "Source Save must adopt evidence for the new exact durable Dart anchor");
            DesignerCommandRevision savedSourceRevision =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    savedSourceRevision.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceRevision.persistenceKind());
            assertArrayEquals(sourceS2,
                    savedSourceRevision.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "Source Save must retain the exact semantic B-to-C1 edge");
            String sourceUndoPresentation = combined.getUndoPresentationName();
            assertNotEquals("Undo Set Flutter Property",
                    sourceUndoPresentation,
                    "Source Save must leave ordinary Source S2 above the semantic edge");
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
            assertTrue(combined.canUndo());

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the first Undo must remove only the saved Source overlay");
            assertSame(savedSourceRevision, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty(),
                    "ordinary Source history must not move the Designer cursor");
            assertTrue(editor.sourceModified(),
                    "C1 differs from the newer S2 CES savepoint");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "Undoing saved S2 must republish the one stable SaveCookie");
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo());
            String sourceRedoPresentation = combined.getRedoPresentationName();
            assertNotEquals("Redo Set Flutter Property",
                    sourceRedoPresentation,
                    "the native redo suffix above C1 must still be ordinary Source S2");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    sourceCurrent,
                    historicalB,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS2,
                    history.savedFd(),
                    history.oldDart(),
                    history.oldFd());
            PairSaveCoordinator.StagedPairProofSnapshot historicalProof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(historicalProof);
            assertArrayEquals(history.savedDart(),
                    historicalProof.preparedPairIdentity().liveDartBytes(),
                    "historical B must be derived from native C1 below the S2 overlay");
            assertArrayEquals(sourceS2,
                    historicalProof.preparedPairIdentity().baselineDartBytes(),
                    "historical B must retain durable S2 as its transaction baseline");
            assertArrayEquals(history.oldDart(),
                    historicalProof.preparedPairIdentity().prospectiveDartBytes(),
                    "historical B must not inherit unmanaged S2 bytes");
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision restoredC1 =
                    orchestrator.currentRevision();
            assertSame(savedSourceRevision, restoredC1);
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    restoredC1.persistenceKind());
            assertArrayEquals(sourceS2, restoredC1.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(sourceCurrent, controller.state());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertTrue(editor.sourceModified(),
                    "semantic Redo reaches C1 below the saved S2 Source entry");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "C1 below the S2 savepoint must retain the stable SaveCookie");
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canRedo());
            assertEquals(sourceRedoPresentation,
                    combined.getRedoPresentationName(),
                    "ordinary Source S2 must be the next Redo after semantic C1");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedSourceRevision, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertFalse(combined.canRedo());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertEquals(sourceUndoPresentation,
                    combined.getUndoPresentationName(),
                    "returning to the S2 savepoint must restore Source S2 as the top Undo");
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void sourceSaveAfterSavedDuplicateBaselineRetainsBothSemanticEdgesAndNativeChronology()
            throws Exception {
        StagedPair staged = stageRealPair(
                "saved_duplicate_baseline_source_overlay");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            TestPair pair = staged.pair();
            PairSaveCoordinator coordinator = pair.coordinator();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            long c0RevisionId = adjacentUndoTargetId(orchestrator);
            DesignerCommandRevision c1 = orchestrator.currentRevision();
            long c1RevisionId = c1.revisionId();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);

            PairSaveCoordinator.StagedCommandSource c1Source =
                    coordinator.captureStagedCommandSource();
            var returnAttempt = c1Source.beginCommand(
                    setDataProperty("before"));
            var pendingC2 = returnAttempt.lease().orElseThrow();
            DesignerCommandRevision dirtyC2 = pendingC2.candidateRevision();
            long c2RevisionId = dirtyC2.revisionId();
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    dirtyC2.persistenceKind());
            assertNotEquals(c0RevisionId, c2RevisionId,
                    "returning to equal durable bytes must allocate a distinct semantic revision");
            assertEquals(staged.current().decoded().document(),
                    dirtyC2.document());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    dirtyC2.dartCandidateBytes());
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    dirtyC2.fdBytes());

            try (pendingC2;
                    PairSaveCoordinator.PairReplacement replacement =
                            coordinator.beginStagedReplacement(
                                    c1Source, pendingC2)) {
                replacement.replaceWithExactBaseline();
            }

            assertSame(dirtyC2, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty(),
                    "the duplicate BASELINE revision is semantic-dirty until Save");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    coordinator.state().status());
            assertNull(coordinator.stagedEvidence());
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount(),
                    "C0-to-C1 and C1-to-C2 must both be retained before Save");
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            stableCookie.save();

            DesignerCommandRevision savedC2 = orchestrator.currentRevision();
            assertNotSame(dirtyC2, savedC2,
                    "semantic BASELINE Save must re-anchor every retained revision identity");
            assertEquals(c2RevisionId, savedC2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedC2.persistenceKind());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    coordinator.state().status());
            assertNull(coordinator.stagedEvidence());
            assertNull(coordinator.stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount(),
                    "saving duplicate C2 must not collapse either stable-id semantic edge");
            FlutterDesignerDocumentState controllerState = controller.state();
            assertTrue(controllerState
                    instanceof FlutterDesignerDocumentState.Current);
            FlutterDesignerDocumentState.Current savedC2Current =
                    (FlutterDesignerDocumentState.Current) controllerState;
            assertNotSame(staged.current(), savedC2Current);

            String sourceEdit =
                    "// saved unmanaged Source S3 above duplicate C2\n";
            document.insertString(document.getLength(), sourceEdit, null);
            byte[] sourceS3 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertSame(stableCookie, sourceCookie,
                    "pair and Source saves must share the one stable SaveCookie");

            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller,
                            staged.prepared().baselineFdBytes(),
                            sourceS3);
            assertNotSame(savedC2Current, sourceCurrent,
                    "Source Save must adopt the exact newer Dart anchor");
            DesignerCommandRevision savedSourceC2 =
                    orchestrator.currentRevision();
            assertEquals(c2RevisionId, savedSourceC2.revisionId(),
                    "Source Save must preserve duplicate C2's stable revision id");
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceC2.persistenceKind());
            assertArrayEquals(sourceS3,
                    savedSourceC2.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    coordinator.state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount(),
                    "Source re-anchor must retain both duplicate-baseline semantic edges");
            String sourceUndoPresentation =
                    combined.getUndoPresentationName();
            assertNotEquals("Undo Set Flutter Property",
                    sourceUndoPresentation,
                    "the saved unmanaged Source edit must precede semantic Undo");
            assertArrayEquals(sourceS3, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the first Undo must remove only the saved unmanaged Source edit");
            assertSame(savedSourceC2, orchestrator.currentRevision(),
                    "native Source Undo must not move the semantic C2 cursor");
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    coordinator.state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalC1 =
                    orchestrator.currentRevision();
            assertEquals(c1RevisionId, historicalC1.revisionId(),
                    "the second Undo must traverse C2-to-C1, not skip a duplicate baseline edge");
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalC1.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(staged.evidence().candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    coordinator.state().status());
            assertNotNull(coordinator.stagedProofSnapshot());
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS3, Files.readAllBytes(pair.dartPath()),
                    "semantic Undo must not rewrite the durable Source anchor");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalC0 =
                    orchestrator.currentRevision();
            assertEquals(c0RevisionId, historicalC0.revisionId(),
                    "the third Undo must retain and reach the original C0 stable id");
            assertNotEquals(c2RevisionId, historicalC0.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    historicalC0.persistenceKind(),
                    "equal C0 and C2 documents must remain distinct re-anchored BASELINE revisions");
            assertEquals(savedSourceC2.document(), historicalC0.document());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    historicalC0.fdBytes());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    coordinator.state().status());
            assertNull(coordinator.stagedProofSnapshot(),
                    "a historical BASELINE below the newer Source savepoint is not a staged pair");
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount(),
                    "both semantic edges must remain replayable after Source Save");
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(sourceS3, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void saveFromHistoricalBaselineOverlayCommitsExactOldBytesAndRetainsGraph()
            throws Exception {
        HistoricalBaselineOverlayFixture fixture =
                historicalBaselineOverlayFixture(
                        "historical_baseline_overlay_save");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            fixture.stableCookie().save();

            FlutterDesignerDocumentState.Current savedCurrent =
                    awaitCurrentWithPair(
                            fixture.controller(),
                            fixture.baselineFd(),
                            fixture.historicalDart());
            DesignerCommandRevision savedC0 = orchestrator.currentRevision();
            assertNotSame(fixture.historicalC0(), savedC0,
                    "saving the historical overlay must install the durable re-anchored revision graph");
            assertEquals(fixture.c0RevisionId(), savedC0.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedC0.persistenceKind());
            assertArrayEquals(fixture.historicalDart(),
                    savedC0.dartCandidateBytes());
            assertArrayEquals(fixture.baselineFd(), savedC0.fdBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(fixture.editor().sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.pair().coordinator().state().status());
            assertNull(fixture.pair().coordinator().stagedProofSnapshot());
            assertNull(fixture.pair().dataObject().getCookie(SaveCookie.class));
            assertEquals(2,
                    fixture.pair().coordinator().unsavedPairHistoryEdgeCount(),
                    "saving historical C0 must retain both stable semantic edges");
            assertSame(savedCurrent, fixture.controller().state());
            assertArrayEquals(fixture.historicalDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.historicalDart(),
                    Files.readAllBytes(fixture.pair().dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.pair().designerPath()));

            replayPairHistory(
                    fixture.pair(),
                    DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC1 = orchestrator.currentRevision();
            assertEquals(fixture.c1RevisionId(), redoneC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    redoneC1.persistenceKind());
            assertNotNull(fixture.pair().coordinator().stagedProofSnapshot(),
                    "the first retained edge must rebind C1 against saved C0");
            assertEquals(2,
                    fixture.pair().coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(fixture.historicalDart(),
                    Files.readAllBytes(fixture.pair().dartPath()),
                    "semantic Redo must not rewrite the newly saved C0 pair");

            replayPairHistory(
                    fixture.pair(),
                    DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC2 = orchestrator.currentRevision();
            assertEquals(fixture.c2RevisionId(), redoneC2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    redoneC2.persistenceKind());
            assertTrue(orchestrator.dirty(),
                    "C2 is a distinct duplicate-baseline revision above saved C0");
            assertArrayEquals(fixture.historicalDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertNull(fixture.pair().coordinator().stagedProofSnapshot());
            assertEquals(2,
                    fixture.pair().coordinator().unsavedPairHistoryEdgeCount(),
                    "both re-anchored edges must remain replayable after Save");
            assertArrayEquals(fixture.historicalDart(),
                    Files.readAllBytes(fixture.pair().dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.pair().designerPath()));
        }
    }

    @Test
    void historicalBaselineOverlayOwnsPropertiesAuthorityButArbitrarySourceDoesNot()
            throws Exception {
        HistoricalBaselineOverlayFixture fixture =
                historicalBaselineOverlayFixture(
                        "historical_baseline_overlay_properties_authority");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            PairSaveCoordinator coordinator = fixture.pair().coordinator();
            DesignerCommandRevision historicalC0 =
                    orchestrator.currentRevision();
            long exactEpoch = coordinator.state().epoch();

            assertSame(fixture.historicalC0(), historicalC0);
            assertTrue(coordinator.ownsExactSemanticBaselineRevision(
                    exactEpoch, orchestrator, historicalC0),
                    "the exact historical Source-overlay BASELINE is Designer-owned Properties authority");
            assertFalse(coordinator.ownsExactSemanticBaselineRevision(
                    exactEpoch + 1, orchestrator, historicalC0),
                    "Properties authority must remain epoch-bound");

            StyledDocument document = fixture.editor().getDocument();
            assertNotNull(document);
            document.insertString(document.getLength(),
                    "// arbitrary dirty Source above historical C0\n", null);
            LiveDartDocumentSnapshot arbitraryLive =
                    fixture.editor().liveSnapshot();

            assertFalse(Arrays.equals(
                    fixture.historicalDart(),
                    arbitraryLive.markerBearingUtf8()));
            assertTrue(fixture.editor().sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    coordinator.state().status());
            assertFalse(coordinator.ownsExactSemanticBaselineRevision(
                    coordinator.state().epoch(), orchestrator, historicalC0),
                    "an arbitrary dirty Source edit must not inherit the historical Designer authority");
            assertSame(fixture.stableCookie(),
                    fixture.pair().dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(fixture.durableSourceDart(),
                    Files.readAllBytes(fixture.pair().dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.pair().designerPath()));
        }
    }

    @Test
    void sourceSaveAfterFinalSemanticEdgeIsTrimmedReanchorsLiveCommandOwner()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_save_after_final_edge_trim");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            DesignerCommandRevision savedC1 =
                    orchestrator.currentRevision();
            var nativeHistory =
                    editor.nativeUndoRedoManagerForCombinedBridge();
            int originalLimit = nativeHistory.getLimit();
            try {
                onEdt(() -> {
                    // UndoManager trims by invoking edit.die(); this exercises
                    // DesignerSemanticUndoableEdit.release() without using the
                    // destructive discardAllEdits() recovery API.
                    nativeHistory.setLimit(0);
                    return null;
                });
            } finally {
                nativeHistory.setLimit(originalLimit);
            }

            assertEquals(0,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "trimming the final native semantic edit must release its exact edge");
            assertTrue(combined.designerSessionActive(),
                    "edge trimming must not close the still-live command owner");
            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(combined.canUndo());
            assertFalse(combined.canRedo());

            document.insertString(document.getLength(),
                    "// durable Source S2 after final semantic edge trim\n",
                    null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);

            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            DesignerCommandRevision reanchoredC1 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    reanchoredC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    reanchoredC1.persistenceKind());
            assertArrayEquals(sourceS2,
                    reanchoredC1.dartCandidateBytes(),
                    "the live command owner must adopt exact durable S2 even with no retained edges");
            assertArrayEquals(history.savedFd(), reanchoredC1.fdBytes());
            assertSame(sourceCurrent, controller.state());
            assertEquals(0,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "Source Save must not recreate the trimmed semantic edge");
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
            assertTrue(combined.canUndo(),
                    "ordinary Source S2 must remain in native history");
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());

            PairSaveEvidence c2 = stageNextPairCommand(
                    pair, sourceCurrent, orchestrator,
                    "after-final-edge-trim");
            assertArrayEquals(sourceS2,
                    c2.preparedPairIdentity().baselineDartBytes(),
                    "the next Designer command must start from exact durable S2");
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    orchestrator.currentRevision().persistenceKind());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertEquals(1,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "only the new S2-to-C2 semantic edge may be retained");
        }
    }

    @Test
    void ordinarySourceSaveRetiresClosedOwnerAfterFinalSemanticEdgeTrim()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_save_after_closed_trimmed_owner");
        DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator();
        try {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            var nativeHistory =
                    editor.nativeUndoRedoManagerForCombinedBridge();
            int originalLimit = nativeHistory.getLimit();
            try {
                onEdt(() -> {
                    nativeHistory.setLimit(0);
                    return null;
                });
            } finally {
                nativeHistory.setLimit(originalLimit);
            }

            assertEquals(0,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "the last trimmed semantic edit must release its edge before owner close");
            assertTrue(combined.designerSessionActive());

            orchestrator.close();

            assertFalse(combined.designerSessionActive(),
                    "the trimmed command owner must be observably closed");
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision);
            assertEquals(0,
                    pair.coordinator().unsavedPairHistoryEdgeCount());

            document.insertString(document.getLength(),
                    "// ordinary Source S2 after trimmed owner close\n", null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());

            sourceCookie.save();

            assertArrayEquals(sourceS2,
                    Files.readAllBytes(pair.dartPath()),
                    "a closed edge-less owner must fall back to exact ordinary Source persistence");
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()),
                    "ordinary Source Save must not rewrite the durable Designer model");
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(0,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "retiring the stale owner must not recreate semantic history");
            assertFalse(combined.designerSessionActive());
            assertTrue(combined.canUndo(),
                    "the ordinary S2 edit must remain in native Source history");
        } finally {
            orchestrator.close();
        }
    }

    @Test
    void ordinarySourceSaveReentersWhenTrimmedOwnerClosesAfterSelection()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_save_owner_close_after_selection");
        DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator();
        TestPair pair = history.staged().pair();
        FlutterDesignerEditorSupport editor =
                pair.dataObject().getEditorSupport();
        DesignerCombinedUndoRedo combined =
                pair.dataObject().getCombinedUndoRedo();
        StyledDocument document = editor.getDocument();
        assertNotNull(document);

        var nativeHistory = editor.nativeUndoRedoManagerForCombinedBridge();
        int originalLimit = nativeHistory.getLimit();
        try {
            onEdt(() -> {
                nativeHistory.setLimit(0);
                return null;
            });
        } finally {
            nativeHistory.setLimit(originalLimit);
        }
        assertEquals(0, pair.coordinator().unsavedPairHistoryEdgeCount());
        assertTrue(combined.designerSessionActive());

        document.insertString(document.getLength(),
                "// ordinary Source S2 while selected owner closes\n", null);
        byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
        SaveCookie sourceCookie = pair.dataObject()
                .getCookie(SaveCookie.class);
        assertNotNull(sourceCookie);

        AtomicInteger selectedOwners = new AtomicInteger();
        AtomicInteger sourceFinalizations = new AtomicInteger();
        pair.coordinator().setSourceHistoryOwnerSelectionHookForTests(owner -> {
            assertSame(orchestrator, owner);
            assertEquals(1, selectedOwners.incrementAndGet(),
                    "the stale history owner must be selected only once");
            owner.close();
        });
        editor.setPersistenceFinalizationHook(
                sourceFinalizations::incrementAndGet);
        try {
            sourceCookie.save();
        } finally {
            pair.coordinator()
                    .setSourceHistoryOwnerSelectionHookForTests(null);
            editor.clearPersistenceFinalizationHook();
            orchestrator.close();
        }

        assertEquals(1, selectedOwners.get());
        assertEquals(1, sourceFinalizations.get(),
                "race re-entry must perform exactly one ordinary CES Save");
        assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
        assertArrayEquals(history.savedFd(),
                Files.readAllBytes(pair.designerPath()));
        assertFalse(editor.sourceModified());
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                pair.coordinator().state().status());
        assertNull(pair.dataObject().getCookie(SaveCookie.class));
        assertEquals(0, pair.coordinator().unsavedPairHistoryEdgeCount());
        assertFalse(combined.designerSessionActive());
        assertTrue(combined.canUndo(),
                "ordinary Source S2 must remain in native Undo history");
    }

    @Test
    void unchangedSourceSaveAfterExactEditRevertRetainsSavedSemanticHistory()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_unchanged_source_save");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            DesignerCommandRevision savedC1 =
                    orchestrator.currentRevision();
            FlutterDesignerDocumentState.Current savedCurrent =
                    history.savedCurrent();
            int editOffset = document.getLength();
            String transientSource =
                    "// transient unmanaged Source edit reverted before Save\n";
            document.insertString(editOffset, transientSource, null);
            byte[] sourceWithTransientEdit =
                    editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            document.remove(editOffset, transientSource.length());

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "edit plus exact revert must serialize to durable C1");
            assertTrue(editor.sourceModified(),
                    "CES must remain dirty until its current native position is saved");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());

            // Both exact candidates already equal disk, so the transaction
            // result for this history-aware Source Save is UNCHANGED.
            sourceCookie.save();

            assertSame(savedC1, orchestrator.currentRevision(),
                    "UNCHANGED must preserve the exact command revision owner");
            assertSame(savedCurrent, controller.state(),
                    "UNCHANGED must not replace the exact loaded Current");
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified(),
                    "CES must move its savepoint to the exact reverted position");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "UNCHANGED must retain the saved B-to-C1 semantic edge");
            assertSavedC1RemainsDurable(history);
            assertTrue(combined.canUndo(),
                    "the exact edit/revert native entries must remain above the semantic edge");

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(sourceWithTransientEdit,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the first Undo must undo only the transient removal");
            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo());
            assertTrue(combined.canRedo());

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the second Undo must remove the transient insertion and expose native C1");
            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "native C1 below the newer savepoint is still dirty");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo(),
                    "the saved semantic B-to-C1 edge must remain next");
            assertTrue(combined.canRedo(),
                    "the transient insertion must remain the first native Redo");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertFormerDurableBIsStaged(history, historicalB);
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedC1RemainsDurable(history);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedCurrent, controller.state());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertTrue(editor.sourceModified(),
                    "semantic C1 remains below the exact edit/revert savepoint");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "the transient insertion must follow semantic C1");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceWithTransientEdit,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedC1, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "the transient removal must be the final native Redo");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedC1, orchestrator.currentRevision());
            assertSame(savedCurrent, controller.state());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified(),
                    "the final Redo must return to the exact saved native position");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertFalse(combined.canRedo());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedC1RemainsDurable(history);
        }
    }

    @Test
    void secondConsecutiveSourceSavePreservesEveryNativeOverlayAboveSemanticHistory()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_second_source_overlay");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// first saved unmanaged Source S2 above C1\n", null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());

            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrentS2 =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            DesignerCommandRevision savedSourceS2 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    savedSourceS2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceS2.persistenceKind());
            assertArrayEquals(sourceS2,
                    savedSourceS2.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());

            document.insertString(document.getLength(),
                    "// second saved unmanaged Source S3 above S2\n", null);
            byte[] sourceS3 = editor.liveSnapshot().markerBearingUtf8();
            assertFalse(Arrays.equals(sourceS2, sourceS3));
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "both Source overlays must use the one stable SaveCookie");

            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrentS3 =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS3);
            assertNotSame(sourceCurrentS2, sourceCurrentS3,
                    "the second Source Save must adopt its own exact durable Current");
            DesignerCommandRevision savedSourceS3 =
                    orchestrator.currentRevision();
            assertNotSame(savedSourceS2, savedSourceS3);
            assertEquals(history.savedRevisionId(),
                    savedSourceS3.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceS3.persistenceKind());
            assertArrayEquals(sourceS3,
                    savedSourceS3.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "both Source saves must retain the one B-to-C1 semantic edge");
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());
            assertArrayEquals(sourceS3, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the first Undo must remove only saved Source S3");
            assertSame(savedSourceS3, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "S2 is below the newer S3 CES savepoint");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo(),
                    "saved Source S2 must now be the next native Undo");
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());
            assertTrue(combined.canRedo(),
                    "saved Source S3 must be the current native Redo");

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the second Undo must remove saved Source S2 and expose native C1");
            assertSame(savedSourceS3, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "native C1 differs from durable Source S3");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo(),
                    "the semantic B-to-C1 edge must remain below S2 and S3");
            assertTrue(combined.canRedo(),
                    "Source S2 must be the first native Redo above C1");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    sourceCurrentS3,
                    historicalB,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    history.savedFd(),
                    history.oldDart(),
                    history.oldFd());
            PairSaveCoordinator.StagedPairProofSnapshot historicalProof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(historicalProof);
            assertArrayEquals(history.savedDart(),
                    historicalProof.preparedPairIdentity().liveDartBytes(),
                    "semantic B must still use native C1 below both Source overlays");
            assertArrayEquals(sourceS3,
                    historicalProof.preparedPairIdentity().baselineDartBytes(),
                    "the second saved Source overlay is the durable semantic baseline");
            assertArrayEquals(history.oldDart(),
                    historicalProof.preparedPairIdentity().prospectiveDartBytes(),
                    "semantic B must exclude both unmanaged Source overlays");
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(savedSourceS3, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(sourceCurrentS3, controller.state());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "Source S2 must be the first Redo above semantic C1");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedSourceS3, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "S2 remains below the durable S3 savepoint");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "Source S3 must be the final native Redo");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS3,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedSourceS3, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertFalse(combined.canRedo());
            assertTrue(combined.canUndo());
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS3, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void newerSourceEditDuringSourceSaveFinalizationStaysDirtyAboveSavedAnchor()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_finalization_overlay");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// durable Source S2 below a finalization race\n", null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);

            CountDownLatch durableOutputReached = new CountDownLatch(1);
            CountDownLatch releaseFinalization = new CountDownLatch(1);
            AtomicReference<Throwable> saveFailure = new AtomicReference<>();
            Thread saveThread = new Thread(() -> {
                try {
                    sourceCookie.save();
                } catch (Throwable failure) {
                    saveFailure.set(failure);
                }
            }, "latched-source-history-save");
            saveThread.setDaemon(true);
            editor.setPersistenceFinalizationHook(() -> {
                durableOutputReached.countDown();
                try {
                    if (!releaseFinalization.await(10, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "Timed out waiting for the newer Source S3 edit");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Interrupted while Source finalization was latched",
                            interrupted);
                }
            });

            byte[] sourceS3;
            try {
                saveThread.start();
                assertTrue(durableOutputReached.await(10, TimeUnit.SECONDS));
                sourceS3 = onEdt(() -> {
                    document.insertString(document.getLength(),
                            "// unmanaged Source S3 after durable S2 output\n",
                            null);
                    return editor.liveSnapshot().markerBearingUtf8();
                });
                assertSame(sourceCookie,
                        pair.dataObject().getCookie(SaveCookie.class),
                        "the newer S3 edit must republish the stable SaveCookie");
            } finally {
                releaseFinalization.countDown();
                saveThread.join(TimeUnit.SECONDS.toMillis(10));
                editor.clearPersistenceFinalizationHook();
                if (saveThread.isAlive()) {
                    saveThread.interrupt();
                }
            }

            assertFalse(saveThread.isAlive());
            assertNull(saveFailure.get());
            assertFalse(Arrays.equals(sourceS2, sourceS3));
            FlutterDesignerDocumentState.Current sourceCurrentS2 =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            DesignerCommandRevision savedSourceS2 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    savedSourceS2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceS2.persistenceKind());
            assertArrayEquals(sourceS2,
                    savedSourceS2.dartCandidateBytes(),
                    "the command anchor must stop at the exact durable S2 output");
            assertFalse(orchestrator.dirty());
            assertArrayEquals(sourceS3,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the unmanaged S3 edit must survive Source-save adoption");
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()),
                    "durable Dart must remain the exact planned S2 bytes");
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "Undo S3 must reveal the exact durable S2 savepoint");
            assertSame(savedSourceS2, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertSame(sourceCurrentS2, controller.state());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());
            assertTrue(combined.canRedo(),
                    "late S3 must remain the current native Redo");

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "Undo S2 must expose native C1 below the durable overlay");
            assertSame(savedSourceS2, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canUndo(),
                    "the semantic B-to-C1 edge must remain below Source S2");
            assertTrue(combined.canRedo(),
                    "Source S2 must be the first native Redo above C1");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    historicalB.persistenceKind());
            assertTrue(orchestrator.dirty());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    sourceCurrentS2,
                    historicalB,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS2,
                    history.savedFd(),
                    history.oldDart(),
                    history.oldFd());
            PairSaveCoordinator.StagedPairProofSnapshot historicalProof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(historicalProof);
            assertArrayEquals(history.savedDart(),
                    historicalProof.preparedPairIdentity().liveDartBytes(),
                    "semantic B must derive from native C1, not late S3");
            assertArrayEquals(sourceS2,
                    historicalProof.preparedPairIdentity().baselineDartBytes(),
                    "semantic history must keep durable S2 as its baseline");
            assertArrayEquals(history.oldDart(),
                    historicalProof.preparedPairIdentity().prospectiveDartBytes(),
                    "the semantic candidate must exclude unmanaged S2 and S3");
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(savedSourceS2, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(sourceCurrentS2, controller.state());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "Source S2 must remain the first native Redo above C1");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedSourceS2, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertTrue(combined.canRedo(),
                    "late S3 must remain the final native Redo above saved S2");

            onEdt(() -> {
                combined.redo();
                return null;
            });

            assertArrayEquals(sourceS3,
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(savedSourceS2, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "redoing unmanaged S3 must leave the durable anchor at S2");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertFalse(combined.canRedo());
            assertTrue(combined.canUndo());
            assertNotEquals("Undo Set Flutter Property",
                    combined.getUndoPresentationName());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void newDesignerCommandAfterSourceSaveStartsAtS2AndRetainsOlderOverlayChronology()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_command_after_source_save");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// durable Source S2 before a new Designer command\n",
                    null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            sourceCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            DesignerCommandRevision savedSourceRevision =
                    orchestrator.currentRevision();
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceRevision.persistenceKind());
            assertArrayEquals(sourceS2,
                    savedSourceRevision.dartCandidateBytes());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());

            PairSaveEvidence c2 = stageNextPairCommand(
                    pair, sourceCurrent, orchestrator,
                    "after-source-save");

            DesignerCommandRevision stagedC2 = orchestrator.currentRevision();
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    stagedC2.persistenceKind());
            assertSame(stagedC2.preparedPair().orElseThrow(),
                    c2.preparedPairIdentity());
            assertArrayEquals(sourceS2,
                    c2.preparedPairIdentity().baselineDartBytes(),
                    "the new command must start from exact durable Source S2");
            assertTrue(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "the new C1-to-C2 edge must not flatten the older B-to-C1 overlay");

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            assertSame(savedSourceRevision, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canUndo(),
                    "saved Source S2 must remain below the new semantic command");

            onEdt(() -> {
                combined.undo();
                return null;
            });

            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Undo must expose C1 below Source S2");
            assertSame(savedSourceRevision, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision historicalB =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), historicalB.revisionId());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    sourceCurrent,
                    historicalB,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS2,
                    history.savedFd(),
                    history.oldDart(),
                    history.oldFd());
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void pairSaveAfterSourceSavePreservesTwoAxisSemanticAndNativeChronology()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_pair_after_source_overlay");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// durable unmanaged Source S2 before C2\n", null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);

            stableCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            DesignerCommandRevision savedSourceC1 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    savedSourceC1.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedSourceC1.persistenceKind());
            assertArrayEquals(sourceS2,
                    savedSourceC1.dartCandidateBytes());
            assertEquals(1, pair.coordinator().unsavedPairHistoryEdgeCount());

            PairSaveEvidence c2 = stageNextPairCommand(
                    pair, sourceCurrent, orchestrator,
                    "pair-after-source-overlay");
            long c2RevisionId = orchestrator.currentRevision().revisionId();
            assertArrayEquals(sourceS2,
                    c2.preparedPairIdentity().baselineDartBytes(),
                    "C2 must be generated from the exact durable S2 envelope");
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "B-to-C1 and C1-to-C2 must remain distinct semantic edges");
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "Source Save and Pair Save must share one stable SaveCookie");

            stableCookie.save();

            FlutterDesignerDocumentState.Current savedC2Current =
                    awaitCurrentWithPair(
                            controller,
                            c2.preparedPairIdentity().prospectiveFdBytes(),
                            c2.candidateDartBytes());
            DesignerCommandRevision savedC2 = orchestrator.currentRevision();
            assertEquals(c2RevisionId, savedC2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedC2.persistenceKind());
            assertArrayEquals(c2.candidateDartBytes(),
                    savedC2.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision c1WithS2 = orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(), c1WithS2.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    c1WithS2.persistenceKind());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Undo must change C2 to C1 without removing S2");
            assertSavedHistoryProof(
                    pair,
                    savedC2Current,
                    c1WithS2,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    sourceS2,
                    history.savedFd());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            DesignerCommandRevision c1WithS0 = orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(), c1WithS0.revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Source Undo must remove S2 while retaining C1");
            assertSavedHistoryProof(
                    pair,
                    savedC2Current,
                    c1WithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.savedDart(),
                    history.savedFd());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision bWithS0 = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), bWithS0.revisionId());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the older semantic Undo must change C1 to B inside S0");
            assertSavedHistoryProof(
                    pair,
                    savedC2Current,
                    bWithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.oldDart(),
                    history.oldFd());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);
            assertFalse(combined.canUndo());
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            DesignerCommandRevision redoneC1WithS0 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    redoneC1WithS0.revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Redo must restore C1 without adding S2");
            assertSavedHistoryProof(
                    pair,
                    savedC2Current,
                    redoneC1WithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.savedDart(),
                    history.savedFd());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
                return null;
            });

            DesignerCommandRevision redoneC1WithS2 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    redoneC1WithS2.revisionId());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Source Redo must restore S2 without changing C1");
            assertSavedHistoryProof(
                    pair,
                    savedC2Current,
                    redoneC1WithS2,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    c2.candidateDartBytes(),
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    sourceS2,
                    history.savedFd());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertEquals(c2RevisionId,
                    orchestrator.currentRevision().revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertArrayEquals(c2.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the final semantic Redo must restore clean C2 inside S2");
            assertSame(savedC2Current, controller.state());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertFalse(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);

            document.insertString(document.getLength(),
                    "// durable unmanaged Source S3 after saved C2\n", null);
            byte[] sourceS3 = editor.liveSnapshot().markerBearingUtf8();
            assertFalse(Arrays.equals(c2.candidateDartBytes(), sourceS3));
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            stableCookie.save();

            FlutterDesignerDocumentState.Current savedC2S3Current =
                    awaitCurrentWithPair(
                            controller,
                            c2.preparedPairIdentity().prospectiveFdBytes(),
                            sourceS3);
            DesignerCommandRevision savedC2S3 =
                    orchestrator.currentRevision();
            assertNotSame(savedC2, savedC2S3,
                    "Source S3 must install a new exact physical baseline identity");
            assertEquals(c2RevisionId, savedC2S3.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    savedC2S3.persistenceKind());
            assertArrayEquals(sourceS3,
                    savedC2S3.dartCandidateBytes());
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "the repeated Source Save must retain both semantic edges");
            assertArrayEquals(sourceS3,
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(c2.preparedPairIdentity().prospectiveFdBytes(),
                    Files.readAllBytes(pair.designerPath()));

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            assertSame(savedC2S3, orchestrator.currentRevision());
            assertArrayEquals(c2.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Undo must remove only S3 and expose C2/S2");
            assertSame(savedC2S3Current, controller.state());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision repeatedC1WithS2 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    repeatedC1WithS2.revisionId());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Undo must restore the physical C1/S2 variant");
            assertSavedHistoryProof(
                    pair,
                    savedC2S3Current,
                    repeatedC1WithS2,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    sourceS2,
                    history.savedFd());
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            DesignerCommandRevision repeatedC1WithS0 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    repeatedC1WithS0.revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Undo must restore the distinct physical C1/S0 variant");
            assertSavedHistoryProof(
                    pair,
                    savedC2S3Current,
                    repeatedC1WithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.savedDart(),
                    history.savedFd());
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision repeatedBWithS0 =
                    orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(),
                    repeatedBWithS0.revisionId());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the oldest semantic Undo must restore B/S0");
            assertSavedHistoryProof(
                    pair,
                    savedC2S3Current,
                    repeatedBWithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.oldDart(),
                    history.oldFd());
            assertFalse(combined.canUndo());
            assertTrue(combined.canRedo());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertEquals(history.savedRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Redo must restore C1/S0");
            assertSavedHistoryProof(
                    pair,
                    savedC2S3Current,
                    orchestrator.currentRevision(),
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    history.savedDart(),
                    history.savedFd());

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
                return null;
            });

            assertEquals(history.savedRevisionId(),
                    orchestrator.currentRevision().revisionId());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Redo must restore C1/S2");
            assertSavedHistoryProof(
                    pair,
                    savedC2S3Current,
                    orchestrator.currentRevision(),
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    sourceS3,
                    c2.preparedPairIdentity().prospectiveFdBytes(),
                    sourceS2,
                    history.savedFd());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(savedC2S3, orchestrator.currentRevision());
            assertArrayEquals(c2.candidateDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Redo must restore C2/S2 below S3");
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertFalse(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
                return null;
            });

            assertSame(savedC2S3, orchestrator.currentRevision());
            assertSame(savedC2S3Current, controller.state());
            assertArrayEquals(sourceS3,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the final native Redo must restore clean C2/S3");
            assertFalse(orchestrator.dirty());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertFalse(combined.canRedo());
            assertArrayEquals(sourceS3,
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(c2.preparedPairIdentity().prospectiveFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void nextDesignerCommandFromHistoricalPhysicalEndpointBranchesOnExactS0Envelope()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_command_from_physical_c1_s0");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// durable unmanaged Source S2 before saved C2\n", null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);
            stableCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            PairSaveEvidence c2 = stageNextPairCommand(
                    pair,
                    sourceCurrent,
                    orchestrator,
                    "durable-C2-before-physical-branch");
            long c2RevisionId = orchestrator.currentRevision().revisionId();
            stableCookie.save();

            byte[] durableC2Dart = c2.candidateDartBytes();
            byte[] durableC2Fd = c2.preparedPairIdentity()
                    .prospectiveFdBytes();
            FlutterDesignerDocumentState.Current durableC2Current =
                    awaitCurrentWithPair(
                            controller, durableC2Fd, durableC2Dart);
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "semantic Undo must first expose physical C1/S2");

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            DesignerCommandRevision c1WithS0 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    c1WithS0.revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "native Undo must expose the exact noncanonical C1/S0 endpoint");
            assertSavedHistoryProof(
                    pair,
                    durableC2Current,
                    c1WithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    durableC2Dart,
                    durableC2Fd,
                    history.savedDart(),
                    history.savedFd());
            PairSaveCoordinator.StagedPairProofSnapshot c1WithS0Proof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(c1WithS0Proof);
            PairSaveCoordinator.StagedCommandSource commandSource =
                    pair.coordinator().captureStagedCommandSource();
            PreparedDesignerPair exactC1WithS0 =
                    c1WithS0Proof.preparedPairIdentity();
            assertSame(exactC1WithS0,
                    commandSource.physicalPairIdentity());
            assertArrayEquals(durableC2Dart,
                    exactC1WithS0.baselineDartBytes(),
                    "the endpoint proof must retain exact durable C2/S2 as its transaction baseline");
            assertArrayEquals(durableC2Fd,
                    exactC1WithS0.baselineFdBytes());
            assertFalse(Arrays.equals(
                    history.savedDart(), exactC1WithS0.liveDartBytes()),
                    "the durable-side template must carry C2 managed payloads rather than the C1 candidate");
            assertFalse(new String(
                    exactC1WithS0.liveDartBytes(), StandardCharsets.UTF_8)
                    .contains("durable unmanaged Source S2"),
                    "the endpoint proof must project durable C2 managed payloads into the S0 unmanaged envelope");
            assertArrayEquals(history.savedDart(),
                    exactC1WithS0.prospectiveDartBytes());
            assertTrue(combined.canRedo(),
                    "the unbranched native suffix still contains Source S2 and C2");

            var commandAttempt = commandSource.beginCommand(
                    new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue(
                                    "branched-C3-from-C1-S0")));
            var pendingC3 = commandAttempt.lease().orElseThrow();
            DesignerCommandRevision c3Revision =
                    pendingC3.candidateRevision();
            PreparedDesignerPair preparedC3 = c3Revision
                    .preparedPair().orElseThrow();
            assertEquals(c2RevisionId + 1, c3Revision.revisionId(),
                    "branching after Undo must allocate C3 without reusing C2's revision id");
            assertSame(exactC1WithS0,
                    pendingC3.physicalPredecessorPairIdentity().orElseThrow());
            assertArrayEquals(durableC2Dart,
                    preparedC3.baselineDartBytes(),
                    "the pending command must remain transaction-bound to durable C2/S2");
            assertArrayEquals(durableC2Fd,
                    preparedC3.baselineFdBytes());
            assertArrayEquals(exactC1WithS0.liveDartBytes(),
                    preparedC3.liveDartBytes(),
                    "C3 must be generated from the exact durable-side S0 template, not durable S2");
            assertFalse(Arrays.equals(sourceS2,
                    preparedC3.liveDartBytes()));

            Path projectRoot = pair.dartPath().getParent().getParent();
            Path flutterSdkRoot = temporaryDirectory.resolve(
                    projectRoot.getFileName() + "-flutter").toRealPath();
            Path frameworkReal = flutterSdkRoot.resolve(
                    "packages/flutter/lib/src/widgets/framework.dart")
                    .toRealPath();
            PairSaveEvidence c3;
            try (pendingC3;
                    PairSaveCoordinator.PairReplacement replacement =
                            pair.coordinator().beginStagedReplacement(
                                    commandSource,
                                    pendingC3)) {
                FlutterSettings settings = FlutterSettings.getDefault();
                FlutterToolchainConfig priorConfig = settings.load();
                PairCandidateAnalysisTicket ticket;
                try {
                    settings.save(new FlutterToolchainConfig(
                            flutterSdkRoot.toString(), true, ""));
                    ticket = replacement.prepareAnalysis(
                            projectRoot.toRealPath(),
                            DartCandidateWarningPolicy.ALLOW);
                } finally {
                    settings.save(priorConfig);
                }
                assertSame(durableC2Current, ticket.currentIdentity());
                assertSame(preparedC3, ticket.preparedIdentity());
                PairSaveEvidenceResult result =
                        replacement.acceptAnalysisAndReplace(
                                ticket,
                                passingAnalysis(ticket, frameworkReal, 0));
                assertTrue(result.ready(),
                        () -> result.diagnostics().toString());
                c3 = ((PairSaveEvidenceResult.Ready) result).evidence();
            }

            assertSame(c3Revision, orchestrator.currentRevision());
            assertSame(preparedC3, c3.preparedPairIdentity());
            assertArrayEquals(durableC2Dart, c3.baselineDartBytes());
            assertArrayEquals(durableC2Fd, c3.baselineFdBytes());
            assertArrayEquals(preparedC3.prospectiveDartBytes(),
                    c3.candidateDartBytes());
            assertArrayEquals(preparedC3.prospectiveDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSame(c3, pair.coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertTrue(editor.sourceModified());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "the branch must replace C1-to-C2 with exactly one native C1-to-C3 semantic edge");
            assertFalse(combined.canRedo(),
                    "admitting C3 must discard the native S2/C2 redo branch");
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            assertSame(c1WithS0, orchestrator.currentRevision(),
                    "one semantic Undo must restore the exact C1 logical predecessor");
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "one semantic Undo must restore the exact physical S0 envelope");
            PairSaveCoordinator.StagedPairProofSnapshot undoneC1Proof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(undoneC1Proof);
            assertSame(exactC1WithS0,
                    undoneC1Proof.preparedPairIdentity(),
                    "Undo must restore the endpoint-specific predecessor proof identity");
            assertTrue(combined.canUndo());
            assertTrue(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);

            DesignerCommandRevision bWithS0 = orchestrator.currentRevision();
            assertEquals(history.oldRevisionId(), bWithS0.revisionId());
            assertArrayEquals(history.oldDart(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "the older semantic Undo must restore B/S0");
            assertSavedHistoryProof(
                    pair,
                    durableC2Current,
                    bWithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    durableC2Dart,
                    durableC2Fd,
                    history.oldDart(),
                    history.oldFd());
            assertFalse(combined.canUndo());
            assertTrue(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(c1WithS0, orchestrator.currentRevision());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            PairSaveCoordinator.StagedPairProofSnapshot redoneC1Proof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(redoneC1Proof);
            assertSame(exactC1WithS0,
                    redoneC1Proof.preparedPairIdentity());
            assertTrue(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.REDO);

            assertSame(c3Revision, orchestrator.currentRevision());
            PairSaveEvidence reboundC3 = pair.coordinator().stagedEvidence();
            assertNotNull(reboundC3);
            assertSame(c3.analyzedCandidateIdentity(),
                    reboundC3.analyzedCandidateIdentity(),
                    "Redo may rebind live evidence but must not rerun or replace analyzer authority");
            assertSame(preparedC3, reboundC3.preparedPairIdentity());
            assertArrayEquals(preparedC3.prospectiveDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertTrue(editor.sourceModified());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertFalse(combined.canRedo());
            assertSavedPairRemainsDurable(pair, c2);
        }
    }

    @Test
    void canonicalCommandFromHistoricalC1S0IsRejectedWithoutMutationAndMayRetryPhysically()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_rejects_canonical_seam");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            FlutterDesignerEditorSupport editor = fixture.editor();
            Object exactSavedProof = stagedCommandSourceProofIdentity(
                    fixture.commandSource());

            var canonicalAttempt = orchestrator.beginCommand(
                    setDataProperty("canonical-command-must-not-use-S2"));
            var canonicalPending = canonicalAttempt.lease().orElseThrow();
            assertTrue(canonicalPending
                    .physicalPredecessorPairIdentity().isEmpty(),
                    "plain beginCommand must not fabricate physical-endpoint authority");
            assertArrayEquals(fixture.durableC2().candidateDartBytes(),
                    canonicalPending.candidateRevision()
                            .preparedPair().orElseThrow().liveDartBytes(),
                    "the canonical seam demonstrates why durable C2/S2 is wrong for live C1/S0");
            try (canonicalPending) {
                IOException failure = assertThrows(
                        IOException.class,
                        () -> pair.coordinator().beginStagedReplacement(
                                fixture.commandSource(), canonicalPending));
                assertTrue(failure.getMessage().contains(
                        "pending predecessor is not the exact staged C1 pair"),
                        () -> "unexpected rejection: " + failure.getMessage());
            }

            assertSame(fixture.c1WithS0(),
                    orchestrator.currentRevision(),
                    "rejection must abort the pending canonical branch to exact C1");
            assertTrue(fixture.liveC1WithS0().sameEvidence(
                    editor.liveSnapshot()),
                    "rejection before reservation must not touch document identity or bytes");
            assertSame(exactSavedProof,
                    stagedCommandSourceProofIdentity(
                            pair.coordinator().captureStagedCommandSource()),
                    "canonical rejection must preserve the exact SavedHistoryProof identity");
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);

            var retryAttempt = fixture.commandSource().beginCommand(
                    setDataProperty("physical-retry-after-canonical-rejection"));
            try (var retryPending = retryAttempt.lease().orElseThrow();
                    PairSaveCoordinator.PairReplacement retryReplacement =
                            pair.coordinator().beginStagedReplacement(
                                    fixture.commandSource(), retryPending)) {
                assertEquals(PairSaveCoordinatorStatus.PREPARING_REPLACEMENT,
                        pair.coordinator().state().status(),
                        "the same exact token must remain usable for a physical retry");
            }

            assertSame(fixture.c1WithS0(), orchestrator.currentRevision());
            assertArrayEquals(fixture.history().savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void nativeMoveAfterPhysicalCommandSourceCaptureMakesReservationTokenStale()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_stale_after_native_move");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            DesignerCombinedUndoRedo combined = fixture.combined();
            PairSaveCoordinator.StagedCommandSource staleSource =
                    fixture.commandSource();
            PreparedDesignerPair stalePhysicalPair =
                    staleSource.physicalPairIdentity();
            Object staleSavedProof = stagedCommandSourceProofIdentity(
                    staleSource);

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
                return null;
            });

            assertSame(fixture.c1WithS0(), orchestrator.currentRevision(),
                    "native S0-to-S2 Redo must not move the logical C1 cursor");
            assertArrayEquals(fixture.sourceS2(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            PairSaveCoordinator.StagedCommandSource currentSource =
                    pair.coordinator().captureStagedCommandSource();
            assertNotSame(stalePhysicalPair,
                    currentSource.physicalPairIdentity(),
                    "the native move must install a distinct physical C1/S2 endpoint");
            assertNotSame(staleSavedProof,
                    stagedCommandSourceProofIdentity(currentSource),
                    "the captured C1/S0 SavedHistoryProof token must become stale");

            var staleAttempt = staleSource.beginCommand(
                    setDataProperty("stale-C3-from-captured-S0"));
            try (var stalePending = staleAttempt.lease().orElseThrow()) {
                IOException failure = assertThrows(
                        IOException.class,
                        () -> pair.coordinator().beginStagedReplacement(
                                staleSource, stalePending));
                assertTrue(failure.getMessage().contains(
                        "Only the exact currently staged Flutter Designer pair"),
                        () -> "unexpected stale-token rejection: "
                        + failure.getMessage());
            }

            assertSame(fixture.c1WithS0(), orchestrator.currentRevision());
            assertArrayEquals(fixture.sourceS2(),
                    fixture.editor().liveSnapshot().markerBearingUtf8(),
                    "stale reservation rejection must preserve the newer C1/S2 live endpoint");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertTrue(combined.canRedo(),
                    "rejecting stale C1/S0 must preserve the remaining semantic C2 Redo");
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void sourceEditFromCommandListenerInvalidatesPhysicalTokenBeforeReservation()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_listener_source_edit");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    fixture.commandSource();
            StyledDocument document = fixture.editor().getDocument();
            assertNotNull(document);
            String userEdit = "// user Source edit from command listener\n";
            AtomicBoolean edited = new AtomicBoolean();
            AtomicBoolean preparingPublished = new AtomicBoolean();
            javax.swing.event.ChangeListener commandListener = event -> {
                if (!edited.compareAndSet(false, true)) {
                    return;
                }
                try {
                    onEdt(() -> {
                        document.insertString(
                                document.getLength(), userEdit, null);
                        return null;
                    });
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            };
            java.beans.PropertyChangeListener pairListener = event -> {
                if (event.getNewValue()
                        instanceof PairSaveCoordinatorSnapshot snapshot
                        && snapshot.status()
                            == PairSaveCoordinatorStatus.PREPARING_REPLACEMENT) {
                    preparingPublished.set(true);
                }
            };
            orchestrator.addChangeListener(commandListener);
            pair.coordinator().addPropertyChangeListener(pairListener);
            try {
                var attempt = commandSource.beginCommand(
                        setDataProperty("C3-after-listener-edit"));
                try (var pending = attempt.lease().orElseThrow()) {
                    IOException failure = assertThrows(
                            IOException.class,
                            () -> pair.coordinator().beginStagedReplacement(
                                    commandSource, pending));
                    assertTrue(failure.getMessage().contains(
                            "Only the exact currently staged Flutter Designer pair"),
                            () -> "unexpected stale-source rejection: "
                            + failure.getMessage());
                }
            } finally {
                orchestrator.removeChangeListener(commandListener);
                pair.coordinator().removePropertyChangeListener(pairListener);
            }

            assertTrue(edited.get());
            assertFalse(preparingPublished.get(),
                    "the monotonic document version must reject before replacement publication");
            assertSame(fixture.c1WithS0(), orchestrator.currentRevision(),
                    "an unclaimed stale token must abort the pending command");
            byte[] expectedLive = (new String(
                    fixture.history().savedDart(), StandardCharsets.UTF_8)
                    + userEdit).getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(expectedLive,
                    fixture.editor().liveSnapshot().markerBearingUtf8(),
                    "early rejection must preserve the exact user Source edit");
            assertSavedPairRemainsDurable(pair, fixture.durableC2());
        }
    }

    @Test
    void exactEditRevertAfterPhysicalSourceCaptureRejectsBeforePreparing()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_stale_after_exact_edit_revert");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            FlutterDesignerEditorSupport editor = fixture.editor();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);
            assertTrue(editor.sourceModified(),
                    "the C1/S0 historical endpoint must already be dirty relative to durable C2/S2");

            PairSaveCoordinator.StagedCommandSource staleSource =
                    fixture.commandSource();
            PreparedDesignerPair exactPhysicalPair =
                    staleSource.physicalPairIdentity();
            long documentVersionBefore =
                    editor.liveDocumentVersion(document);
            String transientEdit =
                    "// transient exact-revert stale-token probe\n";
            onEdt(() -> {
                int offset = document.getLength();
                document.insertString(offset, transientEdit, null);
                document.remove(offset, transientEdit.length());
                return null;
            });

            LiveDartDocumentSnapshot revertedLive = editor.liveSnapshot();
            assertArrayEquals(fixture.history().savedDart(),
                    revertedLive.markerBearingUtf8(),
                    "the transient Source edit must revert to byte-exact C1/S0");
            assertTrue(editor.sourceModified(),
                    "an exact revert inside an already-dirty CES must stay dirty");
            assertTrue(editor.liveDocumentVersion(document)
                    > documentVersionBefore,
                    "the document version must distinguish exact bytes reached through a newer edit path");
            assertFalse(fixture.liveC1WithS0().sameEvidence(revertedLive),
                    "byte equality must not collapse the newer document evidence identity");

            AtomicInteger preparingTransitions = new AtomicInteger();
            java.beans.PropertyChangeListener stateListener = event -> {
                if (event.getNewValue()
                        instanceof PairSaveCoordinatorSnapshot snapshot
                        && snapshot.status()
                            == PairSaveCoordinatorStatus.PREPARING_REPLACEMENT) {
                    preparingTransitions.incrementAndGet();
                }
            };
            pair.coordinator().addPropertyChangeListener(stateListener);
            try {
                var staleAttempt = staleSource.beginCommand(
                        setDataProperty("stale-C3-after-exact-edit-revert"));
                try (var stalePending =
                        staleAttempt.lease().orElseThrow()) {
                    IOException failure = assertThrows(
                            IOException.class,
                            () -> pair.coordinator().beginStagedReplacement(
                                    staleSource, stalePending));
                    assertTrue(failure.getMessage().contains(
                            "Only the exact currently staged Flutter Designer pair"),
                            () -> "unexpected exact-revert rejection: "
                            + failure.getMessage());
                }
            } finally {
                pair.coordinator().removePropertyChangeListener(
                        stateListener);
            }

            assertEquals(0, preparingTransitions.get(),
                    "a stale document-version token must reject before PREPARING_REPLACEMENT publication");
            assertSame(fixture.c1WithS0(), orchestrator.currentRevision());
            assertArrayEquals(fixture.history().savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());

            PairSaveCoordinator.StagedCommandSource reboundSource =
                    pair.coordinator().captureStagedCommandSource();
            assertSame(exactPhysicalPair,
                    reboundSource.physicalPairIdentity(),
                    "refresh after exact revert must retain the endpoint-specific PreparedPair proof");
            PairSaveCoordinator.StagedPairProofSnapshot reboundProof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(reboundProof);
            assertEquals(PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    reboundProof.kind());
            assertSame(exactPhysicalPair,
                    reboundProof.preparedPairIdentity());
            assertArrayEquals(fixture.durableC2().candidateDartBytes(),
                    reboundProof.baselineDartBytes());
            assertArrayEquals(fixture.history().savedDart(),
                    reboundProof.candidateDartBytes());
            assertTrue(reboundProof.liveCandidateIdentity().sameEvidence(
                    editor.liveSnapshot()),
                    "the retained proof must rebind only its exact newer live evidence");
            assertSavedPairRemainsDurable(
                    fixture.pair(), fixture.durableC2());
            assertEquals(1,
                    pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "the transient native branch must trim C1-to-C2 while retaining B-to-C1");
            assertSame(fixture.stableCookie(),
                    pair.dataObject().getCookie(SaveCookie.class));
        }
    }

    @Test
    void closingPhysicalReplacementBeforeApplyRetainsExactC1S0SavedHistoryProof()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_close_before_apply");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    fixture.commandSource();
            Object exactSavedProof = stagedCommandSourceProofIdentity(
                    commandSource);
            PreparedDesignerPair exactPhysicalPair =
                    commandSource.physicalPairIdentity();

            var commandAttempt = commandSource.beginCommand(
                    setDataProperty("cancel-C3-before-live-apply"));
            try (var pending = commandAttempt.lease().orElseThrow();
                    PairSaveCoordinator.PairReplacement replacement =
                            pair.coordinator().beginStagedReplacement(
                                    commandSource, pending)) {
                assertEquals(PairSaveCoordinatorStatus.PREPARING_REPLACEMENT,
                        pair.coordinator().state().status());
                assertSame(exactPhysicalPair,
                        pending.physicalPredecessorPairIdentity().orElseThrow());
                // Closing the resources is the tested cancellation edge: no
                // analyzer ticket and no live apply have been issued.
            }

            PairSaveCoordinator.StagedCommandSource retainedSource =
                    pair.coordinator().captureStagedCommandSource();
            assertSame(exactSavedProof,
                    stagedCommandSourceProofIdentity(retainedSource),
                    "cancellation before apply must retain the exact SavedHistoryProof object");
            assertSame(exactPhysicalPair,
                    retainedSource.physicalPairIdentity(),
                    "cancellation must retain the exact C1/S0 PreparedPair identity");
            assertSame(fixture.c1WithS0(), orchestrator.currentRevision());
            assertTrue(fixture.liveC1WithS0().sameEvidence(
                    fixture.editor().liveSnapshot()),
                    "closing the replacement must not mutate or rebound the live document");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertSame(fixture.stableCookie(),
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(fixture.combined().canRedo(),
                    "cancellation must preserve the original S2/C2 redo suffix");
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void analyzerRejectionRetainsExactPhysicalC1S0SavedHistoryProof()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_analyzer_rejection");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    fixture.commandSource();
            Object exactSavedProof = stagedCommandSourceProofIdentity(
                    commandSource);
            PreparedDesignerPair exactPhysicalPair =
                    commandSource.physicalPairIdentity();
            LiveDartDocumentSnapshot exactLive = fixture.liveC1WithS0();
            boolean canUndo = fixture.combined().canUndo();
            boolean canRedo = fixture.combined().canRedo();
            String undoName = fixture.combined().getUndoPresentationName();
            String redoName = fixture.combined().getRedoPresentationName();

            var commandAttempt = commandSource.beginCommand(
                    setDataProperty("rejected-C3-from-C1-S0"));
            PairSaveEvidenceResult result;
            try (var pending = commandAttempt.lease().orElseThrow();
                    PairSaveCoordinator.PairReplacement replacement =
                            pair.coordinator().beginStagedReplacement(
                                    commandSource, pending)) {
                ReplacementTicket analysisTicket = replacementTicket(
                        fixture.history().staged(), replacement);
                result = replacement.acceptAnalysisAndReplace(
                        analysisTicket.ticket(),
                        passingAnalysis(
                                analysisTicket.ticket(),
                                analysisTicket.frameworkReal(),
                                1));
            }

            assertFalse(result.ready(),
                    "stale analyzer evidence must reject the physical candidate");
            PairSaveCoordinator.StagedCommandSource retainedSource =
                    pair.coordinator().captureStagedCommandSource();
            assertSame(exactSavedProof,
                    stagedCommandSourceProofIdentity(retainedSource),
                    "analyzer rejection before apply must retain the exact SavedHistoryProof object");
            assertSame(exactPhysicalPair,
                    retainedSource.physicalPairIdentity());
            assertSame(fixture.c1WithS0(),
                    orchestrator.currentRevision());
            assertTrue(exactLive.sameEvidence(
                    fixture.editor().liveSnapshot()),
                    "analyzer rejection must not mutate or rebind C1/S0");
            assertNull(pair.coordinator().stagedEvidence(),
                    "SavedHistoryProof must remain analyzer-free");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertSame(fixture.stableCookie(),
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2,
                    pair.coordinator().unsavedPairHistoryEdgeCount());
            assertEquals(canUndo, fixture.combined().canUndo());
            assertEquals(canRedo, fixture.combined().canRedo());
            assertEquals(undoName,
                    fixture.combined().getUndoPresentationName());
            assertEquals(redoName,
                    fixture.combined().getRedoPresentationName());
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void physicalApplyFailureRebuildsExactC1S0ProofWithoutHistoryMutation()
            throws Exception {
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        "physical_command_apply_failure_recovery");
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    fixture.commandSource();
            Object priorSavedProof = stagedCommandSourceProofIdentity(
                    commandSource);
            PreparedDesignerPair exactPhysicalPair =
                    commandSource.physicalPairIdentity();
            LiveDartDocumentSnapshot exactLive = fixture.liveC1WithS0();
            StyledDocument document = fixture.editor().getDocument();
            assertNotNull(document);
            boolean canUndo = fixture.combined().canUndo();
            boolean canRedo = fixture.combined().canRedo();
            String undoName = fixture.combined().getUndoPresentationName();
            String redoName = fixture.combined().getRedoPresentationName();

            var commandAttempt = commandSource.beginCommand(
                    setDataProperty("failed-C3-from-C1-S0"));
            AtomicBoolean failedOnce = new AtomicBoolean();
            DocumentListener failingListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    failOnce();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    failOnce();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    failOnce();
                }

                private void failOnce() {
                    if (failedOnce.compareAndSet(false, true)) {
                        throw new AssertionError(
                                "synthetic physical C3 apply failure");
                    }
                }
            };
            try (var pending = commandAttempt.lease().orElseThrow();
                    PairSaveCoordinator.PairReplacement replacement =
                            pair.coordinator().beginStagedReplacement(
                                    commandSource, pending)) {
                ReplacementTicket analysisTicket = replacementTicket(
                        fixture.history().staged(), replacement);
                document.addDocumentListener(failingListener);
                try {
                    assertThrows(IOException.class,
                            () -> replacement.acceptAnalysisAndReplace(
                                    analysisTicket.ticket(),
                                    passingAnalysis(
                                            analysisTicket.ticket(),
                                            analysisTicket.frameworkReal(),
                                            0)));
                } finally {
                    document.removeDocumentListener(failingListener);
                }
            }

            assertTrue(failedOnce.get());
            PairSaveCoordinator.StagedCommandSource recoveredSource =
                    pair.coordinator().captureStagedCommandSource();
            Object recoveredProof = stagedCommandSourceProofIdentity(
                    recoveredSource);
            assertNotSame(priorSavedProof, recoveredProof,
                    "an observed apply/rollback must rebuild fresh live proof identity");
            assertSame(exactPhysicalPair,
                    recoveredSource.physicalPairIdentity(),
                    "recovery must retain the exact physical C1/S0 pair identity");
            assertSame(fixture.c1WithS0(),
                    orchestrator.currentRevision());
            LiveDartDocumentSnapshot restoredLive =
                    fixture.editor().liveSnapshot();
            assertSame(exactLive.documentIdentity(),
                    restoredLive.documentIdentity());
            assertTrue(restoredLive.documentVersion()
                    > exactLive.documentVersion(),
                    "the fresh proof must observe the apply/rollback revision");
            assertArrayEquals(exactLive.markerBearingUtf8(),
                    restoredLive.markerBearingUtf8(),
                    "recovery must restore exact C1/S0 bytes");
            assertNull(pair.coordinator().stagedEvidence(),
                    "the rebuilt SavedHistoryProof must remain analyzer-free");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertSame(fixture.stableCookie(),
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2,
                    pair.coordinator().unsavedPairHistoryEdgeCount());
            assertEquals(canUndo, fixture.combined().canUndo());
            assertEquals(canRedo, fixture.combined().canRedo());
            assertEquals(undoName,
                    fixture.combined().getUndoPresentationName());
            assertEquals(redoName,
                    fixture.combined().getRedoPresentationName(),
                    "atomic rollback must not add a phantom native history entry");
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void physicalCommandAggregateBudgetRejectsBeforeAnalyzerAndCesMutation()
            throws Exception {
        String sourceSuffix =
                "// budgeted Source S2 retained beside physical C1/S0\n";
        String c2Value = "budgeted-durable-C2";
        String c3Value = "budgeted-branch-C3";
        StagedPair staged = stageRealPairWithPhysicalCommandBudget(
                "physical_command_aggregate_budget",
                sourceSuffix,
                c2Value,
                c3Value);
        HistoricalPhysicalCommandFixture fixture =
                historicalPhysicalC1S0Fixture(
                        saveC1HistoryFixture(staged),
                        sourceSuffix,
                        c2Value);
        try (DesignerCommandSessionOrchestrator orchestrator =
                fixture.orchestrator()) {
            TestPair pair = fixture.pair();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    fixture.commandSource();
            Object exactSavedProof = stagedCommandSourceProofIdentity(
                    commandSource);
            PreparedDesignerPair exactPhysicalPair =
                    commandSource.physicalPairIdentity();
            LiveDartDocumentSnapshot exactLive = fixture.liveC1WithS0();
            DesignerCommandRevision exactC1 = fixture.c1WithS0();
            long maximum = staged.maxRetainedPairBytes();

            var firstAttempt = commandSource.beginCommand(
                    setDataProperty(c3Value));
            var firstPending = firstAttempt.lease().orElseThrow();
            PreparedDesignerPair candidate = firstPending
                    .candidateRevision().preparedPair().orElseThrow();
            assertSame(exactPhysicalPair,
                    firstPending.physicalPredecessorPairIdentity()
                            .orElseThrow());
            assertTrue(retainedPairBytes(
                    candidate.prospectiveDartBytes(),
                    candidate.prospectiveFdBytes()) <= maximum,
                    "the C3 endpoint must fit individually so only aggregate physical history rejects it");
            try (firstPending) {
                IOException failure = assertThrows(
                        IOException.class,
                        () -> pair.coordinator().beginStagedReplacement(
                                commandSource, firstPending));
                assertTrue(failure.getMessage().contains(
                        "Retained physical Source-envelope history requires"),
                        () -> "unexpected aggregate rejection: "
                        + failure.getMessage());
            }

            assertSame(exactC1, orchestrator.currentRevision(),
                    "pre-analyzer budget rejection must abort to exact C1");
            assertNull(pair.coordinator().stagedEvidence(),
                    "budget rejection must occur before any analyzer evidence can be issued or published");
            assertTrue(exactLive.sameEvidence(
                    fixture.editor().liveSnapshot()),
                    "pre-analyzer rejection must preserve CES identity, version and bytes");
            PairSaveCoordinator.StagedCommandSource afterFirstFailure =
                    pair.coordinator().captureStagedCommandSource();
            assertSame(exactSavedProof,
                    stagedCommandSourceProofIdentity(afterFirstFailure));
            assertSame(exactPhysicalPair,
                    afterFirstFailure.physicalPairIdentity());
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);

            var retryAttempt = afterFirstFailure.beginCommand(
                    setDataProperty(c3Value));
            try (var retryPending = retryAttempt.lease().orElseThrow()) {
                IOException retryFailure = assertThrows(
                        IOException.class,
                        () -> pair.coordinator().beginStagedReplacement(
                                afterFirstFailure, retryPending));
                assertTrue(retryFailure.getMessage().contains(
                        "Retained physical Source-envelope history requires"));
            }
            assertSame(exactC1, orchestrator.currentRevision(),
                    "a deterministic retry rejection must release its pending command");
            assertNull(pair.coordinator().stagedEvidence());

            PairSaveCoordinator.StagedCommandSource cancelSource =
                    pair.coordinator().captureStagedCommandSource();
            var cancelAttempt = cancelSource.beginCommand(
                    setDataProperty(c3Value));
            try (var cancelledPending =
                    cancelAttempt.lease().orElseThrow()) {
                assertSame(exactPhysicalPair,
                        cancelledPending.physicalPredecessorPairIdentity()
                                .orElseThrow());
            }

            assertSame(exactC1, orchestrator.currentRevision());
            assertNull(pair.coordinator().stagedEvidence());
            assertTrue(exactLive.sameEvidence(
                    fixture.editor().liveSnapshot()));
            assertSame(exactSavedProof,
                    stagedCommandSourceProofIdentity(
                            pair.coordinator().captureStagedCommandSource()),
                    "retry and explicit pending cancel must retain exact proof authority");
            assertHistoricalPhysicalFixtureRemainsDurable(fixture);
        }
    }

    @Test
    void aggregatePhysicalEndpointBudgetRejectsPairSaveBeforeCesAndTransaction()
            throws Exception {
        String sourceSuffix =
                "// durable S2 retained beside the older physical envelope\n";
        String c2Value = "aggregate-budget-C2-" + "z".repeat(2_048);
        StagedPair staged = stageRealPairWithAggregateBudget(
                "saved_history_aggregate_physical_budget",
                sourceSuffix,
                c2Value);
        SavedPairHistoryFixture history = saveC1HistoryFixture(staged);
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = staged.pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(), sourceSuffix, null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);
            stableCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            PairSaveEvidence c2 = stageNextPairCommand(
                    pair, sourceCurrent, orchestrator, c2Value);
            DesignerCommandRevision stagedC2 = orchestrator.currentRevision();
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    stagedC2.persistenceKind());
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());

            long maximum = staged.maxRetainedPairBytes();
            long[] uniquePhysicalEndpointBytes = {
                retainedPairBytes(history.oldDart(), history.oldFd()),
                retainedPairBytes(history.savedDart(), history.savedFd()),
                retainedPairBytes(sourceS2, history.savedFd()),
                retainedPairBytes(
                        c2.candidateDartBytes(),
                        c2.preparedPairIdentity().prospectiveFdBytes())
            };
            long aggregate = 0;
            for (long endpointBytes : uniquePhysicalEndpointBytes) {
                assertTrue(endpointBytes <= maximum,
                        "every physical endpoint must fit the individual session bound");
                aggregate = Math.addExact(aggregate, endpointBytes);
            }
            assertTrue(aggregate > maximum,
                    "the unique physical endpoint aggregate must exceed the command-history bound");

            AtomicInteger pairTransactions = new AtomicInteger();
            pair.coordinator().setPairTransactionForTests(request -> {
                pairTransactions.incrementAndGet();
                throw new AssertionError(
                        "aggregate preflight must reject before PairFileTransaction");
            });

            IOException failure = assertThrows(
                    IOException.class, stableCookie::save);

            assertTrue(failure.getMessage().contains(
                    "Retained physical Source-envelope history requires"),
                    "unexpected preflight failure: "
                    + failure.getMessage() + "; maximum=" + maximum
                    + "; aggregate=" + aggregate
                    + "; endpoints="
                    + Arrays.toString(uniquePhysicalEndpointBytes));
            assertEquals(0, pairTransactions.get());
            assertSame(stagedC2, orchestrator.currentRevision(),
                    "pre-CES rejection must abort the durable lease back to exact C2");
            assertTrue(orchestrator.dirty());
            assertTrue(editor.sourceModified(),
                    "CES must retain the staged C2 dirty savepoint");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    pair.coordinator().state().status());
            assertSame(c2, pair.coordinator().stagedEvidence());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertEquals(2, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
        }
    }

    @Test
    void controllerGenerationChangeAfterCommittedHistorySourceOutputFailsClosed()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_controller_generation");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// Source S2 before a stale controller adoption ticket\n",
                    null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);

            editor.setPersistenceFinalizationHook(controller::reload);
            try {
                sourceCookie.save();
            } finally {
                editor.clearPersistenceFinalizationHook();
            }

            assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(0, pair.coordinator().unsavedPairHistoryEdgeCount(),
                    "split Source/model adoption must revoke semantic history authority");
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision,
                    "a stale Current ticket must invalidate the Source-anchor owner");
            assertArrayEquals(sourceS2, Files.readAllBytes(pair.dartPath()),
                    "the exact S2 output was already durably committed");
            assertArrayEquals(history.savedFd(),
                    Files.readAllBytes(pair.designerPath()));
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "the recovery conflict must retain the stable SaveCookie");
        }
    }

    @Test
    void rejectedHistorySourceTransactionInvalidatesAuthorityWithoutDiscardingNativeHistory()
            throws Exception {
        SavedPairHistoryFixture history = saveC1HistoryFixture(
                "saved_history_source_rejected_after_ces");
        try (DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator()) {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    "// Source S2 whose cached FD baseline becomes stale\n",
                    null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie sourceCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(sourceCookie);
            var nativeHistory = editor.nativeUndoRedoManagerForCombinedBridge();
            boolean nativeCanUndoBefore = nativeHistory.canUndo();
            boolean nativeCanRedoBefore = nativeHistory.canRedo();
            assertTrue(nativeCanUndoBefore);

            byte[] staleFd = Arrays.copyOf(
                    history.savedFd(), history.savedFd().length + 1);
            staleFd[staleFd.length - 1] = ' ';
            AtomicBoolean injected = new AtomicBoolean();
            AtomicReference<Throwable> injectionFailure = new AtomicReference<>();
            javax.swing.event.ChangeListener staleBaselineListener = event -> {
                if (injected.compareAndSet(false, true)) {
                    try {
                        // Bypass FileObject notification deliberately: the
                        // final transaction baseline check must reject it.
                        Files.write(pair.designerPath(), staleFd);
                    } catch (Throwable failure) {
                        injectionFailure.set(failure);
                    }
                }
            };
            orchestrator.addChangeListener(staleBaselineListener);
            try {
                assertThrows(IOException.class, sourceCookie::save);
            } finally {
                orchestrator.removeChangeListener(staleBaselineListener);
            }

            assertTrue(injected.get());
            assertNull(injectionFailure.get());
            assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                    pair.coordinator().state().status());
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(0, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision,
                    "CES entered persistence, so the command owner must fail closed");
            assertEquals(nativeCanUndoBefore, nativeHistory.canUndo(),
                    "fail-closed Source invalidation must preserve native Undo");
            assertEquals(nativeCanRedoBefore, nativeHistory.canRedo(),
                    "fail-closed Source invalidation must preserve native Redo");
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8(),
                    "a rejected transaction must not mutate live Source S2");
            assertTrue(editor.sourceModified());
            assertArrayEquals(history.savedDart(),
                    Files.readAllBytes(pair.dartPath()),
                    "a rejected transaction must perform zero Dart writes");
            assertArrayEquals(staleFd,
                    Files.readAllBytes(pair.designerPath()));
            assertSame(sourceCookie,
                    pair.dataObject().getCookie(SaveCookie.class),
                    "the sticky conflict must retain the stable SaveCookie");
        }
    }

    @Test
    void zeroWriteFailureAfterCesSavepointInvalidatesAuthorityButPreservesNativeHistory()
            throws Exception {
        StagedPair staged = stageRealPair("pair_false_savepoint_failure");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            AtomicInteger attempts = new AtomicInteger();
            staged.pair().coordinator().setPairTransactionForTests(request -> {
                attempts.incrementAndGet();
                return failedPairResult(
                        PairFileTransactionStatus.FAILED,
                        0,
                        false,
                        PairFileTransactionIssueCode.LOCK_ACQUISITION_FAILED,
                        "synthetic zero-write failure");
            });
            SaveCookie cookie = staged.pair().dataObject()
                    .getCookie(SaveCookie.class);
            var nativeHistory = staged.pair().dataObject().getEditorSupport()
                    .nativeUndoRedoManagerForCombinedBridge();
            boolean nativeCanUndoBefore = nativeHistory.canUndo();
            boolean nativeCanRedoBefore = nativeHistory.canRedo();
            assertTrue(nativeCanUndoBefore,
                    "the staged managed apply must establish the native history fixture");

            assertThrows(IOException.class, cookie::save);

            assertEquals(1, attempts.get());
            assertNull(staged.pair().coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    staged.pair().coordinator().state().status());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision,
                    "the command session cannot survive CES moving its hidden savepoint");
            assertEquals(nativeCanUndoBefore, nativeHistory.canUndo(),
                    "fail-closed command invalidation must not discard native history");
            assertEquals(nativeCanRedoBefore, nativeHistory.canRedo(),
                    "fail-closed command invalidation must not rewrite native Redo");
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    Files.readAllBytes(staged.pair().dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(staged.pair().designerPath()));
            assertSame(cookie,
                    staged.pair().dataObject().getCookie(SaveCookie.class));

            assertThrows(IOException.class, cookie::save,
                    "the false CES savepoint is a sticky recovery conflict, not a retry");
            assertEquals(1, attempts.get(),
                    "a second Save must fail before re-entering pair persistence");
            assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    staged.pair().coordinator().state().status());
        }
    }

    @Test
    void unprovableStagedAuthorityBeforePersistencePreservesNativeHistory()
            throws Exception {
        StagedPair staged = stageRealPair(
                "pair_unprovable_before_persistence");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            TestPair pair = staged.pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);
            SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
            assertNotNull(cookie);

            var nativeHistory = editor.nativeUndoRedoManagerForCombinedBridge();
            assertTrue(nativeHistory.canUndo(),
                    "the staged managed apply must establish native history");
            boolean nativeCanRedoBefore = nativeHistory.canRedo();
            String sourceBeforeNewerEdit =
                    document.getText(0, document.getLength());
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            AtomicBoolean newerEditInjected = new AtomicBoolean();
            AtomicReference<Throwable> injectionFailure = new AtomicReference<>();
            AtomicReference<String> injectedUndoPresentation =
                    new AtomicReference<>();
            AtomicReference<String> injectedRedoPresentation =
                    new AtomicReference<>();
            AtomicInteger pairTransactions = new AtomicInteger();
            String newerEdit =
                    "// user edit after the durable lease was pinned\n";
            pair.coordinator().setPairTransactionForTests(request -> {
                pairTransactions.incrementAndGet();
                throw new AssertionError(
                        "unprovable staged authority must start no pair I/O");
            });
            javax.swing.event.ChangeListener leaseListener = event -> {
                if (!newerEditInjected.compareAndSet(false, true)) {
                    return;
                }
                try {
                    onEdt(() -> {
                        document.insertString(
                                document.getLength(), newerEdit, null);
                        injectedUndoPresentation.set(
                                nativeHistory.getUndoPresentationName());
                        injectedRedoPresentation.set(
                                nativeHistory.getRedoPresentationName());
                        return null;
                    });
                } catch (Throwable failure) {
                    injectionFailure.set(failure);
                }
            };
            orchestrator.addChangeListener(leaseListener);
            IOException failure;
            try {
                failure = assertThrows(IOException.class, cookie::save);
            } finally {
                orchestrator.removeChangeListener(leaseListener);
            }

            assertTrue(newerEditInjected.get());
            assertNull(injectionFailure.get());
            assertTrue(failure.getMessage().contains(
                    "live Dart document identity, revision or candidate changed"),
                    () -> "unexpected rejection: " + failure.getMessage());
            assertEquals(0, pairTransactions.get(),
                    "the stale candidate must fail before pair persistence");
            assertNull(pair.coordinator().stagedEvidence());
            assertNull(pair.coordinator().stagedProofSnapshot());
            assertEquals(0, pair.coordinator().unsavedPairHistoryEdgeCount());
            assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    pair.coordinator().state().status());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertFalse(combined.designerSessionActive(),
                    "semantic invalidation must release the Designer binding");
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision,
                    "the unprovable semantic authority must be invalidated");
            assertTrue(nativeHistory.canUndo(),
                    "recovery must retain the native Source Undo history");
            assertEquals(nativeCanRedoBefore, nativeHistory.canRedo(),
                    "recovery must not rewrite the native Redo cursor");
            assertEquals(injectedUndoPresentation.get(),
                    nativeHistory.getUndoPresentationName());
            assertEquals(injectedRedoPresentation.get(),
                    nativeHistory.getRedoPresentationName());
            assertTrue(document.getText(0, document.getLength())
                    .endsWith(newerEdit),
                    "recovery must preserve the newer user-owned Source edit");
            assertTrue(editor.sourceModified());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
            assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));

            PairSaveCoordinatorSnapshot conflict = pair.coordinator().state();
            orchestrator.close();
            assertFalse(combined.designerSessionActive());
            assertSame(conflict, pair.coordinator().state());
            assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));

            onEdt(() -> {
                combined.undo();
                return null;
            });
            assertEquals(sourceBeforeNewerEdit,
                    document.getText(0, document.getLength()),
                    "the retained native Undo must remove the newer Source edit");
            assertThrows(IOException.class, cookie::save,
                    "exact candidate bytes cannot revive invalidated authority");
            assertEquals(0, pairTransactions.get());
            assertSame(conflict, pair.coordinator().state());
            assertSame(cookie, pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
            onEdt(() -> {
                combined.redo();
                return null;
            });
            assertTrue(document.getText(0, document.getLength())
                    .endsWith(newerEdit),
                    "the retained native Redo must restore the newer Source edit");
        }
    }

    @Test
    void uncertainPairFailureInvalidatesLeaseAndClearsStagedAuthority()
            throws Exception {
        StagedPair staged = stageRealPair("pair_uncertain_failure");
        DesignerCommandSessionOrchestrator orchestrator = staged.orchestrator();
        staged.pair().coordinator().setPairTransactionForTests(request ->
                failedPairResult(
                        PairFileTransactionStatus.RECOVERY_CONFLICT,
                        1,
                        true,
                        PairFileTransactionIssueCode.ROLLBACK_MISMATCH,
                        "synthetic unknown durable outcome"));
        SaveCookie cookie = staged.pair().dataObject()
                .getCookie(SaveCookie.class);

        assertThrows(IOException.class, cookie::save);

        assertNull(staged.pair().coordinator().stagedEvidence());
        assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                staged.pair().coordinator().state().status());
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision);
        assertNotNull(staged.pair().dataObject().getCookie(SaveCookie.class));
    }

    @Test
    void stagedAuthorityChangedWhilePinningLeaseStartsNoPairIo()
            throws Exception {
        StagedPair staged = stageRealPair("pair_stale_authority");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            DesignerCommandRevision stagedRevision = orchestrator.currentRevision();
            AtomicInteger transactionCalls = new AtomicInteger();
            AtomicBoolean injectedEvent = new AtomicBoolean();
            staged.pair().coordinator().setPairTransactionForTests(request -> {
                transactionCalls.incrementAndGet();
                throw new AssertionError("stale authority must not begin pair I/O");
            });
            javax.swing.event.ChangeListener staleAuthorityListener = event -> {
                if (injectedEvent.compareAndSet(false, true)) {
                    staged.pair().coordinator().handleFileEvent(
                            new FileEvent(staged.pair().designerFile()));
                }
            };
            orchestrator.addChangeListener(staleAuthorityListener);
            try {
                assertThrows(IOException.class,
                        staged.pair().dataObject().getCookie(SaveCookie.class)::save);
            } finally {
                orchestrator.removeChangeListener(staleAuthorityListener);
            }

            assertTrue(injectedEvent.get());
            assertEquals(0, transactionCalls.get());
            assertSame(stagedRevision, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertSame(staged.evidence(),
                    staged.pair().coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                    staged.pair().coordinator().state().status());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    Files.readAllBytes(staged.pair().dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(staged.pair().designerPath()));
        }
    }

    @Test
    void durablePairCommitRetainsNewerEditWhenFinalizationFails()
            throws Exception {
        StagedPair staged = stageRealPair("post_commit_finalization_race");
        FlutterDesignerEditorSupport editor =
                staged.pair().dataObject().getEditorSupport();
        StyledDocument document = editor.getDocument();
        assertNotNull(document);
        SaveCookie cookie = staged.pair().dataObject()
                .getCookie(SaveCookie.class);
        assertNotNull(cookie);

        CountDownLatch durableOutputReached = new CountDownLatch(1);
        CountDownLatch releaseFinalization = new CountDownLatch(1);
        AtomicReference<Throwable> saveFailure = new AtomicReference<>();
        AtomicReference<String> exactNewerLiveText = new AtomicReference<>();
        String userEdit = "// newer unmanaged edit after durable pair commit\n";
        Thread saveThread = new Thread(() -> {
            try {
                cookie.save();
            } catch (Throwable failure) {
                saveFailure.set(failure);
            }
        }, "latched-designer-pair-save");
        saveThread.setDaemon(true);
        editor.setPersistenceFinalizationHook(() -> {
            durableOutputReached.countDown();
            try {
                if (!releaseFinalization.await(10, TimeUnit.SECONDS)) {
                    throw new IOException(
                            "Timed out waiting for the deterministic newer edit");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException(
                        "Interrupted while finalization was latched", interrupted);
            }
            throw new IOException("Forced guarded finalization failure");
        });

        try {
            saveThread.start();
            assertTrue(durableOutputReached.await(10, TimeUnit.SECONDS),
                    "the save never reached its post-transaction finalization edge");
            assertTrue(saveThread.isAlive(),
                    "the finalization latch must still own the save thread");
            assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                    Files.readAllBytes(staged.pair().dartPath()),
                    "Dart must already be durably committed before the newer edit");
            assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                    Files.readAllBytes(staged.pair().designerPath()),
                    ".fd must already be durably committed before the newer edit");

            exactNewerLiveText.set(onEdt(() -> {
                document.insertString(document.getLength(), userEdit, null);
                return document.getText(0, document.getLength());
            }));
        } finally {
            releaseFinalization.countDown();
            saveThread.join(TimeUnit.SECONDS.toMillis(10));
            editor.clearPersistenceFinalizationHook();
            if (saveThread.isAlive()) {
                saveThread.interrupt();
            }
        }

        assertFalse(saveThread.isAlive(),
                "the latched save must finish after finalization is released");
        assertNull(saveFailure.get(),
                "a durable pair commit remains accepted despite finalization failure");
        assertSame(document, editor.getDocument(),
                "recovery conflict must retain, not reload, the newer document");
        assertEquals(exactNewerLiveText.get(),
                onEdt(() -> document.getText(0, document.getLength())));
        assertTrue(editor.sourceModified(),
                "the newer unmanaged edit must remain dirty");
        byte[] exactExpectedLive = (new String(
                staged.prepared().prospectiveDartBytes(), StandardCharsets.UTF_8)
                + userEdit).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(exactExpectedLive,
                editor.liveSnapshot().markerBearingUtf8());
        assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));
        assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                staged.pair().coordinator().state().status());
        assertTrue(staged.pair().coordinator().state().reason().isPresent());
        assertSame(cookie,
                staged.pair().dataObject().getCookie(SaveCookie.class),
                "the sticky recovery conflict must retain the stable SaveCookie");
    }

    @Test
    void unmanagedSourceEditAfterDurablePairOutputStaysAboveSavedSemanticEdge()
            throws Exception {
        StagedPair staged = stageRealPair("post_commit_unmanaged_source");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            TestPair pair = staged.pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);
            long oldRevisionId = adjacentUndoTargetId(orchestrator);
            SaveCookie cookie = pair.dataObject().getCookie(SaveCookie.class);
            assertNotNull(cookie);

            CountDownLatch durableOutputReached = new CountDownLatch(1);
            CountDownLatch releaseFinalization = new CountDownLatch(1);
            AtomicReference<Throwable> saveFailure = new AtomicReference<>();
            String userEdit = "// unmanaged Source S2 during pair finalization\n";
            Thread saveThread = new Thread(() -> {
                try {
                    cookie.save();
                } catch (Throwable failure) {
                    saveFailure.set(failure);
                }
            }, "latched-unmanaged-pair-save");
            saveThread.setDaemon(true);
            editor.setPersistenceFinalizationHook(() -> {
                durableOutputReached.countDown();
                try {
                    if (!releaseFinalization.await(10, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "Timed out waiting for the unmanaged Source edit");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Interrupted while finalization was latched", interrupted);
                }
            });

            byte[] sourceS2;
            try {
                saveThread.start();
                assertTrue(durableOutputReached.await(10, TimeUnit.SECONDS));
                sourceS2 = onEdt(() -> {
                    document.insertString(document.getLength(), userEdit, null);
                    return editor.liveSnapshot().markerBearingUtf8();
                });
            } finally {
                releaseFinalization.countDown();
                saveThread.join(TimeUnit.SECONDS.toMillis(10));
                editor.clearPersistenceFinalizationHook();
                if (saveThread.isAlive()) {
                    saveThread.interrupt();
                }
            }

            assertFalse(saveThread.isAlive());
            assertNull(saveFailure.get());
            DesignerCommandRevision saved = orchestrator.currentRevision();
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    saved.persistenceKind());
            assertFalse(orchestrator.dirty());
            FlutterDesignerDocumentState.Current savedCurrent =
                    awaitCurrentWithFd(
                            controller, staged.prepared().prospectiveFdBytes());
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    pair.coordinator().state().status());
            assertTrue(combined.canUndo());

            onEdt(() -> {
                combined.undo();
                return null;
            });
            assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);
            assertEquals(oldRevisionId,
                    orchestrator.currentRevision().revisionId());
            PairSaveCoordinator.StagedPairProofSnapshot proof =
                    pair.coordinator().stagedProofSnapshot();
            assertNotNull(proof);
            assertEquals(PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    proof.kind());
            assertSame(savedCurrent, proof.loadedCurrentIdentity());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
        }
    }

    @Test
    void controllerGenerationChangeBeforeCommittedAdoptionFailsClosed()
            throws Exception {
        StagedPair staged = stageRealPair("post_commit_controller_generation");
        DesignerCommandSessionOrchestrator orchestrator = staged.orchestrator();
        FlutterDesignerDocumentController controller =
                staged.pair().dataObject().getDocumentController();
        FlutterDesignerEditorSupport editor =
                staged.pair().dataObject().getEditorSupport();
        editor.setPersistenceFinalizationHook(controller::reload);
        try {
            staged.pair().dataObject().getCookie(SaveCookie.class).save();
        } finally {
            editor.clearPersistenceFinalizationHook();
        }

        assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                staged.pair().coordinator().state().status());
        assertNull(staged.pair().coordinator().stagedProofSnapshot());
        assertEquals(0,
                staged.pair().coordinator().unsavedPairHistoryEdgeCount());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision,
                "a stale controller ticket must not leave a saved command cursor behind");
        assertArrayEquals(staged.prepared().prospectiveDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()));
        assertArrayEquals(staged.prepared().prospectiveFdBytes(),
                Files.readAllBytes(staged.pair().designerPath()));
        orchestrator.close();
    }

    @Test
    void staleFdBaselineRejectsTheTransactionAndRetainsSaveCookie()
            throws Exception {
        StagedPair staged = stageRealPair("stale_pair");
        SaveCookie cookie = staged.pair().dataObject()
                .getCookie(SaveCookie.class);
        assertNotNull(cookie);
        byte[] staleFd = Arrays.copyOf(
                staged.prepared().baselineFdBytes(),
                staged.prepared().baselineFdBytes().length + 1);
        staleFd[staleFd.length - 1] = ' ';

        // Deliberately bypass FileObject notification so the final lock-time
        // baseline check, rather than the early event gate, detects the drift.
        Files.write(staged.pair().designerPath(), staleFd);
        assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                staged.pair().coordinator().state().status());

        assertThrows(IOException.class, cookie::save);

        assertArrayEquals(staged.prepared().baselineDartBytes(),
                Files.readAllBytes(staged.pair().dartPath()),
                "a stale .fd baseline must cause zero Dart writes");
        assertArrayEquals(staleFd,
                Files.readAllBytes(staged.pair().designerPath()));
        assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                staged.pair().coordinator().state().status());
        assertSame(cookie, staged.pair().dataObject().getCookie(SaveCookie.class));
        assertTrue(staged.pair().dataObject().getEditorSupport().sourceModified());
    }

    @Test
    void successfulFirstCommandPublishesCommandThenPairFromJointC1State()
            throws Exception {
        FirstCommandPublicationProbe probe = new FirstCommandPublicationProbe(
                new ArrayList<>(), new AtomicBoolean(), new AtomicBoolean());
        StagedPair c1 = stageRealPair("first_command_publication", probe);
        try (DesignerCommandSessionOrchestrator ignored = c1.orchestrator()) {
            assertEquals(List.of("command", "pair"), probe.callbacks());
            assertTrue(probe.commandSawJointState().get(),
                    "the command callback must observe both exact C1 authorities");
            assertTrue(probe.pairSawJointState().get(),
                    "the pair callback must observe both exact C1 authorities");
        }
    }

    @Test
    void activeFirstCommandClaimRejectsIndependentResolutionAndReturnsToB()
            throws Exception {
        assertTrue(stageRealPair(
                "first_command_claim_fence",
                true,
                BeforeApplyAction.CLAIM_FENCE).isEmpty());
    }

    @Test
    void closingUnappliedFirstCommandAbortsExactBAndReleasesBusyFence()
            throws Exception {
        assertTrue(stageRealPair(
                "first_command_close_exact_b",
                true,
                BeforeApplyAction.CLOSE_EXACT_B).isEmpty());
    }

    @Test
    void rejectedEvidenceRestoresExactLiveDartAndCleanState() throws Exception {
        stageRealPair("rejected_pair", false);
    }

    @Test
    void failedApplyBeforeLeasePublicationReleasesPreparationAndSourceSave()
            throws Exception {
        assertTrue(stageRealPair(
                "failed_apply", true, BeforeApplyAction.FAIL_APPLY).isEmpty());
    }

    @Test
    void closingUnappliedLeaseRetainsIndependentSourceEditAndSaveCookie()
            throws Exception {
        assertTrue(stageRealPair(
                "closed_before_apply", true, BeforeApplyAction.CLOSE).isEmpty());
    }

    @Test
    void externalEventBeforeReservationCannotBeAdoptedAsTheCurrentEpoch()
            throws Exception {
        assertTrue(stageRealPair(
                "external_before_reservation",
                true,
                BeforeApplyAction.EXTERNAL_EVENT_BEFORE_RESERVATION).isEmpty());
    }

    @Test
    void eventDuringAppliedCandidateRestoresExactBytesButStaysFailClosedDirty()
            throws Exception {
        assertTrue(stageRealPair(
                "external_during_apply",
                true,
                BeforeApplyAction.EXTERNAL_EVENT_DURING_APPLY).isEmpty());
    }

    @Test
    void externalEventAtArmedForwardAdmissionAdoptsCommandButPoisonsPairAuthority()
            throws Exception {
        assertTrue(stageRealPair(
                "external_while_forward_armed",
                true,
                BeforeApplyAction.EXTERNAL_EVENT_WHILE_FORWARD_ARMED)
                .isEmpty());
    }

    @Test
    void ownerCloseAtInitialArmedAdmissionClosesBothAuthoritiesAfterExactAck()
            throws Exception {
        assertTrue(stageRealPair(
                "owner_close_while_initial_forward_armed",
                true,
                BeforeApplyAction.OWNER_CLOSE_WHILE_FORWARD_ARMED)
                .isEmpty());
    }

    @Test
    void replacesExactStagedC1WithAnalyzedC2AndAdoptsCommandCursor()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_c1_c2");
        SaveCookie cookie = c1.pair().dataObject().getCookie(SaveCookie.class);
        assertNotNull(cookie);
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var command = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")));
            var pending = command.lease().orElseThrow();
            var c2Revision = pending.candidateRevision();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                assertEquals(PairSaveCoordinatorStatus.PREPARING_REPLACEMENT,
                        c1.pair().coordinator().state().status());
                assertThrows(IOException.class,
                        c1.pair().dataObject().getEditorSupport()::saveDocument,
                        "Save must remain blocked for the whole replacement lease");
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                PairSaveEvidenceResult result =
                        replacement.acceptAnalysisAndReplace(
                                analysisTicket.ticket(),
                                passingAnalysis(
                                        analysisTicket.ticket(),
                                        analysisTicket.frameworkReal(),
                                        0));

                assertTrue(result.ready(), () -> result.diagnostics().toString());
                PairSaveEvidence c2 = ((PairSaveEvidenceResult.Ready) result)
                        .evidence();
                assertSame(c2Revision, orchestrator.currentRevision());
                assertSame(c2, c1.pair().coordinator().stagedEvidence());
                assertSame(c2Revision.preparedPair().orElseThrow(),
                        c2.preparedPairIdentity());
                assertArrayEquals(c2Revision.dartCandidateBytes(),
                        c1.pair().dataObject().getEditorSupport()
                                .liveSnapshot().markerBearingUtf8());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        c1.pair().coordinator().state().status());
                assertSame(cookie,
                        c1.pair().dataObject().getCookie(SaveCookie.class));
                assertArrayEquals(c1.prepared().baselineDartBytes(),
                        Files.readAllBytes(c1.pair().dartPath()));
                assertArrayEquals(c1.prepared().baselineFdBytes(),
                        Files.readAllBytes(c1.pair().designerPath()));
            }
        }
    }

    @Test
    void ownerCloseAtReplacementArmedAdmissionClosesBothAuthoritiesAfterExactAck()
            throws Exception {
        StagedPair c1 = stageRealPair(
                "owner_close_while_replacement_forward_armed");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        FlutterDesignerEditorSupport editor =
                c1.pair().dataObject().getEditorSupport();
        DesignerCommandSessionOrchestrator orchestrator = c1.orchestrator();
        var pending = orchestrator.beginCommand(new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue("C2")))
                .lease().orElseThrow();
        DesignerCommandRevision exactC1 = orchestrator.currentRevision();
        DesignerCommandRevision exactC2 = pending.candidateRevision();
        AtomicBoolean armedCloseRequested = new AtomicBoolean();
        try (pending;
                PairSaveCoordinator.PairReplacement replacement =
                        c1.pair().coordinator().beginStagedReplacement(
                                c1.evidence(), pending)) {
            ReplacementTicket analysisTicket = replacementTicket(
                    c1, replacement);
            c1.pair().coordinator().setForwardAdmissionHookForTests(
                    (exactLease, replacementAdmission) -> {
                        assertSame(pending, exactLease);
                        assertTrue(replacementAdmission);
                        orchestrator.close();
                        assertSame(exactC1, orchestrator.currentRevision(),
                                "close must defer until exact C2 adoption");
                        assertTrue(combined.designerSessionActive(),
                                "the claimed C2 lease must retain its binding until ACK effects");
                        armedCloseRequested.set(true);
                    });
            PairSaveEvidenceResult result;
            try {
                result = replacement.acceptAnalysisAndReplace(
                        analysisTicket.ticket(),
                        passingAnalysis(
                                analysisTicket.ticket(),
                                analysisTicket.frameworkReal(),
                                0));
            } finally {
                c1.pair().coordinator().setForwardAdmissionHookForTests(null);
            }

            assertTrue(armedCloseRequested.get());
            assertTrue(result.ready(),
                    "native ACK remains authoritative when owner close was deferred");
            assertSame(exactC2, retainedClosedCommandRevision(orchestrator),
                    "the close-aware ACK must adopt exact C2 before closing its owner");
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision);
            assertFalse(pending.ownsExactActiveTransition());
            assertFalse(combined.designerSessionActive(),
                    "deferred command effects must release the Combined binding");
            assertArrayEquals(exactC2.dartCandidateBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertTrue(editor.nativeUndoRedoManagerForCombinedBridge().canUndo(),
                    "the exact native C1-to-C2 semantic edge remains recorded");
            assertNull(c1.pair().coordinator().stagedEvidence());
            assertEquals(0,
                    c1.pair().coordinator().unsavedPairHistoryEdgeCount());
            PairSaveCoordinatorSnapshot snapshot =
                    c1.pair().coordinator().state();
            assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    snapshot.status());
            assertEquals(
                    "The Designer command owner closed during native forward admission",
                    snapshot.reason().orElseThrow());
            assertNotNull(c1.pair().dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(c1.prepared().baselineDartBytes(),
                    Files.readAllBytes(c1.pair().dartPath()));
            assertArrayEquals(c1.prepared().baselineFdBytes(),
                    Files.readAllBytes(c1.pair().designerPath()));
        } finally {
            orchestrator.close();
        }
    }

    @Test
    void successfulReplacementPublishesCommandThenPairFromJointC2State()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_publication_order");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            var c2Revision = pending.candidateRevision();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                List<String> callbacks = new ArrayList<>();
                AtomicBoolean commandSawJointC2 = new AtomicBoolean();
                AtomicBoolean pairSawJointC2 = new AtomicBoolean();
                javax.swing.event.ChangeListener commandListener = event -> {
                    callbacks.add("command");
                    PairSaveEvidence staged =
                            c1.pair().coordinator().stagedEvidence();
                    commandSawJointC2.set(
                            orchestrator.currentRevision() == c2Revision
                            && staged != null
                            && staged.preparedPairIdentity()
                                == c2Revision.preparedPair().orElseThrow());
                };
                java.beans.PropertyChangeListener pairListener = event -> {
                    if (event.getNewValue()
                            instanceof PairSaveCoordinatorSnapshot snapshot
                            && snapshot.status()
                                == PairSaveCoordinatorStatus.STAGED_PAIR) {
                        callbacks.add("pair");
                        PairSaveEvidence staged =
                                c1.pair().coordinator().stagedEvidence();
                        pairSawJointC2.set(
                                orchestrator.currentRevision() == c2Revision
                                && staged != null
                                && staged.preparedPairIdentity()
                                    == c2Revision.preparedPair().orElseThrow());
                    }
                };
                orchestrator.addChangeListener(commandListener);
                c1.pair().coordinator().addPropertyChangeListener(pairListener);
                try {
                    PairSaveEvidenceResult result =
                            replacement.acceptAnalysisAndReplace(
                                    analysisTicket.ticket(),
                                    passingAnalysis(
                                            analysisTicket.ticket(),
                                            analysisTicket.frameworkReal(),
                                            0));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                } finally {
                    orchestrator.removeChangeListener(commandListener);
                    c1.pair().coordinator()
                            .removePropertyChangeListener(pairListener);
                }

                assertEquals(List.of("command", "pair"), callbacks);
                assertTrue(commandSawJointC2.get());
                assertTrue(pairSawJointC2.get());
            }
        }
    }

    @Test
    void rejectedC2KeepsExactC1EvidenceCursorAndSaveCookie()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_rejected");
        SaveCookie cookie = c1.pair().dataObject().getCookie(SaveCookie.class);
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                PairSaveEvidenceResult result =
                        replacement.acceptAnalysisAndReplace(
                                analysisTicket.ticket(),
                                passingAnalysis(
                                        analysisTicket.ticket(),
                                        analysisTicket.frameworkReal(),
                                        1));

                assertFalse(result.ready());
                assertSame(exactC1, orchestrator.currentRevision());
                assertSame(c1.evidence(),
                        c1.pair().coordinator().stagedEvidence());
                assertTrue(c1.liveCandidate().sameEvidence(
                        c1.pair().dataObject().getEditorSupport().liveSnapshot()));
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        c1.pair().coordinator().state().status());
                assertSame(cookie,
                        c1.pair().dataObject().getCookie(SaveCookie.class));
                assertTrue(combined.canUndo());
            }
        }
    }

    @Test
    void failedC2ApplyRebindsFreshExactC1EvidenceAndAbortsCommand()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_rollback_rebind");
        SaveCookie cookie = c1.pair().dataObject().getCookie(SaveCookie.class);
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                StyledDocument document = c1.pair().dataObject()
                        .getEditorSupport().getDocument();
                assertNotNull(document);
                AtomicBoolean failedOnce = new AtomicBoolean();
                DocumentListener failingListener = new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent event) {
                        failOnce();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event) {
                        failOnce();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event) {
                        failOnce();
                    }

                    private void failOnce() {
                        if (failedOnce.compareAndSet(false, true)) {
                            throw new AssertionError(
                                    "synthetic post-reservation document failure");
                        }
                    }
                };
                document.addDocumentListener(failingListener);
                try {
                    assertThrows(IOException.class,
                            () -> replacement.acceptAnalysisAndReplace(
                                    analysisTicket.ticket(),
                                    passingAnalysis(
                                            analysisTicket.ticket(),
                                            analysisTicket.frameworkReal(),
                                            0)));
                } finally {
                    document.removeDocumentListener(failingListener);
                }

                assertTrue(failedOnce.get());
                PairSaveEvidence rebound =
                        c1.pair().coordinator().stagedEvidence();
                assertNotNull(rebound);
                assertNotSame(c1.evidence(), rebound);
                assertSame(c1.evidence().analyzedCandidateIdentity(),
                        rebound.analyzedCandidateIdentity());
                assertTrue(rebound.documentVersion()
                        > c1.evidence().documentVersion());
                assertArrayEquals(c1.evidence().candidateDartBytes(),
                        rebound.liveCandidateIdentity().markerBearingUtf8());
                assertSame(exactC1, orchestrator.currentRevision());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        c1.pair().coordinator().state().status());
                assertSame(cookie,
                        c1.pair().dataObject().getCookie(SaveCookie.class));
                assertArrayEquals(c1.prepared().baselineDartBytes(),
                        Files.readAllBytes(c1.pair().dartPath()));
                assertArrayEquals(c1.prepared().baselineFdBytes(),
                        Files.readAllBytes(c1.pair().designerPath()));
            }
        }
    }

    @Test
    void chainsC1ToC2ToC3AgainstOneExactDurableBaseline()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_chain");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            PairSaveEvidence c2 = replaceOnce(
                    c1, orchestrator, c1.evidence(), "C2");
            PairSaveEvidence c3 = replaceOnce(
                    c1, orchestrator, c2, "C3");

            assertSame(c1.current().decoded().original(),
                    c2.preparedPairIdentity().baselineFd());
            assertSame(c1.current().decoded().original(),
                    c3.preparedPairIdentity().baselineFd());
            assertSame(c1.current().threeWayIntegrity().orElseThrow(),
                    c2.preparedPairIdentity().dartTransition().baseline());
            assertSame(c1.current().threeWayIntegrity().orElseThrow(),
                    c3.preparedPairIdentity().dartTransition().baseline());
            assertSame(c3, c1.pair().coordinator().stagedEvidence());
            assertSame(c3.preparedPairIdentity(),
                    orchestrator.currentRevision().preparedPair().orElseThrow());
            assertArrayEquals(c3.candidateDartBytes(),
                    c1.pair().dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
            assertArrayEquals(c1.prepared().baselineDartBytes(),
                    Files.readAllBytes(c1.pair().dartPath()));
            assertArrayEquals(c1.prepared().baselineFdBytes(),
                    Files.readAllBytes(c1.pair().designerPath()));
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    c1.pair().coordinator().state().status());
        }
    }

    @Test
    void unsavedC2ToC1ToC2RebindsExactAnalyzerSeedsWithoutAnalyzerRerun()
            throws Exception {
        StagedPair staged = stageRealPair("history_c2_c1_c2");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            DesignerCommandRevision c1Revision = orchestrator.currentRevision();
            PairSaveEvidence c1Seed = staged.evidence();
            PairSaveEvidence c2Seed = replaceOnce(
                    staged, orchestrator, c1Seed, "C2");
            DesignerCommandRevision c2Revision = orchestrator.currentRevision();

            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);

            PairSaveEvidence reboundC1 =
                    staged.pair().coordinator().stagedEvidence();
            assertNotNull(reboundC1);
            assertSame(c1Seed.analyzedCandidateIdentity(),
                    reboundC1.analyzedCandidateIdentity());
            assertSame(c1Seed.analysisIdentity(), reboundC1.analysisIdentity());
            assertSame(c1Revision, orchestrator.currentRevision());
            assertArrayEquals(c1Seed.candidateDartBytes(),
                    reboundC1.liveCandidateIdentity().markerBearingUtf8());
            assertTrue(reboundC1.documentVersion() > c1Seed.documentVersion());

            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.REDO);

            PairSaveEvidence reboundC2 =
                    staged.pair().coordinator().stagedEvidence();
            assertNotNull(reboundC2);
            assertSame(c2Seed.analyzedCandidateIdentity(),
                    reboundC2.analyzedCandidateIdentity());
            assertSame(c2Seed.analysisIdentity(), reboundC2.analysisIdentity());
            assertSame(c2Revision, orchestrator.currentRevision());
            assertArrayEquals(c2Seed.candidateDartBytes(),
                    reboundC2.liveCandidateIdentity().markerBearingUtf8());
            assertTrue(reboundC2.documentVersion() > c2Seed.documentVersion());
        }
    }

    @Test
    void initialUnsavedC1ToBaselineToC1RetainsExactPairAuthority()
            throws Exception {
        StagedPair staged = stageRealPair("history_c1_b_c1");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            DesignerCommandRevision c1Revision = orchestrator.currentRevision();
            long baselineRevisionId = adjacentUndoTargetId(orchestrator);
            PairSaveEvidence c1Seed = staged.evidence();

            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);

            assertNull(staged.pair().coordinator().stagedEvidence());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());
            assertEquals(baselineRevisionId,
                    orchestrator.currentRevision().revisionId());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    staged.pair().coordinator().state().status());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    staged.pair().dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());

            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.REDO);

            PairSaveEvidence reboundC1 =
                    staged.pair().coordinator().stagedEvidence();
            assertNotNull(reboundC1);
            assertSame(c1Seed.analyzedCandidateIdentity(),
                    reboundC1.analyzedCandidateIdentity());
            assertSame(c1Revision, orchestrator.currentRevision());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    staged.pair().coordinator().state().status());
        }
    }

    @Test
    void successfulAndAbortedReplayReleaseNativeContextForOrdinarySourceEdits()
            throws Exception {
        StagedPair successful = stageRealPair("history_context_success");
        try (DesignerCommandSessionOrchestrator orchestrator =
                successful.orchestrator()) {
            replayUnsavedPairHistory(
                    successful.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);
            FlutterDesignerEditorSupport editor = successful.pair()
                    .dataObject().getEditorSupport();
            assertFalse(editor.sourceModified());
            onEdt(() -> {
                StyledDocument document = editor.getDocument();
                document.insertString(
                        document.getLength(),
                        "// ordinary source after successful replay\n",
                        null);
                return null;
            });
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    successful.pair().coordinator().state().status(),
                    "a successful semantic replay must not retain its internal Source classification");
        }

        StagedPair aborted = stageRealPair("history_context_abort");
        try (DesignerCommandSessionOrchestrator orchestrator =
                aborted.orchestrator()) {
            DesignerCommandRevision c1 = orchestrator.currentRevision();
            long baseline = adjacentUndoTargetId(orchestrator);
            DesignerSemanticUndoableEdit.PreparedReplay prepared = onEdt(() ->
                    aborted.pair().coordinator()
                            .unsavedPairHistoryReplayController().prepare(
                                    DesignerSemanticUndoableEdit.Direction.UNDO,
                                    c1.revisionId(),
                                    baseline));
            onEdt(() -> {
                prepared.abort();
                return null;
            });
            // Direct controller tests publish their deferred pair effects on
            // the next EDT tail because no Combined action barrier owns them.
            onEdt(() -> null);
            assertSame(c1, orchestrator.currentRevision());
            PairSaveEvidence rebound =
                    aborted.pair().coordinator().stagedEvidence();
            assertNotNull(rebound);
            assertSame(aborted.evidence().analyzedCandidateIdentity(),
                    rebound.analyzedCandidateIdentity(),
                    "verified abort must rebind the same analyzer seed to the fresh live identity");

            replayUnsavedPairHistory(
                    aborted.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);
            FlutterDesignerEditorSupport editor = aborted.pair()
                    .dataObject().getEditorSupport();
            assertFalse(editor.sourceModified());
            onEdt(() -> {
                StyledDocument document = editor.getDocument();
                document.insertString(
                        document.getLength(),
                        "// ordinary source after aborted replay\n",
                        null);
                return null;
            });
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    aborted.pair().coordinator().state().status(),
                    "a verified abort must close its native replay context before the next Source edit");
        }
    }

    @Test
    void failedFreshRecoveryContextIsOwnedAndClosedByInvalidation()
            throws Exception {
        StagedPair staged = stageRealPair("history_recovery_context_failure");
        DesignerCommandSessionOrchestrator orchestrator = staged.orchestrator();
        DesignerCommandRevision c1 = orchestrator.currentRevision();
        long baseline = adjacentUndoTargetId(orchestrator);
        DesignerSemanticUndoableEdit.PreparedReplay prepared = onEdt(() ->
                staged.pair().coordinator()
                        .unsavedPairHistoryReplayController().prepare(
                                DesignerSemanticUndoableEdit.Direction.UNDO,
                                c1.revisionId(),
                                baseline));
        FlutterDesignerEditorSupport editor = staged.pair()
                .dataObject().getEditorSupport();

        onEdt(() -> {
            detachPreparedReplayNativeContextForRecoveryTest(prepared);
            Field providerField = FlutterDesignerEditorSupport.class
                    .getDeclaredField("guardedProvider");
            providerField.setAccessible(true);
            DartGuardedSectionsProvider provider =
                    (DartGuardedSectionsProvider) providerField.get(editor);
            LiveDartDocumentSnapshot current = editor.liveSnapshot();
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(
                            current.documentIdentity())) {
                LiveDartDocumentBridge.restoreManagedRegions(
                        provider,
                        current,
                        staged.baselineLive(),
                        restored -> { });
            }

            prepared.abort();

            // abort() opened a fresh recovery token, verification failed, and
            // invalidate must have closed that exact token synchronously.
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(
                            editor.liveSnapshot().documentIdentity())) {
                // Availability is the exact EDT-local lifetime assertion.
            }
            return null;
        });
        onEdt(() -> null);

        assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                staged.pair().coordinator().state().status());
        assertNull(staged.pair().coordinator().stagedEvidence());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision);
        orchestrator.close();
    }

    @Test
    void wrongHistoryEdgeClaimCurrentAndEpochAllFailBeforeRawMove()
            throws Exception {
        StagedPair wrongEdge = stageRealPair("history_wrong_edge");
        try (DesignerCommandSessionOrchestrator orchestrator =
                wrongEdge.orchestrator()) {
            DesignerCommandRevision c1 = orchestrator.currentRevision();
            long baseline = adjacentUndoTargetId(orchestrator);
            byte[] before = wrongEdge.pair().dataObject().getEditorSupport()
                    .liveSnapshot().markerBearingUtf8();
            var history = wrongEdge.pair().coordinator()
                    .unsavedPairHistoryReplayController();
            assertThrows(IOException.class, () -> onEdt(() -> history.prepare(
                    DesignerSemanticUndoableEdit.Direction.UNDO,
                    c1.revisionId(),
                    Math.addExact(baseline, 100_000L))));
            assertThrows(IOException.class, () -> onEdt(() -> history.prepare(
                    DesignerSemanticUndoableEdit.Direction.REDO,
                    baseline,
                    c1.revisionId())));
            assertArrayEquals(before,
                    wrongEdge.pair().dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
            assertSame(c1, orchestrator.currentRevision());
        }

        StagedPair wrongClaim = stageRealPair("history_wrong_claim");
        try (DesignerCommandSessionOrchestrator orchestrator =
                wrongClaim.orchestrator()) {
            DesignerCommandRevision c1 = orchestrator.currentRevision();
            long baseline = adjacentUndoTargetId(orchestrator);
            byte[] before = wrongClaim.evidence().candidateDartBytes();
            wrongClaim.pair().coordinator()
                    .setHistoryPreparationHookForTests(
                            (lease, claim) -> lease.claimForTransition(
                                    new Object()));
            try {
                assertThrows(IOException.class, () -> onEdt(() ->
                        wrongClaim.pair().coordinator()
                                .unsavedPairHistoryReplayController().prepare(
                                        DesignerSemanticUndoableEdit.Direction.UNDO,
                                        c1.revisionId(), baseline)));
            } finally {
                wrongClaim.pair().coordinator()
                        .setHistoryPreparationHookForTests(null);
            }
            assertArrayEquals(before,
                    wrongClaim.pair().dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
            assertSame(c1, orchestrator.currentRevision());
            assertSame(wrongClaim.evidence(),
                    wrongClaim.pair().coordinator().stagedEvidence());
        }

        StagedPair wrongEpoch = stageRealPair("history_wrong_epoch");
        DesignerCommandSessionOrchestrator epochOrchestrator =
                wrongEpoch.orchestrator();
        DesignerCommandRevision c1 = epochOrchestrator.currentRevision();
        long baseline = adjacentUndoTargetId(epochOrchestrator);
        byte[] before = wrongEpoch.evidence().candidateDartBytes();
        wrongEpoch.pair().coordinator().setHistoryPreparationHookForTests(
                (lease, claim) -> wrongEpoch.pair().coordinator()
                        .handleFileEvent(new FileEvent(
                                wrongEpoch.pair().dartFile())));
        try {
            assertThrows(IOException.class, () -> onEdt(() ->
                    wrongEpoch.pair().coordinator()
                            .unsavedPairHistoryReplayController().prepare(
                                    DesignerSemanticUndoableEdit.Direction.UNDO,
                                    c1.revisionId(), baseline)));
        } finally {
            wrongEpoch.pair().coordinator()
                    .setHistoryPreparationHookForTests(null);
        }
        assertArrayEquals(before,
                wrongEpoch.pair().dataObject().getEditorSupport()
                        .liveSnapshot().markerBearingUtf8());
        assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                wrongEpoch.pair().coordinator().state().status());
        assertNull(wrongEpoch.pair().coordinator().stagedEvidence());
        assertThrows(IllegalStateException.class,
                epochOrchestrator::currentRevision);
        epochOrchestrator.close();
    }

    @Test
    void releaseAfterBranchTruncationRemovesOnlyExactDeadEdge()
            throws Exception {
        StagedPair staged = stageRealPair("history_branch_release");
        try (DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator()) {
            DesignerCommandRevision c1Revision = orchestrator.currentRevision();
            PairSaveEvidence c2 = replaceOnce(
                    staged, orchestrator, staged.evidence(), "C2");
            DesignerCommandRevision c2Revision = orchestrator.currentRevision();
            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);

            PairSaveEvidence c1Rebound =
                    staged.pair().coordinator().stagedEvidence();
            PairSaveEvidence c3 = replaceOnce(
                    staged, orchestrator, c1Rebound, "C3");
            DesignerCommandRevision c3Revision = orchestrator.currentRevision();
            assertEquals(2,
                    staged.pair().coordinator().unsavedPairHistoryEdgeCount());
            assertFalse(staged.pair().dataObject().getCombinedUndoRedo()
                    .canRedo(),
                    "the native branch must truncate the exact C1-to-C2 edge");
            assertSame(c3, staged.pair().coordinator().stagedEvidence());
            assertSame(c3Revision, orchestrator.currentRevision());
            assertNotSame(c2, c3);

            replayUnsavedPairHistory(
                    staged.pair(),
                    DesignerSemanticUndoableEdit.Direction.UNDO);
            assertSame(c1Revision, orchestrator.currentRevision());
            assertSame(c1Rebound.analyzedCandidateIdentity(),
                    staged.pair().coordinator().stagedEvidence()
                            .analyzedCandidateIdentity());
        }
    }

    @Test
    void externalEventDuringReplacementPublishesOnlyAfterPairAndCommandInvalidate()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_external_event");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                StyledDocument document = c1.pair().dataObject()
                        .getEditorSupport().getDocument();
                assertNotNull(document);
                AtomicBoolean eventFired = new AtomicBoolean();
                AtomicBoolean callbackFired = new AtomicBoolean();
                AtomicBoolean callbackSawClearedPair = new AtomicBoolean();
                AtomicBoolean callbackSawClosedCommand = new AtomicBoolean();
                java.beans.PropertyChangeListener stateListener = event -> {
                    if (event.getNewValue()
                            instanceof PairSaveCoordinatorSnapshot snapshot
                            && snapshot.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            && callbackFired.compareAndSet(false, true)) {
                        callbackSawClearedPair.set(
                                c1.pair().coordinator().stagedEvidence() == null);
                        try {
                            orchestrator.currentRevision();
                        } catch (IllegalStateException closed) {
                            callbackSawClosedCommand.set(
                                    !combined.designerSessionActive());
                        }
                    }
                };
                DocumentListener fileEventListener = new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent event) {
                        fire();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event) {
                        fire();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event) {
                        fire();
                    }

                    private void fire() {
                        if (eventFired.compareAndSet(false, true)) {
                            assertTrue(c1.pair().coordinator().handleFileEvent(
                                    new FileEvent(c1.pair().designerFile())));
                        }
                    }
                };
                c1.pair().coordinator().addPropertyChangeListener(stateListener);
                document.addDocumentListener(fileEventListener);
                try {
                    assertThrows(IOException.class,
                            () -> replacement.acceptAnalysisAndReplace(
                                    analysisTicket.ticket(),
                                    passingAnalysis(
                                            analysisTicket.ticket(),
                                            analysisTicket.frameworkReal(),
                                            0)));
                } finally {
                    document.removeDocumentListener(fileEventListener);
                    c1.pair().coordinator()
                            .removePropertyChangeListener(stateListener);
                }

                assertTrue(eventFired.get());
                assertTrue(callbackFired.get());
                assertTrue(callbackSawClearedPair.get(),
                        "external-conflict callback must see staged authority cleared");
                assertTrue(callbackSawClosedCommand.get(),
                        "external-conflict callback must see the command session invalidated");
                assertNull(c1.pair().coordinator().stagedEvidence());
                assertFalse(combined.designerSessionActive());
                assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        c1.pair().coordinator().state().status());
                assertArrayEquals(c1.evidence().candidateDartBytes(),
                        c1.pair().dataObject().getEditorSupport()
                                .liveSnapshot().markerBearingUtf8());
            }
        }
    }

    @Test
    void activeReplacementClaimRejectsIndependentPendingResolution()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_claim_fence");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                assertThrows(IllegalStateException.class, pending::adoptStaged);
                assertThrows(IllegalStateException.class, pending::abort);
                assertThrows(IllegalStateException.class, pending::invalidate);
                assertThrows(IllegalStateException.class, pending::close);
                assertSame(exactC1, orchestrator.currentRevision());
                assertSame(c1.evidence(),
                        c1.pair().coordinator().stagedEvidence());
                assertEquals(PairSaveCoordinatorStatus.PREPARING_REPLACEMENT,
                        c1.pair().coordinator().state().status());
            }
            assertSame(exactC1, orchestrator.currentRevision());
            assertSame(c1.evidence(),
                    c1.pair().coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    c1.pair().coordinator().state().status());
        }
    }

    @Test
    void externalEventDuringReplacementPreparationPublishesTerminalOutcome()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_external_prepare");
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                AtomicBoolean callbackSawTerminal = new AtomicBoolean();
                java.beans.PropertyChangeListener listener = event -> {
                    if (event.getNewValue()
                            instanceof PairSaveCoordinatorSnapshot snapshot
                            && snapshot.status()
                                == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT) {
                        boolean commandClosed;
                        try {
                            orchestrator.currentRevision();
                            commandClosed = false;
                        } catch (IllegalStateException closed) {
                            commandClosed = true;
                        }
                        callbackSawTerminal.set(
                                commandClosed
                                && !combined.designerSessionActive()
                                && c1.pair().coordinator()
                                        .stagedEvidence() == null);
                    }
                };
                c1.pair().coordinator().addPropertyChangeListener(listener);
                try {
                    assertTrue(c1.pair().coordinator().handleFileEvent(
                            new FileEvent(c1.pair().designerFile())));
                } finally {
                    c1.pair().coordinator()
                            .removePropertyChangeListener(listener);
                }

                assertTrue(callbackSawTerminal.get());
                assertThrows(IOException.class,
                        () -> replacement.prepareAnalysis(
                                c1.pair().dartPath().getParent().getParent(),
                                DartCandidateWarningPolicy.ALLOW));
                assertNull(c1.pair().coordinator().stagedEvidence());
                assertFalse(combined.designerSessionActive());
                assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        c1.pair().coordinator().state().status());
                assertArrayEquals(c1.evidence().candidateDartBytes(),
                        c1.pair().dataObject().getEditorSupport()
                                .liveSnapshot().markerBearingUtf8());
            }
        }
    }

    @Test
    void userSourceEditBeforeReplacementApplyIsPreservedAndInvalidatesC1C2()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_source_race");
        SaveCookie cookie = c1.pair().dataObject().getCookie(SaveCookie.class);
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending;
                    PairSaveCoordinator.PairReplacement replacement =
                            c1.pair().coordinator().beginStagedReplacement(
                                    c1.evidence(), pending)) {
                ReplacementTicket analysisTicket = replacementTicket(c1, replacement);
                StyledDocument document = c1.pair().dataObject()
                        .getEditorSupport().getDocument();
                assertNotNull(document);
                String userEdit = "// user edit between analysis and apply\n";
                onEdt(() -> {
                    document.insertString(document.getLength(), userEdit, null);
                    return null;
                });

                assertThrows(IOException.class,
                        () -> replacement.acceptAnalysisAndReplace(
                                analysisTicket.ticket(),
                                passingAnalysis(
                                        analysisTicket.ticket(),
                                        analysisTicket.frameworkReal(),
                                        0)));

                assertTrue(document.getText(0, document.getLength())
                        .endsWith(userEdit));
                assertNull(c1.pair().coordinator().stagedEvidence());
                assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        c1.pair().coordinator().state().status());
                assertThrows(IllegalStateException.class,
                        orchestrator::currentRevision);
                assertFalse(combined.designerSessionActive());
                assertSame(cookie,
                        c1.pair().dataObject().getCookie(SaveCookie.class));
                assertArrayEquals(c1.prepared().baselineDartBytes(),
                        Files.readAllBytes(c1.pair().dartPath()));
                assertArrayEquals(c1.prepared().baselineFdBytes(),
                        Files.readAllBytes(c1.pair().designerPath()));
            }
        }
    }

    @Test
    void equivalentButForeignC1IdentityIsRejectedAndClaimIsReleasedToC1()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_foreign_identity");
        DesignerCommandSession foreignBaseline = DesignerCommandSession
                .openVerified(
                        c1.current().decoded().original(),
                        c1.prepared().baselineDartBytes(),
                        c1.current().catalog(),
                        c1.current().sourceIntegrity().orElseThrow(),
                        c1.current().threeWayIntegrity().orElseThrow())
                .session().orElseThrow();
        DesignerCommandSession foreignC1 = foreignBaseline.apply(
                new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("after")))
                .session();
        DesignerCommandRevision exactC1 = c1.orchestrator().currentRevision();
        assertNotSame(exactC1, foreignC1.current());
        c1.orchestrator().close();
        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(foreignC1, combined)) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending) {
                assertThrows(IOException.class,
                        () -> c1.pair().coordinator().beginStagedReplacement(
                                c1.evidence(), pending));
                assertSame(foreignC1.current(), orchestrator.currentRevision());
                assertSame(c1.evidence(),
                        c1.pair().coordinator().stagedEvidence());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        c1.pair().coordinator().state().status());

                var retry = orchestrator.beginCommand(new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("retry")))
                        .lease().orElseThrow();
                retry.abort();
            }
        }
    }

    @Test
    void silentDurableBaselineDriftClearsStagedAuthorityAndCommandSession()
            throws Exception {
        StagedPair c1 = stageRealPair("replace_silent_disk_drift");
        SaveCookie cookie = c1.pair().dataObject().getCookie(SaveCookie.class);
        byte[] externalDart = (new String(
                c1.prepared().baselineDartBytes(), StandardCharsets.UTF_8)
                + "// silent external disk change\n")
                .getBytes(StandardCharsets.UTF_8);
        Files.write(c1.pair().dartPath(), externalDart);
        assertSame(c1.evidence(), c1.pair().coordinator().stagedEvidence(),
                "the test deliberately withholds a FileObject event");

        DesignerCombinedUndoRedo combined =
                c1.pair().dataObject().getCombinedUndoRedo();
        try (DesignerCommandSessionOrchestrator orchestrator =
                c1.orchestrator()) {
            var pending = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C2")))
                    .lease().orElseThrow();
            try (pending) {
                assertThrows(IOException.class,
                        () -> c1.pair().coordinator().beginStagedReplacement(
                                c1.evidence(), pending));
            }

            assertNull(c1.pair().coordinator().stagedEvidence());
            assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                    c1.pair().coordinator().state().status());
            assertThrows(IllegalStateException.class,
                    orchestrator::currentRevision);
            assertFalse(combined.designerSessionActive());
            assertSame(cookie,
                    c1.pair().dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(externalDart,
                    Files.readAllBytes(c1.pair().dartPath()));
            assertArrayEquals(c1.prepared().baselineFdBytes(),
                    Files.readAllBytes(c1.pair().designerPath()));
            assertArrayEquals(c1.evidence().candidateDartBytes(),
                    c1.pair().dataObject().getEditorSupport()
                            .liveSnapshot().markerBearingUtf8());
        }
    }

    private HistoricalPhysicalCommandFixture historicalPhysicalC1S0Fixture(
            String folderName) throws Exception {
        return historicalPhysicalC1S0Fixture(
                saveC1HistoryFixture(folderName),
                "// shared durable Source S2 before physical command\n",
                "shared-durable-C2-before-physical-command");
    }

    private HistoricalBaselineOverlayFixture historicalBaselineOverlayFixture(
            String folderName) throws Exception {
        StagedPair staged = stageRealPair(folderName);
        DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator();
        try {
            TestPair pair = staged.pair();
            PairSaveCoordinator coordinator = pair.coordinator();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            long c0RevisionId = adjacentUndoTargetId(orchestrator);
            long c1RevisionId = orchestrator.currentRevision().revisionId();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);

            PairSaveCoordinator.StagedCommandSource c1Source =
                    coordinator.captureStagedCommandSource();
            var returnAttempt = c1Source.beginCommand(
                    setDataProperty("before"));
            var pendingC2 = returnAttempt.lease().orElseThrow();
            long c2RevisionId = pendingC2.candidateRevision().revisionId();
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    pendingC2.candidateRevision().persistenceKind());
            try (pendingC2;
                    PairSaveCoordinator.PairReplacement replacement =
                            coordinator.beginStagedReplacement(
                                    c1Source, pendingC2)) {
                replacement.replaceWithExactBaseline();
            }

            stableCookie.save();
            assertFalse(orchestrator.dirty());
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount());

            document.insertString(document.getLength(),
                    "// durable Source above duplicate BASELINE history\n",
                    null);
            byte[] durableSourceDart =
                    editor.liveSnapshot().markerBearingUtf8();
            stableCookie.save();
            FlutterDesignerDocumentState.Current durableSourceCurrent =
                    awaitCurrentWithPair(
                            controller,
                            staged.prepared().baselineFdBytes(),
                            durableSourceDart);
            assertSame(durableSourceCurrent, controller.state());
            assertFalse(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    coordinator.state().status());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);
            assertEquals(c1RevisionId,
                    orchestrator.currentRevision().revisionId());

            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);
            DesignerCommandRevision historicalC0 =
                    orchestrator.currentRevision();
            LiveDartDocumentSnapshot historicalLive = editor.liveSnapshot();
            assertEquals(c0RevisionId, historicalC0.revisionId());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    historicalC0.persistenceKind());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    historicalC0.dartCandidateBytes());
            assertArrayEquals(staged.prepared().baselineDartBytes(),
                    historicalLive.markerBearingUtf8());
            assertTrue(orchestrator.dirty());
            assertTrue(editor.sourceModified());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    coordinator.state().status());
            assertNull(coordinator.stagedProofSnapshot());
            assertEquals(2, coordinator.unsavedPairHistoryEdgeCount());
            assertSame(stableCookie,
                    pair.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(durableSourceDart,
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(staged.prepared().baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));

            return new HistoricalBaselineOverlayFixture(
                    staged,
                    pair,
                    orchestrator,
                    editor,
                    controller,
                    combined,
                    stableCookie,
                    c0RevisionId,
                    c1RevisionId,
                    c2RevisionId,
                    historicalC0,
                    historicalLive,
                    staged.prepared().baselineDartBytes(),
                    durableSourceDart,
                    staged.prepared().baselineFdBytes());
        } catch (Exception | Error failure) {
            orchestrator.close();
            throw failure;
        }
    }

    private HistoricalPhysicalCommandFixture historicalPhysicalC1S0Fixture(
            SavedPairHistoryFixture history,
            String sourceSuffix,
            String c2Value) throws Exception {
        DesignerCommandSessionOrchestrator orchestrator =
                history.orchestrator();
        try {
            TestPair pair = history.staged().pair();
            FlutterDesignerEditorSupport editor =
                    pair.dataObject().getEditorSupport();
            FlutterDesignerDocumentController controller =
                    pair.dataObject().getDocumentController();
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            StyledDocument document = editor.getDocument();
            assertNotNull(document);

            document.insertString(document.getLength(),
                    sourceSuffix, null);
            byte[] sourceS2 = editor.liveSnapshot().markerBearingUtf8();
            SaveCookie stableCookie = pair.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(stableCookie);
            stableCookie.save();

            FlutterDesignerDocumentState.Current sourceCurrent =
                    awaitCurrentWithPair(
                            controller, history.savedFd(), sourceS2);
            PairSaveEvidence durableC2 = stageNextPairCommand(
                    pair,
                    sourceCurrent,
                    orchestrator,
                    c2Value);
            stableCookie.save();

            byte[] durableC2Dart = durableC2.candidateDartBytes();
            byte[] durableC2Fd = durableC2.preparedPairIdentity()
                    .prospectiveFdBytes();
            FlutterDesignerDocumentState.Current durableC2Current =
                    awaitCurrentWithPair(
                            controller, durableC2Fd, durableC2Dart);
            replayPairHistory(
                    pair, DesignerSemanticUndoableEdit.Direction.UNDO);
            assertArrayEquals(sourceS2,
                    editor.liveSnapshot().markerBearingUtf8());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
                return null;
            });

            DesignerCommandRevision c1WithS0 =
                    orchestrator.currentRevision();
            assertEquals(history.savedRevisionId(),
                    c1WithS0.revisionId());
            assertArrayEquals(history.savedDart(),
                    editor.liveSnapshot().markerBearingUtf8());
            assertSavedHistoryProof(
                    pair,
                    durableC2Current,
                    c1WithS0,
                    PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                    durableC2Dart,
                    durableC2Fd,
                    history.savedDart(),
                    history.savedFd());
            LiveDartDocumentSnapshot liveC1WithS0 = editor.liveSnapshot();
            PairSaveCoordinator.StagedCommandSource commandSource =
                    pair.coordinator().captureStagedCommandSource();
            assertSame(pair.coordinator().stagedProofSnapshot()
                            .preparedPairIdentity(),
                    commandSource.physicalPairIdentity());
            assertEquals(2,
                    pair.coordinator().unsavedPairHistoryEdgeCount());
            assertTrue(combined.canRedo());
            assertSavedPairRemainsDurable(pair, durableC2);
            return new HistoricalPhysicalCommandFixture(
                    history,
                    pair,
                    orchestrator,
                    editor,
                    combined,
                    durableC2,
                    durableC2Current,
                    sourceS2,
                    c1WithS0,
                    commandSource,
                    liveC1WithS0,
                    stableCookie);
        } catch (Exception | Error failure) {
            orchestrator.close();
            throw failure;
        }
    }

    private static SetProperty setDataProperty(String value) {
        return new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue(value));
    }

    private static Object stagedCommandSourceProofIdentity(
            PairSaveCoordinator.StagedCommandSource source) throws Exception {
        Method proofMethod = source.getClass()
                .getDeclaredMethod("predecessorProof");
        proofMethod.setAccessible(true);
        return proofMethod.invoke(source);
    }

    private static void assertHistoricalPhysicalFixtureRemainsDurable(
            HistoricalPhysicalCommandFixture fixture) throws IOException {
        assertSavedPairRemainsDurable(
                fixture.pair(), fixture.durableC2());
        assertEquals(2,
                fixture.pair().coordinator()
                        .unsavedPairHistoryEdgeCount());
        assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                fixture.pair().coordinator().state().status());
        assertSame(fixture.stableCookie(),
                fixture.pair().dataObject().getCookie(SaveCookie.class));
    }

    private SavedPairHistoryFixture saveC1HistoryFixture(String folderName)
            throws Exception {
        return saveC1HistoryFixture(stageRealPair(folderName));
    }

    private SavedPairHistoryFixture saveC1HistoryFixture(StagedPair staged)
            throws Exception {
        DesignerCommandSessionOrchestrator orchestrator =
                staged.orchestrator();
        DesignerCommandRevision stagedC1 = orchestrator.currentRevision();
        long oldRevisionId = adjacentUndoTargetId(orchestrator);
        SaveCookie cookie = staged.pair().dataObject()
                .getCookie(SaveCookie.class);
        assertNotNull(cookie);

        cookie.save();

        DesignerCommandRevision savedC1 = orchestrator.currentRevision();
        assertEquals(stagedC1.revisionId(), savedC1.revisionId(),
                "Pair Save must preserve C1's chronological revision id");
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                savedC1.persistenceKind());
        assertFalse(orchestrator.dirty());
        assertFalse(staged.pair().dataObject().getEditorSupport()
                .sourceModified());
        assertEquals(PairSaveCoordinatorStatus.CLEAN,
                staged.pair().coordinator().state().status());
        assertNull(staged.pair().coordinator().stagedEvidence());
        assertNull(staged.pair().coordinator().stagedProofSnapshot());
        assertNull(staged.pair().dataObject().getCookie(SaveCookie.class));
        FlutterDesignerDocumentState.Current savedCurrent =
                awaitCurrentWithFd(
                        staged.pair().dataObject().getDocumentController(),
                        staged.prepared().prospectiveFdBytes());

        SavedPairHistoryFixture fixture = new SavedPairHistoryFixture(
                staged,
                orchestrator,
                savedCurrent,
                oldRevisionId,
                savedC1.revisionId(),
                staged.prepared().baselineDartBytes(),
                staged.prepared().baselineFdBytes(),
                staged.prepared().prospectiveDartBytes(),
                staged.prepared().prospectiveFdBytes());
        assertSavedC1RemainsDurable(fixture);
        return fixture;
    }

    private static void assertSavedC1RemainsDurable(
            SavedPairHistoryFixture history) throws IOException {
        assertArrayEquals(history.savedDart(),
                Files.readAllBytes(history.staged().pair().dartPath()),
                "native history must not rewrite durable saved C1 Dart bytes");
        assertArrayEquals(history.savedFd(),
                Files.readAllBytes(history.staged().pair().designerPath()),
                "native history must not rewrite durable saved C1 .fd bytes");
    }

    private static void assertSavedPairRemainsDurable(
            TestPair pair,
            PairSaveEvidence saved) throws IOException {
        assertArrayEquals(saved.candidateDartBytes(),
                Files.readAllBytes(pair.dartPath()),
                "native history must not rewrite durable saved Dart bytes");
        assertArrayEquals(saved.preparedPairIdentity().prospectiveFdBytes(),
                Files.readAllBytes(pair.designerPath()),
                "native history must not rewrite durable saved .fd bytes");
    }

    private static long retainedPairBytes(byte[] dart, byte[] fd) {
        return Math.addExact((long) dart.length, (long) fd.length);
    }

    private static void assertSavedHistoryProof(
            TestPair pair,
            FlutterDesignerDocumentState.Current savedCurrent,
            DesignerCommandRevision revision,
            PairSaveCoordinator.StagedPairProofKind expectedKind,
            byte[] savedDart,
            byte[] savedFd,
            byte[] historicalDart,
            byte[] historicalFd) throws IOException {
        FlutterDesignerEditorSupport editor =
                pair.dataObject().getEditorSupport();
        assertNull(pair.coordinator().stagedEvidence(),
                "saved semantic history must not fabricate fresh analyzer evidence");
        PairSaveCoordinator.StagedPairProofSnapshot proof =
                pair.coordinator().stagedProofSnapshot();
        assertNotNull(proof);
        assertEquals(expectedKind, proof.kind());
        assertSame(savedCurrent, proof.loadedCurrentIdentity());
        assertEquals(revision.document(),
                proof.preparedPairIdentity().prospectiveDocument(),
                "a physical Source-envelope proof must retain the canonical semantic document");
        assertArrayEquals(revision.fdBytes(),
                proof.preparedPairIdentity().prospectiveFdBytes(),
                "a physical Source-envelope proof must retain the canonical semantic .fd bytes");
        assertTrue(proof.liveCandidateIdentity().sameEvidence(
                editor.liveSnapshot()));
        assertArrayEquals(savedDart, proof.baselineDartBytes());
        assertArrayEquals(savedFd, proof.baselineFdBytes());
        assertArrayEquals(historicalDart, proof.candidateDartBytes());
        assertArrayEquals(historicalDart,
                proof.preparedPairIdentity().prospectiveDartBytes());
        assertArrayEquals(historicalFd,
                proof.preparedPairIdentity().prospectiveFdBytes());
        assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                pair.coordinator().state().status());
        assertNotNull(pair.dataObject().getCookie(SaveCookie.class));
    }

    private static void assertFormerDurableBIsStaged(
            SavedPairHistoryFixture history,
            DesignerCommandRevision oldB) throws IOException {
        TestPair pair = history.staged().pair();
        FlutterDesignerEditorSupport editor =
                pair.dataObject().getEditorSupport();
        assertNull(pair.coordinator().stagedEvidence(),
                "former durable B has no analyzer ticket and must not fabricate PairSaveEvidence");
        PairSaveCoordinator.StagedPairProofSnapshot proof =
                pair.coordinator().stagedProofSnapshot();
        assertNotNull(proof);
        assertEquals(PairSaveCoordinator.StagedPairProofKind.FORMER_DURABLE,
                proof.kind());
        assertSame(history.savedCurrent(), proof.loadedCurrentIdentity());
        assertEquals(oldB.document(),
                proof.preparedPairIdentity().prospectiveDocument(),
                "a retained physical Source envelope keeps the exact semantic revision document");
        assertArrayEquals(oldB.fdBytes(),
                proof.preparedPairIdentity().prospectiveFdBytes(),
                "a retained physical Source envelope keeps the exact semantic revision .fd bytes");
        assertTrue(proof.liveCandidateIdentity().sameEvidence(
                editor.liveSnapshot()));
        assertArrayEquals(history.savedDart(), proof.baselineDartBytes());
        assertArrayEquals(history.savedFd(), proof.baselineFdBytes());
        assertArrayEquals(history.oldDart(), proof.candidateDartBytes());
        assertArrayEquals(history.oldFd(),
                proof.preparedPairIdentity().prospectiveFdBytes());
    }

    private PairSaveEvidence replaceOnce(
            StagedPair staged,
            DesignerCommandSessionOrchestrator orchestrator,
            PairSaveEvidence predecessor,
            String value) throws Exception {
        return replaceOnce(staged, orchestrator, predecessor, new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue(value)));
    }

    private PairSaveEvidence replaceOnce(
            StagedPair staged,
            DesignerCommandSessionOrchestrator orchestrator,
            PairSaveEvidence predecessor,
            DesignerCommand command) throws Exception {
        var pending = orchestrator.beginCommand(command)
                .lease().orElseThrow();
        try (pending;
                PairSaveCoordinator.PairReplacement replacement =
                        staged.pair().coordinator().beginStagedReplacement(
                                predecessor, pending)) {
            ReplacementTicket analysisTicket = replacementTicket(
                    staged, replacement);
            PairSaveEvidenceResult result = replacement.acceptAnalysisAndReplace(
                    analysisTicket.ticket(),
                    passingAnalysis(
                            analysisTicket.ticket(),
                            analysisTicket.frameworkReal(),
                            0));
            assertTrue(result.ready(), () -> result.diagnostics().toString());
            return ((PairSaveEvidenceResult.Ready) result).evidence();
        }
    }

    private static long adjacentUndoTargetId(
            DesignerCommandSessionOrchestrator orchestrator) {
        var lease = orchestrator.beginUndoTransition().lease().orElseThrow();
        try (lease) {
            long target = lease.candidateRevision().revisionId();
            lease.abort();
            return target;
        }
    }

    private static void replayUnsavedPairHistory(
            TestPair pair,
            DesignerSemanticUndoableEdit.Direction direction) throws Exception {
        replayPairHistory(pair, direction);
    }

    private static void replayPairHistory(
            TestPair pair,
            DesignerSemanticUndoableEdit.Direction direction) throws Exception {
        onEdt(() -> {
            DesignerCombinedUndoRedo combined =
                    pair.dataObject().getCombinedUndoRedo();
            if (direction == DesignerSemanticUndoableEdit.Direction.UNDO) {
                assertTrue(combined.canUndo(),
                        "one exact semantic native Undo edge must be available");
                combined.undo();
            } else {
                assertTrue(combined.canRedo(),
                        "one exact semantic native Redo edge must be available");
                combined.redo();
            }
            return null;
        });
    }

    private static void detachPreparedReplayNativeContextForRecoveryTest(
            DesignerSemanticUndoableEdit.PreparedReplay prepared)
            throws Exception {
        Field transitionField = prepared.getClass()
                .getDeclaredField("transition");
        transitionField.setAccessible(true);
        Object transition = transitionField.get(prepared);
        Field nativeReplayField = transition.getClass()
                .getDeclaredField("nativeReplay");
        nativeReplayField.setAccessible(true);
        AutoCloseable nativeReplay = (AutoCloseable)
                nativeReplayField.get(transition);
        assertNotNull(nativeReplay);
        nativeReplay.close();
        nativeReplayField.set(transition, null);
    }

    private ReplacementTicket replacementTicket(
            StagedPair staged,
            PairSaveCoordinator.PairReplacement replacement) throws Exception {
        Path projectRoot = staged.pair().dartPath().getParent().getParent();
        Path flutterSdkRoot = temporaryDirectory.resolve(
                projectRoot.getFileName() + "-flutter").toRealPath();
        Path frameworkReal = flutterSdkRoot.resolve(
                "packages/flutter/lib/src/widgets/framework.dart").toRealPath();
        FlutterSettings settings = FlutterSettings.getDefault();
        FlutterToolchainConfig priorConfig = settings.load();
        try {
            settings.save(new FlutterToolchainConfig(
                    flutterSdkRoot.toString(), true, ""));
            return new ReplacementTicket(
                    replacement.prepareAnalysis(
                            projectRoot.toRealPath(),
                            DartCandidateWarningPolicy.ALLOW),
                    frameworkReal);
        } finally {
            settings.save(priorConfig);
        }
    }

    private PairSaveEvidence stageNextPairCommand(
            TestPair pair,
            FlutterDesignerDocumentState.Current current,
            DesignerCommandSessionOrchestrator orchestrator,
            String value) throws Exception {
        FlutterDesignerEditorSupport editor =
                pair.dataObject().getEditorSupport();
        LiveDartDocumentSnapshot liveBefore = editor.liveSnapshot();
        assertFalse(editor.sourceModified());
        Path projectRoot = pair.dartPath().getParent().getParent();
        Path flutterSdkRoot = temporaryDirectory.resolve(
                projectRoot.getFileName() + "-flutter").toRealPath();
        Path frameworkReal = flutterSdkRoot.resolve(
                "packages/flutter/lib/src/widgets/framework.dart").toRealPath();

        var pending = orchestrator.beginCommand(new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue(value)))
                .lease().orElseThrow();
        try (pending;
                PairSaveCoordinator.PairPreparation preparation =
                        pair.coordinator().beginPairPreparation(
                                current, pending, liveBefore)) {
            FlutterSettings settings = FlutterSettings.getDefault();
            FlutterToolchainConfig priorConfig = settings.load();
            PairCandidateAnalysisTicket ticket;
            try {
                settings.save(new FlutterToolchainConfig(
                        flutterSdkRoot.toString(), true, ""));
                ticket = preparation.prepareAnalysis(
                        projectRoot.toRealPath(),
                        DartCandidateWarningPolicy.ALLOW);
            } finally {
                settings.save(priorConfig);
            }
            PairSaveEvidenceResult result = preparation.acceptAnalysisAndStage(
                    ticket, passingAnalysis(ticket, frameworkReal, 0));
            assertTrue(result.ready(), () -> result.diagnostics().toString());
            return ((PairSaveEvidenceResult.Ready) result).evidence();
        }
    }

    private StagedPair stageRealPair(String folderName) throws Exception {
        return stageRealPair(folderName, true);
    }

    private StagedPair stageRealPair(
            String folderName,
            FirstCommandPublicationProbe publicationProbe) throws Exception {
        return stageRealPair(
                folderName,
                true,
                BeforeApplyAction.NONE,
                publicationProbe,
                null).orElseThrow();
    }

    private StagedPair stageRealPair(
            String folderName,
            boolean acceptEvidence) throws Exception {
        return stageRealPair(
                folderName, acceptEvidence, BeforeApplyAction.NONE).orElseThrow();
    }

    private Optional<StagedPair> stageRealPair(
            String folderName,
            boolean acceptEvidence,
            BeforeApplyAction beforeApplyAction) throws Exception {
        return stageRealPair(
                folderName, acceptEvidence, beforeApplyAction, null, null);
    }

    private Optional<StagedPair> stageRealPair(
            String folderName,
            boolean acceptEvidence,
            BeforeApplyAction beforeApplyAction,
            FirstCommandPublicationProbe publicationProbe) throws Exception {
        return stageRealPair(
                folderName,
                acceptEvidence,
                beforeApplyAction,
                publicationProbe,
                null);
    }

    private StagedPair stageRealPairWithAggregateBudget(
            String folderName,
            String sourceSuffix,
            String secondCommandValue) throws Exception {
        return stageRealPair(
                folderName,
                true,
                BeforeApplyAction.NONE,
                null,
                new AggregateBudgetPlan(sourceSuffix, secondCommandValue))
                .orElseThrow();
    }

    private StagedPair stageRealPairWithPhysicalCommandBudget(
            String folderName,
            String sourceSuffix,
            String secondCommandValue,
            String physicalCommandValue) throws Exception {
        return stageRealPair(
                folderName,
                true,
                BeforeApplyAction.NONE,
                null,
                new AggregateBudgetPlan(
                        sourceSuffix,
                        secondCommandValue,
                        physicalCommandValue))
                .orElseThrow();
    }

    private StagedPair stageLegacyV1RealPair(
            String folderName,
            DesignerCommand firstCommand) throws Exception {
        return stageRealPair(
                folderName,
                true,
                BeforeApplyAction.NONE,
                null,
                null,
                new InitialPairPlan(true, firstCommand)).orElseThrow();
    }

    private Optional<StagedPair> stageRealPair(
            String folderName,
            boolean acceptEvidence,
            BeforeApplyAction beforeApplyAction,
            FirstCommandPublicationProbe publicationProbe,
            AggregateBudgetPlan aggregateBudgetPlan) throws Exception {
        return stageRealPair(
                folderName,
                acceptEvidence,
                beforeApplyAction,
                publicationProbe,
                aggregateBudgetPlan,
                null);
    }

    private Optional<StagedPair> stageRealPair(
            String folderName,
            boolean acceptEvidence,
            BeforeApplyAction beforeApplyAction,
            FirstCommandPublicationProbe publicationProbe,
            AggregateBudgetPlan aggregateBudgetPlan,
            InitialPairPlan initialPairPlan) throws Exception {
        Path projectRoot = Files.createDirectories(
                temporaryDirectory.resolve(folderName));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path designerRoot = Files.createDirectories(
                projectRoot.resolve(".fd_templates"));
        Files.writeString(projectRoot.resolve("pubspec.yaml"), """
                name: staged_pair_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(projectRoot);
        Path dartPath = lib.resolve("home_page.dart");
        Path designerPath = designerRoot.resolve("home_page.fd");
        Path flutterSdkRoot = Files.createDirectories(
                temporaryDirectory.resolve(folderName + "-flutter"));
        Path flutterLib = Files.createDirectories(
                flutterSdkRoot.resolve("packages/flutter/lib"));
        Path framework = flutterLib.resolve("src/widgets/framework.dart");
        Files.createDirectories(framework.getParent());
        Files.writeString(framework,
                "abstract class Widget {}\nclass BuildContext {}\n",
                StandardCharsets.UTF_8);
        Path flutterExecutable = Files.createDirectories(
                flutterSdkRoot.resolve("bin")).resolve(
                        System.getProperty("os.name", "")
                                .toLowerCase(java.util.Locale.ROOT)
                                .contains("win")
                                ? "flutter.bat" : "flutter");
        Files.writeString(flutterExecutable, "@echo off\n", StandardCharsets.UTF_8);
        flutterExecutable.toFile().setExecutable(true);

        DartRegionGenerator generator = new DartRegionGenerator();
        DesignerDocument seed = designerDocument(
                descriptor("", ""), "before");
        GeneratedDartRegions generatedBefore = generator.generate(
                seed, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DartSourceDescriptor baselineDescriptor = descriptor(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        DesignerDocument baselineDocument = designerDocument(
                baselineDescriptor, "before");
        byte[] baselineDart = sourceBytes(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        byte[] baselineFd = new FdDocumentCodec()
                .encode(baselineDocument).copyBytes();
        if (initialPairPlan != null && initialPairPlan.legacyV1()) {
            String canonical = new String(
                    baselineFd, StandardCharsets.UTF_8);
            String legacy = canonical.replace(
                    "\"schemaVersion\": 4",
                    "\"schemaVersion\": 1");
            if (legacy.equals(canonical)) {
                throw new AssertionError(
                        "The canonical fixture did not declare schema v4");
            }
            baselineFd = legacy.getBytes(StandardCharsets.UTF_8);
        }
        Files.write(dartPath, baselineDart);
        Files.write(designerPath, baselineFd);
        FileUtil.refreshFor(projectRoot.toFile());

        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        FileObject designer = FileUtil.toFileObject(designerPath.toFile());
        assertNotNull(dart);
        assertNotNull(designer);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                .dataObject(dart, project);
        TestPair pair = new TestPair(
                dartPath,
                designerPath,
                dart,
                designer,
                dataObject,
                dataObject.getPairSaveCoordinator());

        FlutterDesignerDocumentController controller =
                dataObject.getDocumentController();
        assertTrue(controller.viewOpened());
        FlutterDesignerDocumentState.Current current = awaitCurrent(controller);
        FlutterDesignerEditorSupport editor = dataObject.getEditorSupport();
        LiveDartDocumentSnapshot liveBefore = guardedLiveSnapshot(
                editor, baselineDart);
        DesignerCommandSession defaultSession = DesignerCommandSession
                .openVerified(
                        current.decoded().original(),
                        baselineDart,
                        current.catalog(),
                        current.sourceIntegrity().orElseThrow(),
                        current.threeWayIntegrity().orElseThrow())
                .session().orElseThrow();
        final DesignerCommandSession baselineSession;
        if (aggregateBudgetPlan != null) {
            long maximum = aggregateBudgetPlan.physicalCommandValue() == null
                    ? exactSemanticHistoryBytesThroughSecondCommand(
                            defaultSession, aggregateBudgetPlan)
                    : exactPhysicalHistoryBytesBeforeBranchedCommand(
                            defaultSession, aggregateBudgetPlan);
            DesignerCommandLimits defaults = DesignerCommandLimits.defaults();
            DesignerCommandLimits bounded = new DesignerCommandLimits(
                    defaults.fdCodecLimits(),
                    defaults.generationLimits(),
                    defaults.sourceLimits(),
                    defaults.validationLimits(),
                    defaults.maxHistoryEdits(),
                    maximum);
            baselineSession = DesignerCommandSession.openVerified(
                    current.decoded().original(),
                    baselineDart,
                    current.catalog(),
                    current.sourceIntegrity().orElseThrow(),
                    current.threeWayIntegrity().orElseThrow(),
                    bounded).session().orElseThrow();
        } else {
            baselineSession = defaultSession;
        }
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(
                        baselineSession,
                        dataObject.getCombinedUndoRedo());
        DesignerCommand firstCommand = initialPairPlan == null
                ? new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("after"))
                : initialPairPlan.firstCommand();
        var pendingAttempt = orchestrator.beginCommand(firstCommand);
        var pendingCommand = pendingAttempt.lease().orElseThrow();
        PreparedDesignerPair prepared = pendingCommand.candidateRevision()
                .preparedPair().orElseThrow();
        Path flutterReal = flutterSdkRoot.toRealPath();
        Path frameworkReal = framework.toRealPath();
        if (beforeApplyAction
                == BeforeApplyAction.EXTERNAL_EVENT_BEFORE_RESERVATION) {
            byte[] externalDart = (new String(
                    baselineDart, StandardCharsets.UTF_8)
                    + "// external change before reservation\n")
                    .getBytes(StandardCharsets.UTF_8);
            Files.write(dartPath, externalDart);
            assertFalse(pair.coordinator().handleFileEvent(new FileEvent(dart)),
                    "a clean external event may reload, but it must advance the epoch");

            assertThrows(IOException.class,
                    () -> pair.coordinator().beginPairPreparation(
                            current, pendingCommand, liveBefore));

            assertTrue(liveBefore.sameEvidence(editor.liveSnapshot()),
                    "reservation rejection must not mutate the live Dart document");
            assertArrayEquals(externalDart, Files.readAllBytes(dartPath));
            assertArrayEquals(baselineFd, Files.readAllBytes(designerPath));
            assertFalse(editor.sourceModified());
            assertNull(dataObject.getCookie(SaveCookie.class));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertSame(baselineSession.current(), orchestrator.currentRevision(),
                    "rejected reservation must abort the exact pending B-to-C1 command");
            orchestrator.close();
            return Optional.empty();
        }
        LiveDartDocumentSnapshot liveCandidate;
        DartCandidateAnalysisResult analysis;
        PairSaveEvidenceResult staged;
        AtomicBoolean armedEventFired = new AtomicBoolean();
        AtomicBoolean armedOwnerCloseRequested = new AtomicBoolean();
        try (pendingCommand;
                PairSaveCoordinator.PairPreparation preparation =
                pair.coordinator().beginPairPreparation(
                        current, pendingCommand, liveBefore)) {
            assertEquals(PairSaveCoordinatorStatus.PREPARING_PAIR,
                    pair.coordinator().state().status());
            assertThrows(IOException.class, editor::saveDocument,
                    "Source Save must be blocked for the complete preparation lease");
            FlutterSettings settings = FlutterSettings.getDefault();
            FlutterToolchainConfig priorConfig = settings.load();
            PairCandidateAnalysisTicket ticket;
            try {
                settings.save(new FlutterToolchainConfig(
                        flutterReal.toString(), true, ""));
                ticket = preparation.prepareAnalysis(
                        projectRoot.toRealPath(),
                        DartCandidateWarningPolicy.ALLOW);
            } finally {
                settings.save(priorConfig);
            }
            assertTrue(liveBefore.sameEvidence(editor.liveSnapshot()),
                    "creating the analyzer overlay ticket must not apply candidate C");
            assertFalse(editor.sourceModified(),
                    "analysis must begin while the exact predecessor remains unchanged");
            assertNull(pair.dataObject().getCookie(SaveCookie.class),
                    "an analyzer ticket alone must not publish persistence UI");
            assertThrows(IOException.class, editor::saveDocument,
                    "Source Save must remain blocked while analysis owns the lease");
            if (beforeApplyAction == BeforeApplyAction.CLAIM_FENCE) {
                DesignerCommandRevision exactB = baselineSession.current();
                assertThrows(IllegalStateException.class,
                        pendingCommand::adoptStaged);
                assertThrows(IllegalStateException.class,
                        pendingCommand::abort);
                assertThrows(IllegalStateException.class,
                        pendingCommand::invalidate);
                assertThrows(IllegalStateException.class,
                        pendingCommand::close);
                assertSame(exactB, orchestrator.currentRevision(),
                        "claiming B-to-C1 must not move the live command cursor");
                assertEquals(PairSaveCoordinatorStatus.PREPARING_PAIR,
                        pair.coordinator().state().status());

                preparation.close();

                assertSame(exactB, orchestrator.currentRevision());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        pair.coordinator().state().status());
                try (var retry = orchestrator.beginCommand(new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("retry-after-claim")))
                        .lease().orElseThrow()) {
                    assertSame(exactB, orchestrator.currentRevision(),
                            "a fresh command proves the claimed busy fence was released");
                }
                return Optional.empty();
            }
            if (beforeApplyAction == BeforeApplyAction.CLOSE_EXACT_B) {
                DesignerCommandRevision exactB = baselineSession.current();

                preparation.close();

                assertTrue(liveBefore.sameEvidence(editor.liveSnapshot()));
                assertSame(exactB, orchestrator.currentRevision());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        pair.coordinator().state().status());
                assertFalse(editor.sourceModified());
                assertNull(dataObject.getCookie(SaveCookie.class));
                try (var retry = orchestrator.beginCommand(new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("retry-after-close")))
                        .lease().orElseThrow()) {
                    assertSame(exactB, orchestrator.currentRevision(),
                            "closing the preparation must release command busy state");
                }
                return Optional.empty();
            }
            if (beforeApplyAction == BeforeApplyAction.EXTERNAL_EVENT_DURING_APPLY) {
                StyledDocument document = editor.getDocument();
                assertNotNull(document);
                AtomicBoolean fired = new AtomicBoolean();
                AtomicBoolean stateCallbackFired = new AtomicBoolean();
                AtomicBoolean documentReadCompletedInCallback =
                        new AtomicBoolean();
                AtomicBoolean fatalCallbackFired = new AtomicBoolean();
                CountDownLatch documentReadFinished = new CountDownLatch(1);
                AtomicReference<Throwable> documentReadFailure =
                        new AtomicReference<>();
                AtomicReference<Throwable> stateCallbackFailure =
                        new AtomicReference<>();
                AtomicReference<Thread> documentReader = new AtomicReference<>();
                java.beans.PropertyChangeListener stateListener = event -> {
                    if (!(event.getNewValue()
                            instanceof PairSaveCoordinatorSnapshot snapshot)
                            || snapshot.status()
                                    != PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                            || !stateCallbackFired.compareAndSet(false, true)) {
                        return;
                    }
                    Thread worker = new Thread(() -> {
                        try {
                            document.getText(0, document.getLength());
                        } catch (Throwable failure) {
                            documentReadFailure.set(failure);
                        } finally {
                            documentReadFinished.countDown();
                        }
                    }, "document-reader-from-pair-state-callback");
                    worker.setDaemon(true);
                    documentReader.set(worker);
                    worker.start();
                    try {
                        documentReadCompletedInCallback.set(
                                documentReadFinished.await(
                                        2, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        stateCallbackFailure.set(interrupted);
                    }
                    fatalCallbackFired.set(true);
                    throw new AssertionError(
                            "synthetic fatal pair-state presentation failure");
                };
                DocumentListener listener = new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent event) {
                        fireExternalEvent();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event) {
                        fireExternalEvent();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event) {
                        fireExternalEvent();
                    }

                    private void fireExternalEvent() {
                        if (fired.compareAndSet(false, true)) {
                            assertTrue(pair.coordinator().handleFileEvent(
                                    new FileEvent(designer)));
                        }
                    }
                };
                pair.coordinator().addPropertyChangeListener(stateListener);
                document.addDocumentListener(listener);
                try {
                    DartCandidateAnalysisResult passed = passingAnalysis(
                            ticket, frameworkReal, 0);
                    assertThrows(IOException.class,
                            () -> preparation.acceptAnalysisAndStage(
                                    ticket, passed));
                } finally {
                    document.removeDocumentListener(listener);
                    pair.coordinator().removePropertyChangeListener(stateListener);
                }

                assertTrue(fired.get());
                assertTrue(stateCallbackFired.get());
                assertTrue(fatalCallbackFired.get());
                assertTrue(documentReadCompletedInCallback.get(),
                        "state callback must run after the document atomic "
                        + "lock is released");
                assertNull(stateCallbackFailure.get());
                Thread reader = documentReader.get();
                assertNotNull(reader);
                reader.join(TimeUnit.SECONDS.toMillis(2));
                assertFalse(reader.isAlive());
                assertNull(documentReadFailure.get());
                assertSame(document, editor.getDocument());
                assertArrayEquals(liveBefore.markerBearingUtf8(),
                        editor.liveSnapshot().markerBearingUtf8());
                assertTrue(editor.sourceModified(),
                        "an observed apply/restore mutation stays fail-closed dirty");
                assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        pair.coordinator().state().status());
                assertFalse(orchestrator.canUndo());
                assertFalse(orchestrator.canRedo());
                assertThrows(IllegalStateException.class,
                        orchestrator::currentRevision,
                        "an external apply event must invalidate the claimed command");
                assertNotNull(dataObject.getCookie(SaveCookie.class));
                assertArrayEquals(baselineDart, Files.readAllBytes(dartPath));
                assertArrayEquals(baselineFd, Files.readAllBytes(designerPath));
                return Optional.empty();
            }
            if (beforeApplyAction != BeforeApplyAction.NONE) {
                String userEdit = "// independent edit after preparation began\n";
                StyledDocument document = editor.getDocument();
                assertNotNull(document);
                document.insertString(document.getLength(), userEdit, null);
                String exactLiveEdit = document.getText(
                        0, document.getLength());
                SaveCookie recoveryCookie = pair.dataObject()
                        .getCookie(SaveCookie.class);
                assertNotNull(recoveryCookie,
                        "the user's independent live edit must publish SaveCookie");

                if (beforeApplyAction == BeforeApplyAction.FAIL_APPLY) {
                    DartCandidateAnalysisResult passed = passingAnalysis(
                            ticket, frameworkReal, 0);
                    assertThrows(IOException.class,
                            () -> preparation.acceptAnalysisAndStage(
                                    ticket, passed),
                            "an edit during analysis must fail the EDT predecessor CAS");
                } else {
                    preparation.close();
                }
                assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                        pair.coordinator().state().status(),
                        "ending an unapplied lease must release PREPARING_PAIR");
                assertFalse(orchestrator.canUndo());
                assertFalse(orchestrator.canRedo());
                assertThrows(IllegalStateException.class,
                        orchestrator::currentRevision,
                        "a non-B Source revision must invalidate the command session");
                assertTrue(editor.sourceModified());
                assertSame(recoveryCookie,
                        pair.dataObject().getCookie(SaveCookie.class));
                assertSame(document, editor.getDocument());
                assertEquals(exactLiveEdit,
                        document.getText(0, document.getLength()));

                byte[] expectedDart = (new String(
                        prepared.baselineDartBytes(), StandardCharsets.UTF_8)
                        + userEdit).getBytes(StandardCharsets.UTF_8);
                assertArrayEquals(expectedDart,
                        editor.liveSnapshot().markerBearingUtf8());
                assertArrayEquals(prepared.baselineDartBytes(),
                        Files.readAllBytes(pair.dartPath()));
                assertArrayEquals(prepared.baselineFdBytes(),
                        Files.readAllBytes(pair.designerPath()));

                recoveryCookie.save();

                assertArrayEquals(expectedDart,
                        Files.readAllBytes(pair.dartPath()));
                assertArrayEquals(prepared.baselineFdBytes(),
                        Files.readAllBytes(pair.designerPath()));
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        pair.coordinator().state().status());
                assertFalse(editor.sourceModified());
                assertNull(pair.dataObject().getCookie(SaveCookie.class));
                return Optional.empty();
            }
            analysis = passingAnalysis(
                    ticket, frameworkReal, acceptEvidence ? 0 : 1);
            if (beforeApplyAction
                    == BeforeApplyAction.EXTERNAL_EVENT_WHILE_FORWARD_ARMED) {
                pair.coordinator().setForwardAdmissionHookForTests(
                        (exactLease, replacementAdmission) -> {
                            assertSame(pendingCommand, exactLease);
                            assertFalse(replacementAdmission);
                            assertThrows(IOException.class, editor::saveDocument,
                                    "ARMED must continue blocking Save");
                            assertThrows(IllegalStateException.class,
                                    () -> orchestrator.beginCommand(
                                            new SetProperty(
                                                    ROOT_ID,
                                                    DATA,
                                                    new PropertyValue.StringValue(
                                                            "forbidden"))),
                                    "ARMED must retain the one claimed command");
                            assertThrows(IOException.class, preparation::close,
                                    "ARMED must block cancellation of the running lease operation");
                            assertTrue(pair.coordinator().handleFileEvent(
                                    new FileEvent(designer)));
                            armedEventFired.set(true);
                        });
            } else if (beforeApplyAction
                    == BeforeApplyAction.OWNER_CLOSE_WHILE_FORWARD_ARMED) {
                pair.coordinator().setForwardAdmissionHookForTests(
                        (exactLease, replacementAdmission) -> {
                            assertSame(pendingCommand, exactLease);
                            assertFalse(replacementAdmission);
                            orchestrator.close();
                            assertSame(baselineSession.current(),
                                    orchestrator.currentRevision(),
                                    "close must defer until exact C1 adoption");
                            assertTrue(pendingCommand
                                    .ownsExactActiveTransition());
                            assertTrue(dataObject.getCombinedUndoRedo()
                                    .designerSessionActive(),
                                    "the claimed C1 lease must retain its binding until ACK effects");
                            armedOwnerCloseRequested.set(true);
                        });
            }
            javax.swing.event.ChangeListener commandListener = null;
            java.beans.PropertyChangeListener pairListener = null;
            if (publicationProbe != null) {
                DesignerCommandRevision exactC1 =
                        pendingCommand.candidateRevision();
                commandListener = event -> {
                    publicationProbe.callbacks().add("command");
                    PairSaveEvidence exactStaged =
                            pair.coordinator().stagedEvidence();
                    publicationProbe.commandSawJointState().set(
                            orchestrator.currentRevision() == exactC1
                            && exactStaged != null
                            && exactStaged.preparedPairIdentity()
                                == exactC1.preparedPair().orElseThrow());
                };
                pairListener = event -> {
                    if (event.getNewValue()
                            instanceof PairSaveCoordinatorSnapshot snapshot
                            && snapshot.status()
                                == PairSaveCoordinatorStatus.STAGED_PAIR) {
                        publicationProbe.callbacks().add("pair");
                        PairSaveEvidence exactStaged =
                                pair.coordinator().stagedEvidence();
                        publicationProbe.pairSawJointState().set(
                                orchestrator.currentRevision() == exactC1
                                && exactStaged != null
                                && exactStaged.preparedPairIdentity()
                                    == exactC1.preparedPair().orElseThrow());
                    }
                };
                orchestrator.addChangeListener(commandListener);
                pair.coordinator().addPropertyChangeListener(pairListener);
            }
            try {
                staged = preparation.acceptAnalysisAndStage(ticket, analysis);
            } finally {
                pair.coordinator().setForwardAdmissionHookForTests(null);
                if (commandListener != null) {
                    orchestrator.removeChangeListener(commandListener);
                }
                if (pairListener != null) {
                    pair.coordinator().removePropertyChangeListener(pairListener);
                }
            }
            liveCandidate = editor.liveSnapshot();
            if (beforeApplyAction
                    == BeforeApplyAction.EXTERNAL_EVENT_WHILE_FORWARD_ARMED) {
                assertTrue(armedEventFired.get());
                assertTrue(staged.ready(),
                        "native ACK is already authoritative and cannot be reported as an ordinary apply failure");
                assertSame(pendingCommand.candidateRevision(),
                        orchestrator.currentRevision(),
                        "poisoned ACK must still adopt the exact native command target");
                assertArrayEquals(prepared.prospectiveDartBytes(),
                        liveCandidate.markerBearingUtf8());
                assertNull(pair.coordinator().stagedEvidence());
                assertEquals(0,
                        pair.coordinator().unsavedPairHistoryEdgeCount());
                assertEquals(PairSaveCoordinatorStatus.EXTERNAL_CONFLICT,
                        pair.coordinator().state().status());
                assertNotNull(dataObject.getCookie(SaveCookie.class));
                assertArrayEquals(baselineDart, Files.readAllBytes(dartPath));
                assertArrayEquals(baselineFd, Files.readAllBytes(designerPath));
                orchestrator.close();
                return Optional.empty();
            }
            if (beforeApplyAction
                    == BeforeApplyAction.OWNER_CLOSE_WHILE_FORWARD_ARMED) {
                DesignerCommandRevision exactC1 =
                        pendingCommand.candidateRevision();
                assertTrue(armedOwnerCloseRequested.get());
                assertTrue(staged.ready(),
                        "native ACK remains authoritative when owner close was deferred");
                assertSame(exactC1,
                        retainedClosedCommandRevision(orchestrator),
                        "the close-aware ACK must adopt exact C1 before closing its owner");
                assertThrows(IllegalStateException.class,
                        orchestrator::currentRevision);
                assertFalse(pendingCommand.ownsExactActiveTransition());
                assertFalse(dataObject.getCombinedUndoRedo()
                        .designerSessionActive(),
                        "deferred command effects must release the Combined binding");
                assertArrayEquals(exactC1.dartCandidateBytes(),
                        liveCandidate.markerBearingUtf8());
                assertTrue(editor.nativeUndoRedoManagerForCombinedBridge()
                        .canUndo(),
                        "the exact native B-to-C1 semantic edge remains recorded");
                assertNull(pair.coordinator().stagedEvidence());
                assertEquals(0,
                        pair.coordinator().unsavedPairHistoryEdgeCount());
                PairSaveCoordinatorSnapshot snapshot =
                        pair.coordinator().state();
                assertEquals(PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                        snapshot.status());
                assertEquals(
                        "The Designer command owner closed during native forward admission",
                        snapshot.reason().orElseThrow());
                assertNotNull(dataObject.getCookie(SaveCookie.class));
                assertArrayEquals(baselineDart,
                        Files.readAllBytes(dartPath));
                assertArrayEquals(baselineFd,
                        Files.readAllBytes(designerPath));
                return Optional.empty();
            }
            if (acceptEvidence) {
                assertTrue(editor.sourceModified());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        pair.coordinator().state().status());
                assertNotNull(pair.dataObject().getCookie(SaveCookie.class));
                assertArrayEquals(prepared.prospectiveDartBytes(),
                        liveCandidate.markerBearingUtf8());
                assertSame(pendingCommand.candidateRevision(),
                        orchestrator.currentRevision(),
                        "staging C1 must jointly adopt the exact command cursor");
            }
        }
        if (acceptEvidence) {
            assertTrue(staged.ready(), () -> staged.diagnostics().toString());
        } else {
            assertFalse(staged.ready(),
                    "revision-mismatched analyzer evidence must be rejected");
            assertTrue(liveBefore.sameEvidence(editor.liveSnapshot()),
                    "rejected pre-apply analysis must preserve document identity, "
                    + "version, guards, hash and exact predecessor bytes");
            assertArrayEquals(liveBefore.markerBearingUtf8(),
                    editor.liveSnapshot().markerBearingUtf8(),
                    "rejected evidence must retain the exact live Dart baseline");
            assertFalse(editor.sourceModified());
            assertFalse(editor.nativeUndoRedoManagerForCombinedBridge().canUndo(),
                    "rejected analysis must not create a managed Dart Undo edit");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    pair.coordinator().state().status());
            assertNull(pair.dataObject().getCookie(SaveCookie.class));
            assertSame(baselineSession.current(), orchestrator.currentRevision(),
                    "rejected analyzer evidence must abort to exact B");
            assertArrayEquals(prepared.baselineDartBytes(),
                    Files.readAllBytes(pair.dartPath()));
            assertArrayEquals(prepared.baselineFdBytes(),
                    Files.readAllBytes(pair.designerPath()));
        }
        return Optional.of(new StagedPair(
                pair,
                current,
                prepared,
                liveBefore,
                liveCandidate,
                analysis,
                staged instanceof PairSaveEvidenceResult.Ready ready
                        ? ready.evidence() : null,
                orchestrator,
                baselineSession.limits().maxRetainedPairBytes()));
    }

    private static long exactSemanticHistoryBytesThroughSecondCommand(
            DesignerCommandSession baselineSession,
            AggregateBudgetPlan plan) {
        var first = baselineSession.apply(new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue("after")));
        if (!first.changed()) {
            throw new AssertionError(
                    "aggregate-budget probe could not derive C1: "
                    + first.diagnostics());
        }
        DesignerCommandSession savedC1 = first.session().markSaved();
        byte[] sourceS2 = (new String(
                savedC1.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                + plan.sourceSuffix()).getBytes(StandardCharsets.UTF_8);
        DesignerCommandSession sourceAnchored =
                savedC1.reanchorSavedSource(sourceS2);
        var second = sourceAnchored.apply(new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue(plan.secondCommandValue())));
        if (!second.changed()) {
            throw new AssertionError(
                    "aggregate-budget probe could not derive C2: "
                    + second.diagnostics());
        }

        long retainedBytes = 0;
        // Pair Save first re-derives the complete logical history against the
        // exact C2 durable anchor. Size the pure-session limit for that exact
        // canonical graph; the coordinator-level physical-variant aggregate
        // must be the first bound that rejects this fixture.
        DesignerCommandSession cursor = second.session().markSaved();
        while (true) {
            retainedBytes = Math.addExact(
                    retainedBytes,
                    retainedPairBytes(
                            cursor.current().dartCandidateBytes(),
                            cursor.current().fdBytes()));
            var undo = cursor.undo();
            if (!undo.changed()) {
                return retainedBytes;
            }
            cursor = undo.session();
        }
    }

    private static long exactPhysicalHistoryBytesBeforeBranchedCommand(
            DesignerCommandSession baselineSession,
            AggregateBudgetPlan plan) {
        var first = baselineSession.apply(setDataProperty("after"));
        if (!first.changed()) {
            throw new AssertionError(
                    "physical-command budget probe could not derive C1: "
                    + first.diagnostics());
        }
        DesignerCommandSession savedC1 = first.session().markSaved();
        byte[] sourceS2 = (new String(
                savedC1.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                + plan.sourceSuffix()).getBytes(StandardCharsets.UTF_8);
        DesignerCommandSession sourceAnchored =
                savedC1.reanchorSavedSource(sourceS2);
        var second = sourceAnchored.apply(
                setDataProperty(plan.secondCommandValue()));
        if (!second.changed()) {
            throw new AssertionError(
                    "physical-command budget probe could not derive C2: "
                    + second.diagnostics());
        }
        DesignerCommandSession savedC2 = second.session().markSaved();

        long bRevisionId = baselineSession.current().revisionId();
        long c1RevisionId = savedC1.current().revisionId();
        DesignerCommandRevision physicalBWithS0 = savedC2
                .rederiveRetainedRevisionByProjectingAnchor(
                        bRevisionId,
                        baselineSession.current().dartCandidateBytes());
        DesignerCommandRevision physicalC1WithS0 = savedC2
                .rederiveRetainedRevisionByProjectingAnchor(
                        c1RevisionId,
                        savedC1.current().dartCandidateBytes());
        DesignerCommandRevision physicalC1WithS2 = savedC2
                .rederiveRetainedRevisionByProjectingAnchor(
                        c1RevisionId,
                        sourceS2);

        long retainedBytes = 0;
        DesignerCommandRevision[] physicalEndpoints = {
            physicalBWithS0,
            physicalC1WithS0,
            physicalC1WithS2,
            savedC2.current()
        };
        for (DesignerCommandRevision endpoint : physicalEndpoints) {
            retainedBytes = Math.addExact(
                    retainedBytes,
                    retainedPairBytes(
                            endpoint.dartCandidateBytes(), endpoint.fdBytes()));
        }

        DesignerCommandSession atC1 = savedC2.undo().session();
        var third = atC1.applyFromPhysicalEndpoint(
                setDataProperty(plan.physicalCommandValue()),
                physicalC1WithS0.preparedPair().orElseThrow());
        if (!third.changed()) {
            throw new AssertionError(
                    "physical-command budget probe could not derive C3: "
                    + third.diagnostics());
        }
        long candidateBytes = retainedPairBytes(
                third.session().current().dartCandidateBytes(),
                third.session().current().fdBytes());
        if (candidateBytes <= 0
                || Math.addExact(retainedBytes, candidateBytes)
                    <= retainedBytes) {
            throw new AssertionError(
                    "physical-command budget probe did not add a positive C3 endpoint");
        }
        return retainedBytes;
    }

    private TestPair createPair(String baseName) throws Exception {
        return createRawPair(
                baseName,
                SOURCE.getBytes(StandardCharsets.UTF_8),
                canonicalFdBytes(baseName + ".dart"));
    }

    private TestPair createRawPair(
            String baseName,
            byte[] dartBytes,
            byte[] fdBytes) throws Exception {
        Path folderPath = Files.createDirectory(
                temporaryDirectory.resolve(baseName));
        Files.createDirectories(folderPath.resolve("lib"));
        Files.createDirectories(folderPath.resolve(".fd_templates"));
        Files.writeString(folderPath.resolve("pubspec.yaml"), """
                name: pair_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(folderPath);
        Path dartPath = folderPath.resolve("lib/" + baseName + ".dart");
        Path designerPath = folderPath.resolve(
                ".fd_templates/" + baseName + ".fd");
        Files.write(dartPath, dartBytes);
        Files.write(designerPath, fdBytes);
        FileUtil.refreshFor(folderPath.toFile());
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        FileObject designer = FileUtil.toFileObject(designerPath.toFile());
        assertNotNull(dart);
        assertNotNull(designer);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                .dataObject(dart, project);
        return new TestPair(
                dartPath,
                designerPath,
                dart,
                designer,
                dataObject,
                dataObject.getPairSaveCoordinator());
    }

    private static byte[] canonicalFdBytes(String dartFile) throws Exception {
        DartSourceDescriptor sourceDescriptor = new DartSourceDescriptor(
                dartFile,
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("test-profile"),
                new ManagedRegions(
                        new ManagedRegion(hash(SOURCE_IMPORTS)),
                        new ManagedRegion(hash(SOURCE_BUILD))));
        return new FdDocumentCodec()
                .encode(designerDocument(sourceDescriptor, "fixture"))
                .copyBytes();
    }

    private static FlutterDesignerDocumentState.Current awaitCurrent(
            FlutterDesignerDocumentController controller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError("Designer load failed: " + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for current Designer state; found "
                + controller.state());
    }

    private static FlutterDesignerDocumentState.Current awaitCurrentWithFd(
            FlutterDesignerDocumentController controller,
            byte[] expectedFd) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current
                    && Arrays.equals(expectedFd,
                            current.decoded().original().copyBytes())) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError(
                        "Designer reload failed while awaiting saved C1: "
                        + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for exact saved C1 Designer state; found "
                + controller.state());
    }

    private static FlutterDesignerDocumentState.Current awaitCurrentWithPair(
            FlutterDesignerDocumentController controller,
            byte[] expectedFd,
            byte[] expectedDart) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current
                    && Arrays.equals(expectedFd,
                            current.decoded().original().copyBytes())
                    && current.sourceIntegrity().flatMap(value ->
                            value.original()).filter(value ->
                            value.contentEquals(expectedDart)).isPresent()) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError(
                        "Designer reload failed while awaiting an exact saved pair: "
                        + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for exact saved Dart/.fd Designer state; found "
                + controller.state());
    }

    private static LiveDartDocumentSnapshot guardedLiveSnapshot(
            FlutterDesignerEditorSupport editor,
            byte[] baselineDart) throws Exception {
        openGuardedSourceDocument(editor);
        LiveDartDocumentSnapshot snapshot = editor.liveSnapshot();
        assertArrayEquals(baselineDart, snapshot.markerBearingUtf8());
        return snapshot;
    }

    private static StyledDocument openGuardedSourceDocument(
            FlutterDesignerEditorSupport editor) throws Exception {
        // Plain Surefire does not boot the NetBeans MIME registration layer.
        // Install the same production bridge/provider pair before the real
        // EditorSupport load; every subsequent reader/snapshot/apply/save
        // operation is unchanged.
        assertNull(editor.getDocument(),
                "the guarded provider must be installed before document load");
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
        StyledDocument document = editor.openDocument();
        Field wrappers = BaseDocument.class.getDeclaredField(
                "undoEditWrappers");
        wrappers.setAccessible(true);
        wrappers.set(document, List.of(new DesignerUndoableEditWrapper()));
        return document;
    }

    private static byte[] withUtf8Bom(String value) {
        byte[] content = value.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[content.length + 3];
        result[0] = (byte) 0xEF;
        result[1] = (byte) 0xBB;
        result[2] = (byte) 0xBF;
        System.arraycopy(content, 0, result, 3, content.length);
        return result;
    }

    private static DartSymbolEvidence acceptedSymbol(
            DartSymbolProbe probe,
            Path target) {
        assertTrue(probe.offset() >= 0,
                () -> "Missing symbol occurrence " + probe.expectedSymbolName());
        return new DartSymbolEvidence(
                probe,
                List.of(new DartNavigationTarget(
                        "CLASS", target, 0, 1, 1, 1)),
                true,
                Optional.empty());
    }

    private static DartCandidateAnalysisResult passingAnalysis(
            PairCandidateAnalysisTicket ticket,
            Path navigationTarget,
            long versionDelta) {
        var request = ticket.request();
        DartCandidateSnapshot requested = request.snapshot();
        List<DartSymbolEvidence> symbolEvidence = request.symbolProbes().stream()
                .map(probe -> acceptedSymbol(probe, navigationTarget))
                .toList();
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.PASSED,
                new DartCandidateSnapshot(
                        requested.projectRoot(),
                        requested.dartFile(),
                        Math.addExact(requested.version(), versionDelta),
                        requested.sha256(),
                        requested.utf8Size()),
                Optional.of("1.40.1"),
                List.of(),
                symbolEvidence.size(),
                symbolEvidence,
                Optional.empty());
    }

    private static DesignerDocument designerDocument(
            DartSourceDescriptor descriptor,
            String text) {
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue(text)),
                Map.of());
        return new DesignerDocument(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                descriptor,
                root);
    }

    private static DartSourceDescriptor descriptor(
            String imports,
            String build) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("test-profile"),
                new ManagedRegions(
                        new ManagedRegion(hash(imports)),
                        new ManagedRegion(hash(build))));
    }

    private static String hash(String payload) {
        return DartManagedRegionHashing.normalizedSha256(payload);
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return operation.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                result.set(operation.call());
            } catch (Throwable caught) {
                failure.set(caught);
            }
        });
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        return result.get();
    }

    private static byte[] sourceBytes(String imports, String build) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + build
                + "  // </netbeans-flutter-designer>\n"
                + "}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static PairFileTransactionResult failedPairResult(
            PairFileTransactionStatus status,
            int forwardWriteAttempts,
            boolean rollbackAttempted,
            PairFileTransactionIssueCode code,
            String message) {
        return new PairFileTransactionResult(
                status,
                List.of(new PairFileTransactionIssue(
                        code, PairFileRole.TRANSACTION, message)),
                forwardWriteAttempts,
                rollbackAttempted);
    }

    private static void installSyntheticActiveAttempt(
            PairSaveCoordinator coordinator) throws Exception {
        Class<?> activeType = Arrays.stream(
                PairSaveCoordinator.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("ActivePairSave"))
                .findFirst()
                .orElseThrow();
        Constructor<?> constructor = activeType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object active = constructor.newInstance(
                null,
                null,
                null,
                Thread.currentThread(),
                coordinator.state().epoch() + 1);
        Field field = PairSaveCoordinator.class
                .getDeclaredField("activePairSave");
        field.setAccessible(true);
        field.set(coordinator, active);
    }

    private static DesignerCommandRevision retainedClosedCommandRevision(
            DesignerCommandSessionOrchestrator orchestrator) throws Exception {
        Field sessionField = DesignerCommandSessionOrchestrator.class
                .getDeclaredField("session");
        sessionField.setAccessible(true);
        DesignerCommandSession retained =
                (DesignerCommandSession) sessionField.get(orchestrator);
        return retained.current();
    }

    private record TestPair(
            Path dartPath,
            Path designerPath,
            FileObject dartFile,
            FileObject designerFile,
            FlutterDesignerDataObject dataObject,
            PairSaveCoordinator coordinator) {
    }

    private record StagedPair(
            TestPair pair,
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            LiveDartDocumentSnapshot baselineLive,
            LiveDartDocumentSnapshot liveCandidate,
            DartCandidateAnalysisResult analysis,
            PairSaveEvidence evidence,
            DesignerCommandSessionOrchestrator orchestrator,
            long maxRetainedPairBytes) {
    }

    private record AggregateBudgetPlan(
            String sourceSuffix,
            String secondCommandValue,
            String physicalCommandValue) {
        AggregateBudgetPlan(
                String sourceSuffix,
                String secondCommandValue) {
            this(sourceSuffix, secondCommandValue, null);
        }
    }

    private record InitialPairPlan(
            boolean legacyV1,
            DesignerCommand firstCommand) {
        InitialPairPlan {
            if (!legacyV1) {
                throw new IllegalArgumentException(
                        "The test-only initial pair plan currently models only legacy v1 input");
            }
            java.util.Objects.requireNonNull(firstCommand, "firstCommand");
        }
    }

    private record SavedPairHistoryFixture(
            StagedPair staged,
            DesignerCommandSessionOrchestrator orchestrator,
            FlutterDesignerDocumentState.Current savedCurrent,
            long oldRevisionId,
            long savedRevisionId,
            byte[] oldDart,
            byte[] oldFd,
            byte[] savedDart,
            byte[] savedFd) {
        SavedPairHistoryFixture {
            oldDart = oldDart.clone();
            oldFd = oldFd.clone();
            savedDart = savedDart.clone();
            savedFd = savedFd.clone();
        }
    }

    private record HistoricalBaselineOverlayFixture(
            StagedPair staged,
            TestPair pair,
            DesignerCommandSessionOrchestrator orchestrator,
            FlutterDesignerEditorSupport editor,
            FlutterDesignerDocumentController controller,
            DesignerCombinedUndoRedo combined,
            SaveCookie stableCookie,
            long c0RevisionId,
            long c1RevisionId,
            long c2RevisionId,
            DesignerCommandRevision historicalC0,
            LiveDartDocumentSnapshot historicalLive,
            byte[] historicalDart,
            byte[] durableSourceDart,
            byte[] baselineFd) {
        HistoricalBaselineOverlayFixture {
            historicalDart = historicalDart.clone();
            durableSourceDart = durableSourceDart.clone();
            baselineFd = baselineFd.clone();
        }

        @Override
        public byte[] historicalDart() {
            return historicalDart.clone();
        }

        @Override
        public byte[] durableSourceDart() {
            return durableSourceDart.clone();
        }

        @Override
        public byte[] baselineFd() {
            return baselineFd.clone();
        }
    }

    private record HistoricalPhysicalCommandFixture(
            SavedPairHistoryFixture history,
            TestPair pair,
            DesignerCommandSessionOrchestrator orchestrator,
            FlutterDesignerEditorSupport editor,
            DesignerCombinedUndoRedo combined,
            PairSaveEvidence durableC2,
            FlutterDesignerDocumentState.Current durableC2Current,
            byte[] sourceS2,
            DesignerCommandRevision c1WithS0,
            PairSaveCoordinator.StagedCommandSource commandSource,
            LiveDartDocumentSnapshot liveC1WithS0,
            SaveCookie stableCookie) {
        HistoricalPhysicalCommandFixture {
            sourceS2 = sourceS2.clone();
        }

        @Override
        public byte[] sourceS2() {
            return sourceS2.clone();
        }
    }

    private record ReplacementTicket(
            PairCandidateAnalysisTicket ticket,
            Path frameworkReal) {
    }

    private record FirstCommandPublicationProbe(
            List<String> callbacks,
            AtomicBoolean commandSawJointState,
            AtomicBoolean pairSawJointState) {
    }

    private enum BeforeApplyAction {
        NONE,
        FAIL_APPLY,
        CLOSE,
        CLAIM_FENCE,
        CLOSE_EXACT_B,
        EXTERNAL_EVENT_BEFORE_RESERVATION,
        EXTERNAL_EVENT_DURING_APPLY,
        EXTERNAL_EVENT_WHILE_FORWARD_ARMED,
        OWNER_CLOSE_WHILE_FORWARD_ARMED
    }
}
