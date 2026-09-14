package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SizeTransitionContractTest {
    public static final WidgetTypeId TYPE=SizeTransitionWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child"), CHILDREN=new SlotName("children");
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new BigDecimal(n));}
    public static PropertyValue.AlignmentGeometryValue alignment(String x,String y){return new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new BigDecimal(x),new BigDecimal(y));}
    public static WidgetNode adapter(){return WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());}
    public static DesignerDocument document(WidgetNode child){return FlexibleSpaceBarContractTest.doc(new WidgetNode(StableId.random(),
        new WidgetTypeId("flutter.widgets.Column"),Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of(child)))));}
    static WidgetNode node(){return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}

    @Test void completeConstructorDefaultsSlotAndPlacement() {
        var d=C.find(TYPE).orElseThrow();var n=node();
        assertTrue(d.constConstructor());assertEquals("SizeTransition",d.dartClassName());
        assertEquals(List.of("axis","sizeFactor","axisAlignment","alignment","fixedCrossAxisSizeFactor"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(Map.of(new PropertyName("sizeFactor"),number("1")),n.properties());
        var slot=d.slots().getFirst();assertEquals(CHILD,slot.name());assertEquals(5,slot.parameter().order());
        assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
        var padding=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(padding,padding.slots().getFirst(),child),WidgetPlacementRules.accepts(d,slot,child));
        for(var owner:C.definitions())for(var target:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,target,padding),WidgetPlacementRules.accepts(owner,target,d));
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
    }
    @Test void allConstructorBranchesRoundTripAndGenerateExactValues() throws Exception {
        for(String axis:List.of("omit","horizontal","vertical"))
        for(String factor:List.of("-2","0","0.5","1","2"))
        for(String cross:List.of("omit","null","0","0.5","2"))
        for(String strategy:List.of("omit","null","physical","directional","legacy"))
        for(boolean child:List.of(false,true)){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName("sizeFactor"),number(factor));
            if(!axis.equals("omit"))p.put(new PropertyName("axis"),new PropertyValue.EnumValue("Axis",axis));
            if(!cross.equals("omit"))p.put(new PropertyName("fixedCrossAxisSizeFactor"),cross.equals("null")?new PropertyValue.NullValue():number(cross));
            if(strategy.equals("null")){p.put(new PropertyName("alignment"),new PropertyValue.NullValue());p.put(new PropertyName("axisAlignment"),new PropertyValue.NullValue());}
            if(strategy.equals("legacy"))p.put(new PropertyName("axisAlignment"),number("-2"));
            if(strategy.equals("physical")||strategy.equals("directional"))p.put(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(
                strategy.equals("physical")?PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL:PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                new BigDecimal("2"),new BigDecimal("-3")));
            var n=new WidgetNode(StableId.random(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(adapter()):Optional.empty())));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var code=generated(n).build().payload();assertTrue(code.contains("const SizeTransition("),code);
            assertTrue(code.contains("sizeFactor: const AlwaysStoppedAnimation<double>("+factor+(factor.contains(".")?"":".0")+")"),code);
            assertTrue(generated(n).symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-size-animation")));
            assertEquals(!axis.equals("omit"),code.contains("axis:"));
            assertEquals(!cross.equals("omit"),code.contains("fixedCrossAxisSizeFactor:"));
            if(strategy.equals("legacy"))assertTrue(code.contains("axisAlignment: -2.0"));
            if(strategy.equals("directional"))assertTrue(code.contains("AlignmentDirectional(2.0, -3.0)"));
            if(strategy.equals("physical"))assertTrue(code.contains("Alignment(2.0, -3.0)"));
            assertEquals(!child,code.contains("child: null"));
        }
    }
    @Test void allSourceShapesRetainExactNullableTypes() {
        for(var binding:Map.of("sizeFactor","Animation<double>","alignment","AlignmentGeometry?","axisAlignment","double?","fixedCrossAxisSizeFactor","double?").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),factory?
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var result=generated(with(node(),Map.of(new PropertyName(binding.getKey()),ref)));
            assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(binding.getValue())));
        }
    }
    @Test void domainsAndNullableAlignmentConflictAreFailClosed() {
        var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
        for(String field:List.of("sizeFactor","axisAlignment","fixedCrossAxisSizeFactor"))
        for(var bad:List.<PropertyValue>of(number("1e309"),number("-1e309"),new PropertyValue.StringValue("1"),
            new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.IntegerValue(new BigInteger("-9007199254740992"))))
            assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName(field),bad))),C).valid());
        for(String value:List.of("-1","-0.001"))assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("fixedCrossAxisSizeFactor"),number(value)))),C).valid());
        assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("sizeFactor"),new PropertyValue.NullValue()))),C).valid());
        var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"value",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        for(var legacy:List.<PropertyValue>of(number("0"),reference,new PropertyValue.NullValue()))
        for(var modern:List.<PropertyValue>of(alignment("0","0"),reference,new PropertyValue.NullValue())){
            var value=with(n,Map.of(new PropertyName("axisAlignment"),legacy,new PropertyName("alignment"),modern));
            assertEquals(legacy instanceof PropertyValue.NullValue || modern instanceof PropertyValue.NullValue,
                new WidgetTreeValidator().validate(document(value),C).valid());
        }
        for(String field:List.of("axis","sizeFactor"))assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        for(String field:List.of("durationUs","curve","onEnd","filterQuality","clipBehavior","transformHitTests","animation"))
            assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
    }
}
