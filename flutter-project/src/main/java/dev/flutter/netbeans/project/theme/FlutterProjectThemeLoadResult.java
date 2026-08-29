package dev.flutter.netbeans.project.theme;

import java.util.Objects;
import java.util.Optional;

/** Immutable non-throwing project-theme load result for editor and Canvas consumers. */
public record FlutterProjectThemeLoadResult(
        FlutterProjectThemeLoadStatus status,
        Optional<FlutterProjectTheme> theme,
        String detail) {

    public FlutterProjectThemeLoadResult {
        status = Objects.requireNonNull(status, "status");
        theme = Objects.requireNonNull(theme, "theme");
        detail = Objects.requireNonNull(detail, "detail");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("Project theme load detail must not be blank");
        }
        boolean parsedThemeExpected = status == FlutterProjectThemeLoadStatus.VALID
                || status == FlutterProjectThemeLoadStatus.GENERATED_DART_MISSING
                || status == FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH;
        if (theme.isPresent() != parsedThemeExpected) {
            throw new IllegalArgumentException(
                    "Project theme presence is inconsistent with load status " + status);
        }
    }

    public boolean valid() {
        return status == FlutterProjectThemeLoadStatus.VALID;
    }
}
