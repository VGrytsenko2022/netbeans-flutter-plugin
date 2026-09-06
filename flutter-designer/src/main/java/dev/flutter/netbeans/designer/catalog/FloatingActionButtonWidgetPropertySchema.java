package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 FAB constructor union with reusable shape and TextStyle projections. */
public final class FloatingActionButtonWidgetPropertySchema {
    public static final WidgetTypeId FLOATING_ACTION_BUTTON_TYPE =
            new WidgetTypeId("flutter.material.FloatingActionButton");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 78;
    public static final int SLOT_COUNT = 2;
    private static final String TEXT_PREFIX = "extendedTextStyle";

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"),
        ACCESSIBILITY("Accessibility"), SHAPE("Shape"), EXTENDED_TEXT_STYLE("Extended text style");

        private final String displayName;

        Group(String displayName) {
            this.displayName = displayName;
        }

        public String setName() {
            return "floatingActionButton" + switch (this) {
                case BEHAVIOR -> "Behavior";
                case APPEARANCE -> "Appearance";
                case LAYOUT -> "Layout";
                case ACCESSIBILITY -> "Accessibility";
                case SHAPE -> "Shape";
                case EXTENDED_TEXT_STYLE -> "ExtendedTextStyle";
            };
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return "Flutter FloatingActionButton " + displayName.toLowerCase(java.util.Locale.ROOT) + ".";
        }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative FloatingActionButton property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private FloatingActionButtonWidgetPropertySchema() {
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
        return List.of("standard", "small", "large", "extended");
    }

    public static String variant(WidgetNode node) {
        var value = node.properties().get(new PropertyName("variant"));
        return value instanceof PropertyValue.StringValue text ? text.value() : "standard";
    }

    public static boolean isExtendedConstructor(WidgetNode node) {
        return FLOATING_ACTION_BUTTON_TYPE.equals(node.type()) && variant(node).equals("extended");
    }

    public static boolean requiresChild(WidgetNode node) {
        return isExtendedConstructor(node);
    }

    public static Optional<String> slotUnavailableReason(WidgetNode node, SlotName slot) {
        return FLOATING_ACTION_BUTTON_TYPE.equals(node.type()) && slot.value().equals("icon") && !isExtendedConstructor(node)
                ? Optional.of("FloatingActionButton Icon belongs to the Extended constructor; select Extended first.")
                : Optional.empty();
    }

    public static List<String> extendedOnlyProperties() {
        return DEFINITIONS.keySet().stream().filter(name -> name.startsWith("extended")).toList();
    }

    public static boolean propertyAvailableInVariant(String name, String variant) {
        if (name.equals("mini")) {
            return variant.equals("standard");
        }
        if (name.equals("isExtended")) {
            return variant.equals("standard") || variant.equals("extended");
        }
        return !name.startsWith("extended") || variant.equals("extended");
    }

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    public static List<String> shapeKinds() {
        return CardWidgetPropertySchema.shapeKinds();
    }

    public static List<String> builtInShapePropertyNames() {
        return CardWidgetPropertySchema.builtInShapePropertyNames();
    }

    public static boolean isShapeDetailProperty(String name) {
        return CardWidgetPropertySchema.isShapeDetailProperty(name);
    }

    public static boolean shapePropertyAppliesToKind(String name, String kind) {
        return CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind);
    }

    public static String preferredShapeKindForProperty(String name) {
        return CardWidgetPropertySchema.preferredShapeKindForProperty(name);
    }

    public static boolean isTextStyleProperty(PropertyName name) {
        return textStyleBinding(name).isPresent();
    }

    public static Optional<TextWidgetPropertySchema.Definition> textStyleBinding(PropertyName name) {
        String value = Objects.requireNonNull(name).value();
        return value.startsWith(TEXT_PREFIX)
                ? BadgeWidgetPropertySchema.textStyleBinding(new PropertyName("textStyle" + value.substring(TEXT_PREFIX.length())))
                : Optional.empty();
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "tooltip", Group.ACCESSIBILITY, "Tooltip", "Optional tooltip; unset omits the SDK Tooltip wrapper.", 1);
        int order = 2;
        for (String name : List.of("foregroundColor", "backgroundColor", "focusColor", "hoverColor", "splashColor")) {
            add(values, name, Group.APPEARANCE, label(name), "Literal or reviewed ColorScheme role; unset preserves FloatingActionButtonTheme and the actual M2/M3 defaults.", order++);
        }
        add(values, "heroTag", Group.BEHAVIOR, "Hero tag", "Unset preserves the shared SDK default tag; duplicate tags in one route may conflict. Explicit null disables Hero. Literal values or a strictly analyzed non-null Object reference preserve tag identity; isolated Canvas never executes project tag code.", 7);
        order = 8;
        for (String name : List.of("elevation", "focusElevation", "hoverElevation", "highlightElevation", "disabledElevation")) {
            add(values, name, Group.APPEARANCE, label(name), "Non-negative finite elevation or positive Infinity. Unset preserves component theme and framework defaults.", order++);
        }
        add(values, "onPressed", Group.BEHAVIOR, "On pressed", "Strict VoidCallback reference. Enabled with no reference emits a benign no-op; disabled emits null while retaining the stored callback.", 13);
        add(values, "mouseCursor", Group.BEHAVIOR, "Mouse cursor", "All 41 reviewed cursor constants or a strictly analyzed MouseCursor reference; unset uses the component theme then adaptiveClickable.", 14);
        add(values, "mini", Group.LAYOUT, "Mini", "Standard constructor only; unset is false. True selects the small SDK size while retaining the Standard API.", 15);
        add(values, "shape", Group.SHAPE, "Shape reference", "Strict ShapeBorder reference or zero-argument factory, mutually exclusive with all built-in shape fields. Unset preserves FloatingActionButtonTheme and actual variant defaults.", 16);
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior", "Non-null Clip enum; omission uses Clip.none in every constructor. A shape alone does not clip.", 17);
        add(values, "focusNode", Group.BEHAVIOR, "Focus node", "Strict FocusNode reference; isolated Canvas retains its own local focus state and does not execute project code.", 18);
        add(values, "autofocus", Group.BEHAVIOR, "Autofocus", "Unset is false.", 19);
        add(values, "materialTapTargetSize", Group.LAYOUT, "Material tap target size", "Padded or shrinkWrap; unset inherits ThemeData.materialTapTargetSize.", 20);
        add(values, "isExtended", Group.LAYOUT, "Extended state", "Standard and Extended constructors only. Omitted Standard is false; omitted Extended is true. False in Extended retains its required Label in the model but does not mount it.", 21);
        add(values, "enableFeedback", Group.BEHAVIOR, "Enable feedback", "Optional feedback policy; unset preserves component theme and SDK fallback.", 22);
        add(values, "extendedIconLabelSpacing", Group.LAYOUT, "Icon-label spacing", "Extended only. Signed finite spacing or positive Infinity is retained. Negative/Infinity is invalid only when the SDK actually lays out the spacing (Icon present and Extended state true); Canvas reports that context-dependent limitation.", 23);
        add(values, "extendedPadding", Group.LAYOUT, "Extended padding", "Extended only; non-negative physical or directional insets. Unset uses component theme and variant/icon-dependent padding.", 24);
        order = 25;
        for (var entry : BadgeWidgetPropertySchema.definitions().entrySet()) {
            if (!BadgeWidgetPropertySchema.isTextStyleProperty(new PropertyName(entry.getKey()))) {
                continue;
            }
            var shared = textStyleBinding(new PropertyName("extended" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1))).orElseThrow();
            values.put("extended" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1), new Definition(
                    Group.EXTENDED_TEXT_STYLE, shared.displayName(), shared.description()
                            + " Extended constructor only. All leaves unset preserve FloatingActionButtonTheme/default TextStyle. The SDK foregroundColor overrides style color unless foreground Paint is configured.", shared.dartName(), order++));
        }
        add(values, "variant", Group.BEHAVIOR, "Constructor", "Required Designer selector: Standard, Small, Large or Extended. Add Child before selecting Extended; move or clear Icon before leaving it. All four constructors are const-capable.", 57);
        add(values, "enabled", Group.BEHAVIOR, "Enabled", "Required Designer activation policy. Creation is true; false emits onPressed:null and keeps inactive project callback metadata for later re-enabling.", 58);
        order = 59;
        for (String name : builtInShapePropertyNames()) {
            var shared = CardWidgetPropertySchema.definitions().get(name);
            values.put(name, new Definition(Group.SHAPE, shared.displayName(),
                    shared.description().replace("Card", "FloatingActionButton"), shared.dartName(), order++));
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != 80) {
            throw new ExceptionInInitializerError("FloatingActionButton schema count/order mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group, String label, String help, int order) {
        values.put(name, new Definition(group, label, help, name, order));
    }

    private static String label(String name) {
        String words = name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
