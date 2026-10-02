package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complementary legal FAB families cover every constructor, shape and TextStyle leaf. */
public final class FloatingActionButtonTestValues {
    private FloatingActionButtonTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    public static PropertyValue.EnumValue infinity() { return new PropertyValue.EnumValue("double", "infinity"); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE).orElseThrow();
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/fab_values.dart"),
                "fabValues", Optional.of(name), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    public static PropertyValue value(String name) {
        if (name.startsWith("extendedTextStyle")) return BadgeTestValues.value("textStyle" + name.substring(17));
        if (name.equals("shapeRadius")) {
            var a = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.TWO);
            var b = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("3"), new BigDecimal("4"));
            return new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a, b, b, a));
        }
        if (name.equals("extendedPadding")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
        if (name.equals("shapeKind")) return s("roundedRectangle");
        if (name.equals("heroTag")) return s("hero-'-$-\\-\n-tag");
        if (name.equals("mouseCursor")) return s("adaptiveClickable");
        if (name.equals("tooltip")) return s("Action\nsecond line");
        if (name.endsWith("Color")) return BadgeTestValues.theme();
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        if (property.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN))) return new PropertyValue.BooleanValue(false);
        for (var constraint : property.constraints()) if (constraint instanceof PropertyValueConstraint.EnumValues e && !e.dartType().name().equals("double")) {
            return new PropertyValue.EnumValue(e.dartType().name(), e.values().getLast());
        }
        return d(name.equals("shapePoints") ? "5.5" : name.startsWith("shape") ? "0.25" : "2.5");
    }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shape, boolean paints) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : FloatingActionButtonWidgetPropertySchema.definitions().keySet()) {
            if (!FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(name, variant)) continue;
            if (name.equals("shape")) continue;
            if (FloatingActionButtonWidgetPropertySchema.isShapeDetailProperty(name)
                    && !FloatingActionButtonWidgetPropertySchema.shapePropertyAppliesToKind(name, shape)) continue;
            if (paints ? Set.of("extendedTextStyleColor", "extendedTextStyleBackgroundColor").contains(name)
                    : Set.of("extendedTextStyleForeground", "extendedTextStyleBackground").contains(name)) continue;
            values.put(p(name), name.equals("variant") ? s(variant) : name.equals("shapeKind") ? s(shape) : value(name));
        }
        return values;
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("variant"), s("standard"));
        values.put(p("enabled"), new PropertyValue.BooleanValue(true));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), definition().typeId(), values,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Label")),
                        new SlotName("icon"), WidgetSlot.SingleSlot.empty()));
    }
    public static WidgetNode text(String value) { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s(value)), Map.of()); }
    public static DesignerDocument document(WidgetNode node) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("f0000000-0000-4000-8000-000000000001"),
                new DartSourceDescriptor("fab.dart", "FabScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), node);
    }
}
