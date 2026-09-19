package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrientationBuilderContractTest {
    static final WidgetTypeId TYPE = OrientationBuilderWidgetPropertySchema.TYPE;
    static final PropertyName BUILDER = new PropertyName("builder");
    static WidgetNode node(PropertyValue value) {
        return new WidgetNode(StableId.random(), TYPE, value == null ? Map.of() : Map.of(BUILDER,value), Map.of());
    }
    @Test void exactSdkConstructorCapabilitiesAndPlacement() {
        var catalog=BuiltInWidgetCatalog.getDefault(); var d=catalog.find(TYPE).orElseThrow();
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertEquals(OrientationBuilderWidgetPropertySchema.properties(),d.properties());
        assertTrue(d.slots().isEmpty()); assertTrue(d.traits().isEmpty());
        assertTrue(d.properties().getFirst().parameter().required());
        assertEquals(new PropertyValue.StringValue("empty"),WidgetNodePrototypeFactory.create(d,StableId.random()).properties().get(BUILDER));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        int accepted=0;
        for(var parent:catalog.definitions())for(var slot:parent.slots()) {
            boolean actual=WidgetPlacementRules.accepts(parent,slot,d);
            boolean boxSlot=slot.acceptance() instanceof SlotAcceptance.AnyWidget;
            assertEquals(boxSlot,actual,parent.typeId()+"."+slot.name());
            if(actual)accepted++;
        }
        assertTrue(accepted > 0);
        for(var capability:List.of(WidgetCapability.CANVAS,WidgetCapability.CREATE))
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
        var callable=WidgetEventCatalog.find(TYPE,BUILDER).orElseThrow();
        assertEquals(WidgetEventDescriptor.Kind.BUILDER,callable.kind());assertTrue(callable.sdkRequired());
        assertEquals("OrientationWidgetBuilder",callable.callbackType());
        assertFalse(callable.nullableCallback());assertFalse(callable.defaultEvent());
        assertEquals("Widget Function(BuildContext, Orientation)",callable.signature().dartFunctionType());
        assertTrue(callable.signature().importUris().contains("package:flutter/widgets.dart"));
        assertTrue(callable.unsetBehavior().contains("not allowed"));
    }
    @Test void emptyPresetHasExactManifestedBoxAndNoFakeSlot() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var doc=FlexibleSpaceBarContractTest.doc(node(new PropertyValue.StringValue("empty")));
        assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
        var result=new DartRegionGenerator().generate(doc,catalog).generated().orElseThrow();
        var code=result.build().payload();
        assertTrue(code.contains("builder: (context, orientation) => const SizedBox.shrink()"),code);
        assertFalse(code.contains("const OrientationBuilder("),code);
        assertTrue(result.symbolOccurrences().stream().anyMatch(s->s.symbolName().equals("SizedBox")));
        assertTrue(result.symbolOccurrences().stream().anyMatch(s->s.symbolName().equals("shrink")));
        var codec=new FdDocumentCodec(); var bytes=codec.encode(doc);
        assertEquals(doc,((FdDecodeResult.Current)codec.decode(bytes)).document());
    }
    @Test void referencesGettersFactoriesAndMembersCarryStrictTypeProof() {
        for(boolean factory:List.of(false,true))for(boolean member:List.of(false,true))for(boolean imported:List.of(false,true)) {
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/layouts.dart"):Optional.empty(),
                member?"Layouts":"buildSliver",member?Optional.of("responsive"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var doc=FlexibleSpaceBarContractTest.doc(node(value));
            var generated=new DartRegionGenerator().generate(doc,BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
            assertTrue(generated.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream())
                .anyMatch(p->p.expectedDartType().equals("OrientationWidgetBuilder")));
            assertFalse(generated.build().payload().contains("(context, orientation) =>"));
        }
    }
    @Test void missingNullRawCodeUnreviewedPresetsAndInventedChildrenAreRejected() {
        var catalog=BuiltInWidgetCatalog.getDefault();
        for(PropertyValue bad:Arrays.asList(null,new PropertyValue.NullValue(),new PropertyValue.StringValue("noop"),
                new PropertyValue.StringValue("(_) => Text('bad')"),new PropertyValue.BooleanValue(true),
                new PropertyValue.CallbackValue("buildSliver"))) {
            var doc=FlexibleSpaceBarContractTest.doc(node(bad));
            assertFalse(new WidgetTreeValidator().validate(doc,catalog).valid(),String.valueOf(bad));
        }
        var widget=node(new PropertyValue.StringValue("empty"));
        for(String slot:List.of("child","sliver","children")) {
            var bad=new WidgetNode(widget.id(),TYPE,widget.properties(),
                Map.of(new SlotName(slot),WidgetSlot.SingleSlot.of(SliverCrossAxisExpandedContractTest.adapter())));
            assertFalse(new WidgetTreeValidator().validate(FlexibleSpaceBarContractTest.doc(bad),catalog).valid());
        }
    }
}


