package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code DecoratedBox} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. Flutter accepts the
 * abstract {@code Decoration} contract; Designer intentionally exposes the
 * complete reviewed {@code BoxDecoration} branch. The required decoration has
 * no Flutter constructor default, so detached Palette prototypes persist an
 * explicit empty rectangular {@code BoxDecoration}. The optional child remains
 * a Designer slot.</p>
 */
public final class DecoratedBoxWidgetPropertySchema {
    public static final WidgetTypeId DECORATED_BOX_TYPE =
            new WidgetTypeId("flutter.widgets.DecoratedBox");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 2;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        DECORATION("decoratedBoxDecoration", "Decoration",
                "Reviewed BoxDecoration and whether it paints behind or in front of the child.");

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

    /** Metadata for one exact public {@code DecoratedBox} constructor property. */
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

    private DecoratedBoxWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the two reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "decoration", Group.DECORATION, "Decoration",
                "Required reviewed BoxDecoration painted by this widget.", 0);
        add(values, "position", Group.DECORATION, "Position",
                "Paint the decoration behind or in front of the child.", 1);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "DecoratedBox schema must expose exactly "
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
            throw new IllegalStateException("Duplicate DecoratedBox property schema: " + name);
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
