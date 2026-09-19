package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MenuBarTestValues {
    private MenuBarTestValues() { }
    public static PropertyName p(String name) { return new PropertyName(name); }
    public static WidgetDefinition definition() {
        return BuiltInWidgetCatalog.getDefault().find(MenuBarWidgetPropertySchema.MENU_BAR_TYPE).orElseThrow();
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) {
        return MenuAnchorTestValues.reference(name);
    }
    public static WidgetNode node(Map<PropertyName, PropertyValue> overrides) {
        return new WidgetNode(StableId.random(), definition().typeId(), overrides,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of())));
    }
    public static WidgetNode text(String value) { return MenuAnchorTestValues.text(value); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean circles) {
        return MenuAnchorTestValues.full(circles);
    }
    public static DesignerDocument document(WidgetNode root) {
        return MenuAnchorTestValues.document(root);
    }
}
