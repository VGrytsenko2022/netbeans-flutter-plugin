package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageFilteredContractTest {
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static PropertyName p(String n){return new PropertyName(n);}
 static PropertyValue.StringValue s(String n){return new PropertyValue.StringValue(n);}
 static PropertyValue.DartObjectReferenceValue ref(String n){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),n,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 static WidgetNode node(Map<PropertyName,PropertyValue> values){
  var d=C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();
  var n=WidgetNodePrototypeFactory.create(d,StableId.random());var ps=new LinkedHashMap<>(n.properties());ps.putAll(values);
  return new WidgetNode(n.id(),n.type(),ps,n.slots());
 }
 static GeneratedDartRegions code(WidgetNode n){var g=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),C);assertTrue(g.successful(),g.diagnostics().toString());return g.generated().orElseThrow();}
 @Test void allSixFactoriesOptionalChildAndNativeEvents(){
  var d=C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();assertEquals(17,d.properties().size());assertEquals(0,d.slots().getFirst().minChildren());assertTrue(d.constConstructor());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
  for(String mode:ImageFilteredWidgetPropertySchema.FILTERS){
   String source=code(node(Map.of(p("imageFilter"),s(mode),p("shader"),ref("projectShader")))).build().payload();
   assertTrue(source.contains("ImageFilter."+mode+"("),source);assertFalse(source.contains("const ImageFilter."),source);
   assertEquals(mode.equals("shader"),source.contains("projectShader"));
  }
  var peer=C.find(ColorFilteredWidgetPropertySchema.TYPE).orElseThrow();
  for(var owner:C.definitions())for(var slot:owner.slots())assertEquals(WidgetPlacementRules.accepts(owner,slot,peer),WidgetPlacementRules.accepts(owner,slot,d));
 }
 @Test void completeMatrixBoundsAndInactiveDraftsSurviveCodec()throws Exception{
  var storage=java.util.stream.IntStream.range(0,16).mapToObj(i->BigDecimal.valueOf(i-5)).toList();
  var n=node(Map.of(p("imageFilter"),s("matrix"),p("matrix4"),new PropertyValue.Matrix4Value(storage),p("bounds"),ref("inactiveBounds"),p("shader"),ref("inactiveShader")));
  String source=code(n).build().payload();assertTrue(source.contains("Float64List.fromList(<double>[-5.0, -4.0"),source);assertFalse(source.contains("inactive"));assertFalse(source.contains("matrix4:"));
  var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(n);assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
  var ps=new LinkedHashMap<PropertyName,PropertyValue>();for(String f:List.of("boundsLeft","boundsTop","boundsWidth","boundsHeight"))ps.put(p(f),new PropertyValue.IntegerValue(BigInteger.TEN));
  source=code(node(ps)).build().payload();assertTrue(source.contains("Rect.fromLTWH(10.0, 10.0, 10.0, 10.0)"),source);
  ps.put(p("bounds"),new PropertyValue.NullValue());source=code(node(ps)).build().payload();assertTrue(source.contains("bounds: null"));assertFalse(source.contains("Rect.fromLTWH"));
  ps.put(p("imageFilter"),ref("sourceFilter"));source=code(node(ps)).build().payload();assertTrue(source.contains("sourceFilter"));assertFalse(source.contains("bounds:"));
 }
 @Test void all48SourceFormsHaveExactProofsAndIndependentActivePaths(){
  int count=0;
  for(String field:List.of("imageFilter","bounds","matrix4","inner","outer","shader"))for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   String mode=switch(field){case "matrix4"->"matrix";case "inner","outer"->"compose";case "shader"->"shader";default->"blur";};
   String expected=switch(field){case "bounds"->"Rect?";case "matrix4"->"Float64List";case "shader"->"FragmentShader";default->"ImageFilter";};
   var reference=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/filters.dart"):Optional.empty(),"Values",member?Optional.of("value"):Optional.empty(),
    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
   var ps=new LinkedHashMap<PropertyName,PropertyValue>();ps.put(p("imageFilter"),s(mode));ps.put(p(field),reference);
   var g=code(node(ps));assertTrue(g.symbolOccurrences().stream().filter(o->o.modelPath().contains("/properties/"+field)).flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(expected)),field);count++;
  }assertEquals(48,count);
 }
 @Test void closedDomainsAllEnumsAndShaderRequirement(){
  var d=C.find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();
  for(var property:d.properties()) assertEquals(Set.of("tileMode","bounds").contains(property.name().value()),property.constraints().stream().anyMatch(c->c.accepts(new PropertyValue.NullValue())),property.name().value());
  for(String field:List.of("tileMode","filterQuality")){
   var values=(PropertyValueConstraint.EnumValues)d.property(p(field)).orElseThrow().constraints().getFirst();assertEquals(4,values.values().size());
   for(String value:values.values()){String source=code(node(Map.of(p("imageFilter"),s(field.equals("tileMode")?"blur":"matrix"),p(field),new PropertyValue.EnumValue(values.dartType().name(),value)))).build().payload();assertTrue(source.contains(values.dartType().name()+"."+value));}
  }
  for(String mode:List.of("blur","dilate","erode")) {
   var values = new LinkedHashMap<PropertyName,PropertyValue>(); values.put(p("imageFilter"),s(mode));
   for(String field:mode.equals("blur")?List.of("sigmaX","sigmaY"):List.of("radiusX","radiusY"))values.put(p(field),new PropertyValue.IntegerValue(BigInteger.valueOf(-2)));
   assertTrue(code(node(values)).build().payload().contains(": -2.0"),mode);
  }
  assertTrue(ImageFilteredWidgetPropertySchema.relationshipError(node(Map.of(p("imageFilter"),s("shader")))).isPresent());
  assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(node(Map.of(p("imageFilter"),s("shader")))),C).successful());
  for(String mode:List.of("blur","dilate","erode","matrix","compose"))assertTrue(ImageFilteredWidgetPropertySchema.relationshipError(node(Map.of(p("imageFilter"),s(mode)))).isEmpty());
  for(String field:List.of("sigmaX","sigmaY","radiusX","radiusY","boundsWidth","boundsHeight"))assertTrue(d.property(p(field)).orElseThrow().constraints().stream().anyMatch(c->c.accepts(new PropertyValue.IntegerValue(BigInteger.ONE.negate()))));
 }
}
