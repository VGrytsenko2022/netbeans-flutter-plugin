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
public class MatrixTransitionContractTest {
    public static final WidgetTypeId TYPE=MatrixTransitionWidgetPropertySchema.TYPE;
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    static final SlotName CHILD=new SlotName("child");
    public static WidgetNode node(){return WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static DesignerDocument doc(WidgetNode n){return RotationTransitionContractTest.document(n);}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(doc(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    public static PropertyValue.Matrix4Value matrix(String name){
        var values=new ArrayList<>(MatrixTransitionWidgetPropertySchema.identity().storage());
        switch(name){
            case "translation" -> {values.set(12,new BigDecimal("20"));values.set(13,new BigDecimal("-12"));}
            case "scale" -> {values.set(0,new BigDecimal("-2"));values.set(5,new BigDecimal(".5"));}
            case "perspective" -> {values.set(3,new BigDecimal(".001"));values.set(7,new BigDecimal("-.002"));values.set(11,new BigDecimal(".003"));}
            case "singular" -> values.set(0,BigDecimal.ZERO);
            case "zero" -> Collections.fill(values,BigDecimal.ZERO);
            case "overflow" -> {values.set(0,new BigDecimal("1e308"));values.set(12,new BigDecimal("-1e308"));}
        }
        return new PropertyValue.Matrix4Value(values);
    }
    @Test void exactNativeConstructorAndAllPlacements(){
        var d=C.find(TYPE).orElseThrow();assertTrue(d.constConstructor());assertEquals(4,d.properties().size());
        assertEquals(List.of("animation","onTransform","alignment","filterQuality"),d.properties().stream().map(p->p.name().value()).toList());
        assertEquals(List.of(0,1,2,3),d.properties().stream().map(p->p.parameter().order()).toList());
        assertEquals(DartParameter.named(4,false),d.slots().getFirst().parameter());
        assertEquals(0,d.slots().getFirst().minChildren());assertEquals(1,d.slots().getFirst().maxChildren());
        assertEquals(Set.of(new PropertyName("animation"),new PropertyName("onTransform")),node().properties().keySet());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        var plain=C.find(RotationTransitionWidgetPropertySchema.TYPE).orElseThrow();
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),child),WidgetPlacementRules.accepts(d,d.slots().getFirst(),child),child.typeId().value());
        for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,plain),WidgetPlacementRules.accepts(owner,slot,d));
    }
    @Test void transformIsRequiredNonNullComputationDelegateWithSafeEditableStub(){
        var d=C.find(TYPE).orElseThrow();var events=WidgetEventCatalog.eventsFor(d);assertEquals(1,events.size());var e=events.getFirst();
        assertEquals(WidgetEventDescriptor.Kind.DELEGATE,e.kind());assertTrue(e.supportsHandlerActions());assertFalse(e.defaultEvent());assertTrue(e.required());assertTrue(e.sdkRequired());
        assertFalse(e.allowsExplicitNull());assertFalse(e.nullableCallback());assertEquals("TransformCallback",e.callbackType());
        assertEquals("Matrix4 Function(double)",e.signature().dartFunctionType());
        assertEquals(List.of("package:flutter/widgets.dart"),e.signature().importUris());
        assertTrue(e.createStub("_transform").contains("return Matrix4.identity();"));assertFalse(e.createStub("_transform").contains("UnimplementedError"));
        assertEquals(Optional.of(MatrixTransitionWidgetPropertySchema.identity()),e.creationDefault());
        assertInstanceOf(PropertyValue.DartObjectReferenceValue.class,e.bindingValue("_transform",d.property(new PropertyName("onTransform")).orElseThrow()));
    }
    @Test void allLocalMatrixKindsNumericValuesQualityAndChildRoundTrip()throws Exception{
        for(String kind:List.of("identity","translation","scale","perspective","singular","zero","overflow"))
        for(String number:List.of("-2.5","0","1e308"))for(String quality:List.of("omitted","null","none","low","medium","high"))
        for(boolean child:List.of(false,true)){
            var n=node();var p=new LinkedHashMap<>(n.properties());
            p.put(new PropertyName("animation"),RotationTransitionContractTest.number(number));
            p.put(new PropertyName("onTransform"),matrix(kind));
            p.put(new PropertyName("alignment"),RotationTransitionContractTest.alignment("-2","3"));
            if(!quality.equals("omitted"))p.put(new PropertyName("filterQuality"),quality.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("FilterQuality",quality));
            n=new WidgetNode(n.id(),TYPE,p,Map.of(CHILD,new WidgetSlot.SingleSlot(child?Optional.of(RotationTransitionContractTest.adapter()):Optional.empty())));
            var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
            var result=generated(n);var code=result.build().payload();
            assertTrue(code.contains("animation: const AlwaysStoppedAnimation<double>("),code);
            assertTrue(code.contains("onTransform: (double animationValue) => Matrix4.fromList(<double>["),code);
            assertFalse(code.contains("const MatrixTransition("),code);
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":stopped-matrix-animation")));
            assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().contains(":matrix4:")&&o.libraryUri().contains("vector_math")));
            assertEquals(!child,code.contains("child: null"));assertEquals(!quality.equals("omitted"),code.contains("filterQuality:"));
        }
    }
    @Test void all24ReferenceShapesRetainStrictTypes(){
        for(var field:Map.of("animation","Animation<double>","onTransform","TransformCallback","alignment","Alignment").entrySet())
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/values.dart"):Optional.empty(),member?"Values":"value",member?Optional.of("value"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
            var out=generated(with(node(),Map.of(new PropertyName(field.getKey()),ref)));
            assertTrue(out.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(field.getValue())));
            if(field.getKey().equals("onTransform"))assertFalse(out.build().payload().contains("(double animationValue) =>"));
        }
    }
    @Test void invalidDomainsAndInventedArgumentsFailClosed(){
        for(String field:List.of("animation","onTransform")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));
            assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
            for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.CallbackValue("_untyped"),new PropertyValue.StringValue("raw Dart")))
                assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(field),bad))),C).valid());
        }
        var huge=new ArrayList<>(MatrixTransitionWidgetPropertySchema.identity().storage());huge.set(0,new BigDecimal("1e309"));
        assertFalse(new WidgetTreeValidator().validate(doc(with(node(),Map.of(new PropertyName("onTransform"),new PropertyValue.Matrix4Value(huge)))),C).valid());
        for(var bad:List.<PropertyValue>of(RotationTransitionContractTest.number("1e309"),new PropertyValue.IntegerValue(new BigInteger("9007199254740992"))))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),Map.of(new PropertyName("animation"),bad))),C).valid());
        for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,BigDecimal.ZERO,BigDecimal.ZERO)))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),Map.of(new PropertyName("alignment"),bad))),C).valid());
        for(String field:List.of("durationUs","curve","onEnd","turns","transformHitTests","origin","clipBehavior"))
            assertFalse(new WidgetTreeValidator().validate(doc(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
    }
}
