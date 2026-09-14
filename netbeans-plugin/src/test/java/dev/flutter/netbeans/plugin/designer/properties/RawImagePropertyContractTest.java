package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.command.*;
import java.util.*;
import java.math.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RawImagePropertyContractTest {
 @Test void all16RowsWritableStableAndEditableAfterReopen()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(RawImageWidgetPropertySchema.TYPE).orElseThrow();
  var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
  for(var field:d.properties()){
   String name=field.name().value();var row=TooltipPropertyContractTest.cell(node,name);
   assertTrue(row.canWrite(),name);assertTrue(row.supportsDefaultValue(),name);assertNotNull(FlutterTypedPropertyEditors.binding(field).orElseThrow());
   PropertyValue value=switch(name){
    case "invertColors","matchTextDirection","isAntiAlias"->new PropertyValue.BooleanValue(true);
    case "scale","opacity"->new PropertyValue.DoubleValue(BigDecimal.ONE);
    case "alignment"->new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,BigDecimal.ONE,BigDecimal.ZERO);
    case "repeat"->new PropertyValue.EnumValue("ImageRepeat","repeatX");
    case "filterQuality"->new PropertyValue.EnumValue("FilterQuality","high");
    default->new PropertyValue.NullValue();
   };
   row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),field.name(),value),commands.removeFirst());
   var p=new LinkedHashMap<>(widget.properties());p.put(field.name(),value);widget=new WidgetNode(widget.id(),widget.type(),p,Map.of());
   node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,name));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
   var editor=row.getPropertyEditor();editor.setValue(row.getValue());if(Set.of("image","centerSlice","opacity").contains(name))assertNotNull(editor.getCustomEditor());
   if(name.equals("image"))assertTrue(((javax.swing.JComponent)editor.getCustomEditor()).getAccessibleContext().getAccessibleDescription().contains("dart:ui.Image?"));
  }
 }
}
