package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 RangeSlider constructor and closed state projections. */
public final class RangeSliderWidgetPropertySchema {
    public static final WidgetTypeId RANGE_SLIDER_TYPE = new WidgetTypeId("flutter.material.RangeSlider");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 37;
    public static final int DIRECT_PROPERTY_COUNT = 19;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        VALUE("Value"), BEHAVIOR("Behavior"), LABELS("Labels"), APPEARANCE("Appearance"),
        LAYOUT("Layout"), ACCESSIBILITY("Accessibility"), OVERLAY_COLOR("OverlayColor"), MOUSE_CURSOR("MouseCursor");

        private final String suffix;

        Group(String suffix) {
            this.suffix = suffix;
        }

        public String setName() {
            return "rangeSlider" + suffix;
        }

        public String displayName() {
            return suffix.replaceAll("([a-z])([A-Z])", "$1 $2");
        }

        public String description() {
            return "Flutter RangeSlider " + displayName().toLowerCase(java.util.Locale.ROOT) + ".";
        }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative RangeSlider property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private RangeSliderWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name).value());
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name)));
    }

    public static List<String> statePrefixes() {
        return CheckboxWidgetPropertySchema.statePrefixes();
    }

    /** First present matching entry wins, including explicit null; default is last. */
    public static List<String> statePriority() {
        return CheckboxWidgetPropertySchema.statePriority();
    }

    public static List<String> overlayColorStateProperties() {
        return statePrefixes().stream().map(state -> "overlayColor" + state).toList();
    }

    public static List<String> mouseCursorStateProperties() {
        return statePrefixes().stream().map(state -> "mouseCursor" + state).toList();
    }

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    public static List<String> rangeProperties() {
        return List.of("valuesStart", "valuesEnd", "min", "max");
    }

    public static List<String> labelProperties() {
        return List.of("labelsStart", "labelsEnd");
    }

    /** Compare actual Dart doubles, including equal signed infinities, without clamping. */
    public static Optional<String> rangeError(WidgetNode node) {
        Objects.requireNonNull(node);
        Double minimum = number(node.properties().get(new PropertyName("min")), 0.0);
        Double maximum = number(node.properties().get(new PropertyName("max")), 1.0);
        Double start = number(node.properties().get(new PropertyName("valuesStart")), null);
        Double end = number(node.properties().get(new PropertyName("valuesEnd")), null);
        if (minimum == null || maximum == null || start == null || end == null) {
            return Optional.empty(); // Missing/wrong kinds are rejected by the property contract.
        }
        if (!(minimum <= maximum)) {
            return Optional.of("RangeSlider Minimum (" + minimum + ") must not exceed Maximum (" + maximum + ").");
        }
        if (!(start <= end)) {
            return Optional.of("RangeSlider Start (" + start + ") must not exceed End (" + end + "). Values are not automatically clamped.");
        }
        if (!(start >= minimum && start <= maximum && end >= minimum && end <= maximum)) {
            return Optional.of("RangeSlider Start (" + start + ") and End (" + end
                    + ") must be between Minimum (" + minimum + ") and Maximum (" + maximum + "). Values are not automatically clamped.");
        }
        return Optional.empty();
    }

    private static Double number(PropertyValue value, Double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof PropertyValue.IntegerValue integer) {
            return integer.value().doubleValue();
        }
        if (value instanceof PropertyValue.DoubleValue decimal) {
            return decimal.value().doubleValue();
        }
        if (value instanceof PropertyValue.EnumValue member && member.type().equals("double")) {
            return switch (member.value()) {
                case "infinity" -> Double.POSITIVE_INFINITY;
                case "negativeInfinity" -> Double.NEGATIVE_INFINITY;
                default -> null;
            };
        }
        return null;
    }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        add(values, "valuesStart", Group.VALUE, "Required RangeValues.start. Must not exceed End and must remain inside Minimum/Maximum. Finite Dart numbers and signed infinity are preserved without clamping.", 0);
        add(values, "valuesEnd", Group.VALUE, "Required RangeValues.end. Must not precede Start and must remain inside Minimum/Maximum. Equal ranges are constructor-legal; resolved SDK preview limits are reported separately.", 1);
        add(values, "onChanged", Group.BEHAVIOR, "Strict non-null ValueChanged<RangeValues> project reference or zero-argument factory. Enabled with no reference emits a benign no-op; disabled suppresses this argument only while retaining the stored reference.", 2);
        add(values, "onChangeStart", Group.BEHAVIOR, "Strict non-null ValueChanged<RangeValues> reference or zero-argument factory. Preserved and type-proved even when Enabled is false; project code never executes in isolated Canvas.", 3);
        add(values, "onChangeEnd", Group.BEHAVIOR, "Strict non-null ValueChanged<RangeValues> reference or zero-argument factory. Preserved and type-proved even when Enabled is false; project code never executes in isolated Canvas.", 4);
        add(values, "min", Group.VALUE, "Minimum bound; unset preserves SDK 0. Must not exceed Maximum. Exact constructor comparisons admit finite values and signed infinity.", 5);
        add(values, "max", Group.VALUE, "Maximum bound; unset preserves SDK 1. Must not precede Minimum. Exact constructor comparisons admit finite values and signed infinity.", 6);
        add(values, "divisions", Group.VALUE, "Positive portable integer up to 9007199254740991; unset or explicit null is continuous. No model tick-count cap; isolated Canvas may report a resolved-theme paint-budget limitation.", 7);
        add(values, "labels", Group.LABELS, "Explicit null or strict non-null RangeLabels project reference/zero-argument factory. Mutually exclusive with local Start/End labels. Canvas does not execute project label providers.", 8);
        add(values, "labelsStart", Group.LABELS, "Local RangeLabels.start. Either local label creates RangeLabels; an omitted peer becomes an empty string. Explicit empty text is preserved. Excludes whole Labels.", 9);
        add(values, "labelsEnd", Group.LABELS, "Local RangeLabels.end. Either local label creates RangeLabels; an omitted peer becomes an empty string. Explicit empty text is preserved. Excludes whole Labels.", 10);
        add(values, "activeColor", Group.APPEARANCE, "Literal or theme-token active color; unset preserves the actual SliderTheme/SDK fallback.", 11);
        add(values, "inactiveColor", Group.APPEARANCE, "Literal or theme-token inactive color; unset preserves the actual SliderTheme/SDK fallback.", 12);
        add(values, "overlayColor", Group.OVERLAY_COLOR, "Strict non-null WidgetStateProperty<Color?> project reference or zero-argument factory. Excludes all local overlay entries. Dynamic and nullable outer types are rejected.", 13);
        add(values, "mouseCursor", Group.MOUSE_CURSOR, "Strict non-null WidgetStateProperty<MouseCursor?> reference or zero-argument factory, not a plain MouseCursor. Excludes all local cursor entries.", 14);
        add(values, "semanticFormatterCallback", Group.ACCESSIBILITY, "Strict non-null SemanticFormatterCallback reference or zero-argument factory. SDK invokes it for each thumb even when disabled; isolated Canvas uses an explicitly diagnosed default-percentage approximation.", 15);
        add(values, "padding", Group.LAYOUT, "Nonnegative physical or directional padding; unset preserves SliderTheme.padding. Finite-side sum overflow is a contextual layout limitation, not an extra stored-value bound.", 16);
        add(values, "year2023", Group.APPEARANCE, "Deprecated nullable appearance flag. Unset/null preserves SliderTheme then true. Only Material 3 with false selects the 2024 RangeSlider defaults; all other cases use SDK legacy defaults.", 17);
        add(values, "enabled", Group.BEHAVIOR, "Required designer activation selector, initially true. False emits onChanged:null without deleting the stored reference; other callbacks remain source-visible.", 18);
        int order = DIRECT_PROPERTY_COUNT;
        for (String state : statePrefixes()) {
            add(values, "overlayColor" + state, Group.OVERLAY_COLOR, "Local " + state + " overlay entry. Literal/theme color or explicit null. First matching present entry wins; null stops local fallback and lets the SDK resolve its theme/default. Excludes whole Overlay color.", order++);
        }
        for (String state : statePrefixes()) {
            add(values, "mouseCursor" + state, Group.MOUSE_CURSOR, "Local " + state + " cursor entry: reviewed preset, strict non-null MouseCursor reference/factory, or explicit null. First matching present entry wins; null stops local fallback and lets the SDK resolve its theme/default. Excludes whole Mouse cursor.", order++);
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new IllegalStateException("RangeSlider property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group, String description, int order) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, Character.toUpperCase(label.charAt(0)) + label.substring(1), description, name, order));
    }
}
