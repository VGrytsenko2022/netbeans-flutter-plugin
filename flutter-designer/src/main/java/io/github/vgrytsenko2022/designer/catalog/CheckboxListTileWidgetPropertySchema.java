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

/** Both complete Flutter 3.44.8 CheckboxListTile constructors and closed compound projections. */
public final class CheckboxListTileWidgetPropertySchema {
    public static final WidgetTypeId CHECKBOX_LIST_TILE_TYPE = new WidgetTypeId("flutter.material.CheckboxListTile");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 154;
    public static final int DIRECT_PROPERTY_COUNT = 38;
    public static final int SLOT_COUNT = 3;

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"), ACCESSIBILITY("Accessibility"),
        SHAPE("Shape"), CHECKBOX_SHAPE("CheckboxShape"), FILL_COLOR("FillColor"), OVERLAY_COLOR("OverlayColor"),
        SIDE("Side"), MOUSE_CURSOR("MouseCursor");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "checkboxListTile" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter CheckboxListTile " + displayName() + "."; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description);
            Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative CheckboxListTile property order");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private CheckboxListTileWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> variants() { return List.of("standard", "adaptive"); }
    public static List<String> statePrefixes() { return CheckboxWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return CheckboxWidgetPropertySchema.statePriority(); }
    public static List<String> sideStates() { return CheckboxWidgetPropertySchema.sideStates(); }
    public static List<String> sideBaseProperties() { return CheckboxWidgetPropertySchema.sideBaseProperties(); }
    public static List<String> sideBucketProperties(String state) { return CheckboxWidgetPropertySchema.sideBucketProperties(state); }
    public static List<String> sideStateProperties() { return CheckboxWidgetPropertySchema.sideStateProperties(); }
    public static List<String> sideLocalProperties() { return CheckboxWidgetPropertySchema.sideLocalProperties(); }
    public static List<String> colorStateProperties(String family) { return CheckboxWidgetPropertySchema.colorStateProperties(family); }
    public static List<String> colorProperties() { return List.of("activeColor", "checkColor", "hoverColor", "tileColor", "selectedTileColor"); }
    public static List<String> mouseCursorPresets() { return CheckboxWidgetPropertySchema.mouseCursorPresets(); }
    public static List<String> mouseCursorStateProperties() { return statePrefixes().stream().map(state -> "mouseCursor" + state).toList(); }
    public static List<String> cursorStateProperties() { return mouseCursorStateProperties(); }
    public static List<String> geometryProperties() { return ListTileWidgetPropertySchema.geometryProperties(); }
    public static boolean requiresSubtitle(WidgetNode node) {
        return CHECKBOX_LIST_TILE_TYPE.equals(node.type()) && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("isThreeLine")));
    }
    public static List<String> shapeKinds() { return CardWidgetPropertySchema.shapeKinds(); }
    public static List<String> shapeFamilies() { return List.of("shape", "checkboxShape"); }
    public static List<String> builtInShapePropertyNames() { return CardWidgetPropertySchema.builtInShapePropertyNames(); }
    public static List<String> checkboxShapePropertyNames() { return shapeLocalProperties("checkboxShape"); }
    public static List<String> shapeLocalProperties(String family) {
        if (!shapeFamilies().contains(family)) throw new IllegalArgumentException("Not a CheckboxListTile shape family: " + family);
        return builtInShapePropertyNames().stream().map(name -> family + name.substring(5)).toList();
    }
    public static Optional<String> shapeFamily(PropertyName name) { return shapeFamily(name.value()); }
    public static Optional<String> shapeFamily(String name) {
        return shapeFamilies().stream().filter(family -> shapeLocalProperties(family).contains(name)).findFirst();
    }
    public static Optional<String> shapeSourceName(PropertyName name) { return shapeSourceName(name.value()); }
    public static Optional<String> shapeSourceName(String name) {
        return shapeFamily(name).map(family -> "shape" + name.substring(family.length()));
    }
    public static boolean isShapeDetailProperty(String name) { return shapeSourceName(name).map(CardWidgetPropertySchema::isShapeDetailProperty).orElse(false); }
    public static boolean shapePropertyAppliesToKind(String name, String kind) { return shapeSourceName(name).map(value -> CardWidgetPropertySchema.shapePropertyAppliesToKind(value, kind)).orElse(false); }
    public static String preferredShapeKindForProperty(String name) { return CardWidgetPropertySchema.preferredShapeKindForProperty(shapeSourceName(name).orElseThrow()); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        var direct = List.of("value", "onChanged", "mouseCursor", "activeColor", "fillColor", "checkColor", "hoverColor",
                "overlayColor", "splashRadius", "materialTapTargetSize", "visualDensity", "focusNode", "statesController",
                "autofocus", "shape", "side", "isError", "enabled", "tileColor", "isThreeLine", "dense", "selected",
                "controlAffinity", "contentPadding", "tristate", "checkboxShape", "selectedTileColor", "onFocusChange",
                "enableFeedback", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight",
                "checkboxSemanticLabel", "checkboxScaleFactor", "titleAlignment", "internalAddSemanticForOnTap", "variant");
        int order = SLOT_COUNT;
        for (String name : direct) {
            Group group = switch (name) {
                case "shape" -> Group.SHAPE;
                case "checkboxShape" -> Group.CHECKBOX_SHAPE;
                case "side" -> Group.SIDE;
                case "fillColor" -> Group.FILL_COLOR;
                case "overlayColor" -> Group.OVERLAY_COLOR;
                case "mouseCursor" -> Group.MOUSE_CURSOR;
                case "checkboxSemanticLabel", "internalAddSemanticForOnTap" -> Group.ACCESSIBILITY;
                case "visualDensity", "isThreeLine", "dense", "controlAffinity", "contentPadding", "horizontalTitleGap",
                        "minVerticalPadding", "minLeadingWidth", "minTileHeight", "titleAlignment", "materialTapTargetSize", "checkboxScaleFactor" -> Group.LAYOUT;
                case "value", "onChanged", "onFocusChange", "focusNode", "statesController", "autofocus", "isError", "enabled", "selected", "tristate", "enableFeedback", "variant" -> Group.BEHAVIOR;
                default -> Group.APPEARANCE;
            };
            String help = switch (name) {
                case "value" -> "Required controlled checked value, created false. Explicit null requires Tristate true. Canvas does not mutate the stored value.";
                case "onChanged" -> "Required explicit no-op, null, or strict ValueChanged<bool?> reference/factory; created no-op. Null disables the handler. Enabled false does not remove the source argument or its type proof. Canvas never executes project code.";
                case "enabled" -> "Genuine nullable SDK Enabled: unset/null infers tile activation from onChanged; false disables activation, true with a null handler remains legal. No synthetic Designer enablement policy is applied.";
                case "variant" -> "Required constructor selector, created Standard. Standard and Adaptive are both const-capable; Adaptive changes only the internal Checkbox on Apple platforms.";
                case "tristate" -> "Unset false. True permits null Value; SDK activation cycles false/true/null without changing this controlled model itself.";
                case "shape" -> "Whole strict ShapeBorder reference or local tile shape, mutually exclusive. Applies to ListTile, not to the checkbox and does not add clipping.";
                case "checkboxShape" -> "Whole strict OutlinedBorder reference or local checkbox shape, mutually exclusive. Unset inherits CheckboxTheme or Cupertino defaults; it is independent of the tile Shape.";
                case "side", "fillColor", "overlayColor" -> CheckboxWidgetPropertySchema.find(name).orElseThrow().description();
                case "mouseCursor" -> "All 41 presets or a strict MouseCursor reference, including stateful subtypes; exclusive with local states. Passed to the internal Checkbox only, not ListTile.";
                case "activeColor" -> "Literal/theme color or strict Color reference. Also supplies selected title color; that fallback uses CheckboxTheme.fillColor then colorScheme.secondary, not this widget's local Fill color map.";
                case "visualDensity" -> "Strict VisualDensity reference, exclusive with local axes. Forwarded to ListTile only, not to the internal Checkbox.";
                case "materialTapTargetSize" -> "Padded or shrinkWrap. Omitted uses explicit shrinkWrap inside the tile, rather than the standalone Checkbox default; ignored by CupertinoCheckbox.";
                case "checkboxScaleFactor" -> "Signed finite scale or explicit positive/negative Infinity or NaN. Unset uses 1.0; zero and negative scales are legal. Unsafe mounted paint or input geometry is diagnosed only in Canvas.";
                case "splashRadius" -> "Signed finite reaction radius, positive/negative Infinity, NaN or explicit null. Unset/null inherits CheckboxTheme; zero or negative suppresses reaction. Cupertino ignores this field.";
                case "isThreeLine" -> "Unset/null preserves ListTileTheme. Only explicit true requires a nonempty Subtitle slot; no child is fabricated.";
                case "onFocusChange" -> "Optional no-op, null or strict ValueChanged<bool> reference/factory, forwarded to ListTile and retained when disabled. Canvas callbacks are isolated.";
                case "controlAffinity" -> "Leading places the checkbox before the title; trailing/platform place it after. Secondary occupies the opposite side. Unset inherits ListTileTheme then platform.";
                case "internalAddSemanticForOnTap" -> "Unset uses false, unlike standalone ListTile. The actual SDK wraps the tile in MergeSemantics; false omits its additional button flag.";
                case "checkboxSemanticLabel" -> "Optional semantic label forwarded to the internal Checkbox; the outer widget preserves the SDK MergeSemantics contract.";
                case "contentPadding", "dense", "enableFeedback", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight", "titleAlignment", "focusNode", "statesController" -> ListTileWidgetPropertySchema.find(name).orElseThrow().description();
                case "selected", "autofocus", "isError" -> "Unset uses false; selected is independent of Value. Stored intent is forwarded to the actual SDK widget.";
                default -> "Literal/theme color or strict Color reference. Omitted preserves the actual SDK/theme defaults without synthesizing a replacement.";
            };
            add(values, name, group, help, order++);
        }
        for (String axis : List.of("Horizontal", "Vertical")) add(values, "visualDensity" + axis, Group.LAYOUT,
                "Local density -4..4, omitted peer zero; exclusive with whole VisualDensity. Applies to ListTile only.", order++);
        for (String family : shapeFamilies()) for (String name : shapeLocalProperties(family)) {
            var shared = CardWidgetPropertySchema.definitions().get(shapeSourceName(name).orElseThrow());
            values.put(name, new Definition(family.equals("shape") ? Group.SHAPE : Group.CHECKBOX_SHAPE, shared.displayName(),
                    shared.description().replace("Card", "CheckboxListTile " + family), shared.dartName(), order++));
        }
        for (String family : List.of("fillColor", "overlayColor")) for (String name : colorStateProperties(family)) {
            add(values, name, family.equals("fillColor") ? Group.FILL_COLOR : Group.OVERLAY_COLOR,
                    CheckboxWidgetPropertySchema.find(name).orElseThrow().description(), order++);
        }
        for (String name : sideLocalProperties()) add(values, name, Group.SIDE,
                CheckboxWidgetPropertySchema.find(name).orElseThrow().description(), order++);
        for (String name : mouseCursorStateProperties()) add(values, name, Group.MOUSE_CURSOR,
                "Non-null preset or strict MouseCursor reference. Any local map requires an explicit Default; no cursor is invented. First matching disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default entry wins. The internal Checkbox controls requested states.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != SLOT_COUNT + CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("CheckboxListTile schema count/order mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help, int order) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, name.equals("variant") ? "Constructor" : Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, order));
    }
}
