package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 Flow constructor. */
public final class FlowWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.Flow");
    public static final WidgetTypeId UNWRAPPED_TYPE = new WidgetTypeId("flutter.widgets.Flow.unwrapped");
    public static boolean isFlow(WidgetTypeId type) { return TYPE.equals(type) || UNWRAPPED_TYPE.equals(type); }
    public static final String DELEGATE_CLASS = "_FlutterDesignerFlowDelegate";
    public static final PropertyValue.DartObjectReferenceValue INITIAL_DELEGATE =
            new PropertyValue.DartObjectReferenceValue(Optional.empty(), DELEGATE_CLASS, Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
    public static final String DESCRIPTION = "Positions ordinary children with paint-time matrices from a required FlowDelegate. "
            + "Source owns getSize, getConstraintsForChild, paintChildren, shouldRelayout, shouldRepaint and repaint Listenable. "
            + "First insertion creates an editable delegate outside managed regions: constrained 256 by 192 size, "
            + "children at most 48 by 48, left-to-right rows with an 8 pixel gap. Parent size is independent of children. "
            + "Flow wraps children in RepaintBoundary; Flow.unwrapped does not. "
            + "Canvas never executes project delegates: it previews this same starter, not custom transforms or animation. "
            + "Paint each child at most once; hit testing follows painted transforms. When hiding Flow with zero opacity, "
            + "also disable hit testing using IgnorePointer to avoid stale paint geometry. Key uses shared identity.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(new Field("delegate", "Delegate", DESCRIPTION),
            new Field("clipBehavior", "Clip behavior", "All four Clip values; omission uses Clip.hardEdge. No null. Clipping affects painting, not child layout."));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("delegate"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("FlowDelegate")),
                Optional.of(INITIAL_DELEGATE)),
                new PropertyDefinition(new PropertyName("clipBehavior"), DartParameter.named(2, false),
                        List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "Clip"),
                                List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"))), Optional.empty()));
    }
    /** Only the reviewed starter reference requests source scaffolding, never arbitrary project references. */
    public static boolean usesInitialDelegate(WidgetNode node) {
        if (isFlow(node.type()) && INITIAL_DELEGATE.equals(node.properties().get(new PropertyName("delegate")))) return true;
        for (var slot : node.slots().values()) {
            if (slot instanceof WidgetSlot.SingleSlot single && single.child().isPresent()
                    && usesInitialDelegate(single.child().orElseThrow())) return true;
            if (slot instanceof WidgetSlot.ListSlot list && list.children().stream().anyMatch(FlowWidgetPropertySchema::usesInitialDelegate)) return true;
        }
        return false;
    }
    private FlowWidgetPropertySchema() {}
}
