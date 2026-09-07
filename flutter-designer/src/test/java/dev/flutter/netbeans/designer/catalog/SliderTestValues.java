package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complementary local/whole overlay projections cover all 33 Slider fields. */
public final class SliderTestValues {
    private SliderTestValues() {
    }

    public static PropertyName p(String value) { return new PropertyName(value); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.EnumValue infinity(boolean negative) { return new PropertyValue.EnumValue("double", negative ? "negativeInfinity" : "infinity"); }
    public static PropertyValue.ColorValue color() { return CheckboxTestValues.color(); }
    public static PropertyValue.ThemeTokenValue theme() { return CheckboxTestValues.theme(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FilledButtonTestValues.reference(name); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(SliderWidgetPropertySchema.SLIDER_TYPE).orElseThrow();
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "value" -> d("2");
            case "min" -> d("-2");
            case "max" -> d("5");
            case "secondaryTrackValue" -> d("3");
            case "divisions" -> i(7);
            case "variant" -> s("standard");
            case "enabled", "autofocus", "year2023" -> b(true);
            case "label" -> s("Value\nlabel\r\nwith 'quotes' and $symbols");
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
            case "allowedInteraction" -> new PropertyValue.EnumValue("SliderInteraction", "slideThumb");
            case "showValueIndicator" -> new PropertyValue.EnumValue("ShowValueIndicator", "alwaysVisible");
            case "onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "focusNode", "overlayColor", "mouseCursor" -> reference(name);
            default -> color();
        };
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : SliderWidgetPropertySchema.definitions().keySet()) {
            if (name.equals("overlayColor") || name.equals("padding") && variant.equals("adaptive")) continue;
            result.put(p(name), value(name));
        }
        result.put(p("variant"), s(variant));
        return result;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        result.put(p("value"), i(0));
        result.put(p("variant"), s("standard"));
        result.put(p("enabled"), b(true));
        result.putAll(properties);
        return new WidgetNode(StableId.random(), SliderWidgetPropertySchema.SLIDER_TYPE, result, Map.of());
    }

    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
