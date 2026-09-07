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

public class SwitchPropertyContractTest {
    public static final WidgetDefinition DEF = BuiltInWidgetCatalog.getDefault().find(SwitchWidgetPropertySchema.SWITCH_TYPE).orElseThrow();
    private static final StableId ID = StableId.parse("290282b9-2c96-44cc-a578-ef8a9d8e338a");

    @Test void all201CellsKeepStableIdentityAndSparseEditResetCommandsPreservePeers() throws Exception {
        var prototype = WidgetNodePrototypeFactory.create(DEF, ID); var commands = new ArrayList<DesignerCommand>();
        var node = node(prototype, commands); var sets = node.getPropertySets();
        assertEquals(201, DEF.properties().size()); assertEquals(11, sets.length); assertTrue(DEF.slots().isEmpty());
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

    @Test void bothFullConstructorsAndAdaptiveOnlyNullFlagSwitchAtomically() throws Exception {
        for (String variant : SwitchWidgetPropertySchema.variants()) assertValid(new WidgetNode(ID, DEF.typeId(), full(variant), Map.of()));
        var before = new WidgetNode(ID, DEF.typeId(), full("adaptive"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
        cell(node, "variant").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("standard")));
        assertEquals(1, commands.size()); assertInstanceOf(PatchProperties.class, commands.getFirst()); var standard = apply(before, commands.getFirst()); assertValid(standard);
        var expected = new LinkedHashMap<>(before.properties()); expected.remove(p("applyCupertinoTheme")); expected.put(p("variant"), new PropertyValue.StringValue("standard")); assertEquals(expected, standard.properties());
        for (PropertyValue mode : List.of(new PropertyValue.BooleanValue(true), new PropertyValue.BooleanValue(false), new PropertyValue.NullValue())) {
            node.refreshPresentation(standard, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
            cell(node, "applyCupertinoTheme").setValue(FlutterPropertyCellValue.explicit(mode)); assertEquals(1, commands.size());
            var adaptive = apply(standard, commands.getFirst()); assertValid(adaptive); assertEquals(new PropertyValue.StringValue("adaptive"), adaptive.properties().get(p("variant"))); assertEquals(mode, adaptive.properties().get(p("applyCupertinoTheme")));
        }
    }

    @Test void everyWholeReferenceAndLocalLeafSwitchOnlyItsOwnFamily() throws Exception {
        var families = new LinkedHashMap<String, List<String>>();
        for (String family : SwitchWidgetPropertySchema.colorFamilies()) families.put(family, SwitchWidgetPropertySchema.colorStateProperties(family));
        families.put("trackOutlineWidth", SwitchWidgetPropertySchema.outlineWidthStateProperties()); families.put("thumbIcon", SwitchWidgetPropertySchema.thumbIconLocalProperties());
        for (var family : families.entrySet()) {
            var before = new WidgetNode(ID, DEF.typeId(), full("adaptive"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            cell(node, family.getKey()).setValue(FlutterPropertyCellValue.explicit(reference("_" + family.getKey()))); assertEquals(1, commands.size());
            var whole = apply(before, commands.getFirst()); assertValid(whole);
            for (String local : family.getValue()) assertFalse(whole.properties().containsKey(p(local)));
            for (var peer : before.properties().entrySet()) if (!family.getValue().contains(peer.getKey().value())) assertEquals(peer.getValue(), whole.properties().get(peer.getKey()));
            for (String local : family.getValue()) {
                node.refreshPresentation(whole, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                cell(node, local).setValue(FlutterPropertyCellValue.explicit(value(local))); assertEquals(1, commands.size());
                var edited = apply(whole, commands.getFirst()); assertValid(edited); assertFalse(edited.properties().containsKey(p(family.getKey()))); assertEquals(value(local), edited.properties().get(p(local)));
            }
        }
    }

    @Test void allNineIconBranchesDistinguishInheritedNullOmittedDataAndExplicitNone() throws Exception {
        for (String state : SwitchWidgetPropertySchema.thumbIconStates()) {
            var bucket = SwitchWidgetPropertySchema.thumbIconBucketProperties(state); String mode = bucket.getFirst();
            var before = new WidgetNode(ID, DEF.typeId(), full("standard"), Map.of()); var commands = new ArrayList<DesignerCommand>(); var node = node(before, commands);
            for (boolean reset : List.of(false, true)) {
                node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                if (reset) cell(node, mode).restoreDefaultValue(); else cell(node, mode).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.StringValue("inherit")));
                assertEquals(1, commands.size()); var cleared = apply(before, commands.getFirst()); assertValid(cleared);
                for (String leaf : bucket.subList(1, bucket.size())) assertFalse(cleared.properties().containsKey(p(leaf)));
                for (var peer : before.properties().entrySet()) if (!bucket.contains(peer.getKey().value())) assertEquals(peer.getValue(), cleared.properties().get(peer.getKey()));
                for (String leaf : bucket.subList(1, bucket.size())) {
                    node.refreshPresentation(cleared, DEF, commands::add, null, null, FlutterImageAssetChoices.empty()); commands.clear();
                    cell(node, leaf).setValue(FlutterPropertyCellValue.explicit(value(leaf))); var icon = apply(cleared, commands.getFirst()); assertValid(icon);
                    assertEquals(new PropertyValue.StringValue("icon"), icon.properties().get(p(mode)));
                }
            }
            var sparse = new LinkedHashMap<>(WidgetNodePrototypeFactory.create(DEF, ID).properties()); sparse.put(p(mode), new PropertyValue.StringValue("icon"));
            assertValid(new WidgetNode(ID, DEF.typeId(), sparse, Map.of()));
            sparse.put(p("thumbIcon" + state + "Data"), PropertyValue.IconDataValue.none()); assertValid(new WidgetNode(ID, DEF.typeId(), sparse, Map.of()));
        }
    }

    @Test void matchingImageErrorsRequireProviderAndProviderResetPreservesOtherImageAndCallback() throws Exception {
        for (String active : List.of("Active", "Inactive")) {
            String image = Character.toLowerCase(active.charAt(0)) + active.substring(1) + "ThumbImage"; String callback = "on" + active + "ThumbImageError";
            var commands = new ArrayList<DesignerCommand>(); var node = node(WidgetNodePrototypeFactory.create(DEF, ID), commands);
            var failure = assertThrows(IllegalArgumentException.class, () -> cell(node, callback).setValue(FlutterPropertyCellValue.explicit(value(callback)))); assertTrue(failure.getMessage().contains(image)); assertTrue(commands.isEmpty());
            var before = new WidgetNode(ID, DEF.typeId(), full("adaptive"), Map.of()); node.refreshPresentation(before, DEF, commands::add, null, null, FlutterImageAssetChoices.empty());
            cell(node, image).restoreDefaultValue(); assertEquals(1, commands.size()); var result = apply(before, commands.getFirst()); assertValid(result);
            var expected = new LinkedHashMap<>(before.properties()); expected.remove(p(image)); expected.remove(p(callback)); assertEquals(expected, result.properties());
            assertEquals("ImageErrorListener", field(callback).constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance).map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType());
        }
    }

    @Test void exactNullableFamiliesPreserveNullAndSharedIconNumericBindingsStayNarrow() throws Exception {
        var commands = new ArrayList<DesignerCommand>(); var node = node(WidgetNodePrototypeFactory.create(DEF, ID), commands);
        for (String family : SwitchWidgetPropertySchema.colorFamilies()) for (String name : SwitchWidgetPropertySchema.colorStateProperties(family)) {
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_THEME_COLOR, FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().editorKind());
            commands.clear(); cell(node, name).setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())); assertValid(apply(WidgetNodePrototypeFactory.create(DEF, ID), commands.getFirst()));
        }
        for (String name : SwitchWidgetPropertySchema.outlineWidthStateProperties()) {
            assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_NUMBER_WITH_INFINITY, FlutterTypedPropertyEditors.binding(field(name)).orElseThrow().editorKind());
            var editor = cell(node, name).getPropertyEditor(); editor.setAsText("null"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue()), editor.getValue());
            editor.setAsText("Infinity"); assertEquals(FlutterPropertyCellValue.explicit(new PropertyValue.EnumValue("double", "infinity")), editor.getValue());
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("-Infinity"));
            assertThrows(IllegalArgumentException.class, () -> editor.setAsText("double.infinity"));
        }
        assertEquals(FlutterTypedPropertyEditors.EditorKind.BOOLEAN, FlutterTypedPropertyEditors.binding(field("value")).orElseThrow().editorKind());
        assertEquals(FlutterTypedPropertyEditors.EditorKind.NULLABLE_BOOLEAN, FlutterTypedPropertyEditors.binding(field("applyCupertinoTheme")).orElseThrow().editorKind());
        assertThrows(IllegalArgumentException.class, () -> cell(node, "value").setValue(FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())));
        for (String state : SwitchWidgetPropertySchema.thumbIconStates()) {
            var binding = FlutterTypedPropertyEditors.binding(field("thumbIcon" + state + "Data")).orElseThrow(); assertTrue(binding.optional());
            assertEquals(FlutterTypedPropertyEditors.EditorKind.ICON_DATA, binding.editorKind());
        }
        assertFalse(FlutterTypedPropertyEditors.binding(BuiltInWidgetCatalog.getDefault().find(IconWidgetPropertySchema.ICON_TYPE).orElseThrow().property(p("icon")).orElseThrow()).orElseThrow().optional());
    }

    public static LinkedHashMap<PropertyName, PropertyValue> full(String variant) {
        var result = new LinkedHashMap<PropertyName, PropertyValue>();
        for (var field : DEF.properties()) {
            String name = field.name().value();
            if (SwitchWidgetPropertySchema.colorFamilies().contains(name) || List.of("trackOutlineWidth", "thumbIcon").contains(name)) continue;
            if (variant.equals("standard") && name.equals("applyCupertinoTheme")) continue;
            result.put(field.name(), name.equals("variant") ? new PropertyValue.StringValue(variant) : name.equals("enabled") ? new PropertyValue.BooleanValue(true) : value(field));
        }
        return result;
    }
    public static Map<PropertyName, PropertyValue> sparsePrerequisites(String name) {
        return switch (name) {
            case "onActiveThumbImageError" -> Map.of(p("activeThumbImage"), value("activeThumbImage"));
            case "onInactiveThumbImageError" -> Map.of(p("inactiveThumbImage"), value("inactiveThumbImage"));
            default -> Map.of();
        };
    }
    public static PropertyDefinition field(String name) { return DEF.property(p(name)).orElseThrow(); }
    public static PropertyValue value(PropertyDefinition field) { return value(field.name().value()); }
    public static PropertyValue value(String name) {
        var icon = SwitchWidgetPropertySchema.iconSourceName(name);
        if (icon.isPresent()) return iconValue(icon.orElseThrow());
        if (name.equals("variant")) return new PropertyValue.StringValue("adaptive");
        if (name.equals("enabled")) return new PropertyValue.BooleanValue(false);
        if (List.of("value", "autofocus", "applyCupertinoTheme").contains(name)) return new PropertyValue.BooleanValue(true);
        if (name.endsWith("Mode")) return new PropertyValue.StringValue("icon");
        if (List.of("onChanged", "onFocusChange", "focusNode", "mouseCursor", "onActiveThumbImageError", "onInactiveThumbImageError", "trackOutlineWidth", "thumbIcon").contains(name) || SwitchWidgetPropertySchema.colorFamilies().contains(name)) return reference("_" + name);
        if (name.equals("activeThumbImage")) return PropertyValue.ImageProviderValue.asset("assets/switch_thumb.png");
        if (name.equals("inactiveThumbImage")) return PropertyValue.ImageProviderValue.exactAsset("assets/switch_thumb.png", BigDecimal.valueOf(2));
        if (name.equals("materialTapTargetSize")) return new PropertyValue.EnumValue("MaterialTapTargetSize", "shrinkWrap");
        if (name.equals("dragStartBehavior")) return new PropertyValue.EnumValue("DragStartBehavior", "down");
        if (name.equals("padding")) return new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        if (name.startsWith("trackOutlineWidth") || name.equals("splashRadius")) return new PropertyValue.DoubleValue(new BigDecimal("2.5"));
        return new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
    }
    private static PropertyValue iconValue(String name) {
        return switch (name) {
            case "icon" -> new PropertyValue.IconDataValue(Optional.of(0xE5F9), Optional.of("MaterialIcons"), Optional.empty(), false, List.of());
            case "color" -> new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));
            case "shadows" -> new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(StableId.parse("90ad318c-d605-455f-884a-dff35c68982d"), new ColorSource.Literal(0xff112233L), BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
            case "blendMode" -> new PropertyValue.EnumValue("BlendMode", "multiply");
            case "fontWeight" -> new PropertyValue.EnumValue("FontWeight", "w600");
            case "textDirection" -> new PropertyValue.EnumValue("TextDirection", "rtl");
            case "semanticLabel" -> new PropertyValue.StringValue("Switch thumb");
            case "applyTextScaling" -> new PropertyValue.BooleanValue(true);
            case "fill" -> new PropertyValue.DoubleValue(new BigDecimal("0.5"));
            case "grade" -> new PropertyValue.DoubleValue(new BigDecimal("-1"));
            default -> new PropertyValue.DoubleValue(new BigDecimal("24"));
        };
    }
    public static PropertyValue.DartObjectReferenceValue reference(String name) { return FloatingActionButtonPropertyContractTest.reference(name); }
    public static WidgetNode apply(WidgetNode widget, DesignerCommand command) { return FloatingActionButtonPropertyContractTest.apply(widget, command); }
    private static FlutterWidgetPropertiesNode node(WidgetNode widget, List<DesignerCommand> commands) { return new FlutterWidgetPropertiesNode(Children.LEAF, widget, DEF, commands::add); }
    @SuppressWarnings("unchecked") private static Node.Property<FlutterPropertyCellValue> cell(FlutterWidgetPropertiesNode node, String name) { return (Node.Property<FlutterPropertyCellValue>) Arrays.stream(node.getPropertySets()).flatMap(set -> Arrays.stream(set.getProperties())).filter(v -> v.getName().equals(name)).findFirst().orElseThrow(); }
    private static void assertValid(WidgetNode widget) { var region = new ManagedRegion("0".repeat(64)); var document = new DesignerDocument(StableId.random(), new DartSourceDescriptor("switch.dart", "SwitchScreen", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), widget); var result = new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()); assertTrue(result.valid(), result.toString()); }
    private static PropertyName p(String name) { return new PropertyName(name); }
}
