package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;

/** Restores a property's literal runtime argument while retaining all user source. */
public record RemovePropertyStateBinding(StableId widgetId, PropertyName propertyName) implements DesignerCommand {
    public RemovePropertyStateBinding {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(propertyName, "propertyName");
    }
    @Override public DesignerCommandKind kind() { return DesignerCommandKind.REMOVE_PROPERTY_STATE_BINDING; }
}
