package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class TimePickerDialogPropertyContractTest {
    @Test void everyNativePropertyHasAStableWritableEditorAndDateValidation() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(TimePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(definition,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);var sets=node.getPropertySets();
        for(var p:definition.properties()){var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),p.name().value());
            var editor=row.getPropertyEditor();assertNotNull(editor,p.name().value());editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());}
        assertTrue(commands.isEmpty());node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
        var binding=FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("initialTime")).orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.TIME_REFERENCE,binding.editorKind());
        assertThrows(IllegalArgumentException.class,()->binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("24:00"))));
        assertThrows(IllegalArgumentException.class,()->binding.validate(FlutterPropertyCellValue.unset()));
    }
    @Test void hourMinuteDraftIsValidatedWithoutPublishingOrLosingInactiveEdits() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var d=BuiltInWidgetCatalog.getDefault().find(TimePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
            var b=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("initialTime")).orElseThrow()).orElseThrow();
            var original=FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("09:00"));
            var editor=b.createEditor();editor.setValue(original);
            var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
            var panel=(FlutterPropertyEditorComponents.CommitOnValidPanel)FlutterPropertyEditorComponents.customEditor(editor,b,env);
            var mode=find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME,javax.swing.JComboBox.class);
            assertEquals(2,mode.getItemCount());assertEquals("Time of day (HH:mm)",mode.getSelectedItem());
            var hour=find(panel,"flutter.timeOfDay.hour",javax.swing.JTextField.class);
            var minute=find(panel,"flutter.timeOfDay.minute",javax.swing.JTextField.class);
            assertNotNull(hour);assertNotNull(minute);
            hour.setText("24");assertFalse(panel.prepareCommit());hour.setText("23");
            minute.setText("60");assertFalse(panel.prepareCommit());minute.setText("59");
            assertTrue(panel.prepareCommit());assertEquals(new PropertyValue.StringValue("23:59"),panel.validatedDraftValue().explicitValue().orElseThrow());
            mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
            mode.setSelectedItem("Time of day (HH:mm)");
            assertEquals("23",hour.getText());assertEquals("59",minute.getText());
            hour.setText("0");minute.setText("7");assertTrue(panel.prepareCommit());
            assertEquals(new PropertyValue.StringValue("00:07"),panel.validatedDraftValue().explicitValue().orElseThrow());
            for(String bad:List.of("","-1"," 1","1e1","١")){hour.setText(bad);assertFalse(panel.prepareCommit(),bad);}
            assertEquals(original,editor.getValue(),"Draft validation must never publish the edit");
        });
    }
    private static <T> T find(java.awt.Container c,String name,Class<T> type){
        for(var child:c.getComponents()){
            if(name.equals(child.getName())&&type.isInstance(child))return type.cast(child);
            if(child instanceof java.awt.Container nested){var found=find(nested,name,type);if(found!=null)return found;}
        }
        return null;
    }
}
