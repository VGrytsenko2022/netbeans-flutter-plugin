package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomMultiChildLayoutContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetTypeId TYPE = CustomMultiChildLayoutWidgetPropertySchema.TYPE;
    public static WidgetNode layout(List<WidgetNode> children) {
        var n = WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());
        return new WidgetNode(n.id(),n.type(),n.properties(),Map.of(new SlotName("children"),new WidgetSlot.ListSlot(children)));
    }
    public static WidgetNode child() {return WidgetNodePrototypeFactory.create(C.find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue value) {
        var p = new LinkedHashMap<>(n.properties());p.put(new PropertyName(name),value);
        return new WidgetNode(n.id(),n.type(),p,n.slots());
    }
    static boolean valid(WidgetNode n) {return new WidgetTreeValidator().validate(RotationTransitionContractTest.document(n),C).valid();}
    static GeneratedDartRegions generate(WidgetNode n) {
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();
    }
    @Test void exactConstructorsSeedAndParentDataPlacement() {
        var m=C.find(TYPE).orElseThrow();var id=C.find(LayoutIdWidgetPropertySchema.TYPE).orElseThrow();
        assertTrue(m.constConstructor());assertFalse(id.constConstructor());
        assertEquals(List.of("delegate"),m.properties().stream().map(p->p.name().value()).toList());
        assertEquals(List.of("id"),id.properties().stream().map(p->p.name().value()).toList());
        assertEquals(DartParameter.named(1,false),m.slots().getFirst().parameter());
        assertEquals(DartParameter.named(1,true),id.slots().getFirst().parameter());
        assertTrue(WidgetEventCatalog.eventsFor(m).isEmpty());assertTrue(WidgetEventCatalog.eventsFor(id).isEmpty());
        var a=child();var b=child();
        assertNotEquals(a.properties().get(new PropertyName("id")),b.properties().get(new PropertyName("id")));
        assertEquals(a,WidgetNodePrototypeFactory.create(id,a.id()));
        assertFalse(a.slots().isEmpty());assertFalse(valid(a));assertTrue(valid(layout(List.of(a,b))));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(id).isEmpty());
        for(var d:C.definitions()) {
            assertEquals(d.typeId().equals(id.typeId()),WidgetPlacementRules.accepts(m,m.slots().getFirst(),d),d.typeId().value());
            for(var s:d.slots()) assertEquals(d.typeId().equals(TYPE)&&s.name().value().equals("children"),
                    WidgetPlacementRules.accepts(d,s,id),d.typeId()+"."+s.name());
        }
    }
    @Test void duplicateDartEqualityAndRequiredNonNullValues() {
        var one=new PropertyValue.IntegerValue(BigInteger.ONE);
        for(var two:List.<PropertyValue>of(one,new PropertyValue.DoubleValue(new BigDecimal("1.0")),
                new PropertyValue.DoubleValue(new BigDecimal("1.00000000000000001"))))
            assertFalse(valid(layout(List.of(with(child(),"id",one),with(child(),"id",two)))));
        for(var v:List.<PropertyValue>of(new PropertyValue.StringValue(""),new PropertyValue.BooleanValue(false)))
            assertFalse(valid(layout(List.of(with(child(),"id",v),with(child(),"id",v)))));
        assertTrue(valid(layout(List.of(with(child(),"id",one),with(child(),"id",new PropertyValue.StringValue("1")),
                with(child(),"id",new PropertyValue.BooleanValue(true))))));
        for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.CallbackValue("raw"),new PropertyValue.DoubleValue(new BigDecimal("1e400"))))
            assertFalse(valid(layout(List.of(with(child(),"id",bad)))));
        var n=child();assertFalse(valid(layout(List.of(new WidgetNode(n.id(),n.type(),Map.of(),n.slots())))));
        var l=layout(List.of());assertFalse(valid(new WidgetNode(l.id(),l.type(),Map.of(),l.slots())));
        assertFalse(valid(with(l,"delegate",new PropertyValue.NullValue())));
    }
    @Test void allSourceShapesAndLiteralKindsRoundTripAndFactoriesEvaluateOnce() throws Exception {
        var codec=new FdDocumentCodec();
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/values.dart"):Optional.empty(),
                    member?"Values":"sourceValue",member?Optional.of("value"):Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            var n=layout(List.of(with(child(),"id",ref)));
            var g=generate(n);String code=g.build().payload();
            assertTrue(code.contains(".children) (child as "));assertTrue(code.contains(").id"));
            assertEquals(1,code.split("id: ",-1).length-1);
            assertTrue(g.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Object")));
            var doc=RotationTransitionContractTest.document(n);
            assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            var custom=generate(with(n,"delegate",ref));
            assertFalse(custom.build().payload().contains("_fdMultiLayout_"));
            assertTrue(custom.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("MultiChildLayoutDelegate")));
        }
        for(var v:List.<PropertyValue>of(new PropertyValue.StringValue(""),new PropertyValue.IntegerValue(BigInteger.valueOf(-3)),
                new PropertyValue.DoubleValue(new BigDecimal("2.5")),new PropertyValue.BooleanValue(false))) {
            var n=layout(List.of(with(child(),"id",v)));assertTrue(valid(n));generate(n);
        }
        assertTrue(generate(layout(List.of())).build().payload().contains(").ids = ["));
    }
}
