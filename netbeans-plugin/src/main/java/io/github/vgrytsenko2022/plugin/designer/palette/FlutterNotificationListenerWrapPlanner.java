package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.NotificationListenerWidgetPropertySchema;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.command.WrapWidget;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Optional;
import java.util.function.Supplier;

/** Wraps an exact existing child in NotificationListener<Notification>, without a generated handler. */
public final class FlutterNotificationListenerWrapPlanner {
    private FlutterNotificationListenerWrapPlanner() { }
    public static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId, Supplier<StableId> ids) {
        var result = FlutterInteractionWrapPlanner.plan(document, catalog, targetId, ids,
                NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE, "NotificationListener");
        return new Result(result.command(), result.reason());
    }
    public record Result(Optional<WrapWidget> command, String reason) { }
}
