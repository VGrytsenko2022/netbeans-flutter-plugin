package io.github.vgrytsenko2022.designer.rename;

import java.util.Objects;

/** Closed result of preparing a metadata-only Flutter Designer pair rename. */
public sealed interface DesignerPairRenameResult permits
        DesignerPairRenameResult.Ready,
        DesignerPairRenameResult.Rejected {

    record Ready(DesignerPairRenamePlan plan)
            implements DesignerPairRenameResult {
        public Ready {
            Objects.requireNonNull(plan, "plan");
        }
    }

    record Rejected(Code code, String pointer, String message)
            implements DesignerPairRenameResult {
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
        SAME_FILENAME,
        MODEL_NOT_CURRENT,
        MIGRATION_REQUIRED,
        SOURCE_REFERENCE_MISMATCH,
        ENCODE_FAILED,
        ROUND_TRIP_MISMATCH
    }
}
