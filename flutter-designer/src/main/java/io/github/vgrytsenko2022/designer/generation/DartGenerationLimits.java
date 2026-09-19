package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.api.DartCandidateCapacityBudget;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityLimits;
import java.util.Objects;

/** Independent resource limits for one deterministic Dart generation pass. */
public record DartGenerationLimits(
        int maxTotalPayloadUtf8Bytes,
        int maxImports,
        int maxValueCodePoints,
        DartCandidateCapacityBudget candidateCapacityBudget) {

    public static final int DEFAULT_MAX_TOTAL_PAYLOAD_UTF8_BYTES = 1024 * 1024;
    public static final int DEFAULT_MAX_IMPORTS = 1_024;
    public static final int DEFAULT_MAX_VALUE_CODE_POINTS = 65_536;
    public static final int MAX_SUPPORTED_PAYLOAD_UTF8_BYTES =
            DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES - 1;

    public DartGenerationLimits(
            int maxTotalPayloadUtf8Bytes,
            int maxImports,
            int maxValueCodePoints) {
        this(
                maxTotalPayloadUtf8Bytes,
                maxImports,
                maxValueCodePoints,
                DartCandidateCapacityBudget.DEFAULT);
    }

    public DartGenerationLimits {
        positive(maxTotalPayloadUtf8Bytes, "maxTotalPayloadUtf8Bytes");
        positive(maxImports, "maxImports");
        positive(maxValueCodePoints, "maxValueCodePoints");
        Objects.requireNonNull(candidateCapacityBudget,
                "candidateCapacityBudget");
        if (maxTotalPayloadUtf8Bytes > MAX_SUPPORTED_PAYLOAD_UTF8_BYTES) {
            throw new IllegalArgumentException(
                    "maxTotalPayloadUtf8Bytes must remain below the Dart source-integrity "
                    + "limit of " + DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES);
        }
        if (maxTotalPayloadUtf8Bytes
                >= candidateCapacityBudget.maxCandidateUtf8Bytes()) {
            throw new IllegalArgumentException(
                    "generated payload limit must remain below the shared Dart candidate limit");
        }
        if (candidateCapacityBudget.maxGeneratedSymbolOccurrences() < 3) {
            throw new IllegalArgumentException(
                    "candidate capacity must reserve room for Widget, BuildContext "
                    + "and the root widget constructor occurrences");
        }
        if (candidateCapacityBudget.reservedSourceSymbolProbes() != 1) {
            throw new IllegalArgumentException(
                    "generation profile requires exactly one scanner-owned source symbol probe");
        }
    }

    public static DartGenerationLimits defaults() {
        return new DartGenerationLimits(
                DEFAULT_MAX_TOTAL_PAYLOAD_UTF8_BYTES,
                DEFAULT_MAX_IMPORTS,
                DEFAULT_MAX_VALUE_CODE_POINTS,
                DartCandidateCapacityBudget.DEFAULT);
    }

    private static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
