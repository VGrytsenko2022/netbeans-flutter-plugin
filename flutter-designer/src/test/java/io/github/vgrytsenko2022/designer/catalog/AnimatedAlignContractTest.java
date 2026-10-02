package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnimatedAlignContractTest {
    public static final WidgetTypeId TYPE=AnimatedAlignWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child"), CHILDREN=new SlotName("children");
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new BigDecimal(n));}
    public static PropertyValue.AlignmentGeometryValue alignment(boolean directional,String x,String y){
        return new PropertyValue.AlignmentGeometryValue(directional?PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL:
            PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,new BigDecimal(x),new BigDecimal(y));
    }
    public static WidgetNode adapter(){return WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());}
    public static DesignerDocument document(WidgetNode child){return FlexibleSpaceBarContractTest.doc(new WidgetNode(StableId.random(),
        new WidgetTypeId("flutter.widgets.Column"),Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of(child)))));}
    static WidgetNode node(){return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void completeConstructorDefaultsSlotsEventsAndPlacement() {
        var d=C.find(TYPE).orElseThrow();var n=node();
        assertTrue(d.constConstructor());assertEquals("AnimatedAlign",d.dartClassName());
        assertEquals(List.of("alignment","widthFactor","heightFactor","curve","durationUs","onEnd"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(AnimatedAlignWidgetPropertySchema.properties(),d.properties());
        assertEquals(Map.of(new PropertyName("alignment"),alignment(false,"0","0"),new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),n.properties());
        var slot=d.slots().getFirst();assertEquals(CHILD,slot.name());assertEquals(6,slot.parameter().order());
        assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var plain=C.find(new WidgetTypeId("flutter.widgets.Align")).orElseThrow();
        for(var child:C.definitions()) assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        for(var owner:C.definitions())for(var target:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,target,plain),WidgetPlacementRules.accepts(owner,target,d));
        var event=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals("onEnd",event.propertyName().value());
        assertEquals("VoidCallback",event.callbackType());assertTrue(event.nullableCallback());assertFalse(event.sdkRequired());
        assertEquals(WidgetEventDescriptor.Kind.EVENT,event.kind());
    }
    @Test void physicalDirectionalOutsideUnitSquareAndNullableFactorsRoundTrip() throws Exception {
        for(boolean directional:List.of(false,true))for(String xy:List.of("-2.5","0","1"))
        for(PropertyValue factor:Arrays.<PropertyValue>asList(null,new PropertyValue.NullValue(),number("0"),number("0.5"),new PropertyValue.IntegerValue(BigInteger.TWO)))
        for(boolean child:List.of(false,true)){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName("alignment"),alignment(directional,xy,xy));
            if(factor!=null){p.put(new PropertyName("widthFactor"),factor);p.put(new PropertyName("heightFactor"),factor);}
            n=new WidgetNode(n.id(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(adapter()):Optional.empty())));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var code=generated(n).build().payload();assertTrue(code.contains("const AnimatedAlign("),code);
            assertTrue(code.contains(directional?"AlignmentDirectional(":"Alignment("),code);
            assertEquals(factor!=null,code.contains("widthFactor:"));assertEquals(factor!=null,code.contains("heightFactor:"));
            assertEquals(!child,code.contains("child: null"));assertFalse(code.contains("opacity:"));
        }
        var n=node();n=new WidgetNode(n.id(),TYPE,n.properties(),Map.of());
        assertFalse(generated(n).build().payload().contains("child:"));
    }
    @Test void allCurvePresetsDurationBoundariesAndStrictReferenceShapes() {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(long us:List.of(0L,1L,300000L,9007199254740991L)){
            var n=with(node(),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve),
                new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(us))));
            var code=generated(n).build().payload();assertTrue(code.contains("curve: Curves."+curve),code);
            assertTrue(code.contains("duration: const Duration(microseconds: "+us+")"),code);
        }
        for(var binding:Map.of("alignment","AlignmentGeometry","curve","Curve","durationUs","Duration","onEnd","VoidCallback").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),factory?
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var result=generated(with(node(),Map.of(new PropertyName(binding.getKey()),ref)));
            assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream())
                .anyMatch(t->t.expectedDartType().equals(binding.getValue())));
        }
    }
    @Test void invalidDomainsAndRequiredArgumentRemovalAreRejected() {
        for(String field:List.of("alignment","durationUs")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
        }
        for(String field:List.of("widthFactor","heightFactor"))for(var bad:List.<PropertyValue>of(number("-0.1"),number("1e309"),
            new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.StringValue("2"))){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),bad))),C).valid());
        }
        for(String field:List.of("alignment","durationUs","curve")){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("alignment"),alignment(false,"1e309","0")))),C).valid());
        for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),number("0.5")))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("durationUs"),bad))),C).valid());
        assertTrue(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("onEnd"),new PropertyValue.NullValue()))),C).valid());
    }
}
