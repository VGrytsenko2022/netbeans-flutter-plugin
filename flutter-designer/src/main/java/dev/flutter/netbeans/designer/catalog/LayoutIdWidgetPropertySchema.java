package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Parent-data identity for a direct CustomMultiChildLayout child. */
public final class LayoutIdWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.LayoutId");
    public static final String TRAIT = "flutter.widgets.LayoutId";
    public static final String DESCRIPTION = "Required non-null Object ID and Child, directly inside CustomMultiChildLayout.children. "
            + "Accepts String, finite integer/number, Boolean or a strict Object source (including enum values and custom objects). "
            + "IDs must be unique under Dart equality. Known literal duplicates are rejected; source equality/identity is owned by the project. "
            + "Factories are evaluated once in generated Dart. Canvas uses private per-node IDs and never runs source code. "
            + "Key is optional; without it Flutter derives ValueKey<Object>(id).";
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("id"), DartParameter.named(0,true), List.of(
                new PropertyValueConstraint.StringLength(0,4096),
                new PropertyValueConstraint.IntegerRange(DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(), DartNumericLiterals.MAX_PORTABLE_INTEGER),
                new PropertyValueConstraint.DoubleRange(null,true,null,true),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                new PropertyValueConstraint.DartObjectReferenceValues("Object")), Optional.of(new PropertyValue.StringValue("layoutChild"))));
    }
    public static WidgetNode starterChild(StableId owner) {
        var id = new StableId(UUID.nameUUIDFromBytes(("flutter-designer:LayoutId:child:" + owner).getBytes(StandardCharsets.UTF_8)));
        var extent = new PropertyValue.IntegerValue(BigInteger.valueOf(48));
        return new WidgetNode(id,new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"),extent,new PropertyName("height"),extent),Map.of());
    }
    public static Optional<String> duplicateIdError(WidgetNode parent) {
        if (!parent.type().equals(CustomMultiChildLayoutWidgetPropertySchema.TYPE)) return Optional.empty();
        var seen = new HashSet<Object>();
        if (parent.slots().get(new SlotName("children")) instanceof WidgetSlot.ListSlot list) {
            for (var child : list.children()) {
                var value = child.properties().get(new PropertyName("id"));
                Object key = value instanceof PropertyValue.IntegerValue integer ? new BigDecimal(integer.value()).stripTrailingZeros()
                        : value instanceof PropertyValue.DoubleValue number ? finiteNumberKey(number.value())
                        : value instanceof PropertyValue.StringValue || value instanceof PropertyValue.BooleanValue ? value : null;
                if (key != null && !seen.add(key)) return Optional.of("CustomMultiChildLayout has duplicate LayoutId values: "
                        + value + ". Each direct child ID must be unique; change ID on widget " + child.id() + ".");
            }
        }
        return Optional.empty();
    }
    private static Object finiteNumberKey(BigDecimal value) {
        double number = value.doubleValue();
        return Double.isFinite(number) ? BigDecimal.valueOf(number).stripTrailingZeros() : null;
    }
    private LayoutIdWidgetPropertySchema() {}
}
