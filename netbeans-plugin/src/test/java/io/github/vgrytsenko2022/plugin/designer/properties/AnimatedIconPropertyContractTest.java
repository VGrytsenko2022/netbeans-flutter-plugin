package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import java.util.*;
import javax.swing.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedIconPropertyContractTest{
 @Test void completeTypedPropertiesStableRowsAndPreviewEditor()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(AnimatedIconWidgetPropertySchema.TYPE).orElseThrow();var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
  var values=Map.<String,PropertyValue>of("icon",new PropertyValue.StringValue("play_pause"),"progress",new PropertyValue.DoubleValue(new java.math.BigDecimal("-.5")),"size",new PropertyValue.NullValue(),"color",new PropertyValue.ColorValue(0x88223344L),"semanticLabel",new PropertyValue.StringValue(""),"textDirection",new PropertyValue.EnumValue("TextDirection","rtl"));
  for(var e:values.entrySet()){
   var row=TooltipPropertyContractTest.cell(node,e.getKey());assertTrue(row.canWrite());assertEquals(!List.of("icon","progress").contains(e.getKey()),row.supportsDefaultValue());
   row.setValue(FlutterPropertyCellValue.explicit(e.getValue()));assertEquals(new SetProperty(widget.id(),new PropertyName(e.getKey()),e.getValue()),commands.removeFirst());
   var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName(e.getKey()),e.getValue());widget=new WidgetNode(widget.id(),widget.type(),p,Map.of());
   node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,e.getKey()));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,e.getKey()).getValue());
  }
  var row=TooltipPropertyContractTest.cell(node,"icon");var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertTrue(editor.isPaintable());assertTrue(editor.getAsText().contains("AnimatedIcons.play_pause"));
  var panel=editor.getCustomEditor();var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);JComboBox<?> presets=null,modes=null;JLabel preview=null;
  while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterPresetDartReferenceEditorComponent.PRESET_NAME.equals(c.getName()))presets=(JComboBox<?>)c;if(FlutterPresetDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(JComboBox<?>)c;if(FlutterAnimatedIconPreview.PREVIEW_NAME.equals(c.getName()))preview=(JLabel)c;if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
  assertNotNull(presets);assertEquals(14,presets.getItemCount());assertNotNull(preview);assertTrue(preview.getAccessibleContext().getAccessibleName().contains("play_pause"));assertEquals(2,modes.getItemCount());
  var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;presets.setSelectedItem("menu_close");assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("menu_close")),draft.validatedDraftValue());assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
  for(String name:List.of("icon","progress")){var required=TooltipPropertyContractTest.cell(node,name);assertThrows(IllegalArgumentException.class,()->required.setValue(FlutterPropertyCellValue.unset()));}
 }
 @Test void everyPreviewIsReviewedSvgInBothThemes()throws Exception{
  for(String name:AnimatedIconWidgetPropertySchema.ICONS)for(String suffix:List.of(".svg","_dark.svg")){
   try(var in=getClass().getClassLoader().getResourceAsStream(FlutterAnimatedIconPreview.ROOT+name+suffix)){assertNotNull(in);var svg=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(svg.contains("viewBox=\"0 0 144 48\""));assertEquals(3,svg.split("<g transform=", -1).length-1);assertFalse(svg.contains("<text"));assertFalse(svg.contains("NaN"));assertFalse(svg.contains("Infinity"));}
  }
 }
}
