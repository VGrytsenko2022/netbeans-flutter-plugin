package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete non-key PhysicalModel constructor projection for Flutter 3.44.8. */
public final class PhysicalModelWidgetPropertySchema {
    public static final WidgetTypeId PHYSICAL_MODEL_TYPE =
            new WidgetTypeId("flutter.widgets.PhysicalModel");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 6;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        GEOMETRY("physicalModelGeometry", "Geometry", "Physical layer shape and corner geometry."),
        CLIPPING("physicalModelClipping", "Clipping", "Physical layer child clipping behavior."),
        APPEARANCE("physicalModelAppearance", "Appearance", "Physical layer fill, elevation and shadow.");

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
            if (dartOrder < 0) {
                throw new IllegalArgumentException("dartOrder must be non-negative");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private PhysicalModelWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Properties retain the pinned SDK's constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "shape", Group.GEOMETRY, "Shape",
                "Rectangle or circle; Flutter ignores border radius for circles without removing it.", 0);
        add(values, "clipBehavior", Group.CLIPPING, "Clip behavior",
                "How the child is clipped to the physical shape; omitted means Clip.none.", 1);
        add(values, "borderRadius", Group.GEOMETRY, "Border radius",
                "Physical elliptical corners only; ignored but retained when shape is circle.", 2);
        add(values, "elevation", Group.APPEARANCE, "Elevation",
                "Finite non-negative elevation in logical pixels; omitted means zero.", 3);
        add(values, "color", Group.APPEARANCE, "Color",
                "Required physical layer fill color; literal color or reviewed theme color.", 4);
        add(values, "shadowColor", Group.APPEARANCE, "Shadow color",
                "Shadow color; literal or reviewed theme color. Omission preserves opaque black.", 5);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("PhysicalModel schema property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate PhysicalModel property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) { throw new IllegalArgumentException(label + " must not be blank"); }
        return value;
    }
}
