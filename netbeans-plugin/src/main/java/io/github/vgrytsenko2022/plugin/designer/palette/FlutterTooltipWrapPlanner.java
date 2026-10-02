package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.TooltipWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Explicitly wraps an exact selected widget; ordinary Tooltip creation still has an optional child. */
public final class FlutterTooltipWrapPlanner {
    private FlutterTooltipWrapPlanner() { }
    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                TooltipWidgetPropertySchema.TOOLTIP_TYPE, "Tooltip");
        return new Result(result.command(), result.reason());
    }
    public record Result(Optional<WrapWidget> command, String reason) { }
}
