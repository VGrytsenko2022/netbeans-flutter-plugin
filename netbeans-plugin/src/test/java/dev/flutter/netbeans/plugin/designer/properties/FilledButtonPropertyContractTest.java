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

public class FilledButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(FilledButtonWidgetPropertySchema.FILLED_BUTTON_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("b3900004-e530-4b9b-92fa-49e3c491f094");

    @Test void all510RowsHaveStableTypedCellsAndEveryOptionalLeafResetsIndependently() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(510, DEF.properties().size()); assertEquals(15, sets.length);
        assertEquals(490, full(false, false, false).size()); assertEquals(491, full(true, true, false).size());
        assertEquals(464, full(true, true, true).size());
        assertEquals(490, full("tonal", false, false).size()); assertEquals(491, full("tonalIcon", true, false).size());
        assertEquals(498, FilledButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(9, FilledButtonWidgetPropertySchema.statePrefixes().size());
        assertEquals(Map.of(new PropertyName("enabled"), new PropertyValue.BooleanValue(true),
                new PropertyName("variant"), new PropertyValue.StringValue("standard")), initial.properties());
        for (var field : DEF.properties()) {
            String name = field.name().value(); var props = new LinkedHashMap<>(initial.properties());
            if (name.equals("iconAlignment")) props.put(new PropertyName("variant"), new PropertyValue.StringValue("icon"));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); var editor = cell.getPropertyEditor(); var value = value(field);
            assertTrue(cell.canWrite(), name); assertEquals(!field.parameter().required(), cell.supportsDefaultValue(), name);
            editor.setValue(FlutterPropertyCellValue.explicit(value)); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue(), name);
            if (!editor.supportsCustomEditor()) { editor.setAsText(editor.getAsText()); assertEquals(FlutterPropertyCellValue.explicit(value), editor.getValue(), name); }
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(value));
            assertEquals(1, commands.size(), name); props = apply(props, commands.getFirst());
            assertEquals(value, props.get(field.name()), name);
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear(); if (!field.parameter().required()) { cell.restoreDefaultValue(); assertEquals(1, commands.size(), name); assertFalse(apply(props, commands.getFirst()).containsKey(field.name()), name); }
        }
        assertTrue(cell(node, "style").getShortDescription().contains("498"));
        assertEquals(List.of("disabled", "error", "dragged", "pressed", "selected", "scrolledUnder", "hovered", "focused", "any"), FilledButtonWidgetPropertySchema.statePriority());
    }

    @Test void wholeStyleAndAll498LocalLeavesSwitchAtomicallyWithoutRestoringDiscardedFields() throws Exception {
        var initial = withLabel(); var props = new LinkedHashMap<>(initial.properties());
        for (String name : FilledButtonWidgetPropertySchema.localStyleProperties()) props.put(new PropertyName(name), value(name));
        var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), commands);
        cell(node, "style").setValue(FlutterPropertyCellValue.explicit(reference("_style")));
        var patches = new ArrayList<PatchProperties.Patch>();
        for (String name : FilledButtonWidgetPropertySchema.localStyleProperties()) patches.add(new PatchProperties.ResetPatch(new PropertyName(name)));
        patches.add(new PatchProperties.SetPatch(new PropertyName("style"), reference("_style")));
        assertEquals(499, patches.size()); assertEquals(List.of(new PatchProperties(ID, patches)), commands);
        props = new LinkedHashMap<>(initial.properties()); props.put(new PropertyName("style"), reference("_style"));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (String name : FilledButtonWidgetPropertySchema.localStyleProperties()) {
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(1, commands.size(), name); var patch = assertInstanceOf(PatchProperties.class, commands.getFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("style"))), name);
            var edited = apply(props, patch); assertFalse(edited.containsKey(new PropertyName("style"))); assertEquals(value(name), edited.get(new PropertyName(name)));
        }
        commands.clear(); cell(node, "style").restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(ID, new PropertyName("style"))), commands);
    }

    @Test void constructorDependenciesAreAtomicAndNeverDeleteOccupiedIcon() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>();
        var props = new LinkedHashMap<>(initial.properties()); var node = node(initial, commands);
        assertTrue(DEF.property(new PropertyName("isSemanticButton")).isEmpty());
        cell(node, "iconAlignment").setValue(FlutterPropertyCellValue.explicit(value("iconAlignment")));
        assertEquals(List.of(new PatchProperties(ID, List.of(
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("icon")),
                new PatchProperties.SetPatch(new PropertyName("iconAlignment"), value("iconAlignment"))))), commands);
        props = apply(props, commands.getFirst());
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        commands.clear(); cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("standard")));
        var standard = apply(props, commands.getFirst());
        assertFalse(standard.containsKey(new PropertyName("iconAlignment")));
        assertEquals(new PropertyValue.StringValue("standard"), standard.get(new PropertyName("variant")));
        var icon = new WidgetNode(StableId.parse("b4900004-e530-4b9b-92fa-49e3c491f094"), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("icon")), Map.of());
        var slots = new LinkedHashMap<>(initial.slots()); slots.put(new SlotName("icon"), WidgetSlot.SingleSlot.of(icon));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, slots), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        commands.clear();
        var error = assertThrows(IllegalArgumentException.class, () -> cell(node, "variant")
                .setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("standard"))));
        assertTrue(error.getMessage().contains("FilledButton")); assertTrue(error.getMessage().contains("Move or clear Icon first"));
        assertTrue(error.getMessage().contains(icon.id().toString())); assertTrue(commands.isEmpty());
    }

    @Test void nullableClipAndStrictReferencesNeverWidenExistingButtonBindings() {
        for (String name : List.of("clipBehavior")) {
            var editor = cell(node(withLabel(), new ArrayList<>()), name).getPropertyEditor();
            for (String text : List.of("null", "Explicit null")) { editor.setAsText(text); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue()); }
            assertEquals("Explicit null", editor.getAsText()); editor.setAsText("<not set>"); assertEquals(FlutterPropertyCellValue.unset(), editor.getValue());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("null ?? true"));
        }
        for (String name : List.of("onPressed", "onLongPress", "onHover", "onFocusChange", "focusNode", "statesController", "style", "styleBackgroundBuilder", "styleForegroundBuilder")) {
            var editor = FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().createEditor();
            editor.setValue(FlutterPropertyCellValue.explicit(reference("_value"))); assertTrue(editor.supportsCustomEditor());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("() => rawCode()"));
        }
        var old = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.ElevatedButton")).orElseThrow();
        assertEquals(286, old.properties().size());
        var callback = FlutterTypedPropertyEditors.binding(old.properties().stream().filter(p -> p.name().value().equals("onPressed")).findFirst().orElseThrow()).orElseThrow();
        assertEquals(FlutterTypedPropertyEditors.EditorKind.CALLBACK, callback.editorKind());
        for (String type : List.of("flutter.material.ElevatedButton", "flutter.material.RefreshIndicator")) {
            var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
            var property = definition.properties().stream().filter(p -> p.name().value().equals(type.contains("Elevated") ? "autofocus" : "strokeWidth")).findFirst().orElseThrow();
            assertThrows(IllegalArgumentException.class, () -> FlutterTypedPropertyEditors.binding(property).orElseThrow().createEditor().setAsText("null"));
        }
    }

    @Test void localStyleDependenciesSeedAndResetWithoutLosingUnrelatedLeaves() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>(); var props = new LinkedHashMap<>(initial.properties());
        var node = node(initial, commands);
        for (String name : List.of("styleAlignmentX", "styleErrorTextTheme", "styleHoveredTextInherit", "stylePressedShapeRadiusTopLeft")) {
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name))); props = apply(props, commands.getFirst());
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        }
        assertEquals(new PropertyValue.StringValue("physical"), props.get(new PropertyName("styleAlignmentKind")));
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ZERO), props.get(new PropertyName("styleAlignmentY")));
        for (String prefix : List.of("style", "styleDisabled", "styleError", "styleHovered"))
            assertEquals(new PropertyValue.BooleanValue(true), props.get(new PropertyName(prefix + "TextInherit")));
        assertEquals(new PropertyValue.StringValue("roundedRectangle"), props.get(new PropertyName("stylePressedShapeKind")));
        for (String prefix : FilledButtonWidgetPropertySchema.statePrefixes()) {
            props.put(new PropertyName(prefix + "TextBackgroundColor"), new PropertyValue.ColorValue(0xFF000000L));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell(node, prefix + "TextBackground").setValue(FlutterPropertyCellValue.explicit(value(prefix + "TextBackground")));
            props = apply(props, commands.getFirst()); assertFalse(props.containsKey(new PropertyName(prefix + "TextBackgroundColor")));
        }
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        commands.clear(); cell(node, "styleErrorTextInherit").restoreDefaultValue(); props = apply(props, commands.getFirst());
        assertTrue(props.keySet().stream().noneMatch(key -> key.value().endsWith("TextInherit") || key.value().endsWith("TextTheme")));
        assertTrue(props.containsKey(new PropertyName("stylePressedShapeKind")));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        commands.clear(); cell(node, "styleAlignmentX").restoreDefaultValue(); props = apply(props, commands.getFirst());
        assertTrue(props.keySet().stream().noneMatch(key -> key.value().startsWith("styleAlignment")));
    }

    public static LinkedHashMap<PropertyName, PropertyValue> apply(Map<PropertyName, PropertyValue> before, DesignerCommand command) {
        var result = new LinkedHashMap<>(before);
        if (command instanceof SetProperty set) result.put(set.propertyName(), set.value());
        else if (command instanceof ResetProperty reset) result.remove(reset.propertyName());
        else if (command instanceof PatchProperties patch) for (var entry : patch.patches()) {
            if (entry instanceof PatchProperties.SetPatch set) result.put(set.propertyName(), set.value());
            else result.remove(((PatchProperties.ResetPatch) entry).propertyName());
        }
        else throw new IllegalArgumentException(command.toString());
        return result;
    }

    @Test void disabledShapeSeedingDoesNotInheritTheEnabledBaseKind() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>();
        for (String name : List.of("styleDisabledShapeRadiusTopLeft", "styleDisabledShapeCircleEccentricity")) {
            var props = new LinkedHashMap<>(initial.properties()); props.put(new PropertyName("styleShapeKind"), new PropertyValue.StringValue("circle"));
            var node = node(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), commands); commands.clear();
            cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name))); var edited = apply(props, commands.getFirst());
            assertEquals(new PropertyValue.StringValue(name.endsWith("Eccentricity") ? "circle" : "roundedRectangle"), edited.get(new PropertyName("styleDisabledShapeKind")));
            assertEquals(props.get(new PropertyName("styleShapeKind")), edited.get(new PropertyName("styleShapeKind")));
        }
    }

    @Test void everyDenseLifecycleAlternateRemainsAValidModelAfterItsAtomicDependencies() throws Exception {
        var label = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Label")), Map.of());
        Map<SlotName, WidgetSlot> slots = Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(label));
        var region = new ManagedRegion("0".repeat(64));
        var source = new DartSourceDescriptor("button.dart", "ButtonScreen", WidgetClassKind.STATELESS,
                Optional.empty(), new ManagedRegions(region, region));
        var validator = new dev.flutter.netbeans.designer.validation.WidgetTreeValidator();
        var visited = new HashSet<String>(); var commands = new ArrayList<DesignerCommand>();
        for (int family = 0; family < 3; family++) {
            var target = full(family > 0, family > 0, family == 2);
            target.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(true));
            var props = new LinkedHashMap<>(target);
            var node = node(new WidgetNode(ID, DEF.typeId(), props, slots), commands);
            for (var entry : target.entrySet()) {
                String name = entry.getKey().value();
                if (!visited.add(name)) continue;
                for (var edit : List.of(alternate(name, entry.getValue()), entry.getValue())) {
                    node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, slots), DEF,
                            commands::add, null, null, FlutterImageAssetChoices.empty());
                    commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(edit));
                    assertEquals(1, commands.size(), name); props = apply(props, commands.getFirst());
                    var result = validator.validate(new DesignerDocument(StableId.random(), source,
                            new WidgetNode(ID, DEF.typeId(), props, slots)), BuiltInWidgetCatalog.getDefault());
                    assertTrue(result.valid(), () -> name + " " + edit + ": " + result.errors());
                }
            }
        }
        assertEquals(509, visited.size());
    }

    @Test void all510SparseLiveFixturesHaveOnlyRequiredDependenciesAndRemainValid() throws Exception {
        var initial = withLabel();
        var label = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Label")), Map.of());
        Map<SlotName, WidgetSlot> slots = Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(label));
        var region = new ManagedRegion("0".repeat(64));
        var source = new DartSourceDescriptor("button.dart", "ButtonScreen", WidgetClassKind.STATELESS,
                Optional.empty(), new ManagedRegions(region, region));
        var validator = new dev.flutter.netbeans.designer.validation.WidgetTreeValidator();
        var commands = new ArrayList<DesignerCommand>(); var node = node(initial, commands);
        for (var field : DEF.properties()) {
            String name = field.name().value(); var props = new LinkedHashMap<>(initial.properties());
            props.putAll(sparsePrerequisites(name));
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, slots), DEF,
                    commands::add, null, null, FlutterImageAssetChoices.empty());
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(field)));
            assertEquals(1, commands.size(), name); props = apply(props, commands.getFirst());
            var result = validator.validate(new DesignerDocument(StableId.random(), source,
                    new WidgetNode(ID, DEF.typeId(), props, slots)), BuiltInWidgetCatalog.getDefault());
            assertTrue(result.valid(), () -> name + ": " + result.errors());
            assertTrue(props.size() <= 6, name);
        }
    }

    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return name.endsWith("TextPackage")
                ? Map.of(new PropertyName(name.substring(0, name.length() - "Package".length()) + "FontFamily"),
                        new PropertyValue.StringValue("Roboto"))
                : Map.of();
    }

    public static PropertyValue alternate(String name, PropertyValue original) {
        return TextButtonPropertyContractTest.alternate(name, original);
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean icon, boolean paints, boolean circles) {
        return full(icon ? "icon" : "standard", paints, circles);
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant, boolean paints, boolean circles) {
        boolean icon = List.of("icon", "tonalIcon").contains(variant);
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (name.equals("style") || (!icon && name.equals("iconAlignment"))) continue;
            if (paints ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (circles ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            var value = value(field);
            if (name.equals("variant")) value = new PropertyValue.StringValue(variant);
            if (circles && name.endsWith("ShapeKind")) value = new PropertyValue.StringValue("circle");
            result.put(field.name(), value);
        }
        return result;
    }


    @Test void allFourConstructorsRetainChildAndIconButEmptyLabelsCannotEnterIconMode() throws Exception {
        var initial = withLabel(); var commands = new ArrayList<DesignerCommand>();
        var icon = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Icon")), Map.of());
        for (String branch : List.of("icon", "tonalIcon")) {
            var props = full(branch, true, false);
            var slots = new LinkedHashMap<>(initial.slots()); slots.put(new SlotName("icon"), WidgetSlot.SingleSlot.of(icon));
            var widget = new WidgetNode(ID, DEF.typeId(), props, slots); var node = node(widget, commands);
            commands.clear(); String other = branch.equals("icon") ? "tonalIcon" : "icon";
            cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(other)));
            var edited = apply(props, commands.getFirst());
            assertEquals(props.get(new PropertyName("iconAlignment")), edited.get(new PropertyName("iconAlignment")));
            assertEquals(new PropertyValue.StringValue(other), edited.get(new PropertyName("variant")));
            for (String nonIcon : List.of("standard", "tonal")) {
                commands.clear();
                var error = assertThrows(IllegalArgumentException.class, () -> cell(node, "variant")
                        .setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(nonIcon))));
                assertTrue(error.getMessage().contains("Move or clear Icon first")); assertTrue(commands.isEmpty());
            }
            assertSame(icon, ((WidgetSlot.SingleSlot) widget.slots().get(new SlotName("icon"))).child().orElseThrow());
        }
        for (String branch : List.of("standard", "tonal")) {
            var props = new LinkedHashMap<>(initial.properties()); props.put(new PropertyName("variant"), new PropertyValue.StringValue(branch));
            var empty = new WidgetNode(ID, DEF.typeId(), props, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
            var node = node(empty, commands);
            for (String target : List.of("icon", "tonalIcon")) {
                commands.clear();
                var error = assertThrows(IllegalArgumentException.class, () -> cell(node, "variant")
                        .setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue(target))));
                assertTrue(error.getMessage().contains("Child is empty")); assertTrue(commands.isEmpty());
            }
            commands.clear(); assertThrows(IllegalArgumentException.class, () -> cell(node, "iconAlignment")
                    .setValue(FlutterPropertyCellValue.explicit(value("iconAlignment")))); assertTrue(commands.isEmpty());
            node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF,
                    commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, "iconAlignment").setValue(FlutterPropertyCellValue.explicit(value("iconAlignment")));
            assertEquals(new PropertyValue.StringValue(branch.equals("tonal") ? "tonalIcon" : "icon"),
                    apply(props, commands.getFirst()).get(new PropertyName("variant")));
        }
    }

    private static WidgetNode withLabel() {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
        var label = new WidgetNode(StableId.parse("b5900004-e530-4b9b-92fa-49e3c491f094"), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Label")), Map.of());
        var slots = new LinkedHashMap<>(initial.slots()); slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(label));
        return new WidgetNode(ID, DEF.typeId(), initial.properties(), slots);
    }

    public static PropertyDefinition field(String name) { return DEF.properties().stream().filter(p -> p.name().value().equals(name)).findFirst().orElseThrow(); }
    public static PropertyValue value(String name) { return value(field(name)); }
    public static PropertyValue value(PropertyDefinition field) { return TextButtonPropertyContractTest.value(field); }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
}
