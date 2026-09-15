package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class DateRangePickerDialogPropertyContractTest {
    @Test void everyNativePropertyHasAStableWritableEditorAndDateValidation() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(DateRangePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(definition,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);var sets=node.getPropertySets();
        for(var p:definition.properties()){var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),p.name().value());
            var editor=row.getPropertyEditor();assertNotNull(editor,p.name().value());editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());}
        assertTrue(commands.isEmpty());node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
        var binding=FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("firstDate")).orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DATE_REFERENCE,binding.editorKind());
        assertThrows(IllegalArgumentException.class,()->binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("2025-02-29"))));
        assertThrows(IllegalArgumentException.class,()->binding.validate(FlutterPropertyCellValue.unset()));
    }
    @Test void rangeEditorStagesTwoAccessibleEndpointsAndRejectsReversedDates() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            var d=BuiltInWidgetCatalog.getDefault().find(DateRangePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
            var b=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("initialDateRange")).orElseThrow()).orElseThrow();
            assertEquals(FlutterTypedPropertyEditors.EditorKind.DATE_RANGE_REFERENCE,b.editorKind());
            var original=FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("2024-02-29/2024-03-02"));
            var e=b.createEditor();e.setValue(original);
            var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
            var panel=(FlutterPropertyEditorComponents.CommitOnValidPanel)FlutterPropertyEditorComponents.customEditor(e,b,env);
            var start=find(panel,"flutter.dateRange.start",javax.swing.JTextField.class);
            var end=find(panel,"flutter.dateRange.end",javax.swing.JTextField.class);
            assertEquals("2024-02-29",start.getText());assertEquals("2024-03-02",end.getText());
            assertNotNull(start.getAccessibleContext().getAccessibleName());
            start.setText("2024-03-03");assertFalse(panel.prepareCommit());
            assertEquals(original,e.getValue(),"Invalid draft cannot publish");
            end.setText("2024-03-04");assertTrue(panel.prepareCommit());
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("2024-03-03/2024-03-04")),panel.validatedDraftValue());
            assertEquals(original,e.getValue(),"Cancel leaves the owning editor unchanged");
            var mode=find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME,javax.swing.JComboBox.class);
            mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);assertTrue(panel.prepareCommit());
            assertInstanceOf(PropertyValue.NullValue.class,panel.validatedDraftValue().explicitValue().orElseThrow());
            mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.OMIT);assertTrue(panel.prepareCommit());
            assertTrue(panel.validatedDraftValue().explicitValue().isEmpty());
        });
    }
    private static <T> T find(java.awt.Container c,String name,Class<T> type) {
        for(var child:c.getComponents()) {
            if(name.equals(child.getName())&&type.isInstance(child))return type.cast(child);
            if(child instanceof java.awt.Container nested){var found=find(nested,name,type);if(found!=null)return found;}
        }
        return null;
    }
}
