package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.designer.canvas.CanvasIntentKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.model.StableId;
import java.util.Objects;

/** Strictly decoded read-only event from the isolated Flutter Canvas runner. */
public sealed interface CanvasRunnerRuntimeEvent permits
        CanvasRunnerRuntimeEvent.Presented,
        CanvasRunnerRuntimeEvent.Selection {

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
}
