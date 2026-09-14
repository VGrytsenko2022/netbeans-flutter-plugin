package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedSwitcherPropertyContractTest {
    @Test void allSixRowsHaveTypedEditorsAndStableReopenPresentation() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSwitcherWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var sets=node.getPropertySets();
        for(var field:AnimatedSwitcherWidgetPropertySchema.FIELDS){
            var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite());assertEquals(!field.name().equals("durationUs"),row.supportsDefaultValue());
            var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_binding",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName(field.name()),ref),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName(field.name()),ref);widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());assertSame(row,TooltipPropertyContractTest.cell(node,field.name()));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,field.name()).getValue());
        }
    }
    @Test void bothCurvesExposeAllPresetsAndNullIsOnlyAllowedForReverseDuration() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSwitcherWidgetPropertySchema.TYPE).orElseThrow();
        var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
        for(String name:List.of("switchInCurve","switchOutCurve")){
            var row=TooltipPropertyContractTest.cell(node,name);var editor=row.getPropertyEditor();editor.setValue(row.getValue());
            var presets=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
            assertNotNull(presets);assertEquals(43,presets.getItemCount());
            for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()){
                row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(curve)));
                assertEquals(new SetProperty(w.id(),new PropertyName(name),new PropertyValue.StringValue(curve)),commands.removeFirst());
            }
        }
        for(var f:AnimatedSwitcherWidgetPropertySchema.FIELDS){
            var row=TooltipPropertyContractTest.cell(node,f.name());
            if(f.name().equals("reverseDurationUs"))row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            else assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        }
    }
}
