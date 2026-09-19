package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Moves one non-root subtree to a destination in the post-removal tree. */
public record MoveWidget(StableId widgetId, WidgetPlacement destination)
        implements DesignerCommand {
    public MoveWidget {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(destination, "destination");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.MOVE_WIDGET;
    }
}
