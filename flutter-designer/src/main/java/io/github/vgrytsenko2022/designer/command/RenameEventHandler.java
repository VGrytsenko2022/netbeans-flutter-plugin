package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;
import java.util.Optional;

/** Renames the selected local method and every exact local model binding atomically. */
public record RenameEventHandler(StableId widgetId, PropertyName event, String newName)
        implements DesignerCommand {
    public RenameEventHandler {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(event, "event");
        newName = new PropertyValue.DartObjectReferenceValue(Optional.empty(), newName,
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty()).rootSymbol();
    }

    @Override public DesignerCommandKind kind() {
        return DesignerCommandKind.RENAME_EVENT_HANDLER;
    }
}
