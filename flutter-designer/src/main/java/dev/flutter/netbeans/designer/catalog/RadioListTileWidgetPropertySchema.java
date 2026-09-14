package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 RadioListTile constructor union with typed compound projections. */
public final class RadioListTileWidgetPropertySchema {
    public static final WidgetTypeId RADIO_LIST_TILE_TYPE = new WidgetTypeId("flutter.material.RadioListTile");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 153;
    public static final int DIRECT_PROPERTY_COUNT = 40;
    public static final int SLOT_COUNT = 3;
    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"), ACCESSIBILITY("Accessibility"),
        SHAPE("Shape"), FILL_COLOR("FillColor"), OVERLAY_COLOR("OverlayColor"), BACKGROUND_COLOR("BackgroundColor"),
        INNER_RADIUS("InnerRadius"), SIDE("Side"), MOUSE_CURSOR("MouseCursor");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "radioListTile" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter RadioListTile " + displayName() + "."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition { Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(dartName); if (dartOrder < 0) throw new IllegalArgumentException("Negative property order"); }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private RadioListTileWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> variants() { return RadioWidgetPropertySchema.variants(); }
    public static List<String> valueTypes() { return RadioWidgetPropertySchema.valueTypes(); }
    public static boolean nullableValueType(WidgetNode node) { return RadioWidgetPropertySchema.nullableValueType(node); }
    public static boolean isTypeReference(PropertyValue value) { return RadioWidgetPropertySchema.isTypeReference(value); }
    public static Optional<String> valueTypeError(WidgetNode node) { return RadioWidgetPropertySchema.valueTypeError(node).map(v -> v.replace("Radio ", "RadioListTile ")); }
    public static String variant(WidgetNode node) { return node.properties().get(new PropertyName("variant")) instanceof PropertyValue.StringValue value ? value.value() : "standard"; }
    public static boolean propertyAvailable(WidgetNode node, PropertyName name) { return !node.type().equals(RADIO_LIST_TILE_TYPE) || propertyAvailableInVariant(name.value(), variant(node)); }
    public static boolean propertyAvailableInVariant(String name, String variant) { return !name.equals("useCupertinoCheckmarkStyle") || variant.equals("adaptive"); }
    public static boolean requiresSubtitle(WidgetNode node) { return node.type().equals(RADIO_LIST_TILE_TYPE) && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("isThreeLine"))); }
    public static List<String> statePrefixes() { return RadioWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return RadioWidgetPropertySchema.statePriority(); }
    public static List<String> colorFamilies() { return List.of("fillColor", "overlayColor", "radioBackgroundColor"); }
    public static List<String> colorProperties() { return List.of("activeColor", "hoverColor", "tileColor", "selectedTileColor"); }
    public static List<String> colorStateProperties(String family) {
        if (!colorFamilies().contains(family)) throw new IllegalArgumentException("Not a RadioListTile color family: " + family);
        return statePrefixes().stream().map(s -> family + s).toList();
    }
    public static List<String> innerRadiusStateProperties() { return statePrefixes().stream().map(s -> "radioInnerRadius" + s).toList(); }
    public static List<String> sideStates() { return RadioWidgetPropertySchema.sideStates(); }
    public static List<String> sideBaseProperties() { return RadioWidgetPropertySchema.sideBaseProperties().stream().map(RadioListTileWidgetPropertySchema::tileSideName).toList(); }
    public static List<String> sideStateProperties() { return RadioWidgetPropertySchema.sideStateProperties().stream().map(RadioListTileWidgetPropertySchema::tileSideName).toList(); }
    public static List<String> sideLocalProperties() { return RadioWidgetPropertySchema.sideLocalProperties().stream().map(RadioListTileWidgetPropertySchema::tileSideName).toList(); }
    public static List<String> sideBucketProperties(String state) { return RadioWidgetPropertySchema.sideBucketProperties(state).stream().map(RadioListTileWidgetPropertySchema::tileSideName).toList(); }
    private static String tileSideName(String name) { return "radioS" + name.substring(1); }
    public static String radioSourceName(String name) {
        for (String family : List.of("BackgroundColor", "InnerRadius", "Side")) if (name.startsWith("radio" + family)) return Character.toLowerCase(family.charAt(0)) + name.substring(6);
        return name;
    }
    public static String radioSourceName(PropertyName name) { return radioSourceName(name.value()); }
    public static List<String> mouseCursorPresets() { return RadioWidgetPropertySchema.mouseCursorPresets(); }
    public static List<String> mouseCursorStateProperties() { return statePrefixes().stream().map(s -> "mouseCursor" + s).toList(); }
    public static List<String> cursorStateProperties() { return mouseCursorStateProperties(); }
    public static List<String> geometryProperties() { return ListTileWidgetPropertySchema.geometryProperties(); }
    public static List<String> shapeKinds() { return CardWidgetPropertySchema.shapeKinds(); }
    public static List<String> shapeFamilies() { return List.of("shape"); }
    public static List<String> builtInShapePropertyNames() { return CardWidgetPropertySchema.builtInShapePropertyNames(); }
    public static List<String> shapeLocalProperties(String family) { if (!family.equals("shape")) throw new IllegalArgumentException("Not a shape family: " + family); return builtInShapePropertyNames(); }
    public static Optional<String> shapeFamily(PropertyName name) { return shapeFamily(name.value()); }
    public static Optional<String> shapeFamily(String name) { return builtInShapePropertyNames().contains(name) ? Optional.of("shape") : Optional.empty(); }
    public static Optional<String> shapeSourceName(PropertyName name) { return shapeSourceName(name.value()); }
    public static Optional<String> shapeSourceName(String name) { return builtInShapePropertyNames().contains(name) ? Optional.of(name) : Optional.empty(); }
    public static boolean isShapeDetailProperty(String name) { return CardWidgetPropertySchema.isShapeDetailProperty(name); }
    public static boolean shapePropertyAppliesToKind(String name, String kind) { return CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind); }
    public static String preferredShapeKindForProperty(String name) { return CardWidgetPropertySchema.preferredShapeKindForProperty(name); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        var direct = List.of("value", "groupValue", "onChanged", "mouseCursor", "toggleable", "activeColor", "fillColor",
                "hoverColor", "overlayColor", "splashRadius", "materialTapTargetSize", "isThreeLine", "dense", "selected",
                "controlAffinity", "autofocus", "contentPadding", "shape", "tileColor", "selectedTileColor", "visualDensity",
                "focusNode", "statesController", "onFocusChange", "enableFeedback", "horizontalTitleGap", "minVerticalPadding",
                "minLeadingWidth", "minTileHeight", "radioScaleFactor", "titleAlignment", "enabled", "internalAddSemanticForOnTap",
                "radioBackgroundColor", "radioSide", "radioInnerRadius", "useCupertinoCheckmarkStyle", "variant", "valueType", "nullableValueType");
        for (String name : direct) {
            Group group = switch (name) {
                case "shape" -> Group.SHAPE; case "fillColor" -> Group.FILL_COLOR; case "overlayColor" -> Group.OVERLAY_COLOR;
                case "radioBackgroundColor" -> Group.BACKGROUND_COLOR; case "radioSide" -> Group.SIDE; case "radioInnerRadius" -> Group.INNER_RADIUS;
                case "mouseCursor" -> Group.MOUSE_CURSOR; case "internalAddSemanticForOnTap" -> Group.ACCESSIBILITY;
                case "materialTapTargetSize", "isThreeLine", "dense", "controlAffinity", "contentPadding", "visualDensity", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight", "radioScaleFactor", "titleAlignment" -> Group.LAYOUT;
                case "activeColor", "hoverColor", "tileColor", "selectedTileColor" -> Group.APPEARANCE;
                default -> Group.BEHAVIOR;
            };
            String help = switch (name) {
                case "groupValue" -> "Legacy nullable selected value. Effective value is RadioGroup registry groupValue when non-null, otherwise this legacy value; modern null selection can fall back to a configured legacy value. Deprecated in favor of RadioGroup.";
                case "onChanged" -> "Optional no-op, explicit null or strict ValueChanged<T?> reference/factory; creation stores no-op and reset truly omits. Tile first notifies its matching RadioGroup, then this legacy callback if non-null. Neither is executed by Canvas.";
                case "onFocusChange" -> "Optional no-op, null or strict ValueChanged<bool> callback, forwarded only to the outer ListTile, not the inner Radio. User source remains independent of enabled state.";
                case "activeColor" -> "Literal/theme color or strict Color reference. Selected title uses activeColor, then RadioTheme.fillColor resolved with selected when set, then colorScheme.secondary. The local fillColor map does not color the selected title.";
                case "selected" -> "Unset false. Tile selected appearance is independent of whether Value matches the effective group value.";
                case "enabled" -> "Actual nullable SDK field. Omission/null infers activation from legacy callback or matching RadioGroup. Explicit true requires an activation source at mount time; Canvas diagnoses missing activation instead of inventing callbacks. State binding retains this same runtime requirement.";
                case "useCupertinoCheckmarkStyle" -> "Adaptive-only non-null bool, default false. Values and State bindings remain stored but inactive and ungenerated in Standard; switching back restores them without auto-clearing.";
                case "radioScaleFactor" -> "Signed finite scale, zero, Infinity, negative Infinity or NaN; omission is 1.0. Source preserves exact SDK intent; unsafe mounted geometry is diagnosed in Canvas.";
                case "splashRadius" -> "Signed finite radius, Infinity, negative Infinity, NaN or explicit null. Omission/null retains theme defaults; unsafe mounted geometry is diagnosed without rewriting source.";
                case "isThreeLine" -> "Omission/null retains ListTileTheme. Explicit true requires a nonempty Subtitle slot; no child is fabricated.";
                case "controlAffinity" -> "Leading/platform places the Radio before the title; trailing places it after. Secondary occupies the opposite side. Omission inherits ListTileTheme.";
                case "mouseCursor" -> "All 41 presets or strict MouseCursor reference, including stateful subtypes; exclusive with local entries. Local maps require explicit Default. Forwarded to the inner Radio.";
                case "variant" -> "Required Standard/Adaptive selector, created Standard. Both constructors are const-capable; adaptive renders the actual pinned SDK platform branch.";
                case "internalAddSemanticForOnTap" -> "Unset false. The SDK merges tile semantics; children requiring their own semantic nodes can conflict with the framework contract.";
                default -> RadioWidgetPropertySchema.find(radioSourceName(name)).map(DefinitionSource -> DefinitionSource.description().replace("Radio ", "RadioListTile "))
                        .orElseGet(() -> ListTileWidgetPropertySchema.find(name).map(DefinitionSource -> DefinitionSource.description()).orElse("Literal/theme color or strict Color reference; omission retains SDK defaults."));
            };
            add(values, name, group, help);
        }
        for (String name : List.of("visualDensityHorizontal", "visualDensityVertical")) add(values, name, Group.LAYOUT, "Local density axis -4..4; missing peer zero; exclusive with whole VisualDensity. Applies only to ListTile.");
        for (String name : builtInShapePropertyNames()) add(values, name, Group.SHAPE, CardWidgetPropertySchema.definitions().get(name).description().replace("Card", "RadioListTile"));
        for (String family : colorFamilies()) for (String name : colorStateProperties(family)) add(values, name, values.get(family).group(), RadioWidgetPropertySchema.find(radioSourceName(name)).orElseThrow().description());
        for (String name : innerRadiusStateProperties()) add(values, name, Group.INNER_RADIUS, RadioWidgetPropertySchema.find(radioSourceName(name)).orElseThrow().description());
        for (String name : sideLocalProperties()) add(values, name, Group.SIDE, RadioWidgetPropertySchema.find(radioSourceName(name)).orElseThrow().description());
        for (String name : mouseCursorStateProperties()) add(values, name, Group.MOUSE_CURSOR, "Strict cursor preset/reference. First matching state wins. Explicit Default is required; exclusive with a whole cursor.");
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("RadioListTile property count: " + values.size());
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, name.equals("variant") ? "Constructor" : Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, SLOT_COUNT + values.size()));
    }
}
