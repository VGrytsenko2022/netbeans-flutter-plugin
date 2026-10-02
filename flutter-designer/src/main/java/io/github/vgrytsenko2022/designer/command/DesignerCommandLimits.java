package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.generation.DartGenerationLimits;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityLimits;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.Objects;

/** Independent bounds for one immutable command session and its history. */
public record DesignerCommandLimits(
        FdCodecLimits fdCodecLimits,
        DartGenerationLimits generationLimits,
        DartSourceIntegrityLimits sourceLimits,
        ValidationLimits validationLimits,
        int maxHistoryEdits,
        long maxRetainedPairBytes) {

    public static final int DEFAULT_MAX_HISTORY_EDITS = 256;
    public static final long DEFAULT_MAX_RETAINED_PAIR_BYTES = 64L * 1024 * 1024;
    public static final int MAX_SUPPORTED_HISTORY_EDITS = 4_096;

    public DesignerCommandLimits {
        Objects.requireNonNull(fdCodecLimits, "fdCodecLimits");
        Objects.requireNonNull(generationLimits, "generationLimits");
        Objects.requireNonNull(sourceLimits, "sourceLimits");
        Objects.requireNonNull(validationLimits, "validationLimits");
        if (maxHistoryEdits < 1 || maxHistoryEdits > MAX_SUPPORTED_HISTORY_EDITS) {
            throw new IllegalArgumentException(
                    "maxHistoryEdits must be between 1 and "
                    + MAX_SUPPORTED_HISTORY_EDITS);
        }
        if (maxRetainedPairBytes < 1) {
            throw new IllegalArgumentException("maxRetainedPairBytes must be positive");
        }
        if (generationLimits.maxTotalPayloadUtf8Bytes()
                >= sourceLimits.maxSourceBytes()) {
            throw new IllegalArgumentException(
                    "generated payload limit must remain below source byte limit");
        }
        if (sourceLimits.maxSourceBytes()
                > generationLimits.candidateCapacityBudget()
                        .maxCandidateUtf8Bytes()) {
            throw new IllegalArgumentException(
                    "source byte limit must not exceed the shared Dart candidate limit");
        }
    }

    public static DesignerCommandLimits defaults() {
        return new DesignerCommandLimits(
                FdCodecLimits.defaults(),
                DartGenerationLimits.defaults(),
                DartSourceIntegrityLimits.defaults(),
                ValidationLimits.defaults(),
                DEFAULT_MAX_HISTORY_EDITS,
                DEFAULT_MAX_RETAINED_PAIR_BYTES);
    }
}
