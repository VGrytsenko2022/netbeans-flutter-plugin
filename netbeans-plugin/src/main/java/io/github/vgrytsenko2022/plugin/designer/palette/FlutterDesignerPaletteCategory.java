package io.github.vgrytsenko2022.plugin.designer.palette;

import java.util.Objects;

/** Immutable identity and presentation metadata for a palette category. */
public record FlutterDesignerPaletteCategory(
        String categoryId,
        int categoryOrder,
        String displayName) {

    public FlutterDesignerPaletteCategory {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(displayName, "displayName");
        if (categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (categoryOrder < 0) {
            throw new IllegalArgumentException("categoryOrder must not be negative");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
