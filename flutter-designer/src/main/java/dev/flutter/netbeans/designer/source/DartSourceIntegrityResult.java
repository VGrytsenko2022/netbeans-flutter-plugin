package dev.flutter.netbeans.designer.source;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable result of checking one bounded on-disk Dart source snapshot. */
public record DartSourceIntegrityResult(
        Optional<OriginalDartBytes> original,
        List<DartManagedRegionSnapshot> regions,
        List<DartSourceIntegrityDiagnostic> diagnostics,
        Optional<DartDesignerSuperclassOccurrence> superclassOccurrence) {

    private static final String SOURCE_PATH = "/source/dartFile";

    public DartSourceIntegrityResult {
        original = Objects.requireNonNull(original, "original");
        regions = List.copyOf(Objects.requireNonNull(regions, "regions"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        superclassOccurrence = Objects.requireNonNull(
                superclassOccurrence, "superclassOccurrence");
        if (original.isEmpty() && diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "a result without an exact snapshot must contain a diagnostic");
        }
        if (superclassOccurrence.isPresent()) {
            if (original.isEmpty()
                    || !superclassOccurrence.orElseThrow()
                            .belongsTo(original.orElseThrow())) {
                throw new IllegalArgumentException(
                        "superclass occurrence must belong to the exact original identity");
            }
            if (!diagnostics.isEmpty()) {
                throw new IllegalArgumentException(
                        "superclass occurrence may be published only by a clean scan");
            }
        }
    }

    /** Compatibility constructor for explicit unavailable/forged test evidence. */
    public DartSourceIntegrityResult(
            Optional<OriginalDartBytes> original,
            List<DartManagedRegionSnapshot> regions,
            List<DartSourceIntegrityDiagnostic> diagnostics) {
        this(original, regions, diagnostics, Optional.empty());
    }

    /**
     * Whether the bounded source matches its persisted marker, class and hash
     * metadata. This is not authorization to write: a later gate must also
     * compare the hashes produced from the current visual model/generator.
     */
    public boolean onDiskDeclaredMatch() {
        return status() == DartSourceIntegrityStatus.ON_DISK_DECLARED_MATCH;
    }

    public DartSourceIntegrityStatus status() {
        if (original.isEmpty()) {
            return DartSourceIntegrityStatus.UNAVAILABLE;
        }
        if (diagnostics.isEmpty()) {
            return DartSourceIntegrityStatus.ON_DISK_DECLARED_MATCH;
        }
        boolean unsupported = diagnostics.stream().anyMatch(
                DartSourceIntegrityResult::isUnsupportedDiagnostic);
        return unsupported
                ? DartSourceIntegrityStatus.UNSUPPORTED
                : DartSourceIntegrityStatus.CONFLICT;
    }

    /**
     * Returns a diagnostic that explains the aggregate {@link #status()}.
     * Scanner order is intentionally not used for unsupported results because
     * a preceding hash conflict must not hide the unsupported source shape.
     */
    public Optional<DartSourceIntegrityDiagnostic> primaryDiagnostic() {
        return switch (status()) {
            case ON_DISK_DECLARED_MATCH -> Optional.empty();
            case UNAVAILABLE, CONFLICT -> diagnostics.stream().findFirst();
            case UNSUPPORTED -> diagnostics.stream()
                    .filter(DartSourceIntegrityResult::isUnsupportedDiagnostic)
                    .findFirst();
        };
    }

    public Optional<DartManagedRegionSnapshot> region(String id) {
        Objects.requireNonNull(id, "id");
        return regions.stream().filter(region -> region.id().equals(id)).findFirst();
    }

    public static DartSourceIntegrityResult sourceTooLarge(
            long observedBytes,
            int maximumBytes) {
        if (maximumBytes <= 0 || observedBytes <= maximumBytes) {
            throw new IllegalArgumentException(
                    "observedBytes must exceed the positive maximumBytes");
        }
        return unavailable(DartSourceIntegrityDiagnostic.source(
                DartSourceIntegrityDiagnosticCode.SOURCE_TOO_LARGE,
                SOURCE_PATH,
                "Dart source size " + observedBytes + " bytes exceeds the "
                + maximumBytes + " byte safety limit."));
    }

    public static DartSourceIntegrityResult readFailure(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        return unavailable(DartSourceIntegrityDiagnostic.source(
                DartSourceIntegrityDiagnosticCode.SOURCE_READ_FAILED,
                SOURCE_PATH,
                "The paired Dart source could not be read: " + reason.strip()));
    }

    private static DartSourceIntegrityResult unavailable(
            DartSourceIntegrityDiagnostic diagnostic) {
        return new DartSourceIntegrityResult(
                Optional.empty(), List.of(), List.of(diagnostic), Optional.empty());
    }

    private static boolean isUnsupportedDiagnostic(
            DartSourceIntegrityDiagnostic diagnostic) {
        return diagnostic.code().isUnsupportedSourceShape();
    }
}
