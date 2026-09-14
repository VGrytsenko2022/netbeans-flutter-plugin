package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultTextStyleContractTest {
 public static final List<WidgetTypeId> TYPES=List.of(DefaultTextStyleWidgetPropertySchema.TYPE,DefaultTextStyleWidgetPropertySchema.MERGE_TYPE);
 public static final SlotName CHILD=new SlotName("child");
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 public static WidgetNode node(WidgetTypeId type){
   var n=WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random());
   return new WidgetNode(n.id(),type,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(AnimatedSizeContractTest.adapter())));
 }
 public static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){
   var all=new LinkedHashMap<>(n.properties());all.putAll(p);return new WidgetNode(n.id(),n.type(),all,n.slots());
 }
 public static DesignerDocument document(WidgetNode n){return AnimatedSizeContractTest.document(n);}
 static GeneratedDartRegions generate(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
 @Test void bothInsertableFormsHaveAll41FieldsRequiredChildAndNoEventsOrAnimation(){
   assertTrue(C.find(new WidgetTypeId("flutter.widgets.DefaultTextStyle.fallback")).isEmpty());
   for(var type:TYPES){
     var d=C.find(type).orElseThrow();boolean merge=type.equals(TYPES.getLast());
     assertEquals(41,d.properties().size());assertEquals(!merge,d.constConstructor());
     assertEquals(31,d.properties().stream().filter(p->AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(p.name())).count());
     assertEquals(3,d.properties().stream().filter(p->AnimatedDefaultTextStyleWidgetPropertySchema.heightLeaf(p.name())).count());
     assertEquals(1,d.slots().getFirst().minChildren());assertEquals(41,d.slots().getFirst().parameter().order());
     assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isPresent());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
     var n=node(type);assertEquals(merge?Map.of():Map.of(new PropertyName("style"),new PropertyValue.StringValue("local")),n.properties());
     var r=generate(n);var code=r.build().payload();
     assertTrue(code.contains(merge?"DefaultTextStyle.merge(":"DefaultTextStyle("),code);
     assertEquals(!merge,code.contains("style: const TextStyle("));
     assertFalse(code.contains("const DefaultTextStyle.merge("));
     assertEquals(merge,r.symbolOccurrences().stream().anyMatch(o->o.id().endsWith(":defaultTextStyleMergeFactory")&&o.symbolName().equals("merge")));
     for(String absent:List.of("durationUs","curve","onEnd"))assertTrue(d.property(new PropertyName(absent)).isEmpty());
   }
 }
 @Test void allReferenceFormsAndNullableParagraphsRoundTrip()throws Exception{
   for(var type:TYPES)for(String name:List.of("style","maxLines","textHeightBehavior"))
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
      var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/styles.dart"):Optional.empty(),
        member?"Styles":"style",member?Optional.of("configured"):Optional.empty(),
        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
        factory?Optional.of(false):Optional.empty());
      var n=with(node(type),Map.of(new PropertyName(name),value));var doc=document(n);var codec=new FdDocumentCodec();
      assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
      String expected=name.equals("maxLines")?"int?":name.equals("textHeightBehavior")?"TextHeightBehavior?":type.equals(TYPES.getFirst())?"TextStyle":"TextStyle?";
      assertTrue(generate(n).symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(expected)));
    }
   for(var type:TYPES)for(String name:List.of("style","textAlign","softWrap","overflow","maxLines","textWidthBasis","textHeightBehavior")){
     var n=with(node(type),Map.of(new PropertyName(name),new PropertyValue.NullValue()));
     boolean valid=type.equals(TYPES.getLast())||Set.of("textAlign","maxLines","textHeightBehavior").contains(name);
     assertEquals(valid,new WidgetTreeValidator().validate(document(n),C).valid(),type+" "+name);
     if(valid)assertTrue(generate(n).build().payload().contains(name+": null"));
   }
 }
 @Test void localProjectionThemesAndConflictsAreStrict(){
   for(var type:TYPES){
     var n=with(node(type),Map.of(new PropertyName("style"),new PropertyValue.StringValue("local"),
       new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.valueOf(24)),
       new PropertyName("styleColor"),new PropertyValue.ColorValue(0xff112233L),
       new PropertyName("styleFontWeight"),new PropertyValue.EnumValue("FontWeight","w700"),
       new PropertyName("textHeightApplyFirstAscent"),new PropertyValue.BooleanValue(false)));
     var code=generate(n).build().payload();for(String part:List.of("fontSize: 24.0","FontWeight.w700","applyHeightToFirstAscent: false"))assertTrue(code.contains(part),code);
     for(var value:List.<PropertyValue>of(AnimatedDefaultTextStyleContractTest.ref("_style"),new PropertyValue.NullValue()))
       assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("style"),value))),C).valid());
     assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("textHeightBehavior"),new PropertyValue.NullValue()))),C).valid());
     var noStyle=new LinkedHashMap<>(n.properties());noStyle.remove(new PropertyName("style"));
     assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),type,noStyle,n.slots())),C).valid());
     for(String bad:List.of("0","-1","9007199254740992"))assertFalse(new WidgetTreeValidator().validate(document(with(node(type),Map.of(new PropertyName("maxLines"),new PropertyValue.IntegerValue(new BigInteger(bad))))),C).valid());
     var themed=generate(with(node(type),Map.of(new PropertyName("style"),new PropertyValue.StringValue("local"),
       new PropertyName("styleThemeTextStyle"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge"))))).build().payload();
     assertEquals(type.equals(TYPES.getFirst()),themed.contains("?? const TextStyle()"),themed);
   }
 }
}
