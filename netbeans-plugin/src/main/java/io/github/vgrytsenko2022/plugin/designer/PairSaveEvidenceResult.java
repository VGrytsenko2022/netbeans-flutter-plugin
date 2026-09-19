package io.github.vgrytsenko2022.plugin.designer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Closed result of evaluating exact in-memory pair-save evidence. */
sealed interface PairSaveEvidenceResult permits
        PairSaveEvidenceResult.Ready,
        PairSaveEvidenceResult.Rejected {

    boolean ready();

    Optional<PairSaveEvidence> evidenceOptional();

    List<PairSaveEvidenceDiagnostic> diagnostics();

    record Ready(PairSaveEvidence evidence) implements PairSaveEvidenceResult {
        public Ready {
            Objects.requireNonNull(evidence, "evidence");
        }

        @Override
        public boolean ready() {
            return true;
        }

        @Override
        public Optional<PairSaveEvidence> evidenceOptional() {
            return Optional.of(evidence);
        }

        @Override
        public List<PairSaveEvidenceDiagnostic> diagnostics() {
            return List.of();
        }
    }

    record Rejected(List<PairSaveEvidenceDiagnostic> reasons)
            implements PairSaveEvidenceResult {
        public Rejected {
            reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
            if (reasons.isEmpty()) {
                throw new IllegalArgumentException(
                        "a rejected pair-save result requires a diagnostic");
            }
            if (reasons.stream().anyMatch(Objects::isNull)) {
                throw new NullPointerException("reasons contains null");
            }
        }

        @Override
        public boolean ready() {
            return false;
        }

        @Override
        public Optional<PairSaveEvidence> evidenceOptional() {
            return Optional.empty();
        }

        @Override
        public List<PairSaveEvidenceDiagnostic> diagnostics() {
            return reasons;
        }
    }
}
