package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.NotificationListenerWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
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
