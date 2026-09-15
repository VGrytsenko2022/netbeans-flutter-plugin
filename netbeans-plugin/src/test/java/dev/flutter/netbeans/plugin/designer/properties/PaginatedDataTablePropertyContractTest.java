package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class PaginatedDataTablePropertyContractTest {
    @Test void everyNativePropertyIsWritableAndRefreshPreservesRows() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(PaginatedDataTableWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(definition,StableId.random());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);
        var sets=node.getPropertySets();
        for(var property:definition.properties()) {
            var row=TooltipPropertyContractTest.cell(node,property.name().value());
            assertTrue(row.canWrite());var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor());
        }
        assertTrue(commands.isEmpty());
        node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
        assertArrayEquals(sets,node.getPropertySets());
        var editor=new FlutterDataTableGridPropertyEditor(widget);
        assertEquals("Source-owned rows; 2 columns",editor.getAsText());
        var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
        editor.attachEnv(env);editor.getCustomEditor();assertNull(editor.getValue());
        env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
        var command=assertInstanceOf(EditDataTableGrid.class,editor.getValue());
        assertEquals(TableGrid.Operation.ADD_COLUMN,command.operation());
        var changed=DataTableGrid.edit(widget,command.operation(),command.index(),command.destination(),command.seed());
        assertFalse(changed.slots().containsKey(DataTableGrid.ROWS));
        assertEquals(3,DataTableGrid.children(changed,DataTableGrid.COLUMNS).size());
    }
}
