package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.FocusWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Creates Standard Focus around an existing required child; never invents a project FocusNode. */
public final class FlutterFocusWrapPlanner {
    private FlutterFocusWrapPlanner() { }
    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                FocusWidgetPropertySchema.FOCUS_TYPE, "Focus");
        return new Result(result.command(), result.reason());
    }
    public record Result(Optional<WrapWidget> command, String reason) { }
}
