package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code SafeArea} in Flutter 3.44.8.
 *
 * <p>The common framework {@code key} is deliberately excluded. The six
 * optional constructor properties preserve their Flutter defaults when
 * omitted, while the required child is assembled atomically by the generic
 * wrapper creation workflow.</p>
 */
public final class SafeAreaWidgetPropertySchema {
    public static final WidgetTypeId SAFE_AREA_TYPE =
            new WidgetTypeId("flutter.widgets.SafeArea");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 6;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        SIDES("safeAreaSides", "Safe sides",
                "Screen edges whose system intrusions are avoided."),
        PADDING("safeAreaPadding", "Minimum padding",
                "Physical minimum insets applied together with MediaQuery padding."),
        VIEW_PADDING("safeAreaViewPadding", "View padding",
                "Bottom view-padding behavior while the on-screen keyboard is visible.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return setName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    /** Metadata for one exact public {@code SafeArea} constructor property. */
    public record Definition(
            Group group,
            String displayName,
            String description,
            String dartName,
            int dartOrder) {

        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("dartOrder must be non-negative");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private SafeAreaWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the six reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "left", Group.SIDES, "Left",
                "Avoid left-side system intrusions; omission preserves true.", 0);
        add(values, "top", Group.SIDES, "Top",
                "Avoid top-side system intrusions; omission preserves true.", 1);
        add(values, "right", Group.SIDES, "Right",
                "Avoid right-side system intrusions; omission preserves true.", 2);
        add(values, "bottom", Group.SIDES, "Bottom",
                "Avoid bottom-side system intrusions; omission preserves true.", 3);
        add(values, "minimum", Group.PADDING, "Minimum",
                "Physical EdgeInsets combined with MediaQuery padding; omission preserves EdgeInsets.zero.",
                4);
        add(values, "maintainBottomViewPadding", Group.VIEW_PADDING,
                "Maintain bottom view padding",
                "Use MediaQuery viewPadding at the bottom; omission preserves false.", 5);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "SafeArea schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " properties; actual=" + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            int dartOrder) {
        if (values.putIfAbsent(name,
                new Definition(group, displayName, description, name, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate SafeArea property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
