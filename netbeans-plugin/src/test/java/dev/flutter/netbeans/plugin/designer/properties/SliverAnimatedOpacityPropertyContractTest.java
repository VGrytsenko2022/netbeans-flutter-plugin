package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverAnimatedOpacityPropertyContractTest {
    @Test void completeCurveDurationAndCallbackEditorsAreWritableAndTyped() throws Exception {
        var d=BuiltInWidgetCatalog.getDefault().find(SliverAnimatedOpacityWidgetPropertySchema.TYPE).orElseThrow();
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

    @Test void optionalBooleanRowKeepCheckboxesStableIdentityAndResetAfterReopen() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverAnimatedOpacityWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var sets = node.getPropertySets();
        for (var definition : d.properties().stream().filter(p -> p.name().value().equals("alwaysIncludeSemantics")).toList()) {
            var name = definition.name();
            var row = TooltipPropertyContractTest.cell(node, name.value());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
            for (boolean flag : List.of(true, false)) {
                var value = new PropertyValue.BooleanValue(flag);
                row.setValue(FlutterPropertyCellValue.explicit(value));
                assertEquals(new SetProperty(widget.id(), name, value), commands.removeFirst());
                var props = new LinkedHashMap<>(widget.properties()); props.put(name, value);
                widget = new WidgetNode(widget.id(), widget.type(), props, widget.slots());
                node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
                assertSame(row, TooltipPropertyContractTest.cell(node, name.value()));
                assertArrayEquals(sets, node.getPropertySets());
                var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
                assertTrue(editor.isPaintable()); assertNull(editor.getTags());
                var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
                var reopenedRow = TooltipPropertyContractTest.cell(reopened, name.value());
                assertEquals(row.getValue(), reopenedRow.getValue()); assertTrue(reopenedRow.canWrite());
                reopenedRow.restoreDefaultValue();
                assertEquals(new ResetProperty(widget.id(), name), commands.removeFirst());
            }
            assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
            assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("false"))));
        }
        assertTrue(commands.isEmpty());
    }
    @Test void opacityEditsKeepRowIdentityAndRejectUnsetAndOutOfRange() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverAnimatedOpacityWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var row = TooltipPropertyContractTest.cell(node, "opacity"); var sets = node.getPropertySets();
        assertFalse(row.supportsDefaultValue());
        for (String text : List.of("0", "0.125", "1")) {
            var editor = row.getPropertyEditor(); editor.setValue(row.getValue()); editor.setAsText(text);
            var value = (FlutterPropertyCellValue) editor.getValue(); row.setValue(value);
            assertEquals(new SetProperty(widget.id(), new PropertyName("opacity"), value.explicitValue().orElseThrow()), commands.removeFirst());
            var props = new LinkedHashMap<>(widget.properties()); props.put(new PropertyName("opacity"), value.explicitValue().orElseThrow());
            widget = new WidgetNode(widget.id(), widget.type(), props, widget.slots());
            node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, "opacity")); assertArrayEquals(sets, node.getPropertySets());
            var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
            assertEquals(value, TooltipPropertyContractTest.cell(reopened, "opacity").getValue());
        }
        for (String bad : List.of("-0.1", "1.1", "Infinity", "NaN", "<not set>")) {
            var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
        assertTrue(commands.isEmpty());
    }
}
