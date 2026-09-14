package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.GestureDetectorWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
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
