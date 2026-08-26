package dev.flutter.netbeans.plugin.designer;

import java.util.Objects;

/** One stable fail-closed reason why an exact Designer pair is not save-ready. */
record PairSaveEvidenceDiagnostic(
        Code code,
        String subject,
        String message) {

    PairSaveEvidenceDiagnostic {
        Objects.requireNonNull(code, "code");
        subject = requireText(subject, "subject");
        message = requireText(message, "message");
    }

    enum Code {
        CURRENT_STATE_NOT_WRITABLE,
        LOADED_FD_BASELINE_IDENTITY_MISMATCH,
        FD_BASELINE_MISMATCH,
        DART_BASELINE_EVIDENCE_IDENTITY_MISMATCH,
        DART_BASELINE_MISMATCH,
        LIVE_CANDIDATE_MISMATCH,
        ANALYSIS_TICKET_ALREADY_USED,
        ANALYSIS_REQUEST_MISMATCH,
        ANALYSIS_NOT_PASSED,
        ANALYSIS_CANDIDATE_MISMATCH,
        ANALYSIS_VERSION_MISMATCH,
        ANALYSIS_DART_PATH_MISMATCH,
        ZERO_SYMBOL_PROBES,
        INCOMPLETE_SYMBOL_EVIDENCE,
        GENERATED_SYMBOL_PROBE_SET_MISMATCH,
        DUPLICATE_SYMBOL_PROBE,
        INVALID_SYMBOL_OCCURRENCE,
        REQUIRED_STATELESS_WIDGET_PROBE_MISSING,
        REQUIRED_WIDGET_PROBE_MISSING,
        REQUIRED_BUILD_CONTEXT_PROBE_MISSING,
        INVALID_FLUTTER_LIBRARY_URI,
        UNTRUSTED_PROBE_ROOT,
        UNTRUSTED_NAVIGATION_TARGET,
        PATH_VALIDATION_FAILED
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
