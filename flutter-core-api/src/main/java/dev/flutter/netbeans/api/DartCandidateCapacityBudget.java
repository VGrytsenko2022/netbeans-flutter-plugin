package dev.flutter.netbeans.api;

import java.util.Objects;

/**
 * Shared identity-bearing capacity policy for one generated Dart candidate.
 *
 * <p>The same object instance must flow from generation to the analyzer
 * request and analyzer limits. Value-equal but detached instances are distinct
 * policies and must fail closed at the analysis boundary.</p>
 */
public final class DartCandidateCapacityBudget {
    public static final String DEFAULT_PROFILE_ID =
            "fd-dart-candidate-capacity-v1";
    public static final int DEFAULT_MAX_CANDIDATE_UTF8_BYTES =
            2 * 1024 * 1024;
    public static final int DEFAULT_MAX_SYMBOL_PROBES = 256;
    public static final int DEFAULT_RESERVED_SOURCE_SYMBOL_PROBES = 1;

    public static final DartCandidateCapacityBudget DEFAULT =
            new DartCandidateCapacityBudget(
                    DEFAULT_PROFILE_ID,
                    DEFAULT_MAX_CANDIDATE_UTF8_BYTES,
                    DEFAULT_MAX_SYMBOL_PROBES,
                    DEFAULT_RESERVED_SOURCE_SYMBOL_PROBES);

    private final String profileId;
    private final int maxCandidateUtf8Bytes;
    private final int maxSymbolProbes;
    private final int reservedSourceSymbolProbes;

    public DartCandidateCapacityBudget(
            String profileId,
            int maxCandidateUtf8Bytes,
            int maxSymbolProbes,
            int reservedSourceSymbolProbes) {
        this.profileId = requireProfileId(profileId);
        if (maxCandidateUtf8Bytes < 1) {
            throw new IllegalArgumentException(
                    "maxCandidateUtf8Bytes must be positive");
        }
        if (maxSymbolProbes < 0) {
            throw new IllegalArgumentException(
                    "maxSymbolProbes must not be negative");
        }
        if (reservedSourceSymbolProbes < 0
                || reservedSourceSymbolProbes > maxSymbolProbes) {
            throw new IllegalArgumentException(
                    "reservedSourceSymbolProbes must be between zero and maxSymbolProbes");
        }
        this.maxCandidateUtf8Bytes = maxCandidateUtf8Bytes;
        this.maxSymbolProbes = maxSymbolProbes;
        this.reservedSourceSymbolProbes = reservedSourceSymbolProbes;
    }

    public String profileId() {
        return profileId;
    }

    public int maxCandidateUtf8Bytes() {
        return maxCandidateUtf8Bytes;
    }

    public int maxSymbolProbes() {
        return maxSymbolProbes;
    }

    public int reservedSourceSymbolProbes() {
        return reservedSourceSymbolProbes;
    }

    public int maxGeneratedSymbolOccurrences() {
        return maxSymbolProbes - reservedSourceSymbolProbes;
    }

    private static String requireProfileId(String value) {
        Objects.requireNonNull(value, "profileId");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        return normalized;
    }
}
