package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 material/adaptive circular progress constructors. */
public final class CircularProgressIndicatorWidgetPropertySchema {
    public static final WidgetTypeId CIRCULAR_PROGRESS_INDICATOR_TYPE =
            new WidgetTypeId("flutter.material.CircularProgressIndicator");
    /** Fourteen SDK fields and one required Designer constructor selector. */
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 15;
    public static final int SLOT_COUNT = 0;
    public static final PropertyValue.EnumValue POSITIVE_INFINITY =
            new PropertyValue.EnumValue("double", "infinity");

    public enum Group {
        PROGRESS("circularProgressIndicatorProgress", "Progress", "Constructor and progress animation."),
        APPEARANCE("circularProgressIndicatorAppearance", "Appearance", "Local and inherited progress painting."),
        LAYOUT("circularProgressIndicatorLayout", "Layout", "Material track constraints and padding."),
        ACCESSIBILITY("circularProgressIndicatorAccessibility", "Accessibility", "Material progress semantics.");

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
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative CircularProgressIndicator property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private CircularProgressIndicatorWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }

    public static boolean supportsInfinity(PropertyName name) {
        return Objects.requireNonNull(name, "name").value().equals("trackGap");
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.PROGRESS, "Value",
                "Finite progress retained without rewriting. Flutter clamps the display to 0..1. Unset selects indeterminate animation. Setting Value clears Controller atomically.", 0);
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal ARGB or reviewed theme role. Material track color falls through ProgressIndicatorTheme.circularTrackColor and SDK defaults. Adaptive Apple platforms use this as the Cupertino tick color.", 1);
        add(values, "color", Group.APPEARANCE, "Color",
                "Material-only literal/theme indicator color, used when Value color is unset or yields null. Setting Color in Adaptive mode switches to Material atomically; Adaptive has no color constructor argument.", 2);
        add(values, "valueColor", Group.APPEARANCE, "Value color",
                "Stopped literal/theme color, explicit stopped null color, or a project reference with strict non-null Animation<Color?> type proof. Dynamic or nullable outer references are rejected. Isolated Canvas does not execute project animations. Adaptive Apple platforms ignore this field.", 3);
        add(values, "strokeWidth", Group.APPEARANCE, "Stroke width",
                "Signed finite Material arc stroke width. Flutter admits negative values; zero uses hairline behavior. Unset inherits theme/SDK defaults. Resolved paint geometry overflow is reported by Canvas, not clamped. Ignored on adaptive Apple platforms.", 4);
        add(values, "strokeAlign", Group.APPEARANCE, "Stroke alignment",
                "Signed relative stroke alignment: -1 inside, 0 centered, 1 outside. Values beyond -1..1 are permitted by Flutter and retained. Unset inherits the theme/version default. Adaptive Apple platforms ignore this field.", 5);
        add(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label",
                "Accessible purpose of the Material progress indicator. Adaptive Apple platforms use the Cupertino widget and ignore this field.", 6);
        add(values, "semanticsValue", Group.ACCESSIBILITY, "Semantics value",
                "Material accessible value override. Determinate progress requires a number/percentage in 0..100 such as '45' or '45%'; indeterminate loadingSpinner allows free text. Canvas reports invalid resolved semantics without rewriting text. Ignored on adaptive Apple platforms.", 7);
        add(values, "strokeCap", Group.APPEARANCE, "Stroke cap",
                "Butt, round or square arc endings. Unset preserves the SDK's appearance/mode-specific defaults and theme. Ignored on adaptive Apple platforms.", 8);
        add(values, "constraints", Group.LAYOUT, "Constraints",
                "Complete BoxConstraints min/max dimensions including unbounded maximums. Unset inherits theme and SDK defaults; parent constraints still apply. Ignored on adaptive Apple platforms.", 9);
        add(values, "trackGap", Group.APPEARANCE, "Track gap",
                "Signed finite gap or positive Infinity between active arc and background track. Nonpositive values remove the gap. Unset inherits theme/defaults; Year 2023 true suppresses it. Infinity retains SDK behavior, including suppression of the gapped track. Ignored on adaptive Apple platforms.", 10);
        add(values, "year2023", Group.APPEARANCE, "Year 2023 appearance",
                "Optional deprecated SDK appearance flag. Unset inherits ProgressIndicatorTheme.year2023 then true; false requests the newer appearance. Explicit false is retained. Ignored on adaptive Apple platforms.", 11);
        add(values, "padding", Group.LAYOUT, "Padding",
                "Physical or directional nonnegative padding around the Material track. Unset inherits ProgressIndicatorTheme.circularTrackPadding and SDK defaults. Ignored on adaptive Apple platforms.", 12);
        add(values, "controller", Group.PROGRESS, "Controller",
                "Project AnimationController reference with strict non-null type proof. Setting Controller clears Value atomically. Isolated Canvas does not execute project controllers. Adaptive Apple platforms ignore this field; the material path uses theme/internal animation when unset.", 13);
        add(values, "variant", Group.PROGRESS, "Constructor",
                "Required Designer selector: Material or Adaptive. Adaptive uses CupertinoActivityIndicator on iOS/macOS and the material indicator elsewhere. Only Value and Background color affect its Apple branch. Switching to Adaptive clears Color atomically; this selector is never emitted as a Dart argument and cannot be reset.", 14);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("CircularProgressIndicator property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String display, String description, int order) {
        if (values.put(name, new Definition(group, display, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate CircularProgressIndicator property: " + name);
        }
    }
}
