package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 Drawer constructor projection. */
public final class DrawerWidgetPropertySchema {
    public static final WidgetTypeId DRAWER_TYPE =
            new WidgetTypeId("flutter.material.Drawer");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 8;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("drawerAppearance", "Appearance",
                "Theme-aware surface colors, elevation and shape."),
        LAYOUT("drawerLayout", "Layout", "Drawer width and its child content."),
        BEHAVIOR("drawerBehavior", "Behavior", "Accessibility label and clipping policy.");

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

    /** Metadata for one exact public {@code Drawer} constructor property. */
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
                throw new IllegalArgumentException("Negative Drawer property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private DrawerWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name));
    }

    /** Returns the eight reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal or reviewed ColorScheme role; omission uses DrawerTheme and the Material default.", 0);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Finite non-negative elevation; omission uses DrawerTheme and the SDK default.", 1);
        add(values, "shadowColor", Group.APPEARANCE, "Shadow color",
                "Literal or reviewed ColorScheme role; omission follows DrawerTheme and Material defaults.", 2);
        add(values, "surfaceTintColor", Group.APPEARANCE, "Surface tint color",
                "Literal or reviewed ColorScheme role; omission follows DrawerTheme and Material defaults.", 3);
        add(values, "shape", Group.APPEARANCE, "Shape reference",
                "Strict ShapeBorder reference/factory or explicit null; application-owned references are not executed by Canvas.", 4);
        add(values, "width", Group.LAYOUT, "Width",
                "Finite non-negative drawer width; omission uses DrawerTheme and the Material specification default.", 5);
        add(values, "semanticLabel", Group.BEHAVIOR, "Semantic label",
                "Optional accessibility route label; omission lets MaterialLocalizations provide the platform label.", 7);
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior",
                "Clip policy for the drawer shape; omission keeps the SDK/theme shape-dependent default.", 8);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Drawer schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
                            + " properties; actual=" + values.size());
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
            throw new IllegalStateException("Duplicate Drawer property schema: " + name);
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
