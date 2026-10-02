package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedPositionedPropertyContractTest {
    static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new BigDecimal(n));}
    static PropertyValue.DartObjectReferenceValue ref(){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_value",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    @Test void everyRowWritableWithNullableNumbersPresetsAndNoDraftPublication()throws Exception{
        for(var type:AnimatedPositionedWidgetPropertySchema.TYPES){
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            for(var field:AnimatedPositionedWidgetPropertySchema.fields(type)){
                var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite(),field.name());
                var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor);
                if(field.name().equals("rect")||field.name().equals("curve")){
                    var panel=editor.getCustomEditor();
                    var preset=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
                    assertNotNull(preset);assertEquals(field.name().equals("rect")?1:43,preset.getItemCount());
                }
            }
            if(type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE))continue;
            var row=TooltipPropertyContractTest.cell(node,"width");var editor=row.getPropertyEditor();editor.setValue(row.getValue());
            var panel=editor.getCustomEditor();var mode=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
            assertEquals(4,mode.getItemCount());mode.setSelectedIndex(2);
            var input=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
            input.setText("-40.5");var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
            assertEquals(FlutterPropertyCellValue.explicit(number("-40.5")),commit.validatedDraftValue());
            assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
            for(String invalid:List.of("Infinity","NaN","1e309","foo()")){
                input.setText(invalid);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue);
            }
        }
    }
    @Test void eachThirdAxisConstraintChangesAtomicallyWithoutRebuildingRows()throws Exception{
        for(var type:List.of(AnimatedPositionedWidgetPropertySchema.TYPE,AnimatedPositionedWidgetPropertySchema.DIRECTIONAL_TYPE))
        for(var axis:List.of(List.of(AnimatedPositionedWidgetPropertySchema.directional(type)?"start":"left",AnimatedPositionedWidgetPropertySchema.directional(type)?"end":"right","width"),List.of("top","bottom","height")))
        for(String edited:axis){
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();var prototype=WidgetNodePrototypeFactory.create(d,StableId.random());
            var props=new LinkedHashMap<>(prototype.properties());for(String name:axis)if(!name.equals(edited))props.put(new PropertyName(name),number("10"));
            var widget=new WidgetNode(prototype.id(),type,props,prototype.slots());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);var row=TooltipPropertyContractTest.cell(node,edited);
            row.setValue(FlutterPropertyCellValue.explicit(number("20")));
            var change=assertInstanceOf(PatchProperties.class,commands.removeFirst());
            assertEquals(List.of(new PatchProperties.ResetPatch(new PropertyName(edited.equals(axis.get(2))?axis.get(1):axis.get(2))),
                new PatchProperties.SetPatch(new PropertyName(edited),number("20"))),change.patches());
            props.remove(new PropertyName(edited.equals(axis.get(2))?axis.get(1):axis.get(2)));props.put(new PropertyName(edited),number("20"));
            widget=new WidgetNode(widget.id(),type,props,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,edited));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,edited).getValue());
        }
    }
    @Test void rectLocalEditSwitchesProjectSourceAtomicallyAndRetainsDraftValues()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedPositionedWidgetPropertySchema.RECT_TYPE).orElseThrow();
        var prototype=WidgetNodePrototypeFactory.create(d,StableId.random());var props=new LinkedHashMap<>(prototype.properties());
        props.put(new PropertyName("rect"),ref());var widget=new WidgetNode(prototype.id(),prototype.type(),props,prototype.slots());
        var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"rectWidth");assertFalse(row.supportsDefaultValue());
        row.setValue(FlutterPropertyCellValue.explicit(number("-40")));
        var cmd=assertInstanceOf(PatchProperties.class,commands.removeFirst());
        assertEquals(List.of(new PatchProperties.SetPatch(new PropertyName("rect"),new PropertyValue.StringValue("local")),
            new PatchProperties.SetPatch(new PropertyName("rectWidth"),number("-40"))),cmd.patches());
    }
}

