package dev.flutter.netbeans.dart;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import java.time.Duration;
import java.util.Objects;

/** Shared candidate-capacity identity plus independent protocol/time limits. */
public record DartCandidateAnalysisLimits(
        DartCandidateCapacityBudget candidateCapacityBudget,
        int maxJsonLineBytes,
        int maxDiagnostics,
        int maxDiagnosticTextChars,
        Duration totalTimeout,
        Duration closeTimeout) {

    public static final DartCandidateAnalysisLimits DEFAULT =
            new DartCandidateAnalysisLimits(
                    DartCandidateCapacityBudget.DEFAULT,
                    16 * 1024 * 1024,
                    2_048,
                    65_536,
                    Duration.ofSeconds(45),
                    Duration.ofSeconds(2));

    public DartCandidateAnalysisLimits {
        Objects.requireNonNull(candidateCapacityBudget,
                "candidateCapacityBudget");
        if (maxJsonLineBytes <= 0
                || maxDiagnostics <= 0
                || maxDiagnosticTextChars <= 0) {
            throw new IllegalArgumentException("analysis limits must be positive");
        }
        totalTimeout = requirePositive(totalTimeout, "totalTimeout");
        closeTimeout = requirePositive(closeTimeout, "closeTimeout");
    }

    public int maxCandidateBytes() {
        return candidateCapacityBudget.maxCandidateUtf8Bytes();
    }

    public int maxSymbolProbes() {
        return candidateCapacityBudget.maxSymbolProbes();
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
