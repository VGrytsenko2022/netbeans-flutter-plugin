package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import java.math.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class PositionedTransitionPropertyContractTest {
    static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new BigDecimal(n));}
    static PropertyValue.DartObjectReferenceValue ref(){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_rect",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    @Test void requiredRowsAndTypedAnimationPresetEditor()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(PositionedTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(var field:PositionedTransitionWidgetPropertySchema.FIELDS){
            var row=TooltipPropertyContractTest.cell(node,field.name());assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
            var editor=row.getPropertyEditor();editor.setValue(row.getValue());
            if(field.name().equals("rect")){
                var preset=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(editor.getCustomEditor(),FlutterPresetDartReferenceEditorComponent.PRESET_NAME);
                assertEquals(1,preset.getItemCount());assertTrue(d.property(new PropertyName("rect")).orElseThrow().constraints().stream().anyMatch(c->c instanceof PropertyValueConstraint.DartObjectReferenceValues r&&r.expectedDartType().equals("Animation<RelativeRect>")));
            }
            assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
            assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        }
    }
    @Test void everyInsetAtomicallySelectsLocalRetainsOtherInsetsAndPreservesRows()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(PositionedTransitionWidgetPropertySchema.TYPE).orElseThrow();
        for(String edge:PositionedTransitionWidgetPropertySchema.EDGES){
            var prototype=WidgetNodePrototypeFactory.create(d,StableId.random());var props=new LinkedHashMap<>(prototype.properties());
            props.put(new PropertyName("rect"),ref());props.put(new PropertyName("rectBottom"),number("44"));
            var widget=new WidgetNode(prototype.id(),prototype.type(),props,prototype.slots());
            var commands=new ArrayList<DesignerCommand>();var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            var row=TooltipPropertyContractTest.cell(node,edge);var sets=node.getPropertySets();
            row.setValue(FlutterPropertyCellValue.explicit(number("-20.5")));
            var cmd=assertInstanceOf(PatchProperties.class,commands.removeFirst());
            assertEquals(List.of(new PatchProperties.SetPatch(new PropertyName("rect"),new PropertyValue.StringValue("local")),
                new PatchProperties.SetPatch(new PropertyName(edge),number("-20.5"))),cmd.patches());
            props.put(new PropertyName("rect"),new PropertyValue.StringValue("local"));props.put(new PropertyName(edge),number("-20.5"));
            widget=new WidgetNode(widget.id(),widget.type(),props,widget.slots());node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,edge));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,edge).getValue());
            var source=TooltipPropertyContractTest.cell(node,"rect");source.setValue(FlutterPropertyCellValue.explicit(ref()));
            assertEquals(new SetProperty(widget.id(),new PropertyName("rect"),ref()),commands.removeFirst());
            for(String bad:List.of("NaN","Infinity","call()","1e309")){
                var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertThrows(IllegalArgumentException.class,()->editor.setAsText(bad));
                assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
            }
        }
    }
}
