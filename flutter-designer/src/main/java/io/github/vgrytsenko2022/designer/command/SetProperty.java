package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Sets one catalog-declared property to an accepted immutable value. */
public record SetProperty(
        StableId widgetId,
        PropertyName propertyName,
        PropertyValue value) implements DesignerCommand {
    public SetProperty {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(value, "value");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.SET_PROPERTY;
    }
}
