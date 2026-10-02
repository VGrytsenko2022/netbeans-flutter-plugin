package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Enterprise Properties presentation contract for Flutter {@code Icon}. */
public final class IconWidgetPropertySchema {
    public static final WidgetTypeId ICON_TYPE =
            new WidgetTypeId("flutter.widgets.Icon");

    public enum Group {
        DATA("iconData", "Icon data",
                "Nullable const IconData metadata; no arbitrary Dart expressions."),
        APPEARANCE("iconAppearance", "Appearance",
                "Logical size, semantic theme-aware color, shadows, and compositing."),
        VARIABLE_FONT("iconVariableFont", "Variable font",
                "Variable-font axes and fallback FontWeight."),
        ACCESSIBILITY("iconAccessibility", "Accessibility and direction",
                "Assistive label, text direction, and text scaling behavior.");

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

    public record Definition(
            Group group,
            String displayName,
            String description) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private IconWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "icon", Group.DATA, "Icon data",
                "Select None or a glyph from the bundled Flutter 3.44.8 Material Icons registry. "
                + "The project must enable flutter.uses-material-design: true.");
        add(values, "size", Group.APPEARANCE, "Size",
                "Square icon size in logical pixels; unset inherits IconTheme.");
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB color or semantic Material ColorScheme role; unset inherits IconTheme.");
        add(values, "shadows", Group.APPEARANCE, "Shadows",
                "Ordered shadows; unset inherits IconTheme. An explicit empty list suppresses "
                + "inherited shadows.");
        add(values, "blendMode", Group.APPEARANCE, "Blend mode",
                "Foreground compositing mode; unset uses Flutter's default srcOver behavior. "
                + "This value is not inherited from IconTheme.");
        add(values, "fill", Group.VARIABLE_FONT, "Fill",
                "Variable-font FILL axis in the inclusive range 0 through 1; unset inherits "
                + "IconTheme.");
        add(values, "weight", Group.VARIABLE_FONT, "Weight axis",
                "Variable-font wght axis greater than 0 and less than 32768. "
                + "Unset inherits IconTheme; when explicit it overrides Font weight.");
        add(values, "grade", Group.VARIABLE_FONT, "Grade axis",
                "Variable-font GRAD axis from -32768 inclusive to 32768 exclusive; unset "
                + "inherits IconTheme.");
        add(values, "opticalSize", Group.VARIABLE_FONT, "Optical size axis",
                "Variable-font opsz axis greater than 0 and less than 32768; unset inherits "
                + "IconTheme.");
        add(values, "fontWeight", Group.VARIABLE_FONT, "Font weight",
                "Typeface FontWeight w100 through w900; unset leaves TextStyle.fontWeight null. "
                + "This value is not inherited from IconTheme, and an explicit Weight axis "
                + "overrides it while rendering a variable font.");
        add(values, "semanticLabel", Group.ACCESSIBILITY, "Semantic label",
                "Alternative spoken label announced by assistive technologies.");
        add(values, "textDirection", Group.ACCESSIBILITY, "Text direction",
                "Explicit direction used for icons that match text direction.");
        add(values, "applyTextScaling", Group.ACCESSIBILITY, "Apply text scaling",
                "Whether MediaQuery text scaling also scales the icon; unset inherits IconTheme, "
                + "then falls back to false.");
        return Map.copyOf(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description) {
        if (values.putIfAbsent(name,
                new Definition(group, displayName, description)) != null) {
            throw new IllegalStateException("Duplicate Icon property schema: " + name);
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
