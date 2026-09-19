package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageViewWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448StaticSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE).orElseThrow();

        assertEquals("PageView", definition.dartClassName());
        assertFalse(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of(
                        "package:flutter/gestures.dart",
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.scrolling", 250, 40, "PageView"),
                definition.palette());
        assertEquals(PageViewWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(
                        "scrollDirection", "reverse", "controller", "physics",
                        "pageSnapping", "onPageChanged", "dragStartBehavior",
                        "allowImplicitScrolling", "scrollCacheExtent", "restorationId",
                        "clipBehavior", "hitTestBehavior", "scrollBehavior", "padEnds"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14),
                definition.properties().stream()
                        .map(property -> property.parameter().order()).toList());
        assertTrue(definition.properties().stream()
                .noneMatch(property -> property.parameter().required()));
        assertEquals(Set.of(PropertyValueKind.STRING), accepted(definition, "physics"));
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE,
                        PropertyValueKind.NULL),
                accepted(definition, "scrollCacheExtent"));
        assertEquals(Set.of(PropertyValueKind.CALLBACK, PropertyValueKind.DART_OBJECT_REFERENCE,
                        PropertyValueKind.NULL, PropertyValueKind.STRING),
                accepted(definition, "onPageChanged"));

        SlotDefinition children = definition.slot(new SlotName("children")).orElseThrow();
        assertEquals(6, children.parameter().order());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
    }

    @Test
    void prototypeIsAnEmptyPageListWithoutStoredFrameworkDefaults() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE).orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertTrue(prototype.properties().isEmpty());
        WidgetSlot.ListSlot children = (WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("children"));
        assertTrue(children.children().isEmpty());
    }

    private static Set<PropertyValueKind> accepted(
            WidgetDefinition definition, String name) {
        return definition.property(new PropertyName(name)).orElseThrow().acceptedKinds();
    }
}
