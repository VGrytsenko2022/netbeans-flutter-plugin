package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 RelativePositionedTransition projection, except shared Key. */
public final class RelativePositionedTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.RelativePositionedTransition");
    public static final String DESCRIPTION = "Animates a required Child in direct Stack.children using Animation<Rect?> and a reference Size. "
        + "Rect is physical LTWH, not fractions. Size is a reference box, not a scale or layout constraint. "
        + "The native right/bottom offsets are reference size minus Rect right/bottom; changing actual Stack size changes child size. "
        + "A null animation value means Rect.zero, not an absent animation. Wrap an existing Stack child.";
    public static final List<String> RECT_FIELDS = List.of("rectLeft", "rectTop", "rectWidth", "rectHeight");
    public static final List<String> SIZE_FIELDS = List.of("sizeWidth", "sizeHeight");
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("rect", "Rectangle animation", "Required local physical LTWH rectangle, null-value stopped animation, or analyzer-verified Animation<Rect?> reference/getter/factory. The animation itself cannot be null. Inactive local drafts are retained. Project animation preview uses Rect.fromLTWH(0,0,48,48)."),
        new Field("rectLeft", "Rect left", rectDescription("left")),
        new Field("rectTop", "Rect top", rectDescription("top")),
        new Field("rectWidth", "Rect width", rectDescription("width")),
        new Field("rectHeight", "Rect height", rectDescription("height")),
        new Field("size", "Reference size", "Required local Size or typed non-null Size reference/getter/factory. This is a reference box at origin (0,0), not a scale and not actual Stack constraints. Project Size preview is Size(48,48)."),
        new Field("sizeWidth", "Reference width", sizeDescription("width")),
        new Field("sizeHeight", "Reference height", sizeDescription("height")));
    private static String rectDescription(String part) {
        return "Finite signed physical rectangle " + part + " in logical pixels. Editing selects local rectangle atomically, preserving the reference size. "
            + "RTL does not reverse coordinates. Signed dimensions are retained; native negative derived layout sizes clamp to zero.";
    }
    private static String sizeDescription(String part) {
        return "Finite signed reference Size " + part + ". Editing selects local Size atomically, preserving the animation source. "
            + "Zero and negative reference dimensions are legal native values; this does not resize the Stack. Creation: 48.";
    }
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        result.add(new PropertyDefinition(new PropertyName("rect"), DartParameter.named(0, true),
            List.of(new PropertyValueConstraint.StringPattern("(?:local|null)", "local rectangle or null animation value"),
                new PropertyValueConstraint.DartObjectReferenceValues("Animation<Rect?>")),
            Optional.of(new PropertyValue.StringValue("local"))));
        for (String name : RECT_FIELDS) result.add(number(name, result.size(), name.equals("rectLeft") || name.equals("rectTop") ? 0 : 48));
        result.add(new PropertyDefinition(new PropertyName("size"), DartParameter.named(5, true),
            List.of(new PropertyValueConstraint.StringPattern("local", "local reference Size"),
                new PropertyValueConstraint.DartObjectReferenceValues("Size")),
            Optional.of(new PropertyValue.StringValue("local"))));
        for (String name : SIZE_FIELDS) result.add(number(name, result.size(), 48));
        return List.copyOf(result);
    }
    private static PropertyDefinition number(String name, int order, int creation) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, true),
            List.of(new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"), new BigInteger("9007199254740991")),
                new PropertyValueConstraint.DoubleRange(null, true, null, true)),
            Optional.of(new PropertyValue.DoubleValue(BigDecimal.valueOf(creation))));
    }
    public static Optional<String> conflict(WidgetNode node) {
        if (!TYPE.equals(node.type()) || !(node.properties().get(new PropertyName("rect")) instanceof PropertyValue.StringValue mode)) return Optional.empty();
        boolean localSize = new PropertyValue.StringValue("local").equals(node.properties().get(new PropertyName("size")));
        for (var axis : List.of(List.of("rectLeft", "rectWidth", "sizeWidth"), List.of("rectTop", "rectHeight", "sizeHeight"))) {
            Double origin = mode.value().equals("null") ? Double.valueOf(0) : number(node, axis.get(0));
            Double extent = mode.value().equals("null") ? Double.valueOf(0) : number(node, axis.get(1));
            Double size = localSize ? number(node, axis.get(2)) : null;
            if (origin != null && extent != null && (!Double.isFinite(origin + extent)
                    || !Double.isFinite((origin + extent) - origin)
                    || size != null && !Double.isFinite(size - (origin + extent))))
                return Optional.of("Local Rect edges and reference Size offsets must remain finite; reduce " + String.join(", ", axis) + ".");
        }
        return Optional.empty();
    }
    private static Double number(WidgetNode node, String name) {
        var value = node.properties().get(new PropertyName(name));
        if (value instanceof PropertyValue.DoubleValue d) return d.value().doubleValue();
        if (value instanceof PropertyValue.IntegerValue i) return i.value().doubleValue();
        return null;
    }
    private RelativePositionedTransitionWidgetPropertySchema() {}
}
