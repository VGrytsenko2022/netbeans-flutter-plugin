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

public class ListTilePropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(ListTileWidgetPropertySchema.LIST_TILE_TYPE).orElseThrow();

    @Test void all176OptionalFieldsKeepStableCellsEditorsAndIndependentResets() throws Exception {
        var prototype = prototype(); var commands = new ArrayList<DesignerCommand>(); var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(176, DEF.properties().size()); assertEquals(4, DEF.slots().size()); assertTrue(prototype.properties().isEmpty());
        var seen = new HashSet<String>();
        for (var field : DEF.properties()) {
            String name = field.name().value(); var properties = new LinkedHashMap<>(sparsePrerequisites(name));
            var before = new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots());
            node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            var cell = cell(node, name); var editor = cell.getPropertyEditor(); var explicit = value(name);
            assertTrue(seen.add(name)); assertTrue(cell.canWrite()); assertTrue(cell.supportsDefaultValue());
            editor.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(FlutterPropertyCellValue.explicit(explicit), editor.getValue());
            commands.clear(); cell.setValue(FlutterPropertyCellValue.explicit(explicit)); assertEquals(1, commands.size(), name);
            var after = apply(before, commands.getFirst()); assertValid(after); assertEquals(explicit, after.properties().get(field.name()));
            node.refreshPresentation(after, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            assertSame(cell, cell(node, name)); assertEquals(editor.getClass(), cell.getPropertyEditor().getClass()); assertEquals(List.of(sets), List.of(node.getPropertySets()));
            commands.clear(); cell.restoreDefaultValue(); assertEquals(1, commands.size(), name);
            var reset = apply(after, commands.getFirst()); assertValid(reset); assertFalse(reset.properties().containsKey(field.name()));
        }
        assertEquals(176, seen.size());
    }

    @Test void allDenseTextPaintAndInsetsFamiliesAreValidAndWholeValuesSwitchOnlyTheirProjection() throws Exception {
        for (boolean physical : List.of(false, true)) for (boolean paint : List.of(false, true)) {
            var prototype = prototype(); var before = new WidgetNode(prototype.id(), DEF.typeId(), full(physical, paint), prototype.slots()); assertValid(before);
            for (String family : List.of("titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle", "visualDensity", "iconColor", "textColor", "mouseCursor")) {
                var locals = locals(family); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
                cell(node, family).setValue(FlutterPropertyCellValue.explicit(value(family))); assertEquals(1, commands.size());
                var whole = apply(before, commands.getFirst()); assertValid(whole); assertEquals(before.slots(), whole.slots());
                locals.forEach(local -> assertFalse(whole.properties().containsKey(p(local)), local));
                before.properties().forEach((name, value) -> { if (!locals.contains(name.value())) assertEquals(value, whole.properties().get(name)); });
                String local = locals.getFirst(); node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).setValue(FlutterPropertyCellValue.explicit(value(local))); assertEquals(1, commands.size());
                var restored = apply(whole, commands.getFirst()); assertValid(restored); assertFalse(restored.properties().containsKey(p(family)));
            }
        }
    }

    @Test void stateMapsRequireExplicitDefaultAndNeverCopyWholeFallbacks() throws Exception {
        for (String family : List.of("iconColor", "textColor", "mouseCursor")) {
            var prototype = prototype(); String local = family + "Pressed", fallback = family + "Default";
            var before = new WidgetNode(prototype.id(), DEF.typeId(), Map.of(p(family), value(family)), prototype.slots());
            var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            var missing = assertThrows(IllegalArgumentException.class, () -> cell(node, local).setValue(FlutterPropertyCellValue.explicit(value(local))));
            assertTrue(missing.getMessage().contains(fallback)); assertTrue(commands.isEmpty());
            cell(node, fallback).setValue(FlutterPropertyCellValue.explicit(value(fallback)));
            var withDefault = apply(before, commands.getFirst()); assertValid(withDefault); assertFalse(withDefault.properties().containsKey(p(family)));
            node.refreshPresentation(withDefault, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, local).setValue(FlutterPropertyCellValue.explicit(value(local))); var map = apply(withDefault, commands.getFirst()); assertValid(map);
            node.refreshPresentation(map, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            var reset = assertThrows(IllegalArgumentException.class, () -> cell(node, fallback).restoreDefaultValue());
            assertTrue(reset.getMessage().contains("remaining local state")); assertTrue(commands.isEmpty());
            cell(node, family).setValue(FlutterPropertyCellValue.explicit(value(family))); assertEquals(1, commands.size()); assertValid(apply(map, commands.getFirst()));
        }
    }

    @Test void threeLineRequiresSubtitleButFalseNullAndOmissionDoNotFabricateChildren() throws Exception {
        var empty = WidgetNodePrototypeFactory.create(DEF, StableId.random()); var commands = new ArrayList<DesignerCommand>(); var node = node(empty, commands);
        var error = assertThrows(IllegalArgumentException.class, () -> cell(node, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true))));
        assertTrue(error.getMessage().contains("Subtitle")); assertTrue(commands.isEmpty());
        for (PropertyValue value : List.of(new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
            cell(node, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(value)); var after = apply(empty, commands.removeFirst()); assertValid(after); assertEquals(empty.slots(), after.slots());
        }
        var full = new WidgetNode(empty.id(), DEF.typeId(), Map.of(), prototype().slots()); node.refreshPresentation(full, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
        cell(node, "isThreeLine").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(true))); assertValid(apply(full, commands.getFirst()));
    }

    @Test void nullableGeometryPreservesAllClosedConstantsWithoutWideningOtherNumbers() {
        var prototype = prototype(); var node = node(prototype, new ArrayList<>());
        for (String name : ListTileWidgetPropertySchema.geometryProperties()) {
            var editor = cell(node, name).getPropertyEditor();
            for (String token : List.of("Infinity", "-Infinity", "NaN")) {
                editor.setAsText(token); var expected = new PropertyValue.EnumValue("double", token.equals("NaN") ? "nan" : token.startsWith("-") ? "negativeInfinity" : "infinity");
                assertEquals(FlutterPropertyCellValue.explicit(expected), editor.getValue()); assertEquals(token, editor.getAsText());
                var properties = Map.of(p(name), (PropertyValue) expected); assertValid(new WidgetNode(prototype.id(), DEF.typeId(), properties, prototype.slots()));
            }
            editor.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("double.nan"));
        }
        var slider = BuiltInWidgetCatalog.getDefault().find(SliderWidgetPropertySchema.SLIDER_TYPE).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> FlutterTypedPropertyEditors.binding(slider.property(p("secondaryTrackValue")).orElseThrow()).orElseThrow().createEditor().setAsText("NaN"));
        var refresh = BuiltInWidgetCatalog.getDefault().find(RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> FlutterTypedPropertyEditors.binding(refresh.property(p("strokeWidth")).orElseThrow()).orElseThrow().createEditor().setAsText("NaN"));
    }

    @Test void callbacksRemainPresentWhenDisabledAndKeepNoopNullAndOmissionDistinct() throws Exception {
        var prototype = prototype();
        for (String name : List.of("onTap", "onLongPress", "onFocusChange")) {
            var before = new WidgetNode(prototype.id(), DEF.typeId(), Map.of(p(name), value(name)), prototype.slots()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, "enabled").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.BooleanValue(false)));
            var disabled = apply(before, commands.removeFirst()); assertEquals(value(name), disabled.properties().get(p(name))); assertValid(disabled);
            node.refreshPresentation(disabled, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            for (PropertyValue value : List.of(new PropertyValue.StringValue("noop"), new PropertyValue.NullValue())) {
                cell(node, name).setValue(FlutterPropertyCellValue.explicit(value)); var after = apply(disabled, commands.removeFirst()); assertEquals(value, after.properties().get(p(name))); assertValid(after);
            }
            cell(node, name).restoreDefaultValue(); assertFalse(apply(disabled, commands.removeFirst()).properties().containsKey(p(name)));
        }
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full() { return full(false, false); }
    public static LinkedHashMap<PropertyName, PropertyValue> full(boolean physicalPadding, boolean foregroundPaint) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (List.of("shape", "visualDensity", "iconColor", "textColor", "mouseCursor").contains(name) || ListTileWidgetPropertySchema.styleFamilies().contains(name)) continue;
            if (ListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name) && !ListTileWidgetPropertySchema.shapePropertyAppliesToKind(name, "roundedRectangle")) continue;
            var style = ListTileWidgetPropertySchema.textStyleFamily(field.name());
            if (style.isPresent()) {
                String suffix = name.substring(style.orElseThrow().length());
                if (foregroundPaint ? List.of("Color", "BackgroundColor").contains(suffix) : List.of("Foreground", "Background").contains(suffix)) continue;
            }
            result.put(field.name(), value(name));
        }
        if (physicalPadding) result.put(p("contentPadding"), new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        ListTileWidgetPropertySchema.textStyleFamily(p(name)).ifPresent(family -> {
            if (name.equals(family + "Package")) result.put(p(family + "FontFamily"), new PropertyValue.StringValue("Inter"));
        });
        for (String family : List.of("iconColor", "textColor", "mouseCursor"))
            if (locals(family).contains(name) && !name.equals(family + "Default")) result.put(p(family + "Default"), value(family + "Default"));
        return result;
    }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        var style = ListTileWidgetPropertySchema.textStyleFamily(p(name));
        if (style.isPresent()) return BadgePropertyContractTest.value("textStyle" + name.substring(style.orElseThrow().length()));
        if (ListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) return CardPropertyContractTest.value(name);
        if (ListTileWidgetPropertySchema.styleFamilies().contains(name)) return reference("_textStyle");
        if (ListTileWidgetPropertySchema.stateColorFamilies().stream().anyMatch(family -> ListTileWidgetPropertySchema.colorStateProperties(family).contains(name))
                || ListTileWidgetPropertySchema.colorProperties().contains(name)) return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
        if (name.equals("mouseCursor") || ListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) return new PropertyValue.StringValue("click");
        return switch (name) {
            case "visualDensity" -> reference("_density");
            case "shape" -> reference("_shape");
            case "onTap", "onLongPress", "onFocusChange", "focusNode", "statesController" -> reference("_" + name);
            case "contentPadding" -> new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
            case "style" -> new PropertyValue.EnumValue("ListTileStyle", "drawer");
            case "titleAlignment" -> new PropertyValue.EnumValue("ListTileTitleAlignment", "center");
            case "isThreeLine", "dense", "enableFeedback", "selected", "autofocus", "internalAddSemanticForOnTap" -> new PropertyValue.BooleanValue(true);
            case "enabled" -> new PropertyValue.BooleanValue(false);
            default -> new PropertyValue.DoubleValue(new BigDecimal("2.5"));
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode prototype() {
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        for (String name : List.of("leading", "title", "subtitle", "trailing")) slots.put(new SlotName(name), WidgetSlot.SingleSlot.of(
                new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue(name)), Map.of())));
        return new WidgetNode(StableId.random(), DEF.typeId(), Map.of(), slots);
    }
    private static List<String> locals(String family) {
        return family.equals("visualDensity") ? List.of("visualDensityHorizontal", "visualDensityVertical")
                : family.equals("mouseCursor") ? ListTileWidgetPropertySchema.mouseCursorStateProperties()
                : ListTileWidgetPropertySchema.styleFamilies().contains(family) ? ListTileWidgetPropertySchema.localTextStyleProperties(family)
                : ListTileWidgetPropertySchema.colorStateProperties(family);
    }
    static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(s -> Arrays.stream(s.getProperties())).filter(p -> p.getName().equals(name)).findFirst().orElseThrow(); }
    static void assertValid(WidgetNode widget) {
        var region = new ManagedRegion("0".repeat(64)); var result = new WidgetTreeValidator().validate(new DesignerDocument(StableId.random(),
                new DartSourceDescriptor("list_tile.dart", "ListTileScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.valid(), () -> result.errors().toString());
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
