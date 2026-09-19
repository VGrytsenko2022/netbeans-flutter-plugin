package io.github.vgrytsenko2022.designer.catalog;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable presentation metadata; localization remains a NetBeans-edge concern. */
public record PaletteMetadata(
        String categoryId,
        int categoryOrder,
        int itemOrder,
        String displayName) {

    private static final Pattern CATEGORY_ID = Pattern.compile("^[a-z][a-z0-9.-]{0,127}$");

    public PaletteMetadata {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(displayName, "displayName");
        if (!CATEGORY_ID.matcher(categoryId).matches()) {
            throw new IllegalArgumentException("Invalid palette category id: " + categoryId);
        }
        if (categoryOrder < 0 || itemOrder < 0) {
            throw new IllegalArgumentException("Palette order must not be negative");
        }
        if (displayName.isBlank() || displayName.length() > 120) {
            throw new IllegalArgumentException("Palette display name must contain 1 to 120 visible characters");
        }
    }
}
