package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/** Complete Flutter 3.44.8 parent-data constructor: key, flex and required sliver. */
public final class SliverCrossAxisExpandedWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverCrossAxisExpanded");
    public static final String DESCRIPTION = "Allocates a proportional share of remaining cross-axis space. "
            + "Requires a direct SliverCrossAxisGroup.slivers parent and one existing sliver to wrap. "
            + "Flex is a required positive integer; the Designer starts at 1 (not an SDK default). "
            + "The child cannot be cleared or moved out; replace it atomically or undo the wrapping operation.";
    public static PropertyDefinition flex() {
        return new PropertyDefinition(new PropertyName("flex"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER)),
                Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE)));
    }
    public static List<PropertyDefinition> properties() { return List.of(flex()); }
    private SliverCrossAxisExpandedWidgetPropertySchema() {}
}
