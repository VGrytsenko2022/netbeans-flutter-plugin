package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedIconContractTest{
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static WidgetNode node(Map<PropertyName,PropertyValue> p){var n=WidgetNodePrototypeFactory.create(C.find(AnimatedIconWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());var values=new LinkedHashMap<>(n.properties());values.putAll(p);return new WidgetNode(n.id(),n.type(),values,Map.of());}
 static GeneratedDartRegions code(WidgetNode n){var r=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 @Test void exactConstructorDefaultsAndLeafWithoutFabricatedEvents(){
  var d=C.find(AnimatedIconWidgetPropertySchema.TYPE).orElseThrow();
  assertEquals(List.of("icon","progress","color","size","semanticLabel","textDirection"),d.properties().stream().map(p->p.name().value()).toList());
  for(int i=0;i<6;i++){assertEquals(DartParameter.named(i,i<2),d.properties().get(i).parameter());assertEquals(i<2,d.properties().get(i).creationDefault().isPresent());}
  assertTrue(d.constConstructor());assertTrue(d.slots().isEmpty());assertEquals(14,AnimatedIconWidgetPropertySchema.ICONS.size());
  var s=code(node(Map.of())).build().payload();assertTrue(s.contains("AnimatedIcons.menu_close"));assertTrue(s.contains("AlwaysStoppedAnimation<double>(0.0)"));assertFalse(s.contains("onPressed"));
  assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
  var peer=C.find(AnimatedModalBarrierWidgetPropertySchema.TYPE).orElseThrow();
  for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
 }
 @Test void allIconsSignedProgressAndNullablePropertiesRoundTrip()throws Exception{
  var codec=new FdDocumentCodec();
  for(String icon:AnimatedIconWidgetPropertySchema.ICONS)for(String progress:List.of("-2","0","0.5","1","2"))for(String size:List.of("unset","null","0","-12","48")){
   var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName("icon"),new PropertyValue.StringValue(icon));p.put(new PropertyName("progress"),new PropertyValue.DoubleValue(new BigDecimal(progress)));
   if(!size.equals("unset"))p.put(new PropertyName("size"),size.equals("null")?new PropertyValue.NullValue():new PropertyValue.DoubleValue(new BigDecimal(size)));
   p.put(new PropertyName("semanticLabel"),new PropertyValue.StringValue("Open Діалог"));
   var n=node(p);var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
   var generated=code(n);assertTrue(generated.build().payload().contains("AnimatedIcons."+icon));assertEquals(!size.equals("unset"),generated.build().payload().contains("size:"));
   assertTrue(generated.symbolOccurrences().stream().anyMatch(o->o.symbolName().equals(icon)));
  }
 }
 @Test void all32TypedReferenceFormsRetainExpectedTypes(){
  for(var field:Map.of("icon","AnimatedIconData","progress","Animation<double>","color","Color?","size","double?").entrySet())
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/refs.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
   var g=code(node(Map.of(new PropertyName(field.getKey()),ref)));assertTrue(g.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(field.getValue())));
  }
 }
 @Test void missingRequiredAndArbitraryPresetsAreRejected(){
  var n=node(Map.of());for(String field:List.of("icon","progress")){var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(field));assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(new WidgetNode(n.id(),n.type(),p,Map.of())),C).successful());}
  var d=C.find(n.type()).orElseThrow();assertFalse(d.property(new PropertyName("icon")).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.StringValue("custom()"))));
  for(String name:List.of("icon","progress"))assertFalse(d.property(new PropertyName(name)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
 }
}
