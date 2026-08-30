package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.HashSet;
import java.util.List;
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
        FlutterWidgetSlotMutation.Remove,
        FlutterWidgetSlotMutation.Replace,
        FlutterWidgetSlotMutation.ClearAll {

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

    /** Atomically replaces the exact current child of one occupied single slot. */
    record Replace(
            StableId ownerId,
            SlotName slotName,
            StableId expectedChildId,
            Replacement replacement) implements FlutterWidgetSlotMutation {
        public Replace {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(expectedChildId, "expectedChildId");
            Objects.requireNonNull(replacement, "replacement");
        }

        /** Closed source choice retained by the revision-bound UI bridge. */
        public sealed interface Replacement permits NewWidget, ExistingWidget {
        }

        /** Requests a fresh catalog-canonical widget prototype. */
        public record NewWidget(WidgetTypeId widgetType) implements Replacement {
            public NewWidget {
                Objects.requireNonNull(widgetType, "widgetType");
            }
        }

        /** Requests one existing non-root subtree from the bound revision. */
        public record ExistingWidget(StableId sourceId) implements Replacement {
            public ExistingWidget {
                Objects.requireNonNull(sourceId, "sourceId");
            }
        }
    }

    /** Clears the exact ordered contents of one list slot as one mutation. */
    record ClearAll(
            StableId ownerId,
            SlotName slotName,
            List<StableId> expectedChildIds) implements FlutterWidgetSlotMutation {
        public ClearAll {
            Objects.requireNonNull(ownerId, "ownerId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(expectedChildIds, "expectedChildIds");
            expectedChildIds = List.copyOf(expectedChildIds);
            if (expectedChildIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Clear All requires at least one expected direct child.");
            }
            if (expectedChildIds.stream().anyMatch(Objects::isNull)) {
                throw new NullPointerException("expectedChildIds contains null");
            }
            if (new HashSet<>(expectedChildIds).size()
                    != expectedChildIds.size()) {
                throw new IllegalArgumentException(
                        "Expected direct-child ids must be unique.");
            }
        }
    }
}
