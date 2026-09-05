package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Flutter 3.44.8 {@code ClipPath} constructor and {@code ClipPath.shape}
 * static-helper projection. The framework key is excluded. The mutually
 * exclusive clipper and shape properties use the closed Dart object reference
 * contract; configured objects can be supplied through a project getter/factory.
 * Omission selects the unnamed constructor's rectangular default. Setting shape
 * selects the non-const static helper, preserving its inherited text direction.
 */
public final class ClipPathWidgetPropertySchema {
    public static final WidgetTypeId CLIP_PATH_TYPE =
            new WidgetTypeId("flutter.widgets.ClipPath");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 3;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        DELEGATE("clipPathDelegate", "Delegate", "Project-declared path clip delegate."),
        SHAPE("clipPathShape", "Shape", "ShapeBorder-based ClipPath.shape alternative."),
        CLIPPING("clipPathClipping", "Clipping", "Path paint clipping behavior.");

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

    public record Definition(Group group, String displayName, String description,
            String dartName, int dartOrder) {
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

    private ClipPathWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    /** Union of public constructor/helper properties, excluding key and child. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "clipper", Group.DELEGATE, "Clipper",
                "Project Dart reference or zero-argument invocation assignable to CustomClipper<Path>. "
                + "Clipper and Shape are mutually exclusive. Setting Clipper in Properties "
                + "atomically clears Shape and selects the unnamed constructor.", 0);
        add(values, "shape", Group.SHAPE, "Shape",
                "Project/package Dart reference or zero-argument invocation assignable to ShapeBorder. "
                + "Selects the non-const ClipPath.shape helper. Setting Shape in Properties "
                + "atomically clears the mutually exclusive Clipper. "
                + "Configured shapes are supplied through a project getter or factory.", 1);
        add(values, "clipBehavior", Group.CLIPPING, "Clip behavior",
                "How paint outside the path is clipped; omission preserves Clip.antiAlias.", 2);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ClipPath schema property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int dartOrder) {
        if (values.putIfAbsent(name,
                new Definition(group, displayName, description, name, dartOrder)) != null) {
            throw new IllegalStateException("Duplicate ClipPath property schema: " + name);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) { throw new IllegalArgumentException(name + " must not be blank"); }
        return value;
    }
}
