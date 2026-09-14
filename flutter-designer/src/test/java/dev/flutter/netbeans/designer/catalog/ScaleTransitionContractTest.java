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

public class ScaleTransitionContractTest {
    public static final WidgetTypeId TYPE=ScaleTransitionWidgetPropertySchema.TYPE;
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
        assertTrue(d.constConstructor());assertEquals("ScaleTransition",d.dartClassName());
        assertEquals(List.of("scale","alignment","filterQuality"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(ScaleTransitionWidgetPropertySchema.properties(),d.properties());
        assertEquals(Map.of(new PropertyName("scale"),number("1")),n.properties());
        var slot=d.slots().getFirst();assertEquals(CHILD,slot.name());assertEquals(3,slot.parameter().order());
        assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var plain=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
        for(var child:C.definitions()) assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        for(var owner:C.definitions())for(var target:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,target,plain),WidgetPlacementRules.accepts(owner,target,d));
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
    }
    @Test void signedScalePhysicalAlignmentQualityAndOptionalChildRoundTrip() throws Exception {
        for(String scale:List.of("-2.5","0","0.5","2"))
        for(String quality:List.of("omit","null","none","low","medium","high"))
        for(boolean child:List.of(false,true)){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName("scale"),number(scale));
            p.put(new PropertyName("alignment"),alignment("-2","3"));
            if (!quality.equals("omit")) p.put(new PropertyName("filterQuality"),quality.equals("null") ? new PropertyValue.NullValue() : new PropertyValue.EnumValue("FilterQuality",quality));
            n=new WidgetNode(n.id(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(adapter()):Optional.empty())));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var code=generated(n).build().payload();assertTrue(code.contains("const ScaleTransition("),code);
            assertTrue(code.contains("alignment: const Alignment(-2.0, 3.0)"),code);
            assertTrue(code.contains("scale: const AlwaysStoppedAnimation<double>("+scale+(scale.contains(".")?"":".0")+")"),code);
            assertTrue(generated(n).symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-scale-animation")));
            assertEquals(!child,code.contains("child: null"));
            if (!quality.equals("omit")) assertTrue(code.contains("filterQuality: "+(quality.equals("null")?"null":"FilterQuality."+quality)),code);
            else assertFalse(code.contains("filterQuality:"));
        }
        var n=node();n=new WidgetNode(n.id(),TYPE,n.properties(),Map.of());
        var code=generated(n).build().payload();assertFalse(code.contains("child:"));assertFalse(code.contains("alignment:"));
    }
    @Test void strictAnimationAndPhysicalAlignmentReferenceShapes() {
        for(var binding:Map.of("scale","Animation<double>","alignment","Alignment").entrySet())
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
        for(String field:List.of("scale")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
        }
        for(var bad:List.<PropertyValue>of(number("1e309"),number("-1e309"),new PropertyValue.StringValue("1"),
            new PropertyValue.IntegerValue(new BigInteger("9007199254740992")))){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("scale"),bad))),C).valid());
        }
        for(var bad:List.<PropertyValue>of(alignment("1e309","0"),
            new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,BigDecimal.ZERO,BigDecimal.ZERO),
            new PropertyValue.NullValue())){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("alignment"),bad))),C).valid());
        }
        for(String field:List.of("scale")){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("scale"),number("1e309")))),C).valid());
        for(String name:List.of("durationUs","curve","onEnd","transformHitTests","onTransform","animation"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(name),new PropertyValue.NullValue()))),C).valid());

    }
}
