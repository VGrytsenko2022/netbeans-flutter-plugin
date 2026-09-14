package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 {@code Material} constructor projection. */
public final class MaterialWidgetPropertySchema {
    public static final WidgetTypeId MATERIAL_TYPE =
            new WidgetTypeId("flutter.material.Material");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 12;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("materialAppearance", "Appearance",
                "Material kind, elevation, colors and text styling."),
        SHAPE("materialShape", "Shape",
                "Mutually exclusive border radius and ShapeBorder reference."),
        BEHAVIOR("materialBehavior", "Behavior",
                "Clipping, border paint order and color animation."),
        LAYOUT("materialLayout", "Layout",
                "Animation duration and optional child content.");

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

    /** Metadata for one exact public {@code Material} constructor property. */
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
            if (dartOrder < 0) throw new IllegalArgumentException("Negative Material property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private MaterialWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "materialType", Group.APPEARANCE, "Material type",
                "MaterialType selector. Canvas maps all reviewed values to the real SDK enum.", "type", 0);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Finite non-negative elevation. Omission preserves the SDK default of zero.", "elevation", 1);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal or reviewed ColorScheme role; omission follows the Material type and theme.", "color", 2);
        add(values, "shadowColor", Group.APPEARANCE, "Shadow color",
                "Literal or reviewed ColorScheme role for the elevation shadow.", "shadowColor", 3);
        add(values, "surfaceTintColor", Group.APPEARANCE, "Surface tint color",
                "Literal or reviewed ColorScheme role for the elevation overlay.", "surfaceTintColor", 4);
        add(values, "textStyle", Group.APPEARANCE, "Text style",
                "Strict TextStyle project/package reference or explicit null.", "textStyle", 5);
        add(values, "borderRadius", Group.SHAPE, "Border radius",
                "Physical or directional BorderRadiusGeometry. Mutually exclusive with shape and invalid for circle materials.", "borderRadius", 6);
        add(values, "shape", Group.SHAPE, "Shape reference",
                "Strict ShapeBorder project/package reference or explicit null. Canvas retains the reference but uses the SDK fallback shape.", "shape", 7);
        add(values, "borderOnForeground", Group.BEHAVIOR, "Border on foreground",
                "Whether a ShapeBorder paints in front of the child; omission preserves true.", "borderOnForeground", 8);
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior",
                "Clip policy for the material shape; omission preserves Clip.none.", "clipBehavior", 9);
        add(values, "animationDurationUs", Group.LAYOUT, "Animation duration",
                "Exact signed portable microseconds, strict Duration reference/factory or null. Omission preserves kThemeChangeDuration.", "animationDuration", 10);
        add(values, "animateColor", Group.BEHAVIOR, "Animate color",
                "Whether color changes animate; omission preserves false.", "animateColor", 12);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Material schema count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, String dartName, int dartOrder) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, dartName, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate Material property " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
