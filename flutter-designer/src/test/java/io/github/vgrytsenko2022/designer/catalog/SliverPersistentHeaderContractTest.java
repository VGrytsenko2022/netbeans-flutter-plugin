package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverPersistentHeaderContractTest {
    static final WidgetTypeId TYPE = SliverPersistentHeaderWidgetPropertySchema.TYPE;
    static final PropertyName DELEGATE = new PropertyName("delegate");
    static WidgetNode node(PropertyValue value) {
        return new WidgetNode(StableId.random(), TYPE, value == null ? Map.of() : Map.of(DELEGATE,value), Map.of());
    }
    @Test void exactSdkConstructorCapabilitiesAndPlacement() {
        var catalog=BuiltInWidgetCatalog.getDefault(); var d=catalog.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertEquals(SliverPersistentHeaderWidgetPropertySchema.properties(),d.properties());
        assertTrue(d.slots().isEmpty()); assertEquals(Set.of(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),d.traits());
        assertTrue(d.properties().getFirst().parameter().required());
        assertEquals(SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE,WidgetNodePrototypeFactory.create(d,StableId.random()).properties().get(DELEGATE));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        int accepted=0;
        for(var parent:catalog.definitions())for(var slot:parent.slots()) {
            boolean actual=WidgetPlacementRules.accepts(parent,slot,d);
            boolean sliverSlot=slot.acceptance().equals(new SlotAcceptance.HasTrait(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT));
            assertEquals(sliverSlot,actual,parent.typeId()+"."+slot.name());
            if(actual)accepted++;
        }
        assertEquals(20,accepted);
        for(var capability:List.of(WidgetCapability.CANVAS,WidgetCapability.CREATE))
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
        assertTrue(WidgetEventCatalog.find(TYPE,DELEGATE).isEmpty());
    }
    @Test void starterReferenceRoundTripsAndRequiresRealDelegateType() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var doc=SliverCrossAxisExpandedContractTest.document(node(SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE));
        assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
        var generated=new DartRegionGenerator().generate(doc,catalog).generated().orElseThrow();
        assertTrue(generated.build().payload().contains("delegate: const _FlutterDesignerPersistentHeaderDelegate()"),generated.build().payload());
        assertTrue(generated.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream())
                .anyMatch(p->p.expectedDartType().equals("SliverPersistentHeaderDelegate")));
        var codec=new FdDocumentCodec();var bytes=codec.encode(doc);
        assertEquals(doc,((FdDecodeResult.Current)codec.decode(bytes)).document());
    }
    @Test void referencesGettersFactoriesAndMembersCarryStrictTypeProof() {
        for(boolean factory:List.of(false,true))for(boolean member:List.of(false,true))for(boolean imported:List.of(false,true)) {
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/layouts.dart"):Optional.empty(),
                member?"Layouts":"buildSliver",member?Optional.of("responsive"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var doc=SliverCrossAxisExpandedContractTest.document(node(value));
            var generated=new DartRegionGenerator().generate(doc,BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
            assertTrue(generated.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream())
                .anyMatch(p->p.expectedDartType().equals("SliverPersistentHeaderDelegate")));
            assertFalse(generated.build().payload().contains("(context, constraints) =>"));
        }
    }
    @Test void missingNullRawCodeUnreviewedPresetsAndInventedChildrenAreRejected() {
        var catalog=BuiltInWidgetCatalog.getDefault();
        for(PropertyValue bad:Arrays.asList(null,new PropertyValue.NullValue(),new PropertyValue.StringValue("noop"),
                new PropertyValue.StringValue("(_) => Text('bad')"),new PropertyValue.BooleanValue(true),
                new PropertyValue.CallbackValue("buildSliver"))) {
            var doc=SliverCrossAxisExpandedContractTest.document(node(bad));
            assertFalse(new WidgetTreeValidator().validate(doc,catalog).valid(),String.valueOf(bad));
        }
        var widget=node(SliverPersistentHeaderWidgetPropertySchema.INITIAL_DELEGATE);
        for(String slot:List.of("child","sliver","children")) {
            var bad=new WidgetNode(widget.id(),TYPE,widget.properties(),
                Map.of(new SlotName(slot),WidgetSlot.SingleSlot.of(SliverCrossAxisExpandedContractTest.adapter())));
            assertFalse(new WidgetTreeValidator().validate(SliverCrossAxisExpandedContractTest.document(bad),catalog).valid());
        }
    }
}
