package dev.flutter.netbeans.plugin.designer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Serializes the two-phase close of every dedicated Designer editor clone.
 *
 * <p>The NetBeans clone registry exposes only snapshots. Consequently one
 * support-owned reservation must veto sibling closes and clone creation while
 * an asynchronous Canvas retirement is in flight. The retained permit is
 * valid only for the exact clone members and document/pair revision admitted
 * after the last-clone Save/Discard/Cancel decision.</p>
 */
final class FlutterDesignerEditorClosePermitCoordinator {
    private long nextSequence;
    private PendingDecision pendingDecision;
    private PendingSupportClose pendingSupportClose;
    private SupportCloseBatch activeSupportClose;
    private Permit activePermit;
    private CloneCreationReservation cloneCreationReservation;
    private final List<Object> lifecycleShells = new ArrayList<>();
    private final Map<Object, LifecycleStamp> lifecycleShellStamps =
            new IdentityHashMap<>();
    private long lifecycleShellEpoch;
    private long nextLifecycleGeneration;
    private ComponentClosedScope componentClosedScope;

    Admission acquire(
            Object owner,
            Supplier<CloneTopology> topologySupplier,
            Supplier<DocumentRevision> revisionSupplier,
            BooleanSupplier lastCloneDecision) {
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        Supplier<CloneTopology> admittedTopologySupplier = Objects.requireNonNull(
                topologySupplier, "topologySupplier");
        Supplier<DocumentRevision> admittedRevisionSupplier = Objects.requireNonNull(
                revisionSupplier, "revisionSupplier");
        BooleanSupplier admittedDecision = Objects.requireNonNull(
                lastCloneDecision, "lastCloneDecision");

        Permit retained;
        synchronized (this) {
            if (pendingDecision != null
                    || pendingSupportClose != null
                    || activeSupportClose != null
                    || cloneCreationReservation != null) {
                return Admission.rejected(AdmissionStatus.BUSY);
            }
            retained = activePermit;
            if (retained != null && retained.owner != admittedOwner) {
                return Admission.rejected(AdmissionStatus.BUSY);
            }
        }
        if (retained != null) {
            CloneTopology topology = requireTopology(admittedTopologySupplier);
            DocumentRevision revision = requireRevision(admittedRevisionSupplier);
            synchronized (this) {
                if (activePermit != retained) {
                    return Admission.rejected(AdmissionStatus.BUSY);
                }
                if (retained.phase != PermitPhase.READY) {
                    return Admission.rejected(AdmissionStatus.BUSY);
                }
                if (!permitLifecycleCurrentLocked(retained)
                        || !retained.matches(topology, revision)) {
                    activePermit = null;
                    return Admission.rejected(AdmissionStatus.STALE);
                }
                return Admission.authorized(AdmissionStatus.RETAINED, retained);
            }
        }

        CloneTopology initialTopology = requireTopology(admittedTopologySupplier);
        if (!initialTopology.containsIdentity(admittedOwner)) {
            return Admission.rejected(AdmissionStatus.INVALID_TOPOLOGY);
        }
        boolean lastClone = initialTopology.isLast(admittedOwner);
        PendingDecision pending;
        synchronized (this) {
            if (pendingDecision != null
                    || pendingSupportClose != null
                    || activeSupportClose != null
                    || activePermit != null
                    || cloneCreationReservation != null) {
                return Admission.rejected(AdmissionStatus.BUSY);
            }
            pending = new PendingDecision(
                    nextSequenceLocked(), admittedOwner,
                    initialTopology, lastClone, lifecycleShellEpoch);
            pendingDecision = pending;
        }

        final boolean documentAuthorized;
        try {
            documentAuthorized = !lastClone || admittedDecision.getAsBoolean();
        } catch (RuntimeException | Error failure) {
            clearPending(pending);
            throw failure;
        }
        if (!documentAuthorized) {
            clearPending(pending);
            return Admission.rejected(AdmissionStatus.CANCELLED);
        }

        final CloneTopology finalTopology;
        final DocumentRevision revision;
        try {
            finalTopology = requireTopology(admittedTopologySupplier);
            revision = requireRevision(admittedRevisionSupplier);
        } catch (RuntimeException | Error failure) {
            clearPending(pending);
            throw failure;
        }

        synchronized (this) {
            if (pendingDecision != pending || activePermit != null) {
                if (pendingDecision == pending) {
                    pendingDecision = null;
                }
                return Admission.rejected(AdmissionStatus.BUSY);
            }
            pendingDecision = null;
            if (!pending.topology.sameMembers(finalTopology)
                    || pending.lastClone != finalTopology.isLast(admittedOwner)
                    || lifecycleShellEpoch != pending.lifecycleEpoch) {
                return Admission.rejected(AdmissionStatus.STALE);
            }
            Permit permit = new Permit(
                    pending.sequence,
                    admittedOwner,
                    finalTopology,
                    revision,
                    lastClone,
                    null,
                    pending.lifecycleEpoch,
                    lifecycleShellStamps.get(admittedOwner));
            activePermit = permit;
            return Admission.authorized(AdmissionStatus.ACQUIRED, permit);
        }
    }

    /**
     * Reserves one exact, ordered clone group for a support-wide close.
     *
     * <p>The document decision is executed exactly once while clone creation
     * and every ordinary single-owner close are excluded. The final topology
     * and document revision are captured after that decision because Save or
     * Discard may legitimately advance both the live document and pair
     * authority. Any clone topology change during the decision is stale.</p>
     */
    SupportCloseAdmission beginSupportClose(
            Supplier<CloneTopology> topologySupplier,
            Supplier<DocumentRevision> revisionSupplier,
            BooleanSupplier documentDecision) {
        return beginSupportClose(
                topologySupplier,
                revisionSupplier,
                documentDecision,
                false);
    }

    /**
     * Reserves a support close whose final topology remains unavailable to
     * clone creation until one exact post-close operation has finished.
     */
    SupportCloseAdmission beginSupportCloseForContinuation(
            Supplier<CloneTopology> topologySupplier,
            Supplier<DocumentRevision> revisionSupplier,
            BooleanSupplier documentDecision) {
        return beginSupportClose(
                topologySupplier,
                revisionSupplier,
                documentDecision,
                true);
    }

    private SupportCloseAdmission beginSupportClose(
            Supplier<CloneTopology> topologySupplier,
            Supplier<DocumentRevision> revisionSupplier,
            BooleanSupplier documentDecision,
            boolean retainForContinuation) {
        Supplier<CloneTopology> admittedTopologySupplier = Objects.requireNonNull(
                topologySupplier, "topologySupplier");
        Supplier<DocumentRevision> admittedRevisionSupplier = Objects.requireNonNull(
                revisionSupplier, "revisionSupplier");
        BooleanSupplier admittedDecision = Objects.requireNonNull(
                documentDecision, "documentDecision");

        CloneTopology initialTopology = requireTopology(
                admittedTopologySupplier);
        PendingSupportClose pending;
        synchronized (this) {
            if (pendingDecision != null
                    || pendingSupportClose != null
                    || activeSupportClose != null
                    || activePermit != null
                    || cloneCreationReservation != null
                    || componentClosedScope != null) {
                return SupportCloseAdmission.rejected(
                        SupportCloseAdmissionStatus.BUSY);
            }
            if (initialTopology.size() == 0
                    || !initialTopology.sameMembers(lifecycleShells)) {
                return SupportCloseAdmission.rejected(
                        SupportCloseAdmissionStatus.INVALID_TOPOLOGY);
            }
            pending = new PendingSupportClose(
                    nextSequenceLocked(),
                    new CloneTopology(lifecycleShells),
                    lifecycleShellEpoch);
            pendingSupportClose = pending;
        }

        final boolean documentAuthorized;
        try {
            documentAuthorized = admittedDecision.getAsBoolean();
        } catch (RuntimeException | Error failure) {
            clearPendingSupportClose(pending);
            throw failure;
        }
        if (!documentAuthorized) {
            clearPendingSupportClose(pending);
            return SupportCloseAdmission.rejected(
                    SupportCloseAdmissionStatus.CANCELLED);
        }

        final CloneTopology finalTopology;
        final DocumentRevision finalRevision;
        try {
            finalTopology = requireTopology(admittedTopologySupplier);
            finalRevision = requireRevision(admittedRevisionSupplier);
        } catch (RuntimeException | Error failure) {
            clearPendingSupportClose(pending);
            throw failure;
        }

        synchronized (this) {
            if (pendingSupportClose != pending
                    || pendingDecision != null
                    || activeSupportClose != null
                    || activePermit != null
                    || cloneCreationReservation != null
                    || componentClosedScope != null) {
                if (pendingSupportClose == pending) {
                    pendingSupportClose = null;
                }
                return SupportCloseAdmission.rejected(
                        SupportCloseAdmissionStatus.BUSY);
            }
            pendingSupportClose = null;
            if (!pending.topology.sameMembers(finalTopology)
                    || !pending.topology.sameOrderedMembers(lifecycleShells)
                    || lifecycleShellEpoch != pending.lifecycleEpoch) {
                return SupportCloseAdmission.rejected(
                        SupportCloseAdmissionStatus.STALE);
            }
            SupportCloseBatch batch = new SupportCloseBatch(
                    pending.sequence,
                    pending.topology,
                    finalRevision,
                    pending.lifecycleEpoch,
                    retainForContinuation);
            activeSupportClose = batch;
            return SupportCloseAdmission.acquired(batch);
        }
    }

    /**
     * Issues or retains the ordinary per-shell permit for the batch's next
     * exact owner. No document decision is performed on this path.
     */
    Admission acquireSupportCloseOwner(
            SupportCloseBatch batch,
            Object owner,
            CloneTopology topology,
            DocumentRevision revision) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        synchronized (this) {
            if (activeSupportClose != admittedBatch
                    || admittedBatch.phase != SupportClosePhase.ACTIVE) {
                return Admission.rejected(AdmissionStatus.BUSY);
            }
            if (lifecycleShellEpoch
                    != admittedBatch.expectedLifecycleEpoch) {
                abortSupportCloseLocked(admittedBatch);
                return Admission.rejected(AdmissionStatus.INVALID_TOPOLOGY);
            }
            if (!admittedBatch.isNextOwner(admittedOwner)
                    || !admittedBatch.matchesRemaining(
                            admittedTopology, lifecycleShells)) {
                abortSupportCloseLocked(admittedBatch);
                return Admission.rejected(AdmissionStatus.INVALID_TOPOLOGY);
            }
            if (!admittedBatch.revision.sameRevision(admittedRevision)) {
                abortSupportCloseLocked(admittedBatch);
                return Admission.rejected(AdmissionStatus.STALE);
            }
            Permit retained = activePermit;
            if (retained != null) {
                if (retained.supportCloseBatch != admittedBatch
                        || retained.owner != admittedOwner
                        || retained.phase != PermitPhase.READY) {
                    return Admission.rejected(AdmissionStatus.BUSY);
                }
                if (!retained.matches(admittedTopology, admittedRevision)) {
                    abortSupportCloseLocked(admittedBatch);
                    return Admission.rejected(AdmissionStatus.STALE);
                }
                return Admission.authorized(
                        AdmissionStatus.RETAINED, retained);
            }
            Permit permit = new Permit(
                    nextSequenceLocked(),
                    admittedOwner,
                    admittedTopology,
                    admittedRevision,
                    admittedTopology.isLast(admittedOwner),
                    admittedBatch,
                    admittedBatch.expectedLifecycleEpoch,
                    lifecycleShellStamps.get(admittedOwner));
            if (permit.ownerLifecycleStamp == null) {
                abortSupportCloseLocked(admittedBatch);
                return Admission.rejected(
                        AdmissionStatus.INVALID_TOPOLOGY);
            }
            activePermit = permit;
            return Admission.authorized(AdmissionStatus.ACQUIRED, permit);
        }
    }

    /** Returns the exact next identity while the batch topology is current. */
    synchronized Object nextSupportCloseOwner(SupportCloseBatch batch) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase != SupportClosePhase.ACTIVE
                || admittedBatch.complete()
                || lifecycleShellEpoch
                        != admittedBatch.expectedLifecycleEpoch
                || !admittedBatch.matchesRemainingFrom(
                        admittedBatch.nextOwnerIndex, lifecycleShells)) {
            return null;
        }
        return admittedBatch.topology.memberAt(admittedBatch.nextOwnerIndex);
    }

    synchronized boolean supportCloseActive(SupportCloseBatch batch) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        return activeSupportClose == admittedBatch
                && admittedBatch.phase == SupportClosePhase.ACTIVE;
    }

    synchronized SupportCloseOwnerState supportCloseOwnerState(
            SupportCloseBatch batch,
            Object owner,
            Permit permit) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        Permit admittedPermit = Objects.requireNonNull(permit, "permit");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase != SupportClosePhase.ACTIVE
                || activePermit != admittedPermit
                || admittedPermit.supportCloseBatch != admittedBatch
                || admittedPermit.owner != admittedOwner) {
            return SupportCloseOwnerState.NONE;
        }
        if (admittedPermit.phase == PermitPhase.ADMITTED) {
            if (!permitOwnerLifecycleCurrentLocked(admittedPermit)
                    || !containsIdentity(lifecycleShells, admittedOwner)) {
                abortSupportCloseLocked(admittedBatch);
                return SupportCloseOwnerState.NONE;
            }
            if (lifecycleShellEpoch
                        != admittedBatch.expectedLifecycleEpoch
                    || !admittedBatch.isNextOwner(admittedOwner)
                    || !admittedBatch.matchesRemainingFrom(
                            admittedBatch.nextOwnerIndex, lifecycleShells)) {
                // A sibling may have opened or closed after Ref.unregister()
                // made this owner's admission irreversible. Preserve only the
                // exact physical-cleanup authority; the batch cannot advance.
                markAdmittedStateDrifted(admittedPermit,
                        "the lifecycle registry changed after admission");
            }
            return SupportCloseOwnerState.ADMITTED;
        }
        if (!permitLifecycleCurrentLocked(admittedPermit)) {
            if (admittedPermit.phase == PermitPhase.READY) {
                abortSupportCloseLocked(admittedBatch);
            }
            return SupportCloseOwnerState.NONE;
        }
        return admittedPermit.phase == PermitPhase.READY
                ? SupportCloseOwnerState.READY
                : SupportCloseOwnerState.COMMITTING;
    }

    synchronized boolean validateAdmittedSupportCloseOwner(
            SupportCloseBatch batch,
            Object owner,
            Permit permit,
            CloneTopology topology,
            DocumentRevision revision) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        Permit admittedPermit = Objects.requireNonNull(permit, "permit");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        boolean physicalOwnerCurrent = activeSupportClose == admittedBatch
                && admittedBatch.phase == SupportClosePhase.ACTIVE
                && activePermit == admittedPermit
                && admittedPermit.supportCloseBatch == admittedBatch
                && admittedPermit.owner == admittedOwner
                && admittedPermit.phase == PermitPhase.ADMITTED
                && componentClosedScope == null
                && admittedBatch.isNextOwner(admittedOwner)
                && permitOwnerLifecycleCurrentLocked(admittedPermit)
                && containsIdentity(lifecycleShells, admittedOwner);
        if (!physicalOwnerCurrent) {
            if (activePermit == admittedPermit
                    && admittedPermit.phase == PermitPhase.ADMITTED
                    && !permitOwnerLifecycleCurrentLocked(admittedPermit)) {
                abortSupportCloseLocked(admittedBatch);
            }
            return false;
        }
        if (lifecycleShellEpoch != admittedBatch.expectedLifecycleEpoch
                || !admittedBatch.matchesRemainingFrom(
                        admittedBatch.nextOwnerIndex, lifecycleShells)
                || !admittedPermit.acceptAdmittedPhysicalCloseSnapshot(
                        admittedTopology, admittedRevision)) {
            // Ref.unregister(owner) is already irreversible. Allow the exact
            // physical owner to finish closing, but remember that this batch
            // must not advance or authorize the internal document close.
            markAdmittedStateDrifted(admittedPermit,
                    "the admitted physical-close snapshot changed");
        }
        return true;
    }

    /**
     * Advances a batch only after the exact admitted owner has physically
     * left the lifecycle registry and its componentClosed scope has ended.
     */
    synchronized boolean completeSupportCloseOwner(
            SupportCloseBatch batch,
            Object owner,
            Permit permit) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        Permit admittedPermit = Objects.requireNonNull(permit, "permit");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase != SupportClosePhase.ACTIVE
                || activePermit != admittedPermit
                || admittedPermit.supportCloseBatch != admittedBatch
                || admittedPermit.owner != admittedOwner
                || admittedPermit.phase != PermitPhase.ADMITTED
                || admittedPermit.admittedStateDrifted
                || admittedPermit.admittedTopology == null
                || admittedPermit.admittedRevision == null
                || componentClosedScope != null
                || !admittedBatch.isNextOwner(admittedOwner)
                || containsIdentity(lifecycleShells, admittedOwner)
                || (admittedPermit.lastClone
                    && (!admittedPermit.lastCloseConsumed
                        || !admittedPermit.supportCloseConsumed))) {
            return false;
        }
        int nextIndex = admittedBatch.nextOwnerIndex + 1;
        if (admittedBatch.expectedLifecycleEpoch == Long.MAX_VALUE
                || lifecycleShellEpoch
                        != admittedBatch.expectedLifecycleEpoch + 1) {
            return false;
        }
        if (!admittedBatch.matchesRemainingFrom(nextIndex, lifecycleShells)) {
            return false;
        }
        activePermit = null;
        admittedBatch.nextOwnerIndex = nextIndex;
        admittedBatch.expectedLifecycleEpoch = lifecycleShellEpoch;
        if (admittedBatch.complete()) {
            if (admittedBatch.retainForContinuation) {
                admittedBatch.phase = SupportClosePhase.CONTINUATION_READY;
            } else {
                admittedBatch.phase = SupportClosePhase.COMPLETED;
                activeSupportClose = null;
            }
        }
        return true;
    }

    /** Concrete diagnostic for a rejected owner completion. */
    synchronized String supportCloseOwnerCompletionFailureReason(
            SupportCloseBatch batch,
            Object owner,
            Permit permit) {
        if (activeSupportClose != batch) {
            return "the active support-close batch identity changed";
        }
        if (batch.phase != SupportClosePhase.ACTIVE) {
            return "the support-close batch is no longer active";
        }
        if (activePermit != permit) {
            return "the active close-permit identity changed";
        }
        if (permit.supportCloseBatch != batch || permit.owner != owner) {
            return "the close permit is not bound to this batch owner";
        }
        if (permit.phase != PermitPhase.ADMITTED) {
            return "the close permit was not admitted";
        }
        if (permit.admittedStateDrifted) {
            return admittedPermitDriftReason(permit);
        }
        if (permit.admittedTopology == null || permit.admittedRevision == null) {
            return "the admitted post-unregister state was not bound";
        }
        if (componentClosedScope != null) {
            return "the componentClosed scope is still active";
        }
        if (!batch.isNextOwner(owner)) {
            return "the closing shell is not the batch's next owner";
        }
        if (containsIdentity(lifecycleShells, owner)) {
            return "the closing shell remains in the lifecycle registry";
        }
        if (permit.lastClone && !permit.lastCloseConsumed) {
            return "NetBeans did not consume the final clone close";
        }
        if (permit.lastClone && !permit.supportCloseConsumed) {
            return "NetBeans did not consume the internal support close";
        }
        if (batch.expectedLifecycleEpoch == Long.MAX_VALUE
                || lifecycleShellEpoch != batch.expectedLifecycleEpoch + 1) {
            return "the lifecycle epoch did not advance exactly once";
        }
        int nextIndex = batch.nextOwnerIndex + 1;
        if (!batch.matchesRemainingFrom(nextIndex, lifecycleShells)) {
            return "the remaining lifecycle topology does not match the batch";
        }
        return "the exact owner completion was rejected for an unknown reason";
    }

    /**
     * Claims the one post-close continuation retained by a completed batch.
     * The returned proof is an identity token and cannot be acquired twice.
     */
    synchronized PostCloseProof beginPostCloseContinuation(
            SupportCloseBatch batch) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase
                        != SupportClosePhase.CONTINUATION_READY
                || !admittedBatch.complete()
                || !lifecycleShells.isEmpty()
                || activePermit != null
                || componentClosedScope != null) {
            return null;
        }
        admittedBatch.phase = SupportClosePhase.CONTINUATION_RUNNING;
        PostCloseProof proof = new PostCloseProof(
                this, admittedBatch, nextSequenceLocked());
        admittedBatch.postCloseProof = proof;
        return proof;
    }

    /** Releases the retained presentation reservation after the exact callback. */
    synchronized boolean completePostCloseContinuation(
            SupportCloseBatch batch,
            PostCloseProof proof) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        PostCloseProof admittedProof = Objects.requireNonNull(proof, "proof");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase
                        != SupportClosePhase.CONTINUATION_RUNNING
                || admittedBatch.postCloseProof != admittedProof
                || admittedProof.coordinator != this
                || admittedProof.batch != admittedBatch) {
            return false;
        }
        // claim() owns only this proof monitor and never enters the
        // coordinator. Taking the same monitor while already holding the
        // coordinator therefore introduces no reverse lock order and prevents
        // a claimant from returning true after clone admission was released.
        synchronized (admittedProof) {
            if (admittedProof.completed) {
                return false;
            }
            admittedProof.completed = true;
        }
        admittedBatch.postCloseProof = null;
        admittedBatch.phase = SupportClosePhase.COMPLETED;
        activeSupportClose = null;
        return true;
    }

    /** Releases a retained batch when no callback could be dispatched. */
    synchronized boolean abortPostCloseContinuation(SupportCloseBatch batch) {
        SupportCloseBatch admittedBatch = Objects.requireNonNull(batch, "batch");
        if (activeSupportClose != admittedBatch
                || admittedBatch.phase
                        != SupportClosePhase.CONTINUATION_READY) {
            return false;
        }
        admittedBatch.phase = SupportClosePhase.ABORTED;
        activeSupportClose = null;
        return true;
    }

    /**
     * Aborts only while no admitted owner is still physically present.
     * Returning {@code false} deliberately keeps every reservation held.
     */
    synchronized boolean abortSupportClose(SupportCloseBatch batch) {
        return abortSupportCloseLocked(Objects.requireNonNull(batch, "batch"));
    }

    boolean validate(
            Permit permit,
            CloneTopology topology,
            DocumentRevision revision) {
        Objects.requireNonNull(permit, "permit");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        synchronized (this) {
            if (activePermit != permit) {
                return false;
            }
            if (permit.phase != PermitPhase.READY) {
                return false;
            }
            if (!permitLifecycleCurrentLocked(permit)) {
                if (permit.supportCloseBatch != null) {
                    abortSupportCloseLocked(permit.supportCloseBatch);
                } else {
                    activePermit = null;
                }
                return false;
            }
            if (!permit.matches(admittedTopology, admittedRevision)) {
                if (permit.supportCloseBatch == null) {
                    activePermit = null;
                } else {
                    abortSupportCloseLocked(permit.supportCloseBatch);
                }
                return false;
            }
            return true;
        }
    }

    boolean consumeLastClose(
            Permit permit,
            CloneTopology topology,
            DocumentRevision revision) {
        Objects.requireNonNull(permit, "permit");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        synchronized (this) {
            if (activePermit != permit
                    || permit.phase != PermitPhase.COMMITTING
                    || !permitLifecycleCurrentLocked(permit)
                    || !permit.lastClone
                    || permit.lastCloseConsumed
                    || !admittedTopology.isLast(permit.owner)
                    || !permit.matches(admittedTopology, admittedRevision)) {
                return false;
            }
            permit.lastCloseConsumed = true;
            return true;
        }
    }

    boolean beginCommit(
            Permit permit,
            CloneTopology topology,
            DocumentRevision revision) {
        Objects.requireNonNull(permit, "permit");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        synchronized (this) {
            if (activePermit != permit
                    || permit.phase != PermitPhase.READY
                    || !permitLifecycleCurrentLocked(permit)
                    || !permit.matches(admittedTopology, admittedRevision)) {
                if (activePermit == permit
                        && permit.phase == PermitPhase.READY) {
                    if (permit.supportCloseBatch == null) {
                        activePermit = null;
                    } else {
                        abortSupportCloseLocked(permit.supportCloseBatch);
                    }
                }
                return false;
            }
            permit.phase = PermitPhase.COMMITTING;
            return true;
        }
    }

    synchronized boolean markAdmitted(Permit permit) {
        Permit admitted = Objects.requireNonNull(permit, "permit");
        if (activePermit != admitted
                || admitted.phase != PermitPhase.COMMITTING) {
            return false;
        }
        admitted.phase = PermitPhase.ADMITTED;
        if (admitted.supportCloseBatch == null) {
            return permitLifecycleCurrentLocked(admitted);
        }
        if (!permitOwnerLifecycleCurrentLocked(admitted)) {
            return false;
        }
        if (!permitLifecycleCurrentLocked(admitted)
                || !permitBatchOwnerCurrentLocked(admitted)) {
            markAdmittedStateDrifted(admitted,
                    "the batch owner changed while admission was committing");
        }
        return true;
    }

    /**
     * Binds the exact state produced by NetBeans after its irreversible
     * {@code CloneableTopComponent.Ref.unregister(owner)} admission.
     *
     * <p>The clone registry no longer contains {@code owner} at this point,
     * even though the TopComponent remains physically open until
     * {@code componentClosed()}. The final clone may also have unloaded its
     * document through {@code closeLast(false)}. Retries therefore validate
     * this post-admission snapshot, not the pre-admission batch revision.</p>
     */
    synchronized boolean bindAdmittedState(
            Permit permit,
            CloneTopology topology,
            DocumentRevision revision) {
        Permit admitted = Objects.requireNonNull(permit, "permit");
        CloneTopology admittedTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision admittedRevision = Objects.requireNonNull(
                revision, "revision");
        if (activePermit != admitted
                || admitted.phase != PermitPhase.ADMITTED) {
            return false;
        }
        if (admitted.supportCloseBatch != null) {
            if (!permitOwnerLifecycleCurrentLocked(admitted)
                    || admittedTopology.containsIdentity(admitted.owner)) {
                markAdmittedStateDrifted(admitted,
                        "the post-unregister owner lifecycle was not exact");
                return false;
            }
            boolean exact = permitLifecycleCurrentLocked(admitted)
                    && permitBatchOwnerCurrentLocked(admitted)
                    && admitted.topology.sameMembersExcept(
                            admitted.owner, admittedTopology)
                    && admitted.acceptsPostUnregisterRevision(
                            admittedRevision);
            if (!exact) {
                markAdmittedStateDrifted(admitted,
                        "the post-unregister clone topology or document "
                        + "revision was not exact");
                return false;
            }
            admitted.admittedTopology = admittedTopology;
            admitted.admittedRevision = admittedRevision;
            return true;
        }
        if (!permitLifecycleCurrentLocked(admitted)
                || !admitted.topology.sameMembersExcept(
                        admitted.owner, admittedTopology)
                || !admitted.acceptsPostUnregisterRevision(
                        admittedRevision)) {
            if (activePermit == admitted
                    && admitted.phase == PermitPhase.ADMITTED) {
                markAdmittedStateDrifted(admitted,
                        "the ordinary post-unregister topology or document "
                        + "revision was not exact");
            }
            return false;
        }
        admitted.admittedTopology = admittedTopology;
        admitted.admittedRevision = admittedRevision;
        return true;
    }

    synchronized boolean isAdmittedOwner(Permit permit, Object owner) {
        Permit admitted = Objects.requireNonNull(permit, "permit");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        return activePermit == admitted
                && admitted.owner == admittedOwner
                && admitted.phase == PermitPhase.ADMITTED;
    }

    synchronized boolean admittedPhysicalOwnerCurrent(
            Permit permit,
            Object owner) {
        Permit admitted = Objects.requireNonNull(permit, "permit");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        if (activePermit != admitted
                || admitted.owner != admittedOwner
                || admitted.phase != PermitPhase.ADMITTED) {
            return false;
        }
        // Legacy/unit-only permits created without a registered lifecycle use
        // the global fence. Real editor shells always carry an incarnation.
        return admitted.ownerLifecycleStamp == null
                ? permitLifecycleCurrentLocked(admitted)
                : permitOwnerLifecycleCurrentLocked(admitted);
    }

    synchronized boolean releaseStaleAdmittedOwner(
            Permit permit,
            Object owner) {
        Permit admitted = Objects.requireNonNull(permit, "permit");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        if (activePermit != admitted
                || admitted.owner != admittedOwner
                || admitted.phase != PermitPhase.ADMITTED
                || admittedPhysicalOwnerCurrent(admitted, admittedOwner)) {
            return false;
        }
        if (admitted.supportCloseBatch == null) {
            activePermit = null;
            return true;
        }
        return abortSupportCloseLocked(admitted.supportCloseBatch);
    }

    synchronized void abortCommit(Permit permit) {
        Permit rejected = Objects.requireNonNull(permit, "permit");
        if (activePermit == rejected
                && rejected.phase == PermitPhase.COMMITTING) {
            if (rejected.supportCloseBatch == null) {
                activePermit = null;
            } else {
                // Only the exact commit frame may make a COMMITTING batch
                // revocable again. Reentrant callers cannot release authority
                // while NetBeans may already be unregistering the owner.
                rejected.phase = PermitPhase.READY;
                abortSupportCloseLocked(rejected.supportCloseBatch);
            }
        }
    }

    synchronized boolean completeAdmitted(Permit permit) {
        Permit completed = Objects.requireNonNull(permit, "permit");
        if (activePermit != completed
                || completed.phase != PermitPhase.ADMITTED
                || completed.supportCloseBatch != null
                || componentClosedScope != null) {
            return false;
        }
        activePermit = null;
        return true;
    }

    synchronized void revoke(Permit permit) {
        Permit revoked = Objects.requireNonNull(permit, "permit");
        if (activePermit == revoked
                && revoked.phase == PermitPhase.READY) {
            if (revoked.supportCloseBatch == null) {
                activePermit = null;
            } else {
                abortSupportCloseLocked(revoked.supportCloseBatch);
            }
        }
    }

    synchronized void releaseOwner(Object owner) {
        Object releasedOwner = Objects.requireNonNull(owner, "owner");
        if (pendingDecision != null && pendingDecision.owner == releasedOwner) {
            pendingDecision = null;
        }
        if (activePermit != null && activePermit.owner == releasedOwner) {
            Permit releasedPermit = activePermit;
            if (releasedPermit.supportCloseBatch == null) {
                activePermit = null;
            } else if (releasedPermit.phase != PermitPhase.ADMITTED
                    || !permitOwnerLifecycleCurrentLocked(releasedPermit)
                    || !containsIdentity(lifecycleShells, releasedOwner)) {
                abortSupportCloseLocked(releasedPermit.supportCloseBatch);
            }
        }
        if (componentClosedScope != null
                && componentClosedScope.owner == releasedOwner) {
            componentClosedScope.coordinator = null;
            componentClosedScope = null;
        }
    }

    synchronized boolean cloneCreationAllowed() {
        return pendingDecision == null
                && pendingSupportClose == null
                && activeSupportClose == null
                && activePermit == null
                && cloneCreationReservation == null
                && componentClosedScope == null;
    }

    synchronized CloneCreationReservation reserveCloneCreation() {
        if (!cloneCreationAllowed()) {
            return null;
        }
        cloneCreationReservation = new CloneCreationReservation(this);
        return cloneCreationReservation;
    }

    synchronized LifecycleStamp editorShellOpenedOrClosing(Object owner) {
        Object admitted = Objects.requireNonNull(owner, "owner");
        LifecycleStamp current = lifecycleShellStamps.get(admitted);
        if (current != null) {
            return current;
        }
        LifecycleStamp created = new LifecycleStamp(
                admitted, nextLifecycleGenerationLocked());
        lifecycleShellStamps.put(admitted, created);
        lifecycleShells.add(admitted);
        advanceLifecycleShellEpochLocked();
        return created;
    }

    synchronized boolean editorShellClosed(
            Object owner,
            LifecycleStamp lifecycleStamp) {
        Object closed = Objects.requireNonNull(owner, "owner");
        LifecycleStamp admittedStamp = Objects.requireNonNull(
                lifecycleStamp, "lifecycleStamp");
        if (admittedStamp.owner != closed
                || lifecycleShellStamps.get(closed) != admittedStamp) {
            return false;
        }
        lifecycleShellStamps.remove(closed);
        if (!lifecycleShells.removeIf(candidate -> candidate == closed)) {
            throw new IllegalStateException(
                    "The Designer editor lifecycle registry is inconsistent");
        }
        advanceLifecycleShellEpochLocked();
        return true;
    }

    /** Test helper that retires the currently active owner incarnation. */
    synchronized boolean editorShellClosed(Object owner) {
        Object closed = Objects.requireNonNull(owner, "owner");
        LifecycleStamp current = lifecycleShellStamps.get(closed);
        return current != null && editorShellClosed(closed, current);
    }

    synchronized int lifecycleShellCount() {
        return lifecycleShells.size();
    }

    synchronized ComponentClosedScope enterComponentClosed(
            Permit permit,
            Object owner) {
        Permit admittedPermit = Objects.requireNonNull(permit, "permit");
        Object admittedOwner = Objects.requireNonNull(owner, "owner");
        if (componentClosedScope != null
                || activePermit != admittedPermit
                || admittedPermit.owner != admittedOwner
                || admittedPermit.phase != PermitPhase.ADMITTED
                || (admittedPermit.supportCloseBatch == null
                    ? !permitLifecycleCurrentLocked(admittedPermit)
                    : !permitOwnerLifecycleCurrentLocked(admittedPermit))
                || !containsIdentity(lifecycleShells, admittedOwner)) {
            return null;
        }
        if (admittedPermit.supportCloseBatch != null
                && (!permitLifecycleCurrentLocked(admittedPermit)
                    || !permitBatchOwnerCurrentLocked(admittedPermit))) {
            markAdmittedStateDrifted(admittedPermit,
                    "the batch changed before componentClosed began");
        }
        componentClosedScope = new ComponentClosedScope(
                this, admittedPermit, admittedOwner, Thread.currentThread());
        return componentClosedScope;
    }

    synchronized boolean authorizeSupportClose(
            boolean ask,
            CloneTopology topology,
            DocumentRevision revision) {
        if (lifecycleShells.isEmpty()) {
            return pendingDecision == null
                    && pendingSupportClose == null
                    && activeSupportClose == null
                    && activePermit == null
                    && cloneCreationReservation == null
                    && componentClosedScope == null;
        }
        CloneTopology currentTopology = Objects.requireNonNull(
                topology, "topology");
        DocumentRevision currentRevision = Objects.requireNonNull(
                revision, "revision");
        ComponentClosedScope scope = componentClosedScope;
        if (ask
                || scope == null
                || scope.supportCloseStarted
                || scope.permit.supportCloseAttempted
                || scope.thread != Thread.currentThread()
                || activePermit != scope.permit
                || scope.permit.phase != PermitPhase.ADMITTED
                || scope.permit.admittedStateDrifted
                || !permitLifecycleCurrentLocked(scope.permit)
                || !permitBatchOwnerCurrentLocked(scope.permit)
                || !scope.permit.matchesAdmittedSupportCloseCompletion(
                        currentTopology, currentRevision)
                || !scope.permit.lastClone
                || !scope.permit.lastCloseConsumed) {
            if (scope != null
                    && activePermit == scope.permit
                    && scope.permit.phase == PermitPhase.ADMITTED
                    && !scope.permit.matchesAdmittedSupportCloseCompletion(
                            currentTopology, currentRevision)) {
                markAdmittedStateDrifted(scope.permit,
                        "the internal support-close authorization snapshot changed");
            }
            return false;
        }
        scope.supportCloseStarted = true;
        scope.authorizedTopology = currentTopology;
        scope.authorizedRevision = currentRevision;
        scope.permit.supportCloseAttempted = true;
        return true;
    }

    synchronized boolean authorizedSupportCloseRequiresCompletion() {
        ComponentClosedScope scope = componentClosedScope;
        return scope != null
                && scope.supportCloseStarted
                && !scope.supportCloseCompleted
                && scope.thread == Thread.currentThread();
    }

    /** Records the actual result of the exact internal {@code close(false)}. */
    synchronized boolean completeSupportClose(
            boolean succeeded,
            CloneTopology topology,
            DocumentRevision revision) {
        ComponentClosedScope scope = componentClosedScope;
        if (scope == null
                || !scope.supportCloseStarted
                || scope.supportCloseCompleted
                || scope.thread != Thread.currentThread()
                || activePermit != scope.permit
                || scope.permit.phase != PermitPhase.ADMITTED) {
            return false;
        }
        scope.supportCloseCompleted = true;
        boolean snapshotExact = topology != null
                && revision != null
                && scope.authorizedTopology != null
                && scope.authorizedRevision != null
                && scope.authorizedTopology.sameMembers(topology)
                && scope.authorizedRevision.sameSupportCloseCompletion(revision)
                && scope.permit.matchesAdmittedSupportCloseCompletion(
                        topology, revision);
        if (!snapshotExact) {
            markAdmittedStateDrifted(scope.permit,
                    "the internal support-close completion snapshot changed");
        }
        if (succeeded
                && snapshotExact
                && !scope.permit.admittedStateDrifted
                && permitLifecycleCurrentLocked(scope.permit)
                && permitBatchOwnerCurrentLocked(scope.permit)) {
            scope.permit.supportCloseConsumed = true;
        }
        return true;
    }

    synchronized boolean reservedByOther(Object owner) {
        Object candidate = Objects.requireNonNull(owner, "owner");
        return pendingSupportClose != null
                || (activeSupportClose != null
                    && (activePermit == null
                        || activePermit.owner != candidate))
                || (pendingDecision != null && pendingDecision.owner != candidate)
                || (activePermit != null && activePermit.owner != candidate);
    }

    private synchronized void clearPending(PendingDecision expected) {
        if (pendingDecision == expected) {
            pendingDecision = null;
        }
    }

    private synchronized void clearPendingSupportClose(
            PendingSupportClose expected) {
        if (pendingSupportClose == expected) {
            pendingSupportClose = null;
        }
    }

    private boolean abortSupportCloseLocked(SupportCloseBatch batch) {
        if (activeSupportClose != batch
                || batch.phase != SupportClosePhase.ACTIVE) {
            return false;
        }
        Permit permit = activePermit;
        if (permit != null
                && permit.supportCloseBatch == batch
                && permit.phase == PermitPhase.COMMITTING) {
            return false;
        }
        if (permit != null
                && permit.supportCloseBatch == batch
                && permit.phase == PermitPhase.ADMITTED
                && ((componentClosedScope != null
                        && componentClosedScope.permit == permit)
                    || (permitOwnerLifecycleCurrentLocked(permit)
                        && containsIdentity(lifecycleShells, permit.owner)))) {
            return false;
        }
        if (permit != null && permit.supportCloseBatch == batch) {
            activePermit = null;
        }
        batch.phase = SupportClosePhase.ABORTED;
        activeSupportClose = null;
        return true;
    }

    private boolean permitLifecycleCurrentLocked(Permit permit) {
        SupportCloseBatch batch = permit.supportCloseBatch;
        return batch == null
                ? lifecycleShellEpoch == permit.lifecycleEpoch
                : (activeSupportClose == batch
                    && batch.phase == SupportClosePhase.ACTIVE
                    && lifecycleShellEpoch == batch.expectedLifecycleEpoch);
    }

    private boolean permitOwnerLifecycleCurrentLocked(Permit permit) {
        LifecycleStamp stamp = permit.ownerLifecycleStamp;
        return stamp != null
                && stamp.owner == permit.owner
                && lifecycleShellStamps.get(permit.owner) == stamp;
    }

    private boolean permitBatchOwnerCurrentLocked(Permit permit) {
        SupportCloseBatch batch = permit.supportCloseBatch;
        return batch == null
                || (activeSupportClose == batch
                    && batch.phase == SupportClosePhase.ACTIVE
                    && batch.isNextOwner(permit.owner)
                    && batch.matchesRemainingFrom(
                            batch.nextOwnerIndex, lifecycleShells));
    }

    private long nextSequenceLocked() {
        if (nextSequence == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Designer editor close-permit sequence is exhausted");
        }
        return ++nextSequence;
    }

    private void advanceLifecycleShellEpochLocked() {
        if (lifecycleShellEpoch == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Designer editor lifecycle epoch is exhausted");
        }
        lifecycleShellEpoch++;
    }

    private long nextLifecycleGenerationLocked() {
        if (nextLifecycleGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Designer editor lifecycle generation is exhausted");
        }
        return ++nextLifecycleGeneration;
    }

    private static void markAdmittedStateDrifted(
            Permit permit,
            String reason) {
        permit.admittedStateDrifted = true;
        if (permit.admittedStateDriftReason == null) {
            permit.admittedStateDriftReason = Objects.requireNonNull(
                    reason, "reason");
        }
    }

    private static String admittedPermitDriftReason(Permit permit) {
        return permit.admittedStateDriftReason == null
                ? "the admitted clone topology or document revision drifted"
                : permit.admittedStateDriftReason;
    }

    private synchronized void releaseCloneCreation(
            CloneCreationReservation reservation) {
        if (cloneCreationReservation != reservation) {
            throw new IllegalStateException(
                    "The Designer editor clone-creation reservation is not active");
        }
        cloneCreationReservation = null;
    }

    private synchronized void releaseComponentClosedScope(
            ComponentClosedScope scope) {
        if (componentClosedScope != scope) {
            throw new IllegalStateException(
                    "The Designer editor componentClosed scope is not active");
        }
        componentClosedScope = null;
    }

    private static CloneTopology requireTopology(
            Supplier<CloneTopology> supplier) {
        return Objects.requireNonNull(
                supplier.get(), "clone topology supplier returned null");
    }

    private static DocumentRevision requireRevision(
            Supplier<DocumentRevision> supplier) {
        return Objects.requireNonNull(
                supplier.get(), "document revision supplier returned null");
    }

    enum AdmissionStatus {
        ACQUIRED,
        RETAINED,
        BUSY,
        CANCELLED,
        STALE,
        INVALID_TOPOLOGY
    }

    enum SupportCloseAdmissionStatus {
        ACQUIRED,
        BUSY,
        CANCELLED,
        STALE,
        INVALID_TOPOLOGY
    }

    enum SupportCloseOwnerState {
        NONE,
        READY,
        COMMITTING,
        ADMITTED
    }

    record SupportCloseAdmission(
            SupportCloseAdmissionStatus status,
            SupportCloseBatch batch) {
        SupportCloseAdmission {
            Objects.requireNonNull(status, "status");
            if ((status == SupportCloseAdmissionStatus.ACQUIRED)
                    != (batch != null)) {
                throw new IllegalArgumentException(
                        "Only an acquired support close may carry a batch");
            }
        }

        static SupportCloseAdmission acquired(SupportCloseBatch batch) {
            return new SupportCloseAdmission(
                    SupportCloseAdmissionStatus.ACQUIRED,
                    Objects.requireNonNull(batch, "batch"));
        }

        static SupportCloseAdmission rejected(
                SupportCloseAdmissionStatus status) {
            return new SupportCloseAdmission(status, null);
        }

        boolean authorized() {
            return batch != null;
        }
    }

    record Admission(AdmissionStatus status, Permit permit) {
        Admission {
            Objects.requireNonNull(status, "status");
            boolean authorized = status == AdmissionStatus.ACQUIRED
                    || status == AdmissionStatus.RETAINED;
            if (authorized != (permit != null)) {
                throw new IllegalArgumentException(
                        "Only an authorized admission may carry a close permit");
            }
        }

        static Admission authorized(AdmissionStatus status, Permit permit) {
            return new Admission(status, Objects.requireNonNull(permit, "permit"));
        }

        static Admission rejected(AdmissionStatus status) {
            return new Admission(status, null);
        }

        boolean authorized() {
            return permit != null;
        }
    }

    static final class Permit {
        private final long sequence;
        private final Object owner;
        private final CloneTopology topology;
        private final DocumentRevision revision;
        private final boolean lastClone;
        private final SupportCloseBatch supportCloseBatch;
        private final long lifecycleEpoch;
        private final LifecycleStamp ownerLifecycleStamp;
        private PermitPhase phase = PermitPhase.READY;
        private boolean lastCloseConsumed;
        private boolean supportCloseAttempted;
        private boolean supportCloseConsumed;
        private boolean admittedStateDrifted;
        private String admittedStateDriftReason;
        private CloneTopology admittedTopology;
        private DocumentRevision admittedRevision;

        private Permit(
                long sequence,
                Object owner,
                CloneTopology topology,
                DocumentRevision revision,
                boolean lastClone,
                SupportCloseBatch supportCloseBatch,
                long lifecycleEpoch,
                LifecycleStamp ownerLifecycleStamp) {
            this.sequence = sequence;
            this.owner = owner;
            this.topology = topology;
            this.revision = revision;
            this.lastClone = lastClone;
            this.supportCloseBatch = supportCloseBatch;
            this.lifecycleEpoch = lifecycleEpoch;
            this.ownerLifecycleStamp = ownerLifecycleStamp;
        }

        long sequence() {
            return sequence;
        }

        boolean lastClone() {
            return lastClone;
        }

        LifecycleStamp ownerLifecycleStamp() {
            return ownerLifecycleStamp;
        }

        private boolean matches(
                CloneTopology candidateTopology,
                DocumentRevision candidateRevision) {
            return topology.sameMembers(candidateTopology)
                    && revision.sameRevision(candidateRevision);
        }

        private boolean acceptsPostUnregisterRevision(
                DocumentRevision candidateRevision) {
            return revision.sameRevision(candidateRevision)
                    || (lastClone
                        && lastCloseConsumed
                        && revision.sameSupportCloseCompletion(
                                candidateRevision));
        }

        private boolean acceptAdmittedPhysicalCloseSnapshot(
                CloneTopology candidateTopology,
                DocumentRevision candidateRevision) {
            if (admittedTopology == null
                    || admittedRevision == null
                    || !admittedTopology.sameMembers(candidateTopology)) {
                return false;
            }
            if (admittedRevision.sameRevision(candidateRevision)) {
                return true;
            }
            if (!admittedRevision.sameSupportCloseCompletion(
                    candidateRevision)) {
                return false;
            }
            // CloneableEditor.closeLast(false) may unload the already-clean
            // document between our explicit gate and WindowManager's second
            // canClose callback. Bind that single fenced terminal snapshot so
            // componentClosed and the internal support close validate the
            // same identity from here on.
            admittedRevision = candidateRevision;
            return true;
        }

        private boolean matchesAdmittedSupportCloseCompletion(
                CloneTopology candidateTopology,
                DocumentRevision candidateRevision) {
            return admittedTopology != null
                    && admittedRevision != null
                    && admittedTopology.sameMembers(candidateTopology)
                    && admittedRevision.sameSupportCloseCompletion(
                            candidateRevision);
        }
    }

    static final class SupportCloseBatch {
        private final long sequence;
        private final CloneTopology topology;
        private final DocumentRevision revision;
        private int nextOwnerIndex;
        private long expectedLifecycleEpoch;
        private final boolean retainForContinuation;
        private PostCloseProof postCloseProof;
        private SupportClosePhase phase = SupportClosePhase.ACTIVE;

        private SupportCloseBatch(
                long sequence,
                CloneTopology topology,
                DocumentRevision revision,
                long expectedLifecycleEpoch,
                boolean retainForContinuation) {
            this.sequence = sequence;
            this.topology = topology;
            this.revision = revision;
            this.expectedLifecycleEpoch = expectedLifecycleEpoch;
            this.retainForContinuation = retainForContinuation;
        }

        long sequence() {
            return sequence;
        }

        int ownerCount() {
            return topology.size();
        }

        private boolean isNextOwner(Object owner) {
            return nextOwnerIndex < topology.size()
                    && topology.memberAt(nextOwnerIndex) == owner;
        }

        private boolean matchesRemaining(
                CloneTopology candidate,
                Collection<?> lifecycle) {
            return matchesRemainingMembersFrom(
                    nextOwnerIndex, candidate.members)
                    && matchesRemainingFrom(nextOwnerIndex, lifecycle);
        }

        private boolean matchesRemainingMembersFrom(
                int startIndex,
                Collection<?> candidate) {
            int expectedSize = topology.size() - startIndex;
            if (expectedSize != candidate.size()) {
                return false;
            }
            for (Object member : candidate) {
                boolean found = false;
                for (int index = startIndex; index < topology.size(); index++) {
                    if (topology.memberAt(index) == member) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        private boolean matchesRemainingFrom(
                int startIndex,
                Collection<?> candidate) {
            int expectedSize = topology.size() - startIndex;
            if (expectedSize != candidate.size()) {
                return false;
            }
            int index = startIndex;
            for (Object member : candidate) {
                if (topology.memberAt(index++) != member) {
                    return false;
                }
            }
            return true;
        }

        private boolean complete() {
            return nextOwnerIndex == topology.size();
        }
    }

    /** One unforgeable identity proving a retained support-close completion. */
    static final class PostCloseProof {
        private final FlutterDesignerEditorClosePermitCoordinator coordinator;
        private final SupportCloseBatch batch;
        private final long sequence;
        private volatile boolean completed;

        private PostCloseProof(
                FlutterDesignerEditorClosePermitCoordinator coordinator,
                SupportCloseBatch batch,
                long sequence) {
            this.coordinator = coordinator;
            this.batch = batch;
            this.sequence = sequence;
        }

        long sequence() {
            return sequence;
        }

        boolean belongsTo(
                FlutterDesignerEditorClosePermitCoordinator expectedCoordinator) {
            return coordinator == expectedCoordinator && !completed;
        }

        synchronized boolean claim(
                FlutterDesignerEditorClosePermitCoordinator expectedCoordinator,
                Object owner) {
            Objects.requireNonNull(owner, "owner");
            if (coordinator != expectedCoordinator
                    || completed
                    || claimOwner != null) {
                return false;
            }
            claimOwner = owner;
            return true;
        }

        private Object claimOwner;
    }

    /** Unique identity for one physical open incarnation of an editor shell. */
    static final class LifecycleStamp {
        private final Object owner;
        private final long generation;

        private LifecycleStamp(Object owner, long generation) {
            this.owner = owner;
            this.generation = generation;
        }

        long generation() {
            return generation;
        }
    }

    static final class CloneCreationReservation implements AutoCloseable {
        private FlutterDesignerEditorClosePermitCoordinator coordinator;

        private CloneCreationReservation(
                FlutterDesignerEditorClosePermitCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Override
        public void close() {
            FlutterDesignerEditorClosePermitCoordinator owner = coordinator;
            if (owner == null) {
                return;
            }
            owner.releaseCloneCreation(this);
            coordinator = null;
        }
    }

    static final class ComponentClosedScope implements AutoCloseable {
        private FlutterDesignerEditorClosePermitCoordinator coordinator;
        private final Permit permit;
        private final Object owner;
        private final Thread thread;
        private boolean supportCloseStarted;
        private boolean supportCloseCompleted;
        private CloneTopology authorizedTopology;
        private DocumentRevision authorizedRevision;

        private ComponentClosedScope(
                FlutterDesignerEditorClosePermitCoordinator coordinator,
                Permit permit,
                Object owner,
                Thread thread) {
            this.coordinator = coordinator;
            this.permit = permit;
            this.owner = owner;
            this.thread = thread;
        }

        @Override
        public void close() {
            FlutterDesignerEditorClosePermitCoordinator current = coordinator;
            if (current == null) {
                return;
            }
            current.releaseComponentClosedScope(this);
            coordinator = null;
        }
    }

    private enum PermitPhase {
        READY,
        COMMITTING,
        ADMITTED
    }

    private enum SupportClosePhase {
        ACTIVE,
        CONTINUATION_READY,
        CONTINUATION_RUNNING,
        COMPLETED,
        ABORTED
    }

    static final class CloneTopology {
        private final List<Object> members;

        CloneTopology(Collection<?> members) {
            Objects.requireNonNull(members, "members");
            ArrayList<Object> admitted = new ArrayList<>(members.size());
            for (Object member : members) {
                Object candidate = Objects.requireNonNull(
                        member, "clone topology member");
                if (containsIdentity(admitted, candidate)) {
                    throw new IllegalArgumentException(
                            "Clone topology contains the same member twice");
                }
                admitted.add(candidate);
            }
            this.members = List.copyOf(admitted);
        }

        int size() {
            return members.size();
        }

        boolean containsIdentity(Object candidate) {
            return containsIdentity(members, candidate);
        }

        boolean isLast(Object owner) {
            return members.size() == 1 && members.get(0) == owner;
        }

        boolean sameMembers(CloneTopology other) {
            Objects.requireNonNull(other, "other");
            if (members.size() != other.members.size()) {
                return false;
            }
            for (Object member : members) {
                if (!other.containsIdentity(member)) {
                    return false;
                }
            }
            return true;
        }

        boolean sameMembers(Collection<?> other) {
            Objects.requireNonNull(other, "other");
            if (members.size() != other.size()) {
                return false;
            }
            for (Object member : members) {
                if (!containsIdentity(other, member)) {
                    return false;
                }
            }
            return true;
        }

        boolean sameMembersExcept(Object excluded, CloneTopology other) {
            Objects.requireNonNull(excluded, "excluded");
            Objects.requireNonNull(other, "other");
            if (!containsIdentity(excluded)
                    || members.size() - 1 != other.members.size()) {
                return false;
            }
            for (Object member : members) {
                if (member != excluded && !other.containsIdentity(member)) {
                    return false;
                }
            }
            return true;
        }

        boolean sameOrderedMembers(CloneTopology other) {
            Objects.requireNonNull(other, "other");
            return sameOrderedMembers(other.members);
        }

        boolean sameOrderedMembers(Collection<?> other) {
            Objects.requireNonNull(other, "other");
            if (members.size() != other.size()) {
                return false;
            }
            int index = 0;
            for (Object member : other) {
                if (members.get(index++) != member) {
                    return false;
                }
            }
            return true;
        }

        private Object memberAt(int index) {
            return members.get(index);
        }

        private static boolean containsIdentity(
                Collection<?> members,
                Object candidate) {
            for (Object member : members) {
                if (member == candidate) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean containsIdentity(
            Collection<?> members,
            Object candidate) {
        for (Object member : members) {
            if (member == candidate) {
                return true;
            }
        }
        return false;
    }

    record DocumentRevision(
            Object documentIdentity,
            long documentVersion,
            PairSaveCoordinator.CloseRevision pairRevision,
            boolean modified,
            boolean sourceModified) {
        DocumentRevision {
            if (documentVersion < -1) {
                throw new IllegalArgumentException(
                        "documentVersion must be -1 or non-negative");
            }
        }

        boolean sameRevision(DocumentRevision other) {
            Objects.requireNonNull(other, "other");
            return documentIdentity == other.documentIdentity
                    && documentVersion == other.documentVersion
                    && samePairRevision(pairRevision, other.pairRevision)
                    && modified == other.modified
                    && sourceModified == other.sourceModified;
        }

        /**
         * The internal final-support close may unload an already-clean Swing
         * document. That one terminal transition is expected; every pair epoch
         * and dirty flag must still match exactly.
         */
        boolean sameSupportCloseCompletion(DocumentRevision other) {
            Objects.requireNonNull(other, "other");
            if (sameRevision(other)) {
                return true;
            }
            return documentIdentity != null
                    && documentVersion >= 0
                    && other.documentIdentity == null
                    && other.documentVersion == -1
                    && !modified
                    && !sourceModified
                    && !other.modified
                    && !other.sourceModified
                    && samePairRevision(pairRevision, other.pairRevision);
        }

        private static boolean samePairRevision(
                PairSaveCoordinator.CloseRevision first,
                PairSaveCoordinator.CloseRevision second) {
            if (first == null || second == null) {
                return first == second;
            }
            return first.sameRevision(second);
        }
    }

    private record PendingDecision(
            long sequence,
            Object owner,
            CloneTopology topology,
            boolean lastClone,
            long lifecycleEpoch) {
    }

    private record PendingSupportClose(
            long sequence,
            CloneTopology topology,
            long lifecycleEpoch) {
    }
}
