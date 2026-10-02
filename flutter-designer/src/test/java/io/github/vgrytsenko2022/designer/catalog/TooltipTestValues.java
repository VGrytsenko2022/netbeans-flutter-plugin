package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complete complementary Tooltip branches; project references are never fabricated as executed values. */
public final class TooltipTestValues {
    private TooltipTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return RadioTestValues.s(value); }
    public static PropertyValue.BooleanValue b(boolean value) { return RadioTestValues.b(value); }
    public static PropertyValue.IntegerValue i(long value) { return RadioTestValues.i(value); }
    public static PropertyValue.NullValue nil() { return RadioTestValues.nil(); }
    public static PropertyValue.DoubleValue d(String value) { return RadioTestValues.d(value); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TooltipWidgetPropertySchema.TOOLTIP_TYPE).orElseThrow(); }
    public static PropertyValue value(String name) {
        if (TooltipWidgetPropertySchema.isTextStyleProperty(p(name))) return BadgeTestValues.value(name);
        if (TooltipWidgetPropertySchema.durationProperties().contains(name)) return i(123456);
        if (name.equals("message")) return s("Tooltip message");
        if (name.equals("onTriggered")) return s("noop");
        if (name.equals("mouseCursor")) return s("clickable");
        if (List.of("padding", "margin").contains(name)) return BadgeTestValues.value("padding");
        if (List.of("height", "verticalOffset").contains(name)) return d("24.5");
        if (name.equals("constraints")) return new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.of(new BigDecimal("240")), BigDecimal.ZERO, Optional.of(new BigDecimal("180")));
        if (name.equals("decoration")) return new PropertyValue.BoxDecorationValue(Optional.of(new ColorSource.Theme(new ThemeToken("material.colorScheme.surface"))), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        var property = definition().property(p(name)).orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        for (var constraint : property.constraints()) if (constraint instanceof PropertyValueConstraint.EnumValues values) return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
        throw new IllegalArgumentException(name);
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean rich, boolean paints) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : TooltipWidgetPropertySchema.definitions().keySet()) {
            if (List.of("height", "textStyle").contains(name) || name.equals(rich ? "message" : "richMessage")) continue;
            if (paints && List.of("textStyleColor", "textStyleBackgroundColor").contains(name)) continue;
            if (!paints && List.of("textStyleForeground", "textStyleBackground").contains(name)) continue;
            values.put(p(name), value(name));
        }
        return values;
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        return new WidgetNode(StableId.random(), TooltipWidgetPropertySchema.TOOLTIP_TYPE, values, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
    }
    public static WidgetNode with(Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>(); properties.put(p("message"), s("Tooltip")); properties.putAll(values); return node(properties);
    }
    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
