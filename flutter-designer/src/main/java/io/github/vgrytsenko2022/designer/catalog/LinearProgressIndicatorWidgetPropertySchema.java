package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Full pinned Flutter 3.44.8 LinearProgressIndicator constructor contract. */
public final class LinearProgressIndicatorWidgetPropertySchema {
    public static final WidgetTypeId LINEAR_PROGRESS_INDICATOR_TYPE =
            new WidgetTypeId("flutter.material.LinearProgressIndicator");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 13;
    public static final int SLOT_COUNT = 0;
    public static final PropertyValue.EnumValue POSITIVE_INFINITY =
            new PropertyValue.EnumValue("double", "infinity");

    public enum Group {
        PROGRESS("linearProgressIndicatorProgress", "Progress", "Determinate value or indeterminate controller."),
        APPEARANCE("linearProgressIndicatorAppearance", "Appearance", "Local and inherited progress painting."),
        ACCESSIBILITY("linearProgressIndicatorAccessibility", "Accessibility", "Progress semantics for assistive technology.");

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
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("Negative LinearProgressIndicator property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private LinearProgressIndicatorWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static boolean supportsInfinity(PropertyName name) {
        return switch (Objects.requireNonNull(name, "name").value()) {
            case "minHeight", "stopIndicatorRadius", "trackGap" -> true;
            default -> false;
        };
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.PROGRESS, "Value",
                "Finite progress value. Flutter clamps out-of-range numbers to 0..1 without rewriting the stored value. Unset selects indeterminate animation. Setting Value clears Controller atomically.", 0);
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal ARGB or reviewed semantic color. Unset uses ProgressIndicatorTheme.linearTrackColor, then the active Material version's track color.", 1);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB or reviewed semantic color. Used when Value color is unset or its animation currently yields null, then ProgressIndicatorTheme.color and ColorScheme.primary.", 2);
        add(values, "valueColor", Group.APPEARANCE, "Value color",
                "Literal/theme color wrapped in AlwaysStoppedAnimation<Color>, explicit null animation wrapped in AlwaysStoppedAnimation<Color?>(null), or a typed Animation<Color?> project reference. Unset omits the animation. Project references require strict non-null Animation<Color?> type proof; dynamic or nullable outer types are rejected. Project animation preview is explicitly unavailable in the isolated Canvas.", 3);
        add(values, "minHeight", Group.APPEARANCE, "Minimum height",
                "Strictly positive number or Infinity. Unset uses ProgressIndicatorTheme.linearMinHeight, then 4 logical pixels. Infinity requires bounded parent height; all indicators need bounded width.", 4);
        add(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label",
                "Accessible label describing the purpose of this progress indicator.", 5);
        add(values, "semanticsValue", Group.ACCESSIBILITY, "Semantics value",
                "Accessible progress value override. For determinate progress, use a number or percentage in 0..100 such as '45' or '45%'; the SDK validates this at runtime when semantics are enabled. Indeterminate loadingSpinner permits free text. Unset determinate semantics use the clamped integer percentage without a percent sign, with progressBar role. Canvas reports incompatible context-dependent semantics without rewriting the stored text.", 6);
        add(values, "borderRadius", Group.APPEARANCE, "Border radius",
                "Physical or directional elliptical corners for active indicator and track. Unset preserves ProgressIndicatorTheme and version-specific defaults; indeterminate painting is clipped with the resolved radius.", 7);
        add(values, "stopIndicatorColor", Group.APPEARANCE, "Stop indicator color",
                "Literal/theme stop color, used with the newer appearance. The pinned SDK can also honor explicit stop settings under Material 2 when Year 2023 is false. Missing effective stop color in that combination is an SDK preview limitation, not an invented model dependency.", 8);
        add(values, "stopIndicatorRadius", Group.APPEARANCE, "Stop indicator radius",
                "Signed finite number or Infinity. Values at or below zero hide the stop; positive values are capped to half the actual line height by Flutter. Unset inherits theme/default radius. Stop rendering depends on appearance and determinate mode.", 9);
        add(values, "trackGap", Group.APPEARANCE, "Track gap",
                "Signed finite number or Infinity. Zero/nonpositive values remove the determinate gap; indeterminate painting retains the SDK's signed-gap behavior. Positive Infinity suppresses the track. Unset inherits the theme/default gap.", 10);
        add(values, "year2023", Group.APPEARANCE, "Year 2023 appearance",
                "Optional deprecated SDK appearance flag. Unset uses ProgressIndicatorTheme.year2023 then true in Flutter 3.44.8; false opts into the 2024 appearance. Explicit false is preserved, not reset to true.", 11);
        add(values, "controller", Group.PROGRESS, "Controller",
                "Typed AnimationController project reference for indeterminate animation. Setting Controller clears Value atomically. Unset preserves theme controller/internal 1800 ms animation. The isolated Canvas never executes project controllers and explicitly marks their preview unavailable.", 12);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("LinearProgressIndicator property count mismatch");
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group, String display, String description, int order) {
        if (values.put(name, new Definition(group, display, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate LinearProgressIndicator property: " + name);
        }
    }
}
