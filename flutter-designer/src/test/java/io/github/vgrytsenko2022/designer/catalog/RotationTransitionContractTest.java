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

public class RotationTransitionContractTest {
    public static final WidgetTypeId TYPE=RotationTransitionWidgetPropertySchema.TYPE;
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
        assertTrue(d.constConstructor());assertEquals("RotationTransition",d.dartClassName());
        assertEquals(List.of("turns","alignment","filterQuality"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(RotationTransitionWidgetPropertySchema.properties(),d.properties());
        assertEquals(Map.of(new PropertyName("turns"),number("0")),n.properties());
        var slot=d.slots().getFirst();assertEquals(CHILD,slot.name());assertEquals(3,slot.parameter().order());
        assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var plain=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
        for(var child:C.definitions()) assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,slot,child),child.typeId().value());
        for(var owner:C.definitions())for(var target:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,target,plain),WidgetPlacementRules.accepts(owner,target,d));
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
    }
    @Test void signedTurnsPhysicalAlignmentQualityAndOptionalChildRoundTrip() throws Exception {
        for(String turns:List.of("-2.5","-0.25","0","0.125","0.25","0.5","1","2"))
        for(String quality:List.of("omit","null","none","low","medium","high"))
        for(boolean child:List.of(false,true)){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName("turns"),number(turns));
            p.put(new PropertyName("alignment"),alignment("-2","3"));
            if (!quality.equals("omit")) p.put(new PropertyName("filterQuality"),quality.equals("null") ? new PropertyValue.NullValue() : new PropertyValue.EnumValue("FilterQuality",quality));
            n=new WidgetNode(n.id(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(adapter()):Optional.empty())));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var code=generated(n).build().payload();assertTrue(code.contains("const RotationTransition("),code);
            assertTrue(code.contains("alignment: const Alignment(-2.0, 3.0)"),code);
            assertTrue(code.contains("turns: const AlwaysStoppedAnimation<double>("+turns+(turns.contains(".")?"":".0")+")"),code);
            assertTrue(generated(n).symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-turns-animation")));
            assertEquals(!child,code.contains("child: null"));
            if (!quality.equals("omit")) assertTrue(code.contains("filterQuality: "+(quality.equals("null")?"null":"FilterQuality."+quality)),code);
            else assertFalse(code.contains("filterQuality:"));
        }
        var n=node();n=new WidgetNode(n.id(),TYPE,n.properties(),Map.of());
        var code=generated(n).build().payload();assertFalse(code.contains("child:"));assertFalse(code.contains("alignment:"));
    }
    @Test void integerAndExtremeFiniteTurnsRetainExactSourceAndRequiredEvidence() {
        for(String value:List.of("-9007199254740991","-1","0","1","9007199254740991")){
            var n=with(node(),Map.of(new PropertyName("turns"),new PropertyValue.IntegerValue(new BigInteger(value))));
            var result=generated(n);
            var literal=java.util.regex.Pattern.compile("turns: const AlwaysStoppedAnimation<double>\\(([^)]+)\\)").matcher(result.build().payload());
            assertTrue(literal.find(),result.build().payload());
            assertEquals(0,new BigDecimal(value).compareTo(new BigDecimal(literal.group(1))));
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-turns-animation")));
        }
        for(String value:List.of("-1e308","1e308")){
            var n=with(node(),Map.of(new PropertyName("turns"),number(value)));
            assertTrue(new WidgetTreeValidator().validate(document(n),C).valid());
            assertTrue(generated(n).build().payload().contains("AlwaysStoppedAnimation<double>"));
        }
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("turns"),
            new PropertyValue.IntegerValue(new BigInteger("-9007199254740992"))))),C).valid());
    }
    @Test void strictAnimationAndPhysicalAlignmentReferenceShapes() {
        for(var binding:Map.of("turns","Animation<double>","alignment","Alignment").entrySet())
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
        for(String field:List.of("turns")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
        }
        for(var bad:List.<PropertyValue>of(number("1e309"),number("-1e309"),new PropertyValue.StringValue("1"),
            new PropertyValue.IntegerValue(new BigInteger("9007199254740992")))){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("turns"),bad))),C).valid());
        }
        for(var bad:List.<PropertyValue>of(alignment("1e309","0"),
            new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,BigDecimal.ZERO,BigDecimal.ZERO),
            new PropertyValue.NullValue())){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("alignment"),bad))),C).valid());
        }
        for(String field:List.of("turns")){
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("turns"),number("1e309")))),C).valid());
        for(String name:List.of("durationUs","curve","onEnd","transformHitTests","onTransform","animation"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(name),new PropertyValue.NullValue()))),C).valid());

    }
}
