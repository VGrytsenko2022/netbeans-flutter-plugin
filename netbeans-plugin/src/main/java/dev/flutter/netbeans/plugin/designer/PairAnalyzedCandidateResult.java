package dev.flutter.netbeans.plugin.designer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Closed result of consuming one exact candidate-analysis ticket. */
sealed interface PairAnalyzedCandidateResult permits
        PairAnalyzedCandidateResult.Ready,
        PairAnalyzedCandidateResult.Rejected {

    boolean ready();

    Optional<PairAnalyzedCandidate> analyzedOptional();

    List<PairSaveEvidenceDiagnostic> diagnostics();

    record Ready(PairAnalyzedCandidate analyzed)
            implements PairAnalyzedCandidateResult {
        public Ready {
            Objects.requireNonNull(analyzed, "analyzed");
        }

        @Override
        public boolean ready() {
            return true;
        }

        @Override
        public Optional<PairAnalyzedCandidate> analyzedOptional() {
            return Optional.of(analyzed);
        }

        @Override
        public List<PairSaveEvidenceDiagnostic> diagnostics() {
            return List.of();
        }
    }

    record Rejected(List<PairSaveEvidenceDiagnostic> reasons)
            implements PairAnalyzedCandidateResult {
        public Rejected {
            reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
            if (reasons.isEmpty()) {
                throw new IllegalArgumentException(
                        "a rejected analyzed candidate requires a diagnostic");
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
        public Optional<PairAnalyzedCandidate> analyzedOptional() {
            return Optional.empty();
        }

        @Override
        public List<PairSaveEvidenceDiagnostic> diagnostics() {
            return reasons;
        }
    }
}
