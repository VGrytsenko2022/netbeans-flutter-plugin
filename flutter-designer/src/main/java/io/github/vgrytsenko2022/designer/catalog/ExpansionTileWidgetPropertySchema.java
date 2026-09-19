package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 ExpansionTile constructor and compound fields. */
public final class ExpansionTileWidgetPropertySchema {
    public static final WidgetTypeId EXPANSION_TILE_TYPE = new WidgetTypeId("flutter.material.ExpansionTile");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 76;
    public static final int DIRECT_PROPERTY_COUNT = 28;
    public static final int SLOT_COUNT = 5;
    public enum Group {
        BEHAVIOR("Behavior"), APPEARANCE("Appearance"), LAYOUT("Layout"), ACCESSIBILITY("Accessibility"),
        SHAPE("Shape"), COLLAPSED_SHAPE("CollapsedShape"), ANIMATION("Animation");
        private final String suffix;
        Group(String suffix) { this.suffix = suffix; }
        public String setName() { return "expansionTile" + suffix; }
        public String displayName() { return suffix.replaceAll("([a-z])([A-Z])", "$1 $2"); }
        public String description() { return "Flutter ExpansionTile " + displayName() + "."; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition { Objects.requireNonNull(group); Objects.requireNonNull(displayName); Objects.requireNonNull(description); Objects.requireNonNull(dartName); if (dartOrder < 0) throw new IllegalArgumentException("Negative property order"); }
    }
    private static final List<String> CURVES = List.of("linear", "decelerate", "fastLinearToSlowEaseIn", "fastEaseInToSlowEaseOut",
            "ease", "easeIn", "easeInToLinear", "easeInSine", "easeInQuad", "easeInCubic", "easeInQuart", "easeInQuint", "easeInExpo", "easeInCirc", "easeInBack",
            "easeOut", "linearToEaseOut", "easeOutSine", "easeOutQuad", "easeOutCubic", "easeOutQuart", "easeOutQuint", "easeOutExpo", "easeOutCirc", "easeOutBack",
            "easeInOut", "easeInOutSine", "easeInOutQuad", "easeInOutCubic", "easeInOutCubicEmphasized", "easeInOutQuart", "easeInOutQuint", "easeInOutExpo", "easeInOutCirc", "easeInOutBack",
            "fastOutSlowIn", "slowMiddle", "bounceIn", "bounceOut", "bounceInOut", "elasticIn", "elasticOut", "elasticInOut");
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private ExpansionTileWidgetPropertySchema() { }
    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(name)); }
    public static Optional<Definition> find(PropertyName name) { return find(name.value()); }
    public static List<String> colorProperties() { return List.of("backgroundColor", "collapsedBackgroundColor", "textColor", "collapsedTextColor", "iconColor", "collapsedIconColor", "splashColor"); }
    public static List<String> shapeKinds() { return CardWidgetPropertySchema.shapeKinds(); }
    public static List<String> shapeFamilies() { return List.of("shape", "collapsedShape"); }
    public static List<String> shapeLocalProperties(String family) {
        if (!shapeFamilies().contains(family)) throw new IllegalArgumentException("Not an ExpansionTile shape family: " + family);
        return CardWidgetPropertySchema.builtInShapePropertyNames().stream().map(name -> family + name.substring(5)).toList();
    }
    public static List<String> builtInShapePropertyNames() { return shapeFamilies().stream().flatMap(family -> shapeLocalProperties(family).stream()).toList(); }
    public static Optional<String> shapeFamily(PropertyName name) { return shapeFamily(name.value()); }
    public static Optional<String> shapeFamily(String name) { return shapeFamilies().stream().filter(family -> shapeLocalProperties(family).contains(name)).findFirst(); }
    public static Optional<String> shapeSourceName(PropertyName name) { return shapeSourceName(name.value()); }
    public static Optional<String> shapeSourceName(String name) { return shapeFamily(name).map(family -> "shape" + name.substring(family.length())); }
    public static boolean isShapeDetailProperty(String name) { return shapeSourceName(name).map(CardWidgetPropertySchema::isShapeDetailProperty).orElse(false); }
    public static boolean shapePropertyAppliesToKind(String name, String kind) { return shapeSourceName(name).map(source -> CardWidgetPropertySchema.shapePropertyAppliesToKind(source, kind)).orElse(false); }
    public static String preferredShapeKindForProperty(String name) { return CardWidgetPropertySchema.preferredShapeKindForProperty(shapeSourceName(name).orElseThrow()); }
    public static List<String> animationStylePresets() { return List.of("noAnimation"); }
    public static List<String> curvePresets() { return CURVES; }
    public static List<String> animationStyleLocalProperties() { return List.of("expansionAnimationStyleDurationUs", "expansionAnimationStyleCurve", "expansionAnimationStyleReverseDurationUs", "expansionAnimationStyleReverseCurve"); }
    public static List<String> animationDurationProperties() { return List.of("expansionAnimationStyleDurationUs", "expansionAnimationStyleReverseDurationUs"); }
    public static List<String> animationCurveProperties() { return List.of("expansionAnimationStyleCurve", "expansionAnimationStyleReverseCurve"); }
    public static List<String> nonNullableBooleanProperties() { return List.of("showTrailingIcon", "initiallyExpanded", "maintainState", "enabled", "internalAddSemanticForOnTap"); }

    private static Map<String, Definition> createDefinitions() {
        var values = new LinkedHashMap<String, Definition>();
        for (String name : List.of("onExpansionChanged", "showTrailingIcon", "initiallyExpanded", "maintainState", "tilePadding",
                "expandedCrossAxisAlignment", "expandedAlignment", "childrenPadding", "backgroundColor", "collapsedBackgroundColor",
                "textColor", "collapsedTextColor", "iconColor", "collapsedIconColor", "shape", "collapsedShape", "clipBehavior",
                "controlAffinity", "controller", "dense", "splashColor", "visualDensity", "minTileHeight", "enableFeedback", "enabled",
                "expansionAnimationStyle", "internalAddSemanticForOnTap", "statesController")) {
            Group group = switch (name) {
                case "shape" -> Group.SHAPE; case "collapsedShape" -> Group.COLLAPSED_SHAPE;
                case "expansionAnimationStyle" -> Group.ANIMATION; case "internalAddSemanticForOnTap" -> Group.ACCESSIBILITY;
                case "tilePadding", "childrenPadding", "expandedCrossAxisAlignment", "expandedAlignment", "dense", "visualDensity", "minTileHeight", "controlAffinity" -> Group.LAYOUT;
                case "backgroundColor", "collapsedBackgroundColor", "textColor", "collapsedTextColor", "iconColor", "collapsedIconColor", "splashColor" -> Group.APPEARANCE;
                default -> Group.BEHAVIOR;
            };
            String help = switch (name) {
                case "onExpansionChanged" -> "Optional no-op, explicit null or strict ValueChanged<bool> reference/factory. Receives true/false when expansion starts, including controller-driven changes. Omission leaves notification unset, not disabled expansion; Canvas never executes project callbacks.";
                case "showTrailingIcon" -> "Default true. False suppresses the entire trailing slot, including a supplied trailing widget. It does not suppress the leading expansion arrow when affinity is leading; stored trailing content is preserved.";
                case "initiallyExpanded" -> "Initial seed only, default false; read at State initialization and ignored on ordinary rebuild. True expands even a supplied controller before the expansion listener is installed. Not a controlled State value or State consumer; use a project-owned ExpansibleController for runtime changes.";
                case "maintainState" -> "Default false: collapsed children are removed after collapse completes. True retains them offstage with tickers disabled. The generated child list is never cleared; a required Title is separate from Children.";
                case "controller" -> "Optional strict ExpansibleController reference/factory or null; the deprecated ExpansionTileController typedef is accepted by Dart type analysis. Project lifecycle remains user-owned. A factory can create a fresh controller on each rebuild; the SDK only disposes its internally created controller.";
                case "statesController" -> "Optional WidgetStatesController reference/factory or null for header interaction states, not expanded state. Project-owned lifecycle; omission uses the ListTile-owned controller.";
                case "enabled" -> "Default true. False disables header interaction and appearance, but a supplied controller can still expand/collapse the tile; callbacks are not removed.";
                case "enableFeedback" -> "Nullable SDK field with constructor default true. Omission uses true; explicit null asks the backing ListTile/theme for its fallback.";
                case "expandedCrossAxisAlignment" -> "Nullable CrossAxisAlignment start/end/center/stretch. Baseline is rejected by the actual SDK constructor assertion. Omission/null uses center; this aligns children inside the column.";
                case "expandedAlignment" -> "Physical/directional alignment or strict AlignmentGeometry reference/null, aligning the expanded column inside the tile. Omission/null inherits ExpansionTileTheme then center.";
                case "tilePadding", "childrenPadding" -> "Physical/directional signed insets, strict EdgeInsetsGeometry reference or explicit null. Omission/null inherits ExpansionTileTheme; tile padding defaults to horizontal 16 and children padding to zero. Unsafe mounted geometry is diagnosed without changing source.";
                case "shape", "collapsedShape" -> "Strict ShapeBorder reference/factory or explicit null, exclusive with this family's ten local shape constructors. Omission/null retains ExpansionTileTheme and expanded divider-border/collapsed transparent-border defaults. The other shape family is independent.";
                case "clipBehavior" -> "Nullable Clip. With an effective expanded or collapsed shape, omission/null inherits ExpansionTileTheme then antiAlias. Without a provided shape, the SDK's default decorated-border branch does not introduce that Material clip.";
                case "controlAffinity" -> "Nullable ListTileControlAffinity. Omission inherits ListTileTheme; platform/null defaults to trailing. Leading places the automatic rotating arrow before Title; explicit leading/trailing widgets can replace the automatic icon.";
                case "expansionAnimationStyle" -> "Whole strict AnimationStyle reference/factory, noAnimation preset or explicit null, exclusive with four local fields. Omission/null retains theme defaults (200ms and easeIn). Project code is not executed in Canvas. Flutter 3.44.8 ExpansionTile ignores reverseDuration even when set.";
                case "minTileHeight" -> "Nullable signed finite height, Infinity, negative Infinity or NaN; omission/null preserves the ListTile/theme minimum. Unsafe mounted layout is diagnosed in Canvas without rewriting Dart.";
                case "dense" -> "Nullable compact layout flag; omission/null inherits ListTileTheme.";
                case "visualDensity" -> "Whole strict VisualDensity reference/factory or explicit null; exclusive with local horizontal/vertical axes. Omission/null uses the ListTile/theme density.";
                case "internalAddSemanticForOnTap" -> "Default false. Forwarded to the backing ListTile semantic behavior; the outer ExpansionTile has platform-adaptive expansion semantics.";
                default -> "Nullable literal/theme color or strict Color reference/factory. Omission/null preserves the pinned ExpansionTileTheme/ListTile/Material fallback; expanded and collapsed colors remain independent.";
            };
            add(values, name, group, help);
        }
        for (String name : List.of("visualDensityHorizontal", "visualDensityVertical")) add(values, name, Group.LAYOUT, "Local VisualDensity axis -4..4; missing peer zero. Exclusive with whole visualDensity, including explicit null.");
        for (String family : shapeFamilies()) for (String name : shapeLocalProperties(family)) add(values, name,
                family.equals("shape") ? Group.SHAPE : Group.COLLAPSED_SHAPE,
                CardWidgetPropertySchema.definitions().get(shapeSourceName(name).orElseThrow()).description().replace("Card", "ExpansionTile " + family));
        for (String name : animationStyleLocalProperties()) add(values, name, Group.ANIMATION,
                (name.endsWith("Us") ? "Exact signed portable integer microseconds, strict Duration reference/factory or explicit null. Zero disables this duration; negative Duration is preserved but may be unsafe for a mounted animation. "
                        : "All 43 pinned Curves presets, strict Curve reference/factory or explicit null. Custom curves are never executed in Canvas; undershooting curves may be unsafe for expanded height. ")
                + (name.contains("ReverseDuration") ? "Flutter 3.44.8 ExpansionTile ignores reverseDuration; the field remains stored and generated exactly. " : "")
                + "Any local field builds AnimationStyle; omitted/null members use the SDK's individual theme/default fallback. Exclusive with whole expansionAnimationStyle.");
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("ExpansionTile property count: " + values.size());
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String help) {
        String label = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        values.put(name, new Definition(group, Character.toUpperCase(label.charAt(0)) + label.substring(1), help, name, SLOT_COUNT + values.size()));
    }
}
