package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code ClipRRect} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. The optional
 * {@code borderRadius} reuses the Designer's closed physical and directional
 * radius geometry, while omission preserves {@code BorderRadius.zero}. A
 * non-null {@code CustomClipper<RRect>} is represented only by the closed
 * project-Dart reference/zero-argument-invocation contract. The remaining
 * optional {@code clipBehavior} argument and optional child are represented
 * without arbitrary Dart expressions.</p>
 */
public final class ClipRRectWidgetPropertySchema {
    public static final WidgetTypeId CLIP_RRECT_TYPE =
            new WidgetTypeId("flutter.widgets.ClipRRect");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 3;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        GEOMETRY("clipRRectGeometry", "Geometry",
                "Rounded-rectangle clipping geometry."),
        DELEGATE("clipRRectDelegate", "Delegate",
                "Project-declared rounded-rectangle clip delegate."),
        CLIPPING("clipRRectClipping", "Clipping",
                "Rounded-rectangle paint clipping behavior.");

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

    /** Metadata for the three reviewed public constructor properties. */
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

    private ClipRRectWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "borderRadius", Group.GEOMETRY, "Border radius",
                "Physical or directional rounded corners used by the clip.", 0);
        add(values, "clipper", Group.DELEGATE, "Clipper",
                "Project Dart reference or zero-argument invocation assignable to CustomClipper<RRect>.",
                1);
        add(values, "clipBehavior", Group.CLIPPING, "Clip behavior",
                "How paint outside the rounded rectangle is clipped.", 2);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipRRect schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " properties; actual=" + values.size());
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
            throw new IllegalStateException("Duplicate ClipRRect property schema: " + name);
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
