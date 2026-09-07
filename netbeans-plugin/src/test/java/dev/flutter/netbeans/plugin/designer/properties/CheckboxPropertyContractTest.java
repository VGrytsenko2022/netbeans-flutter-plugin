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

public class CheckboxPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(CheckboxWidgetPropertySchema.CHECKBOX_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("dabd60b1-5e9c-4d7a-b0bb-15b8a3c91232");

    @Test void all106CellsStayStableAndEverySparseEditResetHasAValidTypedCommand() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(106, DEF.properties().size()); assertEquals(9, sets.length); assertTrue(DEF.slots().isEmpty());
        assertEquals(22, Arrays.stream(sets).filter(set -> set.getName().equals("checkboxShape")).findFirst().orElseThrow().getProperties().length);
        for (var field : DEF.properties()) {
            var cell = cell(node, field.name().value()); var editor = cell.getPropertyEditor(); var value = value(field.name().value());
            assertTrue(cell.canWrite()); assertEquals(!field.parameter().required(), cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size(), field.name().value());
            var edited = apply(initial, commands.getFirst()); assertValid(edited); assertEquals(value, edited.properties().get(field.name()));
            assertTrue(edited.properties().size() <= 6, field.name().value());
            node.refreshPresentation(edited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, field.name().value())); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear();
            if (!field.parameter().required()) { cell.restoreDefaultValue(); assertEquals(1, commands.size()); var reset = apply(edited, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name())); }
            else { assertThrows(IllegalArgumentException.class, () -> cell.setValue(FlutterPropertyCellValue.unset())); cell.restoreDefaultValue(); assertTrue(commands.isEmpty()); }
            node.refreshPresentation(initial, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        }
        assertEquals(List.of("standard", "adaptive"), List.of(cell(node, "variant").getPropertyEditor().getTags()));
    }

    @Test void requiredMixedValueAndTristateChangesAreSingleAtomicCommands() throws Exception {
        for (boolean reset : List.of(false, true)) {
            var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands);
            cell(node, "value").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
            assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst());
            var mixed = apply(initial, commands.getFirst()); assertValid(mixed); assertEquals(new PropertyValue.BooleanValue(true), mixed.properties().get(p("tristate")));
            node.refreshPresentation(mixed, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            if (reset) cell(node, "tristate").restoreDefaultValue(); else cell(node, "tristate").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
            assertEquals(1, commands.size()); var result = apply(mixed, commands.getFirst()); assertValid(result); assertEquals(new PropertyValue.BooleanValue(false), result.properties().get(p("value")));
            assertEquals(reset ? null : new PropertyValue.BooleanValue(false), result.properties().get(p("tristate")));
        }
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, FlutterTypedPropertyEditors.binding(field("value")).orElseThrow().editorKind());
        assertFalse(FlutterTypedPropertyEditors.binding(field("value")).orElseThrow().optional());
    }

    @Test void bothConstructorsAndAllTenShapeFamiliesPreserveUnrelatedStateAndReferences() throws Exception {
        for (String variant : CheckboxWidgetPropertySchema.variants()) for (String shape : CheckboxWidgetPropertySchema.shapeKinds()) for (boolean stateful : List.of(false, true)) {
            var before = new WidgetNode(ID, DEF.typeId(), full(variant, shape, stateful), Map.of()); assertValid(before);
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(variant.equals("standard") ? "adaptive" : "standard")));
            assertEquals(1, commands.size()); assertInstanceOf(SetProperty.class, commands.getFirst()); var after = apply(before, commands.getFirst()); assertValid(after);
            var expected = new LinkedHashMap<>(before.properties()); expected.put(p("variant"), after.properties().get(p("variant"))); assertEquals(expected, after.properties());
            for (String target : CheckboxWidgetPropertySchema.shapeKinds()) {
                commands.clear(); cell(node, "shapeKind").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(target)));
                if (target.equals(shape)) { assertTrue(commands.isEmpty()); continue; }
                assertEquals(1, commands.size()); var switched = apply(before, commands.getFirst()); assertValid(switched);
                for (var entry : before.properties().entrySet()) if (!entry.getKey().value().startsWith("shape")) assertEquals(entry.getValue(), switched.properties().get(entry.getKey()));
            }
        }
        assertEquals("OutlinedBorder", field("shape").constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance).map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType());
    }

    @Test void wholeColorReferencesAndEveryNullableStateLeafSwitchOnlyTheirOwnFamily() throws Exception {
        for (String family : List.of("fillColor", "overlayColor")) {
            var before = new WidgetNode(ID, DEF.typeId(), full("adaptive", "roundedRectangle", true), Map.of());
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(reference("_" + family))); assertEquals(1, commands.size());
            var whole = apply(before, commands.getFirst()); assertValid(whole);
            for (String local : CheckboxWidgetPropertySchema.colorStateProperties(family)) assertFalse(whole.properties().containsKey(p(local)));
            for (var entry : before.properties().entrySet()) if (!CheckboxWidgetPropertySchema.colorStateProperties(family).contains(entry.getKey().value())) assertEquals(entry.getValue(), whole.properties().get(entry.getKey()));
            for (String local : CheckboxWidgetPropertySchema.colorStateProperties(family)) for (PropertyValue value : List.of(value(local), new PropertyValue.ColorValue(0x10203040L), new PropertyValue.NullValue())) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(1, commands.size()); var edited = apply(whole, commands.getFirst()); assertValid(edited);
                assertFalse(edited.properties().containsKey(p(family))); assertEquals(value, edited.properties().get(p(local)));
                assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_THEME_COLOR, FlutterTypedPropertyEditors.binding(field(local)).orElseThrow().editorKind());
            }
        }
    }

    @Test void statefulSideModeDetailAndPlainResetsPreserveExactBaseAndOtherStates() throws Exception {
        for (String state : CheckboxWidgetPropertySchema.sideStates()) {
            var before = new WidgetNode(ID, DEF.typeId(), full("standard", "circle", true), Map.of());
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands); String mode = "side" + state + "Mode";
            cell(node, mode).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("inherit"))); assertEquals(1, commands.size());
            var inherited = apply(before, commands.getFirst()); assertValid(inherited);
            for (String detail : CheckboxWidgetPropertySchema.sideBucketProperties(state).subList(1, 5)) assertFalse(inherited.properties().containsKey(p(detail)));
            for (String detail : CheckboxWidgetPropertySchema.sideBucketProperties(state).subList(1, 5)) {
                node.refreshPresentation(inherited, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear(); cell(node, detail).setValue(FlutterPropertyCellValue.explicit(value(detail)));
                var border = apply(inherited, commands.getFirst()); assertValid(border); assertEquals(new PropertyValue.StringValue("border"), border.properties().get(p(mode)));
                for (String base : CheckboxWidgetPropertySchema.sideBaseProperties()) assertEquals(before.properties().get(p(base)), border.properties().get(p(base)));
            }
        }
        for (boolean reset : List.of(false, true)) {
            var before = new WidgetNode(ID, DEF.typeId(), full("adaptive", "roundedRectangle", true), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            if (reset) cell(node, "sideStateful").restoreDefaultValue(); else cell(node, "sideStateful").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
            var plain = apply(before, commands.getFirst()); assertValid(plain); for (String field : CheckboxWidgetPropertySchema.sideStateProperties()) assertFalse(plain.properties().containsKey(p(field)));
            for (String base : CheckboxWidgetPropertySchema.sideBaseProperties()) assertEquals(before.properties().get(p(base)), plain.properties().get(p(base)));
        }
    }

    @Test void wholeSideAndShapeReferencesAreExclusiveWithEveryLocalLeaf() throws Exception {
        for (String family : List.of("side", "shape")) {
            var before = new WidgetNode(ID, DEF.typeId(), full("standard", "roundedRectangle", true), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(reference("_" + family))); var whole = apply(before, commands.getFirst()); assertValid(whole);
            var leaves = family.equals("side") ? CheckboxWidgetPropertySchema.sideLocalProperties() : CheckboxWidgetPropertySchema.builtInShapePropertyNames();
            for (String field : leaves) assertFalse(whole.properties().containsKey(p(field)));
            for (String field : leaves) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear(); cell(node, field).setValue(FlutterPropertyCellValue.explicit(value(field)));
                var edited = apply(whole, commands.getFirst()); assertValid(edited); assertFalse(edited.properties().containsKey(p(family))); assertEquals(value(field), edited.properties().get(p(field)));
            }
        }
    }

    @Test void invalidValuesFailWithoutCommandsAndSharedEditorsRemainNarrow() throws Exception {
        var commands = new ArrayList<DesignerCommand>(); var node = node(WidgetNodePrototypeFactory.create(DEF, ID), commands);
        for (String field : List.of("value", "enabled", "variant")) assertThrows(IllegalArgumentException.class, () -> cell(node, field).setValue(FlutterPropertyCellValue.unset()));
        for (String field : List.of("onChanged", "focusNode", "shape", "side", "fillColor", "overlayColor")) assertThrows(IllegalArgumentException.class, () -> cell(node, field).getPropertyEditor().setAsText("arbitrary()"));
        for (String field : List.of("visualDensityHorizontal", "visualDensityVertical")) assertThrows(IllegalArgumentException.class, () -> cell(node, field).getPropertyEditor().setAsText("5"));
        assertThrows(IllegalArgumentException.class, () -> cell(node, "sideWidth").getPropertyEditor().setAsText("-1"));
        assertThrows(IllegalArgumentException.class, () -> cell(node, "activeColor").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        assertTrue(commands.isEmpty());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.THEME_COLOR, FlutterTypedPropertyEditors.binding(field("activeColor")).orElseThrow().editorKind());
        var progress = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.LinearProgressIndicator")).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.COLOR_ANIMATION, FlutterTypedPropertyEditors.binding(progress.property(p("valueColor")).orElseThrow()).orElseThrow().editorKind());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, String shapeKind, boolean stateful) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (List.of("shape", "fillColor", "overlayColor", "side").contains(name)) continue;
            if (CheckboxWidgetPropertySchema.builtInShapePropertyNames().contains(name) && !CheckboxWidgetPropertySchema.shapePropertyAppliesToKind(name, shapeKind)) continue;
            if (!stateful && CheckboxWidgetPropertySchema.sideStateProperties().contains(name)) continue;
            values.put(field.name(), name.equals("variant") ? new PropertyValue.StringValue(variant) : name.equals("shapeKind") ? new PropertyValue.StringValue(shapeKind)
                    : name.equals("enabled") ? new PropertyValue.BooleanValue(true) : name.equals("sideStateful") ? new PropertyValue.BooleanValue(stateful) : value(name));
        }
        return values;
    }

    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) { return Map.of(); }
    public static PropertyDefinition field(String name) { return DEF.property(p(name)).orElseThrow(); }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        if (name.equals("variant")) return new PropertyValue.StringValue("adaptive");
        if (name.equals("enabled")) return new PropertyValue.BooleanValue(false);
        if (List.of("value", "tristate", "autofocus", "isError", "sideStateful").contains(name)) return new PropertyValue.BooleanValue(true);
        if (List.of("onChanged", "focusNode", "mouseCursor", "shape", "side", "fillColor", "overlayColor").contains(name)) return reference("_" + name);
        if (name.startsWith("shape")) return CardPropertyContractTest.value(name);
        if (name.equals("semanticLabel")) return new PropertyValue.StringValue("Accept terms");
        if (name.endsWith("Mode")) return new PropertyValue.StringValue("border");
        if (name.endsWith("Style")) return new PropertyValue.EnumValue("BorderStyle", "solid");
        if (name.endsWith("Color") || name.startsWith("fillColor") || name.startsWith("overlayColor")) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        if (name.equals("materialTapTargetSize")) return new PropertyValue.EnumValue("MaterialTapTargetSize", "shrinkWrap");
        return new PropertyValue.DoubleValue(new BigDecimal(name.endsWith("StrokeAlign") ? "-2.5" : "2.5"));
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("checkbox.dart", "CheckboxScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
