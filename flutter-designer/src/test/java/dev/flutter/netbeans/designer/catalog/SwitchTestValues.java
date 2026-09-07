package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;

/** Complementary legal Switch projections cover all 201 metadata fields. */
public final class SwitchTestValues {
    private SwitchTestValues() {
    }

    public static PropertyName p(String value) { return new PropertyName(value); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    public static PropertyValue.BooleanValue b(boolean value) { return new PropertyValue.BooleanValue(value); }
    public static PropertyValue.NullValue nil() { return new PropertyValue.NullValue(); }
    public static PropertyValue.ColorValue color() { return CheckboxTestValues.color(); }
    public static PropertyValue.ThemeTokenValue theme() { return CheckboxTestValues.theme(); }
    public static PropertyValue.DartObjectReferenceValue reference(String value) { return FilledButtonTestValues.reference(value); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(SwitchWidgetPropertySchema.SWITCH_TYPE).orElseThrow();
    }

    public static PropertyValue value(String name) {
        var property = definition().property(p(name)).orElseThrow();
        if (property.creationDefault().isPresent()) return property.creationDefault().orElseThrow();
        if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference(name);
        if (property.acceptedKinds().contains(PropertyValueKind.COLOR)) return color();
        if (property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)) return b(true);
        if (property.acceptedKinds().contains(PropertyValueKind.IMAGE_PROVIDER)) return PropertyValue.ImageProviderValue.asset("assets/switch.png");
        if (property.acceptedKinds().contains(PropertyValueKind.ICON_DATA)) {
            return BuiltInWidgetCatalog.getDefault().find(IconWidgetPropertySchema.ICON_TYPE).orElseThrow()
                    .property(p("icon")).orElseThrow().creationDefault().orElseThrow();
        }
        if (property.acceptedKinds().contains(PropertyValueKind.SHADOW_LIST)) return new PropertyValue.ShadowListValue(List.of());
        if (property.acceptedKinds().contains(PropertyValueKind.EDGE_INSETS)) {
            return new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO);
        }
        if (name.endsWith("Mode") && SwitchWidgetPropertySchema.iconSourceName(name).isEmpty()) return s("icon");
        if (name.endsWith("SemanticLabel")) return s("Thumb glyph\nAccessible metadata");
        if (name.equals("splashRadius")) return new PropertyValue.EnumValue("double", "infinity");
        for (var constraint : property.constraints()) {
            if (constraint instanceof PropertyValueConstraint.EnumValues values) {
                return new PropertyValue.EnumValue(values.dartType().name(), values.values().getLast());
            }
        }
        return d("0.25");
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : SwitchWidgetPropertySchema.definitions().keySet()) {
            if (SwitchWidgetPropertySchema.colorFamilies().contains(name) || name.equals("trackOutlineWidth") || name.equals("thumbIcon")) continue;
            if (name.equals("applyCupertinoTheme") && variant.equals("standard")) continue;
            properties.put(p(name), value(name));
        }
        properties.put(p("variant"), s(variant));
        return properties;
    }

    public static WidgetNode node(Map<PropertyName, PropertyValue> properties) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("value"), b(false));
        values.put(p("variant"), s("standard"));
        values.put(p("enabled"), b(true));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), SwitchWidgetPropertySchema.SWITCH_TYPE, values, Map.of());
    }

    public static DesignerDocument document(WidgetNode root) { return FilledButtonTestValues.document(root); }
}
