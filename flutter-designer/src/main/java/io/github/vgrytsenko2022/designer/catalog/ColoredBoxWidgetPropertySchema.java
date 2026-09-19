package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code ColoredBox} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. The required color,
 * optional anti-alias policy, and optional child preserve the complete public
 * default-constructor surface used by the Designer.</p>
 */
public final class ColoredBoxWidgetPropertySchema {
    public static final WidgetTypeId COLORED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.ColoredBox");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 2;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("coloredBoxAppearance", "Appearance",
                "Background color and edge anti-aliasing policy.");

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

    /** Metadata for one exact public {@code ColoredBox} constructor property. */
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

    private ColoredBoxWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the two reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "color", "Color",
                "Required literal ARGB color or reviewed Material ColorScheme role.", 0);
        add(values, "isAntiAlias", "Anti-alias",
                "Whether the painted box smooths transformed edges; omission preserves true.",
                1);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ColoredBox schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " properties; actual=" + values.size());
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
                new Definition(
                        Group.APPEARANCE,
                        displayName,
                        description,
                        name,
                        dartOrder)) != null) {
            throw new IllegalStateException("Duplicate ColoredBox property schema: " + name);
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
