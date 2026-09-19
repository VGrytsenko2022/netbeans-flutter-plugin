package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

public final class MenuItemButtonTestValues {
    private MenuItemButtonTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE).orElseThrow(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> overrides) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("enabled"), new PropertyValue.BooleanValue(true)); values.putAll(overrides);
        return new WidgetNode(StableId.random(), definition().typeId(), values, Map.of());
    }
    public static WidgetNode text(String value) { return TextButtonTestValues.text(value); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean paints, boolean circles) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        TextButtonTestValues.full(false, paints, circles).forEach((name, value) -> {
            if (MenuItemButtonWidgetPropertySchema.localStyleProperties().contains(name.value())) values.put(name, value);
        });
        values.put(p("shortcutTrigger"), new PropertyValue.EnumValue("LogicalKeyboardKey", "keyA"));
        values.put(p("shortcutControl"), new PropertyValue.BooleanValue(true));
        values.put(p("shortcutNumLock"), new PropertyValue.EnumValue("LockState", "locked"));
        return values;
    }
    public static DesignerDocument document(WidgetNode root) {
        var original = TextButtonTestValues.document(root);
        if (root.propertyBindings().isEmpty()) return original;
        var source = original.source();
        return new DesignerDocument(original.documentId(), new DartSourceDescriptor(source.dartFile(), source.className(),
                WidgetClassKind.STATEFUL, source.generatorVersion(), source.managedRegions()), root);
    }
}
