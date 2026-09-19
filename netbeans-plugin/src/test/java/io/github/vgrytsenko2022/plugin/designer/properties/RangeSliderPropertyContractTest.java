package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class RangeSliderPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("07aa78c8-6a23-42e1-9f6a-daa4c21e22a5");

    @Test void all37CellsRetainStableSetsAndIndependentTypedEditsResets() throws Exception {
        var prototype = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(37, DEF.properties().size()); assertEquals(10, sets.length); assertTrue(DEF.slots().isEmpty()); assertFalse(DEF.constConstructor());
        assertEquals(Map.of(p("valuesStart"), new PropertyValue.IntegerValue(BigInteger.ZERO), p("valuesEnd"), new PropertyValue.IntegerValue(BigInteger.ONE), p("enabled"), new PropertyValue.BooleanValue(true)), prototype.properties());
        for (var field : DEF.properties()) {
            var values = new LinkedHashMap<>(prototype.properties()); values.putAll(sparsePrerequisites(field.name().value()));
            var before = new WidgetNode(ID, DEF.typeId(), values, Map.of()); assertValid(before);
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, field.name().value()); var editor = cell.getPropertyEditor(); var next = value(field);
            assertTrue(cell.canWrite()); assertEquals(!field.parameter().required(), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(FlutterPropertyCellValue.explicit(next), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(next)); assertEquals(1, commands.size(), field.name().value());
            var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(next, after.properties().get(field.name()));
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, field.name().value())); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear();
            if (!field.parameter().required()) { cell.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(after, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); }
            else { assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset())); cell.restoreDefaultValue(); assertTrue(commands.isEmpty()); }
        }
    }

    @Test void labelsNullWholeReferenceAndEmptyLocalPeersSwitchOnlyTheirFamily() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of()); assertValid(before);
        var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        for (PropertyValue wholeValue : List.of(reference("_labels"), new PropertyValue.NullValue())) {
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, "labels").setValue(FlutterPropertyCellValue.explicit(wholeValue)); assertEquals(1, commands.size());
            assertInstanceOf(PatchProperties.class, commands.getFirst()); var whole = apply(before, commands.getFirst()); assertValid(whole);
            assertFalse(whole.properties().containsKey(p("labelsStart"))); assertFalse(whole.properties().containsKey(p("labelsEnd")));
            assertEquals(wholeValue, whole.properties().get(p("labels"))); assertPeersUnchanged(before, whole, List.of("labels", "labelsStart", "labelsEnd"));
            for (String local : List.of("labelsStart", "labelsEnd")) for (String label : List.of("", "quote' $value\nline\r\nlast")) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(label)));
                assertEquals(1, commands.size()); var after = apply(whole, commands.getFirst()); assertValid(after);
                assertEquals(new PropertyValue.StringValue(label), after.properties().get(p(local))); assertFalse(after.properties().containsKey(p("labels")));
                assertFalse(after.properties().containsKey(p(local.equals("labelsStart") ? "labelsEnd" : "labelsStart")));
                assertPeersUnchanged(whole, after, List.of("labels", "labelsStart", "labelsEnd"));
                node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).restoreDefaultValue(); var reset = apply(after, commands.getFirst()); assertValid(reset);
                assertFalse(reset.properties().containsKey(p(local))); assertFalse(reset.properties().containsKey(p("labels")));
            }
        }
    }

    @Test void invalidProspectiveRangesAndResetsSubmitNothingWithoutClampingPeers() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        for (var invalid : Map.of("valuesStart", number("4"), "valuesEnd", number("0"), "min", number("2"), "max", number("2")).entrySet()) {
            var failure = assertThrows(IllegalArgumentException.class, () -> cell(node, invalid.getKey()).setValue(FlutterPropertyCellValue.explicit(invalid.getValue())));
            assertTrue(failure.getMessage().contains(invalid.getKey())); assertTrue(failure.getMessage().contains("RangeSlider")); assertTrue(commands.isEmpty());
            assertEquals(FlutterPropertyCellValue.explicit(before.properties().get(p(invalid.getKey()))), cell(node, invalid.getKey()).getValue());
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "max").restoreDefaultValue()); assertTrue(commands.isEmpty());
        cell(node, "valuesStart").setValue(FlutterPropertyCellValue.explicit(number("3"))); var equal = apply(before, commands.getFirst()); assertValid(equal);
        assertEquals(number("3"), equal.properties().get(p("valuesEnd")));
    }

    @Test void stateColorAndCursorWholeLocalNullAndAllPresetsRemainIndependent() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full(), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        for (String family : List.of("overlayColor", "mouseCursor")) {
            var locals = family.equals("overlayColor") ? RangeSliderWidgetPropertySchema.overlayColorStateProperties() : RangeSliderWidgetPropertySchema.mouseCursorStateProperties();
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(reference("_" + family)));
            assertEquals(1, commands.size()); var whole = apply(before, commands.getFirst()); assertValid(whole);
            for (String local : locals) assertFalse(whole.properties().containsKey(p(local)));
            var allowed = new ArrayList<String>(locals); allowed.add(family); assertPeersUnchanged(before, whole, allowed);
            for (String local : locals) {
                List<PropertyValue> alternatives = family.equals("overlayColor") ? List.of(value(local), new PropertyValue.ColorValue(0xff102030L), new PropertyValue.NullValue())
                        : List.of(value(local), reference("_cursor"), new PropertyValue.NullValue());
                for (var alternate : alternatives) {
                    node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                    cell(node, local).setValue(FlutterPropertyCellValue.explicit(alternate)); assertEquals(1, commands.size());
                    var after = apply(whole, commands.getFirst()); assertValid(after); assertFalse(after.properties().containsKey(p(family)));
                    assertEquals(alternate, after.properties().get(p(local))); assertPeersUnchanged(whole, after, allowed);
                }
            }
        }
        assertEquals(41, RangeSliderWidgetPropertySchema.mouseCursorPresets().size());
        for (String preset : RangeSliderWidgetPropertySchema.mouseCursorPresets()) {
            var binding = binding("mouseCursorHovered"); binding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(preset)));
        }
    }

    @Test void numericNullAndSignedInfinityDomainsDoNotWidenUnrelatedEditors() throws Exception {
        for (String member : List.of("infinity", "negativeInfinity")) {
            var values = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(DEF, ID).properties());
            for (String name : RangeSliderWidgetPropertySchema.rangeProperties()) values.put(p(name), new PropertyValue.EnumValue("double", member));
            assertValid(new WidgetNode(ID, DEF.typeId(), values, Map.of()));
        }
        for (String name : RangeSliderWidgetPropertySchema.rangeProperties()) {
            var editor = binding(name).createEditor();
            for (String text : List.of("Infinity", "-Infinity")) { editor.setAsText(text); assertEquals(text, editor.getAsText()); }
            for (String invalid : List.of("NaN", "1e400", "double.infinity", "double.negativeInfinity", "null")) assertThrows(IllegalArgumentException.class, () -> editor.setAsText(invalid));
        }
        var divisions = binding("divisions").createEditor();
        for (String invalid : List.of("0", "-1", "1.5", "Infinity", "9007199254740992")) assertThrows(IllegalArgumentException.class, () -> divisions.setAsText(invalid));
        divisions.setAsText("9007199254740991"); divisions.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), divisions.getValue());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, binding("year2023").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_DART_REFERENCE, binding("labels").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.PRESET_DART_REFERENCE, binding("mouseCursorHovered").editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE, binding("mouseCursor").editorKind());
        var oldCursor = SliderPropertyContractTest.field("mouseCursor");
        var oldBinding = FlutterTypedPropertyEditors.binding(oldCursor, Optional.empty(), false, SliderWidgetPropertySchema.mouseCursorPresets()).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> oldBinding.validate(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "labels", "overlayColor", "mouseCursor"))
            assertThrows(IllegalArgumentException.class, () -> binding(name).createEditor().setAsText("arbitrary()"));
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) if (!List.of("labels", "overlayColor", "mouseCursor").contains(field.name().value()))
            result.put(field.name(), field.name().value().equals("enabled") ? new PropertyValue.BooleanValue(true) : value(field));
        result.put(p("valuesStart"), number("1")); result.put(p("valuesEnd"), number("3"));
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) { return Map.of(); }
    public static PropertyDefinition field(String name) { return DEF.property(p(name)).orElseThrow(); }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        if (RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return new PropertyValue.StringValue("click");
        return switch (name) {
            case "enabled", "year2023" -> new PropertyValue.BooleanValue(false);
            case "valuesStart" -> number("0.25");
            case "valuesEnd" -> number("0.75");
            case "min" -> number("-2");
            case "max" -> number("5");
            case "divisions" -> new PropertyValue.IntegerValue(BigInteger.valueOf(7));
            case "labelsStart" -> new PropertyValue.StringValue("Start value");
            case "labelsEnd" -> new PropertyValue.StringValue("End value");
            case "onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "labels", "mouseCursor", "overlayColor" -> reference("_" + name);
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            default -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    private static FlutterTypedPropertyEditors.Binding binding(String name) { return FlutterTypedPropertyEditors.binding(field(name), Optional.empty(), false, RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(name) ? RangeSliderWidgetPropertySchema.mouseCursorPresets() : List.of()).orElseThrow(); }
    private static PropertyValue.DoubleValue number(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertPeersUnchanged(WidgetNode before, WidgetNode after, List<String> allowed) { for (var entry : before.properties().entrySet()) if (!allowed.contains(entry.getKey().value())) assertEquals(entry.getValue(), after.properties().get(entry.getKey())); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("range_slider.dart", "RangeSliderScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
