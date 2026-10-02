package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

public final class MenuAnchorTestValues {
    private MenuAnchorTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE).orElseThrow(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> overrides) {
        return new WidgetNode(StableId.random(), definition().typeId(), overrides,
                Map.of(new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of())));
    }
    public static WidgetNode text(String value) { return TextButtonTestValues.text(value); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean circles) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        TextButtonTestValues.full(false, false, circles).forEach((name, value) -> {
            if (MenuAnchorWidgetPropertySchema.localStyleProperties().contains(name.value())) values.put(name, value);
        });
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
