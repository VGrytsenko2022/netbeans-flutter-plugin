package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complementary local/whole projections cover every RangeSlider constructor field. */
public final class RangeSliderTestValues {
    private RangeSliderTestValues() {
    }

    public static PropertyName p(String value) { return new PropertyName(value); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.EnumValue infinity(boolean negative) { return SliderTestValues.infinity(negative); }
    public static PropertyValue.ColorValue color() { return CheckboxTestValues.color(); }
    public static PropertyValue.ThemeTokenValue theme() { return CheckboxTestValues.theme(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FilledButtonTestValues.reference(name); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE).orElseThrow();
    }

    public static PropertyValue value(String name) {
        if (RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return s("click");
        return switch (name) {
            case "valuesStart" -> d("2");
            case "valuesEnd" -> d("3");
            case "min" -> d("-2");
            case "max" -> d("5");
            case "divisions" -> i(7);
            case "enabled", "year2023" -> b(true);
            case "labelsStart" -> s("Start\nwith 'quotes' $symbols");
            case "labelsEnd" -> s("End\r\nwith\ttab");
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
            case "onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "labels", "overlayColor", "mouseCursor" -> reference(name);
            default -> color();
        };
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : RangeSliderWidgetPropertySchema.definitions().keySet()) {
            if (!Set.of("labels", "overlayColor", "mouseCursor").contains(name)) result.put(p(name), value(name));
        }
        return result;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        result.put(p("valuesStart"), i(0));
        result.put(p("valuesEnd"), i(1));
        result.put(p("enabled"), b(true));
        result.putAll(properties);
        return new WidgetNode(StableId.random(), RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE, result, Map.of());
    }

    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
