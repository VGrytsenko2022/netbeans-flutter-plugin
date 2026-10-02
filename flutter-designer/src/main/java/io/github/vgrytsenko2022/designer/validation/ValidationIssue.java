package io.github.vgrytsenko2022.designer.validation;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** One deterministic, model-addressed designer validation issue. */
public record ValidationIssue(
        String code,
        ValidationSeverity severity,
        String path,
        Optional<StableId> widgetId,
        String message) {

    public ValidationIssue {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Validation issue code must not be blank.");
        }
        Objects.requireNonNull(severity, "severity");
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Validation issue path must be an absolute model path.");
        }
        widgetId = Objects.requireNonNull(widgetId, "widgetId");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Validation issue message must not be blank.");
        }
    }
}
