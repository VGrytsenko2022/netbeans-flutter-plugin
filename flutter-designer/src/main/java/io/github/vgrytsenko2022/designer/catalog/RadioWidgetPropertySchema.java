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

/** Complete Flutter 3.44.8 Radio constructor union with a closed, explicit generic type. */
public final class RadioWidgetPropertySchema {
    public static final WidgetTypeId RADIO_TYPE = new WidgetTypeId("flutter.material.Radio");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 107;
    public static final int DIRECT_PROPERTY_COUNT = 26;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"),
        FILL_COLOR("FillColor"), OVERLAY_COLOR("OverlayColor"), BACKGROUND_COLOR("BackgroundColor"),
        INNER_RADIUS("InnerRadius"), SIDE("Side");

        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "radio" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter Radio " + displayName() + "."; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative Radio property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private RadioWidgetPropertySchema() {}
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> variants() { return List.of("standard", "adaptive"); }
    public static List<String> valueTypes() { return List.of("String", "int", "double", "num", "bool", "Object"); }
    public static List<String> mouseCursorPresets() { return CheckboxWidgetPropertySchema.mouseCursorPresets(); }
    public static List<String> statePrefixes() { return CheckboxWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return CheckboxWidgetPropertySchema.statePriority(); }
    public static List<String> sideStates() { return CheckboxWidgetPropertySchema.sideStates(); }
    public static List<String> sideBaseProperties() { return CheckboxWidgetPropertySchema.sideBaseProperties(); }
    public static List<String> sideBucketProperties(String state) { return CheckboxWidgetPropertySchema.sideBucketProperties(state); }
    public static List<String> sideStateProperties() { return CheckboxWidgetPropertySchema.sideStateProperties(); }
    public static List<String> sideLocalProperties() { return CheckboxWidgetPropertySchema.sideLocalProperties(); }
    public static List<String> colorStateProperties(String family) {
        if (!List.of("fillColor", "overlayColor", "backgroundColor").contains(family)) {
            throw new IllegalArgumentException("Not a Radio color family: " + family);
        }
        return statePrefixes().stream().map(state -> family + state).toList();
    }
    public static List<String> innerRadiusStateProperties() {
        return statePrefixes().stream().map(state -> "innerRadius" + state).toList();
    }
    public static boolean nullableValueType(WidgetNode node) {
        return new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("nullableValueType")));
    }
    public static boolean isTypeReference(PropertyValue value) {
        return value instanceof PropertyValue.DartObjectReferenceValue reference
                && reference.access() == PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                && reference.member().isEmpty() && reference.constant().isEmpty();
    }
    /** Pure prospective-edit check; project aliases and references still require exact analyzer proof. */
    public static Optional<String> valueTypeError(WidgetNode node) {
        PropertyValue type = node.properties().get(new PropertyName("valueType"));
        if (type instanceof PropertyValue.DartObjectReferenceValue && !isTypeReference(type)) {
            return Optional.of("Radio Value Type requires a simple class, enum or typedef reference: no member, invocation or constness.");
        }
        for (String field : List.of("value", "groupValue")) {
            PropertyValue value = node.properties().get(new PropertyName(field));
            if (value == null) continue;
            if (value instanceof PropertyValue.NullValue) {
                if (field.equals("value") && !nullableValueType(node)) return Optional.of("Radio null Value requires Nullable Value Type true.");
                continue;
            }
            if (value instanceof PropertyValue.DartObjectReferenceValue || !(type instanceof PropertyValue.StringValue builtin)) continue;
            boolean accepted = switch (builtin.value()) {
                case "Object" -> true;
                case "String" -> value instanceof PropertyValue.StringValue;
                case "bool" -> value instanceof PropertyValue.BooleanValue;
                case "int" -> value instanceof PropertyValue.IntegerValue;
                case "double", "num" -> value instanceof PropertyValue.IntegerValue || value instanceof PropertyValue.DoubleValue
                        || value instanceof PropertyValue.EnumValue numeric && numeric.type().equals("double")
                        && List.of("infinity", "negativeInfinity", "nan").contains(numeric.value());
                default -> false;
            };
            if (!accepted) return Optional.of("Radio " + field + " literal does not match selected type " + builtin.value() + ". Change the type and value together; values are not coerced or discarded.");
        }
        return Optional.empty();
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "value", Group.BEHAVIOR, "Required controlled value, created 'option'. Its literal or project reference must match the selected value type; null requires a nullable value type. Canvas never executes project references.");
        add(values, "groupValue", Group.BEHAVIOR, "Legacy nullable selected group value, checked against the selected type. Omission preserves RadioGroup/registry behavior; explicit null is retained. Deprecated by the SDK in favor of RadioGroup.");
        add(values, "onChanged", Group.BEHAVIOR, "Optional No-op preset, explicit null, or strictly typed ValueChanged<T?> project reference. Creation stores No-op; omission truly omits the callback. Deprecated by the SDK in favor of RadioGroup. Project callbacks are never executed in Canvas.");
        add(values, "mouseCursor", Group.BEHAVIOR, "All reviewed cursor presets or a strict MouseCursor reference, including WidgetStateMouseCursor. Adaptive Apple forwards this property.");
        add(values, "toggleable", Group.BEHAVIOR, "Unset is false. True permits tapping the selected radio to request null without mutating the stored value.");
        add(values, "activeColor", Group.APPEARANCE, "Literal or semantic active color; actual RadioTheme and M2/M3 or Cupertino defaults remain authoritative.");
        add(values, "fillColor", Group.FILL_COLOR, colorReferenceHelp("fillColor"));
        add(values, "focusColor", Group.APPEARANCE, "Literal or semantic focus color; adaptive Apple also accepts this property.");
        add(values, "hoverColor", Group.APPEARANCE, "Literal or semantic hover color. Ignored by the adaptive Apple branch.");
        add(values, "overlayColor", Group.OVERLAY_COLOR, colorReferenceHelp("overlayColor"));
        add(values, "splashRadius", Group.APPEARANCE, "Signed finite reaction radius or either infinity. Zero and negative values suppress the reaction; ignored by adaptive Apple.");
        add(values, "materialTapTargetSize", Group.LAYOUT, "Padded or shrinkWrap. Unset inherits RadioTheme/defaults; ignored by adaptive Apple.");
        add(values, "visualDensity", Group.LAYOUT, "Strict VisualDensity project reference, mutually exclusive with the two local axes. Ignored by adaptive Apple.");
        add(values, "visualDensityHorizontal", Group.LAYOUT, "Finite density from -4 to 4. The missing peer axis is zero; both absent preserve theme/defaults. Exclusive with the whole density reference.");
        add(values, "visualDensityVertical", Group.LAYOUT, "Finite density from -4 to 4. The missing peer axis is zero; both absent preserve theme/defaults. Exclusive with the whole density reference.");
        add(values, "focusNode", Group.BEHAVIOR, "Strict FocusNode reference. Preview uses isolated local focus ownership.");
        add(values, "autofocus", Group.BEHAVIOR, "Unset is false.");
        add(values, "useCupertinoCheckmarkStyle", Group.APPEARANCE, "Adaptive constructor only, default false. Controls the Cupertino checkmark style only on Apple platforms.");
        add(values, "enabled", Group.BEHAVIOR, "Actual nullable SDK enabled field. Unset/null infers activation from callback or registry/RadioGroup. Explicit true requires one of those activation sources at runtime; false disables without removing stored arguments.");
        add(values, "groupRegistry", Group.BEHAVIOR, "Explicit null or strict RadioGroupRegistry<T> reference. The selected generic type and registerClient consumption are both checked; covariance alone is insufficient. An absent/null registry preserves inherited RadioGroup lookup.");
        add(values, "backgroundColor", Group.BACKGROUND_COLOR, colorReferenceHelp("backgroundColor"));
        add(values, "side", Group.SIDE, "Strict BorderSide reference, including WidgetStateBorderSide; exclusive with local side fields. Plain sides apply only when unselected. Ignored by adaptive Apple.");
        add(values, "innerRadius", Group.INNER_RADIUS, "Strict WidgetStateProperty<double?> reference, exclusive with local radius buckets. Ignored by adaptive Apple.");
        add(values, "variant", Group.BEHAVIOR, "Required constructor selector, created Standard. Both Standard and Adaptive are const-capable.");
        add(values, "valueType", Group.BEHAVIOR, "Required explicit generic type: String, int, double, num, bool, Object, or a simple project class/enum/typedef reference. No invocation, member expression or raw Dart type is accepted. Created String.");
        add(values, "nullableValueType", Group.BEHAVIOR, "Unset/false uses T; true uses T?. Group values and callbacks use the nullable form in either case. Type changes never rewrite or discard values, callbacks or registry references.");
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor")) {
            Group group = family.equals("fillColor") ? Group.FILL_COLOR : family.equals("overlayColor") ? Group.OVERLAY_COLOR : Group.BACKGROUND_COLOR;
            for (String name : colorStateProperties(family)) add(values, name, group,
                    "Literal color, semantic role or explicit null. First matching disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default entry wins; null stops lower entries and defers to the SDK theme. Omission adds no entry.");
        }
        for (String name : innerRadiusStateProperties()) add(values, name, Group.INNER_RADIUS,
                "Signed finite radius, either infinity, or explicit null. First matching state entry wins; null defers to the SDK/theme and omission adds no entry. The pinned painter resolves inner radius using its selected/pressed states.");
        for (String name : sideLocalProperties()) {
            var shared = CheckboxWidgetPropertySchema.find(name).orElseThrow();
            add(values, name, Group.SIDE, shared.description().replace("Checkbox", "Radio") + " Ignored by adaptive Apple.");
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("Radio property count mismatch");
        return Collections.unmodifiableMap(values);
    }

    private static String colorReferenceHelp(String family) {
        return "Strict WidgetStateProperty<Color?> reference, exclusive with all local " + family + " entries. Dynamic or nullable outer references are rejected. Ignored by adaptive Apple.";
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, name.equals("variant") ? "Constructor"
                : Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, values.size()));
    }
}
