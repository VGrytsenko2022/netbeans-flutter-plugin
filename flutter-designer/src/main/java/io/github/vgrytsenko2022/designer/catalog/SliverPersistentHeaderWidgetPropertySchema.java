package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 SliverPersistentHeader constructor. */
public final class SliverPersistentHeaderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverPersistentHeader");
    public static final String DELEGATE_CLASS = "_FlutterDesignerPersistentHeaderDelegate";
    public static final PropertyValue.DartObjectReferenceValue INITIAL_DELEGATE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), DELEGATE_CLASS, Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
    public static final String DESCRIPTION = "A sliver header whose size varies between delegate.minExtent and maxExtent. "
            + "Required Delegate accepts a strict SliverPersistentHeaderDelegate reference, getter or zero-argument factory. "
            + "First insertion adds an editable starter delegate outside managed Dart regions, atomically with the widget. "
            + "Edit that class in Source or select your own delegate for build, extents, shouldRebuild, vsync, snap, stretch "
            + "and show-on-screen behavior. The delegate must return a box widget and stable finite extents with min <= max. "
            + "Floating snap/show-on-screen animation requires a suitable vsync. "
            + "Canvas never executes delegate code: it shows a labeled 56–112 logical-pixel preview. "
            + "There is no child/sliver slot or native widget Event; the delegate owns content and behavior. Key uses shared identity.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("delegate", "Delegate", DESCRIPTION),
            new Field("pinned", "Pinned", "Keep the header's minimum extent visible at the start of the viewport. Omission uses false."),
            new Field("floating", "Floating", "Reveal the header immediately when scrolling back. Can be combined with Pinned. Omission uses false."));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("delegate"), DartParameter.named(0, true),
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues("SliverPersistentHeaderDelegate")),
                        Optional.of(INITIAL_DELEGATE)),
                new PropertyDefinition(new PropertyName("pinned"), DartParameter.named(1, false),
                        List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()),
                new PropertyDefinition(new PropertyName("floating"), DartParameter.named(2, false),
                        List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
    }
    /** Only the reviewed starter reference requests source scaffolding, never arbitrary project references. */
    public static boolean usesInitialDelegate(WidgetNode node) {
        if (node.type().equals(TYPE) && INITIAL_DELEGATE.equals(node.properties().get(new PropertyName("delegate")))) return true;
        for (var slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot single && single.child().isPresent()
                    && usesInitialDelegate(single.child().orElseThrow())) return true;
            if (slot instanceof WidgetSlot.ListSlot list && list.children().stream().anyMatch(SliverPersistentHeaderWidgetPropertySchema::usesInitialDelegate)) return true;
        }
        return false;
    }
    private SliverPersistentHeaderWidgetPropertySchema() {}
}

