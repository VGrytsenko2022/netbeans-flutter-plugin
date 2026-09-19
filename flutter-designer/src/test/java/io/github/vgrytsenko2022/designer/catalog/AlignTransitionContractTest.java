package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AlignTransitionContractTest {
    static final WidgetTypeId TYPE=AlignTransitionWidgetPropertySchema.TYPE;
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    static final SlotName CHILD=new SlotName("child");
    static WidgetNode node(){
        var p=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());
        return new WidgetNode(p.id(),TYPE,p.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(AnimatedAlignContractTest.adapter())));
    }
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static DesignerDocument doc(WidgetNode n){return FlexibleSpaceBarContractTest.doc(n);}
    static GeneratedDartRegions generated(WidgetNode n){var result=new DartRegionGenerator().generate(doc(n),C);assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();}
    @Test void exactConstructorOrderAndAllDestinations(){
        var d=C.find(TYPE).orElseThrow();assertTrue(d.constConstructor());
        assertEquals(List.of("alignment","widthFactor","heightFactor"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(List.of(0,2,3),d.properties().stream().map(p->p.parameter().order()).toList());
        assertEquals(DartParameter.named(1,true),d.slots().getFirst().parameter());
        assertEquals(1,d.slots().getFirst().minChildren());assertEquals(1,d.slots().getFirst().maxChildren());
        assertEquals(CHILD,WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow().name());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        var plain=C.find(new WidgetTypeId("flutter.widgets.Align")).orElseThrow();
        for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,plain),WidgetPlacementRules.accepts(owner,slot,d));
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,d.slots().getFirst(),child));
    }
    @Test void localAlignmentAndNullableFactorMatrixRoundTrips()throws Exception{
        for(boolean directional:List.of(false,true))for(String xy:List.of("-2","-1","0",".5","1","2","1e100"))
        for(String factor:List.of("omitted","null","0",".5","1","3","1e100")){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName("alignment"),AnimatedAlignContractTest.alignment(directional,xy,xy));
            if(!factor.equals("omitted"))for(String f:List.of("widthFactor","heightFactor"))p.put(new PropertyName(f),factor.equals("null")?new PropertyValue.NullValue():AnimatedAlignContractTest.number(factor));
            var n=with(node(),p);var document=doc(n);var codec=new FdDocumentCodec();
            assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
            var out=generated(n);var code=out.build().payload();
            assertTrue(code.contains("const AlignTransition("),code);assertTrue(code.contains("const AlwaysStoppedAnimation<AlignmentGeometry>("),code);
            assertTrue(code.contains(directional?"AlignmentDirectional(":"Alignment("),code);
            assertEquals(!factor.equals("omitted"),code.contains("widthFactor:"));
            if(!factor.equals("omitted"))assertTrue(code.indexOf("child:")<code.indexOf("widthFactor:"),code);
            assertTrue(out.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-alignment-animation")));
        }
    }
    @Test void all24SourceFormsKeepExactStaticTypeEvidence(){
        for(var field:Map.of("alignment","Animation<AlignmentGeometry>","widthFactor","double?","heightFactor","double?").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/values.dart"):Optional.empty(),member?"Values":"value",
                member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var out=generated(with(node(),Map.of(new PropertyName(field.getKey()),ref)));
            assertTrue(out.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(field.getValue())));
            assertFalse(out.build().payload().contains("const AlignTransition("));
            if(field.getKey().equals("alignment"))assertFalse(out.build().payload().contains("AlwaysStoppedAnimation"));
        }
    }
    @Test void missingRequiredAndInvalidDomainsFailClosed(){
        var n=node();assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
        assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
        for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("Alignment.center"),AnimatedAlignContractTest.alignment(false,"1e309","0")))
          assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName("alignment"),bad))),C).valid());
        for(String field:List.of("widthFactor","heightFactor"))for(var bad:List.<PropertyValue>of(AnimatedAlignContractTest.number("-1"),AnimatedAlignContractTest.number("1e309"),new PropertyValue.StringValue("1")))
          assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(field),bad))),C).valid());
        for(String field:List.of("durationUs","curve","onEnd","clipBehavior","textDirection"))
          assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(field),new PropertyValue.BooleanValue(true)))),C).valid());
    }
}
