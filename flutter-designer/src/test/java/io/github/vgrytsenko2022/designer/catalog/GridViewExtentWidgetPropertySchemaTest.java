package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridViewExtentWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448ExtentSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE)
                .orElseThrow();

        assertEquals("flutter.widgets.GridView.extent", definition.typeId().value());
        assertEquals("GridView", definition.dartClassName());
        assertEquals("extent", definition.namedConstructor().orElseThrow());
        assertFalse(definition.constConstructor());
        assertEquals(List.of(
                        "package:flutter/gestures.dart",
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals(new PaletteMetadata(
                "flutter.scrolling", 250, 25, "GridView.extent"), definition.palette());

        assertEquals(List.of(
                        "scrollDirection", "reverse", "primary", "physics",
                        "shrinkWrap", "padding", "maxCrossAxisExtent",
                        "mainAxisSpacing", "crossAxisSpacing", "childAspectRatio",
                        "mainAxisExtent", "addAutomaticKeepAlives",
                        "addRepaintBoundaries", "addSemanticIndexes",
                        "scrollCacheExtent", "semanticChildCount",
                        "dragStartBehavior", "keyboardDismissBehavior",
                        "restorationId", "clipBehavior", "hitTestBehavior"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(GridViewExtentWidgetPropertySchema.definitions().keySet().stream()
                        .toList(), definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        for (PropertyDefinition property : definition.properties()) {
            int order = property.parameter().order();
            assertEquals(order < 15 ? order : order - 1,
                    definition.properties().indexOf(property));
            assertEquals("maxCrossAxisExtent".equals(property.name().value()),
                    property.parameter().required());
        }
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.valueOf(200.0)),
                definition.property(new PropertyName("maxCrossAxisExtent")).orElseThrow()
                        .creationDefault().orElseThrow());
        assertEquals(Set.of(PropertyValueKind.DOUBLE),
                accepted(definition, "maxCrossAxisExtent"));
        assertEquals(Set.of(PropertyValueKind.DOUBLE),
                accepted(definition, "mainAxisSpacing"));
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                accepted(definition, "scrollCacheExtent"));

        SlotDefinition children = definition.slot(new SlotName("children")).orElseThrow();
        assertEquals(15, children.parameter().order());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
    }

    @Test
    void prototypeStoresOnlyTheRequiredMaximumCrossAxisExtentDefault() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertEquals(java.util.Map.of(
                        new PropertyName("maxCrossAxisExtent"),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(200.0))),
                prototype.properties());
        WidgetSlot.ListSlot children = (WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("children"));
        assertTrue(children.children().isEmpty());
    }

    private static Set<PropertyValueKind> accepted(
            WidgetDefinition definition, String name) {
        return definition.property(new PropertyName(name)).orElseThrow().acceptedKinds();
    }
}
