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

public class AnimatedSizeContractTest {
    public static final WidgetTypeId TYPE=AnimatedSizeWidgetPropertySchema.TYPE;
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
    @Test void completeConstructorDefaultsSlotsEventsAndPlacement() {
        var d=C.find(TYPE).orElseThrow();var n=node();
        assertTrue(d.constConstructor());assertEquals("AnimatedSize",d.dartClassName());
        assertEquals(List.of("alignment","curve","durationUs","reverseDurationUs","clipBehavior","onEnd"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(AnimatedSizeWidgetPropertySchema.properties(),d.properties());
        assertEquals(Map.of(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),n.properties());
        var slot=d.slots().getFirst();assertEquals(CHILD,slot.name());assertEquals(6,slot.parameter().order());
        assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var plain=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
        for(var child:C.definitions()) assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        for(var owner:C.definitions())for(var target:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,target,plain),WidgetPlacementRules.accepts(owner,target,d));
        var event=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals("onEnd",event.propertyName().value());
        assertEquals("VoidCallback",event.callbackType());assertTrue(event.nullableCallback());assertFalse(event.sdkRequired());
        assertEquals(WidgetEventDescriptor.Kind.EVENT,event.kind());
    }

    @Test void allNullableReverseDurationsClipsAndAlignmentBasesRoundTrip() throws Exception {
        for(var basis:PropertyValue.AlignmentGeometryValue.HorizontalBasis.values())
        for(String clip:List.of("omit","none","hardEdge","antiAlias","antiAliasWithSaveLayer"))
        for(String reverse:List.of("omit","null","0","1","400000","9007199254740991"))
        for(boolean child:List.of(false,true)){
            var n=node();var p=new LinkedHashMap<>(n.properties());
            p.put(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(basis,new BigDecimal("-2"),new BigDecimal("3")));
            if(!clip.equals("omit"))p.put(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip",clip));
            if(!reverse.equals("omit"))p.put(new PropertyName("reverseDurationUs"),reverse.equals("null")?new PropertyValue.NullValue():new PropertyValue.IntegerValue(new BigInteger(reverse)));
            n=new WidgetNode(n.id(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(adapter()):Optional.empty())));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var result=generated(n);var code=result.build().payload();
            assertTrue(code.contains("const AnimatedSize("),code);
            assertTrue(code.contains("alignment: const "+(basis==PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL?"Alignment":"AlignmentDirectional")+"(-2.0, 3.0)"),code);
            assertEquals(!child,code.contains("child: null"));
            assertFalse(code.contains("reverseDurationUs:"));
            if(reverse.equals("omit"))assertFalse(code.contains("reverseDuration:"));
            else assertTrue(code.contains("reverseDuration: "+(reverse.equals("null")?"null":"const Duration(microseconds: "+reverse+")")),code);
            assertEquals(result.symbolOccurrences().size(),result.symbolOccurrences().stream().map(o->o.id()).distinct().count(),"Duration evidence IDs must be unique");
            if(!clip.equals("omit"))assertTrue(code.contains("clipBehavior: Clip."+clip),code);
        }
    }
    @Test void allCurvePresetsDurationBoundariesAndStrictReferenceShapes() {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(long us:List.of(0L,1L,300000L,9007199254740991L)){
            var n=with(node(),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve),
                new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(us))));
            var code=generated(n).build().payload();assertTrue(code.contains("curve: Curves."+curve),code);
            assertTrue(code.contains("duration: const Duration(microseconds: "+us+")"),code);
        }
        for(var binding:Map.of("alignment","AlignmentGeometry","curve","Curve","durationUs","Duration","reverseDurationUs","Duration?","onEnd","VoidCallback").entrySet())
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

    @Test void invalidDomainsRequiredRemovalAndUnknownConstructorArgumentsAreRejected() {
        var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
        for(String field:List.of("durationUs","reverseDurationUs"))
        for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),number("0.5"),new PropertyValue.StringValue("300000")))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),bad))),C).valid());
        for(String field:List.of("alignment","durationUs","curve","clipBehavior"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        for(String field:List.of("width","height","vsync"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),number("1")))),C).valid());
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("alignment"),alignment("1e309","0")))),C).valid());
    }
}
