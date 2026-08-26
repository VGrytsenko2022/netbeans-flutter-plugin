package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;

/**
 * Destination inside a parent slot.
 *
 * <p>A single-valued slot accepts only index {@code 0}. A list slot accepts
 * insertion indexes from zero through its current child count. For a move,
 * the index addresses the destination after the moved subtree has been
 * removed.</p>
 */
public record WidgetPlacement(StableId parentId, SlotName slotName, int index) {
    public WidgetPlacement {
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(slotName, "slotName");
    }
}
