package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
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
