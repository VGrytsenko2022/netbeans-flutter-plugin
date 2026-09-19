package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.Admission;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.AdmissionStatus;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.CloneTopology;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.DocumentRevision;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.LifecycleStamp;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.PostCloseProof;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.SupportCloseAdmission;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.SupportCloseAdmissionStatus;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.SupportCloseBatch;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerEditorClosePermitCoordinator.SupportCloseOwnerState;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void ordinaryCloseRejectsSameIdentityLifecycleAbaDuringDecision() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 1, 2, true);
        coordinator.editorShellOpenedOrClosing(owner);

        Admission stale = coordinator.acquire(
                owner,
                () -> topology(owner),
                () -> revision,
                () -> {
                    coordinator.editorShellClosed(owner);
                    coordinator.editorShellOpenedOrClosing(owner);
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
        assertFalse(coordinator.authorizeSupportClose(true, null, null),
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
        DocumentRevision revision = revision(new Object(), 1, 1, true);

        assertTrue(coordinator.authorizeSupportClose(true, null, null),
                "the ordinary MultiView route remains unchanged");
        coordinator.editorShellOpenedOrClosing(owner);
        assertFalse(coordinator.authorizeSupportClose(
                true, topology(owner), revision));
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(owner), revision),
                "direct componentClosed must not discard a dirty document");

        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.authorizeSupportClose(true, null, null));
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
        assertTrue(coordinator.bindAdmittedState(
                admission.permit(), topology(), revision));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(
                true, topology(), revision),
                "an admitted component callback never authorizes a second prompt");
        assertTrue(coordinator.authorizeSupportClose(
                false, topology(), revision));
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), revision),
                "the internal CES close delegation is one-shot");
        assertTrue(coordinator.completeSupportClose(
                true, topology(), revision));
        assertFalse(coordinator.completeAdmitted(admission.permit()),
                "authority remains held for the complete componentClosed scope");

        scope.close();
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope
                repeatedScope = coordinator.enterComponentClosed(
                        admission.permit(), owner);
        assertTrue(repeatedScope != null);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), revision),
                "the internal CES close delegation is one-shot for the permit, "
                + "not merely for one callback scope");
        repeatedScope.close();
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
        assertTrue(coordinator.bindAdmittedState(
                admission.permit(), topology(sibling), revision));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(sibling), revision),
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
        assertTrue(coordinator.bindAdmittedState(
                admission.permit(), topology(), revision));

        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(admission.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), revision),
                "last-clone status alone cannot replace closeLast's one-shot consumption");

        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.completeAdmitted(admission.permit()));
    }

    @Test
    void supportBatchClosesTwoOwnersInExactOrderAfterOneDocumentDecision() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object first = new Object();
        Object second = new Object();
        Object document = new Object();
        AtomicReference<DocumentRevision> revision = new AtomicReference<>(
                revision(document, 3, 5, true));
        AtomicInteger decisions = new AtomicInteger();
        coordinator.editorShellOpenedOrClosing(first);
        coordinator.editorShellOpenedOrClosing(second);

        SupportCloseAdmission support = coordinator.beginSupportClose(
                () -> topology(first, second),
                revision::get,
                () -> {
                    decisions.incrementAndGet();
                    revision.set(revision(document, 4, 6, false));
                    return true;
                });

        assertEquals(SupportCloseAdmissionStatus.ACQUIRED, support.status());
        assertTrue(support.authorized());
        assertEquals(1, decisions.get());
        assertEquals(2, support.batch().ownerCount());
        assertSame(first, coordinator.nextSupportCloseOwner(support.batch()));
        assertFalse(coordinator.cloneCreationAllowed());
        assertNull(coordinator.reserveCloneCreation());
        assertEquals(AdmissionStatus.BUSY, coordinator.acquire(
                first,
                () -> topology(first, second),
                revision::get,
                () -> true).status());
        assertEquals(SupportCloseAdmissionStatus.BUSY,
                coordinator.beginSupportClose(
                        () -> topology(first, second),
                        revision::get,
                        () -> {
                            decisions.incrementAndGet();
                            return true;
                        }).status(),
                "a repeated CloseCookie request must coalesce without a "
                + "second document decision");
        assertEquals(1, decisions.get());

        Admission wrongOrder = coordinator.acquireSupportCloseOwner(
                support.batch(), second,
                topology(first, second), revision.get());
        assertEquals(AdmissionStatus.INVALID_TOPOLOGY, wrongOrder.status());
        assertNull(coordinator.nextSupportCloseOwner(support.batch()),
                "an out-of-order dispatch must revoke the complete batch");
        assertTrue(coordinator.cloneCreationAllowed());

        SupportCloseAdmission retry = coordinator.beginSupportClose(
                () -> topology(first, second), revision::get, () -> {
                    decisions.incrementAndGet();
                    return true;
                });
        SupportCloseBatch batch = retry.batch();
        Admission firstClose = coordinator.acquireSupportCloseOwner(
                batch, first, topology(first, second), revision.get());
        Admission retainedFirst = coordinator.acquireSupportCloseOwner(
                batch, first, topology(first, second), revision.get());
        assertTrue(firstClose.authorized());
        assertSame(firstClose.permit(), retainedFirst.permit());
        assertFalse(firstClose.permit().lastClone());
        assertTrue(firstClose.permit().sequence() > batch.sequence());
        admitNonLast(
                coordinator,
                firstClose,
                topology(first, second),
                topology(second),
                revision.get());
        coordinator.editorShellClosed(first);
        assertTrue(coordinator.completeSupportCloseOwner(
                batch, first, firstClose.permit()));
        assertSame(second, coordinator.nextSupportCloseOwner(batch));

        Admission secondClose = coordinator.acquireSupportCloseOwner(
                batch, second, topology(second), revision.get());
        assertTrue(secondClose.authorized());
        assertTrue(secondClose.permit().lastClone());
        assertTrue(secondClose.permit().sequence()
                > firstClose.permit().sequence());
        assertTrue(coordinator.beginCommit(
                secondClose.permit(), topology(second), revision.get()));
        assertTrue(coordinator.consumeLastClose(
                secondClose.permit(), topology(second), revision.get()));
        assertTrue(coordinator.markAdmitted(secondClose.permit()));
        assertTrue(coordinator.bindAdmittedState(
                secondClose.permit(), topology(), revision.get()));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(secondClose.permit(), second);
        assertTrue(scope != null);
        assertTrue(coordinator.authorizeSupportClose(
                false, topology(), revision.get()),
                "the final batch owner retains the exact internal closeLast scope");
        assertTrue(coordinator.completeSupportClose(
                true, topology(), revision.get()));
        scope.close();
        coordinator.editorShellClosed(second);
        assertTrue(coordinator.completeSupportCloseOwner(
                batch, second, secondClose.permit()));
        assertNull(coordinator.nextSupportCloseOwner(batch));
        assertTrue(coordinator.cloneCreationAllowed());
        assertEquals(2, decisions.get(),
                "each batch, not each owner, asks exactly once");
    }

    @Test
    void continuationReservationSurvivesFinalOwnerAndCompletesExactlyOnce() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 3, 5, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportCloseForContinuation(
                () -> topology(owner), () -> revision, () -> true).batch();

        completeSingleOwnerSupportClose(coordinator, batch, owner, revision);

        assertFalse(coordinator.cloneCreationAllowed(),
                "the post-close operation must retain presentation authority");
        assertNull(coordinator.reserveCloneCreation(),
                "no clone may reopen between physical close and continuation");
        assertEquals(SupportCloseAdmissionStatus.BUSY,
                coordinator.beginSupportClose(
                        () -> topology(), () -> revision, () -> true).status());

        PostCloseProof proof = coordinator.beginPostCloseContinuation(batch);
        assertTrue(proof != null);
        assertTrue(proof.belongsTo(coordinator));
        assertNull(coordinator.beginPostCloseContinuation(batch),
                "the retained continuation can be claimed only once");
        assertFalse(coordinator.cloneCreationAllowed(),
                "claiming the callback must not release its reservation");

        assertTrue(coordinator.completePostCloseContinuation(batch, proof));
        assertFalse(proof.belongsTo(coordinator));
        assertFalse(coordinator.completePostCloseContinuation(batch, proof),
                "the same callback proof cannot complete twice");
        assertNull(coordinator.beginPostCloseContinuation(batch));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void proofClaimCannotCompletePastConcurrentReservationRelease()
            throws Exception {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object claimOwner = new Object();
        DocumentRevision revision = revision(new Object(), 3, 5, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportCloseForContinuation(
                () -> topology(owner), () -> revision, () -> true).batch();
        completeSingleOwnerSupportClose(coordinator, batch, owner, revision);
        PostCloseProof proof = coordinator.beginPostCloseContinuation(batch);
        assertTrue(proof != null);

        CountDownLatch completionEntered = new CountDownLatch(1);
        CountDownLatch completionReturned = new CountDownLatch(1);
        AtomicBoolean completionResult = new AtomicBoolean();
        AtomicReference<Throwable> completionFailure = new AtomicReference<>();
        Thread completionThread = new Thread(() -> {
            completionEntered.countDown();
            try {
                completionResult.set(coordinator.completePostCloseContinuation(
                        batch, proof));
            } catch (Throwable failure) {
                completionFailure.set(failure);
            } finally {
                completionReturned.countDown();
            }
        }, "post-close-proof-completion");
        completionThread.setDaemon(true);

        synchronized (proof) {
            completionThread.start();
            assertTrue(completionEntered.await(5, TimeUnit.SECONDS));
            assertFalse(completionReturned.await(200, TimeUnit.MILLISECONDS),
                    "completion must serialize with an in-flight proof claim");
            assertTrue(proof.claim(coordinator, claimOwner));
        }

        assertTrue(completionReturned.await(5, TimeUnit.SECONDS));
        completionThread.join(5_000);
        assertFalse(completionThread.isAlive());
        assertNull(completionFailure.get());
        assertTrue(completionResult.get());
        assertFalse(proof.claim(coordinator, new Object()),
                "a completed proof cannot admit a late claimant");
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void readyPostCloseContinuationCanBeAbortedExactlyOnce() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 4, 6, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportCloseForContinuation(
                () -> topology(owner), () -> revision, () -> true).batch();

        completeSingleOwnerSupportClose(coordinator, batch, owner, revision);

        assertFalse(coordinator.cloneCreationAllowed());
        assertTrue(coordinator.abortPostCloseContinuation(batch));
        assertFalse(coordinator.abortPostCloseContinuation(batch),
                "an already-aborted continuation is no longer READY");
        assertNull(coordinator.beginPostCloseContinuation(batch));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void ordinarySupportCloseReleasesImmediatelyWithoutContinuationPhase() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 5, 7, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();

        completeSingleOwnerSupportClose(coordinator, batch, owner, revision);

        assertTrue(coordinator.cloneCreationAllowed(),
                "ordinary CloseCookie keeps its immediate-release contract");
        assertNull(coordinator.beginPostCloseContinuation(batch));
        assertFalse(coordinator.abortPostCloseContinuation(batch));
    }

    @Test
    void supportBatchCancelAndTopologyChangePublishNoAuthority() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object first = new Object();
        Object second = new Object();
        AtomicReference<CloneTopology> topology = new AtomicReference<>(
                topology(first));
        AtomicInteger decisions = new AtomicInteger();
        coordinator.editorShellOpenedOrClosing(first);

        SupportCloseAdmission cancelled = coordinator.beginSupportClose(
                topology::get,
                () -> revision(new Object(), 1, 2, true),
                () -> {
                    decisions.incrementAndGet();
                    return false;
                });
        assertEquals(SupportCloseAdmissionStatus.CANCELLED, cancelled.status());
        assertFalse(cancelled.authorized());
        assertTrue(coordinator.cloneCreationAllowed());

        SupportCloseAdmission stale = coordinator.beginSupportClose(
                topology::get,
                () -> revision(new Object(), 2, 3, false),
                () -> {
                    decisions.incrementAndGet();
                    topology.set(topology(first, second));
                    return true;
                });
        assertEquals(SupportCloseAdmissionStatus.STALE, stale.status());
        assertFalse(stale.authorized());
        assertTrue(coordinator.cloneCreationAllowed());
        assertEquals(2, decisions.get());
    }

    @Test
    void supportBatchUsesLifecycleOrderButAcceptsEquivalentRegistryOrder() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object first = new Object();
        Object second = new Object();
        DocumentRevision revision = revision(new Object(), 1, 2, false);
        coordinator.editorShellOpenedOrClosing(first);
        coordinator.editorShellOpenedOrClosing(second);

        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(second, first),
                () -> revision,
                () -> true).batch();

        assertSame(first, coordinator.nextSupportCloseOwner(batch),
                "physical lifecycle order is the deterministic batch order");
        Admission firstClose = coordinator.acquireSupportCloseOwner(
                batch, first, topology(second, first), revision);
        assertTrue(firstClose.authorized(),
                "clone-registry enumeration order is not semantic topology");
        coordinator.revoke(firstClose.permit());
        assertFalse(coordinator.supportCloseActive(batch));
    }

    @Test
    void supportBatchRevisionOrRemainingTopologyChangeIsStale() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object document = new Object();
        DocumentRevision admitted = revision(document, 5, 8, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseAdmission support = coordinator.beginSupportClose(
                () -> topology(owner), () -> admitted, () -> true);

        Admission staleRevision = coordinator.acquireSupportCloseOwner(
                support.batch(), owner, topology(owner),
                revision(document, 6, 8, false));
        assertEquals(AdmissionStatus.STALE, staleRevision.status());
        assertNull(coordinator.nextSupportCloseOwner(support.batch()));
        assertTrue(coordinator.cloneCreationAllowed());

        SupportCloseAdmission second = coordinator.beginSupportClose(
                () -> topology(owner), () -> admitted, () -> true);
        Object unexpected = new Object();
        coordinator.editorShellOpenedOrClosing(unexpected);
        assertNull(coordinator.nextSupportCloseOwner(second.batch()),
                "the dispatcher must not return an owner for stale lifecycle order");
        Admission staleTopology = coordinator.acquireSupportCloseOwner(
                second.batch(), owner, topology(owner), admitted);
        assertEquals(AdmissionStatus.INVALID_TOPOLOGY, staleTopology.status());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void supportBatchRejectsCloseReopenAbaDuringDocumentDecision() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 1, 1, false);
        coordinator.editorShellOpenedOrClosing(owner);

        SupportCloseAdmission stale = coordinator.beginSupportClose(
                () -> topology(owner),
                () -> revision,
                () -> {
                    coordinator.editorShellClosed(owner);
                    coordinator.editorShellOpenedOrClosing(owner);
                    return true;
                });

        assertEquals(SupportCloseAdmissionStatus.STALE, stale.status());
        assertFalse(stale.authorized());
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void finalBatchOwnerRequiresConsumedInternalSupportCloseScope() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertEquals(SupportCloseOwnerState.READY,
                coordinator.supportCloseOwnerState(
                        batch, owner, close.permit()));
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertEquals(SupportCloseOwnerState.ADMITTED,
                coordinator.supportCloseOwnerState(
                        batch, owner, close.permit()));
        DocumentRevision postAdmissionRevision = new DocumentRevision(
                null, -1, revision.pairRevision(), false, false);
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), postAdmissionRevision),
                "NetBeans unregisters the admitted owner from Ref before the "
                + "physical componentClosed callback; the final clone may "
                + "also unload its document in closeLast(false)");
        assertTrue(coordinator.validateAdmittedSupportCloseOwner(
                batch, owner, close.permit(), topology(),
                postAdmissionRevision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);
        assertTrue(coordinator.authorizeSupportClose(
                false, topology(), postAdmissionRevision));
        assertTrue(coordinator.completeSupportClose(
                false, topology(), postAdmissionRevision),
                "a failed CES close result must be acknowledged explicitly");
        scope.close();
        coordinator.editorShellClosed(owner);

        assertFalse(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()),
                "final completion must prove that the exact internal "
                + "close(false) succeeded, not merely that it was authorized");
        assertTrue(coordinator.abortSupportClose(batch));
    }

    @Test
    void postUnregisterPairOrSourceDriftCannotBeBoundOrAdvanceBatch() {
        Object document = new Object();
        DocumentRevision baseline = revision(document, 2, 3, false);
        List<DocumentRevision> driftedRevisions = List.of(
                revision(document, 2, 4, false),
                new DocumentRevision(
                        document,
                        2,
                        new PairSaveCoordinator.CloseRevision(
                                PAIR_IDENTITY, 3, 0, 1),
                        false,
                        false));

        for (DocumentRevision drifted : driftedRevisions) {
            FlutterDesignerEditorClosePermitCoordinator coordinator =
                    new FlutterDesignerEditorClosePermitCoordinator();
            Object owner = new Object();
            coordinator.editorShellOpenedOrClosing(owner);
            SupportCloseBatch batch = coordinator
                    .beginSupportCloseForContinuation(
                            () -> topology(owner),
                            () -> baseline,
                            () -> true)
                    .batch();
            Admission close = coordinator.acquireSupportCloseOwner(
                    batch, owner, topology(owner), baseline);

            assertTrue(coordinator.beginCommit(
                    close.permit(), topology(owner), baseline));
            assertTrue(coordinator.consumeLastClose(
                    close.permit(), topology(owner), baseline));
            assertTrue(coordinator.markAdmitted(close.permit()));
            assertFalse(coordinator.bindAdmittedState(
                    close.permit(), topology(), drifted),
                    "a pair/source epoch changed after final-close consumption");

            FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope
                    scope = coordinator.enterComponentClosed(
                            close.permit(), owner);
            assertTrue(scope != null,
                    "irreversible admission still permits physical cleanup");
            assertFalse(coordinator.authorizeSupportClose(
                    false, topology(), drifted),
                    "drifted state must not receive internal CES close authority");
            scope.close();
            coordinator.editorShellClosed(owner);
            assertFalse(coordinator.completeSupportCloseOwner(
                    batch, owner, close.permit()),
                    "the batch must not advance after post-unregister drift");
            assertNull(coordinator.beginPostCloseContinuation(batch),
                    "no filesystem continuation proof may escape the drifted batch");
            assertTrue(coordinator.abortSupportClose(batch));
            assertTrue(coordinator.cloneCreationAllowed());
        }
    }

    @Test
    void admittedSnapshotDriftAllowsPhysicalCleanupButCannotAdvanceBatch() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        DocumentRevision postAdmissionRevision = new DocumentRevision(
                null, -1, revision.pairRevision(), false, false);
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), postAdmissionRevision));

        assertTrue(coordinator.validateAdmittedSupportCloseOwner(
                batch,
                owner,
                close.permit(),
                topology(),
                new DocumentRevision(
                        null,
                        -1,
                        revision.pairRevision(),
                        true,
                        false)),
                "an already-unregistered owner must still be allowed to finish "
                + "its physical close after post-admission drift");
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), postAdmissionRevision),
                "drifted authority must not unload or close a newer document state");
        scope.close();
        coordinator.editorShellClosed(owner);
        assertFalse(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()),
                "physical cleanup after drift must not report batch success");
        assertTrue(coordinator.abortSupportClose(batch));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void lifecycleDriftInsideComponentClosedScopeCannotAuthorizeSupportClose() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object unexpected = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), revision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);

        coordinator.editorShellOpenedOrClosing(unexpected);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), revision),
                "a reentrant lifecycle change must invalidate the internal "
                + "no-ask document close before it runs");
        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.abortSupportClose(batch));
        coordinator.editorShellClosed(unexpected);
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void siblingLifecycleDriftStillAllowsExactAdmittedOwnerCleanup() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object sibling = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        LifecycleStamp ownerStamp = coordinator.editorShellOpenedOrClosing(owner);
        LifecycleStamp oldSiblingStamp =
                coordinator.editorShellOpenedOrClosing(sibling);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner, sibling),
                () -> revision,
                () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner, sibling), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner, sibling), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(sibling), revision));

        assertTrue(coordinator.editorShellClosed(sibling, oldSiblingStamp));
        LifecycleStamp newSiblingStamp =
                coordinator.editorShellOpenedOrClosing(sibling);
        assertNotSame(oldSiblingStamp, newSiblingStamp);
        assertEquals(SupportCloseOwnerState.ADMITTED,
                coordinator.supportCloseOwnerState(
                        batch, owner, close.permit()),
                "sibling lifecycle drift must retain exact physical cleanup");
        assertTrue(coordinator.validateAdmittedSupportCloseOwner(
                batch,
                owner,
                close.permit(),
                topology(sibling),
                revision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);
        assertFalse(coordinator.authorizeSupportClose(
                false, topology(sibling), revision),
                "a drifted batch must not close document authority");
        scope.close();
        assertTrue(coordinator.editorShellClosed(owner, ownerStamp));
        assertFalse(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()));
        assertTrue(coordinator.abortSupportClose(batch));
        assertFalse(coordinator.editorShellClosed(sibling, oldSiblingStamp),
                "a delayed callback from the old incarnation must be inert");
        assertTrue(coordinator.editorShellClosed(sibling, newSiblingStamp));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void sameOwnerLifecycleAbaVetoesOldAdmittedAuthority() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        LifecycleStamp oldStamp = coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), revision));

        assertTrue(coordinator.editorShellClosed(owner, oldStamp));
        LifecycleStamp newStamp = coordinator.editorShellOpenedOrClosing(owner);
        assertNotSame(oldStamp, newStamp);
        assertFalse(coordinator.validateAdmittedSupportCloseOwner(
                batch, owner, close.permit(), topology(), revision),
                "old authority must never close a reopened owner incarnation");
        assertFalse(coordinator.supportCloseActive(batch));
        assertTrue(coordinator.cloneCreationAllowed());
        assertFalse(coordinator.editorShellClosed(owner, oldStamp));
        assertEquals(1, coordinator.lifecycleShellCount());
        assertTrue(coordinator.editorShellClosed(owner, newStamp));
    }

    @Test
    void lifecycleAbaBetweenCommitAndAdmissionCanReleaseOldBatch() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        LifecycleStamp oldStamp = coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));

        assertTrue(coordinator.editorShellClosed(owner, oldStamp));
        LifecycleStamp newStamp = coordinator.editorShellOpenedOrClosing(owner);
        assertFalse(coordinator.markAdmitted(close.permit()));
        assertFalse(coordinator.admittedPhysicalOwnerCurrent(
                close.permit(), owner));
        assertTrue(coordinator.releaseStaleAdmittedOwner(
                close.permit(), owner));
        assertFalse(coordinator.supportCloseActive(batch));
        assertTrue(coordinator.cloneCreationAllowed());
        assertTrue(coordinator.editorShellClosed(owner, newStamp));
    }

    @Test
    void revisionDriftBeforeInternalCloseIsRejected() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object document = new Object();
        DocumentRevision revision = revision(document, 2, 3, false);
        DocumentRevision changed = revision(document, 3, 3, false);
        LifecycleStamp stamp = coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), revision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);

        assertFalse(coordinator.authorizeSupportClose(
                false, topology(), changed));
        scope.close();
        assertTrue(coordinator.editorShellClosed(owner, stamp));
        assertFalse(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()));
        assertTrue(coordinator.abortSupportClose(batch));
    }

    @Test
    void revisionDriftDuringInternalCloseCannotReportSuccess() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        Object document = new Object();
        DocumentRevision revision = revision(document, 2, 3, false);
        DocumentRevision changed = revision(document, 3, 3, false);
        LifecycleStamp stamp = coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), revision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);
        assertTrue(coordinator.authorizeSupportClose(
                false, topology(), revision));

        assertTrue(coordinator.completeSupportClose(
                true, topology(), changed),
                "the attempt is acknowledged even though its proof drifted");
        scope.close();
        assertTrue(coordinator.editorShellClosed(owner, stamp));
        assertFalse(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()));
        assertTrue(coordinator.abortSupportClose(batch));
    }

    @Test
    void supportBatchAndCloneReservationExcludeEachOtherAcrossDecision() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 1, 1, false);
        coordinator.editorShellOpenedOrClosing(owner);
        FlutterDesignerEditorClosePermitCoordinator.CloneCreationReservation
                reservation = coordinator.reserveCloneCreation();
        assertTrue(reservation != null);
        assertEquals(SupportCloseAdmissionStatus.BUSY,
                coordinator.beginSupportClose(
                        () -> topology(owner), () -> revision, () -> true)
                        .status());
        reservation.close();

        SupportCloseAdmission support = coordinator.beginSupportClose(
                () -> topology(owner),
                () -> revision,
                () -> {
                    assertNull(coordinator.reserveCloneCreation());
                    assertEquals(AdmissionStatus.BUSY, coordinator.acquire(
                            owner,
                            () -> topology(owner),
                            () -> revision,
                            () -> true).status());
                    assertEquals(SupportCloseAdmissionStatus.BUSY,
                            coordinator.beginSupportClose(
                                    () -> topology(owner),
                                    () -> revision,
                                    () -> true).status());
                    return true;
                });
        assertTrue(support.authorized());
        assertNull(coordinator.reserveCloneCreation());
        assertTrue(coordinator.abortSupportClose(support.batch()));
        assertTrue(coordinator.cloneCreationAllowed());
    }

    @Test
    void supportBatchAbortCannotReleaseAnAdmittedPhysicalOwner() {
        FlutterDesignerEditorClosePermitCoordinator coordinator =
                new FlutterDesignerEditorClosePermitCoordinator();
        Object owner = new Object();
        DocumentRevision revision = revision(new Object(), 2, 3, false);
        coordinator.editorShellOpenedOrClosing(owner);
        SupportCloseBatch batch = coordinator.beginSupportClose(
                () -> topology(owner), () -> revision, () -> true).batch();
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));

        assertFalse(coordinator.abortSupportClose(batch),
                "an admitted owner that is still physically registered is irreversible");
        assertFalse(coordinator.cloneCreationAllowed());
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.abortSupportClose(batch),
                "after physical removal, explicit abort may release a failed batch");
        assertTrue(coordinator.cloneCreationAllowed());
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

    private static void admitNonLast(
            FlutterDesignerEditorClosePermitCoordinator coordinator,
            Admission admission,
            CloneTopology topology,
            CloneTopology admittedTopology,
            DocumentRevision revision) {
        assertTrue(coordinator.beginCommit(
                admission.permit(), topology, revision));
        assertTrue(coordinator.markAdmitted(admission.permit()));
        assertTrue(coordinator.bindAdmittedState(
                admission.permit(), admittedTopology, revision));
    }

    private static void completeSingleOwnerSupportClose(
            FlutterDesignerEditorClosePermitCoordinator coordinator,
            SupportCloseBatch batch,
            Object owner,
            DocumentRevision revision) {
        Admission close = coordinator.acquireSupportCloseOwner(
                batch, owner, topology(owner), revision);
        assertTrue(close.authorized());
        assertTrue(coordinator.beginCommit(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.consumeLastClose(
                close.permit(), topology(owner), revision));
        assertTrue(coordinator.markAdmitted(close.permit()));
        assertTrue(coordinator.bindAdmittedState(
                close.permit(), topology(), revision));
        FlutterDesignerEditorClosePermitCoordinator.ComponentClosedScope scope =
                coordinator.enterComponentClosed(close.permit(), owner);
        assertTrue(scope != null);
        assertTrue(coordinator.authorizeSupportClose(
                false, topology(), revision));
        assertTrue(coordinator.completeSupportClose(
                true, topology(), revision));
        scope.close();
        coordinator.editorShellClosed(owner);
        assertTrue(coordinator.completeSupportCloseOwner(
                batch, owner, close.permit()));
    }
}
