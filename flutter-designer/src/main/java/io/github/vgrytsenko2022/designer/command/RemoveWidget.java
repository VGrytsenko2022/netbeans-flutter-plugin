package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Removes a non-root widget subtree when its parent slot remains valid. */
public record RemoveWidget(StableId widgetId) implements DesignerCommand {
    public RemoveWidget {
        Objects.requireNonNull(widgetId, "widgetId");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.REMOVE_WIDGET;
    }
}
