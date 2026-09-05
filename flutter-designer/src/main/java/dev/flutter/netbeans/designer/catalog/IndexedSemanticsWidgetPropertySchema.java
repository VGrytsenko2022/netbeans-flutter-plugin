package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code IndexedSemantics} in Flutter 3.44.8.
 *
 * <p>The framework {@code key} remains Designer-managed. The required signed
 * {@code index} uses the shared cross-target integer range; its prototype value
 * is zero. The optional child is an ordinary Designer any-widget slot.</p>
 */
public final class IndexedSemanticsWidgetPropertySchema {
    public static final WidgetTypeId INDEXED_SEMANTICS_TYPE =
            new WidgetTypeId("flutter.widgets.IndexedSemantics");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        SEMANTICS("indexedSemanticsBehavior", "Semantics",
                "Accessibility index annotation for the first child semantics node.");

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

    private IndexedSemanticsWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Returns the reviewed property in Flutter constructor order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "index", Group.SEMANTICS, "Index",
                "Required signed integer from -9007199254740991 to 9007199254740991. Annotates the first child semantics node; negative indexes are supported by Flutter. Zero is a Designer prototype value, not an SDK default. Cannot be unset. Layout, painting, and pointer hit testing are unchanged.",
                0);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "IndexedSemantics schema must expose exactly "
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
                    "Duplicate IndexedSemantics property schema: " + name);
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
