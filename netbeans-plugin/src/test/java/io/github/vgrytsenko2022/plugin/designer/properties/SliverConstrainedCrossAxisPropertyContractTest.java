package io.github.vgrytsenko2022.plugin.designer.properties;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;
class SliverConstrainedCrossAxisPropertyContractTest {
    @Test void maxExtentNumberAndInfinityKeepRowIdentityAfterEditAndReopen() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverConstrainedCrossAxisWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var row = TooltipPropertyContractTest.cell(node, "maxExtent"); var sets = node.getPropertySets();
        assertTrue(row.canWrite()); assertFalse(row.supportsDefaultValue());
        for (var value : List.of(new PropertyValue.DoubleValue(BigDecimal.ZERO),
                new PropertyValue.DoubleValue(new BigDecimal("37.5")), SliverConstrainedCrossAxisWidgetPropertySchema.INFINITY,
                new PropertyValue.IntegerValue(BigInteger.valueOf(90)))) {
            var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
            editor.setAsText(value instanceof PropertyValue.EnumValue ? "Infinity"
                    : value instanceof PropertyValue.DoubleValue decimal ? decimal.value().toPlainString() : "90");
            row.setValue((FlutterPropertyCellValue)editor.getValue());
            assertEquals(new SetProperty(widget.id(), new PropertyName("maxExtent"),
                    ((FlutterPropertyCellValue)editor.getValue()).explicitValue().orElseThrow()), commands.removeFirst());
            widget = new WidgetNode(widget.id(), widget.type(), Map.of(new PropertyName("maxExtent"), value), widget.slots());
            node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, "maxExtent")); assertArrayEquals(sets, node.getPropertySets());
            var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
            assertEquals(row.getValue(), TooltipPropertyContractTest.cell(reopened, "maxExtent").getValue());
            assertTrue(TooltipPropertyContractTest.cell(reopened, "maxExtent").canWrite());
        }
        for (String bad : List.of("-1", "-Infinity", "NaN", "null", "1e309", "someDartCode()")) {
            var editor = row.getPropertyEditor(); editor.setValue(row.getValue());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
        assertTrue(commands.isEmpty());
    }
}

