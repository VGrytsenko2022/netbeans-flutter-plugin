package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnimatedModalBarrierContractTest {
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    static final WidgetTypeId TYPE=AnimatedModalBarrierWidgetPropertySchema.TYPE;
    static WidgetNode node(Map<PropertyName,PropertyValue> p){
        var n=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());
        var values=new LinkedHashMap<>(n.properties());values.putAll(p);
        return new WidgetNode(n.id(),n.type(),values,Map.of());
    }
    static GeneratedDartRegions generate(WidgetNode n){var r=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void exactDefaultsAndLeafPlacement(){
        var d=C.find(TYPE).orElseThrow();assertTrue(d.constConstructor());assertEquals(7,d.properties().size());assertTrue(d.slots().isEmpty());
        assertEquals(List.of("color","dismissible","semanticsLabel","barrierSemanticsDismissible","onDismiss","clipDetailsNotifier","semanticsOnTapHint"),d.properties().stream().map(p->p.name().value()).toList());
        for(int i=0;i<7;i++){assertEquals(DartParameter.named(i,i==0),d.properties().get(i).parameter());assertEquals(i==0,d.properties().get(i).creationDefault().isPresent());}
        assertEquals(Map.of(new PropertyName("color"),new PropertyValue.NullValue()),node(Map.of()).properties());
        var defaults=generate(node(Map.of())).build().payload();assertTrue(defaults.contains("const AnimatedModalBarrier("));assertTrue(defaults.contains("AlwaysStoppedAnimation<Color?>(null)"));assertFalse(defaults.contains("barrierSemanticsDismissible:"));
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var peer=C.find(MatrixTransitionWidgetPropertySchema.TYPE).orElseThrow();
        for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
    }
    @Test void localDefaultNullFalseEmptyAndThemeStatesRoundTrip()throws Exception{
        for(String dismiss:List.of("unset","true","false"))for(String sem:List.of("unset","null","true","false"))
        for(String text:List.of("unset","null","","Close \"dialog\"\nДіалог"))for(String color:List.of("unset","null","clear","solid","theme")){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();
            if(!dismiss.equals("unset"))p.put(new PropertyName("dismissible"),new PropertyValue.BooleanValue(Boolean.parseBoolean(dismiss)));
            if(!sem.equals("unset"))p.put(new PropertyName("barrierSemanticsDismissible"),sem.equals("null")?new PropertyValue.NullValue():new PropertyValue.BooleanValue(Boolean.parseBoolean(sem)));
            if(!text.equals("unset"))for(String field:List.of("semanticsLabel","semanticsOnTapHint"))p.put(new PropertyName(field),text.equals("null")?new PropertyValue.NullValue():new PropertyValue.StringValue(text));
            if(!color.equals("unset"))p.put(new PropertyName("color"),switch(color){case "null"->new PropertyValue.NullValue();case "theme"->new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.scrim"));default->new PropertyValue.ColorValue(color.equals("clear")?0:0x88223344L);});
            var n=node(p);var doc=RotationTransitionContractTest.document(n);var codec=new FdDocumentCodec();
            assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
            var code=generate(n).build().payload();assertFalse(code.contains("child:"));assertFalse(code.contains("onDismiss:"));assertTrue(code.contains("AlwaysStoppedAnimation<"));assertTrue(code.contains("color:"));
            assertFalse(code.contains("color: null"));
            assertEquals(!sem.equals("unset"),code.contains("barrierSemanticsDismissible:"));assertEquals(!dismiss.equals("unset"),code.contains("dismissible:"));
        }
    }
    @Test void exactNullableTypedReferencesKeepAll24SourceForms(){
        for(var field:Map.of("color","Animation<Color?>","onDismiss","VoidCallback?","clipDetailsNotifier","ValueNotifier<EdgeInsets>?").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/values.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
            var generated=generate(node(Map.of(new PropertyName(field.getKey()),ref)));
            assertTrue(generated.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(field.getValue())));
        }
    }
    @Test void requiredAnimationCannotBeRemovedOrResetButStoppedNullIsValid(){
        var d=C.find(TYPE).orElseThrow();var seed=node(Map.of());
        var missing=new WidgetNode(seed.id(),TYPE,Map.of(),Map.of());
        assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(missing),C).successful());
        var color=d.property(new PropertyName("color")).orElseThrow();
        assertTrue(color.parameter().required());assertTrue(color.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
        assertFalse(color.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.DoubleValue(java.math.BigDecimal.ONE))));
        assertFalse(color.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.StringValue("ColorTween()"))));
    }
    @Test void dismissalIsAnOptionalNativeEventAndNotifierIsNot(){
        var events=WidgetEventCatalog.eventsFor(C.find(TYPE).orElseThrow());assertEquals(1,events.size());var e=events.getFirst();
        assertEquals("onDismiss",e.propertyName().value());assertEquals(WidgetEventDescriptor.Kind.EVENT,e.kind());assertTrue(e.defaultEvent());assertTrue(e.nullableCallback());assertTrue(e.allowsExplicitNull());
        assertFalse(e.required());assertFalse(e.sdkRequired());assertEquals("void Function()",e.signature().dartFunctionType());
        assertTrue(e.unsetBehavior().contains("Navigator.maybePop"));assertTrue(e.createStub("_dismiss").contains("void _dismiss()"));
        assertTrue(WidgetEventCatalog.find(TYPE,new PropertyName("clipDetailsNotifier")).isEmpty());
        for(String field:List.of("onDismiss","clipDetailsNotifier","color"))assertTrue(C.find(TYPE).orElseThrow().property(new PropertyName(field)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
        assertFalse(C.find(TYPE).orElseThrow().property(new PropertyName("dismissible")).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
    }
}
