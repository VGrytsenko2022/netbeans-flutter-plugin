package io.github.vgrytsenko2022.designer.source;

import java.util.Objects;
import java.util.Optional;

/** One concrete problem that prevents trusting the paired Dart source. */
public record DartSourceIntegrityDiagnostic(
        DartSourceIntegrityDiagnosticCode code,
        String path,
        Optional<String> regionId,
        String message) {

    public DartSourceIntegrityDiagnostic {
        Objects.requireNonNull(code, "code");
        path = requireText(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must be an absolute model path");
        }
        regionId = Objects.requireNonNull(regionId, "regionId")
                .map(value -> requireText(value, "regionId"));
        message = requireText(message, "message");
    }

    public static DartSourceIntegrityDiagnostic source(
            DartSourceIntegrityDiagnosticCode code,
            String path,
            String message) {
        return new DartSourceIntegrityDiagnostic(code, path, Optional.empty(), message);
    }

    public static DartSourceIntegrityDiagnostic region(
            DartSourceIntegrityDiagnosticCode code,
            String path,
            String regionId,
            String message) {
        return new DartSourceIntegrityDiagnostic(
                code, path, Optional.of(regionId), message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
