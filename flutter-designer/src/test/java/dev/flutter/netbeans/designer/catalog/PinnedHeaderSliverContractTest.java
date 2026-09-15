package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinnedHeaderSliverContractTest {
    static final WidgetTypeId TYPE=PinnedHeaderSliverWidgetSchema.TYPE;
    static WidgetNode header(int mask) {
        var slots=new LinkedHashMap<SlotName,WidgetSlot>();
        for(int i=0;i<1;i++) slots.put(new SlotName(List.of("child").get(i)),
                (mask&(1<<i))==0?WidgetSlot.SingleSlot.empty():WidgetSlot.SingleSlot.of(box(i)));
        return new WidgetNode(StableId.random(),TYPE,Map.of(),slots);
    }
    static WidgetNode box(int index) {
        return new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("height"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(40+index*40))),Map.of());
    }
    @Test void exactConstructorSlotsCapabilitiesPlacementAndNoInventedEvents() {
        var catalog=BuiltInWidgetCatalog.getDefault();var d=catalog.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());assertTrue(d.properties().isEmpty());
        assertEquals(Set.of(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),d.traits());
        assertEquals(List.of("child"),d.slots().stream().map(s->s.name().value()).toList());
        for(var slot:d.slots()) {
            assertFalse(slot.parameter().required());assertEquals(SlotCardinality.SINGLE,slot.cardinality());
            assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());
            assertEquals(new SlotAcceptance.AnyWidget(),slot.acceptance());
            for(var child:catalog.definitions()) assertEquals(!WidgetPlacementRules.isSliverWidget(child)
                    && !Set.of("flutter.widgets.Expanded","flutter.widgets.Flexible","flutter.widgets.Spacer","flutter.widgets.LayoutId","flutter.widgets.TableRow","flutter.widgets.TableCell").contains(child.typeId().value()) && !WidgetPlacementRules.isStackPositionedWidget(child),
                    WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        }
        var prototype=WidgetNodePrototypeFactory.create(d,StableId.random());assertTrue(prototype.properties().isEmpty());
        assertEquals(1,prototype.slots().size());assertTrue(prototype.slots().values().stream().allMatch(s->((WidgetSlot.SingleSlot)s).child().isEmpty()));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        for(var capability:WidgetCapability.values()) assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        int accepted=0;
        for(var parent:catalog.definitions())for(var slot:parent.slots()) {
            boolean sliverSlot=slot.acceptance().equals(new SlotAcceptance.HasTrait(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT));
            assertEquals(sliverSlot,WidgetPlacementRules.accepts(parent,slot,d));
            if(sliverSlot)accepted++;
        }
        assertEquals(20,accepted);
    }
    @Test void bothNullableChildStatesRoundTripWithExactConstructorArguments() throws Exception {
        for(int mask=0;mask<2;mask++) {
            var widget=header(mask);
            var doc=SliverCrossAxisExpandedContractTest.document(widget);
            var catalog=BuiltInWidgetCatalog.getDefault();
            assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var generated=new DartRegionGenerator().generate(doc,catalog).generated().orElseThrow();
            String code=generated.build().payload();assertTrue(code.contains("PinnedHeaderSliver("),code);
            int previous=-1;
            for(int i=0;i<1;i++) {
                var name=List.of("child").get(i);
                // Inspect the header invocation only (its outer adapter can also contain a child argument).
                String headerCode=code.substring(code.indexOf("PinnedHeaderSliver("));
                int pos=headerCode.indexOf(name+":");
                assertTrue(pos>=0,code);
                assertEquals((mask&(1<<i))==0,headerCode.contains(name+": null"),code);
                if(pos>=0) { assertTrue(pos>previous,code);previous=pos; }
            }
            assertFalse(code.contains("SliverPersistentHeaderDelegate"));
            assertTrue(generated.symbolOccurrences().stream().anyMatch(s->s.symbolName().equals("PinnedHeaderSliver")));
        }
    }
    @Test void rawPropertiesAndSliverChildAreRejected() {
        var widget=header(0);var catalog=BuiltInWidgetCatalog.getDefault();
        for(String property:List.of("minExtent","maxExtent","delegate","pinned","floating")) {
            var bad=new WidgetNode(widget.id(),TYPE,Map.of(new PropertyName(property),new PropertyValue.BooleanValue(true)),widget.slots());
            assertFalse(new WidgetTreeValidator().validate(SliverCrossAxisExpandedContractTest.document(bad),catalog).valid());
        }
        for(var slot:List.of("child")) {
            var slots=new LinkedHashMap<>(widget.slots());slots.put(new SlotName(slot),WidgetSlot.SingleSlot.of(SliverCrossAxisExpandedContractTest.adapter()));
            assertFalse(new WidgetTreeValidator().validate(SliverCrossAxisExpandedContractTest.document(new WidgetNode(widget.id(),TYPE,Map.of(),slots)),catalog).valid());
        }
    }
}
