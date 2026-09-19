package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverDynamicContractTest {
    @Test void allSixConstructorsHaveExactFieldsPresetsPlacementAndCallbackContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var viewport = catalog.find(new WidgetTypeId("flutter.widgets.CustomScrollView")).orElseThrow();
        var box = catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        int properties = 0, callables = 0;
        for (var kind : SliverDynamicWidgetPropertySchema.Kind.values()) {
            var definition = catalog.find(kind.type()).orElseThrow();
            assertEquals(kind.constructor(), definition.namedConstructor());
            assertEquals(kind.constructor().isEmpty(), definition.constConstructor());
            assertEquals(Set.of(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT), definition.traits());
            assertFalse(WidgetPlacementRules.evaluateRoot(definition).accepted());
            assertTrue(WidgetPlacementRules.accepts(viewport, viewport.slots().getFirst(), definition));
            assertEquals(kind.hasChildren() ? 1 : 0, definition.slots().size());
            if (kind.hasChildren()) {
                var slot = definition.slots().getFirst();
                assertEquals(SlotCardinality.LIST, slot.cardinality());
                assertTrue(slot.parameter().required());
                assertTrue(WidgetPlacementRules.accepts(definition, slot, box));
                assertFalse(WidgetPlacementRules.accepts(definition, slot, definition));
            }
            var node = WidgetNodePrototypeFactory.create(definition, StableId.random());
            for (var field : SliverDynamicWidgetPropertySchema.fields(kind)) {
                var p = definition.properties().stream().filter(v -> v.name().value().equals(field.name())).findFirst().orElseThrow();
                assertEquals(field.required(), p.parameter().required());
                assertEquals(field.required(), p.creationDefault().isPresent());
                if (field.required()) assertEquals(new PropertyValue.StringValue(field.preset()), node.properties().get(p.name()));
                boolean callback = field.type().contains("WidgetBuilder") || field.type().equals("ChildIndexGetter?");
                var event = WidgetEventCatalog.find(kind.type(), p.name());
                assertEquals(callback, event.isPresent());
                if (callback) {
                    callables++;
                    assertEquals(field.required(), event.orElseThrow().sdkRequired());
                    if (field.required()) assertTrue(event.orElseThrow().unsetBehavior().contains("not allowed"));
                }
                properties++;
            }
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(definition, WidgetCapability.CANVAS));
        }
        assertEquals(31, properties); assertEquals(8, callables);
    }
    @Test void separatedIndexCallbacksCannotSilentlyOverrideOneAnother() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_index", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        properties.put(new PropertyName("findChildIndexCallback"), reference);
        properties.put(new PropertyName("findItemIndexCallback"), reference);
        var type = SliverDynamicWidgetPropertySchema.Kind.LIST_SEPARATED.type();
        assertTrue(SliverDynamicWidgetPropertySchema.conflict(new WidgetNode(StableId.random(), type, properties, Map.of())).isPresent());
        properties.put(new PropertyName("findChildIndexCallback"), new PropertyValue.NullValue());
        assertTrue(SliverDynamicWidgetPropertySchema.conflict(new WidgetNode(StableId.random(), type, properties, Map.of())).isEmpty());
    }
}
