package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalysisStatus;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionResult;
import dev.flutter.netbeans.designer.command.DesignerCommandStatus;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationPlanner;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.StyledDocument;
import javax.swing.undo.AbstractUndoableEdit;
import org.junit.jupiter.api.Test;
import org.openide.awt.UndoRedo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignerCommandSessionOrchestratorTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId ROOT_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final StableId FIRST_ID = StableId.parse(
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final StableId SECOND_ID = StableId.parse(
            "dddddddd-dddd-4ddd-8ddd-dddddddddddd");
    private static final PropertyName DATA = new PropertyName("data");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void pureCursorOwnsLifetimeTokenWhileCombinedHistoryRemainsNative()
            throws Exception {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        DesignerCommandSession initial = session();
        AtomicInteger changes = new AtomicInteger();
        combined.addChangeListener(event -> changes.incrementAndGet());
        source.undoableEditHappened(new UndoableEditEvent(
                source,
                new AbstractUndoableEdit() {
                    @Override
                    public String getPresentationName() {
                        return "Native source edit";
                    }
                }));
        int afterNativeEdit = changes.get();

        assertTrue(DesignerCommandSessionOrchestrator.PUBLIC_MUTATION_UI_ENABLED,
                "the verified Properties slice must be publicly enabled");
        assertTrue(DesignerCommandSessionOrchestrator
                        .PUBLIC_PALETTE_CATALOG_INSERT_DND_ENABLED,
                "the catalog-validated Palette insertion slice must be publicly enabled");
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            assertTrue(combined.designerSessionActive());
            assertFalse(orchestrator.dirty());
            assertTrue(combined.canUndo());
            assertEquals("Undo Native source edit",
                    combined.getUndoPresentationName());
            assertEquals(afterNativeEdit, changes.get(),
                    "binding a pure session must not publish native presentation");

            var applied = applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("after")));
            assertEquals(DesignerCommandStatus.APPLIED, applied.status());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
            assertTrue(combined.canUndo());
            assertEquals("Undo Native source edit",
                    combined.getUndoPresentationName());
            assertEquals(afterNativeEdit, changes.get(),
                    "pure command publication must not select a second public history");
            assertEquals(
                    DesignerRevisionPersistenceKind.PAIRED,
                    orchestrator.currentRevision().persistenceKind());

            combined.undo();
            assertTrue(orchestrator.dirty(),
                    "public native Undo must not move the pure command cursor");
            assertTrue(combined.canRedo());

            orchestrator.undo();
            assertFalse(orchestrator.dirty(),
                    "Undo to the exact saved revision must clear dirty state");
            assertTrue(orchestrator.canRedo());

            orchestrator.redo();
            DesignerCommandSessionOrchestrator.DurableSaveLease committed =
                    orchestrator.beginDurableSave();
            assertSame(orchestrator.currentRevision(), committed.revision());
            assertFalse(orchestrator.canUndo(),
                    "a pre-commit lease must pin the exact pure cursor");
            assertTrue(combined.canRedo(),
                    "pinning the pure cursor must not hide native history");
            committed.adoptCommitted();
            assertFalse(orchestrator.dirty());
            assertEquals(
                    DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind(),
                    "a durable commit must re-anchor the exact current pair");

            int beforeNoChange = changes.get();
            var noChange = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("after")));
            assertEquals(DesignerCommandStatus.NO_CHANGE,
                    noChange.result().status());
            assertTrue(noChange.lease().isEmpty());
            assertEquals(beforeNoChange, changes.get(),
                    "a rejected/no-op pure command must not perturb Undo actions");
        }

        assertFalse(combined.designerSessionActive());
        assertTrue(combined.canRedo(),
                "closing the lifetime token must leave native history unchanged");
    }

    @Test
    void durableLeaseRequiresDirtyCursorAndAbortRetainsExactRevision()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(session(), combined)) {
            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("after")));
            var exactDirtyRevision = orchestrator.currentRevision();

            DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                    orchestrator.beginDurableSave();

            assertSame(exactDirtyRevision, lease.revision());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    () -> orchestrator.beginCommand(new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("blocked"))));
            assertThrows(IllegalStateException.class, orchestrator::undo);
            assertThrows(IllegalStateException.class, orchestrator::redo);
            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);

            lease.abort();
            lease.abort();
            assertSame(exactDirtyRevision, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());

            DesignerCommandSessionOrchestrator.DurableSaveLease replacement =
                    orchestrator.beginDurableSave();
            assertThrows(IllegalStateException.class,
                    lease::adoptCommitted,
                    "a resolved lease cannot adopt a later exact reservation");
            replacement.abort();
        }
    }

    @Test
    void durableLeaseExposesStableReanchoredRevisionLookup()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCommandRevision revision0 = initial.current();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision revision1 =
                    orchestrator.currentRevision();
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C2")));
            DesignerCommandRevision revision2 =
                    orchestrator.currentRevision();
            List<DesignerCommandRevision> leasedRevisions = List.of(
                    revision0, revision1, revision2);

            try (DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                    orchestrator.beginDurableSave()) {
                assertSame(revision2, lease.revision());
                assertEquals(revision2.revisionId(), lease.savedRevisionId());
                for (DesignerCommandRevision leased : leasedRevisions) {
                    DesignerCommandRevision reanchored =
                            lease.reanchoredRevision(leased.revisionId());
                    assertEquals(leased.revisionId(),
                            reanchored.revisionId());
                    assertNotSame(leased, reanchored,
                            "the precomputed saved session must expose its newly derived identity");
                }
                assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                        lease.reanchoredRevision(lease.savedRevisionId())
                                .persistenceKind());
                assertThrows(IllegalArgumentException.class,
                        () -> lease.reanchoredRevision(-1));
                assertThrows(IllegalArgumentException.class,
                        () -> lease.reanchoredRevision(Long.MAX_VALUE));
            }

            assertSame(revision2, orchestrator.currentRevision(),
                    "lookup and lease abort must not change save behavior");
            assertTrue(orchestrator.dirty());
        }
    }

    @Test
    void exactPairDurableLeaseRetainsPhysicalOverlayAndTemplateVariants()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCommandRevision historical = initial.current();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("physical")));
            DesignerCommandRevision canonical = orchestrator.currentRevision();
            PreparedDesignerPair exactPair = exactOverlayPair(
                    canonical, "// exact native overlay\n");
            assertFalse(Arrays.equals(
                    canonical.dartCandidateBytes(),
                    exactPair.prospectiveDartBytes()));

            DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                    orchestrator.beginDurableSave(exactPair);

            assertSame(canonical, lease.revision());
            assertEquals(initial.limits().maxRetainedPairBytes(),
                    lease.maxRetainedPairBytes());
            assertArrayEquals(
                    exactPair.prospectiveDartBytes(),
                    lease.reanchoredRevision(lease.savedRevisionId())
                            .dartCandidateBytes());
            byte[] firstTemplate = exactPair.prospectiveDartBytes();
            byte[] secondTemplate = (new String(
                    firstTemplate, StandardCharsets.UTF_8)
                    + "// second physical template\n")
                    .getBytes(StandardCharsets.UTF_8);
            DesignerCommandRevision first =
                    lease.reanchoredPhysicalRevision(
                            historical.revisionId(), firstTemplate);
            DesignerCommandRevision second =
                    lease.reanchoredPhysicalRevision(
                            historical.revisionId(), secondTemplate);
            assertNotSame(first, second);
            assertEquals(historical.revisionId(), first.revisionId());
            assertEquals(historical.revisionId(), second.revisionId());
            assertArrayEquals(first.fdBytes(), second.fdBytes());
            assertFalse(Arrays.equals(
                    first.dartCandidateBytes(), second.dartCandidateBytes()));
            assertArrayEquals(firstTemplate,
                    first.preparedPair().orElseThrow().liveDartBytes());
            assertArrayEquals(secondTemplate,
                    second.preparedPair().orElseThrow().liveDartBytes());

            lease.adoptCommitted();
            assertFalse(orchestrator.dirty());
            assertArrayEquals(
                    exactPair.prospectiveDartBytes(),
                    orchestrator.currentRevision().dartCandidateBytes());
        }
    }

    @Test
    void exactPairLeaseProjectsNewAnchorIntoHistoricalPhysicalEnvelope()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision canonicalC1 =
                    orchestrator.currentRevision();
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C2")));
            DesignerCommandRevision canonicalC2 =
                    orchestrator.currentRevision();
            PreparedDesignerPair exactC2 = exactOverlayPair(
                    canonicalC2, "// durable S2 envelope\n");

            try (DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                    orchestrator.beginDurableSave(exactC2)) {
                DesignerCommandRevision physicalC1 =
                        lease.reanchoredPhysicalRevisionByProjectingAnchor(
                                canonicalC1.revisionId(),
                                canonicalC1.dartCandidateBytes());
                PreparedDesignerPair pair =
                        physicalC1.preparedPair().orElseThrow();

                assertEquals(canonicalC1.revisionId(),
                        physicalC1.revisionId());
                assertArrayEquals(
                        exactC2.prospectiveDartBytes(),
                        pair.baselineDartBytes());
                assertArrayEquals(
                        canonicalC2.dartCandidateBytes(),
                        pair.liveDartBytes());
                assertArrayEquals(
                        canonicalC1.dartCandidateBytes(),
                        pair.prospectiveDartBytes());

                byte[] unsupported = new String(
                        canonicalC1.dartCandidateBytes(),
                        StandardCharsets.UTF_8)
                        .replace(
                                "region=\"build\"",
                                "region=\"future\"")
                        .getBytes(StandardCharsets.UTF_8);
                assertThrows(IllegalArgumentException.class,
                        () -> lease
                                .reanchoredPhysicalRevisionByProjectingAnchor(
                                        canonicalC1.revisionId(),
                                        unsupported));
            }
        }
    }

    @Test
    void fdOnlyLeasePublishesCloneSafeExactAnchorsAndReanchorsHistory()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(
                            fdOnlySession(), combined)) {
            var candidate = orchestrator.currentRevision();
            assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                    candidate.persistenceKind());

            DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                    orchestrator.beginDurableSave();
            byte[] durableFd = lease.durableFdBytes();
            byte[] durableDart = lease.durableDartBytes();

            assertSame(candidate, lease.revision());
            assertArrayEquals(durableDart, candidate.dartCandidateBytes());
            assertFalse(Arrays.equals(durableFd, candidate.fdBytes()));
            durableFd[0] ^= 0x7f;
            durableDart[0] ^= 0x7f;
            assertNotEquals(durableFd[0], lease.durableFdBytes()[0]);
            assertNotEquals(durableDart[0], lease.durableDartBytes()[0]);

            lease.adoptCommitted();
            lease.adoptCommitted();
            assertFalse(orchestrator.dirty());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    orchestrator.currentRevision().persistenceKind());

            orchestrator.undo();
            assertTrue(orchestrator.dirty());
            assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                    orchestrator.currentRevision().persistenceKind());
            orchestrator.redo();
            assertFalse(orchestrator.dirty());
        }
    }

    @Test
    void metadataDurableLeaseClonesEnvelopeAbortsExactlyAndRetainsHistoryOnRetry()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        try (var orchestrator = new DesignerCommandSessionOrchestrator(fdOnlySession(), combined)) {
            var candidate = orchestrator.currentRevision();
            applyAndAdopt(orchestrator, new MoveWidget(
                    FIRST_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 0)));
            var redoRevision = orchestrator.currentRevision();
            orchestrator.undo();
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.canUndo());
            assertTrue(orchestrator.canRedo());
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> callbacks.incrementAndGet());
            byte[] envelope = sourceOverlay(candidate, "// exact unmanaged source envelope\n");
            byte[] expectedEnvelope = envelope.clone();
            var aborted = orchestrator.beginMetadataDurableSave(envelope);
            envelope[0] ^= 0x7f;
            var proposedSaved = aborted.reanchoredRevision(aborted.savedRevisionId());
            assertArrayEquals(expectedEnvelope, proposedSaved.dartCandidateBytes());
            byte[] exposed = proposedSaved.dartCandidateBytes();
            exposed[0] ^= 0x7f;
            assertArrayEquals(expectedEnvelope, proposedSaved.dartCandidateBytes());
            assertArrayEquals(candidate.dartCandidateBytes(), aborted.durableDartBytes(),
                    "the lease retains the previous durable anchor for exact persistence guards");
            assertSame(candidate, aborted.revision());
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            aborted.abort();
            int afterAbort = callbacks.get();
            assertEquals(2, afterAbort, "begin and abort each publish one state change");
            aborted.abort();
            assertEquals(afterAbort, callbacks.get());
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
            assertTrue(orchestrator.canRedo());
            orchestrator.redo();
            assertSame(redoRevision, orchestrator.currentRevision());
            orchestrator.undo();
            assertSame(candidate, orchestrator.currentRevision());

            var retry = orchestrator.beginMetadataDurableSave(expectedEnvelope);
            byte[] durableFd = candidate.fdBytes();
            retry.adoptCommitted();
            var saved = orchestrator.currentRevision();
            assertEquals(candidate.revisionId(), saved.revisionId());
            assertArrayEquals(expectedEnvelope, saved.dartCandidateBytes());
            assertArrayEquals(durableFd, saved.fdBytes());
            assertFalse(orchestrator.dirty());
            assertTrue(orchestrator.canRedo(), "saving metadata must retain the existing redo branch");
            orchestrator.undo();
            assertTrue(orchestrator.dirty());
            assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                    orchestrator.currentRevision().persistenceKind());
            assertArrayEquals(expectedEnvelope, orchestrator.currentRevision().dartCandidateBytes());
            orchestrator.redo();
            assertSame(saved, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            orchestrator.redo();
            assertEquals(redoRevision.revisionId(), orchestrator.currentRevision().revisionId());
            assertArrayEquals(expectedEnvelope, orchestrator.currentRevision().dartCandidateBytes());
        }
    }

    @Test
    void metadataDurableLeaseRejectsManagedChangesWithoutPublishingOrLosingCursor()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        try (var orchestrator = new DesignerCommandSessionOrchestrator(fdOnlySession(), combined)) {
            var candidate = orchestrator.currentRevision();
            byte[] invalid = new String(candidate.dartCandidateBytes(), StandardCharsets.UTF_8)
                    .replace("'same'", "'changed'").getBytes(StandardCharsets.UTF_8);
            assertFalse(Arrays.equals(candidate.dartCandidateBytes(), invalid));
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> callbacks.incrementAndGet());
            assertThrows(IllegalArgumentException.class,
                    () -> orchestrator.beginMetadataDurableSave(invalid));
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertEquals(0, callbacks.get());
            try (var retry = orchestrator.beginMetadataDurableSave(candidate.dartCandidateBytes())) {
                assertSame(candidate, retry.revision());
            }
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
        }
    }

    @Test
    void closeDuringDurableLeaseIsDeferredUntilCommittedAdoption()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        applyAndAdopt(orchestrator, new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("after")));
        DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                orchestrator.beginDurableSave();

        orchestrator.close();

        assertTrue(combined.designerSessionActive(),
                "close must not detach the history before post-commit adoption");
        assertSame(lease.revision(), orchestrator.currentRevision());
        lease.adoptCommitted();
        lease.adoptCommitted();
        assertFalse(combined.designerSessionActive());
        assertFalse(orchestrator.dirty());
        assertThrows(IllegalStateException.class,
                orchestrator::currentRevision);
    }

    @Test
    void closeAwareDurableAdoptionCommitsExactSavedSessionBeforePublication()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        applyAndAdopt(orchestrator, new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("after")));
        DesignerCommandRevision dirty = orchestrator.currentRevision();
        DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                orchestrator.beginDurableSave();
        DesignerCommandRevision saved = lease.reanchoredRevision(
                lease.savedRevisionId());
        var savedSessionField = DesignerCommandSessionOrchestrator
                .DurableSaveLease.class.getDeclaredField("precomputedSaved");
        savedSessionField.setAccessible(true);
        DesignerCommandSession exactSavedSession =
                (DesignerCommandSession) savedSessionField.get(lease);
        var currentSessionField = DesignerCommandSessionOrchestrator.class
                .getDeclaredField("session");
        currentSessionField.setAccessible(true);
        AtomicInteger peerCommits = new AtomicInteger();

        orchestrator.close();
        assertTrue(combined.designerSessionActive(),
                "the durable lease must defer binding close until adoption");

        var effects = lease.adoptCommittedCloseAwareDeferredEffects(
                closePending -> {
                    assertTrue(closePending,
                            "the peer commit must see the requested owner close");
                    assertSame(dirty, orchestrator.currentRevision(),
                            "the peer commit must run before the saved-session swap");
                    peerCommits.incrementAndGet();
                });

        assertEquals(1, peerCommits.get());
        assertFalse(lease.ownsExactActiveRevision());
        assertSame(exactSavedSession, currentSessionField.get(orchestrator),
                "adoption must install the lease's exact precomputed saved session");
        assertSame(saved, exactSavedSession.current());
        assertFalse(exactSavedSession.dirty());
        assertThrows(IllegalStateException.class,
                orchestrator::currentRevision,
                "adoption must finish the requested logical close");
        assertTrue(combined.designerSessionActive(),
                "binding close is an outward effect and must await publication");

        lease.adoptCommittedCloseAwareDeferredEffects(
                closePending -> peerCommits.incrementAndGet()).publish();
        assertEquals(1, peerCommits.get(),
                "an adopted durable lease must not repeat the peer commit");
        assertTrue(combined.designerSessionActive());

        effects.publish();
        assertFalse(combined.designerSessionActive());
        effects.publish();
        assertFalse(combined.designerSessionActive());
    }

    @Test
    void invalidatingDurableLeaseClosesUnsafeSession() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        applyAndAdopt(orchestrator, new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("after")));
        DesignerCommandSessionOrchestrator.DurableSaveLease lease =
                orchestrator.beginDurableSave();

        lease.invalidate();
        lease.invalidate();

        assertFalse(combined.designerSessionActive());
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertFalse(orchestrator.dirty());
        assertThrows(IllegalStateException.class,
                orchestrator::currentRevision);
        assertThrows(IllegalStateException.class,
                orchestrator::beginDurableSave);
    }

    @Test
    void sourceAnchorLeasePinsCleanRevisionAndPublishesCloneSafeAnchors()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCommandRevision historical = initial.current();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("saved")));
            orchestrator.beginDurableSave().adoptCommitted();
            DesignerCommandRevision exactSaved =
                    orchestrator.currentRevision();
            byte[] plannedDart = sourceOverlay(
                    exactSaved, "// planned unmanaged Source anchor\n");

            DesignerCommandSessionOrchestrator.SourceAnchorLease lease =
                    orchestrator.beginSourceAnchor(plannedDart);

            assertSame(exactSaved, lease.revision());
            assertSame(initial.catalog(), lease.catalogIdentity());
            assertEquals(exactSaved.revisionId(), lease.savedRevisionId());
            assertTrue(lease.ownsExactActiveRevision());
            assertSame(exactSaved, orchestrator.currentRevision(),
                    "precomputation must not move the authoritative cursor");
            assertEquals(exactSaved.revisionId(),
                    lease.reanchoredRevision(exactSaved.revisionId())
                            .revisionId());
            assertEquals(historical.revisionId(),
                    lease.reanchoredRevision(historical.revisionId())
                            .revisionId());
            assertArrayEquals(exactSaved.fdBytes(), lease.priorFdBytes());
            assertArrayEquals(exactSaved.fdBytes(), lease.candidateFdBytes());
            assertArrayEquals(exactSaved.dartCandidateBytes(),
                    lease.priorDartBytes());
            assertArrayEquals(plannedDart, lease.candidateDartBytes());
            assertArrayEquals(plannedDart,
                    lease.reanchoredRevision(lease.savedRevisionId())
                            .dartCandidateBytes());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    lease.reanchoredRevision(lease.savedRevisionId())
                            .persistenceKind());
            assertThrows(IllegalArgumentException.class,
                    () -> lease.reanchoredRevision(-1));
            assertThrows(IllegalArgumentException.class,
                    () -> lease.reanchoredRevision(Long.MAX_VALUE));

            byte[] priorFdCopy = lease.priorFdBytes();
            byte[] priorDartCopy = lease.priorDartBytes();
            byte[] candidateFdCopy = lease.candidateFdBytes();
            byte[] candidateDartCopy = lease.candidateDartBytes();
            priorFdCopy[0] ^= 0x7f;
            priorDartCopy[0] ^= 0x7f;
            candidateFdCopy[0] ^= 0x7f;
            candidateDartCopy[0] ^= 0x7f;
            assertNotEquals(priorFdCopy[0], lease.priorFdBytes()[0]);
            assertNotEquals(priorDartCopy[0], lease.priorDartBytes()[0]);
            assertNotEquals(candidateFdCopy[0], lease.candidateFdBytes()[0]);
            assertNotEquals(candidateDartCopy[0],
                    lease.candidateDartBytes()[0]);

            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    () -> orchestrator.beginCommand(new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("blocked"))));
            assertThrows(IllegalStateException.class, orchestrator::undo);
            assertThrows(IllegalStateException.class, orchestrator::redo);
            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);
            assertThrows(IllegalStateException.class,
                    () -> orchestrator.beginSourceAnchor(plannedDart));

            lease.abort();
            lease.abort();
            assertFalse(lease.ownsExactActiveRevision());
            assertSame(exactSaved, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());

            DesignerCommandSessionOrchestrator.SourceAnchorLease replacement =
                    orchestrator.beginSourceAnchor(plannedDart);
            assertThrows(IllegalStateException.class,
                    lease::adoptCommitted,
                    "a resolved Source lease cannot adopt a later reservation");
            replacement.close();
            replacement.close();
        }
    }

    @Test
    void sourceAnchorJointAdoptionRunsPeerBeforeSwapAndDefersEffects()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            DesignerCommandRevision prior = orchestrator.currentRevision();
            byte[] plannedDart = sourceOverlay(
                    prior, "// committed unmanaged Source anchor\n");
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease =
                    orchestrator.beginSourceAnchor(plannedDart);
            DesignerCommandRevision reanchored = lease.reanchoredRevision(
                    lease.savedRevisionId());
            AtomicInteger peerCommits = new AtomicInteger();
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> callbacks.incrementAndGet());
            callbacks.set(0);

            var effects = lease.adoptCommittedCloseAwareDeferredEffects(
                    closePending -> {
                        assertFalse(closePending);
                        assertSame(prior, orchestrator.currentRevision(),
                                "the peer assignment must run before the session swap");
                        peerCommits.incrementAndGet();
                    });

            assertEquals(1, peerCommits.get());
            assertEquals(0, callbacks.get(),
                    "joint adoption must retain callbacks until publication");
            assertSame(reanchored, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertArrayEquals(plannedDart,
                    orchestrator.currentRevision().dartCandidateBytes());
            assertFalse(lease.ownsExactActiveRevision());

            var repeated = lease.adoptCommittedCloseAwareDeferredEffects(
                    closePending -> peerCommits.incrementAndGet());
            assertEquals(1, peerCommits.get(),
                    "an adopted Source lease must not repeat the peer commit");
            repeated.publish();
            assertEquals(0, callbacks.get());
            effects.publish();
            effects.publish();
            assertEquals(1, callbacks.get());
        }
    }

    @Test
    void sourceAnchorPeerFailureRetainsExactActiveLease() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            DesignerCommandRevision prior = orchestrator.currentRevision();
            DesignerCommandSessionOrchestrator.SourceAnchorLease lease =
                    orchestrator.beginSourceAnchor(sourceOverlay(
                            prior, "// peer failure Source anchor\n"));
            IllegalStateException peerFailure = new IllegalStateException(
                    "synthetic Source peer failure");

            assertSame(peerFailure, assertThrows(IllegalStateException.class,
                    () -> lease.adoptCommittedCloseAwareDeferredEffects(
                            closePending -> {
                                throw peerFailure;
                            })));
            assertTrue(lease.ownsExactActiveRevision());
            assertSame(prior, orchestrator.currentRevision());
            assertFalse(orchestrator.canUndo());

            lease.abortDeferredEffects().publish();
            assertFalse(lease.ownsExactActiveRevision());
            assertSame(prior, orchestrator.currentRevision());
        }
    }

    @Test
    void sourceAnchorCloseIsDeferredAndInvalidationClosesUnsafeOwner()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        DesignerCommandRevision prior = orchestrator.currentRevision();
        DesignerCommandSessionOrchestrator.SourceAnchorLease lease =
                orchestrator.beginSourceAnchor(sourceOverlay(
                        prior, "// close-aware Source anchor\n"));

        orchestrator.close();
        assertTrue(combined.designerSessionActive(),
                "close must wait for the exact Source outcome");
        AtomicBoolean closeSeen = new AtomicBoolean();
        var effects = lease.adoptCommittedCloseAwareDeferredEffects(
                closePending -> closeSeen.set(closePending));

        assertTrue(closeSeen.get(),
                "the peer must see the already requested owner close");
        assertTrue(combined.designerSessionActive(),
                "binding close remains deferred with outward effects");
        assertThrows(IllegalStateException.class,
                orchestrator::currentRevision);
        effects.publish();
        assertFalse(combined.designerSessionActive());

        DesignerCombinedUndoRedo unsafeCombined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator unsafe =
                new DesignerCommandSessionOrchestrator(session(), unsafeCombined);
        DesignerCommandSessionOrchestrator.SourceAnchorLease uncertain =
                unsafe.beginSourceAnchor(sourceOverlay(
                        unsafe.currentRevision(),
                        "// uncertain Source anchor\n"));
        uncertain.invalidate();
        uncertain.invalidate();
        assertFalse(unsafeCombined.designerSessionActive());
        assertFalse(unsafe.canUndo());
        assertFalse(unsafe.canRedo());
        assertFalse(unsafe.dirty());
        assertThrows(IllegalStateException.class, unsafe::currentRevision);
        assertThrows(IllegalStateException.class,
                () -> unsafe.beginSourceAnchor(new byte[0]));
    }

    @Test
    void unchangedSourceAbortJoinsCloseAwarePeerWithoutSessionSwap()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        DesignerCommandRevision prior = orchestrator.currentRevision();
        DesignerCommandSessionOrchestrator.SourceAnchorLease lease =
                orchestrator.beginSourceAnchor(sourceOverlay(
                        prior, "// exact unchanged Source candidate\n"));
        orchestrator.close();
        AtomicBoolean closeSeen = new AtomicBoolean();
        AtomicInteger peerCommits = new AtomicInteger();

        var effects = lease.abortUnchangedCloseAwareDeferredEffects(
                closePending -> {
                    assertTrue(closePending);
                    assertSame(prior, orchestrator.currentRevision(),
                            "UNCHANGED adoption must retain the old session identity");
                    closeSeen.set(true);
                    peerCommits.incrementAndGet();
                });

        assertTrue(closeSeen.get());
        assertEquals(1, peerCommits.get());
        assertFalse(lease.ownsExactActiveRevision());
        assertTrue(combined.designerSessionActive(),
                "binding close must remain deferred until publication");
        assertThrows(IllegalStateException.class,
                orchestrator::currentRevision);
        lease.abortUnchangedCloseAwareDeferredEffects(
                closePending -> peerCommits.incrementAndGet()).publish();
        assertEquals(1, peerCommits.get(),
                "a resolved unchanged lease must not repeat the peer commit");
        effects.publish();
        effects.publish();
        assertFalse(combined.designerSessionActive());
    }

    @Test
    void pendingCommandRetainsExactPredecessor() throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            var predecessor = orchestrator.currentRevision();

            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")));
            var lease = attempt.lease().orElseThrow();
            var candidate = attempt.result().session().current();

            assertEquals(DesignerCommandStatus.APPLIED,
                    attempt.result().status());
            assertSame(predecessor, orchestrator.currentRevision(),
                    "analysis must observe a pending candidate without moving the cursor");
            assertFalse(orchestrator.dirty());
            assertSame(predecessor, lease.predecessorRevision());
            assertSame(candidate, lease.candidateRevision());
            assertSame(attempt.result().edit().orElseThrow(), lease.edit());
            assertTrue(lease.physicalPredecessorPairIdentity().isEmpty());
            assertSame(initial.catalog(), lease.catalogIdentity());
            assertTrue(lease.ownsExactActiveTransition());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());

            lease.abort();

            assertFalse(lease.ownsExactActiveTransition());
            assertSame(predecessor, orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
        }
    }

    @Test
    void physicalEndpointCommandLeasePinsExactPairUntilJointAdoption()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCommandSession c1 = initial.apply(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("C1")))
                .session();
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = c1.apply(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("C2")))
                .session();
        PreparedDesignerPair exactC2 = exactOverlayPair(
                c2.current(), "// durable S2 envelope\n");
        DesignerCommandSession saved = c2.markSaved(exactC2);
        DesignerCommandSession atC1 = saved.undo().session();
        PreparedDesignerPair physicalC1 = saved
                .rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(),
                        canonicalC1.dartCandidateBytes())
                .preparedPair().orElseThrow();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(atC1, combined)) {
            DesignerCommandRevision logicalPredecessor =
                    orchestrator.currentRevision();

            var attempt = orchestrator.beginCommandFromPhysicalEndpoint(
                    new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("C3")),
                    physicalC1);
            var lease = attempt.lease().orElseThrow();
            DesignerCommandRevision candidate = lease.candidateRevision();
            PreparedDesignerPair candidatePair =
                    candidate.preparedPair().orElseThrow();

            assertSame(logicalPredecessor,
                    orchestrator.currentRevision(),
                    "endpoint admission must not move the owned cursor");
            assertSame(physicalC1,
                    lease.physicalPredecessorPairIdentity().orElseThrow());
            assertSame(logicalPredecessor, lease.predecessorRevision());
            assertArrayEquals(
                    physicalC1.liveDartBytes(),
                    candidatePair.liveDartBytes());
            assertSame(
                    physicalC1.dartTransition().baseline(),
                    candidatePair.dartTransition().baseline());
            assertTrue(lease.ownsExactActiveTransition());
            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);

            lease.adoptStaged();

            assertSame(candidate, orchestrator.currentRevision());
            assertFalse(lease.ownsExactActiveTransition());
            assertTrue(orchestrator.dirty());

            var continuation = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID,
                    DATA,
                    new PropertyValue.StringValue("C4")));
            var continuationLease = continuation.lease().orElseThrow();
            PreparedDesignerPair continuationPair = continuationLease
                    .candidateRevision().preparedPair().orElseThrow();
            assertTrue(continuationLease
                    .physicalPredecessorPairIdentity().isEmpty(),
                    "the follow-up uses ordinary admission, not a second endpoint capability");
            assertArrayEquals(
                    physicalC1.liveDartBytes(),
                    continuationPair.liveDartBytes(),
                    "the adopted C3 revision must keep S0 sticky for ordinary C4 admission");
            assertFalse(new String(
                    continuationPair.prospectiveDartBytes(),
                    StandardCharsets.UTF_8)
                    .contains("durable S2 envelope"));
            assertEquals(
                    attempt.result().session().revisionCount() + 1,
                    continuation.result().session().revisionCount(),
                    "the bounded continuation adds exactly one retained revision");
            assertTrue(continuation.result().session().revisionCount()
                    <= atC1.limits().maxHistoryEdits() + 1);
            continuationLease.abort();
            assertSame(candidate, orchestrator.currentRevision());
        }
    }

    @Test
    void physicalEndpointCommandMayReturnToExactDirtyBaseline()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            PreparedDesignerPair physicalC1 = exactC1
                    .preparedPair().orElseThrow();

            var attempt = orchestrator.beginCommandFromPhysicalEndpoint(
                    exactC1,
                    new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("before")),
                    physicalC1);
            var lease = attempt.lease().orElseThrow();
            DesignerCommandRevision baseline = lease.candidateRevision();

            assertEquals(DesignerCommandStatus.APPLIED,
                    attempt.result().status());
            assertSame(exactC1, orchestrator.currentRevision(),
                    "physical admission must pin C1 until joint adoption");
            assertSame(exactC1, lease.predecessorRevision());
            assertSame(physicalC1,
                    lease.physicalPredecessorPairIdentity().orElseThrow());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    baseline.persistenceKind());
            assertTrue(baseline.preparedPair().isEmpty());
            assertTrue(baseline.sourceTransition().isEmpty());
            assertSame(physicalC1.baselineFd(), baseline.fdSnapshot());
            assertEquals(physicalC1.baselineDocument(),
                    baseline.document());
            assertArrayEquals(physicalC1.liveDartBytes(),
                    baseline.dartCandidateBytes());
            assertTrue(lease.ownsExactActiveTransition());

            Object claim = new Object();
            AtomicInteger peerCommits = new AtomicInteger();
            lease.claimForTransition(claim);
            var effects = lease.adoptExactTargetCloseAwareDeferredEffects(
                    baseline,
                    closePending -> {
                        assertFalse(closePending);
                        assertSame(exactC1, orchestrator.currentRevision());
                        peerCommits.incrementAndGet();
                    },
                    claim);

            assertEquals(1, peerCommits.get());
            assertSame(baseline, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty(),
                    "an explicit new baseline revision stays dirty until Save");
            assertTrue(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertFalse(lease.ownsExactActiveTransition());
            effects.publish();
        }
    }

    @Test
    void detachedPhysicalEndpointFailsWithoutPoisoningNextAdmission()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCommandSession c1 = initial.apply(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("C1")))
                .session();
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = c1.apply(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("C2")))
                .session();
        DesignerCommandSession saved = c2.markSaved(exactOverlayPair(
                c2.current(), "// durable S2 envelope\n"));
        DesignerCommandSession atC1 = saved.undo().session();
        PreparedDesignerPair physicalC1 = saved
                .rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(),
                        canonicalC1.dartCandidateBytes())
                .preparedPair().orElseThrow();
        PreparedDesignerPair wrongSemanticEndpoint = saved
                .rederiveRetainedRevisionByProjectingAnchor(
                        initial.current().revisionId(),
                        initial.current().dartCandidateBytes())
                .preparedPair().orElseThrow();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(atC1, combined)) {
            SetProperty command = new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C3"));

            assertThrows(IllegalArgumentException.class,
                    () -> orchestrator.beginCommandFromPhysicalEndpoint(
                            command, wrongSemanticEndpoint));
            assertSame(atC1.current(), orchestrator.currentRevision());

            var retry = orchestrator.beginCommandFromPhysicalEndpoint(
                    command, physicalC1);
            assertSame(physicalC1,
                    retry.lease().orElseThrow()
                            .physicalPredecessorPairIdentity().orElseThrow());
            retry.lease().orElseThrow().abort();
            assertSame(atC1.current(), orchestrator.currentRevision());
        }
    }

    @Test
    void adoptionMovesCursorToTheExactCandidateOnlyOnce() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")));
            var lease = attempt.lease().orElseThrow();
            var candidate = lease.candidateRevision();

            lease.adoptStaged();
            lease.adoptStaged();

            assertFalse(lease.ownsExactActiveTransition());
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
        }
    }

    @Test
    void deferredAdoptionSwapsExactCursorBeforeOneShotPublication()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            AtomicInteger orchestratorChanges = new AtomicInteger();
            AtomicInteger combinedChanges = new AtomicInteger();
            orchestrator.addChangeListener(
                    event -> orchestratorChanges.incrementAndGet());
            combined.addChangeListener(event -> combinedChanges.incrementAndGet());
            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")));
            var lease = attempt.lease().orElseThrow();
            var candidate = lease.candidateRevision();
            int orchestratorBeforeAdoption = orchestratorChanges.get();
            int combinedBeforeAdoption = combinedChanges.get();

            var effects = lease.adoptStagedDeferredEffects();

            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertFalse(lease.ownsExactActiveTransition());
            assertEquals(orchestratorBeforeAdoption, orchestratorChanges.get(),
                    "the semantic cursor swap must not publish its callback inline");
            assertEquals(combinedBeforeAdoption, combinedChanges.get(),
                    "the outward Undo/Redo bridge must remain quiet before publication");

            effects.publish();

            assertEquals(orchestratorBeforeAdoption + 1,
                    orchestratorChanges.get());
            assertEquals(combinedBeforeAdoption, combinedChanges.get(),
                    "pure semantic adoption must not publish native history");
            effects.publish();
            assertEquals(orchestratorBeforeAdoption + 1,
                    orchestratorChanges.get(),
                    "deferred effects must publish at most once");
            assertEquals(combinedBeforeAdoption, combinedChanges.get());
        }
    }

    @Test
    void deferredAdoptionClosesStateBeforeDetachingCombinedBinding()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        var attempt = orchestrator.beginCommand(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("candidate")));
        var lease = attempt.lease().orElseThrow();

        orchestrator.close();
        assertTrue(combined.designerSessionActive());

        var effects = lease.adoptStagedDeferredEffects();

        assertFalse(orchestrator.dirty());
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision,
                "the deferred close must become semantic state at adoption");
        assertTrue(combined.designerSessionActive(),
                "binding removal is an outward effect and must await publication");

        effects.publish();

        assertFalse(combined.designerSessionActive());
        effects.publish();
        assertFalse(combined.designerSessionActive());
    }

    @Test
    void closeAwareStagedAdoptionReportsPendingCloseBeforeJointCommit()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        DesignerCommandRevision predecessor = orchestrator.currentRevision();
        var lease = orchestrator.beginCommand(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                .lease().orElseThrow();
        PairSaveEvidence evidence = evidenceFor(lease.candidateRevision());
        Object claim = new Object();
        lease.claimForTransition(claim);
        AtomicInteger outwardCallbacks = new AtomicInteger();
        AtomicInteger peerCommits = new AtomicInteger();
        AtomicBoolean observedClose = new AtomicBoolean();
        orchestrator.addChangeListener(
                event -> outwardCallbacks.incrementAndGet());

        orchestrator.close();
        int callbacksBeforeAck = outwardCallbacks.get();
        assertTrue(combined.designerSessionActive(),
                "the active lease must defer binding close until adoption");

        var effects = lease.adoptStagedCloseAwareDeferredEffects(
                evidence,
                ownerClosePending -> {
                    observedClose.set(ownerClosePending);
                    assertTrue(ownerClosePending,
                            "the peer ACK must see the already requested close");
                    assertTrue(lease.ownsExactActiveTransition(),
                            "the callback must run before lease resolution");
                    assertSame(predecessor, orchestrator.currentRevision(),
                            "the callback must run before the command cursor swap");
                    assertEquals(callbacksBeforeAck, outwardCallbacks.get(),
                            "no outward callback may run under the joint monitors");
                    peerCommits.incrementAndGet();
                },
                claim);

        assertTrue(observedClose.get());
        assertEquals(1, peerCommits.get());
        assertFalse(lease.ownsExactActiveTransition());
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision,
                "finishDeferredCloseLocked must close the owner after the peer ACK");
        assertTrue(combined.designerSessionActive(),
                "binding close remains a deferred outward effect");
        assertEquals(callbacksBeforeAck, outwardCallbacks.get());

        effects.publish();

        assertFalse(combined.designerSessionActive());
        assertEquals(callbacksBeforeAck + 1, outwardCallbacks.get());
        effects.publish();
        assertEquals(callbacksBeforeAck + 1, outwardCallbacks.get());
    }

    @Test
    void closeAwareStagedCallbackRunsOnlyAfterExactAdmissionChecks()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCombinedUndoRedo foreignCombined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(session(), combined);
                DesignerCommandSessionOrchestrator foreignOrchestrator =
                    new DesignerCommandSessionOrchestrator(
                            session(), foreignCombined)) {
            var lease = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                    .lease().orElseThrow();
            var foreignLease = foreignOrchestrator.beginCommand(
                    new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("foreign")))
                    .lease().orElseThrow();
            Object claim = new Object();
            lease.claimForTransition(claim);
            AtomicInteger peerCommits = new AtomicInteger();

            assertThrows(IllegalArgumentException.class,
                    () -> lease.adoptStagedCloseAwareDeferredEffects(
                            evidenceFor(foreignLease.candidateRevision()),
                            closePending -> peerCommits.incrementAndGet(),
                            claim));
            assertThrows(IllegalStateException.class,
                    () -> lease.adoptStagedCloseAwareDeferredEffects(
                            evidenceFor(lease.candidateRevision()),
                            closePending -> peerCommits.incrementAndGet(),
                            new Object()));

            assertEquals(0, peerCommits.get(),
                    "foreign evidence or claim must never enter the peer phase");
            assertTrue(lease.ownsExactActiveTransition());
            lease.abortToExactPredecessorDeferredEffects(claim).publish();
            foreignLease.abort();
        }
    }

    @Test
    void fatalDeferredPublicationCannotChangeAdoptedCandidate()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")));
            var lease = attempt.lease().orElseThrow();
            var candidate = lease.candidateRevision();
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> {
                callbacks.incrementAndGet();
                throw new AssertionError(
                        "synthetic fatal deferred command listener");
            });

            var effects = lease.adoptStagedDeferredEffects();
            assertSame(candidate, orchestrator.currentRevision());
            assertEquals(0, callbacks.get());

            effects.publish();

            assertEquals(1, callbacks.get());
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertFalse(lease.ownsExactActiveTransition());
            effects.publish();
            assertEquals(1, callbacks.get());
            assertSame(candidate, orchestrator.currentRevision());
        }
    }

    @Test
    void outcomeBoundAdoptionRejectsForeignPreparedPairBeforePeerCommit()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCombinedUndoRedo foreignCombined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                    new DesignerCommandSessionOrchestrator(session(), combined);
                DesignerCommandSessionOrchestrator foreignOrchestrator =
                    new DesignerCommandSessionOrchestrator(
                            session(), foreignCombined)) {
            var predecessor = orchestrator.currentRevision();
            var lease = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                    .lease().orElseThrow();
            var foreignLease = foreignOrchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                    .lease().orElseThrow();
            PairSaveEvidence foreignEvidence = evidenceFor(
                    foreignLease.candidateRevision());
            AtomicInteger peerCommits = new AtomicInteger();

            assertThrows(IllegalArgumentException.class,
                    () -> lease.adoptStagedDeferredEffects(
                            foreignEvidence,
                            peerCommits::incrementAndGet));

            assertEquals(0, peerCommits.get());
            assertSame(predecessor, orchestrator.currentRevision());
            assertTrue(lease.ownsExactActiveTransition(),
                    "foreign staged evidence must not consume the exact lease");
            lease.abort();
            foreignLease.abort();
        }
    }

    @Test
    void outcomeBoundAdoptionCommitsPeerThenCursorWithoutInlineCallbacks()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            var predecessor = orchestrator.currentRevision();
            var lease = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                    .lease().orElseThrow();
            var candidate = lease.candidateRevision();
            PairSaveEvidence evidence = evidenceFor(candidate);
            AtomicInteger peerState = new AtomicInteger();
            ArrayList<String> order = new ArrayList<>();
            ChangeListener listener = event -> {
                assertEquals(1, peerState.get());
                assertSame(candidate, orchestrator.currentRevision());
                order.add("callback");
            };
            orchestrator.addChangeListener(listener);

            var effects = lease.adoptStagedDeferredEffects(evidence, () -> {
                assertEquals(0, peerState.get());
                assertSame(predecessor, orchestrator.currentRevision(),
                        "the peer assignment must execute before the cursor swap");
                assertTrue(lease.ownsExactActiveTransition());
                peerState.set(1);
                order.add("peer-commit");
            });

            assertEquals(List.of("peer-commit"), order,
                    "no outward listener may run inside the joined critical section");
            assertSame(candidate, orchestrator.currentRevision());
            assertFalse(lease.ownsExactActiveTransition());

            effects.publish();

            assertEquals(List.of("peer-commit", "callback"), order);
            orchestrator.removeChangeListener(listener);
        }
    }

    @Test
    void outcomeBoundAdoptionAndItsDeferredEffectsAreOneShot()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            var lease = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                    .lease().orElseThrow();
            PairSaveEvidence evidence = evidenceFor(lease.candidateRevision());
            AtomicInteger peerCommits = new AtomicInteger();
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> callbacks.incrementAndGet());

            var effects = lease.adoptStagedDeferredEffects(
                    evidence, peerCommits::incrementAndGet);
            var repeatedEffects = lease.adoptStagedDeferredEffects(
                    evidence, peerCommits::incrementAndGet);

            assertEquals(1, peerCommits.get());
            assertEquals(0, callbacks.get());
            effects.publish();
            effects.publish();
            repeatedEffects.publish();
            assertEquals(1, callbacks.get());
            assertSame(lease.candidateRevision(), orchestrator.currentRevision());
        }
    }

    @Test
    void invalidationAfterResolvedAbortClosesOnlyTheExactPredecessor()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        var predecessor = orchestrator.currentRevision();
        var lease = orchestrator.beginCommand(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("candidate")))
                .lease().orElseThrow();

        var abortEffects = lease.abortDeferredEffects();
        assertSame(predecessor, orchestrator.currentRevision());
        var invalidateEffects = lease.invalidateDeferredEffects();
        var repeatedEffects = lease.invalidateDeferredEffects();

        assertFalse(orchestrator.dirty());
        assertFalse(orchestrator.canUndo());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision);
        assertTrue(combined.designerSessionActive(),
                "binding callbacks remain deferred while recovery locks are held");

        abortEffects.publish();
        invalidateEffects.publish();
        repeatedEffects.publish();
        assertFalse(combined.designerSessionActive());
    }

    @Test
    void abortRetainsExactPredecessorAndItsRedoBranch() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C2")));
            var exactC2 = orchestrator.currentRevision();
            orchestrator.undo();
            var exactC1 = orchestrator.currentRevision();
            assertTrue(orchestrator.canRedo());

            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("branch-C3")));
            var lease = attempt.lease().orElseThrow();

            assertSame(exactC1, orchestrator.currentRevision());
            assertFalse(orchestrator.canRedo(),
                    "the active transition must pin rather than expose the old redo branch");
            lease.abort();

            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(orchestrator.canRedo(),
                    "aborting must restore the exact predecessor session, including redo");
            orchestrator.redo();
            assertSame(exactC2, orchestrator.currentRevision());
        }
    }

    @Test
    void activeCommandLeaseBlocksCommandsCursorMovesAndDurableSave()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C2")));
            var lease = attempt.lease().orElseThrow();

            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            assertThrows(IllegalStateException.class,
                    () -> orchestrator.beginCommand(new SetProperty(
                            ROOT_ID,
                            DATA,
                            new PropertyValue.StringValue("blocked"))));
            assertThrows(IllegalStateException.class, orchestrator::undo);
            assertThrows(IllegalStateException.class, orchestrator::redo);
            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);

            lease.abort();
            assertTrue(orchestrator.canUndo());
        }
    }

    @Test
    void pendingUndoRetainsExactLiveCursorUntilExplicitAdoption()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();

            var attempt = orchestrator.beginUndoTransition();
            var pending = attempt.lease().orElseThrow();

            assertEquals(DesignerCommandStatus.UNDONE, attempt.result().status());
            assertEquals(
                    DesignerCommandSessionOrchestrator.PendingTransitionKind.UNDO,
                    attempt.kind());
            assertEquals(attempt.kind(), pending.kind());
            assertSame(exactC1, pending.predecessorRevision());
            assertSame(initial.current(), pending.candidateRevision());
            assertSame(exactC1, orchestrator.currentRevision(),
                    "preparing Undo must not move the authoritative cursor");
            assertTrue(orchestrator.dirty());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());

            pending.adoptStaged();

            assertSame(initial.current(), orchestrator.currentRevision());
            assertFalse(orchestrator.dirty());
            assertFalse(orchestrator.canUndo());
            assertTrue(orchestrator.canRedo());
        }
    }

    @Test
    void pendingRedoRetainsExactLiveCursorUntilExplicitAdoption()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            var undo = orchestrator.beginUndoTransition().lease().orElseThrow();
            undo.adoptStaged();
            DesignerCommandRevision exactBaseline = orchestrator.currentRevision();

            var attempt = orchestrator.beginRedoTransition();
            var pending = attempt.lease().orElseThrow();

            assertEquals(DesignerCommandStatus.REDONE, attempt.result().status());
            assertEquals(
                    DesignerCommandSessionOrchestrator.PendingTransitionKind.REDO,
                    attempt.kind());
            assertEquals(attempt.kind(), pending.kind());
            assertSame(exactBaseline, pending.predecessorRevision());
            assertSame(exactC1, pending.candidateRevision());
            assertSame(exactBaseline, orchestrator.currentRevision(),
                    "preparing Redo must not move the authoritative cursor");
            assertFalse(orchestrator.dirty());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());

            pending.adoptStaged();

            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
        }
    }

    @Test
    void claimedUndoAdoptsExactPairedToBaselineTargetAfterPeerCommit()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            DesignerCommandRevision exactBaseline = initial.current();
            var pending = orchestrator.beginUndoTransition()
                    .lease().orElseThrow();
            Object claim = new Object();
            AtomicInteger peerCommits = new AtomicInteger();
            pending.claimForTransition(claim);

            var effects = pending.adoptExactTargetDeferredEffects(
                    exactBaseline,
                    () -> {
                        assertSame(exactC1, orchestrator.currentRevision(),
                                "the peer must commit before the command cursor moves");
                        assertTrue(pending.ownsExactActiveTransition());
                        peerCommits.incrementAndGet();
                    },
                    claim);

            assertEquals(1, peerCommits.get());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    exactBaseline.persistenceKind());
            assertSame(exactBaseline, orchestrator.currentRevision());
            assertFalse(pending.ownsExactActiveTransition());
            assertFalse(orchestrator.dirty());
            assertTrue(orchestrator.canRedo());
            effects.publish();
        }
    }

    @Test
    void claimedRedoAdoptsExactBaselineToPairedTargetAfterPeerCommit()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            orchestrator.beginUndoTransition().lease().orElseThrow()
                    .adoptStaged();
            DesignerCommandRevision exactBaseline = orchestrator.currentRevision();
            var pending = orchestrator.beginRedoTransition()
                    .lease().orElseThrow();
            Object claim = new Object();
            AtomicInteger peerCommits = new AtomicInteger();
            pending.claimForTransition(claim);

            var effects = pending.adoptExactTargetDeferredEffects(
                    exactC1,
                    () -> {
                        assertSame(exactBaseline, orchestrator.currentRevision(),
                                "the peer must commit before the command cursor moves");
                        assertTrue(pending.ownsExactActiveTransition());
                        peerCommits.incrementAndGet();
                    },
                    claim);

            assertEquals(1, peerCommits.get());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    exactC1.persistenceKind());
            assertSame(exactC1, orchestrator.currentRevision());
            assertFalse(pending.ownsExactActiveTransition());
            assertTrue(orchestrator.dirty());
            assertTrue(orchestrator.canUndo());
            effects.publish();
        }
    }

    @Test
    void claimedAdoptionRejectsForeignTargetAndClaimWithoutMovingCursor()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            DesignerCommandRevision exactBaseline = initial.current();
            var pending = orchestrator.beginUndoTransition()
                    .lease().orElseThrow();
            Object exactClaim = new Object();
            AtomicInteger peerCommits = new AtomicInteger();
            pending.claimForTransition(exactClaim);

            assertThrows(IllegalArgumentException.class,
                    () -> pending.adoptExactTargetDeferredEffects(
                            exactC1,
                            peerCommits::incrementAndGet,
                            exactClaim));
            assertThrows(IllegalStateException.class,
                    () -> pending.adoptExactTargetDeferredEffects(
                            exactBaseline,
                            peerCommits::incrementAndGet,
                            new Object()));

            assertEquals(0, peerCommits.get());
            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(pending.ownsExactActiveTransition(),
                    "a rejected target or claim must preserve the exact lease");
            pending.abortToExactPredecessorDeferredEffects(exactClaim).publish();
            assertSame(exactC1, orchestrator.currentRevision());
        }
    }

    @Test
    void peerFailureBeforeClaimedAdoptionRetainsExactActiveTransition()
            throws Exception {
        DesignerCommandSession initial = session();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(initial, combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            DesignerCommandRevision exactBaseline = initial.current();
            var pending = orchestrator.beginUndoTransition()
                    .lease().orElseThrow();
            Object claim = new Object();
            pending.claimForTransition(claim);
            IllegalStateException peerFailure = new IllegalStateException(
                    "synthetic peer commit failure");

            assertSame(peerFailure, assertThrows(IllegalStateException.class,
                    () -> pending.adoptExactTargetDeferredEffects(
                            exactBaseline,
                            () -> {
                                throw peerFailure;
                            },
                            claim)));

            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(pending.ownsExactActiveTransition());
            assertFalse(orchestrator.canUndo());
            assertFalse(orchestrator.canRedo());
            pending.abortToExactPredecessorDeferredEffects(claim).publish();
            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(orchestrator.canUndo());
        }
    }

    @Test
    void abortingPreparedUndoAndRedoRetainsExactCursorAndBothBranches()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            DesignerCommandRevision exactC1 = orchestrator.currentRevision();
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C2")));
            DesignerCommandRevision exactC2 = orchestrator.currentRevision();
            orchestrator.beginUndoTransition().lease().orElseThrow().adoptStaged();
            assertSame(exactC1, orchestrator.currentRevision());

            var undo = orchestrator.beginUndoTransition().lease().orElseThrow();
            undo.abort();
            undo.abort();
            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(orchestrator.canUndo());
            assertTrue(orchestrator.canRedo(),
                    "aborted Undo must retain the exact C2 redo branch");

            var redo = orchestrator.beginRedoTransition().lease().orElseThrow();
            assertSame(exactC2, redo.candidateRevision());
            redo.abort();
            redo.abort();
            assertSame(exactC1, orchestrator.currentRevision());
            assertTrue(orchestrator.canUndo());
            assertTrue(orchestrator.canRedo());
        }
    }

    @Test
    void invalidatingPreparedUndoOrRedoClosesTheUnsafeSession()
            throws Exception {
        DesignerCombinedUndoRedo undoCombined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator undoOrchestrator =
                new DesignerCommandSessionOrchestrator(session(), undoCombined);
        applyAndAdopt(undoOrchestrator, new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("undo")));
        var pendingUndo = undoOrchestrator.beginUndoTransition()
                .lease().orElseThrow();

        pendingUndo.invalidate();
        pendingUndo.invalidate();

        assertFalse(undoCombined.designerSessionActive());
        assertFalse(undoOrchestrator.canUndo());
        assertFalse(undoOrchestrator.canRedo());
        assertThrows(IllegalStateException.class,
                undoOrchestrator::currentRevision);

        DesignerCombinedUndoRedo redoCombined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator redoOrchestrator =
                new DesignerCommandSessionOrchestrator(session(), redoCombined);
        applyAndAdopt(redoOrchestrator, new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("redo")));
        redoOrchestrator.beginUndoTransition().lease().orElseThrow().adoptStaged();
        var pendingRedo = redoOrchestrator.beginRedoTransition()
                .lease().orElseThrow();

        pendingRedo.invalidate();
        pendingRedo.invalidate();

        assertFalse(redoCombined.designerSessionActive());
        assertFalse(redoOrchestrator.canUndo());
        assertFalse(redoOrchestrator.canRedo());
        assertThrows(IllegalStateException.class,
                redoOrchestrator::currentRevision);
    }

    @Test
    void pendingUndoAndRedoExcludeEveryCompetingSessionOperation()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));

            var pendingUndo = orchestrator.beginUndoTransition()
                    .lease().orElseThrow();
            assertBusyCursorTransition(orchestrator);
            pendingUndo.adoptStaged();

            var pendingRedo = orchestrator.beginRedoTransition()
                    .lease().orElseThrow();
            assertBusyCursorTransition(orchestrator);
            pendingRedo.abort();
        }
    }

    @Test
    void rejectedCommandCreatesNoLeaseAndPreservesPresentation()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        AtomicInteger changes = new AtomicInteger();
        combined.addChangeListener(event -> changes.incrementAndGet());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            boolean canUndo = combined.canUndo();
            boolean canRedo = combined.canRedo();
            String undoName = combined.getUndoPresentationName();
            String redoName = combined.getRedoPresentationName();
            int before = changes.get();

            var rejected = orchestrator.beginCommand(new ResetProperty(
                    DOCUMENT_ID, DATA));

            assertEquals(DesignerCommandStatus.REJECTED,
                    rejected.result().status());
            assertTrue(rejected.lease().isEmpty());
            assertEquals(canUndo, combined.canUndo());
            assertEquals(canRedo, combined.canRedo());
            assertEquals(undoName, combined.getUndoPresentationName());
            assertEquals(redoName, combined.getRedoPresentationName());
            assertEquals(before, changes.get());
        }
    }

    @Test
    void closeDuringPendingCommandIsDeferredUntilLeaseResolution()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        var predecessor = orchestrator.currentRevision();
        var attempt = orchestrator.beginCommand(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("pending")));
        var lease = attempt.lease().orElseThrow();

        orchestrator.close();

        assertTrue(combined.designerSessionActive());
        assertSame(predecessor, orchestrator.currentRevision());
        lease.abort();
        lease.abort();

        assertFalse(combined.designerSessionActive());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision);
    }

    @Test
    void invalidatingPendingCommandClosesUnsafeSession() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined);
        var attempt = orchestrator.beginCommand(new SetProperty(
                ROOT_ID, DATA, new PropertyValue.StringValue("pending")));
        var lease = attempt.lease().orElseThrow();

        lease.invalidate();
        lease.invalidate();

        assertFalse(lease.ownsExactActiveTransition());
        assertFalse(combined.designerSessionActive());
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertFalse(orchestrator.dirty());
        assertThrows(IllegalStateException.class, orchestrator::currentRevision);
        assertThrows(IllegalStateException.class,
                () -> orchestrator.beginCommand(new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("closed"))));
    }

    @Test
    void fatalPresentationListenerCannotChangeCommandResolution()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            AtomicInteger callbacks = new AtomicInteger();
            orchestrator.addChangeListener(event -> {
                callbacks.incrementAndGet();
                throw new AssertionError("synthetic fatal command listener");
            });

            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("adopted")));
            var lease = attempt.lease().orElseThrow();
            var candidate = lease.candidateRevision();
            lease.adoptStaged();

            assertTrue(callbacks.get() >= 2,
                    "begin and adoption must both attempt presentation publication");
            assertFalse(lease.ownsExactActiveTransition());
            assertSame(candidate, orchestrator.currentRevision());
            assertTrue(orchestrator.dirty());
        }
    }

    @Test
    void durableLeaseNeverCapturesAPendingCandidate() throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(
                new UndoRedo.Manager());
        try (DesignerCommandSessionOrchestrator orchestrator =
                new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(orchestrator, new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("C1")));
            var exactC1 = orchestrator.currentRevision();
            var attempt = orchestrator.beginCommand(new SetProperty(
                    ROOT_ID, DATA, new PropertyValue.StringValue("pending-C2")));
            var pendingC2 = attempt.lease().orElseThrow();

            assertThrows(IllegalStateException.class,
                    orchestrator::beginDurableSave);
            assertSame(exactC1, orchestrator.currentRevision());
            assertNotEquals(exactC1, pendingC2.candidateRevision());

            pendingC2.abort();
            try (DesignerCommandSessionOrchestrator.DurableSaveLease durable =
                    orchestrator.beginDurableSave()) {
                assertSame(exactC1, durable.revision());
                durable.abort();
            }
        }
    }

    @Test
    void sourceRestagePinsTheLogicalCursorAndAbortDoesNotAdmitItsPhysicalPair()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        try (var owner = new DesignerCommandSessionOrchestrator(session(), combined)) {
            applyAndAdopt(owner, new SetProperty(ROOT_ID, DATA, new PropertyValue.StringValue("staged")));
            var logical = owner.currentRevision();
            var original = logical.preparedPair().orElseThrow();
            byte[] observed = sourceOverlay(logical, "// User-owned edit before first Save\n");
            var lease = owner.beginSourceRestage(logical, original, observed);
            assertSame(logical, lease.revision());
            assertSame(logical, owner.currentRevision());
            assertSame(original, lease.predecessorPair());
            assertNotSame(logical, lease.physicalRevision());
            assertEquals(logical.revisionId(), lease.physicalRevision().revisionId());
            assertArrayEquals(observed, lease.preparedPair().prospectiveDartBytes());
            assertTrue(lease.ownsExactActiveRevision());
            assertBusyCursorTransition(owner);

            lease.abortDeferredEffects().publish();
            lease.close();
            assertSame(logical, owner.currentRevision());
            assertFalse(lease.ownsExactActiveRevision());
            assertTrue(owner.canUndo());
            assertThrows(IllegalArgumentException.class, () -> owner.beginDurableSave(lease.preparedPair()));
            try (var save = owner.beginDurableSave(original)) {
                assertSame(logical, save.revision());
            }
        }
    }

    @Test
    void sourceRestageAdoptsExactAnalyzedBytesWithoutAnExtraSemanticUndoEdge()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        try (var owner = new DesignerCommandSessionOrchestrator(session(), combined)) {
            var initial = owner.currentRevision();
            applyAndAdopt(owner, new SetProperty(ROOT_ID, DATA, new PropertyValue.StringValue("staged")));
            var logical = owner.currentRevision();
            byte[] observed = sourceOverlay(logical, "// Edited while the Designer pair is pending\n");
            var lease = owner.beginSourceRestage(logical, logical.preparedPair().orElseThrow(), observed);
            var evidence = evidenceFor(lease.physicalRevision());
            AtomicInteger peerCommits = new AtomicInteger();
            AtomicInteger callbacks = new AtomicInteger();
            owner.addChangeListener(event -> callbacks.incrementAndGet());
            var effects = lease.adoptAnalyzedDeferredEffects(evidence, () -> {
                assertTrue(lease.ownsExactActiveRevision());
                assertSame(logical, owner.currentRevision());
                peerCommits.incrementAndGet();
            });
            assertEquals(1, peerCommits.get());
            assertEquals(0, callbacks.get());
            assertSame(logical, owner.currentRevision(), "Source restaging must retain the logical revision object");
            assertFalse(lease.ownsExactActiveRevision());
            effects.publish();
            effects.publish();
            assertEquals(1, callbacks.get());

            try (var save = owner.beginDurableSave(lease.preparedPair())) {
                assertArrayEquals(observed, save.reanchoredRevision(logical.revisionId()).dartCandidateBytes());
                save.adoptCommitted();
            }
            assertFalse(owner.dirty());
            assertArrayEquals(observed, owner.currentRevision().dartCandidateBytes());
            owner.undo();
            assertEquals(initial.revisionId(), owner.currentRevision().revisionId());
            assertArrayEquals(initial.dartCandidateBytes(), owner.currentRevision().dartCandidateBytes());
            assertFalse(owner.canUndo(), "Source adoption is not another semantic command");
        }
    }

    @Test
    void sourceRestageRejectsStaleGuardedAndForeignEvidenceWithoutConsumingTheLease()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        try (var owner = new DesignerCommandSessionOrchestrator(session(), combined)) {
            var stale = owner.currentRevision();
            applyAndAdopt(owner, new SetProperty(ROOT_ID, DATA, new PropertyValue.StringValue("staged")));
            var logical = owner.currentRevision();
            var original = logical.preparedPair().orElseThrow();
            byte[] observed = sourceOverlay(logical, "// User source\n");
            assertThrows(DesignerCommandSessionOrchestrator.StaleRevisionException.class,
                    () -> owner.beginSourceRestage(stale, original, observed));
            byte[] guardedEdit = new String(observed, StandardCharsets.UTF_8)
                    .replace("'staged'", "'guard changed'").getBytes(StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class,
                    () -> owner.beginSourceRestage(logical, original, guardedEdit));
            assertSame(logical, owner.currentRevision());
            assertTrue(owner.canUndo());
            var lease = owner.beginSourceRestage(logical, original, observed);
            AtomicInteger peerCommits = new AtomicInteger();
            assertThrows(IllegalArgumentException.class,
                    () -> lease.adoptAnalyzedDeferredEffects(evidenceFor(logical), peerCommits::incrementAndGet));
            assertEquals(0, peerCommits.get());
            assertTrue(lease.ownsExactActiveRevision());
            lease.adoptAnalyzedDeferredEffects(evidenceFor(lease.physicalRevision()), peerCommits::incrementAndGet).publish();
            assertEquals(1, peerCommits.get());
            assertSame(logical, owner.currentRevision());
        }
    }

    @Test
    void sourceRestageStrictAdoptionRejectsPendingCloseBeforePeerChanges()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        var owner = new DesignerCommandSessionOrchestrator(session(), combined);
        applyAndAdopt(owner, new SetProperty(ROOT_ID, DATA, new PropertyValue.StringValue("staged")));
        var logical = owner.currentRevision();
        var lease = owner.beginSourceRestage(logical, logical.preparedPair().orElseThrow(),
                sourceOverlay(logical, "// User source before closing\n"));
        var evidence = evidenceFor(lease.physicalRevision());
        AtomicInteger peerCommits = new AtomicInteger();
        owner.close();
        assertThrows(IllegalStateException.class,
                () -> lease.adoptAnalyzedDeferredEffects(evidence, peerCommits::incrementAndGet));
        assertEquals(0, peerCommits.get());
        assertSame(logical, owner.currentRevision());
        assertTrue(lease.ownsExactActiveRevision());
        assertTrue(combined.designerSessionActive());
        lease.close();
        assertFalse(combined.designerSessionActive());
        assertThrows(IllegalStateException.class, owner::currentRevision);
    }

    @Test
    void sourceRestageCloseAwareAdoptionDefersOutwardCloseUntilPublication()
            throws Exception {
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(new UndoRedo.Manager());
        var owner = new DesignerCommandSessionOrchestrator(session(), combined);
        applyAndAdopt(owner, new SetProperty(ROOT_ID, DATA, new PropertyValue.StringValue("staged")));
        var logical = owner.currentRevision();
        var lease = owner.beginSourceRestage(logical, logical.preparedPair().orElseThrow(),
                sourceOverlay(logical, "// Last edit before closing\n"));
        var evidence = evidenceFor(lease.physicalRevision());
        owner.close();
        assertTrue(combined.designerSessionActive());
        AtomicBoolean closingObserved = new AtomicBoolean();
        var effects = lease.adoptAnalyzedCloseAwareDeferredEffects(evidence, closing -> {
            assertTrue(closing);
            assertTrue(lease.ownsExactActiveRevision());
            closingObserved.set(closing);
        });
        assertTrue(closingObserved.get());
        assertThrows(IllegalStateException.class, owner::currentRevision);
        assertTrue(combined.designerSessionActive());
        effects.publish();
        assertFalse(combined.designerSessionActive());
        lease.close();
    }

    private static void assertBusyCursorTransition(
            DesignerCommandSessionOrchestrator orchestrator) {
        assertFalse(orchestrator.canUndo());
        assertFalse(orchestrator.canRedo());
        assertThrows(IllegalStateException.class,
                orchestrator::beginUndoTransition);
        assertThrows(IllegalStateException.class,
                orchestrator::beginRedoTransition);
        assertThrows(IllegalStateException.class,
                () -> orchestrator.beginCommand(new SetProperty(
                        ROOT_ID,
                        DATA,
                        new PropertyValue.StringValue("blocked"))));
        assertThrows(IllegalStateException.class,
                orchestrator::beginDurableSave);
        assertThrows(IllegalStateException.class, orchestrator::undo);
        assertThrows(IllegalStateException.class, orchestrator::redo);
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

    private static PreparedDesignerPair exactOverlayPair(
            DesignerCommandRevision revision,
            String unmanagedSuffix) {
        PreparedDesignerPair canonical = revision.preparedPair().orElseThrow();
        byte[] liveOverlay = (new String(
                canonical.liveDartBytes(), StandardCharsets.UTF_8)
                + unmanagedSuffix).getBytes(StandardCharsets.UTF_8);
        var transition = new DartSourceTransitionPlanner()
                .plan(
                        canonical.dartTransition().baseline(),
                        liveOverlay,
                        canonical.baselineDocument().source(),
                        revision.generation());
        assertTrue(transition.ready(), () -> transition.diagnostics().toString());
        var prepared = new DesignerPairPreparationPlanner()
                .prepare(
                        canonical.baselineFd(),
                        revision.document(),
                        transition.plan().orElseThrow());
        assertTrue(prepared.ready(), () -> prepared.diagnostics().toString());
        return prepared.preparedPair().orElseThrow();
    }

    private static PairSaveEvidence evidenceFor(
            DesignerCommandRevision candidate) throws Exception {
        var prepared = candidate.preparedPair().orElseThrow();
        FdDecodeResult.Current decoded = (FdDecodeResult.Current)
                new FdDocumentCodec().decode(prepared.baselineFdBytes());
        FlutterDesignerDocumentState.Current current =
                new FlutterDesignerDocumentState.Current(
                        decoded,
                        new ValidationResult(List.of()),
                        BuiltInWidgetCatalog.getDefault(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty());
        byte[] candidateBytes = prepared.prospectiveDartBytes();
        String content = new String(candidateBytes, StandardCharsets.UTF_8);
        Path projectRoot = Path.of(
                System.getProperty("java.io.tmpdir"),
                "flutter-designer-orchestrator-evidence")
                .toAbsolutePath().normalize();
        Path dartFile = projectRoot.resolve(
                prepared.prospectiveDocument().source().dartFile());
        DartCandidateAnalysisRequest request =
                new DartCandidateAnalysisRequest(
                        projectRoot,
                        dartFile,
                        content,
                        1,
                        sha256(candidateBytes),
                        DartCandidateWarningPolicy.ALLOW,
                        List.of());
        PairCandidateAnalysisTicket ticket = new PairCandidateAnalysisTicket(
                current,
                prepared,
                request,
                projectRoot,
                projectRoot,
                dartFile);
        DartCandidateAnalysisResult analysis =
                new DartCandidateAnalysisResult(
                        DartCandidateAnalysisStatus.PASSED,
                        request.snapshot(),
                        Optional.of("test"),
                        List.of(),
                        0,
                        List.of(),
                        Optional.empty());

        StyledDocument document = (StyledDocument) new DartEditorKit()
                .createDefaultDocument();
        DartGuardedSectionsProvider provider =
                new DartGuardedSectionsProvider(() -> document);
        try (Reader reader = provider.createGuardedReader(
                new ByteArrayInputStream(candidateBytes),
                StandardCharsets.UTF_8)) {
            document.insertString(0, readAll(reader), null);
        }
        LiveDartDocumentSnapshot live = LiveDartDocumentBridge.snapshot(
                document, provider);
        return new PairSaveEvidence(
                new PairAnalyzedCandidate(ticket, analysis), live);
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[512];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, read);
        }
        return content.toString();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }

    private static DesignerCommandSession session() throws Exception {
        WidgetCatalog catalog = BuiltInWidgetCatalog.getDefault();
        DesignerDocument provisional = document(descriptor(
                "0".repeat(64), "0".repeat(64)), "before");
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(provisional, catalog)
                .generated().orElseThrow();
        DesignerDocument exact = document(
                new DartSourceDescriptor(
                        "home_page.dart",
                        "HomePage",
                        WidgetClassKind.STATELESS,
                        Optional.of(generated.profileId()),
                        new ManagedRegions(
                                new ManagedRegion(
                                        generated.imports().normalizedSha256()),
                                new ManagedRegion(
                                        generated.build().normalizedSha256()))),
                "before");
        GeneratedDartRegions exactGenerated = new DartRegionGenerator()
                .generate(exact, catalog)
                .generated().orElseThrow();
        byte[] dart = source(exactGenerated).getBytes(StandardCharsets.UTF_8);
        return DesignerCommandSession.open(
                new FdDocumentCodec().encode(exact), dart, catalog)
                .session().orElseThrow();
    }

    private static DesignerCommandSession fdOnlySession() throws Exception {
        WidgetCatalog catalog = BuiltInWidgetCatalog.getDefault();
        DesignerDocument provisional = columnDocument(descriptor(
                "0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(provisional, catalog)
                .generated().orElseThrow();
        DesignerDocument exact = columnDocument(new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(generated.profileId()),
                new ManagedRegions(
                        new ManagedRegion(
                                generated.imports().normalizedSha256()),
                        new ManagedRegion(
                                generated.build().normalizedSha256()))));
        GeneratedDartRegions exactGenerated = new DartRegionGenerator()
                .generate(exact, catalog)
                .generated().orElseThrow();
        DesignerCommandSession initial = DesignerCommandSession.open(
                new FdDocumentCodec().encode(exact),
                source(exactGenerated).getBytes(StandardCharsets.UTF_8),
                catalog).session().orElseThrow();
        var moved = initial.apply(new MoveWidget(
                FIRST_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 1)));
        assertEquals(DesignerCommandStatus.APPLIED, moved.status());
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                moved.session().current().persistenceKind());
        return moved.session();
    }

    private static DesignerDocument document(
            DartSourceDescriptor descriptor,
            String text) {
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue(text)),
                Map.of());
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DesignerDocument columnDocument(
            DartSourceDescriptor descriptor) {
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
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DartSourceDescriptor descriptor(
            String importsHash,
            String buildHash) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static byte[] sourceOverlay(
            DesignerCommandRevision revision,
            String unmanagedSuffix) {
        return (new String(
                revision.dartCandidateBytes(), StandardCharsets.UTF_8)
                + unmanagedSuffix).getBytes(StandardCharsets.UTF_8);
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
}
