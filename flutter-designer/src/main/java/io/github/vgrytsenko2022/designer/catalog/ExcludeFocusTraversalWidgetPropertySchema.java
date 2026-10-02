package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code ExcludeFocusTraversal} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. Omitting
 * {@code excluding} preserves Flutter's exact {@code true} default, while the
 * required child must be supplied by atomic wrapping; no fake child is created.</p>
 */
public final class ExcludeFocusTraversalWidgetPropertySchema {
    public static final WidgetTypeId EXCLUDE_FOCUS_TRAVERSAL_TYPE =
            new WidgetTypeId("flutter.widgets.ExcludeFocusTraversal");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        FOCUS("excludeFocusTraversalBehavior", "Focus",
                "Keyboard focus traversal eligibility of the descendant subtree.");

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

    private ExcludeFocusTraversalWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed property in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "excluding", Group.FOCUS, "Excluding",
                "Omission preserves true: descendants are skipped by keyboard focus traversal, but explicit focus requests remain permitted and currently focused descendants retain focus. False permits traversal without automatically changing focus. Descendants' own skipTraversal configuration is not rewritten, but their effective skipTraversal getter is true under an excluded ancestor. Other focus restrictions still apply. Layout, painting and pointer hit testing are unchanged.",
                0);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ExcludeFocusTraversal schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " property; actual=" + values.size());
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
            throw new IllegalStateException(
                    "Duplicate ExcludeFocusTraversal property schema: " + name);
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
