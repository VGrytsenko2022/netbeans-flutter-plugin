package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.WidgetNode;
import java.util.Objects;

/** Inserts an immutable widget subtree at a validated destination. */
public record AddWidget(WidgetPlacement destination, WidgetNode widget)
        implements DesignerCommand {
    public AddWidget {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(widget, "widget");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.ADD_WIDGET;
    }
}
