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

public class AnimatedSwitcherContractTest {
    public static final WidgetTypeId TYPE=AnimatedSwitcherWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child");
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static WidgetNode node(){return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());}
    public static DesignerDocument document(WidgetNode n){return FlexibleSpaceBarContractTest.doc(n);}
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void fullConstructorOptionalChildBuildersAndNoInventedOnEnd() {
        var d=C.find(TYPE).orElseThrow();var n=node();
        assertTrue(d.constConstructor());assertEquals("AnimatedSwitcher",d.dartClassName());
        assertEquals(6,d.properties().size());assertEquals(AnimatedSwitcherWidgetPropertySchema.FIELDS.stream().map(f->f.name()).toList(),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(Map.of(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),n.properties());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertEquals(0,d.slots().getFirst().minChildren());assertFalse(d.slots().getFirst().parameter().required());assertEquals(6,d.slots().getFirst().parameter().order());
        assertTrue(((WidgetSlot.SingleSlot)n.slots().get(CHILD)).child().isEmpty());
        var events=WidgetEventCatalog.eventsFor(d);assertEquals(2,events.size());
        for(var builder:events){assertEquals(WidgetEventDescriptor.Kind.BUILDER,builder.kind());assertFalse(builder.allowsExplicitNull());assertFalse(builder.sdkRequired());
            assertTrue(builder.createStub("_builder").contains("return AnimatedSwitcher.default"));}
        assertEquals("Widget Function(Widget, Animation<double>)",events.getFirst().signature().dartFunctionType());
        assertEquals("Widget Function(Widget?, List<Widget>)",events.getLast().signature().dartFunctionType());
        assertTrue(WidgetEventCatalog.find(TYPE,new PropertyName("onEnd")).isEmpty());
    }
    @Test void allCurvesDurationsOptionalChildAndRoundTrip() throws Exception {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(boolean child:List.of(false,true)){
            var n=with(node(),Map.of(new PropertyName("switchInCurve"),new PropertyValue.StringValue(curve),
                new PropertyName("switchOutCurve"),new PropertyValue.StringValue(curve),
                new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.ZERO),
                new PropertyName("reverseDurationUs"),new PropertyValue.IntegerValue(new BigInteger("9007199254740991"))));
            n=new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,child?WidgetSlot.SingleSlot.of(AnimatedFractionallySizedBoxContractTest.adapter()):WidgetSlot.SingleSlot.empty()));
            var doc=document(n);assertTrue(new WidgetTreeValidator().validate(doc,C).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var result=generated(n);var code=result.build().payload();
            for(String name:List.of("switchInCurve","switchOutCurve"))assertTrue(code.contains(name+": Curves."+curve),code);
            for(String expected:List.of("const AnimatedSwitcher(","duration: const Duration(microseconds: 0)","reverseDuration: const Duration(microseconds: 9007199254740991)"))assertTrue(code.contains(expected),code);
            assertEquals(!child,code.contains("child: null"));assertFalse(code.contains("key:"));assertFalse(code.contains("onEnd:"));
            assertEquals(2,result.symbolOccurrences().stream().filter(o->o.id().contains("animated-switcher")&&o.id().endsWith("duration")).count());
        }
    }
    @Test void strictReferenceShapesAndClosedDomains() {
        for(var binding:Map.of("switchInCurve","Curve","switchOutCurve","Curve","durationUs","Duration","reverseDurationUs","Duration?","layoutBuilder","AnimatedSwitcherLayoutBuilder","transitionBuilder","AnimatedSwitcherTransitionBuilder").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),factory?
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            assertTrue(generated(with(node(),Map.of(new PropertyName(binding.getKey()),ref))).symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(binding.getValue())));
        }
        for(String field:List.of("switchInCurve","switchOutCurve","durationUs","layoutBuilder","transitionBuilder"))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid(),field);
        assertTrue(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("reverseDurationUs"),new PropertyValue.NullValue()))),C).valid());
        for(String field:List.of("durationUs","reverseDurationUs"))for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.DoubleValue(new BigDecimal(".5"))))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),bad))),C).valid());
        var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("onEnd"),new PropertyValue.NullValue()))),C).valid());
    }
}
