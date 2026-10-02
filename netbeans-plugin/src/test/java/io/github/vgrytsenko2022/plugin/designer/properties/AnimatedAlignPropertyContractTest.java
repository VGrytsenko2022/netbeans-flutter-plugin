package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedAlignPropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedAlignWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var curve=TooltipPropertyContractTest.cell(node,"curve");
        var editor=curve.getPropertyEditor();editor.setValue(curve.getValue());
        var queue=new ArrayDeque<java.awt.Component>();queue.add(editor.getCustomEditor());javax.swing.JComboBox<?> presets=null;
        while(!queue.isEmpty()){
            var c=queue.removeFirst();
            if(FlutterPresetDartReferenceEditorComponent.PRESET_NAME.equals(c.getName())) presets=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));
        }
        assertNotNull(presets);assertEquals(43,presets.getItemCount());
        for(String preset:ExpansionTileWidgetPropertySchema.curvePresets()){
            curve.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)));
            assertEquals(new SetProperty(widget.id(),new PropertyName("curve"),new PropertyValue.StringValue(preset)),commands.removeFirst());
        }
        var duration=TooltipPropertyContractTest.cell(node,"durationUs");assertFalse(duration.supportsDefaultValue());
        editor=duration.getPropertyEditor();editor.setValue(duration.getValue());assertNotNull(editor.getCustomEditor());
        for(long us:List.of(0L,1L,300000L)){
            var value=new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(us));
            duration.setValue(FlutterPropertyCellValue.explicit(value));
            if(us==300000L) assertTrue(commands.isEmpty());
            else assertEquals(new SetProperty(widget.id(),new PropertyName("durationUs"),value),commands.removeFirst());
        }
        var end=TooltipPropertyContractTest.cell(node,"onEnd");assertTrue(end.canWrite());assertTrue(end.supportsDefaultValue());
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_handleEnd",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        end.setValue(FlutterPropertyCellValue.explicit(ref));
        assertEquals(new SetProperty(widget.id(),new PropertyName("onEnd"),ref),commands.removeFirst());
        assertTrue(commands.isEmpty());
    }

    @Test void requiredAlignmentEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedAlignWidgetPropertySchema.TYPE).orElseThrow();
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
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedAlignWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        for(String name:List.of("widthFactor","heightFactor")){
            var row=TooltipPropertyContractTest.cell(node,name);var sets=node.getPropertySets();
            assertTrue(row.canWrite());assertTrue(row.supportsDefaultValue());assertEquals(FlutterPropertyCellValue.unset(),row.getValue());
            for(var value:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.IntegerValue(java.math.BigInteger.ZERO),
                new PropertyValue.DoubleValue(new java.math.BigDecimal("2.5")))){
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

