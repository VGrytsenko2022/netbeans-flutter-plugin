package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.List;

/** Complete pinned Flutter 3.44.8 AnimatedPadding constructor, including inherited animation arguments. */
public final class AnimatedPaddingWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedPadding");
    public static final String DESCRIPTION = "Animates nonnegative physical or RTL-aware directional padding. "
            + "Padding and Duration are required; creation uses 16 pixels per side and 300 ms. Child is optional. "
            + "On end fires on completed transitions. Canvas never executes project-owned references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("padding", "Padding", "Required nonnegative EdgeInsets, RTL-aware EdgeInsetsDirectional, or typed EdgeInsetsGeometry reference/getter/factory, including mixed geometry. Creation and isolated custom-geometry preview use 16 pixels per side. Project-owned geometry must satisfy the nonnegative runtime contract."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference/factory. Omission and custom-curve preview use linear. Native Flutter clamps interpolated padding to nonnegative values, including overshooting curves."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation and custom-duration preview use 300000; zero completes immediately."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Runs on completed transitions, not initial mount or unchanged target. Configure the handler in Events; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(SliverPaddingWidgetPropertySchema.padding(), animation.get(1), animation.get(2), animation.get(3));
    }
    private AnimatedPaddingWidgetPropertySchema() {}
}

