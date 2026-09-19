package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SlideTransitionPropertyContractTest {
    @Test void fractionalEditorStagesSignedCoordinatesAndReferencesWithoutPublishingDrafts() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(SlideTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,WidgetNodePrototypeFactory.create(d,StableId.random()),d,ignored->{});
        var row=TooltipPropertyContractTest.cell(node,"position");var editor=row.getPropertyEditor();editor.setValue(row.getValue());
        var panel=editor.getCustomEditor();var draft=(FlutterPropertyEditorComponents.CommitOnValidPanel)panel;
        var modes=(javax.swing.JComboBox<?>)AnimatedRotationPropertyContractTest.find(panel,FlutterLocalDartReferenceEditorComponent.MODE_NAME);
        assertEquals(2,modes.getItemCount());
        assertEquals(FlutterLocalDartReferenceEditorComponent.OFFSET,modes.getItemAt(0));
        var x=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterOffsetPropertyEditorComponents.DX_COMPONENT_NAME);
        var y=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterOffsetPropertyEditorComponents.DY_COMPONENT_NAME);
        assertTrue(x.getToolTipText().contains("fraction of child width"));assertTrue(y.getToolTipText().contains("fraction of child height"));
        y.setText("-2.5");
        for(String value:List.of("-2.5","0","0.25","1e308")){x.setText(value);assertTrue(draft.validatedDraftValue().isExplicit());}
        for(String value:List.of("NaN","Infinity","1e309","","1+1")){x.setText(value);assertThrows(IllegalArgumentException.class,draft::validatedDraftValue);}
        modes.setSelectedItem(FlutterLocalDartReferenceEditorComponent.PROJECT);
        var root=(javax.swing.JTextField)AnimatedRotationPropertyContractTest.find(panel,FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME);
        root.setText("_position");assertInstanceOf(PropertyValue.DartObjectReferenceValue.class,draft.validatedDraftValue().explicitValue().orElseThrow());
        assertEquals(row.getValue(),editor.getValue(),"Cancel never publishes a draft");
    }

    @Test void requiredOffsetEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(SlideTransitionWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"position");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var x:List.of(new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE)){
            PropertyValue value=new PropertyValue.OffsetValue(x,java.math.BigDecimal.TWO.negate());
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("position"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("position"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"position"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"position").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_position",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("position"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
    @Test void requiredAnimationEditorAndNullableCheckboxRemainEditableAcrossRefreshAndReopen() throws Exception {
        for(var type:List.of(SlideTransitionWidgetPropertySchema.TYPE)) {
            var d=BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var w=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
            var node=new FlutterWidgetPropertiesNode(Children.LEAF,w,d,commands::add);
            var opacity=TooltipPropertyContractTest.cell(node,"position");var sets=node.getPropertySets();
            assertEquals("Position animation",opacity.getDisplayName());assertFalse(opacity.supportsDefaultValue());
            var editor=opacity.getPropertyEditor();editor.setValue(opacity.getValue());assertNotNull(editor.getCustomEditor());
            var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_fade",Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            opacity.setValue(FlutterPropertyCellValue.explicit(ref));
            assertEquals(ref,assertInstanceOf(SetProperty.class,commands.removeFirst()).value());
            var updated=new WidgetNode(w.id(),type,Map.of(new PropertyName("position"),ref),w.slots());
            node.refreshPresentation(updated,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(opacity,TooltipPropertyContractTest.cell(node,"position"));assertArrayEquals(sets,node.getPropertySets());
            var bool=TooltipPropertyContractTest.cell(node,"transformHitTests");
            assertTrue(bool.supportsDefaultValue());assertNotNull(bool.getPropertyEditor());
            bool.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true)));assertEquals(new PropertyValue.BooleanValue(true),assertInstanceOf(SetProperty.class,commands.removeFirst()).value());
            var flagged=new WidgetNode(w.id(),type,Map.of(new PropertyName("position"),ref,new PropertyName("transformHitTests"),new PropertyValue.BooleanValue(true)),w.slots());
            node.refreshPresentation(flagged,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            var checkEditor=bool.getPropertyEditor();checkEditor.setValue(bool.getValue());assertTrue(checkEditor.isPaintable());assertNull(checkEditor.getTags());
            bool.restoreDefaultValue();assertInstanceOf(ResetProperty.class,commands.removeFirst());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,updated,d,commands::add);
            assertTrue(TooltipPropertyContractTest.cell(reopened,"position").canWrite());
            assertEquals(opacity.getValue(),TooltipPropertyContractTest.cell(reopened,"position").getValue());
        }
    }

}
