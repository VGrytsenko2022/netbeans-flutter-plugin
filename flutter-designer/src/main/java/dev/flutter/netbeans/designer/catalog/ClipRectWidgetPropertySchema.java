package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code ClipRect} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. A non-null
 * {@code CustomClipper<Rect>} is also outside the closed Designer model because
 * Flutter exposes no public concrete delegate and managed Dart regions do not
 * own class declarations. Omitting {@code clipper} therefore keeps the exact
 * child-bounds rectangle. The remaining optional {@code clipBehavior} argument
 * and optional child are represented without arbitrary Dart expressions.</p>
 */
public final class ClipRectWidgetPropertySchema {
    public static final WidgetTypeId CLIP_RECT_TYPE =
            new WidgetTypeId("flutter.widgets.ClipRect");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        CLIPPING("clipRectClipping", "Clipping",
                "Rectangular paint clipping behavior.");

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

    /** Metadata for the one reviewed public constructor property. */
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

    private ClipRectWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed property in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "clipBehavior", "Clip behavior",
                "How paint outside the child-bounds rectangle is clipped.", 0);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipRect schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " property; actual=" + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            int dartOrder) {
        if (values.putIfAbsent(name,
                new Definition(Group.CLIPPING, displayName, description,
                        name, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate ClipRect property schema: " + name);
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
