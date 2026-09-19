package io.github.vgrytsenko2022.designer.source;

import io.github.vgrytsenko2022.designer.generation.DartGenerationDiagnosticCode;
import io.github.vgrytsenko2022.designer.generation.DartGenerationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable evidence from a bounded on-disk actual/declared/generated check.
 * A match is deliberately not authorization to modify either paired file.
 * Instances are created only by {@link DartThreeWayIntegrityGate} so callers
 * cannot manufacture a matching aggregate from unchecked comparison records.
 */
public final class DartThreeWayIntegrityResult {
    private final DartSourceIntegrityResult source;
    private final DartGenerationResult generation;
    private final List<DartThreeWayRegionComparison> comparisons;
    private final List<DartThreeWayIntegrityDiagnostic> diagnostics;

    DartThreeWayIntegrityResult(
            DartSourceIntegrityResult source,
            DartGenerationResult generation,
            List<DartThreeWayRegionComparison> comparisons,
            List<DartThreeWayIntegrityDiagnostic> diagnostics) {
        this.source = Objects.requireNonNull(source, "source");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.comparisons = List.copyOf(Objects.requireNonNull(
                comparisons, "comparisons"));
        this.diagnostics = List.copyOf(Objects.requireNonNull(
                diagnostics, "diagnostics"));
    }

    public DartSourceIntegrityResult source() {
        return source;
    }

    public DartGenerationResult generation() {
        return generation;
    }

    public List<DartThreeWayRegionComparison> comparisons() {
        return comparisons;
    }

    public List<DartThreeWayIntegrityDiagnostic> diagnostics() {
        return diagnostics;
    }

    public DartThreeWayIntegrityStatus status() {
        if (source.status() == DartSourceIntegrityStatus.UNSUPPORTED
                || generation.diagnostics().stream().anyMatch(diagnostic ->
                    isUnsupportedGeneration(diagnostic.code()))
                || diagnostics.stream().anyMatch(diagnostic ->
                    diagnostic.code().isUnsupported())) {
            return DartThreeWayIntegrityStatus.UNSUPPORTED;
        }
        if (source.status() == DartSourceIntegrityStatus.UNAVAILABLE
                || generation.generated().isEmpty()
                || diagnostics.stream().anyMatch(diagnostic ->
                    diagnostic.code().isUnavailable())) {
            return DartThreeWayIntegrityStatus.UNAVAILABLE;
        }
        if (source.status() == DartSourceIntegrityStatus.CONFLICT
                || !diagnostics.isEmpty()
                || !hasCompleteMatchingEvidence()) {
            return DartThreeWayIntegrityStatus.CONFLICT;
        }
        return DartThreeWayIntegrityStatus.ON_DISK_THREE_WAY_MATCH;
    }

    /** Whether actual, declared and generated normalized hashes agree. Never a write gate. */
    public boolean onDiskThreeWayMatch() {
        return status() == DartThreeWayIntegrityStatus.ON_DISK_THREE_WAY_MATCH;
    }

    /** Exact on-disk source baseline used by this comparison, when available. */
    public Optional<OriginalDartBytes> original() {
        return source.original();
    }

    public Optional<DartThreeWayRegionComparison> comparison(String id) {
        Objects.requireNonNull(id, "id");
        return comparisons.stream().filter(value -> value.id().equals(id)).findFirst();
    }

    private boolean hasCompleteMatchingEvidence() {
        return comparisons.size() == 2
                && comparisons.get(0).id().equals(DartSourceIntegrityScanner.IMPORTS_REGION)
                && comparisons.get(1).id().equals(DartSourceIntegrityScanner.BUILD_REGION)
                && comparisons.stream().allMatch(
                        DartThreeWayRegionComparison::allNormalizedHashesMatch);
    }

    private static boolean isUnsupportedGeneration(DartGenerationDiagnosticCode code) {
        return switch (code) {
            case UNSUPPORTED_WIDGET_KIND, DART_EXPRESSION_UNSUPPORTED -> true;
            case MODEL_INVALID,
                    INVALID_UNICODE,
                    IMPORT_LIMIT,
                    OUTPUT_SIZE_LIMIT,
                    SYMBOL_PROBE_LIMIT,
                    INTERNAL_CATALOG_INCONSISTENCY -> false;
        };
    }
}
