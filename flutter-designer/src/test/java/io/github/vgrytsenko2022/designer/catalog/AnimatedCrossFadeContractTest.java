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

public class AnimatedCrossFadeContractTest {
    public static final WidgetTypeId TYPE=AnimatedCrossFadeWidgetPropertySchema.TYPE;
    public static final SlotName FIRST=new SlotName("firstChild"), SECOND=new SlotName("secondChild");
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static WidgetNode node(){return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());}
    public static DesignerDocument document(WidgetNode n){return FlexibleSpaceBarContractTest.doc(n);}
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void fullConstructorSeededSlotsStableIdsAndCallableMetadata() {
        var d=C.find(TYPE).orElseThrow();var n=node();
        assertTrue(d.constConstructor());assertEquals("AnimatedCrossFade",d.dartClassName());
        assertEquals(10,d.properties().size());assertEquals(AnimatedCrossFadeWidgetPropertySchema.FIELDS.stream().map(f->f.name()).toList(),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(2,n.properties().size());assertEquals(new PropertyValue.EnumValue("CrossFadeState","showFirst"),n.properties().get(new PropertyName("crossFadeState")));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        var ids=new HashSet<StableId>();ids.add(n.id());
        for(var slot:d.slots()){
            assertTrue(slot.parameter().required());assertEquals(1,slot.minChildren());assertEquals(1,slot.maxChildren());
            var seed=((WidgetSlot.SingleSlot)n.slots().get(slot.name())).child().orElseThrow();assertTrue(ids.add(seed.id()));
            assertEquals(seed,AnimatedCrossFadeWidgetPropertySchema.starterChild(n.id(),slot.name().value()));
            assertEquals("flutter.widgets.SizedBox",seed.type().value());
        }
        var counter=new java.util.concurrent.atomic.AtomicInteger();
        assertEquals(n,WidgetNodePrototypeFactory.create(d,()->{counter.incrementAndGet();return n.id();}));assertEquals(1,counter.get());
        var events=WidgetEventCatalog.eventsFor(d);assertEquals(2,events.size());assertTrue(events.stream().anyMatch(e->e.propertyName().value().equals("onEnd")));
        var builder=WidgetEventCatalog.eventsFor(d).stream().filter(e->e.propertyName().value().equals("layoutBuilder")).findFirst().orElseThrow();
        assertEquals("AnimatedCrossFadeBuilder",builder.callbackType());assertEquals(WidgetEventDescriptor.Kind.BUILDER,builder.kind());
        assertEquals("Widget Function(Widget, Key, Widget, Key)",builder.signature().dartFunctionType());
        assertFalse(builder.allowsExplicitNull());assertFalse(builder.sdkRequired());
        assertTrue(builder.createStub("_layout").contains("AnimatedCrossFade.defaultLayoutBuilder(topChild, topChildKey, bottomChild, bottomChildKey)"));
    }
    @Test void allCurvesStatesDirectionsDurationsAndRoundTrip() throws Exception {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(String state:List.of("showFirst","showSecond")){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();
            for(String name:List.of("firstCurve","secondCurve","sizeCurve"))p.put(new PropertyName(name),new PropertyValue.StringValue(curve));
            p.put(new PropertyName("crossFadeState"),new PropertyValue.EnumValue("CrossFadeState",state));
            p.put(new PropertyName("excludeBottomFocus"),new PropertyValue.BooleanValue(false));
            p.put(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.ZERO));
            p.put(new PropertyName("reverseDurationUs"),new PropertyValue.IntegerValue(new BigInteger("9007199254740991")));
            p.put(new PropertyName("alignment"),AnimatedFractionallySizedBoxContractTest.alignment(state.equals("showFirst"),"-2.5","1"));
            var n=with(node(),p);var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var result=generated(n);var code=result.build().payload();
            for(String name:List.of("firstCurve","secondCurve","sizeCurve"))assertTrue(code.contains(name+": Curves."+curve),code);
            for(String expected:List.of("const AnimatedCrossFade(", "firstChild:", "secondChild:", "crossFadeState: CrossFadeState."+state,
                "duration: const Duration(microseconds: 0)","reverseDuration: const Duration(microseconds: 9007199254740991)","excludeBottomFocus: false"))assertTrue(code.contains(expected),code);
            assertFalse(code.contains("clipBehavior:"));assertFalse(code.contains("layoutBuilder:"));
            assertEquals(2,result.symbolOccurrences().stream().filter(o->o.id().contains("animated-cross-fade")&&o.id().endsWith("duration")).count());
        }
    }
    @Test void strictReferenceShapesAndNullability() {
        for(var binding:Map.of("alignment","AlignmentGeometry","firstCurve","Curve","secondCurve","Curve","sizeCurve","Curve","durationUs","Duration","reverseDurationUs","Duration?","layoutBuilder","AnimatedCrossFadeBuilder","onEnd","VoidCallback").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),factory?
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var result=generated(with(node(),Map.of(new PropertyName(binding.getKey()),ref)));
            assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(binding.getValue())));
        }
        for(String field:List.of("alignment","firstCurve","secondCurve","sizeCurve","durationUs","crossFadeState","layoutBuilder","excludeBottomFocus"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid(),field);
        for(String field:List.of("reverseDurationUs","onEnd"))assertTrue(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
        for(String field:List.of("durationUs","reverseDurationUs"))for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.DoubleValue(new BigDecimal(".5"))))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),bad))),C).valid());
        for(String field:List.of("durationUs","crossFadeState")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
        }
        for(var slot:List.of(FIRST,SECOND)){
            var n=node();var slots=new LinkedHashMap<>(n.slots());slots.remove(slot);
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,n.properties(),slots)),C).valid());
            slots.put(slot,WidgetSlot.SingleSlot.empty());assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,n.properties(),slots)),C).valid());
        }
    }
}
