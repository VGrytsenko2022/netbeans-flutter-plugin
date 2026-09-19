package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class LinearProgressIndicatorPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900002-e530-4b9b-92fa-49e3c491f091");

    @Test void allThirteenOptionalRowsReuseTypedEditorsAndPreserveGroupsAndCells() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertTrue(initial.properties().isEmpty()); assertTrue(initial.slots().isEmpty());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(13, DEF.properties().size()); assertEquals(4, sets.length);
        for (var field : DEF.properties()) {
            String name = field.name().value(); node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); assertTrue(cell.canWrite()); assertTrue(cell.supportsDefaultValue());
            assertEquals(FlutterPropertyCellValue.unset(), cell.getValue());
            var editor = cell.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue());
            if (List.of("valueColor", "controller", "borderRadius").contains(name)) assertTrue(editor.supportsCustomEditor());
            else { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue()); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new SetProperty(ID, new PropertyName(name), value(name))), commands);
            var edited = new WidgetNode(ID, DEF.typeId(), Map.of(new PropertyName(name), value(name)), Map.of());
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear(); cell.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(ID, new PropertyName(name))), commands);
        }
        for (String name : List.of("backgroundColor", "color", "stopIndicatorColor", "valueColor")) assertTrue(cell(node, name).getPropertyEditor().isPaintable());
        assertNull(cell(node, "year2023").getPropertyEditor().getTags()); assertTrue(cell(node, "year2023").getPropertyEditor().isPaintable());
    }

    @Test void valueAndControllerSwitchInOneAtomicPatchWhileResetOnlyOmitsTheChosenField() throws Exception {
        for (String name : List.of("value", "controller")) {
            String opposite = name.equals("value") ? "controller" : "value";
            var initial = new WidgetNode(ID, DEF.typeId(), Map.of(new PropertyName(opposite), value(opposite),
                    new PropertyName("color"), value("color")), Map.of());
            var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands);
            cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(1, commands.size()); var patch = assertInstanceOf(PatchProperties.class, commands.getFirst());
            assertEquals(List.of(new PatchProperties.ResetPatch(new PropertyName(opposite)),
                    new PatchProperties.SetPatch(new PropertyName(name), value(name))), patch.patches());
            commands.clear(); cell(node, opposite).restoreDefaultValue();
            assertEquals(List.of(new ResetProperty(ID, new PropertyName(opposite))), commands);
        }
    }

    @Test void numericEditorsPreserveSdkValuesIncludingClosedPositiveInfinityWithoutClamping() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("value", "trackGap", "stopIndicatorRadius")) {
            var editor = cell(node, name).getPropertyEditor();
            for (String number : List.of("-2", "0", "1.5")) { editor.setAsText(number); assertEquals(new BigDecimal(number), numeric(((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow())); }
        }
        for (String name : List.of("minHeight", "trackGap", "stopIndicatorRadius")) {
            var editor = cell(node, name).getPropertyEditor(); editor.setAsText("Infinity");
            assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "infinity")), editor.getValue());
        }
        for (String name : List.of("value", "minHeight", "trackGap", "stopIndicatorRadius")) {
            var editor = cell(node, name).getPropertyEditor(); editor.setAsText("4.5"); var before = editor.getValue();
            for (String invalid : List.of("NaN", "-Infinity", "double.infinity", "1 + 2", "1e999")) { assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid)); assertEquals(before, editor.getValue()); }
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "value").getPropertyEditor().setAsText("Infinity"));
        for (String invalid : List.of("0", "-1")) assertThrows(IllegalArgumentException.class, () -> cell(node, "minHeight").getPropertyEditor().setAsText(invalid));
    }

    @Test void valueColorSupportsStoppedLiteralThemeNullReferenceAndDistinctUnsetWithoutDeletingColor() throws Exception {
        var initial = new WidgetNode(ID, DEF.typeId(), Map.of(new PropertyName("color"), value("color")), Map.of());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var cell = cell(node, "valueColor");
        for (PropertyValue value : List.of(new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.tertiary")),
                new PropertyValue.NullValue(), reference("_valueColor"))) {
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(List.of(new SetProperty(ID, new PropertyName("valueColor"), value)), commands);
            var editor = cell.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(value));
            assertFalse(editor.getAsText().contains("<not set>"));
            if (value instanceof PropertyValue.NullValue) assertEquals("AlwaysStoppedAnimation<Color?>(null)", editor.getAsText());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("ColorTween(begin: Colors.red)"));
        }
        assertEquals(value("color"), cell(node, "color").getValue().explicitValue().orElseThrow());
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "value" -> new PropertyValue.DoubleValue(new BigDecimal("1.5"));
            case "backgroundColor" -> new PropertyValue.ColorValue(0x80123456L);
            case "color" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "valueColor" -> new PropertyValue.NullValue();
            case "minHeight" -> new PropertyValue.DoubleValue(new BigDecimal("5.5"));
            case "semanticsLabel" -> new PropertyValue.StringValue("Download progress");
            case "semanticsValue" -> new PropertyValue.StringValue("Almost ready");
            case "borderRadius" -> new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                    new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.valueOf(2)),
                    new PropertyValue.BoxDecorationValue.Radius(BigDecimal.valueOf(3), BigDecimal.valueOf(4)),
                    new PropertyValue.BoxDecorationValue.Radius(BigDecimal.valueOf(5), BigDecimal.valueOf(6)),
                    new PropertyValue.BoxDecorationValue.Radius(BigDecimal.valueOf(7), BigDecimal.valueOf(8))));
            case "stopIndicatorColor" -> new PropertyValue.ColorValue(0xff00aabbL);
            case "stopIndicatorRadius" -> new PropertyValue.IntegerValue(BigInteger.valueOf(-3));
            case "trackGap" -> new PropertyValue.DoubleValue(new BigDecimal("-2.5"));
            case "year2023" -> new PropertyValue.BooleanValue(false);
            case "controller" -> reference("_progressController");
            default -> throw new IllegalArgumentException(name);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String symbol) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), symbol, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static BigDecimal numeric(PropertyValue value) { return value instanceof PropertyValue.IntegerValue integer ? new BigDecimal(integer.value()) : ((PropertyValue.DoubleValue) value).value(); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
}
