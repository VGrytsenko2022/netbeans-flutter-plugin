package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class CircularProgressIndicatorPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900002-e530-4b9b-92fa-49e3c491f092");
    public static final Map<PropertyName, PropertyValue> MATERIAL = Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material"));

    @Test void allFifteenRowsReuseTypedEditorsAndRetainGroupsCellsAndOnlyRequiredSelector() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertEquals(MATERIAL, initial.properties()); assertTrue(initial.slots().isEmpty());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(15, DEF.properties().size()); assertEquals(5, sets.length);
        var progress = Arrays.stream(sets).filter(set -> set.getName().equals("circularProgressIndicatorProgress")).findFirst().orElseThrow();
        assertEquals(List.of("variant", "value", "controller"), Arrays.stream(progress.getProperties()).map(Node.Property::getName).toList());
        for (var field : DEF.properties()) {
            String name = field.name().value(); node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); assertTrue(cell.canWrite()); assertEquals(!name.equals("variant"), cell.supportsDefaultValue());
            assertEquals(name.equals("variant") ? FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("material")) : FlutterPropertyCellValue.unset(), cell.getValue());
            var editor = cell.getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue());
            if (List.of("valueColor", "controller", "constraints", "padding").contains(name)) assertTrue(editor.supportsCustomEditor());
            else { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue()); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new SetProperty(ID, new PropertyName(name), value(name))), commands);
            var values = new HashMap<>(MATERIAL); values.put(field.name(), value(name));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), values, Map.of()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            if (!name.equals("variant")) {
                commands.clear(); cell.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(ID, new PropertyName(name))), commands);
            } else {
                assertArrayEquals(new String[]{"material", "adaptive"}, editor.getTags());
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText("<not set>"));
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText("cupertino"));
            }
        }
        for (String name : List.of("backgroundColor", "color", "valueColor")) assertTrue(cell(node, name).getPropertyEditor().isPaintable());
        assertNull(cell(node, "year2023").getPropertyEditor().getTags()); assertTrue(cell(node, "year2023").getPropertyEditor().isPaintable());
        assertTrue(cell(node, "semanticsValue").getShortDescription().contains("45%"));
    }

    @Test void constructorSwitchAndColorEditAreAtomicAndNeverRecreateCells() throws Exception {
        var values = new HashMap<>(MATERIAL); values.put(new PropertyName("color"), value("color"));
        values.put(new PropertyName("valueColor"), new PropertyValue.NullValue());
        var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), values, Map.of()), commands);
        var variantCell = cell(node, "variant"); var colorCell = cell(node, "color"); var sets = node.getPropertySets();
        variantCell.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("adaptive")));
        assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.ResetPatch(new PropertyName("color")),
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"))))), commands);
        values.remove(new PropertyName("color")); values.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), values, Map.of()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(variantCell, cell(node, "variant")); assertSame(colorCell, cell(node, "color")); assertEquals(List.of(sets), List.of(node.getPropertySets()));
        assertTrue(colorCell.canWrite(), "Color remains reachable and selecting it switches constructor explicitly");
        commands.clear(); colorCell.setValue(FlutterPropertyCellValue.explicit(value("color")));
        assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("material")),
                new PatchProperties.SetPatch(new PropertyName("color"), value("color"))))), commands);
        commands.clear(); colorCell.restoreDefaultValue();
        assertTrue(commands.isEmpty(), "Resetting an already unset adaptive Color is a no-op");
        values.put(new PropertyName("variant"), new PropertyValue.StringValue("material")); values.put(new PropertyName("color"), value("color"));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), values, Map.of()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        assertSame(colorCell, cell(node, "color")); colorCell.restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(ID, new PropertyName("color"))), commands);
        assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), cell(node, "valueColor").getValue());
    }

    @Test void valueAndControllerSwitchAtomicallyWhileResetDoesNotInventOpposite() throws Exception {
        for (String name : List.of("value", "controller")) {
            String opposite = name.equals("value") ? "controller" : "value";
            var values = new HashMap<>(MATERIAL); values.put(new PropertyName(opposite), value(opposite));
            var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), values, Map.of()), commands);
            cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.ResetPatch(new PropertyName(opposite)),
                    new PatchProperties.SetPatch(new PropertyName(name), value(name))))), commands);
            commands.clear(); cell(node, opposite).restoreDefaultValue();
            assertEquals(List.of(new ResetProperty(ID, new PropertyName(opposite))), commands);
        }
    }

    @Test void sdkNumericDomainsPreserveSignedValuesWithoutClampingAndOnlyGapAdmitsInfinity() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("value", "strokeWidth", "strokeAlign", "trackGap")) {
            var editor = cell(node, name).getPropertyEditor();
            for (String number : List.of("-2", "0", "1.5", "40")) {
                editor.setAsText(number); var v = ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow();
                assertEquals(0, new BigDecimal(number).compareTo(v instanceof PropertyValue.IntegerValue i ? new BigDecimal(i.value()) : ((PropertyValue.DoubleValue) v).value()));
            }
            var before = editor.getValue();
            for (String invalid : List.of("NaN", "-Infinity", "double.infinity", "1 + 2", "1e999")) {
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid)); assertEquals(before, editor.getValue());
            }
            if (name.equals("trackGap")) { editor.setAsText("Infinity"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "infinity")), editor.getValue()); }
            else assertThrows(IllegalArgumentException.class, () -> editor.setAsText("Infinity"));
        }
        var cap = cell(node, "strokeCap").getPropertyEditor();
        for (String value : List.of("butt", "round", "square")) {
            cap.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("StrokeCap", value)));
            cap.setAsText(cap.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("StrokeCap", value)), cap.getValue());
        }
    }

    @Test void fullLayoutsAndEveryNullableAnimationBranchAreTypedAndDistinctFromUnset() throws Exception {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (PropertyValue animation : List.of(new PropertyValue.ColorValue(0x80123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.tertiary")),
                new PropertyValue.NullValue(), LinearProgressIndicatorPropertyContractTest.reference("_valueColor"))) {
            var editor = cell(node, "valueColor").getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(animation));
            assertFalse(editor.getAsText().contains("<not set>")); assertThrows(IllegalArgumentException.class, () -> editor.setAsText("ColorTween(begin: Colors.red)"));
        }
        for (PropertyValue padding : List.of(value("padding"),
                new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4)))) {
            var editor = cell(node, "padding").getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(padding));
            assertEquals(FlutterPropertyCellValue.explicit(padding), editor.getValue());
        }
        var constraints = new PropertyValue.BoxConstraintsValue(PropertyValue.BoxConstraintBound.Infinity.INSTANCE, PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.finite(BigDecimal.ZERO), PropertyValue.BoxConstraintBound.Infinity.INSTANCE);
        var editor = cell(node, "constraints").getPropertyEditor(); editor.setValue(FlutterPropertyCellValue.explicit(constraints));
        assertEquals(FlutterPropertyCellValue.explicit(constraints), editor.getValue());
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "variant" -> new PropertyValue.StringValue("adaptive");
            case "value" -> new PropertyValue.DoubleValue(new BigDecimal("1.5"));
            case "backgroundColor" -> new PropertyValue.ColorValue(0x80123456L);
            case "color" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "valueColor" -> new PropertyValue.NullValue();
            case "strokeWidth" -> new PropertyValue.DoubleValue(new BigDecimal("-2.5"));
            case "strokeAlign" -> new PropertyValue.DoubleValue(new BigDecimal("3.5"));
            case "strokeCap" -> new PropertyValue.EnumValue("StrokeCap", "round");
            case "semanticsLabel" -> new PropertyValue.StringValue("Download progress");
            case "semanticsValue" -> new PropertyValue.StringValue("45%");
            case "constraints" -> new PropertyValue.BoxConstraintsValue(BigDecimal.valueOf(24), Optional.of(BigDecimal.valueOf(80)), BigDecimal.valueOf(30), Optional.empty());
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "trackGap" -> new PropertyValue.DoubleValue(new BigDecimal("-2.5"));
            case "year2023" -> new PropertyValue.BooleanValue(false);
            case "controller" -> LinearProgressIndicatorPropertyContractTest.reference("_progressController");
            default -> throw new IllegalArgumentException(name);
        };
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
}
