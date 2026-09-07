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

/** Complete Flutter 3.44.8 ListTile constructor and closed compound projections. */
public final class ListTileWidgetPropertySchema {
    public static final WidgetTypeId LIST_TILE_TYPE = new WidgetTypeId("flutter.material.ListTile");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 176;
    public static final int DIRECT_PROPERTY_COUNT = 33;
    public static final int SLOT_COUNT = 4;
    private static final List<String> STYLE_SUFFIXES = List.of(
            "ThemeTextStyle", "Inherit", "Color", "BackgroundColor", "FontSize",
            "FontWeight", "FontStyle", "LetterSpacing", "WordSpacing", "TextBaseline",
            "Height", "LeadingDistribution", "LocaleLanguageCode", "LocaleScriptCode",
            "LocaleCountryCode", "DecorationUnderline", "DecorationOverline",
            "DecorationLineThrough", "Foreground", "Background", "Shadows",
            "FontFeatures", "FontVariations", "DecorationColor", "DecorationStyle",
            "DecorationThickness", "DebugLabel", "FontFamily", "FontFamilyFallback",
            "Package", "Overflow");

    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"),
        ACCESSIBILITY("Accessibility"), SHAPE("Shape"), ICON_COLOR("IconColor"),
        TEXT_COLOR("TextColor"), MOUSE_CURSOR("MouseCursor"),
        TITLE_STYLE("TitleStyle"), SUBTITLE_STYLE("SubtitleStyle"),
        LEADING_TRAILING_STYLE("LeadingTrailingStyle");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "listTile" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter ListTile " + displayName() + "."; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName);
            Objects.requireNonNull(description); Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative ListTile property order");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private ListTileWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static List<String> styleFamilies() { return List.of("titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle"); }
    public static List<String> localTextStyleProperties(String family) {
        if (!styleFamilies().contains(family)) throw new IllegalArgumentException("Not a ListTile text style: " + family);
        return STYLE_SUFFIXES.stream().map(suffix -> family + suffix).toList();
    }
    public static Optional<String> textStyleFamily(PropertyName name) {
        return styleFamilies().stream().filter(family -> localTextStyleProperties(family).contains(name.value())).findFirst();
    }
    public static Optional<TextWidgetPropertySchema.Definition> textStyleBinding(PropertyName name) {
        return textStyleFamily(name).flatMap(family -> TextWidgetPropertySchema.find(
                new PropertyName("style" + name.value().substring(family.length()))));
    }
    public static List<String> colorProperties() { return List.of("selectedColor", "iconColor", "textColor", "focusColor", "hoverColor", "splashColor", "tileColor", "selectedTileColor"); }
    public static List<String> stateColorFamilies() { return List.of("iconColor", "textColor"); }
    public static List<String> statePrefixes() { return CheckboxWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return CheckboxWidgetPropertySchema.statePriority(); }
    public static List<String> colorStateProperties(String family) {
        if (!stateColorFamilies().contains(family)) throw new IllegalArgumentException("Not a ListTile state color: " + family);
        return statePrefixes().stream().map(state -> family + state).toList();
    }
    public static List<String> mouseCursorStateProperties() { return statePrefixes().stream().map(state -> "mouseCursor" + state).toList(); }
    public static List<String> cursorStateProperties() { return mouseCursorStateProperties(); }
    public static List<String> mouseCursorPresets() { return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets(); }
    public static List<String> geometryProperties() { return List.of("horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight"); }
    public static boolean requiresSubtitle(WidgetNode node) {
        return LIST_TILE_TYPE.equals(node.type()) && new PropertyValue.BooleanValue(true).equals(node.properties().get(new PropertyName("isThreeLine")));
    }
    public static List<String> shapeKinds() { return CardWidgetPropertySchema.shapeKinds(); }
    public static List<String> builtInShapePropertyNames() { return CardWidgetPropertySchema.builtInShapePropertyNames(); }
    public static boolean isShapeDetailProperty(String name) { return CardWidgetPropertySchema.isShapeDetailProperty(name); }
    public static boolean shapePropertyAppliesToKind(String name, String kind) { return CardWidgetPropertySchema.shapePropertyAppliesToKind(name, kind); }
    public static String preferredShapeKindForProperty(String name) { return CardWidgetPropertySchema.preferredShapeKindForProperty(name); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        var direct = List.of("isThreeLine", "dense", "visualDensity", "shape", "style", "selectedColor", "iconColor", "textColor",
                "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle", "contentPadding", "enabled", "onTap", "onLongPress",
                "onFocusChange", "mouseCursor", "selected", "focusColor", "hoverColor", "splashColor", "focusNode", "autofocus",
                "tileColor", "selectedTileColor", "enableFeedback", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth",
                "minTileHeight", "titleAlignment", "internalAddSemanticForOnTap", "statesController");
        int order = 4;
        for (String name : direct) {
            Group group = switch (name) {
                case "shape" -> Group.SHAPE;
                case "iconColor" -> Group.ICON_COLOR;
                case "textColor" -> Group.TEXT_COLOR;
                case "mouseCursor" -> Group.MOUSE_CURSOR;
                case "titleTextStyle" -> Group.TITLE_STYLE;
                case "subtitleTextStyle" -> Group.SUBTITLE_STYLE;
                case "leadingAndTrailingTextStyle" -> Group.LEADING_TRAILING_STYLE;
                case "internalAddSemanticForOnTap" -> Group.ACCESSIBILITY;
                case "isThreeLine", "dense", "visualDensity", "contentPadding", "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight", "titleAlignment" -> Group.LAYOUT;
                case "enabled", "onTap", "onLongPress", "onFocusChange", "selected", "focusNode", "autofocus", "enableFeedback", "statesController" -> Group.BEHAVIOR;
                default -> Group.APPEARANCE;
            };
            String help = switch (name) {
                case "isThreeLine" -> "Unset or null preserves ListTileTheme. Explicit true requires a nonempty Subtitle slot; no subtitle or content is fabricated.";
                case "dense" -> "Unset or null inherits ListTileTheme. Dense overrides title/subtitle font sizes to 13/12 and uses compact SDK geometry.";
                case "visualDensity" -> "Strict VisualDensity reference, exclusive with local axes. Unset preserves ListTileTheme then Theme.visualDensity.";
                case "shape" -> "Strict ShapeBorder reference, exclusive with all local shape fields. Unset preserves ListTileTheme then Border(); shape does not add clipping.";
                case "style" -> "ListTileStyle.list or drawer; unset inherits local and global ListTileTheme. Material 2 defaults depend on this style.";
                case "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle" -> "Strict TextStyle reference, exclusive with this family's local leaves. This is a complete replacement, not an implicit merge with component defaults. The SDK subsequently overrides resolved text color and dense title/subtitle size.";
                case "contentPadding" -> "Signed physical/directional insets or a strict EdgeInsetsGeometry reference. SafeArea takes the maximum of each resolved minimum and system padding, so negative minima are legal. Unset preserves ListTileTheme and actual M2/M3 defaults.";
                case "enabled" -> "Unset uses true. False disables SDK tap/long-press activation but retains supplied callback arguments and their semantic-button presence.";
                case "selected", "autofocus" -> "Unset uses false. Stored intent is passed directly to the real SDK widget.";
                case "onTap", "onLongPress", "onFocusChange" -> "Optional explicit no-op, null, or strict typed callback reference/factory. Unset omits the argument. No callback is injected at creation or removed when disabled; isolated Canvas never executes project callbacks.";
                case "mouseCursor" -> "All 41 reviewed cursor presets or strict MouseCursor reference, including WidgetStateMouseCursor. The SDK resolves only disabled when !enabled or both gesture callbacks are null; selected is not included in the pinned implementation.";
                case "focusNode", "statesController" -> "Strict typed project reference/factory. Isolated Canvas uses owned SDK state rather than executing project objects.";
                case "enableFeedback" -> "Unset/null inherits ListTileTheme then true; explicit false disables SDK gesture feedback.";
                case "horizontalTitleGap", "minVerticalPadding", "minLeadingWidth", "minTileHeight" -> "Signed finite number, positive/negative Infinity, NaN, or explicit null. No constructor sign constraint exists. Unset/null preserves the SDK theme/default; unsafe mounted geometry is diagnosed only in Canvas.";
                case "titleAlignment" -> "threeLine, titleHeight, top, center or bottom. Unset inherits ListTileTheme then threeLine in M3 or titleHeight in M2.";
                case "internalAddSemanticForOnTap" -> "Unset uses true. True supplies button semantics whenever onTap or onLongPress is non-null, including when Enabled is false; false suppresses that explicit semantic flag.";
                case "iconColor", "textColor" -> "Literal/theme color or strict Color reference (including WidgetStateColor). Plain colors and stateful colors have distinct disabled/selected fallback behavior. Whole value is exclusive with local state entries.";
                default -> "Literal/theme color or strict Color reference. Unset preserves the actual ListTileTheme/SDK fallback; no component default is synthesized.";
            };
            add(values, name, group, help, order++);
        }
        for (String axis : List.of("Horizontal", "Vertical")) add(values, "visualDensity" + axis, Group.LAYOUT,
                "Local density axis from -4 to 4; the missing peer is zero. Both omitted preserve theme density. Exclusive with the whole VisualDensity reference.", order++);
        for (String name : builtInShapePropertyNames()) {
            var shared = CardWidgetPropertySchema.definitions().get(name);
            values.put(name, new Definition(Group.SHAPE, shared.displayName(), shared.description().replace("Card", "ListTile"), shared.dartName(), order++));
        }
        for (String family : styleFamilies()) {
            Group group = family.equals("titleTextStyle") ? Group.TITLE_STYLE : family.equals("subtitleTextStyle") ? Group.SUBTITLE_STYLE : Group.LEADING_TRAILING_STYLE;
            for (String name : localTextStyleProperties(family)) {
                var shared = textStyleBinding(new PropertyName(name)).orElseThrow();
                values.put(name, new Definition(group, shared.displayName(), shared.description()
                        + " Local ListTile TextStyle replacement; SDK color and dense-size copyWith behavior remains authoritative. Exclusive with the whole style reference.", shared.dartName(), order++));
            }
        }
        for (String family : stateColorFamilies()) for (String name : colorStateProperties(family)) add(values, name,
                family.equals("iconColor") ? Group.ICON_COLOR : Group.TEXT_COLOR,
                "Non-null local state color. Default is required whenever a local map exists; no fallback color is invented. First match: disabled/error/dragged/pressed/selected/scrolledUnder/hovered/focused/default. ListTile requests only disabled/selected.", order++);
        for (String name : mouseCursorStateProperties()) add(values, name, Group.MOUSE_CURSOR,
                "Non-null cursor preset or strict MouseCursor reference. Local Default is required; no cursor is invented. Closed first-match state priority matches the color maps, but ListTile requests only disabled.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != CONSTRUCTOR_PROPERTY_COUNT + SLOT_COUNT) throw new ExceptionInInitializerError("ListTile schema count/order mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help, int order) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, order));
    }
}
