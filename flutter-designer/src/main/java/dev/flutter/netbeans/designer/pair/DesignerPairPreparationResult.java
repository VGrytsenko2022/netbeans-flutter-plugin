package dev.flutter.netbeans.designer.pair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Atomic result of preparing canonical prospective pair bytes. */
public final class DesignerPairPreparationResult {
    private final DesignerPairPreparationStatus status;
    private final Optional<PreparedDesignerPair> preparedPair;
    private final List<DesignerPairPreparationDiagnostic> diagnostics;

    DesignerPairPreparationResult(
            DesignerPairPreparationStatus status,
            Optional<PreparedDesignerPair> preparedPair,
            List<DesignerPairPreparationDiagnostic> diagnostics) {
        this.status = Objects.requireNonNull(status, "status");
        this.preparedPair = Objects.requireNonNull(preparedPair, "preparedPair");
        this.diagnostics = List.copyOf(Objects.requireNonNull(
                diagnostics, "diagnostics"));
        if (this.diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        if ((status == DesignerPairPreparationStatus.READY)
                != this.preparedPair.isPresent()) {
            throw new IllegalArgumentException(
                    "only READY may publish a prepared pair");
        }
        if (status == DesignerPairPreparationStatus.READY
                && !this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "READY cannot contain diagnostics");
        }
        if (status != DesignerPairPreparationStatus.READY
                && this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "a non-ready result must contain a diagnostic");
        }
    }

    public DesignerPairPreparationStatus status() {
        return status;
    }

    public Optional<PreparedDesignerPair> preparedPair() {
        return preparedPair;
    }

    public List<DesignerPairPreparationDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean ready() {
        return status == DesignerPairPreparationStatus.READY;
    }
}
