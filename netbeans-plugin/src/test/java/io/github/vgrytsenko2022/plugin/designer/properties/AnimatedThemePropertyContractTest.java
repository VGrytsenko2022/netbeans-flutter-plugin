package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedThemePropertyContractTest {
 @Test void fourStableRowsReopenAndRequiredDataHasNoOmission()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(AnimatedThemeWidgetPropertySchema.TYPE).orElseThrow();
  var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var sets=node.getPropertySets();
  for(var field:AnimatedThemeWidgetPropertySchema.FIELDS){
   var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite());assertEquals(!field.name().equals("data"),row.supportsDefaultValue());
   var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
   var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_binding",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
   row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(w.id(),new PropertyName(field.name()),ref),commands.removeFirst());
   var p=new LinkedHashMap<>(w.properties());p.put(new PropertyName(field.name()),ref);w=new WidgetNode(w.id(),w.type(),p,w.slots());
   node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,field.name()));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,field.name()).getValue());
  }
 }
 @Test void presetsAndDurationOmissionAreAvailableWithoutPermittingNull()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(AnimatedThemeWidgetPropertySchema.TYPE).orElseThrow();var w=WidgetNodePrototypeFactory.create(d,StableId.random());
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,ignored->{});
  for(var entry:Map.of("data",6,"curve",43).entrySet()){
   var row=TooltipPropertyContractTest.cell(node,entry.getKey());var editor=row.getPropertyEditor();editor.setValue(row.getValue());
   var combo=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
   assertNotNull(combo);assertEquals(entry.getValue(),combo.getItemCount());assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
  }
  var duration=TooltipPropertyContractTest.cell(node,"durationUs");assertTrue(duration.supportsDefaultValue());
  var editor=duration.getPropertyEditor();editor.setValue(duration.getValue());
  var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterDurationReferenceEditorComponent.MODE_NAME);
  var items=new ArrayList<>();for(int i=0;i<modes.getItemCount();i++)items.add(modes.getItemAt(i));
  assertTrue(items.contains(FlutterDurationReferenceEditorComponent.OMIT));assertFalse(items.contains(FlutterDurationReferenceEditorComponent.NULL));
 }
}
