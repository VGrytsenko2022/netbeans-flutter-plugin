package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complementary complete local theme branches, with original model paths. */
public final class TooltipThemeTestValues {
    private TooltipThemeTestValues() { }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE).orElseThrow(); }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue value(String name) { return name.equals("data") ? TooltipTestValues.reference("themeData") : TooltipTestValues.value(name); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean paints) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : TooltipThemeWidgetPropertySchema.localProperties()) {
            if (List.of("height", "textStyle").contains(name)) continue;
            if (paints && List.of("textStyleColor", "textStyleBackgroundColor").contains(name)) continue;
            if (!paints && List.of("textStyleForeground", "textStyleBackground").contains(name)) continue;
            values.put(p(name), value(name));
        }
        return values;
    }
    public static WidgetNode text() { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue("Theme anchor")), Map.of()); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> values) { return node(values, text()); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> values, WidgetNode child) {
        return new WidgetNode(StableId.random(), TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE, values,
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(child))));
    }
    public static DesignerDocument document(WidgetNode root) {
        var original = TooltipTestValues.document(root);
        if (root.propertyBindings().isEmpty()) return original;
        var source = original.source();
        return new DesignerDocument(original.documentId(), new DartSourceDescriptor(source.dartFile(), source.className(),
                WidgetClassKind.STATEFUL, source.generatorVersion(), source.managedRegions()), root);
    }
}
