package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class DialogPropertyContractTest {
    @Test void all39RowsAreWritableStableAndExposeOnlyTheirConstructorFields()throws Exception{
        for(boolean full:List.of(false,true)){
            var d=BuiltInWidgetCatalog.getDefault().find(full?DialogWidgetPropertySchema.FULLSCREEN_TYPE:DialogWidgetPropertySchema.TYPE).orElseThrow();
            var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var sets=node.getPropertySets();
            for(var p:d.properties()){
                var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());
                var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
                if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
            }
            assertTrue(commands.isEmpty());node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
            assertNotNull(TooltipPropertyContractTest.cell(node,"insetAnimationCurve").getPropertyEditor().getCustomEditor());
            if(!full)assertEquals(CardWidgetPropertySchema.shapeKinds(),List.of(TooltipPropertyContractTest.cell(node,"shapeKind").getPropertyEditor().getTags()).subList(1,11));
        }
    }
    @Test void all100ShapeSwitchesAreAtomicAndPreserveUnrelatedProperties()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(DialogWidgetPropertySchema.TYPE).orElseThrow();
        for(String from:CardWidgetPropertySchema.shapeKinds())for(String to:CardWidgetPropertySchema.shapeKinds()){
            var values=new LinkedHashMap<PropertyName,PropertyValue>();values.put(new PropertyName("shapeKind"),new PropertyValue.StringValue(from));
            values.put(new PropertyName("backgroundColor"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.surface")));
            for(String n:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(n)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(n,from))values.put(new PropertyName(n),CardPropertyContractTest.value(n));
            var w=new WidgetNode(StableId.random(),d.typeId(),values,Map.of());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
            TooltipPropertyContractTest.cell(node,"shapeKind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(to)));
            if(from.equals(to)){assertTrue(commands.isEmpty());continue;}
            assertEquals(1,commands.size());var after=apply(w,commands.getFirst());assertEquals(new PropertyValue.StringValue(to),after.properties().get(new PropertyName("shapeKind")));
            for(var entry:values.entrySet()){
                String name=entry.getKey().value();if(name.equals("shapeKind"))continue;
                if(!name.startsWith("shape")||CardWidgetPropertySchema.shapePropertyAppliesToKind(name,to))assertEquals(entry.getValue(),after.properties().get(entry.getKey()));
                else assertFalse(after.properties().containsKey(entry.getKey()),name);
            }
            commands.clear();TooltipPropertyContractTest.cell(node,"shape").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            assertEquals(1,commands.size());var nulled=apply(w,commands.getFirst());assertTrue(nulled.properties().containsKey(new PropertyName("backgroundColor")));
            assertTrue(CardWidgetPropertySchema.builtInShapePropertyNames().stream().noneMatch(n->nulled.properties().containsKey(new PropertyName(n))));
            assertEquals(new PropertyValue.NullValue(),nulled.properties().get(new PropertyName("shape")));
        }
    }
    @Test void nonnullableDurationsAndPhysicalInsetsRejectWrongDrafts(){
        var d=BuiltInWidgetCatalog.getDefault().find(DialogWidgetPropertySchema.TYPE).orElseThrow();
        var inset=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("insetPadding")).orElseThrow()).orElseThrow();
        assertDoesNotThrow(()->inset.validate(FlutterPropertyCellValue.unset()));assertDoesNotThrow(()->inset.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class,()->inset.validate(FlutterPropertyCellValue.explicit(new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE))));
        var duration=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("insetAnimationDurationUs")).orElseThrow()).orElseThrow();
        assertThrows(IllegalArgumentException.class,()->duration.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    private static WidgetNode apply(WidgetNode w,DesignerCommand command){
        var p=new LinkedHashMap<>(w.properties());var patches=command instanceof PatchProperties all?all.patches():command instanceof SetProperty set?List.<PatchProperties.Patch>of(new PatchProperties.SetPatch(set.propertyName(),set.value())):List.<PatchProperties.Patch>of(new PatchProperties.ResetPatch(((ResetProperty)command).propertyName()));
        for(var patch:patches)if(patch instanceof PatchProperties.SetPatch set)p.put(set.propertyName(),set.value());else p.remove(patch.propertyName());
        return new WidgetNode(w.id(),w.type(),p,w.slots());
    }
}
