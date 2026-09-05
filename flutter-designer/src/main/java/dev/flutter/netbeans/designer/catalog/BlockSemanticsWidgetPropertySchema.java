package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code BlockSemantics} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} is deliberately excluded. Omitting
 * {@code blocking} preserves Flutter's exact {@code true} default, while the
 * optional child remains an ordinary Designer any-widget slot.</p>
 */
public final class BlockSemanticsWidgetPropertySchema {
    public static final WidgetTypeId BLOCK_SEMANTICS_TYPE =
            new WidgetTypeId("flutter.widgets.BlockSemantics");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        SEMANTICS("blockSemanticsBehavior", "Semantics",
                "Accessibility semantics blocking for earlier-painted nodes within the same semantics container.");

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

    private BlockSemanticsWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed property in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "blocking", Group.SEMANTICS, "Blocking",
                "Omission preserves true: removes semantics of earlier-painted widgets in the same semantics container, not this widget's descendants. False disables blocking. Layout, painting, and pointer hit testing are unchanged.",
                0);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "BlockSemantics schema must expose exactly "
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
                    "Duplicate BlockSemantics property schema: " + name);
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
