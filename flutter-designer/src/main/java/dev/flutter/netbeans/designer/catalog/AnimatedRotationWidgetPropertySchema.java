package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 AnimatedRotation constructor and inherited arguments. */
public final class AnimatedRotationWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedRotation");
    public static final String DESCRIPTION = "Animates a child's paint rotation without changing layout. Turns are signed revolutions: 1 is 360 degrees, positive clockwise even in RTL. "
            + "Targets are not normalized or shortened: 0 to 1 animates a full turn. Only Turns animates; physical Alignment and Filter quality update immediately. "
            + "Turns and Duration are required; creation uses 0 and 300 ms. Child is optional. Canvas never executes project-owned references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("turns", "Turns", "Required finite signed revolutions or typed double reference/getter/factory. 1 = 360 degrees; 0.25 = 90 degrees clockwise, also in RTL. Negative values rotate counterclockwise. No modulo or shortest-path conversion; zero is identity, not invisible. Layout is unchanged. Creation and custom-turns preview use 0."),
        new Field("alignment", "Alignment", "Optional physical Alignment or typed Alignment reference/getter/factory. Omission and custom-alignment preview use center. Finite coordinates outside [-1,1] are allowed; AlignmentDirectional and null are not. Updates immediately without a separate tween."),
        new Field("filterQuality", "Filter quality", "Optional none, low, medium, high or explicit null. Omission uses native null. Flutter applies the configured filter only while rotation is animating, not on initial mount or after completion. Changes alone do not start an animation."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference/factory. Omission and custom-curve preview use linear. Native overshoot is preserved, not clamped."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation and custom-duration preview use 300000; zero completes immediately."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Runs on completed rotation transitions, not initial mount or unchanged target. Configure the handler in Events; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
            new PropertyDefinition(new PropertyName("turns"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.IntegerRange(new BigInteger("-9007199254740991"), new BigInteger("9007199254740991")),
                    new PropertyValueConstraint.DoubleRange(null, true, null, true),
                    new PropertyValueConstraint.DartObjectReferenceValues("double")),
                Optional.of(new PropertyValue.DoubleValue(BigDecimal.ZERO))),
            new PropertyDefinition(new PropertyName("alignment"), DartParameter.named(1, false),
                List.of(new PropertyValueConstraint.AlignmentValues(), new PropertyValueConstraint.DartObjectReferenceValues("Alignment")), Optional.empty()),
            new PropertyDefinition(new PropertyName("filterQuality"), DartParameter.named(2, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "FilterQuality"),
                    List.of("none", "low", "medium", "high")), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()),
            at(animation.get(1), 3), at(animation.get(2), 4), at(animation.get(3), 5));
    }
    private static PropertyDefinition at(PropertyDefinition property, int order) {
        return new PropertyDefinition(property.name(), DartParameter.named(order, property.parameter().required()),
            property.constraints(), property.creationDefault());
    }
    private AnimatedRotationWidgetPropertySchema() {}
}


