package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import java.util.Objects;

/**
 * Replaces a target with a wrapper subtree and inserts the target into one
 * wrapper slot. Root wrapping is supported.
 */
public record WrapWidget(
        StableId widgetId,
        WidgetNode wrapper,
        SlotName wrapperSlot,
        int wrapperIndex) implements DesignerCommand {
    public WrapWidget {
        Objects.requireNonNull(widgetId, "widgetId");
        Objects.requireNonNull(wrapper, "wrapper");
        Objects.requireNonNull(wrapperSlot, "wrapperSlot");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.WRAP_WIDGET;
    }
}
