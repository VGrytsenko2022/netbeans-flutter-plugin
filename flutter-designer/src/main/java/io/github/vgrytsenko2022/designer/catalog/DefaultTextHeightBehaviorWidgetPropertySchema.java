package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete flattened Flutter 3.44.8 DefaultTextHeightBehavior projection. */
public final class DefaultTextHeightBehaviorWidgetPropertySchema {
    public static final WidgetTypeId DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE =
            new WidgetTypeId("flutter.widgets.DefaultTextHeightBehavior");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 3;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        HEIGHT("defaultTextHeightBehavior", "Text height", "Inherited first-line ascent, last-line descent and leading distribution.");
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
    private DefaultTextHeightBehaviorWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "textHeightApplyFirstAscent", "Apply height to first ascent",
                "Omission preserves true inside the required TextHeightBehavior object. False uses the font's default ascent for the first line instead of TextStyle.height. This flag matters only when a text height is specified. All rows unset still establish a new true/true/proportional behavior; they do not inherit the outer DefaultTextHeightBehavior.",
                "applyHeightToFirstAscent", 0);
        add(values, "textHeightApplyLastDescent", "Apply height to last descent",
                "Omission preserves true inside the required TextHeightBehavior object. False uses the font's default descent for the last line instead of TextStyle.height. Descendant Text's explicit textHeightBehavior and DefaultTextStyle's non-null behavior take precedence over this inherited default.",
                "applyHeightToLastDescent", 1);
        add(values, "textHeightLeadingDistribution", "Leading distribution",
                "Omission preserves TextLeadingDistribution.proportional. Proportional distributes leading according to font ascent/descent; even distributes it equally. Distribution is applied before the first-ascent and last-descent flags. Negative leading is valid when TextStyle.height is small; no extra height constraint is imposed.",
                "leadingDistribution", 2);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("DefaultTextHeightBehavior schema/property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, String displayName,
            String description, String dartName, int order) {
        if (values.putIfAbsent(name, new Definition(Group.HEIGHT, displayName, description, dartName, order)) != null) {
            throw new IllegalStateException("Duplicate DefaultTextHeightBehavior property schema: " + name);
        }
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
