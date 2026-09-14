package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.transition.DartUserSourceProjection;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;
import org.openide.awt.UndoRedo;
import org.openide.text.NbDocument;

class LiveDartDocumentBridgeTest {
    private static final String IMPORTS_PAYLOAD =
            "import 'package:flutter/widgets.dart';\n";
    private static final String BUILD_PAYLOAD = "  @override\n"
            + "  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n"
            + "  }\n";
    private static final String SOURCE = "// user-owned Привіт 😀\n"
            + "// <netbeans-flutter-designer region=\"imports\">\n"
            + IMPORTS_PAYLOAD
            + "// </netbeans-flutter-designer>\n\n"
            + "class HomePage extends StatelessWidget {\n"
            + "  // <netbeans-flutter-designer region=\"build\">\n"
            + BUILD_PAYLOAD
            + "  // </netbeans-flutter-designer>\n"
            + "}\n";

    @Test
    void capturesExactDocumentRevisionUtf8AndGuardIdentities() throws Exception {
        Loaded loaded = load(SOURCE);

        LiveDartDocumentSnapshot snapshot = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());

        assertSame(loaded.document(), snapshot.documentIdentity());
        assertTrue(snapshot.documentVersion() >= 0);
        assertArrayEquals(SOURCE.getBytes(StandardCharsets.UTF_8),
                snapshot.markerBearingUtf8());
        assertEquals(sha256(SOURCE.getBytes(StandardCharsets.UTF_8)),
                snapshot.markerBearingSha256());
        assertEquals("imports", snapshot.imports().id());
        assertEquals("build", snapshot.build().id());
        GuardedSectionManager manager = GuardedSectionManager.getInstance(loaded.document());
        assertNotNull(manager);
        assertSame(manager.findSimpleSection("imports"),
                snapshot.imports().sectionIdentity());
        assertSame(manager.findSimpleSection("build"),
                snapshot.build().sectionIdentity());
        assertEquals(SOURCE.indexOf("// <netbeans-flutter-designer region=\"imports\">"),
                snapshot.imports().sectionStartChar());
        assertEquals(SOURCE.indexOf(IMPORTS_PAYLOAD),
                snapshot.imports().payloadStartChar());
        assertEquals(SOURCE.indexOf("  // <netbeans-flutter-designer region=\"build\">"),
                snapshot.build().sectionStartChar());
        assertEquals(SOURCE.indexOf(BUILD_PAYLOAD),
                snapshot.build().payloadStartChar());
    }

    @Test
    void appliesBothRegionsAtomicallyAndPreservesMaskedMarkerLines() throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        byte[] candidate = candidate(generated);

        LiveDartDocumentSnapshot after = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate));

        assertSame(before.documentIdentity(), after.documentIdentity());
        assertTrue(after.documentVersion() > before.documentVersion());
        assertNotEquals(before.markerBearingSha256(), after.markerBearingSha256());
        assertArrayEquals(candidate, after.markerBearingUtf8());
        assertSame(before.imports().sectionIdentity(), after.imports().sectionIdentity());
        assertSame(before.build().sectionIdentity(), after.build().sectionIdentity());
        String persisted = new String(after.markerBearingUtf8(), StandardCharsets.UTF_8);
        assertTrue(persisted.startsWith("// user-owned Привіт 😀\n"));
        assertTrue(persisted.contains(
                "  // <netbeans-flutter-designer region=\"build\">\n"));
        assertTrue(persisted.contains("  // </netbeans-flutter-designer>\n"));

        AtomicReference<BadLocationException> rejected = new AtomicReference<>();
        onEdt(() -> {
            SimpleSection build = after.build().sectionIdentity();
            NbDocument.runAtomicAsUser(loaded.document(), () -> {
                try {
                    loaded.document().insertString(
                            build.getStartPosition().getOffset() + 1, "X", null);
                } catch (BadLocationException ex) {
                    rejected.set(ex);
                }
            });
            return null;
        });
        assertNotNull(rejected.get(),
                "programmatic Designer apply must not remove the user guard");
    }

    @Test
    void provedHandlerAndImportAndManagedChangesHaveOneNativeUndoRedoEdge() throws Exception {
        DartSourceTransitionPlan transition = eventTransition();
        Loaded loaded = load(new String(transition.liveSource().original().orElseThrow().copyBytes(),
                StandardCharsets.UTF_8));
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        NativeUndoProbe nativeUndo = trackNativeUndo(loaded.document());

        LiveDartDocumentSnapshot applied = onEdt(() -> LiveDartDocumentBridge.applyManagedRegions(
                loaded.provider(), before, transition.generation().generated().orElseThrow(),
                transition.candidateBytes(), transition, exact -> { }));

        assertArrayEquals(transition.candidateBytes(), applied.markerBearingUtf8());
        String candidateText = new String(applied.markerBearingUtf8(), StandardCharsets.UTF_8);
        assertTrue(candidateText.startsWith("import 'dart:async';\n"));
        assertTrue(candidateText.contains("void _onChanged(String value)"));
        assertTrue(candidateText.contains("// User body Привіт 😀"));
        assertTrue(candidateText.contains("ChangedWidget"));
        assertTrue(candidateText.contains("package:changed/widgets.dart"));
        assertEquals(1, nativeUndo.events().get(), "all proved user and guarded edits are one native edge");
        assertTrue(nativeUndo.history().canUndo());

        onEdt(() -> { nativeUndo.history().undo(); return null; });
        assertExactPredecessor(before, LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider()));
        assertFalse(nativeUndo.history().canUndo());
        assertTrue(nativeUndo.history().canRedo());

        onEdt(() -> { nativeUndo.history().redo(); return null; });
        LiveDartDocumentSnapshot redone = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        assertArrayEquals(applied.markerBearingUtf8(), redone.markerBearingUtf8());
        assertSame(before.imports().sectionIdentity(), redone.imports().sectionIdentity());
        assertSame(before.build().sectionIdentity(), redone.build().sectionIdentity());
        assertFalse(nativeUndo.history().canRedo());
    }

    @Test
    void provedHandlerFinalizerFailureRollsBackUserImportsMethodsAndManagedRegions() throws Exception {
        DartSourceTransitionPlan transition = eventTransition();
        Loaded loaded = load(new String(transition.liveSource().original().orElseThrow().copyBytes(),
                StandardCharsets.UTF_8));
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        NativeUndoProbe nativeUndo = trackNativeUndo(loaded.document());
        AtomicReference<LiveDartDocumentSnapshot> rejected = new AtomicReference<>();

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(loaded.provider(), before,
                        transition.generation().generated().orElseThrow(), transition.candidateBytes(), transition,
                        exact -> {
                            rejected.set(exact);
                            assertArrayEquals(transition.candidateBytes(), exact.markerBearingUtf8());
                            throw new IOException("synthetic event-source finalizer rejection");
                        })));

        assertTrue(failure.getMessage().contains("synthetic event-source finalizer rejection"));
        assertNotNull(rejected.get());
        LiveDartDocumentSnapshot restored = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        assertExactPredecessor(before, restored);
        assertTrue(restored.documentVersion() > rejected.get().documentVersion());
        assertNoNativeUndo(nativeUndo);
    }

    @Test
    void explicitRestoreRevertsProvedHandlerAndImportAlongsideGeneratedRegions() throws Exception {
        DartSourceTransitionPlan transition = eventTransition();
        Loaded loaded = load(new String(transition.liveSource().original().orElseThrow().copyBytes(),
                StandardCharsets.UTF_8));
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        LiveDartDocumentSnapshot applied = onEdt(() -> LiveDartDocumentBridge.applyManagedRegions(
                loaded.provider(), before, transition.generation().generated().orElseThrow(),
                transition.candidateBytes(), transition, exact -> { }));
        AtomicReference<LiveDartDocumentSnapshot> finalized = new AtomicReference<>();

        LiveDartDocumentSnapshot restored = onEdt(() -> LiveDartDocumentBridge.restoreManagedRegions(
                loaded.provider(), applied, before, finalized::set));

        assertSame(finalized.get(), restored);
        assertExactPredecessor(before, restored);
        assertTrue(restored.documentVersion() > applied.documentVersion());
        assertFalse(new String(restored.markerBearingUtf8(), StandardCharsets.UTF_8).contains("_onChanged"));
    }

    @Test
    void provedHandlerApplyRejectsAnUnprovedCandidateBeforePublishingNativeUndo() throws Exception {
        DartSourceTransitionPlan transition = eventTransition();
        Loaded loaded = load(new String(transition.liveSource().original().orElseThrow().copyBytes(),
                StandardCharsets.UTF_8));
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider());
        NativeUndoProbe nativeUndo = trackNativeUndo(loaded.document());
        byte[] forgedCandidate = (new String(transition.candidateBytes(), StandardCharsets.UTF_8)
                + "// unproved source change\n").getBytes(StandardCharsets.UTF_8);

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(loaded.provider(), before,
                        transition.generation().generated().orElseThrow(), forgedCandidate, transition, exact -> { })));

        assertTrue(failure.getMessage().contains("event source proof"));
        assertTrue(before.sameEvidence(LiveDartDocumentBridge.snapshot(loaded.document(), loaded.provider())));
        assertNoNativeUndo(nativeUndo);
    }

    @Test
    void applyFinalizerReceivesExactSnapshotWhileAtomicLockExcludesNewerEdit()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        byte[] candidate = candidate(generated);
        String userEdit = "// user edit queued during apply finalization\n";
        CountDownLatch editStarted = new CountDownLatch(1);
        CountDownLatch editFinished = new CountDownLatch(1);
        AtomicReference<Throwable> editFailure = new AtomicReference<>();
        AtomicReference<Thread> editThread = new AtomicReference<>();
        AtomicReference<LiveDartDocumentSnapshot> finalized =
                new AtomicReference<>();

        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(),
                        before,
                        generated,
                        candidate,
                        exactApplied -> {
                            finalized.set(exactApplied);
                            assertSame(before.documentIdentity(),
                                    exactApplied.documentIdentity());
                            assertTrue(exactApplied.documentVersion()
                                    > before.documentVersion());
                            assertArrayEquals(candidate,
                                    exactApplied.markerBearingUtf8());
                            Thread worker = new Thread(() -> {
                                editStarted.countDown();
                                try {
                                    loaded.document().insertString(
                                            loaded.document().getLength(),
                                            userEdit,
                                            null);
                                } catch (Throwable failure) {
                                    editFailure.set(failure);
                                } finally {
                                    editFinished.countDown();
                                }
                            }, "queued-user-edit-during-designer-apply");
                            worker.setDaemon(true);
                            editThread.set(worker);
                            worker.start();
                            try {
                                assertTrue(editStarted.await(2, TimeUnit.SECONDS));
                                assertFalse(editFinished.await(
                                        100, TimeUnit.MILLISECONDS),
                                        "the user edit must wait for apply finalization");
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                        }));

        assertSame(finalized.get(), applied,
                "the published result must be the exact finalized C2 identity");
        Thread worker = editThread.get();
        assertNotNull(worker);
        worker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(worker.isAlive());
        assertNull(editFailure.get());
        assertArrayEquals(
                (new String(candidate, StandardCharsets.UTF_8) + userEdit)
                        .getBytes(StandardCharsets.UTF_8),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    @Test
    void checkedApplyFinalizerFailureRollsBackExactPredecessorAndPublishesNoResult()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        NativeUndoProbe nativeUndo = trackNativeUndo(loaded.document());
        AtomicReference<LiveDartDocumentSnapshot> observedCandidate =
                new AtomicReference<>();
        AtomicReference<LiveDartDocumentSnapshot> publishedResult =
                new AtomicReference<>();

        IOException failure = assertThrows(IOException.class, () ->
                publishedResult.set(onEdt(() ->
                        LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(),
                        before,
                        generated,
                        candidate(generated),
                        exactApplied -> {
                            observedCandidate.set(exactApplied);
                            throw new IOException("synthetic C2 binding failure");
                        }))));

        assertTrue(failure.getMessage().contains("synthetic C2 binding failure"));
        assertNotNull(observedCandidate.get());
        assertNull(publishedResult.get(),
                "a rejected C2 finalizer must publish no applied result");
        LiveDartDocumentSnapshot rolledBack = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        assertExactPredecessor(before, rolledBack);
        assertNoNativeUndo(nativeUndo);
        assertTrue(rolledBack.documentVersion()
                > observedCandidate.get().documentVersion(),
                "rollback must invalidate the rejected C2 identity");
    }

    @Test
    void runtimeApplyFinalizerFailureAlsoRollsBackExactPredecessor()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(),
                        before,
                        generated,
                        candidate(generated),
                        exactApplied -> {
                            throw new IllegalStateException(
                                    "synthetic runtime binding failure");
                        })));

        assertTrue(failure.getMessage().contains("runtime binding failure"));
        assertArrayEquals(before.markerBearingUtf8(),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    @Test
    void rejectsAStaleRevisionWithoutChangingTheDocument() throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot expected = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        onEdt(() -> {
            loaded.document().insertString(
                    loaded.document().getLength(), "// user edit\n", null);
            return null;
        });
        LiveDartDocumentSnapshot changed = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), expected, generated, candidate(generated))));

        assertTrue(failure.getMessage().contains("changed before apply"));
        assertArrayEquals(changed.markerBearingUtf8(),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    @Test
    void candidateMismatchAtomicallyRestoresExactPredecessorWithoutNativeUndo()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        NativeUndoProbe nativeUndo = trackNativeUndo(loaded.document());
        byte[] wrongCandidate = (new String(candidate(generated), StandardCharsets.UTF_8)
                + "// unexpected\n").getBytes(StandardCharsets.UTF_8);

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, wrongCandidate)));

        assertTrue(failure.getMessage().contains("differs"));
        LiveDartDocumentSnapshot restored = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        assertExactPredecessor(before, restored);
        assertNoNativeUndo(nativeUndo);
        assertTrue(restored.documentVersion() > before.documentVersion(),
                "a rolled-back mutation must still invalidate the old revision token");
    }

    @Test
    void leasedRestoreReturnsBothManagedRegionsToExactPreApplyBytes()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));

        LiveDartDocumentSnapshot restored = onEdt(() ->
                LiveDartDocumentBridge.restoreManagedRegions(
                        loaded.provider(), applied, before, () -> { }));

        assertArrayEquals(before.markerBearingUtf8(), restored.markerBearingUtf8());
        assertTrue(restored.documentVersion() > applied.documentVersion());
        assertSame(before.imports().sectionIdentity(), restored.imports().sectionIdentity());
        assertSame(before.build().sectionIdentity(), restored.build().sectionIdentity());
    }

    @Test
    void restoreFinalizerKeepsTheAtomicLockUntilAWaitingUserEditCanBecomeNewer()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        String userEdit = "// user edit queued during restore finalization\n";
        CountDownLatch editStarted = new CountDownLatch(1);
        CountDownLatch editFinished = new CountDownLatch(1);
        AtomicReference<Throwable> editFailure = new AtomicReference<>();
        AtomicReference<Thread> editThread = new AtomicReference<>();
        AtomicReference<LiveDartDocumentSnapshot> finalized =
                new AtomicReference<>();

        LiveDartDocumentSnapshot restored = onEdt(() ->
                LiveDartDocumentBridge.restoreManagedRegions(
                        loaded.provider(), applied, before, exactRestored -> {
                            finalized.set(exactRestored);
                            assertArrayEquals(before.markerBearingUtf8(),
                                    exactRestored.markerBearingUtf8());
                            Thread worker = new Thread(() -> {
                                editStarted.countDown();
                                try {
                                    loaded.document().insertString(
                                            loaded.document().getLength(),
                                            userEdit,
                                            null);
                                } catch (Throwable failure) {
                                    editFailure.set(failure);
                                } finally {
                                    editFinished.countDown();
                                }
                            }, "queued-user-edit-during-designer-restore");
                            worker.setDaemon(true);
                            editThread.set(worker);
                            worker.start();
                            try {
                                assertTrue(editStarted.await(2, TimeUnit.SECONDS));
                                assertFalse(editFinished.await(
                                        100, TimeUnit.MILLISECONDS),
                                        "the user edit must wait for restore finalization");
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                        }));

        assertSame(finalized.get(), restored,
                "the published result must be the exact finalized C1 identity");
        Thread worker = editThread.get();
        assertNotNull(worker);
        worker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(worker.isAlive());
        assertNull(editFailure.get());
        assertArrayEquals(before.markerBearingUtf8(), restored.markerBearingUtf8(),
                "the restore result must remain exact B");
        assertArrayEquals(
                (SOURCE + userEdit).getBytes(StandardCharsets.UTF_8),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
        assertSame(before.documentIdentity(), loaded.document(),
                "the newer edit must remain in the same live document");
    }

    @Test
    void checkedRestoreFinalizerFailurePublishesNoRestoredSemanticIdentity()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        AtomicReference<LiveDartDocumentSnapshot> observedRestored =
                new AtomicReference<>();
        AtomicReference<LiveDartDocumentSnapshot> publishedResult =
                new AtomicReference<>();

        IOException failure = assertThrows(IOException.class, () ->
                publishedResult.set(onEdt(() ->
                        LiveDartDocumentBridge.restoreManagedRegions(
                        loaded.provider(),
                        applied,
                        before,
                        exactRestored -> {
                            observedRestored.set(exactRestored);
                            throw new IOException(
                                    "synthetic predecessor rebind failure");
                        }))));

        assertTrue(failure.getMessage().contains("predecessor rebind failure"));
        assertNotNull(observedRestored.get());
        assertNull(publishedResult.get(),
                "a failed finalizer must not expose a restored semantic identity");
        assertArrayEquals(before.markerBearingUtf8(),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    @Test
    void leasedRestoreRefusesToOverwriteARevisionChangedAfterApply()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        onEdt(() -> {
            loaded.document().insertString(
                    loaded.document().getLength(), "// user edit after apply\n", null);
            return null;
        });
        LiveDartDocumentSnapshot userRevision = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.restoreManagedRegions(
                        loaded.provider(), applied, before, () -> { })));

        assertTrue(failure.getMessage().contains("changed after"));
        assertArrayEquals(userRevision.markerBearingUtf8(),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    @Test
    void committedPairVerificationPreservesNativeHistoryAndReturnsExactSnapshot()
            throws Exception {
        Loaded loaded = load(SOURCE);
        NativeUndoProbe undo = trackNativeUndo(loaded.document());
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        assertTrue(undo.history().canUndo());
        int nativeEvents = undo.events().get();

        LiveDartDocumentSnapshot verified = onEdt(() ->
                LiveDartDocumentBridge
                        .verifyCommittedPairWithoutHistoryMutation(
                                loaded.provider(),
                                applied,
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_FULL_CONTENT));

        assertTrue(applied.sameEvidence(verified));
        assertTrue(undo.history().canUndo(),
                "read-only commit verification must retain the native edit");
        assertEquals(nativeEvents, undo.events().get(),
                "read-only commit verification must publish no document edit");
    }

    @Test
    void committedPairVerificationAllowsUnmanagedSourceOnlyUnderManagedPolicy()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        String sourceEdit = "// Source S2 above committed M1\n";
        onEdt(() -> {
            loaded.document().insertString(0, sourceEdit, null);
            return null;
        });

        LiveDartDocumentSnapshot retained = onEdt(() ->
                LiveDartDocumentBridge
                        .verifyCommittedPairWithoutHistoryMutation(
                                loaded.provider(),
                                applied,
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_MANAGED_CONTENT));
        IOException fullMismatch = assertThrows(IOException.class, () ->
                onEdt(() -> LiveDartDocumentBridge
                        .verifyCommittedPairWithoutHistoryMutation(
                                loaded.provider(),
                                applied,
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_FULL_CONTENT)));

        assertTrue(applied.sameManagedContent(retained));
        assertTrue(new String(retained.markerBearingUtf8(), StandardCharsets.UTF_8)
                .startsWith(sourceEdit));
        assertTrue(fullMismatch.getMessage().contains(
                "complete live Dart source differs"));
    }

    @Test
    void committedPairVerificationRejectsManagedContentMismatch()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        onEdt(() -> {
            applied.build().sectionIdentity().setText(
                    before.build().originalReplacementText());
            return null;
        });

        IOException mismatch = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge
                        .verifyCommittedPairWithoutHistoryMutation(
                                loaded.provider(),
                                applied,
                                LiveDartDocumentBridge
                                        .CommittedPairContentPolicy
                                        .EXACT_MANAGED_CONTENT)));

        assertTrue(mismatch.getMessage().contains("managed Dart regions changed"));
    }

    @Test
    void committedPairSealAllowsUnmanagedEditAndEstablishesUndoBarrier()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        onEdt(() -> {
            loaded.document().insertString(0, "// concurrent user edit\n", null);
            return null;
        });
        AtomicBoolean barrier = new AtomicBoolean();

        LiveDartDocumentSnapshot sealed = onEdt(() ->
                LiveDartDocumentBridge.sealCommittedPair(
                        loaded.provider(), applied, () -> barrier.set(true)));

        assertTrue(barrier.get());
        assertTrue(applied.sameManagedContent(sealed));
        assertTrue(sealed.documentVersion() > applied.documentVersion());
    }

    @Test
    void committedPairSealRejectsManagedUndoButStillClearsUndoHistory()
            throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        onEdt(() -> {
            applied.build().sectionIdentity().setText(
                    before.build().originalReplacementText());
            return null;
        });
        AtomicBoolean barrier = new AtomicBoolean();

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.sealCommittedPair(
                        loaded.provider(), applied, () -> barrier.set(true))));

        assertTrue(failure.getMessage().contains("managed Dart regions changed"));
        assertTrue(barrier.get(), "unsafe pre-commit Undo history must still be cleared");
    }

    @Test
    void strictWritableSliceRejectsBomCrLfAndOffEdtApply() throws Exception {
        Loaded loaded = load(SOURCE);
        LiveDartDocumentSnapshot before = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        GeneratedDartRegions generated = generatedExampleRegions();

        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'x'};
        IOException bomFailure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, bom)));
        assertTrue(bomFailure.getMessage().contains("BOM"));

        byte[] crlf = new String(candidate(generated), StandardCharsets.UTF_8)
                .replace("\n", "\r\n")
                .getBytes(StandardCharsets.UTF_8);
        IOException crlfFailure = assertThrows(IOException.class, () -> onEdt(() ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, crlf)));
        assertTrue(crlfFailure.getMessage().contains("LF"));

        assertThrows(IllegalStateException.class, () ->
                LiveDartDocumentBridge.applyManagedRegions(
                        loaded.provider(), before, generated, candidate(generated)));
        assertArrayEquals(before.markerBearingUtf8(),
                LiveDartDocumentBridge.snapshot(
                        loaded.document(), loaded.provider()).markerBearingUtf8());
    }

    private static Loaded load(String source) throws Exception {
        StyledDocument document = (StyledDocument) new DartEditorKit().createDefaultDocument();
        DartGuardedSectionsProvider provider = new DartGuardedSectionsProvider(() -> document);
        try (Reader reader = provider.createGuardedReader(
                new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8)) {
            document.insertString(0, readAll(reader), null);
        }
        return new Loaded(document, provider);
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder value = new StringBuilder();
        char[] buffer = new char[512];
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            value.append(buffer, 0, count);
        }
        return value.toString();
    }

    private static NativeUndoProbe trackNativeUndo(StyledDocument document) {
        UndoRedo.Manager history = new UndoRedo.Manager();
        AtomicInteger events = new AtomicInteger();
        document.addUndoableEditListener(history);
        document.addUndoableEditListener(event -> events.incrementAndGet());
        return new NativeUndoProbe(history, events);
    }

    private static void assertNoNativeUndo(NativeUndoProbe probe) {
        assertEquals(0, probe.events().get(),
                "a rolled-back apply must publish no native Undo event");
        assertFalse(probe.history().canUndo(),
                "a rolled-back apply must leave no phantom native Undo entry");
        assertFalse(probe.history().canRedo(),
                "a rolled-back apply must leave no phantom native Redo entry");
    }

    private static void assertExactPredecessor(
            LiveDartDocumentSnapshot expected,
            LiveDartDocumentSnapshot restored) {
        assertSame(expected.documentIdentity(), restored.documentIdentity());
        assertArrayEquals(expected.markerBearingUtf8(), restored.markerBearingUtf8());
        assertEquals(expected.maskedContent(), restored.maskedContent());
        assertExactGuard(expected.imports(), restored.imports());
        assertExactGuard(expected.build(), restored.build());
    }

    private static void assertExactGuard(
            LiveDartDocumentSnapshot.ManagedSection expected,
            LiveDartDocumentSnapshot.ManagedSection restored) {
        assertEquals(expected.id(), restored.id());
        assertSame(expected.sectionIdentity(), restored.sectionIdentity());
        assertTrue(expected.sectionIdentity().isValid());
        assertTrue(restored.sectionIdentity().isValid());
        assertEquals(expected.sectionStartChar(), restored.sectionStartChar());
        assertEquals(expected.sectionEndChar(), restored.sectionEndChar());
        assertEquals(expected.payloadStartChar(), restored.payloadStartChar());
        assertEquals(expected.payloadEndChar(), restored.payloadEndChar());
        assertEquals(expected.originalSectionText(), restored.originalSectionText());
    }

    private static GeneratedDartRegions generatedExampleRegions() throws Exception {
        return generatedExampleRegions("SampleWidget", "package:example/widgets.dart");
    }

    private static GeneratedDartRegions generatedExampleRegions(String dartClass, String importUri) throws Exception {
        String modelJson = """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "home_page.dart",
                    "className": "HomePage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {"sha256": "%s"},
                      "build": {"sha256": "%s"}
                    }
                  },
                  "root": {
                    "id": "35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce",
                    "type": "example.widgets.SampleWidget",
                    "properties": {},
                    "slots": {}
                  }
                }
                """.formatted("0".repeat(64), "0".repeat(64));
        FdDecodeResult.Current decoded = (FdDecodeResult.Current) new FdDocumentCodec()
                .decode(modelJson.getBytes(StandardCharsets.UTF_8));
        DesignerDocument document = decoded.document();
        WidgetDefinition definition = new WidgetDefinition(
                new WidgetTypeId("example.widgets.SampleWidget"),
                dartClass,
                Optional.empty(),
                true,
                importUri,
                List.of(importUri),
                Set.of(),
                new PaletteMetadata("example", 10, 10, "Sample Widget"),
                List.of(),
                List.of());
        return new DartRegionGenerator()
                .generate(document, WidgetCatalog.strict(List.of(definition)))
                .generated()
                .orElseThrow();
    }

    private static DartSourceTransitionPlan eventTransition() throws Exception {
        GeneratedDartRegions baselineGenerated = generatedExampleRegions();
        GeneratedDartRegions nextGenerated = generatedExampleRegions("ChangedWidget", "package:changed/widgets.dart");
        byte[] original = candidate(baselineGenerated);
        var descriptor = new DartSourceDescriptor("home_page.dart", "HomePage", WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(
                        new ManagedRegion(baselineGenerated.imports().normalizedSha256()),
                        new ManagedRegion(baselineGenerated.build().normalizedSha256())));
        var baselineGeneration = new DartGenerationResult(new ValidationResult(List.of()),
                Optional.of(baselineGenerated), List.of());
        var prospectiveGeneration = new DartGenerationResult(new ValidationResult(List.of()),
                Optional.of(nextGenerated), List.of());
        var baseline = new DartThreeWayIntegrityGate().evaluate(
                new DartSourceIntegrityScanner().scan(original, descriptor), descriptor, baselineGeneration);
        var projection = DartUserSourceProjection.identity(original, descriptor).insertHandler(
                "HomePage", "_onChanged", "void _onChanged(String value) {\n  // User body Привіт 😀\n}",
                List.of("dart:async"));
        var result = new DartSourceTransitionPlanner().plan(baseline, original, descriptor,
                prospectiveGeneration, projection);
        assertEquals(DartSourceTransitionStatus.READY, result.status(), result.diagnostics().toString());
        return result.plan().orElseThrow();
    }

    private static byte[] candidate(GeneratedDartRegions generated) {
        return SOURCE
                .replace(IMPORTS_PAYLOAD, generated.imports().payload())
                .replace(BUILD_PAYLOAD, generated.build().payload())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value));
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
            } catch (Throwable ex) {
                failure.set(ex);
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

    private record Loaded(
            StyledDocument document,
            DartGuardedSectionsProvider provider) {
    }

    private record NativeUndoProbe(
            UndoRedo.Manager history,
            AtomicInteger events) {
    }
}
