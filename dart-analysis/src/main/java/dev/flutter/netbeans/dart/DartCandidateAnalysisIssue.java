package dev.flutter.netbeans.dart;

import java.util.Objects;

/** Concrete reason a candidate could not obtain passing analyzer evidence. */
public record DartCandidateAnalysisIssue(
        DartCandidateAnalysisIssueCode code,
        String message) {

    public DartCandidateAnalysisIssue {
        Objects.requireNonNull(code, "code");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        message = message.strip();
    }
}
