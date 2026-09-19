package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 NotificationListener constructor and bounded generic type selection. */
public final class NotificationListenerWidgetPropertySchema {
    public static final WidgetTypeId NOTIFICATION_LISTENER_TYPE = new WidgetTypeId("flutter.widgets.NotificationListener");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 2;
    public static final int CALLBACK_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;
    public static final String CALLBACK_TYPE = "NotificationListenerCallback<Notification>";
    private static final List<String> TYPE_PRESETS = List.of("Notification", "LayoutChangedNotification", "ScrollNotification",
            "ScrollStartNotification", "ScrollUpdateNotification", "OverscrollNotification", "ScrollEndNotification",
            "UserScrollNotification", "SizeChangedLayoutNotification", "ScrollMetricsNotification",
            "OverscrollIndicatorNotification", "DraggableScrollableNotification", "KeepAliveNotification", "NavigationNotification");

    public enum Group {
        BEHAVIOR;
        public String id() { return "notificationListenerBehavior"; }
        public String setName() { return id(); }
        public String displayName() { return "Behavior"; }
        public String description() { return "Notification subtype filtering and upward propagation."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(dartName);
            if (dartOrder < 1) throw new IllegalArgumentException("NotificationListener property order follows required child");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private NotificationListenerWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> notificationTypes() { return TYPE_PRESETS; }
    public static List<String> typePresets() { return TYPE_PRESETS; }
    public static boolean isTypeReference(PropertyValue value) {
        return value instanceof PropertyValue.DartObjectReferenceValue reference
                && reference.access() == PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                && reference.member().isEmpty() && reference.constant().isEmpty();
    }
    public static Optional<String> notificationTypeError(WidgetNode node) {
        PropertyValue value = node.properties().get(new PropertyName("notificationType"));
        if (value instanceof PropertyValue.DartObjectReferenceValue && !isTypeReference(value)) {
            return Optional.of("Notification Type requires a simple non-nullable class or typedef reference: no member, invocation, generic spelling or constness. Its Notification subtype bound is verified by the Dart analyzer.");
        }
        return Optional.empty();
    }
    private static Map<String, Definition> createDefinitions() {
        var result = new LinkedHashMap<String, Definition>();
        result.put("notificationType", new Definition(Group.BEHAVIOR, "Notification type",
                "Required generic subtype T, created Notification. Choose a reviewed Flutter preset or simple project class/typedef reference. Strict analyzer proof requires T extends Notification even when no callback is bound. Changing T preserves the callback and rechecks its compatibility.", "notificationType", 1));
        result.put("onNotification", new Definition(Group.BEHAVIOR, "On notification",
                "Nullable bool callback receiving the selected Notification subtype. Return true to stop bubbling; false, omission or null continues propagation. Created handlers accept Notification so they can be shared across subtypes; user-owned code may narrow the parameter. Layout notifications can arrive during layout: do not synchronously call setState then. Project callbacks never run in Canvas.", "onNotification", 2));
        return Collections.unmodifiableMap(result);
    }
}
