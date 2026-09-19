package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.util.List;

/** Complete Flutter 3.44.8 SliverPadding constructor, apart from shared model identity/key. */
public final class SliverPaddingWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverPadding");
    public static final PropertyName PADDING = new PropertyName("padding");
    public static final String DESCRIPTION = "Required non-negative EdgeInsetsGeometry. Edit physical left/top/right/bottom or directional start/top/end/bottom; start/end resolve through the nearest Directionality. A typed project reference/getter/factory also supports mixed geometry; its runtime non-negative contract remains the application's responsibility. Isolated Canvas cannot evaluate project geometry and labels its zero-padding approximation. The initial 16 pixels on each side are a Designer preset, not a Flutter default. Unset/null is not allowed.";
    public static PropertyDefinition padding() {
        var side = BigDecimal.valueOf(16);
        return new PropertyDefinition(PADDING, DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.EdgeInsetsValues(true),
                        new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry")),
                java.util.Optional.of(new PropertyValue.EdgeInsetsValue(side, side, side, side)));
    }
    private SliverPaddingWidgetPropertySchema() {}
}

