package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complementary complete RadioListTile projections preserve original prefixed paths. */
public final class RadioListTileTestValues {
    private RadioListTileTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return RadioTestValues.s(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return RadioTestValues.b(value); }
    public static PropertyValue.IntegerValue i(long value) { return RadioTestValues.i(value); }
    public static PropertyValue.NullValue nil() { return RadioTestValues.nil(); }
    public static PropertyValue.DoubleValue d(String value) { return RadioTestValues.d(value); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return RadioTestValues.reference(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        if (name.startsWith("mouseCursor")) return s("click");
        if (name.equals("radioScaleFactor")) return d("1.25");
        if (name.equals("controlAffinity")) return new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
        String source = RadioListTileWidgetPropertySchema.radioSourceName(name);
        if (RadioWidgetPropertySchema.find(source).isPresent()) return RadioTestValues.value(source);
        return ListTileTestValues.value(name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shape) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : RadioListTileWidgetPropertySchema.definitions().keySet()) {
            if (List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius", "radioSide", "visualDensity", "shape", "mouseCursor").contains(name)) continue;
            if (RadioListTileWidgetPropertySchema.isShapeDetailProperty(name) && !RadioListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, shape)) continue;
            values.put(p(name), value(name));
        }
        values.put(p("variant"), s(variant)); values.put(p("shapeKind"), s(shape));
        return values;
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) { return full(variant, "roundedRectangle"); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) { return node(properties, Map.of()); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties, Map<SlotName, WidgetSlot> slots) {
        var values = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(definition(), StableId.random()).properties());
        values.putAll(properties);
        return new WidgetNode(StableId.random(), RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE, values, slots);
    }
    public static WidgetNode without(WidgetNode node, String... names) {
        var values = new LinkedHashMap<>(node.properties()); for (String name : names) values.remove(p(name));
        return new WidgetNode(node.id(), node.type(), values, node.slots(), node.extensions(), node.stateBinding(), node.propertyBindings());
    }
    public static WidgetNode fullNode(String variant, String shape) {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "subtitle", "secondary")) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
        return node(full(variant, shape), slots);
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
