package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class FloatingActionButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("c3900004-e530-4b9b-92fa-49e3c491f094");

    @Test void all78RowsHaveStableTypedCellsWithIndependentResetAndRequiredConstructorActivation() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(78, DEF.properties().size()); assertEquals(9, sets.length);
        assertEquals(22, Arrays.stream(sets).filter(s -> s.getName().equals("floatingActionButtonShape")).findFirst().orElseThrow().getProperties().length);
        assertEquals(31, Arrays.stream(sets).filter(s -> s.getName().equals("floatingActionButtonExtendedTextStyle")).findFirst().orElseThrow().getProperties().length);
        for (var field : DEF.properties()) {
            var before = withPrerequisites(initial, field.name().value());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, field.name().value()); var editor = cell.getPropertyEditor(); var explicit = value(field.name().value());
            assertTrue(cell.canWrite(), field.name().value()); assertEquals(!field.parameter().required(), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(FlutterPropertyCellValue.explicit(explicit), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(1, commands.size(), field.name().value());
            var edited = apply(before, commands.getFirst()); assertValid(edited); assertEquals(explicit, edited.properties().get(field.name()));
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, field.name().value())); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            if (!field.parameter().required()) {
                commands.clear(); cell.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(edited, commands.getFirst()); assertValid(reset);
                assertFalse(reset.properties().containsKey(field.name()));
            } else assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset()));
            node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        }
        assertEquals(FloatingActionButtonWidgetPropertySchema.variants(), List.of(cell(node, "variant").getPropertyEditor().getTags()));
    }

    @Test void allFourConstructorTransitionsRemoveOnlyInapplicableScalarsAndNeverDeleteSlots() throws Exception {
        var initial = withLabel();
        for (String from : FloatingActionButtonWidgetPropertySchema.variants()) for (String to : FloatingActionButtonWidgetPropertySchema.variants()) {
            var widget = new WidgetNode(ID, DEF.typeId(), full(from), initial.slots()); assertValid(widget);
            var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(to)));
            if (from.equals(to)) { assertTrue(commands.isEmpty()); continue; }
            assertEquals(1, commands.size()); var result = apply(widget, commands.getFirst()); assertValid(result);
            assertEquals(initial.slots(), result.slots()); assertEquals(new PropertyValue.StringValue(to), result.properties().get(new PropertyName("variant")));
            for (var entry : widget.properties().entrySet()) if (!entry.getKey().value().equals("variant")) {
                if (FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(entry.getKey().value(), to)) assertEquals(entry.getValue(), result.properties().get(entry.getKey()));
                else assertFalse(result.properties().containsKey(entry.getKey()));
            }
        }
    }

    @Test void extendedSpecificEditsRequireLabelAndPreserveOccupiedIconUntilExplicitlyCleared() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>();
        for (String field : FloatingActionButtonWidgetPropertySchema.extendedOnlyProperties()) {
            var before = withPrerequisites(initial, field); var node = node(before, commands); commands.clear(); cell(node, field).setValue(FlutterPropertyCellValue.explicit(value(field)));
            var result = apply(before, commands.getFirst()); assertValid(result);
            assertEquals(new PropertyValue.StringValue("extended"), result.properties().get(new PropertyName("variant")));
        }
        var empty = WidgetNodePrototypeFactory.create(DEF, ID); var node = node(empty, commands);
        for (String field : List.of("variant", "extendedPadding", "extendedTextStyleFontSize")) {
            commands.clear(); var error = assertThrows(IllegalArgumentException.class, () -> cell(node, field).setValue(FlutterPropertyCellValue.explicit(value(field))));
            assertTrue(error.getMessage().contains("Child is empty")); assertTrue(commands.isEmpty());
        }
        var icon = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Icon")), Map.of());
        var slots = new LinkedHashMap<>(initial.slots()); slots.put(new SlotName("icon"), WidgetSlot.SingleSlot.of(icon));
        var widget = new WidgetNode(ID, DEF.typeId(), full("extended"), slots); node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (String target : List.of("standard", "small", "large")) {
            commands.clear(); var error = assertThrows(IllegalArgumentException.class, () -> cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(target))));
            assertTrue(error.getMessage().contains("Move or clear Icon first")); assertTrue(commands.isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "mini").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true))));
        assertEquals(icon, ((WidgetSlot.SingleSlot) widget.slots().get(new SlotName("icon"))).child().orElseThrow());
    }

    @Test void directMiniAndExtendedStateSelectOnlyCompatibleConstructors() throws Exception {
        var initial = withLabel();
        for (String from : FloatingActionButtonWidgetPropertySchema.variants()) for (String field : List.of("mini", "isExtended")) {
            var values = full(from); values.remove(new PropertyName(field)); var widget = new WidgetNode(ID, DEF.typeId(), values, initial.slots());
            var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, field).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false))); var result = apply(widget, commands.getFirst()); assertValid(result);
            String expected = field.equals("mini") || from.equals("small") || from.equals("large") ? "standard" : from;
            assertEquals(new PropertyValue.StringValue(expected), result.properties().get(new PropertyName("variant")));
        }
    }

    @Test void all100BuiltInShapeTransitionsAndCustomReferenceSwitchesRemainAtomic() throws Exception {
        var initial = withLabel();
        for (String from : FloatingActionButtonWidgetPropertySchema.shapeKinds()) for (String to : FloatingActionButtonWidgetPropertySchema.shapeKinds()) {
            var values = new LinkedHashMap<>(initial.properties()); values.put(new PropertyName("shapeKind"), new PropertyValue.StringValue(from));
            for (String field : FloatingActionButtonWidgetPropertySchema.builtInShapePropertyNames())
                if (!field.equals("shapeKind") && FloatingActionButtonWidgetPropertySchema.shapePropertyAppliesToKind(field, from)) values.put(new PropertyName(field), value(field));
            var widget = new WidgetNode(ID, DEF.typeId(), values, initial.slots()); assertValid(widget);
            var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, "shapeKind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(to)));
            if (from.equals(to)) { assertTrue(commands.isEmpty()); continue; }
            var result = apply(widget, commands.getFirst()); assertValid(result);
            assertEquals(new PropertyValue.StringValue(to), result.properties().get(new PropertyName("shapeKind")));
            node.refreshPresentation(result, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, "shape").setValue(FlutterPropertyCellValue.explicit(reference("_shape"))); var custom = apply(result, commands.getFirst()); assertValid(custom);
            assertTrue(custom.properties().keySet().stream().noneMatch(key -> FloatingActionButtonWidgetPropertySchema.builtInShapePropertyNames().contains(key.value())));
        }
    }

    @Test void extendedTextPaintAndColorEditsRetainUnrelatedLeavesAndUseOnePatch() throws Exception {
        var initial = withLabel();
        for (var names : List.of(List.of("Foreground", "Color"), List.of("Background", "BackgroundColor"))) {
            var values = full("extended"); String paint = "extendedTextStyle" + names.getFirst(), color = "extendedTextStyle" + names.getLast();
            var widget = new WidgetNode(ID, DEF.typeId(), values, initial.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, paint).setValue(FlutterPropertyCellValue.explicit(value(paint))); var edited = apply(widget, commands.getFirst()); assertValid(edited);
            assertFalse(edited.properties().containsKey(new PropertyName(color))); assertEquals(widget.slots(), edited.slots());
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, color).setValue(FlutterPropertyCellValue.explicit(value(color))); var restored = apply(edited, commands.getFirst()); assertValid(restored);
            assertFalse(restored.properties().containsKey(new PropertyName(paint))); assertEquals(values, restored.properties());
        }
    }

    @Test void exactHeroTagUnionDoesNotWidenNumericBooleanOrNullableColorEditors() {
        assertEquals(FlutterTypedPropertyEditors.EditorKind.OBJECT_TAG, FlutterTypedPropertyEditors.binding(field("heroTag")).orElseThrow().editorKind());
        var node = node(withLabel(), new ArrayList<>());
        var hero = cell(node, "heroTag").getPropertyEditor();
        for (var value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue(""), new PropertyValue.IntegerValue(java.math.BigInteger.ONE),
                new PropertyValue.DoubleValue(new BigDecimal("1.0")), new PropertyValue.BooleanValue(false), reference("_heroTag"))) {
            hero.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), hero.getValue()); assertFalse(hero.getAsText().isBlank());
        }
        assertThrows(IllegalArgumentException.class, () -> hero.setAsText("Object()"));
        assertThrows(IllegalArgumentException.class, () -> cell(node, "clipBehavior").getPropertyEditor().setAsText("null"));
        for (String name : List.of("elevation", "focusElevation", "hoverElevation", "highlightElevation", "disabledElevation", "extendedIconLabelSpacing"))
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NUMBER_WITH_INFINITY, FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().editorKind());
        var progress = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.RefreshProgressIndicator")).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_NUMBER, FlutterTypedPropertyEditors.binding(progress.property(new PropertyName("strokeWidth")).orElseThrow()).orElseThrow().editorKind());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (!FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(name, variant) || name.equals("shape")
                    || name.equals("extendedTextStyleForeground") || name.equals("extendedTextStyleBackground")) continue;
            if (FloatingActionButtonWidgetPropertySchema.builtInShapePropertyNames().contains(name)
                    && !FloatingActionButtonWidgetPropertySchema.shapePropertyAppliesToKind(name, "roundedRectangle")) continue;
            result.put(field.name(), name.equals("variant") ? new PropertyValue.StringValue(variant)
                    : name.equals("enabled") ? new PropertyValue.BooleanValue(true) : name.equals("heroTag") ? reference("_heroTag") : value(name));
        }
        return result;
    }

    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return name.equals("extendedTextStylePackage")
                ? Map.of(new PropertyName("extendedTextStyleFontFamily"), new PropertyValue.StringValue("Roboto")) : Map.of();
    }
    private static WidgetNode withPrerequisites(WidgetNode widget, String name) {
        var properties = new LinkedHashMap<>(widget.properties()); properties.putAll(sparsePrerequisites(name));
        if (!sparsePrerequisites(name).isEmpty()) properties.put(new PropertyName("variant"), new PropertyValue.StringValue("extended"));
        return new WidgetNode(widget.id(), widget.type(), properties, widget.slots());
    }
    public static PropertyDefinition field(String name) { return DEF.property(new PropertyName(name)).orElseThrow(); }
    public static PropertyValue value(String name) {
        if (name.equals("variant")) return new PropertyValue.StringValue("extended");
        if (name.equals("enabled")) return new PropertyValue.BooleanValue(false);
        if (List.of("mini", "isExtended", "autofocus", "enableFeedback").contains(name)) return new PropertyValue.BooleanValue(true);
        if (List.of("onPressed", "focusNode", "mouseCursor", "shape").contains(name)) return reference("_" + name);
        if (name.equals("heroTag")) return new PropertyValue.StringValue("fab-tag");
        if (name.equals("tooltip")) return new PropertyValue.StringValue("Create item");
        if (name.startsWith("extendedTextStyle")) return BadgePropertyContractTest.value("textStyle" + name.substring("extendedTextStyle".length()));
        if (name.startsWith("shape")) return CardPropertyContractTest.value(name);
        if (name.endsWith("Color")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        if (name.equals("extendedPadding")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        if (name.equals("clipBehavior")) return new PropertyValue.EnumValue("Clip", "antiAlias");
        if (name.equals("materialTapTargetSize")) return new PropertyValue.EnumValue("MaterialTapTargetSize", "shrinkWrap");
        return new PropertyValue.DoubleValue(new BigDecimal("2.5"));
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return new WidgetNode(widget.id(), widget.type(), FilledButtonPropertyContractTest.apply(widget.properties(), command), widget.slots()); }
    private static WidgetNode withLabel() {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var slots = new LinkedHashMap<>(initial.slots());
        slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue("Label")), Map.of())));
        return new WidgetNode(ID, DEF.typeId(), initial.properties(), slots);
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) {
        var region = new ManagedRegion("0".repeat(64));
        var result = new WidgetTreeValidator().validate(new DesignerDocument(StableId.random(),
                new DartSourceDescriptor("fab.dart", "FabScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.valid(), () -> result.errors().toString());
    }
}
