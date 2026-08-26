package dev.flutter.netbeans.designer.source;

/** Stable machine-readable diagnostics produced only by the three-way gate. */
public enum DartThreeWayIntegrityDiagnosticCode {
    SOURCE_EVIDENCE_INCONSISTENT,
    GENERATION_UNAVAILABLE,
    GENERATED_EVIDENCE_INCONSISTENT,
    GENERATED_REGION_HASH_MISMATCH,
    GENERATED_CANDIDATE_INVALID,
    GENERATED_CANDIDATE_UNSUPPORTED,
    GENERATED_CANDIDATE_TOO_LARGE;

    /** Whether this diagnostic means that no complete comparison was possible. */
    public boolean isUnavailable() {
        return switch (this) {
            case SOURCE_EVIDENCE_INCONSISTENT,
                    GENERATION_UNAVAILABLE,
                    GENERATED_EVIDENCE_INCONSISTENT,
                    GENERATED_CANDIDATE_TOO_LARGE -> true;
            case GENERATED_REGION_HASH_MISMATCH,
                    GENERATED_CANDIDATE_INVALID,
                    GENERATED_CANDIDATE_UNSUPPORTED -> false;
        };
    }

    /** Whether the generated candidate uses a source shape the gate cannot prove. */
    public boolean isUnsupported() {
        return this == GENERATED_CANDIDATE_UNSUPPORTED;
    }
}
