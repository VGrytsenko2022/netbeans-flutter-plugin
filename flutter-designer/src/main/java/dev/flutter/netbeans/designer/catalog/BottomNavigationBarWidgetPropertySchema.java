package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 BottomNavigationBar constructor projection. */
public final class BottomNavigationBarWidgetPropertySchema {
    public static final WidgetTypeId BOTTOM_NAVIGATION_BAR_TYPE =
            new WidgetTypeId("flutter.material.BottomNavigationBar");
    /** Number of direct constructor arguments; the required items list is a slot. */
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 20;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("Behavior", "Selection and activation."),
        APPEARANCE("Appearance", "Colors, elevation and icon themes."),
        LABEL("Labels", "Label styles and visibility."),
        LAYOUT("Layout", "Icon sizing and landscape arrangement.");

        private final String displayName;
        private final String description;

        Group(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return "bottomNavigationBar" + displayName.replace(" ", "");
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return "Flutter BottomNavigationBar " + description.toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Definition(Group group, String displayName, String description,
            String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("Negative BottomNavigationBar property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private BottomNavigationBarWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name, "name").value());
    }

    public static List<String> colorProperties() {
        return List.of("backgroundColor", "selectedItemColor", "unselectedItemColor");
    }

    public static List<String> referenceProperties() {
        return List.of("selectedIconTheme", "unselectedIconTheme", "selectedLabelStyle", "unselectedLabelStyle", "mouseCursor");
    }

    private static Map<String, Definition> createDefinitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "onTap", Group.BEHAVIOR, "Optional ValueChanged<int> callback reference, explicit null or no-op. Canvas never executes project callbacks.", "onTap", 0);
        add(values, "currentIndex", Group.BEHAVIOR, "Non-negative active item index, created at zero and validated against the item list.", "currentIndex", 1);
        add(values, "elevation", Group.APPEARANCE, "Finite non-negative elevation; omission/null uses BottomNavigationBarTheme and SDK defaults.", "elevation", 2);
        add(values, "barType", Group.LAYOUT, "BottomNavigationBarType fixed or shifting; omission follows the SDK item-count default.", "type", 3);
        add(values, "backgroundColor", Group.APPEARANCE, "Literal or reviewed Material ColorScheme token/reference. Omission/null uses theme and SDK defaults.", "backgroundColor", 4);
        add(values, "iconSize", Group.LAYOUT, "Finite non-negative icon size, defaulting to 24 logical pixels.", "iconSize", 5);
        add(values, "selectedItemColor", Group.APPEARANCE, "Literal or reviewed Material ColorScheme token/reference for selected icons and labels.", "selectedItemColor", 6);
        add(values, "unselectedItemColor", Group.APPEARANCE, "Literal or reviewed Material ColorScheme token/reference for unselected icons and labels.", "unselectedItemColor", 7);
        add(values, "selectedIconTheme", Group.APPEARANCE, "Strict IconThemeData reference/factory or explicit null.", "selectedIconTheme", 8);
        add(values, "unselectedIconTheme", Group.APPEARANCE, "Strict IconThemeData reference/factory or explicit null.", "unselectedIconTheme", 9);
        add(values, "selectedFontSize", Group.LABEL, "Finite non-negative selected-label font size, defaulting to 14 logical pixels.", "selectedFontSize", 10);
        add(values, "unselectedFontSize", Group.LABEL, "Finite non-negative unselected-label font size, defaulting to 12 logical pixels.", "unselectedFontSize", 11);
        add(values, "selectedLabelStyle", Group.LABEL, "Strict TextStyle reference/factory or explicit null.", "selectedLabelStyle", 12);
        add(values, "unselectedLabelStyle", Group.LABEL, "Strict TextStyle reference/factory or explicit null.", "unselectedLabelStyle", 13);
        add(values, "showSelectedLabels", Group.LABEL, "Nullable boolean controlling selected labels; null preserves theme/SDK defaults.", "showSelectedLabels", 14);
        add(values, "showUnselectedLabels", Group.LABEL, "Nullable boolean controlling unselected labels; null preserves theme/SDK defaults.", "showUnselectedLabels", 15);
        add(values, "mouseCursor", Group.APPEARANCE, "Strict MouseCursor reference/factory or explicit null.", "mouseCursor", 16);
        add(values, "enableFeedback", Group.BEHAVIOR, "Nullable boolean controlling acoustic/haptic feedback.", "enableFeedback", 17);
        add(values, "landscapeLayout", Group.LAYOUT, "BottomNavigationBarLandscapeLayout spread, centered or linear; omission preserves theme/SDK defaults.", "landscapeLayout", 18);
        add(values, "useLegacyColorScheme", Group.APPEARANCE, "Whether legacy selected/unselected color resolution is used; omission defaults to true.", "useLegacyColorScheme", 19);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("BottomNavigationBar property count: " + values.size());
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String description, String dartName, int order) {
        String label = name.equals("barType") ? "Type"
                : Character.toUpperCase(name.charAt(0)) + name.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
        if (values.put(name, new Definition(group, label, description, dartName, order)) != null) {
            throw new IllegalStateException("Duplicate BottomNavigationBar property " + name);
        }
    }
}
