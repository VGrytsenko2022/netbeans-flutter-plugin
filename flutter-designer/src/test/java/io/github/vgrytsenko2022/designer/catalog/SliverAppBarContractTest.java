package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverAppBarContractTest {
 static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
 static PropertyName p(String n){return new PropertyName(n);}
 static PropertyValue i(long n){return new PropertyValue.IntegerValue(BigInteger.valueOf(n));}
 static WidgetNode node(String t,Map<PropertyName,PropertyValue> props){return new WidgetNode(StableId.random(),new WidgetTypeId(t),props,Map.of());}
 static DesignerDocument doc(WidgetNode n){return SliverCrossAxisExpandedContractTest.document(n);}
 static boolean valid(WidgetNode n){return new WidgetTreeValidator().validate(doc(n),C).valid();}
 static String code(WidgetNode n){var result=new DartRegionGenerator().generate(doc(n),C);assertTrue(result.generated().isPresent(),result.toString());return result.generated().orElseThrow().build().payload();}
 @Test void allThreeConstructorsHaveEveryNativeArgumentFiveIndependentSlotsAndOneAsyncEvent(){
  for(String t:SliverAppBarWidgetPropertySchema.TYPES){
   var d=C.find(new WidgetTypeId(t)).orElseThrow();
   assertEquals(131,d.properties().size());assertEquals(5,d.slots().size());assertTrue(d.constConstructor());
   assertEquals("SliverAppBar",d.dartClassName());assertEquals(t.endsWith(".medium")?Optional.of("medium"):t.endsWith(".large")?Optional.of("large"):Optional.empty(),d.namedConstructor());
   assertEquals(Set.of(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),d.traits());assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
   for(String excluded:SliverAppBarWidgetPropertySchema.EXCLUDED)assertTrue(d.property(p(excluded)).isEmpty());
   for(var capability:WidgetCapability.values())assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
   var event=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals(1,WidgetEventCatalog.eventsFor(d).size());
   assertEquals("AsyncCallback",event.callbackType());assertEquals(WidgetEventDescriptor.Kind.EVENT,event.kind());
   assertTrue(valid(node(t,Map.of())));assertTrue(code(node(t,Map.of())).contains(t.substring("flutter.material.".length())+"("));
   var bottom=d.slots().stream().filter(s->s.name().value().equals("bottom")).findFirst().orElseThrow();
   for(var source:C.definitions())assertEquals(source.traits().contains(BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT),WidgetPlacementRules.accepts(d,bottom,source));
  }
 }
 @Test void allBehaviorCombinationsAndDefaultDependentHeightGuardsAreExact() throws Exception {
  for(String t:SliverAppBarWidgetPropertySchema.TYPES){
   for(int mask=0;mask<64;mask++){
    var props=new LinkedHashMap<PropertyName,PropertyValue>();int bit=0;
    for(String n:List.of("floating","snap","pinned","stretch","forceElevated","primary"))props.put(p(n),new PropertyValue.BooleanValue((mask&(1<<bit++))!=0));
    var n=node(t,props);boolean expected=(mask&2)==0||(mask&1)!=0;assertEquals(expected,valid(n));
    if(expected){assertTrue(code(n).contains("snap:"));var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());}
   }
   long min=(long)SliverAppBarWidgetPropertySchema.toolbarDefault(new WidgetTypeId(t));
   assertFalse(valid(node(t,Map.of(p("collapsedHeight"),i(min-1)))));assertTrue(valid(node(t,Map.of(p("collapsedHeight"),i(min)))));
   assertFalse(valid(node(t,Map.of(p("stretchTriggerOffset"),i(0)))));assertTrue(valid(node(t,Map.of(p("stretchTriggerOffset"),i(1)))));
   assertFalse(valid(node(t,Map.of(p("toolbarHeight"),new PropertyValue.NullValue()))));
   assertTrue(code(node(t,Map.of(p("onStretchTrigger"),new PropertyValue.StringValue("noop")))).contains("onStretchTrigger: () async {}"));
  }
 }
 @Test void allWholeStylesSupportTypedAccessAndRejectLocalConflictsIncludingNull(){
  for(String t:SliverAppBarWidgetPropertySchema.TYPES)for(String whole:SliverAppBarWidgetPropertySchema.WHOLE_TYPES.keySet()){
   for(boolean invoke:List.of(false,true))for(boolean imported:List.of(false,true)){
    var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:app/styles.dart"):Optional.empty(),"styles",Optional.of("value"),
      invoke?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,invoke?Optional.of(false):Optional.empty());
    var code=code(node(t,Map.of(p(whole),ref)));assertTrue(code.contains(whole+":"));assertTrue(code.contains("styles.value"+(invoke?"()":"")));
   }
   assertTrue(code(node(t,Map.of(p(whole),new PropertyValue.NullValue()))).contains(whole+": null"));
   for(String local:SliverAppBarWidgetPropertySchema.localFamily(whole))assertFalse(valid(node(t,Map.of(p(whole),new PropertyValue.NullValue(),p(local),new PropertyValue.NullValue()))));
  }
 }
 @Test void sharedStyleLeavesStillGenerateCompositesAndServicesImport(){
  var props=Map.of(p("shapeKind"),new PropertyValue.StringValue("roundedRectangle"),p("shapeRadiusTopLeft"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8)),
    p("iconThemeSize"),i(20),p("actionsIconThemeSize"),i(22),p("toolbarTextStyleFontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(14)),
    p("titleTextStyleFontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(24)),p("systemOverlayStyleStatusBarBrightness"),new PropertyValue.EnumValue("Brightness","dark"));
  for(String t:SliverAppBarWidgetPropertySchema.TYPES){String out=code(node(t,props));for(String part:List.of("shape:","RoundedRectangleBorder","iconTheme:","actionsIconTheme:","toolbarTextStyle:","titleTextStyle:","SystemUiOverlayStyle"))assertTrue(out.contains(part),out);}
 }
}
