package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.PaletteMetadata;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Objects;

/**
 * Immutable, NetBeans-facing description of one item in the Flutter designer
 * palette. The complete immutable {@link WidgetDefinition} is exposed beside
 * this value in the palette item lookup.
 */
public record FlutterDesignerPaletteItem(
        WidgetTypeId typeId,
        String categoryId,
        int categoryOrder,
        int itemOrder,
        String displayName) {

    public FlutterDesignerPaletteItem {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(displayName, "displayName");
        if (categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (categoryOrder < 0 || itemOrder < 0) {
            throw new IllegalArgumentException("Palette order must not be negative");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }

    static FlutterDesignerPaletteItem from(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        PaletteMetadata palette = definition.palette();
        return new FlutterDesignerPaletteItem(
                definition.typeId(),
                palette.categoryId(),
                palette.categoryOrder(),
                palette.itemOrder(),
                palette.displayName());
    }
}
