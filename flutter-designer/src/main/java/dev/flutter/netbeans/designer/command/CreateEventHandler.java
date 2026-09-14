package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** Creates one user-owned Dart member and binds its event in one analyzed command. */
public record CreateEventHandler(StableId widgetId, PropertyName event, String handlerName)
        implements DesignerCommand {
    public CreateEventHandler {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(event, "event");
        handlerName = new PropertyValue.DartObjectReferenceValue(Optional.empty(), handlerName,
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty()).rootSymbol();
    }

    @Override public DesignerCommandKind kind() {
        return DesignerCommandKind.CREATE_EVENT_HANDLER;
    }
}
