package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.StableId;
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
