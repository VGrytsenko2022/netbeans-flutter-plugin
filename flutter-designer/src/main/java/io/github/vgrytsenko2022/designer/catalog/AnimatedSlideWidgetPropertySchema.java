package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Complete pinned Flutter 3.44.8 AnimatedSlide constructor and inherited arguments. */
public final class AnimatedSlideWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedSlide");
    public static final String DESCRIPTION = "Animates a child's physical offset as fractions of its size, without changing layout. "
            + "Positive X moves right even in RTL. Offset and Duration are required; creation uses zero and 300 ms. "
            + "Child is optional. Canvas never executes project-owned references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("offset", "Offset", "Required finite signed Offset or typed Offset reference/getter/factory. X and Y are fractions of child width and height, not pixels. Values outside [-1,1] are allowed; positive X moves right in both LTR and RTL. Creation and custom-offset preview use zero."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference/factory. Omission and custom-curve preview use linear. Native overshoot is preserved, not clamped."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation and custom-duration preview use 300000; zero completes immediately."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Runs on completed transitions, not initial mount or unchanged target. Configure the handler in Events; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(new PropertyDefinition(new PropertyName("offset"), DartParameter.named(0, true),
            List.of(new PropertyValueConstraint.OffsetValues(), new PropertyValueConstraint.DartObjectReferenceValues("Offset")),
            Optional.of(new PropertyValue.OffsetValue(BigDecimal.ZERO, BigDecimal.ZERO))),
            animation.get(1), animation.get(2), animation.get(3));
    }
    private AnimatedSlideWidgetPropertySchema() {}
}

