package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Objects;

/**
 * Revision-bound structural intent emitted by one named-slot editor.
 *
 * <p>The Properties UI never constructs authoritative designer commands.
 * The active Design view replans this intent against the exact document and
 * catalog revision that owns the selected node.</p>
 */
public sealed interface FlutterWidgetSlotMutation permits
        FlutterWidgetSlotMutation.Add,
        FlutterWidgetSlotMutation.Move,
        FlutterWidgetSlotMutation.Remove {

    StableId ownerId();

    SlotName slotName();

    /** Adds one catalog-backed widget at an exact slot index. */
    record Add(
            StableId ownerId,
            SlotName slotName,
            WidgetTypeId widgetType,
            int index) implements FlutterWidgetSlotMutation {
        public Add {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(widgetType, "widgetType");
            if (index < 0) {
                throw new IllegalArgumentException("Slot insertion index must be non-negative.");
            }
        }
    }

    /** Moves one existing subtree to an exact post-removal slot index. */
    record Move(
            StableId ownerId,
            SlotName slotName,
            StableId sourceId,
            int postRemovalIndex) implements FlutterWidgetSlotMutation {
        public Move {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(sourceId, "sourceId");
            if (postRemovalIndex < 0) {
                throw new IllegalArgumentException(
                        "Post-removal slot index must be non-negative.");
            }
        }
    }

    /** Removes one exact direct child and its complete subtree. */
    record Remove(
            StableId ownerId,
            SlotName slotName,
            StableId childId) implements FlutterWidgetSlotMutation {
        public Remove {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(childId, "childId");
        }
    }
}
