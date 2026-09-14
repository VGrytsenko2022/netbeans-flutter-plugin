package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complementary legal projections cover every SwitchListTile constructor/compound field. */
public final class SwitchListTileTestValues {
    private SwitchListTileTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return SwitchTestValues.s(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return SwitchTestValues.b(value); }
    public static PropertyValue.NullValue nil() { return SwitchTestValues.nil(); }
    public static PropertyValue.DoubleValue d(String value) { return SwitchTestValues.d(value); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return SwitchTestValues.reference(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (name.startsWith("mouseCursor")) return s("click");
        if (List.of("onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError").contains(name)) return reference(name);
        if (SwitchWidgetPropertySchema.find(name).isPresent()) return SwitchTestValues.value(name);
        if (name.equals("controlAffinity")) return new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
        return ListTileTestValues.value(name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shape) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : SwitchListTileWidgetPropertySchema.definitions().keySet()) {
            if (SwitchListTileWidgetPropertySchema.colorFamilies().contains(name)
                    || List.of("thumbIcon", "shape", "visualDensity", "mouseCursor").contains(name)) continue;
            if (SwitchListTileWidgetPropertySchema.isShapeDetailProperty(name)
                    && !SwitchListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, shape)) continue;
            values.put(p(name), value(name));
        }
        values.put(p("variant"), s(variant));
        values.put(p("shapeKind"), s(shape));
        return values;
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) { return full(variant, "roundedRectangle"); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) { return node(properties, Map.of()); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties, Map<SlotName, WidgetSlot> slots) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("value"), b(false)); values.put(p("onChanged"), s("noop")); values.put(p("variant"), s("standard"));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE, values, slots);
    }
    public static WidgetNode fullNode(String variant, String shape) {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "subtitle", "secondary")) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
        return node(full(variant, shape), slots);
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
