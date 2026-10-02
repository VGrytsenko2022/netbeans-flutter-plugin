package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 Card variants and editable standard ShapeBorder constructors. */
public final class CardWidgetPropertySchema {
    public static final WidgetTypeId CARD_TYPE = new WidgetTypeId("flutter.material.Card");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 31;
    public static final int SLOT_COUNT = 1;
    private static final List<String> SHAPE_KINDS = List.of("roundedRectangle", "beveledRectangle",
            "continuousRectangle", "roundedSuperellipse", "circle", "oval", "stadium", "linear", "star", "polygon");
    public enum Group {
        APPEARANCE("cardAppearance", "Appearance", "Card surface, elevation and border painting."),
        LAYOUT("cardLayout", "Layout", "Outer physical or directional margin."),
        BEHAVIOR("cardBehavior", "Behavior", "Constructor variant, clipping and semantics."),
        SHAPE("cardShape", "Shape", "Ten editable shape constructors or an analyzed ShapeBorder reference.");
        private final String setName;
        private final String displayName;
        private final String description;
        Group(String setName, String displayName, String description) {
            this.setName = setName; this.displayName = displayName; this.description = description;
        }
        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }
    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group); Objects.requireNonNull(displayName);
            Objects.requireNonNull(description); Objects.requireNonNull(dartName);
            if (dartOrder < 0) throw new IllegalArgumentException("Negative Card property order");
        }
    }
    private static final Map<String, Definition> DEFINITIONS = definitions();
    private CardWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name).value()));
    }
    public static List<String> shapeKinds() { return SHAPE_KINDS; }
    /** Includes kind itself, but excludes the mutually exclusive typed reference. */
    public static List<String> builtInShapePropertyNames() {
        return DEFINITIONS.keySet().stream().filter(name -> name.startsWith("shape") && !name.equals("shape")).toList();
    }
    public static boolean isShapeDetailProperty(String name) {
        return builtInShapePropertyNames().contains(name) && !name.equals("shapeKind");
    }
    public static boolean shapePropertyAppliesToKind(String name, String kind) {
        if (!SHAPE_KINDS.contains(kind)) return false;
        if (name.equals("shapeKind") || name.startsWith("shapeSide")) return true;
        return switch (name) {
            case "shapeRadius" -> List.of("roundedRectangle", "beveledRectangle", "continuousRectangle", "roundedSuperellipse").contains(kind);
            case "shapeCircleEccentricity" -> kind.equals("circle") || kind.equals("oval");
            case "shapePoints", "shapePointRounding", "shapeRotation", "shapeSquash" -> kind.equals("star") || kind.equals("polygon");
            case "shapeInnerRadiusRatio", "shapeValleyRounding" -> kind.equals("star");
            case "shapeStartSize", "shapeStartAlignment", "shapeEndSize", "shapeEndAlignment",
                    "shapeTopSize", "shapeTopAlignment", "shapeBottomSize", "shapeBottomAlignment" -> kind.equals("linear");
            default -> false;
        };
    }
    public static String preferredShapeKindForProperty(String name) {
        if (!isShapeDetailProperty(name)) throw new IllegalArgumentException("Not a Card shape detail: " + name);
        if (name.equals("shapeCircleEccentricity")) return "circle";
        if (List.of("shapePoints", "shapeInnerRadiusRatio", "shapePointRounding", "shapeValleyRounding", "shapeRotation", "shapeSquash").contains(name)) return "star";
        if (!name.startsWith("shapeSide") && !name.equals("shapeRadius")) return "linear";
        return "roundedRectangle";
    }
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "color", Group.APPEARANCE, "Color", "Literal or reviewed ColorScheme role; unset inherits CardTheme then the selected variant's M2/M3 defaults.", 0);
        add(values, "shadowColor", Group.APPEARANCE, "Shadow color", "Shadow color; unset inherits CardTheme and the Material theme.", 1);
        add(values, "surfaceTintColor", Group.APPEARANCE, "Surface tint color", "Optional elevation overlay; unset inherits CardTheme, with transparent M3 defaults. Explicit transparent disables the inherited tint.", 2);
        add(values, "elevation", Group.APPEARANCE, "Elevation", "Finite non-negative elevation; unset is theme dependent (M3 elevated 1, filled/outlined 0; M2 1).", 3);
        add(values, "borderOnForeground", Group.APPEARANCE, "Border on foreground", "Whether the shape border paints in front of the child; unset uses true.", 4);
        add(values, "margin", Group.LAYOUT, "Margin", "Non-negative physical or directional outer insets; unset inherits CardTheme then four logical pixels on each side.", 5);
        add(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior", "Unset inherits CardTheme then Clip.none. A shape alone does not clip the child.", 6);
        add(values, "semanticContainer", Group.BEHAVIOR, "Semantic container", "Unset uses true and groups child semantics; false exposes explicit child semantic nodes.", 7);
        add(values, "variant", Group.BEHAVIOR, "Variant", "Required Designer constructor selector: elevated, filled or outlined. All are const constructors; M2 renders the three with the same defaults. Cannot be unset.", 9);
        add(values, "shape", Group.SHAPE, "Shape reference", "Analyzed ShapeBorder project/package reference or zero-argument factory. Mutually exclusive with all built-in shape fields. Custom references generate exactly but cannot execute in the isolated Canvas preview.", 10);
        add(values, "shapeKind", Group.SHAPE, "Shape kind", "Optional built-in ShapeBorder constructor; unset with no shape reference inherits CardTheme and the variant's default. Reset removes all built-in shape details.", 11);
        add(values, "shapeRadius", Group.SHAPE, "Radius", "Physical or directional elliptical corner radii for the four rectangular shapes. Unset uses that shape constructor's zero radius, not the Card theme radius.", 12);
        add(values, "shapeSideColor", Group.SHAPE, "Side color", "Literal or reviewed semantic BorderSide color. Setting any side field constructs BorderSide (otherwise the shape constructor keeps BorderSide.none).", 13);
        add(values, "shapeSideWidth", Group.SHAPE, "Side width", "Finite non-negative BorderSide width; explicit zero is a hairline.", 14);
        add(values, "shapeSideStyle", Group.SHAPE, "Side style", "BorderStyle.none or solid. Omitted within an explicit side uses solid.", 15);
        add(values, "shapeSideStrokeAlign", Group.SHAPE, "Side stroke align", "Finite stroke alignment; -1 inside, 0 centered, 1 outside. Values outside this conventional interval are supported by the SDK.", 16);
        add(values, "shapeCircleEccentricity", Group.SHAPE, "Circle / oval eccentricity", "Value from zero to one for CircleBorder or OvalBorder; omitted defaults are respectively zero and one.", 17);
        add(values, "shapePoints", Group.SHAPE, "Points / sides", "Finite number at least two, including fractional values; maps to StarBorder.points or StarBorder.polygon sides, default five. Very large counts can exceed the isolated preview's resource budget without changing source values.", 18);
        add(values, "shapeInnerRadiusRatio", Group.SHAPE, "Inner radius ratio", "Star-only ratio from zero to one; default 0.4. Polygon computes its own ratio.", 19);
        add(values, "shapePointRounding", Group.SHAPE, "Point rounding", "Star/polygon rounding from zero to one, default zero. For stars its sum with valley rounding must not exceed one.", 20);
        add(values, "shapeValleyRounding", Group.SHAPE, "Valley rounding", "Star-only valley rounding from zero to one, default zero; point plus valley rounding must not exceed one.", 21);
        add(values, "shapeRotation", Group.SHAPE, "Rotation", "Finite clockwise rotation in DEGREES for star/polygon, default zero.", 22);
        add(values, "shapeSquash", Group.SHAPE, "Squash", "Star/polygon aspect-ratio adaptation from zero to one, default zero.", 23);
        int order = 24;
        for (String edge : List.of("Start", "End", "Top", "Bottom")) {
            add(values, "shape" + edge + "Size", Group.SHAPE, edge + " edge size", "LinearBorder edge length fraction from zero to one. Either edge leaf creates that edge (size default one, alignment default zero); both unset omit it. Start/end follow text direction.", order++);
            add(values, "shape" + edge + "Alignment", Group.SHAPE, edge + " edge alignment", "Finite LinearBorderEdge alignment; conventional range -1 (start) to 1 (end), zero centered. Horizontal edge alignment follows text direction. SDK accepts finite values outside the conventional range.", order++);
        }
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("Card schema count mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String label, String help, int order) {
        if (values.putIfAbsent(name, new Definition(group, label, help, name, order)) != null) throw new IllegalArgumentException("Duplicate Card field " + name);
    }
}
