package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete Flutter 3.44.8 PositionedTransition projection, excluding shared Key. */
public final class PositionedTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.PositionedTransition");
    public static final String DESCRIPTION = "Animates a required Child inside direct Stack.children using Animation<RelativeRect>. "
        + "Wrap an existing Stack child. Local physical left/top/right/bottom insets create a stopped animation; all four zero fills the Stack. "
        + "Project references own animation lifetime and preview at zero insets. Stack requires finite bounds when all children are positioned.";
    public static final List<String> EDGES = List.of("rectLeft", "rectTop", "rectRight", "rectBottom");
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("rect", "Rectangle animation", "Required local physical RelativeRect insets or strict non-null Animation<RelativeRect> reference/getter/factory. Local values create AlwaysStoppedAnimation<RelativeRect>. No fractions or LTWH rectangle conversion. Source preview uses zero insets; local draft edges are retained."),
        new Field("rectLeft", "Rect left", edgeDescription("left")),
        new Field("rectTop", "Rect top", edgeDescription("top")),
        new Field("rectRight", "Rect right", edgeDescription("right")),
        new Field("rectBottom", "Rect bottom", edgeDescription("bottom")));
    private static String edgeDescription(String edge) {
        return "Finite signed physical " + edge + " inset from the Stack edge in logical pixels. RTL does not reverse it. "
            + "Editing selects local rectangle atomically. Negative insets extend outside the Stack; overconstrained derived sizes clamp to zero. "
            + "The Stack controls clipping and ancestor pointer bounds.";
    }
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        result.add(new PropertyDefinition(new PropertyName("rect"), DartParameter.named(0, true),
            List.of(new PropertyValueConstraint.StringPattern("local", "local physical RelativeRect insets"),
                new PropertyValueConstraint.DartObjectReferenceValues("Animation<RelativeRect>")),
            Optional.of(new PropertyValue.StringValue("local"))));
        for (String name : EDGES) result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), true),
            List.of(new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"), new BigInteger("9007199254740991")),
                new PropertyValueConstraint.DoubleRange(null, true, null, true)),
            Optional.of(new PropertyValue.DoubleValue(BigDecimal.ZERO))));
        return List.copyOf(result);
    }
    private PositionedTransitionWidgetPropertySchema() {}
}
