package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete non-key AbsorbPointer constructor contract, including its deprecated nullable semantics override. */
public final class AbsorbPointerWidgetPropertySchema {
    public static final WidgetTypeId ABSORB_POINTER_TYPE =
            new WidgetTypeId("flutter.widgets.AbsorbPointer");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 2;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("absorbPointerBehavior", "Behavior", "Pointer hit-testing behavior without changing layout or painting."),
        SEMANTICS("absorbPointerSemantics", "Semantics", "Deprecated accessibility override retained for full SDK compatibility.");

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

    private AbsorbPointerWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns both reviewed properties in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "absorbing", Group.BEHAVIOR, "Absorbing",
                "Omission preserves true: this widget absorbs pointer hits, blocking its subtree and targets behind it while preserving layout and painting.",
                0);
        add(values, "ignoringSemantics", Group.SEMANTICS, "Ignoring semantics (deprecated)",
                "Deprecated SDK override. Omission preserves null: absorbing blocks semantic user actions but retains other semantics. "
                + "False preserves semantic actions even when absorbing is true; true drops the semantics subtree.",
                1);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "AbsorbPointer schema must expose exactly "
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
            throw new IllegalStateException(
                    "Duplicate AbsorbPointer property schema: " + name);
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
