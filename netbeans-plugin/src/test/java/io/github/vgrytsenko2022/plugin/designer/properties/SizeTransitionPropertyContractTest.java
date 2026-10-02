package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SizeTransitionPropertyContractTest {
    @Test void requiredSizeEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(SizeTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"sizeFactor");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var x:List.of(new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE)){
            PropertyValue value=new PropertyValue.DoubleValue(x);
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("sizeFactor"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("sizeFactor"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"sizeFactor"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"sizeFactor").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_size",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("sizeFactor"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }

    @Test void nullableDirectionalAlignmentLegacyAndCrossAxisEditorsPreserveStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(SizeTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"alignment");var editor=row.getPropertyEditor();editor.setValue(row.getValue());
        var panel=editor.getCustomEditor();var basis=(javax.swing.JComboBox<?>)find(panel,FlutterContainerPropertyEditorComponents.ALIGNMENT_BASIS_NAME);
        assertEquals(2,basis.getItemCount());assertTrue(row.supportsDefaultValue());
        var sets=node.getPropertySets();
        for(String name:List.of("alignment","axisAlignment","fixedCrossAxisSizeFactor")){
            var cell=TooltipPropertyContractTest.cell(node,name);assertTrue(cell.canWrite());assertTrue(cell.supportsDefaultValue());
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_value",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            var local=name.equals("alignment")?new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                new java.math.BigDecimal("2"),new java.math.BigDecimal("-3")):new PropertyValue.DoubleValue(new java.math.BigDecimal("0.5"));
            for(var value:List.of(local,ref,new PropertyValue.NullValue())){
                cell.setValue(FlutterPropertyCellValue.explicit(value));assertEquals(new SetProperty(widget.id(),new PropertyName(name),value),commands.removeFirst());
                var updated=new LinkedHashMap<>(widget.properties());updated.put(new PropertyName(name),value);
                var snapshot=new WidgetNode(widget.id(),widget.type(),updated,widget.slots());
                node.refreshPresentation(snapshot,d,commands::add,null,null,FlutterImageAssetChoices.empty());
                assertSame(cell,TooltipPropertyContractTest.cell(node,name));assertArrayEquals(sets,node.getPropertySets());
                var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,snapshot,d,commands::add);
                assertEquals(cell.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
            }
            cell.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName(name)),commands.removeFirst());
        }
        var cross=TooltipPropertyContractTest.cell(node,"fixedCrossAxisSizeFactor");
        assertThrows(IllegalArgumentException.class,()->cross.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("-0.01")))));
        var legacy=TooltipPropertyContractTest.cell(node,"axisAlignment");editor=legacy.getPropertyEditor();editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.ZERO)));
        panel=editor.getCustomEditor();var field=(javax.swing.JTextField)find(panel,FlutterNullableNumberEditorComponent.VALUE_NAME);
        field.setText("-2.5");var commit=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("-2.5"))),commit.validatedDraftValue());
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(java.math.BigDecimal.ZERO)),editor.getValue());
        for(String invalid:List.of("NaN","Infinity","","call()")){field.setText(invalid);assertThrows(IllegalArgumentException.class,commit::validatedDraftValue);}
    }
    static java.awt.Component find(java.awt.Component root,String name){
        if(name.equals(root.getName()))return root;
        if(root instanceof java.awt.Container parent)for(var child:parent.getComponents()){
            var found=find(child,name);if(found!=null)return found;
        }
        return null;
    }

}
