package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class FlexibleSpaceBarPropertyContractTest {
    @Test void allFiveTypedRowsRemainStableWritableAndResettable() throws Exception {
        var definition = BuiltInWidgetCatalog.getDefault().find(FlexibleSpaceBarWidgetPropertySchema.TYPE).orElseThrow();
        var widget = WidgetNodePrototypeFactory.create(definition, StableId.random());
        var commands = new ArrayList<DesignerCommand>();
        var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, commands::add);
        var sets = node.getPropertySets();
        var values = Map.<String,PropertyValue>of(
                "centerTitle", new PropertyValue.BooleanValue(true),
                "titlePadding", new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE),
                "collapseMode", new PropertyValue.EnumValue("CollapseMode", "pin"),
                "stretchModes", new PropertyValue.StringValue("zoomBackground,blurBackground,fadeTitle"),
                "expandedTitleScale", new PropertyValue.DoubleValue(BigDecimal.ONE));
        for (var field : FlexibleSpaceBarWidgetPropertySchema.FIELDS) {
            var row = TooltipPropertyContractTest.cell(node, field.name());
            assertTrue(row.canWrite()); assertTrue(row.supportsDefaultValue()); assertNotNull(row.getPropertyEditor());
            assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
            row.setValue(FlutterPropertyCellValue.explicit(values.get(field.name())));
            assertEquals(1, commands.size());
            widget = RadioPropertyContractTest.apply(widget, commands.removeFirst());
            node.refreshPresentation(widget, definition, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(row, TooltipPropertyContractTest.cell(node, field.name())); assertArrayEquals(sets, node.getPropertySets());
            assertEquals(FlutterPropertyCellValue.explicit(values.get(field.name())), row.getValue());
            row.restoreDefaultValue(); widget = RadioPropertyContractTest.apply(widget, commands.removeFirst());
            node.refreshPresentation(widget, definition, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertEquals(FlutterPropertyCellValue.unset(), row.getValue());
        }
        assertEquals(FlutterTypedPropertyEditors.EditorKind.EDGE_INSETS_REFERENCE,
                FlutterTypedPropertyEditors.binding(definition.property(new PropertyName("titlePadding")).orElseThrow()).orElseThrow().editorKind());
        var stretch = definition.property(new PropertyName("stretchModes")).orElseThrow();
        var binding = FlutterTypedPropertyEditors.binding(stretch, Optional.empty(), false, FlexibleSpaceBarWidgetPropertySchema.STRETCH_PRESETS).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE, binding.editorKind());
        for (var preset : FlexibleSpaceBarWidgetPropertySchema.STRETCH_PRESETS)
            assertDoesNotThrow(() -> binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset))));
    }
}

