package dev.flutter.netbeans.plugin.designer.properties;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverPaddingPropertyContractTest {
    @Test void requiredPaddingEditorSupportsPhysicalDirectionalReferencesAndStableReopenedRows() throws Exception {
        var d = BuiltInWidgetCatalog.getDefault().find(SliverPaddingWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(d, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
        var groups = node.getPropertySets();
        var row = TooltipPropertyContractTest.cell(node, "padding");
        assertTrue(row.canWrite()); assertFalse(row.supportsDefaultValue());
        var editor = row.getPropertyEditor();
        editor.setValue(row.getValue());
        assertNotNull(editor.getCustomEditor());
        var z = BigDecimal.ZERO;
        for (PropertyValue value : List.of(new PropertyValue.EdgeInsetsValue(z,z,z,z),
                new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,z,BigDecimal.TEN,z),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:sample/insets.dart"),
                        "configured", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                        "_makeInsets", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            row.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(new SetProperty(widget.id(), SliverPaddingWidgetPropertySchema.PADDING, value), commands.removeFirst());
            widget = new WidgetNode(widget.id(), widget.type(), Map.of(SliverPaddingWidgetPropertySchema.PADDING, value), widget.slots());
            node.refreshPresentation(widget, d, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, "padding")); assertArrayEquals(groups, node.getPropertySets());
            assertEquals(FlutterPropertyCellValue.explicit(value), row.getValue());
            var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, d, commands::add);
            assertEquals(row.getValue(), TooltipPropertyContractTest.cell(reopened, "padding").getValue());
            assertTrue(TooltipPropertyContractTest.cell(reopened, "padding").canWrite());
        }
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EdgeInsetsValue(BigDecimal.ONE.negate(),z,z,z))));
        assertTrue(commands.isEmpty());
    }
}
