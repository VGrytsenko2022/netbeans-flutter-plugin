package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable atomic result: both managed regions are present or neither is. */
public record DartGenerationResult(
        ValidationResult modelValidation,
        Optional<GeneratedDartRegions> generated,
        List<DartGenerationDiagnostic> diagnostics) {

    public DartGenerationResult {
        Objects.requireNonNull(modelValidation, "modelValidation");
        Objects.requireNonNull(generated, "generated");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        if (generated.isPresent() != (modelValidation.valid() && diagnostics.isEmpty())) {
            throw new IllegalArgumentException(
                    "Generated regions must be present exactly for a valid diagnostic-free result");
        }
    }

    public boolean successful() {
        return generated.isPresent();
    }
}
