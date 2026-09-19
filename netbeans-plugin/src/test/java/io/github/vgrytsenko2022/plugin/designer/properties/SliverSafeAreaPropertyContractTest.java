package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverSafeAreaPropertyContractTest {
    @Test void booleanRowKeepsCheckboxStableIdentityAndResetAfterReopen() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverSafeAreaWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var sets = node.getPropertySets();
        for (var definition : d.properties().stream().filter(p -> !p.name().value().equals("minimum")).toList()) {
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
        var minimum = new PropertyName("minimum");
        var row = TooltipPropertyContractTest.cell(node, "minimum");
        var z = java.math.BigDecimal.ZERO;
        var values=List.of(new PropertyValue.EdgeInsetsValue(z,z,z,z),
                new PropertyValue.EdgeInsetsValue(z.subtract(java.math.BigDecimal.ONE),java.math.BigDecimal.TEN,z,z));
        assertEquals(FlutterPropertyCellValue.unset(), row.getValue());assertTrue(row.supportsDefaultValue());
        var editor=row.getPropertyEditor();editor.setValue(row.getValue());assertNotNull(editor.getCustomEditor());
        for(var value:values){
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(),minimum,value),commands.removeFirst());
            var props=new LinkedHashMap<>(widget.properties());props.put(minimum,value);
            widget=new WidgetNode(widget.id(),widget.type(),props,widget.slots());
            node.refreshPresentation(widget,d,commands::add,null,null,FlutterImageAssetChoices.empty());
            assertSame(row,TooltipPropertyContractTest.cell(node,"minimum"));assertArrayEquals(sets,node.getPropertySets());
            var reopened=new FlutterWidgetPropertiesNode(Children.LEAF,widget,d,commands::add);
            assertEquals(row.getValue(),TooltipPropertyContractTest.cell(reopened,"minimum").getValue());
            TooltipPropertyContractTest.cell(reopened,"minimum").restoreDefaultValue();
            assertEquals(new ResetProperty(widget.id(),minimum),commands.removeFirst());
        }
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EdgeInsetsDirectionalValue(z,z,z,z))));
        assertThrows(IllegalArgumentException.class,()->row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertTrue(commands.isEmpty());
    }
}

