package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 BottomAppBar constructor projection. */
public final class BottomAppBarWidgetPropertySchema {
    public static final WidgetTypeId BOTTOM_APP_BAR_TYPE =
            new WidgetTypeId("flutter.material.BottomAppBar");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 9;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("bottomAppBarAppearance", "Appearance",
                "Theme-aware surface colors, elevation and shape."),
        LAYOUT("bottomAppBarLayout", "Layout",
                "Bottom app bar height, padding and notch margin."),
        BEHAVIOR("bottomAppBarBehavior", "Behavior",
                "Clipping policy and application-owned shape reference.");

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

    /** Metadata for one exact public {@code BottomAppBar} constructor property. */
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
                throw new IllegalArgumentException("Negative BottomAppBar property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private BottomAppBarWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name));
    }

    /** Returns the ten reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal or reviewed ColorScheme role; omission uses BottomAppBarTheme and Material defaults.", 0);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Finite non-negative elevation; omission uses BottomAppBarTheme and the SDK default.", 1);
        add(values, "shape", Group.BEHAVIOR, "Shape reference",
                "Strict NotchedShape reference/factory or explicit null; application-owned references are not executed by Canvas.", 2);
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior",
                "Clip policy for the bar shape; omission preserves the SDK default Clip.none.", 3);
        add(values, "notchMargin", Group.LAYOUT, "Notch margin",
                "Finite non-negative margin around a FloatingActionButton notch; omission uses the SDK default of 4.0.", 4);
        add(values, "padding", Group.LAYOUT, "Padding",
                "Optional physical or directional EdgeInsetsGeometry around the child content.", 7);
        add(values, "surfaceTintColor", Group.APPEARANCE, "Surface tint color",
                "Literal or reviewed ColorScheme role; omission follows BottomAppBarTheme and Material defaults.", 8);
        add(values, "shadowColor", Group.APPEARANCE, "Shadow color",
                "Literal or reviewed ColorScheme role; omission follows BottomAppBarTheme and Material defaults.", 9);
        add(values, "height", Group.LAYOUT, "Height",
                "Finite non-negative bar height; omission uses the SDK/theme default.", 10);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "BottomAppBar schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
                            + " properties; actual=" + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int dartOrder) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate BottomAppBar property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
