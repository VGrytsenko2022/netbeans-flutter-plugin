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

public class RefreshIndicatorPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault()
            .find(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900003-e530-4b9b-92fa-49e3c491f093");

    @Test void allThirteenStableRowsKeepExactValuesEditorsGroupsAndOptionalReset() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        assertEquals(Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material")), initial.properties());
        assertEquals(1, DEF.slots().size()); assertEquals(1, DEF.slots().getFirst().minChildren());
        assertEquals(13, DEF.properties().size());
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(7, sets.length);
        for (var field : DEF.properties()) {
            String name = field.name().value();
            var properties = new LinkedHashMap<>(initial.properties());
            if (name.equals("onStatusChange")) properties.put(new PropertyName("variant"), new PropertyValue.StringValue("noSpinner"));
            var state = new WidgetNode(ID, DEF.typeId(), properties, initial.slots());
            node.refreshPresentation(state, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); var editor = cell.getPropertyEditor();
            assertTrue(cell.canWrite()); assertEquals(!name.equals("variant"), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue());
            if (List.of("notificationPredicate", "onRefresh", "onStatusChange").contains(name)) {
                assertTrue(editor.supportsCustomEditor());
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText("() => arbitraryCode()"));
            } else { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value(name)), editor.getValue()); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new SetProperty(ID, field.name(), value(name))), commands);
            properties.put(field.name(), value(name));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), properties, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass());
            assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear();
            if (!name.equals("variant")) { cell.restoreDefaultValue(); assertEquals(List.of(new ResetProperty(ID, field.name())), commands); }
        }
        assertTrue(cell(node, "onRefresh").getShortDescription().contains("onRefresh: () async {}"));
        assertTrue(cell(node, "strokeWidth").getShortDescription().contains("Explicit null is invalid"));
        assertTrue(cell(node, "semanticsValue").getShortDescription().contains("45%"));
        assertTrue(Arrays.stream(sets).flatMap(s -> Arrays.stream(s.getProperties())).anyMatch(p -> p.getShortDescription().contains("vertical ScrollView")));
    }

    @Test void noSpinnerAndStatusSwitchesAreSingleAtomicPatchesClearingExactlyFiveFields() throws Exception {
        var props = new LinkedHashMap<PropertyName, PropertyValue>();
        props.put(new PropertyName("variant"), new PropertyValue.StringValue("adaptive"));
        for (String name : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) props.put(new PropertyName(name), value(name));
        props.put(new PropertyName("onRefresh"), value("onRefresh"));
        props.put(new PropertyName("notificationPredicate"), value("notificationPredicate"));
        props.put(new PropertyName("elevation"), value("elevation"));
        var state = new WidgetNode(ID, DEF.typeId(), props, Map.of());
        var commands = new ArrayList<DesignerCommand>(); var node = node(state, commands);
        for (String target : List.of("variant", "onStatusChange")) {
            commands.clear();
            cell(node, target).setValue(FlutterPropertyCellValue.explicit(target.equals("variant") ? new PropertyValue.StringValue("noSpinner") : value(target)));
            var command = assertInstanceOf(PatchProperties.class, commands.getFirst());
            var expected = new ArrayList<PatchProperties.Patch>();
            for (String name : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) expected.add(new PatchProperties.ResetPatch(new PropertyName(name)));
            expected.add(new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("noSpinner")));
            if (target.equals("onStatusChange")) expected.add(new PatchProperties.SetPatch(new PropertyName(target), value(target)));
            assertEquals(new PatchProperties(ID, expected), command); assertEquals(1, commands.size());
        }
    }

    @Test void everySpinnerFieldSelectsMaterialAndLeavingNoSpinnerClearsOnlyStatus() throws Exception {
        var props = Map.of(new PropertyName("variant"), (PropertyValue) new PropertyValue.StringValue("noSpinner"),
                new PropertyName("onStatusChange"), value("onStatusChange"), new PropertyName("onRefresh"), value("onRefresh"));
        var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), props, Map.of()), commands);
        for (String name : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) {
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(List.of(new PatchProperties(ID, List.of(
                    new PatchProperties.ResetPatch(new PropertyName("onStatusChange")),
                    new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("material")),
                    new PatchProperties.SetPatch(new PropertyName(name), value(name))))), commands);
            commands.clear(); cell(node, name).restoreDefaultValue();
            assertTrue(commands.isEmpty(), "Reset of an already omitted field does not invent an edit or change the constructor");
        }
        for (String variant : List.of("material", "adaptive")) {
            commands.clear(); cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(variant)));
            assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.ResetPatch(new PropertyName("onStatusChange")),
                    new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue(variant))))), commands);
        }
        commands.clear(); cell(node, "onStatusChange").restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(ID, new PropertyName("onStatusChange"))), commands);
    }

    @Test void numericAndEnumDomainsRejectNullInfinityAndExpressionsWithoutClamping() {
        var node = node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>());
        for (String name : List.of("displacement", "edgeOffset", "strokeWidth", "elevation")) {
            var editor = cell(node, name).getPropertyEditor();
            boolean nonnegative = List.of("displacement", "elevation").contains(name);
            for (String number : nonnegative ? List.of("0", "2.5", "1000") : List.of("-4", "0", "2.5", "1000")) editor.setAsText(number);
            var before = editor.getValue();
            for (String bad : List.of("null", "Inherited (null)", "Infinity", "-Infinity", "NaN", "1e999", "2 + 3")) {
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText(bad)); assertEquals(before, editor.getValue());
            }
            if (nonnegative) assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-0.01"));
        }
        var trigger = cell(node, "triggerMode").getPropertyEditor();
        for (String mode : List.of("onEdge", "anywhere")) {
            trigger.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", mode)));
            trigger.setAsText(trigger.getAsText());
        }
        var variant = cell(node, "variant").getPropertyEditor();
        assertEquals(List.of("material", "adaptive", "noSpinner"), List.of(variant.getTags()));
        assertThrows(IllegalArgumentException.class, () -> variant.setAsText("<not set>"));
    }

    @Test void predicateRequiresReviewedUnionBindingAndAllCallbacksRemainTypedReferences() {
        for (String name : List.of("onRefresh", "onStatusChange", "notificationPredicate")) {
            var definition = DEF.properties().stream().filter(p -> p.name().value().equals(name)).findFirst().orElseThrow();
            var binding = name.equals("notificationPredicate") ? FlutterTypedPropertyEditors.binding(definition, Optional.empty(), false,
                    RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets()).orElseThrow() : FlutterTypedPropertyEditors.binding(definition).orElseThrow();
            assertEquals(name.equals("notificationPredicate") ? FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE
                    : FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE, binding.editorKind());
            for (PropertyValue reference : List.of(reference("_handler"), new PropertyValue.DartObjectReferenceValue(
                    Optional.of("package:demo/refresh.dart"), "Handlers", Optional.of("create"),
                    PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
                var editor = binding.createEditor(); editor.setValue(FlutterPropertyCellValue.explicit(reference));
                assertEquals(FlutterPropertyCellValue.explicit(reference), editor.getValue());
                assertThrows(IllegalArgumentException.class, () -> editor.setAsText("Handlers.create(1)"));
            }
            if (name.equals("notificationPredicate")) {
                assertTrue(FlutterTypedPropertyEditors.binding(definition).isEmpty());
                var editor = binding.createEditor();
                for (String preset : RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets()) {
                    editor.setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)));
                    assertEquals("Preset: " + preset, editor.getAsText());
                }
            }
        }
    }

    public static PropertyValue value(String name) {
        return switch (name) {
            case "variant" -> new PropertyValue.StringValue("adaptive");
            case "displacement" -> new PropertyValue.DoubleValue(new BigDecimal("48.5"));
            case "edgeOffset" -> new PropertyValue.DoubleValue(new BigDecimal("-3.5"));
            case "onRefresh" -> reference("_refresh");
            case "color" -> new PropertyValue.ColorValue(0x80123456L);
            case "backgroundColor" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "notificationPredicate" -> new PropertyValue.StringValue("all");
            case "semanticsLabel" -> new PropertyValue.StringValue("Refresh records");
            case "semanticsValue" -> new PropertyValue.StringValue("45%");
            case "strokeWidth" -> new PropertyValue.DoubleValue(new BigDecimal("-2.5"));
            case "triggerMode" -> new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", "anywhere");
            case "elevation" -> new PropertyValue.DoubleValue(new BigDecimal("5.5"));
            case "onStatusChange" -> reference("_status");
            default -> throw new IllegalArgumentException(name);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
}
