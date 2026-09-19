package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.GestureDetectorWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Explicit optional-child wrapping; ordinary Palette drag/drop remains an Add operation. */
public final class FlutterGestureDetectorWrapPlanner {
    private FlutterGestureDetectorWrapPlanner() { }

    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE, "GestureDetector");
        return new Result(result.command(), result.reason());
    }

    public record Result(Optional<WrapWidget> command, String reason) { }
}
