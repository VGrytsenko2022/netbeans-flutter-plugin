package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;

/** Strictly decoded event from the isolated Flutter Canvas runner. */
public sealed interface CanvasRunnerRuntimeEvent permits
        CanvasRunnerRuntimeEvent.Presented,
        CanvasRunnerRuntimeEvent.Selection,
        CanvasRunnerRuntimeEvent.PaletteDrop {

    record Presented(CanvasLayoutKey layoutKey) implements CanvasRunnerRuntimeEvent {
        public Presented {
            Objects.requireNonNull(layoutKey, "layoutKey");
        }
    }

    record Selection(
            CanvasIntentKey intentKey,
            StableId widgetId) implements CanvasRunnerRuntimeEvent {
        public Selection {
            Objects.requireNonNull(intentKey, "intentKey");
            Objects.requireNonNull(widgetId, "widgetId");
        }
    }

    /** One exact, one-shot request to append a Palette widget to a list slot. */
    record PaletteDrop(
            CanvasIntentKey intentKey,
            String token,
            StableId parentWidgetId,
            SlotName slotName,
            int insertionIndex) implements CanvasRunnerRuntimeEvent {
        public PaletteDrop {
            Objects.requireNonNull(intentKey, "intentKey");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(parentWidgetId, "parentWidgetId");
            Objects.requireNonNull(slotName, "slotName");
            if (token.isBlank()) {
                throw new IllegalArgumentException("token must not be blank");
            }
            if (insertionIndex < 0) {
                throw new IllegalArgumentException(
                        "insertionIndex must not be negative");
            }
        }
    }
}
