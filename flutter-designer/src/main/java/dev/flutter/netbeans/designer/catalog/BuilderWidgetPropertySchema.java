package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 Builder constructor contract. */
public final class BuilderWidgetPropertySchema {
    public static final WidgetTypeId BUILDER_TYPE = new WidgetTypeId("flutter.widgets.Builder");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 1;
    public static final int CALLBACK_PROPERTY_COUNT = 1;
    public static final String CALLBACK_TYPE = "WidgetBuilder";

    public enum Group {
        BUILDER("builder", "Builder", "The required BuildContext-to-Widget callback used to create the subtree.");

        private final String id;
        private final String displayName;
        private final String description;

        Group(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String id() { return id; }
        public String setName() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description,
            String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Builder property order must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private BuilderWidgetPropertySchema() { }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return Optional.ofNullable(DEFINITIONS.get(name.value())); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }

    private static Map<String, Definition> createDefinitions() {
        var result = new LinkedHashMap<String, Definition>();
        result.put("builder", new Definition(Group.BUILDER, "Builder",
                "Required WidgetBuilder callback receiving BuildContext and returning the subtree. "
                        + "The Designer starts with a no-op preview callback; project code is retained and analyzed "
                        + "but never executed by the isolated Canvas.", "builder", 0));
        if (result.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Builder catalog/property schema count mismatch");
        }
        return Collections.unmodifiableMap(result);
    }
}
