package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class ThemePropertyContractTest {
 @Test void stableDataRowReopensAndCannotBeOmitted()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(ThemeWidgetPropertySchema.TYPE).orElseThrow();
  var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var sets=node.getPropertySets();
  for(var field:ThemeWidgetPropertySchema.FIELDS){
   var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite());assertEquals(!field.name().equals("data"),row.supportsDefaultValue());
   var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
   var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_binding",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
   row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(w.id(),new PropertyName(field.name()),ref),commands.removeFirst());
   var p=new LinkedHashMap<>(w.properties());p.put(new PropertyName(field.name()),ref);w=new WidgetNode(w.id(),w.type(),p,w.slots());
   node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,field.name()));assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,field.name()).getValue());
  }
 }
 @Test void sixPresetsAreAvailableWithoutNullOrOmission()throws Exception{
  var d=BuiltInWidgetCatalog.getDefault().find(ThemeWidgetPropertySchema.TYPE).orElseThrow();
  var w=WidgetNodePrototypeFactory.create(d,StableId.random());
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,ignored->{});
  var row=TooltipPropertyContractTest.cell(node,"data");var editor=row.getPropertyEditor();editor.setValue(row.getValue());
  var combo=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
  assertNotNull(combo);assertEquals(6,combo.getItemCount());assertFalse(row.supportsDefaultValue());
  assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
 }

}
