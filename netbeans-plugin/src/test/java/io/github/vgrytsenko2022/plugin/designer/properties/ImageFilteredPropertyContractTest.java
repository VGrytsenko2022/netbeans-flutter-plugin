package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import java.util.*;
import java.math.*;
import javax.swing.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ImageFilteredPropertyContractTest {
 @Test void all17RowsWritableStableAfterEditAndReopenAndSixPresets()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(ImageFilteredWidgetPropertySchema.TYPE).orElseThrow();
  var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
  for(var field:d.properties()){
   String name=field.name().value();var row=TooltipPropertyContractTest.cell(node,name);assertTrue(row.canWrite(),name);assertEquals(!name.equals("imageFilter"),row.supportsDefaultValue());
   PropertyValue value=switch(name){case "imageFilter"->new PropertyValue.StringValue("dilate");case "enabled"->new PropertyValue.BooleanValue(false);case "tileMode"->new PropertyValue.EnumValue("TileMode","decal");case "filterQuality"->new PropertyValue.EnumValue("FilterQuality","high");case "matrix4"->ImageFilteredWidgetPropertySchema.identity();case "bounds","inner","outer","shader"->new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());default->new PropertyValue.DoubleValue(BigDecimal.ONE);};
   row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),field.name(),value),commands.removeFirst());
   var values=new LinkedHashMap<>(widget.properties());values.put(field.name(),value);widget=new WidgetNode(widget.id(),widget.type(),values,widget.slots());
   node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,name));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
   var editor=row.getPropertyEditor();editor.setValue(row.getValue());
   if(name.equals("imageFilter")){
    var queue=new ArrayDeque<java.awt.Component>();queue.add(editor.getCustomEditor());JComboBox<?> presets=null;
    while(!queue.isEmpty()){var v=queue.removeFirst();if(FlutterPresetDartReferenceEditorComponent.PRESET_NAME.equals(v.getName()))presets=(JComboBox<?>)v;if(v instanceof java.awt.Container c)queue.addAll(Arrays.asList(c.getComponents()));}
    assertNotNull(presets);assertEquals(6,presets.getItemCount());
   }
   if(name.equals("matrix4")) assertNotNull(editor.getCustomEditor());
  }
 }
}
