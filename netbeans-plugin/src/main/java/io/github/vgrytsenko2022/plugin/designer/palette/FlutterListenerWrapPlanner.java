package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.ListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Explicit Listener wrapping, separate from ordinary palette Add. */
public final class FlutterListenerWrapPlanner {
    private FlutterListenerWrapPlanner() { }
    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                ListenerWidgetPropertySchema.LISTENER_TYPE, "Listener");
        return new Result(result.command(), result.reason());
    }
    public record Result(Optional<WrapWidget> command, String reason) { }
}
