package io.github.vgrytsenko2022.designer.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Stable machine-readable catalog composition failure. */
public record CatalogDiagnostic(
        CatalogDiagnosticCode code,
        String subject,
        List<String> contributors,
        String message) {

    public CatalogDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(contributors, "contributors");
        Objects.requireNonNull(message, "message");
        ArrayList<String> ordered = new ArrayList<>(contributors);
        if (ordered.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("contributors contains null");
        }
        ordered.sort(String::compareTo);
        contributors = List.copyOf(ordered);
    }
}
