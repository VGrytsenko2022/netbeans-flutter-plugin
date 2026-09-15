package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class DatePickerDialogPropertyContractTest {
    @Test void everyNativePropertyHasAStableWritableEditorAndDateValidation() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(DatePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
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
}
