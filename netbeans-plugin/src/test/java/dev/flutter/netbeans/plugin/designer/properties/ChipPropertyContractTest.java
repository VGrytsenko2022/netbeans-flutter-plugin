package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class ChipPropertyContractTest {
    private static final WidgetDefinition DEF=BuiltInWidgetCatalog.getDefault().find(ChipWidgetPropertySchema.TYPE).orElseThrow();
    @Test void all170RowsAreWritableResettableAndKeepStableSheetIdentity()throws Exception{
        var w=WidgetNodePrototypeFactory.create(DEF,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);var sets=node.getPropertySets();
        assertEquals(170,DEF.properties().size());
        for(var p:DEF.properties()){
            var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),"writable: "+p.name());assertTrue(row.supportsDefaultValue(),"reset: "+p.name());
            var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
            if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
        }
        assertTrue(commands.isEmpty(),commands.toString());node.refreshPresentation(w,DEF,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
    }
    @Test void everyStyleLeafReplacesWholeStyleAtomicallyAndPreservesOtherProperties()throws Exception {
        for(String name:ChipWidgetPropertySchema.textLeaves()){
            var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());var props=new LinkedHashMap<>(seed.properties());
            props.put(new PropertyName("labelStyle"),new PropertyValue.NullValue());
            props.put(new PropertyName("autofocus"),new PropertyValue.BooleanValue(true));
            var w=new WidgetNode(seed.id(),seed.type(),props,seed.slots());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);
            var value=BadgePropertyContractTest.value("textStyle"+name.substring("labelStyle".length()));
            TooltipPropertyContractTest.cell(node,name).setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(1,commands.size());var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("labelStyle"))));
            assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName(name),value)));
            assertFalse(patch.patches().stream().anyMatch(p->p.propertyName().value().equals("autofocus")));
        }
    }
    @Test void nullableShadowEditorPreservesNullEmptyAndOmissionWithoutEagerCommit()throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var b=FlutterTypedPropertyEditors.binding(DEF.property(new PropertyName("iconThemeShadows")).orElseThrow()).orElseThrow();
            var initial=FlutterPropertyCellValue.explicit(new PropertyValue.NullValue());
            var editor=b.createEditor();editor.setValue(initial);
            var env=org.openide.explorer.propertysheet.PropertyEnv.create(new java.beans.FeatureDescriptor());
            var panel=(FlutterPropertyEditorComponents.CommitOnValidPanel)FlutterPropertyEditorComponents.customEditor(editor,b,env);
            var nil=find(panel,FlutterComplexPropertyEditorComponents.SHADOW_TABLE_NAME+".null",javax.swing.JCheckBox.class);
            assertNotNull(nil);assertTrue(nil.isSelected());assertTrue(panel.prepareCommit());assertEquals(initial,panel.validatedDraftValue());
            nil.doClick();assertTrue(panel.prepareCommit());assertEquals(new PropertyValue.ShadowListValue(List.of()),panel.validatedDraftValue().explicitValue().orElseThrow());
            assertEquals(initial,editor.getValue());nil.doClick();assertTrue(panel.prepareCommit());assertEquals(initial,panel.validatedDraftValue());
            var omit=find(panel,"Use inherited/default value (omit argument)",javax.swing.JCheckBox.class);
            assertNotNull(omit);omit.doClick();assertTrue(panel.prepareCommit());assertEquals(FlutterPropertyCellValue.unset(),panel.validatedDraftValue());
            assertEquals(initial,editor.getValue());
        });
    }
    private static <T> T find(java.awt.Container c,String name,Class<T> type){
        for(var child:c.getComponents()){
            if((name.equals(child.getName())||child instanceof javax.swing.JCheckBox check&&name.equals(check.getText()))&&type.isInstance(child))return type.cast(child);
            if(child instanceof java.awt.Container nested){var found=find(nested,name,type);if(found!=null)return found;}
        }
        return null;
    }
    @Test void nestedAnimationAndIconFamiliesSwitchAtomically()throws Exception {
        for(var entry:Map.of("chipAnimationStyleEnableAnimationDurationUs",new PropertyValue.IntegerValue(java.math.BigInteger.TEN),
                "iconThemeSize",new PropertyValue.DoubleValue(java.math.BigDecimal.TEN)).entrySet()){
            var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());var props=new LinkedHashMap<>(seed.properties());
            String parent=entry.getKey().startsWith("icon")?"iconTheme":"chipAnimationStyle";
            props.put(new PropertyName(parent),new PropertyValue.NullValue());
            if(parent.equals("chipAnimationStyle"))props.put(new PropertyName("chipAnimationStyleEnableAnimation"),new PropertyValue.NullValue());
            var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,new WidgetNode(seed.id(),seed.type(),props,seed.slots()),DEF,commands::add);
            TooltipPropertyContractTest.cell(node,entry.getKey()).setValue(FlutterPropertyCellValue.explicit(entry.getValue()));
            assertEquals(1,commands.size());var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName(parent))));
            if(parent.equals("chipAnimationStyle"))assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("chipAnimationStyleEnableAnimation"))));
        }
    }
    @Test void sideStateEditsEnableStatefulAndCursorRequiresExplicitDefault()throws Exception {
        var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());
        var widget=new WidgetNode(seed.id(),seed.type(),Map.of(new PropertyName("side"),new PropertyValue.NullValue()),seed.slots());
        var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,DEF,commands::add);
        TooltipPropertyContractTest.cell(node,"sideHoveredWidth").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.TWO)));
        var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());
        assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("side"))));
        assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName("sideStateful"),new PropertyValue.BooleanValue(true))));
        assertTrue(patch.patches().contains(new PatchProperties.SetPatch(new PropertyName("sideHoveredMode"),new PropertyValue.StringValue("border"))));
        commands.clear();
        assertThrows(IllegalArgumentException.class,()->TooltipPropertyContractTest.cell(node,"mouseCursorHovered").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("click"))));
        assertTrue(commands.isEmpty());
    }
    @Test void wholeStyleClearsAllLocalLeavesInOneCommand()throws Exception {
        var seed=WidgetNodePrototypeFactory.create(DEF,StableId.random());var props=new LinkedHashMap<>(seed.properties());
        for(String name:ChipWidgetPropertySchema.textLeaves())
            props.put(new PropertyName(name),BadgePropertyContractTest.value("textStyle"+name.substring("labelStyle".length())));
        var w=new WidgetNode(seed.id(),seed.type(),props,seed.slots());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,DEF,commands::add);
        TooltipPropertyContractTest.cell(node,"labelStyle").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        var patch=assertInstanceOf(PatchProperties.class,commands.getFirst());assertEquals(32,patch.patches().size());
    }
}
