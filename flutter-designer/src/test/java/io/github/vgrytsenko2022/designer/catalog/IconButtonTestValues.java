package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complementary dense families covering every reviewed IconButton field. */
public final class IconButtonTestValues {
    private IconButtonTestValues() {
    }

    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.EnumValue infinity() { return new PropertyValue.EnumValue("double", "infinity"); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE).orElseThrow();
    }

    public static PropertyValue.DartObjectReferenceValue reference(String member) {
        return FilledButtonTestValues.reference(member);
    }

    public static PropertyValue value(String name) {
        if (IconButtonWidgetPropertySchema.localStyleProperties().contains(name)) {
            return FilledButtonTestValues.value(name);
        }
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        return switch (name) {
            case "iconSize" -> d("24");
            case "splashRadius" -> d("24");
            case "visualDensityHorizontal", "visualDensityVertical" -> d("1");
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
            case "alignment" -> new PropertyValue.AlignmentGeometryValue(
                    PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL, BigDecimal.ONE, BigDecimal.ZERO);
            case "constraints" -> new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.of(new BigDecimal("400")),
                    BigDecimal.ZERO, Optional.of(new BigDecimal("400")));
            case "tooltip" -> s("Icon action");
            case "color", "focusColor", "hoverColor", "highlightColor", "splashColor", "disabledColor" ->
                    new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            default -> new PropertyValue.BooleanValue(true);
        };
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, boolean paint, boolean circle) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : IconButtonWidgetPropertySchema.definitions().keySet()) {
            if (name.equals("style")) continue;
            if (paint ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (circle ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            PropertyValue value = value(name);
            if (name.equals("variant")) value = s(variant);
            if (circle && name.endsWith("ShapeKind")) value = s("circle");
            values.put(p(name), value);
        }
        return values;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("enabled"), new PropertyValue.BooleanValue(true));
        values.put(p("variant"), s("standard"));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE, values,
                Map.of(new SlotName("icon"), new WidgetSlot.SingleSlot(Optional.of(text("Icon"))),
                        new SlotName("selectedIcon"), WidgetSlot.SingleSlot.empty()));
    }

    public static WidgetNode text(String value) {
        return FilledButtonTestValues.text(value);
    }

    public static DesignerDocument document(WidgetNode root) {
        return FilledButtonTestValues.document(root);
    }
}
