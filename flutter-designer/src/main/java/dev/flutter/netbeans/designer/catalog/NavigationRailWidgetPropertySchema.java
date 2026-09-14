package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 NavigationRail constructor projection. */
public final class NavigationRailWidgetPropertySchema {
    public static final WidgetTypeId NAVIGATION_RAIL_TYPE =
            new WidgetTypeId("flutter.material.NavigationRail");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 20;
    public static final int DIRECT_PROPERTY_COUNT = 20;
    public static final int SLOT_COUNT = 3;

    public enum Group {
        BEHAVIOR("Behavior", "Selection, extension and destination interaction."),
        APPEARANCE("Appearance", "Navigation rail colors, elevation and text/icon themes."),
        INDICATOR("Indicator", "Selected destination indicator."),
        LABEL("Labels", "Destination label behavior."),
        LAYOUT("Layout", "Rail alignment, sizing and scrolling.");

        private final String displayName;
        private final String description;

        Group(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return "navigationRail" + displayName.replace(" ", "");
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return "Flutter NavigationRail " + description.toLowerCase(java.util.Locale.ROOT);
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
                throw new IllegalArgumentException("Negative NavigationRail property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private NavigationRailWidgetPropertySchema() {
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
        return List.of("backgroundColor", "indicatorColor");
    }

    public static List<String> referenceProperties() {
        return List.of("unselectedLabelTextStyle", "selectedLabelTextStyle",
                "unselectedIconTheme", "selectedIconTheme", "indicatorShape");
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "backgroundColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference. Omission/null uses NavigationRailTheme and SDK defaults.",
                "backgroundColor", 0);
        add(values, "extended", Group.BEHAVIOR,
                "Whether the rail expands beside destination labels. Omission uses false. When true, labelType must be null or none.",
                "extended", 1);
        add(values, "selectedIndex", Group.BEHAVIOR,
                "Nullable non-negative destination index. Null leaves no destination selected; invalid indices are rejected without clamping.",
                "selectedIndex", 5);
        add(values, "onDestinationSelected", Group.BEHAVIOR,
                "Optional ValueChanged<int> callback reference, explicit null or no-op. Canvas never executes project callbacks.",
                "onDestinationSelected", 6);
        add(values, "elevation", Group.APPEARANCE,
                "Finite positive elevation; omission/null uses NavigationRailTheme and SDK defaults.",
                "elevation", 7);
        add(values, "groupAlignment", Group.LAYOUT,
                "Finite alignment from -1.0 (top) through 1.0 (bottom). Omission/null uses the SDK start alignment.",
                "groupAlignment", 8);
        add(values, "labelType", Group.LABEL,
                "NavigationRailLabelType none, selected or all. Omission/null uses NavigationRailTheme and SDK defaults.",
                "labelType", 9);
        add(values, "unselectedLabelTextStyle", Group.APPEARANCE,
                "Strict TextStyle reference/factory or explicit null. Canvas keeps the SDK/theme fallback for application-owned references.",
                "unselectedLabelTextStyle", 10);
        add(values, "selectedLabelTextStyle", Group.APPEARANCE,
                "Strict TextStyle reference/factory or explicit null. Canvas keeps the SDK/theme fallback for application-owned references.",
                "selectedLabelTextStyle", 11);
        add(values, "unselectedIconTheme", Group.APPEARANCE,
                "Strict IconThemeData reference/factory or explicit null. Canvas keeps the SDK/theme fallback for application-owned references.",
                "unselectedIconTheme", 12);
        add(values, "selectedIconTheme", Group.APPEARANCE,
                "Strict IconThemeData reference/factory or explicit null. Canvas keeps the SDK/theme fallback for application-owned references.",
                "selectedIconTheme", 13);
        add(values, "minWidth", Group.LAYOUT,
                "Finite positive minimum rail width. Omission/null uses the SDK default of 72 logical pixels.",
                "minWidth", 14);
        add(values, "minExtendedWidth", Group.LAYOUT,
                "Finite positive extended width, at least minWidth when both are supplied. Omission/null uses the SDK default of 256 logical pixels.",
                "minExtendedWidth", 15);
        add(values, "useIndicator", Group.INDICATOR,
                "Whether to draw the selected destination indicator. Null follows NavigationRailTheme and Material 3 defaults.",
                "useIndicator", 16);
        add(values, "indicatorColor", Group.INDICATOR,
                "Literal or reviewed Material ColorScheme token/reference for the selected indicator.",
                "indicatorColor", 17);
        add(values, "indicatorShape", Group.INDICATOR,
                "Strict ShapeBorder reference/factory or explicit null. Omission/null uses NavigationRailTheme and the SDK StadiumBorder fallback.",
                "indicatorShape", 18);
        add(values, "leadingAtTop", Group.BEHAVIOR,
                "Pins the optional leading widget to the top when true. Omission uses true.",
                "leadingAtTop", 19);
        add(values, "trailingAtBottom", Group.BEHAVIOR,
                "Pins the optional trailing widget to the bottom when true. Omission uses false.",
                "trailingAtBottom", 20);
        add(values, "scrollable", Group.BEHAVIOR,
                "Allows the main destination group to scroll when vertical space is insufficient. Omission uses false.",
                "scrollable", 21);
        add(values, "mainAxisAlignment", Group.LAYOUT,
                "Optional MainAxisAlignment controlling destination spacing when extra vertical space is available.",
                "mainAxisAlignment", 22);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationRail property count: " + values.size());
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
            throw new IllegalStateException("Duplicate NavigationRail property " + name);
        }
    }
}
