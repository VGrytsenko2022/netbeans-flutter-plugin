package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class SlideTransitionContractTest {
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static final WidgetTypeId TYPE=SlideTransitionWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child"), CHILDREN=new SlotName("children");
    public static PropertyValue.OffsetValue offset(String x,String y){return new PropertyValue.OffsetValue(new BigDecimal(x),new BigDecimal(y));}
    public static WidgetNode adapter(){return AnimatedOpacityContractTest.adapter();}
    public static DesignerDocument document(WidgetNode node){return AnimatedOpacityContractTest.document(node);}
    static WidgetNode wrapper(PropertyValue value,boolean empty){
        return new WidgetNode(StableId.random(),TYPE,value==null?Map.of():Map.of(new PropertyName("position"),value),
            Map.of(CHILD,new WidgetSlot.SingleSlot(empty?Optional.empty():Optional.of(adapter()))));
    }
    @Test void exactConstructorAndBoxPlacementMatrix(){
        var d=C.find(TYPE).orElseThrow();assertEquals(3,d.properties().size());assertTrue(d.constConstructor());
        assertEquals(SlideTransitionWidgetPropertySchema.properties(),d.properties());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        assertEquals(3,d.slots().getFirst().parameter().order());assertEquals(0,d.slots().getFirst().minChildren());
        assertEquals(Set.of(PropertyValueKind.OFFSET,PropertyValueKind.DART_OBJECT_REFERENCE),d.properties().getFirst().acceptedKinds());
        var baseline=C.find(AnimatedSlideWidgetPropertySchema.TYPE).orElseThrow();
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(baseline,baseline.slots().getFirst(),child),WidgetPlacementRules.accepts(d,d.slots().getFirst(),child));
        for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,baseline),WidgetPlacementRules.accepts(owner,slot,d));
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertEquals(Map.of(new PropertyName("position"),offset("0","0")),WidgetNodePrototypeFactory.create(d,StableId.random()).properties());
    }
    @Test void signedFractionsNullDirectionCheckboxAndEmptyChildRoundTripWithExactSymbols() throws Exception {
        for(String x:List.of("-2.5","0",".25","1e308"))for(String y:List.of("-1","0",".5"))
        for(boolean empty:List.of(false,true))for(Boolean hit:Arrays.asList(null,false,true))for(String dir:List.of("unset","null","ltr","rtl")){
            var n=wrapper(offset(x,y),empty);var props=new LinkedHashMap<>(n.properties());
            if(hit!=null)props.put(new PropertyName("transformHitTests"),new PropertyValue.BooleanValue(hit));
            if(!dir.equals("unset"))props.put(new PropertyName("textDirection"),dir.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("TextDirection",dir));
            var doc=document(new WidgetNode(n.id(),TYPE,props,n.slots()));
            var codec=new FdDocumentCodec();assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            var generated=new DartRegionGenerator().generate(doc,C);assertTrue(generated.successful(),generated.diagnostics().toString());
            var result=generated.generated().orElseThrow();var code=result.build().payload();
            assertTrue(code.contains("const AlwaysStoppedAnimation<Offset>(const Offset("),code);
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-offset-animation")));
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-offset-type")));
            assertEquals(hit!=null,code.contains("transformHitTests:"));assertEquals(!dir.equals("unset"),code.contains("textDirection:"));
            assertEquals(empty,code.contains("child: null"));
            for(String absent:List.of("duration:","curve:","onEnd:"))assertFalse(code.contains(absent),code);
        }
    }
    @Test void exactAnimationProofAndRejectedDomains(){
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/slides.dart"):Optional.empty(),
                member?"Slides":"slide",member?Optional.of("value"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var result=new DartRegionGenerator().generate(document(wrapper(ref,false)),C).generated().orElseThrow();
            assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Animation<Offset>")));
            assertFalse(result.build().payload().contains("AlwaysStoppedAnimation"));
        }
        for(var bad:List.<PropertyValue>of(offset("1e309","0"),offset("0","-1e309"),new PropertyValue.NullValue(),new PropertyValue.StringValue("Offset.zero"),new PropertyValue.BooleanValue(true)))
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(bad,false)),C).valid(),bad.toString());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(null,false)),C).valid());
    }
}
