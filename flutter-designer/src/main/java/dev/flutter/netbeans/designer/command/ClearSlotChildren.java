package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Atomically clears the exact ordered contents of one list-valued slot.
 *
 * <p>{@code expectedChildIds} is a revision fence. All direct children are
 * removed as one semantic command and therefore become one Undo/Redo edit.</p>
 */
public record ClearSlotChildren(
        StableId ownerId,
        SlotName slotName,
        List<StableId> expectedChildIds) implements DesignerCommand {

    public ClearSlotChildren {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(slotName, "slotName");
        Objects.requireNonNull(expectedChildIds, "expectedChildIds");
        expectedChildIds = List.copyOf(expectedChildIds);
        if (expectedChildIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Clear Slot Children requires at least one expected child");
        }
        if (expectedChildIds.size() > WidgetSlot.MAX_LIST_CHILDREN) {
            throw new IllegalArgumentException(
                    "Expected child count exceeds the model list-slot limit");
        }
        if (expectedChildIds.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("expectedChildIds contains null");
        }
        if (new HashSet<>(expectedChildIds).size() != expectedChildIds.size()) {
            throw new IllegalArgumentException(
                    "Expected list-slot child ids must be unique");
        }
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.CLEAR_SLOT_CHILDREN;
    }
}
