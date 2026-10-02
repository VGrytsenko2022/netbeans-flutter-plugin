package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code IndexedStack} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. All five remaining
 * constructor properties are direct optional named arguments, and
 * {@code children} is an ordered optional list slot. Omitting {@code index}
 * preserves Flutter's default zero, while {@code PropertyValue.NullValue}
 * represents an explicitly supplied Dart {@code null}.</p>
 */
public final class IndexedStackWidgetPropertySchema {
    public static final WidgetTypeId INDEXED_STACK_TYPE =
            new WidgetTypeId("flutter.widgets.IndexedStack");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 5;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        LAYOUT("indexedStackLayout", "Layout",
                "Alignment, direction, sizing, clipping, and the visible child index.");

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

    /** Metadata for one exact public constructor property. */
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

    private IndexedStackWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the five reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "alignment", "Alignment",
                "Alignment used for every child within the stack.", 0);
        add(values, "textDirection", "Text direction",
                "Direction used to resolve directional alignment.", 1);
        add(values, "clipBehavior", "Clip behavior",
                "How overflowing child paint is clipped.", 2);
        add(values, "sizing", "Sizing",
                "How non-positioned children contribute to the stack size.", 3);
        add(values, "index", "Index",
                "Visible child index; explicit null hides every child.", 4);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "IndexedStack schema must expose exactly "
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
                new Definition(Group.LAYOUT, displayName, description, name, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate IndexedStack property schema: " + name);
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
