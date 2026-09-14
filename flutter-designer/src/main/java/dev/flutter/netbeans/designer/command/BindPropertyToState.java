package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import java.util.Objects;

/** Binds one reviewed scalar property without changing its literal Canvas preview. */
public record BindPropertyToState(StableId widgetId, PropertyName propertyName, StatePropertyBinding binding)
        implements DesignerCommand {
    public BindPropertyToState {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(binding, "binding");
    }
    @Override public DesignerCommandKind kind() { return DesignerCommandKind.BIND_PROPERTY_TO_STATE; }
}
