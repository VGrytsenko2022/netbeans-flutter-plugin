package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
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
