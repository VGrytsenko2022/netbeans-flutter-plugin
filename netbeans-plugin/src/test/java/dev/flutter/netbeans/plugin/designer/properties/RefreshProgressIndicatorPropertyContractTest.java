package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class RefreshProgressIndicatorPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900002-e530-4b9b-92fa-49e3c491f093");

    @Test void allTwelveOptionalRowsRetainExactTypedValuesGroupsCellsAndReset() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertTrue(initial.properties().isEmpty()); assertTrue(initial.slots().isEmpty());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(12, DEF.properties().size()); assertEquals(5, sets.length);
        assertTrue(DEF.properties().stream().noneMatch(field -> List.of("controller", "variant", "year2023", "constraints", "trackGap").contains(field.name().value())));
        for (var field : DEF.properties()) {
            String name = field.name().value(); node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); assertTrue(cell.canWrite()); assertTrue(cell.supportsDefaultValue()); assertEquals(FlutterPropertyCellValue.unset(), cell.getValue());
            var editor = cell.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue());
            if (List.of("valueColor", "indicatorMargin", "indicatorPadding").contains(name)) assertTrue(editor.supportsCustomEditor());
            else { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue()); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new SetProperty(ID, field.name(), value(name))), commands);
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), Map.of(field.name(), value(name)), Map.of()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear(); cell.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(ID, field.name())), commands);
        }
        assertTrue(cell(node, "strokeWidth").getShortDescription().contains("2.5"));
        assertTrue(cell(node, "strokeWidth").getShortDescription().contains("fallback 4"));
        assertTrue(cell(node, "semanticsValue").getShortDescription().contains("45%"));
    }

    @Test void nullableWidthUsesNumericUnionAndPreservesOmissionNullAndExactFiniteNumber() {
        var property = DEF.properties().stream().filter(field -> field.name().value().equals("strokeWidth")).findFirst().orElseThrow();
        var binding = FlutterTypedPropertyEditors.binding(property).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_NUMBER, binding.editorKind());
        var editor = binding.createEditor(); assertTrue(editor.supportsCustomEditor());
        for (String text : List.of("null", "NULL", "Inherited (null)")) {
            editor.setAsText(text); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
            assertEquals("Inherited (null)", editor.getAsText());
        }
        editor.setAsText("<not set>"); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        editor.setAsText("-2.5"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.DoubleValue(new BigDecimal("-2.5"))), editor.getValue());
        editor.setAsText("null"); editor.setAsText("2"); assertInstanceOf(PropertyValue.IntegerValue.class, ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow());
        var before = editor.getValue();
        for (String invalid : List.of("Infinity", "-Infinity", "NaN", "double.infinity", "null ?? 2.5", "1e999")) {
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid)); assertEquals(before, editor.getValue());
        }
        editor.setAsText(""); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        var colorBinding = FlutterTypedPropertyEditors.binding(DEF.properties().stream().filter(field -> field.name().value().equals("valueColor")).findFirst().orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.COLOR_ANIMATION, colorBinding.editorKind());
    }

    @Test void numericDomainsAndStrokeCapsAreCompleteWithoutClampingOrInventingProperties() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("value", "strokeWidth", "strokeAlign", "elevation")) {
            var editor = cell(node, name).getPropertyEditor();
            for (String numeric : name.equals("elevation") ? List.of("0", "2.5", "40") : List.of("-2", "0", "1.5", "40")) {
                editor.setAsText(numeric); var v = ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow();
                assertEquals(0, new BigDecimal(numeric).compareTo(v instanceof PropertyValue.IntegerValue i ? new BigDecimal(i.value()) : ((PropertyValue.DoubleValue) v).value()));
            }
            var before = editor.getValue();
            for (String invalid : List.of("Infinity", "-Infinity", "NaN", "1 + 2", "1e999")) {
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid)); assertEquals(before, editor.getValue());
            }
            if (!name.equals("strokeWidth")) assertThrows(IllegalArgumentException.class, () -> editor.setAsText("null"));
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "elevation").getPropertyEditor().setAsText("-0.01"));
        var cap = cell(node, "strokeCap").getPropertyEditor();
        for (String value : List.of("butt", "round", "square")) {
            cap.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("StrokeCap", value)));
            cap.setAsText(cap.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("StrokeCap", value)), cap.getValue());
        }
    }

    @Test void bothInsetsAndEveryNullableColorAnimationBranchPreserveTheirClosedTypes() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("indicatorMargin", "indicatorPadding")) {
            for (PropertyValue v : List.of(value("indicatorMargin"), value("indicatorPadding"))) {
                var editor = cell(node, name).getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(v));
                assertEquals(FlutterPropertyCellValue.explicit(v), editor.getValue()); assertTrue(editor.supportsCustomEditor());
            }
        }
        for (PropertyValue animation : List.of(new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.tertiary")),
                new PropertyValue.NullValue(), LinearProgressIndicatorPropertyContractTest.reference("_valueColor"))) {
            var editor = cell(node, "valueColor").getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(animation));
            assertFalse(editor.getAsText().contains("<not set>")); assertThrows(IllegalArgumentException.class, () -> editor.setAsText("ColorTween(begin: Colors.red)"));
        }
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "value" -> new PropertyValue.DoubleValue(new BigDecimal("1.5"));
            case "backgroundColor" -> new PropertyValue.ColorValue(0x80123456L);
            case "color" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "valueColor", "strokeWidth" -> new PropertyValue.NullValue();
            case "strokeAlign" -> new PropertyValue.DoubleValue(new BigDecimal("3.5"));
            case "strokeCap" -> new PropertyValue.EnumValue("StrokeCap", "round");
            case "semanticsLabel" -> new PropertyValue.StringValue("Refresh progress");
            case "semanticsValue" -> new PropertyValue.StringValue("45%");
            case "elevation" -> new PropertyValue.DoubleValue(new BigDecimal("6.5"));
            case "indicatorMargin" -> new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "indicatorPadding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.valueOf(5), BigDecimal.valueOf(6), BigDecimal.valueOf(7), BigDecimal.valueOf(8));
            default -> throw new IllegalArgumentException(name);
        };
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
}
