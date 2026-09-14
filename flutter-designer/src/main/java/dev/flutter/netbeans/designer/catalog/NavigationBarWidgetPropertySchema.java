package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 NavigationBar constructor projection. */
public final class NavigationBarWidgetPropertySchema {
    public static final WidgetTypeId NAVIGATION_BAR_TYPE =
            new WidgetTypeId("flutter.material.NavigationBar");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 15;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("Behavior", "Selection, animation and activation."),
        APPEARANCE("Appearance", "Navigation bar colors and elevation."),
        INDICATOR("Indicator", "Selected destination indicator."),
        LABEL("Labels", "Destination labels and spacing."),
        LAYOUT("Layout", "Navigation bar dimensions and safe-area behavior.");

        private final String displayName;
        private final String description;

        Group(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return "navigationBar" + displayName.replace(" ", "");
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return "Flutter NavigationBar " + description.toLowerCase(java.util.Locale.ROOT);
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
                throw new IllegalArgumentException("Negative NavigationBar property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private NavigationBarWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name)));
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name).value());
    }

    public static List<String> durationProperties() {
        return List.of("animationDurationUs");
    }

    public static List<String> colorProperties() {
        return List.of("backgroundColor", "shadowColor", "surfaceTintColor", "indicatorColor");
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "animationDurationUs", Group.BEHAVIOR,
                "Exact signed portable microseconds, strict Duration reference/factory or null. Omission/null preserves NavigationBarTheme and SDK animation defaults; source and model retain the exact value.",
                "animationDuration", 0);
        add(values, "selectedIndex", Group.BEHAVIOR,
                "Required non-negative destination index, created at zero. It must be less than the number of destinations; invalid edits are rejected without clamping.",
                "selectedIndex", 1);
        add(values, "onDestinationSelected", Group.BEHAVIOR,
                "Optional ValueChanged<int> callback reference, explicit null or no-op. Canvas never executes project callbacks.",
                "onDestinationSelected", 2);
        add(values, "backgroundColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference. Omission/null uses NavigationBarTheme and SDK defaults.",
                "backgroundColor", 3);
        add(values, "elevation", Group.APPEARANCE,
                "Finite signed elevation; omission/null uses NavigationBarTheme and the Material 3 default.",
                "elevation", 4);
        add(values, "shadowColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference for the drop shadow.",
                "shadowColor", 5);
        add(values, "surfaceTintColor", Group.APPEARANCE,
                "Literal or reviewed Material ColorScheme token/reference for the surface tint.",
                "surfaceTintColor", 6);
        add(values, "indicatorColor", Group.INDICATOR,
                "Literal or reviewed Material ColorScheme token/reference for the selected indicator.",
                "indicatorColor", 7);
        add(values, "indicatorShape", Group.INDICATOR,
                "Strict ShapeBorder reference/factory or explicit null. Omission/null uses NavigationBarTheme and the SDK StadiumBorder/RoundedRectangleBorder fallback.",
                "indicatorShape", 8);
        add(values, "height", Group.LAYOUT,
                "Finite non-negative height; omission/null uses NavigationBarTheme and the SDK default of 80 logical pixels.",
                "height", 9);
        add(values, "labelBehavior", Group.LABEL,
                "NavigationDestinationLabelBehavior alwaysShow, onlyShowSelected or alwaysHide. Omission/null preserves theme and SDK defaults.",
                "labelBehavior", 10);
        add(values, "overlayColor", Group.APPEARANCE,
                "Strict WidgetStateProperty<Color?> reference/factory or explicit null. The isolated Canvas does not execute project state resolution.",
                "overlayColor", 11);
        add(values, "labelTextStyle", Group.LABEL,
                "Strict WidgetStateProperty<TextStyle?> reference/factory or explicit null. The isolated Canvas preserves the SDK/theme label style.",
                "labelTextStyle", 12);
        add(values, "labelPadding", Group.LABEL,
                "Physical or directional EdgeInsetsGeometry, strict reference/factory or explicit null. Omission/null uses the SDK four-pixel top padding.",
                "labelPadding", 13);
        add(values, "maintainBottomViewPadding", Group.LAYOUT,
                "Whether the internal SafeArea preserves bottom view padding while the software keyboard opens. Omission uses false.",
                "maintainBottomViewPadding", 14);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("NavigationBar property count: " + values.size());
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
        String label = name.equals("animationDurationUs") ? "Animation duration"
                : Character.toUpperCase(name.charAt(0))
                        + name.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
        if (values.put(name, new Definition(group, label, description, dartName, order)) != null) {
            throw new IllegalStateException("Duplicate NavigationBar property " + name);
        }
    }
}
