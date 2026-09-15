package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AlertDialogPropertyContractTest {
    @Test void all218RowsAreWritableStableAndExposeOnlyTheirConstructorFields()throws Exception{
        for(boolean full:List.of(false,true)){
            var d=BuiltInWidgetCatalog.getDefault().find(full?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
            var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);var sets=node.getPropertySets();
            for(var p:d.properties()){
                var row=TooltipPropertyContractTest.cell(node,p.name().value());assertTrue(row.canWrite(),p.name().value());assertTrue(row.supportsDefaultValue(),p.name().value());
                var editor=row.getPropertyEditor();assertNotNull(editor);editor.setValue(row.getValue());
                if(editor.supportsCustomEditor())assertNotNull(editor.getCustomEditor(),p.name().value());
            }
            assertTrue(commands.isEmpty());node.refreshPresentation(w,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertArrayEquals(sets,node.getPropertySets());
            if(full)assertNotNull(TooltipPropertyContractTest.cell(node,"insetAnimationCurve").getPropertyEditor().getCustomEditor());
            assertEquals(CardWidgetPropertySchema.shapeKinds(),List.of(TooltipPropertyContractTest.cell(node,"shapeKind").getPropertyEditor().getTags()).subList(1,11));
        }
    }
    @Test void all100ShapeSwitchesAreAtomicAndPreserveUnrelatedProperties()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
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
        var d=BuiltInWidgetCatalog.getDefault().find(AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
        var inset=FlutterTypedPropertyEditors.binding(d.property(new PropertyName("insetPadding")).orElseThrow()).orElseThrow();
        assertDoesNotThrow(()->inset.validate(FlutterPropertyCellValue.unset()));assertDoesNotThrow(()->inset.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class,()->inset.validate(FlutterPropertyCellValue.explicit(new PropertyValue.EdgeInsetsDirectionalValue(java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE))));
        var duration=FlutterTypedPropertyEditors.binding(BuiltInWidgetCatalog.getDefault().find(AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE).orElseThrow().property(new PropertyName("insetAnimationDurationUs")).orElseThrow()).orElseThrow();
        assertThrows(IllegalArgumentException.class,()->duration.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void styleSwitchClearsWholeAndOnlyTheMutuallyExclusiveLocalFields()throws Exception{
        for(boolean adaptive:List.of(false,true))for(String family:AlertDialogWidgetPropertySchema.styleFamilies()){
            var d=BuiltInWidgetCatalog.getDefault().find(adaptive?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
            var p=new LinkedHashMap<PropertyName,PropertyValue>();p.put(new PropertyName(family+"FontSize"),new PropertyValue.DoubleValue(java.math.BigDecimal.TEN));
            p.put(new PropertyName("scrollable"),new PropertyValue.BooleanValue(true));
            var w=new WidgetNode(StableId.random(),d.typeId(),p,Map.of());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
            TooltipPropertyContractTest.cell(node,family).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            var after=apply(w,commands.getFirst());assertFalse(after.properties().containsKey(new PropertyName(family+"FontSize")));
            assertTrue(after.properties().containsKey(new PropertyName("scrollable")));assertTrue(after.properties().get(new PropertyName(family)) instanceof PropertyValue.NullValue);
            commands.clear();node.refreshPresentation(after,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            TooltipPropertyContractTest.cell(node,family+"FontSize").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.TEN)));
            var local=apply(after,commands.getFirst());assertFalse(local.properties().containsKey(new PropertyName(family)));
            assertEquals(0,java.math.BigDecimal.TEN.compareTo(((PropertyValue.DoubleValue)local.properties().get(new PropertyName(family+"FontSize"))).value()));
        }
    }
    @Test void everyLocalStyleLeafUsesItsTypedEditorAndGeneratesACommand()throws Exception {
        for(boolean adaptive:List.of(false,true))for(String family:AlertDialogWidgetPropertySchema.styleFamilies()) {
            var d=BuiltInWidgetCatalog.getDefault().find(adaptive?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow();
            for(String name:AlertDialogWidgetPropertySchema.localStyleProperties(family)) {
                var values=new LinkedHashMap<PropertyName,PropertyValue>();
                if(name.endsWith("Package"))values.put(new PropertyName(family+"FontFamily"),new PropertyValue.StringValue("Inter"));
                var widget=new WidgetNode(StableId.random(),d.typeId(),values,Map.of());var commands=new ArrayList<DesignerCommand>();
                var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
                var row=TooltipPropertyContractTest.cell(node,name);
                var value=BadgePropertyContractTest.value("textStyle"+name.substring(family.length()));
                row.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(1,commands.size(),name);
                assertEquals(value,apply(widget,commands.getFirst()).properties().get(new PropertyName(name)),name);
            }
        }
    }
    private static WidgetNode apply(WidgetNode w,DesignerCommand command){
        var p=new LinkedHashMap<>(w.properties());var patches=command instanceof PatchProperties all?all.patches():command instanceof SetProperty set?List.<PatchProperties.Patch>of(new PatchProperties.SetPatch(set.propertyName(),set.value())):List.<PatchProperties.Patch>of(new PatchProperties.ResetPatch(((ResetProperty)command).propertyName()));
        for(var patch:patches)if(patch instanceof PatchProperties.SetPatch set)p.put(set.propertyName(),set.value());else p.remove(patch.propertyName());
        return new WidgetNode(w.id(),w.type(),p,w.slots());
    }
}
