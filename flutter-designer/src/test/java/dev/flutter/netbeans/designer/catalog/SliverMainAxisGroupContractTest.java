package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverMainAxisGroupContractTest {
    @Test void completeConstructorHasOnlyRequiredSliversAndGeneratedKey() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var d = catalog.find(SliverMainAxisGroupWidgetPropertySchema.TYPE).orElseThrow();
        assertTrue(d.properties().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertEquals(1,d.slots().size());
        var slot=d.slots().getFirst();
        assertEquals("slivers",slot.name().value());
        assertTrue(slot.parameter().required()); assertEquals(SlotCardinality.LIST,slot.cardinality());
        assertEquals(0,slot.minChildren()); assertEquals(10000,slot.maxChildren());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(d));
        var node=WidgetNodePrototypeFactory.create(d,StableId.random());
        assertTrue(node.properties().isEmpty());
        assertEquals(Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(List.of())),node.slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,WidgetCapability.CANVAS));
        for(var child:catalog.definitions())
            assertEquals(WidgetPlacementRules.isSliverWidget(child) && !child.typeId().equals(SliverCrossAxisExpandedWidgetPropertySchema.TYPE),WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        assertTrue(WidgetPlacementRules.accepts(d,slot,d),"Nested groups are valid; cycles of widget instances are not.");
    }
}
