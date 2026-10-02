package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TweenAnimationBuilderContractTest {
    static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    static WidgetNode node(boolean sliver) {
        return WidgetNodePrototypeFactory.create(CATALOG.find(sliver ? TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE
                : TweenAnimationBuilderWidgetPropertySchema.TYPE).orElseThrow(), StableId.random());
    }
    static DesignerDocument doc(WidgetNode node) {
        return node.type().equals(TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE)
                ? SliverCrossAxisExpandedContractTest.document(node) : FlexibleSpaceBarContractTest.doc(node);
    }
    @Test void exactConstructorBothProtocolsAndCallableContract() {
        for(boolean sliver:List.of(false,true)) {
            var node=node(sliver);var d=CATALOG.find(node.type()).orElseThrow();
            assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());
            assertEquals("TweenAnimationBuilder",d.dartClassName());
            assertEquals(TweenAnimationBuilderWidgetPropertySchema.properties(),d.properties());
            assertEquals(7,d.properties().size());assertEquals(1,d.slots().size());
            var slot=d.slots().getFirst();assertEquals("child",slot.name().value());
            assertEquals(0,slot.minChildren());assertEquals(1,slot.maxChildren());assertFalse(slot.parameter().required());
            assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
            assertEquals(!sliver,WidgetPlacementRules.evaluateRoot(d).accepted());
            assertEquals(new PropertyValue.StringValue("default"),node.properties().get(new PropertyName("tween")));
            assertEquals(new PropertyValue.StringValue("child"),node.properties().get(new PropertyName("builder")));
            var cb=WidgetEventCatalog.find(node.type(),new PropertyName("builder")).orElseThrow();
            assertEquals("ValueWidgetBuilder<Object>",cb.callbackType());assertEquals(WidgetEventDescriptor.Kind.BUILDER,cb.kind());
            assertEquals("Widget Function(BuildContext, Object, Widget?)",cb.signature().dartFunctionType());
            assertTrue(cb.sdkRequired());assertFalse(cb.nullableCallback());assertFalse(cb.defaultEvent());
            assertTrue(WidgetEventCatalog.find(node.type(),new PropertyName("tween")).isEmpty());
            var end=WidgetEventCatalog.find(node.type(),new PropertyName("onEnd")).orElseThrow();
            assertTrue(end.nullableCallback());assertFalse(end.sdkRequired());assertTrue(end.defaultEvent());
            assertEquals(WidgetEventDescriptor.Kind.EVENT,end.kind());
            for(var capability:List.of(WidgetCapability.CREATE,WidgetCapability.CANVAS))
                assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
            var box=CATALOG.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow();
            var raw=CATALOG.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow();
            assertEquals(!sliver,WidgetPlacementRules.accepts(d,slot,box));
            assertEquals(sliver,WidgetPlacementRules.accepts(d,slot,raw));
        }
    }
    @Test void presetsChildRoundTripAndExactSymbolEvidence() throws Exception {
        for(boolean sliver:List.of(false,true))for(boolean withChild:List.of(false,true)) {
            var node=node(sliver);
            if(withChild) {
                var child=sliver?SliverCrossAxisExpandedContractTest.adapter():
                        new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SizedBox"),Map.of(),Map.of());
                node=new WidgetNode(node.id(),node.type(),node.properties(),
                        Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
            }
            var document=doc(node);assertTrue(new WidgetTreeValidator().validate(document,CATALOG).valid());
            var result=new DartRegionGenerator().generate(document,CATALOG).generated().orElseThrow();
            var code=result.build().payload();
            assertTrue(code.contains("tween: Tween<double>(begin: 0.0, end: 1.0)"),code);
            assertTrue(code.contains("builder: (context, value, child) => child ?? const "+(sliver?"SliverToBoxAdapter()":"SizedBox.shrink()")),code);
            assertFalse(code.contains("TweenAnimationBuilder.sliver("));assertFalse(code.contains("const TweenAnimationBuilder<double>("));
            assertTrue(result.symbolOccurrences().stream().anyMatch(s->s.symbolName().equals("Tween")));
            var codec=new FdDocumentCodec();assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
        }
    }
    @Test void allBuiltInAndNullableTypesRetainExactConstructorAndDefault() {
        for (boolean sliver : List.of(false, true)) for (boolean nullable : List.of(false, true))
            for (String type : RadioWidgetPropertySchema.valueTypes()) {
                var initial = node(sliver); var props = new HashMap<>(initial.properties());
                props.put(new PropertyName("valueType"), new PropertyValue.StringValue(type));
                props.put(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(nullable));
                var node = new WidgetNode(initial.id(), initial.type(), props, initial.slots());
                var document = doc(node);
                assertTrue(new WidgetTreeValidator().validate(document, CATALOG).valid());
                var result = new DartRegionGenerator().generate(document, CATALOG).generated().orElseThrow();
                String selected = type + (nullable ? "?" : "");
                assertTrue(result.build().payload().contains("TweenAnimationBuilder<" + selected + ">"));
                assertTrue(result.build().payload().contains(TweenAnimationBuilderWidgetPropertySchema.presetClass(node) + (type.equals("int") ? "(" : "<" + selected + ">(")), result.build().payload());
                assertFalse(result.build().payload().contains(">(null)"));
                assertFalse(result.imports().payload().contains("dart:core"), "Do not suppress implicit core scope");
                assertFalse(result.build().payload().contains("valueType:"));
                assertFalse(result.build().payload().contains("nullableValueType:"));
            }
    }
    @Test void customTypeAlwaysRequiresATweenWithNonNullEndAndNeverRawTypes() {
        for (boolean sliver : List.of(false, true)) {
            var initial=node(sliver); var props=new HashMap<>(initial.properties());
            var type = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/types.dart"), "Model",
                    Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            props.put(new PropertyName("valueType"), type);
            assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(initial.id(),initial.type(),props,initial.slots())),CATALOG).valid());
            props.put(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(true));
            var nullable = new WidgetNode(initial.id(),initial.type(),props,initial.slots());
            assertFalse(new WidgetTreeValidator().validate(doc(nullable),CATALOG).valid());
            for (String bad:List.of("dynamic","Null","Never","void","List<int>")) {
                var invalid=new HashMap<>(props);invalid.put(new PropertyName("valueType"),new PropertyValue.StringValue(bad));
                assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(initial.id(),initial.type(),invalid,initial.slots())),CATALOG).valid());
            }
        }
    }
    @Test void strictProofForBothPropertiesAndAllReferenceShapes() {
        for(boolean sliver:List.of(false,true))for(boolean factory:List.of(false,true))
            for(boolean member:List.of(false,true))for(boolean imported:List.of(false,true)) {
                var node=node(sliver);
                var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/bindings.dart"):Optional.empty(),
                        member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                node=new WidgetNode(node.id(),node.type(),Map.of(new PropertyName("tween"),value,new PropertyName("builder"),value,new PropertyName("valueType"),new PropertyValue.StringValue("double"),new PropertyName("durationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(300000))),node.slots());
                var result=new DartRegionGenerator().generate(doc(node),CATALOG).generated().orElseThrow();
                var types=result.symbolOccurrences().stream().flatMap(s->s.staticTypeRequirement().stream()).map(p->p.expectedDartType()).toList();
                assertTrue(types.containsAll(List.of("Tween<Object>","ValueWidgetBuilder<Object>")),types.toString());
                assertFalse(result.build().payload().contains("AlwaysStoppedAnimation"));
                assertFalse(result.build().payload().contains("(context, value, child) =>"));
            }
    }
    @Test void tweenIsTheConstructorArgumentNotAnimationOrAnEvent() {
        for(boolean sliver:List.of(false,true)) {
            var node=node(sliver);
            for(String invented:List.of("listenable","animation","duration","controller","reverseDuration","onChanged")) {
                var props=new HashMap<>(node.properties());
                props.put(new PropertyName(invented),new PropertyValue.StringValue("default"));
                var invalid=new WidgetNode(node.id(),node.type(),props,node.slots());
                assertFalse(new WidgetTreeValidator().validate(doc(invalid),CATALOG).valid(),invented);
            }
        }
    }
    @Test void durationCurveAndCompletionCoverAllNativeArguments() {
        for(boolean sliver:List.of(false,true)) for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()) {
            var node=node(sliver);var props=new HashMap<>(node.properties());
            props.put(new PropertyName("curve"),new PropertyValue.StringValue(curve));
            props.put(new PropertyName("durationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO));
            props.put(new PropertyName("onEnd"),new PropertyValue.NullValue());
            var result=new DartRegionGenerator().generate(doc(new WidgetNode(node.id(),node.type(),props,node.slots())),CATALOG).generated().orElseThrow();
            assertTrue(result.build().payload().contains("curve: Curves."+curve));
            assertTrue(result.build().payload().contains("duration: const Duration(microseconds: 0)"));
            assertTrue(result.build().payload().contains("onEnd: null"));
        }
        for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-1)),
                new PropertyValue.IntegerValue(new java.math.BigInteger("9007199254740992")),new PropertyValue.NullValue())) {
            var node=node(false);var props=new HashMap<>(node.properties());props.put(new PropertyName("durationUs"),bad);
            assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(node.id(),node.type(),props,node.slots())),CATALOG).valid());
        }
    }
    @Test void rejectMissingNullRawWrongPresetAndUnknownSlot() {
        for(boolean sliver:List.of(false,true))for(String property:List.of("tween","builder")) {
            for(PropertyValue bad:Arrays.asList(null,new PropertyValue.NullValue(),new PropertyValue.BooleanValue(true),
                    new PropertyValue.StringValue("noop"),new PropertyValue.StringValue("(_) => arbitrary()"),new PropertyValue.CallbackValue("handler"))) {
                var node=node(sliver);var props=new HashMap<>(node.properties());
                if(bad==null)props.remove(new PropertyName(property));else props.put(new PropertyName(property),bad);
                var invalid=new WidgetNode(node.id(),node.type(),props,node.slots());
                assertFalse(new WidgetTreeValidator().validate(doc(invalid),CATALOG).valid(),String.valueOf(bad));
            }
            var node=node(sliver);
            var invalid=new WidgetNode(node.id(),node.type(),node.properties(),Map.of(new SlotName("children"),new WidgetSlot.ListSlot(List.of())));
            assertFalse(new WidgetTreeValidator().validate(doc(invalid),CATALOG).valid());
        }
    }
}

