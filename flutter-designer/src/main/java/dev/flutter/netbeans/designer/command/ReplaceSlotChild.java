package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import java.util.Objects;

/**
 * Atomically replaces the exact current child of one single-valued slot.
 *
 * <p>The expected child is a revision fence: a command planned against an
 * older slot value is rejected instead of replacing a newer child. The
 * replacement is either a fresh immutable subtree or one existing non-root
 * subtree moved from the same document.</p>
 */
public record ReplaceSlotChild(
        StableId ownerId,
        SlotName slotName,
        StableId expectedChildId,
        Replacement replacement) implements DesignerCommand {

    public ReplaceSlotChild {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(slotName, "slotName");
        Objects.requireNonNull(expectedChildId, "expectedChildId");
        Objects.requireNonNull(replacement, "replacement");
    }

    @Override
    public DesignerCommandKind kind() {
        return DesignerCommandKind.REPLACE_SLOT_CHILD;
    }

    /** Closed replacement-source contract for deterministic command handling. */
    public sealed interface Replacement permits NewSubtree, ExistingWidget {
    }

    /** Inserts a fresh subtree whose stable ids are absent from the document. */
    public record NewSubtree(WidgetNode widget) implements Replacement {
        public NewSubtree {
            Objects.requireNonNull(widget, "widget");
        }
    }

    /** Moves one existing non-root subtree into the occupied single slot. */
    public record ExistingWidget(StableId widgetId) implements Replacement {
        public ExistingWidget {
            Objects.requireNonNull(widgetId, "widgetId");
        }
    }
}
