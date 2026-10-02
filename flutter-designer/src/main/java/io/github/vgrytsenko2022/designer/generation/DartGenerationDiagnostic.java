package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** One deterministic model-addressed generation failure. */
public record DartGenerationDiagnostic(
        DartGenerationDiagnosticCode code,
        String path,
        Optional<StableId> widgetId,
        Optional<DartManagedRegionId> region,
        String message) {

    public DartGenerationDiagnostic {
        Objects.requireNonNull(code, "code");
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("Generation diagnostic path must be absolute");
        }
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(region, "region");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Generation diagnostic message must not be blank");
        }
    }
}
