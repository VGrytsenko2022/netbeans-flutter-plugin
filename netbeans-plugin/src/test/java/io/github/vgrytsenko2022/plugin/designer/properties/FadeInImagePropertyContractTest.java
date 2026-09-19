package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.command.*;
import java.util.*;
import javax.swing.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FadeInImagePropertyContractTest {
 @Test void all23RowsWritableStableReopenAndIndependentImageDrafts()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow();
  var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
  for(var field:d.properties()){
   String name=field.name().value();var row=TooltipPropertyContractTest.cell(node,name);
   assertTrue(row.canWrite(),name);assertEquals(!Set.of("placeholder","image").contains(name),row.supportsDefaultValue(),name);
   PropertyValue value=switch(name){
    case "placeholder","image"->PropertyValue.ImageProviderValue.unresolved();
    case "excludeFromSemantics","matchTextDirection"->new PropertyValue.BooleanValue(true);
    case "fadeOutDurationUs","fadeInDurationUs"->new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(1000));
    case "fadeOutCurve","fadeInCurve"->new PropertyValue.StringValue("easeIn");
    case "alignment"->new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,java.math.BigDecimal.ONE,java.math.BigDecimal.ZERO);
    case "repeat"->new PropertyValue.EnumValue("ImageRepeat","repeatX");
    case "filterQuality"->new PropertyValue.EnumValue("FilterQuality","high");
    default->new PropertyValue.NullValue();
   };
   var previous=row.getValue();row.setValue(FlutterPropertyCellValue.explicit(value));
   if(previous.equals(FlutterPropertyCellValue.explicit(value)))assertTrue(commands.isEmpty());else assertEquals(new SetProperty(widget.id(),field.name(),value),commands.removeFirst());
   var p=new LinkedHashMap<>(widget.properties());p.put(field.name(),value);widget=new WidgetNode(widget.id(),widget.type(),p,Map.of());
   node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
   assertSame(row,TooltipPropertyContractTest.cell(node,name));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
   var editor=row.getPropertyEditor();editor.setValue(row.getValue());
   if(name.endsWith("Curve")){
    var panel=editor.getCustomEditor();var presets=find(panel,FlutterPresetDartReferenceEditorComponent.PRESET_NAME,JComboBox.class);assertEquals(43,presets.getItemCount());
   }
  }
  for(String name:List.of("placeholder","image")){
   var row=TooltipPropertyContractTest.cell(node,name);var editor=row.getPropertyEditor();editor.setValue(row.getValue());
   var panel=(FlutterPropertyEditorComponents.CommitOnValidPanel)editor.getCustomEditor();
   var modes=find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME,JComboBox.class);
   assertEquals(2,modes.getItemCount());assertEquals(FlutterLocalDartReferenceEditorComponent.IMAGE,modes.getSelectedItem());
   assertEquals(row.getValue(),panel.validatedDraftValue());
   modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
   assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
   modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.IMAGE);assertEquals(row.getValue(),panel.validatedDraftValue());
   assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
  }
 }
 private static <T> T find(java.awt.Component component,String name,Class<T> type){
  var queue=new ArrayDeque<java.awt.Component>();queue.add(component);
  while(!queue.isEmpty()){var c=queue.removeFirst();if(name.equals(c.getName()))return type.cast(c);if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
  throw new AssertionError("Missing "+name);
 }
}
