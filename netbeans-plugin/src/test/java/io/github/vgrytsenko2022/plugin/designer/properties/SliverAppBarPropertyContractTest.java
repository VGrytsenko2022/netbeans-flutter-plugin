package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverAppBarPropertyContractTest {
 static PropertyName p(String n){return new PropertyName(n);}
 static WidgetNode apply(WidgetNode n,DesignerCommand c){return RadioPropertyContractTest.apply(n,c);}
 static FlutterWidgetPropertiesNode node(WidgetNode w,List<DesignerCommand> commands){return new FlutterWidgetPropertiesNode(Children.LEAF,w,BuiltInWidgetCatalog.getDefault().find(w.type()).orElseThrow(),commands::add);}
 @Test void everyRowIsTypedWritableStableAndResettableAcrossAllVariants() throws Exception {
  for(String type:SliverAppBarWidgetPropertySchema.TYPES){
   var d=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
   var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();var n=node(w,commands);var sets=n.getPropertySets();
   assertTrue(TooltipPropertyContractTest.cell(n,"toolbarHeight").getShortDescription()
           .contains("native constructor default: " + SliverAppBarWidgetPropertySchema.toolbarDefault(w.type())));
   for(var property:d.properties()){
    var row=TooltipPropertyContractTest.cell(n,property.name().value());assertTrue(row.canWrite(),property.name().value());assertTrue(row.supportsDefaultValue(),property.name().value());
    assertEquals(FlutterPropertyCellValue.unset(),row.getValue());assertNotNull(row.getPropertyEditor());
    n.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());
    assertSame(row,TooltipPropertyContractTest.cell(n,property.name().value()));assertArrayEquals(sets,n.getPropertySets());
    assertEquals(row.getValue(),TooltipPropertyContractTest.cell(node(w,commands),property.name().value()).getValue());
   }
  }
 }
 @Test void snapFloatingWholeLocalAndShapeChangesAreAtomicWithoutLosingChildren() throws Exception {
  for(String type:SliverAppBarWidgetPropertySchema.TYPES){
   var d=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();var w=WidgetNodePrototypeFactory.create(d,StableId.random());
   var commands=new ArrayList<DesignerCommand>();var n=node(w,commands);
   TooltipPropertyContractTest.cell(n,"snap").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));
   assertEquals(1,commands.size());w=apply(w,commands.removeFirst());assertEquals(new PropertyValue.BooleanValue(true),w.properties().get(p("floating")));
   n=node(w,commands);TooltipPropertyContractTest.cell(n,"floating").restoreDefaultValue();w=apply(w,commands.removeFirst());
   assertFalse(w.properties().containsKey(p("floating")));assertEquals(new PropertyValue.BooleanValue(false),w.properties().get(p("snap")));
   for(String whole:SliverAppBarWidgetPropertySchema.WHOLE_TYPES.keySet()){
    var props=new LinkedHashMap<>(w.properties());props.put(p(whole),new PropertyValue.NullValue());w=new WidgetNode(w.id(),w.type(),props,w.slots());
    n=node(w,commands);
    String local=switch(whole){case "shape"->"shapeRadiusTopLeft";case "iconTheme","actionsIconTheme"->whole+"Size";case "toolbarTextStyle","titleTextStyle"->whole+"FontSize";default->whole+"StatusBarColor";};
    PropertyValue value=whole.equals("systemOverlayStyle")?new PropertyValue.ColorValue(0xff123456L):new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8));
    TooltipPropertyContractTest.cell(n,local).setValue(FlutterPropertyCellValue.explicit(value));assertEquals(1,commands.size());
    w=apply(w,commands.removeFirst());assertFalse(w.properties().containsKey(p(whole)));assertEquals(value,w.properties().get(p(local)));
    n=node(w,commands);TooltipPropertyContractTest.cell(n,whole).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
    w=apply(w,commands.removeFirst());assertFalse(w.properties().containsKey(p(local)));
   }
  }
 }
}
