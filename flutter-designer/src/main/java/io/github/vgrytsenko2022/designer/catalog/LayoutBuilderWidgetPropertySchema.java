package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 LayoutBuilder constructor. */
public final class LayoutBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.LayoutBuilder");
    public static final String CALLBACK_TYPE = "LayoutWidgetBuilder";
    public static final String DESCRIPTION = "Builds a box subtree at layout time from BuildContext and BoxConstraints. "
            + "Required Builder accepts the empty preset or a typed project function/getter/factory. "
            + "The result must produce a box, not a sliver; Dart's Widget return type cannot prove this runtime rule. "
            + "Empty returns SizedBox.shrink(), respecting the parent's constraints. "
            + "There is no Child slot: the callback owns the subtree. Intrinsic/dry layout is not supported by Flutter. "
            + "Canvas never executes project code and labels custom responsive content as unavailable. "
            + "Key uses shared stable identity; there are no native events.";
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("builder"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.StringPattern("empty", "empty box preset"),
                        new PropertyValueConstraint.DartObjectReferenceValues(CALLBACK_TYPE)),
                Optional.of(new PropertyValue.StringValue("empty"))));
    }
    private LayoutBuilderWidgetPropertySchema() {}
}

