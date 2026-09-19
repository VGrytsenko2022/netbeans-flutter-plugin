package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Removes one optional catalog-declared property from a widget. */
public record ResetProperty(StableId widgetId, PropertyName propertyName)
        implements DesignerCommand {
    public ResetProperty {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(propertyName, "propertyName");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.RESET_PROPERTY;
    }
}
