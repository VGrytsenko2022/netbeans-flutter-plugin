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

public class AnimatedThemeContractTest {
 public static final WidgetTypeId TYPE=AnimatedThemeWidgetPropertySchema.TYPE;
 public static final SlotName CHILD=new SlotName("child");
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 public static WidgetNode node(){var n=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());return new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(PhysicalModelTestSupport.text())));}
 public static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var all=new LinkedHashMap<>(n.properties());all.putAll(p);return new WidgetNode(n.id(),TYPE,all,n.slots());}
 public static DesignerDocument document(WidgetNode n){return PhysicalModelTestSupport.document(n);}
 static GeneratedDartRegions generate(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 @Test void exactDefaultsRequiredDataAndChildOptionalDurationNativeEvent(){
   var d=C.find(TYPE).orElseThrow();assertEquals(4,d.properties().size());assertTrue(d.constConstructor());
   assertEquals(AnimatedThemeWidgetPropertySchema.FIELDS.stream().map(f->f.name()).toList(),d.properties().stream().map(p->p.name().value()).toList());
   assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isPresent());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
   assertEquals(1,d.slots().getFirst().minChildren());assertEquals(4,d.slots().getFirst().parameter().order());
   var e=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals("onEnd",e.propertyName().value());assertEquals("VoidCallback",e.callbackType());assertTrue(e.nullableCallback());
   var n=node();assertEquals(Map.of(new PropertyName("data"),new PropertyValue.StringValue("light")),n.properties());
   var code=generate(n).build().payload();assertTrue(code.contains("data: ThemeData.light()"),code);assertFalse(code.contains("const AnimatedTheme("),code);
   for(String v:List.of("duration:","curve:","onEnd:"))assertFalse(code.contains(v),code);
   assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,Map.of(),n.slots())),C).valid());
   assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
 }
 @Test void allSixFactoriesAnd43CurvesRoundTripWithExactDurationAndNonConstFactoryEvidence()throws Exception{
   for(String preset:AnimatedThemeWidgetPropertySchema.PRESETS)for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()){
     var n=with(node(),Map.of(new PropertyName("data"),new PropertyValue.StringValue(preset),new PropertyName("curve"),new PropertyValue.StringValue(curve),
         new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.ZERO)));
     var doc=document(n);var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
     var result=generate(n);var code=result.build().payload();
     assertTrue(code.contains("data: ThemeData."+preset.replace("M2","")+(preset.endsWith("M2")?"(useMaterial3: false)":"()")),code);
     assertTrue(code.contains("curve: Curves."+curve),code);assertTrue(code.contains("duration: const Duration(microseconds: 0)"),code);
     assertFalse(code.contains("const AnimatedTheme("),code);
     assertEquals(2,result.symbolOccurrences().stream().filter(o->o.id().contains("animated-theme")&&o.libraryUri().equals("package:flutter/material.dart")).count());
     assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":animated-theme-duration")&&o.libraryUri().equals("dart:core")));
   }
 }
 @Test void wholeThemeAndOtherFieldsSupportStrictReferenceForms(){
   for(var binding:Map.of("data","ThemeData","curve","Curve","durationUs","Duration","onEnd","VoidCallback").entrySet())
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
     var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/theme.dart"):Optional.empty(),member?"Themes":"theme",
       member?Optional.of("value"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
       factory?Optional.of(false):Optional.empty());
     var result=generate(with(node(),Map.of(new PropertyName(binding.getKey()),ref)));
     assertTrue(result.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(binding.getValue())));
    }
 }
 @Test void rejectsWrongPresetsNullDataCurveDurationAndMissingChild(){
   for(String field:List.of("data","curve","durationUs"))
      assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))),C).valid());
   assertTrue(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("onEnd"),new PropertyValue.NullValue()))),C).valid());
   for(String preset:List.of("Theme.of(context)","ThemeData()","Light","darkM3","local","light);evil()"))
      assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("data"),new PropertyValue.StringValue(preset)))),C).valid());
   for(long value:List.of(-1L,9007199254740992L))assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(value))))),C).valid());
 }
}
