package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code Placeholder} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. All four value
 * parameters and the optional child preserve their Flutter defaults through
 * omission in a detached Designer prototype.</p>
 */
public final class PlaceholderWidgetPropertySchema {
    public static final WidgetTypeId PLACEHOLDER_TYPE =
            new WidgetTypeId("flutter.widgets.Placeholder");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 4;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("placeholderAppearance", "Appearance",
                "Outline color and stroke width."),
        FALLBACK_SIZE("placeholderFallbackSize", "Fallback size",
                "Dimensions used when the incoming axis is unbounded.");

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

    /** Metadata for one exact public {@code Placeholder} constructor property. */
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

    private PlaceholderWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the four reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "color", Group.APPEARANCE, "Color",
                "Outline color; omission preserves Color(0xFF455A64).", 0);
        add(values, "strokeWidth", Group.APPEARANCE, "Stroke width",
                "Non-negative outline width; omission preserves 2.0.", 1);
        add(values, "fallbackWidth", Group.FALLBACK_SIZE, "Fallback width",
                "Non-negative width used only for unbounded width; omission preserves 400.0.",
                2);
        add(values, "fallbackHeight", Group.FALLBACK_SIZE, "Fallback height",
                "Non-negative height used only for unbounded height; omission preserves 400.0.",
                3);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Placeholder schema must expose exactly "
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
            throw new IllegalStateException("Duplicate Placeholder property schema: " + name);
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
