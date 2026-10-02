package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.junit.jupiter.api.Assertions.*;

public class CardPropertyContractTest {
    private static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(CardWidgetPropertySchema.CARD_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("63ab7bb7-975b-477a-aaad-4b881ef2bafb");
    private static final PropertyValue.StringValue ELEVATED = s("elevated");

    @Test void all31RowsStayStableWithRequiredVariantAndOptionalCenteredBooleans() throws Exception {
        var widget = WidgetNodePrototypeFactory.create(DEF, ID);
        assertEquals(Map.of(p("variant"), ELEVATED), widget.properties());
        var commands = new ArrayList<DesignerCommand>();
        var node = node(widget, commands);
        var sets = node.getPropertySets();
        assertTrue(Arrays.stream(sets).flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(cell -> cell.getName().equals("child")).findFirst().orElseThrow()
                .getShortDescription().contains("Shape alone does not clip"));
        assertEquals(31, DEF.properties().size());
        assertEquals(22, Arrays.stream(sets).filter(v -> v.getName().equals("cardShape")).findFirst().orElseThrow().getProperties().length);
        for (var definition : DEF.properties()) {
            var cell = cell(node, definition.name().value());
            assertTrue(cell.canWrite());
            assertEquals(!definition.parameter().required(), cell.supportsDefaultValue());
            assertNotNull(cell.getPropertyEditor());
            if (definition.name().value().equals("variant")) continue;
            assertEquals(FlutterPropertyCellValue.unset(), cell.getValue());
            var value = value(definition.name().value());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(1, commands.size(), definition.name().value());
            var edited = apply(widget, commands.getFirst());
            assertEquals(value, edited.properties().get(definition.name()));
            assertValid(edited);
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, definition.name().value()));
            assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear(); cell.restoreDefaultValue(); assertEquals(1, commands.size());
            assertFalse(apply(edited, commands.getFirst()).properties().containsKey(definition.name()));
            node.refreshPresentation(widget, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        }
        var variant = cell(node, "variant");
        assertEquals(List.of("elevated", "filled", "outlined"), List.of(variant.getPropertyEditor().getTags()));
        commands.clear(); variant.restoreDefaultValue(); assertTrue(commands.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> variant.setValue(FlutterPropertyCellValue.unset()));
        assertThrows(IllegalArgumentException.class, () -> variant.getPropertyEditor().setAsText("other"));
        for (String name : List.of("borderOnForeground", "semanticContainer")) {
            var editor = cell(node, name).getPropertyEditor();
            assertTrue(editor.isPaintable()); assertNull(editor.getTags());
            for (boolean flag : List.of(true, false)) { editor.setAsText(Boolean.toString(flag)); assertEquals(new PropertyValue.BooleanValue(flag), ((FlutterPropertyCellValue) editor.getValue()).explicitValue().orElseThrow()); }
        }
    }

    @Test void all100KindTransitionsPruneOnlyIncompatibleDetailsAsOneCommand() throws Exception {
        for (String from : CardWidgetPropertySchema.shapeKinds()) for (String to : CardWidgetPropertySchema.shapeKinds()) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>(); values.put(p("variant"), ELEVATED); values.put(p("color"), value("color")); values.put(p("shapeKind"), s(from));
            for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) if (CardWidgetPropertySchema.isShapeDetailProperty(name) && CardWidgetPropertySchema.shapePropertyAppliesToKind(name, from)) values.put(p(name), value(name));
            var widget = new WidgetNode(ID, DEF.typeId(), values, Map.of()); assertValid(widget);
            var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, "shapeKind").setValue(FlutterPropertyCellValue.explicit(s(to)));
            if (from.equals(to)) { assertTrue(commands.isEmpty()); continue; }
            assertEquals(1, commands.size()); var result = apply(widget, commands.getFirst()); assertValid(result);
            assertEquals(s(to), result.properties().get(p("shapeKind")));
            for (var entry : values.entrySet()) if (!entry.getKey().value().equals("shapeKind")) {
                String name = entry.getKey().value();
                if (!name.startsWith("shape") || CardWidgetPropertySchema.shapePropertyAppliesToKind(name, to)) assertEquals(entry.getValue(), result.properties().get(entry.getKey()), from + "->" + to + ":" + name);
                else assertFalse(result.properties().containsKey(entry.getKey()), name);
            }
            commands.clear(); cell(node, "shapeKind").restoreDefaultValue();
            assertInstanceOf(PatchProperties.class, commands.getFirst());
            var reset = apply(widget, commands.getFirst()); assertValid(reset);
            assertEquals(Map.of(p("variant"), ELEVATED, p("color"), value("color")), reset.properties());
        }
    }

    @Test void everyDetailSwitchesFromTypedReferenceAndChoosesCompatibleKindAtomically() throws Exception {
        for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) {
            var widget = new WidgetNode(ID, DEF.typeId(), Map.of(p("variant"), ELEVATED, p("shape"), value("shape"), p("margin"), value("margin")), Map.of());
            var commands = new ArrayList<DesignerCommand>(); var node = node(widget, commands);
            cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
            var result = apply(widget, commands.getFirst()); assertValid(result);
            assertFalse(result.properties().containsKey(p("shape"))); assertEquals(value(name), result.properties().get(p(name)));
            assertEquals(value("margin"), result.properties().get(p("margin")));
            node.refreshPresentation(result, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, "shape").setValue(FlutterPropertyCellValue.explicit(value("shape")));
            assertEquals(1, commands.size()); result = apply(result, commands.getFirst()); assertValid(result);
            assertEquals(widget, result);
        }
    }

    @Test void numericDomainsRetainUncappedPointsAlignmentAndRoundingValidation() throws Exception {
        var commands = new ArrayList<DesignerCommand>(); var node = node(WidgetNodePrototypeFactory.create(DEF, ID), commands);
        for (String name : List.of("shapeRotation", "shapeSideStrokeAlign", "shapeStartAlignment", "shapeEndAlignment", "shapeTopAlignment", "shapeBottomAlignment")) {
            for (String number : List.of("-10000", "10000")) { var editor = cell(node, name).getPropertyEditor(); editor.setAsText(number); assertNotNull(editor.getValue()); }
        }
        cell(node, "shapePoints").getPropertyEditor().setAsText("1000000.5");
        for (String name : List.of("shapePoints", "elevation", "shapeSideWidth", "shapeStartSize", "shapePointRounding", "shapeValleyRounding")) {
            assertThrows(IllegalArgumentException.class, () -> cell(node, name).getPropertyEditor().setAsText("-1"));
            assertThrows(IllegalArgumentException.class, () -> cell(node, name).getPropertyEditor().setAsText("1e999"));
        }
        var widget = new WidgetNode(ID, DEF.typeId(), Map.of(p("variant"), ELEVATED, p("shapeKind"), s("star"), p("shapePointRounding"), n("0.8")), Map.of());
        var starNode = node(widget, commands); commands.clear(); cell(starNode, "shapeValleyRounding").setValue(FlutterPropertyCellValue.explicit(n("0.8")));
        var invalid = apply(widget, commands.getFirst());
        assertEquals(n("0.8"), invalid.properties().get(p("shapePointRounding"))); assertEquals(n("0.8"), invalid.properties().get(p("shapeValleyRounding")));
        assertFalse(new WidgetTreeValidator().validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid(), "The exact SDK sum is rejected rather than clamped");
    }

    public static PropertyValue value(String name) {
        if (name.equals("variant")) return s("outlined");
        if (name.equals("shapeKind")) return s("roundedRectangle");
        if (name.equals("shape")) return new PropertyValue.DartObjectReferenceValue(Optional.empty(), "ProjectShape", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
        if (name.endsWith("Color") || name.equals("color")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant"));
        if (name.equals("borderOnForeground") || name.equals("semanticContainer")) return new PropertyValue.BooleanValue(false);
        if (name.equals("clipBehavior")) return new PropertyValue.EnumValue("Clip", "antiAlias");
        if (name.equals("shapeSideStyle")) return new PropertyValue.EnumValue("BorderStyle", "solid");
        if (name.equals("margin")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        if (name.equals("shapeRadius")) { var a = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE, BigDecimal.valueOf(2)); var b = new PropertyValue.BoxDecorationValue.Radius(BigDecimal.valueOf(3), BigDecimal.valueOf(4)); return new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,b,a)); }
        if (name.equals("shapePoints")) return n("6.5");
        if (name.endsWith("Alignment") || name.equals("shapeSideStrokeAlign")) return n("-2.5");
        return n("0.25");
    }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static WidgetNode apply(WidgetNode widget, DesignerCommand command) {
        var values = new LinkedHashMap<>(widget.properties());
        var patches = command instanceof PatchProperties patch ? patch.patches() : command instanceof SetProperty set ? List.<PatchProperties.Patch>of(new PatchProperties.SetPatch(set.propertyName(), set.value())) : List.<PatchProperties.Patch>of(new PatchProperties.ResetPatch(((ResetProperty) command).propertyName()));
        for (var patch : patches) { if (patch instanceof PatchProperties.SetPatch set) values.put(set.propertyName(), set.value()); else values.remove(patch.propertyName()); }
        return new WidgetNode(widget.id(), widget.type(), values, widget.slots());
    }
    private static void assertValid(WidgetNode widget) { var validation = new WidgetTreeValidator().validate(document(widget), BuiltInWidgetCatalog.getDefault()); assertTrue(validation.valid(), validation.toString()); }
    private static DesignerDocument document(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.random(), new DartSourceDescriptor("card.dart", "CardScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); }
    private static PropertyName p(String value) { return new PropertyName(value); }
    private static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    private static PropertyValue.DoubleValue n(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
}
