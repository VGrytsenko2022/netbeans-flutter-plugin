package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliverPrototypeExtentListContractTest {
    @Test void nativeFieldsAndMeasurementSlotsHaveExactRequirednessAndPlacement() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        int properties = 0;
        for(var kind : SliverPrototypeExtentListWidgetPropertySchema.Kind.values()) {
            var definition = catalog.find(kind.type()).orElseThrow();
            assertEquals(kind.constructor(),definition.namedConstructor());
            assertEquals(kind == SliverPrototypeExtentListWidgetPropertySchema.Kind.DELEGATE,definition.constConstructor());
            assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(definition));
            assertFalse(WidgetPlacementRules.evaluateRoot(definition).accepted());
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(definition,WidgetCapability.CANVAS));
            assertEquals(kind.hasChildren()?2:1,definition.slots().size());
            var prototype = definition.slot(new SlotName("prototypeItem")).orElseThrow();
            assertTrue(prototype.parameter().required());
            assertEquals(SlotCardinality.SINGLE,prototype.cardinality());
            assertEquals(0,prototype.minChildren()); assertEquals(1,prototype.maxChildren());
            assertTrue(WidgetPlacementRules.accepts(definition,prototype,catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow()));
            for(String rejected : List.of("SliverPadding","Expanded","Flexible","Spacer"))
                assertFalse(WidgetPlacementRules.accepts(definition,prototype,catalog.find(new WidgetTypeId("flutter.widgets."+rejected)).orElseThrow()));
            assertTrue(definition.property(new PropertyName("itemExtent")).isEmpty());
            assertTrue(definition.property(new PropertyName("semanticIndexOffset")).isEmpty(),"No such native constructor argument");
            var created = WidgetNodePrototypeFactory.create(definition,StableId.random());
            assertTrue(((WidgetSlot.SingleSlot)created.slots().get(prototype.name())).child().isEmpty());
            for(var field : SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
                var property = definition.property(new PropertyName(field.name())).orElseThrow();
                assertEquals(field.required(),property.parameter().required());
                assertEquals(field.required(),property.creationDefault().isPresent());
                assertEquals(field.type().endsWith("?"),property.acceptedKinds().contains(PropertyValueKind.NULL));
                properties++;
            }
            var callables = WidgetEventCatalog.eventsFor(definition);
            assertEquals(kind == SliverPrototypeExtentListWidgetPropertySchema.Kind.BUILDER?2:0,callables.size());
            assertTrue(callables.stream().noneMatch(v -> v.kind() == WidgetEventDescriptor.Kind.EVENT));
        }
        assertEquals(10,properties);
    }
}

