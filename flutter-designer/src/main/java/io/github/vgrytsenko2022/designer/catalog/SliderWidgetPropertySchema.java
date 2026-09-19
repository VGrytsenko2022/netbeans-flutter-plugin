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

/** Complete Flutter 3.44.8 Slider constructors and closed overlay-state projection. */
public final class SliderWidgetPropertySchema {
    public static final WidgetTypeId SLIDER_TYPE = new WidgetTypeId("flutter.material.Slider");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 33;
    public static final int DIRECT_PROPERTY_COUNT = 24;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        VALUE("Value"), BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"),
        ACCESSIBILITY("Accessibility"), OVERLAY_COLOR("OverlayColor");

        private final String suffix;

        Group(String suffix) {
            this.suffix = suffix;
        }

        public String setName() {
            return "slider" + suffix;
        }

        public String displayName() {
            return suffix.replaceAll("([a-z])([A-Z])", "$1 $2");
        }

        public String description() {
            return "Flutter Slider " + displayName().toLowerCase(java.util.Locale.ROOT) + ".";
        }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative Slider property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private SliderWidgetPropertySchema() {
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

    public static List<String> variants() {
        return List.of("standard", "adaptive");
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

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    public static List<String> rangeProperties() {
        return List.of("value", "min", "max", "secondaryTrackValue");
    }

    public static List<String> standardOnlyProperties() {
        return List.of("padding");
    }

    /** The SDK compares effective Dart doubles, including equal signed infinities, without clamping. */
    public static Optional<String> rangeError(WidgetNode node) {
        Objects.requireNonNull(node);
        Double minimum = number(node.properties().get(new PropertyName("min")), 0.0);
        Double maximum = number(node.properties().get(new PropertyName("max")), 1.0);
        Double value = number(node.properties().get(new PropertyName("value")), null);
        if (minimum == null || maximum == null || value == null) {
            return Optional.empty(); // Missing/wrong kinds are rejected by the property contract.
        }
        if (!(minimum <= maximum)) {
            return Optional.of("Slider Minimum (" + minimum + ") must not exceed Maximum (" + maximum + ").");
        }
        if (!(value >= minimum && value <= maximum)) {
            return Optional.of("Slider Value (" + value + ") must be between Minimum (" + minimum
                    + ") and Maximum (" + maximum + "). Values are not automatically clamped.");
        }
        Double secondary = number(node.properties().get(new PropertyName("secondaryTrackValue")), null);
        if (secondary != null && !(secondary >= minimum && secondary <= maximum)) {
            return Optional.of("Slider Secondary track value (" + secondary + ") must be between Minimum ("
                    + minimum + ") and Maximum (" + maximum + "). Values are not automatically clamped.");
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
        if (value instanceof PropertyValue.EnumValue enumeration && enumeration.type().equals("double")) {
            return switch (enumeration.value()) {
                case "infinity" -> Double.POSITIVE_INFINITY;
                case "negativeInfinity" -> Double.NEGATIVE_INFINITY;
                default -> null;
            };
        }
        return null;
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.VALUE, "Required controlled value, created 0. Must remain between the effective Minimum and Maximum. The widget never changes the stored value itself. " + rangeHelp(), 0);
        add(values, "secondaryTrackValue", Group.VALUE, "Optional secondary value within the effective range; explicit null disables the secondary track, like unset. A value below Value is legal and hides the secondary track. Ignored by adaptive Apple rendering. " + rangeHelp(), 1);
        add(values, "onChanged", Group.BEHAVIOR, "Strict non-null ValueChanged<double> reference or zero-argument factory. Enabled without a reference emits a benign no-op; disabled emits null while retaining metadata. Canvas never executes project callbacks.", 2);
        add(values, "onChangeStart", Group.BEHAVIOR, "Strict non-null ValueChanged<double> reference or zero-argument factory. Stored and emitted even when disabled; the SDK controls invocation. Canvas never executes it.", 3);
        add(values, "onChangeEnd", Group.BEHAVIOR, "Strict non-null ValueChanged<double> reference or zero-argument factory. Stored and emitted even when disabled; the SDK controls invocation. Canvas never executes it.", 4);
        add(values, "min", Group.VALUE, "Unset uses SDK minimum 0. Must not exceed Maximum; Value and a non-null Secondary track value must stay in range. Invalid edits are rejected, never clamped. " + rangeHelp(), 5);
        add(values, "max", Group.VALUE, "Unset uses SDK maximum 1. Must not be below Minimum; equal endpoints are constructor-legal. Invalid edits are rejected, never clamped. " + rangeHelp(), 6);
        add(values, "divisions", Group.VALUE, "Positive portable integer (1 through 9007199254740991) or explicit null for continuous sliding. Unset also selects continuous sliding; there is no artificial model tick-count cap.", 7);
        add(values, "label", Group.APPEARANCE, "Optional value-indicator label. Unset displays no value indicator; empty text is retained. Actual visibility follows Show value indicator and the SDK. Ignored by adaptive Apple rendering.", 8);
        add(values, "activeColor", Group.APPEARANCE, "Literal or semantic active-track color. Also participates in Material thumb, overlay and value-indicator fallback; Cupertino uses its actual active-color behavior.", 9);
        add(values, "inactiveColor", Group.APPEARANCE, "Literal or semantic inactive-track color; unset preserves SliderTheme and SDK defaults. Ignored by adaptive Apple rendering.", 10);
        add(values, "secondaryActiveColor", Group.APPEARANCE, "Literal or semantic secondary-track color. Unset preserves SliderTheme and SDK defaults; ignored by adaptive Apple rendering.", 11);
        add(values, "thumbColor", Group.APPEARANCE, "Literal or semantic thumb color. Material falls back through Active color, SliderTheme and defaults; adaptive Apple falls back to Cupertino white.", 12);
        add(values, "overlayColor", Group.OVERLAY_COLOR, "Strict non-null WidgetStateProperty<Color?> reference or zero-argument factory, exclusive with all local overlay entries. A resolved null allows the SDK active-color/theme/default fallback. Ignored by adaptive Apple rendering.", 13);
        add(values, "mouseCursor", Group.BEHAVIOR, "All 41 reviewed cursor constants or a strict MouseCursor reference, including WidgetStateMouseCursor. Ignored by adaptive Apple rendering.", 14);
        add(values, "semanticFormatterCallback", Group.ACCESSIBILITY, "Strict non-null SemanticFormatterCallback reference or zero-argument factory with signature String Function(double). Emitted even when disabled; Canvas never executes it and explicitly previews SDK percentage semantics. Ignored by adaptive Apple rendering.", 15);
        add(values, "focusNode", Group.BEHAVIOR, "Strict non-null FocusNode reference. Canvas uses an isolated local focus owner; adaptive Apple rendering ignores this SDK field.", 16);
        add(values, "autofocus", Group.BEHAVIOR, "Unset uses false. Ignored by adaptive Apple rendering.", 17);
        add(values, "allowedInteraction", Group.BEHAVIOR, "SliderInteraction tapAndSlide, tapOnly, slideOnly or slideThumb. Unset preserves SliderTheme then tapAndSlide; ignored by adaptive Apple rendering.", 18);
        add(values, "padding", Group.LAYOUT, "Standard constructor only: non-negative physical or directional EdgeInsetsGeometry. Unset preserves SliderTheme and SDK layout. Adaptive has no padding argument.", 19);
        add(values, "showValueIndicator", Group.APPEARANCE, "All six ShowValueIndicator members, including deprecated always. Always/onDrag means while dragging, whereas alwaysVisible is persistent. Unset preserves SliderTheme then onlyForDiscrete; ignored by adaptive Apple rendering.", 20);
        add(values, "year2023", Group.APPEARANCE, "Deprecated SDK appearance flag: true, false or explicit null. Unset/null preserves SliderTheme then true. False selects the 2024 M3 appearance; ignored by M2 and adaptive Apple rendering.", 21);
        add(values, "variant", Group.BEHAVIOR, "Required constructor selector, created Standard. Both constructors are const-capable. Adaptive uses CupertinoSlider on iOS/macOS and Material Slider otherwise; selecting Adaptive requires resetting Padding.", 22);
        add(values, "enabled", Group.BEHAVIOR, "Required activation policy, created true. False emits only onChanged:null while preserving all callback metadata; Start/End and semantic formatter remain source-visible.", 23);
        int order = DIRECT_PROPERTY_COUNT;
        for (String name : overlayColorStateProperties()) {
            add(values, name, Group.OVERLAY_COLOR, "Literal color, semantic role or explicit null. Presence-aware priority: disabled, error, dragged, pressed, selected, scrolledUnder, hovered, focused, default. Explicit null stops lower local entries and lets the SDK fallback resolve; no implicit disabled entry. Actual Material states are disabled, hovered, focused and dragged. Ignored by adaptive Apple rendering.", order++);
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new IllegalStateException("Incomplete Slider constructor schema");
        }
        return Collections.unmodifiableMap(values);
    }

    private static String rangeHelp() {
        return "Signed finite numbers and explicit positive/negative Infinity are preserved. Contextual SDK normalization or layout failures are diagnosed in Canvas without changing the model or generated source.";
    }

    private static void add(Map<String, Definition> values, String name, Group group, String help, int order) {
        String display = name.equals("variant") ? "Constructor"
                : Character.toUpperCase(name.charAt(0)) + name.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
        if (values.put(name, new Definition(group, display, help, name, order)) != null) {
            throw new IllegalStateException("Duplicate Slider property " + name);
        }
    }
}
