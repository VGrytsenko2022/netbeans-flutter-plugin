package dev.flutter.netbeans.plugin.designer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private Permit activePermit;
    private CloneCreationReservation cloneCreationReservation;
    private final List<Object> lifecycleShells = new ArrayList<>();
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
            if (pendingDecision != null || cloneCreationReservation != null) {
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
                if (!retained.matches(topology, revision)) {
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
                    || activePermit != null
                    || cloneCreationReservation != null) {
                return Admission.rejected(AdmissionStatus.BUSY);
            }
            if (nextSequence == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "Designer editor close-permit sequence is exhausted");
            }
            pending = new PendingDecision(
                    ++nextSequence, admittedOwner, initialTopology, lastClone);
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
                    || pending.lastClone != finalTopology.isLast(admittedOwner)) {
                return Admission.rejected(AdmissionStatus.STALE);
            }
            Permit permit = new Permit(
                    pending.sequence,
                    admittedOwner,
                    finalTopology,
                    revision,
                    lastClone);
            activePermit = permit;
            return Admission.authorized(AdmissionStatus.ACQUIRED, permit);
        }
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
            if (!permit.matches(admittedTopology, admittedRevision)) {
                activePermit = null;
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
                    || !permit.matches(admittedTopology, admittedRevision)) {
                if (activePermit == permit
                        && permit.phase == PermitPhase.READY) {
                    activePermit = null;
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
        return true;
    }

    synchronized void abortCommit(Permit permit) {
        Permit rejected = Objects.requireNonNull(permit, "permit");
        if (activePermit == rejected
                && rejected.phase == PermitPhase.COMMITTING) {
            activePermit = null;
        }
    }

    synchronized boolean completeAdmitted(Permit permit) {
        Permit completed = Objects.requireNonNull(permit, "permit");
        if (activePermit != completed
                || completed.phase != PermitPhase.ADMITTED
                || componentClosedScope != null) {
            return false;
        }
        activePermit = null;
        return true;
    }

    synchronized void revoke(Permit permit) {
        Permit revoked = Objects.requireNonNull(permit, "permit");
        if (activePermit == revoked
                && revoked.phase != PermitPhase.ADMITTED) {
            activePermit = null;
        }
    }

    synchronized void releaseOwner(Object owner) {
        Object releasedOwner = Objects.requireNonNull(owner, "owner");
        if (pendingDecision != null && pendingDecision.owner == releasedOwner) {
            pendingDecision = null;
        }
        if (activePermit != null && activePermit.owner == releasedOwner) {
            activePermit = null;
        }
        if (componentClosedScope != null
                && componentClosedScope.owner == releasedOwner) {
            componentClosedScope.coordinator = null;
            componentClosedScope = null;
        }
    }

    synchronized boolean cloneCreationAllowed() {
        return pendingDecision == null
                && activePermit == null
                && cloneCreationReservation == null;
    }

    synchronized CloneCreationReservation reserveCloneCreation() {
        if (!cloneCreationAllowed()) {
            return null;
        }
        cloneCreationReservation = new CloneCreationReservation(this);
        return cloneCreationReservation;
    }

    synchronized void editorShellOpenedOrClosing(Object owner) {
        Object admitted = Objects.requireNonNull(owner, "owner");
        if (!containsIdentity(lifecycleShells, admitted)) {
            lifecycleShells.add(admitted);
        }
    }

    synchronized void editorShellClosed(Object owner) {
        Object closed = Objects.requireNonNull(owner, "owner");
        lifecycleShells.removeIf(candidate -> candidate == closed);
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
                || !containsIdentity(lifecycleShells, admittedOwner)) {
            return null;
        }
        componentClosedScope = new ComponentClosedScope(
                this, admittedPermit, admittedOwner, Thread.currentThread());
        return componentClosedScope;
    }

    synchronized boolean authorizeSupportClose(boolean ask) {
        if (lifecycleShells.isEmpty()) {
            return pendingDecision == null
                    && activePermit == null
                    && cloneCreationReservation == null
                    && componentClosedScope == null;
        }
        ComponentClosedScope scope = componentClosedScope;
        if (ask
                || scope == null
                || scope.supportCloseConsumed
                || scope.thread != Thread.currentThread()
                || activePermit != scope.permit
                || scope.permit.phase != PermitPhase.ADMITTED
                || !scope.permit.lastClone
                || !scope.permit.lastCloseConsumed) {
            return false;
        }
        scope.supportCloseConsumed = true;
        return true;
    }

    synchronized boolean reservedByOther(Object owner) {
        Object candidate = Objects.requireNonNull(owner, "owner");
        return (pendingDecision != null && pendingDecision.owner != candidate)
                || (activePermit != null && activePermit.owner != candidate);
    }

    private synchronized void clearPending(PendingDecision expected) {
        if (pendingDecision == expected) {
            pendingDecision = null;
        }
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
        private PermitPhase phase = PermitPhase.READY;
        private boolean lastCloseConsumed;

        private Permit(
                long sequence,
                Object owner,
                CloneTopology topology,
                DocumentRevision revision,
                boolean lastClone) {
            this.sequence = sequence;
            this.owner = owner;
            this.topology = topology;
            this.revision = revision;
            this.lastClone = lastClone;
        }

        long sequence() {
            return sequence;
        }

        boolean lastClone() {
            return lastClone;
        }

        private boolean matches(
                CloneTopology candidateTopology,
                DocumentRevision candidateRevision) {
            return topology.sameMembers(candidateTopology)
                    && revision.sameRevision(candidateRevision);
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
        private boolean supportCloseConsumed;

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
            boolean lastClone) {
    }
}
