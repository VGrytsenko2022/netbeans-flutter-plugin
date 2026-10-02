package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 constructor, with explicit box/sliver Designer projections. */
public final class DeviceOrientationBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.DeviceOrientationBuilder");
    public static final WidgetTypeId SLIVER_TYPE = new WidgetTypeId("flutter.widgets.DeviceOrientationBuilder.sliver");
    public static final String CALLBACK_TYPE = "OrientationWidgetBuilder";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || SLIVER_TYPE.equals(type); }
    public static String description(WidgetTypeId type) {
        return "Builds from BuildContext and MediaQuery.orientationOf(context), not parent layout constraints. "
                + "Requires a MediaQuery ancestor. Required Builder accepts Empty or a typed project function/getter/factory. "
                + (SLIVER_TYPE.equals(type)
                    ? "Sliver placement projection; Empty returns SliverToBoxAdapter(). The callback must produce a sliver here. "
                    : "Box placement projection; Empty returns SizedBox.shrink(). The callback must produce a box here. ")
                + "Both projections generate the same unnamed DeviceOrientationBuilder constructor, not a fictional SDK variant. "
                + "The Widget return type cannot prove the rendering protocol. There is no Child slot; the callback owns its subtree. "
                + "Build-time construction permits intrinsic layout when the returned box supports it. "
                + "Canvas never executes project code and labels custom content as unavailable. "
                + "Key uses shared stable identity; there are no native events.";
    }
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("builder"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.StringPattern("empty", "empty result matching the placement projection"),
                        new PropertyValueConstraint.DartObjectReferenceValues(CALLBACK_TYPE)),
                Optional.of(new PropertyValue.StringValue("empty"))));
    }
    private DeviceOrientationBuilderWidgetPropertySchema() {}
}

