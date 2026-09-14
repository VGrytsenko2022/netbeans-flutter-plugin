package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 standard/adaptive SwitchListTile constructor and compound projection. */
public final class SwitchListTileWidgetPropertySchema {
    public static final WidgetTypeId SWITCH_LIST_TILE_TYPE = new WidgetTypeId("flutter.material.SwitchListTile");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 236;
    public static final int DIRECT_PROPERTY_COUNT = 42;
    public static final int SLOT_COUNT = 3;

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"), ACCESSIBILITY("Accessibility"),
        IMAGES("Images"), SHAPE("Shape"), THUMB_COLOR("ThumbColor"), TRACK_COLOR("TrackColor"),
        TRACK_OUTLINE_COLOR("TrackOutlineColor"), OVERLAY_COLOR("OverlayColor"), THUMB_ICON("ThumbIcon"), MOUSE_CURSOR("MouseCursor");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "switchListTile" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter SwitchListTile " + displayName() + "."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative SwitchListTile property order");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private SwitchListTileWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> variants() { return List.of("standard", "adaptive"); }
    public static String variant(WidgetNode node) { return node.properties().get(new PropertyName("variant")) instanceof PropertyValue.StringValue value ? value.value() : "standard"; }
    public static boolean propertyAvailable(WidgetNode node, PropertyName name) { return !node.type().equals(SWITCH_LIST_TILE_TYPE) || propertyAvailableInVariant(name.value(), variant(node)); }
    public static boolean propertyAvailableInVariant(String name, String variant) { return !name.equals("applyCupertinoTheme") || variant.equals("adaptive"); }
    public static List<String> statePrefixes() { return SwitchWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return SwitchWidgetPropertySchema.statePriority(); }
    public static List<String> colorFamilies() { return SwitchWidgetPropertySchema.colorFamilies(); }
    public static List<String> colorStateProperties(String family) { return SwitchWidgetPropertySchema.colorStateProperties(family); }
    public static List<String> colorProperties() { return List.of("activeColor", "activeThumbColor", "activeTrackColor", "inactiveThumbColor", "inactiveTrackColor", "tileColor", "selectedTileColor", "hoverColor"); }
    public static List<String> thumbIconStates() { return SwitchWidgetPropertySchema.thumbIconStates(); }
    public static List<String> thumbIconBucketProperties(String state) { return SwitchWidgetPropertySchema.thumbIconBucketProperties(state); }
    public static List<String> thumbIconLocalProperties() { return SwitchWidgetPropertySchema.thumbIconLocalProperties(); }
    public static Optional<String> iconSourceName(PropertyName name) { return iconSourceName(name.value()); }
    public static Optional<String> iconSourceName(String name) { return SwitchWidgetPropertySchema.iconSourceName(name); }
    public static List<String> mouseCursorPresets() { return SwitchWidgetPropertySchema.mouseCursorPresets(); }
    public static List<String> mouseCursorStateProperties() { return statePrefixes().stream().map(state -> "mouseCursor" + state).toList(); }
    public static List<String> cursorStateProperties() { return mouseCursorStateProperties(); }
    public static List<String> geometryProperties() { return ListTileWidgetPropertySchema.geometryProperties(); }
    public static boolean requiresSubtitle(WidgetNode node) { return SWITCH_LIST_TILE_TYPE.equals(node.type()) && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("isThreeLine"))); }
    public static List<String> shapeKinds() { return CardWidgetPropertySchema.shapeKinds(); }
    public static List<String> shapeFamilies() { return List.of("shape"); }
    public static List<String> builtInShapePropertyNames() { return CardWidgetPropertySchema.builtInShapePropertyNames(); }
    public static List<String> shapeLocalProperties(String family) {
        if (!family.equals("shape")) throw new IllegalArgumentException("Not a SwitchListTile shape family: " + family);
        return builtInShapePropertyNames();
    }
    public static Optional<String> shapeFamily(PropertyName name) { return shapeFamily(name.value()); }
    public static Optional<String> shapeFamily(String name) { return builtInShapePropertyNames().contains(name) ? Optional.of("shape") : Optional.empty(); }
    public static Optional<String> shapeSourceName(PropertyName name) { return shapeSourceName(name.value()); }
    public static Optional<String> shapeSourceName(String name) { return builtInShapePropertyNames().contains(name) ? Optional.of(name) : Optional.empty(); }
    public static boolean isShapeDetailProperty(String name) { return CardWidgetPropertySchema.isShapeDetailProperty(name); }
    public static boolean shapePropertyAppliesToKind(String name, String kind) { return CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind); }
    public static String preferredShapeKindForProperty(String name) { return CardWidgetPropertySchema.preferredShapeKindForProperty(name); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        var direct = List.of("value", "onChanged", "activeColor", "activeThumbColor", "activeTrackColor", "inactiveThumbColor", "inactiveTrackColor",
                "activeThumbImage", "onActiveThumbImageError", "inactiveThumbImage", "onInactiveThumbImageError", "thumbColor", "trackColor", "trackOutlineColor",
                "thumbIcon", "materialTapTargetSize", "dragStartBehavior", "mouseCursor", "overlayColor", "splashRadius", "focusNode", "statesController",
                "onFocusChange", "autofocus", "applyCupertinoTheme", "tileColor", "isThreeLine", "dense", "contentPadding", "selected", "controlAffinity",
                "shape", "selectedTileColor", "visualDensity", "enableFeedback", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight",
                "hoverColor", "internalAddSemanticForOnTap", "variant");
        int order = SLOT_COUNT;
        for (String name : direct) {
            Group group = switch (name) {
                case "shape" -> Group.SHAPE;
                case "thumbColor" -> Group.THUMB_COLOR;
                case "trackColor" -> Group.TRACK_COLOR;
                case "trackOutlineColor" -> Group.TRACK_OUTLINE_COLOR;
                case "overlayColor" -> Group.OVERLAY_COLOR;
                case "thumbIcon" -> Group.THUMB_ICON;
                case "mouseCursor" -> Group.MOUSE_CURSOR;
                case "activeThumbImage", "inactiveThumbImage", "onActiveThumbImageError", "onInactiveThumbImageError" -> Group.IMAGES;
                case "internalAddSemanticForOnTap" -> Group.ACCESSIBILITY;
                case "materialTapTargetSize", "visualDensity", "isThreeLine", "dense", "contentPadding", "controlAffinity", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight" -> Group.LAYOUT;
                case "value", "onChanged", "onFocusChange", "autofocus", "applyCupertinoTheme", "focusNode", "statesController", "dragStartBehavior", "selected", "enableFeedback", "variant" -> Group.BEHAVIOR;
                default -> Group.APPEARANCE;
            };
            String help = switch (name) {
                case "value" -> "Required controlled bool value, created false. There is no tristate or nullable value. Selected is independent of Value; Canvas never updates the stored value.";
                case "onChanged" -> "Required explicit no-op, null or strict ValueChanged<bool> project reference/factory; created no-op. Null disables activation. No synthetic Enabled property exists. Canvas never invokes project callbacks.";
                case "onFocusChange" -> "Optional no-op, null or strict ValueChanged<bool> reference/factory, forwarded to both outer ListTile and inner Switch (inside ExcludeFocus). Callback source is retained when disabled; Canvas never invokes it.";
                case "activeColor", "activeThumbColor" -> "Literal/theme color or strict Color reference. Selected tile title uses activeThumbColor, then deprecated activeColor, then SwitchTheme.thumbColor resolved with selected when set, then colorScheme.secondary. The local Thumb color state map does not color the selected title.";
                case "onActiveThumbImageError", "onInactiveThumbImageError" -> "Optional no-op, null or strict ImageErrorListener reference/factory. Non-null handlers require the corresponding thumb image; explicit null does not. Canvas never invokes project callbacks.";
                case "variant" -> "Required Standard/Adaptive constructor selector, created Standard. Both are const-capable. Adaptive uses a Cupertino-style switch on Apple platforms without changing this controlled model.";
                case "applyCupertinoTheme" -> "Adaptive-only nullable bool. Unset/null lets Flutter choose the Cupertino theme policy. Stored values and State bindings are retained but inactive and not emitted in Standard; switching back restores them.";
                case "mouseCursor" -> "All 41 presets or strict MouseCursor reference, including stateful subtypes; exclusive with local state entries. Any local map requires explicit Default. Applied to the internal Switch, not the tile.";
                case "materialTapTargetSize" -> "Padded or shrinkWrap; the tile supplies shrinkWrap when omitted. Cupertino ignores Material-specific visual arguments.";
                case "splashRadius" -> "Signed finite radius, positive/negative Infinity, NaN or explicit null. Null/omission preserves SDK/theme fallback. Unsafe mounted reaction geometry is diagnosed in Canvas without changing source/model.";
                case "shape" -> "Whole strict ShapeBorder reference or local tile shape, mutually exclusive; this shapes the ListTile, not the Switch and does not add clipping.";
                case "isThreeLine" -> "Unset/null retains ListTileTheme. Only explicit true requires a nonempty Subtitle slot; no child is fabricated.";
                case "controlAffinity" -> "Leading places the switch before the title; trailing/platform places it after. Secondary occupies the opposite side. Unset inherits the SDK ListTileTheme policy.";
                case "selected" -> "Unset false. Selected tile appearance is independent of the controlled switch Value.";
                case "internalAddSemanticForOnTap" -> "Unset false. The SDK wraps the tile in MergeSemantics; rich children requiring their own semantics can conflict with that SDK contract.";
                case "visualDensity", "contentPadding", "dense", "enableFeedback", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight", "focusNode", "statesController" -> ListTileWidgetPropertySchema.find(name).orElseThrow().description();
                default -> SwitchWidgetPropertySchema.find(name).map(DefinitionSource -> DefinitionSource.description().replace("Switch ", "SwitchListTile "))
                        .orElse("Literal/theme color or strict Color reference. Omission preserves SDK/theme defaults.");
            };
            add(values, name, group, help, order++);
        }
        for (String axis : List.of("Horizontal", "Vertical")) add(values, "visualDensity" + axis, Group.LAYOUT,
                "Local VisualDensity axis -4..4; omitted peer zero, exclusive with whole VisualDensity. Applies to ListTile only.", order++);
        for (String name : builtInShapePropertyNames()) {
            var shared = CardWidgetPropertySchema.definitions().get(name);
            values.put(name, new Definition(Group.SHAPE, shared.displayName(), shared.description().replace("Card", "SwitchListTile"), shared.dartName(), order++));
        }
        for (String family : colorFamilies()) for (String name : colorStateProperties(family)) add(values, name, values.get(family).group(), SwitchWidgetPropertySchema.find(name).orElseThrow().description(), order++);
        for (String name : thumbIconLocalProperties()) add(values, name, Group.THUMB_ICON, SwitchWidgetPropertySchema.find(name).orElseThrow().description(), order++);
        for (String name : mouseCursorStateProperties()) add(values, name, Group.MOUSE_CURSOR,
                "Non-null preset or strict MouseCursor reference. Any local map requires explicit Default; disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default is the first-match order. The internal Switch chooses runtime states.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != SLOT_COUNT + CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("SwitchListTile schema count/order mismatch: " + values.size());
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help, int order) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, name.equals("variant") ? "Constructor" : Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, order));
    }
}
