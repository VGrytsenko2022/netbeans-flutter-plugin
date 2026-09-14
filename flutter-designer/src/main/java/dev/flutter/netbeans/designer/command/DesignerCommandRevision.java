package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartUserSourceProjection;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, fully derived state at one command cursor.
 *
 * <p>The byte values are candidates only; this type performs no I/O and grants
 * no persistence authority.</p>
 */
public final class DesignerCommandRevision {
    private final long revisionId;
    private final DesignerDocument document;
    private final OriginalFdBytes fdSnapshot;
    private final DartGenerationResult generation;
    private final byte[] dartCandidate;
    private final DartSourceIntegrityResult sourceIntegrity;
    private final Optional<DartSourceTransitionPlan> sourceTransition;
    private final Optional<PreparedDesignerPair> preparedPair;
    private final DesignerRevisionPersistenceKind persistenceKind;
    private final DartUserSourceProjection userSourceProjection;
    private final boolean historicalUserEnvelope;

    DesignerCommandRevision(
            long revisionId,
            DesignerDocument document,
            OriginalFdBytes fdSnapshot,
            DartGenerationResult generation,
            byte[] dartCandidate,
            DartSourceIntegrityResult sourceIntegrity,
            Optional<DartSourceTransitionPlan> sourceTransition,
            Optional<PreparedDesignerPair> preparedPair,
            DesignerRevisionPersistenceKind persistenceKind,
            DartUserSourceProjection userSourceProjection) {
        this(revisionId, document, fdSnapshot, generation, dartCandidate, sourceIntegrity,
                sourceTransition, preparedPair, persistenceKind, userSourceProjection, false);
    }

    private DesignerCommandRevision(long revisionId, DesignerDocument document, OriginalFdBytes fdSnapshot,
            DartGenerationResult generation, byte[] dartCandidate, DartSourceIntegrityResult sourceIntegrity,
            Optional<DartSourceTransitionPlan> sourceTransition, Optional<PreparedDesignerPair> preparedPair,
            DesignerRevisionPersistenceKind persistenceKind, DartUserSourceProjection userSourceProjection,
            boolean historicalUserEnvelope) {
        if (revisionId < 0) {
            throw new IllegalArgumentException("revisionId must not be negative");
        }
        this.revisionId = revisionId;
        this.document = Objects.requireNonNull(document, "document");
        this.fdSnapshot = Objects.requireNonNull(fdSnapshot, "fdSnapshot");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.dartCandidate = Objects.requireNonNull(
                dartCandidate, "dartCandidate").clone();
        this.sourceIntegrity = Objects.requireNonNull(
                sourceIntegrity, "sourceIntegrity");
        this.sourceTransition = Objects.requireNonNull(
                sourceTransition, "sourceTransition");
        this.preparedPair = Objects.requireNonNull(preparedPair, "preparedPair");
        this.persistenceKind = Objects.requireNonNull(
                persistenceKind, "persistenceKind");
        this.userSourceProjection = Objects.requireNonNull(userSourceProjection, "userSourceProjection");
        this.historicalUserEnvelope = historicalUserEnvelope;

        if (!generation.successful()) {
            throw new IllegalArgumentException("revision generation must be successful");
        }
        DartCandidateCapacityBudget capacity = generation.generated()
                .orElseThrow().candidateCapacityBudget();
        if (this.dartCandidate.length > capacity.maxCandidateUtf8Bytes()) {
            throw new IllegalArgumentException(
                    "revision Dart candidate exceeds its exact shared capacity budget");
        }
        if (!sourceIntegrity.onDiskDeclaredMatch()
                || sourceIntegrity.original().isEmpty()
                || !sourceIntegrity.original().orElseThrow()
                        .contentEquals(this.dartCandidate)) {
            throw new IllegalArgumentException(
                    "revision Dart candidate must retain exact matching source evidence");
        }
        if (!userSourceProjection.targetMatches(this.dartCandidate, document.source())) {
            throw new IllegalArgumentException("User-source proof must match the exact revision envelope");
        }
        sourceTransition.ifPresent(transition -> {
            if (!transition.prospectiveDescriptor().equals(document.source())
                    || !Arrays.equals(
                            transition.candidateBytes(), this.dartCandidate)) {
                throw new IllegalArgumentException(
                        "source transition must describe the exact revision candidate");
            }
        });
        preparedPair.ifPresent(pair -> {
            if (sourceTransition.isEmpty()
                    || pair.dartTransition() != sourceTransition.orElseThrow()
                    || pair.dartTransition().generation() != generation
                    || pair.dartTransition().generation().generated()
                            .orElseThrow().candidateCapacityBudget() != capacity
                    || !pair.prospectiveDocument().equals(document)
                    || !pair.prospectiveFd().equals(fdSnapshot)
                    || !Arrays.equals(
                            pair.prospectiveDartBytes(), this.dartCandidate)) {
                throw new IllegalArgumentException(
                        "prepared pair must bind the exact revision outputs");
            }
        });
        if ((persistenceKind == DesignerRevisionPersistenceKind.PAIRED)
                != preparedPair.isPresent()) {
            throw new IllegalArgumentException(
                    "only a PAIRED revision must publish a prepared pair");
        }
        if (persistenceKind == DesignerRevisionPersistenceKind.FD_ONLY
                && sourceTransition.isPresent()) {
            throw new IllegalArgumentException(
                    "an FD_ONLY revision cannot publish a Dart transition");
        }
    }

    public long revisionId() {
        return revisionId;
    }

    public DesignerDocument document() {
        return document;
    }

    public OriginalFdBytes fdSnapshot() {
        return fdSnapshot;
    }

    public byte[] fdBytes() {
        return fdSnapshot.copyBytes();
    }

    public DartGenerationResult generation() {
        return generation;
    }

    public byte[] dartCandidateBytes() {
        return dartCandidate.clone();
    }

    public DartSourceIntegrityResult sourceIntegrity() {
        return sourceIntegrity;
    }

    public Optional<DartSourceTransitionPlan> sourceTransition() {
        return sourceTransition;
    }

    public Optional<PreparedDesignerPair> preparedPair() {
        return preparedPair;
    }

    public DesignerRevisionPersistenceKind persistenceKind() {
        return persistenceKind;
    }

    /** Opaque bounded proof of deliberate user-member edits, never persisted in the .fd model. */
    public DartUserSourceProjection userSourceProjection() {
        return userSourceProjection;
    }

    boolean historicalUserEnvelope() {
        return historicalUserEnvelope;
    }

    DesignerCommandRevision preservingHistoricalUserEnvelope() {
        return historicalUserEnvelope ? this : new DesignerCommandRevision(revisionId, document, fdSnapshot,
                generation, dartCandidate, sourceIntegrity, sourceTransition, preparedPair, persistenceKind,
                userSourceProjection, true);
    }

    /** Exact shared capacity identity under which this revision was derived. */
    public DartCandidateCapacityBudget candidateCapacityBudget() {
        return generation.generated().orElseThrow().candidateCapacityBudget();
    }

    long retainedPairBytes() {
        return (long) fdSnapshot.size() + dartCandidate.length + userSourceProjection.retainedBytes();
    }
}
