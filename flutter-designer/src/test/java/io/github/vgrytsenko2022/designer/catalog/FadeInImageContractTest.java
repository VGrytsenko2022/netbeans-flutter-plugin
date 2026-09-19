package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FadeInImageContractTest {
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static WidgetNode node(Map<PropertyName,PropertyValue> p){var n=WidgetNodePrototypeFactory.create(C.find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());var v=new LinkedHashMap<>(n.properties());v.putAll(p);return new WidgetNode(n.id(),n.type(),v,Map.of());}
 static GeneratedDartRegions code(WidgetNode n){var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();}
 @Test void exactConstructorAll23ParametersTwoRequiredProvidersNoEvents() {
  var d=C.find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow();
  assertEquals(List.of("placeholder","placeholderErrorBuilder","image","imageErrorBuilder","excludeFromSemantics","imageSemanticLabel","fadeOutDurationUs","fadeOutCurve","fadeInDurationUs","fadeInCurve","color","colorBlendMode","placeholderColor","placeholderColorBlendMode","width","height","fit","placeholderFit","filterQuality","placeholderFilterQuality","alignment","repeat","matchTextDirection"),d.properties().stream().map(p->p.name().value()).toList());
  for(int i=0;i<23;i++){assertEquals(DartParameter.named(i,i==0||i==2),d.properties().get(i).parameter());assertEquals(i==0||i==2,d.properties().get(i).creationDefault().isPresent());}
  assertTrue(d.constConstructor());assertTrue(d.slots().isEmpty());assertEquals(2, WidgetEventCatalog.eventsFor(d).size());assertTrue(WidgetEventCatalog.eventsFor(d).stream().allMatch(e->e.kind()==WidgetEventDescriptor.Kind.BUILDER));
  var g=code(node(Map.of()));assertTrue(g.build().payload().contains("FadeInImage("));assertTrue(g.build().payload().contains("placeholder:"));assertTrue(g.build().payload().contains("image:"));assertTrue(g.build().payload().contains("MemoryImage("));
  var peer=C.find(AnimatedIconWidgetPropertySchema.TYPE).orElseThrow();
  for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
 }
 @Test void independentAssetsResizeDurationsCurvesEnumsAndNullableValuesRoundTrip()throws Exception {
  var d=C.find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow();var codec=new FdDocumentCodec();
  var a=new PropertyValue.ImageProviderValue(PropertyValue.ImageProviderValue.ProviderKind.ASSET,"assets/placeholder.png",Optional.empty(),Optional.empty(),Optional.empty());
  var b=new PropertyValue.ImageProviderValue(PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET,"assets/target.png",Optional.of("pictures"),Optional.of(new BigDecimal("2")),Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.of(128), Optional.of(64), PropertyValue.ImageProviderValue.ResizePolicy.FIT, true)));
  for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()) {
   var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName("placeholder"),a);p.put(new PropertyName("image"),b);
   p.put(new PropertyName("fadeOutCurve"),new PropertyValue.StringValue(curve));p.put(new PropertyName("fadeInCurve"),new PropertyValue.StringValue(curve));
   p.put(new PropertyName("fadeOutDurationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(1000)));p.put(new PropertyName("fadeInDurationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(999999)));
   for(var property:d.properties())if(property.acceptedKinds().contains(PropertyValueKind.NULL))p.putIfAbsent(property.name(),new PropertyValue.NullValue());
   var n=node(p);var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
   var g=code(n);String s=g.build().payload();assertTrue(s.contains("Curves."+curve));assertTrue(s.contains("fadeOutDuration: const Duration(microseconds: 1000)"));assertTrue(s.contains("fadeInDuration: const Duration(microseconds: 999999)"));assertFalse(s.contains("DurationUs:"));assertTrue(s.contains("assets/placeholder.png"));assertTrue(s.contains("ExactAssetImage("));
   assertEquals(g.symbolOccurrences().size(),g.symbolOccurrences().stream().map(o->o.id()).distinct().count());
  }
  for(var p:d.properties())for(var constraint:p.constraints())if(constraint instanceof PropertyValueConstraint.EnumValues values)for(String v:values.values())code(node(Map.of(p.name(),new PropertyValue.EnumValue(values.dartType().name(),v))));
 }
 @Test void all104TypedFormsCarryExactAnalyzerWitnesses() {
  for(var property:C.find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow().properties())
  for(var constraint:property.constraints())if(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues required)
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/refs.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
   var g=code(node(Map.of(property.name(),ref)));assertTrue(g.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(required.expectedDartType())));
  }
 }
 @Test void missingProvidersAndInvalidDurationsAreRejected(){
  var n=node(Map.of());var d=C.find(n.type()).orElseThrow();
  for(String name:List.of("image","placeholder")){var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(name));assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(new WidgetNode(n.id(),n.type(),p,Map.of())),C).successful());}
  for(String name:List.of("fadeInDurationUs","fadeOutDurationUs"))for(int value:List.of(-1,0,1,999))assertFalse(d.property(new PropertyName(name)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.IntegerValue(BigInteger.valueOf(value)))));
  for(String name:List.of("placeholder","image","fadeInDurationUs","fadeOutDurationUs","fadeInCurve","fadeOutCurve","alignment","filterQuality","repeat"))assertFalse(d.property(new PropertyName(name)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())));
 }
}
