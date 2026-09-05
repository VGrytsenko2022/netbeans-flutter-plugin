package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Shared complete typed fixture; never copies framework defaults into omission tests. */
public final class PhysicalShapeTestSupport {
    private PhysicalShapeTestSupport() { }

    public static PropertyName name(String value) { return new PropertyName(value); }

    public static PropertyValue.BorderRadiusValue radius(boolean directional) {
        var a = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("1.5"), new BigDecimal("2.5"));
        var b = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("3.5"), new BigDecimal("4.5"));
        var c = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("5.5"), new BigDecimal("6.5"));
        var d = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("7.5"), new BigDecimal("8.5"));
        return new PropertyValue.BorderRadiusValue(directional
                ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a, b, c, d)
                : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a, b, c, d));
    }

    public static Map<PropertyName, PropertyValue> fullProperties(PropertyValue.ShapeBorderClipperValue.Shape shape, String clip, boolean themed) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(name("clipper"), clipper(shape, false));
        values.put(name("clipBehavior"), new PropertyValue.EnumValue("Clip", clip));
        values.put(name("elevation"), new PropertyValue.DoubleValue(new BigDecimal("12.5")));
        values.put(name("color"), themed
                ? new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))
                : new PropertyValue.ColorValue(0xFF102030L));
        values.put(name("shadowColor"), themed
                ? new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.shadow"))
                : new PropertyValue.ColorValue(0x80204060L));
        return values;
    }

    public static PropertyValue.ShapeBorderClipperValue clipper(
            PropertyValue.ShapeBorderClipperValue.Shape shape, boolean directional) {
        return new PropertyValue.ShapeBorderClipperValue(shape, radius(directional).geometry(),
                directional && shape.supportsRadius()
                        ? Optional.of(PropertyValue.ShapeBorderClipperValue.TextDirection.RTL) : Optional.empty());
    }

    public static Map<PropertyName, PropertyValue> defaults() {
        return Map.of(name("color"), new PropertyValue.ColorValue(0xFF2196F3L),
                name("clipper"), PropertyValue.ShapeBorderClipperValue.defaultValue());
    }

    public static WidgetNode physicalShape(Map<PropertyName, PropertyValue> values, boolean child) {
        return new WidgetNode(StableId.random(), PhysicalShapeWidgetPropertySchema.PHYSICAL_SHAPE_TYPE,
                values, Map.of(new SlotName("child"), child
                        ? WidgetSlot.SingleSlot.of(text()) : WidgetSlot.SingleSlot.empty()));
    }

    public static WidgetNode text() {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(name("data"), new PropertyValue.StringValue("Preserved child")), Map.of());
    }

    public static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor(
                "sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(),
                new ManagedRegions(region, region)), root);
    }
}
