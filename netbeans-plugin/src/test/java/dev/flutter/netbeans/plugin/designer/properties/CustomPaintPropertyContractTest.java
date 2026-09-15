package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.command.*;
import java.util.*;
import java.math.*;
import org.openide.nodes.Children;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CustomPaintPropertyContractTest {
 @Test void sizeUnionKeepsDraftsPrivateAndAllowsOmission() throws Exception {
  javax.swing.SwingUtilities.invokeAndWait(() -> {
   try {
    var def=CustomPaintWidgetPropertySchema.properties().stream().filter(p->p.name().value().equals("size")).findFirst().orElseThrow();
    var binding=FlutterTypedPropertyEditors.binding(def).orElseThrow();
    var editor=binding.createEditor(); var original=FlutterPropertyCellValue.explicit(new PropertyValue.SizeValue(BigDecimal.ONE,BigDecimal.TEN));
    editor.setValue(original);
    var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
    ((org.openide.explorer.propertysheet.ExPropertyEditor)editor).attachEnv(env);
    var panel=editor.getCustomEditor();
    var width=ShaderMaskPropertyContractTest.find(panel,javax.swing.JTextField.class,c->"flutter.size.width".equals(c.getName()));
    var height=ShaderMaskPropertyContractTest.find(panel,javax.swing.JTextField.class,c->"flutter.size.height".equals(c.getName()));
    width.setText("48.5");height.setText("32.25");
    assertEquals(original,editor.getValue(),"Cancel/draft must not publish changes");
    env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
    assertEquals(new PropertyValue.SizeValue(new BigDecimal("48.5"),new BigDecimal("32.25")),((FlutterPropertyCellValue)editor.getValue()).explicitValue().orElseThrow());
    var resetEnv=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
    ((org.openide.explorer.propertysheet.ExPropertyEditor)editor).attachEnv(resetEnv);
    var resetPanel=editor.getCustomEditor();
    var mode=ShaderMaskPropertyContractTest.find(resetPanel,javax.swing.JComboBox.class,c->FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()));
    assertEquals(3,mode.getItemCount());
    mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.OMIT);
    resetEnv.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
    assertTrue(((FlutterPropertyCellValue)editor.getValue()).explicitValue().isEmpty());
   } catch(Exception ex){throw new RuntimeException(ex);}
  });
 }
 @Test void allFiveRowsRemainWritableStableAndEditableAfterReopen() throws Exception {
  var d=BuiltInWidgetCatalog.getDefault().find(CustomPaintWidgetPropertySchema.TYPE).orElseThrow();
  var widget=WidgetNodePrototypeFactory.create(d,StableId.random()); var commands=new ArrayList<DesignerCommand>();
  var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add); var sets=node.getPropertySets();
  for(var field:d.properties()){
   String name=field.name().value(); var row=TooltipPropertyContractTest.cell(node,name);
   assertTrue(row.canWrite(),name); assertTrue(row.supportsDefaultValue(),name);
   var binding=FlutterTypedPropertyEditors.binding(field).orElseThrow();
   PropertyValue value=switch(name){
    case "isComplex","willChange"->new PropertyValue.BooleanValue(true);
    case "size"->new PropertyValue.SizeValue(BigDecimal.valueOf(48),BigDecimal.valueOf(32));
    default->new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_painter",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
   };
   row.setValue(FlutterPropertyCellValue.explicit(value));
   assertEquals(new SetProperty(widget.id(),field.name(),value),commands.removeFirst());
   var p=new LinkedHashMap<>(widget.properties()); p.put(field.name(),value);
   widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
   node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
   assertSame(row,TooltipPropertyContractTest.cell(node,name)); assertArrayEquals(sets,node.getPropertySets());
   var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
   assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
   var editor=row.getPropertyEditor(); editor.setValue(row.getValue());
   if(Set.of("painter","foregroundPainter","size").contains(name)) assertNotNull(editor.getCustomEditor());
   if(name.equals("size")) assertEquals(FlutterTypedPropertyEditors.EditorKind.SIZE_REFERENCE,binding.editorKind());
   if(name.equals("isComplex")||name.equals("willChange")) assertTrue(editor.isPaintable());
  }
 }
}
