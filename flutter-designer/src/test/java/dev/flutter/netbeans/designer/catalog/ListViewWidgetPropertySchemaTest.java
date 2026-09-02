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

class ListViewWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448StaticSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ListViewWidgetPropertySchema.LIST_VIEW_TYPE).orElseThrow();

        assertEquals("ListView", definition.dartClassName());
        assertFalse(definition.constConstructor(),
                "Flutter 3.44.8 ListView(List<Widget>) is not const");
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of(
                        "package:flutter/gestures.dart",
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals("flutter.scrolling", definition.palette().categoryId());
        assertEquals(250, definition.palette().categoryOrder());
        assertEquals("ListView", definition.palette().displayName());

        assertEquals(List.of(
                        "scrollDirection", "reverse", "primary", "physics",
                        "shrinkWrap", "padding", "itemExtent",
                        "addAutomaticKeepAlives", "addRepaintBoundaries",
                        "addSemanticIndexes", "scrollCacheExtent",
                        "semanticChildCount", "dragStartBehavior",
                        "keyboardDismissBehavior", "restorationId",
                        "clipBehavior", "hitTestBehavior"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(ListViewWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        for (int order = 0; order < definition.properties().size(); order++) {
            PropertyDefinition property = definition.properties().get(order);
            assertEquals(order < 11 ? order : order + 1,
                    property.parameter().order());
            assertFalse(property.parameter().required());
            assertTrue(property.creationDefault().isEmpty());
        }

        assertEquals(Set.of(PropertyValueKind.STRING), accepted(definition, "physics"));
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                accepted(definition, "scrollCacheExtent"));
        assertEquals(Set.of(PropertyValueKind.INTEGER),
                accepted(definition, "semanticChildCount"));
        PropertyValueConstraint.EnumValues hitTest = definition
                .property(new PropertyName("hitTestBehavior")).orElseThrow()
                .constraints().stream()
                .map(PropertyValueConstraint.EnumValues.class::cast)
                .findFirst().orElseThrow();
        assertEquals(new DartSymbolReference(
                        "package:flutter/rendering.dart", "HitTestBehavior"),
                hitTest.dartType());
        assertEquals(List.of("deferToChild", "opaque", "translucent"),
                hitTest.values());

        assertEquals(1, definition.slots().size());
        SlotDefinition children = definition.slot(new SlotName("children")).orElseThrow();
        assertEquals(11, children.parameter().order());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
    }

    @Test
    void prototypeIsAValidEmptyStaticListWithoutStoredFrameworkDefaults() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ListViewWidgetPropertySchema.LIST_VIEW_TYPE).orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertTrue(prototype.properties().isEmpty());
        WidgetSlot.ListSlot children = (WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("children"));
        assertTrue(children.children().isEmpty());
    }

    private static Set<PropertyValueKind> accepted(
            WidgetDefinition definition,
            String name) {
        return definition.property(new PropertyName(name)).orElseThrow().acceptedKinds();
    }
}
