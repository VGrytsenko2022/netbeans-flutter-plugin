package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.ImageFilteredContractTest.*;
class BackdropFilterContractTest {
 static final List<WidgetTypeId> FILTER_TYPES=List.of(BackdropFilterWidgetPropertySchema.TYPE,BackdropFilterWidgetPropertySchema.GROUPED);
 static WidgetNode backdrop(WidgetTypeId type,Map<PropertyName,PropertyValue> values){
  var n=WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random());var ps=new LinkedHashMap<>(n.properties());ps.putAll(values);
  var slots=type.equals(BackdropFilterWidgetPropertySchema.GROUP)?Map.<SlotName,WidgetSlot>of(new SlotName("child"),WidgetSlot.SingleSlot.of(
    WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.SizedBox")).orElseThrow(),StableId.random()))):n.slots();
  return new WidgetNode(n.id(),n.type(),ps,slots);
 }
 @Test void constructorsCompletePropertiesAndPlacement(){
  assertEquals(26,C.find(FILTER_TYPES.getFirst()).orElseThrow().properties().size());
  assertEquals(25,C.find(FILTER_TYPES.getLast()).orElseThrow().properties().size());
  var group=C.find(BackdropFilterWidgetPropertySchema.GROUP).orElseThrow();assertEquals(1,group.properties().size());assertFalse(group.constConstructor());
  assertEquals(1,group.slots().getFirst().minChildren());
  for(var type:FILTER_TYPES){var d=C.find(type).orElseThrow();assertTrue(d.constConstructor());assertEquals(0,d.slots().getFirst().minChildren());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
   for(String mode:ImageFilteredWidgetPropertySchema.FILTERS){
    var source=code(backdrop(type,Map.of(p("filter"),s(mode),p("shader"),ref("shaderValue")))).build().payload();
    assertTrue(source.contains("ImageFilter."+mode+"("),source);
    assertEquals(type.equals(BackdropFilterWidgetPropertySchema.GROUPED),source.contains("BackdropFilter.grouped("),source);
    assertFalse(source.contains("filterConfig:"));assertEquals(mode.equals("shader"),source.contains("shaderValue"));
   }
   for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow()),WidgetPlacementRules.accepts(owner,slot,d));
  }
  assertFalse(C.find(BackdropFilterWidgetPropertySchema.GROUPED).orElseThrow().properties().stream().anyMatch(p->p.name().value().equals("backdropGroupKey")));
  assertTrue(WidgetEventCatalog.eventsFor(group).isEmpty());
 }
 @Test void configFactoriesPrecedenceAndInactiveDrafts()throws Exception{
  for(var type:FILTER_TYPES)for(String config:BackdropFilterWidgetPropertySchema.CONFIGS){
   var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(p("filterConfig"),s(config));values.put(p("filter"),config.equals("wrap")?s("matrix"):ref("inactiveFilter"));
   values.put(p("bounds"),ref("inactiveBounds"));values.put(p("configSigmaX"),new PropertyValue.IntegerValue(BigInteger.valueOf(-2)));values.put(p("configBounded"),new PropertyValue.BooleanValue(true));
   var n=backdrop(type,values);String src=code(n).build().payload();assertTrue(src.contains("filterConfig:"),src);assertFalse(src.contains("filter:"),src);assertFalse(src.contains("inactive"),src);
   assertTrue(src.contains("ImageFilterConfig"+(config.equals("wrap")?"(":"."+config+"(")),src);
   if(config.equals("blur")){assertTrue(src.contains("sigmaX: -2"),src);assertTrue(src.contains("bounded: true"),src);assertTrue(src.contains("const "),src);}
   var doc=RotationTransitionContractTest.document(n);var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
  }
  String source=code(backdrop(FILTER_TYPES.getFirst(),Map.of(p("filterConfig"),ref("sourceConfig"),p("filter"),s("shader")))).build().payload();
  assertTrue(source.contains("sourceConfig"),source);assertFalse(source.contains("ImageFilter.shader"),source);
 }
 @Test void activeRequirementsAndClosedNulls(){
  for(var type:FILTER_TYPES){
   var d=C.find(type).orElseThrow();
   for(var prop:d.properties())assertEquals(Set.of("filter","filterConfig","bounds","tileMode","backdropGroupKey").contains(prop.name().value()),prop.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())),prop.name().value());
   for(var config:List.<PropertyValue>of(new PropertyValue.NullValue(),s("wrap")))
    assertTrue(BackdropFilterWidgetPropertySchema.relationshipError(backdrop(type,Map.of(p("filter"),new PropertyValue.NullValue(),p("filterConfig"),config))).isPresent());
   for(String config:List.of("blur","compose")) assertTrue(BackdropFilterWidgetPropertySchema.relationshipError(backdrop(type,Map.of(p("filter"),new PropertyValue.NullValue(),p("filterConfig"),s(config)))).isEmpty());
   assertTrue(BackdropFilterWidgetPropertySchema.relationshipError(backdrop(type,Map.of(p("filter"),s("shader")))).isPresent());
   assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(backdrop(type,Map.of(p("filter"),new PropertyValue.NullValue()))),C).successful());
   var blends=(PropertyValueConstraint.EnumValues)d.property(p("blendMode")).orElseThrow().constraints().getFirst();assertEquals(29,blends.values().size());
   for(String blend:blends.values()) assertTrue(code(backdrop(type,Map.of(p("blendMode"),new PropertyValue.EnumValue("BlendMode",blend)))).build().payload().contains("BlendMode."+blend));
  }
 }
 @Test void inactiveSourcePackagesDoNotBecomeDartImports(){
  var missing=new PropertyValue.DartObjectReferenceValue(Optional.of("package:absent/never.dart"),"AbsentValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
  var backdrop=backdrop(FILTER_TYPES.getFirst(),Map.of(p("filterConfig"),s("blur"),p("filter"),missing,p("configInner"),missing,p("shader"),missing));
  assertFalse(code(backdrop).imports().payload().contains("package:absent"));
  var image=node(Map.of(p("imageFilter"),s("matrix"),p("bounds"),missing,p("shader"),missing));
  assertFalse(code(image).imports().payload().contains("package:absent"));
  assertTrue(code(backdrop(FILTER_TYPES.getFirst(),Map.of(p("filterConfig"),s("wrap"),p("filter"),missing))).imports().payload().contains("package:absent/never.dart"));
 }
 @Test void allSourceFormsExactActiveTypesAndKeyIdentity(){
  int count=0;
  for(var type:FILTER_TYPES)for(String field:List.of("filter","filterConfig","configInner","configOuter","bounds","matrix4","inner","outer","shader","backdropGroupKey")){
   if(type.equals(BackdropFilterWidgetPropertySchema.GROUPED)&&field.equals("backdropGroupKey"))continue;
   for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
    var reference=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/filters.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),
     factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
    var values=new LinkedHashMap<PropertyName,PropertyValue>();
    values.put(p("filter"),s(switch(field){case "matrix4"->"matrix";case "inner","outer"->"compose";case "shader"->"shader";default->"blur";}));
    if(field.equals("configInner")||field.equals("configOuter"))values.put(p("filterConfig"),s("compose"));
    values.put(p(field),reference);
    String expected=switch(field){case "filterConfig","configInner","configOuter"->"ImageFilterConfig";case "bounds"->"Rect?";case "matrix4"->"Float64List";case "shader"->"FragmentShader";case "backdropGroupKey"->"BackdropKey?";default->"ImageFilter";};
    assertTrue(code(backdrop(type,values)).symbolOccurrences().stream().filter(o->o.modelPath().contains("/properties/"+field)).flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(expected)),type+"."+field);count++;
   }
  }
  assertEquals(152,count);
  assertTrue(code(backdrop(BackdropFilterWidgetPropertySchema.GROUP,Map.of(p("backdropKey"),ref("sharedKey")))).symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("BackdropKey?")));
 }
}
