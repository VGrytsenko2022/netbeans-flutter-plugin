package io.github.vgrytsenko2022.designer.copy;

import java.util.Objects;

/** Closed result of preparing a Flutter Designer pair copy. */
public sealed interface DesignerPairCopyResult permits
        DesignerPairCopyResult.Ready,
        DesignerPairCopyResult.Rejected {

    record Ready(DesignerPairCopyPlan plan) implements DesignerPairCopyResult {
        public Ready {
            Objects.requireNonNull(plan, "plan");
        }
    }

    record Rejected(Code code, String pointer, String message)
            implements DesignerPairCopyResult {
        public Rejected {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(pointer, "pointer");
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }

    enum Code {
        INVALID_ORIGINAL_FILENAME,
        INVALID_TARGET_FILENAME,
        MODEL_NOT_CURRENT,
        MIGRATION_REQUIRED,
        SOURCE_REFERENCE_MISMATCH,
        SAME_DOCUMENT_ID,
        ENCODE_FAILED,
        ROUND_TRIP_MISMATCH
    }
}
