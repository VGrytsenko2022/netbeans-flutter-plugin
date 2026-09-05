package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete six-field Flutter 3.44.8 VerticalDivider constructor presentation. */
public final class VerticalDividerWidgetPropertySchema {
    public static final WidgetTypeId VERTICAL_DIVIDER_TYPE = new WidgetTypeId("flutter.material.VerticalDivider");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 6;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        LAYOUT("verticalDividerLayout", "Layout", "Total horizontal space and physical top/bottom insets."),
        APPEARANCE("verticalDividerAppearance", "Appearance", "Line thickness, semantic color and corner geometry.");
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

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = definitions();
    private VerticalDividerWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "width", Group.LAYOUT, "Width",
                "Non-negative total horizontal space with the vertical line centered inside it. Unset uses DividerTheme.space, then 16 logical pixels; this is not the line thickness.", 0);
        add(values, "thickness", Group.APPEARANCE, "Thickness",
                "Non-negative line thickness. Unset uses DividerTheme.thickness, then 1 in Material 3 or the zero-width hairline in Material 2. Explicit zero is a one-device-pixel hairline. The SDK cannot paint a nonzero corner radius on a hairline.", 1);
        add(values, "indent", Group.LAYOUT, "Indent",
                "Non-negative empty space above the vertical line. This is a top inset and does not change in RTL. Unset uses DividerTheme.indent, then zero.", 2);
        add(values, "endIndent", Group.LAYOUT, "End indent",
                "Non-negative empty space below the vertical line. This is a bottom inset and does not change in RTL. Unset uses DividerTheme.endIndent, then zero.", 3);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB color or reviewed Material ColorScheme role. Unset uses DividerTheme.color, then ColorScheme.outlineVariant in Material 3 or Theme.dividerColor in Material 2.", 4);
        add(values, "radius", Group.APPEARANCE, "Radius",
                "Physical or directional non-negative elliptical corner radii. Directional corners follow ambient text direction, independently of the physical top/bottom indents. Unset uses DividerTheme.radius, then square corners; explicit zero clears an inherited radius. Nonzero radii require a positive effective line thickness to paint in the pinned SDK.", 5);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("VerticalDivider schema/property count mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, order)) != null) throw new IllegalStateException("Duplicate VerticalDivider property schema: " + name);
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
