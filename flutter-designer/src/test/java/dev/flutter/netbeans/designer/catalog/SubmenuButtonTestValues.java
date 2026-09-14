package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

public final class SubmenuButtonTestValues {
    private SubmenuButtonTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE).orElseThrow(); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return MenuAnchorTestValues.reference(name); }
    public static WidgetNode text(String text) { return TextButtonTestValues.text(text); }
    public static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        return new WidgetNode(StableId.random(), definition().typeId(), values, Map.of(
                new SlotName("child"), new WidgetSlot.SingleSlot(Optional.empty()),
                new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of())));
    }
    public static DesignerDocument document(WidgetNode root) { return MenuAnchorTestValues.document(root); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean circles) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        TextButtonTestValues.full(false, circles, circles).forEach((name, value) -> {
            if (SubmenuButtonWidgetPropertySchema.localStyleProperties().contains(name.value())) values.put(name, value);
        });
        MenuAnchorTestValues.full(circles).forEach((name, value) -> values.put(p(SubmenuButtonWidgetPropertySchema.menuStylePropertyName(name.value())), value));
        return values;
    }
}
