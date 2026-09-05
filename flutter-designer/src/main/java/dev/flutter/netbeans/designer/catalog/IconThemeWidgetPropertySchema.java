package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 IconThemeData projection and IconTheme merge choice. */
public final class IconThemeWidgetPropertySchema {
    public static final WidgetTypeId ICON_THEME_TYPE = new WidgetTypeId("flutter.widgets.IconTheme");
    /** Nine optional SDK data fields plus one required Designer construction choice. */
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 10;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        APPEARANCE("iconThemeAppearance", "Appearance", "Inherited icon size, color, opacity and shadows."),
        VARIABLE_FONT("iconThemeVariableFont", "Variable font", "Inherited variable-font axes within render-safe limits."),
        BEHAVIOR("iconThemeBehavior", "Behavior", "Text scaling and the explicit direct or merging construction choice.");
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
    private IconThemeWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "size", Group.APPEARANCE, "Size",
                "Non-negative logical icon size. Unset inherits the nearest field in Merge mode; direct mode uses the SDK fallback 24. Descendant Icon's explicit size takes precedence.", 0);
        add(values, "fill", Group.VARIABLE_FONT, "Fill",
                "Variable-font FILL axis from 0 through 1. Unset inherits in Merge mode or falls back to 0 in direct mode. Descendant Icon's explicit fill takes precedence.", 1);
        add(values, "weight", Group.VARIABLE_FONT, "Weight axis",
                "Variable-font wght axis greater than 0 and less than 32768. Unset inherits in Merge mode or falls back to 400. A descendant Icon's explicit weight overrides this default.", 2);
        add(values, "grade", Group.VARIABLE_FONT, "Grade axis",
                "Variable-font GRAD axis from -32768 inclusive to 32768 exclusive. Unset inherits in Merge mode or falls back to 0.", 3);
        add(values, "opticalSize", Group.VARIABLE_FONT, "Optical size axis",
                "Variable-font opsz axis greater than 0 and less than 32768. Unset inherits in Merge mode or falls back to 48.", 4);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB icon color or reviewed Material ColorScheme role. Unset inherits in Merge mode or falls back to black in direct mode. Descendant Icon's explicit color takes precedence but still receives the theme opacity.", 5);
        add(values, "opacity", Group.APPEARANCE, "Opacity",
                "Finite opacity applied to explicit and inherited icon color alpha. Flutter clamps the effective value to 0 through 1; the entered value is retained. Unset inherits in Merge mode or falls back to 1. This does not apply an Opacity wrapper to children or shadows.", 6);
        add(values, "shadows", Group.APPEARANCE, "Shadows",
                "Ordered typed shadows. Unset inherits in Merge mode or leaves shadows null in direct mode. An explicit empty list suppresses inherited shadows. A descendant Icon's explicit list takes precedence.", 7);
        add(values, "applyTextScaling", Group.BEHAVIOR, "Apply text scaling",
                "Whether MediaQuery text scaling scales icon size. Unset inherits in Merge mode or falls back to false. Explicit false is retained; a descendant Icon's explicit setting takes precedence.", 8);
        add(values, "merge", Group.BEHAVIOR, "Merge inherited theme",
                "Required Designer-only mode, created as false. False uses the const-capable IconTheme constructor with required IconThemeData, even when all data fields are unset; missing fields use framework fallbacks rather than the outer theme. True calls non-const IconTheme.merge and inherits each unset field. This choice cannot be reset and is never emitted as a Dart argument.", 10);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("IconTheme schema/property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate IconTheme property schema: " + name);
        }
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
