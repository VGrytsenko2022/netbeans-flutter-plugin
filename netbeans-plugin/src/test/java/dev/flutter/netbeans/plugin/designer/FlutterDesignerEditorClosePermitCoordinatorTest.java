package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.Admission;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.AdmissionStatus;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.CloneTopology;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.DocumentRevision;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FlutterDesignerEditorClosePermitCoordinatorTest {
    private static final Object PAIR_IDENTITY = new Object();

    @Test
    void cancelOnLastCloneCreatesNoPermitAndStartsNoIrreversiblePhase() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        AtomicInteger decisions = new AtomicInteger();

        Admission cancelled = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision(new Object(), 7, 11, true),
                () -> {
                    decisions.incrementAndGet();
                    return false;
                });

        assertEquals(AdmissionStatus.CANCELLED, cancelled.status());
        assertFalse(cancelled.authorized());
        assertEquals(1, decisions.get());
        assertTrue(coordinator.cloneCreationAllowed());
        assertFalse(coordinator.reservedByOther(new Object()));
    }

    @Test
    void lastCloneDecisionIsRetainedOnceAndConsumedWithoutSecondPrompt() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object document = new Object();
        AtomicReference<DocumentRevision> current = new AtomicReference<>(
                revision(document, 4, 8, true));
        AtomicInteger decisions = new AtomicInteger();

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology(owner),
                current::get,
                () -> {
                    decisions.incrementAndGet();
                    current.set(revision(document, 4, 9, false));
                    return true;
                });
        Admission retained = coordinator.acquire(
                owner,
                () -> topology(owner),
                current::get,
                () -> {
                    decisions.incrementAndGet();
                    return true;
                });

        assertEquals(AdmissionStatus.ACQUIRED, acquired.status());
        assertEquals(AdmissionStatus.RETAINED, retained.status());
        assertSame(acquired.permit(), retained.permit());
        assertTrue(acquired.permit().lastClone());
        assertEquals(1, decisions.get(),
                "Save/Discard/Cancel must be decided before Canvas exactly once");
        assertTrue(coordinator.beginCommit(
                acquired.permit(), topology(owner), current.get()));
        assertTrue(coordinator.consumeLastClose(
                acquired.permit(), topology(owner), current.get()));
        assertFalse(coordinator.consumeLastClose(
                acquired.permit(), topology(owner), current.get()),
                "the last-close authorization is one-shot");
        assertTrue(coordinator.markAdmitted(acquired.permit()));
        assertFalse(coordinator.cloneCreationAllowed(),
                "authority remains reserved through componentClosed");

        assertTrue(coordinator.completeAdmitted(acquired.permit()));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void nonLastReservationVetoesSiblingCloseAndCloneCreation() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        Object document = new Object();
        CloneTopology topology = topology(owner, sibling);
        DocumentRevision revision = revision(document, 3, 5, true);

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology,
                () -> revision,
                () -> {
                    throw new AssertionError(
                            "a non-last clone must not ask about the document");
                });
        Admission siblingClose = coordinator.acquire(
                sibling,
                () -> topology,
                () -> revision,
                () -> true);

        assertTrue(acquired.authorized());
        assertFalse(acquired.permit().lastClone());
        assertEquals(AdmissionStatus.BUSY, siblingClose.status());
        assertTrue(coordinator.reservedByOther(sibling));
        assertFalse(coordinator.cloneCreationAllowed());
        assertFalse(coordinator.consumeLastClose(
                acquired.permit(), topology, revision),
                "a non-last admission cannot authorize an unexpected last close");

        coordinator.revoke(acquired.permit());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void documentVersionOrPairEpochChangeInvalidatesRetainedPermit() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        Object document = new Object();
        AtomicReference<DocumentRevision> current = new AtomicReference<>(
                revision(document, 12, 20, true));

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology(owner, sibling),
                current::get,
                () -> true);
        current.set(revision(document, 13, 21, true));
        Admission stale = coordinator.acquire(
                owner,
                () -> topology(owner, sibling),
                current::get,
                () -> true);

        assertTrue(acquired.authorized());
        assertEquals(AdmissionStatus.STALE, stale.status());
        assertFalse(stale.authorized());
        assertTrue(coordinator.cloneCreationAllowed(),
                "stale authority must not strand the clone group");
    }

    @Test
    void topologyChangeDuringDocumentDecisionCannotPublishPermit() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        Object document = new Object();
        AtomicReference<CloneTopology> currentTopology =
                new AtomicReference<>(topology(owner));

        Admission stale = coordinator.acquire(
                owner,
                currentTopology::get,
                () -> revision(document, 1, 2, true),
                () -> {
                    currentTopology.set(topology(owner, sibling));
                    return true;
                });

        assertEquals(AdmissionStatus.STALE, stale.status());
        assertFalse(stale.authorized());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void topologyComparisonUsesIdentityAndIgnoresSnapshotOrder() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new String("same-value");
        Object sibling = new String("same-value");
        Object impostor = new String("same-value");
        DocumentRevision revision = revision(new Object(), 2, 4, false);

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology(owner, sibling),
                () -> revision,
                () -> true);
        Admission retained = coordinator.acquire(
                owner,
                () -> topology(sibling, owner),
                () -> revision,
                () -> true);

        assertSame(acquired.permit(), retained.permit());
        assertNotSame(owner, sibling);
        assertFalse(coordinator.validate(
                acquired.permit(), topology(owner, impostor), revision));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void ownerMustBelongToTheCapturedCloneTopology() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();

        Admission invalid = coordinator.acquire(
                owner,
                () -> topology(new Object()),
                () -> revision(new Object(), 0, 0, false),
                () -> true);

        assertEquals(AdmissionStatus.INVALID_TOPOLOGY, invalid.status());
        assertFalse(invalid.authorized());
    }

    @Test
    void documentDecisionOwnsTheWholeCloneTopologyWhileDialogIsOpen() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        DocumentRevision revision = revision(new Object(), 5, 9, true);

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> {
                    assertFalse(coordinator.cloneCreationAllowed());
                    Admission competing = coordinator.acquire(
                            sibling,
                            () -> topology(owner, sibling),
                            () -> revision,
                            () -> true);
                    assertEquals(AdmissionStatus.BUSY, competing.status());
                    assertTrue(coordinator.reservedByOther(sibling));
                    return true;
                });

        assertTrue(acquired.authorized());
        coordinator.revoke(acquired.permit());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void changedRevisionAtCommitRevokesTheReadyPermit() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object document = new Object();
        DocumentRevision admitted = revision(document, 8, 13, false);

        Admission acquired = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> admitted,
                () -> true);

        assertFalse(coordinator.beginCommit(
                acquired.permit(),
                topology(owner),
                revision(document, 9, 13, true)));
        assertTrue(coordinator.cloneCreationAllowed());
        assertFalse(coordinator.markAdmitted(acquired.permit()));
    }

    @Test
    void abortedCommitReleasesReservationButAdmittedCloseCannotBeRevoked() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 3, 7, false);

        Admission aborted = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true);
        assertTrue(coordinator.beginCommit(
                aborted.permit(), topology(owner), revision));
        coordinator.abortCommit(aborted.permit());
        assertTrue(coordinator.cloneCreationAllowed());

        Admission admitted = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true);
        assertTrue(coordinator.beginCommit(
                admitted.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                admitted.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(admitted.permit()));

        coordinator.revoke(admitted.permit());
        assertFalse(coordinator.cloneCreationAllowed(),
                "componentClosed is the only normal release for admitted authority");
        assertEquals(AdmissionStatus.BUSY, coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true).status());
        assertTrue(coordinator.completeAdmitted(admitted.permit()));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void cloneCreationReservationAndCloseAdmissionAreMutuallyExclusive() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 1, 1, false);

        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                cloneCreation = coordinator.reserveCloneCreation();
        assertTrue(cloneCreation != null);
        assertFalse(coordinator.cloneCreationAllowed());
        assertFalse(coordinator.authorizeSupportClose(true),
                "support close must not cross an unpublished clone reservation");
        assertEquals(AdmissionStatus.BUSY, coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true).status());
        assertNull(coordinator.reserveCloneCreation());

        cloneCreation.close();
        cloneCreation.close();
        Admission close = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true);
        assertTrue(close.authorized());
        assertNull(coordinator.reserveCloneCreation());
        coordinator.revoke(close.permit());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void supportCloseIsFailClosedWithoutExactComponentClosedScope() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();

        assertTrue(coordinator.authorizeSupportClose(true),
                "the ordinary MultiView route remains unchanged");
        coordinator.editorShellOpenedOrClosing(owner);
        assertFalse(coordinator.authorizeSupportClose(true));
        assertFalse(coordinator.authorizeSupportClose(false),
                "direct componentClosed must not discard a dirty document");

        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.authorizeSupportClose(true));
    }

    @Test
    void admittedComponentClosedScopeAllowsOneInternalNoAskClose() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, true);
        coordinator.editorShellOpenedOrClosing(owner);
        Admission admission = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true);
        assertTrue(coordinator.beginCommit(
                admission.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                admission.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(admission.permit()));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(true),
                "an admitted component callback never authorizes a second prompt");
        assertTrue(coordinator.authorizeSupportClose(false));
        assertFalse(coordinator.authorizeSupportClose(false),
                "the internal CES close delegation is one-shot");
        assertFalse(coordinator.completeAdmitted(admission.permit()),
                "authority remains held for the complete componentClosed scope");

        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.completeAdmitted(admission.permit()));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void nonLastAdmittedScopeCannotAuthorizeDocumentDiscard() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, true);
        coordinator.editorShellOpenedOrClosing(owner);
        coordinator.editorShellOpenedOrClosing(sibling);
        Admission admission = coordinator.acquire(
                owner,
                () -> topology(owner, sibling),
                () -> revision,
                () -> {
                    throw new AssertionError(
                            "a non-last clone must not ask the document question");
                });
        assertTrue(coordinator.beginCommit(
                admission.permit(), topology(owner, sibling), revision));
        assertTrue(coordinator.markAdmitted(admission.permit()));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(false),
                "a non-last close never owns permission to unload the document");

        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.completeAdmitted(admission.permit()));
    }

    @Test
    void lastAdmittedScopeWithoutConsumedCloseCannotAuthorizeDocumentDiscard() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, true);
        coordinator.editorShellOpenedOrClosing(owner);
        Admission admission = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> true);
        assertTrue(coordinator.beginCommit(
                admission.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(admission.permit()));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(false),
                "last-clone status alone cannot replace closeLast's one-shot consumption");

        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.completeAdmitted(admission.permit()));
    }

    @Test
    void documentRevisionRequiresEveryExactPairAuthorityCounterAndIdentity() {
        Object document = new Object();
        Object pair = new Object();
        DocumentRevision baseline = new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(pair, 3, 5, 7),
                true,
                true);

        assertTrue(baseline.sameRevision(new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(pair, 3, 5, 7),
                true,
                true)));
        assertFalse(baseline.sameRevision(new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(pair, 4, 5, 7),
                true,
                true)));
        assertFalse(baseline.sameRevision(new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(pair, 3, 6, 7),
                true,
                true)));
        assertFalse(baseline.sameRevision(new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(pair, 3, 5, 8),
                true,
                true)));
        assertFalse(baseline.sameRevision(new DocumentRevision(
                document,
                17,
                new PairSaveCoordinator.CloseRevision(
                        new Object(), 3, 5, 7),
                true,
                true)));
    }

    private static CloneTopology topology(Object... members) {
        return new CloneTopology(List.of(members));
    }

    private static DocumentRevision revision(
            Object document,
            long version,
            long pairEpoch,
            boolean modified) {
        return new DocumentRevision(
                document,
                version,
                new PairSaveCoordinator.CloseRevision(
                        PAIR_IDENTITY, pairEpoch, 0, 0),
                modified,
                modified);
    }
}
