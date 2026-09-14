package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 {@code Scrollbar} constructor projection. */
public final class ScrollbarWidgetPropertySchema {
    public static final WidgetTypeId SCROLLBAR_TYPE =
            new WidgetTypeId("flutter.material.Scrollbar");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 8;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("scrollbarAppearance", "Appearance",
                "Thumb and track visibility for the scrollable child."),
        BEHAVIOR("scrollbarBehavior", "Behavior",
                "Controller, notification filtering and interaction policy."),
        LAYOUT("scrollbarLayout", "Layout",
                "Thumb geometry and placement." );

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description,
            String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("Negative Scrollbar property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private ScrollbarWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "controller", Group.BEHAVIOR, "Controller",
                "Optional ScrollController reference; omission uses the nearest primary controller.", "controller", 1);
        add(values, "thumbVisibility", Group.APPEARANCE, "Thumb visibility",
                "Nullable boolean; null follows ScrollbarThemeData and the SDK default.", "thumbVisibility", 2);
        add(values, "trackVisibility", Group.APPEARANCE, "Track visibility",
                "Nullable boolean; the track is shown only when the thumb is also visible.", "trackVisibility", 3);
        add(values, "thickness", Group.LAYOUT, "Thickness",
                "Nullable non-negative logical-pixel thickness; null preserves platform/theme defaults.", "thickness", 4);
        add(values, "radius", Group.LAYOUT, "Thumb radius",
                "Optional Radius reference; omission preserves platform defaults and explicit null makes the thumb rectangular.", "radius", 5);
        add(values, "notificationPredicate", Group.BEHAVIOR, "Notification predicate",
                "Optional ScrollNotificationPredicate reference; omission uses the default depth-zero predicate.", "notificationPredicate", 6);
        add(values, "interactive", Group.BEHAVIOR, "Interactive",
                "Nullable boolean controlling drag and hover gestures; null follows theme/platform defaults.", "interactive", 7);
        add(values, "scrollbarOrientation", Group.LAYOUT, "Scrollbar orientation",
                "Nullable orientation; left/right are for vertical scrolls and top/bottom for horizontal scrolls.", "scrollbarOrientation", 8);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Scrollbar schema count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, String dartName, int dartOrder) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, dartName, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate Scrollbar property " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
