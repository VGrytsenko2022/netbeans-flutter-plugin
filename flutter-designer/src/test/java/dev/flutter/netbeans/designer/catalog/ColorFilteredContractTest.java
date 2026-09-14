package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorFilteredContractTest {
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static final SlotName CHILD=new SlotName("child");
 static WidgetNode node(Map<PropertyName,PropertyValue> values){
  var d=C.find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow();var prototype=WidgetNodePrototypeFactory.create(d,StableId.random());
  var p=new LinkedHashMap<>(prototype.properties());p.putAll(values);return new WidgetNode(prototype.id(),prototype.type(),p,prototype.slots());
 }
 static GeneratedDartRegions code(WidgetNode n){var r=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 static PropertyName p(String n){return new PropertyName(n);}
 static PropertyValue.StringValue s(String n){return new PropertyValue.StringValue(n);}
 static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 @Test void allFiveFactoriesIdentityCreationOptionalChildAndNoEvents(){
  var d=C.find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow();assertEquals(24,d.properties().size());assertTrue(d.constConstructor());assertEquals(1,d.slots().size());assertEquals(0,d.slots().getFirst().minChildren());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
  for(int i=0;i<24;i++){var property=d.properties().get(i);assertEquals(DartParameter.named(i,i==0),property.parameter());assertEquals(i==0,property.creationDefault().isPresent());}
  for(String preset:ColorFilteredWidgetPropertySchema.FILTERS){
   String code=code(node(Map.of(p("colorFilter"),s(preset)))).build().payload();assertTrue(code.contains("ColorFilter."+preset+"("),code);
   assertEquals(!preset.equals("saturation"),code.contains("const ColorFilter."+preset+"("),code);
   assertFalse(code.contains("m00:"));assertFalse(code.contains("saturation:"));
  }
  var peer=C.find(RawImageWidgetPropertySchema.TYPE).orElseThrow();for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
 }
 @Test void all29BlendModesSignedMatrixAndSaturationPreserveValues()throws Exception{
  var d=C.find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow();var codec=new FdDocumentCodec();
  var enumValues=(PropertyValueConstraint.EnumValues)d.property(p("blendMode")).orElseThrow().constraints().getFirst();
  assertEquals(29,enumValues.values().size());
  for(String blend:enumValues.values())assertTrue(code(node(Map.of(p("colorFilter"),s("mode"),p("blendMode"),new PropertyValue.EnumValue("BlendMode",blend)))).build().payload().contains("BlendMode."+blend));
  var values=new LinkedHashMap<PropertyName,PropertyValue>();for(int row=0;row<4;row++)for(int col=0;col<5;col++)values.put(p("m"+row+col),new PropertyValue.DoubleValue(BigDecimal.valueOf(-row*5-col)));
  values.put(p("color"),ref("inactiveColor"));var n=node(values);String source=code(n).build().payload();assertTrue(source.contains("-19.0"));assertFalse(source.contains("inactiveColor"));
  var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
  for(int value:List.of(-2,0,1,3))assertTrue(code(node(Map.of(p("colorFilter"),s("saturation"),p("saturation"),new PropertyValue.IntegerValue(BigInteger.valueOf(value))))).build().payload().contains("ColorFilter.saturation("+value+".0)"));
 }
 @Test void exactTypedProofsAndInactiveDraftsNeverEnterGeneratedDart(){
  int count=0;
  for(String name:List.of("colorFilter","color"))for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/filters.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
   var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(p("colorFilter"),s("mode"));p.put(p(name),value);
   var g=code(node(p));assertTrue(g.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(name.equals("color")?"Color":"ColorFilter")));count++;
  }
  assertEquals(16,count);
  var base=node(Map.of(p("colorFilter"),ref("projectFilter"),p("color"),ref("inactiveColor")));var g=code(base);assertTrue(g.build().payload().contains("projectFilter"));assertFalse(g.build().payload().contains("inactiveColor"));
  for(String mode:List.of("matrix","saturation","linearToSrgbGamma","srgbToLinearGamma")){
   assertFalse(code(node(Map.of(p("colorFilter"),s(mode),p("color"),ref("inactiveColor")))).build().payload().contains("inactiveColor"));
  }
 }
 @Test void filterCannotBeMissingOrNullAndAllRowsHaveClosedDomains(){
  var d=C.find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow();
  for(var field:d.properties())assertFalse(field.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
  assertFalse(d.property(p("colorFilter")).orElseThrow().constraints().stream().anyMatch(c->c.accepts(s("anything"))));
  var n=node(Map.of());assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(new WidgetNode(n.id(),n.type(),Map.of(),n.slots())),C).successful());
 }
}
