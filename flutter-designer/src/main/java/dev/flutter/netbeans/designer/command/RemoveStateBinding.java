package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;

/** Disconnects a widget state binding while retaining every user-owned Dart member. */
public record RemoveStateBinding(StableId widgetId) implements DesignerCommand {
    public RemoveStateBinding { Objects.requireNonNull(widgetId, "widgetId"); }
    @Override public DesignerCommandKind kind() { return DesignerCommandKind.REMOVE_STATE_BINDING; }
}
