package io.github.vgrytsenko2022.designer.catalog;

import java.util.List;
import java.util.Objects;

public record CatalogBuildResult(WidgetCatalog catalog, List<CatalogDiagnostic> diagnostics) {
    public CatalogBuildResult {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(diagnostics, "diagnostics");
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }
}
