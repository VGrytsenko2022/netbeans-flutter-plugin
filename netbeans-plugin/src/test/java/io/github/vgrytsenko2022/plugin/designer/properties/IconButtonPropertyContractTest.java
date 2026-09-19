package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class IconButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("b7900004-e530-4b9b-92fa-49e3c491f094");

    @Test void all524RowsHaveStableTypedCellsAndOptionalResetWithoutSlotMutation() throws Exception {
        var initial = withIcons(); var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(524, DEF.properties().size()); assertEquals(498, IconButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(505, full("standard", false, false).size()); assertEquals(478, full("outlined", true, true, true).size());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var props = new LinkedHashMap<>(initial.properties());
            props.putAll(sparsePrerequisites(name));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); var editor = cell.getPropertyEditor(); var value = value(field);
            assertTrue(cell.canWrite(), name); assertEquals(!field.parameter().required(), cell.supportsDefaultValue(), name);
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue(), name);
            if (!editor.supportsCustomEditor()) { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue(), name); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), name);
            props = apply(props, commands.getFirst()); assertEquals(value, props.get(field.name()), name);
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass());
            assertEquals(List.of(sets), List.of(node.getPropertySets())); commands.clear();
            if (!field.parameter().required()) { cell.restoreDefaultValue(); assertEquals(1, commands.size(), name); assertFalse(apply(props, commands.getFirst()).containsKey(field.name()), name); }
        }
    }

    @Test void everySparseCellAndDenseConstructorFamilyIsValidWithinPersistedBudget() throws Exception {
        var initial = withIcons(); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands);
        for (var field : DEF.properties()) {
            var props = new LinkedHashMap<>(initial.properties()); props.putAll(sparsePrerequisites(field.name().value()));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell(node, field.name().value()).setValue(FlutterPropertyCellValue.explicit(value(field)));
            assertEquals(1, commands.size(), field.name().value());
            props = apply(props, commands.getFirst()); validate(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), field.name().value());
            assertTrue(props.size() <= 6, field.name().value());
        }
        for (String variant : IconButtonWidgetPropertySchema.variants()) for (boolean paint : List.of(false, true))
            for (boolean circle : List.of(false, true)) {
                var props = full(variant, paint, paint, circle); assertTrue(props.size() <= 512);
                validate(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), variant + paint + circle);
            }
    }

    @Test void allSixteenConstructorTransitionsRetainBothSlotsAndEveryExplicitProperty() throws Exception {
        var initial = withIcons(); var commands = new ArrayList<DesignerCommand>();
        for (String before : IconButtonWidgetPropertySchema.variants()) for (String after : IconButtonWidgetPropertySchema.variants()) {
            var props = full(before, false, false); var widget = new WidgetNode(ID, DEF.typeId(), props, initial.slots());
            var node = node(widget, commands); commands.clear();
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(after)));
            if (before.equals(after)) assertTrue(commands.isEmpty());
            else {
                assertEquals(List.of(new SetProperty(ID, new PropertyName("variant"), new PropertyValue.StringValue(after))), commands);
                var edited = apply(props, commands.getFirst()); props.put(new PropertyName("variant"), new PropertyValue.StringValue(after));
                assertEquals(props, edited); validate(new WidgetNode(ID, DEF.typeId(), edited, initial.slots()), before + after);
            }
            assertEquals(initial.slots(), widget.slots());
        }
    }

    @Test void selectionIsThreeStateOptionalBooleanAndNeverDropsSelectedIcon() throws Exception {
        var widget = withIcons(); var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
        var cell = cell(node, "isSelected"); var editor = cell.getPropertyEditor();
        for (var selected : List.<PropertyValue>of(new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true), new PropertyValue.NullValue())) {
            editor.setValue(FlutterPropertyCellValue.explicit(selected)); editor.setAsText(editor.getAsText());
            assertEquals(FlutterPropertyCellValue.explicit(selected), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(selected));
            var props = apply(widget.properties(), commands.getFirst());
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, widget.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell.restoreDefaultValue(); assertFalse(apply(props, commands.getFirst()).containsKey(new PropertyName("isSelected")));
            assertTrue(((WidgetSlot.SingleSlot) widget.slots().get(new SlotName("selectedIcon"))).child().isPresent());
            node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        }
        editor.setAsText("<not set>"); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
        editor.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, FlutterTypedPropertyEditors.binding(field("isSelected")).orElseThrow().editorKind());
    }

    @Test void wholeStyleAndAll498LeavesSwitchInOnePatchWithoutConstructorSideEffects() throws Exception {
        var initial = withIcons(); var props = new LinkedHashMap<>(initial.properties());
        for (String name : IconButtonWidgetPropertySchema.localStyleProperties()) props.put(new PropertyName(name), value(name));
        var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), commands);
        cell(node, "style").setValue(FlutterPropertyCellValue.explicit(reference("_style")));
        var command = assertInstanceOf(PatchProperties.class, commands.getFirst()); assertEquals(499, command.patches().size());
        var cleared = apply(props, command); assertEquals(3, cleared.size()); assertEquals(reference("_style"), cleared.get(new PropertyName("style")));
        for (String name : IconButtonWidgetPropertySchema.localStyleProperties()) {
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), cleared, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            var edited = apply(cleared, commands.getFirst()); assertFalse(edited.containsKey(new PropertyName("style")), name);
            assertEquals(value(name), edited.get(new PropertyName(name))); assertEquals(cleared.get(new PropertyName("variant")), edited.get(new PropertyName("variant")));
        }
    }

    @Test void numericDomainsReferencesAndOldButtonEditorsRemainNarrow() {
        for (String density : List.of("visualDensityHorizontal", "visualDensityVertical")) {
            var editor = cell(node(withIcons(), new ArrayList<>()), density).getPropertyEditor();
            for (String text : List.of("-4", "4", "0.5")) editor.setAsText(text);
            for (String text : List.of("-4.01", "4.01", "Infinity", "null", "1e400")) assertThrows(IllegalArgumentException.class, () -> editor.setAsText(text));
        }
        for (String name : List.of("onPressed", "onLongPress", "onHover", "focusNode", "statesController", "style", "styleBackgroundBuilder", "styleForegroundBuilder")) {
            var editor = cell(node(withIcons(), new ArrayList<>()), name).getPropertyEditor();
            editor.setValue(FlutterPropertyCellValue.explicit(reference("_value"))); assertTrue(editor.supportsCustomEditor());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("() => rawCode()"));
        }
        assertTrue(DEF.property(new PropertyName("child")).isEmpty()); assertTrue(DEF.property(new PropertyName("iconAlignment")).isEmpty());
        assertEquals(288, BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.ElevatedButton")).orElseThrow().properties().size());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, boolean directional, boolean paint) { return full(variant, directional, paint, false); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, boolean directional, boolean paint, boolean circles) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (name.equals("style")) continue;
            if (paint ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (circles ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            var value = value(field);
            if (name.equals("variant")) value = new PropertyValue.StringValue(variant);
            if (circles && name.endsWith("ShapeKind")) value = new PropertyValue.StringValue("circle");
            if (name.equals("padding")) value = directional ? new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)
                    : new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
            if (name.equals("alignment")) value = new PropertyValue.AlignmentGeometryValue(directional ? PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL : PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL, BigDecimal.ZERO, BigDecimal.ZERO);
            result.put(field.name(), value);
        }
        return result;
    }
    public static PropertyDefinition field(String name) { return DEF.property(new PropertyName(name)).orElseThrow(); }
    public static PropertyValue value(String name) { return value(field(name)); }
    public static PropertyValue value(PropertyDefinition field) {
        return switch (field.name().value()) {
            case "variant" -> new PropertyValue.StringValue("filled");
            case "alignment" -> new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL, BigDecimal.ZERO, BigDecimal.ONE);
            case "constraints" -> new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.of(BigDecimal.valueOf(100)), BigDecimal.ZERO, Optional.of(BigDecimal.valueOf(100)));
            case "splashRadius" -> new PropertyValue.DoubleValue(BigDecimal.valueOf(24));
            default -> TextButtonPropertyContractTest.value(field);
        };
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) { return FilledButtonPropertyContractTest.sparsePrerequisites(name); }
    public static LinkedHashMap<PropertyName, PropertyValue> apply(Map<PropertyName, PropertyValue> before, DesignerCommand command) { return FilledButtonPropertyContractTest.apply(before, command); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return TextButtonPropertyContractTest.reference(name); }
    private static void validate(WidgetNode widget, String message) {
        var region = new ManagedRegion("0".repeat(64));
        var source = new DartSourceDescriptor("button.dart", "ButtonScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region));
        var result = new io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator().validate(new DesignerDocument(StableId.random(), source, widget), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.valid(), () -> message + ": " + result.errors());
    }
    private static WidgetNode withIcons() {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        var icon = new WidgetNode(StableId.parse("b8900004-e530-4b9b-92fa-49e3c491f094"), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Icon")), Map.of());
        var selected = new WidgetNode(StableId.parse("b9900004-e530-4b9b-92fa-49e3c491f094"), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Selected")), Map.of());
        return new WidgetNode(ID, DEF.typeId(), initial.properties(), Map.of(new SlotName("icon"), WidgetSlot.SingleSlot.of(icon), new SlotName("selectedIcon"), WidgetSlot.SingleSlot.of(selected)));
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
}
