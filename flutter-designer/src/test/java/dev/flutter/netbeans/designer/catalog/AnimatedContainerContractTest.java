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

public class AnimatedContainerContractTest {
    public static final WidgetTypeId TYPE=AnimatedContainerWidgetPropertySchema.TYPE;
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

    @Test void completeConstructorDefaultsSlotEventsAndConstraints() {
        var d=C.find(TYPE).orElseThrow(); assertFalse(d.constConstructor());
        assertEquals(15,d.properties().size());assertEquals(1,d.slots().size());assertEquals(15,d.slots().getFirst().parameter().order());
        assertEquals(Map.of(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),node().properties());
        assertEquals("onEnd",WidgetEventCatalog.eventsFor(d).getFirst().propertyName().value());
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        var padding=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
        for(var child:C.definitions()) assertEquals(WidgetPlacementRules.accepts(padding,padding.slots().getFirst(),child),
            WidgetPlacementRules.accepts(d,d.slots().getFirst(),child),child.typeId().value());
        var code=generated(node()).build().payload();
        assertTrue(code.contains("AnimatedContainer("),code);assertFalse(code.contains("const AnimatedContainer("),code);
        assertTrue(code.contains("duration: const Duration(microseconds: 300000)"),code);
        assertFalse(code.contains("isAntiAlias"),code);
    }
    @Test void nullableLocalValuesAndExactReferenceTypesRoundTrip() throws Exception {
        var nulls=new LinkedHashMap<PropertyName,PropertyValue>();
        for(var property:C.find(TYPE).orElseThrow().properties()) if(property.acceptedKinds().contains(PropertyValueKind.NULL))
            nulls.put(property.name(),new PropertyValue.NullValue());
        assertEquals(12,nulls.size());
        var n=with(node(),nulls); var doc=document(n); var codec=new FdDocumentCodec();
        assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
        var code=generated(n).build().payload();
        for(var name:nulls.keySet()) assertTrue(code.contains(name.value()+": null"),code);
        for(var property:C.find(TYPE).orElseThrow().properties())
        for(var constraint:property.constraints()) if(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues reference)
        for(boolean member:List.of(false,true)) for(boolean factory:List.of(false,true)) {
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.of("package:sample/bindings.dart"),
                member?"Bindings":"binding",member?Optional.of("value"):Optional.empty(),
                factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var result=generated(with(node(),Map.of(property.name(),ref)));
            assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream())
                .anyMatch(t->t.expectedDartType().equals(reference.expectedDartType())),reference.expectedDartType());
        }
    }
    @Test void backgroundClippingAndInfinityAreNativeNotInventedRestrictions() {
        var color=new PropertyValue.ColorValue(0xff123456L);
        var decoration=new PropertyValue.BoxDecorationValue(Optional.empty(),Optional.empty(),Optional.empty(),List.of(),Optional.empty(),Optional.empty(),PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
        for(String clip:List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer")){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName("clipBehavior"),new PropertyValue.EnumValue("Clip",clip));
            assertEquals(clip.equals("none"),valid(p));
            p.put(new PropertyName("color"),color);assertTrue(valid(p));
            p.put(new PropertyName("decoration"),new PropertyValue.NullValue());assertTrue(valid(p));
            p.put(new PropertyName("decoration"),decoration);assertFalse(valid(p));
            p.put(new PropertyName("color"),new PropertyValue.NullValue());assertTrue(valid(p));
        }
        for(String name:List.of("width","height")){
            for(String n:List.of("0","1","1e100"))assertTrue(valid(Map.of(new PropertyName(name),number(n))));
            for(String n:List.of("-1","1e309"))assertFalse(valid(Map.of(new PropertyName(name),number(n))));
            assertTrue(valid(Map.of(new PropertyName(name),new PropertyValue.EnumValue("double","infinity"))));
            assertFalse(valid(Map.of(new PropertyName(name),new PropertyValue.EnumValue("double","negativeInfinity"))));
        }
        assertTrue(valid(Map.of(new PropertyName("width"),number("200"),
            new PropertyName("constraints"),new PropertyValue.BoxConstraintsValue(BigDecimal.TEN,Optional.of(new BigDecimal("100")),BigDecimal.ZERO,Optional.empty()))));
        for(var name:List.of("padding","margin"))assertFalse(valid(Map.of(new PropertyName(name),
            new PropertyValue.EdgeInsetsValue(BigDecimal.ONE.negate(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO))));
    }
    @Test void allCurvesAndRequiredDuration() {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()) {
            var code=generated(with(node(),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve)))).build().payload();
            assertTrue(code.contains("Curves."+curve),code);
        }
        for(String name:List.of("durationUs","curve","clipBehavior")) assertFalse(valid(Map.of(new PropertyName(name),new PropertyValue.NullValue())));
        var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
    }
    static boolean valid(Map<PropertyName,PropertyValue> p){return new WidgetTreeValidator().validate(document(with(node(),p)),C).valid();}
}

