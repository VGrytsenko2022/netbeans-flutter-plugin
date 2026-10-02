package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Properties presentation and Dart-constructor contract for Flutter
 * {@code Container}.
 *
 * <p>The schema deliberately models Flutter's structured values instead of
 * flattening {@code AlignmentGeometry}, {@code BoxConstraints},
 * {@code Matrix4}, or {@code BoxDecoration} into unrelated rows. The optional
 * {@code child} remains a Designer slot and is therefore not counted among the
 * thirteen user-facing constructor properties.</p>
 */
public final class ContainerWidgetPropertySchema {
    public static final WidgetTypeId CONTAINER_TYPE =
            new WidgetTypeId("flutter.widgets.Container");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 13;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        LAYOUT("containerLayout", "Layout",
                "Alignment, size, and reviewed box constraints."),
        SPACING("containerSpacing", "Spacing",
                "Insets outside and inside the decorated box."),
        BACKGROUND("containerBackground", "Background",
                "Background color or a structured background decoration."),
        FOREGROUND("containerForeground", "Foreground",
                "Structured decoration painted in front of the child."),
        TRANSFORM("containerTransform", "Transform",
                "Reviewed four-by-four transform matrix and its alignment origin."),
        COMPOSITING("containerCompositing", "Compositing",
                "Decoration anti-aliasing and clipping behavior.");

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

    /** Metadata for one exact public {@code Container} constructor property. */
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

    private ContainerWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the thirteen properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "alignment", Group.LAYOUT, "Alignment",
                "Physical or directional alignment of the child within the container.", 0);
        add(values, "padding", Group.SPACING, "Padding",
                "Non-negative logical pixels between the decoration and the child.", 1);
        add(values, "color", Group.BACKGROUND, "Color",
                "Literal ARGB color or semantic Material ColorScheme role. Mutually exclusive "
                + "with Background decoration.", 2);
        add(values, "isAntiAlias", Group.COMPOSITING, "Anti-alias",
                "Whether Container anti-aliases its simple color layer; unset preserves "
                + "Flutter's true default.", 3);
        add(values, "decoration", Group.BACKGROUND, "Background decoration",
                "Reviewed BoxDecoration painted behind the child; mutually exclusive with Color.", 4);
        add(values, "foregroundDecoration", Group.FOREGROUND, "Foreground decoration",
                "Reviewed BoxDecoration painted in front of the child.", 5);
        add(values, "width", Group.LAYOUT, "Width",
                "Finite non-negative width in logical pixels.", 6);
        add(values, "height", Group.LAYOUT, "Height",
                "Finite non-negative height in logical pixels.", 7);
        add(values, "constraints", Group.LAYOUT, "Constraints",
                "Reviewed minimum and maximum width and height constraints.", 8);
        add(values, "margin", Group.SPACING, "Margin",
                "Non-negative logical pixels outside the decorated box.", 9);
        add(values, "transform", Group.TRANSFORM, "Transform",
                "Finite 4x4 Matrix4 applied before painting.", 10);
        add(values, "transformAlignment", Group.TRANSFORM, "Transform alignment",
                "Physical or directional origin for the transform.", 11);
        // Dart constructor position 12 belongs to the optional child slot.
        add(values, "clipBehavior", Group.COMPOSITING, "Clip behavior",
                "Clip.none or a reviewed clipping mode; non-none clipping requires a decoration.", 13);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Container schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
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
            throw new IllegalStateException("Duplicate Container property schema: " + name);
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
