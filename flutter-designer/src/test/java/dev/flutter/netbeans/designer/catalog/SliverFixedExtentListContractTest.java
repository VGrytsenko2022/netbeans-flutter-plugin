package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverFixedExtentListContractTest {
    @Test void allThreeConstructorsHaveExactFieldsPresetsPlacementAndCallbackContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var viewport = catalog.find(new WidgetTypeId("flutter.widgets.CustomScrollView")).orElseThrow();
        var box = catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
        int properties = 0, callables = 0;
        for (var kind : SliverFixedExtentListWidgetPropertySchema.Kind.values()) {
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
            for (var field : SliverFixedExtentListWidgetPropertySchema.fields(kind)) {
                var p = definition.properties().stream().filter(v -> v.name().value().equals(field.name())).findFirst().orElseThrow();
                assertEquals(field.required(), p.parameter().required());
                assertEquals(field.required(), p.creationDefault().isPresent());
                if (field.required()) assertEquals(field.type().equals("double")
                        ? new PropertyValue.DoubleValue(new java.math.BigDecimal("48"))
                        : new PropertyValue.StringValue(field.preset()), node.properties().get(p.name()));
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
        assertEquals(14, properties); assertEquals(2, callables);
    }
    @Test void extentAndNullabilityFollowNativeLayoutContract() {
        for(var kind : SliverFixedExtentListWidgetPropertySchema.Kind.values()) {
            var definition = BuiltInWidgetCatalog.getDefault().find(kind.type()).orElseThrow();
            var extent = definition.property(new PropertyName("itemExtent")).orElseThrow();
            for(String value : List.of("0","0.125","48","1000"))
                assertTrue(extent.constraints().stream().anyMatch(c -> c.accepts(new PropertyValue.DoubleValue(new java.math.BigDecimal(value)))));
            for(PropertyValue value : List.of(new PropertyValue.DoubleValue(new java.math.BigDecimal("-1")),
                    new PropertyValue.NullValue(), new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                    new PropertyValue.StringValue("48"))) assertFalse(extent.constraints().stream().anyMatch(c -> c.accepts(value)));
            for(var field : SliverFixedExtentListWidgetPropertySchema.fields(kind)) {
                var property = definition.property(new PropertyName(field.name())).orElseThrow();
                assertEquals(field.type().endsWith("?"), property.acceptedKinds().contains(PropertyValueKind.NULL));
            }
        }
    }
}
