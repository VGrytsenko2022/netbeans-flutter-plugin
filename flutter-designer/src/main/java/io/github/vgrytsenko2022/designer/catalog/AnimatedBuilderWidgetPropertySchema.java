package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Flutter 3.44.8 AnimatedBuilder with fixed box/sliver placement projections. */
public final class AnimatedBuilderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedBuilder");
    public static final WidgetTypeId SLIVER_TYPE = new WidgetTypeId("flutter.widgets.AnimatedBuilder.sliver");
    public static final String CALLBACK_TYPE = "TransitionBuilder";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || SLIVER_TYPE.equals(type); }
    public static String description(WidgetTypeId type) {
        return "Rebuilds when Animation notifies. Animation accepts No notifications or a typed project Listenable object/getter/factory; it is not restricted to Animation<double>. "
                + "Builder accepts Child or a typed TransitionBuilder: Widget Function(BuildContext, Widget?). "
                + (SLIVER_TYPE.equals(type)
                    ? "Sliver placement: Child and callback result must be slivers; absent Child returns SliverToBoxAdapter(). "
                    : "Box placement: Child and callback result must be boxes; absent Child returns SizedBox.shrink(). ")
                + "Both projections generate the unnamed AnimatedBuilder constructor. Child is optional and passed unchanged to Builder. "
                + "The source owner, not this widget, disposes the Listenable. Flutter subscribes, replaces subscriptions and unsubscribes on removal. "
                + "Canvas never evaluates project objects/getters/factories or callbacks; custom behavior is explicitly unavailable. "
                + "The source owner supplies and disposes any AnimationController and its ticker; no controller or onEnd callback is invented. "
                + "Key uses shared identity; Builder is not a native Event.";
    }
    public static List<PropertyDefinition> properties() {
        return List.of(property("animation", 0, "none", "no notifications", "Listenable"),
                property("builder", 1, "child", "return Child or an empty result", CALLBACK_TYPE));
    }
    private static PropertyDefinition property(String name, int order, String preset, String label, String type) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, true),
                List.of(new PropertyValueConstraint.StringPattern(preset, label),
                        new PropertyValueConstraint.DartObjectReferenceValues(type)),
                Optional.of(new PropertyValue.StringValue(preset)));
    }
    private AnimatedBuilderWidgetPropertySchema() {}
}


