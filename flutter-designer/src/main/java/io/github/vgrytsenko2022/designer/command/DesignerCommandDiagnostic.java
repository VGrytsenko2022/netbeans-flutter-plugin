package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** One deterministic command/session rejection with a JSON-pointer-like path. */
public record DesignerCommandDiagnostic(
        DesignerCommandDiagnosticCode code,
        String path,
        Optional<StableId> widgetId,
        String message) {
    public DesignerCommandDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(message, "message");
        if (!path.isEmpty() && !path.startsWith("/")) {
            throw new IllegalArgumentException("path must be empty or start with '/'");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
