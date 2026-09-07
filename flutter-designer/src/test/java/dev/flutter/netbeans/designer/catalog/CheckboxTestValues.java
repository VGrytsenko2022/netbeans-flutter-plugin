package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complementary legal Checkbox families cover every public metadata field. */
public final class CheckboxTestValues {
    private CheckboxTestValues() {
    }

    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.ColorValue color() { return new PropertyValue.ColorValue(0xFF123456L); }
    public static PropertyValue.ThemeTokenValue theme() { return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FilledButtonTestValues.reference(name); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(CheckboxWidgetPropertySchema.CHECKBOX_TYPE).orElseThrow();
    }

    public static PropertyValue value(String name) {
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        if (property.acceptedKinds().contains(PropertyValueKind.COLOR)) return color();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (name.endsWith("Mode")) return s("border");
        if (name.equals("semanticLabel")) return s("Mixed choice\nAccessible label");
        if (name.equals("shapeKind")) return s("roundedRectangle");
        if (name.equals("shapeRadius")) {
            var a = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.TWO);
            return new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a, a, a, a));
        }
        if (name.equals("splashRadius")) return new PropertyValue.EnumValue("double", "infinity");
        for (var constraint : property.constraints()) {
            if (constraint instanceof PropertyValueConstraint.EnumValues values) {
                return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
            }
        }
        if (name.equals("shapePoints")) return d("5.5");
        if (name.equals("shapeRotation") || name.endsWith("StrokeAlign")) return d("-2.5");
        return d("0.25");
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shape) {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : CheckboxWidgetPropertySchema.definitions().keySet()) {
            if (List.of("shape", "side", "fillColor", "overlayColor").contains(name)) continue;
            if (CheckboxWidgetPropertySchema.isShapeDetailProperty(name)
                    && !CheckboxWidgetPropertySchema.shapePropertyAppliesToKind(name, shape)) continue;
            properties.put(p(name), value(name));
        }
        properties.put(p("variant"), s(variant));
        properties.put(p("shapeKind"), s(shape));
        return properties;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("value"), b(false));
        values.put(p("variant"), s("standard"));
        values.put(p("enabled"), b(true));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), CheckboxWidgetPropertySchema.CHECKBOX_TYPE, values, Map.of());
    }

    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
