package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import static org.junit.jupiter.api.Assertions.*;

class SliverVariedExtentListPropertyContractTest {
    @Test void everyDynamicSliverFieldIsEditableAndRefreshKeepsItsRowAndPropertySets() throws Exception {
        for (var kind : SliverVariedExtentListWidgetPropertySchema.Kind.values()) {
            var type = kind.type();
            var definition = BuiltInWidgetCatalog.getDefault().find(type).orElseThrow();
            var widget = WidgetNodePrototypeFactory.create(definition, StableId.random());
            var commands = new ArrayList<DesignerCommand>();
            var node = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, commands::add);
            var groups = node.getPropertySets();
            for (var property : definition.properties()) {
                var row = TooltipPropertyContractTest.cell(node, property.name().value());
                assertTrue(row.canWrite());
                assertEquals(!property.parameter().required(), row.supportsDefaultValue());
                PropertyValue value = property.name().value().equals("itemExtent") ? new PropertyValue.DoubleValue(BigDecimal.ZERO)
                        : property.acceptedKinds().contains(PropertyValueKind.BOOLEAN)
                        ? new PropertyValue.BooleanValue(false)
                        : property.acceptedKinds().contains(PropertyValueKind.INTEGER)
                                ? new PropertyValue.IntegerValue(BigInteger.valueOf(3))
                                : property.creationDefault().orElseGet(() -> new PropertyValue.NullValue());
                if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) {
                    value = new PropertyValue.DartObjectReferenceValue(Optional.of("package:sample/builders.dart"),
                            "configured", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
                }
                row.setValue(FlutterPropertyCellValue.explicit(value));
                assertEquals(new SetProperty(widget.id(), property.name(), value), commands.removeFirst());
                var values = new LinkedHashMap<>(widget.properties());
                values.put(property.name(), value);
                widget = new WidgetNode(widget.id(), widget.type(), values, widget.slots());
                node.refreshPresentation(widget, definition, commands::add, null, null, FlutterImageAssetChoices.empty());
                assertSame(row, TooltipPropertyContractTest.cell(node, property.name().value()));
                assertArrayEquals(groups, node.getPropertySets());
                assertEquals(FlutterPropertyCellValue.explicit(value), row.getValue());
                if (value instanceof PropertyValue.BooleanValue) {
                    assertTrue(row.getPropertyEditor().isPaintable());
                    assertNull(row.getPropertyEditor().getTags());
                }
                if (property.parameter().required()) {
                    assertThrows(IllegalArgumentException.class, () -> row.setValue(FlutterPropertyCellValue.unset()));
                }
            }
            var reopened = new FlutterWidgetPropertiesNode(Children.LEAF, widget, definition, commands::add);
            for (var property : definition.properties()) {
                var reopenedRow = TooltipPropertyContractTest.cell(reopened, property.name().value());
                assertTrue(reopenedRow.canWrite());
                if (!property.parameter().required()) {
                    reopenedRow.restoreDefaultValue();
                    assertEquals(new ResetProperty(widget.id(), property.name()), commands.removeFirst());
                } else {
                    assertFalse(reopenedRow.supportsDefaultValue());
                    assertThrows(IllegalArgumentException.class, () -> reopenedRow.setValue(FlutterPropertyCellValue.unset()));
                }
                assertEquals(FlutterPropertyCellValue.explicit(widget.properties().get(property.name())),
                        TooltipPropertyContractTest.cell(reopened, property.name().value()).getValue());
            }
        }
    }
}

