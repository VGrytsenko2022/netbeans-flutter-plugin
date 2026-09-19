package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Complete, legal sparse-style families used by generation, codec and history tests. */
public final class FilledButtonTestValues {
    private FilledButtonTestValues() { }

    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE).orElseThrow();
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:buttons/styles.dart"),
                "buttonValues", Optional.of(name), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }

    public static PropertyValue value(String name) {
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        if (name.endsWith("TextTheme")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.labelLarge"));
        if (name.endsWith("Color")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.error"));
        if (name.endsWith("Padding")) return new PropertyValue.EdgeInsetsDirectionalValue(
                BigDecimal.ONE, BigDecimal.TWO, new BigDecimal("3"), new BigDecimal("4"));
        if (name.endsWith("TextBackground")) return BadgeTestValues.paint();
        if (name.endsWith("Shadows") || name.endsWith("FontFeatures") || name.endsWith("FontVariations")) {
            return BadgeTestValues.value(name);
        }
        if (name.endsWith("LocaleLanguageCode")) return s("uk");
        if (name.endsWith("LocaleScriptCode")) return s("Cyrl");
        if (name.endsWith("LocaleCountryCode")) return s("UA");
        if (name.endsWith("FontFamilyFallback")) return s("Noto Sans\nRoboto");
        if (name.endsWith("FontFamily")) return s("Noto Sans");
        if (name.endsWith("Package")) return s("button_fonts");
        if (name.endsWith("ShapeKind")) return s("roundedRectangle");
        if (name.endsWith("MouseCursor")) return s("click");
        if (name.equals("styleAlignmentKind")) return s("directional");
        if (name.equals("styleSplashFactory")) return s("inkRipple");
        for (var constraint : property.constraints()) {
            if (constraint instanceof PropertyValueConstraint.EnumValues values) {
                return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
            }
            if (constraint.kind() == PropertyValueKind.BOOLEAN) return new PropertyValue.BooleanValue(true);
        }
        if (name.equals("styleAnimationDurationMs")) return new PropertyValue.IntegerValue(BigInteger.valueOf(180));
        if (name.endsWith("MaximumWidth") || name.endsWith("MaximumHeight")) return d("320");
        if (name.endsWith("MinimumWidth") || name.endsWith("MinimumHeight")) return d("64");
        if (name.endsWith("ShapeCircleEccentricity")) return d("0.5");
        return d("2.5");
    }

    /** Every row participates across these complementary legal families. */
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean icon, boolean paints, boolean circles) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : FilledButtonWidgetPropertySchema.definitions().keySet()) {
            if (name.equals("style") || (icon ? name.equals("isSemanticButton") : name.equals("iconAlignment"))) continue;
            if (paints ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (circles ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            PropertyValue value = value(name);
            if (name.equals("variant")) value = s(icon ? "icon" : "standard");
            if (circles && name.endsWith("ShapeKind")) value = s("circle");
            values.put(p(name), value);
        }
        return values;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("enabled"), new PropertyValue.BooleanValue(true));
        values.put(p("variant"), s("standard"));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE, values,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("Label"))),
                        new SlotName("icon"), WidgetSlot.SingleSlot.empty()));
    }

    public static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), s(value)), Map.of());
    }

    public static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("fd6d41cd-f81b-4c39-bf31-a02bdfb3bf52"),
                new DartSourceDescriptor("button.dart", "ButtonScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}
