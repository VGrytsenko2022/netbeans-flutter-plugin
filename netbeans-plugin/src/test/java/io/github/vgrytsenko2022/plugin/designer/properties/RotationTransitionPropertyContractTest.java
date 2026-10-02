package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class RotationTransitionPropertyContractTest {
    @Test void requiredTurnsEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(RotationTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"turns");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var x:List.of(new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE)){
            PropertyValue value=new PropertyValue.DoubleValue(x);
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("turns"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("turns"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"turns"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"turns").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_turns",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("turns"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void alignmentIsPhysicalOnlyAndQualityRetainsOmittedNullAndAllValues() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(RotationTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"alignment");
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var basis=(javax.swing.JComboBox<?>)find(panel,FlutterContainerPropertyEditorComponents.ALIGNMENT_BASIS_NAME);
        assertEquals(1,basis.getItemCount());assertEquals(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,basis.getItemAt(0));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.AlignmentGeometryValue(
            PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO))));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        var quality=TooltipPropertyContractTest.cell(node,"filterQuality");assertTrue(quality.supportsDefaultValue());
        var sets=node.getPropertySets();
        for(String v:List.of("none","low","medium","high","null")){
            var value=v.equals("null")?new PropertyValue.NullValue():new PropertyValue.EnumValue("FilterQuality",v);
            quality.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),new PropertyName("filterQuality"),value),commands.removeFirst());
            var updated=new LinkedHashMap<>(widget.properties());updated.put(new PropertyName("filterQuality"),value);
            var snapshot=new WidgetNode(widget.id(),widget.type(),updated,widget.slots());
            node.refreshPresentation(snapshot,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(quality,TooltipPropertyContractTest.cell(node,"filterQuality"));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,snapshot,d,commands::add);
            assertEquals(quality.getValue(),TooltipPropertyContractTest.cell(reopened,"filterQuality").getValue());
        }
        quality.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName("filterQuality")),commands.removeFirst());
        var turns=TooltipPropertyContractTest.cell(node,"turns");editor=turns.getPropertyEditor();editor.setValue(turns.getValue());
        panel=editor.getCustomEditor();
        var numericMode=(javax.swing.JComboBox<?>)find(panel,FlutterNullableNumberEditorComponent.MODE_NAME);
        assertEquals(1,numericMode.getItemCount());assertEquals(FlutterNullableNumberEditorComponent.NUMBER,numericMode.getItemAt(0));
        var field=(javax.swing.JTextField)find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
        field.setText("-2.5");
        var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("-2.5"))),commit.validatedDraftValue());
        assertEquals(turns.getValue(),editor.getValue(),"Draft must not publish into the cell");
        for(String invalid:List.of("NaN","Infinity","null","","someDart()")){
            field.setText(invalid);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue,invalid);
        }
    }
    static java.awt.Component find(java.awt.Component root,String name){
        if(name.equals(root.getName()))return root;
        if(root instanceof java.awt.Container parent)for(var child:parent.getComponents()){
            var found=find(child,name);if(found!=null)return found;
        }
        return null;
    }

}
