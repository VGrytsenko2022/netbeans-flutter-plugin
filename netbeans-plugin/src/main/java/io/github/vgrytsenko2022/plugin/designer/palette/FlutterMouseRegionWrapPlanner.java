package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.MouseRegionWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Explicit MouseRegion wrapping, separate from ordinary palette Add. */
public final class FlutterMouseRegionWrapPlanner {
    private FlutterMouseRegionWrapPlanner() { }
    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE, "MouseRegion");
        return new Result(result.command(), result.reason());
    }
    public record Result(Optional<WrapWidget> command, String reason) { }
}
