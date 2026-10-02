package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 CustomMultiChildLayout constructor. */
public final class CustomMultiChildLayoutWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.CustomMultiChildLayout");
    public static final String DELEGATE_CLASS = "_FlutterDesignerMultiChildLayoutDelegate";
    public static final PropertyValue.DartObjectReferenceValue INITIAL_DELEGATE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), DELEGATE_CLASS, Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
    public static final String DESCRIPTION = "Lays out ordered LayoutId children using a required MultiChildLayoutDelegate. "
            + "Source owns getSize, performLayout, shouldRelayout and relayout notifications; every child must be laid out exactly once. "
            + "The editable starter divides a constrained 256 by 192 area into vertical cells. Designer initializes its ids from the actual children "
            + "once per build, without reevaluating ID factories. Retain its ids field when editing the starter. "
            + "Custom source delegates own their ID contract. Canvas never executes project code: it shows the starter layout with private preview IDs.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(new Field("delegate", "Delegate", DESCRIPTION));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("delegate"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("MultiChildLayoutDelegate")),
                Optional.of(INITIAL_DELEGATE)));
    }
    /** Only the reviewed starter reference requests source scaffolding, never arbitrary project references. */
    public static boolean usesInitialDelegate(WidgetNode node) {
        if (node.type().equals(TYPE) && INITIAL_DELEGATE.equals(node.properties().get(new PropertyName("delegate")))) return true;
        for (var slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot single && single.child().isPresent()
                    && usesInitialDelegate(single.child().orElseThrow())) return true;
            if (slot instanceof WidgetSlot.ListSlot list && list.children().stream().anyMatch(CustomMultiChildLayoutWidgetPropertySchema::usesInitialDelegate)) return true;
        }
        return false;
    }
    private CustomMultiChildLayoutWidgetPropertySchema() {}
}
