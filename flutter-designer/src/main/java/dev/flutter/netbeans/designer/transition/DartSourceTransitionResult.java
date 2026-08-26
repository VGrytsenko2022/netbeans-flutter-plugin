package dev.flutter.netbeans.designer.transition;

import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Atomic result of prospective managed-Dart transition planning. */
public final class DartSourceTransitionResult {
    private final DartSourceTransitionStatus status;
    private final DartThreeWayIntegrityResult baseline;
    private final DartSourceIntegrityResult liveSource;
    private final DartGenerationResult generation;
    private final Optional<DartSourceTransitionPlan> plan;
    private final List<DartSourceTransitionDiagnostic> diagnostics;

    DartSourceTransitionResult(
            DartSourceTransitionStatus status,
            DartThreeWayIntegrityResult baseline,
            DartSourceIntegrityResult liveSource,
            DartGenerationResult generation,
            Optional<DartSourceTransitionPlan> plan,
            List<DartSourceTransitionDiagnostic> diagnostics) {
        this.status = Objects.requireNonNull(status, "status");
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.liveSource = Objects.requireNonNull(liveSource, "liveSource");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.diagnostics = List.copyOf(Objects.requireNonNull(
                diagnostics, "diagnostics"));
        if (this.diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("diagnostics contains null");
        }
        if ((status == DartSourceTransitionStatus.READY) != plan.isPresent()) {
            throw new IllegalArgumentException("only READY may publish a transition plan");
        }
        if (status == DartSourceTransitionStatus.NO_CHANGES
                && !this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException("NO_CHANGES cannot contain diagnostics");
        }
        if (status != DartSourceTransitionStatus.READY
                && status != DartSourceTransitionStatus.NO_CHANGES
                && this.diagnostics.isEmpty()) {
            throw new IllegalArgumentException("a failed result must contain a diagnostic");
        }
    }

    public DartSourceTransitionStatus status() {
        return status;
    }

    public DartThreeWayIntegrityResult baseline() {
        return baseline;
    }

    public DartSourceIntegrityResult liveSource() {
        return liveSource;
    }

    public DartGenerationResult generation() {
        return generation;
    }

    public Optional<DartSourceTransitionPlan> plan() {
        return plan;
    }

    public List<DartSourceTransitionDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean ready() {
        return status == DartSourceTransitionStatus.READY;
    }
}
