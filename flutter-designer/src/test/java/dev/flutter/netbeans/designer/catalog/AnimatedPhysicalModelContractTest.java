package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AnimatedPhysicalModelContractTest {
 public static final WidgetTypeId TYPE=AnimatedPhysicalModelWidgetPropertySchema.TYPE;
 public static final SlotName CHILD=new SlotName("child");
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 public static WidgetNode node(){var n=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());return new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(PhysicalModelTestSupport.text())));}
 public static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var all=new LinkedHashMap<>(n.properties());all.putAll(p);return new WidgetNode(n.id(),TYPE,all,n.slots());}
 public static DesignerDocument document(WidgetNode n){return PhysicalModelTestSupport.document(n);}
 public static PropertyValue.DartObjectReferenceValue ref(String s){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),s,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 static GeneratedDartRegions generate(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 @Test void exactRequiredArgumentsDefaultsSlotAndEvent(){
   var d=C.find(TYPE).orElseThrow();assertEquals(11,d.properties().size());assertTrue(d.constConstructor());
   assertEquals(AnimatedPhysicalModelWidgetPropertySchema.FIELDS.stream().map(AnimatedPhysicalModelWidgetPropertySchema.Field::name).toList(),d.properties().stream().map(p->p.name().value()).toList());
   assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isPresent());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
   assertEquals(1,d.slots().getFirst().minChildren());assertEquals(11,d.slots().getFirst().parameter().order());
   var e=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals("onEnd",e.propertyName().value());assertEquals("VoidCallback",e.callbackType());assertTrue(e.nullableCallback());
   var code=generate(node()).build().payload();for(String v:List.of("const AnimatedPhysicalModel(","color: const Color(0xFF2196F3)","shadowColor: const Color(0xFF000000)","duration: const Duration(microseconds: 300000)"))assertTrue(code.contains(v),code);
   for(String v:List.of("shape:","clipBehavior:","elevation:","borderRadius:","animateColor:","animateShadowColor:","onEnd:"))assertFalse(code.contains(v),code);
   for(String name:List.of("color","shadowColor","durationUs")){
     var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(name));assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
   }
   var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
 }
 @Test void allShapesClipsFlagPairsPhysicalCornersThemesAndNullRoundTrip()throws Exception{
   for(String shape:List.of("rectangle","circle"))for(String clip:List.of("none","hardEdge","antiAlias","antiAliasWithSaveLayer"))
   for(boolean fill:List.of(true,false))for(boolean shadow:List.of(true,false))for(boolean theme:List.of(true,false)){
     var p=new LinkedHashMap<>(PhysicalModelTestSupport.fullProperties(shape,clip,theme));
     p.put(new PropertyName("animateColor"),new PropertyValue.BooleanValue(fill));p.put(new PropertyName("animateShadowColor"),new PropertyValue.BooleanValue(shadow));
     var n=with(node(),p);var doc=document(n);var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
     var code=generate(n).build().payload();
     for(String part:List.of("shape: BoxShape."+shape,"clipBehavior: Clip."+clip,"borderRadius: const BorderRadius.only(","elevation: 12.5","animateColor: "+fill,"animateShadowColor: "+shadow))assertTrue(code.contains(part),code);
     assertEquals(!theme,code.contains("return const AnimatedPhysicalModel("));
   }
   assertTrue(generate(with(node(),Map.of(new PropertyName("borderRadius"),new PropertyValue.NullValue()))).build().payload().contains("borderRadius: null"));
 }
 @Test void exactTypedReferencesAndAllCurves(){
   for(var e:Map.of("borderRadius","BorderRadius?","elevation","double","color","Color","shadowColor","Color","curve","Curve","durationUs","Duration","onEnd","VoidCallback").entrySet()){
      var result=generate(with(node(),Map.of(new PropertyName(e.getKey()),ref("_binding"))));
      assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(p->p.expectedDartType().equals(e.getValue())));
   }
   for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(long us:List.of(0L,1L,9007199254740991L)){
      var code=generate(with(node(),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve),new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(us))))).build().payload();
      assertTrue(code.contains("curve: Curves."+curve));assertTrue(code.contains("duration: const Duration(microseconds: "+us+")"));
   }
 }
 @Test void strictLocalGeometryNullAndDirectionalDomains(){
   for(String name:List.of("shape","clipBehavior","elevation","color","shadowColor","animateColor","animateShadowColor","curve","durationUs"))
     assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(name),new PropertyValue.NullValue()))),C).valid(),name);
   assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("borderRadius"),PhysicalModelTestSupport.radius(true)))),C).valid());
   for(String name:List.of("elevation","durationUs"))
     assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(name),new PropertyValue.IntegerValue(BigInteger.valueOf(-1))))),C).valid());
   assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("elevation"),new PropertyValue.DoubleValue(new BigDecimal("1e309"))))),C).valid());
 }
}

