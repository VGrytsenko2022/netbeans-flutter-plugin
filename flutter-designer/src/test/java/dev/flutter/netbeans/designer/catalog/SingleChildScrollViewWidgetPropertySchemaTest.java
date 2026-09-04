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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleChildScrollViewWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DeclarativeSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE)
                .orElseThrow();

        assertEquals("SingleChildScrollView", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals(List.of(
                        "package:flutter/gestures.dart",
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.scrolling", 250, 30, "SingleChildScrollView"),
                definition.palette());

        assertEquals(List.of(
                        "scrollDirection", "reverse", "padding", "primary", "physics",
                        "dragStartBehavior", "clipBehavior", "hitTestBehavior",
                        "restorationId", "keyboardDismissBehavior"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(SingleChildScrollViewWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 6, 7, 8, 9, 10),
                definition.properties().stream()
                        .map(property -> property.parameter().order()).toList());
        assertTrue(definition.properties().stream()
                .noneMatch(property -> property.parameter().required()));
        assertEquals(Set.of(PropertyValueKind.STRING), accepted(definition, "physics"));
        assertEquals(Set.of(PropertyValueKind.EDGE_INSETS), accepted(definition, "padding"));
        assertEquals(Set.of(PropertyValueKind.ENUM),
                accepted(definition, "hitTestBehavior"));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(5, child.parameter().order());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertTrue(child.acceptance() instanceof SlotAcceptance.AnyWidget);
    }

    @Test
    void prototypeStoresNoFrameworkDefaultsAndStartsChildless() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertTrue(prototype.properties().isEmpty());
        WidgetSlot.SingleSlot child = (WidgetSlot.SingleSlot) prototype.slots()
                .get(new SlotName("child"));
        assertTrue(child.child().isEmpty());
    }

    private static Set<PropertyValueKind> accepted(
            WidgetDefinition definition,
            String name) {
        return definition.property(new PropertyName(name)).orElseThrow().acceptedKinds();
    }
}
