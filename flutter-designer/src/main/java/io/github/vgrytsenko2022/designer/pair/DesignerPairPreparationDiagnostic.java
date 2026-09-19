package io.github.vgrytsenko2022.designer.pair;

import java.util.Objects;

/** One bounded reason why a prospective pair could not be prepared. */
public record DesignerPairPreparationDiagnostic(
        DesignerPairPreparationDiagnosticCode code,
        String path,
        String message) {

    public DesignerPairPreparationDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(message, "message");
        if (!path.isEmpty() && !path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "path must be empty or an absolute model path");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
