package io.github.vgrytsenko2022.designer.transition;

import java.util.Objects;
import java.util.Optional;

/** One bounded, user-facing reason why a transition plan is unavailable. */
public record DartSourceTransitionDiagnostic(
        DartSourceTransitionDiagnosticCode code,
        String path,
        Optional<String> regionId,
        String message) {

    public DartSourceTransitionDiagnostic {
        Objects.requireNonNull(code, "code");
        path = requireText(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must be an absolute model path");
        }
        regionId = Objects.requireNonNull(regionId, "regionId");
        regionId = regionId.map(value -> requireText(value, "regionId"));
        message = requireText(message, "message");
    }

    public static DartSourceTransitionDiagnostic source(
            DartSourceTransitionDiagnosticCode code,
            String path,
            String message) {
        return new DartSourceTransitionDiagnostic(
                code, path, Optional.empty(), message);
    }

    public static DartSourceTransitionDiagnostic region(
            DartSourceTransitionDiagnosticCode code,
            String path,
            String regionId,
            String message) {
        return new DartSourceTransitionDiagnostic(
                code, path, Optional.of(regionId), message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
