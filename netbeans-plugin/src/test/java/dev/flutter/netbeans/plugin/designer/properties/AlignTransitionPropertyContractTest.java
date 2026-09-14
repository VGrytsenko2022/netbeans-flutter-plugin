package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class AlignTransitionPropertyContractTest {
    @Test void nestedDraftsAreRetainedAndNotCommittedBeforeOk()throws Exception{
        var d=BuiltInWidgetCatalog.getDefault().find(AlignTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(String name:List.of("alignment","widthFactor","heightFactor")){
            var row=TooltipPropertyContractTest.cell(node,name);var editor=row.getPropertyEditor();
            editor.setValue(row.getValue());var panel=editor.getCustomEditor();
            var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
            assertEquals(name.equals("alignment")?2:4,modes.getItemCount());
            var local=name.equals("alignment")?FlutterLocalDartReferenceEditorComponent.ALIGNMENT:FlutterLocalDartReferenceEditorComponent.NUMBER;
            modes.setSelectedItem(local);
            var field=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,name.equals("alignment")?
                FlutterContainerPropertyEditorComponents.ALIGNMENT_HORIZONTAL_NAME:FlutterNullableNumberEditorComponent.VALUE_NAME);
            field.setText(name.equals("alignment")?"-2.5":"2.5");
            var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;var localValue=draft.validatedDraftValue();
            modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
            assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
            modes.setSelectedItem(local);assertEquals(localValue,draft.validatedDraftValue());
            field.setText("NaN");assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);
            assertEquals(row.getValue(),editor.getValue());assertTrue(commands.isEmpty());
        }
    }
    @Test void requiredAlignmentEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AlignTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"alignment");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var basis:PropertyValue.AlignmentGeometryValue.HorizontalBasis.values()){
            var value=new PropertyValue.AlignmentGeometryValue(basis,new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE);
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("alignment"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("alignment"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"alignment"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"alignment").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_alignment",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("alignment"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void nullableFactorsKeepOmissionNullZeroAndPositiveValuesDistinctAfterReopen() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AlignTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(String name:List.of("widthFactor","heightFactor")){
            var row=TooltipPropertyContractTest.cell(node,name);var sets=node.getPropertySets();
            assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());assertEquals(FlutterPropertyCellValue.unset(),row.getValue());
            for(var value:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("2.5")),new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_factor",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()))){
                row.setValue(FlutterPropertyCellValue.explicit(value));
                assertEquals(new SetProperty(widget.id(),new PropertyName(name),value),commands.removeFirst());
                var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName(name),value);
                widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
                node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
                assertSame(row,TooltipPropertyContractTest.cell(node,name));assertArrayEquals(sets,node.getPropertySets());
                var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
                assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,name).getValue());
            }
            row.restoreDefaultValue();assertEquals(new ResetProperty(widget.id(),new PropertyName(name)),commands.removeFirst());
            assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new java.math.BigDecimal("-0.1")))));
        }
    }
}
