package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class TablePropertyContractTest {
    @Test void existingLargeTablesStillOpenForRemovalAndReorder() {
        var d=BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.TYPE).orElseThrow();
        var table=WidgetNodePrototypeFactory.create(d,StableId.random());
        var rows=new ArrayList<WidgetNode>();
        for(int i=0;i<1001;i++) rows.add(TableGrid.starterRow(StableId.random(),1));
        var editor=new FlutterTableGridPropertyEditor(TableGrid.withChildren(table,rows));
        assertNotNull(editor.getCustomEditor());
        assertNull(editor.getValue(),"The default Add row draft must be rejected above the editor addition limit");
    }

    @Test void everyPropertyHasWritableTypedCancelSafeEditor() throws Exception {
        for(var type:List.of(TableWidgetPropertySchema.TYPE,TableWidgetPropertySchema.ROW,TableWidgetPropertySchema.CELL)) {
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
            var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            var sets=node.getPropertySets();
            for(var p:d.properties()) {
                var row=TooltipPropertyContractTest.cell(node,p.name().value());
                assertTrue(row.canWrite(),p.name().value());
                var editor=row.getPropertyEditor();editor.setValue(row.getValue());
                assertNotNull(editor,p.name().value());
                if(editor.supportsCustomEditor()) assertNotNull(editor.getCustomEditor(),p.name().value());
            }
            assertTrue(commands.isEmpty(),"Opening/cancelling editors must not mutate the model");
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertArrayEquals(sets,node.getPropertySets());
        }
    }
    @Test void widthsAndKeyAcceptOnlyTheirClosedLocalOrTypedSourceUnion() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"defaultColumnWidth");
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("arbitraryCode()"))));
        row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("max(fixed(40),intrinsic(2))")));
        assertInstanceOf(SetProperty.class,commands.removeFirst());
        var key=TooltipPropertyContractTest.cell(node,"key");
        key.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("table")));
        assertInstanceOf(SetProperty.class,commands.removeFirst());
    }
    @Test void borderModeSwitchClearsOnlyInactiveLeavesAtomically() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.TYPE).orElseThrow();
        var original=WidgetNodePrototypeFactory.create(d,StableId.random());
        var values=Map.<PropertyName,PropertyValue>of(new PropertyName("border"),new PropertyValue.StringValue("all"),
                new PropertyName("borderAllColor"),new PropertyValue.ColorValue(0xff112233L));
        var widget=new WidgetNode(original.id(),original.type(),values,original.slots());
        var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        TooltipPropertyContractTest.cell(node,"border").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("custom")));
        var patch=assertInstanceOf(PatchProperties.class,commands.removeFirst());
        assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("borderAllColor"))));
        assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName("border"),new PropertyValue.StringValue("custom"))));
    }
    @Test void gridEditorStagesNoCommandUntilConfirmedAndRetainsSnapshotFence() {
        var d=BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());
        var editor=new FlutterTableGridPropertyEditor(widget);
        assertEquals("2 rows × 2 columns",editor.getAsText());
        var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
        editor.attachEnv(env);
        var panel=(javax.swing.JPanel)editor.getCustomEditor();
        assertNull(editor.getValue(),"Opening or cancelling a grid editor must not publish a command");
        var spinner=Arrays.stream(panel.getComponents()).filter(c->"flutter.tableGrid.index".equals(c.getName()))
                .map(javax.swing.JSpinner.class::cast).findFirst().orElseThrow();
        ((javax.swing.JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setText("1");
        env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
        assertSame(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID,env.getState());
        var command=assertInstanceOf(EditTableGrid.class,editor.getValue());
        assertEquals(1,command.index(),"OK must commit spinner text, not its previous value");
        assertSame(widget,command.expectedTable());
        assertEquals(3,TableGrid.children(TableGrid.edit(widget,command.operation(),command.index(),command.destination(),command.seed())).size());
        assertEquals(2,TableGrid.children(widget).size());
    }
}
