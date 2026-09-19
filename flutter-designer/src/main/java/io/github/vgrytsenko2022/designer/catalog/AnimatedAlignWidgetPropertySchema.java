package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 AnimatedAlign constructor. */
public final class AnimatedAlignWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedAlign");
    public static final String DESCRIPTION = "Animates child alignment and nonnegative width/height factors. "
            + "Physical or directional alignment and Duration are required; creation uses center and 300 ms. "
            + "Child is optional. On end fires on completed transitions, not initial mount. Canvas never executes project code.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("alignment", "Alignment", "Required physical Alignment, RTL-aware AlignmentDirectional, or typed AlignmentGeometry reference/factory. Finite coordinates outside [-1,1] are allowed. Creation uses center; Canvas uses center for project-owned alignment."),
        new Field("widthFactor", "Width factor", "Optional nonnegative finite multiplier of the child's width, or null. Omission uses native null. Flutter 3.44.8 may retain an existing factor tween when changed back to null until remount."),
        new Field("heightFactor", "Height factor", "Optional nonnegative finite multiplier of the child's height, or null. Omission uses native null. Flutter 3.44.8 may retain an existing factor tween when changed back to null until remount."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference/factory. Omission uses linear. Canvas uses linear for project-owned curves."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation uses 300000; zero completes immediately. Canvas uses 300000 for project-owned values."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Runs after each completed transition, not initial mount or unchanged target. Configure the handler in Events; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
            new PropertyDefinition(new PropertyName("alignment"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.AlignmentGeometryValues(),
                    new PropertyValueConstraint.DartObjectReferenceValues("AlignmentGeometry")),
                Optional.of(new PropertyValue.AlignmentGeometryValue(
                    PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL, BigDecimal.ZERO, BigDecimal.ZERO))),
            factor("widthFactor", 1), factor("heightFactor", 2),
            at(animation.get(1), 3), at(animation.get(2), 4), at(animation.get(3), 5));
    }
    private static PropertyDefinition factor(String name, int order) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, false),
            List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, new BigInteger("9007199254740991")),
                new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty());
    }
    private static PropertyDefinition at(PropertyDefinition property, int order) {
        return new PropertyDefinition(property.name(), DartParameter.named(order, property.parameter().required()),
            property.constraints(), property.creationDefault());
    }
    private AnimatedAlignWidgetPropertySchema() {}
}
