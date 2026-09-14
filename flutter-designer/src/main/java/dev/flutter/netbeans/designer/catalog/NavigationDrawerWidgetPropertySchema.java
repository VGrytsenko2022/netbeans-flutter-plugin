package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 NavigationDrawer constructor projection. */
public final class NavigationDrawerWidgetPropertySchema {
    public static final WidgetTypeId NAVIGATION_DRAWER_TYPE =
            new WidgetTypeId("flutter.material.NavigationDrawer");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 9;
    public static final int DIRECT_PROPERTY_COUNT = 9;
    public static final int SLOT_COUNT = 3;

    public enum Group {
        BEHAVIOR("Behavior", "Selection and destination activation."),
        APPEARANCE("Appearance", "Drawer colors, elevation and indicator."),
        LAYOUT("Layout", "Drawer padding and content slots.");

        private final String displayName;
        private final String description;

        Group(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return "navigationDrawer" + displayName.replace(" ", "");
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return "Flutter NavigationDrawer " + description.toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Definition(
            Group group,
            String displayName,
            String description,
            String dartName,
            int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative NavigationDrawer property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private NavigationDrawerWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name, "name").value());
    }

    public static List<String> colorProperties() {
        return List.of("backgroundColor", "shadowColor", "surfaceTintColor", "indicatorColor");
    }

    public static List<String> referenceProperties() {
        return List.of("indicatorShape");
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "backgroundColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference. Omission/null uses NavigationDrawerTheme and SDK defaults.",
                "backgroundColor", 3);
        add(values, "shadowColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference for the drop shadow.",
                "shadowColor", 4);
        add(values, "surfaceTintColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference for the surface tint.",
                "surfaceTintColor", 5);
        add(values, "elevation", Group.APPEARANCE,
                "Finite signed elevation; omission/null uses NavigationDrawerTheme and the Material default.",
                "elevation", 6);
        add(values, "indicatorColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference for selected destinations.",
                "indicatorColor", 7);
        add(values, "indicatorShape", Group.APPEARANCE,
                "Strict ShapeBorder reference/factory or explicit null. Omission/null uses NavigationDrawerTheme and the SDK StadiumBorder fallback.",
                "indicatorShape", 8);
        add(values, "onDestinationSelected", Group.BEHAVIOR,
                "Optional ValueChanged<int> callback reference, explicit null or no-op. Canvas never executes project callbacks.",
                "onDestinationSelected", 9);
        add(values, "selectedIndex", Group.BEHAVIOR,
                "Nullable non-negative destination index, created at zero. Invalid indices are retained but render all destinations unselected.",
                "selectedIndex", 10);
        add(values, "tilePadding", Group.LAYOUT,
                "Physical or directional non-negative EdgeInsetsGeometry, strict reference/factory or explicit null. Omission/null uses horizontal 12 logical pixels.",
                "tilePadding", 11);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationDrawer property count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String description,
            String dartName,
            int order) {
        String label = Character.toUpperCase(name.charAt(0))
                + name.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
        if (values.put(name, new Definition(group, label, description, dartName, order)) != null) {
            throw new IllegalStateException("Duplicate NavigationDrawer property " + name);
        }
    }
}
