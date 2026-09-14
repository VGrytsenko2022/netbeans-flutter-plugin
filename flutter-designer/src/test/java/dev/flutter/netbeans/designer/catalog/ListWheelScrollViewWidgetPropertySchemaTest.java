package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListWheelScrollViewWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448StaticSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE)
                .orElseThrow();

        assertEquals("ListWheelScrollView", definition.dartClassName());
        assertFalse(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of(
                        "package:flutter/gestures.dart",
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.scrolling", 250, 50, "ListWheelScrollView"),
                definition.palette());
        assertEquals(ListWheelScrollViewWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream().map(property -> property.name().value()).toList());
        assertEquals(List.of(
                        "controller", "physics", "diameterRatio", "perspective", "offAxisFraction",
                        "useMagnifier", "magnification", "overAndUnderCenterOpacity", "itemExtent", "squeeze",
                        "onSelectedItemChanged", "renderChildrenOutsideViewport", "clipBehavior", "hitTestBehavior",
                        "restorationId", "scrollBehavior", "dragStartBehavior", "changeReportingBehavior"),
                definition.properties().stream().map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
                definition.properties().stream().map(property -> property.parameter().order()).toList());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                definition.property(new PropertyName("itemExtent")).orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE,
                        PropertyValueKind.NULL, PropertyValueKind.STRING),
                definition.property(new PropertyName("onSelectedItemChanged")).orElseThrow().acceptedKinds());

        SlotDefinition children = definition.slot(new SlotName("children")).orElseThrow();
        assertEquals(18, children.parameter().order());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
    }

    @Test
    void prototypeIsAnEmptyWheelListWithoutStoredFrameworkDefaults() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(new dev.flutter.netbeans.designer.model.PropertyValue.IntegerValue(java.math.BigInteger.valueOf(50)),
                prototype.properties().get(new PropertyName("itemExtent")));
        WidgetSlot.ListSlot children = (WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("children"));
        assertTrue(children.children().isEmpty());
    }
}
