package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complementary complete ListTile families; no component defaults or children are invented. */
public final class ListTileTestValues {
    private ListTileTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FilledButtonTestValues.reference(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(ListTileWidgetPropertySchema.LIST_TILE_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        var family = ListTileWidgetPropertySchema.textStyleFamily(p(name));
        if (family.isPresent()) return BadgeTestValues.value("textStyle" + name.substring(family.orElseThrow().length()));
        if (ListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) return CheckboxTestValues.value(name);
        if (name.equals("contentPadding")) return new PropertyValue.EdgeInsetsDirectionalValue(new BigDecimal("-2"), BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE);
        if (ListTileWidgetPropertySchema.geometryProperties().contains(name)) return d("-2.5");
        if (name.startsWith("visualDensity") && !name.equals("visualDensity")) return d("-1.5");
        if (name.startsWith("mouseCursor")) return s("click");
        if (List.of("onTap", "onLongPress", "onFocusChange").contains(name)) return reference(name);
        var property = definition().property(p(name)).orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.COLOR)) return CheckboxTestValues.theme();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        for (var constraint : property.constraints()) if (constraint instanceof PropertyValueConstraint.EnumValues values) {
            return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
        }
        throw new IllegalArgumentException(name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String shape, boolean paints) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : ListTileWidgetPropertySchema.definitions().keySet()) {
            if (List.of("shape", "visualDensity", "iconColor", "textColor", "mouseCursor").contains(name)
                    || ListTileWidgetPropertySchema.styleFamilies().contains(name)) continue;
            if (ListTileWidgetPropertySchema.isShapeDetailProperty(name) && !ListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, shape)) continue;
            var family = ListTileWidgetPropertySchema.textStyleFamily(p(name));
            if (family.isPresent()) {
                String suffix = name.substring(family.orElseThrow().length());
                if (paints && List.of("Color", "BackgroundColor").contains(suffix)) continue;
                if (!paints && List.of("Foreground", "Background").contains(suffix)) continue;
            }
            values.put(p(name), value(name));
        }
        values.put(p("shapeKind"), s(shape));
        return values;
    }
    public static WidgetNode text(String value) { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s(value)), Map.of()); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) { return new WidgetNode(StableId.random(), ListTileWidgetPropertySchema.LIST_TILE_TYPE, properties, Map.of()); }
    public static WidgetNode fullNode(String shape, boolean paints) {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("leading", "title", "subtitle", "trailing")) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(text(name))));
        return new WidgetNode(StableId.random(), ListTileWidgetPropertySchema.LIST_TILE_TYPE, full(shape, paints), slots);
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
