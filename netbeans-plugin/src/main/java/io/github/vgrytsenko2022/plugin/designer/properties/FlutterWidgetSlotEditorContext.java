package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable catalog/document authority exposed to a named-slot editor. */
public record FlutterWidgetSlotEditorContext(
        DesignerDocument document,
        WidgetCatalog catalog,
        List<WidgetTypeId> insertableWidgetTypes) {

    public FlutterWidgetSlotEditorContext {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(insertableWidgetTypes, "insertableWidgetTypes");
        Set<WidgetTypeId> unique = new LinkedHashSet<>();
        for (WidgetTypeId type : insertableWidgetTypes) {
            Objects.requireNonNull(type, "insertableWidgetTypes contains null");
            if (catalog.find(type).isEmpty()) {
                throw new IllegalArgumentException(
                        "Insertable widget type is absent from the bound catalog: "
                        + type.value());
            }
            if (!unique.add(type)) {
                throw new IllegalArgumentException(
                        "Insertable widget types must be unique: " + type.value());
            }
        }
        insertableWidgetTypes = List.copyOf(unique);
    }
}
