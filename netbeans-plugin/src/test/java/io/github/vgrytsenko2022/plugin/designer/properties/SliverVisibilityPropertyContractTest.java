package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverVisibilityPropertyContractTest {
    @Test void booleanRowKeepsCheckboxStableIdentityAndResetAfterReopen() throws Exception {
        for (var type : List.of(SliverVisibilityWidgetPropertySchema.TYPE, SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE)) {
        var d = BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var sets = node.getPropertySets();
        for (var definition : d.properties()) {
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
    }
}

