package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RawImageContractTest {
 static boolean accepts(WidgetDefinition d,String n,PropertyValue v){return d.property(new PropertyName(n)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(v));}
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static WidgetNode node(Map<PropertyName,PropertyValue> p){return new WidgetNode(StableId.random(),RawImageWidgetPropertySchema.TYPE,p,Map.of());}
 static GeneratedDartRegions code(WidgetNode n){var r=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 @Test void exact16OptionalParametersNoSlotsNoEventsAndSafeEmptyCreation(){
  var d=C.find(RawImageWidgetPropertySchema.TYPE).orElseThrow();
  assertEquals(List.of("image","debugImageLabel","width","height","scale","color","opacity","colorBlendMode","fit","alignment","repeat","centerSlice","matchTextDirection","invertColors","filterQuality","isAntiAlias"),d.properties().stream().map(p->p.name().value()).toList());
  for(int i=0;i<16;i++){assertEquals(DartParameter.named(i,false),d.properties().get(i).parameter());assertTrue(d.properties().get(i).creationDefault().isEmpty());}
  assertTrue(d.constConstructor());assertTrue(d.slots().isEmpty());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
  var n=WidgetNodePrototypeFactory.create(d,StableId.random());assertTrue(n.properties().isEmpty());
  String s=code(n).build().payload();assertTrue(s.contains("RawImage("));assertFalse(s.contains("image:"));assertFalse(s.contains("MemoryImage"));
  var peer=C.find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow();
  for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
 }
 @Test void nullableLocalPaintingAndAllEnumsRoundTrip()throws Exception{
  var d=C.find(RawImageWidgetPropertySchema.TYPE).orElseThrow();var codec=new FdDocumentCodec();
  for(var p:d.properties()){
   if(p.acceptedKinds().contains(PropertyValueKind.NULL)){
    var n=node(Map.of(p.name(),new PropertyValue.NullValue()));assertTrue(code(n).build().payload().contains(p.name()+": null"));
    var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
   }
   for(var c:p.constraints())if(c instanceof PropertyValueConstraint.EnumValues v)for(String value:v.values())code(node(Map.of(p.name(),new PropertyValue.EnumValue(v.dartType().name(),value))));
  }
  for(PropertyValue v:List.of(new PropertyValue.IntegerValue(BigInteger.ZERO),new PropertyValue.DoubleValue(new BigDecimal("0.35")),new PropertyValue.IntegerValue(BigInteger.ONE))){
   String s=code(node(Map.of(new PropertyName("opacity"),v))).build().payload();assertTrue(s.contains("AlwaysStoppedAnimation<double>"),s);
  }
 }
 @Test void all64TypedReferenceFormsAreManifestedAndDoNotCopySourceToCanvas()throws Exception{
  int count=0;
  for(var p:C.find(RawImageWidgetPropertySchema.TYPE).orElseThrow().properties())for(var c:p.constraints())if(c instanceof PropertyValueConstraint.DartObjectReferenceValues required)
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/refs.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
   var n=node(Map.of(p.name(),ref));var g=code(n);assertTrue(g.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(required.expectedDartType())));
   var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());count++;
  }
  assertEquals(64,count);
 }
 @Test void localDomainRejectsBadScaleOpacityWrongImageKindAndRequiredNulls(){
  var d=C.find(RawImageWidgetPropertySchema.TYPE).orElseThrow();
  for(String name:List.of("scale","alignment","repeat","filterQuality","invertColors","matchTextDirection","isAntiAlias"))assertFalse(accepts(d,name,new PropertyValue.NullValue()));
  for(String n:List.of("scale","opacity","width","height"))assertFalse(accepts(d,n,new PropertyValue.DoubleValue(BigDecimal.ONE.negate())));
  assertFalse(accepts(d,"scale",new PropertyValue.DoubleValue(BigDecimal.ZERO)));
  assertFalse(accepts(d,"opacity",new PropertyValue.DoubleValue(new BigDecimal("1.1"))));
  assertFalse(accepts(d,"image",PropertyValue.ImageProviderValue.unresolved()));
 }
}
