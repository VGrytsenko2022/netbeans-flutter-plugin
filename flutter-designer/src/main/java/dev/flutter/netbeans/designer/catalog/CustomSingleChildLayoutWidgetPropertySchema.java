package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 CustomSingleChildLayout constructor. */
public final class CustomSingleChildLayoutWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.CustomSingleChildLayout");
    public static final String DELEGATE_CLASS = "_FlutterDesignerSingleChildLayoutDelegate";
    public static final PropertyValue.DartObjectReferenceValue INITIAL_DELEGATE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), DELEGATE_CLASS, Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
    public static final String DESCRIPTION = "Uses a required, non-null SingleChildLayoutDelegate to size the parent, constrain and position optional Child. "
            + "Accepts a typed reference, getter or zero-argument factory. First insertion adds an editable starter class outside managed Dart regions. "
            + "The starter prefers 128 by 96 logical pixels, constrains Child to that size and centers it. Parent constraints still apply. "
            + "Source owns getSize, getConstraintsForChild, getPositionForChild, shouldRelayout and relayout Listenable. "
            + "Parent size cannot depend on Child size. Canvas never executes project delegates: it uses the same bounded 128 by 96 preview "
            + "even after source edits. This is a layout preview, not an evaluation of the custom delegate. Key uses shared identity.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(new Field("delegate", "Delegate", DESCRIPTION));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("delegate"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("SingleChildLayoutDelegate")),
                Optional.of(INITIAL_DELEGATE)));
    }
    /** Only the reviewed starter reference requests source scaffolding, never arbitrary project references. */
    public static boolean usesInitialDelegate(WidgetNode node) {
        if (node.type().equals(TYPE) && INITIAL_DELEGATE.equals(node.properties().get(new PropertyName("delegate")))) return true;
        for (var slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot single && single.child().isPresent()
                    && usesInitialDelegate(single.child().orElseThrow())) return true;
            if (slot instanceof WidgetSlot.ListSlot list && list.children().stream().anyMatch(CustomSingleChildLayoutWidgetPropertySchema::usesInitialDelegate)) return true;
        }
        return false;
    }
    private CustomSingleChildLayoutWidgetPropertySchema() {}
}
