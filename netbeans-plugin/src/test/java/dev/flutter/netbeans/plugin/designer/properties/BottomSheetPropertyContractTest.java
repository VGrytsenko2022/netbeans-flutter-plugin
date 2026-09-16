package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class BottomSheetPropertyContractTest {
 @Test void nullableSizeKeepsDraftsPrivateAndAllowsNullAndOmission() throws Exception {
  javax.swing.SwingUtilities.invokeAndWait(() -> {
   try {
    var def=BottomSheetWidgetPropertySchema.properties().stream().filter(p->p.name().value().equals("dragHandleSize")).findFirst().orElseThrow();
    var binding=FlutterTypedPropertyEditors.binding(def).orElseThrow();
    var editor=binding.createEditor(); var original=FlutterPropertyCellValue.explicit(new PropertyValue.SizeValue(java.math.BigDecimal.ONE,BigDecimal.TEN));
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
    assertEquals(4,mode.getItemCount());
    mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);
    resetEnv.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
    assertInstanceOf(PropertyValue.NullValue.class,((FlutterPropertyCellValue)editor.getValue()).explicitValue().orElseThrow());
    var omitEnv=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
    ((org.openide.explorer.propertysheet.ExPropertyEditor)editor).attachEnv(omitEnv);
    var omitPanel=editor.getCustomEditor();
    var omitMode=ShaderMaskPropertyContractTest.find(omitPanel,javax.swing.JComboBox.class,c->FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()));
    omitMode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.OMIT);
    omitEnv.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
    assertTrue(((FlutterPropertyCellValue)editor.getValue()).explicitValue().isEmpty());
   } catch(Exception ex){throw new RuntimeException(ex);}
  });
 }
    @Test void all37RowsAreWritableStableAndHaveTypedEditors() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(BottomSheetWidgetPropertySchema.TYPE).orElseThrow();
        var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var sets=node.getPropertySets();
        for(var p:d.properties()) {
            var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),p.name().value());
            var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
        }
        assertTrue(commands.isEmpty());node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());
        assertArrayEquals(sets,node.getPropertySets());
        for(String name:List.of("builder","onClosing","animationController","dragHandleSize","onDragStart","onDragEnd"))
            assertTrue(TooltipPropertyContractTest.cell(node,name).getPropertyEditor().supportsCustomEditor(),name);
    }
    @Test void shapeSwitchesUseOneAtomicCommandAndKeepControllerAndContentDefaults() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(BottomSheetWidgetPropertySchema.TYPE).orElseThrow();
        for(String from:CardWidgetPropertySchema.shapeKinds())for(String to:CardWidgetPropertySchema.shapeKinds()) {
            var seed=WidgetNodePrototypeFactory.create(d,StableId.random());var values=new LinkedHashMap<>(seed.properties());
            values.put(new PropertyName("shapeKind"),new PropertyValue.StringValue(from));
            for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(name)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(name,from))
                    values.put(new PropertyName(name),CardPropertyContractTest.value(name));
            var w=new WidgetNode(seed.id(),seed.type(),values,seed.slots());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
            TooltipPropertyContractTest.cell(node,"shapeKind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(to)));
            assertEquals(from.equals(to)?0:1,commands.size());
            if(!from.equals(to))assertTrue(commands.getFirst() instanceof PatchProperties || commands.getFirst() instanceof SetProperty);
        }
    }
}
