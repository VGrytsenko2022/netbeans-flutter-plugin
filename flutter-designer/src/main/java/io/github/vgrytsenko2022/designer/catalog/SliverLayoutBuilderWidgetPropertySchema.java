package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 SliverLayoutBuilder constructor. */
public final class SliverLayoutBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverLayoutBuilder");
    public static final String CALLBACK_TYPE = "SliverLayoutWidgetBuilder";
    public static final String DESCRIPTION = "Builds a sliver at layout time from BuildContext and SliverConstraints. "
            + "Required Builder accepts the empty preset or a typed project function/getter/factory. "
            + "The function must return a sliver, not a box widget; Dart's Widget return type cannot prove that runtime rule. "
            + "The empty preset returns a zero-extent SliverToBoxAdapter. "
            + "There is no child/sliver slot: the callback owns the subtree. "
            + "Canvas never executes project code and labels custom builder content as unavailable. "
            + "Key uses the shared stable widget identity; this widget has no native events.";
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("builder"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.StringPattern("empty", "empty sliver preset"),
                        new PropertyValueConstraint.DartObjectReferenceValues(CALLBACK_TYPE)),
                Optional.of(new PropertyValue.StringValue("empty"))));
    }
    private SliverLayoutBuilderWidgetPropertySchema() {}
}

