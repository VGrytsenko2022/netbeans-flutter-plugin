package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complementary local/whole Radio fixtures cover every closed constructor field. */
public final class RadioTestValues {
    private RadioTestValues() {}
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.ColorValue color() { return CheckboxTestValues.color(); }
    public static PropertyValue.ThemeTokenValue theme() { return CheckboxTestValues.theme(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FilledButtonTestValues.reference(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(RadioWidgetPropertySchema.RADIO_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        if (List.of("value", "groupValue").contains(name)) return s("option");
        if (name.equals("valueType")) return s("String");
        if (name.equals("variant")) return s("standard");
        if (RadioWidgetPropertySchema.innerRadiusStateProperties().contains(name)) return d("4.5");
        if (RadioWidgetPropertySchema.sideLocalProperties().contains(name)) return CheckboxTestValues.value(name);
        var property = definition().property(p(name)).orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        if (property.acceptedKinds().contains(PropertyValueKind.COLOR)) return color();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (name.equals("splashRadius")) return new PropertyValue.EnumValue("double", "negativeInfinity");
        if (name.startsWith("visualDensity")) return d("-0.25");
        for (var constraint : property.constraints()) if (constraint instanceof PropertyValueConstraint.EnumValues values) {
            return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
        }
        throw new IllegalArgumentException("No Radio sample for " + name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : RadioWidgetPropertySchema.definitions().keySet()) {
            if (List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity").contains(name)) continue;
            if (name.equals("useCupertinoCheckmarkStyle") && !variant.equals("adaptive")) continue;
            result.put(p(name), value(name));
        }
        result.put(p("variant"), s(variant));
        return result;
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var result = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(definition(), StableId.random()).properties());
        result.putAll(properties);
        return new WidgetNode(StableId.random(), RadioWidgetPropertySchema.RADIO_TYPE, result, Map.of());
    }
    public static WidgetNode without(WidgetNode node, String... names) {
        var result = new LinkedHashMap<>(node.properties());
        for (String name : names) result.remove(p(name));
        return new WidgetNode(node.id(), node.type(), result, node.slots());
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
