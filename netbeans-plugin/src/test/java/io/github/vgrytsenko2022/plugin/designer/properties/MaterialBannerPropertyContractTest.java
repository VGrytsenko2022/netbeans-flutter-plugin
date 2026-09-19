package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class MaterialBannerPropertyContractTest {
    private static final WidgetDefinition DEF=BuiltInWidgetCatalog.getDefault().find(MaterialBannerWidgetPropertySchema.TYPE).orElseThrow();
    @Test void all46RowsAreWritableResettableAndKeepStableSheetIdentity()throws Exception{
        var w=WidgetNodePrototypeFactory.create(DEF,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);var sets=node.getPropertySets();
        assertEquals(46,DEF.properties().size());
        for(var p:DEF.properties()){
            var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());
            var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
        }
        assertTrue(commands.isEmpty());node.refreshPresentation(w,DEF,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
    }
    @Test void everyStyleLeafReplacesWholeStyleAtomicallyAndPreservesOtherProperties()throws Exception {
        for(String name:AlertDialogWidgetPropertySchema.localStyleProperties("contentTextStyle")){
            var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());var props=new LinkedHashMap<>(seed.properties());
            props.put(new PropertyName("contentTextStyle"),new PropertyValue.NullValue());
            props.put(new PropertyName("forceActionsBelow"),new PropertyValue.BooleanValue(true));
            var w=new WidgetNode(seed.id(),seed.type(),props,seed.slots());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);
            var value=BadgePropertyContractTest.value("textStyle"+name.substring("contentTextStyle".length()));
            TooltipPropertyContractTest.cell(node,name).setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(1,commands.size());var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("contentTextStyle"))));
            assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName(name),value)));
            assertFalse(patch.patches().stream().anyMatch(p->p.propertyName().value().equals("forceActionsBelow")));
        }
    }
    @Test void wholeStyleClearsAllLocalLeavesInOneCommand()throws Exception {
        var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());var props=new LinkedHashMap<>(seed.properties());
        for(String name:AlertDialogWidgetPropertySchema.localStyleProperties("contentTextStyle"))
            props.put(new PropertyName(name),BadgePropertyContractTest.value("textStyle"+name.substring("contentTextStyle".length())));
        var w=new WidgetNode(seed.id(),seed.type(),props,seed.slots());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);
        TooltipPropertyContractTest.cell(node,"contentTextStyle").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());assertEquals(32,patch.patches().size());
    }
}
