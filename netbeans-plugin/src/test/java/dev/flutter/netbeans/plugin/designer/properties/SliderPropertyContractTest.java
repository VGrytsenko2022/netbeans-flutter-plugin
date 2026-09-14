package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class SliderPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(SliderWidgetPropertySchema.SLIDER_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("07aa78c8-6a23-42e1-9f6a-daa4c21e22a5");

    @Test void all33CellsKeepStableSetsAndIndependentTypedEditsResets() throws Exception {
        var prototype = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(33, DEF.properties().size()); assertEquals(8, sets.length); assertTrue(DEF.slots().isEmpty());
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

    @Test void paddingAndConstructorSwitchesAreOnePatchAndPreserveAllIgnoredAdaptiveParameters() throws Exception {
        for (String variant : SliderWidgetPropertySchema.variants()) assertValid(new WidgetNode(ID, DEF.typeId(), full(variant), Map.of()));
        var before = new WidgetNode(ID, DEF.typeId(), full("standard"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("adaptive")));
        assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst()); var adaptive = apply(before, commands.getFirst()); assertValid(adaptive);
        var expected = new LinkedHashMap<>(before.properties()); expected.remove(p("padding")); expected.put(p("variant"), new PropertyValue.StringValue("adaptive")); assertEquals(expected, adaptive.properties());
        node.refreshPresentation(adaptive, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
        cell(node, "padding").setValue(FlutterPropertyCellValue.explicit(value("padding"))); assertEquals(1, commands.size());
        assertInstanceOf(PatchProperties.class, commands.getFirst()); assertEquals(before, apply(adaptive, commands.getFirst()));
        node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
        cell(node, "padding").restoreDefaultValue(); var reset = apply(before, commands.getFirst()); assertValid(reset); assertEquals(before.properties().get(p("variant")), reset.properties().get(p("variant")));
    }

    @Test void invalidProspectiveRangesAndResetsSubmitNothingAndNeverClampPeers() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full("standard"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        for (var invalid : Map.of("value", number("6"), "min", number("2.5"), "max", number("2.5"), "secondaryTrackValue", number("-3")).entrySet()) {
            var failure = assertThrows(IllegalArgumentException.class, () -> cell(node, invalid.getKey()).setValue(FlutterPropertyCellValue.explicit(invalid.getValue())));
            assertTrue(failure.getMessage().contains(invalid.getKey())); assertTrue(failure.getMessage().contains("Slider")); assertTrue(commands.isEmpty());
            assertEquals(FlutterPropertyCellValue.explicit(before.properties().get(p(invalid.getKey()))), cell(node, invalid.getKey()).getValue());
        }
        assertThrows(IllegalArgumentException.class, () -> cell(node, "max").restoreDefaultValue()); assertTrue(commands.isEmpty());
        cell(node, "secondaryTrackValue").setValue(FlutterPropertyCellValue.explicit(number("1"))); var belowMain = apply(before, commands.getFirst()); assertValid(belowMain);
        assertEquals(number("2"), belowMain.properties().get(p("value"))); assertEquals(number("1"), belowMain.properties().get(p("secondaryTrackValue")));
    }

    @Test void signedInfinityAndEqualEndpointRangesRemainExactButOldPositiveOnlySchemasRejectNegative() throws Exception {
        var infinity = new PropertyValue.EnumValue("double", "infinity"); var negative = new PropertyValue.EnumValue("double", "negativeInfinity");
        for (var endpoint : List.of(infinity, negative)) {
            var values = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(DEF, ID).properties());
            for (String name : SliderWidgetPropertySchema.rangeProperties()) values.put(p(name), endpoint);
            var equal = new WidgetNode(ID, DEF.typeId(), values, Map.of()); assertValid(equal); assertTrue(SliderWidgetPropertySchema.rangeError(equal).isEmpty());
        }
        for (String name : SliderWidgetPropertySchema.rangeProperties()) {
            var editor = FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().createEditor();
            for (String text : List.of("Infinity", "-Infinity")) {
                editor.setAsText(text); assertEquals(text, editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(text.startsWith("-") ? negative : infinity), editor.getValue());
            }
            for (String text : List.of("double.negativeInfinity", "double.infinity", "NaN", "1e400")) assertThrows(IllegalArgumentException.class, () -> editor.setAsText(text));
        }
        for (var typeField : List.of(List.of("flutter.material.Switch", "splashRadius"), List.of("flutter.material.Switch", "trackOutlineWidthDefault"), List.of("flutter.material.CircleAvatar", "radius"))) {
            var def = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(typeField.getFirst())).orElseThrow().property(p(typeField.getLast())).orElseThrow();
            var editor = FlutterTypedPropertyEditors.binding(def).orElseThrow().createEditor(); assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-Infinity"));
        }
    }

    @Test void overlayWholeReferenceAndAllNineNullableStatesSwitchOnlyTheirFamily() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full("adaptive"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "overlayColor").setValue(FlutterPropertyCellValue.explicit(reference("_overlayColor"))); assertEquals(1, commands.size());
        var whole = apply(before, commands.getFirst()); assertValid(whole);
        for (String local : SliderWidgetPropertySchema.overlayColorStateProperties()) assertFalse(whole.properties().containsKey(p(local)));
        for (var peer : before.properties().entrySet()) if (!SliderWidgetPropertySchema.overlayColorStateProperties().contains(peer.getKey().value())) assertEquals(peer.getValue(), whole.properties().get(peer.getKey()));
        for (String local : SliderWidgetPropertySchema.overlayColorStateProperties()) for (PropertyValue value : List.of(value(local), new PropertyValue.ColorValue(0xff102030L), new PropertyValue.NullValue())) {
            node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, local).setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size()); var after = apply(whole, commands.getFirst()); assertValid(after);
            assertFalse(after.properties().containsKey(p("overlayColor"))); assertEquals(value, after.properties().get(p(local)));
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_THEME_COLOR, FlutterTypedPropertyEditors.binding(field(local)).orElseThrow().editorKind());
        }
    }

    @Test void optionalNullModesEnumsAndStrictReferencesStayTypedWithoutRawDart() throws Exception {
        var before = new WidgetNode(ID, DEF.typeId(), full("standard"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        for (String name : List.of("divisions", "secondaryTrackValue", "year2023")) {
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); assertEquals(1, commands.size()); assertValid(apply(before, commands.getFirst()));
        }
        for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "focusNode", "overlayColor")) {
            assertThrows(IllegalArgumentException.class, () -> cell(node, name).getPropertyEditor().setAsText("arbitrary()"));
            assertEquals(FlutterTypedPropertyEditors.EditorKind.DART_OBJECT_REFERENCE, FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().editorKind());
        }
        var divisions = cell(node, "divisions").getPropertyEditor();
        for (String invalid : List.of("0", "-1", "1.5", "Infinity", "9007199254740992")) assertThrows(IllegalArgumentException.class, () -> divisions.setAsText(invalid));
        divisions.setAsText("9007199254740991"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.IntegerValue(new BigInteger("9007199254740991"))), divisions.getValue());
        assertEquals(6, ((PropertyValueConstraint.EnumValues) field("showValueIndicator").constraints().getFirst()).values().size());
        assertEquals(4, ((PropertyValueConstraint.EnumValues) field("allowedInteraction").constraints().getFirst()).values().size());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, FlutterTypedPropertyEditors.binding(field("year2023")).orElseThrow().editorKind());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (name.equals("overlayColor") || variant.equals("adaptive") && name.equals("padding")) continue;
            result.put(field.name(), name.equals("variant") ? new PropertyValue.StringValue(variant) : name.equals("enabled") ? new PropertyValue.BooleanValue(true) : value(field));
        }
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return List.of("value", "secondaryTrackValue").contains(name) ? Map.of(p("max"), number("5")) : Map.of();
    }
    public static PropertyDefinition field(String name) { return DEF.property(p(name)).orElseThrow(); }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        return switch (name) {
            case "variant" -> new PropertyValue.StringValue("adaptive");
            case "enabled", "year2023" -> new PropertyValue.BooleanValue(false);
            case "autofocus" -> new PropertyValue.BooleanValue(true);
            case "value" -> number("2");
            case "secondaryTrackValue" -> number("3");
            case "min" -> number("-2");
            case "max" -> number("5");
            case "divisions" -> new PropertyValue.IntegerValue(BigInteger.valueOf(7));
            case "label" -> new PropertyValue.StringValue("Selected value");
            case "onChanged", "onChangeStart", "onChangeEnd", "semanticFormatterCallback", "focusNode", "mouseCursor", "overlayColor" -> reference("_" + name);
            case "allowedInteraction" -> new PropertyValue.EnumValue("SliderInteraction", "slideThumb");
            case "showValueIndicator" -> new PropertyValue.EnumValue("ShowValueIndicator", "alwaysVisible");
            case "padding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            default -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    private static PropertyValue.DoubleValue number(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("slider.dart", "SliderScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
