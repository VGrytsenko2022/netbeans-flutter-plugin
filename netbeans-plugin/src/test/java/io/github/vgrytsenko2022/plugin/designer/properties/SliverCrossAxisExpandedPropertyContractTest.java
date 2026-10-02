package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverCrossAxisExpandedPropertyContractTest {
    @Test void requiredFlexEditsKeepRowIdentityAndRemainWritableAfterReopen() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverCrossAxisExpandedWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var row = TooltipPropertyContractTest.cell(node, "flex");
        var sets = node.getPropertySets();
        assertTrue(row.canWrite()); assertFalse(row.supportsDefaultValue());
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(BigInteger.ONE)), row.getValue());
        for (long n : List.of(2L, 7L, 9007199254740991L, 1L)) {
            var value = new PropertyValue.IntegerValue(BigInteger.valueOf(n));
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(), new PropertyName("flex"), value), commands.removeFirst());
            widget = new WidgetNode(widget.id(), widget.type(), Map.of(new PropertyName("flex"), value), widget.slots());
            node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, "flex")); assertArrayEquals(sets, node.getPropertySets());
            var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
            assertEquals(row.getValue(), TooltipPropertyContractTest.cell(reopened, "flex").getValue());
            assertTrue(TooltipPropertyContractTest.cell(reopened, "flex").canWrite());
        }
        for (var value : List.of(new PropertyValue.IntegerValue(BigInteger.ZERO),
                new PropertyValue.IntegerValue(BigInteger.valueOf(-1)), new PropertyValue.NullValue(),
                new PropertyValue.DoubleValue(new BigDecimal("1.5"))))
            assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(value)));
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
        assertTrue(commands.isEmpty());
    }
}
