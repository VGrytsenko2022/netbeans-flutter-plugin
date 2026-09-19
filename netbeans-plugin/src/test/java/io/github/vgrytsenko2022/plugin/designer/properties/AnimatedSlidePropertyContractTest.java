package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedSlidePropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSlideWidgetPropertySchema.TYPE).orElseThrow();
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

    @Test void requiredOffsetEditorHasOnlyLocalAndReferenceModesAndStableRows() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(AnimatedSlideWidgetPropertySchema.TYPE).orElseThrow();
        var widget=WidgetNodePrototypeFactory.create(d,StableId.random());var commands=new ArrayList<DesignerCommand>();
        var node=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
        var row=TooltipPropertyContractTest.cell(node,"offset");assertTrue(row.canWrite());assertFalse(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());var panel=editor.getCustomEditor();
        var queue=new ArrayDeque<java.awt.Component>();queue.add(panel);javax.swing.JComboBox<?> modes=null;
        while(!queue.isEmpty()){var c=queue.removeFirst();if(FlutterLocalDartReferenceEditorComponent.MODE_NAME.equals(c.getName()))modes=(javax.swing.JComboBox<?>)c;
            if(c instanceof java.awt.Container container)queue.addAll(Arrays.asList(container.getComponents()));}
        assertNotNull(modes);assertEquals(2,modes.getItemCount());
        for(var x:List.of(new java.math.BigDecimal("-2.5"),java.math.BigDecimal.ONE)){
            PropertyValue value=new PropertyValue.OffsetValue(x,java.math.BigDecimal.TWO.negate());
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),new PropertyName("offset"),value),commands.removeFirst());
            var p=new LinkedHashMap<>(widget.properties());p.put(new PropertyName("offset"),value);
            widget=new WidgetNode(widget.id(),widget.type(),p,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"offset"));
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"offset").getValue());
        }
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_offset",Optional.empty(),
            PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        row.setValue(FlutterPropertyCellValue.explicit(ref));assertEquals(new SetProperty(widget.id(),new PropertyName("offset"),ref),commands.removeFirst());
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
    }
}


