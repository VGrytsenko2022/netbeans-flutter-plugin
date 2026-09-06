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

public class TextButtonPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(TextButtonWidgetPropertySchema.TEXT_BUTTON_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("a3900004-e530-4b9b-92fa-49e3c491f093");

    @Test void all511RowsHaveStableTypedCellsAndEveryOptionalLeafResetsIndependently() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(initial, commands); var sets = node.getPropertySets();
        assertEquals(511, DEF.properties().size()); assertEquals(15, sets.length);
        assertEquals(498, TextButtonWidgetPropertySchema.localStyleProperties().size());
        assertEquals(9, TextButtonWidgetPropertySchema.statePrefixes().size());
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
        assertEquals(List.of("disabled", "error", "dragged", "pressed", "selected", "scrolledUnder", "hovered", "focused", "any"), TextButtonWidgetPropertySchema.statePriority());
    }

    @Test void wholeStyleAndAll498LocalLeavesSwitchAtomicallyWithoutRestoringDiscardedFields() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var props = new LinkedHashMap<>(initial.properties());
        for (String name : TextButtonWidgetPropertySchema.localStyleProperties()) props.put(new PropertyName(name), value(name));
        var commands = new ArrayList<DesignerCommand>(); var node = node(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), commands);
        cell(node, "style").setValue(FlutterPropertyCellValue.explicit(reference("_style")));
        var patches = new ArrayList<PatchProperties.Patch>();
        for (String name : TextButtonWidgetPropertySchema.localStyleProperties()) patches.add(new PatchProperties.ResetPatch(new PropertyName(name)));
        patches.add(new PatchProperties.SetPatch(new PropertyName("style"), reference("_style")));
        assertEquals(499, patches.size()); assertEquals(List.of(new PatchProperties(ID, patches)), commands);
        props = new LinkedHashMap<>(initial.properties()); props.put(new PropertyName("style"), reference("_style"));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (String name : TextButtonWidgetPropertySchema.localStyleProperties()) {
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(value(name)));
            assertEquals(1, commands.size(), name); var patch = assertInstanceOf(PatchProperties.class, commands.getFirst());
            assertTrue(patch.patches().contains(new PatchProperties.ResetPatch(new PropertyName("style"))), name);
            var edited = apply(props, patch); assertFalse(edited.containsKey(new PropertyName("style"))); assertEquals(value(name), edited.get(new PropertyName(name)));
        }
        commands.clear(); cell(node, "style").restoreDefaultValue();
        assertEquals(List.of(new ResetProperty(ID, new PropertyName("style"))), commands);
    }

    @Test void constructorDependenciesAreAtomicAndNeverDeleteOccupiedIcon() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var props = new LinkedHashMap<>(initial.properties()); props.put(new PropertyName("isSemanticButton"), new PropertyValue.NullValue());
        var node = node(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), commands);
        cell(node, "iconAlignment").setValue(FlutterPropertyCellValue.explicit(value("iconAlignment")));
        assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.ResetPatch(new PropertyName("isSemanticButton")),
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("icon")),
                new PatchProperties.SetPatch(new PropertyName("iconAlignment"), value("iconAlignment"))))), commands);
        props.remove(new PropertyName("isSemanticButton")); props.put(new PropertyName("variant"), new PropertyValue.StringValue("icon")); props.put(new PropertyName("iconAlignment"), value("iconAlignment"));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, initial.slots()), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        commands.clear(); cell(node, "isSemanticButton").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()));
        assertEquals(List.of(new PatchProperties(ID, List.of(new PatchProperties.ResetPatch(new PropertyName("iconAlignment")),
                new PatchProperties.SetPatch(new PropertyName("variant"), new PropertyValue.StringValue("standard")),
                new PatchProperties.SetPatch(new PropertyName("isSemanticButton"), new PropertyValue.NullValue())))), commands);
        var icon = new WidgetNode(StableId.parse("a4900004-e530-4b9b-92fa-49e3c491f093"), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("icon")), Map.of());
        var slots = new LinkedHashMap<>(initial.slots()); slots.put(new SlotName("icon"), WidgetSlot.SingleSlot.of(icon));
        node.refreshPresentation(new WidgetNode(ID, DEF.typeId(), props, slots), DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        for (String name : List.of("variant", "isSemanticButton")) {
            commands.clear();
            var error = assertThrows(IllegalArgumentException.class, () -> cell(node, name).setValue(FlutterPropertyCellValue.explicit(
                    name.equals("variant") ? new PropertyValue.StringValue("standard") : new PropertyValue.NullValue())));
            assertTrue(error.getMessage().contains("Move or clear Icon first")); assertTrue(error.getMessage().contains(icon.id().toString())); assertTrue(commands.isEmpty());
        }
    }

    @Test void nullableBooleanEnumAndStrictReferencesNeverWidenExistingButtonBindings() {
        for (String name : List.of("isSemanticButton", "clipBehavior")) {
            var editor = cell(node(WidgetNodePrototypeFactory.create(DEF, ID), new ArrayList<>()), name).getPropertyEditor();
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
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>(); var props = new LinkedHashMap<>(initial.properties());
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
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
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
        var initial = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
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
        assertEquals(510, visited.size());
    }

    @Test void all511SparseLiveFixturesHaveOnlyRequiredDependenciesAndRemainValid() throws Exception {
        var initial = WidgetNodePrototypeFactory.create(DEF, ID);
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
        return switch (original) {
            case PropertyValue.BooleanValue value -> new PropertyValue.BooleanValue(!value.value());
            case PropertyValue.IntegerValue ignored -> new PropertyValue.IntegerValue(name.endsWith("MaximumWidth") || name.endsWith("MaximumHeight") ? java.math.BigInteger.TWO : java.math.BigInteger.ZERO);
            case PropertyValue.DoubleValue ignored -> new PropertyValue.DoubleValue(name.endsWith("MaximumWidth") || name.endsWith("MaximumHeight") ? BigDecimal.TWO : BigDecimal.ZERO);
            case PropertyValue.ColorValue ignored -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.error"));
            case PropertyValue.ThemeTokenValue value -> new PropertyValue.ThemeTokenValue(new ThemeToken(((PropertyValueConstraint.ThemeTokenValues) field(name).constraints().stream()
                    .filter(PropertyValueConstraint.ThemeTokenValues.class::isInstance).findFirst().orElseThrow()).wireIds().stream().filter(id -> !new ThemeToken(id).equals(value.token())).findFirst().orElseThrow()));
            case PropertyValue.NullValue ignored -> name.equals("isSemanticButton") ? new PropertyValue.BooleanValue(true) : new PropertyValue.EnumValue("Clip", "hardEdge");
            case PropertyValue.EnumValue value -> new PropertyValue.EnumValue(value.type(), ((PropertyValueConstraint.EnumValues) field(name).constraints().stream()
                    .filter(PropertyValueConstraint.EnumValues.class::isInstance).findFirst().orElseThrow()).values().stream().filter(item -> !item.equals(value.value())).findFirst().orElseThrow());
            case PropertyValue.StringValue value -> new PropertyValue.StringValue(name.equals("variant") ? value.value().equals("standard") ? "icon" : "standard"
                    : name.endsWith("ShapeKind") ? "roundedSuperellipse" : name.endsWith("MouseCursor") ? "basic"
                    : name.equals("styleAlignmentKind") ? "physical" : name.equals("styleSplashFactory") ? "inkSplash"
                    : name.endsWith("LocaleLanguageCode") ? "uk" : name.endsWith("LocaleScriptCode") ? "Cyrl" : name.endsWith("LocaleCountryCode") ? "UA"
                    : name.endsWith("Package") ? "other_fonts" : name.endsWith("FontFamilyFallback") ? "Noto Serif\nNoto Sans" : "Noto Serif");
            case PropertyValue.DartObjectReferenceValue ignored -> reference("_edited_" + name);
            case PropertyValue.EdgeInsetsDirectionalValue ignored -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TWO);
            case PropertyValue.PaintValue ignored -> PropertyValue.PaintValue.defaults(new ColorSource.Literal(0xffabcdefL));
            case PropertyValue.ShadowListValue ignored -> new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(
                    StableId.random(), new ColorSource.Literal(0xff123456L), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)));
            case PropertyValue.FontFeatureListValue ignored -> new PropertyValue.FontFeatureListValue(List.of(new PropertyValue.FontFeatureListValue.FontFeature(StableId.random(), "kern", 1)));
            case PropertyValue.FontVariationListValue ignored -> new PropertyValue.FontVariationListValue(List.of(new PropertyValue.FontVariationListValue.FontVariation(StableId.random(), "wght", BigDecimal.valueOf(500))));
            default -> throw new IllegalArgumentException(name + ": " + original);
        };
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean icon, boolean paints, boolean circles) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (name.equals("style") || (icon ? name.equals("isSemanticButton") : name.equals("iconAlignment"))) continue;
            if (paints ? name.endsWith("TextBackgroundColor") : name.endsWith("TextBackground")) continue;
            if (circles ? name.contains("ShapeRadius") : name.endsWith("ShapeCircleEccentricity")) continue;
            var value = value(field);
            if (name.equals("variant")) value = new PropertyValue.StringValue(icon ? "icon" : "standard");
            if (circles && name.endsWith("ShapeKind")) value = new PropertyValue.StringValue("circle");
            result.put(field.name(), value);
        }
        return result;
    }

    public static PropertyDefinition field(String name) { return DEF.properties().stream().filter(p -> p.name().value().equals(name)).findFirst().orElseThrow(); }
    public static PropertyValue value(String name) { return value(field(name)); }
    public static PropertyValue value(PropertyDefinition field) {
        String name = field.name().value();
        if (name.equals("variant")) return new PropertyValue.StringValue("icon");
        if (name.equals("enabled")) return new PropertyValue.BooleanValue(false);
        if (List.of("isSemanticButton", "clipBehavior").contains(name)) return new PropertyValue.NullValue();
        if (field.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) return reference("_" + name);
        var kind = field.constraints().getFirst().kind();
        return switch (kind) {
            case BOOLEAN -> new PropertyValue.BooleanValue(true);
            case INTEGER -> new PropertyValue.IntegerValue(java.math.BigInteger.ONE);
            case DOUBLE -> new PropertyValue.DoubleValue(BigDecimal.ONE);
            case COLOR -> new PropertyValue.ColorValue(0x80445566L);
            case THEME_TOKEN -> new PropertyValue.ThemeTokenValue(new ThemeToken(((PropertyValueConstraint.ThemeTokenValues)field.constraints().getFirst()).wireIds().getFirst()));
            case ENUM -> { var constraint = (PropertyValueConstraint.EnumValues) field.constraints().getFirst(); yield new PropertyValue.EnumValue(constraint.dartType().name(), constraint.values().getFirst()); }
            case EDGE_INSETS -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
            case PAINT -> PropertyValue.PaintValue.defaults(new ColorSource.Literal(0x80123456L));
            case SHADOW_LIST -> new PropertyValue.ShadowListValue(List.of());
            case FONT_FEATURE_LIST -> new PropertyValue.FontFeatureListValue(List.of());
            case FONT_VARIATION_LIST -> new PropertyValue.FontVariationListValue(List.of());
            case STRING -> new PropertyValue.StringValue(name.endsWith("ShapeKind") ? "roundedRectangle" : name.endsWith("MouseCursor") ? "click"
                    : name.equals("styleAlignmentKind") ? "directional" : name.equals("styleSplashFactory") ? "noSplash"
                    : name.endsWith("LocaleLanguageCode") ? "en" : name.endsWith("LocaleScriptCode") ? "Latn" : name.endsWith("LocaleCountryCode") ? "US"
                    : name.endsWith("FontFamilyFallback") ? "Roboto\nNoto Sans" : name.endsWith("Package") ? "demo_fonts" : "Roboto");
            default -> throw new IllegalArgumentException(name + ": " + kind);
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
}
