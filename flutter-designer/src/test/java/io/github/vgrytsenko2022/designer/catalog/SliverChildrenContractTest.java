package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverChildrenContractTest {
    private final WidgetCatalog catalog = BuiltInWidgetCatalog.getDefault();

    @Test void staticConstructorsHaveCompleteTypedPropertiesAndDefaults() {
        for (var type : SliverChildrenWidgetPropertySchema.TYPES) {
            var definition = catalog.find(type).orElseThrow();
            assertFalse(definition.constConstructor());
            assertEquals(SliverChildrenWidgetPropertySchema.definitions(type).keySet(),
                    new LinkedHashSet<>(definition.properties().stream().map(p -> p.name().value()).toList()));
            assertEquals(Set.of(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT), definition.traits());
            assertEquals(SlotCardinality.LIST, definition.slots().getFirst().cardinality());
            assertEquals(type.equals(SliverChildrenWidgetPropertySchema.LIST),
                    definition.slots().getFirst().parameter().required());
            for (var property : definition.properties()) {
                if (property.parameter().required()) assertTrue(property.creationDefault().isPresent());
            }
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(definition, WidgetCapability.CANVAS));
        }
    }

    @Test void sliversOnlyEnterSliverSlotsAndTheirChildrenRemainBoxes() {
        var viewport = catalog.find(new WidgetTypeId("flutter.widgets.CustomScrollView")).orElseThrow();
        var adapter = catalog.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow();
        var slivers = new ArrayList<WidgetDefinition>();
        for (var type : SliverChildrenWidgetPropertySchema.TYPES) slivers.add(catalog.find(type).orElseThrow());
        slivers.add(adapter);
        for (var sliver : slivers) {
            assertFalse(WidgetPlacementRules.evaluateRoot(sliver).accepted());
            assertTrue(WidgetPlacementRules.accepts(viewport, viewport.slots().getFirst(), sliver));
            for (var parent : slivers) {
                for (var slot : parent.slots()) {
                    assertFalse(WidgetPlacementRules.accepts(parent, slot, sliver));
                }
            }
            for (String type : List.of("flutter.widgets.Text", "flutter.widgets.Image", "flutter.widgets.CustomScrollView")) {
                assertTrue(WidgetPlacementRules.accepts(sliver, sliver.slots().getFirst(),
                        catalog.find(new WidgetTypeId(type)).orElseThrow()));
            }
        }
    }
}
