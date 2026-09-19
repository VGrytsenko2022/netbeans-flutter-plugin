package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FadeTransitionContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    public static final WidgetTypeId TYPE = FadeTransitionWidgetPropertySchema.TYPE;
    public static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");
    public static PropertyValue.DoubleValue number(String s) { return AnimatedOpacityContractTest.number(s); }
    public static WidgetNode adapter() { return AnimatedOpacityContractTest.adapter(); }
    public static DesignerDocument document(WidgetNode node) { return AnimatedOpacityContractTest.document(node); }
    public static WidgetNode wrapper(WidgetTypeId type, PropertyValue value, boolean empty) {
        var sliver = type.equals(FadeTransitionWidgetPropertySchema.SLIVER_TYPE);
        var child = sliver ? WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), StableId.random()) : adapter();
        return new WidgetNode(StableId.random(),type,value==null?Map.of():Map.of(new PropertyName("opacity"),value),
            Map.of(new SlotName(sliver?"sliver":"child"),new WidgetSlot.SingleSlot(empty?Optional.empty():Optional.of(child))));
    }
    public static DesignerDocument doc(WidgetNode node) {
        return node.type().equals(TYPE) ? document(node) : FlexibleSpaceBarContractTest.doc(new WidgetNode(
            StableId.random(),new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
            Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(List.of(node)))));
    }
    @Test void exactConstructorsAndPlacementParity() {
        for(var type:List.of(TYPE,FadeTransitionWidgetPropertySchema.SLIVER_TYPE)) {
            var d=C.find(type).orElseThrow();var sliver=type.equals(FadeTransitionWidgetPropertySchema.SLIVER_TYPE);
            assertEquals(2,d.properties().size());assertTrue(d.constConstructor());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
            assertEquals(FadeTransitionWidgetPropertySchema.properties(),d.properties());
            assertEquals(2,d.slots().getFirst().parameter().order());assertEquals(0,d.slots().getFirst().minChildren());
            assertEquals(Set.of(PropertyValueKind.INTEGER,PropertyValueKind.DOUBLE,PropertyValueKind.DART_OBJECT_REFERENCE),d.properties().getFirst().acceptedKinds());
            var baseline=C.find(new WidgetTypeId(sliver?"flutter.widgets.SliverOpacity":"flutter.widgets.Opacity")).orElseThrow();
            for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(baseline,baseline.slots().getFirst(),child),
                WidgetPlacementRules.accepts(d,d.slots().getFirst(),child),child.typeId().value());
            for(var owner:C.definitions())for(var slot:owner.slots())
                assertEquals(WidgetPlacementRules.accepts(owner,slot,baseline),WidgetPlacementRules.accepts(owner,slot,d));
            assertEquals(!sliver,WidgetPlacementRules.evaluateRoot(d).accepted());
            assertEquals(Map.of(new PropertyName("opacity"),number("1")),WidgetNodePrototypeFactory.create(d,StableId.random()).properties());
        }
    }
    @Test void localEndpointsFractionsSemanticsAndEmptyChildrenRoundTripWithSymbolEvidence() throws Exception {
        for(var type:List.of(TYPE,FadeTransitionWidgetPropertySchema.SLIVER_TYPE))
            for(var n:List.<PropertyValue>of(number("0"),number(".125"),number("1"),new PropertyValue.IntegerValue(java.math.BigInteger.ONE)))
                for(boolean empty:List.of(false,true))for(Boolean semantics:Arrays.asList(null,false,true)) {
            var node=wrapper(type,n,empty);var props=new LinkedHashMap<>(node.properties());
            if(semantics!=null)props.put(new PropertyName("alwaysIncludeSemantics"),new PropertyValue.BooleanValue(semantics));
            var doc=doc(new WidgetNode(node.id(),type,props,node.slots()));
            var codec=new FdDocumentCodec();assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            var generated=new DartRegionGenerator().generate(doc,C);assertTrue(generated.successful(),generated.diagnostics().toString());
            var result=generated.generated().orElseThrow();var code=result.build().payload();
            assertTrue(code.contains("const AlwaysStoppedAnimation<double>("),code);
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-opacity-animation")));
            assertEquals(semantics!=null,code.contains("alwaysIncludeSemantics:"));
            if(type.equals(TYPE))assertEquals(empty,code.contains("child: null"));
            else assertTrue(code.contains("sliver: const SliverToBoxAdapter("),code);
            for(String absent:List.of("duration:","curve:","onEnd:"))assertFalse(code.contains(absent),code);
        }
    }
    @Test void typedAnimationReferencesRetainExactProofAndInvalidDomainsAreRejected() {
        for(var type:List.of(TYPE,FadeTransitionWidgetPropertySchema.SLIVER_TYPE)) {
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
                var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/fades.dart"):Optional.empty(),
                    member?"Fades":"fade",member?Optional.of("value"):Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
                var result=new DartRegionGenerator().generate(doc(wrapper(type,ref,false)),C).generated().orElseThrow();
                assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Animation<double>")));
                assertFalse(result.build().payload().contains("AlwaysStoppedAnimation"));
            }
            for(var bad:List.<PropertyValue>of(number("-0.01"),number("1.001"),number("1e309"),
                new PropertyValue.IntegerValue(java.math.BigInteger.TWO),new PropertyValue.NullValue(),new PropertyValue.StringValue(".5"),new PropertyValue.BooleanValue(true)))
                assertFalse(new WidgetTreeValidator().validate(doc(wrapper(type,bad,false)),C).valid(),bad.toString());
            assertFalse(new WidgetTreeValidator().validate(doc(wrapper(type,null,false)),C).valid());
        }
    }
}
