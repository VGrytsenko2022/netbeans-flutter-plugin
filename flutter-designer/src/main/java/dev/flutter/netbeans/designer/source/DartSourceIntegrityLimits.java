package dev.flutter.netbeans.designer.source;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;

/** Independent resource limits for bounded Dart source-integrity checks. */
public record DartSourceIntegrityLimits(
        int maxSourceBytes,
        int maxMarkers,
        int maxDiagnostics) {

    /**
     * Keeps allocation amplification from UTF-8 decoding, offset indexing and
     * normalized hashing practical inside the IDE heap.
     */
    public static final int DEFAULT_MAX_SOURCE_BYTES =
            DartCandidateCapacityBudget.DEFAULT_MAX_CANDIDATE_UTF8_BYTES;
    public static final int DEFAULT_MAX_MARKERS = 64;
    public static final int DEFAULT_MAX_DIAGNOSTICS = 32;

    public DartSourceIntegrityLimits {
        positive(maxSourceBytes, "maxSourceBytes");
        positive(maxMarkers, "maxMarkers");
        positive(maxDiagnostics, "maxDiagnostics");
    }

    public static DartSourceIntegrityLimits defaults() {
        return new DartSourceIntegrityLimits(
                DEFAULT_MAX_SOURCE_BYTES,
                DEFAULT_MAX_MARKERS,
                DEFAULT_MAX_DIAGNOSTICS);
    }

    private static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
