package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class InputDatePickerFormFieldPropertyContractTest {
    @Test void everyNativePropertyHasAStableWritableEditorAndDateValidation() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(InputDatePickerFormFieldWidgetPropertySchema.TYPE).orElseThrow();
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
        var initial=FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("initialDate")).orElseThrow()).orElseThrow();
        assertDoesNotThrow(()->initial.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertDoesNotThrow(()->initial.validate(FlutterPropertyCellValue.unset()));
        var changed=FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("onDateSubmitted")).orElseThrow(),Optional.empty(),false,List.of("noop")).orElseThrow();
        assertDoesNotThrow(()->changed.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertDoesNotThrow(()->changed.validate(FlutterPropertyCellValue.unset()));
    }
    @Test void nullableInitialDateHasIndependentLocalDraftAndOmission() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var d=BuiltInWidgetCatalog.getDefault().find(InputDatePickerFormFieldWidgetPropertySchema.TYPE).orElseThrow();
            var b=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("initialDate")).orElseThrow()).orElseThrow();
            var original=FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            var editor=b.createEditor();editor.setValue(original);
            var environment=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
            var panel=(FlutterPropertyEditorComponents.CommitOnValidPanel)FlutterPropertyEditorComponents.customEditor(editor,b,environment);
            var mode=find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME,javax.swing.JComboBox.class);
            assertEquals(FlutterLocalDartReferenceEditorComponent.NULL,mode.getSelectedItem());
            assertTrue(java.util.stream.IntStream.range(0,mode.getItemCount()).anyMatch(i->FlutterLocalDartReferenceEditorComponent.OMIT.equals(mode.getItemAt(i))));
            assertTrue(panel.prepareCommit());assertEquals(original,panel.validatedDraftValue());
            mode.setSelectedItem("Gregorian date (YYYY-MM-DD)");
            assertTrue(panel.prepareCommit());
            assertEquals(new PropertyValue.StringValue("2000-01-01"),panel.validatedDraftValue().explicitValue().orElseThrow());
            assertEquals(original,editor.getValue(),"Switching drafts must not commit");
            mode.setSelectedItem(FlutterLocalDartReferenceEditorComponent.NULL);
            assertTrue(panel.prepareCommit());assertEquals(original,panel.validatedDraftValue());
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
