package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complementary ExpansionTile families with an explicit real Title and no fabricated defaults. */
public final class ExpansionTileTestValues {
    private ExpansionTileTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return RadioTestValues.s(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return RadioTestValues.b(value); }
    public static PropertyValue.IntegerValue i(long value) { return RadioTestValues.i(value); }
    public static PropertyValue.NullValue nil() { return RadioTestValues.nil(); }
    public static PropertyValue.DoubleValue d(String value) { return RadioTestValues.d(value); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioTestValues.reference(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        var shape = ExpansionTileWidgetPropertySchema.shapeSourceName(name);
        if (shape.isPresent()) return ListTileTestValues.value(shape.orElseThrow());
        if (ExpansionTileWidgetPropertySchema.animationDurationProperties().contains(name)) return i(234567);
        if (ExpansionTileWidgetPropertySchema.animationCurveProperties().contains(name)) return s(name.contains("Reverse") ? "easeOut" : "easeIn");
        if (name.equals("onExpansionChanged")) return s("noop");
        if (List.of("tilePadding", "childrenPadding").contains(name)) return ListTileTestValues.value("contentPadding");
        if (name.equals("expandedAlignment")) return new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL, new BigDecimal("-0.5"), new BigDecimal("0.25"));
        if (List.of("visualDensityHorizontal", "visualDensityVertical").contains(name)) return d("-1.5");
        if (name.equals("minTileHeight")) return d("48");
        var property = definition().property(p(name)).orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.COLOR)) return CheckboxTestValues.theme();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        for (var constraint : property.constraints()) if (constraint instanceof PropertyValueConstraint.EnumValues values) return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
        throw new IllegalArgumentException(name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String shape, String collapsedShape) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : ExpansionTileWidgetPropertySchema.definitions().keySet()) {
            if (List.of("shape", "collapsedShape", "visualDensity", "expansionAnimationStyle").contains(name)) continue;
            String kind = name.startsWith("collapsedShape") ? collapsedShape : shape;
            if (ExpansionTileWidgetPropertySchema.isShapeDetailProperty(name) && !ExpansionTileWidgetPropertySchema.shapePropertyAppliesToKind(name, kind)) continue;
            values.put(p(name), value(name));
        }
        values.put(p("shapeKind"), s(shape)); values.put(p("collapsedShapeKind"), s(collapsedShape));
        return values;
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var slots = new LinkedHashMap<>(prototype.slots());
        slots.put(new SlotName("title"), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text("Title"))));
        return new WidgetNode(prototype.id(), prototype.type(), values, slots);
    }
    public static WidgetNode fullNode(String shape, String collapsedShape) {
        var node = node(full(shape, collapsedShape)); var slots = new LinkedHashMap<>(node.slots());
        for (String name : List.of("leading", "subtitle", "trailing")) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
        slots.put(new SlotName("children"), new WidgetSlot.ListSlot(List.of(ListTileTestValues.text("First"), ListTileTestValues.text("Second"))));
        return new WidgetNode(node.id(), node.type(), node.properties(), slots);
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
