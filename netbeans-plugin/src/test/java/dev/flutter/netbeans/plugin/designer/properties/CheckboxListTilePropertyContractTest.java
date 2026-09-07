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

public class CheckboxListTilePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE).orElseThrow();

    @Test void all154FieldsKeepStableCellsEditorsAndTypedSparseResets() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(154, DEF.properties().size()); assertEquals(3, DEF.slots().size()); var seen = new HashSet<String>();
        assertEquals(Map.of(p("value"), new PropertyValue.BooleanValue(false), p("onChanged"), new PropertyValue.StringValue("noop"), p("variant"), new PropertyValue.StringValue("standard")), prototype.properties());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(prototype.properties()); properties.putAll(sparsePrerequisites(name));
            var before = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); var editor = cell.getPropertyEditor(); var explicit = value(name);
            assertTrue(seen.add(name)); assertTrue(cell.canWrite()); assertEquals(!field.parameter().required(), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(FlutterPropertyCellValue.explicit(explicit), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(explicit, after.properties().get(field.name()));
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear();
            if (!field.parameter().required()) {
                cell.restoreDefaultValue(); assertEquals(1, commands.size(), name); var reset = apply(after, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name()));
            } else {
                assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset())); cell.restoreDefaultValue(); assertTrue(commands.isEmpty());
            }
        }
        assertEquals(154, seen.size());
    }

    @Test void constructorsAndBothIndependentTenShapeFamiliesPreservePeersAndSlots() throws Exception {
        for (String variant : CheckboxListTileWidgetPropertySchema.variants()) for (String shape : CheckboxListTileWidgetPropertySchema.shapeKinds()) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(variant, shape, shape, true), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(variant.equals("standard") ? "adaptive" : "standard")));
            var after = apply(before, commands.removeFirst()); assertValid(after); assertEquals(before.slots(), after.slots());
            var peers = new LinkedHashMap<>(before.properties()); peers.put(p("variant"), after.properties().get(p("variant"))); assertEquals(peers, after.properties());
            for (String family : CheckboxListTileWidgetPropertySchema.shapeFamilies()) for (String kind : CheckboxListTileWidgetPropertySchema.shapeKinds()) {
                commands.clear(); cell(node, family + "Kind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(kind)));
                if (kind.equals(shape)) { assertTrue(commands.isEmpty()); continue; }
                var switched = apply(before, commands.getFirst()); assertValid(switched); assertEquals(before.slots(), switched.slots());
                before.properties().forEach((name, value) -> { if (!CheckboxListTileWidgetPropertySchema.shapeLocalProperties(family).contains(name.value())) assertEquals(value, switched.properties().get(name)); });
            }
        }
        assertEquals("ShapeBorder", referenceType("shape")); assertEquals("OutlinedBorder", referenceType("checkboxShape"));
    }

    @Test void wholeReferencesClearOnlyTheirOwnLocalFamiliesAndLeafEditsRestoreOnlyThatFamily() throws Exception {
        for (String family : List.of("shape", "checkboxShape", "side", "fillColor", "overlayColor", "visualDensity", "mouseCursor")) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(), prototype.slots()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var local = locals(family);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(value(family))); assertEquals(1, commands.size()); var whole = apply(before, commands.getFirst()); assertValid(whole);
            local.forEach(name -> assertFalse(whole.properties().containsKey(p(name)), name));
            before.properties().forEach((name, value) -> { if (!local.contains(name.value())) assertEquals(value, whole.properties().get(name)); });
            for (String name : local) {
                var properties = new LinkedHashMap<>(whole.properties()); properties.putAll(sparsePrerequisites(name));
                if (!sparsePrerequisites(name).isEmpty()) properties.remove(p(family));
                var sparse = new WidgetNode(before.id(), DEF.typeId(), properties, before.slots()); assertValid(sparse);
                node.refreshPresentation(sparse, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name))); assertEquals(1, commands.size(), name);
                var restored = apply(sparse, commands.getFirst()); assertValid(restored); assertFalse(restored.properties().containsKey(p(family)));
            }
        }
    }

    @Test void requiredMixedValueNullableEnabledAndCallbackModesAreIndependent() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands);
        cell(node, "value").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); var mixed = apply(prototype, commands.removeFirst()); assertValid(mixed);
        assertEquals(new PropertyValue.BooleanValue(true), mixed.properties().get(p("tristate")));
        for (boolean reset : List.of(false, true)) {
            node.refreshPresentation(mixed, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            if (reset) cell(node, "tristate").restoreDefaultValue(); else cell(node, "tristate").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
            var after = apply(mixed, commands.removeFirst()); assertValid(after); assertEquals(new PropertyValue.BooleanValue(false), after.properties().get(p("value")));
        }
        for (PropertyValue callback : List.of(new PropertyValue.StringValue("noop"), new PropertyValue.NullValue(), reference("_onChanged"))) {
            var properties = new LinkedHashMap<>(prototype.properties()); properties.put(p("onChanged"), callback);
            var before = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots());
            for (PropertyValue enabled : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true), new PropertyValue.NullValue())) {
                node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, "enabled").setValue(FlutterPropertyCellValue.explicit(enabled)); var after = apply(before, commands.getFirst()); assertValid(after);
                assertEquals(callback, after.properties().get(p("onChanged"))); assertEquals(enabled, after.properties().get(p("enabled")));
            }
        }
    }

    @Test void threeLineRequiresSubtitleAndCursorStatesRequireUserSuppliedDefault() throws Exception {
        var empty = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var commands = new ArrayList<DesignerCommand>(); var node = node(empty, commands);
        var emptyNode = node;
        var threeLineError = assertThrows(IllegalArgumentException.class,
                () -> cell(emptyNode, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true))));
        assertTrue(threeLineError.getMessage().contains("Subtitle"));
        assertTrue(commands.isEmpty());
        var cursorError = assertThrows(IllegalArgumentException.class,
                () -> cell(emptyNode, "mouseCursorPressed").setValue(FlutterPropertyCellValue.explicit(value("mouseCursorPressed"))));
        assertTrue(cursorError.getMessage().contains("mouseCursorDefault"));
        assertTrue(commands.isEmpty());
        var prototype = prototype(); var properties = new LinkedHashMap<>(prototype.properties()); properties.put(p("mouseCursorDefault"), value("mouseCursorDefault")); properties.put(p("mouseCursorPressed"), value("mouseCursorPressed"));
        var map = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots()); node = node(map, commands); var mapNode = node;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> cell(mapNode, "mouseCursorDefault").restoreDefaultValue()).getMessage().contains("remaining local state")); assertTrue(commands.isEmpty());
    }

    @Test void nullableAndNonNullableNumbersKeepClosedConstantsWithoutWideningExistingFields() {
        var node = node(prototype(), new ArrayList<>()); var names = new ArrayList<>(CheckboxListTileWidgetPropertySchema.geometryProperties()); names.add("splashRadius"); names.add("checkboxScaleFactor");
        for (String name : names) {
            var editor = cell(node, name).getPropertyEditor();
            for (String token : List.of("Infinity", "-Infinity", "NaN")) { editor.setAsText(token); assertEquals(token, editor.getAsText()); }
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("double.nan"));
            if (name.equals("checkboxScaleFactor")) assertThrows(IllegalArgumentException.class, () -> editor.setAsText("null"));
            else { editor.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue()); }
        }
        var slider = BuiltInWidgetCatalog.getDefault().find(SliderWidgetPropertySchema.SLIDER_TYPE).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> FlutterTypedPropertyEditors.binding(slider.property(p("value")).orElseThrow()).orElseThrow().createEditor().setAsText("NaN"));
    }

    @Test void everyStatefulSideModeAndResetRetainsBaseAndOtherBuckets() throws Exception {
        for (String state : CheckboxListTileWidgetPropertySchema.sideStates()) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(), prototype.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); var bucket = CheckboxListTileWidgetPropertySchema.sideBucketProperties(state);
            cell(node, bucket.getFirst()).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("inherit"))); var inherit = apply(before, commands.removeFirst()); assertValid(inherit);
            bucket.stream().skip(1).forEach(name -> assertFalse(inherit.properties().containsKey(p(name))));
            node.refreshPresentation(inherit, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, bucket.get(1)).setValue(FlutterPropertyCellValue.explicit(value(bucket.get(1)))); var border = apply(inherit, commands.removeFirst()); assertValid(border);
            assertEquals(new PropertyValue.StringValue("border"), border.properties().get(p(bucket.getFirst())));
            node.refreshPresentation(border, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); cell(node, "sideStateful").restoreDefaultValue();
            var plain = apply(border, commands.removeFirst()); assertValid(plain); CheckboxListTileWidgetPropertySchema.sideStateProperties().forEach(name -> assertFalse(plain.properties().containsKey(p(name))));
            CheckboxListTileWidgetPropertySchema.sideBaseProperties().forEach(name -> assertEquals(before.properties().get(p(name)), plain.properties().get(p(name))));
        }
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full("standard", "roundedRectangle", "roundedRectangle", true); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shapeKind, String checkboxShapeKind, boolean stateful) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); if (List.of("shape", "checkboxShape", "side", "fillColor", "overlayColor", "visualDensity", "mouseCursor").contains(name)) continue;
            if (!stateful && CheckboxListTileWidgetPropertySchema.sideStateProperties().contains(name)) continue;
            var family = CheckboxListTileWidgetPropertySchema.shapeFamily(name);
            if (family.isPresent() && !CheckboxListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, family.orElseThrow().equals("shape") ? shapeKind : checkboxShapeKind)) continue;
            result.put(field.name(), switch (name) {
                case "variant" -> new PropertyValue.StringValue(variant);
                case "shapeKind" -> new PropertyValue.StringValue(shapeKind);
                case "checkboxShapeKind" -> new PropertyValue.StringValue(checkboxShapeKind);
                case "sideStateful" -> new PropertyValue.BooleanValue(stateful);
                default -> value(name);
            });
        }
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name) && !name.equals("mouseCursorDefault")
                ? Map.of(p("mouseCursorDefault"), value("mouseCursorDefault")) : Map.of();
    }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        var source = CheckboxListTileWidgetPropertySchema.shapeSourceName(name); if (source.isPresent()) return CardPropertyContractTest.value(source.orElseThrow());
        if (CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return new PropertyValue.StringValue("click");
        return switch (name) {
            case "variant" -> new PropertyValue.StringValue("adaptive");
            case "value", "tristate", "autofocus", "isError", "selected", "isThreeLine", "dense", "enableFeedback", "internalAddSemanticForOnTap", "sideStateful" -> new PropertyValue.BooleanValue(true);
            case "enabled" -> new PropertyValue.BooleanValue(false);
            case "shape", "checkboxShape", "side", "fillColor", "overlayColor", "onChanged", "onFocusChange", "focusNode", "statesController" -> reference("_" + name);
            case "visualDensity" -> reference("_density");
            case "mouseCursor" -> reference("_cursor");
            case "contentPadding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "controlAffinity" -> new PropertyValue.EnumValue("ListTileControlAffinity", "leading");
            case "titleAlignment" -> new PropertyValue.EnumValue("ListTileTitleAlignment", "center");
            case "checkboxSemanticLabel" -> new PropertyValue.StringValue("Accept terms");
            default -> CheckboxPropertyContractTest.value(name);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var base = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("title", "subtitle", "secondary")) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(
                new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue(name)), Map.of())));
        return new WidgetNode(base.id(), DEF.typeId(), base.properties(), slots);
    }
    private static List<String> locals(String family) {
        return CheckboxListTileWidgetPropertySchema.shapeFamilies().contains(family) ? CheckboxListTileWidgetPropertySchema.shapeLocalProperties(family)
                : family.equals("side") ? CheckboxListTileWidgetPropertySchema.sideLocalProperties()
                : family.equals("visualDensity") ? List.of("visualDensityHorizontal", "visualDensityVertical")
                : family.equals("mouseCursor") ? CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties() : CheckboxListTileWidgetPropertySchema.colorStateProperties(family);
    }
    private static String referenceType(String name) { return DEF.property(p(name)).orElseThrow().constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance).map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType(); }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
    static void assertValid(WidgetNode widget) { ListTilePropertyContractTest.assertValid(widget); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
