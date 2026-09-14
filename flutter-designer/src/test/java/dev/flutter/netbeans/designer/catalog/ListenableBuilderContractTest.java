package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListenableBuilderContractTest {
    static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    static WidgetNode node(boolean sliver) {
        return WidgetNodePrototypeFactory.create(CATALOG.find(sliver ? ListenableBuilderWidgetPropertySchema.SLIVER_TYPE
                : ListenableBuilderWidgetPropertySchema.TYPE).orElseThrow(), StableId.random());
    }
    static DesignerDocument doc(WidgetNode node) {
        return node.type().equals(ListenableBuilderWidgetPropertySchema.SLIVER_TYPE)
                ? SliverCrossAxisExpandedContractTest.document(node) : FlexibleSpaceBarContractTest.doc(node);
    }
    @Test void exactConstructorBothProtocolsAndCallableContract() {
        for(boolean sliver:List.of(false,true)) {
            var node=node(sliver);var d=CATALOG.find(node.type()).orElseThrow();
            assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());
            assertEquals("ListenableBuilder",d.dartClassName());
            assertEquals(ListenableBuilderWidgetPropertySchema.properties(),d.properties());
            assertEquals(2,d.properties().size());assertEquals(1,d.slots().size());
            var slot=d.slots().getFirst();assertEquals("child",slot.name().value());
            assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
            assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
            assertEquals(!sliver,WidgetPlacementRules.evaluateRoot(d).accepted());
            assertEquals(new PropertyValue.StringValue("none"),node.properties().get(new PropertyName("listenable")));
            assertEquals(new PropertyValue.StringValue("child"),node.properties().get(new PropertyName("builder")));
            var cb=WidgetEventCatalog.find(node.type(),new PropertyName("builder")).orElseThrow();
            assertEquals("TransitionBuilder",cb.callbackType());assertEquals(WidgetEventDescriptor.Kind.BUILDER,cb.kind());
            assertEquals("Widget Function(BuildContext, Widget?)",cb.signature().dartFunctionType());
            assertTrue(cb.sdkRequired());assertFalse(cb.nullableCallback());assertFalse(cb.defaultEvent());
            assertTrue(WidgetEventCatalog.find(node.type(),new PropertyName("listenable")).isEmpty());
            for(var capability:List.of(WidgetCapability.CREATE,WidgetCapability.CANVAS))
                assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
            var box=CATALOG.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
            var raw=CATALOG.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow();
            assertEquals(!sliver,WidgetPlacementRules.accepts(d,slot,box));
            assertEquals(sliver,WidgetPlacementRules.accepts(d,slot,raw));
        }
    }
    @Test void presetsChildRoundTripAndExactSymbolEvidence() throws Exception {
        for(boolean sliver:List.of(false,true))for(boolean withChild:List.of(false,true)) {
            var node=node(sliver);
            if(withChild) {
                var child=sliver?SliverCrossAxisExpandedContractTest.adapter():
                        new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),Map.of(),Map.of());
                node=new WidgetNode(node.id(),node.type(),node.properties(),
                        Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
            }
            var document=doc(node);assertTrue(new WidgetTreeValidator().validate(document,CATALOG).valid());
            var result=new DartRegionGenerator().generate(document,CATALOG).generated().orElseThrow();
            var code=result.build().payload();
            assertTrue(code.contains("listenable: const AlwaysStoppedAnimation<double>(0.0)"),code);
            assertTrue(code.contains("builder: (context, child) => child ?? const "+(sliver?"SliverToBoxAdapter()":"SizedBox.shrink()")),code);
            assertFalse(code.contains("ListenableBuilder.sliver("));assertFalse(code.contains("const ListenableBuilder("));
            assertTrue(result.symbolOccurrences().stream().anyMatch(s->s.symbolName().equals("AlwaysStoppedAnimation")));
            var codec=new FdDocumentCodec();assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
        }
    }
    @Test void strictProofForBothPropertiesAndAllReferenceShapes() {
        for(boolean sliver:List.of(false,true))for(boolean factory:List.of(false,true))
            for(boolean member:List.of(false,true))for(boolean imported:List.of(false,true)) {
                var node=node(sliver);
                var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                        member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                node=new WidgetNode(node.id(),node.type(),Map.of(new PropertyName("listenable"),value,new PropertyName("builder"),value),node.slots());
                var result=new DartRegionGenerator().generate(doc(node),CATALOG).generated().orElseThrow();
                var types=result.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream()).map(p->p.expectedDartType()).toList();
                assertTrue(types.containsAll(List.of("Listenable","TransitionBuilder")),types.toString());
                assertFalse(result.build().payload().contains("AlwaysStoppedAnimation"));
                assertFalse(result.build().payload().contains("(context, child) =>"));
            }
    }
    @Test void rejectMissingNullRawWrongPresetAndUnknownSlot() {
        for(boolean sliver:List.of(false,true))for(String property:List.of("listenable","builder")) {
            for(PropertyValue bad:Arrays.asList(null,new PropertyValue.NullValue(),new PropertyValue.BooleanValue(true),
                    new PropertyValue.StringValue("noop"),new PropertyValue.StringValue("(_) => arbitrary()"),new PropertyValue.CallbackValue("handler"))) {
                var node=node(sliver);var props=new HashMap<>(node.properties());
                if(bad==null)props.remove(new PropertyName(property));else props.put(new PropertyName(property),bad);
                var invalid=new WidgetNode(node.id(),node.type(),props,node.slots());
                assertFalse(new WidgetTreeValidator().validate(doc(invalid),CATALOG).valid(),String.valueOf(bad));
            }
            var node=node(sliver);
            var invalid=new WidgetNode(node.id(),node.type(),node.properties(),Map.of(new SlotName("children"),new WidgetSlot.ListSlot(List.of())));
            assertFalse(new WidgetTreeValidator().validate(doc(invalid),CATALOG).valid());
        }
    }
}
