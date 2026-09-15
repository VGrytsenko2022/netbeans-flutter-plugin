package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class DataTablePropertyContractTest {
    @Test void allRowsHaveWritableEditorsAndOpeningDoesNotCommitOrRecreateSheet() throws Exception {
        for(var type:DataTableWidgetPropertySchema.TYPES) {
            var definition=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var widget=WidgetNodePrototypeFactory.create(definition,StableId.random());
            var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);
            var sets=node.getPropertySets();
            for(var field:definition.properties()) {
                var row=TooltipPropertyContractTest.cell(node,field.name().value());
                assertTrue(row.canWrite(),type+"."+field.name());
                var editor=row.getPropertyEditor();assertNotNull(editor,field.name().value());editor.setValue(row.getValue());
                if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),type+"."+field.name());
            }
            assertTrue(commands.isEmpty(),"Editor drafts cannot mutate saved values");
            node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertArrayEquals(sets,node.getPropertySets());
        }
    }
    @Test void localStateAndStylesSwitchSourceAtomically() throws Exception {
        var definition=BuiltInWidgetCatalog.getDefault().find(DataTableWidgetPropertySchema.TYPE).orElseThrow();
        var original=WidgetNodePrototypeFactory.create(definition,StableId.random());
        for(String family:List.of("dataRowColor","headingRowColor","dataTextStyle","headingTextStyle")) {
            var p=new LinkedHashMap<PropertyName,PropertyValue>();
            p.put(new PropertyName(family),new PropertyValue.NullValue());
            var widget=new WidgetNode(original.id(),original.type(),p,original.slots());
            var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,definition,commands::add);
            String leaf=family.endsWith("Style")?family+"FontSize":family+"Default";
            var value=family.endsWith("Style")?(PropertyValue)new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(16)):new PropertyValue.ColorValue(0xff123456L);
            TooltipPropertyContractTest.cell(node,leaf).setValue(FlutterPropertyCellValue.explicit(value));
            var patch=assertInstanceOf(PatchProperties.class,commands.removeFirst());
            assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName(family),new PropertyValue.StringValue("local"))));
            p.put(new PropertyName(family),new PropertyValue.StringValue("local"));p.put(new PropertyName(leaf),value);
            widget=new WidgetNode(original.id(),original.type(),p,original.slots());
            node.refreshPresentation(widget,definition,commands::add,null,null,FlutterImageAssetChoices.empty());
            TooltipPropertyContractTest.cell(node,family).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            patch=assertInstanceOf(PatchProperties.class,commands.removeFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName(leaf))));
        }
    }
    @Test void gridDraftIsCancelSafeAndSnapshotFenced() {
        var widget=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(DataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var editor=new FlutterDataTableGridPropertyEditor(widget);
        assertEquals("2 rows × 2 columns",editor.getAsText());
        var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
        editor.attachEnv(env);editor.getCustomEditor();assertNull(editor.getValue());
        env.setState(org.openide.explorer.propertysheet.PropertyEnv.STATE_VALID);
        var command=assertInstanceOf(EditDataTableGrid.class,editor.getValue());
        assertSame(widget,command.expectedTable());
        assertEquals(3,DataTableGrid.children(DataTableGrid.edit(widget,command.operation(),command.index(),command.destination(),command.seed()),DataTableGrid.ROWS).size());
    }
}
