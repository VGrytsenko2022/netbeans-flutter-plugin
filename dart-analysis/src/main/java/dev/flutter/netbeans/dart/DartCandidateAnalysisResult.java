package dev.flutter.netbeans.dart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable fail-closed evidence for one exact candidate snapshot. */
public record DartCandidateAnalysisResult(
        DartCandidateAnalysisStatus status,
        DartCandidateSnapshot snapshot,
        Optional<String> analyzerProtocolVersion,
        List<DartCandidateDiagnostic> diagnostics,
        int requestedSymbolProbes,
        List<DartSymbolEvidence> symbolEvidence,
        Optional<DartCandidateAnalysisIssue> issue) {

    public DartCandidateAnalysisResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(snapshot, "snapshot");
        analyzerProtocolVersion = Objects.requireNonNull(
                analyzerProtocolVersion, "analyzerProtocolVersion")
                .map(value -> {
                    if (value.isBlank()) {
                        throw new IllegalArgumentException(
                                "analyzerProtocolVersion must not be blank");
                    }
                    return value.strip();
                });
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        if (requestedSymbolProbes < 0) {
            throw new IllegalArgumentException("requestedSymbolProbes must not be negative");
        }
        symbolEvidence = List.copyOf(Objects.requireNonNull(
                symbolEvidence, "symbolEvidence"));
        if (symbolEvidence.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("symbolEvidence contains null");
        }
        issue = Objects.requireNonNull(issue, "issue");

        boolean blockingDiagnostic = diagnostics.stream().anyMatch(
                DartCandidateDiagnostic::blocking);
        boolean rejectedSymbol = symbolEvidence.stream().anyMatch(
                evidence -> !evidence.accepted());
        if (status == DartCandidateAnalysisStatus.PASSED) {
            if (analyzerProtocolVersion.isEmpty()
                    || issue.isPresent()
                    || blockingDiagnostic
                    || rejectedSymbol
                    || symbolEvidence.size() != requestedSymbolProbes) {
                throw new IllegalArgumentException("PASSED result lacks complete passing evidence");
            }
        } else if (status == DartCandidateAnalysisStatus.REJECTED) {
            if (!blockingDiagnostic && !rejectedSymbol) {
                throw new IllegalArgumentException(
                        "REJECTED result requires a blocking diagnostic or symbol rejection");
            }
        } else if (issue.isEmpty()) {
            throw new IllegalArgumentException(status + " result requires an issue");
        }
    }

    public boolean passed() {
        return status == DartCandidateAnalysisStatus.PASSED;
    }
}
