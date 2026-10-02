package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code ExcludeFocus} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. Omitting
 * {@code excluding} preserves Flutter's exact {@code true} default, while the
 * required child must be supplied by atomic wrapping; no fake child is created.</p>
 */
public final class ExcludeFocusWidgetPropertySchema {
    public static final WidgetTypeId EXCLUDE_FOCUS_TYPE =
            new WidgetTypeId("flutter.widgets.ExcludeFocus");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        FOCUS("excludeFocusBehavior", "Focus",
                "Keyboard focus eligibility of the descendant subtree.");

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

    private ExcludeFocusWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed property in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "excluding", Group.FOCUS, "Excluding",
                "Omission preserves true: descendants cannot receive focus and currently focused descendants are unfocused. False permits focus but does not automatically refocus them. Descendants' own canRequestFocus configuration is not rewritten, but their effective focus eligibility is blocked. Layout, painting and pointer hit testing are unchanged.",
                0);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ExcludeFocus schema must expose exactly "
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
                    "Duplicate ExcludeFocus property schema: " + name);
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
